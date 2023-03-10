package ru.yandex.market.replenishment.autoorder.api.scenario;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.sun.istack.NotNull;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.replenishment.autoorder.config.ControllerTest;
import ru.yandex.market.replenishment.autoorder.model.SupplierRequestNotificationInfo;
import ru.yandex.market.replenishment.autoorder.model.dto.AdjustedRecommendationDTO;
import ru.yandex.market.replenishment.autoorder.model.dto.AdjustedSummaryRecommendationsDTO;
import ru.yandex.market.replenishment.autoorder.model.dto.DemandIdentityDTO;
import ru.yandex.market.replenishment.autoorder.repository.postgres.SupplierRequestFfShopRequestRepository;
import ru.yandex.market.replenishment.autoorder.security.WithMockLogin;
import ru.yandex.market.replenishment.autoorder.service.NotifySupplierAboutRequestLoader;
import ru.yandex.market.replenishment.autoorder.service.ParentDemand3pUpdateLoader;
import ru.yandex.market.replenishment.autoorder.service.PdbReplenishmentService;
import ru.yandex.market.replenishment.autoorder.service.SupplierRequestDraftCloseLoader;
import ru.yandex.market.replenishment.autoorder.service.SupplierRequestNotificationService;
import ru.yandex.market.replenishment.autoorder.service.excel.core.reader.BaseExcelReader;
import ru.yandex.market.replenishment.autoorder.utils.StringCapturingMatcher;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.APPLICATION_JSON_UTF8;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.replenishment.autoorder.utils.TestUtils.dtoToString;
@WithMockLogin
public class PdbReplenishment3pScenarioTest extends ControllerTest {
    @Autowired
    TransactionTemplate transactionTemplate;

    @Autowired
    PdbReplenishmentService pdbReplenishmentService;

    @Autowired
    SupplierRequestDraftCloseLoader supplierRequestDraftCloseLoader;

    @Autowired
    NotifySupplierAboutRequestLoader notifySupplierAboutRequestLoader;

    @Autowired
    SupplierRequestFfShopRequestRepository supplierRequestFfShopRequestRepository;

    @Autowired
    ParentDemand3pUpdateLoader parentDemand3pUpdateLoader;

    private static final long PARENT_DEMAND_ID = 1;
    private static final long PARENT_DEMAND_ID2 = 2;
    private static final long DEMAND_ID = 11;
    private static final int DEMAND_VERSION = 1;

    @Before
    public void prepareMocks() {
        setTestTime(LocalDateTime.of(2021, 8, 10, 18, 0, 0));
    }

    /**
     * ?????????????????????? ???????????????????????? => ???????????????????? ?????????????? => ???????????????? ??????????????????????(whitelist) => ???????????????????????? ?? AX
     * => ???????????????? ?????????? ???????????????? ?? ????
     */
    @Test
    @DbUnitDataSet(
            before = "PdbReplenishment3pScenarioTest_testScenario1.before.csv",
            after = "PdbReplenishment3pScenarioTest_testScenario1.after.csv")
    @DbUnitDataSet(
            dataSource = "pdbDataSource",
            after = "PdbReplenishment3pScenarioTest_testScenario1.pdb.after.csv")
    public void testScenario1() throws Exception {
        //?????????????????????? ????????????????????????
        AdjustedSummaryRecommendationsDTO adjustedRecommendationsDTO = new AdjustedSummaryRecommendationsDTO();
        adjustedRecommendationsDTO.setAdjustedRecommendations(
                List.of(AdjustedRecommendationDTO.builder().msku(200L).adjustedPurchQty(30).correctionReason(1L).build()));
        adjustedRecommendationsDTO.setDemandKeys(List.of(new DemandIdentityDTO(DEMAND_ID, DEMAND_VERSION)));

        mockMvc.perform(put("/api/v1/recommendations/summary/adjust?demandType=TYPE_3P")
                .contentType(APPLICATION_JSON_UTF8)
                .content(dtoToString(adjustedRecommendationsDTO)))
                .andExpect(status().isOk());

        parentDemand3pUpdateLoader.load();

        //???????????????? ??????????????????????
        mockMvc.perform(post("/api/v1/demands-3p/export")
                .contentType(APPLICATION_JSON)
                .content("{ \"ids\": [" + PARENT_DEMAND_ID + "] }"))
                .andExpect(status().isOk());

        //???????????????? ??????????????????????
        supplierRequestDraftCloseLoader.load();
        prepareSupplierRequestNotificationMocks(true);
        notifySupplierAboutRequestLoader.load();

        //?????????????? ?? pdb
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(@NotNull TransactionStatus status) {
                pdbReplenishmentService.exportToPdb();
            }
        });

        //?????????????????? ?????????????????? ???????????????? ?????? ????
        testPartnerInterfaceEndpoints(2);
    }

    /**
     * ?????????????????????? ???????????????????????? => ???????????????????? ?????????????? ???? parentDemand 1 => ???????????????????? ?????????????? ???? parentDemand 2 =>
     * ???????????????? ?????????????????????? (?????????????????? ?????????????????????? ????????????????) => ???????????????? ??????????????????????(blacklist) => ???????????????????????? ?? AX
     * => ???????????????? ?????????? ???????????????? ?? ????
     */
    @Test
    @DbUnitDataSet(
            before = "PdbReplenishment3pScenarioTest_testScenario2.before.csv",
            after = "PdbReplenishment3pScenarioTest_testScenario2.after.csv")
    @DbUnitDataSet(
            dataSource = "pdbDataSource",
            after = "PdbReplenishment3pScenarioTest_testScenario1.pdb.after.csv")
    public void testScenario2() throws Exception {
        //?????????????????????? ????????????????????????
        AdjustedSummaryRecommendationsDTO adjustedRecommendationsDTO = new AdjustedSummaryRecommendationsDTO();
        adjustedRecommendationsDTO.setAdjustedRecommendations(
                List.of(AdjustedRecommendationDTO.builder().msku(200L).adjustedPurchQty(30).correctionReason(1L).build()));
        adjustedRecommendationsDTO.setDemandKeys(List.of(new DemandIdentityDTO(DEMAND_ID, DEMAND_VERSION)));
        mockMvc.perform(put("/api/v1/recommendations/summary/adjust?demandType=TYPE_3P")
                .contentType(APPLICATION_JSON_UTF8)
                .content(dtoToString(adjustedRecommendationsDTO)))
                .andExpect(status().isOk());

        parentDemand3pUpdateLoader.load();

        //???????????????? ???????????????? ???? parentDemandId
        mockMvc.perform(post("/api/v1/demands-3p/export")
                .contentType(APPLICATION_JSON)
                .content("{ \"ids\": [" + PARENT_DEMAND_ID + "] }"))
                .andExpect(status().isOk());


        //???????????????? ???????????????? ???? parentDemandId2
        mockMvc.perform(post("/api/v1/demands-3p/export")
                .contentType(APPLICATION_JSON)
                .content("{ \"ids\": [" + PARENT_DEMAND_ID2 + "] }"))
                .andExpect(status().isOk());

        //???????????????? ??????????????????????
        supplierRequestDraftCloseLoader.load();
        prepareSupplierRequestNotificationMocks(false, null);
        notifySupplierAboutRequestLoader.load();

        //?????????????? ?? pdb
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(@NotNull TransactionStatus status) {
                pdbReplenishmentService.exportToPdb();
            }
        });

        //?????????????????? ?????????????????? ???????????????? ?????? ????
        testPartnerInterfaceEndpoints(4);
    }

    private void prepareSupplierRequestNotificationMocks(boolean isWhiteList, long... supplierRequestIdsForError) {
        final SupplierRequestNotificationService supplierRequestNotificationService =
            Mockito.mock(SupplierRequestNotificationService.class);
        when(supplierRequestNotificationService.notifySupplierAboutRequest(
                ArgumentMatchers.any(SupplierRequestNotificationInfo.class),
                ArgumentMatchers.any(LocalDate.class),
                ArgumentMatchers.any(LocalDate.class)))
                .thenAnswer(getTheSameOrErrorForRequestId(supplierRequestIdsForError));
        ReflectionTestUtils.setField(notifySupplierAboutRequestLoader,
            "notificationSuppliersIsWhiteList", isWhiteList);
        ReflectionTestUtils.setField(notifySupplierAboutRequestLoader,
                "supplierRequestNotificationService", supplierRequestNotificationService);
    }

    private static Answer<CompletableFuture<SupplierRequestNotificationService.NotificationInfoAndError>> getTheSameOrErrorForRequestId(
            long... supplierRequestIdsForError) {
        return (InvocationOnMock invocation) -> {
            Object argument = invocation.getArgument(0);
            if (argument instanceof SupplierRequestNotificationInfo) {
                SupplierRequestNotificationInfo info = (SupplierRequestNotificationInfo) argument;
                Long requestId = info.getRequestId();
                if (requestId != null
                        && supplierRequestIdsForError != null
                        && Arrays.binarySearch(supplierRequestIdsForError, requestId) >= 0) {
                    return CompletableFuture.completedFuture(SupplierRequestNotificationService.NotificationInfoAndError.of(
                            info, "Test error for supplier request id " + requestId));
                }
                return CompletableFuture.completedFuture(SupplierRequestNotificationService.NotificationInfoAndError.of(info));
            }
            return CompletableFuture.failedFuture(new IllegalStateException("Null argument in test"));
        };
    }

    private void testPartnerInterfaceEndpoints(int expectedRowCount) throws Exception {
        final String url = "/api/v1/supplier-request";

        StringCapturingMatcher idCapturer = new StringCapturingMatcher(v -> v.startsWith("RPL-"));

        //?????????????????? ????????????????
        {
            mockMvc.perform(get(url + "/?supplierId=1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.requests", hasSize(1)));

            mockMvc.perform(get(url + "?supplierId=1&status=NEW&count=10&page=1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requests", hasSize(1)))
                .andExpect(jsonPath("$.count").value(1))
                .andExpect(jsonPath("$.requests[0].id").value(idCapturer))
                .andExpect(jsonPath("$.requests[0].supplierId").value(1));

            mockMvc.perform(get(url + "?supplierId=1&idSubstring=" + idCapturer.getValue()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.requests", hasSize(1)))
                    .andExpect(jsonPath("$.requests[0].id").value(idCapturer.getValue()));
        }

        String rId = idCapturer.getValue();

        //???????????????? ???????????????? ?? ????????????
        {
            byte[] excelData = mockMvc.perform(get("/api/v1/supplier-request/" + rId + "/excel"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsByteArray();


            List<List<Object>> lists = BaseExcelReader.extractFromExcel(
                    new ByteArrayInputStream(excelData),
                    16
            );

            assertEquals(expectedRowCount, lists.size());
        }

        //???????????????? ?????????????? ??????????????????????
        {
            mockMvc.perform(post(url + "/" + rId + "/response")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{ \"accept\": true }"))
                    .andExpect(status().isOk());
        }

        //?????????????????? ????????????????
        {
            //???? ???? ?????? ???????????? ???????????????? ???????????????? FulfillmentShopRequestIncrementalLoader, ?????? ???????????? ?????? ??????????
            long requestId = Long.parseLong(rId.substring(rId.length() - 1));
            supplierRequestFfShopRequestRepository.insert(requestId, 1L);
            mockMvc.perform(get(url + "/ff-shop-requests?supplierRequestIds=" + rId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].supplierRequestId").value(rId))
                    .andExpect(jsonPath("$[0].ffShopRequestsIds", hasSize(1)))
                    .andExpect(jsonPath("$[0].ffShopRequestsIds[0]").value("1"))
            ;
        }
    }
}
