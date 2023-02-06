package ru.yandex.market.replenishment.autoorder.config;

import com.google.common.base.Charsets;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.core.io.ClassPathResource;

public class UnitTestPlaceholderConfigurer extends PropertyPlaceholderConfigurer {

    private static final String PROPERTIES_FILE = "functional-test.properties";

    public UnitTestPlaceholderConfigurer() {
        setLocations(new ClassPathResource(PROPERTIES_FILE));
        setFileEncoding(Charsets.UTF_8.displayName());
        setLocalOverride(true);
        setIgnoreUnresolvablePlaceholders(true);
        setIgnoreResourceNotFound(true);
    }
}
