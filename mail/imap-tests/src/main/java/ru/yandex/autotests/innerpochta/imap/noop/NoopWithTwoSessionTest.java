package ru.yandex.autotests.innerpochta.imap.noop;

import java.util.Arrays;
import java.util.List;

import javax.mail.MessagingException;

import org.junit.Rule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.imap.base.BaseTest;
import ru.yandex.autotests.innerpochta.imap.consts.base.ImapCmd;
import ru.yandex.autotests.innerpochta.imap.consts.base.MyStories;
import ru.yandex.autotests.innerpochta.imap.core.imap.ImapClient;
import ru.yandex.autotests.innerpochta.imap.steps.SmtpSteps;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import static ru.yandex.autotests.innerpochta.imap.requests.ExpungeRequest.expunge;
import static ru.yandex.autotests.innerpochta.imap.requests.NoOpRequest.noOp;
import static ru.yandex.autotests.innerpochta.imap.rules.CleanRule.withCleanBefore;

/**
 * Created with IntelliJ IDEA.
 * User: vicdev
 * Date: 25.06.14
 * Time: 21:47
 * <p/>
 * [AUTOTESTPERS-134]
 * [MAILPROTO-2205]
 * [MAILPROTO-2210]
 */
@Aqua.Test
@Title("Команда NOOP. Делаем NOOP и EXPUNGE внутри другой сессии")
@Features({ImapCmd.NOOP})
@Stories({MyStories.TWO_SESSION})
@Description("Работа с письмами в двух сессиях параллельно")
public class NoopWithTwoSessionTest extends BaseTest {
    private static Class<?> currentClass = NoopWithTwoSessionTest.class;

    public static final int NUMBER_OF_MESSAGES = 5;


    private final SmtpSteps smtp = new SmtpSteps(currentClass.getSimpleName());

    @Rule
    public ImapClient prodImap = withCleanBefore(newLoginedClient(currentClass));

    @Rule
    public ImapClient imap = newLoginedClient(currentClass);

    @Rule
    public ImapClient imap2 = newLoginedClient(currentClass);

    @Test
    @Stories(MyStories.TWO_SESSION)
    @Description("Отправляем себе письмо проверяем, что письмо пришло в NOOP")
    @ru.yandex.qatools.allure.annotations.TestCaseId("322")
    public void noopWithSendLettersShouldSeeRecentAndExist() throws MessagingException {
        imap.select().inbox();
        smtp.subj("JavaMail hello world example1").text("Hello, world!\n").send();
        imap2.select().waitMsgsInInbox(1);
        imap.request(noOp()).existsShouldBe(1).recentShouldBe(1);
    }

    @Test
    @Title("Noop с двумя сессиями")
    @Stories({MyStories.TWO_SESSION, MyStories.HARD_CASES, MyStories.STARTREK, MyStories.JIRA})
    @Description("[AUTOTESTPERS-134] (2й кейс), [MAILPROTO-2210]")
    @ru.yandex.qatools.allure.annotations.TestCaseId("323")
    public void noopAfterExpungeShouldNotSeeLetters() throws Exception {
        imap.select().inbox();
        imap2.select().inbox();
        imap.append().appendRandomMessagesInInbox(NUMBER_OF_MESSAGES);
        List<String> allMsg = imap.search().allMessages();
        imap.store().deletedOnMessages(allMsg);

//        imap2.fetch().waitFlags("1", MessageFlags.DELETED.value());
        //здесь должны увидеть тольк 1 1 1 1 1 1
        imap2.request(expunge()).shouldBeOk().expungeShouldBe(1, 1, 1, 1, 1);

        imap2.request(noOp()).shouldBeOk().expungedShouldBeEmpty();
        //второй сессии должны узнать об изменениях
        imap.request(noOp()).shouldBeOk().expungeShouldBe(1, 1, 1, 1, 1);
    }

    @Description("Проверяем что после EXPUNGE вызов NOOP делает корректный status update")
    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("680")
    public void noopAfterExpunge() throws Exception {
        prodImap.append().appendRandomMessagesInInbox(2);
        prodImap.select().waitMsgsInInbox(2);

        imap.select().inbox();
        imap.noop().pullChanges();
        imap.store().deletedOnMessages(Arrays.asList("1"));
        imap.request(expunge()).shouldBeOk().expungeShouldBe(1);

        prodImap.request(noOp()).shouldBeOk().expungeShouldBe(1);
        prodImap.store().deletedOnMessages(Arrays.asList("1"));
        prodImap.request(expunge()).shouldBeOk().expungeShouldBe(1);
        imap.request(noOp()).shouldBeOk().expungeShouldBe(1);
    }
}
