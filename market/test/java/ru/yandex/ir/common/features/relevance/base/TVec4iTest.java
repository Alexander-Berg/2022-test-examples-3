package ru.yandex.ir.common.features.relevance.base;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class TVec4iTest {

    @Test
    void add() {
        TVec4i a = new TVec4i(1, 2, 3, 4);
        TVec4i b = new TVec4i(20, 30, 40, 50);

        TVec4i sum = TVec4i.Add(a, b);
        Assertions.assertArrayEquals(new int[]{21, 32, 43, 54}, sum.val);
    }

    @Test
    void shiftLeft() {
        TVec4i a = new TVec4i(0b00001, 0b00010, 0b00011, 0b00100);

        TVec4i shifted = TVec4i.ShiftLeft(a, 2);
        Assertions.assertArrayEquals(new int[]{0b00100, 0b01000, 0b01100, 0b10000}, shifted.val);
    }

    @Test
    void shiftLeftInplace() {
        TVec4i a = new TVec4i(0b00001, 0b00010, 0b00011, 0b00100);

        a.ShiftLeft(2);
        Assertions.assertArrayEquals(new int[]{0b00100, 0b01000, 0b01100, 0b10000}, a.val);
    }

    @Test
    void shiftRight() {
        TVec4i a = new TVec4i(0b00101, 0b01110, 0b10011, 0b00100);

        TVec4i shifted = TVec4i.ShiftRight(a, 1);
        Assertions.assertArrayEquals(new int[]{0b00010, 0b00111, 0b01001, 0b00010}, shifted.val);
    }

    @Test
    void bitwizeAnd() {
        TVec4i a = new TVec4i(0b00101, 0b01110, 0b10011, 0b00100);
        TVec4i b = new TVec4i(0b00001, 0b11111, 0b10101, 0b00000);

        TVec4i res = TVec4i.BitwizeAnd(a, b);
        Assertions.assertArrayEquals(new int[]{0b00001, 0b01110, 0b10001, 0b00000}, res.val);
    }

    @Test
    void bitwizeOrInplace() {
        TVec4i a = new TVec4i(0b00101, 0b01110, 0b10011, 0b00100);
        TVec4i b = new TVec4i(0b00001, 0b11111, 0b10101, 0b00000);

        a.BitwizeOr(b);
        Assertions.assertArrayEquals(new int[]{0b00101, 0b11111, 0b10111, 0b00100}, a.val);
    }

    @Test
    void store() {
        TVec4i a = new TVec4i(1, 2, 3, 4);
        int[] b = new int[4];
        a.Store(b);

        Assertions.assertArrayEquals(new int[]{1, 2, 3, 4}, b);
    }

    @Test
    void asFloat() {
        TVec4i intVector = new TVec4i(1, 2, 3, 4);
        TVec4f floatVector = intVector.AsFloat();

        Assertions.assertArrayEquals(new float[]{1.0f, 2.0f, 3.0f, 4.0f}, floatVector.val);
    }

    @Test
    void equal() {
        TVec4i a = new TVec4i(1, 2, 3, 4);
        TVec4i b = new TVec4i(1, 2, 1, 2);

        TVec4b res = a.Equal(b);
        Assertions.assertArrayEquals(new boolean[]{true, true, false, false}, res.val);
    }

    @Test
    void greaterThan() {
        TVec4i a = new TVec4i(1, 2, 3, 4);
        TVec4i b = new TVec4i(2, 2, 2, 2);

        TVec4b res = a.GreaterThan(b);
        Assertions.assertArrayEquals(new boolean[]{false, false, true, true}, res.val);
    }

    @Test
    void cmpMask() {
        TVec4i a = new TVec4i(1, 2, 3, 4);
        TVec4i b = new TVec4i(1, 2, 1, 2);

        TVec4i res = a.CmpMask(b);
        Assertions.assertArrayEquals(new int[]{0xFFFFFFFF, 0xFFFFFFFF, 0, 0}, res.val);
    }
}
