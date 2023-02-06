package ru.yandex.market.wms.autostart.autostartlogic.nonsort;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.market.wms.common.spring.enums.ConsolidationLocationType;
import ru.yandex.market.wms.common.spring.enums.WaveType;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;


public class RoundRobinProviderTest {

    @Test
    public void getByTypeTest() {
        Map<ConsolidationLocationType, List<String>> locs = new HashMap<>();
        locs.put(ConsolidationLocationType.OVERSIZE, Lists.newArrayList("A", "B"));
        locs.put(ConsolidationLocationType.WITHDRAWAL, Lists.newArrayList("W"));
        locs.put(ConsolidationLocationType.SINGLES, Lists.newArrayList("A", "C", "D"));

        RoundRobinProvider<ConsolidationLocationType, String> locsSupplier = new RoundRobinProvider<>(locs);

        assertThat(locsSupplier.get(ConsolidationLocationType.OVERSIZE), is(equalTo("A")));
        assertThat(locsSupplier.get(ConsolidationLocationType.OVERSIZE), is(equalTo("B")));
        assertThat(locsSupplier.get(ConsolidationLocationType.OVERSIZE), is(equalTo("A")));
        assertThat(locsSupplier.get(ConsolidationLocationType.OVERSIZE), is(equalTo("B")));
        assertThat(locsSupplier.get(ConsolidationLocationType.OVERSIZE), is(equalTo("A")));

        assertThat(locsSupplier.get(ConsolidationLocationType.SINGLES), is(equalTo("A")));
        assertThat(locsSupplier.get(ConsolidationLocationType.SINGLES), is(equalTo("C")));
        assertThat(locsSupplier.get(ConsolidationLocationType.SINGLES), is(equalTo("D")));
        assertThat(locsSupplier.get(ConsolidationLocationType.SINGLES), is(equalTo("A")));
        assertThat(locsSupplier.get(ConsolidationLocationType.SINGLES), is(equalTo("C")));
        assertThat(locsSupplier.get(ConsolidationLocationType.SINGLES), is(equalTo("D")));
        assertThat(locsSupplier.get(ConsolidationLocationType.SINGLES), is(equalTo("A")));

        assertThat(locsSupplier.get(ConsolidationLocationType.WITHDRAWAL), is(equalTo("W")));
        assertThat(locsSupplier.get(ConsolidationLocationType.WITHDRAWAL), is(equalTo("W")));
        assertThat(locsSupplier.get(ConsolidationLocationType.WITHDRAWAL), is(equalTo("W")));
    }

    @Test
    public void emptyNullTest() {
        Map<ConsolidationLocationType, List<String>> locs = new HashMap<>();
        locs.put(ConsolidationLocationType.OVERSIZE, Collections.emptyList());
        RoundRobinProvider<ConsolidationLocationType, String> locsSupplier = new RoundRobinProvider<>(locs);

        Arrays.stream(ConsolidationLocationType.values())
                .forEach(type -> assertThat(locsSupplier.get(type, ""), is(equalTo(""))));
    }


    @Test
    public void aosWaveTypeSequenceTest() {
        AosWaveTypeStartSequenceProvider provider = new AosWaveTypeStartSequenceProvider();
        Map<WaveType, Integer> map = new HashMap<>();
        for (int i = 0; i < 100; i++) {
            WaveType next = provider.getNext(null).getWaveType();
            map.putIfAbsent(next, 1);
            map.computeIfPresent(next, (k, v) -> v + 1);
        }

        int size = map.size();
        int total = map.values().stream().mapToInt(a -> a).sum();
        int distribution = total / size;
        map.values().forEach(v -> Assertions.assertThat(Math.abs(distribution - v)).isLessThan(2));
    }
}
