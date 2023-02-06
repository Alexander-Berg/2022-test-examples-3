
package ru.yandex.autotests.direct.cmd.steps.groups;

import ru.yandex.autotests.direct.cmd.data.ajaxupdatehowconditions.AjaxUpdateShowConditions;
import ru.yandex.autotests.direct.cmd.data.ajaxupdatehowconditions.AjaxUpdateShowConditionsObjects;
import ru.yandex.autotests.direct.cmd.data.ajaxupdatehowconditions.AjaxUpdateShowConditionsRequest;
import ru.yandex.autotests.direct.cmd.data.banners.GroupsParameters;
import ru.yandex.autotests.direct.cmd.data.commons.ErrorResponse;
import ru.yandex.autotests.direct.cmd.data.commons.group.Group;
import ru.yandex.autotests.direct.cmd.data.performanceGroups.savePerformanceAdGroups.GroupErrorsResponse;
import ru.yandex.autotests.direct.cmd.rules.BannersRule;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.db.models.jooq.ppc.tables.records.GroupParamsRecord;
import ru.yandex.autotests.direct.utils.campaigns.CampaignTypeEnum;

import static org.hamcrest.Matchers.nullValue;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assumeThat;

public class GroupHelper {
    public static Group saveAdGroup(DirectCmdRule cmdRule, String ulogin, Long campaignId, Group group,
            CampaignTypeEnum mediaType)
    {
        cmdRule.cmdSteps().groupsSteps().postSaveTextAdGroups(prepareGroupRequest(cmdRule, ulogin, campaignId, group, mediaType));
        return cmdRule.cmdSteps().groupsSteps().getGroups(ulogin, campaignId).get(0);
    }

    public static GroupErrorsResponse saveInvalidAdGroup(DirectCmdRule cmdRule, String ulogin, Long campaignId, Group group,
            CampaignTypeEnum mediaType)
    {
        return cmdRule.cmdSteps().groupsSteps().postSaveTextAdGroupsInvalidData(prepareGroupRequest(cmdRule, ulogin, campaignId, group, mediaType));
    }

    private static GroupsParameters prepareGroupRequest(DirectCmdRule cmdRule, String ulogin, Long campaignId, Group group,
            CampaignTypeEnum mediaType)
    {
        cmdRule.cmdSteps().groupsSteps().prepareGroupForUpdate(group, mediaType);
        return GroupsParameters.forExistingCamp(ulogin, campaignId, group);
    }

}
