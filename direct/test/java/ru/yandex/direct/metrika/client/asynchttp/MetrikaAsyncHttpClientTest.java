package ru.yandex.direct.metrika.client.asynchttp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Lists;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.recursive.comparison.RecursiveComparisonConfiguration;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath;
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.CompareStrategy;
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies;
import ru.yandex.direct.currency.CurrencyCode;
import ru.yandex.direct.metrika.client.MetrikaClientException;
import ru.yandex.direct.metrika.client.internal.Attribution;
import ru.yandex.direct.metrika.client.internal.Dimension;
import ru.yandex.direct.metrika.client.internal.MetrikaByTimeStatisticsParams;
import ru.yandex.direct.metrika.client.internal.MetrikaSourcesParams;
import ru.yandex.direct.metrika.client.model.request.RetargetingGoal;
import ru.yandex.direct.metrika.client.model.request.RetargetingGoalGroup;
import ru.yandex.direct.metrika.client.model.response.Counter;
import ru.yandex.direct.metrika.client.model.response.CounterGoal;
import ru.yandex.direct.metrika.client.model.response.CounterInfoDirect;
import ru.yandex.direct.metrika.client.model.response.GoalConversionInfo;
import ru.yandex.direct.metrika.client.model.response.RetargetingCondition;
import ru.yandex.direct.metrika.client.model.response.Segment;
import ru.yandex.direct.metrika.client.model.response.TurnOnCallTrackingResponse;
import ru.yandex.direct.metrika.client.model.response.UpdateCounterGrantsResponse;
import ru.yandex.direct.metrika.client.model.response.UserCounters;
import ru.yandex.direct.metrika.client.model.response.UserCountersExtended;
import ru.yandex.direct.metrika.client.model.response.sources.TrafficSource;
import ru.yandex.direct.metrika.client.model.response.statistics.StatisticsResponse;
import ru.yandex.direct.metrika.client.model.response.statistics.StatisticsResponseRow;
import ru.yandex.direct.test.utils.differ.BigDecimalDiffer;
import ru.yandex.direct.tvm.TvmIntegration;
import ru.yandex.direct.tvm.TvmService;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;

public class MetrikaAsyncHttpClientTest {
    private static final String TICKET_BODY = "ticketBody";

    private static final CompareStrategy COUNTER_GOALS_COMPARE_STRATEGY = DefaultCompareStrategies.onlyExpectedFields()
            .forFields(BeanFieldPath.newPath("\\d+", "defaultPrice")).useDiffer(new BigDecimalDiffer());

    @Rule
    public final MockedMetrika mockedMetrikaForAsyncHttp = new MockedMetrika();
    @Rule
    public final ExpectedException exception = ExpectedException.none();
    private MetrikaAsyncHttpClient client;
    private TvmIntegration tvmIntegration;

    @Before
    public void setup() {
        tvmIntegration = mock(TvmIntegration.class);
        when(tvmIntegration.isEnabled()).thenReturn(true);
        when(tvmIntegration.getTicket(any())).thenReturn(TICKET_BODY);
        client = mockedMetrikaForAsyncHttp.createClient(tvmIntegration);
    }

    @Test
    public void testGetGoalsByUid() {
        Map<Long, List<RetargetingCondition>> result = client.getGoalsByUids(Lists.newArrayList(123L, 234L, 345L));
        Map<Long, List<RetargetingCondition>> expected = new HashMap<>();
        expected.put(123L, Lists.newArrayList(
                new RetargetingCondition().withId(1233456).withName("visit@1").withOwner(23456789)
                        .withType(RetargetingCondition.Type.GOAL).withCounterId(11111111)
                        .withCounterDomain("foobar.com").withCounterName("foobar.com-1"),
                new RetargetingCondition().withId(1233457).withName("visit@2").withOwner(23456789)
                        .withType(RetargetingCondition.Type.GOAL).withCounterId(11111111)
                        .withCounterDomain("foobar.com").withCounterName("foobar.com-1")));
        assertThat(result, beanDiffer(expected));
    }

    @Test
    public void testEstimateUsersByCondition() {
        List<RetargetingGoalGroup> condition = Lists.newArrayList(
                new RetargetingGoalGroup(RetargetingGoalGroup.Type.ALL,
                        Lists.newArrayList(new RetargetingGoal(4010108741L, 30))));
        long result = client.estimateUsersByCondition(condition);
        assertThat(result, is(42L));
    }

    @Test
    public void testEstimateUsersByComplexCondition() {
        List<RetargetingGoalGroup> condition = Lists.newArrayList(
                new RetargetingGoalGroup(RetargetingGoalGroup.Type.ALL,
                        Lists.newArrayList(new RetargetingGoal(4010108741L, 30), new RetargetingGoal(4010108742L, 14))),
                new RetargetingGoalGroup(RetargetingGoalGroup.Type.OR,
                        Lists.newArrayList(new RetargetingGoal(4010108743L, 30), new RetargetingGoal(4010108744L, 14))),
                new RetargetingGoalGroup(RetargetingGoalGroup.Type.NOT,
                        Lists.newArrayList(new RetargetingGoal(4010108745L, 30),
                                new RetargetingGoal(4010108746L, 14))));
        long result = client.estimateUsersByCondition(condition);
        assertThat(result, is(51119L));
    }

    @Test
    public void testEstimateUsersByIncorrectCondition() {
        exception.expect(MetrikaClientException.class);
        List<RetargetingGoalGroup> condition = Lists.newArrayList(
                new RetargetingGoalGroup(RetargetingGoalGroup.Type.ALL,
                        Lists.newArrayList(new RetargetingGoal(401010874100L, 30))));
        client.estimateUsersByCondition(condition);
    }

    @Test
    public void testGetProductImpressions() {
        Long counterId = 32233222L;
        Double result = client.getProductImpressionsByCounterId(Set.of(counterId), 14).get(counterId);
        assertThat(result, is(42.0));
    }

    @Test
    public void testGetAvailableSources() {
        var result = client.getAvailableSources(
                new MetrikaSourcesParams(
                        16705159L,
                        Attribution.CROSS_DEVICE_LAST_YANDEX_DIRECT_CLICK,
                        List.of("ad", "social"),
                        List.of("messenger.telegram"),
                        LocalDate.of(2022, Month.JANUARY, 17),
                        LocalDate.of(2022, Month.JANUARY, 17),
                        5L
                )
        );
        RecursiveComparisonConfiguration configuration = RecursiveComparisonConfiguration.builder().build();
        Assertions.assertThat(result.getItems())
                .usingRecursiveFieldByFieldElementComparator(configuration)
                .containsExactlyInAnyOrder(
                        new TrafficSource(
                                new Dimension("ad", "Ad traffic"),
                                new Dimension("ad.Яндекс: Директ", "Yandex: Direct")
                        ),
                        new TrafficSource(
                                new Dimension("social", "Social network traffic"),
                                new Dimension("social.instagram", "instagram.com")
                        ),
                        new TrafficSource(
                                new Dimension("messenger", "Messenger traffic"),
                                new Dimension("messenger.telegram", "Telegram")
                        )
                );
    }

    @Test
    public void testGetEndToEndStatisticsWithConversionRate() {
        StatisticsResponse result = client.getEndToEndStatistics(
                new MetrikaByTimeStatisticsParams(
                        null,
                        79204741L,
                        Attribution.CROSS_DEVICE_LAST_YANDEX_DIRECT_CLICK,
                        CurrencyCode.RUB,
                        null,
                        false,
                        true,
                        true,
                        LocalDate.of(2021, Month.MAY, 10),
                        LocalDate.of(2021, Month.MAY, 11),
                        null)
        );
        Assertions.assertThat(result.getCurrencyCode()).isEqualTo("RUB");
        var google = List.of(new Dimension("1.google", "google"));
        var other = List.of(new Dimension("1.other", "other"));
        var yandex = List.of(new Dimension("1.yandex", "yandex"));
        RecursiveComparisonConfiguration configuration = RecursiveComparisonConfiguration.builder().build();
        Assertions.assertThat(result.getRowset())
                .usingRecursiveFieldByFieldElementComparator(configuration)
                .containsExactly(
                        new StatisticsResponseRow(google, "2021-05-10", 10L, 0.0, 0L,
                                BigDecimal.valueOf(152621.0), BigDecimal.valueOf(152621.0)),
                        new StatisticsResponseRow(google, "2021-05-11", 10L, 20.0, 2L,
                                BigDecimal.valueOf(150927.0), BigDecimal.valueOf(150927.0)),
                        new StatisticsResponseRow(google, null, 20L, 10.0, 2L,
                                BigDecimal.valueOf(303548.0), BigDecimal.valueOf(303548.0)),
                        new StatisticsResponseRow(other, "2021-05-10", 100L, 31.0, 31L,
                                BigDecimal.valueOf(8878.0), BigDecimal.valueOf(8878.0)),
                        new StatisticsResponseRow(other, "2021-05-11", 110L, 10.0, 11L,
                                BigDecimal.valueOf(8942.0), BigDecimal.valueOf(8942.0)),
                        new StatisticsResponseRow(other, null, 210L, 20.0, 42L,
                                BigDecimal.valueOf(17820.0), BigDecimal.valueOf(17820.0)),
                        new StatisticsResponseRow(yandex, "2021-05-10", 1000L, 10.0, 100L,
                                BigDecimal.valueOf(125361.0), BigDecimal.valueOf(125361.0)),
                        new StatisticsResponseRow(yandex, "2021-05-11", 1010L, 10.0, 101L,
                                BigDecimal.valueOf(89432.0), BigDecimal.valueOf(89432.0)),
                        new StatisticsResponseRow(yandex, null, 2010L, 10.0, 201L,
                                BigDecimal.valueOf(214793.0), BigDecimal.valueOf(214793.0))
                );
    }

    @Test
    public void testGetEndToEndStatisticsWithGoal() {
        StatisticsResponse result = client.getEndToEndStatistics(
                new MetrikaByTimeStatisticsParams(
                        null,
                        79204741L,
                        Attribution.CROSS_DEVICE_LAST_YANDEX_DIRECT_CLICK,
                        CurrencyCode.RUB,
                        186218983L,
                        false,
                        true,
                        false,
                        LocalDate.of(2021, Month.MAY, 10),
                        LocalDate.of(2021, Month.MAY, 11),
                        null)
        );
        Assertions.assertThat(result.getCurrencyCode()).isEqualTo("RUB");
        var google = List.of(new Dimension("1.google", "google"));
        var other = List.of(new Dimension("1.other", "other"));
        var yandex = List.of(new Dimension("1.yandex", "yandex"));
        RecursiveComparisonConfiguration configuration = RecursiveComparisonConfiguration.builder().build();
        Assertions.assertThat(result.getRowset())
                .usingRecursiveFieldByFieldElementComparator(configuration)
                .containsExactly(
                        new StatisticsResponseRow(google, "2021-05-10", 10L, null, 5L,
                                BigDecimal.valueOf(152621.0), BigDecimal.valueOf(152621.0)),
                        new StatisticsResponseRow(google, "2021-05-11", 20L, null, 6L,
                                BigDecimal.valueOf(150927.0), BigDecimal.valueOf(150927.0)),
                        new StatisticsResponseRow(google, null, 30L, null, 11L,
                                BigDecimal.valueOf(303548.0), BigDecimal.valueOf(303548.0)),
                        new StatisticsResponseRow(other, "2021-05-10", 100L, null, 50L,
                                BigDecimal.valueOf(8878.0), BigDecimal.valueOf(8878.0)),
                        new StatisticsResponseRow(other, "2021-05-11", 110L, null, 51L,
                                BigDecimal.valueOf(8942.0), BigDecimal.valueOf(8942.0)),
                        new StatisticsResponseRow(other, null, 210L, null, 101L,
                                BigDecimal.valueOf(17820.0), BigDecimal.valueOf(17820.0)),
                        new StatisticsResponseRow(yandex, "2021-05-10", 1000L, null, 100L,
                                BigDecimal.valueOf(125361.0), BigDecimal.valueOf(125361.0)),
                        new StatisticsResponseRow(yandex, "2021-05-11", 1010L, null, 101L,
                                BigDecimal.valueOf(89432.0), BigDecimal.valueOf(89432.0)),
                        new StatisticsResponseRow(yandex, null, 2010L, null, 201L,
                                BigDecimal.valueOf(214793.0), BigDecimal.valueOf(214793.0))
                );
    }

    @Test
    public void testGetEndToEndStatisticsSkippingGoalData() {
        StatisticsResponse result = client.getEndToEndStatistics(
                new MetrikaByTimeStatisticsParams(
                        null,
                        79204741L,
                        Attribution.CROSS_DEVICE_LAST_YANDEX_DIRECT_CLICK,
                        CurrencyCode.RUB,
                        null,
                        true,
                        true,
                        true,
                        LocalDate.of(2021, Month.MAY, 10),
                        LocalDate.of(2021, Month.MAY, 11),
                        null)
        );
        Assertions.assertThat(result.getCurrencyCode()).isEqualTo("RUB");
        var google = List.of(new Dimension("1.google", "google"));
        var other = List.of(new Dimension("1.other", "other"));
        var yandex = List.of(new Dimension("1.yandex", "yandex"));
        RecursiveComparisonConfiguration configuration = RecursiveComparisonConfiguration.builder().build();
        Assertions.assertThat(result.getRowset())
                .usingRecursiveFieldByFieldElementComparator(configuration)
                .containsExactly(
                        new StatisticsResponseRow(google, "2021-05-10", 10L, null, null,
                                BigDecimal.valueOf(152621.0), null),
                        new StatisticsResponseRow(google, "2021-05-11", 20L, null, null,
                                BigDecimal.valueOf(150927.0), null),
                        new StatisticsResponseRow(google, null, 30L, null, null,
                                BigDecimal.valueOf(303548.0), null),
                        new StatisticsResponseRow(other, "2021-05-10", 100L, null, null,
                                BigDecimal.valueOf(8878.0), null),
                        new StatisticsResponseRow(other, "2021-05-11", 110L, null, null,
                                BigDecimal.valueOf(8942.0), null),
                        new StatisticsResponseRow(other, null, 210L, null, null,
                                BigDecimal.valueOf(17820.0), null),
                        new StatisticsResponseRow(yandex, "2021-05-10", 1000L, null, null,
                                BigDecimal.valueOf(125361.0), null),
                        new StatisticsResponseRow(yandex, "2021-05-11", 1010L, null, null,
                                BigDecimal.valueOf(89432.0), null),
                        new StatisticsResponseRow(yandex, null, 2010L, null, null,
                                BigDecimal.valueOf(214793.0), null)
                );
    }

    @Test
    public void testGetEndToEndStatisticsWithChiefLogin() {
        StatisticsResponse result = client.getEndToEndStatistics(
                new MetrikaByTimeStatisticsParams(
                        "chief_login",
                        79204741L,
                        Attribution.CROSS_DEVICE_LAST_YANDEX_DIRECT_CLICK,
                        CurrencyCode.RUB,
                        null,
                        false,
                        true,
                        false,
                        LocalDate.of(2021, Month.MAY, 10),
                        LocalDate.of(2021, Month.MAY, 11),
                        null)
        );
        Assertions.assertThat(result.getCurrencyCode()).isEqualTo("RUB");
        var google = List.of(new Dimension("1.google", "google"));
        var other = List.of(new Dimension("1.other", "other"));
        var yandex = List.of(new Dimension("1.yandex", "yandex"));
        RecursiveComparisonConfiguration configuration = RecursiveComparisonConfiguration.builder().build();
        Assertions.assertThat(result.getRowset())
                .usingRecursiveFieldByFieldElementComparator(configuration)
                .containsExactly(
                        new StatisticsResponseRow(google, "2021-05-10", 10L, null, null,
                                BigDecimal.valueOf(152621.0), BigDecimal.valueOf(152621.0)),
                        new StatisticsResponseRow(google, "2021-05-11", 20L, null, null,
                                BigDecimal.valueOf(150927.0), BigDecimal.valueOf(150927.0)),
                        new StatisticsResponseRow(google, null, 30L, null, null,
                                BigDecimal.valueOf(303548.0), BigDecimal.valueOf(303548.0)),
                        new StatisticsResponseRow(other, "2021-05-10", 100L, null, null,
                                BigDecimal.valueOf(8878.0), BigDecimal.valueOf(8878.0)),
                        new StatisticsResponseRow(other, "2021-05-11", 110L, null, null,
                                BigDecimal.valueOf(8942.0), BigDecimal.valueOf(8942.0)),
                        new StatisticsResponseRow(other, null, 210L, null, null,
                                BigDecimal.valueOf(17820.0), BigDecimal.valueOf(17820.0)),
                        new StatisticsResponseRow(yandex, "2021-05-10", 1000L, null, null,
                                BigDecimal.valueOf(125361.0), BigDecimal.valueOf(125361.0)),
                        new StatisticsResponseRow(yandex, "2021-05-11", 1010L, null, null,
                                BigDecimal.valueOf(89432.0), BigDecimal.valueOf(89432.0)),
                        new StatisticsResponseRow(yandex, null, 2010L, null, null,
                                BigDecimal.valueOf(214793.0), BigDecimal.valueOf(214793.0))
                );
    }

    @Test
    public void testGetEndToEndStatisticsWithoutRevenue() {
        StatisticsResponse result = client.getEndToEndStatistics(
                new MetrikaByTimeStatisticsParams(
                        null,
                        79204741L,
                        Attribution.CROSS_DEVICE_LAST_YANDEX_DIRECT_CLICK,
                        CurrencyCode.RUB,
                        null,
                        false,
                        false,
                        false,
                        LocalDate.of(2021, Month.MAY, 10),
                        LocalDate.of(2021, Month.MAY, 11),
                        null)
        );
        Assertions.assertThat(result.getCurrencyCode()).isEqualTo("RUB");
        var google = List.of(new Dimension("1.google", "google"));
        var other = List.of(new Dimension("1.other", "other"));
        var yandex = List.of(new Dimension("1.yandex", "yandex"));
        RecursiveComparisonConfiguration configuration = RecursiveComparisonConfiguration.builder().build();
        Assertions.assertThat(result.getRowset())
                .usingRecursiveFieldByFieldElementComparator(configuration)
                .containsExactly(
                        new StatisticsResponseRow(google, "2021-05-10", 10L, null, null,
                                BigDecimal.valueOf(152621.0), null),
                        new StatisticsResponseRow(google, "2021-05-11", 20L, null, null,
                                BigDecimal.valueOf(150927.0), null),
                        new StatisticsResponseRow(google, null, 30L, null, null,
                                BigDecimal.valueOf(303548.0), null),
                        new StatisticsResponseRow(other, "2021-05-10", 100L, null, null,
                                BigDecimal.valueOf(8878.0), null),
                        new StatisticsResponseRow(other, "2021-05-11", 110L, null, null,
                                BigDecimal.valueOf(8942.0), null),
                        new StatisticsResponseRow(other, null, 210L, null, null,
                                BigDecimal.valueOf(17820.0), null),
                        new StatisticsResponseRow(yandex, "2021-05-10", 1000L, null, null,
                                BigDecimal.valueOf(125361.0), null),
                        new StatisticsResponseRow(yandex, "2021-05-11", 1010L, null, null,
                                BigDecimal.valueOf(89432.0), null),
                        new StatisticsResponseRow(yandex, null, 2010L, null, null,
                                BigDecimal.valueOf(214793.0), null)
                );
    }

    @Test
    public void testGetTrafficSourceStatistics() {
        var result = client.getTrafficSourceStatistics(
                new MetrikaByTimeStatisticsParams(
                        null,
                        16705159L,
                        Attribution.CROSS_DEVICE_LAST_YANDEX_DIRECT_CLICK,
                        CurrencyCode.BYN,
                        1820101L,
                        false,
                        true,
                        false,
                        LocalDate.of(2022, Month.JANUARY, 17),
                        LocalDate.of(2022, Month.JANUARY, 17),
                        null)
        );
        Assertions.assertThat(result.getCurrencyCode()).isEqualTo("BYN");
        var ad = List.of(new Dimension("ad", "Ad traffic"));
        var organic = List.of(new Dimension("organic", "Search engine traffic"));
        var direct = List.of(new Dimension("direct", "Direct traffic"));
        var internal = List.of(new Dimension("internal", "Internal traffic"));
        RecursiveComparisonConfiguration configuration = RecursiveComparisonConfiguration.builder().build();
        Assertions.assertThat(result.getRowset())
                .usingRecursiveFieldByFieldElementComparator(configuration)
                .containsExactly(
                        new StatisticsResponseRow(ad, "2022-01-17", 103424L, null, 263421L,
                                null, BigDecimal.valueOf(553590.1)),
                        new StatisticsResponseRow(ad, null, 103424L, null, 263421L,
                                null, BigDecimal.valueOf(553590.1)),
                        new StatisticsResponseRow(organic, "2022-01-17", 69000L, null, 137352L,
                                null, BigDecimal.valueOf(552283.339998)),
                        new StatisticsResponseRow(organic, null, 69000L, null, 137352L,
                                null, BigDecimal.valueOf(552283.339998)),
                        new StatisticsResponseRow(direct, "2022-01-17", 6599L, null, 7013L,
                                null, BigDecimal.valueOf(71226.019999)),
                        new StatisticsResponseRow(direct, null, 6599L, null, 7013L,
                                null, BigDecimal.valueOf(71226.019999)),
                        new StatisticsResponseRow(internal, "2022-01-17", 3197L, null, 6252L,
                                null, BigDecimal.valueOf(49577.279999)),
                        new StatisticsResponseRow(internal, null, 3197L, null, 6252L,
                                null, BigDecimal.valueOf(49577.279999))
                );
    }

    @Test
    public void testGetTrafficSourceStatisticsWithRowIds() {
        var result = client.getTrafficSourceStatistics(
                new MetrikaByTimeStatisticsParams(
                        null,
                        16705159L,
                        Attribution.CROSS_DEVICE_LAST_YANDEX_DIRECT_CLICK,
                        CurrencyCode.BYN,
                        1820101L,
                        false,
                        true,
                        false,
                        LocalDate.of(2022, Month.JANUARY, 17),
                        LocalDate.of(2022, Month.JANUARY, 17),
                        List.of(List.of("ad", "ad.Google Ads"), List.of("organic")))
        );
        Assertions.assertThat(result.getCurrencyCode()).isEqualTo("BYN");
        var google = List.of(
                new Dimension("ad", "Ad traffic"),
                new Dimension("ad.Google Ads", "Google Ads")
        );
        var organic = List.of(new Dimension("organic", "Search engine traffic"));
        RecursiveComparisonConfiguration configuration = RecursiveComparisonConfiguration.builder().build();
        Assertions.assertThat(result.getRowset())
                .usingRecursiveFieldByFieldElementComparator(configuration)
                .containsExactly(
                        new StatisticsResponseRow(google, "2022-01-17", 103424L, null, 263421L,
                                null, BigDecimal.valueOf(553590.1)),
                        new StatisticsResponseRow(google, null, 103424L, null, 263421L,
                                null, BigDecimal.valueOf(553590.1)),
                        new StatisticsResponseRow(organic, "2022-01-17", 69000L, null, 137352L,
                                null, BigDecimal.valueOf(552283.339998)),
                        new StatisticsResponseRow(organic, null, 69000L, null, 137352L,
                                null, BigDecimal.valueOf(552283.339998))
                );
    }

    @Test
    public void getConversionVisitsCountByGoalIdForCounterIds() {
        var conversionVisitsCountByGoalIdForCounterIds = client.getGoalsConversionInfoByCounterIds(List.of(32233222),
                14);
        Map<Long, GoalConversionInfo> expected = Map.of(
                30606879L, new GoalConversionInfo(30606879L, 420L, true),
                41646742L, new GoalConversionInfo(41646742L, 155L, true),
                30606889L, new GoalConversionInfo(30606889L, 101L, false),
                30606884L, new GoalConversionInfo(30606884L, 1L, false)
        );
        assertThat(conversionVisitsCountByGoalIdForCounterIds, beanDiffer(expected));
    }

    @Test
    public void getGoalsStatistics() {
        var goalsStatistics = client.getGoalsStatistics(
                List.of(32233222),
                LocalDate.of(2016, 6, 13),
                LocalDate.of(2016, 6, 27));
        Map<Long, Long> expected = Map.of(
                30606879L, 420L,
                41646742L, 155L,
                30606889L, 101L,
                30606884L, 1L
        );
        assertThat(goalsStatistics, beanDiffer(expected));
    }

    @Test
    public void testGetCounterGoals() {
        Map<Integer, List<CounterGoal>> counterToGoals = client.getMassCountersGoalsFromMetrika(Set.of(323322));
        List<CounterGoal> expected =
                Lists.newArrayList(getCounterGoal(123, "url_goal_name", CounterGoal.Type.URL, BigDecimal.ZERO),
                        getCounterGoal(125, "number_goal_name", CounterGoal.Type.NUMBER, BigDecimal.TEN),
                        getCounterGoal(127, "step_goal_name", CounterGoal.Type.STEP, BigDecimal.ZERO),
                        getCounterGoal(129, "action_goal_name", CounterGoal.Type.ACTION, BigDecimal.ZERO),
                        getCounterGoal(131, "offline_goal_name", CounterGoal.Type.OFFLINE, BigDecimal.ZERO),
                        getCounterGoal(133, "call_goal_name", CounterGoal.Type.CALL, BigDecimal.ZERO),
                        getCounterGoal(135, "phone_goal_name", CounterGoal.Type.PHONE, BigDecimal.ZERO),
                        getCounterGoal(137, "email_goal_name", CounterGoal.Type.EMAIL, BigDecimal.ZERO),
                        getCounterGoal(139, "form_goal_name", CounterGoal.Type.FORM, BigDecimal.ZERO),
                        getCounterGoal(141, "cdp_order_in_progress_goal_name",
                                CounterGoal.Type.CDP_ORDER_IN_PROGRESS, BigDecimal.ZERO),
                        getCounterGoal(143, "cdp_order_paid_goal_name", CounterGoal.Type.CDP_ORDER_PAID,
                                BigDecimal.ZERO),
                        getCounterGoal(145, "messenger_goal_name", CounterGoal.Type.MESSENGER, BigDecimal.ZERO),
                        getCounterGoal(147, "file_goal_name", CounterGoal.Type.FILE, BigDecimal.ZERO),
                        getCounterGoal(149, "search_goal_name", CounterGoal.Type.SEARCH, BigDecimal.ZERO),
                        getCounterGoal(151, "button_goal_name", CounterGoal.Type.BUTTON, BigDecimal.ZERO),
                        getCounterGoal(153, "e_cart_goal_name", CounterGoal.Type.E_CART, BigDecimal.ZERO),
                        getCounterGoal(155, "e_purchase_goal_name", CounterGoal.Type.E_PURCHASE, BigDecimal.ZERO),
                        getCounterGoal(157, "a_cart_goal_name", CounterGoal.Type.A_CART, BigDecimal.ZERO),
                        getCounterGoal(159, "a_purchase_goal_name", CounterGoal.Type.A_PURCHASE, BigDecimal.ZERO),
                        getCounterGoal(161, "conditional_call_goal_name", CounterGoal.Type.CONDITIONAL_CALL,
                                BigDecimal.ZERO),
                        getCounterGoal(163, "social_goal_name", CounterGoal.Type.SOCIAL, BigDecimal.ZERO),
                        getCounterGoal(165, "payment_system_goal_name", CounterGoal.Type.PAYMENT_SYSTEM,
                                BigDecimal.ZERO),
                        getCounterGoal(166, "contact_data_goal_name", CounterGoal.Type.CONTACT_DATA,
                                BigDecimal.ZERO),
                        getCounterGoal(167, "url_goal_name", CounterGoal.Type.URL, CounterGoal.Source.AUTO,
                                BigDecimal.ZERO));
        assertThat(counterToGoals.get(323322), beanDiffer(expected).useCompareStrategy(COUNTER_GOALS_COMPARE_STRATEGY));
    }

    public static CounterGoal getCounterGoal(int goalId, String goalName, CounterGoal.Type goalType,
                                             BigDecimal defaultPrice) {
        return getCounterGoal(goalId, goalName, goalType, CounterGoal.Source.USER, defaultPrice);
    }

    public static CounterGoal getCounterGoal(int goalId, String goalName, CounterGoal.Type goalType,
                                             CounterGoal.Source goalSource, BigDecimal defaultPrice) {
        return new CounterGoal()
                .withId(goalId)
                .withName(goalName)
                .withType(goalType)
                .withSource(goalSource)
                .withDefaultPrice(defaultPrice);
    }

    @Test
    public void testGetCounterGoalsWithUnknownGoalType() {
        Map<Integer, List<CounterGoal>> counterToGoals = client.getMassCountersGoalsFromMetrika(Set.of(323323));
        List<CounterGoal> expected =
                Lists.newArrayList(
                        getCounterGoal(1234, "url_goal_name", CounterGoal.Type.URL, BigDecimal.ZERO)
                );
        assertThat(counterToGoals.get(323323), beanDiffer(expected).useCompareStrategy(COUNTER_GOALS_COMPARE_STRATEGY));
    }

    @Test
    public void testGetCounterGoalsWithUnknownGoalSource() {
        Map<Integer, List<CounterGoal>> counterToGoals = client.getMassCountersGoalsFromMetrika(Set.of(323324));
        List<CounterGoal> expected =
                Lists.newArrayList(
                        getCounterGoal(2234, "url_goal_name", CounterGoal.Type.URL, BigDecimal.ZERO)
                );
        assertThat(counterToGoals.get(323324), beanDiffer(expected).useCompareStrategy(COUNTER_GOALS_COMPARE_STRATEGY));
    }

    @Test
    public void testGetUsersCountersNum() {
        List<UserCounters> response = client.getUsersCountersNum(Lists.newArrayList(123L, 234L));
        List<UserCounters> expected = Lists.newArrayList(
                new UserCounters().withOwner(123L).withCountersCnt(42).withCounterIds(Lists.newArrayList(111, 222)),
                new UserCounters().withOwner(234L).withCountersCnt(51)
                        .withCounterIds(Lists.newArrayList(333, 444, 555)));
        assertThat(response, beanDiffer(expected));
    }

    @Test
    public void testGetUsersCountersNumExtended() {
        List<UserCountersExtended> response = client.getUsersCountersNumExtended(List.of(123L, 234L));
        List<CounterInfoDirect> firstUidCounters = List.of(
                new CounterInfoDirect()
                        .withId(111)
                        .withName("name_111")
                        .withSitePath("site_path_111")
                        .withCounterPermission("edit")
                        .withEcommerce(true)
                        .withCounterSource("turbodirect"),
                new CounterInfoDirect()
                        .withId(222)
                        .withName("name_222")
                        .withSitePath("site_path_222")
                        .withCounterPermission("own")
                        .withEcommerce(false)
                        .withCounterSource("sprav"));
        List<CounterInfoDirect> secondUidCounters = List.of(
                new CounterInfoDirect()
                        .withId(333)
                        .withName("name_333")
                        .withSitePath("site_path_333")
                        .withEcommerce(true)
                        .withCounterPermission("view"),
                new CounterInfoDirect()
                        .withId(444)
                        .withName("name_444")
                        .withSitePath("site_path_444")
                        .withCounterSource("sprav")
                        .withEcommerce(false)
                        .withCounterPermission("edit"),
                new CounterInfoDirect()
                        .withId(555)
                        .withName("name_555")
                        .withEcommerce(false)
                        .withSitePath("site_path_555"),
                new CounterInfoDirect()
                        .withId(666)
                        .withName("name_666")
                        .withSitePath("site_path_666")
                        .withCounterPermission("own")
                        .withEcommerce(false)
                        .withCounterSource("system"),
                new CounterInfoDirect()
                        .withId(777)
                        .withName("name_777")
                        .withSitePath("site_path_777")
                        .withCounterPermission("view")
                        .withEcommerce(false)
                        .withCounterSource("partner"),
                new CounterInfoDirect()
                        .withId(888)
                        .withName("name_888")
                        .withSitePath("site_path_888")
                        .withCounterPermission("own")
                        .withEcommerce(false)
                        .withCounterSource("market"),
                new CounterInfoDirect()
                        .withId(999)
                        .withName("name_999")
                        .withSitePath("site_path_999")
                        .withCounterPermission("view")
                        .withEcommerce(false)
                        .withCounterSource("eda"));

        List<UserCountersExtended> expected = Lists.newArrayList(
                new UserCountersExtended()
                        .withOwner(123L)
                        .withCountersCnt(42)
                        .withCounters(firstUidCounters),
                new UserCountersExtended()
                        .withOwner(234L)
                        .withCountersCnt(51)
                        .withCounters(secondUidCounters));
        assertThat(response, beanDiffer(expected));
    }

    @Test
    public void testUpdateCounterGrantsSuccess() {
        Set<String> logins = new LinkedHashSet<>(asList("login1", "login2"));
        UpdateCounterGrantsResponse response = client.updateCounterGrants(1234567L, logins);
        assertThat(response.isSuccessful(), is(true));
    }

    @Test
    public void testUpdateCounterGrantsFailed() {
        Set<String> logins = new LinkedHashSet<>(asList("login1", "login2"));
        UpdateCounterGrantsResponse response = client.updateCounterGrants(890L, logins);
        assertThat(response.isSuccessful(), is(false));
    }

    @Test
    public void testTurnOnCallTracking() {
        TurnOnCallTrackingResponse turnOnCallTrackingResponse = client.turnOnCallTracking(1234567L);
        TurnOnCallTrackingResponse expected = new TurnOnCallTrackingResponse()
                .withGoal(new CounterGoal()
                        .withId(1111)
                        .withName("Звонок")
                        .withType(CounterGoal.Type.CALL));
        assertThat(turnOnCallTrackingResponse, beanDiffer(expected));
    }

    @Test
    public void testGetTicketIsCalled() {
        // Calling intapi, audience, and metric
        client.getUsersCountersNum(asList(123L, 234L));
        client.getGoalsByUids(Lists.newArrayList(123L, 234L, 345L));
        client.getProductImpressionsByCounterId(Set.of(32233222L), 14);

        verify(tvmIntegration).getTicket(TvmService.METRIKA_INTERNAL_API_PROD);
        verify(tvmIntegration).getTicket(TvmService.METRIKA_AUDIENCE_PROD);
        verify(tvmIntegration).getTicket(TvmService.METRIKA_API_PROD);
    }

    @Test
    public void testGetEditableCounters() {
        var expected = List.of(
                new Counter().withId(1).withDomain("ya.ru"),
                new Counter().withId(2).withDomain("yandex.ru"),
                new Counter().withId(3).withDomain("yandex.net")
        );
        var actual = client.getEditableCounters(null);
        assertThat(actual, is(equalTo(expected)));
    }

    @Test
    public void testGetCounter() {
        int counterId = 20220126;

        Counter actual = client.getCounter((long) counterId);

        Counter expected = new Counter().withId(counterId).withDomain("ya.ru").withFeatures(Set.of("ecommerce"));
        Assertions.assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void testGetSegments() {
        var segment1 = new Segment()
                .withId(1)
                .withCounterId(1234567)
                .withName("Новые посетители")
                .withExpression("ym:s:isNewUser=='Yes'");
        var segment2 = new Segment()
                .withId(2)
                .withCounterId(1234567)
                .withName("Отказы")
                .withExpression("ym:s:bounce=='Yes'");
        var expected = List.of(segment1, segment2);

        var actual = client.getSegments(1234567, null);

        assertThat(actual, beanDiffer(expected));
    }

    @Test
    public void testCreateSegment() {
        var expected = new Segment()
                .withId(1)
                .withCounterId(1234567)
                .withName("Неотказы")
                .withExpression("ym:s:bounce=='No'");
        var actual = client.createSegment(1234567, "Неотказы", "ym:s:bounce=='No'", null);
        assertThat(actual, beanDiffer(expected));
    }
}
