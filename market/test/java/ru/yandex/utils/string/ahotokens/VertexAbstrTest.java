package ru.yandex.utils.string.ahotokens;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Random;

public class VertexAbstrTest {

    @Test
    public void testAddOut() {
        Random random = new Random(1);
        int size = 0;
        Object[] result = null;
        final int a10000 = 10000;
        final int a1000 = 1000;
        for (int i = 0; i < a10000; i++) {
            final int i1 = 1 + random.nextInt(a1000);
            size += i1;
            final Object[] objects = new Object[i1];
            Arrays.setAll(objects, value -> {
                return new Object();
            });

            result = VertexAbstr.addOut(objects, result);

            if (random.nextBoolean()) {
                result = VertexAbstr.addToArr(new Object(), result);
                size++;
            }
        }
        result = VertexAbstr.trim(result);
        boolean findNull = false;
        int resultNotNulCount = 0;
        for (int i = 0; i < result.length; i++) {
            if (result[i] != null) {
                resultNotNulCount++;
                Assert.assertFalse("Not null values find after null value", findNull);
            } else {
                findNull = true;
            }
        }
        Assert.assertEquals(size, resultNotNulCount);
    }
}
