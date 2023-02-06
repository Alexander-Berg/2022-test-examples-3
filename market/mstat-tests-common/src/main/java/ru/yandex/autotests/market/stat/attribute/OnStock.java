package ru.yandex.autotests.market.stat.attribute;

/**
 * User: jkt
 * Date: 05.07.13
 * Time: 16:08
 */
public enum OnStock {

    _0, _1;

    String code;

    OnStock() {
        this.code = name().replace("_", "");
    }

    public String getCode() {
        return code;
    }
}
