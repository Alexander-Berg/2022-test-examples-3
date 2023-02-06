package ru.yandex.market.logistics.logging.trace;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.Appender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;

import ru.yandex.market.logistics.logging.AbstractTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class TracingLogUtilTest extends AbstractTest {

    private Appender traceAppender;

    @BeforeEach
    public void setUp() {
        traceAppender = mock(Appender.class);

        Logger logger = (((Logger) LoggerFactory.getLogger("requestTrace")));
        logger.addAppender(traceAppender);
        logger.setLevel(Level.TRACE);
    }

    @Test
    public void checkInRecordWasWritten() {
        TracingLogUtil.executeAndWriteInRecord(
                () -> { },
                builder -> builder.addKeyValue("someKey", "someValue")
        );

        ArgumentCaptor<Appender> argumentCaptor = ArgumentCaptor.forClass(Appender.class);
        verify(traceAppender).doAppend(argumentCaptor.capture());

        assertEquals(1, argumentCaptor.getAllValues().size());
        String message = ((LoggingEvent) argumentCaptor.getAllValues().get(0)).getMessage();
        assertTrue(message.contains("type=IN"));
        assertTrue(message.contains("kv.someKey=someValue"));
        assertFalse(message.contains("error_code"));
    }

    @Test
    public void checkInRecordWithErrorWasWritten() {
        try {
            TracingLogUtil.executeAndWriteInRecord(
                    () -> {
                        throw new RuntimeException("some exception");
                        },
                    builder -> { }
            );
        } catch (Exception e) {
            //do nothing
        }

        ArgumentCaptor<Appender> argumentCaptor = ArgumentCaptor.forClass(Appender.class);
        verify(traceAppender).doAppend(argumentCaptor.capture());

        assertEquals(1, argumentCaptor.getAllValues().size());
        String message = ((LoggingEvent) argumentCaptor.getAllValues().get(0)).getMessage();
        assertTrue(message.contains("type=IN"));
        assertTrue(message.contains("error_code=some exception"));
    }
}
