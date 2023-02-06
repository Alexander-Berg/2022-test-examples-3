package ru.yandex.direct.common.db;

import java.time.Duration;
import java.time.Instant;

import javax.annotation.ParametersAreNonnullByDefault;

import org.jooq.impl.DSL;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.common.testing.CommonTest;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;

import static org.hamcrest.Matchers.lessThan;
import static org.junit.Assert.assertThat;

@ParametersAreNonnullByDefault
@CommonTest
@RunWith(SpringRunner.class)
@Ignore("DIRECT-65875")
public class TimeDriftTest {
    @Autowired
    private DslContextProvider dslContextProvider;

    @Test
    public void testTimeDrift() {
        Instant dbTime = dslContextProvider.ppcdict()
                .select(DSL.currentTimestamp())
                .fetchOne()
                .value1()
                .toInstant();

        Instant clientTime = Instant.now();

        assertThat("время в приложении и SQL-сервере отличается не больше, чем на несколько минут",
                Duration.between(dbTime, clientTime).abs(),
                lessThan(Duration.ofMinutes(10)));
    }
}
