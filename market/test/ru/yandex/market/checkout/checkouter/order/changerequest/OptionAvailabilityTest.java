package ru.yandex.market.checkout.checkouter.order.changerequest;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class OptionAvailabilityTest {

    @Test
    public void testToString() {
        final OptionAvailability optionAvailability = new OptionAvailability(AvailableOptionType.SHOW_RUNNING_COURIER);
        assertEquals("OptionAvailability{availableOptionType=SHOW_RUNNING_COURIER}",
                optionAvailability.toString());
    }
}
