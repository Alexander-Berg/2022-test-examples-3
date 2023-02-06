package ru.yandex.autotests.direct.httpclient.data;

/**
 * @author : Alex Samokhin (alex-samo@yandex-team.ru)
 *         Date: 19.09.14
 */

//properties of responses
public enum Responses {
    SUCCESS("success"),
    OK("ok"),
    RESULT("result"),
    STATUS("status"),
    CODE("code"),
    ERROR("error"),
    ERROR_TYPE("error_type"),
    ERROR_NO("error_no"),
    ERROR_CODE("error_code"),
    ESTIMATE("estimate"),
    CMD("cmd"),
    PROBLEM("problem"),
    CAMPAIGN_ERRORS("campaign/errors");

    public static final String PATH = "$.";

    Responses(String name) {
        this.name = name;
    }

    private String name;

    public String getName() {
        return name;
    }

    public String getPath() {
        return PATH + name;
    }
}
