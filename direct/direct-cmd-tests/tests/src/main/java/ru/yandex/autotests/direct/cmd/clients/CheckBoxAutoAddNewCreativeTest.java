package ru.yandex.autotests.direct.cmd.clients;

import org.hamcrest.Matcher;
import org.junit.After;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
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

// таск: testirt-9730
@Aqua.Test
@Description("проверка значения is_agreed_on_creatives_autogeneration при наличии perfomance кампании")
@Stories(TestFeatures.Client.USER_SETTINGS)
@Features(TestFeatures.CLIENT)
@Tag(CmdTag.USER_SETTINGS)
@Tag(ObjectTag.USER)
@Tag(TrunkTag.YES)
public class CheckBoxAutoAddNewCreativeTest {
    private static final String CLIENT = "at-direct-auto-add-creative";

    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();

    private PerformanceBannersRule bannersRule = new PerformanceBannersRule().withUlogin(CLIENT);
    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule().withRules(bannersRule);

    @Test
    @Description("при наличии смарт-кампаний сохраняется значение 0")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9569")
    public void withPerfomanceCampsTurnOff() {
        SettingsModel userSettings = cmdRule.cmdSteps().userSettingsSteps().getUserSettings(CLIENT);
        userSettings.setUlogin(CLIENT);
        userSettings.withIsAgreedOnCreativesAutogeneration(0);
        cmdRule.cmdSteps().userSettingsSteps().postSaveSettings(userSettings);
        check(equalTo(0));
    }

    @Test
    @Description("при наличии смарт-кампаний стартовое значение 1")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9570")
    public void withPerfomanceCampsTurnOn() {
        check(equalTo(1));
    }

    @After
    public void after() {
        SettingsModel userSettings = cmdRule.cmdSteps().userSettingsSteps().getUserSettings(CLIENT);
        userSettings.setUlogin(CLIENT);
        userSettings.withIsAgreedOnCreativesAutogeneration(1);
        cmdRule.cmdSteps().userSettingsSteps().postSaveSettings(userSettings);
    }

    private void check(Matcher<Integer> matcher) {
        SettingsModel userSettings = cmdRule.cmdSteps().userSettingsSteps().getUserSettings(CLIENT);
        assertThat("значение is_agreed_on_creatives_autogeneration соответствует ожиданиям",
                userSettings.getIsAgreedOnCreativesAutogeneration(),
                matcher
        );
    }
}
