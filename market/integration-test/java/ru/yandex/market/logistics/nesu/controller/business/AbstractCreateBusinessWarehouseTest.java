package ru.yandex.market.logistics.nesu.controller.business;

import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.fulfillment.stockstorage.client.StockStorageOrderClient;
import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.request.businessWarehouse.BusinessWarehouseValidationRequest;
import ru.yandex.market.logistics.management.entity.request.businessWarehouse.CreateBusinessWarehouseDto;
import ru.yandex.market.logistics.management.entity.response.businessWarehouse.BusinessWarehouseValidationStatus;
import ru.yandex.market.logistics.management.entity.type.PartnerType;
import ru.yandex.market.logistics.nesu.configuration.properties.FeatureProperties;
import ru.yandex.market.logistics.nesu.dto.business.BusinessWarehouseRequest;
import ru.yandex.market.logistics.nesu.jobs.producer.PushFfLinkToMbiProducer;
import ru.yandex.market.logistics.nesu.jobs.producer.PushPartnerMappingToL4SProducer;
import ru.yandex.market.logistics.nesu.jobs.producer.SetupStockSyncStrategyProducer;
import ru.yandex.market.logistics.nesu.jobs.producer.UpdateBusinessWarehousePartnerApiMethodsProducer;
import ru.yandex.market.logistics.nesu.utils.ValidationErrorData;
import ru.yandex.market.mbi.api.client.MbiApiClient;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static ru.yandex.market.logistics.nesu.model.LmsFactory.createSettingsApiDto;
import static ru.yandex.market.logistics.nesu.model.ModelFactory.createSchedule;
import static ru.yandex.market.logistics.nesu.utils.BusinessWarehouseTestUtils.BUSINESS_ID;
import static ru.yandex.market.logistics.nesu.utils.BusinessWarehouseTestUtils.WHITESPACES_AND_RUSSIAN_LETTERS;
import static ru.yandex.market.logistics.nesu.utils.BusinessWarehouseTestUtils.dropshipBusinessWarehouseRequest;
import static ru.yandex.market.logistics.nesu.utils.BusinessWarehouseTestUtils.dropshipCreateWarehouseDtoBuilder;
import static ru.yandex.market.logistics.nesu.utils.BusinessWarehouseTestUtils.dropshipCreateWarehouseDtoToLms;
import static ru.yandex.market.logistics.nesu.utils.BusinessWarehouseTestUtils.dropshipResponse;
import static ru.yandex.market.logistics.nesu.utils.BusinessWarehouseTestUtils.externalIdValidationErrorData;
import static ru.yandex.market.logistics.nesu.utils.BusinessWarehouseTestUtils.minimalValidBusinessWarehouseRequest;
import static ru.yandex.market.logistics.nesu.utils.BusinessWarehouseTestUtils.partnerSettingDtoBuilder;
import static ru.yandex.market.logistics.nesu.utils.BusinessWarehouseTestUtils.supplierBusinessWarehouseRequest;
import static ru.yandex.market.logistics.nesu.utils.BusinessWarehouseTestUtils.supplierCreateWarehouseDtoToLms;
import static ru.yandex.market.logistics.nesu.utils.BusinessWarehouseTestUtils.supplierResponse;
import static ru.yandex.market.logistics.nesu.utils.BusinessWarehouseTestUtils.validationRequest;
import static ru.yandex.market.logistics.nesu.utils.MatcherUtils.validationErrorMatcher;
import static ru.yandex.market.logistics.nesu.utils.PartnerTestUtils.DROPSHIP_METHODS;
import static ru.yandex.market.logistics.nesu.utils.PartnerTestUtils.DROPSHIP_PARTNER_ID;
import static ru.yandex.market.logistics.nesu.utils.PartnerTestUtils.DROPSHIP_SHOP_ID;
import static ru.yandex.market.logistics.nesu.utils.PartnerTestUtils.SUPPLIER_BUSINESS_ID;
import static ru.yandex.market.logistics.nesu.utils.PartnerTestUtils.SUPPLIER_PARTNER_ID;
import static ru.yandex.market.logistics.nesu.utils.PartnerTestUtils.SUPPLIER_SHOP_ID;
import static ru.yandex.market.logistics.nesu.utils.PartnerTestUtils.createDropshipMethodsExcept;
import static ru.yandex.market.logistics.nesu.utils.ValidationErrorData.fieldError;
import static ru.yandex.market.logistics.nesu.utils.ValidationErrorData.fieldErrorBuilder;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.errorMessage;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.noContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@ParametersAreNonnullByDefault
abstract class AbstractCreateBusinessWarehouseTest extends AbstractModifyingBusinessWarehouseTest {
    @Autowired
    protected LMSClient lmsClient;

    @Autowired
    protected MbiApiClient mbiApiClient;

    @Autowired
    private StockStorageOrderClient stockStorageOrderClient;

    @Autowired
    protected SetupStockSyncStrategyProducer setupStockSyncStrategyProducer;

    @Autowired
    protected PushFfLinkToMbiProducer pushFFLinkToMbiProducer;

    @Autowired
    protected UpdateBusinessWarehousePartnerApiMethodsProducer updateBusinessWarehousePartnerApiMethodsProducer;

    @Autowired
    protected PushPartnerMappingToL4SProducer pushPartnerMappingToL4SProducer;

    @Autowired
    private FeatureProperties featureProperties;

    @BeforeEach
    void setup() {
        doReturn(dropshipResponse())
            .when(lmsClient).createBusinessWarehouse(dropshipCreateWarehouseDtoToLms());
        doReturn(dropshipResponse())
            .when(lmsClient).createBusinessWarehouse(
                dropshipCreateWarehouseDtoBuilder()
                    .partnerSettingDto(
                        partnerSettingDtoBuilder(PartnerType.DROPSHIP, 2)
                            .canSellMedicine(true)
                            .canDeliverMedicine(false)
                            .build()
                    )
                    .build()
            );
        doReturn(dropshipResponse())
            .when(lmsClient).createBusinessWarehouse(
                dropshipCreateWarehouseDtoBuilder()
                    .partnerSettingDto(
                        partnerSettingDtoBuilder(PartnerType.DROPSHIP, 2)
                            .canSellMedicine(false)
                            .canDeliverMedicine(true)
                            .build()
                    )
                    .build()
            );
        doNothing()
            .when(setupStockSyncStrategyProducer).produceTask(anyLong(), anyLong());
        doNothing()
            .when(pushFFLinkToMbiProducer).produceTask(anyLong(), anyLong());
        doNothing()
            .when(pushPartnerMappingToL4SProducer).produceTask(anyLong(), anyLong());
        doNothing()
            .when(updateBusinessWarehousePartnerApiMethodsProducer).produceTask(anyLong(), anyLong());
        doReturn(BusinessWarehouseValidationStatus.OK)
            .when(lmsClient).validateExternalIdInBusiness(validationRequest());
    }

    @AfterEach
    void tearDown() {
        featureProperties.setNullableBusinessWarehouseAddress(true);

        verifyNoMoreInteractions(
            lmsClient,
            mbiApiClient,
            stockStorageOrderClient,
            setupStockSyncStrategyProducer,
            pushFFLinkToMbiProducer,
            pushPartnerMappingToL4SProducer
        );
    }

    @Test
    @SneakyThrows
    @DisplayName("Магазин не существует")
    void notFoundMissingMarketIdShopIdLink() {
        createBusinessWarehouse("-1", minimalValidBusinessWarehouseRequest())
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Failed to find [SHOP] with ids [-1]"));
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource({"bodyValidationSourceBase", "bodyValidationSource"})
    @DisplayName("Валидация тела запроса")
    void validation(
        ValidationErrorData.ValidationErrorDataBuilder validationError,
        Consumer<BusinessWarehouseRequest> requestConsumer
    ) throws Exception {
        BusinessWarehouseRequest request = minimalValidBusinessWarehouseRequest();
        requestConsumer.accept(request);
        createBusinessWarehouse("1", request)
            .andExpect(status().isBadRequest())
            .andExpect(validationErrorMatcher(validationError.forObject("businessWarehouseRequest")));
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource({"bodyValidationSourceBase", "bodyValidationSource"})
    @DisplayName("Валидация тела запроса для DSBS")
    void validationDSBS(
        ValidationErrorData.ValidationErrorDataBuilder validationError,
        Consumer<BusinessWarehouseRequest> requestConsumer
    ) throws Exception {
        BusinessWarehouseRequest request = minimalValidBusinessWarehouseRequest();
        requestConsumer.accept(request);
        createBusinessWarehouse(DROPSHIP_SHOP_ID, request)
            .andExpect(status().isBadRequest())
            .andExpect(validationErrorMatcher(validationError.forObject("businessWarehouseRequest")));
    }

    @SneakyThrows
    @MethodSource
    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @DisplayName("Валидация внешнего идентификатора: валидные случаи")
    void externalIdValidationValidCases(
        String displayName,
        BusinessWarehouseRequest request
    ) {
        doThrow(new RuntimeException())
            .when(lmsClient).createBusinessWarehouse(any());

        createBusinessWarehouse(DROPSHIP_SHOP_ID, request)
            .andExpect(status().is5xxServerError());

        //для случая с проверкой валидных символов
        if (StringUtils.isNotBlank(request.getExternalId())) {
            verify(lmsClient).validateExternalIdInBusiness(
                validationRequest(BUSINESS_ID, request.getExternalId(), null)
            );
        }
        verify(lmsClient).createBusinessWarehouse(any());
    }

    @Nonnull
    private static Stream<Arguments> externalIdValidationValidCases() {
        return Stream.of(
            Arguments.of(
                "Пустая строка в качестве внешнего идентификатора",
                minimalValidBusinessWarehouseRequest().setExternalId("")
            ),
            Arguments.of(
                "Латинские буквы, цифры и символы /\\-_",
                minimalValidBusinessWarehouseRequest().setExternalId("aB123/\\-_")
            ),
            Arguments.of(
                "Строка, состоящая из пробелов",
                minimalValidBusinessWarehouseRequest().setExternalId("    ")
            ),
            Arguments.of(
                "Не указан внешний идентификатор",
                minimalValidBusinessWarehouseRequest().setExternalId(null)
            ),
            Arguments.of(
                "Строка с пробелами и кириллицей",
                minimalValidBusinessWarehouseRequest().setExternalId(WHITESPACES_AND_RUSSIAN_LETTERS)
            )
        );
    }

    @Nonnull
    private static Stream<Arguments> bodyValidationSource() {
        return Stream.<Pair<ValidationErrorData.ValidationErrorDataBuilder, Consumer<BusinessWarehouseRequest>>>of(
            Pair.of(
                fieldErrorBuilder("address", ValidationErrorData.ErrorType.NOT_NULL),
                rq -> rq.setAddress(null)
            ),
            Pair.of(
                fieldErrorBuilder("name", ValidationErrorData.ErrorType.NOT_BLANK),
                rq -> rq.setName(null)
            ),
            Pair.of(
                fieldErrorBuilder("name", ValidationErrorData.ErrorType.NOT_BLANK),
                rq -> rq.setName(" \t\n ")
            )
        )
            .map(p -> Arguments.of(p.getLeft(), p.getRight()));
    }

    @Test
    @SneakyThrows
    @DisplayName("Некорректная роль магазина")
    void wrongShopType() {
        createBusinessWarehouse("2", minimalValidBusinessWarehouseRequest())
            .andExpect(status().isBadRequest())
            .andExpect(errorMessage(
                "Shop must have role [DROPSHIP, SUPPLIER, DROPSHIP_BY_SELLER, RETAIL] for this operation"
            ));
    }

    @Test
    @DisplayName("Отключенный магазин")
    void disabledShop() throws Exception {
        createBusinessWarehouse("5", minimalValidBusinessWarehouseRequest())
            .andExpect(status().isForbidden())
            .andExpect(noContent());
    }

    @Test
    @SneakyThrows
    @DisplayName("Ошибка от лмс при создании бизнес склада")
    void invalidPartnerType() {
        doThrow(new RuntimeException())
            .when(lmsClient).createBusinessWarehouse(any(CreateBusinessWarehouseDto.class));

        createBusinessWarehouse(DROPSHIP_SHOP_ID, dropshipBusinessWarehouseRequest())
            .andExpect(status().is5xxServerError());

        verifyDropshipWarehouseCreated();
    }

    @Test
    @DisplayName("Склад и настройки создаются")
    @ExpectedDatabase(
        value = "/controller/business/create-unique/after/dropship_setting_exists.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void newWarehouseCreated() throws Exception {
        createDropshipBusinessWarehouse()
            .andExpect(status().isOk());
        verifyTasksCreated(1L, 1L);
        verifyDropshipWarehouseCreated();
    }

    @Test
    @DisplayName("Успешное создание кроссдок партнера")
    void supplierCreatedWithDefaultStrategy() throws Exception {
        doReturn(supplierResponse())
            .when(lmsClient).createBusinessWarehouse(supplierCreateWarehouseDtoToLms());
        doReturn(BusinessWarehouseValidationStatus.OK)
            .when(lmsClient).validateExternalIdInBusiness(validationRequest(SUPPLIER_BUSINESS_ID));

        createBusinessWarehouse("3", supplierBusinessWarehouseRequest())
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonContent("controller/business/response/supplier_partner_create_response.json"));

        verify(lmsClient).createBusinessWarehouse(supplierCreateWarehouseDtoToLms());
        verify(lmsClient).validateExternalIdInBusiness(validationRequest(SUPPLIER_BUSINESS_ID));
        verifyTasksCreated(SUPPLIER_PARTNER_ID, SUPPLIER_SHOP_ID);
    }

    @SneakyThrows
    @ParameterizedTest
    @ValueSource(strings = {"1", "3"})
    @DisplayName("Попытка создать склад для CPA магазина с некорректным расписанием")
    void cpaIncorrectSchedule(String shopId) {
        BusinessWarehouseRequest dropship = dropshipBusinessWarehouseRequest()
            .setSchedule(createSchedule(1))
            .setExternalId(null);
        createBusinessWarehouse(shopId, dropship)
            .andExpect(status().isBadRequest())
            .andExpect(validationErrorMatcher(fieldError(
                "schedule",
                "Schedule days count must be greater than or equal to 5",
                "businessWarehouseRequest",
                "ValidScheduleDaysCount",
                Map.of("value", 5)
            )));
    }

    @SneakyThrows
    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource
    @DisplayName("Создание склада дропшипа с расписанием null успешно")
    void dropshipNullScheduleIsCorrect(
        @SuppressWarnings("unused") String displayName,
        boolean nullableBusinessWarehouseAddress,
        String resultPath
    ) {
        featureProperties.setNullableBusinessWarehouseAddress(nullableBusinessWarehouseAddress);

        CreateBusinessWarehouseDto lmsRequestDto = dropshipCreateWarehouseDtoBuilder().schedule(null).build();

        doReturn(dropshipResponse())
            .when(lmsClient).createBusinessWarehouse(lmsRequestDto);
        doReturn(BusinessWarehouseValidationStatus.OK)
            .when(lmsClient).validateExternalIdInBusiness(any(BusinessWarehouseValidationRequest.class));

        BusinessWarehouseRequest dropship = dropshipBusinessWarehouseRequest()
            .setSchedule(null);
        createBusinessWarehouse(DROPSHIP_SHOP_ID, dropship)
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonContent(resultPath));

        verify(lmsClient).createBusinessWarehouse(lmsRequestDto);
        verify(lmsClient).validateExternalIdInBusiness(validationRequest(BUSINESS_ID));
        verifyTasksCreated(DROPSHIP_PARTNER_ID, 1L);
    }

    @Nonnull
    private static Stream<Arguments> dropshipNullScheduleIsCorrect() {
        return Stream.of(
            Arguments.of(
                "В ответе расписание null",
                true,
                "controller/business/response/dropship_partner_create_response.json"
            ),
            Arguments.of(
                "В ответе пустое расписание",
                false,
                "controller/business/response/dropship_partner_create_response_empty_schedule.json"
            )
        );
    }

    @Test
    @SneakyThrows
    @DisplayName("Попытка создать дропшип партнёра с ошибкой валидации externalId")
    void createWithInvalidExternalId() {
        doReturn(BusinessWarehouseValidationStatus.INVALID)
            .when(lmsClient).validateExternalIdInBusiness(validationRequest());

        createDropshipBusinessWarehouse()
            .andExpect(status().isBadRequest())
            .andExpect(validationErrorMatcher(externalIdValidationErrorData()));

        verifyValidation();
    }

    @Test
    @DisplayName("Проставляется настройка canSellMedicine при наличии лицензии в базе")
    @DatabaseSetup("/controller/business/before/has_can_sell_license.xml")
    void canSellMedicineSetting() throws Exception {
        createDropshipBusinessWarehouse()
            .andExpect(status().isOk());
        verifyTasksCreated(1L, 1L);
        verifyDropshipWarehouseCreatedWithLicesnes(true, false);
    }

    @Test
    @DisplayName("Проставляется настройка canDeliverMedicine при наличии лицензии в базе")
    @DatabaseSetup("/controller/business/before/has_can_deliver_license.xml")
    void canDeliverMedicineSetting() throws Exception {
        createDropshipBusinessWarehouse()
            .andExpect(status().isOk());
        verifyTasksCreated(1L, 1L);
        verifyDropshipWarehouseCreatedWithLicesnes(false, true);
    }

    @Nonnull
    private static Stream<Arguments> missingMethodSource() {
        return DROPSHIP_METHODS.stream()
            .map(method -> Arguments.of(method.getKey(), method.getValue()));
    }

    protected void verifyDropshipWarehouseCreated() {
        verifyWarehouseCreated(dropshipCreateWarehouseDtoToLms());
    }

    private void verifyWarehouseCreated(CreateBusinessWarehouseDto createBusinessWarehouseDto) {
        verify(lmsClient).createBusinessWarehouse(createBusinessWarehouseDto);
        verifyValidation();
    }

    protected void verifyValidation() {
        verify(lmsClient).validateExternalIdInBusiness(validationRequest());
    }

    protected ResultActions createDropshipBusinessWarehouse() throws Exception {
        return createBusinessWarehouse(DROPSHIP_SHOP_ID, dropshipBusinessWarehouseRequest());
    }


    protected void mockDropshipPartnerApiSettings() {
        doReturn(createSettingsApiDto(DROPSHIP_PARTNER_ID))
            .when(lmsClient).getPartnerApiSettings(DROPSHIP_PARTNER_ID);
        doReturn(createDropshipMethodsExcept())
            .when(lmsClient).getPartnerApiSettingsMethods(DROPSHIP_PARTNER_ID);
    }

    protected void verifyDropshipTasksCreated() {
        verifyTasksCreated(DROPSHIP_PARTNER_ID, Long.parseLong(DROPSHIP_SHOP_ID));
    }

    private void verifyTasksCreated(long partnerId, long shopId) {
        verify(setupStockSyncStrategyProducer).produceTask(partnerId, shopId);
        verify(pushFFLinkToMbiProducer).produceTask(partnerId, shopId);
        verify(updateBusinessWarehousePartnerApiMethodsProducer).produceTask(partnerId, shopId);
        verify(pushPartnerMappingToL4SProducer).produceTask(partnerId, shopId);
    }

    protected void verifyDropshipWarehouseCreatedWithLicesnes(Boolean canSellMedicine, Boolean canDeliverMedicine) {
        verifyWarehouseCreated(
            dropshipCreateWarehouseDtoBuilder()
                .partnerSettingDto(
                    partnerSettingDtoBuilder(PartnerType.DROPSHIP, 2)
                        .canSellMedicine(canSellMedicine)
                        .canDeliverMedicine(canDeliverMedicine)
                        .build()
                )
                .build()
        );
    }

    @Nonnull
    abstract ResultActions createBusinessWarehouse(String shopId, BusinessWarehouseRequest dto) throws Exception;
}
