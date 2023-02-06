package ru.yandex.market.checkout.checkouter.json;

import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.springframework.http.MediaType;
import org.springframework.mock.http.MockHttpOutputMessage;

public class JSONObjectJsonSerializerTest extends AbstractJsonHandlerTestBase {

    private static final String JSON_SAMPLE_STR =
            "{\n" +
                    "  \"id\": \"7893318\",\n" +
                    "  \"type\": \"enum\",\n" +
                    "  \"name\": \"Производитель\",\n" +
                    "  \"xslname\": \"vendor\",\n" +
                    "  \"subType\": \"\",\n" +
                    "  \"kind\": 1,\n" +
                    "  \"position\": 1,\n" +
                    "  \"noffers\": 1,\n" +
                    "  \"values\": [\n" +
                    "    {\n" +
                    "      \"initialFound\": 1,\n" +
                    "      \"found\": 1,\n" +
                    "      \"value\": \"Nikon\",\n" +
                    "      \"vendor\": {\n" +
                    "        \"name\": \"Nikon\",\n" +
                    "        \"entity\": \"vendor\",\n" +
                    "        \"id\": 152826\n" +
                    "      },\n" +
                    "      \"id\": \"152826\"\n" +
                    "    }\n" +
                    "  ],\n" +
                    "  \"valuesGroups\": [\n" +
                    "    {\n" +
                    "      \"type\": \"all\",\n" +
                    "      \"valuesIds\": [\n" +
                    "        \"152826\"\n" +
                    "      ]\n" +
                    "    }\n" +
                    "  ]\n" +
                    "}";

    @Test
    public void shouldSerializeJSONObject() throws Exception {
        JSONObject jsonObject = new JSONObject(JSON_SAMPLE_STR);
        MockHttpOutputMessage outputMessage = new MockHttpOutputMessage();
        converter.write(jsonObject, MediaType.APPLICATION_JSON, outputMessage);
        JSONAssert.assertEquals(JSON_SAMPLE_STR, outputMessage.getBodyAsString(), false);
    }
}
