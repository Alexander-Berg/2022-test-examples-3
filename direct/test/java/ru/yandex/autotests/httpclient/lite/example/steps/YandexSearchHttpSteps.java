package ru.yandex.autotests.httpclient.lite.example.steps;

import ru.yandex.autotests.httpclient.lite.core.BackEndResponse;
import ru.yandex.autotests.httpclient.lite.core.config.HttpStepsConfig;
import ru.yandex.autotests.httpclient.lite.core.steps.BackEndBaseSteps;
import ru.yandex.autotests.httpclient.lite.example.data.YandexSearchParameters;
import ru.yandex.qatools.allure.annotations.Step;

/**
 * @author Roman Kuhta (kuhtich@yandex-team.ru)
 */
public class YandexSearchHttpSteps extends BackEndBaseSteps {
    @Override
    protected void init(HttpStepsConfig config) {
        config.getClientConfig().path("/yandsearch");
        super.init(config);
    }

    @Step
    public BackEndResponse getSearchResults(YandexSearchParameters params) {
        return execute(config.getRequestBuilder().get(params));
    }
}
