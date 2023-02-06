package ru.yandex.market.billing.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import ru.yandex.market.billing.FunctionalTest;
import ru.yandex.market.tms.quartz2.service.TmsMonitoringService;
import ru.yandex.market.tms.quartz2.spring.AnnotatedTriggersFactory;

/**
 * @author Georgiy Klimov gaklimov@yandex-team.ru
 */
public class BillingCronExpressionsTest extends FunctionalTest {

    @Autowired
    ApplicationContext applicationContext;

    @Autowired
    TmsMonitoringService tmsMonitoringService;

    @Test
    void testCronExpressions() {
        AnnotatedTriggersFactory productionTriggerFactory =
                new AnnotatedTriggersFactory(() -> false, applicationContext, tmsMonitoringService);

        // Если есть проблемы, здесь вылелетает исключение CronTriggerCreationException с сообщением вида
        // Could not create cron trigger transactionLogYtExportExecutor with expression * 0/30 * * * 2099
        productionTriggerFactory.getAnnotatedTriggers();
    }
}
