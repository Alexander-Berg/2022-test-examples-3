package ru.yandex.autotests.innerpochta.tests.autotests.messageslist;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.rules.InitStepsRule;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.TouchRulesManager;
import ru.yandex.autotests.innerpochta.rules.ZombieProxyEnableRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.autotests.webcommon.util.Utils;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Step;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.core.IsNot.not;
import static ru.yandex.autotests.innerpochta.data.QuickFragments.FOLDER_ID;
import static ru.yandex.autotests.innerpochta.data.QuickFragments.LABEL_ID;
import static ru.yandex.autotests.innerpochta.rules.TouchRulesManager.touchRulesManager;
import static ru.yandex.autotests.innerpochta.touch.data.FidsAndLids.PIN_LABEL;
import static ru.yandex.autotests.innerpochta.touch.data.FidsAndLids.SENT_FOLDER;
import static ru.yandex.autotests.innerpochta.touch.data.ToolbarBtns.INFOLDER;
import static ru.yandex.autotests.innerpochta.util.MailConst.SENT_RU;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.INBOX;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.SENT;

/**
 * @author oleshko
 */

@Aqua.Test
@Title("Тесты на группировку писем")
@Features(FeaturesConst.MESSAGE_LIST)
@Stories(FeaturesConst.GENERAL)
public class DateGroupTest {

    private static final String TODAY = "Сегодня";
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private String today;

    private TouchRulesManager rules = touchRulesManager().withLock(AccLockRule.use().useTusAccount());
    private AccLockRule accLock = rules.getLock();
    private InitStepsRule steps = rules.getSteps();

    @ClassRule
    public static ZombieProxyEnableRule zomb = new ZombieProxyEnableRule();

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain chain = rules.createTouchRuleChain();

    @Before
    public void login() {
        steps.user().imapSteps().addMessage(6, 2019, 3)
            .addMessage(6, 2019, 3);
        steps.user().apiMessagesSteps().moveMessagesFromFolderToFolder(
            SENT,
            steps.user().apiMessagesSteps().getAllMessages().get(1)
        );
        today = LocalDateTime.now().format(DATE_FORMAT);
        steps.user().apiMessagesSteps()
            .sendMail(accLock.firstAcc().getSelfEmail(), Utils.getRandomString(), "");
        steps.user().loginSteps().forAcc(accLock.firstAcc()).logins();
    }

    @Test
    @Title("Разделитель в списке писем исчезает после удаления последнего письма группы")
    @TestCaseId("1090")
    public void shouldNotSeeEmptyGroupDate() {
        steps.user().defaultSteps().opensDefaultUrlWithPostFix(FOLDER_ID.makeTouchUrlPart(SENT_FOLDER))
            .shouldSeeElementInList(steps.pages().touch().messageList().dateGroup().waitUntil(not(empty())), TODAY);
        steps.user().touchSteps()
            .rightSwipe(steps.pages().touch().messageList().messages().waitUntil(not(empty())).get(0));
        steps.user().defaultSteps().clicksOn(steps.pages().touch().messageList().messageBlock().swipeDelBtn())
            .waitInSeconds(2)
            .shouldNotSeeElementInList(steps.pages().touch().messageList().dateGroup().waitUntil(not(empty())), TODAY);
    }

    @Test
    @Title("В закрепленных нет группировки по дате")
    @TestCaseId("1100")
    public void shouldNotSeeGroupDateInPin() {
        steps.user().apiLabelsSteps().pinLetter(steps.user().apiMessagesSteps().getAllMessages().get(0));
        steps.user().defaultSteps().opensDefaultUrlWithPostFix(LABEL_ID.makeTouchUrlPart(PIN_LABEL))
            .shouldSeeElementsCount(steps.pages().touch().messageList().dateGroup(), 0);
    }

    @Test
    @Title("Разделитель появляется при образовании новой группы")
    @TestCaseId("1089")
    public void shouldSeeNewGroupDate() {
        deleteTodayMsgFromFolder(INBOX);
        steps.user().defaultSteps().refreshPage()
            .shouldNotSeeElementInList(steps.pages().touch().messageList().dateGroup().waitUntil(not(empty())), TODAY);
        steps.user().apiMessagesSteps().sendMailWithNoSave(accLock.firstAcc(), today, "");
        steps.user().defaultSteps().shouldSeeThatElementHasText(
            steps.pages().touch().messageList().messageBlock().subject(),
            today
        );
        steps.user().defaultSteps().shouldSeeElementInList(steps.pages().touch().messageList().dateGroup(), TODAY);
    }

    @Test
    @Title("Разделитель появляется при переносе письма")
    @TestCaseId("1093")
    public void shouldSeeNewGroupDateAfterMove() {
        deleteTodayMsgFromFolder(SENT);
        steps.user().defaultSteps().opensDefaultUrlWithPostFix(FOLDER_ID.makeTouchUrlPart(SENT_FOLDER))
            .shouldNotSeeElementInList(steps.pages().touch().messageList().dateGroup().waitUntil(not(empty())), TODAY)
            .clicksOn(steps.pages().touch().messageList().headerBlock().sidebar())
            .clicksOn(steps.pages().touch().sidebar().inboxFolder())
            .shouldSeeElementInList(steps.pages().touch().messageList().dateGroup().waitUntil(not(empty())), TODAY);
        steps.user().touchSteps().openActionsForMessages(0);
        steps.user().defaultSteps()
            .clicksOnElementWithText(steps.pages().touch().messageView().btnsList(), INFOLDER.btn())
            .clicksOnElementWithText(steps.pages().touch().messageList().folderPopup().folders(), SENT_RU)
            .waitInSeconds(2)
            .shouldNotSeeElementInList(steps.pages().touch().messageList().dateGroup(), TODAY)
            .clicksOn(steps.pages().touch().messageList().headerBlock().sidebar())
            .clicksOn(steps.pages().touch().sidebar().sentFolder())
            .shouldSeeElementInList(steps.pages().touch().messageList().dateGroup(), TODAY);

    }

    @Step("Удаляем все письма за сегодня и папки")
    private void deleteTodayMsgFromFolder(String folder) {
        steps.user().apiMessagesSteps().getAllMessagesByDate(today, folder)
            .forEach(msg -> steps.user().apiMessagesSteps().deleteMessages(msg));
    }
}
