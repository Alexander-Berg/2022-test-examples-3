package ru.yandex.market.ir.smartmatcher.jniDssmJava;

import org.junit.Assert;
import org.junit.Test;
import ru.yandex.devtools.test.Paths;

import java.io.IOException;
import java.nio.file.Files;

public class JniDssmTest {
    private static final String[] HEADERS = {"query", "title"};
    private static final String[] OUTPUT_VARIABLES = {"joint_output"};
    private static final String DUMMY_TITLE = "title";
    private static final int WAIT_COUNT = 1;
    private static final float WAIT_VALUE = 2.799f;

    @Test
    public void test() {
        //Load native library
        System.loadLibrary("jni-dssm-native");
        //Read model
        Model model = new Model(Paths.getSandboxResourcesRoot() + "/model.dssm");
        //Calculate features
        float[] result = model.apply(HEADERS, new String[]{"Cookies", DUMMY_TITLE}, OUTPUT_VARIABLES);
        Assert.assertEquals(WAIT_COUNT, result.length);
        Assert.assertEquals(WAIT_VALUE, result[0], 0.001f);
    }

/*
    @Test
    public void testFromBytes() throws IOException {
        //Load native library
        System.loadLibrary("jni-dssm-native");
        //Read model
        String filePath = "/Users/alvorontsov/Downloads/sm_hard2_doc.dssm";
        String[] HARD2_HEADERS = { "host", "path", "title", "query" };
        String[] HARD2_OUT = {"doc_embedding"};
        java.nio.file.Path path = java.nio.file.Paths.get(filePath);
        byte[] bytes = Files.readAllBytes(path);
        Model model = new Model(bytes, bytes.length);
        //Calculate features
        float[] result = model.apply(HARD2_HEADERS, new String[]{"https://market.yandex.ru",
                "product/0/",
                 DUMMY_TITLE,
                ""}, HARD2_OUT);
        Assert.assertEquals(50, result.length);
    }
*/
}
