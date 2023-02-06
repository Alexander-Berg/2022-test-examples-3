package ru.yandex.autotests.innerpochta.tests.filters;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.junit.*;
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
import javax.mail.Session;
import javax.mail.internet.MimeMessage;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import static java.lang.Thread.currentThread;
import static javax.mail.Message.RecipientType.TO;
import static ru.yandex.autotests.innerpochta.utils.MxUtils.*;
import static ru.yandex.autotests.innerpochta.utils.WmiApiUtils.inMailbox;
import static ru.yandex.qatools.elliptics.ElClient.elliptics;

/**
 *  
 *
 * @author stassiak
 *         <p>
 *         53 письма лежит в элиптиксе.
 *         В письмах уже прописаны получатели (либо mxfilter-test-01@yandex.ru,mxfilter-test-02@yandex.ru)
 *         У этих получателей уже настроены фильтры.
 *         В каждом письме есть:
 *         Autotest-Header - комментарий расшифровка о том, куда должно отфильтроваться письмо;
 *         Autotest-Folder - папка, куда мы это письмо должно отфильтроваться;
 *         Autotest-Spam - спамовое письмо или нет.
 *         <p>
 *         Также у писем варьируется текст (спамовый или неспамовый текст), аттачи(есть или нет).
 *         К сожалению, много совпадающих тем, поэтому в тесте мы чистим ящик всякий раз, когда делаем отправку.
 *         <p>
 */
@Stories("FASTSRV")
@Feature("Фильтры")
@Aqua.Test(title = "Тестирование фильтров",
        description = "Tестирование фильтров со спамом в условиях")
@Title("FiltersTest. Тестирование фильтров")
@Description("Tестирование фильтров со спамом в условиях")
@RunWith(Parameterized.class)
public class FiltersTest {
    private static final String PWD = "testqa";
    private  Logger log = LogManager.getLogger(this.getClass());
    private String expectedFolder;
    private String comment;
    private User rcpt;
    private TestMessage msg;

    @Parameterized.Parameter(0)
    public String fName;

    //@ClassRule
    //public static SshConnectionRule connRule = new SshConnectionRule(mxTestProps().getNslsHost());
    @Rule
    public LogConfigRule newAquaLogRule = new LogConfigRule();

    @Parameterized.Parameters
    public static Collection<Object[]> data() throws Exception {
        File list = new File(currentThread().getContextClassLoader().getResource("spamfilter.list").getFile());
        Collection<Object[]> data = new LinkedList<Object[]>();
        for (String fName : (List<String>) FileUtils.readLines(list)) {
            data.add(new Object[]{fName});
        }
        return data;
    }

    @Before
    public void prepareTest() throws IOException, MessagingException {
        msg = new TestMessage(new MimeMessage(Session.getDefaultInstance(new Properties()),
                elliptics().indefinitely().path(FiltersTest.class).name(fName).get().asStream()));
        msg.saveChanges();
        comment = msg.getHeader("Autotest-Header")[0];
        expectedFolder = msg.getHeader("Autotest-Folder")[0].replace("INBOX", "Inbox");
        rcpt = new User(msg.getRecipients(TO)[0].toString(), PWD);
        inMailbox(rcpt).clearAll();

    }

    @Test
    public void shouldSeeSpamFiltersAction() throws Exception {
        log.info(comment);
        String messageId = getMessageIdByServerResponse(
                sendByNsls(msg));
        //log.info(getInfoFromNsls(connRule.getConn(), messageId));
        inMailbox(rcpt).inFolder(expectedFolder).shouldSeeLetterWithSubject(msg.getSubject());
    }
}
