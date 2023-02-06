package ru.yandex.market.crm.operatorwindow.json;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.market.crm.operatorwindow.serialization.EnumViewModule;
import ru.yandex.market.jmf.metainfo.MetaInfoSource;
import ru.yandex.market.jmf.script.ScriptType;
import ru.yandex.market.jmf.script.storage.ScriptInfo;
import ru.yandex.market.jmf.script.storage.impl.ScriptImpl;
import ru.yandex.market.jmf.ui.controller.actions.EditScriptRequest;
import ru.yandex.market.jmf.ui.controller.actions.ScriptForListResult;
import ru.yandex.market.jmf.utils.serialize.ObjectMapperFactory;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = ScriptSerializationTest.Configuration.class)
public class ScriptSerializationTest {

    @Inject
    private ObjectMapperFactory objectMapperFactory;

    public ObjectMapper getObjectMapper() {
        return objectMapperFactory.getJsonObjectMapper();
    }

    @Test
    public void testScriptTypeSerialization() throws JsonProcessingException {
        ObjectMapper mapper = getObjectMapper();
        Assertions.assertEquals("\"DEFAULT\"", mapper.writeValueAsString(ScriptType.DEFAULT));
        Assertions.assertEquals("\"MODULE\"", mapper.writeValueAsString(ScriptType.MODULE));
    }

    @Test
    public void testScriptTypeDeserialization() throws IOException {
        ObjectMapper mapper = getObjectMapper();
        Assertions.assertEquals(ScriptType.DEFAULT, mapper.readValue("\"DEFAULT\"", ScriptType.class));
        Assertions.assertEquals(ScriptType.MODULE, mapper.readValue("\"MODULE\"", ScriptType.class));
    }

    @Test
    public void testScriptForListResultSerialization() {
        ScriptImpl script = new ScriptImpl("scriptCode",
                Map.of("ru", "scriptTitle"),
                "scriptBody", OffsetDateTime.now(),
                ScriptType.MODULE,
                Set.of()
        );
        ScriptInfo scriptInfo = new ScriptInfo(MetaInfoSource.SYSTEM, script);
        ScriptForListResult value = new ScriptForListResult(scriptInfo);
        JsonNode res = getObjectMapper().convertValue(value, JsonNode.class);
        Assertions.assertEquals("scriptCode", res.get("code").textValue());
        Assertions.assertEquals("scriptTitle", res.get("title").textValue());
        Assertions.assertEquals("MODULE", res.get("type").textValue());
    }

    @Test
    public void testEditScriptRequestDeserialization() throws IOException {
        String value = "{"
                + "\"title\": \"scriptTitle\", "
                + "\"body\": \"scriptBody\", "
                + "\"type\": \"MODULE\""
                + "}";
        EditScriptRequest res = getObjectMapper().readValue(value, EditScriptRequest.class);
        Assertions.assertEquals("scriptTitle", res.getTitle());
        Assertions.assertEquals("scriptBody", res.getBody());
        Assertions.assertEquals(ScriptType.MODULE, res.getType());
    }

    @Import(ObjectMapperFactory.class)
    public static class Configuration {

        @Bean
        public EnumViewModule enumViewModule() {
            return new EnumViewModule(null, null);
        }

    }

}
