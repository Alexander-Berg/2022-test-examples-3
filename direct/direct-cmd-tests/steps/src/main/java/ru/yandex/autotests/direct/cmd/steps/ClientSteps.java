package ru.yandex.autotests.direct.cmd.steps;

import com.google.gson.Gson;

import ru.yandex.autotests.direct.cmd.data.CmdBeans;
import ru.yandex.autotests.direct.cmd.data.banners.GroupsParameters;
import ru.yandex.autotests.direct.cmd.data.commons.group.Group;
import ru.yandex.autotests.direct.cmd.data.redirect.RedirectResponse;
import ru.yandex.autotests.direct.cmd.data.savecamp.SaveCampRequest;
import ru.yandex.autotests.direct.cmd.steps.base.DirectBackEndSteps;
import ru.yandex.autotests.direct.cmd.steps.campaings.CampaignSteps;
import ru.yandex.autotests.direct.cmd.steps.groups.GroupsSteps;
import ru.yandex.autotests.direct.cmd.util.BeanLoadHelper;

import static ru.yandex.autotests.direct.cmd.steps.campaings.CampaignSteps.extractCidFromSaveCampResponse;

/**
 * Created by aleran on 15.12.2015.
 */
public class ClientSteps extends DirectBackEndSteps {

    public CampaignSteps campaignSteps() {
        return getInstance(CampaignSteps.class, getContext());
    }

    public GroupsSteps groupsSteps() {
        return getInstance(GroupsSteps.class, getContext());
    }

    public Long createCampaignWithoutGroup(SaveCampRequest saveCampRequest) {
        RedirectResponse redirectResponse = campaignSteps().postSaveNewCamp(saveCampRequest);
        return extractCidFromSaveCampResponse(redirectResponse);
    }

    public Long createCampaignWithoutGroup(String login, String campaignTemplate) {
        SaveCampRequest saveCampRequest = BeanLoadHelper.loadCmdBean(campaignTemplate, SaveCampRequest.class);
        saveCampRequest.setUlogin(login);
        return createCampaignWithoutGroup(saveCampRequest);
    }

    public void createDynamicGroup(String login, Long campaignId, String groupTemplate) {
        Group savingGroup = BeanLoadHelper.loadCmdBean(groupTemplate, Group.class);
        GroupsParameters groupRequest = new GroupsParameters();
        groupRequest.setUlogin(login);
        groupRequest.setCid(campaignId.toString());
        groupRequest.setJsonGroups(new Gson().toJson(new Group[]{savingGroup}));
        groupsSteps().postSaveDynamicAdGroups(groupRequest);
    }

    public Long createDynamicCampaign(String login, String campaignTemplate, String groupTemplate) {
        Long campaignId = createCampaignWithoutGroup(login, campaignTemplate);
        createDynamicGroup(login, campaignId, groupTemplate);
        return campaignId;
    }

    public Long createDefaultDynamicCampaign(String login) {
        Long campaignId = createCampaignWithoutGroup(login, CmdBeans.SAVE_NEW_DYNAMIC_CAMP_FULL);
        createDynamicGroup(login, campaignId, CmdBeans.COMMON_REQUEST_GROUP_DYNAMIC_DEFAULT2);
        return campaignId;
    }

    public void createMobileGroup(String login, Long campaignId, String groupTemplate) {
        Group savingGroup = BeanLoadHelper.loadCmdBean(groupTemplate, Group.class);
        GroupsParameters groupRequest = new GroupsParameters();
        groupRequest.setUlogin(login);
        groupRequest.setCid(campaignId.toString());
        groupRequest.setJsonGroups(new Gson().toJson(new Group[]{savingGroup}));
        groupsSteps().postSaveMobileAdGroups(groupRequest);
    }

    public Long createMobileCampaign(String login, String campaignTemplate, String groupTemplate) {
        Long campaignId = createCampaignWithoutGroup(login, campaignTemplate);
        createMobileGroup(login, campaignId, groupTemplate);
        return campaignId;
    }
}
