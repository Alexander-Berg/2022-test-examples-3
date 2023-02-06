package ru.yandex.market.billing.sortingcenter.model;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;


class SCServiceTypeTest {

    @DisplayName("проверка просто по валью.")
    @Test
    void toValue() {
        assertEquals("Хранение", SCServiceType.ORDER_AWAITING_CLARIFICATION_FF.getReadableValue());
    }

    @DisplayName("дисириализация")
    @Test
    void fromValue() {
        assertFalse(SCServiceType.findByReadableValue("Такого типа точно нет").isPresent());
        assertEquals(
                Optional.of(SCServiceType.ORDER_SHIPPED_TO_SO_FF),
                SCServiceType.findByReadableValue("Отгрузка курьеру")
        );
    }

}
