package ru.yandex.direct.bsexport.messaging.json;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import ru.yandex.direct.bsexport.messaging.BaseSerializationTest;
import ru.yandex.direct.testing.model.TestMessage;

import static org.assertj.core.api.Assertions.assertThat;

class StringSerializationTest extends BaseSerializationTest {

    private TestMessage.Builder builder;

    @BeforeEach
    void prepare() {
        builder = TestMessage.newBuilder();
    }

    private void serialize() {
        super.serialize(builder.build());
    }

    @Test
    void stringIsNotSet_NotSerialized() {
        serialize();

        assertThat(json).isEqualTo("{}");
    }

    @Test
    void emptyString_SerializedAsIs() {
        builder.setStringField("");

        serialize();

        assertThat(json).isEqualTo("{\"StringField\":\"\"}");
    }

    @ParameterizedTest
    @ValueSource(strings = {"qwerty", "0123456789", "abc, 987! -- ZWY", "?", ":", ";", "/", "|", "~", "`",
            "!", "@", "_", "-", "+", "()", "[]", "{}", "^", "*", "$", "%", ",.", "#"})
    void latinString_SerializedAsIs(String str) {
        builder.setStringField(str);

        serialize();

        assertThat(json).isEqualTo("{\"StringField\":\"" + str + "\"}");
    }

    @ParameterizedTest
    @ValueSource(strings = {"\\", "\""})
    void latinString_SerializedEscaped(String str) {
        builder.setStringField(str);

        serialize();

        assertThat(json).isEqualTo("{\"StringField\":\"\\" + str + "\"}");
    }

    /**
     * в perl сериализовалось as-is
     */
    @Test
    void equalsSign_serializedAsUnicode_notAsInPerl() {
        builder.setStringField("=");

        serialize();

        assertThat(json).isEqualTo("{\"StringField\":\"\\u003d\"}");
    }

    /**
     * в perl сериализовалось as-is
     */
    @Test
    void apostrophe_serializedAsUnicode_notAsInPerl() {
        builder.setStringField("'");

        serialize();

        assertThat(json).isEqualTo("{\"StringField\":\"\\u0027\"}");
    }

    @Test
    void carriageReturnString_serializedEscaped() {
        builder.setStringField("before\rafter");

        serialize();

        assertThat(json).isEqualTo("{\"StringField\":\"before\\rafter\"}");
    }

    @Test
    void lineSeparatorString_serializedEscaped() {
        builder.setStringField("before\nafter");

        serialize();

        assertThat(json).isEqualTo("{\"StringField\":\"before\\nafter\"}");
    }

    @ParameterizedTest
    @ValueSource(strings = {"строка", "Шлакоблоки по 300", "запчасть №2", "абвгд abcdef"})
    void nationalString_SerializedAsIs(String str) {
        builder.setStringField(str);

        serialize();

        assertThat(json).isEqualTo("{\"StringField\":\"" + str + "\"}");
    }
}
