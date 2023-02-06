package ru.yandex.market.checkout.common.logging;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.status.OnConsoleStatusListener;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;

import static org.junit.jupiter.api.Assertions.assertEquals;


class TamperedTimestampLoggerTest {

    @Captor
    private ArgumentCaptor<ILoggingEvent> captor;

    @Mock
    private Appender<ILoggingEvent> appender;

    @BeforeEach
    public void setupAppender() {
        MockitoAnnotations.initMocks(this);

        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();

        final OnConsoleStatusListener statusListener = new OnConsoleStatusListener();
        statusListener.setContext(context);
        statusListener.start();
        context.getStatusManager().add(statusListener);

        appender.setContext(context);
        appender.start();
        context.getLogger("ROOT").addAppender(appender);
    }

    @AfterEach
    public void teardownAppender() {
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        context.reset();

        context.getLogger("ROOT").detachAppender(appender);
    }

    @Test
    void testTamperTimestamp() {
        final TamperedTimestampLogger timestampLogger = TamperedTimestampLogger.getLogger("com.foo.bar");

        final long tamperedTimestamp = System.currentTimeMillis() + 60 * 60 * 1000;
        timestampLogger.log(tamperedTimestamp, Level.INFO, "Hello");

        Mockito.verify(appender).doAppend(captor.capture());
        final ILoggingEvent actualLoggedEvent = captor.getValue();
        assertEquals("Hello", actualLoggedEvent.getMessage());
        assertEquals(tamperedTimestamp, actualLoggedEvent.getTimeStamp());
    }
}
