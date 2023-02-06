package ru.yandex.autotests.direct.cmd.groups;

import java.util.Collections;
import ru.yandex.qatools.allure.annotations.TestCaseId;

import org.hamcrest.core.IsEqual;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.Logins;
import ru.yandex.autotests.direct.cmd.data.banners.GroupsParameters;
import ru.yandex.autotests.direct.cmd.data.commons.ErrorResponse;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.rules.TextBannersRule;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.direct.httpclient.data.textresources.groups.EditGroupsErrors;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

@Aqua.Test
@Description("Проверка валидации контроллера saveAdGroups")
@Stories(TestFeatures.Groups.BANNER_MULTI_SAVE)
@Features(TestFeatures.GROUPS)
@Tag(CmdTag.SAVE_TEXT_ADGROUPS)
@Tag(ObjectTag.GROUP)
@Tag(TrunkTag.YES)
public class SaveAdGroupsValidationTest {
    private static final String CLIENT = Logins.DEFAULT_CLIENT;
    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();
    private TextBannersRule bannersRule = new TextBannersRule().withUlogin(CLIENT);

    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule().withRules(bannersRule);

    @Test
    @Description("Проверка ошибки при вызове контроллера с пустым списком групп")
    @TestCaseId("11036")
    public void emptyGroupListTest() {
        GroupsParameters groupsParameters =
                GroupsParameters.forExistingCamp(CLIENT, bannersRule.getCampaignId(), Collections.emptyList());
        check(groupsParameters, EditGroupsErrors.NO_GROUPS_FOUND.toString());
    }

    private void check(GroupsParameters groupsParameters, String errorText) {
        ErrorResponse errorResponse =
                cmdRule.cmdSteps().groupsSteps().postSaveTextAdGroupsInvalidData(groupsParameters);
        assertThat("ошибка соотвествует ожидаемой", errorResponse.getError(), IsEqual.equalTo(errorText));
    }

}
