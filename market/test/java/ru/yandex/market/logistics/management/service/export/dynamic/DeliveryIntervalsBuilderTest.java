package ru.yandex.market.logistics.management.service.export.dynamic;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.mockito.Mockito;

import ru.yandex.market.logistics.Logistics;
import ru.yandex.market.logistics.management.domain.dto.CalendarDayDto;
import ru.yandex.market.logistics.management.domain.dto.Locations;
import ru.yandex.market.logistics.management.domain.dto.TimeInterval;
import ru.yandex.market.logistics.management.domain.entity.type.PartnerType;
import ru.yandex.market.logistics.management.service.export.dynamic.source.dto.DeliveryDto;
import ru.yandex.market.logistics.management.service.export.dynamic.source.dto.DeliveryIntervalDto;
import ru.yandex.market.logistics.management.service.export.dynamic.source.dto.ScheduleDayDto;

class DeliveryIntervalsBuilderTest extends AbstractDynamicBuilderTest {

    private static final String FILE_PATH = PATH_PREFIX + "intervals/";
    private static final String TIME_INTERVALS_SET_POSTFIX = "__time_set.json";
    private static final String TIME_INTERVALS_POSTFIX = "__days.json";

    private static final long CUSTOM_CALENDAR_ID = 1L;

    @BeforeEach
    void initMocks() {
        builder = new ReportDynamicBuilder(
            validationService,
            partnerService,
            platformClientService,
            deliveryDistributorParamsRepository,
            jdbcDeliveryRepository,
            jdbcWarehouseRepository,
            partnerPlatformClientRepository,
            partnerRelationRepository,
            dynamicLogService,
            CLOCK_MOCK,
            calendarService,
            factory,
            logisticsPointService,
            transactionTemplate
        );
        builder.setDepth(15);
        builder.setDateOffset(0);
    }

    @ParameterizedTest(name = "{index} : {1}")
    @ArgumentsSource(TestArgumentsProvider.class)
    void testDeliveryIntervals(List<DeliveryDto> deliveries,
                               Set<CalendarDayDto> holidays,
                               String jsonNameTemplate) {

        Mockito.when(jdbcDeliveryRepository.findAll(Mockito.any(), Mockito.any())).thenReturn(deliveries);
        Mockito.when(calendarService.getAllHolidaysByCalendarId(Collections.singleton(CUSTOM_CALENDAR_ID)))
            .thenReturn(Map.of(CUSTOM_CALENDAR_ID, holidays));

        Logistics.MetaInfo metaInfo = buildReport();

        String path = FILE_PATH + jsonNameTemplate + "%s";

        softly.assertThat(metaInfo).as("Delivery services are equal")
            .hasSameDSsAs(String.format(path, TIME_INTERVALS_POSTFIX));

        softly.assertThat(metaInfo).as("Days sets are equal")
            .hasSameDaySetAs(String.format(path, DAYS_SET_POSTFIX));

        softly.assertThat(metaInfo).as("Time intervals are equal")
            .hasSameTimeIntervalsAs(String.format(path, TIME_INTERVALS_SET_POSTFIX));
    }

    @Test
    void testEmptyDeliveryIntervals() {
        Mockito.when(jdbcDeliveryRepository.findAll(Mockito.any(), Mockito.any()))
            .thenReturn(createDeliveriesWithEmptyIntervals());

        Logistics.MetaInfo metaInfo = buildReport();

        String path = FILE_PATH + "empty_intervals" + "%s";

        softly.assertThat(metaInfo).as("Delivery services are equal")
            .hasSameDSsAs(String.format(path, TIME_INTERVALS_POSTFIX));

        softly.assertThat(metaInfo).as("Days sets are equal")
            .hasSameDaySetAs(String.format(path, DAYS_SET_POSTFIX));

        softly.assertThat(metaInfo).as("Time intervals are equal")
            .hasSameTimeIntervalsAs(String.format(path, TIME_INTERVALS_SET_POSTFIX));
    }

    private static List<DeliveryDto> createDeliveryWithAllDaysWithSameIntervals() {
        ImmutableMap<Integer, List<TimeInterval>> intervalPerLocation = ImmutableMap.of(
            Locations.RUSSIA,
            Collections.singletonList(newTimeInterval(10, 18))
        );

        return createDeliveryWithLocations(intervalPerLocation);
    }

    private static List<DeliveryDto> createDeliveryWithLocations(
        Map<Integer, List<TimeInterval>> intervalsPerLocation) {
        return createDeliveryWithLocationsAndCalendars(intervalsPerLocation);
    }

    private static List<DeliveryDto> createDeliveryWithLocationsAndCalendars(
        Map<Integer, List<TimeInterval>> intervalsPerLocation) {
        Map<Integer, Map<DayOfWeek, List<TimeInterval>>> sameIntervalsPerDayPerLocation =
            intervalsPerLocation.entrySet().stream()
                .collect(
                    Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> allWeekSameIntervals(entry.getValue())));

        return createDelivery(sameIntervalsPerDayPerLocation);
    }

    private static List<DeliveryDto> createDelivery(
        Map<Integer, Map<DayOfWeek, List<TimeInterval>>> intervalsPerLocation
    ) {
        DeliveryDto delivery = newDelivery(10L);

        int i = 0;
        for (Map.Entry<Integer, Map<DayOfWeek, List<TimeInterval>>> entry : intervalsPerLocation.entrySet()) {

            delivery.getDeliveryIntervals().add(
                createDeliveryInterval(
                    delivery.getId() * 10 + ++i,
                    entry.getKey(),
                    entry.getValue()
                )
            );
        }

        return Collections.singletonList(delivery);
    }

    private static List<DeliveryDto> createDeliveriesWithEmptyIntervals() {
        return IntStream.range(0, 2)
            .mapToObj(
                index -> newDelivery(index * 10L)
            )
            .collect(Collectors.toList());
    }

    private static DeliveryIntervalDto createDeliveryInterval(long id,
                                                              int locationId,
                                                              Map<DayOfWeek, List<TimeInterval>> intervalsPerWeekDay) {
        DeliveryIntervalDto interval = newDeliveryInterval(locationId, CUSTOM_CALENDAR_ID);

        for (Map.Entry<DayOfWeek, List<TimeInterval>> entry : intervalsPerWeekDay.entrySet()) {
            int weekDayIndex = entry.getKey().getValue();
            List<TimeInterval> timeIntervals = entry.getValue();
            for (int j = 0; j < timeIntervals.size(); j++) {
                TimeInterval timeInterval = timeIntervals.get(j);
                interval.addScheduleDay(
                    newScheduleDay(id * 100 + weekDayIndex * 10 + j,
                        weekDayIndex,
                        timeInterval.getFrom().getHour(),
                        timeInterval.getTo().getHour()));
            }
        }

        return interval;
    }

    private static Map<DayOfWeek, List<TimeInterval>> allWeekSameIntervals(List<TimeInterval> intervals) {
        return Stream.of(DayOfWeek.values())
            .collect(Collectors
                .toMap(Function.identity(), dayOfWeek -> intervals));
    }

    private static DeliveryDto newDelivery(long id) {
        return (DeliveryDto) new DeliveryDto()
            .setName("Delivery" + id)
            .setRating(1)
            .setId(id)
            .setPartnerType(PartnerType.DELIVERY);
    }

    private static DeliveryIntervalDto newDeliveryInterval(int locationId, long calendarId) {
        return new DeliveryIntervalDto(calendarId, locationId, new HashSet<>());
    }

    private static ScheduleDayDto newScheduleDay(long id, int day, int hourFrom, int hourTo) {
        return new ScheduleDayDto(day, LocalTime.of(hourFrom, 0), LocalTime.of(hourTo, 0));
    }

    private static Set<CalendarDayDto> newCalendarDays(Set<LocalDate> days) {
        if (days == null) {
            return null;
        }

        return days.stream()
            .map(DeliveryIntervalsBuilderTest::newCalendarDay)
            .collect(Collectors.toSet());
    }

    private static CalendarDayDto newCalendarDay(LocalDate day) {
        return new CalendarDayDto(1, day, true);
    }

    private static TimeInterval newTimeInterval(int hourFrom, int hourTo) {
        return new TimeInterval(LocalTime.of(hourFrom, 0), LocalTime.of(hourTo, 0));
    }

    private static class TestArgumentsProvider implements ArgumentsProvider {

        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
            return Stream.of(
                Arguments.arguments(
                    createDeliveryWithAllDaysWithSameIntervals(),
                    Collections.emptySet(),
                    "one_location_same_intervals"
                ),
                Arguments.arguments(
                    createDeliveryWithLocations(ImmutableMap.of(
                        Locations.RUSSIA, Collections.singletonList(
                            newTimeInterval(10, 18)),
                        Locations.MOSCOW, Arrays.asList(
                            newTimeInterval(10, 18),
                            newTimeInterval(20, 23))
                    )),
                    Collections.emptySet(),
                    "two_locations_different_intervals"
                ),
                Arguments.arguments(
                    createDelivery(ImmutableMap.of(
                        Locations.RUSSIA, ImmutableMap.of(
                            DayOfWeek.MONDAY, Collections.singletonList(
                                newTimeInterval(10, 18)),
                            DayOfWeek.TUESDAY, Collections.singletonList(
                                newTimeInterval(10, 18)),
                            DayOfWeek.WEDNESDAY, Collections.singletonList(
                                newTimeInterval(11, 18)),
                            DayOfWeek.THURSDAY, Collections.singletonList(
                                newTimeInterval(12, 18)),
                            DayOfWeek.FRIDAY, Collections.singletonList(
                                newTimeInterval(10, 18))
                        ))
                    ),
                    Collections.emptySet(),
                    "one_location_different_intervals"
                ),
                Arguments.arguments(
                    createDeliveryWithLocationsAndCalendars(
                        ImmutableMap.of(
                            Locations.RUSSIA, Collections.singletonList(
                                newTimeInterval(10, 18)
                            )
                        )
                    ),
                    newCalendarDays(ImmutableSet.of(LocalDate.of(2018, 10, 6))),
                    "with_location_calendar"
                ),
                Arguments.arguments(
                    createDeliveryWithLocationsAndCalendars(
                        ImmutableMap.of(
                            Locations.RUSSIA, Collections.singletonList(
                                newTimeInterval(10, 18)
                            )
                        )),
                    newCalendarDays(ImmutableSet.of(LocalDate.of(2018, 10, 6))),
                    "with_custom_calendar"
                ),
                Arguments.arguments(
                    createDeliveryWithLocationsAndCalendars(
                        ImmutableMap.of(
                            Locations.RUSSIA, Collections.singletonList(
                                newTimeInterval(10, 18)
                            )
                        )
                    ),
                    newCalendarDays(ImmutableSet.of(LocalDate.of(2018, 10, 6),
                        LocalDate.of(2018, 10, 7))
                    ),
                    "with_two_calendars_and_custom_adds_one_day"
                ),
                Arguments.arguments(
                    createDeliveryWithLocationsAndCalendars(
                        ImmutableMap.of(
                            Locations.RUSSIA, Collections.singletonList(
                                newTimeInterval(10, 18)
                            )
                        )
                    ),
                    Collections.emptySet(),
                    "with_two_calendars_nulls"
                )
            );

        }
    }
}
