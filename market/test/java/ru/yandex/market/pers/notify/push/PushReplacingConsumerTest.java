package ru.yandex.market.pers.notify.push;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;

import javax.annotation.PostConstruct;

import freemarker.template.Configuration;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.changerequest.ChangeRequest;
import ru.yandex.market.checkout.checkouter.order.changerequest.ChangeRequestStatus;
import ru.yandex.market.checkout.checkouter.order.changerequest.ChangeRequestType;
import ru.yandex.market.pers.notify.PushConsumerTest;
import ru.yandex.market.pers.notify.ems.event.NotificationEvent;
import ru.yandex.market.pers.notify.ems.persistence.NotificationEventService;
import ru.yandex.market.pers.notify.model.event.NotificationEventDataName;
import ru.yandex.market.pers.notify.model.event.NotificationEventSource;
import ru.yandex.market.pers.notify.model.event.NotificationEventStatus;
import ru.yandex.market.pers.notify.model.push.MobileAppInfo;
import ru.yandex.market.pers.notify.model.push.MobilePlatform;
import ru.yandex.market.pers.notify.model.push.PushTemplateType;
import ru.yandex.market.pers.notify.templates.FTLoaderFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static ru.yandex.market.pers.notify.mock.MarketMailerMockFactory.generateChangeRequest;

/**
 * @author Ivan Anisimov
 *         valter@yandex-team.ru
 *         25.12.15
 */
@Disabled
public class PushReplacingConsumerTest extends PushConsumerTest {

	@Autowired
	private Configuration configuration;

	@Autowired
	private FTLoaderFactory ftLoaderFactory;

	@PostConstruct
	public void afterPropertiesSet() {
		configuration.setTemplateLoader(ftLoaderFactory.getNewTemplateLoader());
	}

	@Autowired
	private NotificationEventService notificationEventService;
    
    @Autowired
    private MobileAppInfoDAO mobileAppInfoDAO;

	@Test
	public void testReplace() {
        mobileAppInfoDAO.add(new MobileAppInfo(1L, "null", "app", "none", MobilePlatform.ANY, false));
		NotificationEvent event = notificationEventService.addEvent(NotificationEventSource
				.pushFromUid(1L, PushTemplateType.LEAVE_GRADE_SHOP)
				.addTemplateParam(NotificationEventDataName.SHOP_ID, "234")
				.build());
		assertNotNull(event);
        assertEquals(NotificationEventStatus.SENT, pushReplacingConsumer.processEvent(event).getStatus());
    }

    @Test
    public void testReplaceCluster() {
        mobileAppInfoDAO.add(new MobileAppInfo(1L, "null", "app", "none", MobilePlatform.ANY, false));
        NotificationEvent event = notificationEventService.addEvent(NotificationEventSource
                .pushFromUid(1L, PushTemplateType.HOT_CATEGORY_OFFER)
                .addTemplateParam(NotificationEventDataName.CLUSTER_ID, "567")
                .addTemplateParam(NotificationEventDataName.DISCOUNT, "100")
                .build());
        assertNotNull(event);
        assertEquals(NotificationEventStatus.SENT, pushReplacingConsumer.processEvent(event).getStatus());
    }

	@Test
	public void testCancellationPushOutdated() {
		mobileAppInfoDAO.add(new MobileAppInfo(1L, "null", "app", "none", MobilePlatform.ANY, false));
		Order order = createOrder();
		setupCheckouterClient(order);
		Instant threeWeeksOldInstant = Instant.now().minus(28, ChronoUnit.DAYS);
		ChangeRequest changeRequest = generateChangeRequest(
				ChangeRequestType.CANCELLATION,
				ChangeRequestStatus.NEW,
				threeWeeksOldInstant);

		order.setChangeRequests(Collections.singletonList(changeRequest));

		NotificationEventStatus status = createCancelledEventAndProcess(PushTemplateType.ORDER_STATUS_CANCELLED);

		assertEquals(NotificationEventStatus.EXPIRED, status);
	}

	@Test
	public void testCancellationPush() {
		mobileAppInfoDAO.add(new MobileAppInfo(1L, "null", "app", "none", MobilePlatform.ANY, false));
		Order order = createOrder();
		setupCheckouterClient(order);
		ChangeRequest changeRequest = generateChangeRequest(
				ChangeRequestType.CANCELLATION,
				ChangeRequestStatus.NEW,
				Instant.now());

		order.setChangeRequests(Collections.singletonList(changeRequest));
		NotificationEventStatus status = createCancelledEventAndProcess(PushTemplateType.ORDER_STATUS_CANCELLED);

		assertEquals(NotificationEventStatus.SENT, status);
	}
}
