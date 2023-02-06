package ru.yandex.autotests.innerpochta.tests.filters;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.hamcrest.Matcher;
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

import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;

import static com.jayway.jsonassert.impl.matcher.IsCollectionWithSize.hasSize;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assume.assumeThat;
import static ru.yandex.autotests.innerpochta.utils.MxUtils.*;
import static ru.yandex.autotests.innerpochta.utils.WmiApiUtils.inMailbox;

/**
 * User: alex89
 * Date: 07.04.15
 * https://st.yandex-team.ru/DARIA-46279
 */

@Stories("FASTSRV")
@Feature("Karma")
@Aqua.Test(title = "Тестирование запрета пересылки писем по фильтрам от пользователей с плохой кармой",
        description = "Проверяем, что с пользователей с плохой кармой (85\\100) не рассылаем autoreply,notify,frw")
@Title("KarmaFiltersTest. Тестирование запрета пересылки писем по фильтрам от пользователей с плохой кармой")
@Description("Проверяем, что с пользователей с плохой кармой (85\\100) не рассылаем autoreply,notify,frw[DARIA-46279]")
@RunWith(Parameterized.class)
public class KarmaFiltersTest {
    private static final String COMMON_RESTRICT_NOTATION_IN_LOG =
            "ommiting forward/notify/autoreply to karma-test-receiver@ya.ru; reason: bad karma";
    private static final String AUTO_REPLY_RESTRICT_NOTATION_IN_LOG = "autoreply action skipped; for %s";
    private static final String NOTIFY_RESTRICT_NOTATION_IN_LOG = "notify action skipped; for %s";
    private static final String FORWARD_RESTRICT_NOTATION_IN_LOG = "forward action skipped; for %s";
    private static final User GOOD_USER_WHO_COLLECT_MSGS_BY_FILTERS = new User("karma-test-receiver@ya.ru", "testqa");
    private static final User KARMA_85_USER = new User("karma85-no-forward@yandex.ru", "testqa");
    private static final User KARMA_100_USER = new User("karma100-no-forward@yandex.ru", "testqa");

    private Logger log = LogManager.getLogger(this.getClass());
    private String serverResponse;

    @Parameterized.Parameter(0)
    public User from;
    @Parameterized.Parameter(1)
    public User to;
    @Parameterized.Parameter(2)
    public String subjectPrefix;
    @Parameterized.Parameter(3)
    public Matcher<String> sessionLogMatcher;

    @Rule
    public LogConfigRule newAquaLogRule = new LogConfigRule();
    //@Rule
    //public SshConnectionRule connectionRule = new SshConnectionRule(mxTestProps().getNslsHost());

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        Collection<Object[]> data = new LinkedList<Object[]>();
        for (User badKarmaUser : asList(KARMA_100_USER, KARMA_85_USER)) {
            data.add(new Object[]{GOOD_USER_WHO_COLLECT_MSGS_BY_FILTERS, badKarmaUser, "autoreply",
                    allOf(containsString(COMMON_RESTRICT_NOTATION_IN_LOG),
                            containsString(format(AUTO_REPLY_RESTRICT_NOTATION_IN_LOG, badKarmaUser.getLogin())))});
            data.add(new Object[]{GOOD_USER_WHO_COLLECT_MSGS_BY_FILTERS, badKarmaUser, "notify",
                    allOf(containsString(COMMON_RESTRICT_NOTATION_IN_LOG),
                            containsString(format(NOTIFY_RESTRICT_NOTATION_IN_LOG, badKarmaUser.getLogin())))});
            data.add(new Object[]{GOOD_USER_WHO_COLLECT_MSGS_BY_FILTERS, badKarmaUser, "forward",
                    allOf(containsString(COMMON_RESTRICT_NOTATION_IN_LOG),
                            containsString(format(FORWARD_RESTRICT_NOTATION_IN_LOG, badKarmaUser.getLogin())))});
            data.add(new Object[]{GOOD_USER_WHO_COLLECT_MSGS_BY_FILTERS, badKarmaUser, "multi_frw",
                    allOf(containsString(COMMON_RESTRICT_NOTATION_IN_LOG),
                            containsString(format(AUTO_REPLY_RESTRICT_NOTATION_IN_LOG, badKarmaUser.getLogin())),
                            containsString(format(NOTIFY_RESTRICT_NOTATION_IN_LOG, badKarmaUser.getLogin())),
                            containsString(format(FORWARD_RESTRICT_NOTATION_IN_LOG, badKarmaUser.getLogin())))});
        }
        return data;
    }

    @Before
    public void sendTestMessage() throws Exception {
        inMailbox(GOOD_USER_WHO_COLLECT_MSGS_BY_FILTERS).clearAll();
        TestMessage msg = new TestMessage();
        msg.setFrom(from.getLogin());
        msg.setRecipient(to.getLogin());
        msg.setSubject(subjectPrefix + randomAlphanumeric(15));
        msg.setText(randomAlphanumeric(15));
        msg.saveChanges();
        log.info(format("Отправляем письмо от %s к %s", from.getLogin(), to.getLogin()));
        serverResponse = sendByNsls(msg);
    }

    @Test
    @Title("Проверка записей в логах о том, что с пользователей с плохой кармой (85\\100) " +
            "не произошло пересылок писем по фильтрам")
    @Description("Отправлем пользователю с плохой кармой (85\\100) письмо, которое подпадает под фильтры-пересылки, " +
            "настроеные у этого пользователя. Ожидаем, что autoreply,notify,frw-письма не будут отправлены, " +
            "о чем должна быть в логе соответсвующая запись[DARIA-46279]")
    public void shouldSeeAutoReplyNotifyFrwdRestrictNotationsInSessionLog() throws IOException, InterruptedException {
        //String sessionLog = getInfoFromNsls(connectionRule.getConn(), getMessageIdByServerResponse(serverResponse));
        //log.info(sessionLog);
        //assertThat("Не обнаружили в логе записи о запрете пересылок писем по фильтрам[DARIA-46279]!",
        //        sessionLog, sessionLogMatcher);
    }

    @Test
    @Title("Проверка того, что фильтры пользователей с плохой кармой (85\\100) действительно писем не отправили")
    @Description("Отправлем пользователю с плохой кармой (85\\100) письмо, которое подпадает под фильтры-пересылки, " +
            "настроеные у этого пользователя. Ожидаем, что на karma-test-receiver@ya.ru" +
            " не прилетит ни одного autoreply,notify,frw-письма [DARIA-46279]")
    public void shouldNotSeeAnyAutoReplyNotifyFrwdMsgsFromBadKarmaUsers() throws IOException {
        assertThat("Фильтры ошибочно сработали на пользователях с плохой кармой, " +
                        "т.к. karma-test-receiver@ya.ru получил от них письмо",
                inMailbox(GOOD_USER_WHO_COLLECT_MSGS_BY_FILTERS).getAllMidsInMailbox(), hasSize(0));
    }

}
