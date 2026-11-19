package glassfolio.model;

/**
 * A person using the app, with their budget and chosen risk profile.
 */
public class User {
    /** Risk profile number for careful investors. */
    public static final int STABILITY_PROFILE = 1;
    /** Risk profile number for growth investors. */
    public static final int GROWTH_PROFILE = 2;
    /** The longest name the app accepts. */
    public static final int MAX_NAME_LENGTH = 60;

    private String username;
    private String password; // reserved for a future sign-in; the form passes an empty one today
    private double budget;
    private int riskProfile;

    /**
     * @param username    the user's name
     * @param password    reserved for a future sign-in; empty for now
     * @param budget      the amount to invest, above 0
     * @param riskProfile STABILITY_PROFILE or GROWTH_PROFILE
     * @throws IllegalArgumentException if the name, budget or riskProfile is invalid
     */
    public User(String username, String password, double budget, int riskProfile) {
        setUsername(username);
        this.password = password;
        setBudget(budget);
        setRiskProfile(riskProfile);
    }
    /** @return the user's name */
    public String getUsername(){
        return username;
    }
    /** @return the user's password */
    public String getPassword(){
        return password;
    }
    /** @return the amount to invest */
    public double getBudget(){
        return budget;
    }
    /** @return STABILITY_PROFILE or GROWTH_PROFILE */
    public int getRiskProfile(){
        return riskProfile;
    }
    /**
     * @param username the new name, 1 to {@value #MAX_NAME_LENGTH} characters
     * @throws IllegalArgumentException if the name is blank or too long
     */
    public final void setUsername(String username){
        if (username == null || username.isBlank() || username.length() > MAX_NAME_LENGTH) {
            throw new IllegalArgumentException("Enter a name of 1 to " + MAX_NAME_LENGTH + " characters.");
        }
        this.username = username;
    }
    /**
     * @param password the new password, empty if none
     * @throws IllegalArgumentException if the password is null
     */
    public void setPassword(String password){
        if (password == null) {
            throw new IllegalArgumentException("Password cannot be null");
        }
        this.password = password;
    }
    /**
     * @param budget the amount to invest, above 0
     * @throws IllegalArgumentException if budget is 0 or less, or not a real number
     */
    public final void setBudget(double budget){
        if (!(budget > 0) || !Double.isFinite(budget)) {
            throw new IllegalArgumentException("Budget must be above 0");
        }
        this.budget = budget;
    }
    /**
     * @param riskProfile STABILITY_PROFILE or GROWTH_PROFILE
     * @throws IllegalArgumentException for any other number
     */
    public final void setRiskProfile(int riskProfile){
        if (riskProfile != STABILITY_PROFILE && riskProfile != GROWTH_PROFILE) {
            throw new IllegalArgumentException("Risk profile must be 1 or 2");
        }
        this.riskProfile = riskProfile;
    }
}
