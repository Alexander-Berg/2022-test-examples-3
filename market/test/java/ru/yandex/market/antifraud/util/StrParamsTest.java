package ru.yandex.market.antifraud.util;

import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class StrParamsTest {
    @Test
    public void mustReplace() {
        assertThat(StrParams.replace("${p1}-${p2}", "p1", "v1", "p2", "v2"),
                is("v1-v2"));
    }

    @Test(expected = RuntimeException.class)
    public void mustFailTooFewParams() {
        StrParams.replace("${p1}-${p2}", "p1", "v1");
    }

    @Test(expected = RuntimeException.class)
    public void mustFailTooManyParams() {
        StrParams.replace("${p1}-${p2}",
                "p1", "v1", "p2", "v2", "p3", "v3");
    }
}
