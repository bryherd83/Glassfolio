package glassfolio.gui;

import java.util.function.DoubleFunction;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.util.Duration;

/**
 * The small animations every screen shares. Each one runs once in response to
 * something (a screen opening, the mouse entering or pressing), never on a loop.
 */
public final class Motion {
    private static final Duration ENTRANCE = Duration.millis(450);
    private static final double ENTRANCE_OFFSET = 30;
    private static final Duration HOVER = Duration.millis(150);
    private static final double HOVER_RISE = -2;
    private static final double PRESS_SCALE = 0.95;
    private static final Duration PRESS = Duration.millis(80);
    private static final Duration ROW_FADE = Duration.millis(350);
    private static final Duration ROW_STAGGER = Duration.millis(120);
    private static final double ROW_OFFSET = 16;
    private static final Duration LIFT = Duration.millis(180);
    private static final double LIFT_HEIGHT = -4;
    private static final Duration COUNT = Duration.millis(900);
    // fast start, slow finish, so the number settles instead of stopping hard
    private static final Interpolator EASE_OUT = Interpolator.SPLINE(0.2, 0.8, 0.2, 1);

    private Motion() {
    }

    /**
     * Fades a node in while sliding it up. Call when its screen is shown.
     *
     * @param node the node to animate
     */
    public static void playEntrance(Node node) {
        FadeTransition fade = new FadeTransition(ENTRANCE, node);
        fade.setFromValue(0);
        fade.setToValue(1);
        TranslateTransition slide = new TranslateTransition(ENTRANCE, node);
        slide.setFromY(ENTRANCE_OFFSET);
        slide.setToY(0);
        slide.setInterpolator(EASE_OUT);
        new ParallelTransition(fade, slide).play();
    }

    /**
     * Makes a button rise a little while the mouse is over it and squish
     * while it is pressed, so a click feels physical.
     *
     * @param button the button
     */
    public static void addHoverPop(Button button) {
        ScaleTransition scale = new ScaleTransition(HOVER, button);
        TranslateTransition rise = new TranslateTransition(HOVER, button);
        rise.setInterpolator(EASE_OUT);
        button.setOnMouseEntered(e -> riseTo(rise, HOVER_RISE));
        button.setOnMouseExited(e -> riseTo(rise, 0));
        button.setOnMousePressed(e -> scaleTo(scale, PRESS, PRESS_SCALE));
        button.setOnMouseReleased(e -> scaleTo(scale, HOVER, 1));
    }

    /**
     * Restarts a rise animation from wherever the node is now.
     *
     * @param rise   the node's move animation
     * @param target how far up to end, 0 for its normal place
     */
    private static void riseTo(TranslateTransition rise, double target) {
        rise.stop();
        rise.setToY(target);
        rise.playFromStart();
    }

    /**
     * Restarts a scale animation from wherever the node is now.
     *
     * @param scale    the node's scale animation
     * @param duration how long it takes
     * @param target   the size to end at, 1 for normal
     */
    private static void scaleTo(ScaleTransition scale, Duration duration, double target) {
        scale.stop();
        scale.setDuration(duration);
        scale.setToX(target);
        scale.setToY(target);
        scale.playFromStart();
    }

    /**
     * Makes a card float up a little while the mouse is over it. It moves by whole
     * pixels and uses no effect, so the card's text stays sharp.
     *
     * @param card the card
     */
    public static void addHoverLift(Node card) {
        Timeline lift = new Timeline();
        card.hoverProperty().addListener((obs, wasHover, hover) -> {
            lift.stop();
            lift.getKeyFrames().setAll(new KeyFrame(LIFT,
                    new KeyValue(card.translateYProperty(), hover ? LIFT_HEIGHT : 0, EASE_OUT)));
            lift.playFromStart();
        });
    }

    /**
     * Fades a node in and slides it up after a delay that grows with its
     * position, so a list of nodes appears one after another.
     *
     * @param node  the node to animate
     * @param order its position, 0 for the first
     */
    public static void fadeInStaggered(Node node, int order) {
        node.setOpacity(0);
        FadeTransition fade = new FadeTransition(ROW_FADE, node);
        fade.setFromValue(0);
        fade.setToValue(1);
        TranslateTransition slide = new TranslateTransition(ROW_FADE, node);
        slide.setFromY(ROW_OFFSET);
        slide.setToY(0);
        slide.setInterpolator(EASE_OUT);
        ParallelTransition both = new ParallelTransition(fade, slide);
        both.setDelay(ROW_STAGGER.multiply(order));
        both.play();
    }

    /**
     * Shows a number counting up from zero to its value. The label ends
     * on exactly format(value).
     *
     * @param label  where the number is shown
     * @param value  the final number
     * @param format turns a number into the label's text
     * @param order  the label's card position, so it starts with its card; 0 for the first
     */
    public static void countUp(Label label, double value, DoubleFunction<String> format, int order) {
        DoubleProperty shown = new SimpleDoubleProperty(0);
        shown.addListener((obs, oldValue, newValue) -> label.setText(format.apply(newValue.doubleValue())));
        label.setText(format.apply(0));
        Timeline count = new Timeline(new KeyFrame(COUNT, new KeyValue(shown, value, EASE_OUT)));
        count.setDelay(ROW_STAGGER.multiply(order));
        count.setOnFinished(e -> label.setText(format.apply(value)));
        count.play();
    }
}
