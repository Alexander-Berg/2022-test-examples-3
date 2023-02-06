package ru.yandex.market.core.util.schedule;

import java.time.DayOfWeek;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoField;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;
import org.junit.jupiter.api.DisplayName;

import static java.time.DayOfWeek.FRIDAY;
import static java.time.DayOfWeek.MONDAY;
import static java.time.DayOfWeek.SATURDAY;
import static java.time.DayOfWeek.SUNDAY;
import static java.time.DayOfWeek.THURSDAY;
import static java.time.DayOfWeek.TUESDAY;
import static java.time.DayOfWeek.WEDNESDAY;

/**
 * Тесты для {@link ScheduleUtils}.
 *
 * @author Kirill Batalin (batalin@yandex-team.ru)
 */
public class ScheduleUtilsTest {

    private static final ZoneOffset SAMOA_TZ = getTimezone("Pacific/Samoa", -39600); // -11
    private static final ZoneOffset BOGOTA_TZ = getTimezone("America/Bogota", -18000); // -5
    private static final ZoneOffset LIBREVILLE_TZ = getTimezone("Africa/Libreville", 3600); // 1
    private static final ZoneOffset ROME_TZ = getTimezone("Europe/Rome", 7200); // 2
    private static final ZoneOffset MOSCOW_TZ = getTimezone("Europe/Moscow", 10800); // 3
    private static final ZoneOffset YEREVAN_TZ = getTimezone("Asia/Yerevan", 14400); // 4
    private static final ZoneOffset TEHRAN_TZ = getTimezone("Asia/Tehran", 16200); // 4.5
    private static final ZoneOffset YEKATERINBURG_TZ = getTimezone("Asia/Yekaterinburg", 18000); // 5
    private static final ZoneOffset TONGATAPU_TZ = getTimezone("Pacific/Tongatapu", 46800); // 13
    private static final ZoneOffset KIRITIMATI_TZ = getTimezone("Pacific/Kiritimati", 50400); // 14

    // ----- Тесты конвертации таймзоны -------

    private static ZoneOffset getTimezone(final String name, final int offset) {
        return ZoneOffset.ofTotalSeconds(offset);
    }

    @Nonnull
    private static ZonedDateTime createDateTime(final ZoneOffset zone, final DayOfWeek day, final int hours, final int minutes) {
        return ZonedDateTime.now(zone)
                .with(ChronoField.DAY_OF_WEEK, day.getValue())
                .with(ChronoField.MILLI_OF_DAY, 0)
                .with(ChronoField.HOUR_OF_DAY, hours)
                .with(ChronoField.MINUTE_OF_HOUR, minutes);
    }

    @Test
    @DisplayName("Та же таймзона")
    public void testSameTz() {
        final ScheduleInterval schedule = new ScheduleInterval(
                createDateTime(MOSCOW_TZ, THURSDAY, 3, 12),
                createDateTime(MOSCOW_TZ, SATURDAY, 11, 40)
        );
        final List<ScheduleInterval> weeklySchedules = ScheduleUtils.convertTimezone(schedule, MOSCOW_TZ);

        Assert.assertEquals(1, weeklySchedules.size());
        Assert.assertEquals(schedule, weeklySchedules.get(0));
    }

    @Test
    @DisplayName("Расписание на всю неделю")
    public void testTzFull() {
        final ScheduleInterval schedule = new ScheduleInterval(
                createDateTime(MOSCOW_TZ, MONDAY, 0, 0),
                createDateTime(MOSCOW_TZ, MONDAY, 0, 0),
                1
        );
        List<ScheduleInterval> actual = ScheduleUtils.convertTimezone(schedule, ROME_TZ);

        List<ScheduleInterval> expected = Arrays.asList(
                new ScheduleInterval(
                        createDateTime(ROME_TZ, MONDAY, 0, 0),
                        createDateTime(ROME_TZ, SUNDAY, 23, 0),
                        1
                ),
                new ScheduleInterval(
                        createDateTime(ROME_TZ, MONDAY, 23, 0),
                        createDateTime(ROME_TZ, MONDAY, 0, 0),
                        1
                )
        );
        Assert.assertThat(actual, Matchers.containsInAnyOrder(expected.toArray()));

        // Обратно
        actual = ScheduleUtils.convertTimezone(expected, MOSCOW_TZ, true);
        Assert.assertEquals(1, actual.size());
        Assert.assertEquals(schedule, actual.get(0));
    }

    @Test
    @DisplayName("Сдвиг в рамках тех же дней")
    public void testTzInDay() {
        // Вперед
        final ScheduleInterval schedule = new ScheduleInterval(
                createDateTime(LIBREVILLE_TZ, THURSDAY, 5, 0),
                createDateTime(LIBREVILLE_TZ, FRIDAY, 19, 0)
        );

        List<ScheduleInterval> actual = ScheduleUtils.convertTimezone(schedule, MOSCOW_TZ);

        final ScheduleInterval expected = new ScheduleInterval(
                createDateTime(MOSCOW_TZ, THURSDAY, 7, 0),
                createDateTime(MOSCOW_TZ, FRIDAY, 21, 0)
        );
        Assert.assertEquals(1, actual.size());
        Assert.assertEquals(expected, actual.get(0));

        // Назад
        actual = ScheduleUtils.convertTimezone(expected, LIBREVILLE_TZ);

        Assert.assertEquals(1, actual.size());
        Assert.assertEquals(schedule, actual.get(0));
    }

    @Test
    @DisplayName("Сдвиг вперед в рамках одной недели")
    public void testTzInWeekForward() {
        // Вперед
        final ScheduleInterval schedule = new ScheduleInterval(
                createDateTime(LIBREVILLE_TZ, TUESDAY, 5, 0),
                createDateTime(LIBREVILLE_TZ, FRIDAY, 23, 0),
                1
        );

        List<ScheduleInterval> actual = ScheduleUtils.convertTimezone(schedule, YEKATERINBURG_TZ);

        List<ScheduleInterval> expected = Arrays.asList(
                new ScheduleInterval(
                        createDateTime(YEKATERINBURG_TZ, TUESDAY, 9, 0),
                        createDateTime(YEKATERINBURG_TZ, SATURDAY, 0, 0),
                        1
                ),
                new ScheduleInterval(
                        createDateTime(YEKATERINBURG_TZ, WEDNESDAY, 0, 0),
                        createDateTime(YEKATERINBURG_TZ, SATURDAY, 3, 0),
                        1
                )
        );
        Assert.assertThat(actual, Matchers.containsInAnyOrder(expected.toArray()));

        // Назад с объединением
        actual = ScheduleUtils.convertTimezone(expected, LIBREVILLE_TZ, true);
        Assert.assertEquals(1, actual.size());
        Assert.assertEquals(schedule, actual.get(0));

        // Назад без объединения
        actual = expected.stream()
                .map(e -> ScheduleUtils.convertTimezone(e, LIBREVILLE_TZ))
                .flatMap(List::stream)
                .collect(Collectors.toList());

        expected = Arrays.asList(
                new ScheduleInterval(
                        createDateTime(LIBREVILLE_TZ, TUESDAY, 5, 0),
                        createDateTime(LIBREVILLE_TZ, FRIDAY, 20, 0),
                        1
                ),
                new ScheduleInterval(
                        createDateTime(LIBREVILLE_TZ, TUESDAY, 20, 0),
                        createDateTime(LIBREVILLE_TZ, FRIDAY, 23, 0),
                        1
                )
        );
        Assert.assertThat(actual, Matchers.containsInAnyOrder(expected.toArray()));
    }

    @Test
    @DisplayName("Сдвиг назад в рамках одной недели")
    public void testTzInWeekBackward() {
        // Назад
        final ScheduleInterval schedule = new ScheduleInterval(
                createDateTime(YEKATERINBURG_TZ, TUESDAY, 1, 21),
                createDateTime(YEKATERINBURG_TZ, FRIDAY, 19, 43),
                1
        );
        List<ScheduleInterval> actual = ScheduleUtils.convertTimezone(schedule, LIBREVILLE_TZ);

        List<ScheduleInterval> expected = Arrays.asList(
                new ScheduleInterval(
                        createDateTime(LIBREVILLE_TZ, MONDAY, 21, 21),
                        createDateTime(LIBREVILLE_TZ, FRIDAY, 0, 0),
                        1
                ),
                new ScheduleInterval(
                        createDateTime(LIBREVILLE_TZ, TUESDAY, 0, 0),
                        createDateTime(LIBREVILLE_TZ, FRIDAY, 15, 43),
                        1
                )
        );
        Assert.assertThat(actual, Matchers.containsInAnyOrder(expected.toArray()));

        // Вперед с объединением
        actual = ScheduleUtils.convertTimezone(expected, YEKATERINBURG_TZ, true);
        Assert.assertEquals(1, actual.size());
        Assert.assertEquals(schedule, actual.get(0));

        // Вперед без объединения
        actual = expected.stream()
                .map(e -> ScheduleUtils.convertTimezone(e, YEKATERINBURG_TZ))
                .flatMap(List::stream)
                .collect(Collectors.toList());

        expected = Arrays.asList(
                new ScheduleInterval(
                        createDateTime(YEKATERINBURG_TZ, TUESDAY, 1, 21),
                        createDateTime(YEKATERINBURG_TZ, FRIDAY, 4, 0),
                        1
                ),
                new ScheduleInterval(
                        createDateTime(YEKATERINBURG_TZ, TUESDAY, 4, 0),
                        createDateTime(YEKATERINBURG_TZ, FRIDAY, 19, 43),
                        1
                )

        );
        Assert.assertThat(actual, Matchers.containsInAnyOrder(expected.toArray()));
    }

    @Test
    @DisplayName("Сдвиг вперед. Один день следующей недели")
    public void testTzForwardOne() {
        // Вперед
        final ScheduleInterval schedule = new ScheduleInterval(
                createDateTime(LIBREVILLE_TZ, TUESDAY, 5, 0),
                createDateTime(LIBREVILLE_TZ, SUNDAY, 23, 0),
                1
        );

        List<ScheduleInterval> actual = ScheduleUtils.convertTimezone(schedule, YEKATERINBURG_TZ);

        List<ScheduleInterval> expected = Arrays.asList(
                new ScheduleInterval(
                        createDateTime(YEKATERINBURG_TZ, TUESDAY, 9, 0),
                        createDateTime(YEKATERINBURG_TZ, MONDAY, 0, 0),
                        1
                ),
                new ScheduleInterval(
                        createDateTime(YEKATERINBURG_TZ, WEDNESDAY, 0, 0),
                        createDateTime(YEKATERINBURG_TZ, SUNDAY, 3, 0),
                        1
                ),
                new ScheduleInterval(
                        createDateTime(YEKATERINBURG_TZ, MONDAY, 0, 0),
                        createDateTime(YEKATERINBURG_TZ, MONDAY, 3, 0),
                        1
                )
        );
        Assert.assertThat(actual, Matchers.containsInAnyOrder(expected.toArray()));

        // Назад с объединением
        actual = ScheduleUtils.convertTimezone(expected, LIBREVILLE_TZ, true);
        Assert.assertEquals(1, actual.size());
        Assert.assertEquals(schedule, actual.get(0));

        // Назад без объединения
        actual = expected.stream()
                .map(e -> ScheduleUtils.convertTimezone(e, LIBREVILLE_TZ))
                .flatMap(List::stream)
                .collect(Collectors.toList());

        expected = Arrays.asList(
                new ScheduleInterval(
                        createDateTime(LIBREVILLE_TZ, TUESDAY, 5, 0),
                        createDateTime(LIBREVILLE_TZ, SUNDAY, 20, 0),
                        1
                ),
                new ScheduleInterval(
                        createDateTime(LIBREVILLE_TZ, TUESDAY, 20, 0),
                        createDateTime(LIBREVILLE_TZ, SATURDAY, 23, 0),
                        1
                ),
                new ScheduleInterval(
                        createDateTime(LIBREVILLE_TZ, SUNDAY, 20, 0),
                        createDateTime(LIBREVILLE_TZ, SUNDAY, 23, 0),
                        1
                )
        );
        Assert.assertThat(actual, Matchers.containsInAnyOrder(expected.toArray()));
    }

    @Test
    @DisplayName("Сдвиг назад. Один день предыдущей недели")
    public void testTzBackwardOne() {
        // Назад
        final ScheduleInterval schedule = new ScheduleInterval(
                createDateTime(YEKATERINBURG_TZ, MONDAY, 1, 21),
                createDateTime(YEKATERINBURG_TZ, FRIDAY, 19, 43),
                1
        );
        List<ScheduleInterval> actual = ScheduleUtils.convertTimezone(schedule, LIBREVILLE_TZ);

        List<ScheduleInterval> expected = Arrays.asList(
                new ScheduleInterval(
                        createDateTime(LIBREVILLE_TZ, MONDAY, 21, 21),
                        createDateTime(LIBREVILLE_TZ, FRIDAY, 0, 0),
                        1
                ),
                new ScheduleInterval(
                        createDateTime(LIBREVILLE_TZ, MONDAY, 0, 0),
                        createDateTime(LIBREVILLE_TZ, FRIDAY, 15, 43),
                        1
                ),
                new ScheduleInterval(
                        createDateTime(LIBREVILLE_TZ, SUNDAY, 21, 21),
                        createDateTime(LIBREVILLE_TZ, MONDAY, 0, 0),
                        1
                )
        );
        Assert.assertThat(actual, Matchers.containsInAnyOrder(expected.toArray()));

        // Вперед с объединением
        actual = ScheduleUtils.convertTimezone(expected, YEKATERINBURG_TZ, true);
        Assert.assertEquals(1, actual.size());
        Assert.assertEquals(schedule, actual.get(0));

        // Вперед без объединения
        actual = expected.stream()
                .map(e -> ScheduleUtils.convertTimezone(e, YEKATERINBURG_TZ))
                .flatMap(List::stream)
                .collect(Collectors.toList());

        expected = Arrays.asList(
                new ScheduleInterval(
                        createDateTime(YEKATERINBURG_TZ, MONDAY, 1, 21),
                        createDateTime(YEKATERINBURG_TZ, MONDAY, 4, 0),
                        1
                ),
                new ScheduleInterval(
                        createDateTime(YEKATERINBURG_TZ, TUESDAY, 1, 21),
                        createDateTime(YEKATERINBURG_TZ, FRIDAY, 4, 0),
                        1
                ),
                new ScheduleInterval(
                        createDateTime(YEKATERINBURG_TZ, MONDAY, 4, 0),
                        createDateTime(YEKATERINBURG_TZ, FRIDAY, 19, 43),
                        1
                )
        );
        Assert.assertThat(actual, Matchers.containsInAnyOrder(expected.toArray()));
    }

    @Test
    @DisplayName("Сдвиг вперед. Несколько дней следующей недели")
    public void testTzForwardSeveral() {
        // Вперед
        final ScheduleInterval schedule = new ScheduleInterval(
                createDateTime(SAMOA_TZ, TUESDAY, 5, 0),
                createDateTime(SAMOA_TZ, MONDAY, 0, 0),
                1
        );
        List<ScheduleInterval> actual = ScheduleUtils.convertTimezone(schedule, KIRITIMATI_TZ);

        List<ScheduleInterval> expected = Arrays.asList(
                new ScheduleInterval(
                        createDateTime(KIRITIMATI_TZ, MONDAY, 0, 0),
                        createDateTime(KIRITIMATI_TZ, TUESDAY, 1, 0),
                        1
                ),
                new ScheduleInterval(
                        createDateTime(KIRITIMATI_TZ, THURSDAY, 0, 0),
                        createDateTime(KIRITIMATI_TZ, SUNDAY, 1, 0),
                        1
                ),
                new ScheduleInterval(
                        createDateTime(KIRITIMATI_TZ, MONDAY, 6, 0),
                        createDateTime(KIRITIMATI_TZ, TUESDAY, 0, 0),
                        1
                ),
                new ScheduleInterval(
                        createDateTime(KIRITIMATI_TZ, WEDNESDAY, 6, 0),
                        createDateTime(KIRITIMATI_TZ, MONDAY, 0, 0),
                        1
                )
        );
        Assert.assertThat(actual, Matchers.containsInAnyOrder(expected.toArray()));

        // Назад с объединением
        actual = ScheduleUtils.convertTimezone(expected, SAMOA_TZ, true);
        Assert.assertEquals(1, actual.size());
        Assert.assertEquals(schedule, actual.get(0));

        // Назад без объединения
        actual = expected.stream()
                .map(e -> ScheduleUtils.convertTimezone(e, SAMOA_TZ))
                .flatMap(List::stream)
                .collect(Collectors.toList());

        expected = Arrays.asList(
                new ScheduleInterval(
                        createDateTime(SAMOA_TZ, TUESDAY, 5, 0),
                        createDateTime(SAMOA_TZ, SATURDAY, 23, 0),
                        1
                ),
                new ScheduleInterval(
                        createDateTime(SAMOA_TZ, TUESDAY, 23, 0),
                        createDateTime(SAMOA_TZ, SATURDAY, 0, 0),
                        1
                ),
                new ScheduleInterval(
                        createDateTime(SAMOA_TZ, SATURDAY, 23, 0),
                        createDateTime(SAMOA_TZ, MONDAY, 0, 0),
                        1
                ),
                new ScheduleInterval(
                        createDateTime(SAMOA_TZ, SUNDAY, 5, 0),
                        createDateTime(SAMOA_TZ, SUNDAY, 23, 0),
                        1
                )
        );
        Assert.assertThat(actual, Matchers.containsInAnyOrder(expected.toArray()));
    }

    @Test
    @DisplayName("Сдвиг назад. Несколько дней предыдущей недели")
    public void testTzBackwardSeveral() {
        // Назад
        final ScheduleInterval schedule = new ScheduleInterval(
                createDateTime(KIRITIMATI_TZ, MONDAY, 0, 21),
                createDateTime(KIRITIMATI_TZ, FRIDAY, 19, 43),
                1
        );
        List<ScheduleInterval> actual = ScheduleUtils.convertTimezone(schedule, SAMOA_TZ);

        List<ScheduleInterval> expected = Arrays.asList(
                new ScheduleInterval(
                        createDateTime(SAMOA_TZ, SATURDAY, 23, 21),
                        createDateTime(SAMOA_TZ, MONDAY, 0, 0),
                        1
                ),
                new ScheduleInterval(
                        createDateTime(SAMOA_TZ, MONDAY, 0, 0),
                        createDateTime(SAMOA_TZ, THURSDAY, 18, 43),
                        1
                ),
                new ScheduleInterval(
                        createDateTime(SAMOA_TZ, MONDAY, 23, 21),
                        createDateTime(SAMOA_TZ, THURSDAY, 0, 0),
                        1
                ),
                new ScheduleInterval(
                        createDateTime(SAMOA_TZ, SUNDAY, 0, 0),
                        createDateTime(SAMOA_TZ, SUNDAY, 18, 43),
                        1
                )
        );
        Assert.assertThat(actual, Matchers.containsInAnyOrder(expected.toArray()));

        // Вперед с объединением
        actual = ScheduleUtils.convertTimezone(expected, KIRITIMATI_TZ, true);
        Assert.assertEquals(1, actual.size());
        Assert.assertEquals(schedule, actual.get(0));

        // Вперед без объединений
        actual = expected.stream()
                .map(e -> ScheduleUtils.convertTimezone(e, KIRITIMATI_TZ))
                .flatMap(List::stream)
                .collect(Collectors.toList());

        expected = Arrays.asList(
                new ScheduleInterval(
                        createDateTime(KIRITIMATI_TZ, MONDAY, 0, 21),
                        createDateTime(KIRITIMATI_TZ, TUESDAY, 1, 0),
                        1
                ),
                new ScheduleInterval(
                        createDateTime(KIRITIMATI_TZ, TUESDAY, 1, 0),
                        createDateTime(KIRITIMATI_TZ, FRIDAY, 19, 43),
                        1
                ),
                new ScheduleInterval(
                        createDateTime(KIRITIMATI_TZ, WEDNESDAY, 0, 21),
                        createDateTime(KIRITIMATI_TZ, FRIDAY, 1, 0),
                        1
                ),
                new ScheduleInterval(
                        createDateTime(KIRITIMATI_TZ, MONDAY, 1, 0),
                        createDateTime(KIRITIMATI_TZ, MONDAY, 19, 43),
                        1
                )
        );
        Assert.assertThat(actual, Matchers.containsInAnyOrder(expected.toArray()));
    }

    @Test
    @DisplayName("Сдвиг цельного блока на неделю")
    public void testTzWhole() {
        // Вперед
        final ScheduleInterval schedule = new ScheduleInterval(
                createDateTime(LIBREVILLE_TZ, SUNDAY, 21, 0),
                createDateTime(LIBREVILLE_TZ, SUNDAY, 23, 0),
                1
        );
        List<ScheduleInterval> actual = ScheduleUtils.convertTimezone(schedule, YEKATERINBURG_TZ);

        final ScheduleInterval expected = new ScheduleInterval(
                createDateTime(YEKATERINBURG_TZ, MONDAY, 1, 0),
                createDateTime(YEKATERINBURG_TZ, MONDAY, 3, 0),
                1
        );
        Assert.assertEquals(1, actual.size());
        Assert.assertEquals(expected, actual.get(0));

        // Назад
        actual = ScheduleUtils.convertTimezone(actual.get(0), LIBREVILLE_TZ);

        Assert.assertEquals(1, actual.size());
        Assert.assertEquals(schedule, actual.get(0));
    }

    @Test
    @DisplayName("Сдвиг, чтобы конец выпал на полночь в рамках одной недели")
    public void testTzEndMidnightInWeek() {
        // Начало выпадет на середину дня
        ScheduleInterval schedule = new ScheduleInterval(
                createDateTime(LIBREVILLE_TZ, WEDNESDAY, 12, 0),
                createDateTime(LIBREVILLE_TZ, WEDNESDAY, 23, 0),
                1
        );

        List<ScheduleInterval> actual = ScheduleUtils.convertTimezone(schedule, ROME_TZ);

        ScheduleInterval expected = new ScheduleInterval(
                createDateTime(ROME_TZ, WEDNESDAY, 13, 0),
                createDateTime(ROME_TZ, THURSDAY, 0, 0),
                1
        );
        Assert.assertEquals(1, actual.size());
        Assert.assertEquals(expected, actual.get(0));

        // Начало выпадет на 00
        schedule = new ScheduleInterval(
                createDateTime(SAMOA_TZ, WEDNESDAY, 0, 0),
                createDateTime(SAMOA_TZ, THURSDAY, 0, 0),
                1
        );
        actual = ScheduleUtils.convertTimezone(schedule, TONGATAPU_TZ);

        expected = new ScheduleInterval(
                createDateTime(TONGATAPU_TZ, THURSDAY, 0, 0),
                createDateTime(TONGATAPU_TZ, FRIDAY, 0, 0),
                1
        );
        Assert.assertEquals(1, actual.size());
        Assert.assertEquals(expected, actual.get(0));
    }

    @Test
    @DisplayName("Сдвиг, чтобы конец выпал на полночь с залезанием на соседнюю неделю: чтобы разбить на несколько прямоугольников")
    public void testTzEndMidnightNextWeek() {
        // Начало выпадет на середину дня
        ScheduleInterval schedule = new ScheduleInterval(
                createDateTime(SAMOA_TZ, SATURDAY, 12, 0),
                createDateTime(SAMOA_TZ, SUNDAY, 23, 0),
                1
        );

        List<ScheduleInterval> actual = ScheduleUtils.convertTimezone(schedule, KIRITIMATI_TZ);

        List<ScheduleInterval> expected = Arrays.asList(
                new ScheduleInterval(
                        createDateTime(KIRITIMATI_TZ, SUNDAY, 13, 0),
                        createDateTime(KIRITIMATI_TZ, MONDAY, 0, 0),
                        1
                ),
                new ScheduleInterval(
                        createDateTime(KIRITIMATI_TZ, MONDAY, 13, 0),
                        createDateTime(KIRITIMATI_TZ, TUESDAY, 0, 0),
                        1
                )
        );
        Assert.assertThat(actual, Matchers.containsInAnyOrder(expected.toArray()));

        // Начало выпадет на 00
        schedule = new ScheduleInterval(
                createDateTime(SAMOA_TZ, SATURDAY, 0, 0),
                createDateTime(SAMOA_TZ, MONDAY, 0, 0),
                1
        );
        actual = ScheduleUtils.convertTimezone(schedule, TONGATAPU_TZ);

        expected = Arrays.asList(
                new ScheduleInterval(
                        createDateTime(KIRITIMATI_TZ, SUNDAY, 0, 0),
                        createDateTime(KIRITIMATI_TZ, MONDAY, 0, 0),
                        1
                ),
                new ScheduleInterval(
                        createDateTime(KIRITIMATI_TZ, MONDAY, 0, 0),
                        createDateTime(KIRITIMATI_TZ, TUESDAY, 0, 0),
                        1
                )
        );
        Assert.assertThat(actual, Matchers.containsInAnyOrder(expected.toArray()));
    }

    @Test
    @DisplayName("Сдвиг по горизонтали с переходом на след. неделю")
    public void testTzHorizontal() {
        // Вперед
        final ScheduleInterval schedule = new ScheduleInterval(
                createDateTime(SAMOA_TZ, FRIDAY, 12, 0),
                createDateTime(SAMOA_TZ, SUNDAY, 15, 0),
                1
        );

        final List<ScheduleInterval> actual = ScheduleUtils.convertTimezone(schedule, TONGATAPU_TZ);

        final List<ScheduleInterval> expected = Arrays.asList(
                new ScheduleInterval(
                        createDateTime(TONGATAPU_TZ, SATURDAY, 12, 0),
                        createDateTime(TONGATAPU_TZ, SUNDAY, 15, 0),
                        1
                ),
                new ScheduleInterval(
                        createDateTime(TONGATAPU_TZ, MONDAY, 12, 0),
                        createDateTime(TONGATAPU_TZ, MONDAY, 15, 0),
                        1
                )
        );
        Assert.assertThat(actual, Matchers.containsInAnyOrder(expected.toArray()));
    }

    @Test
    @DisplayName("Полоска на всю ширину недели")
    public void testTzFullWidth() {
        // Сдвиг в рамках одной недели
        final ScheduleInterval schedule = new ScheduleInterval(
                createDateTime(BOGOTA_TZ, MONDAY, 12, 0),
                createDateTime(BOGOTA_TZ, SUNDAY, 15, 0),
                1
        );

        List<ScheduleInterval> actual = ScheduleUtils.convertTimezone(schedule, MOSCOW_TZ);

        final ScheduleInterval expectedInWeek = new ScheduleInterval(
                createDateTime(MOSCOW_TZ, MONDAY, 20, 0),
                createDateTime(MOSCOW_TZ, SUNDAY, 23, 0),
                1
        );
        Assert.assertEquals(1, actual.size());
        Assert.assertEquals(expectedInWeek, actual.get(0));

        // Конец попадет на полночь восркесенья
        actual = ScheduleUtils.convertTimezone(schedule, YEREVAN_TZ);

        final ScheduleInterval expectedInWeek2 = new ScheduleInterval(
                createDateTime(YEREVAN_TZ, MONDAY, 21, 0),
                createDateTime(YEREVAN_TZ, MONDAY, 0, 0),
                1
        );
        Assert.assertEquals(1, actual.size());
        Assert.assertEquals(expectedInWeek2, actual.get(0));

        // Переход на след. неделю. Параллельный перенос
        final ScheduleInterval schedule2 = new ScheduleInterval(
                createDateTime(BOGOTA_TZ, MONDAY, 20, 0),
                createDateTime(BOGOTA_TZ, SUNDAY, 23, 0),
                1
        );
        actual = ScheduleUtils.convertTimezone(schedule2, MOSCOW_TZ);
        List<ScheduleInterval> expected = Collections.singletonList(
                new ScheduleInterval(
                        createDateTime(MOSCOW_TZ, MONDAY, 4, 0),
                        createDateTime(MOSCOW_TZ, SUNDAY, 7, 0),
                        1
                )
        );
        Assert.assertThat(actual, Matchers.containsInAnyOrder(expected.toArray()));

        // Переход на след. неделю. пересечение с полночью
        final ScheduleInterval schedule3 = new ScheduleInterval(
                createDateTime(LIBREVILLE_TZ, MONDAY, 20, 0),
                createDateTime(LIBREVILLE_TZ, SUNDAY, 23, 0),
                1
        );
        actual = ScheduleUtils.convertTimezone(schedule3, MOSCOW_TZ);
        expected = Arrays.asList(
                new ScheduleInterval(
                        createDateTime(MOSCOW_TZ, MONDAY, 22, 0),
                        createDateTime(MOSCOW_TZ, MONDAY, 0, 0),
                        1
                ),
                new ScheduleInterval(
                        createDateTime(MOSCOW_TZ, MONDAY, 0, 0),
                        createDateTime(MOSCOW_TZ, SUNDAY, 1, 0),
                        1
                )
        );
        Assert.assertThat(actual, Matchers.containsInAnyOrder(expected.toArray()));
    }

    @Test
    @DisplayName("Полоска на всю высоту недели")
    public void testTzFullHeight() {
        // Сдвиг ровно на день вперед
        ScheduleInterval schedule = new ScheduleInterval(
                createDateTime(SAMOA_TZ, THURSDAY, 0, 0),
                createDateTime(SAMOA_TZ, SUNDAY, 0, 0),
                1
        );
        List<ScheduleInterval> actual = ScheduleUtils.convertTimezone(schedule, TONGATAPU_TZ);

        final ScheduleInterval expectedInWeek = new ScheduleInterval(
                createDateTime(TONGATAPU_TZ, FRIDAY, 0, 0),
                createDateTime(TONGATAPU_TZ, MONDAY, 0, 0),
                1
        );
        Assert.assertEquals(1, actual.size());
        Assert.assertEquals(expectedInWeek, actual.get(0));

        // Сдвиг в рамках одной недели. Параллельный перенос
        schedule = new ScheduleInterval(
                createDateTime(BOGOTA_TZ, THURSDAY, 0, 0),
                createDateTime(BOGOTA_TZ, SUNDAY, 0, 0),
                1
        );
        actual = ScheduleUtils.convertTimezone(schedule, MOSCOW_TZ);
        List<ScheduleInterval> expected = Arrays.asList(
                new ScheduleInterval(
                        createDateTime(MOSCOW_TZ, THURSDAY, 8, 0),
                        createDateTime(MOSCOW_TZ, SUNDAY, 0, 0),
                        1
                ),
                new ScheduleInterval(
                        createDateTime(MOSCOW_TZ, FRIDAY, 0, 0),
                        createDateTime(MOSCOW_TZ, SUNDAY, 8, 0),
                        1
                )
        );
        Assert.assertThat(actual, Matchers.containsInAnyOrder(expected.toArray()));

        // Переход на след. неделю. пересечение с полночью
        schedule = new ScheduleInterval(
                createDateTime(LIBREVILLE_TZ, FRIDAY, 0, 0),
                createDateTime(LIBREVILLE_TZ, MONDAY, 0, 0),
                1
        );
        actual = ScheduleUtils.convertTimezone(schedule, MOSCOW_TZ);
        expected = Arrays.asList(
                new ScheduleInterval(
                        createDateTime(MOSCOW_TZ, FRIDAY, 2, 0),
                        createDateTime(MOSCOW_TZ, MONDAY, 0, 0),
                        1
                ),
                new ScheduleInterval(
                        createDateTime(MOSCOW_TZ, MONDAY, 0, 0),
                        createDateTime(MOSCOW_TZ, MONDAY, 2, 0),
                        1
                ),
                new ScheduleInterval(
                        createDateTime(MOSCOW_TZ, SATURDAY, 0, 0),
                        createDateTime(MOSCOW_TZ, SUNDAY, 2, 0),
                        1
                )
        );
        Assert.assertThat(actual, Matchers.containsInAnyOrder(expected.toArray()));
    }

    @Test
    @DisplayName("Сдвиг на таймзону, не кратную часу")
    public void testTzNotWhole() {
        // Вперед
        final ScheduleInterval schedule = new ScheduleInterval(
                createDateTime(MOSCOW_TZ, THURSDAY, 5, 0),
                createDateTime(MOSCOW_TZ, FRIDAY, 12, 0),
                1
        );
        final List<ScheduleInterval> actual = ScheduleUtils.convertTimezone(Collections.singletonList(schedule), TEHRAN_TZ, false);

        final ScheduleInterval expected = new ScheduleInterval(
                createDateTime(MOSCOW_TZ, THURSDAY, 6, 30),
                createDateTime(MOSCOW_TZ, FRIDAY, 13, 30),
                1
        );
        Assert.assertEquals(1, actual.size());
        Assert.assertEquals(expected, actual.get(0));
    }

    @Test
    @DisplayName("Конвертация без номера интервала")
    public void testWithoutIntervalNumber() {
        // Вперед
        final ScheduleInterval schedule = new ScheduleInterval(
                createDateTime(MOSCOW_TZ, THURSDAY, 5, 0),
                createDateTime(MOSCOW_TZ, FRIDAY, 12, 0)
        );
        final List<ScheduleInterval> actual = ScheduleUtils.convertTimezone(Collections.singletonList(schedule), TEHRAN_TZ, true);

        final ScheduleInterval expected = new ScheduleInterval(
                createDateTime(MOSCOW_TZ, THURSDAY, 6, 30),
                createDateTime(MOSCOW_TZ, FRIDAY, 13, 30)
        );
        Assert.assertEquals(1, actual.size());
        Assert.assertEquals(expected, actual.get(0));
    }
}
