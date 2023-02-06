package ru.yandex.market.logistics.lom.service.tracker.listener.implementation;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.logistic.gateway.client.DeliveryClient;
import ru.yandex.market.logistic.gateway.common.model.common.Partner;
import ru.yandex.market.logistics.lom.configuration.properties.FeatureProperties;
import ru.yandex.market.logistics.lom.dto.queue.LomSegmentCheckpoint;
import ru.yandex.market.logistics.lom.entity.CancellationOrderRequest;
import ru.yandex.market.logistics.lom.entity.Order;
import ru.yandex.market.logistics.lom.entity.enums.CancellationOrderStatus;
import ru.yandex.market.logistics.lom.entity.enums.PartnerType;
import ru.yandex.market.logistics.lom.entity.enums.SegmentStatus;
import ru.yandex.market.logistics.lom.jobs.model.OrderIdWaybillSegmentPayload;
import ru.yandex.market.logistics.lom.jobs.model.QueueType;
import ru.yandex.market.logistics.lom.jobs.processor.CallCourierProcessor;
import ru.yandex.market.logistics.lom.repository.CancellationOrderRequestRepository;
import ru.yandex.market.logistics.lom.service.order.OrderService;
import ru.yandex.market.logistics.lom.utils.jobs.PayloadFactory;

import static org.mockito.Mockito.verify;

@DisplayName("Обработка 110 чекпоинта: заказ поступил на склад")
@DatabaseSetup("/service/listener/orderPackaging/before/setup.xml")
class FfExpressOrderPackagingListenerTest extends AbstractCheckpointListenerTest {
    @Autowired
    private FfExpressOrderPackagingListener ffExpressOrderPackagingListener;
    @Autowired
    private OrderService orderService;
    @Autowired
    private TransactionTemplate transactionTemplate;
    @Autowired
    private FeatureProperties featureProperties;
    @Autowired
    private DeliveryClient deliveryClient;
    @Autowired
    private CallCourierProcessor callCourierProcessor;
    @Autowired
    private CancellationOrderRequestRepository cancellationOrderRequestRepository;

    private static final OrderIdWaybillSegmentPayload PAYLOAD =
        PayloadFactory.createWaybillSegmentPayload(1, 2, "1", 1);
    private static final Duration EXPECTED_DURATION = Duration.ofSeconds(41400L);

    @BeforeEach
    void setUp() {
        featureProperties.setEnabledCallCourierForExpressOn110Checkpoint(true);
        clock.setFixed(Instant.parse("2020-11-02T02:30:00.00Z"), ZoneId.systemDefault());
    }

    @AfterEach
    void tearDown() {
        featureProperties.setEnabledCallCourierForExpressOn110Checkpoint(false);
    }

    @Test
    @DisplayName("Курьер вызван")
    @ExpectedDatabase(
        value = "/service/listener/orderPackaging/after/courier_is_called.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void testCourierCalled() {
        transactionTemplate.execute(arg -> {
            ffExpressOrderPackagingListener.apply(
                orderService.getOrderOrThrow(1L),
                List.of(create110Checkpoint()),
                create110Checkpoint(),
                null
            );
            return null;
        });
        queueTaskChecker.assertQueueTaskCreatedWithDelay(QueueType.CALL_COURIER, PAYLOAD, EXPECTED_DURATION);

        callCourierProcessor.processPayload(PAYLOAD);

        verify(deliveryClient).callCourier(
            ru.yandex.market.logistic.gateway.common.model.common.ResourceId.builder()
                .setYandexId("LO1")
                .setPartnerId("ds_external_id")
                .build(),
            Duration.ofSeconds(300),
            new Partner(48L)
        );
    }

    @Test
    @DisplayName("Курьер вызван только один раз: два 110 чекпоинта")
    @ExpectedDatabase(
        value = "/service/listener/orderPackaging/after/courier_is_called.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void testCourierCalledOnlyOnce() {
        transactionTemplate.execute(arg -> {
            ffExpressOrderPackagingListener.apply(
                orderService.getOrderOrThrow(1L),
                List.of(create110Checkpoint()),
                create110Checkpoint(),
                null
            );
            ffExpressOrderPackagingListener.apply(
                orderService.getOrderOrThrow(1L),
                List.of(create110Checkpoint()),
                create110Checkpoint(),
                null
            );
            return null;
        });

        queueTaskChecker.assertQueueTaskCreatedWithDelay(QueueType.CALL_COURIER, PAYLOAD, EXPECTED_DURATION);
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @DisplayName("Курьер не вызван: неподходящий чекпоинт")
    @EnumSource(value = SegmentStatus.class, names = {"IN"}, mode = EnumSource.Mode.EXCLUDE)
    void testNot110Checkpoint(SegmentStatus status) {
        transactionTemplate.execute(arg -> {
            ffExpressOrderPackagingListener.apply(
                orderService.getOrderOrThrow(1L),
                List.of(create110Checkpoint()),
                createCheckpoint(status),
                null
            );
            return null;
        });

        queueTaskChecker.assertNoQueueTasksCreated();
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @DisplayName("Курьер не вызван: неподходящий тип партнёра")
    @EnumSource(value = PartnerType.class, names = {"FULFILLMENT"}, mode = EnumSource.Mode.EXCLUDE)
    void testNotFFSegment(PartnerType partnerType) {
        transactionTemplate.execute(arg -> {
            Order order = orderService.getOrderOrThrow(1L);
            order.getWaybill().get(0).setPartnerType(partnerType);
            ffExpressOrderPackagingListener.apply(
                order,
                List.of(create110Checkpoint()),
                create110Checkpoint(),
                null
            );
            return null;
        });

        queueTaskChecker.assertNoQueueTasksCreated();
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @DisplayName("Курьер не вызван: неподходящий тип следующего за ФФ партнёра")
    @EnumSource(value = PartnerType.class, names = {"DELIVERY"}, mode = EnumSource.Mode.EXCLUDE)
    void testNextSegmentNotDeliverySegment(PartnerType nextSegmentPartnerType) {
        transactionTemplate.execute(arg -> {
            Order order = orderService.getOrderOrThrow(1L);
            order.getWaybill().get(1).setPartnerType(nextSegmentPartnerType);
            ffExpressOrderPackagingListener.apply(
                order,
                List.of(create110Checkpoint()),
                create110Checkpoint(),
                null
            );
            return null;
        });

        queueTaskChecker.assertNoQueueTasksCreated();
    }

    @Test
    @DisplayName("Курьер не вызван: DELIVERY сегмент не содержит тэг экспресса")
    void testDeliverySegmentNotExpress() {
        featureProperties.setEnabledCallCourierForExpressOn110Checkpoint(true);

        transactionTemplate.execute(arg -> {
            Order order = orderService.getOrderOrThrow(1L);
            order.getWaybill().get(1).getWaybillSegmentTags().clear();
            ffExpressOrderPackagingListener.apply(
                order,
                List.of(create110Checkpoint()),
                create110Checkpoint(),
                null
            );
            return null;
        });

        queueTaskChecker.assertNoQueueTasksCreated();
    }

    @Test
    @DisplayName("Курьер не вызван: не DARKSTORE")
    void testFfNotDarkstore() {
        transactionTemplate.execute(arg -> {
            Order order = orderService.getOrderOrThrow(1L);
            order.getWaybill().get(0).setPartnerSubtype(null);
            ffExpressOrderPackagingListener.apply(
                order,
                List.of(create110Checkpoint()),
                create110Checkpoint(),
                null
            );
            return null;
        });

        queueTaskChecker.assertNoQueueTasksCreated();
    }

    @Test
    @DisplayName("Курьер не вызван: есть активная заявка на отмену")
    void testOrderHasActiveCancellationRequest() {
        transactionTemplate.execute(arg -> {
            Order order = orderService.getOrderOrThrow(1L);
            cancellationOrderRequestRepository.save(
                new CancellationOrderRequest()
                    .setOrder(order)
                    .setStatus(CancellationOrderStatus.SUCCESS)
            );
            ffExpressOrderPackagingListener.apply(
                order,
                List.of(create110Checkpoint()),
                create110Checkpoint(),
                null
            );
            return null;
        });

        queueTaskChecker.assertNoQueueTasksCreated();
    }

    @Test
    @DisplayName("Курьер не вызван: FeatureProperty.enabledCallCourierForExpressOn110Checkpoint=false")
    void testCourierNotCalled() {
        featureProperties.setEnabledCallCourierForExpressOn110Checkpoint(false);

        transactionTemplate.execute(arg -> {
            ffExpressOrderPackagingListener.apply(
                orderService.getOrderOrThrow(1L),
                List.of(create110Checkpoint()),
                create110Checkpoint(),
                null
            );
            return null;
        });

        queueTaskChecker.assertNoQueueTasksCreated();
    }

    @Nonnull
    private LomSegmentCheckpoint create110Checkpoint() {
        return createCheckpoint(SegmentStatus.IN);
    }
}
