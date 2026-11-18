/**
 * The latest U.S. economic numbers shown above the suggestions. All values are
 * percents, so 4.33 means 4.33%.
 *
 * @param fedFundsRate the Federal Reserve's interest rate
 * @param inflation    how much consumer prices rose over the last 12 months
 * @param unemployment the share of the labor force without a job
 */
public record EconomySnapshot(double fedFundsRate, double inflation, double unemployment) { }
