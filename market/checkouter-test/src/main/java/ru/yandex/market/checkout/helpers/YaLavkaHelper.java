package ru.yandex.market.checkout.helpers;

import java.time.Duration;
import java.time.LocalTime;
import java.util.List;

import javax.annotation.Nonnull;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.context.WebApplicationContext;

import ru.yandex.market.checkout.checkouter.cart.MultiCart;
import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.common.WebTestHelper;
import ru.yandex.market.checkout.helpers.utils.MockMvcAware;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.test.providers.ActualDeliveryProvider;
import ru.yandex.market.checkout.util.serialization.TestSerializationService;
import ru.yandex.market.common.report.model.DeliveryTimeInterval;

import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.market.checkout.test.providers.DeliveryProvider.MOCK_DELIVERY_SERVICE_ID;

@WebTestHelper
public class YaLavkaHelper extends MockMvcAware {

    public static final long LAVKA_DELIVERY_SERVICE_ID = 19463827;

    @Autowired
    private YandexMarketDeliveryHelper yandexMarketDeliveryHelper;

    public YaLavkaHelper(WebApplicationContext webApplicationContext,
                         TestSerializationService testSerializationService) {
        super(webApplicationContext, testSerializationService);
    }

    public Parameters buildParameters(boolean isLavkaFinalOption, DeliveryOptionParamsHolder... params) {
        return buildParameters(isLavkaFinalOption, false, params);
    }

    public Parameters buildParameters(boolean isLavkaFinalOption,
                                      boolean isCombinatorFlow,
                                      DeliveryOptionParamsHolder... params) {
        ActualDeliveryProvider.ActualDeliveryBuilder actualDeliveryBuilder = ActualDeliveryProvider.builder();
        for (DeliveryOptionParamsHolder paramsHolder : params) {
            actualDeliveryBuilder.addDelivery(
                    paramsHolder.isLavkaDeliveryService ? LAVKA_DELIVERY_SERVICE_ID : MOCK_DELIVERY_SERVICE_ID,
                    isCombinatorFlow && !paramsHolder.isLavkaDeliveryService ? null : 1,
                    null,
                    Duration.ofHours(23),
                    null,
                    null,
                    paramsHolder.deliveryDay,
                    paramsHolder.deliveryDay,
                    ado -> ado.setIsOnDemand(isCombinatorFlow)
            );
        }
        Parameters parameters = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withDeliveryServiceId(isLavkaFinalOption ? LAVKA_DELIVERY_SERVICE_ID : MOCK_DELIVERY_SERVICE_ID)
                .withDeliveryType(DeliveryType.DELIVERY)
                .withColor(Color.BLUE)
                .withActualDelivery(actualDeliveryBuilder.build())
                .withCombinator(isCombinatorFlow)
                .buildParameters();
        if (isCombinatorFlow) {
            parameters.getReportParameters().getDeliveryRoute().getResults().get(0).getOption().setIsOnDemand(true);
        }

        return parameters;
    }

    public static DeliveryOptionParamsHolder normalOption(int shipmentDay) {
        return new DeliveryOptionParamsHolder(false, shipmentDay);
    }

    public static DeliveryOptionParamsHolder lavkaOption(int shipmentDay) {
        return new DeliveryOptionParamsHolder(true, shipmentDay);
    }

    public static class DeliveryOptionParamsHolder {

        private final boolean isLavkaDeliveryService;
        private final int deliveryDay;

        private DeliveryOptionParamsHolder(boolean isLavkaDeliveryService, int deliveryDay) {
            this.isLavkaDeliveryService = isLavkaDeliveryService;
            this.deliveryDay = deliveryDay;
        }
    }

    public static List<? extends Delivery> getDeliveryOptions(@Nonnull MultiCart multiCart) {
        return multiCart.getCarts().get(0).getDeliveryOptions();
    }

    public static void assertHasNoErrors(@Nonnull MultiCart multiCart) {
        assertFalse(multiCart.hasErrors());
        assertFalse(multiCart.hasFailures());
        assertFalse(multiCart.hasOrderErrors());
    }

    public static void assertHasErrors(@Nonnull MultiCart multiCart) {
        assertTrue(multiCart.hasErrors() || multiCart.hasFailures() || multiCart.hasOrderErrors());
    }

    public Parameters buildParametersForDeferredCourier(int deliveryDay) {
        return buildParametersForDeferredCourier(
                addDeferredCourierDelivery(ActualDeliveryProvider.builder(), deliveryDay)
        );
    }

    public Parameters buildParametersForDeferredCourier(
            ActualDeliveryProvider.ActualDeliveryBuilder actualDeliveryBuilder) {
        Parameters parameters = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID)
                .withDeliveryType(DeliveryType.DELIVERY)
                .withColor(Color.BLUE)
                .withActualDelivery(actualDeliveryBuilder.build())
                .withCombinator(true)
                .withDeliveryType(DeliveryType.DELIVERY)
                .buildParameters();
        parameters.getReportParameters().getDeliveryRoute().getResults().get(0).getOption().setIsDeferredCourier(true);
        parameters.setMinifyOutlets(true);
        return parameters;
    }

    public ActualDeliveryProvider.ActualDeliveryBuilder addDeferredCourierDelivery(
            ActualDeliveryProvider.ActualDeliveryBuilder actualDeliveryBuilder,
            int deliveryDay) {

        actualDeliveryBuilder.addDelivery(
                MOCK_DELIVERY_SERVICE_ID,
                1,
                null,
                Duration.ofHours(23),
                null,
                null,
                deliveryDay,
                deliveryDay,
                ado -> {
                    ado.setIsDeferredCourier(true);
                    ado.setTimeIntervals(singletonList(new DeliveryTimeInterval(
                            LocalTime.of(10, 0),
                            LocalTime.of(11, 0))));
                });
        return actualDeliveryBuilder;
    }
}
