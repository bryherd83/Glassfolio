package glassfolio.data;

import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import java.util.regex.Pattern;

/**
 * Finds and remembers the FRED API key, so users can paste it into the app
 * instead of setting an environment variable. The saved key sits in the user's
 * Java preferences (on Windows, the current user's registry), which other
 * Windows accounts can't read. That is the same protection the
 * environment variable gets. The key is never logged or written anywhere else.
 */
public class ApiKeyStore {
    /** Environment variable that also holds a key, for users who set one up. */
    public static final String ENV_VARIABLE = "FRED_API_KEY";
    // FRED keys are 32 lowercase letters and digits, so nothing else can reach the request URL
    private static final Pattern KEY_FORMAT = Pattern.compile("[a-z0-9]{32}");
    private static final String SAVED_KEY = "fredApiKey";

    private final Preferences preferences;

    /**
     * Creates a store backed by the given preferences node.
     *
     * @param preferences where the key is saved
     * @throws IllegalArgumentException if preferences is null
     */
    public ApiKeyStore(Preferences preferences) {
        if (preferences == null) {
            throw new IllegalArgumentException("ApiKeyStore needs a preferences node");
        }
        this.preferences = preferences;
    }

    /**
     * Checks that a key has FRED's format.
     *
     * @param key the key to check
     * @return true if it is 32 lowercase letters and digits
     */
    public static boolean isValidFormat(String key) {
        return key != null && KEY_FORMAT.matcher(key).matches();
    }

    /**
     * Cleans up a pasted key: drops spaces around it and lowercases it.
     *
     * @param typed what the user typed or pasted
     * @return the cleaned key, which may still be invalid
     */
    public static String clean(String typed) {
        return typed == null ? "" : typed.strip().toLowerCase();
    }

    /**
     * Finds a key: the one saved in the app first, then the environment variable.
     * A saved value that isn't a valid key (for example, edited by hand) is ignored.
     *
     * @param environmentValue the environment variable's value, or null if it isn't set
     * @return a key in valid format, or null if there is none
     */
    public String find(String environmentValue) {
        String saved = preferences.get(SAVED_KEY, null);
        if (isValidFormat(saved)) {
            return saved;
        }
        String fromEnvironment = clean(environmentValue);
        return isValidFormat(fromEnvironment) ? fromEnvironment : null;
    }

    /**
     * Saves a key so the app finds it next time.
     *
     * @param key a key in valid format
     * @throws IllegalArgumentException if the key isn't in FRED's format
     * @throws BackingStoreException    if the key couldn't be written
     */
    public void save(String key) throws BackingStoreException {
        if (!isValidFormat(key)) {
            throw new IllegalArgumentException("A FRED API key is 32 letters and numbers.");
        }
        preferences.put(SAVED_KEY, key);
        preferences.flush();
    }

    /**
     * Deletes the saved key, if any.
     *
     * @throws BackingStoreException if the change couldn't be written
     */
    public void forget() throws BackingStoreException {
        preferences.remove(SAVED_KEY);
        preferences.flush();
    }
}
