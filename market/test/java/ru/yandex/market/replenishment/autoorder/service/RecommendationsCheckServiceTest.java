package ru.yandex.market.replenishment.autoorder.service;

import java.time.LocalDateTime;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.replenishment.autoorder.config.FunctionalTest;
import ru.yandex.market.replenishment.autoorder.model.DemandType;

public class RecommendationsCheckServiceTest extends FunctionalTest {

    @Autowired
    private RecommendationsCheckService recommendationsCheckService;

    @Before
    public void mockMethods() {
        setTestTime(LocalDateTime.of(2021, 6, 4, 0, 0));
    }

    @Test
    @DbUnitDataSet(before = "RecommendationsCheckServiceTest.before.csv",
            after = "RecommendationsCheckServiceTest.after.csv")
    public void testPushAlertIfNotCorrectDemandExist() {
        recommendationsCheckService.checkRecommendationsCorrectness(DemandType.TYPE_1P);
        recommendationsCheckService.checkRecommendationsCorrectness(DemandType.TYPE_3P);
    }

    @Test
    @DbUnitDataSet(before = "RecommendationsCheckServiceTestCorrect.before.csv",
            after = "RecommendationsCheckServiceTestCorrect.after.csv")
    public void testPushAlertIfAllDemandCorrect() {
        recommendationsCheckService.checkRecommendationsCorrectness(DemandType.TYPE_1P);
        recommendationsCheckService.checkRecommendationsCorrectness(DemandType.TYPE_3P);
    }
}
