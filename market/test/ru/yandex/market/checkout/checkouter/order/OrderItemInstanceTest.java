package ru.yandex.market.checkout.checkouter.order;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OrderItemInstanceTest {

    @Test
    @DisplayName("Проверить уникальность наименования типа")
    void testUniquenessOfInstanceTypeName() {
        var names = Stream.of(OrderItemInstance.InstanceType.values())
                .map(OrderItemInstance.InstanceType::getName)
                .collect(Collectors.toList());

        assertEquals(names.size(), Set.copyOf(names).size());
    }

}
