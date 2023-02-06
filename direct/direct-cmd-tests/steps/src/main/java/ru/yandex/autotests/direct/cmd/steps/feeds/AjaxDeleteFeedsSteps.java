package ru.yandex.autotests.direct.cmd.steps.feeds;

import ru.yandex.autotests.direct.cmd.data.CMD;
import ru.yandex.autotests.direct.cmd.data.feeds.AjaxDeleteFeedsRequest;
import ru.yandex.autotests.direct.cmd.data.feeds.AjaxDeleteFeedsResponse;
import ru.yandex.autotests.direct.cmd.steps.base.DirectBackEndSteps;
import ru.yandex.autotests.irt.testutils.json.JsonUtils;
import ru.yandex.qatools.allure.annotations.Step;

import java.util.ArrayList;
import java.util.List;

import static ru.yandex.autotests.irt.testutils.allure.AllureUtils.addJsonAttachment;

public class AjaxDeleteFeedsSteps extends DirectBackEndSteps {

    @Step("Удаляем фид")
    public AjaxDeleteFeedsResponse deleteFeed(AjaxDeleteFeedsRequest request) {
        return get(CMD.AJAX_DELETE_FEEDS, request, AjaxDeleteFeedsResponse.class);
    }

    @Step("Удаляем фиды")
    public AjaxDeleteFeedsResponse deleteFeeds(String login, List<Long> feedsIds) {
        addJsonAttachment("Ид фидов", JsonUtils.toStringLow(feedsIds));
        AjaxDeleteFeedsRequest request = new AjaxDeleteFeedsRequest();
        request.setUlogin(login);
        request.setFeedsIdsByLong(feedsIds);
        return get(CMD.AJAX_DELETE_FEEDS, request, AjaxDeleteFeedsResponse.class);
    }

    public AjaxDeleteFeedsResponse deleteFeed(String login, Long feedsId) {
        List<Long> feedsIds = new ArrayList<>();
        feedsIds.add(feedsId);
        return deleteFeeds(login, feedsIds);
    }
}
