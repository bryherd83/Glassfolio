package glassfolio.gui;

import glassfolio.data.HistoryCache;
import glassfolio.model.Stock;
import java.util.List;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * The start screen's market card: the three indexes with their latest value,
 * 1-year change and trend line, floating at the foot of the picture panel. It
 * stays hidden until data arrives, so a missing key or no internet just leaves it out.
 */
public final class MarketStrip {
    private static final double ITEM_SPACING = 18;
    private static final Insets OUTER_PADDING = new Insets(0, 18, 18, 18); // floats just inside the picture panel
    private static final Insets BAR_PADDING = new Insets(14, 20, 14, 20);
    private static final double LINE_SPACING = 3;
    private static final double SPARK_WIDTH = 64;
    private static final double SPARK_HEIGHT = 26;
    private static final double PERCENT = 100;

    private static final String CAPTION_STYLE = Styles.LABEL;
    private static final String INDEX_VALUE_STYLE = Styles.NUMBER;
    private static final String CHANGE_STYLE = Styles.HEADING + "-fx-font-size: 12px; -fx-text-fill: ";

    private final HBox bar = new HBox(ITEM_SPACING);
    private final StackPane root = new StackPane(bar);

    /** Builds the empty, hidden card. */
    public MarketStrip() {
        bar.setPadding(BAR_PADDING);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setStyle(Styles.SOLID_CARD);
        root.setPadding(OUTER_PADDING);
        root.setMaxHeight(Region.USE_PREF_SIZE); // sits at the panel's foot instead of filling it
        root.setVisible(false);
        root.setManaged(false);
    }

    /** @return the card's node, to place in a screen */
    public Node node() {
        return root;
    }

    /**
     * Fills the card and fades it in. Nothing is shown for an empty list.
     *
     * @param indexes the loaded indexes
     * @param history where each index's past year comes from, for the trend lines
     */
    public void show(List<Stock> indexes, HistoryCache history) {
        if (indexes.isEmpty()) {
            return;
        }
        bar.getChildren().clear();
        for (Stock stock : indexes) {
            if (!bar.getChildren().isEmpty()) {
                bar.getChildren().add(new Separator(Orientation.VERTICAL));
            }
            Region item = indexItem(stock, history.getPercentChange(stock.getTicker()));
            HBox.setHgrow(item, Priority.ALWAYS);
            bar.getChildren().add(item);
        }
        Styles.keepWholeText(bar);
        root.setManaged(true);
        root.setVisible(true);
        Motion.fadeInStaggered(root, 0);
    }

    /**
     * Builds one index: name, latest value, 1-year change and trend line.
     *
     * @param stock the index
     * @param trend its past year as percent change, oldest first
     * @return the item
     */
    private static Region indexItem(Stock stock, List<Double> trend) {
        boolean up = stock.getOneYearReturn() >= 0;
        Label name = new Label(stock.getName());
        name.setStyle(CAPTION_STYLE);
        Label value = new Label();
        Motion.countUp(value, stock.getPrice(), v -> String.format("%,.2f", v), 0);
        value.setStyle(INDEX_VALUE_STYLE);
        Label change = new Label(ResultsScreen.signedPercent(stock.getOneYearReturn() * PERCENT) + " past year");
        change.setStyle(CHANGE_STYLE + (up ? Styles.UP : Styles.DOWN) + ";");
        value.setMinWidth(Region.USE_PREF_SIZE); // the number is never cut to "..."
        change.setMinWidth(Region.USE_PREF_SIZE);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox item = new HBox(new VBox(LINE_SPACING, name, value, change), spacer,
                Sparkline.build(trend, up, SPARK_WIDTH, SPARK_HEIGHT));
        item.setAlignment(Pos.CENTER_LEFT);
        return item;
    }
}
