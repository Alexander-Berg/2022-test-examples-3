package ru.yandex.market.loyalty.admin.service;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Collections;

import com.google.common.collect.ImmutableMap;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.event.HistoryEventType;
import ru.yandex.market.checkout.checkouter.order.OrderPropertyType;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.pay.PaymentType;
import ru.yandex.market.loyalty.admin.test.MarketLoyaltyAdminCheckouterEventProcessorTest;
import ru.yandex.market.loyalty.admin.tms.TriggerEventTmsProcessor;
import ru.yandex.market.loyalty.api.model.PaymentSystem;
import ru.yandex.market.loyalty.api.model.perk.PerkType;
import ru.yandex.market.loyalty.core.model.promo.CashbackLevelType;
import ru.yandex.market.loyalty.core.model.promo.RuleParameterName;
import ru.yandex.market.loyalty.core.rule.RuleType;
import ru.yandex.market.loyalty.core.service.ConfigurationService;
import ru.yandex.market.loyalty.core.service.PromoManager;
import ru.yandex.market.loyalty.core.service.blackbox.BlackboxClient;
import ru.yandex.market.loyalty.core.service.blackbox.UserInfoResponse;
import ru.yandex.market.loyalty.core.test.BlackboxUtils;
import ru.yandex.market.loyalty.core.utils.CheckouterUtils;
import ru.yandex.market.loyalty.core.utils.PromoUtils;
import ru.yandex.market.loyalty.core.utils.UserDataFactory;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasProperty;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.DELIVERY_SERVICE_RECEIVED;
import static ru.yandex.market.loyalty.core.service.ConfigurationService.YANDEX_PLUS_REQUIRED_FOR_CASHBACK_EMIT;
import static ru.yandex.market.loyalty.core.utils.CheckouterUtils.defaultOrderItem;

public class CashbackOnOrderPaidTest extends MarketLoyaltyAdminCheckouterEventProcessorTest {

    @Autowired
    private TriggerEventTmsProcessor triggerEventTmsProcessor;
    @Autowired
    private PromoManager promoManager;

    @Test
    public void shouldCorrectlyCalculatePromoOnOrderPaid() {
        configurationService.enable(ConfigurationService.YANDEX_CASHBACK_ENABLED);
        configurationService.disable(YANDEX_PLUS_REQUIRED_FOR_CASHBACK_EMIT);
        BlackboxUtils.mockBlackbox(UserDataFactory.DEFAULT_UID, PerkType.YANDEX_PLUS, true, blackboxRestTemplate);
        mockBlackbox(UserDataFactory.DEFAULT_UID, true, true);

        promoManager.createCashbackPromo(
                PromoUtils.Cashback.defaultFixed(BigDecimal.TEN, CashbackLevelType.MULTI_ORDER)
                        .addCashbackRule(RuleType.ONLY_ONLINE_CARD_PAYMENT_FILTER_RULE,
                                RuleParameterName.CLIENT_ONLINE_CARD_PAYMENT_SYSTEM, PaymentSystem.MASTERCARD)
        );

        processEvent(
                CheckouterUtils.defaultOrder(OrderStatus.PROCESSING)
                        .setOrdersCount(1)
                        .setOrderId(1L)
                        .setOrderSubstatus(DELIVERY_SERVICE_RECEIVED)
                        .setPaymentType(PaymentType.PREPAID)
                        .setDeliveryType(DeliveryType.DELIVERY)
                        .setNoAuth(false)
                        .setUid(UserDataFactory.DEFAULT_UID)
                        .setProperty(OrderPropertyType.PAYMENT_SYSTEM, "MasterCard")
                        .addItem(defaultOrderItem().build())
                        .build(),
                HistoryEventType.ORDER_STATUS_UPDATED
        );
        triggerEventTmsProcessor.processTriggerEvents(Duration.ZERO);
    }

    private void mockBlackbox(long uid, boolean isYandexEmployee, boolean isYAPlus) {
        UserInfoResponse.User user = new UserInfoResponse.User();
        if (isYandexEmployee) {
            user.setDbfields(ImmutableMap.of(BlackboxClient.STAFF_LOGIN, "someLogin"));
        }
        user.setAttributes(ImmutableMap.of(BlackboxClient.YANDEX_PLUS_FLAG_NAME_MAGIC, isYAPlus ? "1" : "0"));
        user.setId(String.valueOf(uid));
        when(blackboxRestTemplate.exchange(
                argThat(hasProperty("query", containsString("uid=" + uid))),
                eq(HttpMethod.GET),
                eq(HttpEntity.EMPTY),
                eq(UserInfoResponse.class)
        )).thenReturn(ResponseEntity.ok(
                new UserInfoResponse(Collections.singletonList(user)
                )
        ));
    }
}
