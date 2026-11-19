package glassfolio.gui;

import glassfolio.data.ApiKeyStore;
import java.util.function.Consumer;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * Asks for the FRED API key, so nobody has to set an environment variable. The
 * field hides what is typed. This screen only checks the key's format and hands
 * it to its listener (Observer pattern); the controller tests it with FRED and
 * saves it.
 */
public final class KeyScreen {
    /** FRED's page for getting a free key. */
    public static final String KEY_PAGE = "https://fred.stlouisfed.org/docs/api/api_key.html";
    private static final double CARD_WIDTH = 380;
    private static final double SPACING = 14;
    private static final double LABEL_GAP = 5;
    private static final double CARD_PADDING = 30;
    private static final double BUTTON_ROW_SPACING = 12;
    private static final double SPINNER_SIZE = 24;
    private static final String LINK_STYLE = Styles.LINK;

    private final PasswordField keyField = new PasswordField();
    private final CheckBox rememberBox = new CheckBox("Remember on this computer");
    private final Button connectButton = new Button("Connect");
    private final Button backButton = new Button("Back");
    private final ProgressIndicator spinner = new ProgressIndicator();
    private final Label statusLabel = new Label();
    private final VBox card;
    private final Region screenRoot;

    /**
     * What happens when the user submits a key in valid format.
     */
    @FunctionalInterface
    public interface KeyListener {
        /**
         * Called with a key in valid format.
         *
         * @param key      the cleaned key
         * @param remember true if the user wants it saved on this computer
         */
        void connect(String key, boolean remember);
    }

    /**
     * Builds the screen.
     *
     * @param onConnect called with a key in valid format
     * @param openLink  opens a web page in the user's browser
     * @param onBack    goes back to the form
     * @throws IllegalArgumentException if any argument is null
     */
    public KeyScreen(KeyListener onConnect, Consumer<String> openLink, Runnable onBack) {
        if (onConnect == null || openLink == null || onBack == null) {
            throw new IllegalArgumentException("KeyScreen needs a connect listener, a link opener and a back action");
        }
        Label title = new Label("Add your FRED key");
        title.setStyle(Styles.TITLE + "-fx-text-fill: linear-gradient(to right, -text, -accent);");
        Label intro = new Label("Glassfolio gets its prices from FRED, the Federal Reserve's free data "
                + "service.");
        intro.setStyle(Styles.BOX_TEXT + "-fx-font-size: 14px;");
        intro.setWrapText(true);

        Hyperlink getKey = new Hyperlink("Get a free key at fred.stlouisfed.org");
        getKey.setStyle(LINK_STYLE);
        getKey.setOnAction(e -> openLink.accept(KEY_PAGE));

        Label fieldLabel = new Label("API key");
        fieldLabel.setStyle(Styles.LABEL);
        fieldLabel.setLabelFor(keyField);
        keyField.setPromptText("32-character key");
        rememberBox.setSelected(false); // the key is stored only if the user asks
        rememberBox.setStyle(Styles.BODY);

        Styles.clickable(connectButton, Styles.PRIMARY_BUTTON, Styles.PRIMARY_BUTTON_HOVER);
        connectButton.setDefaultButton(true); // Enter in the field connects
        connectButton.setOnAction(e -> {
            String key = ApiKeyStore.clean(keyField.getText());
            if (!ApiKeyStore.isValidFormat(key)) {
                showError("That key doesn't look right. FRED keys are 32 letters and numbers.");
                return;
            }
            onConnect.connect(key, rememberBox.isSelected());
        });
        Styles.clickable(backButton, Styles.SECONDARY_BUTTON, Styles.SECONDARY_BUTTON_HOVER);
        backButton.setOnAction(e -> onBack.run());
        spinner.setPrefSize(SPINNER_SIZE, SPINNER_SIZE);
        spinner.setVisible(false);
        statusLabel.setWrapText(true);

        Label note = new Label("Your key is only sent to FRED, over a secure connection. "
                + "Leave the box unticked on a shared computer.");
        note.setStyle(Styles.BOX_TEXT);
        note.setWrapText(true);

        // the field and Connect share one pill, so it reads as one step
        keyField.setStyle(Styles.BARE_FIELD);
        HBox.setHgrow(keyField, Priority.ALWAYS);
        HBox keyWell = new HBox(keyField, connectButton);
        keyWell.setAlignment(Pos.CENTER_LEFT);
        keyWell.setStyle(Styles.WELL);
        keyField.focusedProperty().addListener((obs, was, focused) ->
                keyWell.setStyle(focused ? Styles.WELL_FOCUSED : Styles.WELL));

        HBox buttons = new HBox(BUTTON_ROW_SPACING, backButton, spinner);
        buttons.setAlignment(Pos.CENTER_LEFT);
        VBox introBox = new VBox(LABEL_GAP, intro, getKey);
        introBox.setStyle(Styles.TINT_BOX);
        VBox keyGroup = new VBox(SPACING, new VBox(LABEL_GAP, fieldLabel, keyWell), rememberBox, buttons, statusLabel);
        keyGroup.setStyle(Styles.GROUP_CARD);
        Label noteTitle = new Label("Your key stays private");
        noteTitle.setStyle(Styles.BOX_TITLE);
        VBox noteBox = new VBox(LABEL_GAP, noteTitle, note);
        noteBox.setStyle(Styles.TINT_BOX + "-fx-border-color: transparent transparent transparent -accent; -fx-border-width: 0 0 0 4;");
        card = new VBox(SPACING, Styles.wordmark(), title, introBox, keyGroup, noteBox);
        card.setPadding(new Insets(CARD_PADDING));
        card.setStyle(Styles.FRAME); // the same frame as the other screens
        card.setPrefWidth(CARD_WIDTH);
        card.setMaxSize(VBox.USE_PREF_SIZE, VBox.USE_PREF_SIZE);
        screenRoot = GlassTheme.buildRoot(new StackPane(Styles.shadowed(card, Styles.FRAME_RADIUS, Styles.FRAME_SHADOW)));
        Styles.keepWholeText(screenRoot);
    }

    /**
     * Shows this screen with an empty field.
     *
     * @param stage     the app's window
     * @param canGoBack true if a working key is already set, so Back is offered
     */
    public void show(Stage stage, boolean canGoBack) {
        keyField.clear(); // don't leave a key sitting in the field
        statusLabel.setText("");
        showBusy(false);
        backButton.setVisible(canGoBack);
        backButton.setManaged(canGoBack);
        stage.getScene().setRoot(screenRoot);
        Motion.playEntrance(card);
        keyField.requestFocus();
    }

    /**
     * Shows that the key is being checked.
     *
     * @param busy true while FRED is being asked
     */
    public void showBusy(boolean busy) {
        connectButton.setDisable(busy);
        spinner.setVisible(busy);
        if (busy) {
            statusLabel.setStyle(Styles.STATUS);
            statusLabel.setText("Checking your key with FRED");
        }
    }

    /**
     * Shows an error message under the buttons.
     *
     * @param message what went wrong, in plain words
     */
    public void showError(String message) {
        statusLabel.setStyle(Styles.ERROR);
        statusLabel.setText(message);
    }
}
