package ru.yandex.market.global.checkout.domain.queue.task;

import java.time.OffsetDateTime;

import io.github.benas.randombeans.api.EnhancedRandom;
import lombok.RequiredArgsConstructor;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import ru.yoomoney.tech.dbqueue.config.DatabaseAccessLayer;
import ru.yoomoney.tech.dbqueue.config.QueueShard;

import ru.yandex.market.global.checkout.BaseFunctionalTest;
import ru.yandex.market.global.checkout.api.OrderApiService;
import ru.yandex.market.global.checkout.domain.order.OrderRepository;
import ru.yandex.market.global.checkout.domain.queue.task.deliveries.UpdateCourierReceiveOrderCodeConsumer;
import ru.yandex.market.global.checkout.domain.queue.task.deliveries.UpdateCourierReturnOrderCodeConsumer;
import ru.yandex.market.global.checkout.factory.TestOrderFactory;
import ru.yandex.market.global.checkout.queue.TestQueueTaskRunner;
import ru.yandex.market.global.checkout.util.RandomDataGenerator;
import ru.yandex.market.global.db.jooq.enums.EDeliveryOrderState;
import ru.yandex.market.global.db.jooq.enums.EOrderState;
import ru.yandex.market.global.db.jooq.tables.pojos.Order;
import ru.yandex.mj.generated.client.taxi_v2_intergration.api.ClaimsApiClient;
import ru.yandex.mj.generated.client.taxi_v2_intergration.model.ConfirmationCodeResponse;
import ru.yandex.mj.generated.server.model.OrderDto;

import static ru.yandex.market.global.db.jooq.enums.EDeliveryOrderState.COURIER_FOUND;
import static ru.yandex.market.global.db.jooq.enums.EDeliveryOrderState.DELIVERING_ORDER;
import static ru.yandex.market.global.db.jooq.enums.EDeliveryOrderState.RETURNING_ORDER;
import static ru.yandex.market.global.db.jooq.enums.EOrderState.CANCELED;
import static ru.yandex.market.global.db.jooq.enums.EOrderState.CANCELING;
import static ru.yandex.market.global.db.jooq.enums.EOrderState.FINISHED;
import static ru.yandex.market.global.db.jooq.enums.EOrderState.NEW;
import static ru.yandex.market.global.db.jooq.enums.EOrderState.PROCESSING;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class UpdateCourierCodesTest extends BaseFunctionalTest {

    private static final EnhancedRandom RANDOM = RandomDataGenerator.dataRandom(UpdateCourierCodesTest.class).build();
    public static final String CONFIRMATION_CODE = "AAA-123";
    public static final String CONFIRMATION_CODE_2 = "BBB-123";

    private final ClaimsApiClient claimsApiClient;
    private final QueueShard<DatabaseAccessLayer> shard;
    private final UpdateCourierReceiveOrderCodeConsumer confirmationConsumer;
    private final UpdateCourierReturnOrderCodeConsumer returningConsumer;
    private final TestOrderFactory testOrderFactory;
    private final OrderApiService orderApiService;
    private final OrderRepository orderRepository;

    private void mockWithCode(String code) {
        Mockito.when(claimsApiClient.integrationV2ClaimsConfirmationCode(Mockito.any(), Mockito.any(), Mockito.any())
                .schedule().join()).thenReturn(new ConfirmationCodeResponse().code(code).attempts(100));
    }

    private void mockWithError() {
        Mockito.when(claimsApiClient.integrationV2ClaimsConfirmationCode(Mockito.any(), Mockito.any(), Mockito.any())
                        .schedule().join()).thenThrow(new RuntimeException("ru.yandex.market.common.retrofit" +
                        ".CommonRetrofitHttpExecutionException: HTTP 404, {\"code\":\"1\"," +
                        "\"message\":\"address_not_found\"}" +
                        " @ GET https://b2b.taxi.tst.yandex.net/b2b/cargo/integration/v1/claims/confirmation-code"))
                .thenReturn(null);
    }

    private long createOrder(EOrderState state, EDeliveryOrderState deliveryState) {
        return testOrderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                .setupOrder(it -> it.setOrderState(state).setDeliveryState(deliveryState))
                .setupDelivery(it -> it.setCourierReceiveOrderCode(null).setCourierReturnOrderCode(null))
                .build()).getOrder().getId();
    }

    private void updateOrderStatus(long orderId, EOrderState orderState, EDeliveryOrderState deliveryState) {
        Order order = orderRepository.fetchOneById(orderId);
        order.setOrderState(orderState).setDeliveryState(deliveryState);
        if (orderState != FINISHED) {
            order.setFinishedAt(null);
        } else {
            order.setFinishedAt(RANDOM.nextObject(OffsetDateTime.class));
        }
        if (orderState != CANCELED) {
            order.setCanceledAt(null);
        } else {
            order.setCanceledAt(RANDOM.nextObject(OffsetDateTime.class));
        }
        orderRepository.update(order);
    }

    @Test
    public void testConfirmationCode() {

        long orderId = createOrder(PROCESSING, COURIER_FOUND);
        mockWithCode(CONFIRMATION_CODE);
        TestQueueTaskRunner.runTaskAndReturnResult(confirmationConsumer, orderId);

        OrderDto dto = orderApiService.apiV1OrderGetGet(orderId).getBody();
        Assertions.assertThat(dto).isNotNull();
        Assertions.assertThat(dto.getCourierReceiveOrderCode()).isEqualTo(CONFIRMATION_CODE);


        mockWithCode(CONFIRMATION_CODE_2);
        TestQueueTaskRunner.runTaskAndReturnResult(confirmationConsumer, orderId);

        dto = orderApiService.apiV1OrderGetGet(orderId).getBody();
        Assertions.assertThat(dto).isNotNull();
        Assertions.assertThat(dto.getCourierReceiveOrderCode()).isEqualTo(CONFIRMATION_CODE_2);

    }

    @Test
    public void testStopUpdatingConfirmationCode() {

        long orderId = createOrder(PROCESSING, COURIER_FOUND);
        mockWithCode(CONFIRMATION_CODE);
        TestQueueTaskRunner.runTaskAndReturnResult(confirmationConsumer, orderId);

        OrderDto dto = orderApiService.apiV1OrderGetGet(orderId).getBody();
        Assertions.assertThat(dto).isNotNull();
        Assertions.assertThat(dto.getCourierReceiveOrderCode()).isEqualTo(CONFIRMATION_CODE);

        mockWithCode(CONFIRMATION_CODE_2);

        updateOrderStatus(orderId, FINISHED, COURIER_FOUND);
        TestQueueTaskRunner.runTaskAndReturnResult(confirmationConsumer, orderId);

        dto = orderApiService.apiV1OrderGetGet(orderId).getBody();
        Assertions.assertThat(dto).isNotNull();
        Assertions.assertThat(dto.getCourierReceiveOrderCode()).isEqualTo(CONFIRMATION_CODE);


        updateOrderStatus(orderId, PROCESSING, DELIVERING_ORDER);
        TestQueueTaskRunner.runTaskAndReturnResult(confirmationConsumer, orderId);

        dto = orderApiService.apiV1OrderGetGet(orderId).getBody();
        Assertions.assertThat(dto).isNotNull();
        Assertions.assertThat(dto.getCourierReceiveOrderCode()).isEqualTo(CONFIRMATION_CODE);
    }

    @Test
    public void testReturningCode() {

        long orderId = createOrder(CANCELING, RETURNING_ORDER);
        mockWithCode(CONFIRMATION_CODE);
        TestQueueTaskRunner.runTaskAndReturnResult(returningConsumer, orderId);

        OrderDto dto = orderApiService.apiV1OrderGetGet(orderId).getBody();
        Assertions.assertThat(dto).isNotNull();
        Assertions.assertThat(dto.getCourierReturnOrderCode()).isEqualTo(CONFIRMATION_CODE);


        mockWithCode(CONFIRMATION_CODE_2);
        TestQueueTaskRunner.runTaskAndReturnResult(returningConsumer, orderId);

        dto = orderApiService.apiV1OrderGetGet(orderId).getBody();
        Assertions.assertThat(dto).isNotNull();
        Assertions.assertThat(dto.getCourierReturnOrderCode()).isEqualTo(CONFIRMATION_CODE_2);

    }

    @Test
    public void testNotReadyConfirmationCode() {

        long orderId = createOrder(NEW, EDeliveryOrderState.NEW);
        mockWithCode(CONFIRMATION_CODE);
        TestQueueTaskRunner.runTaskAndReturnResult(confirmationConsumer, orderId);

        OrderDto dto = orderApiService.apiV1OrderGetGet(orderId).getBody();
        Assertions.assertThat(dto).isNotNull();
        Assertions.assertThat(dto.getCourierReceiveOrderCode()).isEqualTo(CONFIRMATION_CODE);
    }


    @Test
    public void testMissedConfirmationCodeUpdating() {

        long orderId = createOrder(PROCESSING, COURIER_FOUND);
        mockWithCode(CONFIRMATION_CODE);
        TestQueueTaskRunner.runTaskAndReturnResult(confirmationConsumer, orderId);

        OrderDto dto = orderApiService.apiV1OrderGetGet(orderId).getBody();
        Assertions.assertThat(dto).isNotNull();
        Assertions.assertThat(dto.getCourierReceiveOrderCode()).isEqualTo(CONFIRMATION_CODE);


        mockWithError();
        TestQueueTaskRunner.runTaskAndReturnResult(confirmationConsumer, orderId);

        dto = orderApiService.apiV1OrderGetGet(orderId).getBody();
        Assertions.assertThat(dto).isNotNull();
        Assertions.assertThat(dto.getCourierReceiveOrderCode()).isEqualTo(CONFIRMATION_CODE);
    }

    @Test
    public void testMissedConfirmationCode() {

        long orderId = createOrder(PROCESSING, COURIER_FOUND);
        mockWithError();
        TestQueueTaskRunner.runTaskAndReturnResult(confirmationConsumer, orderId);

        OrderDto dto = orderApiService.apiV1OrderGetGet(orderId).getBody();
        Assertions.assertThat(dto).isNotNull();
        Assertions.assertThat(dto.getCourierReceiveOrderCode()).isNull();

    }


}
