import java.time.LocalDateTime;
import java.util.List;

public class Report {
    private User user;
    private String strategyName;
    private List<Suggestion> suggestions;
    private String generatedOn;

    public Report(User user, String strategyName, List<Suggestion> suggestions) {
        this.user = user;
        this.strategyName = strategyName;
        this.suggestions = suggestions;
        this.generatedOn = LocalDateTime.now().toString();
    }

    //Actual report that user will read. Should look at making this a JSON file or converting this to a file type (.doc, pdf, etc)
    public String generateSummary(){
        StringBuilder summary = new StringBuilder();
        summary.append("Stock Suggestion Report");

        summary.append("User:")
                .append(user);
        summary.append("Strategy:")
                .append(strategyName);
        summary.append("Date of Suggestion")
                .append(generatedOn);

        for (Suggestion suggestion : suggestions) {
            summary.append(", ");
            summary.append("Ticker:")
                    .append(suggestion.getStock().getTicker());

            summary.append("Company:")
                    .append(suggestion.getStock().getCompanyName());

            summary.append("Price")
                    .append(suggestion.getStock().getPrice());

            summary.append("Score:")
                    .append(suggestion.getScore());

            summary.append("Reason:")
                    .append(suggestion.getReason());


        }
        return summary.toString();
    }

    //This should be used somewhere in the class I think? Maybe used in stockSuggestor class. Not completely sure but having an used method I dont think is a good thing.
    public void display(){
        IO.print(generateSummary());
    }
}
