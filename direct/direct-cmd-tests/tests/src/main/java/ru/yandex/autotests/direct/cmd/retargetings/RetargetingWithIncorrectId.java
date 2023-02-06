package ru.yandex.autotests.direct.cmd.retargetings;

import org.apache.commons.lang3.RandomUtils;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.Logins;
import ru.yandex.autotests.direct.cmd.data.banners.GroupsParameters;
import ru.yandex.autotests.direct.cmd.data.commons.ErrorResponse;
import ru.yandex.autotests.direct.cmd.data.commons.group.Group;
import ru.yandex.autotests.direct.cmd.data.commons.group.Retargeting;
import ru.yandex.autotests.direct.cmd.data.groups.GroupsFactory;
import ru.yandex.autotests.direct.cmd.rules.CampaignRule;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.direct.httpclient.data.textresources.banners.GroupsErrorsEnum;
import ru.yandex.autotests.direct.utils.campaigns.CampaignTypeEnum;
import ru.yandex.autotests.direct.utils.textresource.TextResourceFormatter;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import java.util.Collections;

import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

/**
 * https://st.yandex-team.ru/DIRECT-42003
 */
@Aqua.Test
@Description("Невозможно создать группу со случайным id ретагретинга")
@Stories(TestFeatures.Groups.BANNER_MULTI_SAVE)
@Features(TestFeatures.GROUPS)
@Tag(CmdTag.SAVE_TEXT_ADGROUPS)
@Tag(ObjectTag.GROUP)
@Tag(ObjectTag.RETAGRETING)
@Tag(CampTypeTag.TEXT)
public class RetargetingWithIncorrectId {

    private static final String CLIENT = Logins.DEFAULT_CLIENT;

    @ClassRule
    public static DirectCmdRule directCmdClassRule = DirectCmdRule.defaultClassRule();

    private CampaignRule campaignRule = new CampaignRule().
            withMediaType(CampaignTypeEnum.TEXT).
            withUlogin(CLIENT);

    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule().withRules(campaignRule);


    private Long campaignId;
    private Long wrongRetargetingId;

    @Before
    public void before() {
        campaignId = campaignRule.getCampaignId();
        wrongRetargetingId = RandomUtils.nextLong(1, Integer.MAX_VALUE);
    }

    @Test
    @Description("Невозможно создать группу со случайным id ретагретинга")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9936")
    public void testCreateGroupWithRetargetingWithIncorrectId() {
        Group savingGroup = getGroupWithIncorrectId();

        GroupsParameters groupRequest = GroupsParameters.forNewCamp(CLIENT, campaignId, savingGroup);
        ErrorResponse response = cmdRule.cmdSteps().groupsSteps().postSaveTextAdGroupsInvalidData(groupRequest);

        String actualError = response.getError();
        String expectedError = String.format(
                TextResourceFormatter.resource(GroupsErrorsEnum.RETARGETING_CONDITION_NOT_FOUND).toString(),
                wrongRetargetingId);
        assertThat("в ответе присутствует ожидаемая ошибка", actualError, equalTo(expectedError));
    }

    private Group getGroupWithIncorrectId() {
        Retargeting invalidRetargeting = new Retargeting().withRetCondId(wrongRetargetingId).withPriceContext("250");
        return GroupsFactory.getDefaultTextGroup()
                .withRetargetings(Collections.singletonList(invalidRetargeting));
    }
}
