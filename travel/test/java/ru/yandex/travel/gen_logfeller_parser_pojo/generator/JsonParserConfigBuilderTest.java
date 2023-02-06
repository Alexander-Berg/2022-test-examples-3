package ru.yandex.travel.gen_logfeller_parser_pojo.generator;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import org.junit.Test;

import ru.yandex.travel.gen_logfeller_parser_pojo.annotations.LogfellerConfig;
import ru.yandex.travel.gen_logfeller_parser_pojo.annotations.LogfellerDecompose;
import ru.yandex.travel.gen_logfeller_parser_pojo.annotations.LogfellerIgnore;
import ru.yandex.travel.gen_logfeller_parser_pojo.generator.model.JsonParserConfig;
import ru.yandex.travel.gen_logfeller_parser_pojo.model.FieldDemand;
import ru.yandex.travel.gen_logfeller_parser_pojo.model.FieldType;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;


public class JsonParserConfigBuilderTest {

    @Data
    public static class SimpleClass {
        private String field;
    }

    @Test
    public void shouldBuildConfigForSimpleClass() {
        final JsonParserConfig config = JsonParserConfigBuilder.buildConfig(SimpleClass.class);

        assertEquals(FieldType.VT_STRING, config.getFields().get(0).getType());
        assertEquals("field", config.getFields().get(0).getName());

        assertNull(config.getFields().get(0).getPath());
        assertNull(config.getFields().get(0).getFieldDemand());
    }

    @Data
    public static class ClassWithAllFieldsType {
        private String stringField;
        private int primitiveIntegerField;
        private Integer integerField;
        private long primitiveLongField;
        private Long longField;
        private float primitiveFloatField;
        private Float floatField;
        private double primitiveDoubleField;
        private Double doubleField;
        private boolean primitiveBooleanField;
        private Boolean booleanField;
        private List<String> listOfStringsField;
        private SimpleClass simpleClassField;
    }

    @Test
    public void shouldBuildConfigForAllFieldsType() {
        final JsonParserConfig config = JsonParserConfigBuilder.buildConfig(ClassWithAllFieldsType.class);

        assertEquals(13, config.getFields().size());

        assertEquals(FieldType.VT_STRING, config.getFields().get(0).getType());
        assertEquals("stringField", config.getFields().get(0).getName());

        assertEquals(FieldType.VT_INT32, config.getFields().get(1).getType());
        assertEquals("primitiveIntegerField", config.getFields().get(1).getName());

        assertEquals(FieldType.VT_INT32, config.getFields().get(2).getType());
        assertEquals("integerField", config.getFields().get(2).getName());

        assertEquals(FieldType.VT_INT64, config.getFields().get(3).getType());
        assertEquals("primitiveLongField", config.getFields().get(3).getName());

        assertEquals(FieldType.VT_INT64, config.getFields().get(4).getType());
        assertEquals("longField", config.getFields().get(4).getName());

        assertEquals(FieldType.VT_DOUBLE, config.getFields().get(5).getType());
        assertEquals("primitiveFloatField", config.getFields().get(5).getName());

        assertEquals(FieldType.VT_DOUBLE, config.getFields().get(6).getType());
        assertEquals("floatField", config.getFields().get(6).getName());

        assertEquals(FieldType.VT_DOUBLE, config.getFields().get(7).getType());
        assertEquals("primitiveDoubleField", config.getFields().get(7).getName());

        assertEquals(FieldType.VT_DOUBLE, config.getFields().get(8).getType());
        assertEquals("doubleField", config.getFields().get(8).getName());

        assertEquals(FieldType.VT_BOOLEAN, config.getFields().get(9).getType());
        assertEquals("primitiveBooleanField", config.getFields().get(9).getName());

        assertEquals(FieldType.VT_BOOLEAN, config.getFields().get(10).getType());
        assertEquals("booleanField", config.getFields().get(10).getName());

        assertEquals(FieldType.VT_ANY, config.getFields().get(11).getType());
        assertEquals("listOfStringsField", config.getFields().get(11).getName());

        assertEquals(FieldType.VT_ANY, config.getFields().get(12).getType());
        assertEquals("simpleClassField", config.getFields().get(12).getName());
    }

    @Data
    public static class ClassWithIgnoredFields {
        private String field;

        @JsonIgnore
        private String ignoredField1;

        @LogfellerIgnore
        private String ignoredField2;
    }

    @Test
    public void shouldBuildConfigForClassWithIgnoredFields() {
        final JsonParserConfig config = JsonParserConfigBuilder.buildConfig(ClassWithIgnoredFields.class);

        assertEquals(1, config.getFields().size());

        assertEquals(FieldType.VT_STRING, config.getFields().get(0).getType());
        assertEquals("field", config.getFields().get(0).getName());

        assertNull(config.getFields().get(0).getPath());
        assertNull(config.getFields().get(0).getFieldDemand());
    }

    @Data
    public static class ClassWithTypeOverride {
        @LogfellerConfig(name = "override", demand = FieldDemand.REQUIRED, type = FieldType.VT_DOUBLE)
        private int field;
    }

    @Test
    public void shouldBuildConfigForClassWithTypeOverride() {
        final JsonParserConfig config = JsonParserConfigBuilder.buildConfig(ClassWithTypeOverride.class);

        assertEquals(1, config.getFields().size());

        assertEquals(FieldType.VT_DOUBLE, config.getFields().get(0).getType());
        assertEquals("override", config.getFields().get(0).getName());
        assertEquals("field", config.getFields().get(0).getPath());
        assertEquals(FieldDemand.REQUIRED, config.getFields().get(0).getFieldDemand());
    }

    @Data
    public static class NestedClass {
        private String simpleField;
    }

    @Data
    public static class ClassWithNestedFields {
        private String simpleField;

        @LogfellerDecompose
        private NestedClass nestedClass;
    }

    @Data
    public static class ClassWithNestedFieldsMisconfigured {
        @LogfellerConfig(type = FieldType.VT_STRING)
        @LogfellerDecompose
        private NestedClass nestedClass;
    }

    @Test
    public void shouldBuildConfigForClassWithNestedFields() {
        final JsonParserConfig config = JsonParserConfigBuilder.buildConfig(ClassWithNestedFields.class);

        assertEquals(2, config.getFields().size());

        assertEquals(FieldType.VT_STRING, config.getFields().get(0).getType());
        assertEquals("simpleField", config.getFields().get(0).getName());
        assertNull(config.getFields().get(0).getPath());
        assertNull(config.getFields().get(0).getFieldDemand());

        assertEquals(FieldType.VT_STRING, config.getFields().get(1).getType());
        assertEquals("nestedClass_simpleField", config.getFields().get(1).getName());
        assertEquals("nestedClass/simpleField", config.getFields().get(1).getPath());
        assertNull(config.getFields().get(1).getFieldDemand());
    }

    @Test(expected = IllegalStateException.class)
    public void shouldThrowOnMisconfiguredClass() {
        JsonParserConfigBuilder.buildConfig(ClassWithNestedFieldsMisconfigured.class);
    }
}
