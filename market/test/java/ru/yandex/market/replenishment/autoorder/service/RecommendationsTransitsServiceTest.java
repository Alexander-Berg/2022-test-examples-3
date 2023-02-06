package ru.yandex.market.replenishment.autoorder.service;

import java.util.List;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.replenishment.autoorder.config.FunctionalTest;
import ru.yandex.market.replenishment.autoorder.model.DemandType;
import ru.yandex.market.replenishment.autoorder.model.RecommendationNew;
import ru.yandex.market.replenishment.autoorder.model.entity.postgres.RecommendationRegionInfo;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
@ActiveProfiles("unittest")
public class RecommendationsTransitsServiceTest extends FunctionalTest {

    @Autowired
    RecommendationsTransitsService recommendationsTransitsService;

    @Test
    @DbUnitDataSet(before = "RecommendationsTransitsServiceTest.transits.csv")
    public void testSetTransits() {
        var input = List.of(
                createRecommendationNew(123, 111, DemandType.TYPE_1P, 1212),
                createRecommendationNew(12345, 145, DemandType.TYPE_1P, 1)
        );

        recommendationsTransitsService.setTransits(input);

        assertAll(
                () -> assertNotNull(input),
                () -> assertEquals(2, input.size())
        );

        var filledRecommendation = input.get(1);
        var transits = filledRecommendation.getTransits();
        var tomorrow = timeService.getNowDate().plusDays(1L);
        var afterTomorrow = timeService.getNowDate().plusDays(2L);

        assertAll(
                () -> assertEquals(20, transits.get(tomorrow)),
                () -> assertEquals(10, transits.get(afterTomorrow))
        );
    }

    private RecommendationNew createRecommendationNew(long msku, int regionId, DemandType demandType,
                                                      long demandId) {
        var r = new RecommendationNew();
        r.setMsku(msku);
        r.setDemandType(demandType);
        r.setDemandId(demandId);
        r.setRegionInfo(new RecommendationRegionInfo());
        r.getRegionInfo().setRegionId(regionId);
        return r;
    }
}
