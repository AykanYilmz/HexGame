package org.example.hexgame.ui;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.RotateTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.effect.BlurType;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Polygon;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.util.Duration;
import org.example.hexgame.model.Difficulty;
import org.example.hexgame.model.GameConfig;
import org.example.hexgame.model.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

/** Landing screen: animated hero on the left, match setup card on the right. */
public class MenuView extends StackPane {

    private final List<Animation> loops = new ArrayList<>();
    private final HBox content;

    public MenuView(GameConfig initial, Consumer<GameConfig> onStart) {
        getStyleClass().add("menu-view");

        // ---------------------------------------------------------------- hero
        Node logo = buildLogo();
        Text title = new Text("HEX");
        title.setFont(Font.font("Segoe UI Black", FontWeight.BLACK, 132));
        title.setFill(Fx.brandGradient());
        title.setEffect(new DropShadow(BlurType.GAUSSIAN, Theme.VIOLET.deriveColor(0, 1, 1, 0.6), 30, 0.15, 0, 0));
        title.setCache(true);
        // Breathing halo: a pre-blurred copy whose opacity pulses. Animating opacity of a cached
        // node is nearly free, whereas animating a blur radius re-blurs the text every frame.
        Text halo = new Text("HEX");
        halo.setFont(title.getFont());
        halo.setFill(Fx.brandGradient());
        halo.setEffect(new GaussianBlur(36));
        halo.setCache(true);
        halo.setMouseTransparent(true);
        Timeline glowPulse = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(halo.opacityProperty(), 0.25)),
                new KeyFrame(Duration.seconds(2.2), new KeyValue(halo.opacityProperty(), 0.85, Fx.EASE_IN_OUT)));
        glowPulse.setAutoReverse(true);
        glowPulse.setCycleCount(Animation.INDEFINITE);
        glowPulse.play();
        loops.add(glowPulse);
        StackPane titleBox = new StackPane(halo, title);
        titleBox.setAlignment(Pos.CENTER_LEFT);

        Label tagline = new Label("Kenarlarını birleştir. Rakibini kes.");
        tagline.getStyleClass().add("tagline");
        Label blurb = new Label("Beraberliği olmayan, 1942'den beri oynanan saf strateji oyunu. "
                + "Kırmızı soldan sağa, mavi yukarıdan aşağıya bir zincir kurmaya çalışır.");
        blurb.getStyleClass().add("muted");
        blurb.setWrapText(true);
        blurb.setMaxWidth(420);

        HBox chips = new HBox(8, featureChip("Bilgisayar rakip"), featureChip("6 tahta boyutu"), featureChip("Takas kuralı"));
        VBox hero = new VBox(14, logo, titleBox, tagline, blurb, chips);
        hero.setAlignment(Pos.CENTER_LEFT);
        hero.setMaxWidth(470);

        // ---------------------------------------------------------------- setup card
        Segmented<GameConfig.Mode> mode = new Segmented<>(List.of(GameConfig.Mode.PVP, GameConfig.Mode.AI),
                m -> m == GameConfig.Mode.PVP ? "İki Oyuncu" : "Bilgisayara Karşı", initial.mode());

        boolean pvpInit = initial.mode() == GameConfig.Mode.PVP;
        TextField redName = field("Kırmızı oyuncunun adı", pvpInit && initial.seatAColor() == Player.RED ? initial.nameA() : "Oyuncu 1");
        TextField blueName = field("Mavi oyuncunun adı", pvpInit ? initial.nameB() : "Oyuncu 2");
        if (pvpInit && initial.seatAColor() == Player.BLUE) {
            redName.setText(initial.nameB());
            blueName.setText(initial.nameA());
        }
        VBox pvpBox = new VBox(10, nameRow(Player.RED, redName), nameRow(Player.BLUE, blueName));

        TextField humanName = field("Adın", pvpInit ? "Oyuncu" : initial.nameA());
        Segmented<Player> color = new Segmented<>(List.of(Player.RED, Player.BLUE),
                p -> p == Player.RED ? "Kırmızı · ilk hamle" : "Mavi", initial.seatAColor());
        Segmented<Difficulty> difficulty = new Segmented<>(Arrays.asList(Difficulty.values()), Difficulty::label, initial.difficulty());
        VBox aiBox = new VBox(10, nameRow(null, humanName), section("RENGİN", color), section("ZORLUK", difficulty));

        StackPane playersSlot = new StackPane();
        playersSlot.setAlignment(Pos.TOP_LEFT);
        Runnable applyMode = () -> {
            Node show = mode.getValue() == GameConfig.Mode.PVP ? pvpBox : aiBox;
            playersSlot.getChildren().setAll(show);
            Fx.reveal(show, 0, 10);
        };
        playersSlot.getChildren().setAll(pvpInit ? pvpBox : aiBox);
        mode.valueProperty().addListener((o, a, b) -> applyMode.run());

        List<Integer> sizes = new ArrayList<>();
        for (int s : GameConfig.SIZES) {
            sizes.add(s);
        }
        Segmented<Integer> size = new Segmented<>(sizes, s -> s + "×" + s, initial.size());

        ToggleSwitch swap = new ToggleSwitch(initial.swapRule());
        Label swapTitle = new Label("Takas kuralı");
        swapTitle.getStyleClass().add("row-title");
        Label swapHelp = new Label("İlk taşın avantajını dengeler: mavi, kırmızının ilk taşını devralabilir.");
        swapHelp.getStyleClass().add("muted-small");
        swapHelp.setWrapText(true);
        VBox swapText = new VBox(2, swapTitle, swapHelp);
        HBox.setHgrow(swapText, Priority.ALWAYS);
        HBox swapRow = new HBox(16, swapText, swap);
        swapRow.setAlignment(Pos.CENTER_LEFT);
        swapRow.getStyleClass().add("option-row");

        Button start = new Button("Oyuna başla", Icons.icon(Icons.PLAY, 20, Color.WHITE));
        start.getStyleClass().add("btn-primary");
        start.setMaxWidth(Double.MAX_VALUE);
        start.setDefaultButton(true);
        Fx.interactive(start, 1.03);
        start.setOnAction(e -> {
            GameConfig cfg;
            if (mode.getValue() == GameConfig.Mode.PVP) {
                cfg = new GameConfig(size.getValue(), GameConfig.Mode.PVP, difficulty.getValue(), swap.isSelected(),
                        orDefault(redName, "Oyuncu 1"), orDefault(blueName, "Oyuncu 2"), Player.RED);
            } else {
                cfg = new GameConfig(size.getValue(), GameConfig.Mode.AI, difficulty.getValue(), swap.isSelected(),
                        orDefault(humanName, "Oyuncu"), orDefault(blueName, "Oyuncu 2"), color.getValue());
            }
            onStart.accept(cfg);
        });

        Button rules = new Button("Nasıl oynanır?", Icons.icon(Icons.HELP, 16, Theme.MUTED));
        rules.getStyleClass().add("btn-link");
        Label keys = new Label("F11 tam ekran · Ctrl+Z geri al · H ipucu");
        keys.getStyleClass().add("muted-small");
        Region grow = new Region();
        HBox.setHgrow(grow, Priority.ALWAYS);
        HBox footer = new HBox(rules, grow, keys);
        footer.setAlignment(Pos.CENTER_LEFT);

        Label cardTitle = new Label("Yeni oyun");
        cardTitle.getStyleClass().add("card-title");

        VBox card = new VBox(18,
                cardTitle,
                section("MOD", mode),
                section("OYUNCULAR", playersSlot),
                section("TAHTA BOYUTU", size),
                swapRow,
                start,
                footer);
        card.getStyleClass().addAll("card", "setup-card");
        card.setPrefWidth(470);
        card.setMaxWidth(470);
        card.setMaxHeight(Region.USE_PREF_SIZE);

        content = new HBox(80, hero, card);
        content.setAlignment(Pos.CENTER);
        content.setPadding(new Insets(32));
        // never squash: layout at natural size, layoutChildren() scales it down to fit
        content.setMinSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        card.setMinHeight(Region.USE_PREF_SIZE);
        setMinSize(0, 0); // ...but let this view itself follow the window size
        getChildren().add(content);

        rules.setOnAction(e -> showRules());

        // ---------------------------------------------------------------- entrance
        int i = 0;
        for (Node node : hero.getChildren()) {
            Fx.reveal(node, 60 + i++ * 90, 26);
        }
        i = 0;
        for (Node node : card.getChildren()) {
            Fx.reveal(node, 180 + i++ * 60, 18);
        }
    }

    // ------------------------------------------------------------------ building blocks

    private static TextField field(String prompt, String value) {
        TextField tf = new TextField(value);
        tf.setPromptText(prompt);
        tf.getStyleClass().add("field");
        HBox.setHgrow(tf, Priority.ALWAYS);
        tf.textProperty().addListener((o, a, b) -> {
            if (b.length() > 18) {
                tf.setText(b.substring(0, 18));
            }
        });
        return tf;
    }

    private static String orDefault(TextField tf, String fallback) {
        String s = tf.getText() == null ? "" : tf.getText().trim();
        return s.isEmpty() ? fallback : s;
    }

    private static HBox nameRow(Player p, TextField tf) {
        StackPane dot = new StackPane();
        Polygon hex = Fx.hexagon(13);
        if (p == null) {
            hex.setFill(Fx.brandGradient());
        } else {
            hex.setFill(Theme.color(p));
            hex.setEffect(new DropShadow(BlurType.GAUSSIAN, Theme.color(p), 12, 0.3, 0, 0));
        }
        dot.getChildren().add(hex);
        dot.setMinWidth(30);
        HBox row = new HBox(12, dot, tf);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private static VBox section(String caption, Node body) {
        Label l = new Label(caption);
        l.getStyleClass().add("section-title");
        return new VBox(8, l, body);
    }

    private static Label featureChip(String s) {
        Label l = new Label(s);
        l.getStyleClass().add("feature-chip");
        return l;
    }

    /** Seven-hex flower that slowly spins while its petals breathe. */
    private Node buildLogo() {
        Group flower = new Group();
        double r = 17;
        double d = r * Fx.SQRT3 + 3;
        double[][] offsets = new double[7][2];
        for (int k = 0; k < 6; k++) {
            double a = Math.toRadians(60 * k - 30);
            offsets[k + 1][0] = Math.cos(a) * d;
            offsets[k + 1][1] = Math.sin(a) * d;
        }
        for (int k = 0; k < 7; k++) {
            Polygon hex = Fx.hexagon(r);
            hex.setLayoutX(offsets[k][0]);
            hex.setLayoutY(offsets[k][1]);
            Color c = k == 0 ? Color.WHITE : (k % 2 == 0 ? Theme.RED : Theme.BLUE);
            hex.setFill(new LinearGradient(0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
                    new Stop(0, c.interpolate(Color.WHITE, 0.35)), new Stop(1, c.deriveColor(0, 1, 0.8, 1))));
            hex.setOpacity(k == 0 ? 0.95 : 0.9);
            flower.getChildren().add(hex);
            ScaleTransition breathe = new ScaleTransition(Duration.millis(1400), hex);
            breathe.setFromX(1);
            breathe.setFromY(1);
            breathe.setToX(0.8);
            breathe.setToY(0.8);
            breathe.setAutoReverse(true);
            breathe.setCycleCount(Animation.INDEFINITE);
            breathe.setDelay(Duration.millis(k * 160));
            breathe.play();
            loops.add(breathe);
        }
        // soft glow drawn as a gradient (no per-frame blur while the petals animate)
        Circle aura = new Circle(70, new RadialGradient(0, 0, 0.5, 0.5, 0.5, true, CycleMethod.NO_CYCLE,
                new Stop(0, Theme.VIOLET.deriveColor(0, 1, 1, 0.45)), new Stop(1, Color.TRANSPARENT)));
        aura.setMouseTransparent(true);
        RotateTransition spin = new RotateTransition(Duration.seconds(24), flower);
        spin.setByAngle(360);
        spin.setCycleCount(Animation.INDEFINITE);
        spin.setInterpolator(javafx.animation.Interpolator.LINEAR);
        spin.play();
        loops.add(spin);
        StackPane holder = new StackPane(aura, flower);
        holder.setMinSize(120, 120);
        holder.setMaxSize(120, 120);
        return holder;
    }

    private void showRules() {
        Label title = new Label("Nasıl oynanır?");
        title.getStyleClass().add("card-title");
        VBox steps = new VBox(14,
                step("1", "Sırayla boş bir altıgene taş koyun. Kırmızı başlar."),
                step("2", "Kırmızı sol ve sağ kenarı, mavi üst ve alt kenarı birleştiren kesintisiz bir zincir kurmaya çalışır."),
                step("3", "Her altıgenin altı komşusu vardır. Zincir bu komşuluklar üzerinden ilerler."),
                step("4", "Hex'te beraberlik yoktur: tahta dolmadan biri mutlaka kazanır."),
                step("5", "Takas kuralı açıksa mavi ilk hamlesinde kırmızının taşını kendi rengine çevirebilir. Böylece güçlü bir açılış cezalandırılır."));
        Label tip = new Label("İpucu: iki taş arasında iki ortak boş komşu bırakan “köprü” hamlesi kesilemez. Oyunun en güçlü taktiğidir.");
        tip.getStyleClass().add("tip-box");
        tip.setWrapText(true);
        Button ok = new Button("Anladım");
        ok.getStyleClass().add("btn-primary");
        ok.setMaxWidth(Double.MAX_VALUE);
        VBox body = new VBox(18, title, steps, tip, ok);
        body.setPrefWidth(520);
        Modal modal = new Modal(content, true, body);
        ok.setOnAction(e -> modal.close());
        modal.show(this);
    }

    private static HBox step(String number, String text) {
        Label n = new Label(number);
        n.getStyleClass().add("step-number");
        n.setMinSize(30, 30);
        n.setAlignment(Pos.CENTER);
        Label t = new Label(text);
        t.setWrapText(true);
        t.getStyleClass().add("step-text");
        HBox row = new HBox(14, n, t);
        row.setAlignment(Pos.TOP_LEFT);
        return row;
    }

    /** Shrinks the whole layout uniformly on small windows instead of clipping or squashing it. */
    @Override
    protected void layoutChildren() {
        super.layoutChildren();
        double pw = content.prefWidth(-1);
        double ph = content.prefHeight(-1);
        double s = Math.min(1, Math.min(getWidth() / pw, getHeight() / ph));
        content.setScaleX(s);
        content.setScaleY(s);
    }

    public void dispose() {
        loops.forEach(Animation::stop);
    }
}
