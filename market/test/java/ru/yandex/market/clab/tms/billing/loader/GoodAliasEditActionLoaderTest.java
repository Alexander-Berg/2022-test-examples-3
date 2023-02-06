package ru.yandex.market.clab.tms.billing.loader;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.clab.ControlledClock;
import ru.yandex.market.clab.common.service.audit.AuditActionFilter;
import ru.yandex.market.clab.common.service.audit.AuditRepository;
import ru.yandex.market.clab.common.service.audit.AuditRepositoryStub;
import ru.yandex.market.clab.common.service.audit.AuditService;
import ru.yandex.market.clab.common.service.audit.AuditedEntity;
import ru.yandex.market.clab.common.service.audit.AuditedEntityId;
import ru.yandex.market.clab.common.service.audit.AuditedProperty;
import ru.yandex.market.clab.common.service.audit.wrapper.GoodWrapper;
import ru.yandex.market.clab.common.service.barcode.SsBarcodeRepository;
import ru.yandex.market.clab.common.service.good.GoodRepository;
import ru.yandex.market.clab.common.service.good.GoodRepositoryStub;
import ru.yandex.market.clab.common.service.good.GoodService;
import ru.yandex.market.clab.common.service.good.GoodServiceImpl;
import ru.yandex.market.clab.common.test.asset.Dates;
import ru.yandex.market.clab.db.jooq.generated.enums.EntityType;
import ru.yandex.market.clab.db.jooq.generated.enums.GoodState;
import ru.yandex.market.clab.db.jooq.generated.enums.PaidAction;
import ru.yandex.market.clab.db.jooq.generated.tables.pojos.AuditAction;
import ru.yandex.market.clab.db.jooq.generated.tables.pojos.BillingAction;
import ru.yandex.market.clab.db.jooq.generated.tables.pojos.BillingTarif;
import ru.yandex.market.clab.db.jooq.generated.tables.pojos.Good;
import ru.yandex.market.clab.tms.billing.BillingTarifProvider;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static java.time.Month.AUGUST;
import static java.time.Month.DECEMBER;
import static java.time.Month.JULY;
import static java.time.Month.JUNE;
import static java.time.Month.SEPTEMBER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.market.clab.common.test.asset.Names.DYLAN;
import static ru.yandex.market.clab.common.test.asset.Names.LUCAS;
import static ru.yandex.market.clab.common.test.asset.Names.MABEL;

/**
 * @author Alexander Kramarev (pochemuto@yandex-team.ru)
 * @since 26.04.2019
 */
public class GoodAliasEditActionLoaderTest {

    private static final String SKU_ID = "sku-id";
    private static final long CATEGORY_ID = 82086972;
    private static final int PRICE = 780;

    private AuditRepository auditRepository;
    private GoodService goodService;
    private AuditService auditService;
    private GoodAliasEditActionLoader loader;
    private ControlledClock clock;

    private LocalDateTime periodStart;
    private LocalDateTime periodEnd;

    private BillingTarifProvider tarifProvider;

    @Before
    public void setUp() {
        GoodRepository goodRepository = new GoodRepositoryStub();
        auditRepository = new AuditRepositoryStub();
        goodService = new GoodServiceImpl(goodRepository, mock(SsBarcodeRepository.class));
        auditService = new AuditService(auditRepository, "mbo-robot-unittest");
        loader = new GoodAliasEditActionLoader(auditRepository, goodRepository);

        clock = new ControlledClock(Clock.fixed(Dates.SUMMER_DAY.toInstant(), Dates.MSK_ZONE));
        periodStart = LocalDateTime.now(clock).truncatedTo(ChronoUnit.DAYS);
        periodEnd = periodStart.plusDays(1);

        tarifProvider = mock(BillingTarifProvider.class);
        BillingTarif tarif = new BillingTarif(2L, CATEGORY_ID, Dates.SUMMER_DAY.minusMonths(2)
            .toLocalDateTime(), PaidAction.MSKU_ALIAS_CHANGE, PRICE);
        when(tarifProvider.getTarif(PaidAction.MSKU_ALIAS_CHANGE, CATEGORY_ID))
            .thenReturn(tarif);
    }

    @Test
    public void countOneChange() {
        Good good = createGood();
        Msku msku = new Msku(good.getId(), SKU_ID);

        saveChanges(DYLAN, msku.withMonth(AUGUST), msku.withMonth(JUNE));
        clock.tickHour();
        LocalDateTime verifiedTime = LocalDateTime.now(clock);
        saveChanges(LUCAS, good, new Good(good).setState(GoodState.VERIFIED));


        List<BillingAction> actions = loader.loadActions(periodStart, periodEnd, tarifProvider);


        assertThat(actions).usingElementComparatorIgnoringFields("id", "auditActionId")
            .containsExactly(changeValueAction(verifiedTime, DYLAN));

        BillingAction action = actions.get(0);
        assertThat(action.getAuditActionId()).isNotNull();

        AuditAction linkedAction = getLinkedAuditAction(action);
        assertThat(linkedAction.getEntityType()).isEqualTo(EntityType.MSKU_ALIAS);
        assertThat(linkedAction.getNewValue()).isEqualTo(JUNE.toString());
    }

    @Test
    public void collapsing() {
        Good good = createGood();
        Msku msku = new Msku(good.getId(), SKU_ID);

        saveChanges(DYLAN, msku.withMonth(AUGUST), msku.withMonth(JUNE));
        clock.tickHour();
        saveChanges(DYLAN, msku.withMonth(JUNE), msku.withMonth(DECEMBER));
        clock.tickHour();
        LocalDateTime verifiedTime = LocalDateTime.now(clock);
        saveChanges(LUCAS, good, new Good(good).setState(GoodState.VERIFIED));


        List<BillingAction> actions = loader.loadActions(periodStart, periodEnd, tarifProvider);


        assertThat(actions).usingElementComparatorIgnoringFields("id", "auditActionId")
            .containsExactly(changeValueAction(verifiedTime, DYLAN));

        BillingAction action = actions.get(0);
        assertThat(action.getAuditActionId()).isNotNull();

        AuditAction auditAction = getLinkedAuditAction(action);
        assertThat(auditAction.getNewValue()).isEqualTo(DECEMBER.toString());
    }

    @Test
    public void collapsingToNothing() {
        Good good = createGood();
        Msku msku = new Msku(good.getId(), SKU_ID);

        saveChanges(DYLAN, msku.withMonth(AUGUST), msku.withMonth(JUNE));
        clock.tickHour();
        saveChanges(DYLAN, msku.withMonth(JUNE), msku.withMonth(AUGUST));
        clock.tickHour();
        saveChanges(LUCAS, good, new Good(good).setState(GoodState.VERIFIED));


        List<BillingAction> actions = loader.loadActions(periodStart, periodEnd, tarifProvider);


        assertThat(actions).isEmpty();
    }

    @Test
    public void countRemoving() {
        Good good = createGood();
        Msku msku = new Msku(good.getId(), SKU_ID);

        saveChanges(DYLAN, msku.withMonth(AUGUST), msku.withMonth(null));
        clock.tickHour();
        LocalDateTime verifiedTime = LocalDateTime.now(clock);
        saveChanges(LUCAS, good, new Good(good).setState(GoodState.VERIFIED));


        List<BillingAction> actions = loader.loadActions(periodStart, periodEnd, tarifProvider);


        assertThat(actions).usingElementComparatorIgnoringFields("id", "auditActionId")
            .containsExactly(changeValueAction(verifiedTime, DYLAN));
    }

    @Test
    public void dontCollapseDifferentUsers() {
        Good good = createGood();
        Msku msku = new Msku(good.getId(), SKU_ID);

        saveChanges(DYLAN, msku.withMonth(AUGUST), msku.withMonth(JULY));
        clock.tickHour();
        saveChanges(MABEL, msku.withMonth(JULY), msku.withMonth(SEPTEMBER));
        clock.tickHour();
        LocalDateTime verifiedTime = LocalDateTime.now(clock);
        saveChanges(LUCAS, good, new Good(good).setState(GoodState.VERIFIED));


        List<BillingAction> actions = loader.loadActions(periodStart, periodEnd, tarifProvider);


        assertThat(actions).usingElementComparatorIgnoringFields("id", "auditActionId")
            .containsExactly(
                changeValueAction(verifiedTime, DYLAN),
                changeValueAction(verifiedTime, MABEL)
            );
    }

    @Test
    public void countByPropertyName() {
        Good good = createGood();
        Msku msku = new Msku(good.getId(), SKU_ID);

        saveChanges(DYLAN, msku.withMonth(AUGUST), msku.withMonth(JULY));
        clock.tickHour();
        saveChanges(DYLAN, msku.withColor(JULY.toString()), msku.withColor(AUGUST.toString()));
        clock.tickHour();
        LocalDateTime verifiedTime = LocalDateTime.now(clock);
        saveChanges(LUCAS, good, new Good(good).setState(GoodState.VERIFIED));


        List<BillingAction> actions = loader.loadActions(periodStart, periodEnd, tarifProvider);


        assertThat(actions).usingElementComparatorIgnoringFields("id", "auditActionId")
            .containsExactly(
                changeValueAction(verifiedTime, DYLAN),
                changeValueAction(verifiedTime, DYLAN)
            );
    }

    private static BillingAction changeValueAction(LocalDateTime verifiedTime, String login) {
        return new BillingAction()
            .setBillingDate(verifiedTime)
            .setStaffLogin(login)
            .setCategoryId(CATEGORY_ID)
            .setPaidAction(PaidAction.MSKU_ALIAS_CHANGE)
            .setPriceKopeck(PRICE);
    }

    private AuditAction getLinkedAuditAction(BillingAction billingAction) {
        AuditActionFilter filter = new AuditActionFilter().addId(billingAction.getAuditActionId());
        List<AuditAction> linkedActions = auditRepository.findActions(filter);
        assertThat(linkedActions).hasSize(1);
        return linkedActions.get(0);
    }

    private void saveChanges(String user, Object before, Object after) {
        before = wrapIfNeeded(before);
        after = wrapIfNeeded(after);

        List<AuditAction> actions = auditService.createAuditActions(before, after);
        actions.forEach(a -> {
            a.setStaffLogin(user);
            a.setActionDate(LocalDateTime.now(clock));
        });
        if (before instanceof Msku) {
            // we avoid using ModelAuditService for simplicity
            // fix behavior difference here
            actions.forEach(a -> {
                // model audit
                a.setActionType(AuditService.getActionType(a.getOldValue(), a.getNewValue()));
            });
        }
        auditService.writeAuditActions(actions);
    }

    private Object wrapIfNeeded(Object object) {
        if (object == null) {
            return null;
        }
        if (object instanceof Good) {
            return new GoodWrapper((Good) object);
        }
        return object;
    }

    private Good createGood() {
        Good good = new Good();
        good.setCategoryId(CATEGORY_ID);
        return goodService.createGood(good);
    }

    @AuditedEntity(EntityType.MSKU_ALIAS)
    public static class Msku {
        public static final String COLOR = "Цвет";
        public static final String MONTH = "Месяц";

        private final long goodId;

        private final String mskuId;

        private final Month month;

        private final String color;

        private Msku(long goodId, String mskuId, Month month, String color) {
            this.goodId = goodId;
            this.mskuId = mskuId;
            this.month = month;
            this.color = color;
        }

        private Msku(long goodId, String mskuId) {
            this.goodId = goodId;
            this.mskuId = mskuId;
            this.month = null;
            this.color = null;
        }

        @AuditedEntityId
        public long getGoodId() {
            return goodId;
        }

        @AuditedEntityId(internal = false)
        public String getMskuId() {
            return mskuId;
        }

        @AuditedProperty(MONTH)
        public Month getMonth() {
            return month;
        }

        @AuditedProperty(COLOR)
        public String getColor() {
            return color;
        }

        public Msku withMonth(Month month) {
            return new Msku(goodId, mskuId, month, color);
        }

        public Msku withColor(String color) {
            return new Msku(goodId, mskuId, month, color);
        }
    }
}

