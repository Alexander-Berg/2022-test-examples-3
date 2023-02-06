package ru.yandex.market.olap2.step;

import com.google.common.collect.ImmutableMap;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import ru.yandex.market.olap2.config.IntegrationTestConfig;
import ru.yandex.market.olap2.step.model.StepEvent;
import ru.yandex.market.olap2.step.model.StepEventParams;

import java.util.concurrent.ThreadLocalRandom;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

@RunWith(SpringRunner.class)
@ActiveProfiles("integration-test")
@SpringBootTest(classes = {IntegrationTestConfig.class})
public class StepSaverITest {

    @Autowired
    private NamedParameterJdbcTemplate jdbcTemplate;

    @Autowired
    private StepSaver stepSaver;

    @Test
    public void testSaveNullPartition() {
        StepEvent e = generate(true);
        stepSaver.save(e);
        assertThat(jdbcTemplate.queryForObject(
            "select count(*) from step_events where event_step_id = :event_step_id",
            ImmutableMap.of("event_step_id", e.getId()),
            Integer.class),
            is(1));
    }

    @Test
    public void testSave() {
        StepEvent e = generate(false);
        stepSaver.save(e);
        assertThat(jdbcTemplate.queryForObject(
            "select count(*) from step_events where event_step_id = :event_step_id",
            ImmutableMap.of("event_step_id", e.getId()),
            Integer.class),
            is(1));
    }

    private StepEvent generate(boolean noPartition) {
        StepEventParams params = new StepEventParams();
        params.setCluster("cluster1");
        params.setPartition(noPartition ? null : "2018-03");
        params.setPath("path1");

        StepEvent e = new StepEvent();
        e.setId("eid1_" + ThreadLocalRandom.current().nextInt());
        e.setName("ename1");
        e.setTimeCreated("2018-04-24 12:36:56.110000");
        e.setStepEventParams(params);

        return e;
    }
}
