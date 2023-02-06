package ru.yandex.market.pharmatestshop.config;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import ru.yandex.market.application.MarketApplicationCommonConfig;
import ru.yandex.market.application.monitoring.MonitoringController;
import ru.yandex.market.request.trace.Module;

/**
 * The spring application config.
 */
@Configuration
@ComponentScan("ru.yandex.market.pharmatestshop.domain")
@Import({
        PharmaDatasourceConfiguration.class,
        PharmaLiquibaseConfiguration.class,
        PharmaDbConfiguration.class,
        QuartzTasksConfiguration.class,
})
public class SpringApplicationConfig extends MarketApplicationCommonConfig implements ApplicationContextAware {

    private ApplicationContext context;

    public SpringApplicationConfig() {
        super(Module.MARKET_PHARMA_TEST_SHOP, false);
    }

    @Override
    public void setApplicationContext(ApplicationContext context) throws BeansException {
        this.context = context;
    }

    @Override
    public MonitoringController monitoringController() {
        return new ApplicationMonitoringController(this.monitoring(), this.ping(), context);
    }
}
