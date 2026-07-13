package com.example.mockbankandroidbad

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.mockbankandroidbad.ui.theme.MockBankAndroidBADTheme
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
        private const val PREFS_NAME = "auth_prefs"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MockBankAndroidBADTheme {
                LoginScreen(
                    context = this
                )
            }
        }
    }

    @Composable
    fun LoginScreen(context: Context) {
        var username by remember { mutableStateOf("intern") }
        var password by remember { mutableStateOf("123456") }
        var rememberMe by remember { mutableStateOf(true) }
        var resultText by remember { mutableStateOf("Not logged in") }

        val scope = rememberCoroutineScope()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Mock Bank v1 - Vulnerable Login",
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

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = rememberMe,
                    onCheckedChange = { rememberMe = it }
                )

                Text(text = "Remember me - insecure in v1")
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    scope.launch {
                        resultText = "Logging in..."

                        val result = loginAndStoreTokens(
                            context = context,
                            username = username,
                            password = password,
                            rememberMe = rememberMe
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

    private suspend fun loginAndStoreTokens(
        context: Context,
        username: String,
        password: String,
        rememberMe: Boolean
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

                // v1-vulnerable:
                // Token'lar düz SharedPreferences içine plaintext yazılıyor.
                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

                prefs.edit()
                    .putString("access_token", accessToken)
                    .putString("refresh_token", refreshToken)
                    .putInt("expires_in", expiresIn)
                    .putString("username", username)
                    .apply()

                // v1-vulnerable:
                // Remember me açıksa password bile plaintext saklanıyor.
                if (rememberMe) {
                    prefs.edit()
                        .putString("remembered_password", password)
                        .apply()
                }

                """
                Login success.
                
                accessToken saved to SharedPreferences.
                refreshToken saved to SharedPreferences.
                rememberMe: $rememberMe
                
                This is intentionally insecure v1 behavior.
                """.trimIndent()
            } catch (e: Exception) {
                "Error: ${e.message}"
            }
        }
    }
}