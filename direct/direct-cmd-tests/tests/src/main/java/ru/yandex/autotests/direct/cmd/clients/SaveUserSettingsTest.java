package ru.yandex.autotests.direct.cmd.clients;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.clients.SettingsModel;
import ru.yandex.autotests.direct.cmd.data.clients.TagsAllowedEnum;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.cmd.util.BeanLoadHelper;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.direct.httpclient.data.Logins;
import ru.yandex.autotests.directapi.model.User;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import java.util.Arrays;
import java.util.Collection;

import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;

@Aqua.Test
@Description("Проверка сохранения параметров пользователя (контроллер saveSettings)")
@Stories(TestFeatures.Client.USER_SETTINGS)
@Features(TestFeatures.CLIENT)
@Tag(CmdTag.SAVE_SETTINGS)
@Tag(ObjectTag.USER)
@Tag(TrunkTag.YES)
@RunWith(Parameterized.class)
public class SaveUserSettingsTest {

    protected static final String SUPER = Logins.SUPER;
    protected static final String CLIENT = "at-direct-perf-filters-1";

    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();

    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule();
    @Parameterized.Parameter(0)
    public String account;
    @Parameterized.Parameter(1)
    public String login;
    private SettingsModel expectedSettings;

    @Parameterized.Parameters(name = "Под логином {0}")
    public static Collection<Object[]> testData() {
        return Arrays.asList(new Object[][]{
                {SUPER, CLIENT},
                {CLIENT, CLIENT},
        });
    }

    @Before
    public void before() {
        cmdRule.cmdSteps().authSteps().authenticate(User.get(account));
        expectedSettings = BeanLoadHelper.loadCmdBean("cmd.saveSettings.request.default", SettingsModel.class);
        expectedSettings.setUlogin(login);
    }

    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("9577")
    public void checkRemoveTextAutocorrection() {
        expectedSettings.setTextAutocorrection("0");
        cmdRule.cmdSteps().userSettingsSteps().postSaveSettings(expectedSettings);
        expectedSettings.setTextAutocorrection("");
        check();
    }

    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("9578")
    public void checkAddTextAutocorrection() {
        expectedSettings.withTagsAllowed(TagsAllowedEnum.NUMERIC_ON);
        cmdRule.cmdSteps().userSettingsSteps().postSaveSettings(expectedSettings);
        expectedSettings.setTextAutocorrection("");
        expectedSettings.withTagsAllowed(TagsAllowedEnum.CHECKED);
        check();
    }

    private void check() {
        SettingsModel actualSettings = cmdRule.cmdSteps().userSettingsSteps().getUserSettings(login);
        assertThat("параметр автокоррекции сохранился", actualSettings,
                beanDiffer(prepareExpectedSettings(expectedSettings)).useCompareStrategy(onlyExpectedFields()));
    }

    private SettingsModel prepareExpectedSettings(SettingsModel expectedSettings) {
        return expectedSettings.withNews(null).withWarn(null).withUlogin(null);
    }
}
