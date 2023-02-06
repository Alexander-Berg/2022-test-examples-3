package ru.yandex.market.olap2.controller;

import com.google.common.collect.ImmutableSet;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import ru.yandex.market.olap2.config.IntegrationTestConfig;

import java.util.HashSet;
import java.util.Set;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;


@RunWith(SpringRunner.class)
@ActiveProfiles("integration-test")
@SpringBootTest(classes = {IntegrationTestConfig.class})
public class MonitorLoadsControllerITest {

    @Autowired
    private NamedParameterJdbcTemplate jdbcTemplate;

    @Autowired
    private MonitorLoadsController monitorLoadsController;

    @Value("${olap2.cluster}")
    private String cluster;

    @Before
    public void init() {
        e("truncate table step_events");
        String sql = "insert into step_events " +
            "(event_step_id, cluster, event_name, created_at, step_created_at, path, partition, loaded, data_rejected) values ";
        // rejected no more
        e(sql + "('e1', '" + cluster + "', 'en1', now() - interval '10 minutes', now() - interval '11 minutes', '//some/testpath/unreject_1', 201803, false, true)");
        e(sql + "('e2', '" + cluster + "', 'en2', now(), now() - interval '1 minutes', '//some/testpath/unreject_1', 201803, true, false)");

        // not loaded for too long
        e(sql + "('e3', '" + cluster + "', 'en3', '2018-01-01', '2018-01-01', '//some/testpath/too_long_1', 201803, false, false)");

        // totally ok, but not yet loaded
        e(sql + "('e4', '" + cluster + "', 'en4', now(), now() - interval '1 minutes', '//some/testpath/ok_1', 201803, false, false)");

        // rejected and not partitioned
        e(sql + "('e5', '" + cluster + "', 'en5', now() - interval '10 minutes', now() - interval '11 minutes', '//some/testpath/reject_1', null, true, false)");
        e(sql + "('e6', '" + cluster + "', 'en6', now(), now() - interval '1 minutes', '//some/testpath/reject_1', null, false, true)");
    }

    @Test
    public void testGetLastIdPerLoad() {
        Set<String> lastIds = new HashSet<>(monitorLoadsController.getLastIdPerLoad());
        assertThat(lastIds, is(ImmutableSet.of("e2", "e3", "e4", "e6")));
    }

    @Test
    public void testGenerateError() {
        Set<String> errors = monitorLoadsController.generateError();
        assertThat(errors, is(ImmutableSet.of(
            "//some/testpath/reject_1 rejected",
            "//some/testpath/too_long_1/201803 not loaded for 4 hours"
        )));
    }

    private void e(String sql) {
        jdbcTemplate.getJdbcOperations().execute(sql);
    }

}
