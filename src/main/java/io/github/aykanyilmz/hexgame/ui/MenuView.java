package io.github.aykanyilmz.hexgame.ui;

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
import io.github.aykanyilmz.hexgame.model.Difficulty;
import io.github.aykanyilmz.hexgame.model.GameConfig;
import io.github.aykanyilmz.hexgame.model.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import javafx.util.Subscription;

/** Landing screen: animated hero on the left, match setup card on the right. */
public class MenuView extends StackPane {

    private final List<Animation> loops = new ArrayList<>();
    private final HBox content;
    private final Subscription languageSubscription;

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

        Label tagline = text("menu.tagline");
        tagline.getStyleClass().add("tagline");
        tagline.setWrapText(true);
        tagline.setMaxWidth(470);
        Label blurb = text("menu.blurb");
        blurb.getStyleClass().add("muted");
        blurb.setWrapText(true);
        blurb.setMaxWidth(420);

        HBox chips = new HBox(8, featureChip("menu.chip.ai"), featureChip("menu.chip.sizes"), featureChip("menu.chip.swap"));
        VBox hero = new VBox(14, logo, titleBox, tagline, blurb, chips);
        hero.setAlignment(Pos.CENTER_LEFT);
        hero.setMaxWidth(470);

        // ---------------------------------------------------------------- setup card
        Segmented<GameConfig.Mode> mode = new Segmented<>(List.of(GameConfig.Mode.PVP, GameConfig.Mode.AI),
                m -> I18n.get("mode." + m.name()), initial.mode());

        boolean pvpInit = initial.mode() == GameConfig.Mode.PVP;
        TextField redName = field("menu.prompt.red", pvpInit && initial.seatAColor() == Player.RED ? initial.nameA() : I18n.get("name.player1"));
        TextField blueName = field("menu.prompt.blue", pvpInit ? initial.nameB() : I18n.get("name.player2"));
        if (pvpInit && initial.seatAColor() == Player.BLUE) {
            redName.setText(initial.nameB());
            blueName.setText(initial.nameA());
        }
        VBox pvpBox = new VBox(10, nameRow(Player.RED, redName), nameRow(Player.BLUE, blueName));

        TextField humanName = field("menu.prompt.human", pvpInit ? I18n.get("name.player") : initial.nameA());
        for (TextField f : List.of(redName, blueName, humanName)) {
            f.setText(I18n.playerName(f.getText()));
        }
        Segmented<Player> color = new Segmented<>(List.of(Player.RED, Player.BLUE),
                p -> I18n.get(p == Player.RED ? "menu.color.red" : "menu.color.blue"), initial.seatAColor());
        Segmented<Difficulty> difficulty = new Segmented<>(Arrays.asList(Difficulty.values()),
                d -> I18n.get("difficulty." + d.name()), initial.difficulty());
        VBox aiBox = new VBox(10, nameRow(null, humanName), section("menu.section.color", color),
                section("menu.section.difficulty", difficulty));

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
        Label swapTitle = text("menu.swap.title");
        swapTitle.getStyleClass().add("row-title");
        Label swapHelp = text("menu.swap.help");
        swapHelp.getStyleClass().add("muted-small");
        swapHelp.setWrapText(true);
        VBox swapText = new VBox(2, swapTitle, swapHelp);
        HBox.setHgrow(swapText, Priority.ALWAYS);
        HBox swapRow = new HBox(16, swapText, swap);
        swapRow.setAlignment(Pos.CENTER_LEFT);
        swapRow.getStyleClass().add("option-row");

        Button start = new Button(null, Icons.icon(Icons.PLAY, 20, Color.WHITE));
        start.textProperty().bind(I18n.text("menu.start"));
        start.getStyleClass().add("btn-primary");
        start.setMaxWidth(Double.MAX_VALUE);
        start.setDefaultButton(true);
        Fx.interactive(start, 1.03);
        start.setOnAction(e -> {
            GameConfig cfg;
            if (mode.getValue() == GameConfig.Mode.PVP) {
                cfg = new GameConfig(size.getValue(), GameConfig.Mode.PVP, difficulty.getValue(), swap.isSelected(),
                        orDefault(redName, "name.player1"), orDefault(blueName, "name.player2"), Player.RED);
            } else {
                cfg = new GameConfig(size.getValue(), GameConfig.Mode.AI, difficulty.getValue(), swap.isSelected(),
                        orDefault(humanName, "name.player"), orDefault(blueName, "name.player2"), color.getValue());
            }
            onStart.accept(cfg);
        });

        Button rules = new Button(null, Icons.icon(Icons.HELP, 16, Theme.MUTED));
        rules.textProperty().bind(I18n.text("menu.rules"));
        rules.getStyleClass().add("btn-link");
        Label keys = text("menu.keys");
        keys.getStyleClass().add("muted-small");
        Region grow = new Region();
        HBox.setHgrow(grow, Priority.ALWAYS);
        HBox footer = new HBox(rules, grow, keys);
        footer.setAlignment(Pos.CENTER_LEFT);

        Label cardTitle = text("menu.newGame");
        cardTitle.getStyleClass().add("card-title");

        VBox card = new VBox(18,
                cardTitle,
                section("menu.section.mode", mode),
                section("menu.section.players", playersSlot),
                section("menu.section.size", size),
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
        LanguageSwitch languageSwitch = new LanguageSwitch();
        StackPane.setAlignment(languageSwitch, Pos.TOP_RIGHT);
        StackPane.setMargin(languageSwitch, new Insets(20, 24, 0, 0));
        getChildren().addAll(content, languageSwitch);

        rules.setOnAction(e -> showRules());

        // Option captions and untouched default names follow the interface language.
        languageSubscription = I18n.onChange(() -> {
            mode.refreshLabels();
            color.refreshLabels();
            difficulty.refreshLabels();
            for (TextField f : List.of(redName, blueName, humanName)) {
                f.setText(I18n.playerName(f.getText()));
            }
        });

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

    /** A label whose text follows the interface language. */
    private static Label text(String key) {
        Label l = new Label();
        l.textProperty().bind(I18n.text(key));
        return l;
    }

    private static TextField field(String promptKey, String value) {
        TextField tf = new TextField(value);
        tf.promptTextProperty().bind(I18n.text(promptKey));
        tf.getStyleClass().add("field");
        HBox.setHgrow(tf, Priority.ALWAYS);
        tf.textProperty().addListener((o, a, b) -> {
            if (b.length() > 18) {
                tf.setText(b.substring(0, 18));
            }
        });
        return tf;
    }

    private static String orDefault(TextField tf, String fallbackKey) {
        String s = tf.getText() == null ? "" : tf.getText().trim();
        return s.isEmpty() ? I18n.get(fallbackKey) : s;
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

    private static VBox section(String captionKey, Node body) {
        Label l = text(captionKey);
        l.getStyleClass().add("section-title");
        return new VBox(8, l, body);
    }

    private static Label featureChip(String key) {
        Label l = text(key);
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
        Label title = text("rules.title");
        title.getStyleClass().add("card-title");
        VBox steps = new VBox(14,
                step("1", "rules.step1"),
                step("2", "rules.step2"),
                step("3", "rules.step3"),
                step("4", "rules.step4"),
                step("5", "rules.step5"));
        Label tip = text("rules.tip");
        tip.getStyleClass().add("tip-box");
        tip.setWrapText(true);
        Button ok = new Button();
        ok.textProperty().bind(I18n.text("rules.ok"));
        ok.getStyleClass().add("btn-primary");
        ok.setMaxWidth(Double.MAX_VALUE);
        VBox body = new VBox(18, title, steps, tip, ok);
        body.setPrefWidth(520);
        Modal modal = new Modal(content, true, body);
        ok.setOnAction(e -> modal.close());
        modal.show(this);
    }

    private static HBox step(String number, String textKey) {
        Label n = new Label(number);
        n.getStyleClass().add("step-number");
        n.setMinSize(30, 30);
        n.setAlignment(Pos.CENTER);
        Label t = text(textKey);
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
        languageSubscription.unsubscribe();
        loops.forEach(Animation::stop);
    }
}
