package ru.yandex.autotests.direct.httpclient.data.newclient;

import ru.yandex.autotests.direct.httpclient.core.AbstractFormParameters;
import ru.yandex.autotests.httpclient.lite.core.FormParameter;

/**
 * @author : Alex Samokhin (alex-samo@yandex-team.ru)
 *         Date: 05.11.14
 */
public class AjaxValidateLoginParameters extends AbstractFormParameters {

    @FormParameter("login")
    private String login;

    @FormParameter("track_id")
    private String trackId;

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public String getTrackId() {
        return trackId;
    }

    public void setTrackId(String trackId) {
        this.trackId = trackId;
    }
}
