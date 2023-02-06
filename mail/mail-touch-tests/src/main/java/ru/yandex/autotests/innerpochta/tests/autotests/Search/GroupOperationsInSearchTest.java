package ru.yandex.autotests.innerpochta.tests.autotests.Search;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.annotations.DoTestOnlyForEnvironment;
import ru.yandex.autotests.innerpochta.rules.InitStepsRule;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.TouchRulesManager;
import ru.yandex.autotests.innerpochta.rules.ZombieProxyEnableRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.autotests.innerpochta.util.Utils;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Step;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import static ru.yandex.autotests.innerpochta.data.QuickFragments.SEARCH_TOUCH;
import static ru.yandex.autotests.innerpochta.rules.TouchRulesManager.touchRulesManager;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.ARCHIVE;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.INBOX;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.SPAM;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.TRASH;
import static ru.yandex.autotests.innerpochta.util.handlers.LabelsConstants.LABELS_PARAM_RED_COLOR;

/**
 * @author oleshko
 */

@Aqua.Test
@Title("Групповые операции в поиске")
@Features({FeaturesConst.SEARCH})
@Stories(FeaturesConst.MESSAGE_LIST)
public class GroupOperationsInSearchTest {

    private static final int NUM_OF_MSG = 13;
    private static final int NUM_OF_MSG_FOR_TABLET = 16;
    private static final int MESSAGE_NUMBER = 2;
    private static final int USER_FOLDER_INDEX = 0;
    private static final int USER_LABEL_INDEX = 1;

    private String folderName, labelName;

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
    public void prep() {
        folderName = Utils.getRandomName();
        labelName = Utils.getRandomName();
        steps.user().apiFoldersSteps().createNewFolder(folderName);
        steps.user().apiMessagesSteps().sendCoupleMessages(accLock.firstAcc(), MESSAGE_NUMBER);
        steps.user().apiLabelsSteps().addNewLabel(labelName, LABELS_PARAM_RED_COLOR);
        steps.user().apiLabelsSteps().markWithLabel(
            steps.user().apiMessagesSteps().getAllMessagesInFolder(INBOX).get(0),
            steps.user().apiLabelsSteps().getAllUserLabels().get(0)
        );
        steps.user().loginSteps().forAcc(accLock.firstAcc()).logins();
        steps.user().defaultSteps().opensDefaultUrlWithPostFix(SEARCH_TOUCH.makeTouchUrlPart())
            .inputsTextInElement(steps.pages().touch().search().header().input(), "subj ")
            .clicksOn(steps.pages().touch().search().header().find())
            .shouldSee(steps.pages().touch().search().messageBlock());
    }

    @Test
    @Title("Удаляем одно письмо")
    @TestCaseId("839")
    @DoTestOnlyForEnvironment("Phone")
    public void shouldDeleteMessages() {
        openGroupOperationToolbar();
        steps.user().defaultSteps()
            .clicksOn(steps.pages().touch().messageList().groupOperationsToolbarPhone().delete());
        checkMsgsCountAfterOperation(MESSAGE_NUMBER, TRASH, 1);
        openGroupOperationToolbar();
        steps.user().defaultSteps()
            .clicksOn(steps.pages().touch().messageList().groupOperationsToolbarPhone().delete());
        checkMsgsCountAfterOperation(MESSAGE_NUMBER - 1, TRASH, 0);
    }

    @Test
    @Title("Удаляем одно письмо")
    @TestCaseId("839")
    @DoTestOnlyForEnvironment("Tablet")
    public void shouldDeleteMessagesTablet() {
        openGroupOperationToolbar();
        steps.user().defaultSteps()
            .clicksOn(steps.pages().touch().messageView().groupOperationsToolbarTablet().delete());
        checkMsgsCountAfterOperation(MESSAGE_NUMBER, TRASH, 1);
        openGroupOperationToolbar();
        steps.user().defaultSteps()
            .clicksOn(steps.pages().touch().messageView().groupOperationsToolbarTablet().delete());
        checkMsgsCountAfterOperation(MESSAGE_NUMBER - 1, TRASH, 0);
    }

    @Test
    @Title("Отправляем письмо в спам")
    @TestCaseId("830")
    @DoTestOnlyForEnvironment("Phone")
    public void shouldMoveMessagesToSpam() {
        openGroupOperationToolbar();
        steps.user().defaultSteps().clicksOn(steps.pages().touch().messageList().groupOperationsToolbarPhone().spam());
        checkMsgsCountAfterOperation(MESSAGE_NUMBER - 1, SPAM, 1);
    }

    @Test
    @Title("Отправляем письмо в спам")
    @TestCaseId("830")
    @DoTestOnlyForEnvironment("Tablet")
    public void shouldMoveMessagesToSpamTablet() {
        openGroupOperationToolbar();
        steps.user().defaultSteps().clicksOn(steps.pages().touch().messageView().groupOperationsToolbarTablet().spam());
        checkMsgsCountAfterOperation(MESSAGE_NUMBER - 1, SPAM, 1);
    }

    @Test
    @Title("Переносим письмо в пользовательскую папку")
    @TestCaseId("1079")
    @DoTestOnlyForEnvironment("Phone")
    public void shouldMoveMessagesToFolder() {
        openGroupOperationToolbar();
        steps.user().defaultSteps().clicksOn(
            steps.pages().touch().messageList().groupOperationsToolbarPhone().more(),
            steps.pages().touch().messageList().groupOperationsToast().folder()
        )
            .clicksOn(steps.pages().touch().messageList().folderPopup().folders().get(USER_FOLDER_INDEX));
        checkMsgsCountAfterOperation(MESSAGE_NUMBER, folderName, 1);
    }

    @Test
    @Title("Переносим письмо в пользовательскую папку")
    @TestCaseId("1079")
    @DoTestOnlyForEnvironment("Tablet")
    public void shouldMoveMessagesToFolderTablet() {
        openGroupOperationToolbar();
        steps.user().defaultSteps()
            .clicksOn(steps.pages().touch().messageView().groupOperationsToolbarTablet().folder())
            .clicksOn(steps.pages().touch().messageList().folderPopup().folders().get(USER_FOLDER_INDEX));
        checkMsgsCountAfterOperation(MESSAGE_NUMBER, folderName, 1);
    }

    @Test
    @Title("Отправляем письмо в архив")
    @TestCaseId("1078")
    @DoTestOnlyForEnvironment("Phone")
    public void shouldArchiveMessages() {
        openGroupOperationToolbar();
        steps.user().defaultSteps().clicksOn(
            steps.pages().touch().messageList().groupOperationsToolbarPhone().more(),
            steps.pages().touch().messageList().groupOperationsToast().archive()
        );
        checkMsgsCountAfterOperation(MESSAGE_NUMBER, ARCHIVE, 1);
    }

    @Test
    @Title("Отправляем письмо в архив")
    @TestCaseId("1078")
    @DoTestOnlyForEnvironment("Tablet")
    public void shouldArchiveMessagesTablet() {
        openGroupOperationToolbar();
        steps.user().defaultSteps()
            .clicksOn(steps.pages().touch().messageView().groupOperationsToolbarTablet().archive());
        checkMsgsCountAfterOperation(MESSAGE_NUMBER, ARCHIVE, 1);
    }

    @Test
    @Title("Отмечаем письмо прочитанным")
    @TestCaseId("1102")
    @DoTestOnlyForEnvironment("Phone")
    public void shouldMarkMessagesRead() {
        steps.user().defaultSteps().shouldSee(steps.pages().touch().messageList().messages().get(1).unreadToggler());
        openGroupOperationToolbar();
        steps.user().defaultSteps()
            .clicksOn(steps.pages().touch().messageList().groupOperationsToolbarPhone().read())
            .shouldNotSee(steps.pages().touch().messageList().messages().get(1).unreadToggler())
            .shouldSeeElementsCount(steps.pages().touch().messageList().messages(), MESSAGE_NUMBER);
    }

    @Test
    @Title("Отмечаем письмо прочитанным")
    @TestCaseId("1102")
    @DoTestOnlyForEnvironment("Tablet")
    public void shouldMarkMessagesReadTablet() {
        steps.user().defaultSteps().shouldSee(steps.pages().touch().messageList().messages().get(1).unreadToggler());
        openGroupOperationToolbar();
        steps.user().defaultSteps()
            .clicksOn(steps.pages().touch().messageView().groupOperationsToolbarTablet().read())
            .shouldNotSee(steps.pages().touch().messageList().headerBlock().unreadCounter())
            .shouldSeeElementsCount(steps.pages().touch().messageList().messages(), MESSAGE_NUMBER);
    }

    @Test
    @Title("Отмечаем письмо непрочитанным")
    @TestCaseId("1103")
    @DoTestOnlyForEnvironment("Phone")
    public void shouldMarkMessagesUnread() {
        steps.user().apiMessagesSteps().markAllMsgRead();
        steps.user().defaultSteps().refreshPage();
        openGroupOperationToolbar();
        steps.user().defaultSteps()
            .shouldNotSee(steps.pages().touch().messageList().messages().get(1).unreadToggler())
            .clicksOn(steps.pages().touch().messageList().groupOperationsToolbarPhone().unread())
            .shouldSee(steps.pages().touch().messageList().messages().get(1).unreadToggler());
    }

    @Test
    @Title("Отмечаем письмо непрочитанным")
    @TestCaseId("1103")
    @DoTestOnlyForEnvironment("Tablet")
    public void shouldMarkMessagesUnreadTablet() {
        steps.user().apiMessagesSteps().markAllMsgRead();
        steps.user().defaultSteps().refreshPage();
        openGroupOperationToolbar();
        steps.user().defaultSteps()
            .shouldNotSee(steps.pages().touch().messageList().messages().get(1).unreadToggler())
            .clicksOn(steps.pages().touch().messageView().groupOperationsToolbarTablet().unread())
            .shouldSee(steps.pages().touch().messageList().messages().get(1).unreadToggler());
    }

    @Test
    @Title("Должны поставить метку на все выделенные письма")
    @TestCaseId("1104")
    @DoTestOnlyForEnvironment("Phone")
    public void shouldLabelMessages() {
        openGroupOperationsToolbarAndChangeLabel();
        steps.user().defaultSteps().clicksOn(steps.pages().touch().messageList().popup().done())
            .shouldSeeThatElementTextEquals(steps.pages().touch().messageList().messages().get(0).label(), labelName)
            .shouldSeeThatElementTextEquals(steps.pages().touch().messageList().messages().get(1).label(), labelName);
        checkLabeledMsgCount(2);
    }

    @Test
    @Title("Должны поставить метку на все выделенные письма")
    @TestCaseId("1104")
    @DoTestOnlyForEnvironment("Tablet")
    public void shouldLabelMessagesTablet() {
        openGroupOperationsToolbarAndChangeLabelTablet();
        steps.user().defaultSteps().clicksOn(steps.pages().touch().messageList().popup().done())
            .shouldSeeThatElementTextEquals(steps.pages().touch().messageList().messages().get(0).label(), labelName)
            .shouldSeeThatElementTextEquals(steps.pages().touch().messageList().messages().get(1).label(), labelName);
        checkLabeledMsgCount(2);
    }

    @Test
    @Title("Должны снять метку со всех выделенных писем")
    @TestCaseId("1105")
    @DoTestOnlyForEnvironment("Phone")
    public void shouldUnLabelMessages() {
        openGroupOperationsToolbarAndChangeLabel();
        steps.user().defaultSteps().clicksOn(steps.pages().touch().messageList().popup().labels().get(USER_LABEL_INDEX))
            .shouldNotSee(steps.pages().touch().messageList().popup().tick())
            .clicksOn(steps.pages().touch().messageList().popup().done())
            .shouldNotSee(steps.pages().touch().messageList().messageBlock().label());
        checkLabeledMsgCount(0);
    }

    @Test
    @Title("Должны снять метку со всех выделенных писем")
    @TestCaseId("1105")
    @DoTestOnlyForEnvironment("Tablet")
    public void shouldUnLabelMessagesTablet() {
        openGroupOperationsToolbarAndChangeLabelTablet();
        steps.user().defaultSteps().clicksOn(steps.pages().touch().messageList().popup().labels().get(USER_LABEL_INDEX))
            .shouldNotSee(steps.pages().touch().messageList().popup().tick())
            .clicksOn(steps.pages().touch().messageList().popup().done())
            .shouldNotSee(steps.pages().touch().messageList().messageBlock().label());
        checkLabeledMsgCount(0);
    }

    @Test
    @Title("После нажатия на Удалить в тулбаре ГО попап с выбором папки исчезает")
    @TestCaseId("1106")
    @DoTestOnlyForEnvironment("Tablet")
    public void shouldNotSeeFolderPopupForTablet() {
        openGroupOperationToolbar();
        steps.user().defaultSteps()
            .clicksOn(steps.pages().touch().messageView().groupOperationsToolbarTablet().folder())
            .shouldSee(steps.pages().touch().messageList().folderPopup().folders())
            .clicksOn(steps.pages().touch().messageList().folderPopup())
            .shouldNotSee(steps.pages().touch().messageList().folderPopup())
            .shouldSee(steps.pages().touch().messageView().groupOperationsToolbarTablet());
    }

    @Test
    @Title("После отметки всех писем в поисковой выдаче как спам должны подгрузиться ещё")
    @TestCaseId("608")
    @DoTestOnlyForEnvironment("Phone")
    public void shouldLoadMoreMsgesAfterSpam() {
        steps.user().apiMessagesSteps().sendCoupleMessages(accLock.firstAcc(), NUM_OF_MSG);
        steps.user().defaultSteps().refreshPage();
        openGroupOperationToolbar();
        steps.user().defaultSteps()
            .clicksOn(
                steps.pages().touch().search().groupOperationsToolbarHeader().selectAll(),
                steps.pages().touch().search().groupOperationsToolbarPhone().spam()
            )
            .shouldSee(steps.pages().touch().search().messageBlock());
    }

    @Test
    @Title("После отметки всех писем в поисковой выдаче как спам должны подгрузиться ещё")
    @TestCaseId("608")
    @DoTestOnlyForEnvironment("Tablet")
    public void shouldLoadMoreMsgesAfterSpamTablet() {
        steps.user().apiMessagesSteps().sendCoupleMessages(accLock.firstAcc(), NUM_OF_MSG_FOR_TABLET);
        steps.user().defaultSteps().refreshPage();
        openGroupOperationToolbar();
        steps.user().defaultSteps()
            .clicksOn(
                steps.pages().touch().messageList().groupOperationsToolbarHeader().selectAll(),
                steps.pages().touch().messageView().groupOperationsToolbarTablet().spam()
            )
            .shouldSee(steps.pages().touch().search().messageBlock());
    }

    @Test
    @Title("После удаления всех писем в поисковой выдаче должны подгрузиться ещё")
    @TestCaseId("957")
    @DoTestOnlyForEnvironment("Phone")
    public void shouldLoadMoreMsgesAfterDelete() {
        steps.user().apiMessagesSteps().sendCoupleMessages(accLock.firstAcc(), NUM_OF_MSG);
        steps.user().defaultSteps().refreshPage();
        openGroupOperationToolbar();
        steps.user().defaultSteps()
            .clicksOn(
                steps.pages().touch().search().groupOperationsToolbarHeader().selectAll(),
                steps.pages().touch().search().groupOperationsToolbarPhone().delete()
            )
            .shouldSee(steps.pages().touch().search().messageBlock());
    }

    @Test
    @Title("После удаления всех писем в поисковой выдаче должны подгрузиться ещё")
    @TestCaseId("957")
    @DoTestOnlyForEnvironment("Tablet")
    public void shouldLoadMoreMsgesAfterDeleteTablet() {
        steps.user().apiMessagesSteps().sendCoupleMessages(accLock.firstAcc(), NUM_OF_MSG_FOR_TABLET);
        steps.user().defaultSteps().refreshPage();
        openGroupOperationToolbar();
        steps.user().defaultSteps()
            .clicksOn(
                steps.pages().touch().messageList().groupOperationsToolbarHeader().selectAll(),
                steps.pages().touch().messageView().groupOperationsToolbarTablet().delete()
            )
            .shouldSee(steps.pages().touch().search().messageBlock());
    }

    @Step("Проверяем количество писем с пользовательской меткой")
    private void checkLabeledMsgCount(int num) {
        steps.user().apiMessagesSteps().shouldGetMsgCountInLabelViaApi(
            steps.user().apiLabelsSteps().getAllUserLabels().get(0).getName(),
            num
        );
    }

    @Step("Открываем тулбар групповых операций")
    private void openGroupOperationToolbar() {
        steps.user().defaultSteps().clicksOn(steps.pages().touch().messageList().messages().get(1).avatar());
    }

    @Step("Проверяем количесвто писем после выполнения действия")
    private void checkMsgsCountAfterOperation(int searchCount, String folder, int folderCount) {
        steps.user().defaultSteps().waitInSeconds(1)
            .shouldSeeElementsCount(steps.pages().touch().messageList().messages(), searchCount);
        steps.user().apiMessagesSteps().shouldGetMsgCountViaApi(folder, folderCount);
    }

    @Step("Открываем тулбар групповых операций для 2ух писем и начинаем менять метки")
    private void openGroupOperationsToolbarAndChangeLabel() {
        openGroupOperationToolbar();
        steps.user().defaultSteps().clicksOn(
            steps.pages().touch().messageList().messages().get(0).avatar(),
            steps.pages().touch().messageList().groupOperationsToolbarPhone().more(),
            steps.pages().touch().messageList().groupOperationsToast().label()
        )
            .shouldSee(steps.pages().touch().messageList().popup())
            .clicksOn(steps.pages().touch().messageList().popup().labels().get(USER_LABEL_INDEX))
            .shouldSee(steps.pages().touch().messageList().popup().tick());
    }

    @Step("Открываем тулбар групповых операций для 2ух писем и начинаем менять метки")
    private void openGroupOperationsToolbarAndChangeLabelTablet() {
        openGroupOperationToolbar();
        steps.user().defaultSteps()
            .clicksOn(
                steps.pages().touch().messageList().messages().get(0).avatar(),
                steps.pages().touch().messageView().groupOperationsToolbarTablet().label()
            )
            .shouldSee(steps.pages().touch().messageList().popup())
            .clicksOn(steps.pages().touch().messageList().popup().labels().get(USER_LABEL_INDEX))
            .shouldSeeElementsCount(steps.pages().touch().messageList().popup().tick(), 1);
    }
}
