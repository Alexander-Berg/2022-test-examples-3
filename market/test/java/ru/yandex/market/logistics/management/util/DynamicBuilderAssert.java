package ru.yandex.market.logistics.management.util;

import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.stream.StreamSupport;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.google.protobuf.Message;
import com.googlecode.protobuf.format.JsonFormat;
import net.javacrumbs.jsonunit.core.Option;
import org.assertj.core.api.AbstractAssert;

import ru.yandex.market.logistics.Logistics;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;

public class DynamicBuilderAssert extends AbstractAssert<DynamicBuilderAssert, Logistics.MetaInfo> {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final BiConsumer<String, String> JSON_TESTER = (s1, s2) ->
        assertThatJson(s2)
            .when(Option.IGNORING_ARRAY_ORDER)
            .isEqualTo(s1);

    public DynamicBuilderAssert(Logistics.MetaInfo actual) {
        super(actual, DynamicBuilderAssert.class);
    }

    public static DynamicBuilderAssert assertThat(Logistics.MetaInfo actual) {
        return new DynamicBuilderAssert(actual);
    }

    public DynamicBuilderAssert hasSameFFsAs(String pathToJson) {
        isNotNull();
        String expectedJson = UnitTestUtil.readFile(pathToJson);
        String actualJson = getField(Logistics.MetaInfo.WAREHOUSES_FIELD_NUMBER, actual.getWarehousesList());
        JSON_TESTER.accept(expectedJson, actualJson);
        return this;
    }

    public DynamicBuilderAssert hasSameFFsAndDssAs(String pathToJson) {
        isNotNull();
        String expectedJson = UnitTestUtil.readFile(pathToJson);
        String actualJson = getField(Logistics.MetaInfo.WAREHOUSES_AND_DELIVERY_SERVICES_FIELD_NUMBER,
            actual.getWarehousesAndDeliveryServicesList());
        JSON_TESTER.accept(expectedJson, actualJson);
        return this;
    }

    public DynamicBuilderAssert hasSameWhsToWhsAs(String pathToJson) {
        isNotNull();
        String expectedJson = UnitTestUtil.readFile(pathToJson);
        String actualJson = getField(Logistics.MetaInfo.WAREHOUSES_TO_WAREHOUSES_FIELD_NUMBER,
                actual.getWarehousesToWarehousesList());
        JSON_TESTER.accept(expectedJson, actualJson);
        return this;
    }

    public DynamicBuilderAssert hasSameDaySetAs(String pathToJson) {
        isNotNull();
        String expectedJson = UnitTestUtil.readFile(pathToJson);
        String actualJson = getField(Logistics.MetaInfo.DAYS_SETS_FIELD_NUMBER,
            actual.getDaysSetsList());
        JSON_TESTER.accept(expectedJson, actualJson);
        return this;
    }

    public DynamicBuilderAssert hasSameDSsAs(String pathToJson) {
        isNotNull();
        String expectedJson = UnitTestUtil.readFile(pathToJson);
        String actualJson = getField(Logistics.MetaInfo.DELIVERY_SERVICES_FIELD_NUMBER,
            actual.getDeliveryServicesList());
        JSON_TESTER.accept(expectedJson, actualJson);
        return this;
    }

    public DynamicBuilderAssert hasSameTimeIntervalsAs(String pathToJson) {
        isNotNull();
        String expectedJson = UnitTestUtil.readFile(pathToJson);
        String actualJson = getField(Logistics.MetaInfo.TIME_INTERVALS_SETS_FIELD_NUMBER,
            actual.getTimeIntervalsSetsList());
        JSON_TESTER.accept(expectedJson, actualJson);
        return this;
    }

    public DynamicBuilderAssert hasShopsAndFFsAs(String pathToJson) {
        isNotNull();
        String expectedJson = UnitTestUtil.readFile(pathToJson);
        String actualJson = getField(Logistics.MetaInfo.SHOPS_AND_WAREHOUSES_FIELD_NUMBER,
            actual.getShopsAndWarehousesList());
        JSON_TESTER.accept(expectedJson, actualJson);
        return this;
    }

    private String getField(int fieldConstant, Iterable<? extends Message> iterable) {
        return printToJson(getFieldName(fieldConstant), iterable);
    }

    private String getFieldName(int fieldNumber) {
        return actual.getDescriptorForType()
            .findFieldByNumber(fieldNumber)
            .getName();
    }

    private static String printToJson(String wrapperProperty, Iterable<? extends Message> list) {
        return Optional.of(list)
            .map(DynamicBuilderAssert::listToJson)
            .filter(a -> a.size() != 0)
            .map(a -> MAPPER.createObjectNode().set(wrapperProperty, a))
            .map(UnitTestUtil::objectToPrettyString)
            .orElse(UnitTestUtil.objectToPrettyString(JsonNodeFactory.instance.objectNode()));
    }

    private static ArrayNode listToJson(Iterable<? extends Message> list) {
        return StreamSupport.stream(list.spliterator(), false)
            .map(JsonFormat::printToString)
            .map(UnitTestUtil::stringToJsonNode)
            .collect(UnitTestUtil.toArrayNode());
    }
}
