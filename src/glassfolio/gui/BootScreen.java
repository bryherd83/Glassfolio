package glassfolio.gui;

import java.util.List;
import java.util.Objects;
import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.PauseTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.TranslateTransition;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.util.Duration;

/**
 * The startup screen: three bars in the chart's colors rise like a small
 * rising chart while the chime plays, the wordmark appears under them, and the
 * screen fades into the start screen. A click or any key skips it.
 */
public final class BootScreen {
    private static final double BAR_WIDTH = 14;
    private static final double[] BAR_HEIGHTS = {38, 62, 92};
    private static final List<String> BAR_COLORS = List.of("-series-1", "-series-2", "-series-3");
    private static final double BAR_SPACING = 8;
    private static final double BAR_ARC = 4;
    private static final double LOGO_SPACING = 20;
    private static final Duration BAR_RISE = Duration.millis(420);
    private static final Duration BAR_STAGGER = Duration.millis(160); // matches the chime's note spacing
    private static final Duration WORDMARK_DELAY = Duration.millis(620);
    private static final Duration WORDMARK_FADE = Duration.millis(600);
    private static final double WORDMARK_RISE = 12;
    private static final Duration HOLD = Duration.millis(300);
    private static final Duration FADE_OUT = Duration.millis(450);
    private static final String WORDMARK_STYLE = Styles.HEADING + "-fx-font-size: 42px; -fx-text-fill: -text;";

    private final Region screenRoot;
    private final VBox logo;
    private final HBox bars = new HBox(BAR_SPACING);
    private final Label wordmark = new Label("Glassfolio");
    private final Runnable onDone;
    private SequentialTransition sequence;
    private boolean finished;

    /**
     * Builds the startup screen.
     *
     * @param onDone called once, when the animation ends or is skipped
     * @throws NullPointerException if onDone is null
     */
    public BootScreen(Runnable onDone) {
        this.onDone = Objects.requireNonNull(onDone, "onDone");
        bars.setAlignment(Pos.BOTTOM_CENTER);
        for (int i = 0; i < BAR_HEIGHTS.length; i++) {
            Rectangle bar = new Rectangle(BAR_WIDTH, BAR_HEIGHTS[i]);
            bar.setArcWidth(BAR_ARC);
            bar.setArcHeight(BAR_ARC);
            bar.setStyle("-fx-fill: " + BAR_COLORS.get(i) + ";");
            bar.setScaleY(0);
            bar.setTranslateY(BAR_HEIGHTS[i] / 2); // grow up from the bottom edge, not the middle
            bars.getChildren().add(bar);
        }
        bars.setMinHeight(BAR_HEIGHTS[BAR_HEIGHTS.length - 1]);
        wordmark.setStyle(WORDMARK_STYLE);
        wordmark.setOpacity(0);
        logo = new VBox(LOGO_SPACING, bars, wordmark);
        logo.setAlignment(Pos.CENTER);

        StackPane root = new StackPane(logo);
        root.setStyle(Styles.BACKGROUND);
        root.addEventFilter(MouseEvent.MOUSE_CLICKED, e -> finish());
        root.addEventFilter(KeyEvent.KEY_PRESSED, e -> finish());
        screenRoot = root;
    }

    /**
     * Shows the screen, plays the chime and the animation.
     *
     * @param stage the app's window
     */
    public void show(Stage stage) {
        stage.getScene().setRoot(screenRoot); // same Scene, so full screen stays on
        screenRoot.requestFocus(); // so a key press can skip
        Chime.play();

        ParallelTransition rise = new ParallelTransition();
        for (int i = 0; i < bars.getChildren().size(); i++) {
            var bar = bars.getChildren().get(i);
            ScaleTransition grow = new ScaleTransition(BAR_RISE, bar);
            grow.setToY(1);
            TranslateTransition anchor = new TranslateTransition(BAR_RISE, bar);
            anchor.setToY(0);
            ParallelTransition one = new ParallelTransition(grow, anchor);
            one.setDelay(BAR_STAGGER.multiply(i));
            rise.getChildren().add(one);
        }

        FadeTransition showWord = new FadeTransition(WORDMARK_FADE, wordmark);
        showWord.setToValue(1);
        TranslateTransition liftWord = new TranslateTransition(WORDMARK_FADE, wordmark);
        liftWord.setFromY(WORDMARK_RISE);
        liftWord.setToY(0);
        ParallelTransition word = new ParallelTransition(showWord, liftWord);
        word.setDelay(WORDMARK_DELAY);

        FadeTransition fadeOut = new FadeTransition(FADE_OUT, logo);
        fadeOut.setToValue(0);

        sequence = new SequentialTransition(new ParallelTransition(rise, word), new PauseTransition(HOLD), fadeOut);
        sequence.setOnFinished(e -> finish());
        sequence.play();
    }

    /** Ends the screen once, whether the animation finished or the user skipped it. */
    private void finish() {
        if (finished) {
            return;
        }
        finished = true;
        if (sequence != null) {
            sequence.stop();
        }
        onDone.run();
    }
}
