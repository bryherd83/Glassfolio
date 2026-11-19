package glassfolio.model;

import java.util.List;

/**
 * Three everyday questions that pick a risk profile for someone who doesn't
 * know what volatility means. Each answer is worth 0, 1 or 2 points; a total of
 * {@link #GROWTH_THRESHOLD} or more means Growth, anything less means Stability,
 * so an unsure user lands on the safer profile.
 */
public final class RiskQuiz {
    /**
     * One question and its three answers, from most careful to most daring.
     *
     * @param text    the question
     * @param answers the three answers; an answer's points are its position (0, 1 or 2)
     */
    public record Question(String text, List<String> answers) { }

    /** The questions, in the order they are asked. */
    public static final List<Question> QUESTIONS = List.of(
            new Question("If your $1,000 dropped to $800 next month, you would...",
                    List.of("Sell to stop the loss", "Wait for it to recover", "Buy more while it's cheaper")),
            new Question("When will you need this money?",
                    List.of("Within a year", "In 1 to 5 years", "Not for 5 years or more")),
            new Question("What matters more to you?",
                    List.of("Not losing money", "A bit of both", "Growing it as much as possible")));
    /** The lowest total that picks Growth. */
    public static final int GROWTH_THRESHOLD = 4;

    private RiskQuiz() {
    }

    /**
     * Picks a profile from the chosen answers.
     *
     * @param answers the chosen answer for each question, in order; each is 0, 1 or 2
     * @return {@link User#GROWTH_PROFILE} or {@link User#STABILITY_PROFILE}
     * @throws IllegalArgumentException if the number of answers or any answer is out of range
     */
    public static int profileFor(int[] answers) {
        if (answers == null || answers.length != QUESTIONS.size()) {
            throw new IllegalArgumentException("Answer every question once");
        }
        int total = 0;
        for (int i = 0; i < answers.length; i++) {
            if (answers[i] < 0 || answers[i] >= QUESTIONS.get(i).answers().size()) {
                throw new IllegalArgumentException("Answer " + (i + 1) + " is not one of the choices");
            }
            total += answers[i];
        }
        return total >= GROWTH_THRESHOLD ? User.GROWTH_PROFILE : User.STABILITY_PROFILE;
    }
}
