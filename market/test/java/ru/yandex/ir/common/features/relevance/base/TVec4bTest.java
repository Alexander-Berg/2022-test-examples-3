package ru.yandex.ir.common.features.relevance.base;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TVec4bTest {

    @Test
    public void asInt() {
        TVec4b boolVector = new TVec4b(true, false, false, true);
        TVec4i intVector = boolVector.AsInt();

        Assertions.assertArrayEquals(new int[]{1, 0, 0, 1}, intVector.val);
    }
}
