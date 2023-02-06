package ru.yandex.market.core.database;

import java.sql.SQLException;

import javax.sql.DataSource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.core.config.DevIntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class OracleToPostgresJdbcTemplateIntegrationTest extends DevIntegrationTest {
    @Autowired
    DataSource dataSource;
    @Autowired
    MdbMbiConfig mdbMbiConfig;

    @BeforeEach
    void setUp() {
        dataSource = spy(dataSource);
        jdbcTemplate = mdbMbiConfig.makeJdbcTemplate(
                dataSource,
                true,
                () -> OracleToPostgresJdbcTemplate.ExpQueryRouting.WITH_FALLBACK
        );
    }

    @Test
    void selectGoesToExperiment() {
        var r = getOne();
        assertThat(r).isOne();
        verifyNoInteractions(dataSource);
    }

    @Test
    void selectWithinTxGoesToMain() throws SQLException {
        var r = transactionTemplate.execute(status -> getOne());
        assertThat(r).isOne();
        verify(dataSource, atLeastOnce()).getConnection();
    }

    private Integer getOne() {
        return jdbcTemplate.queryForObject("select 1 from dual", Integer.class);
    }
}
