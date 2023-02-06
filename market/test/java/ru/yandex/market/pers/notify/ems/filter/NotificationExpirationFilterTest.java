package ru.yandex.market.pers.notify.ems.filter;

import org.junit.jupiter.api.Test;
import ru.yandex.market.pers.notify.ems.filter.config.NotificationExpirationFilterConfig;
import ru.yandex.market.pers.notify.model.NotificationSubtype;
import ru.yandex.market.pers.notify.ems.event.NotificationEvent;
import ru.yandex.market.pers.notify.test.MarketMailerMockedDbTest;

import java.util.Calendar;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Ivan Anisimov
 *         valter@yandex-team.ru
 *         29.03.16
 */
public class NotificationExpirationFilterTest extends MarketMailerMockedDbTest {
	@Test
	public void test() {
		NotificationExpirationFilter filter = new NotificationExpirationFilter();
		
		NotificationEvent event = new NotificationEvent();
		event.setNotificationSubtype(NotificationSubtype.ADVERTISING_1);

		Calendar calendar = Calendar.getInstance();
		calendar.setTime(new Date());
		
		event.setSendTime(calendar.getTime());
		assertTrue(filter.canProcess(event, new NotificationExpirationFilterConfig()));

		calendar.add(Calendar.DAY_OF_YEAR, -1);
		event.setSendTime(calendar.getTime());
		assertFalse(filter.canProcess(event, new NotificationExpirationFilterConfig()));

		calendar.add(Calendar.MINUTE, 2);
		event.setSendTime(calendar.getTime());
		assertTrue(filter.canProcess(event, new NotificationExpirationFilterConfig()));
	}
}
