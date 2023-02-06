package ru.yandex.autotests.direct.cmd.steps.feeds;

import ru.yandex.autotests.direct.cmd.data.CMD;
import ru.yandex.autotests.direct.cmd.steps.base.DirectBackEndSteps;
import ru.yandex.autotests.direct.cmd.data.feeds.AjaxFeedsResponse;
import ru.yandex.autotests.direct.cmd.data.feeds.AjaxGetFeedsRequest;
import ru.yandex.qatools.allure.annotations.Step;

public class AjaxGetFeedsSteps extends DirectBackEndSteps {

    @Step("Получаем ответ контроллера ajaxGetFeeds")
    public AjaxFeedsResponse getFeeds(String login) {
        AjaxGetFeedsRequest ajaxGetFeedsRequest = new AjaxGetFeedsRequest();
        ajaxGetFeedsRequest.setUlogin(login);
        return get(CMD.AJAX_GET_FEEDS, ajaxGetFeedsRequest, AjaxFeedsResponse.class);
    }
}