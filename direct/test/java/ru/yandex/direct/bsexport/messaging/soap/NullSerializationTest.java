package ru.yandex.direct.bsexport.messaging.soap;

import com.google.protobuf.Value;
import org.junit.jupiter.api.Test;

import ru.yandex.direct.bsexport.messaging.BaseSerializationTest;
import ru.yandex.direct.testing.model.TestMessage;

import static com.google.protobuf.NullValue.NULL_VALUE;
import static org.assertj.core.api.Assertions.assertThat;

class NullSerializationTest extends BaseSerializationTest {

    @Test
    void nullValue_serializedAsNullType() {
        Value nullValue = Value.newBuilder().setNullValue(NULL_VALUE).build();
        TestMessage message = TestMessage.newBuilder().setValueField(nullValue).build();

        serialize(message);

        assertThat(soap).contains("<ValueField xsi:null=\"1\"/>");
    }
}
