package ru.yandex.market.checkout.checkouter.order.edit;

import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.checkouter.order.changerequest.AvailableOptionType;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PossibleOrderOptionTest {

    @Test
    public void testToString() {
        final PossibleOrderOption orderOption = new PossibleOrderOption(
                AvailableOptionType.OPEN_PICKUP_TERMINAL, 111, 222);
        assertEquals("PossibleOrderOption{availableOptionType='OPEN_PICKUP_TERMINAL', " +
                        "checkpointStatusFrom=111, " +
                        "checkpointStatusTo=222}",
                orderOption.toString());
    }
}
