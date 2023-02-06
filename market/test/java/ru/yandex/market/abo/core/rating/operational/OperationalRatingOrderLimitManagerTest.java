package ru.yandex.market.abo.core.rating.operational;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.market.abo.core.exception.ExceptionalShopReason;
import ru.yandex.market.abo.core.exception.ExceptionalShopsService;
import ru.yandex.market.abo.core.rating.partner.PartnerRatingActual;
import ru.yandex.market.abo.core.rating.partner.PartnerRatingService;
import ru.yandex.market.abo.core.rating.partner.order_limit.PartnerRatingLimitRange;
import ru.yandex.market.abo.core.rating.partner.order_limit.PartnerRatingLimitRangeService;
import ru.yandex.market.abo.core.rating.partner.order_limit.PartnerRatingLimitType;
import ru.yandex.market.abo.cpa.MbiApiService;
import ru.yandex.market.abo.cpa.order.limit.CpaOrderLimitService;
import ru.yandex.market.abo.cpa.order.limit.count.CpaOrderCountService;
import ru.yandex.market.abo.cpa.order.limit.model.CpaOrderLimit;
import ru.yandex.market.abo.cpa.order.model.PartnerModel;
import ru.yandex.market.core.abo.AboCutoff;
import ru.yandex.market.util.db.ConfigurationService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static ru.yandex.market.abo.core.rating.operational.OperationalRatingOrderLimitManager.DAYS_PERIOD_FOR_AVG_ORDERS_CALCULATION;
import static ru.yandex.market.abo.core.rating.operational.OperationalRatingOrderLimitManager.ORDERS_LOWER_BOUND;

/**
 * @author Aleksei Neliubin (neliubin@yandex-team.ru)
 * @date 10.06.2020
 */
class OperationalRatingOrderLimitManagerTest {

    private static final long SHOP_ID = 123L;
    private static final PartnerModel PARTNER_MODEL = PartnerModel.DSBB;
    private static final long DEFAULT_DAYS_AVG_OFFERS_COUNT = 5;

    private static final double MEDIUM_RATING_LOWER_BOUND = 40.0;
    private static final double HIGH_RATING_LOWER_BOUND = 95.0;
    private static final double ORDERS_PART_FOR_LIMIT = 0.5;

    private static final int LIMIT_LOWER_BOUND = 1;
    private static final int LOW_RATING_ORDER_LIMIT = 0;

    @InjectMocks
    private OperationalRatingOrderLimitManager operationalRatingOrderLimitManager;

    @Mock
    private PartnerRatingService partnerRatingService;
    @Mock
    private CpaOrderLimitService orderLimitService;
    @Mock
    private ExceptionalShopsService exceptionalShopsService;
    @Mock
    private CpaOrderCountService cpaOrderCountService;
    @Mock
    private OperationalRatingOrderLimitNotifier operationalRatingOrderLimitNotifier;
    @Mock
    private PartnerRatingLimitRangeService partnerRatingLimitRangeService;
    @Mock
    private MbiApiService mbiApiService;
    @Mock
    private ConfigurationService coreCounterService;

    @Mock
    private PartnerRatingActual partnerRating;
    @Mock
    private CpaOrderLimit orderLimit;

    @BeforeEach
    void init() {
        MockitoAnnotations.openMocks(this);

        var limitRanges = List.of(
                buildLimitRange(1, 0.0, MEDIUM_RATING_LOWER_BOUND, PartnerRatingLimitType.SWITCH_OFF, 0.0),
                buildLimitRange(
                        2, MEDIUM_RATING_LOWER_BOUND, HIGH_RATING_LOWER_BOUND,
                        PartnerRatingLimitType.ORDERS_LIMIT, ORDERS_PART_FOR_LIMIT
                ),
                buildLimitRange(3, HIGH_RATING_LOWER_BOUND, 100.0, PartnerRatingLimitType.NO_LIMIT, 1.0)
        );
        when(partnerRatingLimitRangeService.getModelRanges(any())).thenReturn(limitRanges);

        when(exceptionalShopsService.loadShops(ExceptionalShopReason.DONT_CREATE_RATING_ORDER_LIMIT))
                .thenReturn(Collections.emptySet());
        when(mbiApiService.getShopsWithAboCutoff(AboCutoff.LOW_RATING))
                .thenReturn(Collections.emptySet());

        when(partnerRating.getPartnerId()).thenReturn(SHOP_ID);
        when(partnerRating.getPartnerModel()).thenReturn(PARTNER_MODEL);
        when(partnerRating.getOrdersCount()).thenReturn(ORDERS_LOWER_BOUND + 3);
        when(partnerRatingService.getActualRatings()).thenReturn(List.of(partnerRating));

        when(orderLimitService.findNonRatingLimits()).thenReturn(Collections.emptyList());
        when(orderLimitService.updateLimitWithOperationalRatingReason(eq(SHOP_ID), eq(PartnerModel.DSBB), any())).thenReturn(orderLimit);
        when(orderLimitService.markOperationalRatingLimitDeleted(SHOP_ID, PartnerModel.DSBB)).thenReturn(orderLimit);
        when(orderLimitService.findActiveLimit(SHOP_ID, PartnerModel.DSBB)).thenReturn(Optional.empty());
        when(orderLimit.getShopId()).thenReturn(SHOP_ID);

        when(coreCounterService.getValueAsLocalDate(any())).thenReturn(null);
    }

    @Test
    void doNotCreateLimitIfShopInExceptions() {
        when(exceptionalShopsService.loadShops(ExceptionalShopReason.DONT_CREATE_RATING_ORDER_LIMIT))
                .thenReturn(Set.of(SHOP_ID));

        operationalRatingOrderLimitManager.createCpaOrderLimitsByRating();

        verifyNoMoreInteractions(operationalRatingOrderLimitNotifier);
        verify(orderLimitService, never()).updateLimitWithOperationalRatingReason(
                eq(SHOP_ID), eq(PartnerModel.DSBB), any()
        );
        verify(orderLimitService, never()).markOperationalRatingLimitDeleted(SHOP_ID, PartnerModel.DSBB);
    }

    @Test
    void doNotCreateLimitIfShopHasRatingCutoff() {
        when(mbiApiService.getShopsWithAboCutoff(AboCutoff.LOW_RATING))
                .thenReturn(Set.of(SHOP_ID));

        operationalRatingOrderLimitManager.createCpaOrderLimitsByRating();

        verifyNoMoreInteractions(operationalRatingOrderLimitNotifier);
        verify(orderLimitService, never()).updateLimitWithOperationalRatingReason(
                eq(SHOP_ID), eq(PartnerModel.DSBB), any()
        );
        verify(orderLimitService, never()).markOperationalRatingLimitDeleted(SHOP_ID, PartnerModel.DSBB);
    }

    @ParameterizedTest
    @CsvSource({"DSBB, false", "DSBS, true"})
    void doNotCreateLimitIfExistsNonRatingLimit(PartnerModel existsLimitModel, boolean processLimit) {
        when(partnerRating.getTotal()).thenReturn(MEDIUM_RATING_LOWER_BOUND + 1.0);
        var existsLimit = mock(CpaOrderLimit.class);
        when(existsLimit.getShopId()).thenReturn(SHOP_ID);
        when(existsLimit.getPartnerModel()).thenReturn(existsLimitModel);
        when(orderLimitService.findNonRatingLimits()).thenReturn(List.of(existsLimit));

        operationalRatingOrderLimitManager.createCpaOrderLimitsByRating();

        verify(orderLimitService, times(processLimit ? 1 : 0)).updateLimitWithOperationalRatingReason(
                eq(SHOP_ID), eq(PARTNER_MODEL), any()
        );
    }

    @Test
    void doNotUpdateLimitIfRatingRangeNotChangedAndLimitExists() {
        double previousRatingValue = MEDIUM_RATING_LOWER_BOUND + 10;
        mockPreviousRating(previousRatingValue);

        when(partnerRating.getTotal()).thenReturn(previousRatingValue);

        var activeLimit = mockActiveLimit(LOW_RATING_ORDER_LIMIT);
        when(orderLimitService.findActiveLimit(SHOP_ID, PartnerModel.DSBB)).thenReturn(Optional.of(activeLimit));

        operationalRatingOrderLimitManager.createCpaOrderLimitsByRating();

        verify(orderLimitService, never()).updateLimitWithOperationalRatingReason(
                eq(SHOP_ID), eq(PartnerModel.DSBB), any()
        );
        verify(orderLimitService, never()).markOperationalRatingLimitDeleted(SHOP_ID, PartnerModel.DSBB);
        verifyNoMoreInteractions(operationalRatingOrderLimitNotifier);
    }

    @Test
    void doNotUpdateLimitIfLimitExistsAndRatingAboveLow() {
        double previousRatingValue = MEDIUM_RATING_LOWER_BOUND - 1;
        mockPreviousRating(previousRatingValue);

        when(partnerRating.getTotal()).thenReturn(previousRatingValue + MEDIUM_RATING_LOWER_BOUND + 1);

        var activeLimit = mockActiveLimit(LOW_RATING_ORDER_LIMIT);
        when(orderLimitService.findActiveLimit(SHOP_ID, PartnerModel.DSBB)).thenReturn(Optional.of(activeLimit));

        operationalRatingOrderLimitManager.createCpaOrderLimitsByRating();

        verify(orderLimitService, never()).updateLimitWithOperationalRatingReason(
                eq(SHOP_ID), eq(PartnerModel.DSBB), any()
        );
        verify(orderLimitService, never()).markOperationalRatingLimitDeleted(SHOP_ID, PartnerModel.DSBB);
        verifyNoMoreInteractions(operationalRatingOrderLimitNotifier);
    }

    @Test
    void doNotUpdateLimitForSequentialHighRanges() {
        double highRatingValue = HIGH_RATING_LOWER_BOUND + 1;
        mockPreviousRating(highRatingValue);

        when(partnerRating.getTotal()).thenReturn(highRatingValue);

        operationalRatingOrderLimitManager.createCpaOrderLimitsByRating();

        verify(orderLimitService, never()).updateLimitWithOperationalRatingReason(
                eq(SHOP_ID), eq(PartnerModel.DSBB), any()
        );
        verify(orderLimitService, never()).markOperationalRatingLimitDeleted(SHOP_ID, PartnerModel.DSBB);
        verifyNoMoreInteractions(operationalRatingOrderLimitNotifier);
    }

    @ParameterizedTest(name = "limitResolveTest_{index}")
    @MethodSource("limitResolveTestArguments")
    void limitResolveTest(double operationalRatingValue, long daysAvgOrdersCount, Integer expectedOrdersLimit) {
        var now = LocalDate.now();
        when(partnerRating.getTotal()).thenReturn(operationalRatingValue);
        when(cpaOrderCountService.loadAvg(PARTNER_MODEL, DAYS_PERIOD_FOR_AVG_ORDERS_CALCULATION))
                .thenReturn(Map.of(SHOP_ID, daysAvgOrdersCount));
        when(orderLimit.getDeleted()).thenReturn(false);

        operationalRatingOrderLimitManager.createCpaOrderLimitsByRating();

        verify(orderLimitService).updateLimitWithOperationalRatingReason(
                eq(SHOP_ID), eq(PartnerModel.DSBB), eq(expectedOrdersLimit)
        );
        verify(operationalRatingOrderLimitNotifier).notify(
                eq(SHOP_ID), eq(PartnerModel.DSBB), eq(orderLimit), any(), any()
        );
    }

    @Test
    void deleteLimitIfRatingHigh() {
        when(partnerRating.getTotal()).thenReturn(HIGH_RATING_LOWER_BOUND + 1.0);
        when(orderLimit.getDeleted()).thenReturn(true);

        operationalRatingOrderLimitManager.createCpaOrderLimitsByRating();

        verify(orderLimitService).markOperationalRatingLimitDeleted(SHOP_ID, PartnerModel.DSBB);
        verify(orderLimitService, never()).updateLimitWithOperationalRatingReason(
                eq(SHOP_ID), eq(PartnerModel.DSBB), anyInt()
        );
    }

    private void mockPreviousRating(double ratingValue) {
        when(partnerRatingService.getPreviousRatingTotalByPartner(eq(Set.of(SHOP_ID)), any()))
                .thenReturn(Map.of(SHOP_ID, ratingValue));
    }

    private static CpaOrderLimit mockActiveLimit(int ordersLimit) {
        var limit = mock(CpaOrderLimit.class);
        when(limit.getShopId()).thenReturn(SHOP_ID);
        when(limit.getOrderLimit()).thenReturn(ordersLimit);

        return limit;
    }

    private static Stream<Arguments> limitResolveTestArguments() {
        return Stream.of(
                Arguments.of(
                        MEDIUM_RATING_LOWER_BOUND + 1,
                        DEFAULT_DAYS_AVG_OFFERS_COUNT,
                        (int) Math.ceil(DEFAULT_DAYS_AVG_OFFERS_COUNT * 0.5)
                ),
                Arguments.of(
                        MEDIUM_RATING_LOWER_BOUND + 1,
                        (int) (LIMIT_LOWER_BOUND / ORDERS_PART_FOR_LIMIT) + 10,
                        (int) (LIMIT_LOWER_BOUND + (ORDERS_PART_FOR_LIMIT * 10))
                )
        );
    }

    private static PartnerRatingLimitRange buildLimitRange(int id, double lowerBound, double upperBound,
                                                           PartnerRatingLimitType limitType,
                                                           double ordersPartForLimit) {
        return new PartnerRatingLimitRange(
                id, PARTNER_MODEL, lowerBound, upperBound, limitType, ordersPartForLimit,
                limitType == PartnerRatingLimitType.SWITCH_OFF ? 0 : LIMIT_LOWER_BOUND
        );
    }
}
