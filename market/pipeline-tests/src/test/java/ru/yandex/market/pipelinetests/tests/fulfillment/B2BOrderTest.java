package ru.yandex.market.pipelinetests.tests.fulfillment;

import java.util.Set;

import dto.requests.checkouter.CreateOrderParameters;
import dto.responses.lom.admin.order.LomAdminOrderTag;
import factory.OfferItems;
import io.qameta.allure.Epic;
import io.qameta.allure.TmsLink;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import ru.qatools.properties.Resource;

import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.checkouter.pay.PaymentType;

@Resource.Classpath({"delivery/checkouter.properties", "delivery/delivery.properties"})
@DisplayName("Blue FF B2B Create/Cancel order Test")
@Epic("Blue FF B2B")
@Tag("B2BFulfillmentOrderTest")
@Slf4j
public class B2BOrderTest extends AbstractFulfillmentTest {
    private void createOrder() {
        params = CreateOrderParameters
            .newBuilder(213L, OfferItems.FF_172_B2B.getItem(), DeliveryType.DELIVERY)
            .paymentType(PaymentType.PREPAID)
            .paymentMethod(PaymentMethod.B2B_ACCOUNT_PREPAYMENT)
            .businessBuyerBalanceId(1234L)
            .type(1L)
            .availableForBusiness(1L)
            .build();
        order = ORDER_STEPS.createOrder(params);
        ORDER_STEPS.payOrder(order, OrderStatus.PROCESSING);
        ORDER_STEPS.changeOrderStatusAndSubStatus(order.getId(), null, OrderStatus.PROCESSING, OrderSubstatus.STARTED);
        ORDER_STEPS.verifySDTracksCreated(order);
        ORDER_STEPS.verifyFFTrackCreated(order);
    }

    @Test
    @TmsLink("logistic-8")
    @DisplayName("B2B заказ: Создание заказа типа DELIVERY в регионе 213")
    public void createDeliveryB2BOrderTest() {
        log.info("Trying to create checkouter order");

        createOrder();

        lomOrderId = LOM_ORDER_STEPS.getLomOrderId(order);
        LOM_ORDER_STEPS.verifyOrderStatus(
            lomOrderId,
            ru.yandex.market.logistics.lom.model.enums.OrderStatus.PROCESSING
        );

        lomOrder = LOM_ORDER_STEPS.verifyVerificationCodePresent(lomOrderId);
        ORDER_STEPS.verifyCode(order.getId(), lomOrder.getRecipientVerificationCode());
        LOM_ORDER_STEPS.verifyOrderTags(lomOrderId, Set.of(LomAdminOrderTag.B2B_CUSTOMER));
    }
}
