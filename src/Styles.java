import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Every shared style in one place. Styles only use the named colors from
 * {@link Theme} (like -text or -accent), so they work in both themes.
 */
public final class Styles {
    /**
     * The display typeface for headings: a serif, as the financial press uses to signal trust,
     * while body text stays in the system sans. Falls back to the system font where it isn't installed.
     */
    public static final String DISPLAY_FONT = "-fx-font-family: 'Georgia';";
    /** Style for screen and card titles. */
    public static final String TITLE = DISPLAY_FONT + "-fx-font-size: 24px; -fx-text-fill: -text;";
    /** Style for supporting text. */
    public static final String SUBTITLE = "-fx-font-size: 13px; -fx-text-fill: -text-2;";
    /** Named color for values that went up. */
    public static final String UP = "-up";
    /** Named color for values that went down. */
    public static final String DOWN = "-down";
    /** Solid surface for content (cards, text, charts). */
    public static final String SOLID_CARD = """
            -fx-background-color: -surface; -fx-background-radius: 16;
            -fx-border-color: -surface-border; -fx-border-radius: 16; -fx-border-width: 1;
            """;
    /** Frosted glass for the floating controls: the results bar and the form. */
    public static final String GLASS = """
            -fx-background-color: -glass; -fx-background-radius: 18;
            -fx-border-color: -glass-border; -fx-border-radius: 18; -fx-border-width: 1;
            """;
    /** Style for the main action button. Only one element per screen uses the accent. */
    public static final String PRIMARY_BUTTON = """
            -fx-background-color: -accent; -fx-text-fill: -accent-text; -fx-font-weight: bold;
            -fx-background-radius: 10; -fx-padding: 9 24; -fx-cursor: hand;
            """;
    /** Style for less important buttons, like Back. */
    public static final String SECONDARY_BUTTON = """
            -fx-background-color: -field-bg; -fx-text-fill: -text;
            -fx-border-color: -field-border; -fx-border-radius: 10; -fx-background-radius: 10;
            -fx-padding: 8 18; -fx-cursor: hand;
            """;
    /** Style for error messages. */
    public static final String ERROR = "-fx-text-fill: -error;";
    /** Style for normal status messages. */
    public static final String STATUS = "-fx-text-fill: -text-2;";
    /** Style for a screen's background. */
    public static final String BACKGROUND = "-fx-background-color: -bg;";

    /** Rules inline styles can't reach (inputs, chart parts, scroll pane), as a stylesheet URL. */
    public static final String SHEET_URL;

    private static final String SHEET = """
            .text-field, .combo-box {
                -fx-background-color: -field-bg; -fx-background-radius: 10;
                -fx-border-color: -field-border; -fx-border-radius: 10;
                -fx-text-fill: -text; -fx-prompt-text-fill: -text-3;
                -fx-highlight-fill: -accent; -fx-highlight-text-fill: -accent-text; -fx-padding: 6 10;
            }
            .text-field:focused, .combo-box:focused { -fx-border-color: -accent; }
            .combo-box .list-cell { -fx-text-fill: -text; -fx-background-color: transparent; }
            .combo-box .arrow { -fx-background-color: -text-2; }
            .combo-box .arrow-button { -fx-background-color: transparent; }
            .combo-box-popup .list-view { -fx-background-color: -bg; -fx-border-color: -field-border; }
            .combo-box-popup .list-view .list-cell { -fx-background-color: -bg; -fx-text-fill: -text; }
            .combo-box-popup .list-view .list-cell:hover { -fx-background-color: -rank-bg; }
            .progress-indicator { -fx-progress-color: -accent; }
            .chart-plot-background { -fx-background-color: transparent; }
            .chart-vertical-grid-lines { -fx-stroke: transparent; }
            .chart-horizontal-grid-lines { -fx-stroke: -grid; }
            .chart-alternative-row-fill { -fx-fill: transparent; }
            .chart-vertical-zero-line { -fx-stroke: transparent; }
            .chart-horizontal-zero-line { -fx-stroke: -text-3; }
            .chart-series-line { -fx-stroke-width: 2px; }
            .default-color0.chart-series-line { -fx-stroke: -series-1; }
            .default-color1.chart-series-line { -fx-stroke: -series-2; }
            .default-color2.chart-series-line { -fx-stroke: -series-3; }
            .default-color0.chart-legend-item-symbol { -fx-background-color: -series-1, -surface; }
            .default-color1.chart-legend-item-symbol { -fx-background-color: -series-2, -surface; }
            .default-color2.chart-legend-item-symbol { -fx-background-color: -series-3, -surface; }
            .chart-legend { -fx-background-color: transparent; }
            .chart-legend-item { -fx-text-fill: -text-2; }
            .axis { -fx-tick-label-fill: -text-3; }
            .axis-label { -fx-text-fill: -text-3; }
            .scroll-pane, .scroll-pane > .viewport { -fx-background-color: transparent; }
            .separator .line { -fx-border-color: -surface-border; -fx-border-width: 0 0 0 1; }
            """;

    static {
        SHEET_URL = "data:text/css;base64," + Base64.getEncoder().encodeToString(SHEET.getBytes(StandardCharsets.UTF_8));
    }

    private Styles() {
    }
}
