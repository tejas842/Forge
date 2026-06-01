package com.example.data.remote

import android.content.Context
import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class GeminiService(private val context: Context) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun generateContent(prompt: String, contextFiles: List<String> = emptyList()): String = withContext(Dispatchers.IO) {
        val sharedPrefs = context.getSharedPreferences("forge_settings", Context.MODE_PRIVATE)
        val savedApiKey = sharedPrefs.getString("api_key", null)
        
        val apiKey = if (!savedApiKey.isNullOrBlank()) savedApiKey else BuildConfig.GEMINI_API_KEY
        
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext "Error: Gemini API key is missing or invalid. Please set it in Settings."
        }

        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=\$apiKey"
        
        // Build JSON request manually using org.json
        val requestBodyJson = JSONObject()
        val contentsArray = JSONArray()
        
        val partsArray = JSONArray()
        
        // Add context
        if (contextFiles.isNotEmpty()) {
            val contextText = contextFiles.joinToString("\n\n")
            partsArray.put(JSONObject().put("text", "Context files:\n\$contextText\n\n"))
        }
        
        // Add prompt
        partsArray.put(JSONObject().put("text", prompt))
        
        val contentObj = JSONObject().put("parts", partsArray)
        contentsArray.put(contentObj)
        
        requestBodyJson.put("contents", contentsArray)

        val requestBody = requestBodyJson.toString().toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        try {
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()
            if (response.isSuccessful && responseBody != null) {
                val jsonResponse = JSONObject(responseBody)
                val candidates = jsonResponse.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val firstCandidate = candidates.getJSONObject(0)
                    val content = firstCandidate.optJSONObject("content")
                    val parts = content?.optJSONArray("parts")
                    if (parts != null && parts.length() > 0) {
                        return@withContext parts.getJSONObject(0).optString("text", "")
                    }
                }
                return@withContext "No response text found."
            } else {
                Log.e("GeminiService", "Error Response: \$responseBody")
                return@withContext "Error: \${response.code} \${response.message}"
            }
        } catch (e: Exception) {
            Log.e("GeminiService", "Exception", e)
            return@withContext "Exception: \${e.message}"
        }
    }
}
