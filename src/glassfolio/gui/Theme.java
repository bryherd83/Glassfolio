package glassfolio.gui;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import javafx.scene.paint.Color;

/**
 * The app's two color themes. Each defines the same named colors (like -text or
 * -accent) that every style in the app refers to, so switching the theme only
 * swaps one small stylesheet. The particle chart's colors live here too because
 * 3D materials can't be styled with CSS.
 */
public enum Theme {
    // AppIcon mirrors the -series-1/2/3 colors below for the window icon; change both together
    /** Light glass over pink-violet blobs; violet accent. */
    LIGHT("""
            -bg: #f6effc;
            -surface: rgba(255,255,255,0.62); -surface-border: #e6dcfb;
            -glass: rgba(255,255,255,0.66); -glass-border: rgba(255,255,255,0.9);
            -text: #0f172a; -text-2: #475569; -text-3: #556274;
            -accent: #7c3aed; -accent-text: white;
            -up: #15803d; -down: #b91c1c; -error: #b91c1c;
            -field-bg: white; -field-border: #d6c8f4;
            -rank-bg: #f3ebff; -rank-text: #4c1d95; -grid: #e8edf5;
            -blob-1: rgba(196,160,255,0.6); -blob-2: rgba(255,170,220,0.6); -blob-3: rgba(160,190,255,0.6);
            -star: transparent; -vignette: rgba(15,23,42,0.10);
            -scene-top: #a98bff; -scene-bottom: #ff9be0; -scene-text: #2e1065;
            -series-1: #7c3aed; -series-2: #ec4899; -series-3: #0d9488;
            -tint-1: rgba(169,139,255,0.22); -tint-2: rgba(255,155,224,0.18);
            -glow: rgba(124,58,237,0.14); -edge: rgba(124,58,237,0.25);
            """, Color.web("#ffffff"), Color.web("#ffe3f7"), false),

    /** Dark teal and navy; lime accent. */
    DARK("""
            -bg: #04100f;
            -surface: rgba(10,28,30,0.82); -surface-border: rgba(255,255,255,0.10);
            -glass: rgba(18,24,38,0.78); -glass-border: rgba(255,255,255,0.12);
            -text: #f1f5f9; -text-2: #d3dae6; -text-3: #aab4c6;
            -accent: #b8f23d; -accent-text: #0b1a10;
            -up: #4ade80; -down: #f87171; -error: #fca5a5;
            -field-bg: #0f2524; -field-border: rgba(255,255,255,0.16);
            -rank-bg: rgba(255,255,255,0.08); -rank-text: #e2e8f0; -grid: rgba(255,255,255,0.08);
            -blob-1: rgba(20,184,166,0.22); -blob-2: rgba(59,130,246,0.18); -blob-3: rgba(184,242,61,0.12);
            -star: #dbe4f5; -vignette: rgba(0,0,0,0.55);
            -scene-top: #0b4d4a; -scene-bottom: #0a1f4d; -scene-text: #d5dcea;
            -series-1: #3b82f6; -series-2: #f59e0b; -series-3: #10b981;
            -tint-1: rgba(20,184,166,0.20); -tint-2: rgba(184,242,61,0.10);
            -glow: rgba(184,242,61,0.18); -edge: rgba(184,242,61,0.35);
            """, Color.web("#b8f23d"), Color.web("#5eead4"), true);

    private final String stylesheet;
    private final Color shardColor;
    private final Color topShardColor;
    private final boolean glow;

    Theme(String colors, Color shardColor, Color topShardColor, boolean glow) {
        String css = ".root {" + colors + "}";
        this.stylesheet = "data:text/css;base64,"
                + Base64.getEncoder().encodeToString(css.getBytes(StandardCharsets.UTF_8));
        this.shardColor = shardColor;
        this.topShardColor = topShardColor;
        this.glow = glow;
    }

    /** @return a stylesheet URL that defines this theme's named colors */
    public String stylesheet() {
        return stylesheet;
    }

    /** @return the particle chart's main shard color */
    public Color shardColor() {
        return shardColor;
    }

    /** @return the color of the tallest bar's shards */
    public Color topShardColor() {
        return topShardColor;
    }

    /** @return whether the particle chart should glow (only reads well on a dark background) */
    public boolean glow() {
        return glow;
    }

    /** @return the other theme */
    public Theme other() {
        return this == LIGHT ? DARK : LIGHT;
    }
}
