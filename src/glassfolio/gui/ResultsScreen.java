package glassfolio.gui;

import glassfolio.data.HistoryCache;
import glassfolio.model.EconomySnapshot;
import glassfolio.model.Stock;
import glassfolio.model.Suggestion;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.DoubleFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javafx.beans.property.ObjectProperty;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Stage;

/**
 * Second screen: one rounded frame with a header (wordmark, title, profile,
 * the other-profile switch, theme switch and Back), the economy numbers in a
 * tile, then one card per suggested index, best first: a head row (rank, name,
 * reason, trend line, score) over a row of fact tiles. Then the indexes left
 * out and a chart of every index's past year. The best pick carries the accent
 * (rank badge, border, score); the other picks stay in ink, so it reads first.
 * It only displays data it is given.
 */
public final class ResultsScreen {
    private static final double OUTER_PADDING = 24;
    private static final Insets FRAME_MARGIN = new Insets(40, 48, 48, 48); // leaves room to see the background
    private static final String HEADER_STYLE =
            "-fx-border-color: transparent transparent -surface-border transparent; -fx-border-width: 0 0 1 0;"
            + " -fx-background-color: linear-gradient(to right, -tint-1, -tint-2);"
            + " -fx-background-radius: 30 30 0 0;";
    private static final double SECTION_SPACING = 14;
    private static final double ROW_SPACING = 18;
    private static final double RANK_SIZE = 34;
    private static final double SPARK_WIDTH = 130;
    private static final double SPARK_HEIGHT = 40;
    private static final double MAX_CONTENT_WIDTH = 1280; // past this, lines get too long to scan
    private static final double PERCENT = 100;
    private static final double CLIP_ROOM = 8; // keeps card borders, and the lifted top card, from being cut off by the scroll pane
    private static final double BAR_SPACING = 12;
    private static final Insets BAR_PADDING = new Insets(22, 24, 18, 24);
    private static final double TILE_SPACING = 2;
    private static final Insets TILE_PADDING = new Insets(12, 18, 12, 18);
    private static final double EMPTY_PADDING = 20;
    private static final double DETAILS_SPACING = 6;
    private static final double CARD_SPACING = 14;
    private static final double TILE_GAP = 10;
    private static final double TILE_LINE_GAP = 2;
    private static final Insets TILE_INNER_PADDING = new Insets(10, 14, 10, 14);
    private static final Insets ROW_PADDING = new Insets(14, 20, 14, 16);
    private static final String MINUS = "\u2212"; // a real minus sign, wider than a hyphen

    private static final String NAME_STYLE = Styles.SECTION;
    private static final String REASON_STYLE = Styles.BODY;
    private static final String VALUE_STYLE = Styles.DISPLAY + "-fx-font-size: 18px; -fx-text-fill: -text;";
    /** A pick card: the surface fading into the theme tint, so no card is a plain box. */
    private static final String CARD_STYLE = Styles.SOLID_CARD
            + "-fx-background-color: linear-gradient(to bottom right, -surface, -tint-1);";
    /**
     * The top pick: a stronger double tint, accent border and glow, like a featured plan.
     * Its background-color replaces CARD_STYLE's on purpose (the later declaration wins).
     */
    private static final String TOP_CARD_STYLE = CARD_STYLE
            + "-fx-background-color: -surface, linear-gradient(to bottom right, -tint-1, -tint-2),"
            + " linear-gradient(to bottom right, -tint-1, transparent);"
            + " -fx-border-color: -accent; -fx-border-width: 2;"
            + " -fx-effect: dropshadow(gaussian, -edge, 32, 0.15, 0, 10);";
    /**
     * Raises the top pick above the other cards. A layout margin, not a translate,
     * because the entrance and hover animations own translateY.
     */
    private static final Insets TOP_CARD_LIFT = new Insets(-6, 0, 6, 0);
    /** A fact tile with a soft tint toward the corner. */
    private static final String TILE_STYLE = "-fx-background-color: linear-gradient(to bottom right, -rank-bg, -tint-2);"
            + " -fx-background-radius: 16;";
    /** The other picks' scores in ink, so only the top pick's score carries the accent. */
    private static final String SCORE_STYLE = Styles.DISPLAY + "-fx-font-size: 26px; -fx-text-fill: -text;";
    private static final String SECTION_STYLE = Styles.SECTION;
    // context, so a step below the picks' numbers: the top pick reads first
    private static final String ECONOMY_VALUE_STYLE = Styles.DISPLAY + "-fx-font-size: 20px; -fx-text-fill: -text;";
    // the top pick reads first: bigger name and score than the rest
    private static final String TOP_NAME_STYLE = Styles.HEADING + "-fx-font-size: 21px; -fx-text-fill: -text;";
    private static final String TOP_SCORE_STYLE = Styles.DISPLAY + "-fx-font-size: 40px; -fx-text-fill: -accent;";
    /** The page title fades from ink to the accent, like the start screen headline. */
    private static final String TITLE_STYLE = Styles.TITLE + "-fx-text-fill: linear-gradient(to right, -text, -accent);";
    /** The economy numbers sit in the same tinted box as the start screen callouts. */
    private static final String STRIP_STYLE = "-fx-background-color: linear-gradient(to right, -tint-1, -tint-2);"
            + " -fx-background-radius: 16; -fx-border-color: -edge; -fx-border-radius: 16;";
    /** The worst drop in the down color, so the risk reads at a glance. */
    private static final String DROP_STYLE = Styles.META + "-fx-text-fill: -down;";
    private static final String EMPTY_STYLE = Styles.BODY;
    private static final String RANK_STYLE = """
            -fx-background-color: -rank-bg; -fx-background-radius: %s;
            -fx-text-fill: -rank-text; -fx-font-size: 14px;
            """.formatted(RANK_SIZE / 2) + Styles.HEADING; // a circle: half the badge's size
    private static final String TOP_RANK_STYLE = """
            -fx-background-color: -accent; -fx-background-radius: %s;
            -fx-text-fill: -accent-text; -fx-font-size: 14px;
            """.formatted(RANK_SIZE / 2) + Styles.HEADING;
    private static final String PROFILE_CHIP_STYLE = Styles.HEADING + """
            -fx-background-color: -accent; -fx-background-radius: 999; -fx-padding: 5 14;
            -fx-font-size: 13px; -fx-text-fill: -accent-text;
            """;
    private static final String BACK_STYLE = "-fx-background-color: transparent; -fx-text-fill: -accent; "
            + "-fx-font-size: 13px; -fx-padding: 8 10; -fx-cursor: hand;";
    private static final String MISSED_NAME_STYLE = Styles.HEADING + "-fx-font-size: 14px; -fx-text-fill: -text;";
    /** The "Not suggested" title as a small violet pill, so the card has a clear start. */
    private static final String MISSED_HEADING_STYLE = Styles.HEADING
            + "-fx-font-size: 13px; -fx-text-fill: -rank-text; -fx-background-color: -rank-bg;"
            + " -fx-background-radius: 999; -fx-padding: 4 12 4 12;";
    /** Each left-out index sits in its own soft tile. */
    private static final String MISSED_ROW_STYLE = "-fx-background-color: linear-gradient(to right, -rank-bg, -tint-2);"
            + " -fx-background-radius: 14; -fx-border-color: transparent transparent transparent -down;"
            + " -fx-border-width: 0 0 0 3; -fx-border-radius: 14; -fx-padding: 10 14 10 14;";
    /** Reason text in a left-out row. */
    private static final String MISSED_TEXT_STYLE = "-fx-font-size: 14px; -fx-fill: -text-2;";
    /** The index's own number in a left-out row: bold and in the down color, since it missed. */
    private static final String MISSED_ACTUAL_STYLE = Styles.HEADING + "-fx-font-size: 14px; -fx-fill: -down;";
    /** The user's goal or limit in a left-out row: bold in the accent color. */
    private static final String MISSED_GOAL_STYLE = Styles.HEADING + "-fx-font-size: 14px; -fx-fill: -accent;";
    /** Explains the score, shown when hovering the score or its caption. */
    private static final String SCORE_TIP = "How well the index fits your profile. Higher is better.";
    /** Finds the percent numbers in a reason, so they can be colored. */
    private static final Pattern PERCENT_NUMBER = Pattern.compile("-?\\d+(\\.\\d+)?%");
    private static final double MISSED_SPACING = 8;
    private static final double MISSED_NAME_WIDTH = 240;
    private static final double SCORE_COLUMN_WIDTH = 84;
    private static final double WHY_SPACING = 6; // gap between the lines inside a "Why this pick?" panel
    private static final Insets WHY_PADDING = new Insets(12, 16, 12, 16);
    /** Link text that opens a pick card's explanation. */
    private static final String WHY_OPEN = "Why this pick?";
    /** Shorter link text for a left-out row, where space is tight. */
    private static final String WHY_SHORT = "Why?";
    /** Link text while an explanation is open. */
    private static final String WHY_CLOSE = "Hide";

    /**
     * What every card needs besides its own suggestion.
     *
     * @param budget  the user's budget, for the worst-case sentence
     * @param economy the economy numbers for the inflation sentence, or null if they didn't load
     * @param rule    the strategy's rule in plain words, for the "Why this pick?" panel
     */
    private record CardContext(double budget, EconomySnapshot economy, String rule) { }

    private final VBox root;
    private final Region screenRoot;

    /**
     * Builds the results screen.
     *
     * @param data        what to show
     * @param theme       the app's current theme; the switch on this screen changes it
     * @param switchLabel the text on the button that shows the other profile's picks
     * @param onSwitch    called when the user clicks that button
     * @param onBack      called when the user clicks Back
     * @throws NullPointerException if any argument is null
     */
    public ResultsScreen(ResultsData data, ObjectProperty<Theme> theme, String switchLabel,
                         Runnable onSwitch, Runnable onBack) {
        Objects.requireNonNull(data, "data");
        Objects.requireNonNull(theme, "theme");
        Objects.requireNonNull(switchLabel, "switchLabel");
        Objects.requireNonNull(onSwitch, "onSwitch");
        Objects.requireNonNull(onBack, "onBack");
        VBox content = new VBox(SECTION_SPACING);
        if (data.economy() != null) {
            content.getChildren().add(buildEconomyStrip(data.economy()));
        }
        Double savings = data.economy() == null ? null : data.economy().savingsWorth(data.budget());
        content.getChildren().add(buildList(data.strategyName(), data.picks(), data.history(), savings,
                new CardContext(data.budget(), data.economy(), data.rule())));
        if (!data.missed().isEmpty()) {
            content.getChildren().add(buildMissedCard(data.missed(), data.rule()));
        }
        String topTicker = data.picks().isEmpty() ? null : data.picks().get(0).stock().getTicker();
        VBox chartCard = IndexChart.build(data.indexes(), data.history(), topTicker);
        VBox.setVgrow(chartCard, Priority.ALWAYS);
        content.getChildren().add(chartCard);
        content.setPadding(new Insets(CLIP_ROOM));
        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        VBox.setVgrow(scroll, Priority.ALWAYS);
        // fill a tall window instead of leaving a gap; still scrolls when the window is short
        content.minHeightProperty().bind(scroll.viewportBoundsProperty().map(bounds -> bounds.getHeight() - 2 * CLIP_ROOM));

        root = new VBox(SECTION_SPACING, buildTopBar(data.strategyName(), theme, switchLabel, onSwitch, onBack),
                scroll);
        root.setPadding(new Insets(OUTER_PADDING));
        root.setMaxWidth(MAX_CONTENT_WIDTH);
        root.setStyle(Styles.FRAME); // one rounded frame, like the start screen
        StackPane centered = new StackPane(Styles.shadowed(root, Styles.FRAME_RADIUS, Styles.FRAME_SHADOW));
        centered.setPadding(FRAME_MARGIN);
        screenRoot = GlassTheme.buildRoot(centered);
        Styles.keepWholeText(screenRoot);
    }

    /**
     * Shows this screen in the window and plays the entrance animation.
     *
     * @param stage the app's window
     */
    public void show(Stage stage) {
        stage.getScene().setRoot(screenRoot); // same Scene, so full screen stays on
        Motion.playEntrance(root);
    }

    /**
     * Builds the frame's header: wordmark, title, profile,
     * the other-profile switch, theme switch and Back.
     *
     * @param strategyName the user's strategy
     * @param theme        the app's current theme
     * @param switchLabel  the text on the other-profile button
     * @param onSwitch     called when the user clicks it
     * @param onBack       called when the user clicks Back
     * @return the bar
     */
    private static HBox buildTopBar(String strategyName, ObjectProperty<Theme> theme, String switchLabel,
                                    Runnable onSwitch, Runnable onBack) {
        Label title = new Label("Your suggestions");
        title.setStyle(TITLE_STYLE);
        Label profile = new Label(strategyName + " profile");
        profile.setStyle(PROFILE_CHIP_STYLE); // a small tag, so it reads as the page's setting, not more title

        Button switchProfile = new Button(switchLabel);
        Styles.clickable(switchProfile, Styles.SECONDARY_BUTTON, Styles.SECONDARY_BUTTON_HOVER);
        switchProfile.setOnAction(e -> onSwitch.run());

        Button back = new Button("Back"); // a plain link-like button, so the two real choices stand out
        Styles.clickable(back, BACK_STYLE, BACK_STYLE + "-fx-underline: true;");
        back.setOnAction(e -> onBack.run());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox brand = Styles.wordmark();
        HBox bar = new HBox(BAR_SPACING, brand, title, profile, spacer, switchProfile,
                GlassTheme.buildThemeSwitch(theme), back);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(BAR_PADDING);
        bar.setStyle(HEADER_STYLE); // the frame's header, set off by a hairline
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
                economyItem("Unemployment", economy.unemployment()),
                new Separator(Orientation.VERTICAL),
                economyItem("Safe savings rate, past year", economy.savingsRate()));
        strip.setPadding(TILE_PADDING);
        strip.setStyle(STRIP_STYLE); // the numbers sit together in one tinted tile
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
        Label captionLabel = new Label(caption.toUpperCase(Locale.ROOT));
        captionLabel.setStyle(Styles.CAPS);
        Label value = new Label();
        Motion.countUp(value, percent, v -> String.format("%.2f%%", v), 0);
        value.setStyle(ECONOMY_VALUE_STYLE);
        captionLabel.setMinWidth(Region.USE_PREF_SIZE);
        value.setMinWidth(Region.USE_PREF_SIZE);
        VBox item = new VBox(TILE_SPACING, captionLabel, value);
        HBox.setHgrow(item, Priority.ALWAYS);
        item.setPrefWidth(0); // all four economy numbers get the same width
        return item;
    }

    /**
     * Builds the list with its shared card context.
     *
     * @param strategyName the user's strategy, used in the empty message
     * @param suggestions  the ranked suggestions
     * @param history      where each card's trend line comes from
     * @param savings      what the budget would be in safe savings, or null when the economy numbers didn't load
     * @param context      the budget and economy numbers every card uses
     * @return the list
     */
    private static VBox buildList(String strategyName, List<Suggestion> suggestions, HistoryCache history,
                                  Double savings, CardContext context) {
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
            VBox row = buildRow(i + 1, suggestion, history.getPercentChange(suggestion.stock().getTicker()), savings,
                    context);
            list.getChildren().add(row);
            if (i == 0) {
                VBox.setMargin(row, TOP_CARD_LIFT);
            }
            Motion.fadeInStaggered(row, i);
            Motion.addHoverLift(row);
        }
        return list;
    }

    /**
     * Builds one suggestion card: a head row (rank, name, reason, trend line and
     * score) over a row of up to four fact tiles.
     *
     * @param rank       1 for the best suggestion
     * @param suggestion the suggestion to show
     * @param trend      the index's past year as percent change, oldest first
     * @param savings    what the budget would be in safe savings, or null when the economy numbers didn't load
     * @param context    the budget and economy numbers, for the money tiles
     * @return the card
     */
    private static VBox buildRow(int rank, Suggestion suggestion, List<Double> trend, Double savings,
                                 CardContext context) {
        boolean top = rank == 1;
        Stock stock = suggestion.stock();
        boolean up = stock.getOneYearReturn() >= 0;
        int order = rank - 1;

        Label rankLabel = new Label(String.valueOf(rank));
        rankLabel.setStyle(top ? TOP_RANK_STYLE : RANK_STYLE);
        rankLabel.setAlignment(Pos.CENTER);
        rankLabel.setMinSize(RANK_SIZE, RANK_SIZE);
        rankLabel.setMaxSize(RANK_SIZE, RANK_SIZE);
        Label name = new Label(stock.getName());
        name.setStyle(top ? TOP_NAME_STYLE : NAME_STYLE);
        Label reason = new Label(suggestion.reason());
        reason.setStyle(REASON_STYLE);
        VBox why = whyPanel(context.rule(), suggestion.math());
        VBox details = new VBox(DETAILS_SPACING, name, reason, whyLink(WHY_OPEN, why));
        details.setAlignment(Pos.CENTER_LEFT);
        Region gap = new Region();
        HBox.setHgrow(gap, Priority.ALWAYS);

        Label score = new Label();
        Motion.countUp(score, suggestion.score(), v -> String.format("%.1f", v), order);
        score.setStyle(top ? TOP_SCORE_STYLE : SCORE_STYLE);
        Label scoreCaption = new Label("Fit for your profile");
        scoreCaption.setStyle(Styles.META);
        scoreCaption.setVisible(top); // said once, on the top pick; the score's tooltip explains it on every card
        scoreCaption.setManaged(top);
        Tooltip.install(score, new Tooltip(SCORE_TIP));
        scoreCaption.setTooltip(new Tooltip(SCORE_TIP));
        VBox scoreBox = new VBox(score, scoreCaption);
        scoreBox.setAlignment(Pos.CENTER_RIGHT);
        scoreBox.setMinWidth(SCORE_COLUMN_WIDTH); // the trend line sits in the same place on every card

        HBox head = new HBox(ROW_SPACING, rankLabel, details, gap,
                Sparkline.build(trend, up, SPARK_WIDTH, SPARK_HEIGHT), scoreBox);
        head.setAlignment(Pos.CENTER_LEFT);

        // every fact gets its own small box, label inside, so nothing is crammed or cut off
        HBox tiles = new HBox(TILE_GAP,
                tile("Last 12 months", value(stock.getOneYearReturn() * PERCENT, ResultsScreen::signedPercent,
                        up ? Styles.UP : Styles.DOWN, order)));
        if (context.economy() != null) {
            double real = context.economy().realReturn(stock.getOneYearReturn() * PERCENT);
            tiles.getChildren().add(tile("After inflation", value(real, ResultsScreen::signedPercent, null, order)));
        }
        VBox lowPoint = tile(String.format("Your $%,.0f at its lowest", context.budget()),
                value(stock.worstCaseValue(context.budget()), v -> String.format("$%,.0f", v), null, order));
        Label drop = new Label("Worst drop " + signedPercent(-stock.getBiggestDrop() * PERCENT));
        drop.setStyle(DROP_STYLE);
        lowPoint.getChildren().add(drop);
        tiles.getChildren().add(lowPoint);
        if (savings != null) {
            double versus = Math.round(suggestion.worth() - savings); // whole dollars, so "$0" is never red
            tiles.getChildren().add(tile(versus >= 0 ? "Ahead of savings by" : "Behind savings by",
                    value(Math.abs(versus), v -> String.format("$%,.0f", v), versus >= 0 ? Styles.UP : Styles.DOWN, order)));
        }

        VBox card = new VBox(CARD_SPACING, head, tiles, why);
        card.setPadding(ROW_PADDING);
        card.setStyle(top ? TOP_CARD_STYLE : CARD_STYLE);
        return card;
    }

    /**
     * Puts one fact in its own small box: a label over its value.
     *
     * @param label what the fact is
     * @param value the value, already styled
     * @return the box, which shares the row's width with its neighbors
     */
    private static VBox tile(String label, Label value) {
        Label name = new Label(label.toUpperCase(Locale.ROOT));
        name.setStyle(Styles.CAPS);
        name.setMinWidth(Region.USE_PREF_SIZE);
        VBox box = new VBox(TILE_LINE_GAP, name, value);
        box.setPadding(TILE_INNER_PADDING);
        box.setStyle(TILE_STYLE);
        box.setPrefWidth(0); // every tile gets the same share of the row
        box.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(box, Priority.ALWAYS);
        return box;
    }

    /**
     * Builds the hidden panel that explains a pick: the rule, then this index's score math.
     *
     * @param rule the strategy's rule in plain words
     * @param math how this index's score was worked out
     * @return the panel, hidden and out of the layout until a link shows it
     */
    private static VBox whyPanel(String rule, String math) {
        Label title = new Label("How we picked");
        title.setStyle(Styles.BOX_TITLE);
        VBox panel = new VBox(WHY_SPACING, title, whyText(rule), whyText(math));
        panel.setPadding(WHY_PADDING);
        panel.setStyle(Styles.TINT_BOX);
        panel.setVisible(false);
        panel.managedProperty().bind(panel.visibleProperty());
        return panel;
    }

    /**
     * @param text one line of the panel
     * @return a wrapped label in the box text style
     */
    private static Label whyText(String text) {
        Label label = new Label(text);
        label.setWrapText(true);
        label.setStyle(Styles.BOX_TEXT);
        return label;
    }

    /**
     * Makes the link that shows or hides a panel; its text turns to "Hide" while the panel is open.
     *
     * @param openText the link's text while the panel is closed
     * @param panel    the panel to toggle
     * @return the link
     */
    private static Hyperlink whyLink(String openText, VBox panel) {
        Hyperlink link = new Hyperlink(openText);
        link.setStyle(Styles.LINK);
        link.setOnAction(e -> {
            panel.setVisible(!panel.isVisible());
            link.setText(panel.isVisible() ? WHY_CLOSE : openText);
        });
        return link;
    }

    /**
     * Builds one card listing the indexes that weren't suggested and why, so
     * the user sees the whole comparison, not only the winners.
     *
     * @param missed the left-out indexes with their reasons
     * @param rule   the strategy's rule in plain words, for each row's "Why?" panel
     * @return the card
     */
    private static VBox buildMissedCard(List<Suggestion> missed, String rule) {
        Label heading = new Label("Not suggested");
        heading.setStyle(MISSED_HEADING_STYLE);
        VBox card = new VBox(MISSED_SPACING, heading);
        for (Suggestion miss : missed) {
            Stock stock = miss.stock();
            Label name = new Label(stock.getName());
            name.setStyle(MISSED_NAME_STYLE);
            name.setMinWidth(MISSED_NAME_WIDTH);
            TextFlow reason = highlightPercents(miss.reason());
            VBox why = whyPanel(rule, miss.math());
            HBox line = new HBox(BAR_SPACING, name, reason, whyLink(WHY_SHORT, why));
            line.setAlignment(Pos.CENTER_LEFT);
            line.setStyle(MISSED_ROW_STYLE);
            card.getChildren().add(new VBox(WHY_SPACING, line, why));
        }
        card.setPadding(TILE_PADDING);
        card.setStyle(Styles.SOLID_CARD);
        return card;
    }

    /**
     * Turns a reason into text where the first percent (the index's own number) is
     * in the down color and later ones (the user's goal or limit) are in the accent.
     *
     * @param reason the reason text
     * @return the reason as styled runs
     */
    private static TextFlow highlightPercents(String reason) {
        TextFlow flow = new TextFlow();
        Matcher match = PERCENT_NUMBER.matcher(reason);
        int at = 0;
        boolean first = true;
        while (match.find()) {
            flow.getChildren().add(run(reason.substring(at, match.start()), MISSED_TEXT_STYLE));
            flow.getChildren().add(run(match.group(), first ? MISSED_ACTUAL_STYLE : MISSED_GOAL_STYLE));
            first = false;
            at = match.end();
        }
        flow.getChildren().add(run(reason.substring(at), MISSED_TEXT_STYLE));
        return flow;
    }

    /**
     * @param text  the words
     * @param style the run's style
     * @return one styled run of text
     */
    private static Text run(String text, String style) {
        Text piece = new Text(text);
        piece.setStyle(style);
        return piece;
    }

    /**
     * Builds one number that counts up when its card appears.
     *
     * @param number the number
     * @param format turns the number into text
     * @param color  the text color, or null for the normal text color
     * @param order  the card's position, so the count starts with its card
     * @return the cell
     */
    private static Label value(double number, DoubleFunction<String> format, String color, int order) {
        Label cell = new Label();
        cell.setMinWidth(Region.USE_PREF_SIZE); // the whole number, never "..."
        cell.setStyle(VALUE_STYLE + (color == null ? "" : "-fx-text-fill: " + color + ";"));
        Motion.countUp(cell, number, format, order);
        return cell;
    }

    /**
     * Formats a percent with a sign, using a real minus sign. Zero shows as
     * "0.0%", never "-0.0%".
     *
     * @param percent the value, 5.0 means +5%
     * @return for example "+5.0%" or "−10.1%"
     */
    static String signedPercent(double percent) {
        String text = String.format("%.1f%%", Math.abs(percent));
        if (text.equals("0.0%")) {
            return text;
        }
        return (percent > 0 ? "+" : MINUS) + text;
    }
}
