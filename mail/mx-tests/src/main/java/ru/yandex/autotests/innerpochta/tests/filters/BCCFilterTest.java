package ru.yandex.autotests.innerpochta.tests.filters;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.Ignore;
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

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import java.io.UnsupportedEncodingException;
import java.util.Collection;

import static java.util.Arrays.asList;
import static javax.mail.Message.RecipientType.TO;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static ru.yandex.autotests.innerpochta.utils.MxConstants.PG_FOLDER_DEFAULT;
import static ru.yandex.autotests.innerpochta.utils.MxUtils.sendMessageByNsls;
import static ru.yandex.autotests.innerpochta.utils.WmiApiUtils.inMailbox;

/**
 * @author stassiak
 *         bcc-filter-test1.user - создан фильтр:
 *         если письмо содержит в BCC "user@yandex.", то положить письмо в папку "BCCFilter"
 *         <p>
 *         bcc-filter-test2.user - создан фильтр:
 *         если у письма BCC совпадает с "bcc-filter-test2.user@yandex.ru", то положить письмо в папку "BCCFilter2"
 */
@Stories("FASTSRV")
@Feature("Фильтры")
@Aqua.Test(title = "Тестирование фильтров с BCC заголовком в условии",
        description = "Тестирование филтров с BCC в условии фильтра")
@Title("BCCFilterTest.Тестирование фильтров с BCC заголовком в условии")
@Description("Тестирование фильтров с BCC заголовком в условии")
@RunWith(Parameterized.class)
public class BCCFilterTest {
    private static final User RECEIVER_MAIN = new User("yantester@yandex.ru", "12345678");
    private static final User RECEIVER1 = new User("bcc-filter-test1.user@yandex.ru", "12345678");
    private static final User RECEIVER2 = new User("bcc-filter-test2.user@yandex.ru", "12345678");
    private static final String RECEIVER1_FOLDER = "BCCFilter";
    private static final String RECEIVER2_FOLDER = "BCCFilter2";
    private Logger log = LogManager.getLogger(this.getClass());
    private TestMessage msg;

    @Parameterized.Parameter(0)
    public User toHeaderValue;
    @Parameterized.Parameter(1)
    public String bccHeaderValue;
    @Parameterized.Parameter(2)
    public String targetFolderOfReceiver1;
    @Parameterized.Parameter(3)
    public String targetFolderOfReceiver2;

    @Rule
    public LogConfigRule newAquaLogRule = new LogConfigRule();
    //@Rule
    //public SshConnectionRule sshConnectionRule = new SshConnectionRule(mxTestProps().getNslsHost());

    @Parameterized.Parameters
    public static Collection<Object[]> data() throws UnsupportedEncodingException, AddressException {
        return asList(
                new Object[]{RECEIVER_MAIN, "bcc-filter-test1.user@yandex.com, bcc-filter-test2.user@yandex.ua",
                        RECEIVER1_FOLDER, PG_FOLDER_DEFAULT},
                new Object[]{RECEIVER_MAIN, "bcc-filter-test1.user@ya.ru, bcc-filter-test2.user@ya.ru",
                        PG_FOLDER_DEFAULT, PG_FOLDER_DEFAULT},
                new Object[]{RECEIVER1, "bcc-filter-test2.user@yandex.ru", PG_FOLDER_DEFAULT, RECEIVER2_FOLDER},
                new Object[]{RECEIVER2, "bcc-filter-test1.user@yandex.com", RECEIVER1_FOLDER, PG_FOLDER_DEFAULT},
                new Object[]{RECEIVER2, "bcc-filter-test1.user@ya.ru", PG_FOLDER_DEFAULT, PG_FOLDER_DEFAULT});
    }

    @Before
    public void prepareTestMessage() throws Exception {
        inMailbox(RECEIVER1).clearAll();
        inMailbox(RECEIVER2).clearAll();
        msg = new TestMessage();
        msg.setSubject("BCCFilterTest" + randomAlphanumeric(20));
        msg.setRecipient(TO, new InternetAddress(toHeaderValue.getLogin()));
        msg.addHeader("BCC", bccHeaderValue);
        msg.setFrom("devnull@yandex.ru");
        msg.setText("Message with " + randomAlphanumeric(20));
        msg.saveChanges();
    }

    @Test
    @Ignore("MAILDLV-3393,MAILDLV-3297")
    public void shouldSeeThatBCCFiltersWork() throws Exception {
        log.info("Отправляем письмо " + msg.getSubject());
        String messageId = sendMessageByNsls(msg);
        //getInfoFromNsls(sshConnectionRule.getConn(), messageId);
        log.info("Проверяем наличие письма в ящиках, в правильных папках");
        inMailbox(RECEIVER1).inFolder(targetFolderOfReceiver1).shouldSeeLetterWithSubject(msg.getSubject());
        inMailbox(RECEIVER2).inFolder(targetFolderOfReceiver2).shouldSeeLetterWithSubject(msg.getSubject());
    }
}
