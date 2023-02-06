package ru.yandex.market.stat.dicts.loaders.loadchecks;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.time.LocalDateTime;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

@RunWith(MockitoJUnitRunner.class)
public class JdbcModificationTimeLoadCheckTest {

    private static final LocalDateTime MODIFICATION_TIME = LocalDateTime.parse("2018-06-01T02:14:00");

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Test
    public void needLoad() {
        String testSql = "test sql";
        Mockito.when(jdbcTemplate.queryForObject(testSql, Timestamp.class))
            .thenReturn(Timestamp.valueOf(MODIFICATION_TIME));
        JdbcModificationTimeLoadCheck check = new JdbcModificationTimeLoadCheck(jdbcTemplate, testSql);

        assertThat(check.needLoad(LocalDateTime.parse("2018-06-01T02:13:59")), equalTo(true));
        assertThat(check.needLoad(LocalDateTime.parse("2018-06-01T02:14:00")), equalTo(false));
        assertThat(check.needLoad(LocalDateTime.parse("2018-06-01T02:14:01")), equalTo(false));
    }
}
