package ru.yandex.market.logistics.lom.service.process;

import java.time.Duration;
import java.time.Instant;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.common.util.DateTimeUtils;
import ru.yandex.market.logistics.lom.AbstractContextualTest;
import ru.yandex.market.logistics.lom.checker.QueueTaskChecker;
import ru.yandex.market.logistics.lom.entity.Order;
import ru.yandex.market.logistics.lom.entity.WaybillSegment;
import ru.yandex.market.logistics.lom.entity.enums.SegmentType;
import ru.yandex.market.logistics.lom.jobs.model.OrderIdWaybillSegmentPayload;
import ru.yandex.market.logistics.lom.jobs.model.QueueType;
import ru.yandex.market.logistics.lom.service.order.OrderService;
import ru.yandex.market.logistics.lom.utils.WaybillUtils;
import ru.yandex.market.logistics.lom.utils.jobs.PayloadFactory;

@DisplayName("Создание таски на вызов курьера")
@DatabaseSetup("/service/call_courier_payload/before/order.xml")
class CallCourierPayloadServiceTest extends AbstractContextualTest {
    @Autowired
    private QueueTaskChecker taskChecker;

    @Autowired
    private CallCourierPayloadService callCourierPayloadService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private TransactionTemplate transactionTemplate;

    private static final long ORDER_ID = 1L;
    private static final long WAYBILL_SEGMENT_ID = 2L;
    private static final OrderIdWaybillSegmentPayload PAYLOAD = PayloadFactory
        .createWaybillSegmentPayload(ORDER_ID, WAYBILL_SEGMENT_ID, "1", 1L);
    private static final Duration EXPECTED_DURATION = Duration.ofSeconds(5410L);

    @BeforeEach
    void setUp() {
        clock.setFixed(Instant.parse("2021-07-13T12:00:00.00Z"), DateTimeUtils.MOSCOW_ZONE);
    }

    @Test
    @DisplayName("Создание отложенной таски")
    @DatabaseSetup(
        value = "/service/call_courier_payload/before/waybill_segment_with_delay.xml",
        type = DatabaseOperation.INSERT
    )
    void produceTaskWithDelay() {
        callProduceTask();
        checkTaskWasProducedWithDelay(EXPECTED_DURATION);
    }

    @Test
    @DatabaseSetup(
        value = "/service/call_courier_payload/before/waybill_segment_with_zero_delay.xml",
        type = DatabaseOperation.INSERT
    )
    @DisplayName("Создание таски без задержки, так как время вызова курьера меньше текущего времени")
    void produceTaskImmediatelyCourierTimeBeforeNow() {
        callProduceTask();
        checkTaskWasProducedWithDelay(Duration.ZERO);
    }

    @Test
    @DatabaseSetup(
        value = "/service/call_courier_payload/before/waybill_segment_without_call_courier_time.xml",
        type = DatabaseOperation.INSERT
    )
    @DisplayName("Создание таски без задержки, так как время вызова курьера не установлено")
    void produceTaskImmediatelyNoCourierTime() {
        callProduceTask();
        checkTaskWasProducedWithDelay(Duration.ZERO);
    }

    private void callProduceTask() {
        transactionTemplate.execute(arg -> {
            Order order = orderService.findById(ORDER_ID);
            WaybillSegment courierSegment =
                WaybillUtils.getFirstSegmentByType(order, SegmentType.COURIER).orElseThrow();
            callCourierPayloadService.produceTaskIfNeeded(order, courierSegment);
            return 0;
        });
    }

    private void checkTaskWasProducedWithDelay(@Nonnull Duration expectedDelay) {
        taskChecker.assertQueueTaskCreatedWithDelay(
            QueueType.CALL_COURIER,
            PAYLOAD,
            expectedDelay
        );
    }

    @Test
    @DisplayName("Вызов курьера не может быть осуществлён, таска не создаётся")
    @DatabaseSetup(
        value = "/service/call_courier_payload/before/wrong_preconditions_for_call_courier_task.xml",
        type = DatabaseOperation.INSERT
    )
    void noNeedToCallCourier() {
        callProduceTask();
        taskChecker.assertQueueTaskNotCreated(QueueType.CALL_COURIER);
    }
}
