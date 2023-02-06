package ru.yandex.direct.bsexport.messaging.json;

import com.google.common.primitives.UnsignedInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import ru.yandex.direct.bsexport.messaging.BaseSerializationTest;
import ru.yandex.direct.testing.model.TestMessage;

import static org.assertj.core.api.Assertions.assertThat;

public class IntegerSerializationTest extends BaseSerializationTest {
    private TestMessage.Builder builder;

    @BeforeEach
    void prepare() {
        builder = TestMessage.newBuilder();
    }

    private void serialize() {
        super.serialize(builder.build());
    }

    @Test
    void IsNotSet_NotSerialized() {
        serialize();

        assertThat(json).isEqualTo("{}");
    }

    @ParameterizedTest
    @ValueSource(ints = {0, Integer.MIN_VALUE, Integer.MAX_VALUE, 42, -500})
    void int32_SerializedAsIs(int value) {
        builder.setInt32Field(value);

        serialize();

        assertThat(json).isEqualTo("{\"Int32Field\":" + value + "}");
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 2, Integer.MIN_VALUE, Integer.MAX_VALUE, 40, -88800})
    void sfixed32_SerializedAsIs(int value) {
        builder.setSignedFixed32Field(value);

        serialize();

        assertThat(json).isEqualTo("{\"SignedFixed32Field\":" + value + "}");
    }

    @ParameterizedTest
    @ValueSource(longs = {0, 42, 2147483647, 3183856183L, 4294967294L, 4294967295L})
    void uint32_SerializedAsIs(long value) {
        UnsignedInteger unsignedInteger = UnsignedInteger.valueOf(value);
        builder.setUnsignedInt32Field(unsignedInteger.intValue());

        serialize();

        assertThat(json).contains("{\"UnsignedInt32Field\":" + value + "}");
    }

    @ParameterizedTest
    @ValueSource(longs = {Long.MIN_VALUE, Integer.MIN_VALUE, -333, 0, 42, Integer.MAX_VALUE, Long.MAX_VALUE})
    void int64_SerializedAsString(long value) {
        builder.setInt64Field(value);

        serialize();

        assertThat(json).contains("{\"Int64Field\":\"" + value + "\"}");
    }

    @ParameterizedTest
    @ValueSource(longs = {Long.MIN_VALUE, Integer.MIN_VALUE, -333, 0, 42, Integer.MAX_VALUE, Long.MAX_VALUE})
    void sfixed64_SerializedAsString(long value) {
        builder.setSignedFixed64Field(value);

        serialize();

        assertThat(json).contains("{\"SignedFixed64Field\":\"" + value + "\"}");
    }
}
