package ru.yandex.autotests.direct.httpclient.steps.campaigns;

import ru.yandex.autotests.direct.httpclient.core.DirectResponse;
import ru.yandex.autotests.direct.httpclient.data.CMD;
import ru.yandex.autotests.direct.httpclient.data.CSRFToken;
import ru.yandex.autotests.direct.httpclient.data.campaigns.CampUnarcRequestBean;
import ru.yandex.autotests.direct.httpclient.steps.base.DirectBackEndSteps;
import ru.yandex.qatools.allure.annotations.Step;

/**
 * @author : Alex Samokhin (alex-samo@yandex-team.ru)
 *         Date: 30.04.15
 */
public class CampUnarcSteps extends DirectBackEndSteps {

    @Step("Разархивируем кампании с помощью контроллера campUnarc")
    public DirectResponse unarchiveCampaign(CSRFToken token, CampUnarcRequestBean campUnarcRequestBean) {
        return execute(getRequestBuilder().get(CMD.CAMP_UNARC, token, campUnarcRequestBean));
    }

    @Step("Разархивируем кампании с помощью контроллера campUnarc и проверяем")
    public void unarchiveCampaignAndCheckResponse(CSRFToken token, CampUnarcRequestBean campUnarcRequestBean) {
        DirectResponse directResponse = execute(getRequestBuilder().get(CMD.CAMP_UNARC, token, campUnarcRequestBean));

    }
}
