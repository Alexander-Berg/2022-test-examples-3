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
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;


@Aqua.Test
@Title("Добавление новых юзеров и переключение между ними")
@Features(FeaturesConst.MULTI_AUTH)
@Tag(FeaturesConst.MULTI_AUTH)
@Stories(FeaturesConst.GENERAL)
public class MultiAuthLoginSeveralUsersWithPddTest extends BaseTest {

    public static final String CREDS = "MultiAuthLoginSeveralUsersWithPddTest";
    private static final String CREDS2 = "MultiAuthLoginSeveralUsersWithPddTest2";
    private static final String CREDS_PDD = "MultiAuthLoginSeveralUsersWithPddTest3";

    private AllureStepStorage user = new AllureStepStorage(webDriverRule);

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public AccLockRule lock = AccLockRule.use().ignoreLock().annotation();

    @Before
    public void login() {
        user.loginSteps().forAcc(lock.acc(CREDS)).logins()
            .enableMultiAuthPromo();
        user.defaultSteps().refreshPage();
    }

    @Test
    @Title("Логин несколькими юзерами, в том числе ПДД")
    @Description("Логинимся. Включаем промо мультиавторизации. Кликаем по промо, добавляем юзера" +
            "потом добавляем пдд-юзера. Кликаем по юзер-меню, выбираем первый в списке аккаунт." +
            "Проверяем, что выбранный акаунт прописан в шапке.")
    @UseCreds({CREDS, CREDS2, CREDS_PDD})
    @TestCaseId("1662")
    public void multiLogin() {
        user.defaultSteps().clicksOn(onHomePage().mail360HeaderBlock().userMenu())
            .clicksOn(onHomePage().userMenuDropdown().addUserButton());
        user.loginSteps().multiLoginWith(lock.acc(CREDS2), lock.acc(CREDS_PDD));
        user.defaultSteps().clicksOn(onHomePage().mail360HeaderBlock().userMenu())
            .clicksOnElementWithText(onHomePage().userMenuDropdown().userList(), lock.acc(CREDS).getSelfEmail())
            .shouldContainText(onHomePage().mail360HeaderBlock().userMenu(), lock.acc(CREDS).getLogin());
    }

    @Test
    @Title("Логин несколькими юзерами, в том числе ПДД в трёх разных табах")
    @Description("Логинимся юзером1, открываем новый таб и логинимся юзером2, открываем ещё один таб " +
            "и логинимся юзеромпдд. переходим в первый таб - там болжен остаться юзер1, во втором - юзер2")
    @UseCreds({CREDS, CREDS2, CREDS_PDD})
    @TestCaseId("1664")
    public void multiLoginInDifferentTabs() {
        user.defaultSteps().opensNewWindowAndSwitchesOnIt();
        user.defaultSteps().clicksOn(onHomePage().mail360HeaderBlock().userMenu())
            .clicksOn(onHomePage().userMenuDropdown().addUserButton());
        user.loginSteps().multiLoginWith(lock.acc(CREDS2));
        user.defaultSteps().opensNewWindowAndSwitchesOnIt();
        user.defaultSteps().clicksOn(onHomePage().mail360HeaderBlock().userMenu())
            .shouldSee(onHomePage().userMenuDropdown())
            .clicksOn(onHomePage().userMenuDropdown().addUserButton());
        user.loginSteps().multiLoginWith(lock.acc(CREDS_PDD));
        user.defaultSteps()
            .shouldContainText(onHomePage().mail360HeaderBlock().userMenu(), lock.acc(CREDS_PDD).getSelfEmail())
            .switchOnWindow(0)
            .shouldContainText(onHomePage().mail360HeaderBlock().userMenu(), lock.acc(CREDS).getLogin())
            .switchOnWindow(1)
            .shouldContainText(onHomePage().mail360HeaderBlock().userMenu(), lock.acc(CREDS2).getLogin());
    }
}
