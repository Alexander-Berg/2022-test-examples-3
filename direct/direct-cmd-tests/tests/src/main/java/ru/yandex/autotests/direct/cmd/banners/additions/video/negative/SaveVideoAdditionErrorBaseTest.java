package ru.yandex.autotests.direct.cmd.banners.additions.video.negative;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;

import ru.yandex.autotests.direct.cmd.bssynced.BsSyncedHelper;
import ru.yandex.autotests.direct.cmd.data.banners.GroupsParameters;
import ru.yandex.autotests.direct.cmd.data.commons.group.Group;
import ru.yandex.autotests.direct.cmd.data.performanceGroups.savePerformanceAdGroups.GroupErrorsResponse;
import ru.yandex.autotests.direct.cmd.rules.BannersRule;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.rules.TextBannersRule;
import ru.yandex.autotests.direct.cmd.rules.VideoAdditionCreativeRule;
import ru.yandex.autotests.direct.db.steps.DirectJooqDbSteps;
import ru.yandex.autotests.direct.httpclient.TestEnvironment;
import ru.yandex.autotests.direct.httpclient.data.textresources.groups.EditGroupsErrors;

import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

public abstract class SaveVideoAdditionErrorBaseTest {
    protected static final String CLIENT = "at-direct-video-addition-1";
    static final DirectJooqDbSteps dbSteps = TestEnvironment.newDbSteps(CLIENT);

    private VideoAdditionCreativeRule videoAdditionCreativeRule = new VideoAdditionCreativeRule(CLIENT);
    final BannersRule bannerRule = new TextBannersRule()
            .withVideoAddition(videoAdditionCreativeRule)
            .withUlogin(CLIENT);

    @ClassRule
    public static DirectCmdRule classRule = DirectCmdRule.defaultClassRule();

    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule()
            .withRules(videoAdditionCreativeRule, bannerRule);

    @Before
    public void before() {
        BsSyncedHelper.makeCampSynced(cmdRule, bannerRule.getCampaignId());
    }

    protected abstract Group modifyTestGroup(Group group);

    public void saveAndCheckResponse() {
        Group group = bannerRule.getGroupForUpdate();
        GroupsParameters request =
                GroupsParameters.forExistingCamp(CLIENT, bannerRule.getCampaignId(), modifyTestGroup(group));
        GroupErrorsResponse errorsResponse =
                cmdRule.cmdSteps().groupsSteps().postSaveTextAdGroupsInvalidData(request);

        assertThat("получена страница с ошибкой", errorsResponse.getError(),
                equalTo(EditGroupsErrors.VIDEO_ADDITION_NOT_FOUND.toString()));
    }
}
