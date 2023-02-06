package ru.yandex.market.clab.tms.billing;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.clab.common.service.billing.BillingActionFilter;
import ru.yandex.market.clab.common.service.billing.BillingRepositoryImpl;
import ru.yandex.market.clab.common.service.category.CategoryRepository;
import ru.yandex.market.clab.db.jooq.generated.enums.BillingSessionState;
import ru.yandex.market.clab.db.jooq.generated.enums.PaidAction;
import ru.yandex.market.clab.db.jooq.generated.tables.pojos.BillingAction;
import ru.yandex.market.clab.db.jooq.generated.tables.pojos.BillingSession;
import ru.yandex.market.clab.db.jooq.generated.tables.pojos.Category;
import ru.yandex.market.clab.tms.BaseTmsIntegrationTest;
import ru.yandex.market.clab.tms.billing.loader.BillingActionLoader;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author anmalysh
 * @since 2/27/2019
 */
public class BillingSessionServiceTest extends BaseTmsIntegrationTest {

    private BillingSessionService billingSessionService;

    @Autowired
    private BillingRepositoryImpl billingRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Before
    public void setUp() {
        BillingActionLoader loader = (periodStart, periodEnd, tarifProvider) ->
            Collections.singletonList(new BillingAction()
                .setPaidAction(PaidAction.GOOD_ACCEPT)
                .setBillingDate(periodEnd.minusHours(3))
                .setCategoryId(1L)
                .setStaffLogin("user1")
                .setPriceKopeck(100)
            );
        // Root category is required
        categoryRepository.create(new Category().setId(1L));
        billingSessionService = new BillingSessionService(
            billingRepository, categoryRepository, loader);
    }

    @Test
    public void testFirstTimeBilling() {
        boolean counted = billingSessionService.checkAndCountBillingSession();

        assertThat(counted).isTrue();

        assertSuccessfulSession(BillingSessionService.BILLING_START_DATE.plusDays(1));
    }

    @Test
    public void testBillingWithExistingSuccessfulSession() {
        BillingSession billingSession = new BillingSession()
            .setState(BillingSessionState.SUCCESS)
            .setBillingDate(LocalDate.now().atStartOfDay().minusDays(3));
        billingSession = billingRepository.saveSession(billingSession);

        boolean counted = billingSessionService.checkAndCountBillingSession();

        assertThat(counted).isTrue();

        assertSuccessfulSession(billingSession.getBillingDate().plusDays(1), billingSession);
    }

    @Test
    public void testBillingNotCountedTooEarly() {
        BillingSession billingSession = new BillingSession()
            .setState(BillingSessionState.SUCCESS)
            .setBillingDate(LocalDate.now().atStartOfDay());
        billingRepository.saveSession(billingSession);

        boolean counted = billingSessionService.checkAndCountBillingSession();

        assertThat(counted).isFalse();
        assertThat(billingRepository.getAllSessions()).hasSize(1);
        assertThat(billingRepository.findActions(new BillingActionFilter())).isEmpty();
    }

    @Test
    public void testCleanOnlySession() {
        // Create successful session with actions
        boolean counted = billingSessionService.checkAndCountBillingSession();
        assertThat(counted).isTrue();

        boolean cleaned = billingSessionService.cleanBillingSession();
        assertThat(cleaned).isTrue();
        assertThat(billingRepository.getLastSuccessfulSession()).isNull();
        assertThat(billingRepository.findActions(new BillingActionFilter())).isEmpty();
    }

    @Test
    public void testCleanOneSession() {
        // Create 2 successful sessions with actions
        boolean counted = billingSessionService.checkAndCountBillingSession();
        assertThat(counted).isTrue();

        counted = billingSessionService.checkAndCountBillingSession();
        assertThat(counted).isTrue();

        boolean cleaned = billingSessionService.cleanBillingSession();
        assertThat(cleaned).isTrue();
        assertThat(billingRepository.getLastSuccessfulSession()).isNotNull();
        assertThat(billingRepository.findActions(new BillingActionFilter())).isNotEmpty();
    }

    @Test
    public void testAttemptCleanNotExistingSession() {
        BillingSession billingSession = new BillingSession()
            .setState(BillingSessionState.FAILED)
            .setBillingDate(LocalDate.now().atStartOfDay());
        billingSession = billingRepository.saveSession(billingSession);

        boolean cleaned = billingSessionService.cleanBillingSession();
        assertThat(cleaned).isFalse();
        assertThat(billingRepository.getAllSessions()).containsExactly(billingSession);
    }

    private void assertSuccessfulSession(LocalDateTime billingDate, BillingSession... sessionsToIgnore) {
        Set<BillingSession> sessionsToIgnoreSet = new HashSet<>(Arrays.asList(sessionsToIgnore));
        List<BillingSession> sessions = billingRepository.getAllSessions().stream()
            .filter(s -> !sessionsToIgnoreSet.contains(s))
            .collect(Collectors.toList());

        assertThat(sessions).singleElement().satisfies(session -> {
            assertThat(session.getId()).isNotNull();
            assertThat(session.getBillingDate()).isEqualTo(
                billingDate);
            assertThat(session.getEndDate()).isNotNull();
            assertThat(session.getStartDate()).isNotNull();
            assertThat(session.getModifiedDate()).isNotNull();
            assertThat(session.getState()).isEqualTo(BillingSessionState.SUCCESS);
        });

        List<BillingAction> actions = billingRepository.findActions(new BillingActionFilter());

        assertThat(actions).singleElement().satisfies(action -> {
            assertThat(action.getId()).isNotNull();
            assertThat(action.getBillingDate()).isEqualTo(billingDate.minusHours(3));
            assertThat(action.getPaidAction()).isEqualTo(PaidAction.GOOD_ACCEPT);
            assertThat(action.getCategoryId()).isEqualTo(1L);
            assertThat(action.getStaffLogin()).isEqualTo("user1");
            assertThat(action.getPriceKopeck()).isEqualTo(100);
        });
    }
}
