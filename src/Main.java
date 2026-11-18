import javafx.application.Application;

/**
 * Starts Glassfolio. Kept apart from StockApp so the app also
 * starts when JavaFX is on the classpath instead of the module path.
 */
public class Main {

        IO.println("What is your Budget?");
        double budget = Double.parseDouble(IO.readln());

        user.setBudget(budget);

        IO.println("""
                Select Risk Profile:
                1. Stability
                2. Growth
                """);
        int choice = Integer.parseInt(IO.readln());
        Stratgey stratgey;

        if(choice == 1) {
            user.setRiskProfile(1);
            stratgey = new Stability(1.2, 0.03);
        } else {
            user.setRiskProfile(2);
            stratgey = new Growth(0.15);
        }
        Suggestion suggestor = new suggestion
        }
    /**
     * Opens the JavaFX window.
     *
     * @param args passed on to JavaFX
     */
    public static void main(String[] args) {
        Application.launch(StockApp.class, args);
    }
}
