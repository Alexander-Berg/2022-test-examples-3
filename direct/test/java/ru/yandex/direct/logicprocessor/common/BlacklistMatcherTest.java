package ru.yandex.direct.logicprocessor.common;

import java.util.List;
import java.util.Map;

import javax.annotation.ParametersAreNonnullByDefault;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;

import ru.yandex.direct.core.entity.essblacklist.model.EssBlacklistItem;
import ru.yandex.direct.ess.common.models.BaseLogicObject;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

@ParametersAreNonnullByDefault
public class BlacklistMatcherTest {

    @Test
    public void simpleObjectMatchesSpec() {
        var matcher = BlacklistMatcher.create(
                List.of(new EssBlacklistItem().withFilterSpec("{\"long\": 1234567}")),
                TestLogicObject.class
        );

        var logicObject = new TestLogicObject(null, 1234567L, 42);
        assertThat(matcher.matches(logicObject), is(true));
    }

    @Test
    public void simpleObjectNotMatchesSpec() {
        var matcher = BlacklistMatcher.create(
                List.of(new EssBlacklistItem().withFilterSpec("{\"long\": 1234567}")),
                TestLogicObject.class
        );

        var logicObject = new TestLogicObject(null, 13L, 42);
        assertThat(matcher.matches(logicObject), is(false));
    }

    @Test
    public void specContainsFieldFromLogicObjectSuperClass() {
        var matcher = BlacklistMatcher.create(
                List.of(new EssBlacklistItem().withFilterSpec("{\"str\": \"100500\"}")),
                TestLogicObject.class
        );

        var logicObject = new TestLogicObject("100500", 13L, 42);
        assertThat(matcher.matches(logicObject), is(true));
    }

    @Test
    public void complicatedObjectMatchesSpec() {
        var matcher = BlacklistMatcher.create(
                List.of(new EssBlacklistItem().withFilterSpec("{" +
                        "\"map\":{\"id\":300500,\"val\":1234567}," +
                        "\"list\": [\"1\"]" +
                        "}")),
                TestLogicObjectWithComplexProps.class
        );

        Map<String, Long> map = Map.of("id", 300500L, "val", 1234567L);
        List<String> list = List.of("1");

        var logicObject = new TestLogicObjectWithComplexProps(map, list);
        assertThat(matcher.matches(logicObject), is(true));
    }

    @Test
    public void complicatedObjectNotMatchesSpec() {
        var matcher = BlacklistMatcher.create(
                List.of(new EssBlacklistItem().withFilterSpec("{" +
                        "\"map\":{\"id\":300500}," +
                        "\"list\": [\"1\"]" +
                        "}")),
                TestLogicObjectWithComplexProps.class
        );

        // в фильтре задано только {id: 300500}, поэтому объект не сматчится,
        // т.к. значение поля map не равно значению этого поля в фильтре
        Map<String, Long> map = Map.of("id", 300500L, "val", 1234567L);
        List<String> list = List.of("1");

        var logicObject = new TestLogicObjectWithComplexProps(map, list);
        assertThat(matcher.matches(logicObject), is(false));
    }

    @Test
    public void specContainsNoAnyFieldsFromLogicObjectType() {
        var matcher = BlacklistMatcher.create(
                List.of(new EssBlacklistItem().withFilterSpec("{\"unknownStr\": \"test\", \"unknownLong\": 1234567}")),
                TestLogicObject.class
        );

        var logicObject = new TestLogicObject("test", 1234567L, 42);
        assertThat(matcher.matches(logicObject), is(false));
    }

    @Test
    public void specContainsNullFields() {
        var matcher = BlacklistMatcher.create(
                List.of(new EssBlacklistItem().withFilterSpec("{\"str\": \"test\", \"long\": null}")),
                TestLogicObject.class
        );

        var softly = new SoftAssertions();
        softly.assertThat(matcher.matches(new TestLogicObject("test", 2L, 100))).isFalse();
        softly.assertThat(matcher.matches(new TestLogicObject("test", null, 100))).isTrue();
        softly.assertAll();
    }

    @Test
    public void specContainsFieldsWithInvalidValues() {
        var matcher = BlacklistMatcher.create(
                List.of(new EssBlacklistItem().withFilterSpec(
                        // типы данных в json не соответствуют типам свойств класса
                        "{\"str\": 42, \"long\": \"foo\", \"primitiveLong\": {\"id\":300500}}")),
                TestLogicObject.class
        );

        var softly = new SoftAssertions();
        // объект, очевидно, не сматчится, но и исключения не будет
        softly.assertThat(matcher.matches(new TestLogicObject("test", 2L, 100))).isFalse();
        softly.assertAll();
    }

    private static class TestLogicObject extends BaseLogicObject {
        @JsonProperty("str")
        private final String stringProp;

        @JsonProperty("long")
        private final Long longProp;

        @JsonProperty("primitiveLong")
        private final long primitiveLongProp;

        @JsonCreator
        public TestLogicObject(@JsonProperty("str") String stringProp, @JsonProperty("long") Long longProp,
                               @JsonProperty("primitiveLong") long primitiveLongProp) {
            this.stringProp = stringProp;
            this.longProp = longProp;
            this.primitiveLongProp = primitiveLongProp;
        }

        public String getStringProp() {
            return stringProp;
        }

        public Long getLongProp() {
            return longProp;
        }

        public long getPrimitiveLongProp() {
            return primitiveLongProp;
        }
    }

    private static class TestLogicObjectWithComplexProps extends BaseLogicObject {
        @JsonProperty("map")
        private final Map<String, Long> strToLongMap;

        @JsonProperty("list")
        private final List<String> strList;

        @JsonCreator
        public TestLogicObjectWithComplexProps(@JsonProperty("map") Map<String, Long> strToLongMap,
                                               @JsonProperty("list") List<String> strList) {
            this.strToLongMap = strToLongMap;
            this.strList = strList;
        }

        public Map<String, Long> getStrToLongMap() {
            return strToLongMap;
        }

        public List<String> getStrList() {
            return strList;
        }
    }
}
