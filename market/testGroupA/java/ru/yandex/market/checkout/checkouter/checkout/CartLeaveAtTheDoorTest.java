package ru.yandex.market.checkout.checkouter.checkout;

import java.util.Iterator;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.cart.MultiCart;
import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryPartnerType;
import ru.yandex.market.checkout.checkouter.order.MultiOrder;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.WhiteParametersProvider;
import ru.yandex.market.checkout.test.providers.ActualDeliveryProvider;
import ru.yandex.market.checkout.test.providers.DeliveryProvider;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;

public class CartLeaveAtTheDoorTest extends AbstractWebTestBase {

    @Test
    public void shouldReturnDeliveryOptionsWithLeaveAtTheDoorField() {
        Parameters parameters = WhiteParametersProvider.defaultWhiteParameters();
        parameters.getReportParameters().setActualDelivery(
                ActualDeliveryProvider.builder()
                        .addDelivery(DeliveryProvider.createFrom(parameters.getOrder().getDelivery())
                                .serviceId(Delivery.SELF_DELIVERY_SERVICE_ID)
                                .partnerType(DeliveryPartnerType.SHOP)
                                .leaveAtTheDoor(Boolean.TRUE)
                                .buildActualDeliveryOption()
                        )
                        .addDelivery(DeliveryProvider.createFrom(parameters.getOrder().getDelivery())
                                .serviceId(Delivery.SELF_DELIVERY_SERVICE_ID)
                                .partnerType(DeliveryPartnerType.SHOP)
                                .leaveAtTheDoor(Boolean.FALSE)
                                .buildActualDeliveryOption()
                        ).build()
        );

        MultiCart actualMultiCart = orderCreateHelper.cart(parameters);

        Order actualOrder = actualMultiCart.getCarts().get(0);
        assertThat(actualOrder.getDeliveryOptions(), hasSize(2));
        Iterator<? extends Delivery> deliveryIterator = actualOrder.getDeliveryOptions().iterator();

        Delivery delivery1 = deliveryIterator.next();
        Assertions.assertTrue(delivery1.isLeaveAtTheDoor());

        Delivery delivery2 = deliveryIterator.next();
        Assertions.assertFalse(delivery2.isLeaveAtTheDoor());
    }

    @Test
    public void shouldCreateOrderWithLeaveAtTheDoor() {
        checkouterProperties.setEnableLeaveAtTheDoor(true);
        Parameters parameters = WhiteParametersProvider.defaultWhiteParameters();
        parameters.getOrder().getDelivery().setLeaveAtTheDoor(true);
        parameters.getReportParameters().setActualDelivery(
                ActualDeliveryProvider.builder()
                        .addDelivery(DeliveryProvider.createFrom(parameters.getOrder().getDelivery())
                                .serviceId(Delivery.SELF_DELIVERY_SERVICE_ID)
                                .partnerType(DeliveryPartnerType.SHOP)
                                .leaveAtTheDoor(Boolean.TRUE)
                                .buildActualDeliveryOption()
                        )
                        .addDelivery(DeliveryProvider.createFrom(parameters.getOrder().getDelivery())
                                .serviceId(Delivery.SELF_DELIVERY_SERVICE_ID)
                                .partnerType(DeliveryPartnerType.SHOP)
                                .leaveAtTheDoor(Boolean.FALSE)
                                .buildActualDeliveryOption()
                        ).build()
        );

        MultiOrder multiOrder = orderCreateHelper.createMultiOrder(parameters, multiCart -> {
            Delivery option = multiCart.getCarts().get(0).getDeliveryOptions().get(0);
            multiCart.getCarts().get(0).setDelivery(option);
        });
        Assertions.assertEquals(true, multiOrder.getCarts().get(0).getDelivery().isLeaveAtTheDoor());
    }

    @Test
    public void shouldCreateOrderWithLeaveAtTheDoorFalse() {
        checkouterProperties.setEnableLeaveAtTheDoor(true);
        Parameters parameters = WhiteParametersProvider.defaultWhiteParameters();
        parameters.setLeaveAtTheDoor(false);
        parameters.getReportParameters().setActualDelivery(
                ActualDeliveryProvider.builder()
                        .addDelivery(DeliveryProvider.createFrom(parameters.getOrder().getDelivery())
                                .serviceId(Delivery.SELF_DELIVERY_SERVICE_ID)
                                .partnerType(DeliveryPartnerType.SHOP)
                                .leaveAtTheDoor(Boolean.TRUE)
                                .buildActualDeliveryOption()
                        )
                        .addDelivery(DeliveryProvider.createFrom(parameters.getOrder().getDelivery())
                                .serviceId(Delivery.SELF_DELIVERY_SERVICE_ID)
                                .partnerType(DeliveryPartnerType.SHOP)
                                .leaveAtTheDoor(Boolean.FALSE)
                                .buildActualDeliveryOption()
                        ).build()
        );

        MultiOrder multiOrder = orderCreateHelper.createMultiOrder(parameters, multiCart -> {
            Delivery option = multiCart.getCarts().get(0).getDeliveryOptions().get(1);
            multiCart.getCarts().get(0).setDelivery(option);
        });

        Assertions.assertEquals(false, multiOrder.getCarts().get(0).getDelivery().isLeaveAtTheDoor());
    }

    @Test
    public void shouldTakeLeaveAtTheDoorFromOrderNotes() {
        checkouterProperties.setEnableLeaveAtTheDoor(true);
        Parameters parameters = WhiteParametersProvider.defaultWhiteParameters();
        parameters.getOrder().setNotes("Оставить у двери");
        parameters.getReportParameters().setActualDelivery(
                ActualDeliveryProvider.builder()
                        .addDelivery(DeliveryProvider.createFrom(parameters.getOrder().getDelivery())
                                .serviceId(Delivery.SELF_DELIVERY_SERVICE_ID)
                                .partnerType(DeliveryPartnerType.SHOP)
                                .leaveAtTheDoor(Boolean.TRUE)
                                .buildActualDeliveryOption()
                        )
                        .addDelivery(DeliveryProvider.createFrom(parameters.getOrder().getDelivery())
                                .serviceId(Delivery.SELF_DELIVERY_SERVICE_ID)
                                .partnerType(DeliveryPartnerType.SHOP)
                                .leaveAtTheDoor(Boolean.FALSE)
                                .buildActualDeliveryOption()
                        ).build()
        );

        MultiOrder multiOrder = orderCreateHelper.createMultiOrder(parameters);
        Assertions.assertEquals(true, multiOrder.getCarts().get(0).getDelivery().isLeaveAtTheDoor());
    }

    @Test
    public void shouldPutFalseIfCouldNotFind() {
        checkouterProperties.setEnableLeaveAtTheDoor(true);
        Parameters parameters = WhiteParametersProvider.defaultWhiteParameters();
        parameters.getReportParameters().setActualDelivery(
                ActualDeliveryProvider.builder()
                        .addDelivery(DeliveryProvider.createFrom(parameters.getOrder().getDelivery())
                                .serviceId(Delivery.SELF_DELIVERY_SERVICE_ID)
                                .partnerType(DeliveryPartnerType.SHOP)
                                .leaveAtTheDoor(null)
                                .buildActualDeliveryOption()
                        )
                        .build()
        );

        MultiOrder multiOrder = orderCreateHelper.createMultiOrder(parameters);
        Assertions.assertEquals(false, multiOrder.getCarts().get(0).getDelivery().isLeaveAtTheDoor());
    }

    @Test
    public void shouldPutNullIfFeatureIsDisabled() {
        checkouterProperties.setEnableLeaveAtTheDoor(false);
        Parameters parameters = WhiteParametersProvider.defaultWhiteParameters();
        parameters.getReportParameters().setActualDelivery(
                ActualDeliveryProvider.builder()
                        .addDelivery(DeliveryProvider.createFrom(parameters.getOrder().getDelivery())
                                .serviceId(Delivery.SELF_DELIVERY_SERVICE_ID)
                                .partnerType(DeliveryPartnerType.SHOP)
                                .leaveAtTheDoor(Boolean.TRUE)
                                .buildActualDeliveryOption()
                        )
                        .addDelivery(DeliveryProvider.createFrom(parameters.getOrder().getDelivery())
                                .serviceId(Delivery.SELF_DELIVERY_SERVICE_ID)
                                .partnerType(DeliveryPartnerType.SHOP)
                                .leaveAtTheDoor(Boolean.FALSE)
                                .buildActualDeliveryOption()
                        ).build()
        );

        MultiOrder multiOrder = orderCreateHelper.createMultiOrder(parameters);
        Assertions.assertNull(multiOrder.getCarts().get(0).getDelivery().isLeaveAtTheDoor());
    }
}
