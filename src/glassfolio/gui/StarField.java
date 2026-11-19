package glassfolio.gui;

import java.util.Random;
import javafx.scene.Group;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Circle;

/**
 * Faint, still stars scattered behind the start screen's chart. They keep their
 * places relative to the area at any size: pale on the dark theme's night sky,
 * soft blue on the light theme's day sky (the -star color in {@link Theme}).
 */
final class StarField {
    private static final int STAR_COUNT = 70;
    private static final long STAR_SEED = 17; // the same sky every time
    private static final double MIN_RADIUS = 0.6;
    private static final double RADIUS_RANGE = 1.4;
    private static final double MIN_OPACITY = 0.12;
    private static final double OPACITY_RANGE = 0.4;

    private StarField() {
    }

    /**
     * Builds the star layer.
     *
     * @return the layer, which ignores the mouse
     */
    static Pane build() {
        Random random = new Random(STAR_SEED);
        Pane layer = new Pane();
        Group stars = new Group();
        for (int i = 0; i < STAR_COUNT; i++) {
            Circle star = new Circle(MIN_RADIUS + random.nextDouble() * RADIUS_RANGE);
            star.centerXProperty().bind(layer.widthProperty().multiply(random.nextDouble()));
            star.centerYProperty().bind(layer.heightProperty().multiply(random.nextDouble()));
            star.setStyle("-fx-fill: -star;");
            star.setOpacity(MIN_OPACITY + random.nextDouble() * OPACITY_RANGE);
            stars.getChildren().add(star);
        }
        stars.setManaged(false);
        layer.getChildren().add(stars);
        layer.setMinSize(0, 0);
        layer.setMouseTransparent(true);
        return layer;
    }
}
