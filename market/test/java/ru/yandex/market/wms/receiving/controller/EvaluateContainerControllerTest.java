package ru.yandex.market.wms.receiving.controller;

import java.util.Collection;
import java.util.Set;

import javax.servlet.http.Cookie;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.invocation.Invocation;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.Description;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.logistics.cte.client.FulfillmentCteClientApi;
import ru.yandex.market.logistics.cte.client.dto.QualityAttributeDTO;
import ru.yandex.market.logistics.cte.client.dto.QualityAttributesResponseDTO;
import ru.yandex.market.logistics.cte.client.dto.TransportationUnitRequestDTO;
import ru.yandex.market.logistics.cte.client.enums.QualityAttributeType;
import ru.yandex.market.logistics.cte.client.enums.UnitType;
import ru.yandex.market.wms.common.model.enums.AuthenticationParam;
import ru.yandex.market.wms.receiving.ReceivingIntegrationTest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.wms.common.spring.utils.FileContentUtils.getFileContent;

@SpyBean(value = {
        FulfillmentCteClientApi.class,
})
class EvaluateContainerControllerTest extends ReceivingIntegrationTest {

    @Autowired
    protected FulfillmentCteClientApi cteClient;

    @BeforeEach
    public void init() {
        super.init();
        mockCte();
    }

    @AfterEach
    public void after() {
        Mockito.reset(cteClient);
    }

    @Test
    @DatabaseSetup("/controller/evaluate-container-controller/2/before.xml")
    @ExpectedDatabase(value = "/controller/evaluate-container-controller/2/after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    void cteShouldBeNotified() throws Exception {
        Mockito.doAnswer((Answer<Void>) invocation -> {
            Object[] args = invocation.getArguments();
            Long yandexSupplyId = (Long) args[0];
            TransportationUnitRequestDTO requestDTO = (TransportationUnitRequestDTO) args[1];
            Assertions.assertEquals(1622645458L, yandexSupplyId);

            Assertions.assertEquals("box_barcode", requestDTO.getLabel());
            Assertions.assertEquals("order234567", requestDTO.getMeta().get("order_id"));
            Assertions.assertEquals(2, requestDTO.getQualityAttributeIds().size());
            return null;
        }).when(cteClient).evaluateTransportationUnit(anyLong(), any());

        MvcResult result = mockMvc.perform(post("/quality-check/container-assessment-upload")
                .cookie(new Cookie(AuthenticationParam.USERNAME.getCode(), "TEST"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("controller/evaluate-container-controller/2/request.json")))
                .andExpect(status().isOk()).andReturn();

        Collection<Invocation> invocations = Mockito.mockingDetails(cteClient).getInvocations();
        int numberOfCalls = invocations.size();
        Assertions.assertEquals(1, numberOfCalls);
    }

    @Test
    @DatabaseSetup("/controller/evaluate-container-controller/3/before.xml")
    @ExpectedDatabase()
    void wrongReceiptTypeShouldNotPerformCTECall() throws Exception {
        MvcResult result = mockMvc.perform(post("/quality-check/container-assessment-upload")
                .cookie(new Cookie(AuthenticationParam.USERNAME.getCode(), "TEST"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("controller/evaluate-container-controller/3/request.json")))
                .andExpect(status().isBadRequest()).andReturn();

        String content = result.getResponse().getContentAsString();
        Assertions.assertTrue(content.contains("Unexpected receipt type"));
    }

    @Test
    @DatabaseSetup("/controller/evaluate-container-controller/4/before.xml")
    void getContainerQualityAttributesFromCte() throws Exception {

        // /quality-check/{receipt-id}/container/{container-barcode}/quality-attributes
        MvcResult result = mockMvc.perform(
                get("/quality-check/receiptId/container/box_barcode/quality-attributes")
                .cookie(new Cookie(AuthenticationParam.USERNAME.getCode(), "TEST"))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andExpect(content().json(
                        getFileContent("controller/evaluate-container-controller/4/response.json")))
                .andReturn();

        Collection<Invocation> invocations = Mockito.mockingDetails(cteClient).getInvocations();
        int numberOfCalls = invocations.size();
        Assertions.assertEquals(1, numberOfCalls);
    }

    @Test
    @DatabaseSetup("/controller/evaluate-container-controller/5/before.xml")
    @ExpectedDatabase(value = "/controller/evaluate-container-controller/5/after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    void containerStatusShouldBeChangedAfterInitialReceiving() throws Exception {
        Mockito.doAnswer((Answer<Void>) invocation -> {
            Object[] args = invocation.getArguments();
            Long yandexSupplyId = (Long) args[0];
            TransportationUnitRequestDTO requestDTO = (TransportationUnitRequestDTO) args[1];
            Assertions.assertEquals(1622645458L, yandexSupplyId);
            Assertions.assertEquals("box_barcode", requestDTO.getLabel());
            Assertions.assertEquals("order234567", requestDTO.getMeta().get("order_id"));
            Assertions.assertEquals(2, requestDTO.getQualityAttributeIds().size());
            return null;
        }).when(cteClient).evaluateTransportationUnit(anyLong(), any());

        MvcResult result = mockMvc.perform(post("/quality-check/container-assessment-upload")
                .cookie(new Cookie(AuthenticationParam.USERNAME.getCode(), "TEST"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("controller/evaluate-container-controller/5/request.json")))
                .andExpect(status().isOk()).andReturn();

        Collection<Invocation> invocations = Mockito.mockingDetails(cteClient).getInvocations();
        int numberOfCalls = invocations.size();
        Assertions.assertEquals(1, numberOfCalls);
    }

    @Test
    @DatabaseSetup("/controller/evaluate-container-controller/6/before.xml")
    @ExpectedDatabase(value = "/controller/evaluate-container-controller/6/after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    void onlyPalletsWithStatusNewShouldBeReceivable() throws Exception {
        MvcResult result = mockMvc.perform(post("/quality-check/container-assessment-upload")
                .cookie(new Cookie(AuthenticationParam.USERNAME.getCode(), "TEST"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("controller/evaluate-container-controller/6/request.json")))
                .andExpect(status().isBadRequest()).andReturn();

        Assertions.assertTrue(result.getResponse().getContentAsString()
                .contains("CONTAINER_IN_WRONG_STATUS"));
    }

    @Test
    @DatabaseSetup("/controller/evaluate-container-controller/7/before.xml")
    @ExpectedDatabase(value = "/controller/evaluate-container-controller/7/before.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    void duplicateIdOnBalanceShouldFail() throws Exception {
        MvcResult result = mockMvc.perform(post("/quality-check/container-assessment-upload")
                        .cookie(new Cookie(AuthenticationParam.USERNAME.getCode(), "TEST"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent("controller/evaluate-container-controller/7/request.json")))
                .andExpect(status().isBadRequest()).andReturn();

        Assertions.assertTrue(result.getResponse().getContentAsString()
                .contains("The container box_barcode is already on the warehouse balance."));
    }

    @Test
    @DisplayName("Запретить принимать дропшиповую коробку")
    @DatabaseSetup("/controller/evaluate-container-controller/not-acceptable-box/before.xml")
    @ExpectedDatabase(value = "/controller/evaluate-container-controller/not-acceptable-box/before.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    void notAcceptableBoxShouldFail() throws Exception {
        MvcResult result = mockMvc.perform(
                        get("/quality-check/receiptId/container/box_barcode/quality-attributes")
                                .cookie(new Cookie(AuthenticationParam.USERNAME.getCode(), "TEST"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(getFileContent("controller/evaluate-container-controller/" +
                                        "not-acceptable-box/request.json")))
                .andExpect(status().isBadRequest()).andReturn();

        String content = result.getResponse().getContentAsString();
        Assertions.assertTrue(content.contains("Not acceptable container"));
        Assertions.assertTrue(content.contains("Return to Sort Centre"));
    }

    // initial receiving BY_PALLET, when pallet is scanned
    @Test
    @DatabaseSetup("/controller/evaluate-container-controller/receiving-by-pallet/before.xml")
    @ExpectedDatabase(value = "/controller/evaluate-container-controller/receiving-by-pallet/before.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    void getQualityAttributesForPallet() throws Exception {
        Mockito.doAnswer((Answer<QualityAttributesResponseDTO>) invocation -> {
            Object[] args = invocation.getArguments();
            UnitType unitType = (UnitType) args[0];
            Assertions.assertEquals(UnitType.PALLET, unitType);
            return new QualityAttributesResponseDTO(getQualityAttrSet());

        }).when(cteClient).resolveQualityAttributes(any(), any());

        mockMvc.perform(
                get("/quality-check/receiptId/container/DRP123/quality-attributes")
                        .cookie(new Cookie(AuthenticationParam.USERNAME.getCode(), "TEST"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent("controller/evaluate-container-controller/" +
                                "receiving-by-pallet/request.json")))
                .andExpect(status().isOk());
    }

    // initial receiving BY_PALLET, when box is scanned
    @Test
    @DatabaseSetup("/controller/evaluate-container-controller/receiving-by-pallet/before.xml")
    @ExpectedDatabase(value = "/controller/evaluate-container-controller/receiving-by-pallet/before.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    void getQualityAttributesForBoxInCaseByPalletReceiving() throws Exception {
        MvcResult result = mockMvc.perform(
                get("/quality-check/receiptId/container/RET_FF_123/quality-attributes")
                        .cookie(new Cookie(AuthenticationParam.USERNAME.getCode(), "TEST"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent("controller/evaluate-container-controller/" +
                                "receiving-by-pallet/request-box.json")))
                .andExpect(status().isBadRequest()).andReturn();

        Assertions.assertTrue(result.getResponse().getContentAsString().contains("WRONG_CONTAINER_TYPE"));
    }

    // initial receiving BY_BOX, when box is scanned
    @Test
    @DatabaseSetup("/controller/evaluate-container-controller/receiving-by-box/before.xml")
    @ExpectedDatabase(value = "/controller/evaluate-container-controller/receiving-by-box/before.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    void getQualityAttributesForBoxOnInitialReceiving() throws Exception {
        mockMvc.perform(
                get("/quality-check/receiptId/container/RET_FF_123/quality-attributes")
                        .cookie(new Cookie(AuthenticationParam.USERNAME.getCode(), "TEST"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent("controller/evaluate-container-controller/" +
                                "receiving-by-pallet/request.json")))
                .andExpect(status().isOk());
    }

    // initial receiving BY_BOX, when pallet is scanned
    @Test
    @DatabaseSetup("/controller/evaluate-container-controller/receiving-by-box/before-when-pallet-declared.xml")
    @ExpectedDatabase(value = "/controller/evaluate-container-controller/receiving-by-box/" +
            "before-when-pallet-declared.xml", assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    void getQualityAttributesForPalletOnInitialReceivingInCaseOfBoxReceiving() throws Exception {
        MvcResult result = mockMvc.perform(
                get("/quality-check/receiptId/container/DRP123/quality-attributes")
                        .cookie(new Cookie(AuthenticationParam.USERNAME.getCode(), "TEST"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent("controller/evaluate-container-controller/" +
                                "receiving-by-pallet/request.json")))
                .andExpect(status().isBadRequest()).andReturn();

        Assertions.assertTrue(result.getResponse().getContentAsString().contains("WRONG_CONTAINER_TYPE"));
    }

    // secondary receiving with previous initial BY_PALLET, when box is scanned
    @Test
    @DatabaseSetup("/controller/evaluate-container-controller/receiving-by-pallet/before-box-secondary.xml")
    @ExpectedDatabase(value = "/controller/evaluate-container-controller/receiving-by-pallet/before-box-secondary.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    void getQualityAttributesForBoxOnSecondaryReceiving() throws Exception {
        mockMvc.perform(
                get("/quality-check/receiptId/container/RET_FF_123/quality-attributes")
                        .cookie(new Cookie(AuthenticationParam.USERNAME.getCode(), "TEST"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent("controller/evaluate-container-controller/" +
                                "receiving-by-pallet/request-box.json")))
                .andExpect(status().isOk());
    }

    // secondary receiving with previous initial BY_PALLET, when box is scanned
    @Test
    @DatabaseSetup("/controller/evaluate-container-controller/receiving-by-pallet/pallet-not-received-before.xml")
    @ExpectedDatabase(value = "/controller/evaluate-container-controller/receiving-by-pallet/pallet-not-received" +
            "-before.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    void checkIfPalletWasNotReceived() throws Exception {
        MvcResult result = mockMvc.perform(
                        get("/quality-check/receiptId/container/RET_FF_123/quality-attributes")
                                .cookie(new Cookie(AuthenticationParam.USERNAME.getCode(), "TEST"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(getFileContent("controller/evaluate-container-controller/" +
                                        "receiving-by-pallet/request-box.json")))
                .andExpect(status().isBadRequest()).andReturn();

        Assertions.assertTrue(result.getResponse().getContentAsString().contains("EMPTY_UNREDEEMED_BY_PALLET"));
    }

    // secondary receiving with previous initial BY_PALLET, when pallet is scanned
    @Description("При Первичке по палетам, вторичку по палетам надо запрещать")
    @Test
    @DatabaseSetup("/controller/evaluate-container-controller/receiving-by-pallet/before-pallet-secondary.xml")
    @ExpectedDatabase(value = "/controller/evaluate-container-controller/receiving-by-pallet/before-pallet-secondary" +
            ".xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    void getQualityAttributesForPalletOnSecondaryReceivingShouldFail() throws Exception {
        MvcResult result = mockMvc.perform(
                        get("/quality-check/receiptId/container/DRP123/quality-attributes")
                                .cookie(new Cookie(AuthenticationParam.USERNAME.getCode(), "TEST"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(getFileContent("controller/evaluate-container-controller/" +
                                        "receipt-wrong-status/request.json")))
                .andExpect(status().isBadRequest()).andReturn();

        Assertions.assertTrue(result.getResponse().getContentAsString().contains("WRONG_CONTAINER_TYPE"));
    }

    // secondary receiving with previous initial BY_BOX, when box is scanned
    @Test
    @DisplayName("Кидать ошибку для фронта если контейнер не в статусе NEW (для скипа экрана)")
    @DatabaseSetup("/controller/evaluate-container-controller/container-in-wrong-status/before.xml")
    @ExpectedDatabase(value = "/controller/evaluate-container-controller/container-in-wrong-status/before.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    void containerStatusShouldFail() throws Exception {
        MvcResult result = mockMvc.perform(
                get("/quality-check/receiptId/container/box_barcode/quality-attributes")
                        .cookie(new Cookie(AuthenticationParam.USERNAME.getCode(), "TEST"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent(
                                "controller/evaluate-container-controller/container-in-wrong-status/request" +
                                        ".json")))
                .andExpect(status().isBadRequest()).andReturn();

        Assertions.assertTrue(result.getResponse().getContentAsString()
                .contains("CONTAINER_IN_WRONG_STATUS"));
    }

    @Test
    @DatabaseSetup("/controller/evaluate-container-controller/auto-additional-receipt/before-first-box.xml")
    @ExpectedDatabase(value = "/controller/evaluate-container-controller/auto-additional-receipt/before-first-box.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    void secondaryReceivingOfTheFirstBoxInAutoAdditionalReceipt() throws Exception {
        mockMvc.perform(
                get("/quality-check/receiptId/container/RET_FF_123/quality-attributes")
                        .cookie(new Cookie(AuthenticationParam.USERNAME.getCode(), "TEST"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent("controller/evaluate-container-controller/" +
                                "auto-additional-receipt/request.json")))
                .andExpect(status().isOk());
    }

    @Test
    @DatabaseSetup("/controller/evaluate-container-controller/auto-additional-receipt/before-second-box.xml")
    @ExpectedDatabase(value = "/controller/evaluate-container-controller/auto-additional-receipt/before-second-box.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    void secondaryReceivingOfTheSecondBoxInAutoAdditionalReceipt() throws Exception {
        mockMvc.perform(
                get("/quality-check/receiptId/container/RET_FF_123/quality-attributes")
                        .cookie(new Cookie(AuthenticationParam.USERNAME.getCode(), "TEST"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent("controller/evaluate-container-controller/" +
                                "auto-additional-receipt/request.json")))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Запретить принимать items в поставку с статусами отличными от 0 и 6")
    @DatabaseSetup("/controller/evaluate-container-controller/receipt-wrong-status/before.xml")
    @ExpectedDatabase(value = "/controller/evaluate-container-controller/receipt-wrong-status/before.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    void receiptStatusShouldFail() throws Exception {
        MvcResult result = mockMvc.perform(
                        get("/quality-check/receiptId/container/box_barcode/quality-attributes")
                                .cookie(new Cookie(AuthenticationParam.USERNAME.getCode(), "TEST"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(getFileContent("controller/evaluate-container-controller/" +
                                        "receipt-wrong-status/request.json")))
                .andExpect(status().isBadRequest()).andReturn();

        Assertions.assertTrue(result.getResponse().getContentAsString()
                .contains("RECEIPT_IN_WRONG_STATUS"));
    }

    // вторичная приемка возвратной коробки в статусе PALLET_ACCEPTANCE,
    // при этом родительская тара в статусе RECEIVED_COMPLETE (коробка переоткрыта)
    @Test
    @DatabaseSetup("/controller/evaluate-container-controller/8/before.xml")
    @ExpectedDatabase(value = "/controller/evaluate-container-controller/8/before.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    void secondaryReceivingReturnReopenBox() throws Exception {
        MvcResult result = mockMvc.perform(
                        get("/quality-check/receiptId/container/VOZ_FF_123/quality-attributes")
                                .cookie(new Cookie(AuthenticationParam.USERNAME.getCode(), "TEST"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(getFileContent("controller/evaluate-container-controller/" +
                                        "8/request.json")))
                .andExpect(status().isBadRequest()).andReturn();

        Assertions.assertTrue(result.getResponse().getContentAsString()
                .contains("CONTAINER_IN_WRONG_STATUS"));
    }

    @Test
    @DatabaseSetup("/controller/evaluate-container-controller/receiving-by-pallet/before-box-secondary.xml")
    @ExpectedDatabase(value = "/controller/evaluate-container-controller/receiving-by-pallet/after-box-secondary.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    void receiveBoxSecondary() throws Exception {
        mockMvc.perform(post("/quality-check/container-assessment-upload")
                .cookie(new Cookie(AuthenticationParam.USERNAME.getCode(), "TEST"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("controller/evaluate-container-controller/receiving-by-pallet/" +
                                "request-box.json")))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Автоматически переводить статус поставки первичной приемки в 7 если приняты все паллеты")
    @DatabaseSetup("/controller/evaluate-container-controller/automatic-initial-close/before.xml")
    @ExpectedDatabase(value = "/controller/evaluate-container-controller/automatic-initial-close/after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    void automaticCloseInitialReceivingWithChangingStatus() throws Exception {
        ResultActions result = mockMvc.perform(post("/quality-check/container-assessment-upload")
                .cookie(new Cookie(AuthenticationParam.USERNAME.getCode(), "TEST"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent(
                        "controller/evaluate-container-controller/automatic-initial-close/request.json")));

        result.andExpect(status().isOk()).andReturn();
    }

    @Test
    @DisplayName("Автоматически не переводить статус поставки первичной приемки в 7 если приняты не все паллеты")
    @DatabaseSetup("/controller/evaluate-container-controller/automatic-initial-close/before-should-not-close.xml")
    @ExpectedDatabase(value = "/controller/evaluate-container-controller/automatic-initial-close/after-should-not" +
            "-close.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    void automaticCloseInitialReceivingWithoutChangingStatus() throws Exception {
        ResultActions result = mockMvc.perform(post("/quality-check/container-assessment-upload")
                .cookie(new Cookie(AuthenticationParam.USERNAME.getCode(), "TEST"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent(
                        "controller/evaluate-container-controller/automatic-initial-close/request.json")));

        result.andExpect(status().isOk()).andReturn();
    }

    private void mockCte() {
        Mockito.doNothing().when(cteClient).evaluateTransportationUnit(anyLong(), any());

        Mockito.doAnswer((Answer<QualityAttributesResponseDTO>) invocation -> {
            Object[] args = invocation.getArguments();
            UnitType unitType = (UnitType) args[0];
            Assertions.assertEquals(UnitType.BOX, unitType);
            return new QualityAttributesResponseDTO(getQualityAttrSet());

        }).when(cteClient).resolveQualityAttributes(any(), any());
    }

    @Test
    @DatabaseSetup("/controller/evaluate-container-controller/boxes-without-orders/before.xml")
    @ExpectedDatabase(value = "/controller/evaluate-container-controller/boxes-without-orders/after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    void passBoxesWithoutOrderId() throws Exception {
        Mockito.doAnswer((Answer<Void>) invocation -> {
            Object[] args = invocation.getArguments();
            Long yandexSupplyId = (Long) args[0];
            TransportationUnitRequestDTO requestDTO = (TransportationUnitRequestDTO) args[1];
            Assertions.assertEquals(1622645458L, yandexSupplyId);
            Assertions.assertEquals("box_barcode", requestDTO.getLabel());
            Assertions.assertNull(requestDTO.getMeta().get("order_id"));
            Assertions.assertEquals(2, requestDTO.getQualityAttributeIds().size());
            return null;
        }).when(cteClient).evaluateTransportationUnit(anyLong(), any());

        mockMvc.perform(post("/quality-check/container-assessment-upload")
                        .cookie(new Cookie(AuthenticationParam.USERNAME.getCode(), "TEST"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent(
                                "controller/evaluate-container-controller/boxes-without-orders/request.json")))
                .andExpect(status().isOk()).andReturn();

        Collection<Invocation> invocations = Mockito.mockingDetails(cteClient).getInvocations();
        int numberOfCalls = invocations.size();
        Assertions.assertEquals(1, numberOfCalls);
    }

    private Set<QualityAttributeDTO> getQualityAttrSet() {
        return Set.of(
                new QualityAttributeDTO(1L, "QA_1_name", "QA_1_title",
                        "QA_1_ref", QualityAttributeType.PACKAGE, "QA_1_description"),
                new QualityAttributeDTO(2L, "QA_2_name", "QA_2_title",
                        "QA_2_ref", QualityAttributeType.PACKAGE, "QA_2_description")
        );
    }
}
