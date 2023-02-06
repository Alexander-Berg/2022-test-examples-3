package ru.yandex.market.db;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.mbi.environment.EnvironmentService;
import ru.yandex.market.shop.FunctionalTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static ru.yandex.market.db.PostgresMatViewRefresher.UPDATE_CONCURRENTLY_VARIABLE_NAME;

class PostgresMatViewRefresherTest extends FunctionalTest {
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private EnvironmentService environmentService;
    @Autowired
    private List<PostgresMatViewRefresher> instances;

    @Test
    void wrongViewName() {
        var instance = new PostgresMatViewRefresher("shops_web.v_search_dat", jdbcTemplate, environmentService);
        assertThatExceptionOfType(BadSqlGrammarException.class)
                .isThrownBy(() -> instance.doJob(null))
                .withMessageContaining("bad SQL grammar");
    }

    @Test
    void allSupportedViews() {
        assertThat(instances).isNotEmpty();

        environmentService.setValue(UPDATE_CONCURRENTLY_VARIABLE_NAME, Boolean.FALSE.toString());
        instances.forEach(i -> i.doJob(null));

        environmentService.setValue(UPDATE_CONCURRENTLY_VARIABLE_NAME, Boolean.TRUE.toString());
        instances.forEach(i -> i.doJob(null));
    }

    @Test
    @DbUnitDataSet(before = "PostgresMatViewRefresherTest.searchDataUpdaterCancelled.before.csv")
    void searchDataUpdaterCancelled() {
        var instance = new PostgresMatViewRefresher("shops_web.v_search_data", jdbcTemplate, environmentService);
        instance.doJob(null);
    }
}
