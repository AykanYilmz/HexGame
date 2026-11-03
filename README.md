# HEX

JavaFX ile yazılmış modern bir **Hex** strateji oyunu. İki kişi aynı bilgisayarda oynayabilir ya da üç zorluk seviyesi olan yapay zekâya karşı oynanabilir.

> Kırmızı **sol ↔ sağ**, mavi **üst ↕ alt** kenarları birleştiren kesintisiz bir zincir kurmaya çalışır. Hex'te beraberlik yoktur.

## Özellikler

- **6 tahta boyutu:** 5×5, 7×7, 9×9, 11×11, 13×13, 17×17. Tahta kodla üretilir ve pencereye göre ölçeklenir.
- **Bilgisayar rakip (HexBot):** Kolay, Orta ve Zor seviyeleri var. Yapay zekâ arka planda çalışır, arayüz donmaz.
- **Takas kuralı (pie rule):** Mavi, kırmızının ilk taşını devralabilir. HexBot da bu kararı kendisi verir.
- **Geri alma (Ctrl+Z), ipucu (H) ve yeniden başlatma (R).**
- **Rövanş:** Renkler yer değiştirir, skor oyuncuyla birlikte taşınır.
- **Oyuncu kartları:** Hamle sayısı, oyuncu başına süre, galibiyetler ve sıranın kimde olduğunu gösteren parlama.
- **Animasyonlar:** Hareketli arka plan, tahtanın dalga halinde belirmesi, taşların "pop" efekti ve dalgası, hover önizlemesi, kazanan zincirin ışıması ve bulanık arka planlı sonuç kartı.
- **Ayarları hatırlama:** İsimler, mod, boyut ve zorluk bir sonraki açılışta hazır gelir.
- **Tam ekran:** F11 ile açılır, Esc ile çıkılır.

## Çalıştırma

Gereken tek şey **JDK 21**. Maven gerekmez, wrapper kendini indirir.

```bash
# JAVA_HOME bir JDK 21 kurulumunu göstermeli
./mvnw javafx:run        # macOS / Linux / Git Bash
mvnw.cmd javafx:run      # Windows (cmd / PowerShell)
```

> **Hibrit grafikli dizüstüler (Intel + NVIDIA):** Windows `java.exe`yi varsayılan olarak entegre GPU'da çalıştırabilir. *Ayarlar → Sistem → Ekran → Grafik* bölümünden JDK'nın `java.exe` dosyasını ekleyip **Yüksek performans** seçin.

Testler:

```bash
./mvnw test
```

## Proje yapısı

```
src/main/java/org/example/hexgame/
├── HexApp.java            Uygulama girişi, ekran geçişleri
├── model/                 Oyun mantığı (UI'dan bağımsız)
│   ├── Board.java         Tahta, hamle geçmişi, takas, geri alma, kazanan yol (BFS)
│   ├── Player.java        RED / BLUE
│   ├── GameConfig.java    Maç ayarları
│   ├── Difficulty.java
│   └── Settings.java      java.util.prefs ile ayar saklama
├── ai/HexAI.java          0-1 BFS bağlantı mesafesi + 2 hamle ileriye bakan minimax
└── ui/                    Görünümler ve bileşenler
    ├── MenuView, GameView, BoardView, PlayerCard
    ├── Segmented, ToggleSwitch, Modal, AnimatedBackground
    └── Theme, Fx (animasyon yardımcıları), Icons
src/main/resources/org/example/hexgame/styles.css
```

### Yapay zekâ nasıl düşünüyor?

Her pozisyon için iki oyuncunun da "kenarlarını birleştirmek için kaç boş hücreye daha ihtiyacı var" değeri 0-1 BFS ile hesaplanır. Kendi taşın 0, boş hücre 1 sayılır, rakip taşı duvardır. Eşitlik durumunda en kısa yollar üzerindeki boş hücre sayısına ve merkeze yakınlığa bakılır.

- **Kolay:** Gürültülü tek hamlelik değerlendirme, ara sıra özensiz hamle.
- **Orta:** Tek hamlelik değerlendirme.
- **Zor:** En iyi adaylar için rakibin her cevabını deneyen 2 hamlelik minimax.

Her seviye kazandıran hamleyi hemen oynar ve rakibin anlık tehditlerini kapatır.
