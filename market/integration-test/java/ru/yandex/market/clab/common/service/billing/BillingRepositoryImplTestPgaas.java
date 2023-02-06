package ru.yandex.market.clab.common.service.billing;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableList;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.test.context.transaction.TestTransaction;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import ru.yandex.market.clab.common.service.ConcurrentModificationException;
import ru.yandex.market.clab.common.service.PageFilter;
import ru.yandex.market.clab.common.service.SortOrder;
import ru.yandex.market.clab.common.service.Sorting;
import ru.yandex.market.clab.common.test.RandomTestUtils;
import ru.yandex.market.clab.db.jooq.generated.enums.BillingSessionState;
import ru.yandex.market.clab.db.jooq.generated.enums.PaidAction;
import ru.yandex.market.clab.db.jooq.generated.tables.pojos.BillingAction;
import ru.yandex.market.clab.db.jooq.generated.tables.pojos.BillingSession;
import ru.yandex.market.clab.db.jooq.generated.tables.pojos.BillingTarif;
import ru.yandex.market.clab.db.test.BasePgaasIntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author anmalysh
 * @since 21.10.2018
 */
@SuppressWarnings("checkstyle:magicnumber")
public class BillingRepositoryImplTestPgaas extends BasePgaasIntegrationTest {

    private final LocalDateTime startOfDay = LocalDate.now().atStartOfDay();

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private BillingRepository billingRepository;

    private BillingAction filterAction1;

    private BillingAction filterAction2;

    private BillingAction filterAction3;

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    List<BillingAction> filterActions;

    @Before
    public void before() {

        filterAction1 = RandomTestUtils.randomObject(BillingAction.class, "id")
            .setBillingDate(startOfDay)
            .setStaffLogin("user1");

        filterAction2 = RandomTestUtils.randomObject(BillingAction.class, "id")
            .setBillingDate(startOfDay.plusDays(1))
            .setStaffLogin("user1");

        filterAction3 = RandomTestUtils.randomObject(BillingAction.class, "id")
            .setBillingDate(startOfDay.plusDays(2))
            .setStaffLogin("user2");

        filterActions = ImmutableList.of(filterAction1, filterAction2, filterAction3);
    }

    @Test
    public void insertAndGet() {
        BillingAction billingAction = RandomTestUtils.randomObject(BillingAction.class, "id");

        billingRepository.writeActions(Collections.singletonList(billingAction));

        List<BillingAction> saved = billingRepository.findActions(new BillingActionFilter());

        assertIdsExistAndClean(saved);
        assertThat(saved).containsExactly(billingAction);
    }

    @Test
    public void insertBatchAndGet() {
        BillingAction billingAction1 = RandomTestUtils.randomObject(BillingAction.class, "id");
        BillingAction billingAction2 = RandomTestUtils.randomObject(BillingAction.class, "id");
        BillingAction billingAction3 = RandomTestUtils.randomObject(BillingAction.class, "id");
        billingAction2.setBillingDate(billingAction1.getBillingDate().minusDays(1));
        billingAction3.setBillingDate(billingAction1.getBillingDate().minusDays(2));

        billingRepository.writeActions(ImmutableList.of(billingAction1, billingAction2, billingAction3));

        List<BillingAction> saved = billingRepository.findActions(new BillingActionFilter());

        assertIdsExistAndClean(saved);
        assertThat(saved).containsExactly(billingAction3, billingAction2, billingAction1);
    }

    @Test
    public void testSortByDateAndPaging() {
        BillingAction billingAction1 = RandomTestUtils.randomObject(BillingAction.class, "id");
        BillingAction billingAction2 = RandomTestUtils.randomObject(BillingAction.class, "id");
        BillingAction billingAction3 = RandomTestUtils.randomObject(BillingAction.class, "id");
        billingAction2.setBillingDate(billingAction1.getBillingDate().plusDays(1));
        billingAction3.setBillingDate(billingAction1.getBillingDate().plusDays(2));

        billingRepository.writeActions(ImmutableList.of(billingAction1, billingAction2, billingAction3));

        List<BillingAction> saved = billingRepository.findActions(
            new BillingActionFilter(),
            BillingSortBy.BILLING_DATE.asc(),
            PageFilter.page(1, 2));

        assertIdsExistAndClean(saved);
        assertThat(saved).containsExactly(billingAction3);
    }

    @Test
    public void testSortByStaffLoginAndPaging() {
        BillingAction billingAction1 = RandomTestUtils.randomObject(BillingAction.class, "id");
        BillingAction billingAction2 = RandomTestUtils.randomObject(BillingAction.class, "id");
        BillingAction billingAction3 = RandomTestUtils.randomObject(BillingAction.class, "id");
        billingAction1.setStaffLogin("abc");
        billingAction2.setStaffLogin("cde");
        billingAction3.setStaffLogin("def");

        billingRepository.writeActions(ImmutableList.of(billingAction1, billingAction2, billingAction3));

        List<BillingAction> saved = billingRepository.findActions(
            new BillingActionFilter(),
            Sorting.of(BillingSortBy.STAFF_LOGIN, SortOrder.DESC),
            PageFilter.page(0, 2));

        assertIdsExistAndClean(saved);
        assertThat(saved).containsExactly(billingAction3, billingAction2);
    }

    @Test
    public void testFilterByDate() {
        billingRepository.writeActions(filterActions);

        List<BillingAction> filtered = billingRepository.findActions(
            new BillingActionFilter()
                .setStartDate(filterAction1.getBillingDate())
                .setEndDate(filterAction2.getBillingDate())
        );

        assertIdsExistAndClean(filtered);
        assertThat(filtered).containsExactlyInAnyOrder(filterAction2);
    }

    @Test
    public void testFilterByStaffLogin() {
        billingRepository.writeActions(filterActions);

        List<BillingAction> filtered = billingRepository.findActions(
            new BillingActionFilter()
                .setStaffLogin("user1")
        );

        assertIdsExistAndClean(filtered);
        assertThat(filtered).containsExactlyInAnyOrder(filterAction1, filterAction2);
    }

    @Test
    public void testRemoveActionsAfter() {
        billingRepository.writeActions(filterActions);

        List<BillingAction> actionsBeforeRemove = billingRepository.findActions(new BillingActionFilter());

        assertIdsExistAndClean(actionsBeforeRemove);
        assertThat(actionsBeforeRemove).containsExactlyInAnyOrder(filterAction1, filterAction2, filterAction3);

        billingRepository.removeActionsAfter(filterAction1.getBillingDate());

        List<BillingAction> actionsAfterRemove = billingRepository.findActions(new BillingActionFilter());

        assertIdsExistAndClean(actionsAfterRemove);
        assertThat(actionsAfterRemove).containsExactlyInAnyOrder(filterAction1);
    }

    @Test
    public void testGetPaidActionUserStats() {
        prepareStatsTestData();

        List<PaidActionUserStats> stats = billingRepository.getPaidActionUserStats(
            new BillingActionFilter()
                .setStartDate(startOfDay)
        );

        assertThat(stats).containsExactlyInAnyOrder(
            new PaidActionUserStats(new PaidActionStats(PaidAction.GOOD_ACCEPT, 2, 8), "user1"),
            new PaidActionUserStats(new PaidActionStats(PaidAction.GOOD_ADD_TO_CART, 1, 1), "user1"),
            new PaidActionUserStats(new PaidActionStats(PaidAction.GOOD_ACCEPT, 1, 7), "user2")
        );
    }

    @Test
    public void testGetPaidActionStats() {
        prepareStatsTestData();

        List<PaidActionStats> stats = billingRepository.getPaidActionStats(
            new BillingActionFilter()
                .setStartDate(startOfDay)
        );

        assertThat(stats).containsExactlyInAnyOrder(
            new PaidActionStats(PaidAction.GOOD_ACCEPT, 3, 15),
            new PaidActionStats(PaidAction.GOOD_ADD_TO_CART, 1, 1)
        );
    }

    @Test
    public void testSaveAndGetTarif() {
        BillingTarif tarif = RandomTestUtils.randomObject(BillingTarif.class, "id");
        BillingTarif tarif2 = new BillingTarif(tarif)
            .setStartDate(tarif.getStartDate().plusHours(1));

        billingRepository.saveTarifs(Arrays.asList(tarif, tarif2));

        List<BillingTarif> savedTarifs = billingRepository.getAllTarif();
        assertThat(savedTarifs).hasSize(2);

        BillingTarif savedTarif2 = savedTarifs.get(0);
        BillingTarif savedTarif = savedTarifs.get(1);

        assertThat(savedTarif).extracting(BillingTarif::getId).isNotNull();
        assertThat(savedTarif2).extracting(BillingTarif::getId).isNotNull();
        assertThat(new BillingTarif(savedTarif).setId(null)).isEqualTo(tarif);
        assertThat(new BillingTarif(savedTarif2).setId(null)).isEqualTo(tarif2);

        List<BillingTarif> latestTarifs = billingRepository.getCategoryTarifs(tarif.getCategoryId());
        assertThat(latestTarifs).containsExactly(savedTarif2);
    }

    @Test
    public void testGetCategoryTarifs() {
        createCategoryTarifs();

        List<BillingTarif> categoryTarifs = billingRepository.getCategoryTarifs(Arrays.asList(3L, 1L));

        List<BillingTarif> tarifsWithoutGeneratedFields = categoryTarifs.stream()
            .map(t -> t.setId(null).setStartDate(null))
            .collect(Collectors.toList());

        assertThat(tarifsWithoutGeneratedFields).containsExactlyInAnyOrder(
            new BillingTarif()
                .setPaidAction(PaidAction.GOOD_ACCEPT)
                .setCategoryId(3L)
                .setPriceKopeck(1),
            new BillingTarif()
                .setPaidAction(PaidAction.GOOD_EDIT_PICTURE)
                .setCategoryId(1L)
                .setPriceKopeck(3),
            new BillingTarif()
                .setPaidAction(PaidAction.GOOD_MAKE_PICTURE)
                .setCategoryId(3L)
                .setPriceKopeck(5)
        );
    }

    @Test
    public void testSaveAndGetBillingSession() {
        BillingSession session = RandomTestUtils.randomObject(BillingSession.class, "id", "modifiedDate")
            .setState(BillingSessionState.SUCCESS);
        BillingSession session2 = new BillingSession(session)
            .setBillingDate(session.getBillingDate().plusDays(1))
            .setState(BillingSessionState.FAILED);
        BillingSession session3 = new BillingSession(session)
            .setBillingDate(session.getBillingDate().plusDays(1))
            .setState(BillingSessionState.SUCCESS);
        BillingSession session4 = new BillingSession(session)
            .setBillingDate(session.getBillingDate().plusDays(2))
            .setState(BillingSessionState.STARTED);

        BillingSession savedSession = billingRepository.saveSession(session);
        BillingSession savedSession2 = billingRepository.saveSession(session2);
        BillingSession savedSession3 = billingRepository.saveSession(session3);
        BillingSession savedSession4 = billingRepository.saveSession(session4);

        assertThat(savedSession).extracting(BillingSession::getId).isNotNull();
        assertThat(savedSession2).extracting(BillingSession::getId).isNotNull();
        assertThat(savedSession3).extracting(BillingSession::getId).isNotNull();
        assertThat(savedSession4).extracting(BillingSession::getId).isNotNull();
        assertThat(new BillingSession(savedSession).setId(null).setModifiedDate(null)).isEqualTo(session);
        assertThat(new BillingSession(savedSession2).setId(null).setModifiedDate(null)).isEqualTo(session2);
        assertThat(new BillingSession(savedSession3).setId(null).setModifiedDate(null)).isEqualTo(session3);
        assertThat(new BillingSession(savedSession4).setId(null).setModifiedDate(null)).isEqualTo(session4);

        BillingSession lastSuccessfulSession = billingRepository.getLastSuccessfulSession();
        assertThat(lastSuccessfulSession).isEqualTo(savedSession3);
    }

    @Test
    public void testMarkSessionsOutdated() {
        BillingSession session = RandomTestUtils.randomObject(BillingSession.class, "id", "modifiedDate")
            .setState(BillingSessionState.SUCCESS);
        BillingSession started = new BillingSession(session)
            .setBillingDate(session.getBillingDate().plusDays(1))
            .setEndDate(null)
            .setState(BillingSessionState.STARTED);

        billingRepository.saveSession(session);
        BillingSession startedSession = billingRepository.saveSession(started);

        billingRepository.markSessionsOutdated();

        List<BillingSession> allSessions = billingRepository.getAllSessions();

        BillingSession outdatedSession = allSessions.stream()
            .filter(s -> s.getId().equals(startedSession.getId()))
            .findFirst().orElseThrow(() -> new AssertionError("Outdated session not found"));

        assertThat(outdatedSession)
            .extracting(BillingSession::getState)
            .isEqualTo(BillingSessionState.OUTDATED);
        assertThat(outdatedSession)
            .extracting(BillingSession::getEndDate)
            .isNotNull();
    }

    @Test
    public void testFinishBillingSession() {
        BillingSession session = RandomTestUtils.randomObject(BillingSession.class, "id", "modifiedDate")
            .setState(BillingSessionState.SUCCESS);
        BillingSession started = new BillingSession(session)
            .setBillingDate(session.getBillingDate().plusDays(1))
            .setEndDate(null)
            .setState(BillingSessionState.STARTED);

        billingRepository.saveSession(session);
        BillingSession startedSession = billingRepository.saveSession(started);

        billingRepository.finishSession(startedSession);

        List<BillingSession> allSessions = billingRepository.getAllSessions();

        BillingSession finishedSession = allSessions.stream()
            .filter(s -> s.getId().equals(startedSession.getId()))
            .findFirst().orElseThrow(() -> new AssertionError("Outdated session not found"));

        assertThat(finishedSession)
            .extracting(BillingSession::getState)
            .isEqualTo(BillingSessionState.SUCCESS);
        assertThat(finishedSession)
            .extracting(BillingSession::getEndDate)
            .isNotNull();
    }

    @Test
    public void testBillingSessionOptimisticLocking() {
        BillingSession session = RandomTestUtils.randomObject(BillingSession.class, "id", "modifiedDate");

        BillingSession savedSession = billingRepository.saveSession(session);
        billingRepository.saveSession(savedSession);

        assertThatThrownBy(() -> {
            billingRepository.saveSession(savedSession);
        }).isInstanceOf(ConcurrentModificationException.class);
    }

    @Test
    public void testBillingSessionTableLock() {
        billingRepository.lockSessionTable();

        BillingSession session = RandomTestUtils.randomObject(BillingSession.class, "id", "modifiedDate");
        billingRepository.saveSession(session);

        TestTransaction.end();
        TestTransaction.start();

        billingRepository.lockSessionTable();
    }

    @Test
    public void testBillingSessionTableLockAlreadyLocked() {
        billingRepository.lockSessionTable();

        // Have to do in separate thread to have separate transaction
        Future result = executorService.submit(() -> {
            TransactionStatus status = transactionManager.getTransaction(new DefaultTransactionDefinition());
            try {
                billingRepository.lockSessionTable();
            } finally {
                transactionManager.rollback(status);
            }
        });

        assertThatThrownBy(result::get)
            .isInstanceOf(ExecutionException.class)
            .hasCauseInstanceOf(CannotAcquireLockException.class);
    }

    private void assertIdsExistAndClean(List<BillingAction> actions) {
        assertThat(actions).noneMatch(a -> a.getId() == null);
        actions.forEach(a -> a.setId(null));
    }

    private void prepareStatsTestData() {
        BillingAction billingAction1 = new BillingAction()
            .setPaidAction(PaidAction.GOOD_ACCEPT)
            .setBillingDate(startOfDay.plusHours(1))
            .setStaffLogin("user1")
            .setPriceKopeck(3);
        BillingAction billingAction2 = new BillingAction()
            .setPaidAction(PaidAction.GOOD_ACCEPT)
            .setBillingDate(startOfDay.plusHours(2))
            .setStaffLogin("user1")
            .setPriceKopeck(5);
        BillingAction billingAction3 = new BillingAction()
            .setPaidAction(PaidAction.GOOD_ADD_TO_CART)
            .setBillingDate(startOfDay.plusHours(3))
            .setStaffLogin("user1")
            .setPriceKopeck(1);
        BillingAction billingAction4 = new BillingAction()
            .setPaidAction(PaidAction.GOOD_ACCEPT)
            .setBillingDate(startOfDay.plusHours(4))
            .setStaffLogin("user2")
            .setPriceKopeck(7);
        BillingAction billingAction5 = new BillingAction()
            .setPaidAction(PaidAction.GOOD_MAKE_PICTURE)
            .setBillingDate(startOfDay.minusHours(1))
            .setStaffLogin("user2")
            .setPriceKopeck(100);
        billingRepository.writeActions(Arrays.asList(
            billingAction1, billingAction2, billingAction3, billingAction4, billingAction5));

    }

    private void createCategoryTarifs() {
        billingRepository.saveTarifs(Arrays.asList(
            new BillingTarif()
                .setPaidAction(PaidAction.GOOD_ACCEPT)
                .setCategoryId(3L)
                .setPriceKopeck(1),
            new BillingTarif()
                .setPaidAction(PaidAction.GOOD_ACCEPT)
                .setCategoryId(2L)
                .setPriceKopeck(2),
            new BillingTarif()
                .setPaidAction(PaidAction.GOOD_EDIT_PICTURE)
                .setCategoryId(1L)
                .setPriceKopeck(3),
            new BillingTarif()
                .setPaidAction(PaidAction.GOOD_MAKE_PICTURE)
                .setCategoryId(2L)
                .setPriceKopeck(4),
            new BillingTarif()
                .setPaidAction(PaidAction.GOOD_MAKE_PICTURE)
                .setCategoryId(3L)
                .setPriceKopeck(5)
            ));
    }
}
