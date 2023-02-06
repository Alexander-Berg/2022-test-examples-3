package ru.yandex.market.wms.receiving.controller;

import java.nio.charset.StandardCharsets;

import javax.servlet.http.Cookie;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;

import ru.yandex.market.wms.common.model.enums.AuthenticationParam;
import ru.yandex.market.wms.common.spring.dao.implementation.ReceiptDao;
import ru.yandex.market.wms.common.spring.dao.implementation.ReceiptStatusHistoryDao;
import ru.yandex.market.wms.receiving.ReceivingIntegrationTest;
import ru.yandex.market.wms.receiving.config.ServiceBusConfiguration;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT;
import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.skyscreamer.jsonassert.JSONAssert.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.wms.common.spring.utils.FileContentUtils.getFileContent;

@ContextConfiguration(classes = ServiceBusConfiguration.class)
class InboundControllerTest extends ReceivingIntegrationTest {

    @Autowired
    @SpyBean
    private ReceiptDao receiptDao;

    @Autowired
    @SpyBean
    private ReceiptStatusHistoryDao receiptStatusHistoryDao;

    @AfterEach
    void after() {
        Mockito.reset(receiptDao, receiptStatusHistoryDao);
    }

    @Test
    @DatabaseSetup("/cancel-inbound/before/common.xml")
    @ExpectedDatabase(value = "/cancel-inbound/after/allowed-statuses.xml", assertionMode = NON_STRICT)
    public void cancelInboundInAllowedStatus() throws Exception {
        mockMvc.perform(post("/INFOR_SCPRD_wmwhse1/inbound/0000000001/cancel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent("cancel-inbound/request/empty-request.json")))
                .andExpect(status().isOk());

        mockMvc.perform(post("/INFOR_SCPRD_wmwhse1/inbound/0000000002/cancel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent("cancel-inbound/request/empty-request.json")))
                .andExpect(status().isOk());

        mockMvc.perform(post("/INFOR_SCPRD_wmwhse1/inbound/0000000003/cancel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent("cancel-inbound/request/empty-request.json")))
                .andExpect(status().isOk());
    }

    @Test
    @DatabaseSetup("/cancel-inbound/before/common.xml")
    @ExpectedDatabase(value = "/cancel-inbound/before/common.xml", assertionMode = NON_STRICT)
    public void cancelInboundInDisallowedStatus() throws Exception {
        mockMvc.perform(post("/INFOR_SCPRD_wmwhse1/inbound/0000000004/cancel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent("cancel-inbound/request/empty-request.json")))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DatabaseSetup("/cancel-inbound/before/common.xml")
    @ExpectedDatabase(value = "/cancel-inbound/before/common.xml", assertionMode = NON_STRICT)
    public void cancelInboundIfAlreadyReceived() throws Exception {
        mockMvc.perform(post("/INFOR_SCPRD_wmwhse1/inbound/0000000005/cancel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent("cancel-inbound/request/empty-request.json")))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DatabaseSetup("/cancel-inbound/additional-receipt-wrong-tare-status/before.xml")
    public void cancelInboundAdditionalReceiptTaresHaveWrongStatuses()
            throws Exception {
        assertError("cancel-inbound/request/empty-request.json",
                post("/INFOR_SCPRD_wmwhse1/inbound/0000000005/cancel")
                        .contentType(MediaType.APPLICATION_JSON),
                "Аномальная тара PLT00001 имеет различные статусы",
                status().isBadRequest());
    }

    @Test
    @DatabaseSetup("/cancel-inbound/additional-receipt/before.xml")
    @ExpectedDatabase(value = "/cancel-inbound/additional-receipt/after.xml",
            assertionMode = NON_STRICT)
    public void cancelInboundAdditionalReceipt()
            throws Exception {
        mockMvc.perform(post("/INFOR_SCPRD_wmwhse1/inbound/0000000005/cancel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent("cancel-inbound/request/empty-request.json")))
                .andExpect(status().isOk());
    }

    @Test
    @DatabaseSetup(value = "/items/db/initial.xml", connection = "enterpriseConnection")
    @DatabaseSetup(value = "/items/db/initial.xml", connection = "wmwhseConnection")
    @DatabaseSetup(value = "/inbound/db/storer-sku-and-related.xml", connection = "wmwhseConnection")
    @DatabaseSetup(value = "/inbound/db/storer-partner-facility-control.xml")
    @ExpectedDatabase(value = "/inbound/db/receipt-and-details.xml", connection = "wmwhseConnection",
            assertionMode = NON_STRICT_UNORDERED)
    @ExpectedDatabase(value = "/inbound/db/receipt-and-details-status-history.xml", connection = "wmwhseConnection",
            assertionMode = NON_STRICT_UNORDERED)
    public void createInbound() throws Exception {
        assertRequest(
                post("/INFOR_SCPRD_wmwhse1/inbound"),
                status().isOk(),
                "inbound/create/request/base.json",
                "inbound/create/response/first-receipt.json"
        );
    }

    @Test
    @DatabaseSetup(value = "/items/db/initial.xml", connection = "enterpriseConnection")
    @DatabaseSetup(value = "/items/db/initial.xml", connection = "wmwhseConnection")
    @DatabaseSetup(value = "/inbound/db/storer-sku-and-related.xml", connection = "enterpriseConnection")
    @DatabaseSetup(value = "/inbound/db/storer-sku-and-related.xml", connection = "wmwhseConnection")
    @DatabaseSetup(value = "/inbound/db/receipt-and-details-before-update.xml", connection = "wmwhseConnection")
    @DatabaseSetup(value = "/inbound/db/receipt-and-details-status-history.xml", connection = "wmwhseConnection")
    @ExpectedDatabase(value = "/inbound/db/storer-sku-and-related.xml", connection = "wmwhseConnection",
            assertionMode = NON_STRICT_UNORDERED)
    @ExpectedDatabase(value = "/inbound/db/receipt-and-details.xml", connection = "wmwhseConnection",
            assertionMode = NON_STRICT_UNORDERED)
    @ExpectedDatabase(value = "/inbound/db/receipt-and-details-status-history.xml", connection = "wmwhseConnection",
            assertionMode = NON_STRICT_UNORDERED)
    public void updateInbound() throws Exception {
        assertRequest(
                post("/INFOR_SCPRD_wmwhse1/inbound"),
                status().isOk(),
                "inbound/create/request/base.json",
                "inbound/create/response/first-receipt.json"
        );
    }

    @Test
    @DatabaseSetup(value = "/items/db/initial.xml", connection = "wmwhseConnection")
    @DatabaseSetup(value = "/inbound/db/storer-sku-and-related.xml", connection = "wmwhseConnection")
    @DatabaseSetup(value = "/inbound/db/receipt-and-details-status-history-3.xml", connection = "wmwhseConnection")
    public void getInboundStatuses() throws Exception {
        final String result = mockMvc.perform(post("/INFOR_SCPRD_wmwhse1/inbound/get-inbound-statuses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent("inbound/get/request/get-inbound-statuses.json")))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn()
                .getResponse()
                .getContentAsString();
        assertEquals(getFileContent("inbound/get/response/get-inbound-statuses.json"), result,
                JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    @DatabaseSetup(value = "/items/db/initial.xml", connection = "wmwhseConnection")
    @DatabaseSetup(value = "/inbound/db/storer-sku-and-related.xml", connection = "wmwhseConnection")
    @DatabaseSetup(value = "/inbound/db/receipt-and-trailer-status-history.xml", connection = "wmwhseConnection")
    public void getInboundTrailerStatuses() throws Exception {
        final String result = mockMvc.perform(post("/INFOR_SCPRD_wmwhse1/inbound/get-inbound-statuses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent("inbound/get/request/get-inbound-statuses.json")))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn()
                .getResponse()
                .getContentAsString();
        assertEquals(getFileContent("inbound/get/response/get-inbound-statuses-with-trailer.json"), result,
                JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    @DatabaseSetup(value = "/items/db/initial.xml", connection = "wmwhseConnection")
    @DatabaseSetup(value = "/inbound/db/storer-sku-and-related.xml", connection = "wmwhseConnection")
    @DatabaseSetup(value = "/inbound/db/receipt-and-details-status-history-3.xml", connection = "wmwhseConnection")
    public void getInboundHistory() throws Exception {
        final String result = mockMvc.perform(post("/INFOR_SCPRD_wmwhse1/inbound/get-inbound-history")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent("inbound/get/request/get-inbound-history.json")))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn()
                .getResponse()
                .getContentAsString();
        assertEquals(getFileContent("inbound/get/response/get-inbound-history.json"), result,
                JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    @DatabaseSetup(value = "/items/db/initial.xml", connection = "wmwhseConnection")
    @DatabaseSetup(value = "/inbound/db/storer-sku-and-related.xml", connection = "wmwhseConnection")
    @DatabaseSetup(value = "/inbound/db/receipt-and-details-status-history-3.xml", connection = "wmwhseConnection")
    public void getInboundHistories() throws Exception {
        final String result = mockMvc.perform(post("/INFOR_SCPRD_wmwhse1/inbound/get-inbound-histories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent("inbound/get/request/get-inbound-histories.json")))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn()
                .getResponse()
                .getContentAsString();
        assertEquals(getFileContent("inbound/get/response/get-inbound-histories.json"), result,
                JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    @ExpectedDatabase(value = "/inbound/db/interstore-expected.xml", connection = "wmwhseConnection",
            assertionMode = NON_STRICT_UNORDERED)
    public void createInterstoreReceipt() throws Exception {
        mockMvc.perform(post("/INFOR_SCPRD_wmwhse1/inbound")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent("inbound/create/request/interstore.json")))
                .andExpect(status().isOk());
    }

    @Test
    @DatabaseSetup(value = "/inbound/interstore/1/interstore-initial.xml", connection = "wmwhseConnection")
    @ExpectedDatabase(value = "/inbound/interstore/1/interstore-expected.xml", connection = "wmwhseConnection",
            assertionMode = NON_STRICT_UNORDERED)
    public void interstoreReceiptShouldBeUpdatedIfNEW() throws Exception {
        mockMvc.perform(post("/INFOR_SCPRD_wmwhse1/inbound")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent("inbound/interstore/1/interstore.json")))
                .andExpect(status().isOk());
    }

    @Test
    @DatabaseSetup(value = "/inbound/interstore/2/interstore-initial.xml", connection = "wmwhseConnection")
    @ExpectedDatabase(value = "/inbound/interstore/2/interstore-expected.xml", connection = "wmwhseConnection",
            assertionMode = NON_STRICT_UNORDERED)
    public void interstoreReceiptShouldNotBeUpdatedIfNotNEW() throws Exception {
        mockMvc.perform(post("/INFOR_SCPRD_wmwhse1/inbound")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent("inbound/interstore/2/interstore.json")))
                .andExpect(status().isOk());
    }

    @Test
    @DatabaseSetup(value = "/inbound/additional-delivery/1/initial.xml", connection = "wmwhseConnection")
    @DatabaseSetup(value = "/inbound/additional-delivery/1/storer-sku-and-related.xml",
            connection = "wmwhseConnection")
    @DatabaseSetup(value = "/inbound/additional-delivery/1/receipt-and-details.xml", connection = "wmwhseConnection")
    @ExpectedDatabase(value = "/inbound/additional-delivery/1/additional-expected.xml", connection = "wmwhseConnection",
            assertionMode = NON_STRICT_UNORDERED)
    public void createReceiptForAdditionalInbound() throws Exception {
        mockMvc.perform(post("/INFOR_SCPRD_wmwhse1/inbound/additional")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent("inbound/additional-delivery/1/request.json")))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(content().json(getFileContent("inbound/additional-delivery/1/response.json")));
    }

    @Test
    @DatabaseSetup(value = "/inbound/additional-delivery/2/initial.xml", connection = "wmwhseConnection")
    @DatabaseSetup(value = "/inbound/additional-delivery/2/storer-sku-and-related.xml",
            connection = "wmwhseConnection")
    @DatabaseSetup(value = "/inbound/additional-delivery/2/receipt-and-details.xml", connection = "wmwhseConnection")
    @ExpectedDatabase(value = "/inbound/additional-delivery/2/additional-expected.xml", connection = "wmwhseConnection",
            assertionMode = NON_STRICT_UNORDERED)
    public void existingReceiptWithParentShouldBeReturnedForAdditionalInbound() throws Exception {
        mockMvc.perform(post("/INFOR_SCPRD_wmwhse1/inbound/additional")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent("inbound/additional-delivery/2/request.json")))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(content().json(getFileContent("inbound/additional-delivery/2/response.json")));
    }

    @Test
    @DatabaseSetup(value = "/inbound/additional-delivery/6/initial_db.xml", connection = "wmwhseConnection")
    @ExpectedDatabase(value = "/inbound/additional-delivery/6/expected_db.xml",
            connection = "wmwhseConnection", assertionMode = NON_STRICT_UNORDERED)
    public void additionalDeliveryShouldNotBeCreatedIfAnotherNotInFinalStatus() throws Exception {
        MvcResult result = mockMvc.perform(post("/INFOR_SCPRD_wmwhse1/inbound/additional")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent("inbound/additional-delivery/6/request.json")))
                .andExpect(status().isBadRequest()).andReturn();
        String content = result.getResponse().getContentAsString();
        String model = "There is already active additional deliveries for receipt 000000222";
        Assertions.assertTrue(content.contains(model));
    }

    @Test
    @DatabaseSetup(value = "/inbound/additional-delivery/10/initial_db.xml", connection = "wmwhseConnection")
    @ExpectedDatabase(value = "/inbound/additional-delivery/10/expected_db.xml",
            connection = "wmwhseConnection", assertionMode = NON_STRICT_UNORDERED)
    public void additionalDeliveryShouldBeCreatedForReceiptWithAnomaliesInChildReceipts() throws Exception {
        mockMvc.perform(post("/INFOR_SCPRD_wmwhse1/inbound/additional")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent("inbound/additional-delivery/10/request.json")))
                .andExpect(status().isOk()).andReturn();
    }

    @Test
    @DatabaseSetup(value = "/inbound/additional-delivery/13/initial_db.xml", connection = "wmwhseConnection")
    @ExpectedDatabase(value = "/inbound/additional-delivery/13/expected_db.xml",
            connection = "wmwhseConnection", assertionMode = NON_STRICT_UNORDERED)
    public void putInboundShouldUpdateReceiptInfoForNEWStatus() throws Exception {
        mockMvc.perform(post("/INFOR_SCPRD_wmwhse1/inbound/additional")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent("inbound/additional-delivery/13/request.json")))
                .andExpect(status().isOk()).andReturn();

        //обновим дату рецепта
        mockMvc.perform(post("/INFOR_SCPRD_wmwhse1/inbound/additional")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent("inbound/additional-delivery/13/request_update.json")))
                .andExpect(status().isOk()).andReturn();

    }

    @Test
    @DatabaseSetup(value = "/inbound/additional-delivery/14/initial_db.xml", connection = "wmwhseConnection")
    @ExpectedDatabase(value = "/inbound/additional-delivery/14/expected_db.xml",
            connection = "wmwhseConnection", assertionMode = NON_STRICT_UNORDERED)
    public void putInboundShouldNotUpdateReceiptInfoForOtherStatuses() throws Exception {
        mockMvc.perform(post("/INFOR_SCPRD_wmwhse1/inbound/additional")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent("inbound/additional-delivery/14/request_update.json")))
                .andExpect(status().isOk()).andReturn();
    }

    @Test
    @DatabaseSetup("/cancel-inbound/before/common.xml")
    @ExpectedDatabase(value = "/cancel-inbound/before/common.xml", assertionMode = NON_STRICT)
    public void cancelNonexistentInbound() throws Exception {
        mockMvc.perform(post("/INFOR_SCPRD_wmwhse1/inbound/NOT_EXISTENT_KEY/cancel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent("cancel-inbound/request/empty-request.json")))
                .andExpect(status().isNotFound());
    }

    @Test
    @DatabaseSetup(value = "/service/inbound/get/correct-additional/initial.xml", connection = "wmwhseConnection")
    @DatabaseSetup(value = "/service/inbound/get/correct-additional/anomalies.xml", connection = "wmwhseConnection")
    @DatabaseSetup(value = "/service/inbound/get/correct-additional/receipt-and-details-with-anomaly.xml",
            connection = "wmwhseConnection")
    @DatabaseSetup(value = "/service/inbound/db/storer-sku-and-related.xml", connection = "wmwhseConnection")
    @DatabaseSetup(value = "/service/inbound/db/receipt-and-details-status-history.xml",
            connection = "wmwhseConnection")
    public void getInboundAdditional() throws Exception {
        assertRequest(
                post("/INFOR_SCPRD_wmwhse1/inbound/get-inbound"),
                status().isOk(),
                "service/inbound/get/correct-additional/request.json",
                "service/inbound/get/correct-additional/response.json"
        );
    }

    @Test
    @DatabaseSetup(value = "/service/inbound/get/correct-no-anomaly-sku/initial.xml", connection = "wmwhseConnection")
    @DatabaseSetup(value = "/service/inbound/get/correct-no-anomaly-sku/anomalies.xml", connection = "wmwhseConnection")
    @DatabaseSetup(value = "/service/inbound/get/correct-no-anomaly-sku/receipt-and-details-with-anomaly.xml",
            connection = "wmwhseConnection")
    @DatabaseSetup(value = "/service/inbound/db/storer-sku-and-related.xml", connection = "wmwhseConnection")
    @DatabaseSetup(value = "/service/inbound/db/receipt-and-details-status-history.xml",
            connection = "wmwhseConnection")
    public void getInboundNoAnomalySku() throws Exception {
        assertRequest(
                post("/INFOR_SCPRD_wmwhse1/inbound/get-inbound"),
                status().isOk(),
                "service/inbound/get/correct-no-anomaly-sku/request.json",
                "service/inbound/get/correct-no-anomaly-sku/response.json"
        );
    }

    @Test
    @DatabaseSetup(value = "/service/inbound/get/correct-additional/initial.xml", connection = "wmwhseConnection")
    @DatabaseSetup(value = "/service/inbound/get/correct-additional/receipt-and-details.xml",
            connection = "wmwhseConnection")
    @DatabaseSetup(value = "/service/inbound/db/storer-sku-and-related.xml", connection = "wmwhseConnection")
    @DatabaseSetup(value = "/service/inbound/db/receipt-and-details-status-history.xml",
            connection = "wmwhseConnection")
    public void getInboundNoAnomalyRegistries() throws Exception {
        assertRequest(
                post("/INFOR_SCPRD_wmwhse1/inbound/get-inbound"),
                status().isOk(),
                "service/inbound/get/correct-additional/request.json",
                "service/inbound/get/correct-additional/response-no-anomalies.json"
        );
    }

    @Test
    @DatabaseSetup(value = "/service/inbound/get/incorrect-lot-status/initial.xml", connection = "wmwhseConnection")
    @DatabaseSetup(value = "/service/inbound/get/incorrect-lot-status/receipt-and-details.xml",
            connection = "wmwhseConnection")
    @DatabaseSetup(value = "/service/inbound/db/storer-sku-and-related.xml", connection = "wmwhseConnection")
    @DatabaseSetup(value = "/service/inbound/db/receipt-and-details-status-history.xml",
            connection = "wmwhseConnection")
    public void getInboundIncorrectAnomalyLotStatus() throws Exception {
        assertError(
                "service/inbound/get/incorrect-lot-status/request.json",
                post("/INFOR_SCPRD_wmwhse1/inbound/get-inbound"),
                "Часть партий в контейнере PLT123 имеет статус отличный от RE_RECEIVED", status().is4xxClientError());
    }

    @Test
    @DatabaseSetup(value = "/service/inbound/get/correct/initial.xml", connection = "wmwhseConnection")
    @DatabaseSetup(value = "/service/inbound/get/correct/anomalies.xml", connection = "wmwhseConnection")
    @DatabaseSetup(value = "/service/inbound/get/correct/receipt-and-details-with-anomaly.xml",
            connection = "wmwhseConnection")
    @DatabaseSetup(value = "/service/inbound/db/storer-sku-and-related.xml", connection = "wmwhseConnection")
    @DatabaseSetup(value = "/service/inbound/db/receipt-and-details-status-history.xml",
            connection = "wmwhseConnection")
    public void getInbound() throws Exception {
        assertRequest(
                post("/INFOR_SCPRD_wmwhse1/inbound/get-inbound"),
                status().isOk(),
                "service/inbound/get/correct/request.json",
                "service/inbound/get/correct/response.json"
        );
    }

    @Test
    @DatabaseSetup(value = "/service/inbound/get/correct-condition-code/initial.xml", connection = "wmwhseConnection")
    @DatabaseSetup(value = "/service/inbound/get/correct-condition-code/receipt-and-details-with-anomaly.xml",
            connection = "wmwhseConnection")
    @DatabaseSetup(value = "/service/inbound/get/correct-condition-code/storer-sku-and-related.xml",
            connection = "wmwhseConnection")
    @DatabaseSetup(value = "/service/inbound/db/receipt-and-details-status-history.xml",
            connection = "wmwhseConnection")
    public void getInboundConditionCode() throws Exception {
        assertRequest(
                post("/INFOR_SCPRD_wmwhse1/inbound/get-inbound"),
                status().isOk(),
                "service/inbound/get/correct-condition-code/request.json",
                "service/inbound/get/correct-condition-code/response.json"
        );
    }

    @Test
    @DatabaseSetup(value = "/service/inbound/get/correct-duplicate-tares/initial.xml", connection = "wmwhseConnection")
    @DatabaseSetup(value = "/service/inbound/get/correct-duplicate-tares/anomalies.xml",
            connection = "wmwhseConnection")
    @DatabaseSetup(value = "/service/inbound/get/correct-duplicate-tares/receipt-and-details-with-anomaly.xml",
            connection = "wmwhseConnection")
    @DatabaseSetup(value = "/service/inbound/db/storer-sku-and-related.xml", connection = "wmwhseConnection")
    @DatabaseSetup(value = "/service/inbound/db/receipt-and-details-status-history.xml",
            connection = "wmwhseConnection")
    public void getInboundDuplicateTares() throws Exception {
        assertRequest(
                post("/INFOR_SCPRD_wmwhse1/inbound/get-inbound"),
                status().isOk(),
                "service/inbound/get/correct-duplicate-tares/request.json",
                "service/inbound/get/correct-duplicate-tares/response.json"
        );
    }

    @Test
    @DatabaseSetup(value = "/service/inbound/get/correct-returns/initial.xml", connection = "wmwhseConnection")
    @DatabaseSetup(value = "/service/inbound/get/correct-returns/receipt-and-details.xml",
            connection = "wmwhseConnection")
    @DatabaseSetup(value = "/service/inbound/get/correct-returns/storer-sku-and-related.xml",
            connection = "wmwhseConnection")
    @DatabaseSetup(value = "/service/inbound/db/receipt-and-details-status-history.xml",
            connection = "wmwhseConnection")
    public void getInboundReturns() throws Exception {
        assertRequest(
                post("/INFOR_SCPRD_wmwhse1/inbound/get-inbound"),
                status().isOk(),
                "service/inbound/get/correct-returns/request.json",
                "service/inbound/get/correct-returns/response.json"
        );
    }

    @Test
    @DatabaseSetup(value = "/service/inbound/get/correct-returns/initial.xml", connection = "wmwhseConnection")
    @DatabaseSetup(value = "/service/inbound/get/correct-returns/receipt-and-details-initial.xml",
            connection = "wmwhseConnection")
    @DatabaseSetup(value = "/service/inbound/get/correct-returns/storer-sku-and-related.xml",
            connection = "wmwhseConnection")
    @DatabaseSetup(value = "/service/inbound/db/receipt-and-details-status-history.xml",
            connection = "wmwhseConnection")
    public void getInboundReturnsInitialByBox() throws Exception {
        assertRequest(
                post("/INFOR_SCPRD_wmwhse1/inbound/get-inbound"),
                status().isOk(),
                "service/inbound/get/correct-returns/request.json",
                "service/inbound/get/correct-returns/response-initial.json"
        );
    }

    @Test
    @DatabaseSetup(value = "/service/inbound/get/correct-returns/initial.xml", connection = "wmwhseConnection")
    @DatabaseSetup(value = "/service/inbound/get/return-initial-pallet/receipt-and-details-initial.xml",
            connection = "wmwhseConnection")
    @DatabaseSetup(value = "/service/inbound/get/correct-returns/storer-sku-and-related.xml",
            connection = "wmwhseConnection")
    @DatabaseSetup(value = "/service/inbound/db/receipt-and-details-status-history.xml",
            connection = "wmwhseConnection")
    public void getInboundReturnsInitialByPallet() throws Exception {
        assertRequest(
                post("/INFOR_SCPRD_wmwhse1/inbound/get-inbound"),
                status().isOk(),
                "service/inbound/get/correct-returns/request.json",
                "service/inbound/get/return-initial-pallet/response-initial.json"
        );
    }

    @Test
    @DatabaseSetup(value = "/service/inbound/get/correct-returns/initial.xml", connection = "wmwhseConnection")
    @DatabaseSetup(value = "/service/inbound/get/correct-returns/receipt-and-details-unknown-box-initial.xml",
            connection = "wmwhseConnection")
    @DatabaseSetup(value = "/service/inbound/get/correct-returns/storer-sku-and-related.xml",
            connection = "wmwhseConnection")
    @DatabaseSetup(value = "/service/inbound/db/receipt-and-details-status-history.xml",
            connection = "wmwhseConnection")
    public void getInboundReturnsInitialWithUnknownBox() throws Exception {
        assertRequest(
                post("/INFOR_SCPRD_wmwhse1/inbound/get-inbound"),
                status().isOk(),
                "service/inbound/get/correct-returns/request.json",
                "service/inbound/get/correct-returns/response-unknown-box-initial.json"
        );
    }

    @Test
    @DatabaseSetup(value = "/service/inbound/get/correct-returns/initial.xml", connection = "wmwhseConnection")
    @DatabaseSetup(value = "/service/inbound/get/correct-returns/receipt-and-details-unknown-box.xml",
            connection = "wmwhseConnection")
    @DatabaseSetup(value = "/service/inbound/get/correct-returns/storer-sku-and-related.xml",
            connection = "wmwhseConnection")
    @DatabaseSetup(value = "/service/inbound/db/receipt-and-details-status-history.xml",
            connection = "wmwhseConnection")
    public void getInboundReturnsSecondaryWithUnknownBox() throws Exception {
        assertRequest(
                post("/INFOR_SCPRD_wmwhse1/inbound/get-inbound"),
                status().isOk(),
                "service/inbound/get/correct-returns/request.json",
                "service/inbound/get/correct-returns/response-unknown-box.json"
        );
    }

    @Test
    @DatabaseSetup(value = "/service/inbound/get/correct-returns/initial.xml", connection = "wmwhseConnection")
    @DatabaseSetup(value = "/service/inbound/get/correct-returns/receipt-and-details-with-cis.xml",
            connection = "wmwhseConnection")
    @DatabaseSetup(value = "/service/inbound/get/correct-returns/storer-sku-and-related.xml",
            connection = "wmwhseConnection")
    @DatabaseSetup(value = "/service/inbound/db/receipt-and-details-status-history.xml",
            connection = "wmwhseConnection")
    public void getInboundReturnsWithCisItemsFromDifferentStocks() throws Exception {
        assertRequest(
                post("/INFOR_SCPRD_wmwhse1/inbound/get-inbound"),
                status().isOk(),
                "service/inbound/get/correct-returns/request.json",
                "service/inbound/get/correct-returns/response-with-cis.json"
        );
    }

    @Test
    @DatabaseSetup(value = "/service/inbound/get/unredeemed/before.xml", connection = "wmwhseConnection")
    public void getInboundUnredeemedWithIdentitiesFromItemsOfTheSameSkuButDifferentOrders() throws Exception {
        assertRequest(
                post("/INFOR_SCPRD_wmwhse1/inbound/get-inbound"),
                status().isOk(),
                "service/inbound/get/unredeemed/request.json",
                "service/inbound/get/unredeemed/response.json"
        );
    }

    @Test
    @DatabaseSetup(value = "/service/inbound/get/correct-returns/initial.xml", connection = "wmwhseConnection")
    @DatabaseSetup(value = "/service/inbound/get/correct-returns/receipt-and-details-updatable-initial.xml",
            connection = "wmwhseConnection")
    @DatabaseSetup(value = "/service/inbound/get/correct-returns/storer-sku-and-related.xml",
            connection = "wmwhseConnection")
    @DatabaseSetup(value = "/service/inbound/db/receipt-and-details-status-history.xml",
            connection = "wmwhseConnection")
    public void getInboundUpdatableReturnsInitial() throws Exception {
        assertRequest(
                post("/INFOR_SCPRD_wmwhse1/inbound/get-inbound"),
                status().isOk(),
                "service/inbound/get/correct-returns/request.json",
                "service/inbound/get/correct-returns/response-updatable-initial.json"
        );
    }

    @Test
    @DatabaseSetup(value = "/service/inbound/get/correct/initial.xml", connection = "wmwhseConnection")
    @DatabaseSetup(value = "/service/inbound/get/correct/anomalies.xml", connection = "wmwhseConnection")
    @DatabaseSetup(value = "/service/inbound/get/correct/receipt-and-details-with-anomaly.xml",
            connection = "wmwhseConnection")
    @DatabaseSetup(value = "/service/inbound/db/storer-sku-and-related.xml", connection = "wmwhseConnection")
    @DatabaseSetup(value = "/service/inbound/db/receipt-and-details-status-history.xml",
            connection = "wmwhseConnection")
    @DatabaseSetup(value = "/service/inbound/get/assortment/assortment.xml", connection = "wmwhseConnection")
    public void getInboundAssortment() throws Exception {
        assertRequest(
                post("/INFOR_SCPRD_wmwhse1/inbound/get-inbound"),
                status().isOk(),
                "service/inbound/get/correct/request.json",
                "service/inbound/get/assortment/response.json"
        );
    }

    @Test
    @DisplayName("putInbound возвратной поставки")
    @ExpectedDatabase(value = "/controller/putInbound/returns/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void putInboundReturns() throws Exception {
        assertRequest(
                post("/INFOR_SCPRD_wmwhse1/inbound/returns"),
                status().isOk(),
                "controller/putInbound/returns/request.json",
                "controller/putInbound/returns/response.json"
        );
        verify(receiptDao, never()).delete("0000000001");
        verify(receiptStatusHistoryDao, never()).delete("0000000001");
        verify(receiptDao).insert(any(), any(), anyString());
        verify(receiptStatusHistoryDao).insert(any(), any());
    }

    @Test
    @DisplayName("putInbound возвратной поставки, поставка уже существует в статусе NEW")
    @DatabaseSetup(value = "/controller/putInbound/returns/already-exists-new/before.xml",
            connection = "wmwhseConnection")
    @ExpectedDatabase(value = "/controller/putInbound/returns/already-exists-new/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void putInboundReturnsAlreadyExistsNew() throws Exception {
        assertRequest(
                post("/INFOR_SCPRD_wmwhse1/inbound/returns"),
                status().isOk(),
                "controller/putInbound/returns/already-exists-new/request.json",
                "controller/putInbound/returns/already-exists-new/response.json"
        );
        verify(receiptDao).lockReceiptByExternalKey(anyString());
        verify(receiptDao).updateReceiptDateAndNotes(any(), anyString());
    }

    @Test
    @DisplayName("putInbound возвратной поставки, поставка уже существует, но статус не NEW")
    @DatabaseSetup(value = "/controller/putInbound/returns/already-exists-wrong-status/before.xml",
            connection = "wmwhseConnection")
    @ExpectedDatabase(value = "/controller/putInbound/returns/already-exists-wrong-status/before.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void putInboundReturnsAlreadyExistsWrongStatus() throws Exception {
        assertRequest(
                post("/INFOR_SCPRD_wmwhse1/inbound/returns"),
                status().isOk(),
                "controller/putInbound/returns/already-exists-wrong-status/request.json",
                "controller/putInbound/returns/already-exists-wrong-status/response.json"
        );
        verify(receiptDao, never()).delete("0000000001");
        verify(receiptStatusHistoryDao, never()).delete("0000000001");
        verify(receiptDao, never()).insert(any(), any(), anyString());
        verify(receiptStatusHistoryDao, never()).insert(any(), any());
    }

    @Test
    @DisplayName("putInbound возвратной поставки, поставка уже существует, другой тип поставки в запросе")
    @DatabaseSetup(value = "/controller/putInbound/returns/already-exists-wrong-type/before.xml",
            connection = "wmwhseConnection")
    @ExpectedDatabase(value = "/controller/putInbound/returns/already-exists-wrong-type/before.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void putInboundReturnsAlreadyExistsWrongType() throws Exception {
        assertRequest(
                post("/INFOR_SCPRD_wmwhse1/inbound/returns"),
                status().isOk(),
                "controller/putInbound/returns/already-exists-wrong-type/request.json",
                "controller/putInbound/returns/already-exists-wrong-type/response.json"
        );
        verify(receiptDao, never()).delete("0000000001");
        verify(receiptStatusHistoryDao, never()).delete("0000000001");
        verify(receiptDao, never()).insert(any(), any(), anyString());
        verify(receiptStatusHistoryDao, never()).insert(any(), any());
    }

    @Test
    @DisplayName("Автоматическая допоставка возвратного потока")
    @DatabaseSetup(value = "/controller/putInbound/returns/parent-id/before.xml",
            connection = "wmwhseConnection")
    @ExpectedDatabase(value = "/controller/putInbound/returns/parent-id/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void putInboundReturnsWithParentReceiptKey() throws Exception {
        assertRequest(
                post("/INFOR_SCPRD_wmwhse1/inbound/returns"),
                status().isOk(),
                "controller/putInbound/returns/parent-id/request.json",
                "controller/putInbound/returns/parent-id/response.json"
        );
        verify(receiptDao, never()).delete("0000000001");
        verify(receiptStatusHistoryDao, never()).delete("0000000001");
        verify(receiptDao).insert(any(), any(), anyString());
        verify(receiptStatusHistoryDao).insert(any(), any());
    }

    @Test
    @DisplayName("putInbound обновляемой возвратной поставки")
    @ExpectedDatabase(value = "/controller/putInbound/returns/updatable/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void putInboundUpdatableReturns() throws Exception {
        assertRequest(
                post("/INFOR_SCPRD_wmwhse1/inbound/returns"),
                status().isOk(),
                "controller/putInbound/returns/updatable/request.json",
                "controller/putInbound/returns/updatable/response.json"
        );
        verify(receiptDao, never()).delete("0000000001");
        verify(receiptStatusHistoryDao, never()).delete("0000000001");
        verify(receiptDao).insert(any(), any(), anyString());
        verify(receiptStatusHistoryDao).insert(any(), any());
    }

    @Test
    @DatabaseSetup("/controller/update-inbound/before/common.xml")
    @ExpectedDatabase(value = "/controller/update-inbound/after/updeted-expected-recepit-date.xml",
            assertionMode = NON_STRICT)
    public void updateExpectedReceiptDate() throws Exception {
        assertRequest(
                post("/INFOR_SCPRD_wmwhse1/inbound/0000000001/update"),
                status().isOk(),
                "controller/update-inbound/request/base.json",
                "controller/update-inbound/response/expected.json");
    }

    @Test
    @DatabaseSetup("/controller/update-inbound/before/common.xml")
    @ExpectedDatabase(value = "/controller/update-inbound/before/common.xml", assertionMode = NON_STRICT)
    public void updateAbsentExpectedReceiptDate()
            throws Exception {
        mockMvc.perform(post("/INFOR_SCPRD_wmwhse1/inbound/NOT_EXISTENT_KEY/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent("controller/update-inbound/request/base.json")))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @DatabaseSetup("/controller/update-inbound/before/common.xml")
    @ExpectedDatabase(value = "/controller/update-inbound/before/common.xml", assertionMode = NON_STRICT)
    public void updateCancelExpectedReceiptDate()
            throws Exception {
        mockMvc.perform(post("/INFOR_SCPRD_wmwhse1/inbound/0000000002/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent("controller/update-inbound/request/base.json")))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @DatabaseSetup(value = "/controller/inbound-registry/additional-delivery/1/initial_db.xml", connection =
            "wmwhseConnection")
    @ExpectedDatabase(value = "/controller/inbound-registry/additional-delivery/1/expected_db.xml", connection =
            "wmwhseConnection",
            assertionMode = NON_STRICT_UNORDERED)
    public void doAdditionalDeliveryHappyPath() throws Exception {
        mockMvc.perform(put("/INFOR_SCPRD_wmwhse1/inbound/registry")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent(
                                "controller/inbound-registry/additional-delivery/1/inboundRegistryRequest.json")))
                .andExpect(status().isOk());
    }

    @Test
    @DatabaseSetup(value = "/controller/inbound-registry/additional-delivery/3/initial_db.xml", connection =
            "wmwhseConnection")
    @ExpectedDatabase(value = "/controller/inbound-registry/additional-delivery/3/expected.xml", connection =
            "wmwhseConnection",
            assertionMode = NON_STRICT_UNORDERED)
    public void doAdditionalDeliveryLotNotConsolidated() throws Exception {
        mockMvc.perform(put("/INFOR_SCPRD_wmwhse1/inbound/registry")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent(
                                "controller/inbound-registry/additional-delivery/3/inboundRegistryRequest.json")))
                .andExpect(status().isOk()).andReturn();
    }

    @Test
    @DatabaseSetup(value = "/controller/inbound-registry/additional-delivery/4/initial_db.xml", connection =
            "wmwhseConnection")
    @ExpectedDatabase(value = "/controller/inbound-registry/additional-delivery/4/expected_db.xml",
            connection = "wmwhseConnection", assertionMode = NON_STRICT_UNORDERED)
    public void doAdditionalDeliveryLot2AnomalyBoxes() throws Exception {
        mockMvc.perform(put("/INFOR_SCPRD_wmwhse1/inbound/registry")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent(
                                "controller/inbound-registry/additional-delivery/4/inboundRegistryRequest.json")))
                .andExpect(status().isOk());
    }

    @Test
    @DatabaseSetup(value = "/controller/inbound-registry/additional-delivery/5/initial_db.xml", connection =
            "wmwhseConnection")
    @ExpectedDatabase(value = "/controller/inbound-registry/additional-delivery/5/expected_db.xml",
            connection = "wmwhseConnection", assertionMode = NON_STRICT_UNORDERED)
    public void doAdditionalDeliveryWithNotSpecifiedLots() throws Exception {
        mockMvc.perform(put("/INFOR_SCPRD_wmwhse1/inbound/registry")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent(
                                "controller/inbound-registry/additional-delivery/5/inboundRegistryRequest.json")))
                .andExpect(status().isOk());
    }

    @Test
    @DatabaseSetup(value = "/controller/inbound-registry/additional-delivery/8/initial_db.xml", connection =
            "wmwhseConnection")
    @ExpectedDatabase(value = "/controller/inbound-registry/additional-delivery/8/expected_db.xml",
            connection = "wmwhseConnection", assertionMode = NON_STRICT_UNORDERED)
    public void anomalyBoxStatusShouldChangeOnlyForItemsFromRequest() throws Exception {
        mockMvc.perform(put("/INFOR_SCPRD_wmwhse1/inbound/registry")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent(
                                "controller/inbound-registry/additional-delivery/8/inboundRegistryRequest.json")))
                .andExpect(status().isOk());
    }

    @Test
    @DatabaseSetup(value = "/controller/inbound-registry/additional-delivery/9/initial_db.xml", connection =
            "wmwhseConnection")
    @ExpectedDatabase(value = "/controller/inbound-registry/additional-delivery/9/expected_db.xml",
            connection = "wmwhseConnection", assertionMode = NON_STRICT_UNORDERED)
    public void validCisWithoutCryptoTailShouldBeSaved() throws Exception {
        mockMvc.perform(put("/INFOR_SCPRD_wmwhse1/inbound/registry")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent(
                                "controller/inbound-registry/additional-delivery/9/inboundRegistryRequest.json")))
                .andExpect(status().isOk());
    }

    @Test
    @DatabaseSetup(value = "/controller/inbound-registry/additional-delivery/11/initial_db.xml", connection =
            "wmwhseConnection")
    @ExpectedDatabase(value = "/controller/inbound-registry/additional-delivery/11/expected_db.xml",
            connection = "wmwhseConnection", assertionMode = NON_STRICT_UNORDERED)
    public void anomalyContainerShouldBeUniqueInReceiptDetail() throws Exception {
        mockMvc.perform(put("/INFOR_SCPRD_wmwhse1/inbound/registry")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent(
                                "controller/inbound-registry/additional-delivery/11/inboundRegistryRequest.json")))
                .andExpect(status().isOk()).andReturn();
    }

    @Test
    @DatabaseSetup(value = "/controller/inbound-registry/additional-delivery/12/initial_db.xml", connection =
            "wmwhseConnection")
    @ExpectedDatabase(value = "/controller/inbound-registry/additional-delivery/12/expected_db.xml",
            connection = "wmwhseConnection", assertionMode = NON_STRICT_UNORDERED)
    public void registryShouldBeCreatedForAnomalyFromAnotherChild() throws Exception {
        mockMvc.perform(put("/INFOR_SCPRD_wmwhse1/inbound/registry")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent(
                                "controller/inbound-registry/additional-delivery/12/inboundRegistryRequest.json")))
                .andExpect(status().isOk()).andReturn();
    }

    @Test
    @DatabaseSetup(value = "/controller/inbound-registry/additional-delivery/13/initial_db.xml", connection =
            "wmwhseConnection")
    @ExpectedDatabase(value = "/controller/inbound-registry/additional-delivery/13/expected_db.xml",
            connection = "wmwhseConnection", assertionMode = NON_STRICT_UNORDERED)
    public void doAdditionalDeliveryAssortment() throws Exception {
        mockMvc.perform(put("/INFOR_SCPRD_wmwhse1/inbound/registry")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent(
                                "controller/inbound-registry/additional-delivery/13/inboundRegistryRequest.json")))
                .andExpect(status().isOk()).andReturn();
    }

    @Test
    @DatabaseSetup(value = "/controller/inbound-registry/additional-delivery/14/initial_db.xml", connection =
            "wmwhseConnection")
    @ExpectedDatabase(value = "/controller/inbound-registry/additional-delivery/14/expected_db.xml",
            connection = "wmwhseConnection", assertionMode = NON_STRICT_UNORDERED)
    public void doAdditionalDeliveryWithAllLotsOfSku() throws Exception {
        mockMvc.perform(put("/INFOR_SCPRD_wmwhse1/inbound/registry")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent(
                                "controller/inbound-registry/additional-delivery/14/inboundRegistryRequest.json")))
                .andExpect(status().isOk()).andReturn();
    }

    @Test
    @DatabaseSetup(value = "/controller/inbound-registry/additional-delivery/15/initial_db.xml", connection =
            "wmwhseConnection")
    @ExpectedDatabase(value = "/controller/inbound-registry/additional-delivery/15/expected_db.xml",
            connection = "wmwhseConnection", assertionMode = NON_STRICT_UNORDERED)
    public void doAdditionalDeliveryWithUnrecognizedSkuLots() throws Exception {
        mockMvc.perform(put("/INFOR_SCPRD_wmwhse1/inbound/registry")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent(
                                "controller/inbound-registry/additional-delivery/15/inboundRegistryRequest.json")))
                .andExpect(status().isOk()).andReturn();
    }

    /*
     * Проверка и сохранение значений identity переданных в реестре
     * */
    @Test
    @DatabaseSetup(value = "/controller/inbound-registry/identity-in-registry/1/initial_db.xml", connection =
            "wmwhseConnection")
    @ExpectedDatabase(value = "/controller/inbound-registry/identity-in-registry/1/expected_db.xml", connection =
            "wmwhseConnection",
            assertionMode = NON_STRICT_UNORDERED)
    public void identitiesShouldBeCheckedAndSaved() throws Exception {
        mockMvc.perform(put("/INFOR_SCPRD_wmwhse1/inbound/registry")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent(
                                "controller/inbound-registry/identity-in-registry/1/inboundRegistryRequest.json")))
                .andExpect(status().isOk());
    }

    @Test
    @DatabaseSetup(value = "/controller/inbound-registry/identity-in-registry/2/initial_db.xml", connection =
            "wmwhseConnection")
    @ExpectedDatabase(value = "/controller/inbound-registry/identity-in-registry/2/expected_db.xml", connection =
            "wmwhseConnection",
            assertionMode = NON_STRICT_UNORDERED)
    public void putInboundRegistryDuplicateIdentitiesInRequest() throws Exception {
        assertError("controller/inbound-registry/identity-in-registry/2/inboundRegistryRequest.json",
                put("/INFOR_SCPRD_wmwhse1/inbound/registry"),
                "\"400 BAD_REQUEST \\\"Duplicated identities in request: IdentityFrontInfoDto(identity=test_sn",
                status().is4xxClientError());
    }

    @Test
    @DatabaseSetup(value = "/controller/inbound-registry/identity-in-registry/3/initial_db.xml", connection =
            "wmwhseConnection")
    @ExpectedDatabase(value = "/controller/inbound-registry/identity-in-registry/3/expected_db.xml", connection =
            "wmwhseConnection",
            assertionMode = NON_STRICT_UNORDERED)
    public void putInboundRegistryIdentityValidationError() throws Exception {
        assertError("controller/inbound-registry/identity-in-registry/3/inboundRegistryRequest.json",
                put("/INFOR_SCPRD_wmwhse1/inbound/registry"),
                "{\"message\":\"400 BAD_REQUEST \\\"Invalid IMEI: CRC error\\\"\",\"status\":\"BAD_REQUEST\"," +
                        "\"resourceType\":\"IMEI_INVALID_CRC\",\"wmsErrorCode\":\"IMEI_INVALID_CRC\"}",
                status().is4xxClientError());

    }

    @Test
    @DatabaseSetup(value = "/controller/inbound-registry/identity-in-registry/4/initial_db.xml", connection =
            "wmwhseConnection")
    @ExpectedDatabase(value = "/controller/inbound-registry/identity-in-registry/4/expected_db.xml", connection =
            "wmwhseConnection",
            assertionMode = NON_STRICT_UNORDERED)
    public void putInboundRegistryDuplicateIdentitiesInRequestDifferentItems() throws Exception {
        assertError("controller/inbound-registry/identity-in-registry/4/inboundRegistryRequest.json",
                put("/INFOR_SCPRD_wmwhse1/inbound/registry"),
                "Duplicated identities in request: IdentityFrontInfoDto(identity=test_sn_duplicated",
                status().is4xxClientError());
    }

    @Test
    @DisplayName("putInboundRegistry возвратной поставки заполняет externOrderKey, returnId, returnReasonId")
    @DatabaseSetup(value = "/controller/inbound-registry/returns/1/before.xml", connection = "wmwhseConnection")
    @ExpectedDatabase(value = "/controller/inbound-registry/returns/1/after.xml", connection = "wmwhseConnection",
            assertionMode = NON_STRICT_UNORDERED)
    public void putInboundRegistryReturns() throws Exception {
        assertRequest(put("/INFOR_SCPRD_wmwhse1/inbound/registry"),
                status().isOk(),
                "controller/inbound-registry/returns/1/request.json",
                "controller/inbound-registry/returns/1/response.json");
    }

    @Test
    @DisplayName("putInboundRegistry возвратной поставки заполняет externOrderKey, returnId, returnReasonId")
    @DatabaseSetup(value = "/controller/inbound-registry/returns/14/before.xml", connection = "wmwhseConnection")
    @ExpectedDatabase(value = "/controller/inbound-registry/returns/14/after.xml", connection = "wmwhseConnection",
            assertionMode = NON_STRICT_UNORDERED)
    public void putInboundRegistryReturnsUpdatable() throws Exception {
        assertRequest(put("/INFOR_SCPRD_wmwhse1/inbound/registry"),
                status().isOk(),
                "controller/inbound-registry/returns/14/request.json",
                "controller/inbound-registry/returns/14/response.json");
    }

    @Test
    @DisplayName("putInboundRegistry обновляемой возвратной поставки не заполняет идентификаторы")
    @DatabaseSetup(value = "/controller/inbound-registry/updatable-returns/1/before.xml",
            connection = "wmwhseConnection")
    @ExpectedDatabase(value = "/controller/inbound-registry/updatable-returns/1/after.xml",
            connection = "wmwhseConnection",
            assertionMode = NON_STRICT_UNORDERED)
    public void putInboundRegistryUpdatableReturns() throws Exception {
        assertRequest(put("/INFOR_SCPRD_wmwhse1/inbound/registry"),
                status().isOk(),
                "controller/inbound-registry/updatable-returns/1/request.json",
                "controller/inbound-registry/updatable-returns/1/response.json");
    }

    @Test
    @DisplayName("putInboundRegistry поставки невыкупов кидает ошибку если нет ORDER_ID для коробки")
    @DatabaseSetup(value = "/controller/inbound-registry/returns/2/before.xml", connection = "wmwhseConnection")
    @ExpectedDatabase(value = "/controller/inbound-registry/returns/2/before.xml", connection = "wmwhseConnection",
            assertionMode = NON_STRICT_UNORDERED)
    public void putInboundRegistryThrowsExceptionAbsentOrderForBox() throws Exception {
        assertError("controller/inbound-registry/returns/2/request.json",
                put("/INFOR_SCPRD_wmwhse1/inbound/registry"),
                "ORDER_ID isn't set for some boxes", status().is4xxClientError()

        );
    }

    @Test
    @DisplayName("putInboundRegistry возвратной поставки кидает ошибку если нет ORDER_ID для айтема")
    @DatabaseSetup(value = "/controller/inbound-registry/returns/3/before.xml", connection = "wmwhseConnection")
    @ExpectedDatabase(value = "/controller/inbound-registry/returns/3/before.xml", connection = "wmwhseConnection",
            assertionMode = NON_STRICT_UNORDERED)
    public void putInboundRegistryThrowsExceptionAbsentOrderForItem() throws Exception {
        assertError("controller/inbound-registry/returns/3/request.json",
                put("/INFOR_SCPRD_wmwhse1/inbound/registry"),
                "ORDER_ID isn't set for some items", status().is4xxClientError()
        );
    }

    @Test
    @DisplayName("putInboundRegistry возвратной поставки кидает ошибку если нет ORDER_RETURN_ID для айтема")
    @DatabaseSetup(value = "/controller/inbound-registry/returns/5/before.xml", connection = "wmwhseConnection")
    @ExpectedDatabase(value = "/controller/inbound-registry/returns/5/before.xml", connection = "wmwhseConnection",
            assertionMode = NON_STRICT_UNORDERED)
    public void putInboundRegistryReturnsAbsentItemReturnId() throws Exception {
        assertError("controller/inbound-registry/returns/5/request.json",
                put("/INFOR_SCPRD_wmwhse1/inbound/registry"),
                "ORDER_RETURN_ID isn't set for some items",
                status().is4xxClientError()
        );
    }

    @Test
    @DisplayName("putInboundRegistry заполняет returnReasonId и comments null если их нет для возвратов")
    @DatabaseSetup(value = "/controller/inbound-registry/returns/6/before.xml", connection = "wmwhseConnection")
    @ExpectedDatabase(value = "/controller/inbound-registry/returns/6/after.xml", connection = "wmwhseConnection",
            assertionMode = NON_STRICT_UNORDERED)
    public void putInboundRegistryReturnsAbsentItemReturnReasonIdAndComments() throws Exception {
        assertRequest(put("/INFOR_SCPRD_wmwhse1/inbound/registry"),
                status().isOk(),
                "controller/inbound-registry/returns/6/request.json",
                "controller/inbound-registry/returns/6/response.json");
    }

    @Test
    @DisplayName("putInboundRegistry возвратной с одинаковыми ску в двух разных возвратах одного заказа")
    @DatabaseSetup(value = "/controller/inbound-registry/returns/7/before.xml", connection = "wmwhseConnection")
    @ExpectedDatabase(value = "/controller/inbound-registry/returns/7/after.xml", connection = "wmwhseConnection",
            assertionMode = NON_STRICT_UNORDERED)
    public void putInboundRegistryReturnsSameSkusInDifferentReturns() throws Exception {
        assertRequest(put("/INFOR_SCPRD_wmwhse1/inbound/registry"),
                status().isOk(),
                "controller/inbound-registry/returns/7/request.json",
                "controller/inbound-registry/returns/7/response.json");
    }

    @Test
    @DisplayName("putInboundRegistry возвратной поставки объединяет уиты и причины возврата неск. unitId")
    @DatabaseSetup(value = "/controller/inbound-registry/returns/8/before.xml", connection = "wmwhseConnection")
    @ExpectedDatabase(value = "/controller/inbound-registry/returns/8/after.xml", connection = "wmwhseConnection",
            assertionMode = NON_STRICT_UNORDERED)
    public void putInboundRegistryReturnsUitsMerged() throws Exception {
        assertRequest(put("/INFOR_SCPRD_wmwhse1/inbound/registry"),
                status().isOk(),
                "controller/inbound-registry/returns/8/request.json",
                "controller/inbound-registry/returns/8/response.json");
    }

    @Test
    @DisplayName("В возвратной поставке несколько возвратов с экземплярами одной sku и с КИЗами")
    @DatabaseSetup(value = "/controller/inbound-registry/returns/9/before.xml", connection = "wmwhseConnection")
    @ExpectedDatabase(value = "/controller/inbound-registry/returns/9/after.xml", connection = "wmwhseConnection",
            assertionMode = NON_STRICT_UNORDERED)
    public void putInboundRegistryReturnsMultipleReturnsWithCisAndTheSameSku() throws Exception {
        assertRequest(put("/INFOR_SCPRD_wmwhse1/inbound/registry"),
                status().isOk(),
                "controller/inbound-registry/returns/9/request.json",
                "controller/inbound-registry/returns/9/response.json");
    }

    @Test
    @DisplayName("В одном возврате несколько экземпляров одного SKU с КИЗами")
    @DatabaseSetup(value = "/controller/inbound-registry/returns/10/before.xml", connection = "wmwhseConnection")
    @ExpectedDatabase(value = "/controller/inbound-registry/returns/10/after.xml", connection = "wmwhseConnection",
            assertionMode = NON_STRICT_UNORDERED)
    public void putInboundRegistryReturnsSeveralItemsWithCisInTheSameReturn() throws Exception {
        assertRequest(put("/INFOR_SCPRD_wmwhse1/inbound/registry"),
                status().isOk(),
                "controller/inbound-registry/returns/10/request.json",
                "controller/inbound-registry/returns/10/response.json");
    }

    @Test
    @DisplayName("Сохранение информации о дропшиповой коробке")
    @DatabaseSetup(value = "/controller/inbound-registry/returns/not-acceptable-box/before.xml", connection =
            "wmwhseConnection")
    @ExpectedDatabase(value = "/controller/inbound-registry/returns/not-acceptable-box/after.xml", connection =
            "wmwhseConnection",
            assertionMode = NON_STRICT_UNORDERED)
    public void putInboundRegistryNotAcceptableBox() throws Exception {
        assertRequest(put("/INFOR_SCPRD_wmwhse1/inbound/registry"),
                status().isOk(),
                "controller/inbound-registry/returns/not-acceptable-box/request.json",
                "controller/inbound-registry/returns/not-acceptable-box/response.json");

    }

    @Test
    @DisplayName("Сохранение информации о родительской палете для коробки")
    @DatabaseSetup(value = "/controller/inbound-registry/returns/id-for-box/before.xml", connection =
            "wmwhseConnection")
    @ExpectedDatabase(value = "/controller/inbound-registry/returns/id-for-box/after.xml", connection =
            "wmwhseConnection",
            assertionMode = NON_STRICT_UNORDERED)
    public void putInboundRegistryContainerIdForBox() throws Exception {
        assertRequest(put("/INFOR_SCPRD_wmwhse1/inbound/registry"),
                status().isOk(),
                "controller/inbound-registry/returns/id-for-box/request.json",
                "controller/inbound-registry/returns/id-for-box/response.json");

    }

    @Test
    @DisplayName("putInboundRegistry поставки невыкупа заполняет externOrderKey")
    @DatabaseSetup(value = "/controller/inbound-registry/unredeemed/1/before.xml", connection = "wmwhseConnection")
    @ExpectedDatabase(value = "/controller/inbound-registry/unredeemed/1/after.xml", connection = "wmwhseConnection",
            assertionMode = NON_STRICT_UNORDERED)
    public void putInboundRegistryUnredeemed() throws Exception {
        assertRequest(put("/INFOR_SCPRD_wmwhse1/inbound/registry"),
                status().isOk(),
                "controller/inbound-registry/unredeemed/1/request.json",
                "controller/inbound-registry/unredeemed/1/response.json");
    }

    /*
     * Обычная поставка, несколько услуг, в том числе SORT_BY_SKU
     * */
    @Test
    @DatabaseSetup(value = "/controller/inbound-registry/assortment/initial_common.xml", connection =
            "wmwhseConnection")
    @DatabaseSetup(value = "/controller/inbound-registry/assortment/1/initial_receipt.xml", connection =
            "wmwhseConnection")
    @ExpectedDatabase(value = "/controller/inbound-registry/assortment/1/expected_db.xml", connection =
            "wmwhseConnection",
            assertionMode = NON_STRICT_UNORDERED)
    public void putInboundAssortmentMultipleServices() throws Exception {
        mockMvc.perform(put("/INFOR_SCPRD_wmwhse1/inbound/registry")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent("controller/inbound-registry/assortment/1/inboundRegistryRequest" +
                                ".json")))
                .andExpect(status().isOk());
    }

    /*
     * Обычная поставка, нет ассортиментного SKU среди item'ов
     * */
    @Test
    @DatabaseSetup(value = "/controller/inbound-registry/assortment/initial_common.xml", connection =
            "wmwhseConnection")
    @DatabaseSetup(value = "/controller/inbound-registry/assortment/2/initial_receipt.xml", connection =
            "wmwhseConnection")
    @ExpectedDatabase(value = "/controller/inbound-registry/assortment/2/expected_db.xml", connection =
            "wmwhseConnection",
            assertionMode = NON_STRICT_UNORDERED)
    public void putInboundAssortmentMissingAssortmentSku() throws Exception {
        assertError("controller/inbound-registry/assortment/2/inboundRegistryRequest.json",
                put("/INFOR_SCPRD_wmwhse1/inbound/registry"),
                "Не определен ASSORTMENT_ARTICLE",
                status().is4xxClientError());
    }

    /*
     * поставка CROSSDOCK, но при этом есть услуга SORT_BY_SKU
     * */
    @Test
    @DatabaseSetup(value = "/controller/inbound-registry/assortment/initial_common.xml", connection =
            "wmwhseConnection")
    @DatabaseSetup(value = "/controller/inbound-registry/assortment/3/initial_receipt.xml", connection =
            "wmwhseConnection")
    @ExpectedDatabase(value = "/controller/inbound-registry/assortment/3/expected_db.xml", connection =
            "wmwhseConnection",
            assertionMode = NON_STRICT_UNORDERED)
    public void putInboundAssortmentWrongReceiptType() throws Exception {
        assertError("controller/inbound-registry/assortment/3/inboundRegistryRequest.json",
                put("/INFOR_SCPRD_wmwhse1/inbound/registry"),
                "не поддерживается для типа поставки",
                status().is4xxClientError());
    }

    /*
     * обычная поставка, есть ассортиментный SKU, есть SKU c привязанными услугами SORT_BY_SKU,
     * отсутствуют ASSORTMENT_ARTICLE
     * */
    @Test
    @DatabaseSetup(value = "/controller/inbound-registry/assortment/initial_common.xml", connection =
            "wmwhseConnection")
    @DatabaseSetup(value = "/controller/inbound-registry/assortment/4/initial_receipt.xml", connection =
            "wmwhseConnection")
    @ExpectedDatabase(value = "/controller/inbound-registry/assortment/4/expected_db.xml", connection =
            "wmwhseConnection",
            assertionMode = NON_STRICT_UNORDERED)
    public void putInboundAssortmentMissingAssortmentArticle() throws Exception {
        assertError("controller/inbound-registry/assortment/4/inboundRegistryRequest.json",
                put("/INFOR_SCPRD_wmwhse1/inbound/registry"),
                "Не задан ASSORTMENT_ARTICLE для SKU",
                status().is4xxClientError());
    }

    /*
     * Обычная поставка, несколько услуг, в том числе SORT_BY_SKU, дублирующиеся manufacturer SKU
     * */
    @Test
    @DatabaseSetup(value = "/controller/inbound-registry/assortment/initial_common.xml", connection =
            "wmwhseConnection")
    @DatabaseSetup(value = "/controller/inbound-registry/assortment/5/initial_receipt.xml", connection =
            "wmwhseConnection")
    @ExpectedDatabase(value = "/controller/inbound-registry/assortment/5/expected_db.xml", connection =
            "wmwhseConnection",
            assertionMode = NON_STRICT_UNORDERED)
    public void putInboundAssortmentMultipleServicesSameManufacturerSku() throws Exception {
        mockMvc.perform(put("/INFOR_SCPRD_wmwhse1/inbound/registry")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent("controller/inbound-registry/assortment/5/inboundRegistryRequest" +
                                ".json")))
                .andExpect(status().isOk());
    }

    /*
     * Повторный вызов  putInboundRegistry
     * */
    @Test
    @DatabaseSetup(value = "/controller/inbound-registry/assortment/initial_common.xml", connection =
            "wmwhseConnection")
    @DatabaseSetup(value = "/controller/inbound-registry/assortment/5/initial_receipt.xml", connection =
            "wmwhseConnection")
    @ExpectedDatabase(value = "/controller/inbound-registry/assortment/5/expected_db.xml", connection =
            "wmwhseConnection",
            assertionMode = NON_STRICT_UNORDERED)
    public void putInboundRegistryAssortmentRepeatedly() throws Exception {
        for (int i = 0; i < 2; i++) {
            mockMvc.perform(put("/INFOR_SCPRD_wmwhse1/inbound/registry")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(getFileContent("controller/inbound-registry/assortment/5/inboundRegistryRequest" +
                                    ".json")))
                    .andExpect(status().isOk());
        }
    }

    @Test
    @DisplayName("putInboundRegistry возвратной поставки кидает ошибку если есть дубли по SkuId+ReturnId")
    @DatabaseSetup(value = "/controller/inbound-registry/returns/11/before.xml", connection = "wmwhseConnection")
    public void putInboundReturnRepeatingSkuId() throws Exception {
        assertError("controller/inbound-registry/returns/11/request.json",
                put("/INFOR_SCPRD_wmwhse1/inbound/registry"),
                "repeating items", status().is4xxClientError()
        );
    }

    @Test
    @DisplayName("putInboundRegistry возвратной поставки без items")
    @DatabaseSetup(value = "/controller/inbound-registry/returns/12/before.xml", connection = "wmwhseConnection")
    @ExpectedDatabase(value = "/controller/inbound-registry/returns/12/after.xml", connection = "wmwhseConnection",
            assertionMode = NON_STRICT_UNORDERED)
    public void putInboundRegistryReturnsEmptyItems() throws Exception {
        assertRequest(put("/INFOR_SCPRD_wmwhse1/inbound/registry"),
                status().isOk(),
                "controller/inbound-registry/returns/12/request.json",
                "controller/inbound-registry/returns/12/response.json");
    }

    @Test
    @DisplayName("putInboundRegistry возвратной поставки без items")
    @DatabaseSetup(value = "/controller/inbound-registry/returns/12/before.xml", connection = "wmwhseConnection")
    @ExpectedDatabase(value = "/controller/inbound-registry/returns/12/after.xml", connection = "wmwhseConnection",
            assertionMode = NON_STRICT_UNORDERED)
    public void putInboundRegistryReturnsNoItems() throws Exception {
        assertRequest(put("/INFOR_SCPRD_wmwhse1/inbound/registry"),
                status().isOk(),
                "controller/inbound-registry/returns/12/request.json",
                "controller/inbound-registry/returns/12/response.json");
    }

    @Test
    @DisplayName("putInboundRegistry невыкупа кидает ошибку если есть дубли по SkuId+OrderKey")
    @DatabaseSetup(value = "/controller/inbound-registry/unredeemed/2/before.xml", connection = "wmwhseConnection")
    public void putInboundUnredeemedRepeatingSkuId() throws Exception {
        assertError("controller/inbound-registry/unredeemed/2/request.json",
                put("/INFOR_SCPRD_wmwhse1/inbound/registry"),
                "repeating items", status().is4xxClientError()
        );
    }

    @Test
    @DisplayName("putInboundRegistry невыкупа кидает ошибку если есть дубли по BoxId")
    @DatabaseSetup(value = "/controller/inbound-registry/unredeemed/duplicate-container-ids/before.xml", connection =
            "wmwhseConnection")
    public void putInboundUnredeemedRepeatingBoxIds() throws Exception {
        assertError("controller/inbound-registry/unredeemed/duplicate-container-ids/request.json",
                put("/INFOR_SCPRD_wmwhse1/inbound/registry"),
                "Some boxes have the same ids", status().is4xxClientError()
        );
    }

    @Test
    @DisplayName("putInboundRegistry невыкупа НЕ кидает ошибку если есть разные коробки из одного заказа")
    @DatabaseSetup(value = "/controller/inbound-registry/unredeemed/multiple-container-ids-in-one-order/before.xml",
            connection = "wmwhseConnection")
    public void putInboundUnredeemedWithMultipleBoxesInOneOrder() throws Exception {
        assertRequest(put("/INFOR_SCPRD_wmwhse1/inbound/registry"),
                status().isOk(),
                "controller/inbound-registry/unredeemed/multiple-container-ids-in-one-order/request.json",
                "controller/inbound-registry/unredeemed/multiple-container-ids-in-one-order/response.json");
    }

    @Test
    @DisplayName("putInboundRegistry для нескеольких товаров с разными UnitCountType")
    @DatabaseSetup(value = "/controller/inbound-registry/returns/15/before.xml", connection = "wmwhseConnection")
    @ExpectedDatabase(value = "/controller/inbound-registry/returns/15/after.xml", connection = "wmwhseConnection",
            assertionMode = NON_STRICT_UNORDERED)
    public void putInboundRegistryWithDifferentUnitCountType() throws Exception {
        assertRequest(put("/INFOR_SCPRD_wmwhse1/inbound/registry"),
                status().isOk(),
                "controller/inbound-registry/returns/15/request.json",
                "controller/inbound-registry/returns/15/response.json");
    }

    @Test
    @DisplayName("putInboundRegistry поставки межсклада просрока")
    @DatabaseSetup(value = "/controller/inbound-registry/interwarehouse/1/before.xml", connection = "wmwhseConnection")
    @ExpectedDatabase(value = "/controller/inbound-registry/interwarehouse/1/after.xml",
            connection = "wmwhseConnection", assertionMode = NON_STRICT_UNORDERED)
    public void putInboundRegistryInterwarehouseExpired() throws Exception {
        assertRequest(put("/INFOR_SCPRD_wmwhse1/inbound/registry"),
                status().isOk(),
                "controller/inbound-registry/interwarehouse/1/request.json",
                "controller/inbound-registry/interwarehouse/1/response.json");
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

    private void assertError(String requestFile, MockHttpServletRequestBuilder request, String errorInfo,
                             ResultMatcher resultMatcher)
            throws Exception {
        MvcResult mvcResult = mockMvc.perform(request
                        .cookie(new Cookie(AuthenticationParam.USERNAME.getCode(), "TEST"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent(requestFile)))
                .andExpect(resultMatcher)
                .andReturn();
        assertions.assertThat(mvcResult.getResponse().getContentAsString(StandardCharsets.UTF_8)).contains(errorInfo);
    }

}
