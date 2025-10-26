package socialnetwork.Model;

public enum Gender {
    MALE,
    FEMALE,
    NON_BINARY;

    public static Gender fromString(String gender) {
        return Gender.valueOf(gender.toUpperCase());
    }
}
