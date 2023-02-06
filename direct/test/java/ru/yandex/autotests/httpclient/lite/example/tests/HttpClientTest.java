package ru.yandex.autotests.httpclient.lite.example.tests;

import org.junit.Before;
import org.junit.Test;
import ru.yandex.autotests.httpclient.lite.core.BackEndResponse;
import ru.yandex.autotests.httpclient.lite.core.config.HttpClientConnectionConfig;
import ru.yandex.autotests.httpclient.lite.core.config.HttpStepsConfig;
import ru.yandex.autotests.httpclient.lite.example.data.YandexSearchParameters;
import ru.yandex.autotests.httpclient.lite.example.steps.UserSteps;

/**
 * @author Roman Kuhta (kuhtich@yandex-team.ru)
 */
public class HttpClientTest {

    private UserSteps user;

    @Before
    public void init() {
        user = new UserSteps(
                new HttpStepsConfig().useClientConfig(
                new HttpClientConnectionConfig()
                        .scheme("http")
                        .host("ya.ru")));
    }

    @Test
    public void someTest() {
        YandexSearchParameters params = new YandexSearchParameters();
        params.setQuery("hello");

        BackEndResponse r = user.yaRuHttpSteps().getSearchResults(params);
//        r.getResponseContent().
    }

}
