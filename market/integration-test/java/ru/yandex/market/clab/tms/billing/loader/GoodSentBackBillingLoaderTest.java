package ru.yandex.market.clab.tms.billing.loader;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.clab.common.service.audit.AuditRepository;
import ru.yandex.market.clab.common.service.audit.wrapper.GoodWrapper;
import ru.yandex.market.clab.common.service.good.GoodRepository;
import ru.yandex.market.clab.common.test.RandomTestUtils;
import ru.yandex.market.clab.db.jooq.generated.enums.ActionType;
import ru.yandex.market.clab.db.jooq.generated.enums.EntityType;
import ru.yandex.market.clab.db.jooq.generated.enums.GoodState;
import ru.yandex.market.clab.db.jooq.generated.enums.PaidAction;
import ru.yandex.market.clab.db.jooq.generated.tables.pojos.AuditAction;
import ru.yandex.market.clab.db.jooq.generated.tables.pojos.BillingAction;
import ru.yandex.market.clab.db.jooq.generated.tables.pojos.BillingTarif;
import ru.yandex.market.clab.db.jooq.generated.tables.pojos.Category;
import ru.yandex.market.clab.db.jooq.generated.tables.pojos.Good;
import ru.yandex.market.clab.tms.BaseTmsIntegrationTest;
import ru.yandex.market.clab.tms.billing.BillingTarifProvider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author anmalysh
 * @since 2/28/2019
 */
@SuppressWarnings("checkstyle:magicnumber")
public class GoodSentBackBillingLoaderTest extends BaseTmsIntegrationTest {

    private LocalDateTime periodStart = LocalDate.now().atStartOfDay();
    private LocalDateTime periodEnd = periodStart.plusDays(1);
    private Good good;

    @Autowired
    private GoodSentBackBillingLoader goodSentBackBillingLoader;

    @Autowired
    private GoodRepository goodRepository;

    @Autowired
    private AuditRepository auditRepository;

    @Before
    public void setUp() {
        good = goodRepository.save(RandomTestUtils.randomObject(Good.class, "id")
            .setCategoryId(2L)
            .setState(GoodState.NEW)
            .setOutgoingMovementId(null));
    }

    @Test
    public void testCountAddToOutgoing() {
        auditRepository.writeActions(Arrays.asList(
            createAuditAction(periodStart.plusMinutes(1), GoodWrapper.STATE, GoodState.SENT.name()),
            createAuditAction(periodStart.minusDays(2), GoodWrapper.OUTDOING_MOVEMENT_ID, 100)
                .setStaffLogin("user100500")
        ));
        List<BillingAction> actions =
            goodSentBackBillingLoader.loadActions(periodStart, periodEnd, createTarifProviderWithTarifs());

        assertThat(actions).hasOnlyOneElementSatisfying(a -> {
            assertThat(a).extracting(BillingAction::getPaidAction).isEqualTo(PaidAction.GOOD_ADD_TO_OUTGOING);
            assertThat(a).extracting(BillingAction::getStaffLogin).isEqualTo("user100500");
            assertThat(a).extracting(BillingAction::getPriceKopeck).isEqualTo(10);
            assertThat(a).extracting(BillingAction::getCategoryId).isEqualTo(2L);
            assertThat(a).extracting(BillingAction::getBillingDate).isEqualTo(periodStart.plusMinutes(1));
            assertThat(a).extracting(BillingAction::getAuditActionId).isNotNull();
        });
    }

    @Test
    public void testCountAddToOutgoingNoAddToOutgoing() {
        auditRepository.writeActions(Collections.singletonList(
            createAuditAction(periodStart.plusMinutes(1), GoodWrapper.STATE, GoodState.SENT.name())
        ));
        List<BillingAction> actions =
            goodSentBackBillingLoader.loadActions(periodStart, periodEnd, createTarifProviderWithTarifs());

        assertThat(actions).isEmpty();
    }

    @Test
    public void testCountAddToOutgoingNoTarif() {
        auditRepository.writeActions(Arrays.asList(
            createAuditAction(periodStart.plusMinutes(1), GoodWrapper.STATE, GoodState.SENT.name()),
            createAuditAction(periodStart.minusDays(2), GoodWrapper.OUTDOING_MOVEMENT_ID, 100)
                .setStaffLogin("user100500")
        ));
        List<BillingAction> actions =
            goodSentBackBillingLoader.loadActions(periodStart, periodEnd, createTarifProvider(Collections.emptyList()));

        assertThat(actions).hasOnlyOneElementSatisfying(a -> {
            assertThat(a).extracting(BillingAction::getPaidAction).isEqualTo(PaidAction.GOOD_ADD_TO_OUTGOING);
            assertThat(a).extracting(BillingAction::getStaffLogin).isEqualTo("user100500");
            assertThat(a).extracting(BillingAction::getPriceKopeck).isEqualTo(0);
            assertThat(a).extracting(BillingAction::getCategoryId).isEqualTo(2L);
            assertThat(a).extracting(BillingAction::getBillingDate).isEqualTo(periodStart.plusMinutes(1));
            assertThat(a).extracting(BillingAction::getAuditActionId).isNotNull();
        });
    }

    @Test
    public void testFailedIfGoodNotFound() {
        auditRepository.writeActions(Collections.singletonList(
            createAuditAction(periodStart.plusMinutes(1), GoodWrapper.STATE, GoodState.SENT.name())
                .setEntityInternalId(good.getId() + 1)
        ));
        assertThatThrownBy(() ->
                goodSentBackBillingLoader.loadActions(periodStart, periodEnd, createTarifProviderWithTarifs()))
            .isInstanceOf(IllegalStateException.class);
    }

    private BillingTarifProvider createTarifProviderWithTarifs() {
        List<BillingTarif> tarifs = Collections.singletonList(
            new BillingTarif()
                .setCategoryId(2L)
                .setPaidAction(PaidAction.GOOD_ADD_TO_OUTGOING)
                .setStartDate(periodStart.minusSeconds(1))
                .setPriceKopeck(10));
        return createTarifProvider(tarifs);
    }

    private BillingTarifProvider createTarifProvider(List<BillingTarif> tarifs) {
        List<Category> categories = Collections.singletonList(new Category().setId(2L));
        return new BillingTarifProvider(periodStart, categories, tarifs);
    }

    private AuditAction createAuditAction(LocalDateTime date, String propertyName, Object newValue) {
        return RandomTestUtils.randomObject(AuditAction.class, "id")
            .setEntityInternalId(good.getId())
            .setActionType(ActionType.UPDATE)
            .setEntityType(EntityType.GOOD)
            .setActionDate(date)
            .setPropertyName(propertyName)
            .setStaffLogin("user1")
            .setNewValue(String.valueOf(newValue));
    }
}
