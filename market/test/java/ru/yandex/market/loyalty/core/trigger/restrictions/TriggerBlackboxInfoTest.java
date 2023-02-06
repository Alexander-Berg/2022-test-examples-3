package ru.yandex.market.loyalty.core.trigger.restrictions;

import java.net.SocketTimeoutException;
import java.time.Duration;
import java.util.Collections;
import java.util.List;

import com.google.common.collect.ImmutableMap;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.ResourceAccessException;

import ru.yandex.market.loyalty.core.dao.trigger.TriggerActionResult;
import ru.yandex.market.loyalty.core.model.CoreMarketPlatform;
import ru.yandex.market.loyalty.core.model.coin.Coin;
import ru.yandex.market.loyalty.core.model.promo.BudgetMode;
import ru.yandex.market.loyalty.core.model.promo.Promo;
import ru.yandex.market.loyalty.core.model.trigger.Trigger;
import ru.yandex.market.loyalty.core.model.trigger.event.OrderStatusUpdatedEvent;
import ru.yandex.market.loyalty.core.service.PromoManager;
import ru.yandex.market.loyalty.core.service.blackbox.BlackboxClient;
import ru.yandex.market.loyalty.core.service.blackbox.UserInfoResponse;
import ru.yandex.market.loyalty.core.service.coin.CoinService;
import ru.yandex.market.loyalty.core.service.trigger.TriggerEventQueueService;
import ru.yandex.market.loyalty.core.test.MarketLoyaltyCoreMockedDbTestBase;
import ru.yandex.market.loyalty.core.trigger.actions.ProcessResultUtils;
import ru.yandex.market.loyalty.core.utils.DiscountUtils;
import ru.yandex.market.loyalty.core.utils.EventFactory;
import ru.yandex.market.loyalty.core.utils.PromoUtils;
import ru.yandex.market.loyalty.core.utils.TriggersFactory;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;
import static ru.yandex.market.loyalty.core.dao.trigger.TriggerActionResultStatus.SUCCESS;
import static ru.yandex.market.loyalty.core.model.TriState.FALSE;
import static ru.yandex.market.loyalty.core.model.TriState.TRUE;
import static ru.yandex.market.loyalty.core.model.TriState.UNSET;
import static ru.yandex.market.loyalty.core.utils.EventFactory.noAuth;
import static ru.yandex.market.loyalty.core.utils.EventFactory.withUid;
import static ru.yandex.market.loyalty.core.utils.UserDataFactory.ANOTHER_UID;
import static ru.yandex.market.loyalty.core.utils.UserDataFactory.DEFAULT_UID;
import static ru.yandex.market.loyalty.core.utils.UserDataFactory.THIRD_UID;

public class TriggerBlackboxInfoTest extends MarketLoyaltyCoreMockedDbTestBase {
    @Autowired
    private PromoManager promoManager;
    @Autowired
    private TriggerEventQueueService triggerEventQueueService;
    @Autowired
    private TriggersFactory triggersFactory;
    @Autowired
    private ProcessResultUtils processResultUtils;
    @Autowired
    private CoinService coinService;
    @Autowired
    private DiscountUtils discountUtils;

    @Test
    public void shouldCreateCoinOnlyForYandexEmployee() {
        Promo promo = promoManager.createSmartShoppingPromo(PromoUtils.SmartShopping.defaultFixed());

        triggersFactory.createCoinTriggerWithBlackboxRestriction(
                promo,
                BlackboxInfoRestrictionDto.builder().setIsEmployee(TRUE).setIsYaPlus(UNSET).build()
        );

        mockBlackbox(DEFAULT_UID, true, false);
        mockBlackbox(ANOTHER_UID, false, false);

        triggerEventQueueService.addEventToQueue(EventFactory.orderStatusUpdated(withUid(DEFAULT_UID)));
        triggerEventQueueService.addEventToQueue(EventFactory.orderStatusUpdated(withUid(ANOTHER_UID)));
        triggerEventQueueService.processEventsFromQueue(Duration.ZERO);

        assertThat(coinService.search.getInactiveCoinsByUid(DEFAULT_UID, CoreMarketPlatform.BLUE), not(empty()));
        assertThat(coinService.search.getInactiveCoinsByUid(ANOTHER_UID, CoreMarketPlatform.BLUE), is(empty()));
    }

    /**
     * TODO rewrite with parameterized
     */
    @Test
    public void shouldCreateCoinOnlyForNoAuth() {
        Promo promo = promoManager.createSmartShoppingPromo(PromoUtils.SmartShopping.defaultFixed());

        Trigger<OrderStatusUpdatedEvent> triggerForNotEmployee =
                triggersFactory.createCoinTriggerWithBlackboxRestriction(
                promo,
                BlackboxInfoRestrictionDto.builder().setIsEmployee(FALSE).setIsYaPlus(UNSET).build()
        );

        Trigger<OrderStatusUpdatedEvent> triggerForNotYaPlus = triggersFactory.createCoinTriggerWithBlackboxRestriction(
                promo,
                BlackboxInfoRestrictionDto.builder().setIsEmployee(UNSET).setIsYaPlus(FALSE).build()
        );

        Trigger<OrderStatusUpdatedEvent> triggerForNotYaPlusAndNotEmployee =
                triggersFactory.createCoinTriggerWithBlackboxRestriction(
                promo,
                BlackboxInfoRestrictionDto.builder().setIsEmployee(FALSE).setIsYaPlus(FALSE).build()
        );

        triggersFactory.createCoinTriggerWithBlackboxRestriction(
                promo,
                BlackboxInfoRestrictionDto.builder().setIsEmployee(UNSET).setIsYaPlus(TRUE).build()
        );

        triggersFactory.createCoinTriggerWithBlackboxRestriction(
                promo,
                BlackboxInfoRestrictionDto.builder().setIsEmployee(TRUE).setIsYaPlus(UNSET).build()
        );

        assertThat(
                triggerEventQueueService.insertAndProcessEvent(
                        EventFactory.orderStatusUpdated(noAuth(), b -> b.setUid(null)),
                        discountUtils.getRulesPayload(), BudgetMode.SYNC),
                containsInAnyOrder(
                        allOf(
                                hasProperty("triggerId", equalTo(triggerForNotEmployee.getId())),
                                hasProperty("status", equalTo(SUCCESS))
                        ),
                        allOf(
                                hasProperty("triggerId", equalTo(triggerForNotYaPlus.getId())),
                                hasProperty("status", equalTo(SUCCESS))
                        ),
                        allOf(
                                hasProperty("triggerId", equalTo(triggerForNotYaPlusAndNotEmployee.getId())),
                                hasProperty("status", equalTo(SUCCESS))
                        )
                )
        );
    }

    @Test
    public void shouldCreateCoinOnlyForYaPlusButNotEmployee() {
        Promo promo = promoManager.createSmartShoppingPromo(PromoUtils.SmartShopping.defaultFixed());

        triggersFactory.createCoinTriggerWithBlackboxRestriction(
                promo,
                BlackboxInfoRestrictionDto.builder().setIsEmployee(FALSE).setIsYaPlus(TRUE).build()
        );

        mockBlackbox(DEFAULT_UID, false, true);
        mockBlackbox(ANOTHER_UID, true, true);
        mockBlackbox(THIRD_UID, false, false);

        triggerEventQueueService.addEventToQueue(EventFactory.orderStatusUpdated(withUid(DEFAULT_UID)));
        triggerEventQueueService.addEventToQueue(EventFactory.orderStatusUpdated(withUid(ANOTHER_UID)));
        triggerEventQueueService.addEventToQueue(EventFactory.orderStatusUpdated(withUid(THIRD_UID)));
        triggerEventQueueService.processEventsFromQueue(Duration.ZERO);

        assertThat(coinService.search.getInactiveCoinsByUid(DEFAULT_UID, CoreMarketPlatform.BLUE), not(empty()));
        assertThat(coinService.search.getInactiveCoinsByUid(ANOTHER_UID, CoreMarketPlatform.BLUE), is(empty()));
        assertThat(coinService.search.getInactiveCoinsByUid(THIRD_UID, CoreMarketPlatform.BLUE), is(empty()));
    }

    @Test
    public void shouldHandleBlackboxException() {
        Promo promo = promoManager.createSmartShoppingPromo(PromoUtils.SmartShopping.defaultFixed());

        triggersFactory.createCoinTriggerWithBlackboxRestriction(
                promo,
                BlackboxInfoRestrictionDto.builder().setIsEmployee(TRUE).setIsYaPlus(TRUE).build()
        );

        when(blackboxRestTemplate.getForObject(
                argThat(hasProperty("query", containsString("uid=" + DEFAULT_UID))),
                eq(UserInfoResponse.class)
        )).thenThrow(new ResourceAccessException("network is unreachable", new SocketTimeoutException()));

        List<TriggerActionResult> results = triggerEventQueueService.insertAndProcessEvent(
                EventFactory.orderStatusUpdated(withUid(DEFAULT_UID)),
                discountUtils.getRulesPayload(), BudgetMode.SYNC);

        assertThat(processResultUtils.request(results, Coin.class), is(empty()));
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
