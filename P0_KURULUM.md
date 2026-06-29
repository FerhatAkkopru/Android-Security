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

ADB, Android Studio ile birlikte gelir. Ancak terminal onu doğrudan bulamayabilir. PATH'e eklenmesi gerekir.

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

Ağ trafiğini araya girip (intercept) dinlemek ve değiştirmek için kullanılan proxy aracı.

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

APK dosyalarını decompile edip okunabilir Java/Kotlin koduna çeviren araç. Hardcoded secret avlamak için kullanılır.

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

APK'yı Smali koduna ve kaynak dosyalarına ayırıp yeniden paketlemeye yarayan araç.

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

Çalışan bir uygulamanın belleğine sızıp runtime'da fonksiyonları manipüle eden araç seti. SSL pinning bypass, root detection bypass gibi işlemler için kullanılır.

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

APK'yı otomatik tarayıp güvenlik raporu çıkaran statik ve dinamik analiz aracı. Docker üzerinden çalıştırılır.

**Ön koşul:** Docker Desktop kurulu ve çalışır durumda olmalı.

```bash
# Docker kontrolü
docker --version

# Docker Desktop çalışıyor mu?
docker info
# Eğer "Cannot connect to the Docker daemon" hatası alırsan:
# Spotlight (Cmd + Space) → "Docker" yaz → Docker Desktop'ı aç
# Sol altta yeşil "Running" yazana kadar bekle (30-60 saniye)
```

**Kurulum:**
```bash
docker pull opensecurity/mobile-security-framework-mobsf
```

**Çalıştırma (test):**
```bash
docker run -it --rm -p 8000:8000 opensecurity/mobile-security-framework-mobsf
```

**Doğrulama:**

1. Tarayıcıda **http://localhost:8000** adresine git
2. MobSF arayüzü açılırsa kurulum tamam
3. Varsayılan giriş bilgileri: `mobsf / mobsf`
4. Terminalde `Ctrl+C` ile durdur

---

## 7. Rootlu Emülatör (AVD + Magisk)

Güvenlik testlerinde tam yetki elde etmek için root yetkisine sahip bir Android emülatörü gerekir.

### Adım 7.1 — Emülatör Oluşturma

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

### Adım 7.2 — rootAVD ile Magisk Kurulumu

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

### Adım 7.3 — Root Doğrulama

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