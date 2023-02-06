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
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.util.MailConst.OLD_USER_TAG;


/**
 * <p> Created by IntelliJ IDEA.
 * <p> User: lanwen
 * <p> Date: 22.05.12
 * <p> Time: 18:48
 */

@Aqua.Test
@Title("Показывать ли рекламу")
@Features(FeaturesConst.SETTINGS)
@Tag(FeaturesConst.SETTINGS)
@Stories(FeaturesConst.OTHER_SETTINGS)
public class OtherParametersStoryShowAdTest extends BaseTest {

    private AccLockRule lock = AccLockRule.use().useTusAccount(OLD_USER_TAG);
    private RestAssuredAuthRule auth = RestAssuredAuthRule.auth(lock);
    private AllureStepStorage user = new AllureStepStorage(webDriverRule, auth);

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain rules = RuleChain.outerRule(lock)
        .around(auth)
        .around(clearAcc(() -> user));

    @Before
    public void logIn() {
        user.loginSteps().forAcc(lock.firstAcc()).logins();
        user.defaultSteps().opensFragment(QuickFragments.SETTINGS_OTHER);
    }

    @Test
    @Title("Показ рекламы включаем")
    @TestCaseId("1825")
    public void testShowAdIsOn() {
        user.defaultSteps().turnTrue(onOtherSettings().blockSetupOther().topPanel().showAdvertisement());
        user.settingsSteps().saveTopPanelChangesOnOtherSetup();
        user.defaultSteps().shouldNotSee(onOtherSettings().blockSetupOther().topPanel().saveButton())
            .opensFragment(QuickFragments.INBOX);
        user.messagesSteps().shouldSeeMessagesPresent();
        user.defaultSteps().shouldSee(onHomePage().advertiseBanner()).shouldSee(onHomePage().directAd());
    }

    @Test
    @Title("Показ рекламы отключаем")
    @TestCaseId("1826")
    public void testShowAdIsOff() {
        user.defaultSteps().deselects(onOtherSettings().blockSetupOther().topPanel().showAdvertisement());
        user.settingsSteps().saveTopPanelChangesOnOtherSetup();
        user.defaultSteps().shouldNotSee(onOtherSettings().blockSetupOther().topPanel().saveButton())
            .opensFragment(QuickFragments.INBOX);
        user.messagesSteps().shouldSeeMessagesPresent();
        user.defaultSteps().shouldNotSee(onHomePage().advertiseBanner())
            .shouldNotSee(onHomePage().directAd());
    }
}
