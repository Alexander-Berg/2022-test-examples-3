package ru.yandex.market.checkout.checkouter.lavka;

import java.util.Collections;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.BeforeEach;

import ru.yandex.market.checkout.checkouter.feature.type.common.ComplexFeatureType;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderTypeUtils;
import ru.yandex.market.checkout.common.util.SwitchWithWhitelist;
import ru.yandex.market.checkout.helpers.utils.Parameters;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.market.checkout.checkouter.feature.type.common.BooleanFeatureType.ENABLE_DEFERRED_COURIER_NEW_DELIVERY_SUBSTATUSES;
import static ru.yandex.market.checkout.checkouter.order.OrderStatus.DELIVERY;

public class DeferredCourierDeliveryFlowTest extends YandexLavkaDeliveryFlowTest {

    @BeforeEach
    public void before() {
        personalMockConfigurer.mockV1MultiTypesRetrieveAddressAndGps();
    }

    @Override
    @Nonnull
    protected Order createLavkaOrderWithDsTrack() throws Exception {
        Order order = createDeferredCourierOrder();
        assertTrue(OrderTypeUtils.isDeferredCourierDelivery(order));
        order = orderStatusHelper.proceedOrderToStatus(order, DELIVERY);
        return addDsTrack(order);
    }

    private Order createDeferredCourierOrder() {
        checkouterFeatureWriter.writeValue(ComplexFeatureType.COMBINATOR_FLOW, new SwitchWithWhitelist(true,
                Collections.<String>emptySet()));
        // включаем переходы по подстатусам для deferred_courier
        checkouterFeatureWriter.writeValue(ENABLE_DEFERRED_COURIER_NEW_DELIVERY_SUBSTATUSES, true);

        Parameters parameters = yaLavkaHelper.buildParametersForDeferredCourier(1);
        Order order = orderCreateHelper.createOrder(parameters);
        assertTrue(OrderTypeUtils.isDeferredCourierDelivery(order));
        return order;
    }
}
