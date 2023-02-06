package ui_tests.src.test.java.Classes.user;

public class User {
    private SecondProperties secondProperties;

    public SecondProperties getSecondProperties() {
        return secondProperties;
    }

    public User setSecondProperties(SecondProperties secondProperties) {
        this.secondProperties = secondProperties;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        if (secondProperties != null) {
            if (!secondProperties.equals(user.secondProperties)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public String toString() {
        return "User{" +
                "secondProperties=" + secondProperties +
                '}';
    }
}
