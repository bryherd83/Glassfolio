import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Console version of the Stock Suggestion System. Asks for the user's details,
 * loads index data and prints suggestions. All objects are built here and
 * passed down (dependency injection). The JavaFX GUI will replace this later.
 */
public class Main{
    private static final String API_KEY_VARIABLE = "FRED_API_KEY";

    /**
     * Runs the console app.
     *
     * @param args not used
     */
    public static void main(String[] args){
        IO.println("Welcome to the Stock Suggestion System");
        String username = readLine("Username:");
        String password = readLine("Password:");
        double budget = readPositiveDouble("What is your Budget?");
        int riskProfile = readRiskProfile();
        User user = new User(username, password, budget, riskProfile);

        Stratgey strategy = (riskProfile == User.STABILITY_PROFILE)
                ? new Stability(Stability.DEFAULT_MAX_VOLATILITY)
                : new Growth(Growth.DEFAULT_MIN_GROWTH);

        List<Stock> indexes;
        try {
            MarketDataSource source = new FredClient(System.getenv(API_KEY_VARIABLE));
            indexes = new Market(source).loadIndexes();
        } catch (IOException | IllegalArgumentException | IllegalStateException e) {
            IO.println("Could not load market data: " + e.getMessage());
            return;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            IO.println("Loading market data was interrupted.");
            return;
        }

        printSuggestions(strategy, user, indexes);
    }

    /**
     * Prints the indexes that pass the strategy, best score first.
     *
     * @param strategy the user's strategy
     * @param user     the user being advised
     * @param indexes  the loaded indexes
     */
    private static void printSuggestions(Stratgey strategy, User user, List<Stock> indexes) {
        List<Stock> ranked = new ArrayList<>(indexes);
        ranked.sort(Comparator.comparingDouble((Stock s) -> strategy.score(s, user)).reversed());

        IO.println("\nSuggestions for a " + strategy.getName() + " profile:");
        boolean found = false;
        for (Stock stock : ranked) {
            double score = strategy.score(stock, user);
            if (score <= 0) {
                continue;
            }
            found = true;
            IO.println(String.format("%-30s score %5.1f   price %,10.2f", stock.getName(), score, stock.getPrice()));
            IO.println("   " + strategy.explain(stock, user));
            IO.println(String.format("   $%,.2f invested a year ago would be worth $%,.2f today",
                    user.getBudget(), user.getBudget() * (1 + stock.getOneYearReturn())));
        }
        if (!found) {
            IO.println("No index meets the " + strategy.getName() + " criteria right now.");
        }
    }

    /**
     * Shows a prompt and reads one line. Exits cleanly if the input is closed.
     *
     * @param prompt the text to show, or null for none
     * @return the trimmed line
     */
    private static String readLine(String prompt) {
        if (prompt != null) {
            IO.println(prompt);
        }
        String line = IO.readln();
        if (line == null) {
            IO.println("Input closed. Goodbye.");
            System.exit(0);
        }
        return line.trim();
    }

    /**
     * Asks until the user types a number above 0.
     *
     * @param prompt the question to show
     * @return the number
     */
    private static double readPositiveDouble(String prompt) {
        while (true) {
            try {
                double value = Double.parseDouble(readLine(prompt));
                if (value > 0) {
                    return value;
                }
            } catch (NumberFormatException e) {
                // not a number: fall through and ask again
            }
            IO.println("Please enter a number greater than 0.");
        }
    }

    /**
     * Asks until the user picks a valid risk profile.
     *
     * @return User.STABILITY_PROFILE or User.GROWTH_PROFILE
     */
    private static int readRiskProfile() {
        IO.println("""
                Select Risk Profile:
                1. Stability
                2. Growth
                """);
        while (true) {
            String input = readLine(null);
            if (input.equals(String.valueOf(User.STABILITY_PROFILE))) {
                return User.STABILITY_PROFILE;
            }
            if (input.equals(String.valueOf(User.GROWTH_PROFILE))) {
                return User.GROWTH_PROFILE;
            }
            IO.println("Please enter 1 or 2.");
        }
    }
}
