package ru.yandex.market.yt.util.utils;

import java.io.IOException;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

import com.google.protobuf.DescriptorProtos;
import com.google.protobuf.Descriptors;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.inside.yt.kosher.ytree.YTreeNode;

/**
 * @author amaslak
 */
public class YqlUtilsTest {

    private static final int BUFFER_SIZE = 128;

    private static final Descriptors.Descriptor TEST_DESCRIPTOR = DescriptorProtos.EnumOptions.getDescriptor();

    @Test
    public void testYqlProtoField() throws DataFormatException, IOException {
        YTreeNode yqlProtoFieldData = YqlUtils.getYqlProtoFieldData(TEST_DESCRIPTOR);

        JSONObject jsonObject = new JSONObject(yqlProtoFieldData.stringValue());

        Assert.assertEquals("protobin", jsonObject.getString("format"));
        Assert.assertEquals(0, jsonObject.getInt("skip"));
        Assert.assertEquals(TEST_DESCRIPTOR.getName(), jsonObject.getString("name"));
        Assert.assertEquals(false, jsonObject.has("view"));
        String base64AndGzippedProtoDescription = jsonObject.getString("meta");

        byte[] gzippedProtoDescription = Base64.getDecoder().decode(base64AndGzippedProtoDescription);
        byte[] protoDescription = zlibDecompress(gzippedProtoDescription);
        DescriptorProtos.FileDescriptorSet parsedProto = DescriptorProtos.FileDescriptorSet.parseFrom(protoDescription);

        List<DescriptorProtos.DescriptorProto> allProtos = parsedProto.getFileList()
            .stream()
            .map(DescriptorProtos.FileDescriptorProto::getMessageTypeList)
            .flatMap(List::stream)
            .collect(Collectors.toList());

        boolean containsTestDescriptor = allProtos.contains(TEST_DESCRIPTOR.toProto());
        Assert.assertTrue(containsTestDescriptor);
    }

    @Test
    public void testRecursionMode() throws DataFormatException, IOException {
        YTreeNode yqlProtoFieldData = YqlUtils.getYqlProtoFieldData(TEST_DESCRIPTOR, YqlUtils.RecursionMode.IGNORE);

        JSONObject jsonObject = new JSONObject(yqlProtoFieldData.stringValue());
        Assert.assertEquals(true, jsonObject.has("view"));

        JSONObject viewObject = jsonObject.getJSONObject("view");
        Assert.assertEquals(true, viewObject.has("recursion"));
        Assert.assertEquals(YqlUtils.RecursionMode.IGNORE.toString(), viewObject.getString("recursion"));
    }

    private static byte[] zlibDecompress(byte[] gzippedProtoDescription) throws DataFormatException {
        byte[] decompressBuffer = new byte[BUFFER_SIZE];
        Inflater inflater = new Inflater();
        inflater.setInput(gzippedProtoDescription);

        byte[] result = new byte[0];
        while (!inflater.finished()) {
            int resultLength = inflater.inflate(decompressBuffer);
            int length = result.length;
            result = Arrays.copyOf(result, length + resultLength);
            System.arraycopy(decompressBuffer, 0, result, length, resultLength);
        }
        inflater.end();
        return result;
    }
}
