package ru.yandex.autotests.innerpochta.tests.autotests.sidebar;

import io.qameta.allure.junit4.Tag;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.openqa.selenium.Keys;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.cal.steps.api.Event;
import ru.yandex.autotests.innerpochta.rules.CalendarRulesManager;
import ru.yandex.autotests.innerpochta.rules.InitStepsRule;
import ru.yandex.autotests.innerpochta.rules.RestAssuredAuthRule;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.ZombieProxyEnableRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.steps.beans.modelsdoupdateusersettings.Params;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.autotests.innerpochta.util.props.UrlProps;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.Arrays;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.core.IsNot.not;
import static ru.yandex.autotests.innerpochta.cal.util.CalConsts.CAL_BASE_URL;
import static ru.yandex.autotests.innerpochta.cal.util.CalConsts.INVITE_NOTIFICATION_REJECTED;
import static ru.yandex.autotests.innerpochta.cal.util.CalConsts.RANDOM_X_COORD;
import static ru.yandex.autotests.innerpochta.cal.util.CalConsts.RANDOM_Y_COORD;
import static ru.yandex.autotests.innerpochta.cal.util.CalConsts.WEEK_GRID;
import static ru.yandex.autotests.innerpochta.rules.CalendarRulesManager.calendarRulesManager;
import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.util.MailConst.MAIL_BASE_URL;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomString;

/**
 * @author mariya-murm
 */
@Aqua.Test
@Title("Тесты на сайдбар слоев")
@Features(FeaturesConst.CAL)
@Tag(FeaturesConst.CAL)
@Stories(FeaturesConst.LAYER_SIDEBAR)
public class LayerSidebarTest {

    private static final String LAYER = "Мои события";
    private String name;

    private CalendarRulesManager rules = calendarRulesManager();
    private InitStepsRule steps = rules.getSteps();
    private AccLockRule lock = rules.getLock().usePreloadedTusAccounts(2);
    private RestAssuredAuthRule auth2 = RestAssuredAuthRule.auth(lock.accNum(1));

    @ClassRule
    public static ZombieProxyEnableRule zomb = new ZombieProxyEnableRule();

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain chain = rules.createCalendarRuleChain()
        .around(auth2)
        .around(clearAcc(() -> steps.user()));

    @Before
    public void setUp() {
        name = getRandomString();
        steps.user().apiCalSettingsSteps().withAuth(auth2).deleteAllAndCreateNewLayer();
        steps.user().apiCalSettingsSteps()
            .updateUserSettings("Разворачиваем календари", new Params().withIsCalendarsListExpanded(true));
        steps.user().apiCalSettingsSteps().deleteLayers();
        steps.user().loginSteps().forAcc(lock.accNum(0)).logins();
        steps.user().defaultSteps().shouldNotSeeElementInList(
            steps.pages().cal().home().leftPanel().layersList(),
            LAYER
        );
    }

    @Test
    @Title("По ховеру на слой календаря выводится полное название слоя")
    @TestCaseId("598")
    public void shouldSeeLayerNameTooltip() {
        String layerName = getRandomString() + getRandomString();
        steps.user().apiCalSettingsSteps().createNewLayer(
            steps.user().settingsCalSteps().formDefaultLayer().withName(layerName));
        steps.user().defaultSteps().opensDefaultUrlWithPostFix(WEEK_GRID)
            .shouldHasTitle(steps.pages().cal().home().leftPanel().layersNames().get(0), layerName);
    }

    @Test
    @Title("Сайдбар слоя закрывается по ESC")
    @TestCaseId("848")
    public void shouldCloseLayerSidebar() {
        steps.user().defaultSteps()
            .clicksOn(steps.pages().cal().home().leftPanel().addCal())
            .shouldSee(steps.pages().cal().home().addCalSideBar());
        steps.user().hotkeySteps().pressCalHotKeys(Keys.ESCAPE.toString());
        steps.user().defaultSteps().shouldNotSee(steps.pages().cal().home().addCalSideBar());
    }

    @Test
    @Title("Слой «Мои события» появляется после создания первого события через поп-ап")
    @TestCaseId("40")
    public void shouldSeeLayerWithEventByPopup() {
        steps.user().defaultSteps()
            .offsetClick(RANDOM_X_COORD, RANDOM_Y_COORD)
            .shouldSee(steps.pages().cal().home().newEventPopup())
            .clicksOn(steps.pages().cal().home().newEventPopup().nameInput())
            .inputsTextInElement(steps.pages().cal().home().newEventPopup().nameInput(), name)
            .clicksOn(steps.pages().cal().home().newEventPopup().createFromPopupBtn())
            .shouldNotSee(steps.pages().cal().home().newEventPopup())
            .shouldContainText(steps.pages().cal().home().eventsAllList().get(0).eventName(), name)
            .shouldSeeElementInList(steps.pages().cal().home().leftPanel().layersList(), LAYER);
    }

    @Test
    @Title("Слой «Мои события» появляется после создания первого события через кнопку «Создать событие»")
    @TestCaseId("40")
    public void shouldSeeLayerWithEventByPage() {
        steps.user().defaultSteps()
            .clicksOn(steps.pages().cal().home().leftPanel().createEvent())
            .shouldSee(steps.pages().cal().home().newEventPage())
            .inputsTextInElement(steps.pages().cal().home().newEventPage().nameInput(), name)
            .clicksOn(steps.pages().cal().home().newEventPage().createFromPageBtn())
            .shouldNotSee(steps.pages().cal().home().newEventPage())
            .shouldContainText(steps.pages().cal().home().eventsAllList().get(0).eventName(), name)
            .shouldSeeElementInList(steps.pages().cal().home().leftPanel().layersList(), LAYER);
    }

    @Test
    @Title("Слой «Мои события» появляется после добавления первого события по приглашению")
    @TestCaseId("40")
    public void shouldSeeLayerWithEventByInvite() {
        steps.user().apiCalSettingsSteps().createNewLayer(steps.user().settingsCalSteps().formDefaultLayer());
        steps.user().apiCalSettingsSteps().withAuth(auth2)
            .deleteLayers();
        Long layerID = steps.user().apiCalSettingsSteps().getUserLayers().get(0).getId();
        Event event = steps.user().settingsCalSteps().formDefaultEvent(layerID).withName(name);
        steps.user().apiCalSettingsSteps()
            .createNewEventWithAttendees(event, Arrays.asList(lock.accNum(1).getSelfEmail()));

        steps.user().loginSteps().forAcc(lock.accNum(1)).logins();
        steps.user().defaultSteps()
            .opensDefaultUrlWithPostFix(WEEK_GRID)
            .shouldContainText(steps.pages().cal().home().eventsAllList().get(0).eventName(), name)
            .shouldSeeElementInList(steps.pages().cal().home().leftPanel().layersList(), LAYER);
    }

    @Test
    @Title("Отказаться от приглашения использовать календарь")
    @TestCaseId("518")
    public void shouldNotSeeLayerAfterReject() {
        steps.user().apiCalSettingsSteps().createNewLayer(steps.user().settingsCalSteps().formDefaultLayer());
        steps.user().defaultSteps().refreshPage()
            .onMouseHoverAndClick(steps.pages().cal().home().leftPanel().calSettings())
            .clicksOn(steps.pages().cal().home().settings().tabAccess())
            .clicksOn(steps.pages().cal().home().settings().inputContact())
            .inputsTextInElement(steps.pages().cal().home().settings().inputContact(), lock.accNum(1).getSelfEmail())
            .clicksOn(steps.pages().cal().home().suggestItem().waitUntil(not(empty())).get(0))
            .clicksOn(steps.pages().cal().home().editCalSideBar().saveBtn());
        steps.user().loginSteps().forAcc(lock.accNum(1)).logins();
        steps.user().defaultSteps().opensUrl(MAIL_BASE_URL);
        steps.user().messagesSteps().clicksOnMessageByNumber(0)
            .clicksOnMessageByNumber(1);
        String rejectUrl = steps.pages().mail().msgView().messageTextBlock().messageHref().get(1).getAttribute("href");
        rejectUrl = rejectUrl.replace(CAL_BASE_URL, UrlProps.urlProps().getBaseUri());
        steps.user().defaultSteps().opensUrl(rejectUrl)
            .shouldSeeThatElementHasText(
                steps.pages().cal().home().notificationMessage(),
                INVITE_NOTIFICATION_REJECTED
            );
    }

}
