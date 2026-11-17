import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

/**
 * Contract for anything that can supply daily values for a market series.
 * FredClient implements it today. A second API, or a test stub, can plug in
 * later without any change to Market (Dependency Inversion).
 */
public interface MarketDataSource {

    /**
     * Gets the daily values of a series from startDate to today.
     *
     * @param seriesId  the series id, for example "SP500"
     * @param startDate the first day to include
     * @return the values, oldest first, with days that have no data left out
     * @throws IOException          if the data can't be downloaded or read
     * @throws InterruptedException if the request is interrupted while waiting
     */
    List<Double> getValues(String seriesId, LocalDate startDate) throws IOException, InterruptedException;
}
