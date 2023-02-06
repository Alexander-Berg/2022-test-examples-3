package ru.yandex.direct.bstransport.yt.utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.protobuf.CodedInputStream;
import com.google.protobuf.DescriptorProtos;
import com.google.protobuf.Descriptors;
import com.google.protobuf.DynamicMessage;
import com.google.protobuf.Message;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.bstransport.yt.utils.ProtoSeqSerializer.MAGIC;

class ProtoSeqSerializerTest {

    private static ProtoSeqSerializer protoSeqSerializer;
    private static Descriptors.Descriptor stringDescriptor;

    @BeforeAll
    static void before() throws Descriptors.DescriptorValidationException {
        protoSeqSerializer = new ProtoSeqSerializer(3, 1);
        stringDescriptor = getStringDescriptor();
    }

    @Test
    void testEmptyList() {
        var result = protoSeqSerializer.serialize(List.of());
        assertThat(result).isEmpty();
    }

    @Test
    void testProtoMessagesList() throws Descriptors.DescriptorValidationException, IOException {
        var stringDescriptor = getStringDescriptor();
        var numberDescriptor = getNumberDescriptor();
        var stringMessage = getStringProtoMessage(stringDescriptor);
        var numberMessage = getNumberProtoMessage(numberDescriptor);
        var result = protoSeqSerializer.serialize(List.of(stringMessage, numberMessage)).get(0);
        CodedInputStream inputStream = CodedInputStream.newInstance(result);
        var size1 = inputStream.readFixed32();
        var stringMessageBytes = inputStream.readRawBytes(size1);
        var stringMessageGot = DynamicMessage.parseFrom(stringDescriptor, stringMessageBytes);
        var magic1 = inputStream.readRawBytes(32);
        var size2 = inputStream.readFixed32();
        var numberMessageBytes = inputStream.readRawBytes(size2);
        var numberMessageGot = DynamicMessage.parseFrom(numberDescriptor, numberMessageBytes);
        var magic2 = inputStream.readRawBytes(32);
        inputStream.isAtEnd();
        assertThat(inputStream.isAtEnd()).isTrue();
        assertThat(stringMessageGot).isEqualTo(stringMessage);
        assertThat(numberMessageGot).isEqualTo(numberMessage);
        assertThat(magic1).isEqualTo(MAGIC);
        assertThat(magic2).isEqualTo(MAGIC);
    }

    private static Stream<Arguments> parameters() {
        return Stream.of(
                Arguments.of(
                        "no messages",
                        generateMessages(List.of())),
                Arguments.of(
                        "splits by number of messages",
                        generateMessages(List.of(
                                List.of(1, 1, 1),
                                List.of(1, 1, 1)))),
                Arguments.of(
                        "splits by size of messages",
                        generateMessages(List.of(
                                List.of(400, 400),
                                List.of(400)))),
                Arguments.of(
                        "big messages are handled correctly",
                        generateMessages(List.of(
                                List.of(1500),
                                List.of(1)))));
    }

    private static List<List<Message>> generateMessages(List<List<Integer>> textLengths) {
        return textLengths
                .stream()
                .map(lengths -> lengths
                        .stream()
                        .map(length -> {
                            var text = "0".repeat(length);
                            return getCustomStringMessage(stringDescriptor, text);
                        })
                        .collect(Collectors.toList()))
                .collect(Collectors.toList());
    }

    @ParameterizedTest
    @MethodSource("parameters")
    void testBatching(
            @SuppressWarnings("unused")
            String description,
            List<List<Message>> originalMessages) throws IOException {
        var flatMessages = originalMessages
                .stream()
                .flatMap(List::stream)
                .collect(Collectors.toList());

        var protoseqs = protoSeqSerializer.serialize(flatMessages);
        assertThat(protoseqs).hasSameSizeAs(originalMessages);

        var softly = new SoftAssertions();

        for (int i = 0; i < protoseqs.size(); ++i) {
            var chunk = originalMessages.get(i);
            var protoseq = protoseqs.get(i);
            var messagesSizeSum = chunk
                    .stream()
                    .mapToInt(Message::getSerializedSize)
                    .sum();
            if (messagesSizeSum <= protoSeqSerializer.getChunkSizeBytes()) {
                softly.assertThat(protoseq.length).isLessThanOrEqualTo(protoSeqSerializer.getChunkSizeBytes());
            }
            var deserializedMessages = deserializeProtoseq(protoseq);
            softly.assertThat(chunk).isEqualTo(deserializedMessages);
        }

        softly.assertAll();
    }

    private static List<Message> deserializeProtoseq(byte[] protoseq) throws IOException {
        CodedInputStream inputStream = CodedInputStream.newInstance(protoseq);
        List<Message> result = new ArrayList<>();
        while (!inputStream.isAtEnd()) {
            var size = inputStream.readFixed32();
            var bytes = inputStream.readRawBytes(size);
            var message = DynamicMessage.parseFrom(stringDescriptor, bytes);
            result.add(message);
            var magic = inputStream.readRawBytes(MAGIC.length);
            assertThat(magic).isEqualTo(ProtoSeqSerializer.MAGIC);
        }
        return result;
    }

    private static Descriptors.Descriptor getStringDescriptor() throws Descriptors.DescriptorValidationException {
        String messageName = "TestMessage";
        DescriptorProtos.FileDescriptorProto fileDescriptorProto = DescriptorProtos.FileDescriptorProto
                .newBuilder()
                .addMessageType(DescriptorProtos.DescriptorProto.newBuilder()
                        .setName(messageName)
                        .addField(DescriptorProtos.FieldDescriptorProto.newBuilder()
                                .setName("name")
                                .setNumber(1)
                                .setType(DescriptorProtos.FieldDescriptorProto.Type.TYPE_STRING)
                                .build())
                        .build())
                .build();

        return Descriptors.FileDescriptor
                .buildFrom(fileDescriptorProto, new Descriptors.FileDescriptor[0])
                .findMessageTypeByName(messageName);
    }

    private static Descriptors.Descriptor getNumberDescriptor() throws Descriptors.DescriptorValidationException {
        String messageName = "TestMessage";
        DescriptorProtos.FileDescriptorProto fileDescriptorProto = DescriptorProtos.FileDescriptorProto
                .newBuilder()
                .addMessageType(DescriptorProtos.DescriptorProto.newBuilder()
                        .setName(messageName)
                        .addField(DescriptorProtos.FieldDescriptorProto.newBuilder()
                                .setName("number")
                                .setNumber(1)
                                .setType(DescriptorProtos.FieldDescriptorProto.Type.TYPE_INT32)
                                .build())
                        .build())
                .build();

        return Descriptors.FileDescriptor
                .buildFrom(fileDescriptorProto, new Descriptors.FileDescriptor[0])
                .findMessageTypeByName(messageName);
    }

    private static Message getCustomStringMessage(Descriptors.Descriptor messageDescriptor, String value) {
        return DynamicMessage
                .newBuilder(messageDescriptor)
                .setField(messageDescriptor.findFieldByNumber(1), value)
                .build();
    }

    private static Message getStringProtoMessage(Descriptors.Descriptor messageDescriptor) {
        return getCustomStringMessage(messageDescriptor, "name1");
    }

    private static Message getNumberProtoMessage(Descriptors.Descriptor messageDescriptor) {
        return DynamicMessage
                .newBuilder(messageDescriptor)
                .setField(messageDescriptor.findFieldByNumber(1), 13)
                .build();
    }
}
