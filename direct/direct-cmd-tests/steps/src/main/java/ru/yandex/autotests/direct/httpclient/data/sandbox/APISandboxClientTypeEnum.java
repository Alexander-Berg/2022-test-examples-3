package ru.yandex.autotests.direct.httpclient.data.sandbox;

/**
 * Created by proxeter (Nikolay Mulyar - proxeter@yandex-team.ru) on 19.05.2014.
 */
public enum APISandboxClientTypeEnum {

    CLIENT("client"),
    AGENCY("agency");

    private String clientType;

    APISandboxClientTypeEnum(String clientType) {
        this.clientType = clientType;
    }

    public String getClientType() {
        return clientType;
    }

}
