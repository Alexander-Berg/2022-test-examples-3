package ru.yandex.market.logistics.lom.controller.order;

import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;
import java.util.stream.Stream;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import ru.yandex.market.logistics.lom.AbstractContextualTest;
import ru.yandex.market.logistics.lom.entity.enums.tags.OrderTag;
import ru.yandex.market.logistics.lom.utils.LmsFactory;
import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.util.client.tvm.client.TvmClientApi;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static ru.yandex.market.logistics.lom.controller.order.TvmClientApiTestUtil.SERVICE_HEADERS;
import static ru.yandex.market.logistics.lom.controller.order.TvmClientApiTestUtil.SERVICE_ID;
import static ru.yandex.market.logistics.lom.controller.order.TvmClientApiTestUtil.USER_AND_SERVICE_HEADERS;
import static ru.yandex.market.logistics.lom.controller.order.TvmClientApiTestUtil.USER_HEADERS;
import static ru.yandex.market.logistics.lom.controller.order.TvmClientApiTestUtil.USER_UID;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.errorMessage;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DisplayName("Создание черновика заказа")
class OrderCreateTest extends AbstractContextualTest {

    @Autowired
    private LMSClient lmsClient;

    @Autowired
    private TvmClientApi tvmClientApi;

    @BeforeEach
    void setup() {
        clock.setFixed(Instant.parse("2019-01-01T00:00:00.00Z"), ZoneId.systemDefault());
    }

    @Test
    @DisplayName("Создать черновик заказа со всеми заполненными полями")
    @DatabaseSetup("/controller/order/before/logbroker_source_write_lock.xml")
    @ExpectedDatabase(value = "/controller/order/after/create_order.xml", assertionMode = NON_STRICT_UNORDERED)
    @ExpectedDatabase(value = "/controller/order/after/create_order_waybill.xml", assertionMode = NON_STRICT_UNORDERED)
    void createOrderDraft() throws Exception {
        when(lmsClient.getPartner(1L)).thenReturn(Optional.of(LmsFactory.createPartnerResponse(1L)));
        TvmClientApiTestUtil.mockTvmClientApi(tvmClientApi);
        OrderTestUtil.createOrder(mockMvc, "controller/order/request/create_order.json")
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/order/response/create_order.json", "created", "updated"));
        OrderHistoryTestUtil.assertOrderDiff(jdbcTemplate, 1L, "controller/order/after/create_order_diff.json");

        softly.assertThat(OrderTestUtil.getSenderEmails(jdbcTemplate, ORDER_ID))
            .containsExactly("test.sender@test.ru", "second.sender@test.ru");
        OrderHistoryTestUtil.assertOrderHistoryEvent(jdbcTemplate, ORDER_ID, null, null);
    }

    @Test
    @DisplayName("Создать черновик Daas-заказа и закоммитить его")
    @ExpectedDatabase(value = "/controller/order/after/waybill_indexes.xml", assertionMode = NON_STRICT_UNORDERED)
    void createDaasOrderDraftWithAutoCommit() throws Exception {
        when(lmsClient.getPartner(1L)).thenReturn(Optional.of(LmsFactory.createPartnerResponse(1L, 2L)));
        TvmClientApiTestUtil.mockTvmClientApi(tvmClientApi);
        mockMvc.perform(
            post("/orders")
                .param("autoCommit", "true")
                .contentType(MediaType.APPLICATION_JSON)
                .content(extractFileContent("controller/order/request/create_order_with_auto_commit.json"))
        )
            .andExpect(status().isOk())
            .andExpect(
                jsonContent("controller/order/response/create_order_with_auto_commit.json", "created", "updated")
            );
    }

    @Test
    @DisplayName("Создать черновик заказа Yandex Go и закоммитить его")
    @ExpectedDatabase(value = "/controller/order/after/waybill_indexes.xml", assertionMode = NON_STRICT_UNORDERED)
    @ExpectedDatabase(value = "/controller/order/after/create_order_c2c_tag.xml", assertionMode = NON_STRICT_UNORDERED)
    void createYandexGoOrderDraftWithAutoCommit() throws Exception {
        when(lmsClient.getPartner(1L)).thenReturn(Optional.of(LmsFactory.createPartnerResponse(1L, 2L)));
        TvmClientApiTestUtil.mockTvmClientApi(tvmClientApi);
        mockMvc.perform(
                post("/orders")
                    .param("autoCommit", "true")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(extractFileContent(
                        "controller/order/request/create_order_with_auto_commit_yandex_go.json"
                    ))
            )
            .andExpect(status().isOk())
            .andExpect(
                jsonContent(
                    "controller/order/response/create_order_with_auto_commit_yandex_go.json",
                    "created",
                    "updated"
                )
            );
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource("yandexGoOrderValidationArguments")
    @DisplayName("Пре-коммитные проверки для заказов Yandex Go")
    void createYandexGoOrderDraftWithAutoCommitValidation(
        String caseName,
        String requestSource,
        String propertyPath,
        String errorMessage
    ) throws Exception {
        when(lmsClient.getPartner(1L)).thenReturn(Optional.of(LmsFactory.createPartnerResponse(1L, 2L)));
        TvmClientApiTestUtil.mockTvmClientApi(tvmClientApi);
        mockMvc.perform(
                post("/orders")
                    .param("autoCommit", "true")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(extractFileContent(requestSource))
            )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("fieldErrors[0].propertyPath").value(propertyPath))
            .andExpect(jsonPath("fieldErrors[0].message").value(errorMessage));
    }

    private static Stream<Arguments> yandexGoOrderValidationArguments() {
        return Stream.of(
            Arguments.of(
                "Не указан marketIdFrom",
                "controller/order/request/create_order_with_auto_commit_yandex_go_validation_market_id_from.json",
                "marketIdFrom",
                "must not be null"
            ),
            Arguments.of(
                "Не указан идентификатор клиента сендера в Балансе",
                "controller/order/request/create_order_with_auto_commit_yandex_go_validation_balance_client.json",
                "sender.balanceClientId",
                "must not be null"
            ),
            Arguments.of(
                "Не указан идентификатор продукта сендера в Балансе",
                "controller/order/request/create_order_with_auto_commit_yandex_go_validation_balance_product.json",
                "sender.balanceProductId",
                "must not be null"
            ),
            Arguments.of(
                "У товаров заказа совпадают артикулы",
                "controller/order/request/create_order_with_auto_commit_yandex_go_validation_items_articles.json",
                "items",
                "item articles must be unique for order"
            ),
            Arguments.of(
                "Есть грузоместа типа PLACE и без размеров, и с размерами",
                "controller/order/request/create_order_with_auto_commit_yandex_go_validation_places_dimensions.json",
                "units",
                "Order must have all places dimensions or none"
            )
        );
    }

    @Test
    @DisplayName("Создать черновик Beru-заказа и закоммитить его")
    // Проверяем, externalId Daas, Yandex Go и Beru заказов могут пересекаться и это не влияет на проверку уникальности
    @DatabaseSetup("/controller/order/before/order_duplicate_check.xml")
    @ExpectedDatabase(value = "/controller/order/after/waybill_indexes.xml", assertionMode = NON_STRICT_UNORDERED)
    void createBeruOrderDraftWithAutoCommit() throws Exception {
        when(lmsClient.getPartner(1L)).thenReturn(Optional.of(LmsFactory.createPartnerResponse(1L, 2L)));
        TvmClientApiTestUtil.mockTvmClientApi(tvmClientApi);
        mockMvc.perform(
            post("/orders")
                .param("autoCommit", "true")
                .contentType(MediaType.APPLICATION_JSON)
                .content(extractFileContent("controller/order/request/create_beru_order_with_auto_commit.json"))
        )
            .andExpect(status().isOk())
            .andExpect(
                jsonContent("controller/order/response/create_beru_order_with_auto_commit.json", "created", "updated")
            );
    }

    @Test
    @DisplayName("Дубль externalId отправителя для Beru заказа")
    @DatabaseSetup("/controller/order/before/order_duplicate_check.xml")
    @DatabaseSetup(
        value = "/controller/order/before/beru_order.xml",
        type = DatabaseOperation.REFRESH
    )
    void createOrderDraftSenderExternalIdDuplicateBeru() throws Exception {
        OrderTestUtil.createOrder(mockMvc, "controller/order/request/create_beru_order.json")
            .andExpect(status().isAlreadyReported())
            .andExpect(jsonContent(
                "controller/order/response/beru_order_duplicate_check.json",
                "created",
                "updated"
            ));
    }

    @Test
    @DisplayName("Дубль externalId отправителя для Yandex Go заказа")
    @DatabaseSetup("/controller/order/before/order_duplicate_check.xml")
    void createOrderDraftSenderExternalIdDuplicateYandexGo() throws Exception {
        OrderTestUtil.createOrder(mockMvc, "controller/order/request/create_yandex_go_order.json")
            .andExpect(status().isAlreadyReported())
            .andExpect(jsonContent(
                "controller/order/response/yandex_go_order_duplicate_check.json",
                "created",
                "updated"
            ));
    }

    @Test
    @DisplayName("Дубль externalId отправителя для DBS заказа")
    @DatabaseSetup("/controller/order/before/order_duplicate_check.xml")
    void createOrderDraftSenderExternalIdDuplicateDBS() throws Exception {
        OrderTestUtil.createOrder(mockMvc, "controller/order/request/create_dbs_order.json")
            .andExpect(status().isAlreadyReported())
            .andExpect(jsonContent(
                "controller/order/response/dbs_order_duplicate_check.json",
                "created",
                "updated"
            ));
    }

    @Test
    @DisplayName("Дубль externalId отправителя для FaaS заказа")
    @DatabaseSetup("/controller/order/before/order_duplicate_check.xml")
    void createOrderDraftSenderExternalIdDuplicateFaaS() throws Exception {
        OrderTestUtil.createOrder(mockMvc, "controller/order/request/create_faas_order.json")
            .andExpect(status().isAlreadyReported())
            .andExpect(jsonContent(
                "controller/order/response/faas_order_duplicate_check.json",
                "created",
                "updated"
            ));
    }

    @Test
    @DisplayName("Дубль externalId отправителя для Daas заказа")
    @DatabaseSetup("/controller/order/before/order_duplicate_check.xml")
    @ExpectedDatabase(value = "/controller/order/after/waybill_indexes.xml", assertionMode = NON_STRICT_UNORDERED)
    void createOrderDraftSenderExternalIdDuplicateDaas() throws Exception {
        when(lmsClient.getPartner(1L)).thenReturn(Optional.of(LmsFactory.createPartnerResponse(1L)));
        TvmClientApiTestUtil.mockTvmClientApi(tvmClientApi);
        OrderTestUtil.createOrder(mockMvc, "controller/order/request/create_order.json")
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/order/response/create_duplicate_order.json", "created", "updated"));
    }

    @Test
    @DisplayName("Создать черновик заказа с юзер-тикетом в хедере и проверить что проставился автор")
    @ExpectedDatabase(value = "/controller/order/after/waybill_indexes.xml", assertionMode = NON_STRICT_UNORDERED)
    void createOrderDraftWithUserTicket() throws Exception {
        when(lmsClient.getPartner(1L)).thenReturn(Optional.of(LmsFactory.createPartnerResponse(1L)));
        TvmClientApiTestUtil.mockTvmClientApi(tvmClientApi);
        OrderTestUtil.createOrder(mockMvc, "controller/order/request/create_order.json", USER_HEADERS)
            .andExpect(status().isOk());
        OrderHistoryTestUtil.assertOrderHistoryEvent(jdbcTemplate, ORDER_ID, USER_UID, null);
    }

    @Test
    @DisplayName("Создать черновик заказа с адресом получателя в маршруте")
    @ExpectedDatabase(
        value = "/controller/order/after/create_order_with_waybill_recipient.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void createOrderDraftWithRecipientAddress() throws Exception {
        when(lmsClient.getPartner(1L)).thenReturn(Optional.of(LmsFactory.createPartnerResponse(1L)));
        TvmClientApiTestUtil.mockTvmClientApi(tvmClientApi);
        OrderTestUtil.createOrder(mockMvc, "controller/order/request/create_order_with_waybill_recipient.json")
            .andExpect(status().isOk());
        OrderHistoryTestUtil.assertOrderHistoryEvent(jdbcTemplate, ORDER_ID, null, null);
    }

    @Test
    @DisplayName("Создать черновик заказа с сервис-тикетом в хедере и проверить что проставился автор")
    @ExpectedDatabase(value = "/controller/order/after/waybill_indexes.xml", assertionMode = NON_STRICT_UNORDERED)
    void createOrderDraftWithServiceTicket() throws Exception {
        when(lmsClient.getPartner(1L)).thenReturn(Optional.of(LmsFactory.createPartnerResponse(1L)));
        TvmClientApiTestUtil.mockTvmClientApi(tvmClientApi);
        OrderTestUtil.createOrder(mockMvc, "controller/order/request/create_order.json", SERVICE_HEADERS)
            .andExpect(status().isOk());
        OrderHistoryTestUtil.assertOrderHistoryEvent(jdbcTemplate, ORDER_ID, null, SERVICE_ID);
    }

    @Test
    @DisplayName("Создать черновик заказа с юзер-тикетом и сервис-тикетом в хедере и проверить что проставился автор")
    @ExpectedDatabase(value = "/controller/order/after/waybill_indexes.xml", assertionMode = NON_STRICT_UNORDERED)
    void createOrderDraftWithUserAndServiceTicket() throws Exception {
        when(lmsClient.getPartner(1L)).thenReturn(Optional.of(LmsFactory.createPartnerResponse(1L)));
        TvmClientApiTestUtil.mockTvmClientApi(tvmClientApi);
        OrderTestUtil.createOrder(
            mockMvc,
            "controller/order/request/create_order.json",
            USER_AND_SERVICE_HEADERS
        ).andExpect(status().isOk());
        OrderHistoryTestUtil.assertOrderHistoryEvent(jdbcTemplate, ORDER_ID, USER_UID, SERVICE_ID);
    }

    @Test
    @DisplayName("Новая структура товарных мест (только товары)")
    @ExpectedDatabase(
        value = "/controller/order/after/create_order_new_places_only_items.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void createOrderDraftNewPlacesOnlyItems() throws Exception {
        when(lmsClient.getPartner(1L)).thenReturn(Optional.of(LmsFactory.createPartnerResponse(1L)));
        OrderTestUtil.createOrder(mockMvc, "controller/order/request/create_order_new_places_only_items.json")
            .andExpect(status().isOk())
            .andExpect(jsonContent(
                "controller/order/response/create_order_new_places_only_items.json",
                "created",
                "updated"
            ));
        OrderHistoryTestUtil.assertOrderDiff(
            jdbcTemplate,
            1L,
            "controller/order/after/create_order_new_places_only_items.json"
        );
    }

    @Test
    @DisplayName("Новая структура товарных мест")
    @ExpectedDatabase(
        value = "/controller/order/after/create_order_new_places.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void createOrderDraftNewPlaces() throws Exception {
        when(lmsClient.getPartner(1L)).thenReturn(Optional.of(LmsFactory.createPartnerResponse(1L)));
        OrderTestUtil.createOrder(mockMvc, "controller/order/request/create_order_new_places.json")
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/order/response/create_order_new_places.json", "created", "updated"));
        OrderHistoryTestUtil.assertOrderDiff(jdbcTemplate, 1L, "controller/order/after/create_order_new_places.json");
    }

    @Test
    @DisplayName("Все уникальные некорневые места без externalId")
    @ExpectedDatabase(
        value = "/controller/order/after/create_order_new_places_with_generated_ids.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void createOrderDraftNewPlacesWithoutExternalIds() throws Exception {
        when(lmsClient.getPartner(1L)).thenReturn(Optional.of(LmsFactory.createPartnerResponse(1L)));
        OrderTestUtil.createOrder(
            mockMvc,
            "controller/order/request/create_order_new_places_without_external_ids.json"
        )
            .andExpect(status().isOk())
            .andExpect(jsonContent(
                "controller/order/response/create_order_new_places_with_generated_external_ids.json",
                "created",
                "updated"
            ));
        OrderHistoryTestUtil.assertOrderDiff(
            jdbcTemplate,
            1L,
            "controller/order/after/create_order_new_places_with_generated_external_ids.json"
        );
    }

    @Test
    @DisplayName("Все неуникальные некорневые места без externalId")
    @ExpectedDatabase(
        value = "/controller/order/after/create_order_new_non_unique_places_with_generated_ids.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void createOrderDraftNewNonUniquePlacesWithoutExternalIds() throws Exception {
        when(lmsClient.getPartner(1L)).thenReturn(Optional.of(LmsFactory.createPartnerResponse(1L)));
        OrderTestUtil.createOrder(
            mockMvc,
            "controller/order/request/create_order_new_non_unique_places_without_external_ids.json"
        )
            .andExpect(status().isOk())
            .andExpect(jsonContent(
                "controller/order/response/create_order_new_non_unique_places_with_generated_external_ids.json",
                "created",
                "updated"
            ));
        OrderHistoryTestUtil.assertOrderDiff(
            jdbcTemplate,
            1L,
            "controller/order/after/create_order_new_non_unique_places_with_generated_external_ids.json"
        );
    }

    @Test
    @DisplayName("Часть некорневых мест без externalId")
    @ExpectedDatabase(
        value = "/controller/order/after/create_order_new_places_with_generated_ids.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void createOrderDraftGenerateUniqueExternalIds() throws Exception {
        when(lmsClient.getPartner(1L)).thenReturn(Optional.of(LmsFactory.createPartnerResponse(1L)));
        OrderTestUtil.createOrder(
            mockMvc,
            "controller/order/request/create_order_generate_unique_external_ids.json"
        )
            .andExpect(status().isOk())
            .andExpect(jsonContent(
                "controller/order/response/create_order_new_places_with_generated_external_ids.json",
                "created",
                "updated"
            ));
        OrderHistoryTestUtil.assertOrderDiff(
            jdbcTemplate,
            1L,
            "controller/order/after/create_order_new_places_with_generated_external_ids.json"
        );
    }

    @Test
    @DisplayName("Черновик с товарами, связанными с грузоместами через индексы")
    @ExpectedDatabase(
        value = "/controller/order/after/create_order_new_places_with_generated_ids.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void createDraftWithPlacesIndexes() throws Exception {
        when(lmsClient.getPartner(1L)).thenReturn(Optional.of(LmsFactory.createPartnerResponse(1L)));
        OrderTestUtil.createOrder(
            mockMvc,
            "controller/order/request/create_draft_with_places_indexes.json"
        )
            .andExpect(status().isOk())
            .andExpect(jsonContent(
                "controller/order/response/create_draft_with_places_indexes.json",
                "created",
                "updated"
            ));

        OrderHistoryTestUtil.assertOrderDiff(
            jdbcTemplate,
            1L,
            "controller/order/after/create_draft_with_places_indexes.json"
        );
    }

    @Test
    @DisplayName("Создать черновик с местами, но без вейбилла")
    @ExpectedDatabase(
        value = "/controller/order/after/create_order_new_places_no_waybill.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void createDraftPlacesNoWaybill() throws Exception {
        when(lmsClient.getPartner(1L)).thenReturn(Optional.of(LmsFactory.createPartnerResponse(1L)));
        OrderTestUtil.createOrder(mockMvc, "controller/order/request/create_order_new_places_no_waybill.json")
            .andExpect(status().isOk())
            .andExpect(jsonContent(
                "controller/order/response/create_order_new_places_no_waybill.json",
                "created",
                "updated"
            ));
        OrderHistoryTestUtil.assertOrderDiff(
            jdbcTemplate,
            1L,
            "controller/order/after/create_order_new_places_no_waybill.json"
        );
    }

    @Test
    @DisplayName("Создать черновик фейкового заказа со всеми заполненными полями")
    @ExpectedDatabase(value = "/controller/order/after/create_fake_order.xml", assertionMode = NON_STRICT_UNORDERED)
    void createFakeOrderDraft() throws Exception {
        when(lmsClient.getPartner(1L)).thenReturn(Optional.of(LmsFactory.createPartnerResponse(1L)));
        OrderTestUtil.createOrder(mockMvc, "controller/order/request/create_fake_order.json")
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/order/response/create_fake_order.json", "created", "updated"));
        OrderHistoryTestUtil.assertOrderDiff(jdbcTemplate, 1L, "controller/order/after/create_fake_order_diff.json");
    }

    @Test
    @DisplayName("Создать черновик заказа с отсутствующими обязательными полями")
    void createOrderDraftBadRequest() throws Exception {
        OrderTestUtil.createOrder(mockMvc, "controller/order/request/empty_object.json")
            .andExpect(status().isBadRequest())
            .andExpect(content()
                .json(extractFileContent("controller/order/response/create_or_update_order_bad_request.json")));
    }

    @Test
    @DisplayName("Создать черновик заказа только с обязательными полями")
    @ExpectedDatabase(
        value = "/controller/order/after/create_order_with_only_required_fields.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void createOrderDraftWithOnlyRequiredFields() throws Exception {
        String relativePath = "controller/order/request/order_minimal.json";
        OrderTestUtil.createOrder(mockMvc, relativePath)
            .andExpect(status().isOk())
            .andExpect(jsonContent(
                "controller/order/response/create_order_with_only_required_fields.json",
                "created",
                "updated"
            ));
        OrderHistoryTestUtil.assertOrderDiff(jdbcTemplate, 1L, "controller/order/after/create_order_minimal_diff.json");
    }

    @Test
    @DisplayName("Неправильный формат даты")
    void nonParsableDate() throws Exception {
        OrderTestUtil.createOrder(mockMvc, "controller/order/request/non_parseable_delivery_date.json")
            .andExpect(status().isBadRequest())
            .andExpect(content()
                .json(extractFileContent("controller/order/response/non_parseable_delivery_date.json")));
    }

    @Test
    @DisplayName("Создать заказ с несуществующим партнёром")
    void createOrderDraftWithoutPartnerId() throws Exception {
        when(lmsClient.getPartner(1L)).thenReturn(Optional.empty());
        OrderTestUtil.createOrder(mockMvc, "controller/order/request/create_order.json")
            .andExpect(status().isNotFound())
            .andExpect(content().json(extractFileContent("controller/order/response/partner_not_found.json")));
    }

    @Test
    @DisplayName("Создать заказ с некорректным типом налогообложения")
    void createOrderWithInvalidTaxSystem() throws Exception {
        OrderTestUtil.createOrder(mockMvc, "controller/order/request/create_order_with_invalid_tax_system.json")
            .andExpect(status().isBadRequest())
            .andExpect(content().json(extractFileContent(
                "controller/order/response/create_order_with_invalid_tax_system.json"))
            );
    }

    @Test
    @DisplayName("Создать черновик заказа с невалидным типом сегмента")
    void createOrderWithInvalidWaybillSegmentType() throws Exception {
        OrderTestUtil.createOrder(
            mockMvc,
            "controller/order/request/create_order_with_invalid_waybill_segment_type.json"
        )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("message").value(startsWith(
                "Cannot deserialize value of type `ru.yandex.market.logistics.lom.model.enums.SegmentType` from "
                    + "String \"SSHIPMENTT\": value not one of declared Enum instance names: "
                    + "[POST, NO_OPERATION, GO_PLATFORM, PICKUP, COURIER, FULFILLMENT, MOVEMENT, SORTING_CENTER, "
                    + "SUPPLIER]"
            )));
    }

    @Test
    @DisplayName("Создать черновик заказа из OpenApi и проверить что проставился тег")
    @ExpectedDatabase(
        value = "/controller/order/after/create_order_from_open_api.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(value = "/controller/order/after/waybill_indexes.xml", assertionMode = NON_STRICT_UNORDERED)
    void createOrderDraftFromOpenApi() throws Exception {
        when(lmsClient.getPartner(1L)).thenReturn(Optional.of(LmsFactory.createPartnerResponse(1L)));
        TvmClientApiTestUtil.mockTvmClientApi(tvmClientApi);
        mockMvc.perform(
            post("/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .param("source", OrderTag.CREATED_VIA_DAAS_OPEN_API.name())
                .content(extractFileContent("controller/order/request/create_order_with_source_open_api.json")))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Создать черновик заказа из ЛК и проверить что проставился тег")
    @ExpectedDatabase(
        value = "/controller/order/after/create_order_from_back_office.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(value = "/controller/order/after/waybill_indexes.xml", assertionMode = NON_STRICT_UNORDERED)
    void createOrderDraftFromBackOffice() throws Exception {
        when(lmsClient.getPartner(1L)).thenReturn(Optional.of(LmsFactory.createPartnerResponse(1L)));
        TvmClientApiTestUtil.mockTvmClientApi(tvmClientApi);
        mockMvc.perform(
            post("/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(extractFileContent("controller/order/request/create_order_with_source_back_office.json")))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Создать черновик заказа с не null sourceId")
    @DatabaseSetup("/controller/order/before/logbroker_source_lock.xml")
    @ExpectedDatabase(
        value = "/controller/order/after/create_order_with_only_required_fields_source_id.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void createOrderDraftWithSourceId() throws Exception {
        String relativePath = "controller/order/request/order_minimal.json";
        OrderTestUtil.createOrder(mockMvc, relativePath)
            .andExpect(status().isOk())
            .andExpect(jsonContent(
                "controller/order/response/create_order_with_only_required_fields.json",
                "created",
                "updated"
            ));
        OrderHistoryTestUtil.assertOrderDiff(
            jdbcTemplate,
            1L,
            "controller/order/after/create_order_minimal_source_id_diff.json"
        );
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("createOrderWithoutFieldsArgumentProvider")
    @DisplayName("Создать заказ с незаполненными полями")
    void createOrderWithoutFields(
        String requestPath,
        String responsePath,
        @SuppressWarnings("unused") String displayName
    ) throws Exception {
        when(lmsClient.getPartner(1L)).thenReturn(Optional.of(LmsFactory.createPartnerResponse(1L)));
        OrderTestUtil.createOrder(mockMvc, requestPath)
            .andExpect(status().isOk())
            .andExpect(jsonContent(responsePath, "created", "updated"));
    }

    private static Stream<Arguments> createOrderWithoutFieldsArgumentProvider() {
        return Stream.of(
            Arguments.of(
                "controller/order/request/create_order_without_contact_type.json",
                "controller/order/response/create_order_without_contact_type.json",
                "Заказ с заполненным контактом, но без contactType"
            ),
            Arguments.of(
                "controller/order/request/create_order_without_phone.json",
                "controller/order/response/create_order_without_phone.json",
                "Заказ с заполненным контактом, но без phone"
            ),
            Arguments.of(
                "controller/order/request/create_order_without_fio.json",
                "controller/order/response/create_order_without_fio.json",
                "Заказ с заполненным контактом, но без fio"
            ),
            Arguments.of(
                "controller/order/request/create_order_without_shipment.json",
                "controller/order/response/create_order_without_shipment.json",
                "Заказ с заполненным сегментом путевого листа, но без shipment"
            ),
            Arguments.of(
                "controller/order/request/create_order_without_item_price.json",
                "controller/order/response/create_order_without_item_price.json",
                "Заказ с заполненным товаром, но без price"
            ),
            Arguments.of(
                "controller/order/request/create_order_without_item_assessed_value.json",
                "controller/order/response/create_order_without_item_assessed_value.json",
                "Заказ с заполненным товаром, но без assessedValue"
            ),
            Arguments.of(
                "controller/order/request/create_order_without_item_name.json",
                "controller/order/response/create_order_without_item_name.json",
                "Заказ с заполненным товаром, но без name"
            )
        );
    }
}
