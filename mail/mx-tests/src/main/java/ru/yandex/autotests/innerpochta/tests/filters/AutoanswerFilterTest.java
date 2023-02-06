package ru.yandex.autotests.innerpochta.tests.filters;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.junit.Before;
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
import java.util.Collection;
import java.util.LinkedList;

import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static ru.yandex.autotests.innerpochta.tests.headers.HeadersData.HeaderNames.X_YANDEX_UNIQ;
import static ru.yandex.autotests.innerpochta.tests.matchers.MessageHeaderMatcher.hasHeader;
import static ru.yandex.autotests.innerpochta.utils.MxUtils.sendByNsls;
import static ru.yandex.autotests.innerpochta.utils.WmiApiUtils.inMailbox;
import static ru.yandex.autotests.innerpochta.utils.MxTestProperties.mxTestProps;

/**
 * User: alex89
 * Date: 22.04.14
 * У пользователей mxtest-01@mail.yandex-team.ru,
 * auto-answer-filter-test1@ya.ru,
 * auto-answer-filter-test1-comtr@galatasaray.net,
 * auto-answer-filter-test1-com@yandex.com,
 * test-auto-answer1@testtrest.yaconnect.com
 * настроен фильтр
 * Если «Тема» содержит «autoanswer»
 * — автоматический ответ «autoanswer автоответ text 1234567890!@>><<,*&?=+-$%^()_][}{||\\///`~""'':;.»
 * <p>
 * MAILPROTO-2004
 */

@Stories("FASTSRV")
@Feature("Фильтры")
@Aqua.Test(title = "Фильтры с автоответом",
        description = "Проверка работы фильтров на автоответ + контроль содержимого")
@Title("AutoanswerFilterTest. Фильтры с автоответом")
@Description("Проверка работы фильтров на автоответ + контроль содержимого")
@RunWith(Parameterized.class)
public class AutoanswerFilterTest {
    private static final int WAIT_TIMEOUT = 60; //sec.
    private static final String EXPECTED_FROM_CORP = "<mxtest-01@yandex-team.ru>";
    private static final String EXPECTED_FROM_FOR_KUBR_USER = "<auto-answer-filter-test1@yandex.ru>";
    private static final String EXPECTED_FROM_FOR_COM_USER = "<auto-answer-filter-test1-com@yandex.com>";
    private static final String EXPECTED_FROM_FOR_GL_USER = "<auto-answer-filter-test1-comtr@galatasaray.net>";
    private static final String EXPECTED_FROM_FOR_PDD_USER = "<test-auto-answer1@testtrest.yaconnect.com>";
    private static final String EXPECTED_AUTOANSWER_TEXT =
            "autoanswer автоответ text 1234567890!@>><<,*&?=+-$%^()_][}{||\\\\///`~\"\"'':;.";
    private static final User SENDER = mxTestProps().isCorpServer() ?
            new User("mxtest-02@mail.yandex-team.ru", "H@pl@x4-02") :
            new User("auto-answer-filter-test3@ya.ru", "testqa");
    private Logger log = LogManager.getLogger(this.getClass());
    private TestMessage msg;

    @Parameterized.Parameter(0)
    public User receiver;
    @Parameterized.Parameter(1)
    public String expectedFromInAutoAnswerMsg;

    @Rule
    public LogConfigRule newAquaLogRule = new LogConfigRule();

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        Collection<Object[]> data = new LinkedList<Object[]>();
        if (mxTestProps().isCorpServer()) {
            data.add(new Object[]{new User("mxtest-01@mail.yandex-team.ru", "H@pl@x4-01"), EXPECTED_FROM_CORP});
            data.add(new Object[]{new User("mxtest-01@mail.yandex-team.com", "H@pl@x4-01"), EXPECTED_FROM_CORP});
            data.add(new Object[]{new User("mxtest-01@mail.yandex-team.com.tr", "H@pl@x4-01"), EXPECTED_FROM_CORP});
        } else {
            data.add(new Object[]{new User("auto-answer-filter-test1@ya.ru", "testqa"), EXPECTED_FROM_FOR_KUBR_USER});
            data.add(new Object[]{new User("auto-answer-filter-test1@yandex.ru", "testqa"),
                    EXPECTED_FROM_FOR_KUBR_USER});
            data.add(new Object[]{new User("auto-answer-filter-test1@yandex.by", "testqa"),
                    EXPECTED_FROM_FOR_KUBR_USER});
            data.add(new Object[]{new User("auto-answer-filter-test1@yandex.ua", "testqa"),
                    EXPECTED_FROM_FOR_KUBR_USER});
            data.add(new Object[]{new User("auto-answer-filter-test1@yandex.com", "testqa"),
                    EXPECTED_FROM_FOR_KUBR_USER});
            data.add(new Object[]{new User("auto-answer-filter-test1@yandex.com.tr", "testqa"),
                    EXPECTED_FROM_FOR_KUBR_USER});
            data.add(new Object[]{new User("auto-answer-filter-test1@narod.ru", "testqa"),
                    EXPECTED_FROM_FOR_KUBR_USER});
            data.add(new Object[]{new User("auto-answer-filter-test1-com@yandex.com", "testqa"),
                    EXPECTED_FROM_FOR_COM_USER});
            data.add(new Object[]{new User("auto-answer-filter-test1-comtr@yandex.com", "testqa"),
                    EXPECTED_FROM_FOR_GL_USER});
            data.add(new Object[]{new User("auto-answer-filter-test1-comtr@yandex.com.tr", "testqa"),
                    EXPECTED_FROM_FOR_GL_USER});
            data.add(new Object[]{new User("auto-answer-filter-test1-comtr@galatasaray.net", "testqa"),
                    EXPECTED_FROM_FOR_GL_USER});
            data.add(new Object[]{new User("test-auto-answer1@testtrest.yaconnect.com", "testqa12345678"),
                    EXPECTED_FROM_FOR_PDD_USER});
        }
        return data;
    }

    @Before
    public void prepareTestMessage() throws FileNotFoundException, MessagingException {
        inMailbox(receiver).clearDefaultFolder();
        String randomStr = randomAlphanumeric(20);
        this.msg = new TestMessage();
        msg.setSubject("autoanswer " + randomStr);
        msg.setHeader(X_YANDEX_UNIQ.getName(), randomStr);
        msg.setFrom(SENDER.getLogin());
        msg.setText("Message with " + randomStr);
        msg.setRecipient(receiver.getLogin());
        msg.saveChanges();
    }

    @Test
    public void testCheckOriginAndAutoanswerMsg() throws Exception {
        log.info("Отправляем письмо");
        sendByNsls(msg);
        log.info("Проверяем наличие письма в ящике с фильтром.");
        inMailbox(receiver).shouldSeeLetterWithSubject(msg.getSubject(), WAIT_TIMEOUT);
        log.info("Проверяем наличие автоответа.");
        inMailbox(SENDER).shouldSeeLetterWithSubject(msg.getSubject(), WAIT_TIMEOUT);
        TestMessage receivedMsg = inMailbox(SENDER).getMessageWithSubject(msg.getSubject());
        assertThat("Неверное значение From [MAILPROTO-2004]!", receivedMsg,
                hasHeader("From", containsString(expectedFromInAutoAnswerMsg)));
        assertThat("Неверное содержание автоответа!", (String) receivedMsg.getContent(),
                containsString(EXPECTED_AUTOANSWER_TEXT));
    }
}

