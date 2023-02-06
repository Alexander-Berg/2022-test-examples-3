package ru.yandex.autotests.direct.cmd.steps.campaings;

import ru.yandex.autotests.direct.cmd.data.CMD;
import ru.yandex.autotests.direct.cmd.data.campaigns.SetCampDontShowMultiRequest;
import ru.yandex.autotests.direct.cmd.data.campaigns.SetCampDontShowMultiResponse;
import ru.yandex.autotests.direct.cmd.steps.base.DirectBackEndSteps;
import ru.yandex.qatools.allure.annotations.Step;

import java.util.List;

public class SetCampDontShowMultiSteps extends DirectBackEndSteps {

    @Step("POST cmd = setCampDontShowMulti (изменение списка запрещенных площадок/ssp-платформ)")
    public SetCampDontShowMultiResponse postSetCampDontShowMulti(SetCampDontShowMultiRequest request) {
        return post(CMD.SET_CAMP_DONT_SHOW_MULTI, request, SetCampDontShowMultiResponse.class);
    }

    @Step("Добавить в список запрещенные площадки и ssp-платформы (login = {0}, cid = {1}, запрещенные: {2}")
    public SetCampDontShowMultiResponse disableShow(String uLogin,
                                                    Long campaignId,
                                                    List<String> pagesAndPlatforms) {
        return postSetCampDontShowMulti(new SetCampDontShowMultiRequest().
                withOp(SetCampDontShowMultiRequest.Operation.DISABLE).
                withCampaignId(campaignId).
                withPagesChecked(pagesAndPlatforms).
                withUlogin(uLogin));
    }

    @Step("Добавить в список запрещенные площадки и ssp-платформы (login = {0}, cid = {1}, запрещенные: {2}")
    public SetCampDontShowMultiResponse enableShow(String uLogin,
                                                   Long campaignId,
                                                   List<String> pagesAndPlatforms) {
        return postSetCampDontShowMulti(new SetCampDontShowMultiRequest().
                withOp(SetCampDontShowMultiRequest.Operation.ENABLE).
                withCampaignId(campaignId).
                withPagesChecked(pagesAndPlatforms).
                withUlogin(uLogin));
    }
}
