package ru.yandex.market.checkout.checkouter.delivery;

import java.text.SimpleDateFormat;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.common.util.date.DateUtil;

public class RawDeliveryIntervalTest {

    @Test
    public void testIntervalKeysOrdering() {
        final RawDeliveryIntervalsCollection intervals = new RawDeliveryIntervalsCollection();
        final List<String> data = Arrays.asList("2018-07-13", "2018-02-11", "2018-04-16", "2018-04-17", "2018-04-12");
        final List<String> expectedOrder = data.stream().sorted().collect(Collectors.toList());
        final SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");

        for (String item : data) {
            intervals.add(new RawDeliveryInterval(DateUtil.convertToDate(item + " 00:00:00")));
        }

        int cter = 0;
        for (Iterator<Date> i = intervals.getCollection().keySet().iterator(); i.hasNext(); ) {
            Assertions.assertEquals(formatter.format(i.next()), expectedOrder.get(cter++));
        }
    }

    @Test
    public void testIntervalSetOrdering() {
        final Date baseDate = DateUtil.convertToDate("2018-06-23 00:00:00");
        final DateTimeFormatter tmf = DateTimeFormatter.ISO_TIME;

        final RawDeliveryInterval dateA = new RawDeliveryInterval(
                baseDate, LocalTime.parse("11:00", tmf), LocalTime.parse("16:00", tmf)
        );

        final RawDeliveryInterval dateB = new RawDeliveryInterval(
                baseDate, LocalTime.parse("15:00", tmf), LocalTime.parse("16:00", tmf)
        );

        final RawDeliveryInterval dateC = new RawDeliveryInterval(
                baseDate, LocalTime.parse("15:00", tmf), LocalTime.parse("20:00", tmf)
        );

        Assertions.assertEquals(-1, dateA.compareTo(dateB));
        Assertions.assertEquals(-1, dateA.compareTo(dateC));
        Assertions.assertEquals(-1, dateB.compareTo(dateC));
        Assertions.assertEquals(1, dateC.compareTo(dateA));
    }
}
