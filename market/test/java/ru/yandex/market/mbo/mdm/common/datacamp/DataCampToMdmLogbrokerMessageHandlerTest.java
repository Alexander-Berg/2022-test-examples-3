package ru.yandex.market.mbo.mdm.common.datacamp;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import Market.DataCamp.API.DatacampMessageOuterClass;
import Market.DataCamp.DataCampContentStatus;
import Market.DataCamp.DataCampOfferMapping;
import Market.DataCamp.DataCampOfferMeta;
import com.google.protobuf.Timestamp;
import org.assertj.core.api.Assertions;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.kikimr.persqueue.consumer.transport.message.inbound.data.MessageBatch;
import ru.yandex.market.mbo.mdm.common.infrastructure.logbroker.DataCampToMdmLogbrokerMessageHandler;
import ru.yandex.market.mbo.mdm.common.infrastructure.logbroker.DcMappingsToMdmLogbrokerMessageHandler;
import ru.yandex.market.mbo.mdm.common.masterdata.model.ImpersonalSourceId;
import ru.yandex.market.mbo.mdm.common.masterdata.model.MappingCacheDao;
import ru.yandex.market.mbo.mdm.common.masterdata.model.ServiceOfferMigrationInfo;
import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.MasterDataSourceType;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.SskuParamValue;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.SskuSilverParamValue;
import ru.yandex.market.mbo.mdm.common.masterdata.model.queue.MdmEnqueueReason;
import ru.yandex.market.mbo.mdm.common.masterdata.model.queue.MdmMskuQueueInfo;
import ru.yandex.market.mbo.mdm.common.masterdata.model.queue.SskuToRefreshInfo;
import ru.yandex.market.mbo.mdm.common.masterdata.model.supplier.MdmSupplier;
import ru.yandex.market.mbo.mdm.common.masterdata.model.supplier.MdmSupplierSalesModel;
import ru.yandex.market.mbo.mdm.common.masterdata.model.supplier.MdmSupplierType;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.MappingsCacheRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.MdmSupplierRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.ServiceOfferMigrationRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.SskuExistenceRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.param.SilverSskuRepositoryParamValueImpl;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.queue.MdmQueuesManager;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.queue.MskuToRefreshRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.queue.SskuToRefreshRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.services.business.MdmSskuGroupManager;
import ru.yandex.market.mbo.mdm.common.masterdata.services.business.MdmSupplierCachingService;
import ru.yandex.market.mbo.mdm.common.masterdata.services.iris.BeruId;
import ru.yandex.market.mbo.mdm.common.masterdata.services.iris.BeruIdMock;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.CommonSskuFromDataCampConverter;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownMdmParams;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.TestMdmParamUtils;
import ru.yandex.market.mbo.mdm.common.service.mapping.MappingsUpdateService;
import ru.yandex.market.mbo.mdm.common.utils.MdmBaseDbTestClass;
import ru.yandex.market.mbo.mdm.common.utils.MdmQueueInfoBaseUtils;
import ru.yandex.market.mbo.storage.StorageKeyValueService;
import ru.yandex.market.mboc.common.offers.model.ShopSkuKey;
import ru.yandex.market.mboc.common.utils.MdmProperties;

/**
 * @author dmserebr
 * @date 19/01/2021
 */
public class DataCampToMdmLogbrokerMessageHandlerTest extends MdmBaseDbTestClass {
    private static final int BUSINESS_ID = 123;
    private static final int ANOTHER_BUSINESS_ID = 789;
    private static final int SUPPLIER_ID_1 = 1234;
    private static final int SUPPLIER_ID_2 = 2345;
    private static final int WHITE_SUPPLIER_ID = 5678;
    private static final int WHITE_DBS_SUPPLIER_ID = 6789;
    private static final String SHOP_SKU = "shapoklyak";
    private static final long MSKU_ID = 12392;
    private static final int CATEGORY_ID = 1;
    private static final long MASTER_DATA_VERSION = 5L;
    private static final int UNPROCESSED_BATCH_SIZE = 100;

    private static final DataCampOfferMeta.UpdateMeta META = DataCampOfferMeta.UpdateMeta.newBuilder()
        .setTimestamp(Timestamp.newBuilder().setSeconds(9000).build()).build();

    @Autowired
    private StorageKeyValueService keyValueService;
    @Autowired
    private CommonSskuFromDataCampConverter commonSskuFromDatacampConverter;
    @Autowired
    private MdmQueuesManager mdmQueuesManager;
    @Autowired
    private MdmSskuGroupManager mdmSskuGroupManager;
    @Autowired
    private MappingsUpdateService mappingsUpdateService;
    @Autowired
    private MappingsCacheRepository mappingsCacheRepository;
    @Autowired
    private MdmSupplierRepository mdmSupplierRepository;
    @Autowired
    private SilverSskuRepositoryParamValueImpl silverSskuRepository;
    @Autowired
    private ServiceOfferMigrationRepository serviceOfferMigrationRepository;
    @Autowired
    private ServiceOfferMigrationService serviceOfferMigrationService;
    @Autowired
    private SskuToRefreshRepository sskuQueue;
    @Autowired
    private MskuToRefreshRepository mskuQueue;
    @Autowired
    private SskuExistenceRepository sskuExistenceRepository;
    @Autowired
    private MdmSupplierCachingService mdmSupplierCachingService;
    @Autowired
    private DatacampOffersFiltrator datacampOffersFiltrator;
    @Autowired
    private SupplierSilverSskuTransformationService supplierSilverSskuTransformationService;

    private DataCampToMdmLogbrokerMessageHandler logbrokerMessageHandler;
    private DcMappingsToMdmLogbrokerMessageHandler dcMappingsMessageHandler;
    private BeruId beruId;
    private SilverSskuRepositoryParamValueImpl sskuSilverParamValueRepoSpy;
    private DatacampOffersImporterImpl datacampOffersImporterSpy;

    @Before
    public void before() {
        keyValueService.putValue(MdmProperties.IMPORT_MAPPINGS_FROM_DATACAMP_ENABLED, true);
        keyValueService.putValue(MdmProperties.BUSINESS_MERGE_ENABLED_KEY, true);
        beruId = new BeruIdMock();
        silverSskuRepository.setDeleteBeforeSave(true);
        sskuSilverParamValueRepoSpy = Mockito.spy(silverSskuRepository);
        sskuSilverParamValueRepoSpy.setDeleteBeforeSave(true);

        datacampOffersImporterSpy = Mockito.spy(
            new DatacampOffersImporterImpl(
                keyValueService,
                mdmQueuesManager,
                mdmSskuGroupManager,
                mappingsUpdateService,
                serviceOfferMigrationService,
                datacampOffersFiltrator,
                supplierSilverSskuTransformationService));

        logbrokerMessageHandler = new DataCampToMdmLogbrokerMessageHandler(keyValueService,
            commonSskuFromDatacampConverter,
            datacampOffersImporterSpy,
            mdmQueuesManager);

        dcMappingsMessageHandler = new DcMappingsToMdmLogbrokerMessageHandler(keyValueService,
            commonSskuFromDatacampConverter,
            datacampOffersImporterSpy,
            mdmQueuesManager);
        keyValueService.putValue(MdmProperties.DC_MAPPINGS_IMPORT_MODE, DcMappingsImportMode.BOTH_TOPICS);
        keyValueService.invalidateCache();
    }

    @After
    public void cleanup() {
        silverSskuRepository.setDeleteBeforeSave(false);
        sskuSilverParamValueRepoSpy.setDeleteBeforeSave(false);
    }

    @Test
    public void testImportMappings() {
        mdmSupplierRepository.insert(new MdmSupplier().setId(BUSINESS_ID).setType(MdmSupplierType.BUSINESS));
        mdmSupplierRepository.insert(new MdmSupplier().setId(SUPPLIER_ID_1).setType(MdmSupplierType.THIRD_PARTY)
            .setBusinessId(BUSINESS_ID).setBusinessEnabled(true));
        mdmSupplierCachingService.refresh();

        sskuExistenceRepository.markExistence(List.of(new ShopSkuKey(SUPPLIER_ID_1, SHOP_SKU)), true);

        MessageBatch testMessageBatch = DatacampOffersTestUtil.createTestMessage(
            DatacampOffersTestUtil.createUnitedOffer(
                DataCampContentStatus.MappingConfidence.MAPPING_CONFIDENCE_CONTENT, META));

        logbrokerMessageHandler.process(DatacampOffersTestUtil.getDataCampMessages(testMessageBatch));

        Map<ShopSkuKey, MappingCacheDao> mappings = mappingsCacheRepository.findAll().stream()
            .collect(Collectors.toMap(MappingCacheDao::getShopSkuKey, Function.identity()));

        // business has 1 service supplier
        Assertions.assertThat(mappings).hasSize(2);
        Assertions.assertThat(mappings.get(new ShopSkuKey(BUSINESS_ID, SHOP_SKU))).isEqualTo(
            new MappingCacheDao().setSupplierId(BUSINESS_ID).setShopSku(SHOP_SKU)
                .setMskuId(MSKU_ID).setCategoryId(CATEGORY_ID));
        Assertions.assertThat(mappings.get(new ShopSkuKey(SUPPLIER_ID_1, SHOP_SKU))).isEqualTo(
            new MappingCacheDao().setSupplierId(SUPPLIER_ID_1).setShopSku(SHOP_SKU)
                .setMskuId(MSKU_ID).setCategoryId(CATEGORY_ID));
    }

    @Test
    public void testImportMappingsDisabledInSeparateMode() {
        keyValueService.putValue(MdmProperties.DC_MAPPINGS_IMPORT_MODE, DcMappingsImportMode.SEPARATE_TOPICS);
        keyValueService.invalidateCache();
        mdmSupplierRepository.insert(new MdmSupplier().setId(BUSINESS_ID).setType(MdmSupplierType.BUSINESS));
        mdmSupplierRepository.insert(new MdmSupplier().setId(SUPPLIER_ID_1).setType(MdmSupplierType.THIRD_PARTY)
            .setBusinessId(BUSINESS_ID).setBusinessEnabled(true));
        mdmSupplierCachingService.refresh();

        sskuExistenceRepository.markExistence(List.of(new ShopSkuKey(SUPPLIER_ID_1, SHOP_SKU)), true);

        MessageBatch testMessageBatch = DatacampOffersTestUtil.createTestMessage(
            DatacampOffersTestUtil.createUnitedOffer(
                DataCampContentStatus.MappingConfidence.MAPPING_CONFIDENCE_CONTENT, META));

        logbrokerMessageHandler.process(DatacampOffersTestUtil.getDataCampMessages(testMessageBatch));

        Map<ShopSkuKey, MappingCacheDao> mappings = mappingsCacheRepository.findAll().stream()
            .collect(Collectors.toMap(MappingCacheDao::getShopSkuKey, Function.identity()));

        Assertions.assertThat(mappings).hasSize(0);

        // Now use separate importer
        dcMappingsMessageHandler.process(DatacampOffersTestUtil.getDataCampMessages(testMessageBatch));
        mappings = mappingsCacheRepository.findAll().stream()
            .collect(Collectors.toMap(MappingCacheDao::getShopSkuKey, Function.identity()));
        Assertions.assertThat(mappings).hasSize(2);
        Assertions.assertThat(mappings.get(new ShopSkuKey(BUSINESS_ID, SHOP_SKU))).isEqualTo(
            new MappingCacheDao().setSupplierId(BUSINESS_ID).setShopSku(SHOP_SKU)
                .setMskuId(MSKU_ID).setCategoryId(CATEGORY_ID));
        Assertions.assertThat(mappings.get(new ShopSkuKey(SUPPLIER_ID_1, SHOP_SKU))).isEqualTo(
            new MappingCacheDao().setSupplierId(SUPPLIER_ID_1).setShopSku(SHOP_SKU)
                .setMskuId(MSKU_ID).setCategoryId(CATEGORY_ID));
    }

    @Test
    public void testImportMappingsDeletion() {
        mdmSupplierRepository.insert(new MdmSupplier().setId(BUSINESS_ID).setType(MdmSupplierType.BUSINESS));
        mdmSupplierRepository.insert(new MdmSupplier().setId(SUPPLIER_ID_1).setType(MdmSupplierType.THIRD_PARTY)
            .setBusinessId(BUSINESS_ID).setBusinessEnabled(true));
        mdmSupplierCachingService.refresh();
        sskuExistenceRepository.markExistence(List.of(new ShopSkuKey(SUPPLIER_ID_1, SHOP_SKU)), true);
        // add already existed mappings
        var serviceMapping = mappingCacheDao(new ShopSkuKey(SUPPLIER_ID_1, SHOP_SKU), 0L);
        var businessMapping = mappingCacheDao(new ShopSkuKey(BUSINESS_ID, SHOP_SKU), 0L);
        mappingsCacheRepository.insertOrUpdateAll(List.of(serviceMapping, businessMapping));

        // import mapping without mskuId, it means that mapping is deleted
        DataCampOfferMapping.Mapping.Builder mappingBuilder = DataCampOfferMapping.Mapping.newBuilder()
            .setMarketCategoryId(CATEGORY_ID)
            .setMeta(META);
        DataCampOfferMapping.ContentBinding contentBinding = DataCampOfferMapping.ContentBinding.newBuilder()
            .setApproved(mappingBuilder.build())
            .build();

        MessageBatch testMessageBatch = DatacampOffersTestUtil.createTestMessage(
            DatacampOffersTestUtil.createUnitedOfferWithTunedVersions(
                META,
                MASTER_DATA_VERSION,
                MASTER_DATA_VERSION,
                contentBinding,
                DatacampOffersTestUtil.createServiceOfferBuilder(BUSINESS_ID, SUPPLIER_ID_1, true)));

        logbrokerMessageHandler.process(DatacampOffersTestUtil.getDataCampMessages(testMessageBatch));

        Map<ShopSkuKey, MappingCacheDao> mappings = mappingsCacheRepository.findAll().stream()
            .collect(Collectors.toMap(MappingCacheDao::getShopSkuKey, Function.identity()));

        Assertions.assertThat(mappings).hasSize(0);
    }

    @Test
    public void testShouldImportApprovedMappingsForAnyConfidence() {
        mdmSupplierRepository.insert(new MdmSupplier().setId(BUSINESS_ID).setType(MdmSupplierType.BUSINESS));
        mdmSupplierRepository.insert(new MdmSupplier().setId(SUPPLIER_ID_1).setType(MdmSupplierType.THIRD_PARTY)
            .setBusinessId(BUSINESS_ID).setBusinessEnabled(true));
        mdmSupplierCachingService.refresh();
        sskuExistenceRepository.markExistence(List.of(new ShopSkuKey(SUPPLIER_ID_1, SHOP_SKU)), true);

        MessageBatch testMessageBatch = DatacampOffersTestUtil.createTestMessage(
            DatacampOffersTestUtil.createUnitedOffer(
                DataCampContentStatus.MappingConfidence.MAPPING_CONFIDENCE_PARTNER, META));

        logbrokerMessageHandler.process(DatacampOffersTestUtil.getDataCampMessages(testMessageBatch));

        Assertions.assertThat(mappingsCacheRepository.findAll()).isNotEmpty();
    }

    @Test
    public void testCloneMappingsOnServiceEvenIfEmptySupplierCache() {
        MessageBatch testMessageBatch = DatacampOffersTestUtil.createTestMessage(
            DatacampOffersTestUtil.createUnitedOffer(DataCampContentStatus.MappingConfidence.MAPPING_CONFIDENCE_CONTENT,
                META, DatacampOffersTestUtil.createServiceOfferBuilder(BUSINESS_ID, SUPPLIER_ID_1)));

        logbrokerMessageHandler.process(DatacampOffersTestUtil.getDataCampMessages(testMessageBatch));

        Map<ShopSkuKey, MappingCacheDao> mappings = mappingsCacheRepository.findAll().stream()
            .collect(Collectors.toMap(MappingCacheDao::getShopSkuKey, Function.identity()));

        // business has 1 service supplier
        Assertions.assertThat(mappings).hasSize(2);
        Assertions.assertThat(mappings.get(new ShopSkuKey(BUSINESS_ID, SHOP_SKU))).isEqualTo(
            new MappingCacheDao().setSupplierId(BUSINESS_ID).setShopSku(SHOP_SKU)
                .setMskuId(MSKU_ID).setCategoryId(CATEGORY_ID));
        Assertions.assertThat(mappings.get(new ShopSkuKey(SUPPLIER_ID_1, SHOP_SKU))).isEqualTo(
            new MappingCacheDao().setSupplierId(SUPPLIER_ID_1).setShopSku(SHOP_SKU)
                .setMskuId(MSKU_ID).setCategoryId(CATEGORY_ID));
    }

    @Test
    public void testSkipEmptyOfferIfSilverDataDoesNotExists() {
        mdmSupplierRepository.insert(new MdmSupplier().setId(BUSINESS_ID).setType(MdmSupplierType.BUSINESS));
        mdmSupplierCachingService.refresh();

        MessageBatch testMessageBatch = DatacampOffersTestUtil.createTestMessage(
            DatacampOffersTestUtil.createUnitedOfferWithEmptySilverData(
                DataCampContentStatus.MappingConfidence.MAPPING_CONFIDENCE_CONTENT, META));

        logbrokerMessageHandler.process(DatacampOffersTestUtil.getDataCampMessages(testMessageBatch));

        Map<ShopSkuKey, List<SskuSilverParamValue>> silvers = silverSskuRepository.findAll()
            .stream()
            .collect(Collectors.groupingBy(SskuParamValue::getShopSkuKey));
        Assertions.assertThat(silvers.size()).isEqualTo(0);
    }

    @Test
    public void testMasterDataVersionFromBaseAndServiceOffersIsSaved() {
        keyValueService.putValue(MdmProperties.CALCULATE_MD_PARTNER_VERDICTS_ENABLED_KEY, true);
        keyValueService.invalidateCache();

        ShopSkuKey businessKey = new ShopSkuKey(BUSINESS_ID, SHOP_SKU);
        ShopSkuKey serviceKey = new ShopSkuKey(SUPPLIER_ID_1, SHOP_SKU);

        mdmSupplierRepository.insert(new MdmSupplier().setId(BUSINESS_ID).setType(MdmSupplierType.BUSINESS));
        mdmSupplierRepository.insert(new MdmSupplier().setId(SUPPLIER_ID_1).setType(MdmSupplierType.THIRD_PARTY)
            .setBusinessId(BUSINESS_ID).setBusinessEnabled(true));
        mdmSupplierCachingService.refresh();

        MessageBatch testMessageBatch = DatacampOffersTestUtil.createTestMessage(
            DatacampOffersTestUtil.createUnitedOffer(
                DataCampContentStatus.MappingConfidence.MAPPING_CONFIDENCE_CONTENT, META,
                DatacampOffersTestUtil.createServiceOfferBuilder(BUSINESS_ID, SUPPLIER_ID_1)));

        logbrokerMessageHandler.process(DatacampOffersTestUtil.getDataCampMessages(testMessageBatch));

        Map<ShopSkuKey, List<SskuSilverParamValue>> silvers = silverSskuRepository.findAll()
            .stream()
            .collect(Collectors.groupingBy(SskuParamValue::getShopSkuKey));
        Assertions.assertThat(silvers).isNotEmpty();
        Assertions.assertThat(silvers.get(businessKey).get(0).getDatacampMasterDataVersion())
            .isEqualTo(MASTER_DATA_VERSION);
        Assertions.assertThat(silvers.get(serviceKey).get(0).getDatacampMasterDataVersion())
            .isEqualTo(MASTER_DATA_VERSION);
    }

    @Test
    public void testImportCorrectData() {
        ShopSkuKey businessKey = new ShopSkuKey(BUSINESS_ID, SHOP_SKU);
        ShopSkuKey serviceKey = new ShopSkuKey(SUPPLIER_ID_1, SHOP_SKU);

        mdmSupplierRepository.insert(new MdmSupplier().setId(BUSINESS_ID).setType(MdmSupplierType.BUSINESS));
        mdmSupplierRepository.insert(new MdmSupplier().setId(SUPPLIER_ID_1).setType(MdmSupplierType.THIRD_PARTY)
            .setBusinessId(BUSINESS_ID).setBusinessEnabled(true));
        mdmSupplierCachingService.refresh();

        MessageBatch testMessageBatch = DatacampOffersTestUtil.createTestMessage(
            DatacampOffersTestUtil.createUnitedOffer(
                DataCampContentStatus.MappingConfidence.MAPPING_CONFIDENCE_CONTENT, META,
                DatacampOffersTestUtil.createServiceOfferBuilder(BUSINESS_ID, SUPPLIER_ID_1)));
        logbrokerMessageHandler.process(DatacampOffersTestUtil.getDataCampMessages(testMessageBatch));

        Map<ShopSkuKey, List<SskuSilverParamValue>> silvers = silverSskuRepository.findAll()
            .stream()
            .collect(Collectors.groupingBy(SskuParamValue::getShopSkuKey));
        assertThatVersionsAreCorrect(silvers, Map.of(businessKey, MASTER_DATA_VERSION, serviceKey,
            MASTER_DATA_VERSION));

        testMessageBatch = DatacampOffersTestUtil.createTestMessage(
            DatacampOffersTestUtil.createUnitedOfferWithTunedServiceVersions(
                META, MASTER_DATA_VERSION + 1,
                DatacampOffersTestUtil.createServiceOfferBuilder(BUSINESS_ID, SUPPLIER_ID_1)));
        logbrokerMessageHandler.process(DatacampOffersTestUtil.getDataCampMessages(testMessageBatch));

        silvers = silverSskuRepository.findAll()
            .stream()
            .collect(Collectors.groupingBy(SskuParamValue::getShopSkuKey));
        assertThatVersionsAreCorrect(silvers, Map.of(businessKey, MASTER_DATA_VERSION,
            serviceKey, MASTER_DATA_VERSION + 1));

        testMessageBatch = DatacampOffersTestUtil.createTestMessage(
            DatacampOffersTestUtil.createUnitedOfferWithTunedServiceVersions(
                META, MASTER_DATA_VERSION,
                DatacampOffersTestUtil.createServiceOfferBuilder(BUSINESS_ID, SUPPLIER_ID_1)));
        logbrokerMessageHandler.process(DatacampOffersTestUtil.getDataCampMessages(testMessageBatch));

        silvers = silverSskuRepository.findAll()
            .stream()
            .collect(Collectors.groupingBy(SskuParamValue::getShopSkuKey));
        assertThatVersionsAreCorrect(silvers, Map.of(businessKey, MASTER_DATA_VERSION,
            serviceKey, MASTER_DATA_VERSION + 1));
    }

    @Test
    public void testSaveNaked1PsOnImport() {
        ShopSkuKey businessKey = new ShopSkuKey(beruId.getBusinessId(), SHOP_SKU);

        mdmSupplierRepository.insert(new MdmSupplier().setId(beruId.getBusinessId()).setType(MdmSupplierType.BUSINESS));
        mdmSupplierRepository.insert(new MdmSupplier().setId(beruId.getId()).setType(MdmSupplierType.FIRST_PARTY)
            .setBusinessId(beruId.getBusinessId()).setBusinessEnabled(true));
        mdmSupplierCachingService.refresh();

        var unitedOffer = DatacampOffersTestUtil.createUnitedOffer(
            DataCampContentStatus.MappingConfidence.MAPPING_CONFIDENCE_CONTENT, META);

        // Заменим дефолтный бизИД на наш 1Ршный, а сервисов вообще не будет
        unitedOffer = unitedOffer.toBuilder().setBasic(
            unitedOffer.getBasic().toBuilder().setIdentifiers(
                unitedOffer.getBasic().getIdentifiers().toBuilder()
                    .setBusinessId(businessKey.getSupplierId())
                    .setOfferId(businessKey.getShopSku())
                    .build()
            ).build()
        ).build();

        // проставим необходимый флажок united_catalog
        unitedOffer = unitedOffer.toBuilder().setBasic(
            unitedOffer.getBasic().toBuilder().setStatus(
                unitedOffer.getBasic().getStatus().toBuilder()
                    .setUnitedCatalog(DataCampOfferMeta.Flag.newBuilder().setFlag(true).build())
                    .build()
            ).build()
        ).build();

        MessageBatch testMessageBatch = DatacampOffersTestUtil.createTestMessage(unitedOffer);
        logbrokerMessageHandler.process(DatacampOffersTestUtil.getDataCampMessages(testMessageBatch));

        Map<ShopSkuKey, List<SskuSilverParamValue>> silvers = silverSskuRepository.findAll()
            .stream()
            .collect(Collectors.groupingBy(SskuParamValue::getShopSkuKey));
        assertThatVersionsAreCorrect(silvers, Map.of(businessKey, MASTER_DATA_VERSION));
    }

    @Test
    public void testIgnoreNaked3PsOnImport() {
        ShopSkuKey businessKey = new ShopSkuKey(BUSINESS_ID, SHOP_SKU);

        mdmSupplierRepository.insert(new MdmSupplier().setId(BUSINESS_ID).setType(MdmSupplierType.BUSINESS));
        mdmSupplierRepository.insert(new MdmSupplier().setId(beruId.getId()).setType(MdmSupplierType.FIRST_PARTY)
            .setBusinessId(beruId.getBusinessId()).setBusinessEnabled(true));
        mdmSupplierCachingService.refresh();

        var unitedOffer = DatacampOffersTestUtil.createUnitedOffer(
            DataCampContentStatus.MappingConfidence.MAPPING_CONFIDENCE_CONTENT, META);

        // Проставим бизнес-ключ, а сервисов вообще не будет
        unitedOffer = unitedOffer.toBuilder().setBasic(
            unitedOffer.getBasic().toBuilder().setIdentifiers(
                unitedOffer.getBasic().getIdentifiers().toBuilder()
                    .setBusinessId(businessKey.getSupplierId())
                    .setOfferId(businessKey.getShopSku())
                    .build()
            ).build()
        ).build();

        MessageBatch testMessageBatch = DatacampOffersTestUtil.createTestMessage(unitedOffer);
        logbrokerMessageHandler.process(DatacampOffersTestUtil.getDataCampMessages(testMessageBatch));

        Assertions.assertThat(silverSskuRepository.findAll()).isEmpty();
    }

    @Test
    public void testMasterDataFromWhiteServiceOffersIsFiltered() {
        ShopSkuKey businessKey = new ShopSkuKey(BUSINESS_ID, SHOP_SKU);
        ShopSkuKey serviceKey = new ShopSkuKey(SUPPLIER_ID_1, SHOP_SKU);
        ShopSkuKey whiteServiceKey = new ShopSkuKey(WHITE_SUPPLIER_ID, SHOP_SKU);

        mdmSupplierRepository.insert(new MdmSupplier().setId(BUSINESS_ID).setType(MdmSupplierType.BUSINESS));
        mdmSupplierRepository.insert(new MdmSupplier().setId(SUPPLIER_ID_1).setType(MdmSupplierType.THIRD_PARTY)
            .setBusinessId(BUSINESS_ID).setBusinessEnabled(true));
        mdmSupplierRepository.insert(new MdmSupplier().setId(WHITE_SUPPLIER_ID).setType(MdmSupplierType.MARKET_SHOP)
            .setBusinessId(BUSINESS_ID).setBusinessEnabled(true));
        mdmSupplierCachingService.refresh();

        MessageBatch testMessageBatch = DatacampOffersTestUtil.createTestMessage(
            DatacampOffersTestUtil.createUnitedOffer(
                DataCampContentStatus.MappingConfidence.MAPPING_CONFIDENCE_CONTENT, META,
                DatacampOffersTestUtil.createServiceOfferBuilder(BUSINESS_ID, SUPPLIER_ID_1),
                DatacampOffersTestUtil.createServiceOfferBuilder(BUSINESS_ID, WHITE_SUPPLIER_ID)));

        logbrokerMessageHandler.process(DatacampOffersTestUtil.getDataCampMessages(testMessageBatch));

        Map<ShopSkuKey, List<SskuSilverParamValue>> silvers = silverSskuRepository.findAll()
            .stream()
            .collect(Collectors.groupingBy(SskuParamValue::getShopSkuKey));
        Assertions.assertThat(silvers).isNotEmpty();
        Assertions.assertThat(silvers.get(businessKey).get(0).getDatacampMasterDataVersion())
            .isEqualTo(MASTER_DATA_VERSION);
        Assertions.assertThat(silvers.get(serviceKey).get(0).getDatacampMasterDataVersion())
            .isEqualTo(MASTER_DATA_VERSION);
        Assertions.assertThat(silvers.get(whiteServiceKey)).isNull();
    }

    // In real world, offer with multiple services arrives as N messages. Each has 1 base offer and 1 service offer.
    @Test
    public void testMultiServiceOfferArrivesInParts() {
        keyValueService.invalidateCache();

        ShopSkuKey businessKey = new ShopSkuKey(BUSINESS_ID, SHOP_SKU);
        ShopSkuKey serviceKey1 = new ShopSkuKey(SUPPLIER_ID_1, SHOP_SKU);
        ShopSkuKey serviceKey2 = new ShopSkuKey(SUPPLIER_ID_2, SHOP_SKU);

        mdmSupplierRepository.insertBatch(
            new MdmSupplier().setId(BUSINESS_ID).setType(MdmSupplierType.BUSINESS),
            new MdmSupplier().setId(SUPPLIER_ID_1).setType(MdmSupplierType.THIRD_PARTY)
                .setBusinessId(BUSINESS_ID).setBusinessEnabled(true),
            new MdmSupplier().setId(SUPPLIER_ID_2).setType(MdmSupplierType.THIRD_PARTY)
                .setBusinessId(BUSINESS_ID).setBusinessEnabled(true));
        mdmSupplierCachingService.refresh();

        MessageBatch testMessageBatch = DatacampOffersTestUtil.createTestMessage(
            DatacampOffersTestUtil.createUnitedOffer(
                DataCampContentStatus.MappingConfidence.MAPPING_CONFIDENCE_CONTENT, META,
                DatacampOffersTestUtil.createServiceOfferBuilder(BUSINESS_ID, SUPPLIER_ID_1)),
            DatacampOffersTestUtil.createUnitedOffer(
                DataCampContentStatus.MappingConfidence.MAPPING_CONFIDENCE_CONTENT, META,
                DatacampOffersTestUtil.createServiceOfferBuilder(BUSINESS_ID, SUPPLIER_ID_2)));

        logbrokerMessageHandler.process(DatacampOffersTestUtil.getDataCampMessages(testMessageBatch));

        Map<ShopSkuKey, List<SskuSilverParamValue>> silvers = silverSskuRepository.findAll()
            .stream()
            .collect(Collectors.groupingBy(SskuParamValue::getShopSkuKey));

        Assertions.assertThat(silvers).hasSize(3);
        Assertions.assertThat(silvers.get(businessKey))
            .usingComparatorForElementFieldsWithNames(
                TestMdmParamUtils.MODIFICATION_INFO_COMPARATOR, "modificationInfo")
            .containsExactlyInAnyOrder(
                TestMdmParamUtils.createSskuSilverParamValue(
                    KnownMdmParams.DOCUMENT_REG_NUMBER, "documentRegNumber", "345",
                    MasterDataSourceType.SUPPLIER, ImpersonalSourceId.DATACAMP.name(), businessKey, MASTER_DATA_VERSION,
                    SskuSilverParamValue.SskuSilverTransportType.DATACAMP),
                TestMdmParamUtils.createSskuSilverParamValue(
                    KnownMdmParams.DATACAMP_MASTER_DATA_VERSION, "datacampMDVersion", MASTER_DATA_VERSION,
                    MasterDataSourceType.SUPPLIER, ImpersonalSourceId.DATACAMP.name(), businessKey, MASTER_DATA_VERSION,
                    SskuSilverParamValue.SskuSilverTransportType.DATACAMP));

        Assertions.assertThat(silvers.get(serviceKey1))
            .usingComparatorForElementFieldsWithNames(
                TestMdmParamUtils.MODIFICATION_INFO_COMPARATOR, "modificationInfo")
            .containsExactlyInAnyOrder(
                TestMdmParamUtils.createSskuSilverParamValue(
                    KnownMdmParams.TRANSPORT_UNIT_SIZE, "transportUnitSize", 12,
                    MasterDataSourceType.SUPPLIER, ImpersonalSourceId.DATACAMP.name(), serviceKey1, MASTER_DATA_VERSION,
                    SskuSilverParamValue.SskuSilverTransportType.DATACAMP),
                TestMdmParamUtils.createSskuSilverParamValue(
                    KnownMdmParams.SERVICE_EXISTS, "serviceExists", true,
                    MasterDataSourceType.SUPPLIER, ImpersonalSourceId.DATACAMP.name(), serviceKey1, MASTER_DATA_VERSION,
                    SskuSilverParamValue.SskuSilverTransportType.DATACAMP),
                TestMdmParamUtils.createSskuSilverParamValue(
                    KnownMdmParams.DATACAMP_MASTER_DATA_VERSION, "datacampMDVersion", MASTER_DATA_VERSION,
                    MasterDataSourceType.SUPPLIER, ImpersonalSourceId.DATACAMP.name(), serviceKey1, MASTER_DATA_VERSION,
                    SskuSilverParamValue.SskuSilverTransportType.DATACAMP));

        Assertions.assertThat(silvers.get(serviceKey2))
            .usingComparatorForElementFieldsWithNames(
                TestMdmParamUtils.MODIFICATION_INFO_COMPARATOR, "modificationInfo")
            .containsExactlyInAnyOrder(
                TestMdmParamUtils.createSskuSilverParamValue(
                    KnownMdmParams.TRANSPORT_UNIT_SIZE, "transportUnitSize", 12,
                    MasterDataSourceType.SUPPLIER, ImpersonalSourceId.DATACAMP.name(), serviceKey2, MASTER_DATA_VERSION,
                    SskuSilverParamValue.SskuSilverTransportType.DATACAMP),
                TestMdmParamUtils.createSskuSilverParamValue(
                    KnownMdmParams.SERVICE_EXISTS, "serviceExists", true,
                    MasterDataSourceType.SUPPLIER, ImpersonalSourceId.DATACAMP.name(), serviceKey2, MASTER_DATA_VERSION,
                    SskuSilverParamValue.SskuSilverTransportType.DATACAMP),
                TestMdmParamUtils.createSskuSilverParamValue(
                    KnownMdmParams.DATACAMP_MASTER_DATA_VERSION, "datacampMDVersion", MASTER_DATA_VERSION,
                    MasterDataSourceType.SUPPLIER, ImpersonalSourceId.DATACAMP.name(), serviceKey2, MASTER_DATA_VERSION,
                    SskuSilverParamValue.SskuSilverTransportType.DATACAMP));
    }

    @Test
    public void testMigrationIsProcessedIfDstBusinessIdIsCorrect() {
        MessageBatch testMessageBatch = DatacampOffersTestUtil.createTestMessage(
            DatacampOffersTestUtil.createUnitedOffer(
                DataCampContentStatus.MappingConfidence.MAPPING_CONFIDENCE_CONTENT, META,
                DatacampOffersTestUtil.createServiceOfferBuilder(BUSINESS_ID, SUPPLIER_ID_1)));

        var migrationInfo = new ServiceOfferMigrationInfo()
            .setSupplierId(SUPPLIER_ID_1)
            .setShopSku(SHOP_SKU)
            .setSrcBusinessId(ANOTHER_BUSINESS_ID)
            .setDstBusinessId(BUSINESS_ID)
            .setAddedTimestamp(Instant.now());
        serviceOfferMigrationRepository.insert(migrationInfo);

        logbrokerMessageHandler.process(DatacampOffersTestUtil.getDataCampMessages(testMessageBatch));

        List<ServiceOfferMigrationInfo> updatedMigrationInfos = serviceOfferMigrationRepository.findAll();
        Assertions.assertThat(updatedMigrationInfos).hasSize(1);
        Assertions.assertThat(updatedMigrationInfos.get(0)).isEqualToIgnoringGivenFields(migrationInfo,
            "isProcessed", "processedTimestamp");
        Assertions.assertThat(updatedMigrationInfos.get(0).isProcessed()).isTrue();
    }

    @Test
    public void testMigrationIsNotProcessedIfWrongDstBusinessId() {
        MessageBatch testMessageBatch = DatacampOffersTestUtil.createTestMessage(
            DatacampOffersTestUtil.createUnitedOffer(
                DataCampContentStatus.MappingConfidence.MAPPING_CONFIDENCE_CONTENT, META,
                DatacampOffersTestUtil.createServiceOfferBuilder(BUSINESS_ID, SUPPLIER_ID_1)));

        var migrationInfo = new ServiceOfferMigrationInfo()
            .setSupplierId(SUPPLIER_ID_1)
            .setShopSku(SHOP_SKU)
            .setSrcBusinessId(BUSINESS_ID)
            .setDstBusinessId(Integer.MAX_VALUE)
            .setAddedTimestamp(Instant.now());
        serviceOfferMigrationRepository.insert(migrationInfo);

        logbrokerMessageHandler.process(DatacampOffersTestUtil.getDataCampMessages(testMessageBatch));

        List<ServiceOfferMigrationInfo> updatedMigrationInfos = serviceOfferMigrationRepository.findAll();
        Assertions.assertThat(updatedMigrationInfos).hasSize(1);
        Assertions.assertThat(updatedMigrationInfos.get(0).isProcessed()).isFalse();
    }

    @Test
    public void testMigrationIsProcessedForWhiteAndWhiteDbsOffers() {
        mdmSupplierRepository.insert(new MdmSupplier().setId(BUSINESS_ID).setType(MdmSupplierType.BUSINESS));
        mdmSupplierRepository.insert(new MdmSupplier().setId(WHITE_SUPPLIER_ID).setType(MdmSupplierType.MARKET_SHOP)
            .setBusinessId(BUSINESS_ID).setBusinessEnabled(true));
        mdmSupplierRepository.insert(new MdmSupplier().setId(WHITE_DBS_SUPPLIER_ID).setType(MdmSupplierType.MARKET_SHOP)
            .setBusinessId(BUSINESS_ID).setBusinessEnabled(true)
            .setSalesModels(List.of(MdmSupplierSalesModel.DROPSHIP_BY_SELLER)));
        mdmSupplierCachingService.refresh();

        MessageBatch testMessageBatch = DatacampOffersTestUtil.createTestMessage(
            DatacampOffersTestUtil.createUnitedOffer(
                DataCampContentStatus.MappingConfidence.MAPPING_CONFIDENCE_CONTENT, META,
                DatacampOffersTestUtil.createServiceOfferBuilder(BUSINESS_ID, WHITE_SUPPLIER_ID)),
            DatacampOffersTestUtil.createUnitedOffer(
                DataCampContentStatus.MappingConfidence.MAPPING_CONFIDENCE_CONTENT, META,
                DatacampOffersTestUtil.createServiceOfferBuilder(BUSINESS_ID, WHITE_DBS_SUPPLIER_ID)));

        var whiteMigrationInfoExp = createMigrationInfo(WHITE_SUPPLIER_ID, SHOP_SKU,
            ANOTHER_BUSINESS_ID, BUSINESS_ID);
        var whiteDbsMigrationInfoExp = createMigrationInfo(WHITE_DBS_SUPPLIER_ID, SHOP_SKU,
            ANOTHER_BUSINESS_ID, BUSINESS_ID);
        serviceOfferMigrationRepository.insertBatch(whiteMigrationInfoExp, whiteDbsMigrationInfoExp);

        // when
        logbrokerMessageHandler.process(DatacampOffersTestUtil.getDataCampMessages(testMessageBatch));

        // then
        List<ServiceOfferMigrationInfo> updatedMigrationInfos = serviceOfferMigrationRepository.findAll();
        Assertions.assertThat(updatedMigrationInfos).hasSize(2);

        Map<ShopSkuKey, ServiceOfferMigrationInfo> migrationInfoMap =
            updatedMigrationInfos.stream().collect(Collectors.toMap(ServiceOfferMigrationInfo::getShopSkuKey,
                Function.identity()));
        ShopSkuKey whiteKey = new ShopSkuKey(WHITE_SUPPLIER_ID, SHOP_SKU);
        ServiceOfferMigrationInfo whiteMigrationInfoResult = migrationInfoMap.get(whiteKey);
        Assertions.assertThat(whiteMigrationInfoResult).isEqualToIgnoringGivenFields(whiteMigrationInfoExp,
            "isProcessed", "processedTimestamp");
        Assertions.assertThat(whiteMigrationInfoResult.isProcessed()).isTrue();

        ShopSkuKey whiteDbsKey = new ShopSkuKey(WHITE_DBS_SUPPLIER_ID, SHOP_SKU);
        ServiceOfferMigrationInfo whiteDbsMigrationInfoResult = migrationInfoMap.get(whiteDbsKey);
        Assertions.assertThat(whiteDbsMigrationInfoResult).isEqualToIgnoringGivenFields(whiteDbsMigrationInfoExp,
            "isProcessed", "processedTimestamp");
        Assertions.assertThat(whiteDbsMigrationInfoResult.isProcessed()).isTrue();
    }

    private ServiceOfferMigrationInfo createMigrationInfo(int supplierId, String shopSku,
                                                          int srcBizId, int dstBizId) {
        return new ServiceOfferMigrationInfo()
            .setSupplierId(supplierId)
            .setShopSku(shopSku)
            .setSrcBusinessId(srcBizId)
            .setDstBusinessId(dstBizId)
            .setAddedTimestamp(Instant.now());
    }

    @Test
    public void testOnlyRootKeysAreEnqueuedForSskuGoldRecalculation() {
        keyValueService.invalidateCache();

        mdmSupplierRepository.insert(new MdmSupplier().setId(BUSINESS_ID).setType(MdmSupplierType.BUSINESS));
        mdmSupplierRepository.insert(new MdmSupplier().setId(SUPPLIER_ID_1).setType(MdmSupplierType.THIRD_PARTY)
            .setBusinessId(BUSINESS_ID).setBusinessEnabled(true));
        mdmSupplierCachingService.refresh();

        sskuExistenceRepository.markExistence(List.of(new ShopSkuKey(SUPPLIER_ID_1, SHOP_SKU)), true);

        MessageBatch testMessageBatch = DatacampOffersTestUtil.createTestMessage(
            DatacampOffersTestUtil.createUnitedOffer(
                DataCampContentStatus.MappingConfidence.MAPPING_CONFIDENCE_CONTENT, META,
                DatacampOffersTestUtil.createServiceOfferBuilder(BUSINESS_ID, SUPPLIER_ID_1)));

        logbrokerMessageHandler.process(DatacampOffersTestUtil.getDataCampMessages(testMessageBatch));

        List<SskuToRefreshInfo> enqueuedSskus = sskuQueue.getUnprocessedBatch(UNPROCESSED_BATCH_SIZE);
        List<MdmMskuQueueInfo> enqueuedMskus = mskuQueue.getUnprocessedBatch(UNPROCESSED_BATCH_SIZE);

        List<ShopSkuKey> sskuKeys = MdmQueueInfoBaseUtils.keys(enqueuedSskus);
        List<Long> mskuKeys = MdmQueueInfoBaseUtils.keys(enqueuedMskus);

        List<MdmEnqueueReason> sskuReasons = MdmQueueInfoBaseUtils.reasons(enqueuedSskus);
        List<MdmEnqueueReason> mskuReasons = MdmQueueInfoBaseUtils.reasons(enqueuedMskus);

        Assertions.assertThat(sskuKeys.size()).isEqualTo(1);
        Assertions.assertThat(sskuKeys).containsExactly(new ShopSkuKey(BUSINESS_ID, SHOP_SKU));
        Assertions.assertThat(sskuReasons).containsExactly(MdmEnqueueReason.CHANGED_SSKU_SILVER_DATA);

        Assertions.assertThat(mskuKeys.size()).isEqualTo(1);
        Assertions.assertThat(mskuKeys).containsExactly(MSKU_ID);
        Assertions.assertThat(mskuReasons).containsExactly(MdmEnqueueReason.CHANGED_MAPPING_EOX);
    }

    @Ignore("Тест работает, но может флапать")
    @Test
    public void testProcessWithDifferentLBRateLimitsWhileOffersImport() {
        // given
        keyValueService.invalidateCache();

        mdmSupplierRepository.insert(new MdmSupplier().setId(BUSINESS_ID).setType(MdmSupplierType.BUSINESS));
        mdmSupplierRepository.insert(new MdmSupplier().setId(SUPPLIER_ID_1).setType(MdmSupplierType.THIRD_PARTY)
            .setBusinessId(BUSINESS_ID).setBusinessEnabled(true));
        mdmSupplierCachingService.refresh();

        sskuExistenceRepository.markExistence(List.of(new ShopSkuKey(SUPPLIER_ID_1, SHOP_SKU)), true);

        int messageCount = 200;
        List<DatacampMessageOuterClass.DatacampMessage> messages = getDatacampMessages(messageCount);

        // Достаточно сложно найти баланс между кол-вом converted-сообщений и rate limit:
        // если сообщений мало - метод process() с low rate limit отрабатывает даже быстрее, чем high rare limit
        // (т.к. import офферов работает быстрее на маленьких объемах).
        // Когда же сообщений много (> 100), то ограничение потока помогает снизить скорость импорта офферов,
        // и время выполнения метода process() растет.
        float highRateLimit = messageCount;
        float lowRateLimit = 1f;

        // when
        long startTime = LocalDateTime.now().toLocalTime().toSecondOfDay();
        keyValueService.putValue(MdmProperties.LB_RATE_LIMIT, highRateLimit);
        keyValueService.invalidateCache();
        logbrokerMessageHandler.process(messages);
        long fastProcessing = LocalDateTime.now().toLocalTime().toSecondOfDay() - startTime;

        startTime = LocalDateTime.now().toLocalTime().toSecondOfDay();
        keyValueService.putValue(MdmProperties.LB_RATE_LIMIT, lowRateLimit);
        keyValueService.invalidateCache();
        logbrokerMessageHandler.process(messages);
        long slowProcessing = LocalDateTime.now().toLocalTime().toSecondOfDay() - startTime;

        // then
        Assertions.assertThat(slowProcessing).isGreaterThan(fastProcessing);
    }

    @Test
    public void testSequentialImportTwoPartialOffers() {
        keyValueService.invalidateCache();

        ShopSkuKey businessKey = new ShopSkuKey(BUSINESS_ID, SHOP_SKU);
        ShopSkuKey serviceKey1 = new ShopSkuKey(SUPPLIER_ID_1, SHOP_SKU);
        ShopSkuKey serviceKey2 = new ShopSkuKey(SUPPLIER_ID_2, SHOP_SKU);

        mdmSupplierRepository.insertBatch(
            new MdmSupplier().setId(BUSINESS_ID).setType(MdmSupplierType.BUSINESS),
            new MdmSupplier().setId(SUPPLIER_ID_1).setType(MdmSupplierType.THIRD_PARTY)
                .setBusinessId(BUSINESS_ID).setBusinessEnabled(true),
            new MdmSupplier().setId(SUPPLIER_ID_2).setType(MdmSupplierType.THIRD_PARTY)
                .setBusinessId(BUSINESS_ID).setBusinessEnabled(true));
        mdmSupplierCachingService.refresh();

        MessageBatch testMessageBatch = DatacampOffersTestUtil.createTestMessage(
            DatacampOffersTestUtil.createUnitedOfferWithTunedServiceVersions(
                META, MASTER_DATA_VERSION + 1,
                DatacampOffersTestUtil.createServiceOfferBuilder(BUSINESS_ID, SUPPLIER_ID_1)));

        logbrokerMessageHandler.process(DatacampOffersTestUtil.getDataCampMessages(testMessageBatch));

        Map<ShopSkuKey, List<SskuSilverParamValue>> silvers = silverSskuRepository.findAll()
            .stream()
            .collect(Collectors.groupingBy(SskuParamValue::getShopSkuKey));

        Assertions.assertThat(silvers).hasSize(2);
        Assertions.assertThat(silvers.get(businessKey))
            .usingComparatorForElementFieldsWithNames(
                TestMdmParamUtils.MODIFICATION_INFO_COMPARATOR, "modificationInfo")
            .containsExactlyInAnyOrder(
                TestMdmParamUtils.createSskuSilverParamValue(
                    KnownMdmParams.DOCUMENT_REG_NUMBER, "documentRegNumber", "345",
                    MasterDataSourceType.SUPPLIER, ImpersonalSourceId.DATACAMP.name(), businessKey, MASTER_DATA_VERSION,
                    SskuSilverParamValue.SskuSilverTransportType.DATACAMP),
                TestMdmParamUtils.createSskuSilverParamValue(
                    KnownMdmParams.DATACAMP_MASTER_DATA_VERSION, "datacampMDVersion", MASTER_DATA_VERSION,
                    MasterDataSourceType.SUPPLIER, ImpersonalSourceId.DATACAMP.name(), businessKey, MASTER_DATA_VERSION,
                    SskuSilverParamValue.SskuSilverTransportType.DATACAMP));

        Assertions.assertThat(silvers.get(serviceKey1))
            .usingComparatorForElementFieldsWithNames(
                TestMdmParamUtils.MODIFICATION_INFO_COMPARATOR, "modificationInfo")
            .containsExactlyInAnyOrder(
                TestMdmParamUtils.createSskuSilverParamValue(
                    KnownMdmParams.TRANSPORT_UNIT_SIZE, "transportUnitSize", 12,
                    MasterDataSourceType.SUPPLIER, ImpersonalSourceId.DATACAMP.name(), serviceKey1,
                    MASTER_DATA_VERSION + 1,
                    SskuSilverParamValue.SskuSilverTransportType.DATACAMP),
                TestMdmParamUtils.createSskuSilverParamValue(
                    KnownMdmParams.SERVICE_EXISTS, "serviceExists", true,
                    MasterDataSourceType.SUPPLIER, ImpersonalSourceId.DATACAMP.name(), serviceKey1,
                    MASTER_DATA_VERSION + 1,
                    SskuSilverParamValue.SskuSilverTransportType.DATACAMP),
                TestMdmParamUtils.createSskuSilverParamValue(
                    KnownMdmParams.DATACAMP_MASTER_DATA_VERSION, "datacampMDVersion",
                    MASTER_DATA_VERSION + 1,
                    MasterDataSourceType.SUPPLIER, ImpersonalSourceId.DATACAMP.name(), serviceKey1,
                    MASTER_DATA_VERSION + 1,
                    SskuSilverParamValue.SskuSilverTransportType.DATACAMP));

        testMessageBatch = DatacampOffersTestUtil.createTestMessage(
            DatacampOffersTestUtil.createUnitedOfferWithTunedServiceVersions(
                META, MASTER_DATA_VERSION,
                DatacampOffersTestUtil.createServiceOfferBuilder(BUSINESS_ID, SUPPLIER_ID_2)));

        logbrokerMessageHandler.process(DatacampOffersTestUtil.getDataCampMessages(testMessageBatch));

        silvers = silverSskuRepository.findAll()
            .stream()
            .collect(Collectors.groupingBy(SskuParamValue::getShopSkuKey));

        Assertions.assertThat(silvers).hasSize(3);
        Assertions.assertThat(silvers.get(businessKey))
            .usingComparatorForElementFieldsWithNames(
                TestMdmParamUtils.MODIFICATION_INFO_COMPARATOR, "modificationInfo")
            .containsExactlyInAnyOrder(
                TestMdmParamUtils.createSskuSilverParamValue(
                    KnownMdmParams.DOCUMENT_REG_NUMBER, "documentRegNumber", "345",
                    MasterDataSourceType.SUPPLIER, ImpersonalSourceId.DATACAMP.name(), businessKey, MASTER_DATA_VERSION,
                    SskuSilverParamValue.SskuSilverTransportType.DATACAMP),
                TestMdmParamUtils.createSskuSilverParamValue(
                    KnownMdmParams.DATACAMP_MASTER_DATA_VERSION, "datacampMDVersion", MASTER_DATA_VERSION,
                    MasterDataSourceType.SUPPLIER, ImpersonalSourceId.DATACAMP.name(), businessKey, MASTER_DATA_VERSION,
                    SskuSilverParamValue.SskuSilverTransportType.DATACAMP));

        Assertions.assertThat(silvers.get(serviceKey1))
            .usingComparatorForElementFieldsWithNames(
                TestMdmParamUtils.MODIFICATION_INFO_COMPARATOR, "modificationInfo")
            .containsExactlyInAnyOrder(
                TestMdmParamUtils.createSskuSilverParamValue(
                    KnownMdmParams.TRANSPORT_UNIT_SIZE, "transportUnitSize", 12,
                    MasterDataSourceType.SUPPLIER, ImpersonalSourceId.DATACAMP.name(), serviceKey1,
                    MASTER_DATA_VERSION + 1,
                    SskuSilverParamValue.SskuSilverTransportType.DATACAMP),
                TestMdmParamUtils.createSskuSilverParamValue(
                    KnownMdmParams.SERVICE_EXISTS, "serviceExists", true,
                    MasterDataSourceType.SUPPLIER, ImpersonalSourceId.DATACAMP.name(), serviceKey1,
                    MASTER_DATA_VERSION + 1,
                    SskuSilverParamValue.SskuSilverTransportType.DATACAMP),
                TestMdmParamUtils.createSskuSilverParamValue(
                    KnownMdmParams.DATACAMP_MASTER_DATA_VERSION, "datacampMDVersion", MASTER_DATA_VERSION + 1,
                    MasterDataSourceType.SUPPLIER, ImpersonalSourceId.DATACAMP.name(), serviceKey1,
                    MASTER_DATA_VERSION + 1,
                    SskuSilverParamValue.SskuSilverTransportType.DATACAMP));

        Assertions.assertThat(silvers.get(serviceKey2))
            .usingComparatorForElementFieldsWithNames(
                TestMdmParamUtils.MODIFICATION_INFO_COMPARATOR, "modificationInfo")
            .containsExactlyInAnyOrder(
                TestMdmParamUtils.createSskuSilverParamValue(
                    KnownMdmParams.TRANSPORT_UNIT_SIZE, "transportUnitSize", 12,
                    MasterDataSourceType.SUPPLIER, ImpersonalSourceId.DATACAMP.name(), serviceKey2, MASTER_DATA_VERSION,
                    SskuSilverParamValue.SskuSilverTransportType.DATACAMP),
                TestMdmParamUtils.createSskuSilverParamValue(
                    KnownMdmParams.SERVICE_EXISTS, "serviceExists", true,
                    MasterDataSourceType.SUPPLIER, ImpersonalSourceId.DATACAMP.name(), serviceKey2, MASTER_DATA_VERSION,
                    SskuSilverParamValue.SskuSilverTransportType.DATACAMP),
                TestMdmParamUtils.createSskuSilverParamValue(
                    KnownMdmParams.DATACAMP_MASTER_DATA_VERSION, "datacampMDVersion", MASTER_DATA_VERSION,
                    MasterDataSourceType.SUPPLIER, ImpersonalSourceId.DATACAMP.name(), serviceKey2, MASTER_DATA_VERSION,
                    SskuSilverParamValue.SskuSilverTransportType.DATACAMP));
    }

    @Test
    public void testImportOfferWithEmptyServicePart() {
        keyValueService.invalidateCache();

        ShopSkuKey businessKey = new ShopSkuKey(BUSINESS_ID, SHOP_SKU);
        ShopSkuKey serviceKey1 = new ShopSkuKey(SUPPLIER_ID_1, SHOP_SKU);

        mdmSupplierRepository.insertBatch(
            new MdmSupplier().setId(BUSINESS_ID).setType(MdmSupplierType.BUSINESS),
            new MdmSupplier().setId(SUPPLIER_ID_1).setType(MdmSupplierType.THIRD_PARTY)
                .setBusinessId(BUSINESS_ID).setBusinessEnabled(true),
            new MdmSupplier().setId(SUPPLIER_ID_2).setType(MdmSupplierType.THIRD_PARTY)
                .setBusinessId(BUSINESS_ID).setBusinessEnabled(true));
        mdmSupplierCachingService.refresh();

        MessageBatch testMessageBatch = DatacampOffersTestUtil.createTestMessage(
            DatacampOffersTestUtil.createUnitedOffer(
                DataCampContentStatus.MappingConfidence.MAPPING_CONFIDENCE_CONTENT, META,
                DatacampOffersTestUtil.createServiceOfferBuilder(BUSINESS_ID, SUPPLIER_ID_1)));

        logbrokerMessageHandler.process(DatacampOffersTestUtil.getDataCampMessages(testMessageBatch));

        Map<ShopSkuKey, List<SskuSilverParamValue>> silvers = silverSskuRepository.findAll()
            .stream()
            .collect(Collectors.groupingBy(SskuParamValue::getShopSkuKey));

        Assertions.assertThat(silvers).hasSize(2);
        Assertions.assertThat(silvers.get(businessKey))
            .usingComparatorForElementFieldsWithNames(
                TestMdmParamUtils.MODIFICATION_INFO_COMPARATOR, "modificationInfo")
            .containsExactlyInAnyOrder(
                TestMdmParamUtils.createSskuSilverParamValue(
                    KnownMdmParams.DOCUMENT_REG_NUMBER, "documentRegNumber", "345",
                    MasterDataSourceType.SUPPLIER, ImpersonalSourceId.DATACAMP.name(), businessKey, MASTER_DATA_VERSION,
                    SskuSilverParamValue.SskuSilverTransportType.DATACAMP),
                TestMdmParamUtils.createSskuSilverParamValue(
                    KnownMdmParams.DATACAMP_MASTER_DATA_VERSION, "datacampMDVersion", MASTER_DATA_VERSION,
                    MasterDataSourceType.SUPPLIER, ImpersonalSourceId.DATACAMP.name(), businessKey, MASTER_DATA_VERSION,
                    SskuSilverParamValue.SskuSilverTransportType.DATACAMP));

        Assertions.assertThat(silvers.get(serviceKey1))
            .usingComparatorForElementFieldsWithNames(
                TestMdmParamUtils.MODIFICATION_INFO_COMPARATOR, "modificationInfo")
            .containsExactlyInAnyOrder(
                TestMdmParamUtils.createSskuSilverParamValue(
                    KnownMdmParams.TRANSPORT_UNIT_SIZE, "transportUnitSize", 12,
                    MasterDataSourceType.SUPPLIER, ImpersonalSourceId.DATACAMP.name(), serviceKey1, MASTER_DATA_VERSION,
                    SskuSilverParamValue.SskuSilverTransportType.DATACAMP),
                TestMdmParamUtils.createSskuSilverParamValue(
                    KnownMdmParams.SERVICE_EXISTS, "serviceExists", true,
                    MasterDataSourceType.SUPPLIER, ImpersonalSourceId.DATACAMP.name(), serviceKey1, MASTER_DATA_VERSION,
                    SskuSilverParamValue.SskuSilverTransportType.DATACAMP),
                TestMdmParamUtils.createSskuSilverParamValue(
                    KnownMdmParams.DATACAMP_MASTER_DATA_VERSION, "datacampMDVersion", MASTER_DATA_VERSION,
                    MasterDataSourceType.SUPPLIER, ImpersonalSourceId.DATACAMP.name(), serviceKey1, MASTER_DATA_VERSION,
                    SskuSilverParamValue.SskuSilverTransportType.DATACAMP));

        testMessageBatch = DatacampOffersTestUtil.createTestMessage(
            DatacampOffersTestUtil.createUnitedOfferWithTunedVersions(
                META,
                MASTER_DATA_VERSION + 1, MASTER_DATA_VERSION));

        logbrokerMessageHandler.process(DatacampOffersTestUtil.getDataCampMessages(testMessageBatch));

        // база cхлопнулась с существующим сохранённым ранее сервисом и получился не голый оффер
        silvers = silverSskuRepository.findAll()
            .stream()
            .collect(Collectors.groupingBy(SskuParamValue::getShopSkuKey));

        Assertions.assertThat(silvers).hasSize(2);
        Assertions.assertThat(silvers.get(businessKey))
            .usingComparatorForElementFieldsWithNames(
                TestMdmParamUtils.MODIFICATION_INFO_COMPARATOR, "modificationInfo")
            .containsExactlyInAnyOrder(
                TestMdmParamUtils.createSskuSilverParamValue(
                    KnownMdmParams.DOCUMENT_REG_NUMBER,
                    "documentRegNumber",
                    "345",
                    MasterDataSourceType.SUPPLIER,
                    ImpersonalSourceId.DATACAMP.name(),
                    businessKey,
                    MASTER_DATA_VERSION + 1,
                    SskuSilverParamValue.SskuSilverTransportType.DATACAMP),
                TestMdmParamUtils.createSskuSilverParamValue(
                    KnownMdmParams.DATACAMP_MASTER_DATA_VERSION,
                    "datacampMDVersion",
                    MASTER_DATA_VERSION + 1,
                    MasterDataSourceType.SUPPLIER,
                    ImpersonalSourceId.DATACAMP.name(),
                    businessKey,
                    MASTER_DATA_VERSION + 1,
                    SskuSilverParamValue.SskuSilverTransportType.DATACAMP)
            );

        Assertions.assertThat(silvers.get(serviceKey1))
            .usingComparatorForElementFieldsWithNames(
                TestMdmParamUtils.MODIFICATION_INFO_COMPARATOR, "modificationInfo")
            .containsExactlyInAnyOrder(
                TestMdmParamUtils.createSskuSilverParamValue(
                    KnownMdmParams.TRANSPORT_UNIT_SIZE, "transportUnitSize", 12,
                    MasterDataSourceType.SUPPLIER, ImpersonalSourceId.DATACAMP.name(), serviceKey1, MASTER_DATA_VERSION,
                    SskuSilverParamValue.SskuSilverTransportType.DATACAMP),
                TestMdmParamUtils.createSskuSilverParamValue(
                    KnownMdmParams.SERVICE_EXISTS, "serviceExists", true,
                    MasterDataSourceType.SUPPLIER, ImpersonalSourceId.DATACAMP.name(), serviceKey1, MASTER_DATA_VERSION,
                    SskuSilverParamValue.SskuSilverTransportType.DATACAMP),
                TestMdmParamUtils.createSskuSilverParamValue(
                    KnownMdmParams.DATACAMP_MASTER_DATA_VERSION, "datacampMDVersion", MASTER_DATA_VERSION,
                    MasterDataSourceType.SUPPLIER, ImpersonalSourceId.DATACAMP.name(), serviceKey1, MASTER_DATA_VERSION,
                    SskuSilverParamValue.SskuSilverTransportType.DATACAMP));
    }

    @Test
    public void testSynchronousImportTwoPartialOffers() {
        keyValueService.invalidateCache();

        ShopSkuKey businessKey = new ShopSkuKey(BUSINESS_ID, SHOP_SKU);
        ShopSkuKey serviceKey1 = new ShopSkuKey(SUPPLIER_ID_1, SHOP_SKU);
        ShopSkuKey serviceKey2 = new ShopSkuKey(SUPPLIER_ID_2, SHOP_SKU);

        mdmSupplierRepository.insertBatch(
            new MdmSupplier().setId(BUSINESS_ID).setType(MdmSupplierType.BUSINESS),
            new MdmSupplier().setId(SUPPLIER_ID_1).setType(MdmSupplierType.THIRD_PARTY)
                .setBusinessId(BUSINESS_ID).setBusinessEnabled(true),
            new MdmSupplier().setId(SUPPLIER_ID_2).setType(MdmSupplierType.THIRD_PARTY)
                .setBusinessId(BUSINESS_ID).setBusinessEnabled(true));
        mdmSupplierCachingService.refresh();

        MessageBatch testMessageBatch = DatacampOffersTestUtil.createTestMessage(
            DatacampOffersTestUtil.createUnitedOfferWithTunedServiceVersions(
                META, MASTER_DATA_VERSION + 1,
                DatacampOffersTestUtil.createServiceOfferBuilder(BUSINESS_ID, SUPPLIER_ID_1)),
            DatacampOffersTestUtil.createUnitedOfferWithTunedServiceVersions(
                META, MASTER_DATA_VERSION,
                DatacampOffersTestUtil.createServiceOfferBuilder(BUSINESS_ID, SUPPLIER_ID_2))
        );

        logbrokerMessageHandler.process(DatacampOffersTestUtil.getDataCampMessages(testMessageBatch));

        Map<ShopSkuKey, List<SskuSilverParamValue>> silvers = silverSskuRepository.findAll()
            .stream()
            .collect(Collectors.groupingBy(SskuParamValue::getShopSkuKey));

        Assertions.assertThat(silvers).hasSize(3);
        Assertions.assertThat(silvers.get(businessKey))
            .usingComparatorForElementFieldsWithNames(
                TestMdmParamUtils.MODIFICATION_INFO_COMPARATOR, "modificationInfo")
            .containsExactlyInAnyOrder(
                TestMdmParamUtils.createSskuSilverParamValue(
                    KnownMdmParams.DOCUMENT_REG_NUMBER, "documentRegNumber", "345",
                    MasterDataSourceType.SUPPLIER, ImpersonalSourceId.DATACAMP.name(), businessKey, MASTER_DATA_VERSION,
                    SskuSilverParamValue.SskuSilverTransportType.DATACAMP),
                TestMdmParamUtils.createSskuSilverParamValue(
                    KnownMdmParams.DATACAMP_MASTER_DATA_VERSION, "datacampMDVersion", MASTER_DATA_VERSION,
                    MasterDataSourceType.SUPPLIER, ImpersonalSourceId.DATACAMP.name(), businessKey, MASTER_DATA_VERSION,
                    SskuSilverParamValue.SskuSilverTransportType.DATACAMP));

        Assertions.assertThat(silvers.get(serviceKey1))
            .usingComparatorForElementFieldsWithNames(
                TestMdmParamUtils.MODIFICATION_INFO_COMPARATOR, "modificationInfo")
            .containsExactlyInAnyOrder(
                TestMdmParamUtils.createSskuSilverParamValue(
                    KnownMdmParams.TRANSPORT_UNIT_SIZE, "transportUnitSize", 12,
                    MasterDataSourceType.SUPPLIER, ImpersonalSourceId.DATACAMP.name(), serviceKey1,
                    MASTER_DATA_VERSION + 1,
                    SskuSilverParamValue.SskuSilverTransportType.DATACAMP),
                TestMdmParamUtils.createSskuSilverParamValue(
                    KnownMdmParams.SERVICE_EXISTS, "serviceExists", true,
                    MasterDataSourceType.SUPPLIER, ImpersonalSourceId.DATACAMP.name(), serviceKey1,
                    MASTER_DATA_VERSION + 1,
                    SskuSilverParamValue.SskuSilverTransportType.DATACAMP),
                TestMdmParamUtils.createSskuSilverParamValue(
                    KnownMdmParams.DATACAMP_MASTER_DATA_VERSION, "datacampMDVersion", MASTER_DATA_VERSION + 1,
                    MasterDataSourceType.SUPPLIER, ImpersonalSourceId.DATACAMP.name(), serviceKey1,
                    MASTER_DATA_VERSION + 1,
                    SskuSilverParamValue.SskuSilverTransportType.DATACAMP));

        Assertions.assertThat(silvers.get(serviceKey2))
            .usingComparatorForElementFieldsWithNames(
                TestMdmParamUtils.MODIFICATION_INFO_COMPARATOR, "modificationInfo")
            .containsExactlyInAnyOrder(
                TestMdmParamUtils.createSskuSilverParamValue(
                    KnownMdmParams.TRANSPORT_UNIT_SIZE, "transportUnitSize", 12,
                    MasterDataSourceType.SUPPLIER, ImpersonalSourceId.DATACAMP.name(), serviceKey2, MASTER_DATA_VERSION,
                    SskuSilverParamValue.SskuSilverTransportType.DATACAMP),
                TestMdmParamUtils.createSskuSilverParamValue(
                    KnownMdmParams.SERVICE_EXISTS, "serviceExists", true,
                    MasterDataSourceType.SUPPLIER, ImpersonalSourceId.DATACAMP.name(), serviceKey2, MASTER_DATA_VERSION,
                    SskuSilverParamValue.SskuSilverTransportType.DATACAMP),
                TestMdmParamUtils.createSskuSilverParamValue(
                    KnownMdmParams.DATACAMP_MASTER_DATA_VERSION, "datacampMDVersion", MASTER_DATA_VERSION,
                    MasterDataSourceType.SUPPLIER, ImpersonalSourceId.DATACAMP.name(), serviceKey2, MASTER_DATA_VERSION,
                    SskuSilverParamValue.SskuSilverTransportType.DATACAMP));
    }

    @Test
    public void testWhenImportRewriteExistingData() {
        ShopSkuKey businessKey = new ShopSkuKey(BUSINESS_ID, SHOP_SKU);
        ShopSkuKey serviceKey1 = new ShopSkuKey(SUPPLIER_ID_1, SHOP_SKU);

        mdmSupplierRepository.insertBatch(
            new MdmSupplier().setId(BUSINESS_ID).setType(MdmSupplierType.BUSINESS),
            new MdmSupplier().setId(SUPPLIER_ID_1).setType(MdmSupplierType.THIRD_PARTY)
                .setBusinessId(BUSINESS_ID).setBusinessEnabled(true));
        mdmSupplierCachingService.refresh();

        MessageBatch testMessageBatch = DatacampOffersTestUtil.createTestMessage(
            DatacampOffersTestUtil.createUnitedOffer(
                DataCampContentStatus.MappingConfidence.MAPPING_CONFIDENCE_CONTENT, META,
                DatacampOffersTestUtil.createServiceOfferBuilder(BUSINESS_ID, SUPPLIER_ID_1)));

        logbrokerMessageHandler.process(DatacampOffersTestUtil.getDataCampMessages(testMessageBatch));

        Map<ShopSkuKey, List<SskuSilverParamValue>> silvers = silverSskuRepository.findAll()
            .stream()
            .collect(Collectors.groupingBy(SskuParamValue::getShopSkuKey));

        Assertions.assertThat(silvers).hasSize(2);
        Assertions.assertThat(silvers.get(businessKey))
            .usingComparatorForElementFieldsWithNames(
                TestMdmParamUtils.MODIFICATION_INFO_COMPARATOR, "modificationInfo")
            .containsExactlyInAnyOrder(
                TestMdmParamUtils.createSskuSilverParamValue(
                    KnownMdmParams.DOCUMENT_REG_NUMBER, "documentRegNumber", "345",
                    MasterDataSourceType.SUPPLIER, ImpersonalSourceId.DATACAMP.name(), businessKey, MASTER_DATA_VERSION,
                    SskuSilverParamValue.SskuSilverTransportType.DATACAMP),
                TestMdmParamUtils.createSskuSilverParamValue(
                    KnownMdmParams.DATACAMP_MASTER_DATA_VERSION, "datacampMDVersion", MASTER_DATA_VERSION,
                    MasterDataSourceType.SUPPLIER, ImpersonalSourceId.DATACAMP.name(), businessKey, MASTER_DATA_VERSION,
                    SskuSilverParamValue.SskuSilverTransportType.DATACAMP));

        Assertions.assertThat(silvers.get(serviceKey1))
            .usingComparatorForElementFieldsWithNames(
                TestMdmParamUtils.MODIFICATION_INFO_COMPARATOR, "modificationInfo")
            .containsExactlyInAnyOrder(
                TestMdmParamUtils.createSskuSilverParamValue(
                    KnownMdmParams.TRANSPORT_UNIT_SIZE, "transportUnitSize", 12,
                    MasterDataSourceType.SUPPLIER, ImpersonalSourceId.DATACAMP.name(), serviceKey1, MASTER_DATA_VERSION,
                    SskuSilverParamValue.SskuSilverTransportType.DATACAMP),
                TestMdmParamUtils.createSskuSilverParamValue(
                    KnownMdmParams.SERVICE_EXISTS, "serviceExists", true,
                    MasterDataSourceType.SUPPLIER, ImpersonalSourceId.DATACAMP.name(), serviceKey1, MASTER_DATA_VERSION,
                    SskuSilverParamValue.SskuSilverTransportType.DATACAMP),
                TestMdmParamUtils.createSskuSilverParamValue(
                    KnownMdmParams.DATACAMP_MASTER_DATA_VERSION, "datacampMDVersion", MASTER_DATA_VERSION,
                    MasterDataSourceType.SUPPLIER, ImpersonalSourceId.DATACAMP.name(), serviceKey1, MASTER_DATA_VERSION,
                    SskuSilverParamValue.SskuSilverTransportType.DATACAMP));

        testMessageBatch = DatacampOffersTestUtil.createTestMessage(
            DatacampOffersTestUtil.createUnitedOfferWithTunedVersions(
                META, MASTER_DATA_VERSION + 1,
                MASTER_DATA_VERSION + 1,
                DatacampOffersTestUtil.createServiceOfferBuilder(BUSINESS_ID, SUPPLIER_ID_1, true)));

        logbrokerMessageHandler.process(DatacampOffersTestUtil.getDataCampMessages(testMessageBatch));

        silvers = silverSskuRepository.findAll()
            .stream()
            .collect(Collectors.groupingBy(SskuParamValue::getShopSkuKey));

        Assertions.assertThat(silvers).hasSize(2);
        Assertions.assertThat(silvers.get(businessKey))
            .usingComparatorForElementFieldsWithNames(
                TestMdmParamUtils.MODIFICATION_INFO_COMPARATOR, "modificationInfo")
            .containsExactlyInAnyOrder(
                TestMdmParamUtils.createSskuSilverParamValue(
                    KnownMdmParams.DOCUMENT_REG_NUMBER, "documentRegNumber", "345",
                    MasterDataSourceType.SUPPLIER, ImpersonalSourceId.DATACAMP.name(), businessKey,
                    MASTER_DATA_VERSION + 1,
                    SskuSilverParamValue.SskuSilverTransportType.DATACAMP),
                TestMdmParamUtils.createSskuSilverParamValue(
                    KnownMdmParams.DATACAMP_MASTER_DATA_VERSION, "datacampMDVersion", MASTER_DATA_VERSION + 1,
                    MasterDataSourceType.SUPPLIER, ImpersonalSourceId.DATACAMP.name(), businessKey,
                    MASTER_DATA_VERSION + 1,
                    SskuSilverParamValue.SskuSilverTransportType.DATACAMP));

        Assertions.assertThat(silvers.get(serviceKey1))
            .usingComparatorForElementFieldsWithNames(
                TestMdmParamUtils.MODIFICATION_INFO_COMPARATOR, "modificationInfo")
            .containsExactlyInAnyOrder(
                TestMdmParamUtils.createSskuSilverParamValue(
                    KnownMdmParams.SERVICE_EXISTS, "serviceExists", true,
                    MasterDataSourceType.SUPPLIER, ImpersonalSourceId.DATACAMP.name(), serviceKey1,
                    MASTER_DATA_VERSION + 1,
                    SskuSilverParamValue.SskuSilverTransportType.DATACAMP),
                TestMdmParamUtils.createSskuSilverParamValue(
                    KnownMdmParams.DATACAMP_MASTER_DATA_VERSION, "datacampMDVersion", MASTER_DATA_VERSION + 1,
                    MasterDataSourceType.SUPPLIER, ImpersonalSourceId.DATACAMP.name(), serviceKey1,
                    MASTER_DATA_VERSION + 1,
                    SskuSilverParamValue.SskuSilverTransportType.DATACAMP));

    }

    private void assertThatVersionsAreCorrect(Map<ShopSkuKey, List<SskuSilverParamValue>> silvers,
                                              Map<ShopSkuKey, Long> expectedVersions) {
        silvers.forEach((key, paramValues) -> {
            SskuSilverParamValue mdVersionParamValue = paramValues.stream()
                .filter(paramValue -> paramValue.getMdmParamId() == KnownMdmParams.DATACAMP_MASTER_DATA_VERSION)
                .findFirst().get();
            Assertions.assertThat(mdVersionParamValue.getNumeric().get().longValue())
                .isEqualTo(expectedVersions.get(key));
        });
    }

    private List<DatacampMessageOuterClass.DatacampMessage> getDatacampMessages(int num) {
        List<DatacampMessageOuterClass.DatacampMessage> messages = new ArrayList<>();

        int n = 0;
        while (n < num / 3) {
            MessageBatch batch1 = DatacampOffersTestUtil.createTestMessage(
                DatacampOffersTestUtil.createUnitedOffer(
                    DataCampContentStatus.MappingConfidence.MAPPING_CONFIDENCE_CONTENT, META,
                    DatacampOffersTestUtil.createServiceOfferBuilder(BUSINESS_ID, SUPPLIER_ID_1)));
            MessageBatch batch2 = DatacampOffersTestUtil.createTestMessage(
                DatacampOffersTestUtil.createUnitedOffer(
                    DataCampContentStatus.MappingConfidence.MAPPING_CONFIDENCE_CONTENT, META,
                    DatacampOffersTestUtil.createServiceOfferBuilder(BUSINESS_ID, SUPPLIER_ID_2)));
            MessageBatch batch3 = DatacampOffersTestUtil.createTestMessage(DatacampOffersTestUtil.createUnitedOffer(
                DataCampContentStatus.MappingConfidence.MAPPING_CONFIDENCE_CONTENT, META,
                DatacampOffersTestUtil.createServiceOfferBuilder(ANOTHER_BUSINESS_ID, SUPPLIER_ID_1)));

            messages.addAll(DatacampOffersTestUtil.getDataCampMessages(batch1));
            messages.addAll(DatacampOffersTestUtil.getDataCampMessages(batch2));
            messages.addAll(DatacampOffersTestUtil.getDataCampMessages(batch3));

            n++;
        }

        return messages;
    }

    private static MappingCacheDao mappingCacheDao(ShopSkuKey shopSkuKey, long mskuId) {
        return new MappingCacheDao()
            .setSupplierId(shopSkuKey.getSupplierId())
            .setShopSku(shopSkuKey.getShopSku())
            .setMskuId(mskuId)
            .setCategoryId(111)
            .setVersionTimestamp(Instant.ofEpochSecond(1L));
    }
}
