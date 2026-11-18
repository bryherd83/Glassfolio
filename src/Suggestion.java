public class Suggestion {
    //Feel like these might need to be public but not sure yet
    private Stock stock;
    private double score;
    private String reason;

    //Will be used by StockSuggestion class, this is what the best stock would be based on user risk profile and then the stocks beta or dividends
    public Suggestion(Stock stock, double score, String reason) {
        this.stock = stock;
        this.score = score;
        this.reason = reason;
    }
    public Stock getStock() {
        return stock;
    }
    public double getScore() {
        return score;
    }
    public String getReason() {
        return reason;
    }

    public String toString(){
        return "Ticker" + stock.getTicker()
                + "Company: " + stock.getCompanyName()
                + "Price: " + stock.getPrice()
                + "Reason: " + reason;
    }

}
