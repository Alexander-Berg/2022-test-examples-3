package ru.yandex.market.loyalty.core.utils;

import java.util.Arrays;
import java.util.Date;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;

import ru.yandex.market.checkout.checkouter.pay.PaymentType;
import ru.yandex.market.loyalty.core.model.CoreMarketPlatform;
import ru.yandex.market.loyalty.core.model.EmissionClientDeviceType;
import ru.yandex.market.loyalty.core.model.Experiments;
import ru.yandex.market.loyalty.core.model.promo.Promo;
import ru.yandex.market.loyalty.core.model.trigger.event.BaseTriggerEvent;
import ru.yandex.market.loyalty.core.model.trigger.event.CoreOrderStatus;
import ru.yandex.market.loyalty.core.model.trigger.event.ForceCreateCouponEvent;
import ru.yandex.market.loyalty.core.model.trigger.event.ForceEmmitCouponEvent;
import ru.yandex.market.loyalty.core.model.trigger.event.LoginEvent;
import ru.yandex.market.loyalty.core.model.trigger.event.OrderStatusPredicate;
import ru.yandex.market.loyalty.core.model.trigger.event.OrderStatusUpdatedEvent;
import ru.yandex.market.loyalty.core.model.trigger.event.PromoActionEvent;
import ru.yandex.market.loyalty.core.model.trigger.event.SubscriptionEvent;
import ru.yandex.market.loyalty.core.model.trigger.event.data.OrderEventInfo;
import ru.yandex.market.pers.notify.model.NotificationType;

import static ru.yandex.market.loyalty.core.model.trigger.EventParamName.ACTION_ID;
import static ru.yandex.market.loyalty.core.model.trigger.EventParamName.CANCELLATION;
import static ru.yandex.market.loyalty.core.model.trigger.EventParamName.ORDER_ID;
import static ru.yandex.market.loyalty.core.model.trigger.EventParamName.PROMO_ID;
import static ru.yandex.market.loyalty.core.model.trigger.event.TriggerEventTypes.PROMO_ACTION_EVENT;
import static ru.yandex.market.loyalty.core.utils.UserDataFactory.DEFAULT_EMAIL;
import static ru.yandex.market.loyalty.core.utils.UserDataFactory.DEFAULT_PHONE_ID;
import static ru.yandex.market.loyalty.core.utils.UserDataFactory.DEFAULT_UID;

public class EventFactory {

    public static final CoreOrderStatus DEFAULT_ORDER_STATUS = CoreOrderStatus.LOYALTY_EFFECTIVELY_PROCESSING;
    public static final OrderStatusPredicate DEFAULT_ORDER_STATUS_PREDICATE =
            OrderStatusPredicate.EFFECTIVELY_PROCESSING;
    public static final String DEFAULT_MULTI_ORDER_ID = "1";
    public static final String DEFAULT_EVENT_UNIQUE_KEY = "some_unique_key";
    public static final String ANOTHER_EVENT_UNIQUE_KEY = "another_unique_key";
    public static final String DEFAULT_REQUEST_ID = "request_id";

    private static final AtomicLong COUNTER = new AtomicLong();

    private EventFactory() {
    }

    @SafeVarargs
    public static OrderStatusUpdatedEvent orderStatusUpdated(
            Function<OrderEventInfo.Builder, OrderEventInfo.Builder>... customizers
    ) {
        final OrderEventInfo orderEventInfo = orderEventInfoBuilder(customizers).build();
        return OrderStatusUpdatedEvent.builder()
                .setSingleOrderUniqueKey(orderEventInfo.getOrderId())
                .addPersistentData(orderEventInfo)
                .build();
    }

    @SafeVarargs
    public static OrderEventInfo.Builder orderEventInfoBuilder(
            Function<OrderEventInfo.Builder, OrderEventInfo.Builder>... customizers
    ) {
        OrderEventInfo.Builder builder = orderEventInfoWithoutItems();
        for (Function<OrderEventInfo.Builder, OrderEventInfo.Builder> customizer : customizers) {
            builder = customizer.apply(builder);
        }
        if (builder.getItems().isEmpty()) {
            builder.addItem(OrderUtil.convertOrderItem(CheckouterUtils.defaultOrderItem().build()));
        }
        return builder;
    }

    public static Function<OrderEventInfo.Builder, OrderEventInfo.Builder> withItem(
            CheckouterUtils.OrderItemBuilder itemBuilder
    ) {
        return b -> b.addItem(OrderUtil.convertOrderItem(itemBuilder.build()));
    }

    public static Function<OrderEventInfo.Builder, OrderEventInfo.Builder> withUid(long uid) {
        return b -> b.setUid(uid);
    }

    public static Function<OrderEventInfo.Builder, OrderEventInfo.Builder> noAuth() {
        return b -> b.setNoAuth(true);
    }

    public static Function<OrderEventInfo.Builder, OrderEventInfo.Builder> withExperiments(String... flags) {
        return b -> b.setExperiments(new Experiments((String.join(";", flags))));
    }

    public static Function<OrderEventInfo.Builder, OrderEventInfo.Builder> withUserSegments(String... segments) {
        return b -> b.setUserSegments(Arrays.stream(segments).collect(Collectors.toSet()));
    }

    public static Function<OrderEventInfo.Builder, OrderEventInfo.Builder> withOrderId(long orderId) {
        return b -> b.setOrderId(orderId);
    }

    public static Function<OrderEventInfo.Builder, OrderEventInfo.Builder> withDeliveryRegion(long region) {
        return b -> b.setDeliveryRegion(region);
    }

    public static Function<OrderEventInfo.Builder, OrderEventInfo.Builder> withUserEmail(String userEmail) {
        return b -> b.setEmail(userEmail);
    }

    public static Function<OrderEventInfo.Builder, OrderEventInfo.Builder> withMuid(long muid) {
        return b -> b.setMuid(muid);
    }

    public static Function<OrderEventInfo.Builder, OrderEventInfo.Builder> withPersonalPhoneId(String personalPhoneId) {
        return b -> b.setPersonalPhoneId(personalPhoneId);
    }

    public static Function<OrderEventInfo.Builder, OrderEventInfo.Builder> withMultiOrderId(
            @Nullable String multiOrderId
    ) {
        return b -> b.setMultiOrderId(multiOrderId);
    }

    public static Function<OrderEventInfo.Builder, OrderEventInfo.Builder> withPaymentType(
            PaymentType paymentType
    ) {
        return b -> b.setPaymentType(paymentType);
    }

    public static Function<OrderEventInfo.Builder, OrderEventInfo.Builder> withClientDeviceType(
            EmissionClientDeviceType clientDeviceType
    ) {
        return b -> b.setClientDeviceType(clientDeviceType);
    }

    private static OrderEventInfo.Builder orderEventInfoWithoutItems() {
        // Reserving 2 unique IDs at a time
        long baseId = COUNTER.addAndGet(2L);
        return OrderEventInfo.builder()
                .setOrderId(baseId + 1L)
                .setUid(DEFAULT_UID)
                .setUuid(UserDataFactory.DEFAULT_UUID)
                .setMuid(UserDataFactory.DEFAULT_MUID)
                .setYandexUid(UserDataFactory.DEFAULT_YANDEX_UID)
                .setPlatform(CoreMarketPlatform.BLUE)
                .setPersonalPhoneId(DEFAULT_PHONE_ID)
                .setEmail(DEFAULT_EMAIL)
                .setDeliveryRegion(213L)
                .setUserName(UserDataFactory.DEFAULT_USER_NAME)
                .setNoAuth(false)
                .setPaymentType(PaymentType.PREPAID)
                .setExperiments(Experiments.EMPTY);
    }

    public static LoginEvent createLoginEvent(long uid, @NotNull CoreMarketPlatform platform) {
        return LoginEvent.createNew(uid, null, platform, null, null, DEFAULT_REQUEST_ID);
    }

    public static BrokenLoginEvent createBrokenLoginEvent(long uid, @NotNull CoreMarketPlatform platform) {
        LoginEvent loginEvent = LoginEvent.createNew(
                uid, null, platform, null, null, DEFAULT_REQUEST_ID);
        return new BrokenLoginEvent(
                loginEvent.getId(),
                loginEvent.getParams(),
                null,
                null,
                loginEvent.getProcessTryCount(),
                loginEvent.getProcessedResult(),
                false
        );
    }

    public static LoginEvent createLoginEvent(long uid, @NotNull CoreMarketPlatform platform, String userName) {
        return LoginEvent.createNew(uid, null, platform, null, userName, DEFAULT_REQUEST_ID);
    }

    public static ForceCreateCouponEvent createForceCreateCouponEvent(String email, String clientUniqueKey) {
        return createForceCreateCouponEvent(null, email, clientUniqueKey);
    }

    public static ForceCreateCouponEvent createForceCreateCouponEvent(Long uid, String email, String clientUniqueKey) {
        return ForceCreateCouponEvent.createNew(
                uid, email, clientUniqueKey, false, DEFAULT_REQUEST_ID);
    }

    public static ForceEmmitCouponEvent createForceEmmitCouponEvent(String clientUniqueKey) {
        return ForceEmmitCouponEvent.createNew(clientUniqueKey, DEFAULT_REQUEST_ID);
    }

    public static SubscriptionEvent createSubscriptionEvent(NotificationType notificationType, String email) {
        return createSubscriptionEvent(notificationType, email, null, null);
    }

    public static SubscriptionEvent createSubscriptionEvent(
            NotificationType notificationType,
            String email,
            Date lastUnsubscribeDate,
            Long uid
    ) {
        return SubscriptionEvent.createNew(
                notificationType,
                email,
                CoreMarketPlatform.BLUE,
                uid,
                123L,
                lastUnsubscribeDate,
                DEFAULT_REQUEST_ID
        );
    }

    public static BaseTriggerEvent.Builder<PromoActionEvent> createActionEvent(
            Long orderId, Promo promo, Long actionId, boolean cancellation
    ) {
        return PromoActionEvent.builder(PROMO_ACTION_EVENT)
                .addParam(ORDER_ID, orderId)
                .addParam(PROMO_ID, promo.getPromoId().getId())
                .addParam(ACTION_ID, actionId)
                .addParam(CANCELLATION, cancellation);
    }
}
