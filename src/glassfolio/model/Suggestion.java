package glassfolio.model;

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
 * @param math   the strategy's plain-words score math, or why the index scored 0
 * @param worth  what the user's budget, invested a year ago, would be worth today; 0 for a left-out index
 */
public record Suggestion(Stock stock, double score, String reason, String math, double worth) {

    /** @throws IllegalArgumentException if the stock, reason or math is missing, or the score or worth is negative or not a number */
    public Suggestion {
        if (stock == null || reason == null || math == null || !(score >= 0) || !(worth >= 0)) {
            throw new IllegalArgumentException("Suggestion needs a stock, a reason, the score math and a score and worth of 0 or more");
        }
    }

    /**
     * Scores every index with the strategy and keeps the ones worth suggesting.
     *
     * @param strategy the user's strategy
     * @param user     the user being advised
     * @param indexes  the loaded indexes
     * @return the suggestions, best score first; empty if no index passes
     */
    public static List<Suggestion> rank(Strategy strategy, User user, List<Stock> indexes) {
        List<Suggestion> picks = new ArrayList<>();
        for (Stock stock : indexes) {
            double score = strategy.score(stock, user);
            if (score > 0) {
                picks.add(new Suggestion(stock, score, strategy.explain(stock, user),
                        strategy.scoreMath(stock, user),
                        user.getBudget() * (1 + stock.getOneYearReturn())));
            }
        }
        picks.sort(Comparator.comparingDouble(Suggestion::score).reversed());
        return picks;
    }

    /**
     * Lists the indexes the strategy left out, each with the reason why.
     *
     * @param strategy the user's strategy
     * @param user     the user being advised
     * @param indexes  the loaded indexes
     * @return the left-out indexes with a score of 0, in their original order
     */
    public static List<Suggestion> missed(Strategy strategy, User user, List<Stock> indexes) {
        List<Suggestion> missed = new ArrayList<>();
        for (Stock stock : indexes) {
            if (!(strategy.score(stock, user) > 0)) {
                missed.add(new Suggestion(stock, 0, strategy.explainMiss(stock, user),
                        strategy.scoreMath(stock, user), 0));
            }
        }
        return missed;
    }
}
