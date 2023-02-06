package ru.yandex.market.stat.dicts.config.factory;

import com.zaxxer.hikari.HikariDataSource;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import ru.yandex.market.stat.dicts.config.JdbcConfig;

import javax.sql.DataSource;
import java.util.Properties;

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = DataSourceProviderTest.TestConfig.class)
public class DataSourceProviderTest {

    @Autowired
    private DataSourceProvider dataSourceProvider;

    @Test
    public void connectionPropertiesDataSourceProvider() {
        HikariDataSource dataSource = dataSourceProvider.createDataSource("proptest");
        Properties connectionProperties = dataSource.getDataSourceProperties();
        assertThat(connectionProperties.size(), is(4));
        assertThat(connectionProperties.keySet(), hasItems("oracle.net.CONNECT_TIMEOUT", "socket_timeout", "connection_timeout", "oracle.jdbc.ReadTimeout"));
        assertThat(connectionProperties.getProperty("oracle.jdbc.ReadTimeout"), is(("130")));
        assertThat(connectionProperties.getProperty("socket_timeout"), is(("70")));
        assertThat(connectionProperties.getProperty("connection_timeout"), is(("120")));
        assertThat(connectionProperties.getProperty("oracle.net.CONNECT_TIMEOUT"), is(("60")));
    }

    @Test
    public void onlyRequiredPropertiesDataSourceProvider() {
        HikariDataSource dataSource = dataSourceProvider.createDataSource("test");
        assertThat(dataSource, notNullValue(DataSource.class));
        assertThat(dataSource.getDriverClassName(), is("oracle.jdbc.driver.OracleDriver"));
        assertThat(dataSource.getJdbcUrl(), is("jdbc:some://mno"));
        assertThat(dataSource.getUsername(), is("abc"));
        assertThat(dataSource.getPassword(), is("xyz"));
    }

    @Test
    public void requiredAndOptionalPropertiesDataSourceProvider() {
        HikariDataSource dataSource = dataSourceProvider.createDataSource("test2");
        assertThat(dataSource, notNullValue(DataSource.class));
        assertThat(dataSource.getSchema(), is("my_schema_path"));
        assertThat(dataSource.getConnectionInitSql(), allOf(
            containsString("init 1"),
            containsString("and three"),
            containsString("init 2")
        ));
    }

    @Test
    public void missingPropertiesDataSourceProvider() {
        try {
            dataSourceProvider.createDataSource("badtest");
            fail(JdbcConfig.MissingJdbcConfigException.class + " should be thrown");
        } catch (JdbcConfig.MissingJdbcConfigException e) {
            assertThat(e.getBadConfig(), notNullValue());
            assertThat(e.getBadConfig().getTap(), is("badtest"));
            assertThat(e.getBadConfig().getMissingProperties(), notNullValue());
            assertThat(e.getBadConfig().getMissingProperties().size(), is(1));
            assertThat(e.getBadConfig().getMissingProperties(), hasItem("username"));
        }
    }

    @Test
    public void wrongFormattedPropertiesDataSourceProvider() {
        try {
            dataSourceProvider.createDataSource("badtest2");
            fail(IllegalArgumentException.class + " should be thrown");
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), containsString("not-a-number"));
        }
    }

    @ComponentScan(basePackageClasses = JdbcPropertiesProvider.class)
    @PropertySource("classpath:jdbc-config-test.properties")
    public static class TestConfig {
        @Bean
        public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
            return new PropertySourcesPlaceholderConfigurer();
        }
    }
}
