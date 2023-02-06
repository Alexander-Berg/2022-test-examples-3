package ru.yandex.market.wms.autostart.util.dispenser;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

class SubSequencesDispenserTest {

    @Test
    void apply() {
        // 1st allocation of 2 to 4 items:   X  X       X     X
        // 2nd allocation of 2 to 4 items:                 X     X
        List<Integer> list = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8);
        List<Integer> counts = Arrays.asList(1, 1, 100, 1, 3, 1, 1, 1);
        SubSequencesDispenser<Integer> sut = new SubSequencesDispenser<>(
                list,
                () -> new SingleAccumulatedLimitChecker<>(value -> counts.get(value - 1), 4)
        );

        assertThat(sut.isEmpty(), is(equalTo(false)));
        assertThat(sut.remainingCount(), is(equalTo(8)));

        Dispenser.Plan<Integer> plan1 = sut.plan(2, 4);
        assertThat(plan1, not(equalTo(null)));
        assertThat(plan1.isMaximal(), is(equalTo(true)));
        assertThat(plan1.get(), is(equalTo(Arrays.asList(1, 2, 4, 6))));
        assertThat(sut.isEmpty(), is(equalTo(false)));
        assertThat(sut.remainingCount(), is(equalTo(4)));

        Dispenser.Plan<Integer> plan2 = sut.plan(2, 4);
        assertThat(plan2, not(equalTo(null)));
        assertThat(plan2.isMaximal(), is(equalTo(true)));
        assertThat(plan2.get(), is(equalTo(Arrays.asList(5, 7))));
        assertThat(sut.isEmpty(), is(equalTo(false)));
        assertThat(sut.remainingCount(), is(equalTo(2)));

        Dispenser.Plan<Integer> plan3 = sut.plan(2, 4);
        assertThat(plan3, equalTo(null));
        assertThat(sut.isEmpty(), is(equalTo(false)));
        assertThat(sut.remainingCount(), is(equalTo(2)));
    }
}
