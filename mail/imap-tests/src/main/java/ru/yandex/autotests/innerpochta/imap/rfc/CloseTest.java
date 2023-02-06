package ru.yandex.autotests.innerpochta.imap.rfc;

import java.util.List;

import javax.mail.MessagingException;

import org.junit.Rule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.imap.base.BaseTest;
import ru.yandex.autotests.innerpochta.imap.consts.base.MyStories;
import ru.yandex.autotests.innerpochta.imap.core.imap.ImapClient;
import ru.yandex.autotests.innerpochta.imap.steps.SmtpSteps;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import static ru.yandex.autotests.innerpochta.imap.requests.CloseRequest.close;
import static ru.yandex.autotests.innerpochta.imap.requests.FetchRequest.fetch;
import static ru.yandex.autotests.innerpochta.imap.requests.SearchRequest.search;
import static ru.yandex.autotests.innerpochta.imap.requests.SelectRequest.select;
import static ru.yandex.autotests.innerpochta.imap.utils.Utils.generateName;

@Aqua.Test
@Title("Запрос CLOSE")
@Features({"RFC"})
@Stories("6.4.2 CLOSE")
@Description("http://tools.ietf.org/html/rfc3501#section-6.4.2")
public class CloseTest extends BaseTest {
    private static Class<?> currentClass = CloseTest.class;
    private final SmtpSteps smtp = new SmtpSteps(currentClass.getSimpleName());
    @Rule
    public ImapClient imap = newLoginedClient(currentClass);

    @Test
    @Title("Должны успешно закрыть папку после ее выбора")
    @ru.yandex.qatools.allure.annotations.TestCaseId("428")
    public void testCloseSucceeds() {
        imap.request(select("inbox")).shouldBeOk();
        imap.request(close()).shouldBeOk();
    }

    @Test
    @Title("Сообщение с флагом \\DELETED должно удалиться после CLOSE")
    @ru.yandex.qatools.allure.annotations.TestCaseId("430")
    public void messagesWithDeletedFlagShouldBeDeletedAfterClose() throws MessagingException {
        imap.request(select("INBOX")).shouldBeOk();
        smtp.subj("close" + generateName()).text("test").send();
        imap.messageShouldBeReceived();

        List<String> lettersToDelete = imap.request(search().all()).shouldBeOk().getMessages();

        imap.store().deletedOnMessages(lettersToDelete);

        imap.request(close()).shouldBeOk();

        imap.request(select("inbox")).shouldBeOk();
        imap.noop().pullChanges(); // Чтобы избежать standby
        imap.request(search().all()).shouldBeOk().shouldBeEmpty();
    }

    @Test
    @Title("Можем зафетчить письмо")
    @Description("[MAILPROTO-1986]")
    @Stories(MyStories.JIRA)
    @ru.yandex.qatools.allure.annotations.TestCaseId("429")
    public void testSimpleFetch() throws MessagingException {
        imap.select().inbox();
        smtp.subj("fetch" + generateName()).text("test").send();
        imap.messageShouldBeReceived();
        imap.request(fetch("1").body()).shouldBeOk();
    }
}
