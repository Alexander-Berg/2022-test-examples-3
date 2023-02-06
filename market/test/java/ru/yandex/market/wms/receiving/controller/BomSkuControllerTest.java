package ru.yandex.market.wms.receiving.controller;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import ru.yandex.market.wms.common.spring.utils.columnFilters.PrimaryKeyColumnFilter;
import ru.yandex.market.wms.common.spring.utils.columnFilters.ReceiptLineNumberFilter;
import ru.yandex.market.wms.receiving.ReceivingIntegrationTest;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.wms.common.spring.utils.FileContentUtils.getFileContent;
import static ru.yandex.market.wms.common.spring.utils.JsonAssertUtils.assertFileNonExtensibleEquals;

public class BomSkuControllerTest extends ReceivingIntegrationTest {

    @Test
    @DatabaseSetup(value = "/controller/bom-sku-controller/6/create-new-bom-sku-before.xml",
            connection = "wmwhseConnection")
    @ExpectedDatabase(value = "/controller/bom-sku-controller/6/create-new-bom-sku-after.xml",
            connection = "wmwhseConnection", assertionMode = NON_STRICT_UNORDERED)
    public void createBomNotInOrderReturns() throws Exception {
        String actualResult = mockMvc.perform(post("/bom/create-not-in-order")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent(
                        "controller/bom-sku-controller/6/create-new-bom-sku-request.json")))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        assertFileNonExtensibleEquals(
                "controller/bom-sku-controller/6/create-new-bom-sku-response.json",
                actualResult,
                ImmutableList.of("[*].id", "[*].receiptDetailSerialKey")
        );
    }

    /**
     * Стандартная приёмка, сценарий 1:
     * бомок нет, создаём первую
     */
    @Test
    @DatabaseSetup(value = "/controller/bom-sku-controller/default-type-flow/common.xml")
    @DatabaseSetup(value = "/controller/bom-sku-controller/default-type-flow/before-no-boms.xml",
            connection = "wmwhseConnection", type = DatabaseOperation.INSERT)
    @ExpectedDatabase(value = "/controller/bom-sku-controller/default-type-flow/after-no-boms.xml",
            connection = "wmwhseConnection", assertionMode = NON_STRICT_UNORDERED,
            columnFilters = {ReceiptLineNumberFilter.class, PrimaryKeyColumnFilter.class})
    public void createFirstBomDefault() throws Exception {
        String actualResult = mockMvc.perform(post("/bom/create-not-in-order")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent(
                        "controller/bom-sku-controller/default-type-flow/request/create-first-bom.json")))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        assertFileNonExtensibleEquals(
                "controller/bom-sku-controller/default-type-flow/response/create-first-bom.json",
                actualResult,
                ImmutableList.of("[*].id", "[*].receiptDetailSerialKey")
        );
    }


    /**
     * Стандартная приёмка, сценарий 2:
     * одна бомка уже есть, создаём вторую
     */
    @Test
    @DatabaseSetup(value = "/controller/bom-sku-controller/default-type-flow/common.xml")
    @DatabaseSetup(value = "/controller/bom-sku-controller/default-type-flow/before-with-free-bom.xml",
            connection = "wmwhseConnection", type = DatabaseOperation.INSERT)
    @ExpectedDatabase(value = "/controller/bom-sku-controller/default-type-flow/after-with-free-bom.xml",
            connection = "wmwhseConnection", assertionMode = NON_STRICT_UNORDERED,
            columnFilters = {ReceiptLineNumberFilter.class, PrimaryKeyColumnFilter.class})
    public void createSecondBomDefault() throws Exception {
        String actualResult = mockMvc.perform(post("/bom/create-not-in-order")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent(
                        "controller/bom-sku-controller/default-type-flow/request/create-second-bom.json")))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        assertFileNonExtensibleEquals(
                "controller/bom-sku-controller/default-type-flow/response/create-second-bom.json",
                actualResult,
                ImmutableList.of("[*].id", "[*].receiptDetailSerialKey")
        );
    }

    /**
     * Стандартная приёмка, сценарий 3:
     * одна бомка уже есть, создаём вторую с тем же именем, но получаем конфликт по имени
     */
    @Test
    @DatabaseSetup(value = "/controller/bom-sku-controller/default-type-flow/common.xml")
    @DatabaseSetup(value = "/controller/bom-sku-controller/default-type-flow/before-with-free-bom.xml",
            connection = "wmwhseConnection", type = DatabaseOperation.INSERT)
    @ExpectedDatabase(value = "/controller/bom-sku-controller/default-type-flow/after-no-boms.xml",
            connection = "wmwhseConnection", assertionMode = NON_STRICT_UNORDERED,
            columnFilters = {ReceiptLineNumberFilter.class, PrimaryKeyColumnFilter.class})
    public void createSecondBomDefaultNameConflict() throws Exception {
        String actualResult = mockMvc.perform(post("/bom/create-not-in-order")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent(
                        "controller/bom-sku-controller/default-type-flow/request/create-first-bom.json")))
                .andExpect(status().is4xxClientError())
                .andReturn()
                .getResponse()
                .getContentAsString();
        assertFileNonExtensibleEquals(
                "controller/bom-sku-controller/default-type-flow/response/bom-name-conflict.json",
                actualResult,
                ImmutableList.of("[*].id", "[*].receiptDetailSerialKey")
        );
    }

    /**
     * Стандартная приёмка, сценарий 4:
     * две бомки из двух уже есть, создаём третью, но получаем конфликт по лимиту
     */
    @Test
    @DatabaseSetup(value = "/controller/bom-sku-controller/default-type-flow/common.xml")
    @DatabaseSetup(value = "/controller/bom-sku-controller/default-type-flow/before-with-full-boms.xml",
            connection = "wmwhseConnection", type = DatabaseOperation.REFRESH)
    @ExpectedDatabase(value = "/controller/bom-sku-controller/default-type-flow/before-with-full-boms.xml",
            connection = "wmwhseConnection", assertionMode = NON_STRICT_UNORDERED,
            columnFilters = {ReceiptLineNumberFilter.class, PrimaryKeyColumnFilter.class})
    public void createThirdBomDefaultSUSRConflict() throws Exception {
        String actualResult = mockMvc.perform(post("/bom/create-not-in-order")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent(
                        "controller/bom-sku-controller/default-type-flow/request/create-third-bom.json")))
                .andExpect(status().isBadRequest())
                .andReturn()
                .getResponse()
                .getContentAsString();
        assertFileNonExtensibleEquals(
                "controller/bom-sku-controller/default-type-flow/response/susr-conflict.json",
                actualResult,
                ImmutableList.of("[*].id", "[*].receiptDetailSerialKey")
        );
    }


    /**
     * Стандартная приёмка, полный список бомок по мастеру
     */
    @Test
    @DatabaseSetup(value = "/controller/bom-sku-controller/default-type-flow/common.xml",
            connection = "wmwhseConnection")
    @DatabaseSetup(value = "/controller/bom-sku-controller/default-type-flow/before-with-full-boms.xml",
            connection = "wmwhseConnection", type = DatabaseOperation.REFRESH)
    @ExpectedDatabase(value = "/controller/bom-sku-controller/default-type-flow/before-with-full-boms.xml",
            connection = "wmwhseConnection", assertionMode = NON_STRICT_UNORDERED,
            columnFilters = {ReceiptLineNumberFilter.class, PrimaryKeyColumnFilter.class})
    public void getAllBomsByMasterTest() throws Exception {
        String actualResult = mockMvc.perform(post("/bom/get-boms-for-master")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent(
                        "controller/bom-sku-controller/default-type-flow/request/get-by-master.json")))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        assertFileNonExtensibleEquals(
                "controller/bom-sku-controller/default-type-flow/response/get-by-master.json",
                actualResult,
                ImmutableList.of("[*].id", "[*].receiptDetailSerialKey")
        );
    }

    /**
     * Стандартная приёмка, выбор уже имеющейся бомки c уже созданными receiptDetail
     */
    @Test
    @DatabaseSetup(value = "/controller/bom-sku-controller/default-type-flow/common.xml",
            connection = "wmwhseConnection")
    @DatabaseSetup(value = "/controller/bom-sku-controller/default-type-flow/select-created-receiptdetail.xml",
            connection = "wmwhseConnection", type = DatabaseOperation.REFRESH)
    @ExpectedDatabase(value = "/controller/bom-sku-controller/default-type-flow/select-created-receiptdetail.xml",
            connection = "wmwhseConnection", assertionMode = NON_STRICT_UNORDERED,
            columnFilters = {ReceiptLineNumberFilter.class, PrimaryKeyColumnFilter.class})
    public void selectBomWithReceiptDetail() throws Exception {
        String actualResult = mockMvc.perform(post("/bom/get-receipt-sku-for-bom")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent(
                        "controller/bom-sku-controller/default-type-flow/request/select-created-bom.json")))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        assertFileNonExtensibleEquals(
                "controller/bom-sku-controller/default-type-flow/response/select-created-bom.json",
                actualResult,
                ImmutableList.of("[*].id", "[*].receiptDetailSerialKey")
        );
    }

    /**
     * Стандартная приёмка, выбор уже имеющейся бомки c ещё не созданным receiptDetail
     */
    @Test
    @DatabaseSetup(value = "/controller/bom-sku-controller/default-type-flow/common.xml",
            connection = "wmwhseConnection")
    @DatabaseSetup(value = "/controller/bom-sku-controller/default-type-flow/select-not-created-receiptdetail.xml",
            connection = "wmwhseConnection", type = DatabaseOperation.REFRESH)
    @ExpectedDatabase(value = "/controller/bom-sku-controller/default-type-flow/select-created-receiptdetail.xml",
            connection = "wmwhseConnection", assertionMode = NON_STRICT_UNORDERED,
            columnFilters = {ReceiptLineNumberFilter.class, PrimaryKeyColumnFilter.class})
    public void selectBomWithoutReceiptDetail() throws Exception {
        String actualResult = mockMvc.perform(post("/bom/get-receipt-sku-for-bom")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent(
                        "controller/bom-sku-controller/default-type-flow/request/select-created-bom.json")))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        assertFileNonExtensibleEquals(
                "controller/bom-sku-controller/default-type-flow/response/select-created-bom.json",
                actualResult,
                ImmutableList.of("[*].id", "[*].receiptDetailSerialKey")
        );
    }

    @Test
    @DisplayName("Получение детали BOM, деталь существует")
    @DatabaseSetup(value = "/controller/bom-sku-controller/returns/select-created-receiptdetail.xml",
            connection = "wmwhseConnection")
    @ExpectedDatabase(value = "/controller/bom-sku-controller/returns/select-created-receiptdetail.xml",
            connection = "wmwhseConnection", assertionMode = NON_STRICT_UNORDERED)
    public void selectBomWithReceiptDetailReturns() throws Exception {
        String actualResult = mockMvc.perform(post("/bom/get-receipt-sku-for-bom")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent(
                        "controller/bom-sku-controller/returns/request/select-created-bom.json")))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        assertFileNonExtensibleEquals(
                "controller/bom-sku-controller/returns/response/select-created-bom.json",
                actualResult,
                ImmutableList.of("[*].id", "[*].receiptDetailSerialKey")
        );
    }

    @Test
    @DisplayName("Получение детали BOM, новая деталь создается")
    @DatabaseSetup(value = "/controller/bom-sku-controller/returns/select-not-created-receiptdetail.xml",
            connection = "wmwhseConnection")
    @ExpectedDatabase(value = "/controller/bom-sku-controller/returns/select-created-receiptdetail.xml",
            connection = "wmwhseConnection", assertionMode = NON_STRICT_UNORDERED)
    public void selectBomWithoutReceiptDetailReturns() throws Exception {
        String actualResult = mockMvc.perform(post("/bom/get-receipt-sku-for-bom")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent(
                        "controller/bom-sku-controller/returns/request/select-created-bom.json")))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        assertFileNonExtensibleEquals(
                "controller/bom-sku-controller/returns/response/select-created-bom.json",
                actualResult,
                ImmutableList.of("[*].id", "[*].receiptDetailSerialKey")
        );
    }
}
