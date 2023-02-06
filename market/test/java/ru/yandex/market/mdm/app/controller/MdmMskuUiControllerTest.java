package ru.yandex.market.mdm.app.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.assertj.core.api.Assertions;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import ru.yandex.market.mbo.common.utils.LocalizedStringUtils;
import ru.yandex.market.mbo.export.MboParameters;
import ru.yandex.market.mbo.http.ModelStorage;
import ru.yandex.market.mbo.lightmapper.JsonMapper;
import ru.yandex.market.mbo.mdm.common.infrastructure.MdmFileHistoryRepositoryMock;
import ru.yandex.market.mbo.mdm.common.infrastructure.MdmFileImportHelperService;
import ru.yandex.market.mbo.mdm.common.infrastructure.MdmS3FileServiceMock;
import ru.yandex.market.mbo.mdm.common.masterdata.MdmUser;
import ru.yandex.market.mbo.mdm.common.masterdata.model.MappingCacheDao;
import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.MasterDataSourceType;
import ru.yandex.market.mbo.mdm.common.masterdata.model.msku.CommonMsku;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.MdmParamIoType;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.MskuParamValue;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.MappingsCacheRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.MdmMboUser;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.MdmMboUsersRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.MdmUserRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.MdmUserRepositoryMock;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.param.MskuRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.queue.MdmQueuesManager;
import ru.yandex.market.mbo.mdm.common.masterdata.services.GlobalParamValueService;
import ru.yandex.market.mbo.mdm.common.masterdata.services.MboMskuUpdateService;
import ru.yandex.market.mbo.mdm.common.masterdata.services.editor.MdmSampleDataService;
import ru.yandex.market.mbo.mdm.common.masterdata.services.editor.MdmSampleDataServiceImpl;
import ru.yandex.market.mbo.mdm.common.masterdata.services.msku.MdmCommonMskuMboService;
import ru.yandex.market.mbo.mdm.common.masterdata.services.msku.MdmCommonMskuMboServiceImpl;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.CommonMskuConverter;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownMdmParams;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.MdmParamCache;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.MdmParamProviderImpl;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.MskuMdmParamExcelExportService;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.MskuMdmParamExcelService;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.TestMdmParamUtils;
import ru.yandex.market.mbo.mdm.common.service.msku.MskuFullCircuitSyncService;
import ru.yandex.market.mbo.mdm.common.utils.MdmBaseDbTestClass;
import ru.yandex.market.mbo.storage.StorageKeyValueService;
import ru.yandex.market.mboc.common.masterdata.KnownMdmMboParams;
import ru.yandex.market.mboc.common.masterdata.parsing.CommonMskuValidator;
import ru.yandex.market.mboc.common.masterdata.repository.CargoTypeRepository;
import ru.yandex.market.mboc.common.masterdata.repository.CargoTypeRepositoryMock;
import ru.yandex.market.mboc.common.services.modelstorage.MboModelsService;
import ru.yandex.market.mboc.common.services.modelstorage.MboModelsServiceMock;
import ru.yandex.market.mboc.common.users.UserRoles;
import ru.yandex.market.mboc.common.utils.MdmProperties;
import ru.yandex.market.mboc.common.utils.SecurityUtil;
import ru.yandex.market.mboc.common.web.DataPage;

/**
 * @author albina-gima
 * @date 10/2/20
 */
@SuppressWarnings("checkstyle:magicnumber")
public class MdmMskuUiControllerTest extends MdmBaseDbTestClass {
    private static final long TIMESTAMP = 123L;
    private static final long MSKU_ID = 100500L;
    private static final long SEED = 20200607L;
    private static final MappingCacheDao MAPPING = new MappingCacheDao()
        .setMskuId(MSKU_ID)
        .setCategoryId(38)
        .setShopSku("U-238")
        .setSupplierId(1999);

    @Autowired
    private MskuMdmParamExcelExportService mskuMdmParamExcelExportService;
    @Autowired
    private MskuMdmParamExcelService mskuMdmParamExcelService;
    @Autowired
    private MdmParamCache mdmParamCache;
    @Autowired
    private StorageKeyValueService keyValueService;
    @Autowired
    MskuRepository mskuRepository;
    @Autowired
    private GlobalParamValueService globalParamValueService;
    @Autowired
    private MdmQueuesManager queuesManager;
    @Autowired
    private MdmParamProviderImpl mdmParamProvider;
    @Autowired
    private CommonMskuValidator commonMskuValidator;
    @Autowired
    private MappingsCacheRepository mappingsCacheRepository;
    @Autowired
    private StorageKeyValueService skv;

    private CargoTypeRepository cargoTypeRepository;
    private MboModelsService mboModelsServiceMock;
    private MboMskuUpdateService mboMskuUpdateService;
    private MdmCommonMskuMboService mdmCommonMskuMboService;
    private MdmMboUsersRepository mdmMboUsersRepository;
    private CommonMskuConverter commonMskuConverter;
    private MockMvc mockMvc;
    private MdmSampleDataService mdmSampleDataService;
    private MdmMskuUiController mdmMskuUiController;
    private MdmUserRepository mdmUserRepository;
    private CommonMsku msku1;

    private Random random;

    @Before
    public void setUp() {
        this.mdmUserRepository = new MdmUserRepositoryMock();
        random = new Random(SEED);
        mboModelsServiceMock = new MboModelsServiceMock();
        cargoTypeRepository = new CargoTypeRepositoryMock();
        mdmMboUsersRepository = Mockito.mock(MdmMboUsersRepository.class);
        Mockito.when(mdmMboUsersRepository.findByStaffLogin(Mockito.any())).then(ivc ->
            Optional.of(new MdmMboUser().setUid(1).setStaffLogin("vasia")
                .setYandexLogin("null").setFullName("abc")));
        mdmUserRepository.insert(new MdmUser().setLogin("vasia").setRoles(List.of()));
        SecurityUtil.authenticate("vasia");
        skv.putValue(MdmProperties.UI_SAVE_AS_ADMIN_ENABLED, true);
        skv.invalidateCache();
        commonMskuConverter = new CommonMskuConverter(mdmParamCache);
        mboMskuUpdateService = new MboMskuUpdateService(mboModelsServiceMock, cargoTypeRepository);
        mdmCommonMskuMboService = new MdmCommonMskuMboServiceImpl(mboModelsServiceMock, mboMskuUpdateService,
            commonMskuConverter, globalParamValueService, mskuRepository, queuesManager,
            commonMskuValidator, mappingsCacheRepository, mdmUserRepository, skv);

        MdmFileImportHelperService fileImportHelperService = new MdmFileImportHelperService(
            new MdmS3FileServiceMock(), new MdmFileHistoryRepositoryMock());

        mdmSampleDataService = new MdmSampleDataServiceImpl(keyValueService, mappingsCacheRepository);

        mdmMskuUiController = new MdmMskuUiController(
            new ObjectMapper(),
            mdmParamProvider,
            mdmCommonMskuMboService,
            mskuMdmParamExcelExportService,
            mskuMdmParamExcelService,
            fileImportHelperService,
            mdmMboUsersRepository,
            mdmUserRepository,
            mdmSampleDataService,
            Mockito.mock(MskuFullCircuitSyncService.class));
        mockMvc = MockMvcBuilders.standaloneSetup(mdmMskuUiController).build();
        mboModelsServiceMock.saveModels(List.of(model()));

        loadDataToRepos();
    }

    @After
    public void cleanUp() {
        SecurityUtil.deauthenticate();
    }

    @Test
    public void testUpdateWithAssessor() throws Exception {
        Mockito.reset(mdmMboUsersRepository);
        mdmUserRepository.deleteAll();
        mdmUserRepository.insert(new MdmUser().setLogin("vasia").setRoles(List.of(UserRoles.MDM_ASSESSOR)));

        MockHttpServletRequestBuilder getRequest = MockMvcRequestBuilders.get(
            "/mdm-api/ui/msku/get?mskuId={mskuId}", MSKU_ID).accept(MediaType.APPLICATION_JSON_UTF8);
        MvcResult getResult = mockMvc.perform(getRequest)
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON_UTF8))
            .andExpect(MockMvcResultMatchers.jsonPath("$.mskuId").value(MSKU_ID))
            .andReturn();

        String jsonMsku = getResult.getResponse().getContentAsString();
        MockHttpServletRequestBuilder updateRequest =
            MockMvcRequestBuilders.post("/mdm-api/ui/msku/update?userType={userType}",
                MasterDataSourceType.MDM_OPERATOR)
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .content(jsonMsku);

        mockMvc.perform(updateRequest)
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andReturn();

        var msku = mskuRepository.findMsku(MSKU_ID).orElseThrow();
        var pvs = new HashMap<>(msku.getParamValues());
        pvs.remove(KnownMdmParams.HEAVY_GOOD);
        pvs.remove(KnownMdmParams.BMDM_ID);
        Assertions.assertThat(pvs).hasSize(1);
        Assertions.assertThat(pvs.values())
            .allMatch(pv -> pv.getMasterDataSourceType() == MasterDataSourceType.MDM_OPERATOR);
        Assertions.assertThat(pvs.values())
            .allMatch(pv -> pv.getMdmParamId() == KnownMdmParams.EXPIR_DATE);
    }

    @Test
    public void testUpdateMboWithAdminSourceTypeSholdReturnErrorWithoutGrants() throws Exception {
        MockHttpServletRequestBuilder getRequest = MockMvcRequestBuilders.get(
            "/mdm-api/ui/msku/get?mskuId={mskuId}", MSKU_ID).accept(MediaType.APPLICATION_JSON_UTF8);
        MvcResult getResult = mockMvc.perform(getRequest)
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON_UTF8))
            .andExpect(MockMvcResultMatchers.jsonPath("$.mskuId").value(MSKU_ID))
            .andReturn();

        String jsonMsku = getResult.getResponse().getContentAsString();
        MockHttpServletRequestBuilder updateRequest =
            MockMvcRequestBuilders.post("/mdm-api/ui/msku/update?userType={userType}", MasterDataSourceType.MDM_ADMIN)
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .content(jsonMsku);

        mockMvc.perform(updateRequest)
            .andExpect(MockMvcResultMatchers.status().isBadRequest())
            .andExpect(MockMvcResultMatchers.xpath("/List/item[1]").string(
                "MskuId: 100500, [AccessException: У пользователя недостаточно прав для выполнения операции.]"))
            .andReturn();

        var msku = mskuRepository.findMsku(MSKU_ID).orElseThrow();
        var pvs = new HashMap<>(msku.getParamValues());
        pvs.remove(KnownMdmParams.BMDM_ID);
        Assertions.assertThat(pvs).hasSize(1);
        Assertions.assertThat(pvs.get(KnownMdmParams.HEAVY_GOOD))
            .isEqualTo(msku1.getParamValue(KnownMdmParams.HEAVY_GOOD).orElseThrow());
    }

    @Test
    public void testUpdateMboWithOperatorSourceType() throws Exception {
        MockHttpServletRequestBuilder getRequest = MockMvcRequestBuilders.get(
            "/mdm-api/ui/msku/get?mskuId={mskuId}", MSKU_ID).accept(MediaType.APPLICATION_JSON_UTF8);
        MvcResult getResult = mockMvc.perform(getRequest)
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON_UTF8))
            .andExpect(MockMvcResultMatchers.jsonPath("$.mskuId").value(MSKU_ID))
            .andReturn();

        String jsonMsku = getResult.getResponse().getContentAsString();
        MockHttpServletRequestBuilder updateRequest = MockMvcRequestBuilders
            .post("/mdm-api/ui/msku/update?userType={userType}", MasterDataSourceType.MDM_OPERATOR)
            .contentType(MediaType.APPLICATION_JSON_UTF8)
            .content(jsonMsku);

        mockMvc.perform(updateRequest)
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andReturn();

        var msku = mskuRepository.findMsku(MSKU_ID).orElseThrow();
        var pvs = new HashMap<>(msku.getParamValues());
        pvs.remove(KnownMdmParams.HEAVY_GOOD);
        pvs.remove(KnownMdmParams.BMDM_ID);
        Assertions.assertThat(pvs).isNotEmpty();
        Assertions.assertThat(pvs.values())
            .allMatch(pv -> pv.getMasterDataSourceType() == MasterDataSourceType.MDM_OPERATOR);
    }

    @Test
    public void testUpdateMboWithAdminSourceTypeShouldReturnOk() throws Exception {
        MockHttpServletRequestBuilder getRequest = MockMvcRequestBuilders.get(
            "/mdm-api/ui/msku/get?mskuId={mskuId}", MSKU_ID).accept(MediaType.APPLICATION_JSON_UTF8);
        MvcResult getResult = mockMvc.perform(getRequest)
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON_UTF8))
            .andExpect(MockMvcResultMatchers.jsonPath("$.mskuId").value(MSKU_ID))
            .andReturn();

        var sourceTypeId =
            SecurityUtil.getCurrentUserLoginOrDefault(MasterDataSourceType.UNKNOWN_MDM_OPERATOR_SOURCE_ID);
        mdmUserRepository.insert(new MdmUser().setLogin(sourceTypeId).setRoles(Set.of("MDM_UI_ADMIN")));
        String jsonMsku = getResult.getResponse().getContentAsString();
        MockHttpServletRequestBuilder updateRequest =
            MockMvcRequestBuilders.post("/mdm-api/ui/msku/update?userType={userType}", MasterDataSourceType.MDM_ADMIN)
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .content(jsonMsku);

        mockMvc.perform(updateRequest)
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andReturn();

        var msku = mskuRepository.findMsku(MSKU_ID).orElseThrow();
        var pvs = new HashMap<>(msku.getParamValues());
        pvs.remove(KnownMdmParams.HEAVY_GOOD);
        pvs.remove(KnownMdmParams.BMDM_ID);
        Assertions.assertThat(pvs).isNotEmpty();
        Assertions.assertThat(pvs.values())
            .allMatch(pv -> pv.getMasterDataSourceType() == MasterDataSourceType.MDM_ADMIN);
    }

    @Test
    public void testUpdateMboWithoutSourceType() throws Exception {
        MockHttpServletRequestBuilder getRequest = MockMvcRequestBuilders.get(
            "/mdm-api/ui/msku/get?mskuId={mskuId}", MSKU_ID).accept(MediaType.APPLICATION_JSON_UTF8);
        MvcResult getResult = mockMvc.perform(getRequest)
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON_UTF8))
            .andExpect(MockMvcResultMatchers.jsonPath("$.mskuId").value(MSKU_ID))
            .andReturn();

        String jsonMsku = getResult.getResponse().getContentAsString();
        MockHttpServletRequestBuilder updateRequest = MockMvcRequestBuilders.post("/mdm-api/ui/msku/update")
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .content(jsonMsku);

        mockMvc.perform(updateRequest)
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andReturn();

        var msku = mskuRepository.findMsku(MSKU_ID).orElseThrow();
        var pvs = new HashMap<>(msku.getParamValues());
        pvs.remove(KnownMdmParams.HEAVY_GOOD);
        pvs.remove(KnownMdmParams.BMDM_ID);
        Assertions.assertThat(pvs).isNotEmpty();
        Assertions.assertThat(pvs.values())
            .allMatch(pv -> pv.getMasterDataSourceType() == MasterDataSourceType.MDM_OPERATOR);
    }

    @Test
    public void whenGetMetadataShouldReturnLiquibaseImportResults() throws Exception {
        MockHttpServletRequestBuilder getRequest = MockMvcRequestBuilders.get(
            "/mdm-api/ui/msku/metadata?type={type}", MdmParamIoType.MSKU_EXCEL_EXPORT.name()
        ).accept(MediaType.APPLICATION_JSON_UTF8);

        mockMvc.perform(getRequest)
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON_UTF8))
            .andExpect(MockMvcResultMatchers.jsonPath("$[0].id").value(8))
            .andExpect(MockMvcResultMatchers.jsonPath("$[0].title").value("Срок годности"))
            .andExpect(MockMvcResultMatchers.jsonPath("$[1].id").value(9))
            .andExpect(MockMvcResultMatchers.jsonPath("$[1].title").value("Единица измерения срока годности"))
            .andExpect(MockMvcResultMatchers.jsonPath("$[2].id").value(10))
            .andExpect(MockMvcResultMatchers.jsonPath("$[2].title").value("Дополнительные условия хранения"))
            .andExpect(MockMvcResultMatchers.jsonPath("$[3].id").value(20))
            .andExpect(MockMvcResultMatchers.jsonPath("$[3].title").value("Скрывать срок годности"))
            .andExpect(MockMvcResultMatchers.jsonPath("$[4].id").value(11))
            .andExpect(MockMvcResultMatchers.jsonPath("$[4].title").value("Срок службы"))
            .andExpect(MockMvcResultMatchers.jsonPath("$[5].id").value(12))
            .andExpect(MockMvcResultMatchers.jsonPath("$[5].title").value("Единица измерения срока службы"))
            .andExpect(MockMvcResultMatchers.jsonPath("$[6].id").value(13))
            .andExpect(MockMvcResultMatchers.jsonPath("$[6].title").value("Дополнительные условия использования"))
            .andExpect(MockMvcResultMatchers.jsonPath("$[7].id").value(21))
            .andExpect(MockMvcResultMatchers.jsonPath("$[7].title").value("Скрывать срок службы"))
            .andExpect(MockMvcResultMatchers.jsonPath("$[15].id").value(502))
            .andExpect(MockMvcResultMatchers.jsonPath("$[15].title").value("Ценное"));
    }

    @Test
    public void whenExportExcelShouldReturnExcelWithMskuMasterData() throws Exception {
        long mskuId = 50L;
        ModelStorage.Model model = ModelStorage.Model.newBuilder()
            .setId(mskuId).setCurrentType("GURU")
            .addParameterValues(ModelStorage.ParameterValue.newBuilder()
                .setParamId(KnownMdmMboParams.HEAVY_GOOD_CATEGORY_PARAM_ID)
                .setXslName("cargoType300")
                .setValueType(MboParameters.ValueType.BOOLEAN)
                .setBoolValue(true)
                .setModificationDate(TIMESTAMP))
            .addParameterValues(ModelStorage.ParameterValue.newBuilder()
                .setParamId(KnownMdmMboParams.HEAVY_GOOD_20_CATEGORY_PARAM_ID)
                .setXslName("cargoType301")
                .setValueType(MboParameters.ValueType.BOOLEAN)
                .setBoolValue(true)
                .setModificationDate(TIMESTAMP))
            .build();
        mboModelsServiceMock.saveModels(List.of(model));

        MockHttpServletRequestBuilder postRequest = MockMvcRequestBuilders
            .get("/mdm-api/ui/msku/export-to-excel/?searchString=" + mskuId);

        mockMvc.perform(postRequest)
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.header().string("Content-Disposition",
                "attachment; filename*=UTF-8''msku-param-values.xlsx"))
            .andReturn();
    }

    @Test
    public void testGenerateExportByFileTemplateShouldSuccessfullyGenerateEmptyFileWithHeader() throws Exception {
        MockHttpServletRequestBuilder getRequest = MockMvcRequestBuilders.get(
            "/mdm-api/ui/msku/export-by-file-template");

        MvcResult mvcResult = mockMvc.perform(getRequest)
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.header().string("Content-Disposition",
                "attachment; filename*=UTF-8''msku-template.xlsx"))
            .andReturn();
    }

    @Test
    public void testFindSampleData() throws Exception {
        keyValueService.putValue(MdmProperties.MSKU_SAMPLE_TABLE_PERCENTAGE_KEY, "100");
        MockHttpServletRequestBuilder getRequest = MockMvcRequestBuilders.get(
            "/mdm-api/ui/msku/find-sample").accept(MediaType.APPLICATION_JSON_UTF8);

        MvcResult result = mockMvc.perform(getRequest)
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON_UTF8))
            .andReturn();

        String dataPageString = result.getResponse().getContentAsString();
        DataPage<CommonMsku> dataPage = JsonMapper.DEFAULT_OBJECT_MAPPER.readValue(
            dataPageString, new TypeReference<DataPage<CommonMsku>>() {
            });
        Assertions.assertThat(dataPage.getTotalCount()).isEqualTo(1);
        Assertions.assertThat(dataPage.getItems().get(0).getMskuId()).isEqualTo(MSKU_ID);
        var paramValue = dataPage.getItems().get(0).getValues().stream()
            .filter(v -> v.getXslName().equals("warrantyPeriod"))
            .findAny();
        Assertions.assertThat(paramValue).isPresent();
        Assertions.assertThat(paramValue.get().getStrings()).isEqualTo(List.of("Проверка"));
    }

    private static ModelStorage.Model model() {
        return ModelStorage.Model.newBuilder()
            .setId(MSKU_ID).setCurrentType("GURU")
            .addParameterValues(getModelParamVal(KnownMdmMboParams.HEAVY_GOOD_CATEGORY_PARAM_ID,
                "cargoType300", MboParameters.ValueType.BOOLEAN, true))
            .addParameterValues(getModelParamVal(KnownMdmMboParams.HEAVY_GOOD_20_CATEGORY_PARAM_ID,
                "cargoType301", MboParameters.ValueType.BOOLEAN, true))
            .addParameterValues(getModelParamVal(KnownMdmMboParams.WARRANTY_PERIOD_PARAM_ID,
                "warrantyPeriod", MboParameters.ValueType.STRING, "Проверка"))
            .addParameterValues(getModelParamVal(KnownMdmMboParams.EXPIR_DATE_PARAM_ID,
                "expir_date", MboParameters.ValueType.BOOLEAN, false))
            .build();
    }

    private static ModelStorage.ParameterValue.Builder getModelParamVal(long paramId,
                                                                        String xslName,
                                                                        MboParameters.ValueType type,
                                                                        Object typeValue) {
        ModelStorage.ParameterValue.Builder builder = ModelStorage.ParameterValue.newBuilder();
        builder.setParamId(paramId)
            .setXslName(xslName)
            .setTypeId(type.getNumber())
            .setValueType(type)
            .setModificationDate(TIMESTAMP);

        if (MboParameters.ValueType.BOOLEAN.equals(type) && typeValue instanceof Boolean) {
            builder.setBoolValue((boolean) typeValue);
        } else if (MboParameters.ValueType.NUMERIC.equals(type) && typeValue instanceof String) {
            builder.setNumericValue((String) typeValue);
        } else if (MboParameters.ValueType.STRING.equals(type) && typeValue instanceof String) {
            builder.addStrValue(LocalizedStringUtils.defaultString((String) typeValue));
        }

        return builder;
    }

    private void loadDataToRepos() {
        MskuParamValue value = generateMskuParamValue();
        msku1 = new CommonMsku(value.getMskuId(), List.of(value));
        mskuRepository.insertOrUpdateMsku(msku1);
        mappingsCacheRepository.insert(MAPPING);
    }

    private MskuParamValue generateMskuParamValue() {
        MskuParamValue data = new MskuParamValue().setMskuId(MSKU_ID);
        TestMdmParamUtils.createRandomMdmParamValue(random, mdmParamCache.get(KnownMdmParams.HEAVY_GOOD))
            .copyTo(data);
        data.setMasterDataSourceType(MasterDataSourceType.AUTO);
        data.setMasterDataSourceId(MasterDataSourceType.EMPTY_SOURCE_ID);
        data.setBool(true);
        return data;
    }
}
