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
    /** Light glass over pale blobs; blue accent. */
    LIGHT("""
            -bg: #eef2f9;
            -surface: rgba(255,255,255,0.96); -surface-border: #e3e9f2;
            -glass: rgba(255,255,255,0.66); -glass-border: rgba(255,255,255,0.9);
            -text: #0f172a; -text-2: #475569; -text-3: #64748b;
            -accent: #2563eb; -accent-text: white;
            -up: #15803d; -down: #b91c1c; -error: #b91c1c;
            -field-bg: white; -field-border: #cbd5e1;
            -rank-bg: #eef2f8; -rank-text: #334155; -grid: #e8edf5;
            -blob-1: rgba(147,180,245,0.6); -blob-2: rgba(248,207,138,0.6); -blob-3: rgba(159,227,198,0.6);
            -star: transparent;
            -series-1: #3b82f6; -series-2: #f59e0b; -series-3: #10b981;
            """, Color.web("#2f5fd0"), Color.web("#0d9488"), false),

    /** Dark space with glowing gold and teal; gold accent. */
    DARK("""
            -bg: #05070d;
            -surface: rgba(17,24,39,0.94); -surface-border: rgba(255,255,255,0.10);
            -glass: rgba(18,24,38,0.78); -glass-border: rgba(255,255,255,0.12);
            -text: #f1f5f9; -text-2: #a7b0c3; -text-3: #8b95a9;
            -accent: #e2bd72; -accent-text: #111827;
            -up: #4ade80; -down: #f87171; -error: #fca5a5;
            -field-bg: rgba(255,255,255,0.06); -field-border: rgba(255,255,255,0.16);
            -rank-bg: rgba(255,255,255,0.08); -rank-text: #e2e8f0; -grid: rgba(255,255,255,0.08);
            -blob-1: rgba(37,99,235,0.20); -blob-2: rgba(226,189,114,0.14); -blob-3: rgba(52,214,193,0.14);
            -star: #dbe4f5;
            -series-1: #3b82f6; -series-2: #f59e0b; -series-3: #10b981;
            """, Color.web("#e2bd72"), Color.web("#34d6c1"), true);

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
