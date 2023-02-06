package ru.yandex.direct.manualtests.app;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import ru.yandex.direct.jcommander.ParserWithHelp;
import ru.yandex.direct.logging.LoggingInitializer;
import ru.yandex.direct.logging.LoggingInitializerParams;
import ru.yandex.direct.manualtests.configuration.DefaultAppConfiguration;
import ru.yandex.direct.tracing.TraceHelper;

public class TestTasksRunner {
    public static <T extends Runnable> void runTask(Class<T> clazz, String[] args) {
        runTask(clazz, DefaultAppConfiguration.class, args);
    }

    public static <T extends Runnable> void runTask(Class<T> clazz, Class<?> configurationClass, String[] args) {
        var loggingParams = new LoggingInitializerParams();
        ParserWithHelp.parse(clazz.getCanonicalName(), args, loggingParams);
        LoggingInitializer.initialize(loggingParams, "direct.manual-tests");
        try (var ctx = new AnnotationConfigApplicationContext(configurationClass)) {
            try (var guard = ctx.getBean(TraceHelper.class).guard(clazz.getSimpleName())) {
                ctx.getBean(clazz).run();
            }
        }
    }
}
