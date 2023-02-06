package ru.yandex.market.stock;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.currency.Currency;
import ru.yandex.common.util.currency.CurrencyRate;
import ru.yandex.common.util.currency.RateSource;
import ru.yandex.market.billing.FunctionalTest;
import ru.yandex.market.stock.model.SdtDTO;
import ru.yandex.market.stock.model.StockDTO;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static ru.yandex.market.stock.CurrencyMatchers.rateMatcher;
import static ru.yandex.market.stock.CurrencyMatchers.rateTsMatcher;

/**
 * Тесты для {@link NewsRateLoader}.
 *
 * @author Vladislav Bauer
 */
class NewsRateLoaderTest extends FunctionalTest {

    @Autowired
    private NewsRateLoader newsRateLoader;

    @Test
    @DisplayName("Проверка парсинга CurrencyRate")
    void testParseCurrencyRates() throws Exception {
        try (InputStream dataStream = getClass().getResourceAsStream("device.xml")) {
            final List<CurrencyRate> rates = newsRateLoader.buildCurrencyRates(dataStream, false);

            assertThat(
                    rates,
                    contains(
                            ImmutableList.of(
                                    rateMatcher(Currency.USD, RateSource.CBRF_DAILY, 1, "2019-02-13",
                                            BigDecimal.valueOf(65.7147)),
                                    rateMatcher(Currency.USD, RateSource.ZMBW_DAILY, 1, "2016-06-30",
                                            BigDecimal.valueOf(20053.0)),
                                    rateMatcher(Currency.RUR, RateSource.ZMBW_DAILY, 1, "2016-06-30",
                                            BigDecimal.valueOf(312.12)),
                                    rateMatcher(Currency.EUR, RateSource.CBRF_DAILY, 1, "2019-02-12",
                                            BigDecimal.valueOf(74.3243)),
                                    rateMatcher(Currency.GBP, RateSource.CBRF_DAILY, 1, "2022-02-10",
                                            BigDecimal.valueOf(101.4608)),
                                    rateMatcher(Currency.TRY, RateSource.CBRF_DAILY, 1, "2022-02-10",
                                            BigDecimal.valueOf(5.5027)),
                                    rateMatcher(Currency.UAH, RateSource.TCMB_DAILY, 1, "2022-03-21",
                                            BigDecimal.valueOf(0.5044)),
                                    rateMatcher(Currency.GBP, RateSource.FAKE_BUSD, 1, "2022-04-12",
                                            BigDecimal.valueOf(1.3015)),
                                    rateMatcher(Currency.USD, RateSource.BOE_DAILY, 1, "2022-05-23",
                                            BigDecimal.valueOf(1.2571))
                                    //stockId 500 отфильтрован так как запись для последней даты помечена как unconfirmed
                            )
                    )
            );
        }
    }

    /**
     * На примере одного источника цбрф и валюты USD проверяем, что возвращаются данные по всем дням без фильтрации.
     */
    @DisplayName("Курсы без фильтрации по дням")
    @Test
    void test_buildCurrencyRates_withAllDays() throws IOException {
        try (InputStream dataStream = getClass().getResourceAsStream("device.xml")) {
            final List<CurrencyRate> rates = newsRateLoader.buildCurrencyRates(dataStream, true);
            assertThat(rates, hasSize(39));

            final List<CurrencyRate> cbrfUsdRates = rates.stream()
                    .filter(x -> x.getRateSource() == RateSource.CBRF_DAILY)
                    .filter(x -> x.getCurrency() == Currency.USD)
                    .sorted(Comparator.comparing(CurrencyRate::getEventTime))
                    .collect(Collectors.toList());

            assertThat(
                    cbrfUsdRates,
                    contains(
                            ImmutableList.of(
                                    rateTsMatcher("2019-02-05"),
                                    rateTsMatcher("2019-02-06"),
                                    rateTsMatcher("2019-02-07"),
                                    rateTsMatcher("2019-02-08"),
                                    rateTsMatcher("2019-02-09"),
                                    rateTsMatcher("2019-02-12"),
                                    rateTsMatcher("2019-02-13")
                            )
                    )
            );
        }
    }

    @DisplayName("Курсы без фильтрации по дням + фильтрация unconfirmed")
    @Test
    void test_buildCurrencyRates_withAllDaysConfirmedFiltration() throws IOException {
        try (InputStream dataStream = getClass().getResourceAsStream("device_unconfirmed.xml")) {
            final List<CurrencyRate> rates = newsRateLoader.buildCurrencyRates(dataStream, true)
                    .stream()
                    .filter(x -> x.getRateSource() == RateSource.CBRF_DAILY)
                    .filter(x -> x.getCurrency() == Currency.USD)
                    .sorted(Comparator.comparing(CurrencyRate::getEventTime))
                    .collect(Collectors.toList());

            assertThat(
                    rates,
                    contains(
                            ImmutableList.of(
                                    rateTsMatcher("2019-02-09"),
                                    rateTsMatcher("2019-02-13")
                            )
                    )
            );
        }
    }

    @Test
    @DisplayName("Проверка парсинга xml файла")
    void parseXmlTest() throws Exception {
        try (InputStream dataStream = getClass().getResourceAsStream("parseXmlTest_device.xml")) {
            final List<StockDTO> stocksDTO = newsRateLoader.parseDataXml(dataStream);

            assertThat(stocksDTO, hasSize(2));

            StockDTO stockDTO1 = stocksDTO.get(0);
            Assertions.assertEquals(1L, stockDTO1.getId());
            Assertions.assertEquals(1, stockDTO1.getNominal().intValue());
            SdtDTO sdtDTO1 = newsRateLoader.getActualSdt(stockDTO1.getSdtList());
            Assertions.assertEquals("2018-11-02", sdtDTO1.getDate());
            Assertions.assertEquals(65.6517, sdtDTO1.getValue().doubleValue());
            Assertions.assertTrue(sdtDTO1.isConfirmed());

            StockDTO stockDTO2 = stocksDTO.get(1);
            Assertions.assertEquals(23, stockDTO2.getId());
            Assertions.assertEquals(1, stockDTO2.getNominal().intValue());
            SdtDTO sdtDTO2 = newsRateLoader.getActualSdt(stockDTO2.getSdtList());
            Assertions.assertEquals("2018-11-02", sdtDTO2.getDate());
            Assertions.assertEquals(74.5803, sdtDTO2.getValue().doubleValue());
            Assertions.assertFalse(sdtDTO2.isConfirmed());
        }
    }
}
