package ru.yandex.market.abo.debug;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import ru.yandex.market.abo.util.trace.AboTraceHelper;
import ru.yandex.market.application.properties.AppPropertyContextInitializer;

/**
 * @author Vasiliy Briginets (0x40@yandex-team.ru)
 */
public class DebugMain {

    private static final Logger log = LoggerFactory.getLogger(DebugMain.class);

    private DebugMain() {
    }

    public static void main(String[] args) {
        System.setProperty("ru.yandex.market.checkout.common.LogInitiallizerClass", "ru.yandex.common.util.application.LoggerInitializer");
        System.setProperty("log4j2.contextSelector", "org.apache.logging.log4j.core.async.AsyncLoggerContextSelector',");
        System.setProperty("environment", "development");
        System.setProperty("spring.profiles.active", "development");
        System.setProperty("host.name", "development");
        System.setProperty("host.fqdn", "localhost");
        System.setProperty("configs.path", "./abo-main/src/main/properties.d");
        System.setProperty("javax.xml.transform.TransformerFactory", "net.sf.saxon.TransformerFactoryImpl");
        System.setProperty(AboTraceHelper.WRITE_TRACE, "false");

        ClassPathXmlApplicationContext applicationContext = new ClassPathXmlApplicationContext();
        applicationContext.registerShutdownHook();
        applicationContext.setConfigLocation("bean.xml");

        AppPropertyContextInitializer initializer = new AppPropertyContextInitializer();
        initializer.initialize(applicationContext);

        try {
            applicationContext.refresh();
            log.info("Abo-main up and running");
        } catch (Exception e) {
            log.error("Abo-main failed to start", e);
            System.exit(1);
        }

        Thread.setDefaultUncaughtExceptionHandler(
                (t, e) -> log.error("Uncaught Exception in thread " + t.toString(), e));
    }
}
