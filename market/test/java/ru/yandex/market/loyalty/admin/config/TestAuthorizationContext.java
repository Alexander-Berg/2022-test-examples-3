package ru.yandex.market.loyalty.admin.config;

public class TestAuthorizationContext implements AuthorizationContext {
    public static final String DEFAULT_USER_NAME = "test";
    private volatile String userName = null;

    @Override
    public String getAppUserLogin() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }
}
