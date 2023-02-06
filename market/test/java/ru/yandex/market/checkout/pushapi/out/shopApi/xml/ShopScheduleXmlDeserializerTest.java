package ru.yandex.market.checkout.pushapi.out.shopApi.xml;

import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.pushapi.model.WorkingTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ShopScheduleXmlDeserializerTest {

    @Test
    public void shouldDeserialize() {
        ShopScheduleXmlDeserializer deserializer = new ShopScheduleXmlDeserializer();
        List<WorkingTime> workingTimeList = deserializer.deserializeXmlSchedule("<WorkingTime>\n" +
                "    <WorkingDaysFrom>1</WorkingDaysFrom>\n" +
                "    <WorkingDaysTill>1</WorkingDaysTill>\n" +
                "    <WorkingHoursFrom>09:00</WorkingHoursFrom>\n" +
                "    <WorkingHoursTill>20:00</WorkingHoursTill>\n" +
                "</WorkingTime><WorkingTime>\n" +
                "<WorkingDaysFrom>2</WorkingDaysFrom>\n" +
                "<WorkingDaysTill>2</WorkingDaysTill>\n" +
                "<WorkingHoursFrom>09:00</WorkingHoursFrom>\n" +
                "<WorkingHoursTill>20:00</WorkingHoursTill>\n" +
                "</WorkingTime><WorkingTime>\n" +
                "<WorkingDaysFrom>3</WorkingDaysFrom>\n" +
                "<WorkingDaysTill>3</WorkingDaysTill>\n" +
                "<WorkingHoursFrom>09:00</WorkingHoursFrom>\n" +
                "<WorkingHoursTill>20:00</WorkingHoursTill>\n" +
                "</WorkingTime><WorkingTime>\n" +
                "<WorkingDaysFrom>4</WorkingDaysFrom>\n" +
                "<WorkingDaysTill>4</WorkingDaysTill>\n" +
                "<WorkingHoursFrom>09:00</WorkingHoursFrom>\n" +
                "<WorkingHoursTill>20:00</WorkingHoursTill>\n" +
                "</WorkingTime><WorkingTime>\n" +
                "<WorkingDaysFrom>5</WorkingDaysFrom>\n" +
                "<WorkingDaysTill>5</WorkingDaysTill>\n" +
                "<WorkingHoursFrom>09:00</WorkingHoursFrom>\n" +
                "<WorkingHoursTill>20:00</WorkingHoursTill>\n" +
                "</WorkingTime><WorkingTime>\n" +
                "<WorkingDaysFrom>6</WorkingDaysFrom>\n" +
                "<WorkingDaysTill>6</WorkingDaysTill>\n" +
                "<WorkingHoursFrom>09:00</WorkingHoursFrom>\n" +
                "<WorkingHoursTill>19:00</WorkingHoursTill>\n" +
                "</WorkingTime><WorkingTime>\n" +
                "<WorkingDaysFrom>7</WorkingDaysFrom>\n" +
                "<WorkingDaysTill>7</WorkingDaysTill>\n" +
                "<WorkingHoursFrom>09:00</WorkingHoursFrom>\n" +
                "<WorkingHoursTill>19:00</WorkingHoursTill>\n" +
                "</WorkingTime>");
        assertNotNull(workingTimeList);
        assertEquals(7, workingTimeList.size());
        assertTrue(workingTimeList.stream().filter(w -> w.getWorkingDaysFrom() == 6).anyMatch(workingTime ->
                workingTime.getWorkingHoursTill().equals(LocalTime.of(19, 0))));
        assertTrue(workingTimeList.stream().filter(w -> w.getWorkingDaysFrom() == 1).anyMatch(workingTime ->
                workingTime.getWorkingHoursFrom().equals(LocalTime.of(9, 0))));


    }

}
