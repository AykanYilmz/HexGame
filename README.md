<div align="center">

# ⬢ HEX

**Kenarlarını birleştir. Rakibini kes.**

JavaFX ile yazılmış, modern arayüzlü ve yapay zekâ rakipli bir Hex strateji oyunu.

![Java](https://img.shields.io/badge/Java-21-ED8B00?logo=openjdk&logoColor=white)
![JavaFX](https://img.shields.io/badge/JavaFX-21-4CC9F0)
![Maven](https://img.shields.io/badge/Maven-wrapper-C71A36?logo=apachemaven&logoColor=white)
![Tests](https://img.shields.io/badge/tests-17%20passing-7B5CFF)

<img src="docs/screenshots/menu.png" alt="Ana menü" width="860">

</div>

---

## Oyun hakkında

Hex, 1942'de Piet Hein'ın, 1948'de de John Nash'in birbirinden bağımsız olarak icat ettiği iki kişilik bir bağlantı oyunudur. Kuralları bir dakikada öğrenilir, ustalaşmak ise yıllar sürer.

- Oyuncular sırayla boş bir altıgene taş koyar. **Kırmızı başlar.**
- 🔴 **Kırmızı** sol ve sağ kenarı, 🔵 **mavi** üst ve alt kenarı birleştiren kesintisiz bir zincir kurmaya çalışır.
- **Beraberlik yoktur:** Tahta dolmadan biri mutlaka kazanır.

> 💡 **İpucu:** İki taşın arasında iki ortak boş komşu bırakan "köprü" hamlesi kesilemez. Oyunun en güçlü taktiğidir.

## Ekran görüntüleri

| Bilgisayara karşı | Kazanan zincir |
| :---: | :---: |
| <img src="docs/screenshots/vs-ai.png" alt="HexBot'a karşı oyun" width="420"> | <img src="docs/screenshots/winning-path.png" alt="Kazanan zincirin vurgulanması" width="420"> |
| **Takas kuralı:** HexBot güçlü açılışı devralıyor | **Ana menü** |
| <img src="docs/screenshots/swap-rule.png" alt="Takas kuralı" width="420"> | <img src="docs/screenshots/menu.png" alt="Ana menü" width="420"> |

## Özellikler

### 🎮 Oynanış
- **İki oyuncu** aynı bilgisayarda ya da **HexBot'a karşı** oynanabilir. Kolay, Orta ve Zor seviyeleri var.
- **6 tahta boyutu:** 5×5, 7×7, 9×9, 11×11, 13×13, 17×17.
- **Takas kuralı (pie rule):** Mavi, kırmızının ilk taşını devralabilir. HexBot da ne zaman takas yapacağına kendisi karar verir.
- **Geri alma, ipucu ve yeniden başlatma.**
- **Rövanş:** Renkler yer değiştirir, skor oyuncuyla birlikte taşınır.
- **Oyuncu kartları:** Her oyuncu için hamle sayısı, süre ve galibiyet.

### ✨ Arayüz
- Koyu neon tema, cam efektli kartlar ve süzülen altıgenlerden oluşan arka plan.
- Tahta açılışta dalga halinde belirir. Taşlar "pop" efektiyle yerleşir ve komşu hücreleri hafifçe titretir.
- Fare hangi hücrenin üzerindeyse orada sıradaki rengin önizlemesi görünür. Son hamle işaretlenir.
- Kazanınca zincir taş taş parlar, üzerinden ışık akar ve bulanık arka planlı bir sonuç kartı açılır.
- Tahta her pencere boyutuna ve tam ekrana ölçeklenir. Kenarlarda koordinatlar (A1, B2…) gösterilir.
- Son kullanılan isimler, mod ve tahta boyutu bir sonraki açılışta hatırlanır.

## Kısayollar

| Tuş | İşlev |
| --- | --- |
| `F11` | Tam ekran aç/kapat (`Esc` ile çık) |
| `Ctrl + Z` | Geri al |
| `H` | İpucu göster |
| `R` | Oyunu yeniden başlat |
| `Enter` | Menüde oyunu başlat |

## Kurulum ve çalıştırma

Gereken tek şey **JDK 21**, örneğin [Eclipse Temurin](https://adoptium.net/). Maven kurmanıza gerek yok, proje wrapper ile gelir.

```bash
git clone https://github.com/AykanYilmz/HexGame.git
cd HexGame

./mvnw javafx:run        # macOS / Linux / Git Bash
mvnw.cmd javafx:run      # Windows (cmd / PowerShell)
```

Testleri çalıştırmak için:

```bash
./mvnw test
```

> **Hibrit grafikli dizüstüler (Intel + NVIDIA/AMD):** Windows `java.exe`'yi varsayılan olarak entegre GPU'da çalıştırabilir. Tam ekranda akıcılık için *Ayarlar → Sistem → Ekran → Grafik* bölümünden JDK'nın `bin\java.exe` dosyasını ekleyip **Yüksek performans** seçin.

## HexBot nasıl düşünüyor?

HexBot her pozisyonu **0-1 BFS** ile değerlendirir: iki oyuncunun da kenarlarını birleştirmek için en az kaç boş hücreye daha ihtiyacı olduğunu hesaplar. Bu hesapta kendi taşın 0, boş hücre 1 sayılır, rakip taşı ise geçilemez. Puan, rakibin mesafesi ile kendi mesafesi arasındaki farktır. Eşitlik durumunda en kısa yollar üzerindeki boş hücre sayısına ve merkeze yakınlığa bakılır.

| Seviye | Strateji |
| --- | --- |
| Kolay | Gürültülü tek hamle değerlendirmesi, ara sıra özensiz hamle |
| Orta | Tüm hamleleri tek hamle derinliğinde değerlendirir |
| Zor | En iyi adaylar için rakibin her cevabını deneyen 2 hamlelik minimax |

Her seviye kazandıran hamleyi hemen oynar ve rakibin "bir hamlede kazanma" tehditlerini kapatır. Hesaplama arka plan iş parçacığında yapıldığı için arayüz donmaz; 17×17 tahtada bile en uzun hamle yaklaşık 0,1 saniye sürer.

## Proje yapısı

```
src/main/java/org/example/hexgame/
├── HexApp.java              Uygulama girişi ve ekran geçişleri
├── model/                   Oyun mantığı (arayüzden bağımsız)
│   ├── Board.java           Tahta, hamle geçmişi, takas, geri alma, kazanan yol (BFS)
│   ├── GameConfig.java      Maç ayarları
│   ├── Player.java · Difficulty.java
│   └── Settings.java        Ayarların java.util.prefs ile saklanması
├── ai/
│   └── HexAI.java           HexBot
└── ui/
    ├── MenuView.java        Ana menü ve kurallar penceresi
    ├── GameView.java        Oyun akışı, araç çubuğu, sonuç kartı
    ├── BoardView.java       Altıgen tahta ve taş animasyonları
    ├── PlayerCard.java      Oyuncu kartları
    ├── Segmented · ToggleSwitch · Modal · AnimatedBackground
    └── Theme · Fx · Icons   Renkler, animasyon yardımcıları, vektör ikonlar
src/main/resources/org/example/hexgame/styles.css
src/test/java/…              Tahta kuralları ve HexBot için birim testleri
```

### Performans notları
- Taşların, tahta kenarlarının ve başlıkların parlama efektleri bir kez çizilip önbellekten kullanılır.
- Oyun sırasında arka plan animasyonu durur. Sonuç kartının arkasındaki bulanık ekran bir kez hesaplanır.
- Sürekli dönen animasyonlar yalnızca efekt içermeyen ya da önbelleğe alınmış öğelerde kullanılır.

---

<div align="center">
Arif Aykan Yılmaz
</div>
