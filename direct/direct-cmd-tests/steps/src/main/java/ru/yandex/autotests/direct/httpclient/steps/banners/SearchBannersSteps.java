package ru.yandex.autotests.direct.httpclient.steps.banners;

import ru.yandex.autotests.direct.httpclient.core.DirectResponse;
import ru.yandex.autotests.direct.httpclient.data.CMD;
import ru.yandex.autotests.direct.httpclient.data.CSRFToken;
import ru.yandex.autotests.direct.httpclient.data.banners.searchbanners.SearchBannersParameters;
import ru.yandex.autotests.direct.httpclient.steps.base.DirectBackEndSteps;
import ru.yandex.qatools.allure.annotations.Step;

/**
 * Created by shmykov on 16.06.15.
 */
public class SearchBannersSteps extends DirectBackEndSteps {

    @Step("Получаем ответ контроллера SearchBanners")
    public DirectResponse searchBanners(SearchBannersParameters params, CSRFToken token) {
        return execute(getRequestBuilder().get(CMD.SEARCH_BANNERS, token, params));
    }
}