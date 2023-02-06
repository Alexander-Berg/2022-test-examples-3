package ru.yandex.market.logistics.lom.logging;

import java.util.List;
import java.util.regex.Pattern;

import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.ConsoleAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.SimpleMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

import ru.yandex.market.logistics.logging.backlog.BackLogWrapper;
import ru.yandex.market.logistics.logging.backlog.layout.log4j.BackLogLayout;
import ru.yandex.market.logistics.lom.exception.LomException;
import ru.yandex.market.logistics.lom.logging.enums.OrderEventCode;
import ru.yandex.market.request.trace.RequestContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

@Slf4j
@ExtendWith(MockitoExtension.class)
class BackLogLayoutConfigTest {

    private static final String ERROR_MARK = "level=ERROR";
    private static final String TRACE_MARK = "level=TRACE";
    private static final Pattern LOG_PATTERN = Pattern.compile(".*ts.*level.*format.*code.*payload.*");

    private LoggerContext context;

    private Configuration configuration;

    private Appender backLogAppender;

    private BackLogLayout backLogLayout;

    @BeforeEach
    void setUp() {
        context = LoggerContext.getContext(false);
        configuration = context.getConfiguration();
        backLogAppender = configuration.getAppender("BACK_LOG");
        backLogLayout = (BackLogLayout) backLogAppender.getLayout();
    }

    @Test
    void xmlConfigurationTest() {
        assertThat(backLogAppender).describedAs("BackLog appended is loaded").isNotNull();
        assertThat(backLogLayout).describedAs("BackLogLayout is loaded").isNotNull();
    }

    @Test
    void errorMappingTest() {
        LogEvent fatalEvent = getEventWithLevel(
            Level.FATAL,
            new SimpleMessage("Fatal test message"),
            new LomException("Lom test exception")
        );

        assertThat(backLogLayout.toSerializable(fatalEvent)).contains(ERROR_MARK);
    }

    @Test
    void traceMappingTest() {
        LogEvent offEvent = getEventWithLevel(Level.OFF, new SimpleMessage("Off test message"), null);
        LogEvent traceEvent = getEventWithLevel(Level.TRACE, new SimpleMessage("Trace test message"), null);

        assertThat(backLogLayout.toSerializable(offEvent)).contains(TRACE_MARK);
        assertThat(backLogLayout.toSerializable(traceEvent)).contains(TRACE_MARK);
    }

    @Test
    void errorCodeAndPayloadTest() {
        LogEvent errorEvent = getEventWithLevel(
            Level.ERROR,
            new SimpleMessage("Error test msg"),
            new LomException("Lom exception")
        );

        assertThat(backLogLayout.toSerializable(errorEvent))
            .contains("code=ru.yandex.market.logistics.lom.exception.LomException")
            .contains("stackTrace");
    }

    @Test
    void infoCodeAndPayloadTest() {
        LogEvent infoEvent = getEventWithLevel(Level.INFO, new SimpleMessage("Info test msg"), null);
        assertThat(backLogLayout.toSerializable(infoEvent)).contains("payload=Info test msg");
    }

    @Test
    void consistencyTest() {
        LogEvent errorEvent = getEventWithLevel(
            Level.ERROR,
            new SimpleMessage("Error test msg"),
            new LomException("Lom Exception")
        );

        String firstSerialization = backLogLayout.toSerializable(errorEvent);
        String secondSerialization = backLogLayout.toSerializable(errorEvent);

        assertThat(firstSerialization).containsPattern(LOG_PATTERN);
        assertThat(secondSerialization).containsPattern(LOG_PATTERN);
    }

    @Test
    void wrapInfoTest() {
        BackLogLayout spyLayout = Mockito.spy(backLogLayout);

        ConsoleAppender consoleAppender = ConsoleAppender.newBuilder()
            .setName("test")
            .setLayout(spyLayout)
            .build();

        LoggerConfig logger = configuration.getLoggerConfig("ru.yandex.market.logistics.lom");
        logger.addAppender(consoleAppender, Level.INFO, null);
        logger.removeAppender("BACK_LOG");
        context.updateLoggers();

        ResultCaptor<String> resultCaptor = new ResultCaptor<>();

        doAnswer(resultCaptor).when(spyLayout).toSerializable(any(LogEvent.class));

        RequestContextHolder.createContext("testId");

        BackLogWrapper.of(log)
            .withEntities("order", List.of("1", "3", "4"))
            .withCode(OrderEventCode.CREATE_ORDER_EXTERNAL_ERROR)
            .info("Info message");

        RequestContextHolder.clearContext();

        assertThat(resultCaptor.getResult())
            .contains(
                "request_id=testId",
                "entity_types=order",
                "entity_values=order:1,order:3,order:4",
                "code=CREATE_ORDER_EXTERNAL_ERROR"
            );
    }

    private LogEvent getEventWithLevel(Level level, Message msg, Throwable t) {
        return Log4jLogEvent.newBuilder()
            .setLoggerName("ru.yandex.market.logistics.lom")
            .setLevel(level)
            .setMessage(msg)
            .setThrown(t)
            .build();
    }

    private static class ResultCaptor<T> implements Answer<T> {
        private T result = null;

        public T getResult() {
            return result;
        }

        @SuppressWarnings("unchecked")
        @Override
        public T answer(InvocationOnMock invocationOnMock) throws Throwable {
            result = (T) invocationOnMock.callRealMethod();
            return result;
        }
    }

}
