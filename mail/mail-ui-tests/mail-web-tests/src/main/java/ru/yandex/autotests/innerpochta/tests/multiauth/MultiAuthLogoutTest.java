package ru.yandex.autotests.innerpochta.tests.multiauth;

import io.qameta.allure.junit4.Tag;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.rules.acclock.UseCreds;
import ru.yandex.autotests.innerpochta.steps.AllureStepStorage;
import ru.yandex.autotests.innerpochta.tests.BaseTest;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;


@Aqua.Test
@Title("Проверка работы кнопок выход/выход со всех устройств")
@Features(FeaturesConst.MULTI_AUTH)
@Tag(FeaturesConst.MULTI_AUTH)
@Stories(FeaturesConst.GENERAL)
public class MultiAuthLogoutTest extends BaseTest {

    private static final String CREDS3 = "MultiAuthRedirectTest";
    private static final String CREDS4 = "MultiAuthRedirectTest2";
    private static final String CREDS = "MultiAuthMaxUsersTest3";
    private static final String CREDS2 = "MultiAuthMaxUsersTest4";

    private AllureStepStorage user = new AllureStepStorage(webDriverRule);

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public AccLockRule lock = AccLockRule.use().ignoreLock().annotation();

    @Before
    public void login() {
        user.loginSteps().forAcc(lock.acc(CREDS)).logins()
            .multiLoginWith(lock.acc(CREDS2), lock.acc(CREDS3), lock.acc(CREDS4));
    }

    @Test
    @UseCreds({CREDS, CREDS2, CREDS3, CREDS4})
    @Title("Проверка работы кнопки выход")
    @TestCaseId("4273")
    public void multiLoginExit() {
        user.defaultSteps()
            .clicksOn(onHomePage().mail360HeaderBlock().userMenu())
            .shouldSee(onHomePage().userMenuDropdown())
            .clicksOn(onHomePage().userMenuDropdown().userLogOutLink())
            .shouldNotSee(onHomePage().exitAllPopup())
            .shouldContainText(onHomePage().mail360HeaderBlock().userMenu(), lock.acc(CREDS3).getLogin());
    }
}
