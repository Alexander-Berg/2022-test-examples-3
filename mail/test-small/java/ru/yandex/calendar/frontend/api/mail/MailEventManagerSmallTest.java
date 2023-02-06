package ru.yandex.calendar.frontend.api.mail;

import java.util.List;
import java.util.stream.Stream;

import lombok.AllArgsConstructor;
import lombok.val;
import one.util.streamex.StreamEx;
import org.joda.time.Duration;
import org.joda.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.bolts.collection.CollectorsF;
import ru.yandex.calendar.logic.ics.iv5j.ical.IcsCalendar;
import ru.yandex.calendar.logic.ics.iv5j.ical.component.IcsVEvent;
import ru.yandex.calendar.logic.ics.iv5j.ical.property.IcsMethod;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@AllArgsConstructor
public class MailEventManagerSmallTest {
    public static Stream<Arguments> parameters() {
        return Stream.of(
                Arguments.of(new IcsCalendar(), "no-events"),
                Arguments.of(prepareCalendar(emptyList(), IcsMethod.ADD), "unsupported-ics-method"),
                Arguments.of(prepareCalendar(emptyList(), IcsMethod.REQUEST, IcsMethod.CANCEL),
                        "unsupported-ics-method"),
                Arguments.of(prepareCalendar(emptyList(), IcsMethod.CANCEL), "no-events"),
                Arguments.of(prepareCalendar(List.of("2", "1", "2"), IcsMethod.CANCEL), "more-than-one-event"),
                Arguments.of(new IcsCalendar().addComponent(
                        new IcsVEvent()
                        .withDtStart(Instant.now().plus(Duration.standardMinutes(30)))
                        .withDtEnd(Instant.now().plus(Duration.standardMinutes(60)))
                ), "missing-ics-uid"),
                Arguments.of(prepareCalendar(List.of("1", "1")), ""));
    }

    @ParameterizedTest(name = "{index}. {1}")
    @DisplayName("Calculate IcsVEventGroup base on IcsMethod.")
    @MethodSource("parameters")
    public void getAcceptableGroup(IcsCalendar calendar, String message) {
        if (message.isEmpty()) {
            val group = MailEventManager.getAcceptableGroup(calendar);
            assertThat(group.getUid().toOptional()).contains("1");
            assertThat(group.getEvents().stream().flatMap(e -> e.getUid().stream()))
                    .containsExactlyInAnyOrder("1", "1");
        } else {
            assertThatThrownBy(() -> MailEventManager.getAcceptableGroup(calendar))
                    .isInstanceOf(MailEventException.class)
                    .hasMessage(message);
        }
    }

    private static IcsCalendar prepareCalendar(List<String> events, IcsMethod... icsMethods) {
        return new IcsCalendar()
                .withProperties(StreamEx.of(icsMethods).collect(CollectorsF.toList()))
                .addComponents(StreamEx.of(events).map(uid -> {
                    Instant start = Instant.now()
                            .plus(Math.round(Math.random() * Duration.standardMinutes(60).getStandardSeconds() * 1000));
                    return new IcsVEvent()
                            .withDtStart(start)
                            .withDtEnd(start.plus(Duration.standardHours(1)))
                            .withUid(uid);
                }).collect(CollectorsF.toList()));
    }
}
