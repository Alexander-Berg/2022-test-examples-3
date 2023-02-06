package ru.yandex.market.checkout.checkouter.trace;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.slf4j.LoggerFactory;

import ru.yandex.market.checkout.application.AbstractContainerTestBase;
import ru.yandex.market.checkout.carter.InMemoryAppender;
import ru.yandex.market.checkout.checkouter.log.Loggers;

public abstract class AbstractTraceLogTestBase extends AbstractContainerTestBase {

    public static final Logger LOG = ((Logger) LoggerFactory.getLogger(Loggers.REQUEST_TRACE));
    protected InMemoryAppender inMemoryAppender;
    private Level oldLevel;

    @BeforeEach
    public void setUp() {
        inMemoryAppender = new InMemoryAppender();
        inMemoryAppender.start();

        LOG.addAppender(inMemoryAppender);
        oldLevel = LOG.getLevel();
        LOG.setLevel(Level.TRACE);
    }

    @AfterEach
    public void tearDown() {
        LOG.detachAppender(inMemoryAppender);
        LOG.setLevel(oldLevel);
    }
}
