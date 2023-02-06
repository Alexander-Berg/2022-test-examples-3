package ru.yandex.market.abo.core.ticket.order;

import java.util.Arrays;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.EmptyTest;
import ru.yandex.market.abo.clch.model.PhoneNumber;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author artemmz
 * @date 10.12.17.
 */
class OrderPhoneManagerTest extends EmptyTest {
    @Autowired
    private OrderPhoneManager orderPhoneManager;

    @Test
    void testDao() {
        var shopsWithPhone = orderPhoneManager.getShopsWithTicketsContaining("42");
        assertNotNull(shopsWithPhone);
    }

    @Test
    void removePhoneFromTickets() {
        orderPhoneManager.removePhoneFromTickets(1, new PhoneNumber("499", "111-22-33"),
                Arrays.stream(OrderPhoneManager.RemoveCondition.values())
                        .map(Enum::name).map(String::toLowerCase)
                        .collect(Collectors.joining())
        );
    }
}
