package socialnetwork.Service;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.PriorityQueue;
import java.util.UUID;

import socialnetwork.Model.Profile;
import socialnetwork.Model.Gender;
import socialnetwork.Util.Suggestion;


public class SuggestionsEngine {
    private static Map<UUID, Profile> profiles;

    private static void setProfiles(Map<UUID, Profile> profiles) {
        SuggestionsEngine.profiles = profiles;
    }

    public static List<Profile> getSuggestions(
        UUID profileID,
        int maxSuggestions,
        Gender genderFilter,
        int minAgeFilter,
        int maxAgeFilter,
        Map<UUID, Profile> profiles
    ) {
        setProfiles(profiles);
        PriorityQueue<Suggestion> suggestions = loadSuggestions(profileID);

        Profile profile = null;
        List<Profile> suggestionsList = new ArrayList<>();
        while (!suggestions.isEmpty()) {
            if (suggestionsList.size() >= maxSuggestions) {
                break;
            }

            profile = suggestions.poll().getProfile();
            if (genderFilter != null && !profile.getGender().equals(genderFilter)) {
                continue;
            }

            if (profile.getAge() < minAgeFilter || profile.getAge() > maxAgeFilter) {
                continue;
            }

            suggestionsList.add(profile);
        }

        return suggestionsList;
    }

    private static Map<UUID, Integer> getBaseSuggestions(UUID profileID) {
        Profile profile = profiles.get(profileID);

        Map<UUID, Integer> friends = profile.getFriends();
        
        Map<UUID, Integer> fofs = new HashMap<>();
        Map<UUID, Integer> suggestionsMap = new HashMap<>();

        UUID friendID;
        Integer friendshipLevel;
        Integer currentFriendshipLevel;
        for (Map.Entry<UUID, Integer> friend : friends.entrySet()) {
            friendID = friend.getKey();
            friendshipLevel = friend.getValue();

            fofs = profiles.get(friendID).getFriends();
            for (UUID fofID : fofs.keySet()) {
                // Skip itself
                if (fofID.equals(profileID)) {
                    continue;
                }

                // Skip friends
                if (friends.containsKey(fofID)) {
                    continue;
                }

                if (suggestionsMap.containsKey(fofID)) {
                    currentFriendshipLevel = suggestionsMap.get(fofID);
                    if (friendshipLevel > currentFriendshipLevel) {
                        suggestionsMap.put(fofID, friendshipLevel);
                    }
                    continue;
                }

                suggestionsMap.put(fofID, friendshipLevel);   
            }
        }
    
        return suggestionsMap;
    }

    private static PriorityQueue<Suggestion> loadSuggestions(UUID profileID) {
        PriorityQueue<Suggestion> suggestions = new PriorityQueue<>(Collections.reverseOrder());
        Map<UUID, Integer> suggestionsMap = getBaseSuggestions(profileID);
        
        Suggestion suggestionObj = null;
        Profile profile = null;
        Profile suggestionsReceiver = profiles.get(profileID);

        for (Map.Entry<UUID, Integer> entry : suggestionsMap.entrySet()) {
            profile = profiles.get(entry.getKey());
            suggestionObj = new Suggestion(entry.getValue(), profile, suggestionsReceiver);
            suggestions.add(suggestionObj);
        }

        return suggestions;
    }
}
