import java.util.List;

/**
 * A market index (like the S&P 500) with stats calculated from its price history.
 * The class keeps the name Stock to match the class diagram.
 */
public class Stock {
    private static final int TRADING_DAYS_PER_YEAR = 252;
    private static final int MIN_PRICES = 3; // need at least 2 daily returns to measure volatility

    private final String ticker;
    private final String name;
    private final double price;
    private final double oneYearReturn; // 0.12 means +12% over the last year
    private final double volatility;    // yearly, 0.15 means 15%

    /**
     * Creates a Stock from values that are already calculated.
     *
     * @param ticker        the series id, for example "SP500"
     * @param name          the display name, for example "S&P 500"
     * @param price         the latest price, above 0
     * @param oneYearReturn the change over the last year, 0.12 means +12%
     * @param volatility    the yearly volatility, 0 or more
     * @throws IllegalArgumentException if a value is missing or out of range
     */
    public Stock(String ticker, String name, double price, double oneYearReturn, double volatility) {
        if (ticker == null || ticker.isBlank() || name == null || name.isBlank()) {
            throw new IllegalArgumentException("Stock needs a ticker and a name");
        }
        if (price <= 0 || volatility < 0) {
            throw new IllegalArgumentException("Price must be above 0 and volatility can't be negative");
        }
        this.ticker = ticker;
        this.name = name;
        this.price = price;
        this.oneYearReturn = oneYearReturn;
        this.volatility = volatility;
    }

    /**
     * Factory method: builds a Stock from daily closing prices by working out
     * its 1-year return and yearly volatility.
     *
     * @param ticker the series id
     * @param name   the display name
     * @param prices daily closing prices, oldest first
     * @return the new Stock
     * @throws IllegalStateException if there are fewer than 3 prices
     */
    public static Stock fromPrices(String ticker, String name, List<Double> prices) {
        if (prices == null || prices.size() < MIN_PRICES) {
            throw new IllegalStateException("Not enough data for " + ticker);
        }
        double first = prices.get(0);
        double last = prices.get(prices.size() - 1);

        double[] dailyReturns = new double[prices.size() - 1];
        double mean = 0;
        for (int i = 1; i < prices.size(); i++) {
            dailyReturns[i - 1] = prices.get(i) / prices.get(i - 1) - 1;
            mean += dailyReturns[i - 1];
        }
        mean /= dailyReturns.length;

        double variance = 0;
        for (double r : dailyReturns) {
            variance += (r - mean) * (r - mean);
        }
        variance /= (dailyReturns.length - 1);

        double volatility = Math.sqrt(variance) * Math.sqrt(TRADING_DAYS_PER_YEAR);
        return new Stock(ticker, name, last, last / first - 1, volatility);
    }

    /** @return the series id, for example "SP500" */
    public String getTicker(){
        return ticker;
    }
    /** @return the display name, for example "S&P 500" */
    public String getName(){
        return name;
    }
    /** @return the latest price */
    public double getPrice(){
        return price;
    }
    /** @return the change over the last year, 0.12 means +12% */
    public double getOneYearReturn(){
        return oneYearReturn;
    }
    /** @return the yearly volatility, 0.15 means 15% */
    public double getVolatility(){
        return volatility;
    }

}
