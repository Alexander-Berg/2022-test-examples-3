package ru.yandex.autotests.direct.httpclient.data.newclient;

import ru.yandex.autotests.direct.httpclient.core.AbstractFormParameters;
import ru.yandex.autotests.httpclient.lite.core.FormParameter;

/**
 * @author : Alex Samokhin (alex-samo@yandex-team.ru)
 *         Date: 06.11.14
 */
public class AjaxSuggestLoginParameters extends AbstractFormParameters {

    @FormParameter("firstname")
    private String firstName;

    @FormParameter("lastname")
    private String lastName;

    @FormParameter("login")
    private String login;

    @FormParameter("track_id")
    private String trackId;

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

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
