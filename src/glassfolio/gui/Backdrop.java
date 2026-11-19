package glassfolio.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import javafx.animation.Animation;
import javafx.animation.Interpolator;
import javafx.animation.Transition;
import javafx.animation.TranslateTransition;
import javafx.beans.binding.Bindings;
import javafx.beans.value.ObservableValue;
import javafx.scene.CacheHint;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.shape.Circle;
import javafx.stage.Window;
import javafx.util.Duration;

/**
 * The animated background behind each screen: a sky gradient, two layers of
 * soft blobs that drift (each blurred once and cached), a dot grid whose dots
 * part around the mouse, and dimmed corners. The animations run while the screen is in a window and
 * stop when it is swapped out or the window goes away.
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

    /**
     * One depth of blobs.
     *
     * @param blobs    the blobs in this layer
     * @param opacity  how strong they are; the far layer is fainter
     */
    private record Layer(List<Blob> blobs, double opacity) { }

    /** Opacity of the near blobs: strong, so the color reads through the see-through cards. */
    private static final double NEAR_OPACITY = 0.9;
    /** Opacity of the far blobs, fainter than the near ones for depth. */
    private static final double FAR_OPACITY = 0.6;
    private static final String WINDOW_WATCH_KEY = "windowWatch";
    private static final double MAX_BLUR = 63; // the largest radius JavaFX's GaussianBlur accepts
    private static final List<Layer> LAYERS = List.of(
            new Layer(List.of(
                    new Blob(0.42, 0.38, 0.92, 160, -90, "-blob-1"),
                    new Blob(0.36, 0.95, 0.62, -140, 110, "-blob-3")), FAR_OPACITY),
            new Layer(List.of(
                    new Blob(0.26, 0.17, 0.21, 280, 170, "-blob-1"),
                    new Blob(0.22, 0.82, 0.24, -260, 230, "-blob-2"),
                    new Blob(0.29, 0.65, 0.79, -300, -160, "-blob-3")), NEAR_OPACITY));
    private static final String SKY_STYLE =
            "-fx-background-color: linear-gradient(to bottom, derive(-bg, 4%) 0%, -bg 55%, derive(-bg, -7%) 100%);";
    private static final String VIGNETTE_STYLE =
            "-fx-background-color: radial-gradient(center 50% 42%, radius 78%, transparent 55%, -vignette 100%);";
    private static final double DRIFT_SECONDS = 13;
    private static final double DRIFT_STEP_SECONDS = 3.5; // each blob a bit slower so they never move in sync
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
        Region sky = new Region();
        sky.setStyle(SKY_STYLE);
        sky.prefWidthProperty().bind(backdrop.widthProperty());
        sky.prefHeightProperty().bind(backdrop.heightProperty());
        backdrop.getChildren().add(sky);

        List<Animation> drifts = new ArrayList<>();
        int order = 0;
        for (Layer layer : LAYERS) {
            Group group = new Group();
            for (Blob shape : layer.blobs()) {
                Circle blob = new Circle();
                blob.centerXProperty().bind(backdrop.widthProperty().multiply(shape.x()));
                blob.centerYProperty().bind(backdrop.heightProperty().multiply(shape.y()));
                blob.radiusProperty().bind(Bindings.max(MIN_RADIUS, backdrop.heightProperty().multiply(shape.radius())));
                blob.setStyle("-fx-fill: " + shape.color() + ";");
                blob.setEffect(new GaussianBlur(MAX_BLUR));
                blob.setCache(true); // blur once, then only move the cached picture
                blob.setCacheHint(CacheHint.SPEED);
                double seconds = DRIFT_SECONDS + order * DRIFT_STEP_SECONDS;
                TranslateTransition drift = new TranslateTransition(Duration.seconds(seconds), blob);
                drift.setByX(shape.driftX());
                drift.setByY(shape.driftY());
                for (Transition loop : List.<Transition>of(drift)) {
                    loop.setInterpolator(Interpolator.EASE_BOTH);
                    loop.setAutoReverse(true);
                    loop.setCycleCount(Animation.INDEFINITE);
                    drifts.add(loop);
                }
                order++;
                group.getChildren().add(blob);
            }
            group.setOpacity(layer.opacity());
            group.setManaged(false); // blobs may drift past the edges without growing the layout
            backdrop.getChildren().add(group);
        }

        // a fine dot grid gives the page one shared structure; its dots part around the mouse
        backdrop.getChildren().add(new DotField(backdrop).node());


        Region vignette = new Region();
        vignette.setStyle(VIGNETTE_STYLE);
        vignette.prefWidthProperty().bind(backdrop.widthProperty());
        vignette.prefHeightProperty().bind(backdrop.heightProperty());
        backdrop.getChildren().add(vignette);

        backdrop.setMinSize(0, 0);
        backdrop.setMouseTransparent(true);
        ObservableValue<Window> window = backdrop.sceneProperty().flatMap(Scene::windowProperty);
        backdrop.getProperties().put(WINDOW_WATCH_KEY, window); // keeps the watcher alive as long as the backdrop
        window.addListener((obs, oldWindow, newWindow) -> {
            Consumer<Animation> action = (newWindow == null) ? Animation::stop : Animation::playFromStart;
            drifts.forEach(action);
        });
        return backdrop;
    }

}
