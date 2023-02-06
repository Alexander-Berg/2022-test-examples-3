package ru.yandex.market.billing.tasks;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mockito;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import ru.yandex.common.util.terminal.CommandInvocation;
import ru.yandex.common.util.terminal.Terminal;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;

public class TableBackwardRenameTest {
    static private NamedParameterJdbcTemplate jdbcTemplate = Mockito.mock(NamedParameterJdbcTemplate.class);

    private Terminal terminal = Mockito.mock(Terminal.class);

    private StringWriter terminalBuffer;

    private TableBackwardRenameCommand command = new TableBackwardRenameCommand(jdbcTemplate);

    @BeforeAll
    static void init() {
        Mockito.when(jdbcTemplate.queryForObject(Mockito.anyString(),
                Mockito.any(MapSqlParameterSource.class), Mockito.eq(Integer.class))).thenReturn(0);
        Mockito.when(jdbcTemplate.queryForObject(Mockito.eq("SELECT COUNT(*) FROM all_tables WHERE owner = 'SCHEMA' AND table_name = 'TABLE'"),
                Mockito.any(MapSqlParameterSource.class), Mockito.eq(Integer.class))).thenReturn(1);
        Mockito.when(jdbcTemplate.queryForObject(Mockito.eq("SELECT COUNT(*) FROM all_tables WHERE owner = 'SCHEMA' AND table_name = 'TABLE_TO_DELETE'"),
                Mockito.any(MapSqlParameterSource.class), Mockito.eq(Integer.class))).thenReturn(1);
        Mockito.when(jdbcTemplate.queryForObject(Mockito.eq("SELECT COUNT(*) FROM all_tables WHERE owner = 'SCHEMA' AND table_name = 'TABLE_DLT'"),
                Mockito.any(MapSqlParameterSource.class), Mockito.eq(Integer.class))).thenReturn(1);
        Mockito.when(jdbcTemplate.getJdbcTemplate()).thenReturn(Mockito.mock(JdbcTemplate.class));
    }

    @BeforeEach
    void setup() {
        this.terminalBuffer = new StringWriter();
        PrintWriter printWriter = new PrintWriter(terminalBuffer);
        Mockito.when(terminal.getWriter())
                .thenReturn(printWriter);
    }

    @Test
    @DisplayName("Корректное удаление таблицы")
    public void correctRemovingTest() {
        command.executeCommand(
                new CommandInvocation("", new String[]{"schema.table_to_delete"}, new HashMap()), terminal);
        String messages = terminalBuffer.toString();
        Assertions.assertThat(messages).contains("Success");
    }

    @Test
    @DisplayName("Корректное удаление таблицы с укороченным суффиксом")
    public void correctRemovingShortSuffixTest() {
        command.executeCommand(
                new CommandInvocation("", new String[]{"schema.table_dlt"}, new HashMap()), terminal);
        String messages = terminalBuffer.toString();
        Assertions.assertThat(messages).contains("Success");
    }

    @Test
    @DisplayName("Удаление таблицы с неправильным названием")
    public void wrongNameTest() {
        command.executeCommand(
                new CommandInvocation("", new String[]{"schema.table"}, new HashMap()), terminal);
        String messages = terminalBuffer.toString();
        Assertions.assertThat(messages).contains("Wrong table name");
    }

    @Test
    @DisplayName("Удаление несуществующей таблицы")
    public void doesntExistsTest() {
        command.executeCommand(
                new CommandInvocation("", new String[]{"wrong_table_to_delete"}, new HashMap()), terminal);
        String messages = terminalBuffer.toString();
        Assertions.assertThat(messages).contains("Table does not exist");
    }

    @Test
    @DisplayName("Некорректные аргументы команды")
    public void wrongArgumentTest() {
        command.executeCommand(
                new CommandInvocation("", new String[]{}, new HashMap()), terminal);
        String messages = terminalBuffer.toString();
        Assertions.assertThat(messages).contains("Missing table name");
    }
}
