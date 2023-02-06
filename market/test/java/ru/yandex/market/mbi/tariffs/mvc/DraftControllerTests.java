package ru.yandex.market.mbi.tariffs.mvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.spring.RestTemplateFactory;
import ru.yandex.market.mbi.tariffs.Constants;
import ru.yandex.market.mbi.tariffs.FunctionalTest;
import ru.yandex.market.mbi.tariffs.TestUtils;
import ru.yandex.market.mbi.tariffs.Utils;
import ru.yandex.market.mbi.tariffs.matcher.DraftDTOMatcher;
import ru.yandex.market.mbi.tariffs.matcher.ErrorInfoMatcher;
import ru.yandex.market.mbi.tariffs.model.BillingUnitEnum;
import ru.yandex.market.mbi.tariffs.model.CommonJsonSchema;
import ru.yandex.market.mbi.tariffs.model.DistributionJsonSchema;
import ru.yandex.market.mbi.tariffs.model.DraftChangeDTO;
import ru.yandex.market.mbi.tariffs.model.DraftDTO;
import ru.yandex.market.mbi.tariffs.model.DraftsFindQuery;
import ru.yandex.market.mbi.tariffs.model.ModelType;
import ru.yandex.market.mbi.tariffs.model.PagerResponseInfo;
import ru.yandex.market.mbi.tariffs.model.PartnersFilter;
import ru.yandex.market.mbi.tariffs.model.ServiceTypeEnum;
import ru.yandex.market.partner.error.info.model.ErrorInfo;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static ru.yandex.market.common.test.spring.FunctionalTestHelper.get;
import static ru.yandex.market.common.test.spring.FunctionalTestHelper.post;
import static ru.yandex.market.common.test.spring.FunctionalTestHelper.postForJson;
import static ru.yandex.market.mbi.tariffs.TestUtils.getErrors;
import static ru.yandex.market.mbi.tariffs.TestUtils.parseOneResult;
import static ru.yandex.market.mbi.tariffs.TestUtils.parsePagerResponse;
import static ru.yandex.market.mbi.tariffs.matcher.DraftDTOMatcher.hasApprovalTicket;
import static ru.yandex.market.mbi.tariffs.matcher.DraftDTOMatcher.hasDateFrom;
import static ru.yandex.market.mbi.tariffs.matcher.DraftDTOMatcher.hasDateTo;
import static ru.yandex.market.mbi.tariffs.matcher.DraftDTOMatcher.hasId;
import static ru.yandex.market.mbi.tariffs.matcher.DraftDTOMatcher.hasMetaSize;
import static ru.yandex.market.mbi.tariffs.matcher.DraftDTOMatcher.hasModelType;
import static ru.yandex.market.mbi.tariffs.matcher.DraftDTOMatcher.hasServiceType;
import static ru.yandex.market.mbi.tariffs.matcher.DraftDTOMatcher.hasTags;
import static ru.yandex.market.mbi.tariffs.matcher.DraftDTOMatcher.hasUpdatedTime;
import static ru.yandex.market.mbi.tariffs.matcher.ErrorInfoMatcher.hasCode;
import static ru.yandex.market.mbi.tariffs.matcher.ErrorInfoMatcher.hasMessage;

/**
 * Тесты для {@link ru.yandex.market.mbi.tariffs.mvc.controller.DraftsController}
 */
public class DraftControllerTests extends FunctionalTest {

    @Test
    @DisplayName("Тест на получение черновика по идентификатору")
    @DbUnitDataSet(
            before = "draft/getDraftById.before.csv"
    )
    void testGetDraftById() {
        ResponseEntity<String> response = get(baseUrl() + "/drafts/1");
        assertOk(response);
        DraftDTO draftDTO = parseOneResult(response.getBody(), DraftDTO.class);
        assertThat(draftDTO, allOf(
                hasId(1L),
                hasServiceType(ServiceTypeEnum.DISTRIBUTION),
                hasModelType(ModelType.FULFILLMENT_BY_YANDEX),
                DraftDTOMatcher.hasPartner(Constants.Partners.VALID_PARTNER_SHOP),
                hasDateFrom(LocalDate.of(2020, 9, 1)),
                hasDateTo(LocalDate.of(2020,10,1)),
                hasTags(List.of("tag1", "tag2")),
                hasUpdatedTime(OffsetDateTime.of(LocalDateTime.of(2020, 8, 10, 20, 0, 0), ZoneOffset.UTC)),
                hasMetaSize(3),
                hasApprovalTicket("MBI-54580")
        ));

        List<DistributionJsonSchema> meta = TestUtils.convert(draftDTO.getMeta(), DistributionJsonSchema.class);
        DistributionJsonSchema firstMeta = meta.get(0);
        assertEquals(new BigDecimal("1.80"), firstMeta.getAmount());
        assertEquals(CommonJsonSchema.TypeEnum.RELATIVE, firstMeta.getType());
        assertEquals(198119L, (long) firstMeta.getCategoryId());

        assertEquals("CEHAC", firstMeta.getTariffName());
        assertEquals("closer-others", firstMeta.getPartnerSegmentTariffKey());

    }

    @Test
    @DisplayName("Тест на получение черновика, у которого нет тегов, по идентификатору")
    @DbUnitDataSet(
            before = "draft/testGetDraftWithoutTagsById.before.csv"
    )
    void testGetDraftWithoutTagsById() {
        ResponseEntity<String> response = get(baseUrl() + "/drafts/1");
        assertOk(response);
        DraftDTO draftDTO = parseOneResult(response.getBody(), DraftDTO.class);
        assertThat(draftDTO, allOf(
                hasId(1L),
                hasServiceType(ServiceTypeEnum.DISTRIBUTION),
                DraftDTOMatcher.hasPartner(null),
                hasDateFrom(LocalDate.of(2020, 9, 1)),
                hasDateTo(LocalDate.of(2020,10,1)),
                hasTags(List.of()),
                hasUpdatedTime(OffsetDateTime.of(LocalDateTime.of(2020, 8, 10, 20, 0, 0), ZoneOffset.UTC)),
                hasMetaSize(1)
        ));
    }

    @Test
    @DisplayName("Получение клиентской ошибки, если черновик не найден по идентификатору")
    void testGetDraftByIdNotFound() {
        HttpClientErrorException exception = assertThrows(
                HttpClientErrorException.class,
                () -> get(baseUrl() + "/drafts/1")
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        List<ErrorInfo> errors = getErrors(exception.getResponseBodyAsString());
        assertThat(errors, hasSize(1));
        assertThat(errors.get(0), allOf(
                hasCode("ENTITY_NOT_FOUND"),
                hasMessage("The draft with id 1 is not found")
        ));
    }

    @Test
    @DisplayName("Тест на удаление черновиков, часть из которых не существует (или уже удалены)")
    @DbUnitDataSet(before = "draft/deleted.before.csv")
    void testDeleteNotExistedDrafts() {
        HttpClientErrorException exception = assertThrows(
                HttpClientErrorException.class,
                () -> postForJson(
                        baseUrl() + "/drafts/delete",
                        /*language=json*/ "{\"draftIds\": [1,2,3]}"
                )
        );
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        List<ErrorInfo> errors = getErrors(exception.getResponseBodyAsString());
        assertThat(errors, hasSize(1));
        assertThat(errors.get(0), allOf(
                hasCode("ENTITY_NOT_FOUND"),
                hasMessage("Unknown draft identifiers: [2, 3]")
        ));

    }

    @Test
    @DisplayName("Тест на удаление множества (больше 10) черновиков, часть из которых не существует (или уже удалены)")
    @DbUnitDataSet(before = "draft/deleted.before.csv")
    void testDeleteNotExistedDraftsBigCount() {
        HttpClientErrorException exception = assertThrows(
                HttpClientErrorException.class,
                () -> postForJson(
                        baseUrl() + "/drafts/delete",
                        /*language=json*/ "{ \"draftIds\": [1,2,3,4,5,6,7,8,9,10,11,12,13] }"
                )
        );
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        List<ErrorInfo> errors = getErrors(exception.getResponseBodyAsString());
        assertThat(errors, hasSize(1));
        assertThat(errors.get(0), allOf(
                hasCode("ENTITY_NOT_FOUND"),
                hasMessage("Unknown draft identifiers: [2, 3, 5, 6, 7, 8, 9, 10, 11, 12 and 1 more]")
        ));

    }

    @Test
    @DisplayName("Тест на нормальное удаление черновиков")
    @DbUnitDataSet(
            before = "draft/deleted.before.csv",
            after = "draft/deleted.after.csv"
    )
    void testDeleteDrafts() {
        ResponseEntity<String> response = RestTemplateFactory.createJsonRestTemplate().exchange(
                baseUrl() + "/drafts/delete",
                HttpMethod.POST,
                createHttpRequestEntity("{\"draftIds\": [4]}"),
                String.class
        );
        assertOk(response);
    }

    @Test
    @DisplayName("Тест на получение удаленных черновиков")
    @DbUnitDataSet(before = "draft/deleted.before.csv")
    void testGetDeletedDrafts() {
        ResponseEntity<String> response = get(baseUrl() + "/drafts/deleted");
        assertOk(response);
        PagerResponseInfo pagerResponse = TestUtils.parsePagerResponse(response.getBody(), DraftDTO.class);
        assertThat(pagerResponse.getTotalCount(), is(2L));
        List<DraftDTO> items = pagerResponse.getItems().stream()
                .map(obj -> (DraftDTO) obj)
                .collect(Collectors.toList());
        assertThat(items, hasSize(2));
        assertThat(
                items.stream().map(DraftDTO::getId).collect(Collectors.toList()),
                contains(3L, 2L)
        );
    }

    @Test
    @DisplayName("Тест на воскрешение удаленных черновиков, часть из которых не существует")
    @DbUnitDataSet(before = "draft/activateDeleted.before.csv")
    void testActivateDeletedUnknownDrafts() {
        HttpClientErrorException exception = assertThrows(
                HttpClientErrorException.class,
                () -> postForJson(baseUrl() + "/drafts/activate", /*language=json*/ "{\"draftIds\": [1,2,3]}")
        );
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        List<ErrorInfo> errors = getErrors(exception.getResponseBodyAsString());
        assertThat(errors, hasSize(1));
        assertThat(errors.get(0), allOf(
                hasCode("ENTITY_NOT_FOUND"),
                hasMessage("Unknown draft identifiers: [1]")
        ));
    }

    @Test
    @DisplayName("Тест на воскрешение удаленных черновиков")
    @DbUnitDataSet(
            before = "draft/activateDeleted.before.csv",
            after = "draft/activateDeleted.after.csv"
    )
    void testActivateDeletedDrafts() {
        ResponseEntity<String> response = RestTemplateFactory.createJsonRestTemplate().exchange(
                baseUrl() + "/drafts/activate",
                HttpMethod.POST,
                createHttpRequestEntity("{\"draftIds\": [3]}"),
                String.class
        );
        assertOk(response);
    }

    @Test
    @DisplayName("Тест на успешное создание черновика")
    @DbUnitDataSet(
            before = "draft/createDraft.based.on.tariff.before.csv",
            after = "draft/createDraft.based.on.tariff.after.csv"
    )
    void testCreateDraftBasedOnExistedTariff() {
        DraftChangeDTO draftChange = new DraftChangeDTO()
                .id(null)
                .tariffId(1L)
                .serviceType(ServiceTypeEnum.DISTRIBUTION)
                .dateFrom(LocalDate.of(2020, 9, 1))
                .dateTo(LocalDate.of(2020, 10, 1))
                .tags(List.of("tag1", "tag2"))
                .approvalTicketId("MBI-54580")
                .modelType(ModelType.FULFILLMENT_BY_SELLER)
                .meta(Utils.convert(
                        List.of(new DistributionJsonSchema()
                                .categoryId(198119L)
                                .tariffName("CEHAC")
                                .amount(new BigDecimal("1.8")) //1.8%
                                .type(CommonJsonSchema.TypeEnum.RELATIVE)
                                .currency("RUB")
                                .billingUnit(BillingUnitEnum.ORDER)
                        )
                ));

        HttpEntity<String> bodyEntity = createHttpRequestEntity(draftChange);
        ResponseEntity<String> response = post(baseUrl() + "/drafts", bodyEntity);
        assertOk(response);
        long id = parseOneResult(response.getBody(), Long.class);
        assertEquals(1L, id);
    }

    @Test
    @DisplayName("Тест на успешное создание черновика на основе тарифа")
    @DbUnitDataSet(after = "draft/createDraft.without.based.tariff.after.csv")
    void testCreateDraftWithoutBasedTariff() {
        DraftChangeDTO draftChange = new DraftChangeDTO()
                .id(null)
                .serviceType(ServiceTypeEnum.DISTRIBUTION)
                .dateFrom(LocalDate.of(2020, 9, 1))
                .dateTo(LocalDate.of(2020, 10, 1))
                .tags(List.of("tag1", "tag2"))
                .approvalTicketId("MBI-54580")
                .modelType(ModelType.FULFILLMENT_BY_SELLER)
                .meta(Utils.convert(
                        List.of(new DistributionJsonSchema()
                                .categoryId(198119L)
                                .tariffName("CEHAC")
                                .partnerSegmentTariffKey("marketing-bloggers")
                                .amount(new BigDecimal("1.8")) //1.8%
                                .type(CommonJsonSchema.TypeEnum.RELATIVE)
                                .currency("RUB")
                                .billingUnit(BillingUnitEnum.ORDER)
                        )
                ));

        HttpEntity<String> bodyEntity = createHttpRequestEntity(draftChange);
        ResponseEntity<String> response = post(baseUrl() + "/drafts", bodyEntity);
        assertOk(response);
        long id = parseOneResult(response.getBody(), Long.class);
        assertEquals(1L, id);
    }

    @Test
    @DisplayName("Тест на не успешное создание черновика, т.к. не существует тарифа, на основе которого создается черновик")
    void testCreateDraftWithNotExistedTariff() {
        DraftChangeDTO draftChange = new DraftChangeDTO()
                .id(null)
                .tariffId(101L)
                .serviceType(ServiceTypeEnum.DISTRIBUTION)
                .dateFrom(LocalDate.of(2020, 9, 1))
                .dateTo(LocalDate.of(2020, 10, 1))
                .modelType(ModelType.FULFILLMENT_BY_YANDEX)
                .tags(List.of("tag1", "tag2"))
                .meta(Utils.convert(
                        List.of(new DistributionJsonSchema()
                                .categoryId(198119L)
                                .tariffName("CEHAC")
                                .amount(new BigDecimal("0.018"))
                                .type(CommonJsonSchema.TypeEnum.RELATIVE)
                                .currency("RUB")
                                .billingUnit(BillingUnitEnum.ORDER)
                        )
                ));
        HttpClientErrorException exception = assertThrows(HttpClientErrorException.class, () -> {
            HttpEntity<String> bodyEntity = createHttpRequestEntity(draftChange);
            post(baseUrl() + "/drafts", bodyEntity);
        });
        List<ErrorInfo> errors = getErrors(exception.getResponseBodyAsString());
        assertThat(errors, hasSize(1));
        assertThat(errors.get(0), allOf(
                ErrorInfoMatcher.hasCode("ENTITY_NOT_FOUND"),
                ErrorInfoMatcher.hasMessage("The tariff with id 101 is not found")
        ));
    }

    @Test
    @DisplayName("Тест на не успешное создание черновика, т.к. у тарифа уже есть черновик его")
    @DbUnitDataSet(before = "draft/alreadyAssociated.before.csv")
    void testCreateDraftWithAlreadyAssociatedTariff() {
        DraftChangeDTO draftChange = new DraftChangeDTO()
                .tariffId(1L)
                .serviceType(ServiceTypeEnum.DISTRIBUTION)
                .dateFrom(LocalDate.of(2020, 9, 1))
                .dateTo(LocalDate.of(2020, 10, 1))
                .modelType(ModelType.FULFILLMENT_BY_YANDEX)
                .meta(Utils.convert(
                        List.of(new DistributionJsonSchema()
                                .categoryId(198119L)
                                .tariffName("CEHAC")
                                .partnerSegmentTariffKey("marketing-others")
                                .amount(new BigDecimal("0.018"))
                                .type(CommonJsonSchema.TypeEnum.RELATIVE)
                                .currency("RUB")
                                .billingUnit(BillingUnitEnum.ORDER)
                        )
                ));
        HttpClientErrorException exception = assertThrows(HttpClientErrorException.class, () -> {
            HttpEntity<String> bodyEntity = createHttpRequestEntity(draftChange);
            post(baseUrl() + "/drafts", bodyEntity);
        });

        List<ErrorInfo> errors = getErrors(exception.getResponseBodyAsString());
        assertThat(errors, hasSize(1));
        assertThat(errors.get(0), allOf(
                ErrorInfoMatcher.hasCode("BAD_PARAM"),
                ErrorInfoMatcher.hasMessage("The tariff with id 1 is already associated with draft 1")
        ));
    }

    @Test
    @DisplayName("Тест на не успешное обновление черновика, который связан с тарифом")
    @DbUnitDataSet(before = "draft/updateDraftWhichAssociatedWithTariff.before.csv")
    void testUpdateDraftWhichAssociatedWithTariff() {
        DraftChangeDTO draftChange = new DraftChangeDTO()
                .tariffId(1L)
                .serviceType(ServiceTypeEnum.DISTRIBUTION)
                .dateFrom(LocalDate.of(2020, 9, 1))
                .dateTo(LocalDate.of(2020, 10, 1))
                .modelType(ModelType.FULFILLMENT_BY_YANDEX)
                .meta(Utils.convert(
                        List.of(new DistributionJsonSchema()
                                .categoryId(198119L)
                                .tariffName("CEHAC")
                                .amount(new BigDecimal("0.018"))
                                .type(CommonJsonSchema.TypeEnum.RELATIVE)
                                .currency("RUB")
                                .billingUnit(BillingUnitEnum.ORDER)
                        )
                ));

        //создаем драфт на основе 1 тарифа
        ResponseEntity<String> createDraftResponseEntity = post(
                baseUrl() + "/drafts",
                createHttpRequestEntity(draftChange)
        );
        assertOk(createDraftResponseEntity);

        long createdDraftId = parseOneResult(createDraftResponseEntity.getBody(), long.class);

        //создаем изменение драфта, в котором меняем даты
        draftChange = draftChange.id(createdDraftId)
                .dateFrom(LocalDate.of(2020, 10, 1))
                .dateTo(LocalDate.of(2020, 11, 1));

        //обновляем драфт
        ResponseEntity<String> updateDraftResponseEntity = post(
                baseUrl() + "/drafts",
                new HttpEntity<>(draftChange, jsonHeaders())
        );
        assertOk(updateDraftResponseEntity);
    }

    @Test
    @DisplayName("Тест на успешное создание c партнер ид")
    @DbUnitDataSet(
            before = "draft/createDrafts.withPartner.before.csv",
            after = "draft/createDrafts.withPartner.after.csv"
    )
    void testCreateDraftWithPartner() {
        DraftChangeDTO draftChange = new DraftChangeDTO()
                .id(null)
                .tariffId(1L)
                .serviceType(ServiceTypeEnum.DISTRIBUTION)
                .dateFrom(LocalDate.of(2020, 9, 1))
                .dateTo(LocalDate.of(2020, 10, 1))
                .tags(List.of("tag1", "tag2"))
                .partner(Constants.Partners.VALID_PARTNER_SHOP)
                .approvalTicketId("MBI-54580")
                .modelType(ModelType.FULFILLMENT_BY_SELLER)
                .meta(Utils.convert(
                        List.of(new DistributionJsonSchema()
                                .categoryId(198119L)
                                .tariffName("CEHAC")
                                .amount(new BigDecimal("1.8")) // 1.8%
                                .type(CommonJsonSchema.TypeEnum.RELATIVE)
                                .currency("RUB")
                                .billingUnit(BillingUnitEnum.ORDER)
                        )
                ));

        HttpEntity<String> bodyEntity = createHttpRequestEntity(draftChange);
        ResponseEntity<String> response = post(baseUrl() + "/drafts", bodyEntity);
        assertOk(response);
        long id = parseOneResult(response.getBody(), Long.class);
        assertEquals(1L, id);
    }

    @Test
    @DisplayName("Тест на успешное изменение черновика")
    @DbUnitDataSet(
            before = "draft/updateDraft.before.csv",
            after = "draft/updateDraft.after.csv"
    )
    void testUpdateDraft() {
        DraftChangeDTO draftChange = new DraftChangeDTO()
                .id(1L)
                .serviceType(ServiceTypeEnum.DISTRIBUTION)
                .dateFrom(LocalDate.of(2020, 9, 1))
                .dateTo(LocalDate.of(2020, 11, 1))
                .partner(Constants.Partners.VALID_PARTNER_SHOP)
                .approvalTicketId("MBI-54580")
                .modelType(ModelType.FULFILLMENT_BY_SELLER)
                .meta(List.of(new DistributionJsonSchema()
                        .categoryId(198119L)
                        .tariffName("CEHAC")
                        .amount(new BigDecimal("1")) // 1%
                        .type(CommonJsonSchema.TypeEnum.RELATIVE)
                        .currency("RUB")
                        .billingUnit(BillingUnitEnum.ORDER)
                ));

        HttpEntity<String> bodyEntity = createHttpRequestEntity(draftChange);
        ResponseEntity<String> response = post(baseUrl() + "/drafts", bodyEntity);
        assertOk(response);

        long id = parseOneResult(response.getBody(), Long.class);
        assertEquals(1L, id);
    }

    @Test
    @DisplayName("Тест на получение черновиков с пагинацией")
    @DbUnitDataSet(
            before = "draft/getDraftsPaging.csv"
    )
    void testGetDraftsPaging() {
        ResponseEntity<String> response = get(baseUrl() + "/drafts?sortBy=dateFrom&page=0&pageSize=3");
        assertOk(response);
        PagerResponseInfo pagerResponse = parsePagerResponse(response.getBody(), DraftDTO.class);
        assertThat(pagerResponse.getTotalCount(), is(7L));
        assertThat(pagerResponse.getItems()
                        .stream()
                        .map(o -> ((DraftDTO) o).getId())
                        .collect(Collectors.toList()),
                contains(1L, 2L, 3L)
        );
    }

    @ParameterizedTest(name = "[{index}] {4}")
    @MethodSource("updateOrCreateDraftFailedTestData")
    @DbUnitDataSet(before = "draft/testUpdateDraftFailed.before.csv")
    @DisplayName("Тест на не успешное изменение черновика")
    void testUpdateDraftFailed(
            DraftChangeDTO draftChange,
            String expectedCode,
            String expectedMessage,
            Map<String, String> expectedDetails,
            String testName
    ) {
        HttpClientErrorException exception = assertThrows(
                HttpClientErrorException.class,
                () -> post(baseUrl() + "/drafts", createHttpRequestEntity(draftChange))
        );
        List<ErrorInfo> errors = getErrors(exception.getResponseBodyAsString());
        assertThat(errors, hasSize(1));
        assertThat(errors.get(0), allOf(
                hasCode(expectedCode),
                hasMessage(expectedMessage)
        ));

        Map<String, String> errorDetails = TestUtils.getErrorDetails(exception.getResponseBodyAsString(), 0);
        assertThat(expectedDetails, equalTo(errorDetails));
    }

    private static Stream<Arguments> updateOrCreateDraftFailedTestData() {
        return Stream.of(
                Arguments.of(
                        validDraftChangeDTO().id(10L),
                        "ENTITY_NOT_FOUND",
                        "The draft with id 10 is not found",
                        Map.of(),
                        "The draft with id 10 is not found"
                ),
                Arguments.of(
                        validDraftChangeDTO().dateFrom(null),
                        "BAD_PARAM",
                        null,
                        Map.of("reason", "must not be null", "field", "dateFrom", "subcode", "INVALID"),
                        "DateFrom must be not null"
                ),
                Arguments.of(
                        validDraftChangeDTO().serviceType(null),
                        "BAD_PARAM",
                        null,
                        Map.of("reason", "must not be null", "field", "serviceType", "subcode", "INVALID"),
                        "ServiceType must be not null"
                ),
                Arguments.of(
                        validDraftChangeDTO().partner(Constants.Partners.INVALID_PARTNER),
                        "ENTITY_NOT_FOUND",
                        "Business partner with id [100] doesn't exist",
                        Map.of(),
                        "Business partner with id [100] doesn't exist"
                ),
                Arguments.of(
                        validDraftChangeDTO().tariffId(1000L).serviceType(ServiceTypeEnum.FIXED_TARIFFS),
                        "BAD_PARAM",
                        "Draft and based tariff have difference serviceType",
                        Map.of(),
                        "Draft and based tariff have difference serviceType"
                )
        );
    }

    private static DraftChangeDTO validDraftChangeDTO() {
        return new DraftChangeDTO()
                .id(null)
                .serviceType(ServiceTypeEnum.DISTRIBUTION)
                .dateFrom(LocalDate.of(2020, 9, 1))
                .dateTo(LocalDate.of(2020, 10, 1))
                .tags(List.of("tag1", "tag2"))
                .approvalTicketId("MBI-54580")
                .modelType(ModelType.FULFILLMENT_BY_YANDEX)
                .meta(Utils.convert(
                        List.of(new DistributionJsonSchema()
                                .categoryId(198119L)
                                .tariffName("CEHAC")
                                .amount(new BigDecimal("0.018"))
                                .type(CommonJsonSchema.TypeEnum.RELATIVE)
                                .currency("RUB")
                                .billingUnit(BillingUnitEnum.ORDER)
                        )
                ));
    }

    @ParameterizedTest(name = "[{index}] {displayName}")
    @MethodSource("findDraftArguments")
    @DisplayName("Тест на поиск черновиков")
    @DbUnitDataSet(before = "draft/find.before.csv")
    void testFindDrafts(
            DraftsFindQuery findQuery,
            long expectedTotalCount,
            List<Long> expectedIds
    ) {
        HttpEntity<String> bodyEntity = createHttpRequestEntity(findQuery);
        ResponseEntity<String> response = post(baseUrl() + "/drafts/find?page=0&pageSize=100&sortBy=id", bodyEntity);
        assertOk(response);

        PagerResponseInfo pagerResponse = parsePagerResponse(response.getBody(), DraftDTO.class);
        assertEquals(expectedTotalCount, (long) pagerResponse.getTotalCount());
        assertThat(
                pagerResponse.getItems().stream()
                        .map(obj -> (DraftDTO) obj)
                        .map(DraftDTO::getId)
                        .collect(Collectors.toList()),
                is(expectedIds)
        );
    }

    static Stream<Arguments> findDraftArguments() {
        return Stream.of(
                Arguments.of( // 1
                        new DraftsFindQuery(),
                        7,
                        List.of(1L, 2L, 3L, 4L, 7L, 8L, 9L)
                ),
                Arguments.of( // 2
                        new DraftsFindQuery().updatedBy("test"),
                        3,
                        List.of(7L, 8L, 9L)
                ),
                Arguments.of( // 3
                        new DraftsFindQuery().updatedBy("test1").partner(Constants.Partners.VALID_PARTNER_SUPPLIER),
                        1,
                        List.of(4L)
                ),
                Arguments.of( // 4
                        new DraftsFindQuery().tags(List.of("tag1", "tag2")),
                        1,
                        List.of(1L)
                ),
                Arguments.of( // 5
                        new DraftsFindQuery().tags(List.of("tag1")),
                        2,
                        List.of(1L, 2L)
                ),
                Arguments.of( // 6
                        new DraftsFindQuery()
                                .serviceType(ServiceTypeEnum.DISTRIBUTION)
                                .dateFromLower(LocalDate.of(2020, 8, 1))
                                .dateFromUpper(LocalDate.of(2021, 1, 1))
                                .dateToLower(LocalDate.of(2020, 8, 1))
                                .dateToUpper(LocalDate.of(2020, 11, 1))
                                .partner(Constants.Partners.VALID_PARTNER_SUPPLIER)
                                .updatedTimeFrom(Utils.toOffsetDateTime(LocalDateTime.of(2020, 8, 10, 20, 3, 0)))
                                .updatedTimeTo(Utils.toOffsetDateTime(LocalDateTime.of(2020, 8, 10, 21, 4, 0)))
                                .updatedBy("andreybystrov"),
                        1,
                        List.of(3L)
                ),
                Arguments.of( // 7
                        new DraftsFindQuery().approvalTicketId("MBI-123456"),
                        1,
                        List.of(9L)
                ),
                Arguments.of( // 8
                        new DraftsFindQuery().modelType(ModelType.FULFILLMENT_BY_YANDEX),
                        3,
                        List.of(1L, 2L, 3L)
                ),
                Arguments.of( // 9
                        new DraftsFindQuery().serviceTypes(List.of(ServiceTypeEnum.FEE, ServiceTypeEnum.FF_PROCESSING)),
                        3,
                        List.of(1L, 2L, 7L)
                ),
                Arguments.of( // 10
                        new DraftsFindQuery().serviceTypes(null),
                        7,
                        List.of(1L, 2L, 3L, 4L, 7L, 8L, 9L)
                ),
                Arguments.of( // 11
                        new DraftsFindQuery().serviceTypes(List.of()),
                        0,
                        List.of()
                ),
                Arguments.of( // 12
                        new DraftsFindQuery().partner(Constants.Partners.VALID_PARTNER_SUPPLIER),
                        2,
                        List.of(3L, 4L)
                ),
                Arguments.of( // 13
                        new DraftsFindQuery().partner(Constants.Partners.VALID_PARTNER_SUPPLIER),
                        2,
                        List.of(3L, 4L)
                ),
                Arguments.of( // 14
                        new DraftsFindQuery().filterByPartners(PartnersFilter.GENERAL),
                        4,
                        List.of(1L, 2L, 7L, 8L) // 5 и 6 удаленные
                ),
                Arguments.of( // 15
                        new DraftsFindQuery().filterByPartners(PartnersFilter.CUSTOM),
                        3,
                        List.of(3L, 4L, 9L)
                ),
                Arguments.of( // 16
                        new DraftsFindQuery()
                                .filterByPartners(PartnersFilter.GENERAL)
                                .partner(Constants.Partners.VALID_PARTNER_SHOP),
                        4,
                        List.of(1L, 2L, 7L, 8L) // 5 и 6 удаленные
                ),
                Arguments.of( // 17
                        new DraftsFindQuery()
                                .filterByPartners(PartnersFilter.GENERAL)
                                .partner(Constants.Partners.VALID_PARTNER_SHOP),
                        4,
                        List.of(1L, 2L, 7L, 8L) // 5 и 6 удаленные
                ),
                Arguments.of( // 18
                        new DraftsFindQuery()
                                .filterByPartners(PartnersFilter.CUSTOM)
                                .partner(Constants.Partners.VALID_PARTNER_SUPPLIER),
                        3,
                        List.of(3L, 4L, 9L)
                ),
                Arguments.of( // 19
                        new DraftsFindQuery()
                                .filterByPartners(PartnersFilter.CUSTOM)
                                .partner(Constants.Partners.VALID_PARTNER_SUPPLIER),
                        3,
                        List.of(3L, 4L, 9L)
                ),
                Arguments.of( // 20
                        new DraftsFindQuery().id(1L),
                        1,
                        List.of(1L)
                )
        );
    }

    @Test
    @DisplayName("Тест на ручку по изменению тегов")
    @DbUnitDataSet(
            before = "draft/updateTags.before.csv",
            after = "draft/updateTags.after.csv"
    )
    void testUpdateTags() {
        HttpEntity<String> bodyEntity = createHttpRequestEntity("[\"anotherTag1\", \"anotherTag2\"]");
        ResponseEntity<String> response = post(baseUrl() + "/drafts/1/tags", bodyEntity);
        assertOk(response);
    }

    @Test
    @DisplayName("Тест на ручку по получение ошибки по изменению тегов")
    @DbUnitDataSet()
    void testUpdateTagsFailed() {
        HttpEntity<String> bodyEntity = createHttpRequestEntity("[\"anotherTag1\", \"anotherTag2\"]");
        HttpClientErrorException exception = assertThrows(
                HttpClientErrorException.class,
                () -> post(baseUrl() +"/drafts/101/tags", bodyEntity)
        );
        List<ErrorInfo> errors = getErrors(exception.getResponseBodyAsString());
        assertThat(errors, hasSize(1));
        assertThat(errors.get(0).getMessage(), is("The draft with id 101 is not found"));
    }

    @Test
    @DbUnitDataSet(
            before = "draft/commit.success.before.csv",
            after = "draft/commit.success.after.csv"
    )
    void testDraftCommitSuccess() {
        ResponseEntity<String> response = post(baseUrl() + "/drafts/1/commit");
        assertOk(response);

        long tariffId = parseOneResult(response.getBody(), long.class);
        assertEquals(1L, tariffId);
    }

    @Test
    @DbUnitDataSet(
            before = "draft/commit.with.partner.success.before.csv",
            after = "draft/commit.with.partner.success.after.csv"
    )
    void testDraftWithPartnerCommitSuccess() {
        ResponseEntity<String> response = post(baseUrl() + "/drafts/1/commit");
        assertOk(response);

        long tariffId = parseOneResult(response.getBody(), long.class);
        assertEquals(1L, tariffId);
    }

    @Test
    @DbUnitDataSet(
            before = "draft/commit.with.tariff.success.before.csv",
            after = "draft/commit.with.tariff.success.after.csv"
    )
    @DisplayName("Тест на коммит черновика, который создан на основе тарифа")
    void testDraftCommitSuccessBaseOnTariff() {
        ResponseEntity<String> response = post(baseUrl() + "/drafts/1/commit");
        assertOk(response);

        long tariffId = parseOneResult(response.getBody(), long.class);
        assertEquals(1L, tariffId);
    }

    @Test
    @DbUnitDataSet(
            before = "draft/commit.with.tariff.success.with.no.tariff.changes.before.csv",
            after = "draft/commit.with.tariff.success.with.no.tariff.changes.after.csv"
    )
    @DisplayName("Тест на коммит черновика, который создан на основе тарифа, но у старого тарифа дата не поменяется")
    void testDraftCommitWithNoChangePrefTariff() {
        ResponseEntity<String> response = post(baseUrl() + "/drafts/1/commit");
        assertOk(response);

        long tariffId = parseOneResult(response.getBody(), long.class);
        assertEquals(1L, tariffId);
    }

    @Test
    @DisplayName("Тест на коммит черновика, которого не существует")
    void testCommitNotExistingDraftFailed() {
        HttpClientErrorException exception = assertThrows(
                HttpClientErrorException.class,
                () -> post(baseUrl() + "/drafts/1/commit")
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        List<ErrorInfo> errors = getErrors(exception.getResponseBodyAsString());
        assertThat(errors, hasSize(1));
        assertThat(errors.get(0), allOf(
                hasCode("ENTITY_NOT_FOUND"),
                hasMessage("The draft with id 1 is not found")
        ));
    }

    @Test
    @DbUnitDataSet(
            before = "draft/commit.draft.check.before.csv",
            after = "draft/commit.draft.check.after.csv"
    )
    @DisplayName("Тест простую проверку чекера при коммите")
    void testDraftCheck() {
        HttpClientErrorException exception = assertThrows(
                HttpClientErrorException.class,
                () -> post(baseUrl() + "/drafts/1/commit")
        );
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());

        List<ErrorInfo> errors = getErrors(exception.getResponseBodyAsString());
        assertThat(errors, hasSize(1));
        assertThat(errors.get(0), hasCode("BAD_PARAM"));

    }

    @Test
    @DbUnitDataSet(
            before = "draft/commit.forbidden.before.csv"
    )
    @DisplayName("Тест на ошибку когда пользователь последним редактировавший драфт пытается его закоммитить")
    void testDraftCommitForbiddenForSameUser() {
        HttpClientErrorException exception = assertThrows(HttpClientErrorException.class,
                () -> post(baseUrl() + "/drafts/1/commit"));
        assertThat(exception.getStatusCode(), is(HttpStatus.FORBIDDEN));
    }

    @DisplayName("Тест на сортировку черновиков")
    @ParameterizedTest
    @ValueSource(strings = {"serviceType", "dateFrom", "dateTo", "tags", "partnerId", "updatedTime", "updatedBy", "id", "modelType", "approvalTicket"})
    void testSortBy(String sortBy) {
        HttpEntity<String> bodyEntity = createHttpRequestEntity(new DraftsFindQuery());
        ResponseEntity<String> response = post(baseUrl() + "/drafts/find?page=0&pageSize=100&sortBy=" + sortBy, bodyEntity);
        assertOk(response);
    }

    @Test
    @DbUnitDataSet(before = "draft/validate.draft.by.id.before.csv")
    void testValidateDraftByIdSuccess() {
        ResponseEntity<String> response = post(baseUrl() + "/drafts/1/validate");
        assertOk(response);
    }

    @Test
    void testValidateDraftByIdFailed() {
        HttpClientErrorException exception = assertThrows(
                HttpClientErrorException.class,
                () -> post(baseUrl() + "/drafts/123/validate")
        );
        assertThat(exception.getStatusCode(), is(HttpStatus.NOT_FOUND));
    }
}
