package glassfolio.data;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import java.util.regex.Pattern;

/**
 * Remembers the user's last visit on this computer, so the start screen can say
 * "Welcome back" and show how their top pick moved since then. Only the name,
 * the profile, the top pick and its price are kept, in the same per-user Java
 * preferences as the API key. Nothing leaves the computer, and {@link #forget}
 * deletes it all.
 */
public class VisitStore {
    /**
     * One saved visit.
     *
     * @param name      the name the user typed
     * @param profile   the risk profile they used (User.STABILITY_PROFILE or User.GROWTH_PROFILE)
     * @param ticker    the top pick's series id, for example "SP500"
     * @param pickName  the top pick's display name, for example "S&P 500"
     * @param price     the top pick's price that day
     * @param date      the day of the visit
     */
    public record Visit(String name, int profile, String ticker, String pickName, double price, LocalDate date) {
        /**
         * Checks the visit's values.
         *
         * @throws IllegalArgumentException if any value is missing or out of range
         */
        public Visit {
            if (name == null || name.isBlank() || name.length() > MAX_NAME_LENGTH) {
                throw new IllegalArgumentException("A visit needs a name of at most " + MAX_NAME_LENGTH + " characters");
            }
            if (profile != STABILITY && profile != GROWTH) {
                throw new IllegalArgumentException("Unknown profile");
            }
            if (ticker == null || !TICKER_FORMAT.matcher(ticker).matches()) {
                throw new IllegalArgumentException("A visit needs a series id like SP500");
            }
            if (pickName == null || pickName.isBlank() || pickName.length() > MAX_NAME_LENGTH) {
                throw new IllegalArgumentException("A visit needs its top pick's name");
            }
            if (!(price > 0) || !Double.isFinite(price)) {
                throw new IllegalArgumentException("A visit needs a positive price");
            }
            if (date == null || date.isAfter(LocalDate.now())) {
                throw new IllegalArgumentException("A visit needs a date that isn't in the future");
            }
        }
    }

    private static final int MAX_NAME_LENGTH = 60; // matches User.MAX_NAME_LENGTH
    private static final Pattern TICKER_FORMAT = Pattern.compile("[A-Z0-9]{1,20}"); // FRED series ids
    private static final int STABILITY = 1; // matches User.STABILITY_PROFILE; data can't depend on model
    private static final int GROWTH = 2;    // matches User.GROWTH_PROFILE
    private static final String NAME = "visitName";
    private static final String PROFILE = "visitProfile";
    private static final String TICKER = "visitTicker";
    private static final String PICK_NAME = "visitPickName";
    private static final String PRICE = "visitPrice";
    private static final String DATE = "visitDate";

    private static final System.Logger LOG = System.getLogger(VisitStore.class.getName());

    private final Preferences preferences;

    /**
     * Creates a store backed by the given preferences node.
     *
     * @param preferences where the visit is saved
     * @throws IllegalArgumentException if preferences is null
     */
    public VisitStore(Preferences preferences) {
        if (preferences == null) {
            throw new IllegalArgumentException("VisitStore needs a preferences node");
        }
        this.preferences = preferences;
    }

    /**
     * Saves a visit, replacing the last one.
     *
     * @param visit the visit
     * @throws BackingStoreException if it couldn't be written
     */
    public void save(Visit visit) throws BackingStoreException {
        preferences.put(NAME, visit.name());
        preferences.putInt(PROFILE, visit.profile());
        preferences.put(TICKER, visit.ticker());
        preferences.put(PICK_NAME, visit.pickName());
        preferences.putDouble(PRICE, visit.price());
        preferences.put(DATE, visit.date().toString());
        preferences.flush();
    }

    /**
     * Reads the last visit. A missing or hand-edited value means no visit.
     *
     * @return the last visit, or null if there is none
     */
    public Visit load() {
        String date = preferences.get(DATE, null);
        if (date == null) {
            return null;
        }
        try {
            return new Visit(preferences.get(NAME, null), preferences.getInt(PROFILE, 0),
                    preferences.get(TICKER, null), preferences.get(PICK_NAME, null),
                    preferences.getDouble(PRICE, 0), LocalDate.parse(date));
        } catch (IllegalArgumentException | DateTimeParseException e) {
            // only the error's type is logged, so the saved name never reaches a log
            LOG.log(System.Logger.Level.INFO, "Ignored a saved visit that is not valid: " + e.getClass().getSimpleName());
            return null;
        }
    }

    /**
     * Deletes the saved visit ("Not you?").
     *
     * @throws BackingStoreException if the change couldn't be written
     */
    public void forget() throws BackingStoreException {
        for (String key : new String[] {NAME, PROFILE, TICKER, PICK_NAME, PRICE, DATE}) {
            preferences.remove(key);
        }
        preferences.flush();
    }
}
