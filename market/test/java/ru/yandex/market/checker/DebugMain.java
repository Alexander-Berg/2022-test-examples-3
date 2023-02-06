package ru.yandex.market.checker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import ru.yandex.market.application.AppPropertyContextInitializer;

/**
 * @author komarovns
 * @date 10.10.18
 */
public class DebugMain {
    private static final Logger log = LoggerFactory.getLogger(DebugMain.class);

    private DebugMain() {
    }

    public static void main(String[] args) {
        System.setProperty(
                "ru.yandex.market.checkout.common.LogInitiallizerClass",
                "ru.yandex.common.util.application.LoggerInitializer");
        System.setProperty(
                "log4j2.contextSelector",
                "org.apache.logging.log4j.core.async.AsyncLoggerContextSelector");
        System.setProperty("configs.path", "urlchecker/src/main/properties.d");

        ClassPathXmlApplicationContext applicationContext = new ClassPathXmlApplicationContext();
        applicationContext.registerShutdownHook();
        applicationContext.setConfigLocation("bean.xml");

        AppPropertyContextInitializer initializer = new AppPropertyContextInitializer();
        initializer.initialize(applicationContext);

        applicationContext.refresh();

        Thread.setDefaultUncaughtExceptionHandler(
                (t, e) -> log.error("Uncaught Exception in thread " + t.toString(), e));
    }
}
