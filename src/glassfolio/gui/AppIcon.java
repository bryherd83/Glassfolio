package glassfolio.gui;

import java.util.ArrayList;
import java.util.List;
import javafx.geometry.Pos;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;

/**
 * Glassfolio's symbol: three rising rounded bars in the chart's three index
 * colors, the same shape the startup animation builds. It is drawn in code, so
 * there is no image file to ship, and it is used both as the window and
 * taskbar icon and as the mark beside the wordmark.
 */
final class AppIcon {
    // share of the icon's height, shortest first: the startup animation's 38, 62 and 92 pixel bars
    private static final List<Double> HEIGHTS = List.of(38.0 / 92, 62.0 / 92, 1.0);
    // the -series-1/2/3 colors from Theme; a window icon is drawn before any stylesheet applies
    private static final List<Color> COLORS = List.of(Color.web("#7c3aed"), Color.web("#ec4899"), Color.web("#0d9488"));
    private static final List<String> COLOR_NAMES = List.of("-series-1", "-series-2", "-series-3");
    private static final List<Integer> ICON_SIZES = List.of(16, 32, 48, 64, 128, 256);
    private static final double BAR_SHARE = 0.26; // each bar's width as a share of the icon; crisp even at 16px
    private static final double GAP_SHARE = 0.08; // the gap between bars
    private static final double MARK_HEIGHT = 22;  // the mark beside the wordmark, in pixels
    private static final double MARK_BAR_WIDTH = 5;
    private static final double MARK_GAP = 2;
    private static final double MARK_BAR_RADIUS = 3;

    private AppIcon() {
    }

    /**
     * Draws the icon at the sizes Windows asks for (title bar, taskbar, Alt+Tab).
     *
     * @return the icon images, smallest first
     */
    static List<Image> images() {
        List<Image> images = new ArrayList<>();
        for (int size : ICON_SIZES) {
            images.add(draw(size));
        }
        return images;
    }

    /**
     * Draws the three bars on a transparent square.
     *
     * @param size the square's side in pixels
     * @return the picture
     */
    private static Image draw(int size) {
        Canvas canvas = new Canvas(size, size);
        GraphicsContext pen = canvas.getGraphicsContext2D();
        double barWidth = size * BAR_SHARE;
        double gap = size * GAP_SHARE;
        double left = (size - (HEIGHTS.size() * barWidth + (HEIGHTS.size() - 1) * gap)) / 2;
        for (int i = 0; i < HEIGHTS.size(); i++) {
            double height = size * HEIGHTS.get(i);
            pen.setFill(COLORS.get(i));
            pen.fillRoundRect(left + i * (barWidth + gap), size - height, barWidth, height, barWidth, barWidth);
        }
        SnapshotParameters transparent = new SnapshotParameters();
        transparent.setFill(Color.TRANSPARENT);
        return canvas.snapshot(transparent, null);
    }

    /**
     * Builds the small version that sits beside the "Glassfolio" wordmark. It
     * uses the theme's named colors, so it follows the theme like everything else.
     *
     * @return the mark
     */
    static HBox mark() {
        HBox bars = new HBox(MARK_GAP);
        bars.setAlignment(Pos.BOTTOM_LEFT);
        for (int i = 0; i < HEIGHTS.size(); i++) {
            Region bar = new Region();
            bar.setStyle("-fx-background-color: " + COLOR_NAMES.get(i) + "; -fx-background-radius: " + MARK_BAR_RADIUS + ";");
            double height = MARK_HEIGHT * HEIGHTS.get(i);
            bar.setMinSize(MARK_BAR_WIDTH, height);
            bar.setMaxSize(MARK_BAR_WIDTH, height);
            bars.getChildren().add(bar);
        }
        bars.setMinHeight(MARK_HEIGHT);
        return bars;
    }
}
