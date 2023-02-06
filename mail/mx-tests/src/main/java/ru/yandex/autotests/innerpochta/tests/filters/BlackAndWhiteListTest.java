/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collection;

import static java.lang.Thread.sleep;
import static java.util.Arrays.asList;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static ru.yandex.autotests.innerpochta.tests.headers.HeadersData.HeaderNames.X_YANDEX_SPAM;
import static ru.yandex.autotests.innerpochta.utils.MxUtils.sendByNsls;
import static ru.yandex.autotests.innerpochta.utils.WmiApiUtils.inMailbox;

/**
 * @author amois
 *         В черном списке - userinblacklist@yandex.ru
 *         В белом списке - userinwhitelist@yandex.ru
 */
@Stories("FASTSRV")
@Feature("Фильтры")
@Aqua.Test(title = "Тестирование белого и черного списка", description = "Тестирование белого и черного списка")
@Title("BlackAndWhiteListTest.Тестирование белого и черного списка")
@Description("Тестирование белого и черного списка")
@RunWith(Parameterized.class)
public class BlackAndWhiteListTest {
    private static final String SPAM_STRING =
            "XJS*C4JDBQADN1.NSBN3*2IDNEN*GTUBE-STANDARD-ANTI-UBE-TEST-EMAIL*C.34X";
    private static final User BLACK_LIST_USER = new User("userinblacklist@yandex.ru", "12345678");
    private static final User WHITE_LIST_USER = new User("userinwhitelist@yandex.ru", "12345678");
    private static final User USER_WITH_FILTERS = new User("blackandwhitetest@yandex.ru", "12345678");
    private static final User PDD_USER_WITH_FILTERS =
            new User("blackandwhitetest@testtrest.yaconnect.com", "testqa12345678");
    private static final int ADDITIONAL_DELAY = 40000;
    private Logger log = LogManager.getLogger(this.getClass());
    private TestMessage msg;

    @Parameterized.Parameter(0)
    public User receiver;

    @Rule
    public LogConfigRule newAquaLogRule = new LogConfigRule();

    @Parameterized.Parameters
    public static Collection<Object[]> data() throws Exception {
        return asList(new Object[]{USER_WITH_FILTERS}, new Object[]{PDD_USER_WITH_FILTERS});
    }

    @BeforeClass
    public static void clearTestUser() throws Exception {
        inMailbox(USER_WITH_FILTERS).clearDefaultFolder();
        inMailbox(PDD_USER_WITH_FILTERS).clearDefaultFolder();
    }

    @Before
    public void prepareTestMessage() throws FileNotFoundException, MessagingException {
        this.msg = new TestMessage();
        msg.setText("Message text " + randomAlphanumeric(20));
        msg.setRecipient(receiver.getLogin());
        msg.saveChanges();
    }

    @Test
    public void shouldSeeHamLetterInInboxFromWhiteListUser()
            throws IOException, MessagingException {
        msg.setFrom(WHITE_LIST_USER.getLogin());
        msg.setSubject("testWhiteList " + randomAlphanumeric(20));
        msg.saveChanges();
        log.info("Отправляем письмо пользователю из белого списка");
        sendByNsls(msg);
        inMailbox(receiver).shouldSeeLetterWithSubject(msg.getSubject());
    }

    @Test
    public void shouldNotSeeHamLetterInInboxFromBlackListUser()
            throws MessagingException, IOException, InterruptedException {
        msg.setFrom(BLACK_LIST_USER.getLogin());
        msg.setSubject("testBlackList " + randomAlphanumeric(20));
        msg.saveChanges();
        log.info("Отправляем письмо пользователю из черного списка");
        sendByNsls(msg);
        sleep(ADDITIONAL_DELAY);
        inMailbox(receiver).shouldNotSeeLetterWithSubject(msg.getSubject());
    }

    @Test
    public void shouldSeeSpamLetterInInboxFromWhiteListUser()
            throws MessagingException, IOException {
        msg.setFrom(WHITE_LIST_USER.getLogin());
        msg.setText(SPAM_STRING);
        msg.setSubject("testWhiteListSpam " + randomAlphanumeric(20));
        msg.setHeader(X_YANDEX_SPAM.getName(), "4");
        msg.saveChanges();
        log.info("Отправляем СПАМОВОЕ письмо пользователю из белого списка");
        sendByNsls(msg);
        inMailbox(receiver).shouldSeeLetterWithSubject(msg.getSubject());
    }

    @Test
    public void shouldSeeSpamLetterInSpamFolderFromBlackListUser()
            throws MessagingException, IOException, InterruptedException {
        msg.setFrom(BLACK_LIST_USER.getLogin());
        msg.setText(SPAM_STRING);
        msg.setSubject("testWhiteListSpam " + randomAlphanumeric(20));
        msg.setHeader(X_YANDEX_SPAM.getName(), "4");
        msg.saveChanges();
        log.info("Отправляем СПАМОВОЕ письмо пользователю из черного списка");
        sendByNsls(msg);
        sleep(ADDITIONAL_DELAY);
        inMailbox(receiver).shouldNotSeeLetterWithSubject(msg.getSubject());
    }
}
