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
import java.util.LinkedList;

import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assume.assumeThat;
import static ru.yandex.autotests.innerpochta.utils.MxConstants.PG_FOLDER_DEFAULT;
import static ru.yandex.autotests.innerpochta.utils.MxUtils.sendByNwsmtp;
import static ru.yandex.autotests.innerpochta.utils.WmiApiUtils.inMailbox;
import static ru.yandex.autotests.innerpochta.utils.MxTestProperties.mxTestProps;

/**
 * User: alex89
 * Date: 20.03.14
 */

@Stories("NWSMTP-FASTSRV")
@Feature("Фильтры")
@Aqua.Test(title = "Проверка пересылки писем по форвардным фильтрам",
        description = "Используется POP3.Проверка пересылки писем по форвардным фильтрам от яндексового пользователя" +
                " к пользователям:яндексоводу, ПДД,google,rambler.Проверяется опция: 'сохранить копию при пересылке'")
@Title("ForwardFiltersTest. Проверка пересылки писем по форвардным фильтрам")
@Description("Используется POP3.Проверка пересылки писем по форвардным фильтрам от яндексового пользователя " +
        "к пользователям:яндексоводу, ПДД,google,rambler.Проверяется опция: 'сохранить копию при пересылке'")
@RunWith(Parameterized.class)
public class ForwardFiltersTest {
    private static final User SENDER = new User("yantester@ya.ru", "12345678");
    private static final User RCPT_WITH_FILTERS = new User("rcpt-with-fwd-filters@ya.ru", "testqa");
    private static final User REAL_RCPT1 = new User("test-fwd-rcpt-1@yandex.ru", "testqa");
    private static final User REAL_RCPT2 = new User("test-fwd-rcpt-2@yandex.ru", "testqa");
    private static final User REAL_RCPT3 = new User("test.fwd.rcpt3@gmail.com", "testqa12345678");
    private static final User REAL_RCPT4 = new User("test.fwd.rcpt4@rambler.ru", "testqa12345678");
    private static final User REAL_RCPT5 = new User("test-fwd-rcpt.5@testtrest.yaconnect.com", "testqa12345678");

    private Logger log = LogManager.getLogger(this.getClass());
    private TestMessage msg;

    @Parameterized.Parameter(0)
    public String themePrefix;
    @Parameterized.Parameter(1)
    public User realRcpt;
    @Parameterized.Parameter(2)
    public int numberOfMsgsInInboxOfRcptWithFilters;
    @Parameterized.Parameter(3)
    public boolean isExternalUser;
    @Parameterized.Parameter(4)
    public String popServer;
    @Parameterized.Parameter(5)
    public int popPort;
    @Parameterized.Parameter(6)
    public boolean isSsl;

    @Rule
    public LogConfigRule newAquaLogRule = new LogConfigRule();

    @Parameterized.Parameters
    public static Collection<Object[]> data() throws Exception {
        Collection<Object[]> data = new LinkedList<Object[]>();
        data.add(new Object[]{"test-fwd-rcpt-1", REAL_RCPT1, 0, false, "", -1, false});
        data.add(new Object[]{"test-fwd-rcpt-2", REAL_RCPT2, 1, false, "", -1, false});
        // data.add(new Object[]{"test.fwd.rcpt3", REAL_RCPT3, 0, true, "pop.gmail.com", 995, true}); - TODO(nickitat): uncomment after MAILDLV-3820 will be resolved
      //  data.add(new Object[]{"test.fwd.rcpt4", REAL_RCPT4, 1, true, "pop.rambler.ru", 995, true});
        data.add(new Object[]{"test-fwd-rcpt.5", REAL_RCPT5, 1, false, "", -1, false});
        return data;
    }

    @BeforeClass
    public static void clearUsers() {
        inMailbox(RCPT_WITH_FILTERS).clearDefaultFolder();
        inMailbox(REAL_RCPT1).clearDefaultFolder();
        inMailbox(REAL_RCPT2).clearDefaultFolder();
    }


    @Before
    public void prepareTestMessage() throws Exception {
        msg = new TestMessage();
        msg.setSubject(themePrefix + randomAlphanumeric(9));
        msg.setRecipient(RCPT_WITH_FILTERS.getLogin());
        msg.setFrom(SENDER.getLogin());
        msg.setText("test-fwd-rcpt-2");
        msg.saveChanges();
    }

    @Test
    public void shouldSeeForwardedWithFiltersLettersDeliveryToYandexUsers()
            throws IOException, MessagingException, InterruptedException {
        assumeThat("Этот тест предназначен для Яндексовых пользователей!", isExternalUser, is(false));
        log.info(String.format("Отпрвляем письмо с темой '%s', Ожидаем пересылки на ящик %s",
                msg.getSubject(), realRcpt.getLogin()));
        sendByNwsmtp(msg, mxTestProps().getMxServer(), mxTestProps().getMxPort());
        inMailbox(realRcpt).shouldSeeLetterWithSubject(msg.getSubject(), 120);
        inMailbox(RCPT_WITH_FILTERS).inFolder(PG_FOLDER_DEFAULT).shouldSeeLettersWithSubject(msg.getSubject(),
                numberOfMsgsInInboxOfRcptWithFilters);
    }
}
