package glassfolio.gui;

import java.util.List;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Polyline;
import javafx.scene.shape.StrokeLineJoin;

/**
 * A tiny trend line with no axes, green if the series ended higher than it
 * started and red if lower. Used on the start screen and the results screen.
 */
public final class Sparkline {
    private static final double STROKE = 2;

    private Sparkline() {
    }

    /**
     * Draws the trend line.
     *
     * @param values the series, oldest first
     * @param up     whether the series ended higher than it started
     * @param width  the line's width in pixels
     * @param height the line's height in pixels
     * @return a pane of exactly that size holding the line
     */
    public static Pane build(List<Double> values, boolean up, double width, double height) {
        Pane pane = new Pane();
        pane.setMinSize(width, height);
        pane.setMaxSize(width, height);
        if (values.size() < 2) {
            return pane;
        }
        double min = values.stream().mapToDouble(Double::doubleValue).min().orElse(0);
        double max = values.stream().mapToDouble(Double::doubleValue).max().orElse(0);
        double range = (max == min) ? 1 : max - min; // flat line: avoid dividing by zero

        Polyline line = new Polyline();
        for (int i = 0; i < values.size(); i++) {
            double x = i * width / (values.size() - 1);
            double y = height - (values.get(i) - min) / range * height;
            line.getPoints().addAll(x, y);
        }
        line.setStyle("-fx-stroke: " + (up ? Styles.UP : Styles.DOWN) + ";");
        line.setAccessibleText("Past year trend, " + (up ? "up" : "down"));
        line.setStrokeWidth(STROKE);
        line.setStrokeLineJoin(StrokeLineJoin.ROUND);
        pane.getChildren().add(line);
        return pane;
    }
}
