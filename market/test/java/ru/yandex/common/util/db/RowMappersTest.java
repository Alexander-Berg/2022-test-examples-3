package ru.yandex.common.util.db;

import junit.framework.Assert;
import org.junit.Test;

/**
 * Tests for common RowMappers. Please note, that since order of constructors,
 * returned by {@link Class#getConstructors()} is unspecified, there are two
 * different orders for some tests.
 *
 * @author maxkar
 */
public class RowMappersTest {

    public static class TFPC {
        @SuppressWarnings("unused")
        private TFPC(Object o) {}

        public TFPC(Integer val) {
        }

        public TFPC(Long val) {
        }

        @SuppressWarnings("unused")
        private TFPC(long o) {}
    }

    ;

    /**
     * Tests, that mapper finds only public constructor.
     */
    @Test
    public void testFindsPublicConstructor() throws Exception {
        RowMappers.constructor(TFPC.class,
                RowMappers.simply(Integer.valueOf(1))).mapRow(null, 0);
        RowMappers.constructor(TFPC.class, RowMappers.simply(Long.valueOf(1)))
                .mapRow(null, 0);
    }

    public static class TFNPT1 {
        public TFNPT1(int val) {
        }

        public TFNPT1(Integer val) {
        }
    }

    public static class TFNPT2 {
        public TFNPT2(Integer val) {
        }

        public TFNPT2(int val) {
        }
    }

    /**
     * Tests, that mapper can find constructor with reference types where
     * avaliable.
     */
    @Test
    public void testFindsNonPrimitiveType() throws Exception {
        RowMappers.constructor(TFNPT1.class,
                RowMappers.simplyNull(Integer.class)).mapRow(null, 0);
        RowMappers.constructor(TFNPT2.class,
                RowMappers.simply(1)).mapRow(null, 0);
    }

    public static class TSCM {
        public final int mode;

        public TSCM(Integer i) {
            mode = 1;
        }

        public TSCM(Object o) {
            mode = 0;
        }

        public TSCM(String s) {
            mode = 2;
        }
    }

    /**
     * Tests, that mapper can find exactly-mathcing constructor. Simplest
     * variant of "most-specific" interface test.
     */
    @Test
    public void testStringConstructorMatch() throws Exception {
        Assert.assertEquals(1, RowMappers.constructor(TSCM.class,
                RowMappers.simply(Integer.valueOf(1))).mapRow(null, -1).mode);
        Assert.assertEquals(2, RowMappers.constructor(TSCM.class,
                RowMappers.simply("Ye!")).mapRow(null, -1).mode);
    }

    public static class TA {
        public TA(Integer i) {
        }

        public TA(String i) {
        }

        ;
    }
}

