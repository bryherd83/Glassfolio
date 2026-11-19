package glassfolio.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntFunction;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;

/**
 * Adds a crosshair to a line chart: while the mouse is over the plot, a guide
 * line marks the nearest day, a dot sits on each line, and a small box lists
 * the date and every line's value. Reading by column means the user doesn't
 * have to hit a 2px line exactly.
 */
final class ChartReadout {
    private static final double DOT_RADIUS = 4;
    private static final double BOX_GAP = 12;
    private static final double BOX_SPACING = 3;
    private static final Insets BOX_PADDING = new Insets(8, 12, 8, 12);
    private static final String GUIDE_STYLE = "-fx-stroke: -text-3; -fx-stroke-width: 1;";
    private static final String BOX_STYLE = """
            -fx-background-color: -bg, -surface; -fx-background-radius: 12;
            -fx-border-color: -field-border; -fx-border-radius: 12;
            -fx-effect: dropshadow(gaussian, -glow, 16, 0, 0, 4);
            """;
    private static final String DATE_STYLE = Styles.CAPS; // small grey caps, like the form labels
    private static final String VALUE_STYLE = Styles.DISPLAY + "-fx-font-size: 13px; -fx-text-fill: ";
    private static final String DOT_STYLE = "-fx-stroke: -surface; -fx-stroke-width: 2; -fx-fill: ";

    private ChartReadout() {
    }

    /**
     * Wraps a chart in a layer that shows the readout on hover.
     *
     * @param chart     the chart, already filled
     * @param days      its x-axis, one step per trading day
     * @param change    its y-axis
     * @param names     each line's name, in the chart's series order
     * @param values    each line's values by day, in the same order
     * @param dateLabel turns a day number into the date text
     * @return the chart with its readout layer, to put in the layout instead of the chart
     */
    static StackPane attach(LineChart<Number, Number> chart, NumberAxis days, NumberAxis change,
                            List<String> names, List<List<Double>> values, IntFunction<String> dateLabel) {
        Pane layer = new Pane();
        layer.setMouseTransparent(true); // the chart keeps every mouse event

        Line guide = new Line();
        guide.setStyle(GUIDE_STYLE);
        Label date = new Label();
        date.setStyle(DATE_STYLE);
        VBox box = new VBox(BOX_SPACING, date);
        box.setPadding(BOX_PADDING);
        box.setStyle(BOX_STYLE);
        List<Circle> dots = new ArrayList<>();
        List<Label> rows = new ArrayList<>();
        for (int i = 0; i < names.size(); i++) {
            String color = "-series-" + (i + 1) + ";"; // matches the line colors in the stylesheet
            Circle dot = new Circle(DOT_RADIUS);
            dot.setStyle(DOT_STYLE + color);
            dots.add(dot);
            Label row = new Label();
            row.setStyle(VALUE_STYLE + color);
            rows.add(row);
        }
        box.getChildren().addAll(rows);
        layer.getChildren().add(guide);
        layer.getChildren().addAll(dots);
        layer.getChildren().add(box);
        layer.getChildren().forEach(node -> node.setManaged(false));
        setShown(layer, false);

        int lastDay = values.stream().mapToInt(List::size).max().orElse(1) - 1;
        chart.addEventHandler(MouseEvent.MOUSE_MOVED, e -> {
            Node plot = chart.lookup(".chart-plot-background");
            Point2D inPlot = plot == null ? null : plot.sceneToLocal(e.getSceneX(), e.getSceneY());
            if (inPlot == null || !plot.getLayoutBounds().contains(inPlot)) {
                setShown(layer, false);
                return;
            }
            int day = (int) Math.round(days.getValueForDisplay(inPlot.getX()).doubleValue());
            day = Math.clamp(day, 0, lastDay);
            double x = days.getDisplayPosition(day);
            Point2D top = layer.sceneToLocal(plot.localToScene(x, 0));
            Point2D bottom = layer.sceneToLocal(plot.localToScene(x, plot.getLayoutBounds().getHeight()));
            guide.setStartX(top.getX());
            guide.setStartY(top.getY());
            guide.setEndX(bottom.getX());
            guide.setEndY(bottom.getY());

            date.setText(dateLabel.apply(day).toUpperCase(java.util.Locale.ROOT));
            for (int i = 0; i < names.size(); i++) {
                List<Double> line = values.get(i);
                boolean has = day < line.size();
                dots.get(i).setVisible(has);
                rows.get(i).setText(names.get(i) + "   "
                        + (has ? ResultsScreen.signedPercent(line.get(day)) : "no data"));
                if (has) {
                    Point2D point = layer.sceneToLocal(plot.localToScene(x, change.getDisplayPosition(line.get(day))));
                    dots.get(i).setCenterX(point.getX());
                    dots.get(i).setCenterY(point.getY());
                }
            }
            box.autosize();
            double boxX = top.getX() + BOX_GAP;
            if (boxX + box.getWidth() > layer.getWidth()) { // flip to the left near the right edge
                boxX = top.getX() - BOX_GAP - box.getWidth();
            }
            box.relocate(boxX, top.getY() + BOX_GAP);
            guide.setVisible(true);
            box.setVisible(true);
        });
        chart.addEventHandler(MouseEvent.MOUSE_EXITED, e -> setShown(layer, false));
        return new StackPane(chart, layer);
    }

    /**
     * Shows or hides everything in the readout layer.
     *
     * @param layer the readout layer
     * @param shown true to show
     */
    private static void setShown(Pane layer, boolean shown) {
        layer.getChildren().forEach(node -> node.setVisible(shown));
    }
}
