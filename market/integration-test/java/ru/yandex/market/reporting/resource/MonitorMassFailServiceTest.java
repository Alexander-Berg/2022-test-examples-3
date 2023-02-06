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

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = IntegrationTestConfig.class)
@Log4j2
public class MonitorMassFailServiceTest {
    @Value("${service.url}")
    private String serviceUrl;

    private WebTarget target;

    @Autowired
    private NamedParameterJdbcTemplate metadataJdbcTemplate;

    @Before
    public void setUp() throws Exception {
        target = ClientBuilder.newClient().target(serviceUrl);
        metadataJdbcTemplate.getJdbcOperations().execute("truncate jobs");
        for(int i = 0; i < 3; i++) {
            metadataJdbcTemplate.getJdbcOperations().execute("insert into jobs " +
                "(name, submitted_by, params, started_at, submitted_at, finished_at, updated_at, status, submitted_from) " +
                "values\n" +
                "('jname_" + i + "', 'robot-market-forc', 'params1', NOW() - INTERVAL '1 SECOND', NOW(), NOW(), " +
                "NOW() - INTERVAL '10 HOURS', 'NEW', 'from1')");
        }
    }

    @Test
    public void test() {
        String responseMsgErr = target.path("monitor_mass_fail").request().get(String.class);
        assertThat(responseMsgErr, is("0;Ok"));
    }
}
