package glassfolio.gui;

import glassfolio.data.HistoryCache;
import glassfolio.data.VisitStore;
import glassfolio.model.EconomySnapshot;
import glassfolio.model.Stock;
import glassfolio.model.User;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import javafx.beans.property.ObjectProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Control;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;

/**
 * First screen: one rounded frame floating on the backdrop. The left panel holds
 * the wordmark, headline and form; the right panel is a picture of the market:
 * the particle chart on a sky-colored scene with the live market numbers along
 * its foot. It only checks the input and hands a valid User to its listener
 * (Observer pattern: the controller listens through onSubmit). No investing
 * logic lives here.
 */
public final class FormScreen {
    private static final String STABILITY = "Stability";
    private static final String GROWTH = "Growth";
    private static final double FORM_SHARE = 0.4; // the form panel takes 40% of the frame
    private static final double FORM_MIN_WIDTH = 340;
    private static final double FRAME_MAX_WIDTH = 1360;
    private static final double FRAME_MAX_HEIGHT = 860;
    private static final Insets FRAME_MARGIN = new Insets(28, 48, 28, 48); // leaves room to see the background
    private static final double FRAME_INSET = 12; // the gap between the frame's edge and the scene panel
    private static final double SCENE_RADIUS = 22;
    private static final Insets FORM_PADDING = new Insets(28, 44, 24, 44);
    private static final double LABEL_GAP = 5;
    private static final double FORM_SPACING = 10;
    private static final double SECTION_GAP = 18;
    private static final double BUTTON_ROW_SPACING = 12;
    private static final double SPINNER_SIZE = 24;
    private static final Insets CAPTION_PADDING = new Insets(22, 26, 0, 26);
    private static final String BUDGET_PATTERN = "\\d{0,12}(\\.\\d{0,2})?";
    private static final String NO_PASSWORD = ""; // the app has no sign-in yet, so it never asks for one
    private static final double BRAND_SPACING = 10;

    private static final String SCENE_STYLE = "-fx-background-color: linear-gradient(to bottom, -scene-top, -scene-bottom);"
            + " -fx-background-radius: " + SCENE_RADIUS + ";";
    /** The headline fades from ink through the accent to pink, so the title carries the page's color. */
    private static final String HEADLINE_STYLE = Styles.HERO
            + " -fx-text-fill: linear-gradient(to right, -text 0%, -accent 55%, -series-2 100%);";
    private static final String BLURB_STYLE = "-fx-font-family: 'Poppins'; -fx-font-size: 14px; -fx-fill: -rank-text;";
    private static final String BLURB_POP_STYLE = Styles.HEADING + "-fx-font-size: 14px; -fx-fill: -accent;";
    /** The tinted box the blurb sits in, with a glow so it reads as a card. */
    private static final String BLURB_BOX_STYLE = Styles.TINT_BOX + "-fx-effect: dropshadow(gaussian, -glow, 20, 0, 0, 6);";
    private static final String FIELD_LABEL_STYLE = Styles.CAPS;
    private static final String HINT_STYLE = Styles.BODY;
    /** The profile callout: a violet-to-pink tint with a bold accent bar, so it reads as a highlight. */
    private static final String CALLOUT_STYLE = "-fx-font-size: 13px; -fx-line-spacing: 2px; -fx-text-fill: -rank-text;"
            + " -fx-background-color: linear-gradient(to right, -tint-1, -tint-2);"
            + " -fx-background-radius: 14; -fx-border-color: transparent transparent transparent -accent;"
            + " -fx-border-width: 0 0 0 4; -fx-border-radius: 14; -fx-padding: 8 12 8 14;";
    private static final String WELCOME_STYLE = Styles.LABEL;
    private static final DateTimeFormatter VISIT_DATE = DateTimeFormatter.ofPattern("MMM d", Locale.US);
    private static final double PERCENT = 100;
    private static final String PICK_PROMPT = "Stability picks steadier indexes. Growth picks the one that rose most.";
    private static final String NOTE_STYLE = Styles.BOX_TEXT + "-fx-font-size: 12px;";
    private static final String CAPTION_STYLE = Styles.HEADING + "-fx-font-size: 14px; -fx-text-fill: -scene-text;";
    private static final String LINK_STYLE = Styles.LINK;

    private final TextField usernameField = new TextField();
    private final TextField budgetField = new TextField();
    private final ToggleButton stabilityButton = new ToggleButton(STABILITY);
    private final ToggleButton growthButton = new ToggleButton(GROWTH);
    private final ToggleGroup riskGroup = new ToggleGroup();
    private final Label profileHint = new Label(PICK_PROMPT);
    private final Button suggestButton = new Button("Get suggestions");
    private final ProgressIndicator spinner = new ProgressIndicator();
    private final Label statusLabel = new Label();
    private final Hyperlink changeKeyLink = new Hyperlink("Change API key");
    private final Consumer<User> onSubmit;
    private final ParticleChart chart;
    private final HBox frame;
    private final MarketStrip marketStrip = new MarketStrip();
    private final Label chartCaption = new Label();
    private final EconomyPanel economyPanel = new EconomyPanel();
    private final RiskQuizPanel quiz = new RiskQuizPanel(this::pickProfile, () -> showQuiz(false));
    private final Label welcomeLine = new Label();
    private final Label sinceLine = new Label();
    private final Hyperlink notYouLink = new Hyperlink("Not you?");
    private final CheckBox rememberBox = new CheckBox("Remember me");
    private final VBox welcomeBox = new VBox(LABEL_GAP);
    private VBox fieldsBox;
    private VisitStore.Visit lastVisit; // null for a first visit
    private boolean clearing; // true while "Not you?" empties the profile choice
    private final Region screenRoot;

    /**
     * Builds the form.
     *
     * @param onSubmit    called with the new User when the input is valid
     * @param theme       the app's current theme; the switch on this screen changes it
     * @param onChangeKey opens the API key screen
     * @throws IllegalArgumentException if onSubmit or onChangeKey is null
     */
    public FormScreen(Consumer<User> onSubmit, ObjectProperty<Theme> theme, Runnable onChangeKey) {
        if (onSubmit == null || onChangeKey == null) {
            throw new IllegalArgumentException("FormScreen needs a submit listener and a change-key action");
        }
        this.onSubmit = onSubmit;
        changeKeyLink.setStyle(LINK_STYLE);
        changeKeyLink.setOnAction(e -> onChangeKey.run());

        usernameField.setPromptText("For example Alex");
        usernameField.setTextFormatter(new TextFormatter<String>(change ->
                change.getControlNewText().length() <= User.MAX_NAME_LENGTH ? change : null));
        rememberBox.setSelected(false); // nothing is saved unless the user asks
        rememberBox.setStyle("-fx-font-size: 14px; -fx-text-fill: -text;"); // in ink, not grey, so it is seen beside the button
        budgetField.setPromptText("For example 1000");
        // only digits and one decimal point can be typed, so most mistakes never happen
        budgetField.setTextFormatter(new TextFormatter<String>(change ->
                change.getControlNewText().matches(BUDGET_PATTERN) ? change : null));
        for (ToggleButton choice : List.of(stabilityButton, growthButton)) {
            choice.setToggleGroup(riskGroup);
            choice.setStyle(Styles.SEGMENT);
            choice.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(choice, Priority.ALWAYS);
            choice.selectedProperty().addListener((obs, was, selected) ->
                    choice.setStyle(selected ? Styles.SEGMENT_SELECTED : Styles.SEGMENT));
        }
        riskGroup.selectedToggleProperty().addListener((obs, old, now) -> {
            if (now == null) { // only "Not you?" clears the choice; a second click keeps it picked
                if (clearing) {
                    profileHint.setText(PICK_PROMPT);
                } else if (old != null) {
                    old.setSelected(true);
                }
                return;
            }
            profileHint.setText(ProfileGuide.describe(now == stabilityButton));
        });
        profileHint.setStyle(CALLOUT_STYLE); // a soft callout, set apart from the fields
        profileHint.setWrapText(true);
        profileHint.setMinHeight(Region.USE_PREF_SIZE);

        Styles.clickable(suggestButton, Styles.PRIMARY_BUTTON, Styles.PRIMARY_BUTTON_HOVER);
        suggestButton.setDefaultButton(true); // Enter in any field submits
        suggestButton.setOnAction(e -> submit());
        spinner.setPrefSize(SPINNER_SIZE, SPINNER_SIZE);
        spinner.setVisible(false);
        spinner.managedProperty().bind(spinner.visibleProperty()); // takes no room in the button row until shown
        statusLabel.setWrapText(true);
        statusLabel.setMinHeight(Region.USE_PREF_SIZE);

        chart = new ParticleChart(theme.get());
        theme.addListener((obs, oldTheme, newTheme) -> chart.setTheme(newTheme)); // this screen lives as long as the app

        VBox form = buildFormPanel(theme);
        StackPane scene = buildScenePanel();
        frame = new HBox(form, scene);
        form.prefWidthProperty().bind(frame.widthProperty().multiply(FORM_SHARE));
        HBox.setHgrow(scene, Priority.ALWAYS);
        frame.setPadding(new Insets(FRAME_INSET, FRAME_INSET, FRAME_INSET, 0));
        frame.setStyle(Styles.FRAME);
        frame.setMaxSize(FRAME_MAX_WIDTH, FRAME_MAX_HEIGHT);

        StackPane centered = new StackPane(Styles.shadowed(frame, Styles.FRAME_RADIUS, Styles.FRAME_SHADOW));
        centered.setPadding(FRAME_MARGIN);
        screenRoot = GlassTheme.buildRoot(centered);
        Styles.keepWholeText(screenRoot);
    }

    /**
     * Shows this screen in the window and plays the entrance animations.
     *
     * @param stage the app's window
     */
    public void show(Stage stage) {
        stage.getScene().setRoot(screenRoot); // same Scene, so full screen stays on
        Motion.playEntrance(frame);
        chart.play();
    }

    /**
     * Shows the live market overview along the foot of the picture panel.
     *
     * @param indexes the loaded indexes
     * @param history where each index's past year comes from
     * @param economy the economy numbers, or null to leave those tiles out
     */
    public void showMarketPreview(List<Stock> indexes, HistoryCache history, EconomySnapshot economy) {
        if (indexes.isEmpty()) {
            return;
        }
        marketStrip.show(indexes, history);
        showSinceLastVisit(indexes);
        Stock lead = indexes.get(0);
        double[] months = ParticleChart.monthlyValues(history.getPercentChange(lead.getTicker()));
        if (months.length >= 2) {
            chart.showData(months);
            chartCaption.setText(lead.getName() + ", month by month");
            chartCaption.setVisible(true);
        }
    }

    /**
     * Greets someone who has been here before: fills in their name and profile
     * and says "Welcome back". "Not you?" clears it all.
     *
     * @param visit    the last saved visit
     * @param onForget deletes the saved visit; returns false if that failed
     * @throws IllegalArgumentException if either argument is null
     */
    public void showWelcome(VisitStore.Visit visit, BooleanSupplier onForget) {
        if (visit == null || onForget == null) {
            throw new IllegalArgumentException("showWelcome needs a visit and a forget action");
        }
        lastVisit = visit;
        welcomeLine.setText("Welcome back, " + visit.name() + ".");
        usernameField.setText(visit.name());
        riskGroup.selectToggle(visit.profile() == User.STABILITY_PROFILE ? stabilityButton : growthButton);
        notYouLink.setOnAction(e -> {
            if (!onForget.getAsBoolean()) {
                showError("Couldn't forget your details. Try again.");
                return;
            }
            lastVisit = null;
            usernameField.clear();
            clearing = true;
            riskGroup.selectToggle(null);
            clearing = false;
            welcomeBox.setVisible(false);
        });
        welcomeBox.setVisible(true);
    }

    /**
     * Says whether "Remember me" is ticked.
     *
     * @return true if the user wants their name and last pick remembered on this computer
     */
    public boolean remembersVisitor() {
        return rememberBox.isSelected();
    }

    /**
     * Tells a returning user how their last top pick moved since their visit.
     *
     * @param indexes the indexes just loaded
     */
    private void showSinceLastVisit(List<Stock> indexes) {
        if (lastVisit == null || !lastVisit.date().isBefore(LocalDate.now())) {
            return; // nothing to compare on the same day
        }
        for (Stock stock : indexes) {
            if (stock.getTicker().equals(lastVisit.ticker())) {
                double change = (stock.getPrice() / lastVisit.price() - 1) * PERCENT;
                sinceLine.setText(String.format("Since %s, your last top pick, the %s, is %s %.1f%%.",
                        lastVisit.date().format(VISIT_DATE), lastVisit.pickName(),
                        change >= 0 ? "up" : "down", Math.abs(change)));
                return;
            }
        }
    }

    /**
     * Swaps the form for the quiz, or back.
     *
     * @param open true to show the quiz
     */
    private void showQuiz(boolean open) {
        if (open) {
            quiz.start();
        }
        quiz.node().setVisible(open);
        fieldsBox.setVisible(!open);
    }

    /**
     * Selects the profile the quiz picked and returns to the form.
     *
     * @param profile User.STABILITY_PROFILE or User.GROWTH_PROFILE
     */
    private void pickProfile(int profile) {
        boolean stability = profile == User.STABILITY_PROFILE;
        riskGroup.selectToggle(stability ? stabilityButton : growthButton);
        profileHint.setText("Picked from your answers. " + ProfileGuide.describe(stability));
        showQuiz(false);
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
        statusLabel.setText(busy ? "Loading market data..." : "");
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
            showError("Enter your name.");
            return;
        }
        if (riskGroup.getSelectedToggle() == null) {
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
        int riskProfile = stabilityButton.isSelected() ? User.STABILITY_PROFILE : User.GROWTH_PROFILE;

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
     * Builds the left panel: wordmark, headline, the form and a footer with the
     * key link, theme switch and the not-advice note.
     *
     * @param theme the app's current theme
     * @return the panel
     */
    private VBox buildFormPanel(ObjectProperty<Theme> theme) {
        HBox brandRow = Styles.wordmark();

        Label headline = new Label("Which index fits you?");
        headline.setStyle(HEADLINE_STYLE);
        headline.setWrapText(true);
        headline.setMinHeight(Region.USE_PREF_SIZE);
        TextFlow blurb = new TextFlow(plain("Pick your "), pop("risk"), plain(" level and we will rank the "),
                pop("S&P 500"), plain(", "), pop("NASDAQ"), plain(" and "), pop("Dow"), plain(" for you."));
        blurb.setLineSpacing(2);
        blurb.setMinHeight(Region.USE_PREF_SIZE);
        welcomeLine.setStyle(WELCOME_STYLE);
        sinceLine.setStyle(HINT_STYLE);
        sinceLine.setWrapText(true);
        sinceLine.setMinHeight(Region.USE_PREF_SIZE);
        notYouLink.setStyle(LINK_STYLE);
        HBox welcomeRow = new HBox(BRAND_SPACING, welcomeLine, notYouLink);
        welcomeRow.setAlignment(Pos.BASELINE_LEFT);
        welcomeBox.getChildren().setAll(welcomeRow, sinceLine);
        welcomeBox.managedProperty().bind(welcomeBox.visibleProperty());
        welcomeBox.setVisible(false); // only for someone who has been here before
        VBox blurbBox = new VBox(blurb);
        blurbBox.setStyle(BLURB_BOX_STYLE);
        VBox intro = new VBox(FORM_SPACING / 2, welcomeBox, headline, blurbBox);

        Hyperlink quizLink = new Hyperlink("Not sure? Take the 3-question quiz");
        quizLink.setStyle(LINK_STYLE);
        quizLink.setOnAction(e -> showQuiz(true));
        HBox submitRow = new HBox(BUTTON_ROW_SPACING, suggestButton, rememberBox, spinner);
        submitRow.setAlignment(Pos.CENTER_LEFT); // the box sits beside the button, one line instead of two
        VBox fields = new VBox(FORM_SPACING,
                labeled("NAME", usernameField),
                labeled("BUDGET ($)", budgetField),
                labeled("RISK PROFILE", stabilityButton,
                        new VBox(LABEL_GAP * 2, buildRiskPicker(), profileHint, quizLink)),
                submitRow,
                statusLabel);
        fields.setStyle(Styles.GROUP_CARD);
        fieldsBox = fields;
        fields.managedProperty().bind(fields.visibleProperty());
        quiz.node().managedProperty().bind(quiz.node().visibleProperty());
        quiz.node().setVisible(false);
        StackPane formOrQuiz = new StackPane(fields, quiz.node());
        formOrQuiz.setAlignment(Pos.TOP_LEFT);

        Region push = new Region(); // pushes the footer to the bottom of the panel
        VBox.setVgrow(push, Priority.ALWAYS);
        Region gap = new Region();
        HBox.setHgrow(gap, Priority.ALWAYS);
        HBox controls = new HBox(BRAND_SPACING, changeKeyLink, gap, GlassTheme.buildThemeSwitch(theme));
        controls.setAlignment(Pos.CENTER_LEFT);
        Label note = new Label(ProfileGuide.NOTE);
        note.setStyle(NOTE_STYLE);
        note.setWrapText(true);
        note.setMinHeight(Region.USE_PREF_SIZE);

        Label noteTitle = new Label("Good to know");
        noteTitle.setStyle(Styles.BOX_TITLE);
        VBox noteBox = new VBox(LABEL_GAP, noteTitle, note);
        noteBox.setStyle(Styles.TINT_BOX);

        VBox panel = new VBox(SECTION_GAP, brandRow, intro, formOrQuiz, push, new VBox(LABEL_GAP * 2, controls, noteBox));
        panel.setPadding(FORM_PADDING);
        panel.setMinWidth(FORM_MIN_WIDTH);
        return panel;
    }

    /**
     * Builds the right panel: the particle chart on a sky-colored scene, its
     * caption at the top and the market strip along the foot.
     *
     * @return the panel
     */
    private StackPane buildScenePanel() {
        chartCaption.setStyle(CAPTION_STYLE);
        chartCaption.setPadding(CAPTION_PADDING);
        chartCaption.setVisible(false); // shown once real data shapes the chart
        chartCaption.setMouseTransparent(true);
        StackPane scene = new StackPane(StarField.build(), chart, chartCaption, marketStrip.node());
        StackPane.setAlignment(chartCaption, Pos.TOP_LEFT);
        StackPane.setAlignment(marketStrip.node(), Pos.BOTTOM_CENTER);
        scene.setStyle(SCENE_STYLE);
        scene.setMinSize(0, 0);
        Rectangle clip = new Rectangle(); // keeps the chart's shards inside the rounded corners
        clip.setArcWidth(2 * SCENE_RADIUS);
        clip.setArcHeight(2 * SCENE_RADIUS);
        clip.widthProperty().bind(scene.widthProperty());
        clip.heightProperty().bind(scene.heightProperty());
        scene.setClip(clip);
        return scene;
    }

    /**
     * Builds the Stability / Growth picker: two choices in one pill-shaped groove.
     *
     * @return the picker
     */
    private HBox buildRiskPicker() {
        HBox groove = new HBox(stabilityButton, growthButton);
        groove.setStyle(Styles.WELL);
        return groove;
    }

    /** @return a run of blurb text in the light weight */
    private static Text plain(String text) {
        Text run = new Text(text);
        run.setStyle(BLURB_STYLE);
        return run;
    }

    /** @return a run of blurb text in the accent color and a heavier weight, so key words stand out */
    private static Text pop(String text) {
        Text run = new Text(text);
        run.setStyle(BLURB_POP_STYLE);
        return run;
    }

    /**
     * Puts a label above a group of controls and links it to the first one for screen readers.
     *
     * @param text  the label text
     * @param first the control the label names
     * @param group the controls as laid out
     * @return the label and group stacked together
     */
    private static Node labeled(String text, Control first, Node group) {
        Label label = new Label(text);
        label.setLabelFor(first);
        label.setStyle(FIELD_LABEL_STYLE);
        return new VBox(LABEL_GAP, label, group);
    }

    /**
     * Puts a label above a control and links them for screen readers.
     *
     * @param text    the label text
     * @param control the input control
     * @return the label and control stacked together
     */
    private static Node labeled(String text, Control control) {
        return labeled(text, control, control);
    }
}
