package ru.yandex.market.stock;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;

import com.google.common.collect.ImmutableList;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import ru.yandex.common.util.currency.Currency;
import ru.yandex.common.util.currency.CurrencyRate;
import ru.yandex.common.util.currency.RateSource;
import ru.yandex.market.core.util.DateTimes;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class HistoricalCurrencyRateLoaderTest {

    private static final List<CurrencyRate> CURRENT_CURRENCY_RATES = ImmutableList.of(
            new CurrencyRate(Currency.USD, RateSource.CBRF_DAILY, 1, BigDecimal.valueOf(65.7147),
                    DateTimes.asDate(LocalDate.of(2019, 2, 13))),
            new CurrencyRate(Currency.EUR, RateSource.CBRF_DAILY, 1, BigDecimal.valueOf(74.3243),
                    DateTimes.asDate(LocalDate.of(2019, 2, 12))),
            new CurrencyRate(Currency.USD, RateSource.ZMBW_DAILY, 1, BigDecimal.valueOf(20053.0),
                    DateTimes.asDate(LocalDate.of(2016, 6, 30))),
            new CurrencyRate(Currency.RUR, RateSource.ZMBW_DAILY, 1, BigDecimal.valueOf(312.12),
                    DateTimes.asDate(LocalDate.of(2016, 6, 30)))
    );

    private static final Resource RESPONSE_BODY =
            new UrlResource(HistoricalCurrencyRateLoaderTest.class.getResource("graphmin_1.json"));

    private static final LocalDate CURRENCY_RATE_DATE = LocalDate.of(2019, 2, 12);

    private RestTemplate restTemplate;
    private MockRestServiceServer server;

    @Mock
    private NewsRateLoader newsRateLoader;

    @BeforeEach
    void init() {
        MockitoAnnotations.initMocks(this);

        restTemplate = new RestTemplate();
        server = MockRestServiceServer.createServer(restTemplate);
        server.expect(requestTo("/xmlhist/graphmin_1.json")).andRespond(withSuccess(RESPONSE_BODY, MediaType.APPLICATION_JSON));
    }

    @AfterEach
    void resetMock() {
        Mockito.reset(newsRateLoader);
    }

    @Test
    void loadCurrencyRate() {
        when(newsRateLoader.load()).thenReturn(CURRENT_CURRENCY_RATES);

        HistoricalCurrencyRateLoader loader = new HistoricalCurrencyRateLoader(
                "/xmlhist/graphmin_%d.json",
                restTemplate,
                newsRateLoader
        );

        CurrencyRate currencyRate = loader.load(Currency.USD, RateSource.CBRF_DAILY, CURRENCY_RATE_DATE);

        server.verify();
        assertThat(currencyRate, CurrencyMatchers.rateMatcher(
                Currency.USD,
                RateSource.CBRF_DAILY,
                1,
                "2019-02-12",
                BigDecimal.valueOf(65.6517)
        ));
    }


    @Test
    void testFailOnMissingNominal() {
        when(newsRateLoader.load()).thenReturn(Collections.emptyList());

        HistoricalCurrencyRateLoader loader = new HistoricalCurrencyRateLoader(
                "/xmlhist/graphmin_%d.json",
                restTemplate,
                newsRateLoader
        );

        NoSuchElementException exception = Assertions.assertThrows(NoSuchElementException.class,
                () -> loader.load(Currency.USD, RateSource.CBRF_DAILY, CURRENCY_RATE_DATE));
        assertThat(exception.getMessage(), Matchers.containsString("No nominal value"));
    }
}