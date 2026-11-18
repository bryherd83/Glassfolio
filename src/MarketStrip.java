import java.util.List;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/**
 * The start screen's market overview: one tile per index (latest value,
 * 1-year change and trend) plus the Fed rate and inflation. It stays hidden
 * until data arrives, so a missing key or no internet just leaves it out.
 */
public final class MarketStrip {
    private static final double SPACING = 14;
    private static final Insets PADDING = new Insets(0, 24, 22, 24);
    private static final Insets TILE_PADDING = new Insets(12, 16, 12, 16);
    private static final double TILE_SPACING = 4;
    private static final double SPARK_WIDTH = 90;
    private static final double SPARK_HEIGHT = 30;
    private static final double PERCENT = 100;
    private static final String UP_ARROW = "▲"; // the arrow says the direction, not just the color
    private static final String DOWN_ARROW = "▼";

    private static final String CAPTION_STYLE = "-fx-font-size: 12px; -fx-text-fill: -text-3;";
    private static final String VALUE_STYLE = "-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: -text;";
    private static final String CHANGE_STYLE = "-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: ";

    private final HBox root = new HBox(SPACING);

    /** Builds the empty, hidden strip. */
    public MarketStrip() {
        root.setPadding(PADDING);
        root.setVisible(false);
        root.setManaged(false);
    }

    /** @return the strip's node, to place in a screen */
    public Node node() {
        return root;
    }

    /**
     * Fills the strip with tiles and fades it in.
     *
     * @param indexes the loaded indexes
     * @param history where each index's past year comes from, for the trend lines
     * @param economy the economy numbers, or null to leave those tiles out
     */
    public void show(List<Stock> indexes, HistoryCache history, EconomySnapshot economy) {
        root.getChildren().clear();
        for (Stock stock : indexes) {
            root.getChildren().add(indexTile(stock, history.getPercentChange(stock.getTicker())));
        }
        if (economy != null) {
            root.getChildren().add(economyTile("Fed interest rate", economy.fedFundsRate(), "Set by the Federal Reserve"));
            root.getChildren().add(economyTile("Inflation", economy.inflation(), "Consumer prices, last 12 months"));
        }
        root.getChildren().forEach(tile -> HBox.setHgrow(tile, Priority.ALWAYS));
        root.setManaged(true);
        root.setVisible(true);
        Motion.fadeInStaggered(root, 0);
    }

    /**
     * Builds one index tile: name, latest value, 1-year change and trend line.
     *
     * @param stock the index
     * @param trend its past year as percent change, oldest first
     * @return the tile
     */
    private static Region indexTile(Stock stock, List<Double> trend) {
        boolean up = stock.getOneYearReturn() >= 0;
        Label name = new Label(stock.getName());
        name.setStyle(CAPTION_STYLE);
        Label value = new Label(String.format("%,.2f", stock.getPrice()));
        value.setStyle(VALUE_STYLE);
        Label change = new Label(String.format("%s %+.1f%% past year",
                up ? UP_ARROW : DOWN_ARROW, stock.getOneYearReturn() * PERCENT));
        change.setStyle(CHANGE_STYLE + (up ? Styles.UP : Styles.DOWN) + ";");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox valueRow = new HBox(value, spacer, Sparkline.build(trend, up, SPARK_WIDTH, SPARK_HEIGHT));
        valueRow.setAlignment(Pos.CENTER_LEFT);
        return tile(name, valueRow, change);
    }

    /**
     * Builds one economy tile: caption, percent and a short note on what it is.
     *
     * @param caption what the number is
     * @param percent the value, 4.33 means 4.33%
     * @param note    one line explaining the number
     * @return the tile
     */
    private static Region economyTile(String caption, double percent, String note) {
        Label name = new Label(caption);
        name.setStyle(CAPTION_STYLE);
        Label value = new Label(String.format("%.2f%%", percent));
        value.setStyle(VALUE_STYLE);
        Label noteLabel = new Label(note);
        noteLabel.setStyle(CAPTION_STYLE);
        return tile(name, value, noteLabel);
    }

    /**
     * Stacks a tile's lines on a solid card.
     *
     * @param lines the tile's lines, top to bottom
     * @return the tile
     */
    private static Region tile(Node... lines) {
        VBox tile = new VBox(TILE_SPACING, lines);
        tile.setPadding(TILE_PADDING);
        tile.setMaxWidth(Double.MAX_VALUE);
        tile.setStyle(Styles.SOLID_CARD);
        return tile;
    }
}
