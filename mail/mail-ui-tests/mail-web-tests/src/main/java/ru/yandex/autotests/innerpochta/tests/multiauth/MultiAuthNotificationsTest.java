package ru.yandex.autotests.innerpochta.tests.multiauth;

import io.qameta.allure.junit4.Tag;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.annotations.ConditionalIgnore;
import ru.yandex.autotests.innerpochta.conditions.TicketInProgress;
import ru.yandex.autotests.innerpochta.rules.RestAssuredAuthRule;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.rules.acclock.UseCreds;
import ru.yandex.autotests.innerpochta.steps.AllureStepStorage;
import ru.yandex.autotests.innerpochta.tests.BaseTest;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import static ru.yandex.autotests.innerpochta.util.Utils.getRandomString;

@Aqua.Test
@Title("Проверка работы ксивы")
@Description("Проверка работы ксивы в дропдауне в верхнем правом углу почты и в списке писем")
@Features({FeaturesConst.MULTI_AUTH, FeaturesConst.NOT_TUS})
@Tag(FeaturesConst.MULTI_AUTH)
@Stories(FeaturesConst.GENERAL)
public class MultiAuthNotificationsTest extends BaseTest {

    public static final String CREDS = "MultiAuthNotificationsTest1";
    public static final String CREDS2 = "MultiAuthNotificationsTest2";
    public static final String CREDS3 = "MultiAuthNotificationsTest3";
    private static final int USER_COUNT = 2;
    private static final String NOTIFICATION_1 = "1";

    public AccLockRule lock = AccLockRule.use().ignoreLock().annotation();
    public RestAssuredAuthRule auth = RestAssuredAuthRule.auth(lock);
    private AllureStepStorage user = new AllureStepStorage(webDriverRule, auth);

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain rules = RuleChain.outerRule(lock)
        .around(auth);

    @Before
    public void login() {
        user.loginSteps().forAcc(lock.acc(CREDS)).logins()
            .multiLoginWith(lock.acc(CREDS2), lock.acc(CREDS3));
        user.defaultSteps().shouldNotSee(onHomePage().mail360HeaderBlock().userMenuNotification());
        user.apiMessagesSteps().sendMailWithNoSave(lock.acc(CREDS).getSelfEmail(),
            getRandomString(), getRandomString());
        user.apiMessagesSteps().sendMailWithNoSave(lock.acc(CREDS2).getSelfEmail(),
            getRandomString(), getRandomString());
    }

    @Test
    @Title("Нотификации в меню пользователя")
    @UseCreds({CREDS, CREDS2, CREDS3})
    @TestCaseId("1668")
    @ConditionalIgnore(condition = TicketInProgress.class)
    @Issue("DARIA-61649")
    public void userMenuNotification() {
        user.defaultSteps().shouldSee(onHomePage().mail360HeaderBlock().userMenuNotification())
            .clicksOn(onHomePage().mail360HeaderBlock().userMenu())
            .shouldSee(onHomePage().userMenuDropdown())
            .shouldSeeElementsCount(onHomePage().userMenuDropdown().userList(), USER_COUNT)
            .shouldSeeThatElementTextEquals(onHomePage().userMenuDropdown().userList().get(0).notification(), NOTIFICATION_1)
            .shouldSeeThatElementTextEquals(onHomePage().userMenuDropdown().userList().get(1).notification(), NOTIFICATION_1)

            .clicksOnElementWithText(onHomePage().userMenuDropdown().userList(), lock.acc(CREDS).getSelfEmail())
            .shouldNotSee(onHomePage().mail360HeaderBlock().userMenuNotification())

            .clicksOn(onHomePage().mail360HeaderBlock().userMenu())
            .shouldSeeElementsCount(onHomePage().userMenuDropdown().userList(), USER_COUNT)
            .shouldSeeThatElementTextEquals(onHomePage().userMenuDropdown().userList().get(0).notification(), NOTIFICATION_1)
            .shouldNotSee(onHomePage().userMenuDropdown().userList().get(1).notification())

            .clicksOnElementWithText(onHomePage().userMenuDropdown().userList(), lock.acc(CREDS2).getSelfEmail())
            .shouldNotSee(onHomePage().mail360HeaderBlock().userMenuNotification())
            .clicksOn(onHomePage().mail360HeaderBlock().userMenu())
            .shouldSeeElementsCount(onHomePage().userMenuDropdown().userList(), 2)
            .shouldNotSee(onHomePage().userMenuDropdown().userList().get(0).notification())
            .shouldNotSee(onHomePage().userMenuDropdown().userList().get(1).notification());
    }
}
