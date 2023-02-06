package ru.yandex.market.wms.common.model.enums;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


public class OrderStatusTest {

    @Test
    public void nullable() {
        OrderStatus nullable = OrderStatus.nullable("9");
        OrderStatus nullableWithZero = OrderStatus.nullable("09");
        assertEquals(nullable, OrderStatus.NOT_STARTED);
        assertEquals(nullable, nullableWithZero);
    }

    @Test
    public void optional() {
        Optional<OrderStatus> optional = OrderStatus.optional("6");
        Optional<OrderStatus> optionalWithZero = OrderStatus.optional("0006");
        assertTrue(optional.isPresent());
        assertTrue(optionalWithZero.isPresent());
        assertEquals(optional.get(), OrderStatus.DID_NOT_ALLOCATE);
        assertEquals(optional.get(), optionalWithZero.get());
    }

    @Test
    public void of() {
        OrderStatus zero1 = OrderStatus.of("000");
        OrderStatus zero2 = OrderStatus.of("0");
        assertEquals(zero1, OrderStatus.EMPTY_ORDER);
        assertEquals(zero1, zero2);
    }
}
