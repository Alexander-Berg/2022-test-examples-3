package ru.yandex.market.logistics.lom.controller.order;

import java.time.Instant;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.common.util.DateTimeUtils;
import ru.yandex.market.logistics.lom.AbstractContextualTest;
import ru.yandex.market.logistics.lom.jobs.model.QueueType;
import ru.yandex.market.logistics.lom.model.dto.OrderCommitRequest;
import ru.yandex.market.logistics.lom.model.enums.tags.OrderTag;
import ru.yandex.market.logistics.lom.utils.jobs.PayloadFactory;
import ru.yandex.market.logistics.util.client.tvm.client.TvmClientApi;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static ru.yandex.market.logistics.lom.controller.order.TvmClientApiTestUtil.SERVICE_HEADERS;
import static ru.yandex.market.logistics.lom.controller.order.TvmClientApiTestUtil.SERVICE_ID;
import static ru.yandex.market.logistics.lom.controller.order.TvmClientApiTestUtil.USER_AND_SERVICE_HEADERS;
import static ru.yandex.market.logistics.lom.controller.order.TvmClientApiTestUtil.USER_HEADERS;
import static ru.yandex.market.logistics.lom.controller.order.TvmClientApiTestUtil.USER_UID;
import static ru.yandex.market.logistics.lom.model.enums.tags.OrderTag.COMMITTED_VIA_DAAS_BACK_OFFICE;
import static ru.yandex.market.logistics.lom.model.enums.tags.OrderTag.COMMITTED_VIA_DAAS_OPEN_API;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.noContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DisplayName("Оформление заказа")
class OrderCommitTest extends AbstractContextualTest {

    private static final OrderCommitRequest OPEN_API_REQUEST = OrderCommitRequest.builder()
        .source(COMMITTED_VIA_DAAS_OPEN_API)
        .build();

    private static final OrderCommitRequest BACK_OFFICE_REQUEST = OrderCommitRequest.builder()
        .source(COMMITTED_VIA_DAAS_BACK_OFFICE)
        .build();

    @Autowired
    private TvmClientApi tvmClientApi;

    @BeforeEach
    void setup() {
        clock.setFixed(Instant.parse("2019-06-12T00:00:00.00Z"), DateTimeUtils.MOSCOW_ZONE);
    }

    @Test
    @DisplayName("Оформить заказ, у которого заполнены только обязательные для черновика поля")
    @DatabaseSetup("/controller/commit/before/commit_order_with_only_draft_required_fields_set.xml")
    @ExpectedDatabase(
        value = "/controller/commit/after/commit_order_without_all_fields_set.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void commitOrderWithOnlyDraftRequiredFieldsSet() throws Exception {
        OrderTestUtil.commitOrder(mockMvc, ORDER_ID)
            .andExpect(status().isBadRequest())
            .andExpect(jsonContent("controller/commit/response/commit_order_with_only_draft_required_fields_set.json"));
        OrderHistoryTestUtil.assertOrderDiff(
            jdbcTemplate, 1L, "controller/order/after/commit_order_without_all_fields_set.json"
        );

        softly.assertThat(backLogCaptor.getResults().toString())
            .contains("level=WARN\t" +
                "format=plain\t" +
                "code=COMMIT_ORDER_ERROR\t" +
                "payload=[" +
                "FieldError(propertyPath=billingEntity, message=must not be null), " +
                "FieldError(propertyPath=cost, message=must not be null), " +
                "FieldError(propertyPath=deliveryType, message=must not be null), " +
                "FieldError(propertyPath=items, message=must not be empty), " +
                "FieldError(propertyPath=marketIdFrom, message=must not be null), " +
                "FieldError(propertyPath=sender.balanceClientId, message=must not be null), " +
                "FieldError(propertyPath=sender.balanceProductId, message=must not be null), " +
                "FieldError(propertyPath=units, message=Order must have at least 1 place), " +
                "FieldError(propertyPath=waybill, message=must not be empty)]\t" +
                "request_id=1000000000000/abcdabcdabcdabcdabcdabcdabcdabcd\t" +
                "tags=BUSINESS_ORDER_EVENT\tentity_types=order,lom_order,sender,platform\t" +
                "entity_values=order:null,lom_order:1,sender:1,platform:YANDEX_DELIVERY");
    }

    @Test
    @DisplayName("Оформить заказ, у которого отсутствуют все обязательные поля")
    @DatabaseSetup("/controller/commit/before/commit_order_without_all_fields_set.xml")
    @ExpectedDatabase(
        value = "/controller/commit/after/commit_order_without_all_fields_set.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void commitOrderWithoutAllRequiredFieldsSet() throws Exception {
        OrderTestUtil.commitOrder(mockMvc, ORDER_ID)
            .andExpect(status().isBadRequest())
            .andExpect(jsonContent("controller/commit/response/commit_order_without_all_fields_set.json"));
        OrderHistoryTestUtil.assertOrderDiff(
            jdbcTemplate, 1L, "controller/order/after/commit_order_without_all_required_fields_set.json"
        );
    }

    @Test
    @DisplayName("Успешно оформить заказ")
    @DatabaseSetup("/controller/commit/before/commit_order_success.xml")
    @DatabaseSetup("/service/common/before/order_items_units.xml")
    @DatabaseSetup(value = "/service/common/before/waybill_segment_storage_unit.xml", type = DatabaseOperation.UPDATE)
    @ExpectedDatabase(
        value = "/controller/commit/after/commit_order_success.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void commitOrderSuccess() throws Exception {
        TvmClientApiTestUtil.mockTvmClientApi(tvmClientApi);
        OrderTestUtil.commitOrder(mockMvc, ORDER_ID).andExpect(status().isOk());
        OrderHistoryTestUtil.assertOrderHistoryEvent(jdbcTemplate, ORDER_ID, null, null);
    }

    @Test
    @DisplayName("Успешно оформить заказ через OpenApi и проверить, что выставился тег")
    @DatabaseSetup("/controller/commit/before/commit_order_success.xml")
    @DatabaseSetup("/service/common/before/order_items_units.xml")
    @DatabaseSetup(value = "/service/common/before/waybill_segment_storage_unit.xml", type = DatabaseOperation.UPDATE)
    @ExpectedDatabase(
        value = "/controller/commit/after/commit_order_success_from_open_api.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void commitOrderSuccessFromOpenApi() throws Exception {
        TvmClientApiTestUtil.mockTvmClientApi(tvmClientApi);
        performCommitWithSource(COMMITTED_VIA_DAAS_OPEN_API).andExpect(status().isOk()).andExpect(noContent());
        OrderHistoryTestUtil.assertOrderHistoryEvent(jdbcTemplate, ORDER_ID, null, null);
    }

    @Test
    @DisplayName("Успешно оформить заказ через личный кабинет и проверить, что выставился тег")
    @DatabaseSetup("/controller/commit/before/commit_order_success.xml")
    @DatabaseSetup("/service/common/before/order_items_units.xml")
    @DatabaseSetup(value = "/service/common/before/waybill_segment_storage_unit.xml", type = DatabaseOperation.UPDATE)
    @ExpectedDatabase(
        value = "/controller/commit/after/commit_order_success_from_back_office.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void commitOrderSuccessFromBackOffice() throws Exception {
        TvmClientApiTestUtil.mockTvmClientApi(tvmClientApi);
        performCommitWithSource(COMMITTED_VIA_DAAS_BACK_OFFICE).andExpect(status().isOk()).andExpect(noContent());
        OrderHistoryTestUtil.assertOrderHistoryEvent(jdbcTemplate, ORDER_ID, null, null);
    }

    @NotNull
    private ResultActions performCommitWithSource(OrderTag source) throws Exception {
        return mockMvc.perform(request(
            HttpMethod.POST,
            "/orders/" + ORDER_ID + "/commit",
            OrderCommitRequest.builder().source(source).build()
        ));
    }

    @Test
    @DisplayName("Успешно оформить заказ с юзер-тикетом в хедере и проверить что проставился автор")
    @DatabaseSetup("/controller/commit/before/commit_order_success.xml")
    @DatabaseSetup("/service/common/before/order_items_units.xml")
    @DatabaseSetup(value = "/service/common/before/waybill_segment_storage_unit.xml", type = DatabaseOperation.UPDATE)
    void commitOrderSuccessWithUserTicket() throws Exception {
        TvmClientApiTestUtil.mockTvmClientApi(tvmClientApi);
        OrderTestUtil.commitOrder(mockMvc, ORDER_ID, USER_HEADERS).andExpect(status().isOk());
        OrderHistoryTestUtil.assertOrderHistoryEvent(jdbcTemplate, ORDER_ID, USER_UID, null);
    }

    @Test
    @DisplayName("Успешно оформить заказ с сервис-тикетом в хедере и проверить что проставился автор")
    @DatabaseSetup("/controller/commit/before/commit_order_success.xml")
    @DatabaseSetup("/service/common/before/order_items_units.xml")
    @DatabaseSetup(value = "/service/common/before/waybill_segment_storage_unit.xml", type = DatabaseOperation.UPDATE)
    void commitOrderSuccessWithServiceTicket() throws Exception {
        TvmClientApiTestUtil.mockTvmClientApi(tvmClientApi);
        OrderTestUtil.commitOrder(mockMvc, ORDER_ID, SERVICE_HEADERS).andExpect(status().isOk());
        OrderHistoryTestUtil.assertOrderHistoryEvent(jdbcTemplate, ORDER_ID, null, SERVICE_ID);
    }

    @Test
    @DisplayName("Успешно оформить заказ с юзер-тикетом и сервис-тикетом в хедере и проверить что проставился автор")
    @DatabaseSetup("/controller/commit/before/commit_order_success.xml")
    @DatabaseSetup("/service/common/before/order_items_units.xml")
    @DatabaseSetup(value = "/service/common/before/waybill_segment_storage_unit.xml", type = DatabaseOperation.UPDATE)
    void commitOrderSuccessWithUserAndServiceTicket() throws Exception {
        TvmClientApiTestUtil.mockTvmClientApi(tvmClientApi);
        OrderTestUtil.commitOrder(mockMvc, ORDER_ID, USER_AND_SERVICE_HEADERS).andExpect(status().isOk());
        OrderHistoryTestUtil.assertOrderHistoryEvent(jdbcTemplate, ORDER_ID, USER_UID, SERVICE_ID);
    }

    @Test
    @DisplayName("Успешно оформить заказ - дабл комит")
    @DatabaseSetup("/controller/commit/before/commit_order_success.xml")
    @DatabaseSetup("/service/common/before/order_items_units.xml")
    @DatabaseSetup(value = "/service/common/before/waybill_segment_storage_unit.xml", type = DatabaseOperation.UPDATE)
    @ExpectedDatabase(
        value = "/controller/commit/after/commit_order_success.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void commitOrderSuccessDouble() throws Exception {
        OrderTestUtil.commitOrder(mockMvc, ORDER_ID).andExpect(status().isOk());
        queueTaskChecker.assertQueueTaskCreated(
            QueueType.VALIDATE_ORDER_EXTERNAL,
            PayloadFactory.createOrderIdPayload(ORDER_ID, 1L)
        );
        OrderTestUtil.commitOrder(mockMvc, ORDER_ID)
            .andExpect(status().isBadRequest())
            .andExpect(jsonContent("controller/commit/response/commit_order_double.json"));
        queueTaskChecker.assertQueueTaskNotCreated(
            QueueType.VALIDATE_ORDER_EXTERNAL,
            PayloadFactory.createOrderIdPayload(ORDER_ID, 2L)
        );
    }

    @Test
    @DisplayName("Успешно оформить фейковый заказ")
    @DatabaseSetup("/controller/commit/before/commit_order_success_fake.xml")
    @DatabaseSetup("/service/common/before/order_items_units.xml")
    @DatabaseSetup(value = "/service/common/before/waybill_segment_storage_unit.xml", type = DatabaseOperation.UPDATE)
    @ExpectedDatabase(
        value = "/controller/commit/after/commit_order_success_fake.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void validateOwnDeliveryOrderSuccess() throws Exception {
        OrderTestUtil.commitOrder(mockMvc, ORDER_ID).andExpect(status().isOk());
        queueTaskChecker.assertNoQueueTasksCreated();
    }

    @Test
    @DisplayName("Попытка оформить фейковый заказ с более чем одним сегментом")
    @DatabaseSetup("/controller/commit/before/commit_order_success_fake.xml")
    @DatabaseSetup(
        value = "/controller/commit/before/addtitional_waybill_segment.xml",
        type = DatabaseOperation.INSERT
    )
    @DatabaseSetup("/service/common/before/order_items_units.xml")
    @DatabaseSetup(value = "/service/common/before/waybill_segment_storage_unit.xml", type = DatabaseOperation.UPDATE)
    void validateOwnDeliveryOrderWaybillSegmentIsNotSingle() throws Exception {
        OrderTestUtil.commitOrder(mockMvc, ORDER_ID)
            .andExpect(status().isBadRequest())
            .andExpect(jsonContent(
                "controller/commit/response/commit_fake_order_with_multiple_waybill_segments.json"
            ));
    }

    @Test
    @DisplayName("Коммит заказа с некорректным email")
    @DatabaseSetup("/controller/commit/before/commit_order_success.xml")
    @DatabaseSetup(value = "/controller/commit/before/invalid_email.xml", type = DatabaseOperation.UPDATE)
    @DatabaseSetup("/service/common/before/order_items_units.xml")
    @DatabaseSetup(value = "/service/common/before/waybill_segment_storage_unit.xml", type = DatabaseOperation.UPDATE)
    void commitOrderWithInvalidRecipientEmail() throws Exception {
        OrderTestUtil.commitOrder(mockMvc, ORDER_ID)
            .andExpect(status().isBadRequest())
            .andExpect(jsonContent("controller/commit/response/commit_order_with_invalid_email.json"));
    }

    @Test
    @DisplayName("Коммит заказа с email, содержащим русские буквы")
    @DatabaseSetup("/controller/commit/before/commit_order_success.xml")
    @DatabaseSetup(value = "/controller/commit/before/email_with_russian_letters.xml", type = DatabaseOperation.UPDATE)
    @DatabaseSetup("/service/common/before/order_items_units.xml")
    @DatabaseSetup(value = "/service/common/before/waybill_segment_storage_unit.xml", type = DatabaseOperation.UPDATE)
    void commitOrderWithRussianLettersInRecipientEmail() throws Exception {
        OrderTestUtil.commitOrder(mockMvc, ORDER_ID).andExpect(status().isOk());
    }

    @Test
    @DisplayName("Коммит заказа с пустым email")
    @DatabaseSetup("/controller/commit/before/commit_order_success.xml")
    @DatabaseSetup(value = "/controller/commit/before/null_email.xml", type = DatabaseOperation.REFRESH)
    @DatabaseSetup("/service/common/before/order_items_units.xml")
    @DatabaseSetup(value = "/service/common/before/waybill_segment_storage_unit.xml", type = DatabaseOperation.UPDATE)
    void commitOrderWithNullEmail() throws Exception {
        OrderTestUtil.commitOrder(mockMvc, ORDER_ID).andExpect(status().isOk());
    }

    @Test
    @DisplayName("Коммит заказа с пустым адресом, но заполненным персональным адресом")
    @DatabaseSetup("/controller/commit/before/commit_order_success.xml")
    @DatabaseSetup(
        value = "/controller/commit/before/null_address_add_personal_address_id.xml",
        type = DatabaseOperation.REFRESH)
    @DatabaseSetup("/service/common/before/order_items_units.xml")
    @DatabaseSetup(value = "/service/common/before/waybill_segment_storage_unit.xml", type = DatabaseOperation.UPDATE)
    void commitOrderWithNullAddressButPersonalAddressId() throws Exception {
        OrderTestUtil.commitOrder(mockMvc, ORDER_ID)
            .andExpect(status().isBadRequest())
            .andExpect(jsonContent("controller/commit/response/commit_order_with_invalid_address_fields.json"));
    }


    @Test
    @DisplayName("Коммит заказа с почти пустым адресом, но заполненным персональным адресом")
    @DatabaseSetup("/controller/commit/before/commit_order_success.xml")
    @DatabaseSetup(
        value = "/controller/commit/before/only_geoid_address_add_personal_address_id.xml",
        type = DatabaseOperation.REFRESH)
    @DatabaseSetup("/service/common/before/order_items_units.xml")
    @DatabaseSetup(value = "/service/common/before/waybill_segment_storage_unit.xml", type = DatabaseOperation.UPDATE)
    void commitOrderWithGeoIdAddressAndPersonalAddressId() throws Exception {
        OrderTestUtil.commitOrder(mockMvc, ORDER_ID)
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Коммит заказа с пустым ФИО, но заполненным personal_fullname_id")
    @DatabaseSetup("/controller/commit/before/commit_order_success.xml")
    @DatabaseSetup(
        value = "/controller/commit/before/empty_recipient_fio_and_add_personal_fullname_id.xml",
        type = DatabaseOperation.REFRESH)
    @DatabaseSetup("/service/common/before/order_items_units.xml")
    @DatabaseSetup(value = "/service/common/before/waybill_segment_storage_unit.xml", type = DatabaseOperation.UPDATE)
    void commitOrderWithEmptyFioAndFilledPersonalFullnameId() throws Exception {
        OrderTestUtil.commitOrder(mockMvc, ORDER_ID)
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Коммит заказа с пустым ФИО и personal_fullname_id")
    @DatabaseSetup("/controller/commit/before/commit_order_success.xml")
    @DatabaseSetup(
        value = "/controller/commit/before/empty_recipient_fio_and_personal_fullname_id.xml",
        type = DatabaseOperation.REFRESH)
    @DatabaseSetup("/service/common/before/order_items_units.xml")
    @DatabaseSetup(value = "/service/common/before/waybill_segment_storage_unit.xml", type = DatabaseOperation.UPDATE)
    void commitOrderWithEmptyFioAndPersonalFullnameId() throws Exception {
        OrderTestUtil.commitOrder(mockMvc, ORDER_ID)
            .andExpect(status().isBadRequest())
            .andExpect(
                jsonContent("controller/commit/response/commit_order_with_invalid_recipient_fullname_fields.json")
            );
    }

    @Test
    @DisplayName("Коммит заказа с пустым контактом но не пустым персонал полями personal_fullname_id")
    @DatabaseSetup("/controller/commit/before/commit_order_success.xml")
    @DatabaseSetup(
        value = "/controller/commit/before/contact_with_personal_fields.xml",
        type = DatabaseOperation.REFRESH)
    @DatabaseSetup("/service/common/before/order_items_units.xml")
    @DatabaseSetup(value = "/service/common/before/waybill_segment_storage_unit.xml", type = DatabaseOperation.UPDATE)
    void commitOrderWithEmptyContactFioAndPhoneAndFilledPersonalFields() throws Exception {
        OrderTestUtil.commitOrder(mockMvc, ORDER_ID)
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Коммит заказа с пустым контактом")
    @DatabaseSetup("/controller/commit/before/commit_order_success.xml")
    @DatabaseSetup(
        value = "/controller/commit/before/empty_contact.xml",
        type = DatabaseOperation.REFRESH)
    @DatabaseSetup("/service/common/before/order_items_units.xml")
    @DatabaseSetup(value = "/service/common/before/waybill_segment_storage_unit.xml", type = DatabaseOperation.UPDATE)
    void commitOrderWithEmptyContact() throws Exception {
        OrderTestUtil.commitOrder(mockMvc, ORDER_ID)
            .andExpect(status().isBadRequest())
            .andExpect(
                jsonContent("controller/commit/response/commit_order_with_invalid_contact_personal_fields.json")
            );
    }

    @Test
    @DisplayName("Интервал вместо даты доставки")
    @DatabaseSetup("/controller/commit/before/commit_order_success.xml")
    @DatabaseSetup(value = "/controller/commit/before/delivery_interval.xml", type = DatabaseOperation.UPDATE)
    @DatabaseSetup("/service/common/before/order_items_units.xml")
    @DatabaseSetup(value = "/service/common/before/waybill_segment_storage_unit.xml", type = DatabaseOperation.UPDATE)
    void deliveryIntervalMinMax() throws Exception {
        OrderTestUtil.commitOrder(mockMvc, ORDER_ID).andExpect(status().isOk());
    }

    @Test
    @DisplayName("Коммит заказа в неподходящем статусе")
    @DatabaseSetup("/controller/commit/before/commit_order_in_invalid_status.xml")
    @ExpectedDatabase(
        value = "/controller/commit/after/commit_order_in_invalid_status.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void commitOrderSuccessRepeat() throws Exception {
        OrderTestUtil.commitOrder(mockMvc, ORDER_ID)
            .andExpect(status().isBadRequest())
            .andExpect(jsonContent("controller/commit/response/commit_order_in_invalid_status.json"));
    }

    @Test
    @DisplayName("Успешное оформить заказ с сегментом типа FULFILLMENT")
    @DatabaseSetup("/controller/commit/before/commit_order_success_fulfillment_segment.xml")
    @DatabaseSetup("/service/common/before/order_items_units.xml")
    @ExpectedDatabase(
        value = "/controller/commit/after/commit_order_success.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void commitOrderSuccessFulfillmentSegment() throws Exception {
        TvmClientApiTestUtil.mockTvmClientApi(tvmClientApi);
        OrderTestUtil.commitOrder(mockMvc, ORDER_ID).andExpect(status().isOk());
        OrderHistoryTestUtil.assertOrderHistoryEvent(jdbcTemplate, ORDER_ID, null, null);
    }

    @Test
    @DisplayName("Успешное оформить заказ с сегментом типа FULFILLMENT и без locationTo в DS сегменте")
    @DatabaseSetup("/controller/commit/before/commit_order_success_fulfillment_segment.xml")
    @DatabaseSetup("/service/common/before/order_items_units.xml")
    @DatabaseSetup(
        value = "/controller/commit/before/drop_location_to_second_segment.xml",
        type = DatabaseOperation.UPDATE
    )
    @ExpectedDatabase(
        value = "/controller/commit/after/commit_order_success.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void commitOrderSuccessFulfillmentAndDeliveryWoLocationToSegment() throws Exception {
        TvmClientApiTestUtil.mockTvmClientApi(tvmClientApi);
        OrderTestUtil.commitOrder(mockMvc, ORDER_ID).andExpect(status().isOk());
        OrderHistoryTestUtil.assertOrderHistoryEvent(jdbcTemplate, ORDER_ID, null, null);
    }

    @Test
    @DisplayName("Успешно оформить заказ без указания assessedValue товара")
    @DatabaseSetup("/controller/commit/before/commit_order_success.xml")
    @DatabaseSetup("/service/common/before/order_items_units.xml")
    @DatabaseSetup(value = "/service/common/before/waybill_segment_storage_unit.xml", type = DatabaseOperation.UPDATE)
    @DatabaseSetup(value = "/service/common/before/item_without_assessed_value.xml", type = DatabaseOperation.UPDATE)
    @ExpectedDatabase(
        value = "/controller/commit/after/commit_order_success.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void commitOrderSuccessWithoutItemAssessedValue() throws Exception {
        TvmClientApiTestUtil.mockTvmClientApi(tvmClientApi);
        OrderTestUtil.commitOrder(mockMvc, ORDER_ID).andExpect(status().isOk());
        OrderHistoryTestUtil.assertOrderHistoryEvent(jdbcTemplate, ORDER_ID, null, null);
    }

    @Test
    @DisplayName("Проверка затирания тега ошибки из OpenApi, при появлении новой ошибки")
    @DatabaseSetup("/controller/commit/before/commit_order_without_all_fields_set.xml")
    @ExpectedDatabase(
        value = "/controller/commit/after/commit_invalid_order_from_open_api.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void commitInvalidOrderFromOpenApi() throws Exception {
        performAndCheckInvalidCommit(OPEN_API_REQUEST);
        performAndCheckInvalidCommit(OPEN_API_REQUEST);
    }

    @Test
    @DisplayName("Проверка затирания тега ошибки из личного кабинета, при появлении новой ошибки")
    @DatabaseSetup("/controller/commit/before/commit_order_without_all_fields_set.xml")
    @ExpectedDatabase(
        value = "/controller/commit/after/commit_invalid_order_from_back_office.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void commitInvalidOrderFromBackOffice() throws Exception {
        performAndCheckInvalidCommit(BACK_OFFICE_REQUEST);
        performAndCheckInvalidCommit(BACK_OFFICE_REQUEST);
    }

    @Test
    @DisplayName("Проверка проставления последнего тега ошибки")
    @DatabaseSetup("/controller/commit/before/commit_order_without_all_fields_set.xml")
    @ExpectedDatabase(
        value = "/controller/commit/after/commit_invalid_order_from_open_api.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void commitInvalidOrderLastFail() throws Exception {
        performAndCheckInvalidCommit(BACK_OFFICE_REQUEST);
        performAndCheckInvalidCommit(OPEN_API_REQUEST);
    }

    @Test
    @DisplayName("Коммит многоместного DAAS заказа с частично заполненными габаритами грузомест")
    @DatabaseSetup("/controller/commit/before/commit_order_success.xml")
    @DatabaseSetup("/service/common/before/multiplace_order_units.xml")
    @DatabaseSetup(value = "/service/common/before/waybill_segment_storage_unit.xml", type = DatabaseOperation.UPDATE)
    @ExpectedDatabase(
        value = "/controller/commit/after/commit_order_with_partial_places_dimensions.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void commitOrderWithPartialDimensions() throws Exception {
        OrderTestUtil.commitOrder(mockMvc, ORDER_ID)
            .andExpect(status().isBadRequest())
            .andExpect(jsonContent("controller/commit/response/commit_order_with_partial_places_dimensions.json"));

        OrderHistoryTestUtil.assertOrderDiff(
            jdbcTemplate, 1L, "controller/order/after/commit_order_with_partial_places_dimensions.json"
        );
    }

    private void performAndCheckInvalidCommit(OrderCommitRequest request) throws Exception {
        mockMvc.perform(request(HttpMethod.POST, "/orders/" + ORDER_ID + "/commit", request))
            .andExpect(status().isBadRequest())
            .andExpect(jsonContent("controller/commit/response/commit_order_without_all_fields_set.json"));
    }
}
