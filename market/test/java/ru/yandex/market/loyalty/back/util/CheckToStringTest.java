package ru.yandex.market.loyalty.back.util;

import org.junit.Test;

import java.io.IOException;

import static ru.yandex.market.loyalty.test.ToStringChecker.checkToStringInSameModule;

public class CheckToStringTest {
    @Test
    public void checkToStringInBackModule() throws IOException {
        checkToStringInSameModule();
    }
}
