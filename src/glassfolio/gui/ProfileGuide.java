package glassfolio.gui;

import glassfolio.model.Growth;
import glassfolio.model.Stability;

/**
 * Plain-language text that explains the two risk profiles, so the user can pick
 * one without knowing what volatility means. The limits come from the strategy
 * classes, so the text can't drift from the rules.
 */
public final class ProfileGuide {
    /** The reminder shown under the form. */
    public static final String NOTE = "Picks use the past year only, and the past doesn't predict the future. "
            + "This is not financial advice.";
    private static final double PERCENT = 100;

    private ProfileGuide() {
    }

    /**
     * Explains one profile in a sentence or two.
     *
     * @param stability true for Stability, false for Growth
     * @return what the profile is for and how it picks
     */
    public static String describe(boolean stability) {
        if (stability) {
            double limit = Stability.DEFAULT_MAX_VOLATILITY * Stability.STABILITY_PROFILE_FACTOR * PERCENT;
            return String.format("Steadiest index last year, under %.1f%% swing.",
                    limit);
        }
        return String.format("Fastest-growing index last year, at least %.0f%%.",
                Growth.DEFAULT_MIN_GROWTH * PERCENT);
    }
}
