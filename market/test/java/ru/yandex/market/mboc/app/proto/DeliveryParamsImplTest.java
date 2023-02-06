package ru.yandex.market.mboc.app.proto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import io.github.benas.randombeans.api.EnhancedRandom;
import org.assertj.core.api.Assertions;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.deepmind.openapi.client.ApiException;
import ru.yandex.market.deepmind.openapi.client.api.AvailabilitiesApiMock;
import ru.yandex.market.deepmind.openapi.client.api.SskuStatusApiMock;
import ru.yandex.market.deepmind.openapi.client.model.BlockInfo;
import ru.yandex.market.deepmind.openapi.client.model.Message;
import ru.yandex.market.deepmind.openapi.client.model.UpdateSskuStatusRequest;
import ru.yandex.market.http.BadRequestException;
import ru.yandex.market.mbo.export.MboParameters;
import ru.yandex.market.mbo.http.ModelStorage;
import ru.yandex.market.mbo.mdm.common.masterdata.services.SupplierConverterService;
import ru.yandex.market.mbo.storage.StorageKeyValueService;
import ru.yandex.market.mboc.common.MbocErrors;
import ru.yandex.market.mboc.common.availability.msku.MskuRepository;
import ru.yandex.market.mboc.common.availability.utils.AvailabilityProtoConverter;
import ru.yandex.market.mboc.common.db.jooq.generated.mbo_category.tables.pojos.CargoTypeSnapshot;
import ru.yandex.market.mboc.common.db.jooq.generated.mbo_category.tables.pojos.Msku;
import ru.yandex.market.mboc.common.dict.MbocSupplierType;
import ru.yandex.market.mboc.common.dict.SupplierRepository;
import ru.yandex.market.mboc.common.dict.WarehouseService;
import ru.yandex.market.mboc.common.dict.WarehouseServiceAuditRecorder;
import ru.yandex.market.mboc.common.dict.WarehouseServiceRepository;
import ru.yandex.market.mboc.common.dict.WarehouseServiceService;
import ru.yandex.market.mboc.common.logisticsparams.warehouse.WarehouseRepository;
import ru.yandex.market.mboc.common.masterdata.KnownMdmMboParams;
import ru.yandex.market.mboc.common.masterdata.TestDataUtils;
import ru.yandex.market.mboc.common.masterdata.model.MasterData;
import ru.yandex.market.mboc.common.msku.TestUtils;
import ru.yandex.market.mboc.common.offers.model.Offer;
import ru.yandex.market.mboc.common.offers.model.OfferContent;
import ru.yandex.market.mboc.common.offers.model.ShopSkuKey;
import ru.yandex.market.mboc.common.offers.repository.OfferRepository;
import ru.yandex.market.mboc.common.services.business.BusinessSupplierService;
import ru.yandex.market.mboc.common.services.cargo_type.CargoTypeCachingServiceMock;
import ru.yandex.market.mboc.common.services.modelstorage.ModelStorageCachingServiceMock;
import ru.yandex.market.mboc.common.services.modelstorage.models.Model;
import ru.yandex.market.mboc.common.services.proto.MasterDataHelperService;
import ru.yandex.market.mboc.common.services.proto.MboMappingsHelperService;
import ru.yandex.market.mboc.common.services.storage.StorageKeyValueServiceMock;
import ru.yandex.market.mboc.common.test.YamlTestUtil;
import ru.yandex.market.mboc.common.utils.BaseDbTestClass;
import ru.yandex.market.mboc.common.utils.OfferTestUtils;
import ru.yandex.market.mboc.http.MboMappingsForDelivery;
import ru.yandex.market.mboc.http.MboMappingsForDelivery.OfferFulfilmentInfo;
import ru.yandex.market.mboc.http.MboMappingsForDelivery.SearchFulfillmentSskuParamsForIntervalRequest;
import ru.yandex.market.mboc.http.MboMappingsForDelivery.SearchFulfilmentSskuParamsRequest;
import ru.yandex.market.mboc.http.MboMappingsForDelivery.SearchFulfilmentSskuParamsResponse;
import ru.yandex.market.mboc.http.MboMappingsForDelivery.WarehouseAvailability;
import ru.yandex.market.mboc.http.SupplierOffer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static ru.yandex.market.deepmind.openapi.client.model.SskuStatus.ACTIVE;
import static ru.yandex.market.deepmind.openapi.client.model.SskuStatus.DELISTED;
import static ru.yandex.market.deepmind.openapi.client.model.SskuStatus.INACTIVE_TMP;
import static ru.yandex.market.deepmind.openapi.client.model.SskuStatus.PENDING;
import static ru.yandex.market.mboc.common.logisticsparams.warehouse.WarehouseRepository.TOMILINO_ID;

public class DeliveryParamsImplTest extends BaseDbTestClass {
    private static final long SEED = 9;
    private static final int BERU_ID = 465852;

    private EnhancedRandom enhancedRandom;

    @Autowired
    private OfferRepository offerRepository;
    @Autowired
    private SupplierRepository supplierRepository;
    @Autowired
    private MskuRepository mskuRepository;
    @Autowired
    private SupplierConverterService supplierConverterService;
    @Autowired
    private WarehouseServiceRepository warehouseServiceRepository;

    private DeliveryParamsImpl service;

    private ModelStorageCachingServiceMock modelStorageCachingService;
    private StorageKeyValueService storageKeyValueService;
    private WarehouseServiceService warehouseServiceService;
    private SskuStatusApiMock sskuStatusApi;
    private AvailabilitiesApiMock availabilitiesApi;

    @Before
    public void setup() {
        supplierConverterService.clearCache();
        enhancedRandom = TestDataUtils.defaultRandom(SEED);

        storageKeyValueService = new StorageKeyValueServiceMock();
        supplierRepository.insertBatch(YamlTestUtil.readSuppliersFromResource("delivery_params/suppliers.yml"));
        offerRepository.insertOffers(YamlTestUtil.readOffersFromResources("delivery_params/offers.yml"));

        mskuRepository.save(List.of(
            TestUtils.newMsku(100000, 22L),
            TestUtils.newMsku(404040L, 33L).setCargoTypeLmsIds(1L, 2L, 3L),
            TestUtils.newMsku(303030L, 22L),
            TestUtils.newMsku(505050L, 22L)
        ));

        CargoTypeCachingServiceMock cargoTypeCachingServiceMock = new CargoTypeCachingServiceMock();
        cargoTypeCachingServiceMock.put(
            new CargoTypeSnapshot(1L, "cargo10", 10L),
            new CargoTypeSnapshot(2L, "cargo20", 20L),
            new CargoTypeSnapshot(3L, "cargo30", 30L)
        );

        modelStorageCachingService = Mockito.spy(new ModelStorageCachingServiceMock());
        Mockito.doThrow(new RuntimeException("mbo is down"))
            .when(modelStorageCachingService).getModelsFromMboOnly(Mockito.anyCollection());
        modelStorageCachingService
            .addModel(createTestModel(10L, 10L))
            .addModel(createTestModel(20L, 20L))
            .addModel(createTestModel(404040L, 33L))
            .addModel(createTestModel(303030L, 22L))
            .addModel(createTestModel(505050L, 22L));

        var masterDataServiceMock = new MasterDataServiceMock();
        var supplierDocumentServiceMock = new SupplierDocumentServiceMock(masterDataServiceMock);
        MasterDataHelperService masterDataHelperService = new MasterDataHelperService(masterDataServiceMock,
            supplierDocumentServiceMock,
            supplierConverterService, storageKeyValueService);
        masterDataHelperService.saveSskuMasterDataAndDocuments(List.of(
            createMasterData(60, "sku4", 4, "cccode4"),
            createMasterData(77, "sku5", 5, "cccode5")
        ));

        BusinessSupplierService businessSupplierService = new BusinessSupplierService(supplierRepository,
            offerRepository);
        MboMappingsHelperService mboMappingsHelperService = new MboMappingsHelperService(
            supplierRepository, masterDataHelperService, businessSupplierService, BERU_ID);
        warehouseServiceService = Mockito.spy(new WarehouseServiceService(
            warehouseServiceRepository,
            offerRepository,
            supplierRepository,
            mskuRepository,
            Mockito.mock(WarehouseServiceAuditRecorder.class)
        ));
        sskuStatusApi = new SskuStatusApiMock();
        availabilitiesApi = new AvailabilitiesApiMock();
        service = new DeliveryParamsImpl(
            modelStorageCachingService,
            mboMappingsHelperService,
            masterDataHelperService,
            cargoTypeCachingServiceMock,
            mskuRepository,
            supplierRepository,
            offerRepository,
            storageKeyValueService,
            warehouseServiceService,
            sskuStatusApi,
            availabilitiesApi
        );
        storageKeyValueService.putValue("search_fulfilment_ssku_param_to_mboc_flag", true);
    }

    @Test
    public void searchShouldReturnWarehouseAvailabilityWithoutDate() {
        SearchFulfilmentSskuParamsResponse response = service
            .searchFulfilmentSskuParams(SearchFulfilmentSskuParamsRequest.newBuilder()
                .addKeys(protoKey(60, "sku4"))
                .addWarehouseId(TOMILINO_ID)
                .build());

        OfferFulfilmentInfo fulfilmentInfo = response.getFulfilmentInfo(0);
        Assertions.assertThat(fulfilmentInfo.getShopSku()).isEqualTo("sku4");
        Assertions.assertThat(fulfilmentInfo.getAllowInbound()).isTrue();
        Assertions.assertThat(fulfilmentInfo.getAllowCargoType()).isTrue();

        WarehouseAvailability warehouseAvailabilities = fulfilmentInfo.getWarehouseAvailabilities(0);
        Assertions.assertThat(warehouseAvailabilities.getAllowInbound()).isTrue();
        Assertions.assertThat(warehouseAvailabilities.getAllowCargoType()).isTrue();
    }

    @Test
    public void searchByInterval() {
        // act
        SearchFulfilmentSskuParamsResponse response =
            service.searchFulfillmentSskuParamsForInterval(SearchFulfillmentSskuParamsForIntervalRequest.newBuilder()
                .addKeys(protoKey(BERU_ID, "000042.sku5"))
                .addWarehouseId(TOMILINO_ID)
                .setInboundDateFrom("2020-01-02").setInboundDateTo("2020-01-03")
                .build());

        // assert
        assertThat(response.getFulfilmentInfoList()).hasSize(1);
        OfferFulfilmentInfo fulfilmentInfo = response.getFulfilmentInfo(0);
        assertThat(fulfilmentInfo.getShopSku()).isEqualTo("000042.sku5");
        assertThat(fulfilmentInfo.getAllowInbound()).isTrue();
        assertThat(fulfilmentInfo.getAllowCargoType()).isTrue();
        assertThat(fulfilmentInfo.getWarehouseIntervalAvailabilitiesList()).isEmpty();
    }

    @Test
    public void searchByKeysShouldNotFindPassBeruWithIncorrectShopSku() {
        SearchFulfilmentSskuParamsResponse response =
            service.searchFulfilmentSskuParams(SearchFulfilmentSskuParamsRequest.newBuilder()
                .addKeys(protoKey(BERU_ID, "no dot symbol!"))
                .addKeys(protoKey(BERU_ID, "000042.sku5"))
                .addWarehouseId(TOMILINO_ID)
                .setInboundDate("2019-07-07")
                .build());

        assertThat(response.getFulfilmentInfoList())
            .extracting(OfferFulfilmentInfo::getShopSku)
            .containsExactlyInAnyOrder("000042.sku5");
    }

    @Test
    public void searchByKeysShouldFailIfMboIsUnreachable() {
        Assertions.assertThatThrownBy(() -> {
                service.searchFulfilmentSskuParams(SearchFulfilmentSskuParamsRequest.newBuilder()
                    .addKeys(protoKey(61, "sku42-42"))
                    .addKeys(protoKey(BERU_ID, "000042.sku5"))
                    .addKeys(protoKey(77, "sku6"))
                    .build());
            })
            .hasMessage("Can't fetch models from mbo or get from stuff: shop_skus/msku: " +
                "[sku6]/100000 ");
    }

    @Test
    public void searchByKeyWillBeCorrectIf1POfferIsPassed() {
        // arrange
        var key = new ShopSkuKey(BERU_ID, "000042.sku5");
        var blockInfo = blockInfo("code");
        availabilitiesApi.putAvailability(key, TOMILINO_ID, blockInfo);

        // act
        SearchFulfilmentSskuParamsResponse response =
            service.searchFulfilmentSskuParams(SearchFulfilmentSskuParamsRequest.newBuilder()
                .addKeys(protoKey(BERU_ID, "000042.sku5"))
                .addWarehouseId(TOMILINO_ID)
                .build());

        // assert
        assertThat(response.getFulfilmentInfoList()).hasSize(1);
        OfferFulfilmentInfo fulfilmentInfo = response.getFulfilmentInfo(0);
        assertThat(fulfilmentInfo.getShopSku()).isEqualTo("000042.sku5");
        assertThat(fulfilmentInfo.getWarehouseAvailabilitiesList()).containsExactly(
            AvailabilityProtoConverter.convert(TOMILINO_ID, blockInfo)
        );
    }

    @Test
    public void searchByKeyWillBeCorrectIf1POfferIsPassedInInterval() {
        // arrange
        var key = new ShopSkuKey(BERU_ID, "000042.sku5");
        var blockInfo = blockInfo("delisted-code");
        availabilitiesApi.putAvailability(key, TOMILINO_ID, blockInfo);

        // act
        SearchFulfilmentSskuParamsResponse response =
            service.searchFulfillmentSskuParamsForInterval(SearchFulfillmentSskuParamsForIntervalRequest.newBuilder()
                .addKeys(protoKey(BERU_ID, "000042.sku5"))
                .addWarehouseId(TOMILINO_ID)
                .setInboundDateFrom("2020-01-02").setInboundDateTo("2020-01-03")
                .build());

        // assert
        assertThat(response.getFulfilmentInfoList()).hasSize(1);
        OfferFulfilmentInfo fulfilmentInfo = response.getFulfilmentInfo(0);
        assertThat(fulfilmentInfo.getShopSku()).isEqualTo("000042.sku5");
        assertThat(fulfilmentInfo.getWarehouseIntervalAvailabilitiesList())
            .containsExactly(
                AvailabilityProtoConverter.convert(LocalDate.parse("2020-01-02"), LocalDate.parse("2020-01-03"),
                    TOMILINO_ID, blockInfo
                )
            );
    }

    @Test
    public void dontCheckWarehouseAvailabilitiesIfNotRequested() {
        // arrange
        var key = new ShopSkuKey(BERU_ID, "000042.sku5");
        var blockInfo = blockInfo("code");
        availabilitiesApi.putAvailability(key, TOMILINO_ID, blockInfo);

        // act
        SearchFulfilmentSskuParamsResponse response =
            service.searchFulfilmentSskuParams(SearchFulfilmentSskuParamsRequest.newBuilder()
                .addKeys(protoKey(BERU_ID, "000042.sku5"))
                .addWarehouseId(TOMILINO_ID)
                .setReturnWarehouseAvailabilities(false)
                .build());

        // assert
        assertThat(response.getFulfilmentInfoList()).hasSize(1);
        OfferFulfilmentInfo fulfilmentInfo = response.getFulfilmentInfo(0);
        assertThat(fulfilmentInfo.getShopSku()).isEqualTo("000042.sku5");
        assertThat(fulfilmentInfo.getWarehouseAvailabilitiesList()).isEmpty();
    }

    @Test
    public void dontCheckWarehouseAvailabilitiesIfNotRequestedInInterval() {
        // arrange
        var key = new ShopSkuKey(BERU_ID, "000042.sku5");
        var blockInfo = blockInfo("delisted-code");
        availabilitiesApi.putAvailability(key, TOMILINO_ID, blockInfo);

        // act
        SearchFulfilmentSskuParamsResponse response =
            service.searchFulfillmentSskuParamsForInterval(SearchFulfillmentSskuParamsForIntervalRequest.newBuilder()
                .addKeys(protoKey(BERU_ID, "000042.sku5"))
                .addWarehouseId(TOMILINO_ID)
                .setInboundDateFrom("2020-01-02").setInboundDateTo("2020-01-03")
                .setReturnWarehouseAvailabilities(false)
                .build());

        // assert
        assertThat(response.getFulfilmentInfoList()).hasSize(1);
        OfferFulfilmentInfo fulfilmentInfo = response.getFulfilmentInfo(0);
        assertThat(fulfilmentInfo.getShopSku()).isEqualTo("000042.sku5");
        assertThat(fulfilmentInfo.getWarehouseIntervalAvailabilitiesList()).isEmpty();
    }

    @Test
    public void searchByKeyWillReturnOnlyNotAllowedIntervals() {
        // arrange
        var key = new ShopSkuKey(BERU_ID, "000042.sku5");
        var blockInfo = blockInfo("delisted-code");
        availabilitiesApi.putAvailability(key, TOMILINO_ID, blockInfo);

        // act
        SearchFulfilmentSskuParamsResponse response =
            service.searchFulfillmentSskuParamsForInterval(SearchFulfillmentSskuParamsForIntervalRequest.newBuilder()
                .addKeys(protoKey(BERU_ID, "000042.sku5"))
                .addWarehouseId(TOMILINO_ID)
                .setInboundDateFrom("2020-01-02").setInboundDateTo("2020-01-03")
                .build());

        // assert
        assertThat(response.getFulfilmentInfoList()).hasSize(1);
        OfferFulfilmentInfo fulfilmentInfo = response.getFulfilmentInfo(0);
        assertThat(fulfilmentInfo.getShopSku()).isEqualTo("000042.sku5");
        assertThat(fulfilmentInfo.getAllowInbound()).isTrue();
        assertThat(fulfilmentInfo.getAllowCargoType()).isTrue();
        assertThat(fulfilmentInfo.getWarehouseIntervalAvailabilitiesList())
            .containsExactly(
                AvailabilityProtoConverter.convert(LocalDate.parse("2020-01-02"), LocalDate.parse("2020-01-03"),
                    TOMILINO_ID, blockInfo
                )
            );
    }

    @Test
    public void searchDontFetchMboIfNotAsked() {
        SearchFulfilmentSskuParamsResponse response =
            service.searchFulfilmentSskuParams(SearchFulfilmentSskuParamsRequest.newBuilder()
                .addKeys(protoKey(BERU_ID, "000042.sku5"))
                .setReturnMskuData(false)
                .build()
            );

        assertThat(response.getFulfilmentInfoList()).hasSize(1);

        OfferFulfilmentInfo fulfilmentInfo = response.getFulfilmentInfo(0);
        assertThat(fulfilmentInfo.getShopSku()).isEqualTo("000042.sku5");
        assertThat(fulfilmentInfo.hasMskuTitle()).isFalse();
    }

    @Test
    public void shouldFillVendorInfo() {
        SearchFulfilmentSskuParamsResponse response =
            service.searchFulfilmentSskuParams(SearchFulfilmentSskuParamsRequest.newBuilder()
                .addKeys(protoKey(BERU_ID, "000042.sku5"))
                .setReturnMskuData(false)
                .build()
            );

        assertThat(response.getFulfilmentInfoList()).hasSize(1);

        OfferFulfilmentInfo fulfilmentInfo = response.getFulfilmentInfo(0);
        assertThat(fulfilmentInfo.getShopSku()).isEqualTo("000042.sku5");
        assertThat(fulfilmentInfo.getMarketVendorId()).isEqualTo(12);
        assertThat(fulfilmentInfo.getMarketVendorName()).isEqualTo("Vendor 12!");
    }

    @Test
    public void searchIntervalDontFetchMboIfNotAsked() {
        SearchFulfilmentSskuParamsResponse response =
            service.searchFulfillmentSskuParamsForInterval(SearchFulfillmentSskuParamsForIntervalRequest.newBuilder()
                .addKeys(protoKey(BERU_ID, "000042.sku5"))
                .setInboundDateFrom("2020-01-01")
                .setInboundDateTo("2020-01-01")
                .addWarehouseId(TOMILINO_ID)
                .setReturnMskuData(false)
                .build()
            );

        assertThat(response.getFulfilmentInfoList()).hasSize(1);

        OfferFulfilmentInfo fulfilmentInfo = response.getFulfilmentInfo(0);
        assertThat(fulfilmentInfo.getShopSku()).isEqualTo("000042.sku5");
        assertThat(fulfilmentInfo.hasMskuTitle()).isFalse();
    }

    @Test
    public void searchShouldCheck3pOffers() {
        var msku = TestUtils.newMsku(191919L, 14L);
        mskuRepository.save(msku);
        modelStorageCachingService.addModel(createTestModel(191919L, 14L));

        offerRepository.insertOffers(new Offer()
            .setBusinessId(82).setShopSku("sku-msku-archived")
            .setTitle("sku-msku-archived").setCategoryIdForTests(14L, Offer.BindingKind.APPROVED)
            .setShopCategoryName("category 22")
            .setIsOfferContentPresent(true)
            .storeOfferContent(OfferContent.initEmptyContent())
            .setApprovedSkuMappingInternal(new Offer.Mapping(191919L, LocalDateTime.now()))
            .setApprovedSkuMappingConfidence(Offer.MappingConfidence.CONTENT)
            .setServiceOffers(List.of(new Offer.ServiceOffer(84)
                .setSupplierType(MbocSupplierType.THIRD_PARTY)))
        );

        // arrange
        var key = new ShopSkuKey(84, "sku-msku-archived");
        var blockInfo = blockInfo("archived");
        availabilitiesApi.putAvailability(key, TOMILINO_ID, blockInfo);

        SearchFulfilmentSskuParamsResponse response =
            service.searchFulfilmentSskuParams(SearchFulfilmentSskuParamsRequest.newBuilder()
                .addKeys(protoKey(84, "sku-msku-archived"))
                .addWarehouseId(TOMILINO_ID)
                .build());

        assertThat(response.getFulfilmentInfoList())
            .extracting(OfferFulfilmentInfo::getShopSku)
            .containsExactlyInAnyOrder("sku-msku-archived");

        OfferFulfilmentInfo allow = response.getFulfilmentInfo(0);
        WarehouseAvailability availability = allow.getWarehouseAvailabilities(0);
        assertThat(availability.getAllowInbound()).isFalse();
    }

    @Test
    public void searchShouldContainRenderedMessage() {
        // arrange
        var key = new ShopSkuKey(60, "sku4");
        var blockInfo = blockInfo("seasonal");
        blockInfo.getMessage().setText("Склад 'WAREHOUSE-NAME' #998 не принимает этот товар в текущий момент," +
            " возможные даты поставок: 1 августа - 7 ноября, 20 декабря - 13 января");
        availabilitiesApi.putAvailability(key, TOMILINO_ID, blockInfo);

        SearchFulfilmentSskuParamsResponse response = service.searchFulfilmentSskuParams(
            SearchFulfilmentSskuParamsRequest.newBuilder()
                .addKeys(protoKey(60, "sku4"))
                .addKeys(protoKey(BERU_ID, "000042.sku5"))
                .addWarehouseId(TOMILINO_ID)
                .build()
        );

        OfferFulfilmentInfo infoSku4 = response.getFulfilmentInfoList().stream()
            .filter(info -> info.getShopSku().equals("sku4")).findFirst().orElseThrow();
        WarehouseAvailability availability = infoSku4.getWarehouseAvailabilities(0);
        assertThat(availability.getAllowInboundComment().getRendered())
            .isEqualTo("Склад 'WAREHOUSE-NAME' #998 не принимает этот товар в текущий момент," +
                " возможные даты поставок: 1 августа - 7 ноября, 20 декабря - 13 января");
    }

    @Test
    public void searchShouldWorkEvenIfMskuNotSyncedFromMbo() {
        long marketSkuId = 30967937;
        long categoryId = 912;
        long offerId = 95232016;

        modelStorageCachingService
            .addModel(createTestModel(marketSkuId, categoryId));

        offerRepository.insertOffer(
            OfferTestUtils.simpleOffer(offerId)
                .updateApprovedSkuMapping(OfferTestUtils.mapping(marketSkuId),
                    Offer.MappingConfidence.CONTENT)
                .setCategoryIdForTests(categoryId, Offer.BindingKind.SUGGESTED)
        );

        Offer offer = offerRepository.getOfferById(offerId);

        assertThat(mskuRepository.findById(marketSkuId)).isEmpty();

        SearchFulfilmentSskuParamsResponse response = service.searchFulfilmentSskuParams(
            SearchFulfilmentSskuParamsRequest.newBuilder()
                .addKeys(protoKey(offer.getBusinessId(), offer.getShopSku()))
                .addWarehouseId(TOMILINO_ID)
                .build()
        );

        OfferFulfilmentInfo infoSku = response.getFulfilmentInfoList().stream()
            .filter(info -> info.getShopSku().equals(offer.getShopSku())).findFirst().orElseThrow();
        WarehouseAvailability availability = infoSku.getWarehouseAvailabilities(0);

        assertThat(availability.getAllowInbound()).isTrue();
    }

    @Test
    public void searchShouldWorkEvenIfNoApprovedMapping() {
        long categoryId = 920;
        long offerId = 95232018;

        offerRepository.insertOffer(
            OfferTestUtils.simpleOffer(offerId)
                .setCategoryIdForTests(categoryId, Offer.BindingKind.SUGGESTED)
        );

        Offer offer = offerRepository.getOfferById(offerId);
        Assert.assertNull(offer.getApprovedSkuMapping());

        SearchFulfilmentSskuParamsResponse response = service.searchFulfilmentSskuParams(
            SearchFulfilmentSskuParamsRequest.newBuilder()
                .addKeys(protoKey(offer.getBusinessId(), offer.getShopSku()))
                .build()
        );

        List<OfferFulfilmentInfo> resultOffers = response.getFulfilmentInfoList().stream()
            .filter(info -> info.getShopSku().equals(offer.getShopSku()))
            .collect(Collectors.toList());

        Assert.assertTrue(resultOffers.isEmpty());
    }

    @Test
    public void searchShouldUseMskuTable() {
        Msku msku = mskuRepository.getById(404040L);
        msku.setCargoTypeLmsIds(2L);
        mskuRepository.save(msku);

        SearchFulfilmentSskuParamsResponse response = service
            .searchFulfilmentSskuParams(SearchFulfilmentSskuParamsRequest.newBuilder()
                .addKeys(protoKey(60, "sku4"))
                .build());

        OfferFulfilmentInfo infoSku4 = response.getFulfilmentInfoList().stream()
            .filter(info -> info.getShopSku().equals("sku4")).findFirst().orElseThrow();

        assertThat(infoSku4.getCargoTypesList())
            .extracting(OfferFulfilmentInfo.MskuCargoType::getName)
            .containsExactly("cargo20");
    }

    @Test
    public void searchShouldReturnCargoType() {
        Msku msku = mskuRepository.findById(404040L).orElseThrow().setCargoTypeLmsIds(1L);
        mskuRepository.save(msku);
        msku = mskuRepository.findById(505050L).orElseThrow().setCargoTypeLmsIds(2L, 3L);
        mskuRepository.save(msku);

        SearchFulfilmentSskuParamsResponse response = service
            .searchFulfilmentSskuParams(SearchFulfilmentSskuParamsRequest.newBuilder()
                .addKeys(protoKey(60, "sku4"))
                .addKeys(protoKey(BERU_ID, "000042.sku5"))
                .build());

        OfferFulfilmentInfo infoSku4 = response.getFulfilmentInfoList().stream()
            .filter(info -> info.getShopSku().equals("sku4")).findFirst().orElseThrow();
        assertThat(infoSku4.getCargoTypesList())
            .containsExactlyInAnyOrder(OfferFulfilmentInfo.MskuCargoType.newBuilder()
                .setId(1L)
                .setParameterId(10L)
                .setName("cargo10")
                .buildPartial());

        OfferFulfilmentInfo infoSku5 = response.getFulfilmentInfoList().stream()
            .filter(info -> info.getShopSku().equals("000042.sku5")).findFirst().orElseThrow();
        assertThat(infoSku5.getCargoTypesList())
            .containsExactlyInAnyOrder(OfferFulfilmentInfo.MskuCargoType.newBuilder()
                    .setId(2L)
                    .setParameterId(20L)
                    .setName("cargo20")
                    .buildPartial(),
                OfferFulfilmentInfo.MskuCargoType.newBuilder()
                    .setId(3L)
                    .setParameterId(30L)
                    .setName("cargo30")
                    .buildPartial());
    }

    @Test
    public void testReturnsBothCargoTypeAvailabilityAndOtherReason() {
        // тест проверяет, что если оффер заблокирован одновременно
        // из-за карготипов и любой другой причине (например, потому что оффер delisted),
        // то будет возвращено 2 статуса
        var key = new ShopSkuKey(60, "sku4");
        var delisted = blockInfo("mboc.msku.error.supply-forbidden.delisted-offer");
        delisted.getMessage().setText(MbocErrors.get().mskuNotAvailableForDeliveryDelistedOffer().toString());
        var missing = blockInfo("mboc.msku.error.supply-forbidden.cargo-type-missing");
        missing.setMissingCargoTypes(List.of(2L));

        availabilitiesApi.putAvailability(key, TOMILINO_ID, delisted, missing);

        // блокируем по карготипу
        Msku msku = mskuRepository.findById(404040L).orElseThrow().setCargoTypeLmsIds(1L);
        mskuRepository.save(msku);

        SearchFulfilmentSskuParamsResponse response = service
            .searchFulfilmentSskuParams(SearchFulfilmentSskuParamsRequest.newBuilder()
                .addKeys(protoKey(60, "sku4"))
                .addWarehouseId(WarehouseRepository.TOMILINO_ID)
                .build());

        OfferFulfilmentInfo infoSku4 = response.getFulfilmentInfoList().stream()
            .filter(info -> info.getShopSku().equals("sku4")).findFirst().orElseThrow();
        WarehouseAvailability availability = infoSku4.getWarehouseAvailabilities(0);

        assertThat(availability.getAllowInbound()).isFalse();
        assertThat(availability.getAllowInboundComment().getRendered())
            .isEqualTo(MbocErrors.get().mskuNotAvailableForDeliveryDelistedOffer().toString());
        assertThat(availability.getAllowCargoType()).isFalse();

        assertThat(infoSku4.getCargoTypesList())
            .containsExactlyInAnyOrder(
                OfferFulfilmentInfo.MskuCargoType.newBuilder()
                    .setId(1L)
                    .setParameterId(10L)
                    .setName("cargo10")
                    .buildPartial());
    }

    @Test
    public void convertEmptyToActiveFor3P() throws ApiException {
        var response = service.searchFulfilmentSskuParams(SearchFulfilmentSskuParamsRequest.newBuilder()
            .addKeys(protoKey(60, "sku4"))
            .addWarehouseId(WarehouseRepository.TOMILINO_ID)
            .build());

        var infoSku4 = response.getFulfilmentInfoList().stream()
            .filter(info -> info.getShopSku().equals("sku4")).findFirst().orElseThrow();
        assertThat(infoSku4.getAvailability()).isEqualTo(SupplierOffer.Availability.ACTIVE);
    }

    @Test
    public void convertEmptyToInactiveFor1P() throws ApiException {
        var response = service.searchFulfilmentSskuParams(SearchFulfilmentSskuParamsRequest.newBuilder()
            .addKeys(protoKey(BERU_ID, "000042.sku5"))
            .addWarehouseId(WarehouseRepository.TOMILINO_ID)
            .build());

        var infoSku4 = response.getFulfilmentInfoList().stream()
            .filter(info -> info.getShopSku().equals("000042.sku5")).findFirst().orElseThrow();
        assertThat(infoSku4.getAvailability()).isEqualTo(SupplierOffer.Availability.INACTIVE);
    }

    @Test
    public void convertInactiveTmpToInactive() throws ApiException {
        var key = new ShopSkuKey(60, "sku4");
        sskuStatusApi.updateSskuStatus(new UpdateSskuStatusRequest().addKeysItem(key).status(INACTIVE_TMP));

        var response = service.searchFulfilmentSskuParams(SearchFulfilmentSskuParamsRequest.newBuilder()
            .addKeys(protoKey(60, "sku4"))
            .addWarehouseId(WarehouseRepository.TOMILINO_ID)
            .build());

        var infoSku4 = response.getFulfilmentInfoList().stream()
            .filter(info -> info.getShopSku().equals("sku4")).findFirst().orElseThrow();
        assertThat(infoSku4.getAvailability()).isEqualTo(SupplierOffer.Availability.INACTIVE);
    }

    @Test
    public void convertPendingToActive() throws ApiException {
        var key = new ShopSkuKey(60, "sku4");
        sskuStatusApi.updateSskuStatus(new UpdateSskuStatusRequest().addKeysItem(key).status(PENDING));

        var response = service.searchFulfilmentSskuParams(SearchFulfilmentSskuParamsRequest.newBuilder()
            .addKeys(protoKey(60, "sku4"))
            .addWarehouseId(WarehouseRepository.TOMILINO_ID)
            .build());

        var infoSku4 = response.getFulfilmentInfoList().stream()
            .filter(info -> info.getShopSku().equals("sku4")).findFirst().orElseThrow();
        assertThat(infoSku4.getAvailability()).isEqualTo(SupplierOffer.Availability.ACTIVE);
    }

    @Test
    public void convertActiveToActive() throws ApiException {
        var key = new ShopSkuKey(60, "sku4");
        sskuStatusApi.updateSskuStatus(new UpdateSskuStatusRequest().addKeysItem(key).status(ACTIVE));

        var response = service.searchFulfilmentSskuParams(SearchFulfilmentSskuParamsRequest.newBuilder()
            .addKeys(protoKey(60, "sku4"))
            .addWarehouseId(WarehouseRepository.TOMILINO_ID)
            .build());

        var infoSku4 = response.getFulfilmentInfoList().stream()
            .filter(info -> info.getShopSku().equals("sku4")).findFirst().orElseThrow();
        assertThat(infoSku4.getAvailability()).isEqualTo(SupplierOffer.Availability.ACTIVE);
    }

    @Test
    public void convertDelistedToDelisted() throws ApiException {
        var key = new ShopSkuKey(60, "sku4");
        sskuStatusApi.updateSskuStatus(new UpdateSskuStatusRequest().addKeysItem(key).status(DELISTED));

        var response = service.searchFulfilmentSskuParams(SearchFulfilmentSskuParamsRequest.newBuilder()
            .addKeys(protoKey(60, "sku4"))
            .addWarehouseId(WarehouseRepository.TOMILINO_ID)
            .build());

        var infoSku4 = response.getFulfilmentInfoList().stream()
            .filter(info -> info.getShopSku().equals("sku4")).findFirst().orElseThrow();
        assertThat(infoSku4.getAvailability()).isEqualTo(SupplierOffer.Availability.DELISTED);
    }

    @Test
    public void testPassWrongParamsWillFallWithBadRequest() {
        Assertions.assertThatThrownBy(() -> service
                .searchFulfilmentSskuParams(SearchFulfilmentSskuParamsRequest.newBuilder()
                    .addKeys(protoKey(60, "sku4"))
                    .setInboundDate("2020/01/01")
                    .build())
            ).isInstanceOf(BadRequestException.class)
            .hasMessage("Failed to parse date 2020/01/01");
    }

    @Test
    public void searchServiceOffers() {
        SearchFulfilmentSskuParamsResponse response =
            service.searchFulfilmentSskuParams(SearchFulfilmentSskuParamsRequest.newBuilder()
                .addKeys(protoKey(102, "sku100"))
                .addKeys(protoKey(202, "sku200"))
                .build());

        assertThat(response.getFulfilmentInfoList())
            .extracting(s -> new ShopSkuKey(s.getSupplierId(), s.getShopSku()))
            .containsExactlyInAnyOrder(
                new ShopSkuKey(102, "sku100")
                // supplier_id 202 weren't find, because it is MARKET_SHOP
            );
    }

    @Test
    public void searchShouldSkipBizOffers() {
        SearchFulfilmentSskuParamsResponse response =
            service.searchFulfilmentSskuParams(SearchFulfilmentSskuParamsRequest.newBuilder()
                .addKeys(protoKey(100, "sku100"))
                .addKeys(protoKey(200, "sku200"))
                .build());

        assertThat(response.getFulfilmentInfoList()).isEmpty();
    }

    @Test
    public void testPassWrongParamsInIntervalHandlerWillFallWithBadRequest() {
        Assertions.assertThatThrownBy(() -> service
                .searchFulfillmentSskuParamsForInterval(SearchFulfillmentSskuParamsForIntervalRequest.newBuilder()
                    .addKeys(protoKey(60, "sku4"))
                    .addWarehouseId(TOMILINO_ID)
                    .build())
            ).isInstanceOf(BadRequestException.class)
            .hasMessage("Both inbound_date_from and inbound_date_to should be set");

        Assertions.assertThatThrownBy(() -> service
                .searchFulfillmentSskuParamsForInterval(SearchFulfillmentSskuParamsForIntervalRequest.newBuilder()
                    .addKeys(protoKey(60, "sku4"))
                    .setInboundDateTo("2020-01-01")
                    .addWarehouseId(TOMILINO_ID)
                    .build())
            ).isInstanceOf(BadRequestException.class)
            .hasMessage("Both inbound_date_from and inbound_date_to should be set");

        Assertions.assertThatThrownBy(() -> service
                .searchFulfillmentSskuParamsForInterval(SearchFulfillmentSskuParamsForIntervalRequest.newBuilder()
                    .addKeys(protoKey(60, "sku4"))
                    .setInboundDateFrom("2020-01-01")
                    .addWarehouseId(TOMILINO_ID)
                    .build())
            ).isInstanceOf(BadRequestException.class)
            .hasMessage("Both inbound_date_from and inbound_date_to should be set");

        Assertions.assertThatThrownBy(() -> service
                .searchFulfillmentSskuParamsForInterval(SearchFulfillmentSskuParamsForIntervalRequest.newBuilder()
                    .addKeys(protoKey(60, "sku4"))
                    .setInboundDateFrom("2020/01/01")
                    .setInboundDateTo("2020-01-01")
                    .addWarehouseId(TOMILINO_ID)
                    .build())
            ).isInstanceOf(BadRequestException.class)
            .hasMessage("Failed to parse date 2020/01/01");

        Assertions.assertThatThrownBy(() -> service
                .searchFulfillmentSskuParamsForInterval(SearchFulfillmentSskuParamsForIntervalRequest.newBuilder()
                    .addKeys(protoKey(60, "sku4"))
                    .setInboundDateFrom("2020-01-01")
                    .setInboundDateTo("2020/01/01")
                    .addWarehouseId(TOMILINO_ID)
                    .build())
            ).isInstanceOf(BadRequestException.class)
            .hasMessage("Failed to parse date 2020/01/01");

        Assertions.assertThatThrownBy(() -> service
                .searchFulfillmentSskuParamsForInterval(SearchFulfillmentSskuParamsForIntervalRequest.newBuilder()
                    .addKeys(protoKey(60, "sku4"))
                    .setInboundDateFrom("2020-02-02")
                    .setInboundDateTo("2020-01-01")
                    .addWarehouseId(TOMILINO_ID)
                    .build())
            ).isInstanceOf(BadRequestException.class)
            .hasMessage("inbound_date_from (2020-02-02) is after inbound_date_to (2020-01-01)");

        // good check
        Assertions.assertThatCode(() -> service
            .searchFulfillmentSskuParamsForInterval(SearchFulfillmentSskuParamsForIntervalRequest.newBuilder()
                .addKeys(protoKey(60, "sku4"))
                .setInboundDateFrom("2020-01-01")
                .setInboundDateTo("2020-01-01")
                .addWarehouseId(TOMILINO_ID)
                .build())
        ).doesNotThrowAnyException();
    }

    @Test
    public void testReturnIntervalWarehouseIfIntervalWasPass() {
        var key = new ShopSkuKey(60, "sku4");
        var archived = blockInfo("archived");
        var msku = blockInfo("msku");
        availabilitiesApi.putAvailability(key, TOMILINO_ID, archived);
        availabilitiesApi.putAvailability(key, TOMILINO_ID, LocalDate.parse("2020-05-07"), LocalDate.MAX, msku);

        SearchFulfilmentSskuParamsResponse response = service
            .searchFulfillmentSskuParamsForInterval(SearchFulfillmentSskuParamsForIntervalRequest.newBuilder()
                .addKeys(protoKey(60, "sku4"))
                .setInboundDateFrom("2020-05-01")
                .setInboundDateTo("2020-05-20")
                .addWarehouseId(TOMILINO_ID)
                .build());

        OfferFulfilmentInfo fulfilmentInfo = response.getFulfilmentInfo(0);
        Assertions.assertThat(fulfilmentInfo.getShopSku()).isEqualTo("sku4");
        Assertions.assertThat(fulfilmentInfo.getAllowInbound()).isTrue();
        Assertions.assertThat(fulfilmentInfo.getAllowCargoType()).isTrue();
        Assertions.assertThat(fulfilmentInfo.getWarehouseAvailabilitiesList()).isEmpty();
        Assertions.assertThat(fulfilmentInfo.getWarehouseIntervalAvailabilitiesList())
            .containsExactlyInAnyOrder(
                AvailabilityProtoConverter.convert(LocalDate.parse("2020-05-01"), LocalDate.parse("2020-05-06"),
                    TOMILINO_ID, archived),
                AvailabilityProtoConverter.convert(LocalDate.parse("2020-05-07"), LocalDate.parse("2020-05-20"),
                    TOMILINO_ID, archived, msku)
            );
    }

    @Test
    public void searchShouldSetDefaultMaskWhenNoMaskAndMaskControlFlagSet() {
        String defaultSNMask = "^([\\dA-Za-z\\/]{10,12}|[\\dA-Za-z\\/]{14,16}|[\\dA-Za-z\\/]{18,20})$";
        String defaultIMEIMask = "^(\\d{15}|\\d{17})$";
        storageKeyValueService.putValue(DeliveryParamsImpl.DEFAULT_SERIAL_NUMBER_MASK_KEY, defaultSNMask);
        storageKeyValueService.putValue(DeliveryParamsImpl.DEFAULT_IMEI_MASK_KEY, defaultIMEIMask);
        modelStorageCachingService.getModel(404040L).ifPresent(model -> model.setParameterValues(
                List.of(
                    ModelStorage.ParameterValue.newBuilder()
                        .setParamId(KnownMdmMboParams.SERIAL_NUMBER_CONTROL_PARAM_ID)
                        .setXslName("-")
                        .setValueType(MboParameters.ValueType.BOOLEAN)
                        .setBoolValue(true)
                        .build(),
                    ModelStorage.ParameterValue.newBuilder()
                        .setParamId(KnownMdmMboParams.IMEI_CONTROL_PARAM_ID)
                        .setXslName("-")
                        .setValueType(MboParameters.ValueType.BOOLEAN)
                        .setBoolValue(true)
                        .build()
                )
            )
        );
        SearchFulfilmentSskuParamsResponse response =
            service.searchFulfilmentSskuParams(SearchFulfilmentSskuParamsRequest.newBuilder()
                .addKeys(protoKey(60, "sku4"))
                .build());

        OfferFulfilmentInfo fulfilmentInfo = response.getFulfilmentInfo(0);
        Assertions.assertThat(
            fulfilmentInfo.getModelParamList().stream().anyMatch(
                parameterValue -> containsStringParamValue(
                    parameterValue,
                    KnownMdmMboParams.IMEI_MASK_PARAM_ID,
                    defaultIMEIMask
                )
            )
        ).isTrue();

        Assertions.assertThat(
            fulfilmentInfo.getModelParamList().stream().anyMatch(
                parameterValue -> containsStringParamValue(
                    parameterValue,
                    KnownMdmMboParams.SERIAL_NUMBER_MASK_PARAM_ID,
                    defaultSNMask
                )
            )
        ).isTrue();
    }

    @Test
    public void searchShouldReturnWarehouseServices() {
        final int supplierIdNeedSort = 60;
        final String shopSkuNeedSort = "sku4";

        final int supplierIdNoInfo = 102;
        final String shopSkuNoInfo = "sku100";

        var warehouseServiceNeedSort = WarehouseService.builder()
            .supplierId(supplierIdNeedSort)
            .shopSku(shopSkuNeedSort)
            .needSort(true)
            .build();

        warehouseServiceRepository.saveOrUpdateAll(List.of(warehouseServiceNeedSort));

        SearchFulfilmentSskuParamsResponse response = service
            .searchFulfilmentSskuParams(SearchFulfilmentSskuParamsRequest.newBuilder()
                .addKeys(protoKey(supplierIdNeedSort, shopSkuNeedSort))
                .addKeys(protoKey(supplierIdNoInfo, shopSkuNoInfo))
                .addWarehouseId(TOMILINO_ID)
                .setReturnWarehouseServices(true)
                .build());

        var supplierIdToWarehouseService = response.getFulfilmentInfoList()
            .stream()
            .collect(Collectors.toMap(OfferFulfilmentInfo::getSupplierId, OfferFulfilmentInfo::getWarehouseServices));

        Assertions.assertThat(response.getFulfilmentInfoList()).hasSize(2);

        Assertions.assertThat(supplierIdToWarehouseService.get(supplierIdNeedSort))
            .extracting(OfferFulfilmentInfo.WarehouseServices::getNeedSort)
            .isEqualTo(true);

        Assertions.assertThat(supplierIdToWarehouseService.get(supplierIdNoInfo))
            .extracting(OfferFulfilmentInfo.WarehouseServices::getNeedSort)
            .isEqualTo(false);

        verify(warehouseServiceService, times(1)).getWarehouseServicesAsMap(anyCollection());
    }

    @Test
    public void searchShouldNotReturnWarehouseServicesOnNoNeedSort() {
        final int supplierIdNeedSort = 60;
        final String shopSkuNeedSort = "sku4";

        final int supplierIdNoInfo = 102;
        final String shopSkuNoInfo = "sku100";

        var warehouseServiceNeedSort = WarehouseService.builder()
            .supplierId(supplierIdNeedSort)
            .shopSku(shopSkuNeedSort)
            .needSort(false)
            .build();

        warehouseServiceRepository.saveOrUpdateAll(List.of(warehouseServiceNeedSort));

        SearchFulfilmentSskuParamsResponse response = service
            .searchFulfilmentSskuParams(SearchFulfilmentSskuParamsRequest.newBuilder()
                .addKeys(protoKey(supplierIdNeedSort, shopSkuNeedSort))
                .addKeys(protoKey(supplierIdNoInfo, shopSkuNoInfo))
                .addWarehouseId(TOMILINO_ID)
                .build());

        assertThat(response.getFulfilmentInfoList())
            .noneMatch(OfferFulfilmentInfo::hasWarehouseServices);
    }

    private boolean containsStringParamValue(
        ModelStorage.ParameterValue paramValue,
        long paramId,
        String value
    ) {
        return paramValue.hasParamId()
            && paramValue.getParamId() == paramId
            && paramValue.getValueType() == MboParameters.ValueType.STRING
            && value.equals(
            paramValue.getStrValueList().stream()
                .findAny()
                .map(ModelStorage.LocalizedString::getValue)
                .orElse(null)
        );
    }

    private MasterData createMasterData(int suppierId, String shopSku, int minShipment, String customCommunityCode) {
        MasterData result = TestDataUtils.generateMasterData(shopSku, suppierId, enhancedRandom);
        result.setMinShipment(minShipment);
        result.setCustomsCommodityCode(customCommunityCode);
        return result;
    }

    private Model createTestModel(long id, long categoryId) {
        return new Model()
            .setId(id)
            .setTitle("Model " + id)
            .setCategoryId(categoryId)
            .setModelType(Model.ModelType.GURU)
            .setVendorCodes(List.of("TEST", "TSET"))
            .setBarCodes(List.of("A", "B"))
            .setParameterValues(List.of(ModelStorage.ParameterValue.newBuilder()
                .setXslName("test")
                .setValueType(MboParameters.ValueType.BOOLEAN)
                .setBoolValue(true)
                .build()));
    }

    private MboMappingsForDelivery.FulfillmentShopSkuKey protoKey(int supplierId, String shopSku) {
        return MboMappingsForDelivery.FulfillmentShopSkuKey.newBuilder()
            .setSupplierId(supplierId)
            .setShopSku(shopSku)
            .build();
    }

    private BlockInfo blockInfo(String errorCode) {
        return new BlockInfo().message(new Message().code(errorCode).text("Error"));
    }
}
