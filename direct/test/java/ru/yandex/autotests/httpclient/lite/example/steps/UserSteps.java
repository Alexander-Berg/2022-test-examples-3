package ru.yandex.autotests.httpclient.lite.example.steps;

import ru.yandex.autotests.httpclient.lite.core.config.HttpStepsConfig;
import ru.yandex.autotests.httpclient.lite.core.steps.BackEndBaseSteps;

/**
 * @author Roman Kuhta (kuhtich@yandex-team.ru)
 */
public class UserSteps {
    public UserSteps(HttpStepsConfig config) {
        this.config = config;
    }

    protected HttpStepsConfig config;

    public YandexSearchHttpSteps yaRuHttpSteps() {
        return BackEndBaseSteps.getInstance(YandexSearchHttpSteps.class, config);
    }
}
