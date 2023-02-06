package ru.yandex.market.logistics.lom.service;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.lom.AbstractContextualTest;
import ru.yandex.market.logistics.lom.jobs.model.QueueType;
import ru.yandex.market.logistics.lom.jobs.processor.ProcessWaybillService;
import ru.yandex.market.logistics.lom.utils.jobs.PayloadFactory;

@DisplayName("Обработка сегментов waybill заказа")
public class ProcessWaybillServiceTest extends AbstractContextualTest {

    @Autowired
    private ProcessWaybillService processWaybillService;

    @Test
    @DisplayName("Отмененный заказ")
    @DatabaseSetup("/service/order/before/cancelled_order.xml")
    void processCancelledOrder() {
        processWaybillService.processPayload(PayloadFactory.createWaybillSegmentPayload(1L, 1L));

        queueTaskChecker.assertNoQueueTasksCreated();
    }

    @Test
    @DisplayName("Процессинг СЦ сегмента")
    @DatabaseSetup("/service/order/before/sc_segment_order.xml")
    void processSortingCenter() {
        processWaybillService.processPayload(PayloadFactory.createWaybillSegmentPayload(1L, 1L));

        queueTaskChecker.assertQueueTaskCreated(
            QueueType.FULFILLMENT_CREATE_ORDER_EXTERNAL,
            PayloadFactory.createWaybillSegmentPayload(1L, 1L, "1", 1)
        );
    }

    @Test
    @DisplayName("Процессинг сегмента кроссдок-поставщика")
    @DatabaseSetup("/service/order/before/supplier_segment_order.xml")
    void processSupplierOrder() {
        processWaybillService.processPayload(PayloadFactory.createWaybillSegmentPayload(1L, 1L));

        queueTaskChecker.assertNoQueueTasksCreated();
    }
}
