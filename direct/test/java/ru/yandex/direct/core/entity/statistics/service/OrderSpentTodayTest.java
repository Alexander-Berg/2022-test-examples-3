package ru.yandex.direct.core.entity.statistics.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.core.entity.statistics.model.SpentInfo;
import ru.yandex.direct.core.entity.statistics.repository.OrderStatDayRepository;
import ru.yandex.direct.core.entity.tax.model.TaxInfo;
import ru.yandex.direct.core.entity.tax.service.TaxHistoryService;
import ru.yandex.direct.currency.CurrencyCode;
import ru.yandex.direct.currency.Money;
import ru.yandex.direct.currency.Percent;
import ru.yandex.direct.utils.TimeProvider;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(Parameterized.class)
public class OrderSpentTodayTest {

    private static final LocalDateTime now = LocalDateTime.of(2019, 7, 8, 3, 4, 5);
    private static final LocalDate today = LocalDate.of(2019, 7, 8);
    private static final Long orderId = 1L;
    private static final Collection<Long> orderIds = Collections.singletonList(orderId);
    private static final Long rubTaxId = 17L;
    private static final Long rubCurrencyId = 643L;
    private static final Percent rubPercent = Percent.fromRatio(BigDecimal.valueOf(0.20));
    private static final LocalDate rubStartDate = LocalDate.of(2019, 1, 1);

    @Parameterized.Parameter
    public Map<Long, SpentInfo> repositoryResult;

    @Parameterized.Parameter(1)
    public Boolean withNds;

    @Parameterized.Parameter(2)
    public Map<Long, Money> expectedResult;

    @Parameterized.Parameters
    public static Collection<Object[]> parameters() {
        return asList(new Object[][]{
                {
                        Collections.emptyMap(), true, Collections.emptyMap()
                },
                {
                        Map.of(orderId, new SpentInfo(orderId, null, 9199L, rubTaxId, rubCurrencyId)),
                        true,
                        Map.of(orderId, Money.valueOf("0.91", CurrencyCode.RUB))
                },
                {
                        Map.of(orderId, new SpentInfo(orderId, null, 0L, rubTaxId, rubCurrencyId)),
                        true,
                        Map.of(orderId, Money.valueOf("0", CurrencyCode.RUB))
                },
                {
                        Map.of(orderId, new SpentInfo(orderId, null, 12000000L, rubTaxId, rubCurrencyId)),
                        false,
                        Map.of(orderId, Money.valueOf("1000", CurrencyCode.RUB))
                }
        });
    }

    private OrderStatService orderStatService;

    @Before
    public void init() {
        TimeProvider timeProvider = mock(TimeProvider.class);
        when(timeProvider.now()).thenReturn(now);

        OrderStatDayRepository orderStatDayRepository = mock(OrderStatDayRepository.class);
        when(orderStatDayRepository.getOrdersSpent(orderIds, today)).thenReturn(repositoryResult);

        TaxHistoryService taxHistoryService = mock(TaxHistoryService.class);
        when(taxHistoryService.getTaxInfo(rubTaxId, today))
                .thenReturn(new TaxInfo(rubTaxId, rubStartDate, rubPercent));

        orderStatService = new OrderStatService(orderStatDayRepository, null, null, taxHistoryService, null,
                null, timeProvider);
    }

    @Test
    public void test() {
        Map<Long, Money> res = orderStatService.getOrdersSpentToday(orderIds, withNds);
        assertThat(res).isEqualTo(expectedResult);
    }
}
