package ru.yandex.autotests.direct.cmd.steps.campaings;

import java.util.stream.Stream;

import ru.yandex.autotests.direct.cmd.data.CMD;
import ru.yandex.autotests.direct.cmd.data.campaigns.DeleteCampaignsRequest;
import ru.yandex.autotests.direct.cmd.data.campaigns.UnarchiveCampiagnsRequest;
import ru.yandex.autotests.direct.cmd.steps.base.DirectBackEndSteps;
import ru.yandex.qatools.allure.annotations.Step;

import static java.util.stream.Collectors.toList;

public class CampsMultiActionSteps extends DirectBackEndSteps {

    public void deleteCampaigns(String login, Long... campaignIds) {
        DeleteCampaignsRequest deleteCampaignsRequest = new DeleteCampaignsRequest()
                .withCampaignIds(Stream.of(campaignIds).collect(toList()))
                .withUlogin(login);
        deleteCampaigns(deleteCampaignsRequest);
    }

    @Step("POST cmd = delCamp (удаление кампаний)")
    public void deleteCampaigns(DeleteCampaignsRequest deleteCampaignsRequest) {
        post(CMD.DEL_CAMP, deleteCampaignsRequest, Void.class);
    }

    public void unarchiveCampaigns(String login, Long... campaignIds) {
        UnarchiveCampiagnsRequest unarchiveCampiagnsRequest = new UnarchiveCampiagnsRequest()
                .withCampaingIds(Stream.of(campaignIds).collect(toList()))
                .withUlogin(login);
        unarchiveCampaigns(unarchiveCampiagnsRequest);
    }

    @Step("GET cmd = campUnarc (разархивирование кампаний)")
    public void unarchiveCampaigns(UnarchiveCampiagnsRequest unarchiveCampiagnsRequest) {
        get(CMD.CAMP_UNARC, unarchiveCampiagnsRequest, Void.class);
    }
}
