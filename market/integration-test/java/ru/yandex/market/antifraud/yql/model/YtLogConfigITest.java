package ru.yandex.market.antifraud.yql.model;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import ru.yandex.market.antifraud.yql.config.IntegrationTestAntifraudYqlConfig;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {IntegrationTestAntifraudYqlConfig.class})
@ActiveProfiles("integration-tests")
@Slf4j
public class YtLogConfigITest {
    @Autowired
    private YtLogConfig showsLogConfig;

    @Test
    public void testScales() {
        log.info(showsLogConfig.getScales().toString());
        assertThat(showsLogConfig.scaleTypeFromString("30min"), is(UnvalidatedDay.Scale.RECENT));
        assertThat(showsLogConfig.scaleTypeFromString("1d"), is(UnvalidatedDay.Scale.ARCHIVE));
    }
}
