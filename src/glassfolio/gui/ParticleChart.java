package glassfolio.gui;

import java.util.List;
import java.util.Random;
import javafx.animation.AnimationTimer;
import javafx.beans.value.ObservableValue;
import javafx.scene.AmbientLight;
import javafx.scene.Group;
import javafx.scene.PerspectiveCamera;
import javafx.scene.Scene;
import javafx.scene.SceneAntialiasing;
import javafx.scene.SubScene;
import javafx.scene.effect.Bloom;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;
import javafx.scene.transform.Rotate;
import javafx.stage.Window;

/**
 * The start screen's 3D centerpiece: thousands of small shards that fly in from
 * scattered space and assemble into a bar chart, turn once to show it is 3D,
 * then only move with the mouse. Once market data arrives the shards flow into
 * the real shape of the data (see {@link #showData}). Each shard is one triangle
 * in a shared mesh, so it stays fast without creating thousands of nodes.
 */
public final class ParticleChart extends Pane {
    private static final int MONTHS_PER_YEAR = 12;
    /** Placeholder bar heights until real data arrives, as shares of the tallest. */
    private static final double[] PLACEHOLDER_HEIGHTS = {0.22, 0.34, 0.28, 0.48, 0.42, 0.66, 0.6, 1.0};
    private static final double CHART_WIDTH = 486;
    private static final double GAP_TO_BAR = 0.68; // the gap between bars as a share of a bar's width
    private static final double MAX_BAR_HEIGHT = 380;
    private static final int MAX_BARS = 12; // the shard pools only divide cleanly up to this many bars
    private static final double MIN_BAR_SHARE = 0.15; // the lowest bar still reads as a bar
    private static final int MAIN_SHARDS = 2860;
    private static final int TOP_SHARDS = 900;
    private static final int BASELINE_SHARDS = 160;
    private static final double SHARD_SIZE = 3.2;
    private static final double SCATTER_RADIUS = 700;

    private static final double BLOOM_THRESHOLD = 0.35;

    private static final double CAMERA_DISTANCE = -1250;
    private static final double FIELD_OF_VIEW = 32;
    private static final double NEAR_CLIP = 1;
    private static final double FAR_CLIP = 5000;
    private static final double TILT_X = -18; // look down at the chart a little
    private static final double SHOWCASE_TURN_DEGREES = 360;
    private static final double SHOWCASE_SECONDS = 6;
    /** The cosine of the turn below which the chart face is too far away to aim a split at. */
    private static final double MIN_FACING = 0.35;
    private static final double MOUSE_TILT_DEGREES = 10;
    private static final double MOUSE_TURN_DEGREES = 30;
    private static final double MOUSE_FOLLOW = 0.06; // how fast the chart catches up with the mouse
    private static final double SETTLED = 0.01; // degrees left to move before the timer can rest
    private static final double CENTER = 0.5;
    private static final double STARTING_SIZE = 1; // replaced by the bound size on first layout
    private static final double ASSEMBLE_SECONDS = 2.8;
    private static final double STAGGER = 0.45; // bottom shards land first, the top ones last
    private static final double NANOS_PER_SECOND = 1e9;
    private static final long RANDOM_SEED = 350; // same layout every launch
    // the hologram split: shards near the mouse fly apart, then glide back
    private static final double SPLIT_REACH = 35;     // chart units around the mouse that react
    private static final double SPLIT_PUSH = 30;      // how far the nearest shards fly
    private static final double SPLIT_FOLLOW = 0.16;  // how fast a shard moves toward where it should be
    private static final double SPLIT_DEPTH = 0.6;    // shards also scatter in depth, for the 3D look
    private static final long SPLIT_HOLD_NANOS = 1_000_000_000L; // a split shard stays apart a second
    private static final double SPLIT_AT_REST = 0.05; // chart units left to move before a shard counts as still
    private static final double VIEW_HALF_ANGLE = Math.toRadians(FIELD_OF_VIEW / 2);

    private final Random random = new Random(RANDOM_SEED);
    private final Shards main = new Shards(MAIN_SHARDS, random);
    private final Shards top = new Shards(TOP_SHARDS, random);
    private final SubScene view;
    private final Rotate turn = new Rotate(0, Rotate.Y_AXIS);
    private final Rotate tilt = new Rotate(TILT_X, Rotate.X_AXIS);
    private final AnimationTimer timer;
    private double[] barHeights = PLACEHOLDER_HEIGHTS;
    private long assembleStart = -1;
    private boolean assembling;
    private long turnStart = -1;
    private boolean turning;
    private boolean running;
    private double mouseTilt;
    private double targetMouseTilt;
    private double mouseTurn;
    private double targetMouseTurn;
    private boolean played; // the assembly and showcase turn play only the first time
    private double splitX = Double.NaN; // the mouse in chart units, or NaN when it is away
    private double splitY = Double.NaN;

    /**
     * Builds the chart. It fills whatever space its parent gives it; the 3D
     * view scales with the height, so it looks the same windowed or full screen.
     *
     * @param theme the starting theme, which sets the shard colors and glow
     */
    public ParticleChart(Theme theme) {
        layOutBars();

        Group chart = new Group(main.view, top.view);
        chart.setTranslateY(MAX_BAR_HEIGHT / 2); // center the chart's height on the camera
        Group world = new Group(chart, new AmbientLight(Color.WHITE));
        world.getTransforms().addAll(tilt, turn);

        PerspectiveCamera camera = new PerspectiveCamera(true);
        camera.setFieldOfView(FIELD_OF_VIEW);
        camera.setNearClip(NEAR_CLIP);
        camera.setFarClip(FAR_CLIP);
        camera.setTranslateZ(CAMERA_DISTANCE);

        view = new SubScene(world, STARTING_SIZE, STARTING_SIZE, true, SceneAntialiasing.BALANCED);
        view.setFill(Color.TRANSPARENT);
        view.setCamera(camera);
        view.widthProperty().bind(widthProperty());
        view.heightProperty().bind(heightProperty());
        view.setManaged(false); // sized by the bindings above, not by the layout
        getChildren().add(view);
        setMinSize(0, 0);
        setTheme(theme);

        timer = buildTimer();
        setOnMouseMoved(e -> {
            targetMouseTilt = (e.getY() / getHeight() - CENTER) * -MOUSE_TILT_DEGREES * 2;
            targetMouseTurn = (e.getX() / getWidth() - CENTER) * MOUSE_TURN_DEGREES * 2;
            // shortcut: maps the mouse onto the chart's face as if it were not tilted; the turn is
            // corrected in the frame loop, and the tilt stays small
            double unitsPerPixel = -2 * CAMERA_DISTANCE * Math.tan(VIEW_HALF_ANGLE) / getHeight();
            splitX = (e.getX() - getWidth() * CENTER) * unitsPerPixel;
            splitY = (e.getY() - getHeight() * CENTER) * unitsPerPixel - MAX_BAR_HEIGHT / 2;
            startTimer();
        });
        setOnMouseExited(e -> {
            splitX = Double.NaN; // the shards glide back together
            startTimer();
        });

        // Only animate while on screen, so the results screen doesn't pay for it
        ObservableValue<Window> window = sceneProperty().flatMap(Scene::windowProperty);
        window.addListener((obs, oldWindow, newWindow) -> {
            if (newWindow == null) {
                stopTimer();
            }
        });
    }

    /** Scatters the shards, then plays the assembly and one showcase turn. */
    public void play() {
        if (played) { // coming back to the screen: no replay, so the chart reacts at once
            startTimer();
            return;
        }
        played = true;
        main.scatter();
        top.scatter();
        assembleStart = -1;
        assembling = true;
        turnStart = -1;
        turning = true;
        startTimer();
    }

    /**
     * Picks one value per month from a year of daily values: the last trading
     * day of each twelfth of the year. Gives {@link #showData} its bars.
     *
     * @param daily the year's daily values, oldest first
     * @return up to 12 values, oldest first (fewer if there are fewer daily values)
     * @throws IllegalArgumentException if the list is null
     */
    public static double[] monthlyValues(List<Double> daily) {
        if (daily == null) {
            throw new IllegalArgumentException("Daily values are missing");
        }
        int months = Math.min(MONTHS_PER_YEAR, daily.size());
        double[] values = new double[months];
        for (int i = 0; i < months; i++) {
            int last = (int) Math.round((i + 1) * (double) daily.size() / months) - 1;
            values[i] = daily.get(last);
        }
        return values;
    }

    /**
     * Reshapes the bars to match real values, flowing the shards from where
     * they are now. The last value's bar is highlighted as the newest.
     *
     * @param values the values to show, oldest first, 2 to 12 of them
     * @throws IllegalArgumentException if there are fewer than 2 or more than 12 values
     */
    public void showData(double[] values) {
        if (values == null || values.length < 2 || values.length > MAX_BARS) {
            throw new IllegalArgumentException("The chart needs 2 to " + MAX_BARS + " values");
        }
        double min = Double.MAX_VALUE;
        double max = -Double.MAX_VALUE;
        for (double v : values) {
            min = Math.min(min, v);
            max = Math.max(max, v);
        }
        double range = (max == min) ? 1 : max - min; // flat data: avoid dividing by zero
        double[] heights = new double[values.length];
        for (int i = 0; i < values.length; i++) {
            heights[i] = MIN_BAR_SHARE + (values[i] - min) / range * (1 - MIN_BAR_SHARE);
        }
        barHeights = heights;
        main.holdCurrentAsStart();
        top.holdCurrentAsStart();
        layOutBars();
        assembleStart = -1;
        assembling = true;
        if (getScene() != null && getScene().getWindow() != null) { // off-screen, play() starts it later
            startTimer();
        }
    }

    /**
     * Recolors the shards for a theme. The glow only suits the dark theme; on a
     * light background it would wash the shards out.
     *
     * @param theme the theme to use
     */
    public void setTheme(Theme theme) {
        main.setColor(theme.shardColor());
        top.setColor(theme.topShardColor());
        view.setEffect(theme.glow() ? new Bloom(BLOOM_THRESHOLD) : null);
    }

    /**
     * Gives every shard a target on the current bars: the last bar gets the
     * highlight shards, the others share the main shards by height, and a few
     * main shards form the floor line.
     */
    private void layOutBars() {
        int bars = barHeights.length;
        double barSize = CHART_WIDTH / (bars + (bars - 1) * GAP_TO_BAR);
        double gap = barSize * GAP_TO_BAR;
        double left = -CHART_WIDTH / 2;

        double mainHeightTotal = 0;
        for (int i = 0; i < bars - 1; i++) {
            mainHeightTotal += barHeights[i];
        }
        main.clearTargets();
        top.clearTargets();
        int barShards = MAIN_SHARDS - BASELINE_SHARDS;
        int placed = 0;
        for (int i = 0; i < bars - 1; i++) {
            int count = (i == bars - 2) ? barShards - placed // the rounding remainder goes to the last one
                    : (int) Math.round(barShards * barHeights[i] / mainHeightTotal);
            main.addBar(left + i * (barSize + gap), barSize, barHeights[i] * MAX_BAR_HEIGHT, count);
            placed += count;
        }
        top.addBar(left + (bars - 1) * (barSize + gap), barSize, barHeights[bars - 1] * MAX_BAR_HEIGHT, TOP_SHARDS);
        main.addBaseline(left - gap, CHART_WIDTH + 2 * gap, barSize, BASELINE_SHARDS);
    }

    /** Starts the frame loop if it isn't already running. */
    private void startTimer() {
        if (!running) {
            running = true;
            timer.start();
        }
    }

    /** Stops the frame loop; the mouse or new data starts it again. */
    private void stopTimer() {
        running = false;
        timer.stop();
    }

    /**
     * Builds the frame loop: moves the shards while they assemble, plays one
     * eased turn, and eases the chart toward the mouse. It stops itself once
     * nothing is moving, and the mouse or new data starts it again.
     *
     * @return the timer
     */
    private AnimationTimer buildTimer() {
        return new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (assembling) {
                    if (assembleStart < 0) {
                        assembleStart = now;
                    }
                    double progress = Math.min(1, (now - assembleStart) / NANOS_PER_SECOND / ASSEMBLE_SECONDS);
                    main.moveTo(progress);
                    top.moveTo(progress);
                    assembling = progress < 1;
                }
                double easedTurn = 1;
                if (turning) {
                    if (turnStart < 0) {
                        turnStart = now;
                    }
                    double showcase = Math.min(1, (now - turnStart) / NANOS_PER_SECOND / SHOWCASE_SECONDS);
                    easedTurn = 1 - Math.pow(1 - showcase, 3);
                    turning = showcase < 1;
                }
                mouseTilt += (targetMouseTilt - mouseTilt) * MOUSE_FOLLOW;
                mouseTurn += (targetMouseTurn - mouseTurn) * MOUSE_FOLLOW;
                turn.setAngle(SHOWCASE_TURN_DEGREES * easedTurn + mouseTurn);
                tilt.setAngle(TILT_X + mouseTilt);

                // the chart can be split from the start: while it turns, the mouse is mapped onto its face,
                // and the split pauses only while the face is turned too far away to aim at
                double facing = Math.cos(Math.toRadians(SHOWCASE_TURN_DEGREES * easedTurn + mouseTurn));
                double mouseX = facing > MIN_FACING ? splitX / facing : Double.NaN;
                boolean split = main.split(mouseX, splitY, now) | top.split(mouseX, splitY, now); // both must run
                main.draw();
                top.draw();
                boolean settled = Math.abs(targetMouseTilt - mouseTilt) < SETTLED
                        && Math.abs(targetMouseTurn - mouseTurn) < SETTLED;
                if (!assembling && !turning && settled && !split) {
                    stopTimer();
                }
            }
        };
    }

    /**
     * One color of shards: a mesh with one small triangle per shard, plus where
     * each shard starts, where it is now, and where it lands.
     */
    private static final class Shards {
        private static final int AXES = 3; // x, y, z
        private static final int POINTS_PER_SHARD = 3;
        private static final int FLOATS_PER_SHARD = POINTS_PER_SHARD * AXES;
        private static final int VALUES_PER_FACE_CORNER = 2; // a point index and a texture index
        private static final int VALUES_PER_FACE = POINTS_PER_SHARD * VALUES_PER_FACE_CORNER;
        private static final int BAR_SIDES = 4;
        private static final double BASELINE_DEPTH = 2; // the floor line is twice a bar's depth

        private final MeshView view;
        private final PhongMaterial material = new PhongMaterial();
        private final TriangleMesh mesh = new TriangleMesh();
        private final float[] points;
        private final double[] start;
        private final double[] current;
        private final double[] target;
        private final double[] corners; // each shard's corner offsets, a random orientation
        private final double[] delay;
        private final double[] push;    // each shard's extra offset from the hologram split
        private final long[] heldUntil; // when a split shard may start drifting back
        private final Random random;
        private int added;

        /**
         * Builds the mesh for a number of shards, each a tiny triangle turned a random way.
         *
         * @param count  how many shards
         * @param random where the random turns come from, so every launch looks the same
         */
        Shards(int count, Random random) {
            this.random = random;
            points = new float[count * FLOATS_PER_SHARD];
            start = new double[count * AXES];
            current = new double[count * AXES];
            target = new double[count * AXES];
            corners = new double[count * FLOATS_PER_SHARD];
            delay = new double[count];
            push = new double[count * AXES];
            heldUntil = new long[count];

            int[] faces = new int[count * VALUES_PER_FACE];
            for (int i = 0; i < count; i++) {
                for (int corner = 0; corner < POINTS_PER_SHARD; corner++) {
                    faces[i * VALUES_PER_FACE + corner * VALUES_PER_FACE_CORNER] = i * POINTS_PER_SHARD + corner;
                }
                for (int corner = 0; corner < POINTS_PER_SHARD; corner++) {
                    double ax = random.nextGaussian();
                    double ay = random.nextGaussian();
                    double az = random.nextGaussian();
                    double length = Math.sqrt(ax * ax + ay * ay + az * az);
                    corners[i * FLOATS_PER_SHARD + corner * AXES] = ax / length * SHARD_SIZE;
                    corners[i * FLOATS_PER_SHARD + corner * AXES + 1] = ay / length * SHARD_SIZE;
                    corners[i * FLOATS_PER_SHARD + corner * AXES + 2] = az / length * SHARD_SIZE;
                }
            }
            mesh.getTexCoords().addAll(0, 0);
            mesh.getFaces().addAll(faces);
            mesh.getPoints().addAll(points);

            view = new MeshView(mesh);
            view.setCullFace(CullFace.NONE); // shards show from both sides as the chart turns
            material.setSpecularColor(Color.TRANSPARENT);
            view.setMaterial(material);
        }

        /** @param color the new shard color */
        void setColor(Color color) {
            material.setDiffuseColor(color);
        }

        /** Forgets every target, before new bars are laid out. */
        void clearTargets() {
            added = 0;
        }

        /**
         * Places shards on the surface of one bar, so it reads as a shell.
         *
         * @param left   the bar's left edge, in chart units
         * @param size   the bar's width and depth
         * @param height the bar's height
         * @param count  how many shards make the bar
         */
        void addBar(double left, double size, double height, int count) {
            for (int i = 0; i < count; i++) {
                double x = left + random.nextDouble() * size;
                double z = -size / 2 + random.nextDouble() * size;
                double y = -random.nextDouble() * height;
                switch (random.nextInt(BAR_SIDES)) { // snap to one of the four sides
                    case 0 -> x = left;
                    case 1 -> x = left + size;
                    case 2 -> z = -size / 2;
                    default -> z = size / 2;
                }
                add(x, y, z, -y / MAX_BAR_HEIGHT);
            }
        }

        /**
         * Places a thin line of shards under the bars as the chart's floor.
         *
         * @param left    the line's left end, in chart units
         * @param width   the line's length
         * @param barSize a bar's width, which sets the line's depth
         * @param count   how many shards make the line
         */
        void addBaseline(double left, double width, double barSize, int count) {
            for (int i = 0; i < count; i++) {
                add(left + random.nextDouble() * width, 0,
                        (random.nextDouble() - CENTER) * barSize * BASELINE_DEPTH, 0);
            }
        }

        private void add(double x, double y, double z, double heightShare) {
            int i = added++;
            target[i * AXES] = x;
            target[i * AXES + 1] = y;
            target[i * AXES + 2] = z;
            delay[i] = heightShare * STAGGER;
        }

        /** Sends every shard to a random point in a sphere around the chart. */
        void scatter() {
            for (int i = 0; i < start.length / AXES; i++) {
                double ax = random.nextGaussian();
                double ay = random.nextGaussian();
                double az = random.nextGaussian();
                double length = Math.sqrt(ax * ax + ay * ay + az * az);
                double radius = SCATTER_RADIUS * Math.cbrt(random.nextDouble());
                start[i * AXES] = ax / length * radius;
                start[i * AXES + 1] = ay / length * radius - MAX_BAR_HEIGHT / 2;
                start[i * AXES + 2] = az / length * radius;
            }
            System.arraycopy(start, 0, current, 0, start.length);
        }

        /** Makes the next move start from where every shard is right now. */
        void holdCurrentAsStart() {
            System.arraycopy(current, 0, start, 0, current.length);
        }

        /**
         * Moves every shard from its start toward its target.
         *
         * @param progress 0 at the start of the move, 1 when done
         */
        void moveTo(double progress) {
            for (int i = 0; i < added; i++) {
                double own = Math.clamp((progress * (1 + STAGGER) - delay[i]), 0, 1);
                double eased = 1 - Math.pow(1 - own, 3);
                for (int axis = 0; axis < AXES; axis++) {
                    int at = i * AXES + axis;
                    current[at] = start[at] + (target[at] - start[at]) * eased;
                }
            }
        }

        /**
         * Pushes the shards near the mouse apart, like a hologram breaking up,
         * and lets the others drift back to their places.
         *
         * @param mouseX the mouse across the chart, in chart units, or NaN when it is away
         * @param mouseY the mouse up the chart, in chart units
         * @param now    the frame's time in nanoseconds, so a split shard can hold for a moment
         * @return true while any shard is still moving or held apart
         */
        boolean split(double mouseX, double mouseY, long now) {
            boolean moving = false;
            for (int i = 0; i < added; i++) {
                double wantX = 0;
                double wantY = 0;
                double wantZ = 0;
                if (!Double.isNaN(mouseX)) {
                    double dx = current[i * AXES] - mouseX;
                    double dy = current[i * AXES + 1] - mouseY;
                    double squared = dx * dx + dy * dy;
                    if (squared < SPLIT_REACH * SPLIT_REACH && squared > 0) { // the square root only for the few in reach
                        double distance = Math.sqrt(squared);
                        double strength = SPLIT_PUSH * (1 - distance / SPLIT_REACH);
                        wantX = dx / distance * strength;
                        wantY = dy / distance * strength;
                        wantZ = corners[i * FLOATS_PER_SHARD] / SHARD_SIZE * strength * SPLIT_DEPTH;
                        heldUntil[i] = now + SPLIT_HOLD_NANOS;
                    }
                }
                if (wantX == 0 && wantY == 0 && now < heldUntil[i]) {
                    moving = true; // stays apart a moment after the mouse passes
                    continue;
                }
                moving |= ease(i * AXES, wantX);
                moving |= ease(i * AXES + 1, wantY);
                moving |= ease(i * AXES + 2, wantZ);
            }
            return moving;
        }

        /**
         * Moves one of a shard's split offsets a step toward where it should be.
         *
         * @param at   the offset's index in the push array
         * @param want where the offset should end up
         * @return true while it still has a way to go
         */
        private boolean ease(int at, double want) {
            push[at] += (want - push[at]) * SPLIT_FOLLOW;
            return Math.abs(want - push[at]) > SPLIT_AT_REST;
        }

        /** Writes every shard's corners into the mesh: where it is, plus its split offset. */
        void draw() {
            for (int i = 0; i < added; i++) {
                for (int axis = 0; axis < AXES; axis++) {
                    int at = i * AXES + axis;
                    double center = current[at] + push[at];
                    for (int corner = 0; corner < POINTS_PER_SHARD; corner++) {
                        int point = i * FLOATS_PER_SHARD + corner * AXES + axis;
                        points[point] = (float) (center + corners[point]);
                    }
                }
            }
            mesh.getPoints().set(0, points, 0, added * FLOATS_PER_SHARD);
        }
    }
}
