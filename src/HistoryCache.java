import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Wraps another MarketDataSource and remembers every series it loads, so the
 * GUI can chart an index's past year without asking FRED a second time
 * (Decorator pattern: same interface as the source, one extra behavior).
 */
public class HistoryCache implements MarketDataSource {
    private static final double PERCENT = 100;

    private final MarketDataSource source;
    // Filled on the background loading thread, read on the JavaFX thread
    private final Map<String, List<Double>> history = new ConcurrentHashMap<>();

    /**
     * @param source the real data source to load from
     * @throws IllegalArgumentException if source is null
     */
    public HistoryCache(MarketDataSource source) {
        if (source == null) {
            throw new IllegalArgumentException("HistoryCache needs a data source");
        }
        this.source = source;
    }

    @Override
    public List<Double> getValues(String seriesId, LocalDate startDate, String units)
            throws IOException, InterruptedException {
        List<Double> values = source.getValues(seriesId, startDate, units);
        if (units.equals(LEVELS)) { // the charts only use plain values
            history.put(seriesId, List.copyOf(values));
        }
        return values;
    }

    /**
     * Gives the last loaded values of a series as percent change from its first day.
     *
     * @param seriesId the series id, for example "SP500"
     * @return the changes, oldest first, 5.0 means +5%; empty if the series was never loaded
     */
    public List<Double> getPercentChange(String seriesId) {
        List<Double> values = history.getOrDefault(seriesId, List.of());
        List<Double> changes = new ArrayList<>(values.size());
        if (values.isEmpty()) {
            return changes;
        }
        double first = values.get(0);
        for (double value : values) {
            changes.add((value / first - 1) * PERCENT);
        }
        return changes;
    }
}
