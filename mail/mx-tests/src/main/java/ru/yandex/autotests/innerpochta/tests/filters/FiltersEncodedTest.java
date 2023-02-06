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

import java.util.Collection;
import java.util.LinkedList;

import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static ru.yandex.autotests.innerpochta.utils.MxUtils.sendByNwsmtp;
import static ru.yandex.autotests.innerpochta.utils.WmiApiUtils.inMailbox;
import static ru.yandex.autotests.innerpochta.utils.MxTestProperties.mxTestProps;

/**
 * @author amois
 */
@Stories("NWSMTP-FASTSRV")
@Feature("Фильтры")
@Aqua.Test(title = " Проверка фильтров для закодированных в base64 писем",
        description = "Отправляем письмо пользователю с содержимым, закодированным в base64 и удовлетворяющим " +
                "условиям фильтра. Проверяем, что фильтр сработал, не смотря на то, что содержимое было закадированно")
@Title("FiltersEncodedTest. Проверка фильтров для закодированных в base64 писем")
@Description("Отправляем письмо пользователю с содержимым, закодированным в base64 и удовлетворяющим " +
        "условиям фильтра. Проверяем, что фильтр сработал, не смотря на то, что содержимое было закадированно")
@RunWith(Parameterized.class)
public class FiltersEncodedTest {
    private static final User RECEIVER = new User("filtersencoded@yandex.ru", "12345678");
    private static final User SENDER = new User("yantester@yandex.ru", "12345678");
    private static String testServer = mxTestProps().getMxServer();
    private static int smtpPort = mxTestProps().getMxPort();
    private Logger log = LogManager.getLogger(this.getClass());
    private TestMessage msg;

    @Parameterized.Parameter(0)
    public String body;
    @Parameterized.Parameter(1)
    public String targetFolder;

    @Rule
    public LogConfigRule newAquaLogRule = new LogConfigRule();

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        Collection<Object[]> data = new LinkedList<Object[]>();
        data.add(new Object[]{"put to 1", "1"});
        data.add(new Object[]{"КЕЙСИНСЕНСИТИВ", "cAsE"});
        return data;
    }

    @BeforeClass
    public static void clearUser() {
        inMailbox(RECEIVER).clearAll();
    }

    @Before
    public void prepareTestMessage() throws Exception {
        //  System.setProperty("file.encoding", "UTF-8");
        msg = new TestMessage();
        msg.setFrom(SENDER.getLogin());
        msg.setSubject("FilrersEncoded2Test" + randomAlphanumeric(20));
        msg.setRecipient(RECEIVER.getLogin());
        msg.setContent(body, "text/plain; charset=UTF-8");
        msg.addHeader("Content-Transfer-Encoding", "base64");
        msg.saveChanges();
    }

    @Test
    public void shouldSeeThatFiltersWorkWithEncodedBody() throws Exception {
        log.info("Отправляем письмо " + msg.getSubject());
        sendByNwsmtp(msg, testServer, smtpPort, SENDER.getLogin(), SENDER.getPassword());
        log.info("Проверяем наличие письма в ящиках, в правильных папках");
        inMailbox(RECEIVER).inFolder(targetFolder).shouldSeeLetterWithSubject(msg.getSubject());
    }
}
