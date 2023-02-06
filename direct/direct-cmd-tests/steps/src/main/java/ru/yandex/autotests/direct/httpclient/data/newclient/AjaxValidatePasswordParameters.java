package ru.yandex.autotests.direct.httpclient.data.newclient;

import ru.yandex.autotests.direct.httpclient.core.AbstractFormParameters;
import ru.yandex.autotests.httpclient.lite.core.FormParameter;

/**
 * @author : Alex Samokhin (alex-samo@yandex-team.ru)
 *         Date: 06.11.14
 */
public class AjaxValidatePasswordParameters extends AbstractFormParameters {

    @FormParameter("password")
    private String password;

    @FormParameter("track_id")
    private String trackId;

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getTrackId() {
        return trackId;
    }

    public void setTrackId(String trackId) {
        this.trackId = trackId;
    }
}
