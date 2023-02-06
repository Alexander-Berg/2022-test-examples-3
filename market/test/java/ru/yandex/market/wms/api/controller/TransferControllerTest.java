package ru.yandex.market.wms.api.controller;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import lombok.SneakyThrows;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import ru.yandex.market.wms.common.spring.IntegrationTest;
import ru.yandex.market.wms.common.spring.utils.JsonAssertUtils;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.wms.common.spring.utils.FileContentUtils.getFileContent;
import static ru.yandex.market.wms.common.spring.utils.JsonAssertUtils.assertFileEquals;

@DatabaseSetup("/transfer/db/before.xml")
class TransferControllerTest extends IntegrationTest {

    @Test
    @DatabaseSetup("/transfer/db/completed-transfers.xml")
    void getTransfers_emptyListTransferKeys() throws Exception {
        mockMvc.perform(post("/ENTERPRISE/transfer/getTransfers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("transfer/request/empty-list.json")))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DatabaseSetup("/transfer/db/completed-transfers.xml")
    void getTransfers_nullTransferKey() throws Exception {
        mockMvc.perform(post("/ENTERPRISE/transfer/getTransfers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("transfer/request/blank-key.json")))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DatabaseSetup("/transfer/db/completed-transfers.xml")
    void getTransfers() throws Exception {
        MvcResult mvcResult = mockMvc.perform(post("/ENTERPRISE/transfer/getTransfers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("transfer/request/transfer-keys.json")))
                .andExpect(status().isOk())
                .andReturn();
        JsonAssertUtils.assertFileNonExtensibleEquals("transfer/response/transfer-keys.json",
                mvcResult.getResponse().getContentAsString());
    }

    @Test
    @DatabaseSetup("/transfer/db/completed-transfers.xml")
    void getTransferDetails() throws Exception {
        MvcResult mvcResult = mockMvc.perform(post("/ENTERPRISE/transfer/getTransferDetails")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("transfer/request/transfer-key-1.json")))
                .andExpect(status().isOk())
                .andReturn();
        JsonAssertUtils.assertFileNonExtensibleEquals("transfer/response/transfer-key-1.json",
                mvcResult.getResponse().getContentAsString());
    }

    @Test
    @DatabaseSetup("/transfer/db/completed-transfers.xml")
    void getTransferHistory() throws Exception {
        MvcResult mvcResult = mockMvc.perform(post("/ENTERPRISE/transfer/getTransferHistory")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("transfer/request/transfer-key-2.json")))
                .andExpect(status().isOk())
                .andReturn();
        JsonAssertUtils.assertFileNonExtensibleEquals("transfer/response/transfer-key-2.json",
                mvcResult.getResponse().getContentAsString());
    }

    @Test
    @ExpectedDatabase(value = "/transfer/db/transfer.xml", connection = "wmwhseConnection",
            assertionMode = NON_STRICT_UNORDERED)
    void createTransferUtilizationOfEmptyBalance() throws Exception {
        MvcResult mvcResult = mockMvc.perform(post("/ENTERPRISE/transfer/createTransfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent("transfer/request/transfer-utilization.json")))
                .andExpect(status().isOk())
                .andReturn();
        assertFileEquals("transfer/response/created-transfer.json",
                mvcResult.getResponse().getContentAsString(), JSONCompareMode.LENIENT);
    }

    @Test
    @DatabaseSetup("/transfer/db/cis-quar.xml")
    @ExpectedDatabase(value = "/transfer/db/transfer-cis_full-item.xml", connection = "wmwhseConnection",
            assertionMode = NON_STRICT_UNORDERED)
    void createTransferCisQuarFound() throws Exception {
        MvcResult mvcResult = mockMvc.perform(post("/ENTERPRISE/transfer/createTransfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent("transfer/request/transfer-cis.json")))
                .andExpect(status().isOk())
                .andReturn();
        assertFileEquals("transfer/response/created-transfer-found-cis.json",
                mvcResult.getResponse().getContentAsString(), JSONCompareMode.LENIENT);
    }

    @Test
    @ExpectedDatabase(value = "/transfer/db/transfer-cis.xml", connection = "wmwhseConnection",
            assertionMode = NON_STRICT_UNORDERED)
    void createTransferAbsentCIS() throws Exception {
        MvcResult mvcResult = mockMvc.perform(post("/ENTERPRISE/transfer/createTransfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent("transfer/request/transfer-cis.json")))
                .andExpect(status().isOk())
                .andReturn();
        assertFileEquals("transfer/response/created-transfer-cis.json",
                mvcResult.getResponse().getContentAsString(), JSONCompareMode.LENIENT);
    }

    @Test
    @DisplayName("Создать трансфер")
    @DatabaseSetup(value = "/transfer/create/simple/no-transfers.xml", connection = "wmwhseConnection")
    @ExpectedDatabase(value = "/transfer/create/simple/new-transfer.xml", connection = "wmwhseConnection",
            assertionMode = NON_STRICT_UNORDERED)
    @SneakyThrows
    public void simpleTransferCreationTest() {
        assertHttpCall(post("/TENANT_NAME/transfer/createTransfer"),
                status().isOk(),
                "transfer/create/simple/request.json");
    }
}
