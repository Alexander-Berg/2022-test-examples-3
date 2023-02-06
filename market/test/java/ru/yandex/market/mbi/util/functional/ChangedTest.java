package ru.yandex.market.mbi.util.functional;

import java.util.Optional;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@ParametersAreNonnullByDefault
class ChangedTest {

    @Test
    void testAsIntDelta() {
        assertThat(Changed.fromTo(4, 15).asIntDelta(value -> value)).isEqualTo(11);
        assertThat(Changed.fromTo(15, 3).asIntDelta(value -> value)).isEqualTo(-12);
        assertThat(Changed.fromTo(5, 5).asIntDelta(value -> value)).isEqualTo(0);
    }

    @Test
    void testAsIncrement() {
        var changed = Changed.fromTo(5, 15);
        assertThat(changed.map(i1 -> i1 > 10).asIntDelta(value -> value
                ? 1
                : 0)).isEqualTo(1);

        changed = Changed.fromTo(15, 5);
        assertThat(changed.map(i1 -> i1 > 10).asIntDelta(value -> value
                ? 1
                : 0)).isEqualTo(-1);

        changed = Changed.fromTo(15, 20);
        assertThat(changed.map(i -> i > 10).asIntDelta(value -> value
                ? 1
                : 0)).isEqualTo(0);

        changed = Changed.fromTo(3, 7);
        assertThat(changed.map(i -> i > 10).asIntDelta(value -> value
                ? 1
                : 0)).isEqualTo(0);
    }

    @Test
    void testDidChange() {
        var changed = Changed.fromTo(5, 15);
        assertThat(changed.didChange()).isTrue();

        changed = Changed.fromTo(15, 15);
        assertThat(changed.didChange()).isFalse();
    }

    @Test
    void testFromToWhenBothPresent() {
        assertThat(Changed.fromToWhenBothPresent(Changed.fromTo(Optional.empty(), Optional.empty()))).isEmpty();
        assertThat(Changed.fromToWhenBothPresent(Changed.fromTo(Optional.of(5), Optional.empty()))).isEmpty();
        assertThat(Changed.fromToWhenBothPresent(Changed.fromTo(Optional.empty(), Optional.of(15)))).isEmpty();

        var extractedChanged = Changed.fromToWhenBothPresent(Changed.fromTo(Optional.of(3), Optional.of(13)));
        assertThat(extractedChanged).isPresent();
        var changed = extractedChanged.orElseThrow(IllegalStateException::new);
        assertThat(changed.previousValue().intValue()).isEqualTo(3);
        assertThat(changed.value().intValue()).isEqualTo(13);
    }
}
