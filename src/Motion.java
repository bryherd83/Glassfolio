import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.util.Duration;

/**
 * The small animations both screens share. Each one runs once in response to
 * something (a screen opening, the mouse entering a button), never on a loop.
 */
public final class Motion {
    private static final Duration ENTRANCE = Duration.millis(450);
    private static final double ENTRANCE_OFFSET = 30;
    private static final Duration HOVER = Duration.millis(150);
    private static final double HOVER_SCALE = 1.06;
    private static final Duration ROW_FADE = Duration.millis(350);
    private static final Duration ROW_STAGGER = Duration.millis(120);

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
        new ParallelTransition(fade, slide).play();
    }

    /**
     * Makes a button grow a little while the mouse is over it.
     *
     * @param button the button
     */
    public static void addHoverPop(Button button) {
        ScaleTransition grow = new ScaleTransition(HOVER, button);
        grow.setToX(HOVER_SCALE);
        grow.setToY(HOVER_SCALE);
        ScaleTransition shrink = new ScaleTransition(HOVER, button);
        shrink.setToX(1);
        shrink.setToY(1);
        button.setOnMouseEntered(e -> {
            shrink.stop();
            grow.playFromStart();
        });
        button.setOnMouseExited(e -> {
            grow.stop();
            shrink.playFromStart();
        });
    }

    /**
     * Fades a node in after a delay that grows with its position, so a list
     * of nodes appears one after another.
     *
     * @param node  the node to fade in
     * @param order its position, 0 for the first
     */
    public static void fadeInStaggered(Node node, int order) {
        node.setOpacity(0);
        FadeTransition fade = new FadeTransition(ROW_FADE, node);
        fade.setDelay(ROW_STAGGER.multiply(order));
        fade.setFromValue(0);
        fade.setToValue(1);
        fade.play();
    }
}
