package ru.yandex.market.wms.receiving.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import javax.servlet.http.Cookie;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.apache.commons.text.TextStringBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import ru.yandex.market.wms.common.model.enums.AnomalyLotStatus;
import ru.yandex.market.wms.common.model.enums.AuthenticationParam;
import ru.yandex.market.wms.common.spring.dao.entity.AnomalyContainer;
import ru.yandex.market.wms.common.spring.dao.entity.AnomalyLot;
import ru.yandex.market.wms.common.spring.enums.AnomalyCategory;
import ru.yandex.market.wms.common.spring.enums.ReceivingItemType;
import ru.yandex.market.wms.common.spring.utils.JsonAssertUtils;
import ru.yandex.market.wms.receiving.ReceivingIntegrationTest;
import ru.yandex.market.wms.receiving.model.enums.ScanningOperationType;
import ru.yandex.market.wms.receiving.service.ScanningOperationLog;
import ru.yandex.market.wms.shared.libs.label.printer.domain.pojo.ZplTemplateBuilder;
import ru.yandex.market.wms.shared.libs.label.printer.service.printer.AnomalyLabelPrinter;
import ru.yandex.market.wms.shared.libs.label.printer.service.printer.PrintService;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.wms.common.spring.utils.FileContentUtils.getFileContent;

public class AnomalyLotControllerTest extends ReceivingIntegrationTest {

    @SpyBean
    @Autowired
    private ScanningOperationLog scanningOperationLog;

    private static final String LABEL_TEMPLATE =
            "<>$@CONTAINER@$<>$@DATE@$<>$@RECEIPTKEY@$<>$@TYPE@$<>$@SUPPLIER@$<>";
    private static final String LABEL_TEMPLATE_RF =
            "<>$@CONTAINER@$<>$@RECEIPTKEY@$<>ANOMALY<>";

    private static final String LABEL_TEMPLATE_RF_SMALL =
            "small<>$@CONTAINER@$<>$@RECEIPTKEY@$<>ANOMALY<>";

    @SpyBean
    @Autowired
    private ZplTemplateBuilder templateBuilder;

    @SpyBean
    @Autowired
    private PrintService printService;

    @Override
    @BeforeEach
    public void init() {
        super.init();
        MockitoAnnotations.openMocks(this);
        Mockito.doAnswer(i -> new TextStringBuilder(LABEL_TEMPLATE)).when(templateBuilder)
                .getTemplate("anomaly_100x100.zpl");
        Mockito.doAnswer(i -> new TextStringBuilder(LABEL_TEMPLATE_RF)).when(templateBuilder)
                .getTemplate("ANOMALY_RF.zpl");
        Mockito.doAnswer(i -> new TextStringBuilder(LABEL_TEMPLATE_RF_SMALL)).when(templateBuilder)
                .getTemplate("ANOMALY_RF2.zpl");
        Mockito.doAnswer(i -> new TextStringBuilder(LABEL_TEMPLATE_RF)).when(templateBuilder)
                .getTemplate("ANOMALY_RF3.zpl");
    }

    @Test
    @DatabaseSetup("/controller/anomaly-lot/update-by-key/existing/db-before.xml")
    @ExpectedDatabase(value = "/controller/anomaly-lot/update-by-key/existing/db-after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void updateByExistingKey() throws Exception {
        callApi("controller/anomaly-lot/update-by-key/existing/request.json",
                "/anomaly/update-by-key");
    }

    @Test
    @DatabaseSetup("/controller/anomaly-lot/update-by-key/existing/multipleitemtypes/db-before.xml")
    @ExpectedDatabase(value = "/controller/anomaly-lot/update-by-key/existing/multipleitemtypes/db-after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void updateByExistingKeyWithMultipleItemtypes() throws Exception {
        callApi("controller/anomaly-lot/update-by-key/existing/multipleitemtypes/request.json",
                "/anomaly/update-by-key");
    }

    @Test
    @DatabaseSetup("/controller/anomaly-lot/update-by-key/wrong/db-before.xml")
    @ExpectedDatabase(value = "/controller/anomaly-lot/update-by-key/wrong/db-before.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void updateByWrongKey() throws Exception {
        callApiWithStatus("controller/anomaly-lot/update-by-key/wrong/request.json",
                "/anomaly/update-by-key", 404);
    }

    @Test
    @DatabaseSetup("/controller/anomaly-lot/set-anomaly-lot-category/db-before.xml")
    @ExpectedDatabase(value = "/controller/anomaly-lot/set-anomaly-lot-category/db-after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void setCorrectCategory() throws Exception {
        callApi("controller/anomaly-lot/set-anomaly-lot-category/requestCorrect.json",
                "/anomaly/set-anomaly-lot-category");
    }

    @Test
    @DatabaseSetup("/controller/anomaly-lot/set-anomaly-lot-category/db-before.xml")
    @ExpectedDatabase(value = "/controller/anomaly-lot/set-anomaly-lot-category/db-before.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void setIncorrectCategory() throws Exception {
        callApiWithStatus("controller/anomaly-lot/set-anomaly-lot-category/requestIncorrect.json",
                "/anomaly/set-anomaly-lot-category", 500);
    }

    @Test
    @DatabaseSetup("/controller/anomaly-lot/update-by-key/closed/immutable.xml")
    @ExpectedDatabase(value = "/controller/anomaly-lot/update-by-key/closed/immutable.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void shouldPreventUpdatingAnomalyOfClosedReceipt() throws Exception {
        callApiWithStatus("controller/anomaly-lot/update-by-key/existing/request.json",
                "/anomaly/update-by-key", 400);
    }

    @Test
    @DatabaseSetup("/controller/anomaly-lot/update-by-key/type-change/db-before.xml")
    public void shouldPreventUpdatingAnomalyType() throws Exception {
        callApiWithStatus("controller/anomaly-lot/update-by-key/type-change/request.json",
                "/anomaly/update-by-key", 400);
    }

    @Test
    @DatabaseSetup("/controller/anomaly-lot/delete-by-key/existing/db-before.xml")
    @ExpectedDatabase(value = "/controller/anomaly-lot/delete-by-key/existing/db-after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void deleteByExistingKey() throws Exception {
        callApi("controller/anomaly-lot/delete-by-key/existing/request.json",
                "/anomaly/delete-by-key");
    }

    @Test
    @DatabaseSetup("/controller/anomaly-lot/delete-by-key/last/db-before.xml")
    @ExpectedDatabase(value = "/controller/anomaly-lot/delete-by-key/last/db-after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void deleteLastLotByExistingKey() throws Exception {
        callApi("controller/anomaly-lot/delete-by-key/last/request.json",
                "/anomaly/delete-by-key");
    }

    @Test
    @DatabaseSetup("/controller/anomaly-lot/delete-by-key/wrong/db-before.xml")
    @ExpectedDatabase(value = "/controller/anomaly-lot/delete-by-key/wrong/db-before.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void deleteByWrongKey() throws Exception {
        callApiWithStatus("controller/anomaly-lot/delete-by-key/wrong/request.json",
                "/anomaly/delete-by-key", 404);
    }

    @Test
    @DatabaseSetup("/controller/anomaly-lot/delete-by-key/closed/immutable.xml")
    @ExpectedDatabase(value = "/controller/anomaly-lot/delete-by-key/closed/immutable.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void shouldPreventDeletingAnomalyOfClosedReceipt() throws Exception {
        callApiWithStatus("controller/anomaly-lot/delete-by-key/existing/request.json",
                "/anomaly/delete-by-key", 400);
    }

    @Test
    @DatabaseSetup("/controller/anomaly-lot/delete-by-key/last-with-discrepancies/db-before.xml")
    @ExpectedDatabase(value = "/controller/anomaly-lot/delete-by-key/last-with-discrepancies/db-after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void deleteLastLotByExistingKey_receipt_closedWithDiscrepancies_taskCreated() throws Exception {
        callApi("controller/anomaly-lot/delete-by-key/last-with-discrepancies/request.json",
                "/anomaly/delete-by-key");
    }

    @Test
    @DatabaseSetup("/controller/anomaly-lot/delete-by-key/last-and-no-containers-left/db-before.xml")
    @ExpectedDatabase(value = "/controller/anomaly-lot/delete-by-key/last-and-no-containers-left/db-after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void deleteLastLotByExistingKey_noContainersLeft_noTaskCreated() throws Exception {
        callApi("controller/anomaly-lot/delete-by-key/last-and-no-containers-left/request.json",
                "/anomaly/delete-by-key");
    }

    @Test
    @DatabaseSetup("/controller/anomaly-lot/delete-by-container-id/existing/db-before.xml")
    @ExpectedDatabase(value = "/controller/anomaly-lot/delete-by-container-id/existing/db-after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void deleteByExistingContainerId() throws Exception {
        callApi("controller/anomaly-lot/delete-by-container-id/existing/request.json",
                "/anomaly/delete-by-container-id");
    }

    @Test
    @DatabaseSetup("/controller/anomaly-lot/delete-by-container-id/consolidation/single/db-before.xml")
    @ExpectedDatabase(value = "/controller/anomaly-lot/delete-by-container-id/consolidation/single/db-after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void deleteSingleContainerByIdFromConsolidationTask() throws Exception {
        callApi("controller/anomaly-lot/delete-by-container-id/consolidation/single/request.json",
                "/anomaly/delete-by-container-id");
    }

    @Test
    @DatabaseSetup("/controller/anomaly-lot/delete-by-container-id/consolidation/multi/db-before.xml")
    @ExpectedDatabase(value = "/controller/anomaly-lot/delete-by-container-id/consolidation/multi/db-after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void deleteNonSingleContainerByIdFromConsolidationTask() throws Exception {
        callApi("controller/anomaly-lot/delete-by-container-id/consolidation/multi/request.json",
                "/anomaly/delete-by-container-id");
    }

    @Test
    @DatabaseSetup("/controller/anomaly-lot/delete-by-container-id/wrong/db-before.xml")
    @ExpectedDatabase(value = "/controller/anomaly-lot/delete-by-container-id/wrong/db-before.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void deleteByWrongContainerId() throws Exception {
        callApiWithStatus("controller/anomaly-lot/delete-by-container-id/wrong/request.json",
                "/anomaly/delete-by-container-id", 404);
    }

    @Test
    @DatabaseSetup("/controller/anomaly-lot/delete-by-container-id/closed/immutable.xml")
    @ExpectedDatabase(value = "/controller/anomaly-lot/delete-by-container-id/closed/immutable.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void shouldPreventDeletingContainerOfClosedReceipt() throws Exception {
        callApiWithStatus("controller/anomaly-lot/delete-by-container-id/closed/request.json",
                "/anomaly/delete-by-container-id", 400);
    }

    @Test
    @DatabaseSetup("/controller/anomaly-lot/delete-by-container-id/last-not-least/db-before.xml")
    @ExpectedDatabase(value = "/controller/anomaly-lot/delete-by-container-id/last-not-least/db-after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void lastNotLeastContainerDeleted_shouldStartConsolidation() throws Exception {
        callApi("controller/anomaly-lot/delete-by-container-id/last-not-least/request.json",
                "/anomaly/delete-by-container-id");
    }

    @Test
    @DatabaseSetup("/controller/anomaly-lot/delete-by-container-id/last/db-before.xml")
    @ExpectedDatabase(value = "/controller/anomaly-lot/delete-by-container-id/last/db-after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void lastContainerDeleted_shouldntStartConsolidation() throws Exception {
        callApi("controller/anomaly-lot/delete-by-container-id/last/request.json",
                "/anomaly/delete-by-container-id");
    }

    @Test
    @DatabaseSetup("/controller/anomaly-lot/find-anomaly-lot/db.xml")
    @ExpectedDatabase(value = "/controller/anomaly-lot/find-anomaly-lot/db.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void findOneAnomalyLot() throws Exception {
        callApi("controller/anomaly-lot/find-anomaly-lot/requestOne.json",
                "/anomaly/find-anomaly-lot",
                "controller/anomaly-lot/find-anomaly-lot/responseOne.json");
    }

    @Test
    @DatabaseSetup("/controller/anomaly-lot/find-anomaly-lot/db.xml")
    @ExpectedDatabase(value = "/controller/anomaly-lot/find-anomaly-lot/db.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void findTwoAnomalyLot() throws Exception {
        callApi("controller/anomaly-lot/find-anomaly-lot/requestTwo.json",
                "/anomaly/find-anomaly-lot",
                "controller/anomaly-lot/find-anomaly-lot/responseTwo.json");
    }

    @Test
    @DatabaseSetup("/controller/anomaly-lot/find-anomaly-lot/db.xml")
    @ExpectedDatabase(value = "/controller/anomaly-lot/find-anomaly-lot/db.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void findTwoAnomalyLotWithProblem() throws Exception {
        callApi("controller/anomaly-lot/find-anomaly-lot/badRequestTwo.json",
                "/anomaly/find-anomaly-lot",
                "controller/anomaly-lot/find-anomaly-lot/badResponseTwo.json");
    }

    @Test
    @DatabaseSetup("/controller/anomaly-lot/find-anomaly-lot/db.xml")
    @ExpectedDatabase(value = "/controller/anomaly-lot/find-anomaly-lot/db.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void findZeroAnomalyLot() throws Exception {
        callApiWithStatus("controller/anomaly-lot/find-anomaly-lot/requestZero.json",
                "/anomaly/find-anomaly-lot", 404);
    }

    @Test
    @DatabaseSetup("/controller/anomaly-lot/get-anomaly-container/existing/db.xml")
    @ExpectedDatabase(value = "/controller/anomaly-lot/get-anomaly-container/existing/db.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void getExistingAnomalyContainer() throws Exception {
        callApi("controller/anomaly-lot/get-anomaly-container/existing/request.json",
                "/anomaly/get-container",
                "controller/anomaly-lot/get-anomaly-container/existing/response.json");
    }

    @Test
    @DatabaseSetup("/controller/anomaly-lot/get-anomaly-container/wrong/db.xml")
    @ExpectedDatabase(value = "/controller/anomaly-lot/get-anomaly-container/wrong/db.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void getWrongAnomalyContainer() throws Exception {
        mockMvc.perform(post("/anomaly/get-container")
                        .cookie(new Cookie(AuthenticationParam.USERNAME.getCode(), "TEST"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent("controller/anomaly-lot/get-anomaly-container/wrong/request.json")))
                .andExpect(status().isBadRequest())
                .andExpect(content().json(getFileContent(
                        "controller/anomaly-lot/get-anomaly-container/wrong/response.json")));
    }

    @Test
    @DatabaseSetup("/controller/anomaly-lot/get-containers-info/db.xml")
    @ExpectedDatabase(value = "/controller/anomaly-lot/get-containers-info/db.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void getAnomalyContainersInfoByReceiptKey() throws Exception {
        callApi("controller/anomaly-lot/get-containers-info/by-receipt-key/request.json",
                "/anomaly/get-containers-info",
                "controller/anomaly-lot/get-containers-info/by-receipt-key/response.json");
    }

    @Test
    @DatabaseSetup("/controller/anomaly-lot/get-containers-info/db.xml")
    @ExpectedDatabase(value = "/controller/anomaly-lot/get-containers-info/db.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void getAnomalyContainersInfoByContainerId() throws Exception {
        callApi("controller/anomaly-lot/get-containers-info/by-container-id/request.json",
                "/anomaly/get-containers-info",
                "controller/anomaly-lot/get-containers-info/by-container-id/response.json");
    }

    @Test
    @DatabaseSetup("/controller/anomaly-lot/get-containers-info/db.xml")
    @ExpectedDatabase(value = "/controller/anomaly-lot/get-containers-info/db.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void getAnomalyContainersInfoByWrongIdentifier() throws Exception {
        callApiWithStatus("controller/anomaly-lot/get-containers-info/wrong/request.json",
                "/anomaly/get-containers-info",
                404);
    }

    @Test
    @DatabaseSetup("/controller/anomaly-lot/ship-containers/before.xml")
    @ExpectedDatabase(value = "/controller/anomaly-lot/ship-containers/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void shipContainersOk() throws Exception {
        callApi("controller/anomaly-lot/ship-containers/request.json",
                "/anomaly/ship-containers",
                "controller/anomaly-lot/ship-containers/response.json");
    }


    @Test
    @DatabaseSetup("/controller/anomaly-lot/ship-containers-fail/immutable-state.xml")
    @ExpectedDatabase(value = "/controller/anomaly-lot/ship-containers-fail/immutable-state.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void shipContainersFail() throws Exception {
        callApiWithStatus("controller/anomaly-lot/ship-containers-fail/request.json",
                "/anomaly/ship-containers",
                400);
    }

    @Test
    @DatabaseSetup("/controller/anomaly-lot/move-containers/before.xml")
    @ExpectedDatabase(value = "/controller/anomaly-lot/move-containers/after.xml", assertionMode = NON_STRICT_UNORDERED)
    public void moveContainers() throws Exception {
        callApiWithStatus("controller/anomaly-lot/move-containers/request.json",
                "/anomaly/move-containers", 204);
    }

    @Test
    @DatabaseSetup("/controller/anomaly-lot/move-containers-anomaly-withdrawal/before.xml")
    @ExpectedDatabase(value = "/controller/anomaly-lot/move-containers-anomaly-withdrawal/after.xml", assertionMode =
            NON_STRICT_UNORDERED)
    public void moveContainersAnomalyWithdrawal() throws Exception {
        callApiWithStatus("controller/anomaly-lot/move-containers-anomaly-withdrawal/request.json",
                "/anomaly/move-containers", 204);
    }

    @Test
    @DatabaseSetup("/controller/anomaly-lot/lose-containers/before.xml")
    @ExpectedDatabase(value = "/controller/anomaly-lot/lose-containers/after.xml", assertionMode = NON_STRICT_UNORDERED)
    public void loseContainersOk() throws Exception {
        callApi("controller/anomaly-lot/lose-containers/request.json",
                "/anomaly/lose-containers",
                "controller/anomaly-lot/lose-containers/response.json");
    }

    @Test
    @DatabaseSetup("/controller/anomaly-lot/print-label/db-1P.xml")
    @ExpectedDatabase(value = "/controller/anomaly-lot/print-label/db-1P.xml", assertionMode = NON_STRICT_UNORDERED)
    public void printLabel1P() throws Exception {
        callApi("controller/anomaly-lot/print-label/request.json",
                "/anomaly/print-label",
                "controller/anomaly-lot/print-label/response.json");

        String date = LocalDate.now().format(AnomalyLabelPrinter.DATE_FORMATTER);
        String expectedZpl = "<>PLT00001<>" + date + "<>0000000016<>1P<>38297 OOO ROSA<>";
        Mockito.verify(printService).print(expectedZpl, "prn123");
    }

    @Test
    @DatabaseSetup("/controller/anomaly-lot/print-label/db-3P.xml")
    @ExpectedDatabase(value = "/controller/anomaly-lot/print-label/db-3P.xml", assertionMode = NON_STRICT_UNORDERED)
    public void printLabel3P() throws Exception {
        callApi("controller/anomaly-lot/print-label/request.json",
                "/anomaly/print-label",
                "controller/anomaly-lot/print-label/response.json");

        String date = LocalDate.now().format(AnomalyLabelPrinter.DATE_FORMATTER);
        String expectedZpl = "<>PLT00001<>" + date + "<>0000000016<>3P<>481243 OOO FIALKA<>";
        Mockito.verify(printService).print(expectedZpl, "prn123");
    }

    @Test
    @DatabaseSetup("/controller/anomaly-lot/create-and-print-label/db.xml")
    @ExpectedDatabase(value = "/controller/anomaly-lot/create-and-print-label/db.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void printLabelAsUIT() throws Exception {
        callApi("controller/anomaly-lot/create-and-print-label/request-small.json",
                "/anomaly/create-anomaly-container",
                "controller/anomaly-lot/create-and-print-label/response.json");

        String expectedZpl = "small<>AN9900000002<>0000000016<>ANOMALY<>";
        Mockito.verify(printService).print(expectedZpl, "prn125");
    }

    @Test
    @DatabaseSetup("/controller/anomaly-lot/create-and-print-label/db.xml")
    @ExpectedDatabase(value = "/controller/anomaly-lot/create-and-print-label/db.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void printLabelAsSmallUIT() throws Exception {
        callApi("controller/anomaly-lot/create-and-print-label/request-large.json",
                "/anomaly/create-anomaly-container",
                "controller/anomaly-lot/create-and-print-label/response.json");

        String expectedZpl = "<>AN9900000002<>0000000016<>ANOMALY<>";
        Mockito.verify(printService).print(expectedZpl, "prn124");
    }

    @Test
    @DatabaseSetup("/controller/anomaly-lot/create-and-print-label/db.xml")
    @ExpectedDatabase(value = "/controller/anomaly-lot/create-and-print-label/db.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void printLabelAssUIT() throws Exception {
        callApi("controller/anomaly-lot/create-and-print-label/request.json",
                "/anomaly/create-anomaly-container",
                "controller/anomaly-lot/create-and-print-label/response.json");

        String expectedZpl = "<>AN9900000002<>0000000016<>ANOMALY<>";
        Mockito.verify(printService).print(expectedZpl, "prn126");
    }

    @Test
    @DatabaseSetup("/controller/anomaly-lot/ship-containers/before.xml")
    public void checkRunScanningOperationLog() throws Exception {
        Mockito.reset(scanningOperationLog);

        callApi("controller/anomaly-lot/ship-containers/request.json",
                "/anomaly/ship-containers",
                "controller/anomaly-lot/ship-containers/response.json");

        var anomalyContainers = List.of(
                AnomalyContainer.builder(null)
                        .status(AnomalyLotStatus.CONSOLIDATED)
                        .transportUnitId("PLT00001")
                        .receiptKey("0000000016")
                        .loc("STAGE01")
                        .build(),
                AnomalyContainer.builder(null)
                        .status(AnomalyLotStatus.CONSOLIDATED)
                        .transportUnitId("PLT00002")
                        .receiptKey("0000000016")
                        .loc("STAGE01")
                        .build()
        );

        var anomalyLots = List.of(
                AnomalyLot.builder(anomalyContainers.get(0))
                        .id("1")
                        .storerKey("465852")
                        .altSku("ALTSKU01")
                        .sku("ROV0000000000000000358")
                        .description("Description 1")
                        .types(Set.of(ReceivingItemType.DAMAGED))
                        .amount(1)
                        .category(AnomalyCategory.FOOD.getCategory())
                        .build(),
                AnomalyLot.builder(anomalyContainers.get(0))
                        .id("2")
                        .storerKey("465852")
                        .altSku("ALTSKU02")
                        .sku("ROV0000000000000000359")
                        .manufacturerSku("SKU27")
                        .types(Set.of(ReceivingItemType.EXPIRED))
                        .amount(12)
                        .category(AnomalyCategory.NON_FOOD.getCategory())
                        .build(),
                AnomalyLot.builder(anomalyContainers.get(1))
                        .id("3")
                        .storerKey("465852")
                        .altSku("ALTSKU03")
                        .description("Description 2")
                        .sku("ROV0000000000000000359")
                        .types(Set.of(ReceivingItemType.EXPIRED))
                        .amount(5)
                        .category(AnomalyCategory.CHEMISTRY.getCategory())
                        .build()
        );

        Mockito.verify(scanningOperationLog).writeAnomaliesOperation(
                ScanningOperationType.SHIP_ANOMALY_CONTAINERS,
                anomalyLots
        );
    }

    @Test
    @DatabaseSetup("/controller/anomaly-lot/receipt-not-shipped/receipt-not-shipped.xml")
    void getReceiptsWithNotShippedAnomalies() throws Exception {
        //when
        MvcResult result = mockMvc.perform(
                        post("/anomaly/not-shipped-anomalies")
                                .cookie(new Cookie(AuthenticationParam.USERNAME.getCode(), "TEST"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("[\"0000000101\", \"0000000102\", \"0000000103\", \"0000000104\"]"))
                .andExpect(status().isOk())
                .andReturn();

        //then
        assertThat("Reponse body", result.getResponse().getContentAsString(),
                equalTo("[\"0000000102\",\"0000000104\"]"));
    }

    private void callApi(String filePath, String apiUrl) throws Exception {
        callApi(filePath, apiUrl, null);
    }

    private void callApi(String filePath, String apiUrl, String responseFile) throws Exception {
        MvcResult mvcResult = mockMvc.perform(post(apiUrl)
                        .cookie(new Cookie(AuthenticationParam.USERNAME.getCode(), "TEST"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent(filePath)))
                .andExpect(status().isOk())
                .andReturn();
        if (responseFile != null) {
            JsonAssertUtils.assertFileEquals(
                    responseFile,
                    mvcResult.getResponse().getContentAsString(),
                    JSONCompareMode.NON_EXTENSIBLE
            );
        }
    }

    private void callApiWithStatus(String filePath, String apiUrl, int status) throws Exception {
        mockMvc.perform(post(apiUrl)
                        .cookie(new Cookie(AuthenticationParam.USERNAME.getCode(), "TEST"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent(filePath)))
                .andExpect(status().is(status))
                .andReturn();
    }
}
