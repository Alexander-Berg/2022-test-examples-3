package ru.yandex.market.reporting.resource;

import lombok.extern.log4j.Log4j2;
import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import ru.yandex.market.reporting.config.TestingSmokeTestConfig;

@Ignore("Manual use only")
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TestingSmokeTestConfig.class)
@Log4j2
public class ReportingApiV1ServiceTestingSmokeITest extends ReportingApiV1ServiceITestCommon {

}
