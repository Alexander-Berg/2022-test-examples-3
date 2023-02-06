package ru.yandex.market.replenishment.autoorder.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Spliterator;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.apache.ibatis.cursor.Cursor;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.replenishment.autoorder.config.FunctionalTest;
import ru.yandex.market.replenishment.autoorder.model.AbstractRecommendation;
import ru.yandex.market.replenishment.autoorder.model.DemandType;
import ru.yandex.market.replenishment.autoorder.model.RecommendationNew;
import ru.yandex.market.replenishment.autoorder.model.entity.postgres.NotGroupedRecommendation;
import ru.yandex.market.replenishment.autoorder.repository.postgres.RecommendationRepository;
import ru.yandex.market.replenishment.autoorder.repository.postgres.SalesRepository;
import ru.yandex.market.replenishment.autoorder.service.environment.EnvironmentService;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertNull;
import static ru.yandex.market.replenishment.autoorder.service.environment.EnvironmentConstants.USE_DENORMALIZED_INFOS_ENABLED;
import static ru.yandex.market.replenishment.autoorder.utils.DemandTypePartitionedTable.RECOMMENDATIONS_COUNTRY_INFOS;
import static ru.yandex.market.replenishment.autoorder.utils.DemandTypePartitionedTable.RECOMMENDATIONS_REGION_INFOS;
import static ru.yandex.market.replenishment.autoorder.utils.DemandTypePartitionedTable.RECOMMENDATIONS_WH_INFOS;

public class RecommendationRepositoryTest extends FunctionalTest {
    @Autowired
    SalesRepository salesRepository;
    @Autowired
    RecommendationRepository recommendationRepository;
    @Autowired
    SqlSessionFactory sqlSessionFactory;
    @Autowired
    private TransactionTemplate transactionTemplate;
    @Autowired
    private EnvironmentService environmentService;

    @Test
    @DbUnitDataSet(before = "RecommendationRepositoryTest_testGetNotRecommendationsWithDistrDemand.before.csv")
    public void testGetRecommendationsWithDistrDemand() {
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(@NotNull TransactionStatus status) {
                recommendationRepository.recreateReplenishmentResultTransitViewForGrouping();
            }
        });

        List<NotGroupedRecommendation> results =
            getRecommendations(r -> r.getNotGroupedRecommendationsByType(DemandType.TYPE_1P,
                RECOMMENDATIONS_WH_INFOS.partition(DemandType.TYPE_1P),
                RECOMMENDATIONS_REGION_INFOS.partition(DemandType.TYPE_1P),
                RECOMMENDATIONS_COUNTRY_INFOS.partition(DemandType.TYPE_1P)));

        assertThat(results, hasSize(1));

        assertThat(results.get(0).getMsku(), equalTo(200L));

        assertThat(results.get(0).getCountryInfo().getSales1p()[0], equalTo(20));
        assertThat(results.get(0).getCountryInfo().getSales1p()[1], equalTo(22));
        assertThat(results.get(0).getCountryInfo().getSales1p()[2], equalTo(24));
        assertThat(results.get(0).getCountryInfo().getSales1p()[3], equalTo(26));
        assertThat(results.get(0).getCountryInfo().getSales1p()[4], equalTo(28));
        assertThat(results.get(0).getCountryInfo().getSales1p()[5], equalTo(30));
        assertThat(results.get(0).getCountryInfo().getSales1p()[6], equalTo(32));
        assertThat(results.get(0).getCountryInfo().getSales1p()[7], equalTo(34));

        assertThat(results.get(0).getCountryInfo().getSalesAll()[0], equalTo(30));
        assertThat(results.get(0).getCountryInfo().getSalesAll()[1], equalTo(32));
        assertThat(results.get(0).getCountryInfo().getSalesAll()[2], equalTo(34));
        assertThat(results.get(0).getCountryInfo().getSalesAll()[3], equalTo(36));
        assertThat(results.get(0).getCountryInfo().getSalesAll()[4], equalTo(38));
        assertThat(results.get(0).getCountryInfo().getSalesAll()[5], equalTo(40));
        assertThat(results.get(0).getCountryInfo().getSalesAll()[6], equalTo(42));
        assertThat(results.get(0).getCountryInfo().getSalesAll()[7], equalTo(44));

        assertThat(results.get(0).getCountryInfo().getSalesForecast14days(), equalTo(15.));
        assertThat(results.get(0).getCountryInfo().getSalesForecast28days(), equalTo(35.));
        assertThat(results.get(0).getCountryInfo().getSalesForecast56days(), equalTo(55.));

        assertThat(results.get(0).getCountryInfo().getStock(), equalTo(15L));
        assertThat(results.get(0).getCountryInfo().getStockOverall(), equalTo(30L));

        assertNull(results.get(0).getWarehouseInfo().getRegionalSales1p());
        assertNull(results.get(0).getWarehouseInfo().getRegionalSalesAll());
    }

    @Test
    @DbUnitDataSet(before =
        "RecommendationRepositoryTest_testGetNotRecommendationsWithDistrDemandRegionalWarehouse" +
            ".before.csv")
    public void testGetRecommendationsWithDistrDemandRegionalWarehouse() {
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(@NotNull TransactionStatus status) {
                recommendationRepository.recreateReplenishmentResultTransitViewForGrouping();
            }
        });

        List<NotGroupedRecommendation> results =
            getRecommendations(r -> r.getNotGroupedRecommendationsByType(DemandType.TYPE_1P,
                RECOMMENDATIONS_WH_INFOS.partition(DemandType.TYPE_1P),
                RECOMMENDATIONS_REGION_INFOS.partition(DemandType.TYPE_1P),
                RECOMMENDATIONS_COUNTRY_INFOS.partition(DemandType.TYPE_1P)));

        assertThat(results, hasSize(1));

        assertThat(results.get(0).getMsku(), equalTo(200L));

        assertThat(results.get(0).getCountryInfo().getSales1p()[0], equalTo(20));
        assertThat(results.get(0).getCountryInfo().getSales1p()[1], equalTo(22));
        assertThat(results.get(0).getCountryInfo().getSales1p()[2], equalTo(24));
        assertThat(results.get(0).getCountryInfo().getSales1p()[3], equalTo(26));
        assertThat(results.get(0).getCountryInfo().getSales1p()[4], equalTo(28));
        assertThat(results.get(0).getCountryInfo().getSales1p()[5], equalTo(30));
        assertThat(results.get(0).getCountryInfo().getSales1p()[6], equalTo(32));
        assertThat(results.get(0).getCountryInfo().getSales1p()[7], equalTo(34));

        assertThat(results.get(0).getCountryInfo().getSalesAll()[0], equalTo(30));
        assertThat(results.get(0).getCountryInfo().getSalesAll()[1], equalTo(32));
        assertThat(results.get(0).getCountryInfo().getSalesAll()[2], equalTo(34));
        assertThat(results.get(0).getCountryInfo().getSalesAll()[3], equalTo(36));
        assertThat(results.get(0).getCountryInfo().getSalesAll()[4], equalTo(38));
        assertThat(results.get(0).getCountryInfo().getSalesAll()[5], equalTo(40));
        assertThat(results.get(0).getCountryInfo().getSalesAll()[6], equalTo(42));
        assertThat(results.get(0).getCountryInfo().getSalesAll()[7], equalTo(44));

        assertThat(results.get(0).getCountryInfo().getSalesForecast14days(), equalTo(15.));
        assertThat(results.get(0).getCountryInfo().getSalesForecast28days(), equalTo(35.));
        assertThat(results.get(0).getCountryInfo().getSalesForecast56days(), equalTo(55.));

        assertThat(results.get(0).getCountryInfo().getStock(), equalTo(15L));
        assertThat(results.get(0).getCountryInfo().getStockOverall(), equalTo(30L));

        assertThat(results.get(0).getWarehouseInfo().getRegionalSales1p(),
            equalTo(new int[]{10, 11, 12, 12, 13, 13, 13, 13}));

        assertThat(results.get(0).getWarehouseInfo().getRegionalSalesAll(),
            equalTo(new int[]{15, 16, 17, 17, 18, 18, 18, 18}));
    }

    @Test
    @DbUnitDataSet(before = "RecommendationRepositoryTest_testGetAllReturnsIsCoreFixMatrix.before.csv")
    public void testGetAllReturnsIsCoreFixMatrix() {

        final boolean useDenormalized = environmentService.getBooleanWithDefault(USE_DENORMALIZED_INFOS_ENABLED,
            false);
        List<RecommendationNew> results = getRecommendations(
            (RecommendationRepository repository) -> repository.getAll(DemandType.TYPE_1P, useDenormalized));

        assertThat(results, hasSize(2));
        assertThat(results.get(0).isCoreFixMatrix(), equalTo(true));
        assertThat(results.get(1).isCoreFixMatrix(), equalTo(false));
    }

    @Test
    @DbUnitDataSet(before = "RecommendationRepositoryTest_testDeleteOldRecommendationAdjustments.before.csv",
        after = "RecommendationRepositoryTest_testDeleteOldRecommendationAdjustments.after.csv")
    public void testDeleteOldRecommendationAdjustments() {
        try (SqlSession session = sqlSessionFactory.openSession()) {
            final RecommendationRepository recommendationRepository =
                session.getMapper(RecommendationRepository.class);
            recommendationRepository.deleteFutureRecommendationAdjustments(
                DemandType.TYPE_1P, LocalDate.of(2021, 5, 25));
        }
    }

    @Test
    @DbUnitDataSet(before = "RecommendationRepositoryTest_testGetRecommendations3p.before.csv")
    public void testGetRecommendations3p() {

        List<NotGroupedRecommendation> results =
            getRecommendations(r -> r.getNotGroupedRecommendationsByType(DemandType.TYPE_3P,
                RECOMMENDATIONS_WH_INFOS.partition(DemandType.TYPE_3P),
                RECOMMENDATIONS_REGION_INFOS.partition(DemandType.TYPE_3P),
                RECOMMENDATIONS_COUNTRY_INFOS.partition(DemandType.TYPE_3P)));

        assertThat(results, hasSize(1));
        assertThat(results.get(0).getMsku(), equalTo(200L));
        assertThat(results.get(0).getPrice(), nullValue());
    }

    @NotNull
    private <T extends AbstractRecommendation> List<T> getRecommendations(Function<RecommendationRepository,
        Cursor<T>> rowsGetter) {
        try (SqlSession session = sqlSessionFactory.openSession()) {
            final RecommendationRepository recommendationRepository =
                session.getMapper(RecommendationRepository.class);
            Spliterator<T> spliterator = rowsGetter.apply(recommendationRepository).spliterator();
            return StreamSupport.stream(spliterator, false)
                .collect(Collectors.toList());
        }
    }
}
