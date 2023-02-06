package ru.yandex.market.crm.campaign.test;

import java.util.Enumeration;
import java.util.Properties;

import javax.annotation.Nonnull;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StringValueResolver;

/**
 * @author apershukov
 */
public class TestPropertyPlaceholderConfigurer extends PropertyPlaceholderConfigurer {

    private Properties properties;

    public TestPropertyPlaceholderConfigurer() {

        setLocations(
            new ClassPathResource("mcrm_int_test.properties"),
            new ClassPathResource("test_db_support.properties"),
            new ClassPathResource("transactions.properties")
        );

        setFileEncoding("UTF-8");
        setLocalOverride(true);
        setIgnoreUnresolvablePlaceholders(true);
        setIgnoreResourceNotFound(true);
    }

    public Properties getProperties() {
        return properties;
    }

    @Override
    protected void processProperties(@Nonnull ConfigurableListableBeanFactory beanFactoryToProcess,
                                     @Nonnull Properties props) throws BeansException {
        this.properties = new Properties(props);
        super.processProperties(beanFactoryToProcess, props);
    }

    @Override
    protected void doProcessProperties(ConfigurableListableBeanFactory beanFactoryToProcess,
                                       StringValueResolver valueResolver) {
        super.doProcessProperties(beanFactoryToProcess, valueResolver);
        for (Enumeration<?> e = this.properties.propertyNames(); e.hasMoreElements(); ) {
            String propName = (String) e.nextElement();
            this.properties.setProperty(propName, valueResolver.resolveStringValue("${" + propName + "}"));
        }
    }
}
