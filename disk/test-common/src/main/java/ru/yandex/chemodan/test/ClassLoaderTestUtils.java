package ru.yandex.chemodan.test;

import ru.yandex.misc.io.InputStreamSource;
import ru.yandex.misc.io.InputStreamSourceUtils2;
import ru.yandex.misc.io.cl.ClassLoaderUtils;

/**
 * @author akirakozov
 */
public class ClassLoaderTestUtils {

    public static InputStreamSource byteStreamSourceForResource(Class<?> cl, String fileName) {
        byte[] data = ClassLoaderUtils.streamSourceForResource(cl, fileName).readBytes();
        return InputStreamSourceUtils2.bytes(data);
    }
}
