package ru.yandex.ir.nirvana.dssm.docEmbeddings;
import junit.framework.Assert;
import ru.yandex.market.ir.smartmatcher.jniDssmJava.Model;

import org.junit.jupiter.api.Test;


public class DssmModelApplierTest {

    /*@Test
    public void testDssmModelApply() {
        System.loadLibrary("jni-dssm-native");
        String modelPath = "/Users/alvorontsov/Downloads/new_query_dssm.bin";
        Model documentModel = new Model(modelPath);
        String[] DOCUMENT_OUTPUT_VARIABLES = {"query_embedding"};
        float[] docEmbedings = documentModel.apply(
                new String[]{"query"},
                new String[]{ "iphone 13 черный"},
                DOCUMENT_OUTPUT_VARIABLES
        );
        Assert.assertEquals(docEmbedings.length, 100);
        boolean zeros = true;
        for (float x: docEmbedings) {
            if (x != 0.0) {
                zeros = false;
            }
        }
        Assert.assertEquals(zeros, false);
    }*/
}
