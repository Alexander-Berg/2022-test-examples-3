package ru.yandex.market.crm.lb.test;

import java.util.Properties;

import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;

/**
 * @author apershukov
 */
class TestPlaceholderConfigurer extends PropertyPlaceholderConfigurer {

    TestPlaceholderConfigurer() {
        Properties properties = new Properties();
        properties.setProperty("logBroker.clientId", "client-id");
        properties.setProperty("logBroker.enabled", "true");
        properties.setProperty("logBroker.safe.interval.size", "10");
        properties.setProperty("logbroker.parallel.interval.size", "4");
        properties.setProperty("logBroker.workers.pool.size", "3");
        properties.setProperty("logBroker.error.delay", "10");
        properties.setProperty("sql.changelog", "/sql/liquibase/test-changelog.xml");
        properties.setProperty("sql.schema", "lb_test");
        setProperties(properties);
        setIgnoreUnresolvablePlaceholders(true);
    }
}
