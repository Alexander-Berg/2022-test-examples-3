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
import ru.yandex.autotests.innerpochta.rules.SetCorpUrlRule;
import ru.yandex.autotests.innerpochta.rules.ZombieProxyEnableRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.rules.acclock.UseCreds;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import static org.hamcrest.Matchers.containsString;
import static ru.yandex.autotests.innerpochta.cal.util.CalConsts.EVENT_GRID;
import static ru.yandex.autotests.innerpochta.rules.CalendarRulesManager.calendarRulesManager;
import static ru.yandex.autotests.innerpochta.util.MailConst.DEV_NULL_EMAIL;
import static ru.yandex.autotests.innerpochta.util.MailConst.MORDA_URL;
import static ru.yandex.autotests.innerpochta.util.ScriptConst.DISABLE_ALERT_SCRIPT;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomString;

/**
 * @author marchart
 */
@Aqua.Test
@Title("Редиректы при авторизации")
@Features(FeaturesConst.CAL)
@Tag(FeaturesConst.CAL)
@Stories(FeaturesConst.REDIRECTS)
@UseCreds(RedirectTestCorp.CORP_CREDS)
public class RedirectTestCorp {

    public static final String CORP_CREDS = "CorpMeetingsTest";
    private static final String CORP_PASSPORT_URL = "https://passport.yandex-team.ru/";

    private CalendarRulesManager rules = calendarRulesManager().withLock(AccLockRule.use().annotation());
    private InitStepsRule steps = rules.getSteps();
    private AccLockRule lock = rules.getLock();

    @ClassRule
    public static ZombieProxyEnableRule zomb = new ZombieProxyEnableRule();

    @ClassRule
    public static SetCorpUrlRule setCorpUrlRule = new SetCorpUrlRule();

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain chain = rules.createCalendarRuleChain();

    @Test
    @Title("При потере авторизации попадаем на паспорт и возвращается в заполненную форму при повторном логине")
    @TestCaseId("1246")
    public void shouldSeePassportAfterLogout() {
        String msgString = getRandomString();
        steps.user().defaultSteps().setsWindowSize(1920, 1080)
            .opensDefaultUrl()
            .inputsTextInElement(steps.user().pages().PassportPage().login(), lock.firstAcc().getLogin())
            .inputsTextInElement(steps.user().pages().PassportPage().psswd(), lock.firstAcc().getPassword())
            .clicksOn(steps.user().pages().PassportPage().submitCorp())
            .clicksOn(steps.pages().cal().home().leftPanel().createEvent())
            .inputsTextInElement(steps.pages().cal().home().newEventPage().membersInput(), DEV_NULL_EMAIL)
            .clicksOn(steps.pages().cal().home().suggestItem().get(0))
            .inputsTextInElement(steps.pages().cal().home().newEventPage().nameInput(), msgString)
            .clicksOn(steps.pages().cal().home().newEventPage().descriptionAddBtn())
            .inputsTextInElement(steps.pages().cal().home().newEventPage().descriptionInput(), msgString);
        steps.user().hotkeySteps().clicksOnElementHoldingCtrlKey(steps.pages().cal().home().oldCalHeaderBlock().oldCalLink());
        steps.user().defaultSteps().switchOnWindow(1)
            .executesJavaScript(DISABLE_ALERT_SCRIPT)
            .clicksOn(steps.pages().cal().home().oldCalHeaderBlock().oldUserAvatar())
            .clicksOn(steps.pages().cal().home().oldLogout())
            .shouldBeOnUrl(MORDA_URL)
            .switchOnWindow(0)
            .shouldBeOnUrl(containsString(CORP_PASSPORT_URL))
            .inputsTextInElement(steps.user().pages().PassportPage().psswd(), lock.firstAcc().getPassword())
            .clicksOn(steps.user().pages().PassportPage().submitCorp())
            .shouldBeOnUrl(containsString(EVENT_GRID))
            .shouldHasValue(steps.pages().cal().home().newEventPage().nameInput(), msgString)
            .clicksIfCanOn(steps.pages().cal().home().newEventPage().descriptionAddBtn())
            .shouldSeeThatElementHasText(steps.pages().cal().home().newEventPage().descriptionInput(), msgString)
            .shouldSeeElementInList(steps.pages().cal().home().newEventPage().membersList(), DEV_NULL_EMAIL);
    }
}
