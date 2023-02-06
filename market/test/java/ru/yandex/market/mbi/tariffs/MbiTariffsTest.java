package ru.yandex.market.mbi.tariffs;

import java.time.ZoneId;
import java.util.Arrays;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import ru.yandex.market.mbi.tariffs.model.ModelType;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class MbiTariffsTest extends FunctionalTest{

    @Autowired
    private NamedParameterJdbcTemplate postgresJdbcTemplate;

    @Test
    public void test() {
        postgresJdbcTemplate.queryForObject("select version()", Map.of(), String.class);
    }

    @Test
    void checkTimezones() {
        String postgreTimezone = postgresJdbcTemplate.queryForObject("" +
                "SELECT name FROM pg_timezone_names WHERE name = current_setting('TIMEZONE');",
                Map.of(),
                String.class
        );
        String javaTimeZone = ZoneId.systemDefault().toString();
        assertEquals("Java : " + javaTimeZone + ", postgre : " + postgreTimezone, javaTimeZone, postgreTimezone);
    }

    @Test
    void checkHumanReadableModelType() {
        Arrays.stream(ModelType.values())
                .map(Utils::humanReadableModelType)
                .forEach(it -> assertNotEquals("unknown", it));
    }
}
