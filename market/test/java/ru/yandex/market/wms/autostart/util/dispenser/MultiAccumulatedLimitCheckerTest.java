package ru.yandex.market.wms.autostart.util.dispenser;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static ru.yandex.market.wms.autostart.autostartlogic.CollectionsUtils.listOf;

class MultiAccumulatedLimitCheckerTest {

    @Test
    void test_accept() {
        MultiAccumulatedLimitChecker<Double> checker = new MultiAccumulatedLimitChecker<>(
                listOf(new SingleAccumulatedLimitChecker<>(x -> x, 20d))
        );

        assertThat(checker.test(10d), is(equalTo(true)));
        checker.accept(10d);

        assertThat(checker.test(20d), is(equalTo(false)));

        assertThat(checker.test(10d), is(equalTo(true)));
    }
}
