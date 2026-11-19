package glassfolio.gui;

import glassfolio.data.HistoryCache;
import glassfolio.model.EconomySnapshot;
import glassfolio.model.Stock;
import glassfolio.model.Suggestion;
import java.util.List;
import java.util.Objects;

/**
 * Everything the results screen shows, in one parameter object so the screen's
 * constructor stays short.
 *
 * @param strategyName the user's strategy, for example "Growth"
 * @param rule         the strategy's rule in plain words, for the "Why this pick?" panels
 * @param picks        the ranked suggestions, best first
 * @param missed       the indexes left out, each with the reason why
 * @param indexes      every loaded index, for the chart
 * @param history      where the chart and trend lines read each index's past year
 * @param economy      the current economy numbers, or null to leave that strip out
 * @param budget       the user's budget in dollars, for the savings comparison
 * @throws NullPointerException     if anything but economy is null
 * @throws IllegalArgumentException if budget is not positive
 */
public record ResultsData(String strategyName, String rule, List<Suggestion> picks, List<Suggestion> missed,
                          List<Stock> indexes, HistoryCache history, EconomySnapshot economy, double budget) {
    /** Checks the inputs and keeps unchangeable copies of the lists. */
    public ResultsData {
        if (!(budget > 0) || !Double.isFinite(budget)) {
            throw new IllegalArgumentException("Budget must be positive");
        }
        Objects.requireNonNull(strategyName, "strategyName");
        Objects.requireNonNull(rule, "rule");
        Objects.requireNonNull(history, "history");
        picks = List.copyOf(picks);
        missed = List.copyOf(missed);
        indexes = List.copyOf(indexes);
    }
}
