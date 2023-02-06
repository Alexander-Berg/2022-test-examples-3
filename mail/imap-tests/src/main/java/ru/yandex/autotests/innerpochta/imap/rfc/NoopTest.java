package ru.yandex.autotests.innerpochta.imap.rfc;

import javax.mail.MessagingException;

import org.junit.Rule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.imap.base.BaseTest;
import ru.yandex.autotests.innerpochta.imap.core.imap.ImapClient;
import ru.yandex.autotests.innerpochta.imap.steps.SmtpSteps;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import static ru.yandex.autotests.innerpochta.imap.requests.NoOpRequest.noOp;
import static ru.yandex.autotests.innerpochta.imap.requests.SelectRequest.select;
import static ru.yandex.autotests.innerpochta.imap.rules.CleanRule.withCleanBefore;

@Aqua.Test
@Title("Запрос NOOP")
@Features({"RFC"})
@Stories("6.1.2 NOOP")
@Description("http://tools.ietf.org/html/rfc3501#section-6.1.2")
public class NoopTest extends BaseTest {
    private static Class<?> currentClass = NoopTest.class;

    private final SmtpSteps smtp = new SmtpSteps(currentClass.getSimpleName());

    @Rule
    public ImapClient imap = withCleanBefore(newLoginedClient(currentClass));

    @Test
    @Title("NOOP должен вернуть OK")
    @ru.yandex.qatools.allure.annotations.TestCaseId("448")
    public void testNoopSucceeds() {
        imap.request(noOp()).shouldBeOk();
    }

    @Test
    @Title("NOOP должен сообщить о новых письмах")
    @ru.yandex.qatools.allure.annotations.TestCaseId("449")
    public void testNoopReturnsUpdates() throws MessagingException {
        imap.request(select("INBOX"));
        smtp.subj("JavaMail hello world example1").text("Hello, world!\n").send();
        imap.messageShouldBeReceived();
    }

}
