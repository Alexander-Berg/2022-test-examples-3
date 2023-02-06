package ru.yandex.autotests.innerpochta.tests.autotests.messageslist;

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

import static ru.yandex.autotests.innerpochta.rules.TouchRulesManager.touchRulesManager;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.ARCHIVE;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.INBOX;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.SPAM;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.TRASH;
import static ru.yandex.autotests.innerpochta.util.handlers.LabelsConstants.LABELS_PARAM_RED_COLOR;

/**
 * @author puffyfloof
 */

@Aqua.Test
@Title("Тулбар групповых операций")
@Features({FeaturesConst.MESSAGE_LIST})
@Stories(FeaturesConst.TOOLBAR)
public class GroupOperationsToolbarTest {

    private static final int MESSAGE_NUMBER = 2;
    private static final int USER_FOLDER_INDEX = 0;
    private static final int USER_LABEL_INDEX = 1;
    private static final String SUBJECT_ONE = "subj 1";
    private static final String SUBJECT_TWO = "subj 2";
    private static final String SUBJECT_THREE = "subj 3";

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
        labelName = Utils.getRandomName();
        folderName = Utils.getRandomName();
        steps.user().apiMessagesSteps().sendCoupleMessages(accLock.firstAcc(), MESSAGE_NUMBER);
        steps.user().apiFoldersSteps().createNewFolder(folderName);
        steps.user().apiLabelsSteps().markWithLabel(
            steps.user().apiMessagesSteps().getAllMessagesInFolder(INBOX).get(0),
            steps.user().apiLabelsSteps().addNewLabel(labelName, LABELS_PARAM_RED_COLOR)
        );
        steps.user().defaultSteps().waitInSeconds(3);//ждём, чтобы все письма пришли и счётчики не скакали после логина
        steps.user().loginSteps().forAcc(accLock.firstAcc()).logins();
    }

    @Test
    @Title("Удаляем одно письмо")
    @TestCaseId("392")
    @DoTestOnlyForEnvironment("Phone")
    public void shouldDeleteMessages() {
        openGroupOperationToolbar();
        steps.user().defaultSteps()
            .clicksOn(steps.pages().touch().messageList().groupOperationsToolbarPhone().delete())
            .shouldSeeElementsCount(steps.pages().touch().messageList().messages(), MESSAGE_NUMBER - 1);
        steps.user().apiMessagesSteps().shouldGetMsgCountViaApi(TRASH, 1);
    }

    @Test
    @Title("Удаляем письма по-одному")
    @TestCaseId("392")
    @DoTestOnlyForEnvironment("Tablet")
    public void shouldDeleteMessagesTablet() {
        steps.user().apiMessagesSteps().sendMail(accLock.firstAcc(), SUBJECT_THREE, "");
        steps.user().defaultSteps().refreshPage()
            .clicksOn(steps.pages().touch().messageList().messages().get(1))
            .shouldSeeThatElementHasText(steps.pages().touch().messageView().threadHeader(), SUBJECT_TWO);
        deleteMsgFromToolbar(1, MESSAGE_NUMBER);
        steps.user().defaultSteps()
            .shouldSeeThatElementHasText(steps.pages().touch().messageView().threadHeader(), SUBJECT_ONE);
        steps.user().apiMessagesSteps().shouldGetMsgCountViaApi(TRASH, 1);
        deleteMsgFromToolbar(1, MESSAGE_NUMBER - 1);
        steps.user().defaultSteps()
            .shouldSeeThatElementHasText(steps.pages().touch().messageView().threadHeader(), SUBJECT_THREE);
        deleteMsgFromToolbar(0, MESSAGE_NUMBER - 2);
        steps.user().defaultSteps().shouldNotSee(steps.pages().touch().messageView().msgBody());
    }

    @Test
    @Title("Отправляем письмо в спам")
    @TestCaseId("393")
    @DoTestOnlyForEnvironment("Phone")
    public void shoudMoveMessagesToSpam() {
        openGroupOperationToolbar();
        steps.user().defaultSteps()
            .clicksOn(steps.pages().touch().messageList().groupOperationsToolbarPhone().spam())
            .shouldSeeElementsCount(steps.pages().touch().messageList().messages(), MESSAGE_NUMBER - 1);
        steps.user().apiMessagesSteps().shouldGetMsgCountViaApi(SPAM, 1);
    }

    @Test
    @Title("Отправляем письмо в спам")
    @TestCaseId("393")
    @DoTestOnlyForEnvironment("Tablet")
    public void shoudMoveMessagesToSpamTablet() {
        openGroupOperationToolbar();
        steps.user().defaultSteps()
            .clicksOn(steps.pages().touch().messageView().groupOperationsToolbarTablet().spam())
            .shouldSeeElementsCount(steps.pages().touch().messageList().messages(), MESSAGE_NUMBER - 1);
        steps.user().apiMessagesSteps().shouldGetMsgCountViaApi(SPAM, 1);
    }

    @Test
    @Title("Отмечаем письмо прочитанным")
    @TestCaseId("394")
    @DoTestOnlyForEnvironment("Phone")
    public void shouldMarkMessagesRead() {
        steps.user().defaultSteps().shouldSeeThatElementTextEquals(
            steps.pages().touch().messageList().headerBlock().unreadCounter(),
            String.valueOf(MESSAGE_NUMBER)
        );
        openGroupOperationToolbar();
        steps.user().defaultSteps()
            .clicksOn(steps.pages().touch().messageList().groupOperationsToolbarPhone().read())
            .shouldSeeThatElementTextEquals(
                steps.pages().touch().messageList().headerBlock().unreadCounter(),
                String.valueOf(MESSAGE_NUMBER - 1)
            )
            .shouldSeeElementsCount(steps.pages().touch().messageList().messages(), MESSAGE_NUMBER);
    }

    @Test
    @Title("Отмечаем письмо прочитанным")
    @TestCaseId("394")
    @DoTestOnlyForEnvironment("Tablet")
    public void shouldMarkMessagesReadTablet() {
        steps.user().defaultSteps().shouldSeeThatElementTextEquals(
            steps.pages().touch().messageList().headerBlock().unreadCounter(),
            String.valueOf(MESSAGE_NUMBER - 1)
        );
        openGroupOperationToolbar();
        steps.user().defaultSteps()
            .clicksOn(steps.pages().touch().messageView().groupOperationsToolbarTablet().read())
            .shouldNotSee(steps.pages().touch().messageList().headerBlock().unreadCounter())
            .shouldSeeElementsCount(steps.pages().touch().messageList().messages(), MESSAGE_NUMBER);
    }

    @Test
    @Title("Отмечаем письмо непрочитанным")
    @TestCaseId("395")
    @DoTestOnlyForEnvironment("Phone")
    public void shouldMarkMessagesUnread() {
        steps.user().apiMessagesSteps().markAllMsgRead();
        steps.user().defaultSteps().refreshPage();
        openGroupOperationToolbar();
        steps.user().defaultSteps()
            .shouldNotSee(steps.pages().touch().messageList().headerBlock().unreadCounter())
            .clicksOn(steps.pages().touch().messageList().groupOperationsToolbarPhone().unread())
            .shouldSeeThatElementTextEquals(
                steps.pages().touch().messageList().headerBlock().unreadCounter(),
                String.valueOf(MESSAGE_NUMBER - 1)
            );
    }

    @Test
    @Title("Отмечаем письмо непрочитанным")
    @TestCaseId("395")
    @DoTestOnlyForEnvironment("Tablet")
    public void shouldMarkMessagesUnreadTablet() {
        steps.user().apiMessagesSteps().markAllMsgRead();
        steps.user().defaultSteps().refreshPage();
        openGroupOperationToolbar();
        steps.user().defaultSteps()
            .shouldNotSee(steps.pages().touch().messageList().headerBlock().unreadCounter())
            .clicksOn(steps.pages().touch().messageView().groupOperationsToolbarTablet().unread())
            .shouldSeeThatElementTextEquals(
                steps.pages().touch().messageList().headerBlock().unreadCounter(),
                String.valueOf(MESSAGE_NUMBER - 1)
            );
    }

    @Test
    @Title("Переносим письмо в пользовательскую папку")
    @TestCaseId("396")
    @DoTestOnlyForEnvironment("Phone")
    public void shouldMoveMessagesToFolder() {
        openGroupOperationToolbar();
        steps.user().defaultSteps()
            .clicksOn(
                steps.pages().touch().messageList().groupOperationsToolbarPhone().more(),
                steps.pages().touch().messageList().groupOperationsToast().folder()
            )
            .clicksOn(steps.pages().touch().messageList().folderPopup().folders().get(USER_FOLDER_INDEX))
            .shouldSeeElementsCount(steps.pages().touch().messageList().messages(), MESSAGE_NUMBER - 1);
        steps.user().apiMessagesSteps().shouldGetMsgCountViaApi(folderName, 1);
    }

    @Test
    @Title("Переносим письмо в пользовательскую папку")
    @TestCaseId("396")
    @DoTestOnlyForEnvironment("Tablet")
    public void shouldMoveMessagesToFolderTablet() {
        openGroupOperationToolbar();
        steps.user().defaultSteps()
            .clicksOn(steps.pages().touch().messageView().groupOperationsToolbarTablet().folder())
            .clicksOn(steps.pages().touch().messageList().folderPopup().folders().get(USER_FOLDER_INDEX))
            .shouldSeeElementsCount(steps.pages().touch().messageList().messages(), MESSAGE_NUMBER - 1);
        steps.user().apiMessagesSteps().shouldGetMsgCountViaApi(folderName, 1);
    }

    @Test
    @Title("Отправляем письмо в архив")
    @TestCaseId("398")
    @DoTestOnlyForEnvironment("Phone")
    public void shouldArchiveMessages() {
        openGroupOperationToolbar();
        steps.user().defaultSteps()
            .clicksOn(
                steps.pages().touch().messageList().groupOperationsToolbarPhone().more(),
                steps.pages().touch().messageList().groupOperationsToast().archive()
            )
            .shouldSeeElementsCount(steps.pages().touch().messageList().messages(), MESSAGE_NUMBER - 1);
        steps.user().apiMessagesSteps().shouldGetMsgCountViaApi(ARCHIVE, 1);
    }

    @Test
    @Title("Отправляем письмо в архив")
    @TestCaseId("398")
    @DoTestOnlyForEnvironment("Tablet")
    public void shouldArchiveMessagesTablet() {
        openGroupOperationToolbar();
        steps.user().defaultSteps()
            .clicksOn(steps.pages().touch().messageView().groupOperationsToolbarTablet().archive())
            .shouldSeeElementsCount(steps.pages().touch().messageList().messages(), MESSAGE_NUMBER - 1);
        steps.user().apiMessagesSteps().shouldGetMsgCountViaApi(ARCHIVE, 1);
    }

    @Test
    @Title("Возвращаем письмо из спама")
    @TestCaseId("406")
    @DoTestOnlyForEnvironment("Phone")
    public void shouldUnspamMessage() {
        steps.user().apiMessagesSteps().moveMessagesToSpam(
            steps.user().apiMessagesSteps().getAllMessagesInFolder(INBOX).get(0),
            steps.user().apiMessagesSteps().getAllMessagesInFolder(INBOX).get(1)
        );
        steps.user().touchUrlSteps().messageListInFid(
            steps.user().apiFoldersSteps().getFolderBySymbol(SPAM).getFid()
        ).open();
        openGroupOperationToolbar();
        steps.user().defaultSteps()
            .clicksOn(steps.pages().touch().messageList().groupOperationsToolbarPhone().unspam())
            .shouldSeeElementsCount(steps.pages().touch().messageList().messages(), MESSAGE_NUMBER - 1);
        steps.user().apiMessagesSteps().shouldGetMsgCountViaApi(INBOX, 1);
    }

    @Test
    @Title("Возвращаем письмо из спама")
    @TestCaseId("406")
    @DoTestOnlyForEnvironment("Tablet")
    public void shouldUnspamMessageTablet() {
        steps.user().apiMessagesSteps().moveMessagesToSpam(
            steps.user().apiMessagesSteps().getAllMessagesInFolder(INBOX).get(0),
            steps.user().apiMessagesSteps().getAllMessagesInFolder(INBOX).get(1)
        );
        steps.user().touchUrlSteps().messageListInFid(
            steps.user().apiFoldersSteps().getFolderBySymbol(SPAM).getFid()
        ).open();
        openGroupOperationToolbar();
        steps.user().defaultSteps()
            .clicksOn(steps.pages().touch().messageView().groupOperationsToolbarTablet().unspam())
            .shouldSeeElementsCount(steps.pages().touch().messageList().messages(), MESSAGE_NUMBER - 1);
        steps.user().apiMessagesSteps().shouldGetMsgCountViaApi(INBOX, 1);
    }

    @Test
    @Title("Должны поставить метку на все выделенные письма")
    @TestCaseId("284")
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
    @TestCaseId("284")
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
    @TestCaseId("286")
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
    @TestCaseId("286")
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
    @TestCaseId("559")
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
    @Title("Должны видеть действие «Прочитано», если выделено хотя бы одно непрочитанное письмо")
    @TestCaseId("1339")
    @DoTestOnlyForEnvironment("Phone")
    public void shouldChangeActionFromUnreadToRead() {
        steps.user().apiMessagesSteps().markLetterRead(steps.user().apiMessagesSteps().getAllMessages().get(1));
        steps.user().defaultSteps().refreshPage();
        openGroupOperationToolbar();
        steps.user().defaultSteps()
            .shouldSee(steps.pages().touch().messageList().groupOperationsToolbarPhone().unread())
            .clicksOn(steps.pages().touch().messageList().messages().get(0).avatar())
            .shouldSee(steps.pages().touch().messageList().groupOperationsToolbarPhone().read());
    }

    @Test
    @Title("Должны видеть действие «Прочитано», если выделено хотя бы одно непрочитанное письмо")
    @TestCaseId("1339")
    @DoTestOnlyForEnvironment("Tablet")
    public void shouldChangeActionFromUnreadToReadTablet() {
        openGroupOperationToolbar(0);
        steps.user().defaultSteps()
            .shouldSee(steps.pages().touch().messageView().groupOperationsToolbarTablet().unread())
            .clicksOn(steps.pages().touch().messageList().messages().get(1).avatar())
            .shouldSee(steps.pages().touch().messageView().groupOperationsToolbarTablet().read());
    }

    @Test
    @Title("Должны отменить выделение письма")
    @TestCaseId("1384")
    public void shouldCancelSelection() {
        openGroupOperationToolbar(0);
        steps.user().defaultSteps()
            .clicksOn(steps.pages().touch().messageList().groupOperationsToolbarHeader().cancel())
            .shouldNotSee(
                steps.pages().touch().messageList().groupOperationsToolbarHeader(),
                steps.pages().touch().messageList().checkedMessageBlock()
            );
    }

    @Test
    @Title("Должны снять выделене со всех писем по клику в «Снять выделение»")
    @TestCaseId("1385")
    public void shouldClearSelection() {
        openGroupOperationToolbar(0);
        steps.user().defaultSteps()
            .clicksOn(steps.pages().touch().messageList().groupOperationsToolbarHeader().selectAll())
            .clicksOn(steps.pages().touch().messageList().groupOperationsToolbarHeader().clearAll())
            .shouldNotSee(
                steps.pages().touch().messageList().groupOperationsToolbarHeader(),
                steps.pages().touch().messageList().checkedMessageBlock()
            );
    }

    @Test
    @Title("Должны закрыть плашку с дополнительными действиями")
    @TestCaseId("1503")
    @DoTestOnlyForEnvironment("Phone")
    public void shouldCloseGroupOperationsToast() {
        openGroupOperationToolbar(0);
        steps.user().defaultSteps()
            .clicksOn(steps.pages().touch().messageList().groupOperationsToolbarPhone().more())
            .clicksOn(steps.pages().touch().messageList().groupOperationsToast().closeBtn())
            .shouldNotSee(steps.pages().touch().messageList().groupOperationsToast())
            .shouldSee(
                steps.pages().touch().messageList().groupOperationsToolbarPhone(),
                steps.pages().touch().messageList().groupOperationsToolbarHeader()
            );
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

    @Step("Проверяем количество писем с пользовательской меткой")
    private void checkLabeledMsgCount(int num) {
        steps.user().apiMessagesSteps().shouldGetMsgCountInLabelViaApi(
            steps.user().apiLabelsSteps().getAllUserLabels().get(0).getName(),
            num
        );
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

    @Step("Открываем тулбар групповых операций")
    private void openGroupOperationToolbar() {
        steps.user().defaultSteps().shouldSee(steps.pages().touch().messageList().messageBlock())
            .clicksOn(steps.pages().touch().messageList().messages().get(1).avatar());
    }

    @Step("Открываем тулбар групповых операций")
    private void openGroupOperationToolbar(int num) {
        steps.user().defaultSteps().shouldSee(steps.pages().touch().messageList().messageBlock())
            .clicksOn(steps.pages().touch().messageList().messages().get(num).avatar());
    }

    @Step("Удаляем письмо с помощью тулбара групповых операций")
    private void deleteMsgFromToolbar(int num, int count) {
        openGroupOperationToolbar(num);
        steps.user().defaultSteps().clicksOn(steps.pages().touch().messageView().groupOperationsToolbarTablet().delete())
            .shouldSeeElementsCount(steps.pages().touch().messageList().messages(), count);
    }
}
