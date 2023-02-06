package ru.yandex.autotests.direct.cmd.clients;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.clients.SettingsModel;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.cmd.util.BeanLoadHelper;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assumeThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;

// TODO После открытия всем дополнить проверкой на клиента

@Aqua.Test
@Description("Проверка сохранения параметров пользователя, опции только для супера (контроллер saveSettings)")
@Stories(TestFeatures.Client.USER_SETTINGS)
@Features(TestFeatures.CLIENT)
@Tag(CmdTag.SAVE_SETTINGS)
@Tag(ObjectTag.USER)
@Tag(TrunkTag.YES)
@RunWith(Parameterized.class)
public class SaveUserSettingsSuperTest {
    protected static final String CLIENT = "at-direct-back-user-settings";

    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();

    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule();

    private SettingsModel expectedSettings;

    @Parameterized.Parameter(0)
    public String beforeValue;

    @Parameterized.Parameter(1)
    public String afterValue;

    @Parameterized.Parameters(name = "Меняем значение auto_video с {0} на {1}")
    public static Collection<Object[]> testData() {
        return Arrays.asList(new Object[][]{
                {"0", "1"},
                {"1", "0"},
        });
    }

    @Before
    public void before() {
        expectedSettings = BeanLoadHelper.loadCmdBean("cmd.saveSettings.request.default", SettingsModel.class);
        expectedSettings.setAutoVideo(beforeValue);
        expectedSettings.setUlogin(CLIENT);
        cmdRule.cmdSteps().userSettingsSteps().postSaveSettings(expectedSettings);
        SettingsModel actualSettings = cmdRule.cmdSteps().userSettingsSteps().getUserSettings(CLIENT);
        assumeThat("параметр auto_video сохранился", actualSettings.getAutoVideo(), equalTo(beforeValue));
    }

    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("10658")
    public void checkChangeAutoVideo() {
        expectedSettings.setAutoVideo(afterValue);
        cmdRule.cmdSteps().userSettingsSteps().postSaveSettings(expectedSettings);
        check();
    }

    private void check() {
        SettingsModel actualSettings = cmdRule.cmdSteps().userSettingsSteps().getUserSettings(CLIENT);
        assertThat("параметр auto_video сохранился", actualSettings,
                beanDiffer(prepareExpectedSettings(expectedSettings)).useCompareStrategy(onlyExpectedFields()));
    }

    private SettingsModel prepareExpectedSettings(SettingsModel expectedSettings) {
        return expectedSettings.withNews(null).withWarn(null).withUlogin(null);
    }
}
