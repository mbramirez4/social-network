package socialnetwork.Model;

import java.util.Map;
import java.util.HashMap;
import java.util.UUID;

public class Profile {
    private final UUID profileID;
    private final String name;
    private int age;
    private Gender gender;
    private Map<UUID, Integer> friends;

    public Profile(
        UUID profileID,
        String name,
        int age,
        String gender,
        Map<UUID, Integer> friends
    ) throws IllegalArgumentException {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Name cannot be null or empty");
        }
        if (age < 0) {
            throw new IllegalArgumentException("Age cannot be negative");
        }
        if (gender == null || gender.isEmpty()) {
            throw new IllegalArgumentException("Gender cannot be null or empty");
        }
        
        Gender genderEnum = Gender.fromString(gender);
        if (genderEnum == null) {
            throw new IllegalArgumentException("Invalid gender");
        }

        if (profileID == null) {
            profileID = UUID.randomUUID();
        }

        if (friends == null) {
            friends = new HashMap<>();
        }

        this.profileID = profileID;
        this.name = name;
        this.age = age;
        this.gender = genderEnum;
        this.friends = new HashMap<>();
    }

    public Profile(String name, int age, String gender) throws IllegalArgumentException {
        this(UUID.randomUUID(), name, age, gender, null);
    }

    public UUID getProfileID() {
        return profileID;
    }

    public String getName() {
        return name;
    }

    public int getAge() {
        return age;
    }

    public Map<UUID, Integer> getFriends() {
        return new HashMap<>(friends); // Return a copy of the friends map to prevent modification
    }

    public void addFriend(UUID friendID, int friendshipLevel) throws IllegalArgumentException {
        if (friends.containsKey(friendID)) {
            throw new IllegalArgumentException("Frienship relationship already exists");
        }
        friends.put(friendID, friendshipLevel);
    }

    public void removeFriend(UUID friendID) throws IllegalArgumentException {
        if (!friends.containsKey(friendID)) {
            throw new IllegalArgumentException("Friendship relationship does not exist");
        }
        friends.remove(friendID);
    }
}
