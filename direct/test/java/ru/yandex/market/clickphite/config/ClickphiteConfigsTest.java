package ru.yandex.market.clickphite.config;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.PropertySource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import ru.yandex.market.clickphite.QueryBuilder;
import ru.yandex.market.clickphite.config.metric.GraphiteMetricConfig;
import ru.yandex.market.clickphite.config.metric.MetricField;
import ru.yandex.market.clickphite.config.metric.StatfaceReportConfig;
import ru.yandex.market.clickphite.config.validation.DashboardValidator;
import ru.yandex.market.clickphite.config.validation.MetricFieldValidator;
import ru.yandex.market.clickphite.config.validation.context.ConfigValidationException;
import ru.yandex.market.clickphite.config.validation.context.ConfigValidator;

import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Anton Sukhonosenko <a href="mailto:algebraic@yandex-team.ru"></a>
 * @date 28.11.16
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {TestConfiguration.class, ClickphiteConfigsTest.Config.class})
public class ClickphiteConfigsTest {
    private static final Logger log = LogManager.getLogger();

    @Autowired
    private ConfigurationService configurationService;

    @Test
    public void validateConfigs() throws ConfigValidationException {
        ClickphiteConfiguration configuration = configurationService.getConfiguration();

        log.info("Metric context groups count is {}", configuration.getMetricContextGroups().size());
        log.info("Metric contexts count is {}", configuration.getMetricContexts().size());

        Map<String, Long> tableGroupCounts = configuration.getMetricContextGroups().stream()
            .collect(Collectors.groupingBy(g -> g.getTable().getFullName(), Collectors.counting()))
            .entrySet()
            .stream()
            .sorted(Comparator.<Map.Entry<String, Long>, Long>comparing(Map.Entry::getValue).reversed())
            .collect(
                Collectors.toMap(
                    Map.Entry::getKey, Map.Entry::getValue,
                    (u, v) -> {
                        throw new IllegalStateException(String.format("Duplicate key %s", u));
                    },
                    LinkedHashMap::new
                ));

        tableGroupCounts.forEach((k, v) -> log.info("{} {}", k, v));

        Collection<ConfigFile> configFiles = configuration.getConfigFiles().values();

        if (configFiles.isEmpty()) {
            Assert.fail(
                "There must be at least 1 config in config directory. " +
                    "Please check that config directory is correct." +
                    configurationService.getConfigDir().getAbsolutePath()
            );
        }

        for (ConfigFile configFile : configFiles) {
            try {
                configurationService.parseAndCheck(configFile);
            } catch (Exception e) {
                throw new IllegalStateException("Unable to parse " + configFile.getName(), e);
            }

            for (GraphiteMetricConfig metricConfig : configFile.getGraphiteMetricConfigs()) {
                for (MetricField field : metricConfig.getFields()) {
                    MetricFieldValidator.validateField(field);
                }

                DashboardValidator.validateDashboards(metricConfig);
            }

            for (StatfaceReportConfig metricConfig : configFile.getStatfaceReportConfigs()) {
                Set<String> fieldNames = metricConfig.getFields().stream()
                    .map(MetricField::getName)
                    .collect(Collectors.toSet());

                for (MetricField field : metricConfig.getFields()) {
                    MetricFieldValidator.validateField(field, fieldNames);
                }
            }
        }
    }

    @Test
    @Ignore
    public void validateQueries() {
        ClickphiteConfiguration configuration = configurationService.getConfiguration();
        configuration.getMetricContextGroups().forEach(metricContextGroup -> {
            String queryTemplate = QueryBuilder.buildMetricQueryTemplate(metricContextGroup);
            log.info("Query for metric group {} is\n{}\n", metricContextGroup.getId(), queryTemplate);
        });
    }

    @PropertySource("file:src/properties/clickphite.properties")
    public static class Config {
        @Value("${clickphite.config.forbidden-functions:}")
        private String forbiddenFunctionsConfig;
        @Value("${clickphite.config.allowed-functions:}")
        private String allowedFunctionsConfig;

        @Bean
        public ConfigValidator configValidator() {
            return new ConfigValidator(forbiddenFunctionsConfig, allowedFunctionsConfig);
        }
    }
}
