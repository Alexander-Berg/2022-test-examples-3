package ru.yandex.market.mbo.common.validation.json;

import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

/**
 * @author ayratgdl
 * @date 19.02.18
 */
public class JsonSchemaValidatorTest {
    private static final String SCHEMA = "{\n" +
        "  \"type\": \"object\",\n" +
        "  \"properties\": {\n" +
        "    \"a\": { \"type\": \"number\"}\n" +
        "  }\n" +
        "}";

    @Test
    public void allValidSchema() throws JsonValidationException {
        JsonSchemaValidator validator = JsonSchemaValidator.createFromSchema("{}");
        String validJson = "{\"a\": 5}";

        // no exception
        validator.validate(validJson);
    }

    @Test(expected = JsonValidationException.class)
    public void wrongJson() throws JsonValidationException {
        JsonSchemaValidator validator = JsonSchemaValidator.createFromSchema("{}");
        String wrongJson = "wrong json";

        validator.validate(wrongJson);
    }

    @Test(expected = IllegalArgumentException.class)
    public void wrongSchema() {
        JsonSchemaValidator.createFromSchema("wrong schema");
    }

    @Test(expected = JsonValidationException.class)
    public void emptyJson() throws JsonValidationException {
        JsonSchemaValidator validator = JsonSchemaValidator.createFromSchema(SCHEMA);

        // no exception
        validator.validate("");
    }

    @Test
    public void validateJsonObject() throws JsonValidationException {
        JsonSchemaValidator validator = JsonSchemaValidator.createFromSchema(SCHEMA);
        String validJson = "{\"a\": 5}";

        // no exception
        validator.validate(validJson);
    }

    @Test(expected = JsonValidationException.class)
    public void validateWrongJsonObject() throws JsonValidationException {
        JsonSchemaValidator validator = JsonSchemaValidator.createFromSchema(SCHEMA);
        String noValidJson = "{\"a\": \"aa\"}";

        validator.validate(noValidJson);
    }

    @Test
    public void validateJsonArray() throws JsonValidationException {
        String schema = "{ \"type\": \"array\" }";

        JsonSchemaValidator validator = JsonSchemaValidator.createFromSchema(schema);
        String validJson = "[{\"a\": 5}]";

        // no exception
        validator.validate(validJson);
    }

    @Test
    public void validateJsonFromFile() throws IOException, JsonValidationException {
        File jsonFile = null;
        try {
            jsonFile = Files.createTempFile("json-schema-validate-test-", ".json").toFile();
            Files.write(jsonFile.toPath(), "{\"a\": 5}".getBytes());

            JsonSchemaValidator validator = JsonSchemaValidator.createFromSchema(SCHEMA);

            // no exception
            validator.validate(jsonFile.toPath());
        } finally {
            if (jsonFile != null) {
                jsonFile.delete();
            }
        }
    }

    @Test
    public void validateJsonWhereSchemaFromClasspath() throws JsonValidationException {
        JsonSchemaValidator validator = JsonSchemaValidator.createFromClasspath("/test.schema.json");
        String validJson = "{\"a\": 5}";

        // no exception
        validator.validate(validJson);
    }

    @Test
    public void schemaRefersToExternalSchema() throws JsonValidationException {
        String externalSchema = "{ \"id\": \"external.schema.json\", \"type\": \"number\" }";
        String schema = "{ \"type\": \"object\", \"properties\": { \"a\": { \"$ref\": \"external.schema.json\" } } }";

        JsonSchemaValidator validator = JsonSchemaValidator.createFromSchema(schema, externalSchema);

        // no exception
        validator.validate("{ \"a\": 123 }");
    }

    @Test (expected = JsonValidationException.class)
    public void jsonIsNotValidByExternalSchema() throws JsonValidationException {
        String externalSchema = "{ \"id\": \"external.schema.json\", \"type\": \"number\" }";
        String schema = "{ \"type\": \"object\", \"properties\": { \"a\": { \"$ref\": \"external.schema.json\" } } }";

        JsonSchemaValidator validator = JsonSchemaValidator.createFromSchema(schema, externalSchema);

        validator.validate("{ \"a\": \"abc\" }");
    }

    @Test
    public void schemaWithRefOnSelfAndExternalSchema() throws JsonValidationException {
        String externalSchema = "{ \"id\": \"external.schema.json\", \"type\": \"number\" }";
        String schema = "{\n" +
            "  \"type\": \"object\",\n" +
            "  \"properties\": {\n" +
            "    \"a\": { \"$ref\": \"external.schema.json\" },\n" +
            "    \"b\": { \"$ref\": \"#/definitions/c\" }\n" +
            "  },\n" +
            "  \"definitions\": {\n" +
            "    \"c\": { \"type\": \"boolean\" }\n" +
            "  }\n" +
            "}";

        JsonSchemaValidator validator = JsonSchemaValidator.createFromSchema(schema, externalSchema);

        validator.validate("{ \"a\": 123, \"b\": true }");
    }

    @Test
    public void schemaWithIdRefersToItself() throws JsonValidationException {
        String schema = "{\n" +
            "  \"id\": \"schema.id\",\n" +
            "  \"type\": \"object\",\n" +
            "  \"properties\": {\n" +
            "    \"a\": { \"$ref\": \"#/definitions/b\" }\n" +
            "  },\n" +
            "  \"definitions\": {\n" +
            "    \"b\": { \"type\": \"number\" }\n" +
            "  }\n" +
            "}";

        JsonSchemaValidator validator = JsonSchemaValidator.createFromSchema(schema);
        validator.validate("{ \"a\": 123 }");
    }

    @Test
    public void schemaRefersToExternalSchemaWithPoundSymbol() throws JsonValidationException {
        String externalSchema = "{\n" +
            "  \"id\": \"external.schema.json\",\n" +
            "  \"definitions\": {\n" +
            "    \"a\": { \"type\": \"number\" }\n" +
            "  }\n" +
            "}";
        String schema = "{\n" +
            "  \"type\": \"object\",\n" +
            "  \"properties\": {\n" +
            "    \"b\": { \"$ref\": \"external.schema.json#/definitions/a\" }\n" +
            "  }\n" +
            "}";

        JsonSchemaValidator validator = JsonSchemaValidator.createFromSchema(schema, externalSchema);

        validator.validate("{ \"b\": 123 }");
    }

    @Test (expected = RuntimeException.class)
    public void externalSchemaWithoutId() {
        String externalSchema = "{ \"type\": \"number\" }";
        String schema = "{}";

        JsonSchemaValidator validator = JsonSchemaValidator.createFromSchema(schema, externalSchema);
    }

    @Test (expected = RuntimeException.class)
    public void schemaRefersToUnknownId() throws JsonValidationException {
        String schema = "{ \"$ref\": \"unknown.id\" }";

        JsonSchemaValidator validator = JsonSchemaValidator.createFromSchema(schema);

        validator.validate("{ \"a\": 5 }");
    }
}
