package ru.yandex.market.wms.autostart.util.dispenser;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

class SubListsDispenserTest {

    @Test
    void apply() {
        List<Integer> list = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8);
        SubListsDispenser<Integer> sut = new SubListsDispenser<>(list);

        assertThat(sut.isEmpty(), is(equalTo(false)));
        assertThat(sut.remainingCount(), is(equalTo(8)));

        Dispenser.Plan<Integer> plan1 = sut.plan(3, 3);
        assertThat(plan1, not(equalTo(null)));
        assertThat(plan1.isMaximal(), is(equalTo(true)));
        assertThat(plan1.get(), is(equalTo(Arrays.asList(1, 2, 3))));
        assertThat(sut.isEmpty(), is(equalTo(false)));
        assertThat(sut.remainingCount(), is(equalTo(5)));

        Dispenser.Plan<Integer> plan2 = sut.plan(3, 4);
        assertThat(plan2, not(equalTo(null)));
        assertThat(plan2.isMaximal(), is(equalTo(true)));
        assertThat(plan2.get(), is(equalTo(Arrays.asList(4, 5, 6, 7))));
        assertThat(sut.isEmpty(), is(equalTo(false)));
        assertThat(sut.remainingCount(), is(equalTo(1)));

        Dispenser.Plan<Integer> plan3 = sut.plan(3, 3);
        assertThat(plan3, equalTo(null));
        assertThat(sut.isEmpty(), is(equalTo(false)));
        assertThat(sut.remainingCount(), is(equalTo(1)));

        Dispenser.Plan<Integer> plan4 = sut.plan(3, 3);
        assertThat(plan4, equalTo(null));
        assertThat(sut.isEmpty(), is(equalTo(false)));
        assertThat(sut.remainingCount(), is(equalTo(1)));
    }
}
