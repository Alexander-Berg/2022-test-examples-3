package ru.yandex.market.checkout.checkouter.json;

import org.json.JSONArray;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.springframework.http.MediaType;
import org.springframework.mock.http.MockHttpOutputMessage;

public class JSONArrayJsonSerializerTest extends AbstractJsonHandlerTestBase {

    private static final String JSON_SAMPLE_STR =
            "[\n" +
                    "  {\n" +
                    "    \"id\": \"7893318\",\n" +
                    "    \"type\": \"enum\",\n" +
                    "    \"name\": \"Производитель\",\n" +
                    "    \"xslname\": \"vendor\",\n" +
                    "    \"subType\": \"\",\n" +
                    "    \"kind\": 1,\n" +
                    "    \"position\": 1,\n" +
                    "    \"noffers\": 1,\n" +
                    "    \"values\": [\n" +
                    "      {\n" +
                    "        \"initialFound\": 1,\n" +
                    "        \"found\": 1,\n" +
                    "        \"value\": \"Nikon\",\n" +
                    "        \"vendor\": {\n" +
                    "          \"name\": \"Nikon\",\n" +
                    "          \"entity\": \"vendor\",\n" +
                    "          \"id\": 152826\n" +
                    "        },\n" +
                    "        \"id\": \"152826\"\n" +
                    "      }\n" +
                    "    ],\n" +
                    "    \"valuesGroups\": [\n" +
                    "      {\n" +
                    "        \"type\": \"all\",\n" +
                    "        \"valuesIds\": [\n" +
                    "          \"152826\"\n" +
                    "        ]\n" +
                    "      }\n" +
                    "    ]\n" +
                    "  },\n" +
                    "  {\n" +
                    "    \"id\": \"12842592\",\n" +
                    "    \"type\": \"enum\",\n" +
                    "    \"name\": \"Тип камеры\",\n" +
                    "    \"xslname\": \"CamType\",\n" +
                    "    \"subType\": \"\",\n" +
                    "    \"kind\": 1,\n" +
                    "    \"position\": 3,\n" +
                    "    \"noffers\": 1,\n" +
                    "    \"values\": [\n" +
                    "      {\n" +
                    "        \"initialFound\": 1,\n" +
                    "        \"found\": 1,\n" +
                    "        \"value\": \"зеркальная\",\n" +
                    "        \"id\": \"12842597\"\n" +
                    "      }\n" +
                    "    ],\n" +
                    "    \"valuesGroups\": [\n" +
                    "      {\n" +
                    "        \"type\": \"all\",\n" +
                    "        \"valuesIds\": [\n" +
                    "          \"12842597\"\n" +
                    "        ]\n" +
                    "      }\n" +
                    "    ]\n" +
                    "  }\n" +
                    "]";

    @Test
    public void shouldSerializeJSONObject() throws Exception {
        JSONArray jsonObject = new JSONArray(JSON_SAMPLE_STR);
        MockHttpOutputMessage outputMessage = new MockHttpOutputMessage();
        converter.write(jsonObject, MediaType.APPLICATION_JSON, outputMessage);
        JSONAssert.assertEquals(JSON_SAMPLE_STR, outputMessage.getBodyAsString(), false);
    }
}
