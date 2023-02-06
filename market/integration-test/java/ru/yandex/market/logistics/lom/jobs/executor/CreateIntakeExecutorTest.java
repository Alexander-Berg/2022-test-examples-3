package ru.yandex.market.logistics.lom.jobs.executor;

import java.time.Instant;
import java.time.ZoneId;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.quartz.JobExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.lom.AbstractContextualTest;
import ru.yandex.market.logistics.lom.jobs.model.QueueType;
import ru.yandex.market.logistics.lom.jobs.producer.DeliveryServiceShipmentPayloadProducer;
import ru.yandex.market.logistics.lom.jobs.producer.FulfillmentShipmentPayloadProducer;
import ru.yandex.market.logistics.lom.service.partner.LogisticsPointsService;
import ru.yandex.market.logistics.lom.service.shipment.ShipmentService;
import ru.yandex.market.logistics.management.entity.response.point.LogisticsPointResponse;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static ru.yandex.market.logistics.lom.utils.jobs.PayloadFactory.createShipmentApplicationIdPayload;

@DisplayName("Тесты джобы создания заявок на забор")
class CreateIntakeExecutorTest extends AbstractContextualTest {
    @Autowired
    private DeliveryServiceShipmentPayloadProducer deliveryServiceShipmentPayloadProducer;

    @Autowired
    private FulfillmentShipmentPayloadProducer fulfillmentShipmentPayloadProducer;

    @Autowired
    @Mock
    private LogisticsPointsService logisticsPointsService;

    private CreateIntakeExecutor executor;

    @Autowired
    private ShipmentService shipmentService;

    @BeforeEach
    void setup() {
        MockitoAnnotations.initMocks(this);
        when(logisticsPointsService.getLogisticsPointById(anyLong()))
            .thenReturn(LogisticsPointResponse.newBuilder().build());
        executor = new CreateIntakeExecutor(
            shipmentService,
            deliveryServiceShipmentPayloadProducer,
            fulfillmentShipmentPayloadProducer
        );
    }

    @Test
    @DatabaseSetup("/jobs/executor/createintake/before/shipment_application_new.xml")
    @DisplayName("Продюсить таски для новых отгрузок на завтра, которые не в очереди")
    void produceTasksForNewIntakes() {
        JobExecutionContext context = Mockito.mock(JobExecutionContext.class);
        clock.setFixed(Instant.parse("2019-05-24T00:00:00.00Z"), ZoneId.systemDefault());
        executor.doJob(context);
        assertQueueTasksCreated();
    }

    @Test
    @DatabaseSetup("/jobs/executor/createintake/before/shipment_application_new_in_queue.xml")
    @DisplayName("Не продюсить таски для новых отгрузок на завтра, которые уже в очереди")
    void produceNoTasksForNewIntakesThatAreInQueue() {
        JobExecutionContext context = Mockito.mock(JobExecutionContext.class);
        clock.setFixed(Instant.parse("2019-05-24T00:00:00.00Z"), ZoneId.systemDefault());
        executor.doJob(context);
        queueTaskChecker.assertNoQueueTasksCreated();
    }

    @Test
    @DatabaseSetup("/jobs/executor/createintake/before/shipment_application_new.xml")
    @DisplayName("Не продюсить таски для второго запуска джобы")
    void produceNoTasksForDoubleJobRun() {
        JobExecutionContext context = Mockito.mock(JobExecutionContext.class);
        clock.setFixed(Instant.parse("2019-05-24T00:00:00.00Z"), ZoneId.systemDefault());
        executor.doJob(context);
        clock.setFixed(Instant.parse("2019-05-24T01:00:00.00Z"), ZoneId.systemDefault());
        executor.doJob(context);
        assertQueueTasksCreated();
    }

    private void assertQueueTasksCreated() {
        queueTaskChecker.assertQueueTaskCreated(
            QueueType.DELIVERY_SERVICE_SHIPMENT_CREATION,
            createShipmentApplicationIdPayload(2L, "2", 2L)
        );
        queueTaskChecker.assertQueueTaskCreated(
            QueueType.DELIVERY_SERVICE_SHIPMENT_CREATION,
            createShipmentApplicationIdPayload(1L, "3", 3L)
        );
        queueTaskChecker.assertQueueTaskCreated(
            QueueType.FULFILLMENT_SHIPMENT_CREATION,
            createShipmentApplicationIdPayload(3L, "1", 1L)
        );
    }
}
