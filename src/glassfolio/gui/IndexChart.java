package glassfolio.gui;

import glassfolio.data.HistoryCache;
import glassfolio.model.Period;
import glassfolio.model.Stock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.DoubleFunction;
import javafx.geometry.Pos;
import javafx.geometry.Insets;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

/**
 * The chart card on the results screen: one line per index, with a picker for
 * the past week, month, three months or year. Every line is drawn as percent
 * change, so indexes with very different prices fit on one scale. Pointing at
 * the chart shows each index's change on that day (see {@link ChartReadout}).
 */
final class IndexChart {
    private static final double CHART_HEIGHT = 260; // the chart's smallest height; it grows into spare room
    private static final double CARD_SPACING = 4;
    private static final Insets CARD_PADDING = new Insets(16, 18, 10, 18);
    private static final double HEADER_SPACING = 12;
    private static final int MONTHS_PER_YEAR = 12;
    private static final int DATE_TICKS = 6;
    private static final double DAYS_PER_YEAR = 365;
    private static final String TOP_LINE_STYLE = "-fx-stroke-width: 4px;"; // tells the top pick apart without color
    private static final String HEADING_STYLE = Styles.SECTION + "-fx-text-fill: linear-gradient(to right, -text, -accent);";
    private static final DateTimeFormatter TICK_DATE = DateTimeFormatter.ofPattern("MMM d", Locale.US);
    private static final DateTimeFormatter HOVER_DATE = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.US);

    private IndexChart() {
    }

    /**
     * Builds the chart card, showing the past year first.
     *
     * @param indexes   the indexes to draw
     * @param history   where each index's past year comes from
     * @param topTicker the top pick's series id, drawn thicker; null if there is none
     * @return the card
     */
    static VBox build(List<Stock> indexes, HistoryCache history, String topTicker) {
        Label heading = new Label();
        heading.setStyle(HEADING_STYLE);
        StackPane holder = new StackPane();
        VBox.setVgrow(holder, Priority.ALWAYS);

        ToggleGroup group = new ToggleGroup();
        HBox picker = new HBox();
        picker.setStyle(Styles.WELL);
        for (Period period : Period.values()) {
            ToggleButton choice = new ToggleButton(period.label());
            choice.setUserData(period);
            choice.setToggleGroup(group);
            choice.setStyle(Styles.SEGMENT);
            choice.selectedProperty().addListener((obs, was, selected) ->
                    choice.setStyle(selected ? Styles.SEGMENT_SELECTED : Styles.SEGMENT));
            picker.getChildren().add(choice);
        }
        group.selectedToggleProperty().addListener((obs, old, now) -> {
            if (now == null) { // clicking the picked period again keeps it picked
                old.setSelected(true);
                return;
            }
            Period period = (Period) now.getUserData();
            heading.setText("How each index moved over the past " + period.label().replace("1 ", ""));
            holder.getChildren().setAll(buildChart(indexes, history, topTicker, period));
        });
        group.selectToggle(group.getToggles().get(Period.YEAR.ordinal()));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox header = new HBox(HEADER_SPACING, heading, spacer, picker);
        header.setAlignment(Pos.CENTER_LEFT);
        VBox card = new VBox(CARD_SPACING, header, holder);
        card.setPadding(CARD_PADDING);
        card.setStyle(Styles.SOLID_CARD);
        return card;
    }

    /**
     * Builds the chart for one period, with its hover readout.
     *
     * @param indexes   the indexes to draw
     * @param history   where each index's past year comes from
     * @param topTicker the top pick's series id, drawn thicker; null if there is none
     * @param period    how much of the year to show
     * @return the chart inside its readout layer
     */
    private static StackPane buildChart(List<Stock> indexes, HistoryCache history, String topTicker, Period period) {
        int yearLastDay = 0;
        List<List<Double>> windows = new ArrayList<>();
        List<String> names = new ArrayList<>();
        for (Stock stock : indexes) {
            List<Double> year = history.getPercentChange(stock.getTicker());
            yearLastDay = Math.max(yearLastDay, year.size() - 1);
            windows.add(period.window(year));
            names.add(stock.getName());
        }
        int lastDay = windows.stream().mapToInt(List::size).max().orElse(1) - 1;
        // shortcut: assumes every index has the same days (true for FRED's three indexes); store dates to drop this
        int firstYearDay = yearLastDay - lastDay; // where this window starts within the year
        int yearEnd = yearLastDay;

        double axisEnd = Math.max(lastDay, 1);
        boolean isYear = period == Period.YEAR;
        NumberAxis days = new NumberAxis(0, axisEnd, isYear ? axisEnd / MONTHS_PER_YEAR : Math.max(1, Math.round(axisEnd / DATE_TICKS)));
        days.setMinorTickVisible(false);
        days.setTickLabelFormatter(labels(day -> dateOf(firstYearDay + day, yearEnd), isYear));
        NumberAxis change = new NumberAxis();
        change.setTickLabelFormatter(new StringConverter<>() { // whole percents read faster than "-2.5"
            @Override
            public String toString(Number value) {
                return Math.round(value.doubleValue()) + "%";
            }

            @Override
            public Number fromString(String text) {
                throw new UnsupportedOperationException("the axis only shows labels");
            }
        });

        LineChart<Number, Number> chart = new LineChart<>(days, change);
        chart.setCreateSymbols(false);
        chart.setAnimated(false);
        chart.setMinHeight(CHART_HEIGHT);
        chart.setPrefHeight(CHART_HEIGHT); // grows into spare room; a large default forces scrolling
        for (int i = 0; i < indexes.size(); i++) {
            XYChart.Series<Number, Number> line = new XYChart.Series<>();
            line.setName(names.get(i));
            List<Double> values = windows.get(i);
            for (int day = 0; day < values.size(); day++) {
                line.getData().add(new XYChart.Data<>(day, values.get(day)));
            }
            chart.getData().add(line);
            if (indexes.get(i).getTicker().equals(topTicker)) {
                line.getNode().setStyle(line.getNode().getStyle() + TOP_LINE_STYLE);
            }
        }
        return ChartReadout.attach(chart, days, change, names, windows,
                day -> dateOf(firstYearDay + day, yearEnd).format(HOVER_DATE));
    }

    /**
     * Estimates the calendar date of a trading-day number.
     *
     * @param day     the day number, 0 for a year ago
     * @param lastDay the number of the newest day, which is today
     * @return the estimated date
     */
    static LocalDate dateOf(double day, int lastDay) {
        // shortcut: spreads trading days evenly over 365 days; exact only if the cache kept dates
        long daysAgo = lastDay == 0 ? 0 : Math.round((lastDay - day) * DAYS_PER_YEAR / lastDay);
        return LocalDate.now().minusDays(daysAgo);
    }

    /**
     * Labels the x-axis: month names for a year, dates like "Nov 12" for shorter periods.
     *
     * @param date   turns a point's number into its date
     * @param months true to show month names
     * @return the formatter for the x-axis
     */
    private static StringConverter<Number> labels(DoubleFunction<LocalDate> date, boolean months) {
        return new StringConverter<>() {
            @Override
            public String toString(Number day) {
                LocalDate when = date.apply(day.doubleValue());
                return months ? when.getMonth().getDisplayName(TextStyle.SHORT, Locale.US) : when.format(TICK_DATE);
            }

            @Override
            public Number fromString(String text) {
                throw new UnsupportedOperationException("the axis only shows labels");
            }
        };
    }
}
