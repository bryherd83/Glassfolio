import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * One suggested index with its score and the reason it was picked.
 * Model class behind the results table, so the GUI never ranks indexes itself.
 *
 * @param stock  the suggested index
 * @param score  the strategy's score, above 0
 * @param reason one sentence from the strategy saying why it was picked
 * @param worth  what the user's budget, invested a year ago, would be worth today
 */
public record Suggestion(Stock stock, double score, String reason, double worth) {

    /**
     * Scores every index with the strategy and keeps the ones worth suggesting.
     *
     * @param strategy the user's strategy
     * @param user     the user being advised
     * @param indexes  the loaded indexes
     * @return the suggestions, best score first; empty if no index passes
     */
    public static List<Suggestion> rank(Stratgey strategy, User user, List<Stock> indexes) {
        List<Suggestion> picks = new ArrayList<>();
        for (Stock stock : indexes) {
            double score = strategy.score(stock, user);
            if (score > 0) {
                picks.add(new Suggestion(stock, score, strategy.explain(stock, user),
                        user.getBudget() * (1 + stock.getOneYearReturn())));
            }
        }
        picks.sort(Comparator.comparingDouble(Suggestion::score).reversed());
        return picks;
    }
}
