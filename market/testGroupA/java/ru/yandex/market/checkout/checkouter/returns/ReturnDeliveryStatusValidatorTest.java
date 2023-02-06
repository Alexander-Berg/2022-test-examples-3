package ru.yandex.market.checkout.checkouter.returns;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.pay.validation.ReturnDeliveryStatusValidator;
import ru.yandex.market.checkout.providers.ReturnProvider;
import ru.yandex.market.checkout.test.providers.OrderProvider;

/**
 * проверяем работу валидатора статусов
 */
public class ReturnDeliveryStatusValidatorTest {

    private final ReturnDeliveryStatusValidator validator = new ReturnDeliveryStatusValidator();

    public static Stream<Arguments> getAllStatuses() {
        return Stream.of(ReturnDeliveryStatus.values()).map(Arguments::of);
    }

    public static Stream<Arguments> getDeliveryStatuses() {
        return Stream.of(
                ReturnDeliveryStatus.CREATED,
                ReturnDeliveryStatus.SENDER_SENT,
                ReturnDeliveryStatus.DELIVERY,
                ReturnDeliveryStatus.PICKUP_SERVICE_RECEIVED,
                ReturnDeliveryStatus.READY_FOR_PICKUP,
                ReturnDeliveryStatus.DELIVERED).map(Arguments::of);
    }

    @ParameterizedTest
    @MethodSource("getAllStatuses")
    public void testValidatorFromNullStatus(ReturnDeliveryStatus status) {
        Order order = OrderProvider.getBlueOrder();
        Return aReturn = ReturnProvider.generateReturn(order);
        ReturnDelivery returnDelivery = ReturnProvider.getDefaultReturnDelivery();
        aReturn.setDelivery(returnDelivery);
        validator.validate(returnDelivery, status);
    }

    @ParameterizedTest
    @MethodSource("getDeliveryStatuses")
    public void testDeliveryToCancelStatus(ReturnDeliveryStatus status) {
        Order order = OrderProvider.getBlueOrder();
        Return aReturn = ReturnProvider.generateReturn(order);
        ReturnDelivery delivery = ReturnProvider.getDefaultReturnDelivery();
        delivery.setStatus(status);
        aReturn.setDelivery(delivery);
        validator.validate(delivery, ReturnDeliveryStatus.CANCELED);
    }

    @ParameterizedTest
    @MethodSource("getDeliveryStatuses")
    public void testDeliveryToLostStatus(ReturnDeliveryStatus status) {
        Order order = OrderProvider.getBlueOrder();
        Return aReturn = ReturnProvider.generateReturn(order);
        ReturnDelivery delivery = ReturnProvider.getDefaultReturnDelivery();
        delivery.setStatus(status);
        aReturn.setDelivery(delivery);
        validator.validate(delivery, ReturnDeliveryStatus.LOST);
    }
}
