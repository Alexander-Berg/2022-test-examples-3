package ru.yandex.autotests.direct.httpclient.data.newclient;

import ru.yandex.autotests.direct.httpclient.core.AbstractFormParameters;
import ru.yandex.autotests.httpclient.lite.core.FormParameter;

/**
 * @author : Alex Samokhin (alex-samo@yandex-team.ru)
 *         Date: 10.11.14
 */
public class AjaxRegisterLoginParameters extends AbstractFormParameters {

    @FormParameter("firstname")
    private String firstName;

    @FormParameter("language")
    private String language;

    @FormParameter("lastname")
    private String lastname;

    @FormParameter("login")
    private String login;

    @FormParameter("password")
    private String password;

    @FormParameter("track_id")
    private String trackId;

    @FormParameter("x_captcha_code")
    private String xCaptchaCode;

    @FormParameter("x_captcha_id")
    private String xCaptchaId;

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getLastname() {
        return lastname;
    }

    public void setLastname(String lastname) {
        this.lastname = lastname;
    }

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

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

    public String getxCaptchaCode() {
        return xCaptchaCode;
    }

    public void setxCaptchaCode(String xCaptchaCode) {
        this.xCaptchaCode = xCaptchaCode;
    }

    public String getxCaptchaId() {
        return xCaptchaId;
    }

    public void setxCaptchaId(String xCaptchaId) {
        this.xCaptchaId = xCaptchaId;
    }
}
