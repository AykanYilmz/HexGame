package io.github.aykanyilmz.hexgame.ui;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.CacheHint;
import javafx.scene.Group;
import javafx.scene.control.Label;
import javafx.scene.effect.BlurType;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.StrokeLineCap;
import javafx.util.Duration;
import io.github.aykanyilmz.hexgame.model.Player;

/** Side panel showing one colour's player, goal, stats and turn state. */
public class PlayerCard extends VBox {

    private final Player color;
    private final Label name = new Label();
    private final Label badge = new Label();
    private final Label moves = new Label("0");
    private final Label time = new Label("00:00");
    private final Label score = new Label("0");
    private final Label thinking = new Label();
    private final StackPane avatar = new StackPane();
    private final ScaleTransition pulse;
    private final Timeline dots;
    private boolean active;

    public PlayerCard(Player color) {
        this.color = color;
        getStyleClass().addAll("card", "player-card");
        setSpacing(16);
        setAlignment(Pos.TOP_CENTER);
        setPrefWidth(236);
        setMinWidth(236);
        setMaxWidth(250);

        Polygon hex = Fx.hexagon(34);
        hex.setFill(new LinearGradient(0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Theme.light(color)), new Stop(0.5, Theme.color(color)), new Stop(1, Theme.dark(color))));
        hex.setEffect(new DropShadow(BlurType.GAUSSIAN, Theme.color(color).deriveColor(0, 1, 1, 0.6), 22, 0.2, 0, 0));
        hex.setCache(true); // shadow rendered once, reused while the avatar animates
        hex.setCacheHint(CacheHint.SCALE);
        avatar.getChildren().add(hex);
        avatar.setMinSize(76, 76);

        name.getStyleClass().add("player-name");
        name.setMaxWidth(210);
        badge.getStyleClass().addAll("badge", color == Player.RED ? "badge-red" : "badge-blue");
        badge.textProperty().bind(I18n.text(color == Player.RED ? "card.badge.red" : "card.badge.blue"));

        VBox goal = new VBox(8, sectionLabel("card.goal"), goalDiagram(),
                muted(color == Player.RED ? "card.goal.red" : "card.goal.blue"));
        goal.setAlignment(Pos.CENTER);

        HBox stats = new HBox(stat(moves, "card.moves"), divider(), stat(time, "card.time"), divider(), stat(score, "card.wins"));
        stats.setAlignment(Pos.CENTER);
        stats.getStyleClass().add("stats-row");

        thinking.getStyleClass().add("thinking");
        thinking.setVisible(false);

        getChildren().addAll(avatar, name, badge, goal, stats, thinking);

        // one-shot "your turn" bounce; a looping pulse would force the card's large drop
        // shadow to be recomputed every frame
        pulse = new ScaleTransition(Duration.millis(260), avatar);
        pulse.setFromX(1);
        pulse.setFromY(1);
        pulse.setToX(1.14);
        pulse.setToY(1.14);
        pulse.setAutoReverse(true);
        pulse.setCycleCount(2);
        pulse.setInterpolator(Fx.EASE_OUT_CUBIC);

        int[] i = {0};
        dots = new Timeline(new KeyFrame(Duration.millis(300),
                e -> thinking.setText(I18n.get("card.thinking") + ".".repeat(i[0]++ % 4))));
        dots.setCycleCount(Animation.INDEFINITE);
    }

    private static Label sectionLabel(String key) {
        Label l = new Label();
        l.textProperty().bind(I18n.text(key));
        l.getStyleClass().add("section-title");
        return l;
    }

    private static Label muted(String key) {
        Label l = new Label();
        l.textProperty().bind(I18n.text(key));
        l.getStyleClass().add("muted");
        l.setWrapText(true);
        return l;
    }

    private static VBox stat(Label value, String captionKey) {
        value.getStyleClass().add("stat-value");
        Label cap = new Label();
        cap.textProperty().bind(I18n.text(captionKey));
        cap.getStyleClass().add("stat-label");
        value.setMinWidth(Region.USE_PREF_SIZE);
        cap.setMinWidth(Region.USE_PREF_SIZE);
        VBox box = new VBox(2, value, cap);
        box.setAlignment(Pos.CENTER);
        HBox.setHgrow(box, Priority.ALWAYS);
        box.setMaxWidth(Double.MAX_VALUE);
        return box;
    }

    private static Region divider() {
        Region r = new Region();
        r.getStyleClass().add("v-divider");
        r.setPrefWidth(1);
        r.setMinWidth(1);
        HBox.setMargin(r, new Insets(4, 4, 4, 4));
        return r;
    }

    /** Miniature board outline with this player's two target sides lit up. */
    private Group goalDiagram() {
        double w = 70;
        double h = 42;
        double off = w / 2;
        double[][] corners = {{0, 0}, {w, 0}, {w + off, h}, {off, h}};
        Polygon fill = new Polygon(0, 0, w, 0, w + off, h, off, h);
        fill.setFill(Color.rgb(255, 255, 255, 0.04));
        Group g = new Group(fill);
        for (int i = 0; i < 4; i++) {
            double[] a = corners[i];
            double[] b = corners[(i + 1) % 4];
            boolean horizontal = i % 2 == 0;
            Player owner = horizontal ? Player.BLUE : Player.RED;
            Line l = new Line(a[0], a[1], b[0], b[1]);
            l.setStrokeLineCap(StrokeLineCap.ROUND);
            l.setStrokeWidth(owner == color ? 5 : 2);
            l.setStroke(owner == color ? Theme.color(color) : Color.rgb(255, 255, 255, 0.14));
            if (owner == color) {
                l.setEffect(new DropShadow(BlurType.GAUSSIAN, Theme.color(color), 10, 0.3, 0, 0));
            }
            g.getChildren().add(l);
        }
        return g;
    }

    public void update(String playerName, boolean ai, int moveCount, long timeMs, int wins) {
        name.setText(playerName);
        avatar.getChildren().removeIf(node -> node instanceof StackPane);
        avatar.getChildren().add(Icons.icon(ai ? Icons.ROBOT : Icons.PERSON, 30, Color.rgb(255, 255, 255, 0.92)));
        moves.setText(String.valueOf(moveCount));
        time.setText(Fx.clock(timeMs));
        score.setText(String.valueOf(wins));
    }

    public void setTime(long timeMs) {
        time.setText(Fx.clock(timeMs));
    }

    public void bumpScore() {
        ScaleTransition st = new ScaleTransition(Duration.millis(240), score);
        st.setFromX(1);
        st.setFromY(1);
        st.setToX(1.6);
        st.setToY(1.6);
        st.setAutoReverse(true);
        st.setCycleCount(2);
        st.play();
    }

    public void setActive(boolean on) {
        if (on == active) {
            return;
        }
        active = on;
        String cls = color == Player.RED ? "active-red" : "active-blue";
        getStyleClass().remove(cls);
        if (on) {
            getStyleClass().add(cls);
            pulse.playFromStart();
        } else {
            pulse.stop();
            avatar.setScaleX(1);
            avatar.setScaleY(1);
        }
        ScaleTransition st = new ScaleTransition(Duration.millis(300), this);
        st.setToX(on ? 1.03 : 0.97);
        st.setToY(on ? 1.03 : 0.97);
        st.setInterpolator(Fx.EASE_OUT_BACK);
        st.play();
        setOpacity(on ? 1 : 0.72);
    }

    public void setThinking(boolean on) {
        thinking.setVisible(on);
        if (on) {
            dots.play();
        } else {
            dots.stop();
        }
    }

    public void stopAnimations() {
        pulse.stop();
        dots.stop();
    }
}
