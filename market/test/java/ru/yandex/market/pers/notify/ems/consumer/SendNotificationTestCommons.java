package ru.yandex.market.pers.notify.ems.consumer;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.annotation.PostConstruct;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryServiceType;
import ru.yandex.market.checkout.checkouter.delivery.shipment.Parcel;
import ru.yandex.market.checkout.checkouter.delivery.tracking.Track;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.checkout.checkouter.order.changerequest.ChangeRequest;
import ru.yandex.market.checkout.checkouter.order.changerequest.ChangeRequestStatus;
import ru.yandex.market.checkout.checkouter.order.changerequest.ChangeRequestType;
import ru.yandex.market.checkout.checkouter.receipt.Receipt;
import ru.yandex.market.checkout.checkouter.receipt.ReceiptType;
import ru.yandex.market.checkout.checkouter.receipt.Receipts;
import ru.yandex.market.loyalty.api.exception.MarketLoyaltyException;
import ru.yandex.market.loyalty.api.model.MarketLoyaltyErrorCode;
import ru.yandex.market.loyalty.api.model.coin.CoinStatus;
import ru.yandex.market.loyalty.api.model.coin.CoinType;
import ru.yandex.market.loyalty.api.model.coin.SmartShoppingImageTypes;
import ru.yandex.market.loyalty.api.model.coin.UserCoinResponse;
import ru.yandex.market.loyalty.client.MarketLoyaltyClient;
import ru.yandex.market.pers.notify.EmailSnippetGeneratorTest;
import ru.yandex.market.pers.notify.ems.NotificationEventConsumer;
import ru.yandex.market.pers.notify.ems.event.NotificationEvent;
import ru.yandex.market.pers.notify.ems.event.NotificationEventProcessingResult;
import ru.yandex.market.pers.notify.ems.persistence.NotificationEventService;
import ru.yandex.market.pers.notify.ems.service.MailerNotificationEventService;
import ru.yandex.market.pers.notify.external.checkouter.CheckouterService;
import ru.yandex.market.pers.notify.mail.consumer.CheckoutOrderSenderConsumer;
import ru.yandex.market.pers.notify.mail.consumer.PostBoxDeliveryOrderConsumer;
import ru.yandex.market.pers.notify.model.NotificationSubtype;
import ru.yandex.market.pers.notify.model.SenderTemplate;
import ru.yandex.market.pers.notify.model.Uid;
import ru.yandex.market.pers.notify.model.event.NotificationEventDataName;
import ru.yandex.market.pers.notify.model.event.NotificationEventSource;
import ru.yandex.market.pers.notify.model.event.NotificationEventStatus;
import ru.yandex.market.pers.notify.settings.SubscriptionAndIdentityService;
import ru.yandex.market.pers.notify.test.MarketMailerMockedDbTest;
import ru.yandex.market.report.ReportService;
import ru.yandex.market.util.db.ConfigurationService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;
import static ru.yandex.market.pers.notify.mock.MarketMailerMockFactory.generateChangeRequest;
import static ru.yandex.market.pers.notify.model.NotificationSubtype.BLUE_ORDER_CANCELLED;
import static ru.yandex.market.pers.notify.model.NotificationSubtype.BLUE_ORDER_CASH_RETURN_RECEIPT_PRINTED;
import static ru.yandex.market.pers.notify.model.NotificationSubtype.BLUE_ORDER_DELIVERED;
import static ru.yandex.market.pers.notify.model.NotificationSubtype.BLUE_ORDER_DELIVERY;
import static ru.yandex.market.pers.notify.model.NotificationSubtype.BLUE_ORDER_DELIVERY_POST_BOX;
import static ru.yandex.market.pers.notify.model.NotificationSubtype.BLUE_ORDER_DELIVERY_TRANSPORTATION_RECIPIENT;
import static ru.yandex.market.pers.notify.model.NotificationSubtype.BLUE_ORDER_PENDING;
import static ru.yandex.market.pers.notify.model.NotificationSubtype.BLUE_ORDER_PICKUP;
import static ru.yandex.market.pers.notify.model.NotificationSubtype.BLUE_ORDER_RETURN_RECEIPT_PRINTED;
import static ru.yandex.market.pers.notify.model.NotificationSubtype.BLUE_ORDER_UNPAID;
import static ru.yandex.market.pers.notify.model.NotificationSubtype.BLUE_PROMO_MISC;
import static ru.yandex.market.pers.notify.model.NotificationSubtype.CART_1;
import static ru.yandex.market.pers.notify.model.NotificationSubtype.CART_2;
import static ru.yandex.market.pers.notify.model.NotificationSubtype.CONFIRM_SUBSCRIPTION;
import static ru.yandex.market.pers.notify.model.NotificationSubtype.COUPON_FOR_PROMOTER_SUBSCRIPTION;
import static ru.yandex.market.pers.notify.model.NotificationSubtype.DIGITAL_ORDER_DELIVERY;
import static ru.yandex.market.pers.notify.model.NotificationSubtype.GRADE_MODEL_AFTER_ORDER_COUPON_ACTIVATED;
import static ru.yandex.market.pers.notify.model.NotificationSubtype.NEW_COMMENT_GRADE;
import static ru.yandex.market.pers.notify.model.NotificationSubtype.NEW_GRADE_MODEL_COMMENT;
import static ru.yandex.market.pers.notify.model.NotificationSubtype.NEW_GRADE_MODEL_COMMENT_ON_COMMENT;
import static ru.yandex.market.pers.notify.model.NotificationSubtype.ORDER_CANCELLED;
import static ru.yandex.market.pers.notify.model.NotificationSubtype.ORDER_DELIVERED;
import static ru.yandex.market.pers.notify.model.NotificationSubtype.ORDER_DELIVERED_CHECK;
import static ru.yandex.market.pers.notify.model.NotificationSubtype.ORDER_DELIVERY_CHANGE_DIFF;
import static ru.yandex.market.pers.notify.model.NotificationSubtype.ORDER_DELIVERY_CHANGE_WITH_TRACKING;
import static ru.yandex.market.pers.notify.model.NotificationSubtype.ORDER_DELIVERY_WITHOUT_TRACKING;
import static ru.yandex.market.pers.notify.model.NotificationSubtype.ORDER_DELIVERY_WITH_TRACKING;
import static ru.yandex.market.pers.notify.model.NotificationSubtype.ORDER_PAID_RECEIPT_PRINTED;
import static ru.yandex.market.pers.notify.model.NotificationSubtype.ORDER_PICKUP;
import static ru.yandex.market.pers.notify.model.NotificationSubtype.ORDER_PROCESSING;
import static ru.yandex.market.pers.notify.model.NotificationSubtype.ORDER_RETURN_RECEIPT_PRINTED;
import static ru.yandex.market.pers.notify.model.NotificationSubtype.ORDER_STRUCTURE_CHANGE_DIFF;
import static ru.yandex.market.pers.notify.model.NotificationSubtype.ORDER_TRACKING_PARCEL_DELIVERY_WITHOUT_TRACKING;
import static ru.yandex.market.pers.notify.model.NotificationSubtype.ORDER_TRACKING_PARCEL_DELIVERY_WITH_TRACKING;
import static ru.yandex.market.pers.notify.model.NotificationSubtype.ORDER_TRACKING_PARCEL_PICKUP_WITHOUT_TRACKING;
import static ru.yandex.market.pers.notify.model.NotificationSubtype.ORDER_TRACKING_PARCEL_PICKUP_WITH_TRACKING;
import static ru.yandex.market.pers.notify.model.NotificationSubtype.ORDER_UNPAID;
import static ru.yandex.market.pers.notify.model.NotificationSubtype.ORDER_UNPAID_WAITING_USER_DELIVERY_INPUT;
import static ru.yandex.market.pers.notify.model.NotificationSubtype.PA_EXIST_ON_SALE;
import static ru.yandex.market.pers.notify.model.NotificationSubtype.PA_WELCOME;
import static ru.yandex.market.pers.notify.model.NotificationSubtype.PRICE_DROP_FOUND;
import static ru.yandex.market.pers.notify.model.NotificationSubtype.PROMO_MISC;
import static ru.yandex.market.pers.notify.model.NotificationSubtype.PUSH_STORE_UNPAID_WAITING_USER_DELIVERY_INPUT;
import static ru.yandex.market.pers.notify.model.NotificationSubtype.PUSH_YAPLUS_DELIVERED_CASH_BACK;
import static ru.yandex.market.pers.notify.model.NotificationSubtype.QUALITY_INTERVIEW;
import static ru.yandex.market.pers.notify.model.NotificationSubtype.REFEREE_REFUND_STATEMENT;
import static ru.yandex.market.pers.notify.model.NotificationSubtype.SHOP_GRADE;
import static ru.yandex.market.pers.notify.model.NotificationSubtype.SUCCESSFUL_MODEL_GRADE;
import static ru.yandex.market.pers.notify.model.NotificationSubtype.SUCCESSFUL_SHOP_GRADE;
import static ru.yandex.market.pers.notify.model.NotificationSubtype.THANKS_FOR_ORDER_COUPON;

public abstract class SendNotificationTestCommons extends MarketMailerMockedDbTest {
    public static final String PA_WELCOME_MODEL_ID = "3243209";
    public static final String PA_WELCOME_CURRENCY = "RUR";
    public static final String PA_WELCOME_REGION_ID = "2";
    protected static final String EMAIL = "valter@yandex-team.ru";
    protected static final String UUID_STR = "uuid";
    protected static final long UID = 23427062L;
    protected static final String GRADE_ID = "781135";
    protected static final long ORDER_ID = 8246827L;
    protected static final long MULTIORDER_ID = 8484748L;
    protected static final long RECEIPT_ID = 2136423L;
    private static final long ADS_SUBSCRIPTION_ID = 8246827L;
    private static final String ADS_LOCATION = "mall";
    private static final long REGION_ID = 213L;
    private static final String COIN_IMAGE_URL = "http://example.com/img";
    private static final BiConsumer<Map<NotificationSubtype, Supplier<NotificationEventSource>>, NotificationSubtype> ADD_SENDER_ORDER_SCHEDULER = (schedulers, type) ->
        schedulers.put(type, () -> NotificationEventSource
            .fromEmail(EMAIL, type)
            .setSourceId(randomLong())
            .addDataParam(NotificationEventDataName.ORDER_ID, String.valueOf(ORDER_ID))
            .build());

    private static final BiConsumer<Map<NotificationSubtype, Supplier<NotificationEventSource>>, NotificationSubtype> ADD_SENDER_MULTIORDER_SCHEDULER = (schedulers, type) ->
            schedulers.put(type, () -> NotificationEventSource
                    .fromEmail(EMAIL, type)
                    .setSourceId(randomLong())
                    .addDataParam(NotificationEventDataName.ORDER_IDS, String.valueOf(ORDER_ID))
                    .addDataParam(NotificationEventDataName.MULTIORDER_ID, String.valueOf(MULTIORDER_ID))
                    .build());

    private static final BiConsumer<Map<NotificationSubtype, Supplier<NotificationEventSource>>, NotificationSubtype> TRANSBOUNDARY_TRADING_ORDER_SCHEDULER = (schedulers, type) ->
        schedulers.put(type, () -> NotificationEventSource
            .fromEmail(EMAIL, type)
            .setSourceId(randomLong())
            .addDataParam(NotificationEventDataName.ORDER_ID, String.valueOf(ORDER_ID))
            .build());
    public static final Date COIN_CREATION_DATE = new GregorianCalendar(2018, 9, 2).getTime();
    public static final String ACTIVATION_TOKEN = "123";

    private final BiConsumer<Map<NotificationSubtype, Supplier<NotificationEventSource>>, NotificationSubtype> STORE_PUSH_SCHEDULER = (schedulers, type) -> {
        schedulers.put(type, () -> NotificationEventSource
            .pushFromUuid(UUID_STR, type)
            .setSourceId(randomLong())
            .addDataParam(NotificationEventDataName.ORDER_ID, String.valueOf(ORDER_ID))
            .addTemplateParam(NotificationEventDataName.ORDER_ID, String.valueOf(ORDER_ID))
            .addTemplateParam("coupon", "coupon1")
            .addTemplateParam("delivery_service_name", "delivery service 1")
            .addTemplateParam("storage_expiration_date", "01.01.1990")
            .addTemplateParam("outlet_name", "outlet name 1")
            .addTemplateParam("track_number", "track number 1")
            .addTemplateParam("expiration_time", "01.01.1991")
            .addTemplateParam(NotificationEventDataName.BONUS_HEADER, "А вот и ваш бонус")
            .addTemplateParam(NotificationEventDataName.BONUS_BODY, "Жмякни посмотреть")
            .addTemplateParam(NotificationEventDataName.MINUTES_TO_WAIT, "30")
            .addTemplateParam(NotificationEventDataName.MESSAGE, "message")
            .addTemplateParam(NotificationEventDataName.TITLE, "title")
            .build());
    };

    private final BiConsumer<Map<NotificationSubtype, Supplier<NotificationEventSource>>, NotificationSubtype> TRANSBOUNDARY_TRADING_PUSH_SCHEDULER = (schedulers, type) -> {
        schedulers.put(type, () -> NotificationEventSource
            .pushFromUuid(UUID_STR, type)
            .setSourceId(randomLong())
            .addTemplateParam("text", "my text!")
            .build());
    };

    private static final BiConsumer<Map<NotificationSubtype, Supplier<NotificationEventSource>>, NotificationSubtype> ADD_ES_ORDER_SCHEDULER = (schedulers, type) ->
        schedulers.put(type, () -> NotificationEventSource
            .fromEmail(EMAIL, type)
            .setSourceId(randomLong())
            .addDataParam(NotificationEventDataName.ORDER_ID, String.valueOf(ORDER_ID))
            .addDataParam(NotificationEventDataName.CHECKOUT_DATA, EmailSnippetGeneratorTest.xmlToSnippet.entrySet().stream()
                .filter(e -> type.name().equals(e.getValue().getSecond().name()))
                .findAny()
                .orElse(null)
                .getValue().getFirst())
            .build());

    @Autowired
    protected ApplicationContext context;
    @Autowired
    protected SubscriptionAndIdentityService subscriptionAndIdentityService;
    @Autowired
    protected ReportService reportService;
    @Autowired
    protected CheckouterService checkouterService;
    @Autowired
    protected CheckoutOrderSenderConsumer checkoutOrderSenderConsumer;
    @Autowired
    protected PostBoxDeliveryOrderConsumer postBoxDeliveryOrderConsumer;
    @Autowired
    protected ConfigurationService notifyConfigurationService;
    protected Map<NotificationSubtype, Supplier<NotificationEventSource>> schedulers;
    private Map<NotificationSubtype, NotificationEventConsumer> consumers;
    @Autowired
    private NotificationEventService notificationEventService;
    @Autowired
    protected MailerNotificationEventService mailerNotificationEventService;
    @Autowired
    private MarketLoyaltyClient marketLoyaltyClient;

    protected static String randomIntString() {
        return String.valueOf(RND.nextInt(200_000) + 1);
    }

    private static int randomInt() {
        return RND.nextInt(200_000) + 1;
    }

    protected static long randomLong() {
        return (long) randomInt();
    }

    @PostConstruct
    public void initSchedulersAndConsumers() {
        schedulers = new HashMap<>();

        STORE_PUSH_SCHEDULER.accept(schedulers, NotificationSubtype.PUSH_STORE_PREPAID_PENDING);
        STORE_PUSH_SCHEDULER.accept(schedulers, NotificationSubtype.PUSH_STORE_PREPAID_DELAY);
        STORE_PUSH_SCHEDULER.accept(schedulers, NotificationSubtype.PUSH_STORE_POSTPAID_PENDING);
        STORE_PUSH_SCHEDULER.accept(schedulers, NotificationSubtype.PUSH_STORE_PICKUP_PREPAID);
        STORE_PUSH_SCHEDULER.accept(schedulers, NotificationSubtype.PUSH_STORE_PICKUP_POSTPAID);
        STORE_PUSH_SCHEDULER.accept(schedulers, NotificationSubtype.PUSH_STORE_DELIVERY_PREPAID);
        STORE_PUSH_SCHEDULER.accept(schedulers, NotificationSubtype.PUSH_STORE_DELIVERY_POSTPAID);
        STORE_PUSH_SCHEDULER.accept(schedulers, NotificationSubtype.PUSH_STORE_DELIVERY_READY_FOR_LAST_MILE);
        STORE_PUSH_SCHEDULER.accept(schedulers, NotificationSubtype.PUSH_STORE_DELIVERED);
        STORE_PUSH_SCHEDULER.accept(schedulers, NotificationSubtype.PUSH_STORE_CANCELLED_USER_REFUSED_PREPAID);
        STORE_PUSH_SCHEDULER.accept(schedulers, NotificationSubtype.PUSH_STORE_CANCELLED_USER_REFUSED_POSTPAID);
        STORE_PUSH_SCHEDULER.accept(schedulers, NotificationSubtype.PUSH_STORE_CANCELLED_USER_NOT_PAID);
        STORE_PUSH_SCHEDULER.accept(schedulers, NotificationSubtype.PUSH_STORE_CANCELLED_SHOP_FAILED_PREPAID);
        STORE_PUSH_SCHEDULER.accept(schedulers, NotificationSubtype.PUSH_STORE_CANCELLED_SHOP_FAILED_POSTPAID);
        STORE_PUSH_SCHEDULER.accept(schedulers, NotificationSubtype.PUSH_STORE_CANCELLED_PENDING_CANCELLED_PREPAID);
        STORE_PUSH_SCHEDULER.accept(schedulers, NotificationSubtype.PUSH_STORE_CANCELLED_PENDING_CANCELLED_POSTPAID);
        STORE_PUSH_SCHEDULER.accept(schedulers, NotificationSubtype.PUSH_STORE_COIN_ACTIVATED);
        STORE_PUSH_SCHEDULER.accept(schedulers, NotificationSubtype.PUSH_STORE_LAVKA);
        STORE_PUSH_SCHEDULER.accept(schedulers, NotificationSubtype.PUSH_STORE_UNPAID_WAITING_USER_DELIVERY_INPUT);

        TRANSBOUNDARY_TRADING_PUSH_SCHEDULER.accept(schedulers, NotificationSubtype.PUSH_TRANSBOUNDARY_TRADING_GENERIC);
        TRANSBOUNDARY_TRADING_PUSH_SCHEDULER.accept(schedulers, NotificationSubtype.PUSH_TRANSBOUNDARY_TRADING_ADVERTISING);
        TRANSBOUNDARY_TRADING_PUSH_SCHEDULER.accept(schedulers, NotificationSubtype.PUSH_TRANSBOUNDARY_TRADING_TRANSACTION);
        TRANSBOUNDARY_TRADING_PUSH_SCHEDULER.accept(schedulers, NotificationSubtype.PUSH_TRANSBOUNDARY_TRADING_TRIGGER);

        TRANSBOUNDARY_TRADING_ORDER_SCHEDULER.accept(schedulers, NotificationSubtype.TRANSBOUNDARY_TRADING_ORDER_PAID);
        TRANSBOUNDARY_TRADING_ORDER_SCHEDULER.accept(schedulers, NotificationSubtype.TRANSBOUNDARY_TRADING_ORDER_CANCELLED);
        TRANSBOUNDARY_TRADING_ORDER_SCHEDULER.accept(schedulers, NotificationSubtype.TRANSBOUNDARY_TRADING_ORDER_CANCELLED_BY_USER);
        TRANSBOUNDARY_TRADING_ORDER_SCHEDULER.accept(schedulers, NotificationSubtype.TRANSBOUNDARY_TRADING_ORDER_DELIVERY);
        TRANSBOUNDARY_TRADING_ORDER_SCHEDULER.accept(schedulers, NotificationSubtype.TRANSBOUNDARY_TRADING_ORDER_PICKUP);
        TRANSBOUNDARY_TRADING_ORDER_SCHEDULER.accept(schedulers, NotificationSubtype.TRANSBOUNDARY_TRADING_ORDER_DELIVERED);

        ADD_SENDER_ORDER_SCHEDULER.accept(schedulers, BLUE_ORDER_PENDING);
        ADD_SENDER_ORDER_SCHEDULER.accept(schedulers, ORDER_DELIVERED);
        ADD_SENDER_ORDER_SCHEDULER.accept(schedulers, BLUE_ORDER_DELIVERED);
        ADD_SENDER_ORDER_SCHEDULER.accept(schedulers, BLUE_ORDER_DELIVERY);
        schedulers.put(BLUE_ORDER_DELIVERY_POST_BOX, () -> NotificationEventSource
            .fromEmail(EMAIL, NotificationSubtype.BLUE_ORDER_DELIVERY_POST_BOX)
            .setSourceId(ORDER_ID)
            .addDataParam(NotificationEventDataName.ORDER_ID, String.valueOf(ORDER_ID))
            .addDataParam(PostBoxDeliveryOrderConsumer.POST_BOX_PINCODE, "1223435")
            .addDataParam(PostBoxDeliveryOrderConsumer.POST_BOX_EXPIRED_DATE, "01.01.2019")
            .addDataParam(PostBoxDeliveryOrderConsumer.POST_BOX_CLIENT_PHONE, "+7 999 999-99-99")
            .build()
        );
        ADD_SENDER_ORDER_SCHEDULER.accept(schedulers, ORDER_CANCELLED);
        ADD_SENDER_ORDER_SCHEDULER.accept(schedulers, BLUE_ORDER_CANCELLED);
        ADD_SENDER_ORDER_SCHEDULER.accept(schedulers, ORDER_PICKUP);
        ADD_SENDER_ORDER_SCHEDULER.accept(schedulers, BLUE_ORDER_PICKUP);
        ADD_SENDER_ORDER_SCHEDULER.accept(schedulers, ORDER_PROCESSING);
        ADD_SENDER_ORDER_SCHEDULER.accept(schedulers, ORDER_UNPAID);
        ADD_SENDER_ORDER_SCHEDULER.accept(schedulers, BLUE_ORDER_UNPAID);
        ADD_SENDER_ORDER_SCHEDULER.accept(schedulers, ORDER_UNPAID_WAITING_USER_DELIVERY_INPUT);

        ADD_SENDER_ORDER_SCHEDULER.accept(schedulers, ORDER_DELIVERY_CHANGE_DIFF);
        ADD_SENDER_ORDER_SCHEDULER.accept(schedulers, ORDER_STRUCTURE_CHANGE_DIFF);
        ADD_SENDER_ORDER_SCHEDULER.accept(schedulers, ORDER_DELIVERED_CHECK);
        ADD_SENDER_ORDER_SCHEDULER.accept(schedulers, ORDER_DELIVERY_WITH_TRACKING);
        ADD_SENDER_ORDER_SCHEDULER.accept(schedulers, ORDER_DELIVERY_WITHOUT_TRACKING);
        ADD_SENDER_ORDER_SCHEDULER.accept(schedulers, ORDER_DELIVERY_CHANGE_WITH_TRACKING);

        ADD_SENDER_ORDER_SCHEDULER.accept(schedulers, ORDER_TRACKING_PARCEL_PICKUP_WITHOUT_TRACKING);
        ADD_SENDER_ORDER_SCHEDULER.accept(schedulers, ORDER_TRACKING_PARCEL_DELIVERY_WITHOUT_TRACKING);
        ADD_SENDER_ORDER_SCHEDULER.accept(schedulers, BLUE_ORDER_DELIVERY_TRANSPORTATION_RECIPIENT);
        ADD_SENDER_ORDER_SCHEDULER.accept(schedulers, ORDER_TRACKING_PARCEL_PICKUP_WITH_TRACKING);
        ADD_SENDER_ORDER_SCHEDULER.accept(schedulers, ORDER_TRACKING_PARCEL_DELIVERY_WITH_TRACKING);

        schedulers.put(PROMO_MISC, () -> NotificationEventSource
            .fromEmail(EMAIL, NotificationSubtype.PROMO_MISC)
            .addDataParam(NotificationEventDataName.SENDER_TEMPLATE, SenderTemplate.PROMO_MISC_TEST.name())
            .build()
        );

        schedulers.put(BLUE_PROMO_MISC, () -> NotificationEventSource
            .fromEmail(EMAIL, NotificationSubtype.BLUE_PROMO_MISC)
            .addDataParam(NotificationEventDataName.SENDER_TEMPLATE, SenderTemplate.PROMO_MISC_TEST.name())
            .build()
        );

        Function<NotificationSubtype, Supplier<NotificationEventSource>>  receiptShedulersFactory = subtype -> () ->
            NotificationEventSource.fromEmail(EMAIL, subtype)
            .setSourceId(ORDER_ID)
            .addDataParam(NotificationEventDataName.RECEIPT_ID, String.valueOf(RECEIPT_ID))
            .build();
        schedulers.put(ORDER_PAID_RECEIPT_PRINTED, receiptShedulersFactory.apply(ORDER_PAID_RECEIPT_PRINTED));

        schedulers.put(ORDER_RETURN_RECEIPT_PRINTED, receiptShedulersFactory.apply(ORDER_RETURN_RECEIPT_PRINTED));
        schedulers.put(BLUE_ORDER_RETURN_RECEIPT_PRINTED,
            receiptShedulersFactory.apply(BLUE_ORDER_RETURN_RECEIPT_PRINTED));
        schedulers.put(BLUE_ORDER_CASH_RETURN_RECEIPT_PRINTED,
                receiptShedulersFactory.apply(BLUE_ORDER_CASH_RETURN_RECEIPT_PRINTED));

        schedulers.put(NEW_COMMENT_GRADE, () -> NotificationEventSource
            .fromUid(UID, NEW_COMMENT_GRADE)
            .addDataParam(NotificationEventDataName.GRADE_ID, GRADE_ID)
            .addDataParam(NotificationEventDataName.COMMENT, "comment")
            .addDataParam(NotificationEventDataName.PARENT_COMMENT, "parentComment")
            .build());

        schedulers.put(NEW_GRADE_MODEL_COMMENT, () -> NotificationEventSource
            .fromUid(UID, NEW_GRADE_MODEL_COMMENT)
            .addDataParam(NotificationEventDataName.GRADE_ID, GRADE_ID)
            .addDataParam(NotificationEventDataName.COMMENT_AUTHOR_UID, String.valueOf(UID))
            .addDataParam(NotificationEventDataName.COMMENT, "comment")
            .build());
        schedulers.put(NEW_GRADE_MODEL_COMMENT_ON_COMMENT, () -> NotificationEventSource
            .fromUid(UID, NEW_GRADE_MODEL_COMMENT_ON_COMMENT)
            .addDataParam(NotificationEventDataName.GRADE_ID, GRADE_ID)
            .addDataParam(NotificationEventDataName.COMMENT, "comment")
            .addDataParam(NotificationEventDataName.COMMENT_DATE, String.valueOf(System.currentTimeMillis()))
            .addDataParam(NotificationEventDataName.COMMENT_AUTHOR_UID, randomIntString())
            .addDataParam(NotificationEventDataName.PARENT_COMMENT, "parent comment")
            .addDataParam(NotificationEventDataName.PARENT_COMMENT_AUTHOR_UID, randomIntString())
            .build());

        schedulers.put(QUALITY_INTERVIEW, () -> NotificationEventSource
            .fromIdentity(new Uid(UID), EMAIL, QUALITY_INTERVIEW)
            .setSourceId(randomLong())
            .build());

        schedulers.put(PA_WELCOME, () -> NotificationEventSource
            .fromEmail(EMAIL, PA_WELCOME)
            .setSourceId(randomLong())
            .addDataParam(NotificationEventDataName.MODEL_ID, PA_WELCOME_MODEL_ID)
            .addDataParam(NotificationEventDataName.SUBSCRIPTION_ID, "1")
            .addDataParam(NotificationEventDataName.CURRENCY, PA_WELCOME_CURRENCY)
            .addDataParam(NotificationEventDataName.REGION_ID, PA_WELCOME_REGION_ID)
            .addDataParam(NotificationEventDataName.QUALITY_RATING, "1")
            .addDataParam(NotificationEventDataName.UID, String.valueOf(UID))
            .build());

        schedulers.put(PA_EXIST_ON_SALE, () -> NotificationEventSource
            .fromEmail(EMAIL, PA_EXIST_ON_SALE)
            .setSourceId(randomLong())
            .addDataParam(NotificationEventDataName.MODEL_ID, randomIntString())
            .addDataParam(NotificationEventDataName.SUBSCRIPTION_ID, randomIntString())
            .addDataParam(NotificationEventDataName.CURRENCY, "RUR")
            .addDataParam(NotificationEventDataName.REGION_ID, randomIntString())
            .addDataParam(NotificationEventDataName.QUALITY_RATING, "1")
            .addDataParam(NotificationEventDataName.UID, String.valueOf(UID))
            .build());

        schedulers.put(PRICE_DROP_FOUND, () -> NotificationEventSource
            .fromEmail(EMAIL, PRICE_DROP_FOUND)
            .setSourceId(randomLong())
            .addDataParam(NotificationEventDataName.MODEL_ID, randomIntString())
            .addDataParam(NotificationEventDataName.PRICE, "0")
            .addDataParam(NotificationEventDataName.SUBSCRIPTION_ID, randomIntString())
            .addDataParam(NotificationEventDataName.CURRENCY, "RUR")
            .addDataParam(NotificationEventDataName.REGION_ID, randomIntString())
            .addDataParam(NotificationEventDataName.QUALITY_RATING, "1")
            .addDataParam(NotificationEventDataName.UID, String.valueOf(UID))
            .build());

        schedulers.put(SHOP_GRADE, () -> new NotificationEventSource.Builder<>()
            .setMbiAddress("1")
            .setNotificationSubtype(SHOP_GRADE)
            .addDataParam(NotificationEventDataName.GRADE_ID, GRADE_ID)
            .addDataParam(NotificationEventDataName.COMMENT, "comment")
            .addDataParam(NotificationEventDataName.COMMENT_AUTHOR_UID, String.valueOf(RND.nextInt()))
            .addDataParam(NotificationEventDataName.COMMENT_DATE, String.valueOf(System.currentTimeMillis()))
            .addDataParam(NotificationEventDataName.PARENT_COMMENT, "comment")
            .addDataParam(NotificationEventDataName.PARENT_COMMENT_ID, "parent comment id")
            .build());

        schedulers.put(CONFIRM_SUBSCRIPTION, () -> NotificationEventSource
            .fromEmail(EMAIL, CONFIRM_SUBSCRIPTION)
            .setSourceId(1L)
            .addDataParam(NotificationEventDataName.SUBSCRIPTION_ID, "1")
            .build());

        schedulers.put(CART_1, () -> NotificationEventSource
            .fromUid(UID, CART_1)
            .build());

        schedulers.put(CART_2, () -> NotificationEventSource
            .fromUid(UID, CART_2)
            .build());

        schedulers.put(COUPON_FOR_PROMOTER_SUBSCRIPTION, () -> NotificationEventSource
            .fromEmail(EMAIL, COUPON_FOR_PROMOTER_SUBSCRIPTION)
            .setSourceId(ADS_SUBSCRIPTION_ID)
            .addDataParam(NotificationEventDataName.SUBSCRIPTION_ID, String.valueOf(ADS_SUBSCRIPTION_ID))
            .addDataParam(NotificationEventDataName.REGION_ID, String.valueOf(REGION_ID))
            .addDataParam(NotificationEventDataName.ADS_LOCATION, ADS_LOCATION)
            .setUid(123l)
            .build()
        );
        schedulers.put(GRADE_MODEL_AFTER_ORDER_COUPON_ACTIVATED, () -> NotificationEventSource
            .fromUid(UID, GRADE_MODEL_AFTER_ORDER_COUPON_ACTIVATED)
            .setSourceId(Long.parseLong(GRADE_ID))
            .addDataParam(NotificationEventDataName.COUPON, "coupon-code")
            .addDataParam(NotificationEventDataName.ORDER_ID, "1")
            .build()
        );

        schedulers.put(THANKS_FOR_ORDER_COUPON, () -> NotificationEventSource
            .fromUid(UID, THANKS_FOR_ORDER_COUPON)
            .setSourceId(ORDER_ID)
            .addDataParam(NotificationEventDataName.ORDER_ID, String.valueOf(ORDER_ID))
            .build()
        );

        schedulers.put(REFEREE_REFUND_STATEMENT, () -> NotificationEventSource
            .fromEmail(EMAIL, REFEREE_REFUND_STATEMENT)
            .addDataParam(NotificationEventDataName.USER_NAME, "user_name")
            .addDataParam(NotificationEventDataName.ORDER_ID, "1")
            .addDataParam(NotificationEventDataName.CONVERSATION_ID, "2")
            .addDataParam(NotificationEventDataName.ATTACHMENT_GROUP, "3")
            .addDataParam(NotificationEventDataName.ATTACHMENT_ID, "4")
            .build()
        );

        schedulers.put(PUSH_YAPLUS_DELIVERED_CASH_BACK, () -> new NotificationEventSource
                .PushBuilder(PUSH_YAPLUS_DELIVERED_CASH_BACK)
                .setSourceId(ORDER_ID)
                .setUid(UID)
                .setUuid(UUID_STR)
                .addTemplateParam(NotificationEventDataName.CASH_BACK_FOR_ORDER, "765")
                .build()
        );

        String itemsBoxesStr = "[{" +
                "\"offer_name\":\"Тестовая подписка\"," +
                "\"keys\":[\"123\",\"345\"]," +
                "\"activate_till\":\"2021-01-31 00:00:00\"," +
                "\"slip\":\"Текст\"}]";
        schedulers.put(DIGITAL_ORDER_DELIVERY, () -> NotificationEventSource
                .fromEmail(EMAIL, DIGITAL_ORDER_DELIVERY)
                .addDataParam(NotificationEventDataName.ORDER_ID, String.valueOf(ORDER_ID))
                .addDataParam(NotificationEventDataName.ITEMS_BOXES, itemsBoxesStr)
                .build()
        );

        NotificationEventConsumer orderReceiptSenderConsumer =
            (NotificationEventConsumer) context.getBean("orderReceiptSenderConsumer");
        consumers = new HashMap<>();
        consumers.put(ORDER_CANCELLED, checkoutOrderSenderConsumer);
        consumers.put(
            BLUE_ORDER_CANCELLED,
            checkoutOrderSenderConsumer
        );
        consumers.put(BLUE_ORDER_PENDING, checkoutOrderSenderConsumer);
        consumers.put(BLUE_ORDER_DELIVERY, checkoutOrderSenderConsumer);
        consumers.put(BLUE_ORDER_DELIVERY_POST_BOX, postBoxDeliveryOrderConsumer);
        consumers.put(ORDER_DELIVERED, checkoutOrderSenderConsumer);
        consumers.put(BLUE_ORDER_DELIVERED, checkoutOrderSenderConsumer);
        consumers.put(ORDER_DELIVERED_CHECK, checkoutOrderSenderConsumer);
        consumers.put(BLUE_ORDER_PICKUP, checkoutOrderSenderConsumer);
        consumers.put(ORDER_PICKUP, checkoutOrderSenderConsumer);
        consumers.put(ORDER_PROCESSING, checkoutOrderSenderConsumer);
        consumers.put(ORDER_UNPAID, checkoutOrderSenderConsumer);
        consumers.put(BLUE_ORDER_UNPAID, checkoutOrderSenderConsumer);
        consumers.put(ORDER_DELIVERY_CHANGE_DIFF, checkoutOrderSenderConsumer);
        consumers.put(ORDER_STRUCTURE_CHANGE_DIFF, checkoutOrderSenderConsumer);
        consumers.put(ORDER_DELIVERY_WITH_TRACKING, checkoutOrderSenderConsumer);
        consumers.put(ORDER_DELIVERY_WITHOUT_TRACKING, checkoutOrderSenderConsumer);
        consumers.put(ORDER_DELIVERY_CHANGE_WITH_TRACKING, checkoutOrderSenderConsumer);
        consumers.put(NEW_COMMENT_GRADE, (NotificationEventConsumer) context.getBean("userGradeCommentConsumer"));
        consumers.put(NEW_GRADE_MODEL_COMMENT, (NotificationEventConsumer) context.getBean("modelGradeCommentConsumer"));
        consumers.put(NEW_GRADE_MODEL_COMMENT_ON_COMMENT, (NotificationEventConsumer) context.getBean("modelCommentOnCommentConsumer"));
        consumers.put(QUALITY_INTERVIEW, (NotificationEventConsumer) context.getBean("qualityInterviewConsumer"));
        consumers.put(PA_WELCOME, (NotificationEventConsumer) context.getBean("priceAlertConsumer"));
        consumers.put(PA_EXIST_ON_SALE, (NotificationEventConsumer) context.getBean("priceAlertOnSaleConsumer"));
        consumers.put(PRICE_DROP_FOUND, (NotificationEventConsumer) context.getBean("priceAlertOnSaleConsumer"));
        consumers.put(SHOP_GRADE, (NotificationEventConsumer) context.getBean("shopGradeConsumer"));
        consumers.put(CONFIRM_SUBSCRIPTION, (NotificationEventConsumer) context.getBean("confirmationSenderConsumer"));
        consumers.put(CART_1, (NotificationEventConsumer) context.getBean("carterConsumer_1"));
        consumers.put(CART_2, (NotificationEventConsumer) context.getBean("carterConsumer_2"));
        consumers.put(ORDER_PAID_RECEIPT_PRINTED, orderReceiptSenderConsumer);
        consumers.put(ORDER_RETURN_RECEIPT_PRINTED, orderReceiptSenderConsumer);
        consumers.put(BLUE_ORDER_RETURN_RECEIPT_PRINTED, orderReceiptSenderConsumer);
        consumers.put(BLUE_ORDER_CASH_RETURN_RECEIPT_PRINTED, orderReceiptSenderConsumer);
        consumers.put(COUPON_FOR_PROMOTER_SUBSCRIPTION, (NotificationEventConsumer) context.getBean("couponForSubscriptionConsumer"));
        consumers.put(GRADE_MODEL_AFTER_ORDER_COUPON_ACTIVATED, (NotificationEventConsumer) context.getBean("gradeAfterOrderCouponActivatedConsumer"));
        consumers.put(THANKS_FOR_ORDER_COUPON, (NotificationEventConsumer) context.getBean("thanksForOrderConsumer"));
        consumers.put(ORDER_UNPAID_WAITING_USER_DELIVERY_INPUT, checkoutOrderSenderConsumer);

        consumers.put(ORDER_TRACKING_PARCEL_PICKUP_WITHOUT_TRACKING, checkoutOrderSenderConsumer);
        consumers.put(ORDER_TRACKING_PARCEL_DELIVERY_WITHOUT_TRACKING, checkoutOrderSenderConsumer);
        consumers.put(BLUE_ORDER_DELIVERY_TRANSPORTATION_RECIPIENT, checkoutOrderSenderConsumer);
        consumers.put(ORDER_TRACKING_PARCEL_PICKUP_WITH_TRACKING, checkoutOrderSenderConsumer);
        consumers.put(ORDER_TRACKING_PARCEL_DELIVERY_WITH_TRACKING, checkoutOrderSenderConsumer);

        consumers.put(SUCCESSFUL_SHOP_GRADE, (NotificationEventConsumer) context.getBean("shopGradeModerationConsumer"));
        consumers.put(SUCCESSFUL_MODEL_GRADE, (NotificationEventConsumer) context.getBean("modelGradeModerationConsumer"));
        consumers.put(PROMO_MISC, (NotificationEventConsumer) context.getBean("simplePromoConsumer"));
        consumers.put(BLUE_PROMO_MISC, (NotificationEventConsumer) context.getBean("bluePromoMiscEventConsumer"));

        consumers.put(REFEREE_REFUND_STATEMENT,
            (NotificationEventConsumer) context.getBean("refereeRefundStatementNotificationConsumer"));

        consumers.put(NotificationSubtype.TRANSBOUNDARY_TRADING_ORDER_PAID, checkoutOrderSenderConsumer);
        consumers.put(NotificationSubtype.TRANSBOUNDARY_TRADING_ORDER_CANCELLED, checkoutOrderSenderConsumer);
        consumers.put(NotificationSubtype.TRANSBOUNDARY_TRADING_ORDER_CANCELLED_BY_USER, checkoutOrderSenderConsumer);
        consumers.put(NotificationSubtype.TRANSBOUNDARY_TRADING_ORDER_DELIVERY, checkoutOrderSenderConsumer);
        consumers.put(NotificationSubtype.TRANSBOUNDARY_TRADING_ORDER_PICKUP, checkoutOrderSenderConsumer);
        consumers.put(NotificationSubtype.TRANSBOUNDARY_TRADING_ORDER_DELIVERED, checkoutOrderSenderConsumer);

        consumers.put(NotificationSubtype.PUSH_STORE_PREPAID_PENDING, (NotificationEventConsumer) context.getBean("pushSimpleConsumer"));
        consumers.put(NotificationSubtype.PUSH_STORE_PREPAID_DELAY, (NotificationEventConsumer) context.getBean("pushSimpleConsumer"));
        consumers.put(NotificationSubtype.PUSH_STORE_POSTPAID_PENDING, (NotificationEventConsumer) context.getBean("pushSimpleConsumer"));
        consumers.put(NotificationSubtype.PUSH_STORE_PICKUP_PREPAID, (NotificationEventConsumer) context.getBean("pushSimpleConsumer"));
        consumers.put(NotificationSubtype.PUSH_STORE_PICKUP_POSTPAID, (NotificationEventConsumer) context.getBean("pushSimpleConsumer"));
        consumers.put(NotificationSubtype.PUSH_STORE_DELIVERY_PREPAID, (NotificationEventConsumer) context.getBean("pushSimpleConsumer"));
        consumers.put(NotificationSubtype.PUSH_STORE_DELIVERY_POSTPAID, (NotificationEventConsumer) context.getBean("pushSimpleConsumer"));
        consumers.put(NotificationSubtype.PUSH_STORE_DELIVERY_READY_FOR_LAST_MILE, (NotificationEventConsumer) context.getBean("pushSimpleConsumer"));
        consumers.put(NotificationSubtype.PUSH_STORE_DELIVERED, (NotificationEventConsumer) context.getBean("pushSimpleConsumer"));
        consumers.put(NotificationSubtype.PUSH_STORE_CANCELLED_USER_REFUSED_PREPAID, (NotificationEventConsumer) context.getBean("pushSimpleConsumer"));
        consumers.put(NotificationSubtype.PUSH_STORE_CANCELLED_USER_REFUSED_POSTPAID, (NotificationEventConsumer) context.getBean("pushSimpleConsumer"));
        consumers.put(NotificationSubtype.PUSH_STORE_CANCELLED_USER_NOT_PAID, (NotificationEventConsumer) context.getBean("pushSimpleConsumer"));
        consumers.put(NotificationSubtype.PUSH_STORE_CANCELLED_SHOP_FAILED_PREPAID, (NotificationEventConsumer) context.getBean("pushSimpleConsumer"));
        consumers.put(NotificationSubtype.PUSH_STORE_CANCELLED_SHOP_FAILED_POSTPAID, (NotificationEventConsumer) context.getBean("pushSimpleConsumer"));
        consumers.put(NotificationSubtype.PUSH_STORE_CANCELLED_PENDING_CANCELLED_PREPAID, (NotificationEventConsumer) context.getBean("pushSimpleConsumer"));
        consumers.put(NotificationSubtype.PUSH_STORE_CANCELLED_PENDING_CANCELLED_POSTPAID, (NotificationEventConsumer) context.getBean("pushSimpleConsumer"));
        consumers.put(NotificationSubtype.PUSH_TRANSBOUNDARY_TRADING_GENERIC, (NotificationEventConsumer) context.getBean("pushSimpleConsumer"));
        consumers.put(NotificationSubtype.PUSH_TRANSBOUNDARY_TRADING_ADVERTISING, (NotificationEventConsumer) context.getBean("pushSimpleConsumer"));
        consumers.put(NotificationSubtype.PUSH_TRANSBOUNDARY_TRADING_TRANSACTION, (NotificationEventConsumer) context.getBean("pushSimpleConsumer"));
        consumers.put(NotificationSubtype.PUSH_TRANSBOUNDARY_TRADING_TRIGGER, (NotificationEventConsumer) context.getBean("pushSimpleConsumer"));
        consumers.put(NotificationSubtype.PUSH_STORE_COIN_ACTIVATED, (NotificationEventConsumer) context.getBean("pushSimpleConsumer"));
        consumers.put(NotificationSubtype.PUSH_STORE_LAVKA, (NotificationEventConsumer) context.getBean("pushSimpleConsumer"));
        consumers.put(NotificationSubtype.PUSH_YAPLUS_DELIVERED_CASH_BACK, (NotificationEventConsumer) context.getBean("pushSimpleConsumer"));
        consumers.put(DIGITAL_ORDER_DELIVERY, (NotificationEventConsumer) context.getBean("digitalOrderDeliveryEventConsumer"));
        consumers.put(PUSH_STORE_UNPAID_WAITING_USER_DELIVERY_INPUT, (NotificationEventConsumer) context.getBean("pushSimpleConsumer"));
    }

    @BeforeEach
    public void init() {
        // set default email for uid
        subscriptionAndIdentityService.createEmailOwnershipIfNecessary(new Uid(UID), EMAIL, false);
        subscriptionAndIdentityService.confirmEmailOwnership(new Uid(UID), EMAIL);
        subscriptionAndIdentityService.setActiveEmail(new Uid(UID), EMAIL);
    }

    protected void setupCheckouterClientGetOrder(Order order) {
        when(checkouterService.getOrder(ORDER_ID, ClientRole.SYSTEM, 0L, false)).thenReturn(order);
    }

    protected void setupLoyaltyGetCoinsForOrder(long orderId, OrderStatus orderStatus) {
        setupLoyaltyGetCoinsForOrder(orderId, orderStatus, marketLoyaltyClient);
    }

    public static void setupLoyaltyGetCoinsForOrder(long orderId, OrderStatus orderStatus, MarketLoyaltyClient marketLoyaltyClient) {
        setupLoyaltyGetCoinsForOrder(orderId, marketLoyaltyClient, CoinType.FIXED, orderStatus);
    }

    public static void setupLoyaltyGetCoinsForOrder(String orderId, OrderStatus orderStatus, MarketLoyaltyClient marketLoyaltyClient) {
        setupLoyaltyGetCoinsForOrder(Long.parseLong(orderId), orderStatus, marketLoyaltyClient);
    }

    public static void setupLoyaltyGetCoinsForOrder(long orderId, MarketLoyaltyClient marketLoyaltyClient, CoinType coinType, OrderStatus orderStatus) {
        setupLoyaltyGetCoinsForOrder(orderId, marketLoyaltyClient, coinType, orderStatus, false);
    }

    public static void setupLoyaltyGetCoinsForOrder(long orderId, MarketLoyaltyClient marketLoyaltyClient, CoinType coinType, OrderStatus orderStatus, boolean requireAuth) {
        when(marketLoyaltyClient.getCoinsForOrder(orderId, orderStatus.name())).thenReturn(Collections.singletonList(
            new UserCoinResponse(1L, "title", "subtitle", coinType,
                coinType == CoinType.FREE_DELIVERY ? null : BigDecimal.TEN,
                "description", "inactiveDescription", COIN_CREATION_DATE, COIN_CREATION_DATE,
                    COIN_IMAGE_URL, Collections.singletonMap(SmartShoppingImageTypes.STANDARD, COIN_IMAGE_URL),
                    "#000000", CoinStatus.ACTIVE, requireAuth, ACTIVATION_TOKEN, Collections.emptyList(),
                    null, null, true, Collections.emptyList(), "reasonParam",
                    false, null, null)
            )
        );
    }

    public static void setupFuckupLoyaltyGetCoinsForOrder(long orderId, MarketLoyaltyClient marketLoyaltyClient) {
        when(marketLoyaltyClient.getCoinsForOrder(eq(orderId), anyString()))
            .thenThrow(new MarketLoyaltyException(MarketLoyaltyErrorCode.OTHER_ERROR));
    }

    public static void setupFuckupLoyaltyGetCoinsForOrder(String orderId, MarketLoyaltyClient marketLoyaltyClient) {
        setupFuckupLoyaltyGetCoinsForOrder(Long.parseLong(orderId), marketLoyaltyClient);
    }

    protected void setupCheckouterClientGetOrderReceipts(ReceiptType receiptType) {
        Receipt receipt = new Receipt();
        receipt.setId(RECEIPT_ID);
        receipt.setType(receiptType);
        when(checkouterService.getOrderReceipts(anyLong(), any(ClientRole.class), any(Long.class), any(Long.class)))
            .thenReturn(new Receipts(
                Collections.singletonList(receipt)
            ));
    }

    protected Order getOrderFromCheckouter() {
        return checkouterService.getOrder(ORDER_ID, ClientRole.SYSTEM, 0L, false);
    }

    protected Order getCancelledOrder() {
        Order cancelledOrder = getCancelledOrder(OrderSubstatus.SHOP_FAILED);
        setCancellationChangeRequest(cancelledOrder);

        return cancelledOrder;
    }

    protected Order getCancelledOrder(OrderSubstatus substatus) {
        Order order = getOrderFromCheckouter();
        order.setStatus(OrderStatus.CANCELLED);
        order.setSubstatus(substatus);
        return order;
    }

    protected void setCancellationChangeRequest(Order order) {
        ChangeRequest changeRequest = generateChangeRequest(
                ChangeRequestType.CANCELLATION,
                ChangeRequestStatus.NEW,
                Instant.now());

        order.setChangeRequests(Collections.singletonList(changeRequest));
    }

    protected void setupOrderAsFromMarket(Order order) {
        order.setShopName("Маркет");
    }

    protected Order getMarketOrder() {
        Order order = getOrderFromCheckouter();
        setupOrderAsFromMarket(order);
        return order;
    }

    protected Order getCancelledMarketOrder() {
        Order order = getCancelledOrder();
        setupOrderAsFromMarket(order);
        return order;
    }

    protected Order getOrderWithSingleShipment() {
        Order order = getOrderFromCheckouter();
        Track track = new Track("trackCode", 1L);
        track.setDeliveryServiceType(DeliveryServiceType.CARRIER);
        Parcel parcel = new Parcel();
        parcel.setTracks(Collections.singletonList(track));
        order.getDelivery().setParcels(Collections.singletonList(parcel));
        return order;
    }

    protected long scheduleAndCheck(NotificationSubtype type) {
        return scheduleAndCheck(type, null);
    }

    protected long scheduleAndCheck(NotificationSubtype type, Long subscriptionId) {
        NotificationEventSource source = schedulers.get(type).get();
        if (subscriptionId != null) {
            source.getData().put(NotificationEventDataName.SUBSCRIPTION_ID, String.valueOf(subscriptionId));
        }
        return scheduleAndCheck(source);
    }

    protected long scheduleAndCheck(NotificationEventSource source) {
        NotificationEvent event = notificationEventService.addEvent(source);
        assertNotNull(event);
        assertEquals(NotificationEventStatus.NEW, event.getStatus());
        return event.getId();
    }

    protected void sendAndCheck(long id) throws Exception {
        sendAndCheck(id, null);
    }

    protected void sendAndCheck(
        long id,
        NotificationEventConsumer consumer
    ) throws Exception {
        sendAndCheck(id, consumer, NotificationEventStatus.SENT);
    }

    protected void sendAndCheck(
        long id,
        NotificationEventConsumer consumer,
        NotificationEventStatus resultStatusExpected
    ) {
        NotificationEvent event = mailerNotificationEventService.getEvent(id);
        assertNotEquals(NotificationEventStatus.SENT, event.getStatus());
        if (consumer == null) {
            consumer = consumers.get(event.getNotificationSubtype());
        }
        NotificationEventProcessingResult result = consumer.processEvent(event);
        assertEquals(resultStatusExpected, result.getStatus());
        assertTrue(result.getPostEventAction().getAsBoolean());
    }
}
