package glassfolio.gui;

import glassfolio.model.RiskQuiz;
import glassfolio.model.User;
import java.util.function.IntConsumer;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/**
 * The "Not sure?" quiz on the start screen: the {@link RiskQuiz} questions one
 * at a time, each answered with one click. After the last one it shows which
 * profile fits and why; "Use" hands that profile to its listener (Observer pattern). The scoring
 * lives in the model; this class only asks.
 */
final class RiskQuizPanel {
    private static final double SPACING = 16;
    private static final double ANSWER_SPACING = 12;
    private static final double ACTION_SPACING = 16;
    private static final String RESULT_STYLE = Styles.HEADING
            + "-fx-font-size: 26px; -fx-text-fill: linear-gradient(to right, -text, -accent, -series-2);";
    private static final String STEP_STYLE = Styles.META;
    /** The tinted box the quiz question or result sits in. */
    private static final String CARD_STYLE = Styles.TINT_BOX
            + "-fx-padding: 26 28; -fx-effect: dropshadow(gaussian, -glow, 20, 0, 0, 6);";
    private static final String QUESTION_STYLE = Styles.SECTION + "-fx-font-size: 21px;";
    /** Answers are big, tall pills, so the quiz fills the panel the form leaves. */
    private static final String ANSWER_SIZE = "-fx-alignment: center-left; -fx-font-size: 15px; -fx-padding: 16 24 18 24;";
    private static final String ANSWER_STYLE = Styles.SECONDARY_BUTTON + ANSWER_SIZE;
    private static final String ANSWER_HOVER_STYLE = Styles.SECONDARY_BUTTON_HOVER + ANSWER_SIZE;

    private final Label step = new Label();
    private final Label question = new Label();
    private final VBox answers = new VBox(ANSWER_SPACING);
    private final VBox root;
    private final IntConsumer onProfile;
    private int[] chosen;
    private int current;

    /**
     * Builds the quiz.
     *
     * @param onProfile called with User.STABILITY_PROFILE or User.GROWTH_PROFILE when the quiz is done
     * @param onCancel  called when the user goes back to the form without finishing
     * @throws IllegalArgumentException if either argument is null
     */
    RiskQuizPanel(IntConsumer onProfile, Runnable onCancel) {
        if (onProfile == null || onCancel == null) {
            throw new IllegalArgumentException("RiskQuizPanel needs a result listener and a cancel action");
        }
        this.onProfile = onProfile;
        step.setStyle(STEP_STYLE);
        question.setStyle(QUESTION_STYLE);
        question.setWrapText(true);
        Hyperlink back = new Hyperlink("Back to the form");
        back.setStyle(Styles.LINK);
        back.setOnAction(e -> onCancel.run());
        VBox card = new VBox(SPACING, step, question, answers);
        card.setStyle(CARD_STYLE);
        root = new VBox(SPACING, card, back);
    }

    /** @return the quiz's node, to place in a screen */
    Node node() {
        return root;
    }

    /** Starts the quiz from the first question. */
    void start() {
        chosen = new int[RiskQuiz.QUESTIONS.size()];
        current = 0;
        showQuestion();
    }

    /** Shows the current question and its answers as buttons. */
    private void showQuestion() {
        RiskQuiz.Question q = RiskQuiz.QUESTIONS.get(current);
        step.setText("Question " + (current + 1) + " of " + RiskQuiz.QUESTIONS.size());
        question.setText(q.text());
        question.setStyle(QUESTION_STYLE);
        answers.getChildren().clear();
        for (int i = 0; i < q.answers().size(); i++) {
            int answer = i;
            Button choice = new Button(q.answers().get(i));
            Styles.clickable(choice, ANSWER_STYLE, ANSWER_HOVER_STYLE);
            choice.setMaxWidth(Double.MAX_VALUE);
            choice.setOnAction(e -> answer(answer));
            answers.getChildren().add(choice);
        }
        Motion.fadeInStaggered(root, 0);
    }

    /**
     * Records an answer and moves on, or finishes after the last question.
     *
     * @param answer the chosen answer's position
     */
    private void answer(int answer) {
        chosen[current] = answer;
        current++;
        if (current < chosen.length) {
            showQuestion();
        } else {
            showResult(RiskQuiz.profileFor(chosen));
        }
    }

    /**
     * Shows the quiz's answer: which profile fits and why, with a button to use
     * it and a link to try again.
     *
     * @param profile User.STABILITY_PROFILE or User.GROWTH_PROFILE
     */
    private void showResult(int profile) {
        boolean stability = profile == User.STABILITY_PROFILE;
        String name = stability ? "Stability" : "Growth";
        step.setText("Your result");
        question.setText(name + " fits you best");
        question.setStyle(RESULT_STYLE);
        Label why = new Label(ProfileGuide.describe(stability));
        why.setStyle(Styles.BOX_TEXT + "-fx-font-size: 14px;");
        why.setWrapText(true);
        Button use = new Button("Use " + name);
        Styles.clickable(use, Styles.PRIMARY_BUTTON, Styles.PRIMARY_BUTTON_HOVER);
        use.setOnAction(e -> onProfile.accept(profile));
        Hyperlink retake = new Hyperlink("Retake the quiz");
        retake.setStyle(Styles.LINK);
        retake.setOnAction(e -> start());
        HBox actions = new HBox(ACTION_SPACING, use, retake);
        actions.setAlignment(Pos.CENTER_LEFT);
        answers.getChildren().setAll(why, actions);
        Styles.keepWholeText(root);
        Motion.fadeInStaggered(root, 0);
    }
}
