package ru.yandex.direct.common.log.container.bsexport;

import java.io.IOException;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.protobuf.DescriptorProtos;
import com.google.protobuf.Descriptors;
import com.google.protobuf.DynamicMessage;
import com.google.protobuf.Message;
import org.junit.Test;

import ru.yandex.direct.utils.JsonUtils;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class LogBsExportDataSerializationTest {
    @Test
    public void testProto() throws Descriptors.DescriptorValidationException, IOException {
        LogBsExportEssData<Message> data = getBaseLogData(Message.class)
                .withData(getProtoMessage());

        var gotJsonString = JsonUtils.toJson(data);
        assertThat(gotJsonString.split("\n")).hasSize(1);
        var gotJsonNode = JsonUtils.getObjectMapper().readTree(gotJsonString);

        var dataNode = JsonUtils.getObjectMapper().createObjectNode();
        dataNode.put("name", "name1");
        dataNode.put("number", 13);
        var expectedNode = getBaseNode();
        expectedNode.set("data", dataNode);

        assertThat(gotJsonNode).isEqualTo(expectedNode);
    }

    @Test
    public void testNull() throws IOException {
        LogBsExportEssData<Message> data = getBaseLogData(Message.class)
                .withData(null);

        var gotJsonString = JsonUtils.toJson(data);
        assertThat(gotJsonString.split("\n")).hasSize(1);
        var gotJsonNode = JsonUtils.getObjectMapper().readTree(gotJsonString);

        var dataNode = JsonUtils.getObjectMapper().createObjectNode();
        dataNode.put("name", "name1");
        dataNode.put("number", 13);
        var expectedNode = getBaseNode();
        expectedNode.set("data", null);

        assertThat(gotJsonNode).isEqualTo(expectedNode);
    }

    @Test
    public void testJson() throws IOException {
        LogBsExportEssData<TestJson> data = getBaseLogData(TestJson.class)
                .withData(new TestJson().withName("name1").withNumber(13));

        var gotJsonString = JsonUtils.toJson(data);
        assertThat(gotJsonString.split("\n")).hasSize(1);
        var gotJsonNode = JsonUtils.getObjectMapper().readTree(gotJsonString);

        var dataNode = JsonUtils.getObjectMapper().createObjectNode();
        dataNode.put("name", "name1");
        dataNode.put("number", 13);
        var expectedNode = getBaseNode();
        expectedNode.set("data", dataNode);

        assertThat(gotJsonNode).isEqualTo(expectedNode);
    }

    @Test
    public void testString() throws IOException {
        LogBsExportEssData<String> data = getBaseLogData(String.class)
                .withData("test_data");

        var gotJsonString = JsonUtils.toJson(data);
        assertThat(gotJsonString.split("\n")).hasSize(1);
        var gotJsonNode = JsonUtils.getObjectMapper().readTree(gotJsonString);

        var expectedNode = getBaseNode();
        expectedNode.put("data", "test_data");

        assertThat(gotJsonNode).isEqualTo(expectedNode);
    }

    private Message getProtoMessage() throws Descriptors.DescriptorValidationException {
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
                        .addField(DescriptorProtos.FieldDescriptorProto.newBuilder()
                                .setName("number")
                                .setNumber(2)
                                .setType(DescriptorProtos.FieldDescriptorProto.Type.TYPE_INT32)
                                .build())
                        .build())
                .build();

        Descriptors.Descriptor messageDescriptor = Descriptors.FileDescriptor
                .buildFrom(fileDescriptorProto, new Descriptors.FileDescriptor[0])
                .findMessageTypeByName(messageName);

        return DynamicMessage
                .newBuilder(messageDescriptor)
                .setField(messageDescriptor.findFieldByNumber(1), "name1")
                .setField(messageDescriptor.findFieldByNumber(2), 13)
                .build();
    }

    private static class TestJson {
        @JsonProperty("name")
        private String name;
        @JsonProperty("number")
        private Integer number;

        TestJson withName(String name) {
            this.name = name;
            return this;
        }

        TestJson withNumber(Integer number) {
            this.number = number;
            return this;
        }
    }

    private <T> LogBsExportEssData<T> getBaseLogData(Class<T> clazz) {
        return new LogBsExportEssData<T>()
                .withOrderId(1L)
                .withBsBannerId(3L)
                .withBid(4L)
                .withCid(5L)
                .withPid(12L)
                .withStrategyId(15L);
    }

    private ObjectNode getBaseNode() {
        var baseNode = JsonUtils.getObjectMapper().createObjectNode();
        baseNode.put("bid", 4);
        baseNode.put("pid", 12);
        baseNode.put("cid", 5);
        baseNode.put("bs_banner_id", 3);
        baseNode.put("order_id", 1);
        baseNode.put("strategy_id", 15);
        return baseNode;
    }
}
