package ru.yandex.market.logistics.lom.service;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import ru.yandex.market.logistic.gateway.client.FulfillmentClient;
import ru.yandex.market.logistic.gateway.common.model.common.Partner;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.ResourceId;
import ru.yandex.market.logistics.lom.AbstractContextualTest;
import ru.yandex.market.logistics.lom.jobs.model.OrderIdWaybillSegmentPayload;
import ru.yandex.market.logistics.lom.jobs.model.QueueType;
import ru.yandex.market.logistics.lom.jobs.processor.ProcessOrderPlacesChangedService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static ru.yandex.market.logistics.lom.utils.jobs.PayloadFactory.createChangeOrderRequestPayload;
import static ru.yandex.market.logistics.lom.utils.jobs.PayloadFactory.createWaybillSegmentPayload;
import static ru.yandex.market.logistics.lom.utils.jobs.PayloadFactory.lesOrderEventPayload;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.errorMessage;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DisplayName("Процессинг 118го статуса от ФФ")
class ProcessOrderPlacesChangedServiceTest extends AbstractContextualTest {

    private static final OrderIdWaybillSegmentPayload FF_SEGMENT_PAYLOAD =
        createWaybillSegmentPayload(1, 1, "45", 1);

    @Autowired
    private FulfillmentClient fulfillmentClient;

    @Autowired
    private ProcessOrderPlacesChangedService processOrderPlacesChangedService;

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(fulfillmentClient);
    }

    @Test
    @DisplayName("Поставлена задача на изменение грузомест: поменялся ID")
    @DatabaseSetup("/service/process_order_places_changed/before/ff_setup.xml")
    @DatabaseSetup(
        value = "/service/common/before/waybill_segment_storage_unit_sequential.xml",
        type = DatabaseOperation.UPDATE
    )
    @ExpectedDatabase(
        value = "/service/process_order_places_changed/after/ff_places.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void success() throws Exception {
        processOrderPlacesChangedService.processPayload(FF_SEGMENT_PAYLOAD);

        verify(fulfillmentClient).getOrder(
            eq(ResourceId.builder().setYandexId("2-LOinttest-1").setPartnerId("FULFILLMENT-2-LOinttest-1").build()),
            eq(new Partner(172L)),
            any()
        );

        mockMvc.perform(
                post("/orders/ff/get/success")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(extractFileContent("service/process_order_places_changed/response/fulfillment_order.json"))
            )
            .andExpect(status().isOk());

        queueTaskChecker.assertQueueTaskCreated(
            QueueType.EXPORT_ORDER_BOXES_CHANGED,
            lesOrderEventPayload(1, 1, "1", 1)
        );
        queueTaskChecker.assertQueueTaskCreated(
            QueueType.CHANGE_ORDER_REQUEST,
            createChangeOrderRequestPayload(1, "2", 2)
        );
    }

    @Test
    @DisplayName("Отсутствует root на waybill сегменте")
    @DatabaseSetup("/service/process_order_places_changed/before/ff_setup.xml")
    void noRootStorageUnit() throws Exception {
        processOrderPlacesChangedService.processPayload(FF_SEGMENT_PAYLOAD);

        verify(fulfillmentClient).getOrder(
            eq(ResourceId.builder().setYandexId("2-LOinttest-1").setPartnerId("FULFILLMENT-2-LOinttest-1").build()),
            eq(new Partner(172L)),
            any()
        );

        mockMvc.perform(
                post("/orders/ff/get/success")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(extractFileContent("service/process_order_places_changed/response/fulfillment_order.json"))
            )
            .andExpect(status().isInternalServerError())
            .andExpect(errorMessage("No root unit"));

        queueTaskChecker.assertNoQueueTasksCreated();
    }

    @Test
    @DisplayName("Не поменялись грузоместа")
    @DatabaseSetup("/service/process_order_places_changed/before/ff_setup.xml")
    @DatabaseSetup(
        value = "/service/common/before/waybill_segment_storage_unit_sequential.xml",
        type = DatabaseOperation.UPDATE
    )
    void notChangedStorageUnits() throws Exception {
        processOrderPlacesChangedService.processPayload(FF_SEGMENT_PAYLOAD);

        verify(fulfillmentClient).getOrder(
            eq(ResourceId.builder().setYandexId("2-LOinttest-1").setPartnerId("FULFILLMENT-2-LOinttest-1").build()),
            eq(new Partner(172L)),
            any()
        );

        mockMvc.perform(
                post("/orders/ff/get/success")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(extractFileContent(
                        "service/process_order_places_changed/response/fulfillment_order_no_places_changed.json"
                    ))
            )
            .andExpect(status().isOk());

        queueTaskChecker.assertNoQueueTasksCreated();
    }
}
