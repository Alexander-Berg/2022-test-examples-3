package ru.yandex.autotests.httpclient.metabeanprocessor.beans;

import ru.yandex.autotests.direct.httpclient.util.jsonParser.JsonPath;

/**
 * @author : Alex Samokhin (alex-samo@yandex-team.ru)
 *         Date: 26.02.15
 */
public class FourthTestBean {

    @JsonPath(requestPath = "ee")
    private String email;

    public void setEmail(String email) {
        this.email = email;
    }

    public String getEmail() {
        return email;
    }

    public FourthTestBean(String email) {
        this.email = email;
    }

    public FourthTestBean() {
    }
}
