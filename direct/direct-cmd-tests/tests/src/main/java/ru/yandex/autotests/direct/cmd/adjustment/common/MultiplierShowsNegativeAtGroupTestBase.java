package ru.yandex.autotests.direct.cmd.adjustment.common;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import ru.yandex.autotests.direct.cmd.data.banners.GroupsParameters;
import ru.yandex.autotests.direct.cmd.data.commons.ErrorResponse;
import ru.yandex.autotests.direct.cmd.data.commons.adjustment.HierarchicalMultipliers;
import ru.yandex.autotests.direct.cmd.data.commons.group.Group;
import ru.yandex.autotests.direct.cmd.rules.BannersRule;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.rules.TextBannersRule;
import ru.yandex.qatools.allure.annotations.Description;

import static org.hamcrest.CoreMatchers.containsString;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

public abstract class MultiplierShowsNegativeAtGroupTestBase {

    protected final static String VALID_MULTIPLIER = "120";
    private final static String CLIENT = "at-direct-backend-c";
    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();

    private BannersRule bannersRule = new TextBannersRule().withUlogin(getClient());

    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule().withRules(bannersRule);


    private Long campaignId;

    @Before
    public void before() {
        campaignId = bannersRule.getCampaignId();
    }

    protected abstract HierarchicalMultipliers getHierarchicalMultipliers();

    protected abstract String[] getErrorText();

    protected abstract String getSaveGroupErrorText();

    protected String getClient() {
        return CLIENT;
    }

    @Description("Проверяем валидацию при сохранении некорректных корректировок ставок контроллером saveTextAdGroups")
    public void checkSaveGroupMobileMultiplierAtSaveTextAdGroups() {
        GroupsParameters groupsParameters = GroupsParameters.forExistingCamp(getClient(), campaignId, getGroup());
        ErrorResponse errorResponse = cmdRule.cmdSteps().groupsSteps().
                postSaveTextAdGroupsInvalidData(groupsParameters);

        assertThat("корректировки ставок не сохранились", errorResponse.getError(),
                containsString(getSaveGroupErrorText()));
    }

    private Group getGroup() {
        Group group = bannersRule.getGroup().
                withAdGroupID(bannersRule.getGroupId().toString()).
                withHierarchicalMultipliers(getHierarchicalMultipliers());
        group.getBanners().get(0).withBid(bannersRule.getBannerId());
        return group;
    }
}
