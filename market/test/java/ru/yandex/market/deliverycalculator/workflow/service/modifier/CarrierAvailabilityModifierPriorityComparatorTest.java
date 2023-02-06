package ru.yandex.market.deliverycalculator.workflow.service.modifier;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.deliverycalculator.storage.model.modifier.DeliveryModifiersMeta.CarrierAvailabilityModifierMeta;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CarrierAvailabilityModifierPriorityComparatorTest {

    private final CarrierAvailabilityModifierPriorityComparator tested = new CarrierAvailabilityModifierPriorityComparator();

    /**
     * Тест для {@link CarrierAvailabilityModifierPriorityComparator#compare(CarrierAvailabilityModifierMeta, CarrierAvailabilityModifierMeta)}.
     *
     * @param modifier1      - первый сравниваемый модификатор
     * @param modifier2      - второй сравниваемый модификатор
     * @param expectedResult - ожидаемый результат сравнения.
     */
    @ParameterizedTest
    @MethodSource("argumentsForComparison")
    void testComparison(CarrierAvailabilityModifierMeta modifier1,
                        CarrierAvailabilityModifierMeta modifier2,
                        int expectedResult) {
        assertEquals(expectedResult, tested.compare(modifier1, modifier2));
    }

    static Stream<Arguments> argumentsForComparison() {
        return Stream.of(
                // Первый модификатор приоритетнее второго
                Arguments.of(new CarrierAvailabilityModifierMeta.Builder()
                                .withId(1L)
                                .withPriority(2)
                                .withIsCarrierAvailable(false)
                                .build(),
                        new CarrierAvailabilityModifierMeta.Builder()
                                .withId(1L)
                                .withPriority(3)
                                .withIsCarrierAvailable(true)
                                .build(),
                        -1),
                // Первый модификатор менее приоритетный чем второй
                Arguments.of(new CarrierAvailabilityModifierMeta.Builder()
                                .withId(1L)
                                .withPriority(4)
                                .withIsCarrierAvailable(false)
                                .build(),
                        new CarrierAvailabilityModifierMeta.Builder()
                                .withId(1L)
                                .withPriority(3)
                                .withIsCarrierAvailable(false)
                                .build(),
                        1),
                // Первый модификатор имеет тот же приоритет, что и второй, но является модиффикатором включения
                Arguments.of(new CarrierAvailabilityModifierMeta.Builder()
                                .withId(1L)
                                .withPriority(3)
                                .withIsCarrierAvailable(true)
                                .build(),
                        new CarrierAvailabilityModifierMeta.Builder()
                                .withId(1L)
                                .withPriority(3)
                                .withIsCarrierAvailable(false)
                                .build(),
                        -1),
                // Первый модификатор имеет тот же приоритет, что и второй, но является модиффикатором выключения, а
                // второй же модификатор - модификатор включения
                Arguments.of(new CarrierAvailabilityModifierMeta.Builder()
                                .withId(1L)
                                .withPriority(3)
                                .withIsCarrierAvailable(false)
                                .build(),
                        new CarrierAvailabilityModifierMeta.Builder()
                                .withId(1L)
                                .withPriority(3)
                                .withIsCarrierAvailable(true)
                                .build(),
                        1),
                // Первый модификатор по итоговому приоритету аналогичен второму модификатору.
                Arguments.of(new CarrierAvailabilityModifierMeta.Builder()
                                .withId(1L)
                                .withPriority(3)
                                .withIsCarrierAvailable(true)
                                .build(),
                        new CarrierAvailabilityModifierMeta.Builder()
                                .withId(1L)
                                .withPriority(3)
                                .withIsCarrierAvailable(true)
                                .build(),
                        0)
        );
    }
}
