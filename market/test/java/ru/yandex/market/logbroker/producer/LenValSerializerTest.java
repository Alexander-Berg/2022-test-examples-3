package ru.yandex.market.logbroker.producer;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.protobuf.MessageLite;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.logbroker.event.BaseProtobufLogbrokerEvent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class LenValSerializerTest {
    static Stream<Arguments> constructorData() {
        return Stream.of(
            arguments(0, 0, Integer.MAX_VALUE, Integer.MAX_VALUE),
            arguments(Integer.MAX_VALUE, 0, Integer.MAX_VALUE, Integer.MAX_VALUE),
            arguments(0, Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE),
            arguments(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE),
            arguments(1, 1, 1, 1024),
            arguments(10, 8, 10, 8192),
            arguments(10, Integer.MAX_VALUE >> 3, 10, Integer.MAX_VALUE)
        );
    }

    @ParameterizedTest
    @MethodSource("constructorData")
    void constructor(
            int maxChunkSizeIn,
            int maxChunkSizeKilobytesIn,
            int maxChunkSizeOut,
            int maxChunkSizeBytesOut
    ) {
        LenValSerializer serializer = new LenValSerializer(maxChunkSizeIn, maxChunkSizeKilobytesIn);
        assertThat(serializer.getMaxChunkSize()).isEqualTo(maxChunkSizeOut);
        assertThat(serializer.getMaxChunkSizeBytes()).isEqualTo(maxChunkSizeBytesOut);
    }

    @Test
    void serializeEmptyList() {
        LenValSerializer serializer = new LenValSerializer(Integer.MAX_VALUE, Integer.MAX_VALUE, true);
        List<MessageLite> protos = Arrays.asList();
        assertThat(serializer.serialize(protos)).isEmpty();
    }

    @Test
    void serializeOne() {
        LenValSerializer serializer = new LenValSerializer(Integer.MAX_VALUE, Integer.MAX_VALUE, true);
        List<MessageLite> protos = Arrays.asList(
                makeProto("1")
        );
        assertThat(serializer.serialize(protos)).containsExactly(new byte[]{
                // len, val...
                9, 18, 4, 116, 121, 112, 101, 26, 1, 49
        });
    }

    @Test
    void serializeMultiple() {
        LenValSerializer serializer = new LenValSerializer(Integer.MAX_VALUE, Integer.MAX_VALUE, true);
        List<MessageLite> protos = Arrays.asList(
                makeProto("1"),
                makeProto("12"),
                makeProto("123")
        );
        assertThat(serializer.serialize(protos)).containsExactly(new byte[]{
                9, 18, 4, 116, 121, 112, 101, 26, 1, 49,
                10, 18, 4, 116, 121, 112, 101, 26, 2, 49, 50,
                11, 18, 4, 116, 121, 112, 101, 26, 3, 49, 50, 51
        });
    }

    @Test
    void serializeMultipleChunkItems1() {
        LenValSerializer serializer = new LenValSerializer(1, Integer.MAX_VALUE, true);
        List<MessageLite> protos = Arrays.asList(
                makeProto("1"),
                makeProto("12"),
                makeProto("123")
        );
        assertThat(serializer.serialize(protos)).containsExactly(new byte[]{
                9, 18, 4, 116, 121, 112, 101, 26, 1, 49,
        }, new byte[]{
                10, 18, 4, 116, 121, 112, 101, 26, 2, 49, 50,
        }, new byte[]{
                11, 18, 4, 116, 121, 112, 101, 26, 3, 49, 50, 51
        });
    }

    @Test
    void serializeMultipleChunkItems2() {
        LenValSerializer serializer = new LenValSerializer(2, Integer.MAX_VALUE, true);
        List<MessageLite> protos = Arrays.asList(
                makeProto("1"),
                makeProto("12"),
                makeProto("123")
        );
        assertThat(serializer.serialize(protos)).containsExactly(new byte[]{
                9, 18, 4, 116, 121, 112, 101, 26, 1, 49,
                10, 18, 4, 116, 121, 112, 101, 26, 2, 49, 50,
        }, new byte[]{
                11, 18, 4, 116, 121, 112, 101, 26, 3, 49, 50, 51
        });
    }

    @Test
    void serializeMultipleChunkBytes() {
        LenValSerializer serializer = new LenValSerializer(Integer.MAX_VALUE, 22, true);
        List<MessageLite> protos = Arrays.asList(
                makeProto("1"),
                makeProto("12"),
                makeProto("1234567890")
        );
        assertThat(serializer.serialize(protos)).containsExactly(new byte[]{
                9, 18, 4, 116, 121, 112, 101, 26, 1, 49, // 10 bytes
                10, 18, 4, 116, 121, 112, 101, 26, 2, 49, 50, // 11 bytes
        }, new byte[]{
                18, 18, 4, 116, 121, 112, 101, 26, 10, 49, 50, 51, 52, 53, 54, 55, 56, 57, 48
        });
    }

    private static TestProtoClass.DynamicLogEntry makeProto(String id) {
        return TestProtoClass.DynamicLogEntry.newBuilder()
                .setEntityType("type")
                .setEntityId(id)
                .setGenerationId(0)
                .build();
    }

    @Test
    void transformToBatch() {
        LenValSerializer serializer = new LenValSerializer(2, Integer.MAX_VALUE, true);
        List<TestProtoClass.DynamicLogEntry> protos = Arrays.asList(
                makeProto("1"),
                makeProto("12"),
                makeProto("123")
        );
        List<TestEventBatch> collect = serializer.makeBatch(
                protos.stream().map(TestEvent::new),
                TestEventBatch::new
        ).collect(Collectors.toList());
        assertThat(collect.stream().map(LenValSerializer.EventBatch::getBytes)).containsExactly(new byte[]{
                9, 18, 4, 116, 121, 112, 101, 26, 1, 49,
                10, 18, 4, 116, 121, 112, 101, 26, 2, 49, 50,
        }, new byte[]{
                11, 18, 4, 116, 121, 112, 101, 26, 3, 49, 50, 51,
        });
    }

    static class TestEvent extends BaseProtobufLogbrokerEvent<TestProtoClass.DynamicLogEntry> {
        TestEvent(TestProtoClass.DynamicLogEntry payload) {
            super(payload);
        }
    }

    static class TestEventBatch extends LenValSerializer.EventBatch<TestEvent> {
        TestEventBatch(byte[] batchBytes) {
            super(batchBytes);
        }
    }
}
