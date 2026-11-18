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
    /**
     * One index the app can suggest.
     *
     * @param seriesId the data source's id for the index, for example "SP500"
     * @param name     the name shown to the user, for example "S&P 500"
     */
    private record IndexInfo(String seriesId, String name) { }

    private static final List<IndexInfo> INDEXES = List.of(
            new IndexInfo("SP500", "S&P 500"),
            new IndexInfo("NASDAQCOM", "NASDAQ Composite"),
            new IndexInfo("DJIA", "Dow Jones Industrial Average"));
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
        for (IndexInfo index : INDEXES) {
            List<Double> prices = source.getValues(index.seriesId(), start);
            indexes.add(Stock.fromPrices(index.seriesId(), index.name(), prices));
        }
        return indexes;
    }
}
