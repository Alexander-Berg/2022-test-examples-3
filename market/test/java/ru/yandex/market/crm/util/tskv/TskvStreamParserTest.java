package ru.yandex.market.crm.util.tskv;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author apershukov
 */
public class TskvStreamParserTest {

    private static String tskvRecord(String... fields) {
        StringBuilder sb = new StringBuilder("tskv");

        for (int i = 0; i < fields.length; i += 2) {
            sb.append("\t").append(fields[i]).append('=').append(fields[i + 1]);
        }

        return sb.toString();
    }

    @Test
    public void testReturnDefaultValueIfFieldNotFound() {
        String line = tskvRecord(
                "value_1", "iddqd",
                "value_2", "idkfa"
        );

        String parsed = new TestParser().parse(line);
        Assertions.assertEquals(TestParser.DEFAULT_VALUE, parsed);
    }

    @Test
    public void testReturnFieldValueIfFound() {
        String expectedValue = "expected_value";

        String line = tskvRecord(
                "value_1", "iddqd",
                "value_2", "idkfa",
                "value", expectedValue
        );

        String parsed = new TestParser().parse(line);
        Assertions.assertEquals(expectedValue, parsed);
    }

    @Test
    public void testValueIsDecoded() {
        String line = tskvRecord(
                "value", "\\\"encoded\\\""
        );

        String parsed = new TestParser().parse(line);
        Assertions.assertEquals("\"encoded\"", parsed);
    }

    private static class TestParser extends TskvStreamParser<String> {

        static final String DEFAULT_VALUE = "default_value";

        private String value = DEFAULT_VALUE;

        TestParser() {
            onValue("value", v -> value = v);
        }

        @Override
        protected String getResult() {
            return value;
        }
    }
}
