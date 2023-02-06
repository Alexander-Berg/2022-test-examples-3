package ru.yandex.market.replenishment.autoorder.api;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang.ArrayUtils;
import org.hamcrest.Matcher;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.replenishment.autoorder.api.dto.user_fillters.recommendation.RecommendationFilters;
import ru.yandex.market.replenishment.autoorder.api.dto.user_fillters.recommendation.RecommendationUserFilter;
import ru.yandex.market.replenishment.autoorder.api.dto.warning_filters.RecommendationWarningFiltersDTO;
import ru.yandex.market.replenishment.autoorder.config.ControllerTest;
import ru.yandex.market.replenishment.autoorder.config.ExcelTestingHelper;
import ru.yandex.market.replenishment.autoorder.model.ABC;
import ru.yandex.market.replenishment.autoorder.model.DemandType;
import ru.yandex.market.replenishment.autoorder.model.FilterType;
import ru.yandex.market.replenishment.autoorder.model.HonestSign;
import ru.yandex.market.replenishment.autoorder.model.RecommendationFilter;
import ru.yandex.market.replenishment.autoorder.model.StockCoverType;
import ru.yandex.market.replenishment.autoorder.model.WarningFilterType;
import ru.yandex.market.replenishment.autoorder.model.WeeksType;
import ru.yandex.market.replenishment.autoorder.model.dto.AdjustedRecommendationDTO;
import ru.yandex.market.replenishment.autoorder.model.dto.AdjustedRecommendationsDTO;
import ru.yandex.market.replenishment.autoorder.model.dto.AdjustedSummaryRecommendationsDTO;
import ru.yandex.market.replenishment.autoorder.model.dto.AdjustmentDTO;
import ru.yandex.market.replenishment.autoorder.model.dto.DemandIdentityDTO;
import ru.yandex.market.replenishment.autoorder.model.dto.NeedsManualReviewRequest;
import ru.yandex.market.replenishment.autoorder.model.dto.TransitRequest;
import ru.yandex.market.replenishment.autoorder.repository.postgres.SalesRepository;
import ru.yandex.market.replenishment.autoorder.security.WithMockLogin;
import ru.yandex.market.replenishment.autoorder.service.ParentDemand3pUpdateLoader;
import ru.yandex.market.replenishment.autoorder.service.RecommendationService;
import ru.yandex.market.replenishment.autoorder.service.client.LiluCRMClient;
import ru.yandex.market.replenishment.autoorder.service.client.PushOkClient;
import ru.yandex.market.replenishment.autoorder.service.client.StarTrekClient;
import ru.yandex.market.replenishment.autoorder.service.user_filters.base.UserFilterFieldPredicate;
import ru.yandex.market.replenishment.autoorder.utils.CompoziteResultMatcher;
import ru.yandex.market.replenishment.autoorder.utils.IntegerCapturingMatcher;
import ru.yandex.market.replenishment.autoorder.utils.TestUtils;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.isOneOf;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.springframework.http.MediaType.APPLICATION_JSON_UTF8;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.replenishment.autoorder.api.dto.user_fillters.recommendation.RecommendationField.MSKU;
import static ru.yandex.market.replenishment.autoorder.api.dto.user_fillters.recommendation.RecommendationField.PROMO_PURCHASE;
import static ru.yandex.market.replenishment.autoorder.api.dto.user_fillters.recommendation.RecommendationField.SCB;
import static ru.yandex.market.replenishment.autoorder.api.dto.user_fillters.recommendation.RecommendationField.SCF;
import static ru.yandex.market.replenishment.autoorder.api.dto.user_fillters.recommendation.RecommendationField.WAREHOUSE;
import static ru.yandex.market.replenishment.autoorder.model.FilterType.ALL;
import static ru.yandex.market.replenishment.autoorder.model.FilterType.ASSORTMENT_GOODS_SUB_SSKU;
import static ru.yandex.market.replenishment.autoorder.model.FilterType.MULTIPLE_WAREHOUSES;
import static ru.yandex.market.replenishment.autoorder.model.FilterType.SPECIAL_ORDER;
import static ru.yandex.market.replenishment.autoorder.service.user_filters.base.UserFilterFieldPredicate.CONTAINS;
import static ru.yandex.market.replenishment.autoorder.service.user_filters.base.UserFilterFieldPredicate.TRUE;
import static ru.yandex.market.replenishment.autoorder.utils.TestUtils.dtoToString;

@WithMockLogin
public class RecommendationControllerTest extends ControllerTest {

    @Autowired
    RecommendationService recommendationService;

    @Autowired
    SalesRepository salesRepository;

    @Autowired
    ParentDemand3pUpdateLoader parentDemand3pUpdateLoader;

    @MockBean
    LiluCRMClient liluCRMClient;

    @MockBean
    PushOkClient pushOkClient;

    @MockBean
    StarTrekClient starTrekClient;

    @Rule
    public ExpectedException expectedEx = ExpectedException.none();

    private final ExcelTestingHelper excelTestingHelper = new ExcelTestingHelper(this);

    private static final int WAREHOUSE_ID = 171;

    @Before
    public void mockWorkbookConfigAndTimeService() {
        setTestTime(LocalDateTime.of(2020, 12, 22, 0, 0));
    }

    @Test
    @DbUnitDataSet(before = "RecommendationControllerTest.adjQtyRecommendationsWarehouseFrom.before.csv",
        after = "RecommendationControllerTest.adjQtyRecommendationsWarehouseFrom.after.csv")
    public void adjQtyRecommendationsWarehouseFrom() throws Exception {
        AdjustedRecommendationDTO adjustedRecommendationDTO = new AdjustedRecommendationDTO();
        adjustedRecommendationDTO.setNeedsManualReview(false);
        adjustedRecommendationDTO.setAdjustedPurchQty(12);
        adjustedRecommendationDTO.setGroupId(0L);
        adjustedRecommendationDTO.setMsku(100L);
        adjustedRecommendationDTO.setId(1L);

        AdjustedRecommendationsDTO adjustedRecommendationsDTO = new AdjustedRecommendationsDTO();
        adjustedRecommendationsDTO.setAdjustedRecommendations(Collections.singletonList(adjustedRecommendationDTO));
        adjustedRecommendationsDTO.setDemandVersion(1);
        adjustedRecommendationsDTO.setDemandId(1L);

        mockMvc.perform(put("/api/v1/recommendations/adjust").contentType(APPLICATION_JSON_UTF8)
                .content(dtoToString(adjustedRecommendationsDTO)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.adjustedRecommendations[0].id").value(1))
            .andExpect(jsonPath("$.adjustedRecommendations[0].msku").value(100))
            .andExpect(jsonPath("$.adjustedRecommendations[0].ssku").value("020.111"))
            .andExpect(jsonPath("$.adjustedRecommendations[0].warehouseId").value(171))
            .andExpect(jsonPath("$.adjustedRecommendations[0].needsManualReview").value(false))
            .andExpect(jsonPath("$.adjustedRecommendations[0].purchaseQuantity").value(9))
            .andExpect(jsonPath("$.adjustedRecommendations[0].setQuantity").value(12))
            .andExpect(jsonPath("$.warningsChanged").value(true));
    }

    @Test
    @DbUnitDataSet(before = "RecommendationControllerTest.adjQtyRecommendationsWarehouseFrom.before.csv")
    public void adjQtyRecommendationsWithoutChanges() throws Exception {
        AdjustedRecommendationDTO adjustedRecommendationDTO = new AdjustedRecommendationDTO();
        adjustedRecommendationDTO.setNeedsManualReview(false);
        adjustedRecommendationDTO.setAdjustedPurchQty(50);
        adjustedRecommendationDTO.setGroupId(0L);
        adjustedRecommendationDTO.setMsku(100L);
        adjustedRecommendationDTO.setId(1L);

        AdjustedRecommendationsDTO adjustedRecommendationsDTO = new AdjustedRecommendationsDTO();
        adjustedRecommendationsDTO.setAdjustedRecommendations(Collections.singletonList(adjustedRecommendationDTO));
        adjustedRecommendationsDTO.setDemandVersion(1);
        adjustedRecommendationsDTO.setDemandId(1L);

        mockMvc.perform(put("/api/v1/recommendations/adjust").contentType(APPLICATION_JSON_UTF8)
                .content(dtoToString(adjustedRecommendationsDTO)))
            .andExpect(status().isOk());
    }

    @Test
    @DbUnitDataSet(before = "RecommendationControllerTest.adjQty3pParentedRecommendations.before.csv",
        after = "RecommendationControllerTest.adjQty3pParentedRecommendations.after.csv")
    public void adjQty3pParentedRecommendations() throws Exception {
        String url = "/api/v1/recommendations/adjust?demandType=TYPE_3P";

        AdjustedRecommendationsDTO adjustedRecommendationsDTO = new AdjustedRecommendationsDTO();
        AdjustedRecommendationDTO adjustedRecommendationDTO = new AdjustedRecommendationDTO();
        adjustedRecommendationDTO.setMsku(100L);
        adjustedRecommendationDTO.setAdjustedPurchQty(12);
        adjustedRecommendationDTO.setId(1L);
        adjustedRecommendationDTO.setNeedsManualReview(false);
        adjustedRecommendationDTO.setGroupId(0L);

        adjustedRecommendationsDTO.setAdjustedRecommendations(Collections.singletonList(adjustedRecommendationDTO));
        adjustedRecommendationsDTO.setDemandId(1L);
        adjustedRecommendationsDTO.setDemandVersion(1);

        mockMvc.perform(put(url).contentType(APPLICATION_JSON_UTF8)
                .content(dtoToString(adjustedRecommendationsDTO)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.adjustedRecommendations[0].id").value(1));
        parentDemand3pUpdateLoader.load();
    }

    @Test
    @DbUnitDataSet(before = "RecommendationControllerTest.adjQty3pParentedRecommendationsNotAffected1pAndTender" +
        ".before.csv",
        after = "RecommendationControllerTest.adjQty3pParentedRecommendationsNotAffected1pAndTender.after.csv")
    public void adjQty3pParentedRecommendationsNotAffected1pAndTender() throws Exception {
        String url = "/api/v1/recommendations/adjust?demandType=TYPE_1P";

        AdjustedRecommendationsDTO adjustedRecommendationsDTO = new AdjustedRecommendationsDTO();
        AdjustedRecommendationDTO adjustedRecommendationDTO = new AdjustedRecommendationDTO();
        adjustedRecommendationDTO.setMsku(100L);
        adjustedRecommendationDTO.setAdjustedPurchQty(12);
        adjustedRecommendationDTO.setId(2L);
        adjustedRecommendationDTO.setNeedsManualReview(false);
        adjustedRecommendationDTO.setGroupId(0L);
        adjustedRecommendationDTO.setCorrectionReason(1L);

        adjustedRecommendationsDTO.setAdjustedRecommendations(Collections.singletonList(adjustedRecommendationDTO));
        adjustedRecommendationsDTO.setDemandId(2L);
        adjustedRecommendationsDTO.setDemandVersion(1);

        mockMvc.perform(put(url).contentType(APPLICATION_JSON_UTF8)
                .content(dtoToString(adjustedRecommendationsDTO)))
            .andExpect(status().isOk());
    }

    @Test
    @DbUnitDataSet(before = "RecommendationControllerTest.adjQtyTenderRecommendations.before.csv",
        after = "RecommendationControllerTest.adjQtyTenderRecommendations.after.csv")
    public void adjQtyTenderRecommendations() throws Exception {
        String url = "/api/v1/recommendations/adjust?demandType=TENDER";

        AdjustedRecommendationsDTO adjustedRecommendationsDTO = new AdjustedRecommendationsDTO();
        AdjustedRecommendationDTO adjustedRecommendationDTO = new AdjustedRecommendationDTO();
        adjustedRecommendationDTO.setMsku(100L);
        adjustedRecommendationDTO.setAdjustedPurchQty(12);
        adjustedRecommendationDTO.setId(1L);
        adjustedRecommendationDTO.setNeedsManualReview(false);
        adjustedRecommendationDTO.setGroupId(0L);

        adjustedRecommendationsDTO.setAdjustedRecommendations(Collections.singletonList(adjustedRecommendationDTO));
        adjustedRecommendationsDTO.setDemandId(1L);
        adjustedRecommendationsDTO.setDemandVersion(1);

        mockMvc.perform(put(url).contentType(APPLICATION_JSON_UTF8)
                .content(dtoToString(adjustedRecommendationsDTO)))
            .andExpect(status().isOk());
    }

    @Test
    @DbUnitDataSet(before = "RecommendationControllerTest.3p_adjRecommendations.before.csv",
        after = "RecommendationControllerTest.3p_adjQtySummaryRecommendations.after.csv")
    public void adjQty3pRecommendations_OneDemandId() throws Exception {
        adjQty3pRecommendations(Collections.singletonList(
            new DemandIdentityDTO(1L, 1)
        ));
    }

    @Test
    @DbUnitDataSet(before = "RecommendationControllerTest.3p_adjRecommendations.before.csv",
        after = "RecommendationControllerTest.3p_adjQtySummaryRecommendations.after.csv")
    public void adjQty3pRecommendations_FewDemandIds() throws Exception {
        adjQty3pRecommendations(Arrays.asList(
            new DemandIdentityDTO(1L, 1),
            new DemandIdentityDTO(2L, 1),
            new DemandIdentityDTO(3L, 1)
        ));
    }

    @Test
    @DbUnitDataSet(before = "RecommendationControllerTest.3p_adjRecommendationsV1.before.csv",
        after = "RecommendationControllerTest.3p_adjQtySummaryRecommendations.after.csv")
    public void adjQty3pRecommendationsV1_OneDemandId() throws Exception {
        adjQty3pRecommendationsV1(Collections.singletonList(
            new DemandIdentityDTO(1L, 1)
        ));
    }

    @Test
    @DbUnitDataSet(before = "RecommendationControllerTest.3p_adjRecommendationsV1.before.csv",
        after = "RecommendationControllerTest.3p_adjQtySummaryRecommendations.after.csv")
    public void adjQty3pRecommendationsV1_FewDemandIds() throws Exception {
        adjQty3pRecommendationsV1(Arrays.asList(
            new DemandIdentityDTO(1L, 1),
            new DemandIdentityDTO(2L, 1),
            new DemandIdentityDTO(3L, 1)
        ));
    }

    private void adjQty3pRecommendations(List<DemandIdentityDTO> demandIdentityDTOs) throws Exception {
        getResultOfAdjQty3pRecommendations("/api/v2/recommendations/summary/adjust", demandIdentityDTOs)
            .andExpect(jsonPath("$.adjustedRecommendations").isNotEmpty())
            .andExpect(jsonPath("$.adjustedRecommendations", hasSize(3)))
            .andExpect(jsonPath("$.warningsChanged").value(true))
            .andExpect(jsonPath("$.adjustedRecommendations[?(@.warehouseId==145)].setQuantity").value(4))
            .andExpect(jsonPath("$.adjustedRecommendations[?(@.warehouseId==145)].correctionReason.id").value(3))
            .andExpect(jsonPath("$.adjustedRecommendations[?(@.id==-1)].stockCoverBackward").exists())
            .andExpect(jsonPath("$.adjustedRecommendations[?(@.id==-1)].stockCoverBackward1p").exists())
            .andExpect(jsonPath("$.adjustedRecommendations[?(@.id==-1)].stockCoverForward").exists());
        parentDemand3pUpdateLoader.load();
    }

    private void adjQty3pRecommendationsV1(List<DemandIdentityDTO> demandIdentityDTOs) throws Exception {
        getResultOfAdjQty3pRecommendations("/api/v1/recommendations/summary/adjust", demandIdentityDTOs)
            .andExpect(jsonPath("$").isNotEmpty())
            .andExpect(jsonPath("$", hasSize(3)))
            .andExpect(jsonPath("$[?(@.warehouseId==145)].setQuantity").value(4))
            .andExpect(jsonPath("$[?(@.warehouseId==145)].correctionReason.id").value(3))
            .andExpect(jsonPath("$[?(@.id==-1)].stockCoverBackward").exists())
            .andExpect(jsonPath("$[?(@.id==-1)].stockCoverForward").exists());
        parentDemand3pUpdateLoader.load();
    }

    private ResultActions getResultOfAdjQty3pRecommendations(final String url,
                                                             List<DemandIdentityDTO> demandIdentityDTOs
    ) throws Exception {
        AdjustedSummaryRecommendationsDTO adjustedRecommendationsDTO = new AdjustedSummaryRecommendationsDTO();
        AdjustedRecommendationDTO adjustedRecommendationDTO = new AdjustedRecommendationDTO();
        adjustedRecommendationDTO.setMsku(100L);
        adjustedRecommendationDTO.setAdjustedPurchQty(12);
        adjustedRecommendationDTO.setCorrectionReason(3L);
        adjustedRecommendationDTO.setId(1L);
        adjustedRecommendationDTO.setNeedsManualReview(false);
        adjustedRecommendationDTO.setGroupId(0L);

        adjustedRecommendationsDTO.setAdjustedRecommendations(Collections.singletonList(adjustedRecommendationDTO));
        adjustedRecommendationsDTO.setDemandKeys(demandIdentityDTOs);

        return mockMvc.perform(put(url).contentType(APPLICATION_JSON_UTF8)
            .content(dtoToString(adjustedRecommendationsDTO)));
    }

    @Test
    @DbUnitDataSet(before = "RecommendationControllerTest.3p_adjRecommendationsV1.before.csv",
        after = "RecommendationControllerTest.3p_adjQtySummaryRecommendations_fewMsku.after.csv")
    public void adjQty3pRecommendationsV1_adjFewMskus() throws Exception {
        String url = "/api/v1/recommendations/summary/adjust";

        List<AdjustedRecommendationDTO> adjustedSummaryRecommendationsDTOs = new ArrayList<>(2);

        AdjustedRecommendationDTO a = new AdjustedRecommendationDTO();
        a.setMsku(100L);
        a.setAdjustedPurchQty(12);
        a.setCorrectionReason(3L);
        a.setId(1L);
        a.setNeedsManualReview(false);
        a.setGroupId(0L);
        adjustedSummaryRecommendationsDTOs.add(a);

        a = new AdjustedRecommendationDTO();
        a.setMsku(200L);
        a.setAdjustedPurchQty(24);
        a.setCorrectionReason(3L);
        a.setId(1L);
        a.setNeedsManualReview(false);
        a.setGroupId(0L);
        adjustedSummaryRecommendationsDTOs.add(a);

        AdjustedSummaryRecommendationsDTO adjustedRecommendationsDTO = new AdjustedSummaryRecommendationsDTO();
        adjustedRecommendationsDTO.setAdjustedRecommendations(adjustedSummaryRecommendationsDTOs);
        adjustedRecommendationsDTO.setDemandKeys(Collections.singletonList(new DemandIdentityDTO(1L, 1)));

        mockMvc.perform(put(url).contentType(APPLICATION_JSON_UTF8)
                .content(dtoToString(adjustedRecommendationsDTO)))
            .andExpect(jsonPath("$").isNotEmpty())
            .andExpect(jsonPath("$", hasSize(7)))
            .andExpect(jsonPath("$[?(@.id==1)].setQuantity").value(4))
            .andExpect(jsonPath("$[?(@.id==1)].correctionReason.id").value(3))
            .andExpect(jsonPath("$[?(@.id==2)].setQuantity").value(8))
            .andExpect(jsonPath("$[?(@.id==2)].correctionReason.id").value(3))
            .andExpect(jsonPath("$[?(@.id==-1)].stockCoverBackward").exists())
            .andExpect(jsonPath("$[?(@.id==-1)].stockCoverForward").exists())

            .andExpect(jsonPath("$[?(@.id==3)].setQuantity").value(6))
            .andExpect(jsonPath("$[?(@.id==3)].correctionReason.id").value(3))
            .andExpect(jsonPath("$[?(@.id==4)].setQuantity").value(8))
            .andExpect(jsonPath("$[?(@.id==4)].correctionReason.id").value(3))
            .andExpect(jsonPath("$[?(@.id==5)].setQuantity").value(10))
            .andExpect(jsonPath("$[?(@.id==5)].correctionReason.id").value(3))
            .andExpect(jsonPath("$[?(@.id==-3)].stockCoverBackward").exists())
            .andExpect(jsonPath("$[?(@.id==-3)].stockCoverForward").exists());
        parentDemand3pUpdateLoader.load();
    }

    @Test
    @DbUnitDataSet(before = "RecommendationControllerTest.3p_adjRecommendations.before.csv",
        after = "RecommendationControllerTest.3p_adjQtySummaryRecommendations_fewMsku.after.csv")
    public void adjQty3pRecommendations_adjFewMskus() throws Exception {
        String url = "/api/v2/recommendations/summary/adjust";

        List<AdjustedRecommendationDTO> adjustedSummaryRecommendationsDTOs = new ArrayList<>(2);

        AdjustedRecommendationDTO a = new AdjustedRecommendationDTO();
        a.setMsku(100L);
        a.setAdjustedPurchQty(12);
        a.setCorrectionReason(3L);
        a.setId(1L);
        a.setNeedsManualReview(false);
        a.setGroupId(0L);
        adjustedSummaryRecommendationsDTOs.add(a);

        a = new AdjustedRecommendationDTO();
        a.setMsku(200L);
        a.setAdjustedPurchQty(24);
        a.setCorrectionReason(3L);
        a.setId(1L);
        a.setNeedsManualReview(false);
        a.setGroupId(0L);
        adjustedSummaryRecommendationsDTOs.add(a);

        AdjustedSummaryRecommendationsDTO adjustedRecommendationsDTO = new AdjustedSummaryRecommendationsDTO();
        adjustedRecommendationsDTO.setAdjustedRecommendations(adjustedSummaryRecommendationsDTOs);
        adjustedRecommendationsDTO.setDemandKeys(Collections.singletonList(new DemandIdentityDTO(1L, 1)));

        mockMvc.perform(put(url).contentType(APPLICATION_JSON_UTF8)
                .content(dtoToString(adjustedRecommendationsDTO)))
            .andExpect(jsonPath("$").isNotEmpty())
            .andExpect(jsonPath("$.adjustedRecommendations", hasSize(7)))
            .andExpect(jsonPath("$.warningsChanged").value(true))
            .andExpect(jsonPath("$.adjustedRecommendations[?(@.id==1)].setQuantity").value(4))
            .andExpect(jsonPath("$.adjustedRecommendations[?(@.id==1)].correctionReason.id").value(3))
            .andExpect(jsonPath("$.adjustedRecommendations[?(@.id==2)].setQuantity").value(8))
            .andExpect(jsonPath("$.adjustedRecommendations[?(@.id==2)].correctionReason.id").value(3))
            .andExpect(jsonPath("$.adjustedRecommendations[?(@.id==-1)].stockCoverBackward").exists())
            .andExpect(jsonPath("$.adjustedRecommendations[?(@.id==-1)].stockCoverForward").exists())

            .andExpect(jsonPath("$.adjustedRecommendations[?(@.id==3)].setQuantity").value(6))
            .andExpect(jsonPath("$.adjustedRecommendations[?(@.id==3)].correctionReason.id").value(3))
            .andExpect(jsonPath("$.adjustedRecommendations[?(@.id==4)].setQuantity").value(8))
            .andExpect(jsonPath("$.adjustedRecommendations[?(@.id==4)].correctionReason.id").value(3))
            .andExpect(jsonPath("$.adjustedRecommendations[?(@.id==5)].setQuantity").value(10))
            .andExpect(jsonPath("$.adjustedRecommendations[?(@.id==5)].correctionReason.id").value(3))
            .andExpect(jsonPath("$.adjustedRecommendations[?(@.id==-3)].stockCoverBackward").exists())
            .andExpect(jsonPath("$.adjustedRecommendations[?(@.id==-3)].stockCoverForward").exists());
        parentDemand3pUpdateLoader.load();
    }

    @Test
    @DbUnitDataSet(before = "RecommendationControllerTest.3p_adjRecommendationsV1_withRoundByQuantum.before.csv",
        after = "RecommendationControllerTest.3p_adjQtySummaryRecommendations_fewMskuWithRoundByQuantum.after.csv")
    public void adjQty3pRecommendationsV1_adjFewMskusWithRoundByQuantum() throws Exception {
        String url = "/api/v1/recommendations/summary/adjust";

        List<AdjustedRecommendationDTO> adjustedSummaryRecommendationsDTOs = new ArrayList<>(2);

        AdjustedRecommendationDTO a = new AdjustedRecommendationDTO();
        a.setMsku(100L);
        a.setAdjustedPurchQty(2);
        a.setCorrectionReason(3L);
        a.setId(1L);
        a.setNeedsManualReview(false);
        a.setGroupId(0L);
        adjustedSummaryRecommendationsDTOs.add(a);

        a = new AdjustedRecommendationDTO();
        a.setMsku(200L);
        a.setAdjustedPurchQty(30);
        a.setCorrectionReason(3L);
        a.setId(1L);
        a.setNeedsManualReview(false);
        a.setGroupId(0L);
        adjustedSummaryRecommendationsDTOs.add(a);

        AdjustedSummaryRecommendationsDTO adjustedRecommendationsDTO = new AdjustedSummaryRecommendationsDTO();
        adjustedRecommendationsDTO.setAdjustedRecommendations(adjustedSummaryRecommendationsDTOs);
        adjustedRecommendationsDTO.setDemandKeys(Collections.singletonList(new DemandIdentityDTO(1L, 1)));

        mockMvc.perform(put(url).contentType(APPLICATION_JSON_UTF8)
                .content(dtoToString(adjustedRecommendationsDTO)))
            .andExpect(jsonPath("$").isNotEmpty())
            .andExpect(jsonPath("$", hasSize(7)))
            .andExpect(jsonPath("$[?(@.id==1)].setQuantity").value(0))
            .andExpect(jsonPath("$[?(@.id==1)].correctionReason.id").value(3))
            .andExpect(jsonPath("$[?(@.id==2)].setQuantity").value(4))
            .andExpect(jsonPath("$[?(@.id==2)].correctionReason.id").value(3))
            .andExpect(jsonPath("$[?(@.id==-1)].stockCoverBackward").exists())
            .andExpect(jsonPath("$[?(@.id==-1)].stockCoverForward").exists())

            .andExpect(jsonPath("$[?(@.id==3)].setQuantity").value(8))
            .andExpect(jsonPath("$[?(@.id==3)].correctionReason.id").value(3))
            .andExpect(jsonPath("$[?(@.id==4)].setQuantity").value(10))
            .andExpect(jsonPath("$[?(@.id==4)].correctionReason.id").value(3))
            .andExpect(jsonPath("$[?(@.id==5)].setQuantity").value(12))
            .andExpect(jsonPath("$[?(@.id==5)].correctionReason.id").value(3))
            .andExpect(jsonPath("$[?(@.id==-3)].stockCoverBackward").exists())
            .andExpect(jsonPath("$[?(@.id==-3)].stockCoverForward").exists());
        parentDemand3pUpdateLoader.load();
    }


    @Test
    @DbUnitDataSet(before = "RecommendationControllerTest.3p_adjRecommendations.before.csv",
        after = "RecommendationControllerTest.3p_adjNeedsReviewRecommendation.after.csv")
    public void testEbpAdjustReplenishments3p_NeedsManualReviewWithoutUserFilter() throws Exception {
        String url =
            "/api/v2/recommendations/needsManualReview?demandType=TYPE_3P";

        NeedsManualReviewRequest request = new NeedsManualReviewRequest();
        request.setDemandKeys(
            Arrays.asList(
                new DemandIdentityDTO(1L, 1),
                new DemandIdentityDTO(2L, 1),
                new DemandIdentityDTO(3L, 1)
            )
        );

        var filter = new RecommendationFilter();
        filter.setDemandIds(Arrays.asList(1L, 2L, 3L));
        filter.setNeedsManualReview(null);
        filter.setFilter(FilterType.NEW);

        var filters = new RecommendationFilters();
        filters.setFilter(filter);
        filters.setUserFilter(null);
        request.setRecommendationFilters(filters);

        mockMvc.perform(post(url).contentType(APPLICATION_JSON_UTF8)
                .content(new ObjectMapper().writeValueAsString(request)))
            .andExpect(status().isOk());
    }

    @Test
    @DbUnitDataSet(before = "RecommendationControllerTest.ebpAdjRecommendationWithUserFilter.before.csv",
        after = "RecommendationControllerTest.ebpAdjRecommendationWithUserFilter_needsManualReview.after.csv")
    public void testEbpAdjustReplenishmentsWithUserFilter_needsManualReview() throws Exception {

        NeedsManualReviewRequest request = new NeedsManualReviewRequest();

        final RecommendationFilters recommendationFilters = createFilters(null, 1L);
        final RecommendationUserFilter userFilter = new RecommendationUserFilter();
        userFilter.setField(MSKU);
        userFilter.setPredicate(UserFilterFieldPredicate.EQUAL);
        userFilter.setValue("100");
        recommendationFilters.setUserFilter(Collections.singletonList(userFilter));

        request.setDemandKeys(Collections.singletonList(new DemandIdentityDTO(1L, 1)));
        request.setNeedsManualReview(false);
        request.setRecommendationFilters(recommendationFilters);

        mockMvc.perform(post("/api/v2/recommendations/needsManualReview")
                .contentType(APPLICATION_JSON_UTF8)
                .content(dtoToString(request)))
            .andExpect(status().isOk());
    }

    @Test
    @DbUnitDataSet(before = "RecommendationControllerTest.ebpAdjRecommendationWithUserFilter.before.csv",
        after = "RecommendationControllerTest.ebpAdjRecommendationWithUserFilter_transit.after.csv")
    public void testEbpAdjustReplenishmentsWithUserFilter_transit() throws Exception {
        TransitRequest request = new TransitRequest();

        final RecommendationFilters recommendationFilters = createFilters(null, 1L);
        final RecommendationUserFilter userFilter = new RecommendationUserFilter();
        userFilter.setField(MSKU);
        userFilter.setPredicate(UserFilterFieldPredicate.EQUAL);
        userFilter.setValue("100");
        recommendationFilters.setUserFilter(Collections.singletonList(userFilter));

        request.setDemandKeys(Collections.singletonList(new DemandIdentityDTO(1L, 1)));
        request.setTransit(0);
        request.setRecommendationFilters(recommendationFilters);

        mockMvc.perform(post("/api/v2/recommendations/transit")
                .contentType(APPLICATION_JSON_UTF8)
                .content(dtoToString(request)))
            .andExpect(status().isOk());
    }

    @Test
    @DbUnitDataSet(before = "RecommendationControllerTest.3p_adjRecommendations.before.csv")
    public void testEbpAdjust3pReplenishmentsWithUserFilter_transit() throws Exception {
        TransitRequest request = new TransitRequest();

        final RecommendationFilters recommendationFilters = createFilters(null, 1L, 2L, 3L);
        final RecommendationUserFilter userFilter = new RecommendationUserFilter();
        userFilter.setField(MSKU);
        userFilter.setPredicate(UserFilterFieldPredicate.EQUAL);
        userFilter.setValue("100");
        recommendationFilters.setUserFilter(Collections.singletonList(userFilter));

        request.setDemandKeys(
            Arrays.asList(
                new DemandIdentityDTO(1L, 1),
                new DemandIdentityDTO(2L, 1),
                new DemandIdentityDTO(3L, 1)
            )
        );
        request.setTransit(123);
        request.setRecommendationFilters(recommendationFilters);

        mockMvc.perform(post("/api/v2/recommendations/transit?demandType=TYPE_3P")
                .contentType(APPLICATION_JSON_UTF8)
                .content(dtoToString(request)))
            .andExpect(status().isIAmATeapot())
                .andExpect(jsonPath("$.message")
                        .value("Редактирование транзитов не поддерживается, только обнуление."));
    }

    @Test
    @DbUnitDataSet(before = "RecommendationControllerTest.ebpAdjRecommendationWithUserFilter.before.csv",
        after = "RecommendationControllerTest.ebpAdjRecommendationWithUserFilter_quantity.after.csv")
    public void testEbpAdjustReplenishmentsWithUserFilter_quantity() throws Exception {

        AdjustmentDTO request = new AdjustmentDTO();

        final RecommendationFilters recommendationFilters = createFilters(null, 1L);
        final RecommendationUserFilter userFilter = new RecommendationUserFilter();
        userFilter.setField(MSKU);
        userFilter.setPredicate(UserFilterFieldPredicate.EQUAL);
        userFilter.setValue("100");
        recommendationFilters.setUserFilter(Collections.singletonList(userFilter));

        request.setDemandKeys(Collections.singletonList(new DemandIdentityDTO(1L, 1)));
        request.setQuantums(123L);
        request.setCorrectionReason(3L);
        request.setRecommendationFilters(recommendationFilters);

        mockMvc.perform(post("/api/v2/recommendations/quantity")
                .contentType(APPLICATION_JSON_UTF8)
                .content(dtoToString(request)))
            .andExpect(status().isOk());
    }

    @Test
    @DbUnitDataSet(
        before = "RecommendationControllerTest.ebpAdjRecommendationWithUserFilterQuantityBackward.before.csv",
        after = "RecommendationControllerTest.ebpAdjRecommendationWithUserFilter_quantityForward.after.csv")
    public void testEbpAdjustReplenishmentsWithUserFilter_quantityForward() throws Exception {
        salesRepository.refreshMaterializedViews();
        AdjustmentDTO request = new AdjustmentDTO();

        final RecommendationFilters recommendationFilters = createFilters(null, 1L);
        final RecommendationUserFilter userFilter = new RecommendationUserFilter();
        userFilter.setField(MSKU);
        userFilter.setPredicate(UserFilterFieldPredicate.EQUAL);
        userFilter.setValue("100");
        recommendationFilters.setUserFilter(Collections.singletonList(userFilter));

        request.setDemandKeys(Collections.singletonList(new DemandIdentityDTO(1L, 1)));
        request.setCorrectionReason(3L);
        request.setRecommendationFilters(recommendationFilters);
        request.setMakeStockCoverEqualsWeeks(7L);
        request.setStockCoverType(StockCoverType.FORWARD);

        mockMvc.perform(post("/api/v2/recommendations/quantity")
                .contentType(APPLICATION_JSON_UTF8)
                .content(dtoToString(request)))
            .andExpect(status().isOk());
    }

    @Test
    @DbUnitDataSet(
        before = "RecommendationControllerTest.ebpAdjRecommendationWithUserFilterQuantityBackward.before.csv",
        after = "RecommendationControllerTest.ebpAdjRecommendationWithUserFilter_quantityBackward.after.csv")
    public void testEbpAdjustReplenishmentsWithUserFilter_quantityBackward() throws Exception {
        salesRepository.refreshMaterializedViews();
        AdjustmentDTO request = new AdjustmentDTO();

        final RecommendationFilters recommendationFilters = createFilters(null, 1L);
        final RecommendationUserFilter userFilter = new RecommendationUserFilter();
        userFilter.setField(MSKU);
        userFilter.setPredicate(UserFilterFieldPredicate.EQUAL);
        userFilter.setValue("100");
        recommendationFilters.setUserFilter(Collections.singletonList(userFilter));

        request.setDemandKeys(Collections.singletonList(new DemandIdentityDTO(1L, 1)));
        request.setCorrectionReason(3L);
        request.setRecommendationFilters(recommendationFilters);
        request.setMakeStockCoverEqualsWeeks(7L);
        request.setStockCoverType(StockCoverType.BACKWARD);

        mockMvc.perform(post("/api/v2/recommendations/quantity")
                .contentType(APPLICATION_JSON_UTF8)
                .content(dtoToString(request)))
            .andExpect(status().isOk());
    }

    @Test
    @DbUnitDataSet(before = "RecommendationControllerTest.increasingMonoXdocHonestSign.before.csv")
    public void testIncreasingMonoXdocHonestSign() throws Exception {

        AdjustmentDTO request = new AdjustmentDTO();

        final RecommendationFilters recommendationFilters = createFilters(null, 1L);
        final RecommendationUserFilter userFilter = new RecommendationUserFilter();
        userFilter.setField(MSKU);
        userFilter.setPredicate(UserFilterFieldPredicate.EQUAL);
        userFilter.setValue("100");
        recommendationFilters.setUserFilter(Collections.singletonList(userFilter));

        request.setDemandKeys(Collections.singletonList(new DemandIdentityDTO(1L, 1)));
        request.setQuantums(123L);
        request.setCorrectionReason(3L);
        request.setRecommendationFilters(recommendationFilters);

        mockMvc.perform(post("/api/v2/recommendations/quantity")
                .contentType(APPLICATION_JSON_UTF8)
                .content(dtoToString(request)))
            .andExpect(status().isIAmATeapot())
            .andExpect(jsonPath("$.message").value(
                "Нельзя увеличивать значение для Mono-Xdock рекомендации с признаком Честный знак"));
    }

    @Test
    @DbUnitDataSet(before = "RecommendationControllerTest.ebpAdjTenderRecommendation.before.csv",
        after = "RecommendationControllerTest.ebpAdjTenderRecommendation_quantity.after.csv")
    public void testEbpAdjustTenderReplenishments_quantity() throws Exception {

        AdjustmentDTO request = new AdjustmentDTO();

        final RecommendationFilters recommendationFilters = createFilters(null, 1L);
        final RecommendationUserFilter userFilter = new RecommendationUserFilter();
        userFilter.setField(MSKU);
        userFilter.setPredicate(UserFilterFieldPredicate.EQUAL);
        userFilter.setValue("100");
        recommendationFilters.setUserFilter(Collections.singletonList(userFilter));

        request.setDemandKeys(Collections.singletonList(new DemandIdentityDTO(1L, 1)));
        request.setQuantums(123L);
        request.setCorrectionReason(3L);
        request.setRecommendationFilters(recommendationFilters);

        mockMvc.perform(post("/api/v2/recommendations/quantity?demandType=TENDER")
                .contentType(APPLICATION_JSON_UTF8)
                .content(dtoToString(request)))
            .andExpect(status().isOk());
    }

    @Test
    @DbUnitDataSet(before = "RecommendationControllerTest.3p_adjRecommendations.before.csv",
        after = "RecommendationControllerTest.3p_ebpAdjRecommendationWithUserFilter_quantity.after.csv")
    public void testEbpAdjust3pReplenishmentsWithUserFilter_quantity() throws Exception {

        AdjustmentDTO request = new AdjustmentDTO();

        final RecommendationFilters recommendationFilters = createFilters(null, 1L, 2L, 3L);
        final RecommendationUserFilter userFilter = new RecommendationUserFilter();
        userFilter.setField(MSKU);
        userFilter.setPredicate(UserFilterFieldPredicate.EQUAL);
        userFilter.setValue("100");
        recommendationFilters.setUserFilter(Collections.singletonList(userFilter));

        request.setDemandKeys(
            Arrays.asList(
                new DemandIdentityDTO(1L, 1),
                new DemandIdentityDTO(2L, 1),
                new DemandIdentityDTO(3L, 1)
            )
        );
        request.setQuantums(150L);
        request.setCorrectionReason(3L);
        request.setRecommendationFilters(recommendationFilters);

        mockMvc.perform(post("/api/v2/recommendations/quantity?demandType=TYPE_3P")
                .contentType(APPLICATION_JSON_UTF8)
                .content(dtoToString(request)))
            .andExpect(status().isOk());
        parentDemand3pUpdateLoader.load();
    }

    @Test
    @DbUnitDataSet(before = "RecommendationControllerTest.3p_adjRecommendations.before.csv",
        after = "RecommendationControllerTest.3p_ebpAdjRecommendationWithUserFilter_stockCover.after.csv")
    public void testEbpAdjust3pReplenishmentsWithUserFilter_stockCover() throws Exception {
        AdjustmentDTO request = new AdjustmentDTO();

        final RecommendationFilters recommendationFilters = createFilters(null, 1L, 2L, 3L);
        final RecommendationUserFilter userFilter = new RecommendationUserFilter();
        userFilter.setField(MSKU);
        userFilter.setPredicate(UserFilterFieldPredicate.EQUAL);
        userFilter.setValue("100");
        recommendationFilters.setUserFilter(Collections.singletonList(userFilter));

        request.setDemandKeys(
            Arrays.asList(
                new DemandIdentityDTO(1L, 1),
                new DemandIdentityDTO(2L, 1),
                new DemandIdentityDTO(3L, 1)
            )
        );
        request.setMakeStockCoverEqualsWeeks(5L);
        request.setCorrectionReason(3L);
        request.setRecommendationFilters(recommendationFilters);

        mockMvc.perform(post("/api/v2/recommendations/quantity?demandType=TYPE_3P")
                .contentType(APPLICATION_JSON_UTF8)
                .content(dtoToString(request)))
            .andExpect(status().isOk());
        parentDemand3pUpdateLoader.load();
    }

    @Test
    @DbUnitDataSet(before = "RecommendationControllerTest.3p_adjRecommendations_multiMskus.before.csv",
        after = "RecommendationControllerTest.3p_ebpAdjRecommendation_stockCover_multiMskus.after.csv")
    public void testEbpAdjust3pReplenishments_stockCover_forMultiMskus() throws Exception {
        final RecommendationFilters recommendationFilters = createFilters(null, 1L);
        recommendationFilters.getFilter().setMskus(Arrays.asList(100L, 200L));

        final AdjustmentDTO request = new AdjustmentDTO();
        request.setDemandKeys(Collections.singletonList(new DemandIdentityDTO(1L, 1)));
        request.setMakeStockCoverEqualsWeeks(3L);
        request.setCorrectionReason(3L);
        request.setRecommendationFilters(recommendationFilters);

        mockMvc.perform(post("/api/v2/recommendations/quantity?demandType=TYPE_3P")
                .contentType(APPLICATION_JSON_UTF8)
                .content(dtoToString(request)))
            .andExpect(status().isOk());
        parentDemand3pUpdateLoader.load();
    }

    @Test
    @DbUnitDataSet(before = "RecommendationControllerTest.3p_adjRecommendations_multiMskus.before.csv",
        after = "RecommendationControllerTest.3p_ebpAdjRecommendation_quantums_multiMskus.after.csv")
    public void testEbpAdjust3pReplenishments_quantums_forMultiMskus() throws Exception {
        final RecommendationFilters recommendationFilters = createFilters(null, 1L);
        recommendationFilters.getFilter().setMskus(Arrays.asList(100L, 200L));

        final AdjustmentDTO request = new AdjustmentDTO();
        request.setDemandKeys(Collections.singletonList(new DemandIdentityDTO(1L, 1)));
        request.setQuantums(2L);
        request.setCorrectionReason(3L);
        request.setRecommendationFilters(recommendationFilters);

        mockMvc.perform(post("/api/v2/recommendations/quantity?demandType=TYPE_3P")
                .contentType(APPLICATION_JSON_UTF8)
                .content(dtoToString(request)))
            .andExpect(status().isOk());
        parentDemand3pUpdateLoader.load();
    }

    @Test
    @DbUnitDataSet(before = "RecommendationControllerTest.3p_adjRecommendations_multiMskus.before.csv",
        after = "RecommendationControllerTest.3p_ebpAdjRecommendation_needsManualReview_multiMskus.after.csv")
    public void testEbpAdjust3pReplenishments_needsManualReview_forMultiMskus() throws Exception {
        final RecommendationFilters recommendationFilters = createFilters(null, 1L);
        recommendationFilters.getFilter().setMskus(Arrays.asList(100L, 200L));

        final NeedsManualReviewRequest request = new NeedsManualReviewRequest();
        request.setDemandKeys(Collections.singletonList(new DemandIdentityDTO(1L, 1)));
        request.setNeedsManualReview(false);
        request.setRecommendationFilters(recommendationFilters);

        mockMvc.perform(post("/api/v2/recommendations/needsManualReview?demandType=TYPE_3P")
                .contentType(APPLICATION_JSON_UTF8)
                .content(dtoToString(request)))
            .andExpect(status().isOk());
    }

    @Test
    @DbUnitDataSet(before = "RecommendationControllerTest.3p_adjRecommendations_multiMskus.before.csv",
        after = "RecommendationControllerTest.3p_ebpAdjRecommendation_transits_multiMskus.after.csv")
    public void testEbpAdjust3pReplenishments_transits_forMultiMskus() throws Exception {
        final RecommendationFilters recommendationFilters = createFilters(null, 1L);
        recommendationFilters.getFilter().setMskus(Arrays.asList(100L, 200L));

        final TransitRequest request = new TransitRequest();
        request.setDemandKeys(Collections.singletonList(new DemandIdentityDTO(1L, 1)));
        request.setTransit(0);
        request.setRecommendationFilters(recommendationFilters);

        mockMvc.perform(post("/api/v2/recommendations/transit?demandType=TYPE_3P")
                .contentType(APPLICATION_JSON_UTF8)
                .content(dtoToString(request)))
            .andExpect(status().isOk());
    }

    @Test
    @DbUnitDataSet(before = "RecommendationControllerTest.3p_adjRecommendations_multiMskus_denormalize.before.csv",
        after = "RecommendationControllerTest.3p_ebpAdjRecommendation_transits_multiMskus_denormalize.after.csv")
    public void testEbpAdjust3pReplenishments_transits_forMultiMskus_denormalize() throws Exception {
        final RecommendationFilters recommendationFilters = createFilters(null, 1L);
        recommendationFilters.getFilter().setMskus(Arrays.asList(100L, 200L));

        final TransitRequest request = new TransitRequest();
        request.setDemandKeys(Collections.singletonList(new DemandIdentityDTO(1L, 1)));
        request.setTransit(0);
        request.setRecommendationFilters(recommendationFilters);

        mockMvc.perform(post("/api/v2/recommendations/transit?demandType=TYPE_3P")
                .contentType(APPLICATION_JSON_UTF8)
                .content(dtoToString(request)))
            .andExpect(status().isOk());
    }

    @NotNull
    private RecommendationFilters createFilters(FilterType filter, Long... demandIds) {
        final RecommendationFilters recommendationFilters = new RecommendationFilters();
        final RecommendationFilter recommendationFilter = new RecommendationFilter();
        if (filter != null) {
            recommendationFilter.setFilter(filter);
        }
        recommendationFilter.setDemandIds(Arrays.asList(demandIds));
        recommendationFilters.setFilter(recommendationFilter);
        return recommendationFilters;
    }

    @Test
    @DbUnitDataSet(before = "RecommendationControllerTest_testGetRecommendationsWithCount2.before.csv")
    public void testGetRecommendationsWithCount_withReplenishmentResults2() throws Exception {
        createReplenishmentResult();

        final RecommendationFilters recommendationFilters = createFilters(null, 1L);
        mockMvc.perform(post("/api/v2/recommendations/with-count")
                .contentType(APPLICATION_JSON_UTF8)
                .content(dtoToString(recommendationFilters))
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.recommendations.length()").value(2))

            .andExpect(jsonPath("$.recommendations[0].msku").value(100L))
            .andExpect(jsonPath("$.recommendations[0].title").value("msku_1"))
            .andExpect(jsonPath("$.recommendations[0].packageNumInSpike").value(16))
            .andExpect(jsonPath("$.recommendations[0].groupId").value(0L))
            .andExpect(jsonPath("$.recommendations[0].salePromoStart").value("2021-01-01"))
            .andExpect(jsonPath("$.recommendations[0].salePromoEnd").value("2021-02-01"))
            .andExpect(jsonPath("$.recommendations[0].stockCoverBackward").value(161))
            .andExpect(jsonPath("$.recommendations[0].stockCoverBackward1p").value(101))
            .andExpect(jsonPath("$.recommendations[0].stockCoverBackward3p").value(366))
            .andExpect(jsonPath("$.recommendations[0].stockCoverForward").value(116))
            .andExpect(jsonPath("$.recommendations[0].stockCoverForward1p").value(87))
            .andExpect(jsonPath("$.recommendations[0].stockCoverForward3p").value(224))
            .andExpect(jsonPath("$.recommendations[0].supplierLifetime").value(1))
            .andExpect(jsonPath("$.recommendations[0].inLifetime").value(10))
            .andExpect(jsonPath("$.recommendations[0].outLifetime").value(100))
            .andExpect(jsonPath("$.recommendations[0].reasonOfRecommendation").value("comment100"))
            .andExpect(jsonPath("$.recommendations[0].honestSign").value(HonestSign.REQUIRED.name()))
            .andExpect(commonMatcher(0))

            .andExpect(jsonPath("$.recommendations[1].msku").value(200L))
            .andExpect(jsonPath("$.recommendations[1].title").value("msku_2"))
            .andExpect(jsonPath("$.recommendations[1].packageNumInSpike").value(999))
            .andExpect(jsonPath("$.recommendations[1].groupId").value(0L))
            .andExpect(jsonPath("$.recommendations[1].salePromoStart").value("2021-01-02"))
            .andExpect(jsonPath("$.recommendations[1].salePromoEnd").value("2021-02-02"))
            .andExpect(jsonPath("$.recommendations[1].supplierLifetime").value(2))
            .andExpect(jsonPath("$.recommendations[1].inLifetime").value(20))
            .andExpect(jsonPath("$.recommendations[1].outLifetime").value(200))
            .andExpect(jsonPath("$.recommendations[1].reasonOfRecommendation").value("comment200"))
            .andExpect(jsonPath("$.recommendations[1].honestSign").value(HonestSign.REQUIRED.name()))

            .andExpect(commonMatcher(1));
    }

    @Test
    @DbUnitDataSet(before = "RecommendationControllerTest_testGetRecommendationsWithCountDenorm.before.csv")
    public void testGetRecommendationsWithCount_withReplenishmentResultsDenorm() throws Exception {
        createReplenishmentResult();

        final RecommendationFilters recommendationFilters = createFilters(null, 1L);
        mockMvc.perform(post("/api/v2/recommendations/with-count")
                .contentType(APPLICATION_JSON_UTF8)
                .content(dtoToString(recommendationFilters))
            )
            .andExpect(status().isOk());
    }

    @Test
    @DbUnitDataSet(before = "RecommendationControllerTest_testGetRecommendationsWithCount2.before.csv")
    public void testGetRecommendationsWithCount_withResultsReadStoredSCs() throws Exception {
        createReplenishmentResult();
        final RecommendationFilters recommendationFilters = createFilters(null, 1L);
        mockMvc.perform(post("/api/v2/recommendations/with-count")
                .contentType(APPLICATION_JSON_UTF8)
                .content(dtoToString(recommendationFilters))
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.recommendations[0].stockCoverBackward").value(161))
            .andExpect(jsonPath("$.recommendations[0].stockCoverBackward1p").value(101))
            .andExpect(jsonPath("$.recommendations[0].stockCoverBackward3p").value(366))
            .andExpect(jsonPath("$.recommendations[0].stockCoverForward").value(116))
            .andExpect(jsonPath("$.recommendations[0].stockCoverForward1p").value(87))
            .andExpect(jsonPath("$.recommendations[0].stockCoverForward3p").value(224));

        TestUtils.mockTimeService(timeService, LocalDateTime.of(2020, 7, 2, 0, 0, 0));

        mockMvc.perform(post("/api/v2/recommendations/with-count")
                .contentType(APPLICATION_JSON_UTF8)
                .content(dtoToString(recommendationFilters))
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.recommendations[0].stockCoverBackward").value(161))
            .andExpect(jsonPath("$.recommendations[0].stockCoverBackward1p").value(101))
            .andExpect(jsonPath("$.recommendations[0].stockCoverBackward3p").value(366))
            .andExpect(jsonPath("$.recommendations[0].stockCoverForward").value(116))
            .andExpect(jsonPath("$.recommendations[0].stockCoverForward1p").value(87))
            .andExpect(jsonPath("$.recommendations[0].stockCoverForward3p").value(224));
    }

    @Test
    @DbUnitDataSet(before = "RecommendationControllerTest_testGetRecommendationsWithCount2.before.csv")
    public void testGetRecommendationsWithCount_readSCForNotExportedRecommendations() throws Exception {
        final RecommendationFilters recommendationFilters = createFilters(null, 1L);
        mockMvc.perform(post("/api/v2/recommendations/with-count")
                .contentType(APPLICATION_JSON_UTF8)
                .content(dtoToString(recommendationFilters))
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.recommendations[0].stockCoverBackward").value(166))
            .andExpect(jsonPath("$.recommendations[0].stockCoverBackward1p").value(122))
            .andExpect(jsonPath("$.recommendations[0].stockCoverBackward3p").value(366))
            .andExpect(jsonPath("$.recommendations[0].stockCoverForward").value(119))
            .andExpect(jsonPath("$.recommendations[0].stockCoverForward1p").value(106))
            .andExpect(jsonPath("$.recommendations[0].stockCoverForward3p").value(224));
    }

    @Test
    @DbUnitDataSet(before = "RecommendationControllerTest_testGetRecommendationsWithCount2.before.csv")
    public void testGetRecommendationsWithCount_withResultsReadStoredSCs_withFilter() throws Exception {
        createReplenishmentResult();

        var recommendationFilters = RecommendationFilters.getEmpty();
        recommendationFilters.setUserFilter(List.of(
            new RecommendationUserFilter(SCB, UserFilterFieldPredicate.EQUAL, "161"),
            new RecommendationUserFilter(SCF, UserFilterFieldPredicate.EQUAL, "116")
        ));
        recommendationFilters.getFilter().setDemandIds(List.of(1L));

        mockMvc.perform(post("/api/v2/recommendations/with-count")
                .contentType(APPLICATION_JSON_UTF8)
                .content(dtoToString(recommendationFilters))
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.recommendations.length()").value(2));

        TestUtils.mockTimeService(timeService, LocalDateTime.of(2020, 7, 2, 0, 0, 0));

        mockMvc.perform(post("/api/v2/recommendations/with-count")
                .contentType(APPLICATION_JSON_UTF8)
                .content(dtoToString(recommendationFilters))
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.recommendations.length()").value(2));
    }

    @Test
    @DbUnitDataSet(before = "RecommendationControllerTest_testGetRecommendationsWithCountSC.before.csv")
    public void testGetRecommendationsWithCount_withScFilterAllWeeks() throws Exception {
        createReplenishmentResult();
        TestUtils.mockTimeService(timeService, LocalDateTime.of(2020, 7, 2, 0, 0, 0));

        final RecommendationFilters recommendationFilters = createFilters(FilterType.SC, 1L);
        recommendationFilters.getFilter().setWeeks(WeeksType.ALL);
        mockMvc.perform(post("/api/v2/recommendations/with-count")
                .contentType(APPLICATION_JSON_UTF8)
                .content(dtoToString(recommendationFilters))
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.recommendations.length()").value(4))

            .andExpect(jsonPath("$.recommendations[0].msku").value(100L))
            .andExpect(jsonPath("$.recommendations[0].filter").value("SC"))
            .andExpect(jsonPath("$.recommendations[0].filterWeeks").value("ZERO_TWO"))

            .andExpect(jsonPath("$.recommendations[1].msku").value(200L))
            .andExpect(jsonPath("$.recommendations[1].filter").value("SC"))
            .andExpect(jsonPath("$.recommendations[1].filterWeeks").value("TWO_THREE"))

            .andExpect(jsonPath("$.recommendations[2].msku").value(300L))
            .andExpect(jsonPath("$.recommendations[2].filter").value("SC"))
            .andExpect(jsonPath("$.recommendations[2].filterWeeks").value("THREE_FOUR"))

            .andExpect(jsonPath("$.recommendations[3].msku").value(400L))
            .andExpect(jsonPath("$.recommendations[3].filter").value("SC"))
            .andExpect(jsonPath("$.recommendations[3].filterWeeks").value("FOUR_EIGHT"))

            .andExpect(jsonPath("$.count.filter[7].weeks[5].count").value(4))
            .andExpect(jsonPath("$.count.filter[7].weeks[5].id").value("ALL"));
    }

    @Test
    @DbUnitDataSet(before = "RecommendationControllerTest_testGetRecommendationsWithCountSC.before.csv")
    public void testGetRecommendationsWithCount_withErrorScFilterWeeksNull() throws Exception {
        TestUtils.mockTimeService(timeService, LocalDateTime.of(2020, 7, 2, 0, 0, 0));

        final RecommendationFilters recommendationFilters = createFilters(FilterType.SC, 1L);

        mockMvc.perform(post("/api/v2/recommendations/with-count")
                .contentType(APPLICATION_JSON_UTF8)
                .content(dtoToString(recommendationFilters))
            )
            .andExpect(status().isIAmATeapot())
            .andExpect(jsonPath("$.message").value(
                "Invalid recommendation filter (set weeks parameter)"));
    }

    @Test
    @DbUnitDataSet(before = "RecommendationControllerTest_testGetRecommendationsWithCountSC.before.csv")
    public void testGetRecommendationsWithCount_withErrorSalesZeroFilterWeeksWrong() throws Exception {
        TestUtils.mockTimeService(timeService, LocalDateTime.of(2020, 7, 2, 0, 0, 0));

        final RecommendationFilters recommendationFilters = createFilters(FilterType.SALES_ZERO, 1L);
        recommendationFilters.getFilter().setWeeks(WeeksType.ALL);
        mockMvc.perform(post("/api/v2/recommendations/with-count")
                .contentType(APPLICATION_JSON_UTF8)
                .content(dtoToString(recommendationFilters))
            )
            .andExpect(status().isIAmATeapot())
            .andExpect(jsonPath("$.message").value(
                "Invalid recommendation filter (use ALL only for SC)"));

        recommendationFilters.getFilter().setWeeks(WeeksType.EIGHT_INF);
        mockMvc.perform(post("/api/v2/recommendations/with-count")
                .contentType(APPLICATION_JSON_UTF8)
                .content(dtoToString(recommendationFilters))
            )
            .andExpect(status().isIAmATeapot())
            .andExpect(jsonPath("$.message").value(
                "Invalid recommendation filter (use EIGHT_INF only for SC)"));
    }

    @Test
    @DbUnitDataSet(before = "RecommendationControllerTest_testGetRecommendationsWithCount_SpecialTransit.before.csv")
    public void testGetRecommendationsWithCount_SpecialRecsTransit() throws Exception {

        final RecommendationFilters recommendationFilters = createFilters(null, 1L);
        mockMvc.perform(post("/api/v2/recommendations/with-count")
                .contentType(APPLICATION_JSON_UTF8)
                .content(dtoToString(recommendationFilters))
            )
            .andExpect(status().isOk())

            .andExpect(jsonPath("$.recommendations.length()").value(4))

            .andExpect(jsonPath("$.recommendations[0].msku").value(200L))
            .andExpect(jsonPath("$.recommendations[0].demandId").value(1L))
            .andExpect(jsonPath("$.recommendations[0].warehouseId").value(171))
            .andExpect(jsonPath("$.recommendations[0].transit").value(2))
            .andExpect(jsonPath("$.recommendations[0].transit1p").value(2))

            .andExpect(jsonPath("$.recommendations[1].msku").value(300L))
            .andExpect(jsonPath("$.recommendations[1].demandId").value(1L))
            .andExpect(jsonPath("$.recommendations[1].warehouseId").value(171))
            .andExpect(jsonPath("$.recommendations[1].transit").value(6))
            .andExpect(jsonPath("$.recommendations[1].transit1p").value(3))

            .andExpect(jsonPath("$.recommendations[2].msku").value(400L))
            .andExpect(jsonPath("$.recommendations[2].demandId").value(1L))
            .andExpect(jsonPath("$.recommendations[2].warehouseId").value(171))
            .andExpect(jsonPath("$.recommendations[2].transit").value(4))
            .andExpect(jsonPath("$.recommendations[2].transit1p").value(0))

            .andExpect(jsonPath("$.recommendations[3].msku").value(500L))
            .andExpect(jsonPath("$.recommendations[3].demandId").value(1L))
            .andExpect(jsonPath("$.recommendations[3].warehouseId").value(171))
            .andExpect(jsonPath("$.recommendations[3].transit").value(10))
            .andExpect(jsonPath("$.recommendations[3].transit1p").value(10));
    }

    @Test
    @DbUnitDataSet(before = "RecommendationControllerTest_testGetRecommendationsWithCount2.before.csv")
    public void testGetRecommendationsWithCountAndFilters_withReplenishmentResults2() throws Exception {
        createReplenishmentResult();

        final RecommendationFilters recommendationFilters = createFilters(null, 1L);
        final RecommendationUserFilter userFilter = new RecommendationUserFilter();
        userFilter.setField(MSKU);
        userFilter.setPredicate(UserFilterFieldPredicate.EQUAL);
        userFilter.setValue("100");
        recommendationFilters.setUserFilter(Collections.singletonList(userFilter));
        mockMvc.perform(post("/api/v2/recommendations/with-count")
                .contentType(APPLICATION_JSON_UTF8)
                .content(dtoToString(recommendationFilters))
            )
            .andExpect(status().isOk())

            .andExpect(jsonPath("$.count.filter[0].id").value("ALL"))
            .andExpect(jsonPath("$.count.filter[0].count").value(2))

            .andExpect(jsonPath("$.userFiltersCount.length()").value(1))
            .andExpect(jsonPath("$.userFiltersCount[0]").value(1))

            .andExpect(jsonPath("$.numberOfCorrectedRecommendations").value(2))

            .andExpect(jsonPath("$.recommendations.length()").value(1))

            .andExpect(jsonPath("$.recommendations[0].msku").value(100L))
            .andExpect(jsonPath("$.recommendations[0].title").value("msku_1"))
            .andExpect(jsonPath("$.recommendations[0].packageNumInSpike").value(16))
            .andExpect(jsonPath("$.recommendations[0].groupId").value(0L))
            .andExpect(jsonPath("$.recommendations[0].salePromoStart").value("2021-01-01"))
            .andExpect(jsonPath("$.recommendations[0].salePromoEnd").value("2021-02-01"))
            .andExpect(jsonPath("$.recommendations[0].supplierLifetime").value(1))
            .andExpect(jsonPath("$.recommendations[0].inLifetime").value(10))
            .andExpect(jsonPath("$.recommendations[0].outLifetime").value(100))

            .andExpect(commonMatcher(0));
    }

    @Test
    @DbUnitDataSet(before = "RecommendationControllerTest_testGetRecommendationsWithCount2.before.csv")
    public void testGetRecommendationsWithCountAndFilters_withPromoPurchase() throws Exception {
        final RecommendationUserFilter userFilter = new RecommendationUserFilter();
        userFilter.setField(PROMO_PURCHASE);
        userFilter.setPredicate(TRUE);
        userFilter.setValue("");

        final RecommendationFilters recommendationFilters = createFilters(null, 1L, 15L);
        recommendationFilters.setUserFilter(Collections.singletonList(userFilter));

        mockMvc.perform(post("/api/v2/recommendations/with-count")
                .contentType(APPLICATION_JSON_UTF8)
                .content(dtoToString(recommendationFilters))
            )
            .andDo(result -> System.out.println(result.getResponse().getContentAsString()))
            .andExpect(status().isOk())

            .andExpect(jsonPath("$.recommendations", hasSize(2)))
            .andExpect(jsonPath("$.recommendations[0].msku").value(100L))
            .andExpect(jsonPath("$.recommendations[0].purchasePromoStart").value("2020-07-01"))
            .andExpect(jsonPath("$.recommendations[0].purchasePromoEnd").value("2020-07-30"))
            .andExpect(jsonPath("$.recommendations[1].msku").value(200L))
            .andExpect(jsonPath("$.recommendations[1].purchasePromoStart").value("2020-07-01"))
            .andExpect(jsonPath("$.recommendations[1].purchasePromoEnd").value("2020-07-30"))
            .andExpect(commonMatcher(0));
    }

    @Test
    @DbUnitDataSet(before = "RecommendationControllerTest_testGetRecommendationsWithCount2.before.csv")
    public void testWithUserFilters400() throws Exception {
        final RecommendationFilters recommendationFilters = createFilters(null, 1L);
        recommendationFilters.setUserFilter(Collections.singletonList(new RecommendationUserFilter(null, null, null)));
        mockMvc.perform(post("/api/v2/recommendations/with-count")
                .contentType(APPLICATION_JSON_UTF8)
                .content(dtoToString(recommendationFilters))
            )
            .andExpect(status().is(400));
    }

    @Test
    @DbUnitDataSet(before = "RecommendationControllerTest_testGetRecommendationsWithCount2.before.csv")
    public void testWithUserFilters400_2() throws Exception {
        final RecommendationFilters recommendationFilters = createFilters(null, 1L);
        recommendationFilters.setUserFilter(Collections.singletonList(
            new RecommendationUserFilter(MSKU, CONTAINS, "123"))
        );
        mockMvc.perform(post("/api/v2/recommendations/with-count")
                .contentType(APPLICATION_JSON_UTF8)
                .content(dtoToString(recommendationFilters))
            )
            .andExpect(status().isIAmATeapot())
            .andExpect(jsonPath("$.message").value("wrong predicate for LONG type: CONTAINS"));
    }

    @Test
    @DbUnitDataSet(before = "RecommendationControllerTest.adjReplenishments.before.csv")
    public void testAdjustReplenishments_WrongDemand() throws Exception {
        String url = "/api/v1/recommendations/adjust";

        AdjustedRecommendationDTO adjusted = new AdjustedRecommendationDTO();
        adjusted.setAdjustedPurchQty(50);
        adjusted.setGroupId(0L);
        adjusted.setId(1L);
        adjusted.setMsku(123L);
        AdjustedRecommendationsDTO adjustments = new AdjustedRecommendationsDTO(Collections.singletonList(adjusted));
        adjustments.setDemandId(123L);
        adjustments.setDemandVersion(1);

        mockMvc.perform(put(url).contentType(APPLICATION_JSON_UTF8)
                .content(new ObjectMapper().writeValueAsString(adjustments)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value(
                "Потребности с типом 1P и c id 123 нет в базе, вероятно произошел реимпорт рекомендаций " +
                    "и у них поменялись id, попробуйте снова найти их на экране с календарем и " +
                    "открыть заново (не переживайте, ваши корректировки не были перезаписаны)"));
    }

    @Test
    @DbUnitDataSet(before = "RecommendationControllerTest.increasingMonoXdocHonestSign.before.csv")
    public void testAdjustReplenishments_increasingMonoXdockHonestSign() throws Exception {
        String url = "/api/v1/recommendations/adjust";

        AdjustedRecommendationDTO adjusted = new AdjustedRecommendationDTO();
        adjusted.setAdjustedPurchQty(50);
        adjusted.setGroupId(0L);
        adjusted.setId(1L);
        adjusted.setMsku(100L);
        AdjustedRecommendationsDTO adjustments = new AdjustedRecommendationsDTO(Collections.singletonList(adjusted));
        adjustments.setDemandId(1L);
        adjustments.setDemandVersion(1);

        mockMvc.perform(put(url).contentType(APPLICATION_JSON_UTF8)
                .content(new ObjectMapper().writeValueAsString(adjustments)))
            .andExpect(status().isIAmATeapot())
            .andExpect(jsonPath("$.message").value(
                "Нельзя увеличивать значение для Mono-Xdock рекомендации с признаком Честный знак"));
    }

    @Test
    @DbUnitDataSet(before = "RecommendationControllerTest_testGetRecommendationsWithCount2.before.csv")
    public void testWithUserFilters_WrongDemand() throws Exception {
        final RecommendationFilters recommendationFilters = createFilters(null, 1L, 123L);
        final RecommendationUserFilter userFilter = new RecommendationUserFilter();
        userFilter.setField(MSKU);
        userFilter.setPredicate(UserFilterFieldPredicate.EQUAL);
        userFilter.setValue("100");
        mockMvc.perform(post("/api/v2/recommendations/with-count")
                .contentType(APPLICATION_JSON_UTF8)
                .content(dtoToString(recommendationFilters))
            )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value(
                "Потребности с типом 1P и c id 123 нет в базе, вероятно произошел реимпорт рекомендаций " +
                    "и у них поменялись id, попробуйте снова найти их на экране с календарем и " +
                    "открыть заново (не переживайте, ваши корректировки не были перезаписаны)"));
    }

    @Test
    @DbUnitDataSet(before = "RecommendationControllerTest.adjReplenishments.before.csv")
    public void testEbpAdjustReplenishments_WrongDemand() throws Exception {
        String url = "/api/v2/recommendations/needsManualReview?demandIds=123,345&needsManualReview=true";

        NeedsManualReviewRequest request = new NeedsManualReviewRequest();
        request.setDemandKeys(Arrays.asList(new DemandIdentityDTO(123L, 1),
            new DemandIdentityDTO(345L, 1)));
        request.setNeedsManualReview(false);

        var filter = new RecommendationFilter();
        filter.setDemandIds(Arrays.asList(123L, 345L));
        filter.setNeedsManualReview(true);

        var filters = new RecommendationFilters();
        filters.setFilter(filter);
        var userFilter = new RecommendationUserFilter();
        userFilter.setField(MSKU);
        userFilter.setPredicate(UserFilterFieldPredicate.EQUAL);
        userFilter.setValue("100");
        filters.setUserFilter(Collections.singletonList(userFilter));
        request.setRecommendationFilters(filters);

        mockMvc.perform(post(url).contentType(APPLICATION_JSON_UTF8)
                .content(new ObjectMapper().writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value(
                "Потребностей с типом 1P и c id 123, 345 нет в базе, вероятно произошел реимпорт рекомендаций" +
                    " " +
                    "и у них поменялись id, попробуйте снова найти их на экране с календарем и " +
                    "открыть заново (не переживайте, ваши корректировки не были перезаписаны)"));
    }

    @Test
    @DbUnitDataSet(before = "RecommendationControllerTest_testGetRecommendationsWithCount_duplicateMsku2.before.csv")
    public void testGetRecommendationsWithCount_duplicateMsku2() throws Exception {
        final RecommendationFilters recommendationFilters = createFilters(null, 1L);
        mockMvc.perform(post("/api/v2/recommendations/with-count")
                .contentType(APPLICATION_JSON_UTF8)
                .content(dtoToString(recommendationFilters))
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.recommendations.length()").value(3))

            .andExpect(jsonPath("$.recommendations[0].msku").value(100L))
            .andExpect(jsonPath("$.recommendations[0].title").value("msku_1"))
            .andExpect(jsonPath("$.recommendations[0].packageNumInSpike").value(16))
            .andExpect(jsonPath("$.recommendations[0].groupId").value(0L))
            .andExpect(commonMatcher(0))

            .andExpect(jsonPath("$.recommendations[1].msku").value(100L))
            .andExpect(jsonPath("$.recommendations[1].title").value("msku_1"))
            .andExpect(jsonPath("$.recommendations[1].packageNumInSpike").value(16))
            .andExpect(jsonPath("$.recommendations[1].groupId").value(1L))
            .andExpect(commonMatcher(1))

            .andExpect(jsonPath("$.recommendations[2].msku").value(200L))
            .andExpect(jsonPath("$.recommendations[2].title").value("msku_2"))
            .andExpect(jsonPath("$.recommendations[2].packageNumInSpike").value(999))
            .andExpect(jsonPath("$.recommendations[2].groupId").value(0L))
            .andExpect(commonMatcher(2));
    }

    @Test
    @DbUnitDataSet(before = "RecommendationControllerTest_testGetRecommendationsWithDistrDemand.before.csv")
    public void testGetRecommendationsWithDistrDemand() throws Exception {
        final RecommendationFilters recommendationFilters = createFilters(null, 1L);
        mockMvc.perform(post("/api/v2/recommendations/with-count")
                .contentType(APPLICATION_JSON_UTF8)
                .content(dtoToString(recommendationFilters))
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.recommendations.length()").value(1))

            .andExpect(jsonPath("$.recommendations[0].msku").value(200L))
            .andExpect(jsonPath("$.recommendations[0].demandId").value(1L))

            .andExpect(jsonPath("$.recommendations[0].stock").value(15))
            .andExpect(jsonPath("$.recommendations[0].stockOverall").value(30))
            .andExpect(jsonPath("$.recommendations[0].salesForecast14days1p").value(9.))
            .andExpect(jsonPath("$.recommendations[0].salesForecast28days1p").value(28.))
            .andExpect(jsonPath("$.recommendations[0].salesForecast56days1p").value(37.))
            .andExpect(jsonPath("$.recommendations[0].salesForecast14days3p").value(6.))
            .andExpect(jsonPath("$.recommendations[0].salesForecast28days3p").value(7.))
            .andExpect(jsonPath("$.recommendations[0].salesForecast56days3p").value(18.))
            .andExpect(jsonPath("$.recommendations[0].salesForecast14days").value(15.))
            .andExpect(jsonPath("$.recommendations[0].salesForecast28days").value(35.))
            .andExpect(jsonPath("$.recommendations[0].salesForecast56days").value(55.))
            .andExpect(jsonPath("$.recommendations[0].salesForecast14days").value(15.))
            .andExpect(jsonPath("$.recommendations[0].salesForecast28days").value(35.))
            .andExpect(jsonPath("$.recommendations[0].salesForecast56days").value(55.))
            .andExpect(jsonPath("$.recommendations[0].sales1p[0]").value(20))
            .andExpect(jsonPath("$.recommendations[0].sales1p[1]").value(22))
            .andExpect(jsonPath("$.recommendations[0].sales1p[2]").value(24))
            .andExpect(jsonPath("$.recommendations[0].sales1p[3]").value(26))
            .andExpect(jsonPath("$.recommendations[0].sales1p[4]").value(28))
            .andExpect(jsonPath("$.recommendations[0].sales1p[5]").value(30))
            .andExpect(jsonPath("$.recommendations[0].sales1p[6]").value(32))
            .andExpect(jsonPath("$.recommendations[0].sales1p[7]").value(34))
            .andExpect(jsonPath("$.recommendations[0].salesAll[0]").value(30))
            .andExpect(jsonPath("$.recommendations[0].salesAll[1]").value(32))
            .andExpect(jsonPath("$.recommendations[0].salesAll[2]").value(34))
            .andExpect(jsonPath("$.recommendations[0].salesAll[3]").value(36))
            .andExpect(jsonPath("$.recommendations[0].salesAll[4]").value(38))
            .andExpect(jsonPath("$.recommendations[0].salesAll[5]").value(40))
            .andExpect(jsonPath("$.recommendations[0].salesAll[6]").value(42))
            .andExpect(jsonPath("$.recommendations[0].salesAll[7]").value(44));
    }

    @Test
    @DbUnitDataSet(before = "RecommendationControllerTest_testGetRecommendationsWithGoalMsku.before.csv")
    public void testGetRecommendationsWithGoalMsku() throws Exception {
        final RecommendationFilters recommendationFilters = createFilters(null, 1L);
        mockMvc.perform(post("/api/v2/recommendations/with-count")
                .contentType(APPLICATION_JSON_UTF8)
                .content(dtoToString(recommendationFilters))
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.recommendations.length()").value(1))

            .andExpect(jsonPath("$.recommendations[0].msku").value(200L))
            .andExpect(jsonPath("$.recommendations[0].demandId").value(1L))

            .andExpect(jsonPath("$.recommendations[0].stock").value(10))
            .andExpect(jsonPath("$.recommendations[0].stockOverall").value(20))
            .andExpect(jsonPath("$.recommendations[0].salesForecast14days1p").value(7.))
            .andExpect(jsonPath("$.recommendations[0].salesForecast28days1p").value(16.))
            .andExpect(jsonPath("$.recommendations[0].salesForecast56days1p").value(25.))
            .andExpect(jsonPath("$.recommendations[0].salesForecast14days3p").value(3.))
            .andExpect(jsonPath("$.recommendations[0].salesForecast28days3p").value(4.))
            .andExpect(jsonPath("$.recommendations[0].salesForecast56days3p").value(5.))
            .andExpect(jsonPath("$.recommendations[0].salesForecast14days").value(10.))
            .andExpect(jsonPath("$.recommendations[0].salesForecast28days").value(20.))
            .andExpect(jsonPath("$.recommendations[0].salesForecast56days").value(30.))
            .andExpect(jsonPath("$.recommendations[0].sales1p[0]").value(10))
            .andExpect(jsonPath("$.recommendations[0].sales1p[1]").value(11))
            .andExpect(jsonPath("$.recommendations[0].sales1p[2]").value(11))
            .andExpect(jsonPath("$.recommendations[0].sales1p[3]").value(11))
            .andExpect(jsonPath("$.recommendations[0].sales1p[4]").value(11))
            .andExpect(jsonPath("$.recommendations[0].sales1p[5]").value(11))
            .andExpect(jsonPath("$.recommendations[0].sales1p[6]").value(11))
            .andExpect(jsonPath("$.recommendations[0].sales1p[7]").value(11))
            .andExpect(jsonPath("$.recommendations[0].salesAll[0]").value(15))
            .andExpect(jsonPath("$.recommendations[0].salesAll[1]").value(16))
            .andExpect(jsonPath("$.recommendations[0].salesAll[2]").value(16))
            .andExpect(jsonPath("$.recommendations[0].salesAll[3]").value(16))
            .andExpect(jsonPath("$.recommendations[0].salesAll[4]").value(16))
            .andExpect(jsonPath("$.recommendations[0].salesAll[5]").value(16))
            .andExpect(jsonPath("$.recommendations[0].salesAll[6]").value(16))
            .andExpect(jsonPath("$.recommendations[0].salesAll[7]").value(16));
    }

    @Test
    @DbUnitDataSet(before = "RecommendationControllerTest_testGetRecommendationsWithSalePromo.before.csv")
    public void testGetRecommendationsWithSalePromo() throws Exception {
        final RecommendationFilters recommendationFilters = createFilters(null, 1L);
        mockMvc.perform(post("/api/v2/recommendations/with-count")
                .contentType(APPLICATION_JSON_UTF8)
                .content(dtoToString(recommendationFilters))
            )
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.recommendations.length()").value(3))

            .andExpect(jsonPath("$.recommendations[0].msku").value(200L))
            .andExpect(jsonPath("$.recommendations[0].salePromoTitle").value("super-sale-1"))
            .andExpect(jsonPath("$.recommendations[0].salePromoNeededPurchase").value(true))

            .andExpect(jsonPath("$.recommendations[1].msku").value(300L))
            .andExpect(jsonPath("$.recommendations[1].salePromoTitle").value("super-sale-2"))
            .andExpect(jsonPath("$.recommendations[1].salePromoNeededPurchase").value(false))

            .andExpect(jsonPath("$.recommendations[2].msku").value(400L))
            .andExpect(jsonPath("$.recommendations[2].salePromoTitle").value("super-sale-3"))
            .andExpect(jsonPath("$.recommendations[2].salePromoNeededPurchase").value(nullValue())
            );
    }

    @Test
    @DbUnitDataSet(before = "RecommendationControllerTest_testGetRecommendationsWithGoalMskuForMsk.before.csv")
    public void testGetRecommendationsWithGoalMskuForMsk() throws Exception {
        final RecommendationFilters recommendationFilters = createFilters(null, 1L);
        mockMvc.perform(post("/api/v2/recommendations/with-count")
                .contentType(APPLICATION_JSON_UTF8)
                .content(dtoToString(recommendationFilters))
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.recommendations.length()").value(1))

            .andExpect(jsonPath("$.recommendations[0].msku").value(200L))
            .andExpect(jsonPath("$.recommendations[0].demandId").value(1L))

            .andExpect(jsonPath("$.recommendations[0].stock").value(15))
            .andExpect(jsonPath("$.recommendations[0].stockOverall").value(30))
            .andExpect(jsonPath("$.recommendations[0].salesForecast14days").value(15.))
            .andExpect(jsonPath("$.recommendations[0].salesForecast28days").value(35.))
            .andExpect(jsonPath("$.recommendations[0].salesForecast56days").value(55.))
            .andExpect(jsonPath("$.recommendations[0].sales1p[0]").value(20))
            .andExpect(jsonPath("$.recommendations[0].sales1p[1]").value(22))
            .andExpect(jsonPath("$.recommendations[0].sales1p[2]").value(24))
            .andExpect(jsonPath("$.recommendations[0].sales1p[3]").value(26))
            .andExpect(jsonPath("$.recommendations[0].sales1p[4]").value(28))
            .andExpect(jsonPath("$.recommendations[0].sales1p[5]").value(30))
            .andExpect(jsonPath("$.recommendations[0].sales1p[6]").value(32))
            .andExpect(jsonPath("$.recommendations[0].sales1p[7]").value(34))
            .andExpect(jsonPath("$.recommendations[0].salesAll[0]").value(30))
            .andExpect(jsonPath("$.recommendations[0].salesAll[1]").value(32))
            .andExpect(jsonPath("$.recommendations[0].salesAll[2]").value(34))
            .andExpect(jsonPath("$.recommendations[0].salesAll[3]").value(36))
            .andExpect(jsonPath("$.recommendations[0].salesAll[4]").value(38))
            .andExpect(jsonPath("$.recommendations[0].salesAll[5]").value(40))
            .andExpect(jsonPath("$.recommendations[0].salesAll[6]").value(42))
            .andExpect(jsonPath("$.recommendations[0].salesAll[7]").value(44));
    }

    @Test
    @DbUnitDataSet(before = "RecommendationControllerTest_testGetRecommendationsWithTransitWarning.before.csv")
    public void testGetRecommendationsWithTransitWarning() throws Exception {
        final RecommendationFilters recommendationFilters = createFilters(FilterType.TRANSIT_WARNING, 1L);
        mockMvc.perform(post("/api/v2/recommendations/with-count")
                .contentType(APPLICATION_JSON_UTF8)
                .content(dtoToString(recommendationFilters))
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.recommendations.length()").value(1))

            .andExpect(jsonPath("$.recommendations[0].msku").value(200L))
            .andExpect(jsonPath("$.recommendations[0].demandId").value(1L))

            .andExpect(jsonPath("$.recommendations[0].stock").value(10))
            .andExpect(jsonPath("$.recommendations[0].stockOverall").value(20))
            .andExpect(jsonPath("$.recommendations[0].salesForecast14days").value(10.))
            .andExpect(jsonPath("$.recommendations[0].salesForecast28days").value(20.))
            .andExpect(jsonPath("$.recommendations[0].salesForecast56days").value(30.))
            .andExpect(jsonPath("$.recommendations[0].sales1p[0]").value(10))
            .andExpect(jsonPath("$.recommendations[0].sales1p[1]").value(11))
            .andExpect(jsonPath("$.recommendations[0].sales1p[2]").value(12))
            .andExpect(jsonPath("$.recommendations[0].sales1p[3]").value(13))
            .andExpect(jsonPath("$.recommendations[0].sales1p[4]").value(14))
            .andExpect(jsonPath("$.recommendations[0].sales1p[5]").value(15))
            .andExpect(jsonPath("$.recommendations[0].sales1p[6]").value(16))
            .andExpect(jsonPath("$.recommendations[0].sales1p[7]").value(17))
            .andExpect(jsonPath("$.recommendations[0].salesAll[0]").value(15))
            .andExpect(jsonPath("$.recommendations[0].salesAll[1]").value(16))
            .andExpect(jsonPath("$.recommendations[0].salesAll[2]").value(17))
            .andExpect(jsonPath("$.recommendations[0].salesAll[3]").value(18))
            .andExpect(jsonPath("$.recommendations[0].salesAll[4]").value(19))
            .andExpect(jsonPath("$.recommendations[0].salesAll[5]").value(20))
            .andExpect(jsonPath("$.recommendations[0].salesAll[6]").value(21))
            .andExpect(jsonPath("$.recommendations[0].salesAll[7]").value(22));
    }

    @Test
    @DbUnitDataSet(before = "RecommendationControllerTest_testGetTenderRecommendations.before.csv")
    public void testGetTenderRecommendations() throws Exception {
        final RecommendationFilters recommendationFilters = createFilters(null, 1L);
        mockMvc.perform(post("/api/v1/recommendations/tender/with-count")
                .contentType(APPLICATION_JSON_UTF8)
                .content(dtoToString(recommendationFilters))
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.recommendations.length()").value(1))

            .andExpect(jsonPath("$.recommendations[0].msku").value(200L))
            .andExpect(jsonPath("$.recommendations[0].ssku").isEmpty())
            .andExpect(jsonPath("$.recommendations[0].demandId").value(1L))

            .andExpect(jsonPath("$.recommendations[0].transit").value(40))
            .andExpect(jsonPath("$.recommendations[0].stock").value(15))
            .andExpect(jsonPath("$.recommendations[0].stockOverall").value(30))
            .andExpect(jsonPath("$.recommendations[0].salesForecast14days").value(10.))
            .andExpect(jsonPath("$.recommendations[0].salesForecast28days").value(20.))
            .andExpect(jsonPath("$.recommendations[0].salesForecast56days").value(30.))
            .andExpect(jsonPath("$.recommendations[0].price").value(100.))
            .andExpect(jsonPath("$.recommendations[0].purchaseResultPrice").value(0.))
            .andExpect(jsonPath("$.recommendations[0].competitorPrice").value(120.))
            .andExpect(jsonPath("$.recommendations[0].sales1p[0]").value(20))
            .andExpect(jsonPath("$.recommendations[0].sales1p[1]").value(22))
            .andExpect(jsonPath("$.recommendations[0].sales1p[2]").value(24))
            .andExpect(jsonPath("$.recommendations[0].sales1p[3]").value(26))
            .andExpect(jsonPath("$.recommendations[0].sales1p[4]").value(28))
            .andExpect(jsonPath("$.recommendations[0].sales1p[5]").value(30))
            .andExpect(jsonPath("$.recommendations[0].sales1p[6]").value(32))
            .andExpect(jsonPath("$.recommendations[0].sales1p[7]").value(34))
            .andExpect(jsonPath("$.recommendations[0].salesAll[0]").value(30))
            .andExpect(jsonPath("$.recommendations[0].salesAll[1]").value(32))
            .andExpect(jsonPath("$.recommendations[0].salesAll[2]").value(34))
            .andExpect(jsonPath("$.recommendations[0].salesAll[3]").value(36))
            .andExpect(jsonPath("$.recommendations[0].salesAll[4]").value(38))
            .andExpect(jsonPath("$.recommendations[0].salesAll[5]").value(40))
            .andExpect(jsonPath("$.recommendations[0].salesAll[6]").value(42))
            .andExpect(jsonPath("$.recommendations[0].salesAll[7]").value(44))
            .andExpect(jsonPath("$.recommendations[0].vendorName").value("JobsAndCompany"))
            .andExpect(jsonPath("$.recommendations[0].isCoreFixMatrix").value(true))
            .andExpect(jsonPath("$.recommendations[0].priceSegment").value("seg2"))
            .andExpect(jsonPath("$.recommendations[0].supplierLifetime").value(2))
            .andExpect(jsonPath("$.recommendations[0].inLifetime").value(20))
            .andExpect(jsonPath("$.recommendations[0].outLifetime").value(200))
            .andExpect(jsonPath("$.recommendations[0].reasonOfRecommendation").value("comment100"));
    }

    @Test
    @DbUnitDataSet(before = "RecommendationControllerTest.testGetGroupedTenderRecommendations.before.csv")
    public void testGetGroupedTenderRecommendations() throws Exception {
        final RecommendationFilters recommendationFilters = createFilters(null, 1L, 2L);
        mockMvc.perform(post("/api/v1/recommendations/tender/with-count")
                .contentType(APPLICATION_JSON_UTF8)
                .content(dtoToString(recommendationFilters))
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.recommendations.length()").value(4))

            .andExpect(jsonPath("$.recommendations[?(@.id == 1)].msku").value(200))
            .andExpect(jsonPath("$.recommendations[?(@.id == 1)].purchaseQuantity").value(5))
            .andExpect(jsonPath("$.recommendations[?(@.id == 1)].demandId").value(1))
            .andExpect(jsonPath("$.recommendations[?(@.id == 1)].price").value(100.0))

            .andExpect(jsonPath("$.recommendations[?(@.id == 2)].msku").value(300))
            .andExpect(jsonPath("$.recommendations[?(@.id == 2)].purchaseQuantity").value(7))
            .andExpect(jsonPath("$.recommendations[?(@.id == 2)].demandId").value(1))
            .andExpect(jsonPath("$.recommendations[?(@.id == 2)].price").value(101.0))

            .andExpect(jsonPath("$.recommendations[?(@.id == 3)].msku").value(200))
            .andExpect(jsonPath("$.recommendations[?(@.id == 3)].purchaseQuantity").value(6))
            .andExpect(jsonPath("$.recommendations[?(@.id == 3)].demandId").value(2))
            .andExpect(jsonPath("$.recommendations[?(@.id == 3)].price").value(100.0))

            .andExpect(jsonPath("$.recommendations[?(@.id == 4)].msku").value(300))
            .andExpect(jsonPath("$.recommendations[?(@.id == 4)].purchaseQuantity").value(8))
            .andExpect(jsonPath("$.recommendations[?(@.id == 4)].demandId").value(2))
            .andExpect(jsonPath("$.recommendations[?(@.id == 4)].price").value(101.0));
    }

    @Test
    @DbUnitDataSet(before = "RecommendationControllerTest_testGetTenderRecommendations.before.csv")
    public void testGetTenderRecommendationsWarehouseFilter() throws Exception {
        final RecommendationFilters recommendationFilters = createFilters(null, 1L);
        final RecommendationUserFilter userFilter = new RecommendationUserFilter();
        userFilter.setField(WAREHOUSE);
        userFilter.setPredicate(UserFilterFieldPredicate.EQUAL);
        userFilter.setValue("1");
        recommendationFilters.setUserFilter(Collections.singletonList(userFilter));
        mockMvc.perform(post("/api/v1/recommendations/tender/with-count")
                .contentType(APPLICATION_JSON_UTF8)
                .content(dtoToString(recommendationFilters))
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.recommendations.length()").value(0));
        recommendationFilters.getUserFilter().get(0).setValue("171");
        mockMvc.perform(post("/api/v1/recommendations/tender/with-count")
                .contentType(APPLICATION_JSON_UTF8)
                .content(dtoToString(recommendationFilters))
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.recommendations.length()").value(1));
    }

    @Test
    @DbUnitDataSet(before = "RecommendationControllerTest.testGet3PRecommendations.before.csv")
    public void testGet3pRecommendationsDemand() throws Exception {
        final RecommendationFilters recommendationFilters = createFilters(null, 1L);
        mockMvc.perform(post("/api/v2/recommendations/with-count?demandType=TYPE_3P")
                .contentType(APPLICATION_JSON_UTF8)
                .content(dtoToString(recommendationFilters))
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.recommendations.length()").value(1))

            .andExpect(jsonPath("$.recommendations[0].id").value(100500))
            .andExpect(jsonPath("$.recommendations[0].msku").value(200L))
            .andExpect(jsonPath("$.recommendations[0].ssku").value("the_ssku"))
            .andExpect(jsonPath("$.recommendations[0].demandId").value(1L));
    }

    @Test
    @DbUnitDataSet(before = "RecommendationControllerTest.testGet3PRecommendationsWithSummary.before.csv")
    public void testGet3pWithSummaryRecommendations() throws Exception {
        final RecommendationFilters recommendationFilters = createFilters(null, 1L);
        var r = mockMvc.perform(post("/api/v2/recommendations/with-count?demandType=TYPE_3P")
                .contentType(APPLICATION_JSON_UTF8)
                .content(dtoToString(recommendationFilters))
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.recommendations.length()").value(3));

        r.andExpect(jsonPath("$.recommendations[0].purchaseQuantity").value(9))
            .andExpect(jsonPath("$.recommendations[1].purchaseQuantity").value(7))
            .andExpect(jsonPath("$.recommendations[2].purchaseQuantity").value(16))

            .andExpect(jsonPath("$.recommendations[0].oosDays").value(7))
            .andExpect(jsonPath("$.recommendations[1].oosDays").value(7))
            .andExpect(jsonPath("$.recommendations[2].oosDays").value(14))

            .andExpect(jsonPath("$.recommendations[0].transit").value(5))
            .andExpect(jsonPath("$.recommendations[1].transit").value(5))
            .andExpect(jsonPath("$.recommendations[2].transit").value(10))

            .andExpect(jsonPath("$.recommendations[0].id").value(1))
            .andExpect(jsonPath("$.recommendations[1].id").value(2))
            .andExpect(jsonPath("$.recommendations[2].id").value(-1));
    }

    @Test
    @DbUnitDataSet(before = "RecommendationControllerTest.test3PRecommendationsWithSummary_checkSalesAndStocks.before" +
        ".csv")
    public void testGet3pWithSummaryRecommendations_checkSales() throws Exception {
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

        final RecommendationFilters recommendationFilters = createFilters(null, 1L, 2L);
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

            .andExpect(jsonPath("$.recommendations[0].oosDays").value(7))
            .andExpect(jsonPath("$.recommendations[1].oosDays").isEmpty())
            .andExpect(jsonPath("$.recommendations[2].oosDays").value(7))

            .andExpect(jsonPath("$.recommendations[0].missedOrders28d").value(11))
            .andExpect(jsonPath("$.recommendations[1].missedOrders28d").value(11))
            .andExpect(jsonPath("$.recommendations[2].missedOrders28d").value(11))

            .andExpect(jsonPath("$.recommendations[0].missedOrders56d").value(12))
            .andExpect(jsonPath("$.recommendations[1].missedOrders56d").value(12))
            .andExpect(jsonPath("$.recommendations[2].missedOrders56d").value(12))

            .andExpect(jsonPath("$.recommendations[0].salesForecast14days").value(1))
            .andExpect(jsonPath("$.recommendations[1].salesForecast14days").value(1))
            .andExpect(jsonPath("$.recommendations[2].salesForecast14days").value(1))

            .andExpect(jsonPath("$.recommendations[0].salesForecast28days").value(2))
            .andExpect(jsonPath("$.recommendations[1].salesForecast28days").value(2))
            .andExpect(jsonPath("$.recommendations[2].salesForecast28days").value(2))

            .andExpect(jsonPath("$.recommendations[0].salesForecast56days").value(2))
            .andExpect(jsonPath("$.recommendations[1].salesForecast56days").value(2))
            .andExpect(jsonPath("$.recommendations[2].salesForecast56days").value(3))

            .andExpect(jsonPath("$.recommendations[0].stock").value(10))
            .andExpect(jsonPath("$.recommendations[1].stock").value(15))
            .andExpect(jsonPath("$.recommendations[2].stock").value(25))

            .andExpect(jsonPath("$.recommendations[0].stockOverall").value(65))
            .andExpect(jsonPath("$.recommendations[1].stockOverall").value(35))
            .andExpect(jsonPath("$.recommendations[2].stockOverall").value(100));
    }

    private static final int MSKU_1 = 200;
    private static final int MSKU_2 = 300;

    private static final String SSKU_1 = "that_ssku";
    private static final String SSKU_2 = "this_one_for_sure";

    private static final int DEMAND_1 = 1;
    private static final int DEMAND_2 = 2;

    @Test
    @DbUnitDataSet(before = "RecommendationControllerTest.testGetExported3PRecommendationsWithSummary.before.csv")
    public void testGetExported3pWithSummaryRecommendations() throws Exception {
        final Matcher<Iterable<Integer>> isOneOfDemands = everyItem(isOneOf(DEMAND_1, DEMAND_2));

        final RecommendationFilters recommendationFilters = createFilters(null, 1L, 2L);
        mockMvc.perform(post("/api/v2/recommendations/with-count?demandType=TYPE_3P")
                .contentType(APPLICATION_JSON_UTF8)
                .content(dtoToString(recommendationFilters))
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.recommendations", hasSize(6)))

            // Grouped by MSKU #1
            .andExpect(jsonPath("$.recommendations[?(@.id == 1)]", hasSize(1)))
            .andExpect(jsonPath("$.recommendations[?(@.id == 1)].msku").value(MSKU_1))
            .andExpect(jsonPath("$.recommendations[?(@.id == 1)].ssku").value(SSKU_1))
            .andExpect(jsonPath("$.recommendations[?(@.id == 1)].demandId").value(DEMAND_1))
            .andExpect(jsonPath("$.recommendations[?(@.id == 1)].purchaseQuantity").value(9))
            .andExpect(jsonPath("$.recommendations[?(@.id == 1)].transit").value(5))
            .andExpect(jsonPath("$.recommendations[?(@.id == 1)].oosDays").value(7))

            .andExpect(jsonPath("$.recommendations[?(@.id == 3)]", hasSize(1)))
            .andExpect(jsonPath("$.recommendations[?(@.id == 3)].msku").value(MSKU_1))
            .andExpect(jsonPath("$.recommendations[?(@.id == 3)].ssku").value(SSKU_1))
            .andExpect(jsonPath("$.recommendations[?(@.id == 3)].demandId").value(DEMAND_2))
            .andExpect(jsonPath("$.recommendations[?(@.id == 3)].purchaseQuantity").value(100))
            .andExpect(jsonPath("$.recommendations[?(@.id == 3)].transit").value(7))
            .andExpect(jsonPath("$.recommendations[?(@.id == 3)].oosDays").value(9))

            // Summary for MSKU #1
            .andExpect(jsonPath("$.recommendations[?(@.id == -1)]", hasSize(1)))
            .andExpect(jsonPath("$.recommendations[?(@.id == -1)].msku").value(MSKU_1))
            .andExpect(jsonPath("$.recommendations[?(@.id == -1)].ssku", everyItem(nullValue())))
            .andExpect(jsonPath("$.recommendations[?(@.id == -1)].demandId", isOneOfDemands))
            .andExpect(jsonPath("$.recommendations[?(@.id == -1)].purchaseQuantity").value(109))
            .andExpect(jsonPath("$.recommendations[?(@.id == -1)].transit").value(12))
            .andExpect(jsonPath("$.recommendations[?(@.id == -1)].oosDays").value(16))

            // Grouped by MSKU #2
            .andExpect(jsonPath("$.recommendations[?(@.id == 2)]", hasSize(1)))
            .andExpect(jsonPath("$.recommendations[?(@.id == 2)].msku").value(MSKU_2))
            .andExpect(jsonPath("$.recommendations[?(@.id == 2)].ssku").value(SSKU_2))
            .andExpect(jsonPath("$.recommendations[?(@.id == 2)].demandId").value(DEMAND_1))
            .andExpect(jsonPath("$.recommendations[?(@.id == 2)].purchaseQuantity").value(7))
            .andExpect(jsonPath("$.recommendations[?(@.id == 2)].transit").value(6))
            .andExpect(jsonPath("$.recommendations[?(@.id == 2)].oosDays").value(8))

            .andExpect(jsonPath("$.recommendations[?(@.id == 4)]", hasSize(1)))
            .andExpect(jsonPath("$.recommendations[?(@.id == 4)].msku").value(MSKU_2))
            .andExpect(jsonPath("$.recommendations[?(@.id == 4)].ssku").value(SSKU_2))
            .andExpect(jsonPath("$.recommendations[?(@.id == 4)].demandId").value(DEMAND_2))
            .andExpect(jsonPath("$.recommendations[?(@.id == 4)].purchaseQuantity").value(200))
            .andExpect(jsonPath("$.recommendations[?(@.id == 4)].transit").value(8))
            .andExpect(jsonPath("$.recommendations[?(@.id == 4)].oosDays").value(10))

            // Summary for MSKU #1
            .andExpect(jsonPath("$.recommendations[?(@.id == -2)]", hasSize(1)))
            .andExpect(jsonPath("$.recommendations[?(@.id == -2)].msku").value(MSKU_2))
            .andExpect(jsonPath("$.recommendations[?(@.id == -2)].ssku", everyItem(nullValue())))
            .andExpect(jsonPath("$.recommendations[?(@.id == -2)].demandId", isOneOfDemands))
            .andExpect(jsonPath("$.recommendations[?(@.id == -2)].purchaseQuantity").value(207))
            .andExpect(jsonPath("$.recommendations[?(@.id == -2)].transit").value(14))
            .andExpect(jsonPath("$.recommendations[?(@.id == -2)].oosDays").value(18));
    }

    @Test
    @DbUnitDataSet(before = "RecommendationControllerTest.getMultipleWh.before.csv")
    public void testGetRecommendationsMultipleWh() throws Exception {
        final int[] sales1p = {1, 2, 3, 4, 5, 6, 7, 8};
        final int[] salesAll = Arrays.stream(sales1p).map(s -> s * 2).toArray();

        final int[] totalSales1p = ArrayUtils.clone(salesAll);
        final int[] totalSalesAll = Arrays.stream(salesAll).map(s -> s * 2).toArray();

        testSummary(MULTIPLE_WAREHOUSES, 8, salesAll, sales1p, totalSalesAll, totalSales1p);
    }

    @Test
    @DbUnitDataSet(before = "RecommendationControllerTest.getSpecialOrders.before.csv")
    public void testGetRecommendationsSpecialOrder() throws Exception {
        final int[] sales1p = {2, 4, 6, 8, 10, 12, 14, 16};
        final int[] salesAll = Arrays.stream(sales1p).map(s -> s * 2).toArray();

        testSummary(SPECIAL_ORDER, 9, salesAll, sales1p, salesAll, sales1p);
    }

    @Test
    @DbUnitDataSet(before = "RecommendationControllerTest.getMultipleWh_withDistrDemand.before.csv")
    public void testGetRecommendationsMultipleWh_withDistrDemand() throws Exception {
        final int[] sales1p = {1, 2, 3, 4, 5, 6, 7, 8};
        final int[] salesAll = Arrays.stream(sales1p).map(s -> s * 2).toArray();

        final int[] totalSales1p = Arrays.stream(sales1p).map(s -> s * 3).toArray();
        final int[] totalSalesAll = Arrays.stream(salesAll).map(s -> s * 3).toArray();

        testSummaryWithDistrDemand(MULTIPLE_WAREHOUSES, 8, salesAll, sales1p, totalSalesAll, totalSales1p);
    }

    @Test
    @DbUnitDataSet(before = "RecommendationControllerTest.getMultipleWh.before.csv")
    public void testGetRecommendationsMultipleWhWithNotMultipleWhFilter() throws Exception {
        mockMvc.perform(post("/api/v2/recommendations/with-count")
                .contentType(APPLICATION_JSON_UTF8)
                .content(dtoToString(createFilters(ALL, 1L, 2L)))
            )
            .andExpect(status().isOk())

            .andExpect(jsonPath("$.count.filter[0].id").value("ALL"))
            .andExpect(jsonPath("$.count.filter[0].count").value(3))
            .andExpect(jsonPath("$.count.filter[8].id").value("MULTIPLE_WAREHOUSES"))
            .andExpect(jsonPath("$.count.filter[8].count").value(2))

            .andExpect(jsonPath("$.recommendations.length()").value(1))
            .andExpect(jsonPath("$.recommendations[0].msku").value(200L));
    }

    @Test
    @DbUnitDataSet(before = "RecommendationControllerTest.getAssortmentGoods.before.csv")
    public void testGetAssortmentGoods() throws Exception {
        mockMvc.perform(post("/api/v2/recommendations/with-count")
                .contentType(APPLICATION_JSON_UTF8)
                .content(dtoToString(createFilters(ASSORTMENT_GOODS_SUB_SSKU, 2L)))
            )
            .andDo(print())
            .andExpect(status().isOk())

            .andExpect(jsonPath("$.count.filter[0].id").value("ALL"))
            .andExpect(jsonPath("$.count.filter[0].count").value(3))
            .andExpect(jsonPath("$.count.filter[11].id").value("ASSORTMENT_GOODS_SUB_SSKU"))
            .andExpect(jsonPath("$.count.filter[11].count").value(2))

            .andExpect(jsonPath("$.recommendations.length()").value(3))

            .andExpect(jsonPath("$.recommendations[0].id").value(-1L))
            .andExpect(jsonPath("$.recommendations[0].summaryGroupId").value(-1L))
            .andExpect(jsonPath("$.recommendations[0].ssku").value("000042.assort_ssku"))
            .andExpect(jsonPath("$.recommendations[0].msku").value(666L))
            .andExpect(jsonPath("$.recommendations[0].demandId").value(2L))
            .andExpect(jsonPath("$.recommendations[0].categoryName").value("the category"))
            .andExpect(jsonPath("$.recommendations[0].warehouseId").value(172))
            .andExpect(jsonPath("$.recommendations[0].id").value(-1L))
            .andExpect(jsonPath("$.recommendations[0].title").value("msku_666"))
            .andExpect(jsonPath("$.recommendations[0].abc").isEmpty())
            .andExpect(jsonPath("$.recommendations[0].purchaseQuantity").value(3))
            .andExpect(jsonPath("$.recommendations[0].setQuantity").value(3))
            .andExpect(jsonPath("$.recommendations[0].correctionReason").isEmpty())
            .andExpect(jsonPath("$.recommendations[0].sales1p[0]").value(2))
            .andExpect(jsonPath("$.recommendations[0].sales1p[1]").value(4))
            .andExpect(jsonPath("$.recommendations[0].sales1p[2]").value(6))
            .andExpect(jsonPath("$.recommendations[0].sales1p[3]").value(8))
            .andExpect(jsonPath("$.recommendations[0].sales1p[4]").value(10))
            .andExpect(jsonPath("$.recommendations[0].sales1p[5]").value(12))
            .andExpect(jsonPath("$.recommendations[0].sales1p[6]").value(14))
            .andExpect(jsonPath("$.recommendations[0].sales1p[7]").value(16))
            .andExpect(jsonPath("$.recommendations[0].salesAll[0]").value(4))
            .andExpect(jsonPath("$.recommendations[0].salesAll[1]").value(8))
            .andExpect(jsonPath("$.recommendations[0].salesAll[2]").value(12))
            .andExpect(jsonPath("$.recommendations[0].salesAll[3]").value(16))
            .andExpect(jsonPath("$.recommendations[0].salesAll[4]").value(20))
            .andExpect(jsonPath("$.recommendations[0].salesAll[5]").value(24))
            .andExpect(jsonPath("$.recommendations[0].salesAll[6]").value(28))
            .andExpect(jsonPath("$.recommendations[0].salesAll[7]").value(32))
            .andExpect(jsonPath("$.recommendations[0].stock").value(13))
            .andExpect(jsonPath("$.recommendations[0].stockOverall").value(17))
            .andExpect(jsonPath("$.recommendations[0].salesForecast14days").value(3.0))
            .andExpect(jsonPath("$.recommendations[0].salesForecast28days").value(7.0))
            .andExpect(jsonPath("$.recommendations[0].salesForecast56days").value(11.0))
            .andExpect(jsonPath("$.recommendations[0].barcode").value("123, 321"))
            .andExpect(jsonPath("$.recommendations[0].transit1p").value(0))
            .andExpect(jsonPath("$.recommendations[0].transit").value(15))
            .andExpect(jsonPath("$.recommendations[0].minShipment").value(1))
            .andExpect(jsonPath("$.recommendations[0].shipmentQuantum").value(1))
            .andExpect(jsonPath("$.recommendations[0].deliveryTime").value(7))
            .andExpect(jsonPath("$.recommendations[0].exported").value(false))
            .andExpect(jsonPath("$.recommendations[0].orderId").isEmpty())
            .andExpect(jsonPath("$.recommendations[0].orderError").isEmpty())
            .andExpect(jsonPath("$.recommendations[0].oos28Days").value(3))
            .andExpect(jsonPath("$.recommendations[0].oosDays").value(3))
            .andExpect(jsonPath("$.recommendations[0].averageSales1p").value(0.32))
            .andExpect(jsonPath("$.recommendations[0].averageSalesAll").value(0.64))
            .andExpect(jsonPath("$.recommendations[0].safetyStock").value(3))
            .andExpect(jsonPath("$.recommendations[0].missedOrders28d").value(3))
            .andExpect(jsonPath("$.recommendations[0].missedOrders56d").value(5))
            .andExpect(jsonPath("$.recommendations[0].vendorName").value("MirPack"))
            .andExpect(jsonPath("$.recommendations[0].groupId").value(0))
            .andExpect(jsonPath("$.recommendations[0].weight").value(4))
            .andExpect(jsonPath("$.recommendations[0].height").value(3))
            .andExpect(jsonPath("$.recommendations[0].width").value(2))
            .andExpect(jsonPath("$.recommendations[0].length").value(1))
            .andExpect(jsonPath("$.recommendations[0].actualSales").value(8))
            .andExpect(jsonPath("$.recommendations[0].actualFit").value(90))
            .andExpect(jsonPath("$.recommendations[0].needsManualReview").value(true))
            .andExpect(jsonPath("$.recommendations[0].needsManualReviewCause").value(""))
            .andExpect(jsonPath("$.recommendations[0].filter").value("ASSORTMENT_GOODS_SUB_SSKU"))
            .andExpect(jsonPath("$.recommendations[0].filterWeeks").isEmpty())
            .andExpect(jsonPath("$.recommendations[0].hasCheaperRecommendation").value(true))
            .andExpect(jsonPath("$.recommendations[0].isCoreFixMatrix").value(true))

            .andExpect(jsonPath("$.recommendations[1].ssku").value("000042.ssku1"))
            .andExpect(jsonPath("$.recommendations[1].msku").value(100L))
            .andExpect(jsonPath("$.recommendations[1].summaryGroupId").value(-1L))

            .andExpect(jsonPath("$.recommendations[2].ssku").value("000042.ssku2"))
            .andExpect(jsonPath("$.recommendations[2].msku").value(200L))
            .andExpect(jsonPath("$.recommendations[2].summaryGroupId").value(-1L));
    }

    @Test
    @DbUnitDataSet(before = "RecommendationControllerTest.getWithEditWarnings.before.csv")
    public void testGetRecommendationsWithEmptyWarningFilter() throws Exception {
        RecommendationWarningFiltersDTO filters = new RecommendationWarningFiltersDTO(
            List.of(1L, 2L, 5L),
            Collections.emptySet()
        );

        mockMvc.perform(post("/api/v1/recommendations/with-edit-warnings")
                .contentType(APPLICATION_JSON_UTF8)
                .content(dtoToString(filters))
            ).andExpect(status().isOk())

            .andExpect(jsonPath("$.warningCounts.SCB_OVER_MAX_LIFETIME").value(1))
            .andExpect(jsonPath("$.warningCounts.SCF_OVER_MAX_LIFETIME").value(1))
            .andExpect(jsonPath("$.warningCounts.ADJUSTED_PURCH_QTY_OVER_PURCH_QTY").value(2))
            .andExpect(jsonPath("$.recommendations", hasSize(6)));
    }

    @Test
    @DbUnitDataSet(before = "RecommendationControllerTest.getWithEditWarnings.before.csv")
    public void testGetRecommendationsWithSCFOverMaxLifetime() throws Exception {
        RecommendationWarningFiltersDTO filters = new RecommendationWarningFiltersDTO(
            List.of(1L, 2L),
            Collections.singleton(WarningFilterType.SCF_OVER_MAX_LIFETIME)
        );

        mockMvc.perform(post("/api/v1/recommendations/with-edit-warnings")
                .contentType(APPLICATION_JSON_UTF8)
                .content(dtoToString(filters))
            ).andExpect(status().isOk())

            .andExpect(jsonPath("$.warningCounts.SCB_OVER_MAX_LIFETIME").value(1))
            .andExpect(jsonPath("$.warningCounts.SCF_OVER_MAX_LIFETIME").value(1))

            .andExpect(jsonPath("$.recommendations", hasSize(1)))
            .andExpect(jsonPath("$.recommendations[0].id").value(2))
            .andExpect(jsonPath("$.recommendations[0].stockCoverForward").value(365));
    }

    @Test
    @DbUnitDataSet(before = "RecommendationControllerTest.getWithEditWarnings.before.csv")
    public void testGetRecommendationsWithSCBOverMaxLifetime() throws Exception {
        RecommendationWarningFiltersDTO filters = new RecommendationWarningFiltersDTO(
            List.of(1L, 2L),
            Collections.singleton(WarningFilterType.SCB_OVER_MAX_LIFETIME)
        );

        mockMvc.perform(post("/api/v1/recommendations/with-edit-warnings")
                .contentType(APPLICATION_JSON_UTF8)
                .content(dtoToString(filters))
            ).andExpect(status().isOk())

            .andDo(result -> System.out.println(result.getResponse().getContentAsString()))
            .andExpect(jsonPath("$.warningCounts.SCB_OVER_MAX_LIFETIME").value(1))
            .andExpect(jsonPath("$.warningCounts.SCF_OVER_MAX_LIFETIME").value(1))

            .andExpect(jsonPath("$.recommendations", hasSize(1)))
            .andExpect(jsonPath("$.recommendations[0].id").value(2))
            .andExpect(jsonPath("$.recommendations[0].stockCoverBackward").value(365));
    }

    @Test
    @DbUnitDataSet(before = "RecommendationControllerTest.getWithEditWarnings.before.csv")
    public void testGetRecommendationsWithAdjustedQtyOverPurchQty() throws Exception {
        RecommendationWarningFiltersDTO filters = new RecommendationWarningFiltersDTO(
            List.of(1L, 2L, 5L),
            Collections.singleton(WarningFilterType.ADJUSTED_PURCH_QTY_OVER_PURCH_QTY)
        );

        mockMvc.perform(post("/api/v1/recommendations/with-edit-warnings")
                .contentType(APPLICATION_JSON_UTF8)
                .content(dtoToString(filters))
            ).andExpect(status().isOk())
            .andExpect(jsonPath("$.warningCounts.SCB_OVER_MAX_LIFETIME").value(1))
            .andExpect(jsonPath("$.warningCounts.SCF_OVER_MAX_LIFETIME").value(1))
            .andExpect(jsonPath("$.warningCounts.ADJUSTED_PURCH_QTY_OVER_PURCH_QTY").value(2))

            .andExpect(jsonPath("$.recommendations", hasSize(2)))

            .andExpect(jsonPath("$.recommendations[0].id").value(4))
            .andExpect(jsonPath("$.recommendations[0].setQuantity").value(12))
            .andExpect(jsonPath("$.recommendations[0].purchaseQuantity").value(1))

            .andExpect(jsonPath("$.recommendations[1].id").value(5))
            .andExpect(jsonPath("$.recommendations[1].setQuantity").value(2))
            .andExpect(jsonPath("$.recommendations[1].purchaseQuantity").value(100));
    }

    @Test
    @DbUnitDataSet(before = "RecommendationRepository.uploadExcel.before.csv")
    public void testUploadExcelWithEmptyQuantities() throws Exception {
        excelTestingHelper.upload(
                "PUT",
                "/api/v1/recommendations/excel?version=1",
                "RecommendationControllerTest.testUploadExcel_no_quantities.xlsx"
            )
            .andExpect(status().isIAmATeapot())
            .andExpect(jsonPath("$.message")
                .value("Хотя бы одна колонка количества в строке 10 не должна быть пустой"));
    }

    @Test
    @DbUnitDataSet(before = "RecommendationControllerTest.increasingMonoXdocHonestSign.before.csv")
    public void testIncreasingMonoXdockHonestTypeExcel() throws Exception {
        excelTestingHelper.upload(
                "PUT",
                "/api/v1/recommendations/excel?version=1",
                "RecommendationControllerTest.testIncreasingMonoXdockHonestTypeExcel.xlsx"
            )
            .andExpect(status().isIAmATeapot())
            .andExpect(jsonPath("$.message")
                .value("Нельзя увеличивать значение для Mono-Xdock рекомендации с признаком Честный знак"));
    }

    @Test
    @DbUnitDataSet(before = "RecommendationRepository.uploadExcel.before.csv",
        after = "RecommendationRepository.uploadExcel.after.csv")
    public void testUploadExcel() throws Exception {
        excelTestingHelper.upload(
            "PUT",
            "/api/v1/recommendations/excel?version=1",
            "RecommendationControllerTest.testUploadExcel.xlsx"
        ).andExpect(status().isOk());
    }

    @Test
    @DbUnitDataSet(before = "RecommendationControllerTest.3p_adjRecommendations.before.csv",
        after = "RecommendationControllerTest.3p_uploadExcel.after.csv")
    public void testUploadExcel_3p() throws Exception {
        excelTestingHelper.upload(
            "PUT",
            "/api/v1/recommendations/excel?demandType=TYPE_3P&version=1",
            "RecommendationControllerTest.testUploadExcel_3p.xlsx"
        ).andExpect(status().isOk());
        parentDemand3pUpdateLoader.load();
    }

    @Test
    @DbUnitDataSet(before = "RecommendationRepository.uploadExcel.before.csv",
        after = "RecommendationRepository.uploadExcel.after.csv")
    public void testUploadExcelOnlyAdjustment() throws Exception {
        excelTestingHelper.upload(
            "PUT",
            "/api/v1/recommendations/excel?version=1",
            "RecommendationControllerTest.testUploadExcel_no_recommendation.xlsx"
        ).andExpect(status().isOk());
    }

    @Test
    @DbUnitDataSet(before = "RecommendationRepository.uploadExcel.before.csv",
        after = "RecommendationRepository.uploadExcel.after.csv")
    public void testUploadExcelFallbackAdjustment() throws Exception {
        excelTestingHelper.upload(
            "PUT",
            "/api/v1/recommendations/excel?version=1",
            "RecommendationControllerTest.testUploadExcel_no_recommendation.xlsx"
        ).andExpect(status().isOk());
    }

    @Test
    @DbUnitDataSet(before = "RecommendationRepository.uploadExcel.before.csv",
        after = "RecommendationRepository.uploadExcelWithDuplicate.after.csv")
    public void testUploadExcelWithDuplicate() throws Exception {
        excelTestingHelper.upload(
            "PUT",
            "/api/v1/recommendations/excel?version=1",
            "RecommendationControllerTest.testUploadExcelWithDuplicate.xlsx"
        ).andExpect(status().isOk());
    }

    @Test
    @DbUnitDataSet(before = "RecommendationRepository.uploadExcel.before.csv",
        after = "RecommendationRepository.uploadExcel.before.csv")
    public void testUploadExcelWithDuplicate_notGroup() throws Exception {
        excelTestingHelper.upload(
                "PUT",
                "/api/v1/recommendations/excel?version=1",
                "RecommendationControllerTest.testUploadExcelWithDuplicate_notGroup.xlsx"
            ).andExpect(status().isIAmATeapot())
            .andExpect(jsonPath("$.message")
                .value("Присутствуют дубли MSKU 400 внутри одной группы"));
    }

    @Test
    @DbUnitDataSet(before = "RecommendationRepository.sourceDuplicate.before.csv",
        after = "RecommendationRepository.sourceDuplicate.after.csv")
    public void testUploadExcelWithSourceDuplicate() throws Exception {
        excelTestingHelper.upload(
            "PUT",
            "/api/v1/recommendations/excel?version=1",
            "RecommendationControllerTest.sourceDuplicate.xlsx"
        ).andExpect(status().isOk());
    }

    @Test
    @DbUnitDataSet(before = "RecommendationRepository.conflict.before.csv",
        after = "RecommendationRepository.conflict.after.csv")
    public void testUploadExcelWithConflict() throws Exception {
        excelTestingHelper.upload(
            "PUT",
            "/api/v1/recommendations/excel?version=1",
            "RecommendationControllerTest.testConflict.xlsx"
        ).andExpect(status().isOk());
    }

    @Test
    @DbUnitDataSet(before = "GetImportEta.before.csv")
    public void testGetImportEta() throws Exception {
        mockMvc.perform(get("/api/v1/recommendations/import-eta")
                .contentType(APPLICATION_JSON_UTF8))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.intervalMinutes").value("10"))
            .andExpect(jsonPath("$.eta1PMinutes").value("70"))
            .andExpect(jsonPath("$.eta3PMinutes").value("0"))
            .andExpect(jsonPath("$.etaTenderMinutes").value("70"))
            .andExpect(jsonPath("$.started1P").value("2021-11-16T15:00:00"))
            .andExpect(jsonPath("$.started3P").value(nullValue()))
            .andExpect(jsonPath("$.startedTender").value("2021-11-16T15:00:00"));
    }

    @Test
    public void testFiltersInfo() throws Exception {
        mockMvc.perform(get("/api/v1/recommendations/user-filters")
                .contentType(APPLICATION_JSON_UTF8))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].field").value("MSKU"))
            .andExpect(jsonPath("$[0].dataType").value("LONG"))
            .andExpect(jsonPath("$[0].label").value("MSKU"))
            .andExpect(jsonPath("$[0].predicates[0].predicate").value("EQUAL"))
            .andExpect(jsonPath("$[0].predicates[0].label").value("="));
    }


    @Test
    @DbUnitDataSet(
        before = "RecommendationControllerTest.ass_adjRecommendations_oneSsku.before.csv",
        after = "RecommendationControllerTest.ass_adjQtySummaryRecommendations_oneSsku.after.csv"
    )
    public void adjQtyAssRecommendations_adjOneSsku() throws Exception {
        List<AdjustedRecommendationDTO> adjustedSummaryRecommendationsDTOs = new ArrayList<>(2);

        AdjustedRecommendationDTO a = new AdjustedRecommendationDTO();
        a.setMsku(null);
        a.setAdjustedPurchQty(12);
        a.setCorrectionReason(3L);
        a.setId(-1L);
        a.setNeedsManualReview(false);
        a.setGroupId(0L);
        adjustedSummaryRecommendationsDTOs.add(a);

        AdjustedSummaryRecommendationsDTO adjustedRecommendationsDTO = new AdjustedSummaryRecommendationsDTO();
        adjustedRecommendationsDTO.setAdjustedRecommendations(adjustedSummaryRecommendationsDTOs);
        adjustedRecommendationsDTO.setDemandKeys(Collections.singletonList(new DemandIdentityDTO(100L, 1)));

        mockMvc.perform(put("/api/v2/recommendations/summary/adjust?demandType=TYPE_1P")
                .contentType(APPLICATION_JSON_UTF8)
                .content(dtoToString(adjustedRecommendationsDTO)))
            .andExpect(jsonPath("$").isNotEmpty())
            .andExpect(jsonPath("$.adjustedRecommendations", hasSize(3)))
            .andExpect(jsonPath("$.warningsChanged").value(true))

            .andExpect(jsonPath("$.adjustedRecommendations[?(@.id==1)].setQuantity").value(4))
            .andExpect(jsonPath("$.adjustedRecommendations[?(@.id==1)].msku").value(100))
            .andExpect(jsonPath("$.adjustedRecommendations[?(@.id==1)].correctionReason.id").value(3))
            .andExpect(jsonPath("$.adjustedRecommendations[?(@.id==1)].summaryGroupId").value(-1))

            .andExpect(jsonPath("$.adjustedRecommendations[?(@.id==2)].setQuantity").value(8))
            .andExpect(jsonPath("$.adjustedRecommendations[?(@.id==2)].msku").value(200))
            .andExpect(jsonPath("$.adjustedRecommendations[?(@.id==2)].correctionReason.id").value(3))
            .andExpect(jsonPath("$.adjustedRecommendations[?(@.id==2)].summaryGroupId").value(-1))

            .andExpect(jsonPath("$.adjustedRecommendations[?(@.id==-1)].setQuantity").value(12))
            .andExpect(jsonPath("$.adjustedRecommendations[?(@.id==-1)].summaryGroupId").value(-1));
    }

    @Test
    @DbUnitDataSet(
        before = "RecommendationControllerTest.ass_adjRecommendations_fewSskus.before.csv",
        after = "RecommendationControllerTest.ass_adjQtySummaryRecommendations_fewSskus.after.csv"
    )
    public void adjQtyAssRecommendations_adjFewSskus() throws Exception {
        List<AdjustedRecommendationDTO> adjustedSummaryRecommendationsDTOs = new ArrayList<>(2);

        AdjustedRecommendationDTO a = new AdjustedRecommendationDTO();
        a.setMsku(null);
        a.setAdjustedPurchQty(12);
        a.setCorrectionReason(3L);
        a.setId(-10L);
        a.setNeedsManualReview(false);
        a.setGroupId(0L);
        adjustedSummaryRecommendationsDTOs.add(a);

        AdjustedSummaryRecommendationsDTO adjustedRecommendationsDTO = new AdjustedSummaryRecommendationsDTO();
        adjustedRecommendationsDTO.setAdjustedRecommendations(adjustedSummaryRecommendationsDTOs);
        adjustedRecommendationsDTO.setDemandKeys(List.of(
            new DemandIdentityDTO(101L, 1),
            new DemandIdentityDTO(102L, 1)));

        mockMvc.perform(put("/api/v2/recommendations/summary/adjust?demandType=TYPE_1P")
                .contentType(APPLICATION_JSON_UTF8)
                .content(dtoToString(adjustedRecommendationsDTO)))
            .andExpect(jsonPath("$").isNotEmpty())
            .andExpect(jsonPath("$.adjustedRecommendations", hasSize(4)))
            .andExpect(jsonPath("$.warningsChanged").value(true))

            .andExpect(jsonPath("$.adjustedRecommendations[?(@.id==10)].setQuantity").value(2))
            .andExpect(jsonPath("$.adjustedRecommendations[?(@.id==10)].correctionReason.id").value(3))
            .andExpect(jsonPath("$.adjustedRecommendations[?(@.id==10)].summaryGroupId").value(-10))

            .andExpect(jsonPath("$.adjustedRecommendations[?(@.id==20)].setQuantity").value(4))
            .andExpect(jsonPath("$.adjustedRecommendations[?(@.id==20)].correctionReason.id").value(3))
            .andExpect(jsonPath("$.adjustedRecommendations[?(@.id==20)].summaryGroupId").value(-10))

            .andExpect(jsonPath("$.adjustedRecommendations[?(@.id==30)].setQuantity").value(6))
            .andExpect(jsonPath("$.adjustedRecommendations[?(@.id==30)].correctionReason.id").value(3))
            .andExpect(jsonPath("$.adjustedRecommendations[?(@.id==30)].summaryGroupId").value(-10))

            .andExpect(jsonPath("$.adjustedRecommendations[?(@.id==-10)].setQuantity").value(12))
            .andExpect(jsonPath("$.adjustedRecommendations[?(@.id==-10)].summaryGroupId").value(-10));
    }

    @Test
    @DbUnitDataSet(before = "RecommendationControllerTest.ass_adjRecommendations_oneSsku.before.csv")
    public void adjQtyAssRecommendations_adjWithWrongSummaryId_isIAmATeapot() throws Exception {
        List<AdjustedRecommendationDTO> adjustedSummaryRecommendationsDTOs = new ArrayList<>(2);

        AdjustedRecommendationDTO a = new AdjustedRecommendationDTO();
        a.setMsku(null);
        a.setAdjustedPurchQty(12);
        a.setCorrectionReason(3L);
        a.setId(1L);
        a.setNeedsManualReview(false);
        a.setGroupId(0L);
        adjustedSummaryRecommendationsDTOs.add(a);

        AdjustedSummaryRecommendationsDTO adjustedRecommendationsDTO = new AdjustedSummaryRecommendationsDTO();
        adjustedRecommendationsDTO.setAdjustedRecommendations(adjustedSummaryRecommendationsDTOs);
        adjustedRecommendationsDTO.setDemandKeys(Collections.singletonList(new DemandIdentityDTO(1L, 1)));

        mockMvc.perform(put("/api/v2/recommendations/summary/adjust?demandType=TYPE_1P")
                .contentType(APPLICATION_JSON_UTF8)
                .content(dtoToString(adjustedRecommendationsDTO)))
            .andExpect(status().isIAmATeapot())
            .andExpect(jsonPath("$.message").value("Recommendations could not be found for adjustment parameters"));
    }

    @Test
    @DbUnitDataSet(before = "RecommendationControllerTest_getRecommendationHistoryTest.before.csv")
    public void getRecommendationHistoryTest() throws Exception {
        mockMvc.perform(get("/api/v1/recommendation/1/history")
                .contentType(APPLICATION_JSON_UTF8))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)));
    }

    /*
    @Test
    @DbUnitDataSet(before = "RecommendationControllerTest.sentToOW.before.csv")
    public void testSentToOW() throws Exception {
        // Startrack
        ArgumentCaptor<String> stQueueNameCapture = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> stTitleCapture = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> stContentsCapture = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<List<String>> stTagsCapture = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<Set<Long>> stComponentsCapture = ArgumentCaptor.forClass(Set.class);

        when(starTrekClient.createApprovalTask(
            stQueueNameCapture.capture(),
            stTitleCapture.capture(),
            stContentsCapture.capture(),
            stTagsCapture.capture(),
            stComponentsCapture.capture()
        )).thenReturn(new Issue(
            null,
            null,
            "FAKEQUEUE-1337",
            null,
            0,
            (MapF<String, Object>) new HashMap<String, Object>(),
            null
        ));

        doAnswer(invocation -> {
            final String queue = invocation.getArgument(0);
            assertNotNull(queue);
            assertEquals("CMSUP", queue);

            final String title = invocation.getArgument(1);
            assertNotNull(title);
            assertEquals("[Типовой] Перевести товар в статус INACTIVE_TMP", title);

            final Object contents = invocation.getArgument(2);
            assertTrue(contents instanceof String);
            assertEquals("", contents);

            final List<String> tags = invocation.getArgument(3);
            assertNotNull(tags);
            assertEquals(2, tags.size());
            assertThat(tags, contains("typical", "inactivetmp"));

            final Set<Long> components = invocation.getArgument(4);
            assertNotNull(components);
            assertEquals(1, components.size());
            assertEquals(components, contains(106230L));

            return new Issue(
                null,
                null,
                "FAKEQUEUE-1337",
                null,
                0,
                (MapF<String, Object>) new HashMap<String, Object>(),
                null
            );
        }).when(starTrekClient).createApprovalTask(anyString(), anyString(), anyString(), anyList(), anyList());


        // It doesn't work with void methods the other way round
        doAnswer(invocation -> {
            final String issueKey = invocation.getArgument(0);
            assertNotNull(issueKey);
            assertEquals("FAKEQUEUE-1337", issueKey);

            final String contents = invocation.getArgument(1);
            assertNotNull(contents);
            assertEquals(
                "Пожалуйста, подтвердите в комментарии перевод товаров в статус Inactive на указанный период.",
                contents
            );

            final Set<String> groups = invocation.getArgument(2);
            assertNotNull(groups);
            assertEquals(1, groups.size());
            assertThat(groups, contains("svc_deepmind_support"));

            final List<String> approvers = invocation.getArgument(3);
            assertNotNull(approvers);
            assertEquals(2, approvers.size());
            assertThat(approvers, contains("ivan-mironov"));

            return null;
        }).when(pushOkClient).createApproval(anyString(), anyString(), anySet(), anyList());

        doAnswer(invocation -> {
            final String issueKey = invocation.getArgument(0);
            assertNotNull(issueKey);
            assertEquals("FAKEQUEUE-1337", issueKey);

            final String queue = invocation.getArgument(1);
            assertNotNull(queue);
            assertEquals("service@157590284", queue);

            final Supplier supplier = invocation.getArgument(2);
            assertNotNull(supplier);
            assertEquals(1L, (long) supplier.getId());
            assertEquals("1", supplier.getName());

            final String title = invocation.getArgument(3);
            assertNotNull(title);
            assertEquals("Задача на вывод позиции из ассортимента - FAKEQUEUE-1337", title);

            final LocalDateTime deadline = invocation.getArgument(4);
            assertNotNull(deadline);
            assertEquals(LocalDateTime.of(2020, 12, 29, 0, 0), deadline);

            final String categoryKey = invocation.getArgument(5);
            assertNotNull(categoryKey);
            assertEquals("", categoryKey);

            final String responsible = invocation.getArgument(6);
            assertNotNull(responsible);
            assertEquals("boris", responsible);

            final String tag = invocation.getArgument(7);
            assertNotNull(tag);
            assertEquals("", tag);

            return "ticket@1337";
        }).when(liluCRMClient)
            .createB2BLead(anyString(), anyString(), any(), anyString(), any(), anyString(), anyString(), anyString());

        SentToOWInfoDTO sentToOWInfoDTO = new SentToOWInfoDTO(
            "кушать хочется",
            List.of(1L, 2L, 3L)
        );

        mockMvc.perform(post("/api/v1/recommendations/sent-to-ow")
                .contentType(APPLICATION_JSON_UTF8)
                .content(dtoToString(sentToOWInfoDTO))
            ).andExpect(status().isIAmATeapot())
            .andExpect(content().string("ticket@1337"));

        assertEquals("CMSUP", stQueueNameCapture.getValue());
    }
    */

    private static final int ACTUAL_SUMMARY_SALES = 8;

    private void testSummary(
        FilterType filter,
        int filterIndex,
        int[] salesAll,
        int[] sales1p,
        int[] totalSalesAll,
        int[] totalSales1p
    ) throws Exception {

        IntegerCapturingMatcher summaryGroupCapturer = new IntegerCapturingMatcher();
        IntegerCapturingMatcher child1GroupCapturer = new IntegerCapturingMatcher();
        IntegerCapturingMatcher child2GroupCapturer = new IntegerCapturingMatcher();

        mockMvc.perform(post("/api/v2/recommendations/with-count")
                .contentType(APPLICATION_JSON_UTF8)
                .content(dtoToString(createFilters(filter, 1L, 2L)))
            )
            .andExpect(status().isOk())

            .andExpect(jsonPath("$.count.filter[0].id").value("ALL"))
            .andExpect(jsonPath("$.count.filter[0].count").value(3))
            .andExpect(jsonPath("$.count.filter[" + filterIndex + "].id").value(filter + ""))
            .andExpect(jsonPath("$.count.filter[" + filterIndex + "].count").value(2))

            .andExpect(jsonPath("$.recommendations.length()").value(3))

            .andExpect(jsonPath("$.recommendations[0].msku").value(100L))
            .andExpect(jsonPath("$.recommendations[0].summaryGroupId").value(summaryGroupCapturer))
            .andExpect(jsonPath("$.recommendations[0].warehouseId").value(171))
            .andExpect(jsonPath("$.recommendations[0].salesAll[0]").value(salesAll[0]))
            .andExpect(jsonPath("$.recommendations[0].salesAll[1]").value(salesAll[1]))
            .andExpect(jsonPath("$.recommendations[0].salesAll[2]").value(salesAll[2]))
            .andExpect(jsonPath("$.recommendations[0].salesAll[3]").value(salesAll[3]))
            .andExpect(jsonPath("$.recommendations[0].salesAll[4]").value(salesAll[4]))
            .andExpect(jsonPath("$.recommendations[0].salesAll[5]").value(salesAll[5]))
            .andExpect(jsonPath("$.recommendations[0].salesAll[6]").value(salesAll[6]))
            .andExpect(jsonPath("$.recommendations[0].salesAll[7]").value(salesAll[7]))
            .andExpect(jsonPath("$.recommendations[0].sales1p[0]").value(sales1p[0]))
            .andExpect(jsonPath("$.recommendations[0].sales1p[1]").value(sales1p[1]))
            .andExpect(jsonPath("$.recommendations[0].sales1p[2]").value(sales1p[2]))
            .andExpect(jsonPath("$.recommendations[0].sales1p[3]").value(sales1p[3]))
            .andExpect(jsonPath("$.recommendations[0].sales1p[4]").value(sales1p[4]))
            .andExpect(jsonPath("$.recommendations[0].sales1p[5]").value(sales1p[5]))
            .andExpect(jsonPath("$.recommendations[0].sales1p[6]").value(sales1p[6]))
            .andExpect(jsonPath("$.recommendations[0].sales1p[7]").value(sales1p[7]))
            .andExpect(jsonPath("$.recommendations[0].actualSales").value(ACTUAL_SUMMARY_SALES))
            .andExpect(jsonPath("$.recommendations[0].actualFit").value(40))
            .andExpect(jsonPath("$.recommendations[0].stock").value(2))
            .andExpect(jsonPath("$.recommendations[0].stockOverall").value(2))
            .andExpect(jsonPath("$.recommendations[0].oosDays").isEmpty())
            .andExpect(jsonPath("$.recommendations[0].missedOrders28d").isEmpty())
            .andExpect(jsonPath("$.recommendations[0].missedOrders56d").isEmpty())
            .andExpect(jsonPath("$.recommendations[0].salesForecast14days").isEmpty())
            .andExpect(jsonPath("$.recommendations[0].salesForecast28days").isEmpty())
            .andExpect(jsonPath("$.recommendations[0].salesForecast56days").isEmpty())

            .andExpect(jsonPath("$.recommendations[1].msku").value(100L))
            .andExpect(jsonPath("$.recommendations[1].summaryGroupId").value(child1GroupCapturer))
            .andExpect(jsonPath("$.recommendations[1].warehouseId").value(172))
            .andExpect(jsonPath("$.recommendations[1].salesAll[0]").value(salesAll[0]))
            .andExpect(jsonPath("$.recommendations[1].salesAll[1]").value(salesAll[1]))
            .andExpect(jsonPath("$.recommendations[1].salesAll[2]").value(salesAll[2]))
            .andExpect(jsonPath("$.recommendations[1].salesAll[3]").value(salesAll[3]))
            .andExpect(jsonPath("$.recommendations[1].salesAll[4]").value(salesAll[4]))
            .andExpect(jsonPath("$.recommendations[1].salesAll[5]").value(salesAll[5]))
            .andExpect(jsonPath("$.recommendations[1].salesAll[6]").value(salesAll[6]))
            .andExpect(jsonPath("$.recommendations[1].sales1p[0]").value(sales1p[0]))
            .andExpect(jsonPath("$.recommendations[1].sales1p[1]").value(sales1p[1]))
            .andExpect(jsonPath("$.recommendations[1].sales1p[2]").value(sales1p[2]))
            .andExpect(jsonPath("$.recommendations[1].sales1p[3]").value(sales1p[3]))
            .andExpect(jsonPath("$.recommendations[1].sales1p[4]").value(sales1p[4]))
            .andExpect(jsonPath("$.recommendations[1].sales1p[5]").value(sales1p[5]))
            .andExpect(jsonPath("$.recommendations[1].sales1p[6]").value(sales1p[6]))
            .andExpect(jsonPath("$.recommendations[1].sales1p[7]").value(sales1p[7]))
            .andExpect(jsonPath("$.recommendations[1].actualSales").value(ACTUAL_SUMMARY_SALES))
            .andExpect(jsonPath("$.recommendations[1].actualFit").value(40))
            .andExpect(jsonPath("$.recommendations[1].stock").value(1))
            .andExpect(jsonPath("$.recommendations[1].stockOverall").value(1))
            .andExpect(jsonPath("$.recommendations[1].oosDays").isEmpty())
            .andExpect(jsonPath("$.recommendations[1].missedOrders28d").isEmpty())
            .andExpect(jsonPath("$.recommendations[1].missedOrders56d").isEmpty())
            .andExpect(jsonPath("$.recommendations[1].salesForecast14days").isEmpty())
            .andExpect(jsonPath("$.recommendations[1].salesForecast28days").isEmpty())
            .andExpect(jsonPath("$.recommendations[1].salesForecast56days").isEmpty())

            .andExpect(jsonPath("$.recommendations[2].msku").value(100L))
            .andExpect(jsonPath("$.recommendations[2].summaryGroupId").value(child2GroupCapturer))
            .andExpect(jsonPath("$.recommendations[2].warehouseId").isEmpty())
            .andExpect(jsonPath("$.recommendations[2].salesAll[0]").value(totalSalesAll[0]))
            .andExpect(jsonPath("$.recommendations[2].salesAll[1]").value(totalSalesAll[1]))
            .andExpect(jsonPath("$.recommendations[2].salesAll[2]").value(totalSalesAll[2]))
            .andExpect(jsonPath("$.recommendations[2].salesAll[3]").value(totalSalesAll[3]))
            .andExpect(jsonPath("$.recommendations[2].salesAll[4]").value(totalSalesAll[4]))
            .andExpect(jsonPath("$.recommendations[2].salesAll[5]").value(totalSalesAll[5]))
            .andExpect(jsonPath("$.recommendations[2].salesAll[6]").value(totalSalesAll[6]))
            .andExpect(jsonPath("$.recommendations[2].sales1p[0]").value(totalSales1p[0]))
            .andExpect(jsonPath("$.recommendations[2].sales1p[1]").value(totalSales1p[1]))
            .andExpect(jsonPath("$.recommendations[2].sales1p[2]").value(totalSales1p[2]))
            .andExpect(jsonPath("$.recommendations[2].sales1p[3]").value(totalSales1p[3]))
            .andExpect(jsonPath("$.recommendations[2].sales1p[4]").value(totalSales1p[4]))
            .andExpect(jsonPath("$.recommendations[2].sales1p[5]").value(totalSales1p[5]))
            .andExpect(jsonPath("$.recommendations[2].sales1p[6]").value(totalSales1p[6]))
            .andExpect(jsonPath("$.recommendations[2].sales1p[7]").value(totalSales1p[7]))
            .andExpect(jsonPath("$.recommendations[2].actualSales").value(ACTUAL_SUMMARY_SALES))
            .andExpect(jsonPath("$.recommendations[2].actualFit").value(40))
            .andExpect(jsonPath("$.recommendations[2].stock").value(6))
            .andExpect(jsonPath("$.recommendations[2].stockOverall").value(6));

        assertEquals(summaryGroupCapturer.getValue(), child1GroupCapturer.getValue());
        assertEquals(summaryGroupCapturer.getValue(), child2GroupCapturer.getValue());
    }

    private void testSummaryWithDistrDemand(
        FilterType filter,
        int filterIndex,
        int[] salesAll,
        int[] sales1p,
        int[] totalSalesAll,
        int[] totalSales1p
    ) throws Exception {
        mockMvc.perform(post("/api/v2/recommendations/with-count")
                .contentType(APPLICATION_JSON_UTF8)
                .content(dtoToString(createFilters(filter, 1L, 2L)))
            )
            .andExpect(status().isOk())

            .andExpect(jsonPath("$.count.filter[0].id").value("ALL"))
            .andExpect(jsonPath("$.count.filter[0].count").value(3))
            .andExpect(jsonPath("$.count.filter[" + filterIndex + "].id").value(filter + ""))
            .andExpect(jsonPath("$.count.filter[" + filterIndex + "].count").value(2))

            .andExpect(jsonPath("$.recommendations.length()").value(3))

            .andExpect(jsonPath("$.recommendations[0].msku").value(100L))
            .andExpect(jsonPath("$.recommendations[0].warehouseId").value(171))
            .andExpect(jsonPath("$.recommendations[0].salesAll[0]").value(salesAll[0]))
            .andExpect(jsonPath("$.recommendations[0].salesAll[1]").value(salesAll[1]))
            .andExpect(jsonPath("$.recommendations[0].salesAll[2]").value(salesAll[2]))
            .andExpect(jsonPath("$.recommendations[0].salesAll[3]").value(salesAll[3]))
            .andExpect(jsonPath("$.recommendations[0].salesAll[4]").value(salesAll[4]))
            .andExpect(jsonPath("$.recommendations[0].salesAll[5]").value(salesAll[5]))
            .andExpect(jsonPath("$.recommendations[0].salesAll[6]").value(salesAll[6]))
            .andExpect(jsonPath("$.recommendations[0].salesAll[7]").value(salesAll[7]))
            .andExpect(jsonPath("$.recommendations[0].sales1p[0]").value(sales1p[0]))
            .andExpect(jsonPath("$.recommendations[0].sales1p[1]").value(sales1p[1]))
            .andExpect(jsonPath("$.recommendations[0].sales1p[2]").value(sales1p[2]))
            .andExpect(jsonPath("$.recommendations[0].sales1p[3]").value(sales1p[3]))
            .andExpect(jsonPath("$.recommendations[0].sales1p[4]").value(sales1p[4]))
            .andExpect(jsonPath("$.recommendations[0].sales1p[5]").value(sales1p[5]))
            .andExpect(jsonPath("$.recommendations[0].sales1p[6]").value(sales1p[6]))
            .andExpect(jsonPath("$.recommendations[0].sales1p[7]").value(sales1p[7]))
            .andExpect(jsonPath("$.recommendations[0].actualSales").value(ACTUAL_SUMMARY_SALES))
            .andExpect(jsonPath("$.recommendations[0].actualFit").value(40))
            .andExpect(jsonPath("$.recommendations[0].stock").value(2))
            .andExpect(jsonPath("$.recommendations[0].stockOverall").value(2))
            .andExpect(jsonPath("$.recommendations[0].oosDays").isEmpty())
            .andExpect(jsonPath("$.recommendations[0].missedOrders28d").isEmpty())
            .andExpect(jsonPath("$.recommendations[0].missedOrders56d").isEmpty())
            .andExpect(jsonPath("$.recommendations[0].salesForecast14days").isEmpty())
            .andExpect(jsonPath("$.recommendations[0].salesForecast28days").isEmpty())
            .andExpect(jsonPath("$.recommendations[0].salesForecast56days").isEmpty())

            .andExpect(jsonPath("$.recommendations[1].msku").value(100L))
            .andExpect(jsonPath("$.recommendations[1].warehouseId").value(172))
            .andExpect(jsonPath("$.recommendations[1].salesAll[0]").value(salesAll[0]))
            .andExpect(jsonPath("$.recommendations[1].salesAll[1]").value(salesAll[1]))
            .andExpect(jsonPath("$.recommendations[1].salesAll[2]").value(salesAll[2]))
            .andExpect(jsonPath("$.recommendations[1].salesAll[3]").value(salesAll[3]))
            .andExpect(jsonPath("$.recommendations[1].salesAll[4]").value(salesAll[4]))
            .andExpect(jsonPath("$.recommendations[1].salesAll[5]").value(salesAll[5]))
            .andExpect(jsonPath("$.recommendations[1].salesAll[6]").value(salesAll[6]))
            .andExpect(jsonPath("$.recommendations[1].sales1p[0]").value(sales1p[0]))
            .andExpect(jsonPath("$.recommendations[1].sales1p[1]").value(sales1p[1]))
            .andExpect(jsonPath("$.recommendations[1].sales1p[2]").value(sales1p[2]))
            .andExpect(jsonPath("$.recommendations[1].sales1p[3]").value(sales1p[3]))
            .andExpect(jsonPath("$.recommendations[1].sales1p[4]").value(sales1p[4]))
            .andExpect(jsonPath("$.recommendations[1].sales1p[5]").value(sales1p[5]))
            .andExpect(jsonPath("$.recommendations[1].sales1p[6]").value(sales1p[6]))
            .andExpect(jsonPath("$.recommendations[1].sales1p[7]").value(sales1p[7]))
            .andExpect(jsonPath("$.recommendations[1].actualSales").value(ACTUAL_SUMMARY_SALES))
            .andExpect(jsonPath("$.recommendations[1].actualFit").value(40))
            .andExpect(jsonPath("$.recommendations[1].stock").value(5))
            .andExpect(jsonPath("$.recommendations[1].stockOverall").value(10))
            .andExpect(jsonPath("$.recommendations[1].oosDays").isEmpty())
            .andExpect(jsonPath("$.recommendations[1].missedOrders28d").isEmpty())
            .andExpect(jsonPath("$.recommendations[1].missedOrders56d").isEmpty())
            .andExpect(jsonPath("$.recommendations[1].salesForecast14days").isEmpty())
            .andExpect(jsonPath("$.recommendations[1].salesForecast28days").isEmpty())
            .andExpect(jsonPath("$.recommendations[1].salesForecast56days").isEmpty())

            .andExpect(jsonPath("$.recommendations[2].msku").value(100L))
            .andExpect(jsonPath("$.recommendations[2].warehouseId").isEmpty())
            .andExpect(jsonPath("$.recommendations[2].salesAll[0]").value(totalSalesAll[0]))
            .andExpect(jsonPath("$.recommendations[2].salesAll[1]").value(totalSalesAll[1]))
            .andExpect(jsonPath("$.recommendations[2].salesAll[2]").value(totalSalesAll[2]))
            .andExpect(jsonPath("$.recommendations[2].salesAll[3]").value(totalSalesAll[3]))
            .andExpect(jsonPath("$.recommendations[2].salesAll[4]").value(totalSalesAll[4]))
            .andExpect(jsonPath("$.recommendations[2].salesAll[5]").value(totalSalesAll[5]))
            .andExpect(jsonPath("$.recommendations[2].salesAll[6]").value(totalSalesAll[6]))
            .andExpect(jsonPath("$.recommendations[2].sales1p[0]").value(totalSales1p[0]))
            .andExpect(jsonPath("$.recommendations[2].sales1p[1]").value(totalSales1p[1]))
            .andExpect(jsonPath("$.recommendations[2].sales1p[2]").value(totalSales1p[2]))
            .andExpect(jsonPath("$.recommendations[2].sales1p[3]").value(totalSales1p[3]))
            .andExpect(jsonPath("$.recommendations[2].sales1p[4]").value(totalSales1p[4]))
            .andExpect(jsonPath("$.recommendations[2].sales1p[5]").value(totalSales1p[5]))
            .andExpect(jsonPath("$.recommendations[2].sales1p[6]").value(totalSales1p[6]))
            .andExpect(jsonPath("$.recommendations[2].sales1p[7]").value(totalSales1p[7]))
            .andExpect(jsonPath("$.recommendations[2].actualSales").value(ACTUAL_SUMMARY_SALES))
            .andExpect(jsonPath("$.recommendations[2].actualFit").value(40))
            .andExpect(jsonPath("$.recommendations[2].stock").value(5))
            .andExpect(jsonPath("$.recommendations[2].stockOverall").value(10));
    }

    private static final int ACTUAL_COMMON_SALES = 16;

    //todo 2156
    @NotNull
    private CompoziteResultMatcher commonMatcher(int index) {
        return new CompoziteResultMatcher(
            jsonPath("$.recommendations[" + index + "].demandId").value(1L),
            jsonPath("$.recommendations[" + index + "].abc").value(ABC.NEW.toString()),
            jsonPath("$.recommendations[" + index + "].warehouseId").value(WAREHOUSE_ID),
            jsonPath("$.recommendations[" + index + "].categoryName").value("category_1"),
            jsonPath("$.recommendations[" + index + "].subCategoryName").value("all"),
            jsonPath("$.recommendations[" + index + "].subSubCategoryName").value("parent_all"),
            jsonPath("$.recommendations[" + index + "].price").value(100.0),
            jsonPath("$.recommendations[" + index + "].regularPrice").value(200.0),
            jsonPath("$.recommendations[" + index + "].purchasePrice").value(0.00),
            // fix new year
            jsonPath("$.recommendations[" + index + "].barcode").value("123, 321"),
            jsonPath("$.recommendations[" + index + "].correctionReason.id").value(1L),
            jsonPath("$.recommendations[" + index + "].correctionReason.name").value("1. Не согласен с прогнозом"),
            jsonPath("$.recommendations[" + index + "].length").value(1L),
            jsonPath("$.recommendations[" + index + "].width").value(2L),
            jsonPath("$.recommendations[" + index + "].height").value(3L),
            jsonPath("$.recommendations[" + index + "].weight").value(4L),
            jsonPath("$.recommendations[" + index + "].actualSales").value(ACTUAL_COMMON_SALES),
            jsonPath("$.recommendations[" + index + "].stock").value(64),
            jsonPath("$.recommendations[" + index + "].actualFit").value(5L),
            jsonPath("$.recommendations[" + index + "].purchaseQuantity").value(9),
            jsonPath("$.recommendations[" + index + "].transit").value(7),
            jsonPath("$.recommendations[" + index + "].setQuantity").value(23),
            jsonPath("$.recommendations[" + index + "].salesForecast14days").value(24.),
            jsonPath("$.recommendations[" + index + "].salesForecast28days").value(25.),
            jsonPath("$.recommendations[" + index + "].salesForecast56days").value(26.),
            jsonPath("$.recommendations[" + index + "].sales1p[0]").value(10),
            jsonPath("$.recommendations[" + index + "].sales1p[1]").value(11),
            jsonPath("$.recommendations[" + index + "].sales1p[2]").value(12),
            jsonPath("$.recommendations[" + index + "].sales1p[3]").value(13),
            jsonPath("$.recommendations[" + index + "].sales1p[4]").value(14),
            jsonPath("$.recommendations[" + index + "].sales1p[5]").value(15),
            jsonPath("$.recommendations[" + index + "].sales1p[6]").value(16),
            jsonPath("$.recommendations[" + index + "].sales1p[7]").value(17),
            jsonPath("$.recommendations[" + index + "].salesAll[0]").value(15),
            jsonPath("$.recommendations[" + index + "].salesAll[1]").value(16),
            jsonPath("$.recommendations[" + index + "].salesAll[2]").value(17),
            jsonPath("$.recommendations[" + index + "].salesAll[3]").value(18),
            jsonPath("$.recommendations[" + index + "].salesAll[4]").value(19),
            jsonPath("$.recommendations[" + index + "].salesAll[5]").value(20),
            jsonPath("$.recommendations[" + index + "].salesAll[6]").value(21),
            jsonPath("$.recommendations[" + index + "].salesAll[7]").value(22),
            jsonPath("$.recommendations[" + index + "].hasCheaperRecommendation").value(false),
            jsonPath("$.recommendations[" + index + "].purchasePromoStart").value("2020-07-01"),
            jsonPath("$.recommendations[" + index + "].purchasePromoEnd").value("2020-07-30")
        );
    }

    private void createReplenishmentResult() {
        recommendationService.exportRecommendationsForDemandId(DemandType.TYPE_1P, 1, "boris");
    }
}
