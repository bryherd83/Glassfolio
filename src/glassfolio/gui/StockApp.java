package glassfolio.gui;

import glassfolio.data.ApiKeyStore;
import glassfolio.data.FredClient;
import glassfolio.data.HistoryCache;
import glassfolio.data.VisitStore;
import glassfolio.model.EconomySnapshot;
import glassfolio.model.Growth;
import glassfolio.model.Market;
import glassfolio.model.MarketConditions;
import glassfolio.model.Stability;
import glassfolio.model.Stock;
import glassfolio.model.Strategy;
import glassfolio.model.Suggestion;
import glassfolio.model.User;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.prefs.BackingStoreException;
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
 * and passes it down (dependency injection), swaps screens in one Scene, and
 * loads FRED data on a background thread so the window never freezes.
 */
public class StockApp extends Application {
    private static final String KEY_TEST_SERIES = "SP500";
    private static final int KEY_TEST_DAYS = 14;
    private static final String KEY_TEST_UNITS = "lin";
    private static final String WINDOW_TITLE = "Glassfolio";
    private static final String LOADER_THREAD_NAME = "fred-loader";
    private static final Theme START_THEME = Theme.LIGHT; // the glass look; users can switch to dark after
    private static final String PREFERENCES_NODE = "glassfolio";
    private static final double MIN_WIDTH = 860;
    private static final double MIN_HEIGHT = 640;
    private static final System.Logger LOG = System.getLogger(StockApp.class.getName());

    private final Preferences preferences = Preferences.userRoot().node(PREFERENCES_NODE);
    private final ObjectProperty<Theme> theme = new SimpleObjectProperty<>(START_THEME);
    private final ApiKeyStore keyStore = new ApiKeyStore(preferences);
    private final VisitStore visitStore = new VisitStore(preferences);
    private Stage stage;
    private FormScreen form;
    private KeyScreen keyScreen;
    private HistoryCache history;
    private Market market;      // stays null until there is an API key
    private MarketConditions conditions;

    /**
     * Builds the objects and shows the form, or the key screen if there is no API key yet.
     *
     * @param stage the main window, given by JavaFX
     */
    @Override
    public void start(Stage stage) {
        this.stage = stage;
        Styles.loadFonts();
        form = new FormScreen(this::suggest, theme, () -> keyScreen.show(stage, market != null));
        VisitStore.Visit visit = visitStore.load();
        if (visit != null) {
            form.showWelcome(visit, this::forgetVisit);
        }
        keyScreen = new KeyScreen(this::checkKey, getHostServices()::showDocument, () -> form.show(stage));
        String key = keyStore.find(System.getenv(ApiKeyStore.ENV_VARIABLE));
        if (key != null) {
            connect(key);
        }
        stage.setTitle(WINDOW_TITLE);
        stage.getIcons().setAll(AppIcon.images()); // our own symbol in the title bar and taskbar
        stage.setMinWidth(MIN_WIDTH);
        stage.setMinHeight(MIN_HEIGHT);
        stage.addEventHandler(KeyEvent.KEY_PRESSED, e -> {
            if (e.getCode() == KeyCode.F11) {
                stage.setFullScreen(!stage.isFullScreen());
            }
        });
        stage.setScene(GlassTheme.buildScene(theme));
        new BootScreen(this::showFirstScreen).show(stage);
        stage.setFullScreenExitHint("Press F11 or Esc to leave full screen.");
        stage.setFullScreen(true);
        stage.show();
        loadPreview();
    }

    /** Shows the form, or the key screen if there is no API key yet. */
    private void showFirstScreen() {
        if (market == null) {
            keyScreen.show(stage, false);
        } else {
            form.show(stage);
        }
    }

    /**
     * Builds the data objects around a key in valid format.
     *
     * @param key the FRED API key
     */
    private void connect(String key) {
        history = new HistoryCache(new FredClient(key));
        market = new Market(history);
        conditions = new MarketConditions(history);
    }

    /**
     * Tests a key with one small FRED request in the background. If it works,
     * saves it (or deletes the saved one when the user unticked Remember) and
     * opens the form.
     *
     * @param key      a key in valid format
     * @param remember true if the user wants it saved on this computer
     */
    private void checkKey(String key, boolean remember) {
        FredClient client = new FredClient(key);
        Task<List<Double>> test = new Task<>() {
            @Override
            protected List<Double> call() throws IOException, InterruptedException {
                return client.getValues(KEY_TEST_SERIES, LocalDate.now().minusDays(KEY_TEST_DAYS), KEY_TEST_UNITS);
            }
        };
        test.setOnSucceeded(e -> {
            try {
                if (remember) {
                    keyStore.save(key);
                } else {
                    keyStore.forget();
                }
            } catch (BackingStoreException ex) { // the key still works for this session
                LOG.log(System.Logger.Level.WARNING, "Could not update the saved API key: " + ex.getClass().getSimpleName());
            }
            connect(key);
            form.showBusy(false); // clears any old message
            form.show(stage);
            loadPreview();
        });
        test.setOnFailed(e -> {
            keyScreen.showBusy(false);
            LOG.log(System.Logger.Level.WARNING, "Key check failed: " + test.getException().getClass().getSimpleName());
            keyScreen.showError("FRED didn't accept that key, or couldn't be reached. Check the key and your "
                    + "internet connection, then try again.");
        });
        keyScreen.showBusy(true);
        startInBackground(test);
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
        load.setOnFailed(e -> LOG.log(System.Logger.Level.WARNING, "Market preview unavailable: " + load.getException().getClass().getSimpleName()));
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
            keyScreen.show(stage, false);
            return;
        }
        Task<Loaded> load = loadTask();
        load.setOnSucceeded(e -> {
            form.showBusy(false);
            showResults(user, load.getValue(), true);
        });
        load.setOnFailed(e -> {
            form.showBusy(false);
            LOG.log(System.Logger.Level.WARNING, "Market data failed: " + load.getException().getClass().getSimpleName());
            form.showError("Couldn't load market data. Check your internet connection, then try again.");
        });

        form.showBusy(true);
        startInBackground(load);
    }

    /**
     * Ranks the loaded indexes for the user's profile and shows the results. The
     * switch button re-runs this with the other profile on the same data, so it
     * needs no second download.
     *
     * @param user     the user to advise
     * @param loaded   the indexes and economy numbers already loaded
     * @param fromForm true when the user submitted the form, false for the other-profile switch
     */
    private void showResults(User user, Loaded loaded, boolean fromForm) {
        boolean stability = user.getRiskProfile() == User.STABILITY_PROFILE;
        Strategy strategy = stability
                ? new Stability(Stability.DEFAULT_MAX_VOLATILITY)
                : new Growth(Growth.DEFAULT_MIN_GROWTH);
        List<Stock> indexes = loaded.indexes();
        ResultsData data = new ResultsData(strategy.getName(), strategy.rule(user), Suggestion.rank(strategy, user, indexes),
                Suggestion.missed(strategy, user, indexes), indexes, history, loaded.economy(), user.getBudget());

        // only what the user chose on the form is remembered, not the other-profile preview
        if (fromForm && form.remembersVisitor() && !data.picks().isEmpty()) {
            rememberVisit(user, data.picks().get(0).stock());
        } else if (fromForm) { // not remembered, or no pick to remember: don't keep an old visit
            forgetVisit();
        }
        int otherProfile = stability ? User.GROWTH_PROFILE : User.STABILITY_PROFILE;
        User other = new User(user.getUsername(), user.getPassword(), user.getBudget(), otherProfile);
        String switchLabel = stability ? "See growth picks" : "See stability picks";
        new ResultsScreen(data, theme, switchLabel, () -> showResults(other, loaded, false), () -> form.show(stage))
                .show(stage);
    }

    /**
     * Saves this visit, so next time the start screen can say "Welcome back". Remembering
     * is a convenience, so a failure is only logged.
     *
     * @param user the user who asked
     * @param top  their top pick
     */
    private void rememberVisit(User user, Stock top) {
        try {
            visitStore.save(new VisitStore.Visit(user.getUsername(), user.getRiskProfile(), top.getTicker(),
                    top.getName(), top.getPrice(), LocalDate.now()));
        } catch (BackingStoreException | IllegalArgumentException e) {
            // only the error's type is logged, so a name can never end up in a log
            LOG.log(System.Logger.Level.WARNING, "Could not save this visit: " + e.getClass().getSimpleName());
        }
    }

    /**
     * Deletes the saved visit, after "Not you?" or when Remember me is unticked.
     *
     * @return true if it was deleted
     */
    private boolean forgetVisit() {
        try {
            visitStore.forget();
            return true;
        } catch (BackingStoreException e) {
            LOG.log(System.Logger.Level.WARNING, "Could not forget the saved visit: " + e.getClass().getSimpleName());
            return false;
        }
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
            LOG.log(System.Logger.Level.WARNING, "Economy numbers unavailable: " + e.getClass().getSimpleName());
            return null;
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
