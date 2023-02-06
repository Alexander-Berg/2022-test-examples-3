package ru.yandex.autotests.direct.cmd.steps.feeds;

import ru.yandex.autotests.direct.cmd.data.CMD;
import ru.yandex.autotests.direct.cmd.data.CSRFToken;
import ru.yandex.autotests.direct.cmd.steps.base.DirectBackEndSteps;
import ru.yandex.autotests.direct.cmd.data.feeds.FeedSaveRequest;
import ru.yandex.autotests.direct.cmd.steps.base.DirectBeanWrapper;
import ru.yandex.autotests.httpclientlite.core.RequestBuilder;
import ru.yandex.autotests.httpclientlite.core.request.multipart.MultipartRequestBuilder;
import ru.yandex.qatools.allure.annotations.Step;

import static ru.yandex.autotests.direct.cmd.data.Headers.ACCEPT_JSON_HEADER;
import static ru.yandex.autotests.direct.cmd.data.Headers.X_REQUESTED_WITH_HEADER;

public class SaveFeedsSteps extends DirectBackEndSteps {

    @Override
    protected RequestBuilder buildRequestBuilder() {
        MultipartRequestBuilder requestBuilder = new MultipartRequestBuilder();
        requestBuilder.setHeaders(ACCEPT_JSON_HEADER, X_REQUESTED_WITH_HEADER);
        return requestBuilder;
    }

    @Step("Сохраняем фид")
    public <T> T saveFeed(FeedSaveRequest request, Class<T> classOfT) {
        return post(CMD.SAVE_FEED, request, classOfT);
    }

    @Step("Сохраняем фид с кастомным csrf-токеном {1}")
    public <T> T saveFeedWithCustomCsrfToken(FeedSaveRequest request, CSRFToken token, Class<T> classOfT) {
        DirectBeanWrapper wrapper = new DirectBeanWrapper(CMD.SAVE_FEED, token, request);
        return executeRaw(RequestBuilder.Method.POST, wrapper, classOfT);
    }
}
