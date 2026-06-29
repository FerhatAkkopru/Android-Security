## Sand-box

Sand-box: appler arası izolasyonu sağlıyor bu sayede bir app diğerinin verilerine ulaşmaıyor ya da onu bozamıyor her appin bir uid si var bu da androidin linux tabanlı bir sistem olmasından linuxta her userın bir id si var userlar biribrlerinin alanlarına giremiyor bu da aynı şekilde çalışıyor userlar appler aslında, bu ayrım kernel seviyesinde yapıldığı için aşılması mümkün değil

link:https://source.android.com/docs/security/app-sandbox

## Permission Model

Permission Model: Bazen bu sandbox'ları delmek ve cihazın kaynaklarına erişmek veya başka bir uygulamaya birtakım veriler göndermek gerekebiliyor. Android, bu erişimleri kontrollü sağlamak için izinleri temelde üç ana kategoriye ayırır:

- Install-time Permissions (Kurulum Anı İzinleri): Sisteme veya diğer uygulamalara minimum riski olan izinlerdir. Bu izinleri AndroidManifest.xml dosyasına yazman yeterlidir, Android kurulum anında bunları otomatik olarak onaylar. İki alt türü bulunur:
- Normal Permissions: İnternete erişim, alarm kurma gibi kullanıcının gizliliğini veya cihazın güvenliğini doğrudan tehdit etmeyen izinler.
- Signature Permissions (İmza İzinleri): Geliştirdiğin uygulamaların imza sertifikası (keystore) aynıysa sistemin otomatik verdiği, uygulamalar arası güvenli veri paylaşımı sağlayan izinler.
- Runtime Permissions (Çalışma Zamanı / Tehlikeli İzinler): Kullanıcının kişisel verilerine veya cihazın kritik fonksiyonlarına erişim sağlayan izinlerdir (Örn: Kamera, Konum, Mikrofon, Kişiler). Bunları sadece Manifest'e yazmak yetmez; uygulama çalışırken (Runtime) kullanıcıya bir pop-up göstererek açıkça onay alman gerekir. Bir izni istemeden önce, o işi izin almadan yapıp yapamayacağını düşün. Örneğin, uygulamanın içine sıfırdan bir kamera motoru yazıp fotoğraf çekmek istersen CAMERA izni alman gerekir. Ancak Android'in yerleşik kamera uygulamasını bir Intent yardımıyla tetikleyip sadece çekilen fotoğrafın sonucunu alırsan, uygulamana hiçbir izin eklemek zorunda kalmazsın. Buna "Data Minimizasyonu" (Data Minimization) deniyor.
- Special Permissions (Özel İzinler): "Diğer uygulamaların üzerinde çizim yapma" veya "Sistem ayarlarını değiştirme" gibi çok güçlü ve cihazın temel davranışını değiştiren eylemlerdir. Bunlar uygulama içindeki standart bir pop-up ile alınamaz; kullanıcının doğrudan cihaz ayarlarına (Settings) yönlendirilip bu izni manuel olarak aktif etmesi gerekir.

Permission Groups (İzin Grupları):
Sistem, mantıksal olarak birbiriyle ilişkili izinleri kullanıcı arayüzünü (Örn: Ayarlar menüsü) sadeleştirmek için gruplar (Örn: SMS gönderme ve SMS alma aynı gruptadır). Modern Android sürümlerinde sistem, kullanıcıyı yanıltmamak adına bir izin istendiğinde o grubun kapsadığı tüm riskleri içeren geniş bir uyarı metni gösterir (Örn: Uygulama sadece SMS okumak istese bile ekranda "Mesajları gönderme ve görüntüleme" uyarısı çıkar).

Kullanıcı bu geniş uyarıya onay verdiğinde gruba yetki vermiş sayılır ve sistem aynı gruptaki diğer işlemler için tekrar tekrar pop-up çıkarıp kullanıcıyı darlamaz. Ancak, geliştirici olarak kodunda gruptaki bir izni aldığın için diğerlerine de otomatik sahip olduğunu varsayamazsın; her bir spesifik izni işletim sisteminden kod ile açıkça talep etmeye devam etmelisin. Resmi döküman tam olarak bu yüzden uyarır: "Grup yapıları güncellemelerle değişebilir, kodunu asla bu grupların mevcut yapısına güvenerek yazma; yarın gruplar ayrılırsa uygulama çöker."

## Best Practices

Best Practices (En İyi Pratikler)
Geciktir (As late as possible): Kullanıcı uygulamayı ilk açtığı an bütün izinleri ekrana patlatma. Ne zaman mikrofon gerekiyorsa, tam o butona basıldığı an izni iste.

Şeffaf Ol (Be transparent): Kullanıcıya o izni neden istediğini net açıkla.

Kütüphanelere Dikkat Et: Projene eklediğin üçüncü parti bir kütüphane, kendi içinde senin bilmediğin izinleri gizlice projene dahil edebilir (Dependency check).

## Keystore Nedir?

Keystore , uygulamanın geliştiricisi tarafından oluşturulan, içinde özel bir şifreleme anahtarı (Private Key) barındıran dijital bir imza dosyasıdır.
Meta'nın elinde sadece kendisinin bildiği bir Keystore dosyası vardır.
Facebook uygulamasını bu Keystore ile mühürleyip markete koyar.
Messenger ve Instagram uygulamalarını da aynı Keystore ile mühürler.
Bu uygulamalar, senin şifreni tekrar tekrar girmene gerek kalmadan arka planda hesap bilgilerini birbirleriyle paylaşırlar. Bunu yapabilmelerinin tek sebebi, Android'in Signature Permission kuralıdır. Android bakar: "Facebook'un Keystore mühürü ile Instagram'ın mühürü birebir aynı. Demek ki bu ikisi aynı şirketin. Birbirlerinin verilerine erişebilirler."
Peki ya şirket kötü niyetliyse?
Diyelim ki "X Şirketi" bir el feneri uygulaması (İyi) yaptı ve sen bunu kurdun. Sonra aynı şirket bir de oyun (Kötü/Malware) yaptı. İkisini de aynı Keystore ile imzaladı. Oyun arka planda el feneri uygulamasındaki verileri çalmak için Signature Permission kullanırsa, Android buna izin verir. İşletim sistemi bu noktada kördür.

Buradaki asıl güvenlik önlemi Android'in sandbox mimarisi değil, Google Play Protect ve Play Store inceleme ekibidir. Google, bu şirketin oyununun malware olduğunu fark ettiği an şirketin geliştirici hesabını kapatır ve o Keystore ile imzalanmış tüm uygulamaları (iyi olan el feneri dahil) yeryüzünden siler.

Bir uygulamayı Google Play Store'a yüklemeden önce bu Keystore ile "imzalarsın".
Bu imza, uygulamanın kimlik kartı veya parmak izi gibidir. Değiştirilemez ve taklit edilemez.

## targetSdk

targetSdk (Hedef SDK): uygulamanın hangi Android sürümünün güvenlik kurallarına ve davranış değişikliklerine göre test edildiğini işletim sistemine bildiren bir beyandır. her sürümde daha fazla güvenlik önlemi gelebilir bu yüzden eski sürümler tehditlere daha çok açıktır

android sürümleri yeni gelen özellikler: https://developer.android.com/about/versions

## MITM (Man-in-the-Middle)Ne Demek?

İletişim kuran iki tarafın (senin mobil uygulaman ile backend API sunucusu) arasına gizlice üçüncü bir kişinin ağ seviyesinde sızmasıdır. Uygulamanın sunucuya gönderdiği veri (örneğin giriş token'ı veya banka bakiye transfer talebi) doğrudan sunucuya gitmeden önce saldırganın cihazından geçer. Saldırgan, şifrelenmemiş veya zayıf şifrelenmiş bu trafiği okuyabilir (dinleme) veya paketin içeriğini manipüle edip sunucuya farklı bir veri iletebilir (değiştirme).  Korunma: Network Security Config ile cleartext (HTTP) trafiği tamamen yasaklamak + Certificate Pinning ile sunucunun sertifikasının parmak izini uygulamaya sabitleyerek sahte sertifikalı proxy'lerin araya girmesini engellemek.

## Device Theft (Cihaz Hırsızlığı)

Kullanıcının telefonunun fiziksel olarak çalınması veya kaybolmasdır. Saldırganın amacı cihazın içindeki verilere erişmek: auth token'lar, şifreler, veritabanındaki hassas bilgiler gibi. eğer appdeki önemli bilgiler plaintext ise adb veya flash ile doğrudan okunabilir olur bunu önlemk içinde hassas bilgileri keystore ile encrypte etmek

## Rooted Device (Rootlu Cihaz)

Cihazın sahibinin (veya saldırganın) Android'in üretici kısıtlamalarını kaldırarak Linux root yetkisini elde etmesidir. Root yetkisi sandbox kurallarını yıkar,Ayrıca rootlu cihazda Frida gibi araçlar çalıştırılarak uygulamanın çalışma anında bellekteki fonksiyonlar manipüle edilebilir — mesela SSL pinning kontrolünü "her zaman başarılı" döndürecek şekilde değiştirmek gibi.Korunma: Root detection (root tespiti) + verileri Keystore ile şifreleme + Play Integrity API ile sunucu tarafında doğrulama. Ama bunlar geçici önlemlerdir tam kontrol saldırganda root olduğu için

link:https://developer.android.com/privacy-and-security/security-tips

## Repackaged APK (Yeniden Paketlenmiş APK)

Saldırgan uygulamayı Play Store'dan veya internetten indirir, apktool gibi bir araçla APK'yı parçalarına ayırır (decompile eder). İçindeki kodu okur, istediği değişikliği yapar — mesela login ekranına kullanıcının girdiği şifreyi kendi sunucusuna gönderen birkaç satır kod ekler.

Uygulamanı imzalayan Keystore'un parmak izini (signature hash) uygulama her açıldığında kontrol ettirmek bu problemi önleyebilir. Saldırgan kodu değiştirip kendi Keystore'u ile yeniden imzaladıysa parmak izi farklı olacak uygulama da kendini kapatır veya sunucuya "ben sahte bir kopyayım" sinyali gönderir. Ek olarak Google'ın Play Integrity API'si sunucu tarafında uygulamanın orijinalliğini doğrular. Ayrıca R8/ProGuard ile kodunu obfuscate etmek (karıştırmak) repackaging'i engellemez ama saldırganın kodunu okumasını ve değiştirmesini çok daha zor hale getirir.

link1:https://developer.android.com/google/play/integrity/overview
link2:https://developer.android.com/build/shrink-code

## Malicious App on Same Device (Aynı Cihazdaki Zararlı Uygulama)

Kullanıcının telefonunda senin uygulamanla birlikte zararlı bir uygulamanın da kurulu olduğu senaryo. Zararlı uygulama sandbox sayesinde uygulama dosyalarına doğrudan giremez ama dolaylı yollardan saldırabilir:

- Exported Components (Dışa Açık Bileşenler): Android'de Activity, Service, BroadcastReceiver ve ContentProvider denen bileşenler var. Eğer bunlar AndroidManifest.xml'de exported=true olarak bırakılırsa, cihazdaki herhangi bir uygulama bu bileşenlere Intent göndererek tetikleyebilir. Mesela uygulamada para transfer eden bir Service var ve dışa açıksa, zararlı uygulama o Service'e sahte bir Intent göndererek işlem başlatabilir.
- Clipboard (Pano) Dinleme: Kullanıcı uygulamandan bir IBAN kopyaladığında, zararlı uygulama panoyu okuyup o IBAN'ı kendi IBAN'ıyla değiştirebilir. Kullanıcı yapıştırdığında farkında olmadan saldırganın hesabına para gönderir.
- Screen Overlay / Tapjacking (Ekran Üstü Katman): Zararlı uygulama login ekranının üzerine birebir aynı görünümlü sahte bir katman çizer. Kullanıcı şifresini gücenli uygulamaya girdiğini sanırken aslında zararlı uygulamaya yazıyor olur.
- Nasıl korunulur? Bileşenlerini gereksiz yere exported=true yapılmamalı. Intent'lerden gelen verilere körü körüne güvenmemek gerekiyor, mutlaka doğrulamak lazım. Hassas ekranlarda FLAG_SECURE kullanılmalı ve overlay koruması eklenmeli.

## OWASP MASVS (Mobile Application Security Verification Standard)

Bir mobil uygulamanın güvenli kabul edilmesi için karşılaması gereken kuralların listesidir. "Şunlar şunlar yapılmalı" der, nasıl yapılacağını söylemez.
MASVS kuralları şu ana kategorilere ayrılır:

- MASVS-STORAGE: Hassas veriler yerelde güvenli mi saklanıyor? (Şifreli mi, backup'a sızıyor mu, loglara yazılıyor mu?)
- MASVS-CRYPTO: Kriptografi doğru kullanılıyor mu? (Güçlü algoritmalar mı, hardcoded key var mı?)
- MASVS-AUTH: Kimlik doğrulama sağlam mı? (Token yönetimi, biometrik bağlama, oturum süresi)
- MASVS-NETWORK: Ağ iletişimi güvenli mi? (TLS zorunlu mu, pinning var mı, cleartext var mı?)
- MASVS-PLATFORM: Platform özellikleri güvenli kullanılıyor mu? (Exported bileşenler, deep linkler, WebView)
- MASVS-CODE: Kod kalitesi ve sertleştirme yeterli mi? (Obfuscation, tamper detection, debug kontrolleri)
- MASVS-RESILIENCE: Tersine mühendisliğe karşı dayanıklılık var mı? (Root detection, anti-Frida, integrity check)

link:https://mas.owasp.org/MASVS/

## OWASP MASTG (Mobile Application Security Testing Guide)

MASVS'ın pratik karşılığı. "Nasıl test edilir?" sorusuna cevap verir. Her MASVS kuralı için somut test adımları içerir: hangi aracı kullan, neyi kontrol et, ne bulduysan ne anlama gelir.
Örnek akış: MASVS-STORAGE diyor ki "Hassas veri şifreli saklanmalı." MASTG ise diyor ki "Rootlu cihazda adb shell ile /data/data/com.paket.adi/shared_prefs/ klasörüne git, XML dosyalarını aç, plaintext token var mı bak. Varsa bu kural ihlal edilmiş demektir."

link:https://mas.owasp.org/MASTG/

## Some Keywords
- **Intent**: Android'de uygulamalar arası veya uygulama içi iletişim mesajı. "Şu Activity'yi aç", "Şu Service'i başlat", "Kamerayı tetikle" gibi komutlar Intent ile gönderilir.
- **FLAG_SECURE**: Bir Activity'ye eklenen bayrak. Ekran görüntüsü alınmasını ve son uygulamalar (recents) önizlemesinde içeriğin görünmesini engeller.
- **Content Provider**: Uygulamanın verilerini yapılandırılmış bir şekilde (SQL benzeri sorguyla) diğer uygulamalarla paylaşmasını sağlayan Android bileşeni. Rehber uygulaması buna iyi bir örnek — diğer uygulamalar Content Provider üzerinden kişileri okur.
- **Obfuscation**: Kodun çalışmasını değiştirmeden sınıf, metod ve değişken isimlerini anlamsız harflere (a.b.c) çevirmek. Decompile eden saldırganın kodu anlamasını zorlaştırır.