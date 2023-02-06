package ru.yandex.market.pers.notify.ems.consumer;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.currency.Currency;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.checkout.checkouter.receipt.ReceiptType;
import ru.yandex.market.loyalty.api.model.CouponDto;
import ru.yandex.market.loyalty.api.model.CouponStatus;
import ru.yandex.market.loyalty.client.MarketLoyaltyClient;
import ru.yandex.market.pers.grade.client.dto.mailer.MailerModelGrade;
import ru.yandex.market.pers.grade.client.dto.mailer.MailerShopGrade;
import ru.yandex.market.pers.notify.SubscriptionsCacher;
import ru.yandex.market.pers.notify.ems.event.NotificationEvent;
import ru.yandex.market.pers.notify.ems.persistence.EventSourceDAO;
import ru.yandex.market.pers.notify.mock.MarketMailerMockFactory;
import ru.yandex.market.pers.notify.model.NotificationSubtype;
import ru.yandex.market.pers.notify.model.NotificationType;
import ru.yandex.market.pers.notify.model.Uid;
import ru.yandex.market.pers.notify.model.YandexUid;
import ru.yandex.market.pers.notify.model.event.NotificationEventStatus;
import ru.yandex.market.pers.notify.model.subscription.EmailSubscription;
import ru.yandex.market.pers.notify.model.subscription.EmailSubscriptionParam;
import ru.yandex.market.pers.notify.model.subscription.EmailSubscriptionStatus;
import ru.yandex.market.report.model.Model;
import ru.yandex.market.report.model.Prices;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;
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
import static ru.yandex.market.pers.notify.model.NotificationSubtype.REFEREE_REFUND_STATEMENT;
import static ru.yandex.market.pers.notify.model.NotificationSubtype.SHOP_GRADE;

/**
 * Для того чтобы отошли все письма нужно:
 * <p>
 * 1. Убрать моки для senderClient, baseMailer, esApiClient; добавить реальные бины
 * 2. Установить нужные для них проперти в test-application.properties (sender можно через тестинг, es и yabacks - через прод)
 * 3. На macOS проблемы с SSL сертификатом для рассылятора, годный гайд как исправить:
 * http://commandlinefanatic.com/cgi-bin/showarticle.cgi?article=art032
 *
 * @author Ivan Anisimov
 * valter@yandex-team.ru
 * 20.04.17
 */
public class SendNotificationTest extends SendNotificationTestCommons {
    @Autowired
    private MarketLoyaltyClient marketLoyaltyClient;
    @Autowired
    private EventSourceDAO eventSourceDAO;
    @Autowired
    private SubscriptionsCacher subscriptionsCacher;

    @Test
    public void testORDER_PAID_RECEIPT_PRINTED() throws Exception {
        setupCheckouterClientGetOrderReceipts(ReceiptType.INCOME);
        long id = scheduleAndCheck(ORDER_PAID_RECEIPT_PRINTED);
        sendAndCheck(id);
    }

    @Test
    public void testCOUPON_FOR_PROMOTER_SUBSCRIPTION() throws Exception {
        when(marketLoyaltyClient.generateCoupon(any()))
            .thenReturn(new CouponDto(UUID.randomUUID().toString(), CouponStatus.INACTIVE));
        long id = scheduleAndCheck(COUPON_FOR_PROMOTER_SUBSCRIPTION);
        sendAndCheck(id);
    }

    @Test
    public void testORDER_RETURN_RECEIPT_PRINTED() throws Exception {
        setupCheckouterClientGetOrderReceipts(ReceiptType.INCOME_RETURN);
        long id = scheduleAndCheck(ORDER_RETURN_RECEIPT_PRINTED);
        sendAndCheck(id);
    }

    @Test
    public void testBLUE_ORDER_RETURN_RECEIPT_PRINTED() throws Exception {
        setupCheckouterClientGetOrderReceipts(ReceiptType.INCOME_RETURN);
        long id = scheduleAndCheck(BLUE_ORDER_RETURN_RECEIPT_PRINTED);
        sendAndCheck(id);
    }

    @Test
    public void testBLUE_ORDER_CASH_RETURN_RECEIPT_PRINTED() throws Exception {
        setupCheckouterClientGetOrderReceipts(ReceiptType.INCOME_RETURN);
        long id = scheduleAndCheck(BLUE_ORDER_CASH_RETURN_RECEIPT_PRINTED);
        sendAndCheck(id);
    }


    @Test
    public void testNEW_COMMENT_GRADE() throws Exception {
        long id = scheduleAndCheck(NEW_COMMENT_GRADE);
        sendAndCheck(id);
    }

    @Test
    public void testORDER_CANCELLED() throws Exception {
        setupCheckouterClientGetOrder(getCancelledOrder());
        long id = scheduleAndCheck(ORDER_CANCELLED);
        sendAndCheck(id);
    }

    @Test
    public void testBLUE_ORDER_CANCELLED() throws Exception {
        setupCheckouterClientGetOrder(getCancelledOrder());
        long id = scheduleAndCheck(BLUE_ORDER_CANCELLED);
        sendAndCheck(id);
    }

    @Test
    public void testORDER_CANCELLED_MARKET() throws Exception {
        setupCheckouterClientGetOrder(getCancelledMarketOrder());
        long id = scheduleAndCheck(ORDER_CANCELLED);
        sendAndCheck(id, checkoutOrderSenderConsumer);
    }

    @Test
    public void testBLUE_ORDER_PENDING() throws Exception {
        Order order = getOrderFromCheckouter();
        order.setStatus(OrderStatus.PENDING);
        order.setSubstatus(OrderSubstatus.AWAIT_CONFIRMATION);
        setupCheckouterClientGetOrder(order);
        long id = scheduleAndCheck(BLUE_ORDER_PENDING);
        sendAndCheck(id);
    }

    @Test
    public void testBLUE_ORDER_PENDING_WITH_COIN() throws Exception {
        Order order = getOrderFromCheckouter();
        order.setStatus(OrderStatus.PENDING);
        order.setSubstatus(OrderSubstatus.AWAIT_CONFIRMATION);
        setupCheckouterClientGetOrder(order);
        setupLoyaltyGetCoinsForOrder(order.getId(), OrderStatus.PENDING);
        long id = scheduleAndCheck(BLUE_ORDER_PENDING);
        sendAndCheck(id);
    }

    @Test
    public void testBLUE_ORDER_DELIVERY() throws Exception {
        setupCheckouterClientGetOrder(getOrderWithSingleShipment());
        long id = scheduleAndCheck(BLUE_ORDER_DELIVERY);
        sendAndCheck(id);
    }

    @Test
    public void testBLUE_ORDER_DELIVERY_POST_BOX() throws Exception {
        long id = scheduleAndCheck(BLUE_ORDER_DELIVERY_POST_BOX);
        sendAndCheck(id);
    }

    @Test
    public void testORDER_DELIVERED() throws Exception {
        long id = scheduleAndCheck(ORDER_DELIVERED);
        sendAndCheck(id);
    }

    @Test
    public void testBLUE_ORDER_DELIVERED() throws Exception {
        long id = scheduleAndCheck(BLUE_ORDER_DELIVERED);
        sendAndCheck(id);
    }

    @Test
    public void testBLUE_ORDER_DELIVERED_WITH_COIN() throws Exception {
        setupLoyaltyGetCoinsForOrder(ORDER_ID, OrderStatus.DELIVERED);
        long id = scheduleAndCheck(BLUE_ORDER_DELIVERED);
        sendAndCheck(id);
    }

    @Test
    public void testORDER_DELIVERED_MARKET() throws Exception {
        setupCheckouterClientGetOrder(getMarketOrder());
        long id = scheduleAndCheck(ORDER_DELIVERED);
        sendAndCheck(id, checkoutOrderSenderConsumer);
    }

    @Test
    public void testORDER_DELIVERED_CHECK() throws Exception {
        long id = scheduleAndCheck(ORDER_DELIVERED_CHECK);
        sendAndCheck(id);
    }

    @Test
    public void testORDER_PICKUP() throws Exception {
        long id = scheduleAndCheck(ORDER_PICKUP);
        sendAndCheck(id);
    }

    @Test
    public void testBLUE_ORDER_PICKUP() throws Exception {
        long id = scheduleAndCheck(BLUE_ORDER_PICKUP);
        sendAndCheck(id);
    }

    @Test
    public void testORDER_PICKUP_MARKET() throws Exception {
        setupCheckouterClientGetOrder(getMarketOrder());
        long id = scheduleAndCheck(ORDER_PICKUP);
        sendAndCheck(id);
    }

    @Test
    public void testORDER_PROCESSING() throws Exception {
        long id = scheduleAndCheck(ORDER_PROCESSING);
        sendAndCheck(id);
    }

    @Test
    public void testORDER_PROCESSING_MARKET() throws Exception {
        setupCheckouterClientGetOrder(getMarketOrder());
        long id = scheduleAndCheck(ORDER_PROCESSING);
        sendAndCheck(id);
    }

    @Test
    public void testORDER_UNPAID() throws Exception {
        testORDER_UNPAID(ORDER_UNPAID, null);
    }

    @Test
    public void testBLUE_ORDER_UNPAID() throws Exception {
        testORDER_UNPAID(BLUE_ORDER_UNPAID, null);
    }

    @Test
    public void testORDER_UNPAID_WAITING_USER_DELIVERY_INPUT() throws Exception {
        testORDER_UNPAID(ORDER_UNPAID_WAITING_USER_DELIVERY_INPUT, OrderSubstatus.WAITING_USER_DELIVERY_INPUT);
    }

    protected void testORDER_UNPAID(NotificationSubtype subtype, OrderSubstatus subStatus) throws Exception {
        Order order = getOrderFromCheckouter();
        order.setStatus(OrderStatus.UNPAID);
        order.setSubstatus(subStatus);
        setupCheckouterClientGetOrder(order);
        long id = scheduleAndCheck(subtype);
        sendAndCheck(id);
    }

    @Test
    public void testORDER_UNPAID_MARKET() throws Exception {
        Order order = getMarketOrder();
        order.setStatus(OrderStatus.UNPAID);
        setupCheckouterClientGetOrder(order);
        long id = scheduleAndCheck(ORDER_UNPAID);
        sendAndCheck(id);
    }

    @Test
    public void testORDER_DELIVERY_CHANGE_DIFF() throws Exception {
        long id = scheduleAndCheck(ORDER_DELIVERY_CHANGE_DIFF);
        sendAndCheck(id);
    }

    @Test
    public void testORDER_STRUCTURE_CHANGE_DIFF() throws Exception {
        long id = scheduleAndCheck(ORDER_STRUCTURE_CHANGE_DIFF);
        sendAndCheck(id);
    }

    @Test
    public void testORDER_DELIVERY_WITH_TRACKING() throws Exception {
        long id = scheduleAndCheck(ORDER_DELIVERY_WITH_TRACKING);
        sendAndCheck(id);
    }

    @Test
    public void testORDER_TRACKING_PARCEL_PICKUP_WITHOUT_TRACKING() throws Exception {
        long id = scheduleAndCheck(ORDER_TRACKING_PARCEL_PICKUP_WITHOUT_TRACKING);
        sendAndCheck(id);
    }

    @Test
    public void testORDER_TRACKING_PARCEL_DELIVERY_WITHOUT_TRACKING() throws Exception {
        long id = scheduleAndCheck(ORDER_TRACKING_PARCEL_DELIVERY_WITHOUT_TRACKING);
        sendAndCheck(id);
    }

    @Test
    public void testBLUE_ORDER_DELIVERY_TRANSPORTATION_RECIPIENT() throws Exception {
        long id = scheduleAndCheck(BLUE_ORDER_DELIVERY_TRANSPORTATION_RECIPIENT);
        sendAndCheck(id);
    }

    @Test
    public void testORDER_TRACKING_PARCEL_PICKUP_WITH_TRACKING() throws Exception {
        long id = scheduleAndCheck(ORDER_TRACKING_PARCEL_PICKUP_WITH_TRACKING);
        sendAndCheck(id);
    }

    @Test
    public void testORDER_TRACKING_PARCEL_DELIVERY_WITH_TRACKING() throws Exception {
        long id = scheduleAndCheck(ORDER_TRACKING_PARCEL_DELIVERY_WITH_TRACKING);
        sendAndCheck(id);
    }

    @Test
    public void testPROMO_MISC() throws Exception {
        long id = scheduleAndCheck(PROMO_MISC);
        sendAndCheck(id);
    }
    @Test
    public void testBLUE_PROMO_MISC() throws Exception {
        long id = scheduleAndCheck(BLUE_PROMO_MISC);
        sendAndCheck(id);
    }

    @Test
    public void testORDER_DELIVERY_WITHOUT_TRACKING() throws Exception {
        long id = scheduleAndCheck(ORDER_DELIVERY_WITHOUT_TRACKING);
        sendAndCheck(id);
    }

    @Test
    public void testORDER_DELIVERY_CHANGE_WITH_TRACKING() throws Exception {
        long id = scheduleAndCheck(ORDER_DELIVERY_CHANGE_WITH_TRACKING);
        sendAndCheck(id);
    }

    @Test
    public void testNEW_GRADE_MODEL_COMMENT() throws Exception {
        long id = scheduleAndCheck(NEW_GRADE_MODEL_COMMENT);
        sendAndCheck(id);
    }

    @Test
    public void testNEW_GRADE_MODEL_COMMENT_ON_COMMENT() throws Exception {
        MailerModelGrade grade = gradeClient.getModelGradeForMailer(Long.parseLong(GRADE_ID));
        grade.setModelId(2L);
        when(gradeClient.getModelGradeForMailer(Long.parseLong(GRADE_ID))).thenReturn(grade);

        long id = scheduleAndCheck(NEW_GRADE_MODEL_COMMENT_ON_COMMENT);
        sendAndCheck(id);
    }

    @Test
    public void testPA_WELCOME() throws Exception {
        subscriptionAndIdentityService.createSubscriptions(EMAIL, Collections.singletonList(EmailSubscription.builder()
            .setSubscriptionType(NotificationType.PA_ON_SALE)
            .setEmail(EMAIL)
            .setSubscriptionStatus(EmailSubscriptionStatus.CONFIRMED)
            .addParameter(EmailSubscriptionParam.PARAM_MODEL_ID, PA_WELCOME_MODEL_ID)
            .addParameter(EmailSubscriptionParam.PARAM_REGION_ID, PA_WELCOME_REGION_ID)
            .addParameter(EmailSubscriptionParam.PARAM_PRICE, "0")
            .setUid(UID)
            .build()
        ), new Uid(UID), true);
        when(reportService.getOffersByModelWithPreorders(
            eq(Integer.parseInt(PA_WELCOME_REGION_ID)), eq(PA_WELCOME_CURRENCY), eq(Long.parseLong(PA_WELCOME_MODEL_ID)),
            anyInt(), anyInt())
        ).thenReturn(Collections.emptyList());
        long id = scheduleAndCheck(PA_WELCOME);
        sendAndCheck(id);
    }

    @Test
    public void testPA_EXIST_ON_SALE() throws Exception {
        EmailSubscription emailSubscription = EmailSubscription.builder()
            .setSubscriptionType(NotificationType.PA_ON_SALE)
            .setEmail(EMAIL)
            .setSubscriptionStatus(EmailSubscriptionStatus.CONFIRMED)
            .setUid(UID)
            .addParameter(EmailSubscriptionParam.PARAM_MODEL_ID, randomIntString())
            .build();
        subscriptionAndIdentityService.createSubscriptions(EMAIL, Collections.singletonList(emailSubscription),
            new Uid(UID), true);
        long id = scheduleAndCheck(PA_EXIST_ON_SALE, emailSubscription.getId());
        sendAndCheck(id);
    }

    @Test
    public void testPRICE_DROP_FOUND() throws Exception {
        EmailSubscription emailSubscription = EmailSubscription.builder()
            .setSubscriptionType(NotificationType.PRICE_DROP)
            .setEmail(EMAIL)
            .setSubscriptionStatus(EmailSubscriptionStatus.CONFIRMED)
            .setUid(UID)
            .addParameter(EmailSubscriptionParam.PARAM_MODEL_ID, randomIntString())
            .addParameter(EmailSubscriptionParam.PARAM_PRICE, "0")
            .addParameter(EmailSubscriptionParam.PARAM_CURRENCY, "RUR")
            .build();
        subscriptionAndIdentityService.createSubscriptions(EMAIL, Collections.singletonList(emailSubscription),
            new Uid(UID), true);
        Model model = MarketMailerMockFactory.generateModel();
        model.setPrices(new Prices(0.0, 0.0, 0.0, BigDecimal.ZERO, Currency.RUR));
        when(reportService.getModelByIdWithPreorders(anyLong(), anyLong(),
            any(Currency.class))
        ).thenReturn(Optional.of(model));
        long id = scheduleAndCheck(PRICE_DROP_FOUND, emailSubscription.getId());
        sendAndCheck(id);
    }

    @Test
    public void testSHOP_GRADE() throws Exception {
        MailerShopGrade grade = gradeClient.getShopGradeForMailer(Long.parseLong(GRADE_ID), true);
        grade.setShopId(1L);
        when(gradeClient.getShopGradeForMailer(Long.parseLong(GRADE_ID), true)).thenReturn(grade);
        long id = scheduleAndCheck(SHOP_GRADE);
        sendAndCheck(id);
    }

    @Test
    public void testCONFIRM_SUBSCRIPTION() throws Exception {
        subscriptionAndIdentityService.createSubscriptions(EMAIL, Collections.singletonList(EmailSubscription.builder()
            .setSubscriptionType(NotificationType.ADVERTISING)
            .setEmail(EMAIL)
            .setSubscriptionStatus(EmailSubscriptionStatus.NEED_SEND_CONFIRMATION)
            .addParameter(EmailSubscriptionParam.PARAM_USER_NAME, "SubscriptionName")
            .build()
        ), new YandexUid("0"));
        long id = scheduleAndCheck(CONFIRM_SUBSCRIPTION);
        sendAndCheck(id);
    }

    @Test
    public void testShouldNotSendRepeatedCONFIRM_SUBSCRIPTION() {
        subscriptionsCacher.saveSubscriptions(EMAIL, Collections.singletonList(EmailSubscription.builder()
                .setSubscriptionType(NotificationType.ADVERTISING)
                .setEmail(EMAIL)
                .setSubscriptionStatus(EmailSubscriptionStatus.CONFIRMED)
                .addParameter(EmailSubscriptionParam.PARAM_USER_NAME, "SubscriptionName")
                .build()));
        subscriptionAndIdentityService.createSubscriptions(EMAIL, Collections.singletonList(EmailSubscription.builder()
                .setSubscriptionType(NotificationType.ADVERTISING)
                .setEmail(EMAIL)
                .setSubscriptionStatus(EmailSubscriptionStatus.NEED_SEND_CONFIRMATION)
                .addParameter(EmailSubscriptionParam.PARAM_USER_NAME, "SubscriptionName")
                .build()), new YandexUid("1"));
        NotificationEvent event = eventSourceDAO.getLastEventByEmail(EMAIL);
        assertNull(event);
    }

    @Test
    public void testCART_1() throws Exception {
        long id = scheduleAndCheck(CART_1);
        sendAndCheck(id);
    }

    @Test
    public void testGRADE_MODEL_AFTER_ORDER_COUPON_ACTIVATED() throws Exception {
        long id = scheduleAndCheck(GRADE_MODEL_AFTER_ORDER_COUPON_ACTIVATED);
        sendAndCheck(id);
    }

    @Test
    public void testREFEREE_REFUND_STATEMENT() throws Exception {
        long id = scheduleAndCheck(REFEREE_REFUND_STATEMENT);
        sendAndCheck(id);
    }

    @Test
    public void testTRANSBOUNDARY_TRADING_ORDER_PAID() throws Exception {
        setupCheckouterClientGetOrder(getOrderWithSingleShipment());
        long id = scheduleAndCheck(NotificationSubtype.TRANSBOUNDARY_TRADING_ORDER_PAID);
        sendAndCheck(id);
    }

    @Test
    public void testTRANSBOUNDARY_TRADING_ORDER_CANCELLED() throws Exception {
        setupCheckouterClientGetOrder(getCancelledOrder());
        long id = scheduleAndCheck(NotificationSubtype.TRANSBOUNDARY_TRADING_ORDER_CANCELLED);
        sendAndCheck(id);
    }

    @Test
    public void testTRANSBOUNDARY_TRADING_ORDER_CANCELLED_BY_USER() throws Exception {
        setupCheckouterClientGetOrder(getCancelledOrder(OrderSubstatus.PENDING_CANCELLED));
        long id = scheduleAndCheck(NotificationSubtype.TRANSBOUNDARY_TRADING_ORDER_CANCELLED_BY_USER);
        sendAndCheck(id);
    }

    @Test
    public void testTRANSBOUNDARY_TRADING_ORDER_DELIVERY() throws Exception {
        setupCheckouterClientGetOrder(getOrderWithSingleShipment());
        long id = scheduleAndCheck(NotificationSubtype.TRANSBOUNDARY_TRADING_ORDER_DELIVERY);
        sendAndCheck(id);
    }

    @Test
    public void testTRANSBOUNDARY_TRADING_ORDER_PICKUP() throws Exception {
        long id = scheduleAndCheck(NotificationSubtype.TRANSBOUNDARY_TRADING_ORDER_PICKUP);
        sendAndCheck(id);
    }

    @Test
    public void testTRANSBOUNDARY_TRADING_ORDER_DELIVERED() throws Exception {
        long id = scheduleAndCheck(NotificationSubtype.TRANSBOUNDARY_TRADING_ORDER_DELIVERED);
        sendAndCheck(id);
    }

    @Test
    public void testDIGITAL_ORDER_DELIVERY() throws Exception {
        Order order = getOrderFromCheckouter();
        order.setStatus(OrderStatus.DELIVERY);
        order.getDelivery().setType(DeliveryType.DIGITAL);
        setupCheckouterClientGetOrder(order);
        long id = scheduleAndCheck(DIGITAL_ORDER_DELIVERY);
        sendAndCheck(id);
    }
}
