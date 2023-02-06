package ru.yandex.autotests.innerpochta.tests.autotests.auth;

import io.qameta.allure.junit4.Tag;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import ru.yandex.aqua.annotations.project.Aqua;
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

import static org.hamcrest.Matchers.containsString;
import static ru.yandex.autotests.innerpochta.rules.CalendarRulesManager.calendarRulesManager;
import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.util.MailConst.PASSPORT_URL;

/**
 * @author crafty
 */
@Aqua.Test
@Title("Редиректы при авторизации")
@Features(FeaturesConst.CAL)
@Tag(FeaturesConst.CAL)
@Stories(FeaturesConst.REDIRECTS)
public class RedirectTest {

    private static final String URL = "https://calendar.yandex.ru/todo";

    private CalendarRulesManager rules = calendarRulesManager();
    private InitStepsRule steps = rules.getSteps();
    private AccLockRule lock = rules.getLock().useTusAccount();

    @ClassRule
    public static ZombieProxyEnableRule zomb = new ZombieProxyEnableRule();

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain chain = rules.createCalendarRuleChain()
        .around(clearAcc(() -> steps.user()));

    @Test
    @Title("Прокидываем полный retpath при редиректах")
    @TestCaseId("903")
    public void shouldSeeCorrectRetpath() {
        steps.user().defaultSteps().opensUrl(URL)
            .shouldBeOnUrl(containsString(PASSPORT_URL))
            .inputsTextInElement(steps.user().pages().PassportPage().login(), lock.firstAcc().getLogin())
            .clicksOn(steps.user().pages().PassportPage().submit())
            .inputsTextInElement(steps.user().pages().PassportPage().psswd(), lock.firstAcc().getPassword())
            .clicksOn(steps.user().pages().PassportPage().submit())
            .clicksIfCanOn(steps.user().pages().PassportPage().notNowBtn())
            .clicksIfCanOn(steps.user().pages().PassportPage().notNowEmailBtn())
            .clicksIfCanOn(steps.user().pages().PassportPage().skipAvatarBtn())
            .shouldBeOnUrl(containsString(URL))
            .shouldSee(steps.pages().cal().home().todo());
    }
}
