package ru.yandex.autotests.direct.cmd.clients;

import java.util.Arrays;
import java.util.Collection;

import org.junit.ClassRule;
import org.junit.Ignore;
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
import ru.yandex.autotests.directapi.model.User;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;

// TODO убрать тест после открытия на клиента

@Aqua.Test
@Description("Параметры пользователя auto_video сохраняются только для пользователя с флагом allow_edit_auto_video_flag = 1, для остальных всегда 0")
@Stories(TestFeatures.Client.USER_SETTINGS)
@Features(TestFeatures.CLIENT)
@Tag(CmdTag.SAVE_SETTINGS)
@Tag(ObjectTag.USER)
@Tag(TrunkTag.YES)
@Ignore
@RunWith(Parameterized.class)
public class SaveUserSettingsNegativeTest {
    protected static final String CLIENT = "at-direct-back-user-settings3";

    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();

    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule();

    private SettingsModel expectedSettings;

    @Parameterized.Parameter(0)
    public String setValue;

    @Parameterized.Parameter(1)
    public String expValue;

    @Parameterized.Parameters(name = "Пытаемся поменять значение auto_video с {0} на {1}")
    public static Collection<Object[]> testData() {
        return Arrays.asList(new Object[][]{
                {"0", "0"},
                {"1", "0"},
        });
    }

    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("10657")
    public void checkNoChangeAutoVideo() {
        expectedSettings = BeanLoadHelper.loadCmdBean("cmd.saveSettings.request.default", SettingsModel.class);
        expectedSettings.setAutoVideo(setValue);
        expectedSettings.setUlogin(CLIENT);
        cmdRule.cmdSteps().authSteps().authenticate(User.get(CLIENT));
        cmdRule.cmdSteps().userSettingsSteps().postSaveSettings(expectedSettings);
        expectedSettings.setAutoVideo(expValue);
        check();
    }

    private void check() {
        SettingsModel actualSettings = cmdRule.cmdSteps().userSettingsSteps().getUserSettings(CLIENT);
        assertThat("параметр auto_video не сохранился", actualSettings,
                beanDiffer(prepareExpectedSettings(expectedSettings)).useCompareStrategy(onlyExpectedFields()));
    }

    private SettingsModel prepareExpectedSettings(SettingsModel expectedSettings) {
        return expectedSettings.withNews(null).withWarn(null).withUlogin(null);
    }
}
