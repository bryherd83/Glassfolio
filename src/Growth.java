/**
 * Strategy for growth investors: prefers the index with the best 1-year return.
 * One of the concrete strategies in the Strategy pattern (see Stratgey).
 */
public class Growth extends Stratgey{

/** Lowest 1-year return an index needs to be suggested (10%). */
public static final double DEFAULT_MIN_GROWTH = 0.10;

private double baseMinGrowth;

/**
 * @param baseMinGrowth the lowest 1-year return allowed, 0.10 means 10%
 */
public Growth(double baseMinGrowth){
    super ("Growth");
    this.baseMinGrowth = baseMinGrowth;
}

/** @return the lowest 1-year return allowed */
public double getBaseMinGrowth(){
    return baseMinGrowth;
}
/** @param baseMinGrowth the new lowest 1-year return allowed */
public void setBaseMinGrowth(double baseMinGrowth){
    this.baseMinGrowth = baseMinGrowth;
}

@Override
public String explain(Stock stock, User user){
    return String.format("%s grew %.1f%% last year, above the %.1f%% minimum",
            stock.getName(), stock.getOneYearReturn() * SCORE_SCALE, baseMinGrowth * SCORE_SCALE);
}

@Override
public String explainMiss(Stock stock, User user){
    return String.format("Grew %.1f%% last year, below the %.1f%% minimum",
            stock.getOneYearReturn() * SCORE_SCALE, baseMinGrowth * SCORE_SCALE);
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
public double adjustThreshold(double base, User user)
{
    return base;
}

}
