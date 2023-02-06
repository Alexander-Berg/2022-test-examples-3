package ru.yandex.calendar.test.generic;

import javax.sql.DataSource;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.SneakyThrows;
import lombok.val;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;

import ru.yandex.calendar.test.CalendarTestBase;
import ru.yandex.calendar.util.db.CalendarJdbcTemplate;
import ru.yandex.misc.db.embedded.ActivateEmbeddedPg;
import ru.yandex.misc.log.mlf.Logger;
import ru.yandex.misc.log.mlf.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A test case with configuration and logger
 */
@RunWith(CalendarSpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TestBaseContextConfiguration.class)
@TestExecutionListeners({
        DependencyInjectionTestExecutionListener.class
})
@ActivateEmbeddedPg
public abstract class AbstractConfTest extends CalendarTestBase {
    protected static final Logger logger = LoggerFactory.getLogger(AbstractConfTest.class);

    @Autowired
    protected DataSource dataSource;
    @Autowired
    protected MeterRegistry registry;
    @Autowired
    protected CalendarJdbcTemplate jdbcTemplate;

    @Before
    @SneakyThrows
    public void setUpAll() {
        registry.clear();
    }

    private Double extractCounterValue(Meter m) {
        return m.measure().iterator().next().getValue();
    }

    protected void checkCounterValue(String signalName, double value) {
        val reqCounter = registry.getMeters().stream()
                .filter(m -> m.getId().getName().equals(signalName)).findFirst();
        assertThat(reqCounter).map(this::extractCounterValue).hasValue(value);
    }

    protected void checkTimer(String signalName) {
        val timeMetric = registry.getMeters().stream()
                .filter(m -> m.getId().getName().equals(signalName)).findFirst();
        assertThat(timeMetric).isNotEmpty();
    }
}
