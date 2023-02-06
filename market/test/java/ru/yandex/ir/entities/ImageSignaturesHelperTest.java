package ru.yandex.ir.entities;

import junit.framework.Assert;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

public class ImageSignaturesHelperTest {

    @Test
    public void parseSignature() {
        float[] parseSignature = ImageSignaturesHelper.parseSignature("0.05 0.06 0.9");

        assert parseSignature != null;
        assert parseSignature.length == 3;
        Assert.assertEquals(parseSignature[0], 0.05, 0.001);
        Assert.assertEquals(parseSignature[1], 0.06, 0.001);
        Assert.assertEquals(parseSignature[2], 0.9, 0.001);
    }

    @Test
    public void parseSignatureNull() {
        float[] parseSignature = ImageSignaturesHelper.parseSignature(null);

        assert parseSignature == null;
    }

    @Test
    public void parseSignatureEmpty() {
        float[] parseSignature = ImageSignaturesHelper.parseSignature("");

        assert parseSignature == null;
    }

    @Test
    public void parseSignatures() {
        List<float[]> parseSignatures = ImageSignaturesHelper.parseSignatures(Collections.singletonList("0.05 0.06 0.9"));

        assert parseSignatures != null;
        assert parseSignatures.size() == 1;
        assert parseSignatures.get(0) != null;
        assert parseSignatures.get(0).length == 3;
        Assert.assertEquals(parseSignatures.get(0)[0], 0.05, 0.001);
        Assert.assertEquals(parseSignatures.get(0)[1], 0.06, 0.001);
        Assert.assertEquals(parseSignatures.get(0)[2], 0.9, 0.001);
    }

    @Test
    public void parseSignaturesNull() {
        List<float[]> parseSignatures = ImageSignaturesHelper.parseSignatures(null);

        assert parseSignatures != null;
        assert parseSignatures.size() == 0;
    }

    @Test
    public void parseSignaturesEmpty() {
        List<float[]> parseSignatures = ImageSignaturesHelper.parseSignatures(Collections.emptyList());

        assert parseSignatures != null;
        assert parseSignatures.size() == 0;
    }
}
