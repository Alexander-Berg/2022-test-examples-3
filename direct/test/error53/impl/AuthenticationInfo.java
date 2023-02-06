package ru.yandex.autotests.directapi.test.error53.impl;

public final class AuthenticationInfo {
    private String operator;
    private String clientLogin;
    private String apiToken;

    @Override
    public String toString() {
        return "AuthenticationInfo{" +
                "operator='" + operator + '\'' +
                ", clientLogin='" + clientLogin + '\'' +
                ", apiToken='" + apiToken + '\'' +
                '}';
    }

    public String getOperator() {
        return operator;
    }

    public AuthenticationInfo withOperator(String operator) {
        this.operator = operator;
        return this;
    }

    public String getClientLogin() {
        return clientLogin;
    }

    public AuthenticationInfo withClientLogin(String clientLogin) {
        this.clientLogin = clientLogin;
        return this;
    }

    public String getApiToken() {
        return apiToken;
    }

    public AuthenticationInfo withApiToken(String apiToken) {
        this.apiToken = apiToken;
        return this;
    }
}
