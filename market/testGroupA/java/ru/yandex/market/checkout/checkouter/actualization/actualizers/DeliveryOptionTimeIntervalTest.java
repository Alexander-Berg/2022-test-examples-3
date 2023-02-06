package ru.yandex.market.checkout.checkouter.actualization.actualizers;

import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.cart.MultiCart;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryDates;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.feature.type.common.BooleanFeatureType;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.BlueParametersProvider;
import ru.yandex.market.common.report.model.ActualDeliveryResult;
import ru.yandex.market.common.report.model.DeliveryTimeInterval;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DeliveryOptionTimeIntervalTest extends AbstractWebTestBase {

    @Test
    public void shouldReturnIntervalFromReport() {
        checkouterFeatureWriter.writeValue(BooleanFeatureType.ENABLE_CHECK_DELIVERY_TIME_INTERVAL, false);
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        ActualDeliveryResult actualDeliveryResult =
                parameters.getReportParameters().getActualDelivery().getResults().get(0);
        LocalTime fromTime = LocalTime.of(23, 30);
        LocalTime toTime = LocalTime.of(0, 30);
        actualDeliveryResult.getDelivery().get(0).setTimeIntervals(
                List.of(new DeliveryTimeInterval(fromTime, toTime))
        );

        MultiCart cart = orderCreateHelper.cart(parameters);

        assertEquals(1, cart.getCarts().size());
        long count = cart.getCarts().get(0).getDeliveryOptions().stream()
                .filter(option -> DeliveryType.DELIVERY.equals(option.getType()))
                .flatMap(option -> option.getRawDeliveryIntervals().getCollection().entrySet().stream())
                .flatMap(entry -> entry.getValue().stream())
                .filter(interval -> fromTime.equals(interval.getFromTime()))
                .filter(interval -> toTime.equals(interval.getToTime()))
                .count();
        assertTrue(count > 0);
    }

    @Test
    public void shouldNotReturnIntervalFromReport() {
        checkouterFeatureWriter.writeValue(BooleanFeatureType.ENABLE_CHECK_DELIVERY_TIME_INTERVAL, true);
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        ActualDeliveryResult actualDeliveryResult =
                parameters.getReportParameters().getActualDelivery().getResults().get(0);
        LocalTime fromTime = LocalTime.of(23, 30);
        LocalTime toTime = LocalTime.of(0, 30);
        actualDeliveryResult.getDelivery().get(0).setTimeIntervals(
                List.of(new DeliveryTimeInterval(fromTime, toTime))
        );
        parameters.setCheckCartErrors(false);
        MultiCart cart = orderCreateHelper.cart(parameters);
        assertEquals(1, cart.getCartFailures().size());
        assertEquals("A start of interval is less or equals to its end",
                cart.getCartFailures().get(0).getErrorDetails());
    }

    @Test
    public void shouldCheckoutWithoutValidateTimeInterval() {
        checkouterFeatureWriter.writeValue(BooleanFeatureType.ENABLE_CHECK_DELIVERY_TIME_INTERVAL, false);
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        ActualDeliveryResult actualDeliveryResult =
                parameters.getReportParameters().getActualDelivery().getResults().get(0);
        LocalTime fromTime = LocalTime.of(23, 30);
        LocalTime toTime = LocalTime.of(0, 30);
        actualDeliveryResult.getDelivery().get(0).setTimeIntervals(
                List.of(new DeliveryTimeInterval(fromTime, toTime))
        );
        Order order = orderCreateHelper.createOrder(parameters);
        DeliveryDates deliveryDates = order.getDelivery().getDeliveryDates();
        assertEquals(fromTime, deliveryDates.getFromTime());
        assertEquals(toTime, deliveryDates.getToTime());
    }
}
