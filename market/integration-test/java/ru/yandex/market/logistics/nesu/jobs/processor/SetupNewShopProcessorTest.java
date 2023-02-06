package ru.yandex.market.logistics.nesu.jobs.processor;

import java.util.List;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.request.businessWarehouse.BusinessWarehouseValidationRequest;
import ru.yandex.market.logistics.management.entity.request.businessWarehouse.CreateBusinessWarehouseDto;
import ru.yandex.market.logistics.management.entity.request.businessWarehouse.UpdateBusinessWarehouseDto;
import ru.yandex.market.logistics.management.entity.request.partner.PlatformClientStatusDto;
import ru.yandex.market.logistics.management.entity.response.businessWarehouse.BusinessWarehouseResponse;
import ru.yandex.market.logistics.management.entity.response.businessWarehouse.BusinessWarehouseValidationStatus;
import ru.yandex.market.logistics.management.entity.response.core.Address;
import ru.yandex.market.logistics.management.entity.response.core.Phone;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerSettingDto;
import ru.yandex.market.logistics.management.entity.response.point.Contact;
import ru.yandex.market.logistics.management.entity.response.settings.methods.SettingsMethodCreateDto;
import ru.yandex.market.logistics.management.entity.type.PartnerStatus;
import ru.yandex.market.logistics.management.entity.type.PartnerType;
import ru.yandex.market.logistics.management.entity.type.PhoneType;
import ru.yandex.market.logistics.management.entity.type.StockSyncSwitchReason;
import ru.yandex.market.logistics.nesu.AbstractContextualTest;
import ru.yandex.market.logistics.nesu.exception.NesuException;
import ru.yandex.market.logistics.nesu.exception.http.BadRequestException;
import ru.yandex.market.logistics.nesu.exception.http.NesuValidationException;
import ru.yandex.market.logistics.nesu.exception.http.ResourceNotFoundException;
import ru.yandex.market.logistics.nesu.jobs.model.ShopIdPayload;
import ru.yandex.market.logistics.nesu.jobs.producer.PushFfLinkToMbiProducer;
import ru.yandex.market.logistics.nesu.jobs.producer.PushPartnerMappingToL4SProducer;
import ru.yandex.market.logistics.nesu.jobs.producer.SetupStockSyncStrategyProducer;
import ru.yandex.market.logistics.nesu.jobs.producer.UpdateBusinessWarehousePartnerApiMethodsProducer;
import ru.yandex.market.logistics.nesu.utils.BusinessWarehouseTestUtils;
import ru.yandex.market.logistics.nesu.utils.UuidGenerator;
import ru.yandex.market.logistics.util.client.exception.HttpTemplateException;
import ru.yandex.market.mbi.api.client.MbiApiClient;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static ru.yandex.market.logistics.nesu.model.LmsFactory.createSettingsApiDto;
import static ru.yandex.market.logistics.nesu.model.LmsFactory.createSettingsApiUpdateDto;
import static ru.yandex.market.logistics.nesu.model.LmsFactory.createSettingsMethodDtos;
import static ru.yandex.market.logistics.nesu.model.LmsFactory.createSettingsMethodsCreateDtos;
import static ru.yandex.market.logistics.nesu.utils.BusinessWarehouseTestUtils.validationRequest;

/**
 * Механизм взаимодействия с внешними системами:
 * <ol>
 *     <li>Ищем магазин, проверяем роль</li>
 *     <li>Идем в lms, создаем бизнес-склад или получаем существующий</li>
 *     <li>Устанавливаем созданному партнеру статус TESTING</li>
 *     <li>Создаем настройки Api в lms, если они не были заданы</li>
 *     <li>Создаем настройки партнёра в lms, если они не были заданы</li>
 *     <li>Получаем настройки синхронизации стоков из MBI</li>
 *     <li>Устанавливаем их в Stock Storage</li>
 *     <li>Добавляем связку магазина и партнера</li>
 * </ol>
 */
@DisplayName("Настройка нового магазина")
@ParametersAreNonnullByDefault
class SetupNewShopProcessorTest extends AbstractContextualTest {
    private static final long SHOP_ID = 1;
    private static final long BUSINESS_ID = 41;
    private static final long PARTNER_ID = 100500;
    private static final long WAREHOUSE_ID = 130;
    private static final String MOCK_UUID = "123";
    private static final String EXISTING_EXTERNAL_ID = "existing-ext-id";
    private static final int REGION_ID = 213;
    private static final String LOCALITY = "Москва";
    private static final String REGION = "Москва";

    private static final List<SettingsMethodCreateDto> METHODS_CREATE = createSettingsMethodsCreateDtos(List.of(
        Pair.of("getStocks", "reference/100500/getWhiteStocks"),
        Pair.of("getReferenceItems", "reference/100500/getReferenceItems")
    ));

    @Autowired
    private SetupNewShopProcessor setupNewShopProcessor;

    @Autowired
    private MbiApiClient mbiApiClient;

    @Autowired
    private LMSClient lmsClient;

    @Autowired
    private SetupStockSyncStrategyProducer setupStockSyncStrategyProducer;

    @Autowired
    private UuidGenerator uuidGenerator;

    @Autowired
    private PushFfLinkToMbiProducer pushFfLinkToMbiProducer;

    @Autowired
    private PushPartnerMappingToL4SProducer pushPartnerMappingToL4SProducer;

    @Autowired
    private UpdateBusinessWarehousePartnerApiMethodsProducer updateBusinessWarehousePartnerApiMethodsProducer;

    @BeforeEach
    void defaultMockSetup() {
        mockCreateApiMethods();
        mockCreatePartnerSettings();

        doReturn(MOCK_UUID)
            .when(uuidGenerator).randomUuid();
        doReturn(defaultResponse())
            .when(lmsClient).createBusinessWarehouse(any(CreateBusinessWarehouseDto.class));
        doNothing().when(pushFfLinkToMbiProducer).produceTask(anyLong(), anyLong());
        doNothing().when(pushPartnerMappingToL4SProducer).produceTask(anyLong(), anyLong());
        doNothing().when(setupStockSyncStrategyProducer).produceTask(anyLong(), anyLong());
        doNothing().when(updateBusinessWarehousePartnerApiMethodsProducer).produceTask(anyLong(), anyLong());
    }

    @AfterEach
    void checkMocks() {
        verifyNoMoreInteractions(
            mbiApiClient,
            lmsClient,
            setupStockSyncStrategyProducer,
            pushFfLinkToMbiProducer,
            pushPartnerMappingToL4SProducer,
            updateBusinessWarehousePartnerApiMethodsProducer,
            uuidGenerator
        );
    }

    @Test
    @DisplayName("Магазин не существует")
    void shopNotFound() {
        softly.assertThatThrownBy(this::processShop)
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Failed to find [SHOP] with ids [1]");
    }

    @Test
    @DisplayName("Магазин не должен настраиваться фоновым процессом")
    @DatabaseSetup("/jobs/processor/setup_new_shop/before/supplier_unsupported.xml")
    void shopRoleUnsupported() {
        softly.assertThatThrownBy(this::processShop)
            .isInstanceOf(NesuException.class)
            .hasMessage("Unsupported shop role. id: 1, role: SUPPLIER");
    }

    @Test
    @DisplayName("Корректный флоу настройки DSBS партнёра")
    @DatabaseSetup("/jobs/processor/setup_new_shop/before/dropship_by_seller_setup.xml")
    @ExpectedDatabase(
        value = "/jobs/processor/setup_new_shop/after/dropship_by_seller_setup.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void successDropshipBySeller() {
        processShop();

        verifyBusinessWarehouseCall(PartnerType.DROPSHIP_BY_SELLER);
        verifyEnrichPartnerSettings();
    }

    @Test
    @DisplayName("Корректный флоу настройки RETAIL партнёра")
    @DatabaseSetup("/jobs/processor/setup_new_shop/before/retail_setup.xml")
    @ExpectedDatabase(
        value = "/jobs/processor/setup_new_shop/after/retail_setup.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void successRetail() {
        processShop();

        verifyBusinessWarehouseCall(PartnerType.RETAIL);
        verifyAddMbiShopLinks();
    }

    @Test
    @DisplayName("Корректный флоу настройки DROPSHIP партнёра")
    @DatabaseSetup("/jobs/processor/setup_new_shop/before/dropship_setup.xml")
    @ExpectedDatabase(
        value = "/jobs/processor/setup_new_shop/after/dropship_setup.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void successDropship() {
        processShop();

        verifyBusinessWarehouseCall(PartnerType.DROPSHIP);
        verifyEnrichPartnerSettings();
    }

    @Test
    @DisplayName("У магазина указан externalId")
    @DatabaseSetup("/jobs/processor/setup_new_shop/before/dropship_by_seller_setup_with_external_id.xml")
    @ExpectedDatabase(
        value = "/jobs/processor/setup_new_shop/after/dropship_by_seller_setup.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void successWithExternalId() {
        doReturn(BusinessWarehouseValidationStatus.OK)
            .when(lmsClient).validateExternalIdInBusiness(expectedValdationRequest());

        processShop();

        verify(lmsClient).validateExternalIdInBusiness(expectedValdationRequest());
        verifyBusinessWarehouseCall(EXISTING_EXTERNAL_ID, PartnerType.DROPSHIP_BY_SELLER);
        verifyEnrichPartnerSettings();
    }

    @Test
    @DisplayName("Неправильные настройки магазина")
    @DatabaseSetup({
        "/jobs/processor/setup_new_shop/before/dropship_by_seller_setup.xml",
        "/jobs/processor/setup_new_shop/before/dropship_incorrect_shop_settings.xml"
    })
    void incorrectShopSettings() {
        softly.assertThatThrownBy(this::processShop)
            .isInstanceOf(BadRequestException.class)
            .hasMessage("Found more than 1 partner settings for shop 1 and partnerType DROPSHIP_BY_SELLER");
    }

    @Test
    @DisplayName("Создание магазина с невалидным externalId")
    @DatabaseSetup("/jobs/processor/setup_new_shop/before/dropship_by_seller_setup_with_external_id.xml")
    void invalidExternalId() {
        doReturn(BusinessWarehouseValidationStatus.INVALID)
            .when(lmsClient).validateExternalIdInBusiness(expectedValdationRequest());

        softly.assertThatThrownBy(this::processShop)
            .isInstanceOf(NesuValidationException.class)
            .hasMessage("Validation error");

        verify(lmsClient).validateExternalIdInBusiness(expectedValdationRequest());
    }

    @Test
    @DisplayName("Не найден регион для DBS магазина")
    @DatabaseSetup("/jobs/processor/setup_new_shop/before/dropship_by_seller_with_invalid_region_id.xml")
    void noLocationFoundForDBS() {
        processShop();
    }

    @Test
    @DisplayName("Не найден регион для RETAIL магазина")
    @DatabaseSetup("/jobs/processor/setup_new_shop/before/retail_with_invalid_region_id.xml")
    void noLocationFoundForRetail() {
        processShop();
    }

    @Test
    @DisplayName("Корректный флоу настройки DSBS партнёра, lms партнер уже настроен")
    @DatabaseSetup("/jobs/processor/setup_new_shop/before/dropship_by_seller_with_relation_setup.xml")
    @ExpectedDatabase(
        value = "/jobs/processor/setup_new_shop/after/dropship_by_seller_setup.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void successDropshipBySellerWithPartnerCreated() {
        doReturn(defaultResponse())
            .when(lmsClient).updateBusinessWarehouse(eq(PARTNER_ID), any(UpdateBusinessWarehouseDto.class));

        processShop();

        verify(lmsClient).updateBusinessWarehouse(eq(PARTNER_ID), any(UpdateBusinessWarehouseDto.class));
        verify(uuidGenerator).randomUuid();
        verifyEnrichPartnerSettings();
    }

    @Test
    @DisplayName("Нет идентификатора региона для DBS")
    @DatabaseSetup("/jobs/processor/setup_new_shop/before/dropship_by_seller_without_region_id.xml")
    void noRegionIdInDBSShop() {
        softly.assertThatThrownBy(this::processShop)
            .isInstanceOf(NesuException.class)
            .hasMessage("No regionId present for shop 1");
    }

    @Test
    @DisplayName("Нет идентификатора региона для RETAIL")
    @DatabaseSetup("/jobs/processor/setup_new_shop/before/retail_without_region_id.xml")
    void noRegionIdInRetailShop() {
        softly.assertThatThrownBy(this::processShop)
            .isInstanceOf(NesuException.class)
            .hasMessage("No regionId present for shop 1");
    }

    @Test
    @DisplayName("Связка с партнером есть, но в lms партнера нету")
    @DatabaseSetup("/jobs/processor/setup_new_shop/before/dropship_by_seller_with_relation_setup.xml")
    void dropshipBySellerNotFoundAtLms() {
        doThrow(new HttpTemplateException(404, "Error"))
            .when(lmsClient).updateBusinessWarehouse(eq(PARTNER_ID), any());

        softly.assertThatThrownBy(this::processShop)
            .isInstanceOf(HttpTemplateException.class)
            .hasMessageStartingWith("Http request exception: status <404>");

        verify(lmsClient).updateBusinessWarehouse(eq(PARTNER_ID), any(UpdateBusinessWarehouseDto.class));
        verify(uuidGenerator).randomUuid();
    }

    private void verifyEnrichPartnerSettings() {
        verifySetStockSyncStrategy();
        verifyAddMbiShopLinks();
        verifyUpdateApiMethods();
        verifyL4SPartnerMapping();
    }

    private void verifyBusinessWarehouseCall(PartnerType partnerType) {
        verifyBusinessWarehouseCall(MOCK_UUID, partnerType);

        verify(uuidGenerator).randomUuid();

    }

    private void verifyBusinessWarehouseCall(String externalId, PartnerType partnerType) {
        ArgumentCaptor<CreateBusinessWarehouseDto> businessWarehouseDtoCaptor = ArgumentCaptor.forClass(
            CreateBusinessWarehouseDto.class
        );
        verify(lmsClient).createBusinessWarehouse(businessWarehouseDtoCaptor.capture());
        softly.assertThat(businessWarehouseDtoCaptor.getValue())
            .usingRecursiveComparison()
            .isEqualTo(businessWarehouseDto(externalId, partnerType));
    }

    private void mockCreateApiMethods() {
        doReturn(createSettingsApiDto(PARTNER_ID))
            .when(lmsClient).createApiSettings(PARTNER_ID, createSettingsApiUpdateDto());
        doReturn(createSettingsMethodDtos(List.of("getStocks", "getReferenceItems")))
            .when(lmsClient).createPartnerApiMethods(PARTNER_ID, METHODS_CREATE);
    }

    private void mockCreatePartnerSettings() {
        doReturn(defaultPartnerSettingDto())
            .when(lmsClient).updatePartnerSettings(PARTNER_ID, defaultPartnerSettingDto());
    }

    private void verifySetStockSyncStrategy() {
        verify(setupStockSyncStrategyProducer).produceTask(PARTNER_ID, SHOP_ID);
    }

    private void verifyAddMbiShopLinks() {
        verify(pushFfLinkToMbiProducer).produceTask(PARTNER_ID, SHOP_ID);
    }

    private void verifyUpdateApiMethods() {
        verify(updateBusinessWarehousePartnerApiMethodsProducer).produceTask(PARTNER_ID, SHOP_ID);
    }

    private void verifyL4SPartnerMapping() {
        verify(pushPartnerMappingToL4SProducer).produceTask(PARTNER_ID, SHOP_ID);
    }

    private void processShop() {
        setupNewShopProcessor.processPayload(new ShopIdPayload(REQUEST_ID, SHOP_ID));
    }

    @Nonnull
    private static CreateBusinessWarehouseDto businessWarehouseDto(String externalId, PartnerType partnerType) {
        return CreateBusinessWarehouseDto.newBuilder()
            .partnerType(partnerType)
            .businessId(BUSINESS_ID)
            .name("test-shop-name")
            .readableName("test-shop-name")
            .externalId(externalId)
            .address(defaultAddress())
            .partnerSettingDto(BusinessWarehouseTestUtils.partnerSettingDto(partnerType, 213))
            .platformClients(Set.of(
                PlatformClientStatusDto.newBuilder()
                    .platformClientId(partnerType == PartnerType.DROPSHIP_BY_SELLER ? 5L : 1L)
                    .status(PartnerStatus.ACTIVE)
                    .build()
            ))
            .contact(contact(partnerType))
            .phones(phones(partnerType))
            .build();
    }

    @Nullable
    private static Set<Phone> phones(PartnerType partnerType) {
        if (partnerType != PartnerType.DROPSHIP) {
            return null;
        }
        return Set.of(Phone.newBuilder().number("8-800-555-3535").type(PhoneType.PRIMARY).build());
    }

    @Nullable
    private static Contact contact(PartnerType partnerType) {
        if (partnerType != PartnerType.DROPSHIP) {
            return null;
        }
        return new Contact("Энакин", "Скайуокер", null);
    }

    @Nonnull
    private static Address defaultAddress() {
        return Address.newBuilder()
            .locationId(REGION_ID)
            .settlement(LOCALITY)
            .region(REGION)
            .build();
    }

    @Nonnull
    private static BusinessWarehouseResponse defaultResponse() {
        return BusinessWarehouseResponse.newBuilder()
            .partnerId(PARTNER_ID)
            .logisticsPointId(WAREHOUSE_ID)
            .externalId(MOCK_UUID)
            .name("test-shop-name")
            .readableName("test-shop-name")
            .businessId(41L)
            .marketId(132L)
            .address(defaultAddress())
            .build();
    }

    @Nonnull
    private static PartnerSettingDto defaultPartnerSettingDto() {
        return PartnerSettingDto.newBuilder()
            .locationId(REGION_ID)
            .stockSyncSwitchReason(StockSyncSwitchReason.NEW)
            .korobyteSyncEnabled(false)
            .autoSwitchStockSyncEnabled(true)
            .stockSyncEnabled(false)
            .build();
    }

    @Nonnull
    private BusinessWarehouseValidationRequest expectedValdationRequest() {
        return validationRequest(BUSINESS_ID, EXISTING_EXTERNAL_ID, null);
    }
}
