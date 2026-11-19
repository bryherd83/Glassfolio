package glassfolio.data;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

/**
 * Contract for anything that can supply daily values for a market series.
 * FredClient implements it today. A second API, or a test stub, can plug in
 * later without any change to Market (Dependency Inversion).
 */
public interface MarketDataSource {
    /** Units for the series' plain values. */
    String LEVELS = "lin";
    /** Units for the percent change from the same date a year earlier. */
    String PERCENT_CHANGE_FROM_YEAR_AGO = "pc1";

    /**
     * Gets the values of a series from startDate to today, in the given units.
     *
     * @param seriesId  the series id, for example "SP500"
     * @param startDate the first day to include
     * @param units     LEVELS or PERCENT_CHANGE_FROM_YEAR_AGO
     * @return the values, oldest first, with days that have no data left out
     * @throws IOException          if the data can't be downloaded or read
     * @throws InterruptedException if the request is interrupted while waiting
     */
    List<Double> getValues(String seriesId, LocalDate startDate, String units)
            throws IOException, InterruptedException;

    /**
     * Gets the plain values of a series from startDate to today.
     *
     * @param seriesId  the series id, for example "SP500"
     * @param startDate the first day to include
     * @return the values, oldest first, with days that have no data left out
     * @throws IOException          if the data can't be downloaded or read
     * @throws InterruptedException if the request is interrupted while waiting
     */
    default List<Double> getValues(String seriesId, LocalDate startDate) throws IOException, InterruptedException {
        return getValues(seriesId, startDate, LEVELS);
    }
}
