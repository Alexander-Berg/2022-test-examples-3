package ru.yandex.market.logshatter.generic;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Arrays;

import javax.naming.ConfigurationException;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.junit.Test;

import ru.yandex.market.clickhouse.ddl.engine.MergeTree;
import ru.yandex.market.health.configs.logshatter.config.TableDescriptionUtils;
import ru.yandex.market.health.configs.logshatter.json.JsonParser;
import ru.yandex.market.health.configs.logshatter.json.JsonParserConfig;
import ru.yandex.market.logshatter.config.ConfigurationService;
import ru.yandex.market.logshatter.parser.LogParserChecker;
import ru.yandex.market.logshatter.parser.ParserException;

import static org.junit.Assert.assertEquals;

/**
 * @author Dmitry Andreev <a href="mailto:AndreevDm@yandex-team.ru"></a>
 * @date 30/10/2017
 */
public class JsonParserTest {

    @Test
    public void testParser() throws Exception {
        LogParserChecker checker = getChecker("/configs/generic/json/jsonGenericConfig.json");
        checker.check(
            "{  \"date\": \"2017-11-01 14:33:09\",  \"string\": \"str1\",  \"other_string\": \"str2\",  " +
                "\"some_value\": 21 }",
            1509535989, checker.getHost(), "str1", "str2", 21, 42, ""
        );
    }

    @Test
    public void testNestedJsonAsString() throws Exception {
        LogParserChecker checker = getChecker("/configs/generic/json/jsonGenericConfig.json");
        checker.check(
            "{  \"date\": \"2017-11-01 14:33:09\",  \"string\": \"str1\",  \"other_string\": \"str2\", " +
                "\"some_value\": 21, \"nested_value\": {\"some_nested_value\":11,\"some__nested_value2\":2} }",
            1509535989, checker.getHost(), "str1", "str2", 21, 42, "{\"some_nested_value\":11," +
                "\"some__nested_value2\":2}"
        );
    }

    @Test
    public void testNestedJsonExtracted() throws Exception {
        LogParserChecker checker = getChecker("/configs/generic/json/jsonPathGenericConfig.json");
        checker.check(
            "{  \"date\": \"2017-11-01 14:33:09\",  \"other\":{\"string\": \"str2\"}, \"some\": {\"value\": 21}}",
            1509535989, checker.getHost(), "str2", 21, ""
        );
    }

    @Test
    public void testNestedJsonArrayAsString() throws Exception {
        LogParserChecker checker = getChecker("/configs/generic/json/jsonGenericConfig.json");
        checker.check(
            "{  \"date\": \"2017-11-01 14:33:09\",  \"string\": \"str1\",  \"other_string\": \"str2\", " +
                "\"some_value\": 21, \"nested_value\": [{\"some_nested_value\":11},{\"some_nested_value\":2}] }",
            1509535989, checker.getHost(), "str1", "str2", 21, 42, "[{\"some_nested_value\":11}," +
                "{\"some_nested_value\":2}]"
        );
    }

    @Test
    public void testJsonNull() throws Exception {
        LogParserChecker checker = getChecker("/configs/generic/json/jsonGenericConfig.json");
        checker.check(
            "{  \"date\": \"2017-11-01 14:33:09\",  \"string\": \"str1\",  \"other_string\": null,  \"some_value\": " +
                "21, \"other_value\":null }",
            1509535989, checker.getHost(), "str1", "EMPTY", 21, 42, ""
        );
    }

    @Test
    public void testEmptyStringToDefaultNumberValue() throws Exception {
        LogParserChecker checker = getChecker("/configs/generic/json/jsonGenericConfig.json");
        checker.check(
            "{  \"date\": \"2017-11-01 14:33:09\",  \"string\": \"str1\",  \"other_string\": \"\",  \"some_value\": " +
                "21, \"other_value\": \"\" }",
            1509535989, checker.getHost(), "str1", "", 21, 42, ""
        );
    }

    @Test
    public void testTimestamp() throws Exception {
        LogParserChecker checker = getChecker("/configs/generic/json/jsonTimestampConfig.json");
        checker.check(
            "{  \"ts\": 1509535989,  \"value\": 42 }",
            1509535989, 42
        );
        checker.check(
            "{  \"ts\": 1509535989000,  \"value\": 42 }",
            1509535989, 42
        );
    }

    @Test
    public void checkDateOrTs() throws Exception {
        LogParserChecker checker = getChecker("/configs/generic/json/jsonDatesConfig.json");
        checker.check(
            "{  \"date\": \"2017-11-01 14:33:09\",  \"value\": 42 }",
            1509535989, 42
        );
        checker.check(
            "{  \"ts\": 1509535989000,  \"value\": 42 }",
            1509535989, 42
        );
    }

    @Test
    public void checkDatePathOrTsPath() throws Exception {
        LogParserChecker checker = getChecker("/configs/generic/json/jsonPathDatesConfig.json");
        checker.check(
            "{  \"date\": {\"value\": \"2017-11-01 14:33:09\"},  \"value\": 42 }",
            1509535989, 42
        );
        checker.check(
            "{  \"ts\": {\"second\": 1509535989000},  \"value\": 42 }",
            1509535989, 42
        );
    }

    @Test
    public void defaultEmptyString() throws Exception {
        LogParserChecker checker = getChecker("/configs/generic/json/jsonTestEmptyDefaultString.json");
        checker.check(
            "{  \"date\": \"2017-11-01 14:33:09\"}",
            1509535989, ""
        );
    }

    @Test
    public void simpleColumnFormat() throws Exception {
        LogParserChecker checker = getChecker("/configs/generic/json/jsonSimpleColumnFormat.json");
        checker.check(
            "{  \"date\": \"2017-11-01 14:33:09\",  \"string\": \"str1\",    \"int1\": 21 }",
            1509535989, "str1", 21, 42
        );
    }

    @Test
    public void useDefaultOnParsingException() throws Exception {
        LogParserChecker checker = getChecker("/configs/generic/json/jsonDefaultOnParsingException.json");
        checker.check(
            "{  \"date\": \"2017-11-01 14:33:09\", \"int\": \"aaaaaaaaaaaaaaaa\" }",
            1509535989, 42
        );
    }

    @Test
    public void defaultEngine() throws Exception {
        assertEquals(
            TableDescriptionUtils.DEFAULT_ENGINE,
            new JsonParser(readParserConfig("/configs/generic/json/engineDefault.json"))
                .getTableDescription().getEngine()
        );
    }

    @Test
    public void engineWithDefaultFields() throws Exception {
        assertEquals(
            TableDescriptionUtils.DEFAULT_ENGINE,
            new JsonParser(readParserConfig("/configs/generic/json/engineDefaultFields.json"))
                .getTableDescription().getEngine()
        );
    }

    @Test
    public void customEngine() throws Exception {
        assertEquals(
            new MergeTree(
                "partitionBy",
                Arrays.asList("sampleBy", "orderBy1", "orderBy2"),
                "sampleBy",
                TableDescriptionUtils.DEFAULT_ENGINE.getIndexGranularity()
            ),
            new JsonParser(readParserConfig("/configs/generic/json/engineCustom.json"))
                .getTableDescription().getEngine()
        );
    }

    @Test(expected = IllegalArgumentException.class)
    public void noDateColumnsException() throws Exception {
        getChecker("/configs/generic/json/jsonNoColumns.json");
    }

    @Test(expected = ParserException.class)
    public void checkNoDatesInLineException() throws Exception {
        LogParserChecker checker = getChecker("/configs/generic/json/jsonDatesConfig.json");
        checker.check("{  \"value\": 42 }");
    }

    @Test(expected = ParserException.class)
    public void checkNoDatePathInLineException() throws Exception {
        LogParserChecker checker = getChecker("/configs/generic/json/jsonPathDatesConfig.json");
        checker.check("{  \"value\": 42 }");
    }

    @Test(expected = IllegalArgumentException.class)
    public void noDateOrTsException() throws Exception {
        getChecker("/configs/generic/json/jsonNoDateOrTs.json");
    }

    @Test(expected = IllegalArgumentException.class)
    public void dateWithoutFormatException() throws Exception {
        getChecker("/configs/generic/json/jsonDateWithoutFormat.json");
    }

    @Test(expected = ConfigurationException.class)
    public void invalidDefaultValueException() throws Exception {
        getChecker("/configs/generic/json/jsonTestInvalidDefaultValue.json");
    }


    private LogParserChecker getChecker(String configFilePath) throws Exception {
        return new LogParserChecker(new JsonParser(readParserConfig(configFilePath)));
    }

    private JsonParserConfig readParserConfig(String configFilePath) throws IOException, ConfigurationException {
        JsonObject configObject;
        try (Reader confReader = new InputStreamReader(getClass().getResourceAsStream(configFilePath))) {
            configObject = new Gson().fromJson(confReader, JsonObject.class);
        }
        return ConfigurationService.getJsonParserConfig(configObject);
    }
}
