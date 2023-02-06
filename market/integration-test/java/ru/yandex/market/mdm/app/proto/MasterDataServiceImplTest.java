package ru.yandex.market.mdm.app.proto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.collect.Iterables;
import io.github.benas.randombeans.api.EnhancedRandom;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.util.Lists;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.ir.http.MdmIrisPayload;
import ru.yandex.market.ir.http.MdmIrisPayload.Item;
import ru.yandex.market.mbo.mdm.common.infrastructure.MdmLogbrokerService;
import ru.yandex.market.mbo.mdm.common.infrastructure.MdmLogbrokerServiceMock;
import ru.yandex.market.mbo.mdm.common.masterdata.model.MappingCacheDao;
import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.blocks.BlocksToMasterDataMerger;
import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.blocks.MasterDataIntoBlocksSplitter;
import ru.yandex.market.mbo.mdm.common.masterdata.model.msku.YtStorageSupportMode;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.CategoryParamValue;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.MdmParamOption;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.SskuGoldenParamValue;
import ru.yandex.market.mbo.mdm.common.masterdata.model.queue.MdmEnqueueReason;
import ru.yandex.market.mbo.mdm.common.masterdata.model.queue.MdmMskuQueueInfo;
import ru.yandex.market.mbo.mdm.common.masterdata.model.queue.SskuToRefreshInfo;
import ru.yandex.market.mbo.mdm.common.masterdata.model.ssku.CommonSsku;
import ru.yandex.market.mbo.mdm.common.masterdata.model.supplier.MdmSupplier;
import ru.yandex.market.mbo.mdm.common.masterdata.model.supplier.MdmSupplierSalesModel;
import ru.yandex.market.mbo.mdm.common.masterdata.model.supplier.MdmSupplierType;
import ru.yandex.market.mbo.mdm.common.masterdata.model.utils.ValidationContextHelperService;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.BusinessLockStatusRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.FromIrisItemRepositoryMock;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.MappingsCacheRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.MasterDataLogIdService;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.MdmSupplierRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.ReferenceItemRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.param.CategoryParamValueRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.param.CategoryParamValueRepositoryMock;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.param.GoldSskuRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.param.SilverSskuRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.proto.ReferenceItemWrapper;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.queue.MskuToRefreshRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.queue.SskuToRefreshRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.verdict.SskuGoldenVerdictRepositoryMock;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.verdict.SskuPartnerVerdictRepositoryMock;
import ru.yandex.market.mbo.mdm.common.masterdata.services.StorageKeyValueCachingService;
import ru.yandex.market.mbo.mdm.common.masterdata.services.StorageKeyValueCachingServiceMock;
import ru.yandex.market.mbo.mdm.common.masterdata.services.SupplierConverterService;
import ru.yandex.market.mbo.mdm.common.masterdata.services.SupplierConverterServiceMock;
import ru.yandex.market.mbo.mdm.common.masterdata.services.business.MasterDataBusinessMergeService;
import ru.yandex.market.mbo.mdm.common.masterdata.services.business.MdmSskuGroupManager;
import ru.yandex.market.mbo.mdm.common.masterdata.services.business.MdmSupplierCachingService;
import ru.yandex.market.mbo.mdm.common.masterdata.services.cccode.ExistingCCCodeCacheMock;
import ru.yandex.market.mbo.mdm.common.masterdata.services.iris.BeruId;
import ru.yandex.market.mbo.mdm.common.masterdata.services.iris.BeruIdMock;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownMdmParams;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.MdmParamCache;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.ServiceSskuConverter;
import ru.yandex.market.mbo.mdm.common.masterdata.services.verdict.MasterDataValidationService;
import ru.yandex.market.mbo.mdm.common.masterdata.services.verdict.VerdictCalculationByMdHelper;
import ru.yandex.market.mbo.mdm.common.masterdata.services.verdict.VerdictCalculationHelper;
import ru.yandex.market.mbo.mdm.common.masterdata.validator.VghValidationRequirementsProvider;
import ru.yandex.market.mbo.mdm.common.masterdata.validator.block.AdditionalValidationBlocksProvider;
import ru.yandex.market.mbo.mdm.common.masterdata.validator.block.BoxCountValidator;
import ru.yandex.market.mbo.mdm.common.masterdata.validator.block.BusinessPartBlocksValidationService;
import ru.yandex.market.mbo.mdm.common.masterdata.validator.block.CustomsCommodityCodeBlockValidator;
import ru.yandex.market.mbo.mdm.common.masterdata.validator.block.DeliveryTimeBlockValidator;
import ru.yandex.market.mbo.mdm.common.masterdata.validator.block.DimensionsBlockValidator;
import ru.yandex.market.mbo.mdm.common.masterdata.validator.block.DocumentRegNumbersBlockValidator;
import ru.yandex.market.mbo.mdm.common.masterdata.validator.block.GTINValidator;
import ru.yandex.market.mbo.mdm.common.masterdata.validator.block.GuaranteePeriodBlockValidator;
import ru.yandex.market.mbo.mdm.common.masterdata.validator.block.LifeTimeBlockValidator;
import ru.yandex.market.mbo.mdm.common.masterdata.validator.block.ManufacturerCountriesBlockValidator;
import ru.yandex.market.mbo.mdm.common.masterdata.validator.block.MasterDataBlocksValidationService;
import ru.yandex.market.mbo.mdm.common.masterdata.validator.block.MinShipmentBlockValidator;
import ru.yandex.market.mbo.mdm.common.masterdata.validator.block.QuantumOfSupplyBlockValidator;
import ru.yandex.market.mbo.mdm.common.masterdata.validator.block.ServicePartBlocksValidationService;
import ru.yandex.market.mbo.mdm.common.masterdata.validator.block.ShelfLifeBlockValidator;
import ru.yandex.market.mbo.mdm.common.masterdata.validator.block.TransportUnitBlockValidator;
import ru.yandex.market.mbo.mdm.common.masterdata.validator.block.VetisGuidsBlockValidator;
import ru.yandex.market.mbo.mdm.common.masterdata.validator.block.WeightNetValidator;
import ru.yandex.market.mbo.mdm.common.masterdata.validator.block.WeightTareValidator;
import ru.yandex.market.mbo.mdm.common.service.MdmBusinessMigrationMonitoringServiceMock;
import ru.yandex.market.mbo.mdm.common.service.MdmSolomonPushService;
import ru.yandex.market.mbo.mdm.common.service.SskuValidationService;
import ru.yandex.market.mbo.mdm.common.service.SskuValidationServiceImpl;
import ru.yandex.market.mbo.mdm.common.service.monitoring.MdmBusinessMigrationMonitoringService;
import ru.yandex.market.mbo.storage.StorageKeyValueService;
import ru.yandex.market.mboc.common.masterdata.TestDataUtils;
import ru.yandex.market.mboc.common.masterdata.model.MasterData;
import ru.yandex.market.mboc.common.masterdata.model.MbocBaseProtoConverter;
import ru.yandex.market.mboc.common.masterdata.model.TimeInUnits;
import ru.yandex.market.mboc.common.masterdata.parsing.MasterDataValidator;
import ru.yandex.market.mboc.common.masterdata.repository.MasterDataRepository;
import ru.yandex.market.mboc.common.masterdata.services.category.MdmCategorySettingsService;
import ru.yandex.market.mboc.common.masterdata.services.document.DocumentService;
import ru.yandex.market.mboc.common.offers.model.ShopSkuKey;
import ru.yandex.market.mboc.common.offers.repository.MboMappingsServiceMock;
import ru.yandex.market.mboc.common.services.category.CategoryCachingServiceMock;
import ru.yandex.market.mboc.common.utils.MdmProperties;
import ru.yandex.market.mboc.http.MboMappings;
import ru.yandex.market.mboc.http.MboMappingsService;
import ru.yandex.market.mdm.app.MdmBaseIntegrationTestWithProtoApiClass;
import ru.yandex.market.mdm.http.MasterDataProto;
import ru.yandex.market.mdm.http.MdmCommon;

/**
 * @author moskovkin@yandex-team.ru
 * @created 30.06.19
 */
@SuppressWarnings("checkstyle:MagicNumber")
public class MasterDataServiceImplTest extends MdmBaseIntegrationTestWithProtoApiClass {
    private static final int SEED = 42;
    private static final int DATA_COUNT = 10;
    private static final int STRONG_RESTRICTION_CATEGORY_ID = 22;

    @Autowired
    private MasterDataRepository masterDataRepository;
    @Autowired
    private MasterDataLogIdService masterDataLogIdService;
    @Autowired
    private MdmSupplierRepository supplierRepository;
    @Autowired
    private MdmSupplierCachingService mdmSupplierCachingService;
    @Autowired
    private MboMappingsService mboMappingsService;
    @Autowired
    private StorageKeyValueService storageKeyValueService;
    @Autowired
    private MdmLogbrokerService logbrokerProducerService;
    @Autowired
    private ServiceSskuConverter serviceSskuConverter;
    @Autowired
    private SilverSskuRepository silverSskuRepository;
    @Autowired
    private MdmSskuGroupManager mdmSskuGroupManager;
    @Autowired
    private MasterDataBusinessMergeService masterDataBusinessMergeService;
    @Autowired
    private DocumentService documentService;
    @Autowired
    private MskuToRefreshRepository mskuToRefreshRepository;
    @Autowired
    private MappingsCacheRepository mappingsCacheRepository;
    @Autowired
    private BusinessLockStatusRepository businessLockStatusRepository;
    @Autowired
    private SskuToRefreshRepository sskuToRefreshRepository;
    @Autowired
    private VghValidationRequirementsProvider vghValidationRequirementsProvider;
    @Autowired
    private BeruId beruId;
    @Autowired
    private GoldSskuRepository goldSskuRepository;
    @Autowired
    private ReferenceItemRepository referenceItemRepository;
    @Autowired
    private MdmCategorySettingsService mdmCategorySettingsService;
    @Autowired
    private MasterDataIntoBlocksSplitter masterDataIntoBlocksSplitter;
    @Autowired
    private BlocksToMasterDataMerger blocksToMasterDataMerger;
    @Autowired
    private DimensionsBlockValidator dimensionsBlockValidator;
    @Autowired
    private WeightNetValidator weightNetValidator;
    @Autowired
    private WeightTareValidator weightTareValidator;
    @Autowired
    private MdmParamCache mdmParamCache;

    private EnhancedRandom random;
    private SupplierConverterService supplierConverterService;
    private CategoryParamValueRepository categoryParamValueRepository;
    private MboMappingsServiceMock mboMappingsServiceMock;
    private MdmLogbrokerServiceMock logbrokerProducerServiceMock;
    private MasterDataServiceImpl masterDataService;
    private StorageKeyValueCachingService storageKeyValueCachingService;
    private CategoryCachingServiceMock categoryCachingServiceMock;

    @Before
    public void setup() {
        mboMappingsServiceMock = (MboMappingsServiceMock) mboMappingsService;
        logbrokerProducerServiceMock =
            (MdmLogbrokerServiceMock) logbrokerProducerService;
        random = TestDataUtils.defaultRandom(SEED);

        supplierConverterService = new SupplierConverterServiceMock();

        categoryCachingServiceMock = new CategoryCachingServiceMock();
        MasterDataValidationService masterDataValidationService = masterDataValidationService();

        storageKeyValueCachingService = new StorageKeyValueCachingServiceMock(storageKeyValueService);
        VerdictCalculationHelper verdictCalculationHelper = new VerdictCalculationByMdHelper(
            new SskuGoldenVerdictRepositoryMock(),
            new SskuPartnerVerdictRepositoryMock(),
            masterDataValidationService(),
            serviceSskuConverter
        );

        categoryParamValueRepository = new CategoryParamValueRepositoryMock();
        ValidationContextHelperService validationContextHelperService = new ValidationContextHelperService(
            mboMappingsServiceMock,
            categoryParamValueRepository
        );

        SskuValidationService sskuValidationService = new SskuValidationServiceImpl(
            masterDataValidationService,
            validationContextHelperService,
            mappingsCacheRepository,
            vghValidationRequirementsProvider,
            mdmSskuGroupManager,
            documentService);

        MdmBusinessMigrationMonitoringService businessMigrationMonitoringService =
            new MdmBusinessMigrationMonitoringServiceMock();
        masterDataService = new MasterDataServiceImpl(
            masterDataRepository,
            masterDataLogIdService,
            mboMappingsServiceMock,
            logbrokerProducerServiceMock,
            new FromIrisItemRepositoryMock(),
            supplierConverterService,
            storageKeyValueCachingService,
            masterDataBusinessMergeService,
            mdmSskuGroupManager,
            serviceSskuConverter,
            referenceItemRepository,
            Mockito.mock(MdmSolomonPushService.class),
            sskuValidationService,
            supplierRepository,
            businessLockStatusRepository,
            businessMigrationMonitoringService,
            goldSskuRepository,
            beruId,
            silverSskuRepository
        );
        storageKeyValueService.putValue(MdmProperties.SILVER_SSKU_YT_STORAGE_MODE, YtStorageSupportMode.ENABLED.name());
        storageKeyValueService.invalidateCache();
    }

    /**
     * Только для того, чтобы подложить categoryCachingServiceMock в валидаторы.
     * Обычный сервис на тестах в Аркануме падает с ошибкой https://paste.yandex-team.ru/10044329
     * С ходу хитро подложить файлик в ресурсы тоже не получилось.
     */
    private MasterDataValidationService masterDataValidationService() {
        ShelfLifeBlockValidator shelfLifeBlockValidator = new ShelfLifeBlockValidator(categoryCachingServiceMock);
        LifeTimeBlockValidator lifeTimeBlockValidator =
            new LifeTimeBlockValidator(mdmCategorySettingsService, categoryCachingServiceMock);
        GuaranteePeriodBlockValidator guaranteePeriodBlockValidator =
            new GuaranteePeriodBlockValidator(mdmCategorySettingsService, categoryCachingServiceMock);
        BeruId beruId = new BeruIdMock(10000, 10001);
        ExistingCCCodeCacheMock existingCCCodeCacheMock = new ExistingCCCodeCacheMock();

        var customsCommodityCodeBlockValidator =
            new CustomsCommodityCodeBlockValidator(beruId, storageKeyValueService, existingCCCodeCacheMock);

        var servicePartValidationService = new ServicePartBlocksValidationService(
            new MinShipmentBlockValidator(),
            new TransportUnitBlockValidator(),
            new DeliveryTimeBlockValidator(),
            new QuantumOfSupplyBlockValidator()
        );

        var businessPartValidationService = new BusinessPartBlocksValidationService(
            shelfLifeBlockValidator,
            lifeTimeBlockValidator,
            guaranteePeriodBlockValidator,
            new BoxCountValidator(),
            new GTINValidator(),
            new ManufacturerCountriesBlockValidator(storageKeyValueService),
            new VetisGuidsBlockValidator(),
            new DocumentRegNumbersBlockValidator(),
            dimensionsBlockValidator,
            weightNetValidator,
            weightTareValidator,
            customsCommodityCodeBlockValidator
        );

        var masterDataBlocksValidationService = new MasterDataBlocksValidationService(
            shelfLifeBlockValidator,
            lifeTimeBlockValidator,
            guaranteePeriodBlockValidator,
            new BoxCountValidator(),
            customsCommodityCodeBlockValidator,
            new DeliveryTimeBlockValidator(),
            new GTINValidator(),
            new ManufacturerCountriesBlockValidator(storageKeyValueService),
            new MinShipmentBlockValidator(),
            new QuantumOfSupplyBlockValidator(),
            new TransportUnitBlockValidator(),
            new VetisGuidsBlockValidator(),
            new DocumentRegNumbersBlockValidator(),
            dimensionsBlockValidator,
            weightNetValidator,
            weightTareValidator
        );

        MasterDataValidator masterDataValidator = new MasterDataValidator(
            masterDataIntoBlocksSplitter,
            servicePartValidationService,
            businessPartValidationService,
            masterDataBlocksValidationService,
            blocksToMasterDataMerger,
            new AdditionalValidationBlocksProvider(mdmParamCache));
        return new MasterDataValidationService(masterDataValidator);
    }

    @After
    public void cleanup() {
        storageKeyValueService.putValue(MdmProperties.SILVER_SSKU_YT_STORAGE_MODE,
            YtStorageSupportMode.DISABLED.name());
        storageKeyValueService.invalidateCache();
    }

    @Test
    public void whenHasMasterDataShouldFind() {
        List<MasterData> testData = TestDataUtils.generateSskuMsterData(DATA_COUNT, random);
        testData.forEach(md -> md.setVat(null)); // прото не умеет сравнивать ндс для 0%
        testData = storeMasterDataWithSuppliers(testData);
        testData.forEach(md -> md.setModifiedTimestamp(null));

        List<MasterData> whiteData = TestDataUtils.generateSskuMsterData(DATA_COUNT, random);
        whiteData = storeMasterDataWithSuppliers(
            whiteData,
            MdmSupplierType.MARKET_SHOP,
            List.of(MdmSupplierSalesModel.CLICK_AND_COLLECT)
        );
        List<MasterData> unknownData = TestDataUtils.generateSskuMsterData(DATA_COUNT, random);
        masterDataRepository.insertBatch(unknownData);

        List<ShopSkuKey> testKeys = Stream.concat(testData.stream(),
            Stream.concat(whiteData.stream(), unknownData.stream()))
            .map(MasterData::getShopSkuKey)
            .collect(Collectors.toList());

        MasterDataProto.SearchSskuMasterDataRequest request = createSearchRequest(
            testKeys
        );

        MasterDataProto.SearchSskuMasterDataResponse response = masterDataService.searchSskuMasterData(request);

        //testData.addAll(whiteData);
        List<MdmCommon.SskuMasterData> expectedResponseData = testData.stream()
            .map(md -> {
                var data = MbocBaseProtoConverter.pojoToProto(md).toBuilder();
                if (md.getHeavyGood() != null) {
                    data.setHeavyGood(md.getHeavyGood());
                }
                return data.build();
            })
            .collect(Collectors.toList());

        List<MdmCommon.SskuMasterData> result = response.getSskuMasterDataList().stream()
            .map(sskuMD -> sskuMD.toBuilder().clearModifiedTimestamp().build()).collect(Collectors.toList());
        Assertions.assertThat(result)
            .containsExactlyInAnyOrderElementsOf(expectedResponseData);
    }

    @Test
    public void whenShelfLifeOverrideEnabledShouldCorrectlyOverride() {
        MasterData testData = TestDataUtils.generateSskuMsterData(1, random).get(0);
        testData.setVat(null);
        testData.setHeavyGood(null);
        testData = storeMasterDataWithSuppliers(List.of(testData)).get(0);
        testData.setModifiedTimestamp(null);

        MasterDataProto.SearchSskuMasterDataRequest request = createSearchRequest(
            List.of(testData.getShopSkuKey())
        );

        MasterDataProto.SearchSskuMasterDataResponse response = masterDataService.searchSskuMasterData(request);

        MdmCommon.SskuMasterData expectedResponseData = MbocBaseProtoConverter.pojoToProto(testData);

        MdmCommon.SskuMasterData result = response.getSskuMasterDataList().get(0).toBuilder()
            .clearModifiedTimestamp().build();
        Assertions.assertThat(result).isEqualTo(expectedResponseData);

        // test partial shelfLife block
        SskuGoldenParamValue value = (SskuGoldenParamValue) new SskuGoldenParamValue()
            .setShopSkuKey(testData.getShopSkuKey())
            .setMdmParamId(KnownMdmParams.SHELF_LIFE)
            .setNumeric(BigDecimal.valueOf(10));
        SskuGoldenParamValue comment = (SskuGoldenParamValue) new SskuGoldenParamValue()
            .setShopSkuKey(testData.getShopSkuKey())
            .setMdmParamId(KnownMdmParams.SHELF_LIFE_COMMENT)
            .setString("abc");

        goldSskuRepository.insertOrUpdateSsku(
            new CommonSsku(testData.getShopSkuKey())
                .addBaseValue(value)
                .addBaseValue(comment)
        );

        response = masterDataService.searchSskuMasterData(request);

        result = response.getSskuMasterDataList().get(0).toBuilder()
            .clearModifiedTimestamp().build();
        Assertions.assertThat(result).isEqualTo(expectedResponseData);

        // test full shelfLife block
        SskuGoldenParamValue unit = (SskuGoldenParamValue) new SskuGoldenParamValue()
            .setShopSkuKey(testData.getShopSkuKey())
            .setMdmParamId(KnownMdmParams.SHELF_LIFE_UNIT)
            .setOption(new MdmParamOption(3).setRenderedValue("дни"));
        goldSskuRepository.insertOrUpdateSsku(
            goldSskuRepository.findSsku(testData.getShopSkuKey())
                .orElseThrow()
                .addBaseValue(unit)
        );

        response = masterDataService.searchSskuMasterData(request);

        result = response.getSskuMasterDataList().get(0).toBuilder()
            .clearModifiedTimestamp().build();

        testData.setShelfLife(10, TimeInUnits.TimeUnit.DAY);
        testData.setShelfLifeComment("abc");
        expectedResponseData = MbocBaseProtoConverter.pojoToProto(testData);
        Assertions.assertThat(result).isEqualTo(expectedResponseData);
    }

    @Test
    public void whenHaveNoMasterDataShouldNotFind() {
        List<MasterData> testData = TestDataUtils.generateSskuMsterData(DATA_COUNT, random);
        storeMasterDataWithSuppliers(testData);

        List<ShopSkuKey> testKeys = List.of(new ShopSkuKey(1, "NoSuchShopSku"));

        MasterDataProto.SearchSskuMasterDataRequest request = createSearchRequest(
            testKeys
        );

        MasterDataProto.SearchSskuMasterDataResponse response = masterDataService.searchSskuMasterData(request);
        Assertions.assertThat(response.getSskuMasterDataList()).isEmpty();
    }

    @Test
    public void testSerarchSskuWithMeasurementFlag() {
        // given
        int supplierId = 123;
        supplierRepository.insertOrUpdate(new MdmSupplier().setId(supplierId).setType(MdmSupplierType.THIRD_PARTY));

        String shopSku = "456";
        ShopSkuKey shopSkuKey = new ShopSkuKey(supplierId, shopSku);

        long measurementTs = Instant.now().toEpochMilli();
        Item item = Item.newBuilder()
            .setItemId(MdmIrisPayload.MdmIdentifier.newBuilder()
                .setSupplierId(supplierId)
                .setShopSku(shopSku))
            .setMeasurementState(MdmIrisPayload.MeasurementState.newBuilder()
                .setIsMeasured(true)
                .setLastMeasurementTs(measurementTs))
            .build();
        ReferenceItemWrapper referenceItemWrapper = new ReferenceItemWrapper(item);
        referenceItemRepository.insertOrUpdate(referenceItemWrapper);

        // when
        MasterDataProto.SearchSskuMasterDataRequest request = createSearchRequest(List.of(shopSkuKey));
        MasterDataProto.SearchSskuMasterDataResponse response = masterDataService.searchSskuMasterData(request);

        // then
        List<MdmCommon.SskuMasterData> foundSskuMasterDataList = response.getSskuMasterDataList();
        Assertions.assertThat(foundSskuMasterDataList).hasSize(1);
        MdmCommon.SskuMasterData foundSskuMasterData = foundSskuMasterDataList.get(0);
        Assertions.assertThat(foundSskuMasterData.getSupplierId()).isEqualTo(supplierId);
        Assertions.assertThat(foundSskuMasterData.getShopSku()).isEqualTo(shopSku);
        Assertions.assertThat(foundSskuMasterData.getHasMeasurement()).isTrue();
        Assertions.assertThat(foundSskuMasterData.getLastMeasurementTs()).isEqualTo(measurementTs);
    }

    @Test
    public void whenSaveReturnResultAboutAllRecords() {
        List<MasterData> testData = TestDataUtils.generateSskuMsterData(DATA_COUNT, random);
        storeSuppliersFromMasterData(testData);
        MasterDataProto.SaveSskuMasterDataRequest request = createSaveRequest(testData);

        MasterDataProto.SaveSskuMasterDataResponse response = masterDataService.saveSskuMasterData(request);
        List<ShopSkuKey> keysInResponse = response.getResultsList().stream()
            .map(MasterDataProto.OperationInfo::getKey)
            .map(MbocBaseProtoConverter::protoToPojo)
            .collect(Collectors.toList());

        List<ShopSkuKey> keysInRequest = testData.stream()
            .map(MasterData::getShopSkuKey)
            .collect(Collectors.toList());

        Assertions.assertThat(keysInResponse)
            .containsExactlyInAnyOrderElementsOf(keysInRequest);
    }

    @Test
    public void whenSaveValidMasterDataDoInsert() {
        List<MasterData> testData = TestDataUtils.generateSskuMsterData(DATA_COUNT, random);
        storeSuppliersFromMasterData(testData);
        MasterDataProto.SaveSskuMasterDataRequest request = createSaveRequest(testData);

        MasterDataProto.SaveSskuMasterDataResponse response = masterDataService.saveSskuMasterData(request);

        Assertions.assertThat(
            response.getResultsList().stream()
                .map(MasterDataProto.OperationInfo::getStatus)
                .collect(Collectors.toSet())
        )
            .containsOnly(MasterDataProto.OperationStatus.OK);
    }

    @Test
    public void whenSaveMasterDataWithDuplicatedShopSkuKey() {
        List<MasterData> testData = TestDataUtils.generateSskuMsterData(DATA_COUNT, random);
        testData.add(testData.get(testData.size() - 1));
        storeSuppliersFromMasterData(testData, MdmSupplierType.THIRD_PARTY, null);
        MasterDataProto.SaveSskuMasterDataRequest request = createSaveRequest(testData);

        MasterDataProto.SaveSskuMasterDataResponse response = masterDataService.saveSskuMasterData(request);

        Assertions.assertThat(
            response.getResultsList().stream()
                .map(MasterDataProto.OperationInfo::getStatus)
                .collect(Collectors.toSet())
        )
            .containsOnly(MasterDataProto.OperationStatus.OK);
    }

    @Test
    public void whenValidateOnlyDoNotInsert() {
        List<MasterData> testData = TestDataUtils.generateSskuMsterData(DATA_COUNT, random);
        storeSuppliersFromMasterData(testData);
        MasterDataProto.SaveSskuMasterDataRequest request = createSaveRequest(testData, true);

        MasterDataProto.SaveSskuMasterDataResponse response = masterDataService.saveSskuMasterData(request);

        Assertions.assertThat(
            response.getResultsList().stream()
                .map(MasterDataProto.OperationInfo::getStatus)
                .collect(Collectors.toSet())
        )
            .containsOnly(MasterDataProto.OperationStatus.OK);

        var repositoryData = silverSskuRepository.findAll();
        Assertions.assertThat(repositoryData).isEmpty();
        assertNoSskuEnqueued();
    }

    @Test
    public void whenSaveValidMasterDataDoUpdate() {
        // generate data
        List<MasterData> testData = TestDataUtils.generateSskuMsterData(DATA_COUNT, random);
        storeSuppliersFromMasterData(testData);
        MasterDataProto.SaveSskuMasterDataRequest request = createSaveRequest(testData);
        MasterDataProto.SaveSskuMasterDataResponse response = masterDataService.saveSskuMasterData(request);
        Assertions.assertThat(
            response.getResultsList().stream()
                .map(MasterDataProto.OperationInfo::getStatus)
                .collect(Collectors.toSet())
        )
            .containsOnly(MasterDataProto.OperationStatus.OK);

        // update data
        List<MasterData> updateTestData = TestDataUtils.generateSskuMsterData(DATA_COUNT, random);
        for (int i = 0; i < DATA_COUNT; i++) {
            updateTestData.get(i).setSupplierId(testData.get(i).getSupplierId());
            updateTestData.get(i).setShopSku(testData.get(i).getShopSku());
        }
        request = createSaveRequest(updateTestData);
        response = masterDataService.saveSskuMasterData(request);

        Assertions.assertThat(
            response.getResultsList().stream()
                .map(MasterDataProto.OperationInfo::getStatus)
                .collect(Collectors.toSet())
        )
            .containsOnly(MasterDataProto.OperationStatus.OK);
    }

    @Test
    public void whenSaveInvalidMasterDataUpdateAndEnqueue() {
        List<MasterData> validTestData = TestDataUtils.generateSskuMsterData(DATA_COUNT, random);
        storeSuppliersFromMasterData(validTestData, MdmSupplierType.THIRD_PARTY);
        MasterDataProto.SaveSskuMasterDataRequest request = createSaveRequest(validTestData);
        MasterDataProto.SaveSskuMasterDataResponse response = masterDataService.saveSskuMasterData(request);

        Assertions.assertThat(
            response.getResultsList().stream()
                .map(MasterDataProto.OperationInfo::getStatus)
                .collect(Collectors.toSet())
        )
            .containsOnly(MasterDataProto.OperationStatus.OK);

        List<MasterData> invalidTestData = TestDataUtils.generateSskuMsterData(DATA_COUNT, random);
        for (int i = 0; i < DATA_COUNT; i++) {
            invalidTestData.get(i).setSupplierId(validTestData.get(i).getSupplierId());
            invalidTestData.get(i).setShopSku(validTestData.get(i).getShopSku());
            //Put invalid value
            invalidTestData.get(i).setTransportUnitSize(TransportUnitBlockValidator.TRANSPORT_UNIT_SIZE_MAX + 1);
        }

        request = createSaveRequest(invalidTestData);
        response = masterDataService.saveSskuMasterData(request);

        Assertions.assertThat(
            response.getResultsList().stream()
                .map(MasterDataProto.OperationInfo::getStatus)
                .collect(Collectors.toSet())
        )
            .containsOnly(MasterDataProto.OperationStatus.VALIDATION_ERROR);
    }

    @Test
    public void whenSaveInvalidMasterDataInsertAndEnqueue() {
        List<MasterData> invalidTestData = TestDataUtils.generateSskuMsterData(DATA_COUNT, random).stream()
            .peek(md -> md.setTransportUnitSize(TransportUnitBlockValidator.TRANSPORT_UNIT_SIZE_MAX + 1))
            .collect(Collectors.toList());
        storeSuppliersFromMasterData(invalidTestData);

        MasterDataProto.SaveSskuMasterDataRequest request = createSaveRequest(invalidTestData);
        MasterDataProto.SaveSskuMasterDataResponse response = masterDataService.saveSskuMasterData(request);

        Assertions.assertThat(
            response.getResultsList().stream()
                .map(MasterDataProto.OperationInfo::getStatus)
                .collect(Collectors.toSet())
        )
            .containsOnly(MasterDataProto.OperationStatus.VALIDATION_ERROR);
    }

    @Test
    @Ignore
    public void whenSaveWithoutValidationMappingUseMappingFromMboc() {
        categoryParamValueRepository.insert(
            createNumericCategoryParamValue(STRONG_RESTRICTION_CATEGORY_ID, KnownMdmParams.MIN_LIMIT_SHELF_LIFE, 1));
        categoryParamValueRepository.insert(
            createTimeUnitCategoryParamValue(STRONG_RESTRICTION_CATEGORY_ID, KnownMdmParams.MIN_LIMIT_SHELF_LIFE_UNIT,
                TimeInUnits.TimeUnit.DAY));
        categoryParamValueRepository.insert(
            createNumericCategoryParamValue(STRONG_RESTRICTION_CATEGORY_ID, KnownMdmParams.MAX_LIMIT_SHELF_LIFE, 2));
        categoryParamValueRepository.insert(
            createTimeUnitCategoryParamValue(STRONG_RESTRICTION_CATEGORY_ID, KnownMdmParams.MAX_LIMIT_SHELF_LIFE_UNIT,
                TimeInUnits.TimeUnit.DAY));

        List<MasterData> testData = TestDataUtils.generateSskuMsterData(DATA_COUNT, random);
        for (MasterData md : testData) {
            mboMappingsServiceMock.addMapping(MboMappings.ApprovedMappingInfo.newBuilder()
                .setSupplierId(md.getSupplierId())
                .setShopSku(md.getShopSku())
                .setMarketCategoryId(22)
                .setMarketSkuId(44)
                .build()
            );
            md.setShelfLife(3, TimeInUnits.TimeUnit.DAY);
        }

        MasterDataProto.SaveSskuMasterDataRequest request = createSaveRequest(testData);

        MasterDataProto.SaveSskuMasterDataResponse response = masterDataService.saveSskuMasterData(request);
        Assertions.assertThat(response.getResultsList()).extracting(
            r -> r.getStatus(),
            r -> r.getErrorsCount(),
            r -> r.getErrorsList().get(0).getMessage()
        )
            .containsOnly(
                Assertions.tuple(MasterDataProto.OperationStatus.VALIDATION_ERROR, 1,
                    "Значение '3' для колонки 'Срок годности' должно быть в диапазоне 1 - 2"
                )
            );

        assertSskuEnqueued(testData.stream().map(MasterData::getShopSkuKey).distinct().collect(Collectors.toList()));
    }

    @Test
    public void whenSavePartiallyInvalidMasterDataInsertAndEnqueueAll() {
        List<MasterData> validTestData = TestDataUtils.generateSskuMsterData(DATA_COUNT * 2, random);

        List<MasterData> invalidTestData = validTestData.stream()
            .skip(DATA_COUNT)
            .peek(md -> md.setTransportUnitSize(TransportUnitBlockValidator.TRANSPORT_UNIT_SIZE_MAX + 1))
            .collect(Collectors.toList());
        validTestData = validTestData.stream().limit(DATA_COUNT).collect(Collectors.toList());

        List<MasterData> allTestData = Lists.newArrayList(Iterables.concat(validTestData, invalidTestData));
        storeSuppliersFromMasterData(allTestData);

        MasterDataProto.SaveSskuMasterDataRequest request = createSaveRequest(allTestData);
        MasterDataProto.SaveSskuMasterDataResponse response = masterDataService.saveSskuMasterData(request);

        List<ShopSkuKey> okKeys = response.getResultsList().stream()
            .filter(oi -> oi.getStatus() == MasterDataProto.OperationStatus.OK)
            .map(MasterDataProto.OperationInfo::getKey)
            .map(MbocBaseProtoConverter::protoToPojo)
            .collect(Collectors.toList());

        List<ShopSkuKey> errorKeys = response.getResultsList().stream()
            .filter(oi -> oi.getStatus() == MasterDataProto.OperationStatus.VALIDATION_ERROR)
            .map(MasterDataProto.OperationInfo::getKey)
            .map(MbocBaseProtoConverter::protoToPojo)
            .collect(Collectors.toList());

        Assertions.assertThat(okKeys).containsExactlyInAnyOrderElementsOf(
            validTestData.stream().map(MasterData::getShopSkuKey).collect(Collectors.toList())
        );

        Assertions.assertThat(errorKeys).containsExactlyInAnyOrderElementsOf(
            invalidTestData.stream().map(MasterData::getShopSkuKey).collect(Collectors.toList())
        );

        Assertions.assertThat(
            response.getResultsList().stream()
                .map(MasterDataProto.OperationInfo::getStatus)
                .collect(Collectors.toSet())
        )
            .contains(MasterDataProto.OperationStatus.VALIDATION_ERROR, MasterDataProto.OperationStatus.OK);
    }

    @Test
    public void whenValidateOnlyDoNotEnqueueMskuToRefresh() {
        storageKeyValueService.putValue(MdmProperties.ENQUEUE_MSKU_FROM_MD_SERVICE_ENABLED_KEY, true);
        List<MasterData> testData = TestDataUtils.generateSskuMsterData(DATA_COUNT, random);
        storeSuppliersFromMasterData(testData);
        List<Long> mskuIds = generateMappingsAndReturnMsku(testData);
        MasterDataProto.SaveSskuMasterDataRequest request = createSaveRequest(testData, true);

        MasterDataProto.SaveSskuMasterDataResponse response = masterDataService.saveSskuMasterData(request);

        List<MdmMskuQueueInfo> queue = mskuToRefreshRepository.findAll();
        Assertions.assertThat(queue).isEmpty();

        assertNoSskuEnqueued();
    }

    @Test
    public void whenSaveMasterDataThenFilterWhiteSupplier() {
        List<MasterData> testDataBlue = TestDataUtils.generateSskuMsterData(DATA_COUNT, random);
        List<MasterData> testDataWhite = TestDataUtils.generateSskuMsterData(DATA_COUNT, random);
        storeSuppliersFromMasterData(testDataBlue);
        storeSuppliersFromMasterData(testDataWhite, MdmSupplierType.MARKET_SHOP);
        List<MasterData> testDataAll = new ArrayList<>(testDataBlue);
        testDataAll.addAll(testDataWhite);
        MasterDataProto.SaveSskuMasterDataRequest request = createSaveRequest(testDataAll);

        MasterDataProto.SaveSskuMasterDataResponse response = masterDataService.saveSskuMasterData(request);

        Assertions.assertThat(
            response.getResultsList().stream()
                .map(MasterDataProto.OperationInfo::getStatus)
                .collect(Collectors.toSet())
        )
            .containsOnly(MasterDataProto.OperationStatus.OK);
    }

    @Test
    public void whenSaveMasterDataWithWhiteThenReturnResultAboutAllRecords() {
        List<MasterData> testDataBlue = TestDataUtils.generateSskuMsterData(DATA_COUNT, random);
        List<MasterData> testDataWhite = TestDataUtils.generateSskuMsterData(DATA_COUNT, random);
        storeSuppliersFromMasterData(testDataBlue);
        storeSuppliersFromMasterData(testDataWhite, MdmSupplierType.MARKET_SHOP);
        List<MasterData> testDataAll = new ArrayList<>(testDataBlue);
        testDataAll.addAll(testDataWhite);
        MasterDataProto.SaveSskuMasterDataRequest request = createSaveRequest(testDataAll);

        MasterDataProto.SaveSskuMasterDataResponse response = masterDataService.saveSskuMasterData(request);

        List<ShopSkuKey> keysInResponse = response.getResultsList().stream()
            .map(MasterDataProto.OperationInfo::getKey)
            .map(MbocBaseProtoConverter::protoToPojo)
            .collect(Collectors.toList());

        List<ShopSkuKey> keysInRequest = testDataAll.stream()
            .map(MasterData::getShopSkuKey)
            .collect(Collectors.toList());

        Assertions.assertThat(keysInResponse)
            .containsExactlyInAnyOrderElementsOf(keysInRequest);
    }

    @Test
    public void whenSendLogbrokerEventsShouldSendEventsToLogbroker() {
        // Set custom batch size
        storageKeyValueService.putValue(MdmProperties.LB_TO_IRIS_DYNAMIC_BATCH_SIZE_KEY, DATA_COUNT);

        // Create two batches - successful and failing
        Set<ShopSkuKey> data = random.objects(ShopSkuKey.class, DATA_COUNT).collect(Collectors.toSet());
        Set<ShopSkuKey> dataToFail = random.objects(ShopSkuKey.class, DATA_COUNT).collect(Collectors.toSet());

        logbrokerProducerServiceMock.setSuccessFilter(logbrokerEvent -> {
            MdmIrisPayload.ItemBatch event = (MdmIrisPayload.ItemBatch) logbrokerEvent.getPayload();
            Assertions.assertThat(event.getSendTs()).isNotZero();
            List<ShopSkuKey> eventKeys = event.getItemList()
                .stream()
                .map(Item::getItemId)
                .map(id -> new ShopSkuKey((int) id.getSupplierId(), id.getShopSku()))
                .collect(Collectors.toList());
            return !dataToFail.containsAll(eventKeys);
        });

        MasterDataProto.SendSskuMappingUpdatedEventRequest.Builder request =
            MasterDataProto.SendSskuMappingUpdatedEventRequest.newBuilder();

        data.forEach(shopSkuKey -> request.addShopSkuKey(MdmCommon.ShopSkuKey.newBuilder()
            .setSupplierId(shopSkuKey.getSupplierId())
            .setShopSku(shopSkuKey.getShopSku())
            .build()));
        dataToFail.forEach(shopSkuKey -> request.addShopSkuKey(MdmCommon.ShopSkuKey.newBuilder()
            .setSupplierId(shopSkuKey.getSupplierId())
            .setShopSku(shopSkuKey.getShopSku())
            .build()));

        MasterDataProto.SendSskuMappingUpdatedEventResponse response =
            masterDataService.sendSskuMappingUpdatedEvent(request.build());

        Set<ShopSkuKey> success = new HashSet<>();
        Set<ShopSkuKey> fail = new HashSet<>();
        for (MasterDataProto.OperationInfo info : response.getResultList()) {
            ShopSkuKey key = MbocBaseProtoConverter.protoToPojo(info.getKey());
            if (info.getStatus() == MasterDataProto.OperationStatus.OK) {
                success.add(key);
            } else {
                fail.add(key);
            }
        }

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(response.getResultCount()).isEqualTo(data.size() + dataToFail.size());
            s.assertThat(success).isEqualTo(data);
            s.assertThat(fail).isEqualTo(dataToFail);
        });
    }

    @Test
    public void whenSearchManufacturerCountryShouldReturnNew() {
        List<MasterData> md1 = TestDataUtils.generateSskuMsterData(DATA_COUNT, random);
        List<MasterData> md2 = TestDataUtils.generateSskuMsterData(DATA_COUNT, random);
        List<MasterData> md3 = TestDataUtils.generateSskuMsterData(DATA_COUNT, random);

        masterDataRepository.insertBatch(md1);
        masterDataLogIdService.updateModifiedSequence(DATA_COUNT);
        long seqId = masterDataLogIdService.getLastModifiedSequenceId();
        int batchSize = DATA_COUNT / 2;

        masterDataRepository.insertBatch(md2);
        masterDataLogIdService.updateModifiedSequence(DATA_COUNT);
        masterDataRepository.insertBatch(md3);

        var request = MasterDataProto.SearchManufacturerCountryRequest.newBuilder();
        request.setFromSequenceId(seqId);
        request.setCount(batchSize);

        long expectedLastSeqId = seqId + batchSize;

        var response = masterDataService.searchManufacturerCountry(request.build());
        Assertions.assertThat(response.getManufacturerCountryInfoCount()).isEqualTo(batchSize);

        Map<ShopSkuKey, MasterData> masterDataMap = md2.stream()
            .collect(Collectors.toMap(MasterData::getShopSkuKey, Function.identity()));

        Assertions.assertThat(response.getLastSequenceId()).isEqualTo(expectedLastSeqId);
        for (int i = 0; i < response.getManufacturerCountryInfoCount(); i++) {
            var countryInfo = response.getManufacturerCountryInfo(i);
            ShopSkuKey key = MbocBaseProtoConverter.protoToPojo(countryInfo.getShopSkuKey());
            List<String> countries = countryInfo.getManufacturerCountryList()
                .stream()
                .map(m -> m.getRuName())
                .collect(Collectors.toList());

            Assertions.assertThat(masterDataMap).containsKey(key);

            MasterData masterData = masterDataMap.get(key);
            Assertions.assertThat(countries).isEqualTo(masterData.getManufacturerCountries());
        }
    }

    private List<MasterData> storeMasterDataWithSuppliers(Collection<MasterData> masterData) {
        return storeMasterDataWithSuppliers(masterData, MdmSupplierType.THIRD_PARTY);
    }

    private List<MasterData> storeMasterDataWithSuppliers(Collection<MasterData> masterData,
                                                          MdmSupplierType supplierType) {
        return storeMasterDataWithSuppliers(masterData, supplierType, null);
    }


    private List<MasterData> storeMasterDataWithSuppliers(Collection<MasterData> masterData,
                                                          MdmSupplierType supplierType,
                                                          List<MdmSupplierSalesModel> salesModels) {

        storeSuppliersFromMasterData(masterData, supplierType, salesModels);
        masterDataRepository.insertBatch(masterData);
        return masterDataRepository.findByShopSkuKeys(
            masterData.stream().map(MasterData::getShopSkuKey).collect(Collectors.toList())
        );
    }

    private void storeSuppliersFromMasterData(Collection<MasterData> masterData) {
        storeSuppliersFromMasterData(masterData, MdmSupplierType.THIRD_PARTY);
    }

    private void storeSuppliersFromMasterData(Collection<MasterData> masterData, MdmSupplierType supplierType) {
        storeMasterDataWithSuppliers(masterData, supplierType);
    }

    private void storeSuppliersFromMasterData(Collection<MasterData> masterData,
                                              MdmSupplierType supplierType,
                                              List<MdmSupplierSalesModel> salesModels) {
        Set<Integer> supplierIds = masterData.stream().map(MasterData::getSupplierId).collect(Collectors.toSet());
        List<MdmSupplier> suppliers = supplierIds.stream().map(supplierId -> {
            MdmSupplier supplier = random.nextObject(MdmSupplier.class);
            supplier.setId(supplierId);
            supplier.setBusinessId(null);
            supplier.setType(supplierType);
            return supplier;
        }).collect(Collectors.toList());
        if (salesModels != null) {
            for (MdmSupplier supplier : suppliers) {
                supplier.setSalesModels(salesModels);
            }
        }
        supplierRepository.insertBatch(suppliers);
        mdmSupplierCachingService.refresh();
    }

    private MasterDataProto.SaveSskuMasterDataRequest createSaveRequest(List<MasterData> testData) {
        return createSaveRequest(testData, true);
    }

    private MasterDataProto.SaveSskuMasterDataRequest createSaveRequest(
        List<MasterData> testData,
        boolean validateOnly
    ) {
        MasterDataProto.SaveSskuMasterDataRequest.Builder result =
            MasterDataProto.SaveSskuMasterDataRequest.newBuilder();

        result.setValidateOnly(validateOnly);

        result.addAllSskuMasterData(testData.stream()
            .map(MbocBaseProtoConverter::pojoToProto)
            .collect(Collectors.toList())
        );

        return result.build();
    }

    private MasterDataProto.SearchSskuMasterDataRequest createSearchRequest(Collection<ShopSkuKey> pojoKeys) {
        Collection<MdmCommon.ShopSkuKey> protoKeys =
            MbocBaseProtoConverter.pojoToProto(pojoKeys);

        MasterDataProto.SearchSskuMasterDataRequest result = MasterDataProto.SearchSskuMasterDataRequest.newBuilder()
            .addAllShopSkuKeys(protoKeys)
            .build();

        return result;
    }

    private CategoryParamValue createNumericCategoryParamValue(long categoryId, long paramId, int value) {
        CategoryParamValue paramValue = new CategoryParamValue();
        paramValue.setCategoryId(categoryId)
            .setMdmParamId(paramId)
            .setNumeric(BigDecimal.valueOf(value));
        return paramValue;
    }

    private CategoryParamValue createTimeUnitCategoryParamValue(long categoryId, long paramId,
                                                                TimeInUnits.TimeUnit timeUnit) {
        CategoryParamValue paramValue = new CategoryParamValue();
        Long timeUnitId = KnownMdmParams.TIME_UNITS_OPTIONS.inverse().get(timeUnit);
        paramValue.setCategoryId(categoryId)
            .setMdmParamId(paramId)
            .setOption(new MdmParamOption().setId(timeUnitId));
        return paramValue;
    }

    private List<Long> generateMappingsAndReturnMsku(List<MasterData> masterData) {
        List<MappingCacheDao> mc = masterData.stream()
            .map(md -> new MappingCacheDao()
                .setCategoryId(1)
                .setMskuId(1L + random.nextInt(3))
                .setShopSkuKey(md.getShopSkuKey())
            ).collect(Collectors.toList());
        mappingsCacheRepository.insertOrUpdateAll(mc);
        return mc.stream().map(MappingCacheDao::getMskuId).distinct().collect(Collectors.toList());
    }

    private void assertNoSskuEnqueued() {
        Assertions.assertThat(sskuToRefreshRepository.getUnprocessedItemsCount()).isZero();
    }

    private void assertSskuEnqueued(List<ShopSkuKey> sskus) {
        var enqueuedSskus = sskuToRefreshRepository.getUnprocessedBatch(Integer.MAX_VALUE);
        Assertions.assertThat(enqueuedSskus.stream().map(SskuToRefreshInfo::getEntityKey).collect(Collectors.toList()))
            .containsExactlyInAnyOrderElementsOf(sskus);
        Assertions.assertThat(enqueuedSskus.stream().flatMap(info -> info.getOnlyReasons().stream())
            .allMatch(reason -> reason == MdmEnqueueReason.CHANGED_SSKU_SILVER_DATA)).isTrue();
    }

}
