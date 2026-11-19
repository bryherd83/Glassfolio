import java.util.ArrayList;
import java.util.List;

public class StockSuggestor {
    private Stratgey stratgey;

    public StockSuggestor(Stratgey stratgey) {
        this.stratgey = stratgey;
    }

    public void setStratgey(Stratgey stratgey) {
        this.stratgey = stratgey;
    }

    public List<Stock> filterByBudget(List<Stock> stocks, User user) {
        List <Stock> filteredStocks = new ArrayList<>();
        for (Stock stock : stocks){
            if(stock.getPrice() <= user.getBudget()){
                filteredStocks.add(stock);
            }
        }
        return filteredStocks;
    }

    public List<Suggestion> rank(List<Stock> stocks, User user) {
        List<Suggestion> suggestions = new ArrayList<>();
        for (Stock stock : stocks){
            double score = stratgey.score(stock, user);
            Suggestion suggestion = new Suggestion(stock, score);
            suggestions.add(suggestion);
        }
        return suggestions;
    }

    public List<Suggestion> generate(User user, Market market){
        List<Stock> allStocks = market.getStocks();
        List<Stock> inBudgetStocks = filterByBudget(allStocks, user);

        return rank(inBudgetStocks, user);
    }
    public Report buildReport(User user, List <Suggestion> suggestions){
        return new Report(user, stratgey.getClass().getSimpleName(), suggestions);

    }

}
