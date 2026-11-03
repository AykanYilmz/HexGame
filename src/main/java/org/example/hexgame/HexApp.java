package org.example.hexgame;

import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Application;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.StackPane;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.example.hexgame.model.GameConfig;
import org.example.hexgame.model.Settings;
import org.example.hexgame.ui.AnimatedBackground;
import org.example.hexgame.ui.Fx;
import org.example.hexgame.ui.GameView;
import org.example.hexgame.ui.MenuView;
import org.example.hexgame.ui.Theme;

import java.util.Objects;

/** Application entry point and screen navigation. */
public class HexApp extends Application {

    private Stage stage;
    private final AnimatedBackground background = new AnimatedBackground();
    private final StackPane screens = new StackPane();

    @Override
    public void start(Stage stage) {
        this.stage = stage;
        StackPane root = new StackPane(background, screens);
        root.getStyleClass().add("app-root");

        Rectangle2D vb = Screen.getPrimary().getVisualBounds();
        double w = Math.min(1440, vb.getWidth() * 0.92);
        double h = Math.min(880, vb.getHeight() * 0.92);
        Scene scene = new Scene(root, w, h);
        scene.setFill(Theme.BG);
        scene.getStylesheets().add(Objects.requireNonNull(
                HexApp.class.getResource("/org/example/hexgame/styles.css")).toExternalForm());
        scene.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (e.getCode() == KeyCode.F11) {
                toggleFullScreen();
                e.consume();
            }
        });

        stage.setTitle("Hex");
        stage.getIcons().addAll(Fx.appIcon(256), Fx.appIcon(64), Fx.appIcon(32));
        stage.setMinWidth(1024);
        stage.setMinHeight(680);
        stage.setFullScreenExitHint("");
        stage.setFullScreenExitKeyCombination(KeyCombination.keyCombination("Esc"));
        stage.setScene(scene);
        stage.show();
        showMenu();
    }

    private void showMenu() {
        background.setCalm(false);
        navigate(new MenuView(Settings.load(), this::startGame), false);
    }

    private void startGame(GameConfig config) {
        Settings.save(config);
        background.setCalm(true);
        navigate(new GameView(config, this::showMenu, this::toggleFullScreen), true);
    }

    private void toggleFullScreen() {
        stage.setFullScreen(!stage.isFullScreen());
    }

    /** Cross-fades between screens with a short directional slide. */
    private void navigate(Node next, boolean forward) {
        Node old = screens.getChildren().isEmpty() ? null : screens.getChildren().get(screens.getChildren().size() - 1);
        screens.getChildren().add(next);
        next.setOpacity(0);
        next.setTranslateX(forward ? 60 : -60);
        FadeTransition in = new FadeTransition(Duration.millis(380), next);
        in.setToValue(1);
        TranslateTransition slideIn = new TranslateTransition(Duration.millis(480), next);
        slideIn.setToX(0);
        slideIn.setInterpolator(Fx.EASE_OUT_CUBIC);
        new ParallelTransition(in, slideIn).play();

        if (old != null) {
            old.setMouseTransparent(true);
            FadeTransition out = new FadeTransition(Duration.millis(280), old);
            out.setToValue(0);
            TranslateTransition slideOut = new TranslateTransition(Duration.millis(380), old);
            slideOut.setToX(forward ? -60 : 60);
            ParallelTransition pt = new ParallelTransition(out, slideOut);
            pt.setOnFinished(e -> {
                screens.getChildren().remove(old);
                if (old instanceof MenuView menu) {
                    menu.dispose();
                } else if (old instanceof GameView game) {
                    game.dispose();
                }
            });
            pt.play();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
