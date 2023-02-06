package ru.yandex.market.replenishment.autoorder.service.tmp_recommendations_processing;

import java.time.LocalDateTime;

import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.support.AbstractTestExecutionListener;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.replenishment.autoorder.config.FunctionalTest;
import ru.yandex.market.replenishment.autoorder.model.DemandType;
import ru.yandex.market.replenishment.autoorder.repository.postgres.TmpTablesSupportRepository;
import ru.yandex.market.replenishment.autoorder.service.not_grouped_recommendations_processing_service.AbstractTmpRecommendationProcessingLoader;
import ru.yandex.market.replenishment.autoorder.service.not_grouped_recommendations_processing_service.TmpRecommendation1pProcessingLoader;
import ru.yandex.market.replenishment.autoorder.service.not_grouped_recommendations_processing_service.TmpRecommendation3pProcessingLoader;
import ru.yandex.market.replenishment.autoorder.service.not_grouped_recommendations_processing_service.TmpRecommendationTenderProcessingLoader;

import static ru.yandex.market.replenishment.autoorder.repository.postgres.util.DbTableNames.PARENT_DEMAND_3P_TABLE_NAME;
import static ru.yandex.market.replenishment.autoorder.repository.postgres.util.DbTableNames.RECOMMENDATIONS_REGION_SUPPLIER_INFO_TABLE_NAME;
import static ru.yandex.market.replenishment.autoorder.utils.DemandTypePartitionedTable.DEMANDS;
import static ru.yandex.market.replenishment.autoorder.utils.DemandTypePartitionedTable.RECOMMENDATIONS;
import static ru.yandex.market.replenishment.autoorder.utils.DemandTypePartitionedTable.RECOMMENDATIONS_COUNTRY_INFOS;
import static ru.yandex.market.replenishment.autoorder.utils.DemandTypePartitionedTable.RECOMMENDATIONS_REGION_INFOS;
import static ru.yandex.market.replenishment.autoorder.utils.DemandTypePartitionedTable.RECOMMENDATIONS_WH_INFOS;

@TestExecutionListeners(value = {
    TmpRecommendationProcessingLoadersTest.TmpTablesCreation.class
})
@ActiveProfiles("unittest")
public class TmpRecommendationProcessingLoadersTest extends FunctionalTest {

    @Autowired
    TmpRecommendation1pProcessingLoader tmpRecommendation1pProcessingLoader;
    @Autowired
    TmpRecommendation3pProcessingLoader tmpRecommendation3pProcessingLoader;
    @Autowired
    TmpRecommendationTenderProcessingLoader tmpRecommendationTenderProcessingLoader;

    @Test
    @DbUnitDataSet(before = "TmpRecommendationProcessingLoadersTest.test1p.before.csv",
        after = "TmpRecommendationProcessingLoadersTest.test1p.after.csv")
    public void test1p() {
        test(tmpRecommendation1pProcessingLoader);
    }

    @Test
    @DbUnitDataSet(before = "TmpRecommendationProcessingLoadersTest.test3p.before.csv",
        after = "TmpRecommendationProcessingLoadersTest.test3p.after.csv")
    public void test3p() {
        test(tmpRecommendation3pProcessingLoader);
    }

    @Test
    @DbUnitDataSet(before = "TmpRecommendationProcessingLoadersTest.testTender.before.csv",
        after = "TmpRecommendationProcessingLoadersTest.testTender.after.csv")
    public void testTender() {
        test(tmpRecommendationTenderProcessingLoader);
    }

    private void test(AbstractTmpRecommendationProcessingLoader loader) {
        setTestTime(LocalDateTime.of(2020, 10, 5, 0, 0));
        loader.load();
    }

    public static class TmpTablesCreation extends AbstractTestExecutionListener {
        @Override
        public void beforeTestClass(TestContext testContext) {
            SqlSessionFactory factory = testContext.getApplicationContext().getBean("sqlSessionFactory", SqlSessionFactory.class);
            try (SqlSession session = factory.openSession()) {
                TmpTablesSupportRepository mapper = session.getMapper(TmpTablesSupportRepository.class);
                for (DemandType demandType : DemandType.values()) {
                    mapper.createTmpTable(RECOMMENDATIONS.partition(demandType));
                    mapper.createTmpTable(DEMANDS.partition(demandType));
                    mapper.createTmpTable(RECOMMENDATIONS_WH_INFOS.partition(demandType));
                    mapper.createTmpTable(RECOMMENDATIONS_REGION_INFOS.partition(demandType));
                    mapper.createTmpTable(RECOMMENDATIONS_COUNTRY_INFOS.partition(demandType));
                    if (demandType == DemandType.TYPE_3P) {
                        mapper.createTmpTable(RECOMMENDATIONS_REGION_SUPPLIER_INFO_TABLE_NAME);
                        mapper.createTmpTable(PARENT_DEMAND_3P_TABLE_NAME);
                    }
                }
                session.commit();
            }
        }
    }
}
