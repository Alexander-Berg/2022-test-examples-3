package ru.yandex.direct.core.entity.keyword.service;

import java.util.IdentityHashMap;

import org.assertj.core.api.Assertions;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.keyword.model.ForecastCtr;
import ru.yandex.direct.core.entity.keyword.model.Keyword;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.KeywordInfo;
import ru.yandex.direct.core.testing.steps.Steps;

import static java.util.Collections.singletonList;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class KeywordForecastServiceTest {

    private static final double PREMIUM_CTR = 0.3;
    private static final double GUARANTEE_CTR = 0.02;

    @Autowired
    private Steps steps;

    @Autowired
    private KeywordForecastService serviceUnderTest;

    private Keyword keyword;

    @Before
    public void setUp() throws Exception {
        KeywordInfo keywordInfo = steps.keywordSteps().createDefaultKeyword();
        keyword = keywordInfo.getKeyword();
        serviceUnderTest.addForecast(keyword.getNormPhrase(), GUARANTEE_CTR, PREMIUM_CTR);
    }

    @After
    public void tearDown() throws Exception {
        serviceUnderTest.removeForecast(keyword.getNormPhrase());
    }

    @Test
    public void getForecast_returnExpected_whenThereAreDataForNormPhrase() throws Exception {
        IdentityHashMap<Keyword, ForecastCtr> forecasts = serviceUnderTest.getForecast(singletonList(keyword));
        Assertions.assertThat(forecasts)
                .hasSize(1)
                .hasEntrySatisfying(keyword, forecastCtr -> {
                    Assertions.assertThat(forecastCtr.getPremiumCtr()).isEqualTo(PREMIUM_CTR);
                    Assertions.assertThat(forecastCtr.getGuaranteeCtr()).isEqualTo(GUARANTEE_CTR);
                });
    }

    @Test
    public void getForecast_returnEmpty_whenThereAreNoDataForNormPhrase() throws Exception {
        // Создаём ключевую фразу, для которой не добавляем информацию о CTR в forecast_ctr
        KeywordInfo keywordInfo = steps.keywordSteps().createDefaultKeyword();
        Keyword keywordWithoutForecast = keywordInfo.getKeyword();

        IdentityHashMap<Keyword, ForecastCtr> forecasts =
                serviceUnderTest.getForecast(singletonList(keywordWithoutForecast));
        Assertions.assertThat(forecasts)
                .isEmpty();
    }

}
