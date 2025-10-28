package socialnetwork.Util;

import socialnetwork.Model.Profile;

public class Suggestion implements Comparable<Suggestion> {
    private int friendshipLevel;
    private int ageDiff;
    private Profile profile;

    public Suggestion(int friendshipLevel, Profile profile, Profile suggestionsReceiver) {
        this.friendshipLevel = friendshipLevel;
        this.ageDiff = Math.abs(profile.getAge() - suggestionsReceiver.getAge());
        this.profile = profile;
    }

    public int getFriendshipLevel() {
        return this.friendshipLevel;
    }

    public int getAgeDiff() {
        return this.ageDiff;
    }

    public Profile getProfile() {
        return this.profile;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) return true;

        if (other == null || other.getClass() != this.getClass()) return false;

        Suggestion otherSuggestion = (Suggestion) other;
        return this.getProfile().getProfileID().equals(otherSuggestion.getProfile().getProfileID());
    }

    @Override
    public int compareTo(Suggestion other) {
        // Sort by friendship level. The higher the level, the
        // higher the priority
        int friendshipLevelDiff = Integer.compare(
            this.getFriendshipLevel(),
            other.getFriendshipLevel()
        );
        if (friendshipLevelDiff != 0) {
            return friendshipLevelDiff;
        }

        // Sort alphabetically. Adding this and using a heap queue is
        // equivalent to using insertion sort.
        int alphabeticalComparison = this.getProfile().getName().compareTo(other.getProfile().getName());
        if (alphabeticalComparison != 0) {
            return -alphabeticalComparison;
        }

        // Sort by age, the smaller the difference, the higher the
        // priority
        int ageComparison = -Integer.compare(
            this.getAgeDiff(),
            other.getAgeDiff()
        );
        if (ageComparison != 0) {
            return ageComparison;
        }

        return this.getProfile().getProfileID().compareTo(other.getProfile().getProfileID());
    }
}
