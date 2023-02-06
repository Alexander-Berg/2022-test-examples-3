package ru.yandex.market.antifraud.orders.detector;

import java.util.Date;
import java.util.List;
import java.util.Set;

import org.junit.Test;

import ru.yandex.market.antifraud.orders.entity.AntifraudBlacklistRule;
import ru.yandex.market.antifraud.orders.model.OrderDataContainer;
import ru.yandex.market.antifraud.orders.model.OrderDetectorResult;
import ru.yandex.market.antifraud.orders.service.BlacklistService;
import ru.yandex.market.antifraud.orders.test.providers.OrderRequestProvider;
import ru.yandex.market.antifraud.orders.web.dto.checkouter.OrderRequestDto;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static ru.yandex.market.antifraud.orders.entity.AntifraudAction.CANCEL_ORDER;
import static ru.yandex.market.antifraud.orders.entity.AntifraudAction.PREPAID_ONLY;
import static ru.yandex.market.antifraud.orders.entity.AntifraudBlacklistRuleType.EMAIL;
import static ru.yandex.market.antifraud.orders.entity.AntifraudBlacklistRuleType.PHONE;
import static ru.yandex.market.antifraud.orders.entity.AntifraudBlacklistRuleType.UID;
import static ru.yandex.market.antifraud.orders.entity.AntifraudBlacklistRuleType.UUID;

/**
 * Created by max-samoylov on 04.06.2019.
 */
public class AntifraudBlacklistDetectorTest {

    private final BlacklistService blacklistService = mock(BlacklistService.class);
    private final AntifraudBlacklistDetector detector = new AntifraudBlacklistDetector(blacklistService);

    @Test
    public void uid() {
        var order = OrderRequestProvider.getOrderRequest();

        when(blacklistService.getAntifraudRules(anyList()))
                .thenReturn(List.of(new AntifraudBlacklistRule(UID, "359953025", CANCEL_ORDER.getActionName(), "", new Date(), 123L)));

        final OrderDetectorResult expected = OrderDetectorResult.builder()
                .ruleName(detector.getUniqName())
                .actions(Set.of(CANCEL_ORDER))
                .answerText("Пользователь имеет персональные ограничения")
                .reason("Идентификатор uid имеет персональные ограничения: [uid=359953025] (CANCEL_ORDER)")
                .build();
        final OrderDetectorResult actual = detector.detectFraud(OrderDataContainer.builder().orderRequest(order).build());

        assertEquals(expected, actual);

        verify(blacklistService).getAntifraudRules(anyList());
        verifyNoMoreInteractions(blacklistService);
    }

    @Test
    public void uidPrepaid() {
        var order = OrderRequestProvider.getOrderRequest();

        when(blacklistService.getAntifraudRules(anyList()))
                .thenReturn(List.of(new AntifraudBlacklistRule(UID, "359953025", PREPAID_ONLY.getActionName(), "", new Date(), 123L)));

        final OrderDetectorResult expected = OrderDetectorResult.builder()
                .ruleName(detector.getUniqName())
                .actions(Set.of(PREPAID_ONLY))
                .answerText("Пользователь имеет персональные ограничения")
                .reason("Идентификатор uid имеет персональные ограничения: [uid=359953025] (PREPAID_ONLY)")
                .build();
        final OrderDetectorResult actual = detector.detectFraud(OrderDataContainer.builder().orderRequest(order).build());

        assertEquals(expected, actual);

        verify(blacklistService).getAntifraudRules(anyList());
        verifyNoMoreInteractions(blacklistService);
    }

    @Test
    public void uidCancelAndPrepaid() {
        var order = OrderRequestProvider.getOrderRequest();

        when(blacklistService.getAntifraudRules(anyList()))
                .thenReturn(List.of(
                        new AntifraudBlacklistRule(UID, "359953025", PREPAID_ONLY.getActionName(), "", new Date(), 123L),
                        new AntifraudBlacklistRule(UID, "359953025", CANCEL_ORDER.getActionName(), "", new Date(), 123L)));

        final OrderDetectorResult expected = OrderDetectorResult.builder()
                .ruleName(detector.getUniqName())
                .actions(Set.of(CANCEL_ORDER, PREPAID_ONLY))
                .answerText("Пользователь имеет персональные ограничения")
                .reason("Идентификатор uid имеет персональные ограничения: [uid=359953025] (CANCEL_ORDER, PREPAID_ONLY)")
                .build();
        final OrderDetectorResult actual = detector.detectFraud(OrderDataContainer.builder().orderRequest(order).build());

        assertEquals(expected, actual);

        verify(blacklistService).getAntifraudRules(anyList());
        verifyNoMoreInteractions(blacklistService);
    }

    @Test
    public void email() {
        var order = OrderRequestProvider.getOrderRequest();

        when(blacklistService.getAntifraudRules(anyList()))
                .thenReturn(List.of(new AntifraudBlacklistRule(EMAIL, "a@b.com", CANCEL_ORDER.getActionName(), "", new Date(), 123L)));
        final OrderDetectorResult expected = OrderDetectorResult.builder()
                .ruleName(detector.getUniqName())
                .actions(Set.of(CANCEL_ORDER))
                .answerText("Пользователь имеет персональные ограничения")
                .reason("Идентификатор email имеет персональные ограничения: [email=a@b.com] (CANCEL_ORDER)")
                .build();
        final OrderDetectorResult actual = detector.detectFraud(OrderDataContainer.builder().orderRequest(order).build());

        assertEquals(expected, actual);

        verify(blacklistService).getAntifraudRules(anyList());
        verifyNoMoreInteractions(blacklistService);
    }

    @Test
    public void phone() {
        var order = OrderRequestProvider.getOrderRequest();

        when(blacklistService.getAntifraudRules(anyList()))
                .thenReturn(List.of(new AntifraudBlacklistRule(PHONE, "71234567891", CANCEL_ORDER.getActionName(), "", new Date(), 123L)));

        final OrderDetectorResult expected = OrderDetectorResult.builder()
                .ruleName(detector.getUniqName())
                .actions(Set.of(CANCEL_ORDER))
                .answerText("Пользователь имеет персональные ограничения")
                .reason("Идентификатор phone имеет персональные ограничения: [phone=71234567891] (CANCEL_ORDER)")
                .build();
        final OrderDetectorResult actual = detector.detectFraud(OrderDataContainer.builder().orderRequest(order).build());

        assertEquals(expected, actual);

        verify(blacklistService).getAntifraudRules(anyList());
        verifyNoMoreInteractions(blacklistService);
    }

    @Test
    public void uuid() {
        var order = OrderRequestProvider.getOrderRequest();

        when(blacklistService.getAntifraudRules(anyList()))
                .thenReturn(List.of(new AntifraudBlacklistRule(UUID, "100500", CANCEL_ORDER.getActionName(), "", new Date(), 123L)));

        final OrderDetectorResult expected = OrderDetectorResult.builder()
                .ruleName(detector.getUniqName())
                .actions(Set.of(CANCEL_ORDER))
                .answerText("Пользователь имеет персональные ограничения")
                .reason("Идентификатор uuid имеет персональные ограничения: [uuid=100500] (CANCEL_ORDER)")
                .build();
        final OrderDetectorResult actual = detector.detectFraud(OrderDataContainer.builder().orderRequest(order).build());

        assertEquals(expected, actual);

        verify(blacklistService).getAntifraudRules(anyList());
        verifyNoMoreInteractions(blacklistService);
    }

    @Test
    public void whitelist() {
        var order = OrderRequestProvider.getOrderRequest();
        final OrderDetectorResult expected = OrderDetectorResult.empty(detector.getUniqName());
        final OrderDetectorResult actual = detector.detectFraud(OrderDataContainer.builder().orderRequest(order).build());
        assertEquals(expected, actual);
        verify(blacklistService).getAntifraudRules(anyList());
        verifyNoMoreInteractions(blacklistService);
    }
}
