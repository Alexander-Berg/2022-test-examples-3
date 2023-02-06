package ru.yandex.market.wms.api.controller;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import ru.yandex.market.wms.common.spring.IntegrationTest;
import ru.yandex.market.wms.common.spring.utils.JsonAssertUtils;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.wms.common.spring.utils.FileContentUtils.getFileContent;

public class ItemMasterControllerTest extends IntegrationTest {

    @Test
    @DatabaseSetup("/item-master/before.xml")
    @ExpectedDatabase(value = "/item-master/before.xml", assertionMode = NON_STRICT)
    public void getItemBatchWithoutSkuIncorrectRequest() throws Exception {
        mockMvc.perform(post("/ENTERPRISE/item-master/itembatch")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("item-master/request/empty-request.json")))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DatabaseSetup("/item-master/before.xml")
    @ExpectedDatabase(value = "/item-master/before.xml", assertionMode = NON_STRICT)
    public void getItemBatchWithIncorrectDBRequest() throws Exception {
        MvcResult mvcResult = mockMvc.perform(post("/BLAT/item-master/itembatch")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("item-master/request/insert-sku-with-pack-and-alt.json")))
                .andExpect(status().isInternalServerError())
                .andReturn();
        assertions.assertThat(mvcResult.getResponse().getContentAsString())
                .contains("Incorrect scheme type");
    }

    @Test
    @DatabaseSetup("/item-master/before.xml")
    @ExpectedDatabase(value = "/item-master/before.xml", assertionMode = NON_STRICT)
    public void getItemBatchWithoutStorers() throws Exception {
        MvcResult mvcResult = mockMvc.perform(post("/INFOR_ENTERPRISE/item-master/itembatch")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("item-master/request/insert-sku-with-pack-and-alt.json")))
                .andExpect(status().isInternalServerError())
                .andReturn();
        assertions.assertThat(mvcResult.getResponse().getContentAsString())
                .contains("Storer not valid.");
    }

    @Test
    @DatabaseSetup("/item-master/before/before-with-storer.xml")
    @ExpectedDatabase(value = "/item-master/before/before-with-storer.xml", assertionMode = NON_STRICT)
    public void tryInsertNotIntoEnterprise() throws Exception {
        MvcResult mvcResult = mockMvc.perform(post("/INFOR_WMWHSE1/item-master/itembatch")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("item-master/request/insert-sku-with-pack-and-alt.json")))
                .andExpect(status().isInternalServerError())
                .andReturn();
        assertions.assertThat(mvcResult.getResponse().getContentAsString())
                .contains("Insert for a warehouse schema not allowed use enterprise only");
    }

    @Test
    @DatabaseSetup(value = "/item-master/before/before-with-storer.xml", connection = "wmwhseConnection")
    @DatabaseSetup(value = "/item-master/before/before-with-storer.xml", connection = "enterpriseConnection")
    @ExpectedDatabase(value = "/item-master/before/before-with-storer.xml", assertionMode = NON_STRICT)
    public void tryPostIncorrectPackKey() throws Exception {
        MvcResult mvcResult = mockMvc.perform(post("/INFOR_ENTERPRISE/item-master/itembatch")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("item-master/request/incorrect-pack-key.json")))
                .andExpect(status().isInternalServerError())
                .andReturn();
        assertions.assertThat(mvcResult.getResponse().getContentAsString())
                .contains("PackKey is not valid");
    }

    @Test
    @DatabaseSetup(value = "/item-master/before/before-with-storer.xml", connection = "wmwhseConnection")
    @DatabaseSetup(value = "/item-master/before/before-with-storer.xml", connection = "enterpriseConnection")
    @ExpectedDatabase(value = "/item-master/after/after-insert-sku-pack-altsku.xml",
            connection = "wmwhseConnection", assertionMode = NON_STRICT)
    @ExpectedDatabase(value = "/item-master/after/after-insert-sku-pack.xml",
            connection = "enterpriseConnection", assertionMode = NON_STRICT)
    public void insertSkuPackAndAltWithOverlengthAltSku() throws Exception {
        assertPostSkuSuccess("item-master/request/insert-sku-with-pack-and-overlength-alt.json",
                "item-master/response/created-sku.json");
    }

    @Test
    @DatabaseSetup(value = "/item-master/before/before-with-storer.xml", connection = "wmwhseConnection")
    @DatabaseSetup(value = "/item-master/before/before-with-storer.xml", connection = "enterpriseConnection")
    @ExpectedDatabase(value = "/item-master/after/after-insert-sku-pack-altsku.xml",
            connection = "wmwhseConnection", assertionMode = NON_STRICT)
    @ExpectedDatabase(value = "/item-master/after/after-insert-sku-pack.xml",
            connection = "enterpriseConnection", assertionMode = NON_STRICT)
    public void insertSkuPackAndAlt() throws Exception {
        assertPostSkuSuccess("item-master/request/insert-sku-with-pack-and-alt.json",
                "item-master/response/created-sku.json");
    }

    @Test
    @DatabaseSetup(value = "/item-master/after/after-insert-sku-pack-altsku.xml", connection = "wmwhseConnection")
    @DatabaseSetup(value = "/item-master/after/after-insert-sku-pack-altsku.xml", connection = "enterpriseConnection")
    @ExpectedDatabase(value = "/item-master/after/after-update-sku-pack-altsku.xml",
            connection = "wmwhseConnection", assertionMode = NON_STRICT)
    @ExpectedDatabase(value = "/item-master/after/after-update-sku-pack.xml",
            connection = "enterpriseConnection", assertionMode = NON_STRICT)
    public void updateSkuPackAndAlt() throws Exception {
        assertPostSkuSuccess("item-master/request/update-sku-with-pack-and-alt.json",
                "item-master/response/updated-sku.json");
    }

    /**
     * Если индикатор применимости СГ был Y и не изменился - то флаг manualsetuprequired не выставляется
     */
    @Test
    @DatabaseSetup(value = "/item-master/before/before-update-sku-pack-no-manualsetuprequired.xml",
            connection = "wmwhseConnection")
    @DatabaseSetup(value = "/item-master/before/before-update-sku-pack-no-manualsetuprequired.xml",
            connection = "enterpriseConnection")
    @ExpectedDatabase(value = "/item-master/after/after-update-sku-pack-no-manualsetuprequired.xml",
            connection = "wmwhseConnection", assertionMode = NON_STRICT)
    @ExpectedDatabase(value = "/item-master/after/after-update-sku-pack-no-manualsetuprequired.xml",
            connection = "enterpriseConnection", assertionMode = NON_STRICT)
    public void updateSkuPackWithoutManualSetupRequired() throws Exception {
        assertPostSkuSuccess("item-master/request/update-sku-pack-no-manualsetuprequired.json",
                "item-master/response/update-sku-pack-no-manualsetuprequired.json");
    }

    @Test
    @DatabaseSetup(value = "/item-master/after/after-update-sku-pack-altsku.xml", connection = "wmwhseConnection")
    @DatabaseSetup(value = "/item-master/after/after-update-sku-pack-altsku.xml", connection = "enterpriseConnection")
    @ExpectedDatabase(value = "/item-master/after/after-update-sku-pack-altsku.xml",
            connection = "wmwhseConnection", assertionMode = NON_STRICT)
    @ExpectedDatabase(value = "/item-master/after/after-update-sku-pack.xml",
            connection = "enterpriseConnection", assertionMode = NON_STRICT)
    public void updateSkuWithoutPackButDbHasPackAndAlt() throws Exception {
        assertPostSkuSuccess("item-master/request/update-sku-without-but-has-pack-in-db.json",
                "item-master/response/updated-sku-without-pack-but-exist-into-db.json");
    }

    @Test
    @DatabaseSetup(value = "/item-master/before/before-update-sku-pack-altsku-exist-in-stock.xml",
            connection = "wmwhseConnection")
    @DatabaseSetup(value = "/item-master/before/before-update-sku-pack-altsku-exist-in-stock.xml",
            connection = "enterpriseConnection")
    @ExpectedDatabase(value = "/item-master/after/after-update-sku-pack-altsku-exist-in-stock.xml",
        connection = "wmwhseConnection", assertionMode = NON_STRICT)
    @ExpectedDatabase(value = "/item-master/after/after-update-sku-pack-altsku-exist-in-stock.xml",
        connection = "enterpriseConnection", assertionMode = NON_STRICT)
    public void updateSkuWhithShelflifeIndicatorAndSusr1WhenExistsOnStock() throws Exception {
        assertPostSkuSuccess("item-master/request/update-sku-with-pack-and-alt-exist-in-db.json",
            "item-master/response/updated-sku.json");
    }

    @Test
    @DatabaseSetup(value = "/item-master/before/before-update-sku-pack-altsku-not-exist-in-stock.xml",
            connection = "wmwhseConnection")
    @DatabaseSetup(value = "/item-master/before/before-update-sku-pack-altsku-not-exist-in-stock.xml",
            connection = "enterpriseConnection")
    @ExpectedDatabase(value = "/item-master/after/after-update-sku-pack-altsku-not-exist-in-stock.xml",
        connection = "wmwhseConnection", assertionMode = NON_STRICT)
    @ExpectedDatabase(value = "/item-master/after/after-update-sku-pack-altsku-not-exist-in-stock.xml",
        connection = "enterpriseConnection", assertionMode = NON_STRICT)
    public void updateSkuWhithShelflifeIndicatorAndSusr1WhenNotExistsOnStock() throws Exception {
        assertPostSkuSuccess("item-master/request/update-sku-with-pack-and-alt-not-exist-in-db.json",
            "item-master/response/updated-sku.json");
    }


    @Test
    @DatabaseSetup(value = "/item-master/before/before-update-sku-pack-altsku-exist-in-stock-with-susr4.xml",
            connection = "wmwhseConnection")
    @DatabaseSetup(value = "/item-master/before/before-update-sku-pack-altsku-exist-in-stock-with-susr4.xml",
            connection = "enterpriseConnection")
    @ExpectedDatabase(value = "/item-master/after/after-update-sku-pack-altsku-exist-in-stock-without-susr4.xml",
            connection = "wmwhseConnection", assertionMode = NON_STRICT)
    @ExpectedDatabase(value = "/item-master/after/after-update-sku-pack-altsku-exist-in-stock-without-susr4.xml",
            connection = "enterpriseConnection", assertionMode = NON_STRICT)
    public void updateSkuWhithShelflifeIndicatorAndSusr5WithoutSurs4ButSkuHasSurs4IntoDB() throws Exception {
        assertPostSkuSuccess("item-master/request/update-sku-without-surs4.json",
                "item-master/response/updated-sku.json");
    }

    @Test
    @DatabaseSetup(value = "/item-master/before/before-update-sku-pack-altsku-snmfg.xml",
            connection = "wmwhseConnection")
    @DatabaseSetup(value = "/item-master/before/before-update-sku-pack-altsku-snmfg.xml",
            connection = "enterpriseConnection")
    @ExpectedDatabase(value = "/item-master/after/after-update-sku-pack-altsku-snmfg.xml",
            connection = "wmwhseConnection", assertionMode = NON_STRICT)
    @ExpectedDatabase(value = "/item-master/after/after-update-sku-pack-altsku-snmfg.xml",
            connection = "enterpriseConnection", assertionMode = NON_STRICT)
    public void updateSkuWhithShelflifeIndicatorAndSnmfg() throws Exception {
        assertPostSkuSuccess("item-master/request/update-sku-pack-altsku-snmfg.json",
                "item-master/response/updated-sku.json");
    }


    @Test
    @DatabaseSetup(value = "/item-master/before/before-with-storer-and-validation-flags-true.xml",
            connection = "wmwhseConnection")
    @DatabaseSetup(value = "/item-master/before/before-with-storer-and-validation-flags-true.xml",
            connection = "enterpriseConnection")
    @ExpectedDatabase(value = "/item-master/after/after-insert-sku-pack-with-warn-dimensions.xml",
        connection = "wmwhseConnection", assertionMode = NON_STRICT)
    @ExpectedDatabase(value = "/item-master/after/after-insert-sku-pack-with-warn-dimensions.xml",
        connection = "enterpriseConnection", assertionMode = NON_STRICT)
    public void insertSkuPackAndAltWithWarnDimension() throws Exception {
        assertPostSkuSuccess("item-master/request/insert-sku-with-pack-and-alt-and-warn-dimension.json",
            "item-master/response/created-sku-with-warn-dimensions.json");
    }

    @Test
    @DatabaseSetup(value = "/item-master/before/before-with-storer-and-validation-flags-true.xml",
            connection = "wmwhseConnection")
    @DatabaseSetup(value = "/item-master/before/before-with-storer-and-validation-flags-true.xml",
            connection = "enterpriseConnection")
    @ExpectedDatabase(value = "/item-master/after/after-insert-sku-pack-with-crit-dimensions.xml",
        connection = "wmwhseConnection", assertionMode = NON_STRICT)
    @ExpectedDatabase(value = "/item-master/after/after-insert-sku-pack-with-crit-dimensions.xml",
        connection = "enterpriseConnection", assertionMode = NON_STRICT)
    public void insertSkuPackAndAltWithCritDimension() throws Exception {
        assertPostSkuSuccess("item-master/request/insert-sku-with-pack-and-alt-and-crit-dimension.json",
            "item-master/response/created-sku-with-crit-dimensions.json");
    }

    @Test
    @DatabaseSetup(value = "/item-master/before/before-update-needmeasurement-exists-sku.xml",
            connection = "wmwhseConnection")
    @DatabaseSetup(value = "/item-master/before/before-update-needmeasurement-exists-sku.xml",
            connection = "enterpriseConnection")
    @ExpectedDatabase(value = "/item-master/after/after-update-needmeasurement-exists-sku.xml",
        connection = "wmwhseConnection", assertionMode = NON_STRICT)
    @ExpectedDatabase(value = "/item-master/after/after-update-needmeasurement-exists-sku.xml",
        connection = "enterpriseConnection", assertionMode = NON_STRICT)
    public void updateNeedMeasurementCorrect() throws Exception {
        assertPostNeedMeasurementSuccess("item-master/request/sku-needmeasurement.json",
            "item-master/response/sku-needmeasurement.json");
    }

    @Test
    @DatabaseSetup(value = "/item-master/before/before-update-needmeasurement-exists-sku.xml",
            connection = "wmwhseConnection")
    @DatabaseSetup(value = "/item-master/before/before-update-needmeasurement-exists-sku.xml",
            connection = "enterpriseConnection")
    @ExpectedDatabase(value = "/item-master/after/after-update-needmeasurement-not-exists-sku.xml",
        connection = "wmwhseConnection", assertionMode = NON_STRICT)
    @ExpectedDatabase(value = "/item-master/after/after-update-needmeasurement-not-exists-sku.xml",
        connection = "enterpriseConnection", assertionMode = NON_STRICT)
    public void updateNeedMeasurementIncorrect() throws Exception {
        assertPostNeedMeasurementBadRequest("item-master/request/sku-needmeasurement-with-not-existing-sku.json",
            "item-master/response/sku-needmeasurement-with-not-existing-sku.json");
    }

    @Test
    @DatabaseSetup(value = "/item-master/before/before-with-storer-disabled-non-uit.xml",
            connection = "wmwhseConnection")
    @DatabaseSetup(value = "/item-master/before/before-with-storer-disabled-non-uit.xml",
            connection = "enterpriseConnection")
    @ExpectedDatabase(value = "/item-master/after/after-insert-sku-pack-disabled-non-uit.xml",
            connection = "wmwhseConnection", assertionMode = NON_STRICT)
    @ExpectedDatabase(value = "/item-master/after/after-insert-sku-pack-disabled-non-uit.xml",
            connection = "enterpriseConnection", assertionMode = NON_STRICT)
    public void insertSkuPackAndAltDisabledNonUit() throws Exception {
        assertPostSkuSuccess("item-master/request/insert-sku-with-pack-and-alt-non-uit.json",
                "item-master/response/created-sku-non-uit.json");
    }

    @Test
    @DatabaseSetup(value = "/item-master/before/before-with-storer-enabled-non-uit.xml",
            connection = "wmwhseConnection")
    @DatabaseSetup(value = "/item-master/before/before-with-storer-enabled-non-uit.xml",
            connection = "enterpriseConnection")
    @ExpectedDatabase(value = "/item-master/after/after-insert-sku-pack-enabled-non-uit.xml",
            connection = "wmwhseConnection", assertionMode = NON_STRICT)
    @ExpectedDatabase(value = "/item-master/after/after-insert-sku-pack-enabled-non-uit.xml",
            connection = "enterpriseConnection", assertionMode = NON_STRICT)
    public void insertSkuPackAndAltEnabledNonUit() throws Exception {
        assertPostSkuSuccess("item-master/request/insert-sku-with-pack-and-alt-non-uit.json",
                "item-master/response/created-sku-non-uit.json");
    }

    private void assertPostSkuSuccess(String requestFileName, String responseFileName) throws Exception {
        MvcResult mvcResult = mockMvc.perform(post("/INFOR_ENTERPRISE/item-master/itembatch")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent(requestFileName)))
                .andExpect(status().isOk())
                .andReturn();
        JsonAssertUtils.assertFileNonExtensibleEquals(responseFileName, mvcResult.getResponse().getContentAsString());
    }

    private void assertPostNeedMeasurementSuccess(String requestFileName, String responseFileName) throws Exception {
        MvcResult mvcResult = mockMvc.perform(post("/INFOR_ENTERPRISE/items/measure")
            .contentType(MediaType.APPLICATION_JSON)
            .content(getFileContent(requestFileName)))
            .andExpect(status().isOk())
            .andReturn();
        JsonAssertUtils.assertFileNonExtensibleEquals(responseFileName, mvcResult.getResponse().getContentAsString());
    }

    private void assertPostNeedMeasurementBadRequest(String requestFileName, String responseFileName) throws Exception {
        MvcResult mvcResult = mockMvc.perform(post("/INFOR_ENTERPRISE/items/measure")
            .contentType(MediaType.APPLICATION_JSON)
            .content(getFileContent(requestFileName)))
            .andExpect(status().isBadRequest()).andDo(print())
            .andReturn();
        JsonAssertUtils.assertFileEquals(responseFileName, mvcResult.getResponse().getContentAsString(),
                JSONCompareMode.STRICT_ORDER);
    }
}
