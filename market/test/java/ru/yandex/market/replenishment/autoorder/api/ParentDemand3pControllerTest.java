package ru.yandex.market.replenishment.autoorder.api;

import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.replenishment.autoorder.api.dto.user_fillters.recommendation.RecommendationFilters;
import ru.yandex.market.replenishment.autoorder.config.ControllerTest;
import ru.yandex.market.replenishment.autoorder.model.DemandType;
import ru.yandex.market.replenishment.autoorder.model.RecommendationFilter;
import ru.yandex.market.replenishment.autoorder.model.dto.AdjustedRecommendationDTO;
import ru.yandex.market.replenishment.autoorder.model.dto.AdjustedRecommendationsDTO;
import ru.yandex.market.replenishment.autoorder.model.entity.postgres.CorrectionReason;
import ru.yandex.market.replenishment.autoorder.repository.postgres.CorrectionReasonRepository;
import ru.yandex.market.replenishment.autoorder.security.WithMockLogin;
import ru.yandex.market.replenishment.autoorder.service.excel.AdjustedRecommendationsExcelReader;

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.APPLICATION_JSON_UTF8;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.replenishment.autoorder.utils.TestUtils.dtoToString;
@WithMockLogin
public class ParentDemand3pControllerTest extends ControllerTest {

    private static final String EXPORT_URL = "/api/v1/demands-3p/export";

    private static final LocalDateTime MOCK_DATE = LocalDateTime.of(2021, 4, 23, 12, 0);
    private static final String REQUEST_ID_1 = "{ \"ids\": [1] }";

    @Autowired
    private CorrectionReasonRepository correctionReasonRepository;


    @Before
    public void mockMethods() {
        setTestTime(MOCK_DATE);
    }

    @Test
    public void testEmptyFilter() throws Exception {
        mockMvc.perform(post(EXPORT_URL).contentType(APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString(
                        "Field error in object 'parentDemand3pFilterDTO' on field 'ids': rejected value [null];"
                )));
    }

    @Test
    public void testFilterWithNoIds() throws Exception {
        mockMvc.perform(post(EXPORT_URL).contentType(APPLICATION_JSON).content("{ \"ids\": [] }"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                    .value("Поле parentDemand3pFilterDTO.ids must not be empty"));
    }

    @Test
    public void testFilterWithNonExistentId() throws Exception {
        mockMvc.perform(post(EXPORT_URL).contentType(APPLICATION_JSON).content(REQUEST_ID_1))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                    .value("Поле parentDemand3pFilterDTO.ids 3Р потребность с ID #1 не существует"));
    }

    @Test
    @DbUnitDataSet(before = "ParentDemand3pControllerTest.already_exported.before.csv")
    public void testAlreadyExported() throws Exception {
        mockMvc.perform(post(EXPORT_URL).contentType(APPLICATION_JSON).content("{ \"ids\": [1, 2] }"))
                .andExpect(status().isIAmATeapot())
                .andExpect(jsonPath("$.message").value("3P потребности #1, 2 уже были экспортированы"));
    }

    @Test
    @DbUnitDataSet(before = "ParentDemand3pControllerTest.before.csv",
            after = "ParentDemand3pControllerTest.success.after.csv")
    public void testExportWithoutDraftSuccess() throws Exception {
        mockMvc.perform(post(EXPORT_URL).contentType(APPLICATION_JSON).content(REQUEST_ID_1))
                .andExpect(status().isOk());
    }

    @Test
    @DbUnitDataSet(before = "ParentDemand3pControllerTest_exportWithDraft.before.csv",
            after = "ParentDemand3pControllerTest_exportWithDraft.success.after.csv")
    public void testExportWithDraftSuccess() throws Exception {
        mockMvc.perform(post(EXPORT_URL).contentType(APPLICATION_JSON).content(REQUEST_ID_1))
                .andExpect(status().isOk());
    }

    @Test
    @DbUnitDataSet(
        before = "RecommendationControllerTest.test3PRecommendationsWithSummary_checkSalesAndStocks.before.csv")
    public void testExport_checkExtendedInfos() throws Exception {
        mockMvc.perform(post(EXPORT_URL).contentType(APPLICATION_JSON).content("{ \"ids\": [1] }"))
                .andExpect(status().isOk());

        int[] salesId1 = new int[8];
        for (int i = 0; i < 8; i++) {
            salesId1[i] = 1 + 2 + 2 * i;
        }
        int[] salesId2 = new int[8];
        for (int i = 0; i < 8; i++) {
            salesId2[i] = 4 + i;
        }
        int[] salesSum = new int[8];
        for (int i = 0; i < 8; i++) {
            salesSum[i] = salesId1[i] + salesId2[i];
        }

        final RecommendationFilters recommendationFilters = new RecommendationFilters();
        final RecommendationFilter recommendationFilter = new RecommendationFilter();
        recommendationFilter.setDemandIds(Arrays.asList(1L, 2L));
        recommendationFilters.setFilter(recommendationFilter);

        var r = mockMvc.perform(post("/api/v2/recommendations/with-count?demandType=TYPE_3P")
                        .contentType(APPLICATION_JSON_UTF8)
                        .content(dtoToString(recommendationFilters))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recommendations.length()").value(3));

        r.andExpect(jsonPath("$.recommendations[0].purchaseQuantity").value(9))
                .andExpect(jsonPath("$.recommendations[1].purchaseQuantity").value(7))
                .andExpect(jsonPath("$.recommendations[2].purchaseQuantity").value(16))

                .andExpect(jsonPath("$.recommendations[0].id").value(1))
                .andExpect(jsonPath("$.recommendations[1].id").value(2))
                .andExpect(jsonPath("$.recommendations[2].id").value(-1))

                .andExpect(jsonPath("$.recommendations[0].sales1p[0]").value(salesId1[0]))
                .andExpect(jsonPath("$.recommendations[0].sales1p[1]").value(salesId1[1]))
                .andExpect(jsonPath("$.recommendations[0].sales1p[2]").value(salesId1[2]))
                .andExpect(jsonPath("$.recommendations[0].sales1p[3]").value(salesId1[3]))
                .andExpect(jsonPath("$.recommendations[0].sales1p[4]").value(salesId1[4]))
                .andExpect(jsonPath("$.recommendations[0].sales1p[5]").value(salesId1[5]))
                .andExpect(jsonPath("$.recommendations[0].sales1p[6]").value(salesId1[6]))
                .andExpect(jsonPath("$.recommendations[0].sales1p[7]").value(salesId1[7]))

                .andExpect(jsonPath("$.recommendations[0].salesAll[0]").value(salesId1[0]))
                .andExpect(jsonPath("$.recommendations[0].salesAll[1]").value(salesId1[1]))
                .andExpect(jsonPath("$.recommendations[0].salesAll[2]").value(salesId1[2]))
                .andExpect(jsonPath("$.recommendations[0].salesAll[3]").value(salesId1[3]))
                .andExpect(jsonPath("$.recommendations[0].salesAll[4]").value(salesId1[4]))
                .andExpect(jsonPath("$.recommendations[0].salesAll[5]").value(salesId1[5]))
                .andExpect(jsonPath("$.recommendations[0].salesAll[6]").value(salesId1[6]))
                .andExpect(jsonPath("$.recommendations[0].salesAll[7]").value(salesId1[7]))

                .andExpect(jsonPath("$.recommendations[1].sales1p[0]").value(salesId2[0]))
                .andExpect(jsonPath("$.recommendations[1].sales1p[1]").value(salesId2[1]))
                .andExpect(jsonPath("$.recommendations[1].sales1p[2]").value(salesId2[2]))
                .andExpect(jsonPath("$.recommendations[1].sales1p[3]").value(salesId2[3]))
                .andExpect(jsonPath("$.recommendations[1].sales1p[4]").value(salesId2[4]))
                .andExpect(jsonPath("$.recommendations[1].sales1p[5]").value(salesId2[5]))
                .andExpect(jsonPath("$.recommendations[1].sales1p[6]").value(salesId2[6]))
                .andExpect(jsonPath("$.recommendations[1].sales1p[7]").value(salesId2[7]))

                .andExpect(jsonPath("$.recommendations[1].salesAll[0]").value(salesId2[0]))
                .andExpect(jsonPath("$.recommendations[1].salesAll[1]").value(salesId2[1]))
                .andExpect(jsonPath("$.recommendations[1].salesAll[2]").value(salesId2[2]))
                .andExpect(jsonPath("$.recommendations[1].salesAll[3]").value(salesId2[3]))
                .andExpect(jsonPath("$.recommendations[1].salesAll[4]").value(salesId2[4]))
                .andExpect(jsonPath("$.recommendations[1].salesAll[5]").value(salesId2[5]))
                .andExpect(jsonPath("$.recommendations[1].salesAll[6]").value(salesId2[6]))
                .andExpect(jsonPath("$.recommendations[1].salesAll[7]").value(salesId2[7]))

                .andExpect(jsonPath("$.recommendations[2].sales1p[0]").value(salesSum[0]))
                .andExpect(jsonPath("$.recommendations[2].sales1p[1]").value(salesSum[1]))
                .andExpect(jsonPath("$.recommendations[2].sales1p[2]").value(salesSum[2]))
                .andExpect(jsonPath("$.recommendations[2].sales1p[3]").value(salesSum[3]))
                .andExpect(jsonPath("$.recommendations[2].sales1p[4]").value(salesSum[4]))
                .andExpect(jsonPath("$.recommendations[2].sales1p[5]").value(salesSum[5]))
                .andExpect(jsonPath("$.recommendations[2].sales1p[6]").value(salesSum[6]))
                .andExpect(jsonPath("$.recommendations[2].sales1p[7]").value(salesSum[7]))

                .andExpect(jsonPath("$.recommendations[2].salesAll[0]").value(salesSum[0]))
                .andExpect(jsonPath("$.recommendations[2].salesAll[1]").value(salesSum[1]))
                .andExpect(jsonPath("$.recommendations[2].salesAll[2]").value(salesSum[2]))
                .andExpect(jsonPath("$.recommendations[2].salesAll[3]").value(salesSum[3]))
                .andExpect(jsonPath("$.recommendations[2].salesAll[4]").value(salesSum[4]))
                .andExpect(jsonPath("$.recommendations[2].salesAll[5]").value(salesSum[5]))
                .andExpect(jsonPath("$.recommendations[2].salesAll[6]").value(salesSum[6]))
                .andExpect(jsonPath("$.recommendations[2].salesAll[7]").value(salesSum[7]))

                .andExpect(jsonPath("$.recommendations[0].stock").value(10))
                .andExpect(jsonPath("$.recommendations[1].stock").value(15))
                .andExpect(jsonPath("$.recommendations[2].stock").value(25))

                .andExpect(jsonPath("$.recommendations[0].stockOverall").value(65))
                .andExpect(jsonPath("$.recommendations[1].stockOverall").value(35))
                .andExpect(jsonPath("$.recommendations[2].stockOverall").value(100))

                .andExpect(jsonPath("$.recommendations[0].transit").value(2))
                .andExpect(jsonPath("$.recommendations[1].transit").value(0))
                .andExpect(jsonPath("$.recommendations[2].transit").value(2))

                .andExpect(jsonPath("$.recommendations[0].oosDays").value(7))
                .andExpect(jsonPath("$.recommendations[1].oosDays").doesNotExist())
                .andExpect(jsonPath("$.recommendations[2].oosDays").value(7))
        ;
    }

    @Test
    @DbUnitDataSet(before = "ParentDemand3pControllerTest.get.csv")
    public void getByIdsSuccess() throws Exception {
        mockMvc.perform(get("/api/v1/demands-3p?id=1,2")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].regionId").value(145))
                .andExpect(jsonPath("$[0].userId").value(1))
                .andExpect(jsonPath("$[0].sum").value(200))
                .andExpect(jsonPath("$[0].mskus").value(1))
                .andExpect(jsonPath("$[0].items").value(4))
                .andExpect(jsonPath("$[0].adjustedMskus").value(1))
                .andExpect(jsonPath("$[0].adjustedItems").value(1))
                .andExpect(jsonPath("$[0].adjustedSum").value(400))
                .andExpect(jsonPath("$[0].orderDate").value("2021-04-24"))
                .andExpect(jsonPath("$[0].status").value("NEW"))
                .andExpect(jsonPath("$[0].createdDate").value("2021-04-28"))

                .andExpect(jsonPath("$[1].id").value(2))
                .andExpect(jsonPath("$[1].regionId").value(145))
                .andExpect(jsonPath("$[1].userId").isEmpty())
                .andExpect(jsonPath("$[1].sum").value(200))
                .andExpect(jsonPath("$[1].mskus").value(1))
                .andExpect(jsonPath("$[1].items").value(4))
                .andExpect(jsonPath("$[1].adjustedMskus").value(1))
                .andExpect(jsonPath("$[1].adjustedItems").value(1))
                .andExpect(jsonPath("$[1].adjustedSum").value(400))
                .andExpect(jsonPath("$[1].orderDate").value("2021-04-25"))
                .andExpect(jsonPath("$[1].status").value("ORDER_CREATED"))
                .andExpect(jsonPath("$[1].createdDate").value("2021-04-28"));
    }

    @Test
    @DbUnitDataSet(before = "ParentDemand3pControllerTest.get.csv")
    public void getByDates1() throws Exception {
        mockMvc.perform(get("/api/v1/demands-3p"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[1].id").value(2));
    }

    @Test
    @DbUnitDataSet(before = "ParentDemand3pControllerTest.get.csv")
    public void getByDatesAndCatteamId() throws Exception {
        mockMvc.perform(get("/api/v1/demands-3p?catteamId=5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    @DbUnitDataSet(before = "ParentDemand3pControllerTest.get.csv")
    public void getByDates2() throws Exception {
        mockMvc.perform(get("/api/v1/demands-3p?from=2021-04-25"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(2))
                .andExpect(jsonPath("$[1].id").value(3));
    }

    @Test
    @DbUnitDataSet(before = "ParentDemand3pControllerTest.get.csv")
    public void getByDates3() throws Exception {
        mockMvc.perform(get("/api/v1/demands-3p?from=2021-04-25&to=2021-06-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].id").value(2))
                .andExpect(jsonPath("$[1].id").value(3))
                .andExpect(jsonPath("$[2].id").value(4));
    }

    @Test
    @DbUnitDataSet(before = "ParentDemand3pControllerTest.get.csv")
    public void getByDatesWithRegion() throws Exception {
        mockMvc.perform(get("/api/v1/demands-3p?from=2021-04-25&to=2021-06-01&region=147"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(4));
    }

    @Test
    @DbUnitDataSet(before = "ParentDemand3pControllerTest.get.csv")
    public void getByDatesEmptyResult() throws Exception {
        mockMvc.perform(get("/api/v1/demands-3p?from=2022-06-01&to=2022-06-25"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DbUnitDataSet(before = "ParentDemand3pControllerTest.testGetParentDemand3pWithSummary.before.csv")
    public void getParentDemand3pExcel() throws Exception {
        byte[] excelData = mockMvc.perform(get("/api/v1/demands-3p/1/excel"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsByteArray();

        AdjustedRecommendationsExcelReader reader = new AdjustedRecommendationsExcelReader(getCorrectionReasonMap());
        AdjustedRecommendationsDTO parsed = reader.read(new ByteArrayInputStream(excelData), DemandType.TYPE_3P);

        assertNotNull(parsed.getDemandId());
        assertEquals(1, parsed.getDemandId().intValue());

        List<AdjustedRecommendationDTO> recs = parsed.getAdjustedRecommendations();
        assertEquals(2, recs.size());

        var expectedMsku = new int[] {200, 300};
        var expectedAdjPurchQty = new int[] {316, 300};

        for(int i = 0; i < 2; i++) {
            AdjustedRecommendationDTO rec = recs.get(i);
            assertNotNull(rec);

            assertNull(rec.getId());
            assertNull(rec.getCorrectionReason());
            assertNull(rec.getNeedsManualReview());

            Long msku = rec.getMsku();
            assertNotNull(msku);
            assertEquals(expectedMsku[i], msku.intValue());

            Integer adjustedPurchQty = rec.getAdjustedPurchQty();
            assertNotNull(adjustedPurchQty);
            assertEquals(expectedAdjPurchQty[i], adjustedPurchQty.intValue());

            assertNotNull(rec.getGroupId());
            assertEquals(0, rec.getGroupId().intValue());
        }
    }

    private Map<String, Long> getCorrectionReasonMap() {
        return correctionReasonRepository.findAll().stream()
                .collect(Collectors.toMap(CorrectionReason::getName, CorrectionReason::getPosition));
    }
}
