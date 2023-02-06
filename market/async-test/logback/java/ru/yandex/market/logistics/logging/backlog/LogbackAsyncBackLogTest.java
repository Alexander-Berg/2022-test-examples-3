package ru.yandex.market.logistics.logging.backlog;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import ch.qos.logback.classic.AsyncAppender;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.ConsoleAppender;
import org.junit.jupiter.api.DisplayName;
import org.mockito.stubbing.Answer;
import org.slf4j.LoggerFactory;

import ru.yandex.market.logistics.logging.backlog.layout.logback.BackLogLayout;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.spy;

@DisplayName("Тесты на асинхронное логирование через logback")
@ParametersAreNonnullByDefault
class LogbackAsyncBackLogTest extends BackLogAsyncTestBase {

    private AsyncAppender asyncAppender;

    @Nonnull
    @Override
    protected org.slf4j.Logger prepareAsyncLogging(Answer<String> messageCapturingAnswer) {
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        BackLogLayout layout = spy(BackLogLayout.class);
        doAnswer(messageCapturingAnswer).when(layout).doLayout(any(ILoggingEvent.class));
        layout.start();

        ConsoleAppender<ILoggingEvent> consoleAppender = new ConsoleAppender<>();
        consoleAppender.setContext(loggerContext);
        consoleAppender.setLayout(layout);
        consoleAppender.start();

        asyncAppender = new AsyncAppender();
        asyncAppender.setContext(loggerContext);
        asyncAppender.addAppender(consoleAppender);
        asyncAppender.start();


        Logger loggerToTest = loggerContext.getLogger("AsyncTesting");
        loggerToTest.setLevel(Level.ALL);
        loggerToTest.addAppender(asyncAppender);

        return loggerToTest;
    }

    @Override
    protected void joinAsyncLogging(org.slf4j.Logger logger) {
        ((Logger) logger).detachAppender(asyncAppender);
        asyncAppender.stop();
    }

}
