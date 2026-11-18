import java.io.IOException;
import java.util.List;
import java.util.prefs.Preferences;
import javafx.application.Application;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.concurrent.Task;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;

/**
 * The JavaFX app and its controller (MVC: the logic classes are the model, the
 * screens are the views, this class connects them). It builds every object once
 * and passes it down (dependency injection), switches between the Scenes, and
 * loads FRED data on a background thread so the window never freezes.
 */
public class StockApp extends Application {
    private static final String API_KEY_VARIABLE = "FRED_API_KEY";
    private static final String WINDOW_TITLE = "Glassfolio";
    private static final String LOADER_THREAD_NAME = "fred-loader";
    private static final String THEME_KEY = "theme";
    private static final Theme DEFAULT_THEME = Theme.DARK;
    private static final String PREFERENCES_NODE = "glassfolio";
    private static final double MIN_WIDTH = 860;
    private static final double MIN_HEIGHT = 640;
    private static final System.Logger LOG = System.getLogger(StockApp.class.getName());

    private final Preferences preferences = Preferences.userRoot().node(PREFERENCES_NODE);
    private final ObjectProperty<Theme> theme = new SimpleObjectProperty<>(loadTheme());
    private Stage stage;
    private FormScreen form;
    private HistoryCache history;
    private Market market;      // stays null when the app can't be set up
    private MarketConditions conditions;
    private String setupError;  // why the app can't load data, or null

    /**
     * Builds the objects and shows the form.
     *
     * @param stage the main window, given by JavaFX
     */
    @Override
    public void start(Stage stage) {
        this.stage = stage;
        Styles.loadFonts();
        theme.addListener((obs, oldTheme, newTheme) -> preferences.put(THEME_KEY, newTheme.name()));
        form = new FormScreen(this::suggest, theme);
        try {
            history = new HistoryCache(new FredClient(System.getenv(API_KEY_VARIABLE)));
            market = new Market(history);
            conditions = new MarketConditions(history);
        } catch (IllegalArgumentException e) {
            setupError = e.getMessage();
            form.showError(setupError);
        }
        stage.setTitle(WINDOW_TITLE);
        stage.setMinWidth(MIN_WIDTH);
        stage.setMinHeight(MIN_HEIGHT);
        stage.addEventHandler(KeyEvent.KEY_PRESSED, e -> {
            if (e.getCode() == KeyCode.F11) {
                stage.setFullScreen(!stage.isFullScreen());
            }
        });
        form.show(stage);
        stage.show();
        loadPreview();
    }

    /**
     * Loads the market overview for the start screen in the background. It is
     * extra, so a failure is only logged and the strip stays hidden.
     */
    private void loadPreview() {
        if (market == null) {
            return;
        }
        Task<Loaded> load = loadTask();
        load.setOnSucceeded(e -> form.showMarketPreview(load.getValue().indexes(), history, load.getValue().economy()));
        load.setOnFailed(e -> LOG.log(System.Logger.Level.WARNING, "Market preview unavailable", load.getException()));
        startInBackground(load);
    }

    /**
     * Picks the user's strategy and loads the indexes in the background.
     * Runs when the form sends a valid User.
     *
     * @param user the user to advise
     */
    private void suggest(User user) {
        if (market == null) {
            form.showError(setupError);
            return;
        }
        Stratgey strategy = (user.getRiskProfile() == User.STABILITY_PROFILE)
                ? new Stability(Stability.DEFAULT_MAX_VOLATILITY)
                : new Growth(Growth.DEFAULT_MIN_GROWTH);

        Task<Loaded> load = loadTask();
        load.setOnSucceeded(e -> {
            form.showBusy(false);
            List<Stock> indexes = load.getValue().indexes();
            new ResultsScreen(strategy.getName(), Suggestion.rank(strategy, user, indexes), indexes,
                    history, load.getValue().economy(), theme, () -> form.show(stage)).show(stage);
        });
        load.setOnFailed(e -> {
            form.showBusy(false);
            String reason = load.getException().getMessage();
            form.showError("Could not load market data: "
                    + (reason == null ? "check your internet connection and try again." : reason));
        });

        form.showBusy(true);
        startInBackground(load);
    }

    /**
     * Builds a task that loads the indexes and the economy numbers.
     *
     * @return the task, not started yet
     */
    private Task<Loaded> loadTask() {
        return new Task<>() {
            @Override
            protected Loaded call() throws IOException, InterruptedException {
                return new Loaded(market.loadIndexes(), loadEconomy());
            }
        };
    }

    /**
     * Runs a task on its own thread so the window never freezes.
     *
     * @param task the task to run
     */
    private static void startInBackground(Task<?> task) {
        Thread loader = new Thread(task, LOADER_THREAD_NAME);
        loader.setDaemon(true); // don't keep the app alive if the window closes mid-download
        loader.start();
    }

    /**
     * Loads the economy numbers. They are extra, so a failure here hides the
     * strip instead of blocking the suggestions.
     *
     * @return the snapshot, or null if it couldn't be loaded
     * @throws InterruptedException if the loading thread is interrupted
     */
    private EconomySnapshot loadEconomy() throws InterruptedException {
        try {
            return conditions.load();
        } catch (IOException | IllegalStateException e) {
            LOG.log(System.Logger.Level.WARNING, "Economy numbers unavailable", e);
            return null;
        }
    }

    /**
     * Reads the theme the user picked last time.
     *
     * @return the saved theme, or the default if none was saved or the value is unknown
     */
    private Theme loadTheme() {
        try {
            return Theme.valueOf(preferences.get(THEME_KEY, DEFAULT_THEME.name()));
        } catch (IllegalArgumentException e) {
            return DEFAULT_THEME;
        }
    }

    /**
     * Everything the background task loads for the results screen.
     *
     * @param indexes the loaded indexes
     * @param economy the economy numbers, or null if they couldn't be loaded
     */
    private record Loaded(List<Stock> indexes, EconomySnapshot economy) { }
}
