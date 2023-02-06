package ru.yandex.market.wms.receiving.controller;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import javax.servlet.http.Cookie;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;

import ru.yandex.market.wms.common.model.enums.AuthenticationParam;
import ru.yandex.market.wms.receiving.ReceivingIntegrationTest;
import ru.yandex.market.wms.shared.libs.label.printer.domain.pojo.PrintResult;
import ru.yandex.market.wms.shared.libs.label.printer.service.printer.PrintService;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.wms.common.spring.utils.FileContentUtils.getFileContent;

public class ContainerReceivingControllerTest extends ReceivingIntegrationTest {

    @MockBean
    @Autowired
    private PrintService printService;

    @BeforeEach
    public void init() {
        super.init();
        Mockito.reset(printService);
        Mockito.when(printService.print(anyString(), anyString()))
                .thenReturn(new PrintResult(HttpStatus.OK.toString(), null, null));
    }

    @Test
    @DatabaseSetup("/controller/pallet-receiving-controller/create-initial-receipt/before.xml")
    @ExpectedDatabase(value = "/controller/pallet-receiving-controller/create-initial-receipt/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void createInitialInboundReceiptDetailWithPalletId() throws Exception {
        assertSuccessfulRequest("create-initial-receipt",
                "/request/create-initial-inbound-receipt-detail-with-pallet-id.json");
        assertPrintedProperDate();
    }

    @Test
    @DatabaseSetup("/controller/pallet-receiving-controller/create-initial-receipt/before-wrong-receipt-type.xml")
    @ExpectedDatabase(value = "/controller/pallet-receiving-controller/create-initial-receipt/" +
            "before-wrong-receipt-type.xml", assertionMode = NON_STRICT_UNORDERED)
    public void createInitialInboundBadReceiptType() throws Exception {
        assertBadRequest("create-initial-receipt",
                "/request/create-initial-inbound-receipt-detail-with-pallet-id.json",
                status().is4xxClientError(),
                "INITIAL_RECEIVING_PROHIBITED");
    }

    @Test
    @DatabaseSetup("/controller/pallet-receiving-controller/create-initial-receipt/before-wrong-receipt-type.xml")
    @ExpectedDatabase(value = "/controller/pallet-receiving-controller/create-initial-receipt/" +
            "before-wrong-receipt-type.xml", assertionMode = NON_STRICT_UNORDERED)
    public void createInitialInboundAdditionalReceiptType() throws Exception {
        assertBadRequest("create-initial-receipt",
                "/request/create-initial-inbound-additional-receipt-type.json",
                status().is4xxClientError(),
                "INITIAL_RECEIVING_PROHIBITED");
    }

    @Test
    @DatabaseSetup("/controller/pallet-receiving-controller/create-initial-receipt/before-wrong-receipt-type.xml")
    @ExpectedDatabase(value = "/controller/pallet-receiving-controller/create-initial-receipt/" +
            "before-wrong-receipt-type.xml", assertionMode = NON_STRICT_UNORDERED)
    public void createInitialInboundEmptyPrinterTableName() throws Exception {
        assertBadRequest("create-initial-receipt",
                "/request/create-initial-inbound-empty-printer-tablename.json",
                status().is4xxClientError(),
                "INITIAL_RECEIVING_PROHIBITED");
    }

    @Test
    @DatabaseSetup("/controller/pallet-receiving-controller/create-initial-receipt/before-no-building.xml")
    @ExpectedDatabase(value = "/controller/pallet-receiving-controller/create-initial-receipt/after-no-building.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void createInitialInboundReceiptDetailWithPalletIdNoBuilding() throws Exception {
        assertSuccessfulRequest("create-initial-receipt",
                "/request/create-initial-inbound-receipt-detail-with-pallet-id.json");
        assertPrintedProperDate();
    }

    @Test
    @DatabaseSetup("/controller/pallet-receiving-controller/create-initial-receipt/before-no-loc.xml")
    @ExpectedDatabase(value = "/controller/pallet-receiving-controller/create-initial-receipt/after-no-loc.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void createInitialInboundReceiptDetailWithPalletIdNoLoc() throws Exception {
        assertSuccessfulRequest("create-initial-receipt",
                "/request/create-initial-inbound-receipt-detail-with-pallet-id.json");
        assertPrintedProperDate();
    }

    @Test
    @DatabaseSetup("/controller/pallet-receiving-controller/create-initial-receipt/before.xml")
    public void createInitialInboundReceiptDetailWithZeroPallets() throws Exception {
        assertBadRequest("create-initial-receipt",
                "/request/create-initial-inbound-with-zero-pallets.json",
                status().isBadRequest(),
                "INCORRECT_QUANTITY");
    }

    @Test
    @DatabaseSetup("/controller/pallet-receiving-controller/create-initial-receipt/before-cancelled.xml")
    @ExpectedDatabase(value = "/controller/pallet-receiving-controller/create-initial-receipt/before-cancelled.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void createInitialInboundReceiptDetailCancelled() throws Exception {
        assertBadRequest("create-initial-receipt",
                "/request/create-initial-inbound-cancelled.json",
                status().isBadRequest(),
                "UNDEFINED");
    }

    @Test
    @DatabaseSetup("/controller/pallet-receiving-controller/finish-initial-receiving/before-success.xml")
    @ExpectedDatabase(value = "/controller/pallet-receiving-controller/finish-initial-receiving/after-success.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void finishInitialReceivingWhenReceiptInPalletAcceptanceStatus() throws Exception {
        mockMvc.perform(post("/receipt-detail/finish-initial-receiving")
                .cookie(new Cookie(AuthenticationParam.USERNAME.getCode(), "TEST"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("controller/pallet-receiving-controller/finish-initial-receiving/" +
                        "request.json")))
                .andExpect(status().isOk());
    }

    @Test
    @DatabaseSetup("/controller/pallet-receiving-controller/finish-initial-receiving/before-fail.xml")
    @ExpectedDatabase(value = "/controller/pallet-receiving-controller/finish-initial-receiving/before-fail.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void finishInitialReceivingWhenReceiptNotInPalletAcceptanceStatus() throws Exception {
        MvcResult mvcResult = mockMvc.perform(post("/receipt-detail/finish-initial-receiving")
                .cookie(new Cookie(AuthenticationParam.USERNAME.getCode(), "TEST"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("controller/pallet-receiving-controller/finish-initial-receiving/" +
                        "request.json")))
                .andExpect(status().isBadRequest())
                .andReturn();

        Assertions.assertTrue(mvcResult.getResponse().getContentAsString().contains("RECEIPT_IN_WRONG_STATUS"));
    }

    @Test
    @DatabaseSetup("/controller/pallet-receiving-controller/receive-unknown-box/before.xml")
    @ExpectedDatabase(value = "/controller/pallet-receiving-controller/receive-unknown-box/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void receiveUnknownBoxTest() throws Exception {
        mockMvc.perform(post("/box/receive-unknown")
                .cookie(new Cookie(AuthenticationParam.USERNAME.getCode(), "TEST"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("controller/pallet-receiving-controller/receive-unknown-box/request.json")))
                .andExpect(status().isOk());
    }

    @Test
    @DatabaseSetup("/controller/pallet-receiving-controller/receive-unknown-box/before.xml")
    @ExpectedDatabase(value = "/controller/pallet-receiving-controller/receive-unknown-box/before.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void receiveKnownBoxAsUnknownTest() throws Exception {
        MvcResult mvcResult = mockMvc.perform(post("/box/receive-unknown")
                .cookie(new Cookie(AuthenticationParam.USERNAME.getCode(), "TEST"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("controller/pallet-receiving-controller/receive-unknown-box/" +
                        "request-invalid.json")))
                .andExpect(status().isBadRequest())
                .andReturn();

        Assertions.assertTrue(mvcResult.getResponse().getContentAsString().contains("BOX_ALREADY_DECLARED_IN_RECEIPT"));

    }

    @Test
    @DatabaseSetup("/controller/pallet-receiving-controller/receive-unknown-box/before-auto-additional-receipt.xml")
    @ExpectedDatabase(value = "/controller/pallet-receiving-controller/receive-unknown-box/" +
            "before-auto-additional-receipt.xml", assertionMode = NON_STRICT_UNORDERED)
    void tryToReceiveUnknownBoxInAutoAdditionalReceiptTest() throws Exception {
        MvcResult mvcResult = mockMvc.perform(post("/box/receive-unknown")
                .cookie(new Cookie(AuthenticationParam.USERNAME.getCode(), "TEST"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("controller/pallet-receiving-controller/receive-unknown-box/request.json")))
                .andExpect(status().isBadRequest())
                .andReturn();

        Assertions.assertTrue(mvcResult.getResponse().getContentAsString().contains("BOX_RECEIVING_AS_UNKNOWN_AGAIN"));
    }

    @Test
    @DatabaseSetup("/controller/pallet-receiving-controller/update-order-id/before.xml")
    @ExpectedDatabase(value = "/controller/pallet-receiving-controller/update-order-id/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void updateOrderIdTest() throws Exception {
        MvcResult mvcResult = mockMvc.perform(post("/box/update-order-id")
                        .cookie(new Cookie(AuthenticationParam.USERNAME.getCode(), "TEST"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent("controller/pallet-receiving-controller/update-order-id/" +
                                "request-valid.json")))
                .andExpect(status().isOk())
                .andReturn();
    }

    @Test
    @DatabaseSetup("/controller/pallet-receiving-controller/update-order-id/before-invalid-type.xml")
    @ExpectedDatabase(value = "/controller/pallet-receiving-controller/update-order-id/before-invalid-type.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void updateOrderIdInvalidTypeTest() throws Exception {
        MvcResult mvcResult = mockMvc.perform(post("/box/update-order-id")
                        .cookie(new Cookie(AuthenticationParam.USERNAME.getCode(), "TEST"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent("controller/pallet-receiving-controller/update-order-id/" +
                                "request-valid.json")))
                .andExpect(status().isBadRequest())
                .andReturn();

        Assertions.assertTrue(mvcResult.getResponse().getContentAsString().contains("UPDATABLE_CUSTOMER_RETURN"));
    }

    @Test
    @DatabaseSetup("/controller/pallet-receiving-controller/update-order-id/before-invalid-status.xml")
    @ExpectedDatabase(value = "/controller/pallet-receiving-controller/update-order-id/before-invalid-status.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void updateOrderIdInvalidStatusTest() throws Exception {
        MvcResult mvcResult = mockMvc.perform(post("/box/update-order-id")
                        .cookie(new Cookie(AuthenticationParam.USERNAME.getCode(), "TEST"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent("controller/pallet-receiving-controller/update-order-id/" +
                                "request-valid.json")))
                .andExpect(status().isBadRequest())
                .andReturn();

        Assertions.assertTrue(mvcResult.getResponse().getContentAsString().contains("RECEIPT_IN_WRONG_STATUS"));
    }

    @Test
    @DatabaseSetup("/controller/pallet-receiving-controller/update-order-id/before-invalid-sku.xml")
    @ExpectedDatabase(value = "/controller/pallet-receiving-controller/update-order-id/before-invalid-sku.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void updateOrderIdInvalidSkuTest() throws Exception {
        MvcResult mvcResult = mockMvc.perform(post("/box/update-order-id")
                .cookie(new Cookie(AuthenticationParam.USERNAME.getCode(), "TEST"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("controller/pallet-receiving-controller/update-order-id/" +
                        "request-valid.json")))
                .andExpect(status().isBadRequest())
                .andReturn();

        Assertions.assertTrue(mvcResult.getResponse().getContentAsString().contains("000000001"));
    }

    @Test
    @DatabaseSetup("/controller/pallet-receiving-controller/validate-box/before-invalid-type.xml")
    @ExpectedDatabase(value = "/controller/pallet-receiving-controller/validate-box/before-invalid-type.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void validateOrderIdInvalidTypeTest() throws Exception {
        MvcResult mvcResult = mockMvc.perform(post("/box/validate-box")
                        .cookie(new Cookie(AuthenticationParam.USERNAME.getCode(), "TEST"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent("controller/pallet-receiving-controller/validate-box/" +
                                "request-valid.json")))
                .andExpect(status().isBadRequest())
                .andReturn();

        Assertions.assertTrue(mvcResult.getResponse().getContentAsString().contains("UPDATABLE_CUSTOMER_RETURN"));
    }

    @Test
    @DatabaseSetup("/controller/pallet-receiving-controller/validate-box/before-invalid-status.xml")
    @ExpectedDatabase(value = "/controller/pallet-receiving-controller/validate-box/before-invalid-status.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void validateOrderIdInvalidStatusTest() throws Exception {
        MvcResult mvcResult = mockMvc.perform(post("/box/validate-box")
                        .cookie(new Cookie(AuthenticationParam.USERNAME.getCode(), "TEST"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent("controller/pallet-receiving-controller/validate-box/" +
                                "request-valid.json")))
                .andExpect(status().isBadRequest())
                .andReturn();

        Assertions.assertTrue(mvcResult.getResponse().getContentAsString().contains("RECEIPT_IN_WRONG_STATUS"));
    }

    @Test
    @DatabaseSetup("/controller/pallet-receiving-controller/validate-box/before-invalid-sku.xml")
    @ExpectedDatabase(value = "/controller/pallet-receiving-controller/validate-box/before-invalid-sku.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void validateOrderIdInvalidSkuTest() throws Exception {
        MvcResult mvcResult = mockMvc.perform(post("/box/validate-box")
                .cookie(new Cookie(AuthenticationParam.USERNAME.getCode(), "TEST"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("controller/pallet-receiving-controller/validate-box/" +
                        "request-valid.json")))
                .andExpect(status().isBadRequest())
                .andReturn();

        Assertions.assertTrue(mvcResult.getResponse().getContentAsString().contains("000000001"));
    }

    //Принимаем попалетно; коробка привязана к непринятой заявленной палете
    @Test
    @DatabaseSetup("/controller/pallet-receiving-controller/receive-unknown-box/initial-not-received-pallet-before.xml")
    @ExpectedDatabase(value = "/controller/pallet-receiving-controller/receive-unknown-box/" +
            "initial-not-received-pallet-after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void receiveUnknownBoxForNotReceivedPalletTest() throws Exception {
        mockMvc.perform(post("/box/receive-unknown")
                        .cookie(new Cookie(AuthenticationParam.USERNAME.getCode(), "TEST"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent(
                                "controller/pallet-receiving-controller/receive-unknown-box/request-invalid.json")))
                .andExpect(status().isOk());
    }

    // обычная поставка с статусе IN_RECEIVING, одна из тар закрыта
    // переоткрываем одну тару
    @Test
    @DatabaseSetup("/controller/pallet-receiving-controller/reopen-container/1/before.xml")
    @ExpectedDatabase(value = "/controller/pallet-receiving-controller/reopen-container/1/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void testReopenContainerInReceiving() throws Exception {
        ResultActions result = mockMvc.perform(post("/receipt-detail/reopen-container")
                .cookie(new Cookie(AuthenticationParam.USERNAME.getCode(), "TEST"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("controller/pallet-receiving-controller/reopen-container/1/request.json")));

        result.andExpect(status().isOk()).andExpect(content().json(getFileContent(
                "controller/pallet-receiving-controller/reopen-container/1/response.json")));
    }

    // обычная поставка с статусе IN_RECEIVING, одна из тар закрыта
    // переоткрываем обе тары
    @Test
    @DatabaseSetup("/controller/pallet-receiving-controller/reopen-container/1/before.xml")
    @ExpectedDatabase(value = "/controller/pallet-receiving-controller/reopen-container/1/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void testReopenContainerInReceivingMulti() throws Exception {
        ResultActions result = mockMvc.perform(post("/receipt-detail/reopen-container")
                .cookie(new Cookie(AuthenticationParam.USERNAME.getCode(), "TEST"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("controller/pallet-receiving-controller/reopen-container/1/" +
                        "request-multi.json")));

        result.andExpect(status().isOk()).andExpect(content().json(getFileContent(
                "controller/pallet-receiving-controller/reopen-container/1/response-multi.json")));
    }

    // обычная поставка в статусе CLOSED_WITH_DISCREPANCIES, все тары закрыты
    // переоткрываем тару и откатываем статус поставки
    @Test
    @DatabaseSetup("/controller/pallet-receiving-controller/reopen-container/2/before.xml")
    @ExpectedDatabase(value = "/controller/pallet-receiving-controller/reopen-container/2/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void testReopenContainerClosedWithDiscr() throws Exception {
        ResultActions result = mockMvc.perform(post("/receipt-detail/reopen-container")
                .cookie(new Cookie(AuthenticationParam.USERNAME.getCode(), "TEST"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("controller/pallet-receiving-controller/reopen-container/2/request.json")));

        result.andExpect(status().isOk()).andExpect(content().json(getFileContent(
                "controller/pallet-receiving-controller/reopen-container/2/response.json")));
    }

    // допоставка в статусе CLOSED, все тары закрыты
    // переоткрываем тару, переводим аномальную тару в RE_RECEIVING и откатываем статус поставки
    @Test
    @DatabaseSetup("/controller/pallet-receiving-controller/reopen-container/3/before.xml")
    @ExpectedDatabase(value = "/controller/pallet-receiving-controller/reopen-container/3/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void testReopenContainerAdditionalClosed() throws Exception {
        ResultActions result = mockMvc.perform(post("/receipt-detail/reopen-container")
                .cookie(new Cookie(AuthenticationParam.USERNAME.getCode(), "TEST"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("controller/pallet-receiving-controller/reopen-container/3/request.json")));

        result.andExpect(status().isOk()).andExpect(content().json(getFileContent(
                "controller/pallet-receiving-controller/reopen-container/3/response.json")));
    }

    private void assertSuccessfulRequest(String mappingName, String requestFileName) throws Exception {
        mockMvc.perform(post("/receipt-detail/" + mappingName)
                        .cookie(new Cookie(AuthenticationParam.USERNAME.getCode(), "TEST"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent("controller/pallet-receiving-controller/" + mappingName +
                                requestFileName)))
                .andExpect(status().isOk());
    }

    private void assertBadRequest(String mappingName, String requestFileName, ResultMatcher resultMatcher,
                                  String expectedError)
            throws Exception {
        MvcResult mvcResult = mockMvc.perform(post("/receipt-detail/" + mappingName)
                        .cookie(new Cookie(AuthenticationParam.USERNAME.getCode(), "TEST"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent("controller/pallet-receiving-controller/" + mappingName +
                                requestFileName)))
                .andExpect(resultMatcher)
                .andReturn();
        assertions.assertThat(mvcResult.getResponse().getContentAsString()).contains(expectedError);
    }

    private void assertPrintedProperDate() {
        ArgumentCaptor<String> printCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(printService).print(printCaptor.capture(), anyString());
        String printed = printCaptor.getValue();

        ZonedDateTime receiptDetailDate = getReceiptDetailAddDate();
        String receiptDetailDateMSK = DateTimeFormatter
                .ofPattern("dd.MM.yyyy HH.mm")
                .withZone(ZoneId.of("Europe/Moscow"))
                .format(receiptDetailDate);
        assertTrue(printed.contains(receiptDetailDateMSK));
    }

    private ZonedDateTime getReceiptDetailAddDate() {
        // в create-initial-receipt/before.xml есть один receiptdetail, проверяем новые (RECEIPTLINENUMBER > '00001')
        SqlRowSet rs = jdbc.queryForRowSet("select * from receiptdetail where RECEIPTLINENUMBER > '00001'", null, null);
        assertTrue(rs.next());
        return rs.getTimestamp("adddate").toLocalDateTime().atZone(ZoneId.of("UTC"));
    }
}
