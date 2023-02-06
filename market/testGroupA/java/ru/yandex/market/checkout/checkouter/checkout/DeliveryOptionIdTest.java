package ru.yandex.market.checkout.checkouter.checkout;

import java.time.LocalTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import com.google.common.collect.Iterables;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.cart.MultiCart;
import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.BlueParametersProvider;
import ru.yandex.market.checkout.test.providers.ActualDeliveryProvider;
import ru.yandex.market.common.report.model.ActualDeliveryResult;
import ru.yandex.market.common.report.model.DeliveryTimeInterval;

import static java.util.Collections.singletonList;

public class DeliveryOptionIdTest extends AbstractWebTestBase {

    private static List<String> extractDeliveryOptionIds(MultiCart cart) {
        return cart.getCarts().stream()
                .flatMap(c -> c.getDeliveryOptions().stream())
                .map(Delivery::getDeliveryOptionId)
                .sorted()
                .collect(Collectors.toList());
    }

    @Test
    public void shouldGenerateSameDeliveryOptionIdBetweenTwoActualizationRequests() {
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        MultiCart firstCart = orderCreateHelper.cart(parameters);
        MultiCart secondCart = orderCreateHelper.cart(parameters);

        Assertions.assertEquals(
                extractDeliveryOptionIds(secondCart),
                extractDeliveryOptionIds(firstCart)
        );
    }

    @Test
    public void shouldGenerateUniqueNonnullDeliveryOptionIdForMardoOptions() {
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        MultiCart cart = orderCreateHelper.cart(parameters);

        Assertions.assertTrue(cart.getCarts().stream()
                .flatMap(c -> c.getDeliveryOptions().stream())
                .map(Delivery::getDeliveryOptionId)
                .allMatch(Objects::nonNull), "all shop options has non null deliveryOptionId");
        Assertions.assertEquals(cart.getCarts().stream()
                .map(c -> Integer.valueOf(c.getDeliveryOptions().size()).longValue())
                .collect(Collectors.toList()), cart.getCarts().stream()
                .map(c -> c.getDeliveryOptions().stream()
                        .map(Delivery::getDeliveryOptionId)
                        .distinct()
                        .count())
                .collect(Collectors.toList()), "each cart shop options has unique deliveryOptionId");
    }

    @Test
    public void shouldGenerateUniqueDeliveryOptionIdForDifferentTimeOptionsWithExperiment() {
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        parameters.getReportParameters().setActualDelivery(
                ActualDeliveryProvider.builder()
                        .addDelivery(BlueParametersProvider.DELIVERY_SERVICE_ID)
                        .addDelivery(BlueParametersProvider.DELIVERY_SERVICE_ID)
                        .addDelivery(BlueParametersProvider.DELIVERY_SERVICE_ID)
                        .build()
        );
        ActualDeliveryResult actualDeliveryResult = Iterables.getOnlyElement(
                parameters.getReportParameters().getActualDelivery().getResults()
        );
        actualDeliveryResult.getDelivery().get(0).setTimeIntervals(singletonList(new DeliveryTimeInterval(
                LocalTime.of(9, 30),
                LocalTime.of(18, 0))));
        actualDeliveryResult.getDelivery().get(1).setTimeIntervals(singletonList(new DeliveryTimeInterval(
                LocalTime.of(9, 30),
                LocalTime.of(20, 0))));
        actualDeliveryResult.getDelivery().get(2).setTimeIntervals(singletonList(new DeliveryTimeInterval(
                LocalTime.of(10, 0),
                LocalTime.of(20, 0))));

        MultiCart cart = orderCreateHelper.cart(parameters);

        Assertions.assertTrue(cart.getCarts().stream()
                .flatMap(c -> c.getDeliveryOptions().stream())
                .map(Delivery::getDeliveryOptionId)
                .allMatch(Objects::nonNull), "all options has non null deliveryOptionId");

        Assertions.assertEquals(
                cart.getCarts().stream()
                        .map(c -> Integer.valueOf(c.getDeliveryOptions().size()).longValue())
                        .collect(Collectors.toList()),
                cart.getCarts().stream()
                        .map(c -> c.getDeliveryOptions().stream()
                                .map(Delivery::getDeliveryOptionId)
                                .distinct()
                                .count())
                        .collect(Collectors.toList()));
    }
}
