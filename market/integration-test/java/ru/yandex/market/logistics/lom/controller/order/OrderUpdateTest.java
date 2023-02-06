package ru.yandex.market.logistics.lom.controller.order;

import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.lom.AbstractContextualTest;
import ru.yandex.market.logistics.lom.configuration.properties.FeatureProperties;
import ru.yandex.market.logistics.lom.utils.LmsFactory;
import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.util.client.tvm.client.TvmClientApi;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static ru.yandex.market.logistics.lom.controller.order.TvmClientApiTestUtil.SERVICE_HEADERS;
import static ru.yandex.market.logistics.lom.controller.order.TvmClientApiTestUtil.SERVICE_ID;
import static ru.yandex.market.logistics.lom.controller.order.TvmClientApiTestUtil.USER_AND_SERVICE_HEADERS;
import static ru.yandex.market.logistics.lom.controller.order.TvmClientApiTestUtil.USER_HEADERS;
import static ru.yandex.market.logistics.lom.controller.order.TvmClientApiTestUtil.USER_UID;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DisplayName("Обновление черновика заказа")
class OrderUpdateTest extends AbstractContextualTest {
    @Autowired
    private LMSClient lmsClient;

    @Autowired
    private TvmClientApi tvmClientApi;

    @Autowired
    private FeatureProperties featureProperties;

    @BeforeEach
    void setup() {
        clock.setFixed(Instant.parse("2019-01-01T00:00:00.00Z"), ZoneId.systemDefault());
    }

    @Test
    @DisplayName("Успешно обновить черновик заказа")
    @DatabaseSetup("/controller/order/before/update_order.xml")
    @ExpectedDatabase(value = "/controller/order/after/update_order.xml", assertionMode = NON_STRICT_UNORDERED)
    void updateOrderDraft() throws Exception {
        when(lmsClient.getPartner(2L)).thenReturn(Optional.of(LmsFactory.createPartnerResponse(2L)));
        TvmClientApiTestUtil.mockTvmClientApi(tvmClientApi);
        OrderTestUtil.updateOrder(mockMvc, ORDER_ID, "controller/order/request/update_order.json")
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/order/response/update_order.json", "created", "updated"));

        long eventId = 1;
        OrderHistoryTestUtil.assertOrderDiff(
            jdbcTemplate,
            eventId,
            "controller/order/after/update_order_diff.json",
            JSONCompareMode.LENIENT
        );
        OrderHistoryTestUtil.assertOrderSnapshot(
            jdbcTemplate,
            eventId,
            "controller/order/snapshot/update_order.json",
            "created",
            "updated"
        );
        OrderHistoryTestUtil.assertOrderHistoryEvent(jdbcTemplate, ORDER_ID, null, null);
    }

    @Test
    @DisplayName("Успешно обновить черновик заказа с не null sourceId")
    @DatabaseSetup("/controller/order/before/update_order.xml")
    @DatabaseSetup(
        value = "/controller/order/before/update_order_source_id.xml",
        type = DatabaseOperation.REFRESH
    )
    @ExpectedDatabase(
        value = "/controller/order/after/update_order_source_id.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void updateOrderDraftWithSourceId() throws Exception {
        when(lmsClient.getPartner(2L)).thenReturn(Optional.of(LmsFactory.createPartnerResponse(2L)));
        TvmClientApiTestUtil.mockTvmClientApi(tvmClientApi);
        OrderTestUtil.updateOrder(mockMvc, ORDER_ID, "controller/order/request/update_order.json")
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/order/response/update_order.json", "created", "updated"));

        long eventId = 1;
        OrderHistoryTestUtil.assertOrderDiff(
            jdbcTemplate,
            eventId,
            "controller/order/after/update_order_diff.json",
            JSONCompareMode.LENIENT
        );
        OrderHistoryTestUtil.assertOrderSnapshot(
            jdbcTemplate,
            eventId,
            "controller/order/snapshot/update_order.json",
            "created",
            "updated"
        );
        OrderHistoryTestUtil.assertOrderHistoryEvent(jdbcTemplate, ORDER_ID, null, null);
    }

    @Test
    @DisplayName("Успешно обновить черновик заказа с юзер-тикетом в хедере и проверить что есть автор")
    @DatabaseSetup("/controller/order/before/update_order.xml")
    void updateOrderDraftWithUserTicket() throws Exception {
        when(lmsClient.getPartner(2L)).thenReturn(Optional.of(LmsFactory.createPartnerResponse(2L)));
        TvmClientApiTestUtil.mockTvmClientApi(tvmClientApi);
        OrderTestUtil.updateOrder(mockMvc, ORDER_ID, "controller/order/request/update_order.json", USER_HEADERS)
            .andExpect(status().isOk());
        OrderHistoryTestUtil.assertOrderHistoryEvent(jdbcTemplate, ORDER_ID, USER_UID, null);
    }

    @Test
    @DisplayName("Успешно обновить черновик заказа с сервис-тикетом в хедере и проверить что есть автор")
    @DatabaseSetup("/controller/order/before/update_order.xml")
    void updateOrderDraftWithServiceTicket() throws Exception {
        when(lmsClient.getPartner(2L)).thenReturn(Optional.of(LmsFactory.createPartnerResponse(2L)));
        TvmClientApiTestUtil.mockTvmClientApi(tvmClientApi);
        OrderTestUtil.updateOrder(mockMvc, ORDER_ID, "controller/order/request/update_order.json", SERVICE_HEADERS)
            .andExpect(status().isOk());
        OrderHistoryTestUtil.assertOrderHistoryEvent(jdbcTemplate, ORDER_ID, null, SERVICE_ID);
    }

    @Test
    @DisplayName("Успешно обновить черновик заказа с юзер-тикетом сервис-тикетом в хедере и проверить что есть автор")
    @DatabaseSetup("/controller/order/before/update_order.xml")
    void updateOrderDraftWithUserAndServiceTicket() throws Exception {
        when(lmsClient.getPartner(2L)).thenReturn(Optional.of(LmsFactory.createPartnerResponse(2L)));
        TvmClientApiTestUtil.mockTvmClientApi(tvmClientApi);
        OrderTestUtil.updateOrder(
            mockMvc,
            ORDER_ID,
            "controller/order/request/update_order.json",
            USER_AND_SERVICE_HEADERS
        ).andExpect(status().isOk());
        OrderHistoryTestUtil.assertOrderHistoryEvent(jdbcTemplate, ORDER_ID, USER_UID, SERVICE_ID);
    }

    @Test
    @DisplayName("Успешно обновить черновик фейкового заказа")
    @DatabaseSetup("/controller/order/before/update_order.xml")
    @ExpectedDatabase(value = "/controller/order/after/update_fake_order.xml", assertionMode = NON_STRICT_UNORDERED)
    void updateFakeOrderDraft() throws Exception {
        when(lmsClient.getPartner(2L)).thenReturn(Optional.of(LmsFactory.createPartnerResponse(2L)));
        OrderTestUtil.updateOrder(mockMvc, ORDER_ID, "controller/order/request/update_fake_order.json")
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/order/response/update_fake_order.json", "created", "updated"));

        long eventId = 1L;
        OrderHistoryTestUtil.assertOrderDiff(
            jdbcTemplate,
            eventId,
            "controller/order/after/update_fake_order_diff.json",
            JSONCompareMode.LENIENT
        );
        OrderHistoryTestUtil.assertOrderSnapshot(
            jdbcTemplate,
            eventId,
            "controller/order/snapshot/update_fake_order.json",
            "created",
            "updated"
        );
    }

    @Test
    @DisplayName("Успешно обновить черновик заказа с биллингом")
    @DatabaseSetup("/controller/order/before/update_order_with_billing.xml")
    @ExpectedDatabase(value = "/controller/order/after/update_order_with_billing.xml",
        assertionMode = NON_STRICT_UNORDERED)
    void updateOrderDraftWithBilling() throws Exception {
        when(lmsClient.getPartner(2L)).thenReturn(Optional.of(LmsFactory.createPartnerResponse(2L)));
        OrderTestUtil.updateOrder(mockMvc, ORDER_ID, "controller/order/request/update_order.json")
            .andExpect(status().isOk())
            .andExpect(jsonContent(
                "controller/order/response/update_order_without_route_id.json",
                "created",
                "updated"
            ));
    }

    @Test
    @DisplayName("Обновить черновик несуществующего заказа")
    void updateNonexistentOrderDraft() throws Exception {
        OrderTestUtil.updateOrder(mockMvc, ORDER_ID, "controller/order/request/update_order.json")
            .andExpect(status().isNotFound())
            .andExpect(jsonContent("controller/order/response/order_not_found.json"));
    }

    @Test
    @DisplayName("Обновить черновик заказа с отсутствующими обязательными полями")
    @DatabaseSetup("/controller/order/before/update_order.xml")
    void updateOrderDraftBadRequest() throws Exception {
        OrderTestUtil.updateOrder(mockMvc, ORDER_ID, "controller/order/request/empty_object.json")
            .andExpect(status().isBadRequest())
            .andExpect(jsonContent("controller/order/response/create_or_update_order_bad_request.json"));
    }

    @Test
    @DisplayName("Обновить заказ, который находится в неподходящем статусе")
    @DatabaseSetup("/controller/order/before/update_order_in_inappropriate_status.xml")
    void updateOrderInInappropriateStatus() throws Exception {
        OrderTestUtil.updateOrder(mockMvc, ORDER_ID, "controller/order/request/update_order.json")
            .andExpect(status().isBadRequest())
            .andExpect(jsonContent("controller/order/response/update_order_in_inappropriate_status.json"));
    }

    @Test
    @DisplayName("Статус DRAFT устанавливается после обновления заказа")
    @DatabaseSetup("/controller/order/before/update_order_in_validation_error_status.xml")
    void updateOrderInValidationErrorStatus() throws Exception {
        when(lmsClient.getPartner(2L)).thenReturn(Optional.of(LmsFactory.createPartnerResponse(2L)));
        OrderTestUtil.updateOrder(mockMvc, ORDER_ID, "controller/order/request/update_order.json")
            .andExpect(status().isOk())
            .andExpect(jsonContent(
                "controller/order/response/update_order_in_validation_error_status.json",
                "updated"
            ));
    }

    @Test
    @DisplayName("Обновить заказ с типом сегмента")
    @DatabaseSetup("/controller/order/before/update_order_with_waybill_segment_type.xml")
    @ExpectedDatabase(value = "/controller/order/after/update_order_with_waybill_segment_type.xml",
        assertionMode = NON_STRICT_UNORDERED)
    void updateOrderWithWaybillSegmentType() throws Exception {
        when(lmsClient.getPartner(2L)).thenReturn(Optional.of(LmsFactory.createPartnerResponse(2L)));
        OrderTestUtil.updateOrder(
            mockMvc,
            ORDER_ID,
            "controller/order/request/update_order_with_waybill_segment_type.json"
        ).andExpect(status().isOk())
            .andExpect(jsonContent(
                "controller/order/response/update_order_with_waybill_segment_type_without_route_id.json",
                "created",
                "updated"
            ));
    }

    @Test
    @DisplayName("Обновить заказ с добавлением ему нового типа сегмента")
    @DatabaseSetup("/controller/order/before/update_order.xml")
    @ExpectedDatabase(value = "/controller/order/after/update_order_with_waybill_segment_type_add.xml",
        assertionMode = NON_STRICT_UNORDERED)
    void updateOrderWithAddingNewWaybillSegmentType() throws Exception {
        when(lmsClient.getPartner(2L)).thenReturn(Optional.of(LmsFactory.createPartnerResponse(2L)));
        OrderTestUtil.updateOrder(
            mockMvc,
            ORDER_ID,
            "controller/order/request/update_order_with_waybill_segment_type.json"
        ).andExpect(status().isOk())
            .andExpect(jsonContent(
                "controller/order/response/update_order_with_waybill_segment_type.json",
                "created",
                "updated"
            ));
    }

    @Test
    @DisplayName("Обновить заказ с изменением имеющегося у него типа сегмента")
    @DatabaseSetup("/controller/order/before/update_order_with_old_waybill_segment_type.xml")
    @ExpectedDatabase(value = "/controller/order/after/update_order_with_waybill_segment_type.xml",
        assertionMode = NON_STRICT_UNORDERED)
    void updateOrderWithChangingWaybillSegmentType() throws Exception {
        when(lmsClient.getPartner(2L)).thenReturn(Optional.of(LmsFactory.createPartnerResponse(2L)));
        OrderTestUtil.updateOrder(
            mockMvc,
            ORDER_ID,
            "controller/order/request/update_order_with_waybill_segment_type.json"
        ).andExpect(status().isOk())
            .andExpect(jsonContent(
                "controller/order/response/update_order_with_waybill_segment_type_without_route_id.json",
                "created",
                "updated"
            ));
    }

    @Test
    @DisplayName("Обновить заказ с передачей невалидного типа сегмента")
    void updateOrderWithInvalidWaybillSegmentType() throws Exception {
        when(lmsClient.getPartner(2L)).thenReturn(Optional.of(LmsFactory.createPartnerResponse(2L)));
        OrderTestUtil.updateOrder(
            mockMvc,
            ORDER_ID,
            "controller/order/request/update_order_with_invalid_waybill_segment_type.json"
        )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("message").value(startsWith(
                "Cannot deserialize value"
                    + " of type `ru.yandex.market.logistics.lom.model.enums.SegmentType`"
                    + " from String \"SSHIPMENTT\""
            )));
    }

    @Test
    @DisplayName("Заменить единицы хранения заказа")
    @DatabaseSetup("/controller/order/before/update_order.xml")
    @DatabaseSetup("/service/common/before/order_items_units.xml")
    @ExpectedDatabase(value = "/controller/order/after/update_order_units.xml", assertionMode = NON_STRICT_UNORDERED)
    void updateOrderUnits() throws Exception {
        when(lmsClient.getPartner(2L)).thenReturn(Optional.of(LmsFactory.createPartnerResponse(2L)));
        OrderTestUtil.updateOrder(mockMvc, ORDER_ID, "controller/order/request/update_order.json")
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/order/response/update_order.json", "created", "updated"));
    }


    @Test
    @DisplayName("Успешное обновление стрельбового заказа с включеным флагом создает OrderHistoryEvent")
    @DatabaseSetup("/controller/order/before/update_order.xml")
    @DatabaseSetup(
        value = "/controller/order/before/update_order_shooting.xml",
        type = DatabaseOperation.UPDATE
    )
    @ExpectedDatabase(
        value = "/controller/order/after/update_order_shooting_enabled.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void updateOrderDraftShootingEnabled() throws Exception {
        when(lmsClient.getPartner(2L)).thenReturn(Optional.of(LmsFactory.createPartnerResponse(2L)));
        featureProperties.setShootingOrdersEventsExportEnabled(true);
        TvmClientApiTestUtil.mockTvmClientApi(tvmClientApi);
        OrderTestUtil.updateOrder(mockMvc, ORDER_ID, "controller/order/request/update_order.json")
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/order/response/update_order.json", "created", "updated"));
    }

    @Test
    @DisplayName("При успешном обновлении стрельбового заказа с выключеным флагом не создается ивент в базе")
    @DatabaseSetup("/controller/order/before/update_order.xml")
    @DatabaseSetup(
        value = "/controller/order/before/update_order_shooting.xml",
        type = DatabaseOperation.UPDATE
    )
    @ExpectedDatabase(
        value = "/controller/order/after/update_order_shooting.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void updateOrderDraftShootingDisabled() throws Exception {
        when(lmsClient.getPartner(2L)).thenReturn(Optional.of(LmsFactory.createPartnerResponse(2L)));
        featureProperties.setShootingOrdersEventsExportEnabled(false);
        TvmClientApiTestUtil.mockTvmClientApi(tvmClientApi);
        OrderTestUtil.updateOrder(mockMvc, ORDER_ID, "controller/order/request/update_order.json")
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/order/response/update_order.json", "created", "updated"));
    }
}
