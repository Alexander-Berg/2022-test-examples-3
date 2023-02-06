package ru.yandex.autotests.innerpochta.tests.multiauth;

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
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;

@Aqua.Test
@Title("Проверка на максимальное количество мультиавторизованных юзеров")
@Features(FeaturesConst.MULTI_AUTH)
@Tag(FeaturesConst.MULTI_AUTH)
@Stories(FeaturesConst.GENERAL)
public class MultiAuthMaxUsersTest extends BaseTest {

    private AccLockRule lock = AccLockRule.use().useTusAccount(15);
    private RestAssuredAuthRule auth = RestAssuredAuthRule.auth(lock);
    private AllureStepStorage user = new AllureStepStorage(webDriverRule, auth);

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain rules = RuleChain.outerRule(lock)
        .around(auth)
        .around(clearAcc(() -> user));

    @Before
    public void login() {
        user.loginSteps().forAcc(lock.accNum(1)).logins();
        user.loginSteps().multiLoginWith(lock.accNum(0), lock.accNum(2), lock.accNum(3), lock.accNum(4),
            lock.accNum(5), lock.accNum(6), lock.accNum(7), lock.accNum(8), lock.accNum(9), lock.accNum(10),
            lock.accNum(11), lock.accNum(12), lock.accNum(13), lock.accNum(14)
        );
    }

    @Test
    @Title("Добавление 15-го  мультиюзера из меню юзера")
    @TestCaseId("1667")
    public void maxUserLogin() {
        user.defaultSteps().setsWindowSize(1920, 1080)
            .clicksOn(onMessagePage().mail360HeaderBlock().userMenu())
            .shouldSee(onHomePage().userMenuDropdown())
            .shouldSee(onHomePage().userMenuDropdown().addUserButton())
            .shouldNotSee(onHomePage().userMenuDropdown().promo())
            .shouldNotSee(onHomePage().userMenuDropdown().promo().addUserButton());
    }
}
