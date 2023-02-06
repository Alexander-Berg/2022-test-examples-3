package ru.yandex.market.replenishment.autoorder.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import com.google.common.collect.Comparators;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.replenishment.autoorder.api.dto.user_fillters.recommendation.RecommendationFilters;
import ru.yandex.market.replenishment.autoorder.api.dto.user_fillters.recommendation.RecommendationsFilteringResult;
import ru.yandex.market.replenishment.autoorder.config.FunctionalTest;
import ru.yandex.market.replenishment.autoorder.exception.UserWarningException;
import ru.yandex.market.replenishment.autoorder.model.AbstractRecommendation;
import ru.yandex.market.replenishment.autoorder.model.DemandType;
import ru.yandex.market.replenishment.autoorder.model.FilterType;
import ru.yandex.market.replenishment.autoorder.model.RecommendationFilter;
import ru.yandex.market.replenishment.autoorder.model.RecommendationNew;
import ru.yandex.market.replenishment.autoorder.model.WeeksType;
import ru.yandex.market.replenishment.autoorder.model.dto.AdjustedRecommendationDTO;
import ru.yandex.market.replenishment.autoorder.model.dto.AdjustedRecommendationsDTO;
import ru.yandex.market.replenishment.autoorder.model.dto.RecommendationCountsDTO;
import ru.yandex.market.replenishment.autoorder.repository.postgres.RecommendationRepository;
import ru.yandex.market.replenishment.autoorder.service.environment.EnvironmentService;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static ru.yandex.market.replenishment.autoorder.service.environment.EnvironmentConstants.USE_DENORMALIZED_INFOS_ENABLED;

public class DbReplenishmentServiceTest extends FunctionalTest {
    private static final String LOGIN = "pupkin";

    @Autowired
    private DbReplenishmentService dbReplenishmentService;

    @Autowired
    private SqlSessionFactory sqlSessionFactory;

    @Autowired
    private EnvironmentService environmentService;

    private static void assertCount(RecommendationCountsDTO count) {
        assertNotNull(count);

        List<RecommendationCountsDTO.FilterInfo> filtersInfo = count.getFilter();
        assertNotNull(filtersInfo);
        assertThat(filtersInfo, hasSize(12));

        RecommendationCountsDTO.FilterInfo filterInfo = filtersInfo.get(0);
        assertThat(filterInfo.getId(), equalTo(FilterType.ALL));
        assertThat(filterInfo.getCount(), equalTo(21L));
        assertFalse(filterInfo.isProcessed());
        assertNull(filterInfo.getWeeks());

        filterInfo = filtersInfo.get(1);
        assertThat(filterInfo.getId(), equalTo(FilterType.PROCESSED));
        assertThat(filterInfo.getCount(), equalTo(2L));
        assertTrue(filterInfo.isProcessed());
        assertNull(filterInfo.getWeeks());

        filterInfo = filtersInfo.get(2);
        assertThat(filterInfo.getId(), equalTo(FilterType.NEED_MANUAL_REVIEW));
        assertThat(filterInfo.getCount(), equalTo(19L));
        assertFalse(filterInfo.isProcessed());
        assertNull(filterInfo.getWeeks());

        filterInfo = filtersInfo.get(3);
        assertThat(filterInfo.getId(), equalTo(FilterType.NEW));
        assertThat(filterInfo.getCount(), equalTo(2L));
        assertFalse(filterInfo.isProcessed());
        assertNull(filterInfo.getWeeks());

        filterInfo = filtersInfo.get(4);
        assertThat(filterInfo.getId(), equalTo(FilterType.SALES_ZERO));
        assertThat(filterInfo.getCount(), equalTo(4L));
        assertFalse(filterInfo.isProcessed());
        assertNotNull(filterInfo.getWeeks());
        assertWeekInfo(filterInfo, new long[]{1L, 1L, 1L, 1L}, new boolean[]{false, false, false, false});

        filterInfo = filtersInfo.get(5);
        assertThat(filterInfo.getId(), equalTo(FilterType.SALES_ZERO_AND_POSITIVE_STOCK_OR_TRANSIT));
        assertThat(filterInfo.getCount(), equalTo(4L));
        assertTrue(filterInfo.isProcessed());
        assertNotNull(filterInfo.getWeeks());
        assertWeekInfo(filterInfo, new long[]{1L, 1L, 1L, 1L}, new boolean[]{true, true, true, true});

        filterInfo = filtersInfo.get(6);
        assertThat(filterInfo.getId(), equalTo(FilterType.SALES_LT_QUANTUM));
        assertThat(filterInfo.getCount(), equalTo(4L));
        assertFalse(filterInfo.isProcessed());
        assertNotNull(filterInfo.getWeeks());
        assertWeekInfo(filterInfo, new long[]{1L, 1L, 1L, 1L}, new boolean[]{false, false, false, false});

        filterInfo = filtersInfo.get(7);
        assertThat(filterInfo.getId(), equalTo(FilterType.SC));
        assertThat(filterInfo.getCount(), equalTo(2L));
        assertFalse(filterInfo.isProcessed());
        assertNotNull(filterInfo.getWeeks());
        assertWeekInfo(filterInfo, new long[]{0L, 1L, 0L, 1L, 0L, 2L}, new boolean[]{true, false, true, false, true, false});

        filterInfo = filtersInfo.get(8);
        assertThat(filterInfo.getId(), equalTo(FilterType.MULTIPLE_WAREHOUSES));
        assertThat(filterInfo.getCount(), equalTo(1L));
        assertFalse(filterInfo.isProcessed());
        assertNull(filterInfo.getWeeks());

        filterInfo = filtersInfo.get(9);
        assertThat(filterInfo.getId(), equalTo(FilterType.SPECIAL_ORDER));
        assertThat(filterInfo.getCount(), equalTo(1L));
        assertFalse(filterInfo.isProcessed());
        assertNull(filterInfo.getWeeks());

        filterInfo = filtersInfo.get(10);
        assertThat(filterInfo.getId(), equalTo(FilterType.TRANSIT_WARNING));
        assertThat(filterInfo.getCount(), equalTo(1L));
        assertFalse(filterInfo.isProcessed());
        assertNull(filterInfo.getWeeks());
    }

    private static void assertWeekInfo(RecommendationCountsDTO.FilterInfo filterInfo, long[] counts,
                                       boolean[] processed) {
        WeeksType[] weeksTypes =
            {WeeksType.ZERO_TWO, WeeksType.TWO_THREE, WeeksType.THREE_FOUR, WeeksType.FOUR_EIGHT, WeeksType.EIGHT_INF, WeeksType.ALL};
        int i = 0;
        for (RecommendationCountsDTO.WeekInfo weekInfo : filterInfo.getWeeks()) {
            assertThat(weekInfo.getId(), equalTo(weeksTypes[i]));
            assertThat(weekInfo.getCount(), equalTo(counts[i]));
            assertThat(weekInfo.isProcessed(), equalTo(processed[i]));
            i++;
        }
    }

    @Test
    @DbUnitDataSet(before = "DbReplenishmentServiceTest.before.csv")
    public void adjustReplenishmentsSimpleTest2() {
        int[] expectedQty = {15, 20, 30, 2, 2, 2, 2};

        AdjustedRecommendationsDTO input = new InputBuilder()
            .addAdjRecommendation(1001L, 100, 10, 1, 1L, true)
            .addAdjRecommendation(1005L, 500, 30, 5, 2L, false)
            .input;
        input.setDemandId(1L);
        dbReplenishmentService.adjustRecommendations(DemandType.TYPE_1P, 1L, 1, input.getAdjustedRecommendations(),
            LOGIN);

        assertQtyResults(expectedQty);
    }

    @Test
    @DbUnitDataSet(before = "DbReplenishmentServiceTest.before.csv")
    public void adjustReplenishmentsAdjustmentWithoutIds() {
        AdjustedRecommendationsDTO input = new InputBuilder()
            .addAdjRecommendation(null, 100, 10, 1, 1L, true)
            .addAdjRecommendation(null, 500, 30, 5, 2L, false)
            .input;
        input.setDemandId(1L);

        UserWarningException ex = assertThrows(
            UserWarningException.class, () ->
                dbReplenishmentService.adjustRecommendations(DemandType.TYPE_1P, 1L, 1,
                    input.getAdjustedRecommendations(), LOGIN)
        );

        assertThat(ex.getMessage(), equalTo("Для рекомендаций на MSKU 100, 500 не указаны идентификаторы"));
    }

    @Test
    @DbUnitDataSet(before = "DbReplenishmentServiceTest.testInactiveMonoXDock.before.csv")
    public void adjustReplenishmentsAdjustmentWithInactiveMonoXDock() {
        AdjustedRecommendationsDTO input = new InputBuilder()
            .addAdjRecommendation(1001L, 100, 10, 1, 1L, true)
            .input;
        input.setDemandId(1L);

        UserWarningException ex = assertThrows(
            UserWarningException.class, () ->
                dbReplenishmentService.adjustRecommendations(DemandType.TYPE_1P, 1L, 1,
                    input.getAdjustedRecommendations(), LOGIN)
        );

        assertThat(ex.getMessage(), equalTo("Нулевая mono-x-dock рекомендация не может быть изменена по причине: `cannot adj`"));
    }

    @Test
    @DbUnitDataSet(before = "DbReplenishmentServiceTest.before.csv")
    public void adjustReplenishmentsIllegalCorrectionReasonTest2() {
        int[] expectedQty = {23, 20, 2, 2, 2, 2, 2};

        AdjustedRecommendationsDTO input = new InputBuilder()
            .addAdjRecommendation(1001L, 100, 10, 42, 0L)
            .input;
        input.setDemandId(1L);

        UserWarningException ex = assertThrows(
            UserWarningException.class, () ->
                dbReplenishmentService.adjustRecommendations(DemandType.TYPE_1P, 1L, 1,
                    input.getAdjustedRecommendations(), LOGIN)
        );

        assertThat(ex.getMessage(), equalTo("Неверный id причины редактирования 42"));

        assertQtyResults(expectedQty);
    }

    @Test
    @DbUnitDataSet(before = "DbReplenishmentServiceTest.before.csv")
    public void adjustReplenishmentsIllegalIdTest2() {
        int[] expectedQty = {23, 20, 2, 2, 2, 2, 2};

        AdjustedRecommendationsDTO input = new InputBuilder()
            .addAdjRecommendation(1003L, 300, 10, 3, 1L)
            .addAdjRecommendation(1042L, 42, 30, 4, 2L)
            .input;
        input.setDemandId(1L);

        UserWarningException ex = assertThrows(
            UserWarningException.class, () ->
                dbReplenishmentService.adjustRecommendations(DemandType.TYPE_1P, 1L, 1,
                    input.getAdjustedRecommendations(), LOGIN)
        );

        assertThat(ex.getMessage(),
            equalTo("Не найдены редактируемые рекомендации с ID: 1042"));

        assertQtyResults(expectedQty);
    }

    @Test
    @DbUnitDataSet(before = "DbReplenishmentServiceTest_withCount.before.csv")
    public void testGetRecommendationsWithCount_All() {
        testGetRecommendationsWithCountNew(FilterType.ALL, null, 20);
    }

    @Test
    @DbUnitDataSet(before = "DbReplenishmentServiceTest_withCount.before.csv")
    public void testGetRecommendationsWithCount_Processed() {
        testGetRecommendationsWithCountNew(FilterType.PROCESSED, null, 2);
    }

    @Test
    @DbUnitDataSet(before = "DbReplenishmentServiceTest_withCount.before.csv")
    public void testGetRecommendationsWithCount_NeedManualReview() {
        testGetRecommendationsWithCountNew(FilterType.NEED_MANUAL_REVIEW, null, 18);
    }

    @Test
    @DbUnitDataSet(before = "DbReplenishmentServiceTest_withCount.before.csv")
    public void testGetRecommendationsWithCount_New() {
        testGetRecommendationsWithCountNew(FilterType.NEW, null, 2);
    }

    @Test
    @DbUnitDataSet(before = "DbReplenishmentServiceTest_withCount.before.csv")
    public void testGetRecommendationsWithCount_SalesZero() {
        testGetRecommendationsWithCountNew(FilterType.SALES_ZERO, WeeksType.TWO_THREE, 1);
    }

    @Test
    @DbUnitDataSet(before = "DbReplenishmentServiceTest_withCount.before.csv")
    public void testGetRecommendationsWithCount_SalesLtQuantum() {
        testGetRecommendationsWithCountNew(FilterType.SALES_LT_QUANTUM, WeeksType.TWO_THREE, 1);
    }

    @Test
    @DbUnitDataSet(before = "DbReplenishmentServiceTest_withCount.before.csv")
    public void testGetRecommendationsWithCount_Sc() {
        testGetRecommendationsWithCountNew(FilterType.SC, WeeksType.TWO_THREE, 1);
    }

    @Test
    @DbUnitDataSet(before = "DbReplenishmentServiceTest_withCount.before.csv")
    public void testGetRecommendationsWithCount_ScWithAllWeeks() {
        testGetRecommendationsWithCountNew(FilterType.SC, WeeksType.ALL, 2);
    }

    @Test
    @DbUnitDataSet(before = "DbReplenishmentServiceTest_additionalTransit.before.csv")
    public void testGetRecommendationsWithAdditionalTransit() {
        try (SqlSession session = sqlSessionFactory.openSession()) {
            RecommendationRepository mapper = session.getMapper(RecommendationRepository.class);
            mapper.recreateReplenishmentResultTransitViewForGrouping();
            mapper.recreateReplenishmentResultTransitView();
        }

        RecommendationFilter filter = new RecommendationFilter();
        filter.setDemandIds(Collections.singletonList(100499L));

        final RecommendationFilters filters = new RecommendationFilters();
        filters.setFilter(filter);

        RecommendationsFilteringResult result = dbReplenishmentService.getRecommendationsWithCount(
            DemandType.TYPE_1P, filters);
        assertThat(result.getRecommendations(), hasSize(1));
        assertThat(result.getRecommendations().get(0).getTransit(), equalTo(21));
    }

    private void testGetRecommendationsWithCountNew(FilterType filterType, WeeksType weeksType, int size) {
        RecommendationFilter filter = new RecommendationFilter();
        filter.setDemandIds(Collections.singletonList(1L));
        filter.setFilter(filterType);
        filter.setWeeks(weeksType);

        final RecommendationFilters filters = new RecommendationFilters();
        filters.setFilter(filter);

        RecommendationsFilteringResult result = dbReplenishmentService.getRecommendationsWithCount(
            DemandType.TYPE_1P, filters);

        assertNotNull(result.getRecommendations());
        assertThat(result.getRecommendations(), hasSize(size));
        assertTrue(Comparators.isInOrder(result.getRecommendations(),
            Comparator.comparingInt(r -> r.getAbc() == null ? -1 : r.getAbc().getOrder())));
        assertCount(result.getCount());
    }

    private void assertQtyResults(int[] expectedQty) {
        List<RecommendationNew> results;
        final boolean useDenormalized = environmentService.getBooleanWithDefault(USE_DENORMALIZED_INFOS_ENABLED,
            false);
        try (SqlSession session = sqlSessionFactory.openSession()) {
            final RecommendationRepository recommendationRepository = session.getMapper(RecommendationRepository.class);
            results = StreamSupport.stream(recommendationRepository.getAll(DemandType.TYPE_1P, useDenormalized)
                    .spliterator(), false)
                .sorted(Comparator.comparingLong(RecommendationNew::getDemandId)
                    .thenComparingLong(AbstractRecommendation::getMsku))
                .collect(Collectors.toList());
        }
        assertThat(results, hasSize(expectedQty.length));

        for (int i = 0; i < expectedQty.length; i++) {
            assertThat(results.get(i).getAdjustedPurchQty(), equalTo(expectedQty[i]));
        }
    }

    private static class InputBuilder {
        private final AdjustedRecommendationsDTO input = new AdjustedRecommendationsDTO();

        {
            input.setAdjustedRecommendations(new ArrayList<>(2));
        }

        private InputBuilder addAdjRecommendation(
            Long id, long msku, int qty, long reason, Long groupId) {
            return addAdjRecommendation(id, msku, qty, reason, groupId, false);
        }

        private InputBuilder addAdjRecommendation(
            Long id, long msku, int qty, long reason, Long groupId, boolean needsReview) {
            AdjustedRecommendationDTO adj;
            adj = new AdjustedRecommendationDTO();
            adj.setId(id);
            adj.setMsku(msku);
            adj.setAdjustedPurchQty(qty);
            adj.setCorrectionReason(reason);
            adj.setGroupId(groupId);
            adj.setNeedsManualReview(needsReview);
            input.getAdjustedRecommendations().add(adj);
            return this;
        }
    }
}
