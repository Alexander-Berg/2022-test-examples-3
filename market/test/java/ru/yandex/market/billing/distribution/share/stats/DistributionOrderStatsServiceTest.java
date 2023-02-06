package ru.yandex.market.billing.distribution.share.stats;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import name.falgout.jeffrey.testing.junit.mockito.MockitoExtension;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import ru.yandex.common.util.date.TestableClock;
import ru.yandex.market.billing.distribution.share.MobileOrdersListFactory;
import ru.yandex.market.core.billing.distribution.share.stats.DistributionOrderStatsDao;
import ru.yandex.market.core.billing.distribution.share.stats.model.DistributionOrderStats;
import ru.yandex.market.core.billing.distribution.share.stats.model.DistributionOrderStatsDimensionAggregate;
import ru.yandex.market.core.billing.distribution.share.stats.model.DistributionOrderStatsRaw;
import ru.yandex.market.core.category.CategoryService;
import ru.yandex.market.core.category.model.Category;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class DistributionOrderStatsServiceTest {
    private static final Instant NOW = Instant.parse("2021-01-02T00:00:00.00Z");

    private static final DistributionOrderStatsRaw RAW_RECORD_1_1 = DistributionOrderStatsRaw.builder()
            .setMultiOrderId("multi_1")
            .setOrderId(1)
            .setItemId(11)
            .build();

    private static final DistributionOrderStatsRaw RAW_RECORD_1_2 = DistributionOrderStatsRaw.builder()
            .setMultiOrderId("multi_1")
            .setOrderId(1)
            .setItemId(12)
            .build();

    private static final DistributionOrderStatsRaw RAW_RECORD_2_1 = DistributionOrderStatsRaw.builder()
            .setMultiOrderId("multi_1")
            .setOrderId(2)
            .setItemId(22)
            .build();

    private static final DistributionOrderStatsRaw RAW_RECORD_3_1 = DistributionOrderStatsRaw.builder()
            .setMultiOrderId("multi_3")
            .setOrderId(3)
            .setItemId(31)
            .build();

    private TestableClock clock = new TestableClock();
    @Mock
    private DistributionOrderStatsDao distributionOrderStatsDao;
    @Mock
    private DistributionOrderStatsCalculator distributionOrderStatsCalculator;
    @Mock
    private CategoryService categoryService;
    @Mock
    private MobileOrdersListFactory mobileOrdersListFactory;

    private DistributionOrderStatsService distributionOrderStatsService;

    @BeforeEach
    private void setup() {
        clock.setFixed(NOW, ZoneOffset.UTC);
        when(mobileOrdersListFactory.fetch(942)).thenReturn(List.of(3L));
        distributionOrderStatsService = new DistributionOrderStatsService(
                clock, distributionOrderStatsDao, distributionOrderStatsCalculator, categoryService, null,
                mobileOrdersListFactory
        );
    }

    @Test
    public void testSplitOnMultiorders() {
        doAnswer(invocation -> {
            Consumer<DistributionOrderStatsRaw> consumer = invocation.getArgument(2);
            Stream.of(
                    RAW_RECORD_1_1,
                    RAW_RECORD_1_2,
                    RAW_RECORD_2_1,
                    RAW_RECORD_3_1
            ).forEach(consumer);
            return null;
        }).when(distributionOrderStatsDao).getChanges(any(), any(), any());

        distributionOrderStatsService.refreshStats(null);

        ArgumentCaptor<List<DistributionOrderStatsRaw>> captor = ArgumentCaptor.forClass(List.class);
        verify(distributionOrderStatsCalculator, atLeastOnce()).getStatsToSave(
                captor.capture(), eq(Set.of(3L)), eq(true), eq(true));
        Assertions.assertThat(captor.getAllValues()).containsExactly(
            List.of(
                    RAW_RECORD_1_1,
                    RAW_RECORD_1_2,
                    RAW_RECORD_2_1
            ),
            List.of(
                    RAW_RECORD_3_1
            )
        );
    }

    @Test
    public void testSaveAllRecords() {
        doAnswer(invocation -> {
            Consumer<DistributionOrderStatsRaw> consumer = invocation.getArgument(2);
            Stream.of(
                    RAW_RECORD_1_1,
                    RAW_RECORD_1_2,
                    RAW_RECORD_2_1,
                    RAW_RECORD_3_1
            ).forEach(consumer);
            return null;
        }).when(distributionOrderStatsDao).getChanges(any(), any(), any());

        AtomicInteger id = new AtomicInteger(0);
        when(distributionOrderStatsCalculator.getStatsToSave(any(), eq(Set.of(3L)), eq(true), eq(true)))
                .thenAnswer(invocation ->
                        Stream.generate(() -> DistributionOrderStats.builder().setId(id.getAndIncrement()).build())
                            .limit(777)
                            .collect(Collectors.toList())
                );

        distributionOrderStatsService.refreshStats(null);

        ArgumentCaptor<List<DistributionOrderStats>> captor = ArgumentCaptor.forClass(List.class);
        verify(distributionOrderStatsDao, atLeastOnce()).insert(captor.capture());

        List<Long> allSavedItemIds = captor.getAllValues().stream()
                .flatMap(Collection::stream)
                .map(DistributionOrderStats::getId)
                .collect(Collectors.toList());

        List<Long> expectedIds = LongStream.range(0, id.get()).boxed()
                .collect(Collectors.toList());

        Assertions.assertThat(allSavedItemIds)
                .containsExactlyElementsOf(expectedIds);
    }

    @Test
    public void testGetApprovedDistributionOrderStatsCategoryAggregate() {
        when(distributionOrderStatsDao.getApprovedDistributionOrderStatsAggregateByDimension(any()))
                .thenReturn(getDistributionOrderStatsCategoryAggregates());
        Category rootCategory = new Category.Builder()
                .setCategory("ab")
                .setParentId(null)
                .setId(0L)
                .build();
        Category category = new Category.Builder()
                .setCategory("a")
                .setParentId(0L)
                .setId(1L)
                .build();
        when(categoryService.getLeafToRootCategoryBranch(1L))
                .thenReturn(Arrays.asList(
                        category,
                        rootCategory));

        Category category1 = new Category.Builder()
                .setCategory("ab")
                .setParentId(0L)
                .setId(11L)
                .build();
        Category category2 = new Category.Builder()
                .setCategory("ab")
                .setParentId(11L)
                .setId(12L)
                .build();
        Category category3 = new Category.Builder()
                .setCategory("ab")
                .setParentId(12L)
                .setId(13L)
                .build();
        Category category4 = new Category.Builder()
                .setCategory("ab")
                .setParentId(0L)
                .setId(10L)
                .build();
        Category category5 = new Category.Builder()
                .setCategory("ab")
                .setParentId(10L)
                .setId(11L)
                .build();
        Category category6 = new Category.Builder()
                .setCategory("a")
                .setParentId(11L)
                .setId(123L)
                .build();
        Category category7 = new Category.Builder()
                .setCategory("a")
                .setParentId(13L)
                .setId(18L)
                .build();
        Category category8 = new Category.Builder()
                .setCategory("a")
                .setParentId(13L)
                .setId(19L)
                .build();
        when(categoryService.getLeafToRootCategoryBranch(19L))
                .thenReturn(Arrays.asList(
                        category8,
                        category3,
                        category2,
                        category1,
                        rootCategory));
        when(categoryService.getLeafToRootCategoryBranch(18L))
                .thenReturn(Arrays.asList(
                        category7,
                        category3,
                        category2,
                        category1,
                        rootCategory));
        when(categoryService.getLeafToRootCategoryBranch(123L))
                .thenReturn(Arrays.asList(
                        category6,
                        category5,
                        category4,
                        rootCategory
                ));
        String date = "2021-01-01";
        List<DistributionOrderStatsDimensionAggregate> dtoList =
                distributionOrderStatsService.getDistributionOrderStatsAggregateByCategory(LocalDate.parse(date));

        Assertions.assertThat(dtoList)
                .usingRecursiveFieldByFieldElementComparator()
                .containsExactlyInAnyOrder(
                        DistributionOrderStatsDimensionAggregate.builder()
                                .setClid(123)
                                .setCategoryId(11)
                                .setCategoryName("ab")
                                .setDeliveryRegionId(10995L)
                                .setVid("")
                                .setPartnerPayment(BigDecimal.ONE)
                                .setPartnerPaymentMax(BigDecimal.ONE)
                                .setItemsBilledCost(BigDecimal.ONE)
                                .setItemsCount(2)
                                .build(),
                        DistributionOrderStatsDimensionAggregate.builder()
                                .setClid(123)
                                .setCategoryId(12)
                                .setCategoryName("ab")
                                .setDeliveryRegionId(10995L)
                                .setVid("")
                                .setPartnerPayment(BigDecimal.valueOf(2))
                                .setPartnerPaymentMax(BigDecimal.valueOf(2))
                                .setItemsBilledCost(BigDecimal.valueOf(2))
                                .setItemsCount(3)
                                .build(),
                        DistributionOrderStatsDimensionAggregate.builder()
                                .setClid(123)
                                .setCategoryId(1)
                                .setCategoryName("a")
                                .setDeliveryRegionId(10995L)
                                .setVid("")
                                .setPartnerPayment(BigDecimal.valueOf(123.23d))
                                .setPartnerPaymentMax(BigDecimal.valueOf(123.23d))
                                .setItemsBilledCost(BigDecimal.valueOf(213.12d))
                                .setItemsCount(2)
                                .build()
                );
    }

    private List<DistributionOrderStatsDimensionAggregate> getDistributionOrderStatsCategoryAggregates() {
        return List.of(
                DistributionOrderStatsDimensionAggregate.builder()
                        .setClid(123)
                        .setVid("")
                        .setCategoryId(1)
                        .setDeliveryRegionId(10995L)
                        .setPartnerPayment(BigDecimal.valueOf(123.23d))
                        .setPartnerPaymentMax(BigDecimal.valueOf(123.23d))
                        .setItemsBilledCost(BigDecimal.valueOf(213.12d))
                        .setItemsCount(2)
                        .build(),
                DistributionOrderStatsDimensionAggregate.builder()
                        .setClid(123)
                        .setVid("")
                        .setCategoryId(19)
                        .setDeliveryRegionId(10995L)
                        .setPartnerPayment(BigDecimal.ONE)
                        .setPartnerPaymentMax(BigDecimal.ONE)
                        .setItemsBilledCost(BigDecimal.ONE)
                        .setItemsCount(1)
                        .build(),
                DistributionOrderStatsDimensionAggregate.builder()
                        .setClid(123)
                        .setVid("")
                        .setCategoryId(18)
                        .setDeliveryRegionId(10995L)
                        .setPartnerPayment(BigDecimal.ONE)
                        .setPartnerPaymentMax(BigDecimal.ONE)
                        .setItemsBilledCost(BigDecimal.ONE)
                        .setItemsCount(2)
                        .build(),
                DistributionOrderStatsDimensionAggregate.builder()
                        .setClid(123)
                        .setVid("")
                        .setCategoryId(123)
                        .setDeliveryRegionId(10995L)
                        .setPartnerPayment(BigDecimal.ONE)
                        .setPartnerPaymentMax(BigDecimal.ONE)
                        .setItemsBilledCost(BigDecimal.ONE)
                        .setItemsCount(2)
                        .build()
        );
    }
}
