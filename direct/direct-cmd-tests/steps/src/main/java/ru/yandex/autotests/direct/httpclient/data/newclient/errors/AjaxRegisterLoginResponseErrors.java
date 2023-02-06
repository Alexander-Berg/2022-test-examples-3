package ru.yandex.autotests.direct.httpclient.data.newclient.errors;

/**
 * @author : Alex Samokhin (alex-samo@yandex-team.ru)
 *         Date: 10.11.14
 */
public enum AjaxRegisterLoginResponseErrors {

    INCORRECT_CAPTCHA("incorrectcaptcha"),
    LOGIN_NOT_AVAILABLE("login.notavailable"),
    PASSWORD_SHORT("password.short");

    private String error;

    AjaxRegisterLoginResponseErrors(String error) {
        this.error = error;
    }

    public String getError() {
        return error;
    }

    @Override
    public String toString() {
        return error;
    }

}
