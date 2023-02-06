package ru.yandex.market.checkout.checkouter.order.changerequest;

import java.io.IOException;
import java.util.Collections;
import java.util.Set;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.checkouter.order.edit.customerinfo.DeliveryServiceSubtype;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

public class OrderOptionAvailabilityTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Test
    public void shouldNotReturnNullForOptionsWhenConstructedByDefault() {
        OrderOptionAvailability optionAvailability = new OrderOptionAvailability(1L, Collections.emptySet(), make());
        assertNotNull(optionAvailability.getAvailableOptions());
        assertNotNull(optionAvailability.getDeliveryServiceCustomerInfo());
    }

    @Test
    public void shouldNotFailWhenSettingNullDeliveryServiceInfo() {
        OrderOptionAvailability optionAvailability = new OrderOptionAvailability(1L, Collections.emptySet(), null);
        assertNotNull(optionAvailability.getAvailableOptions());
        assertNull(optionAvailability.getDeliveryServiceCustomerInfo());
    }

    @Test
    public void shouldFailWhenConstructedWithNullOptions() {
        Assertions.assertThrows(NullPointerException.class, () -> {
            new OrderOptionAvailability(1L, null, null);
        });
    }

    @Test
    public void toStringTest() {
        OrderOptionAvailability optionAvailability = create(AvailableOptionType.OPEN_PICKUP_TERMINAL);
        assertEquals("OrderOptionAvailability{orderId=1, " +
                        "availableOptions=[OptionAvailability{availableOptionType=OPEN_PICKUP_TERMINAL}], " +
                        "deliveryServiceCustomerInfo=DeliveryServiceCustomerInfo{name='Везу', phones=[], " +
                        "trackOrderSite='http://vezu.ru', trackOrderSource=DS_TRACK_CODE, subtype=CONTRACT_COURIER}}",
                optionAvailability.toString());
    }

    @Test
    public void toJsonTest() throws IOException {
        final OrderOptionAvailability original = create(AvailableOptionType.SHOW_RUNNING_COURIER);
        final String json = OBJECT_MAPPER.writeValueAsString(original);
        assertEquals("{\"orderId\":1,\"availabilities\":[{\"type\":\"SHOW_RUNNING_COURIER\"}]," +
                "\"contacts\":{\"name\":\"Везу\",\"phones\":[]," +
                "\"trackOrderSite\":\"http://vezu.ru\",\"trackCodeSource\":\"DS_TRACK_CODE\"," +
                "\"subtype\":\"CONTRACT_COURIER\"}}", json);
        final OrderOptionAvailability fromJson = OBJECT_MAPPER.readValue(json, OrderOptionAvailability.class);
        assertNotNull(fromJson);
        assertEquals(original, fromJson);
    }

    private DeliveryServiceCustomerInfo make() {
        return new DeliveryServiceCustomerInfo(
                "Везу", Collections.emptyList(), "http://vezu.ru", TrackOrderSource.DS_TRACK_CODE,
                DeliveryServiceSubtype.CONTRACT_COURIER);
    }

    private OrderOptionAvailability create(AvailableOptionType availableOptionType) {
        final Set<OptionAvailability> availabilities = Collections.singleton(
                new OptionAvailability(availableOptionType));
        return new OrderOptionAvailability(1L, availabilities, make());
    }
}
