package ru.yandex.market.logshatter.config;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.logshatter.parser.LogParserChecker;
import ru.yandex.market.logshatter.parser.auto.AutoParser;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Date;

public class CheckouterBackwardCompatibilityTraceTest {
    private static final JsonParser JSON_PARSER = new JsonParser();

    private LogParserChecker checker;

    @Before
    public void setUp() throws IOException {
        JsonObject configObject;
        try (Reader reader = new FileReader("../config-cs-logshatter/src/configs/checkouter_backward_compatibility_trace.json")) {
            configObject = JSON_PARSER.parse(reader).getAsJsonObject();
        }

        AutoParser parser = new AutoParser(ConfigurationService.getParserConfig(configObject));
        checker = new LogParserChecker(parser);
    }


    @Test
    public void shouldParseLine() throws Exception {
        String line = "[2018-05-30 10:21:33,458]\ttest_backward\taaaaa";

        checker.check(
            line,
            new Date(1527664893458L),
            "test_backward",
            "aaaaa");
    }
}
