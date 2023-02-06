package ru.yandex.direct.bsexport.messaging.soap;

import com.google.common.primitives.UnsignedInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import ru.yandex.direct.bsexport.messaging.BaseSerializationTest;
import ru.yandex.direct.testing.model.TestMessage;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.bsexport.testing.Util.getFromClasspath;

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

        String expected = getFromClasspath("soap/update_data2_empty.xml");
        assertThat(soap).isXmlEqualTo(expected);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, Integer.MIN_VALUE, Integer.MAX_VALUE, 42, -500})
    void int32_SerializedAsIs(int value) {
        builder.setInt32Field(value);

        serialize();

        assertThat(soap).contains("<Int32Field xsi:type=\"xsd:int\">" + value + "</Int32Field>");
    }

    @ParameterizedTest
    @ValueSource(ints = {0, Integer.MIN_VALUE, Integer.MAX_VALUE, 43, -501})
    void sfixed32_SerializedAsIs(int value) {
        builder.setSignedFixed32Field(value);

        serialize();

        assertThat(soap).contains("<SignedFixed32Field xsi:type=\"xsd:int\">" + value + "</SignedFixed32Field>");
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "42", "2147483647", "3183856183", "4294967294", "4294967295"})
    void uint32_SerializedAsIs(String value) {
        UnsignedInteger unsignedInteger = UnsignedInteger.valueOf(value);
        builder.setUnsignedInt32Field(unsignedInteger.intValue());

        serialize();

        assertThat(soap).contains("<UnsignedInt32Field xsi:type=\"xsd:int\">" + value + "</UnsignedInt32Field>");
    }

    @ParameterizedTest
    @ValueSource(longs = {Long.MIN_VALUE, Integer.MIN_VALUE, -333, 0, 42, Integer.MAX_VALUE, Long.MAX_VALUE})
    void int64_SerializedAsIs(long value) {
        builder.setInt64Field(value);

        serialize();

        assertThat(soap).contains("<Int64Field xsi:type=\"xsd:int\">" + value + "</Int64Field>");
    }

    @ParameterizedTest
    @ValueSource(longs = {Long.MIN_VALUE, Integer.MIN_VALUE, -343, 0, 1, 41, Integer.MAX_VALUE, Long.MAX_VALUE})
    void sfixed64_SerializedAsIs(long value) {
        builder.setSignedFixed64Field(value);

        serialize();

        assertThat(soap).contains("<SignedFixed64Field xsi:type=\"xsd:int\">" + value + "</SignedFixed64Field>");
    }
}
