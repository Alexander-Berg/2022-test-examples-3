package ru.yandex.market.checkout.checkouter.monitoring.log;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.log.Loggers;
import ru.yandex.market.checkout.common.tasks.ZooTask;
import ru.yandex.market.checkout.common.util.TestAppender;
import ru.yandex.market.checkout.helpers.ReturnHelper;
import ru.yandex.market.checkout.providers.BlueParametersProvider;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class UnfinishedReturnsLoggerTest extends AbstractWebTestBase {

    @Autowired
    private ZooTask unfinishedReturnsTask;
    @Autowired
    private ReturnHelper returnHelper;
    private TestAppender appender = new TestAppender();
    private Level oldLevel;

    @BeforeEach
    public void setup() {
        Logger logger = ((ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Loggers.KEY_VALUE_LOG));
        logger.addAppender(appender);
        oldLevel = logger.getLevel();
        logger.setLevel(Level.INFO);
    }

    @AfterEach
    public void tearDown() {
        Logger logger = ((ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Loggers.KEY_VALUE_LOG));
        logger.detachAppender(appender);
        logger.setLevel(oldLevel);
    }

    @Test
    public void shouldExecuteCorrectly() {
        returnHelper.createOrderAndReturn(BlueParametersProvider.defaultBlueOrderParameters(), null);
        unfinishedReturnsTask.runOnce();
        assertTrue(appender.getLog().stream()
                .map(ILoggingEvent::getMessage)
                .anyMatch(m -> m.contains("unfinished_returns")));
    }

}
