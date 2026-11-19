import glassfolio.data.ApiKeyStore;
import glassfolio.data.FredClient;
import glassfolio.data.HistoryCache;
import glassfolio.data.MarketDataSource;
import glassfolio.data.VisitStore;
import glassfolio.gui.Chime;
import glassfolio.model.EconomySnapshot;
import glassfolio.model.Growth;
import glassfolio.model.Market;
import glassfolio.model.MarketConditions;
import glassfolio.model.Period;
import glassfolio.model.RiskQuiz;
import glassfolio.model.Stability;
import glassfolio.model.Stock;
import glassfolio.model.Suggestion;
import glassfolio.model.User;
import java.util.List;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

/**
 * Checks the model classes (no internet needed). Uses a fake
 * MarketDataSource instead of FRED, so the results are always the same.
 *
 * Run from the project folder (Java 25):
 *   javac -d out/test --module-path <javafx-sdk>/lib --add-modules javafx.controls -sourcepath src test/ModelTest.java
 *   java --module-path <javafx-sdk>/lib --add-modules javafx.controls -cp out/test ModelTest
 */
public class ModelTest {
    private static final double TOLERANCE = 1e-9;
    private static final String FAKE_KEY = "abcdef0123456789abcdef0123456789"; // right format, not a real key
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

        // scoreMath and rule spell out the numbers
        check(growth.scoreMath(index("E", 0.20, 0), growthUser).contains("rose 20.0%, so its score is 20.0"), "Growth math for a pick");
        check(growth.scoreMath(index("D", 0.05, 0), growthUser).contains("rose 5.0%, under the 10.0% goal"), "Growth math for a miss");
        check(growth.rule(growthUser).contains("at least 10.0%"), "Growth rule names the goal");
        check(stability.scoreMath(index("A", 0, 0.10), stabilityUser).contains("limit 16.2% minus its swing 10.0% leaves 6.2 points"),
                "Stability math for a pick");
        check(stability.scoreMath(index("C", 0, 0.17), stabilityUser).contains("swings 17.0% a year, past your 16.2% limit"),
                "Stability math for a miss");
        check(stability.rule(stabilityUser).contains("less than 16.2% a year"), "Stability rule names the limit");

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
            case "DGS3MO" -> List.of(5.0, 4.0);
            default -> units.equals(MarketDataSource.PERCENT_CHANGE_FROM_YEAR_AGO)
                    ? List.of(2.7, 2.9) : List.of(320.0, 321.0);
        };
        EconomySnapshot snapshot = new MarketConditions(economy).load();
        check(near(snapshot.fedFundsRate(), 4.25), "uses the latest Fed rate");
        check(near(snapshot.unemployment(), 4.1), "uses the latest unemployment rate");
        check(near(snapshot.inflation(), 2.9), "inflation is the latest year-over-year change");
        check(near(snapshot.savingsRate(), 4.5), "savings rate is the past year's average 3-month Treasury rate");
        check(near(snapshot.savingsWorth(1000), 1045), "$1,000 at 4.5% for a year is $1,045");
        expectRejected(() -> snapshot.savingsWorth(0), "savings comparison with a budget of 0");
        expectRejected(() -> snapshot.savingsWorth(Double.NaN), "savings comparison with a NaN budget");
        expectRejected(() -> new EconomySnapshot(4, 3, Double.NaN, 4), "economy number that is NaN");
        expectRejected(() -> new Growth(-0.1), "negative growth goal");
        expectRejected(() -> new Suggestion(null, 1, "r", "m", 0), "a suggestion with no stock");
        check(Period.YEAR.window(List.of(-100.0, 5.0)).isEmpty(), "a window that starts at -100% gives no points");
        expectRejected(() -> new Growth(Double.NaN), "growth goal that is NaN");
        expectRejected(() -> Stock.fromPrices("X", "X", List.of(100.0, 0.0, 110.0)), "a price of 0");
        expectRejected(() -> Stock.fromPrices("X", "X", List.of(100.0, -5.0, 110.0)), "a negative price");
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

        // Year range and biggest drop: 100 -> 120 -> 90 -> 110 peaks at 120, falls to 90 (-25%)
        Stock swing = Stock.fromPrices("W", "Swing", List.of(100.0, 120.0, 90.0, 110.0));
        check(near(swing.getYearHigh(), 120) && near(swing.getYearLow(), 90), "year range is the high and the low");
        check(near(swing.getBiggestDrop(), 0.25), "biggest drop is 120 -> 90, 25%");
        check(near(steady.getBiggestDrop(), 0), "a series that only rises never drops");

        // Left-out indexes come with the strategy's reason
        List<Suggestion> missed = Suggestion.missed(growth, growthUser,
                List.of(index("LOW", 0.12, 0), index("NONE", 0.01, 0)));
        check(missed.size() == 1 && missed.get(0).stock().getTicker().equals("NONE"), "only the index under the minimum is missed");
        check(missed.get(0).reason().contains("short of your 10.0% goal"), "growth explains the miss");
        check(stability.explainMiss(index("C", 0, 0.17), stabilityUser).contains("past your 16.2% limit"),
                "stability explains the miss");

        // The chime is real sound and never clips
        byte[] chime = Chime.synthesize();
        int loudest = 0;
        for (int i = 0; i + 1 < chime.length; i += 2) {
            loudest = Math.max(loudest, Math.abs((short) ((chime[i] & 0xff) | (chime[i + 1] << 8))));
        }
        check(loudest > 1000, "the chime is audible");
        check(loudest < Short.MAX_VALUE, "the chime stays under full scale");

        // Period: a window re-measures change from its own first day
        List<Double> yearChange = List.of(0.0, 10.0, 21.0, 10.0, 0.0, 10.0, 21.0);
        List<Double> week = Period.WEEK.window(yearChange);
        check(week.size() == 6, "a week is the last 5 trading days plus its starting day");
        check(near(week.get(0), 0) && near(week.get(5), 10), "the week starts at 0% and 110 -> 121 is +10%");
        check(near(Period.YEAR.window(yearChange).get(6), 21), "the year keeps the change since a year ago");
        check(Period.MONTH.window(List.of()).isEmpty(), "no data gives an empty window");
        expectRejected(() -> Period.WEEK.window(null), "a missing list for a period");

        // RiskQuiz: careful answers pick Stability, daring ones Growth, unsure lands on Stability
        expectRejected(() -> glassfolio.gui.ParticleChart.monthlyValues(null), "missing chart values");
        check(glassfolio.gui.ParticleChart.monthlyValues(java.util.Collections.nCopies(252, 1.0)).length == 12, "a year of values makes 12 points");
        expectRejected(() -> RiskQuiz.profileFor(null), "a null quiz");
        expectRejected(() -> RiskQuiz.profileFor(new int[] {0, -1, 0}), "a negative quiz answer");
        check(RiskQuiz.profileFor(new int[] {0, 0, 0}) == User.STABILITY_PROFILE, "careful answers pick Stability");
        check(RiskQuiz.profileFor(new int[] {2, 2, 2}) == User.GROWTH_PROFILE, "daring answers pick Growth");
        check(RiskQuiz.profileFor(new int[] {1, 1, 1}) == User.STABILITY_PROFILE, "middle answers stay on Stability");
        check(RiskQuiz.profileFor(new int[] {2, 1, 1}) == User.GROWTH_PROFILE, "a total of 4 picks Growth");
        expectRejected(() -> RiskQuiz.profileFor(new int[] {0, 0}), "a quiz with a missing answer");
        expectRejected(() -> RiskQuiz.profileFor(new int[] {0, 3, 0}), "a quiz answer that isn't a choice");

        // VisitStore: save, load, ignore hand-edited values, forget
        Preferences visitNode = Preferences.userRoot().node("glassfolio-visit-test");
        try {
            VisitStore visits = new VisitStore(visitNode);
            visits.forget();
            check(visits.load() == null, "no visit saved means none loaded");
            VisitStore.Visit visit = new VisitStore.Visit("Alex", User.GROWTH_PROFILE, "SP500", "S&P 500", 7000,
                    java.time.LocalDate.of(2026, 11, 18));
            visits.save(visit);
            check(visit.equals(visits.load()), "a saved visit loads back the same");
            visitNode.putDouble("visitPrice", -5);
            check(visits.load() == null, "a hand-edited price is ignored");
            visits.forget();
            check(visits.load() == null, "forget removes the visit");
            expectRejected(() -> new VisitStore.Visit(" ", 1, "SP500", "S&P 500", 1, java.time.LocalDate.now()),
                    "a visit without a name");
            String longest = "a".repeat(User.MAX_NAME_LENGTH);
            check(new VisitStore.Visit(longest, User.STABILITY_PROFILE, "SP500", "S&P 500", 1,
                    java.time.LocalDate.now()).name().equals(longest), "a visit keeps every name a User accepts");
            expectRejected(() -> new VisitStore.Visit("a", User.GROWTH_PROFILE + 1, "SP500", "S&P 500", 1,
                    java.time.LocalDate.now()), "a visit with a profile User doesn't have");
        } finally {
            visitNode.removeNode();
        }

        // Worst case and real return: the money behind "biggest drop" and "after inflation"
        Stock dipped = new Stock("W", "W", 100, 0.1, 0.1, 110, 80, 0.14);
        check(near(dipped.worstCaseValue(1000), 860), "a 14% drop takes $1,000 to $860");
        expectRejected(() -> dipped.worstCaseValue(0), "a worst case for $0");
        EconomySnapshot inflation = new EconomySnapshot(4, 3, 4, 4);
        check(near(inflation.realReturn(3), 0), "a 3% gain with 3% inflation buys nothing more");
        check(near(inflation.realReturn(10), 110.0 / 103 * 100 - 100), "10% with 3% inflation is about +6.8%");
        expectRejected(() -> inflation.realReturn(Double.NaN), "a real return of NaN");

        // Bad input is rejected
        expectRejected(() -> new User("a", "b", 0, User.STABILITY_PROFILE), "budget of 0");
        expectRejected(() -> new User("a", "b", Double.POSITIVE_INFINITY, User.STABILITY_PROFILE), "infinite budget");
        expectRejected(() -> new User("a", "b", 100, 3), "risk profile 3");
        expectRejected(() -> new FredClient(" "), "blank API key");
        expectRejected(() -> new FredClient(FAKE_KEY + "&x=1"), "API key with extra URL text");

        // ApiKeyStore: format check, and save / find / forget in a throwaway preferences node
        check(ApiKeyStore.isValidFormat(FAKE_KEY), "32 lowercase letters and digits is a valid key");
        check(!ApiKeyStore.isValidFormat(FAKE_KEY.substring(1)), "31 characters is not a key");
        check(!ApiKeyStore.isValidFormat(FAKE_KEY.toUpperCase()), "uppercase is not a key");
        check(ApiKeyStore.clean("  " + FAKE_KEY.toUpperCase() + "\n").equals(FAKE_KEY), "pasted key is trimmed and lowercased");
        Preferences testNode = Preferences.userRoot().node("glassfolio-test");
        try {
            ApiKeyStore store = new ApiKeyStore(testNode);
            store.forget();
            check(store.find(null) == null, "no saved key and no variable finds nothing");
            check(FAKE_KEY.equals(store.find(FAKE_KEY)), "environment variable is used when nothing is saved");
            String saved = "b".repeat(32);
            store.save(saved);
            check(saved.equals(store.find(FAKE_KEY)), "saved key wins over the environment variable");
            testNode.put("fredApiKey", "not a key");
            check(store.find(null) == null, "a hand-edited saved value is ignored");
            store.forget();
            check(store.find(null) == null, "forget removes the saved key");
            expectRejected(() -> {
                try {
                    store.save("bad");
                } catch (BackingStoreException e) {
                    throw new IllegalStateException(e);
                }
            }, "saving a badly formed key");
        } finally {
            testNode.removeNode();
        }
        expectRejected(() -> new Stability(0), "volatility limit of 0");
        expectRejected(() -> Stock.fromPrices("Z", "Z", List.of(1.0, 2.0)), "fewer than 3 prices");
        expectRejected(() -> new Market(null), "missing data source");
        expectRejected(() -> new Stock("X", "X", 100, 0, 0, 90, 80, 0), "year high below the price");
        expectRejected(() -> new Stock("X", "X", 100, 0, 0, 110, 80, 1.5), "drop over 100%");

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
