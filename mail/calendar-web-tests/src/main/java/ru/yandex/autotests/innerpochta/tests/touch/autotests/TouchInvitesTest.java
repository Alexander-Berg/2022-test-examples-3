package ru.yandex.autotests.innerpochta.tests.touch.autotests;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import io.qameta.allure.junit4.Tag;
import io.qameta.atlas.webdriver.ElementsCollection;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.rules.InitStepsRule;
import ru.yandex.autotests.innerpochta.rules.RestAssuredAuthRule;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.TouchRulesManager;
import ru.yandex.autotests.innerpochta.rules.ZombieProxyEnableRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Step;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static ru.yandex.autotests.innerpochta.cal.util.CalConsts.INVITE_BUTTON_MAYBE;
import static ru.yandex.autotests.innerpochta.cal.util.CalConsts.INVITE_BUTTON_NO;
import static ru.yandex.autotests.innerpochta.cal.util.CalConsts.INVITE_BUTTON_YES;
import static ru.yandex.autotests.innerpochta.cal.util.CalConsts.INVITE_NOTIFICATION_ACCEPTED;
import static ru.yandex.autotests.innerpochta.cal.util.CalConsts.INVITE_NOTIFICATION_REJECTED;
import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.rules.TouchRulesManager.touchRulesManager;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomName;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomString;

/**
 * @author pavponn
 */
@Aqua.Test
@Title("[Тач] приглашения на события")
@Features(FeaturesConst.CAL_TOUCH)
@Tag(FeaturesConst.CAL_TOUCH)
@Stories(FeaturesConst.VIEW_EVENT_POPUP)
@RunWith(DataProviderRunner.class)
public class TouchInvitesTest {

    private static final String EVENT_NOT_DECIDED_BACKGROUND_COLOR = "background-color: rgba(255, 255, 255, 0.8)";

    private TouchRulesManager rules = touchRulesManager();
    private InitStepsRule steps = rules.getSteps();
    private AccLockRule lock = rules.getLock().usePreloadedTusAccounts(2);
    private RestAssuredAuthRule auth2 = RestAssuredAuthRule.auth(lock.accNum(1));

    @ClassRule
    public static ZombieProxyEnableRule zomb = new ZombieProxyEnableRule();

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain chain = rules.createCalendarTouchRuleChain()
        .around(auth2)
        .around(clearAcc(() -> steps.user()));

    @DataProvider
    public static Object[][] statuses() {
        return new Object[][]{
            {INVITE_BUTTON_YES, "Пойдёт"},
            {INVITE_BUTTON_MAYBE, "Возможно, пойдёт"},
            {INVITE_BUTTON_NO, "Не пойдёт"}
        };
    }

    @Before
    public void setUp() {
        steps.user().apiCalSettingsSteps().withAuth(auth2).deleteAllAndCreateNewLayer();
        steps.user().loginSteps().forAcc(lock.firstAcc()).logins();
    }

    @Test
    @Title("Статусы приглашенных участников в форме просмотра события")
    @TestCaseId("1033")
    @UseDataProvider("statuses")
    public void shouldSeeCorrectInviteStatus(String statusButton, String statusInEvent) {
        createSimpleEvent();
        steps.user().loginSteps().forAcc(lock.accNum(1)).logins();
        steps.user().defaultSteps()
            .clicksOn(getFirstEvent())
            .shouldSeeThatElementHasText(
                steps.pages().cal().touchHome().eventPage().members().waitUntil(not(empty())).get(0),
                getDisplayedEmail(lock.accNum(0).getSelfEmail())
            )
            .clicksOnElementWithText(getEventDecisionButtons(), statusButton)
            .clicksIfCanOn(steps.pages().cal().touchHome().simpleEventDecisionPopup().confirmOrOneEventBtn());
        steps.user().loginSteps().forAcc(lock.accNum(0)).logins();
        steps.user().defaultSteps()
            .clicksOn(getFirstEvent())
            .shouldSeeThatElementHasText(
                steps.pages().cal().touchHome().eventPage().members().waitUntil(not(empty())).get(1),
                statusInEvent
            );
    }

    @Test
    @Title("Приглашение на простое событие - Пойду / Возможно")
    @TestCaseId("1185")
    public void shouldAcceptSimpleEventInvite() {
        createSimpleEvent();
        steps.user().loginSteps().forAcc(lock.accNum(1)).logins();
        steps.user().defaultSteps()
            .shouldContainsAttribute(getFirstEvent(), "style", EVENT_NOT_DECIDED_BACKGROUND_COLOR)
            .clicksOn(getFirstEvent())
            .clicksOnElementWithText(getEventDecisionButtons(), INVITE_BUTTON_YES)
            .shouldSeeThatElementTextEquals(steps.pages().cal().touchHome().successNotify(), INVITE_NOTIFICATION_ACCEPTED)
            .forceClickOn(steps.pages().cal().touchHome().notificationDismiss())
            .refreshPage()
            .clicksOn(steps.user().pages().calTouch().eventPage().cancelEdit())
            .clicksOn(steps.pages().cal().touchHome().events().waitUntil(not(empty())).get(0))
            .shouldSeeThatElementHasText(
                steps.user().pages().calTouch().eventPage().checkedEventDecision(),
                INVITE_BUTTON_YES
            )
            .clicksOnElementWithText(getEventDecisionButtons(), "Возможно")
            .shouldSee(steps.pages().cal().touchHome().successNotify());
    }

    @Test
    @Title("Приглашение на регулярное событие - Пойду")
    @TestCaseId("1186")
    public void shouldAcceptRegularEventInvite() {
        createRegularEvent();
        steps.user().loginSteps().forAcc(lock.accNum(1)).logins();
        steps.user().defaultSteps()
            .clicksOn(getFirstEvent())
            .clicksOnElementWithText(getEventDecisionButtons(), INVITE_BUTTON_YES)
            .shouldSeeThatElementTextEquals(steps.pages().cal().touchHome().successNotify(), INVITE_NOTIFICATION_ACCEPTED)
            .forceClickOn(steps.pages().cal().touchHome().notificationDismiss())
            .refreshPage()
            .clicksOn(steps.user().pages().calTouch().eventPage().cancelEdit())
            .clicksOn(steps.pages().cal().touchHome().events().waitUntil(not(empty())).get(0))
            .shouldSeeThatElementHasText(
                steps.user().pages().calTouch().eventPage().checkedEventDecision(),
                INVITE_BUTTON_YES
            );
        steps.user().calTouchGridSteps().openFutureDayGrid(1);
        steps.user().defaultSteps()
            .clicksOn(steps.pages().cal().touchHome().events().waitUntil(not(empty())).get(0))
            .shouldSeeThatElementHasText(
                steps.user().pages().calTouch().eventPage().checkedEventDecision(),
                INVITE_BUTTON_YES
            );
    }

    @Test
    @Title("Приглашение на простое событие - Не пойду")
    @TestCaseId("1188")
    public void shouldRejectSimpleEventInvite() {
        createSimpleEvent();
        steps.user().loginSteps().forAcc(lock.accNum(1)).logins();
        steps.user().defaultSteps()
            .clicksOn(getFirstEvent())
            .clicksOnElementWithText(getEventDecisionButtons(), INVITE_BUTTON_NO)
            .shouldSee(
                steps.pages().cal().touchHome().simpleEventDecisionPopup().confirmOrOneEventBtn(),
                steps.pages().cal().touchHome().simpleEventDecisionPopup().refuseOrAllEventsBtn()
            )
            .clicksOn(steps.pages().cal().touchHome().simpleEventDecisionPopup().refuseOrAllEventsBtn())
            .shouldSee(steps.pages().cal().touchHome().eventPage())
            .clicksOnElementWithText(getEventDecisionButtons(), INVITE_BUTTON_NO)
            .clicksOn(steps.pages().cal().touchHome().simpleEventDecisionPopup().confirmOrOneEventBtn());
        eventShouldBeDeletedCorrectly();
    }

    @Test
    @Title("Приглашение на регулярное событие - Не пойду")
    @TestCaseId("1190")
    public void shouldRejectRegularEventInvite() {
        String commentForOneEvent = getRandomString();
        String commentForAllEvents = getRandomString();
        createRegularEvent();
        steps.user().loginSteps().forAcc(lock.accNum(1)).logins();
        steps.user().defaultSteps()
            .clicksOn(getFirstEvent())
            .clicksOnElementWithText(getEventDecisionButtons(), INVITE_BUTTON_NO)
            .clicksOn(steps.pages().cal().touchHome().regularEventDecisionPopup().close())
            .clicksOnElementWithText(getEventDecisionButtons(), INVITE_BUTTON_NO)
            .inputsTextInElement(
                steps.pages().cal().touchHome().regularEventDecisionPopup().textCommentArea(),
                commentForOneEvent
            )
            .clicksOn(steps.pages().cal().touchHome().regularEventDecisionPopup().onlyCurrentEventBtn());
        eventShouldBeDeletedCorrectly();
        steps.user().calTouchGridSteps().openFutureDayGrid(1);
        steps.user().defaultSteps()
            .clicksOn(getFirstEvent())
            .clicksOnElementWithText(getEventDecisionButtons(), INVITE_BUTTON_NO)
            .inputsTextInElement(
                steps.pages().cal().touchHome().regularEventDecisionPopup().textCommentArea(),
                commentForAllEvents
            )
            .clicksOn(steps.pages().cal().touchHome().regularEventDecisionPopup().allEventsBtn());
        eventShouldBeDeletedCorrectly();
        steps.user().calTouchGridSteps().openFutureDayGrid(2);
        steps.user().defaultSteps().shouldNotSee(steps.pages().cal().touchHome().events());
    }

    @Step("Проверяем, что событие удалилось корректно в текущем дне")
    private void eventShouldBeDeletedCorrectly() {
        steps.user().defaultSteps()
            .shouldSeeThatElementTextEquals(steps.pages().cal().touchHome().successNotify(), INVITE_NOTIFICATION_REJECTED)
            .shouldSee(steps.pages().cal().touchHome().grid())
            .shouldNotSee(steps.pages().cal().touchHome().events());
    }

    @Step("Создаём регулярное событие")
    private void createRegularEvent() {
        steps.user().calTouchCreateEventSteps().createEventBuilder()
            .withTitle(getRandomName())
            .withParticipants(getDisplayedEmail(lock.accNum(1).getSelfEmail()))
            .withEveryDayRepetition()
            .submit()
            .thenCheck();
    }

    @Step("Создаём обычное событие")
    private void createSimpleEvent() {
        steps.user().calTouchCreateEventSteps().createEventBuilder()
            .withTitle(getRandomName())
            .withParticipants(getDisplayedEmail(lock.accNum(1).getSelfEmail()))
            .submit()
            .thenCheck();
    }

    @Step("Получаем кнопки принятия/отклонения приглашения на событие")
    private ElementsCollection<MailElement> getEventDecisionButtons() {
        return steps.user().pages().calTouch().eventPage().eventDecisionButtons().waitUntil(not(empty()));
    }

    @Step("Получаем первое событие из списка событий")
    private MailElement getFirstEvent() {
        return steps.pages().cal().touchHome().events().waitUntil(not(empty())).get(0);
    }

    @Step("Получаем email ТУЗ аккаунта, отображаемый на экране")
    private String getDisplayedEmail(String email) {
        StringBuffer replacedEmail = new StringBuffer(email);
        return replacedEmail.replace(email.lastIndexOf("-"), email.lastIndexOf("-") + 1, ".").toString();
    }

}
