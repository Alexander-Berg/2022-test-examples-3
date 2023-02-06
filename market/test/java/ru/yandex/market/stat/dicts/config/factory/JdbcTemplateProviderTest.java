package ru.yandex.market.stat.dicts.config.factory;

import com.zaxxer.hikari.HikariDataSource;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = JdbcTemplateProviderTest.TestConfig.class)
public class JdbcTemplateProviderTest {

    @Autowired
    private JdbcTemplateProvider jdbcTemplateProvider;

    @Test
    public void onlyRequiredPropertiesJdbcTemplateProvider() {
        JdbcTemplate jdbcTemplate = jdbcTemplateProvider.createJdbcTemplate(new HikariDataSource(), "test");
        assertThat(jdbcTemplate, notNullValue(JdbcTemplate.class));
    }

    @Test
    public void requiredAndOptionalPropertiesJdbcTemplateProvider() {
        JdbcTemplate jdbcTemplate = jdbcTemplateProvider.createJdbcTemplate(new HikariDataSource(), "test2");
        assertThat(jdbcTemplate, notNullValue(JdbcTemplate.class));
        assertThat(jdbcTemplate.getQueryTimeout(), is(32120));
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
