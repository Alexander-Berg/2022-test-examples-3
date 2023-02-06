package ru.yandex.market.wms.receiving.controller;

import javax.servlet.http.Cookie;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;

import ru.yandex.market.wms.common.model.enums.AuthenticationParam;
import ru.yandex.market.wms.common.service.DbConfigService;
import ru.yandex.market.wms.common.spring.dao.implementation.ReceiptDetailIdentityDao;
import ru.yandex.market.wms.common.spring.dao.implementation.ReceiptDetailUitDao;
import ru.yandex.market.wms.common.spring.service.CacheableFileReader;
import ru.yandex.market.wms.receiving.ReceivingIntegrationTest;
import ru.yandex.market.wms.receiving.repository.findquerygenerator.ReceiptDetailApiField;
import ru.yandex.market.wms.shared.libs.label.printer.domain.pojo.PrintResult;
import ru.yandex.market.wms.shared.libs.label.printer.service.printer.PrintService;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.wms.common.spring.utils.FileContentUtils.getFileContent;

public class ReceiptDetailControllerTest extends ReceivingIntegrationTest {

    private static final String PALLET_ID_LABEL_TEMPLATE = """
            \u0010CT~~CD,~CC^~CT~
            ^XA~TA000~JSN^LT0^MNW^PON^PMN^LH0,0^JMA^PR4,4~SD15^JUS^LRN^CI0^XZ
            ^XA
            ^MMT
            ^PW799
            ^LL0799
            ^LS0
            ^FT$@X1@$,110^A@N,68,67,TT0003M_^FH\\^CI17^F8^FD$@RECEIPTKEYZER@$^FS^CI0
            ^FT348,110^A@N,51,51,TT0003M_^FH\\^CI17^F8^FD$@LEFTRECEIPT@$^FS^CI0
            ^FT$@X2@$,487^A@N,51,51,TT0003M_^FH\\^CI17^F8^FD$@LPNZER@$^FS^CI0
            ^FT428,487^A@N,34,33,TT0003M_^FH\\^CI17^F8^FD$@LEFTTOID@$^FS^CI0
            ^FT49,487^A@N,34,33,TT0003M_^FH\\^CI17^F8^FDНомер грузового места^FS^CI0
            ^FT51,410^A@N,39,38,TT0003M_^FH\\^CI17^F8^FDНомер поставщика^FS^CI0
            ^FT112,109^A@N,68,67,TT0003M_^FH\\^CI17^F8^FD№ ПУО^FS^CI0
            ^FT411,406^A@N,51,51,TT0003M_^FH\\^CI17^F8^FD$@STORERKEY@$^FS^CI0
            ^FT50,199^A@N,39,38,TT0003M_^FH\\^CI17^F8^FDПоставщик:^FS^CI0
            ^FT48,262^A@N,39,38,TT0003M_^FH\\^CI17^F8^FD$@COMPANY1@$^FS^CI0
            ^FT49,326^A@N,39,38,TT0003M_^FH\\^CI17^F8^FD$@COMPANY2@$^FS^CI0
            ^BY4,3,160^FT20,698^B3N,N,,N,N
            ^FD$@LPN@$^FS
            ^PQ1,0,1,Y^XZ
            """;

    private static final String PALLET_LABEL_WITH_DATE_TEMPLATE = """
            \u0010CT~~CD,~CC^~CT~
            ^XA~TA000~JSN^LT0^MNW^PON^PMN^LH0,0^JMA^PR4,4~SD15^JUS^LRN^CI28^XZ
            ^XA
            ^MMT
            ^PW799
            ^LL0799
            ^LS0
            ^FT$@X1@$,110^A0N,68,67^FH\\^FD$@RECEIPTKEYZER@$^FS
            ^FT348,110^A0N,51,51^FH\\^FD$@LEFTRECEIPT@$^FS
            ^FT$@X2@$,487^A0N,51,51^FH\\^FD$@LPNZER@$^FS
            ^FT428,487^A0N,34,33^FH\\^FD$@LEFTTOID@$^FS
            ^FT49,487^A0N,34,33^FH\\^FDНомер грузового места^FS
            ^FT51,410^A0N,39,38,^FH\\^FDНомер поставщика^FS
            ^FT112,109^A0N,68,67^FH\\^FD№ ПУО^FS
            ^FT411,406^A0N,51,51^FH\\^FD$@STORERKEY@$^FS
            ^FT50,199^A0N,39,38^FH\\^FDПоставщик:^FS
            ^FT48,262^A0N,39,38^FH\\^FD$@COMPANY1@$^FS
            ^FT49,326^A0N,39,38^FH\\^FD$@COMPANY2@$^FS
            ^FT49,564^A0N,34,33^FH\\^FDДата создания^FS
            ^FT427,564^A0N,34,33^FH\\^FD$@DATE@$^FS
            ^BY4,3,160^FT20,775^B3N,N,,N,N
            ^FD$@LPN@$^FS
            ^PQ1,0,1,Y^XZ""";

    @MockBean
    @Autowired
    private CacheableFileReader cacheableFileReader;

    @MockBean
    @Autowired
    private PrintService printService;

    @Autowired
    DbConfigService configService;

    @Autowired
    private ReceiptDetailIdentityDao receiptDetailIdentityDao;
    @Autowired
    private ReceiptDetailUitDao receiptDetailUitDao;

    @BeforeEach
    public void init() {
        super.init();
        Mockito.doReturn("SERIAL_INVENTORY_TEMPLATE").when(cacheableFileReader)
                .readFile(eq("/opt/infor/sce/scprd/wm/labels/"), eq("SERIAL_RF.zpl"));
        Mockito.doReturn(PALLET_ID_LABEL_TEMPLATE).when(cacheableFileReader)
                .readFile(eq("/opt/infor/sce/scprd/wm/labels/"), eq("gn_pid.zpl"));
        Mockito.doReturn(PALLET_LABEL_WITH_DATE_TEMPLATE).when(cacheableFileReader)
                .readFile(eq("/opt/infor/sce/scprd/wm/labels/"), eq("gn_pid_date.zpl"));
        Mockito.when(configService.getConfig(anyString(), eq(".*"))).thenReturn("^(CART|PLT|L|TOT|CDR|RCP|TM|AN)\\d+$");

        mockPrintService();
    }

    @AfterEach
    public void reset() {
        Mockito.reset(cacheableFileReader);
    }

    @Test
    @DatabaseSetup("/controller/receipt-detail/create-initial-receipt/before.xml")
    public void createInitialInboundReceiptByExternalReceiptKey3() throws Exception {
        mockPrintService();
        assertSuccessfulRequest("create-initial-receipt",
                "/request/create-initial-inbound-receipt-detail-by-externalreceiptkey3.json");
    }

    @Test
    @DatabaseSetup("/controller/receipt-detail/get-receipt-details/details-with-uits.xml")
    @ExpectedDatabase(value = "/controller/receipt-detail/get-receipt-details/details-with-uits.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void testApiFieldsMappingAtSequentialPageLoad() throws Exception {
        for (ReceiptDetailApiField rdaf : ReceiptDetailApiField.values()) {
            String field = rdaf.getField();
            ResultActions result = mockMvc.perform(get("/receipt-detail/0000000101")
                    .param("sort", field));
            result.andExpect(status().isOk());
        }
    }

    @Test
    @DatabaseSetup("/controller/receipt-detail/get-receipt-details/details-with-uits.xml")
    @ExpectedDatabase(value = "/controller/receipt-detail/get-receipt-details/details-with-uits.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void testSequentialPageLoadReturns() throws Exception {
        ResultActions result = mockMvc.perform(get("/receipt-detail/0000000102")
                .param("offset", "0")
                .param("sort", "uit")
                .param("order", "DESC")
                .param("limit", "100")
                .contentType(MediaType.APPLICATION_JSON));

        result.andExpect(status().isOk()).andExpect(content().json(
                getFileContent(
                        "controller/receipt-detail/get-receipt-details/get-receipt-details-response-returns.json")));

        org.json.JSONObject obj = new org.json.JSONObject(result.andReturn().getResponse().getContentAsString());
        assertions.assertThat(obj).isNotNull();
    }

    @Test
    @DatabaseSetup("/controller/receipt-detail/get-receipt-details/details-with-uits.xml")
    @ExpectedDatabase(value = "/controller/receipt-detail/get-receipt-details/details-with-uits.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void testSequentialPageLoadReturnsComplicatedFilter() throws Exception {
        ResultActions result = mockMvc.perform(get("/receipt-detail/0000000102")
                .param("offset", "0")
                .param("sort", "uit")
                .param("order", "DESC")
                .param("filter", "(toloc==STAGE01,toloc==STAGE02);status==RECEIVED_COMPLETE")
                .param("limit", "100")
                .contentType(MediaType.APPLICATION_JSON));

        result.andExpect(status().isOk()).andExpect(content().json(
                getFileContent(
                        "controller/receipt-detail/get-receipt-details/get-receipt-details-response-returns.json")));

        org.json.JSONObject obj = new org.json.JSONObject(result.andReturn().getResponse().getContentAsString());
        assertions.assertThat(obj).isNotNull();
    }

    @Test
    @DatabaseSetup("/controller/receipt-detail/get-receipt-details/details-with-uits.xml")
    @ExpectedDatabase(value = "/controller/receipt-detail/get-receipt-details/details-with-uits.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void testSequentialPageLoadWithFilterByStatus() throws Exception {
        int offset = 0;
        int total = 0;
        int viewed = 0;
        for (int i = 0; i < 2; i++) {
            ResultActions result = mockMvc.perform(get("/receipt-detail/0000000101")
                    .param("offset", String.valueOf(offset))
                    .param("sort", "uit")
                    .param("order", "DESC")
                    .param("filter", "(toloc==STAGE01,toloc==STAGE02);status==RECEIVED_COMPLETE")
                    .param("limit", "4")
                    .contentType(MediaType.APPLICATION_JSON));

            result.andExpect(status().isOk()).andExpect(content().json(getFileContent(String.format(
                    "controller/receipt-detail/get-receipt-details/get-receipt-details-response-%s.json", i + 1))));

            org.json.JSONObject obj = new org.json.JSONObject(result.andReturn().getResponse().getContentAsString());
            assertions.assertThat(obj).isNotNull();

            total = Integer.parseInt(obj.getString("total"));
            offset += Integer.parseInt(obj.getString("limit"));
            viewed += obj.getJSONArray("content").length();
        }
        assertions.assertThat(viewed == total).isTrue();
    }

    @Test
    @DatabaseSetup("/controller/receipt/db.xml")
    void testReceiptDetailsDownloadXls() throws Exception {
        mockMvc.perform(get("/receipt-detail/{receiptKey}/download/{format}", "0000000101", "xls"))
                .andExpect(status().isOk());
    }

    @Test
    @DatabaseSetup("/controller/receipt/db.xml")
    @ExpectedDatabase(value = "/controller/receipt/db.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void testPalletsSequentialPageLoadWithFilter() throws Exception {
        int offset = 0;
        int total = 0;
        int viewed = 0;
        for (int i = 0; i < 1; i++) {
            ResultActions result = mockMvc.perform(get("/receipt-detail/0000000101/pallets")
                    .param("offset", String.valueOf(offset))
                    .param("sort", "toid")
                    .param("order", "DESC")
                    .param("filter", "status==PALLET_ACCEPTANCE;toLoc==STAGE01")
                    .param("limit", "4")
                    .contentType(MediaType.APPLICATION_JSON));

            result.andExpect(status().isOk()).andExpect(content().json(getFileContent(String.format(
                    "controller/receipt-detail/get-receipt-pallets/get-receipt-pallets-response-%s.json", i + 1))));

            org.json.JSONObject obj = new org.json.JSONObject(result.andReturn().getResponse().getContentAsString());
            assertions.assertThat(obj).isNotNull();

            total = Integer.parseInt(obj.getString("total"));
            offset += Integer.parseInt(obj.getString("limit"));
            viewed += obj.getJSONArray("content").length();
        }
        assertions.assertThat(viewed == total).isTrue();
    }

    @Test
    @DatabaseSetup("/controller/receipt/db.xml")
    @ExpectedDatabase(value = "/controller/receipt/db.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void testReceiptLinesSequentialPageLoadWithFilter() throws Exception {
        int offset = 0;
        int total = 0;
        int viewed = 0;
        for (int i = 0; i < 2; i++) {
            ResultActions result = mockMvc.perform(get("/receipt-detail/0000000101/lines")
                    .param("offset", String.valueOf(offset))
                    .param("sort", "receiptLineNumber")
                    .param("order", "ASC")
                    .param("filter", "(status==NEW,toid==M100);qtyexpected=gt=1")
                    .param("limit", "4")
                    .contentType(MediaType.APPLICATION_JSON));

            result.andExpect(status().isOk()).andExpect(content().json(getFileContent(String.format(
                    "controller/receipt-detail/get-receipt-lines/get-receipt-lines-response-%s.json", i + 1))));

            org.json.JSONObject obj = new org.json.JSONObject(result.andReturn().getResponse().getContentAsString());
            assertions.assertThat(obj).isNotNull();

            total = Integer.parseInt(obj.getString("total"));
            offset += Integer.parseInt(obj.getString("limit"));
            viewed += obj.getJSONArray("content").length();
        }
        assertions.assertThat(viewed == total).isTrue();
    }

    @Test
    @DatabaseSetup("/controller/receipt/db.xml")
    void testReceiptPalletsInterWh() throws Exception {
        ResultActions result = mockMvc.perform(get("/receipt-detail/INTERWH01/pallets")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andExpect(content().json(getFileContent(
                "controller/receipt-detail/get-receipt-pallets/get-receipt-pallets-interwh-response.json")));
    }

    @Test
    @DatabaseSetup("/controller/receipt-detail/cancel/before.xml")
    @ExpectedDatabase(value = "/controller/receipt-detail/cancel/before.xml", assertionMode = NON_STRICT_UNORDERED)
    void testCancelIncorrectQuantity() throws Exception {
        ResultActions result = mockMvc.perform(post("/receipt-detail/cancel")
                .cookie(new Cookie(AuthenticationParam.USERNAME.getCode(), "TEST"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("controller/receipt-detail/cancel/request/multiple-receipt-keys.json")));

        result.andExpect(status().isBadRequest()).andExpect(content().json(getFileContent(
                "controller/receipt-detail/cancel/response/multiple-receipt-keys.json")));
    }

    @Test
    @DatabaseSetup("/controller/receipt-detail/cancel/before.xml")
    @ExpectedDatabase(value = "/controller/receipt-detail/cancel/before.xml", assertionMode = NON_STRICT_UNORDERED)
    void testCancelWrongReceiptStatus() throws Exception {
        ResultActions result = mockMvc.perform(post("/receipt-detail/cancel")
                .cookie(new Cookie(AuthenticationParam.USERNAME.getCode(), "TEST"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("controller/receipt-detail/cancel/request/wrong-receipt-status.json")));

        result.andExpect(status().isOk()).andExpect(content().json(getFileContent(
                "controller/receipt-detail/cancel/response/wrong-receipt-status.json")));
    }

    @Test
    @DatabaseSetup("/controller/receipt-detail/cancel/before.xml")
    @ExpectedDatabase(value = "/controller/receipt-detail/cancel/after.xml", assertionMode = NON_STRICT_UNORDERED)
    void testCancelMultipleLines() throws Exception {
        ResultActions result = mockMvc.perform(post("/receipt-detail/cancel")
                .cookie(new Cookie(AuthenticationParam.USERNAME.getCode(), "TEST"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("controller/receipt-detail/cancel/request/multiple-lines.json")));

        result.andExpect(status().isOk()).andExpect(content().json(getFileContent(
                "controller/receipt-detail/cancel/response/multiple-lines.json")));
    }

    private void assertBadRequest(String mappingName, String requestFileName, ResultMatcher resultMatcher,
                                  String expectedError)
            throws Exception {
        MvcResult mvcResult = mockMvc.perform(post("/receipt-detail/" + mappingName)
                        .cookie(new Cookie(AuthenticationParam.USERNAME.getCode(), "TEST"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent("controller/receipt-detail/" + mappingName + requestFileName)))
                .andExpect(resultMatcher)
                .andReturn();
        assertions.assertThat(mvcResult.getResponse().getContentAsString()).contains(expectedError);
    }

    private void assertSuccessfulRequest(String mappingName, String requestFileName) throws Exception {
        mockMvc.perform(post("/receipt-detail/" + mappingName)
                        .cookie(new Cookie(AuthenticationParam.USERNAME.getCode(), "TEST"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent("controller/receipt-detail/" + mappingName + requestFileName)))
                .andExpect(status().isOk());
    }

    private void mockPrintService() {
        Mockito.reset(printService);
        Mockito.when(printService.print(anyString(), anyString()))
                .thenReturn(new PrintResult(HttpStatus.OK.toString(), null, null));
    }
}
