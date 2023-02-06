package ru.yandex.market.mbo.autotests.util;

import org.junit.Test;

import java.util.function.Supplier;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author ayratgdl
 * @since 08.11.18
 */
public class WaitUtilTest {
    private static final long TIMEOUT_1000 = 1000;
    private static final long TIMEOUT_LESS_0 = -1;

    @Test
    public void ifTimeoutEquals0ThenPredicateIsTestedAtLeastOneTimes() {
        assertTrue(WaitUtil.waitTrue(() -> true, 0));
        assertFalse(WaitUtil.waitTrue(() -> false, 0));
    }

    @Test
    public void ifPredicateReturnsTrueDuringTimeoutThenReturnTrue() {
        Supplier<Boolean> predicate = new BooleanSequence(false, true);
        assertTrue(WaitUtil.waitTrue(predicate, TIMEOUT_1000));
    }

    @Test
    public void ifPredicateAlwaysReturnsFalseThenReturnFalse() {
        assertFalse(WaitUtil.waitTrue(() -> false, TIMEOUT_1000));
    }

    @Test
    public void ifTimeoutLess0ThenPredicateIsTestedUntilTrue() {
        Supplier<Boolean> predicate = new BooleanSequence(false, false, true);
        assertTrue(WaitUtil.waitTrue(predicate, TIMEOUT_LESS_0));
    }

    private static class BooleanSequence implements Supplier<Boolean> {
        private boolean[] sequence;
        int next = 0;

        BooleanSequence(boolean... sequence) {
            this.sequence = sequence;
        }

        @Override
        public Boolean get() {
            return sequence[next++];
        }
    }
}
