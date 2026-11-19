package glassfolio.gui;

import javafx.animation.AnimationTimer;
import javafx.event.EventHandler;
import javafx.geometry.Point2D;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;

/**
 * The background's dot grid, drawn on a canvas so each dot can move: dots near
 * the mouse part to let it through, then drift back into place once it has
 * moved on. It only redraws while some dot is moving, so a still screen costs nothing.
 */
final class DotField {
    private static final double STEP = 22;            // pixels between dots
    private static final double HALF_STEP = STEP / 2;  // a dot sits in the middle of its cell
    private static final double DOT_RADIUS = 1.1;
    private static final Color DOT_COLOR = Color.rgb(100, 116, 139, 0.26); // slate, readable on light and dark
    private static final double REACH = 120;          // how far from the mouse dots start to move
    private static final double PUSH = 18;            // how far the nearest dots move
    private static final double FOLLOW = 0.18;        // how quickly a dot moves toward where it should be
    private static final double AT_REST = 0.05;       // pixels left to move before a dot counts as still

    private final Canvas canvas = new Canvas();
    private double[] offsetX = new double[0];
    private double[] offsetY = new double[0];
    private int columns;
    private int rows;
    private double mouseX = Double.NaN; // NaN until the mouse first moves
    private double mouseY = Double.NaN;
    private boolean running;
    private final AnimationTimer timer = new AnimationTimer() {
        @Override
        public void handle(long now) {
            if (!step()) {
                halt();
            }
            draw();
        }
    };

    /**
     * Builds the field and starts listening to the mouse once it is in a Scene.
     *
     * @param holder the pane the field fills (the backdrop)
     */
    DotField(Pane holder) {
        canvas.widthProperty().bind(holder.widthProperty());
        canvas.heightProperty().bind(holder.heightProperty());
        canvas.setMouseTransparent(true);
        canvas.setManaged(false);
        canvas.widthProperty().addListener((obs, was, now) -> resize());
        canvas.heightProperty().addListener((obs, was, now) -> resize());

        EventHandler<MouseEvent> onMove = e -> {
            Point2D at = holder.sceneToLocal(e.getSceneX(), e.getSceneY());
            mouseX = at.getX();
            mouseY = at.getY();
            wake();
        };
        EventHandler<MouseEvent> onLeave = e -> {
            if (e.getTarget() == holder.getScene().getRoot()) { // the mouse left the window
                mouseX = Double.NaN; // the dots drift home
                wake();
            }
        };
        holder.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (oldScene != null) {
                oldScene.removeEventFilter(MouseEvent.MOUSE_MOVED, onMove);
                oldScene.removeEventFilter(MouseEvent.MOUSE_EXITED, onLeave);
                halt();
            }
            if (newScene != null) {
                newScene.addEventFilter(MouseEvent.MOUSE_MOVED, onMove);
                newScene.addEventFilter(MouseEvent.MOUSE_EXITED, onLeave);
            }
        });
    }

    /** @return the canvas, to place in the backdrop */
    Canvas node() {
        return canvas;
    }

    /** Starts the frame loop if it isn't running. */
    private void wake() {
        if (!running) {
            running = true;
            timer.start();
        }
    }

    /** Stops the frame loop until the mouse moves again. */
    private void halt() {
        timer.stop();
        running = false;
    }

    /** Builds a fresh, still grid for the new size and draws it once. */
    private void resize() {
        columns = (int) Math.ceil(canvas.getWidth() / STEP) + 1;
        rows = (int) Math.ceil(canvas.getHeight() / STEP) + 1;
        offsetX = new double[columns * rows];
        offsetY = new double[columns * rows];
        draw();
    }

    /**
     * Moves every dot a step toward where it should be: away from the mouse if
     * it is close, otherwise back home.
     *
     * @return true while any dot is still moving
     */
    private boolean step() {
        boolean moving = false;
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < columns; col++) {
                int i = row * columns + col;
                double homeX = col * STEP + HALF_STEP;
                double homeY = row * STEP + HALF_STEP;
                double wantX = 0;
                double wantY = 0;
                if (!Double.isNaN(mouseX)) {
                    double dx = homeX - mouseX;
                    double dy = homeY - mouseY;
                    double squared = dx * dx + dy * dy;
                    if (squared < REACH * REACH && squared > 0) { // the square root only for the few in reach
                        double distance = Math.sqrt(squared);
                        double strength = PUSH * (1 - distance / REACH); // closer dots part further
                        wantX = dx / distance * strength;
                        wantY = dy / distance * strength;
                    }
                }
                offsetX[i] += (wantX - offsetX[i]) * FOLLOW;
                offsetY[i] += (wantY - offsetY[i]) * FOLLOW;
                if (Math.abs(wantX - offsetX[i]) > AT_REST || Math.abs(wantY - offsetY[i]) > AT_REST) {
                    moving = true;
                }
            }
        }
        return moving;
    }

    /** Draws every dot at its home plus its current offset. */
    private void draw() {
        GraphicsContext pen = canvas.getGraphicsContext2D();
        pen.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
        pen.setFill(DOT_COLOR);
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < columns; col++) {
                int i = row * columns + col;
                double x = col * STEP + HALF_STEP + offsetX[i];
                double y = row * STEP + HALF_STEP + offsetY[i];
                pen.fillOval(x - DOT_RADIUS, y - DOT_RADIUS, 2 * DOT_RADIUS, 2 * DOT_RADIUS);
            }
        }
    }
}
