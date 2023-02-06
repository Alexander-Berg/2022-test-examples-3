package ru.yandex.autotests.direct.cmd.groups.dynamic;

import com.google.gson.Gson;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import ru.yandex.autotests.direct.cmd.data.CmdBeans;
import ru.yandex.autotests.direct.cmd.data.Logins;
import ru.yandex.autotests.direct.cmd.data.banners.GroupsParameters;
import ru.yandex.autotests.direct.cmd.data.commons.group.Group;
import ru.yandex.autotests.direct.cmd.data.editdynamicadgroups.EditDynamicAdGroupsRequest;
import ru.yandex.autotests.direct.cmd.data.editdynamicadgroups.EditDynamicAdGroupsResponse;
import ru.yandex.autotests.direct.cmd.data.redirect.RedirectResponse;
import ru.yandex.autotests.direct.cmd.data.savecamp.SaveCampRequest;
import ru.yandex.autotests.direct.cmd.data.showcamp.ShowCampResponse;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.util.BeanLoadHelper;
import ru.yandex.autotests.direct.httpclient.TestEnvironment;
import ru.yandex.autotests.irt.testutils.allure.AssumptionException;

import static ru.yandex.autotests.direct.cmd.steps.campaings.CampaignSteps.extractCidFromSaveCampResponse;

/**
 * https://st.yandex-team.ru/TESTIRT-7909
 */
public abstract class DtoBaseTest {
    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();
    protected static String CLIENT = Logins.DEFAULT_CLIENT;
    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule();
    protected int shard;
    protected GroupsParameters groupRequest;
    protected Group savingGroup;
    protected Long campaignId;
    protected Long groupId;
    protected Long bannerId;

    @Before
    public void before() {
        createCampaign();
        createGroupAndGetIds();
        shard = TestEnvironment.newDbSteps().shardingSteps().getShardByCid(campaignId);
    }

    protected void createCampaign() {
        SaveCampRequest saveCampRequest = BeanLoadHelper.loadCmdBean(
                CmdBeans.SAVE_NEW_DYNAMIC_CAMP_FULL, SaveCampRequest.class);
        saveCampRequest.setUlogin(CLIENT);
        RedirectResponse saveNewCampResponse = cmdRule.cmdSteps().campaignSteps().postSaveNewCamp(saveCampRequest);
        campaignId = extractCidFromSaveCampResponse(saveNewCampResponse);
    }

    protected void createGroupAndGetIds() {
        createGroup();
        getCreatedIds();
        cmdRule.apiSteps().bannerSteps().moderateBanner(bannerId);
    }

    protected void createGroup() {
        savingGroup = getDynamicGroup();

        groupRequest = getGroupRequest();
        groupRequest.setUlogin(CLIENT);
        groupRequest.setCid(campaignId.toString());

        groupRequest.setJsonGroups(new Gson().toJson(new Group[]{savingGroup}));
        cmdRule.cmdSteps().groupsSteps().postSaveDynamicAdGroups(groupRequest);
    }

    protected void getCreatedIds() {
        ShowCampResponse showCamp = cmdRule.cmdSteps().campaignSteps().getShowCamp(CLIENT, campaignId.toString());
        groupId = showCamp.getGroups().stream()
                .findFirst()
                .orElseThrow(() -> new AssumptionException("Ожидалось что в кампании есть группа"))
                .getAdGroupId();
        bannerId = showCamp.getGroups().stream()
                .findFirst()
                .orElseThrow(() -> new AssumptionException("Ожидалось что в кампании есть группа"))
                .getBid();
    }

    protected EditDynamicAdGroupsResponse getCreatedGroup() {
        EditDynamicAdGroupsRequest editDynamicAdGroupsRequest = new EditDynamicAdGroupsRequest();
        editDynamicAdGroupsRequest.setAdGroupIds(String.valueOf(groupId));
        editDynamicAdGroupsRequest.setBannerStatus("all");
        editDynamicAdGroupsRequest.setCid(campaignId);
        editDynamicAdGroupsRequest.setUlogin(CLIENT);
        return cmdRule.cmdSteps().groupsSteps().getEditDynamicAdGroups(editDynamicAdGroupsRequest);
    }

    protected ShowCampResponse getCreatedCamp() {
        return cmdRule.cmdSteps().campaignSteps().getShowCamp(CLIENT, campaignId.toString());
    }

    @After
    public void deleteCampaign() {
        if (campaignId != null) {
            cmdRule.apiAggregationSteps().makeCampaignReadyForDelete(campaignId);
            cmdRule.cmdSteps().campaignSteps().deleteCampaign(CLIENT, campaignId);
        }
    }

    protected Group getDynamicGroup() {
        return BeanLoadHelper.loadCmdBean(CmdBeans.COMMON_REQUEST_GROUP_DYNAMIC_DEFAULT2, Group.class);
    }

    protected GroupsParameters getGroupRequest() {
        return BeanLoadHelper.loadCmdBean(CmdBeans.COMMON_REQUEST_GROUP_DYNAMIC_PARAM, GroupsParameters.class);
    }

}
