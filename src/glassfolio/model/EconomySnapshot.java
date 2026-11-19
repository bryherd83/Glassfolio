package glassfolio.model;

/**
 * The latest U.S. economic numbers shown above the suggestions. All values are
 * percents, so 4.33 means 4.33%.
 *
 * @param fedFundsRate the Federal Reserve's interest rate
 * @param inflation    how much consumer prices rose over the last 12 months
 * @param unemployment the share of the labor force without a job
 * @param savingsRate  the average 3-month Treasury bill rate over the past year, a stand-in
 *                     for what safe savings paid
 */
public record EconomySnapshot(double fedFundsRate, double inflation, double unemployment, double savingsRate) {
    private static final double PERCENT = 100;

    /**
     * Checks that every number is a real value.
     *
     * @throws IllegalArgumentException if any value is NaN or infinite
     */
    public EconomySnapshot {
        for (double value : new double[] {fedFundsRate, inflation, unemployment, savingsRate}) {
            if (!Double.isFinite(value)) {
                throw new IllegalArgumentException("Economy numbers must be real values");
            }
        }
    }

    /**
     * Works out what a budget kept in safe savings a year ago would be worth today,
     * so the user can see what taking market risk added or cost.
     *
     * @param budget the amount saved a year ago, in dollars
     * @return the amount today
     * @throws IllegalArgumentException if budget is not a positive number
     */
    public double savingsWorth(double budget) {
        if (!(budget > 0) || !Double.isFinite(budget)) {
            throw new IllegalArgumentException("Budget must be a positive number");
        }
        // shortcut: one year of simple interest at the average rate; fine for a one-year comparison
        return budget * (1 + savingsRate / PERCENT);
    }

    /**
     * Removes inflation from a return, so the user sees what their money can
     * really buy now: (1 + return) / (1 + inflation) - 1.
     *
     * @param nominalPercent the return before inflation, 5.0 means +5%
     * @return the return after inflation, in percent
     * @throws IllegalArgumentException if the return is not a real number
     */
    public double realReturn(double nominalPercent) {
        if (!Double.isFinite(nominalPercent)) {
            throw new IllegalArgumentException("Return must be a real number");
        }
        return ((1 + nominalPercent / PERCENT) / (1 + inflation / PERCENT) - 1) * PERCENT;
    }
}
