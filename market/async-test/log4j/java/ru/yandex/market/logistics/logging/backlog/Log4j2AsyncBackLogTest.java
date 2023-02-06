package ru.yandex.market.logistics.logging.backlog;

import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.ConsoleAppender;
import org.apache.logging.log4j.core.async.AsyncLoggerConfig;
import org.apache.logging.log4j.core.async.AsyncLoggerContext;
import org.apache.logging.log4j.core.config.AppenderRef;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.config.NullConfiguration;
import org.apache.logging.slf4j.Log4jLogger;
import org.junit.jupiter.api.DisplayName;
import org.mockito.stubbing.Answer;
import org.slf4j.Logger;

import ru.yandex.market.logistics.logging.backlog.layout.log4j.BackLogLayout;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.spy;

/*
 * Этот тест может не сработать при запуске в Idea, тогда остается только ya make.
 * Всё из-за того, что идея собирает все PEERDIR модуля вместе и запускает тесты,
 * указывая все зависимости в classpath без учета EXCLUDE для вложенных тест-модулей.
 *
 * Победить можно, отделив как модуль: при генерации проекта через ya ide idea надо
 * добавить ключ --separate-tests-modules.
 * В этом случае каждый тест-модуль будет зарегистрирован как отдельный модуль idea
 * и classpath будет корректный при запуске тестов.
 * Вместе с указанным ключом для больших проектов советую указывать также ключ --group-modules=tree,
 * тогда тестовые модули не будут захламлять project view.
 */
@DisplayName("Тесты на асинхронное логирование через log4j2")
@ParametersAreNonnullByDefault
class Log4j2AsyncBackLogTest extends BackLogAsyncTestBase {

    private LoggerContext loggerContext;

    @Nonnull
    @Override
    protected Logger prepareAsyncLogging(Answer<String> messageCapturingAnswer) {
        loggerContext = new AsyncLoggerContext("AsyncContext");
        loggerContext.start();

        NullConfiguration configuration = new NullConfiguration();
        loggerContext.setConfiguration(configuration);

        BackLogLayout spyLayout = spy(BackLogLayout.createLayout());
        doAnswer(messageCapturingAnswer).when(spyLayout).toSerializable(any(LogEvent.class));

        ConsoleAppender backLogAppender = ConsoleAppender.newBuilder()
            .setName("BackLogAppender")
            .setLayout(spyLayout)
            .build();
        backLogAppender.start();

        LoggerConfig loggerConfig = AsyncLoggerConfig.createLogger(
            false,
            Level.ALL,
            "AsyncTesting",
            null,
            new AppenderRef[0],
            null,
            configuration,
            null
        );
        loggerConfig.start();

        loggerConfig.addAppender(backLogAppender, Level.ALL, null);
        configuration.addLogger("AsyncTesting", loggerConfig);

        configuration.start();

        return new Log4jLogger(loggerContext.getLogger("AsyncTesting"), "AsyncTesting");
    }

    @Override
    protected void joinAsyncLogging(Logger logger) {
        loggerContext.stop(1, TimeUnit.SECONDS);
    }
}
