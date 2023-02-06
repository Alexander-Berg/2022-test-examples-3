package ru.yandex.market.takeout.config;

import java.util.Collections;

import javax.sql.DataSource;

import org.apache.commons.dbcp2.BasicDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import ru.yandex.market.request.datasource.trace.DataSourceTraceUtil;
import ru.yandex.market.request.trace.Module;
import ru.yandex.market.takeout.util.AppUtils;

import static ru.yandex.market.takeout.config.DaoConfig.appendApplicationName;

@Configuration
public class DaoTestConfig {

    @Value("${takeout.metadata.jdbc.url}")
    private String jdbcUrl;
    @Value("${takeout.metadata.jdbc.driver}")
    private String jdbcDriver;
    @Value("${takeout.metadata.jdbc.username}")
    private String jdbcUsername;
    @Value("${takeout.metadata.jdbc.password}")
    private String jdbcPassword;
    @Value("${takeout.metadata.jdbc.schema:}")
    private String jdbcSchema;

    @Bean
    public DataSource metadataDataSource() {
        BasicDataSource dataSource = new BasicDataSource();
        dataSource.setDriverClassName(jdbcDriver);
        dataSource.setUrl(appendApplicationName(jdbcUrl, AppUtils.getAppName()));
        dataSource.setUsername(jdbcUsername);
        dataSource.setPassword(jdbcPassword);
        dataSource.setValidationQuery("SELECT 1");
        dataSource.setTestOnBorrow(true);
        dataSource.setTestOnReturn(true);
        dataSource.setTestWhileIdle(true);
        dataSource.setMaxTotal(60);
        dataSource.setMaxWaitMillis(10000L);
        if (jdbcSchema != null && !jdbcSchema.isEmpty()) {
            String setSchemaSql = "set search_path to " + jdbcSchema + ", public";
            dataSource.setConnectionInitSqls(Collections.singletonList(setSchemaSql));
        }
        return DataSourceTraceUtil.wrap(dataSource, Module.PGAAS);
    }

    @Bean
    public NamedParameterJdbcTemplate metadataJdbcTemplate() {
        return new NamedParameterJdbcTemplate(metadataDataSource());
    }
}
