package ru.yandex.market.checkout.util;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.google.common.collect.ImmutableMap;
import org.mockito.Mockito;

import ru.yandex.common.util.currency.Currency;
import ru.yandex.market.checkout.checkouter.mocks.Mocks;
import ru.yandex.market.checkout.checkouter.order.global.CurrencyConvertService;
import ru.yandex.market.common.report.model.CurrencyConvertResult;

import static org.mockito.ArgumentMatchers.any;

/**
 * Мок для сервиса конвертации.
 * В бою ходим в репорт в плейс currency_convert
 * http://int-report.vs.market.yandex.net:17151/yandsearch?place=currency_convert&currency-from=EUR&currency-to=RUR
 * &currency-value=199&client=checkout&co-from=checkouter
 * Курсы конвертации можно править или добавлять в convertRates.
 */
public final class CurrencyConvertServiceImpl {

    private static Map<Currency, Map<Currency, BigDecimal>> convertRates =
            new HashMap<Currency, Map<Currency, BigDecimal>>() {{
                put(Currency.EUR, ImmutableMap.of(
                        Currency.USD, new BigDecimal("70.00").divide(BigDecimal.valueOf(60), BigDecimal.ROUND_HALF_UP),
                        Currency.RUR, BigDecimal.valueOf(70)
                ));
                put(Currency.USD, new HashMap<Currency, BigDecimal>() {{
                    put(Currency.RUR, BigDecimal.valueOf(60));
                }});
            }};

    private CurrencyConvertServiceImpl() {
    }

    public static CurrencyConvertService getMock() {
        CurrencyConvertService currencyConvertService = Mocks.createMock(CurrencyConvertService.class);
        Mockito.doAnswer(invocation -> {
            BigDecimal rate = BigDecimal.ZERO;

            Object[] args = invocation.getArguments();
            Currency currencyFrom = (Currency) args[0];
            Currency currencyTo = (Currency) args[1];

            rate = Optional.ofNullable(convertRates.get(currencyFrom)).get().get(currencyTo);
            BigDecimal result = rate.multiply((BigDecimal) args[2]);

            CurrencyConvertResult currencyConvertResult = new CurrencyConvertResult();
            currencyConvertResult.setCurrencyFrom(currencyFrom);
            currencyConvertResult.setCurrencyTo(currencyTo);
            currencyConvertResult.setValue(result);
            currencyConvertResult.setConvertedValue(result);
            currencyConvertResult.setRenderedValue(result);
            currencyConvertResult.setRenderedConvertedValue(result);
            return currencyConvertResult;
        }).when(currencyConvertService).convert(any(), any(), any());
        return currencyConvertService;
    }
}
