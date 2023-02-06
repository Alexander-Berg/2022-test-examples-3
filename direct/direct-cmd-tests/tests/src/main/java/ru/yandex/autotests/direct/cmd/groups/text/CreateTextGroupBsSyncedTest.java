package ru.yandex.autotests.direct.cmd.groups.text;//Task: .

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.Logins;
import ru.yandex.autotests.direct.cmd.rules.BannersRule;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.rules.TextBannersRule;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.directapi.enums.StatusBsSynced;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static org.hamcrest.core.IsEqual.equalTo;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

@Aqua.Test
@Description("Проверка статуса синхронизации текстовой-группы при создании")
@Stories(TestFeatures.Groups.BANNER_MULTI_SAVE)
@Features(TestFeatures.GROUPS)
@Tag(CmdTag.SAVE_TEXT_ADGROUPS)
@Tag(ObjectTag.GROUP)
@Tag(CampTypeTag.TEXT)
@Tag(TrunkTag.YES)
public class CreateTextGroupBsSyncedTest {
    private static final String CLIENT = Logins.DEFAULT_CLIENT;
    ;
    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();
    @Rule
    public DirectCmdRule cmdRule;
    protected BannersRule bannersRule;


    public CreateTextGroupBsSyncedTest() {
        bannersRule = new TextBannersRule().withUlogin(CLIENT);
        cmdRule = DirectCmdRule.defaultRule().withRules(bannersRule);
    }

    @Test
    @Description("статус синхрониации группы при создании")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9854")
    public void checkNewGroupBsSyncedStatus() {
        assertThat(
                "статус синхронизации соответcтвует ожиданиям",
                cmdRule.apiSteps().groupFakeSteps().getGroupParams(bannersRule.getGroupId()).getStatusBsSynced(),
                equalTo(StatusBsSynced.NO.toString())
        );
    }

}
