package ru.yandex.market.api.server.warmup;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.jetbrains.annotations.NotNull;
import org.junit.Test;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ConfigurableApplicationContext;

import ru.yandex.devtools.test.Paths;
import ru.yandex.market.api.AppProperties;
import ru.yandex.market.api.config.AppPropertySourcesPlaceholderConfigurer;
import ru.yandex.market.api.controller.jackson.ObjectMapperFactory;
import ru.yandex.market.api.integration.ContainerTestBase;
import ru.yandex.market.api.server.ApplicationContextHolder;
import ru.yandex.market.api.server.Environment;
import ru.yandex.market.api.util.Urls;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.both;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isEmptyOrNullString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.stringContainsInOrder;
import static org.junit.Assert.assertThat;
import static ru.yandex.market.api.ApiMatchers.map;

public class BackendCheckConfigTest extends ContainerTestBase {
    @Inject
    private AppProperties properties;

    @Inject
    private ObjectMapperFactory objectMapperFactory;

    @Inject
    private ConfigurableListableBeanFactory beanFactory;

    @Inject
    private ConfigurableApplicationContext applicationContext;

    @Test
    public void getDefaultServiceConfigs() {
        assertThat(BackendCheckConfig.getDefaultServiceConfigs(properties).collect(Collectors.toList()),
                everyItem(both(hasProperty("serviceName", not(stringContainsInOrder(Arrays.asList("external.", ".url")))))
                        .and(hasProperty("relativeUrl", equalTo("/ping")))
                        .and(hasProperty("ignored", is(false)))));
    }

    @Test
    public void defaultServiceUrls_Production() throws IOException {
        defaultServiceUrls(Environment.PRODUCTION);
    }

    @Test
    public void defaultServiceUrls_Testing() throws IOException {
        defaultServiceUrls(Environment.TESTING);
    }

    @Test
    public void defaultServiceUrls_Local() throws IOException {
        defaultServiceUrls(Environment.LOCAL);
    }

    @Test
    public void defaultServiceUrls_Development() throws IOException {
        defaultServiceUrls(Environment.DEVELOPMENT);
    }

    private void defaultServiceUrls(Environment environment) throws IOException {
        AppProperties properties = getAppProperties(environment);
        if (!properties.getBoolean("backendCheck.enabled")) {
            return;
        }

        assertThat(buildDefaultServiceConfigs(properties),
                everyItem(
                        map(
                                x -> Urls.builder(x.getServiceBaseUrl(properties)),
                                "base url",
                                allOf(hasProperty("path", isEmptyOrNullString()),
                                        hasProperty("queryParams", is(empty()))),
                                BackendServiceCheckConfig::getServiceName
                        )
                )
        );
    }

    @NotNull
    private AppProperties getAppProperties(Environment environment) {
        ApplicationContextHolder.setEnvironment(environment);
        AppPropertySourcesPlaceholderConfigurer configurer = new AppPropertySourcesPlaceholderConfigurer();
        configurer.setEnvironment(applicationContext.getEnvironment());
        configurer.postProcessBeanFactory(beanFactory);
        return new AppProperties(configurer);
    }

    @NotNull
    private Set<BackendServiceCheckConfig> buildDefaultServiceConfigs(AppProperties properties) throws IOException {
        Set<BackendServiceCheckConfig> defaultServiceConfigs = BackendCheckConfig.getDefaultServiceConfigs(properties)
                .collect(Collectors.toSet());

        String backendCheckConfigFile = properties.getString("backendCheck.config.file");
        if (backendCheckConfigFile.startsWith("arcadia/")) {
            backendCheckConfigFile = Paths.getSourcePath(backendCheckConfigFile.substring("arcadia/".length()));
        }
        BackendCheckConfig backendCheckConfig = objectMapperFactory.getJsonObjectMapper()
                .readValue(new File(backendCheckConfigFile), BackendCheckConfig.class);

        defaultServiceConfigs.removeAll(backendCheckConfig.getConfiguredServices());
        return defaultServiceConfigs;
    }
}