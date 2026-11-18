/**
 * Strategy for careful investors: prefers the index with the lowest volatility.
 * One of the concrete strategies in the Strategy pattern (see Stratgey).
 */
public class Stability extends Stratgey {
    /** Highest yearly volatility allowed before the profile adjustment (18%). */
    public static final double DEFAULT_MAX_VOLATILITY = 0.18;
    /** Users with the Stability profile get a 10% tighter limit. */
    public static final double STABILITY_PROFILE_FACTOR = 0.9;

    private double baseMaxVolatility;

    /**
     * @param baseMaxVolatility the highest yearly volatility allowed, above 0
     * @throws IllegalArgumentException if the limit is 0 or less
     */
    public Stability (double baseMaxVolatility){
        super("Stability");
        this.baseMaxVolatility = checkLimit(baseMaxVolatility);
    }
    /** @return the highest yearly volatility allowed before adjustment */
    public double getBaseMaxVolatility(){
        return baseMaxVolatility;
    }
    /**
     * @param baseMaxVolatility the new limit, above 0
     * @throws IllegalArgumentException if the limit is 0 or less
     */
    public void setBaseMaxVolatility(double baseMaxVolatility){
        this.baseMaxVolatility = checkLimit(baseMaxVolatility);
    }

    /** Lower volatility = higher score. Anything over the limit scores 0. */
    @Override
    public double score(Stock stock, User user){
        double maxVolatility = adjustThreshold(baseMaxVolatility, user);
        if (stock.getVolatility() > maxVolatility){
            return 0.0;
        }
        return (maxVolatility - stock.getVolatility()) * SCORE_SCALE;
    }

    @Override
    public double adjustThreshold(double base, User user) {
        if (user.getRiskProfile() == User.STABILITY_PROFILE){
            return base * STABILITY_PROFILE_FACTOR;
        }
        return base;
    }

    @Override
    public String explain(Stock stock, User user){
        return String.format("%s has a volatility of %.1f%%, under the %.1f%% limit",
                stock.getName(), stock.getVolatility() * SCORE_SCALE,
                adjustThreshold(baseMaxVolatility, user) * SCORE_SCALE);
    }

    private static double checkLimit(double limit) {
        if (limit <= 0) {
            throw new IllegalArgumentException("Volatility limit must be above 0");
        }
        return limit;
    }
}
