package glassfolio.gui;

import javafx.beans.property.ObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.beans.value.WeakChangeListener;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;

/**
 * Builds the app's one themed Scene and each screen's root: puts the backdrop
 * behind a screen's content, applies the shared styles and the current theme's
 * colors, and keeps them in sync when the user switches theme. Screens swap
 * their root into the same Scene, because replacing the Scene ends full screen.
 */
public final class GlassTheme {
    /** Starting width of the window; screens stretch to fill any size. */
    public static final double SCENE_WIDTH = 980;
    /** Starting height of the window; screens stretch to fill any size. */
    public static final double SCENE_HEIGHT = 880; // tall enough for the form, the profile guide and the market strip

    private static final String THEME_LISTENER_KEY = "themeListener";

    private GlassTheme() {
    }

    /**
     * Builds the app's single Scene, styled and kept in sync with the theme.
     *
     * @param theme the app's current theme; the Scene restyles when it changes
     * @return the Scene, with an empty root until a screen is shown
     */
    public static Scene buildScene(ObjectProperty<Theme> theme) {
        Scene scene = new Scene(new StackPane(), SCENE_WIDTH, SCENE_HEIGHT);
        applyTheme(scene, theme);
        return scene;
    }

    /**
     * Builds a screen's root: the blurred backdrop behind the given content.
     * The content fills the window up to its own maximum size and stays centered.
     *
     * @param content the screen's layout
     * @return the root to swap into the Scene
     */
    public static Region buildRoot(Region content) {
        StackPane root = new StackPane(Backdrop.build(), content);
        root.setStyle(Styles.BACKGROUND);
        return root;
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
        Styles.clickable(button, Styles.SECONDARY_BUTTON, Styles.SECONDARY_BUTTON_HOVER);
        button.setOnAction(e -> theme.set(theme.get().other()));
        return button;
    }
}
