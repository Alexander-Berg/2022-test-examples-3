package ru.yandex.market.wms.receiving.controller;

import java.nio.charset.StandardCharsets;

import javax.servlet.http.Cookie;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import ru.yandex.market.wms.common.model.enums.AuthenticationParam;
import ru.yandex.market.wms.receiving.ReceivingIntegrationTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.wms.common.spring.utils.FileContentUtils.getFileContent;

@DatabaseSetup("/controller/get-inbound/common/loc_state.xml")
@DatabaseSetup("/controller/get-inbound/common/sku_state.xml")
@DatabaseSetup("/controller/get-inbound/common/receipt_state.xml")
@DatabaseSetup("/controller/get-inbound/common/codelkup_state.xml")
@DatabaseSetup("/controller/get-inbound/common/anomaly_lot.xml")
public class GetInboundTest extends ReceivingIntegrationTest {

    @Test
    @DatabaseSetup("/controller/get-inbound/common/receiptdetail_state.xml")
    public void getOKReceiptKey() throws Exception {
        assertRequest(
                post("/INFOR_SCPRD_wmwhse1/inbound/get-inbound"),
                status().isOk(),
                "controller/get-inbound/ok/request.json",
                "controller/get-inbound/ok/response.json"
        );
    }

    @Test
    @DatabaseSetup("/controller/get-inbound/common/receiptassortment.xml")
    @DatabaseSetup("/controller/get-inbound/common/nsqlconfig.xml")
    public void getAssortmentSkuReceipt() throws Exception {
        assertRequest(
                post("/INFOR_SCPRD_wmwhse1/inbound/get-inbound"),
                status().isOk(),
                "controller/get-inbound/assortment/request.json",
                "controller/get-inbound/assortment/response.json"
        );
    }

    @Test
    @DatabaseSetup(value = "/controller/get-inbound/common/receiptassortment-no-service.xml")
    @DatabaseSetup("/controller/get-inbound/common/nsqlconfig.xml")
    public void getAssortmentNoServiceSkuReceipt() throws Exception {
        assertRequest(
                post("/INFOR_SCPRD_wmwhse1/inbound/get-inbound"),
                status().isOk(),
                "controller/get-inbound/assortment/request.json",
                "controller/get-inbound/assortment/response.json"
        );
    }

    @Test
    @DatabaseSetup("/controller/get-inbound/common/receiptdetail_state.xml")
    public void getWrongReceiptKey() throws Exception {
        assertError(
                "controller/get-inbound/wrong/request.json",
                post("/INFOR_SCPRD_wmwhse1/inbound/get-inbound"),
                "Inbound was not found [0000000045]"
        );
    }

    @Test
    @DatabaseSetup("/controller/get-inbound/common/receiptdetail_state.xml")
    public void getInventarizationReceipt() throws Exception {
        assertRequest(
                post("/INFOR_SCPRD_wmwhse1/inbound/get-inbound"),
                status().isOk(),
                "controller/get-inbound/inventarization/request.json",
                "controller/get-inbound/inventarization/response.json"
        );
    }

    @Test
    @DatabaseSetup("/controller/get-inbound/common/receiptdetail_state.xml")
    public void getInventarizationReceiptWithoutDetails() throws Exception {
        assertRequest(
                post("/INFOR_SCPRD_wmwhse1/inbound/get-inbound"),
                status().isOk(),
                "controller/get-inbound/inventarization/emptyDetailsRequest.json",
                "controller/get-inbound/inventarization/emptyDetailsResponse.json"
        );
    }

    @Test
    @DatabaseSetup("/controller/get-inbound/common/instance-identifiers.xml")
    @DatabaseSetup("/controller/get-inbound/common/receiptdetail-identifiers.xml")
    public void getInstanceIdentifiers() throws Exception {
        assertRequest(
                post("/INFOR_SCPRD_wmwhse1/inbound/get-inbound"),
                status().isOk(),
                "controller/get-inbound/ok/request.json",
                "controller/get-inbound/ok/response-with-cis.json"
        );
    }

    @Test
    @DatabaseSetup("/controller/get-inbound/common/instance-identifiers.xml")
    @DatabaseSetup("/controller/get-inbound/common/receiptdetail-identifiers-with-cancelled-uit.xml")
    public void getInstanceIdentifiersWithCancelledUit() throws Exception {
        assertRequest(
                post("/INFOR_SCPRD_wmwhse1/inbound/get-inbound"),
                status().isOk(),
                "controller/get-inbound/ok/request.json",
                "controller/get-inbound/ok/response-with-cis-and-cancelled-uit.json"
        );
    }

    @Test
    @DatabaseSetup("/controller/get-inbound/common/anomaly-identifiers.xml")
    @DatabaseSetup("/controller/get-inbound/common/receiptdetail-identifiers.xml")
    public void getAnomalyInstanceIdentifiers() throws Exception {
        assertRequest(
                post("/INFOR_SCPRD_wmwhse1/inbound/get-inbound"),
                status().isOk(),
                "controller/get-inbound/ok/request.json",
                "controller/get-inbound/ok/response-with-anomaly-identifiers.json"
        );
    }

    @Test
    @DatabaseSetup("/controller/get-inbound/common/receiptdetail_state_unredeemed.xml")
    public void getUnredeemedWithAnomalyOK() throws Exception {
        assertRequest(
                post("/INFOR_SCPRD_wmwhse1/inbound/get-inbound"),
                status().isOk(),
                "controller/get-inbound/unredeemed/request.json",
                "controller/get-inbound/unredeemed/response.json"
        );
    }

    @Test
    @DatabaseSetup("/controller/get-inbound/returns/not-acceptable-box/receiptdetail_returns_not-acceptable-box.xml")
    public void getNotAcceptableBox() throws Exception {
        assertRequest(
                post("/INFOR_SCPRD_wmwhse1/inbound/get-inbound"),
                status().isOk(),
                "controller/get-inbound/returns/not-acceptable-box/request.json",
                "controller/get-inbound/returns/not-acceptable-box/response.json"
        );
    }

    @Test
    @DatabaseSetup("/controller/get-inbound/common/invalid_multiplace.xml")
    @DatabaseSetup("/controller/get-inbound/common/nsqlconfig.xml")
    public void getInvalidBom() throws Exception {
        assertError(
                "controller/get-inbound/multiplace/request.json",
                post("/INFOR_SCPRD_wmwhse1/inbound/get-inbound"),
                "Failed to get details for receipt 0000000101 (ROV0000000000000000500 has susr1=1)"
        );
    }

    @Test
    @DatabaseSetup("/controller/get-inbound/returns/bom/valid-multiplace.xml")
    @DatabaseSetup("/controller/get-inbound/common/nsqlconfig.xml")
    public void getreturnsRegistryForBom() throws Exception {
        assertRequest(
                post("/INFOR_SCPRD_wmwhse1/inbound/get-inbound"),
                status().isOk(),
                "controller/get-inbound/returns/bom/request.json",
                "controller/get-inbound/returns/bom/response.json"
        );
    }

    private void assertRequest(
            MockHttpServletRequestBuilder requestBuilder,
            ResultMatcher status, String requestFile, String responseFile) throws Exception {
        ResultActions result = mockMvc.perform(requestBuilder
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent(requestFile)))
                .andExpect(status);
        if (responseFile != null) {
            String response = getFileContent(responseFile);
            result.andExpect(content().json(response, false));
        }
    }

    private void assertError(String requestFile, MockHttpServletRequestBuilder request, String errorInfo)
            throws Exception {
        MvcResult mvcResult = mockMvc.perform(request
                .cookie(new Cookie(AuthenticationParam.USERNAME.getCode(), "TEST"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent(requestFile)))
                .andExpect(status().is5xxServerError())
                .andReturn();
        assertions.assertThat(mvcResult.getResponse().getContentAsString(StandardCharsets.UTF_8)).contains(errorInfo);
    }
}
