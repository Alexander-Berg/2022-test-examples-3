package ru.yandex.autotests.innerpochta.tests.autotests.event;

import io.qameta.allure.junit4.Tag;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.openqa.selenium.Keys;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.rules.CalendarRulesManager;
import ru.yandex.autotests.innerpochta.rules.InitStepsRule;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.ZombieProxyEnableRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.steps.beans.modelsdoupdateusersettings.Params;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import static ru.yandex.autotests.innerpochta.cal.rules.RemoveAllOldAndCreateNewLayer.removeAllOldAndCreateNewLayer;
import static ru.yandex.autotests.innerpochta.rules.CalendarRulesManager.calendarRulesManager;
import static ru.yandex.autotests.innerpochta.util.KeysOwn.key;

/**
 * @author marchart
 */
@Aqua.Test
@Title("Создание события на отдельной странице для WS")
@Features(FeaturesConst.CAL)
@Tag(FeaturesConst.CAL)
@Stories(FeaturesConst.NEW_EVENT_PAGE)
public class WSCreateEventOnPageTest {

    private static final String SHARED_CONTACT = "2-robb";

    private CalendarRulesManager rules = calendarRulesManager();
    private InitStepsRule steps = rules.getSteps();
    private AccLockRule lock = rules.getLock();

    @ClassRule
    public static ZombieProxyEnableRule zomb = new ZombieProxyEnableRule();

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain chain = rules.createCalendarRuleChain()
        .around(removeAllOldAndCreateNewLayer(() -> steps.user()));

    @Before
    public void setUp() {
        steps.user().apiCalSettingsSteps().updateUserSettings(
            "Включаем показ выходных в сетке",
            new Params().withShowWeekends(true)
        );
        steps.user().loginSteps().forAcc(lock.firstAcc()).logins();
    }

    @Test
    @Title("Саджест общих контактов для пользователей коннекта")
    @TestCaseId("746")
    public void shouldSeeSharedWSContacts() {
        steps.user().defaultSteps()
            .clicksOn(
                steps.pages().cal().home().leftPanel().createEvent(),
                steps.pages().cal().home().newEventPage().membersField()
            )
            .inputsTextInElement(steps.pages().cal().home().newEventPage().membersInput(), SHARED_CONTACT)
            .shouldSee(steps.pages().cal().home().suggest())
            .onMouseHoverAndClick(steps.pages().cal().home().suggestItem().get(0))
            .shouldContainText(steps.pages().cal().home().newEventPage().membersList().get(0), SHARED_CONTACT);
    }

    @Test
    @Title("Выбор саджеста в поле «Участники» по клавише «Enter»")
    @TestCaseId("725")
    public void shouldEnterMemberSuggestByEnter() {
        steps.user().defaultSteps().clicksOn(steps.pages().cal().home().leftPanel().createEvent())
            .inputsTextInElement(steps.pages().cal().home().newEventPage().membersInput(), SHARED_CONTACT)
            .shouldSee(steps.pages().cal().home().suggest());
        steps.user().hotkeySteps()
            .pressSimpleHotKey(steps.pages().cal().home().newEventPage().membersInput(), key(Keys.ENTER));
        steps.user().defaultSteps()
            .shouldContainText(steps.pages().cal().home().newEventPage().membersField(), SHARED_CONTACT);
    }

}
