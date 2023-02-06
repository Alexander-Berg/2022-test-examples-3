package ru.yandex.autotests.innerpochta.imap.close;

import java.util.Collection;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.imap.base.BaseTest;
import ru.yandex.autotests.innerpochta.imap.consts.base.ImapCmd;
import ru.yandex.autotests.innerpochta.imap.consts.base.MyStories;
import ru.yandex.autotests.innerpochta.imap.consts.flags.MessageFlags;
import ru.yandex.autotests.innerpochta.imap.core.imap.ImapClient;
import ru.yandex.autotests.innerpochta.imap.requests.StoreRequest;
import ru.yandex.autotests.innerpochta.imap.responses.CloseResponse;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import static com.google.common.base.Joiner.on;
import static ru.yandex.autotests.innerpochta.imap.data.TestData.allKindsOfFolders;
import static ru.yandex.autotests.innerpochta.imap.requests.CloseRequest.close;
import static ru.yandex.autotests.innerpochta.imap.requests.CreateRequest.create;
import static ru.yandex.autotests.innerpochta.imap.requests.ExamineRequest.examine;
import static ru.yandex.autotests.innerpochta.imap.requests.SelectRequest.select;
import static ru.yandex.autotests.innerpochta.imap.requests.StoreRequest.store;
import static ru.yandex.autotests.innerpochta.imap.requests.UnselectRequest.unselect;
import static ru.yandex.autotests.innerpochta.imap.rules.CleanRule.withCleanBefore;
import static ru.yandex.autotests.innerpochta.imap.utils.Utils.roundBraceList;

/**
 * Created with IntelliJ IDEA.
 * User: vicdev
 * Date: 12.05.14
 * Time: 19:59
 * <p/>
 * [MAILPROTO-2202]
 * [MAILDLV-164]
 */
@Aqua.Test
@Title("Команда CLOSE. Закрываем пользовательские папки")
@Features({ImapCmd.CLOSE})
@Stories(MyStories.USER_FOLDERS)
@Description("Проверяем реакцию на закрытие (удаление сообщений и т.д.) " +
        "Позитивное и негативное тестирование")
@RunWith(Parameterized.class)
public class CloseUserFolders extends BaseTest {
    private static Class<?> currentClass = CloseUserFolders.class;

    private static final int LEVEL_OF_HIERARCHY = 5;
    private static final int NUMBER_OF_MESSAGE = 10;
    @ClassRule
    public static ImapClient imap = newLoginedClient(currentClass);
    @Rule
    public ImapClient prodImap = withCleanBefore(newLoginedClient(currentClass));
    private String userFolder;


    public CloseUserFolders(String userFolder) {
        this.userFolder = userFolder;
    }

    @Parameterized.Parameters(name = "userFolder - {0}")
    public static Collection<Object[]> foldersForCreate() {
        return allKindsOfFolders();
    }

    @Before
    public void prepareFolder() {
        prodImap.request(create(userFolder));
        imap.list().shouldSeeFolder(userFolder);
    }

    @Test
    @Description("Выбираем пустую папку. Закрываем папку => OK\n" +
            "Еще раз закрываем => BAD")
    @ru.yandex.qatools.allure.annotations.TestCaseId("92")
    public void closeEmptySystemFolderAfterSelect() {
        imap.request(select(userFolder)).repeatUntilOk(imap);
        imap.request(close()).shouldBeOk().shouldBeEmpty();
        imap.request(close()).shouldBeBad().statusLineContains(CloseResponse.WRONG_SESSION_STATE);
    }

    @Test
    @Description("Выбираем пустую папку для чтения. Закрываем папку => OK\n" +
            "Еще раз закрываем => BAD")
    @ru.yandex.qatools.allure.annotations.TestCaseId("83")
    public void closeEmptyFolderAfterExamine() {
        imap.request(examine(userFolder)).repeatUntilOk(imap);
        imap.request(close()).shouldBeOk().shouldBeEmpty();
        imap.request(close()).shouldBeBad().statusLineContains(CloseResponse.WRONG_SESSION_STATE);
    }

    @Test
    @Description("Добавляем 1 письмо, ставим у него флаг /Deleted. Закрываем папку.\n" +
            "Письмо должно удалиться [MAILPROTO-2202]")
    @Stories(MyStories.JIRA)
    @ru.yandex.qatools.allure.annotations.TestCaseId("84")
    public void closeWithDeletedOneMessageAfterSelect() throws Exception {
        prodImap.append().appendRandomMessage(userFolder);
        prodImap.select().waitMsgs(userFolder, 1);

        imap.request(select(userFolder)).shouldBeOk().repeatUntilOk(imap);
        imap.noop().pullChanges();
        imap.store().deletedOnMessages(imap.search().allMessages());
        imap.request(close()).shouldBeOk();

        prodImap.status().numberOfMessagesShouldBe(userFolder, 0);
    }

    @Test
    @Description("Добавляем 1 письмо, ставим у него флаг /Deleted. " +
            "Открываем папку для чтения. Закрываем папку.\n" +
            "Письмо НЕ должно удалиться")
    @ru.yandex.qatools.allure.annotations.TestCaseId("85")
    public void closeWithDeletedOneMessageAfterExamine() throws Exception {
        prodImap.append().appendRandomMessage(userFolder);
        prodImap.select().waitMsgs(userFolder, 1);

        prodImap.request(select(userFolder)).shouldBeOk().repeatUntilOk(imap);
        prodImap.store().deletedOnMessages(imap.search().allMessages());
        prodImap.request(unselect()).shouldBeOk();

        imap.request(examine(userFolder)).shouldBeOk();
        imap.request(close()).shouldBeOk().shouldBeEmpty();

        prodImap.select().waitMsgs(userFolder, 1);
        prodImap.status().numberOfMessagesShouldBe(userFolder, 1);
        //todo: хорошо бы проверить еще search-ем, что id не сбиваются....
    }

    @Test
    @Description("Закрываем папку со 10 сообщениями после выбора папки. Удаляем 10 сообщений в папке\n" +
            "[MAILPROTO-2202]")
    @Stories(MyStories.JIRA)
    @ru.yandex.qatools.allure.annotations.TestCaseId("86")
    public void closeWithManyDeletedMessagesAfterSelect() throws Exception {
        prodImap.append().appendManyRandomMessagesWithCopy(userFolder, NUMBER_OF_MESSAGE);
        prodImap.select().waitMsgs(userFolder, NUMBER_OF_MESSAGE);

        imap.request(select(userFolder)).shouldBeOk().repeatUntilOk(imap).existsShouldBe(NUMBER_OF_MESSAGE);
        imap.store().deletedOnMessages(imap.search().allMessages());
        imap.request(close()).shouldBeOk();

        prodImap.select().waitMsgs(userFolder, 0);
        prodImap.status().numberOfMessagesShouldBe(userFolder, 0);
    }

    @Test
    @Description("Закрываем папку со 10 сообщениями после выбора папки для чтения. Письма должны остаться")
    @Stories(MyStories.JIRA)
    @ru.yandex.qatools.allure.annotations.TestCaseId("87")
    public void closeWithManyDeletedMessagesAfterExamine() throws Exception {
        prodImap.append().appendManyRandomMessagesWithCopy(userFolder, NUMBER_OF_MESSAGE);
        prodImap.select().waitMsgs(userFolder, NUMBER_OF_MESSAGE);

        prodImap.request(select(userFolder)).shouldBeOk().repeatUntilOk(imap);
        prodImap.store().deletedOnMessages(prodImap.search().allMessages());
        prodImap.request(unselect()).shouldBeOk();

        //добавить проверку, что флаги подтянулись
        imap.request(examine(userFolder)).shouldBeOk();
        imap.request(close()).shouldBeOk().shouldBeEmpty();

        prodImap.select().waitMsgs(userFolder, NUMBER_OF_MESSAGE);
        prodImap.status().numberOfMessagesShouldBe(userFolder, NUMBER_OF_MESSAGE);
    }

    @Test
    @Description("Закрываем папку со 10 сообщениями после выбора папки для чтения. Письма должны остаться")
    @Stories(MyStories.JIRA)
    @ru.yandex.qatools.allure.annotations.TestCaseId("88")
    public void closeWithAllFlagsWithoutDeletedMessagesAfterSelect() throws Exception {
        prodImap.append().appendManyRandomMessagesWithCopy(userFolder, NUMBER_OF_MESSAGE);
        prodImap.select().waitMsgs(userFolder, NUMBER_OF_MESSAGE);

        imap.request(select(userFolder)).shouldBeOk().repeatUntilOk(imap);
        imap.request(store(on(",").join(imap.search().allMessages()), StoreRequest.FLAGS, roundBraceList(MessageFlags.SEEN.value(),
                MessageFlags.ANSWERED.value(), MessageFlags.DRAFT.value(),
                MessageFlags.FLAGGED.value()))).shouldBeOk();
        imap.request(close()).shouldBeOk().shouldBeEmpty();

        //todo: добавить проверки что на письмах флаги
        prodImap.select().waitMsgs(userFolder, NUMBER_OF_MESSAGE);
        prodImap.status().numberOfMessagesShouldBe(userFolder, NUMBER_OF_MESSAGE);
    }

    @Test
    @Description("Закрываем папку со 10 сообщениями помеченными всеми возможными флагами " +
            "после выбора папки для чтения.\n" +
            "Письма должны остаться\n")
    @ru.yandex.qatools.allure.annotations.TestCaseId("89")
    public void closeWithAllFlagsWithoutDeletedMessagesAfterExamine() throws Exception {
        prodImap.append().appendManyRandomMessagesWithCopy(userFolder, NUMBER_OF_MESSAGE);
        prodImap.select().waitMsgs(userFolder, NUMBER_OF_MESSAGE);

        prodImap.request(select(userFolder)).shouldBeOk().repeatUntilOk(imap);
        prodImap.request(store(on(",").join(prodImap.search().allMessages()), StoreRequest.FLAGS, roundBraceList(MessageFlags.SEEN.value(),
                MessageFlags.ANSWERED.value(), MessageFlags.DRAFT.value(),
                MessageFlags.FLAGGED.value()))).shouldBeOk();
        prodImap.request(unselect()).shouldBeOk();

        //добавить проверку, что флаги подтянулись
        imap.request(examine(userFolder)).shouldBeOk();
        imap.request(close()).shouldBeOk().shouldBeEmpty();

        prodImap.select().waitMsgs(userFolder, NUMBER_OF_MESSAGE);
        prodImap.status().numberOfMessagesShouldBe(userFolder, NUMBER_OF_MESSAGE);
    }

    @Test
    @Description("Закрываем папку со 10 сообщениями после выбора папки для чтения.\n " +
            "Удаляем письма\n" +
            "MAILPROTO-2202")
    @Stories(MyStories.JIRA)
    @ru.yandex.qatools.allure.annotations.TestCaseId("90")
    public void closeWithAllFlagsMessagesAfterSelect() throws Exception {
        prodImap.append().appendManyRandomMessagesWithCopy(userFolder, NUMBER_OF_MESSAGE);
        prodImap.select().waitMsgs(userFolder, NUMBER_OF_MESSAGE);

        imap.request(select(userFolder)).shouldBeOk().repeatUntilOk(imap);
        imap.request(store(on(",").join(imap.search().allMessages()), StoreRequest.FLAGS,
                roundBraceList(MessageFlags.SEEN.value(), MessageFlags.ANSWERED.value(), MessageFlags.DRAFT.value(),
                        MessageFlags.FLAGGED.value(), MessageFlags.DELETED.value()))).shouldBeOk();

        imap.request(close()).shouldBeOk();

        //todo: добавить проверки что на письмах флаги
        prodImap.select().waitMsgs(userFolder, 0);
        prodImap.status().numberOfMessagesShouldBe(userFolder, 0);
    }

    @Test
    @Description("Закрываем папку с 10 сообщениями, " +
            "помеченными всеми возможными флагами после выбора папки для чтения.\n" +
            "Письма должны остаться")
    @ru.yandex.qatools.allure.annotations.TestCaseId("91")
    public void closeWithAllFlagsMessagesAfterExamine() throws Exception {
        prodImap.append().appendManyRandomMessagesWithCopy(userFolder, NUMBER_OF_MESSAGE);
        prodImap.select().waitMsgs(userFolder, NUMBER_OF_MESSAGE);

        prodImap.request(select(userFolder)).shouldBeOk().repeatUntilOk(imap);
        prodImap.request(store(on(",").join(prodImap.search().allMessages()), StoreRequest.FLAGS,
                roundBraceList(MessageFlags.SEEN.value(), MessageFlags.ANSWERED.value(), MessageFlags.DRAFT.value(),
                        MessageFlags.FLAGGED.value(), MessageFlags.DELETED.value()))).shouldBeOk();

        //добавить проверку, что флаги подтянулись
        imap.request(examine(userFolder)).shouldBeOk();
        imap.request(close()).shouldBeOk().shouldBeEmpty();

        prodImap.select().waitMsgs(userFolder, NUMBER_OF_MESSAGE);
        prodImap.status().numberOfMessagesShouldBe(userFolder, NUMBER_OF_MESSAGE);
    }
}
