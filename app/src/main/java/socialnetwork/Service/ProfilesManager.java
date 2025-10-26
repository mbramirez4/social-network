package socialnetwork.Service;

import java.util.UUID;
import java.util.Map;
import java.util.HashMap;

import socialnetwork.Model.Profile;

public class ProfilesManager {
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
                profiles.get(friendID).removeFriend(profileID);
            } catch (IllegalArgumentException e) {
                System.err.println("Error removing friend: " + e.getMessage());
            }
        }
        
        profiles.remove(profileID);
    }

    public void loadProfiles(String filename) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

}
