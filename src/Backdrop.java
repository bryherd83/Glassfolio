import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import javafx.animation.Animation;
import javafx.animation.TranslateTransition;
import javafx.beans.binding.Bindings;
import javafx.beans.value.ObservableValue;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Circle;
import javafx.stage.Window;
import javafx.util.Duration;

/**
 * The soft blurred blobs behind each screen. Their colors are pale versions of
 * the chart's three index colors, so the backdrop echoes the data. They keep
 * their places relative to the window at any size, and drift out and back each
 * time their screen opens, then rest.
 */
public final class Backdrop {
    /**
     * One blob. Position and size are shares of the window, so it scales.
     *
     * @param radius share of the window height
     * @param x      center as a share of the window width
     * @param y      center as a share of the window height
     * @param driftX how far it drifts sideways, in pixels
     * @param driftY how far it drifts up or down, in pixels
     * @param color  its named color from {@link Theme}
     */
    private record Blob(double radius, double x, double y, double driftX, double driftY, String color) { }

    private static final List<Blob> BLOBS = List.of(
            new Blob(0.26, 0.17, 0.21, 120, 80, "-blob-1"),
            new Blob(0.22, 0.82, 0.24, -110, 120, "-blob-2"),
            new Blob(0.29, 0.65, 0.79, -140, -70, "-blob-3"));
    private static final double BLUR_RADIUS = 80;
    private static final double DRIFT_SECONDS = 7;
    private static final double DRIFT_STEP_SECONDS = 1.5; // each blob a bit slower so they never move in sync
    private static final int OUT_AND_BACK = 2;
    private static final double MIN_RADIUS = 1; // keeps a blob valid before the first layout

    private Backdrop() {
    }

    /**
     * Builds the backdrop pane.
     *
     * @return the pane, ready to sit behind a screen's content
     */
    public static Pane build() {
        Pane backdrop = new Pane();
        Group blobs = new Group();
        List<Animation> drifts = new ArrayList<>();
        for (int i = 0; i < BLOBS.size(); i++) {
            Blob shape = BLOBS.get(i);
            Circle blob = new Circle();
            blob.centerXProperty().bind(backdrop.widthProperty().multiply(shape.x()));
            blob.centerYProperty().bind(backdrop.heightProperty().multiply(shape.y()));
            blob.radiusProperty().bind(Bindings.max(MIN_RADIUS, backdrop.heightProperty().multiply(shape.radius())));
            blob.setStyle("-fx-fill: " + shape.color() + ";");
            TranslateTransition drift = new TranslateTransition(
                    Duration.seconds(DRIFT_SECONDS + i * DRIFT_STEP_SECONDS), blob);
            drift.setByX(shape.driftX());
            drift.setByY(shape.driftY());
            drift.setAutoReverse(true);
            drift.setCycleCount(OUT_AND_BACK);
            drifts.add(drift);
            blobs.getChildren().add(blob);
        }
        blobs.setEffect(new GaussianBlur(BLUR_RADIUS));
        blobs.setManaged(false); // blobs may drift past the edges without growing the layout

        backdrop.getChildren().add(blobs);
        backdrop.setMinSize(0, 0);
        backdrop.setMouseTransparent(true);
        ObservableValue<Window> window = backdrop.sceneProperty().flatMap(Scene::windowProperty);
        window.addListener((obs, oldWindow, newWindow) -> {
            Consumer<Animation> action = (newWindow == null) ? Animation::stop : Animation::playFromStart;
            drifts.forEach(action);
        });
        return backdrop;
    }
}
