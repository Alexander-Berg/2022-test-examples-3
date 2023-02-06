package ru.yandex.market.reporting.resource;

import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import ru.yandex.market.reporting.config.IntegrationTestConfig;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;

import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

@Slf4j
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = IntegrationTestConfig.class)
public class MonitorAvgTimingsServiceTest {
    @Value("${service.url}")
    private String serviceUrl;

    private WebTarget target;

    @Autowired
    private NamedParameterJdbcTemplate metadataJdbcTemplate;

    @Before
    public void setUp() throws Exception {
        target = ClientBuilder.newClient().target(serviceUrl);
        metadataJdbcTemplate.getJdbcOperations().execute("truncate jobs");
        metadataJdbcTemplate.getJdbcOperations().execute("insert into jobs " +
            "(name, submitted_by, params, started_at, submitted_at, finished_at, status, submitted_from) values\n" +
            "('jname1', 'me', 'params1', NOW() - INTERVAL '1 SECOND', NOW(), NOW(), 'SUCCESSFUL', 'from1'),\n" +
            "('jname2', 'me', 'params1',  NOW() - INTERVAL '1 SECOND', NOW(), NOW(), 'SUCCESSFUL', 'from1'),\n" +
            "('jname3', 'me', 'params1',  NOW() - INTERVAL '1 SECOND', NOW(), NOW(), 'SUCCESSFUL', 'from1'),\n" +
            "('jname4', 'me', 'params1',  NOW() - INTERVAL '1 SECOND', NOW(), NOW(), 'SUCCESSFUL', 'from1'),\n" +
            "('jname5', 'me', 'params1',  NOW() - INTERVAL '1 SECOND', NOW(), NOW(), 'SUCCESSFUL', 'from1'),\n" +
            "('jname6', 'me', 'params1',  NOW() - INTERVAL '1 SECOND', NOW(), NOW(), 'SUCCESSFUL', 'from1'),\n" +
            "('jname7', 'me', 'params1',  NOW() - INTERVAL '1 SECOND', NOW(), NOW(), 'SUCCESSFUL', 'from1'),\n" +
            "('jname8', 'me', 'params1',  NOW() - INTERVAL '1 SECOND', NOW(), NOW(), 'SUCCESSFUL', 'from1'),\n" +
            "('jname9', 'me', 'params1',  NOW() - INTERVAL '1 SECOND', NOW(), NOW(), 'SUCCESSFUL', 'from1'),\n" +
            "('jname10', 'me', 'params1',  NOW() - INTERVAL '1 SECOND', NOW(), NOW(), 'SUCCESSFUL', 'from1'),\n" +
            "('jname11', 'me', 'params1',  NOW() - INTERVAL '1 SECOND', NOW(), NOW(), 'SUCCESSFUL', 'from1'),\n" +
            "('jname12', 'me', 'params1',  NOW() - INTERVAL '11 HOURS', NOW(), NOW(), 'NEW', 'from1')");
    }

    @Test
    public void test() {
        String responseMsgErr = target.path("monitor_avg_timings")
            .queryParam("is_forecaster", false)
            .request().get(String.class);
        assertThat(responseMsgErr.toLowerCase(), startsWith("1;{\"me:"));
        metadataJdbcTemplate.getJdbcOperations().execute("truncate jobs");
        String responseMsgOk = target.path("monitor_avg_timings")
            .queryParam("is_forecaster", false)
            .request().get(String.class);
        assertThat(responseMsgOk, is("0;OK"));
    }
}
