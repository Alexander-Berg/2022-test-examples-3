package ru.yandex.calendar.util.dates;

import org.joda.time.LocalTime;
import org.junit.Ignore;
import org.junit.Test;

import ru.yandex.calendar.util.dates.HumanReadableTimeParser.Result;
import ru.yandex.misc.test.Assert;

/**
 * @author Stepan Koltsov
 */
public class HumanReadableTimeParserTest {

    private static void assertStartTime(String input, String title, LocalTime startTime) {
        Result r = HumanReadableTimeParser.parse(input);
        Assert.assertTrue("failed to parse \"" + input + "\"", r.gotSomething());
        Assert.A.equals(title, r.getRest(), "expected title \"" + title + "\" from input \"" + input + "\"");
        Assert.A.equals(startTime, r.getStartTime());
    }

    private static void assertNone(String input) {
        Assert.assertFalse(HumanReadableTimeParser.parse(input).gotSomething());
    }

    @Test
    public void none() {
        assertNone("Сфоткаться ф (или посм) 2шт 3*4");
        assertNone("Гуляю");
        assertNone("Мне в сутках не хватает часов");
        assertNone("Ничего в 25:00");
        assertNone("Позвонить в Яндекс +74957397000");
        assertNone("2 раза погулять");
    }


    @Test
    public void simple() {
        assertStartTime("Кофе, 1 ночи", "Кофе", new LocalTime(1, 0, 0));
        assertStartTime("Кофе, 12 часов", "Кофе", new LocalTime(12, 0, 0));
        assertStartTime("Кофе, 9 утра", "Кофе", new LocalTime(9, 0, 0));
        assertStartTime("Кофе 10 ДП", "Кофе", new LocalTime(10, 0, 0));
        assertStartTime("Кофе 11 дп", "Кофе", new LocalTime(11, 0, 0));
        assertStartTime("Кофе 6 am", "Кофе", new LocalTime(6, 0, 0));
        assertStartTime("Кофе 6 a.m.", "Кофе", new LocalTime(6, 0, 0));
        assertStartTime("Кофе,  6 AM", "Кофе", new LocalTime(6, 0, 0));

        assertStartTime("Обед в 2 дня", "Обед", new LocalTime(14, 0, 0));

        assertStartTime("Ужин 6 вечера", "Ужин", new LocalTime(18, 0, 0));
        assertStartTime("Ужин 6 пп", "Ужин", new LocalTime(18, 0, 0));
        assertStartTime("Ужин 6 pm", "Ужин", new LocalTime(18, 0, 0));
        assertStartTime("Ужин 6 p.m.", "Ужин", new LocalTime(18, 0, 0));
        assertStartTime("Ужин 6:17 p.m.", "Ужин", new LocalTime(18, 17, 0));
        assertStartTime("Ужин 6 PM", "Ужин", new LocalTime(18, 0, 0));
        assertStartTime("Ужин 6 часов вечера", "Ужин", new LocalTime(18, 0, 0));
        assertStartTime("Ужин в 18:30", "Ужин", new LocalTime(18, 30, 0));
        assertStartTime("Ужин 18:30", "Ужин", new LocalTime(18, 30, 0));
        assertStartTime("Ужин 18-00", "Ужин", new LocalTime(18, 0, 0));
        assertStartTime("Ужин 18.00", "Ужин", new LocalTime(18, 0, 0));

        assertStartTime("18:30 Ужин", "Ужин", new LocalTime(18, 30, 0));
    }

    private void assertAllDay(String input, String rest) {
        Result result = HumanReadableTimeParser.parse(input);
        Assert.A.isTrue(result.isAllDay(), "must be all day: " + input);
        Assert.A.equals(rest, result.getRest());
    }

    @Test
    public void allDay() {
        assertAllDay("ничего не делаю весь день", "ничего не делаю");
        assertAllDay("Весь день буду работать", "буду работать");
        assertNone("весь деньга хачу потратить");
    }

    @Test
    @Ignore
    public void notYet() {
        assertStartTime("6 часов вечера ужин", "ужин", new LocalTime(18, 0, 0));
    }

} //~
