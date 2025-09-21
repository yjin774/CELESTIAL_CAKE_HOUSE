package com.example.celestial.data

import android.content.Context
import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.tasks.await
import java.util.regex.Pattern

data class ParsedStockInfo(
    val amount: Double?,
    val unit: String,      // "KG" or "GRAM"
    val expiry: String?,
    val amountOptions: List<Pair<String, String>>, // raw pairs for user correction
    val expiryOptions: List<String>
)

object OCRUtils {
    suspend fun recognizeTextFromBitmap(context: Context, bitmap: Bitmap): String {
        val image = InputImage.fromBitmap(bitmap, 0)
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        val result = recognizer.process(image).await()
        return result.text
    }
    fun parseStockInfo(ocrText: String): ParsedStockInfo {
        val lower = ocrText.lowercase().replace(",", ".")
        val lines = lower.lines().map { it.trim() }
        // Amount patterns
        val amountPatterns = listOf(
            Pattern.compile("(\\d+[.]?\\d*)\\s*(kg|g|gram|grams|ml|lt|pcs)"),
            Pattern.compile("(net\\s*weight|qty|quantity)[ :]*(\\d+[.]?\\d*)\\s*(kg|g|gram|grams|ml|lt|pcs)")
        )
        // Date patterns
        val datePatterns = listOf(
            Pattern.compile("([12]\\d{3}[-/.][01]?\\d[-/.]\\d{2,4})"),          // yyyy-MM-dd or yyyy/MM/dd
            Pattern.compile("(\\d{2,4}[-/.]\\d{2}[-/.][12]\\d{3})"),            // dd-MM-yyyy or dd/MM/yyyy
            Pattern.compile("(\\d{1,2}\\s+\\w{3,9}\\s+[12]\\d{3})")             // 09 Sept 2025, etc
        )

        val amountOptions = mutableListOf<Pair<String, String>>() // value, unit
        for (p in amountPatterns) {
            val m = p.matcher(lower)
            while (m.find()) {
                val amt = m.group(1) ?: m.group(3)
                val unt = m.group(2) ?: m.group(4)
                if (amt != null && unt != null) {
                    amountOptions.add(Pair(amt, unt))
                }
            }
        }
        // Pick first, or null if not found
        val (amountFirst, unitFirst) = amountOptions.firstOrNull() ?: Pair(null, null)
        val amount = amountFirst?.toDoubleOrNull()
        val unit = when {
            unitFirst?.startsWith("kg") == true -> "KG"
            unitFirst?.startsWith("g") == true -> "GRAM"
            else -> "GRAM"
        }

        val expiryOptions = mutableListOf<String>()
        for (pt in datePatterns) {
            val m = pt.matcher(lower)
            while (m.find()) {
                expiryOptions.add(m.group(1))
            }
        }
        // Normalize expiry (use yyyy-MM-dd if possible)
        val expiry = expiryOptions.firstOrNull()?.replace("/", "-")?.replace(".", "-")

        return ParsedStockInfo(amount, unit, expiry, amountOptions, expiryOptions)
    }
}
