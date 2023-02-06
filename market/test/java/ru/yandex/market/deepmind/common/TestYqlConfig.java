package ru.yandex.market.deepmind.common;

import javax.sql.DataSource;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import ru.yandex.market.mboc.common.config.YqlConfig;

/**
 * Тестовая аннотация, для того, чтобы зарааботали yql датасорсы. Возможно, избыточная.
 * https://wiki.yandex-team.ru/users/petrpopov/testy-s-yql
 */
@TestConfiguration
@Profile("test")
public class TestYqlConfig extends YqlConfig {

    @Bean(name = "yqlDataSource")
    @Override
    public DataSource yqlDataSource() {
        return super.yqlDataSource();
    }

    @Bean(name = "yqlJdbcTemplate")
    @Override
    public JdbcTemplate yqlJdbcTemplate() {
        return super.yqlJdbcTemplate();
    }

    @Bean(name = "namedYqlJdbcTemplate")
    @Override
    public NamedParameterJdbcTemplate namedYqlJdbcTemplate() {
        return super.namedYqlJdbcTemplate();
    }

    @Bean(name = "secondaryYqlDataSource")
    @Override
    public DataSource secondaryYqlDataSource() {
        return super.secondaryYqlDataSource();
    }

    @Bean(name = "secondaryYqlJdbcTemplate")
    @Override
    public JdbcTemplate secondaryYqlJdbcTemplate() {
        return super.secondaryYqlJdbcTemplate();
    }

    @Bean(name = "secondaryNamedYqlJdbcTemplate")
    @Override
    public NamedParameterJdbcTemplate secondaryNamedYqlJdbcTemplate() {
        return super.secondaryNamedYqlJdbcTemplate();
    }
}
