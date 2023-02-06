package ru.yandex.market.checkout.checkouter.checkout;

import java.time.LocalTime;
import java.util.Set;

import com.google.common.collect.Iterables;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.delivery.RawDeliveryInterval;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.DeliveryResponseProvider;
import ru.yandex.market.checkout.providers.WhiteParametersProvider;
import ru.yandex.market.checkout.pushapi.client.entity.DeliveryResponse;
import ru.yandex.market.checkout.test.providers.DeliveryProvider;

public class CreateOrderDeliveryIntervalTest extends AbstractWebTestBase {

    @Test
    public void testCreateOrderWithInterval() {
        Parameters parameters = WhiteParametersProvider.simpleWhiteParameters();
        parameters.getOrder().setDelivery(DeliveryProvider.getEmptyDeliveryWithAddress());
        parameters.setPushApiDeliveryResponse(DeliveryResponseProvider.buildDeliveryResponseWithIntervals());

        Order order = orderCreateHelper.createOrder(parameters);

        Assertions.assertNotNull(order);
        Assertions.assertTrue(order.getDelivery().getDeliveryDates().hasTime());
        Assertions.assertNotNull(order.getDelivery().getDeliveryDates().getFromTime(), "delivery.deliveryDates" +
                ".fromTime");
        Assertions.assertNotNull(order.getDelivery().getDeliveryDates().getToTime(), "delivery.deliveryDates.toTime");
    }

    /**
     * checkouter-38. step 8
     */
    @Test
    public void testCreateOrderWithIntervalAndMissingToDate() throws Exception {
        Parameters parameters = WhiteParametersProvider.simpleWhiteParameters();
        parameters.getOrder().setDelivery(DeliveryProvider.getEmptyDeliveryWithAddress());
        parameters.setPushApiDeliveryResponse(DeliveryResponseProvider.buildDeliveryResponseWithIntervals());
        parameters.setMultiCartAction(mc -> {
            mc.getCarts().forEach(c -> {
                c.getDelivery().getDeliveryDates().setToDate(null);
            });
        });

        Order order = orderCreateHelper.createOrder(parameters);

        Assertions.assertNotNull(order);
        Assertions.assertTrue(order.getDelivery().getDeliveryDates().hasTime());
        Assertions.assertNotNull(order.getDelivery().getDeliveryDates().getFromTime(), "delivery.deliveryDates" +
                ".fromTime");
        Assertions.assertNotNull(order.getDelivery().getDeliveryDates().getToTime(), "delivery.deliveryDates.toTime");
    }

    /**
     * checkouter-38. step 9
     */
    @Test
    public void testCreateOrderWithIntervalAndMissingFromTime() throws Exception {
        Parameters parameters = WhiteParametersProvider.simpleWhiteParameters();
        parameters.getOrder().setDelivery(DeliveryProvider.getEmptyDeliveryWithAddress());
        parameters.setPushApiDeliveryResponse(DeliveryResponseProvider.buildDeliveryResponseWithIntervals());
        parameters.setMultiCartAction(mc -> {
            mc.getCarts().forEach(c -> {
                c.getDelivery().getDeliveryDates().setFromTime((LocalTime) null);
            });
        });

        Order order = orderCreateHelper.createOrder(parameters);

        Assertions.assertNotNull(order);
        Assertions.assertFalse(order.getDelivery().getDeliveryDates().hasTime());
    }

    /**
     * checkouter-38. step 10
     */
    @Test
    public void testCreateOrderWithIntervalAndMissingToTime() throws Exception {
        DeliveryResponse deliveryResponse = DeliveryResponseProvider.buildDeliveryResponseWithIntervals();

        Parameters parameters = new Parameters();
        parameters.setPushApiDeliveryResponse(deliveryResponse);
        parameters.setMultiCartAction(mc -> {
            mc.getCarts().forEach(c -> {
                c.getDelivery().getDeliveryDates().setToTime((LocalTime) null);
            });
        });

        Order order = orderCreateHelper.createOrder(parameters);

        Assertions.assertNotNull(order);
        Assertions.assertFalse(order.getDelivery().getDeliveryDates().hasTime());
    }

    /**
     * checkouter-39.
     */
    @Test
    public void testCreateOrderWithIntervalAndGetFromServiceAndController() throws Exception {
        DeliveryResponse deliveryResponse = DeliveryResponseProvider.buildDeliveryResponseWithIntervals();
        Set<RawDeliveryInterval> rawDeliveryIntervals =
                Iterables.get(deliveryResponse.getRawDeliveryIntervals().getCollection().entrySet(), 0).getValue();
        RawDeliveryInterval rawDeliveryInterval = Iterables.get(rawDeliveryIntervals, 0);

        Parameters parameters = WhiteParametersProvider.simpleWhiteParameters();
        parameters.getOrder().setDelivery(DeliveryProvider.getEmptyDeliveryWithAddress());
        parameters.setPushApiDeliveryResponse(deliveryResponse);

        Order order = orderCreateHelper.createOrder(parameters);

        Order orderFromService = orderService.getOrder(order.getId());

        Assertions.assertEquals(rawDeliveryInterval.getFromTime(),
                orderFromService.getDelivery().getDeliveryDates().getFromTime());
        Assertions.assertEquals(rawDeliveryInterval.getToTime(),
                orderFromService.getDelivery().getDeliveryDates().getToTime());

        Order orderFromApi = orderService.getOrder(order.getId());

        Assertions.assertEquals(rawDeliveryInterval.getFromTime(),
                orderFromApi.getDelivery().getDeliveryDates().getFromTime());
        Assertions.assertEquals(rawDeliveryInterval.getToTime(),
                orderFromApi.getDelivery().getDeliveryDates().getToTime());
    }
}
