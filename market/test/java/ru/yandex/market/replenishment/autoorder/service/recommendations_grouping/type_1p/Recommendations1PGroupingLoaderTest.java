package ru.yandex.market.replenishment.autoorder.service.recommendations_grouping.type_1p;

import java.time.LocalDate;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataBaseConfig;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.replenishment.autoorder.model.DemandType;
import ru.yandex.market.replenishment.autoorder.service.not_grouped_recommendations_processing_service.AbstractGroupingLoader;
import ru.yandex.market.replenishment.autoorder.service.not_grouped_recommendations_processing_service.Recommendations1PGroupingLoader;
import ru.yandex.market.replenishment.autoorder.service.recommendations_grouping.BaseGroupingTest;

import static ru.yandex.market.replenishment.autoorder.model.DemandType.TYPE_1P;
import static ru.yandex.market.replenishment.autoorder.model.entity.postgres.ImportEvents.RECOMMENDATIONS_MSKU_INFO_LOAD;
import static ru.yandex.market.replenishment.autoorder.model.entity.postgres.ImportEvents.REGIONAL_ASSORTMENT_LOAD;
import static ru.yandex.market.replenishment.autoorder.model.entity.postgres.ImportEvents.SALES_LOAD;
import static ru.yandex.market.replenishment.autoorder.model.entity.postgres.ImportEvents.SPECIAL_RECOMMENDATIONS_CREATE;
import static ru.yandex.market.replenishment.autoorder.model.entity.postgres.ImportEvents.STOCKS_WITH_LIFETIMES_LOAD;
import static ru.yandex.market.replenishment.autoorder.model.entity.postgres.ImportEvents.TRANSITS_LOAD;
import static ru.yandex.market.replenishment.autoorder.model.entity.postgres.ImportEvents.addSuffix;
import static ru.yandex.market.replenishment.autoorder.model.entity.postgres.ImportEvents.completed;

@DbUnitDataBaseConfig({
    @DbUnitDataBaseConfig.Entry(
        name = "tableType",
        value = "TABLE,MATERIALIZED VIEW")
})
public class Recommendations1PGroupingLoaderTest extends BaseGroupingTest {

    @Autowired
    Recommendations1PGroupingLoader loader;

    protected AbstractGroupingLoader getLoader() {
        return loader;
    }

    @Override
    protected DemandType getDemandType() {
        return DemandType.TYPE_1P;
    }

    @Before
    public void mockMockedTimeService() {
        setTestTime(NOW_DATE_TIME);
    }

    @Test
    @DbUnitDataSet(before = "Recommendations1PGroupingLoaderTest.simple.before.csv",
        after = "Recommendations1PGroupingLoaderTest.simple.after.csv")
    public void testSimpleMappingAndDemandCreation() {
        getLoader().load();
    }

    @Test
    @DbUnitDataSet(before = "Recommendations1PGroupingLoaderTest.monoXdoc.before.csv",
        after = "Recommendations1PGroupingLoaderTest.monoXdoc.after.csv")
    public void testMonoXdocSimpleMappingAndDemandCreation() {
        getLoader().load();
    }

    @Test
    @DbUnitDataSet(before = "Recommendations1PGroupingLoaderTest.startedDemandsRecs.before.csv",
        after = "Recommendations1PGroupingLoaderTest.startedDemandsRecs.after.csv")
    public void testStartedDemandRecsRemaining() {
        getLoader().load();
    }

    @Test
    @DbUnitDataSet(before = "Recommendations1PGroupingLoaderTest.different_checks.before.csv")
    public void testSameDemands() {
        createNotGrouped(1, 500L, 123, TOMILINO, null, 1, LocalDate.now());
        createNotGrouped(2, 500L, 234, TOMILINO, null, 1, LocalDate.now());
        test(true, false);
    }

    @Test
    @DbUnitDataSet(before = "Recommendations1PGroupingLoaderTest.different_checks.before.csv")
    public void testDiffDates() {
        createNotGrouped(1, 500L, 123, TOMILINO, null, 1, LocalDate.now());
        createNotGrouped(2, 500L, 123, TOMILINO, null, 1, LocalDate.now().plusDays(8));
        test(false, false);
    }


    @Test
    @DbUnitDataSet(before = "Recommendations1PGroupingLoaderTest.different_checks.before.csv")
    public void testDifferentWhs() {
        createNotGrouped(1, 500L, 123, TOMILINO, null, 1, LocalDate.now());
        createNotGrouped(2, 500L, 123, SOFYINO, null, 1, LocalDate.now());
        test(false, true);
    }

    @Test
    @DbUnitDataSet(before = "Recommendations1PGroupingLoaderTest.different_checks.before.csv")
    public void testDifferentSuppliers() {
        createNotGrouped(1, 500L, 123, TOMILINO, null, 1, LocalDate.now());
        createNotGrouped(2, 600L, 234, TOMILINO, null, 1, LocalDate.now());
        test(false, false);
    }

    @Test
    @DbUnitDataSet(before = "Recommendations1PGroupingLoaderTest.different_checks.before.csv")
    public void testRostov() {
        createNotGrouped(1, 500L, 123, ROSTOV, null, 1, LocalDate.now());
        createNotGrouped(2, 500L, 123, TOMILINO, null, 1, LocalDate.now());
        test(false, false);
    }

    @Test
    @DbUnitDataSet(before = "Recommendations1PGroupingLoaderTest.withSpecial.before.csv")
    public void testWithSpecial() {
        createNotGrouped(1, 500L, 123, TOMILINO, null, 1, LocalDate.now());
        createNotGrouped(2, 500L, 123, TOMILINO, null, 1, LocalDate.now(), 1L, LocalDate.now());
        test(false, true);
    }

    @Test
    @DbUnitDataSet(before = "Recommendations1PGroupingLoaderTest.different_checks.before.csv")
    public void testDifferentResponsibles() {
        createNotGrouped(1, 500L, 123, TOMILINO, null, 1, LocalDate.now());
        createNotGrouped(2, 500L, 123, TOMILINO, null, 2, LocalDate.now());
        test(false, false);
    }

    @Test
    @DbUnitDataSet(before = "Recommendations1PGroupingLoaderTest.different_checks.before.csv")
    public void testDifferentWeekStartDates() {
        final LocalDate now = LocalDate.now();
        createNotGrouped(1, 500L, 123, TOMILINO, null, 1, now, 3L, now);
        createNotGrouped(2, 500L, 123, TOMILINO, null, 1, now, 4L, now.plusWeeks(1));
        test(false, true);
    }

    @Test
    @DbUnitDataSet(before = "Recommendations1PGroupingLoaderTest.different_checks.before.csv")
    public void testDifferentSupplyRoute() {
        createNotGrouped(1, 500L, 123, ROSTOV, null, 1, LocalDate.now());
        createNotGrouped(2, 500L, 123, ROSTOV, XDOC, 1, LocalDate.now());
        test(false, false);
    }

    @Test
    @DbUnitDataSet(before = "Recommendations1PGroupingLoaderTest.testAutoprocessingSuccess.before.csv",
        after = "Recommendations1PGroupingLoaderTest.testAutoprocessingSuccess.after.csv")
    public void testAutoprocessingFlag() {
        getLoader().load();
    }

    @Test
    @DbUnitDataSet(before = "Recommendations1PGroupingLoaderTest.different_checks.before.csv",
        after = "Recommendations1PGroupingLoaderTest.testGoalAssortmentEbp.after.csv")
    public void testGoalAssortmentEbp() {
        createNotGrouped(1, 500L, 123, ROSTOV, null, 1, LocalDate.now());
        createEvents();
        getLoader().load();
    }

    @Test
    @DbUnitDataSet(before = "Recommendations1PGroupingLoaderTest.linking_checks.before.csv",
        after = "Recommendations1PGroupingLoaderTest.linking_checks.after.csv")
    public void testLinking() {
        getLoader().load();
    }

    @Test
    @DbUnitDataSet(before = "Recommendations1PGroupingLoaderTest.testSubSsku.before.csv",
        after = "Recommendations1PGroupingLoaderTest.testSubSsku.after.csv")
    public void testSubSsku() {
        getLoader().load();
    }

    @Override
    protected void createEvents() {
        super.createEvents();
        createEvent(completed(SPECIAL_RECOMMENDATIONS_CREATE));
        createEvent(addSuffix(completed(STOCKS_WITH_LIFETIMES_LOAD), TYPE_1P));
        createEvent(addSuffix(completed(TRANSITS_LOAD), TYPE_1P));
        createEvent(completed(SALES_LOAD));
        createEvent(completed(REGIONAL_ASSORTMENT_LOAD));
        createEvent(completed(RECOMMENDATIONS_MSKU_INFO_LOAD));
    }
}
