package ru.yandex.market.billing.tasks;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;

/**
 * Tests for {@link TablePartitionAddDbService}.
 */
class TablePartitionAddDbServiceTest {

    private static final LocalDate DATE_2019_12_31 = LocalDate.of(2019, 12, 31);
    //language=sql
    private static final String EXPECTED_SQL_SCRIPT = "" +
            "alter table schema_name.table_name " +
            "add partition prefix_20200101 " +
            "values less than (to_date('20200102', 'YYYYMMDD'))";

    private TablePartitionAddDbService tablePartitionAddDbService;
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    @BeforeEach
    void setUp() {
        namedParameterJdbcTemplate = Mockito.mock(NamedParameterJdbcTemplate.class);
        tablePartitionAddDbService = Mockito.spy(new TablePartitionAddDbService(namedParameterJdbcTemplate));
    }

    @DisplayName("Получить следующую дату в виде строки.")
    @Test
    void test_shouldGetNextDateString() {
        String result = tablePartitionAddDbService.getNextDateString(DATE_2019_12_31);
        assertEquals("20200101", result);
    }

    @DisplayName("Получить название партиции.")
    @Test
    void test_shouldGetPartitionName() {
        String partitionName = tablePartitionAddDbService.getPartitionName("prefix_", DATE_2019_12_31);
        assertEquals("prefix_20191231", partitionName);
    }

    @DisplayName("Получить sql скрипт на добавление партиции.")
    @Test
    void test_shouldGetSqlToAddPartition() {
        Mockito.doReturn(LocalDate.of(2019, 12, 31))
                .when(tablePartitionAddDbService).getLastPartitionDate(eq("schema_name"), eq("table_name"), eq("prefix_"));
        String result = tablePartitionAddDbService.getAddPartitionSql("schema_name", "table_name", "prefix_");
        assertEquals(EXPECTED_SQL_SCRIPT, result);
    }
}