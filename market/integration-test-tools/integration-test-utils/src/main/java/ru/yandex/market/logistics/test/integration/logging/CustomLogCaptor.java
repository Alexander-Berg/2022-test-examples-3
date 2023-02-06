package ru.yandex.market.logistics.test.integration.logging;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.ConsoleAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.layout.AbstractStringLayout;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

/**
 * Расширение для захвата логирования через log4j.
 * Работает по принципу tee, перехватывая строки, прошедшие форматирование перед выводом,
 * и записывая к себе в список, который в свою очередь после каждого теста очищается.
 */
@Getter
@RequiredArgsConstructor
@ParametersAreNonnullByDefault
public class CustomLogCaptor<T extends AbstractStringLayout>
    implements Answer<String>, BeforeEachCallback, AfterEachCallback {

    private static final String SPY_APPENDER = "SPY_CUSTOM_LOG_APPENDER";

    @Nullable
    private final String loggerName;
    private final String loggerAppenderName;
    private final Level level;

    private final List<String> results = new ArrayList<>();

    @Override
    public String answer(InvocationOnMock invocationOnMock) throws Throwable {
        String currentResult = (String) invocationOnMock.callRealMethod();
        results.add(currentResult);
        return currentResult;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void beforeEach(ExtensionContext extensionContext) {
        LoggerContext context = getLoggerContext();
        Configuration configuration = context.getConfiguration();

        T logLayout = (T) configuration.getAppender(loggerAppenderName).getLayout();
        T spyLayout = Mockito.spy(logLayout);

        ConsoleAppender consoleAppender = ConsoleAppender.newBuilder()
            .setName(SPY_APPENDER)
            .setLayout(spyLayout)
            .build();
        consoleAppender.start();

        getLoggerConfig(context).addAppender(consoleAppender, level, null);
        context.updateLoggers();

        doAnswer(this).when(spyLayout).toSerializable(any(LogEvent.class));
    }

    @Override
    public void afterEach(ExtensionContext extensionContext) {
        results.clear();
        LoggerContext context = getLoggerContext();
        LoggerConfig logger = getLoggerConfig(context);
        logger.getAppenders().get(SPY_APPENDER).stop();
        logger.removeAppender(SPY_APPENDER);
        context.updateLoggers();
    }

    @Nonnull
    private LoggerConfig getLoggerConfig(LoggerContext context) {
        if (loggerName != null) {
            return context.getConfiguration().getLoggerConfig(loggerName);
        }
        return context.getConfiguration().getRootLogger();
    }

    private LoggerContext getLoggerContext() {
        return LoggerContext.getContext(false);
    }
}
