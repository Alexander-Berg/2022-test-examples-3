package ru.yandex.market.abo.core.forecast;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.EmptyTest;
import ru.yandex.market.abo.api.entity.forecast.ShopForecast;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author artemmz
 * @date 18.07.17.
 */
public class ForecastProviderTest extends EmptyTest {
    @Autowired
    private CpaForecastProvider cpaForecastProvider;

    @Autowired
    private CpcForecastProvider cpcForecastProvider;

    @Test
    public void forecastMe_CPA() throws Exception {
        ShopForecast shopForecast = cpaForecastProvider.provide(774L);
        assertNotNull(shopForecast);
    }

    @Test
    public void forecastMe_CPC() throws Exception {
        ShopForecast shopForecast = cpcForecastProvider.provide(774L);
        assertNotNull(shopForecast);
    }
}