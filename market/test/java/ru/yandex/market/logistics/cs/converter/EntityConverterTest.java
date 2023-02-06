package ru.yandex.market.logistics.cs.converter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.logistics.cs.AbstractTest;
import ru.yandex.market.logistics.cs.domain.dto.CapacityValueCounterDto;
import ru.yandex.market.logistics.cs.domain.dto.InternalEventDto;
import ru.yandex.market.logistics.cs.domain.entity.CapacityValueCounter;
import ru.yandex.market.logistics.cs.domain.entity.Event;
import ru.yandex.market.logistics.cs.domain.enumeration.DayOffType;
import ru.yandex.market.logistics.cs.domain.enumeration.EventType;
import ru.yandex.market.logistics.cs.domain.enumeration.UnitType;

import static org.junit.jupiter.params.provider.Arguments.arguments;

@DisplayName("Тестирование конвертера перечислений")
public class EntityConverterTest extends AbstractTest {

    private final EnumConverter enumConverter = new EnumConverter();
    private final CapacityValueCounterConverter capacityValueCounterConverter =
        new CapacityValueCounterConverter(enumConverter);
    private final EventConverter eventConverter = new EventConverter(enumConverter, capacityValueCounterConverter);

    @DisplayName("Тестирование конвертирования счетчика капасити")
    @ParameterizedTest(name = "Счетчик с параметрами {0}, {1}")
    @MethodSource(value = "counterArgumentsStream")
    void convertCapacityValueCounter(DayOffType dayOffType, UnitType unitType) {
        CapacityValueCounter counter = createCounter(dayOffType, unitType);
        CapacityValueCounterDto counterDto = capacityValueCounterConverter.toDto(counter);
        checkCapacityValueCounterToDto(counter, counterDto);
    }

    @DisplayName("Тестирование конвертирования события")
    @ParameterizedTest(name = "Событие с типом {0}")
    @MethodSource(value = "eventArgumentsStream")
    void convertEvent(EventType eventType) {
        Event event = createEvent(eventType);
        List<CapacityValueCounter> counters = List.of(
            createCounter(DayOffType.UNSET, UnitType.ORDER),
            createCounter(DayOffType.TECHNICAL, UnitType.ITEM)
        );
        InternalEventDto eventDto = eventConverter.toDto(event, counters);
        checkEventToDto(event, counters, eventDto);
    }

    private void checkCapacityValueCounterToDto(CapacityValueCounter counter, CapacityValueCounterDto dto) {
        softly.assertThat(dto.getId()).isEqualTo(counter.getId());
        softly.assertThat(dto.getCapacityId()).isEqualTo(counter.getCapacityId());
        softly.assertThat(dto.getCapacityValueId()).isEqualTo(counter.getCapacityValueId());
        softly.assertThat(dto.getAdminUnitType().name()).isEqualTo(counter.getUnitType().name());
        softly.assertThat(dto.getDay()).isEqualTo(counter.getDay());
        softly.assertThat(dto.getCount()).isEqualTo(counter.getCount());
        softly.assertThat(dto.getThreshold()).isEqualTo(counter.getThreshold());
        softly.assertThat(dto.isDayOff()).isEqualTo(counter.isDayOff());
        softly.assertThat(dto.getAdminDayOffType().name()).isEqualTo(counter.getDayOffType().name());
        softly.assertThat(dto.getPropagatedFrom()).isEqualTo(counter.getPropagatedFrom());
    }

    private void checkEventToDto(Event event, List<CapacityValueCounter> counters, InternalEventDto dto) {
        softly.assertThat(dto.getId()).isEqualTo(event.getId());
        softly.assertThat(dto.getKey()).isEqualTo(event.getKey());
        softly.assertThat(dto.getType().name()).isEqualTo(event.getType().name());
        softly.assertThat(dto.getRoute()).isEqualTo(event.getRoute());
        softly.assertThat(dto.getEventTimestamp()).isEqualTo(event.getEventTimestamp());
        softly.assertThat(dto.getMaxServiceTime()).isEqualTo(event.getMaxServiceTime());
        softly.assertThat(dto.isProcessed()).isEqualTo(event.isProcessed());
        softly.assertThat(dto.getExternalId()).isEqualTo(event.getExternalId());
        softly.assertThat(dto.isDummy()).isEqualTo(event.isDummy());
        softly.assertThat(dto.getCounters().size()).isEqualTo(2);
        for (int i = 0; i < dto.getCounters().size(); ++i) {
            CapacityValueCounterDto counterDto = dto.getCounters().get(i);
            CapacityValueCounter counter = counters.get(i);
            softly.assertThat(counterDto.getAdminDayOffType().name()).isEqualTo(counter.getDayOffType().name());
            softly.assertThat(counterDto.getAdminUnitType().name()).isEqualTo(counter.getUnitType().name());
        }
    }

    private CapacityValueCounter createCounter(DayOffType dayOffType, UnitType unitType) {
        return CapacityValueCounter.builder()
            .id(111L)
            .capacityId(11L)
            .capacityValueId(1L)
            .unitType(unitType)
            .day(LocalDate.now())
            .count(10L)
            .threshold(100L)
            .dayOff(false)
            .dayOffType(dayOffType)
            .propagatedFrom(null)
            .build();
    }

    private Event createEvent(EventType eventType) {
        return Event.builder()
            .id(1L)
            .key("111_222")
            .type(eventType)
            .route(null)
            .eventTimestamp(LocalDateTime.now())
            .maxServiceTime(LocalDateTime.now())
            .processed(true)
            .externalId(100L)
            .dummy(false)
            .build();
    }

    private static Stream<Arguments> counterArgumentsStream() {
        return Arrays.stream(DayOffType.values())
            .flatMap(dayOffType -> Arrays.stream(UnitType.values())
                .map(unitType -> arguments(dayOffType, unitType))
            );
    }

    private static Stream<Arguments> eventArgumentsStream() {
        return Arrays.stream(EventType.values())
            .map(Arguments::arguments);
    }
}
