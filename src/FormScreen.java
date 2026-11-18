import java.util.List;
import java.util.Random;
import java.util.function.Consumer;
import javafx.beans.property.ObjectProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;

/**
 * First screen: the particle chart on the left, the form and a profile guide on
 * the right, and a live market strip along the bottom. It only checks the input
 * and hands a valid User to its listener (Observer pattern: the controller
 * listens through onSubmit). No investing logic lives here.
 */
public final class FormScreen {
    private static final String STABILITY = "Stability";
    private static final String GROWTH = "Growth";
    private static final double CHART_SHARE = 0.57; // the chart takes a bit more than half the width
    private static final double FIELD_WIDTH = 300;
    private static final double LABEL_GAP = 5;
    private static final double FORM_SPACING = 14;
    private static final double CARD_PADDING = 30;
    private static final double BUTTON_ROW_SPACING = 12;
    private static final double SPINNER_SIZE = 24;
    private static final double OUTER_PADDING = 40;
    private static final double BLURB_WIDTH = 340;
    private static final double HEADLINE_SPACING = 10;
    private static final Insets SWITCH_PADDING = new Insets(20, 24, 0, 0);
    private static final int STAR_COUNT = 70;
    private static final long STAR_SEED = 17;
    private static final double STAR_MIN_RADIUS = 0.6;
    private static final double STAR_RADIUS_RANGE = 1.4;
    private static final double STAR_MIN_OPACITY = 0.12;
    private static final double STAR_OPACITY_RANGE = 0.4;
    private static final double GUIDE_MIN_HEIGHT = 820; // below this the guide would crowd the form
    private static final String NO_PASSWORD = ""; // the app has no sign-in yet, so it never asks for one

    private static final String BRAND_STYLE = Styles.DISPLAY_FONT + "-fx-font-size: 20px; -fx-font-style: italic; -fx-text-fill: -text-2;";
    private static final String HEADLINE_STYLE = Styles.DISPLAY_FONT + "-fx-font-size: 36px; -fx-text-fill: -text;";
    private static final String BLURB_STYLE = "-fx-font-size: 13px; -fx-text-fill: -text-3;";
    private static final String FIELD_LABEL_STYLE = "-fx-font-size: 13px; -fx-text-fill: -text-2;";

    private final TextField usernameField = new TextField();
    private final TextField budgetField = new TextField();
    private final ComboBox<String> riskBox = new ComboBox<>();
    private final Button suggestButton = new Button("Get suggestions");
    private final ProgressIndicator spinner = new ProgressIndicator();
    private final Label statusLabel = new Label();
    private final Consumer<User> onSubmit;
    private final ParticleChart chart;
    private final VBox card;
    private final MarketStrip marketStrip = new MarketStrip();
    private final Scene scene;

    /**
     * Builds the form.
     *
     * @param onSubmit called with the new User when the input is valid
     * @param theme    the app's current theme; the switch on this screen changes it
     * @throws IllegalArgumentException if onSubmit is null
     */
    public FormScreen(Consumer<User> onSubmit, ObjectProperty<Theme> theme) {
        if (onSubmit == null) {
            throw new IllegalArgumentException("FormScreen needs a submit listener");
        }
        this.onSubmit = onSubmit;

        usernameField.setPromptText("Your name");
        budgetField.setPromptText("For example 1000");
        riskBox.getItems().addAll(STABILITY, GROWTH);
        riskBox.setPromptText("Choose one");
        riskBox.setMaxWidth(Double.MAX_VALUE);

        suggestButton.setStyle(Styles.PRIMARY_BUTTON);
        suggestButton.setDefaultButton(true); // Enter in any field submits
        suggestButton.setOnAction(e -> submit());
        Motion.addHoverPop(suggestButton);

        spinner.setPrefSize(SPINNER_SIZE, SPINNER_SIZE);
        spinner.setVisible(false);
        statusLabel.setWrapText(true);
        statusLabel.setMaxWidth(FIELD_WIDTH);

        chart = new ParticleChart(theme.get());
        theme.addListener((obs, oldTheme, newTheme) -> chart.setTheme(newTheme)); // this screen lives as long as the app
        card = buildCard();

        StackPane chartArea = new StackPane(buildStars(), chart, buildHeadline());
        StackPane.setAlignment(chartArea.getChildren().get(2), Pos.TOP_LEFT);
        chartArea.setMinWidth(0);
        VBox guide = ProfileGuide.build();
        guide.maxWidthProperty().bind(card.widthProperty());
        VBox rightColumn = new VBox(FORM_SPACING, card, guide);
        rightColumn.setAlignment(Pos.CENTER);
        StackPane formArea = new StackPane(rightColumn);
        HBox.setHgrow(formArea, Priority.ALWAYS);
        guide.visibleProperty().bind(formArea.heightProperty().greaterThanOrEqualTo(GUIDE_MIN_HEIGHT));
        guide.managedProperty().bind(guide.visibleProperty());
        HBox columns = new HBox(chartArea, formArea);
        chartArea.prefWidthProperty().bind(columns.widthProperty().multiply(CHART_SHARE));

        HBox switchRow = new HBox(GlassTheme.buildThemeSwitch(theme));
        switchRow.setAlignment(Pos.TOP_RIGHT);
        switchRow.setPadding(SWITCH_PADDING);
        switchRow.setPickOnBounds(false); // clicks outside the button reach the chart
        BorderPane layout = new BorderPane(new StackPane(columns, switchRow));
        layout.setBottom(marketStrip.node());
        scene = GlassTheme.buildScene(layout, theme);
    }

    /**
     * Shows this screen in the window and plays the entrance animations.
     *
     * @param stage the app's window
     */
    public void show(Stage stage) {
        stage.setScene(scene);
        Motion.playEntrance(card);
        chart.play();
    }

    /**
     * Shows the live market overview along the bottom.
     *
     * @param indexes the loaded indexes
     * @param history where each index's past year comes from
     * @param economy the economy numbers, or null to leave those tiles out
     */
    public void showMarketPreview(List<Stock> indexes, HistoryCache history, EconomySnapshot economy) {
        marketStrip.show(indexes, history, economy);
    }

    /**
     * Turns the loading state on or off. While busy the button is disabled
     * so the user can't start a second download.
     *
     * @param busy true while market data is loading
     */
    public void showBusy(boolean busy) {
        suggestButton.setDisable(busy);
        spinner.setVisible(busy);
        statusLabel.setStyle(Styles.STATUS);
        statusLabel.setText(busy ? "Loading market data from FRED..." : "");
    }

    /**
     * Shows an error message under the form.
     *
     * @param message what went wrong, in plain words
     */
    public void showError(String message) {
        statusLabel.setStyle(Styles.ERROR);
        statusLabel.setText(message);
    }

    /** Checks every field and, if all are valid, passes a new User to the listener. */
    private void submit() {
        String username = usernameField.getText().trim();
        if (username.isEmpty()) {
            showError("Enter a username.");
            return;
        }
        if (riskBox.getValue() == null) {
            showError("Choose a risk profile.");
            return;
        }
        double budget;
        try {
            budget = Double.parseDouble(budgetField.getText().trim());
        } catch (NumberFormatException e) {
            showError("Budget must be a number, like 1000.");
            return;
        }
        if (!Double.isFinite(budget)) { // parseDouble also accepts "NaN" and "Infinity"
            showError("Budget must be a number, like 1000.");
            return;
        }
        int riskProfile = riskBox.getValue().equals(STABILITY) ? User.STABILITY_PROFILE : User.GROWTH_PROFILE;

        User user;
        try {
            user = new User(username, NO_PASSWORD, budget, riskProfile);
        } catch (IllegalArgumentException e) {
            showError(e.getMessage());
            return;
        }
        statusLabel.setText("");
        onSubmit.accept(user);
    }

    /**
     * Builds the form card on frosted glass.
     *
     * @return the card
     */
    private VBox buildCard() {
        Label title = new Label("Get your suggestion");
        title.setStyle(Styles.TITLE);
        VBox box = new VBox(FORM_SPACING, title,
                labeled("Username", usernameField),
                labeled("Budget ($)", budgetField),
                labeled("Risk profile", riskBox),
                new HBox(BUTTON_ROW_SPACING, suggestButton, spinner),
                statusLabel);
        box.setPadding(new Insets(CARD_PADDING));
        box.setStyle(Styles.GLASS);
        box.setMaxSize(VBox.USE_PREF_SIZE, VBox.USE_PREF_SIZE);
        return box;
    }

    /**
     * Builds the brand name and headline that sit over the particle chart.
     *
     * @return the headline block
     */
    private static VBox buildHeadline() {
        Label brand = new Label("Glassfolio");
        brand.setStyle(BRAND_STYLE);
        Label headline = new Label("Index picks from live\nFederal Reserve data.");
        headline.setStyle(HEADLINE_STYLE);
        Label blurb = new Label("S&P 500, NASDAQ and Dow, ranked for how much risk you want.");
        blurb.setStyle(BLURB_STYLE);
        blurb.setWrapText(true);
        blurb.setMaxWidth(BLURB_WIDTH);
        VBox block = new VBox(HEADLINE_SPACING, brand, headline, blurb);
        block.setPadding(new Insets(OUTER_PADDING));
        block.setMaxSize(VBox.USE_PREF_SIZE, VBox.USE_PREF_SIZE);
        block.setMouseTransparent(true); // let the mouse reach the chart
        return block;
    }

    /**
     * Scatters faint, still stars behind the chart. They keep their places
     * relative to the area at any size. The light theme makes them transparent,
     * since stars only read on a dark sky.
     *
     * @return the star layer
     */
    private static Pane buildStars() {
        Random random = new Random(STAR_SEED);
        Pane layer = new Pane();
        Group stars = new Group();
        for (int i = 0; i < STAR_COUNT; i++) {
            Circle star = new Circle(STAR_MIN_RADIUS + random.nextDouble() * STAR_RADIUS_RANGE);
            star.centerXProperty().bind(layer.widthProperty().multiply(random.nextDouble()));
            star.centerYProperty().bind(layer.heightProperty().multiply(random.nextDouble()));
            star.setStyle("-fx-fill: -star;");
            star.setOpacity(STAR_MIN_OPACITY + random.nextDouble() * STAR_OPACITY_RANGE);
            stars.getChildren().add(star);
        }
        stars.setManaged(false);
        layer.getChildren().add(stars);
        layer.setMinSize(0, 0);
        layer.setMouseTransparent(true);
        return layer;
    }

    /**
     * Puts a label above a control and links them for screen readers.
     *
     * @param text    the label text
     * @param control the input control
     * @return the label and control stacked together
     */
    private static Node labeled(String text, Control control) {
        Label label = new Label(text);
        label.setLabelFor(control);
        label.setStyle(FIELD_LABEL_STYLE);
        control.setPrefWidth(FIELD_WIDTH);
        return new VBox(LABEL_GAP, label, control);
    }
}
