package ru.yandex.market.loyalty.admin.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vividsolutions.jts.util.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.yandex.market.loyalty.admin.controller.dto.CoinEmissionDto;
import ru.yandex.market.loyalty.admin.test.MarketLoyaltyAdminMockedDbTest;
import ru.yandex.market.loyalty.api.model.CoinGeneratorType;
import ru.yandex.market.loyalty.api.model.PagedResponse;
import ru.yandex.market.loyalty.api.model.TableFormat;
import ru.yandex.market.loyalty.api.model.coin.CoinCreationReason;
import ru.yandex.market.loyalty.core.dao.coin.BunchGenerationRequestStatus;
import ru.yandex.market.loyalty.core.dao.coin.GeneratorType;
import ru.yandex.market.loyalty.core.model.CoinEmission;
import ru.yandex.market.loyalty.core.model.promo.Promo;
import ru.yandex.market.loyalty.core.service.CoinEmissionService;
import ru.yandex.market.loyalty.core.service.PromoManager;
import ru.yandex.market.loyalty.core.utils.PromoUtils;
import ru.yandex.market.loyalty.test.TestFor;

import java.sql.Timestamp;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@TestFor(CoinEmissionController.class)
public class CoinEmissionControllerTest extends MarketLoyaltyAdminMockedDbTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private CoinEmissionService coinEmissionService;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private PromoManager promoManager;

    private static final TypeReference<PagedResponse<CoinEmission>> PAGED_RESPONSE_TYPE_REFERENCE =
            new TypeReference<>() {
            };

    private long testCoinEmissionId;
    private long testPromoId;
    private String testPromoName;

    @Before
    public void createTestCoinEmission() {
        Promo promo = promoManager.createSmartShoppingPromo(
                PromoUtils.SmartShopping.defaultFixed());

        testPromoId = promo.getPromoId().getId();
        testPromoName = promo.getName();

        CoinEmission.Builder builder = CoinEmission.builder()
                .setPromoId(promo.getPromoId().getId())
                .setKey("testCoinEmissionKey")
                .setCount(999)
                .setEmail("testCoinEmissionEmail")
                .setStatus(BunchGenerationRequestStatus.PROCESSED)
                .setCreationTime(new Timestamp(new Date().getTime()))
                .setFormat("testCoinEmissionFormat")
                .setRetryCount(3)
                .setSource("testCoinEmissionSource")
                .setGeneratorType(GeneratorType.COIN)
                .setSubstatus("testCoinEmissionSubstatus")
                .setMessage("testCoinEmissionMessage")
                .setProcessedCount(1);
        testCoinEmissionId = coinEmissionService.createCoinEmission(builder.build());
    }

    @Test
    public void shouldReturnPagedEmissions() throws Exception {
        PagedResponse<CoinEmission> pagedResponse = getEmissionsByExpressionToFind(null);
        assertEquals(1, pagedResponse.getData().size());
    }

    @Test
    public void shouldReturnPagedEmissionsByEmissionKey() throws Exception {
        PagedResponse<CoinEmission> pagedResponse = getEmissionsByExpressionToFind("testCoinEmissionKey");
        assertEquals(1, pagedResponse.getData().size());
    }

    @Test
    public void shouldReturnByPromoName() throws Exception {
        PagedResponse<CoinEmission> pagedResponse = getEmissionsByExpressionToFind(testPromoName);
        assertEquals(1, pagedResponse.getData().size());
    }

    @Test
    public void shouldReturnByPartOfEmissionKey() throws Exception {
        PagedResponse<CoinEmission> pagedResponse = getEmissionsByExpressionToFind("testCoinEmi");
        assertEquals(1, pagedResponse.getData().size());
    }

    private PagedResponse<CoinEmission> getEmissionsByExpressionToFind(
            String expressionToFind) throws Exception {
        String contentAsString = mockMvc
                .perform(
                        get("/api/coin/emission/paged")
                                .with(csrf())
                                .param("currentPage", String.valueOf(1))
                                .param("pageSize", String.valueOf(999))
                                .param("expressionToFind", expressionToFind)
                                .param("statusesToFind", BunchGenerationRequestStatus.PROCESSED.getCode())
                )
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readValue(
                contentAsString,
                PAGED_RESPONSE_TYPE_REFERENCE
        );
    }

    @Test
    public void testFilterByInQueue() throws Exception {
        createTestCoinEmissionWithStatus(BunchGenerationRequestStatus.IN_QUEUE);
        testFilteredByStatus(BunchGenerationRequestStatus.IN_QUEUE.getCode(), 1);
    }

    @Test
    public void testFilterByProcessed() throws Exception {
        testFilteredByStatus(BunchGenerationRequestStatus.PROCESSED.getCode(), 1);
    }

    @Test
    public void testFilterByPrepared() throws Exception {
        createTestCoinEmissionWithStatus(BunchGenerationRequestStatus.PREPARED);
        testFilteredByStatus(BunchGenerationRequestStatus.PREPARED.getCode(), 1);
    }

    @Test
    public void testFilterByCancelling() throws Exception {
        createTestCoinEmissionWithStatus(BunchGenerationRequestStatus.CANCELLING);
        testFilteredByStatus(BunchGenerationRequestStatus.CANCELLING.getCode(), 1);
    }

    @Test
    public void testFilterByCancelled() throws Exception {
        createTestCoinEmissionWithStatus(BunchGenerationRequestStatus.CANCELLED);
        testFilteredByStatus(BunchGenerationRequestStatus.CANCELLED.getCode(), 1);
    }

    @Test
    public void testFilterByMultipleStatuses() throws Exception {
        createTestCoinEmissionWithStatus(BunchGenerationRequestStatus.IN_QUEUE);
        createTestCoinEmissionWithStatus(BunchGenerationRequestStatus.PREPARED);
        createTestCoinEmissionWithStatus(BunchGenerationRequestStatus.CANCELLED);
        createTestCoinEmissionWithStatus(BunchGenerationRequestStatus.CANCELLING);
        testFilteredByStatus(
                BunchGenerationRequestStatus.IN_QUEUE.getCode() + "," +
                BunchGenerationRequestStatus.PREPARED.getCode() + "," +
                BunchGenerationRequestStatus.CANCELLED.getCode(),
                3);
    }

    private void testFilteredByStatus(String statuses, int numberOfResponseData) throws Exception {
        String contentAsString = mockMvc
                .perform(
                        get("/api/coin/emission/paged")
                                .with(csrf())
                                .param("currentPage", String.valueOf(1))
                                .param("pageSize", String.valueOf(999))
                                .param("statusesToFind", statuses)
                )
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        PagedResponse<CoinEmission> pagedResponse = objectMapper.readValue(
                contentAsString,
                PAGED_RESPONSE_TYPE_REFERENCE);
        assertEquals(numberOfResponseData, pagedResponse.getData().size());
    }

    private void createTestCoinEmissionWithStatus(BunchGenerationRequestStatus status) {
        coinEmissionService.createCoinEmission(CoinEmission.builder()
                .setPromoId(testPromoId)
                .setKey("testCoinEmissionKey" + status.getCode())
                .setCount(999)
                .setEmail("testCoinEmissionEmail")
                .setStatus(status)
                .setCreationTime(new Timestamp(new Date().getTime()))
                .setFormat("testCoinEmissionFormat")
                .setRetryCount(3)
                .setSource("testCoinEmissionSource")
                .setGeneratorType(GeneratorType.COIN)
                .setSubstatus("testCoinEmissionSubstatus")
                .setMessage("testCoinEmissionMessage")
                .setProcessedCount(1).build());
    }

    @Test
    public void shouldChangeRetryCountAndMessage() throws Exception {
        CoinEmission coinEmissionBeforeChange = coinEmissionService.getEmissionById(testCoinEmissionId);
        assertEquals(3, coinEmissionBeforeChange.getRetryCount().intValue());
        assertEquals("testCoinEmissionMessage", coinEmissionBeforeChange.getMessage());
        mockMvc
                .perform(
                        put("/api/coin/emission/retry")
                                .with(csrf())
                                .param("requestId", String.valueOf(testCoinEmissionId))
                )
                .andExpect(status().isOk())
                .andReturn()
                .getResponse();
        CoinEmission coinEmissionAfterChange = coinEmissionService.getEmissionById(testCoinEmissionId);
        assertEquals(0, coinEmissionAfterChange.getRetryCount().intValue());
        assertNull(coinEmissionAfterChange.getMessage());
    }


    @Test
    public void shouldChangePriority() throws Exception {
        CoinEmission coinEmissionBeforeChange = coinEmissionService.getEmissionById(testCoinEmissionId);
        assertEquals(0, coinEmissionBeforeChange.getPriority().intValue());
        mockMvc
                .perform(
                        put("/api/coin/emission/updatePriority")
                                .with(csrf())
                                .param("requestId", String.valueOf(testCoinEmissionId))
                                .param("priority", String.valueOf(1))
                )
                .andExpect(status().isOk())
                .andReturn()
                .getResponse();
        CoinEmission coinEmissionAfterChange = coinEmissionService.getEmissionById(testCoinEmissionId);
        assertEquals(1, coinEmissionAfterChange.getPriority().intValue());
    }

    @Test
    public void shouldCancelRequest() throws Exception {
        CoinEmission coinEmissionBeforeChange = coinEmissionService.getEmissionById(testCoinEmissionId);
        assertEquals(BunchGenerationRequestStatus.PROCESSED, coinEmissionBeforeChange.getStatus());
        mockMvc
                .perform(
                        put("/api/coin/emission/cancel")
                                .with(csrf())
                                .param("requestId", String.valueOf(testCoinEmissionId))
                )
                .andExpect(status().isOk())
                .andReturn()
                .getResponse();
        CoinEmission coinEmissionAfterChange = coinEmissionService.getEmissionById(testCoinEmissionId);
        assertEquals(BunchGenerationRequestStatus.CANCELLED, coinEmissionAfterChange.getStatus());
    }

    @Test
    public void shouldCreateCoinRequest() throws Exception {
        CoinEmissionDto coinEmissionDto = new CoinEmissionDto();
        coinEmissionDto.setKey("testRequestKey");
        coinEmissionDto.setCount(999);
        coinEmissionDto.setPromoId(testPromoId);
        coinEmissionDto.setIgnoreBudgetExhaustion(true);
        coinEmissionDto.setReason(CoinCreationReason.EMAIL_COMPANY.getCode());
        coinEmissionDto.setCoinGeneratorType(CoinGeneratorType.AUTH.getCode());
        coinEmissionDto.setFormat(TableFormat.CSV.getCode());
        coinEmissionDto.setEmail("testemail@yandex-team.ru");
        coinEmissionDto.setOutputTable(null);
        coinEmissionDto.setErrorOutputTable(null);
        coinEmissionDto.setInputTable("inputTable");

        mockMvc
                .perform(
                        post("/api/coin/emission/create")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(coinEmissionDto))
                                .with(csrf())
                )
                .andExpect(status().isOk())
                .andReturn()
                .getResponse();

        String contentAsString = mockMvc
                .perform(
                        get("/api/coin/emission/paged")
                                .with(csrf())
                                .param("currentPage", String.valueOf(1))
                                .param("pageSize", String.valueOf(999))
                                .param("statusesToFind",
                                        BunchGenerationRequestStatus.IN_QUEUE.getCode() + "," +
                                                BunchGenerationRequestStatus.PROCESSED.getCode())
                )
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        PagedResponse<CoinEmission> pagedResponse =
                objectMapper.readValue(
                        contentAsString,
                        PAGED_RESPONSE_TYPE_REFERENCE
                );
        assertEquals(2, pagedResponse.getData().size());
    }

    @Test
    public void testSorting() throws Exception {
        initForSorting();

        PagedResponse<CoinEmission> response;

        response = getSortedResponse("ID,ASC");
        isSortedBy(Comparator.comparing(CoinEmission::getId), response.getData());
        response = getSortedResponse("ID,DESC");
        isSortedBy(Comparator.comparing(CoinEmission::getId).reversed(), response.getData());


        response = getSortedResponse("PROMO_ID,ASC");
        isSortedBy(Comparator.comparing(CoinEmission::getPromoId), response.getData());
        response = getSortedResponse("PROMO_ID,DESC");
        isSortedBy(Comparator.comparing(CoinEmission::getPromoId).reversed(), response.getData());


        response = getSortedResponse("KEY,ASC");
        isSortedBy(Comparator.comparing(CoinEmission::getKey), response.getData());
        response = getSortedResponse("KEY,DESC");
        isSortedBy(Comparator.comparing(CoinEmission::getKey).reversed(), response.getData());


        response = getSortedResponse("COUNT,ASC");
        isSortedBy(Comparator.comparing(CoinEmission::getCount), response.getData());
        response = getSortedResponse("COUNT,DESC");
        isSortedBy(Comparator.comparing(CoinEmission::getCount).reversed(), response.getData());


        response = getSortedResponse("PRIORITY,ASC");
        isSortedBy(Comparator.comparing(CoinEmission::getPriority), response.getData());
        response = getSortedResponse("PRIORITY,DESC");
        isSortedBy(Comparator.comparing(CoinEmission::getPriority).reversed(), response.getData());


        response = getSortedResponse("CREATION_TIME,ASC");
        isSortedBy(Comparator.comparing(CoinEmission::getCreationTime), response.getData());
        response = getSortedResponse("CREATION_TIME,DESC");
        isSortedBy(Comparator.comparing(CoinEmission::getCreationTime).reversed(), response.getData());


        response = getSortedResponse("RETRY_COUNT,ASC");
        isSortedBy(Comparator.comparing(CoinEmission::getRetryCount), response.getData());
        response = getSortedResponse("RETRY_COUNT,DESC");
        isSortedBy(Comparator.comparing(CoinEmission::getRetryCount).reversed(), response.getData());
    }

    private void isSortedBy(Comparator<CoinEmission> coinEmissionComparator, List<CoinEmission> data) {
        List<CoinEmission> copyOfData = List.copyOf(data);
        data = data.stream().sorted(coinEmissionComparator).collect(Collectors.toList());
        for (int i = 0; i < data.size(); i++) {
            Assert.equals(copyOfData.get(i).getId(), data.get(i).getId());
        }
    }

    private void initForSorting() {
        createCoinEmissionWithParams(1, "5", 4, 3, 2);
        createCoinEmissionWithParams(2, "1", 5, 4, 3);
        createCoinEmissionWithParams(3, "2", 1, 5, 4);
        createCoinEmissionWithParams(4, "3", 2, 1, 5);
        createCoinEmissionWithParams(5, "4", 3, 2, 1);
    }

    private void createCoinEmissionWithParams(long promoId, String key, int count, long timeShift, int retryCount) {
        coinEmissionService.createCoinEmission(CoinEmission.builder()
                .setPromoId(promoId)
                .setKey(key)
                .setCount(count)
                .setEmail("testCoinEmissionEmail")
                .setStatus(BunchGenerationRequestStatus.IN_QUEUE)
                .setCreationTime(new Timestamp(new Date().getTime() - timeShift))
                .setFormat("testCoinEmissionFormat")
                .setRetryCount(retryCount)
                .setSource("testCoinEmissionSource")
                .setGeneratorType(GeneratorType.COIN)
                .setSubstatus("testCoinEmissionSubstatus")
                .setMessage("testCoinEmissionMessage")
                .setProcessedCount(1).build());
    }

    private PagedResponse<CoinEmission> getSortedResponse(String sortStr) throws Exception{
        String contentAsString = mockMvc
                .perform(
                        get("/api/coin/emission/paged")
                                .with(csrf())
                                .param("currentPage", String.valueOf(1))
                                .param("pageSize", String.valueOf(999))
                                .param("sort", sortStr)
                                .param("statusesToFind", "IN_QUEUE")
                )
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readValue(
                contentAsString,
                PAGED_RESPONSE_TYPE_REFERENCE);
    }
}
