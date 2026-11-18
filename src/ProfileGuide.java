import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

/**
 * A short, plain-language card under the form that explains the two risk
 * profiles, so the user can pick one without knowing what volatility means.
 * The limits come from the strategy classes, so the text can't drift from the rules.
 */
public final class ProfileGuide {
    private static final double SPACING = 10;
    private static final double LINE_SPACING = 2;
    private static final Insets PADDING = new Insets(20, 30, 20, 30);
    private static final double PERCENT = 100;

    private static final String TITLE_STYLE = "-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: -text;";
    private static final String NAME_STYLE = "-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: -text;";
    private static final String TEXT_STYLE = "-fx-font-size: 12px; -fx-text-fill: -text-2;";
    private static final String NOTE_STYLE = "-fx-font-size: 11px; -fx-text-fill: -text-3;";

    private ProfileGuide() {
    }

    /**
     * Builds the guide card.
     *
     * @return the card
     */
    public static VBox build() {
        Label title = new Label("Which profile fits you?");
        title.setStyle(TITLE_STYLE);

        double stabilityLimit = Stability.DEFAULT_MAX_VOLATILITY * Stability.STABILITY_PROFILE_FACTOR * PERCENT;
        VBox stability = profile("Stability", String.format(
                "For money you'd rather not see swing. Picks the index whose price moved the least "
                        + "over the past year, with yearly volatility under %.1f%%.", stabilityLimit));
        VBox growth = profile("Growth", String.format(
                "For money you can leave alone for years. Picks the index that rose the most over the "
                        + "past year, at least %.0f%%.", Growth.DEFAULT_MIN_GROWTH * PERCENT));

        Label note = wrapped("Based on the past year only. Past results don't guarantee future returns. "
                + "Not financial advice.", NOTE_STYLE);

        VBox card = new VBox(SPACING, title, stability, growth, note);
        card.setPadding(PADDING);
        card.setStyle(Styles.SOLID_CARD);
        return card;
    }

    /**
     * Builds one profile's name and description.
     *
     * @param name        the profile's name
     * @param description what it's for and how it picks
     * @return the block
     */
    private static VBox profile(String name, String description) {
        Label nameLabel = new Label(name);
        nameLabel.setStyle(NAME_STYLE);
        return new VBox(LINE_SPACING, nameLabel, wrapped(description, TEXT_STYLE));
    }

    private static Label wrapped(String text, String style) {
        Label label = new Label(text);
        label.setStyle(style);
        label.setWrapText(true);
        return label;
    }
}
