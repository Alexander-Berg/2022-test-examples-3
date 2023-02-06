package ru.yandex.autotests.innerpochta.tests.autotests.multiauth;

import io.qameta.allure.junit4.Tag;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.objstruct.base.misc.Account;
import ru.yandex.autotests.innerpochta.rules.CalendarRulesManager;
import ru.yandex.autotests.innerpochta.rules.InitStepsRule;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.ZombieProxyEnableRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.Arrays;

import static ru.yandex.autotests.innerpochta.rules.CalendarRulesManager.calendarRulesManager;
import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.passport.api.core.utilitydata.TermsAndConstants.MAX_NUMBER_OF_USERS_IN_SESSION_ID;

/**
 * @author mariya-murm
 */
@Aqua.Test
@Title("Проверка на максимальное количество мультиавторизованных юзеров")
@Features(FeaturesConst.CAL)
@Tag(FeaturesConst.CAL)
@Stories(FeaturesConst.MULTI_AUTH)
public class MultiAuthMaxUsersTest {

    private CalendarRulesManager rules = calendarRulesManager();
    private InitStepsRule steps = rules.getSteps();
    private AccLockRule lock = rules.getLock().useTusAccount(15);

    @ClassRule
    public static ZombieProxyEnableRule zomb = new ZombieProxyEnableRule();

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain chain = rules.createCalendarRuleChain()
        .around(clearAcc(() -> steps.user()));

    @Before
    public void login() {
        steps.user().loginSteps().forAcc(lock.firstAcc()).logins();
    }

    @Test
    @Title("Есть кнопка «Добавить пользователя», если юзеров 15")
    @TestCaseId("33")
    public void shouldSeeAddUserButton() {
        Account[] accountsToMultiLogin = Arrays.copyOfRange(
                lock.allAccs().toArray(new Account[0]),
                0 ,
                MAX_NUMBER_OF_USERS_IN_SESSION_ID
        );
        steps.user().loginSteps().multiLoginWith(accountsToMultiLogin);
        steps.user().defaultSteps().clicksOn(steps.pages().cal().home().calHeaderBlock().userAvatar())
            .shouldSee(
                steps.pages().cal().home().userMenu(),
                steps.pages().cal().home().adduser()
            );
    }
}
