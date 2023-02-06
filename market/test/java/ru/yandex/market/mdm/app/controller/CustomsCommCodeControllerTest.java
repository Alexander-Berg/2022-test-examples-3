package ru.yandex.market.mdm.app.controller;

import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
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

import ru.yandex.market.mbo.mdm.common.masterdata.model.MappingCacheDao;
import ru.yandex.market.mbo.mdm.common.masterdata.model.MdmGoodGroup;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.CategoryParamValue;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.MappingsCacheRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.MdmGoodGroupRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.param.CategoryParamValueRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.param.CustomsCommCodeRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.services.MdmGoodGroupValidationService;
import ru.yandex.market.mbo.mdm.common.masterdata.services.cccode.CCCodeValidationService;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.CustomsCommCodeMarkupService;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.CustomsCommCodeMarkupServiceImpl;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownMdmParams;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.MdmParamCache;
import ru.yandex.market.mbo.mdm.common.utils.MdmBaseDbTestClass;
import ru.yandex.market.mboc.common.MbocErrors;
import ru.yandex.market.mboc.common.masterdata.model.cccode.CustomsCommCode;
import ru.yandex.market.mboc.common.masterdata.parsing.SskuMasterDataFields;
import ru.yandex.market.mboc.common.offers.model.ShopSkuKey;
import ru.yandex.market.mboc.common.utils.ErrorInfo;
import ru.yandex.market.mboc.common.utils.TaskQueueRegistratorMock;
import ru.yandex.market.mboc.common.web.CustomsCommCodeLite;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertTrue;

public class CustomsCommCodeControllerTest extends MdmBaseDbTestClass {
    @Autowired
    private MdmGoodGroupRepository mdmGoodGroupRepository;
    @Autowired
    private MdmParamCache mdmParamCache;
    @Autowired
    private CustomsCommCodeRepository customsCommCodeRepository;
    @Autowired
    private CCCodeValidationService ccCodeValidationService;
    @Autowired
    private CategoryParamValueRepository categoryParamValueRepository;
    @Autowired
    private MappingsCacheRepository mappingsCacheRepository;

    private CustomsCommCodeMarkupService codeMarkupService;
    private MockMvc mockMvc;
    private CustomsCommCodeController customsCommCodeController;
    private TaskQueueRegistratorMock taskQueueRegistratorMock = new TaskQueueRegistratorMock();
    private final ObjectMapper mapper = new ObjectMapper();
    private MdmGoodGroupValidationService mdmGoodGroupValidationService;

    @Before
    public void setUp() {
        codeMarkupService = new CustomsCommCodeMarkupServiceImpl(mdmParamCache, customsCommCodeRepository,
            ccCodeValidationService, categoryParamValueRepository, taskQueueRegistratorMock, mdmGoodGroupRepository,
            mappingsCacheRepository);
        mdmGoodGroupValidationService = new MdmGoodGroupValidationService(mdmGoodGroupRepository);
        customsCommCodeController = new CustomsCommCodeController(codeMarkupService, mdmGoodGroupRepository,
            mdmGoodGroupValidationService);
        mockMvc = MockMvcBuilders.standaloneSetup(customsCommCodeController).build();
    }

    @Test
    public void whenGetAllGoodsGroup() throws Exception {
        MockHttpServletRequestBuilder request = MockMvcRequestBuilders
            .get("/mdm-api/customs-comm-codes/allGoodsGroup")
            .accept(MediaType.APPLICATION_JSON_UTF8);
        MvcResult mvcResult = mockMvc.perform(request).andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON_UTF8))
            .andReturn();

        String strResponse = mvcResult.getResponse().getContentAsString();
        List<MdmGoodGroup> response = Arrays.asList(mapper.readValue(strResponse, MdmGoodGroup[].class));

        List<MdmGoodGroup> expected = mdmGoodGroupRepository.findAll();
        assertTrue(response.containsAll(expected));
    }

    @Test
    public void whenAddGoodGroup() throws Exception {
        String newGroupName = "some sample group";
        MockHttpServletRequestBuilder request = MockMvcRequestBuilders
            .get("/mdm-api/customs-comm-codes/addGoodGroup?groupName={newGroupName}", newGroupName)
            .accept(MediaType.APPLICATION_JSON_UTF8);
        MvcResult mvcResult = mockMvc.perform(request).andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON_UTF8))
            .andExpect(MockMvcResultMatchers.jsonPath("$.groupName").value(newGroupName))
            .andReturn();

        String strResponse = mvcResult.getResponse().getContentAsString();
        MdmGoodGroup response = mapper.readValue(strResponse, MdmGoodGroup.class);

        MdmGoodGroup mdmGoodGroup = mdmGoodGroupRepository.findById(response.getId());
        assertNotNull(mdmGoodGroup);
        assertEquals(response, mdmGoodGroup);
    }

    @Test
    public void whenLoadAll() throws Exception {
        CustomsCommCode customsCommCode = new CustomsCommCode();
        customsCommCode.setCode("98");
        customsCommCode.setTitle("Some title");
        long goodGroupId = mdmGoodGroupRepository.findAll().get(0).getId();
        customsCommCode.setGoodGroupId(goodGroupId);
        codeMarkupService.create(customsCommCode);

        MockHttpServletRequestBuilder request = MockMvcRequestBuilders
            .get("/mdm-api/customs-comm-codes/list")
            .accept(MediaType.APPLICATION_JSON_UTF8);
        MvcResult mvcResult = mockMvc.perform(request).andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON_UTF8))
            .andReturn();

        String strResponse = mvcResult.getResponse().getContentAsString();
        CustomsCommCodeLite response = Arrays.asList(mapper.readValue(strResponse, CustomsCommCodeLite[].class))
            .get(0);

        CustomsCommCodeLite expected = codeMarkupService.loadAllLite().get(0);
        assertEquals(expected, response);
    }

    @Test
    public void whenGetCodeById() throws Exception {
        CustomsCommCode customsCommCode = new CustomsCommCode();
        customsCommCode.setCode("98");
        customsCommCode.setId(124);
        customsCommCode.setTitle("Some title");
        long goodGroupId = mdmGoodGroupRepository.findAll().get(0).getId();
        customsCommCode.setGoodGroupId(goodGroupId);
        customsCommCodeRepository.insert(customsCommCode);

        MockHttpServletRequestBuilder getRequest = MockMvcRequestBuilders
            .get("/mdm-api/customs-comm-codes/{id}", customsCommCode.getId())
            .accept(MediaType.APPLICATION_JSON_UTF8);
        MvcResult mvcGetResult = mockMvc.perform(getRequest).andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON_UTF8))
            .andExpect(MockMvcResultMatchers.jsonPath("$.id").value(customsCommCode.getId()))
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value(customsCommCode.getCode()))
            .andExpect(MockMvcResultMatchers.jsonPath("$.title").value(customsCommCode.getTitle()))
            .andReturn();
    }

    @Test
    public void whenGetByShopSkuKeyShouldSuccessfullyFindPrefix() throws Exception {
        int categoryId = 55;
        ShopSkuKey shopSkuKey = new ShopSkuKey(1, "magic-key");
        MappingCacheDao mapping = new MappingCacheDao().setShopSkuKey(shopSkuKey).setCategoryId(categoryId);
        mappingsCacheRepository.insert(mapping);

        String prefix = "7789";
        CategoryParamValue paramValue = (CategoryParamValue) new CategoryParamValue()
            .setCategoryId(categoryId)
            .setMdmParamId(KnownMdmParams.CUSTOMS_COMM_CODE_PREFIX)
            .setString(prefix);
        categoryParamValueRepository.insert(paramValue);

        CustomsCommCode customsCommCode = new CustomsCommCode().setId(1).setCode(prefix).setTitle("title for prefix");
        customsCommCodeRepository.insert(customsCommCode);

        MockHttpServletRequestBuilder getRequest = MockMvcRequestBuilders
            .get("/mdm-api/customs-comm-codes/{ssku}/{supplier_id}",
                shopSkuKey.getShopSku(), shopSkuKey.getSupplierId())
            .accept(MediaType.APPLICATION_JSON_UTF8);

        mockMvc.perform(getRequest).andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON_UTF8))
            .andExpect(MockMvcResultMatchers.jsonPath("$.id").value(customsCommCode.getId()))
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value(customsCommCode.getCode()))
            .andExpect(MockMvcResultMatchers.jsonPath("$.title").value(customsCommCode.getTitle()))
            .andReturn();
    }

    @Test
    public void whenGetByShopSkuKeyShouldReturnEmptyResult() throws Exception {
        // no mapping in customCommCodeRepo - no result
        int categoryId = 55;
        ShopSkuKey shopSkuKey = new ShopSkuKey(1, "magic-key");
        MappingCacheDao mapping = new MappingCacheDao().setShopSkuKey(shopSkuKey).setCategoryId(categoryId);
        mappingsCacheRepository.insert(mapping);

        String prefix = "7789";
        CategoryParamValue paramValue = (CategoryParamValue) new CategoryParamValue()
            .setCategoryId(categoryId)
            .setMdmParamId(KnownMdmParams.CUSTOMS_COMM_CODE_PREFIX)
            .setString(prefix);
        categoryParamValueRepository.insert(paramValue);

        MockHttpServletRequestBuilder getRequest = MockMvcRequestBuilders
            .get("/mdm-api/customs-comm-codes/{ssku}/{supplier_id}",
                shopSkuKey.getShopSku(), shopSkuKey.getSupplierId())
            .accept(MediaType.APPLICATION_JSON_UTF8);

        MvcResult mvcResult = mockMvc.perform(getRequest)
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andReturn();

        String responseAsString = mvcResult.getResponse().getContentAsString();
        Assertions.assertThat(responseAsString).isEmpty();
    }

    @Test
    public void whenGetByShopSkuKeyShouldReturnEmptyResult2() throws Exception {
        // no code in repo - no result
        ShopSkuKey shopSkuKey = new ShopSkuKey(1, "magic-key");

        MockHttpServletRequestBuilder getRequest = MockMvcRequestBuilders
            .get("/mdm-api/customs-comm-codes/{ssku}/{supplier_id}",
                shopSkuKey.getShopSku(), shopSkuKey.getSupplierId())
            .accept(MediaType.APPLICATION_JSON_UTF8);

        MvcResult mvcResult = mockMvc.perform(getRequest)
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andReturn();
        String responseAsString = mvcResult.getResponse().getContentAsString();
        Assertions.assertThat(responseAsString).isEmpty();
    }

    @Test
    public void validateFullTnvedCodeShouldReturnErrorMessage() throws Exception {
        List<String> fullTnvedCodes = List.of("0000000000", "778912347", "11111111111", "");

        for (String tnved : fullTnvedCodes) {
            MockHttpServletRequestBuilder getRequest = MockMvcRequestBuilders
                .get("/mdm-api/customs-comm-codes/validate?fullTnvedCode={fullTnvedCode}", tnved)
                .accept(MediaType.APPLICATION_JSON_UTF8);

            MvcResult mvcResult = mockMvc.perform(getRequest).andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON_UTF8))
                .andReturn();

            String responseAsString = mvcResult.getResponse().getContentAsString();
            ErrorInfo errorInfo = mapper.readValue(responseAsString, ErrorInfo.class);

            Assertions.assertThat(errorInfo.render()).isEqualToIgnoringCase(
                MbocErrors.get().mdCustomsCommodityCode(SskuMasterDataFields.CUSTOMS_COMMODITY_CODE, tnved).render());
        }
    }

    @Test
    public void validateFullTnvedCodeShouldReturnOk() throws Exception {
        List<String> fullTnvedCodes = List.of("7789126712", "16475162849123");

        for (String tnved : fullTnvedCodes) {
            MockHttpServletRequestBuilder getRequest = MockMvcRequestBuilders
                .get("/mdm-api/customs-comm-codes/validate?fullTnvedCode={fullTnvedCode}", tnved)
                .accept(MediaType.APPLICATION_JSON_UTF8);

            MvcResult mvcResult = mockMvc.perform(getRequest).andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON_UTF8))
                .andReturn();

            String responseAsString = mvcResult.getResponse().getContentAsString();

            Assertions.assertThat(responseAsString).isEqualTo("Success");
        }
    }
}
