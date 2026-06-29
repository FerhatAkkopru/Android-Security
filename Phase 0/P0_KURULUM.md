# Phase 0 — Araç Kurulum Rehberi

**Platform:** macOS (Apple Silicon — M4)
**Tarih:** Haziran 2026
**Amaç:** Android App Security stajı için gerekli tüm güvenlik test araçlarının kurulumu

---

## Ön Koşullar

- macOS (Apple Silicon)
- Android Studio kurulu olmalı
- Terminal erişimi (zsh)

---

## 1. ADB (Android Debug Bridge)

Android cihaz/emülatör ile bilgisayarın arasındaki köprü. Terminalden cihaza bağlanıp dosya çekme/gönderme, uygulama yükleme, log okuma, shell açma gibi her şey yapılabilir.

**Temel komutlar:**

- adb devices — bağlı cihazları listeleme
- adb install app.apk — APK yükleme
- adb shell — cihazda terminal açma
- adb pull /data/data/com.paket/shared_prefs/token.xml ./ — cihazdan dosya çekme
- adb logcat — canlı log akışını izleme (hassas veri loglanıyor mu diye kontrol)
- adb backup -f backup.ab com.paket — uygulama yedeği alma

**Kontrol:**
```bash
adb version
```

Eğer `command not found` hatası alırsan PATH'e ekle:

```bash
echo 'export PATH="$HOME/Library/Android/sdk/platform-tools:$PATH"' >> ~/.zshrc
source ~/.zshrc
```

**Doğrulama:**
```bash
adb version
# Beklenen çıktı: Android Debug Bridge version 1.0.41 (veya üzeri)
```

---

## 2. Burp Suite Community Edition

Bilgisayarında çalışan bir MITM proxy. Emülatörün internet trafiğini Burp üzerinden geçer, giden/gelen her HTTP/HTTPS isteği görünür, durdurur, değiştirir ve öyle yollarsın.

**Temel kullanım akışı:**

- Burp'te Proxy → Intercept sekmesini aç
- Emülatörün Wi-Fi ayarlarında proxy olarak bilgisayarının IP'sini ve Burp'ün portunu (varsayılan 8080) gir
- Uygulamadan istek atma → istek Burp'e düşer → okursun/değiştirirsin → Forward ile sunucuya yollarsın
- HTTP History sekmesinde geçmiş tüm istekleri inceleyebili

**Kurulum:**

1. Tarayıcıdan şu adrese git: **https://portswigger.net/burp/communitydownload**
2. **macOS (Apple Silicon)** seçeneğini seç ve indir
3. İndirilen `.dmg` dosyasını aç, Burp Suite'i **Applications** klasörüne sürükle
4. İlk açılışta macOS "tanınmayan geliştirici" uyarısı verebilir → **System Settings → Privacy & Security** altından **"Open Anyway"** tıkla

**İlk Çalıştırma:**

1. Burp Suite'i aç
2. **Temporary project** → **Next**
3. **Use Burp defaults** → **Start Burp**
4. Ana ekran açılırsa kurulum tamam

---

## 3. jadx-gui

APK dosyasını alıp içindeki Dalvik bytecode'u okunabilir Java/Kotlin koduna çevirir. Grafiksel arayüzü var, kod içinde arama yapabilirsin. Amacı: uygulamanın kaynak kodunu okumak, hardcoded API key/şifre/token avlamak, uygulama mantığını anlamak.

**Temel kullanım:**

- jadx-gui app.apk — APK'yı arayüzde aç
- Sol panelde paket yapısını gezebilirsin
- Cmd+F ile tüm kodda arama yap (örn: "password", "api_key", "secret", "token")

**Ön koşul:** Homebrew kurulu olmalı.

```bash
# Homebrew kontrolü
brew --version

# Eğer Homebrew yoksa kur:
# /bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"
```

**Kurulum:**
```bash
brew install jadx
```

**Doğrulama:**
```bash
jadx --version
# Beklenen çıktı: 1.5.5 (veya üzeri)
```

---

## 4. apktool

jadx'ten farklı olarak APK'yı Smali koduna (Android assembly) ve ham kaynak dosyalarına (AndroidManifest.xml, res/ klasörü) ayırır. Kritik fark: apktool ile kodu değiştirip APK'yı yeniden paketleyebilirsin (rebuild). jadx sadece okumak içindir, apktool okuma + yazma + yeniden paketleme içindir.

**Temel komutlar:**

- apktool d app.apk -o output_folder — APK'yı parçala (decode)
- Çıkan klasördeki smali kodunu veya AndroidManifest.xml'i değiştir
- apktool b output_folder -o modified.apk — değiştirilmiş kodu yeniden APK yap (build)
- Sonra yeni bir Keystore ile imzala: apksigner sign --ks my.keystore modified.apk

**Kurulum:**
```bash
brew install apktool
```

**Doğrulama:**
```bash
apktool --version
# Beklenen çıktı: 3.0.2 (veya üzeri)
```

---

## 5. Frida + objection

Frida: Çalışan bir uygulamanın belleğine (RAM) runtime'da sızıp fonksiyonları hook'layan (yakalayan) araç. JavaScript ile script yazarsın, Frida o scripti çalışan uygulamaya enjekte eder. Örneğin SSL pinning kontrolü yapan fonksiyonu bulup "her zaman true dön" diye değiştirirsin.
objection: Frida'nın üzerine kurulu, hazır komutlarla gelen üst seviye bir araç. Frida script yazmadan tek satır komutla SSL pinning bypass, root detection bypass gibi işlemleri yapabilirsin.

**Temel komutlar:**

- objection -g com.paket.adi explore — uygulamaya bağlan
- android sslpinning disable — SSL pinning'i bypass et 
- android root disable — root detection'ı bypass et 
- android hooking list classes — uygulamadaki tüm sınıfları listele

**Ön koşul:** Python 3 ve pip3 kurulu olmalı.

```bash
# Kontrol
python3 --version   # 3.10+ önerilir
pip3 --version
```

**Kurulum:**
```bash
pip3 install frida-tools objection
```

**Doğrulama:**
```bash
frida --version
# Beklenen çıktı: 17.15.3 (veya üzeri)

objection version
# Beklenen çıktı: 1.12.5 (veya üzeri)
```

---

## 6. MobSF (Mobile Security Framework)

APK'yı sürükle-bırak ile yükle, otomatik olarak statik analiz yapıp sana bir güvenlik raporu çıkarsın. Hardcoded secret'lar, güvensiz permission'lar, allowBackup durumu, cleartext izni, zayıf crypto kullanımı gibi şeyleri otomatik tarar ve puanlar.

**Kurulum:**
```bash
docker pull opensecurity/mobile-security-framework-mobsf
```

**Temel kullanım:**

- docker run -it --rm -p 8000:8000 opensecurity/mobile-security-framework-mobsf ile başlat
- Tarayıcıda http://localhost:8000 aç
- APK dosyasını sürükle bırak → analiz başlar
- Raporda her bulguyu severity'ye göre (High/Medium/Low) sıralar


**Doğrulama:**

1. Tarayıcıda **http://localhost:8000** adresine git
2. MobSF arayüzü açılırsa kurulum tamam
3. Varsayılan giriş bilgileri: `mobsf / mobsf`
4. Terminalde `Ctrl+C` ile durdur

---

## 7. Magisk

Magisk: Root yetkisini yöneten araç. Hangi uygulamanın root erişimi istediğini kontrol eder (Superuser Request pop-up'ı Magisk'ten geliyor). Ayrıca root'u gizleme (MagiskHide/DenyList) özelliği var — uygulamaların root tespitini test etmek için kullanacaksın.

---

## 8. Rootlu Emülatör (AVD + Magisk)

Güvenlik testlerinde tam yetki elde etmek için root yetkisine sahip bir Android emülatörü gerekir.

### Adım 8.1 — Emülatör Oluşturma

1. Android Studio'yu aç → **Device Manager** → **Create Virtual Device**
2. Cihaz profili olarak **Pixel 9 Pro** veya benzeri bir şey seç (fark etmez)
3. **System Image** seçiminde kritik kural:
   - **Google APIs** yazanı seç ✅
   - **Google Play** yazanı SEÇME ❌ (Play Store imajlarında root atılmaz)
   - **API 35** — ARM 64 v8a imajını seç
   - İmaj indirilmemişse indirme ikonuna tıkla ve bekle
4. **Finish** ile emülatörü oluştur
5. Emülatörü başlat

**Doğrulama:**
```bash
adb devices
# Beklenen çıktı:
# List of devices attached
# emulator-5554   device
```

### Adım 8.2 — rootAVD ile Magisk Kurulumu

**rootAVD scriptini indir:**
```bash
cd ~
git clone https://github.com/newbit1/rootAVD.git
cd rootAVD
```

**Mevcut AVD'leri listele:**
```bash
./rootAVD.sh ListAllAVDs
```

**Magisk'i flashla (emülatör açıkken):**
```bash
./rootAVD.sh system-images/android-35/google_apis/arm64-v8a/ramdisk.img
```

İşlem sırasında:
- Emülatörde "Do you want to save the current state?" diye sorarsa → **No** tıkla
- Terminalde Magisk versiyon seçim menüsü çıkacak → numara yazarak **Stable** versiyonu seç
- Script tamamlandığında emülatör kapanacak, bu normal

### Adım 8.3 — Root Doğrulama

1. Android Studio → **Device Manager** → emülatörün yanındaki **üç nokta (⋮)** → **Cold Boot Now**
2. Emülatör açıldıktan sonra terminalde:

```bash
adb shell su -c "id"
```

3. Emülatör ekranında **Superuser Request** pop-up'ı çıkacak → **GRANT** tıkla

**Beklenen çıktı:**
```
uid=0(root) gid=0(root) groups=0(root) context=u:r:magisk:s0
```

Bu çıktıyı görüyorsan root başarıyla çalışıyor.

---

## Kurulum Özet Tablosu

| Araç | Kurulum Yöntemi | Doğrulama Komutu |
|---|---|---|
| ADB | Android Studio ile gelir, PATH'e ekle | `adb version` |
| Burp Suite Community | portswigger.net'ten indir | Uygulamayı aç |
| jadx-gui | `brew install jadx` | `jadx --version` |
| apktool | `brew install apktool` | `apktool --version` |
| Frida + objection | `pip3 install frida-tools objection` | `frida --version` |
| MobSF | `docker pull opensecurity/mobile-security-framework-mobsf` | `localhost:8000` |
| Rootlu Emülatör | AVD (API 35, Google APIs) + rootAVD scripti | `adb shell su -c "id"` |