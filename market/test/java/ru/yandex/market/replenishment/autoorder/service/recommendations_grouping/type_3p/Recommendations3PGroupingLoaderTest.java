package ru.yandex.market.replenishment.autoorder.service.recommendations_grouping.type_3p;

import java.time.LocalDate;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.replenishment.autoorder.model.DemandType;
import ru.yandex.market.replenishment.autoorder.model.RecommendationNew;
import ru.yandex.market.replenishment.autoorder.model.dto.DemandDTO;
import ru.yandex.market.replenishment.autoorder.service.not_grouped_recommendations_processing_service.AbstractGroupingLoader;
import ru.yandex.market.replenishment.autoorder.service.not_grouped_recommendations_processing_service.Recommendations3PGroupingLoader;
import ru.yandex.market.replenishment.autoorder.service.recommendations_grouping.BaseGroupingTest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static ru.yandex.market.replenishment.autoorder.model.DemandType.TYPE_3P;
import static ru.yandex.market.replenishment.autoorder.model.entity.postgres.ImportEvents.STOCKS_WITH_LIFETIMES_LOAD;
import static ru.yandex.market.replenishment.autoorder.model.entity.postgres.ImportEvents.TRANSITS_LOAD;
import static ru.yandex.market.replenishment.autoorder.model.entity.postgres.ImportEvents.addSuffix;
import static ru.yandex.market.replenishment.autoorder.model.entity.postgres.ImportEvents.completed;

public class Recommendations3PGroupingLoaderTest extends BaseGroupingTest {

    @Autowired
    Recommendations3PGroupingLoader loader;

    @Test
    @DbUnitDataSet(before = "Recommendations3PGroupingLoaderTest.simple.before.csv",
            after = "Recommendations3PGroupingLoaderTest.simple.after.csv")
    public void testSimpleMappingAndDemandCreation() {
        getLoader().load();
    }

    @Test
    @DbUnitDataSet(before = "Recommendations3PGroupingLoaderTest.split.before.csv",
            after = "Recommendations3PGroupingLoaderTest.split.after.csv")
    public void testMappingAndDemandCreationWithSplitting() {
        getLoader().load();
    }

    @Test
    @DbUnitDataSet(before = "Recommendations3PGroupingLoaderTest.different_checks.before.csv")
    public void testSameDemands() {
        createNotGrouped(1, 500L, 123, TOMILINO, null, 1, LocalDate.now());
        createNotGrouped(2, 500L, 234, TOMILINO, null, 1, LocalDate.now());
        test(true, false);
        test3PDemands(true);
    }

    @Test
    @DbUnitDataSet(before = "Recommendations3PGroupingLoaderTest.different_checks.before.csv")
    public void testDiffDates() {
        createNotGrouped(1, 500L, 123, TOMILINO, null, 1, LocalDate.now());
        createNotGrouped(2, 500L, 123, TOMILINO, null, 1, LocalDate.now().plusDays(8));
        test(false, false);
        test3PDemands(false);
    }

    @Test
    @DbUnitDataSet(before = "Recommendations3PGroupingLoaderTest.different_checks.before.csv")
    public void testDifferentWhsButOneRegion() {
        createNotGrouped(1, 500L, 123, TOMILINO, null, 1, LocalDate.now());
        createNotGrouped(2, 500L, 123, SOFYINO, null, 1, LocalDate.now());
        test(false, false);
        test3PDemands(true);
    }

    @Test
    @DbUnitDataSet(before = "Recommendations3PGroupingLoaderTest.different_checks.before.csv")
    public void testDifferentSuppliers() {
        createNotGrouped(1, 500L, 123, TOMILINO, null, 1, LocalDate.now());
        createNotGrouped(2, 600L, 234, TOMILINO, null, 1, LocalDate.now());
        test(false, false);
        test3PDemands(true);
    }

    @Test
    @DbUnitDataSet(before = "Recommendations3PGroupingLoaderTest.different_checks.before.csv")
    public void testRostov() {
        createNotGrouped(1, 500L, 123, ROSTOV, null, 1, LocalDate.now());
        createNotGrouped(2, 500L, 123, TOMILINO, null, 1, LocalDate.now());
        test(false, false);
        test3PDemands(false);
    }

    @Test
    @DbUnitDataSet(before = "Recommendations3PGroupingLoaderTest.different_checks.before.csv")
    public void testDifferentResponsibles() {
        createNotGrouped(1, 500L, 123, TOMILINO, null, 1, LocalDate.now());
        createNotGrouped(2, 500L, 123, TOMILINO, null, 2, LocalDate.now());
        test(false, false);
        test3PDemands(false);
    }

    @Test
    @DbUnitDataSet(before = "Recommendations3PGroupingLoaderTest.different_checks.before.csv")
    public void testDifferentSupplyRoute() {
        createNotGrouped(1, 500L, 123, ROSTOV, null, 1, LocalDate.now());
        createNotGrouped(2, 500L, 123, ROSTOV, XDOC, 1, LocalDate.now());
        test(false, false);
        test3PDemands(true);
    }

    @Override
    protected AbstractGroupingLoader getLoader() {
        return loader;
    }

    @Override
    protected DemandType getDemandType() {
        return DemandType.TYPE_3P;
    }

    private void test3PDemands(boolean sameDemands) {
        RecommendationNew repl1 = find(1);
        RecommendationNew repl2 = find(2);

        DemandDTO demand1 = getSingleDemandById(repl1.getDemandId());
        assertNotNull(demand1);

        DemandDTO demand2 = getSingleDemandById(repl2.getDemandId());
        assertNotNull(demand2);

        if (sameDemands) {
            assertEquals(demand1.getParentDemandId(), demand2.getParentDemandId());
        } else {
            assertNotEquals(demand1.getParentDemandId(), demand2.getParentDemandId());
        }
    }

    @Override
    protected void createEvents() {
        super.createEvents();
        createEvent(addSuffix(completed(STOCKS_WITH_LIFETIMES_LOAD), TYPE_3P));
        createEvent(addSuffix(completed(TRANSITS_LOAD), TYPE_3P));
    }
}
