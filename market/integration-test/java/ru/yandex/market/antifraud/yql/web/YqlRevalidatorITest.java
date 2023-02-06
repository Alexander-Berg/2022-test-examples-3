package ru.yandex.market.antifraud.yql.web;

import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import ru.yandex.market.antifraud.db.LoggingJdbcTemplate;
import ru.yandex.market.antifraud.util.IntDateUtil;
import ru.yandex.market.antifraud.yql.config.IntegrationTestAntifraudYqlConfig;
import ru.yandex.market.antifraud.yql.model.UnvalidatedDay;
import ru.yandex.market.antifraud.yql.validate.UnvalidatedDaysHelper;
import ru.yandex.market.antifraud.yql.validate.YtTestDataGenerator;

import java.util.Set;

import static org.junit.Assert.assertTrue;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {IntegrationTestAntifraudYqlConfig.class})
@ActiveProfiles("integration-tests")
@Slf4j
public class YqlRevalidatorITest {
    @Autowired
    private LoggingJdbcTemplate jdbcTemplate;

    @Autowired
    private YqlRevalidator revalidator;

    @Autowired
    private UnvalidatedDaysHelper unvalidatedDaysHelper;

    @Autowired
    private YtTestDataGenerator testDataGenerator;

    @Before
    public void prepareTestData() {
        testDataGenerator.initOnce();
    }

    @Test
    public void mustRevalidate() {
        jdbcTemplate.exec("truncate step_events");
        jdbcTemplate.exec("truncate sessions");
        int day = 20140328;
        revalidator.revalidate(testDataGenerator.log(), day, "revtest1");
        Set<UnvalidatedDay> unvalidatedDays = unvalidatedDaysHelper.getUnvalidatedDays(testDataGenerator.log());
        assertTrue("Day is not going to be revalidated: " + unvalidatedDays.toString(), unvalidatedDays.contains(
            new UnvalidatedDay(day, -1L, UnvalidatedDay.Scale.ARCHIVE)
        ));
    }

    @Test
    public void mustNotRevalidate() {
        jdbcTemplate.exec("truncate step_events");
        jdbcTemplate.exec("truncate sessions");
        int day = IntDateUtil.todayInt();
        revalidator.revalidate(testDataGenerator.log(), day, "revtest1");
        Set<UnvalidatedDay> unvalidatedDays = unvalidatedDaysHelper.getUnvalidatedDays(testDataGenerator.log());
        assertTrue("Day is going to be revalidated: " + unvalidatedDays.toString(), !unvalidatedDays.contains(
            new UnvalidatedDay(day, -1L, UnvalidatedDay.Scale.ARCHIVE)
        ));
    }
}
