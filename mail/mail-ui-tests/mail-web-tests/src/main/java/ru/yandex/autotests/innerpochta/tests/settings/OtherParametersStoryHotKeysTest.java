package ru.yandex.autotests.innerpochta.tests.settings;

import io.qameta.allure.junit4.Tag;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.data.QuickFragments;
import ru.yandex.autotests.innerpochta.rules.RestAssuredAuthRule;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.steps.AllureStepStorage;
import ru.yandex.autotests.innerpochta.tests.BaseTest;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Step;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import static com.google.common.collect.ImmutableMap.of;
import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.LAYOUT_2PANE;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.LAYOUT_3PANE_VERTICAL;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_PARAM_LAYOUT;

/**
 * <p> Created by IntelliJ IDEA.
 * <p> User: lanwen
 * <p> Date: 22.05.12
 * <p> Time: 18:48
 */

@Aqua.Test
@Title("Включение/Выключение горячих клавиш")
@Features(FeaturesConst.SETTINGS)
@Tag(FeaturesConst.SETTINGS)
@Stories(FeaturesConst.OTHER_SETTINGS)
public class OtherParametersStoryHotKeysTest extends BaseTest {

    private AccLockRule lock = AccLockRule.use().useTusAccount();
    private RestAssuredAuthRule auth = RestAssuredAuthRule.auth(lock);
    private AllureStepStorage user = new AllureStepStorage(webDriverRule, auth);

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain rules = RuleChain.outerRule(lock).around(auth).around(clearAcc(() -> user));

    @Before
    public void logIn() throws InterruptedException {
        user.loginSteps().forAcc(lock.firstAcc()).logins(QuickFragments.SETTINGS_OTHER);
    }

    @Test
    @Title("Включаем/Выключаем «Горячие клавиши» 2pane")
    @TestCaseId("1790")
    public void shouldEnableHotKeys() {
        user.apiSettingsSteps().callWith(of(SETTINGS_PARAM_LAYOUT, LAYOUT_2PANE));
        turnHotkeysOnAndOff();
    }

    @Test
    @Title("Включаем/Выключаем «Горячие клавиши» 3pane")
    @TestCaseId("1793")
    public void shouldEnableHotKeys3Pane() {
        user.apiSettingsSteps().callWith(of(SETTINGS_PARAM_LAYOUT, LAYOUT_3PANE_VERTICAL));
        turnHotkeysOnAndOff();
    }

    @Step("Включаем и выключаем горчие клавиши, проверяем, что применилось")
    private void turnHotkeysOnAndOff() {
        user.defaultSteps().turnTrue(onOtherSettings().blockSetupOther().topPanel().useHotKeys());
        saveAndGoToInbox();
        user.defaultSteps().shouldSee(onHomePage().hotKeysHelp());
        user.defaultSteps().opensFragment(QuickFragments.SETTINGS_OTHER)
            .deselects(onOtherSettings().blockSetupOther().topPanel().useHotKeys());
        saveAndGoToInbox();
        user.defaultSteps().shouldNotSee(onHomePage().hotKeysHelp());
    }

    private void saveAndGoToInbox() {
        user.defaultSteps().clicksIfCanOn(onOtherSettings().blockSetupOther().topPanel().saveButton())
            .shouldNotSee(onOtherSettings().blockSetupOther().topPanel().saveButton())
            .opensFragment(QuickFragments.INBOX);
        user.hotkeySteps().pressSimpleHotKey("?");
    }
}
