package ru.yandex.direct.core.entity.statistics.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import one.util.streamex.EntryStream;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.core.entity.statistics.model.Period;
import ru.yandex.direct.core.entity.statistics.repository.OrderStatDayRepository;
import ru.yandex.direct.currency.CurrencyCode;
import ru.yandex.direct.currency.Money;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(Parameterized.class)
public class GetOrdersSumSpentTest {

    @Parameterized.Parameter
    public List<Long> orderIds;

    @Parameterized.Parameter(1)
    public List<Period> periods;

    @Parameterized.Parameter(2)
    public CurrencyCode currencyCode;

    @Parameterized.Parameter(3)
    public LocalDate startDateToCalc;

    @Parameterized.Parameter(4)
    public LocalDate endDateToCalc;

    @Parameterized.Parameter(5)
    public Map<LocalDate, Long> repResult;

    @Parameterized.Parameter(6)
    public Map<String, Money> expectedResult;

    @Parameterized.Parameters
    public static Collection<Object[]> parameters() {
        return asList(new Object[][]{
                {
                        Collections.singletonList(0L),
                        Collections.singletonList(
                                new Period("name", parse("2006-01-01"), parse("2006-02-01"))
                        ),
                        CurrencyCode.RUB,
                        parse("2006-01-01"),
                        parse("2006-02-01"),
                        Collections.emptyMap(),
                        Collections.singletonMap("name", Money.valueOf(0, CurrencyCode.RUB))
                },
                {
                        Collections.singletonList(8856790L),
                        Arrays.asList(
                                new Period("name1", parse("2016-10-18"), parse("2016-10-19")),
                                new Period("name2", parse("2016-10-20"), parse("2016-10-21"))
                        ),
                        CurrencyCode.RUB,
                        parse("2016-10-18"),
                        parse("2016-10-21"),
                        Map.of(
                                parse("2016-10-18"), 115382L,
                                parse("2016-10-19"), 510593L,
                                parse("2016-10-20"), 33785L,
                                parse("2016-10-21"), 78335L
                        ),
                        Map.of(
                                "name1", Money.valueOf("62.5975", CurrencyCode.RUB),
                                "name2", Money.valueOf("11.212", CurrencyCode.RUB)
                        )
                },
                {
                        Collections.singletonList(54545L),
                        Arrays.asList(
                                new Period("name1", parse("2016-10-18"), parse("2016-10-21")),
                                new Period("name2", parse("2016-10-19"), parse("2016-10-20"))
                        ),
                        CurrencyCode.RUB,
                        parse("2016-10-18"),
                        parse("2016-10-21"),
                        Map.of(
                                parse("2016-10-18"), 115382L,
                                parse("2016-10-19"), 510593L,
                                parse("2016-10-20"), 33785L,
                                parse("2016-10-21"), 78335L
                        ),
                        Map.of(
                                "name1", Money.valueOf("73.8095", CurrencyCode.RUB),
                                "name2", Money.valueOf("54.4378", CurrencyCode.RUB)
                        )
                },
                {
                        Collections.singletonList(54545L),
                        Collections.singletonList(
                                new Period("name1", parse("2016-10-18"), parse("2016-10-21"))
                        ),
                        CurrencyCode.RUB,
                        parse("2016-10-18"),
                        parse("2016-10-21"),
                        Map.of(
                                parse("2016-10-18"), 115382L
                        ),
                        Map.of(
                                "name1", Money.valueOf("11.5382", CurrencyCode.RUB)
                        )
                }
        });
    }

    private OrderStatService orderStatService;

    @Before
    public void before() {
        OrderStatDayRepository orderStatDayRepository = mock(OrderStatDayRepository.class);
        Map<LocalDate, BigDecimal> repResultBD = EntryStream.of(repResult).mapValues(BigDecimal::valueOf).toMap();
        when(orderStatDayRepository.getDateToSumSpent(orderIds, startDateToCalc, endDateToCalc)).thenReturn(repResultBD);

        orderStatService = new OrderStatService(orderStatDayRepository, null, null, null, null, null,
                null);
    }

    @Test
    public void test() {
        Map<String, Money> res = orderStatService.getOrdersSumSpent(orderIds, periods, currencyCode);
        assertThat(res).isEqualTo(expectedResult);
    }

    private static LocalDate parse(String str) {
        return LocalDate.parse(str, DateTimeFormatter.ISO_LOCAL_DATE);
    }
}
