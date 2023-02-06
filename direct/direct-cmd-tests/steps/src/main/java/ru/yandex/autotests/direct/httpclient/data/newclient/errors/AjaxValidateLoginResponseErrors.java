package ru.yandex.autotests.direct.httpclient.data.newclient.errors;

/**
 * @author : Alex Samokhin (alex-samo@yandex-team.ru)
 *         Date: 05.11.14
 */
public enum AjaxValidateLoginResponseErrors {

    STARTS_WITH_DIGIT,
    TOO_LONG,
    STARTS_WITH_DOT,
    STARTS_WITH_HYPHEN,
    ENDS_WITH_HYPHEN,
    DOUBLED_DOT,
    DOUBLED_HYPHEN,
    PROHIBITED_SYMBOLS,
    DOT_HYPHEN,
    HYPHEN_DOT,
    ENDS_WITH_DOT,
    NOT_AVAILABLE,
    INVALID_PARAMS;

    @Override
    public String toString() {
        return name().toLowerCase().replace("_", "");
    }

}
