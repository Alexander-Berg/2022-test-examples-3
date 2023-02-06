package ru.yandex.autotests.innerpochta.tests.messageslist;

import io.qameta.allure.junit4.Tag;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.rules.RestAssuredAuthRule;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.steps.AllureStepStorage;
import ru.yandex.autotests.innerpochta.steps.beans.message.Message;
import ru.yandex.autotests.innerpochta.tests.BaseTest;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import static com.google.common.collect.ImmutableMap.of;
import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.util.MailConst.DEV_NULL_EMAIL;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomString;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.TRUE;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.LAYOUT_2PANE;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_FOLDER_THREAD_VIEW;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_PARAM_LAYOUT;

/**
 * @author vasily-k
 */
@Aqua.Test
@Title("Отображение письма в заголовке треда")
@Features(FeaturesConst.MESSAGE_LIST)
@Tag(FeaturesConst.MESSAGE_LIST)
@Stories(FeaturesConst.THREAD)
public class ThreadHeaderTest extends BaseTest {

    private String inboxText;
    private String sentText;
    private String customText;
    private AccLockRule lock = AccLockRule.use().useTusAccount();
    private RestAssuredAuthRule auth = RestAssuredAuthRule.auth(lock);
    private AllureStepStorage user = new AllureStepStorage(webDriverRule, auth);

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain rules = RuleChain.outerRule(lock).around(auth)
        .around(clearAcc(() -> user));

    @Before
    public void logIn() {
        inboxText = getRandomString();
        sentText = getRandomString();
        customText = getRandomString();
        String customFolder = getRandomString();
        String subject = getRandomString();
        Message msg = user.apiMessagesSteps().sendMailWithNoSave(lock.firstAcc(), subject, inboxText);
        user.apiMessagesSteps().sendMessageToThreadWithMessage(msg, lock.firstAcc(), customText);
        user.apiMessagesSteps().sendMail(DEV_NULL_EMAIL, subject, sentText);
        user.apiFoldersSteps().createNewFolder(customFolder);
        user.apiSettingsSteps().callWithListAndParams(
            "Включаем 2pane и треды",
            of(
                SETTINGS_PARAM_LAYOUT, LAYOUT_2PANE,
                SETTINGS_FOLDER_THREAD_VIEW, TRUE
            )
        );
        user.loginSteps().forAcc(lock.firstAcc()).logins();
        user.messagesSteps().expandsMessagesThread(subject)
            .selectMessagesInThreadCheckBoxWithNumber(1)
            .movesMessageToFolder(customFolder);
        user.defaultSteps().refreshPage();
    }

    @Test
    @Title("Заголовок треда в папке «Входящие» должен иметь текст последнего входящего письма")
    @TestCaseId("2355")
    public void shouldSeeLastIncomingMessageTextInThreadHeader() {
        user.defaultSteps().shouldSee(onMessagePage().displayedMessages())
            .shouldHasText(onMessagePage().displayedMessages().list().get(0).firstLine(), inboxText);
    }

    @Test
    @Title("Заголовок треда в папке «Отправленные» должен иметь текст последнего отправленного письма")
    @TestCaseId("2355")
    public void shouldSeeLastSentMessageTextInThreadHeader() {
        user.leftColumnSteps().opensSentFolder();
        user.defaultSteps().shouldSee(onMessagePage().displayedMessages())
            .shouldHasText(onMessagePage().displayedMessages().list().get(0).firstLine(), sentText);
    }

    @Test
    @Title("Заголовок треда в пользовательской папке должен иметь текст последнего письма в этой папке")
    @TestCaseId("2355")
    public void shouldSeeLastCustomMessageTextInThreadHeader() {
        user.leftColumnSteps().openFolders()
            .opensCustomFolder(0);
        user.defaultSteps().shouldSee(onMessagePage().displayedMessages())
            .shouldHasText(onMessagePage().displayedMessages().list().get(0).firstLine(), customText);
    }
}
