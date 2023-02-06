package ru.yandex.market.logistics.cs.converter;

import java.util.Arrays;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.logistics.cs.AbstractTest;
import ru.yandex.market.logistics.cs.domain.enumeration.CounterOverflowReason;
import ru.yandex.market.logistics.cs.domain.enumeration.DayOffType;
import ru.yandex.market.logistics.cs.domain.enumeration.EventType;
import ru.yandex.market.logistics.cs.domain.enumeration.UnitType;
import ru.yandex.market.logistics.cs.domain.enums.AdminDayOffType;
import ru.yandex.market.logistics.cs.domain.enums.AdminUnitType;
import ru.yandex.market.logistics.cs.domain.enums.InternalEventType;

import static org.junit.jupiter.params.provider.EnumSource.Mode.EXCLUDE;

@DisplayName("Тестирование конвертера перечислений")
public class EnumConverterTest extends AbstractTest {

    private final EnumConverter enumConverter = new EnumConverter();

    @DisplayName("Тестирование конвертирования DayOffType")
    @ParameterizedTest
    @EnumSource(value = DayOffType.class, mode = EXCLUDE)
    void convertDayOffType(DayOffType value) {
        softly.assertThat(enumConverter.convert(value, AdminDayOffType.class))
            .isNotNull();
    }

    @DisplayName("Тестирование конвертирования UnitType")
    @ParameterizedTest
    @EnumSource(value = UnitType.class, mode = EXCLUDE)
    void convertUnitType(UnitType value) {
        softly.assertThat(enumConverter.convert(value, AdminUnitType.class))
            .isNotNull();
    }

    @DisplayName("Тестирование конвертирования EventType")
    @ParameterizedTest
    @EnumSource(value = EventType.class, mode = EXCLUDE)
    void convertUnitType(EventType value) {
        softly.assertThat(enumConverter.convert(value, InternalEventType.class))
            .isNotNull();
    }

    @DisplayName("Тестирование получения CounterOverflowReason по EventType")
    @ParameterizedTest
    @MethodSource(value = "eventTypesArguments")
    void convertEventTypeToCounterOverflowReason(EventType value) {
        CounterOverflowReason actualReason = EnumConverter.getEventReason(value);
        softly.assertThat(actualReason).isNotNull();

        if (EventType.NEW == value) {
            softly.assertThat(actualReason).isEqualTo(CounterOverflowReason.NEW_ORDER);
        } else if (EventType.CHANGE_ROUTE == value) {
            softly.assertThat(actualReason).isEqualTo(CounterOverflowReason.ORDER_ROUTE_RECALCULATED);
        } else {
            softly.assertThat(actualReason).isEqualTo(CounterOverflowReason.UNKNOWN);
        }
    }

    private static Stream<Arguments> eventTypesArguments() {
        return Stream.concat(
            Arrays.stream(EventType.values()).map(Arguments::of),
            Arrays.stream(new EventType[] {null}).map(Arguments::of)
        );
    }
}
