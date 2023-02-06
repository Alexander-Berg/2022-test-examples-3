package ru.yandex.market.antifraud.yql.step;

import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import ru.yandex.market.antifraud.db.LoggingJdbcTemplate;
import ru.yandex.market.antifraud.yql.config.IntegrationTestAntifraudYqlConfig;
import ru.yandex.market.stat.step.model.StepEventsRequestResult;

import static org.junit.Assert.assertTrue;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {IntegrationTestAntifraudYqlConfig.class})
@ActiveProfiles("integration-tests")
@Slf4j
public class StepEventSaverITest {
    @Autowired
    private StepEventSaver showsEventsSaver;

    @Autowired
    private LoggingJdbcTemplate jdbcTemplate;

    @Before
    public void init() {
        jdbcTemplate.exec("truncate step_events");
    }

    @Test
    @Ignore("No events in testing")
    public void mustGetSomeStepEventsOrEmptyResult() {
        StepEventsRequestResult requestResult = showsEventsSaver.getPage(
                showsEventsSaver.getYtLogConfig().getStepEventPublish(),
                showsEventsSaver.getYtLogConfig().getStepLogName(),
                "30min",
                0);
        assertTrue(requestResult.getEvents().size() >= 0);
        showsEventsSaver.savePage(requestResult);
        assertCount();
    }

    @Test
    @Ignore("No events in testing")
    public void mustLoadEverythingFromStep() {
        showsEventsSaver.loadAll();
        assertCount();
    }

    private void assertCount() {
        assertTrue(jdbcTemplate.query(
            "select count(*) from step_events", Long.class) > 0);
    }
}
