package ru.yandex.autotests.direct.cmd.adjustment.common;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import ru.yandex.autotests.direct.cmd.data.banners.GroupsParameters;
import ru.yandex.autotests.direct.cmd.data.campaigns.ShowCampMultiEditRequest;
import ru.yandex.autotests.direct.cmd.data.campaigns.ShowCampMultiEditResponse;
import ru.yandex.autotests.direct.cmd.data.commons.adjustment.HierarchicalMultipliers;
import ru.yandex.autotests.direct.cmd.data.commons.group.Group;
import ru.yandex.autotests.direct.cmd.rules.BannersRule;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.rules.TextBannersRule;
import ru.yandex.qatools.allure.annotations.Description;

import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;

public abstract class GroupMultiplierTestBase {

    protected final static String VALID_MULTIPLIER = "120";
    private final static String CLIENT = "at-direct-backend-c";
    @ClassRule
    public static DirectCmdRule classRule = DirectCmdRule.defaultClassRule();
    protected Long campaignId;
    private BannersRule bannersRule = new TextBannersRule().withUlogin(getClient());
    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule().withRules(bannersRule);

    @Before
    public void before() {
        campaignId = bannersRule.getCampaignId();
    }

    protected abstract HierarchicalMultipliers getHierarchicalMultipliers();

    protected HierarchicalMultipliers getExpectedHierarchicalMultipliers() {
        return getHierarchicalMultipliers();
    }

    @Description("Проверяем сохранение корректировок ставок контроллером saveTextAdGroups")
    public void checkSaveGroupMobileMultiplierAtSaveTextAdGroups() {
        GroupsParameters groupsParameters = GroupsParameters.
                forExistingCamp(getClient(), campaignId, getGroup());
        cmdRule.cmdSteps().groupsSteps().postSaveTextAdGroups(groupsParameters);

        ShowCampMultiEditRequest showCampMultiEditRequest = ShowCampMultiEditRequest.
                forSingleBanner(getClient(), campaignId, bannersRule.getGroupId(), bannersRule.getBannerId());
        ShowCampMultiEditResponse actualResponse = cmdRule.cmdSteps().campaignSteps().
                getShowCampMultiEdit(showCampMultiEditRequest);

        check(actualResponse);
    }

    protected String getClient() {
        return CLIENT;
    }

    private void check(ShowCampMultiEditResponse actualResponse) {
        assertThat("корректировки ставок сохранились", actualResponse.getCampaign()
                        .getGroups().get(0).getHierarchicalMultipliers(),
                beanDiffer(getExpectedHierarchicalMultipliers()));
    }

    protected Group getGroup() {
        Group group = bannersRule.getGroup().
                withAdGroupID(bannersRule.getGroupId().toString()).
                withHierarchicalMultipliers(getHierarchicalMultipliers());
        group.getBanners().get(0).withBid(bannersRule.getBannerId());
        return group;
    }
}
