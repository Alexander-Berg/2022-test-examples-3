package ru.yandex.market.checkout.checkouter.order;

import java.util.function.Consumer;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryPartnerType;
import ru.yandex.market.checkout.helpers.OrderCreateHelper;
import ru.yandex.market.checkout.helpers.OrderGetHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.WhiteParametersProvider;
import ru.yandex.market.checkout.test.providers.ActualDeliveryProvider;
import ru.yandex.market.checkout.test.providers.DeliveryProvider;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class GetOrderWithLeaveAtTheDoorOption extends AbstractWebTestBase {

    @Autowired
    private OrderCreateHelper orderCreateHelper;
    @Autowired
    private OrderGetHelper orderGetHelper;

    @Test
    public void testGetOrderWithLeaveAtTheDoorOption() throws Exception {
        testGetOrderForLeaveAtTheDoor(true, true, Assertions::assertTrue);
    }

    @Test
    public void testGetOrderWithDefaultLeaveAtTheDoorOption() throws Exception {
        testGetOrderForLeaveAtTheDoor(true, null, Assertions::assertFalse);
    }

    @Test
    public void testGetOrderWhenLeaveAtTheDoorIsDisabled() throws Exception {
        testGetOrderForLeaveAtTheDoor(false, true, Assertions::assertNull);
    }

    private void testGetOrderForLeaveAtTheDoor(boolean isFeatureEnabled, Boolean reportResponse,
                                               Consumer<Boolean> assertion) throws Exception {
        checkouterProperties.setEnableLeaveAtTheDoor(isFeatureEnabled);
        Parameters parameters = WhiteParametersProvider.defaultWhiteParameters();
        parameters.getReportParameters().setActualDelivery(
                ActualDeliveryProvider.builder()
                        .addDelivery(DeliveryProvider.createFrom(parameters.getOrder().getDelivery())
                                .serviceId(Delivery.SELF_DELIVERY_SERVICE_ID)
                                .partnerType(DeliveryPartnerType.SHOP)
                                .leaveAtTheDoor(reportResponse)
                                .buildActualDeliveryOption()
                        )
                        .build()
        );

        MultiOrder newOrder = orderCreateHelper.createMultiOrder(parameters);
        Order order = orderGetHelper.getOrder(newOrder.getOrders().get(0).getId(), ClientInfo.SYSTEM);
        assertNotNull(order);
        assertNotNull(order.getDelivery());
        assertion.accept(order.getDelivery().isLeaveAtTheDoor());
    }

}
