import java.util.List;

/**
 * Checks the model classes (no JavaFX or internet needed). Uses a fake
 * MarketDataSource instead of FRED, so the results are always the same.
 *
 * Run from the project folder (Java 25):
 *   javac -d out/test src/Stock.java src/Market.java src/MarketDataSource.java src/Stratgey.java
 *         src/Stability.java src/Growth.java src/User.java src/FredClient.java src/Suggestion.java src/HistoryCache.java
 *         src/MarketConditions.java src/EconomySnapshot.java test/ModelTest.java
 *   java -cp out/test ModelTest
 */
public class ModelTest {
    private static final double TOLERANCE = 1e-9;
    private static int passed = 0;

    /**
     * Runs every check and exits with code 1 if any fails.
     *
     * @param args not used
     * @throws Exception if the fake data source fails
     */
    public static void main(String[] args) throws Exception {
        User stabilityUser = new User("test", "test", 500, User.STABILITY_PROFILE);
        User growthUser = new User("test", "test", 500, User.GROWTH_PROFILE);

        // Stock.fromPrices math
        Stock steady = Stock.fromPrices("X", "Steady", List.of(100.0, 110.0, 121.0));
        check(near(steady.getOneYearReturn(), 0.21), "1-year return is last/first - 1");
        check(near(steady.getVolatility(), 0), "same daily return every day means 0 volatility");
        Stock choppy = Stock.fromPrices("Y", "Choppy", List.of(100.0, 101.0, 100.0, 101.0));
        check(choppy.getVolatility() > 0.1, "up-down prices give high volatility");

        // Stability: lower volatility scores higher, limit is 18% * 0.9 = 16.2%
        Stability stability = new Stability(Stability.DEFAULT_MAX_VOLATILITY);
        check(stability.score(index("A", 0, 0.10), stabilityUser) > stability.score(index("B", 0, 0.15), stabilityUser),
                "lower volatility scores higher");
        check(stability.score(index("C", 0, 0.17), stabilityUser) == 0, "17% volatility is over the 16.2% limit");

        // Growth: needs at least 10% return
        Growth growth = new Growth(Growth.DEFAULT_MIN_GROWTH);
        check(growth.score(index("D", 0.05, 0), growthUser) == 0, "5% return is under the 10% minimum");
        check(near(growth.score(index("E", 0.20, 0), growthUser), 20), "20% return scores 20");

        // Suggestion.rank: filters zeros, best first, worth uses the budget
        List<Suggestion> ranked = Suggestion.rank(growth, growthUser,
                List.of(index("LOW", 0.12, 0), index("NONE", 0.01, 0), index("HIGH", 0.30, 0)));
        check(ranked.size() == 2, "index under the minimum is left out");
        check(ranked.get(0).stock().getTicker().equals("HIGH"), "best score comes first");
        check(near(ranked.get(0).worth(), 650), "$500 with a 30% return is worth $650");

        // Market loads all 3 indexes through the injected source
        MarketDataSource fake = (seriesId, start, units) -> List.of(100.0, 105.0, 103.0, 110.0);
        check(new Market(fake).loadIndexes().size() == 3, "market loads 3 indexes");

        // MarketConditions: latest values; inflation must be asked for as year-over-year change
        MarketDataSource economy = (seriesId, start, units) -> switch (seriesId) {
            case "FEDFUNDS" -> List.of(5.0, 4.5, 4.25);
            case "UNRATE" -> List.of(4.0, 4.1);
            default -> units.equals(MarketDataSource.PERCENT_CHANGE_FROM_YEAR_AGO)
                    ? List.of(2.7, 2.9) : List.of(320.0, 321.0);
        };
        EconomySnapshot snapshot = new MarketConditions(economy).load();
        check(near(snapshot.fedFundsRate(), 4.25), "uses the latest Fed rate");
        check(near(snapshot.unemployment(), 4.1), "uses the latest unemployment rate");
        check(near(snapshot.inflation(), 2.9), "inflation is the latest year-over-year change");
        MarketDataSource noData = (seriesId, start, units) -> List.of();
        expectRejected(() -> {
            try {
                new MarketConditions(noData).load();
            } catch (java.io.IOException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        }, "a series with no recent data");

        // HistoryCache keeps plain values for the charts, not percent-change requests
        HistoryCache cache = new HistoryCache(economy);
        cache.getValues("CPIAUCSL", java.time.LocalDate.now(), MarketDataSource.PERCENT_CHANGE_FROM_YEAR_AGO);
        check(cache.getPercentChange("CPIAUCSL").isEmpty(), "percent-change data isn't cached for charts");
        cache.getValues("CPIAUCSL", java.time.LocalDate.now());
        check(near(cache.getPercentChange("CPIAUCSL").get(1), 0.3125), "320 -> 321 is +0.3125%");

        // Bad input is rejected
        expectRejected(() -> new User("a", "b", 0, User.STABILITY_PROFILE), "budget of 0");
        expectRejected(() -> new User("a", "b", 100, 3), "risk profile 3");
        expectRejected(() -> new FredClient(" "), "blank API key");
        expectRejected(() -> new Stability(0), "volatility limit of 0");
        expectRejected(() -> Stock.fromPrices("Z", "Z", List.of(1.0, 2.0)), "fewer than 3 prices");
        expectRejected(() -> new Market(null), "missing data source");

        System.out.println("All " + passed + " checks passed");
    }

    private static Stock index(String ticker, double oneYearReturn, double volatility) {
        return new Stock(ticker, ticker, 100, oneYearReturn, volatility);
    }

    private static boolean near(double actual, double expected) {
        return Math.abs(actual - expected) < TOLERANCE;
    }

    private static void check(boolean ok, String what) {
        if (!ok) {
            System.out.println("FAILED: " + what);
            System.exit(1);
        }
        passed++;
    }

    private static void expectRejected(Runnable action, String what) {
        try {
            action.run();
        } catch (IllegalArgumentException | IllegalStateException e) {
            passed++;
            return;
        }
        System.out.println("FAILED: should reject " + what);
        System.exit(1);
    }
}
