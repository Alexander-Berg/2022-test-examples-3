package ru.yandex.market.core.testing;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;

@XmlEnum
public enum PremoderationResultStatus {

    @XmlEnumValue("0")
    PASSED(0),

    @XmlEnumValue("1")
    FAILED(1),

    @XmlEnumValue("2")
    NEED_INFO(2);

    private final int id;

    PremoderationResultStatus(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }
}
