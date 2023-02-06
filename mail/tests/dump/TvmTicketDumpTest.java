package ru.yandex.autotests.innerpochta.yfurita.tests.dump;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.junit.*;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.aqua.annotations.project.Feature;
import ru.yandex.autotests.innerpochta.tests.unstable.TestMessage;
import ru.yandex.autotests.innerpochta.tests.unstable.User;
import ru.yandex.autotests.innerpochta.util.rules.RestAssuredLogger;

import ru.yandex.autotests.innerpochta.wmi.core.rules.SSHRule;
import ru.yandex.autotests.innerpochta.wmi.core.rules.local.TcpdumpRule;
import ru.yandex.autotests.innerpochta.yfurita.util.Credentials;
import ru.yandex.autotests.innerpochta.yfurita.util.FilterUser;
import ru.yandex.autotests.innerpochta.yfurita.util.UserInitializationRule;
import ru.yandex.autotests.plugins.testpers.LogConfigRule;
import ru.yandex.qatools.allure.annotations.*;

import java.net.URI;
import java.util.Collection;

import static java.util.Arrays.asList;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assume.assumeThat;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.WaitForMatcherDecorator.withWaitFor;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.ssh.TcpdumpLogMatcher.hasOneQuery;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import java.util.HashMap;

import static ru.yandex.autotests.innerpochta.yfurita.util.FilterSettings.CLIKER_MOVEL;
import static ru.yandex.autotests.innerpochta.yfurita.util.FilterSettings.FiltersParams.*;
import static ru.yandex.autotests.innerpochta.yfurita.util.FilterSettings.FiltersParams.CLICKER;
import static ru.yandex.autotests.innerpochta.yfurita.util.FilterSettings.FiltersParams.FIELD3;
import static ru.yandex.autotests.innerpochta.yfurita.util.FuritaConsts.PG_FOLDER_DEFAULT;
import static ru.yandex.autotests.innerpochta.yfurita.util.YFuritaUtils.JSON;
import static ru.yandex.autotests.innerpochta.yfurita.util.YFuritaUtils.XML;
import static ru.yandex.autotests.innerpochta.yfurita.util.YfuritaProperties.yfuritaProps;
import static ru.yandex.autotests.innerpochta.yfurita.util.matchers.MessageIndexedMatcher.messageIndexed;

/**
 * Created with IntelliJ IDEA.
 * User: alex89
 * Date: 7/13/16
 * <p/>
 */

@Aqua.Test(title = "Тестирование передачи  tvm-тикета в мопс и поиск",
        description = "Тестирование передачи  tvm-тикета в мопс и поиск")
@Title("TvmTicketDumpTest.Тестирование передачи  tvm-тикета в мопс и поиск [MAILDEV-807]")
@Description("Тестирование передачи  tvm-тикета в мопс и поиск [MAILDEV-807]")
@Feature("Yfurita.TcpDump")
@RunWith(Parameterized.class)
public class TvmTicketDumpTest {
    private static final String MSEARCH_HOST = "new-msearch-proxy.mail.yandex.net";
    private static final String MOPS_HOST = "mops.mail.yandex.net";
    private static final long ASYNC_OPERATION_TIMEOUT = 5000;
    private static final long TCPDUMP_START_TIMEOUT = 5000;
    private static String filterId;
    private static FilterUser fUser;
    private static HashMap<String, String> params;
    private static Logger logger = LogManager.getLogger(TvmTicketDumpTest.class);
    private String tvmTicketValue;

    @Credentials(loginGroup = "TvmTicketDumpTest")
    public static User testUser;

    @ClassRule
    public static RestAssuredLogger raLog = new RestAssuredLogger();
    @ClassRule
    public static UserInitializationRule userInitializationRule = new UserInitializationRule();
    @Rule
    public LogConfigRule logRule = new LogConfigRule();
    @ClassRule
    public static SSHRule sshRule =
            new SSHRule(logger).serverUrl(URI.create(yfuritaProps().getYfuritaUrl()));
    @Rule
    public TcpdumpRule tcpdumpRule;

    @Parameterized.Parameter(0)
    public String format;

    @Parameterized.Parameters
    public static Collection<Object[]> data() throws Exception {
        return asList(new Object[]{XML}, new Object[]{JSON});
    }

    @BeforeClass
    public static void initFilterUserAndParams() throws Exception {
        fUser = new FilterUser(testUser);
        params = new HashMap<String, String>();
        params.put(NAME.getName(), randomAlphanumeric(20));
        params.put(LOGIC.getName(), "1");
        params.put(FIELD1.getName(), "subject");
        params.put(FIELD2.getName(), "3");
        params.put(FIELD3.getName(), "mark_letter");
        params.put(CLICKER.getName(), CLIKER_MOVEL);
        params.put(MOVE_LABEL.getName(), "9");
    }

    @Before
    public void sendMsgs() throws Exception {
        fUser.clearAll();
        fUser.removeAllFilters();
        filterId = fUser.createFilter(params);
        tvmTicketValue = fUser.getUserTicket();

        TestMessage testMessage = new TestMessage();
        testMessage.setRecipient(testUser.getLogin());
        testMessage.setFrom("devnull@yandex.ru");
        testMessage.setText("Message to be deleted: " + randomAlphanumeric(20));
        testMessage.setSubject("mark_letter: " + randomAlphanumeric(20));
        testMessage.saveChanges();

        fUser.sendMessageWithFilterOff(testMessage);
        assumeThat(fUser.inFolder(PG_FOLDER_DEFAULT).getMidOfMessageWithSubject(testMessage.getSubject()),
                withWaitFor(messageIndexed(fUser), MINUTES.toMillis(3), SECONDS.toMillis(10)));
    }

    @Test
    @Title("Tcpdump-ом проверяем отправку tvm-тикета при apply-запросе в поиск")
    @Issues({@Issue("MAILDEV-807")})
    @Description("Tcpdump-ом проверяем отправку tvm-тикета при apply-запросе в поиск")
    public void shouldSeeTvmTicketTransmissionToMsearchOnApply() throws Exception {
        tcpdumpRule = new TcpdumpRule(sshRule).host(MSEARCH_HOST);
        tcpdumpRule.startTcpdump();
        Thread.sleep(TCPDUMP_START_TIMEOUT);
        fUser.applyFilterWithTvmTicket(filterId, format, tvmTicketValue);
        Thread.sleep(ASYNC_OPERATION_TIMEOUT);
        tcpdumpRule.finishTcpdump();

        assertThat("Не передали на поиск необходимый tvm-тикет при apply-запросе [MAILDEV-807]",
                tcpdumpRule, hasOneQuery("ticket: " + tvmTicketValue));
    }

    @Test
    @Title("Tcpdump-ом проверяем отправку tvm-тикета при apply-запросе в мопс")
    @Issues({@Issue("MAILDEV-807")})
    @Description("Tcpdump-ом проверяем отправку tvm-тикета при apply-запросе в мопс")
    public void shouldSeeTvmTicketTransmissionToMopsOnApply() throws Exception {
        tcpdumpRule = new TcpdumpRule(sshRule).host(MOPS_HOST);
        tcpdumpRule.startTcpdump();
        Thread.sleep(TCPDUMP_START_TIMEOUT);
        fUser.applyFilterWithTvmTicket(filterId, format, tvmTicketValue);
        Thread.sleep(ASYNC_OPERATION_TIMEOUT);
        tcpdumpRule.finishTcpdump();

        assertThat("Не передали на мопс необходимый tvm-тикет при apply-запросе [MAILDEV-807]",
                tcpdumpRule, hasOneQuery("ticket: " + tvmTicketValue));
    }

    @Test
    @Title("Tcpdump-ом проверяем отправку tvm-тикета при preview-запросе в поиск")
    @Issues({@Issue("MAILDEV-807")})
    @Description("Tcpdump-ом проверяем отправку tvm-тикета при preview-запросе в поиск")
    public void shouldSeeTvmTicketTransmissionOnPreview() throws Exception {
        tcpdumpRule = new TcpdumpRule(sshRule).host(MSEARCH_HOST);
        tcpdumpRule.startTcpdump();
        Thread.sleep(TCPDUMP_START_TIMEOUT);
        fUser.previewFilterWithTvmTicket(filterId, format, tvmTicketValue);
        tcpdumpRule.finishTcpdump();

        assertThat("Не передали на поиск необходимый tvm-тикет при apply-запросе [MAILDEV-807]",
                tcpdumpRule, hasOneQuery("ticket: " + tvmTicketValue));
    }

    @AfterClass
    public static void disableAllFilters() throws Exception {
        fUser.disableAllFilters();
    }
}
