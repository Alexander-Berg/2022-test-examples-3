package ru.yandex.market.pers.notify.ems.generator;

import org.junit.jupiter.api.Test;
import ru.yandex.market.pers.notify.model.event.NotificationEventSource;

import java.util.Calendar;
import java.util.Date;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Ivan Anisimov
 *         valter@yandex-team.ru
 *         28.04.16
 */
public class NotificationEventSourceGeneratorTest {
	@Test
	public void getScheduleTime() throws Exception {
		NotificationEventSourceGenerator generator = new NotificationEventSourceGenerator() {
			@Override
            public Stream<NotificationEventSource> generate() {
                return Stream.empty();
            }
        };
		
		testFirstSchedule(generator);
		testSecondSchedule(generator);
		testThirdSchedule(generator);
	}
	
	private void testFirstSchedule(NotificationEventSourceGenerator generator) throws Exception {
		generator.setSendCronSchedule("0 0 18-21 ? * MON-FRI");

		Calendar calendar = Calendar.getInstance();
		calendar.setTime(new Date());
		calendar.add(Calendar.WEEK_OF_YEAR, 1);

		calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
		calendar.set(Calendar.HOUR_OF_DAY, 17);
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);
		calendar.set(Calendar.MILLISECOND, 0);
		Date actual = generator.getScheduleTime(calendar.getTime());
		calendar.set(Calendar.HOUR_OF_DAY, 18);
		Date expected = calendar.getTime();
		assertEquals(expected, actual);

		calendar.set(Calendar.HOUR_OF_DAY, 19);
		actual = generator.getScheduleTime(calendar.getTime());
		calendar.set(Calendar.HOUR_OF_DAY, 20);
		expected = calendar.getTime();
		assertEquals(expected, actual);

		calendar.set(Calendar.HOUR_OF_DAY, 20);
		calendar.set(Calendar.MINUTE, 59);
		calendar.set(Calendar.SECOND, 59);
		actual = generator.getScheduleTime(calendar.getTime());
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);
		calendar.set(Calendar.HOUR_OF_DAY, 21);
		expected = calendar.getTime();
		assertEquals(expected, actual);

		calendar.set(Calendar.HOUR_OF_DAY, 21);
		calendar.set(Calendar.MINUTE, 1);
		actual = generator.getScheduleTime(calendar.getTime());
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.DAY_OF_WEEK, Calendar.TUESDAY);
		calendar.set(Calendar.HOUR_OF_DAY, 18);
		expected = calendar.getTime();
		assertEquals(expected, actual);

		calendar.set(Calendar.HOUR_OF_DAY, 21);
		calendar.set(Calendar.MINUTE, 1);
		calendar.set(Calendar.DAY_OF_WEEK, Calendar.FRIDAY);
		actual = generator.getScheduleTime(calendar.getTime());
		calendar.add(Calendar.WEEK_OF_YEAR, 1);
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
		calendar.set(Calendar.HOUR_OF_DAY, 18);
		expected = calendar.getTime();
		assertEquals(expected, actual);
	}

	private void testSecondSchedule(NotificationEventSourceGenerator generator) throws Exception {
		generator.setSendCronSchedule("0 0 7 ? * *");

		Calendar calendar = Calendar.getInstance();
		calendar.setTime(new Date());
		calendar.add(Calendar.WEEK_OF_YEAR, 1);

		calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
		calendar.set(Calendar.HOUR_OF_DAY, 17);
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);
		calendar.set(Calendar.MILLISECOND, 0);
		Date actual = generator.getScheduleTime(calendar.getTime());
		calendar.set(Calendar.HOUR_OF_DAY, 7);
		calendar.add(Calendar.DAY_OF_YEAR, 1);
		Date expected = calendar.getTime();
		assertEquals(expected, actual);

		calendar.set(Calendar.HOUR_OF_DAY, 6);
		actual = generator.getScheduleTime(calendar.getTime());
		calendar.set(Calendar.HOUR_OF_DAY, 7);
		expected = calendar.getTime();
		assertEquals(expected, actual);
	}

	private void testThirdSchedule(NotificationEventSourceGenerator generator) throws Exception {
		generator.setSendCronSchedule("0 0 13-15 ? * *");

		Calendar calendar = Calendar.getInstance();
		calendar.setTime(new Date());
		calendar.add(Calendar.WEEK_OF_YEAR, 1);

		calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
		calendar.set(Calendar.HOUR_OF_DAY, 17);
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);
		calendar.set(Calendar.MILLISECOND, 0);
		Date actual = generator.getScheduleTime(calendar.getTime());
		calendar.set(Calendar.HOUR_OF_DAY, 13);
		calendar.add(Calendar.DAY_OF_YEAR, 1);
		Date expected = calendar.getTime();
		assertEquals(expected, actual);

		calendar.set(Calendar.HOUR_OF_DAY, 13);
		calendar.set(Calendar.MINUTE, 30);
		actual = generator.getScheduleTime(calendar.getTime());
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.HOUR_OF_DAY, 14);
		expected = calendar.getTime();
		assertEquals(expected, actual);
	}
}
