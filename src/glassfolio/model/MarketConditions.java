package glassfolio.model;

import glassfolio.data.MarketDataSource;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

/**
 * Loads the current economic conditions (interest rate, inflation,
 * unemployment and the safe savings rate) from a MarketDataSource. FRED is built for this kind of
 * "overall" data, so it complements the index suggestions.
 */
public class MarketConditions {
    private static final String FED_FUNDS = "FEDFUNDS";
    private static final String CONSUMER_PRICES = "CPIAUCSL";
    private static final String UNEMPLOYMENT = "UNRATE";
    private static final String TREASURY_3_MONTH = "DGS3MO"; // what a 3-month Treasury bill pays, as a yearly percent
    private static final int MONTHS_TO_LOAD = 6; // monthly data arrives late, so look a few months back

    private final MarketDataSource source;

    /**
     * @param source where the economic series come from
     * @throws IllegalArgumentException if source is null
     */
    public MarketConditions(MarketDataSource source) {
        if (source == null) {
            throw new IllegalArgumentException("MarketConditions needs a data source");
        }
        this.source = source;
    }

    /**
     * Loads the latest interest rate, yearly inflation and unemployment, and the
     * average 3-month Treasury rate over the past year.
     *
     * @return the snapshot
     * @throws IOException          if the data can't be downloaded or read
     * @throws InterruptedException if a request is interrupted
     * @throws IllegalStateException if a series has no recent data
     */
    public EconomySnapshot load() throws IOException, InterruptedException {
        LocalDate start = LocalDate.now().minusMonths(MONTHS_TO_LOAD);
        double fedFundsRate = latest(source.getValues(FED_FUNDS, start), FED_FUNDS);
        double unemployment = latest(source.getValues(UNEMPLOYMENT, start), UNEMPLOYMENT);
        // FRED works out the year-over-year change by date, so a missing month can't skew it
        double inflation = latest(source.getValues(CONSUMER_PRICES, start,
                MarketDataSource.PERCENT_CHANGE_FROM_YEAR_AGO), CONSUMER_PRICES);
        double savingsRate = average(source.getValues(TREASURY_3_MONTH, LocalDate.now().minusYears(1)),
                TREASURY_3_MONTH);
        return new EconomySnapshot(fedFundsRate, inflation, unemployment, savingsRate);
    }

    /**
     * Averages a series, so a year of rates becomes one comparable number.
     *
     * @param values   the series values
     * @param seriesId the series id, for the error message
     * @return the average
     * @throws IllegalStateException if the series has no data
     */
    private static double average(List<Double> values, String seriesId) {
        return values.stream().mapToDouble(Double::doubleValue).average()
                .orElseThrow(() -> new IllegalStateException("No recent data for " + seriesId));
    }

    /**
     * Picks a series' newest value.
     *
     * @param values   the series values, oldest first
     * @param seriesId the series id, for the error message
     * @return the newest value
     * @throws IllegalStateException if the series has no data
     */
    private static double latest(List<Double> values, String seriesId) {
        if (values.isEmpty()) {
            throw new IllegalStateException("No recent data for " + seriesId);
        }
        return values.get(values.size() - 1);
    }
}
