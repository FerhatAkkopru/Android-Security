package com.example.mockbankandroidbad

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.mockbankandroidbad.ui.theme.MockBankAndroidBADTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST

// --- Veri Modelleri ---
data class LoginRequest(val username: String, val password: String)
data class LoginResponse(val accessToken: String, val refreshToken: String, val expiresIn: Int)

// --- API Arayüzü ---
interface MockBankApiService {
    @POST("/login")
    suspend fun login(@Body request: LoginRequest): LoginResponse
}

class MainActivity : ComponentActivity() {

    companion object {
        private const val BASE_URL = "http://10.0.2.2:5003"
        private const val PREFS_NAME = "auth_prefs"
    }

    // --- Zafiyetli Ağ İstemcisi Kurulumu ---
    private val apiService: MockBankApiService by lazy {
        // ZAFİYET: Log seviyesi BODY. Tüm request ve response'lar Logcat'e sızar.
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .build()

        // ZAFİYET: HTTP üzerinden cleartext haberleşme.
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        retrofit.create(MockBankApiService::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MockBankAndroidBADTheme {
                LoginScreen(context = this)
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

            Row(verticalAlignment = Alignment.CenterVertically) {
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
                        resultText = loginAndStoreTokens(
                            context = context,
                            username = username,
                            password = password,
                            rememberMe = rememberMe
                        )
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
                // Retrofit üzerinden istek atılır
                val request = LoginRequest(username, password)
                val response = apiService.login(request)

                // v1-vulnerable: Token'lar düz SharedPreferences içine plaintext yazılıyor.
                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

                prefs.edit()
                    .putString("access_token", response.accessToken)
                    .putString("refresh_token", response.refreshToken)
                    .putInt("expires_in", response.expiresIn)
                    .putString("username", username)
                    .apply()

                // v1-vulnerable: Remember me açıksa password bile plaintext saklanıyor.
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