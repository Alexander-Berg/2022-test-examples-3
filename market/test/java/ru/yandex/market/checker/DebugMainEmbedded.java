package ru.yandex.market.checker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import ru.yandex.market.application.AppPropertyContextInitializer;

/**
 * Пишет логи в urlchecker.log
 * Наливает базу (см. insert into uc_net_check) и пингует тестовые сайты.
 * TODO локально на базе h2 не работает
 * DELETE FROM uc_net_check WHERE ((creation_time  < sysdate - ?) AND rownum <= 10000);
 * SQL state [HY004]; error code [50004-193]; Неизвестный тип данных: "?"
 *
 * @author Vasiliy Briginets (0x40@yandex-team.ru)
 */
public class DebugMainEmbedded {
    private static final Logger log = LoggerFactory.getLogger(DebugMainEmbedded.class);

    private DebugMainEmbedded() {
    }

    public static void main(String[] args) {
        System.setProperty("environment", "development");
        System.setProperty("host.name", "development");
        System.setProperty("javax.xml.transform.TransformerFactory", "net.sf.saxon.TransformerFactoryImpl");

        ClassPathXmlApplicationContext applicationContext = new ClassPathXmlApplicationContext();
        applicationContext.registerShutdownHook();
        applicationContext.setConfigLocation("test-bean.xml");

        AppPropertyContextInitializer initializer = new AppPropertyContextInitializer();
        initializer.initialize(applicationContext);
        try {
            applicationContext.refresh();
            log.info("UrlCheckerDebug up and running");
        } catch (Exception e) {
            log.error("UrlCheckerDebug failed to start", e);
            System.exit(1);
        }

        Thread.setDefaultUncaughtExceptionHandler(
                (t, e) -> log.error("Uncaught Exception in thread " + t.toString(), e));
    }
}
