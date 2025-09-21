package com.example.celestial.ui.viewmodels

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.CollectionReference
import android.os.Handler
import android.os.Looper
import com.example.celestial.data.models.IngredientEditRecord
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import kotlinx.coroutines.tasks.await
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.map
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.State
import com.example.celestial.data.models.Wastage
import com.example.celestial.data.models.Sale
import com.example.celestial.data.models.SoldCake
import kotlinx.coroutines.launch
import android.util.Log
import android.content.Context
import android.net.Uri
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.*
import com.example.celestial.data.models.Cake
import com.example.celestial.data.models.Ingredient
import com.example.celestial.data.models.Stock
import com.example.celestial.data.repositories.ExpiryRepository
import com.example.celestial.data.repositories.InventoryRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

interface SupabaseStorageService {
    @POST("storage/v1/object/{bucket}/{path}")
    @Headers(
        "x-api-key: eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImVrdGF6enBkZWNyenhraW5tYnNmIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NTc4NDIyMTcsImV4cCI6MjA3MzQxODIxN30.55bWyS2OVq235_SF2Dm-8eVg18mNQlCuvl-qeZvFMgg",
        "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImVrdGF6enBkZWNyenhraW5tYnNmIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NTc4NDIyMTcsImV4cCI6MjA3MzQxODIxN30.55bWyS2OVq235_SF2Dm-8eVg18mNQlCuvl-qeZvFMgg"
    )
    suspend fun uploadImage(
        @Path("bucket") bucket: String,
        @Path("path", encoded = true) path: String,
        @Body body: RequestBody
    ): Response<ResponseBody>
}

data class CartItem(
    val cake: Cake,
    var wholeCakeQuantity: Int = 0,
    var sliceQuantity: Int = 0
)

data class UserProfileUi(
    val username: String = "",
    val email: String = "",
    val fullName: String = "",
    val dateOfBirth: String = "",
    val gender: String = "",
    val phone: String = "",
    val address: String = ""
)

class InventoryViewModel(
    private val repo: InventoryRepository,
    private val expiryRepo: ExpiryRepository
) : ViewModel() {
    private val supabaseApi: SupabaseStorageService by lazy {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://ektazzpdecrzxkinmbsf.supabase.co/")
            .addConverterFactory(GsonConverterFactory.create())
            .client(OkHttpClient.Builder().build())
            .build()
        retrofit.create(SupabaseStorageService::class.java)
    }
    private val supabaseUrl = "https://ektazzpdecrzxkinmbsf.supabase.co"
    private val supabaseAnonKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImVrdGF6enBkZWNyenhraW5tYnNmIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NTc4NDIyMTcsImV4cCI6MjA3MzQxODIxN30.55bWyS2OVq235_SF2Dm-8eVg18mNQlCuvl-qeZvFMgg"

    private val _isEditMode = mutableStateOf(false)
    val isEditMode: State<Boolean> get() = _isEditMode

    fun setEditMode(enabled: Boolean) {
        _isEditMode.value = enabled
    }

    private val pendingAvailabilityUpdates = mutableSetOf<String>()

    private val _userProfile = mutableStateOf(UserProfileUi())
    val userProfile: State<UserProfileUi> = _userProfile

    private val _sales = MutableLiveData<List<Sale>>(emptyList())
    val sales: LiveData<List<Sale>> = _sales


    private var userProfileListener: ListenerRegistration? = null

    private val stockIngredientsListeners = mutableMapOf<String, ListenerRegistration>()

    private val _ingredients = MutableLiveData<List<Ingredient>>(emptyList())
    val ingredients: LiveData<List<Ingredient>> = _ingredients

    private val _cakes = MutableLiveData<List<Cake>>(emptyList())
    val cakes: LiveData<List<Cake>> = _cakes

    private val _expiringIngredients = MutableLiveData<List<Ingredient>>(emptyList())
    val expiringIngredients: LiveData<List<Ingredient>> = _expiringIngredients

    private val stockListeners = mutableMapOf<String, ListenerRegistration?>()

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    private val availableCakesMap = mutableMapOf<String, MutableLiveData<Int>>()
    private val stocksMap = mutableMapOf<String, MutableLiveData<List<Stock>>>()

    private val _cartItems = MutableStateFlow<List<CartItem>>(emptyList())
    val cartItems: StateFlow<List<CartItem>> = _cartItems

    private val originalQuantities =
        mutableMapOf<String, Pair<Int, Int>>() // cakeId -> (whole, slices)onclea
    var reportText by mutableStateOf<String?>(null)

    // Optional: manage loading flag for UI separation
    var isLoading by mutableStateOf(false)

    private var authListener: FirebaseAuth.AuthStateListener? = null

    private val _wastageRecords = MutableLiveData<List<Pair<String, Wastage>>>(emptyList())
    val wastageRecords: LiveData<List<Pair<String, Wastage>>> = _wastageRecords

    private val _ingredientEditRecords = MutableLiveData<List<IngredientEditRecord>>(emptyList())
    val ingredientEditRecords: LiveData<List<IngredientEditRecord>> = _ingredientEditRecords

    private val _deletionProgress = MutableStateFlow(0f)
    val deletionProgress = _deletionProgress.asStateFlow()

    fun saveIngredientEditRecord(record: IngredientEditRecord) {
        viewModelScope.launch {
            try {
                val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return@launch
                val colRef = FirebaseFirestore.getInstance()
                    .collection("users").document(uid)
                    .collection("ingredient_edit_records")
                Log.d("DEBUG", "Attempt to save edit record: $record")
                Log.d("DEBUG", "Current UID: $uid")

                colRef.add(record).await()
                fetchIngredientEditRecords()
            } catch (ex: Exception) {
                _errorMessage.postValue("Failed saving edit record: ${ex.message}")
            }
        }
    }

    fun fetchIngredientEditRecords() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val colRef = FirebaseFirestore.getInstance()
            .collection("users").document(uid)
            .collection("ingredient_edit_records")

        colRef.addSnapshotListener { snap, err ->
            if (err != null) return@addSnapshotListener
            val records = snap?.mapNotNull { it.toObject(IngredientEditRecord::class.java) } ?: emptyList()
            _ingredientEditRecords.postValue(records)
        }
    }


    // ===================== AUTH BINDING =====================

    fun startAuthBinding(context: Context? = null) {
        if (authListener != null) return
        val auth = FirebaseAuth.getInstance()
        authListener = FirebaseAuth.AuthStateListener { fb ->
            userProfileListener?.remove()
            if (fb.currentUser == null) {
                _userProfile.value = UserProfileUi()
            }
            // On login, let handleSuccessfulLogin call startListeningUserProfile
        }
        auth.addAuthStateListener(authListener!!)
    }


    fun handleLogout(context: Context? = null) {
        clearData()
        FirebaseAuth.getInstance().signOut()
        context?.let {
            viewModelScope.launch { clearUsername(it) }
        }
    }



    // Call after login success (on login screen)
    fun handleSuccessfulLogin(context: Context? = null) {
        clearData()
        startAuthBinding(context)
        startListeningUserProfile(context)
        fetchIngredientEditRecords()
    }


    // ===================== PROFILE LISTENER =====================

    fun startListeningUserProfile(context: Context? = null) {
        userProfileListener?.remove()
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: run {
            _userProfile.value = UserProfileUi()
            return
        }
        val ref = FirebaseFirestore.getInstance().collection("users").document(uid)
        userProfileListener = ref.addSnapshotListener { snapshot, error ->
            if (error != null || snapshot == null || !snapshot.exists()) {
                _userProfile.value = UserProfileUi()
                return@addSnapshotListener
            }
            _userProfile.value = UserProfileUi(
                username = snapshot.getString("username").orEmpty(),
                email = snapshot.getString("email").orEmpty(),
                fullName = snapshot.getString("fullName").orEmpty(),
                dateOfBirth = snapshot.getString("dateOfBirth").orEmpty(),
                gender = snapshot.getString("gender").orEmpty(),
                phone = snapshot.getString("phone").orEmpty(),
                address = snapshot.getString("address").orEmpty()
            )
        }
    }


    // ===================== ONE-TIME BACKFILL =====================

    /**
     * If users/{uid} doesn't exist but users_usernames/{username} exists, copy fields into users/{uid}.
     * Call this once after sign-in.
     */
    suspend fun ensureUserProfileFromUsername() {
        val auth = FirebaseAuth.getInstance()
        val uid = auth.currentUser?.uid ?: return
        val db = FirebaseFirestore.getInstance()

        val usersDoc = db.collection("users").document(uid).get().await()
        if (usersDoc.exists()) {
            Log.d(
                "InventoryVM",
                "ensureUserProfileFromUsername: users/$uid already exists, skip backfill"
            )
            return
        }

        // Try to find username from mapping doc or from current _userProfile (if set elsewhere)
        val usernameFromState = userProfile.value.username.takeIf { it.isNotBlank() }
        val mappingUsername = usernameFromState ?: run {
            // Optionally, you can store username in Auth displayName or in DataStore. Not mandatory.
            null
        }

        if (mappingUsername == null) {
            Log.w(
                "InventoryVM",
                "ensureUserProfileFromUsername: no username known; cannot backfill"
            )
            return
        }

        val unameDoc =
            db.collection("users_usernames").document(mappingUsername.lowercase()).get().await()
        if (!unameDoc.exists()) {
            Log.w(
                "InventoryVM",
                "ensureUserProfileFromUsername: users_usernames/${mappingUsername} not found"
            )
            return
        }

        val data = unameDoc.data ?: emptyMap<String, Any>()
        val payload = mapOf(
            "username" to (data["username"] as? String ?: mappingUsername),
            "email" to (data["email"] as? String ?: ""),
            "fullName" to (data["fullName"] as? String ?: ""),
            "dateOfBirth" to (data["dateOfBirth"] as? String ?: ""),
            "gender" to (data["gender"] as? String ?: ""),
            "phone" to (data["phone"] as? String ?: ""),
            "address" to (data["address"] as? String ?: "")
        )

        db.collection("users").document(uid).set(payload, SetOptions.merge()).await()
        Log.d(
            "InventoryVM",
            "ensureUserProfileFromUsername: backfilled users/$uid from users_usernames/${mappingUsername}"
        )
    }

    // Optional utility while testing
    fun debugFetchUserDocOnce() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: run {
            Log.w("InventoryVM", "debugFetchUserDocOnce: no current user")
            return
        }
        val ref = FirebaseFirestore.getInstance().collection("users").document(uid)
        Log.d("InventoryVM", "debugFetchUserDocOnce: fetching from server path=${ref.path}")
        ref.get()
            .addOnSuccessListener { snap ->
                Log.d(
                    "InventoryVM",
                    "debugFetchUserDocOnce: exists=${snap.exists()}; data=${snap.data}"
                )
            }
            .addOnFailureListener { e ->
                Log.e("InventoryVM", "debugFetchUserDocOnce: failed", e)
            }
    }

    // ===================== REGISTRATION (kept, ensure writes to users/{uid}) =====================

    // In InventoryViewModel.kt (top-level or inside the file)
    sealed class RegisterEvent {
        object Success : RegisterEvent()
        data class Error(val message: String) : RegisterEvent()
    }

    // Inside InventoryViewModel class:
    private val _registerEvents =
        kotlinx.coroutines.flow.MutableSharedFlow<RegisterEvent>(extraBufferCapacity = 1)
    val registerEvents = _registerEvents.asSharedFlow()

    // In registerWithUsername, replace your final callbacks with event emissions in every terminal path:
    fun registerWithUsername(
        username: String,
        email: String,
        password: String,
        fullName: String,
        dateOfBirth: String,
        gender: String,
        phone: String,
        address: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        val firestore = FirebaseFirestore.getInstance()
        val usernamesRef = firestore.collection("users_usernames")

        usernamesRef.document(username.lowercase()).get()
            .addOnSuccessListener { doc: DocumentSnapshot ->
                if (doc.exists()) {
                    onFailure("Username already taken.")
                    return@addOnSuccessListener
                }

                FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password)
                    .addOnSuccessListener { authResult ->
                        val newUid = authResult.user?.uid
                        if (newUid == null) {
                            onFailure("Registration failed: UID is null")
                            return@addOnSuccessListener
                        }

                        // Wait for FirebaseAuth to update currentUser to new user
                        waitForAuthUser(newUid, 0, onSuccess, onFailure,
                            onReady = {
                                // Once ready, continue Firestore writes
                                writeUserData(newUid, username, email, fullName, dateOfBirth, gender, phone, address, firestore, usernamesRef, onSuccess, onFailure)
                            })
                    }
                    .addOnFailureListener { e ->
                        onFailure("Registration failed: ${e.message}")
                    }
            }
            .addOnFailureListener { e ->
                onFailure("Registration failed: ${e.message}")
            }
    }

    // Repeatedly checks if FirebaseAuth.currentUser matches newUid, up to max 5 tries
    private fun waitForAuthUser(newUid: String, attempt: Int, onSuccess: () -> Unit, onFailure: (String) -> Unit, onReady: () -> Unit) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null && currentUser.uid == newUid) {
            onReady()
        } else {
            if (attempt >= 5) {
                onFailure("Auth state still not ready, please try again.")
                return
            }
            Handler(Looper.getMainLooper()).postDelayed({
                waitForAuthUser(newUid, attempt + 1, onSuccess, onFailure, onReady)
            }, 500)
        }
    }

    private fun writeUserData(
        uid: String,
        username: String,
        email: String,
        fullName: String,
        dateOfBirth: String,
        gender: String,
        phone: String,
        address: String,
        firestore: FirebaseFirestore,
        usernamesRef: CollectionReference,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        val unameRef = usernamesRef.document(username.lowercase())
        val indexPayload = mapOf(
            "uid" to uid,
            "email" to email,
            "username" to username,
            "fullName" to fullName,
            "dateOfBirth" to dateOfBirth,
            "gender" to gender,
            "phone" to phone,
            "address" to address
        )

        unameRef.set(indexPayload)
            .addOnSuccessListener {
                val userRef = firestore.collection("users").document(uid)
                val profilePayload = mapOf(
                    "username" to username,
                    "email" to email,
                    "fullName" to fullName,
                    "dateOfBirth" to dateOfBirth,
                    "gender" to gender,
                    "phone" to phone,
                    "address" to address
                )

                userRef.set(profilePayload)
                    .addOnSuccessListener { onSuccess() }
                    .addOnFailureListener { e ->
                        onFailure("Registration failed (profile): ${e.message}")
                    }
            }
            .addOnFailureListener { e ->
                onFailure("Registration failed (username index): ${e.message}")
            }
    }



    suspend fun updateUserProfileField(field: String, value: String): Boolean {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return false
        val db = FirebaseFirestore.getInstance()

        val currentUserProfile = _userProfile.value
        val currentUsername = currentUserProfile.username.lowercase()

        val fieldKey = when (field.trim().lowercase()) {
            "display name", "fullname", "full name" -> "fullName"
            "username" -> "username"
            "email address", "email" -> "email"
            "date of birth" -> "dateOfBirth"
            "gender" -> "gender"
            "phone number", "phone" -> "phone"
            "address" -> "address"
            else -> return false
        }

        return try {
            // Update users document
            val userDocRef = db.collection("users").document(uid)
            userDocRef.set(mapOf(fieldKey to value), SetOptions.merge()).await()

            // Fetch the existing users_usernames document data
            val oldUsernameDocRef = db.collection("users_usernames").document(currentUsername)
            val snapshot = oldUsernameDocRef.get().await()
            val existingData = if (snapshot.exists()) snapshot.data!!.toMutableMap() else mutableMapOf<String, Any>()

            // Merge in the edited field
            existingData[fieldKey] = value

            if (fieldKey == "username") {
                // Username changed - delete old doc and create new doc with new username key
                val newUsername = (value as String).lowercase()
                if (currentUsername != newUsername) {
                    // Delete old username doc
                    oldUsernameDocRef.delete().await()
                    // Write merged data to new username doc
                    val newUsernameDocRef = db.collection("users_usernames").document(newUsername)
                    newUsernameDocRef.set(existingData, SetOptions.merge()).await()
                    // Update local userProfile value
                    _userProfile.value = currentUserProfile.copy(username = newUsername)
                } else {
                    // User didn't actually change username string (unlikely), just update existing doc
                    oldUsernameDocRef.set(existingData, SetOptions.merge()).await()
                }
            } else {
                // For other fields, write merged data back to same username doc
                oldUsernameDocRef.set(existingData, SetOptions.merge()).await()
            }

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }





    fun migrateProfileFromUsernameDocToUid(username: String) {
        val auth = FirebaseAuth.getInstance()
        val uid = auth.currentUser?.uid ?: return
        val db = FirebaseFirestore.getInstance()
        val usernameDoc = db.collection("users_usernames").document(username.lowercase())
        val userDoc = db.collection("users").document(uid)

        usernameDoc.get()
            .addOnSuccessListener { snap ->
                if (!snap.exists()) {
                    Log.w("InventoryVM", "Migration: users_usernames/$username not found")
                    return@addOnSuccessListener
                }
                val data = snap.data ?: emptyMap<String, Any>()
                // Map fields present in your username doc; adjust keys if different
                val payload = mapOf(
                    "username" to (data["username"] as? String ?: username),
                    "email" to (data["email"] as? String ?: ""),
                    "fullName" to (data["fullName"] as? String ?: ""),
                    "dateOfBirth" to (data["dateOfBirth"] as? String ?: ""),
                    "gender" to (data["gender"] as? String ?: ""),
                    "phone" to (data["phone"] as? String ?: ""),
                    "address" to (data["address"] as? String ?: "")
                )
                userDoc.set(payload, com.google.firebase.firestore.SetOptions.merge())
                    .addOnSuccessListener {
                        Log.d("InventoryVM", "Migration: wrote profile to users/$uid")
                    }
                    .addOnFailureListener { e ->
                        Log.e("InventoryVM", "Migration: failed writing users/$uid", e)
                    }
            }
            .addOnFailureListener { e ->
                Log.e("InventoryVM", "Migration: failed reading users_usernames/$username", e)
            }
    }


    fun startListeningWastage() {
        repo.listenWastageChanges { combinedList ->
            _wastageRecords.postValue(combinedList) // Always full combined data here
        }
    }

    fun initializeDataAfterLogin() {
        if (FirebaseAuth.getInstance().currentUser != null) {
            startListeningToFirestore()// always first!
            viewModelScope.launch {
                try {
                    repo.seedDataIfEmpty()
                } catch (e: Exception) {
                    _errorMessage.value = "Error initializing data: ${e.message}"
                }
            }
        }
        startListeningWastage()
        fetchIngredientEditRecords()
    }


    val Context.userPrefsDataStore by preferencesDataStore("user_prefs")

    object UserPrefsKeys {
        val USERNAME = stringPreferencesKey("logged_in_username")
    }

    private val _loggedInUsername = mutableStateOf("")
    val loggedInUsername: State<String> = _loggedInUsername

    fun setLoggedInUsername(newUsername: String) {
        _loggedInUsername.value = newUsername
    }

    // Save username to DataStore
    suspend fun saveUsername(context: Context, username: String) {
        context.userPrefsDataStore.edit { prefs ->
            prefs[UserPrefsKeys.USERNAME] = username
        }
    }

    // Read username from DataStore as Flow
    fun getUsernameFlow(context: Context): Flow<String> =
        context.userPrefsDataStore.data.map { prefs ->
            prefs[UserPrefsKeys.USERNAME] ?: ""
        }

    // Load username from DataStore and set into ViewModel state
    fun loadUsername(context: Context) {
        getUsernameFlow(context).onEach { savedName ->
            if (savedName.isNotEmpty()) {
                setLoggedInUsername(savedName)
            }
        }.launchIn(viewModelScope)
    }

    // Clear username from DataStore (for logout)
    suspend fun clearUsername(context: Context) {
        context.userPrefsDataStore.edit { prefs ->
            prefs.remove(UserPrefsKeys.USERNAME)
        }
    }

    private fun startListeningToFirestore() {
        repo.listenIngredientsChanges { ingredientsList ->
            _ingredients.value = ingredientsList  // Synchronous update
            setupRealTimeStockListeners(ingredientsList)
            recalculateAllAvailableCakes()
        }
        repo.listenCakesChanges { cakesList ->
            _cakes.value = cakesList  // Synchronous update
            recalculateAllAvailableCakes()
        }
        repo.listenIngredientsChanges { ingredientsList ->
            _ingredients.value = ingredientsList
            setupRealTimeStockListeners(ingredientsList)
            recalculateAllAvailableCakes()
            fetchAllWastageRecords() // <-- Add this line
        }

    }

    private fun recalculateAllAvailableCakes() {
        _cakes.value?.forEach { cake ->
            calculateAvailableCakes(cake)
        }
    }

    private fun setupRealTimeStockListeners(ingredientsList: List<Ingredient>) {
        val presentIds = ingredientsList.map { it.id }.toSet()
        stockListeners.keys.toList().forEach { ingrId ->
            if (ingrId !in presentIds) {
                stockListeners[ingrId]?.remove()
                stockListeners.remove(ingrId)
                stocksMap.remove(ingrId)
            }
        }
        ingredientsList.forEach { ingr ->
            if (!stockListeners.containsKey(ingr.id)) {
                val liveData = stocksMap.getOrPut(ingr.id) { MutableLiveData(emptyList()) }
                val listener = repo.listenStocksForIngredient(ingr.id) { stocks ->
                    liveData.value = stocks  // Synchronous update for max reactivity
                    recalculateAllAvailableCakes() // Use up-to-the-millisecond state
                }
                stockListeners[ingr.id] = listener
            }
        }
    }

    fun loginWithUsername(
        username: String,
        password: String,
        onSuccess: () -> Unit,
        onFailure: () -> Unit
    ) {
        // Look up email by username (from Firestore)
        val db = FirebaseFirestore.getInstance()
        db.collection("users_usernames").document(username.lowercase()).get()
            .addOnSuccessListener { doc ->
                val email = doc.getString("email")
                if (email == null) {
                    onFailure()
                } else {
                    FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password)
                        .addOnSuccessListener { onSuccess() }
                        .addOnFailureListener { onFailure() }
                }
            }
            .addOnFailureListener { onFailure() }
    }


    private val stockSources = mutableMapOf<String, Observer<List<Stock>>>()
    val expiringSoonCount = MediatorLiveData<Int>().apply {
        fun computeCount() {
            val now = LocalDate.now()
            val threshold = now.plusDays(7)
            var count = 0
            ingredients.value.orEmpty().forEach { ing ->
                getStocksForIngredient(ing.id).value.orEmpty().forEach { stock ->
                    stock.expiryDate?.let { exp ->
                        runCatching { LocalDate.parse(exp) }.getOrNull()?.let { date ->
                            val isExpiringSoon =
                                !date.isBefore(now) && date.isBefore(threshold.plusDays(1)) && stock.quantity > 0
                            if (isExpiringSoon) count++
                        }
                    }
                }
            }
            value = count
        }

        addSource(ingredients) { ings ->
            // Remove previous stock sources
            stockSources.forEach { (ingId, observer) ->
                val stocksLiveData = getStocksForIngredient(ingId)
                removeSource(stocksLiveData)
            }
            stockSources.clear()
            // Add sources for current ingredients
            ings.forEach { ing ->
                val stocksLiveData = getStocksForIngredient(ing.id)
                val observer = Observer<List<Stock>> { computeCount() }
                addSource(stocksLiveData, observer)
                stockSources[ing.id] = observer
            }
            // Also, recompute when ingredient list changes
            computeCount()
        }
    }

    fun getStocksForIngredient(ingredientId: String): LiveData<List<Stock>> {
        return stocksMap.getOrPut(ingredientId) { MutableLiveData(emptyList()) }
    }

    fun getAvailableCakesLiveData(cake: Cake): LiveData<Int> {
        return availableCakesMap.getOrPut(cake.id) {
            MutableLiveData(calculateAvailableCakesSync(cake))
        }
    }

    private fun isStockExpired(stock: Stock): Boolean {
        return try {
            stock.expiryDate?.let {
                LocalDate.parse(it, DateTimeFormatter.ISO_LOCAL_DATE).isBefore(LocalDate.now())
            } ?: false
        } catch (_: Exception) {
            false
        }
    }

    private fun calculateAvailableCakesSync(cake: Cake): Int {
        val ingredientList = _ingredients.value ?: return 0
        if (cake.ingredients.isEmpty()) return 0
        var minCakes = Int.MAX_VALUE
        for ((ingredientId, qtyPerCake) in cake.ingredients) {
            val stocks = stocksMap[ingredientId]?.value
                ?.filter { stock -> !isStockExpired(stock) && stock.quantity > 0 }
                ?.sortedBy { stock ->
                    try {
                        stock.expiryDate?.let { LocalDate.parse(it) } ?: LocalDate.MAX
                    } catch (e: Exception) {
                        LocalDate.MAX
                    }
                }
                ?.map { stock ->
                    if (stock.unit.trim()
                            .uppercase() == "KG"
                    ) stock.quantity * 1000.0 else stock.quantity
                }
                ?.toMutableList() ?: mutableListOf()
            var cakesProduced = 0
            var stocksList = stocks.toMutableList()
            outer@ while (true) {
                var required = qtyPerCake
                for (i in stocksList.indices) {
                    if (stocksList[i] >= required) {
                        stocksList[i] -= required
                        cakesProduced++
                        continue@outer
                    } else {
                        required -= stocksList[i]
                        stocksList[i] = 0.0
                    }
                }
                break
            }
            if (cakesProduced < minCakes) minCakes = cakesProduced
        }
        return minCakes
    }

    fun calculateAvailableCakes(cake: Cake) {
        val avail = calculateAvailableCakesSync(cake)
        // Patch Firestore only when different
        if (cake.availableProduce != avail) {
            viewModelScope.launch {
                try {
                    repo.updateCakeAvailableProduce(cake.id, avail)
                } catch (e: Exception) {
                    Log.e("InventoryVM", "Failed to update availableProduce: ${e.message}")
                }
            }
        }
        availableCakesMap.getOrPut(cake.id) { MutableLiveData() }.value = avail // For Compose
    }

    private suspend fun deductIngredientStocks(ingredientId: String, amountToDeduct: Double) {
        val stocks = stocksMap[ingredientId]?.value
            ?.filter { !isStockExpired(it) && it.quantity > 0 }
            ?.sortedBy { it.expiryDate ?: "" }
            ?.toMutableList() ?: mutableListOf()
        var remaining = amountToDeduct
        for (stock in stocks) {
            if (remaining <= 0) break
            val stockQtyInGrams = when (stock.unit.trim().uppercase()) {
                "KG" -> stock.quantity * 1000.0
                else -> stock.quantity
            }
            val deduct = minOf(stockQtyInGrams, remaining)
            val newQty = stockQtyInGrams - deduct
            val newStockValue = when (stock.unit.trim().uppercase()) {
                "KG" -> newQty / 1000.0
                else -> newQty
            }
            repo.updateStockQuantity(ingredientId, stock.stockId, newStockValue)
            remaining -= deduct
        }
    }

    fun produceCake(cake: Cake, quantity: Int) {
        viewModelScope.launch {
            try {
                val newWhole = cake.wholeCakeQuantity + quantity
                val newSlice = cake.sliceQuantity + (quantity * 8)
                repo.updateCakeQuantities(cake.id, newWhole, newSlice)
                for ((ingredientId, qtyPerCake) in cake.ingredients) {
                    val totalNeeded = qtyPerCake * quantity
                    deductIngredientStocks(ingredientId, totalNeeded)
                }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to produce cake: ${e.message}"
            }
        }
    }

    fun addWastageRecord(ingredientId: String, wastage: Wastage) {
        viewModelScope.launch {
            try {
                repo.addWastageRecord(ingredientId, wastage)
            } catch (e: Exception) {
                _errorMessage.postValue("Failed to add wastage record: ${e.message}")
            }
        }
    }

    fun startListeningUserUsername(context: Context? = null) {
        // Remove previous listener if any
        userProfileListener?.remove()

        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        userProfileListener = FirebaseFirestore.getInstance()
            .collection("users")
            .document(uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w("InventoryVM", "User profile listen failed", error)
                    return@addSnapshotListener
                }
                val name = snapshot?.getString("username").orEmpty()
                if (name.isNotEmpty()) {
                    setLoggedInUsername(name)
                    // Persist as fallback for next app launch (optional)
                    if (context != null) {
                        viewModelScope.launch { saveUsername(context, name) }
                    }
                }
            }
    }


    fun clearError() {
        _errorMessage.value = null
    }

    override fun onCleared() {
        super.onCleared()
        repo.removeListeners()
        stockListeners.values.forEach { it?.remove() }
        stockListeners.clear()
        repo.removeWastageListeners()
        authListener?.let { FirebaseAuth.getInstance().removeAuthStateListener(it) }
        userProfileListener?.remove()
    }

    fun clearData() {
        _userProfile.value = UserProfileUi()
        userProfileListener?.remove()
        userProfileListener = null
        authListener?.let { FirebaseAuth.getInstance().removeAuthStateListener(it) }
        authListener = null

        _ingredients.value = emptyList()
        _cakes.value = emptyList()
        _expiringIngredients.value = emptyList()
        _sales.value = emptyList()
        _wastageRecords.value = emptyList()
        _errorMessage.value = null

        stocksMap.clear()
        availableCakesMap.clear()
        stockListeners.values.forEach { it?.remove() }
        stockListeners.clear()
        _cartItems.value = emptyList()
        originalQuantities.clear()
        repo.removeListeners()
        repo.removeWastageListeners()
    }




    private fun attachStockListeners(ingredientsList: List<Ingredient>) {
        // Remove old listeners no longer needed
        val ingredientIds = ingredientsList.map { it.id }.toSet()
        stockIngredientsListeners.keys.toList().forEach { ingredientId ->
            if (ingredientId !in ingredientIds) {
                stockIngredientsListeners[ingredientId]?.remove()
                stockIngredientsListeners.remove(ingredientId)
            }
        }
        // Attach new listeners as needed
        ingredientsList.forEach { ingredient ->
            if (!stockIngredientsListeners.containsKey(ingredient.id)) {
                val listener = repo.listenStocksForIngredient(ingredient.id) { stocks ->
                    // On any stock change: recalculate all cakes using this ingredient
                    _cakes.value?.forEach { cake ->
                        if (cake.ingredients.containsKey(ingredient.id)) {
                            calculateAvailableCakes(cake)
                        }
                    }
                }
                stockIngredientsListeners[ingredient.id] = listener as ListenerRegistration
            }
        }
    }

    fun addCakeToCart(cake: Cake, quantity: Int, isWholeCake: Boolean) {
        viewModelScope.launch {
            val cartList = _cartItems.value.toMutableList()
            var cartItem = cartList.find { it.cake.id == cake.id }
            if (cartItem == null) {
                cartItem = CartItem(cake, 0, 0)
                cartList.add(cartItem)
            }
            if (isWholeCake) {
                cartItem.wholeCakeQuantity += quantity
            } else {
                cartItem.sliceQuantity += quantity
            }
            _cartItems.value = cartList
        }
    }

    private fun updateCakeLocally(cakeId: String, whole: Int, slice: Int) {
        _cakes.value = _cakes.value?.map {
            if (it.id == cakeId) it.copy(
                wholeCakeQuantity = whole.coerceAtLeast(0),
                sliceQuantity = slice.coerceAtLeast(0)
            ) else it
        }
    }

    private suspend fun updateCakeQuantitiesRealtime(cake: Cake, cartItem: CartItem) {
        val (originalWhole, originalSlice) = originalQuantities[cake.id] ?: (
                (cake.wholeCakeQuantity to cake.sliceQuantity)
                )
        if (cartItem.wholeCakeQuantity > 0) {
            repo.updateCakeQuantities(
                cake.id,
                (originalWhole - cartItem.wholeCakeQuantity).coerceAtLeast(0),
                0
            )
        } else {
            repo.updateCakeQuantities(
                cake.id,
                0,
                (originalSlice - cartItem.sliceQuantity).coerceAtLeast(0)
            )
        }
    }


    fun fetchAllWastageRecords() {
        viewModelScope.launch {
            val user = FirebaseAuth.getInstance().currentUser
            if (user == null) return@launch
            val ings = repo.getIngredients()
            val allWastage = mutableListOf<Pair<String, Wastage>>()
            for (ingredient in ings) {
                val wastages = repo.getWastagesForIngredient(ingredient.id)
                wastages.forEach { allWastage.add(ingredient.id to it) }
            }
            _wastageRecords.postValue(allWastage)
        }
    }


    fun calculateCartTotal(cart: List<CartItem>): Double {
        var total = 0.0
        cart.forEach { item ->
            total += item.wholeCakeQuantity * item.cake.wholeCakePrice
            total += item.sliceQuantity * item.cake.sliceCakePrice
        }
        return total
    }

    fun rollbackCartChanges() {
        viewModelScope.launch {
            val updatedCakes = _cakes.value?.map {
                originalQuantities[it.id]?.let { (whole, slice) ->
                    it.copy(wholeCakeQuantity = whole, sliceQuantity = slice)
                } ?: it
            }
            _cakes.value = updatedCakes
            _cartItems.value = emptyList()
            originalQuantities.clear()
        }
    }

    private suspend fun updateCakeInventoryPhysics(cake: Cake, wholeCakeQty: Int, sliceQty: Int) {
        var wcq = cake.wholeCakeQuantity
        var scq = cake.sliceQuantity

        // Deduct whole cakes directly
        wcq -= wholeCakeQty

        // Deduct slices (convert whole cakes to slices as needed)
        var remainingSliceQty = sliceQty
        while (remainingSliceQty > 0) {
            if (scq > 0) {
                val take = minOf(remainingSliceQty, scq)
                scq -= take
                remainingSliceQty -= take
            } else if (wcq > 0) {
                wcq -= 1
                scq += 8
            } else {
                // Not enough inventory; this should be prevented earlier
                break
            }
        }

        // Save new inventory
        repo.updateCakeQuantities(cake.id, wcq, scq)
    }

    fun confirmSale(customerName: String, cart: List<CartItem>) {
        viewModelScope.launch {
            try {
                if (cart.isEmpty()) return@launch
                val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return@launch

                // For each item in the cart, use getAvailableWholeAndSlice as source of truth
                cart.forEach { cartItem ->
                    val cake = _cakes.value?.find { it.id == cartItem.cake.id }
                    if (cake != null) {
                        val (whole, slice) = getAvailableWholeAndSlice(cake, cartItem)
                        // This writes the quantities you visually displayed in SalesManagementScreen
                        repo.updateCakeQuantities(cake.id, whole, slice)
                    }
                }

                // Prepare sale record
                val soldItems = cart.map {
                    SoldCake(
                        cakeName = it.cake.type,
                        wholeCakeQty = it.wholeCakeQuantity,
                        sliceQty = it.sliceQuantity,
                        totalSale = it.wholeCakeQuantity * it.cake.wholeCakePrice +
                                it.sliceQuantity * it.cake.sliceCakePrice
                    )
                }
                val sale = Sale(
                    id = generateOrderId(),
                    custName = customerName,
                    items = soldItems,
                    date = LocalDateTime.now()
                        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
                )

                // Save sale under user's sales subcollection only
                repo.addSaleToUserSubcollection(uid, sale)

                _cartItems.value = emptyList()
            } catch (e: Exception) {
                _errorMessage.value =
                    "Failed to confirm sale: ${e.localizedMessage ?: e.toString()}"
            }
        }
    }


    private fun generateOrderId(): String {
        val randomNum = (1..999999).random()
        return "OID" + String.format("%06d", randomNum)
    }

    private fun postAvailable(cakeId: String, amount: Int) {
        // Suppress update if optimistic update pending (optional; based on your implementation)
        if (pendingAvailabilityUpdates.contains(cakeId)) return

        availableCakesMap.getOrPut(cakeId) { MutableLiveData() }.postValue(amount)
    }

    suspend fun addStock(ingredientId: String, quantity: Double, unit: String, expiryDate: String): Boolean {
        Log.d("AddStockVM", "Calling repo.addStock with: $ingredientId, qty=$quantity, unit=$unit, expiry=$expiryDate")
        return try {
            val stockId = getUniqueStockId(ingredientId)
            Log.d("AddStockVM", "Generated stockId: $stockId")
            val todayDate = LocalDate.now().toString()
            val qtyGram = if (unit == "KG") quantity * 1000 else quantity
            val qtyKg = if (unit == "GRAM") quantity / 1000 else quantity
            val stock = Stock(
                stockId = stockId,
                quantity = quantity,
                unit = unit,
                expiryDate = expiryDate,
                orderedDate = todayDate,
                orderedAmountGram = qtyGram,
                orderedAmountKg = qtyKg
            )
            repo.addStock(ingredientId, stock)
            Log.d("AddStockVM", "repo.addStock returned successfully.")
            true
        } catch (e: Exception) {
            Log.e("AddStockVM", "Failed to add stock. Exception: ${e.message}", e)
            _errorMessage.postValue(e.toString()) // Use e.toString() for full error type/message
            false
        }
    }






    fun updateStockExpiry(ingredientId: String, stockId: String, newExpiryDate: String) {
        viewModelScope.launch {
            try {
                repo.updateStockExpiry(ingredientId, stockId, newExpiryDate)
            } catch (e: Exception) {
                _errorMessage.postValue("Failed to update stock expiry: ${e.message}")
            }
        }
    }

    fun deleteStock(ingredientId: String, stockId: String) {
        viewModelScope.launch {
            try {
                repo.deleteStock(ingredientId, stockId)
            } catch (e: Exception) {
                _errorMessage.postValue("Failed to delete stock: ${e.message}")
            }
        }
    }

    private suspend fun getUniqueStockId(ingredientId: String): String {
        val userIdVal = FirebaseAuth.getInstance().currentUser?.uid ?: ""
        while (true) {
            val randomNum = (1..999999).random()
            val stockId = "CEL" + String.format("%06d", randomNum)
            val doc = FirebaseFirestore.getInstance()
                .collection("users")
                .document(userIdVal)
                .collection("ingredients")
                .document(ingredientId)
                .collection("stocks")
                .document(stockId)
                .get()
                .await()
            if (!doc.exists()) {
                return stockId
            }
        }
    }

    fun getAvailableWholeAndSlice(
        cake: Cake,
        cartItem: CartItem?
    ): Pair<Int, Int> {
        val originalTotalSlices = cake.sliceQuantity
        val cartSlices = (cartItem?.wholeCakeQuantity ?: 0) * 8 + (cartItem?.sliceQuantity ?: 0)
        val remainingSlices = (originalTotalSlices - cartSlices).coerceAtLeast(0)
        val displayWhole = remainingSlices / 8
        val displaySlice = remainingSlices
        return displayWhole to displaySlice
    }

    class Factory(
        private val inventoryRepository: InventoryRepository,
        private val expiryRepository: ExpiryRepository
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(InventoryViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return InventoryViewModel(inventoryRepository, expiryRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }


    suspend fun softDeleteAccount(): Boolean {
        val auth = FirebaseAuth.getInstance()
        val uid = auth.currentUser?.uid ?: return false
        return try {
            val db = FirebaseFirestore.getInstance()
            db.collection("users").document(uid)
                .set(
                    mapOf(
                        "status" to "deleted",
                        "deletedAt" to System.currentTimeMillis()
                    ),
                    SetOptions.merge()
                )
                .await()
            FirebaseAuth.getInstance().signOut()
            true
        } catch (e: Exception) {
            Log.e("InventoryVM", "Soft delete failed: ${e.message}", e)
            false
        }
    }

    suspend fun deleteUserAccountAndData(
        username: String,
        onProgressUpdate: (Float) -> Unit = {}
    ) {
        val auth = FirebaseAuth.getInstance()
        val db = FirebaseFirestore.getInstance()
        val uid = auth.currentUser?.uid ?: return

        fun log(msg: String) = Log.d("DeleteDebug", msg)

        // Collect all documents to delete
        val cakes = db.collection("users").document(uid).collection("cakes").get().await().documents
        val sales = db.collection("users").document(uid).collection("sales").get().await().documents
        val editRecords = db.collection("users").document(uid).collection("ingredient_edit_records").get().await().documents
        val ingredientDocs = db.collection("users").document(uid).collection("ingredients").get().await().documents
        val editRecords2 = db.collection("users").document(uid).collection("edit_records").get().await().documents
        val usernameDocRef = db.collection("users_usernames").document(username.lowercase())

        // Calculate total steps for progress tracking
        val totalSteps = cakes.size + sales.size + editRecords.size + ingredientDocs.size +
                ingredientDocs.map {
                    it.reference.collection("stocks").get().await().size() +
                            it.reference.collection("wastages").get().await().size()
                }.sum() +
                editRecords2.size + 2 // +2 for root user doc and username mapping

        var completedSteps = 0

        // Delete cakes
        for ((i, doc) in cakes.withIndex()) {
            log("Deleting cake (${i + 1}/${cakes.size}): ${doc.reference.path}")
            doc.reference.delete().await()
            completedSteps++
            onProgressUpdate(completedSteps.toFloat() / totalSteps.toFloat())
        }
        log("Finished deleting cakes")

        // Delete sales
        for ((i, doc) in sales.withIndex()) {
            log("Deleting sale (${i + 1}/${sales.size}): ${doc.reference.path}")
            doc.reference.delete().await()
            completedSteps++
            onProgressUpdate(completedSteps.toFloat() / totalSteps.toFloat())
        }
        log("Finished deleting sales")

        // Delete ingredient_edit_records
        for ((i, doc) in editRecords.withIndex()) {
            log("Deleting ingredient_edit_record (${i + 1}/${editRecords.size}): ${doc.reference.path}")
            doc.reference.delete().await()
            completedSteps++
            onProgressUpdate(completedSteps.toFloat() / totalSteps.toFloat())
        }
        log("Finished deleting ingredient_edit_records")

        // Delete ingredients and nested stocks/wastages
        for ((idx, ingredientDoc) in ingredientDocs.withIndex()) {
            log("Processing ingredient (${idx + 1}/${ingredientDocs.size}): ${ingredientDoc.reference.path}")

            val stocks = ingredientDoc.reference.collection("stocks").get().await().documents
            for ((j, stock) in stocks.withIndex()) {
                log("Deleting stock (${j + 1}/${stocks.size}): ${stock.reference.path}")
                stock.reference.delete().await()
                completedSteps++
                onProgressUpdate(completedSteps.toFloat() / totalSteps.toFloat())
            }
            log("Finished deleting stocks for ingredient ${ingredientDoc.id}")

            val wastages = ingredientDoc.reference.collection("wastages").get().await().documents
            for ((k, wastage) in wastages.withIndex()) {
                log("Deleting wastage (${k + 1}/${wastages.size}): ${wastage.reference.path}")
                wastage.reference.delete().await()
                completedSteps++
                onProgressUpdate(completedSteps.toFloat() / totalSteps.toFloat())
            }
            log("Finished deleting wastages for ingredient ${ingredientDoc.id}")

            log("Deleting ingredient: ${ingredientDoc.reference.path}")
            ingredientDoc.reference.delete().await()
            completedSteps++
            onProgressUpdate(completedSteps.toFloat() / totalSteps.toFloat())
        }
        log("Finished deleting all ingredients")

        // Delete edit_records
        for ((i, doc) in editRecords2.withIndex()) {
            log("Deleting edit_record (${i + 1}/${editRecords2.size}): ${doc.reference.path}")
            doc.reference.delete().await()
            completedSteps++
            onProgressUpdate(completedSteps.toFloat() / totalSteps.toFloat())
        }
        log("Finished deleting edit_records")

        // Delete root user document
        log("Deleting root user doc: users/$uid")
        db.collection("users").document(uid).delete().await()
        completedSteps++
        onProgressUpdate(completedSteps.toFloat() / totalSteps.toFloat())
        log("Finished deleting root user doc")

        // Delete username mapping
        val usernameDoc = usernameDocRef.get().await()
        if (usernameDoc.exists() && usernameDoc.getString("uid") == uid) {
            log("Deleting username mapping: users_usernames/${username.lowercase()}")
            usernameDocRef.delete().await()
            completedSteps++
            onProgressUpdate(completedSteps.toFloat() / totalSteps.toFloat())
            log("Finished deleting username mapping")
        } else {
            log("Username mapping missing or UID mismatch for $username")
        }

        try {
            auth.currentUser?.delete()?.await()
        } catch (e: FirebaseAuthRecentLoginRequiredException) {
            throw e // caller handle reauth logic
        }
    }


    suspend fun reauthenticateUser(email: String, password: String): Boolean {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return false
        val credential = EmailAuthProvider.getCredential(email, password)
        return try {
            currentUser.reauthenticate(credential).await()
            true
        } catch (e: Exception) {
            false
        }
    }





    private suspend fun deleteUserDataInFirestoreOnly() {
        val auth = FirebaseAuth.getInstance()
        val uid = auth.currentUser?.uid ?: return
        val db = FirebaseFirestore.getInstance()
        val userDoc = db.collection("users").document(uid)

        val userCollections = listOf("cakes", "ingredients", "sales")
        for (col in userCollections) {
            val docs = userDoc.collection(col).get().await().documents
            for (doc in docs) {
                if (col == "ingredients") {
                    val stocks = doc.reference.collection("stocks").get().await().documents
                    for (s in stocks) s.reference.delete().await()
                    val wastages = doc.reference.collection("wastages").get().await().documents
                    for (w in wastages) w.reference.delete().await()
                }
                doc.reference.delete().await()
            }
        }

        // Delete root user document
        userDoc.delete().await()

        // Remove username index if present
        try {
            val username = FirebaseAuth.getInstance().currentUser?.displayName?.lowercase()
            if (username != null) {
                FirebaseFirestore.getInstance()
                    .collection("users_usernames")
                    .document(username)
                    .delete()
                    .await()
            }
        } catch (_: Exception) {
        }
    }


    suspend fun deleteAccountWithReauth(email: String?, password: String?): Boolean {
        val auth = FirebaseAuth.getInstance()
        val user = auth.currentUser ?: return false
        return try {
            // delete Firestore data, then delete Auth user
            deleteUserDataInFirestoreOnly()
            user.delete().await()
            true
        } catch (e: FirebaseAuthRecentLoginRequiredException) {
            if (email.isNullOrBlank() || password.isNullOrBlank()) {
                Log.e("InventoryVM", "Recent login required but no credentials provided")
                return false
            }
            val ok = reauthenticateWithEmail(email, password)
            if (!ok) return false
            try {
                deleteUserDataInFirestoreOnly()
                user.delete().await()
                true
            } catch (inner: Exception) {
                Log.e("InventoryVM", "Delete retry failed: ${inner.message}", inner)
                false
            }
        } catch (e: Exception) {
            Log.e("InventoryVM", "Delete failed: ${e.message}", e)
            false
        }
    }

    suspend fun deleteUserAccount(email: String?, password: String?): Boolean {
        // Fallback email if profile not populated
        val effectiveEmail = if (!email.isNullOrBlank()) email
        else FirebaseAuth.getInstance().currentUser?.email
        return deleteAccountWithReauth(effectiveEmail, password)
    }

    suspend fun reauthenticateWithEmail(email: String, password: String): Boolean {
        val user = FirebaseAuth.getInstance().currentUser ?: return false
        val credential = EmailAuthProvider.getCredential(email, password)
        return try {
            user.reauthenticate(credential).await()
            true
        } catch (e: Exception) {
            Log.e("InventoryVM", "Reauth failed: ${e.message}", e)
            false
        }
    }

    fun reauthenticateUser(
        email: String,
        password: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null || email.isEmpty()) {
            onError("Current user email not available")
            return
        }

        val credential = EmailAuthProvider.getCredential(email, password)
        viewModelScope.launch {
            try {
                currentUser.reauthenticate(credential).await()
                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "Reauthentication failed")
            }
        }
    }


    fun deleteAccountAndDataAsync(
        username: String,
        email: String?,
        password: String?,
        onSuccess: () -> Unit,
        onReauthRequired: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                deleteUserAccountAndData(username)
                onSuccess()
            } catch (ex: Exception) {
                if (ex is FirebaseAuthRecentLoginRequiredException) {
                    // Notify UI to request recent login (reauthentication)
                    onReauthRequired()
                } else {
                    onError(ex.message ?: "Unknown error")
                }
            }
        }
    }


    // Deletion progress state exposed to UI
    data class DeletionUiState(
        val running: Boolean = false,
        val percent: Int = 0,
        val message: String = ""
    )

    private val _deletionState = MutableStateFlow(DeletionUiState())
    val deletionState: StateFlow<DeletionUiState> = _deletionState

    private fun setDeletionProgress(percent: Int, message: String) {
        _deletionState.value = DeletionUiState(running = true, percent = percent, message = message)
    }

    // Delete a subcollection under users/{uid}/{subCollectionName} with batched iteration
    private suspend fun deleteUserSubcollection(subCollectionName: String) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val db = FirebaseFirestore.getInstance()
        val parent = db.collection("users").document(uid)
        val colRef = parent.collection(subCollectionName)

        // Page through documents to avoid loading too many
        while (true) {
            val batchDocs = colRef.limit(200).get().await().documents
            if (batchDocs.isEmpty()) break

            // If this subcollection could also have its own nested subcollections, delete them here similarly.
            for (doc in batchDocs) {
                // Example: if subCollectionName == "ingredients", clean nested "stocks" and "wastages"
                if (subCollectionName == "ingredients") {
                    val stocks = doc.reference.collection("stocks").get().await().documents
                    for (s in stocks) s.reference.delete().await()
                    val wastages = doc.reference.collection("wastages").get().await().documents
                    for (w in wastages) w.reference.delete().await()
                }
            }
            // Delete the documents after nested cleanup
            for (doc in batchDocs) {
                doc.reference.delete().await()
            }
        }
    }

    private suspend fun deleteUserRootAndUsernameIndex() {
        val auth = FirebaseAuth.getInstance()
        val uid = auth.currentUser?.uid ?: return
        val db = FirebaseFirestore.getInstance()
        val userDoc = db.collection("users").document(uid)

        // Delete user root document
        userDoc.delete().await()

        // Try to fetch the username from profile document first
        try {
            val profileSnap = userDoc.get().await()
            val username = (profileSnap.getString("username") ?: auth.currentUser?.displayName)?.lowercase()
            if (!username.isNullOrBlank()) {
                db.collection("users_usernames").document(username).delete().await()
            }
        } catch (_: Exception) {
            // Ignore cleanup errors
        }
    }


    suspend fun isUsernameTaken(username: String): Boolean {
        val uid = FirebaseAuth.getInstance().currentUser?.uid // not needed but ok
        val snap = FirebaseFirestore.getInstance()
            .collection("users")
            .whereEqualTo("username", username.lowercase())
            .limit(1)
            .get()
            .await()
        return !snap.isEmpty
    }

    suspend fun isEmailTakenInUsers(email: String): Boolean {
        val snap = FirebaseFirestore.getInstance()
            .collection("users")
            .whereEqualTo("email", email.lowercase())
            .limit(1)
            .get()
            .await()
        return !snap.isEmpty
    }

    suspend fun deleteCurrentUserData() {
        val auth = FirebaseAuth.getInstance()
        val uid = auth.currentUser?.uid ?: return
        val db = FirebaseFirestore.getInstance()
        val userDoc = db.collection("users").document(uid)

        // 1. Delete all known subcollections ('cakes', 'ingredients', etc.)
        val userCollections = listOf("cakes", "ingredients", "sales")
        for (col in userCollections) {
            val docs = userDoc.collection(col).get().await().documents
            for (doc in docs) {
                if (col == "ingredients") {
                    val stocks = doc.reference.collection("stocks").get().await().documents
                    for (stock in stocks) stock.reference.delete().await()
                    val wastages = doc.reference.collection("wastages").get().await().documents
                    for (w in wastages) w.reference.delete().await()
                }
                doc.reference.delete().await()
            }
        }

        // 2. Fetch profile to get username
        val profileSnap = userDoc.get().await()
        val username = (profileSnap.getString("username") ?: auth.currentUser?.displayName)?.lowercase()

        // 3. Delete the root user document
        userDoc.delete().await()

        // 4. Delete the username index entry if known
        if (!username.isNullOrEmpty()) {
            db.collection("users_usernames").document(username).delete().await()
        }

        // 5. Delete Auth user
        auth.currentUser?.delete()?.await()
    }

    private val _addSuccess = MutableLiveData<Boolean>()
    val addSuccess: LiveData<Boolean> = _addSuccess

    suspend fun uploadImageToFirebaseStorage(imageBytes: ByteArray, folder: String): String? {
        val user = FirebaseAuth.getInstance().currentUser ?: throw Exception("User not logged in")
        if (user == null) throw Exception("Not logged in") // or handle gracefully
// Always log the filename to debug
        if (imageBytes.isEmpty()) throw Exception("Image bytes are empty")
        val filename = "$folder/${UUID.randomUUID()}.png"
        Log.d("UPLOAD", "Uploading to Storage: $filename")
        val storageRef = FirebaseStorage.getInstance().reference.child(filename)
        storageRef.putBytes(imageBytes).await()
        return storageRef.downloadUrl.await().toString()
    }

    fun addCakeToFirestore(context: Context, cake: Cake, imageBytes: ByteArray?) {
        viewModelScope.launch {
            try {
                val imageUrl = if (imageBytes != null && imageBytes.isNotEmpty()) {
                    uploadImageToSupabase(imageBytes, "cakes")
                } else null

                if (imageBytes != null && imageBytes.isNotEmpty() && imageUrl == null) {
                    throw Exception("Image upload failed—no URL returned")
                }

                val db = FirebaseFirestore.getInstance()
                val userId = FirebaseAuth.getInstance().currentUser?.uid ?: throw Exception("Not logged in")
                val cakeId = db.collection("users").document(userId).collection("cakes").document().id
                val cakeMap = hashMapOf(
                    "id" to cakeId,
                    "type" to cake.type,
                    "wholeCakeQuantity" to 0,
                    "sliceQuantity" to 0,
                    "ingredients" to (if (cake.ingredients.isNotEmpty()) cake.ingredients else emptyMap<String, Double>()),
                    "wholeCakePrice" to cake.wholeCakePrice,
                    "sliceCakePrice" to cake.sliceCakePrice,
                    "availableProduce" to 0,
                    "imageUrl" to imageUrl
                )
                db.collection("users").document(userId)
                    .collection("cakes").document(cakeId)
                    .set(cakeMap, SetOptions.merge()).await()
                _addSuccess.postValue(true)
            } catch (e: Exception) {
                Log.e("InventoryVM", "Error adding cake: ${e.message}", e)
                _errorMessage.postValue("Failed to add cake: ${e.message}")
                _addSuccess.postValue(false)
            }
        }
    }


    fun addIngredientToFirestore(context: Context, ingredient: Ingredient, imageBytes: ByteArray?) {
        viewModelScope.launch {
            try {
                val imageUrl = if (imageBytes != null && imageBytes.isNotEmpty()) {
                    uploadImageToSupabase(imageBytes, "ingredients")
                } else null

                if (imageBytes != null && imageBytes.isNotEmpty() && imageUrl == null) {
                    throw Exception("Image upload failed—no URL returned")
                }

                val db = FirebaseFirestore.getInstance()
                val userId = FirebaseAuth.getInstance().currentUser?.uid ?: throw Exception("Not logged in")
                val ingredientId = db.collection("users").document(userId)
                    .collection("ingredients").document().id
                val ingredientMap = hashMapOf(
                    "id" to ingredientId,
                    "name" to ingredient.name,
                    "quantity" to ingredient.quantity,
                    "unit" to ingredient.unit,
                    "expiryDate" to ingredient.expiryDate,
                    "disabled" to ingredient.disabled,
                    "imageUrl" to imageUrl
                )
                db.collection("users").document(userId)
                    .collection("ingredients").document(ingredientId)
                    .set(ingredientMap, SetOptions.merge()).await()
                _addSuccess.postValue(true)
            } catch (e: Exception) {
                Log.e("InventoryVM", "Error adding ingredient: ${e.message}", e)
                _errorMessage.postValue("Failed to add ingredient: ${e.message}")
                _addSuccess.postValue(false)
            }
        }
    }

    fun clearSuccess() { _addSuccess.value = null }

    fun addIngredientToFirestore(context: Context, ingredient: Ingredient, imageUri: Uri?) {
        viewModelScope.launch {
            try {
                val db = FirebaseFirestore.getInstance()
                val userId = FirebaseAuth.getInstance().currentUser?.uid ?: throw Exception("Not logged in")
                val ingredientId = db.collection("users").document(userId).collection("ingredients").document().id
                var imageUrl: String? = null
                if (imageUri != null) {
                    val fileRef = FirebaseStorage.getInstance().reference.child("ingredients/${UUID.randomUUID()}.png")
                    val inputStream = context.contentResolver.openInputStream(imageUri)
                    val bytes = inputStream?.readBytes()
                    inputStream?.close()
                    if (bytes == null || bytes.isEmpty()) throw Exception("Cannot read image data.")
                    fileRef.putBytes(bytes).await()
                    imageUrl = fileRef.downloadUrl.await().toString()
                }
                val ingredientMap = hashMapOf(
                    "id" to ingredientId,
                    "name" to ingredient.name,
                    "quantity" to ingredient.quantity,
                    "unit" to ingredient.unit,
                    "expiryDate" to ingredient.expiryDate,
                    "disabled" to ingredient.disabled,
                    "imageUrl" to ingredient.imageUrl
                )
                db.collection("users").document(userId).collection("ingredients").document(ingredientId)
                    .set(ingredientMap, SetOptions.merge()).await()
                // Can set _addSuccess.postValue(true) here if you use LiveData feedback
            } catch (e: Exception) {
                Log.e("InventoryVM", "Error adding ingredient: ${e.message}", e)
                _errorMessage.postValue("Failed to add ingredient: ${e.message}")
            }
        }
    }


    suspend fun deleteCurrentUserDataAndUsername() {
        val auth = FirebaseAuth.getInstance()
        val uid = auth.currentUser?.uid ?: return
        val db = FirebaseFirestore.getInstance()
        val userDoc = db.collection("users").document(uid)
        // --- Fetch username from profile first ---
        val profileSnapshot = userDoc.get().await()
        val username = (profileSnapshot.getString("username") ?: auth.currentUser?.displayName)?.lowercase()
        // --- Delete subcollections ---
        val userCollections = listOf("cakes", "ingredients", "sales")
        for (col in userCollections) {
            val docs = userDoc.collection(col).get().await().documents
            for (doc in docs) {
                if (col == "ingredients") {
                    val stocks = doc.reference.collection("stocks").get().await().documents
                    for (stock in stocks) stock.reference.delete().await()
                    val wastages = doc.reference.collection("wastages").get().await().documents
                    for (w in wastages) w.reference.delete().await()
                }
                doc.reference.delete().await()
            }
        }
        // --- Delete user doc ---
        userDoc.delete().await()
        // --- Delete username index ---
        if (!username.isNullOrBlank()) {
            db.collection("users_usernames").document(username).delete().await()
        }
        // --- Delete the Auth user ---
        auth.currentUser?.delete()?.await()
    }

    // Add inside InventoryViewModel class

    fun removeCake(cakeId: String) {
        viewModelScope.launch {
            try {
                repo.cakesCollection()?.document(cakeId)?.delete()
            } catch (e: Exception) {
                _errorMessage.postValue("Failed to remove cake: ${e.message}")
            }
        }
    }

    fun removeIngredient(ingredientId: String) {
        viewModelScope.launch {
            try {
                // Delete all stocks and wastages subcollections first
                val stockDocs = repo.ingredientsCollection()
                    ?.document(ingredientId)
                    ?.collection("stocks")
                    ?.get()
                    ?.await()
                    ?.documents
                stockDocs?.forEach { it.reference.delete() }
                val wastageDocs = repo.ingredientsCollection()
                    ?.document(ingredientId)
                    ?.collection("wastages")
                    ?.get()
                    ?.await()
                    ?.documents
                wastageDocs?.forEach { it.reference.delete() }
                repo.ingredientsCollection()?.document(ingredientId)?.delete()
            } catch (e: Exception) {
                _errorMessage.postValue("Failed to remove ingredient: ${e.message}")
            }
        }
    }


    suspend fun uploadImageToSupabase(imageBytes: ByteArray, folder: String): String? = withContext(Dispatchers.IO) {
        val fileName = "${System.currentTimeMillis()}.jpg"
        val path = "$folder/$fileName"
        val mediaType = "image/jpeg".toMediaTypeOrNull()
        val requestBody = imageBytes.toRequestBody(mediaType)
        val response = try {
            supabaseApi.uploadImage("images", path, requestBody)
        } catch (e: Exception) {
            Log.e("SupabaseUpload", "Exception: ${e.message}", e)
            null
        }
        if (response == null) {
            Log.e("SupabaseUpload", "No HTTP response from Supabase")
            return@withContext null
        }
        if (!response.isSuccessful) {
            val errorBody = response.errorBody()?.string() ?: ""
            Log.e("SupabaseUpload", "Upload failed, code=${response.code()}, message=${response.message()}, error=$errorBody")
            return@withContext null
        }
        "https://ektazzpdecrzxkinmbsf.supabase.co/storage/v1/object/public/images/$folder/$fileName"
    }

    // Before, if parameter could be nullable:
    fun updateStockForIngredient(ingredientId: String, updatedStock: Stock) {
        viewModelScope.launch {
            repo.updateStockQuantity(ingredientId, updatedStock.stockId, updatedStock.quantity)
            repo.updateStockExpiry(ingredientId, updatedStock.stockId,
                updatedStock.expiryDate.toString()
            )
            // Handle other fields if needed
        }
    }





    suspend fun uploadImageUriToSupabase(context: Context, imageUri: Uri, folder: String): String? {
        return withContext(Dispatchers.IO) {
            val inputStream = context.contentResolver.openInputStream(imageUri)
            val bytes = inputStream?.readBytes()
            inputStream?.close()
            if (bytes == null || bytes.isEmpty()) {
                Log.e("UploadHelper", "Cannot read image data from Uri")
                null
            } else {
                uploadImageToSupabase(bytes, folder)
            }
        }
    }

    fun updateCakeInFirestore(cake: Cake, imageUri: Uri?, context: Context) {
        viewModelScope.launch {
            try {
                val userId = FirebaseAuth.getInstance().currentUser?.uid ?: throw Exception("Not logged in")
                val db = FirebaseFirestore.getInstance()

                var imageUrl = cake.imageUrl
                // Handle image upload if new image selected
                if (imageUri != null) {
                    val uploadedUrl = uploadImageUriToSupabase(context, imageUri, "cakes")
                    if (uploadedUrl != null) {
                        imageUrl = uploadedUrl
                    }
                }
                // Prepare cake data map to overwrite Firestore document
                val cakeData = hashMapOf(
                    "type" to cake.type,
                    "ingredients" to cake.ingredients, // updated ingredients map
                    "wholeCakePrice" to cake.wholeCakePrice,
                    "sliceCakePrice" to cake.sliceCakePrice,
                    "imageUrl" to imageUrl
                )
                // Use set() without merge to fully replace cake document including ingredients
                db.collection("users")
                    .document(userId)
                    .collection("cakes")
                    .document(cake.id) // Use existing cake id to update
                    .set(cakeData)
                    .await()

                // Additional success handling if needed
            } catch (e: Exception) {
                Log.e("InventoryVM", "Update cake failed", e)
                // Handle error reporting (e.g., to UI)
            }
        }
    }



    // Updated function to update Ingredient including image upload with Uri
    fun updateIngredientInFirestore(ingredient: Ingredient, imageUri: Uri?, context: Context) {
        viewModelScope.launch {
            try {
                val userId = FirebaseAuth.getInstance().currentUser?.uid ?: throw Exception("Not logged in")
                val db = FirebaseFirestore.getInstance()
                var imageUrl = ingredient.imageUrl
                if (imageUri != null) {
                    val uploadedUrl = uploadImageUriToSupabase(context, imageUri, "ingredients")
                    if (uploadedUrl != null) imageUrl = uploadedUrl
                }
                val ingredientData = hashMapOf(
                    "name" to ingredient.name,
                    "quantity" to ingredient.quantity,
                    "unit" to ingredient.unit,
                    "expiryDate" to ingredient.expiryDate,
                    "disabled" to ingredient.disabled,
                    "imageUrl" to imageUrl
                )
                db.collection("users").document(userId)
                    .collection("ingredients")
                    .document(ingredient.id)
                    .set(ingredientData, SetOptions.merge())
                    .await()
                // handle success if needed
            } catch (e: Exception) {
                Log.e("InventoryVM", "Update ingredient failed: ${e.message}", e)
                // handle error if needed
            }
        }
    }
}