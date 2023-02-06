package ru.yandex.market.crm.operatorwindow.external;

import java.util.Optional;
import java.util.function.Function;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.market.crm.operatorwindow.external.smartcalls.SmartCallsSerializationHelper;

public class ScenarioStepFieldParserTest {

    private static final SmartCallsSerializationHelper helper = SmartcallsTestUtils.createSerializationHelper();

    @Test
    public void intNode() {
        assertFieldValue("123", parse(x -> x.numberNode(123)));
    }

    @Test
    public void longNode() {
        assertFieldValue("124", parse(x -> x.numberNode(124L)));
    }

    @Test
    public void doubleNode() {
        assertIgnored(parse(x -> x.numberNode(2.3)));
    }

    @Test
    public void nullNode() {
        assertFieldValue("", parse(JsonNodeFactory::nullNode));
    }

    @Test
    public void stringNode() {
        assertFieldValue("abc", parse(x -> x.textNode("abc")));
    }

    @Test
    public void arrayNode() {
        assertIgnored(parse(JsonNodeFactory::arrayNode));
    }

    @Test
    public void objectNode() {
        assertIgnored(parse(JsonNodeFactory::objectNode));
    }

    @Test
    public void booleanNode() {
        assertFieldValue("true", parse(x -> x.booleanNode(true)));
        assertFieldValue("false", parse(x -> x.booleanNode(false)));
    }

    private Optional<String> parse(Function<JsonNodeFactory, JsonNode> jsonNodeCreator) {
        return helper.getCallDataFieldValue(jsonNodeCreator.apply(getJsonNodeFactory()));
    }

    private void assertFieldValue(String expected, Optional<String> actual) {
        Assertions.assertTrue(actual.isPresent());
        Assertions.assertEquals(expected, actual.get());
    }

    private void assertIgnored(Optional<String> actual) {
        Assertions.assertFalse(actual.isPresent());
    }

    private JsonNodeFactory getJsonNodeFactory() {
        return JsonNodeFactory.instance;
    }

}
