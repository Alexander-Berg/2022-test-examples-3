package ru.yandex.market.crm.platform.reader.test;

import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;

import java.util.Properties;

public class PropertiesConfig extends PropertyPlaceholderConfigurer {

    PropertiesConfig() {
        Properties properties = new Properties();
        properties.setProperty("sql.changelog", "/sql/liquibase/test-changelog.xml");
        setProperties(properties);
    }
}
