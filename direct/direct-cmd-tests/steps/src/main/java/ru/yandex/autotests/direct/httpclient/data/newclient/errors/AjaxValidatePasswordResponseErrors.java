package ru.yandex.autotests.direct.httpclient.data.newclient.errors;

/**
 * @author : Alex Samokhin (alex-samo@yandex-team.ru)
 *         Date: 06.11.14
 */
public enum AjaxValidatePasswordResponseErrors {

    TOO_SHORT,
    PROHIBITED_SYMBOLS,
    TOO_LONG,
    LIKE_LOGIN,
    LIKE_OLD_PASSWORD,
    FOUND_IN_HISTORY,
    LIKE_PHONE_NUMBER,
    WEAK,
    INVALID_PARAMS;

    @Override
    public String toString() {
        return name().toLowerCase().replace("_", "");
    }
}
