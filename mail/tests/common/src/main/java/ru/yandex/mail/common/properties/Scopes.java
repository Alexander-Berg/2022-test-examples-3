package ru.yandex.mail.common.properties;

public enum Scopes {
    PRODUCTION("production"),
    INTRANET_PRODUCTION("intranet-production"),
    TESTING("testing"),
    DEVPACK("devpack");

    private String name;

    Scopes(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

    public static Scopes from(String value) {
        for (Scopes scope : values()) {
            if (scope.name.equals(value)) {
                return scope;
            }
        }
        throw new EnumConstantNotPresentException(Scopes.class, value);
    }
}
