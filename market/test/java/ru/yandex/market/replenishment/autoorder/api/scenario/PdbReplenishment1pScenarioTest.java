package ru.yandex.market.replenishment.autoorder.api.scenario;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import com.sun.istack.NotNull;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.replenishment.autoorder.config.ControllerTest;
import ru.yandex.market.replenishment.autoorder.model.dto.AdjustedRecommendationDTO;
import ru.yandex.market.replenishment.autoorder.model.dto.AdjustedRecommendationsDTO;
import ru.yandex.market.replenishment.autoorder.model.dto.DemandIdentityDTO;
import ru.yandex.market.replenishment.autoorder.security.WithMockLogin;
import ru.yandex.market.replenishment.autoorder.service.PdbReplenishmentService;
import ru.yandex.market.replenishment.autoorder.utils.TestUtils;

import static org.springframework.http.MediaType.APPLICATION_JSON_UTF8;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.replenishment.autoorder.utils.TestUtils.dtoToString;
@WithMockLogin
public class PdbReplenishment1pScenarioTest extends ControllerTest {
    @Autowired
    TransactionTemplate transactionTemplate;

    @Autowired
    PdbReplenishmentService pdbReplenishmentService;

    private static final long DEMAND_ID = 101L;
    private static final int DEMAND_VERSION = 1;

    @Before
    public void prepareMocks() {
        setTestTime(LocalDateTime.of(2019, 8, 10, 8, 7));
    }

    @Test
    @DbUnitDataSet(
            before = "PdbReplenishment1pScenarioTest.exportToPdb_recommendations.before.csv",
            after = "PdbReplenishment1pScenarioTest.exportToPdb_recommendations.after.csv")
    @DbUnitDataSet(
            dataSource = "pdbDataSource",
            after = "PdbReplenishment1pScenarioTest.exportToPdb_recommendations.pdb.after.csv")
    public void testExportToPdb_recommendations() throws Exception {
        //экспортируем потребности
        mockMvc.perform(post("/api/v2/demands/export?demandType=TYPE_1P")
                .content(TestUtils.dtoToString(
                        Collections.singletonList(new DemandIdentityDTO(DEMAND_ID, DEMAND_VERSION))))
                .contentType(APPLICATION_JSON_UTF8))
                .andExpect(status().isOk());

        //экспорт в pdb
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(@NotNull TransactionStatus status) {
                pdbReplenishmentService.exportToPdb();
            }
        });
    }

    @Test
    @DbUnitDataSet(
            before = "PdbReplenishment1pScenarioTest.testExportToPdb_recommendationsWithSubSsku.before.csv",
            after = "PdbReplenishment1pScenarioTest.exportToPdb_recommendationsWithSubSsku.after.csv")
    @DbUnitDataSet(
            dataSource = "pdbDataSource",
            after = "PdbReplenishment1pScenarioTest.exportToPdb_recommendationsWithSubSsku.pdb.after.csv")
    public void testExportToPdb_recommendationsWithSubSsku() throws Exception {
        //экспортируем потребности
        mockMvc.perform(post("/api/v2/demands/export?demandType=TYPE_1P")
                .content(TestUtils.dtoToString(
                        Collections.singletonList(new DemandIdentityDTO(DEMAND_ID, DEMAND_VERSION))))
                .contentType(APPLICATION_JSON_UTF8))
                .andExpect(status().isOk());

        //экспорт в pdb
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(@NotNull TransactionStatus status) {
                pdbReplenishmentService.exportToPdb();
            }
        });
    }

    @Test
    @DbUnitDataSet(
            before = "PdbReplenishment1pScenarioTest.exportToPdb_recommendations.before.csv",
            after = "PdbReplenishment1pScenarioTest.exportToPdb_adjustedRecommendations.after.csv")
    @DbUnitDataSet(
            dataSource = "pdbDataSource",
            after = "PdbReplenishment1pScenarioTest.exportToPdb_adjustedRecommendations.pdb.after.csv")
    public void testExportToPdb_adjustedRecommendations() throws Exception {
        //редактируем рекомендации
        AdjustedRecommendationsDTO adjustedRecommendationsDTO = new AdjustedRecommendationsDTO();
        List<AdjustedRecommendationDTO> adjustedRecommendations = List.of(
                AdjustedRecommendationDTO.builder().id(1001L).msku(100L).adjustedPurchQty(5).correctionReason(1L).build(),
                AdjustedRecommendationDTO.builder().id(1005L).msku(500L).adjustedPurchQty(3).correctionReason(1L).build());
        adjustedRecommendationsDTO.setAdjustedRecommendations(adjustedRecommendations);
        adjustedRecommendationsDTO.setDemandId(DEMAND_ID);
        adjustedRecommendationsDTO.setDemandVersion(DEMAND_VERSION);
        mockMvc.perform(put("/api/v1/recommendations/adjust?demandType=TYPE_1P")
                .contentType(APPLICATION_JSON_UTF8)
                .content(dtoToString(adjustedRecommendationsDTO)))
                .andExpect(status().isOk());

        //экспортируем потребности
        mockMvc.perform(post("/api/v2/demands/export?demandType=TYPE_1P")
                .content(TestUtils.dtoToString(
                        Collections.singletonList(new DemandIdentityDTO(DEMAND_ID, DEMAND_VERSION))))
                .contentType(APPLICATION_JSON_UTF8))
                .andExpect(status().isOk());

        //экспорт в pdb
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(@NotNull TransactionStatus status) {
                pdbReplenishmentService.exportToPdb();
            }
        });
    }

    @Test
    @DbUnitDataSet(
            before = "PdbReplenishment1pScenarioTest.exportToPdb_recommendations.before.csv",
            after = "PdbReplenishment1pScenarioTest.exportToPdb_adjustedAndSplittedRecommendations.after.csv")
    @DbUnitDataSet(
            dataSource = "pdbDataSource",
            after = "PdbReplenishment1pScenarioTest.exportToPdb_adjustedAndSplittedRecommendations.pdb.after.csv")
    public void testExportToPdb_adjustedAndSplittedRecommendations() throws Exception {
        //редактируем рекомендации
        AdjustedRecommendationsDTO adjustedRecommendationsDTO = new AdjustedRecommendationsDTO();
        List<AdjustedRecommendationDTO> adjustedRecommendations = List.of(
                AdjustedRecommendationDTO.builder().id(1001L).adjustedPurchQty(5).groupId(1L).correctionReason(1L).build(),
                AdjustedRecommendationDTO.builder().id(1005L).adjustedPurchQty(3).groupId(1L).correctionReason(1L).build());
        adjustedRecommendationsDTO.setAdjustedRecommendations(adjustedRecommendations);
        adjustedRecommendationsDTO.setDemandId(DEMAND_ID);
        adjustedRecommendationsDTO.setDemandVersion(DEMAND_VERSION);
        mockMvc.perform(put("/api/v1/recommendations/adjust?demandType=TYPE_1P").contentType(APPLICATION_JSON_UTF8)
                .content(dtoToString(adjustedRecommendationsDTO)))
                .andExpect(status().isOk());


        //разбиваем на группы
        mockMvc.perform(post("/api/v1/demands/" + DEMAND_ID + "/split-by-groups")
                .contentType(APPLICATION_JSON_UTF8))
                .andExpect(status().isOk());

        //экспортируем потребности
        mockMvc.perform(post("/api/v2/demands/export?demandType=TYPE_1P")
                .content(TestUtils.dtoToString(
                        Collections.singletonList(new DemandIdentityDTO(DEMAND_ID, DEMAND_VERSION))))
                .contentType(APPLICATION_JSON_UTF8))
                .andExpect(status().isOk());

        //экспорт в pdb
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(@NotNull TransactionStatus status) {
                pdbReplenishmentService.exportToPdb();
            }
        });
    }
}
