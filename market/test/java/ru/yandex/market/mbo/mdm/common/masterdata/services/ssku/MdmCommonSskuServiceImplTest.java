package ru.yandex.market.mbo.mdm.common.masterdata.services.ssku;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.collect.Sets;
import io.github.benas.randombeans.api.EnhancedRandom;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.application.monitoring.ComplexMonitoring;
import ru.yandex.market.ir.http.MdmIrisPayload;
import ru.yandex.market.mbo.mdm.common.masterdata.model.MappingCacheDao;
import ru.yandex.market.mbo.mdm.common.masterdata.model.MasterDataSource;
import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.MasterDataSourceType;
import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.blocks.DimensionsBlock;
import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.blocks.MasterDataIntoBlocksSplitter;
import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.blocks.WeightDimensionsSilverItemSplitter;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.MdmParam;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.MdmParamIoType;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.MdmParamValue;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.SskuParamValue;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.SskuSilverParamValue;
import ru.yandex.market.mbo.mdm.common.masterdata.model.queue.MdmEnqueueReason;
import ru.yandex.market.mbo.mdm.common.masterdata.model.queue.MdmQueueInfoBase;
import ru.yandex.market.mbo.mdm.common.masterdata.model.queue.SskuToRefreshInfo;
import ru.yandex.market.mbo.mdm.common.masterdata.model.ssku.CommonSsku;
import ru.yandex.market.mbo.mdm.common.masterdata.model.ssku.MdmSskuSearchFilter;
import ru.yandex.market.mbo.mdm.common.masterdata.model.ssku.ServiceSsku;
import ru.yandex.market.mbo.mdm.common.masterdata.model.ssku.SilverCommonSsku;
import ru.yandex.market.mbo.mdm.common.masterdata.model.ssku.SilverSskuKey;
import ru.yandex.market.mbo.mdm.common.masterdata.model.supplier.MdmSupplier;
import ru.yandex.market.mbo.mdm.common.masterdata.model.supplier.MdmSupplierSalesModel;
import ru.yandex.market.mbo.mdm.common.masterdata.model.supplier.MdmSupplierType;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.MappingsCacheRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.MdmSupplierRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.MdmUserRepositoryMock;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.ReferenceItemRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.SskuExistenceRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.param.CategoryParamValueRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.param.GoldSskuRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.param.SilverSskuRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.proto.FromIrisItemWrapper;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.proto.ItemWrapperHelper;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.proto.ReferenceItemWrapper;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.queue.MdmQueuesManager;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.queue.SskuToRefreshRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.services.SupplierConverterServiceMock;
import ru.yandex.market.mbo.mdm.common.masterdata.services.business.MasterDataBusinessMergeService;
import ru.yandex.market.mbo.mdm.common.masterdata.services.business.MdmMergeSettingsCache;
import ru.yandex.market.mbo.mdm.common.masterdata.services.business.MdmSskuGroupManager;
import ru.yandex.market.mbo.mdm.common.masterdata.services.iris.BeruIdMock;
import ru.yandex.market.mbo.mdm.common.masterdata.services.iris.RslGoldenItemService;
import ru.yandex.market.mbo.mdm.common.masterdata.services.iris.SurplusAndCisGoldenItemServiceMock;
import ru.yandex.market.mbo.mdm.common.masterdata.services.iris.WeightDimensionForceInheritanceService;
import ru.yandex.market.mbo.mdm.common.masterdata.services.iris.WeightDimensionsForceInheritancePostProcessor;
import ru.yandex.market.mbo.mdm.common.masterdata.services.iris.WeightDimensionsGoldenItemService;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownMdmParams;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.MasterDataGoldenItemService;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.MdmParamCache;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.MdmParamProvider;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.ServiceSskuConverter;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.TestMdmParamUtils;
import ru.yandex.market.mbo.mdm.common.masterdata.services.ssku.processing.SskuCalculatingProcessorImpl;
import ru.yandex.market.mbo.mdm.common.masterdata.services.ssku.processing.SskuProcessingContextProvider;
import ru.yandex.market.mbo.mdm.common.masterdata.services.ssku.processing.SskuProcessingContextProviderImpl;
import ru.yandex.market.mbo.mdm.common.masterdata.services.ssku.processing.SskuProcessingDataProvider;
import ru.yandex.market.mbo.mdm.common.masterdata.services.ssku.processing.SskuProcessingPipeProcessor;
import ru.yandex.market.mbo.mdm.common.masterdata.services.ssku.processing.SskuProcessingPipeProcessorImpl;
import ru.yandex.market.mbo.mdm.common.masterdata.services.ssku.processing.SskuProcessingPostProcessor;
import ru.yandex.market.mbo.mdm.common.masterdata.services.ssku.processing.SskuProcessingPostProcessorImpl;
import ru.yandex.market.mbo.mdm.common.masterdata.services.ssku.processing.SskuProcessingPreProcessor;
import ru.yandex.market.mbo.mdm.common.masterdata.services.ssku.processing.SskuProcessingPreProcessorImpl;
import ru.yandex.market.mbo.mdm.common.masterdata.services.ssku.processing.SskuToRefreshProcessingService;
import ru.yandex.market.mbo.mdm.common.masterdata.services.ssku.processing.SskuToRefreshProcessingServiceImpl;
import ru.yandex.market.mbo.mdm.common.masterdata.services.ssku.processing.SskuVerdictProcessor;
import ru.yandex.market.mbo.mdm.common.masterdata.services.ssku.processing.SskuVerdictProcessorImpl;
import ru.yandex.market.mbo.mdm.common.masterdata.services.verdict.MasterDataValidationService;
import ru.yandex.market.mbo.mdm.common.masterdata.services.verdict.MasterDataVersionMapService;
import ru.yandex.market.mbo.mdm.common.masterdata.services.verdict.VerdictCalculationHelper;
import ru.yandex.market.mbo.mdm.common.masterdata.validator.CachedItemBlockValidationContextProvider;
import ru.yandex.market.mbo.mdm.common.masterdata.validator.CachedItemBlockValidationContextProviderImpl;
import ru.yandex.market.mbo.mdm.common.masterdata.validator.WeightDimensionBlockValidationService;
import ru.yandex.market.mbo.mdm.common.masterdata.validator.WeightDimensionBlockValidationServiceImpl;
import ru.yandex.market.mbo.mdm.common.masterdata.validator.WeightDimensionsValidator;
import ru.yandex.market.mbo.mdm.common.masterdata.validator.block.MasterDataBlocksValidationService;
import ru.yandex.market.mbo.mdm.common.service.AllOkMasterDataValidator;
import ru.yandex.market.mbo.mdm.common.service.FeatureSwitchingAssistant;
import ru.yandex.market.mbo.mdm.common.util.SskuGoldenParamUtil;
import ru.yandex.market.mbo.mdm.common.utils.MdmBaseDbTestClass;
import ru.yandex.market.mbo.storage.StorageKeyValueService;
import ru.yandex.market.mboc.common.MbocErrors;
import ru.yandex.market.mboc.common.infrastructure.sql.TransactionHelper;
import ru.yandex.market.mboc.common.masterdata.TestDataUtils;
import ru.yandex.market.mboc.common.masterdata.model.MasterData;
import ru.yandex.market.mboc.common.masterdata.model.QualityDocument;
import ru.yandex.market.mboc.common.masterdata.parsing.SskuMasterDataFields;
import ru.yandex.market.mboc.common.masterdata.repository.MasterDataRepository;
import ru.yandex.market.mboc.common.masterdata.repository.document.QualityDocumentRepository;
import ru.yandex.market.mboc.common.masterdata.services.SskuMasterDataStorageService;
import ru.yandex.market.mboc.common.masterdata.services.cutoff.OfferCutoffService;
import ru.yandex.market.mboc.common.offers.model.ShopSkuKey;
import ru.yandex.market.mboc.common.utils.ErrorInfo;
import ru.yandex.market.mboc.common.utils.MdmProperties;

import static ru.yandex.market.mbo.mdm.common.masterdata.repository.queue.BatchProcessingProperties.BatchProcessingPropertiesBuilder.constantBatchProperties;

/**
 * @author amaslak
 */
@SuppressWarnings("checkstyle:MagicNumber")
public class MdmCommonSskuServiceImplTest extends MdmBaseDbTestClass {
    private static final int SUPPLIER_ID_1 = 42;
    private static final int SUPPLIER_ID_2 = 777;
    private static final int SUPPLIER_ID_3 = 888;
    private static final int SUPPLIER_ID_4 = 999;
    private static final int SUPPLIER_ID_WHITE = 111;
    private static final int SUPPLIER_ID_UNKNOWN = 222;
    private static final int BUSINESS_ID = 43;

    @Autowired
    private MdmParamProvider mdmParamProvider;
    @Autowired
    private MasterDataRepository masterDataRepository;
    @Autowired
    private ReferenceItemRepository referenceItemRepository;
    @Autowired
    private MdmQueuesManager mdmQueuesManager;
    @Autowired
    private MappingsCacheRepository mappingsCacheRepository;
    @Autowired
    private TransactionHelper transactionHelper;
    @Autowired
    private ServiceSskuConverter converter;
    @Autowired
    private QualityDocumentRepository qualityDocumentRepository;
    @Autowired
    private BeruIdMock beruId;
    @Autowired
    private MdmSskuGroupManager mdmSskuGroupManager;
    @Autowired
    private MasterDataBusinessMergeService masterDataBusinessMergeService;
    @Autowired
    private MdmSupplierRepository supplierRepository;
    @Autowired
    private SilverSskuRepository silverSskuRepository;
    @Autowired
    private GoldSskuRepository goldSskuRepository;
    @Autowired
    private StorageKeyValueService storageKeyValueService;
    @Autowired
    private SskuToRefreshRepository sskuToRefreshRepository;
    @Autowired
    private MdmMergeSettingsCache mdmMergeSettingsCache;
    @Autowired
    private SskuExistenceRepository sskuExistenceRepository;
    @Autowired
    private SskuGoldenParamUtil sskuGoldenParamUtil;
    @Autowired
    private MultivalueBusinessHelper multivalueBusinessHelper;
    @Autowired
    private RslGoldenItemService rslGoldenItemService;
    @Autowired
    private MasterDataVersionMapService masterDataVersionMapService;
    @Autowired
    private VerdictCalculationHelper verdictCalculationHelper;
    @Autowired
    private SskuProcessingDataProvider sskuProcessingDataProvider;
    @Autowired
    private FeatureSwitchingAssistant featureSwitchingAssistant;
    @Autowired
    private MasterDataBlocksValidationService masterDataBlocksValidationService;
    @Autowired
    private WeightDimensionBlockValidationService weightDimensionBlockValidationService;
    @Autowired
    private MasterDataIntoBlocksSplitter masterDataIntoBlocksSplitter;
    @Autowired
    private CategoryParamValueRepository categoryParamValueRepository;
    @Autowired
    private MasterDataGoldenItemService masterDataGoldenItemService;
    @Autowired
    private MdmParamCache mdmParamCache;
    @Autowired
    private TraceableSskuGoldenItemService traceableSskuGoldenItemService;
    @Autowired
    protected WeightDimensionsValidator weightDimensionsValidator;

    private MdmCommonSskuService mdmCommonSskuService;
    private SskuToRefreshProcessingService sskuToRefreshProcessingService;
    private EnhancedRandom random;

    @Before
    public void setup() {
        random = TestDataUtils.defaultRandom(170690L);
        SskuMasterDataStorageService sskuMasterDataStorageService = new SskuMasterDataStorageService(
            masterDataRepository,
            qualityDocumentRepository,
            transactionHelper,
            new SupplierConverterServiceMock(),
            new ComplexMonitoring());
        mdmCommonSskuService = new MdmCommonSskuServiceImpl(
            sskuMasterDataStorageService,
            referenceItemRepository,
            converter,
            mdmQueuesManager,
            transactionHelper,
            categoryParamValueRepository,
            mappingsCacheRepository,
            beruId,
            mdmSskuGroupManager,
            masterDataBusinessMergeService,
            goldSskuRepository,
            mdmParamProvider,
            mdmMergeSettingsCache,
            silverSskuRepository,
            supplierRepository,
            weightDimensionBlockValidationService,
            masterDataBlocksValidationService,
            masterDataIntoBlocksSplitter,
            storageKeyValueService,
            new MdmUserRepositoryMock()
        );

        CachedItemBlockValidationContextProvider validationContextProvider =
            new CachedItemBlockValidationContextProviderImpl(storageKeyValueService);
        var itemBlockValidationService = new WeightDimensionBlockValidationServiceImpl(
            validationContextProvider,
            weightDimensionsValidator
        );
        var weightDimensionsService = new WeightDimensionsGoldenItemService(
            new WeightDimensionsSilverItemSplitter(new SupplierConverterServiceMock()),
            itemBlockValidationService,
            featureSwitchingAssistant);
        var forceInheritanceService = new WeightDimensionForceInheritanceService(
            featureSwitchingAssistant,
            new WeightDimensionsForceInheritancePostProcessor(),
            itemBlockValidationService
        );
        SskuGoldenReferenceItemCalculationHelper goldenReferenceItemCalculator =
            new SskuGoldenReferenceItemCalculationHelper(
                weightDimensionsService,
                forceInheritanceService,
                new SurplusAndCisGoldenItemServiceMock(),
                storageKeyValueService,
                converter,
                masterDataBusinessMergeService,
                Mockito.mock(OfferCutoffService.class),
                sskuGoldenParamUtil,
                validationContextProvider,
                weightDimensionsValidator,
                rslGoldenItemService
            );

        SskuGoldenMasterDataCalculationHelper goldenMasterDataCalculator =
            new SskuGoldenMasterDataCalculationHelper(
                converter,
                new MasterDataValidationService(new AllOkMasterDataValidator()),
                masterDataBusinessMergeService,
                storageKeyValueService,
                masterDataGoldenItemService,
                sskuGoldenParamUtil,
                traceableSskuGoldenItemService,
                multivalueBusinessHelper
            );

        sskuToRefreshProcessingService = new SskuToRefreshProcessingServiceImpl(
            sskuProcessingDataProvider,
            sskuVerdictProcessor(),
            sskuProcessingPostProcessor(goldenReferenceItemCalculator),
            sskuProcessingContextProvider(),
            sskuProcessingPipeProcessor(),
            sskuProcessingPreProcessor(),
            sskuCalculatingProcessor(goldenReferenceItemCalculator, goldenMasterDataCalculator));

        supplierRepository.insertBatch(
            Stream.of(SUPPLIER_ID_1, SUPPLIER_ID_2, SUPPLIER_ID_3)
                .map(id -> new MdmSupplier()
                    .setId(id)
                    .setType(MdmSupplierType.THIRD_PARTY))
                .collect(Collectors.toList()));
        supplierRepository.insert(
            new MdmSupplier()
                .setId(SUPPLIER_ID_WHITE)
                .setType(MdmSupplierType.MARKET_SHOP));
        supplierRepository.insert(
            new MdmSupplier()
                .setId(BUSINESS_ID)
                .setType(MdmSupplierType.BUSINESS));

        storageKeyValueService.putValue(MdmProperties.ENABLE_BUSINESS_STATE_UPDATE_IN_MDM_SUPPLIER_CACHE, true);
        storageKeyValueService.putValue(MdmProperties.BUSINESS_MERGE_ENABLED_KEY, true);
        storageKeyValueService.invalidateCache();
    }


    private SskuCalculatingProcessorImpl sskuCalculatingProcessor(SskuGoldenReferenceItemCalculationHelper
                                                                      sskuGoldenReferenceItemCalculationHelper,
                                                                  SskuGoldenMasterDataCalculationHelper
                                                                      sskuGoldenMasterDataCalculationHelper) {
        return new SskuCalculatingProcessorImpl(
            sskuGoldenReferenceItemCalculationHelper,
            sskuGoldenMasterDataCalculationHelper,
            referenceItemRepository,
            masterDataRepository,
            goldSskuRepository
        );
    }

    private SskuProcessingContextProvider sskuProcessingContextProvider() {
        return new SskuProcessingContextProviderImpl(storageKeyValueService);
    }

    private SskuProcessingPipeProcessor sskuProcessingPipeProcessor() {
        return new SskuProcessingPipeProcessorImpl(mdmQueuesManager, converter);
    }

    private SskuProcessingPostProcessor sskuProcessingPostProcessor(SskuGoldenReferenceItemCalculationHelper
                                                                        sskuGoldenReferenceItemCalculationHelper) {
        return new SskuProcessingPostProcessorImpl(sskuGoldenReferenceItemCalculationHelper);
    }

    private SskuProcessingPreProcessor sskuProcessingPreProcessor() {
        return new SskuProcessingPreProcessorImpl(mdmSskuGroupManager);
    }

    private SskuVerdictProcessor sskuVerdictProcessor() {
        return new SskuVerdictProcessorImpl(
            converter,
            masterDataBusinessMergeService,
            masterDataVersionMapService,
            verdictCalculationHelper);
    }

    @Test
    public void whenFindSskuShouldFillMasterDataCategory() {
        ShopSkuKey key = new ShopSkuKey(SUPPLIER_ID_1, "унесенные-ветром");

        MappingCacheDao mappingCacheDao = new MappingCacheDao();
        mappingCacheDao.setShopSkuKey(key);
        int categoryId = 56;
        mappingCacheDao.setCategoryId(categoryId);
        mappingsCacheRepository.insert(mappingCacheDao);

        QualityDocument[] documents = {
            TestDataUtils.generateCorrectDocument(random),
            TestDataUtils.generateCorrectDocument(random),
            TestDataUtils.generateCorrectDocument(random)
        };
        MasterData masterData = TestDataUtils.generateMasterData(key, random, documents);
        masterData.setCategoryId(98L);
        masterDataRepository.insert(masterData);

        List<CommonSsku> sskus = mdmCommonSskuService.find(new MdmSskuSearchFilter().setShopSkuKeys(
            List.of(key)
        ));
        Assertions.assertThat(sskus).hasSize(1);
        CommonSsku ssku = sskus.get(0);
        MasterData restored = converter.toMasterData(ssku.getBaseSsku());
        Assertions.assertThat(restored.getCategoryId()).isEqualTo(Long.valueOf(categoryId));
    }

    @Test
    public void whenHasDocumentsButPassedWithoutThemFromUIShouldStillFetchThem() {
        ShopSkuKey key = new ShopSkuKey(SUPPLIER_ID_1, "унесенные-ветром");

        MappingCacheDao mappingCacheDao = new MappingCacheDao();
        mappingCacheDao.setShopSkuKey(key);
        int categoryId = 56;
        mappingCacheDao.setCategoryId(categoryId);
        mappingCacheDao.setMskuId(0L);
        mappingsCacheRepository.insert(mappingCacheDao);

        QualityDocument[] documents = {
            TestDataUtils.generateCorrectDocument(random),
            TestDataUtils.generateCorrectDocument(random),
            TestDataUtils.generateCorrectDocument(random)
        };
        MasterData masterData = TestDataUtils.generateMasterData(key, random, documents);
        masterData.setCategoryId(98L);
        masterDataRepository.insert(masterData);

        CommonSsku ssku = new CommonSsku(key);
        ssku.addBaseValue(new SskuParamValue()
            .setShopSkuKey(key)
            .setMdmParamId(KnownMdmParams.MANUFACTURER_COUNTRY)
            .setString("Россия")
            .setXslName("manufacturerCountry")
        );

        mdmCommonSskuService.update(List.of(ssku));

        assertSskuEnqueued(List.of(key));
        processSskuToRefresh();

        List<CommonSsku> sskus = mdmCommonSskuService.find(new MdmSskuSearchFilter().setShopSkuKeys(
            List.of(key)
        ));
        Assertions.assertThat(sskus).hasSize(1);
        CommonSsku found = sskus.get(0);
        Assertions.assertThat(found.getBaseValuesByParamId().get(KnownMdmParams.DOCUMENT_REG_NUMBER).getStrings())
            .containsExactlyInAnyOrderElementsOf(Stream.of(documents)
                .map(QualityDocument::getRegistrationNumber).collect(Collectors.toList()));
    }

    @Test
    public void whenFindSskuShouldAddMskuIdReferenceInParams() {
        ShopSkuKey key = new ShopSkuKey(SUPPLIER_ID_1, "унесенные-ветром");

        MappingCacheDao mappingCacheDao = new MappingCacheDao();
        mappingCacheDao.setShopSkuKey(key);
        long mskuId = 123;
        mappingCacheDao.setMskuId(mskuId);
        mappingsCacheRepository.insert(mappingCacheDao);

        QualityDocument[] documents = {
            TestDataUtils.generateCorrectDocument(random),
            TestDataUtils.generateCorrectDocument(random),
            TestDataUtils.generateCorrectDocument(random)
        };
        MasterData masterData = TestDataUtils.generateMasterData(key, random, documents);
        masterDataRepository.insert(masterData);

        List<CommonSsku> sskus = mdmCommonSskuService.find(new MdmSskuSearchFilter().setShopSkuKeys(
            List.of(key)
        ));
        Assertions.assertThat(sskus).hasSize(1);
        CommonSsku ssku = sskus.get(0);
        SskuParamValue mskuIdParam = ssku.getBaseValuesByParamId().get(KnownMdmParams.MSKU_ID_REFERENCE);
        Assertions.assertThat(mskuIdParam.getNumeric().get()).isEqualTo(BigDecimal.valueOf(mskuId));
    }

    @Test
    public void whenFindSskuShouldFillMasterDataCategoryEvenIfNonePresent() {
        ShopSkuKey key = new ShopSkuKey(SUPPLIER_ID_1, "унесенные-ветром");

        MappingCacheDao mappingCacheDao = new MappingCacheDao();
        mappingCacheDao.setShopSkuKey(key);
        int categoryId = 56;
        mappingCacheDao.setCategoryId(categoryId);
        mappingsCacheRepository.insert(mappingCacheDao);

        QualityDocument[] documents = {
            TestDataUtils.generateCorrectDocument(random),
            TestDataUtils.generateCorrectDocument(random),
            TestDataUtils.generateCorrectDocument(random)
        };
        MasterData masterData = TestDataUtils.generateMasterData(key, random, documents);
        masterDataRepository.insert(masterData);

        List<CommonSsku> sskus = mdmCommonSskuService.find(new MdmSskuSearchFilter().setShopSkuKeys(
            List.of(key)
        ));
        Assertions.assertThat(sskus).hasSize(1);
        CommonSsku ssku = sskus.get(0);
        MasterData restored = converter.toMasterData(ssku.getBaseSsku());
        Assertions.assertThat(restored.getCategoryId()).isEqualTo(Long.valueOf(categoryId));
    }

    @Test
    public void whenFindSskuShouldFillMasterDataParamValues() {
        ShopSkuKey key = new ShopSkuKey(SUPPLIER_ID_1, "унесенные-ветром");
        QualityDocument[] documents = {
            TestDataUtils.generateCorrectDocument(random),
            TestDataUtils.generateCorrectDocument(random),
            TestDataUtils.generateCorrectDocument(random)
        };
        MasterData masterData = TestDataUtils.generateMasterData(key, random, documents);
        masterDataRepository.insert(masterData);

        List<CommonSsku> sskus = mdmCommonSskuService.find(new MdmSskuSearchFilter().setShopSkuKeys(
            List.of(key)
        ));
        Assertions.assertThat(sskus).hasSize(1);
        CommonSsku ssku = sskus.get(0);
        MasterData restored = converter.toMasterData(ssku.getBaseSsku());
        Assertions.assertThat(restored.equalsByStoredFields(masterData)).isTrue();
        Assertions.assertThat(restored.getRegNumbers()).containsExactlyInAnyOrderElementsOf(
            Stream.of(documents).map(QualityDocument::getRegistrationNumber).collect(Collectors.toList())
        );
    }

    @Test
    public void whenFindSskuShouldFillReferenceInformationParamValues() {
        ShopSkuKey key = new ShopSkuKey(SUPPLIER_ID_1, "остров-сокровищ");
        ReferenceItemWrapper referenceItem = generateReferenceItem(key);
        referenceItemRepository.insert(referenceItem);

        List<CommonSsku> sskus = mdmCommonSskuService.find(new MdmSskuSearchFilter().setShopSkuKeys(
            List.of(key)
        ));
        Assertions.assertThat(sskus).hasSize(1);
        CommonSsku ssku = sskus.get(0);
        ReferenceItemWrapper restored = converter.toReferenceItem(ssku.getBaseSsku());
        Assertions.assertThat(restored.equalsByDataExcludingTsAndRsl(referenceItem, false)).isTrue();
    }

    @Test
    public void whenFindSskuShouldBeAbleToFindByMskuId() {
        ShopSkuKey key1 = new ShopSkuKey(SUPPLIER_ID_2, "zzz");
        ShopSkuKey key2 = new ShopSkuKey(SUPPLIER_ID_3, "yyy");
        long mskuId1 = 1234567L;
        long mskuId2 = 7777777L;
        ReferenceItemWrapper referenceItem1 = generateReferenceItem(key1);
        ReferenceItemWrapper referenceItem2 = generateReferenceItem(key2);
        referenceItemRepository.insertOrUpdateAll(List.of(referenceItem1, referenceItem2));
        MappingCacheDao mapping1 = new MappingCacheDao()
            .setShopSkuKey(key1)
            .setMskuId(mskuId1)
            .setCategoryId(2021);
        MappingCacheDao mapping2 = new MappingCacheDao()
            .setShopSkuKey(key2)
            .setMskuId(mskuId2)
            .setCategoryId(2021);
        mappingsCacheRepository.insertOrUpdateAll(List.of(mapping1, mapping2));

        List<CommonSsku> sskus = mdmCommonSskuService.find(new MdmSskuSearchFilter().setMskuIdSearchString(
            mskuId1 + "," + mskuId2
        ));
        Assertions.assertThat(sskus).hasSize(2);
        ReferenceItemWrapper restored1 = converter.toReferenceItem(sskus.get(0).getBaseSsku());
        ReferenceItemWrapper restored2 = converter.toReferenceItem(sskus.get(1).getBaseSsku());
        Assertions.assertThat(restored1.getKey().getShopSku().equals(referenceItem1.getShopSku()) ?
            restored1.equalsByDataExcludingTsAndRsl(referenceItem1, false)
            : restored1.equalsByDataExcludingTsAndRsl(referenceItem2, false)).isTrue();
        Assertions.assertThat(restored2.getKey().getShopSku().equals(referenceItem1.getShopSku()) ?
            restored2.equalsByDataExcludingTsAndRsl(referenceItem1, false)
            : restored2.equalsByDataExcludingTsAndRsl(referenceItem2, false)).isTrue();
    }

    @Test
    public void whenFindBySskuShouldNotReturnWhiteOrUnknown() {
        ShopSkuKey key1 = new ShopSkuKey(SUPPLIER_ID_WHITE, "оффер");
        ShopSkuKey key2 = new ShopSkuKey(SUPPLIER_ID_UNKNOWN, "еще-оффер");
        ShopSkuKey key3 = new ShopSkuKey(SUPPLIER_ID_1, "унесенные-ветром");

        int categoryId = 56;
        mappingsCacheRepository.insertOrUpdateAll(
            Stream.of(key1, key2, key3)
                .map(k -> new MappingCacheDao()
                    .setShopSkuKey(k)
                    .setCategoryId(categoryId))
                .collect(Collectors.toList()));

        QualityDocument[] documents = {
            TestDataUtils.generateCorrectDocument(random),
            TestDataUtils.generateCorrectDocument(random),
            TestDataUtils.generateCorrectDocument(random)
        };

        masterDataRepository.insertBatch(
            TestDataUtils.generateMasterData(key1, random, documents),
            TestDataUtils.generateMasterData(key2, random, documents),
            TestDataUtils.generateMasterData(key3, random, documents)
        );

        List<CommonSsku> sskus = mdmCommonSskuService.find(new MdmSskuSearchFilter().setShopSkuKeys(
            List.of(key1, key2, key3)
        ));

        Assertions.assertThat(sskus).hasSize(1);
        CommonSsku ssku = sskus.get(0);
        Assertions.assertThat(ssku.getKey()).isEqualTo(key3);
    }

    @Test
    public void whenFindByMskuShouldNotReturnWhiteOrUnknown() {
        ShopSkuKey key1 = new ShopSkuKey(SUPPLIER_ID_2, "zzz");
        ShopSkuKey key2 = new ShopSkuKey(SUPPLIER_ID_3, "yyy");
        ShopSkuKey key3 = new ShopSkuKey(SUPPLIER_ID_WHITE, "оффер");
        ShopSkuKey key4 = new ShopSkuKey(SUPPLIER_ID_UNKNOWN, "еще-оффер");
        long mskuId1 = 1234567L;
        long mskuId2 = 7777777L;

        int categoryId = 56;
        mappingsCacheRepository.insertOrUpdateAll(
            Stream.of(key1, key3, key4)
                .map(k -> new MappingCacheDao()
                    .setShopSkuKey(k)
                    .setMskuId(mskuId1)
                    .setCategoryId(categoryId))
                .collect(Collectors.toList()));
        mappingsCacheRepository.insert(new MappingCacheDao()
            .setShopSkuKey(key2)
            .setMskuId(mskuId2)
            .setCategoryId(categoryId));

        QualityDocument[] documents = {
            TestDataUtils.generateCorrectDocument(random),
            TestDataUtils.generateCorrectDocument(random),
            TestDataUtils.generateCorrectDocument(random)
        };

        masterDataRepository.insertBatch(
            TestDataUtils.generateMasterData(key1, random, documents),
            TestDataUtils.generateMasterData(key2, random, documents),
            TestDataUtils.generateMasterData(key3, random, documents),
            TestDataUtils.generateMasterData(key4, random, documents)
        );

        List<CommonSsku> sskus = mdmCommonSskuService.find(new MdmSskuSearchFilter().setMskuIdSearchString(
            mskuId1 + "," + mskuId2
        ));

        Assertions.assertThat(sskus.stream().map(CommonSsku::getKey).collect(Collectors.toList()))
            .containsExactlyInAnyOrder(key1, key2);
    }

    @Test
    public void whenFindByBusinessKeyShouldReturnMergedItemWithOnlyBusinessValues() {
        storageKeyValueService.putValue(MdmProperties.BUSINESS_MERGE_ENABLED_KEY, true);

        String shopSku = "сепулькарий";
        var businessKey = new ShopSkuKey(BUSINESS_ID, shopSku);
        var serviceKey = new ShopSkuKey(SUPPLIER_ID_1, shopSku);
        supplierRepository.update(
            supplierRepository.findById(SUPPLIER_ID_1).setBusinessId(BUSINESS_ID).setBusinessEnabled(true));
        sskuExistenceRepository.markExistence(new ShopSkuKey(SUPPLIER_ID_1, shopSku), true);

        int categoryId = 56;
        var businessMapping = new MappingCacheDao().setShopSkuKey(businessKey).setCategoryId(categoryId);
        var serviceMapping = new MappingCacheDao().setShopSkuKey(serviceKey).setCategoryId(categoryId);
        mappingsCacheRepository.insertOrUpdateAll(List.of(businessMapping, serviceMapping));

        // master data & ref information is stored by service key
        MasterData masterData = TestDataUtils.generateMasterData(serviceKey, random);
        masterData.setCategoryId(98L);
        masterDataRepository.insert(masterData);

        ReferenceItemWrapper referenceItem = generateReferenceItem(serviceKey);
        referenceItemRepository.insert(referenceItem);

        List<CommonSsku> sskus = mdmCommonSskuService.find(new MdmSskuSearchFilter().setSearchString(
            BUSINESS_ID + " " + shopSku));

        Assertions.assertThat(sskus.size()).isOne();
        Assertions.assertThat(sskus.get(0).getKey()).isEqualTo(businessKey);
        Assertions.assertThat(sskus.get(0).getBaseValuesByParamId()
                .get(KnownMdmParams.MANUFACTURER_COUNTRY).getString().get())
            .isEqualTo(masterData.getManufacturerCountries().get(0));
        Assertions.assertThat(sskus.get(0).getBaseValuesByParamId()
                .get(KnownMdmParams.WEIGHT_GROSS).getNumeric().get())
            .isEqualTo(new BigDecimal(
                referenceItem.getReferenceInformation().getItemShippingUnit().getWeightGrossMg().getValue()
                    / MdmProperties.MG_IN_KG));

        Set<Long> baseMdmParamIds = mdmParamProvider.getParamsMapForIoType(MdmParamIoType.BUSINESS_SSKU_UI_VIEW, true)
            .values().stream()
            .map(MdmParam::getId)
            .collect(Collectors.toSet());

        Assertions.assertThat(
                Sets.difference(sskus.get(0).getBaseValuesByParamId().keySet(), baseMdmParamIds))
            .isEmpty();
    }

    @Test
    public void whenGetSskuShouldReturnIt() {
        var key = new ShopSkuKey(SUPPLIER_ID_1, "сепуление-сепулек");

        MasterData masterData = TestDataUtils.generateMasterData(key, random);
        masterData.setCategoryId(98L);
        masterDataRepository.insert(masterData);

        ReferenceItemWrapper referenceItem = generateReferenceItem(key);
        referenceItemRepository.insert(referenceItem);

        List<CommonSsku> sskus = mdmCommonSskuService.get(List.of(key));

        Assertions.assertThat(sskus.size()).isOne();
        Assertions.assertThat(sskus.get(0).getKey()).isEqualTo(key);
        Assertions.assertThat(sskus.get(0).getBaseValuesByParamId()
                .get(KnownMdmParams.MANUFACTURER_COUNTRY).getString().get())
            .isEqualTo(masterData.getManufacturerCountries().get(0));
        Assertions.assertThat(sskus.get(0).getBaseValuesByParamId()
                .get(KnownMdmParams.WEIGHT_GROSS).getNumeric().get())
            .isEqualTo(new BigDecimal(
                referenceItem.getReferenceInformation().getItemShippingUnit().getWeightGrossMg().getValue()
                    / MdmProperties.MG_IN_KG));
    }

    @Test
    public void whenGetSskuByBusinessKeyShouldReturnOnlyBusinessValues() {
        storageKeyValueService.putValue(MdmProperties.BUSINESS_MERGE_ENABLED_KEY, true);

        String shopSku = "сепуление-сепулек";
        var businessKey = new ShopSkuKey(BUSINESS_ID, shopSku);
        var serviceKey = new ShopSkuKey(SUPPLIER_ID_1, shopSku);
        supplierRepository.update(
            supplierRepository.findById(SUPPLIER_ID_1).setBusinessId(BUSINESS_ID).setBusinessEnabled(true));
        sskuExistenceRepository.markExistence(new ShopSkuKey(SUPPLIER_ID_1, shopSku), true);

        // master data & ref information is stored by service key
        MasterData masterData = TestDataUtils.generateMasterData(serviceKey, random);
        masterData.setCategoryId(98L);
        masterDataRepository.insert(masterData);

        ReferenceItemWrapper referenceItem = generateReferenceItem(serviceKey);
        referenceItemRepository.insert(referenceItem);

        List<CommonSsku> sskus = mdmCommonSskuService.get(List.of(businessKey));

        Assertions.assertThat(sskus.size()).isOne();
        Assertions.assertThat(sskus.get(0).getKey()).isEqualTo(businessKey);
        Assertions.assertThat(sskus.get(0).getBaseValuesByParamId()
                .get(KnownMdmParams.MANUFACTURER_COUNTRY).getString().get())
            .isEqualTo(masterData.getManufacturerCountries().get(0));
        Assertions.assertThat(sskus.get(0).getBaseValuesByParamId()
                .get(KnownMdmParams.WEIGHT_GROSS).getNumeric().get())
            .isEqualTo(new BigDecimal(
                referenceItem.getReferenceInformation().getItemShippingUnit().getWeightGrossMg().getValue()
                    / MdmProperties.MG_IN_KG));

        Set<Long> baseMdmParamIds = mdmParamProvider.getParamsMapForIoType(MdmParamIoType.BUSINESS_SSKU_UI_VIEW, true)
            .values().stream()
            .map(MdmParam::getId)
            .collect(Collectors.toSet());

        Assertions.assertThat(
                Sets.difference(sskus.get(0).getBaseValuesByParamId().keySet(), baseMdmParamIds))
            .isEmpty();
    }

    @Test
    public void whenGetSskuByServiceKeyShouldReturnMergedBusinessAndServiceValues() {
        storageKeyValueService.putValue(MdmProperties.BUSINESS_MERGE_ENABLED_KEY, true);

        String shopSku = "сепуление-сепулек";
        var serviceKey = new ShopSkuKey(SUPPLIER_ID_1, shopSku);
        supplierRepository.update(
            supplierRepository.findById(SUPPLIER_ID_1).setBusinessId(BUSINESS_ID).setBusinessEnabled(true));
        sskuExistenceRepository.markExistence(new ShopSkuKey(SUPPLIER_ID_1, shopSku), true);

        // master data & ref information is stored by service key
        MasterData masterData = TestDataUtils.generateMasterData(serviceKey, random);
        masterData.setCategoryId(98L);
        masterDataRepository.insert(masterData);

        ReferenceItemWrapper referenceItem = generateReferenceItem(serviceKey);
        referenceItemRepository.insert(referenceItem);

        List<CommonSsku> sskus = mdmCommonSskuService.get(List.of(serviceKey));

        Assertions.assertThat(sskus.size()).isOne();
        Assertions.assertThat(sskus.get(0).getKey()).isEqualTo(serviceKey);
        Assertions.assertThat(sskus.get(0).getBaseValuesByParamId()
                .get(KnownMdmParams.MANUFACTURER_COUNTRY).getString().get())
            .isEqualTo(masterData.getManufacturerCountries().get(0));
        Assertions.assertThat(sskus.get(0).getBaseValuesByParamId()
                .get(KnownMdmParams.WEIGHT_GROSS).getNumeric().get())
            .isEqualTo(new BigDecimal(
                referenceItem.getReferenceInformation().getItemShippingUnit().getWeightGrossMg().getValue()
                    / MdmProperties.MG_IN_KG));
        Assertions.assertThat(sskus.get(0).getServiceSskus().size()).isOne();
        Assertions.assertThat(sskus.get(0).getServiceSsku(SUPPLIER_ID_1).get().getValuesByParamId()
                .get(KnownMdmParams.TRANSPORT_UNIT_SIZE).getNumeric().get())
            .isEqualTo(new BigDecimal(masterData.getTransportUnitSize()));
    }

    @Test
    public void whenGetSskuByRelatedBusinessAndServiceKeysShouldReturnOnlyOneEntry() {
        storageKeyValueService.putValue(MdmProperties.BUSINESS_MERGE_ENABLED_KEY, true);

        String shopSku = "сепуление-сепулек";
        var businessKey = new ShopSkuKey(BUSINESS_ID, shopSku);
        var serviceKey = new ShopSkuKey(SUPPLIER_ID_1, shopSku);
        supplierRepository.update(
            supplierRepository.findById(SUPPLIER_ID_1).setBusinessId(BUSINESS_ID).setBusinessEnabled(true));
        sskuExistenceRepository.markExistence(new ShopSkuKey(SUPPLIER_ID_1, shopSku), true);

        // master data & ref information is stored by service key
        MasterData masterData = TestDataUtils.generateMasterData(serviceKey, random);
        masterData.setCategoryId(98L);
        masterDataRepository.insert(masterData);

        ReferenceItemWrapper referenceItem = generateReferenceItem(serviceKey);
        referenceItemRepository.insert(referenceItem);

        List<CommonSsku> sskus = mdmCommonSskuService.get(List.of(businessKey, serviceKey));

        Assertions.assertThat(sskus.size()).isOne();
        Assertions.assertThat(sskus.get(0).getKey()).isEqualTo(serviceKey);
        Assertions.assertThat(sskus.get(0).getBaseValuesByParamId()
                .get(KnownMdmParams.MANUFACTURER_COUNTRY).getString().get())
            .isEqualTo(masterData.getManufacturerCountries().get(0));
        Assertions.assertThat(sskus.get(0).getBaseValuesByParamId()
                .get(KnownMdmParams.WEIGHT_GROSS).getNumeric().get())
            .isEqualTo(new BigDecimal(
                referenceItem.getReferenceInformation().getItemShippingUnit().getWeightGrossMg().getValue()
                    / MdmProperties.MG_IN_KG));
        Assertions.assertThat(sskus.get(0).getServiceSskus().size()).isOne();
        Assertions.assertThat(sskus.get(0).getServiceSsku(SUPPLIER_ID_1).get().getValuesByParamId()
                .get(KnownMdmParams.TRANSPORT_UNIT_SIZE).getNumeric().get())
            .isEqualTo(new BigDecimal(masterData.getTransportUnitSize()));
    }

    @Test
    public void whenGetByServiceSskuShouldReturnIsDbsMdmParamValue() {
        // given
        createAndSaveDbsSupplier();

        String shopSku = "чебурашка";
        var serviceKey = new ShopSkuKey(SUPPLIER_ID_4, shopSku);
        sskuExistenceRepository.markExistence(new ShopSkuKey(SUPPLIER_ID_4, shopSku), true);

        // master data & ref information is stored by service key
        MasterData masterData = TestDataUtils.generateMasterData(serviceKey, random);
        masterData.setCategoryId(98L);
        masterDataRepository.insert(masterData);

        ReferenceItemWrapper referenceItem = generateReferenceItem(serviceKey);
        referenceItemRepository.insert(referenceItem);

        // when
        List<CommonSsku> sskus = mdmCommonSskuService.get(List.of(serviceKey));

        // then
        Assertions.assertThat(sskus.size()).isOne();
        Assertions.assertThat(sskus.get(0).getKey()).isEqualTo(serviceKey);
        Assertions.assertThat(sskus.get(0).getServiceSskus().size()).isOne();
        Assertions.assertThat(sskus.get(0).getBaseValuesByParamId().get(KnownMdmParams.IS_DBS).getBool().get())
            .isTrue();
    }

    @Test
    public void whenGetByBusinessSskuShouldNotReturnIsDbsMdmParamValue() {
        // given
        createAndSaveDbsSupplier();

        String shopSku = "чебурашка";
        var businessKey = new ShopSkuKey(BUSINESS_ID, shopSku);
        var serviceKey = new ShopSkuKey(SUPPLIER_ID_4, shopSku);
        sskuExistenceRepository.markExistence(new ShopSkuKey(SUPPLIER_ID_4, shopSku), true);

        // master data & ref information is stored by service key
        MasterData masterData = TestDataUtils.generateMasterData(serviceKey, random);
        masterData.setCategoryId(98L);
        masterDataRepository.insert(masterData);

        ReferenceItemWrapper referenceItem = generateReferenceItem(serviceKey);
        referenceItemRepository.insert(referenceItem);

        // when
        List<CommonSsku> sskus = mdmCommonSskuService.get(List.of(businessKey));

        // then
        Assertions.assertThat(sskus.size()).isOne();
        Assertions.assertThat(sskus.get(0).getKey()).isEqualTo(businessKey);
        Assertions.assertThat(sskus.get(0).getServiceSskus().size()).isOne();
        Assertions.assertThat(sskus.get(0).getBaseValuesByParamId()).doesNotContainKey(KnownMdmParams.IS_DBS);
        Assertions.assertThat(sskus.get(0).getServiceSsku(SUPPLIER_ID_4).get().getValuesByParamId())
            .doesNotContainKey(KnownMdmParams.IS_DBS);
    }

    @Test
    public void whenUpdateNonexistingSskuShouldReturnError() {
        ShopSkuKey key = new ShopSkuKey(SUPPLIER_ID_1, "преступление-и-наказание");
        MasterData prototype = TestDataUtils.generateMasterData(key, random);
        ServiceSsku ssku = converter.toServiceSsku(prototype, (FromIrisItemWrapper) null);
        List<ErrorInfo> errors = mdmCommonSskuService.update(List.of(new CommonSsku(key).setBaseSsku(ssku))).get(key);
        Assertions.assertThat(errors.get(0).render()).isEqualToIgnoringCase("SSKU не найдена");
        assertNoSskuEnqueued();
    }

    @Test
    public void whenUpdateInvalidInputShouldReturnValidationErrors() {
        ShopSkuKey key = new ShopSkuKey(SUPPLIER_ID_1, "гордость-и-предубеждение");
        MasterData prototype = TestDataUtils.generateMasterData(key, random);
        masterDataRepository.insert(prototype);
        prototype.setBoxCount(Integer.MAX_VALUE);
        ServiceSsku ssku = converter.toServiceSsku(prototype, (FromIrisItemWrapper) null);
        List<ErrorInfo> errors = mdmCommonSskuService.update(List.of(new CommonSsku(key).setBaseSsku(ssku))).get(key);
        Assertions.assertThat(errors.get(0).getErrorCode()).isEqualTo("mboc.error.excel-value-not-in-range");
        assertNoSskuEnqueued();
    }

    @Test
    public void whenUpdateIncompleteDataShouldRefillFromExistingGoldAndReturnNoError() {
        ShopSkuKey key = new ShopSkuKey(SUPPLIER_ID_1, "приключения-оливера-твиста");
        FromIrisItemWrapper prototype = generateIncompleteIrisItem(key,
            MasterDataSourceType.UNKNOWN_MDM_OPERATOR_SOURCE_ID, MasterDataSourceType.MDM_OPERATOR);
        referenceItemRepository.insert(generateReferenceItem(key));

        ServiceSsku ssku = converter.toServiceSsku(null, prototype);

        // Без стран не сгенерится МДшка
        SskuParamValue countries = new SskuParamValue().setShopSkuKey(key);
        countries.setXslName("manufacturerCountries");
        countries.setString("Китай");
        countries.setMdmParamId(KnownMdmParams.MANUFACTURER_COUNTRY);
        ssku.addParamValue(countries);
        List<ErrorInfo> errors = mdmCommonSskuService.update(List.of(new CommonSsku(key).setBaseSsku(ssku))).get(key);
        Assertions.assertThat(errors).isNullOrEmpty();
        assertSskuEnqueued(List.of(key));
    }

    @Test
    public void whenUpdateNonexistingSskuShouldReturnError2() {
        ShopSkuKey key = new ShopSkuKey(SUPPLIER_ID_1, "из-пушки-на-луну");
        FromIrisItemWrapper prototype = generateIrisItem(key,
            MasterDataSourceType.UNKNOWN_MDM_OPERATOR_SOURCE_ID, MasterDataSourceType.MDM_OPERATOR);
        ServiceSsku ssku = converter.toServiceSsku(null, prototype);

        // Без стран не сгенерится МДшка
        SskuParamValue countries = new SskuParamValue().setShopSkuKey(key);
        countries.setXslName("manufacturerCountries");
        countries.setString("Китай");
        countries.setMdmParamId(KnownMdmParams.MANUFACTURER_COUNTRY);
        ssku.addParamValue(countries);
        List<ErrorInfo> errors = mdmCommonSskuService.update(List.of(new CommonSsku(key).setBaseSsku(ssku))).get(key);
        Assertions.assertThat(errors.get(0).render()).isEqualToIgnoringCase("SSKU не найдена");
        assertNoSskuEnqueued();
    }

    @Test
    public void whenUpdateSskuShouldModifySilver() {
        ShopSkuKey key = new ShopSkuKey(SUPPLIER_ID_1, "оффер");
        FromIrisItemWrapper prototype = generateIrisItem(key,
            MasterDataSourceType.UNKNOWN_MDM_OPERATOR_SOURCE_ID, MasterDataSourceType.MDM_OPERATOR);
        referenceItemRepository.insert(new ReferenceItemWrapper(prototype.getItem()));
        prototype = generateIrisItem(key,
            MasterDataSourceType.UNKNOWN_MDM_OPERATOR_SOURCE_ID, MasterDataSourceType.MDM_OPERATOR);
        ServiceSsku ssku = converter.toServiceSsku(null, prototype);
        SskuParamValue countries = new SskuParamValue().setShopSkuKey(key);
        countries.setXslName("manufacturerCountries");
        countries.setString("Китай");
        countries.setMdmParamId(KnownMdmParams.MANUFACTURER_COUNTRY);
        ssku.addParamValue(countries);
        mdmCommonSskuService.update(List.of(new CommonSsku(key).setBaseSsku(ssku)));

        SilverCommonSsku silver =
            silverSskuRepository.findSsku(unknownOperatorSilverKey(key)).orElseThrow();
        Map<Long, SskuSilverParamValue> silverValues = silver.getBaseValuesByParamId();
        Map<Long, SskuParamValue> expectedValues = ssku.getValuesByParamId();
        Assertions.assertThat(silverValues).hasSameSizeAs(expectedValues);
        silverValues.forEach((paramId, silverValue) -> {
            Assertions.assertThat(silverValue.valueEquals(expectedValues.get(paramId))).isTrue();
        });
        assertSskuEnqueued(List.of(key));
    }

    @Test
    public void whenUpdateSskuShouldModifySilverAndCreateGold() {
        ShopSkuKey key = new ShopSkuKey(SUPPLIER_ID_1, "еще-оффер");
        FromIrisItemWrapper prototype = generateIrisItem(key,
            MasterDataSourceType.UNKNOWN_MDM_OPERATOR_SOURCE_ID, MasterDataSourceType.MDM_OPERATOR);
        referenceItemRepository.insert(new ReferenceItemWrapper(prototype.getItem()));
        prototype = generateIrisItem(key,
            MasterDataSourceType.UNKNOWN_MDM_OPERATOR_SOURCE_ID, MasterDataSourceType.MDM_OPERATOR);
        ServiceSsku ssku = converter.toServiceSsku(null, prototype);

        // Без стран не сгенерится МДшка
        SskuParamValue countries = new SskuParamValue().setShopSkuKey(key);
        countries.setXslName("manufacturerCountries");
        countries.setString("Китай");
        countries.setMdmParamId(KnownMdmParams.MANUFACTURER_COUNTRY);
        ssku.addParamValue(countries);
        mdmCommonSskuService.update(List.of(new CommonSsku(key).setBaseSsku(ssku)));
        assertSskuEnqueued(List.of(key));

        processSskuToRefresh();

        Assertions.assertThat(referenceItemRepository.findAll()).hasSize(1);
        ReferenceItemWrapper gold = referenceItemRepository.findById(key);
        Assertions.assertThat(gold.equalsByDataExcludingTsAndRsl(prototype, false)).isTrue();
        assertNoSskuEnqueued();
    }

    @Test
    public void whenUpdateWhiteOrUnknownShouldNotUpdate() {
        ShopSkuKey key1 = new ShopSkuKey(SUPPLIER_ID_1, "степной-волк");
        ShopSkuKey key2 = new ShopSkuKey(SUPPLIER_ID_WHITE, "оффер");
        ShopSkuKey key3 = new ShopSkuKey(SUPPLIER_ID_UNKNOWN, "еще-оффер");
        QualityDocument[] documents = {
            TestDataUtils.generateCorrectDocument(random),
            TestDataUtils.generateCorrectDocument(random),
            TestDataUtils.generateCorrectDocument(random)
        };
        MasterData prototype1 = TestDataUtils.generateMasterData(key1, random, documents);
        MasterData prototype2 = TestDataUtils.generateMasterData(key2, random, documents);
        MasterData prototype3 = TestDataUtils.generateMasterData(key3, random, documents);
        masterDataRepository.insertBatch(prototype1, prototype2, prototype3);

        prototype1.setBoxCount(100);
        prototype1.setManufacturerCountries(List.of("Чили", "Мадагаскар"));
        ServiceSsku ssku1 = converter.toServiceSsku(prototype1, (FromIrisItemWrapper) null);
        MasterData prototype2mod = new MasterData();
        prototype2mod.copyDataFieldsFrom(prototype2);
        prototype2mod.setBoxCount(100);
        prototype2mod.setManufacturerCountries(List.of("Чили", "Мадагаскар"));
        ServiceSsku ssku2 = converter.toServiceSsku(prototype2mod, (FromIrisItemWrapper) null);
        MasterData prototype3mod = new MasterData();
        prototype3mod.copyDataFieldsFrom(prototype3);
        prototype3mod.setBoxCount(100);
        prototype3mod.setManufacturerCountries(List.of("Чили", "Мадагаскар"));
        ServiceSsku ssku3 = converter.toServiceSsku(prototype3mod, (FromIrisItemWrapper) null);

        List<CommonSsku> toUpdate = new ArrayList<>();
        toUpdate.add(new CommonSsku(key1).setBaseSsku(ssku1));
        toUpdate.add(new CommonSsku(key2).setBaseSsku(ssku2));
        toUpdate.add(new CommonSsku(key3).setBaseSsku(ssku3));

        Map<ShopSkuKey, List<ErrorInfo>> errors = mdmCommonSskuService.update(toUpdate);

        Optional<SilverCommonSsku> silver1 = silverSskuRepository.findSsku(unknownOperatorSilverKey(key1));
        Assertions.assertThat(silver1)
            .flatMap(silver -> silver.getBaseValue(KnownMdmParams.MANUFACTURER_COUNTRY))
            .map(MdmParamValue::getStrings)
            .hasValueSatisfying(countries ->
                Assertions.assertThat(countries).containsExactlyInAnyOrder("Чили", "Мадагаскар"));
        Assertions.assertThat(silverSskuRepository.findSsku(unknownOperatorSilverKey(key2))).isEmpty();
        Assertions.assertThat(silverSskuRepository.findSsku(unknownOperatorSilverKey(key3))).isEmpty();
        Assertions.assertThat(errors.get(key2).get(0).getMessageTemplate())
            .isEqualTo("SSKU по белому поставщику - данные не будут сохранены");
        Assertions.assertThat(errors.get(key3).get(0).getMessageTemplate())
            .isEqualTo("SSKU по неизвестному поставщику - данные не будут сохранены");

        assertSskuEnqueued(List.of(key1));
    }

    @Test
    public void whenUpdateWithoutCountries() {
        ShopSkuKey key = new ShopSkuKey(SUPPLIER_ID_1, "фырфырфыр");
        FromIrisItemWrapper prototype = generateIncompleteIrisItem(key,
            MasterDataSourceType.UNKNOWN_MDM_OPERATOR_SOURCE_ID, MasterDataSourceType.MDM_OPERATOR);
        referenceItemRepository.insert(generateReferenceItem(key));
        ServiceSsku ssku = converter.toServiceSsku(null, prototype);
        List<ErrorInfo> errors = mdmCommonSskuService.update(List.of(new CommonSsku(key).setBaseSsku(ssku))).get(key);

        Assertions.assertThat(errors).isNullOrEmpty();
        assertSskuEnqueued(List.of(key));
    }

    @Test
    public void whenUpdateIncompleteWeightDimensionsWithSameValues() {
        ShopSkuKey key = new ShopSkuKey(SUPPLIER_ID_1, "уи уи уи");
        FromIrisItemWrapper prototype = generateIncompleteIrisItem(key,
            MasterDataSourceType.UNKNOWN_MDM_OPERATOR_SOURCE_ID, MasterDataSourceType.MDM_OPERATOR);
        referenceItemRepository.insert(new ReferenceItemWrapper(prototype.getItem()));
        ServiceSsku ssku = converter.toServiceSsku(null, prototype);

        SskuParamValue countries = new SskuParamValue().setShopSkuKey(key);
        countries.setXslName("manufacturerCountries");
        countries.setString("Китай");
        countries.setMdmParamId(KnownMdmParams.MANUFACTURER_COUNTRY);
        ssku.addParamValue(countries);

        List<ErrorInfo> errors = mdmCommonSskuService.update(List.of(new CommonSsku(key).setBaseSsku(ssku))).get(key);

        Assertions.assertThat(errors).isNullOrEmpty();
        assertSskuEnqueued(List.of(key));
    }

    @Test
    public void whenUpdateIncompleteDimensionsWithNewIncompleteDimensionsShowError() {
        ShopSkuKey key = new ShopSkuKey(SUPPLIER_ID_1, "уи уи уи");
        FromIrisItemWrapper prototype = generateIncompleteIrisItem(key,
            MasterDataSourceType.UNKNOWN_MDM_OPERATOR_SOURCE_ID, MasterDataSourceType.MDM_OPERATOR);
        referenceItemRepository.insert(generateIncompleteReferenceItem(key));
        ServiceSsku ssku = converter.toServiceSsku(null, prototype);

        SskuParamValue countries = new SskuParamValue().setShopSkuKey(key);
        countries.setXslName("manufacturerCountries");
        countries.setString("Китай");
        countries.setMdmParamId(KnownMdmParams.MANUFACTURER_COUNTRY);
        ssku.addParamValue(countries);

        List<ErrorInfo> errors = mdmCommonSskuService.update(List.of(new CommonSsku(key).setBaseSsku(ssku))).get(key);

        Assertions.assertThat(errors)
            .containsExactly(MbocErrors.get().excelIncompleteDimensions(SskuMasterDataFields.BOX_DIMENSIONS));
        assertNoSskuEnqueued();
    }

    @Test
    public void testUpdatingTraceableParam() {
        // given
        supplierRepository.update(
            supplierRepository.findById(SUPPLIER_ID_1)
                .setBusinessId(BUSINESS_ID)
                .setBusinessEnabled(true)
        );

        String shopSku = "Ameno";
        ShopSkuKey key = new ShopSkuKey(SUPPLIER_ID_1, shopSku);
        ShopSkuKey businessKey = new ShopSkuKey(BUSINESS_ID, shopSku);
        sskuExistenceRepository.markExistence(key, true);

        CommonSsku update = new CommonSsku(key)
            .addBaseValue(
                TestMdmParamUtils.createRandomMdmParamValue(random, mdmParamCache.get(KnownMdmParams.IS_TRACEABLE))
            );

        masterDataRepository.insertOrUpdate(new MasterData().setShopSkuKey(key));

        // when
        Map<ShopSkuKey, List<ErrorInfo>> errors = mdmCommonSskuService.update(List.of(update));

        // then
        Assertions.assertThat(errors).isEmpty();
        Optional<SilverCommonSsku> silver1 = silverSskuRepository.findSsku(unknownOperatorSilverKey(businessKey));
        Assertions.assertThat(silver1)
            .flatMap(silver -> silver.getBaseValue(KnownMdmParams.IS_TRACEABLE))
            .flatMap(MdmParamValue::getBool)
            .isEqualTo(update.getBaseValue(KnownMdmParams.IS_TRACEABLE).flatMap(MdmParamValue::getBool));
        assertSskuEnqueued(List.of(businessKey));
    }

    private FromIrisItemWrapper generateIrisItem(ShopSkuKey key, String sourceId, MasterDataSourceType source) {
        FromIrisItemWrapper wrapper = new FromIrisItemWrapper(key);
        var infoB = ItemWrapperHelper.createRefInfoFromDimensionsBlock(new DimensionsBlock(
            new SskuParamValue().setShopSkuKey(key)
                .setMdmParamId(KnownMdmParams.LENGTH)
                .setNumeric(BigDecimal.valueOf(1 + random.nextInt(100)))
                .setMasterDataSourceId(sourceId)
                .setMasterDataSourceType(source),
            new SskuParamValue().setShopSkuKey(key)
                .setMdmParamId(KnownMdmParams.WIDTH)
                .setNumeric(BigDecimal.valueOf(1 + random.nextInt(100)))
                .setMasterDataSourceId(sourceId)
                .setMasterDataSourceType(source),
            new SskuParamValue().setShopSkuKey(key)
                .setMdmParamId(KnownMdmParams.HEIGHT)
                .setNumeric(BigDecimal.valueOf(1 + random.nextInt(100)))
                .setMasterDataSourceId(sourceId)
                .setMasterDataSourceType(source),
            new SskuParamValue().setShopSkuKey(key)
                .setMdmParamId(KnownMdmParams.WEIGHT_GROSS)
                .setNumeric(BigDecimal.valueOf(1 + random.nextInt(100)))
                .setMasterDataSourceId(sourceId)
                .setMasterDataSourceType(source)
        ));
        var itemB = wrapper.getItem().toBuilder();
        itemB.setItemId(MdmIrisPayload.MdmIdentifier.newBuilder()
            .setSupplierId(key.getSupplierId())
            .setShopSku(key.getShopSku())
            .build()).clearInformation().addInformation(infoB);
        wrapper.setSingleInformationItem(itemB.build());
        wrapper.setProcessed(false);
        return wrapper;
    }

    private FromIrisItemWrapper generateIncompleteIrisItem(ShopSkuKey key,
                                                           String sourceId,
                                                           MasterDataSourceType source) {
        FromIrisItemWrapper wrapper = new FromIrisItemWrapper(key);
        var infoB = ItemWrapperHelper.createRefInfoFromDimensionsBlock(new DimensionsBlock(
            new SskuParamValue().setShopSkuKey(key)
                .setMdmParamId(KnownMdmParams.LENGTH)
                .setNumeric(BigDecimal.valueOf(1 + random.nextInt(100)))
                .setMasterDataSourceId(sourceId)
                .setMasterDataSourceType(source),
            null,
            new SskuParamValue().setShopSkuKey(key)
                .setMdmParamId(KnownMdmParams.HEIGHT)
                .setNumeric(BigDecimal.valueOf(1 + random.nextInt(100)))
                .setMasterDataSourceId(sourceId)
                .setMasterDataSourceType(source),
            new SskuParamValue().setShopSkuKey(key)
                .setMdmParamId(KnownMdmParams.WEIGHT_GROSS)
                .setNumeric(BigDecimal.valueOf(1 + random.nextInt(100)))
                .setMasterDataSourceId(sourceId)
                .setMasterDataSourceType(source)
        ));
        var itemB = wrapper.getItem().toBuilder();
        itemB.setItemId(MdmIrisPayload.MdmIdentifier.newBuilder()
            .setSupplierId(key.getSupplierId())
            .setShopSku(key.getShopSku())
            .build()).clearInformation().addInformation(infoB);
        wrapper.setSingleInformationItem(itemB.build());
        wrapper.setProcessed(false);
        return wrapper;
    }

    private ReferenceItemWrapper generateReferenceItem(ShopSkuKey key) {
        ReferenceItemWrapper wrapper = new ReferenceItemWrapper(key);
        var infoB = ItemWrapperHelper.createRefInfoFromDimensionsBlock(new DimensionsBlock(
            new SskuParamValue().setShopSkuKey(key)
                .setMdmParamId(KnownMdmParams.LENGTH)
                .setNumeric(BigDecimal.valueOf(1 + random.nextInt(100)))
                .setMasterDataSourceId(MasterDataSourceType.UNKNOWN_MDM_OPERATOR_SOURCE_ID)
                .setMasterDataSourceType(MasterDataSourceType.MDM_OPERATOR),
            new SskuParamValue().setShopSkuKey(key)
                .setMdmParamId(KnownMdmParams.WIDTH)
                .setNumeric(BigDecimal.valueOf(1 + random.nextInt(100)))
                .setMasterDataSourceId(MasterDataSourceType.UNKNOWN_MDM_OPERATOR_SOURCE_ID)
                .setMasterDataSourceType(MasterDataSourceType.MDM_OPERATOR),
            new SskuParamValue().setShopSkuKey(key)
                .setMdmParamId(KnownMdmParams.HEIGHT)
                .setNumeric(BigDecimal.valueOf(1 + random.nextInt(100)))
                .setMasterDataSourceId(MasterDataSourceType.UNKNOWN_MDM_OPERATOR_SOURCE_ID)
                .setMasterDataSourceType(MasterDataSourceType.MDM_OPERATOR),
            new SskuParamValue().setShopSkuKey(key)
                .setMdmParamId(KnownMdmParams.WEIGHT_GROSS)
                .setNumeric(BigDecimal.valueOf(1 + random.nextInt(100)))
                .setMasterDataSourceId(MasterDataSourceType.UNKNOWN_MDM_OPERATOR_SOURCE_ID)
                .setMasterDataSourceType(MasterDataSourceType.MDM_OPERATOR)
        ));
        var itemB = wrapper.getItem().toBuilder();
        itemB.setItemId(MdmIrisPayload.MdmIdentifier.newBuilder()
            .setSupplierId(key.getSupplierId())
            .setShopSku(key.getShopSku())
            .build()).clearInformation().addInformation(infoB);
        wrapper.setSingleInformationItem(itemB.build());
        wrapper.setProcessed(false);
        return wrapper;
    }

    private ReferenceItemWrapper generateIncompleteReferenceItem(ShopSkuKey key) {
        ReferenceItemWrapper wrapper = new ReferenceItemWrapper(key);
        var infoB = ItemWrapperHelper.createRefInfoFromDimensionsBlock(new DimensionsBlock(
            new SskuParamValue().setShopSkuKey(key)
                .setMdmParamId(KnownMdmParams.LENGTH)
                .setNumeric(BigDecimal.valueOf(1 + random.nextInt(100)))
                .setMasterDataSourceId(MasterDataSourceType.UNKNOWN_MDM_OPERATOR_SOURCE_ID)
                .setMasterDataSourceType(MasterDataSourceType.MDM_OPERATOR),
            null,
            new SskuParamValue().setShopSkuKey(key)
                .setMdmParamId(KnownMdmParams.HEIGHT)
                .setNumeric(BigDecimal.valueOf(1 + random.nextInt(100)))
                .setMasterDataSourceId(MasterDataSourceType.UNKNOWN_MDM_OPERATOR_SOURCE_ID)
                .setMasterDataSourceType(MasterDataSourceType.MDM_OPERATOR),
            new SskuParamValue().setShopSkuKey(key)
                .setMdmParamId(KnownMdmParams.WEIGHT_GROSS)
                .setNumeric(BigDecimal.valueOf(1 + random.nextInt(100)))
                .setMasterDataSourceId(MasterDataSourceType.UNKNOWN_MDM_OPERATOR_SOURCE_ID)
                .setMasterDataSourceType(MasterDataSourceType.MDM_OPERATOR)
        ));
        var itemB = wrapper.getItem().toBuilder();
        itemB.setItemId(MdmIrisPayload.MdmIdentifier.newBuilder()
            .setSupplierId(key.getSupplierId())
            .setShopSku(key.getShopSku())
            .build()).clearInformation().addInformation(infoB);
        wrapper.setSingleInformationItem(itemB.build());
        wrapper.setProcessed(false);
        return wrapper;
    }

    private void createAndSaveDbsSupplier() {
        var dbsSupplier = new MdmSupplier()
            .setId(SUPPLIER_ID_4)
            .setType(MdmSupplierType.THIRD_PARTY)
            .setBusinessEnabled(true)
            .setBusinessId(BUSINESS_ID)
            .setSalesModels(List.of(MdmSupplierSalesModel.DROPSHIP_BY_SELLER, MdmSupplierSalesModel.FULFILLMENT));
        supplierRepository.insert(dbsSupplier);
    }

    private ServiceSsku generateSskuDataWithoutVgh(ShopSkuKey key,
                                                   int categoryId,
                                                   List<MdmSupplierSalesModel> supplierSalesModels) {
        MdmSupplier supplier = supplierRepository.findById(key.getSupplierId());
        supplier.setSalesModels(supplierSalesModels);
        supplierRepository.insertOrUpdate(supplier);

        MappingCacheDao mapping =
            mappingsCacheRepository.insert(new MappingCacheDao().setShopSkuKey(key).setCategoryId(categoryId));
        MasterData masterData = TestDataUtils.generateMasterData(
            key, random, TestDataUtils.generateCorrectDocument(random));
        masterDataRepository.insert(masterData);
        // Создаем ssku без ВГХ
        ServiceSsku ssku = converter.toServiceSsku(masterData, null, mapping);

        // Без стран не сгенерится МДшка
        SskuParamValue countries = new SskuParamValue().setShopSkuKey(key);
        countries.setXslName("manufacturerCountries");
        countries.setString("Китай");
        countries.setMdmParamId(KnownMdmParams.MANUFACTURER_COUNTRY);
        ssku.addParamValue(countries);

        return ssku;
    }

    private void assertNoSskuEnqueued() {
        Assertions.assertThat(sskuToRefreshRepository.getUnprocessedBatch(Integer.MAX_VALUE)).isEmpty();
    }

    private void assertSskuEnqueued(List<ShopSkuKey> sskus) {
        List<SskuToRefreshInfo> allUnprocessed = sskuToRefreshRepository.getUnprocessedBatch(Integer.MAX_VALUE);
        Assertions.assertThat(allUnprocessed)
            .flatMap(MdmQueueInfoBase::getOnlyReasons)
            .allMatch(reason -> reason == MdmEnqueueReason.CHANGED_BY_MDM_OPERATOR);
        Assertions.assertThat(allUnprocessed)
            .map(MdmQueueInfoBase::getEntityKey)
            .containsExactlyInAnyOrderElementsOf(sskus);
    }

    private static SilverSskuKey unknownOperatorSilverKey(ShopSkuKey shopSkuKey) {
        return new SilverSskuKey(
            shopSkuKey,
            new MasterDataSource(MasterDataSourceType.MDM_OPERATOR, MasterDataSourceType.UNKNOWN_MDM_OPERATOR_SOURCE_ID)
        );
    }

    private void processSskuToRefresh() {
        sskuToRefreshRepository.processUniqueEntitiesInBatches(
            constantBatchProperties(300).deleteProcessed(true).build(),
            infos -> {
                List<ShopSkuKey> keys =
                    infos.stream().map(SskuToRefreshInfo::getEntityKey).collect(Collectors.toList());
                sskuToRefreshProcessingService.processShopSkuKeys(keys);
                return true;
            });
    }
}
