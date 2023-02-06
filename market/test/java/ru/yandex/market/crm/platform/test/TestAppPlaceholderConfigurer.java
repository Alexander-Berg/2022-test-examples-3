package ru.yandex.market.crm.platform.test;

import java.util.Properties;

import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.core.io.ClassPathResource;

import ru.yandex.market.mcrm.http.Service;

/**
 * @author apershukov
 */
public class TestAppPlaceholderConfigurer extends PropertyPlaceholderConfigurer {

    public TestAppPlaceholderConfigurer(String configFlleName) {
        Properties serviceProps = new Properties();
        for (Service service : Service.values()) {
            serviceProps.setProperty(service.getUrlParamName(), service.getBaseUrl());
        }
        setProperties(serviceProps);

        setLocations(new ClassPathResource(configFlleName));
        setFileEncoding("UTF-8");
        setLocalOverride(true);
        setIgnoreUnresolvablePlaceholders(true);
        setIgnoreResourceNotFound(true);
    }
}
