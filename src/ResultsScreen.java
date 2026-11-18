import java.util.List;
import javafx.beans.property.ObjectProperty;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * Second screen: a frosted top bar with the title, theme switch and Back, then
 * one solid card per suggested index (best first, full reason, key numbers and a
 * trend line) and a chart of every index's past year. The best pick's rank badge
 * is the screen's only accent. It only displays data it is given.
 */
public final class ResultsScreen {
    private static final double OUTER_PADDING = 24;
    private static final double SECTION_SPACING = 14;
    private static final double ROW_SPACING = 18;
    private static final double METRIC_SPACING = 22;
    private static final double RANK_SIZE = 34;
    private static final double SPARK_WIDTH = 130;
    private static final double SPARK_HEIGHT = 40;
    private static final double CHART_HEIGHT = 260; // the chart's smallest height; it grows into spare room
    private static final double MAX_CONTENT_WIDTH = 1280; // past this, lines get too long to scan
    private static final double PERCENT = 100;
    private static final double CLIP_ROOM = 2; // keeps card borders from being cut off by the scroll pane
    private static final double BAR_SPACING = 12;
    private static final Insets BAR_PADDING = new Insets(14, 18, 14, 22);
    private static final double TILE_SPACING = 2;
    private static final Insets TILE_PADDING = new Insets(12, 18, 12, 18);
    private static final double EMPTY_PADDING = 20;
    private static final double DETAILS_SPACING = 6;
    private static final double METRIC_SPACING_INSIDE = 1;
    private static final double SCORE_MIN_WIDTH = 70;
    private static final Insets ROW_PADDING = new Insets(14, 20, 14, 16);
    private static final double CHART_CARD_SPACING = 4;
    private static final Insets CHART_CARD_PADDING = new Insets(16, 18, 10, 18);

    private static final String NAME_STYLE = "-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: -text;";
    private static final String REASON_STYLE = "-fx-font-size: 13px; -fx-text-fill: -text-2;";
    private static final String CAPTION_STYLE = "-fx-font-size: 11px; -fx-text-fill: -text-3;";
    private static final String VALUE_STYLE = "-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: -text;";
    private static final String SCORE_STYLE = "-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: -text;";
    private static final String SECTION_STYLE = Styles.DISPLAY_FONT + "-fx-font-size: 17px; -fx-text-fill: -text;";
    private static final String ECONOMY_VALUE_STYLE = "-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: -text-2;";
    // the top pick reads first: bigger serif name and score than the rest
    private static final String TOP_NAME_STYLE = Styles.DISPLAY_FONT + "-fx-font-size: 21px; -fx-text-fill: -text;";
    private static final String TOP_SCORE_STYLE = "-fx-font-size: 30px; -fx-font-weight: bold; -fx-text-fill: -text;";
    private static final String EMPTY_STYLE = "-fx-font-size: 14px; -fx-text-fill: -text-2;";
    private static final String RANK_STYLE = """
            -fx-background-color: -rank-bg; -fx-background-radius: 17;
            -fx-text-fill: -rank-text; -fx-font-weight: bold; -fx-font-size: 14px;
            """;
    private static final String TOP_RANK_STYLE = """
            -fx-background-color: -accent; -fx-background-radius: 17;
            -fx-text-fill: -accent-text; -fx-font-weight: bold; -fx-font-size: 14px;
            """;
    private static final String PROFILE_STYLE = "-fx-font-size: 14px; -fx-text-fill: -text-2;";

    private final VBox root;
    private final Scene scene;

    /**
     * Builds the results screen.
     *
     * @param strategyName the user's strategy, for example "Growth"
     * @param suggestions  the ranked suggestions, best first
     * @param indexes      every loaded index, for the chart
     * @param history      where the chart and trend lines read each index's past year
     * @param economy      the current economy numbers, or null to leave that strip out
     * @param theme        the app's current theme; the switch on this screen changes it
     * @param onBack       called when the user clicks Back
     */
    public ResultsScreen(String strategyName, List<Suggestion> suggestions, List<Stock> indexes,
                         HistoryCache history, EconomySnapshot economy, ObjectProperty<Theme> theme,
                         Runnable onBack) {
        VBox content = new VBox(SECTION_SPACING);
        if (economy != null) {
            content.getChildren().add(buildEconomyStrip(economy));
        }
        VBox chartCard = buildChartCard(indexes, history);
        VBox.setVgrow(chartCard, Priority.ALWAYS);
        content.getChildren().addAll(buildList(strategyName, suggestions, history), chartCard);
        content.setPadding(new Insets(CLIP_ROOM));
        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        VBox.setVgrow(scroll, Priority.ALWAYS);
        // fill a tall window instead of leaving a gap; still scrolls when the window is short
        content.minHeightProperty().bind(scroll.viewportBoundsProperty().map(bounds -> bounds.getHeight() - 2 * CLIP_ROOM));

        root = new VBox(SECTION_SPACING, buildTopBar(strategyName, theme, onBack), scroll);
        root.setPadding(new Insets(OUTER_PADDING));
        root.setMaxWidth(MAX_CONTENT_WIDTH);
        scene = GlassTheme.buildScene(root, theme);
    }

    /**
     * Shows this screen in the window and plays the entrance animation.
     *
     * @param stage the app's window
     */
    public void show(Stage stage) {
        stage.setScene(scene);
        Motion.playEntrance(root);
    }

    /**
     * Builds the frosted bar that floats above the content: title, profile,
     * theme switch and Back.
     *
     * @param strategyName the user's strategy
     * @param theme        the app's current theme
     * @param onBack       called when the user clicks Back
     * @return the bar
     */
    private static HBox buildTopBar(String strategyName, ObjectProperty<Theme> theme, Runnable onBack) {
        Label title = new Label("Your suggestions");
        title.setStyle(Styles.TITLE);
        Label profile = new Label(strategyName + " profile");
        profile.setStyle(PROFILE_STYLE);

        Button back = new Button("Back");
        back.setStyle(Styles.SECONDARY_BUTTON);
        back.setOnAction(e -> onBack.run());
        Motion.addHoverPop(back);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox bar = new HBox(BAR_SPACING, title, profile, spacer, GlassTheme.buildThemeSwitch(theme), back);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(BAR_PADDING);
        bar.setStyle(Styles.GLASS);
        return bar;
    }

    /**
     * Builds one bar with the interest rate, inflation and unemployment,
     * separated by dividers. It is context for the picks, so it stays quieter.
     *
     * @param economy the numbers to show
     * @return the bar
     */
    private static HBox buildEconomyStrip(EconomySnapshot economy) {
        HBox strip = new HBox(SECTION_SPACING,
                economyItem("Fed interest rate", economy.fedFundsRate()),
                new Separator(Orientation.VERTICAL),
                economyItem("Inflation, last 12 months", economy.inflation()),
                new Separator(Orientation.VERTICAL),
                economyItem("Unemployment", economy.unemployment()));
        strip.setPadding(TILE_PADDING);
        strip.setStyle(Styles.SOLID_CARD);
        return strip;
    }

    /**
     * Builds one economy number: a caption over the percent.
     *
     * @param caption what the number is
     * @param percent the value, 4.33 means 4.33%
     * @return the item
     */
    private static VBox economyItem(String caption, double percent) {
        Label captionLabel = new Label(caption);
        captionLabel.setStyle(CAPTION_STYLE);
        Label value = new Label(String.format("%.2f%%", percent));
        value.setStyle(ECONOMY_VALUE_STYLE);
        VBox item = new VBox(TILE_SPACING, captionLabel, value);
        HBox.setHgrow(item, Priority.ALWAYS);
        return item;
    }

    /**
     * Builds one card per suggestion, best first. Cards fade in one after another.
     *
     * @param strategyName the user's strategy, used in the empty message
     * @param suggestions  the ranked suggestions
     * @param history      where each card's trend line comes from
     * @return the list
     */
    private static VBox buildList(String strategyName, List<Suggestion> suggestions, HistoryCache history) {
        VBox list = new VBox(SECTION_SPACING);
        if (suggestions.isEmpty()) {
            Label empty = new Label("No index meets the " + strategyName + " criteria right now.");
            empty.setStyle(EMPTY_STYLE);
            VBox emptyCard = new VBox(empty);
            emptyCard.setPadding(new Insets(EMPTY_PADDING));
            emptyCard.setStyle(Styles.SOLID_CARD);
            list.getChildren().add(emptyCard);
            return list;
        }
        for (int i = 0; i < suggestions.size(); i++) {
            Suggestion suggestion = suggestions.get(i);
            HBox row = buildRow(i + 1, suggestion, history.getPercentChange(suggestion.stock().getTicker()));
            list.getChildren().add(row);
            Motion.fadeInStaggered(row, i);
        }
        return list;
    }

    /**
     * Builds one suggestion card: rank, name, full reason, key numbers, trend line and score.
     *
     * @param rank       1 for the best suggestion
     * @param suggestion the suggestion to show
     * @param trend      the index's past year as percent change, oldest first
     * @return the card
     */
    private static HBox buildRow(int rank, Suggestion suggestion, List<Double> trend) {
        boolean top = rank == 1;
        Stock stock = suggestion.stock();
        boolean up = stock.getOneYearReturn() >= 0;

        Label rankLabel = new Label(String.valueOf(rank));
        rankLabel.setStyle(top ? TOP_RANK_STYLE : RANK_STYLE);
        rankLabel.setAlignment(Pos.CENTER);
        rankLabel.setMinSize(RANK_SIZE, RANK_SIZE);
        rankLabel.setMaxSize(RANK_SIZE, RANK_SIZE);

        Label name = new Label(stock.getName());
        name.setStyle(top ? TOP_NAME_STYLE : NAME_STYLE);

        Label reason = new Label(suggestion.reason());
        reason.setStyle(REASON_STYLE);
        reason.setWrapText(true);

        HBox metrics = new HBox(METRIC_SPACING,
                metric("Price", String.format("%,.2f", stock.getPrice()), null),
                metric("1-year change", String.format("%+.1f%%", stock.getOneYearReturn() * PERCENT),
                        up ? Styles.UP : Styles.DOWN),
                metric("Volatility", String.format("%.1f%%", stock.getVolatility() * PERCENT), null),
                metric("Budget a year ago is now", String.format("$%,.2f", suggestion.worth()), null));
        VBox details = new VBox(DETAILS_SPACING, name, reason, metrics);
        HBox.setHgrow(details, Priority.ALWAYS);

        Label score = new Label(String.format("%.1f", suggestion.score()));
        score.setStyle(top ? TOP_SCORE_STYLE : SCORE_STYLE);
        Label scoreCaption = new Label("match score");
        scoreCaption.setStyle(CAPTION_STYLE);
        VBox scoreBox = new VBox(score, scoreCaption);
        scoreBox.setAlignment(Pos.CENTER_RIGHT);
        scoreBox.setMinWidth(SCORE_MIN_WIDTH);

        HBox row = new HBox(ROW_SPACING, rankLabel, details, Sparkline.build(trend, up, SPARK_WIDTH, SPARK_HEIGHT), scoreBox);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(ROW_PADDING);
        row.setStyle(Styles.SOLID_CARD);
        return row;
    }

    /**
     * Builds a small caption-over-value pair, like "Volatility / 13.5%".
     *
     * @param caption what the number is
     * @param value   the formatted number
     * @param color   the value's color, or null for the normal text color
     * @return the pair
     */
    private static Node metric(String caption, String value, String color) {
        Label captionLabel = new Label(caption);
        captionLabel.setStyle(CAPTION_STYLE);
        Label valueLabel = new Label(value);
        valueLabel.setStyle(VALUE_STYLE + (color == null ? "" : "-fx-text-fill: " + color + ";"));
        return new VBox(METRIC_SPACING_INSIDE, captionLabel, valueLabel);
    }

    /**
     * Builds the chart card: one line per index, as percent change, so indexes
     * with very different prices fit on one scale.
     *
     * @param indexes the indexes to draw
     * @param history where each index's values come from
     * @return the card
     */
    private static VBox buildChartCard(List<Stock> indexes, HistoryCache history) {
        NumberAxis days = new NumberAxis();
        days.setLabel("Trading days over the last year");
        NumberAxis change = new NumberAxis();
        change.setLabel("Change since a year ago (%)");

        LineChart<Number, Number> chart = new LineChart<>(days, change);
        chart.setCreateSymbols(false);
        chart.setAnimated(false);
        chart.setMinHeight(CHART_HEIGHT);
        chart.setPrefHeight(CHART_HEIGHT); // grows into spare room; a large default would force scrolling
        VBox.setVgrow(chart, Priority.ALWAYS);
        for (Stock stock : indexes) {
            XYChart.Series<Number, Number> line = new XYChart.Series<>();
            line.setName(stock.getName());
            List<Double> values = history.getPercentChange(stock.getTicker());
            for (int day = 0; day < values.size(); day++) {
                line.getData().add(new XYChart.Data<>(day, values.get(day)));
            }
            chart.getData().add(line);
        }

        Label heading = new Label("How each index moved over the past year");
        heading.setStyle(SECTION_STYLE);
        VBox card = new VBox(CHART_CARD_SPACING, heading, chart);
        card.setPadding(CHART_CARD_PADDING);
        card.setStyle(Styles.SOLID_CARD);
        return card;
    }
}
