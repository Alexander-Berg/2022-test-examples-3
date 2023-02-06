package ru.yandex.market.antifraud.orders.detector;

import java.util.Date;
import java.util.List;
import java.util.Set;

import com.google.common.collect.Lists;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.antifraud.orders.entity.AntifraudAction;
import ru.yandex.market.antifraud.orders.entity.AntifraudBlacklistRule;
import ru.yandex.market.antifraud.orders.entity.MarketUserId;
import ru.yandex.market.antifraud.orders.model.OrderDataContainer;
import ru.yandex.market.antifraud.orders.model.OrderDetectorResult;
import ru.yandex.market.antifraud.orders.service.BlacklistService;
import ru.yandex.market.antifraud.orders.service.ConfigurationService;
import ru.yandex.market.antifraud.orders.service.Utils;
import ru.yandex.market.antifraud.orders.test.providers.OrderRequestProvider;
import ru.yandex.market.antifraud.orders.util.concurrent.FutureValueHolder;
import ru.yandex.market.antifraud.orders.web.dto.checkouter.OrderRequestDto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static ru.yandex.market.antifraud.orders.entity.AntifraudAction.CANCEL_ORDER;
import static ru.yandex.market.antifraud.orders.entity.AntifraudAction.PREPAID_ONLY;
import static ru.yandex.market.antifraud.orders.entity.AntifraudBlacklistRuleType.UID;

/**
 * @author dzvyagin
 */
public class AntifraudBlacklistGluedDetectorTest {

    private final BlacklistService blacklistService = mock(BlacklistService.class);
    private final ConfigurationService configurationService = mock(ConfigurationService.class);
    private final AntifraudBlacklistGluedDetector detector =
            new AntifraudBlacklistGluedDetector(blacklistService, configurationService);

    @Before
    public void setUp() {
        when(configurationService.restrictionsByGlueEnabled()).thenReturn(false);
    }

    @Test
    public void gluedUid() {
        var order = OrderRequestProvider.getOrderRequest();
        Set<MarketUserId> ids = Set.of(
                MarketUserId.fromUid(100099L),
                MarketUserId.fromUid(100100L)
        );
        OrderDataContainer context = OrderDataContainer.builder()
                .orderRequest(order)
                .gluedIdsFuture(new FutureValueHolder<>(ids))
                .build();

        when(blacklistService.checkBlacklist(
                eq(359953025L),
                anyCollection()
        ))
                .thenReturn(List.of(
                        new AntifraudBlacklistRule(UID, "100099", Utils.getBlacklistAction(CANCEL_ORDER), "", new Date(), 123L)
                ));

        final OrderDetectorResult actual = detector.detectFraud(context);
        assertThat(actual).isNotNull();
        assertThat(actual.getActions()).isNotEmpty();
        Set<AntifraudAction> results = actual.getActions();
        assertThat(results).hasSize(1);
        assertThat(results.stream().findFirst().get()).isEqualTo(CANCEL_ORDER);
        verify(blacklistService).checkBlacklist(eq(359953025L), anyList());
        verify(blacklistService, never()).getAntifraudRules(anySet());
        verifyNoMoreInteractions(blacklistService);
    }

    @Test
    public void notBlacklisted() {
        var order = OrderRequestProvider.getOrderRequest();
        Set<MarketUserId> ids = Set.of(
                MarketUserId.fromUid(100099L),
                MarketUserId.fromUid(100100L)
        );
        OrderDataContainer context = OrderDataContainer.builder()
                .orderRequest(order)
                .gluedIdsFuture(new FutureValueHolder<>(ids))
                .build();

        when(blacklistService.checkBlacklist(
                eq(359953025L),
                anyCollection()
        ))
                .thenReturn(List.of());

        final OrderDetectorResult actual = detector.detectFraud(context);
        assertThat(actual).isNotNull();
        assertThat(actual.getActions()).isEmpty();
        verify(blacklistService).checkBlacklist(eq(359953025L), anyList());
        verify(blacklistService, never()).getAntifraudRules(anySet());
        verifyNoMoreInteractions(blacklistService);
    }

    @Test
    public void prepaid() {
        var order = OrderRequestProvider.getOrderRequest();
        Set<MarketUserId> ids = Set.of(
                MarketUserId.fromUid(100099L),
                MarketUserId.fromUid(100100L)
        );
        OrderDataContainer context = OrderDataContainer.builder()
                .orderRequest(order)
                .gluedIdsFuture(new FutureValueHolder<>(ids))
                .build();

        when(configurationService.restrictionsByGlueEnabled()).thenReturn(true);
        when(blacklistService.checkBlacklist(eq(359953025L), anyCollection())).thenReturn(List.of());
        when(blacklistService.getAntifraudRules(Lists.newArrayList(ids))).thenReturn(List.of(
                new AntifraudBlacklistRule(UID, "100099", PREPAID_ONLY.getActionName(), "", new Date(), 123L)));

        OrderDetectorResult actual = detector.detectFraud(context);
        assertThat(actual).isNotNull();
        assertThat(actual.getActions()).containsExactly(PREPAID_ONLY);
        verify(blacklistService).checkBlacklist(eq(359953025L), anyList());
        verify(blacklistService).getAntifraudRules(Lists.newArrayList(ids));
        verifyNoMoreInteractions(blacklistService);
    }

    @Test
    public void noRestrictions() {
        var order = OrderRequestProvider.getOrderRequest();
        Set<MarketUserId> ids = Set.of(
                MarketUserId.fromUid(100099L),
                MarketUserId.fromUid(100100L)
        );
        OrderDataContainer context = OrderDataContainer.builder()
                .orderRequest(order)
                .gluedIdsFuture(new FutureValueHolder<>(ids))
                .build();

        when(configurationService.restrictionsByGlueEnabled()).thenReturn(true);
        when(blacklistService.checkBlacklist(eq(359953025L), anyCollection())).thenReturn(List.of());
        when(blacklistService.getAntifraudRules(Lists.newArrayList(ids))).thenReturn(List.of());

        OrderDetectorResult actual = detector.detectFraud(context);
        assertThat(actual).isNotNull();
        assertThat(actual.getActions()).isEmpty();
        verify(blacklistService).checkBlacklist(eq(359953025L), anyList());
        verify(blacklistService).getAntifraudRules(Lists.newArrayList(ids));
        verifyNoMoreInteractions(blacklistService);
    }
}
