package ru.yandex.market.stat.dicts.loaders.loadchecks;

import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.stat.utils.DateUtil;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

public class PeriodicLoadCheckTest {

    private static final Clock TEST_CLOCK = DateUtil.fixedClock("2018-05-02T01:00:00");

    @Before
    public void setUp() {
        PeriodicLoadCheck.clock = TEST_CLOCK;
    }

    @Test
    public void needLoad() {
        PeriodicLoadCheck check = new PeriodicLoadCheck(Duration.ofHours(24));

        assertThat(check.needLoad(LocalDateTime.parse("2018-05-01T00:59:59")), equalTo(true));
        assertThat(check.needLoad(LocalDateTime.parse("2018-05-01T01:00:00")), equalTo(true));
        assertThat(check.needLoad(LocalDateTime.parse("2018-05-01T01:00:59")), equalTo(true));
        assertThat(check.needLoad(LocalDateTime.parse("2018-05-01T01:01:00")), equalTo(false));
    }
}
