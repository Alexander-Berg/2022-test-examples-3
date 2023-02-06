package ru.yandex.market.logshatter.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.logshatter.parser.LogParserChecker;
import ru.yandex.market.logshatter.parser.auto.AutoParser;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.time.LocalDate;
import java.time.Month;
import java.time.ZoneId;
import java.util.Date;

public class CheckouterDeliveryServiceStatisticsTest {
    private static final JsonParser JSON_PARSER = new JsonParser();

    private LogParserChecker checker;

    @Before
    public void setUp() throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();

        JsonObject configObject;
        try (Reader reader = new FileReader("../config-cs-logshatter/src/configs/checkouter_delivery_service_statistics.json")) {
            configObject = JSON_PARSER.parse(reader).getAsJsonObject();
        }

        AutoParser parser = new AutoParser(ConfigurationService.getParserConfig(configObject));
        checker = new LogParserChecker(parser);
    }


    @Test
    public void shouldParseLine() throws Exception {
        String line = "[2018-05-30 10:21:33,458]\t107\tPICKUP\t2018-03-15\t7\t131";

        checker.check(
            line,
            new Date(1527664893458L),
            107,
            "PICKUP",
            Date.from(LocalDate.of(2018, Month.MARCH, 15).atStartOfDay(ZoneId.systemDefault()).toInstant()),
            7,
            131);
    }
}
