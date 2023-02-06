package ru.yandex.autotests.innerpochta.tests.xiva;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import io.qameta.allure.junit4.Tag;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.rules.RestAssuredAuthRule;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.steps.AllureStepStorage;
import ru.yandex.autotests.innerpochta.steps.beans.folder.Folder;
import ru.yandex.autotests.innerpochta.steps.beans.message.Message;
import ru.yandex.autotests.innerpochta.tests.BaseTest;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.autotests.innerpochta.util.Utils;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Step;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.List;

import static com.google.common.collect.ImmutableMap.of;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static ru.yandex.autotests.innerpochta.matchers.message.MessageThreadMatcher.threadCount;
import static ru.yandex.autotests.innerpochta.rules.resources.SetSettingsRule.setSettings;
import static ru.yandex.autotests.innerpochta.util.MailConst.XIVA_TIMEOUT;
import static ru.yandex.autotests.innerpochta.util.MailConst.PDF_ATTACHMENT;
import static ru.yandex.autotests.innerpochta.util.MailConst.WORD_ATTACHMENT;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomString;
import static ru.yandex.autotests.innerpochta.util.Utils.withWaitFor;
import static ru.yandex.autotests.innerpochta.util.handlers.FiltersConstants.FILTERS_ADD_PARAM_CLICKER_MOVE;
import static ru.yandex.autotests.innerpochta.util.handlers.FiltersConstants.FILTERS_ADD_PARAM_MOVE_FOLDER;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.TRUE;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.LAYOUT_2PANE;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.LAYOUT_3PANE_VERTICAL;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_OPEN_MSG_LIST;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_PARAM_LAYOUT;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.FOLDERS_OPEN;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;

/**
 * @author marchart
 */
@Aqua.Test
@Title("Тесты на треды без обновления страницы")
@Features(FeaturesConst.XIVA)
@Tag(FeaturesConst.XIVA)
@Stories(FeaturesConst.THREAD)
@RunWith(DataProviderRunner.class)
public class XivaThreads extends BaseTest {

    private String expectedSubject;

    private AccLockRule lock = AccLockRule.use().useTusAccount();
    private RestAssuredAuthRule auth = RestAssuredAuthRule.auth(lock);
    private AllureStepStorage user = new AllureStepStorage(webDriverRule, auth);

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain rules = RuleChain.outerRule(lock)
        .around(auth)
        .around(clearAcc(() -> user))
        .around(setSettings(() -> user, of(SETTINGS_OPEN_MSG_LIST, TRUE)));

    @DataProvider
    public static Object[][] userInterface() {
        return new Object[][]{
            {LAYOUT_2PANE},
            {LAYOUT_3PANE_VERTICAL}
        };
    }

    @Before
    public void logIn() {
        user.apiSettingsSteps().callWithListAndParams(
                "Раскрываем все папки",
                of(FOLDERS_OPEN, user.apiFoldersSteps().getAllFids())
        );
        expectedSubject = Utils.getRandomString();
        user.apiMessagesSteps().sendMailWithNoSave(lock.firstAcc(), expectedSubject, Utils.getRandomString());
        user.loginSteps().forAcc(lock.firstAcc()).logins();
    }

    @Test
    @Title("Формируем тред без перезагрузки страницы")
    @TestCaseId("2387")
    @UseDataProvider("userInterface")
    public void shouldCreateThreadWithoutRefresh(String layout) {
        String subjectForFirstlineMessage = Utils.getRandomString();
        enableLayout(layout);
        user.apiMessagesSteps().sendMailWithNoSave(lock.firstAcc(), subjectForFirstlineMessage, Utils.getRandomName());
        user.defaultSteps().refreshPage();
        sendMessageToThread(subjectForFirstlineMessage);
        sendMessageToThread(expectedSubject);
        shouldSeeThreadCount(subjectForFirstlineMessage);
        shouldSeeThreadCount(expectedSubject);
    }

    @Test
    @Title("Формируем тред без перезагрузки страницы в пользовательской папке")
    @TestCaseId("4934")
    @UseDataProvider("userInterface")
    public void shouldCreateThreadWithoutRefreshInCustomFolder(String layout) {
        //добавляем фильтр на перенос в папку, проверяем что в пользовательской папке формируются треды
        Folder folder = user.apiFoldersSteps().createNewFolder(Utils.getRandomString());
        user.apiFiltersSteps().createFilterForFolderOrLabel(
            lock.firstAcc().getLogin(),
            expectedSubject,
            FILTERS_ADD_PARAM_MOVE_FOLDER,
            user.apiFoldersSteps().getFolderByName(folder.getName()).getFid(),
            FILTERS_ADD_PARAM_CLICKER_MOVE,
            false
        );
        enableLayout(layout);
        user.messagesSteps().movesMessageToFolder(expectedSubject, folder.getName());
        user.leftColumnSteps().opensCustomFolder(folder.getName());
        sendMessageToThreadInCustomFolder(expectedSubject, folder);
        shouldSeeThreadCount(expectedSubject);
        shouldSeeThreadCounter(expectedSubject, 3);
    }

    @Test
    @Title("Формируем тред без перезагрузки страницы после ответа через QR")
    @TestCaseId("2398")
    public void shouldCreateThreadAfterQR() {
        enableLayout(LAYOUT_2PANE);
        sendQR(expectedSubject);
        user.defaultSteps().clicksOn(onMessageView().closeMsgBtn());
        shouldSeeThreadCount(expectedSubject);
        shouldSeeThreadCounter(expectedSubject, 3);
    }

    @Test
    @Title("Формируем тред без перезагрузки страницы после ответа через QR в 3pane")
    @TestCaseId("2399")
    public void shouldCreateThreadAfterQR3Pane() {
        enableLayout(LAYOUT_3PANE_VERTICAL);
        sendQR(expectedSubject);
        shouldSeeThreadCount(expectedSubject);
        shouldSeeThreadCounter(expectedSubject, 3);
    }

    @Test
    @Title("Приход письма в тред в момент просмотра письма")
    @TestCaseId("2390")
    public void shouldCreateThreadWhenMessageOpen() {
        enableLayout(LAYOUT_2PANE);
        user.messagesSteps().clicksOnMessageWithSubject(expectedSubject);
        sendMessageToThread(expectedSubject);
        shouldSeeThreadCountInMessageView();
        shouldEqualThreadCountInMessageSubject(onMessageView().messageSubject().threadCount(), 2);
        shouldSeeElementsCount(onMessageView().msgInThread(), 1);
        sendMessageToThread(expectedSubject);
        shouldEqualThreadCountInMessageSubject(onMessageView().messageSubject().threadCount(), 3);
        shouldSeeElementsCount(onMessageView().msgInThread(), 2);
    }

    @Test
    @Title("Приход письма в тред в момент просмотра письма 3pane")
    @TestCaseId("2396")
    public void shouldCreateThreadWhenMessageOpen3pane() {
        enableLayout(LAYOUT_3PANE_VERTICAL);
        user.messagesSteps().clicksOnMessageWithSubject(expectedSubject);
        sendMessageToThread(expectedSubject);
        shouldSeeThreadCount(expectedSubject);
        shouldSeeThreadCounter(expectedSubject, 2);
        shouldSeeElementsCount(onMessageView().expandMsgInThread(), 2);
        sendMessageToThread(expectedSubject);
        shouldSeeThreadCounter(expectedSubject, 3);
        shouldSeeElementsCount(onMessageView().expandMsgInThread(), 3);
    }

    @Test
    @Title("Формируем тред c аттачами")
    @TestCaseId("4699")
    public void shouldCreateThreadWithAttachInHead() {
        sendQRWithAttach(expectedSubject, PDF_ATTACHMENT);
        checkAttachInThreadsHead(PDF_ATTACHMENT, 3);
        sendQRWithAttach(expectedSubject, WORD_ATTACHMENT);
        checkAttachInThreadsHead(WORD_ATTACHMENT, 5);
    }

    @Step("Включаем пользовательский интерфейс с переданным параметром")
    private void enableLayout(String layout) {
        user.apiSettingsSteps().callWithListAndParams("Включаем " + layout, of(SETTINGS_PARAM_LAYOUT, layout));
        user.defaultSteps().refreshPage();
    }

    @Step("Отправляем себе новое сообщение")
    private void sendMessageToThread(String subject) {
        user.apiMessagesSteps().sendMessageToThreadWithSubjectWithNoSave(subject, lock.firstAcc(), "");
    }

    @Step("Отправляем себе новое сообщение без проверки получения")
    private void sendMessageToThreadInCustomFolder(String subject, Folder folder) {
        Message msgWithSubjectInFolder = user.apiMessagesSteps().getAllMessagesInFolder(folder)
            .stream().filter(msg -> msg.getSubject().equals(subject)).findFirst().get();
        user.apiMessagesSteps().sendMessageToThreadWithMessageWithoutCheck(
            msgWithSubjectInFolder,
            lock.firstAcc()
        );
    }

    @Step("Ответить через QR на тред с темой «{0}»")
    private void sendQR(String messageSubject) {
        user.messagesSteps().clicksOnMessageWithSubject(messageSubject);
        user.defaultSteps().clicksOn(onMessageView().quickReplyPlaceholder())
            .inputsTextInElement(onMessageView().quickReply().replyText(), getRandomString())
            .clicksOn(onMessageView().quickReply().sendButton())
            .shouldSee(onMessageView().quickReplyPlaceholder());
    }

    @Step("Ответить через QR на тред с темой «{0}»")
    private void sendQRWithAttach(String messageSubject, String attach) {
        user.messagesSteps().clicksOnMessageWithSubject(messageSubject);
        user.defaultSteps().clicksOn(onMessageView().quickReplyPlaceholder())
                .inputsTextInElement(onMessageView().quickReply().replyText(), getRandomString());
        user.composeSteps().uploadLocalFile(
                onMessageView().quickReply().localAttachInput(),
                attach
        );
        user.defaultSteps().clicksOn(
                onComposePopup().expandedPopup().sendBtn(),
                onMessageView().closeMsgBtn()
        );
    }

    @Step("Проверяем аттачи в шапке треда")
    private void checkAttachInThreadsHead(String attach, int count) {
        shouldSeeThreadCount(expectedSubject);
        shouldSeeThreadCounter(expectedSubject, count);
        user.defaultSteps().shouldContainText(
                user.pages().MessagePage().displayedMessages().list().get(0).attachments().list().get(0).title(),
                attach
        );
    }

    @Step("В треде с темой «{0}» должен отображаться каунтер писем треда")
    private void shouldSeeThreadCount(String subject) {
        user.messagesSteps().shouldSeeMessageWithSubject(subject);
        assertThat(
            "Каутер треда не отображается на странице",
            user.messagesSteps().findMessageBySubject(subject).expandThread(),
            withWaitFor(isDisplayed(), XIVA_TIMEOUT)
        );
    }

    @Step("В открытом треде с темой «{0}» должен отображаться каунтер писем треда")
    private void shouldSeeThreadCountInMessageView() {
        assertThat(
            "Каутер треда не отображается на странице",
            onMessageView().messageSubject().threadCount(),
            withWaitFor(isDisplayed(), XIVA_TIMEOUT)
        );
    }

    @Step("В треде с темой «{0}» должно быть «{1}» сообщений/я/е")
    private void shouldSeeThreadCounter(String subject, int counter) {
        assertThat(
            "Ожидалось другое количество писем в треде",
            webDriverRule,
            withWaitFor(threadCount(subject, counter), XIVA_TIMEOUT)
        );
    }

    @Step("Количество писем треда «{0}» в заголовке должно совпадать с «{1}»")
    private void shouldEqualThreadCountInMessageSubject(MailElement element, int size) {
        String threadCount = String.format("%s", size);
        assertThat("Элемент не содержит нужный текст", element, withWaitFor(hasText(threadCount), XIVA_TIMEOUT));
    }

    @Step("Количество элементов в списке «{0}» должно быть «{1}»")
    private void shouldSeeElementsCount(List<? extends MailElement> list, int size) {
        assertThat("Количество элементов в списке не совпадает", list, withWaitFor(hasSize(size), XIVA_TIMEOUT));
    }
}


