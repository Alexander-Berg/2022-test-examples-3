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
 * Date: 06.05.14
 * Time: 19:51
 * <p/>
 * [DARIA-4359]
 * [MAILPROTO-2202]
 */
@Aqua.Test
@Title("Команда CLOSE. Закрываем папки spam и deleted")
@Features({ImapCmd.CLOSE})
@Stories("#папки Spam и Deleted")
@Description("Проверяем реакцию на закрытие для папок \"Спам\" и \"Удаленные\" (удаление сообщений и т.д.) " +
        "Позитивное и негативное тестирование")
@RunWith(Parameterized.class)
public class CloseSpamAndDeleted extends BaseTest {
    private static Class<?> currentClass = CloseSpamAndDeleted.class;

    private static final int LEVEL_OF_HIERARCHY = 5;
    private static final int NUMBER_OF_MESSAGE = 10;
    @ClassRule
    public static ImapClient imap = newLoginedClient(currentClass);
    @Rule
    public ImapClient prodImap = withCleanBefore(newLoginedClient(currentClass));
    private String sysFolder;


    public CloseSpamAndDeleted(String sysFolder) {
        this.sysFolder = sysFolder;
    }

    @Parameterized.Parameters(name = "folder - {0}")
    public static Collection<Object[]> folders() {
        return with(
                systemFolders().getDeleted(),
                systemFolders().getSpam()
        ).convert(wrap());
    }

    @Description("Выбираем пустую системную папку. Закрываем папку => OK\n" +
            "Еще раз закрываем => BAD [MAILPROTO-2179]")
    @Stories(MyStories.JIRA)
    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("63")
    public void closeEmptyFolderAfterSelect() {
        imap.request(select(sysFolder)).repeatUntilOk(imap);
        imap.request(close()).shouldBeOk().shouldBeEmpty();
        imap.request(close()).shouldBeBad();
    }

    @Description("Выбираем пустую системную папку для чтения. Закрываем папку => OK\n" +
            "Еще раз закрываем => BAD [MAILPROTO-2179]")
    @Stories(MyStories.JIRA)
    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("64")
    public void closeEmptyFolderAfterExamine() {
        imap.request(examine(sysFolder)).repeatUntilOk(imap);
        imap.request(close()).shouldBeOk().shouldBeEmpty();
        imap.request(close()).shouldBeBad();
    }

    @Test
    @Description("Добавляем 1 письмо, ставим у него флаг /Deleted. Закрываем папку.\n" +
            "Письмо должно удалиться\n" +
            "MAILPROTO-2202")
    @Stories(MyStories.JIRA)
    @ru.yandex.qatools.allure.annotations.TestCaseId("65")
    public void closeWithDeletedOneMessageAfterSelect() throws Exception {
        prodImap.append().appendRandomMessage(sysFolder);
        prodImap.select().waitMsgs(sysFolder, 1);

        imap.request(select(sysFolder)).shouldBeOk().repeatUntilOk(imap);
        imap.store().deletedOnMessages(imap.search().allMessages());
        imap.request(close()).shouldBeOk();

        prodImap.status().numberOfMessagesShouldBe(sysFolder, 0);
    }

    @Test
    @Description("Добавляем 1 письмо, ставим у него флаг /Deleted. Закрываем папку.\n" +
            "Письмо НЕ должно удалиться")
    @ru.yandex.qatools.allure.annotations.TestCaseId("66")
    public void closeWithDeletedOneMessageAfterExamine() throws Exception {
        prodImap.append().appendRandomMessage(sysFolder);
        prodImap.select().waitMsgs(sysFolder, 1);

        prodImap.request(select(sysFolder)).shouldBeOk().repeatUntilOk(imap);
        prodImap.store().deletedOnMessages(imap.search().allMessages());
        prodImap.request(unselect()).shouldBeOk();

        imap.request(examine(sysFolder)).shouldBeOk();
        imap.request(close()).shouldBeOk().shouldBeEmpty();

        prodImap.select().waitMsgs(sysFolder, 1);
        prodImap.status().numberOfMessagesShouldBe(sysFolder, 1);
        //todo: хорошо бы проверить еще search-ем, что id не сбиваются....
    }

    @Test
    @Description("Закрываем папку со 10 сообщениями после выбора папки. Удаляем 10 сообщений в папке\n" +
            "MAILPROTO-2202")
    @Stories(MyStories.JIRA)
    @ru.yandex.qatools.allure.annotations.TestCaseId("67")
    public void closeWithManyDeletedMessagesAfterSelect() throws Exception {
        prodImap.append().appendManyRandomMessagesWithCopy(sysFolder, NUMBER_OF_MESSAGE);
        prodImap.select().waitMsgs(sysFolder, NUMBER_OF_MESSAGE);

        imap.request(select(sysFolder)).shouldBeOk().repeatUntilOk(imap);
        imap.store().deletedOnMessages(imap.search().allMessages());
        imap.request(close()).shouldBeOk();

        prodImap.select().waitMsgs(sysFolder, 0);
        prodImap.status().numberOfMessagesShouldBe(sysFolder, 0);
    }

    @Description("Закрываем папку со 10 сообщениями после выбора папки для чтения. Письма должны остаться")
    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("68")
    public void closeWithManyDeletedMessagesAfterExamine() throws Exception {
        prodImap.append().appendManyRandomMessagesWithCopy(sysFolder, NUMBER_OF_MESSAGE);
        prodImap.select().waitMsgs(sysFolder, NUMBER_OF_MESSAGE);

        prodImap.request(select(sysFolder)).shouldBeOk().repeatUntilOk(imap);
        prodImap.store().deletedOnMessages(prodImap.search().allMessages());
        prodImap.request(unselect()).shouldBeOk();

        //добавить проверку, что флаги подтянулись
        imap.request(examine(sysFolder)).shouldBeOk();
        imap.request(close()).shouldBeOk().shouldBeEmpty();

        prodImap.select().waitMsgs(sysFolder, NUMBER_OF_MESSAGE);
        prodImap.status().numberOfMessagesShouldBe(sysFolder, NUMBER_OF_MESSAGE);
    }

    //без флага: MessageFlags.FLAGGED.value()
    @Test
    @Description("Закрываем папку со 10 сообщениями со всеми флагами после выбора папки. Письма должны остаться")
    @ru.yandex.qatools.allure.annotations.TestCaseId("69")
    public void closeWithAllFlagsWithoutDeletedMessagesAfterSelect() throws Exception {
        prodImap.append().appendManyRandomMessagesWithCopy(sysFolder, NUMBER_OF_MESSAGE);
        prodImap.select().waitMsgs(sysFolder, NUMBER_OF_MESSAGE);

        imap.request(select(sysFolder)).shouldBeOk().repeatUntilOk(imap);
        imap.request(store(on(",").join(imap.search().allMessages()), StoreRequest.FLAGS, roundBraceList(MessageFlags.SEEN.value(),
                MessageFlags.ANSWERED.value(), MessageFlags.DRAFT.value()))).shouldBeOk();
        imap.request(close()).shouldBeOk().shouldBeEmpty();

        //todo: добавить проверки что на письмах флаги
        prodImap.select().waitMsgs(sysFolder, NUMBER_OF_MESSAGE);
        prodImap.status().numberOfMessagesShouldBe(sysFolder, NUMBER_OF_MESSAGE);
    }

    @Description("Закрываем папку со 10 сообщениями помеченными всеми возможными флагами после выбора папки для чтения.\n" +
            " Письма должны остаться")
    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("70")
    public void closeWithAllFlagsWithoutDeletedMessagesAfterExamine() throws Exception {
        prodImap.append().appendManyRandomMessagesWithCopy(sysFolder, NUMBER_OF_MESSAGE);
        prodImap.select().waitMsgs(sysFolder, NUMBER_OF_MESSAGE);

        prodImap.request(select(sysFolder)).shouldBeOk().repeatUntilOk(imap);
        prodImap.request(store(on(",").join(prodImap.search().allMessages()), StoreRequest.FLAGS,
                roundBraceList(MessageFlags.SEEN.value(), MessageFlags.ANSWERED.value(), MessageFlags.DRAFT.value())))
                .shouldBeOk();
        prodImap.request(unselect()).shouldBeOk();

        //добавить проверку, что флаги подтянулись
        imap.request(examine(sysFolder)).shouldBeOk();
        imap.request(close()).shouldBeOk().shouldBeEmpty();

        prodImap.select().waitMsgs(sysFolder, NUMBER_OF_MESSAGE);
        prodImap.status().numberOfMessagesShouldBe(sysFolder, NUMBER_OF_MESSAGE);
    }

    @Test
    @Description("Закрываем папку со 10 сообщениями после выбора папки для чтения.\n" +
            " Удаляем письма\n" +
            "[MAILPROTO-2202]")
    @Stories(MyStories.JIRA)
    @ru.yandex.qatools.allure.annotations.TestCaseId("71")
    public void closeWithAllFlagsMessagesAfterSelect() throws Exception {
        prodImap.append().appendManyRandomMessagesWithCopy(sysFolder, NUMBER_OF_MESSAGE);
        prodImap.select().waitMsgs(sysFolder, NUMBER_OF_MESSAGE);

        imap.request(select(sysFolder)).shouldBeOk().repeatUntilOk(imap);
        imap.request(store(on(",").join(imap.search().allMessages()), StoreRequest.FLAGS,
                roundBraceList(MessageFlags.SEEN.value(), MessageFlags.ANSWERED.value(), MessageFlags.DRAFT.value(),
                        MessageFlags.DELETED.value()))).shouldBeOk();

        imap.request(close()).shouldBeOk();

        //todo: добавить проверки что на письмах флаги
        prodImap.select().waitMsgs(sysFolder, 0);
        prodImap.status().numberOfMessagesShouldBe(sysFolder, 0);
    }

    @Test
    @Description("Закрываем папку со 10 сообщениями помеченными всеми возможными флагами после выбора папки для чтения.\n" +
            " Письма должны остаться")
    @ru.yandex.qatools.allure.annotations.TestCaseId("72")
    public void closeWithAllFlagsMessagesAfterExamine() throws Exception {
        prodImap.append().appendManyRandomMessagesWithCopy(sysFolder, NUMBER_OF_MESSAGE);
        prodImap.select().waitMsgs(sysFolder, NUMBER_OF_MESSAGE);

        prodImap.request(select(sysFolder)).shouldBeOk().repeatUntilOk(imap);
        prodImap.request(store(on(",").join(prodImap.search().allMessages()), StoreRequest.FLAGS,
                roundBraceList(MessageFlags.SEEN.value(), MessageFlags.ANSWERED.value(), MessageFlags.DRAFT.value(),
                        MessageFlags.DELETED.value()))).shouldBeOk();

        //добавить проверку, что флаги подтянулись
        imap.request(examine(sysFolder)).shouldBeOk();
        imap.request(close()).shouldBeOk().shouldBeEmpty();

        prodImap.select().waitMsgs(sysFolder, NUMBER_OF_MESSAGE);
        prodImap.status().numberOfMessagesShouldBe(sysFolder, NUMBER_OF_MESSAGE);
    }
}
