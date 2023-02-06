package ru.yandex.market.stock;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Date;
import java.util.List;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import name.falgout.jeffrey.testing.junit.mockito.MockitoExtension;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;

import ru.yandex.common.util.currency.Bank;
import ru.yandex.common.util.currency.Currency;
import ru.yandex.common.util.currency.CurrencyRate;
import ru.yandex.common.util.currency.RateSource;
import ru.yandex.common.util.date.DateUtil;

import static ru.yandex.market.stock.CurrencyMatchers.rateMatcher;

/**
 * Тесты для {@link FakeRateLoader}.
 */
@ExtendWith(MockitoExtension.class)
class FakeRateLoaderTest {
    private static final Date DATE_2019_03_12 = DateUtil.asDate(LocalDate.of(2019, 3, 12));

    @Mock
    private NewsRateLoader newsRateLoader;

    @Test
    void test_smokeTest() {
        Mockito.when(newsRateLoader.load())
                .thenReturn(
                        ImmutableList.of(
                                new CurrencyRate(Currency.USD, RateSource.CBRF_DAILY, 1, BigDecimal.TEN,
                                        DATE_2019_03_12),
                                new CurrencyRate(Currency.EUR, RateSource.CBRF_DAILY, 1, BigDecimal.TEN,
                                        DATE_2019_03_12),
                                new CurrencyRate(Currency.USD, RateSource.FAKE_BUSD, 1, BigDecimal.TEN,
                                        DATE_2019_03_12),
                                //будет фильтировано
                                new CurrencyRate(Currency.RUR, RateSource.CBRF_DAILY, 1, BigDecimal.TEN,
                                        DATE_2019_03_12)
                        )
                );

        final FakeRateLoader fakeRateLoader = new FakeRateLoader(
                ImmutableSet.of(
                        Bank.ECB,
                        Bank.BUSD
                ),
                ImmutableMap.of(
                        Bank.BUSD, RateSource.FAKE_BUSD,
                        Bank.ECB, RateSource.FAKE_ECB
                ),
                newsRateLoader
        );

        final List<CurrencyRate> actual = fakeRateLoader.load();

        MatcherAssert.assertThat(
                actual,
                Matchers.contains(
                        ImmutableList.of(
                                rateMatcher(Currency.RUR, RateSource.FAKE_BUSD, 1, "2019-03-12",
                                        new BigDecimal("0.100000")),
                                rateMatcher(Currency.RUR, RateSource.FAKE_ECB, 1, "2019-03-12",
                                        new BigDecimal("0.100000")),
                                rateMatcher(Currency.USD, RateSource.FAKE_BUSD, 1, "2019-03-12",
                                        new BigDecimal("1"))
                        )
                )
        );

        Mockito.verify(newsRateLoader).load();
        Mockito.verifyNoMoreInteractions(newsRateLoader);
    }

}
