package ru.yandex.market.mdm.app.controller;

import java.util.List;
import java.util.Random;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.util.LinkedMultiValueMap;

import ru.yandex.market.mbo.mdm.common.masterdata.model.msku.CommonMsku;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.MskuParamValue;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.param.MskuRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownMdmParams;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.MdmParamCache;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.TestMdmParamUtils;
import ru.yandex.market.mbo.mdm.common.utils.MdmBaseDbTestClass;

public class MskuParamValueControllerTest extends MdmBaseDbTestClass {
    @Autowired
    private MdmParamCache mdmParamCache;
    @Autowired
    private MskuRepository mskuRepository;

    private MockMvc mockMvc;
    private Random random;

    @Before
    public void setUp() {
        random = new Random(1280);
        MskuParamValueController mskuParamValueController = new MskuParamValueController(mskuRepository);
        mockMvc = MockMvcBuilders.standaloneSetup(mskuParamValueController).build();
    }

    @Test
    public void mskuParams() throws Exception {
        MskuParamValue record = record();
        mskuRepository.insertOrUpdateMsku(new CommonMsku(record.getMskuId(), List.of(record)));

        MockHttpServletRequestBuilder getRequest = MockMvcRequestBuilders.get("/mdm-api/msku-param-value/params")
                .accept(MediaType.APPLICATION_JSON_UTF8).param("mskuId", String.valueOf(record.getMskuId()));

        mockMvc.perform(getRequest)
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].mdmParamId").value(record.getMdmParamId()))
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].mskuId").value(record.getMskuId()))
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].xslName").value(record.getXslName()))
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].strings").value(record.getStrings()))
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].numerics").isEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].bools").isEmpty());
    }

    @Test
    public void mskuParamsByParamId() throws Exception {
        MskuParamValue record = record();
        mskuRepository.insertOrUpdateMsku(new CommonMsku(record.getMskuId(), List.of(record)));

        var params = new LinkedMultiValueMap<String, String>();
        params.add("mskuId", String.valueOf(record.getMskuId()));
        params.add("paramId", String.valueOf(record.getMdmParamId()));

        MockHttpServletRequestBuilder getRequest = MockMvcRequestBuilders.get(
                "/mdm-api/msku-param-value/paramsByParamId/").accept(MediaType.APPLICATION_JSON_UTF8).params(params);

        mockMvc.perform(getRequest)
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].mdmParamId").value(record.getMdmParamId()))
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].mskuId").value(record.getMskuId()))
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].xslName").value(record.getXslName()))
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].strings").value(record.getStrings()))
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].numerics").isEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].bools").isEmpty());
    }

    private MskuParamValue record() {
        MskuParamValue record = new MskuParamValue().setMskuId(random.nextInt());
        TestMdmParamUtils.createRandomMdmParamValue(random, mdmParamCache.get(KnownMdmParams.CUSTOMS_COMM_CODE_PREFIX))
            .copyTo(record);
        return record;
    }
}
