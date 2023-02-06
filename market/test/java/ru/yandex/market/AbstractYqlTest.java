package ru.yandex.market;

import javax.annotation.Nonnull;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import ru.yandex.yql.YqlDataSource;
import ru.yandex.yql.settings.YqlProperties;

/**
 * Абстрактный тест для YQL запросов через {@link NamedParameterJdbcTemplate}
 * Date: 04.03.2021
 * Project: arcadia-market_mbi_mbi
 *
 * @author alexminakov
 */
public abstract class AbstractYqlTest {

    @Nonnull
    public NamedParameterJdbcTemplate yqlNamedParameterJdbcTemplate() {
        return new NamedParameterJdbcTemplate(yqlJdbcTemplate());
    }

    @Nonnull
    private YqlDataSource yqlDataSource() {
        YqlProperties yqlProperties = new YqlProperties();
        yqlProperties.setUser(getUser());
        yqlProperties.setPassword(getPassword());
        yqlProperties.setSyntaxVersion(1);

        return new YqlDataSource(getYqlUrl(), yqlProperties);
    }

    @Nonnull
    private JdbcTemplate yqlJdbcTemplate() {
        return new JdbcTemplate(yqlDataSource());
    }

    /**
     * @return Логин пользователя
     */
    protected abstract String getUser();

    /**
     * @return Пароль пользователя
     */
    protected abstract String getPassword();

    /**
     * @return URL до YQL хоста
     */
    protected abstract String getYqlUrl();
}
