package ru.yandex.market.global.checkout.order;

import java.math.BigDecimal;
import java.util.function.Function;

import lombok.RequiredArgsConstructor;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import ru.yoomoney.tech.dbqueue.config.DatabaseAccessLayer;
import ru.yoomoney.tech.dbqueue.config.QueueShard;

import ru.yandex.market.global.checkout.BaseFunctionalTest;
import ru.yandex.market.global.checkout.api.AdminApiService;
import ru.yandex.market.global.checkout.api.OrderApiService;
import ru.yandex.market.global.checkout.domain.order.OrderModel;
import ru.yandex.market.global.checkout.domain.queue.task.deliveries.UpdateCargoClaimInfoConsumer;
import ru.yandex.market.global.checkout.factory.TestOrderFactory;
import ru.yandex.market.global.checkout.queue.TestQueueTaskRunner;
import ru.yandex.market.global.db.jooq.enums.EDeliveryOrderState;
import ru.yandex.market.global.db.jooq.enums.EOrderState;
import ru.yandex.market.global.db.jooq.tables.pojos.OrderDelivery;
import ru.yandex.mj.generated.client.taxi_v1_intergration.api.PerformerApiClient;
import ru.yandex.mj.generated.client.taxi_v1_intergration.model.PerformerPositionResponse;
import ru.yandex.mj.generated.client.taxi_v1_intergration.model.PerformerPositionResponsePosition;
import ru.yandex.mj.generated.client.taxi_v2_intergration.api.ClaimsApiClient;
import ru.yandex.mj.generated.client.taxi_v2_intergration.model.InlineResponse200;
import ru.yandex.mj.generated.client.taxi_v2_intergration.model.SearchedClaimMP;
import ru.yandex.mj.generated.server.model.AdminOrderDto;
import ru.yandex.mj.generated.server.model.GeoPointDto;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class OrderUpdateCoordsTest extends BaseFunctionalTest {

    public static final BigDecimal LAT = new BigDecimal("4436.3467");
    public static final BigDecimal LON = new BigDecimal("4000.3467");
    public static final String CLAIM_ID = "test00100010010001";

    private final OrderApiService orderApiService;
    private final AdminApiService adminApiService;
    private final QueueShard<DatabaseAccessLayer> shard;
    private final UpdateCargoClaimInfoConsumer consumer;
    private final PerformerApiClient performerApiClient;
    private final ClaimsApiClient claimsApiClient;

    private final TestOrderFactory testOrderFactory;

    @BeforeEach
    void testNotification() {
        Mockito.when(performerApiClient.integrationV1ClaimsPerformerPosition(Mockito.any(), Mockito.any()).schedule().join())
                .thenReturn(new PerformerPositionResponse().position(
                        new PerformerPositionResponsePosition().lat(LAT).lon(LON)));
        Mockito.when(claimsApiClient.integrationV2ClaimsSearch(Mockito.any(), Mockito.any(), Mockito.any())
                .schedule().join()).thenReturn(
                new InlineResponse200().addClaimsItem(
                        new SearchedClaimMP().id(CLAIM_ID)));
    }

    private OrderModel insertOrder(Function<OrderDelivery, OrderDelivery> delivery) {
        return testOrderFactory.createOrder(
                TestOrderFactory.CreateOrderBuilder.builder()
                        .setupOrder(it -> it
                                .setOrderState(EOrderState.PROCESSING)
                                .setDeliveryState(EDeliveryOrderState.DELIVERING_ORDER))
                        .setupDelivery(delivery)
                        .build()
        );
    }

    @Test
    public void testCourierLocation() {
        OrderModel order = insertOrder(it -> it.setCourierPosition(null));
        Long orderId = order.getOrder().getId();

        ResponseEntity<GeoPointDto> dto = orderApiService.apiV1OrderCourierPositionGet(orderId);

        Assertions.assertThat(dto.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        Assertions.assertThat(dto.getBody()).isNull();

        TestQueueTaskRunner.runTaskOnceThrowOnFail(consumer, orderId);

        dto = orderApiService.apiV1OrderCourierPositionGet(orderId);

        GeoPointDto expected = new GeoPointDto().lat(LAT.doubleValue()).lon(LON.doubleValue());

        Assertions.assertThat(dto.getStatusCode()).isEqualTo(HttpStatus.OK);
        Assertions.assertThat(dto.getBody()).usingRecursiveComparison()
                .ignoringExpectedNullFields().isEqualTo(expected);
    }


    @Test
    public void testClaimId() {
        OrderModel order = insertOrder(
                it -> it.setCourierPosition(null).setClaimId(null)
        );

        Long orderId = order.getOrder().getId();
        AdminOrderDto dto = adminApiService.apiV1AdminOrderGetGet(orderId).getBody();

        Assertions.assertThat(dto).isNotNull();
        Assertions.assertThat(dto.getClaimId()).isNull();

        TestQueueTaskRunner.runTaskOnceThrowOnFail(consumer, orderId);

        dto = adminApiService.apiV1AdminOrderGetGet(orderId).getBody();

        Assertions.assertThat(dto).isNotNull();
        Assertions.assertThat(dto.getClaimId()).isEqualTo(CLAIM_ID);
    }

}
