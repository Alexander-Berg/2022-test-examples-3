package ru.yandex.market.abo.clch;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.market.abo.core.shopdata.clch.ShopDataResult;
import ru.yandex.market.abo.core.shopdata.clch.model.ShopSetWithComment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * @author artemmz
 * @date 19.11.17.
 */
class ClchClusterManagerTest {
    private static final int RESULTS_SIZE = 2;
    private static final long PREMOD_SHOP = 100L;
    private static final Set<Long> SHOPS_1 = new HashSet<>(Arrays.asList(1L, 2L, 3L));
    private static final Set<Long> SHOPS_2 = new HashSet<>(Arrays.asList(4L, 5L, 6L));
    private static final Set<Long> PREMOD_SHOP_WITH_ALIKE = Stream.of(Collections.singleton(PREMOD_SHOP), SHOPS_1)
            .flatMap(Collection::stream).collect(Collectors.toSet());

    @InjectMocks
    private ClchClusterManager clchClusterManager;
    @Mock
    private ClchService clchService;
    @Mock
    private CheckerStarter checkerStarter;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(clchService.activeClustersWithShops(PREMOD_SHOP_WITH_ALIKE)).thenReturn(new ArrayList<>());
    }

    @Test
    void createNewClchSessions() {
        long sessionId = 55L;
        when(checkerStarter.startCheckersIfShopsFresh(PREMOD_SHOP_WITH_ALIKE, ClchSessionSource.WHITE_PREMOD))
                .thenReturn(sessionId);

        List<Long> newSessionIds = clchClusterManager.createNewClchSessions(PREMOD_SHOP, initTestResults(true));
        assertEquals(Collections.singletonList(sessionId), newSessionIds);
        verify(clchService).addUserComment(eq(sessionId), any());
    }

    @Test
    void shopsNotForCloneCheck() {
        when(checkerStarter.startCheckersIfShopsFresh(PREMOD_SHOP_WITH_ALIKE, ClchSessionSource.WHITE_PREMOD))
                .thenReturn(0L);
        List<Long> newSessionIds = clchClusterManager.createNewClchSessions(PREMOD_SHOP, initTestResults(true));
        assertTrue(newSessionIds.isEmpty());
    }

    @Test
    void shopAlreadyInActiveCluster() {
        when(clchService.activeClustersWithShops(PREMOD_SHOP_WITH_ALIKE)).thenReturn(Collections.singletonList(123L));
        List<Long> newSessionIds = clchClusterManager.createNewClchSessions(PREMOD_SHOP, initTestResults(true));
        assertTrue(newSessionIds.isEmpty());
        verifyNoMoreInteractions(checkerStarter);
    }

    @Test
    void getUniqueShopSets_same_shops() {
        List<ShopSetWithComment> unique = ClchClusterManager.getUniqueShopSets(initTestResults(true));
        assertEquals(1, unique.size());
        ShopSetWithComment shopSetWithComment = unique.iterator().next();
        assertEquals(SHOPS_1, shopSetWithComment.getShopIds());
        assertEquals("title 2 : 1, 2, 3;\ntitle 1 : 1, 2, 3", shopSetWithComment.getComment().toString());
    }

    @Test
    void getUniqueShopSets_different_shops() {
        List<ShopSetWithComment> unique = ClchClusterManager.getUniqueShopSets(initTestResults(false));
        assertEquals(RESULTS_SIZE, unique.size());
        unique.stream().map(ShopSetWithComment::getShopIds)
                .forEach(shopIds -> assertTrue(shopIds.equals(SHOPS_1) || shopIds.equals(SHOPS_2)));
    }

    private List<ShopDataResult> initTestResults(boolean sameShops) {
        return IntStream.rangeClosed(1, RESULTS_SIZE).mapToObj(i ->
                new ShopDataResult(i, "title " + i, sameShops ? SHOPS_1 : i % 2 == 0 ? SHOPS_2 : SHOPS_1, true)
        ).collect(Collectors.toList());
    }
}
