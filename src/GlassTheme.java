import javafx.beans.property.ObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.beans.value.WeakChangeListener;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;

/**
 * Builds themed Scenes: puts the backdrop behind a screen's content, applies
 * the shared styles and the current theme's colors, and keeps them in sync when
 * the user switches theme.
 */
public final class GlassTheme {
    /** Starting width of the window; screens stretch to fill any size. */
    public static final double SCENE_WIDTH = 980;
    /** Starting height of the window; screens stretch to fill any size. */
    public static final double SCENE_HEIGHT = 760;

    private static final String THEME_LISTENER_KEY = "themeListener";

    private GlassTheme() {
    }

    /**
     * Builds a Scene with the blurred backdrop behind the given content. The
     * content fills the window up to its own maximum size and stays centered.
     *
     * @param content the screen's layout
     * @param theme   the app's current theme; the Scene restyles when it changes
     * @return the new Scene
     */
    public static Scene buildScene(Region content, ObjectProperty<Theme> theme) {
        StackPane root = new StackPane(Backdrop.build(), content);
        root.setStyle(Styles.BACKGROUND);
        Scene scene = new Scene(root, SCENE_WIDTH, SCENE_HEIGHT);
        applyTheme(scene, theme);
        return scene;
    }

    /**
     * Gives a Scene the shared styles and the current theme's colors, and keeps
     * them in sync when the theme changes. The listener is weak, so screens that
     * were left can still be garbage collected.
     *
     * @param scene the Scene to style
     * @param theme the app's current theme
     */
    public static void applyTheme(Scene scene, ObservableValue<Theme> theme) {
        scene.getStylesheets().setAll(Styles.SHEET_URL, theme.getValue().stylesheet());
        ChangeListener<Theme> listener = (obs, oldTheme, newTheme) ->
                scene.getStylesheets().setAll(Styles.SHEET_URL, newTheme.stylesheet());
        scene.getProperties().put(THEME_LISTENER_KEY, listener); // the Scene keeps the listener alive
        theme.addListener(new WeakChangeListener<>(listener));
    }

    /**
     * Builds the button that switches between the light and dark theme.
     *
     * @param theme the app's current theme
     * @return the button
     */
    public static Button buildThemeSwitch(ObjectProperty<Theme> theme) {
        Button button = new Button("Switch theme");
        button.setStyle(Styles.SECONDARY_BUTTON);
        button.setOnAction(e -> theme.set(theme.get().other()));
        return button;
    }
}
