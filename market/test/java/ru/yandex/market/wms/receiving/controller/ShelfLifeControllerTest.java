package ru.yandex.market.wms.receiving.controller;

import java.nio.charset.StandardCharsets;

import javax.servlet.http.Cookie;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.MediaType;

import ru.yandex.market.mbo.http.MdmShelfLife;
import ru.yandex.market.mbo.http.MdmShelfLifeService;
import ru.yandex.market.wms.common.model.enums.AuthenticationParam;
import ru.yandex.market.wms.common.spring.utils.JsonAssertUtils;
import ru.yandex.market.wms.receiving.ReceivingIntegrationTest;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.wms.common.spring.utils.FileContentUtils.getFileContent;

@SpyBean(value = {
        MdmShelfLifeService.class,
})
public class ShelfLifeControllerTest extends ReceivingIntegrationTest {

    @Autowired
    private MdmShelfLifeService mdmShelfLifeService;

    @BeforeEach
    public void init() {
        super.init();
        MdmShelfLife.AllowToUpdateShelfLifeRequiredResponse response =
                MdmShelfLife.AllowToUpdateShelfLifeRequiredResponse.newBuilder()
                .setAllowToSetFalse(true)
                .setAllowToSetTrue(true)
                .build();

        Mockito.doReturn(response).when(mdmShelfLifeService).allowToUpdateShelfLifeRequiredFlag(any());
        Mockito
                .doReturn(MdmShelfLife.UpdateShelfLifeInfoResponse.getDefaultInstance())
                .when(mdmShelfLifeService).updateShelfLifeInfo(any());
    }

    @AfterEach
    public void after() {
        Mockito.reset(mdmShelfLifeService);
    }

    @Test
    @DatabaseSetup(value = "/controller/shelf-life/update/1/before.xml", connection = "wmwhseConnection")
    public void errorForAmbigousSku() throws Exception {
        String actualResult = mockMvc.perform(post("/shelf-life/get-sku-lot-data")
                .cookie(new Cookie(AuthenticationParam.USERNAME.getCode(), "TEST"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("controller/shelf-life/update/1/request.json")))
                .andExpect(status().is(400))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonAssertUtils.assertFileNonExtensibleEquals(
                "controller/shelf-life/update/1/response.json", actualResult);
    }

    @Test
    @DatabaseSetup(value = "/controller/shelf-life/update/1/before.xml", connection = "wmwhseConnection")
    public void unknownSku() throws Exception {
        String actualResult = mockMvc.perform(post("/shelf-life/get-sku-lot-data")
                .cookie(new Cookie(AuthenticationParam.USERNAME.getCode(), "TEST"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("controller/shelf-life/update/1/request-no-sku.json")))
                .andExpect(status().is(404))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonAssertUtils.assertFileNonExtensibleEquals(
                "controller/shelf-life/update/1/response-no-sku.json", actualResult);
    }

    @Test
    @DatabaseSetup(value = "/controller/shelf-life/update/2/before.xml", connection = "wmwhseConnection")
    public void getShelfLifeInfoForUpdateBySku() throws Exception {
        String actualResult = mockMvc.perform(post("/shelf-life/get-sku-lot-data")
                .cookie(new Cookie(AuthenticationParam.USERNAME.getCode(), "TEST"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("controller/shelf-life/update/2/request.json")))
                .andExpect(status().is(200))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonAssertUtils.assertFileNonExtensibleEquals(
                "controller/shelf-life/update/2/response.json", actualResult);
    }

    @Test
    @DatabaseSetup(value = "/controller/shelf-life/update/3/before.xml", connection = "wmwhseConnection")
    public void getShelfLifeInfoForUpdateByLot() throws Exception {
        String actualResult = mockMvc.perform(post("/shelf-life/get-sku-lot-data")
                .cookie(new Cookie(AuthenticationParam.USERNAME.getCode(), "TEST"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("controller/shelf-life/update/3/request.json")))
                .andExpect(status().is(200))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonAssertUtils.assertFileNonExtensibleEquals(
                "controller/shelf-life/update/3/response.json", actualResult);
    }

    @Test
    @DatabaseSetup(value = "/controller/shelf-life/update/4/before.xml", connection = "wmwhseConnection")
    @DatabaseSetup(value = "/controller/shelf-life/update/4/before-enterprise.xml",
            connection = "enterpriseConnection")
    @ExpectedDatabase(value = "/controller/shelf-life/update/4/after.xml",
            connection = "wmwhseConnection", assertionMode = NON_STRICT_UNORDERED)
    @ExpectedDatabase(value = "/controller/shelf-life/update/4/after-enterprise.xml",
            connection = "enterpriseConnection", assertionMode = NON_STRICT_UNORDERED)
    public void lotChangeShouldNotAffectOtherLotsForGivenSku() throws Exception {
        mockMvc.perform(post("/shelf-life/update-lot-shelflife")
                .cookie(new Cookie(AuthenticationParam.USERNAME.getCode(), "TEST"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("controller/shelf-life/update/4/request.json")))
                .andExpect(status().is(200))
                .andReturn()
                .getResponse()
                .getContentAsString();
    }

    @Test
    @DatabaseSetup(value = "/controller/shelf-life/update/5/before.xml", connection = "wmwhseConnection")
    @DatabaseSetup(value = "/controller/shelf-life/update/5/before-enterprise.xml",
            connection = "enterpriseConnection")
    @ExpectedDatabase(value = "/controller/shelf-life/update/5/after.xml",
            connection = "wmwhseConnection", assertionMode = NON_STRICT_UNORDERED)
    @ExpectedDatabase(value = "/controller/shelf-life/update/5/after-enterprise.xml",
            connection = "enterpriseConnection", assertionMode = NON_STRICT_UNORDERED)
    public void correctRotateByAndShelfLifeCodeTypeForSNEXPDATE() throws Exception {
        mockMvc.perform(post("/shelf-life/update-sku-shelflife")
                .cookie(new Cookie(AuthenticationParam.USERNAME.getCode(), "TEST"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("controller/shelf-life/update/5/request.json")))
                .andExpect(status().is(200))
                .andReturn()
                .getResponse()
                .getContentAsString();
    }

    @Test
    @DatabaseSetup(value = "/controller/shelf-life/update/6/before.xml", connection = "wmwhseConnection")
    @DatabaseSetup(value = "/controller/shelf-life/update/6/before-enterprise.xml",
            connection = "enterpriseConnection")
    @ExpectedDatabase(value = "/controller/shelf-life/update/6/after.xml",
            connection = "wmwhseConnection", assertionMode = NON_STRICT_UNORDERED)
    @ExpectedDatabase(value = "/controller/shelf-life/update/6/after-enterprise.xml",
            connection = "enterpriseConnection", assertionMode = NON_STRICT_UNORDERED)
    public void correctRotateByAndShelfLifeCodeTypeForSNMFG() throws Exception {
        mockMvc.perform(post("/shelf-life/update-sku-shelflife")
                .cookie(new Cookie(AuthenticationParam.USERNAME.getCode(), "TEST"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("controller/shelf-life/update/6/request.json")))
                .andExpect(status().is(200))
                .andReturn()
                .getResponse()
                .getContentAsString();
    }

    @Test
    @DatabaseSetup(value = "/controller/shelf-life/update/7/before.xml", connection = "wmwhseConnection")
    @DatabaseSetup(value = "/controller/shelf-life/update/7/before-enterprise.xml",
            connection = "enterpriseConnection")
    @ExpectedDatabase(value = "/controller/shelf-life/update/7/after.xml",
            connection = "wmwhseConnection", assertionMode = NON_STRICT_UNORDERED)
    @ExpectedDatabase(value = "/controller/shelf-life/update/7/after-enterprise.xml",
            connection = "enterpriseConnection", assertionMode = NON_STRICT_UNORDERED)
    public void badRequestShouldBeRespondedForIllegalTemplateSN() throws Exception {
        mockMvc.perform(post("/shelf-life/update-sku-shelflife")
                .cookie(new Cookie(AuthenticationParam.USERNAME.getCode(), "TEST"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("controller/shelf-life/update/7/request.json")))
                .andExpect(status().is(400))
                .andReturn()
                .getResponse()
                .getContentAsString();
    }

    @Test
    @DatabaseSetup(value = "/controller/shelf-life/update/8/before.xml", connection = "wmwhseConnection")
    public void getHistoryTestEventOrderMatters() throws Exception {
        String actualResult = mockMvc.perform(post("/shelf-life/history")
                .cookie(new Cookie(AuthenticationParam.USERNAME.getCode(), "TEST"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("controller/shelf-life/update/8/request.json")))
                .andExpect(status().is(200))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonAssertUtils.assertFileNonExtensibleEquals(
                "controller/shelf-life/update/8/response.json", actualResult,
                JSONCompareMode.STRICT);
    }

    @Test
    @DatabaseSetup(value = "/controller/shelf-life/update/9/before.xml", connection = "wmwhseConnection")
    @DatabaseSetup(value = "/controller/shelf-life/update/9/before-enterprise.xml",
            connection = "enterpriseConnection")
    @ExpectedDatabase(value = "/controller/shelf-life/update/9/after.xml",
            connection = "wmwhseConnection", assertionMode = NON_STRICT_UNORDERED)
    @ExpectedDatabase(value = "/controller/shelf-life/update/9/after-enterprise.xml",
            connection = "enterpriseConnection", assertionMode = NON_STRICT_UNORDERED)
    public void dropShelfLifeControl() throws Exception {
        mockMvc.perform(post("/shelf-life/update-sku-shelflife")
                .cookie(new Cookie(AuthenticationParam.USERNAME.getCode(), "TEST"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("controller/shelf-life/update/9/request.json")))
                .andExpect(status().is(200))
                .andReturn()
                .getResponse()
                .getContentAsString();
    }

    @Test
    @DatabaseSetup(value = "/controller/shelf-life/update/10/before.xml", connection = "wmwhseConnection")
    @DatabaseSetup(value = "/controller/shelf-life/update/10/before-enterprise.xml",
            connection = "enterpriseConnection")
    @ExpectedDatabase(value = "/controller/shelf-life/update/10/after.xml",
            connection = "wmwhseConnection", assertionMode = NON_STRICT_UNORDERED)
    @ExpectedDatabase(value = "/controller/shelf-life/update/10/after-enterprise.xml",
            connection = "enterpriseConnection", assertionMode = NON_STRICT_UNORDERED)
    public void manualSetupRequiredShouldSaveShelfLifeControlAndTemplate() throws Exception {
       mockMvc.perform(post("/shelf-life/update-sku-shelflife")
                .cookie(new Cookie(AuthenticationParam.USERNAME.getCode(), "TEST"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("controller/shelf-life/update/10/request.json")))
                .andExpect(status().is(200))
                .andReturn()
                .getResponse()
                .getContentAsString();
    }


    @Test
    @DatabaseSetup(value = "/controller/shelf-life/update/11/before.xml", connection = "wmwhseConnection")
    @DatabaseSetup(value = "/controller/shelf-life/update/11/before-enterprise.xml",
            connection = "enterpriseConnection")
    @ExpectedDatabase(value = "/controller/shelf-life/update/11/after.xml",
            connection = "wmwhseConnection", assertionMode = NON_STRICT_UNORDERED)
    @ExpectedDatabase(value = "/controller/shelf-life/update/11/after-enterprise.xml",
            connection = "enterpriseConnection", assertionMode = NON_STRICT_UNORDERED)
    public void lotInfoShouldBeChangedIfNoShelflifeControlSetForSku() throws Exception {
        mockMvc.perform(post("/shelf-life/update-lot-shelflife")
                .cookie(new Cookie(AuthenticationParam.USERNAME.getCode(), "TEST"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("controller/shelf-life/update/11/request.json")))
                .andExpect(status().is(200))
                .andReturn()
                .getResponse()
                .getContentAsString();
    }

    @Test
    @DatabaseSetup(value = "/controller/shelf-life/update/15/before.xml", connection = "wmwhseConnection")
    @DatabaseSetup(value = "/controller/shelf-life/update/15/before-enterprise.xml",
            connection = "enterpriseConnection")
    @ExpectedDatabase(value = "/controller/shelf-life/update/15/after.xml",
            connection = "wmwhseConnection", assertionMode = NON_STRICT_UNORDERED)
    @ExpectedDatabase(value = "/controller/shelf-life/update/15/after-enterprise.xml",
            connection = "enterpriseConnection", assertionMode = NON_STRICT_UNORDERED)
    public void lotInfoShouldNotBeChangedIfNoShelflifeControlSetForSku() throws Exception {
        mockMvc.perform(post("/shelf-life/update-lot-shelflife")
                .cookie(new Cookie(AuthenticationParam.USERNAME.getCode(), "TEST"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("controller/shelf-life/update/15/request.json")))
                .andExpect(status().is(400))
                .andReturn()
                .getResponse()
                .getContentAsString();
    }

    @Test
    @DatabaseSetup(value = "/controller/shelf-life/update/12/before.xml", connection = "wmwhseConnection")
    @DatabaseSetup(value = "/controller/shelf-life/update/12/before-enterprise.xml",
            connection = "enterpriseConnection")
    @ExpectedDatabase(value = "/controller/shelf-life/update/12/after.xml",
            connection = "wmwhseConnection", assertionMode = NON_STRICT_UNORDERED)
    @ExpectedDatabase(value = "/controller/shelf-life/update/12/after-enterprise.xml",
            connection = "enterpriseConnection", assertionMode = NON_STRICT_UNORDERED)
    public void shelflifeAndShelflifeOnReceivingShouldBeRecalculatedIfToExpireDaysChanged() throws Exception {
        mockMvc.perform(post("/shelf-life/update-sku-shelflife")
                .cookie(new Cookie(AuthenticationParam.USERNAME.getCode(), "TEST"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("controller/shelf-life/update/12/request.json")))
                .andExpect(status().is(200))
                .andReturn()
                .getResponse()
                .getContentAsString();
    }

    @Test
    @DatabaseSetup(value = "/controller/shelf-life/update/13/before.xml", connection = "wmwhseConnection")
    @DatabaseSetup(value = "/controller/shelf-life/update/13/before-enterprise.xml",
            connection = "enterpriseConnection")
    @ExpectedDatabase(value = "/controller/shelf-life/update/13/after.xml",
            connection = "wmwhseConnection", assertionMode = NON_STRICT_UNORDERED)
    @ExpectedDatabase(value = "/controller/shelf-life/update/13/after-enterprise.xml",
            connection = "enterpriseConnection", assertionMode = NON_STRICT_UNORDERED)
    public void zeroToExpireDaysShouldNotBeAcceptedForTemplatesOtherThanEXPIRATION_DATE_ONLY() throws Exception {
        String actualResult = mockMvc.perform(post("/shelf-life/update-sku-shelflife")
                .cookie(new Cookie(AuthenticationParam.USERNAME.getCode(), "TEST"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("controller/shelf-life/update/13/request.json")))
                .andExpect(status().is(400))
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);
        assertEquals(getFileContent("controller/shelf-life/update/13/response.json"), actualResult);
    }

    @Test
    @DatabaseSetup(value = "/controller/shelf-life/update/14/before.xml", connection = "wmwhseConnection")
    @DatabaseSetup(value = "/controller/shelf-life/update/14/before-enterprise.xml",
            connection = "enterpriseConnection")
    @ExpectedDatabase(value = "/controller/shelf-life/update/14/after.xml",
            connection = "wmwhseConnection", assertionMode = NON_STRICT_UNORDERED)
    @ExpectedDatabase(value = "/controller/shelf-life/update/14/after-enterprise.xml",
            connection = "enterpriseConnection", assertionMode = NON_STRICT_UNORDERED)
    public void zeroToExpireDaysShouldBeAcceptedForTemplateEXPIRATION_DATE_ONLY() throws Exception {
        mockMvc.perform(post("/shelf-life/update-sku-shelflife")
                .cookie(new Cookie(AuthenticationParam.USERNAME.getCode(), "TEST"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("controller/shelf-life/update/14/request.json")))
                .andExpect(status().is(200))
                .andReturn();
    }
}
