package socialnetwork.Service;

import java.util.UUID;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import socialnetwork.Model.Profile;
import socialnetwork.Model.Gender;
import socialnetwork.Util.ProfilesLoader;

public class ProfilesManager {
    private static final Logger logger = LogManager.getLogger(ProfilesManager.class);

    private Map<UUID, Profile> profiles;

    public ProfilesManager() {
        this.profiles = new HashMap<>();
    }

    public void addProfile(Profile profile) throws IllegalArgumentException {
        if (profiles.containsKey(profile.getProfileID())) {
            throw new IllegalArgumentException("Profile already exists");
        }
        profiles.put(profile.getProfileID(), profile);
    }

    public Profile getProfile(UUID profileID) throws IllegalArgumentException {
        if (!profiles.containsKey(profileID)) {
            throw new IllegalArgumentException("Profile does not exist");
        }
        return profiles.get(profileID);
    }

    public void removeProfile(UUID profileID) throws IllegalArgumentException {
        Map<UUID, Integer> friends = getProfile(profileID).getFriends();
        for (UUID friendID : friends.keySet()) {
            try {
                getProfile(friendID).removeFriend(profileID);
            } catch (IllegalArgumentException e) {
                logger.warn("Error removing friend " + profileID + " from profile " + friendID + ": " + e.getMessage(), e);
                continue;
            }
        }

        profiles.remove(profileID);
    }

    /*
    Returns true if profiles were connected, false otherwise
    */
    public boolean connectProfiles(UUID profileID1, UUID profileID2, int friendshipLevel) {
        Profile profile1, profile2;
        try {
            profile1 = getProfile(profileID1);
            profile2 = getProfile(profileID2);
        }
        catch (IllegalArgumentException e) {
            logger.warn("Error connecting profiles " + profileID1 + " and " + profileID2 + ": " + e.getMessage(), e);
            return false;
        }

        try{
            profile1.addFriend(profileID2, friendshipLevel);
            profile2.addFriend(profileID1, friendshipLevel);
        }
        catch (IllegalArgumentException e) {
            logger.warn("Error connecting profiles " + profileID1 + " and " + profileID2 + ": " + e.getMessage(), e);
            cleanupConnectionAttempt(profile1, profile2);
            return false;
        }

        return true;
    }

    /*
    Returns true if profiles were disconnected, false otherwise
    */
    public boolean disconnectProfiles(UUID profileID1, UUID profileID2) {
        Profile profile1, profile2;
        try {
            profile1 = getProfile(profileID1);
            profile2 = getProfile(profileID2);
        }
        catch (IllegalArgumentException e) {
            logger.warn("Error disconnecting profiles " + profileID1 + " and " + profileID2 + ": " + e.getMessage(), e);
            return false;
        }

        Integer friendshipLevel = 0;
        try{
            friendshipLevel = profile1.removeFriend(profileID2);
            friendshipLevel = profile2.removeFriend(profileID1);
        }
        catch (IllegalArgumentException e) {
            logger.warn("Error disconnecting profiles " + profileID1 + " and " + profileID2 + ": " + e.getMessage(), e);
            cleanupDisconnectionAttempt(profile1, profile2, friendshipLevel);
            return false;
        }

        return true;
    }

    public UUID[] getProfilesIDs() {
        return profiles.keySet().toArray(new UUID[0]);
        
    }

    public void loadProfiles(String filename) {
        try {
            ProfilesLoader loader = new ProfilesLoader();
            profiles = loader.loadProfilesFromCSV(filename);
        } catch (Exception e) {
            logger.error("Error loading profiles from file " + filename + ": " + e.getMessage(), e);
            throw new RuntimeException("Failed to load profiles from file", e);
        }
    }

    public List<Profile> suggestConnections(UUID profileID) {
        return suggestConnections(profileID, 100);
    }

    public List<Profile> suggestConnections(UUID profileID, Integer maxSuggestions) {
        return suggestConnections(profileID, maxSuggestions, null, null, null);
    }

    public List<Profile> suggestConnections(
        UUID profileID,
        Integer maxSuggestions,
        String genderFilter,
        Integer minAgeFilter,
        Integer maxAgeFilter
    ) {
        Gender gender = null;
        if (genderFilter != null) {
            gender = Gender.fromString(genderFilter);
        }

        if (maxSuggestions == null) {
            maxSuggestions = 100;
        }

        if (minAgeFilter == null) {
            minAgeFilter = 0;
        }

        if (maxAgeFilter == null) {
            maxAgeFilter = 1000;
        }
        return SuggestionsEngine.getSuggestions(profileID, maxSuggestions, gender, minAgeFilter, maxAgeFilter, Map.copyOf(profiles));
    }

    private void cleanupConnectionAttempt(Profile profile1, Profile profile2) {
        try {
            profile1.removeFriend(profile2.getProfileID());
        }
        catch (IllegalArgumentException e) {}

        try {
            profile2.removeFriend(profile1.getProfileID());
        }
        catch (IllegalArgumentException e) {}
    }

    private void cleanupDisconnectionAttempt(Profile profile1, Profile profile2, Integer friendshipLevel) {
        try {
            profile1.addFriend(profile2.getProfileID(), friendshipLevel);
        }
        catch (IllegalArgumentException e) {}

        try {
            profile2.addFriend(profile1.getProfileID(), friendshipLevel);
        }
        catch (IllegalArgumentException e) {}
    }

}
