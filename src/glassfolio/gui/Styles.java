package glassfolio.gui;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Labeled;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Font;

/**
 * Every shared style in one place. Styles only use the named colors from
 * {@link Theme} (like -text or -accent), so they work in both themes.
 */
public final class Styles {
    /**
     * Poppins is the one typeface: Regular for body text, Medium for big numbers,
     * SemiBold for headings, labels and buttons. Each weight file registers as its
     * own font family, so styles name them directly.
     */
    public static final String HEADING = "-fx-font-family: 'Poppins SemiBold';";
    /** Medium for big numbers: they read first without shouting. */
    public static final String DISPLAY = "-fx-font-family: 'Poppins Medium';";
    // The type scale: most text uses one of these levels, so the hierarchy comes
    // from clear steps in size, weight and color.
    /** Level 1, the one headline per screen, in Poppins SemiBold. */
    public static final String HERO = HEADING + "-fx-font-size: 40px; -fx-text-fill: -text;";
    /** Level 2, stat numbers: index values, economy numbers, scores. */
    public static final String NUMBER = DISPLAY + "-fx-font-size: 24px; -fx-text-fill: -text;";
    /** Level 3, section titles, card names and questions. */
    public static final String SECTION = HEADING + "-fx-font-size: 18px; -fx-text-fill: -text; -fx-line-spacing: 2px;";
    /** Level 4, labels: field names, row names, panel titles. */
    public static final String LABEL = HEADING + "-fx-font-size: 13px; -fx-text-fill: -text;";
    /** Level 5, reading text: blurbs, reasons, hints. */
    public static final String BODY = "-fx-font-size: 14px; -fx-text-fill: -text-2; -fx-line-spacing: 5px;";
    /** Level 6, small print: dates, steps, notes. */
    public static final String META = "-fx-font-size: 12px; -fx-text-fill: -text-3; -fx-line-spacing: 3px;";
    /** Style for screen and card titles. */
    public static final String TITLE = HEADING + "-fx-font-size: 24px; -fx-text-fill: -text;";
    private static final double WORDMARK_SPACING = 10;
    /** Corner radius of the big frame each screen sits in. */
    public static final double FRAME_RADIUS = 30;
    /** The big rounded frame each screen sits in, so every screen reads as one object. */
    public static final String FRAME = "-fx-background-color: -surface; -fx-background-radius: " + FRAME_RADIUS
            + "; -fx-border-color: -glass-border; -fx-border-radius: " + FRAME_RADIUS + "; -fx-border-width: 1;";
    /** The frame's soft shadow, drawn on a plate behind it (see {@link #shadowed}). */
    public static final String FRAME_SHADOW = "dropshadow(gaussian, rgba(15, 23, 42, 0.22), 60, 0, 0, 28)";
    /** A tinted callout box with a glowing edge, for text that should stand out. */
    public static final String TINT_BOX = "-fx-background-color: linear-gradient(to right, -tint-1, -tint-2);"
            + " -fx-background-radius: 16; -fx-border-color: -edge; -fx-border-radius: 16; -fx-padding: 10 14 10 14;";
    /** A card that groups related controls, with a soft glow. */
    public static final String GROUP_CARD = "-fx-background-color: -surface; -fx-background-radius: 22;"
            + " -fx-border-color: -edge; -fx-border-radius: 22; -fx-padding: 16 20 18 20;"
            + " -fx-effect: dropshadow(gaussian, -glow, 24, 0, 0, 8);";
    /** The wordmark in its own white pill, so the app name reads apart from the page title. */
    public static final String BRAND_PILL = "-fx-background-color: -surface; -fx-background-radius: 999;"
            + " -fx-border-color: -edge; -fx-border-radius: 999; -fx-padding: 6 16 6 12;"
            + " -fx-effect: dropshadow(gaussian, -glow, 12, 0, 0, 3);";
    /** Small caption over a number: bold, small and grey; callers pass the text in capitals. */
    public static final String CAPS = HEADING + "-fx-font-size: 11px; -fx-text-fill: -text-3;";
    /** A small accent title for a callout box. */
    public static final String BOX_TITLE = HEADING + "-fx-font-size: 12px; -fx-text-fill: -accent;";
    /** Text inside a tinted box. */
    public static final String BOX_TEXT = "-fx-font-size: 13px; -fx-text-fill: -rank-text; -fx-line-spacing: 3px;";
    /** Named color for values that went up. */
    public static final String UP = "-up";
    /** Named color for values that went down. */
    public static final String DOWN = "-down";
    /** Solid surface for content (cards, text, charts). */
    public static final String SOLID_CARD = """
            -fx-background-color: -surface; -fx-background-radius: 16;
            -fx-border-color: -surface-border; -fx-border-radius: 16; -fx-border-width: 1;
            -fx-effect: dropshadow(gaussian, -glow, 20, 0, 0, 6);
            """;
    /**
     * Style for the main action button: a solid pill with a darker bottom edge, so it
     * looks raised and pressable. Only one element per screen uses the accent.
     */
    public static final String PRIMARY_BUTTON = """
            -fx-background-color: derive(-accent, -30%), -accent; -fx-background-insets: 0, 0 0 3 0;
            -fx-background-radius: 999; -fx-text-fill: -accent-text; -fx-font-family: 'Poppins SemiBold';
            -fx-padding: 12 30 15 30; -fx-font-size: 15px; -fx-cursor: hand;
            """;
    /** The main button under the mouse: a brighter accent and a deeper bottom edge. */
    public static final String PRIMARY_BUTTON_HOVER = """
            -fx-background-color: derive(-accent, -38%), derive(-accent, 14%); -fx-background-insets: 0, 0 0 4 0;
            -fx-background-radius: 999; -fx-text-fill: -accent-text; -fx-font-family: 'Poppins SemiBold';
            -fx-padding: 12 30 15 30; -fx-font-size: 15px; -fx-cursor: hand;
            """;
    /** Style for less important buttons, like Back: a raised pill on the surface color. */
    public static final String SECONDARY_BUTTON = """
            -fx-background-color: -field-border, -surface; -fx-background-insets: 0, 1 1 3 1;
            -fx-background-radius: 999; -fx-text-fill: -text;
            -fx-padding: 9 22 12 22; -fx-cursor: hand;
            """;
    /** A pill-shaped well that holds other controls, like a field and its button. */
    public static final String WELL = """
            -fx-background-color: -field-border, -field-bg; -fx-background-insets: 0, 1;
            -fx-background-radius: 999; -fx-padding: 4;
            """;
    /** The same well while the control inside has keyboard focus. */
    public static final String WELL_FOCUSED = """
            -fx-background-color: -accent, -field-bg; -fx-background-insets: 0, 1.5;
            -fx-background-radius: 999; -fx-padding: 4;
            """;
    /** A text field that sits inside a well, so it draws no box of its own. */
    public static final String BARE_FIELD = """
            -fx-background-color: transparent; -fx-border-color: transparent; -fx-padding: 6 6 6 14;
            """;
    /** A choice in a segmented well that isn't picked. */
    public static final String SEGMENT = """
            -fx-background-color: transparent; -fx-text-fill: -text-2;
            -fx-background-radius: 999; -fx-padding: 7 18; -fx-cursor: hand;
            """;
    /** The picked choice: a raised accent pill that fades toward the second chart color, with a glow. */
    public static final String SEGMENT_SELECTED = """
            -fx-background-color: derive(-accent, -30%), linear-gradient(to right, -accent, -series-2);
            -fx-background-insets: 0, 0 0 2 0; -fx-effect: dropshadow(gaussian, -glow, 14, 0, 0, 4);
            -fx-background-radius: 999; -fx-text-fill: -accent-text; -fx-font-family: 'Poppins SemiBold';
            -fx-padding: 7 18; -fx-cursor: hand;
            """;
    /**
     * Puts a soft shadow behind a panel without touching the panel itself. JavaFX
     * draws text inside a node that has an effect into a separate image, which
     * blurs it, so the shadow goes on a plain plate behind the panel instead.
     *
     * @param panel  the panel to lift off the background
     * @param radius the panel's corner radius, so the plate matches its shape
     * @param shadow the CSS dropshadow(...) for the plate
     * @return the plate and panel stacked together, sized like the panel
     */
    public static StackPane shadowed(Region panel, double radius, String shadow) {
        Region plate = new Region();
        // an opaque plate, so it can't show through the panel's slightly see-through surface
        plate.setStyle("-fx-background-color: -bg; -fx-background-radius: " + radius + "; -fx-effect: " + shadow + ";");
        plate.setMouseTransparent(true);
        StackPane stack = new StackPane(plate, panel);
        stack.maxWidthProperty().bind(panel.maxWidthProperty());
        stack.maxHeightProperty().bind(panel.maxHeightProperty());
        return stack;
    }

    /**
     * Builds the Glassfolio wordmark: the three-bar symbol and the name, used on every
     * screen so they read as one app.
     *
     * @return the wordmark
     */
    public static HBox wordmark() {
        HBox mark = AppIcon.mark(); // the same three bars as the window icon
        Label name = new Label("Glassfolio");
        name.setStyle(HEADING + "-fx-font-size: 20px; -fx-text-fill: -text;");
        HBox wordmark = new HBox(WORDMARK_SPACING, mark, name);
        wordmark.setAlignment(Pos.CENTER_LEFT);
        wordmark.setStyle(BRAND_PILL);
        wordmark.setMaxWidth(Region.USE_PREF_SIZE); // the pill hugs the name instead of filling the row
        return wordmark;
    }

    /** A secondary button under the mouse: its outline turns the accent color. */
    public static final String SECONDARY_BUTTON_HOVER = """
            -fx-background-color: -accent, -surface; -fx-background-insets: 0, 1 1 3 1;
            -fx-background-radius: 999; -fx-text-fill: -text;
            -fx-padding: 9 22 12 22; -fx-cursor: hand;
            """;
    /** A text link: the accent color, a hand cursor, underlined only under the mouse (see the sheet). */
    public static final String LINK = "-fx-font-size: 13px; -fx-text-fill: -accent; -fx-padding: 0; -fx-cursor: hand;";

    /**
     * Gives a button its look, a different look under the mouse, and the shared
     * hover rise and press squish, so everything clickable feels clickable.
     *
     * @param button the button
     * @param normal its usual style
     * @param hover  its style under the mouse
     */
    public static void clickable(Button button, String normal, String hover) {
        button.setStyle(normal);
        // a disabled button keeps its normal look, so it never looks clickable while busy
        Runnable restyle = () -> button.setStyle(button.isHover() && !button.isDisabled() ? hover : normal);
        button.hoverProperty().addListener((obs, was, over) -> restyle.run());
        button.disabledProperty().addListener((obs, was, disabled) -> restyle.run());
        Motion.addHoverPop(button);
    }

    /**
     * Makes every label under a node show its whole text: wrapping labels keep
     * their full height, and single-line labels their full width. JavaFX would
     * otherwise shorten them with "..." when space is tight.
     *
     * @param node the screen or part to fix, with everything inside it
     */
    public static void keepWholeText(Node node) {
        if (node instanceof Labeled label) {
            if (label.isWrapText()) {
                label.setMinHeight(Region.USE_PREF_SIZE);
            } else {
                label.setMinWidth(Region.USE_PREF_SIZE);
            }
        }
        if (node instanceof Parent parent) {
            parent.getChildrenUnmodifiable().forEach(Styles::keepWholeText);
        }
    }

    /** Style for error messages. */
    public static final String ERROR = "-fx-text-fill: -error;";
    /** Style for normal status messages. */
    public static final String STATUS = "-fx-text-fill: -text-2;";
    /** Style for a screen's background. */
    public static final String BACKGROUND = "-fx-background-color: -bg;";

    /** Rules inline styles can't reach (inputs, chart parts, scroll pane), as a stylesheet URL. */
    public static final String SHEET_URL;

    private static final String SHEET = """
            .root { -fx-font-family: 'Poppins'; }
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
            .axis { -fx-tick-label-fill: -text-3; -fx-tick-label-font-size: 11px; }
            .axis-label { -fx-text-fill: -text-3; }
            .scroll-pane, .scroll-pane > .viewport { -fx-background-color: transparent; }
            .scroll-pane > .corner { -fx-background-color: transparent; }
            .scroll-bar { -fx-background-color: transparent; -fx-padding: 2; }
            .scroll-bar:vertical { -fx-pref-width: 10; }
            .scroll-bar:horizontal { -fx-pref-height: 10; }
            .scroll-bar .track, .scroll-bar .track-background {
                -fx-background-color: transparent; -fx-border-color: transparent;
            }
            .scroll-bar .thumb { -fx-background-color: -field-border; -fx-background-radius: 999; }
            .scroll-bar .thumb:hover, .scroll-bar .thumb:pressed { -fx-background-color: -text-3; }
            .scroll-bar .increment-button, .scroll-bar .decrement-button {
                -fx-background-color: transparent; -fx-padding: 0; -fx-pref-height: 0; -fx-pref-width: 0;
            }
            .scroll-bar .increment-arrow, .scroll-bar .decrement-arrow { -fx-shape: " "; -fx-padding: 0; }
            .button, .toggle-button {
                -fx-border-color: transparent; -fx-border-width: 2; -fx-border-radius: 999;
            }
            .button:focused, .toggle-button:focused { -fx-border-color: -accent; }
            .button, .toggle-button, .check-box { -fx-font-family: 'Poppins SemiBold'; }
            .check-box { -fx-label-padding: 0 0 0 10; }
            .check-box .box {
                -fx-background-color: -field-bg; -fx-background-radius: 7;
                -fx-border-color: -edge; -fx-border-width: 1.5; -fx-border-radius: 7; -fx-padding: 6;
            }
            .check-box:hover .box { -fx-border-color: -accent; }
            .check-box:selected .box { -fx-background-color: -accent; -fx-border-color: -accent; }
            .check-box:selected .mark { -fx-background-color: -accent-text; }
            .text { -fx-font-smoothing-type: lcd; }
            .hyperlink { -fx-underline: false; -fx-border-width: 0; }
            .hyperlink:hover, .hyperlink:focused { -fx-underline: true; }
            .separator .line { -fx-border-color: -surface-border; -fx-border-width: 0 0 0 1; }
            .separator:horizontal .line { -fx-border-width: 1 0 0 0; }
            """;

    static {
        SHEET_URL = "data:text/css;base64," + Base64.getEncoder().encodeToString(SHEET.getBytes(StandardCharsets.UTF_8));
    }

    private static final String[] FONT_FILES = {
        "Poppins-Regular.ttf", "Poppins-Medium.ttf",
        "Poppins-SemiBold.ttf"
    };
    private static final double FONT_LOAD_SIZE = 12; // any size works; JavaFX registers the whole family
    private static final System.Logger LOG = System.getLogger(Styles.class.getName());

    private Styles() {
    }

    /**
     * Registers the bundled Poppins fonts. Call once before building any screen. If a
     * file is missing the app still works and falls back to the system font.
     */
    public static void loadFonts() {
        for (String file : FONT_FILES) {
            try (InputStream in = Styles.class.getResourceAsStream("/fonts/" + file)) {
                if (in == null || Font.loadFont(in, FONT_LOAD_SIZE) == null) {
                    LOG.log(System.Logger.Level.WARNING, "Font not loaded: " + file);
                }
            } catch (IOException e) {
                LOG.log(System.Logger.Level.WARNING, "Font not loaded: " + file + " (" + e.getClass().getSimpleName() + ")");
            }
        }
    }
}
