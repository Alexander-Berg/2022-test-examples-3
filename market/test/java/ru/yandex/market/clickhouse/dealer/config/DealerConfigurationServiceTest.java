package ru.yandex.market.clickhouse.dealer.config;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertyResolver;
import org.springframework.core.env.PropertySourcesPropertyResolver;
import org.springframework.core.io.support.ResourcePropertySource;

import ru.yandex.market.application.monitoring.ComplicatedMonitoring;

public class DealerConfigurationServiceTest {

    @Test
    public void parseConfigsWhenOnlyActiveParsingEnable() throws Exception {
        DealerConfigurationService service = buildDealerConfigurationService(true);

        Assert.assertEquals(1, service.getConfigs().size());
        Assert.assertEquals("active-config-example.json", service.getConfigs().get(0).getConfigName());
    }

    @Test
    public void parseConfigsWhenOnlyActiveParsingDisable() throws Exception {
        DealerConfigurationService service = buildDealerConfigurationService(false);

        Assert.assertEquals(2, service.getConfigs().size());
        List<String> actualConfigNames = service.getConfigs()
            .stream()
            .map(DealerConfig::getConfigName)
            .sorted()
            .collect(Collectors.toList());
        Assert.assertEquals("active-config-example.json", actualConfigNames.get(0));
        Assert.assertEquals("inactive-config-example.json", actualConfigNames.get(1));
    }

    private DealerConfigurationService buildDealerConfigurationService(boolean isOnlyActiveConfigEnable)
        throws Exception {
        ClassLoader classLoader = getClass().getClassLoader();
        File file = new File(classLoader.getResource("config/dealer").getFile());
        DealerGlobalConfig onlyActiveConfigParsing =
            DealerGlobalConfig.newBuilder().withOnlyActiveConfigFileParsing(isOnlyActiveConfigEnable).build();
        DealerConfigParser parser = dealerConfigParser("/config/properties-example.properties",
            onlyActiveConfigParsing);
        ComplicatedMonitoring monitoring = new ComplicatedMonitoring();
        return new DealerConfigurationService(parser, file.getAbsolutePath(), Collections.emptySet(), monitoring, 1);
    }

    private DealerConfigParser dealerConfigParser(String propertiesPath, DealerGlobalConfig globalConfig)
        throws Exception {
        ResourcePropertySource propertySource = new ResourcePropertySource(propertiesPath);
        MutablePropertySources propertySources = new MutablePropertySources();
        propertySources.addFirst(propertySource);
        PropertyResolver propertyResolver = new PropertySourcesPropertyResolver(propertySources);
        return new DealerConfigParser(propertyResolver::resolveRequiredPlaceholders, globalConfig);
    }
}
