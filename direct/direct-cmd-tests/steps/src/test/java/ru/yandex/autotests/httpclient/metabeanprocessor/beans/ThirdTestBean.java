package ru.yandex.autotests.httpclient.metabeanprocessor.beans;

import ru.yandex.autotests.direct.httpclient.util.jsonParser.JsonPath;

/**
 * @author : Alex Samokhin (alex-samo@yandex-team.ru)
 *         Date: 28.01.15
 */
public class ThirdTestBean {

    @JsonPath(requestPath = "email")
    private FourthTestBean email;

    @JsonPath(requestPath = "select")
    private String select;

    public void setEmail(FourthTestBean email) {
        this.email = email;
    }

    public void setSelect(String select) {
        this.select = select;
    }

    public FourthTestBean getEmail() {
        return email;
    }

    public String getSelect() {
        return select;
    }

    public ThirdTestBean(FourthTestBean email, String select) {
        this.email = email;
        this.select = select;
    }

    public ThirdTestBean() {
    }
}
