package ru.yandex.autotests.innerpochta.tests.filters;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.aqua.annotations.project.Feature;
import ru.yandex.autotests.innerpochta.tests.unstable.TestMessage;
import ru.yandex.autotests.innerpochta.tests.unstable.User;
import ru.yandex.autotests.plugins.testpers.LogConfigRule;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import javax.mail.MessagingException;
import java.io.IOException;
import java.util.Collection;

import static java.util.Arrays.asList;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.hamcrest.Matchers.is;
import static org.junit.Assume.assumeThat;
import static ru.yandex.autotests.innerpochta.utils.MxConstants.*;
import static ru.yandex.autotests.innerpochta.utils.MxUtils.*;
import static ru.yandex.autotests.innerpochta.utils.WmiApiUtils.inMailbox;
import static ru.yandex.autotests.innerpochta.utils.MxTestProperties.mxTestProps;

/**
 * @author amois
 */
@Feature("Фильтры")
@Stories("FASTSRV")
@Aqua.Test(title = "Тестирование фильтров на автоответ, пересылку и удаление писем",
        description = "Тестирование фильтров на автоответ, пересылку и удаление писем")
@Title("Filters2Test. Тестирование фильтров на автоответ, пересылку и удаление писем")
@Description("Тестирование фильтров на автоответ, пересылку и удаление писем")
@RunWith(Parameterized.class)
public class Filters2Test {
    private static final String RANDOM_PREFIX = randomAlphanumeric(15);
    private static User sender = new User("filter-09-rcpt@yandex.ru", "12345678");
    private static User receiver = mxTestProps().isCorpServer() ?
            new User("filter-09@mail.yandex-team.ru", "SKjre2kmjdg")
            : new User("filter-09@yandex.ru", "testqa12345678");
    private Logger log = LogManager.getLogger(this.getClass());
    private TestMessage msg;

    @Parameterized.Parameter(0)
    public String msgSbj;
    @Parameterized.Parameter(1)
    public String msgText;
    @Parameterized.Parameter(2)
    public String targetFolder;
    @Parameterized.Parameter(3)
    public boolean isResend;
    @Parameterized.Parameter(4)
    public boolean isAutoAnswer;
    @Parameterized.Parameter(5)
    public String xYandexSpamHeaderValue;

    @Rule
    public LogConfigRule newAquaLogRule = new LogConfigRule();
    //@Rule
    //public SshConnectionRule connRule = new SshConnectionRule(mxTestProps().getNslsHost());

    @Parameterized.Parameters
    public static Collection<Object[]> data() throws Exception {
        return asList(new Object[]{"autoanswer_nospam", "nospam", PG_FOLDER_DEFAULT, false, true, "1"},
                new Object[]{"resend_nospam ", "nospam", PG_FOLDER_DEFAULT, true, false, "1"},
                new Object[]{"delete_spam ", SPAM_TEXT, PG_FOLDER_DELETED, false, false, "4"},
                new Object[]{"delete_nospam ", "Not Spam!", PG_FOLDER_DELETED, false, false, "1"},
                new Object[]{"resendNotSpam ", SPAM_TEXT, PG_FOLDER_SPAM, false, false, "4"},

                new Object[]{"resend_spam notRealSpam ", "Not SPAM!!", PG_FOLDER_DEFAULT, false, false, "1"},
                new Object[]{"resend_spam ", SPAM_TEXT, PG_FOLDER_SPAM, false, false, "4"},
                new Object[]{"delete_spam notRealSpam ", "Not SPAM!", PG_FOLDER_DEFAULT, false, false, "1"},
                new Object[]{"resend_nospam SPAM! ", SPAM_TEXT, PG_FOLDER_SPAM, false, false, "4"},
                new Object[]{"autoanswer_nospam SPAM! ", SPAM_TEXT, PG_FOLDER_SPAM, false, false, "4"},
                new Object[]{"resend_all spam ", SPAM_TEXT, PG_FOLDER_SPAM, false, false, "4"},

                new Object[]{"resend_all ham ", "Not spam!", PG_FOLDER_DEFAULT, true, false, "1"},
                new Object[]{"delete_all spam ", SPAM_TEXT, PG_FOLDER_DELETED, false, false, "4"},
                new Object[]{"delete_all not spam", "Not Spam!", PG_FOLDER_DELETED, false, false, "1"},
                new Object[]{"autoanswer_all not spam", "Not SPAM!", PG_FOLDER_DEFAULT, false, true, "1"});
    }

    @BeforeClass
    public static void clearUsers() {
        inMailbox(receiver).clearAll();
        inMailbox(sender).clearAll();
    }

    @Before
    public void prepareTest() throws IOException, MessagingException {
        msg = new TestMessage();
        msg.setRecipient(receiver.getLogin());
        msg.setFrom(sender.getLogin());
        msg.setSubject(msgSbj + RANDOM_PREFIX);
        msg.setText(msgText);
        msg.setHeader("X-Yandex-Spam", xYandexSpamHeaderValue);
        msg.saveChanges();
    }

    @Test
    public void testDeleteFiltersWithAutoAnswer() throws Exception {
        assumeThat("Не тестируем пересылку!", isResend, is(false));
        assumeThat("Тестируем автоответ!", isAutoAnswer, is(true));
        sendLetter();

        inMailbox(receiver).inFolder(targetFolder).shouldSeeLetterWithSubject(msg.getSubject());
        inMailbox(sender).inFolder(PG_FOLDER_DEFAULT).shouldSeeLetterWithSubject(msg.getSubject());
    }

    @Test
    public void testDeleteFiltersWithoutAutoAnswer() throws Exception {
        assumeThat("Не тестируем пересылку!", isResend, is(false));
        assumeThat("Не тестируем автоответ!", isAutoAnswer, is(false));
        sendLetter();

        inMailbox(receiver).inFolder(targetFolder).shouldSeeLetterWithSubject(msg.getSubject());
        inMailbox(sender).inFolder(PG_FOLDER_DEFAULT).shouldSeeLettersWithSubject(msg.getSubject(), 0);
    }

    @Test
    public void testResendFiltersWithoutAutoAnswer() throws Exception {
        assumeThat("Тестируем пересылку!", isResend, is(true));
        sendLetter();

        inMailbox(sender).shouldSeeLetterWithSubject(msg.getSubject());
        inMailbox(receiver).inFolder(PG_FOLDER_DELETED).shouldSeeLettersWithSubject(msg.getSubject(), 0);
        inMailbox(receiver).inFolder(PG_FOLDER_DEFAULT).shouldSeeLettersWithSubject(msg.getSubject(), 0);
        inMailbox(receiver).inFolder(PG_FOLDER_SPAM).shouldSeeLettersWithSubject(msg.getSubject(), 0);
    }

    public void sendLetter() throws Exception {
        log.info("Отправляем письмо с темой " + msg.getSubject());
        String serverResponse = sendByNsls(msg);
        //String sessionLog = getInfoFromNsls(connRule.getConn(), getMessageIdByServerResponse(serverResponse));
        log.info(serverResponse);
        //log.info(sessionLog);
    }
}
