package ru.yandex.market.cocon.util;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import static ru.yandex.market.cocon.util.ExceptionTrimUtil.trimExceptionIfMatch;

public class ExceptionTrimUtilTest {

    @Test
    void testTrimLines() {
        try {
            throw new E1();
        } catch (Exception e) {
            try {
                throw new E2(e);
            } catch (Exception e1) {
                Assertions.assertThat(e1.getStackTrace()).hasSizeGreaterThan(2);
                Assertions.assertThat(e1.getCause().getStackTrace()).hasSizeGreaterThan(2);
                trimExceptionIfMatch(e1, E1.class, 2);
                Assertions.assertThat(e1.getStackTrace()).hasSize(2);
                Assertions.assertThat(e1.getCause().getStackTrace()).hasSize(2);
            }
        }
    }

    @Test
    void testTrimWithCauseLevelsTest() {
        try {
            throw new E1();
        } catch (Exception e) {
            try {
                throw new E2(e);
            } catch (Exception e1) {
                Assertions.assertThat(e1.getStackTrace()).hasSizeGreaterThan(4);
                Assertions.assertThat(e1.getCause().getStackTrace()).hasSizeGreaterThan(4);
                trimExceptionIfMatch(e1, E1.class, 4, 1);
                Assertions.assertThat(e1.getStackTrace()).hasSize(4);
                Assertions.assertThat(e1.getCause().getStackTrace()).hasSize(1);
            }
        }
    }

    static class E1 extends RuntimeException {
    }

    static class E2 extends RuntimeException {
        E2(Throwable cause) {
            super(cause);
        }
    }
}
