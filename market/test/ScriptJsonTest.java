package ru.yandex.market.jmf.logic.def.test;

import java.io.IOException;
import java.util.Map;

import javax.inject.Inject;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.jmf.bcp.BcpService;
import ru.yandex.market.jmf.entity.Entity;
import ru.yandex.market.jmf.metadata.Fqn;
import ru.yandex.market.jmf.script.ScriptService;

@Transactional
@SpringJUnitConfig(classes = InternalLogicDefaultTestConfiguration.class)
public class ScriptJsonTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private static final Fqn FQN = Fqn.of("entityWithJsonAttr");

    @Inject
    BcpService bcpService;

    @Inject
    ScriptService scriptService;

    @Test
    public void checkJsonNullValue() throws IOException {
        JsonNode json = mapper.readTree("null");
        Object result = saveValueToAttributeAndExecScript(json, "e.attr0");
        Assertions.assertNull(result, "JSON type attribute is returned as null");
    }

    @Test
    public void checkJsonBooleanValue() throws IOException {
        JsonNode json = mapper.readTree("true");
        Object result = saveValueToAttributeAndExecScript(json, "e.attr0");
        Assertions.assertEquals(Boolean.TRUE, result, "JSON type attribute is returned as null");
    }

    @Test
    public void checkJsonDoubleValue() throws IOException {
        String val = "1234.56";
        JsonNode json = mapper.readTree(val);
        Object result = saveValueToAttributeAndExecScript(json, "e.attr0");
        Assertions.assertEquals(
                Double.valueOf(val), result, "JSON type attribute is returned as double");
    }

    @Test
    public void checkJsonIntValue() throws IOException {
        String val = "123456";
        JsonNode json = mapper.readTree(val);
        Object result = saveValueToAttributeAndExecScript(json, "e.attr0");
        Assertions.assertEquals(
                Integer.valueOf(val), result, "JSON type attribute is returned as int");
    }

    @Test
    public void checkJsonTextValue() {
        String value = "val";
        JsonNode json = new TextNode(value);
        Object result = saveValueToAttributeAndExecScript(json, "e.attr0");
        Assertions.assertEquals(value, result, "JSON type attribute is returned as String");
    }

    @Test
    public void checkJsonObjectDestruct() {
        String value = "val";
        JsonNode json = mapper.createObjectNode()
                .set("a", mapper.createObjectNode().put("b", value));
        Object result = saveValueToAttributeAndExecScript(json, "e.attr0.a.b");
        Assertions.assertEquals(value, result, "JSON type attribute is destructible by object keys");
    }

    @Test
    public void checkJsonArrayDestruct() {
        String value1 = "val1";
        String value2 = "val2";
        JsonNode json = mapper.createArrayNode()
                .add(value1)
                .add(value2);
        Object result = saveValueToAttributeAndExecScript(json, "e.attr0[1]");
        Assertions.assertEquals(value2, result, "JSON type attribute is destructible by array index");
    }

    public Object saveValueToAttributeAndExecScript(JsonNode json, String script) {
        Entity e = create(json);
        Map<String, Object> variables = Map.of("e", e);
        return scriptService.execute(script, variables);
    }

    private Entity create(JsonNode json) {
        Map<String, Object> properties = ImmutableMap.of("attr0", json);
        return bcpService.create(FQN, properties);
    }
}
