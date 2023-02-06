package ru.yandex.market.mdm.app.controller;

import java.util.List;

import com.fasterxml.jackson.databind.MapperFeature;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import ru.yandex.market.mbo.lightmapper.JsonMapper;
import ru.yandex.market.mbo.mdm.common.masterdata.model.MappingCacheDao;
import ru.yandex.market.mbo.mdm.common.masterdata.model.MdmDoctorQueueStatistic;
import ru.yandex.market.mbo.mdm.common.masterdata.model.MdmDoctorSilversAndMappings;
import ru.yandex.market.mbo.mdm.common.masterdata.model.MdmDoctorUnitedResponse;
import ru.yandex.market.mbo.mdm.common.masterdata.model.MdmDoctorVerdicts;
import ru.yandex.market.mbo.mdm.common.masterdata.model.msku.CommonMsku;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.CategoryParamValue;
import ru.yandex.market.mbo.mdm.common.masterdata.model.queue.MdmMskuQueueInfo;
import ru.yandex.market.mbo.mdm.common.masterdata.model.queue.SskuToRefreshInfo;
import ru.yandex.market.mbo.mdm.common.masterdata.model.ssku.CommonSsku;
import ru.yandex.market.mbo.mdm.common.masterdata.model.ssku.MdmSskuKeyGroup;
import ru.yandex.market.mbo.mdm.common.masterdata.model.verdict.SskuPartnerVerdictResult;
import ru.yandex.market.mbo.mdm.common.masterdata.model.verdict.SskuVerdictResult;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.MappingsCacheRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.param.CategoryParamValueRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.param.MskuRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.param.SilverSskuRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.queue.LbFailedOfferQueueRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.queue.MskuToMboQueueRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.queue.MskuToRefreshRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.queue.SendReferenceItemQRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.queue.SendToDatacampQRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.queue.SendToErpQueueRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.queue.SskuToRefreshRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.verdict.SskuGoldenVerdictRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.verdict.SskuPartnerVerdictRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.services.business.MdmSskuGroupManagerMock;
import ru.yandex.market.mbo.mdm.common.masterdata.services.ssku.MdmCommonSskuServiceMock;
import ru.yandex.market.mbo.mdm.common.utils.MdmBaseDbTestClass;
import ru.yandex.market.mboc.common.offers.model.ShopSkuKey;
import ru.yandex.market.mdm.app.services.MdmDoctorGoldService;
import ru.yandex.market.mdm.app.services.MdmDoctorLinkService;
import ru.yandex.market.mdm.app.services.MdmDoctorQueueStatisticService;
import ru.yandex.market.mdm.app.services.MdmDoctorService;
import ru.yandex.market.mdm.app.services.MdmDoctorSilverAndMappingService;
import ru.yandex.market.mdm.app.services.MdmDoctorVerdictService;

public class MdmDoctorControllerTest extends MdmBaseDbTestClass {

    private static final ShopSkuKey SHOP_SKU_KEY = new ShopSkuKey(10781723, "LUZARLWP0514");
    private static final Integer SHOP_ID = 130;
    private static final ShopSkuKey UNKNOWN_SUPPLIER_KEY = new ShopSkuKey(111, "supplier");
    private static final int NUMBER_OF_QUEUES = 7;
    private static final Integer CATEGORY_ID = 100500;
    private static final Long MSKU_ID = 500100L;
    private static final String LOGFELLER_PATH = "home/logfeller/logs/market-mbo-card-render-test";
    private static final String MDM_UI_URL = "https://mdm.market.yandex-team.ru";
    private static final String MDM_OLD_UI_URL = "https://mbo-mdm.vs.market.yandex.net";
    private static final String MSKU_DATACAMP_TABLE_PATH = "home/market/production/indexer/datacamp/msku/msku";

    @Autowired
    private SendReferenceItemQRepository sendReferenceItemQRepository;
    @Autowired
    private SskuToRefreshRepository sskuToRefreshRepository;
    @Autowired
    private SendToDatacampQRepository sendToDatacampQRepository;
    @Autowired
    private LbFailedOfferQueueRepository lbFailedOfferQueueRepository;
    @Autowired
    private SendToErpQueueRepository sendToErpQueueRepository;
    @Autowired
    private SilverSskuRepository silverSskuRepository;
    @Autowired
    private MappingsCacheRepository mappingsCacheRepository;
    @Autowired
    private MskuRepository mskuRepository;
    @Autowired
    private CategoryParamValueRepository categoryParamValueRepository;
    @Autowired
    private SskuGoldenVerdictRepository sskuGoldenVerdictRepository;
    @Autowired
    private SskuPartnerVerdictRepository sskuPartnerVerdictRepository;
    @Autowired
    private MskuToMboQueueRepository mskuToMboQueueRepository;
    @Autowired
    MskuToRefreshRepository mskuToRefreshRepository;

    private MdmCommonSskuServiceMock mdmCommonSskuService;

    private MockMvc mockMvc;

    private MdmDoctorController mdmDoctorController;

    private MdmSskuGroupManagerMock mdmSskuGroupManager;

    @Before
    public void setUp() {
        mdmSskuGroupManager = new MdmSskuGroupManagerMock();
        mdmCommonSskuService = new MdmCommonSskuServiceMock();
        var mdmDoctorService = new MdmDoctorService(mdmSskuGroupManager);
        this.mdmDoctorController = new MdmDoctorController(
            new MdmDoctorQueueStatisticService(
                sendReferenceItemQRepository,
                sskuToRefreshRepository,
                sendToDatacampQRepository,
                lbFailedOfferQueueRepository,
                sendToErpQueueRepository,
                mskuToMboQueueRepository,
                mskuToRefreshRepository,
                mappingsCacheRepository,
                mdmDoctorService),
            new MdmDoctorSilverAndMappingService(
                silverSskuRepository,
                mappingsCacheRepository,
                mskuRepository,
                categoryParamValueRepository,
                mdmDoctorService
            ),
            new MdmDoctorVerdictService(
                sskuGoldenVerdictRepository,
                sskuPartnerVerdictRepository,
                mdmDoctorService
            ),
            new MdmDoctorGoldService(
                mdmCommonSskuService,
                mdmDoctorService),
            new MdmDoctorLinkService(mappingsCacheRepository, mdmDoctorService,
                LOGFELLER_PATH, LOGFELLER_PATH, LOGFELLER_PATH, LOGFELLER_PATH, MDM_UI_URL, MDM_OLD_UI_URL,
                LOGFELLER_PATH, MSKU_DATACAMP_TABLE_PATH));
        mockMvc = MockMvcBuilders.standaloneSetup(mdmDoctorController).build();
        loadGroupManagerDataToRepos();
        loadQueueStatisticDataToRepos();
        loadSilversAndMappingsDataToRepos();
        loadVerdictsDataToRepos();
        loadGoldSskuDataToRepos();
    }

    @Test
    public void whenGetByKeyShouldReturnOk() throws Exception {
        MockHttpServletRequestBuilder getRequest = MockMvcRequestBuilders.get(
            "/mdm-api/doctor/ssku/offer-info/offer?businessId={businessId}&shopSku={shopSku}",
            SHOP_SKU_KEY.getSupplierId(), SHOP_SKU_KEY.getShopSku()
        ).accept(MediaType.APPLICATION_JSON_UTF8);
        mockMvc.perform(getRequest)
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON_UTF8))
            .andReturn();
    }

    @Test
    public void whenGetByKeyWithShopIdShouldReturnOk() throws Exception {
        MockHttpServletRequestBuilder getRequest = MockMvcRequestBuilders.get(
            "/mdm-api/doctor/ssku/offer-info/offer?businessId={businessId}&shopSku={shopSku}" +
                "&shopId={shopId}",
            SHOP_SKU_KEY.getSupplierId(), SHOP_SKU_KEY.getShopSku(), SHOP_ID
        ).accept(MediaType.APPLICATION_JSON_UTF8);
        mockMvc.perform(getRequest)
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON_UTF8))
            .andReturn();
    }

    @Test
    public void whenGetByUnknownKeyShouldReturnNotFound() throws Exception {
        MockHttpServletRequestBuilder getRequest = MockMvcRequestBuilders.get(
            "/mdm-api/doctor/ssku/offer-info/offer?businessId={businessId}&shopSku={shopSku}",
            UNKNOWN_SUPPLIER_KEY.getSupplierId(), UNKNOWN_SUPPLIER_KEY.getShopSku()
        ).accept(MediaType.APPLICATION_JSON_UTF8);
        mockMvc.perform(getRequest)
            .andExpect(MockMvcResultMatchers.status().isNotFound());
    }

    @Test
    public void checkCorrectNumberOfQueues() throws Exception {
        Assertions.assertThat(getMdmDoctorUnitedResponse().getMdmDoctorQueueStatisticList().size())
            .isEqualTo(NUMBER_OF_QUEUES);
    }

    @Test
    public void checkCorrectAddedTimestamp() throws Exception {
        Assertions.assertThat(isAddedTimestampPresent()).isTrue();
    }

    @Test
    public void checkSilversAndMappingsFound() throws Exception {
        MdmDoctorSilversAndMappings mdmDoctorSilversAndMappings = getMdmDoctorUnitedResponse()
            .getMdmDoctorSilversAndMappings();
        Assertions.assertThat(!mdmDoctorSilversAndMappings.getCategoryParamValueMap().isEmpty()).isTrue();
    }

    @Test
    public void checkVerdictsFound() throws Exception {
        MdmDoctorVerdicts mdmDoctorVerdicts = getMdmDoctorUnitedResponse()
            .getMdmDoctorVerdicts();
        Assertions.assertThat(!mdmDoctorVerdicts.getSskuVerdictResults().isEmpty()
            && !mdmDoctorVerdicts.getSskuPartnerVerdictResults().isEmpty()).isTrue();
    }

    @Test
    public void checkGoldSskusFound() throws Exception {
        List<CommonSsku> goldCommonSskus = getMdmDoctorUnitedResponse()
            .getGoldCommonSsku();
        Assertions.assertThat(!goldCommonSskus.isEmpty()).isTrue();
    }

    private boolean isAddedTimestampPresent() throws Exception {
        for (MdmDoctorQueueStatistic mdmDoctorQueueStatistic :
            getMdmDoctorUnitedResponse().getMdmDoctorQueueStatisticList()) {
            if (mdmDoctorQueueStatistic.getOldestUnprocessed().getAddedTimestamp() == null) {
                return false;
            }
        }
        return true;
    }

    private MdmDoctorUnitedResponse getMdmDoctorUnitedResponse() throws Exception {
        MockHttpServletRequestBuilder getRequest = MockMvcRequestBuilders.get(
            "/mdm-api/doctor/ssku/offer-info/offer?businessId={businessId}&shopSku={shopSku}",
            SHOP_SKU_KEY.getSupplierId(), SHOP_SKU_KEY.getShopSku()
        ).accept(MediaType.APPLICATION_JSON_UTF8);
        MvcResult result = mockMvc.perform(getRequest)
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON_UTF8))
            .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON_UTF8))
            .andReturn();

        String response = result.getResponse().getContentAsString();

        return JsonMapper.DEFAULT_OBJECT_MAPPER.configure(MapperFeature.ALLOW_FINAL_FIELDS_AS_MUTATORS, false)
            .readValue(response, MdmDoctorUnitedResponse.class);
    }

    private void loadGroupManagerDataToRepos() {
        mdmSskuGroupManager.insert(
            SHOP_SKU_KEY,
            MdmSskuKeyGroup.createBusinessGroup(SHOP_SKU_KEY, List.of(SHOP_SKU_KEY))
        );
    }

    private void loadQueueStatisticDataToRepos() {
        sendReferenceItemQRepository.insert(new SskuToRefreshInfo()
            .setShopSku(SHOP_SKU_KEY.getShopSku())
            .setSupplierId(SHOP_SKU_KEY.getSupplierId())
        );
        sskuToRefreshRepository.insert(new SskuToRefreshInfo()
            .setShopSku(SHOP_SKU_KEY.getShopSku())
            .setSupplierId(SHOP_SKU_KEY.getSupplierId())
        );
        sendToDatacampQRepository.insert(new SskuToRefreshInfo()
            .setShopSku(SHOP_SKU_KEY.getShopSku())
            .setSupplierId(SHOP_SKU_KEY.getSupplierId())
        );
        lbFailedOfferQueueRepository.insert(new SskuToRefreshInfo()
            .setShopSku(SHOP_SKU_KEY.getShopSku())
            .setSupplierId(SHOP_SKU_KEY.getSupplierId())
        );
        sendToErpQueueRepository.insert(new SskuToRefreshInfo()
            .setShopSku(SHOP_SKU_KEY.getShopSku())
            .setSupplierId(SHOP_SKU_KEY.getSupplierId())
        );
        var mskuQueueInfo = new MdmMskuQueueInfo();
        mskuQueueInfo.setEntityKey(MSKU_ID);
        mskuToMboQueueRepository.insert(mskuQueueInfo);
        mskuToRefreshRepository.insert(mskuQueueInfo);
    }

    private void loadSilversAndMappingsDataToRepos() {
        mappingsCacheRepository.insert(new MappingCacheDao()
            .setShopSkuKey(SHOP_SKU_KEY)
            .setCategoryId(CATEGORY_ID)
            .setMskuId(MSKU_ID)
        );
        mskuRepository.insertOrUpdateMsku(new CommonMsku(CATEGORY_ID, MSKU_ID));
        categoryParamValueRepository.insert(new CategoryParamValue().setCategoryId(CATEGORY_ID));
    }

    private void loadVerdictsDataToRepos() {
        SskuVerdictResult sskuVerdictResult = new SskuVerdictResult();
        sskuVerdictResult.setSupplierId(SHOP_SKU_KEY.getSupplierId());
        sskuVerdictResult.setShopSku(SHOP_SKU_KEY.getShopSku());
        sskuGoldenVerdictRepository.insert(sskuVerdictResult);
        SskuPartnerVerdictResult sskuPartnerVerdictResult = new SskuPartnerVerdictResult();
        sskuPartnerVerdictResult.setSupplierId(SHOP_SKU_KEY.getSupplierId());
        sskuPartnerVerdictResult.setShopSku(SHOP_SKU_KEY.getShopSku());
        sskuPartnerVerdictRepository.insert(sskuPartnerVerdictResult);
    }

    private void loadGoldSskuDataToRepos() {
        CommonSsku commonSsku = new CommonSsku(SHOP_SKU_KEY);
        mdmCommonSskuService.update(List.of(commonSsku));
    }
}
