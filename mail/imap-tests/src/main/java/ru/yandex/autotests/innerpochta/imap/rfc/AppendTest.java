package ru.yandex.autotests.innerpochta.imap.rfc;

import org.junit.Rule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.imap.base.BaseTest;
import ru.yandex.autotests.innerpochta.imap.consts.folders.Folders;
import ru.yandex.autotests.innerpochta.imap.core.imap.ImapClient;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import static ru.yandex.autotests.innerpochta.imap.requests.AppendRequest.append;
import static ru.yandex.autotests.innerpochta.imap.rules.CleanRule.withCleanBefore;
import static ru.yandex.autotests.innerpochta.imap.utils.Utils.literal;

@Aqua.Test
@Title("Запрос APPEND")
@Features({"RFC"})
@Stories("6.3.11 APPEND")
@Description("http://tools.ietf.org/html/rfc3501#section-6.3.11")
public class AppendTest extends BaseTest {
    private static Class<?> currentClass = AppendTest.class;


    private static final String MESSAGE =
            "Date: Mon, 7 Feb 1994 21:52:25 -0800 (PST)\n" +
                    "From: Fred Foobar <foobar@Blurdybloop.COM>\n" +
                    "Subject: afternoon meeting\n" +
                    "To: mooch@owatagu.siam.edu\n" +
                    "Message-Id: <B27397-0100000@Blurdybloop.COM>\n" +
                    "MIME-Version: 1.0\n" +
                    "Content-Type: TEXT/PLAIN; CHARSET=US-ASCII\n" +
                    "\n" +
                    "Hello Joe, do you think we can meet at 3:30 tomorrow?\n";
    @Rule
    public ImapClient imap = withCleanBefore(newLoginedClient(currentClass));

    @Test
    @Issue("MAILPROTO-2186")
    @Title("Реакция на команду APPEND [MAILPROTO-2186]")
    @ru.yandex.qatools.allure.annotations.TestCaseId("423")
    public void testSimpleAppend() {
        imap.request(append(Folders.INBOX, literal(MESSAGE))).shouldBeOk();
        imap.select().waitMsgsInInbox(1);
    }
}
