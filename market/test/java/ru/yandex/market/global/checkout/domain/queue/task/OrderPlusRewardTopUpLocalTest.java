package ru.yandex.market.global.checkout.domain.queue.task;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.global.checkout.BaseLocalTest;
import ru.yandex.market.global.checkout.domain.queue.task.payments.OrderPlusRewardTopUpConsumer;
import ru.yandex.market.global.checkout.factory.TestOrderFactory;
import ru.yandex.market.global.checkout.queue.TestQueueTaskRunner;
import ru.yandex.market.global.common.trust.client.TrustClient;
import ru.yandex.market.global.common.trust.client.dto.BoundPaymentMethod;
import ru.yandex.market.global.common.trust.client.dto.GetPaymentMethodsRequest;
import ru.yandex.market.global.common.trust.client.dto.GetPaymentMethodsResponse;
import ru.yandex.market.global.common.util.configuration.ConfigurationService;
import ru.yandex.market.global.db.jooq.enums.EPlusActionType;
import ru.yandex.market.global.db.jooq.tables.pojos.Order;

import static ru.yandex.market.global.checkout.configuration.ConfigurationProperties.PLUS_MODE;

@Slf4j
@Disabled
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class OrderPlusRewardTopUpLocalTest extends BaseLocalTest {

    private static final Long UID = 4092490744L;
    //сменить ордер ID ибо у нас в in-memory postgres и trust имеют разные state с разными orderID
    //поэтому trust ругается из-за дублирующихся order-id
    private static final Long ORDER_ID = 100053L;

    private final OrderPlusRewardTopUpConsumer orderPlusRewardTopUpConsumer;
    private final TestOrderFactory orderFactory;
    private final TrustClient trustClient;
    private final ConfigurationService configurationProvider;

    @BeforeEach
    public void setup() {
        configurationProvider.mergeValue(PLUS_MODE, "ON");
    }

    @Test
    void getPaymethods() {
        GetPaymentMethodsResponse response = trustClient.getPaymentMethods(GetPaymentMethodsRequest.builder()
                .uid(UID)
                .showBound(true)
                .showEnabled(true)
                .build());

        log.error("Paymethods: {}", response.getBoundPaymentMethods());
    }

    @Test
    void topUpPlusPoints() throws InterruptedException {

        Order order = createOrder();

        TestQueueTaskRunner.runTaskThrowOnFail(orderPlusRewardTopUpConsumer, order.getId());

        Thread.sleep(40_000);
    }


    private Order createOrder() {
        BoundPaymentMethod payMethod = trustClient.getPaymentMethods(GetPaymentMethodsRequest.builder()
                        .showEnabled(true)
                        .showBound(true)
                        .uid(UID)
                        .build())
                .getBoundPaymentMethods().stream()
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Payment methods not found!"));

        return orderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                .setupOrder(d -> d
                        .setUid(UID)
                        .setId(ORDER_ID)
                        .setPlusSpent(0L)
                        .setPlusEarned(100_00L)
                        .setPlusAction(EPlusActionType.EARN))
                .setupDelivery(d -> d
                        .setRecipientEmail("denr01@yandex.ru")
                        .setRecipientPhone("+78005553535"))
                .setupPayment(p -> p
                        .setTrustPaymethodId(payMethod.getId())
                        .setTrustRegionId(payMethod.getRegionId())
                        .setTrustProductId("1354411205")
                        .setPlusTopUpStartAt(null)
                        .setPlusTopUpPurchaseToken(null)
                )
                .build()).getOrder();
    }

}
