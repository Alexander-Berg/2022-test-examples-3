package ru.yandex.market.abo.clch;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableSet;
import one.util.streamex.StreamEx;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.market.abo.clch.checker.CheckerResult;
import ru.yandex.market.abo.clch.db.ClusterStatus;
import ru.yandex.market.abo.clch.model.ClchSessionReport;
import ru.yandex.market.abo.clch.model.TimeLimits;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Olga Bolshakova (obolshakova@yandex-team.ru)
 * @date 26.09.2008
 */
class ClchServiceTest extends ClchTest {
    private static final long SHOP_SET_ID = 0L;
    private static final ClchSourceWithUser CLCH_SOURCE = ClchSessionSource.WHITE_PREMOD;

    private static final Set<Long> SHOPS = ImmutableSet.of(1L, 2L, 3L);

    @Autowired
    private ClchService clchService;
    @Autowired
    private ClchClusterService clchClusterService;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private CheckerManager checkerManager;

    @Test
    @Disabled("кластер не найден ;(")
    void testRevertCluster() {
        clchService.revertLastClusterState(2750);
    }

    @Test
    void getSessionsWithShops() {
        Map<Integer, Set<Long>> sessionsWithShops = clchService.getSessionsWithShops(ClchSessionStatus.UNDEFINED);
        assertNotNull(sessionsWithShops);
    }

    @Test
    void getSessionBySetId() {
        long sessionId = clchService.saveSession(CLCH_SOURCE, SHOP_SET_ID, TimeLimits.getDefault());
        Long sessionBySetId = clchService.getSessionBySetId(SHOP_SET_ID);
        assertEquals(sessionId, (long) sessionBySetId);
        assertEquals(CLCH_SOURCE.getSource().name(),
                jdbcTemplate.queryForObject("select source_type from clch_session where id = ?", String.class, sessionId));
    }

    @Test
    void getOrCreateShopSet() {
        long setId = clchService.createShopSet(new ArrayList<>(SHOPS));
        long alreadyCreatedSetId = clchService.getOrCreateShopSet(SHOPS);
        assertEquals(setId, alreadyCreatedSetId);

        HashSet<Long> extendedShops = new HashSet<>(SHOPS);
        extendedShops.add(4L);
        long newSetId = clchService.getOrCreateShopSet(extendedShops);
        assertNotEquals(alreadyCreatedSetId, newSetId);
        assertEquals(newSetId, clchService.getOrCreateShopSet(extendedShops));
    }

    @Test
    void getSessionsCountCreatedAfterByUser() {
        ManualClchSource clchSource = new ManualClchSource(RND.nextLong());
        LocalDateTime yesterday = LocalDateTime.now().minusDays(1);
        assertEquals(0, clchService.getSessionsCountCreatedAfterByUser(yesterday, clchSource));

        clchService.saveSession(clchSource, SHOP_SET_ID, TimeLimits.getDefault());
        assertEquals(1, clchService.getSessionsCountCreatedAfterByUser(yesterday, clchSource));
    }

    @Test
    void clchActiveSessionExists() {
        assertFalse(clchService.clchActiveSessionExists(SHOPS.iterator().next()));
        long sessionId = createSession(SHOPS);

        assertFalse(clchService.clchActiveSessionExists(SHOPS.iterator().next()));
        clchService.saveSessionActive(sessionId, ClchSessionActivity.ACTIVE, "foo", CLCH_SOURCE.getUserId());
        assertTrue(clchService.clchActiveSessionExists(SHOPS.iterator().next()));
    }

    @Test
    void getShopsInClustersWith() {
        Long shopId = SHOPS.iterator().next();
        assertTrue(clchService.getShopsInClustersWith(shopId).isEmpty());
        long setId = clchService.createShopSet(new ArrayList<>(SHOPS));

        clchClusterService.saveCluster(setId, CLCH_SOURCE.getUserId(), ClusterStatus.CLONE);
        HashSet<Long> shopsInCluster = new HashSet<>(clchService.getShopsInClustersWith(shopId));
        HashSet<Long> expectedInCluster = new HashSet<>(SHOPS);
        expectedInCluster.remove(shopId);
        assertEquals(expectedInCluster, shopsInCluster);
    }

    @Test
    void activeClustersWithShops() {
        assertTrue(clchService.activeClustersWithShops(SHOPS).isEmpty());

        long setId = clchService.createShopSet(new ArrayList<>(SHOPS));
        clchService.saveSession(CLCH_SOURCE, setId, TimeLimits.getDefault());
        clchClusterService.saveCluster(setId, CLCH_SOURCE.getUserId(), ClusterStatus.CLONE);

        List<Long> clustersByShopSet = clchClusterService.loadClusterIdsByShopSetId(setId);
        assertEquals(1, clustersByShopSet.size());

        List<Long> activeClustersWithShops = clchService.activeClustersWithShops(SHOPS);
        assertEquals(activeClustersWithShops, clustersByShopSet);
    }

    /**
     * нужно прилинковать старую сессию без кластера к кластеру новой сессии.
     */
    @Test
    void linkToNewClusterIfNeeded_linkClusterToOldSess() {
        long oldSetId = clchService.createShopSet(Arrays.asList(5L, 4234L, 333L));
        long oldSessionId = clchService.saveSession(CLCH_SOURCE, oldSetId, TimeLimits.getDefault());
        assertTrue(findClusters(oldSetId).isEmpty());

        long newSetId = clchService.createShopSet(new ArrayList<>(SHOPS));
        long newSessionId = createSessionWithCluster(newSetId);

        Map<Long, ClusterStatus> newSessionClusters = findClusters(newSetId);
        assertEquals(1, newSessionClusters.size());
        assertEquals(ClusterStatus.CLONE, newSessionClusters.values().iterator().next());

        clchService.linkToNewClusterIfNeeded(Collections.singletonList(oldSessionId), newSessionId, CLCH_SOURCE.getUserId());

        Map<Long, ClusterStatus> oldSessionClusters = findClusters(oldSetId);
        assertEquals(1, oldSessionClusters.size());
        assertEquals(ClusterStatus.DELETED, oldSessionClusters.values().iterator().next());
        assertEquals(newSessionClusters.keySet(), oldSessionClusters.keySet());
    }

    /**
     * если у старой сессии уже есть связь с каким-то кластером, не нужно перелинковывать.
     */
    @Test
    void linkToNewClusterIfNeeded_oldSessAlreadyLinked() {
        long oldSetId = clchService.createShopSet(Arrays.asList(533L, 422334L, 32333L));
        long oldSessWithCluster = createSessionWithCluster(oldSetId);

        long newSetId = clchService.createShopSet(new ArrayList<>(SHOPS));
        long newSessionId = createSessionWithCluster(newSetId);

        Map<Long, ClusterStatus> oldSessionCluster = findClusters(oldSetId);
        assertEquals(ClusterStatus.CLONE, oldSessionCluster.values().iterator().next());
        assertEquals(ClusterStatus.CLONE, findClusters(newSetId).values().iterator().next());

        clchService.linkToNewClusterIfNeeded(Collections.singletonList(oldSessWithCluster), newSessionId, CLCH_SOURCE.getUserId());
        Map<Long, ClusterStatus> oldSessClusterAfterLink = findClusters(oldSetId);
        assertEquals(oldSessionCluster, oldSessClusterAfterLink);
    }

    /**
     * если у новой сессии нет кластера, то и со старой ничего делать не надо.
     */
    @Test
    void linkToNewClusterIfNeeded_noNewCluster() {
        long oldSess = createSession(Arrays.asList(5L, 4L, 3L));
        long newSessWithNoCluster = createSession(SHOPS);

        assertNull(clchService.getActiveCluster(newSessWithNoCluster));
        clchService.linkToNewClusterIfNeeded(Collections.singletonList(oldSess), newSessWithNoCluster, CLCH_SOURCE.getUserId());

        Stream.of(oldSess, newSessWithNoCluster).forEach(sessId -> {
            long setId = clchService.getShopSetBySessionId(sessId);
            assertTrue(findClusters(setId).isEmpty());
        });
    }

    @Test
    void getActiveCluster() {
        long setId = clchService.createShopSet(new ArrayList<>(SHOPS));
        long sessionId = clchService.saveSession(CLCH_SOURCE, setId, TimeLimits.getDefault());
        assertNull(clchService.getActiveCluster(sessionId));
        clchClusterService.saveCluster(setId, CLCH_SOURCE.getUserId(), ClusterStatus.CLONE);
        assertNotNull(clchService.getActiveCluster(sessionId));
    }

    @Test
    void getSessionsReport() {
        long sessionId = createSession(SHOPS);
        List<CheckerResult> results = StreamEx.of(SHOPS)
                .pairMap((sh1, sh2) -> new CheckerResult(sh1, sh2, "foo", "oo", 1, 1))
                .toList();
        checkerManager.saveResultSession(sessionId, results);
        checkerManager.analyzeResults(sessionId);

        jdbcTemplate.update("begin; refresh materialized view mv_clch_summary; commit;");

        Set<ClchSessionReport> sessions = clchService.getSessionsReport(SHOPS.stream().limit(1).collect(Collectors.toSet()));
        assertEquals(1, sessions.size());

        ClchSessionReport sessionReport = sessions.iterator().next();
        assertEquals(sessionId, sessionReport.getSessionId());
        assertEquals(SHOPS, Arrays.stream(sessionReport.getShopIds()).map(BigDecimal::longValue).collect(Collectors.toSet()));
    }

    @Test
    void getActiveShopIdsFromSession() {
        long sessionId = createSession(List.of(123L, 456L));
        assertNotNull(clchService.getActiveShopIdsFromSession(sessionId));
    }

    private long createSession(Collection<Long> withShops) {
        long setId = clchService.createShopSet(new ArrayList<>(withShops));
        return clchService.saveSession(CLCH_SOURCE, setId, TimeLimits.getDefault());
    }

    /**
     * @return sessionId
     */
    private long createSessionWithCluster(long setId) {
        long sessionId = clchService.saveSession(CLCH_SOURCE, setId, TimeLimits.getDefault());
        clchClusterService.saveCluster(setId, CLCH_SOURCE.getUserId(), ClusterStatus.CLONE);
        return sessionId;
    }

    private Map<Long, ClusterStatus> findClusters(long shopSetId) {
        Map<Long, ClusterStatus> result = new HashMap<>();
        jdbcTemplate.query("select id, status from clch_cluster where shop_set_id = ?", rs -> {
            result.put(rs.getLong("id"), ClusterStatus.values()[rs.getInt("status")]);
        }, shopSetId);
        return result;
    }
}
