package socialnetwork.Util;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import socialnetwork.Model.Profile;

public class ProfilesLoader {
    private static final Logger logger = LogManager.getLogger(ProfilesLoader.class);

    private static final String profileIDFieldName = "profile_id";
    private static final String nameFieldName = "name";
    private static final String ageFieldName = "age";
    private static final String genderFieldName = "gender";
    private static final String friendsFieldName = "friends";

    private static final String[] requiredFields = {profileIDFieldName, nameFieldName, ageFieldName, genderFieldName, friendsFieldName};

    private List<String> fields;
    private Map<String, Integer> requiredFieldsToIndexMap;

    public ProfilesLoader() {
        fields = new ArrayList<>();
        requiredFieldsToIndexMap = new HashMap<>();
    }

    public Map<UUID, Profile> loadProfilesFromCSV(String filename) throws IOException {
        return loadProfilesFromCSV(filename, 1);
    }

    public Map<UUID, Profile> loadProfilesFromCSV(String filename, int headerLine) throws IOException {
        Profile profile;
        Map<UUID, Profile> profiles = new HashMap<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            String line;
            
            line = reader.readLine();
            for (int i = 1; i < headerLine; i++) {
                line = reader.readLine();
            }

            line = line.trim();
            setFieldsFromLine(line);
            setRequiredFieldsToIndexMap();
            
            List<String> parts;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) {
                    continue;
                }

                parts = parseLine(line);
                if (parts.size() != fields.size()) {
                    throw new IllegalArgumentException("Invalid CSV file structure. Line with a different number of fields than the header found.");
                }

                profile = createProfileFromLineParts(parts);
                logger.info("profile: " + profile + " successfully loaded.");

                profiles.put(profile.getProfileID(), profile);
            }
        }
        
        cleanWrongRelationships(profiles);

        return profiles;
    }

    private void setFieldsFromLine(String line) {
        List<String> tmpFields = parseLine(line);
        boolean valid = checkRequiredFields(tmpFields);
        if (!valid) {
            throw new IllegalArgumentException("The CSV file is missing some required fields.");
        }

        fields = tmpFields;
    }

    private static boolean checkRequiredFields(List<String> fields) {
        for (String field : requiredFields) {
            if (!fields.contains(field)) {
                logger.error("Missing required field: " + field + " fields: " + fields);
                return false;
            }
        }
        return true;
    }

    private void setRequiredFieldsToIndexMap() {
        Map<String, Integer> indexMap = new HashMap<>();
        for (String field : requiredFields) {
            indexMap.put(field, getFieldIndex(fields, field));
        }
        
        requiredFieldsToIndexMap = indexMap;
    }

    private static int getFieldIndex(List<String> fields, String fieldName) {
        for (int i = 0; i < fields.size(); i++) {
            if (fields.get(i).equals(fieldName)) {
                return i;
            }
        }
        return -1;
    }

    private Profile createProfileFromLineParts(List<String> parts) {
        int age = Integer.parseInt(parts.get(
            requiredFieldsToIndexMap.get(ageFieldName)
        ));
        UUID profileID = UUID.fromString(parts.get(
            requiredFieldsToIndexMap.get(profileIDFieldName)
        ));
        String name = parts.get(
            requiredFieldsToIndexMap.get(nameFieldName)
        );
        String gender = parts.get(
            requiredFieldsToIndexMap.get(genderFieldName)
        );
        Map<UUID, Integer> friends = parseFriendshipData(parts, profileID);

        return new Profile(profileID, name, age, gender, friends);
    }

    private Map<UUID, Integer> parseFriendshipData(List<String> parts, UUID profileID) {
        List<String> friendshipData = parseLine(parts.get(requiredFieldsToIndexMap.get(friendsFieldName)));

        String[] friendshipParts;
        Map<UUID, Integer> friends = new HashMap<>();
        for (String friendship : friendshipData) {
            if (friendship == null || friendship.isEmpty()) {
                continue;
            }

            friendshipParts = friendship.split(":");
            if (friendshipParts.length != 2) {
                logger.warn("Invalid friendship data format: " + friendship);
                continue;
            }

            try {
                UUID friendID = UUID.fromString(friendshipParts[0]);
                int friendshipLevel = Integer.parseInt(friendshipParts[1]);
                
                if (friends.containsKey(friendID)) {
                    logger.warn("Friendship relationship already exists for " + profileID + " and " + friendID);
                }
                friends.put(friendID, friendshipLevel);
            } catch (Exception e) {
                logger.error("Error adding friend to profile " + profileID + ": " + e.getMessage(), e);
            }
        }

        return friends;
    }

    private void cleanWrongRelationships(Map<UUID, Profile> profiles) {
        for (Profile profile : profiles.values()) {
            Map<UUID, Integer> friends = profile.getFriends();
            for (Map.Entry<UUID, Integer> entry : friends.entrySet()) {
                UUID friendID = entry.getKey();
                
                // Remove friendships with profiles that don't exist
                if (!profiles.containsKey(friendID)) {
                    logger.warn("Profile " + profile.getProfileID() + " has a friendship relationship with a profile that doesn't exist: " + friendID);
                    profile.removeFriend(friendID);
                    continue;
                }

                // Remove unidirectional friendships
                if (!profiles.get(friendID).getFriends().containsKey(profile.getProfileID())) {
                    logger.warn("Profile " + profile.getProfileID() + " has an unidirectional friendship relationship with profile " + friendID);
                    profile.removeFriend(friendID);
                    continue;
                }

                // Remove self friendships
                if (profile.getProfileID().equals(friendID)) {
                    logger.warn("Profile " + profile.getProfileID() + " has a self friendship relationship with itself");
                    profile.removeFriend(friendID);
                    continue;
                }

                // Remove friendships with different friendship levels
                if (!entry.getValue().equals(profiles.get(friendID).getFriends().get(profile.getProfileID()))) {
                    logger.warn("Profile " + profile.getProfileID() + " has an asymmetrical friendship relationship with profile " + friendID);
                    profile.removeFriend(friendID);
                    continue;
                }
            }
        }
    }
    
    // this is ok now
    private static List<String> parseLine(String line) {
        List<String> fields = new ArrayList<>();
        
        StringBuilder currentField = new StringBuilder();
        boolean inQuotes = false;
        
        char c;

        for (int i = 0; i < line.length(); i++) {
            c = line.charAt(i);

            // If the delimiter is found and we are not in quotes,
            // add the current field to the list and start a new field
            if (c == ',' && !inQuotes) {
                fields.add(currentField.toString().trim());
                currentField = currentField.delete(0, currentField.length());
                continue;
            }

            // If the quote character is found, toggle the quotes flag
            if (c == '"') {
                inQuotes = !inQuotes;
                continue;
            }

            // In any other case, append the character to the current
            // field
            currentField.append(c);
        }
        
        fields.add(currentField.toString().trim());
        logger.debug("Line " + line + " parsed as " + fields);

        return fields;
    }
}
