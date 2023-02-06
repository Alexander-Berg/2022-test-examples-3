package ru.yandex.market.reporting.resource;

import lombok.extern.log4j.Log4j2;
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

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = IntegrationTestConfig.class)
@Log4j2
public class MonitorLastUpdateServiceTest {
    @Value("${service.url}")
    private String serviceUrl;

    private WebTarget target;

    @Autowired
    private NamedParameterJdbcTemplate metadataJdbcTemplate;

    @Before
    public void setUp() throws Exception {
        target = ClientBuilder.newClient().target(serviceUrl);
        metadataJdbcTemplate.getJdbcOperations().execute("truncate jobs");
        metadataJdbcTemplate.getJdbcOperations().execute("alter table jobs disable trigger user");
        metadataJdbcTemplate.getJdbcOperations().execute("insert into jobs " +
            "(name, submitted_by, params, started_at, submitted_at, finished_at, updated_at, status, submitted_from) " +
            "values\n" +
            "('jname13', 'me', 'params1', NOW() - INTERVAL '1 SECOND', NOW(), NOW(), " +
            "NOW() - INTERVAL '10 HOURS', 'NEW', 'from1')");
        metadataJdbcTemplate.getJdbcOperations().execute("alter table jobs enable trigger user");
    }

    @Test
    public void test() {
        String responseMsgErr = target.path("monitor_last_update")
            .queryParam("is_forecaster", false).request().get(String.class);
        assertTrue(responseMsgErr.toLowerCase().startsWith("1;{\"jname13:"));
        metadataJdbcTemplate.getJdbcOperations().execute("truncate jobs");
        String responseMsgOk = target.path("monitor_last_update")
            .queryParam("is_forecaster", false).request().get(String.class);
        assertThat(responseMsgOk, is("0;OK"));
    }
}
