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
import java.util.Collection;
import java.util.LinkedList;

import static java.lang.String.format;
import static org.apache.commons.lang.RandomStringUtils.randomAlphanumeric;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.StringContains.containsString;
import static ru.yandex.autotests.innerpochta.tests.matchers.MessageHeaderMatcher.hasHeader;
import static ru.yandex.autotests.innerpochta.utils.MxUtils.*;
import static ru.yandex.autotests.innerpochta.utils.WmiApiUtils.inMailbox;

/**
 * User: alex89
 * Date: 18.05.2015
 * https://st.yandex-team.ru/MPROTO-183
 */

@Stories("FASTSRV")
@Aqua.Test(title = "Поверка фильтров на уведомления",
        description = "Проверяем, что к письмо-уведомление доставлено и содержит правильный текст " +
                "в зависимости от страны хозяина письма.")
@Title("FiltersNotifyTest. Поверка фильтров на уведомления")
@Description("Проверяем, что к письмо-уведомление доставлено и содержит правильный текст " +
        "в зависимости от страны хозяина письма.")
@Feature("Фильтры")
@RunWith(Parameterized.class)
public class FiltersNotifyTest {
    private static final int WAIT_NOTIFY_TIMEOUT_IN_SECONDS = 60;
    private static final String EXPECTED_FROM = "\"yantester@ya.ru\" <devnull@yandex.ru>";
    private static final User RECEIVER_MAIN_RU = new User("with-filters-notify-test-user@ya.ru", "testqa12345678");
    private static final User NOTIFY_RECEIVER_1 = new User("notify-test-user-1@ya.ru", "testqa12345678");
    private static final User RECEIVER_MAIN_UA = new User("with-filters-notify-test-user2@ya.ru", "testqa12345678");
    private static final User RECEIVER_MAIN_BY = new User("with-filters-notify-test-user3@ya.ru", "testqa12345678");
    private static final User RECEIVER_MAIN_KZ = new User("with-filters-notify-test-user4@ya.ru", "testqa12345678");
    private static final User RECEIVER_MAIN_COM = new User("with-filters-notify-test-user5@ya.ru", "testqa12345678");
    private static final User RECEIVER_MAIN_COMTR = new User("with-filters-notify-test-user6@ya.ru", "testqa12345678");
    private Logger log = LogManager.getLogger(this.getClass());
    private TestMessage testMsg;
    private String textOfMainMsg = "Main msg text\n" + randomAlphanumeric(7);


    @Parameterized.Parameter(0)
    public User mainRecipient;
    @Parameterized.Parameter(1)
    public User notifyRecipient;
    @Parameterized.Parameter(2)
    public String expectedSubjectOfNotify;
    @Parameterized.Parameter(3)
    public String expectedContentOfNotify;

    @Rule
    public LogConfigRule newAquaLogRule = new LogConfigRule();
    //@Rule
    //public SshConnectionRule sshConnectionRule = new SshConnectionRule(mxTestProps().getNslsHost());

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        Collection<Object[]> data = new LinkedList<Object[]>();
        data.add(new Object[]{RECEIVER_MAIN_RU, NOTIFY_RECEIVER_1, "Новое письмо в Яндекс.Почте",
                "Вам письмо на %s. Прочитать: https://ya.cc/ZZZE / Яндекс.Почта"});
        data.add(new Object[]{RECEIVER_MAIN_UA, NOTIFY_RECEIVER_1, "Новое письмо в Яндекс.Почте",
                "Вам письмо на %s. Прочитать: https://ya.cc/ZZ_U / Яндекс.Почта"});
        data.add(new Object[]{RECEIVER_MAIN_BY, NOTIFY_RECEIVER_1, "Новое письмо в Яндекс.Почте",
                "Вам письмо на %s. Прочитать: https://ya.cc/ZZ_i / Яндекс.Почта"});
        data.add(new Object[]{RECEIVER_MAIN_KZ, NOTIFY_RECEIVER_1, "Новое письмо в Яндекс.Почте",
                "Вам письмо на %s. Прочитать: https://ya.cc/ZZ_u / Яндекс.Почта"});
        data.add(new Object[]{RECEIVER_MAIN_COM, NOTIFY_RECEIVER_1, "Новое письмо в Яндекс.Почте",
                "Вам письмо на %s. Прочитать: https://ya.cc/ZZ_A / Яндекс.Почта"});
        data.add(new Object[]{RECEIVER_MAIN_COMTR, NOTIFY_RECEIVER_1, "Новое письмо в Яндекс.Почте",
                "Вам письмо на %s. Прочитать: https://ya.cc/ZZZm / Яндекс.Почта"});
        //оставлено место для параметров, когда починят локализацию.
        return data;
    }

    @Before
    public void prepareTestMessageAndExpectedData() throws Exception {
        inMailbox(NOTIFY_RECEIVER_1).clearDefaultFolder();
        inMailbox(mainRecipient).clearDefaultFolder();
        testMsg = new TestMessage();
        testMsg.setSubject(randomAlphanumeric(7) + "NOTIFY");
        testMsg.setFrom(new InternetAddress("yantester@ya.ru", "Povelitel\'\"писем"));
        testMsg.setRecipient(mainRecipient.getLogin());
        testMsg.setText(textOfMainMsg);
        testMsg.saveChanges();

        log.info(format("Отправили письмо с темой %s", testMsg.getSubject()));
        String serverResponse = sendByNsls(testMsg);
        log.info(serverResponse);
        String messageId = getMessageIdByServerResponse(serverResponse);
        //log.info(getInfoFromNsls(sshConnectionRule.getConn(), messageId));
    }

    @Test
    public void shouldSeeNotifyMsgInMailBoxWithCorrectSubjectTextAndFrom() throws Exception {
        log.info("Проверяем наличие письма-уведомления [MPROTO-183].");
        inMailbox(notifyRecipient).shouldSeeLetterWithSubject(expectedSubjectOfNotify, WAIT_NOTIFY_TIMEOUT_IN_SECONDS);

        TestMessage receivedMsg = inMailbox(notifyRecipient).getMessageWithSubject(expectedSubjectOfNotify);
        assertThat("Неверное значение From в письме-уведомлении [MPROTO-3000]!", receivedMsg,
                hasHeader("From", equalTo(EXPECTED_FROM)));
        assertThat("Неверное содержание письма-уведомления!", (String) receivedMsg.getContent(),
                containsString(format(expectedContentOfNotify, mainRecipient.getLogin())));
    }


    @Test
    public void shouldSeeMainMsgInMailBoxWithCorrectSubjectAndText() throws Exception {
        log.info("Проверяем доставку основного письма [MPROTO-183].");
        inMailbox(mainRecipient).shouldSeeLetterWithSubjectAndContent(testMsg.getSubject(),
                containsString(textOfMainMsg));
    }
}
