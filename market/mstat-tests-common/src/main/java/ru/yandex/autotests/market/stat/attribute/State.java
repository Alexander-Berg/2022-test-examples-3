package ru.yandex.autotests.market.stat.attribute;

/**
 * User: jkt
 * Date: 05.07.13
 * Time: 16:08
 */
public enum State {

    _1, _3, _0;

    String code;

    State() {
        this.code = name().replace("_", "");
    }

    public String getCode() {
        return code;
    }

}
