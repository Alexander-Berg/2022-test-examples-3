package ru.yandex.autotests.direct.cmd.steps.campaings;

import ru.yandex.autotests.direct.cmd.data.CMD;
import ru.yandex.autotests.direct.cmd.data.campaigns.CopyCampRequest;
import ru.yandex.autotests.direct.cmd.data.redirect.LocationParam;
import ru.yandex.autotests.direct.cmd.data.redirect.RedirectResponse;
import ru.yandex.autotests.direct.cmd.steps.base.DirectBackEndSteps;
import ru.yandex.qatools.allure.annotations.Step;

/**
 * ВАЖНО!
 * Скопированную кампанию нужно удалять вручную (через создание Rule или @After), иначе на логине кончаются компании.
 */
public class CopyCampSteps extends DirectBackEndSteps {

    @Step("POST cmd = copyCamp (копирование кампании)")
    public void postCopyCamp(CopyCampRequest request) {
        post(CMD.COPY_CAMP, request, Void.class);
    }

    @Step("Копирование кампании внутри клиента (синхронное; login = {0}; cid = {1}")
    public Long copyCampWithinClient(String login, Long campaignId) {
        CopyCampRequest request = new CopyCampRequest()
                .withNewLogin(login)
                .withOldLogin(login)
                .withCidFrom(campaignId.toString())
                .withReason("dummy");
        RedirectResponse response = post(CMD.COPY_CAMP, request, RedirectResponse.class);
        return response.getLocationParamAsLong(LocationParam.RESULT_NEW_CID);
    }

    @Step("Копирование кампании между клиентами (синхронное; fromLogin = {0}; toLogin={1}, cid = {2}, copyModerateStatus = {3}")
    public Long copyCamp(String fromLogin, String toLogin, Long campaignId, String copyModerateStatus) {
        CopyCampRequest request = new CopyCampRequest()
                .withOldLogin(fromLogin)
                .withNewLogin(toLogin)
                .withCopyModerateStatus(copyModerateStatus)
                .withCidFrom(campaignId.toString())
                .withReason("dummy");
        RedirectResponse response = post(CMD.COPY_CAMP, request, RedirectResponse.class);
        return response.getLocationParamAsLong(LocationParam.RESULT_NEW_CID);
    }

}
