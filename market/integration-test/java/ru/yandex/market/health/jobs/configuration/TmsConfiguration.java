package ru.yandex.market.health.jobs.configuration;

import java.io.IOException;
import java.util.Properties;

import javax.sql.DataSource;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.common.util.terminal.CommandExecutor;
import ru.yandex.common.util.terminal.CommandParser;
import ru.yandex.market.tms.quartz2.model.Executor;
import ru.yandex.market.tms.quartz2.spring.CronTrigger;
import ru.yandex.market.tms.quartz2.spring.config.TmsDataSourceConfig;

@Configuration
public class TmsConfiguration {

    @Bean
    public TmsDataSourceConfig tmsDataSourceConfig(DataSource dataSource) {
        return new TmsDataSourceConfig() {
            @Override
            public DataSource tmsDataSource() {
                return dataSource;
            }

            @Override
            public JdbcTemplate tmsJdbcTemplate() {
                return new JdbcTemplate(dataSource);
            }

            @Override
            public TransactionTemplate tmsTransactionTemplate() {
                return new TransactionTemplate(tmsTransactionManager());
            }

            @Override
            public PlatformTransactionManager tmsTransactionManager() {
                return new DataSourceTransactionManager(dataSource);
            }
        };
    }

    @Bean(name = "quartzProperties")
    public Properties quartzProperties(ResourceLoader resourceLoader) throws IOException {

        Properties properties = new Properties();
        Resource resource = resourceLoader.getResource("classpath:quartz-integration-test.properties");
        properties.load(resource.getInputStream());

        return properties;
    }

    @Bean
    public CommandExecutor commandExecutor() {
        CommandExecutor commandExecutor = new CommandExecutor();
        commandExecutor.setCommandParser(new CommandParser());
        return commandExecutor;
    }

    @Bean
    @CronTrigger(cronExpression = "* * * ? * * *", description = "test")
    public Executor testJob() {
        return context -> {
            try {
                Thread.sleep(500); // проверяем настоящую джобу на предмет того, что он зависла
            } catch (InterruptedException e) {
                // ничего не делаем
            }
        };
    }
}
