package ru.yandex.market.core.schedule;

import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ScheduleUtils}.
 *
 * @author ivmelnik
 * @since 29.05.18
 */
public class ScheduleUtilsTest {

    public static Stream<Arguments> collapseParams() {
        return Stream.of(
                Arguments.of("Non-collapsible",
                        asList(
                                new ScheduleLine(ScheduleLine.DayOfWeek.MONDAY, 0, 540, 480),
                                new ScheduleLine(ScheduleLine.DayOfWeek.WEDNESDAY, 0, 660, 600),
                                new ScheduleLine(ScheduleLine.DayOfWeek.SATURDAY, 0, 600, 120),
                                new ScheduleLine(ScheduleLine.DayOfWeek.SATURDAY, 0, 900, 180)
                        ),
                        asList(
                                new ScheduleLine(ScheduleLine.DayOfWeek.MONDAY, 0, 540, 480),
                                new ScheduleLine(ScheduleLine.DayOfWeek.WEDNESDAY, 0, 660, 600),
                                new ScheduleLine(ScheduleLine.DayOfWeek.SATURDAY, 0, 600, 120),
                                new ScheduleLine(ScheduleLine.DayOfWeek.SATURDAY, 0, 900, 180)
                        )),
                Arguments.of("Perfectly collapsible",
                        asList(
                                new ScheduleLine(ScheduleLine.DayOfWeek.MONDAY, 0, 540, 480),
                                new ScheduleLine(ScheduleLine.DayOfWeek.TUESDAY, 0, 540, 480),
                                new ScheduleLine(ScheduleLine.DayOfWeek.WEDNESDAY, 0, 540, 480),
                                new ScheduleLine(ScheduleLine.DayOfWeek.THURSDAY, 0, 540, 480),
                                new ScheduleLine(ScheduleLine.DayOfWeek.FRIDAY, 0, 540, 480),
                                new ScheduleLine(ScheduleLine.DayOfWeek.SATURDAY, 0, 540, 480),
                                new ScheduleLine(ScheduleLine.DayOfWeek.SUNDAY, 0, 540, 480)
                        ),
                        Collections.singletonList(
                                new ScheduleLine(ScheduleLine.DayOfWeek.MONDAY, 6, 540, 480)
                        )),
                Arguments.of("Partially collapsible workdays and weekend",
                        asList(
                                new ScheduleLine(ScheduleLine.DayOfWeek.MONDAY, 0, 540, 480),
                                new ScheduleLine(ScheduleLine.DayOfWeek.TUESDAY, 0, 540, 480),
                                new ScheduleLine(ScheduleLine.DayOfWeek.WEDNESDAY, 0, 540, 480),
                                new ScheduleLine(ScheduleLine.DayOfWeek.THURSDAY, 0, 540, 480),
                                new ScheduleLine(ScheduleLine.DayOfWeek.FRIDAY, 0, 540, 480),
                                new ScheduleLine(ScheduleLine.DayOfWeek.SATURDAY, 0, 600, 480),
                                new ScheduleLine(ScheduleLine.DayOfWeek.SUNDAY, 0, 600, 480)
                        ),
                        asList(
                                new ScheduleLine(ScheduleLine.DayOfWeek.MONDAY, 4, 540, 480),
                                new ScheduleLine(ScheduleLine.DayOfWeek.SATURDAY, 1, 600, 480)
                        )),
                Arguments.of("Partially collapsible in the mid",
                        asList(
                                new ScheduleLine(ScheduleLine.DayOfWeek.MONDAY, 0, 600, 480),
                                new ScheduleLine(ScheduleLine.DayOfWeek.TUESDAY, 0, 540, 480),
                                new ScheduleLine(ScheduleLine.DayOfWeek.WEDNESDAY, 0, 540, 480),
                                new ScheduleLine(ScheduleLine.DayOfWeek.THURSDAY, 0, 600, 480),
                                new ScheduleLine(ScheduleLine.DayOfWeek.FRIDAY, 0, 540, 480),
                                new ScheduleLine(ScheduleLine.DayOfWeek.SATURDAY, 0, 540, 480),
                                new ScheduleLine(ScheduleLine.DayOfWeek.SUNDAY, 0, 600, 480)
                        ),
                        asList(
                                new ScheduleLine(ScheduleLine.DayOfWeek.MONDAY, 0, 600, 480),
                                new ScheduleLine(ScheduleLine.DayOfWeek.TUESDAY, 1, 540, 480),
                                new ScheduleLine(ScheduleLine.DayOfWeek.THURSDAY, 0, 600, 480),
                                new ScheduleLine(ScheduleLine.DayOfWeek.FRIDAY, 1, 540, 480),
                                new ScheduleLine(ScheduleLine.DayOfWeek.SUNDAY, 0, 600, 480)
                        )),
                Arguments.of("Empty",
                        Collections.emptyList(),
                        Collections.emptyList()),
                Arguments.of("Single line",
                        Collections.singletonList(
                                new ScheduleLine(ScheduleLine.DayOfWeek.MONDAY, 0, 540, 480)
                        ),
                        Collections.singletonList(
                                new ScheduleLine(ScheduleLine.DayOfWeek.MONDAY, 0, 540, 480)
                        )),
                Arguments.of("Two lines equal",
                        asList(
                                new ScheduleLine(ScheduleLine.DayOfWeek.MONDAY, 0, 540, 480),
                                new ScheduleLine(ScheduleLine.DayOfWeek.MONDAY, 0, 540, 480)
                        ),
                        Collections.singletonList(
                                new ScheduleLine(ScheduleLine.DayOfWeek.MONDAY, 0, 540, 480)
                        )),
                Arguments.of("Two lines non-collapsible by day",
                        asList(
                                new ScheduleLine(ScheduleLine.DayOfWeek.MONDAY, 0, 540, 480),
                                new ScheduleLine(ScheduleLine.DayOfWeek.WEDNESDAY, 0, 540, 480)
                        ),
                        asList(
                                new ScheduleLine(ScheduleLine.DayOfWeek.MONDAY, 0, 540, 480),
                                new ScheduleLine(ScheduleLine.DayOfWeek.WEDNESDAY, 0, 540, 480)
                        )),
                Arguments.of("Two lines non-collapsible by time",
                        asList(
                                new ScheduleLine(ScheduleLine.DayOfWeek.MONDAY, 0, 540, 480),
                                new ScheduleLine(ScheduleLine.DayOfWeek.TUESDAY, 0, 600, 480)
                        ),
                        asList(
                                new ScheduleLine(ScheduleLine.DayOfWeek.MONDAY, 0, 540, 480),
                                new ScheduleLine(ScheduleLine.DayOfWeek.TUESDAY, 0, 600, 480)
                        )),
                Arguments.of("Two lines collapsible",
                        asList(
                                new ScheduleLine(ScheduleLine.DayOfWeek.MONDAY, 0, 540, 480),
                                new ScheduleLine(ScheduleLine.DayOfWeek.TUESDAY, 0, 540, 480)
                        ),
                        Collections.singletonList(
                                new ScheduleLine(ScheduleLine.DayOfWeek.MONDAY, 1, 540, 480)
                        )),
                Arguments.of("Multidays non-collapsible by day",
                        asList(
                                new ScheduleLine(ScheduleLine.DayOfWeek.MONDAY, 2, 540, 480),
                                new ScheduleLine(ScheduleLine.DayOfWeek.FRIDAY, 0, 540, 480)
                        ),
                        asList(
                                new ScheduleLine(ScheduleLine.DayOfWeek.MONDAY, 2, 540, 480),
                                new ScheduleLine(ScheduleLine.DayOfWeek.FRIDAY, 0, 540, 480)
                        )),
                Arguments.of("Multidays non-collapsible by time",
                        asList(
                                new ScheduleLine(ScheduleLine.DayOfWeek.MONDAY, 2, 540, 480),
                                new ScheduleLine(ScheduleLine.DayOfWeek.THURSDAY, 0, 600, 480)
                        ),
                        asList(
                                new ScheduleLine(ScheduleLine.DayOfWeek.MONDAY, 2, 540, 480),
                                new ScheduleLine(ScheduleLine.DayOfWeek.THURSDAY, 0, 600, 480)
                        )),
                Arguments.of("Multidays collapsible sequential",
                        asList(
                                new ScheduleLine(ScheduleLine.DayOfWeek.MONDAY, 2, 540, 480),
                                new ScheduleLine(ScheduleLine.DayOfWeek.THURSDAY, 0, 540, 480)
                        ),
                        Collections.singletonList(
                                new ScheduleLine(ScheduleLine.DayOfWeek.MONDAY, 3, 540, 480)
                        )),
                Arguments.of("Multidays collapsible sequential 2",
                        asList(
                                new ScheduleLine(ScheduleLine.DayOfWeek.MONDAY, 0, 540, 480),
                                new ScheduleLine(ScheduleLine.DayOfWeek.TUESDAY, 2, 540, 480)
                        ),
                        Collections.singletonList(
                                new ScheduleLine(ScheduleLine.DayOfWeek.MONDAY, 3, 540, 480)
                        )),
                Arguments.of("Multidays collapsible sequential 3",
                        asList(
                                new ScheduleLine(ScheduleLine.DayOfWeek.MONDAY, 1, 540, 480),
                                new ScheduleLine(ScheduleLine.DayOfWeek.WEDNESDAY, 2, 540, 480)
                        ),
                        Collections.singletonList(
                                new ScheduleLine(ScheduleLine.DayOfWeek.MONDAY, 4, 540, 480)
                        )),
                Arguments.of("Multidays collapsible inclusive",
                        asList(
                                new ScheduleLine(ScheduleLine.DayOfWeek.MONDAY, 2, 540, 480),
                                new ScheduleLine(ScheduleLine.DayOfWeek.WEDNESDAY, 0, 540, 480)
                        ),
                        Collections.singletonList(
                                new ScheduleLine(ScheduleLine.DayOfWeek.MONDAY, 2, 540, 480)
                        )),
                Arguments.of("Multidays collapsible inclusive 2",
                        asList(
                                new ScheduleLine(ScheduleLine.DayOfWeek.MONDAY, 3, 540, 480),
                                new ScheduleLine(ScheduleLine.DayOfWeek.WEDNESDAY, 0, 540, 480)
                        ),
                        Collections.singletonList(
                                new ScheduleLine(ScheduleLine.DayOfWeek.MONDAY, 3, 540, 480)
                        )),
                Arguments.of("Multidays collapsible intersect",
                        asList(
                                new ScheduleLine(ScheduleLine.DayOfWeek.MONDAY, 2, 540, 480),
                                new ScheduleLine(ScheduleLine.DayOfWeek.WEDNESDAY, 2, 540, 480)
                        ),
                        Collections.singletonList(
                                new ScheduleLine(ScheduleLine.DayOfWeek.MONDAY, 4, 540, 480)
                        )),
                Arguments.of("Multidays collapsible equal",
                        asList(
                                new ScheduleLine(ScheduleLine.DayOfWeek.MONDAY, 0, 540, 480),
                                new ScheduleLine(ScheduleLine.DayOfWeek.MONDAY, 0, 540, 480)
                        ),
                        Collections.singletonList(
                                new ScheduleLine(ScheduleLine.DayOfWeek.MONDAY, 0, 540, 480)
                        )),
                Arguments.of("Mix",
                        asList(
                                new ScheduleLine(ScheduleLine.DayOfWeek.MONDAY, 0, 540, 480),
                                new ScheduleLine(ScheduleLine.DayOfWeek.TUESDAY, 0, 600, 480),
                                new ScheduleLine(ScheduleLine.DayOfWeek.TUESDAY, 2, 540, 480),
                                new ScheduleLine(ScheduleLine.DayOfWeek.WEDNESDAY, 2, 540, 480),
                                new ScheduleLine(ScheduleLine.DayOfWeek.FRIDAY, 2, 600, 480),
                                new ScheduleLine(ScheduleLine.DayOfWeek.SATURDAY, 0, 600, 480)
                        ),
                        asList(
                                new ScheduleLine(ScheduleLine.DayOfWeek.MONDAY, 4, 540, 480),
                                new ScheduleLine(ScheduleLine.DayOfWeek.TUESDAY, 0, 600, 480),
                                new ScheduleLine(ScheduleLine.DayOfWeek.FRIDAY, 2, 600, 480)
                        ))
        );
    }

    public static Stream<Arguments> correctAdjacentIntervalsParams() {
        return Stream.of(
                Arguments.of(List.of(
                                new ScheduleLine(ScheduleLine.DayOfWeek.MONDAY, 6, 0, 100),
                                // проверяем, что 0 не конвертится в -1
                                new ScheduleLine(ScheduleLine.DayOfWeek.MONDAY, 6, 100, 0),
                                new ScheduleLine(ScheduleLine.DayOfWeek.MONDAY, 6, 100, 200),
                                new ScheduleLine(ScheduleLine.DayOfWeek.MONDAY, 6, 300, 200)),
                        List.of(
                                new ScheduleLine(ScheduleLine.DayOfWeek.MONDAY, 6, 0, 99),
                                new ScheduleLine(ScheduleLine.DayOfWeek.MONDAY, 6, 100, 0),
                                new ScheduleLine(ScheduleLine.DayOfWeek.MONDAY, 6, 100, 199),
                                new ScheduleLine(ScheduleLine.DayOfWeek.MONDAY, 6, 300, 200))),
                // нет пересечений
                Arguments.of(List.of(
                                new ScheduleLine(ScheduleLine.DayOfWeek.MONDAY, 6, 0, 0),
                                new ScheduleLine(ScheduleLine.DayOfWeek.MONDAY, 6, 0, 100),
                                new ScheduleLine(ScheduleLine.DayOfWeek.MONDAY, 6, 200, 200),
                                new ScheduleLine(ScheduleLine.DayOfWeek.MONDAY, 6, 500, 200)),
                        List.of(
                                new ScheduleLine(ScheduleLine.DayOfWeek.MONDAY, 6, 0, 0),
                                new ScheduleLine(ScheduleLine.DayOfWeek.MONDAY, 6, 0, 100),
                                new ScheduleLine(ScheduleLine.DayOfWeek.MONDAY, 6, 200, 200),
                                new ScheduleLine(ScheduleLine.DayOfWeek.MONDAY, 6, 500, 200)))
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("collapseParams")
    public void collapse(String testName, List<ScheduleLine> originalSchedule,
                         List<ScheduleLine> expectedCollapsedSchedule) {
        List<ScheduleLine> collapsedSchedule = ScheduleUtils.collapse(originalSchedule);
        assertThat(collapsedSchedule)
                .containsExactlyElementsOf(expectedCollapsedSchedule);
    }

    @ParameterizedTest(name = "[{index}]")
    @MethodSource("correctAdjacentIntervalsParams")
    public void testCorrectAdjacentIntervals(List<ScheduleLine> lines, List<ScheduleLine> expectedLines) {
        List<ScheduleLine> correctedLines = ScheduleUtils.correctAdjacentIntervals(lines);
        assertThat(correctedLines)
                .containsExactlyElementsOf(expectedLines);
    }
}
