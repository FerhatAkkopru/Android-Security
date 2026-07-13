package com.example.mockbankandroidgood

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.example.mockbankandroidgood.ui.theme.MockBankAndroidGOODTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : ComponentActivity() {

    companion object {
        private const val BASE_URL = "http://10.0.2.2:5003"
        private const val SECURE_PREFS_NAME = "secure_auth_prefs"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MockBankAndroidGOODTheme {
                LoginScreen(context = this)
            }
        }
    }

    @Composable
    fun LoginScreen(context: Context) {
        var username by remember { mutableStateOf("intern") }
        var password by remember { mutableStateOf("123456") }
        var resultText by remember { mutableStateOf("Not logged in") }

        val scope = rememberCoroutineScope()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Mock Bank v2 - Hardened Login",
                style = MaterialTheme.typography.headlineSmall
            )

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("Username") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    scope.launch {
                        resultText = "Logging in..."

                        val result = loginAndStoreTokensSecurely(
                            context = context,
                            username = username,
                            password = password
                        )

                        resultText = result
                    }
                }
            ) {
                Text("Login")
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(text = resultText)
        }
    }

    private suspend fun loginAndStoreTokensSecurely(
        context: Context,
        username: String,
        password: String
    ): String {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL("$BASE_URL/login")
                val connection = url.openConnection() as HttpURLConnection

                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.doOutput = true

                val requestJson = JSONObject()
                requestJson.put("username", username)
                requestJson.put("password", password)

                OutputStreamWriter(connection.outputStream).use { writer ->
                    writer.write(requestJson.toString())
                    writer.flush()
                }

                val statusCode = connection.responseCode

                if (statusCode != 200) {
                    return@withContext "Login failed. HTTP $statusCode"
                }

                val responseBody = connection.inputStream.bufferedReader().use { it.readText() }
                val responseJson = JSONObject(responseBody)

                val accessToken = responseJson.getString("accessToken")
                val refreshToken = responseJson.getString("refreshToken")
                val expiresIn = responseJson.getInt("expiresIn")

                val masterKey = MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build()

                val securePrefs = EncryptedSharedPreferences.create(
                    context,
                    SECURE_PREFS_NAME,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                )

                securePrefs.edit()
                    .putString("access_token", accessToken)
                    .putString("refresh_token", refreshToken)
                    .putInt("expires_in", expiresIn)
                    .putString("username", username)
                    .apply()

                """
                Login success.

                accessToken saved to EncryptedSharedPreferences.
                refreshToken saved to EncryptedSharedPreferences.
                password was NOT persisted.

                This is hardened v2 storage behavior.
                """.trimIndent()
            } catch (e: Exception) {
                "Error: ${e.message}"
            }
        }
    }
}