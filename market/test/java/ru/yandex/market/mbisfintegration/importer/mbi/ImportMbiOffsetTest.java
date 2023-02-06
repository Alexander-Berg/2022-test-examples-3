package ru.yandex.market.mbisfintegration.importer.mbi;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import ru.yandex.market.mbisfintegration.MbiSfAbstractJdbcRecipeTest;

class ImportMbiOffsetTest extends MbiSfAbstractJdbcRecipeTest {
    @Autowired
    public NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    @Test
    @DisplayName("Проверка существования таблицы")
    public void tableExists() {
        jdbcTemplate.execute("SELECT 1 from import_mbi_offset");
    }

    @Test
    @DisplayName("Проверка выполнения базовых операций с таблицей")
    public void basicOperations() {
        List<OffsetDateTime> timeOffsets = namedParameterJdbcTemplate.queryForList(
                "SELECT TIME_OFFSET FROM import_mbi_offset WHERE URI = :uri",
                Map.of("uri", "test"),
                OffsetDateTime.class
        );
        Assertions.assertTrue(timeOffsets.isEmpty(), "Select из пустой таблицы");

        String expectedTimeOffset = "2020-10-06T01:00:55.158576Z";
        int updateCount = namedParameterJdbcTemplate.update("INSERT INTO import_mbi_offset VALUES (:uri, :time_offset)",
                Map.of("uri", "test", "time_offset", expectedTimeOffset));
        Assertions.assertEquals(1, updateCount, "Вставка одной записи");

        String actualTimeOffset = namedParameterJdbcTemplate.queryForObject(
                "SELECT TIME_OFFSET FROM import_mbi_offset WHERE URI = :uri",
                Map.of("uri", "test"),
                String.class
        );
        Assertions.assertEquals(OffsetDateTime.parse(expectedTimeOffset), OffsetDateTime.parse(actualTimeOffset),
                "Поиск по uri");
    }
}
