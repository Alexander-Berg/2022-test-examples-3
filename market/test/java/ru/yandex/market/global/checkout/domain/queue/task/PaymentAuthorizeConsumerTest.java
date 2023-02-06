package ru.yandex.market.global.checkout.domain.queue.task;

import java.time.Clock;

import io.github.benas.randombeans.api.EnhancedRandom;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import ru.yoomoney.tech.dbqueue.api.TaskExecutionResult;

import ru.yandex.market.global.checkout.BaseFunctionalTest;
import ru.yandex.market.global.checkout.domain.queue.task.payments.PaymentAuthorizeConsumer;
import ru.yandex.market.global.checkout.factory.TestOrderFactory;
import ru.yandex.market.global.checkout.queue.TestQueueTaskRunner;
import ru.yandex.market.global.checkout.util.RandomDataGenerator;
import ru.yandex.market.global.common.test.TestUtil;
import ru.yandex.market.global.common.trust.client.TrustClient;
import ru.yandex.market.global.common.trust.client.dto.CreateOrderResponse;
import ru.yandex.market.global.common.trust.client.dto.CreatePaymentResponse;
import ru.yandex.market.global.common.trust.client.dto.PaymentStatusResponse;
import ru.yandex.market.global.common.trust.client.dto.StartPaymentRequest;
import ru.yandex.market.global.db.jooq.tables.pojos.OrderPayment;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static ru.yoomoney.tech.dbqueue.api.TaskExecutionResult.Type.FAIL;
import static ru.yoomoney.tech.dbqueue.api.TaskExecutionResult.Type.REENQUEUE;

@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class PaymentAuthorizeConsumerTest extends BaseFunctionalTest {
    private static final EnhancedRandom RANDOM = RandomDataGenerator.dataRandom(
            PaymentAuthorizeConsumerTest.class
    ).build();
    private static final String ORDER_ID = "123";
    private static final String TRUST_PRODUCT_ID = "PRODUCT_ID";
    private static final int TRUST_REGION_ID = 213;
    private static final String TRUST_PAYMETHOD_ID = "card";
    private static final String TRUST_PURCHASE_TOKEN = "123gfasdhfashd";
    private static final String STATUS_SUCCESS = "success";

    private final TestOrderFactory testOrderFactory;
    private final TrustClient trustClient;
    private final PaymentAuthorizeConsumer paymentAuthorizeConsumer;
    private final Clock clock;

    @BeforeEach
    void setup() {
        Mockito.when(trustClient.getPayment(Mockito.anyString()))
                .thenReturn(PaymentStatusResponse.builder()
                        .status(STATUS_SUCCESS)
                        .build()
                );

        Mockito.when(trustClient.createOrder(Mockito.any()))
                .thenReturn(CreateOrderResponse.builder()
                        .status(STATUS_SUCCESS)
                        .orderId(ORDER_ID)
                        .productId(TRUST_PRODUCT_ID)
                        .build()
                );

        Mockito.when(trustClient.createPayment(Mockito.any()))
                .thenReturn(CreatePaymentResponse.builder()
                        .purchaseToken(TRUST_PURCHASE_TOKEN)
                        .status(STATUS_SUCCESS)
                        .build()
                );

        Mockito.when(trustClient.startPayment(Mockito.any(StartPaymentRequest.class)))
                .thenReturn(PaymentStatusResponse.builder()
                        .purchaseToken(TRUST_PURCHASE_TOKEN)
                        .status(STATUS_SUCCESS)
                        .build()
                );
    }

    @Test
    void testParallelAuthoriseOnlyOnce() {
        OrderPayment orderPayment = testOrderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                .setupPayment(p -> new OrderPayment()
                        .setTrustPaymethodId(TRUST_PAYMETHOD_ID)
                        .setTrustRegionId(TRUST_REGION_ID)
                        .setTrustProductId(TRUST_PRODUCT_ID)
                        .setVersion(1L)
                        .setRequired_3ds(false)
                )
                .build()
        ).getOrderPayment();

        TestUtil.ParallelCallResults<TaskExecutionResult> results = TestUtil.doParallelCalls(
                3, () -> TestQueueTaskRunner.runTaskAndReturnResult(paymentAuthorizeConsumer, orderPayment.getOrderId())
        );

        Assertions.assertThat(results.getResults())
                .map(TaskExecutionResult::getActionType)
                .containsExactlyInAnyOrder(FAIL, FAIL, REENQUEUE);

        verify(trustClient, times(1)).createPayment(any());
    }
}
