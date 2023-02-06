package ru.yandex.autotests.market.stat.attribute;

/**
 * User: jkt
 * Date: 05.07.13
 * Time: 16:08
 */
public enum AutoBrockerEnabled {

    TRUE("1"), FALSE("0"), NONE(""), ABSENT(null);

    String code;

    AutoBrockerEnabled(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
