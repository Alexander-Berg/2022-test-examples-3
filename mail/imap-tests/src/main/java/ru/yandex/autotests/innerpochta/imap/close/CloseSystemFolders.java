package ru.yandex.autotests.innerpochta.imap.close;

import java.util.Collection;

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

import static ch.lambdaj.collection.LambdaCollections.with;
import static com.google.common.base.Joiner.on;
import static ru.yandex.autotests.innerpochta.imap.config.SystemFoldersProperties.systemFolders;
import static ru.yandex.autotests.innerpochta.imap.converters.ToObjectConverter.wrap;
import static ru.yandex.autotests.innerpochta.imap.requests.CloseRequest.close;
import static ru.yandex.autotests.innerpochta.imap.requests.ExamineRequest.examine;
import static ru.yandex.autotests.innerpochta.imap.requests.SelectRequest.select;
import static ru.yandex.autotests.innerpochta.imap.requests.StoreRequest.store;
import static ru.yandex.autotests.innerpochta.imap.requests.UnselectRequest.unselect;
import static ru.yandex.autotests.innerpochta.imap.rules.CleanRule.withCleanBefore;
import static ru.yandex.autotests.innerpochta.imap.utils.Utils.roundBraceList;

/**
 * Created with IntelliJ IDEA.
 * User: vicdev
 * Date: 29.04.14
 * Time: 20:38
 * <p/>
 * [MAILPROTO-2202]
 */
@Aqua.Test
@Title("Команда CLOSE. Закрываем системные папки")
@Features({ImapCmd.CLOSE})
@Stories(MyStories.SYSTEM_FOLDERS)
@Description("Проверяем реакцию на закрытие системных папок (удаление сообщений и т.д.) " +
        "Позитивное и негативное тестирование [DARIA-4359]")
@RunWith(Parameterized.class)
public class CloseSystemFolders extends BaseTest {
    private static Class<?> currentClass = CloseSystemFolders.class;

    private static final int LEVEL_OF_HIERARCHY = 5;
    private static final int NUMBER_OF_MESSAGE = 2;
    @ClassRule
    public static ImapClient imap = newLoginedClient(currentClass);
    @Rule
    public ImapClient prodImap = withCleanBefore(newLoginedClient(currentClass));
    private String sysFolder;


    public CloseSystemFolders(String sysFolder) {
        this.sysFolder = sysFolder;
    }

    @Parameterized.Parameters(name = "sysFolder - {0}")
    public static Collection<Object[]> foldersForCreate() {
        return with(
                systemFolders().getSent(),                 //todo: добавить inbox
                systemFolders().getDrafts(),
                systemFolders().getOutgoing()
        ).convert(wrap());
    }

    @Test
    @Description("Выбираем пустую системную папку. Закрываем папку => OK\n" +
            "Еще раз закрываем => BAD [MAILPROTO-2179]")
    @Stories(MyStories.JIRA)
    @ru.yandex.qatools.allure.annotations.TestCaseId("73")
    public void closeEmptyFolderAfterSelect() {
        imap.request(select(sysFolder)).repeatUntilOk(imap);
        imap.request(close()).shouldBeOk().shouldBeEmpty();
        imap.request(close()).shouldBeBad().statusLineContains(CloseResponse.WRONG_SESSION_STATE);
    }

    @Test
    @Description("Выбираем пустую системную папку для чтения. Закрываем папку => OK\n" +
            "Еще раз закрываем => BAD [MAILPROTO-2179]")
    @Stories(MyStories.JIRA)
    @ru.yandex.qatools.allure.annotations.TestCaseId("74")
    public void closeEmptyFolderAfterExamine() {
        imap.request(examine(sysFolder)).repeatUntilOk(imap);
        imap.request(close()).shouldBeOk().shouldBeEmpty();
        imap.request(close()).shouldBeBad().statusLineContains(CloseResponse.WRONG_SESSION_STATE);
    }

    @Test
    @Description("Добавляем 1 письмо, ставим у него флаг /Deleted. Закрываем папку.\n" +
            "Письмо должно удалиться\n" +
            "[MAILPROTO-2202]")
    @Stories(MyStories.JIRA)
    @ru.yandex.qatools.allure.annotations.TestCaseId("75")
    public void closeWithDeletedOneMessageAfterSelect() throws Exception {
        imap.append().appendRandomMessage(sysFolder);
        imap.select().waitMsgs(sysFolder, 1);

        imap.request(select(sysFolder)).shouldBeOk().repeatUntilOk(imap);
        imap.store().deletedOnMessages(imap.search().allMessages());
        imap.request(close()).shouldBeOk();

        prodImap.noop().pullChanges();
        prodImap.status().numberOfMessagesShouldBe(sysFolder, 0);
    }

    @Test
    @Description("Добавляем 1 письмо, ставим у него флаг /Deleted. Закрываем папку.\n" +
            "Письмо НЕ должно удалиться")
    @ru.yandex.qatools.allure.annotations.TestCaseId("76")
    public void closeWithDeletedOneMessageAfterExamine() throws Exception {
        imap.append().appendRandomMessage(sysFolder);
        imap.select().waitMsgs(sysFolder, 1);

        imap.request(select(sysFolder)).shouldBeOk().repeatUntilOk(imap);
        imap.store().deletedOnMessages(imap.search().allMessages());
        imap.request(unselect()).shouldBeOk();

        imap.request(examine(sysFolder)).shouldBeOk();
        imap.request(close()).shouldBeOk().shouldBeEmpty();

        prodImap.noop().pullChanges();
        prodImap.select().waitMsgs(sysFolder, 1);
        prodImap.status().numberOfMessagesShouldBe(sysFolder, 1);
        //todo: хорошо бы проверить еще search-ем, что id не сбиваются....
    }

    @Test
    @Description("Закрываем папку с 2 сообщениями после выбора папки.\n" +
            "Удаляем 2 сообщений в папке\n" +
            "[MAILPROTO-2202]")
    @Stories(MyStories.JIRA)
    @ru.yandex.qatools.allure.annotations.TestCaseId("77")
    public void closeWithManyDeletedMessagesAfterSelect() throws Exception {
        imap.append().appendManyRandomMessagesWithCopy(sysFolder, NUMBER_OF_MESSAGE);
        imap.select().waitMsgs(sysFolder, NUMBER_OF_MESSAGE);

        imap.request(select(sysFolder)).shouldBeOk().repeatUntilOk(imap);
        imap.store().deletedOnMessages(imap.search().allMessages());
        imap.request(close()).shouldBeOk();

        prodImap.noop().pullChanges();
        prodImap.select().waitMsgs(sysFolder, 0);
        prodImap.status().numberOfMessagesShouldBe(sysFolder, 0);
    }

    @Test
    @Description("Закрываем папку с 2 сообщениями после выбора папки для чтения. Письма должны остаться")
    @ru.yandex.qatools.allure.annotations.TestCaseId("78")
    public void closeWithManyDeletedMessagesAfterExamine() throws Exception {
        imap.append().appendManyRandomMessagesWithCopy(sysFolder, NUMBER_OF_MESSAGE);
        imap.select().waitMsgs(sysFolder, NUMBER_OF_MESSAGE);

        imap.request(select(sysFolder)).shouldBeOk().repeatUntilOk(imap);
        imap.store().deletedOnMessages(imap.search().allMessages());
        imap.request(unselect()).shouldBeOk();

        //добавить проверку, что флаги подтянулись
        imap.request(examine(sysFolder)).shouldBeOk();
        imap.request(close()).shouldBeOk().shouldBeEmpty();

        prodImap.noop().pullChanges();
        prodImap.select().waitMsgs(sysFolder, NUMBER_OF_MESSAGE);
        prodImap.status().numberOfMessagesShouldBe(sysFolder, NUMBER_OF_MESSAGE);
    }

    @Test
    @Description("Закрываем папку с 2 сообщениями со всеми флагами после выбора папки. Письма должны остаться")
    @ru.yandex.qatools.allure.annotations.TestCaseId("79")
    public void closeWithAllFlagsWithoutDeletedMessagesAfterSelect() throws Exception {
        imap.append().appendManyRandomMessagesWithCopy(sysFolder, NUMBER_OF_MESSAGE);
        imap.select().waitMsgs(sysFolder, NUMBER_OF_MESSAGE);

        imap.request(select(sysFolder)).shouldBeOk().repeatUntilOk(imap);
        imap.request(store(on(",").join(imap.search().allMessages()), StoreRequest.FLAGS,
                roundBraceList(MessageFlags.SEEN.value(), MessageFlags.ANSWERED.value(), MessageFlags.DRAFT.value(),
                        MessageFlags.FLAGGED.value()))).shouldBeOk();
        imap.request(close()).shouldBeOk().shouldBeEmpty();

        //todo: добавить проверки что на письмах флаги
        prodImap.noop().pullChanges();
        prodImap.select().waitMsgs(sysFolder, NUMBER_OF_MESSAGE);
        prodImap.status().numberOfMessagesShouldBe(sysFolder, NUMBER_OF_MESSAGE);
    }

    @Test
    @Description("Закрываем папку с 2 сообщениями помеченными всеми возможными флагами\n" +
            " после выбора папки для чтения.\n" +
            "Письма должны остаться")
    @ru.yandex.qatools.allure.annotations.TestCaseId("80")
    public void closeWithAllFlagsWithoutDeletedMessagesAfterExamine() throws Exception {
        imap.append().appendManyRandomMessagesWithCopy(sysFolder, NUMBER_OF_MESSAGE);
        imap.select().waitMsgs(sysFolder, NUMBER_OF_MESSAGE);

        imap.request(select(sysFolder)).shouldBeOk().repeatUntilOk(imap);
        imap.request(store(on(",").join(imap.search().allMessages()), StoreRequest.FLAGS,
                roundBraceList(MessageFlags.SEEN.value(), MessageFlags.ANSWERED.value(), MessageFlags.DRAFT.value(),
                        MessageFlags.FLAGGED.value()))).shouldBeOk();
        imap.request(unselect()).shouldBeOk();

        //добавить проверку, что флаги подтянулись
        imap.request(examine(sysFolder)).shouldBeOk();
        imap.request(close()).shouldBeOk().shouldBeEmpty();

        prodImap.noop().pullChanges();
        prodImap.select().waitMsgs(sysFolder, NUMBER_OF_MESSAGE);
        prodImap.status().numberOfMessagesShouldBe(sysFolder, NUMBER_OF_MESSAGE);
    }

    @Test
    @Description("Закрываем папку с 2 сообщениями после выбора папки для чтения.\n" +
            " Удаляем письма\n" +
            "MAILPROTO-2202")
    @Stories(MyStories.JIRA)
    @ru.yandex.qatools.allure.annotations.TestCaseId("81")
    public void closeWithAllFlagsMessagesAfterSelect() throws Exception {
        imap.append().appendManyRandomMessagesWithCopy(sysFolder, NUMBER_OF_MESSAGE);
        imap.select().waitMsgs(sysFolder, NUMBER_OF_MESSAGE);

        imap.request(select(sysFolder)).shouldBeOk().repeatUntilOk(imap);
        imap.request(store(on(",").join(imap.search().allMessages()), StoreRequest.FLAGS,
                roundBraceList(MessageFlags.SEEN.value(), MessageFlags.ANSWERED.value(), MessageFlags.DRAFT.value(),
                        MessageFlags.FLAGGED.value(), MessageFlags.DELETED.value()))).shouldBeOk();

        imap.request(close()).shouldBeOk();

        //todo: добавить проверки что на письмах флаги
        prodImap.noop().pullChanges();
        prodImap.select().waitMsgs(sysFolder, 0);
        prodImap.status().numberOfMessagesShouldBe(sysFolder, 0);
    }

    @Description("Закрываем папку с 2 сообщениями помеченными всеми возможными флагами после выбора папки для чтения.\n" +
            " Письма должны остаться")
    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("82")
    public void closeWithAllFlagsMessagesAfterExamine() throws Exception {
        imap.append().appendManyRandomMessagesWithCopy(sysFolder, NUMBER_OF_MESSAGE);
        imap.select().waitMsgs(sysFolder, NUMBER_OF_MESSAGE);

        imap.request(select(sysFolder)).shouldBeOk().repeatUntilOk(imap);
        imap.request(store(on(",").join(imap.search().allMessages()),
                StoreRequest.FLAGS, roundBraceList(MessageFlags.SEEN.value(), MessageFlags.ANSWERED.value(),
                        MessageFlags.DRAFT.value(), MessageFlags.FLAGGED.value(), MessageFlags.DELETED.value()))).shouldBeOk();

        //добавить проверку, что флаги подтянулись
        imap.request(examine(sysFolder)).shouldBeOk();
        imap.request(close()).shouldBeOk().shouldBeEmpty();

        prodImap.noop().pullChanges();
        prodImap.select().waitMsgs(sysFolder, NUMBER_OF_MESSAGE);
        prodImap.status().numberOfMessagesShouldBe(sysFolder, NUMBER_OF_MESSAGE);
    }
}
