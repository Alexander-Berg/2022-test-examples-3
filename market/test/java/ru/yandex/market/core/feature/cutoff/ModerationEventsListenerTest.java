package ru.yandex.market.core.feature.cutoff;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;

import ru.yandex.market.checkout.checkouter.client.CheckouterAPI;
import ru.yandex.market.checkout.checkouter.client.CheckouterShopApi;
import ru.yandex.market.checkout.checkouter.shop.ShopMetaData;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.moderation.event.ModerationEvent;
import ru.yandex.market.core.param.ParamService;
import ru.yandex.market.core.param.model.BooleanParamValue;
import ru.yandex.market.core.param.model.ParamType;
import ru.yandex.market.core.testing.TestingType;
import ru.yandex.market.mbi.environment.EnvironmentService;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

/**
 * Тесты для {@link ModerationEventsListener}
 */
@DbUnitDataSet(before = {
        "ModerationEventsListenerTest.common.before.csv",
        "successMarketplaceSelfDeliveryFeature.before.csv"
})
class ModerationEventsListenerTest extends FunctionalTest {

    private static final long SHOP_ID = 100;
    private static final long PI_DBS_ID = 102;

    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;

    @Autowired
    private EnvironmentService environmentService;

    @Autowired
    private ParamService paramService;

    @Autowired
    private CheckouterAPI checkouterClient;

    @Autowired
    private CheckouterShopApi checkouterShopApi;

    @Autowired
    Clock clock;

    @Test
    @DbUnitDataSet(
            before = "ModerationEventsListenerTest.start_fail_moderation.fatal_cutoffs.before.csv"
    )
    @DisplayName("Проверка, что магазин не может перейти к модерации, т.к. есть открытые фатальные катофы")
    void testFailStartModeration() {
        assertThrows(IllegalStateException.class,
                () -> sendEvent(ModerationEvent.ModerationEventType.START, TestingType.CPA_PREMODERATION));
    }

    @Test
    @DbUnitDataSet(
            before = "ModerationEventsListenerTest.self_check_to_new.before.csv",
            after = "ModerationEventsListenerTest.self_check_to_new.after.csv"
    )
    @DisplayName("Проверка, что магазин уходит на модерации (в статус NEW), при старте SELF_CHECK, если программа находится в статусе DONT_WANT")
    void testTransitionToNewForSelfCheckOnlyFromDontWant() {
        when(clock.instant()).thenReturn(Clock.systemDefaultZone().instant());
        sendEvent(ModerationEvent.ModerationEventType.START, TestingType.SELF_CHECK);
    }

    @Test
    @DbUnitDataSet(
            before = "ModerationEventsListenerTest.self_check_from_fail.before.csv",
            after = "ModerationEventsListenerTest.self_check_from_fail.after.csv"
    )
    @DisplayName("Проверка, что магазин не меняет статус с FAIL при старте SELF_CHECK")
    void testNoTransitionToNewFromFailForSelfCheck() {
        sendEvent(ModerationEvent.ModerationEventType.START, TestingType.SELF_CHECK);
    }

    @Test
    @DbUnitDataSet(
            before = "ModerationEventsListenerTest.cpa_premoderation_from_fail.before.csv",
            after = "ModerationEventsListenerTest.cpa_premoderation_from_fail.after.csv"
    )
    @DisplayName("Проверка, что магазин переходит в статус NEW из FAIL после старта CPA_PREMODERATION")
    void testTransitionToNewFromFailForCpa() {
        when(clock.instant()).thenReturn(Clock.systemDefaultZone().instant());
        sendEvent(ModerationEvent.ModerationEventType.START, TestingType.CPA_PREMODERATION);
    }

    @Test
    @DbUnitDataSet(
            before = "ModerationEventsListenerTest.self_check_delete.before.csv",
            after = "ModerationEventsListenerTest.self_check_delete.after.csv"
    )
    void testSkipSelfcheckResults() {
        when(checkouterClient.shops()).thenReturn(checkouterShopApi);
        when(checkouterShopApi.getShopData(anyLong())).thenReturn(ShopMetaData.DEFAULT);
        Clock fixedClock = Clock.fixed(
                LocalDate.of(2021, 5, 28)
                        .atStartOfDay(ZoneId.systemDefault())
                        .toInstant(),
                ZoneId.systemDefault()
        );
        when(clock.instant()).thenReturn(fixedClock.instant());
        when(clock.getZone()).thenReturn(fixedClock.getZone());

        paramService.setParam(
                new BooleanParamValue(ParamType.CPA_IS_PARTNER_INTERFACE, PI_DBS_ID, true),
                100500
        );
        sendEvent(PI_DBS_ID, ModerationEvent.ModerationEventType.PASS, TestingType.SELF_CHECK);
    }

    private void sendEvent(ModerationEvent.ModerationEventType eventType, TestingType testingType) {
        sendEvent(SHOP_ID, eventType, testingType);
    }

    private void sendEvent(long partnerId, ModerationEvent.ModerationEventType eventType, TestingType testingType) {
        applicationEventPublisher.publishEvent(ModerationEvent.builder()
                .moderationEventType(eventType)
                .partnerId(partnerId)
                .actionId(100500)
                .testingType(testingType)
                .build());
    }
}
