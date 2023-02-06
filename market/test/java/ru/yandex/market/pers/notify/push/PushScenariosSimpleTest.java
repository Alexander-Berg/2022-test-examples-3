package ru.yandex.market.pers.notify.push;

import java.time.Instant;
import java.util.Collections;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.changerequest.ChangeRequest;
import ru.yandex.market.checkout.checkouter.order.changerequest.ChangeRequestStatus;
import ru.yandex.market.checkout.checkouter.order.changerequest.ChangeRequestType;
import ru.yandex.market.pers.notify.PushConsumerTest;
import ru.yandex.market.pers.notify.ems.persistence.NotificationEventService;
import ru.yandex.market.pers.notify.model.event.NotificationEventDataName;
import ru.yandex.market.pers.notify.model.event.NotificationEventSource;
import ru.yandex.market.pers.notify.model.event.NotificationEventStatus;
import ru.yandex.market.pers.notify.model.push.MobileAppInfo;
import ru.yandex.market.pers.notify.model.push.MobilePlatform;
import ru.yandex.market.pers.notify.model.push.PushDeeplink;
import ru.yandex.market.pers.notify.model.push.PushTemplateType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.market.pers.notify.mock.MarketMailerMockFactory.generateChangeRequest;

/**
 * @author Ivan Anisimov
 *         valter@yandex-team.ru
 *         12.04.16
 */
public class PushScenariosSimpleTest extends PushConsumerTest {
    // empty
    private static final String UUID = "18e14e46612e537dc1007adb2570d57d";
    private static final String APP_NAME = "ru.yandex.test.market.app";
    private static final MobilePlatform PLATFORM = MobilePlatform.IPHONE;
    private static final String PUSH_TOKEN = "9fc525e3384f586bc83e9bf23357985e2f6f1f7c793a616cafcc4db18de46deb";

	/*
	// valter@
	private static final Long UID = 23427062L;
	private static final String UUID = "b244a9db42f25312bce2bb90f0ebd139";
	private static final MobilePlatform PLATFORM = MobilePlatform.ANDROID;
	private static final String PUSH_TOKEN = "APA91bEN-LjSL7doZjgVv3Nfs6xP77nMnMeKQmgA6zCfOZP41r-J9kC4L8VJdrhfceJvfJx0gcMFIVxAc6gErZVW4LpM8KZ2ylloO0ml8oIumqC0u161wV8a_i3KE9yEJEQfiL1rSOSb";
	*/

    @SuppressFBWarnings("URF_UNREAD_FIELD")
    private static class TestData {
        String broadcastText = "Привет! Тест броадкаста";

        Long orderId = 1L;

        String modelName = "Тестовый айфон";
        Long modelId = 1L;

        String shopName = "Тестовый магазин";
        Long shopId = 1L;

        Long gradeId = 1L;

        Long sourceId = 1L;

        Long discount = 1L;
    }

    @Autowired
    private NotificationEventService notificationEventService;
    @Autowired
    private MobileAppInfoDAO mobileAppInfoDAO;

    private TestData testData = new TestData();


    private void testPush(NotificationEventSource source) {
        mobileAppInfoDAO.add(new MobileAppInfo(source.getUid(), source.getUuid() == null ? "null" : source.getUuid(),
            "app", "none", MobilePlatform.ANY, false));
        assertEquals(NotificationEventStatus.SENT,
            pushReplacingConsumer.processEvent(notificationEventService.addEvent(source)).getStatus());
    }

    @Test
    public void testService() {
        NotificationEventSource source = NotificationEventSource
            .pushFromUuid(UUID, PushTemplateType.SERVICE)
            .addTemplateParam(NotificationEventDataName.MESSAGE, testData.broadcastText)
            .setSourceId(testData.sourceId)
            .build();
        testPush(source);
    }

    @Test
    public void testOrderStatusPickup() {
        NotificationEventSource source = NotificationEventSource
            .pushFromUuid(UUID, PushTemplateType.ORDER_STATUS_PICKUP)
            .addTemplateParam(NotificationEventDataName.ORDER_ID, String.valueOf(testData.orderId))
            .setSourceId(testData.sourceId)
            .addDeeplink(PushDeeplink.ORDERS, testData.orderId)
            .build();
        testPush(source);
    }

    @Test
    public void testLeaveGradeModel() {
        NotificationEventSource source = NotificationEventSource
            .pushFromUuid(UUID, PushTemplateType.LEAVE_GRADE_MODEL)
            .addTemplateParam(NotificationEventDataName.MODEL_NAME, testData.modelName)
            .setSourceId(testData.sourceId)
            .addDeeplink(PushDeeplink.ADD_MODEL_REVIEW, testData.modelId)
            .build();
        testPush(source);
    }

    @Test
    public void testLeaveGradeShop() {
        NotificationEventSource source = NotificationEventSource
            .pushFromUuid(UUID, PushTemplateType.LEAVE_GRADE_SHOP)
            .addTemplateParam(NotificationEventDataName.SHOP_NAME, testData.shopName)
            .setSourceId(testData.sourceId)
            .addDeeplink(PushDeeplink.ADD_SHOP_REVIEW, testData.shopId)
            .build();
        testPush(source);
    }

    @Test
    public void testOrderNotFinished() {
        NotificationEventSource source = NotificationEventSource
            .pushFromUuid(UUID, PushTemplateType.ORDER_NOT_FINISHED)
            .addTemplateParam(NotificationEventDataName.ORDER_ID, String.valueOf(testData.orderId))
            .setSourceId(testData.sourceId)
            .addDeeplink(PushDeeplink.ORDERS, testData.orderId)
            .build();
        testPush(source);
    }

    @Test
    public void testModelInWishlist() {
        NotificationEventSource source = NotificationEventSource
            .pushFromUuid(UUID, PushTemplateType.MODEL_IN_WISHLIST)
            .addDeeplink(PushDeeplink.WISHLIST)
            .build();
        testPush(source);
    }

    @Test
    public void testOrderStatusCancelled() {
        Order order = createOrder();
        order.setId(testData.orderId);
        ChangeRequest changeRequest = generateChangeRequest(
                ChangeRequestType.CANCELLATION,
                ChangeRequestStatus.NEW,
                Instant.now());
        order.setChangeRequests(Collections.singletonList(changeRequest));
        setupCheckouterClient(order);

        NotificationEventSource source = NotificationEventSource
            .pushFromUuid(UUID, PushTemplateType.ORDER_STATUS_CANCELLED)
            .addTemplateParam(NotificationEventDataName.ORDER_ID, String.valueOf(testData.orderId))
            .setSourceId(testData.sourceId)
            .addDeeplink(PushDeeplink.ORDERS, testData.orderId)
            .build();
        testPush(source);
    }

    @Test
    public void testPriceAlertOnstock() {
        NotificationEventSource source = NotificationEventSource
            .pushFromUuid(UUID, PushTemplateType.PRICE_ALERT_ONSTOCK)
            .addTemplateParam(NotificationEventDataName.MODEL_ID, String.valueOf(testData.orderId))
            .addDeeplink(PushDeeplink.MODEL, testData.modelId)
            .build();
        testPush(source);
    }

    @Test
    public void testUserLongOffline() {
        NotificationEventSource source = NotificationEventSource
            .pushFromUuid(UUID, PushTemplateType.USER_LONG_OFFLINE)
            .addDeeplink(PushDeeplink.START)
            .build();
        testPush(source);
    }

    @Test
    public void testGradeModerated() {
        NotificationEventSource source = NotificationEventSource
            .pushFromUuid(UUID, PushTemplateType.GRADE_MODERATED)
            .addTemplateParam(NotificationEventDataName.MESSAGE, "о магазине \"" + testData.shopName + "\" не прошел модерацию")
            .addDeeplink(PushDeeplink.REVIEWS)
            .build();
        testPush(source);

        source = NotificationEventSource
            .pushFromUuid(UUID, PushTemplateType.GRADE_MODERATED)
            .addTemplateParam(NotificationEventDataName.MESSAGE, "о товаре \"" + testData.modelName + "\" не прошел модерацию")
            .addDeeplink(PushDeeplink.REVIEWS)
            .build();
        testPush(source);
    }

    @Test
    public void testGradeVoted() {
        NotificationEventSource source = NotificationEventSource
            .pushFromUuid(UUID, PushTemplateType.GRADE_VOTED)
            .addTemplateParam(NotificationEventDataName.MESSAGE, "магазин \"" + testData.shopName + "\"")
            .addDeeplink(PushDeeplink.REVIEWS)
            .build();
        testPush(source);
    }

    @Test
    public void testGradeCommented() {
        NotificationEventSource source = NotificationEventSource
            .pushFromUuid(UUID, PushTemplateType.GRADE_COMMENTED)
            .addTemplateParam(NotificationEventDataName.SHOP_NAME, testData.shopName)
            .addDeeplink(PushDeeplink.REVIEWS)
            .build();
        testPush(source);
    }

    @Test
    public void testHotCategoryOffer() {
        long modelId = 100500L;
        NotificationEventSource source = NotificationEventSource
            .pushFromUuid(UUID, PushTemplateType.HOT_CATEGORY_OFFER)
            .addTemplateParam(NotificationEventDataName.MODEL_ID, String.valueOf(testData.modelId))
            .addTemplateParam(NotificationEventDataName.DISCOUNT, String.valueOf(testData.discount))
            .addDeeplink(PushDeeplink.MODEL, modelId)
            .build();
        testPush(source);
    }

    @Test
    public void testCartDiscount() {
        NotificationEventSource source = NotificationEventSource
            .pushFromUuid(UUID, PushTemplateType.CART_DISCOUNT)
            .addDeeplink(PushDeeplink.CART)
            .build();
        testPush(source);
    }

    @Test
    public void testWishlistDiscountSingle() {
        NotificationEventSource source = NotificationEventSource
            .pushFromUuid(UUID, PushTemplateType.WISHLIST_DISCOUNT_SINGLE)
            .addTemplateParam(NotificationEventDataName.MODEL_ID, String.valueOf(testData.modelId))
            .addDeeplink(PushDeeplink.WISHLIST)
            .build();
        testPush(source);
    }

    @Test
    public void testWishlistDiscountMultiple() {
        NotificationEventSource source = NotificationEventSource
            .pushFromUuid(UUID, PushTemplateType.WISHLIST_DISCOUNT_MULTIPLE)
            .addTemplateParam(NotificationEventDataName.MODEL_COUNT, "1")
            .addDeeplink(PushDeeplink.WISHLIST)
            .build();
        testPush(source);

        source = NotificationEventSource
            .pushFromUuid(UUID, PushTemplateType.WISHLIST_DISCOUNT_MULTIPLE)
            .addTemplateParam(NotificationEventDataName.MODEL_COUNT, "103")
            .addDeeplink(PushDeeplink.WISHLIST)
            .build();
        testPush(source);
    }

    //----- Mobile device register -----//

    @Autowired
    @Qualifier("marketPusherService")
    private PusherService marketPusherServiceMock;

    @BeforeEach
    public void setUpDevice() {
        assertTrue(marketPusherServiceMock.register(1L, UUID, APP_NAME, PLATFORM, PUSH_TOKEN));
    }
}
