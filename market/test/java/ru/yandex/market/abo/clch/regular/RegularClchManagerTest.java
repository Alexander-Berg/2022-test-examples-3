package ru.yandex.market.abo.clch.regular;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.market.abo.clch.CheckerManager;
import ru.yandex.market.abo.clch.ClchService;
import ru.yandex.market.abo.clch.ClchSessionSource;
import ru.yandex.market.abo.clch.regular.model.RegularClchReason;
import ru.yandex.market.abo.clch.regular.model.ShopForRegularClch;
import ru.yandex.market.abo.clch.regular.service.RegularClchStTicketService;
import ru.yandex.market.abo.clch.regular.service.ShopForRegularClchService;
import ru.yandex.market.abo.core.CoreConfig;
import ru.yandex.market.abo.core.shopdata.clch.ShopDataChecker;
import ru.yandex.market.abo.core.shopdata.clch.model.ShopSetWithComment;
import ru.yandex.market.util.db.ConfigurationService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * @author Aleksei Neliubin (neliubin@yandex-team.ru)
 * @date 20.07.2020
 */
class RegularClchManagerTest {

    private static final int SESSIONS_DAILY_LIMIT = 10;

    private static final long SHOP_ID = 123L;
    private static final long SHOP_NOT_IN_CLUSTER_WITH = 424L;

    private static final long SHOP_SET_ID = 1L;
    private static final long SESSION_ID = 2L;

    @InjectMocks
    private RegularClchManager regularClchManager;

    @Mock
    private ShopForRegularClchService shopForRegularClchService;
    @Mock
    private ShopDataChecker shopDataChecker;
    @Mock
    private ClchService clchService;
    @Mock
    private CheckerManager checkerManager;
    @Mock
    private RegularClchStTicketService stTicketService;
    @Mock
    private ConfigurationService coreConfigService;

    @Mock
    private ShopForRegularClch shopForRegularClch;

    @BeforeEach
    void init() {
        MockitoAnnotations.openMocks(this);

        when(coreConfigService.getValueAsInt(CoreConfig.DAILY_SESSIONS_LIMIT_FOR_REGULAR_CLCH.getId())).
                thenReturn(SESSIONS_DAILY_LIMIT);

        when(clchService.getSessionsCountCreatedAfterByUser(
                any(LocalDateTime.class), eq(ClchSessionSource.REGULAR_CHECK))
        ).thenReturn(0);
        when(shopForRegularClchService.getAllShopsForClch()).thenReturn(List.of(shopForRegularClch));

        when(shopForRegularClch.getShopId()).thenReturn(SHOP_ID);
        when(shopForRegularClch.getReasons()).thenReturn(Set.of(RegularClchReason.NEW_PHONES));

        when(shopDataChecker.buildPotentialCluster(SHOP_ID, ShopDataChecker.REGULAR_CHECK_TYPES))
                .thenReturn(Optional.of(new ShopSetWithComment(Set.of(SHOP_ID, SHOP_NOT_IN_CLUSTER_WITH), null)));

        when(checkerManager.getShopSet(Set.of(SHOP_ID, SHOP_NOT_IN_CLUSTER_WITH))).thenReturn(Optional.empty());

    }

    @Test
    void processRegularClch__limitExhausted() {
        when(clchService.getSessionsCountCreatedAfterByUser(
                any(LocalDateTime.class), eq(ClchSessionSource.REGULAR_CHECK))
        ).thenReturn(SESSIONS_DAILY_LIMIT);

        regularClchManager.processRegularClch();

        verify(shopDataChecker, never()).buildPotentialCluster(anyLong(), anySet());
        verify(shopForRegularClchService, never()).getAllShopsForClch();
        verify(shopForRegularClchService, never()).saveShops(anyList());
        verifyNoMoreInteractions(shopDataChecker, checkerManager, stTicketService);
    }

    @Test
    void processRegularClch__emptyShopsQueue() {
        when(shopForRegularClchService.getAllShopsForClch()).thenReturn(Collections.emptyList());

        regularClchManager.processRegularClch();

        verify(shopDataChecker, never()).buildPotentialCluster(anyLong(), anySet());
        verifyNoMoreInteractions(shopDataChecker, checkerManager, stTicketService);
        verify(shopForRegularClchService, never()).markShopProcessed(anyLong());
    }

    @Test
    void processRegularClch__shopsAlreadyChecked() {
        when(checkerManager.getShopSet(Set.of(SHOP_ID, SHOP_NOT_IN_CLUSTER_WITH))).thenReturn(Optional.of(SHOP_SET_ID));
        when(checkerManager.getSessionBySetId(SHOP_SET_ID)).thenReturn(SESSION_ID);
        when(stTicketService.hasNoNewTickets(SESSION_ID)).thenReturn(false);

        regularClchManager.processRegularClch();

        verify(shopDataChecker).buildPotentialCluster(SHOP_ID, ShopDataChecker.REGULAR_CHECK_TYPES);
        verify(checkerManager, never()).createDelayedSession(any(), any());
        verify(stTicketService, never()).createStTicket(anyLong(), any(ShopForRegularClch.class), any(ShopSetWithComment.class));
        verify(shopForRegularClchService).markShopProcessed(SHOP_ID);
    }

    @Test
    void processRegularClch__shopsEnabledForCheck() {
        when(checkerManager.createDelayedSession(eq(ClchSessionSource.REGULAR_CHECK),
                eq(new ShopSetWithComment(Set.of(SHOP_ID, SHOP_NOT_IN_CLUSTER_WITH), null)))).thenReturn(SESSION_ID);

        regularClchManager.processRegularClch();

        verify(shopForRegularClchService).markShopProcessed(SHOP_ID);
        verify(checkerManager).createDelayedSession(eq(ClchSessionSource.REGULAR_CHECK),
                eq(new ShopSetWithComment(Set.of(SHOP_ID, SHOP_NOT_IN_CLUSTER_WITH), null))
        );
        verify(stTicketService).createStTicket(SESSION_ID, shopForRegularClch, buildExpectedCluster(SHOP_ID, SHOP_NOT_IN_CLUSTER_WITH));
    }

    private static ShopSetWithComment buildExpectedCluster(Long... shopIds) {
        return new ShopSetWithComment(Set.of(shopIds), new StringBuilder("[телефоны]\n"));
    }
}
