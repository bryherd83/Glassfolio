package glassfolio.model;

/**
 * Strategy for growth investors: prefers the index with the best 1-year return.
 * One of the concrete strategies in the Strategy pattern (see Strategy).
 */
public class Growth extends Strategy {

    /** Lowest 1-year return an index needs to be suggested (10%). */
    public static final double DEFAULT_MIN_GROWTH = 0.10;

    private double baseMinGrowth;

    /**
     * @param baseMinGrowth the lowest 1-year return allowed, 0.10 means 10%
     */
    public Growth(double baseMinGrowth){
        super("Growth");
        this.baseMinGrowth = checkLimit(baseMinGrowth);
    }

    /** @return the lowest 1-year return allowed */
    public double getBaseMinGrowth(){
        return baseMinGrowth;
    }
    /**
     * @param baseMinGrowth the new lowest 1-year return allowed, 0 or more
     * @throws IllegalArgumentException if it is negative or not a real number
     */
    public void setBaseMinGrowth(double baseMinGrowth){
        this.baseMinGrowth = checkLimit(baseMinGrowth);
    }

    private static double checkLimit(double limit){
        if (!Double.isFinite(limit) || limit < 0) {
            throw new IllegalArgumentException("Growth goal must be 0 or more");
        }
        return limit;
    }

    /** Says how the index's return clears the goal. */
    @Override
    public String explain(Stock stock, User user){
        return String.format("Grew %.1f%% last year, past your %.1f%% goal",
                stock.getOneYearReturn() * SCORE_SCALE, baseMinGrowth * SCORE_SCALE);
    }

    /** Says how far the index's return is short of the goal. */
    @Override
    public String explainMiss(Stock stock, User user){
        return String.format("Grew %.1f%% last year, short of your %.1f%% goal",
                stock.getOneYearReturn() * SCORE_SCALE, baseMinGrowth * SCORE_SCALE);
    }

    /** States the goal and what a 1-year return means. */
    @Override
    public String rule(User user){
        return String.format("Growth picks the index that rose the most over the past year, as long as it rose at least %.1f%%."
                + " The 1-year return is how much the price changed over the last 12 months.", baseMinGrowth * SCORE_SCALE);
    }

    /** Shows the score as the percent the index rose, or why it fell under the goal. */
    @Override
    public String scoreMath(Stock stock, User user){
        double rose = stock.getOneYearReturn() * SCORE_SCALE;
        if (score(stock, user) == 0) {
            return String.format("It rose %.1f%%, under the %.1f%% goal, so it scores 0 and is left out.",
                    rose, baseMinGrowth * SCORE_SCALE);
        }
        return String.format("It rose %.1f%%, so its score is %.1f: the score is the percent it rose.",
                rose, score(stock, user));
    }

    /** Higher 1-year return = higher score. Anything under the minimum scores 0. */
    @Override
    public double score(Stock stock, User user){
        if(stock.getOneYearReturn() < baseMinGrowth){
            return 0.0;
        }
        return stock.getOneYearReturn() * SCORE_SCALE;
    }

    /** Growth uses the same limit for every user. */
    @Override
    public double adjustThreshold(double base, User user) {
        return base;
    }
}
