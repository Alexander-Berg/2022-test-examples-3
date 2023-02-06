package ru.yandex.autotests.market.stat.meta;

import org.apache.commons.dbcp2.BasicDataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import ru.yandex.qatools.properties.PropertyLoader;
import ru.yandex.qatools.properties.annotations.Property;
import ru.yandex.qatools.properties.annotations.Resource;

import javax.sql.DataSource;

/**
 * Created by jkt on 14.05.14.
 */
@Configuration
@Resource.Classpath("metabase.properties")
public class MetaBaseConfig {

    private String jdbcDriver = "org.postgresql.Driver";

    @Property("metabase.host")
    private String jdbcHost = "pgaas-test.mail.yandex.net";

    @Property("metabase.user")
    private String username = "market_prest_user";

    @Property("metabase.password")
    private String password = "pjsdnmu499ypharuq5f69s5r9m";

    @Property("dictionaries.yt.database.name")
    private String dictionariesDatabase = "marketstat_prest_dof";

    @Property("reporting.database.name")
    private String reportingDatabaseName = "marketstat_prest_dof";

    public MetaBaseConfig() {
        PropertyLoader.populate(this);
    }

    @Bean
    public JdbcTemplate reportingJdbcTemplate() {
        return new JdbcTemplate(reportingDataSource());
    }

    @Bean
    public JdbcTemplate dictionariesYtJdbcTemplate() {
        return new JdbcTemplate(dictionariesYtDataSource());
    }


    @Bean
    public DataSource reportingDataSource() {
        return dataSourceFor(reportingDatabaseName);
    }

    @Bean
    public DataSource dictionariesYtDataSource() {
        return dataSourceFor(dictionariesDatabase);
    }

    private String databaseUrlFor(String databaseName) {
        return "jdbc:postgresql://" + jdbcHost + ":12000/" +
            databaseName + "?sslmode=require";
    }

    private BasicDataSource dataSourceFor(String databaseName) {
        final BasicDataSource dataSource = new BasicDataSource();
        dataSource.setDriverClassName(jdbcDriver);
        dataSource.setUrl(databaseUrlFor(databaseName));
        dataSource.setUsername(username);
        dataSource.setPassword(password);
        return dataSource;
    }
}
