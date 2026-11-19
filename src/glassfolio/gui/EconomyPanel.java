package glassfolio.gui;

import glassfolio.model.EconomySnapshot;
import java.util.List;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/**
 * "The economy right now" on the start screen: the Fed rate, inflation,
 * unemployment and the safe savings rate in a 2-by-2 grid, each with a plain
 * hint for someone new to investing. It stays out of the layout until the
 * numbers load, so there is no empty gap before then.
 */
final class EconomyPanel {
    /**
     * One number in the grid.
     *
     * @param name  what the number is
     * @param hint  what it means, in plain words
     * @param value the value in percent, 4.33 means 4.33%
     */
    private record Item(String name, String hint, double value) { }

    private static final int COLUMNS = 2;
    private static final double SPACING = 14;
    private static final double GAP = 18;
    private static final double LINE_GAP = 1;
    private static final double FULL_WIDTH_PERCENT = 100;
    private static final String TITLE_STYLE = Styles.SECTION;
    private static final String VALUE_STYLE = Styles.NUMBER;
    private static final String NAME_STYLE = Styles.LABEL;
    private static final String HINT_STYLE = Styles.META;

    private final GridPane grid = new GridPane();
    private final VBox root = new VBox(SPACING);

    /** Builds the empty panel, hidden until {@link #show} fills it. */
    EconomyPanel() {
        Label title = new Label("The economy right now");
        title.setStyle(TITLE_STYLE);
        grid.setHgap(GAP);
        grid.setVgap(GAP / 2);
        for (int column = 0; column < COLUMNS; column++) {
            ColumnConstraints share = new ColumnConstraints();
            share.setPercentWidth(FULL_WIDTH_PERCENT / COLUMNS);
            grid.getColumnConstraints().add(share);
        }
        root.getChildren().setAll(new Separator(), title, grid);
        root.setVisible(false);
        root.managedProperty().bind(root.visibleProperty()); // takes no space while hidden
    }

    /** @return the panel's node, to place in a screen */
    Node node() {
        return root;
    }

    /**
     * Fills the grid with the latest numbers and fades the panel in.
     *
     * @param economy the latest economy numbers
     */
    void show(EconomySnapshot economy) {
        List<Item> items = List.of(
                new Item("Fed interest rate", "the rate banks charge each other", economy.fedFundsRate()),
                new Item("Inflation", "how fast prices rose this year", economy.inflation()),
                new Item("Unemployment", "people looking for work", economy.unemployment()),
                new Item("Safe savings rate", "what a U.S. Treasury bill paid this year", economy.savingsRate()));
        grid.getChildren().clear();
        for (int i = 0; i < items.size(); i++) {
            Item item = items.get(i);
            Label value = new Label();
            value.setStyle(VALUE_STYLE);
            Motion.countUp(value, item.value(), v -> String.format("%.2f%%", v), 0);
            Label name = new Label(item.name());
            name.setStyle(NAME_STYLE);
            Label hint = new Label(item.hint());
            hint.setStyle(HINT_STYLE);
            hint.setWrapText(true);
            hint.setMinHeight(Region.USE_PREF_SIZE);
            grid.add(new VBox(LINE_GAP, value, name, hint), i % COLUMNS, i / COLUMNS);
        }
        Styles.keepWholeText(grid);
        root.setVisible(true);
        Motion.fadeInStaggered(root, 0);
    }
}
