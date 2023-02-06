package ru.yandex.autotests.innerpochta.tests.main;

import io.qameta.allure.junit4.Tag;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.rules.RestAssuredAuthRule;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.steps.AllureStepStorage;
import ru.yandex.autotests.innerpochta.tests.BaseTest;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.autotests.innerpochta.util.MultipleWindowsHandler;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomString;

/**
 * @author mariya-murm
 */
@Aqua.Test
@Title("Тесты на нотификации в новой шапке")
@Features(FeaturesConst.MAIN)
@Tag(FeaturesConst.MAIN)
@Stories(FeaturesConst.HEAD)
public class MailNewHeaderNotificationsTest extends BaseTest {

    private static final int USER_COUNT = 2;
    private static final String NOTIFICATION_1 = "1";

    public AccLockRule lock = AccLockRule.use().ignoreLock().useTusAccount(2);
    public RestAssuredAuthRule auth = RestAssuredAuthRule.auth(lock);
    private AllureStepStorage user = new AllureStepStorage(webDriverRule, auth);

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain rules = RuleChain.outerRule(lock)
        .around(auth)
        .around(clearAcc(() -> user));

    @Before
    public void login() {
        user.loginSteps().forAcc(lock.accNum(1)).logins().multiLoginWith(lock.accNum(0));
        user.defaultSteps().shouldNotSee(onHomePage().mail360HeaderBlock().userMenuNotification());
    }

    @Test
    @Title("Информер о новых письмах при приходе письма в другой аккаунт")
    @TestCaseId("5834")
    public void userMenuNotification() {
        user.apiMessagesSteps().sendMailWithNoSaveWithoutCheck(lock.accNum(1).getSelfEmail(), getRandomString(), "");
        user.defaultSteps().refreshPage()
            .shouldSee(onMessagePage().mail360HeaderBlock().userMenuNotification())
            .clicksOn(onMessagePage().mail360HeaderBlock().userMenu())
            .shouldSee(onMessagePage().mail360HeaderBlock().userMenuDropdown())
            .shouldSeeElementsCount(onMessagePage().mail360HeaderBlock().userMenuDropdown().accs(), USER_COUNT)
            .shouldSeeThatElementTextEquals(
                onMessagePage().mail360HeaderBlock().userMenuDropdown().accs().get(1).messagesCount(),
                NOTIFICATION_1
            )
            .clicksOn(onMessagePage().mail360HeaderBlock().serviceIcons().get(0))
            .shouldNotSee(onMessagePage().mail360HeaderBlock().userMenuNotification());
    }

    @Test
    @Title("Информер о новых письмах не появляется при приходе письма в этот же аккаунт")
    @TestCaseId("5836")
    public void shouldNotSeeUserMenuNotification() {
        user.apiMessagesSteps().sendMail(lock.accNum(0).getSelfEmail(), getRandomString(), "");
        user.defaultSteps().clicksOn(onHomePage().checkMailButton())
            .shouldNotSee(onMessagePage().mail360HeaderBlock().userMenuNotification());
    }

    @Test
    @Title("Информер о новых письмах пропадает, если зашли на аккаунт")
    @TestCaseId("5835")
    public void shouldNotSeeMessagesCounter() {
        user.apiMessagesSteps().sendMailWithNoSaveWithoutCheck(lock.accNum(1).getSelfEmail(), getRandomString(), "");
        user.defaultSteps().refreshPage()
            .shouldSee(onMessagePage().mail360HeaderBlock().userMenuNotification())
            .clicksOn(onMessagePage().mail360HeaderBlock().userMenu())
            .shouldSeeThatElementTextEquals(
                onMessagePage().mail360HeaderBlock().userMenuDropdown().accs().get(1).messagesCount(),
                NOTIFICATION_1
            )
            .clicksOn(onMessagePage().mail360HeaderBlock().serviceIcons().get(0))
            .shouldNotSee(onMessagePage().mail360HeaderBlock().userMenuNotification());
        MultipleWindowsHandler windowsHandler = user.defaultSteps().opensNewWindowAndSwitchesOnIt();
        user.defaultSteps().clicksOn(onMessagePage().mail360HeaderBlock().userMenu())
            .clicksOn(onMessagePage().mail360HeaderBlock().userMenuDropdown().accs().get(1));
        user.messagesSteps().shouldSeeMessagesPresent();
        user.defaultSteps().switchesOnMainWindow(windowsHandler)
            .refreshPage()
            .clicksOn(onMessagePage().mail360HeaderBlock().userMenu())
            .shouldNotSee(onMessagePage().mail360HeaderBlock().userMenuDropdown().accs().get(1).messagesCount());
    }
}
