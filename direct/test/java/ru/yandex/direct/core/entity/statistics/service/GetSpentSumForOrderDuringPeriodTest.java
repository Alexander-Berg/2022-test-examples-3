package ru.yandex.direct.core.entity.statistics.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.direct.core.entity.statistics.model.SpentInfo;
import ru.yandex.direct.core.entity.statistics.repository.OrderStatDayRepository;
import ru.yandex.direct.core.entity.tax.model.TaxInfo;
import ru.yandex.direct.core.entity.tax.service.TaxHistoryService;
import ru.yandex.direct.currency.CurrencyCode;
import ru.yandex.direct.currency.Money;
import ru.yandex.direct.currency.Percent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

@RunWith(MockitoJUnitRunner.class)
public class GetSpentSumForOrderDuringPeriodTest {
    private static final long ORDER_ID_1 = 1L;
    private static final long ORDER_ID_2 = 2L;
    private static final long ORDER_ID_3 = 3L;
    private static final long TAX_ID = 777L;
    private static final long CURRENCY_ID = 555L;
    private static final long COST = 100_000L;

    private OrderStatService orderStatService;
    private OrderStatDayRepository orderStatDayRepository;
    private TaxHistoryService taxHistoryService;

    @Before
    public void before() {
        orderStatDayRepository = mock(OrderStatDayRepository.class);
        taxHistoryService = mock(TaxHistoryService.class);

        orderStatService = new OrderStatService(orderStatDayRepository, null, null, taxHistoryService, null, null,
                null);
    }

    @Test
    public void getSpentSumForOrderDuringPeriodWithNds() {
        LocalDate now = LocalDate.now();
        List<Long> orderIds = List.of(ORDER_ID_1, ORDER_ID_2, ORDER_ID_3);

        List<LocalDate> startDates = List.of(now.minusDays(2), now.minusDays(3), now.minusDays(4));
        List<LocalDate> endDates = List.of(now.minusDays(0), now.minusDays(1), now.minusDays(0));

        doReturn(Map.of(TAX_ID, List.of(
                new TaxInfo(TAX_ID, now.minusDays(5), Percent.fromPercent(BigDecimal.TEN)),
                new TaxInfo(TAX_ID, now.minusDays(1), Percent.fromPercent(BigDecimal.valueOf(20))))))
                .when(taxHistoryService).getTaxInfos(Set.of(TAX_ID));
        doReturn(getSpentMap(now)).when(orderStatDayRepository).getOrdersSpent(orderIds, startDates, endDates);
        Map<Long, Money> spentSumForOrdersWithPeriod = orderStatService.getSpentSumForOrderDuringPeriod(orderIds,
                startDates, endDates, CurrencyCode.RUB, true);
        assertThat(spentSumForOrdersWithPeriod).isEqualTo(Map.of(
                ORDER_ID_1, Money.valueOf("25.75", CurrencyCode.RUB),
                ORDER_ID_2, Money.valueOf("26.51", CurrencyCode.RUB),
                ORDER_ID_3, Money.valueOf("43.93", CurrencyCode.RUB)));
    }

    @Test
    public void getSpentSumForOrderDuringPeriodWithoutNds() {
        LocalDate now = LocalDate.now();
        List<Long> orderIds = List.of(ORDER_ID_1, ORDER_ID_2, ORDER_ID_3);

        List<LocalDate> startDates = List.of(now.minusDays(2), now.minusDays(3), now.minusDays(4));
        List<LocalDate> endDates = List.of(now.minusDays(0), now.minusDays(1), now.minusDays(0));

        doReturn(Map.of(TAX_ID, List.of(
                new TaxInfo(TAX_ID, now.minusDays(5), Percent.fromPercent(BigDecimal.TEN)),
                new TaxInfo(TAX_ID, now.minusDays(1), Percent.fromPercent(BigDecimal.valueOf(20))))))
                .when(taxHistoryService).getTaxInfos(Set.of(TAX_ID));
        doReturn(getSpentMap(now)).when(orderStatDayRepository).getOrdersSpent(orderIds, startDates, endDates);
        Map<Long, Money> spentSumForOrdersWithPeriod = orderStatService.getSpentSumForOrderDuringPeriod(orderIds,
                startDates, endDates, CurrencyCode.RUB, false);

        assertThat(spentSumForOrdersWithPeriod).isEqualTo(Map.of(
                ORDER_ID_1, Money.valueOf("30.00", CurrencyCode.RUB),
                ORDER_ID_2, Money.valueOf("30.00", CurrencyCode.RUB),
                ORDER_ID_3, Money.valueOf("50.00", CurrencyCode.RUB)));
    }

    private Map<Long, List<SpentInfo>> getSpentMap(LocalDate now) {
        return Map.of(
                ORDER_ID_1, List.of(
                        new SpentInfo(ORDER_ID_1, now.minusDays(2).atStartOfDay().toInstant(ZoneOffset.UTC), COST,
                                TAX_ID, CURRENCY_ID),
                        new SpentInfo(ORDER_ID_1, now.minusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC), COST,
                                TAX_ID, CURRENCY_ID),
                        new SpentInfo(ORDER_ID_1, now.atStartOfDay().toInstant(ZoneOffset.UTC), COST, TAX_ID,
                                CURRENCY_ID)),
                ORDER_ID_2, List.of(
                        new SpentInfo(ORDER_ID_2, now.minusDays(3).atStartOfDay().toInstant(ZoneOffset.UTC), COST,
                                TAX_ID, CURRENCY_ID),
                        new SpentInfo(ORDER_ID_2, now.minusDays(2).atStartOfDay().toInstant(ZoneOffset.UTC), COST,
                                TAX_ID, CURRENCY_ID),
                        new SpentInfo(ORDER_ID_2, now.minusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC), COST,
                                TAX_ID, CURRENCY_ID)),
                ORDER_ID_3, List.of(
                        new SpentInfo(ORDER_ID_3, now.minusDays(4).atStartOfDay().toInstant(ZoneOffset.UTC), COST,
                                TAX_ID, CURRENCY_ID),
                        new SpentInfo(ORDER_ID_3, now.minusDays(3).atStartOfDay().toInstant(ZoneOffset.UTC), COST,
                                TAX_ID, CURRENCY_ID),
                        new SpentInfo(ORDER_ID_3, now.minusDays(2).atStartOfDay().toInstant(ZoneOffset.UTC), COST,
                                TAX_ID, CURRENCY_ID),
                        new SpentInfo(ORDER_ID_3, now.minusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC), COST,
                                TAX_ID, CURRENCY_ID),
                        new SpentInfo(ORDER_ID_3, now.atStartOfDay().toInstant(ZoneOffset.UTC), COST, TAX_ID,
                                CURRENCY_ID))
        );
    }
}
