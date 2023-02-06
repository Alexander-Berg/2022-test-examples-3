package ru.yandex.market.antifraud.yql.web;


import com.codahale.metrics.health.HealthCheckRegistry;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.curator.framework.recipes.leader.LeaderSelector;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import ru.yandex.market.antifraud.db.LoggingJdbcTemplate;
import ru.yandex.market.antifraud.filter.RndUtil;
import ru.yandex.market.antifraud.model.SessionStatusEnum;
import ru.yandex.market.antifraud.util.IntDateUtil;
import ru.yandex.market.antifraud.web.WebServer;
import ru.yandex.market.antifraud.yql.config.IntegrationTestAntifraudYqlConfig;
import ru.yandex.market.antifraud.yql.model.YtLogConfig;
import ru.yandex.market.antifraud.yql.runner.YqlAfRunner;
import ru.yandex.market.antifraud.yql.validate.YqlValidator;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.URLEncoder;
import java.util.Arrays;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {IntegrationTestAntifraudYqlConfig.class})
@ActiveProfiles("integration-tests")
@Slf4j
public class YqlWebServerITest {
    private static final int PORT;
    private YqlWebServer webServer;

    @Autowired
    private LoggingJdbcTemplate jdbcTemplate;

    @Test
    public void testStatusUpdates() {
        insertRandomTestStepEvent();
        assertOk(get("/status/updates"));
    }

    @Test
    public void testStatusNoUpdates() {
        jdbcTemplate.exec("truncate step_events");
        insertRandomTestStepEvent();
        try {
            jdbcTemplate.exec("alter table step_events disable trigger user");
            jdbcTemplate.exec("update step_events set step_time_created = updated - interval '1 month'");
            String resp = get("/status/updates");
            assertTrue("WS response was: " + resp, resp.startsWith(WebServer.CRIT));
        } finally {
            jdbcTemplate.exec("alter table step_events enable trigger user");
        }
    }

    @Test
    public void testStatusSessions() {
        insertRandomTestSession(SessionStatusEnum.SUCCESSFUL);
        assertOk(get("/status/sessions"));
    }

    @Test
    public void testStatusNoNewSessions() {
        jdbcTemplate.exec("truncate sessions");
        insertRandomTestSession(SessionStatusEnum.PENDING);
        insertRandomTestSession(SessionStatusEnum.FAILED);
        try {
            jdbcTemplate.exec("alter table sessions disable trigger user");
            jdbcTemplate.exec("update sessions " +
                "set updated = updated - interval '1 month' " +
                "where status = '" + SessionStatusEnum.PENDING + "'");
            String resp = get("/status/sessions");
            assertTrue("WS response was: " + resp, resp.startsWith(WebServer.CRIT));
        } finally {
            jdbcTemplate.exec("alter table sessions enable trigger user");
        }
    }

    @Test
    public void testStatusNoSessions() {
        jdbcTemplate.exec("truncate sessions");
        String resp = get("/status/sessions");
        assertTrue("WS response was: " + resp, resp.startsWith(WebServer.CRIT));
    }

    @Test
    @SneakyThrows
    public void testForceRollback() {
        String resp = get("/forcerollback?logname=testlog1&filter=12345&query=" + URLEncoder.encode("select ${fields} ${tbl}", "utf-8") + "&dryrun=false&day=20110829");
        assertTrue("WS response was: " + resp, resp.startsWith("Force rollback"));
    }

    @Test
    public void testRevalidate() {
        String resp = get("/revalidate?logname=testlog1&day=20110829");
        assertTrue("WS response was: " + resp, resp.startsWith("Revalidating"));
    }

    @Before
    public void initWebServer() {
        LeaderSelector lsMock = mock(LeaderSelector.class);
        when(lsMock.hasLeadership()).thenReturn(true);
        YqlAfRunner afRunnerMock = mock(YqlAfRunner.class);
        afRunnerMock.leaderSelector = lsMock;

        YtLogConfig logConfig = new YtLogConfig("testlog1");
        YqlValidator validatorMock = mock(YqlValidator.class);
        when(validatorMock.getYtLogConfig()).thenReturn(logConfig);

        YqlForceRollback forceRollbackMock = mock(YqlForceRollback.class);
        Mockito.doAnswer(invocation -> {
            Object[] args = invocation.getArguments();
            try {
                assertThat(args[0], is(logConfig));
                assertThat(args[1], is(20110829));
                assertThat(args[2], is("select ${fields} ${tbl}"));
                assertThat(args[3], is(12345L));
                assertThat(args[4], is(false));
            } catch (AssertionError e) {
                throw new RuntimeException(e);
            }
            return null;
        }).when(forceRollbackMock).async(
            Mockito.any(YtLogConfig.class),
            Mockito.anyInt(),
            Mockito.anyString(),
            Mockito.anyLong(),
            Mockito.anyBoolean(),
            Mockito.anyString()
        );

        YqlRevalidator revalidatorMock = mock(YqlRevalidator.class);
        Mockito.doAnswer(invocation -> {
            Object[] args = invocation.getArguments();
            try {
                assertThat(args[0], is(logConfig));
                assertThat(args[1], is(20110829));
            } catch (AssertionError e) {
                throw new RuntimeException(e);
            }
            return null;
        }).when(revalidatorMock).revalidate(
            Mockito.any(YtLogConfig.class),
            Mockito.anyInt(),
            Mockito.anyString()
        );

        webServer = new YqlWebServer(
            afRunnerMock,
            jdbcTemplate,
            forceRollbackMock,
            revalidatorMock,
            Arrays.asList(validatorMock),
            PORT,
            mock(HealthCheckRegistry.class)
        );
        webServer.start();
    }

    @After
    public void stopWebServer() {
        webServer.stop();
    }

    static {
        try(ServerSocket s = new ServerSocket(0)) {
            PORT = s.getLocalPort();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @SneakyThrows
    private void assertOk(String response) {
        assertThat(response, is(WebServer.OK));
    }

    @SneakyThrows
    private String get(String path) {
        HttpClient client = new HttpClient();
        GetMethod method = new GetMethod("http://localhost:" + PORT + path);
        try {
            int statusCode = client.executeMethod(method);
            if (statusCode != HttpStatus.SC_OK) {
                throw new RuntimeException("Status code of " + method + " is " + statusCode);
            }
            return new String(method.getResponseBody()).trim();
        } finally {
            method.releaseConnection();
        }
    }

    private void insertRandomTestStepEvent() {
        jdbcTemplate.update("insert into step_events " +
                "(event_step_id, cluster, event_name, log, scale, timestamp, day) values " +
                "(:event_step_id, '', '', 'somelog', 'testscale', :timestamp, :day)",
            "event_step_id", "wstest_" + RndUtil.randomAlphabetic(10),
            "timestamp", IntDateUtil.hyphenated(20121011),
            "day", 20121011);
    }

    private void insertRandomTestSession(SessionStatusEnum status) {
        jdbcTemplate.exec("insert into sessions " +
            "(day, scale, log, cluster, last_seen_event_id, host, status, seen_partitions) values " +
            "(20101101, 'RECENT', 'testlog1', 'cluster1', 0, 'host1'," +
            "'" + status.name() + "','some seen partitions')");
    }
}
