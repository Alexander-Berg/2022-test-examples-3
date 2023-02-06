package ru.yandex.market.stock;

import java.util.Collections;

import name.falgout.jeffrey.testing.junit.mockito.MockitoExtension;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import ru.yandex.common.util.http.HttpClientFactory;
import ru.yandex.market.billing.FunctionalTest;
import ru.yandex.market.common.test.db.DbUnitDataSet;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Тесты для {@link CurrencyRateLoaderExecutor}
 *
 * @author Sergei Telnov (sergeitelnov@yandex-team.ru)
 */
@ExtendWith(MockitoExtension.class)
class CurrencyRateLoaderExecutorTest extends FunctionalTest {

    @Qualifier(value = "namedParameterJdbcTemplate")
    @Autowired
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    @Autowired
    private NewsRateLoader newsRateLoader;
    @Mock
    private HttpClientFactory mockClientFactory;
    @Mock
    private HttpClient mockClient;
    @Mock
    private HttpResponse mockResponse;
    @Mock
    private HttpEntity mockEntity;
    private CurrencyRateLoaderExecutor currencyRateLoaderExecutor;

    @BeforeEach
    void initExecutor() throws Exception {
        when(mockClientFactory.createHttpClient()).thenReturn(mockClient);
        when(mockClient.execute(any())).thenReturn(mockResponse);
        when(mockResponse.getEntity()).thenReturn(mockEntity);
        when(mockEntity.getContent()).thenReturn(this.getClass().getResourceAsStream("device.xml"));

        ReflectionTestUtils.setField(newsRateLoader, "httpClientFactory", mockClientFactory);

        final CurrencyRateLoaderService service = new CurrencyRateLoaderService(
                Collections.singletonList(newsRateLoader),
                namedParameterJdbcTemplate
        );

        this.currencyRateLoaderExecutor = new CurrencyRateLoaderExecutor(service);
    }

    @Test
    @DisplayName("Тест импорта новых данных")
    @DbUnitDataSet(
            after = "currencyRateLoaderExecutor_reader.currency_rate.after.csv"
    )
    void saveCurrencyRateTest() {
        currencyRateLoaderExecutor.doJob(null);
    }

    @Test
    @DisplayName("Тест импорта новых данных с повторениями")
    @DbUnitDataSet(
            before = "currencyRateLoaderExecutor_reader.currency_rate.before.csv",
            after = "currencyRateLoaderExecutor_reader.currency_rate.after.csv"
    )
    void saveDuplicateCurrencyRateTest() {
        currencyRateLoaderExecutor.doJob(null);
    }
}