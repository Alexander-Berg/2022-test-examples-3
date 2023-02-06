package ru.yandex.market.checkout.checkouter.order.shop.schedule;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.stream.Stream;

import com.google.common.collect.Lists;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.checkout.checkouter.shop.ScheduleLine;
import ru.yandex.market.checkout.checkouter.shop.ShopScheduleSerializationHandler;
import ru.yandex.market.checkout.checkouter.shop.ShopScheduleUtils;

/**
 * Created by tesseract on 11.09.14.
 */
public class ScheduleServiceBeanTest {

    private Collection<ScheduleLine> schedule;
    private int duration;
    private String source;
    private String expected;

    public static Stream<Arguments> parameterizedTestData() {

        return Lists.newArrayList(
                new Object[]{"2014-09-08 00:00", "2014-09-08 02:01", 61, "1:60-240", "текущее время до начала " +
                        "интервала"},
                new Object[]{"2014-09-08 01:00", "2014-09-08 02:01", 61, "1:60-240", "текущее время является началом " +
                        "интервала"},
                new Object[]{"2014-09-08 02:00", "2014-09-08 03:01", 61, "1:60-240", "текущее время попадает в " +
                        "середину интервала"},
                new Object[]{"2014-09-08 04:00", "2014-09-15 02:01", 61, "1:60-240", "текущее время является концом " +
                        "интервала"},
                new Object[]{"2014-09-08 05:00", "2014-09-15 02:01", 61, "1:60-240", "текущее время после интервала"},
                new Object[]{"2014-09-08 00:00", "2014-09-08 03:30", 90, "1:60-120,1:180-240", "время не укладывается" +
                        " в 1 интервал дня"},
                new Object[]{"2014-09-08 00:00", "2014-09-08 02:00", 60, "1:60-120,1:180-240", "время окончания " +
                        "приходится на конец интервала"},
                new Object[]{"2014-09-08 10:00", "2014-09-15 02:00", 60, "1:60-120,1:180-240", "время старта после " +
                        "окончания второго интервала"},
                new Object[]{"2014-12-31 00:00", "2015-01-05 02:00", 60, "1:60-240", "переход года"},
                new Object[]{"2014-09-08 00:00", "2014-09-14 02:00", 60, "7:60-240", "расписание заданное на " +
                        "воскресение"},
                new Object[]{"2014-09-26 16:30", "2014-09-26 21:15", 75, "5:870-885,5:1200-1440", "время старта между" +
                        " интервалами (кейс от Хромова Кирилла)"},
                new Object[]{"2014-09-26 15:24", "2014-09-26 21:00", 75, "5:930-945,5:1200-1440", "время старта до " +
                        "первого интервалами (кейс от Хромова Кирилла)"},
                new Object[]{"2014-09-29 16:07", "2014-09-29 21:00", 75, "1:1080-1095,1:1200-1270", "время старта до " +
                        "первого интервалами (кейс от Хромова Кирилла)"}
        ).stream().map(Arguments::of);
    }

    @ParameterizedTest
    @MethodSource("parameterizedTestData")
    public void simple(String source, String expected, int duration, String schedule, String title)
            throws Exception {
        ShopScheduleSerializationHandler shopScheduleSerializationHandler = new ShopScheduleSerializationHandler();
        this.schedule = shopScheduleSerializationHandler.deserialize(schedule);
        this.duration = duration;
        this.source = source;
        this.expected = expected;

        // настройка системы
        Date date = toDate(this.source);

        // вызов системы
        Date resultDate = ShopScheduleUtils.calcExpirationTime(this.schedule, date, this.duration);

        // проверка утверждений
        assertTime(toDate(this.expected), resultDate);
    }

    private Date toDate(String source) throws ParseException {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm").parse(source);
    }

    private void assertTime(Date expected, Date actual) {
        Assertions.assertEquals(expected, actual, "Expected and actual date does not match");
    }
}
