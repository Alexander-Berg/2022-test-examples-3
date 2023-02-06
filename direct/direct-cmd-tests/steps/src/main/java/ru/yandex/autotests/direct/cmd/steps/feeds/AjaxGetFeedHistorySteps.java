package ru.yandex.autotests.direct.cmd.steps.feeds;

import ru.yandex.autotests.direct.cmd.data.CMD;
import ru.yandex.autotests.direct.cmd.data.feeds.AjaxGetFeedHistoryRequest;
import ru.yandex.autotests.direct.cmd.data.feeds.AjaxGetFeedHistoryResponse;
import ru.yandex.autotests.direct.cmd.steps.base.DirectBackEndSteps;
import ru.yandex.qatools.allure.annotations.Step;

public class AjaxGetFeedHistorySteps extends DirectBackEndSteps {

    @Step("GET cmd = ajaxGetFeedHistory (Получаем историю фида)")
    public AjaxGetFeedHistoryResponse getFeedHistory(AjaxGetFeedHistoryRequest request) {
        return get(CMD.AJAX_GET_FEED_HISTORY, request, AjaxGetFeedHistoryResponse.class);
    }

    @Step("Получаем историю фида с ид = {0} у пользователя {1}")
    public AjaxGetFeedHistoryResponse getFeedHistory(String feedId, String uLogin) {
        return getFeedHistory(new AjaxGetFeedHistoryRequest().withFeedId(feedId).withUlogin(uLogin));
    }
}
