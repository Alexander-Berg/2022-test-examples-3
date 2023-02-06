package ru.yandex.market.logistics.lom.service.tracker.listener.implementation;

import java.util.List;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import lombok.SneakyThrows;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.logistic.gateway.client.FulfillmentClient;
import ru.yandex.market.logistic.gateway.common.model.common.Partner;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.ResourceId;
import ru.yandex.market.logistic.gateway.common.model.properties.ClientRequestMeta;
import ru.yandex.market.logistics.lom.dto.queue.LomSegmentCheckpoint;
import ru.yandex.market.logistics.lom.entity.Order;
import ru.yandex.market.logistics.lom.entity.enums.CancellationSegmentStatus;
import ru.yandex.market.logistics.lom.entity.enums.PartnerType;
import ru.yandex.market.logistics.lom.entity.enums.SegmentStatus;
import ru.yandex.market.logistics.lom.jobs.consumer.ProcessSegmentCancellationConsumer;
import ru.yandex.market.logistics.lom.jobs.model.QueueType;
import ru.yandex.market.logistics.lom.jobs.model.SegmentCancellationRequestIdPayload;
import ru.yandex.market.logistics.lom.service.order.OrderCancellationService;
import ru.yandex.market.logistics.lom.service.order.OrderService;
import ru.yandex.market.logistics.lom.utils.jobs.PayloadFactory;
import ru.yandex.market.logistics.lom.utils.jobs.TaskFactory;
import ru.yandex.money.common.dbqueue.api.Task;

import static org.mockito.Mockito.verify;

@DisplayName("Обработка 101 чекпоинта: ретрай отмены на фф сегмента")
@DatabaseSetup("/service/listener/orderCancellation/before/setup.xml")
class FfExpressOrderCancellationListenerTest extends AbstractCheckpointListenerTest {
    @Autowired
    private FfExpressOrderCancellationListener ffExpressOrderCancellationListener;
    @Autowired
    private OrderService orderService;
    @Autowired
    private OrderCancellationService orderCancellationServices;
    @Autowired
    private TransactionTemplate transactionTemplate;
    @Autowired
    private FulfillmentClient fulfillmentClient;
    @Autowired
    private ProcessSegmentCancellationConsumer processSegmentCancellationConsumer;

    @ParameterizedTest
    @DisplayName("Новая задача на отмену создана")
    @EnumSource(value = CancellationSegmentStatus.class, names = {"FAIL", "TECH_FAIL"})
    @ExpectedDatabase(
        value = "/service/listener/orderCancellation/after/task_created.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @SneakyThrows
    void testTaskCreated(CancellationSegmentStatus status) {
        transactionTemplate.execute(arg -> {
            orderCancellationServices.getCancellationSegmentRequestByIdOrThrow(1).setStatus(status);
            ffExpressOrderCancellationListener.apply(
                orderService.getOrderOrThrow(1L),
                List.of(create101Checkpoint()),
                create101Checkpoint(),
                null
            );
            return null;
        });

        SegmentCancellationRequestIdPayload segmentCancellationRequestIdPayload =
            PayloadFactory.createSegmentCancellationRequestIdPayload(1, "1", 1);
        Task<SegmentCancellationRequestIdPayload> task = TaskFactory.createTask(
            QueueType.PROCESS_WAYBILL_SEGMENT_UPDATE_COURIER,
            segmentCancellationRequestIdPayload
        );
        processSegmentCancellationConsumer.execute(task);

        verify(fulfillmentClient).cancelOrder(
            ResourceId.builder()
                .setYandexId("LO1")
                .setPartnerId("ff_external_id")
                .build(),
            new Partner(1L),
            new ClientRequestMeta("1")
        );
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @DisplayName("Новая задача на отмену не создана: неподходящий тип партнёра")
    @EnumSource(value = PartnerType.class, names = {"FULFILLMENT"}, mode = EnumSource.Mode.EXCLUDE)
    @ExpectedDatabase(
        value = "/service/listener/orderCancellation/after/task_not_created.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void testNotFFSegment(PartnerType partnerType) {
        transactionTemplate.execute(arg -> {
            Order order = orderService.getOrderOrThrow(1L);
            order.getWaybill().get(0).setPartnerType(partnerType);
            ffExpressOrderCancellationListener.apply(
                order,
                List.of(create101Checkpoint()),
                create101Checkpoint(),
                null
            );
            return null;
        });
    }

    @Test
    @DisplayName("Новая задача на отмену не создана: партнер не имеет subtype DARKSTORE")
    @ExpectedDatabase(
        value = "/service/listener/orderCancellation/after/task_not_created.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void testNotDarkstore() {
        transactionTemplate.execute(arg -> {
            Order order = orderService.getOrderOrThrow(1L);
            order.getWaybill().get(0).setPartnerSubtype(null);
            ffExpressOrderCancellationListener.apply(
                orderService.getOrderOrThrow(1L),
                List.of(create101Checkpoint()),
                create101Checkpoint(),
                null
            );
            return null;
        });
    }

    @Test
    @DisplayName("Новая задача на отмену не создана: нет активной заявки для отмены заказа")
    @DatabaseSetup(
        value = "/service/listener/orderCancellation/before/not_active_cancellation_order_request.xml",
        type = DatabaseOperation.UPDATE
    )
    @ExpectedDatabase(
        value = "/service/listener/orderCancellation/after/task_not_created.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void testNoActiveCancellationOrderRequest() {
        transactionTemplate.execute(arg -> {
            ffExpressOrderCancellationListener.apply(
                orderService.getOrderOrThrow(1L),
                List.of(create101Checkpoint()),
                create101Checkpoint(),
                null
            );
            return null;
        });
    }

    @Test
    @DisplayName("Новая задача на отмену не создана: нет упавшей заявки на отмену на сегменте")
    @DatabaseSetup(
        value = "/service/listener/orderCancellation/before/not_failed_cancellation_segment_request.xml",
        type = DatabaseOperation.UPDATE
    )
    @ExpectedDatabase(
        value = "/service/listener/orderCancellation/after/task_not_created.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void testNoFailedCancellationSegmentRequest() {
        transactionTemplate.execute(arg -> {
            ffExpressOrderCancellationListener.apply(
                orderService.getOrderOrThrow(1L),
                List.of(create101Checkpoint()),
                create101Checkpoint(),
                null
            );
            return null;
        });
    }

    @Nonnull
    private LomSegmentCheckpoint create101Checkpoint() {
        return createCheckpoint(SegmentStatus.INFO_RECEIVED);
    }
}
