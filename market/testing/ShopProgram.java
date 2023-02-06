package ru.yandex.market.core.testing;

import java.util.List;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;

import com.google.common.collect.ImmutableList;

/**
 * Программа магазина. Магазин может подключаться к програмам и отключаться по своему желанию. При подключении
 * отправляется на премодерацию.
 *
 * @author Vadim Lyalin
 */
@XmlEnum
public enum ShopProgram {

    /**
     * Общая для CPC и CPA.
     */
    @XmlEnumValue("GENERAL")
    GENERAL,

    /**
     * CPC.
     */
    @XmlEnumValue("CPC")
    CPC,

    /**
     * CPA.
     */
    @XmlEnumValue("CPA")
    CPA,

    /**
     * Самопроверка (выполнение сценариев АБО).
     */
    @XmlEnumValue("SELF_CHECK")
    SELF_CHECK,

    /**
     * Отладка АПИ.
     */
    @XmlEnumValue("API_DEBUG")
    API_DEBUG;

    /**
     * Набор программ размещения, по которым будет разрешена модерация, в случае разблокировки возможности отправиться
     * на нее.
     */
    public static final List<ShopProgram> RELEASED_CHECKS = ImmutableList.of(
            ShopProgram.CPA,
            ShopProgram.CPC,
            ShopProgram.GENERAL
    );

    public boolean isCpc() {
        return this == CPC;
    }

    public boolean isCpa() {
        return this == CPA;
    }

    public boolean isGeneral() {
        return this == GENERAL;
    }
}
