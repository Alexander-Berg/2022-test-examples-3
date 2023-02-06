package ru.yandex.market.pers.notify.push.generator;

import freemarker.template.Configuration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.yandex.common.util.collections.Pair;
import ru.yandex.market.pers.notify.ems.persistence.NotificationEventService;
import ru.yandex.market.pers.notify.mail.generator.UserWithPayload;
import ru.yandex.market.pers.notify.model.event.NotificationEventSource;
import ru.yandex.market.pers.notify.push.PushReplacingConsumer;
import ru.yandex.market.pers.notify.templates.FTLoaderFactory;
import ru.yandex.market.pers.notify.test.MarketMailerMockedDbTest;

import java.util.Date;
import java.util.Objects;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Ivan Anisimov
 *         valter@yandex-team.ru
 *         28.12.15
 */
public class CarterPushGeneratorTest extends MarketMailerMockedDbTest {
	@Autowired
	private CarterPushGenerator carterPushGenerator;
	@Autowired
    private PushReplacingConsumer pushReplacingConsumer;
    @Autowired
	private NotificationEventService notificationEventService;
	
	@Autowired
	private Configuration configuration;
	@Autowired
	private FTLoaderFactory ftLoaderFactory;
	
	@BeforeEach
	public void init() {
		configuration.setTemplateLoader(ftLoaderFactory.getNewTemplateLoader());
	}

	@Test
	public void getUsers() {
		Pair<Date, Date> interval = carterPushGenerator.getInterval();
        Stream<UserWithPayload> users = carterPushGenerator.userReceiver()
            .getUsers(interval.getFirst(), interval.getSecond());
        assertNotNull(users);
    }

	@Test
	public void getInterval() {
		Pair<Date, Date> interval = carterPushGenerator.getInterval();
		assertNotNull(interval);
	}

	@Test
	public void getNotAcceptableUsers() {
		Pair<Date, Date> interval = carterPushGenerator.getInterval();
        Stream<UserWithPayload> users = carterPushGenerator.userReceiver()
            .getUsers(interval.getFirst(), interval.getSecond());
        users = carterPushGenerator.filter(users);
        assertNotNull(users);
	}
	
	@Test
	public void testAll() {
		//pusherService.register(348214245L, "42b604536479bb464189d72eb83a8655", MobilePlatform.ANDROID, "APA91bF3-M1WGl9jMwScoGYN3MgxPGaVPiRFrg_emCFGCk-g-U2Hov_v_tEeqTL88E84l9NsRFtdu4doo9m8Esl4sexLpffJKDo-2_xQHHFPWhrqGKcHbYfhvIEPUW2ErGUj5tuIYJdH");
		//pusherService.push(null, "42b604536479bb464189d72eb83a8655", "Привет!");

		Stream<NotificationEventSource> notifications = carterPushGenerator.generate();
		notifications.forEach(notification -> {
			System.out.println(notification);
			if (!Objects.equals(notification.getUid(), 23427062L) && !Objects.equals(notification.getUid(), 348214245L)
				&& !Objects.equals("b244a9db42f25312bce2bb90f0ebd139", notification.getUuid())
				&& !Objects.equals("42b604536479bb464189d72eb83a8655", notification.getUuid())) {
				return;
			}
            System.out.println(pushReplacingConsumer.processEvent(notificationEventService.addEvent(notification)));
		});

		notifications = carterPushGenerator.generate();
		notifications.forEach(notification -> {
			System.out.println(notification);
			if (!Objects.equals(notification.getUid(), 23427062L) && !Objects.equals(notification.getUid(), 348214245L) 
					&& !Objects.equals("b244a9db42f25312bce2bb90f0ebd139", notification.getUuid())
					&& !Objects.equals("42b604536479bb464189d72eb83a8655", notification.getUuid())) {
				return;
			}
			fail(notification.toString());
		});
		
	}
}
