package ru.yandex.market.yql_test.proxy;

public class YqlResponseWrapper {

    private final String response;
    private final String id;
    private final String status;
    private final boolean hasData;

    public YqlResponseWrapper(String response, String id, String status, boolean hasData) {
        this.response = response;
        this.id = id;
        this.status = status;
        this.hasData = hasData;
    }

    public String getResponse() {
        return response;
    }

    public String getId() {
        return id;
    }

    public String getStatus() {
        return status;
    }

    public boolean isHasData() {
        return hasData;
    }
}
