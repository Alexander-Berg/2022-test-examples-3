package ru.yandex.autotests.innerpochta.tests.main;

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
import ru.yandex.autotests.innerpochta.tests.BaseTest;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import static com.google.common.collect.ImmutableMap.of;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.EMPTY_STR;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.LIZA_MINIFIED_HEADER;

/**
 * @author mabelpines
 */
@Aqua.Test
@Title("Тесты на шапку")
@Features(FeaturesConst.MAIN)
@Tag(FeaturesConst.MAIN)
@Stories(FeaturesConst.HEAD)
@RunWith(DataProviderRunner.class)
public class MailWSHeaderTest extends BaseTest {

    private static final String WS_ACCOUNT = "CustomLogoHeader";

    private AccLockRule lock = AccLockRule.use().names(WS_ACCOUNT);
    private RestAssuredAuthRule auth = RestAssuredAuthRule.auth(lock);
    private AllureStepStorage user = new AllureStepStorage(webDriverRule, auth);

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain rules = RuleChain.outerRule(lock)
        .around(auth);

    @Before
    public void login() {
        user.loginSteps().forAcc(lock.acc(WS_ACCOUNT)).logins();
    }

    @Test
    @Title("Кастомное лого пропадает при включении компактного меню")
    @TestCaseId("4187")
    public void shouldNotSeeCustomLogo() {
        user.apiSettingsSteps().callWithListAndParams(
            "Выключаем компактную шапку",
            of(LIZA_MINIFIED_HEADER, EMPTY_STR)
        );
        user.loginSteps().forAcc(lock.acc(WS_ACCOUNT)).logins();
        user.defaultSteps().shouldSee(onMessagePage().mail360HeaderBlock().wsLogo())
            .opensFragment(QuickFragments.SETTINGS_OTHER)
            .deselects(onOtherSettings().blockSetupOther().topPanel().show360Header());
        user.settingsSteps().saveOtherSettingsSetup();
        user.defaultSteps().opensDefaultUrl()
            .shouldNotSee(onMessagePage().mail360HeaderBlock().wsLogo());
    }
}
