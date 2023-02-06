package ru.yandex.market.mdm.app.controller;

import java.math.BigDecimal;
import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.benas.randombeans.api.EnhancedRandom;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.transaction.TestTransaction;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import ru.yandex.market.application.monitoring.ComplexMonitoring;
import ru.yandex.market.mbo.excel.ExcelFile;
import ru.yandex.market.mbo.excel.ExcelFileConverter;
import ru.yandex.market.mbo.lightmapper.JsonMapper;
import ru.yandex.market.mbo.mdm.common.datacamp.MdmDatacampServiceMock;
import ru.yandex.market.mbo.mdm.common.infrastructure.MdmFileHistoryRepositoryMock;
import ru.yandex.market.mbo.mdm.common.infrastructure.MdmFileImportHelperService;
import ru.yandex.market.mbo.mdm.common.infrastructure.MdmS3FileServiceMock;
import ru.yandex.market.mbo.mdm.common.masterdata.MdmUser;
import ru.yandex.market.mbo.mdm.common.masterdata.model.MappingCacheDao;
import ru.yandex.market.mbo.mdm.common.masterdata.model.MasterDataSource;
import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.MasterDataSourceType;
import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.blocks.MasterDataIntoBlocksSplitter;
import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.blocks.WeightDimensionsSilverItemSplitter;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.MdmParamIoType;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.MdmParamValue;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.SskuGoldenParamValue;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.SskuParamValue;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.SskuSilverParamValue;
import ru.yandex.market.mbo.mdm.common.masterdata.model.ssku.CommonSsku;
import ru.yandex.market.mbo.mdm.common.masterdata.model.ssku.SilverCommonSsku;
import ru.yandex.market.mbo.mdm.common.masterdata.model.ssku.SilverSskuKey;
import ru.yandex.market.mbo.mdm.common.masterdata.model.supplier.MdmSupplier;
import ru.yandex.market.mbo.mdm.common.masterdata.model.supplier.MdmSupplierType;
import ru.yandex.market.mbo.mdm.common.masterdata.model.verdict.SingleVerdictResult;
import ru.yandex.market.mbo.mdm.common.masterdata.model.verdict.SskuVerdictResult;
import ru.yandex.market.mbo.mdm.common.masterdata.model.verdict.VerdictFeature;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.MappingsCacheRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.MdmSupplierRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.MdmUserRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.MdmUserRepositoryMock;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.ReferenceItemRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.SskuExistenceRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.param.CategoryParamValueRepositoryMock;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.param.GoldSskuRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.param.SilverSskuRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.proto.ItemWrapperTestUtil;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.queue.MdmQueuesManager;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.queue.SskuToRefreshRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.verdict.SskuGoldenVerdictRepository;
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
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.MdmParamExcelAttributes;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.MdmParamProvider;
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
import ru.yandex.market.mbo.mdm.common.masterdata.services.verdict.VerdictGeneratorHelper;
import ru.yandex.market.mbo.mdm.common.masterdata.validator.CachedItemBlockValidationContextProvider;
import ru.yandex.market.mbo.mdm.common.masterdata.validator.CachedItemBlockValidationContextProviderImpl;
import ru.yandex.market.mbo.mdm.common.masterdata.validator.WeightDimensionBlockValidationService;
import ru.yandex.market.mbo.mdm.common.masterdata.validator.WeightDimensionBlockValidationServiceImpl;
import ru.yandex.market.mbo.mdm.common.masterdata.validator.WeightDimensionsValidator;
import ru.yandex.market.mbo.mdm.common.masterdata.validator.block.MasterDataBlocksValidationService;
import ru.yandex.market.mbo.mdm.common.service.AllOkMasterDataValidator;
import ru.yandex.market.mbo.mdm.common.service.FeatureSwitchingAssistant;
import ru.yandex.market.mbo.mdm.common.service.queue.ProcessSskuToRefreshQueueService;
import ru.yandex.market.mbo.mdm.common.util.SskuGoldenParamUtil;
import ru.yandex.market.mbo.mdm.common.utils.MdmDbWithCleaningTestClass;
import ru.yandex.market.mbo.storage.StorageKeyValueService;
import ru.yandex.market.mboc.common.infrastructure.sql.TransactionHelper;
import ru.yandex.market.mboc.common.masterdata.TestDataUtils;
import ru.yandex.market.mboc.common.masterdata.model.MasterData;
import ru.yandex.market.mboc.common.masterdata.repository.MasterDataRepository;
import ru.yandex.market.mboc.common.masterdata.repository.document.QualityDocumentRepository;
import ru.yandex.market.mboc.common.masterdata.services.SskuMasterDataStorageService;
import ru.yandex.market.mboc.common.masterdata.services.cutoff.OfferCutoffService;
import ru.yandex.market.mboc.common.offers.model.ShopSkuKey;
import ru.yandex.market.mboc.common.offers.repository.MboMappingsServiceMock;
import ru.yandex.market.mboc.common.utils.MdmProperties;
import ru.yandex.market.mboc.common.utils.SecurityUtil;
import ru.yandex.market.mboc.common.web.DataPage;
import ru.yandex.misc.io.http.UrlUtils;

import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;

/**
 * @author amaslak
 */
@SuppressWarnings("checkstyle:magicnumber")
public class MdmSskuUiControllerTest extends MdmDbWithCleaningTestClass {
    private static final long SEED = 20200607L;

    private static final ShopSkuKey KEY_3P = new ShopSkuKey(42, "3p-offer/желтый");
    private static final ShopSkuKey BUSINESS_KEY = new ShopSkuKey(100, "iron");
    private static final ShopSkuKey SUPPLIER_KEY1 = new ShopSkuKey(101, "iron");
    private static final ShopSkuKey SUPPLIER_KEY2 = new ShopSkuKey(102, "iron");
    private static final ShopSkuKey DISABLED_SUPPLIER_KEY3 = new ShopSkuKey(103, "iron");
    private static final ShopSkuKey ORPHAN_KEY = new ShopSkuKey(104, "supplier");
    private static final ShopSkuKey ORPHAN_BUSINESS_KEY = new ShopSkuKey(105, "business2!");
    private static final ShopSkuKey BUSINESS_WITH_DISABLED_SUPPLIERS_KEY = new ShopSkuKey(106, "business3!");
    private static final ShopSkuKey DISABLED_SUPPLIER_KEY4 = new ShopSkuKey(107, "table");
    private static final ShopSkuKey DISABLED_SUPPLIER_KEY5 = new ShopSkuKey(108, "table");
    private static final ShopSkuKey WHITE_SUPPLIER_KEY = new ShopSkuKey(109, "iron");
    private static final ShopSkuKey WHITE_ORPHAN_SUPPLIER_KEY = new ShopSkuKey(110, "supplier");
    private static final ShopSkuKey UNKNOWN_SUPPLIER_KEY = new ShopSkuKey(111, "supplier");

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
    private QualityDocumentRepository qualityDocumentRepository;
    @Autowired
    private MdmQueuesManager mdmQueuesManager;
    @Autowired
    private StorageKeyValueService keyValueService;
    @Autowired
    private BeruIdMock beruId;
    @Autowired
    private SskuGoldenVerdictRepository sskuGoldenVerdictRepository;
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
    private MasterDataVersionMapService masterDataVersionMapService;
    @Autowired
    private VerdictCalculationHelper verdictCalculationHelper;
    @Autowired
    private SskuGoldenParamUtil sskuGoldenParamUtil;
    @Autowired
    private SskuProcessingDataProvider sskuProcessingDataProvider;
    @Autowired
    private FeatureSwitchingAssistant featureSwitchingAssistant;
    @Autowired
    private SskuToRefreshRepository sskuToRefreshRepository;
    @Autowired
    private MdmMergeSettingsCacheImpl mdmMergeSettingsCache;
    @Autowired
    private RslGoldenItemService rslGoldenItemService;
    @Autowired
    private SskuExistenceRepository sskuExistenceRepository;
    @Autowired
    private MasterDataBlocksValidationService masterDataBlocksValidationService;
    @Autowired
    private WeightDimensionBlockValidationService weightDimensionBlockValidationService;
    @Autowired
    private MasterDataIntoBlocksSplitter masterDataIntoBlocksSplitter;
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
    private MboMappingsServiceMock mboMappingsService;
    private EnhancedRandom random;
    private ProcessSskuToRefreshQueueService sskuToRefreshQueueService;
    private MdmUserRepository mdmUserRepository;

    @Before
    public void setUp() {
        random = TestDataUtils.defaultRandom(SEED);

        sskuMasterDataStorageService = new SskuMasterDataStorageService(
            masterDataRepository, qualityDocumentRepository, transactionHelper, new SupplierConverterServiceMock(),
            new ComplexMonitoring());

        mboMappingsService = new MboMappingsServiceMock();

        mdmUserRepository = new MdmUserRepositoryMock();

        mdmCommonSskuService = new MdmCommonSskuServiceImpl(
            sskuMasterDataStorageService,
            referenceItemRepository,
            serviceSskuConverter,
            mdmQueuesManager,
            transactionHelper,
            new CategoryParamValueRepositoryMock(),
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
            mdmUserRepository
        );

        MdmSampleDataService mdmSampleDataService =
            new MdmSampleDataServiceImpl(keyValueService, mappingsCacheRepository);

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

        loadDataToRepos();

        addMapping(KEY_3P);
        addMapping(SUPPLIER_KEY1);
        addMapping(SUPPLIER_KEY2);
        addMapping(DISABLED_SUPPLIER_KEY3);
        addMapping(ORPHAN_KEY);
        addMapping(DISABLED_SUPPLIER_KEY4);
        addMapping(DISABLED_SUPPLIER_KEY5);

        sskuExistenceRepository.markExistence(List.of(
            SUPPLIER_KEY1,
            SUPPLIER_KEY2,
            DISABLED_SUPPLIER_KEY3,
            ORPHAN_KEY,
            DISABLED_SUPPLIER_KEY4,
            DISABLED_SUPPLIER_KEY5
        ), true);
        initGoldComputer();

        keyValueService.putValue(MdmProperties.ENABLE_BUSINESS_STATE_UPDATE_IN_MDM_SUPPLIER_CACHE, true);
        keyValueService.putValue(MdmProperties.BUSINESS_MERGE_ENABLED_KEY, true);
        keyValueService.putValue(MdmProperties.UI_SAVE_AS_ADMIN_ENABLED, true);
        keyValueService.invalidateCache();
    }

    @Test
    public void whenGetSskuShouldReturnOk() throws Exception {
        MockHttpServletRequestBuilder getRequest = MockMvcRequestBuilders.get(
            "/mdm-api/ui/ssku/get?supplierId={supplierId}&shopSku={shopSku}",
            KEY_3P.getSupplierId(), KEY_3P.getShopSku()
        ).accept(MediaType.APPLICATION_JSON_UTF8);
        MvcResult result = mockMvc.perform(getRequest)
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON_UTF8))
            .andExpect(MockMvcResultMatchers.jsonPath("$.shopSkuKey.supplierId").value(KEY_3P.getSupplierId()))
            .andExpect(MockMvcResultMatchers.jsonPath("$.shopSkuKey.shopSku").value(KEY_3P.getShopSku()))
            .andReturn();

        String jsonSsku = result.getResponse().getContentAsString();
        CommonSsku ssku = JsonMapper.DEFAULT_OBJECT_MAPPER.readValue(jsonSsku, CommonSsku.class);
        Assertions.assertThat(ssku.getKey()).isEqualTo(KEY_3P);
        Assertions.assertThat(BASE_MDM_PARAM_IDS).containsAnyElementsOf(ssku.getBaseValuesByParamId().keySet());
        Assertions.assertThat(SERVICE_MDM_PARAM_IDS).containsAnyElementsOf(ssku.getBaseValuesByParamId().keySet());
    }

    @Test
    public void whenGetBusinessSskuWithMetaShouldNotReturnServiceParamsInMeta() throws Exception {
        MockHttpServletRequestBuilder getRequest = MockMvcRequestBuilders.get(
            "/mdm-api/ui/ssku/get_with_metadata/{supplierId}/{shopSku}",
            BUSINESS_KEY.getSupplierId(), BUSINESS_KEY.getShopSku()
        ).accept(MediaType.APPLICATION_JSON_UTF8);
        MvcResult result = mockMvc.perform(getRequest)
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON_UTF8))
            .andExpect(
                MockMvcResultMatchers.jsonPath("$.commonSsku.shopSkuKey.supplierId").value(BUSINESS_KEY.getSupplierId())
            )
            .andExpect(
                MockMvcResultMatchers.jsonPath("$.commonSsku.shopSkuKey.shopSku").value(BUSINESS_KEY.getShopSku()))
            .andExpect(MockMvcResultMatchers.jsonPath("$.metadata").isArray())
            .andExpect(MockMvcResultMatchers.jsonPath("$.metadata", hasItem(hasEntry("title", "Код ТН ВЭД"))))
            .andExpect(MockMvcResultMatchers.jsonPath("$.metadata",
                not(hasItem(hasEntry("title", "Минимальная партия поставки")))))
            .andExpect(MockMvcResultMatchers.jsonPath("$.metadata",
                not(hasItem(hasEntry("title", "Квант поставки")))))
            .andExpect(MockMvcResultMatchers.jsonPath("$.metadata",
                not(hasItem(hasEntry("title", "Мин. транспортная единица")))))
            .andExpect(MockMvcResultMatchers.jsonPath("$.metadata",
                not(hasItem(hasEntry("title", "Время поставки")))))
            .andExpect(MockMvcResultMatchers.jsonPath("$.metadata",
                not(hasItem(hasEntry("title", "Календарь поставок")))))
            .andExpect(MockMvcResultMatchers.jsonPath("$.metadata",
                not(hasItem(hasEntry("title", "Кол-во в упаковке QTY_IN_PACK")))))
            .andReturn();
    }

    @Test
    public void whenGetServiceSskuWithMetaShouldReturnServiceParamsInMeta() throws Exception {
        MockHttpServletRequestBuilder getRequest = MockMvcRequestBuilders.get(
            "/mdm-api/ui/ssku/get_with_metadata/{supplierId}/{shopSku}",
            SUPPLIER_KEY1.getSupplierId(), SUPPLIER_KEY1.getShopSku()
        ).accept(MediaType.APPLICATION_JSON_UTF8);
        MvcResult result = mockMvc.perform(getRequest)
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON_UTF8))
            .andExpect(
                MockMvcResultMatchers.jsonPath("$.commonSsku.shopSkuKey.supplierId")
                    .value(SUPPLIER_KEY1.getSupplierId()))
            .andExpect(
                MockMvcResultMatchers.jsonPath("$.commonSsku.shopSkuKey.shopSku").value(SUPPLIER_KEY1.getShopSku()))
            .andExpect(MockMvcResultMatchers.jsonPath("$.metadata").isArray())
            .andExpect(MockMvcResultMatchers.jsonPath("$.metadata", hasItem(hasEntry("title", "Код ТН ВЭД"))))
            .andExpect(MockMvcResultMatchers.jsonPath("$.metadata",
                hasItem(hasEntry("title", "Минимальная партия поставки"))))
            .andExpect(MockMvcResultMatchers.jsonPath("$.metadata",
                hasItem(hasEntry("title", "Квант поставки"))))
            .andExpect(MockMvcResultMatchers.jsonPath("$.metadata",
                hasItem(hasEntry("title", "Мин. транспортная единица"))))
            .andExpect(MockMvcResultMatchers.jsonPath("$.metadata",
                hasItem(hasEntry("title", "Время поставки"))))
            .andExpect(MockMvcResultMatchers.jsonPath("$.metadata",
                hasItem(hasEntry("title", "Календарь поставок"))))
            .andExpect(MockMvcResultMatchers.jsonPath("$.metadata",
                hasItem(hasEntry("title", "Кол-во в упаковке QTY_IN_PACK"))))
            .andReturn();
    }


    @Test
    public void whenGetWhiteSskuShouldReturnNotFound() throws Exception {
        MockHttpServletRequestBuilder getRequest = MockMvcRequestBuilders.get(
            "/mdm-api/ui/ssku/get?supplierId={supplierId}&shopSku={shopSku}",
            WHITE_ORPHAN_SUPPLIER_KEY.getSupplierId(), WHITE_ORPHAN_SUPPLIER_KEY.getShopSku()
        ).accept(MediaType.APPLICATION_JSON_UTF8);
        mockMvc.perform(getRequest)
            .andExpect(MockMvcResultMatchers.status().isNotFound());
    }

    @Test
    public void whenGetSskuShouldCompleteWithSskuGoldenParamValues() throws Exception {
        SskuGoldenParamValue paramValue = new SskuGoldenParamValue();
        paramValue.setShopSkuKey(KEY_3P);
        paramValue.setMdmParamId(KnownMdmParams.SSKU_HEIGHT);
        paramValue.setNumeric(BigDecimal.valueOf(100));
        goldSskuRepository.insertOrUpdateSsku(new CommonSsku(KEY_3P).addBaseValue(paramValue));

        MockHttpServletRequestBuilder getRequest = MockMvcRequestBuilders.get(
            "/mdm-api/ui/ssku/get?supplierId={supplierId}&shopSku={shopSku}",
            KEY_3P.getSupplierId(), KEY_3P.getShopSku()
        ).accept(MediaType.APPLICATION_JSON_UTF8);
        MvcResult result = mockMvc.perform(getRequest)
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON_UTF8))
            .andExpect(MockMvcResultMatchers.jsonPath("$.shopSkuKey.supplierId").value(KEY_3P.getSupplierId()))
            .andExpect(MockMvcResultMatchers.jsonPath("$.shopSkuKey.shopSku").value(KEY_3P.getShopSku()))
            .andReturn();

        String jsonSsku = result.getResponse().getContentAsString();
        CommonSsku ssku = JsonMapper.DEFAULT_OBJECT_MAPPER.readValue(jsonSsku, CommonSsku.class);
        Assertions.assertThat(ssku.getKey()).isEqualTo(KEY_3P);
        Assertions.assertThat(BASE_MDM_PARAM_IDS).containsAnyElementsOf(ssku.getBaseValuesByParamId().keySet());
        Assertions.assertThat(SERVICE_MDM_PARAM_IDS).containsAnyElementsOf(ssku.getBaseValuesByParamId().keySet());
        Assertions.assertThat(ssku.getBaseValuesByParamId().get(KnownMdmParams.SSKU_HEIGHT).getNumeric())
            .isEqualTo(paramValue.getNumeric());
    }

    @Test
    public void whenGetUnknownSskuShouldReturnNotFound() throws Exception {
        MockHttpServletRequestBuilder getRequest = MockMvcRequestBuilders.get(
            "/mdm-api/ui/ssku/get?supplierId={supplierId}&shopSku={shopSku}",
            UNKNOWN_SUPPLIER_KEY.getSupplierId(), UNKNOWN_SUPPLIER_KEY.getShopSku()
        ).accept(MediaType.APPLICATION_JSON_UTF8);
        mockMvc.perform(getRequest)
            .andExpect(MockMvcResultMatchers.status().isNotFound());
    }

    @Test
    public void whenGetSskuShouldReturnMskuIdIfHasMapping() throws Exception {
        final long expectedMskuId = 12345L;
        final int expectedCategoryId = 12345;
        MappingCacheDao mapping = new MappingCacheDao()
            .setSupplierId(KEY_3P.getSupplierId())
            .setShopSku(KEY_3P.getShopSku())
            .setMskuId(expectedMskuId)
            .setCategoryId(expectedCategoryId);
        mappingsCacheRepository.insert(mapping);

        MockHttpServletRequestBuilder getRequest = MockMvcRequestBuilders.get(
            "/mdm-api/ui/ssku/get?supplierId={supplierId}&shopSku={shopSku}",
            KEY_3P.getSupplierId(), KEY_3P.getShopSku()
        ).accept(MediaType.APPLICATION_JSON_UTF8);
        MvcResult result = mockMvc.perform(getRequest)
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON_UTF8))
            .andExpect(MockMvcResultMatchers.jsonPath("$.shopSkuKey.supplierId").value(KEY_3P.getSupplierId()))
            .andExpect(MockMvcResultMatchers.jsonPath("$.shopSkuKey.shopSku").value(KEY_3P.getShopSku()))
            .andReturn();

        String jsonSsku = result.getResponse().getContentAsString();
        CommonSsku ssku = JsonMapper.DEFAULT_OBJECT_MAPPER.readValue(jsonSsku, CommonSsku.class);
        Assertions.assertThat(ssku.getKey()).isEqualTo(KEY_3P);

        Long actualMskuId = ssku.getBaseValuesByParamId()
            .get(KnownMdmParams.MSKU_ID_REFERENCE)
            .getNumeric()
            .orElseThrow()
            .longValue();
        Integer actualCategoryId = ssku.getBaseValuesByParamId()
            .get(KnownMdmParams.CATEGORY_ID)
            .getNumeric()
            .orElseThrow()
            .intValue();
        Assertions.assertThat(actualMskuId).isEqualTo(expectedMskuId);
        Assertions.assertThat(actualCategoryId).isEqualTo(expectedCategoryId);
    }

    @Test
    public void whenFindNonexistentSskuShouldReturnEmptyResult() throws Exception {
        MockHttpServletRequestBuilder getRequest = MockMvcRequestBuilders.get(
            "/mdm-api/ui/ssku/find?searchString={shopSkuKey}", "99191_BAD-SHOPSKU-KEY"
        ).accept(MediaType.APPLICATION_JSON_UTF8);

        mockMvc.perform(getRequest)
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON_UTF8))
            .andExpect(MockMvcResultMatchers.jsonPath("$.totalCount").value(0))
            .andExpect(MockMvcResultMatchers.jsonPath("$.items[*]").isEmpty())
            .andReturn();
    }

    @Test
    public void whenFindWhiteSskuShouldReturnEmptyResult() throws Exception {
        MockHttpServletRequestBuilder getRequest = MockMvcRequestBuilders.get(
            "/mdm-api/ui/ssku/find?searchString={shopSkuKey}",
            WHITE_ORPHAN_SUPPLIER_KEY.getSupplierId() + "_" + WHITE_ORPHAN_SUPPLIER_KEY.getShopSku()
        ).accept(MediaType.APPLICATION_JSON_UTF8);

        mockMvc.perform(getRequest)
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON_UTF8))
            .andExpect(MockMvcResultMatchers.jsonPath("$.totalCount").value(0))
            .andExpect(MockMvcResultMatchers.jsonPath("$.items[*]").isEmpty())
            .andReturn();
    }

    @Test
    public void whenFindUnknownSupplierSskuShouldReturnEmptyResult() throws Exception {
        MockHttpServletRequestBuilder getRequest = MockMvcRequestBuilders.get(
            "/mdm-api/ui/ssku/find?searchString={shopSkuKey}",
            UNKNOWN_SUPPLIER_KEY.getSupplierId() + "_" + UNKNOWN_SUPPLIER_KEY.getShopSku()
        ).accept(MediaType.APPLICATION_JSON_UTF8);

        mockMvc.perform(getRequest)
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON_UTF8))
            .andExpect(MockMvcResultMatchers.jsonPath("$.totalCount").value(0))
            .andExpect(MockMvcResultMatchers.jsonPath("$.items[*]").isEmpty())
            .andReturn();
    }

    @Test
    public void whenFindExistentSskuShouldReturnIt() throws Exception {
        MockHttpServletRequestBuilder getRequest = MockMvcRequestBuilders.get(
            "/mdm-api/ui/ssku/find?searchString={shopSkuKey}", KEY_3P.getSupplierId() + " " + KEY_3P.getShopSku()
        ).accept(MediaType.APPLICATION_JSON_UTF8);

        mockMvc.perform(getRequest)
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON_UTF8))
            .andExpect(MockMvcResultMatchers.jsonPath("$.totalCount").value(1))
            .andExpect(MockMvcResultMatchers.jsonPath("$.items.length()").value(1))
            .andExpect(MockMvcResultMatchers.jsonPath("$.items[0].shopSkuKey.supplierId").value(KEY_3P.getSupplierId()))
            .andExpect(MockMvcResultMatchers.jsonPath("$.items[0].shopSkuKey.shopSku").value(KEY_3P.getShopSku()))
            .andReturn();
    }

    @Test
    public void whenFindByExistentMskuShouldReturnSsku() throws Exception {
        long expectedMskuId1 = 12345789L;
        long expectedMskuId2 = 45678910L;
        int expectedCategoryId = 12345;
        MappingCacheDao mapping1 = new MappingCacheDao()
            .setSupplierId(KEY_3P.getSupplierId())
            .setShopSku(KEY_3P.getShopSku())
            .setMskuId(expectedMskuId1)
            .setCategoryId(expectedCategoryId);
        MappingCacheDao mapping2 = new MappingCacheDao()
            .setSupplierId(BUSINESS_KEY.getSupplierId())
            .setShopSku(BUSINESS_KEY.getShopSku())
            .setMskuId(expectedMskuId2)
            .setCategoryId(expectedCategoryId);
        MappingCacheDao mapping3 = new MappingCacheDao()
            .setSupplierId(WHITE_ORPHAN_SUPPLIER_KEY.getSupplierId())
            .setShopSku(WHITE_ORPHAN_SUPPLIER_KEY.getShopSku())
            .setMskuId(expectedMskuId1)
            .setCategoryId(expectedCategoryId);
        MappingCacheDao mapping4 = new MappingCacheDao()
            .setSupplierId(UNKNOWN_SUPPLIER_KEY.getSupplierId())
            .setShopSku(UNKNOWN_SUPPLIER_KEY.getShopSku())
            .setMskuId(expectedMskuId1)
            .setCategoryId(expectedCategoryId);
        mappingsCacheRepository.insertOrUpdateAll(List.of(mapping1, mapping2, mapping3, mapping4));
        referenceItemRepository.insert(ItemWrapperTestUtil.createReferenceNetWeightItem(
            BUSINESS_KEY.getSupplierId(), BUSINESS_KEY.getShopSku(), 42));
        MockHttpServletRequestBuilder getRequest = MockMvcRequestBuilders.get(
                "/mdm-api/ui/ssku/find?mskuIdSearchString={ids}", expectedMskuId1 + "," + expectedMskuId2)
            .accept(MediaType.APPLICATION_JSON_UTF8);

        mockMvc.perform(getRequest)
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON_UTF8))
            .andExpect(MockMvcResultMatchers.jsonPath("$.totalCount").value(2))
            .andExpect(MockMvcResultMatchers.jsonPath("$.items.length()").value(2))
            .andExpect(MockMvcResultMatchers.jsonPath("$.items[0].shopSkuKey.supplierId")
                .value(KEY_3P.getSupplierId()))
            .andExpect(MockMvcResultMatchers.jsonPath("$.items[0].shopSkuKey.shopSku").value(KEY_3P.getShopSku()))
            .andExpect(MockMvcResultMatchers.jsonPath("$.items[1].shopSkuKey.supplierId")
                .value(BUSINESS_KEY.getSupplierId()))
            .andExpect(MockMvcResultMatchers.jsonPath("$.items[1].shopSkuKey.shopSku").value(BUSINESS_KEY.getShopSku()))
            .andReturn();
    }

    @Test
    public void whenFindByExistentMskuAndSskuShouldReturnIntersection() throws Exception {
        long expectedMskuId = 12345789L;
        int expectedCategoryId = 12345;
        MappingCacheDao mapping1 = new MappingCacheDao()
            .setSupplierId(KEY_3P.getSupplierId())
            .setShopSku(KEY_3P.getShopSku())
            .setMskuId(expectedMskuId)
            .setCategoryId(expectedCategoryId);
        MappingCacheDao mapping2 = new MappingCacheDao()
            .setSupplierId(BUSINESS_KEY.getSupplierId())
            .setShopSku(BUSINESS_KEY.getShopSku())
            .setMskuId(expectedMskuId)
            .setCategoryId(expectedCategoryId);
        mappingsCacheRepository.insertOrUpdateAll(List.of(mapping1, mapping2));
        referenceItemRepository.insert(ItemWrapperTestUtil.createReferenceNetWeightItem(
            BUSINESS_KEY.getSupplierId(), BUSINESS_KEY.getShopSku(), 42));
        MockHttpServletRequestBuilder getRequest = MockMvcRequestBuilders.get(
                "/mdm-api/ui/ssku/find?mskuIdSearchString={ids}&searchString={shopSkuKey}",
                expectedMskuId, KEY_3P.getSupplierId() + " " + KEY_3P.getShopSku())
            .accept(MediaType.APPLICATION_JSON_UTF8);

        mockMvc.perform(getRequest)
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON_UTF8))
            .andExpect(MockMvcResultMatchers.jsonPath("$.totalCount").value(1))
            .andExpect(MockMvcResultMatchers.jsonPath("$.items.length()").value(1))
            .andExpect(MockMvcResultMatchers.jsonPath("$.items[0].shopSkuKey.supplierId").value(KEY_3P.getSupplierId()))
            .andExpect(MockMvcResultMatchers.jsonPath("$.items[0].shopSkuKey.shopSku").value(KEY_3P.getShopSku()))
            .andReturn();
    }

    @Test
    public void whenFindByNonexistentMskuShouldReturnEmptyResult() throws Exception {
        Long mskuId = 99999999L;

        MockHttpServletRequestBuilder getRequest = MockMvcRequestBuilders.get(
                "/mdm-api/ui/ssku/find?mskuIdSearchString={ids}", mskuId)
            .accept(MediaType.APPLICATION_JSON_UTF8);

        mockMvc.perform(getRequest)
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON_UTF8))
            .andExpect(MockMvcResultMatchers.jsonPath("$.totalCount").value(0))
            .andExpect(MockMvcResultMatchers.jsonPath("$.items[*]").isEmpty())
            .andReturn();
    }

    @Test
    public void whenFindByNonexistentMskuUrlEncodedShouldReturnEmptyResult() throws Exception {
        Long mskuId = 99999999L;
        String urlEncodedMskuId = UrlUtils.urlEncode(mskuId.toString());

        MockHttpServletRequestBuilder getRequest = MockMvcRequestBuilders.get(
                "/mdm-api/ui/ssku/find?mskuIdSearchString=" + urlEncodedMskuId)
            .accept(MediaType.APPLICATION_JSON_UTF8);

        mockMvc.perform(getRequest)
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON_UTF8))
            .andExpect(MockMvcResultMatchers.jsonPath("$.totalCount").value(0))
            .andExpect(MockMvcResultMatchers.jsonPath("$.items[*]").isEmpty())
            .andReturn();
    }

    @Test
    public void whenFindUrlEncodedSskuShouldReturnIt() throws Exception {
        String urlEncodedShopSkuKey = UrlUtils.urlEncode(KEY_3P.getSupplierId() + " " + KEY_3P.getShopSku());

        MockHttpServletRequestBuilder getRequest = MockMvcRequestBuilders.request(HttpMethod.GET,
            URI.create("/mdm-api/ui/ssku/find?searchString=" + urlEncodedShopSkuKey)
        ).accept(MediaType.APPLICATION_JSON_UTF8);

        mockMvc.perform(getRequest)
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON_UTF8))
            .andExpect(MockMvcResultMatchers.jsonPath("$.totalCount").value(1))
            .andExpect(MockMvcResultMatchers.jsonPath("$.items.length()").value(1))
            .andExpect(MockMvcResultMatchers.jsonPath("$.items[0].shopSkuKey.supplierId").value(KEY_3P.getSupplierId()))
            .andExpect(MockMvcResultMatchers.jsonPath("$.items[0].shopSkuKey.shopSku").value(KEY_3P.getShopSku()))
            .andReturn();
    }

    @Test
    public void whenGetByBusinessSupplierIdShouldReturnMergedSskus() throws Exception {
        keyValueService.putValue(MdmProperties.BUSINESS_MERGE_ENABLED_KEY, true);
        keyValueService.putValue(MdmProperties.ENABLE_BUSINESS_STATE_UPDATE_IN_MDM_SUPPLIER_CACHE, true);
        keyValueService.invalidateCache();

        MockHttpServletRequestBuilder getRequest = MockMvcRequestBuilders.get(
            "/mdm-api/ui/ssku/{supplierId}/{shopSku}", BUSINESS_KEY.getSupplierId(), SUPPLIER_KEY1.getShopSku()
        ).accept(MediaType.APPLICATION_JSON_UTF8);
        MvcResult result = mockMvc.perform(getRequest)
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON_UTF8))
            .andExpect(MockMvcResultMatchers.jsonPath("$.shopSkuKey.supplierId").value(BUSINESS_KEY.getSupplierId()))
            .andExpect(MockMvcResultMatchers.jsonPath("$.shopSkuKey.shopSku").value(SUPPLIER_KEY1.getShopSku()))
            .andReturn();

        String jsonSsku = result.getResponse().getContentAsString();
        CommonSsku ssku = JsonMapper.DEFAULT_OBJECT_MAPPER.readValue(jsonSsku, CommonSsku.class);
        Assertions.assertThat(ssku.getKey().getSupplierId()).isEqualTo(BUSINESS_KEY.getSupplierId());
        Assertions.assertThat(ssku.getKey().getShopSku()).isEqualTo(SUPPLIER_KEY1.getShopSku());

        Map<Long, SskuParamValue> sskusByMdmParamId = ssku.getBaseValuesByParamId();
        Set<Long> resultMdmParamIds = new HashSet<>(sskusByMdmParamId.keySet());
        Assertions.assertThat(BASE_MDM_PARAM_IDS).containsAll(resultMdmParamIds);
        Assertions.assertThat(SERVICE_MDM_PARAM_IDS).doesNotContainAnyElementsOf(resultMdmParamIds);
    }

    @Test
    public void whenGetByServiceSupplierIdShouldReturnServiceSskuWithMergedPart() throws Exception {
        keyValueService.putValue(MdmProperties.BUSINESS_MERGE_ENABLED_KEY, true);
        keyValueService.putValue(MdmProperties.ENABLE_BUSINESS_STATE_UPDATE_IN_MDM_SUPPLIER_CACHE, true);
        keyValueService.invalidateCache();

        MockHttpServletRequestBuilder getRequest = MockMvcRequestBuilders.get(
            "/mdm-api/ui/ssku/{supplierId}/{shopSku}", SUPPLIER_KEY1.getSupplierId(), SUPPLIER_KEY1.getShopSku()
        ).accept(MediaType.APPLICATION_JSON_UTF8);
        MvcResult result = mockMvc.perform(getRequest)
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON_UTF8))
            .andExpect(MockMvcResultMatchers.jsonPath("$.shopSkuKey.supplierId").value(SUPPLIER_KEY1.getSupplierId()))
            .andExpect(MockMvcResultMatchers.jsonPath("$.shopSkuKey.shopSku").value(SUPPLIER_KEY1.getShopSku()))
            .andReturn();

        String jsonSsku = result.getResponse().getContentAsString();
        CommonSsku ssku = JsonMapper.DEFAULT_OBJECT_MAPPER.readValue(jsonSsku, CommonSsku.class);
        Assertions.assertThat(ssku.getKey().getSupplierId()).isEqualTo(SUPPLIER_KEY1.getSupplierId());
        Assertions.assertThat(ssku.getKey().getShopSku()).isEqualTo(SUPPLIER_KEY1.getShopSku());

        Map<Long, SskuParamValue> sskusByMdmParamId = ssku.getBaseValuesByParamId();
        Set<Long> resultMdmParamIds = new HashSet<>(sskusByMdmParamId.keySet());
        Assertions.assertThat(BASE_MDM_PARAM_IDS).containsAnyElementsOf(resultMdmParamIds);
        Assertions.assertThat(SERVICE_MDM_PARAM_IDS).containsAnyElementsOf(resultMdmParamIds);
    }

    @Test
    public void whenGetByBusinessSupplierIdShouldReturn404() throws Exception {
        keyValueService.putValue(MdmProperties.ENABLE_BUSINESS_STATE_UPDATE_IN_MDM_SUPPLIER_CACHE, true);
        keyValueService.putValue(MdmProperties.BUSINESS_MERGE_ENABLED_KEY, true);
        keyValueService.invalidateCache();

        MockHttpServletRequestBuilder getRequest = MockMvcRequestBuilders.get(
            "/mdm-api/ui/ssku/{supplierId}/{shopSku}", BUSINESS_KEY.getSupplierId(), "non_existing_ssku"
        ).accept(MediaType.APPLICATION_JSON_UTF8);
        mockMvc.perform(getRequest)
            .andExpect(MockMvcResultMatchers.status().isNotFound())
            .andReturn();
    }

    @Test
    public void whenUpdateSskuShouldReturnOk() throws Exception {
        MockHttpServletRequestBuilder getRequest = MockMvcRequestBuilders.get(
            "/mdm-api/ui/ssku/get?supplierId={supplierId}&shopSku={shopSku}",
            KEY_3P.getSupplierId(), KEY_3P.getShopSku()
        ).accept(MediaType.APPLICATION_JSON_UTF8);
        MvcResult getResult = mockMvc.perform(getRequest)
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON_UTF8))
            .andExpect(MockMvcResultMatchers.jsonPath("$.shopSkuKey.supplierId").value(KEY_3P.getSupplierId()))
            .andExpect(MockMvcResultMatchers.jsonPath("$.shopSkuKey.shopSku").value(KEY_3P.getShopSku()))
            .andReturn();

        String jsonSsku = getResult.getResponse().getContentAsString();
        MockHttpServletRequestBuilder updateRequest = MockMvcRequestBuilders.post("/mdm-api/ui/ssku/update")
            .contentType(MediaType.APPLICATION_JSON_UTF8)
            .content(jsonSsku);

        mockMvc.perform(updateRequest)
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andReturn();
    }

    @Test
    public void whenUpdateSskuAsAdminShouldReturnOk() {
        String login = "vasya";
        mdmUserRepository.insert(new MdmUser().setLogin(login).setRoles(Set.of("MDM_UI_ADMIN")));
        SecurityUtil.wrapWithLogin(login, () -> {
            try {
                CommonSsku update =
                    getCommonSskuWithWeightDimensionalParams(KEY_3P, 1, 10, 10, 10);
                String updateAsJsonString = JsonMapper.DEFAULT_OBJECT_MAPPER.writeValueAsString(update);
                MockHttpServletRequestBuilder updateRequest = MockMvcRequestBuilders
                    .post("/mdm-api/ui/ssku/update?userType={userType}", MasterDataSourceType.MDM_ADMIN)
                    .contentType(MediaType.APPLICATION_JSON_UTF8)
                    .content(updateAsJsonString);

                mockMvc.perform(updateRequest)
                    .andExpect(MockMvcResultMatchers.status().isOk())
                    .andReturn();

                Assertions.assertThat(silverSskuRepository.findParametrizedSsku(KEY_3P))
                    .isNotEmpty()
                    .allMatch(spv -> spv.getSilverKey().getSourceType() == MasterDataSourceType.MDM_ADMIN);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Test
    public void whenUpdateSskuAsOperatorShouldReturnOk() throws Exception {
        CommonSsku update =
            getCommonSskuWithWeightDimensionalParams(KEY_3P, 1, 10, 10, 10);
        String updateAsJsonString = JsonMapper.DEFAULT_OBJECT_MAPPER.writeValueAsString(update);
        MockHttpServletRequestBuilder updateRequest = MockMvcRequestBuilders
            .post("/mdm-api/ui/ssku/update?userType={userType}", MasterDataSourceType.MDM_OPERATOR)
            .contentType(MediaType.APPLICATION_JSON_UTF8)
            .content(updateAsJsonString);

        mockMvc.perform(updateRequest)
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andReturn();

        Assertions.assertThat(silverSskuRepository.findParametrizedSsku(KEY_3P))
            .isNotEmpty()
            .allMatch(spv -> spv.getSilverKey().getSourceType() == MasterDataSourceType.MDM_OPERATOR);
    }

    @Test
    public void whenUpdateSskuAsAdminWithoutGrantsShouldReturnError() throws Exception {
        CommonSsku update =
            getCommonSskuWithWeightDimensionalParams(KEY_3P, 1, 10, 10, 10);
        String updateAsJsonString = JsonMapper.DEFAULT_OBJECT_MAPPER.writeValueAsString(update);
        MockHttpServletRequestBuilder updateRequest = MockMvcRequestBuilders
            .post("/mdm-api/ui/ssku/update?userType={userType}", MasterDataSourceType.MDM_ADMIN)
            .contentType(MediaType.APPLICATION_JSON_UTF8)
            .content(updateAsJsonString);

        mockMvc.perform(updateRequest)
            .andExpect(MockMvcResultMatchers.status().isBadRequest())
            .andExpect(MockMvcResultMatchers.xpath("/List/item[1]").string(
                "AccessException: У пользователя недостаточно прав для выполнения операции от имени администратора МДМ."
            ))
            .andReturn();

        Assertions.assertThat(silverSskuRepository.findParametrizedSsku(KEY_3P)).isEmpty();
    }

    @Test
    public void whenUpdateSskuShouldReturnErrors() throws Exception {
        MockHttpServletRequestBuilder getRequest = MockMvcRequestBuilders.get(
            "/mdm-api/ui/ssku/get?supplierId={supplierId}&shopSku={shopSku}",
            KEY_3P.getSupplierId(), KEY_3P.getShopSku()
        ).accept(MediaType.APPLICATION_JSON_UTF8);
        MvcResult getResult = mockMvc.perform(getRequest)
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON_UTF8))
            .andExpect(MockMvcResultMatchers.jsonPath("$.shopSkuKey.supplierId").value(KEY_3P.getSupplierId()))
            .andExpect(MockMvcResultMatchers.jsonPath("$.shopSkuKey.shopSku").value(KEY_3P.getShopSku()))
            .andReturn();

        String jsonSsku = getResult.getResponse().getContentAsString();
        jsonSsku = jsonSsku.replace(KEY_3P.getShopSku(), KEY_3P.getShopSku() + "meow");
        MockHttpServletRequestBuilder updateRequest = MockMvcRequestBuilders.post("/mdm-api/ui/ssku/update")
            .contentType(MediaType.APPLICATION_JSON_UTF8)
            .content(jsonSsku);

        mockMvc.perform(updateRequest)
            .andExpect(MockMvcResultMatchers.status().isBadRequest())
            .andExpect(MockMvcResultMatchers.xpath("/List/item[1]").string("SSKU не найдена"))
            .andReturn();
    }

    @Test
    public void whenUpdateWhiteSskuShouldReturnErrors() throws Exception {
        CommonSsku ssku = new CommonSsku(WHITE_ORPHAN_SUPPLIER_KEY);
        ssku.addBaseValue(new SskuParamValue()
            .setShopSkuKey(WHITE_ORPHAN_SUPPLIER_KEY)
            .setMdmParamId(KnownMdmParams.MANUFACTURER_COUNTRY)
            .setString("Россия")
            .setXslName("manufacturerCountry")
        );
        String jsonSsku = JsonMapper.DEFAULT_OBJECT_MAPPER.writeValueAsString(ssku);
        MockHttpServletRequestBuilder updateRequest = MockMvcRequestBuilders.post("/mdm-api/ui/ssku/update")
            .contentType(MediaType.APPLICATION_JSON_UTF8)
            .content(jsonSsku);

        mockMvc.perform(updateRequest)
            .andExpect(MockMvcResultMatchers.status().isBadRequest())
            .andExpect(MockMvcResultMatchers.xpath("/List/item[1]")
                .string("SSKU по белому поставщику - данные не будут сохранены"))
            .andReturn();
    }

    @Test
    public void whenUpdateUnknownSupplierSskuShouldReturnErrors() throws Exception {
        CommonSsku ssku = new CommonSsku(UNKNOWN_SUPPLIER_KEY);
        ssku.addBaseValue(new SskuParamValue()
            .setShopSkuKey(UNKNOWN_SUPPLIER_KEY)
            .setMdmParamId(KnownMdmParams.MANUFACTURER_COUNTRY)
            .setString("Россия")
            .setXslName("manufacturerCountry")
        );
        String jsonSsku = JsonMapper.DEFAULT_OBJECT_MAPPER.writeValueAsString(ssku);
        MockHttpServletRequestBuilder updateRequest = MockMvcRequestBuilders.post("/mdm-api/ui/ssku/update")
            .contentType(MediaType.APPLICATION_JSON_UTF8)
            .content(jsonSsku);

        mockMvc.perform(updateRequest)
            .andExpect(MockMvcResultMatchers.status().isBadRequest())
            .andExpect(MockMvcResultMatchers.xpath("/List/item[1]")
                .string("SSKU по неизвестному поставщику - данные не будут сохранены"))
            .andReturn();
    }

    @Test
    public void whenUpdateSskuByBusinessSupplierIdShouldReturnOk() throws Exception {
        keyValueService.putValue(MdmProperties.ENABLE_BUSINESS_STATE_UPDATE_IN_MDM_SUPPLIER_CACHE, true);
        keyValueService.putValue(MdmProperties.BUSINESS_MERGE_ENABLED_KEY, true);
        keyValueService.invalidateCache();

        String manufacturer = "Best manufacturer";
        boolean traceable = true;
        int quantityInPack = 15;

        // get service sskus before update
        CommonSsku firstServiceSupplierBeforeUpdate = getCommonSsku(SUPPLIER_KEY1);
        CommonSsku secondServiceSupplierBeforeUpdate = getCommonSsku(SUPPLIER_KEY2);
        int firstServiceQtyInPack = firstServiceSupplierBeforeUpdate.getBaseValuesByParamId()
            .get(KnownMdmParams.QUANTITY_IN_PACK).getNumeric().get().intValue();
        int secondServiceQtyInPack = secondServiceSupplierBeforeUpdate.getBaseValuesByParamId()
            .get(KnownMdmParams.QUANTITY_IN_PACK).getNumeric().get().intValue();

        // update service sskus by business supplier id
        CommonSsku bussinessSsku = getCommonSsku(BUSINESS_KEY.getSupplierId(), SUPPLIER_KEY1.getShopSku(),
            manufacturer, traceable, quantityInPack);
        String jsonBusinessSsku = JsonMapper.DEFAULT_OBJECT_MAPPER.writeValueAsString(bussinessSsku);
        MockHttpServletRequestBuilder updateRequest = MockMvcRequestBuilders.post("/mdm-api/ui/ssku/update")
            .contentType(MediaType.APPLICATION_JSON_UTF8)
            .content(jsonBusinessSsku);

        mockMvc.perform(updateRequest)
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andReturn();

        processSskusToRefresh();

        // check that service sskus were updated and get() method returns merged record for each service ssku
        CommonSsku firstServiceSskuAfterUpdate = getCommonSsku(SUPPLIER_KEY1);
        CommonSsku secondServiceSskuAfterUpdate = getCommonSsku(SUPPLIER_KEY2);
        Map<Long, SskuParamValue> firstServiceSskuBaseValues = firstServiceSskuAfterUpdate.getBaseValuesByParamId();
        Map<Long, SskuParamValue> secondServiceSskuBaseValues = secondServiceSskuAfterUpdate.getBaseValuesByParamId();

        Assertions.assertThat(firstServiceSupplierBeforeUpdate).isNotEqualTo(firstServiceSskuAfterUpdate);
        Assertions.assertThat(secondServiceSupplierBeforeUpdate).isNotEqualTo(secondServiceSskuAfterUpdate);

        Assertions.assertThat(firstServiceSskuBaseValues.get(KnownMdmParams.MANUFACTURER).getString().get())
            .isEqualTo(manufacturer);
        Assertions.assertThat(firstServiceSskuBaseValues.get(KnownMdmParams.IS_TRACEABLE).getBool().get())
            .isEqualTo(true);
        Assertions.assertThat(firstServiceSskuBaseValues.get(KnownMdmParams.QUANTITY_IN_PACK).getNumeric().get())
            .isEqualTo(String.valueOf(firstServiceQtyInPack));

        Assertions.assertThat(secondServiceSskuBaseValues.get(KnownMdmParams.MANUFACTURER).getString().get())
            .isEqualTo(manufacturer);
        Assertions.assertThat(secondServiceSskuBaseValues.get(KnownMdmParams.IS_TRACEABLE).getBool().get())
            .isEqualTo(true);
        Assertions.assertThat(secondServiceSskuBaseValues.get(KnownMdmParams.QUANTITY_IN_PACK).getNumeric().get())
            .isEqualTo(String.valueOf(secondServiceQtyInPack));

        Assertions.assertThat(BASE_MDM_PARAM_IDS).containsAnyElementsOf(firstServiceSskuBaseValues.keySet());
        Assertions.assertThat(SERVICE_MDM_PARAM_IDS).containsAnyElementsOf(firstServiceSskuBaseValues.keySet());
        Assertions.assertThat(BASE_MDM_PARAM_IDS).containsAnyElementsOf(secondServiceSskuBaseValues.keySet());
        Assertions.assertThat(SERVICE_MDM_PARAM_IDS).containsAnyElementsOf(secondServiceSskuBaseValues.keySet());
    }

    @Test
    public void whenUpdateSskuByBusinessSupplierIdShouldUpdateWithoutServiceParams() throws Exception {
        keyValueService.putValue(MdmProperties.ENABLE_BUSINESS_STATE_UPDATE_IN_MDM_SUPPLIER_CACHE, true);
        keyValueService.putValue(MdmProperties.BUSINESS_MERGE_ENABLED_KEY, true);
        keyValueService.invalidateCache();

        String manufacturer = "Best manufacturer";
        boolean traceable = false;
        int quantityInPack = 15;

        // get service sskus before update
        CommonSsku firstServiceSupplierBeforeUpdate = getCommonSsku(SUPPLIER_KEY1);
        CommonSsku secondServiceSupplierBeforeUpdate = getCommonSsku(SUPPLIER_KEY2);

        // generate business ssku for update
        CommonSsku bussinessSsku = getCommonSsku(BUSINESS_KEY.getSupplierId(), SUPPLIER_KEY1.getShopSku(),
            manufacturer, traceable, (quantityInPack + 5));

        // add service params
        MdmParamValue addParamValue = getNumericMdmParamValue(KnownMdmParams.MIN_SHIPMENT, 2);
        bussinessSsku.addBaseValue(addParamValue);
        addParamValue = getNumericMdmParamValue(KnownMdmParams.QUANTUM_OF_SUPPLY, 2);
        bussinessSsku.addBaseValue(addParamValue);
        addParamValue = getNumericMdmParamValue(KnownMdmParams.TRANSPORT_UNIT_SIZE, 2);
        bussinessSsku.addBaseValue(addParamValue);
        addParamValue = getNumericMdmParamValue(KnownMdmParams.DELIVERY_TIME, 2);
        bussinessSsku.addBaseValue(addParamValue);
        addParamValue = getNumericMdmParamValue(KnownMdmParams.SUPPLY_SCHEDULE, 2);
        bussinessSsku.addBaseValue(addParamValue);

        CommonSsku bussinessSskuBeforeUpdate = getCommonSsku(BUSINESS_KEY);

        // update service sskus by business supplier id
        String jsonBusinessSsku = JsonMapper.DEFAULT_OBJECT_MAPPER.writeValueAsString(bussinessSsku);

        MockHttpServletRequestBuilder updateRequest = MockMvcRequestBuilders.post("/mdm-api/ui/ssku/update")
            .contentType(MediaType.APPLICATION_JSON_UTF8)
            .content(jsonBusinessSsku);

        MvcResult result = mockMvc.perform(updateRequest)
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andReturn();

        processSskusToRefresh();

        ArrayList<Long> listDifferentServiceParam = new ArrayList<>();
        listDifferentServiceParam.add(KnownMdmParams.QUANTUM_OF_SUPPLY);
        listDifferentServiceParam.add(KnownMdmParams.TRANSPORT_UNIT_SIZE);
        listDifferentServiceParam.add(KnownMdmParams.DELIVERY_TIME);
        listDifferentServiceParam.add(KnownMdmParams.SUPPLY_SCHEDULE);
        listDifferentServiceParam.add(KnownMdmParams.QUANTITY_IN_PACK);
        listDifferentServiceParam.add(KnownMdmParams.MIN_SHIPMENT);
        ArrayList<Long> listBusinessStringParam = new ArrayList<>();
        listBusinessStringParam.add(KnownMdmParams.MANUFACTURER);

        CommonSsku bussinessSskuAfterUpdate = getCommonSsku(BUSINESS_KEY);

        Map<Long, SskuParamValue> businessSskuBaseValuesBeforeUpdate =
            bussinessSskuBeforeUpdate.getBaseValuesByParamId();
        Map<Long, SskuParamValue> businessSskuBaseValuesToUpdate = bussinessSsku.getBaseValuesByParamId();
        Map<Long, SskuParamValue> businessSskuBaseValuesAfterUpdate = bussinessSskuAfterUpdate.getBaseValuesByParamId();

        CommonSsku firstServiceSskuAfterUpdate = getCommonSsku(SUPPLIER_KEY1);
        CommonSsku secondServiceSskuAfterUpdate = getCommonSsku(SUPPLIER_KEY2);
        Map<Long, SskuParamValue> firstServiceSskuBaseValuesBeforeUpdate =
            firstServiceSupplierBeforeUpdate.getBaseValuesByParamId();
        Map<Long, SskuParamValue> secondServiceSskuBaseValuesBeforeUpdate =
            secondServiceSupplierBeforeUpdate.getBaseValuesByParamId();
        Map<Long, SskuParamValue> firstServiceSskuBaseValuesAfterUpdate =
            firstServiceSskuAfterUpdate.getBaseValuesByParamId();
        Map<Long, SskuParamValue> secondServiceSskuBaseValuesAfterUpdate =
            secondServiceSskuAfterUpdate.getBaseValuesByParamId();

        for (long id : listDifferentServiceParam) {
            Assertions.assertThat(businessSskuBaseValuesBeforeUpdate.get(id)).
                isEqualTo(businessSskuBaseValuesAfterUpdate.get(id));
            Assertions.assertThat(firstServiceSskuBaseValuesBeforeUpdate.get(id).getNumeric()).
                isEqualTo(firstServiceSskuBaseValuesAfterUpdate.get(id).getNumeric());
            Assertions.assertThat(secondServiceSskuBaseValuesBeforeUpdate.get(id).getNumeric()).
                isEqualTo(secondServiceSskuBaseValuesAfterUpdate.get(id).getNumeric());
        }
        for (long id : listBusinessStringParam) {
            Assertions.assertThat(businessSskuBaseValuesAfterUpdate.get(id).getString()).
                isEqualTo(businessSskuBaseValuesToUpdate.get(id).getString());
            Assertions.assertThat(firstServiceSskuBaseValuesAfterUpdate.get(id).getString()).
                isEqualTo(businessSskuBaseValuesToUpdate.get(id).getString());
            Assertions.assertThat(secondServiceSskuBaseValuesAfterUpdate.get(id).getString()).
                isEqualTo(businessSskuBaseValuesToUpdate.get(id).getString());
        }
    }


    @Test
    public void whenUpdateSskuShouldFromIrisItemTimestampBeUpdated() throws Exception {
        keyValueService.putValue(MdmProperties.ENABLE_BUSINESS_STATE_UPDATE_IN_MDM_SUPPLIER_CACHE, true);
        keyValueService.putValue(MdmProperties.BUSINESS_MERGE_ENABLED_KEY, true);
        keyValueService.invalidateCache();

        long boxSizeInCm = 30;
        long weightInKg = 10;

        CommonSsku businessSskuForUpdate = getCommonSskuWithWeightDimensionalParams(SUPPLIER_KEY1, weightInKg,
            boxSizeInCm,
            boxSizeInCm, boxSizeInCm);

        // Set timestamp values same as they are sending from UI
        businessSskuForUpdate.getBaseValues().forEach(pv -> {
            pv.getModificationInfo().setUpdatedTs(Instant.now());
            pv.getModificationInfo().setSourceUpdatedTs(null);
        });

        String json = JsonMapper.DEFAULT_OBJECT_MAPPER.writeValueAsString(businessSskuForUpdate);
        MockHttpServletRequestBuilder updateRequest = MockMvcRequestBuilders.post("/mdm-api/ui/ssku/update")
            .contentType(MediaType.APPLICATION_JSON_UTF8)
            .content(json);

        mockMvc.perform(updateRequest)
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andReturn();

        Optional<SilverCommonSsku> silverCommonSsku =
            silverSskuRepository.findSsku(unknownOperatorSilverKey(BUSINESS_KEY));
        Assertions.assertThat(silverCommonSsku)
            .flatMap(ssku -> ssku.getBaseValue(KnownMdmParams.LENGTH))
            .hasValueSatisfying(pv -> Assertions.assertThat(pv.getUpdatedTs()).isAfter(Instant.EPOCH))
            .flatMap(MdmParamValue::getNumeric)
            .map(BigDecimal::longValue)
            .contains(boxSizeInCm);
        Assertions.assertThat(silverCommonSsku)
            .flatMap(ssku -> ssku.getBaseValue(KnownMdmParams.WIDTH))
            .hasValueSatisfying(pv -> Assertions.assertThat(pv.getUpdatedTs()).isAfter(Instant.EPOCH))
            .flatMap(MdmParamValue::getNumeric)
            .map(BigDecimal::longValue)
            .contains(boxSizeInCm);
        Assertions.assertThat(silverCommonSsku)
            .flatMap(ssku -> ssku.getBaseValue(KnownMdmParams.HEIGHT))
            .hasValueSatisfying(pv -> Assertions.assertThat(pv.getUpdatedTs()).isAfter(Instant.EPOCH))
            .flatMap(MdmParamValue::getNumeric)
            .map(BigDecimal::longValue)
            .contains(boxSizeInCm);
        Assertions.assertThat(silverCommonSsku)
            .flatMap(ssku -> ssku.getBaseValue(KnownMdmParams.WEIGHT_GROSS))
            .hasValueSatisfying(pv -> Assertions.assertThat(pv.getUpdatedTs()).isAfter(Instant.EPOCH))
            .flatMap(MdmParamValue::getNumeric)
            .map(BigDecimal::longValue)
            .contains(weightInKg);
    }

    @Test
    public void whenUpdateSskuByServiceSupplierIdShouldReturnOk() throws Exception {
        keyValueService.putValue(MdmProperties.ENABLE_BUSINESS_STATE_UPDATE_IN_MDM_SUPPLIER_CACHE, true);
        keyValueService.putValue(MdmProperties.BUSINESS_MERGE_ENABLED_KEY, true);
        keyValueService.invalidateCache();

        String manufacturer = "Best manufacturer ever";
        boolean traceable = true;
        int quantityInPack = 51;

        // get service sskus before update
        CommonSsku firstServiceSupplierBeforeUpdate = getCommonSsku(SUPPLIER_KEY1);
        MasterData secondSupplierMDBeforeUpdate = masterDataRepository.findByShopSkuKeys(List.of(SUPPLIER_KEY2)).get(0);
        int secondServiceQtyInPack = secondSupplierMDBeforeUpdate.getQuantityInPack();

        // update service sskus by service supplier id
        CommonSsku serviceSsku = getCommonSsku(SUPPLIER_KEY1.getSupplierId(), SUPPLIER_KEY1.getShopSku(),
            manufacturer, traceable, quantityInPack);
        String jsonBusinessSsku = JsonMapper.DEFAULT_OBJECT_MAPPER.writeValueAsString(serviceSsku);
        MockHttpServletRequestBuilder updateRequest = MockMvcRequestBuilders.post("/mdm-api/ui/ssku/update")
            .contentType(MediaType.APPLICATION_JSON_UTF8)
            .content(jsonBusinessSsku);

        mockMvc.perform(updateRequest)
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andReturn();

        System.out.println("Last hope " + mdmSskuGroupManager.findKeyGroupsByKeys(List.of(SUPPLIER_KEY1), false));
        System.out.println("Lst hope "
            + mdmSupplierRepository.findByIds(List.of(SUPPLIER_KEY1.getSupplierId(), BUSINESS_KEY.getSupplierId())));

        Assertions.assertThat(silverSskuRepository.findSsku(unknownOperatorSilverKey(BUSINESS_KEY)))
            .flatMap(ssku -> ssku.getBaseValue(KnownMdmParams.MANUFACTURER))
            .flatMap(SskuSilverParamValue::getString)
            .contains(manufacturer);
        Assertions.assertThat(silverSskuRepository.findSsku(unknownOperatorSilverKey(BUSINESS_KEY)))
            .flatMap(ssku -> ssku.getServiceSsku(SUPPLIER_KEY1.getSupplierId()))
            .flatMap(service -> service.getParamValue(KnownMdmParams.QUANTITY_IN_PACK))
            .flatMap(SskuSilverParamValue::getNumeric)
            .map(BigDecimal::intValue)
            .contains(quantityInPack);
        Assertions.assertThat(silverSskuRepository.findSsku(unknownOperatorSilverKey(BUSINESS_KEY)))
            .flatMap(ssku -> ssku.getServiceSsku(SUPPLIER_KEY2.getSupplierId()))
            .isEmpty();

        processSskusToRefresh();

        CommonSsku firstServiceSskuAfterUpdate = getCommonSsku(SUPPLIER_KEY1);
        CommonSsku secondServiceSskuAfterUpdate = getCommonSsku(SUPPLIER_KEY2);
        Map<Long, SskuParamValue> firstServiceSskuBaseValues = firstServiceSskuAfterUpdate.getBaseValuesByParamId();
        Map<Long, SskuParamValue> secondServiceSskuBaseValues = secondServiceSskuAfterUpdate.getBaseValuesByParamId();

        // check that get() method returns merged record with base param values
        Assertions.assertThat(firstServiceSupplierBeforeUpdate).isNotEqualTo(firstServiceSskuAfterUpdate);

        // check that 2nd supplier was not updated
        MasterData secondSupplierMDAfterUpdate = masterDataRepository.findByShopSkuKeys(List.of(SUPPLIER_KEY2)).get(0);
        Assertions.assertThat(secondSupplierMDBeforeUpdate).isNotEqualTo(secondSupplierMDAfterUpdate);

        Assertions.assertThat(firstServiceSskuBaseValues.get(KnownMdmParams.MANUFACTURER).getString().get())
            .isEqualTo(manufacturer);
        Assertions.assertThat(firstServiceSskuBaseValues.get(KnownMdmParams.IS_TRACEABLE).getBool().get())
            .isEqualTo(true);
        Assertions.assertThat(firstServiceSskuBaseValues.get(KnownMdmParams.QUANTITY_IN_PACK).getNumeric().get())
            .isEqualTo(String.valueOf(quantityInPack));

        Assertions.assertThat(secondServiceSskuBaseValues.get(KnownMdmParams.MANUFACTURER).getString().get())
            .isEqualTo(manufacturer);
        Assertions.assertThat(secondServiceSskuBaseValues.get(KnownMdmParams.IS_TRACEABLE).getBool().get())
            .isEqualTo(true);
        Assertions.assertThat(secondServiceSskuBaseValues.get(KnownMdmParams.QUANTITY_IN_PACK).getNumeric().get())
            .isEqualTo(String.valueOf(secondServiceQtyInPack));

        Assertions.assertThat(BASE_MDM_PARAM_IDS).containsAnyElementsOf(firstServiceSskuBaseValues.keySet());
        Assertions.assertThat(SERVICE_MDM_PARAM_IDS).containsAnyElementsOf(firstServiceSskuBaseValues.keySet());
    }

    @Test
    public void whenUpdateBaseMultivalueThatIsStoredInOtherFlatSskuShouldStillRemoveItRegardless() throws Exception {
        keyValueService.putValue(MdmProperties.ENABLE_BUSINESS_STATE_UPDATE_IN_MDM_SUPPLIER_CACHE, true);
        keyValueService.putValue(MdmProperties.BUSINESS_MERGE_ENABLED_KEY, true);
        keyValueService.invalidateCache();

        String serviceCountry1 = "Уганда";
        String serviceCountry2 = "Эфиопия";
        String businessCountry = "Япония";

        MasterData serviceMD1 = masterDataRepository.findById(SUPPLIER_KEY1);
        MasterData serviceMD2 = masterDataRepository.findById(SUPPLIER_KEY2);
        MasterData businessMD = new MasterData();
        serviceMD1.setManufacturerCountries(List.of(serviceCountry1));
        serviceMD2.setManufacturerCountries(List.of(serviceCountry2));
        businessMD.setManufacturerCountries(List.of(businessCountry));
        businessMD.setShopSkuKey(BUSINESS_KEY);
        masterDataRepository.insertOrUpdateAll(List.of(serviceMD1, serviceMD2, businessMD));

        CommonSsku serviceCommonSsku1 = getCommonSsku(SUPPLIER_KEY1);
        // Удалим страны, лежащие в соседнем сервисе и в базе. Добавим ещё одну.
        serviceCommonSsku1.getBaseValuesByParamId().get(KnownMdmParams.MANUFACTURER_COUNTRY).setStrings(List.of(
            "Китай",
            "Уганда", // старая
            "Россия"
        ));

        // Отправляем на сохранение. Ожидаем, что список стран безотносительно чего-либо вообще обновится во всём
        // бизнес-оффере на новый набор.
        String jsonBusinessSsku = JsonMapper.DEFAULT_OBJECT_MAPPER.writeValueAsString(serviceCommonSsku1);
        MockHttpServletRequestBuilder updateRequest = MockMvcRequestBuilders.post("/mdm-api/ui/ssku/update")
            .contentType(MediaType.APPLICATION_JSON_UTF8)
            .content(jsonBusinessSsku);

        mockMvc.perform(updateRequest)
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andReturn();

        Assertions.assertThat(silverSskuRepository.findSsku(unknownOperatorSilverKey(BUSINESS_KEY)))
            .flatMap(ssku -> ssku.getBaseValue(KnownMdmParams.MANUFACTURER_COUNTRY))
            .map(MdmParamValue::getStrings)
            .hasValueSatisfying(countries -> Assertions.assertThat(countries).containsExactlyInAnyOrder(
                "Китай", "Уганда", "Россия"
            ));

        processSskusToRefresh();

        CommonSsku serviceSskuAfterUpdate1 = getCommonSsku(SUPPLIER_KEY1);
        CommonSsku serviceSskuAfterUpdate2 = getCommonSsku(SUPPLIER_KEY2);
        CommonSsku businessSskuAfterUpdate = getCommonSsku(BUSINESS_KEY);
        Map<Long, SskuParamValue> serviceValues1 = serviceSskuAfterUpdate1.getBaseValuesByParamId();
        Map<Long, SskuParamValue> serviceValues2 = serviceSskuAfterUpdate2.getBaseValuesByParamId();
        Map<Long, SskuParamValue> businessValues = businessSskuAfterUpdate.getBaseValuesByParamId();

        //TODO: Когда будем делать из master_data полноценное золото, разобраться почему так.
        // Скорее всего в рамках проекта MARKETMDM-89.
        Assertions.assertThat(serviceValues1.get(KnownMdmParams.MANUFACTURER_COUNTRY).getStrings())
            .containsExactlyInAnyOrder("Китай", "Уганда", "Россия", "Япония");
        Assertions.assertThat(serviceValues2.get(KnownMdmParams.MANUFACTURER_COUNTRY).getStrings())
            .containsExactlyInAnyOrder("Китай", "Уганда", "Россия", "Япония");
        Assertions.assertThat(businessValues.get(KnownMdmParams.MANUFACTURER_COUNTRY).getStrings())
            .containsExactlyInAnyOrder("Китай", "Уганда", "Россия", "Япония");
    }

    @Test
    public void whenUpdateSskuByServiceSupplierIdShouldRecalculateVerdicts() throws Exception {
        keyValueService.putValue(MdmProperties.ENABLE_BUSINESS_STATE_UPDATE_IN_MDM_SUPPLIER_CACHE, true);
        keyValueService.putValue(MdmProperties.BUSINESS_MERGE_ENABLED_KEY, true);
        keyValueService.invalidateCache();

        String manufacturer = "Uniqlo";
        boolean traceable = true;
        int quantityInPack = 51;

        // update service sskus by service supplier id
        CommonSsku serviceSsku = getCommonSsku(SUPPLIER_KEY1.getSupplierId(), SUPPLIER_KEY1.getShopSku(),
            manufacturer, traceable, quantityInPack);
        String jsonBusinessSsku = JsonMapper.DEFAULT_OBJECT_MAPPER.writeValueAsString(serviceSsku);
        MockHttpServletRequestBuilder updateRequest = MockMvcRequestBuilders.post("/mdm-api/ui/ssku/update")
            .contentType(MediaType.APPLICATION_JSON_UTF8)
            .content(jsonBusinessSsku);

        mockMvc.perform(updateRequest)
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andReturn();

        processSskusToRefresh();

        // check that verdicts were calculated
        Map<ShopSkuKey, SskuVerdictResult> goldenVerdictsMap = sskuGoldenVerdictRepository
            .findByIds(List.of(SUPPLIER_KEY1, BUSINESS_KEY, SUPPLIER_KEY2))
            .stream()
            .collect(Collectors.toMap(goldenVerdict ->
                new ShopSkuKey(goldenVerdict.getSupplierId(), goldenVerdict.getShopSku()), Function.identity()));

        Assertions.assertThat(goldenVerdictsMap.size()).isEqualTo(3);
        Assertions.assertThat(goldenVerdictsMap.keySet()).contains(SUPPLIER_KEY1, BUSINESS_KEY, SUPPLIER_KEY2);
        Assertions.assertThat(goldenVerdictsMap.get(BUSINESS_KEY).isValid()).isEqualTo(true);
        Assertions.assertThat(goldenVerdictsMap.get(SUPPLIER_KEY1).isValid()).isEqualTo(true);
        Assertions.assertThat(goldenVerdictsMap.get(SUPPLIER_KEY2).isValid()).isEqualTo(true);

        Map<VerdictFeature, SingleVerdictResult> singleVerdictResults =
            goldenVerdictsMap.get(SUPPLIER_KEY1).getSingleVerdictResults();
        Assertions.assertThat(singleVerdictResults.keySet()).containsExactlyInAnyOrder(VerdictFeature.UNSPECIFIED);

        Assertions.assertThat(singleVerdictResults.get(VerdictFeature.UNSPECIFIED))
            .isEqualTo(VerdictGeneratorHelper.createOkVerdict(VerdictFeature.UNSPECIFIED));
    }

    @Test
    public void whenUpdateSskuByBusinessWithNoSuppliersShouldReturnError() throws Exception {
        keyValueService.putValue(MdmProperties.ENABLE_BUSINESS_STATE_UPDATE_IN_MDM_SUPPLIER_CACHE, true);
        keyValueService.putValue(MdmProperties.BUSINESS_MERGE_ENABLED_KEY, true);
        keyValueService.invalidateCache();

        String manufacturer = "Gold manufacturer";
        boolean traceable = true;
        int quantityInPack = 51;

        // when business has no suppliers at all, update() should change nothing
        CommonSsku bussinessSsku = getCommonSsku(ORPHAN_BUSINESS_KEY.getSupplierId(), ORPHAN_BUSINESS_KEY.getShopSku(),
            manufacturer, traceable, quantityInPack);
        String jsonBusinessSsku = JsonMapper.DEFAULT_OBJECT_MAPPER.writeValueAsString(bussinessSsku);
        MockHttpServletRequestBuilder updateRequest = MockMvcRequestBuilders.post("/mdm-api/ui/ssku/update")
            .contentType(MediaType.APPLICATION_JSON_UTF8)
            .content(jsonBusinessSsku);

        mockMvc.perform(updateRequest)
            .andExpect(MockMvcResultMatchers.status().isBadRequest())
            .andExpect(MockMvcResultMatchers.xpath("/List/item[1]").string("SSKU не найдена"))
            .andReturn();
    }

    @Test
    public void whenUpdateSskuByBusinessWithOnlyDisabledSuppliersShouldReturnError() throws Exception {
        keyValueService.putValue(MdmProperties.ENABLE_BUSINESS_STATE_UPDATE_IN_MDM_SUPPLIER_CACHE, true);
        keyValueService.putValue(MdmProperties.BUSINESS_MERGE_ENABLED_KEY, true);
        keyValueService.invalidateCache();

        String manufacturer = "The only manufacturer";
        boolean traceable = true;
        int quantityInPack = 51;

        // in case business supplier id is provided, update() should work only for enabled suppliers
        CommonSsku bussinessSsku = getCommonSsku(BUSINESS_WITH_DISABLED_SUPPLIERS_KEY.getSupplierId(),
            DISABLED_SUPPLIER_KEY4.getShopSku(), manufacturer, traceable, quantityInPack);
        String jsonBusinessSsku = JsonMapper.DEFAULT_OBJECT_MAPPER.writeValueAsString(bussinessSsku);
        MockHttpServletRequestBuilder updateRequest = MockMvcRequestBuilders.post("/mdm-api/ui/ssku/update")
            .contentType(MediaType.APPLICATION_JSON_UTF8)
            .content(jsonBusinessSsku);

        mockMvc.perform(updateRequest)
            .andExpect(MockMvcResultMatchers.status().isBadRequest())
            .andExpect(MockMvcResultMatchers.xpath("/List/item[1]").string("SSKU не найдена"))
            .andReturn();
    }

    @Test
    public void whenUpdateSskuByBusinessShouldReturnOk() throws Exception {
        keyValueService.putValue(MdmProperties.ENABLE_BUSINESS_STATE_UPDATE_IN_MDM_SUPPLIER_CACHE, true);
        keyValueService.putValue(MdmProperties.BUSINESS_MERGE_ENABLED_KEY, true);
        keyValueService.invalidateCache();

        String manufacturer = "The only manufacturer";
        boolean traceable = true;
        int quantityInPack = 51;

        CommonSsku bussinessSsku = getCommonSsku(BUSINESS_KEY.getSupplierId(), BUSINESS_KEY.getShopSku(),
            manufacturer, traceable, quantityInPack);
        String jsonBusinessSsku = JsonMapper.DEFAULT_OBJECT_MAPPER.writeValueAsString(bussinessSsku);
        MockHttpServletRequestBuilder updateRequest = MockMvcRequestBuilders.post("/mdm-api/ui/ssku/update")
            .contentType(MediaType.APPLICATION_JSON_UTF8)
            .content(jsonBusinessSsku);

        mockMvc.perform(updateRequest)
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andReturn();
    }

    @Test
    public void whenGetMetadataShouldReturnLiquibaseImportResults() throws Exception {
        MockHttpServletRequestBuilder getRequest = MockMvcRequestBuilders.get(
            "/mdm-api/ui/ssku/metadata?type={type}", MdmParamIoType.SSKU_TABLE_VIEW.name()
        ).accept(MediaType.APPLICATION_JSON_UTF8);

        mockMvc.perform(getRequest)
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON_UTF8))
            .andExpect(MockMvcResultMatchers.jsonPath("$[0].id").value(116))
            .andExpect(MockMvcResultMatchers.jsonPath("$[0].title").value("ИД категории"))
            .andExpect(MockMvcResultMatchers.jsonPath("$[1].id").value(400))
            .andExpect(MockMvcResultMatchers.jsonPath("$[1].xslName").value("mdm_length"))
            .andExpect(MockMvcResultMatchers.jsonPath("$[2].id").value(401))
            .andExpect(MockMvcResultMatchers.jsonPath("$[2].xslName").value("mdm_width"))
            .andExpect(MockMvcResultMatchers.jsonPath("$[3].id").value(402))
            .andExpect(MockMvcResultMatchers.jsonPath("$[3].valueType").value("NUMERIC"));
    }

    @Test
    public void whenExportExcelByFileShouldReturnOKResponse() throws Exception {
        ExcelFile.Builder excel = new ExcelFile.Builder();
        excel.addHeaders(List.of(MdmParamExcelAttributes.SUPPLIER_HEADER,
            MdmParamExcelAttributes.SHOP_SKU_HEADER));
        excel.addLine(List.of(String.valueOf(KEY_3P.getSupplierId()), KEY_3P.getShopSku()));

        MockMultipartFile multipartFile = new MockMultipartFile("input-excel",
            ExcelFileConverter.convertToBytes(excel.build()));

        MockHttpServletRequestBuilder postRequest = MockMvcRequestBuilders
            .multipart("/mdm-api/ui/ssku/export-to-excel-by-file")
            .file("files", multipartFile.getBytes());

        mockMvc.perform(postRequest)
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andReturn();
    }

    @Test
    public void testExportExcelByFileShouldReturnErrorWhenInvalidInputExcelGiven() throws Exception {
        ExcelFile.Builder excel = new ExcelFile.Builder();
        excel.addHeaders(List.of(MdmParamExcelAttributes.SUPPLIER_HEADER,
            MdmParamExcelAttributes.SHOP_SKU_HEADER));
        excel.addLine(List.of("abc", KEY_3P.getShopSku())); //supplierId должен быть int

        MockMultipartFile multipartFile = new MockMultipartFile("input-excel",
            ExcelFileConverter.convertToBytes(excel.build()));

        MockHttpServletRequestBuilder postRequest = MockMvcRequestBuilders
            .multipart("/mdm-api/ui/ssku/export-to-excel-by-file")
            .file("files", multipartFile.getBytes());

        MvcResult result = mockMvc.perform(postRequest)
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andReturn();
        Assertions.assertThat(result.getResponse().getContentAsString()).contains("Некорректное значение SSKU");
    }

    @Test
    public void testGenerateExportByFileTemplateShouldSuccessfullyGenerateEmptyFileWithHeader() throws Exception {
        MockHttpServletRequestBuilder getRequest = MockMvcRequestBuilders.get("/mdm-api/ui/ssku/export-by-file" +
            "-template");

        mockMvc.perform(getRequest)
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.header().string("Content-Disposition",
                "attachment; filename*=UTF-8''shop-sku-template.xlsx"))
            .andReturn();
    }

    @Test
    public void testGenerateExportByFileTemplateAndExportByFileWorkTogether() throws Exception {
        //generate template and add data to it
        ExcelFile.Builder template = shopSkuKeyExcelService.generateEmptyExcelWithShopSkuHeader().toBuilder();
        template.addLine(List.of(String.valueOf(KEY_3P.getSupplierId()), KEY_3P.getShopSku()));

        MockMultipartFile multipartFile = new MockMultipartFile("input-excel",
            ExcelFileConverter.convertToBytes(template.build()));

        MockHttpServletRequestBuilder postRequest = MockMvcRequestBuilders
            .multipart("/mdm-api/ui/ssku/export-to-excel-by-file")
            .file("files", multipartFile.getBytes());

        mockMvc.perform(postRequest)
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andReturn();
    }

    @Test
    public void testFindSampleData() throws Exception {
        keyValueService.putValue(MdmProperties.SSKU_SAMPLE_TABLE_PERCENTAGE_KEY, "100");
        keyValueService.invalidateCache();
        MockHttpServletRequestBuilder getRequest = MockMvcRequestBuilders.get(
            "/mdm-api/ui/ssku/find-sample").accept(MediaType.APPLICATION_JSON_UTF8);

        MappingCacheDao mapping1 = new MappingCacheDao()
            .setSupplierId(KEY_3P.getSupplierId())
            .setShopSku(KEY_3P.getShopSku())
            .setMskuId(123L)
            .setCategoryId(456);
        mappingsCacheRepository.insert(mapping1);

        MvcResult result = mockMvc.perform(getRequest)
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON_UTF8))
            .andReturn();

        String dataPageString = result.getResponse().getContentAsString();
        DataPage<CommonSsku> dataPage = JsonMapper.DEFAULT_OBJECT_MAPPER.readValue(
            dataPageString, new TypeReference<DataPage<CommonSsku>>() {
            });
        Assertions.assertThat(dataPage.getTotalCount()).isEqualTo(1);
        Assertions.assertThat(dataPage.getItems().get(0).getKey()).isEqualTo(KEY_3P);
    }


    private List<MdmSupplier> getSuppliers() {
        MdmSupplier businessMan = supplier(BUSINESS_KEY.getSupplierId(), MdmSupplierType.BUSINESS,
            "Business Man", null, false);

        MdmSupplier firstSupplierMan = supplier(SUPPLIER_KEY1.getSupplierId(), MdmSupplierType.FIRST_PARTY,
            "First supplier Man", BUSINESS_KEY.getSupplierId(), true);

        MdmSupplier secondSupplierMan = supplier(SUPPLIER_KEY2.getSupplierId(), MdmSupplierType.THIRD_PARTY,
            "Second supplier Man", BUSINESS_KEY.getSupplierId(), true);

        //disabled supplier
        MdmSupplier thirdSupplierMan = supplier(DISABLED_SUPPLIER_KEY3.getSupplierId(), MdmSupplierType.THIRD_PARTY,
            "Third supplier Man", BUSINESS_KEY.getSupplierId(), false);

        MdmSupplier lonelyOne = supplier(ORPHAN_KEY.getSupplierId(), MdmSupplierType.THIRD_PARTY,
            "Lonely Man", null, false);

        //business without suppliers
        MdmSupplier businessManWithoutSuppliers = supplier(ORPHAN_BUSINESS_KEY.getSupplierId(),
            MdmSupplierType.BUSINESS, "Business Man without businesses", null, false);

        //business with only disabled suppliers
        MdmSupplier businessManWithDisabledSuppliers = supplier(BUSINESS_WITH_DISABLED_SUPPLIERS_KEY.getSupplierId(),
            MdmSupplierType.BUSINESS, "Business Man with disabled businesses", null, false);

        //disabled suppliers
        MdmSupplier fourthSupplierMan = supplier(DISABLED_SUPPLIER_KEY4.getSupplierId(), MdmSupplierType.THIRD_PARTY,
            "Fourth supplier Man", businessManWithDisabledSuppliers.getId(), false);

        MdmSupplier fifthSupplierMan = supplier(DISABLED_SUPPLIER_KEY5.getSupplierId(), MdmSupplierType.THIRD_PARTY,
            "Fifth supplier Man", businessManWithDisabledSuppliers.getId(), false);

        //3p supplier
        MdmSupplier supplier3P = supplier(KEY_3P.getSupplierId(), MdmSupplierType.THIRD_PARTY,
            "Fifth supplier Man", null, false);

        //white suppliers
        MdmSupplier whiteSupplier = supplier(WHITE_SUPPLIER_KEY.getSupplierId(), MdmSupplierType.MARKET_SHOP,
            "Fifth supplier Man", BUSINESS_KEY.getSupplierId(), true);

        MdmSupplier whiteLonelySupplier = supplier(WHITE_ORPHAN_SUPPLIER_KEY.getSupplierId(),
            MdmSupplierType.MARKET_SHOP, "Fifth supplier Man", null, false);

        return List.of(businessMan, firstSupplierMan, secondSupplierMan, thirdSupplierMan, lonelyOne,
            businessManWithoutSuppliers, businessManWithDisabledSuppliers, fourthSupplierMan, fifthSupplierMan,
            supplier3P, whiteSupplier, whiteLonelySupplier);
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

    private void loadDataToRepos() {
        MasterData masterData3P = TestDataUtils.generateMasterData(KEY_3P, random);
        MasterData masterData1 = TestDataUtils.generateMasterData(SUPPLIER_KEY1, random);
        MasterData masterData2 = TestDataUtils.generateMasterData(SUPPLIER_KEY2, random);
        MasterData masterData3 = TestDataUtils.generateMasterData(DISABLED_SUPPLIER_KEY3, random);
        MasterData masterData4 = TestDataUtils.generateMasterData(ORPHAN_KEY, random);
        MasterData masterData5 = TestDataUtils.generateMasterData(DISABLED_SUPPLIER_KEY4, random);
        MasterData masterData6 = TestDataUtils.generateMasterData(DISABLED_SUPPLIER_KEY5, random);
        MasterData masterData7 = TestDataUtils.generateMasterData(WHITE_SUPPLIER_KEY, random);
        MasterData masterData8 = TestDataUtils.generateMasterData(WHITE_ORPHAN_SUPPLIER_KEY, random);
        MasterData masterData9 = TestDataUtils.generateMasterData(UNKNOWN_SUPPLIER_KEY, random);

        masterData1.setManufacturerCountries(List.of("Китай"));
        masterData1.setItemShippingUnit(
            ItemWrapperTestUtil.generateShippingUnit(5.0, 5.0, 5.0, 1.0, null, null).build());

        masterDataRepository.insertBatch(masterData3P, masterData1, masterData2, masterData3, masterData4, masterData5,
            masterData6, masterData7, masterData8, masterData9);

        referenceItemRepository.insert(ItemWrapperTestUtil.createReferenceNetWeightItem(
            KEY_3P.getSupplierId(), KEY_3P.getShopSku(), 42));

        mdmSupplierRepository.insertBatch(getSuppliers());
        mdmSupplierCachingService.refresh();
    }

    private CommonSsku getCommonSsku(ShopSkuKey key) throws Exception {
        MockHttpServletRequestBuilder getRequest = MockMvcRequestBuilders.get(
            "/mdm-api/ui/ssku/{supplierId}/{shopSku}", key.getSupplierId(), key.getShopSku()
        ).accept(MediaType.APPLICATION_JSON_UTF8);
        MvcResult result = mockMvc.perform(getRequest)
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON_UTF8))
            .andExpect(MockMvcResultMatchers.jsonPath("$.shopSkuKey.supplierId").value(key.getSupplierId()))
            .andExpect(MockMvcResultMatchers.jsonPath("$.shopSkuKey.shopSku").value(key.getShopSku()))
            .andReturn();

        String jsonSsku = result.getResponse().getContentAsString();
        CommonSsku ssku = JsonMapper.DEFAULT_OBJECT_MAPPER.readValue(jsonSsku, CommonSsku.class);
        Assertions.assertThat(ssku.getKey()).isEqualTo(key);

        return ssku;
    }

    private CommonSsku getCommonSsku(int businessId, String shopSku,
                                     String manufacturer, boolean traceable, int quantityInPack) {
        CommonSsku commonSsku = new CommonSsku(new ShopSkuKey(businessId, shopSku));
        Stream.of(
                getStringMdmParamValue(KnownMdmParams.MANUFACTURER, manufacturer),
                getBoolMdmParamValue(KnownMdmParams.IS_TRACEABLE, traceable),
                getNumericMdmParamValue(KnownMdmParams.QUANTITY_IN_PACK, quantityInPack),
                getNumericMdmParamValue(KnownMdmParams.TRANSPORT_UNIT_SIZE, 1))
            .forEach(commonSsku::addBaseValue);
        return commonSsku;
    }

    private CommonSsku getCommonSskuWithWeightDimensionalParams(ShopSkuKey shopSkuKey, long weight,
                                                                long length, long height, long width) {
        CommonSsku businessSsku = new CommonSsku(shopSkuKey);
        var weightParam = getNumericMdmParamValue(KnownMdmParams.WEIGHT_GROSS, weight);
        var lengthParam = getNumericMdmParamValue(KnownMdmParams.LENGTH, length);
        var heightParam = getNumericMdmParamValue(KnownMdmParams.HEIGHT, height);
        var widthParam = getNumericMdmParamValue(KnownMdmParams.WIDTH, width);
        businessSsku.setBaseValues(List.of(weightParam, lengthParam, heightParam, widthParam));
        return businessSsku;
    }

    private MdmParamValue getStringMdmParamValue(long mdmParamId, String value) {
        return new MdmParamValue()
            .setString(value)
            .setMdmParamId(mdmParamId)
            .setUpdatedTs(Instant.now());
    }

    private MdmParamValue getBoolMdmParamValue(long mdmParamId, boolean value) {
        return new MdmParamValue()
            .setBool(value)
            .setMdmParamId(mdmParamId)
            .setUpdatedTs(Instant.now());
    }

    private MdmParamValue getNumericMdmParamValue(long mdmParamId, double value) {
        return new MdmParamValue()
            .setNumeric(BigDecimal.valueOf(value))
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

    private static final Set<Long> BASE_MDM_PARAM_IDS = Set.of(
        KnownMdmParams.CUSTOMS_COMM_CODE_MDM_ID,
        KnownMdmParams.SHELF_LIFE,
        KnownMdmParams.SHELF_LIFE_UNIT,
        KnownMdmParams.SHELF_LIFE_COMMENT,
        KnownMdmParams.LIFE_TIME,
        KnownMdmParams.LIFE_TIME_UNIT,
        KnownMdmParams.LIFE_TIME_COMMENT,
        KnownMdmParams.GUARANTEE_PERIOD,
        KnownMdmParams.GUARANTEE_PERIOD_UNIT,
        KnownMdmParams.GUARANTEE_PERIOD_COMMENT,
        KnownMdmParams.BOX_COUNT,
        KnownMdmParams.MANUFACTURER,
        KnownMdmParams.QUANTITY_IN_PACK,
        KnownMdmParams.USE_IN_MERCURY,
        KnownMdmParams.LENGTH,
        KnownMdmParams.WIDTH,
        KnownMdmParams.HEIGHT,
        KnownMdmParams.WEIGHT_GROSS,
        KnownMdmParams.WEIGHT_NET,
        KnownMdmParams.WEIGHT_TARE,
        KnownMdmParams.MANUFACTURER_COUNTRY,
        KnownMdmParams.DOCUMENT_REG_NUMBER,
        KnownMdmParams.GTIN,
        KnownMdmParams.VETIS_GUID,
        KnownMdmParams.SSKU_LENGTH,
        KnownMdmParams.SSKU_HEIGHT,
        KnownMdmParams.SSKU_WIDTH,
        KnownMdmParams.SSKU_WEIGHT_GROSS,
        KnownMdmParams.SSKU_WEIGHT_NET,
        KnownMdmParams.SSKU_WEIGHT_TARE,
        KnownMdmParams.BUSINESS_ID,
        KnownMdmParams.IS_TRACEABLE
    );

    private static final Set<Long> SERVICE_MDM_PARAM_IDS = Set.of(
        KnownMdmParams.DANGEROUS_GOOD, // теперь это карготип, а не мдм-параметр
        KnownMdmParams.VAT,
        KnownMdmParams.MIN_SHIPMENT,
        KnownMdmParams.QUANTUM_OF_SUPPLY,
        KnownMdmParams.TRANSPORT_UNIT_SIZE,
        KnownMdmParams.DELIVERY_TIME,
        KnownMdmParams.SUPPLY_SCHEDULE,
        KnownMdmParams.RSL_IN_DAYS,
        KnownMdmParams.RSL_OUT_DAYS,
        KnownMdmParams.RSL_IN_PERCENTS,
        KnownMdmParams.RSL_OUT_PERCENTS,
        KnownMdmParams.RSL_ACTIVATION_TS
    );

    private void initGoldComputer() {
        CachedItemBlockValidationContextProvider validationContextProvider =
            new CachedItemBlockValidationContextProviderImpl(keyValueService);
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
                keyValueService,
                serviceSskuConverter,
                masterDataBusinessMergeService,
                Mockito.mock(OfferCutoffService.class),
                sskuGoldenParamUtil,
                validationContextProvider,
                weightDimensionsValidator,
                rslGoldenItemService
            );

        SskuGoldenMasterDataCalculationHelper goldenMasterDataCalculator =
            new SskuGoldenMasterDataCalculationHelper(
                serviceSskuConverter,
                new MasterDataValidationService(new AllOkMasterDataValidator()),
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
            sskuProcessingPostProcessor(goldenReferenceItemCalculator),
            sskuProcessingContextProvider(),
            sskuProcessingPipeProcessor(),
            sskuProcessingPreProcessor(),
            sskuCalculatingProcessor(goldenReferenceItemCalculator, goldenMasterDataCalculator));


        sskuToRefreshQueueService = new ProcessSskuToRefreshQueueService(
            sskuToRefreshRepository,
            keyValueService,
            mdmSskuGroupManager,
            sskuToRefreshProcessingService,
            transactionTemplate
        );
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


    private void commitTransactionAndStartNew() {
        TestTransaction.flagForCommit();
        TestTransaction.end();
        TestTransaction.start();
    }

    private void processSskusToRefresh() {
        commitTransactionAndStartNew();
        sskuToRefreshQueueService.processQueueItems();
    }

    private static SilverSskuKey unknownOperatorSilverKey(ShopSkuKey shopSkuKey) {
        return new SilverSskuKey(
            shopSkuKey,
            new MasterDataSource(MasterDataSourceType.MDM_OPERATOR, MasterDataSourceType.UNKNOWN_MDM_OPERATOR_SOURCE_ID)
        );
    }
}
