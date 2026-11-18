import javafx.application.Application;

/**
 * Starts Glassfolio. Kept apart from StockApp so the app also
 * starts when JavaFX is on the classpath instead of the module path.
 */
public class Main {

    /**
     * Opens the JavaFX window.
     *
     * @param args passed on to JavaFX
     */
    public static void main(String[] args) {
        Application.launch(StockApp.class, args);
    }
}
