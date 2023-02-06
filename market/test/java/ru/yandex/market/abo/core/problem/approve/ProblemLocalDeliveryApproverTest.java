package ru.yandex.market.abo.core.problem.approve;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import one.util.streamex.StreamEx;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.common.util.currency.Currency;
import ru.yandex.market.abo.core.deliverycalculator.db.DeliveryCalculatorInfoSnapshot;
import ru.yandex.market.abo.core.feed.model.DbFeedOfferDelivery;
import ru.yandex.market.abo.core.feed.model.DbFeedOfferDetails;
import ru.yandex.market.abo.core.feed.search.task.model.FeedSearchTask;
import ru.yandex.market.abo.core.feed.search.task.model.SearchTaskTarget;
import ru.yandex.market.abo.core.offer.report.Offer;
import ru.yandex.market.abo.core.problem.model.Problem;
import ru.yandex.market.abo.core.problem.model.ProblemTypeId;
import ru.yandex.market.abo.core.shop.CommonShopInfoService;
import ru.yandex.market.abo.core.shop.ShopInfoService;
import ru.yandex.market.abo.core.ticket.LocalDeliveryPriceService;
import ru.yandex.market.abo.core.ticket.ProblemService;
import ru.yandex.market.abo.core.ticket.model.LocalDeliveryProblem;
import ru.yandex.market.abo.util.idx.ServiceAvailabilityConfig;
import ru.yandex.market.common.report.model.LocalDeliveryOption;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static ru.yandex.market.abo.core.problem.approve.ProblemLocalDeliveryApprover.DELIVERY_CALC_THRESHOLD_HOURS;

/**
 * @author komarovns
 * @date 31.01.19
 */
class ProblemLocalDeliveryApproverTest {
    private static final long MOSCOW = 213;
    private static final long KAZAN = 43;

    private static final int PRICE_1 = 3;
    private static final int DAY_FROM_1 = 5;
    private static final int DAY_TO_1 = 6;

    private static final int PRICE_2 = 30;
    private static final int DAY_FROM_2 = 10;
    private static final int DAY_TO_2 = 20;

    private final List<DbFeedOfferDelivery> NEW_OPTIONS = Arrays.asList(
            createDeliveryOption(DAY_FROM_1, DAY_TO_1, PRICE_1),
            createDeliveryOption(DAY_FROM_2, DAY_TO_2, PRICE_2)
    );

    @Mock
    LocalDeliveryPriceService localDeliveryPriceService;
    @Mock
    CommonShopInfoService commonShopInfoService;
    @Mock
    ShopInfoService shopInfoService;
    @Mock
    ProblemService problemService;
    @Mock
    DbFeedOfferDetails offer;
    @Mock
    Offer storedOffer;
    @Mock
    FeedSearchTask task;
    @Mock
    Problem problem;
    @Mock
    ServiceAvailabilityConfig serviceAvailabilityConfig;

    @InjectMocks
    ProblemLocalDeliveryApprover localDeliveryApprover;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(shopInfoService.getShopOwnRegion(anyLong())).thenReturn(MOSCOW);
        when(storedOffer.getShopId()).thenReturn(1L);
        when(offer.getDeliveryOptions()).thenReturn(NEW_OPTIONS);
        when(task.getOffer()).thenReturn(offer);
        when(task.getDeliveryCalculatorInfoSnapshot()).thenReturn(new DeliveryCalculatorInfoSnapshot(
                true, LocalDateTime.MIN, LocalDateTime.MIN, task
        ));
    }

    @ParameterizedTest
    @EnumSource(SearchTaskTarget.class)
    void sameOption(SearchTaskTarget taskTarget) {
        when(task.getTarget()).thenReturn(taskTarget);
        mockProblemOptionDetails(DAY_FROM_2, DAY_TO_2, PRICE_2);
        when(problem.getProblemTypeId()).thenReturn(ProblemTypeId.BAD_DELIVERY_PRICE);
        assertTrue(localDeliveryApprover.approve(problem, storedOffer, task, null));
    }

    @ParameterizedTest
    @EnumSource(SearchTaskTarget.class)
    void testLowerPriceAndDate(SearchTaskTarget taskTarget) {
        when(task.getTarget()).thenReturn(taskTarget);
        mockProblemOptionDetails(DAY_TO_1 - 1, PRICE_1 - 1);
        when(problem.getProblemTypeId()).thenReturn(ProblemTypeId.BAD_DELIVERY_PRICE);
        assertFalse(localDeliveryApprover.approve(problem, storedOffer, task, null));
    }

    @ParameterizedTest
    @EnumSource(SearchTaskTarget.class)
    void testHigherPriceAndDate(SearchTaskTarget taskTarget) {
        when(task.getTarget()).thenReturn(taskTarget);
        mockProblemOptionDetails(DAY_TO_2 + 10, PRICE_2 + 10);
        when(problem.getProblemTypeId()).thenReturn(ProblemTypeId.BAD_DELIVERY_PRICE);
        assertTrue(localDeliveryApprover.approve(problem, storedOffer, task, null));
    }

    @ParameterizedTest
    @MethodSource("testReportDayToIsNullMethodSource")
    void testReportDayToIsNull(SearchTaskTarget taskTarget, int initialPrice) {
        when(task.getTarget()).thenReturn(taskTarget);
        mockProblemOptionDetails(null, initialPrice);
        when(problem.getProblemTypeId()).thenReturn(ProblemTypeId.BAD_DELIVERY_PRICE);
        assertEquals(initialPrice >= PRICE_1, localDeliveryApprover.approve(problem, storedOffer, task, null));
    }

    private static Stream<Arguments> testReportDayToIsNullMethodSource() {
        return StreamEx.of(SearchTaskTarget.values())
                .cross(PRICE_1 - 1, PRICE_1, PRICE_2 + 1)
                .mapKeyValue((target, price) -> Arguments.of(target, price));
    }

    @ParameterizedTest
    @CsvSource({"true,IDX_API", "false,IDX_API", "true,DATA_CAMP", "false,DATA_CAMP"})
    void testDateProblemAndInMBI(boolean offerAvailable, SearchTaskTarget taskTarget) {
        when(task.getTarget()).thenReturn(taskTarget);
        mockProblemOptionDetails(DAY_FROM_1, DAY_TO_1, PRICE_1);
        when(problem.getProblemTypeId()).thenReturn(ProblemTypeId.BAD_DELIVERY_DATE);
        when(commonShopInfoService.deliveryOptionsSetInPI(anyLong())).thenReturn(true);
        when(offer.getAvailable()).thenReturn(offerAvailable);
        assertEquals(offerAvailable, localDeliveryApprover.approve(problem, storedOffer, task, null));
    }

    @ParameterizedTest
    @MethodSource("testDeliveryCalcMethodSource")
    void testDeliveryCalc(boolean approve, boolean useYml, boolean sameRegion, boolean wasRecentlyUpdated) {
        when(task.getTarget()).thenReturn(SearchTaskTarget.DATA_CAMP);
        mockProblemOptionDetails(DAY_FROM_2, DAY_TO_2, PRICE_2);
        when(problem.getProblemTypeId()).thenReturn(ProblemTypeId.BAD_DELIVERY_PRICE);
        when(offer.getDeliveryOptions()).thenReturn(Collections.emptyList());
        when(shopInfoService.getShopOwnRegion(anyLong())).thenReturn(sameRegion ? MOSCOW : KAZAN);
        var deliveryCalcUpdateTime = LocalDateTime.now().minusHours(DELIVERY_CALC_THRESHOLD_HOURS)
                .plusMinutes(wasRecentlyUpdated ? +10 : -10);
        when(task.getDeliveryCalculatorInfoSnapshot()).thenReturn(new DeliveryCalculatorInfoSnapshot(
                useYml, deliveryCalcUpdateTime, deliveryCalcUpdateTime, task
        ));
        assertEquals(approve, localDeliveryApprover.approve(problem, storedOffer, task, null));
    }

    private static Stream<Arguments> testDeliveryCalcMethodSource() {
        return StreamEx.cartesianPower(3, List.of(true, false)).map(args -> {
            var useYml = args.get(0);
            var sameRegion = args.get(1);
            var wasRecentlyUpdated = args.get(2);
            var approve = !(useYml && sameRegion) && !wasRecentlyUpdated;
            return Arguments.of(approve, useYml, sameRegion, wasRecentlyUpdated);
        });
    }

    private void mockProblemOptionDetails(Integer dayTo, double cost) {
        mockProblemOptionDetails(100, dayTo, cost);
    }

    private void mockProblemOptionDetails(Integer dayFrom, Integer dayTo, double cost) {
        LocalDeliveryProblem problemOptionDetails = new LocalDeliveryProblem();
        problemOptionDetails.setReportOption(createLocalDeliveryOption(dayFrom, dayTo, cost));
        problemOptionDetails.setAssessorOption(new LocalDeliveryOption());
        problemOptionDetails.setRegionId(MOSCOW);
        when(localDeliveryPriceService.loadLocalDeliveryByProblemId(anyLong()))
                .thenReturn(Collections.singletonList(problemOptionDetails));
    }

    private static LocalDeliveryOption createLocalDeliveryOption(Integer dayFrom, Integer dayTo, double cost) {
        LocalDeliveryOption oldOption = new LocalDeliveryOption();
        oldOption.setDayFrom(dayFrom);
        oldOption.setDayTo(dayTo);
        oldOption.setCost(new BigDecimal(cost));
        oldOption.setCurrency(Currency.RUR);
        oldOption.setOrderBefore(24);
        return oldOption;
    }

    private static DbFeedOfferDelivery createDeliveryOption(Integer dayFrom, Integer dayTo, double price) {
        var option = new DbFeedOfferDelivery();
        option.setDayFrom(dayFrom);
        option.setDayTo(dayTo);
        option.setPrice(new BigDecimal(price));
        option.setCurrency(Currency.RUR);
        option.setOrderBefore(24);
        option.setRegionId(MOSCOW);
        option.setRequestedRegionId(MOSCOW);
        return option;
    }
}
