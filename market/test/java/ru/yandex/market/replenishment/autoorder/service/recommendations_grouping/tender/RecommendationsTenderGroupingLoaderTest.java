package ru.yandex.market.replenishment.autoorder.service.recommendations_grouping.tender;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.replenishment.autoorder.model.DemandType;
import ru.yandex.market.replenishment.autoorder.service.not_grouped_recommendations_processing_service.AbstractGroupingLoader;
import ru.yandex.market.replenishment.autoorder.service.not_grouped_recommendations_processing_service.RecommendationsTenderGroupingLoader;
import ru.yandex.market.replenishment.autoorder.service.recommendations_grouping.BaseGroupingTest;
public class RecommendationsTenderGroupingLoaderTest extends BaseGroupingTest {

    @Autowired
    RecommendationsTenderGroupingLoader loader;

    @Test
    @DbUnitDataSet(before = "RecommendationsTenderGroupingLoaderTest.tender.before.csv",
            after = "RecommendationsTenderGroupingLoaderTest.tender.after.csv")
    public void testSimpleMappingAndDemandCreation_tender() {
        loader.load();
    }

    @Override
    protected AbstractGroupingLoader getLoader() {
        return loader;
    }

    @Override
    protected DemandType getDemandType() {
        return DemandType.TENDER;
    }
}
