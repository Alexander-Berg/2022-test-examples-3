package ru.yandex.market.delivery.transport_manager.service.calendaring;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.tuple.Pair;
import org.assertj.core.api.JUnitJupiterSoftAssertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.delivery.transport_manager.domain.entity.WeeklySchedule;
import ru.yandex.market.delivery.transport_manager.service.WeeklyScheduleService;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SlotSelectionServiceTest {

    private static List<SlotData> tuesdayOutSlots = List.of(slot("2021-07-20T11:00:00", "2021-07-20T12:00:00"));
    private static List<SlotData> tuesdayInSlots = List.of(slot("2021-07-20T16:00:00", "2021-07-20T17:00:00"));

    private WeeklyScheduleService weeklyScheduleService = mock(WeeklyScheduleService.class);
    private SlotSelectionService service = new SlotSelectionService(weeklyScheduleService);

    @RegisterExtension
    protected final JUnitJupiterSoftAssertions softly = new JUnitJupiterSoftAssertions();

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("invalidCriteriaData")
    void invalidCriteria(String caseName, SlotSelectionCriteria.Builder criteriaBuilder, String messagePart) {
        softly.assertThatThrownBy(() -> service.selectSlots(List.of(), List.of(), criteriaBuilder.build()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("SlotSelectionCriteria")
            .hasMessageContaining("is invalid")
            .hasMessageContaining(messagePart);
    }

    private static Stream<Arguments> invalidCriteriaData() {
        return Stream.of(
            Arguments.of("minOutboundStart is null", sampleCriteriaBuilder().minOutboundStart(null),
                "some of them are null"),
            Arguments.of("optimalOutboundStart is null", sampleCriteriaBuilder().optimalOutboundStart(null),
                "some of them are null"),
            Arguments.of("maxOutboundEnd is null", sampleCriteriaBuilder().maxOutboundEnd(null),
                "some of them are null"),
            Arguments.of("minInboundStart is null", sampleCriteriaBuilder().minInboundStart(null),
                "some of them are null"),
            Arguments.of("optimalInboundStart is null", sampleCriteriaBuilder().optimalInboundStart(null),
                "some of them are null"),
            Arguments.of("maxInboundEnd is null", sampleCriteriaBuilder().maxInboundEnd(null),
                "some of them are null"),
            Arguments.of("minMovementDuration is null", sampleCriteriaBuilder().minMovementDuration(null),
                "some of them are null"),
            Arguments.of("maxMovementDuration is null", sampleCriteriaBuilder().maxMovementDuration(null),
                "some of them are null"),
            Arguments.of("Incompatible movement bounds", sampleCriteriaBuilder()
                    .minMovementDuration(Duration.ofHours(6)),
                "is greater than"),
            Arguments.of("Negative movement duration", sampleCriteriaBuilder()
                    .minMovementDuration(Duration.ofHours(-1)),
                "is greater than"),
            Arguments.of("Incompatible outbound bounds", sampleCriteriaBuilder()
                    .maxOutboundEnd(LocalDateTime.parse("2021-07-20T08:00:00")),
                "is greater than"),
            Arguments.of("Incompatible inbound bounds", sampleCriteriaBuilder()
                    .maxInboundEnd(LocalDateTime.parse("2021-07-20T14:00:00")),
                "is greater than"),
            Arguments.of("optimalOutboundStart out of range", sampleCriteriaBuilder()
                    .optimalOutboundStart(LocalDateTime.parse("2021-07-20T08:59:00")),
                "is greater than"),
            Arguments.of("optimalInboundStart out of range", sampleCriteriaBuilder()
                    .optimalInboundStart(LocalDateTime.parse("2021-07-20T14:59:00")),
                "is greater than")
        );
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("slotsNotFoundData")
    void slotsNotFound(String caseName, List<SlotData> outboundSlots, List<SlotData> inboundSlots) {
        when(weeklyScheduleService.findAll()).thenReturn(fullSchedule());
        softly.assertThat(service
            .selectSlots(outboundSlots, inboundSlots, sampleCriteriaBuilder().build())
            .isEmpty()
        )
            .isTrue();
    }

    private static Stream<Arguments> slotsNotFoundData() {
        return Stream.of(
            Arguments.of("No slots", List.of(), List.of()),
            Arguments.of("No outbound slots", List.of(), List.of(slot("2021-07-20T11:00:00", "2021-07-20T12:00:00"))),
            Arguments.of("No inbound slots", List.of(slot("2021-07-20T16:00:00", "2021-07-20T17:00:00")), List.of()),
            Arguments.of("Slots not match outbound",
                List.of(slot("2021-07-20T14:00:00", "2021-07-20T14:30:00")),
                List.of(slot("2021-07-20T17:30:00", "2021-07-20T18:00:00"))),
            Arguments.of("Slots not match inbound",
                List.of(slot("2021-07-20T10:00:00", "2021-07-20T11:00:00")),
                List.of(slot("2021-07-20T14:00:00", "2021-07-20T14:30:00"))),
            Arguments.of("Slots not match movement duration",
                List.of(slot("2021-07-20T10:00:00", "2021-07-20T10:30:00")),
                List.of(slot("2021-07-20T17:30:00", "2021-07-20T18:00:00")))
        );
    }

    @Test
    void slotNotFoundWhenScheduleDayNotMatch() {
        // Убираем вторник
        var scheduleServiceResponse = fullSchedule().entrySet().stream()
            .filter(e -> e.getKey() != 2)
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        when(weeklyScheduleService.findAll()).thenReturn(scheduleServiceResponse);
        softly.assertThat(service.selectSlots(
            tuesdayOutSlots,
            tuesdayInSlots,
            sampleCriteriaBuilder().build()
        ).isEmpty())
            .isTrue();
    }

    @Test
    void slotNotFoundWhenScheduleTimeNotMatchOutbound() {
        var scheduleServiceResponse = fullSchedule().entrySet().stream()
            .map(e -> e.getKey() == 2 ?
                Pair.of(2, new WeeklySchedule(
                    2,
                    LocalTime.of(13, 0),
                    LocalTime.of(20, 0)
                )) :
                Pair.of(e.getKey(), e.getValue()))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        when(weeklyScheduleService.findAll()).thenReturn(scheduleServiceResponse);
        softly.assertThat(service.selectSlots(
            tuesdayOutSlots,
            tuesdayInSlots,
            sampleCriteriaBuilder().build()
        ).isEmpty())
            .isTrue();
    }

    @Test
    void slotFoundWhenScheduleTimeNotMatchInbound() {
        var scheduleServiceResponse = fullSchedule().entrySet().stream()
            .map(e -> e.getKey() == 2 ?
                Pair.of(2, new WeeklySchedule(
                    2,
                    LocalTime.of(9, 0),
                    LocalTime.of(15, 0)
                )) :
                Pair.of(e.getKey(), e.getValue()))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        when(weeklyScheduleService.findAll()).thenReturn(scheduleServiceResponse);
        var slotPairOpt = service.selectSlots(tuesdayOutSlots, tuesdayInSlots, sampleCriteriaBuilder().build());
        softly.assertThat(slotPairOpt.isPresent()).isTrue();
        softly.assertThat(slotPairOpt.get().getOutboundSlot())
            .isEqualTo(tuesdayOutSlots.get(0));
        softly.assertThat(slotPairOpt.get().getInboundSlot())
            .isEqualTo(tuesdayInSlots.get(0));
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("slotSelectedData")
    void slotSelected(
        String caseName,
        SlotData expectedOutbound,
        SlotData expectedInbound,
        List<SlotData> outboundSlots,
        List<SlotData> inboundSlots
    ) {
        when(weeklyScheduleService.findAll()).thenReturn(fullSchedule());
        var selected = service.selectSlots(outboundSlots, inboundSlots, sampleCriteriaBuilder().build());
        softly.assertThat(selected.isPresent()).isTrue();
        softly.assertThat(selected.get().getOutboundSlot())
            .isEqualTo(expectedOutbound);
        softly.assertThat(selected.get().getInboundSlot())
            .isEqualTo(expectedInbound);
    }

    private static Stream<Arguments> slotSelectedData() {
        return Stream.of(
            Arguments.of("Select from single",
                tuesdayOutSlots.get(0),
                tuesdayInSlots.get(0),
                tuesdayOutSlots,
                tuesdayInSlots),

            Arguments.of("Select boundary slots ",
                slot("2021-07-20T09:00:00", "2021-07-20T13:00:00"),
                slot("2021-07-20T15:00:00", "2021-07-20T19:00:00"),
                List.of(slot("2021-07-20T09:00:00", "2021-07-20T13:00:00")),
                List.of(slot("2021-07-20T15:00:00", "2021-07-20T19:00:00"))),

            Arguments.of("Select optimal",
                slot("2021-07-20T11:00:00", "2021-07-20T12:00:00"),
                slot("2021-07-20T17:00:00", "2021-07-20T18:00:00"),
                List.of(
                    slot("2021-07-20T11:00:00", "2021-07-20T12:00:00"),
                    slot("2021-07-20T12:00:00", "2021-07-20T12:30:00"),
                    slot("2021-07-20T12:30:00", "2021-07-20T13:00:00")
                ),
                List.of(
                    slot("2021-07-20T16:00:00", "2021-07-20T16:30:00"),
                    slot("2021-07-20T16:30:00", "2021-07-20T17:00:00"),
                    slot("2021-07-20T17:00:00", "2021-07-20T18:00:00")
                ))
        );
    }

    private static SlotSelectionCriteria.Builder sampleCriteriaBuilder() {
        return SlotSelectionCriteria.builder()
            .minOutboundStart(LocalDateTime.parse("2021-07-20T09:00:00"))
            .optimalOutboundStart(LocalDateTime.parse("2021-07-20T11:00:00"))
            .maxOutboundEnd(LocalDateTime.parse("2021-07-20T13:00:00"))

            .minInboundStart(LocalDateTime.parse("2021-07-20T15:00:00"))
            .optimalInboundStart(LocalDateTime.parse("2021-07-20T17:00:00"))
            .maxInboundEnd(LocalDateTime.parse("2021-07-20T19:00:00"))

            .minMovementDuration(Duration.ofHours(2))
            .maxMovementDuration(Duration.ofHours(5));
    }

    private static SlotData slot(String from, String to) {
        return new SlotData(
            LocalDateTime.parse(from),
            LocalDateTime.parse(to)
        );
    }

    private static Map<Integer, WeeklySchedule> fullSchedule() {
        return Map.of(
            1, new WeeklySchedule(1, LocalTime.MIN, LocalTime.MAX),
            2, new WeeklySchedule(2, LocalTime.MIN, LocalTime.MAX),
            3, new WeeklySchedule(3, LocalTime.MIN, LocalTime.MAX),
            4, new WeeklySchedule(4, LocalTime.MIN, LocalTime.MAX),
            5, new WeeklySchedule(5, LocalTime.MIN, LocalTime.MAX),
            6, new WeeklySchedule(6, LocalTime.MIN, LocalTime.MAX),
            7, new WeeklySchedule(7, LocalTime.MIN, LocalTime.MAX)
        );
    }
}
