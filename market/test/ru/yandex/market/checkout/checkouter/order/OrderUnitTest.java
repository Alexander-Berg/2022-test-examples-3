package ru.yandex.market.checkout.checkouter.order;

import java.util.Collections;

import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.checkouter.delivery.Address;
import ru.yandex.market.checkout.checkouter.delivery.AddressImpl;
import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.delivery.outlet.ShopOutlet;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasJsonPath;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class OrderUnitTest {

    /**
     * Пустой order должен сериализовываться без ошибок
     */
    @Test
    public void toStringEmpty() {
        // Arrange
        final Order order = new Order();

        // Act
        final String result = order.toString();

        // Assert
        assertFalse(result.contains("JsonLoggingError"), result);
        assertThat(result, hasJsonPath("$.allowedForFullfilment", is(false)));

        assertFalse(result.contains("itemsMap"), result);
        assertThat(result, hasJsonPath("$.items", nullValue()));
        assertThat(result, hasJsonPath("$.delivery", nullValue()));
    }

    @Test
    public void toStringWithEmptyItem() {
        // Arrange
        final OrderItem item = new OrderItem();
        final Order order = new Order();
        order.setItems(Collections.singletonList(item));

        // Act
        final String result = order.toString();

        // Assert
        assertFalse(result.contains("JsonLoggingError"), result);
        assertThat(result, hasJsonPath("$.allowedForFullfilment", is(false)));

        assertFalse(result.contains("itemsMap"), result);
        assertThat(result, hasJsonPath("$.items", notNullValue()));
        assertFalse(result.contains("itemDescriptionEnglish"), result);
        assertFalse(result.contains("supplierDescription"), result);
        assertFalse(result.contains("description"), result);
        assertFalse(result.contains("pictures"), result);
        assertFalse(result.contains("\"kind2Parameters\""), result);
        assertFalse(result.contains("kind2ParametersAsString"), result);
        assertFalse(result.contains("kind2ParametersString"), result);
        assertFalse(result.contains("snapshot"), result);
        assertFalse(result.contains("manufacturerCountries"), result);
        assertFalse(result.contains("supplierWorkSchedule"), result);
        assertTrue(result.contains("\"kind2ParametersForJson\":null"), result);
        assertTrue(result.contains("\"offerName\":null"), result);
    }

    @Test
    public void toStringWithEmptyDelivery() {
        // Arrange
        final Delivery delivery = new Delivery();
        final Address address = new AddressImpl();
        delivery.setBuyerAddress(address);
        final ShopOutlet outlet = new ShopOutlet();
        delivery.setOutlet(outlet);
        final Order order = new Order();
        order.setDelivery(delivery);

        // Act
        final String result = order.toString();

        // Assert
        assertFalse(result.contains("JsonLoggingError"), result);
        assertThat(result, hasJsonPath("$.allowedForFullfilment", is(false)));

        assertTrue(result.contains("\"delivery\":{"), result);
        assertThat(result, hasJsonPath("$.delivery", notNullValue()));
        assertFalse(result.contains("scheduleString"), result);
        assertFalse(result.contains("outlets"), result);
        assertFalse(result.contains("shipments"), result);
        assertFalse(result.contains("\"shipment\""), result);
        assertFalse(result.contains("tracks"), result);
        assertFalse(result.contains("tracksJson"), result);
        assertFalse(result.contains("shipmentForJson"), result);
        assertTrue(result.contains("\"price\":null"), result);
        assertTrue(result.contains("\"shipmentDate\":null"), result);
        assertTrue(result.contains("\"shipmentDays\":0"), result);
    }
}
