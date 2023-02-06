package ru.yandex.reminders.util.jsonld;

import org.junit.Test;

import ru.yandex.commune.json.JsonObject;
import ru.yandex.commune.json.JsonString;
import ru.yandex.misc.test.Assert;

/**
 * @author dbrylev
 */
public class SchemaOrgJsonLdProcessorTest {

    @Test
    public void markNotSchemaOrgRemoteContext() {
        JsonObject json = JsonObject.parseObject("{" +
                "  \"@context\":\"http://somewhere.com\"," +
                "  \"someField\":\"some-value\"" +
                "}");

        JsonObject compacted = SchemaOrgJsonLdProcessor.compact(json);
        Assert.some(JsonString.valueOf("some-value"), compacted.getO(":x(http://somewhere.com)/someField"));
    }

    @Test
    public void keepSchemaOrgField() {
        JsonObject json = JsonObject.parseObject("{" +
                "  \"http://schema.org/location\":\"Location\"," +
                "  \"location\":\"Omitted\"" +
                "}");

        JsonObject compacted = SchemaOrgJsonLdProcessor.compact(json);
        Assert.some(JsonString.valueOf("Location"), compacted.getO("location"));
    }
}
