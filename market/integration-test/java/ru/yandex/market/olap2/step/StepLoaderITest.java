package ru.yandex.market.olap2.step;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import ru.yandex.market.olap2.config.IntegrationTestConfig;

@RunWith(SpringRunner.class)
@ActiveProfiles("integration-test")
@SpringBootTest(classes = {IntegrationTestConfig.class})
public class StepLoaderITest {

    @Autowired
    private NamedParameterJdbcTemplate jdbcTemplate;

    @Autowired
    private StepLoader stepLoader;

    @Test
    public void loadEvents() {
        jdbcTemplate.getJdbcOperations().execute("truncate table step_events");
        stepLoader.loadEvents();
//        assertTrue(jdbcTemplate.queryForObject(
//            "select count(*) from step_events",
//            Collections.emptyMap(),
//            Integer.class
//        ) > 0);
    }

}
