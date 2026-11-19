package glassfolio.model;

import java.util.ArrayList;
import java.util.List;

/**
 * How much of the past year a chart shows. Each period re-measures the change
 * from its own first day, so "1 week" answers "how did it move this week?"
 * instead of showing a flat slice of the yearly change.
 */
public enum Period {
    WEEK("1 week", 5),
    MONTH("1 month", 21),
    THREE_MONTHS("3 months", 63),
    YEAR("1 year", Integer.MAX_VALUE);

    private static final double PERCENT = 100;

    private final String label;
    private final int tradingDays;

    /**
     * @param label       the text on the period's button
     * @param tradingDays how many trading days it covers (markets trade about 5 days a week)
     */
    Period(String label, int tradingDays) {
        this.label = label;
        this.tradingDays = tradingDays;
    }

    /** @return the text on the period's button, like "1 month" */
    public String label() {
        return label;
    }

    /**
     * Cuts a year of daily changes down to this period and re-measures each day
     * as the change since the period's first day.
     *
     * @param changeSinceYearAgo each day's change since a year ago in percent, oldest first
     * @return each day in the period as the change since the period began, in percent, oldest first
     * @throws IllegalArgumentException if the list is null
     */
    public List<Double> window(List<Double> changeSinceYearAgo) {
        if (changeSinceYearAgo == null) {
            throw new IllegalArgumentException("Period needs a list of values");
        }
        List<Double> result = new ArrayList<>();
        if (changeSinceYearAgo.isEmpty()) {
            return result;
        }
        int start = Math.max(0, changeSinceYearAgo.size() - 1 - tradingDays);
        double base = 1 + changeSinceYearAgo.get(start) / PERCENT;
        if (base <= 0) { // a -100% start leaves nothing to measure from
            return result;
        }
        for (int day = start; day < changeSinceYearAgo.size(); day++) {
            result.add(((1 + changeSinceYearAgo.get(day) / PERCENT) / base - 1) * PERCENT);
        }
        return result;
    }
}
