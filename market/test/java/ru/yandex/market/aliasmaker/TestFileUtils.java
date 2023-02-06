package ru.yandex.market.aliasmaker;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import com.google.common.io.ByteStreams;
import com.google.protobuf.Message;
import com.googlecode.protobuf.format.JsonFormat;

public class TestFileUtils {
    private TestFileUtils() {
    }

    public static <T extends Message.Builder> T load(String fileName, T builder) {
        try {
            JsonFormat.merge(
                    new InputStreamReader(
                            TestFileUtils.class.getResourceAsStream(fileName)
                    ),
                    builder
            );
            return builder;
        } catch (IOException ex) {
            throw new RuntimeException("Can't read " + fileName, ex);
        }
    }

    public static byte[] loadBytes(String fileName) {
        InputStream stream = TestFileUtils.class.getResourceAsStream(fileName);
        try {
            return ByteStreams.toByteArray(stream);
        } catch (IOException e) {
            throw new RuntimeException("Can't read " + fileName + " to byte array", e);
        }
    }
}
