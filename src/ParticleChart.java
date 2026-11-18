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
 * The login screen's 3D centerpiece: thousands of small shards that fly in from
 * scattered space and assemble into a rising bar chart, turn once to show it is
 * 3D, then only move with the mouse. Each shard is one triangle in a shared
 * mesh, so it stays fast without creating thousands of nodes.
 */
public final class ParticleChart extends Pane {
    /** Bar heights from left to right, as a share of the tallest; rises with dips like a market. */
    private static final double[] BAR_HEIGHTS = {0.22, 0.34, 0.28, 0.48, 0.42, 0.66, 0.6, 1.0};
    private static final double BAR_SIZE = 38;
    private static final double BAR_GAP = 26;
    private static final double MAX_BAR_HEIGHT = 380;
    private static final int SHARDS_PER_UNIT_HEIGHT = 900; // shards in a full-height bar
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

    private final Shards main;
    private final Shards top;
    private final SubScene view;
    private final Rotate turn = new Rotate(0, Rotate.Y_AXIS);
    private final Rotate tilt = new Rotate(TILT_X, Rotate.X_AXIS);
    private final AnimationTimer timer;
    private long introStart = -1;
    private boolean introPlaying;
    private boolean running;
    private double mouseTilt;
    private double targetMouseTilt;
    private double mouseTurn;
    private double targetMouseTurn;

    /**
     * Builds the chart. It fills whatever space its parent gives it; the 3D
     * view scales with the height, so it looks the same windowed or full screen.
     *
     * @param theme the starting theme, which sets the shard colors and glow
     */
    public ParticleChart(Theme theme) {
        Random random = new Random(RANDOM_SEED);
        double totalWidth = BAR_HEIGHTS.length * BAR_SIZE + (BAR_HEIGHTS.length - 1) * BAR_GAP;
        double left = -totalWidth / 2;

        int mainCount = BASELINE_SHARDS;
        for (int i = 0; i < BAR_HEIGHTS.length - 1; i++) {
            mainCount += shardsFor(BAR_HEIGHTS[i]);
        }
        main = new Shards(mainCount, random);
        top = new Shards(shardsFor(BAR_HEIGHTS[BAR_HEIGHTS.length - 1]), random);

        for (int i = 0; i < BAR_HEIGHTS.length; i++) {
            Shards target = (i == BAR_HEIGHTS.length - 1) ? top : main;
            double x = left + i * (BAR_SIZE + BAR_GAP);
            target.addBar(x, BAR_HEIGHTS[i] * MAX_BAR_HEIGHT, shardsFor(BAR_HEIGHTS[i]), random);
        }
        main.addBaseline(left - BAR_GAP, totalWidth + 2 * BAR_GAP, BASELINE_SHARDS, random);

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
        main.scatter();
        top.scatter();
        introStart = -1;
        introPlaying = true;
        startTimer();
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

    private void startTimer() {
        if (!running) {
            running = true;
            timer.start();
        }
    }

    private void stopTimer() {
        running = false;
        timer.stop();
    }

    /**
     * Builds the frame loop: moves the shards while they assemble, plays one
     * eased turn, and eases the chart toward the mouse. It stops itself once
     * nothing is moving, and the mouse starts it again.
     *
     * @return the timer
     */
    private AnimationTimer buildTimer() {
        return new AnimationTimer() {
            @Override
            public void handle(long now) {
                double easedTurn = 1;
                if (introPlaying) {
                    if (introStart < 0) {
                        introStart = now;
                    }
                    double seconds = (now - introStart) / NANOS_PER_SECOND;
                    main.moveTo(Math.min(1, seconds / ASSEMBLE_SECONDS));
                    top.moveTo(Math.min(1, seconds / ASSEMBLE_SECONDS));
                    double showcase = Math.min(1, seconds / SHOWCASE_SECONDS);
                    easedTurn = 1 - Math.pow(1 - showcase, 3);
                    introPlaying = showcase < 1;
                }
                mouseTilt += (targetMouseTilt - mouseTilt) * MOUSE_FOLLOW;
                mouseTurn += (targetMouseTurn - mouseTurn) * MOUSE_FOLLOW;
                turn.setAngle(SHOWCASE_TURN_DEGREES * easedTurn + mouseTurn);
                tilt.setAngle(TILT_X + mouseTilt);

                boolean settled = Math.abs(targetMouseTilt - mouseTilt) < SETTLED
                        && Math.abs(targetMouseTurn - mouseTurn) < SETTLED;
                if (!introPlaying && settled) {
                    stopTimer();
                }
            }
        };
    }

    private static int shardsFor(double heightShare) {
        return (int) Math.round(heightShare * SHARDS_PER_UNIT_HEIGHT);
    }

    /**
     * One color of shards: a mesh with one small triangle per shard, plus where
     * each shard starts (scattered) and where it lands (on a bar).
     */
    private static final class Shards {
        private static final int AXES = 3; // x, y, z
        private static final int POINTS_PER_SHARD = 3;
        private static final int FLOATS_PER_SHARD = POINTS_PER_SHARD * AXES;
        private static final int VALUES_PER_FACE_CORNER = 2; // a point index and a texture index
        private static final int VALUES_PER_FACE = POINTS_PER_SHARD * VALUES_PER_FACE_CORNER;
        private static final int BAR_SIDES = 4;

        private final MeshView view;
        private final PhongMaterial material = new PhongMaterial();
        private final TriangleMesh mesh = new TriangleMesh();
        private final float[] points;
        private final double[] start;
        private final double[] target;
        private final double[] corners; // each shard's corner offsets, a random orientation
        private final double[] delay;
        private final Random random;
        private int added;
        private boolean landed;

        Shards(int count, Random random) {
            this.random = random;
            points = new float[count * FLOATS_PER_SHARD];
            start = new double[count * AXES];
            target = new double[count * AXES];
            corners = new double[count * FLOATS_PER_SHARD];
            delay = new double[count];

            int[] faces = new int[count * VALUES_PER_FACE];
            for (int i = 0; i < count; i++) {
                for (int corner = 0; corner < POINTS_PER_SHARD; corner++) {
                    faces[i * VALUES_PER_FACE + corner * VALUES_PER_FACE_CORNER] = i * POINTS_PER_SHARD + corner;
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

        /** Places shards on the surface of one bar, so it reads as a shell. */
        void addBar(double left, double height, int count, Random random) {
            for (int i = 0; i < count; i++) {
                double x = left + random.nextDouble() * BAR_SIZE;
                double z = -BAR_SIZE / 2 + random.nextDouble() * BAR_SIZE;
                double y = -random.nextDouble() * height;
                switch (random.nextInt(BAR_SIDES)) { // snap to one of the four sides
                    case 0 -> x = left;
                    case 1 -> x = left + BAR_SIZE;
                    case 2 -> z = -BAR_SIZE / 2;
                    default -> z = BAR_SIZE / 2;
                }
                add(x, y, z, -y / MAX_BAR_HEIGHT);
            }
        }

        /** Places a thin line of shards under the bars as the chart's floor. */
        void addBaseline(double left, double width, int count, Random random) {
            for (int i = 0; i < count; i++) {
                add(left + random.nextDouble() * width, 0, (random.nextDouble() - 0.5) * BAR_SIZE * 2, 0);
            }
        }

        private void add(double x, double y, double z, double heightShare) {
            int i = added++;
            target[i * AXES] = x;
            target[i * AXES + 1] = y;
            target[i * AXES + 2] = z;
            delay[i] = heightShare * STAGGER;
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

        /** Sends every shard to a random point in a sphere around the chart. */
        void scatter() {
            for (int i = 0; i < added; i++) {
                double ax = random.nextGaussian();
                double ay = random.nextGaussian();
                double az = random.nextGaussian();
                double length = Math.sqrt(ax * ax + ay * ay + az * az);
                double radius = SCATTER_RADIUS * Math.cbrt(random.nextDouble());
                start[i * AXES] = ax / length * radius;
                start[i * AXES + 1] = ay / length * radius - MAX_BAR_HEIGHT / 2;
                start[i * AXES + 2] = az / length * radius;
            }
            landed = false;
        }

        /**
         * Moves every shard from its start toward its bar.
         *
         * @param progress 0 at the start of the assembly, 1 when done
         */
        void moveTo(double progress) {
            if (landed) {
                return;
            }
            for (int i = 0; i < added; i++) {
                double own = Math.clamp((progress * (1 + STAGGER) - delay[i]), 0, 1);
                double eased = 1 - Math.pow(1 - own, 3);
                for (int axis = 0; axis < AXES; axis++) {
                    double center = start[i * AXES + axis] + (target[i * AXES + axis] - start[i * AXES + axis]) * eased;
                    for (int corner = 0; corner < POINTS_PER_SHARD; corner++) {
                        int at = i * FLOATS_PER_SHARD + corner * AXES + axis;
                        points[at] = (float) (center + corners[at]);
                    }
                }
            }
            mesh.getPoints().set(0, points, 0, added * FLOATS_PER_SHARD);
            landed = progress >= 1;
        }
    }
}
