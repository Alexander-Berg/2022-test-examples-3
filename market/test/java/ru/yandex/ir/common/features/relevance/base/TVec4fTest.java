package ru.yandex.ir.common.features.relevance.base;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class TVec4fTest {

    @Test
    void add() {
        TVec4f a = new TVec4f(0.1f, 0.2f, 0.3f, 0.4f);
        TVec4f b = new TVec4f(20, 30, 40, 50);

        TVec4f sum = TVec4f.Add(a, b);
        Assertions.assertArrayEquals(new float[]{20.1f, 30.2f, 40.3f, 50.4f}, sum.val);
    }

    @Test
    void transpose() {
        TVec4f a = new TVec4f(1, 2, 3, 4);
        TVec4f b = new TVec4f(5, 6, 7, 8);
        TVec4f c = new TVec4f(9, 10, 11, 12);
        TVec4f d = new TVec4f(13, 14, 15, 16);

        TVec4f.Transpose(a, b, c, d);

        Assertions.assertArrayEquals(new float[]{1, 5, 9, 13}, a.val);
        Assertions.assertArrayEquals(new float[]{2, 6, 10, 14}, b.val);
        Assertions.assertArrayEquals(new float[]{3, 7, 11, 15}, c.val);
        Assertions.assertArrayEquals(new float[]{4, 8, 12, 16}, d.val);
    }

    @Test
    void addInplace() {
        TVec4f a = new TVec4f(0.1f, 0.2f, 0.3f, 0.4f);
        TVec4f b = new TVec4f(2, 3, 4, 5);

        a.Add(b);
        Assertions.assertArrayEquals(new float[]{2.1f, 3.2f, 4.3f, 5.4f}, a.val);
    }

    @Test
    void mult() {
        TVec4f a = new TVec4f(0.1f, 0.2f, 0.3f, 0.4f);
        TVec4f b = new TVec4f(20, 30, 40, 50);

        TVec4f res = TVec4f.Mult(a, b);
        Assertions.assertArrayEquals(new float[]{2, 6, 12, 20}, res.val);
    }

    @Test
    void multInplace() {
        TVec4f a = new TVec4f(0.1f, 0.2f, 0.3f, 0.4f);
        TVec4f b = new TVec4f(20, 30, 40, 50);

        a.Mult(b);
        Assertions.assertArrayEquals(new float[]{2, 6, 12, 20}, a.val);
    }

    @Test
    void div() {
        TVec4f a = new TVec4f(1, 2, 3, 4);
        TVec4f b = new TVec4f(10, 4, 3, 16);

        TVec4f res = TVec4f.Div(a, b);
        Assertions.assertArrayEquals(new float[]{0.1f, 0.5f, 1.0f, 0.25f}, res.val);
    }

    @Test
    void divInplace() {
        TVec4f a = new TVec4f(1, 2, 3, 4);
        TVec4f b = new TVec4f(10, 4, 3, 16);

        a.Div(b);
        Assertions.assertArrayEquals(new float[]{0.1f, 0.5f, 1.0f, 0.25f}, a.val);
    }

    @Test
    void greaterThan() {
        TVec4f a = new TVec4f(1, 2, 3, 4);
        TVec4f b = new TVec4f(1.1f, 2, 2.9f, 2);

        TVec4b res = a.GreaterThan(b);
        Assertions.assertArrayEquals(new boolean[]{false, false, true, true}, res.val);
    }

    @Test
    void store() {
        TVec4f a = new TVec4f(1, 2, 0.3f, 0.4f);
        float[] b = new float[4];
        a.Store(b);

        Assertions.assertArrayEquals(new float[]{1, 2, 0.3f, 0.4f}, b);
    }
}
