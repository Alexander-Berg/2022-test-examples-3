package ru.yandex.market.logistics.lom.service;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.logistic.gateway.client.FulfillmentClient;
import ru.yandex.market.logistic.gateway.common.model.common.Partner;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.ResourceId;
import ru.yandex.market.logistics.lom.AbstractContextualTest;
import ru.yandex.market.logistics.lom.controller.order.OrderHistoryTestUtil;
import ru.yandex.market.logistics.lom.jobs.model.OrderIdWaybillSegmentPayload;
import ru.yandex.market.logistics.lom.jobs.model.QueueType;
import ru.yandex.market.logistics.lom.jobs.processor.ProcessOrderReadyToShipService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static ru.yandex.market.logistics.lom.utils.jobs.PayloadFactory.createChangeOrderRequestPayload;
import static ru.yandex.market.logistics.lom.utils.jobs.PayloadFactory.createWaybillSegmentPayload;
import static ru.yandex.market.logistics.lom.utils.jobs.PayloadFactory.lesOrderEventPayload;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@ParametersAreNonnullByDefault
@DisplayName("Обработка 120го статуса")
class ProcessOrderReadyToShipServiceTest extends AbstractContextualTest {

    private static final OrderIdWaybillSegmentPayload FF_SEGMENT_PAYLOAD = createWaybillSegmentPayload(1, 1, "45", 1);

    @Autowired
    private FulfillmentClient fulfillmentClient;

    @Autowired
    private ProcessOrderReadyToShipService processOrderReadyToShipService;

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(fulfillmentClient);
    }

    @Test
    @DisplayName("Процессинг 120го статуса от дропшипа")
    @DatabaseSetup("/service/process_order_readytoship/before/dropship_setup.xml")
    @DatabaseSetup(
        value = "/service/common/before/waybill_segment_storage_unit_sequential.xml",
        type = DatabaseOperation.UPDATE
    )
    @ExpectedDatabase(
        value = "/service/process_order_readytoship/after/dropship_places.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/service/process_order_readytoship/after/dropship_change_requests.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void updateOrderDropship() throws Exception {
        processOrderReadyToShipService.processPayload(FF_SEGMENT_PAYLOAD);

        verify(fulfillmentClient).getOrder(
            eq(ResourceId.builder().setYandexId("2-LOinttest-1").setPartnerId("DROPSHIP-2-LOinttest-1").build()),
            eq(new Partner(47755L)),
            any()
        );

        mockMvc.perform(
            post("/orders/ff/get/success")
                .contentType(MediaType.APPLICATION_JSON)
                .content(extractFileContent("service/process_order_readytoship/response/dropship_order.json"))
        )
            .andExpect(status().isOk());

        queueTaskChecker.assertQueueTasksCreated(QueueType.CHANGE_ORDER_REQUEST, 2);
    }

    @Test
    @DisplayName("Процессинг 120го статуса от дропшипа при уже существующем активном ChangeOrderRequest-е на коробки")
    @DatabaseSetup("/service/process_order_readytoship/before/dropship_setup.xml")
    @DatabaseSetup(
        value = "/service/common/before/waybill_segment_storage_unit_sequential.xml",
        type = DatabaseOperation.UPDATE
    )
    @DatabaseSetup(
        value = "/service/process_order_readytoship/before/active_change_request.xml",
        type = DatabaseOperation.INSERT
    )
    @ExpectedDatabase(
        value = "/service/process_order_readytoship/after/dropship_places.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/service/process_order_readytoship/after/dropship_change_requests_active.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void updateOrderDropshipActivePlacesChangeRequestExists() throws Exception {
        processOrderReadyToShipService.processPayload(FF_SEGMENT_PAYLOAD);

        verify(fulfillmentClient).getOrder(
            eq(ResourceId.builder().setYandexId("2-LOinttest-1").setPartnerId("DROPSHIP-2-LOinttest-1").build()),
            eq(new Partner(47755L)),
            any()
        );

        mockMvc.perform(
            post("/orders/ff/get/success")
                .contentType(MediaType.APPLICATION_JSON)
                .content(extractFileContent("service/process_order_readytoship/response/dropship_order.json"))
        )
            .andExpect(status().isOk());

        assertQueueTasks();
    }

    @Test
    @DisplayName("Процессинг 120го статуса от дропшипа с places = null")
    @DatabaseSetup("/service/process_order_readytoship/before/dropship_setup.xml")
    @DatabaseSetup(
        value = "/service/common/before/waybill_segment_storage_unit_sequential.xml",
        type = DatabaseOperation.UPDATE
    )
    @ExpectedDatabase(
        value = "/service/process_order_readytoship/after/null_places.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void updateOrderDropshipNullPlaces() throws Exception {
        processOrderReadyToShipService.processPayload(FF_SEGMENT_PAYLOAD);

        verify(fulfillmentClient).getOrder(
            eq(ResourceId.builder().setYandexId("2-LOinttest-1").setPartnerId("DROPSHIP-2-LOinttest-1").build()),
            eq(new Partner(47755L)),
            any()
        );

        mockMvc.perform(
            post("/orders/ff/get/success")
                .contentType(MediaType.APPLICATION_JSON)
                .content(extractFileContent(
                    "service/process_order_readytoship/response/dropship_order_with_no_places.json"
                ))
        )
            .andExpect(status().isOk());

        assertQueueTasks();
    }

    @Test
    @DisplayName("Процессинг 120го статуса от ФФ-партнёра")
    @DatabaseSetup("/service/process_order_readytoship/before/ff_setup.xml")
    @DatabaseSetup(
        value = "/service/common/before/waybill_segment_storage_unit_sequential.xml",
        type = DatabaseOperation.UPDATE
    )
    @ExpectedDatabase(
        value = "/service/process_order_readytoship/after/ff_places.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void updateOrderFf() throws Exception {
        processOrderReadyToShipService.processPayload(FF_SEGMENT_PAYLOAD);

        verify(fulfillmentClient).getOrder(
            eq(ResourceId.builder().setYandexId("2-LOinttest-1").setPartnerId("FULFILLMENT-2-LOinttest-1").build()),
            eq(new Partner(172L)),
            any()
        );

        mockMvc.perform(
            post("/orders/ff/get/success")
                .contentType(MediaType.APPLICATION_JSON)
                .content(extractFileContent("service/process_order_readytoship/response/fulfillment_order.json"))
        )
            .andExpect(status().isOk());

        assertQueueTasks();

        softly.assertThat(backLogCaptor.getResults().toString()).doesNotContain(
            "level=WARN\t" +
                "format=plain\t" +
                "code=PROCESS_ORDER_ITEMS_CIS_CHECK_ERROR\t"
        );
    }

    @Test
    @DisplayName("Процессинг 120го статуса от ФФ-партнёра во время изменения последней мили")
    @DatabaseSetup("/service/process_order_readytoship/before/ff_setup_last_mile_preparing.xml")
    @DatabaseSetup(
        value = "/service/common/before/waybill_segment_storage_unit_sequential.xml",
        type = DatabaseOperation.UPDATE
    )
    @DatabaseSetup(
        value = "/service/process_order_readytoship/before/change_last_mile_request.xml",
        type = DatabaseOperation.INSERT
    )
    @ExpectedDatabase(
        value = "/service/process_order_readytoship/after/ff_places_last_mile_preparing.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void updateOrderFfWithPreparingLastMile() throws Exception {
        processOrderReadyToShipService.processPayload(FF_SEGMENT_PAYLOAD);

        verify(fulfillmentClient).getOrder(
            eq(ResourceId.builder().setYandexId("2-LOinttest-1").setPartnerId("FULFILLMENT-2-LOinttest-1").build()),
            eq(new Partner(172L)),
            any()
        );

        mockMvc.perform(
            post("/orders/ff/get/success")
                .contentType(MediaType.APPLICATION_JSON)
                .content(extractFileContent(
                    "service/process_order_readytoship/response/fulfillment_order_with_items.json"
                ))
        )
            .andExpect(status().isOk());

        queueTaskChecker.assertQueueTaskCreated(
            QueueType.EXPORT_ORDER_BOXES_CHANGED,
            lesOrderEventPayload(1, 1, "1", 1)
        );
        queueTaskChecker.assertQueueTaskCreated(
            QueueType.CHANGE_ORDER_REQUEST,
            createChangeOrderRequestPayload(2, "2", 2)
        );
        queueTaskChecker.assertQueueTaskCreated(
            QueueType.CHANGE_ORDER_REQUEST,
            createChangeOrderRequestPayload(1, "3", 3)
        );

        softly.assertThat(backLogCaptor.getResults().toString()).doesNotContain(
            "level=WARN\t" +
                "format=plain\t" +
                "code=PROCESS_ORDER_ITEMS_CIS_CHECK_ERROR\t"
        );
    }

    @Test
    @DisplayName("Процессинг 120го статуса от ФФ-партнёра с плохим CIS карготипом")
    @DatabaseSetup("/service/process_order_readytoship/before/ff_setup.xml")
    @DatabaseSetup(
        value = "/service/common/before/waybill_segment_storage_unit_sequential.xml",
        type = DatabaseOperation.UPDATE
    )
    @ExpectedDatabase(
        value = "/service/process_order_readytoship/after/ff_places_with_instances.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void updateOrderFfWithBadCisCargoType() throws Exception {
        processOrderReadyToShipService.processPayload(FF_SEGMENT_PAYLOAD);

        verify(fulfillmentClient).getOrder(
            eq(ResourceId.builder().setYandexId("2-LOinttest-1").setPartnerId("FULFILLMENT-2-LOinttest-1").build()),
            eq(new Partner(172L)),
            any()
        );

        mockMvc.perform(
            post("/orders/ff/get/success")
                .contentType(MediaType.APPLICATION_JSON)
                .content(extractFileContent(
                    "service/process_order_readytoship/response/fulfillment_order_with_bad_cis_cargo_type.json"
                ))
        )
            .andExpect(status().isOk());

        assertQueueTasks();

        softly.assertThat(backLogCaptor.getResults().toString()).contains(
            "level=WARN\t" +
                "format=plain\t" +
                "code=PROCESS_ORDER_ITEMS_CIS_CHECK_ERROR\t"
        );
    }

    @Test
    @DisplayName("Процессинг 120го статуса от ФФ-партнёра с зануленными ВГХ")
    @DatabaseSetup("/service/process_order_readytoship/before/ff_setup.xml")
    @DatabaseSetup(
        value = "/service/common/before/waybill_segment_storage_unit_sequential.xml",
        type = DatabaseOperation.UPDATE
    )
    void updateOrderWithZeroedKorobyte() throws Exception {
        processOrderReadyToShipService.processPayload(FF_SEGMENT_PAYLOAD);

        verify(fulfillmentClient).getOrder(
            eq(ResourceId.builder().setYandexId("2-LOinttest-1").setPartnerId("FULFILLMENT-2-LOinttest-1").build()),
            eq(new Partner(172L)),
            any()
        );

        mockMvc.perform(
            post("/orders/ff/get/success")
                .contentType(MediaType.APPLICATION_JSON)
                .content(extractFileContent(
                    "service/process_order_readytoship/response/fulfillment_order_with_zeroed_korobyte.json"
                ))
        )
            .andExpect(status().isOk());

        assertQueueTasks();

        OrderHistoryTestUtil.assertOrderDiff(
            jdbcTemplate,
            1,
            "service/process_order_readytoship/after/zeroed_korobyte.json",
            JSONCompareMode.STRICT
        );
    }

    @Test
    @DisplayName("Процессинг 120го статуса от кроссдок-партнёра")
    @DatabaseSetup("/service/process_order_readytoship/before/crossdock_setup.xml")
    @DatabaseSetup(
        value = "/service/common/before/waybill_segment_storage_unit_sequential.xml",
        type = DatabaseOperation.UPDATE
    )
    @ExpectedDatabase(
        value = "/service/process_order_readytoship/after/crossdock_places.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void updateOrderCrossdock() throws Exception {
        processOrderReadyToShipService.processPayload(FF_SEGMENT_PAYLOAD);

        verify(fulfillmentClient).getOrder(
            eq(ResourceId.builder().setYandexId("2-LOinttest-1").setPartnerId("SUPPLIER-2-LOinttest-1").build()),
            eq(new Partner(47983L)),
            any()
        );

        mockMvc.perform(
            post("/orders/ff/get/success")
                .contentType(MediaType.APPLICATION_JSON)
                .content(extractFileContent("service/process_order_readytoship/response/crossdock_order.json"))
        )
            .andExpect(status().isOk());

        assertQueueTasks();
    }

    @SneakyThrows
    @Test
    @DatabaseSetup("/service/process_order_readytoship/before/ff_setup.xml")
    @DatabaseSetup(
        value = "/service/common/before/waybill_segment_storage_unit_sequential.xml",
        type = DatabaseOperation.UPDATE
    )
    @DatabaseSetup(
        value = "/service/process_order_readytoship/before/prepare_items_instances.xml",
        type = DatabaseOperation.REFRESH
    )
    @DatabaseSetup(
        value = "/service/process_order_readytoship/before/fashion_order.xml",
        type = DatabaseOperation.INSERT
    )
    @ExpectedDatabase(
        value = "/service/process_order_readytoship/after/ff_fashion_with_updated_uits.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DisplayName("Fashion заказ")
    void uitsUpdatingForFashionOrders() {
        sendUpdateOrderRequest("service/process_order_readytoship/response/ff_order_uits_updating.json")
            .andExpect(status().isOk());

        assertQueueTasks();
    }

    @Test
    @SneakyThrows
    @DatabaseSetup("/service/process_order_readytoship/before/ff_setup.xml")
    @DatabaseSetup(
        value = "/service/common/before/waybill_segment_storage_unit_sequential.xml",
        type = DatabaseOperation.UPDATE
    )
    @DatabaseSetup(
        value = "/service/process_order_readytoship/before/prepare_items_instances.xml",
        type = DatabaseOperation.REFRESH
    )
    @ExpectedDatabase(
        value = "/service/process_order_readytoship/after/ff_duplicate_items.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DisplayName("Обновление не-фешн заказа, дублирующиеся товары")
    void updateNonFashionOrderWithUits() {
        sendUpdateOrderRequest("service/process_order_readytoship/response/ff_order_duplicate_items.json")
            .andExpect(status().isOk());
    }

    private void assertQueueTasks() {
        queueTaskChecker.assertQueueTaskCreated(
            QueueType.EXPORT_ORDER_BOXES_CHANGED,
            lesOrderEventPayload(1, 1, "1", 1)
        );
        queueTaskChecker.assertQueueTaskCreated(
            QueueType.CHANGE_ORDER_REQUEST,
            createChangeOrderRequestPayload(1, "2", 2)
        );
    }

    @Nonnull
    @SneakyThrows
    private ResultActions sendUpdateOrderRequest(
        String lgwGetOrderDtoJsonPath
    ) {
        processOrderReadyToShipService.processPayload(FF_SEGMENT_PAYLOAD);

        verify(fulfillmentClient).getOrder(
            eq(ResourceId.builder().setYandexId("2-LOinttest-1").setPartnerId("FULFILLMENT-2-LOinttest-1").build()),
            eq(new Partner(172L)),
            any()
        );

        return mockMvc.perform(
            post("/orders/ff/get/success")
                .contentType(MediaType.APPLICATION_JSON)
                .content(extractFileContent(lgwGetOrderDtoJsonPath))
        );
    }
}
