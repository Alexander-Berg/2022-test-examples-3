package ru.yandex.market.pricelabs;

import java.time.Instant;

import javax.validation.Validation;
import javax.validation.Validator;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextTestExecutionListener;

import ru.yandex.market.pricelabs.misc.TimeSource;
import ru.yandex.market.pricelabs.misc.TimingUtils;


@ExtendWith(SpringExtension.class)
@TestExecutionListeners({
        DirtiesContextTestExecutionListener.class,
        DependencyInjectionTestExecutionListener.class,
        RandomizedTestListener.class,
        YaMakeTestListener.class,
        LoggingTestListener.class,
        TimingContextListener.class,
        SpringCacheResetListener.class
})
@ContextConfiguration(classes = {CoreConfigurationForTests.Basic.class})
@ActiveProfiles("unittest")
@TestMethodOrder(MethodOrderer.Random.class)
public abstract class AbstractSpringConfiguration {

    private static class Holder {
        private static final Validator VALIDATOR = Validation.buildDefaultValidatorFactory().getValidator();
    }

    protected static Validator getValidator() {
        return Holder.VALIDATOR;
    }

    protected static TimeSource timeSource() {
        return TimingUtils.timeSource();
    }

    protected static Instant getInstant() {
        return TimingUtils.getInstant();
    }


}
