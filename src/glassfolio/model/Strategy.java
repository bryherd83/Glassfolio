package glassfolio.model;

/**
 * Base class for the investing strategies (Strategy pattern).
 * Each child scores an index its own way, and callers only use score()
 * and explain(), so a new strategy plugs in without changing the screens.
 */
public abstract class Strategy {

    /** Turns fractions like 0.12 into points like 12 so scores are easy to read. */
    protected static final double SCORE_SCALE = 100;

    private final String name;

    /**
     * @param name the strategy's display name, for example "Growth"
     * @throws IllegalArgumentException if the name is null or blank
     */
    public Strategy(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Strategy needs a name");
        }
        this.name = name;
    }

    /** @return the strategy's display name */
    public String getName(){
        return name;
    }

    /**
     * Adjusts a base limit for the given user.
     *
     * @param base the limit before adjustment
     * @param user the user being advised
     * @return the limit to use for this user
     */
    public abstract double adjustThreshold(double base, User user);

    /**
     * Scores an index for this user. Higher is better and 0 means "don't suggest it".
     *
     * @param stock the index to score
     * @param user  the user being advised
     * @return the score, 0 or more
     */
    public abstract double score(Stock stock, User user);

    /**
     * Explains in one sentence why an index was suggested.
     *
     * @param stock the suggested index
     * @param user  the user being advised
     * @return the explanation
     */
    public abstract String explain(Stock stock, User user);

    /**
     * Explains in one sentence why an index was not suggested.
     *
     * @param stock the index that scored 0
     * @param user  the user being advised
     * @return the explanation
     */
    public abstract String explainMiss(Stock stock, User user);

    /**
     * States the rule in plain words, with a one-line meaning of its key term.
     *
     * @param user the user being advised
     * @return the rule
     */
    public abstract String rule(User user);

    /**
     * Shows how an index's score was worked out, or why it scored 0.
     *
     * @param stock the index
     * @param user  the user being advised
     * @return the score math in plain words
     */
    public abstract String scoreMath(Stock stock, User user);
}
