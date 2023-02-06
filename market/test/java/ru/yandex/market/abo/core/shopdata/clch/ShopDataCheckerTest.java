package ru.yandex.market.abo.core.shopdata.clch;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import one.util.streamex.StreamEx;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.market.abo.clch.ClchService;
import ru.yandex.market.abo.core.shopdata.clch.model.ShopDataCheckerType;
import ru.yandex.market.abo.core.shopdata.clch.model.ShopSetWithComment;
import ru.yandex.market.abo.test.TestHelper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.when;

/**
 * @author artemmz
 * @date 27/07/2020.
 */
class ShopDataCheckerTest {
    private static final long SHOP_ID = 532452L;
    private static final Long CLUSTER_ID = 423452345L;
    private static final ShopDataCheckerType CHECKER_TYPE = ShopDataCheckerType.DOMAIN;

    private static final Set<Long> ALIKE_SHOPS = Set.of(24423L, 534524L, 643564L);
    private static final Set<Long> CLUSTER_SHOPS = StreamEx.of(ALIKE_SHOPS).append(SHOP_ID).toSet();

    @InjectMocks
    ShopDataChecker shopDataChecker;
    @Mock
    ClchService clchService;
    @Mock
    ShopDataCheckerLoader shopDataCheckerLoader;
    @Mock
    ShopDataTypeChecker shopDataTypeChecker;
    @Mock
    ExecutorService pool;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(shopDataCheckerLoader.loadCheckers()).thenReturn(List.of(shopDataTypeChecker));
        when(shopDataTypeChecker.getCheckerType()).thenReturn(CHECKER_TYPE);
        when(clchService.getActiveShopIds(anySet())).then(inv -> inv.getArguments()[0]);
        when(shopDataTypeChecker.findAlikeShops(SHOP_ID))
                .thenReturn(new ShopDataResult(CHECKER_TYPE.getId(), "checked type", ALIKE_SHOPS, true));
        TestHelper.mockExecutorService(pool);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void buildPotentialCluster(boolean alreadyInCluster) {
        when(clchService.activeClustersWithShops(CLUSTER_SHOPS))
                .thenReturn(alreadyInCluster ? List.of(CLUSTER_ID) : Collections.emptyList());

        var potentialCluster = shopDataChecker.buildPotentialCluster(SHOP_ID, Set.of(CHECKER_TYPE));
        if (alreadyInCluster) {
            assertTrue(potentialCluster.isEmpty());
        } else {
            assertEquals(new ShopSetWithComment(CLUSTER_SHOPS, null), potentialCluster.orElse(null));
        }
    }

    @Test
    void buildPotentialCluster_not_all_active() {
        ImmutableSet<Long> active = ImmutableSet.copyOf(Iterables.limit(ALIKE_SHOPS, ALIKE_SHOPS.size() - 1));
        Set<Long> clusterShops = StreamEx.of(active).append(SHOP_ID).toSet();
        when(clchService.getActiveShopIds(CLUSTER_SHOPS)).thenReturn(clusterShops);

        assertEquals(new ShopSetWithComment(clusterShops, null),
                shopDataChecker.buildPotentialCluster(SHOP_ID, Set.of(CHECKER_TYPE)).orElse(null));

    }
}
