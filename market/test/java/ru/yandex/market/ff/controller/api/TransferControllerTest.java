package ru.yandex.market.ff.controller.api;

import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.DatabaseSetups;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.ResultMatcher;

import ru.yandex.market.ff.base.MvcIntegrationTest;
import ru.yandex.market.ff.client.dto.CreateTransferForm;
import ru.yandex.market.ff.client.dto.CreateTransferItemForm;
import ru.yandex.market.ff.client.dto.RegistryUnitIdDTO;
import ru.yandex.market.ff.client.dto.RegistryUnitPartialIdDTO;
import ru.yandex.market.ff.client.enums.RegistryUnitIdType;
import ru.yandex.market.ff.client.enums.StockType;
import ru.yandex.market.ff.client.enums.TransferCreationType;
import ru.yandex.market.ff.enums.FileExtension;
import ru.yandex.market.ff.model.bo.TransferFilter;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT;
import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static java.lang.ClassLoader.getSystemResourceAsStream;
import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.ff.util.FileContentUtils.getFileContent;

/**
 * Функциональный тест для {@link TransferController}.
 */
class TransferControllerTest extends MvcIntegrationTest {


    private static final long VALID_INBOUND_ID = 4;
    private static final long INVALID_INBOUND_ID = 0;

    private static final long VALID_SUPPLIER_ID = 1;
    private static final long INVALID_SUPPLIER_ID = 3;

    private static final long VALID_SERVICE_ID = 100;
    private static final long INVALID_SERVICE_ID = 200;

    private static final String ALIEN_TRANSFER_NOT_FOUND_ERROR =
        "{\"message\":\"Failed to find [REQUEST] with id [12]\",\"resourceType\":\"REQUEST\",\"identifier\":\"12\"}";

    private static final String INBOUND_NOT_FOUND_ERROR =
        "{\"message\":\"Failed to find [REQUEST] with id [0]\",\"resourceType\":\"REQUEST\",\"identifier\":\"0\"}";

    private static final String SUPPLIER_NOT_FOUND_ERROR =
        "{\"message\":\"Failed to find [SUPPLIER] with id [3]\",\"resourceType\":\"SUPPLIER\",\"identifier\":\"3\"}";

    private static final String SERVICE_NOT_FOUND_ERROR =
        "{\"message\":\"Failed to find [FULFILLMENT_INFO] with id [200]\"," +
            "\"resourceType\":\"FULFILLMENT_INFO\",\"identifier\":\"200\"}";

    private static final String INBOUND_ID_SPECIFIED_FOR_UTILIZATION_TRANSFER =
            "{\"message\":\"Inbound id should not be specified for utilization transfer\"," +
                    "\"type\":\"INBOUND_ID_SPECIFIED_FOR_UTILIZATION_TRANSFER\"}";

    private static final String INCORRECT_TRANSFER_TYPE_ERROR =
            "{\"message\":\"Only transfers could be created using this endpoint\"}";

    /**
     * Проверяет работу метода {@link TransferController#getTransfers(TransferFilter, Pageable, Long)}
     * Пагинация по элементарному набору атрибутов
     */
    @Test
    @DatabaseSetup("classpath:controller/transfer-api/transfers.xml")
    @ExpectedDatabase(value = "classpath:controller/transfer-api/transfers.xml", assertionMode = NON_STRICT_UNORDERED)
    void findTransfers() throws Exception {
        mockMvc.perform(
            get("/transfer")
                .param("statuses", "1", "3")
                .param("stockTypeTo", "3")
                .param("page", "0")
                .param("size", "2")
                .param("sort", "id,asc")
        ).andDo(print())
            .andExpect(status().isOk())
            .andExpect(content().json(getFileContent("controller/transfer-api/get_transfers_with_filter.json")));
    }

    /**
     * Проверяет работу метода {@link TransferController#create(CreateTransferForm)}
     */
    @Test
    @DatabaseSetup("classpath:controller/transfer-api/before-transfers-create.xml")
    @ExpectedDatabase(value = "classpath:controller/transfer-api/after-transfers-create.xml",
        assertionMode = NON_STRICT_UNORDERED)
    void createTransfer() throws Exception {
        CreateTransferForm form = new CreateTransferForm();
        form.setExternalOperationId("10923");
        form.setInboundId(VALID_INBOUND_ID);
        form.setServiceId(100L);
        form.setStockTypeFrom(StockType.FIT);
        form.setStockTypeTo(StockType.SURPLUS);
        form.setSupplierId(VALID_SUPPLIER_ID);

        CreateTransferItemForm itemForm1 = new CreateTransferItemForm("a1", 1, null);
        CreateTransferItemForm itemForm2 = new CreateTransferItemForm("a2", 12, null);

        form.setItems(Arrays.asList(itemForm1, itemForm2));
        String content = toJson(form);
        mockMvc.perform(
            post("/transfer/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content(content)
        ).andDo(print())
            .andExpect(status().isOk())
            .andExpect(content().json(getFileContent("controller/transfer-api/create_transfer.json")));
    }

    /**
     * Проверяет работу метода {@link TransferController#create(CreateTransferForm)}
     */
    @Test
    @DatabaseSetup("classpath:controller/transfer-api/before-transfers-create.xml")
    @ExpectedDatabase(value = "classpath:controller/transfer-api/after-transfers-create-with-cis.xml",
        assertionMode = NON_STRICT_UNORDERED)
    void createTransferForCis() throws Exception {
        CreateTransferForm form = new CreateTransferForm();
        form.setExternalOperationId("10923");
        form.setServiceId(100L);
        form.setStockTypeFrom(StockType.CIS_QUARANTINE);
        form.setStockTypeTo(StockType.FIT);
        form.setSupplierId(VALID_SUPPLIER_ID);
        form.setTransferCreationType(TransferCreationType.CIS_TRANSFER);

        CreateTransferItemForm itemForm1 = new CreateTransferItemForm("a1", 1, null);
        CreateTransferItemForm itemForm2 = new CreateTransferItemForm("a2", 12,
                new RegistryUnitIdDTO(Set.of(
                        new RegistryUnitPartialIdDTO(RegistryUnitIdType.CIS, "123451"),
                        new RegistryUnitPartialIdDTO(RegistryUnitIdType.CIS, "123452"))));

        form.setItems(Arrays.asList(itemForm1, itemForm2));
        mockMvc.perform(
            post("/transfer/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(form))
        ).andDo(print())
            .andExpect(status().isOk())
            .andExpect(content().json(getFileContent("controller/transfer-api/create_transfer_with_cis.json")));
    }

    @Test
    @DatabaseSetup("classpath:controller/transfer-api/before-cis-transfers-create.xml")
    @ExpectedDatabase(value = "classpath:controller/transfer-api/after-cis-transfers-create.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void createTransferForCisToDefect() throws Exception {
        CreateTransferForm form = new CreateTransferForm();
        form.setExternalOperationId("10923");
        form.setServiceId(1L);
        form.setStockTypeFrom(StockType.CIS_QUARANTINE);
        form.setStockTypeTo(StockType.DEFECT);
        form.setSupplierId(VALID_SUPPLIER_ID);
        form.setInboundId(0L);
        form.setTransferCreationType(TransferCreationType.CIS_TRANSFER);

        CreateTransferItemForm itemForm1 = new CreateTransferItemForm("sku1", 12,
                new RegistryUnitIdDTO(Set.of(
                        new RegistryUnitPartialIdDTO(RegistryUnitIdType.CIS, "cis1"))));

        form.setItems(List.of(itemForm1));
        mockMvc.perform(
                post("/transfer/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(form))
        ).andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().json(getFileContent(
                        "controller/transfer-api/create_transfer_with_cis_to_defect.json")));
    }

    @Test
    @DatabaseSetup("classpath:controller/transfer-api/before-transfers-create.xml")
    @ExpectedDatabase(value = "classpath:controller/transfer-api/after-transfers-for-util-create.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void createTransferForUtilization() throws Exception {
        CreateTransferForm form = new CreateTransferForm();
        form.setExternalOperationId("10923");
        form.setServiceId(100L);
        form.setStockTypeFrom(StockType.DEFECT);
        form.setStockTypeTo(StockType.PLAN_UTILIZATION);
        form.setSupplierId(VALID_SUPPLIER_ID);
        form.setTransferCreationType(TransferCreationType.BY_UTILIZER);

        CreateTransferItemForm itemForm1 = new CreateTransferItemForm("a1", 1, null);
        CreateTransferItemForm itemForm2 = new CreateTransferItemForm("a2", 12, null);

        form.setItems(Arrays.asList(itemForm1, itemForm2));
        mockMvc.perform(
                post("/transfer/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(form))
        ).andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().json(getFileContent("controller/transfer-api/create_transfer_for_util.json")));
    }

    @Test
    @DatabaseSetup("classpath:controller/transfer-api/before-transfers-create.xml")
    @ExpectedDatabase(value = "classpath:controller/transfer-api/after-transfers-for-util-create-with-type.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void createTransferForUtilizationWithType() throws Exception {
        createTransferWithType(1203, status().isOk(),
                getFileContent("controller/transfer-api/create_transfer_for_util.json"));
    }

    @Test
    @DatabaseSetup("classpath:controller/transfer-api/before-transfers-create.xml")
    @ExpectedDatabase(value = "classpath:controller/transfer-api/before-transfers-create.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void createTransferForUtilizationWithIncorrectType() throws Exception {
        createTransferWithType(1, status().isBadRequest(), INCORRECT_TRANSFER_TYPE_ERROR);
    }

    private void createTransferWithType(int type, ResultMatcher resultMatcher, String expectedContent)
            throws Exception {
        CreateTransferForm form = new CreateTransferForm();
        form.setExternalOperationId("10923");
        form.setServiceId(100L);
        form.setStockTypeFrom(StockType.DEFECT);
        form.setStockTypeTo(StockType.PLAN_UTILIZATION);
        form.setSupplierId(VALID_SUPPLIER_ID);
        form.setTransferCreationType(TransferCreationType.BY_UTILIZER);
        form.setType(type);

        CreateTransferItemForm itemForm1 = new CreateTransferItemForm("a1", 1, null);
        CreateTransferItemForm itemForm2 = new CreateTransferItemForm("a2", 12, null);

        form.setItems(Arrays.asList(itemForm1, itemForm2));
        mockMvc.perform(
                post("/transfer/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(form))
        ).andExpect(resultMatcher)
                .andExpect(content().json(expectedContent));
    }

    @Test
    @DatabaseSetup("classpath:controller/transfer-api/before-transfers-create.xml")
    @ExpectedDatabase(value = "classpath:controller/transfer-api/after-transfers-for-util-create.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void createTransferForUtilizationWithDuplicatedItems() throws Exception {
        CreateTransferForm form = new CreateTransferForm();
        form.setExternalOperationId("10923");
        form.setServiceId(100L);
        form.setStockTypeFrom(StockType.DEFECT);
        form.setStockTypeTo(StockType.PLAN_UTILIZATION);
        form.setSupplierId(VALID_SUPPLIER_ID);
        form.setTransferCreationType(TransferCreationType.BY_UTILIZER);

        CreateTransferItemForm itemForm1 = new CreateTransferItemForm("a1", 1, null);
        CreateTransferItemForm itemForm2 = new CreateTransferItemForm("a2", 9, null);
        CreateTransferItemForm itemForm3 = new CreateTransferItemForm("a2", 3, null);

        form.setItems(Arrays.asList(itemForm1, itemForm2, itemForm3));
        mockMvc.perform(
                post("/transfer/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(form))
        ).andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().json(getFileContent("controller/transfer-api/create_transfer_for_util.json")));
    }

    /**
     * Проверяет работу метода {@link TransferController#create(CreateTransferForm)} для несуществующей поставки
     */
    @Test
    @DatabaseSetup("classpath:controller/transfer-api/before-transfers-create.xml")
    void createTransferWithWrongInbound() throws Exception {
        CreateTransferForm form = new CreateTransferForm();
        form.setExternalOperationId("10923");
        form.setInboundId(INVALID_INBOUND_ID);
        form.setServiceId(VALID_SERVICE_ID);
        form.setStockTypeFrom(StockType.FIT);
        form.setStockTypeTo(StockType.SURPLUS);
        form.setSupplierId(VALID_SUPPLIER_ID);

        mockMvc.perform(
            post("/transfer/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(form))
        ).andDo(print())
            .andExpect(status().isNotFound())
            .andExpect(content().json(INBOUND_NOT_FOUND_ERROR));
    }

    @Test
    @DatabaseSetup("classpath:controller/transfer-api/before-transfers-create.xml")
    void createTransferForUtilizationWithInbound() throws Exception {
        CreateTransferForm form = new CreateTransferForm();
        form.setExternalOperationId("10923");
        form.setInboundId(INVALID_INBOUND_ID);
        form.setServiceId(VALID_SERVICE_ID);
        form.setStockTypeFrom(StockType.EXPIRED);
        form.setStockTypeTo(StockType.PLAN_UTILIZATION);
        form.setSupplierId(VALID_SUPPLIER_ID);
        form.setTransferCreationType(TransferCreationType.BY_UTILIZER);

        mockMvc.perform(
                post("/transfer/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(form))
        ).andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(content().json(INBOUND_ID_SPECIFIED_FOR_UTILIZATION_TRANSFER));
    }

    /**
     * Проверяет работу метода {@link TransferController#create(CreateTransferForm)} для несуществующего поставщика
     */
    @Test
    @DatabaseSetup("classpath:controller/transfer-api/before-transfers-create.xml")
    void createTransferWithWrongSupplier() throws Exception {
        CreateTransferForm form = new CreateTransferForm();
        form.setExternalOperationId("10923");
        form.setInboundId(VALID_INBOUND_ID);
        form.setServiceId(VALID_SERVICE_ID);
        form.setStockTypeFrom(StockType.FIT);
        form.setStockTypeTo(StockType.SURPLUS);
        form.setSupplierId(INVALID_SUPPLIER_ID);

        mockMvc.perform(
            post("/transfer/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(form))
        ).andDo(print())
            .andExpect(status().isNotFound())
            .andExpect(content().json(SUPPLIER_NOT_FOUND_ERROR));
    }

    /**
     * Проверяет работу метода {@link TransferController#create(CreateTransferForm)} для несуществующего склада
     */
    @Test
    @DatabaseSetup("classpath:controller/transfer-api/before-transfers-create.xml")
    void createTransferWithWrongService() throws Exception {
        CreateTransferForm form = new CreateTransferForm();
        form.setExternalOperationId("10923");
        form.setInboundId(VALID_INBOUND_ID);
        form.setServiceId(INVALID_SERVICE_ID);
        form.setStockTypeFrom(StockType.FIT);
        form.setStockTypeTo(StockType.SURPLUS);
        form.setSupplierId(VALID_SUPPLIER_ID);

        mockMvc.perform(
            post("/transfer/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(form))
        ).andDo(print())
            .andExpect(status().isNotFound())
            .andExpect(content().json(SERVICE_NOT_FOUND_ERROR));
    }

    @Test
    @DatabaseSetups({
            @DatabaseSetup("classpath:service/tanker/all-tanker-keys.xml"),
            @DatabaseSetup("classpath:controller/transfer-api/before-transfers-create.xml")
    })
    @ExpectedDatabase(value = "classpath:controller/transfer-api/after-transfers-create.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void createTransferByFile() throws Exception {
        mockMvc.perform(
                multipart("/transfer/create-by-file")
                        .file(getFile("request.xlsx"))
                        .param("externalOperationId", "10923")
                        .param("inboundId", String.valueOf(VALID_INBOUND_ID))
                        .param("serviceId", String.valueOf(VALID_SERVICE_ID))
                        .param("stockTypeFrom", "0")
                        .param("stockTypeTo", "3")
                        .param("supplierId", String.valueOf(VALID_SUPPLIER_ID))
        ).andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().json(getFileContent("controller/transfer-api/create_transfer.json")));
    }

    @Test
    @DatabaseSetups({
            @DatabaseSetup("classpath:service/tanker/all-tanker-keys.xml"),
            @DatabaseSetup("classpath:controller/transfer-api/before-transfers-create.xml")
    })
    @ExpectedDatabase(value = "classpath:controller/transfer-api/after-invalid-file-transfer-create.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void createTransferByInvalidFile() throws Exception {
        Mockito.when(mdsS3Client.getUrl(any())).thenReturn(new URL("http://localhost:8080/file"));
        mockMvc.perform(
                multipart("/transfer/create-by-file")
                        .file(getFile("request_with_invalid_file.xlsx"))
                        .param("externalOperationId", "10923")
                        .param("inboundId", String.valueOf(VALID_INBOUND_ID))
                        .param("serviceId", String.valueOf(VALID_SERVICE_ID))
                        .param("stockTypeFrom", "0")
                        .param("stockTypeTo", "3")
                        .param("supplierId", String.valueOf(VALID_SUPPLIER_ID))
        ).andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(content().json(
                        getFileContent("controller/transfer-api/create_transfer_invalid_file.json")));
    }

    @Test
    @DatabaseSetups({
            @DatabaseSetup("classpath:service/tanker/all-tanker-keys.xml"),
            @DatabaseSetup("classpath:controller/transfer-api/before-transfers-create.xml")
    })
    @ExpectedDatabase(value = "classpath:controller/transfer-api/after-transfers-for-util-create-by-file.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void createTransferForUtilizationByFile() throws Exception {
        createTransferForUtilizationByFile("request.xlsx", "1103");
    }

    @Test
    @DatabaseSetups({
            @DatabaseSetup("classpath:service/tanker/all-tanker-keys.xml"),
            @DatabaseSetup("classpath:controller/transfer-api/before-transfers-create.xml")
    })
    @ExpectedDatabase(value = "classpath:controller/transfer-api/after-transfers-for-util-create-by-file-with-type.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void createTransferForUtilizationByFileWithType() throws Exception {
        createTransferForUtilizationByFileWithType(1103, "request.xlsx", status().isOk(),
                getFileContent("controller/transfer-api/create_transfer_for_util_by_file.json"));
    }

    @Test
    @DatabaseSetups({
            @DatabaseSetup("classpath:service/tanker/all-tanker-keys.xml"),
            @DatabaseSetup("classpath:controller/transfer-api/before-transfers-create.xml")
    })
    @ExpectedDatabase(value = "classpath:controller/transfer-api/before-transfers-create.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void createTransferForUtilizationByFileWithIncorrectType() throws Exception {
        createTransferForUtilizationByFileWithType(1, "request.xlsx", status().isBadRequest(),
                INCORRECT_TRANSFER_TYPE_ERROR);
    }

    @Test
    @DatabaseSetups({
            @DatabaseSetup("classpath:service/tanker/all-tanker-keys.xml"),
            @DatabaseSetup("classpath:controller/transfer-api/before-transfers-create.xml")
    })
    @ExpectedDatabase(value = "classpath:controller/transfer-api/after-transfers-for-util-create-by-file.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void createTransferForUtilizationByFileWithDuplicates() throws Exception {
        createTransferForUtilizationByFile("request_with_duplicates.xlsx", "1103");
    }

    private void createTransferForUtilizationByFile(String fileName, String subTypeId) throws Exception {
        mockMvc.perform(
                multipart("/transfer/create-by-file")
                        .file(getFile(fileName))
                        .param("externalOperationId", "10923")
                        .param("serviceId", String.valueOf(VALID_SERVICE_ID))
                        .param("stockTypeFrom", "2")
                        .param("stockTypeTo", "4")
                        .param("supplierId", String.valueOf(VALID_SUPPLIER_ID))
                        .param("transferCreationType", "BY_SUPPLIER")
                        .param("type", subTypeId)
        ).andDo(print())
                .andExpect(status().isOk())
                .andExpect(content()
                        .json(getFileContent("controller/transfer-api/create_transfer_for_util_by_file.json")));
    }

    private void createTransferForUtilizationByFileWithType(int type, String fileName, ResultMatcher resultMatcher,
                                                            String expectedContent)
            throws Exception {
        mockMvc.perform(
                multipart("/transfer/create-by-file")
                        .file(getFile(fileName))
                        .param("externalOperationId", "10923")
                        .param("serviceId", String.valueOf(VALID_SERVICE_ID))
                        .param("stockTypeFrom", "2")
                        .param("stockTypeTo", "4")
                        .param("supplierId", String.valueOf(VALID_SUPPLIER_ID))
                        .param("transferCreationType", "BY_SUPPLIER")
                        .param("type", String.valueOf(type))
        ).andExpect(resultMatcher)
                .andExpect(content()
                        .json(expectedContent));
    }

    /**
     * Проверяет работу метода {@link TransferController#create(CreateTransferForm)} для несуществующей поставки
     */
    @Test
    @DatabaseSetup("classpath:controller/transfer-api/before-transfers-create.xml")
    void createTransferByFileWithWrongInbound() throws Exception {
        mockMvc.perform(
                multipart("/transfer/create-by-file")
                        .file(getFile("request.xlsx"))
                        .param("externalOperationId", "10923")
                        .param("inboundId", String.valueOf(INVALID_INBOUND_ID))
                        .param("serviceId", String.valueOf(VALID_SERVICE_ID))
                        .param("stockTypeFrom", "0")
                        .param("stockTypeTo", "3")
                        .param("supplierId", String.valueOf(VALID_SUPPLIER_ID))
        ).andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(content().json(INBOUND_NOT_FOUND_ERROR));
    }

    @Test
    @DatabaseSetup("classpath:controller/transfer-api/before-transfers-create.xml")
    void createTransferForUtilizationByFileWithInbound() throws Exception {
        mockMvc.perform(
                multipart("/transfer/create-by-file")
                        .file(getFile("request.xlsx"))
                        .param("externalOperationId", "10923")
                        .param("inboundId", String.valueOf(INVALID_INBOUND_ID))
                        .param("serviceId", String.valueOf(VALID_SERVICE_ID))
                        .param("stockTypeFrom", "1")
                        .param("stockTypeTo", "4")
                        .param("transferCreationType", TransferCreationType.BY_UTILIZER.name())
                        .param("supplierId", String.valueOf(VALID_SUPPLIER_ID))
        ).andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(content().json(INBOUND_ID_SPECIFIED_FOR_UTILIZATION_TRANSFER));
    }

    /**
     * Проверяет работу метода {@link TransferController#create(CreateTransferForm)} для несуществующего поставщика
     */
    @Test
    @DatabaseSetup("classpath:controller/transfer-api/before-transfers-create.xml")
    void createTransferWithWrongSupplierByFile() throws Exception {
        mockMvc.perform(
                multipart("/transfer/create-by-file")
                        .file(getFile("request.xlsx"))
                        .param("externalOperationId", "10923")
                        .param("inboundId", String.valueOf(VALID_INBOUND_ID))
                        .param("serviceId", String.valueOf(VALID_SERVICE_ID))
                        .param("stockTypeFrom", "0")
                        .param("stockTypeTo", "3")
                        .param("supplierId", String.valueOf(INVALID_SUPPLIER_ID))
        ).andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(content().json(SUPPLIER_NOT_FOUND_ERROR));
    }

    /**
     * Проверяет работу метода {@link TransferController#create(CreateTransferForm)} для несуществующего склада
     */
    @Test
    @DatabaseSetup("classpath:controller/transfer-api/before-transfers-create.xml")
    void createTransferWithWrongServiceByFile() throws Exception {
        mockMvc.perform(
                multipart("/transfer/create-by-file")
                        .file(getFile("request.xlsx"))
                        .param("externalOperationId", "10923")
                        .param("inboundId", String.valueOf(VALID_INBOUND_ID))
                        .param("serviceId", String.valueOf(INVALID_SERVICE_ID))
                        .param("stockTypeFrom", "0")
                        .param("stockTypeTo", "3")
                        .param("supplierId", String.valueOf(VALID_SUPPLIER_ID))
        ).andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(content().json(SERVICE_NOT_FOUND_ERROR));
    }

    /**
     * Проверяет работу метода {@link TransferController#getTransfers(TransferFilter, Pageable, Long)}
     * Пагинация по набору ключевых атрибутов с shopIds и transferIds
     */
    @Test
    @DatabaseSetup("classpath:controller/transfer-api/transfers.xml")
    @ExpectedDatabase(value = "classpath:controller/transfer-api/transfers.xml", assertionMode = NON_STRICT_UNORDERED)
    void findTransfersKeyParams() throws Exception {
        mockMvc.perform(
            get("/transfer")
                .param("statuses", "0", "1", "3")
                .param("stockTypeTo", "3")
                .param("shopIds", "1", "2")
                .param("page", "0")
                .param("size", "2")
                .param("sort", "id,asc")
                .param("stockTypeFrom", "0")
                .param("transferIds", "1", "2", "3")
        ).andDo(print())
            .andExpect(status().isOk())
            .andExpect(content().json(getFileContent("controller/transfer-api/get_transfers_with_filter_2.json")));
    }


    /**
     * Проверяет работу метода {@link TransferController#getTransferDetails(long)}
     * Ожидается получение перемещения
     */
    @Test
    @DatabaseSetup("classpath:controller/transfer-api/transfers.xml")
    @ExpectedDatabase(value = "classpath:controller/transfer-api/transfers.xml", assertionMode = NON_STRICT)
    void getTransfer() throws Exception {
        mockMvc.perform(
            get("/transfer/2")
        ).andDo(print())
            .andExpect(status().isOk())
            .andExpect(content().json(getFileContent("controller/transfer-api/get_transfer_by_id.json")));
    }

    /**
     * Проверяет работу метода {@link TransferController#getTransferDetails(long)}
     * Ожидается ошибка
     */
    @Test
    @DatabaseSetup("classpath:controller/transfer-api/transfers.xml")
    @ExpectedDatabase(value = "classpath:controller/transfer-api/transfers.xml", assertionMode = NON_STRICT_UNORDERED)
    void getAlienTransfer() throws Exception {
        mockMvc.perform(
            get("/transfer/12")
        )
            .andExpect(status().isNotFound())
            .andDo(print())
            .andExpect(content().json(ALIEN_TRANSFER_NOT_FOUND_ERROR))
            .andReturn();
    }

    /**
     * Проверяет работу метода {@link TransferController#getTransferHistory(long)}
     * Ожидается получение истории перемещения
     */
    @Test
    @DatabaseSetup("classpath:controller/transfer-api/transfers.xml")
    @ExpectedDatabase(value = "classpath:controller/transfer-api/transfers.xml", assertionMode = NON_STRICT_UNORDERED)
    void getTransferHistory() throws Exception {
        mockMvc.perform(
            get("/transfer/2/history")
        ).andDo(print())
            .andExpect(status().isOk())
            .andExpect(content().json(getFileContent("controller/transfer-api/get_transfer_history.json")));
    }

    /**
     * Проверяет работу метода {@link TransferController#getTransferHistory(long)}
     * Список перемещений к кизам
     */
    @Test
    @DatabaseSetup("classpath:controller/transfer-api/transfer-with-cises.xml")
    void getTransferCises() throws Exception {
        mockMvc.perform(
                get("/transfer/by-cis")
                        .param("supplierId", "10")
                        .param("inboundId", "1")
                        .param("cises", "CIS1", "CIS2", "1231232")
                        .param("ssku", "111")
        ).andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().json(getFileContent("controller/transfer-api/get_transfers_by_cises.json")));
    }

    /**
     * Проверяет работу метода {@link TransferController#getTransferHistory(long)}
     * Ожидается ошибка
     */
    @Test
    @DatabaseSetup("classpath:controller/transfer-api/transfers.xml")
    @ExpectedDatabase(value = "classpath:controller/transfer-api/transfers.xml", assertionMode = NON_STRICT_UNORDERED)
    void getAlienTransferHistory() throws Exception {
        mockMvc.perform(
            get("/transfer/12/history")
        )
            .andExpect(status().isNotFound())
            .andDo(print())
            .andExpect(content().json(ALIEN_TRANSFER_NOT_FOUND_ERROR));
    }

    private MockMultipartFile getFile(String fileName) throws IOException {
        return new MockMultipartFile("file", fileName, FileExtension.XLSX.getMimeType(),
                getSystemResourceAsStream("controller/transfer-api/" + fileName));
    }

}
