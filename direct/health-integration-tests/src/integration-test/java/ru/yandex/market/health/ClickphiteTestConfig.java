package ru.yandex.market.health;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportResource;
import ru.yandex.market.clickhouse.ClickHouseSource;
import ru.yandex.market.clickhouse.ClickhouseTemplate;
import ru.yandex.market.clickphite.config.validation.context.ConfigValidator;
import ru.yandex.market.clickphite.config.ConfigurationService;
import ru.yandex.market.clickphite.config.validation.config.MetricContextGroupValidator;
import ru.yandex.market.clickphite.dictionary.ClickhouseService;
import ru.yandex.market.clickphite.dictionary.Dictionary;
import ru.yandex.market.clickphite.dictionary.DictionaryLoadTask;
import ru.yandex.market.monitoring.ComplicatedMonitoring;

import java.util.List;

@Configuration
@Import({TestsCommonConfig.class})
@ImportResource("file:${clickphite.dicts-config}")
public class ClickphiteTestConfig {

    @Autowired
    private ApplicationContext applicationContext;

    @Value("${database.name}")
    private String databaseName;

    @Value("${clickphite.clickhouse.host}")
    private String clickhouseHost;

    @Value("${clickphite.conf.dir}")
    private String clickphiteConfDir;

    @Value("${clickphite.config.validation-retry.count}")
    private int retryCount;

    @Value("${clickphite.config.validation-retry.pause-millis}")
    private int retryPauseMillis;

    @Autowired
    private ClickhouseTemplate clickhouseTemplate;

    @Autowired
    private ClickHouseSource clickHouseSource;

    @Autowired
    private ComplicatedMonitoring complicatedMonitoring;

    @Bean
    public ConfigurationService configurationService() {
        ConfigurationService configurationService = new ConfigurationService();
        configurationService.setConfigDir(clickphiteConfDir);
        configurationService.setMonitoring(complicatedMonitoring);
        configurationService.setDefaultDatabase(databaseName);
        configurationService.setConfigValidator(new ConfigValidator());
        configurationService.setMetricContextGroupValidator(metricContextGroupValidator());
        return configurationService;
    }

    @Bean
    public MetricContextGroupValidator metricContextGroupValidator() {
        MetricContextGroupValidator metricContextGroupValidator = new MetricContextGroupValidator();
        metricContextGroupValidator.setClickhouseTemplate(clickhouseTemplate);
        metricContextGroupValidator.setValidationRetryCount(retryCount);
        metricContextGroupValidator.setValidationRetryPauseMillis(retryPauseMillis);
        return metricContextGroupValidator;
    }

    @Bean
    public ClickhouseService clickhouseService() {
        ClickhouseService clickhouseService = new ClickhouseService();
        clickhouseService.setClickhouseTemplate(clickhouseTemplate);
        clickhouseService.setMonitoring(complicatedMonitoring);
        clickhouseService.setClickHouseSource(clickHouseSource);
        return clickhouseService;
    }

    @Bean
    public List<DictionaryLoadTask> dictionaryService() {
        @SuppressWarnings("unchecked")
        List<DictionaryLoadTask> dictionaries = (List<DictionaryLoadTask>) applicationContext.getBean("dictionaries");

        ClickhouseService clickhouseService = clickhouseService();

        for (DictionaryLoadTask dictionaryLoadTask : dictionaries) {
            Dictionary dictionary = dictionaryLoadTask.getDictionary();
            clickhouseService.createDatabaseIfNotExists(dictionary.getDb(), clickhouseHost);

            String dataTable = dictionary.getDb() + "." + dictionary.getTable();

            if (!clickhouseService.tableExists(dataTable, clickhouseHost)) {
                clickhouseService.createTable(
                    dataTable,
                    dictionary.getAllColumns(),
                    dictionary.getEngine(),
                    clickhouseHost,
                    dictionary.getEngineSpec());
            }
        }

        return dictionaries;
    }
}
