package ru.yandex.market.mstat.planner.util;

import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

public class LoggingJdbcTemplateTest {

    @Test
    @Ignore
    public void t() {
        NamedParameterJdbcTemplate ntpl = Mockito.mock(NamedParameterJdbcTemplate.class);
        Mockito.when(ntpl.getJdbcOperations()).thenReturn(Mockito.mock(JdbcOperations.class));
        LoggingJdbcTemplate tpl = new LoggingJdbcTemplate(ntpl);
        t2(tpl);
    }

    public void t2(LoggingJdbcTemplate tpl) {
        t3(tpl);
    }

    public void t3(LoggingJdbcTemplate tpl) {
        tpl.exec("select * from tbl");
    }

}
