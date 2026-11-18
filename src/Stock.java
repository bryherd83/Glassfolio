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
    private final double yearHigh;
    private final double yearLow;
    private final double biggestDrop;   // worst fall from a high, 0.12 means -12%

    /**
     * Creates a Stock from values that are already calculated. The year range
     * is just the price and the biggest drop is 0; the tests use this.
     *
     * @param ticker        the series id, for example "SP500"
     * @param name          the display name, for example "S&P 500"
     * @param price         the latest price, above 0
     * @param oneYearReturn the change over the last year, 0.12 means +12%
     * @param volatility    the yearly volatility, 0 or more
     * @throws IllegalArgumentException if a value is missing or out of range
     */
    public Stock(String ticker, String name, double price, double oneYearReturn, double volatility) {
        this(ticker, name, price, oneYearReturn, volatility, price, price, 0);
    }

    /**
     * Creates a Stock with its year range and biggest drop as well.
     *
     * @param ticker        the series id, for example "SP500"
     * @param name          the display name, for example "S&P 500"
     * @param price         the latest price, above 0
     * @param oneYearReturn the change over the last year, 0.12 means +12%
     * @param volatility    the yearly volatility, 0 or more
     * @param yearHigh      the highest price in the year, at least the price
     * @param yearLow       the lowest price in the year, above 0 and at most the price
     * @param biggestDrop   the worst fall from a high, between 0 and 1
     * @throws IllegalArgumentException if a value is missing or out of range
     */
    public Stock(String ticker, String name, double price, double oneYearReturn, double volatility,
                 double yearHigh, double yearLow, double biggestDrop) {
        if (ticker == null || ticker.isBlank() || name == null || name.isBlank()) {
            throw new IllegalArgumentException("Stock needs a ticker and a name");
        }
        if (price <= 0 || volatility < 0) {
            throw new IllegalArgumentException("Price must be above 0 and volatility can't be negative");
        }
        if (yearLow <= 0 || yearLow > price || yearHigh < price || biggestDrop < 0 || biggestDrop > 1) {
            throw new IllegalArgumentException("Year range must contain the price and the drop must be 0 to 1");
        }
        this.ticker = ticker;
        this.name = name;
        this.price = price;
        this.oneYearReturn = oneYearReturn;
        this.volatility = volatility;
        this.yearHigh = yearHigh;
        this.yearLow = yearLow;
        this.biggestDrop = biggestDrop;
    }

    /**
     * Factory method: builds a Stock from daily closing prices by working out
     * its 1-year return, yearly volatility, year range and biggest drop.
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

        double high = first;
        double low = first;
        double peak = first;
        double biggestDrop = 0;
        for (double p : prices) {
            high = Math.max(high, p);
            low = Math.min(low, p);
            peak = Math.max(peak, p);
            biggestDrop = Math.max(biggestDrop, 1 - p / peak);
        }
        return new Stock(ticker, name, last, last / first - 1, volatility, high, low, biggestDrop);
    }

    /** @return the highest price in the year */
    public double getYearHigh(){
        return yearHigh;
    }
    /** @return the lowest price in the year */
    public double getYearLow(){
        return yearLow;
    }
    /** @return the worst fall from a high during the year, 0.12 means -12% */
    public double getBiggestDrop(){
        return biggestDrop;
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
