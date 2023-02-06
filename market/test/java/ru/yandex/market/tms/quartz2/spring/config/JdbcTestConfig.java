package ru.yandex.market.tms.quartz2.spring.config;

import java.util.Properties;

import javax.sql.DataSource;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;

import ru.yandex.market.tms.quartz2.model.Executor;
import ru.yandex.market.tms.quartz2.spring.BaseTmsTestConfig;

/**
 * @author otedikova
 */
@Import({
        BaseTmsTestConfig.class,
        DatabaseSchedulerFactoryConfig.class, RAMSchedulerFactoryConfig.class
})
@PropertySource("classpath:/ru/yandex/market/tms/quartz2/spring/EnableMarketTmsTest.properties")
@Configuration
public class JdbcTestConfig {

    @Bean
    public Executor executor() {
        return context -> {
        };
    }

    @Bean
    public JdbcTemplate jdbcTemplate() {
        return new JdbcTemplate(dataSource());
    }

    @Bean
    public DataSource dataSource() {
        return new EmbeddedDatabaseBuilder()
                .setType(EmbeddedDatabaseType.H2)
                .addScript("classpath:/sql/tms-core-quartz2_log_table.sql")
                .addScript("classpath:/sql/tms-core-quartz2_schema.sql")
                .build();
    }

    @Bean(name = "quartzProperties")
    public Properties quartzProperties() {
        Properties props = new Properties();
        props.put("org.quartz.jobStore.tablePrefix", "TEST_QRTZ_");
        return props;
    }
}
