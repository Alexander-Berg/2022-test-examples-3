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

import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Collection;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static org.apache.commons.lang.RandomStringUtils.randomAlphanumeric;
import static ru.yandex.autotests.innerpochta.utils.MxConstants.PG_FOLDER_DEFAULT;
import static ru.yandex.autotests.innerpochta.utils.MxUtils.*;
import static ru.yandex.autotests.innerpochta.utils.WmiApiUtils.inMailbox;

/**
 * User: alex89
 * Date: 18.05.2015
 * https://st.yandex-team.ru/MPROTO-183
 * <p>
 * У tocc-filter-test@ya.ru создано правило:
 * Если	заголовок «tocc» совпадает c «tocc-filter-test@yandex.ru»
 * — переместить письмо в папку «redFolder»
 *
 */

@Stories("FASTSRV")
@Aqua.Test(title = "Поверка фильтров с условием 'Кому или копия'",
        description = "Проверяем, что письмо отфильтровано и доставлено в нужную папку")
@Title("FiltersToCcTest. Поверка фильтров с условием 'Кому или копия'")
@Description("Проверяем, что письмо отфильтровано и доставлено в нужную папку")
@Feature("Фильтры")
@RunWith(Parameterized.class)
public class FiltersToCcTest {
    private static final User RECEIVER_1 = new User("tocc-filter-test@ya.ru", "testqa");
    private static final User RECEIVER_2 = new User("tocc-filter-test2@ya.ru", "testqa");
    private static final User RECEIVER_3 = new User("tocc-filter-test3@ya.ru", "testqa");
    private static final User RECEIVER_4 = new User("tocc-filter-test4@ya.ru", "testqa");
    private static final User RECEIVER_5 = new User("tocc-filter-test5@ya.ru", "testqa");
    private static final String FILTERED_FOLDER_NAME = "redFolder";
    private Logger log = LogManager.getLogger(this.getClass());
    private TestMessage msg;

    @Parameterized.Parameter(0)
    public User recipient;
    @Parameterized.Parameter(1)
    public String recipientHeader;
    @Parameterized.Parameter(2)
    public String ccHeader;
    @Parameterized.Parameter(3)
    public String expectedFolder;

    @Rule
    public LogConfigRule newAquaLogRule = new LogConfigRule();
    //@Rule
    //public SshConnectionRule sshConnectionRule = new SshConnectionRule(mxTestProps().getNslsHost());

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return asList(new Object[]{RECEIVER_1, "tocc-filter-test@yandex.ru", null, FILTERED_FOLDER_NAME},
                new Object[]{RECEIVER_1, "tocc-filter-test@yandex.ru", "yantester@ya.ru", FILTERED_FOLDER_NAME},
                new Object[]{RECEIVER_1, "tocc-filter-test@ya.ru", "yantester@ya.ru", PG_FOLDER_DEFAULT},
                new Object[]{RECEIVER_2, "tocc-filter-test2@ya.ru", "yantester@yandex.ru", FILTERED_FOLDER_NAME},
                new Object[]{RECEIVER_2, "tocc-filter-test2@ya.ru", "tocc-filter-test@ya.ru", PG_FOLDER_DEFAULT},
                new Object[]{RECEIVER_3, "tocc-filter-test3@ya.ru", "tocc-filter-test@ya.ru", PG_FOLDER_DEFAULT},
                new Object[]{RECEIVER_3,
                        "tocc-filter-test3@yandex.ru", "tocc-filter-test2@ya.ru", FILTERED_FOLDER_NAME},
                new Object[]{RECEIVER_4,
                        "tocc-filter-test4@ya.ru", "tocc-filter-test@yandex.ru", FILTERED_FOLDER_NAME},
                new Object[]{RECEIVER_4, "tocc-filter-test4@ya.ru", null, PG_FOLDER_DEFAULT},
                new Object[]{RECEIVER_5, "tocc-filter-test5@ya.ru", "yantester@ya.ru", FILTERED_FOLDER_NAME});
    }

    @Before
    public void prepareTestMessageAndExpectedData() throws Exception {
        inMailbox(recipient).clearAll();

        msg = new TestMessage();
        msg.setRecipient(recipientHeader);
        if (ccHeader != null) {
            msg.addRecipient(MimeMessage.RecipientType.CC, new InternetAddress(ccHeader));
        }
        msg.setSubject("FiltersToCcTest" + randomAlphanumeric(20));
        msg.setFrom("devnull@yandex.ru");
        msg.setText("Message for " + ccHeader);
        msg.saveChanges();

        log.info(format("Отправили письмо с темой %s", msg.getSubject()));
        String serverResponse = sendByNsls(msg);
        log.info(serverResponse);
        String messageId = getMessageIdByServerResponse(serverResponse);
        //log.info(getInfoFromNsls(sshConnectionRule.getConn(), messageId));
    }

    @Test
    public void shouldSeeMsgInCorrectFolder() throws Exception {
        log.info("Проверяем результат фильтрации - доставку в нужную папку.");
        inMailbox(recipient).inFolder(expectedFolder).shouldSeeLetterWithSubject(msg.getSubject());
    }
}
