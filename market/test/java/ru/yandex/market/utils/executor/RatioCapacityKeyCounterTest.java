package ru.yandex.market.utils.executor;

import java.util.stream.Stream;

import com.google.common.base.Suppliers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class RatioCapacityKeyCounterTest {
    static RatioCapacityKeyCounter make(int maxCapacity, int maxCapacityPerTaskPercent) {
        return new RatioCapacityKeyCounter(
                maxCapacity,
                Suppliers.ofInstance(1),
                Suppliers.ofInstance(maxCapacityPerTaskPercent)
        );
    }

    static Stream<Arguments> ctorChecksRangeData() {
        return Stream.of(
                arguments(0),
                arguments(Integer.MIN_VALUE),
                arguments(Integer.MAX_VALUE)
        );
    }

    @ParameterizedTest
    @MethodSource("ctorChecksRangeData")
    void ctorChecksRange(int maxCapacity) {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> make(maxCapacity, 100));
    }

    static Stream<Arguments> tryAddRemoveSingleData() {
        return Stream.of(
                arguments(10, 0, 1),
                arguments(10, 10, 1),
                arguments(10, 90, 1),
                arguments(10, 100, 1),
                arguments(10, 110, 1)
        );
    }

    @ParameterizedTest
    @MethodSource("tryAddRemoveSingleData")
    void tryAddRemoveSingle(int maxCapacity, int maxCapacityPerTaskPercent, int count) {
        var key = "whatever";
        var counter = make(maxCapacity, maxCapacityPerTaskPercent);
        assertThat(counter.tryAdd(key)).isTrue();
        assertThat(counter.getCurrent(key)).isEqualTo(count);
        counter.remove(key);
        assertThat(counter.getCurrent(key)).isZero();
    }

    @Test
    void tryAddRemoveConsequentAllCapacity() {
        var key = "whatever";
        var maxCapacity = 10;
        var counter = make(maxCapacity, 100);
        tryAddNTimes(counter, key, maxCapacity);
        assertThat(counter.tryAdd(key)).as("task max capacity >= max capacity => no limits").isTrue();
        assertThat(counter.getCurrent(key)).isEqualTo(maxCapacity + 1);
        for (int i = maxCapacity + 1; i > 0; i--) {
            counter.remove(key);
            assertThat(counter.getCurrent(key)).isEqualTo(i - 1);
        }
        assertThat(counter.getCurrent(key)).isZero();
    }

    @Test
    void tryAddRemoveConsequentLessCapacity() {
        var key = "whatever";
        var maxCapacity = 9;
        var counter = make(maxCapacity + 1, 90);
        tryAddNTimes(counter, key, maxCapacity);
        assertThat(counter.tryAdd(key)).isFalse();
        assertThat(counter.getCurrent(key)).isEqualTo(maxCapacity);
        for (int i = maxCapacity; i > 0; i--) {
            counter.remove(key);
            assertThat(counter.getCurrent(key)).isEqualTo(i - 1);
        }
        assertThat(counter.getCurrent(key)).isZero();
    }

    @Test
    void keysAffectsEachOtherRatios() {
        var counter = make(32, 50);
        tryAddNTimes(counter, "1", 16);
        assertThat(counter.tryAdd("1")).isFalse();
        tryAddNTimes(counter, "2", 8);
        assertThat(counter.tryAdd("2"))
                .as("ключ может занять только до 50% оставшейся capacity")
                .isFalse();
        tryAddNTimes(counter, "3", 4);
        assertThat(counter.tryAdd("3"))
                .as("ключ может занять только до 50% оставшейся capacity")
                .isFalse();
        assertThat(counter.remainingCapacity()).isEqualTo(4);

        // теперь допустим первые таски, которые заняли половину capacity частично закончились
        for (int i = 0; i < 4; i++) {
            counter.remove("1");
        }
        assertThat(counter.getCurrent("1")).isEqualTo(12);
        assertThat(counter.remainingCapacity()).isEqualTo(8);

        // освободившуюся capacity теперь может занять любая другая таска в заданных пределах
        // ключ "3" уже занял 4, добавим свободные 8, новый лимит для таски будет (8+4)*50% = 6
        for (int i = 0; i < 2; i++) {
            assertThat(counter.tryAdd("3"))
                    .as("любая другая таска может занять освободившуюся capacity до тех же 50%")
                    .isTrue();
        }
        assertThat(counter.tryAdd("3")).isFalse();
        assertThat(counter.getCurrent("3")).isEqualTo(6);
    }

    private static void tryAddNTimes(RatioCapacityKeyCounter counter, String key, int times) {
        for (int i = 0; i < times; i++) {
            assertThat(counter.tryAdd(key)).isTrue();
            assertThat(counter.getCurrent(key)).isEqualTo(i + 1);
        }
    }

}
