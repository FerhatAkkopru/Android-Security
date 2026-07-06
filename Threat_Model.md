# Threat Model — Mock Banking App

## Uygulama Özeti

Küçük bir bankacılık uygulaması. Kullanıcı login ekranından giriş yapar, sunucudan bir auth token alır, bu token ile API çağrısı yaparak bakiye sorgular. Yerelde hassas veriler saklanır: bakiye, kullanıcı notları ve bir PIN kodu.

## Korunması Gereken Varlıklar (Assets)

- Kullanıcının login bilgileri (username / password)
- Auth token (access + refresh)
- Yerel veritabanındaki bakiye ve notlar
- PIN kodu
- API endpoint adresleri ve varsa API key

## Saldırı Senaryoları

### 1. Ağ Saldırganı — MITM (Man-in-the-Middle)

Aynı Wi-Fi ağındaki bir saldırgan, uygulama ile sunucu arasındaki trafiğe araya girer. Login isteğindeki kullanıcı adı ve şifreyi yakalar veya auth token'ı çalar. Eğer uygulama cleartext HTTP kullanıyorsa veya sertifika doğrulaması yoksa tüm trafik okunabilir ve değiştirilebilir. Örneğin saldırgan bakiye response'unu manipüle edebilir.

**İlgili MASVS kategorisi:** MASVS-NETWORK

### 2. Cihaz Hırsızı — Device Theft

Telefonu fiziksel olarak ele geçiren biri, adb veya flash okuma ile uygulamanın yerel dosyalarına erişir. Eğer SharedPreferences'taki auth token veya Room veritabanındaki bakiye plaintext (şifresiz) saklanıyorsa doğrudan okunur ve hesap ele geçirilir.

**İlgili MASVS kategorisi:** MASVS-STORAGE, MASVS-CRYPTO

### 3. Rootlu Cihaz Saldırganı — Rooted Device

Kendi cihazını rootlayan veya rootlu bir cihaza erişen saldırgan, sandbox duvarlarını aşarak /data/data/com.app/ klasörüne girer. Şifrelenmemiş tüm dosyaları okur. Ayrıca Frida ile çalışma anında SSL pinning kontrolünü bypass edebilir veya PIN doğrulama fonksiyonunu "her zaman true dön" şeklinde manipüle edebilir.

**İlgili MASVS kategorisi:** MASVS-STORAGE, MASVS-CRYPTO, MASVS-RESILIENCE

### 4. Tersine Mühendis — Repackaged APK

Saldırgan APK'yı indirip jadx ile decompile eder, kaynak kodunu okur. Eğer PIN veya API key koda gömülmüşse (hardcoded) doğrudan görür. Sonra apktool ile kodu değiştirip (örneğin login ekranına şifreyi kendi sunucusuna gönderen kod ekleyip) kendi Keystore'u ile imzalar ve sahte versiyonu dağıtır.

**İlgili MASVS kategorisi:** MASVS-CODE, MASVS-RESILIENCE

### 5. Aynı Cihazdaki Zararlı Uygulama — Malicious App

Kullanıcının telefonunda zararlı bir uygulama da kurulu. Bu uygulama sandbox yüzünden dosyalara doğrudan erişemez ama dolaylı yollardan saldırır: eğer bankacılık uygulamasının bileşenleri exported=true ise Intent göndererek işlem tetikler, clipboard'dan kopyalanan IBAN'ı kendi IBAN'ıyla değiştirir veya login ekranının üzerine birebir aynı görünümlü sahte bir katman (overlay) çizerek şifreyi çalar.

**İlgili MASVS kategorisi:** MASVS-PLATFORM

## Özet Tablo

| Saldırgan | Hedef Varlık | Saldırı Yöntemi | MASVS |
|---|---|---|---|
| Ağ saldırganı (MITM) | Login bilgileri, auth token | Trafiği dinleme/değiştirme | NETWORK |
| Cihaz hırsızı | Token, bakiye, PIN | adb pull, flash okuma | STORAGE, CRYPTO |
| Rootlu cihaz saldırganı | Tüm yerel veriler + runtime | Sandbox bypass, Frida hook | STORAGE, CRYPTO, RESILIENCE |
| Tersine mühendis | Hardcoded secret, uygulama kodu | jadx/apktool decompile + repackage | CODE, RESILIENCE |
| Zararlı uygulama | Kullanıcı girişi, clipboard, bileşenler | Intent, overlay, clipboard hijack | PLATFORM |


## MITM Saldırısı Simülasyonu (Burp Suite)
 
### Ne Yaptım?
 
Emülatördeki Android cihazın internet trafiğini Burp Suite üzerinden geçirerek bir MITM saldırısı simüle ettim. Emülatördeki Chrome tarayıcıdan `http://httpbin.org/get` adresine istek attım ve bu istek sunucuya ulaşmadan önce Burp Suite'te yakalandı — isteğin tüm içeriğini (header'lar, URL, method) okuyabildim.
 
### Nasıl Yaptım?
 
**Adım 1 — Bilgisayarın IP adresini öğren:**
```bash
ifconfig en0 | grep "inet "
# Çıktı: inet 10.4.112.21 netmask 0xffffff00 broadcast 10.4.112.255
```
Bu IP adresi emülatörün trafiği göndereceği hedef — yani Burp'ün çalıştığı bilgisayar.
 
**Adım 2 — Burp Suite'i proxy olarak ayarla:**
- Burp Suite'i aç → Temporary project → Use Burp defaults → Start Burp
- **Proxy** → **Proxy settings** → **Proxy listeners** altında `127.0.0.1:8080` satırını **Edit** de
- **Bind to address** kısmını **All interfaces** olarak değiştir → OK
- Bu ayar Burp'ün sadece kendi bilgisayarından değil, aynı ağdaki diğer cihazlardan (emülatör dahil) gelen trafiği de dinlemesini sağlar
**Adım 3 — Emülatörde proxy ayarla:**
- Emülatörde **Settings** → **Network & internet** → **Internet** → bağlı Wi-Fi'a tıkla → ⚙️ → **Edit** (kalem ikonu)
- **Advanced options** → **Proxy** kısmını **Manual** yap
- **Proxy hostname:** `10.4.112.21` (bilgisayarın IP'si)
- **Proxy port:** `8080` (Burp'ün dinlediği port)
- **Save**
- Bu ayar emülatöre "internete doğrudan çıkma, önce bu bilgisayara uğra" der
**Adım 4 — Burp'te Intercept'i aç:**
- Burp'te **Proxy** → **Intercept** sekmesine gel
- **"Intercept is on"** yazıdığından emin ol (kapalıysa tıkla)
- Bu mod her gelen isteği durdurur ve sana gösterir
**Adım 5 — Emülatörden istek at:**
- Emülatördeki **Chrome** tarayıcıyı aç
- Adres çubuğuna `http://httpbin.org/get` yaz ve git
- Sayfa yüklenmeyecek, donacak — çünkü istek Burp'te bekliyor
**Adım 6 — Burp'te isteği oku:**
- Burp ekranında isteğin tüm detayları görünür: method (GET), URL, Host, User-Agent, header'lar
- User-Agent'ta `Linux; Android 10; K` yazıyorsa istek emülatörden gelmiş demektir
- Üç seçenek var:
  - **Forward:** İsteği sunucuya gönder (sayfa yüklenir)
  - **Drop:** İsteği sil (sayfa asla yüklenmez)
  - İsteği değiştirip öyle Forward'la (saldırgan manipülasyonu)
**Adım 7 — Sessiz izleme modu (gerçek saldırgan gibi):**
- **"Intercept is off"** yap — trafik artık durmadan akar
- Emülatörde sayfaları normal gezebilirsin
- Burp'te **HTTP History** sekmesine geç — tüm istekler ve cevaplar sessizce kaydedilmiş olarak listelenir
- Saldırgan genelde bu modu kullanır: kullanıcı hiçbir şey fark etmez, saldırgan arka planda tüm trafiği okur

### Gerçek Hayatta Bu Nasıl Olur?
 
Gerçek bir saldırıda kullanıcı hiçbir proxy ayarı yapmaz. Saldırgan Wi-Fi ağının kendisini kontrol eder — kafedeki router'ı hackler veya "Free_Cafe_WiFi" gibi sahte bir ağ açar. Kullanıcılar bu ağa bağlandığında tüm trafik saldırganın bilgisayarından geçer. Kullanıcının haberi olmaz.
 
Saldırgan genellikle trafiği durdurmaz (Intercept off), sessizce arka planda kaydeder ve HTTP History'den tüm istekleri ve cevapları okur. Eğer uygulama cleartext HTTP kullanıyorsa veya sertifika doğrulaması düzgün yapılmamışsa, login şifreleri, auth token'lar ve hassas veriler doğrudan okunur.

**Örnek ekran HTTP history görüntüsü:**
![Burp Suite Intercept](burp-intercept.png)