package ru.yandex.autotests.innerpochta.pop3.rfc;

import java.io.IOException;

import javax.mail.MessagingException;

import org.apache.commons.net.pop3.POP3Client;
import org.apache.commons.net.pop3.POP3MessageInfo;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.imap.anno.Web;
import ru.yandex.autotests.innerpochta.imap.consts.base.MyStories;
import ru.yandex.autotests.innerpochta.imap.consts.base.Pop3Cmd;
import ru.yandex.autotests.innerpochta.imap.core.imap.ImapClient;
import ru.yandex.autotests.innerpochta.imap.core.pop3.Pop3Client;
import ru.yandex.autotests.innerpochta.pop3.base.BaseTest;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Severity;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;
import ru.yandex.qatools.allure.model.SeverityLevel;

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.innerpochta.imap.config.ImapProperties.props;
import static ru.yandex.autotests.innerpochta.imap.rules.CleanRule.withCleanBefore;
import static ru.yandex.qatools.matchers.collection.HasSameItemsAsListMatcher.hasSameItemsAsList;

/**
 * Created with IntelliJ IDEA.
 * User: vicdev
 * Date: 10.09.14
 * Time: 19:54
 * <p/>
 * https://ru.wikipedia.org/wiki/POP3
 * <p/>
 * AUTOTESTPERS-141
 */
@Aqua.Test
@Title("Авторизируемся через POP3 c SSL и без. Общие тесты на все комманды")
@Features({MyStories.POP3, MyStories.RFC})
@Stories({Pop3Cmd.USER, Pop3Cmd.PASS, Pop3Cmd.QUIT, Pop3Cmd.TOP})
@Description("Проверяем все операции для POP3")
@Issue("AUTOTESTPERS-141")
@Web
public class RFCPop3Test extends BaseTest {

    public static Integer COUNT_LETTERS_IN_INBOX = 2;

    @ClassRule
    public static ImapClient prodImap = withCleanBefore(imap(LOGIN_GROUP));
    @Rule
    public Pop3Client pop3 = new Pop3Client().pop3(LOGIN_GROUP);
    @Rule
    public Pop3Client prodPop3 = new Pop3Client().pop3(LOGIN_GROUP);

    @BeforeClass
    public static void prepareData() throws Exception {
        prodImap.append().appendRandomMessagesInInbox(COUNT_LETTERS_IN_INBOX);
        //пока проверяем только, что сообщение дошло
        prodImap.select().waitMsgsInInbox(COUNT_LETTERS_IN_INBOX);
    }

    @Test
    @Stories({MyStories.JIRA, MyStories.SSL})
    @Issue("MAILPROTO-2329")
    @Description("Пытаем авторизироваться без использования SSL [MAILPROTO-2329]")
    @ru.yandex.qatools.allure.annotations.TestCaseId("670")
    public void loginWithoutSSL() throws IOException {
        //создаем клиент без поддержки SSL соединения
        POP3Client pop3 = new POP3Client();
        pop3.connect(props().getHost(), 110);
        assertFalse("Ожидали что не сможем подключиться без SSL", pop3.login(props().account(LOGIN_GROUP).getLogin(),
                props().account(LOGIN_GROUP).getPassword()));
    }

    @Test
    @Stories(Pop3Cmd.NOOP)
    @Severity(SeverityLevel.BLOCKER)
    @ru.yandex.qatools.allure.annotations.TestCaseId("672")
    public void testNoop() throws IOException {
        pop3.noop(true);
    }

    @Test
    @Stories({Pop3Cmd.STAT, MyStories.B2B})
    @ru.yandex.qatools.allure.annotations.TestCaseId("671")
    public void testStat() throws IOException {
        POP3MessageInfo messageInfo = pop3.stat();
        POP3MessageInfo prodMessageInfo = prodPop3.stat();
        assertThat("Неправильное количество писем в ящике", messageInfo.number, equalTo(COUNT_LETTERS_IN_INBOX));
        assertThat("Неправильный общий размер писем в ящике", messageInfo.size,
                allOf(equalTo(prodMessageInfo.size), not(equalTo(0))));
    }

    @Test
    @Stories({Pop3Cmd.LIST, MyStories.B2B})
    @ru.yandex.qatools.allure.annotations.TestCaseId("673")
    public void testList() throws IOException, MessagingException {
        //в POP3 нельзя подтянуть вновь прибышие сообщения, поэтому можно только проверять, что сообщения на месте
        POP3MessageInfo[] messages = pop3.list();
        POP3MessageInfo[] prodMessages = prodPop3.list();
        assertThat("В ящике не 2 письма", messages.length, equalTo(COUNT_LETTERS_IN_INBOX));

        assertThat("Неправильный размер первого письма", messages[0].size, allOf(equalTo(prodMessages[0].size),
                not(equalTo(0))));
        assertThat("Неправильный размер второго письма", messages[1].size, allOf(equalTo(prodMessages[1].size),
                not(equalTo(0))));

        POP3MessageInfo message = pop3.list(messages[0].number);
        POP3MessageInfo prodMessage = pop3.list(messages[0].number);
        assertThat("Неправильный размер письма", message.size, allOf(equalTo(prodMessage.size), not(equalTo(0))));
        assertThat("Неправильный номер письма", message.number, equalTo(1));
    }

    @Test
    @Stories({Pop3Cmd.STAT, Pop3Cmd.LIST, MyStories.B2B})
    @Issue("MPROTO-1440")
    @Description("Проверяем, что STAT и LIST возвращает реальный размер письма")
    @ru.yandex.qatools.allure.annotations.TestCaseId("674")
    public void testListAndStatSize() throws IOException {
        POP3MessageInfo[] messages = pop3.list();
        assertThat("В ящике не 2 письма", messages.length, equalTo(COUNT_LETTERS_IN_INBOX));
        assertThat("Неправильный размер первого письма", messages[0].size, equalTo(pop3.getSizeMessage(1)));

        POP3MessageInfo messageInfo = pop3.stat();
        assertThat("Неправильный общий размер писем в ящике", messageInfo.size,
                allOf(equalTo(pop3.getSizeMessage(1) + pop3.getSizeMessage(2)), not(equalTo(0))));

    }

    @Test
    @Severity(SeverityLevel.BLOCKER)
    @Stories({Pop3Cmd.RETR, Pop3Cmd.DELE, MyStories.B2B})
    @ru.yandex.qatools.allure.annotations.TestCaseId("675")
    public void testRetr() throws IOException {
        assertThat("Тело первого письма различается с продакшеном", pop3.retr(1),
                hasSameItemsAsList(prodPop3.retr(1)));
    }

    @Test
    @Severity(SeverityLevel.BLOCKER)
    @Stories({Pop3Cmd.TOP, MyStories.B2B})
    @ru.yandex.qatools.allure.annotations.TestCaseId("676")
    public void testTop() throws IOException {
        assertThat("Тело первого письма (10 строк) различается с продакшеном", pop3.top(1, 10),
                hasSameItemsAsList(prodPop3.top(1, 10)));
    }

    @Test
    @Severity(SeverityLevel.BLOCKER)
    @Stories(Pop3Cmd.RSET)
    @Description("Проверка, что письмо удаляется в ArchiveTest")
    @ru.yandex.qatools.allure.annotations.TestCaseId("677")
    public void testRset() throws IOException {
        pop3.dele(1, true);
        assertThat("Сообщение НЕ пометилось удаленным после DELE", pop3.stat().number, equalTo(1));
        pop3.rset(true);
        assertThat("RST не снял метку удаленные", pop3.stat().number, equalTo(COUNT_LETTERS_IN_INBOX));
    }

    @Test
    @Severity(SeverityLevel.BLOCKER)
    @ru.yandex.qatools.allure.annotations.TestCaseId("678")
    public void deleNotExist() throws IOException {
        pop3.dele(1000000, false);
        assertThat("В ящике не 2 письма", pop3.stat().number, equalTo(2));
    }

}

