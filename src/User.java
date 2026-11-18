/**
 * A person using the app, with their budget and chosen risk profile.
 */
public class User {
    /** Risk profile number for careful investors. */
    public static final int STABILITY_PROFILE = 1;
    /** Risk profile number for growth investors. */
    public static final int GROWTH_PROFILE = 2;

    private String username;
    private String password; // reserved for a future sign-in; the form passes an empty one today
    private double budget;
    private int riskProfile;

    /**
     * @param username    the user's name
     * @param password    reserved for a future sign-in; empty for now
     * @param budget      the amount to invest, above 0
     * @param riskProfile STABILITY_PROFILE or GROWTH_PROFILE
     * @throws IllegalArgumentException if budget or riskProfile is invalid
     */
    public User(String username, String password, double budget, int riskProfile) {
        this.username = username;
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
    /** @param username the new name */
    public void setUsername(String username){
        this.username = username;
    }
    /** @param password the new password */
    public void setPassword(String password){
        this.password = password;
    }
    /**
     * @param budget the amount to invest, above 0
     * @throws IllegalArgumentException if budget is 0 or less
     */
    public final void setBudget(double budget){
        if (budget <= 0) {
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
