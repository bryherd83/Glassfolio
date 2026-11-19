package glassfolio.model;

/**
 * Strategy for careful investors: prefers the index with the lowest volatility.
 * One of the concrete strategies in the Strategy pattern (see Strategy).
 */
public class Stability extends Strategy {
    /** Highest yearly volatility allowed before the profile adjustment (18%). */
    public static final double DEFAULT_MAX_VOLATILITY = 0.18;
    /** Users with the Stability profile get a 10% tighter limit. */
    public static final double STABILITY_PROFILE_FACTOR = 0.9;

    private double baseMaxVolatility;

    /**
     * @param baseMaxVolatility the highest yearly volatility allowed, above 0
     * @throws IllegalArgumentException if the limit is 0 or less
     */
    public Stability(double baseMaxVolatility) {
        super("Stability");
        this.baseMaxVolatility = checkLimit(baseMaxVolatility);
    }
    /** @return the highest yearly volatility allowed before adjustment */
    public double getBaseMaxVolatility() {
        return baseMaxVolatility;
    }
    /**
     * @param baseMaxVolatility the new limit, above 0
     * @throws IllegalArgumentException if the limit is 0 or less
     */
    public void setBaseMaxVolatility(double baseMaxVolatility) {
        this.baseMaxVolatility = checkLimit(baseMaxVolatility);
    }

    /** Lower volatility = higher score. Anything over the limit scores 0. */
    @Override
    public double score(Stock stock, User user) {
        double maxVolatility = adjustThreshold(baseMaxVolatility, user);
        if (stock.getVolatility() > maxVolatility) {
            return 0.0;
        }
        return (maxVolatility - stock.getVolatility()) * SCORE_SCALE;
    }

    /** Users with the Stability profile get a tighter limit; everyone else keeps the base. */
    @Override
    public double adjustThreshold(double base, User user) {
        if (user.getRiskProfile() == User.STABILITY_PROFILE) {
            return base * STABILITY_PROFILE_FACTOR;
        }
        return base;
    }

    /** Says how the index's volatility sits inside the limit. */
    @Override
    public String explain(Stock stock, User user) {
        return String.format("Swings about %.1f%% a year, within your %.1f%% limit",
                stock.getVolatility() * SCORE_SCALE,
                adjustThreshold(baseMaxVolatility, user) * SCORE_SCALE);
    }

    /** Says how far the index's volatility is past the limit. */
    @Override
    public String explainMiss(Stock stock, User user) {
        return String.format("Swings about %.1f%% a year, past your %.1f%% limit",
                stock.getVolatility() * SCORE_SCALE, adjustThreshold(baseMaxVolatility, user) * SCORE_SCALE);
    }

    /** States the user's swing limit and what volatility means. */
    @Override
    public String rule(User user) {
        return String.format("Stability picks the steadiest index, as long as its price swings less than %.1f%% a year (your limit)."
                + " Volatility measures how much the price jumps around in a year; lower is calmer.",
                adjustThreshold(baseMaxVolatility, user) * SCORE_SCALE);
    }

    /** Shows the room left under the limit, or why the index swung too much. */
    @Override
    public String scoreMath(Stock stock, User user) {
        double limit = adjustThreshold(baseMaxVolatility, user) * SCORE_SCALE;
        double swing = stock.getVolatility() * SCORE_SCALE;
        if (score(stock, user) == 0) {
            return String.format("It swings %.1f%% a year, past your %.1f%% limit, so it scores 0 and is left out.", swing, limit);
        }
        return String.format("Your limit %.1f%% minus its swing %.1f%% leaves %.1f points of room, so its score is %.1f.",
                limit, swing, limit - swing, score(stock, user));
    }

    private static double checkLimit(double limit) {
        if (limit <= 0) {
            throw new IllegalArgumentException("Volatility limit must be above 0");
        }
        return limit;
    }
}
