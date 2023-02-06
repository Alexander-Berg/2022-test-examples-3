package ru.yandex.autotests.market.stat.attribute;

/**
 * Created by jkt on 08.08.14.
 */
public enum UserType {

    EXTERNAL(0),
    YANDEX_USER(1);

    private Integer mask;

    UserType(Integer mask) {
        this.mask = mask;
    }

    public Integer mask() {
        return mask;
    }
}
