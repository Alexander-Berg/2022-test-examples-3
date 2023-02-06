package ru.yandex.autotests.direct.cmd.clients;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.Logins;
import ru.yandex.autotests.direct.cmd.data.clients.SettingsModel;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.rules.PerformanceBannersRule;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

// таск : https://st.yandex-team.ru/TESTIRT-10013
@Aqua.Test
@Description("can_use_day_budget всегда отдает 1")
@Stories(TestFeatures.Client.USER_SETTINGS)
@Features(TestFeatures.CLIENT)
@Tag(CmdTag.USER_SETTINGS)
@Tag(ObjectTag.USER)
@Tag(TrunkTag.YES)
public class CanUseDayBudgetAlwaysTrueTest {
    private static final String CLIENT = Logins.DEFAULT_CLIENT;

    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();

    private PerformanceBannersRule bannersRule = new PerformanceBannersRule().withUlogin(CLIENT);
    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule().withRules(bannersRule);

    @Test
    @Description("can_use_day_budget по умолчанию 1")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9567")
    public void canUseDayBudgetDefaultTrue() {
        SettingsModel userSettings = cmdRule.cmdSteps().userSettingsSteps().getUserSettings(CLIENT);
        assertThat("значение can_use_day_budget true", userSettings.getClient().getCanUseDayBudget(), equalTo(1));
    }

    @Test
    @Description("can_use_day_budget не меняется")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9568")
    public void canUseDayBudgetImmutable() {
        SettingsModel userSettings = cmdRule.cmdSteps().userSettingsSteps().getUserSettings(CLIENT);
        userSettings.setUlogin(CLIENT);
        userSettings.getClient().setCanUseDayBudget(0);
        cmdRule.cmdSteps().userSettingsSteps().postSaveSettings(userSettings);
        SettingsModel actualUserSettings = cmdRule.cmdSteps().userSettingsSteps().getUserSettings(CLIENT);
        assertThat("значение can_use_day_budget true", actualUserSettings.getClient().getCanUseDayBudget(), equalTo(1));
    }
}
