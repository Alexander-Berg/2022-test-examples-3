package ru.yandex.market.abo.core.crm;

import java.io.File;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jackson.JsonLoader;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
import com.github.fge.jsonschema.main.JsonValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.abo.core.crm.events.CrmEvent;
import ru.yandex.market.abo.core.crm.events.PingerEvent;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author antipov93@yndx-team.ru
 */
public class CrmEventTest {

    private ObjectMapper objectMapper = new ObjectMapper();
    private JsonValidator validator;
    private JsonNode schema;

    @BeforeEach
    public void initialize() throws Exception {
        String path = getClass().getResource("/crm/schema.json").getFile();
        schema = JsonLoader.fromFile(new File(path));
        validator = JsonSchemaFactory.byDefault().getValidator();
    }

    @Test
    public void testOpenCutoffEvent() throws Exception {
        CrmEvent event = new PingerEvent(774L, true);
        String serialized = objectMapper.writeValueAsString(event);
        System.out.println(serialized);
        validate(serialized);
    }

    private void validate(String json) throws Exception {
        JsonNode data = JsonLoader.fromString(json);
        assertTrue(validator.validate(schema, data).isSuccess());
    }
}
