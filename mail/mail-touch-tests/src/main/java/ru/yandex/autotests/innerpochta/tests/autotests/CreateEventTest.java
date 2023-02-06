package ru.yandex.autotests.innerpochta.tests.autotests;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import ru.yandex.aqua.annotations.project.Aqua;
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

import static org.hamcrest.Matchers.containsString;
import static ru.yandex.autotests.innerpochta.rules.TouchRulesManager.touchRulesManager;
import static ru.yandex.autotests.innerpochta.touch.data.ToolbarBtns.CREATEVENT;
import static ru.yandex.autotests.innerpochta.util.MailConst.DEV_NULL_EMAIL;
import static ru.yandex.autotests.innerpochta.util.MailConst.DEV_NULL_EMAIL_2;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomString;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_PARAM_DEFAULT_EMAIL;

/**
 * @author oleshko
 */
@Aqua.Test
@Title("Тесты на создание события из письма")
@Features({FeaturesConst.CAL})
@Stories(FeaturesConst.GENERAL)
public class CreateEventTest {

    private static final String CALENDAR_URL_PART = "calendar.yandex.ru/event?invited=%s%s%s&name=%s";
    private static final String CALENDAR_URL_PART_FEW_ADDRESSES = "calendar.yandex.ru/event?invited=%s%s%s%s%s&name=%s";

    private String subjOneAddr, subjFewAddr;

    private TouchRulesManager rules = touchRulesManager().withLock(AccLockRule.use().useTusAccount());
    private AccLockRule acc = rules.getLock();
    private AccLockRule lock2 = AccLockRule.use().useTusAccount();
    private RestAssuredAuthRule auth2 = RestAssuredAuthRule.auth(lock2);
    private InitStepsRule steps = rules.getSteps();

    @ClassRule
    public static ZombieProxyEnableRule zomb = new ZombieProxyEnableRule();

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain chain = rules.createTouchRuleChain()
        .around(lock2)
        .around(auth2);

    @Before
    public void prepare() {
        subjOneAddr = getRandomString();
        subjFewAddr = getRandomString();
        steps.user().apiMessagesSteps().addCcEmails(DEV_NULL_EMAIL).addBccEmails(DEV_NULL_EMAIL_2)
            .sendMailWithCcAndBcc(acc.firstAcc().getSelfEmail(), subjFewAddr, getRandomString());
        steps.user().apiMessagesSteps().withAuth(auth2)
            .sendMailWithNoSaveWithoutCheck(acc.firstAcc().getSelfEmail(), subjOneAddr, getRandomString());
        steps.user().loginSteps().forAcc(acc.firstAcc()).logins();
    }

    @Test
    @Title("Должны перейти на страницу создания события в календаре из попапа копирования ябблов")
    @TestCaseId("1319")
    public void shouldClickCreateEventBtn() {
        openPopupCopyYabbleAndClickCreateEvent();
        checkCalendarUrlForMailWithOneAddress(
            steps.user().apiSettingsSteps().withAuth(auth2).getUserSettings(SETTINGS_PARAM_DEFAULT_EMAIL)
        );
    }

    @Test
    @Title("Должны перейти на страницу создания для письма с несколькими адресатом")
    @TestCaseId("1321")
    public void shouldCreateEventForMailWithFewAddresses() {
        clickCreateEventBtn(1);
        checkCalendarUrlForMailWithFewAddresses();
    }

    @Test
    @Title("Должны перейти на страницу создания для треда с несколькими адресатами")
    @TestCaseId("1334")
    public void shouldCreateEventForThreadWithFewAddresses() {
        steps.user().apiMessagesSteps().addCcEmails(DEV_NULL_EMAIL).addBccEmails(DEV_NULL_EMAIL_2)
            .sendMessageToThreadWithCcAndBcc(acc.firstAcc().getSelfEmail(), subjFewAddr, "");
        steps.user().defaultSteps().refreshPage();
        clickCreateEventBtn(0);
        checkCalendarUrlForMailWithFewAddresses();
    }

    @Test
    @Title("Должны перейти на страницу создания для письма с одним адресатом")
    @TestCaseId("1322")
    public void shouldCreateEventForMailWithOneAddress() {
        clickCreateEventBtn(0);
        checkCalendarUrlForMailWithOneAddress(
            steps.user().apiSettingsSteps().withAuth(auth2).getUserSettings(SETTINGS_PARAM_DEFAULT_EMAIL)
        );
    }

    @Test
    @Title("Должны перейти на страницу создания для треда с одним адресатом")
    @TestCaseId("1322")
    public void shouldCreateEventForThreadWithOneAddress() {
        steps.user().apiMessagesSteps().sendMessageToThreadWithSubjectWithNoSave(subjOneAddr, acc.firstAcc(), "");
        steps.user().defaultSteps().refreshPage();
        clickCreateEventBtn(0);
        checkCalendarUrlForMailWithOneAddress(acc.firstAcc().getSelfEmail());
    }

    @Step("Нажимаем кнопку «Создать событие» в свайп-меню")
    private void clickCreateEventBtn(int msgNum) {
        steps.user().touchSteps().openActionsForMessages(msgNum);
        steps.user().defaultSteps()
            .clicksOnElementWithText(steps.pages().touch().messageView().btnsList(), CREATEVENT.btn())
            .switchOnJustOpenedWindow();
    }

    @Step("Проверяем урл календаря для письма с одним адресатом")
    private void checkCalendarUrlForMailWithOneAddress(String firstAddr) {
        steps.user().defaultSteps().shouldBeOnUrl(containsString(
            String.format(
                CALENDAR_URL_PART,
                firstAddr.replace("@", "%40"),
                "%2C",
                acc.firstAcc().getSelfEmail().replace("@", "%40"),
                subjOneAddr
            )
            )
        );
    }

    @Step("Проверяем урл календаря для письма с несколькими адресатами")
    private void checkCalendarUrlForMailWithFewAddresses() {
        steps.user().defaultSteps().shouldBeOnUrl(containsString(
            String.format(
                CALENDAR_URL_PART_FEW_ADDRESSES,
                acc.firstAcc().getSelfEmail().replace("@", "%40"),
                "%2C",
                acc.firstAcc().getSelfEmail().replace("@", "%40"),
                "%2C",
                DEV_NULL_EMAIL.replace("@", "%40"),
                subjFewAddr
            )
            )
        );
    }

    @Step("Открываем попап копирования ябблов и нажимаем «Создать событие»")
    private void openPopupCopyYabbleAndClickCreateEvent() {
        steps.user().defaultSteps().clicksOn(steps.pages().touch().messageList().messageBlock().subject())
            .clicksOn(steps.pages().touch().messageView().avatarToolbar())
            .shouldSee(steps.pages().touch().messageView().msgDetails());
        steps.user().touchSteps().longTap(steps.pages().touch().messageView().yabbles().get(1));
        steps.user().defaultSteps().shouldSee(steps.pages().touch().messageView().yabblePopup())
            .clicksOn(steps.pages().touch().messageView().yabblePopup().btnCreateEvent())
            .switchOnJustOpenedWindow();
    }
}
