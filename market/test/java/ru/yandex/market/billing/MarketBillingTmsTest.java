package ru.yandex.market.billing;

import java.time.ZoneId;
import java.util.Map;

import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import ru.yandex.market.FunctionalTest;

import static org.junit.Assert.assertEquals;

public class MarketBillingTmsTest extends FunctionalTest {

    @Autowired
    private NamedParameterJdbcTemplate pgNamedParameterJdbcTemplate;

    @Test
    public void test() {
        //Just a test example
        Assert.assertTrue(true);
    }

    @Test
    public void testVersion() {
        pgNamedParameterJdbcTemplate.queryForObject("select version()", Map.of(), String.class);
    }

    @Test
    void checkTimezones() {
        String postgreTimezone = pgNamedParameterJdbcTemplate.queryForObject("" +
                        "SELECT name FROM pg_timezone_names WHERE name = current_setting('TIMEZONE');",
                Map.of(),
                String.class
        );
        String javaTimeZone = ZoneId.systemDefault().toString();
        assertEquals("Java : " + javaTimeZone + ", postgre : " + postgreTimezone, javaTimeZone, postgreTimezone);
    }
}
