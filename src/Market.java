import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * The market indexes the app can suggest. Loads their history from a
 * MarketDataSource that is passed in (dependency injection), so FRED can be
 * swapped for another API or a test stub without changing this class.
 */
public class Market {
    /** Each row is {series id, display name}. */
    private static final String[][] INDEXES = {
            {"SP500", "S&P 500"},
            {"NASDAQCOM", "NASDAQ Composite"},
            {"DJIA", "Dow Jones Industrial Average"}
    };
    private static final int YEARS_OF_HISTORY = 1;

    private final MarketDataSource source;

    /**
     * Creates a market that loads its data from the given source.
     *
     * @param source where index values come from
     * @throws IllegalArgumentException if source is null
     */
    public Market(MarketDataSource source) {
        if (source == null) {
            throw new IllegalArgumentException("Market needs a data source");
        }
        this.source = source;
    }

    /**
     * Loads one year of data for every index and calculates its stats.
     *
     * @return one Stock per index, in the order listed in INDEXES
     * @throws IOException          if the data can't be downloaded or read
     * @throws InterruptedException if a request is interrupted
     */
    public List<Stock> loadIndexes() throws IOException, InterruptedException {
        LocalDate start = LocalDate.now().minusYears(YEARS_OF_HISTORY);
        List<Stock> indexes = new ArrayList<>();
        for (String[] index : INDEXES) {
            List<Double> prices = source.getValues(index[0], start);
            indexes.add(Stock.fromPrices(index[0], index[1], prices));
        }
        return indexes;
    }
}
