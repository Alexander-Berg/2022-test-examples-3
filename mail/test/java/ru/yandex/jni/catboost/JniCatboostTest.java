package ru.yandex.jni.catboost;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.test.util.TestBase;

public class JniCatboostTest extends TestBase {
    @Test
    public void test() throws Exception {
        final JniCatboostModel model = new JniCatboostModel(
                resource("tcp_model.bin").toAbsolutePath().toFile(),
                resource("cd.txt").toAbsolutePath().toFile()
        );

        {
            final JniCatboostFeatures features = new JniCatboostFeatures();
            final double[] result = model.calc(features);
            Assert.assertArrayEquals(new double[]{7.005121354968855}, result, 1e-5);
        }

        {
            final JniCatboostFeatures features = new JniCatboostFeatures();
            features.setNumericFeature("syn_quirk_nz_id", 1);

            final double[] result = model.calc(features);
            Assert.assertArrayEquals(new double[]{6.959528236544994}, result, 1e-5);
        }
    }
}

