package ru.yandex.market.mbo.common.db;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.intellij.lang.annotations.Language;
import org.junit.Test;

import java.io.IOException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * @author Alexander Kramarev (pochemuto@yandex-team.ru)
 * @date 15.11.2017
 */
public class TemplateUtilTest {

    @Test
    public void removeNullFieldsFromArray() {
        assertThat("empty array is untouched", TemplateUtil.removeNullFields(json("[]")), is(json("[]")));

        assertThat("values are untouched", TemplateUtil.removeNullFields(json(
                "[" +
                "  1," +
                "  2," +
                "  [" +
                "    3," +
                "    null" +
                "  ]," +
                "  \"string\"," +
                "  5," +
                "  true" +
                "]")), is(json(
                "[" +
                "  1," +
                "  2," +
                "  [" +
                "    3," +
                "    null" +
                "  ]," +
                "  \"string\"," +
                "  5," +
                "  true" +
                "]")));


        assertThat("remove in inner object", TemplateUtil.removeNullFields(json(
                "[" +
                "  1," +
                "  2," +
                "  3," +
                "  \"string\"," +
                "  {" +
                "    \"normal\": \"ok\"," +
                "    \"shouldRemoved\": null" +
                "  }," +
                "  true" +
                "]")), is(json(
                "[" +
                "  1," +
                "  2," +
                "  3," +
                "  \"string\"," +
                "  {" +
                "    \"normal\": \"ok\"" +
                "  }," +
                "  true" +
                "]")));
    }

    @Test
    public void removeNullFieldsFromNative() {
        assertThat("string is untouched", TemplateUtil.removeNullFields(json("\"simple string\"")),
                is(json("\"simple string\"")));
        assertThat("number is untouched", TemplateUtil.removeNullFields(json("45")), is(json("45")));
        assertThat("null is untouched", TemplateUtil.removeNullFields(json("null")), is(json("null")));
        assertThat("boolean is untouched", TemplateUtil.removeNullFields(json("false")), is(json("false")));
    }

    @Test
    public void removeNullFieldsFromObject() {
        assertThat("with values are kept ", TemplateUtil.removeNullFields(json(
                "{" +
                "  \"one\": 1," +
                "  \"two\": 2," +
                "  \"ok\": true" +
                "}")), is(json(
                "{" +
                "  \"one\": 1," +
                "  \"two\": 2," +
                "  \"ok\": true" +
                "}")));

        assertThat("remove null values in root ", TemplateUtil.removeNullFields(json(
                "{" +
                "  \"one\": 1," +
                "  \"keyWithNull\": null," +
                "  \"two\": 2," +
                "  \"ok\": true," +
                "  \"anotherKeyWithNull\": null" +
                "}")), is(json(
                "{" +
                "  \"one\": 1," +
                "  \"two\": 2," +
                "  \"ok\": true" +
                "}")));

        assertThat("remove null in inner", TemplateUtil.removeNullFields(json(
                        "{" +
                        "  \"one\": 1," +
                        "  \"keyWithNull\": null," +
                        "  \"two\": 2," +
                        "  \"inner\": {" +
                        "    \"normal\": \"val\"," +
                        "    \"nullKey\": null" +
                        "  }," +
                        "  \"ok\": true," +
                        "  \"anotherKeyWithNull\": null" +
                        "}")),
                is(json("{" +
                        "  \"one\": 1," +
                        "  \"two\": 2," +
                        "  \"inner\": {" +
                        "    \"normal\": \"val\"" +
                        "  }," +
                        "  \"ok\": true" +
                        "}")));
    }

    @Test
    public void removeNullInInnerArray() {
        assertThat("remove from inner array ", TemplateUtil.removeNullFields(json(
                        "{" +
                        "  \"one\": 1," +
                        "  \"innerArray\": [" +
                        "    null," +
                        "    1," +
                        "    \"four\"," +
                        "    {" +
                        "      \"shouldRemoved\": null," +
                        "      \"ok\": \"\"" +
                        "    }," +
                        "    false" +
                        "  ]," +
                        "  \"ok\": true" +
                        "}")),
                is(json("{" +
                        "  \"one\": 1," +
                        "  \"innerArray\": [" +
                        "    null," +
                        "    1," +
                        "    \"four\"," +
                        "    {" +
                        "      \"ok\": \"\"" +
                        "    }," +
                        "    false" +
                        "  ]," +
                        "  \"ok\": true" +
                        "}")));
    }

    private static JsonNode json(@Language("JSON") String jsonString) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            return objectMapper.readTree(jsonString);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
