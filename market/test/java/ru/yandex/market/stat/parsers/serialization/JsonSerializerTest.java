package ru.yandex.market.stat.parsers.serialization;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Denis Khurtin
 */
public class JsonSerializerTest {

    private static final String testObjectJsonString = "{" +
            "\"name\":\"test\"," +
            "\"age\":27," +
            "\"subObject\":{" +
            "\"longs\":[12,0,34,-557,96]," +
            "\"modules\":{\"m1\":true,\"m2\":false}" +
            "}" +
            "}";

    private static final String testObjectPrettyJsonString = "{\n" +
            "  \"name\" : \"test\",\n" +
            "  \"age\" : 27,\n" +
            "  \"subObject\" : {\n" +
            "    \"longs\" : [ 12, 0, 34, -557, 96 ],\n" +
            "    \"modules\" : {\n" +
            "      \"m1\" : true,\n" +
            "      \"m2\" : false\n" +
            "    }\n" +
            "  }\n" +
            "}";

    @Test
    public void fromJson() {
        // When
        TestObject result = JsonSerializer.fromJson(testObjectJsonString, TestObject.class);

        // Then
        assertThat(result, equalTo(createTestObject()));
    }

    @Test
    public void fromPrettyJson() {
        // When
        TestObject result = JsonSerializer.fromJson(testObjectPrettyJsonString, TestObject.class);

        // Then
        assertThat(result, equalTo(createTestObject()));
    }

    @Test
    public void toJson() {
        // Given
        Object testObject = createTestObject();

        // When
        String result = JsonSerializer.toJson(testObject);

        // Then
        assertThat(result, equalTo(testObjectJsonString));
    }

    @Test
    public void toPrettyJson() {
        System.setProperty("line.separator", "\n");
        // Given
        Object testObject = createTestObject();

        // When
        String result = JsonSerializer.toJson(testObject, true);

        // Then
        assertThat(result, equalTo(testObjectPrettyJsonString));
    }

    @SuppressWarnings("UnusedDeclaration")
    private static class TestObject {
        public String name;
        public int age;
        public TestSubObject subObject;

        private TestObject() {
        }

        private TestObject(String name, int age, TestSubObject subObject) {
            this.name = name;
            this.age = age;
            this.subObject = subObject;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }

            if (obj == null || obj.getClass() != this.getClass()) {
                return false;
            }

            TestObject that = (TestObject) obj;

            return Objects.equal(this.name, that.name) &&
                   this.age == that.age &&
                    Objects.equal(this.subObject, that.subObject);
        }
    }

    @SuppressWarnings("UnusedDeclaration")
    private static class TestSubObject {
        public List<Long> longs;
        public Map<String, Boolean> modules;

        private TestSubObject() {
        }

        private TestSubObject(List<Long> longs, Map<String, Boolean> modules) {
            this.longs = longs;
            this.modules = modules;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }

            if (obj == null || obj.getClass() != this.getClass()) {
                return false;
            }

            TestSubObject that = (TestSubObject) obj;

            return Objects.equal(this.longs, that.longs) &&
                    Objects.equal(this.modules, that.modules);
        }
    }

    private TestObject createTestObject() {
        return new TestObject("test", 27, new TestSubObject(
                ImmutableList.of(12L, 0L, 34L, -557L, 96L),
                ImmutableMap.of("m1", true, "m2", false)
        ));
    }
}
