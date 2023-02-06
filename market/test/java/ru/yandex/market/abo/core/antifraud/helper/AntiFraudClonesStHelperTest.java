package ru.yandex.market.abo.core.antifraud.helper;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.EmptyTest;
import ru.yandex.market.abo.clch.ClchSessionSource;
import ru.yandex.market.abo.clch.db.ClusterStatus;
import ru.yandex.market.abo.core.antifraud.model.AntiFraudCloneCheckResult;
import ru.yandex.market.abo.core.startrek.model.StartrekTicketReason;
import ru.yandex.market.abo.util.FakeUsers;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Aleksei Neliubin (neliubin@yandex-team.ru)
 * @date 03.09.2020
 */
class AntiFraudClonesStHelperTest extends EmptyTest {

    private static final long SHOP_ID = 123L;
    private static final List<Long> SHOP_CLUSTER = List.of(123L, 124L, 125L);
    private static final long SHOP_CLUSTER_ID = 1L;
    private static final long SHOP_CLUSTER_SET_ID = 1L;

    private static final long CLONE_ID = 223L;
    private static final List<Long> CLONE_CLUSTER = List.of(223L, 224L, 225L);
    private static final long CLONE_CLUSTER_ID = 2L;
    private static final long CLONE_CLUSTER_SET_ID = 2L;

    @Autowired
    private AntiFraudClonesStHelper antiFraudClonesStHelper;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void init() {
        SHOP_CLUSTER.forEach(shopId ->
                jdbcTemplate.update("insert into clch_shop_set (set_id, shop_id) values (?, ?)", SHOP_CLUSTER_SET_ID, shopId)
        );
        CLONE_CLUSTER.forEach(shopId ->
                jdbcTemplate.update("insert into clch_shop_set (set_id, shop_id) values (?, ?)", CLONE_CLUSTER_SET_ID, shopId)
        );
        jdbcTemplate.update("insert into clch_cluster (id, shop_set_id, status) values (?, ?, ?), (?, ?, ?)",
                SHOP_CLUSTER_ID, SHOP_CLUSTER_SET_ID, ClusterStatus.CLONE.getId(),
                CLONE_CLUSTER_ID, CLONE_CLUSTER_SET_ID, ClusterStatus.CLONE.getId()
        );
    }

    @Test
    void noNewStClchTask__shopsFromClustersAlreadyChecked() {
        initTicket(
                SHOP_CLUSTER.get(1), CLONE_CLUSTER.get(2),
                LocalDateTime.now().minusDays(AntiFraudClonesStHelper.TASK_EXPIRATION_TIME_DAYS - 10)
        );

        assertFalse(antiFraudClonesStHelper.noNewStClchTask(
                createCheckResult(SHOP_ID, CLONE_ID), StartrekTicketReason.ANTI_FRAUD_POTENTIAL_CLONES, ClchSessionSource.ANTI_FRAUD
        ));
    }

    @Test
    void noNewStClchTask__lastCheckExpired() {
        initTicket(
                SHOP_CLUSTER.get(1), CLONE_CLUSTER.get(2),
                LocalDateTime.now().minusDays(AntiFraudClonesStHelper.TASK_EXPIRATION_TIME_DAYS + 10)
        );

        assertTrue(antiFraudClonesStHelper.noNewStClchTask(
                createCheckResult(SHOP_ID, CLONE_ID), StartrekTicketReason.ANTI_FRAUD_POTENTIAL_CLONES, ClchSessionSource.ANTI_FRAUD
        ));
    }

    @Test
    void noNewStClchTask__shopsNotInClusters() {
        assertTrue(antiFraudClonesStHelper.noNewStClchTask(
                createCheckResult(SHOP_ID + 1, CLONE_ID + 1), StartrekTicketReason.ANTI_FRAUD_POTENTIAL_CLONES, ClchSessionSource.ANTI_FRAUD
        ));
    }

    @Test
    void noNewStClchTask_alreadyCheckedShopsNotFromClusters() {
        initTicket(
                SHOP_ID + 1, CLONE_ID + 1,
                LocalDateTime.now().minusDays(AntiFraudClonesStHelper.TASK_EXPIRATION_TIME_DAYS - 10)
        );

        assertFalse(antiFraudClonesStHelper.noNewStClchTask(
                createCheckResult(SHOP_ID + 1, CLONE_ID + 1), StartrekTicketReason.ANTI_FRAUD_POTENTIAL_CLONES, ClchSessionSource.ANTI_FRAUD
        ));
    }

    private void initTicket(long shopId, long cloneId, LocalDateTime ticketCreationTime) {
        long savedShopSetId = 3;
        long savedSessionId = 3;

        jdbcTemplate.update("insert into clch_shop_set (set_id, shop_id) values (?, ?), (?, ?)",
                savedShopSetId, shopId, savedShopSetId, cloneId
        );
        jdbcTemplate.update("insert into clch_session (id, user_id, source_type, shop_set_id, start_time, end_time) values (?, ?, ?, ?, ?, ?)",
                savedSessionId, FakeUsers.ANTI_FRAUD_POTENTIAL_CLONE_CHECKER.getId(),
                ClchSessionSource.ANTI_FRAUD.name(), savedShopSetId,
                LocalDateTime.now().minusDays(31), LocalDateTime.now()
        );
        jdbcTemplate.update("" +
                        "insert into startrek_tickets (id, ticket_name, source_id, ticket_reason, creation_time, ticket_tag) values " +
                        "(1, 'MARKETCL-1', ?, ?, ?, null)",
                savedSessionId,
                StartrekTicketReason.ANTI_FRAUD_POTENTIAL_CLONES.name(),
                ticketCreationTime
        );

        flushAndClear();
    }

    private static AntiFraudCloneCheckResult createCheckResult(long shopId, long cloneId) {
        var checkResult = new AntiFraudCloneCheckResult();
        checkResult.setCompositeId(shopId, cloneId);
        return checkResult;
    }
}
