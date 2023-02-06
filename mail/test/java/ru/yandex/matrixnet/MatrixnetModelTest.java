package ru.yandex.matrixnet;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.test.util.TestBase;

public class MatrixnetModelTest extends TestBase {
    // CSOFF: MagicNumber
    @Test
    public void testModel() throws Exception {
        MatrixnetModelConfigBuilder builder
            = new MatrixnetModelConfigBuilder();

        builder.content(
            MatrixnetModelTest.class.getResourceAsStream("matrixnet.inc"));

        ImmutableMatrixnetModelConfig config
            = new ImmutableMatrixnetModelConfig(builder);
        BasicMatrixnetModel model = config.buildModel();
        Assert.assertEquals(800, model.iterations());
        Assert.assertEquals(50, model.varsNum());
        Assert.assertEquals(5.775225043e-11, model.scoreMultiplier(), 1e-15);

        for (int i = 0; i < model.varsNum(); i++) {
            if (i <= 45) {
                Assert.assertFalse(
                    "Expecting non empty var on index " + i,
                    model.vars().get(i).empty());
            } else {
                Assert.assertTrue(
                    "Expecting empty var on index " + i,
                    model.vars().get(i).empty());
            }
        }

        Assert.assertEquals(13, model.vars().get(45).factorIndex());
        Assert.assertEquals(0.5, model.vars().get(45).threshold(), 1e-8);

        double[] factors = {
            8.266871341794682, 0.0, 0.0, 1.0, 1.0,
            0.0, 1.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0,
            0.0};

        Assert.assertEquals(0.788334069168, model.score(factors), 1e-8);
    }
    // CSON: MagicNumber
}
