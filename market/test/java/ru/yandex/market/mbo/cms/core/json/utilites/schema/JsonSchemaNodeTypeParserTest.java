package ru.yandex.market.mbo.cms.core.json.utilites.schema;

import java.util.Arrays;
import java.util.LinkedHashSet;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Assert;
import org.junit.Test;

public class JsonSchemaNodeTypeParserTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Test
    public void processParentTypes() throws Exception {
        JsonNode parentTypes = OBJECT_MAPPER.readTree(
                "[{\n" +
                        "    \"$ref\": \"http://cms.market.yandex.ru/schemas/mbo/schema/jsonschema/1-0-0#definitions/" +
                        "WIDGET_REACT\"\n" +
                        "  },{\n" +
                        "    \"$ref\": \"http://cms.market.yandex.ru/schemas/mbo/schema/jsonschema/1-0-0#definitions/" +
                        "HasClientData\"\n" +
                        "  }]");
        LinkedHashSet<String> result = new LinkedHashSet<>(Arrays.asList("WIDGET_REACT", "HasClientData"));

        Assert.assertEquals(result, JsonSchemaNodeTypeParser.processParentTypes(parentTypes));
    }
}
