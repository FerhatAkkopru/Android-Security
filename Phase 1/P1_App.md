# P1 — Authentication Implementation & Local Storage Attack Demo

## Amaç

Bu bölümde Phase 1 Authentication kısmında öğrendiğim token storage konusunu pratikte test ettim.

Önce bilerek güvensiz bir Android uygulaması oluşturdum. Bu uygulamada login sonrası gelen `accessToken`, `refreshToken` ve “remember me” açıkken password değerini düz `SharedPreferences` içine kaydettim.

Daha sonra aynı login akışını daha güvenli bir Android uygulamasında yaptım. Bu sefer token’ları `EncryptedSharedPreferences` içine kaydettim ve password’ü hiçbir şekilde persist etmedim.

Son olarak rootlu emulator üzerinden iki uygulamanın `/data/data/<package>/shared_prefs/` klasörlerini okuyarak farkı gözlemledim.

---

## Öncesinde Ne Hazırladım?

Bu çalışmadan önce basit bir `.NET` backend oluşturdum. Backend’in amacı Android uygulamasına login olabileceği ve token alabileceği küçük bir API sağlamaktı.

Backend endpointleri:

```text
POST /login
GET /balance
POST /refresh
POST /logout
```

Backend localde şu adreste çalışıyordu:

```text
http://localhost:5003
```

Android emulator içinden bilgisayarın localhost’una erişmek için şu adresi kullandım:

```text
http://10.0.2.2:5003
```

Çünkü emulator içinde `localhost`, bilgisayarı değil emulator’ın kendisini ifade eder.

# 0. Backend Hazırlığı — MockBankApi (.NET)

## Amaç

Android tarafındaki v1/v2 token storage testlerini yapabilmek için önce basit bir backend oluşturdum.

Bu backend’in amacı gerçek bir bankacılık sistemi yazmak değil, Phase 1 Authentication akışını test edebileceğim küçük bir API sağlamaktı.

Backend ile şu akışları test ettim:

```text
Kullanıcı login olur.
Backend access token ve refresh token üretir.
Client access token ile korumalı endpoint'e istek atar.
Client refresh token ile yeni access token alır.
Logout sırasında refresh token server tarafında geçersiz kılınır.

Bu sayede Android uygulamasında token’ın nasıl alındığını, nerede saklandığını ve çalınırsa ne işe yarayabileceğini gösterebildim.

0.1 .NET Web Projesi Oluşturma

Terminalde staj projemin ana klasöründe yeni bir backend klasörü oluşturdum:

mkdir MockBankApi
cd MockBankApi
dotnet new web

Bu komut boş bir ASP.NET Core projesi oluşturdu.

Daha sonra backend’i çalıştırdım:

dotnet run

Backend şu adreste ayağa kalktı:

http://localhost:5003

Terminalde şu çıktıyı gördüm:

Now listening on: http://localhost:5003

Bu benim local backend’imin çalıştığını gösterdi.

0.2 Neden Android’de localhost Değil 10.0.2.2 Kullandım?

Backend bilgisayarımda şu adreste çalışıyordu:

http://localhost:5003

Ama Android emulator içinden localhost yazarsam bu bilgisayarımı değil, emulator’ın kendi içini ifade eder.

Bu yüzden Android emulator’dan bilgisayarımdaki backend’e ulaşmak için şu adresi kullandım:

http://10.0.2.2:5003

Android client içinde base URL bu yüzden şöyleydi:

private const val BASE_URL = "http://10.0.2.2:5003"

Bu adres emulator’ın host makinedeki localhost’a ulaşması için kullanılan özel adrestir.

0.3 Backend Endpointleri

Backend içinde 4 temel endpoint oluşturdum:

POST /login
GET /balance
POST /refresh
POST /logout

Bu endpointler Phase 1 için yeterliydi.

0.4 POST /login

Bu endpoint kullanıcı adı ve şifre alır.

Örnek request:

POST /login
Content-Type: application/json

{
  "username": "intern",
  "password": "123456"
}

Eğer bilgiler doğruysa backend bir access token ve refresh token üretir.

Örnek response:

{
  "accessToken": "access_...",
  "refreshToken": "refresh_...",
  "expiresIn": 900
}

Burada:

accessToken:
Korumalı API çağrılarında kullanılır. Kısa ömürlüdür.

refreshToken:
Yeni access token almak için kullanılır. Daha uzun ömürlüdür.

expiresIn:
Access token'ın kaç saniye geçerli olduğunu gösterir.

Bu demo backend’de access token süresini 900 saniye, yani 15 dakika olarak düşündüm.

0.5 GET /balance

Bu endpoint korumalı endpoint olarak tasarlandı.

Yani direkt çağrıldığında çalışmaz; Authorization header içinde access token ister.

Örnek request:

GET /balance
Authorization: Bearer access_...

Token geçerliyse backend bakiye response’u döner:

{
  "balance": 1250,
  "currency": "TRY"
}

Bu endpoint sayesinde Android app’in login sonrası token ile veri çekmesini test ettim.

0.6 POST /refresh

Bu endpoint refresh token alır ve yeni access token üretir.

Örnek request:

POST /refresh
Content-Type: application/json

{
  "refreshToken": "refresh_..."
}

Örnek response:

{
  "accessToken": "access_new...",
  "expiresIn": 900
}

Bu endpoint ile şunu test ettim:

Refresh token geçerliyse kullanıcı tekrar username/password göndermeden yeni access token alabilir.

Bu yüzden refresh token access token’dan daha kritik bir değer olarak değerlendirildi.

0.7 POST /logout

Bu endpoint refresh token’ı server tarafında geçersiz kılar.

Örnek request:

POST /logout
Content-Type: application/json
Authorization: Bearer access_...

{
  "refreshToken": "refresh_..."
}

Logout sonrası aynı refresh token ile tekrar /refresh çağrısı yaptığımda backend 401 Unauthorized döndürdü.

Bu şu anlama gelir:

Logout sadece client tarafında token silmek değildir.
Server tarafında da refresh token invalidation yapılmalıdır.

Daha sonra logout endpoint’ini access token’ı da silecek şekilde güncelledim. Böylece logout sonrası eski access token ile /balance çağrısı da başarısız hale getirilebildi.

0.8 Backend’i Curl ile Test Etme

Önce login endpoint’ini test ettim:

curl -X POST http://localhost:5003/login \
  -H "Content-Type: application/json" \
  -d '{"username":"intern","password":"123456"}'

Response olarak access token ve refresh token aldım:

{
  "accessToken": "access_...",
  "refreshToken": "refresh_...",
  "expiresIn": 900
}

Sonra access token ile /balance endpoint’ini çağırdım:

curl http://localhost:5003/balance \
  -H "Authorization: Bearer access_..."

Beklenen response:

{
  "balance": 1250,
  "currency": "TRY"
}

Daha sonra refresh token ile yeni access token aldım:

curl -X POST http://localhost:5003/refresh \
  -H "Content-Type: application/json" \
  -d '{"refreshToken":"refresh_..."}'

Son olarak logout testini yaptım:

curl -X POST http://localhost:5003/logout \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer access_..." \
  -d '{"refreshToken":"refresh_..."}'

Logout sonrası aynı refresh token ile tekrar /refresh çağırdığımda:

curl -i -X POST http://localhost:5003/refresh \
  -H "Content-Type: application/json" \
  -d '{"refreshToken":"refresh_..."}'

Şu sonucu aldım:

HTTP/1.1 401 Unauthorized

Bu, server-side logout invalidation’ın çalıştığını gösterdi.

0.9 Backend Kodunun Genel Mantığı

Backend’de gerçek database kullanmadım. Demo basit kalsın diye kullanıcıları ve token’ları memory içinde tuttum.

Kullanıcı bilgisi örnek olarak şöyleydi:

username: intern
password: 123456
balance: 1250

Access token ve refresh token değerleri Guid.NewGuid() ile üretilen random stringlerdi.

Örnek token formatı:

access_93c228d9-18b9-4f87-84bb-c85739f1bad8
refresh_3894d05f-1bdf-4a78-a659-ec640ae0ab56

Bu token’lar gerçek JWT değildi. Daha çok opaque token gibi çalıştı. Yani token’ın içinde user bilgisi yoktu; backend token’ı kendi memory map’i içinde arayıp hangi kullanıcıya ait olduğunu buldu.

Bu Phase 1 için yeterliydi çünkü ana amacım JWT implementasyonu değil, Android tarafında token storage davranışını test etmekti.

0.10 Bu Backend Bu Çalışmada Neyi Sağladı?

Bu backend sayesinde Android tarafında şu iki senaryoyu karşılaştırabildim:

BAD app:
Login olur, token alır, token'ı plaintext SharedPreferences içine yazar.

GOOD app:
Login olur, token alır, token'ı EncryptedSharedPreferences içine yazar.

Yani backend bu çalışmada test ortamı görevini gördü.

---

# 1. v1-vulnerable App: MockBankAndroidBAD

## Amaç

Bu uygulamada bilerek kötü token storage yaptım.

Amacım şuydu:

```text
Login sonrası token'lar düz SharedPreferences'a yazılırsa,
rootlu cihazda bu dosya okunabilir mi?
```

---

## 1.1 Android Studio’da Proje Oluşturma

Android Studio’da yeni proje oluşturdum:

```text
New Project
→ Empty Activity
→ Name: MockBankAndroidBAD
→ Package name: com.example.mockbankandroidbad
→ Language: Kotlin
→ Minimum SDK: API 29
→ Finish
```

Bu proje `v1-vulnerable` uygulaması oldu.

---

## 1.2 Manifest Ayarları

Uygulamanın backend’e istek atabilmesi için `AndroidManifest.xml` dosyasına internet izni ekledim.

Dosya yolu:

```text
app/src/main/AndroidManifest.xml
```

Eklediğim permission:

```xml
<uses-permission android:name="android.permission.INTERNET" />
```

Backend localde HTTP çalıştığı için v1’de cleartext trafiğe de izin verdim.

`application` tag’ine şunu ekledim:

```xml
android:usesCleartextTraffic="true"
```

Sonuç olarak manifest kabaca şöyle oldu:

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <uses-permission android:name="android.permission.INTERNET" />

    <application
        android:usesCleartextTraffic="true"
        android:allowBackup="true"
        android:theme="@style/Theme.MockBankAndroidBAD"
        android:label="@string/app_name">

        <activity
            android:name=".MainActivity"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

    </application>
</manifest>
```

Buradaki `usesCleartextTraffic="true"` Phase 1 için zorunlu değil ama backend HTTP olduğu için bu testte gerekliydi. Güvenlik açısından bu ayar Phase 2’de ayrıca ele alınacak.

---

## 1.3 BAD App Login Akışı

`MainActivity.kt` içinde basit bir login ekranı oluşturdum.

Ekranda şunlar vardı:

```text
Username input
Password input
Remember me checkbox
Login button
Result text
```

Login butonuna basınca Android app şu endpoint’e istek attı:

```text
POST http://10.0.2.2:5003/login
```

Gönderilen örnek body:

```json
{
  "username": "intern",
  "password": "123456"
}
```

Backend başarılı response olarak token döndü:

```json
{
  "accessToken": "access_...",
  "refreshToken": "refresh_...",
  "expiresIn": 900
}
```

---

## 1.4 BAD App’te Token’ları Plaintext Saklama

v1’de token’ları bilerek düz `SharedPreferences` içine yazdım.

Kullandığım prefs dosya adı:

```kotlin
private const val PREFS_NAME = "auth_prefs"
```

Token kaydetme mantığı:

```kotlin
val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

prefs.edit()
    .putString("access_token", accessToken)
    .putString("refresh_token", refreshToken)
    .putInt("expires_in", expiresIn)
    .putString("username", username)
    .apply()
```

Ayrıca “remember me” açıksa password’ü de sakladım:

```kotlin
if (rememberMe) {
    prefs.edit()
        .putString("remembered_password", password)
        .apply()
}
```

Bu bilerek kötü bir davranıştı. Çünkü token ve password disk üzerinde plaintext durdu.

---

## 1.5 BAD App’i Çalıştırma

Backend açıkken BAD app’i emulator’da çalıştırdım.

Backend terminalinde:

```bash
dotnet run
```

Android app’te login’e bastığımda ekranda şu sonucu gördüm:

```text
Login success.

accessToken saved to SharedPreferences.
refreshToken saved to SharedPreferences.
rememberMe: true

This is intentionally insecure v1 behavior.
```

---

# 2. Rootlu Emulator ile BAD App Dosyasını Okuma

## 2.1 Önce Root Kontrolü

Rootlu emulator’da adb shell’i root olarak çalıştırdım:

```bash
adb devices
adb -s emulator-5554 root
adb -s emulator-5554 shell id
```

Beklediğim çıktı:

```text
uid=0(root)
```

Bu çıktı geldikten sonra artık her komutta `su -c` kullanmama gerek kalmadı. `adbd` root olarak çalıştığı için dosyaları doğrudan okuyabildim.

---

## 2.2 BAD App SharedPreferences Dosyasını Okuma

BAD app’in package name’i:

```text
com.example.mockbankandroidbad
```

SharedPreferences dosyası şu path’teydi:

```text
/data/data/com.example.mockbankandroidbad/shared_prefs/auth_prefs.xml
```

Dosyayı okumak için şu komutu kullandım:

```bash
adb -s emulator-5554 shell cat /data/data/com.example.mockbankandroidbad/shared_prefs/auth_prefs.xml
```

Gördüğüm sonuçta token ve password plaintext olarak duruyordu.

Buraya BAD terminal screenshot gelecek:

```md
![v1 BAD plaintext SharedPreferences](./images/v1_bad_plaintext_sharedprefs.png)
```

Gözlem:

```text
access_token plaintext görünüyordu.
refresh_token plaintext görünüyordu.
remembered_password plaintext görünüyordu.
```

Bu v1’in temel zafiyetiydi.

---

# 3. v2-hardened App: MockBankAndroidGOOD

## Amaç

Bu uygulamada aynı login akışını daha güvenli hale getirdim.

Amacım şuydu:

```text
Token'ları EncryptedSharedPreferences içine yazarsam,
rootlu cihazda dosya okunsa bile token plaintext görünür mü?
```

---

## 3.1 Android Studio’da GOOD Projesi Oluşturma

Android Studio’da ikinci bir proje oluşturdum:

```text
New Project
→ Empty Activity
→ Name: MockBankAndroidGOOD
→ Package name: com.example.mockbankandroidgood
→ Language: Kotlin
→ Minimum SDK: API 29
→ Finish
```

Bu proje `v2-hardened` uygulaması oldu.

---

## 3.2 Manifest Ayarları

BAD app’te olduğu gibi GOOD app için de internet izni ekledim.

Dosya:

```text
app/src/main/AndroidManifest.xml
```

Eklenen permission:

```xml
<uses-permission android:name="android.permission.INTERNET" />
```

Backend hâlâ HTTP çalıştığı için geçici olarak `usesCleartextTraffic` açık kaldı:

```xml
android:usesCleartextTraffic="true"
```

Bu ayar Phase 1 storage testi için kullanıldı. Network güvenliği Phase 2’de ayrıca düzeltilecek.

---

## 3.3 EncryptedSharedPreferences Dependency Ekleme

GOOD app’te `EncryptedSharedPreferences` kullanmak için Jetpack Security dependency ekledim.

Burada önemli nokta: Dependency project-level dosyaya değil, module-level dosyaya eklendi.

Doğru dosya:

```text
build.gradle.kts (Module :app)
```

Android Studio’da sol tarafta şu dosyayı açtım:

```text
Gradle Scripts
→ build.gradle.kts (Module :app)
```

`dependencies { ... }` bloğunun içine şunu ekledim:

```kotlin
implementation("androidx.security:security-crypto:1.1.0")
```

Örnek:

```kotlin
dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)

    implementation("androidx.security:security-crypto:1.1.0")
}
```

Sonra Android Studio’da çıkan:

```text
Sync Now
```

butonuna bastım.

---

## 3.4 GOOD App Login Akışı

GOOD app’te login akışı BAD app ile aynı kaldı.

Client yine şuraya istek attı:

```text
POST http://10.0.2.2:5003/login
```

Ama bu sefer token’ları düz `SharedPreferences` yerine `EncryptedSharedPreferences` içine yazdım.

---

## 3.5 MasterKey Oluşturma

Önce Android Keystore destekli bir master key oluşturdum:

```kotlin
val masterKey = MasterKey.Builder(context)
    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
    .build()
```

Bu key, EncryptedSharedPreferences’ın verileri şifrelemek için kullandığı anahtar yapısının temelini oluşturur. Anahtar normal bir dosya gibi uygulama klasöründe tutulmaz; Android Keystore tarafından korunur.

---

## 3.6 EncryptedSharedPreferences Oluşturma

Sonra secure preferences objesini oluşturdum:

```kotlin
val securePrefs = EncryptedSharedPreferences.create(
    context,
    SECURE_PREFS_NAME,
    masterKey,
    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
)
```

Burada kullandığım dosya adı:

```kotlin
private const val SECURE_PREFS_NAME = "secure_auth_prefs"
```

---

## 3.7 Token’ları Güvenli Saklama

Token’ları secure prefs içine yazdım:

```kotlin
securePrefs.edit()
    .putString("access_token", accessToken)
    .putString("refresh_token", refreshToken)
    .putInt("expires_in", expiresIn)
    .putString("username", username)
    .apply()
```

BAD app’ten farklı olarak password hiçbir şekilde saklanmadı.

Yani GOOD app’te şu yok:

```kotlin
.putString("remembered_password", password)
```

---

## 3.8 GOOD App’i Çalıştırma

GOOD app’i emulator’da çalıştırdım ve login’e bastım.

Ekranda şu sonucu gördüm:

```text
Login success.

accessToken saved to EncryptedSharedPreferences.
refreshToken saved to EncryptedSharedPreferences.
password was NOT persisted.

This is hardened v2 storage behavior.
```

---

# 4. Rootlu Emulator ile GOOD App Dosyasını Okuma

## 4.1 GOOD App Dosyasını Listeleme

GOOD app’in package name’i:

```text
com.example.mockbankandroidgood
```

SharedPreferences klasörünü listeledim:

```bash
adb -s emulator-5554 shell ls -la /data/data/com.example.mockbankandroidgood/shared_prefs
```

Dosya şu şekilde oluşmuştu:

```text
secure_auth_prefs.xml
```

---

## 4.2 GOOD App Secure Preferences Dosyasını Okuma

Dosyayı okumak için şu komutu kullandım:

```bash
adb -s emulator-5554 shell cat /data/data/com.example.mockbankandroidgood/shared_prefs/secure_auth_prefs.xml
```

Bu sefer dosya okunabildi ama token plaintext görünmedi.

Buraya GOOD terminal screenshot gelecek:

```md
![v2 GOOD encrypted SharedPreferences](./images/v2_good_encrypted_sharedprefs.png)
```

Gözlem:

```text
access_token plaintext görünmedi.
refresh_token plaintext görünmedi.
remembered_password yoktu.
123456 değeri görünmedi.
Dosyada encrypted key/value değerleri vardı.
```

---

# 5. v1 / v2 Karşılaştırması

| Konu | v1 BAD | v2 GOOD |
|---|---|---|
| Token storage | Plain SharedPreferences | EncryptedSharedPreferences |
| Access token | Plaintext | Encrypted |
| Refresh token | Plaintext | Encrypted |
| Password | Remember me ile plaintext saklandı | Hiç saklanmadı |
| Rootlu cihazda dosya okuma | Token/password direkt görünüyor | Ciphertext görünüyor |
| Disk güvenliği | Zayıf | Daha güçlü |
| Runtime saldırılar | Hâlâ mümkün | Hâlâ mümkün |

---

# 6. Sonuç

Bu testte aynı login akışını iki farklı Android uygulamasında denedim.

BAD app’te token’ları ve password’ü düz `SharedPreferences` içine yazdığım için rootlu emulator’da dosyayı okuyunca `access_token`, `refresh_token` ve `remembered_password` değerlerini plaintext olarak gördüm.

GOOD app’te ise token’ları `EncryptedSharedPreferences` içine yazdım ve password’ü hiç saklamadım. Rootlu emulator’da dosyayı okuyabildim ama token değerleri plaintext görünmedi; dosyada sadece encrypted key/value değerleri vardı.

Bu sonuç bana şunu gösterdi:

```text
Plain SharedPreferences token saklamak için güvenli değildir.
Keystore-backed encryption disk üzerindeki token sızıntısını azaltır.
Password hiçbir şekilde persist edilmemelidir.
```

Ama burada sınırı da not etmek gerekiyor:

```text
EncryptedSharedPreferences disk üzerindeki veriyi korur.
Uygulama token'ı API isteğinde kullanacağı anda token RAM'de plaintext hale gelir.
Rootlu cihazda Frida gibi runtime araçlarıyla bu token yine yakalanabilir.
```

Yani v2 disk güvenliğini ciddi şekilde artırdı ama rootlu cihaz/runtime saldırılarına karşı tek başına mutlak koruma sağlamaz.
