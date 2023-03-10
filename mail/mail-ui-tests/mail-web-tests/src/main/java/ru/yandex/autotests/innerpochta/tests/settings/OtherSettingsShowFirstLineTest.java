package ru.yandex.autotests.innerpochta.tests.settings;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import io.qameta.allure.junit4.Tag;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.data.QuickFragments;
import ru.yandex.autotests.innerpochta.rules.RestAssuredAuthRule;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.steps.AllureStepStorage;
import ru.yandex.autotests.innerpochta.steps.beans.message.Message;
import ru.yandex.autotests.innerpochta.tests.BaseTest;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.autotests.innerpochta.util.Utils;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import static com.google.common.collect.ImmutableMap.of;
import static ru.yandex.autotests.innerpochta.data.QuickFragments.SETTINGS_OTHER;
import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.LAYOUT_2PANE;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.LAYOUT_3PANE_VERTICAL;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_PARAM_ENABLE_FIRSTLINE;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_PARAM_LAYOUT;


/**
 * @author cosmopanda
 */
@Aqua.Test
@Title("Показывать первую строчку письма")
@Features(FeaturesConst.SETTINGS)
@Tag(FeaturesConst.SETTINGS)
@Stories(FeaturesConst.OTHER_SETTINGS)
@RunWith(DataProviderRunner.class)
public class OtherSettingsShowFirstLineTest extends BaseTest {

    private AccLockRule lock = AccLockRule.use().useTusAccount();
    private RestAssuredAuthRule auth = RestAssuredAuthRule.auth(lock);
    private AllureStepStorage user = new AllureStepStorage(webDriverRule, auth);

    private Message message;

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain rules = RuleChain.outerRule(lock)
        .around(auth)
        .around(clearAcc(() -> user));

    @Before
    public void logIn() {
        message = user.apiMessagesSteps()
            .sendMailWithNoSave(lock.firstAcc(), Utils.getRandomString(), Utils.getRandomName());
        user.apiSettingsSteps().callWithListAndParams("Включаем фестлайн", of(SETTINGS_PARAM_ENABLE_FIRSTLINE, true));
        user.loginSteps().forAcc(lock.firstAcc()).logins();
    }

    @Test
    @Title("Должны видеть первую строчку письма")
    @DataProvider({LAYOUT_2PANE, LAYOUT_3PANE_VERTICAL})
    @TestCaseId("2727")
    public void shouldSeeCorrectFistLine(String layout) {
        user.apiSettingsSteps().callWithListAndParams(
            "Включаем " + layout,
            of(SETTINGS_PARAM_LAYOUT, layout)
        );
        user.defaultSteps().refreshPage()
            .shouldHasText(
                onMessagePage().displayedMessages().list().get(0).firstLine(),
                message.getFirstline()
            );
    }

    @Test
    @Title("Выключаем показ фестлайна")
    @DataProvider({LAYOUT_2PANE, LAYOUT_3PANE_VERTICAL})
    @TestCaseId("3258")
    public void shouldNotSeeFistLine(String layout) {
        user.apiSettingsSteps().callWithListAndParams(
            "Включаем " + layout,
            of(SETTINGS_PARAM_LAYOUT, layout)
        );
        user.defaultSteps().refreshPage()
            .opensFragment(SETTINGS_OTHER)
            .deselects(onOtherSettings().blockSetupOther().topPanel().showFirstLine())
            .clicksOn(onOtherSettings().blockSetupOther().topPanel().saveButton())
            .shouldBeDeselected(onOtherSettings().blockSetupOther().topPanel().showFirstLine())
            .opensFragment(QuickFragments.INBOX)
            .shouldNotSee(onMessagePage().displayedMessages().list().get(0).firstLine());
    }
}
