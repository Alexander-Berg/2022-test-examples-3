package ru.yandex.market.mdm.app.controller;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.benas.randombeans.api.EnhancedRandom;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.transaction.TestTransaction;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import ru.yandex.market.application.monitoring.ComplexMonitoring;
import ru.yandex.market.mbo.lightmapper.JsonMapper;
import ru.yandex.market.mbo.mdm.common.datacamp.MdmDatacampServiceMock;
import ru.yandex.market.mbo.mdm.common.infrastructure.MdmFileHistoryRepositoryMock;
import ru.yandex.market.mbo.mdm.common.infrastructure.MdmFileImportHelperService;
import ru.yandex.market.mbo.mdm.common.infrastructure.MdmS3FileServiceMock;
import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.blocks.MasterDataIntoBlocksSplitter;
import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.blocks.WeightDimensionsSilverItemSplitter;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.MdmParamOption;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.MdmParamValue;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.SskuParamValue;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.SskuSilverParamValue;
import ru.yandex.market.mbo.mdm.common.masterdata.model.queue.MdmQueueInfoBase;
import ru.yandex.market.mbo.mdm.common.masterdata.model.ssku.CommonSsku;
import ru.yandex.market.mbo.mdm.common.masterdata.model.supplier.MdmSupplier;
import ru.yandex.market.mbo.mdm.common.masterdata.model.supplier.MdmSupplierType;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.MappingsCacheRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.MdmSupplierRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.MdmUserRepositoryMock;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.ReferenceItemRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.SskuExistenceRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.param.CategoryParamValueRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.param.GoldSskuRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.param.SilverSskuRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.proto.ItemWrapperTestUtil;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.queue.MdmQueuesManager;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.queue.SskuToRefreshRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.services.SupplierConverterServiceMock;
import ru.yandex.market.mbo.mdm.common.masterdata.services.business.MasterDataBusinessMergeService;
import ru.yandex.market.mbo.mdm.common.masterdata.services.business.MdmMergeSettingsCacheImpl;
import ru.yandex.market.mbo.mdm.common.masterdata.services.business.MdmSskuGroupManager;
import ru.yandex.market.mbo.mdm.common.masterdata.services.business.MdmSupplierCachingService;
import ru.yandex.market.mbo.mdm.common.masterdata.services.editor.MdmSampleDataService;
import ru.yandex.market.mbo.mdm.common.masterdata.services.editor.MdmSampleDataServiceImpl;
import ru.yandex.market.mbo.mdm.common.masterdata.services.iris.BeruIdMock;
import ru.yandex.market.mbo.mdm.common.masterdata.services.iris.RslGoldenItemService;
import ru.yandex.market.mbo.mdm.common.masterdata.services.iris.SurplusAndCisGoldenItemServiceMock;
import ru.yandex.market.mbo.mdm.common.masterdata.services.iris.WeightDimensionForceInheritanceService;
import ru.yandex.market.mbo.mdm.common.masterdata.services.iris.WeightDimensionsForceInheritancePostProcessor;
import ru.yandex.market.mbo.mdm.common.masterdata.services.iris.WeightDimensionsGoldenItemService;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownMdmParams;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.MasterDataGoldenItemService;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.MdmParamProvider;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.ServiceSskuConverter;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.ShopSkuKeyExcelService;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.SskuMdmParamExcelExportService;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.SskuMdmParamExcelImportService;
import ru.yandex.market.mbo.mdm.common.masterdata.services.ssku.MdmCommonSskuService;
import ru.yandex.market.mbo.mdm.common.masterdata.services.ssku.MdmCommonSskuServiceImpl;
import ru.yandex.market.mbo.mdm.common.masterdata.services.ssku.MultivalueBusinessHelper;
import ru.yandex.market.mbo.mdm.common.masterdata.services.ssku.SskuGoldenMasterDataCalculationHelper;
import ru.yandex.market.mbo.mdm.common.masterdata.services.ssku.SskuGoldenReferenceItemCalculationHelper;
import ru.yandex.market.mbo.mdm.common.masterdata.services.ssku.TraceableSskuGoldenItemService;
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
import ru.yandex.market.mbo.mdm.common.masterdata.validator.CachedItemBlockValidationContextProviderImpl;
import ru.yandex.market.mbo.mdm.common.masterdata.validator.NoOpWeightDimensionBlockValidationServiceImpl;
import ru.yandex.market.mbo.mdm.common.masterdata.validator.WeightDimensionBlockValidationService;
import ru.yandex.market.mbo.mdm.common.masterdata.validator.WeightDimensionsValidator;
import ru.yandex.market.mbo.mdm.common.masterdata.validator.block.MasterDataBlocksValidationService;
import ru.yandex.market.mbo.mdm.common.service.FeatureSwitchingAssistant;
import ru.yandex.market.mbo.mdm.common.service.queue.ProcessSskuToRefreshQueueService;
import ru.yandex.market.mbo.mdm.common.util.SskuGoldenParamUtil;
import ru.yandex.market.mbo.mdm.common.utils.MdmDbWithCleaningTestClass;
import ru.yandex.market.mbo.storage.StorageKeyValueService;
import ru.yandex.market.mboc.common.infrastructure.sql.TransactionHelper;
import ru.yandex.market.mboc.common.masterdata.TestDataUtils;
import ru.yandex.market.mboc.common.masterdata.model.MasterData;
import ru.yandex.market.mboc.common.masterdata.model.TimeInUnits;
import ru.yandex.market.mboc.common.masterdata.parsing.MasterDataValidator;
import ru.yandex.market.mboc.common.masterdata.repository.MasterDataRepository;
import ru.yandex.market.mboc.common.masterdata.repository.document.QualityDocumentRepository;
import ru.yandex.market.mboc.common.masterdata.services.SskuMasterDataStorageService;
import ru.yandex.market.mboc.common.masterdata.services.cutoff.OfferCutoffService;
import ru.yandex.market.mboc.common.offers.model.ShopSkuKey;
import ru.yandex.market.mboc.common.offers.repository.MboMappingsServiceMock;
import ru.yandex.market.mboc.common.utils.MdmProperties;

/**
 * @author albina-gima
 */
@SuppressWarnings("checkstyle:magicnumber")
public class MdmSskuUiControllerWithShelfLifeLifeTimeAndGuaranteePeriodTest extends MdmDbWithCleaningTestClass {
    private static final long SEED = 20200607L;
    private static final int BATCH_SIZE = 50;
    private static final List<Long> LIFE_TIME_PERIOD_PARAMS = List.of(
        KnownMdmParams.SHELF_LIFE, KnownMdmParams.SHELF_LIFE_UNIT, KnownMdmParams.SHELF_LIFE_COMMENT,
        KnownMdmParams.LIFE_TIME, KnownMdmParams.LIFE_TIME_UNIT, KnownMdmParams.LIFE_TIME_COMMENT,
        KnownMdmParams.GUARANTEE_PERIOD, KnownMdmParams.GUARANTEE_PERIOD_UNIT, KnownMdmParams.GUARANTEE_PERIOD_UNIT);

    private static final ShopSkuKey BUSINESS_KEY = new ShopSkuKey(100, "iron");
    private static final ShopSkuKey SUPPLIER_KEY1 = new ShopSkuKey(101, "iron");
    private static final ShopSkuKey SUPPLIER_KEY2 = new ShopSkuKey(102, "iron");

    @Autowired
    private MasterDataRepository masterDataRepository;
    @Autowired
    private ReferenceItemRepository referenceItemRepository;
    @Autowired
    private MdmParamProvider mdmParamProvider;
    @Autowired
    private MappingsCacheRepository mappingsCacheRepository;
    @Autowired
    private SskuMdmParamExcelExportService sskuMdmParamExcelExportService;
    @Autowired
    private SskuMdmParamExcelImportService sskuMdmParamExcelImportService;
    @Autowired
    private ShopSkuKeyExcelService shopSkuKeyExcelService;
    @Autowired
    private TransactionHelper transactionHelper;
    @Autowired
    private ServiceSskuConverter serviceSskuConverter;
    @Autowired
    private QualityDocumentRepository qualityDocumentRepository;
    @Autowired
    private MdmQueuesManager mdmQueuesManager;
    @Autowired
    private StorageKeyValueService keyValueService;
    @Autowired
    private BeruIdMock beruId;
    @Autowired
    private MdmSupplierRepository mdmSupplierRepository;
    @Autowired
    private MdmSskuGroupManager mdmSskuGroupManager;
    @Autowired
    private MasterDataBusinessMergeService masterDataBusinessMergeService;
    @Autowired
    private MultivalueBusinessHelper multivalueBusinessHelper;
    @Autowired
    private MdmSupplierCachingService mdmSupplierCachingService;
    @Autowired
    private SilverSskuRepository silverSskuRepository;
    @Autowired
    private GoldSskuRepository goldSskuRepository;
    @Autowired
    private MasterDataValidator masterDataValidator;
    @Autowired
    private MdmMergeSettingsCacheImpl mdmMergeSettingsCache;
    @Autowired
    private SskuExistenceRepository sskuExistenceRepository;
    @Autowired
    private SskuToRefreshRepository sskuToRefreshRepository;
    @Autowired
    private SskuGoldenParamUtil sskuGoldenParamUtil;
    @Autowired
    private RslGoldenItemService rslGoldenItemService;
    @Autowired
    private FeatureSwitchingAssistant featureSwitchingAssistant;
    @Autowired
    private SskuProcessingDataProvider sskuProcessingDataProvider;
    @Autowired
    private VerdictCalculationHelper verdictCalculationHelper;
    @Autowired
    private MasterDataVersionMapService masterDataVersionMapService;
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
    private TraceableSskuGoldenItemService traceableSskuGoldenItemService;
    @Autowired
    private WeightDimensionsValidator weightDimensionsValidator;

    private SskuMasterDataStorageService sskuMasterDataStorageService;
    private MdmCommonSskuService mdmCommonSskuService;
    private MockMvc mockMvc;
    private MdmSskuUiController mdmSskuUiController;
    private MdmSampleDataService mdmSampleDataService;
    private MboMappingsServiceMock mboMappingsService;
    private EnhancedRandom random;
    private MasterDataValidationService masterDataValidationService;
    private ProcessSskuToRefreshQueueService processSskuToRefreshQueueService;

    @Before
    public void setUp() {
        random = TestDataUtils.defaultRandom(SEED);

        keyValueService.putValue(MdmProperties.ENABLE_BUSINESS_STATE_UPDATE_IN_MDM_SUPPLIER_CACHE, true);
        keyValueService.putValue(MdmProperties.BUSINESS_MERGE_ENABLED_KEY, true);

        sskuMasterDataStorageService = new SskuMasterDataStorageService(
            masterDataRepository, qualityDocumentRepository, transactionHelper, new SupplierConverterServiceMock(),
            new ComplexMonitoring());

        mboMappingsService = new MboMappingsServiceMock();
        masterDataValidationService = new MasterDataValidationService(masterDataValidator);
        mdmCommonSskuService = new MdmCommonSskuServiceImpl(
            sskuMasterDataStorageService,
            referenceItemRepository,
            serviceSskuConverter,
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
            mdmSupplierRepository,
            weightDimensionBlockValidationService,
            masterDataBlocksValidationService,
            masterDataIntoBlocksSplitter,
            keyValueService,
            new MdmUserRepositoryMock()
        );

        mdmSampleDataService = new MdmSampleDataServiceImpl(keyValueService, mappingsCacheRepository);

        MdmFileImportHelperService fileImportHelperService = new MdmFileImportHelperService(
            new MdmS3FileServiceMock(), new MdmFileHistoryRepositoryMock());
        mdmSskuUiController = new MdmSskuUiController(
            new ObjectMapper(),
            mdmParamProvider,
            mdmCommonSskuService,
            sskuMdmParamExcelExportService,
            sskuMdmParamExcelImportService,
            shopSkuKeyExcelService,
            fileImportHelperService,
            mdmSampleDataService,
            beruId,
            new MdmDatacampServiceMock()
        );
        mockMvc = MockMvcBuilders.standaloneSetup(mdmSskuUiController).build();

        SskuGoldenReferenceItemCalculationHelper sskuGoldenReferenceItemCalculationHelper =
            new SskuGoldenReferenceItemCalculationHelper(
                new WeightDimensionsGoldenItemService(
                    new WeightDimensionsSilverItemSplitter(new SupplierConverterServiceMock()),
                    new NoOpWeightDimensionBlockValidationServiceImpl(),
                    featureSwitchingAssistant),
                new WeightDimensionForceInheritanceService(
                    featureSwitchingAssistant,
                    new WeightDimensionsForceInheritancePostProcessor(),
                    new NoOpWeightDimensionBlockValidationServiceImpl()
                ),
                new SurplusAndCisGoldenItemServiceMock(),
                keyValueService,
                serviceSskuConverter,
                masterDataBusinessMergeService,
                Mockito.mock(OfferCutoffService.class),
                sskuGoldenParamUtil,
                new CachedItemBlockValidationContextProviderImpl(keyValueService),
                weightDimensionsValidator,
                rslGoldenItemService
            );
        SskuGoldenMasterDataCalculationHelper sskuGoldenMasterDataCalculationHelper =
            new SskuGoldenMasterDataCalculationHelper(
                serviceSskuConverter,
                masterDataValidationService,
                masterDataBusinessMergeService,
                keyValueService,
                masterDataGoldenItemService,
                sskuGoldenParamUtil,
                traceableSskuGoldenItemService,
                multivalueBusinessHelper
            );

        SskuToRefreshProcessingService sskuToRefreshProcessingService = new SskuToRefreshProcessingServiceImpl(
            sskuProcessingDataProvider,
            sskuVerdictProcessor(),
            sskuProcessingPostProcessor(sskuGoldenReferenceItemCalculationHelper),
            sskuProcessingContextProvider(),
            sskuProcessingPipeProcessor(),
            sskuProcessingPreProcessor(),
            sskuCalculatingProcessor(sskuGoldenReferenceItemCalculationHelper, sskuGoldenMasterDataCalculationHelper));


        processSskuToRefreshQueueService = new ProcessSskuToRefreshQueueService(
            sskuToRefreshRepository,
            keyValueService,
            mdmSskuGroupManager,
            sskuToRefreshProcessingService,
            transactionTemplate
        );

        loadMasterDataToRepos();

        addMapping(SUPPLIER_KEY1);
        addMapping(SUPPLIER_KEY2);

        sskuExistenceRepository.markExistence(List.of(
            SUPPLIER_KEY1,
            SUPPLIER_KEY2
        ), true);
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
        return new SskuProcessingContextProviderImpl(keyValueService);
    }

    private SskuProcessingPipeProcessor sskuProcessingPipeProcessor() {
        return new SskuProcessingPipeProcessorImpl(mdmQueuesManager, serviceSskuConverter);
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
            serviceSskuConverter,
            masterDataBusinessMergeService,
            masterDataVersionMapService,
            verdictCalculationHelper);
    }

    @Test
    public void whenUpdateSskuByServiceSupplierIdShouldCorrectlyRecalcSskuGoldWithLifeTimePeriodPVs() throws Exception {
        // 1. Отправляем запрос на обновление всех сроков (годности, службы, гарантии) из UI MDM
        int shelfLife = 12;
        TimeInUnits.TimeUnit shelfLifeUnitOptionId = TimeInUnits.TimeUnit.MONTH;
        String shelfLifeComment = "Хранить бережно, сдувая пылинки";

        String lifeTime = "1";
        TimeInUnits.TimeUnit lifeTimeUnitOptionId = TimeInUnits.TimeUnit.YEAR;
        String lifeTimeComment = "Довольно короткий срок службы";

        String guaranteePeriod = "365";
        TimeInUnits.TimeUnit guaranteePeriodUnitOptionId = TimeInUnits.TimeUnit.DAY;
        String guaranteePeriodComment = "И гарантия так себе";

        CommonSsku serviceSsku = getCommonSskuWithLifeTimePeriodParameters(SUPPLIER_KEY1,
            shelfLife, shelfLifeUnitOptionId, shelfLifeComment,
            lifeTime, lifeTimeUnitOptionId, lifeTimeComment,
            guaranteePeriod, guaranteePeriodUnitOptionId, guaranteePeriodComment
        );
        String jsonSsku = JsonMapper.DEFAULT_OBJECT_MAPPER.writeValueAsString(serviceSsku);
        MockHttpServletRequestBuilder updateRequest = MockMvcRequestBuilders.post("/mdm-api/ui/ssku/update")
            .contentType(MediaType.APPLICATION_JSON_UTF8)
            .content(jsonSsku);

        mockMvc.perform(updateRequest)
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andReturn();

        // 2. Проверяем, что данные по всем срокам сохранились в серебро по бизнес-ключу с нужным источником
        List<SskuSilverParamValue> silver = silverSskuRepository.findAll();
        Set<ShopSkuKey> skusForLifeTimePeriodPVs = silver.stream()
            .filter(o -> KnownMdmParams.TIME_LIFE_PERIOD_PARAM_IDS.contains(o.getMdmParamId()))
            .map(SskuParamValue::getShopSkuKey)
            .collect(Collectors.toSet());
        Map<Long, List<SskuSilverParamValue>> silverByMdmParamId = silver.stream()
            .collect(Collectors.groupingBy(MdmParamValue::getMdmParamId));

        Assertions.assertThat(skusForLifeTimePeriodPVs.size()).isEqualTo(1);
        Assertions.assertThat(skusForLifeTimePeriodPVs.iterator().next()).isEqualTo(BUSINESS_KEY);

        Assertions.assertThat(silverByMdmParamId.keySet()).containsAll(LIFE_TIME_PERIOD_PARAMS);
        checkNumericValueFromMdmUi(silverByMdmParamId.get(KnownMdmParams.SHELF_LIFE).get(0), shelfLife);
        checkOptionValueFromMdmUi(silverByMdmParamId.get(KnownMdmParams.SHELF_LIFE_UNIT).get(0), shelfLifeUnitOptionId);
        checkStringValueFromMdmUi(silverByMdmParamId.get(KnownMdmParams.SHELF_LIFE_COMMENT).get(0), shelfLifeComment);

        checkStringValueFromMdmUi(silverByMdmParamId.get(KnownMdmParams.LIFE_TIME).get(0), lifeTime);
        checkOptionValueFromMdmUi(silverByMdmParamId.get(KnownMdmParams.LIFE_TIME_UNIT).get(0), lifeTimeUnitOptionId);
        checkStringValueFromMdmUi(silverByMdmParamId.get(KnownMdmParams.LIFE_TIME_COMMENT).get(0), lifeTimeComment);

        checkStringValueFromMdmUi(silverByMdmParamId.get(KnownMdmParams.GUARANTEE_PERIOD).get(0), guaranteePeriod);
        checkOptionValueFromMdmUi(silverByMdmParamId.get(KnownMdmParams.GUARANTEE_PERIOD_UNIT).get(0),
            guaranteePeriodUnitOptionId);
        checkStringValueFromMdmUi(silverByMdmParamId.get(KnownMdmParams.GUARANTEE_PERIOD_COMMENT).get(0),
            guaranteePeriodComment);

        // 3. Проверяем, что бизнес-группа добавилась на пересчет золота
        Set<ShopSkuKey> enqueuedKeys = sskuToRefreshRepository.getUnprocessedBatch(BATCH_SIZE).stream()
            .map(MdmQueueInfoBase::getEntityKey)
            .collect(Collectors.toSet());
        Assertions.assertThat(enqueuedKeys).containsAll(List.of(BUSINESS_KEY));

        // 4. Проверяем, что золото по срокам пересчиталось корректно
        processSskuToRefreshQueueAfterCommit();

        checkMasterDataForLifeTimePeriodParamValues(
            List.of(SUPPLIER_KEY1, SUPPLIER_KEY2),
            shelfLife, shelfLifeUnitOptionId, shelfLifeComment,
            lifeTime, lifeTimeUnitOptionId, lifeTimeComment,
            guaranteePeriod, guaranteePeriodUnitOptionId, guaranteePeriodComment
        );
    }

    private List<MdmSupplier> getSuppliers() {
        MdmSupplier businessMan = supplier(BUSINESS_KEY.getSupplierId(), MdmSupplierType.BUSINESS,
            "Business Man", null, false);

        MdmSupplier firstSupplierMan = supplier(SUPPLIER_KEY1.getSupplierId(), MdmSupplierType.FIRST_PARTY,
            "First supplier Man", BUSINESS_KEY.getSupplierId(), true);

        MdmSupplier secondSupplierMan = supplier(SUPPLIER_KEY2.getSupplierId(), MdmSupplierType.THIRD_PARTY,
            "Second supplier Man", BUSINESS_KEY.getSupplierId(), true);

        return List.of(businessMan, firstSupplierMan, secondSupplierMan);
    }

    private static MdmSupplier supplier(Integer id, MdmSupplierType supplierType, String name,
                                        Integer businessId, boolean businessEnabled) {
        MdmSupplier supplier = new MdmSupplier();
        supplier.setId(id);
        supplier.setType(supplierType);
        supplier.setName(name);
        supplier.setBusinessEnabled(businessEnabled);

        if (businessId != null) {
            supplier.setBusinessId(businessId);
        }

        return supplier;
    }

    private void loadMasterDataToRepos() {
        MasterData masterData1 = TestDataUtils.generateMasterData(SUPPLIER_KEY1, random);
        MasterData masterData2 = TestDataUtils.generateMasterData(SUPPLIER_KEY2, random);

        masterData1.setManufacturerCountries(List.of("Китай"));
        masterData1.setItemShippingUnit(
            ItemWrapperTestUtil.generateShippingUnit(5.0, 5.0, 5.0, 1.0, null, null).build());

        masterDataRepository.insertBatch(masterData1, masterData2);

        mdmSupplierRepository.insertBatch(getSuppliers());
        mdmSupplierCachingService.refresh();
    }

    @SuppressWarnings("checkstyle:ParameterNumber")
    private CommonSsku getCommonSskuWithLifeTimePeriodParameters(
        ShopSkuKey shopSkuKey,
        int shelfLife, TimeInUnits.TimeUnit shelfLifeUnitOptionId, String shelfLifeComment,
        String lifeTime, TimeInUnits.TimeUnit lifeTimeUnitOptionId, String lifeTimeComment,
        String guaranteePeriod, TimeInUnits.TimeUnit guaranteePeriodUnitOptionId, String guaranteePeriodComment) {

        CommonSsku commonSsku = new CommonSsku(shopSkuKey);
        List<MdmParamValue> baseValues = new ArrayList<>();
        commonSsku.setBaseValues(baseValues);
        var shelfLifePV = getNumericMdmParamValue(KnownMdmParams.SHELF_LIFE, shelfLife);
        var shelfLifeUnitPV = getOptionMdmParamValue(KnownMdmParams.SHELF_LIFE_UNIT, shelfLifeUnitOptionId);
        var shelfLifeCommentPV = getStringMdmParamValue(KnownMdmParams.SHELF_LIFE_COMMENT, shelfLifeComment);

        var lifeTimePV = getStringMdmParamValue(KnownMdmParams.LIFE_TIME, lifeTime);
        var lifeTimeUnitPV = getOptionMdmParamValue(KnownMdmParams.LIFE_TIME_UNIT, lifeTimeUnitOptionId);
        var lifeTimeCommentPV = getStringMdmParamValue(KnownMdmParams.LIFE_TIME_COMMENT, lifeTimeComment);

        var guaranteePeriodPV = getStringMdmParamValue(KnownMdmParams.GUARANTEE_PERIOD, guaranteePeriod);
        var guaranteePeriodUnitPV = getOptionMdmParamValue(KnownMdmParams.GUARANTEE_PERIOD_UNIT,
            guaranteePeriodUnitOptionId);
        var guaranteePeriodCommentPV = getStringMdmParamValue(KnownMdmParams.GUARANTEE_PERIOD_COMMENT,
            guaranteePeriodComment);

        baseValues.addAll(List.of(
            shelfLifePV, shelfLifeUnitPV, shelfLifeCommentPV,
            lifeTimePV, lifeTimeUnitPV, lifeTimeCommentPV,
            guaranteePeriodPV, guaranteePeriodUnitPV, guaranteePeriodCommentPV)
        );
        commonSsku.setBaseValues(baseValues);
        return commonSsku;
    }

    private MdmParamValue getStringMdmParamValue(long mdmParamId, String value) {
        return new MdmParamValue()
            .setString(value)
            .setMdmParamId(mdmParamId)
            .setUpdatedTs(Instant.now());
    }

    private MdmParamValue getNumericMdmParamValue(long mdmParamId, double value) {
        return new MdmParamValue()
            .setNumeric(BigDecimal.valueOf(value))
            .setMdmParamId(mdmParamId)
            .setUpdatedTs(Instant.now());
    }

    private MdmParamValue getOptionMdmParamValue(long mdmParamId, TimeInUnits.TimeUnit mdmParamOptionId) {
        return new MdmParamValue()
            .setOption(new MdmParamOption(KnownMdmParams.TIME_UNITS_OPTIONS.inverse().get(mdmParamOptionId)))
            .setMdmParamId(mdmParamId)
            .setUpdatedTs(Instant.now());
    }

    private void addMapping(ShopSkuKey key) {
        mboMappingsService.addMapping(TestDataUtils
            .generateCorrectApprovedMappingInfoBuilder(random)
            .setSupplierId(key.getSupplierId())
            .setShopSku(key.getShopSku())
            .build());
    }

    private void checkStringValueFromMdmUi(SskuSilverParamValue silverPV, String expected) {
        Assertions.assertThat(silverPV.getString().orElseThrow()).isEqualTo(expected);
    }

    private void checkOptionValueFromMdmUi(SskuSilverParamValue silverPV, TimeInUnits.TimeUnit mdmParamOptionId) {
        Assertions.assertThat(silverPV.getOption().orElseThrow()).isEqualTo(
            new MdmParamOption(KnownMdmParams.TIME_UNITS_OPTIONS.inverse().get(mdmParamOptionId)));
    }

    private void checkNumericValueFromMdmUi(SskuSilverParamValue silverPV, int expected) {
        Assertions.assertThat(silverPV.getNumeric().orElseThrow()).isEqualByComparingTo(BigDecimal.valueOf(expected));
        Assertions.assertThat(silverPV.getSskuSilverTransport()).isEqualTo(
            SskuSilverParamValue.SskuSilverTransportType.MDM_UI);
    }

    @SuppressWarnings("checkstyle:ParameterNumber")
    private void checkMasterDataForLifeTimePeriodParamValues(
        List<ShopSkuKey> keys,
        int shelfLife, TimeInUnits.TimeUnit shelfLifeUnitOptionId, String shelfLifeComment,
        String lifeTime, TimeInUnits.TimeUnit lifeTimeUnitOptionId, String lifeTimeComment,
        String guaranteePeriod, TimeInUnits.TimeUnit guaranteePeriodUnitOptionId, String guaranteePeriodComment) {

        for (ShopSkuKey key : keys) {
            MasterData resultMasterData = masterDataRepository.findById(key);
            Assertions.assertThat(resultMasterData.getShelfLife().getTime()).isEqualTo(shelfLife);
            Assertions.assertThat(resultMasterData.getShelfLife().getUnit()).isEqualTo(shelfLifeUnitOptionId);
            Assertions.assertThat(resultMasterData.getShelfLifeComment()).isEqualTo(shelfLifeComment);

            Assertions.assertThat(resultMasterData.getLifeTime().getTime()).isEqualTo(Integer.parseInt(lifeTime));
            Assertions.assertThat(resultMasterData.getLifeTime().getUnit()).isEqualTo(lifeTimeUnitOptionId);
            Assertions.assertThat(resultMasterData.getLifeTimeComment()).isEqualTo(lifeTimeComment);

            Assertions.assertThat(resultMasterData.getGuaranteePeriod().getTime()).isEqualTo(
                Integer.parseInt(guaranteePeriod));
            Assertions.assertThat(resultMasterData.getGuaranteePeriod().getUnit())
                .isEqualTo(guaranteePeriodUnitOptionId);
            Assertions.assertThat(resultMasterData.getGuaranteePeriodComment()).isEqualTo(guaranteePeriodComment);
        }
    }

    private void processSskuToRefreshQueueAfterCommit() {
        TestTransaction.flagForCommit();
        TestTransaction.end();
        TestTransaction.start();
        processSskuToRefreshQueueService.processQueueItems();
    }
}
