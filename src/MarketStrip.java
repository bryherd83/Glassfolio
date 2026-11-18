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
 * The start screen's market overview: one bar with the three indexes (latest
 * value, 1-year change and trend) up front and the Fed rate and inflation
 * smaller at the end. It stays hidden until data arrives, so a missing key or
 * no internet just leaves it out.
 */
public final class MarketStrip {
    private static final double ITEM_SPACING = 22;
    private static final Insets OUTER_PADDING = new Insets(0, 24, 22, 24);
    private static final Insets BAR_PADDING = new Insets(14, 22, 14, 22);
    private static final double LINE_SPACING = 3;
    private static final double SPARK_WIDTH = 90;
    private static final double SPARK_HEIGHT = 30;
    private static final double PERCENT = 100;
    private static final String UP_ARROW = "▲"; // the arrow says the direction, not just the color
    private static final String DOWN_ARROW = "▼";

    private static final String CAPTION_STYLE = "-fx-font-size: 12px; -fx-text-fill: -text-3;";
    private static final String INDEX_VALUE_STYLE = "-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: -text;";
    private static final String ECONOMY_VALUE_STYLE = "-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: -text-2;";
    private static final String CHANGE_STYLE = "-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: ";

    private final HBox bar = new HBox(ITEM_SPACING);
    private final StackPane root = new StackPane(bar);

    /** Builds the empty, hidden strip. */
    public MarketStrip() {
        bar.setPadding(BAR_PADDING);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setStyle(Styles.SOLID_CARD);
        root.setPadding(OUTER_PADDING);
        root.setVisible(false);
        root.setManaged(false);
    }

    /** @return the strip's node, to place in a screen */
    public Node node() {
        return root;
    }

    /**
     * Fills the strip and fades it in.
     *
     * @param indexes the loaded indexes
     * @param history where each index's past year comes from, for the trend lines
     * @param economy the economy numbers, or null to leave them out
     */
    public void show(List<Stock> indexes, HistoryCache history, EconomySnapshot economy) {
        bar.getChildren().clear();
        for (Stock stock : indexes) {
            if (!bar.getChildren().isEmpty()) {
                bar.getChildren().add(divider());
            }
            Region item = indexItem(stock, history.getPercentChange(stock.getTicker()));
            HBox.setHgrow(item, Priority.ALWAYS);
            bar.getChildren().add(item);
        }
        if (economy != null) {
            bar.getChildren().addAll(divider(),
                    economyItem("Fed interest rate", economy.fedFundsRate()),
                    economyItem("Inflation, 12 months", economy.inflation()));
        }
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
        Label value = new Label(String.format("%,.2f", stock.getPrice()));
        value.setStyle(INDEX_VALUE_STYLE);
        Label change = new Label(String.format("%s %+.1f%% past year",
                up ? UP_ARROW : DOWN_ARROW, stock.getOneYearReturn() * PERCENT));
        change.setStyle(CHANGE_STYLE + (up ? Styles.UP : Styles.DOWN) + ";");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox item = new HBox(new VBox(LINE_SPACING, name, value, change), spacer,
                Sparkline.build(trend, up, SPARK_WIDTH, SPARK_HEIGHT));
        item.setAlignment(Pos.CENTER_LEFT);
        return item;
    }

    /**
     * Builds one economy number, quieter than the indexes because it is context.
     *
     * @param caption what the number is
     * @param percent the value, 4.33 means 4.33%
     * @return the item
     */
    private static Region economyItem(String caption, double percent) {
        Label name = new Label(caption);
        name.setStyle(CAPTION_STYLE);
        Label value = new Label(String.format("%.2f%%", percent));
        value.setStyle(ECONOMY_VALUE_STYLE);
        return new VBox(LINE_SPACING, name, value);
    }

    /** @return a thin vertical line between items */
    private static Separator divider() {
        return new Separator(Orientation.VERTICAL);
    }
}
