package ru.yandex.market.deepmind.common.services.rule_engine;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.deepmind.common.services.rule_engine.pojo.REBlock;
import ru.yandex.market.deepmind.common.services.rule_engine.pojo.REStockStorage;

import static ru.yandex.market.deepmind.common.db.jooq.generated.mbo_category.enums.OfferAvailability.ACTIVE;
import static ru.yandex.market.deepmind.common.db.jooq.generated.mbo_category.enums.OfferAvailability.DELISTED;
import static ru.yandex.market.deepmind.common.db.jooq.generated.mbo_category.enums.OfferAvailability.INACTIVE;
import static ru.yandex.market.deepmind.common.db.jooq.generated.mbo_category.enums.OfferAvailability.PENDING;
import static ru.yandex.market.deepmind.common.db.jooq.generated.mbo_category.enums.SupplierType.THIRD_PARTY;
import static ru.yandex.market.deepmind.common.db.jooq.generated.msku.enums.MskuStatusValue.IN_OUT;

/**
 * Test of {@link RuleEngineService} of file: {@link rule_engine_v6/SskuRulesV6_3P.drl}.
 */
public class RuleEngineServiceSsku3PTest extends BaseRuleEngineServiceTest {

    private RuleEngineService ruleEngineService;

    @Before
    public void setUp() throws Exception {
        ruleEngineService = RuleEngineService.createV6();
    }

    @Test
    public void dontChangeActive() {
        var ssku = ssku(1, "a", 10, ACTIVE, THIRD_PARTY);

        var stockStorage = new REStockStorage(ssku, 100).setFit(0)
            .setFitDisappearTime(Instant.now().minus(180, ChronoUnit.DAYS));

        var result = ruleEngineService.processStatuses(new REBlock()
            .addSskuStatus(ssku)
            .addStockStorages(stockStorage)
        );

        Assertions.assertThat(ssku.getAvailability()).isEqualTo(ACTIVE);
        Assertions.assertThat(result.getChangedSize()).isZero();
    }

    @Test
    public void testInactiveToDelisted() {
        var ssku = ssku(1, "a", 10, INACTIVE, THIRD_PARTY)
            .setStatusStartTime(Instant.now().minus(100, ChronoUnit.DAYS));

        var stockStorage = new REStockStorage(ssku, 100);
        stockStorage.setFit(0);

        ruleEngineService.processStatuses(new REBlock().addSskuStatus(ssku).addStockStorages(stockStorage));

        Assertions.assertThat(ssku.getAvailability())
            .isEqualTo(DELISTED);
    }

    @Test
    public void testInactiveIfHasFit() {
        var ssku = ssku(1, "a", 10, INACTIVE, THIRD_PARTY);

        var stockStorage = new REStockStorage(ssku, 123).setFit(1);

        ruleEngineService.processStatuses(new REBlock().addSskuStatus(ssku).addStockStorages(stockStorage));

        Assertions.assertThat(ssku.getAvailability())
            .isEqualTo(INACTIVE);
    }

    @Test
    public void testInactiveIfHasRecentFit() {
        var ssku = ssku(1, "a", 10, INACTIVE, THIRD_PARTY);

        var stockStorage = new REStockStorage(ssku, 100)
            .setFit(0)
            .setFitDisappearTime(Instant.now().minus(2, ChronoUnit.DAYS));

        ruleEngineService.processStatuses(new REBlock().addSskuStatus(ssku).addStockStorages(stockStorage));

        Assertions.assertThat(ssku.getAvailability())
            .isEqualTo(INACTIVE);
    }

    @Test
    public void testInactiveNotToDelistedIfRecentStatusChange() {
        var ssku = ssku(1, "a", 10, INACTIVE, THIRD_PARTY)
            .setStatusStartTime(Instant.now().minus(5, ChronoUnit.DAYS));

        var stockStorage = new REStockStorage(ssku, 101)
            .setFit(0)
            .setFitDisappearTime(Instant.now().minus(100, ChronoUnit.DAYS));

        ruleEngineService.processStatuses(new REBlock().addSskuStatus(ssku).addStockStorages(stockStorage));

        Assertions.assertThat(ssku.getAvailability())
            .isEqualTo(INACTIVE);
    }

    @Test
    public void testInactiveIfLongAgoFit() {
        var ssku = ssku(1, "a", 10, INACTIVE, THIRD_PARTY)
            .setStatusStartTime(Instant.now().minus(100, ChronoUnit.DAYS));

        var stockStorage = new REStockStorage(ssku, 101)
            .setFit(0)
            .setFitDisappearTime(Instant.now().minus(100, ChronoUnit.DAYS));

        ruleEngineService.processStatuses(new REBlock().addSskuStatus(ssku).addStockStorages(stockStorage));

        Assertions.assertThat(ssku.getAvailability())
            .isEqualTo(DELISTED);
    }

    @Test
    public void testInactiveToDelistedIfNoStock() {
        var ssku = ssku(1, "a", 10, INACTIVE, THIRD_PARTY)
            .setStatusStartTime(Instant.now().minus(100, ChronoUnit.DAYS));

        ruleEngineService.processStatuses(new REBlock().addSskuStatus(ssku));

        Assertions.assertThat(ssku.getAvailability())
            .isEqualTo(DELISTED);
    }

    @Test
    public void testInactiveToDelistedIfOtherStock() {
        var ssku = ssku(1, "a", 1, INACTIVE, THIRD_PARTY)
            .setStatusStartTime(Instant.now().minus(100, ChronoUnit.DAYS));

        var stockStorage = new REStockStorage(ssku, 100)
            .setFit(0);

        ruleEngineService.processStatuses(new REBlock().addSskuStatus(ssku).addStockStorages(stockStorage));

        Assertions.assertThat(ssku.getAvailability())
            .isEqualTo(DELISTED);
    }

    @Test
    public void testDelistedToInactive() {
        var ssku = ssku(1, "a", 2, DELISTED, THIRD_PARTY);

        var stockStorage = new REStockStorage(ssku, 102)
            .setFit(1);

        ruleEngineService.processStatuses(new REBlock().addSskuStatus(ssku).addStockStorages(stockStorage));

        Assertions.assertThat(ssku.getAvailability())
            .isEqualTo(INACTIVE);
    }

    @Test
    public void testDelistedWontChangeIfNoStock() {
        var ssku = ssku(1, "a", 100, DELISTED, THIRD_PARTY);

        ruleEngineService.processStatuses(new REBlock().addSskuStatus(ssku));

        Assertions.assertThat(ssku.getAvailability())
            .isEqualTo(DELISTED);
    }

    @Test
    public void testDelistedWontChangeIfStockIsZero() {
        var ssku = ssku(1, "a", 11, DELISTED, THIRD_PARTY);

        var stockStorage = new REStockStorage(ssku, 100)
            .setFit(0);

        ruleEngineService.processStatuses(new REBlock().addSskuStatus(ssku).addStockStorages(stockStorage));

        Assertions.assertThat(ssku.getAvailability())
            .isEqualTo(DELISTED);
    }

    @Test
    public void tp3FromActiveAndInOutOutsidePeriodToInactive() {
        var category50 = category(50);
        var msku = msku(100, IN_OUT, 50)
            .setInoutStartDate(LocalDate.now().minusDays(2))
            .setInoutFinishDate(LocalDate.now().minusDays(1));
        var ssku = ssku(1, "sku", msku, ACTIVE, THIRD_PARTY);

        var reBlock = new REBlock()
            .addMskuStatuses(msku)
            .addCategories(category50)
            .addSskuStatus(ssku)
            .addPurchasePrice(purchasePrice(ssku));
        ruleEngineService.processStatuses(reBlock);

        Assertions.assertThat(ssku.getAvailability()).isEqualTo(INACTIVE);
        Assertions.assertThat(ssku.getRulesThatChangedObject()).containsExactly(
            "TP3. ЕСЛИ (SSKU = Active или Pending) И (MSKU = In/Out)" +
                " И (текущая дата > даты окончания периода In/Out) " +
                "ТО (SSKU = Inactive)"
        );
    }

    @Test
    public void tp3FromPendingAndInOutOutsidePeriodToInactive() {
        var category50 = category(50);
        var msku = msku(100, IN_OUT, 50)
            .setInoutStartDate(LocalDate.now().minusDays(2))
            .setInoutFinishDate(LocalDate.now().minusDays(1));
        var ssku = ssku(1, "sku", msku, PENDING, THIRD_PARTY);

        var reBlock = new REBlock()
            .addMskuStatuses(msku)
            .addCategories(category50)
            .addSskuStatus(ssku)
            .addPurchasePrice(purchasePrice(ssku));
        ruleEngineService.processStatuses(reBlock);

        Assertions.assertThat(ssku.getAvailability()).isEqualTo(INACTIVE);
        Assertions.assertThat(ssku.getRulesThatChangedObject()).containsExactly(
            "TP3. ЕСЛИ (SSKU = Active или Pending) И (MSKU = In/Out)" +
                " И (текущая дата > даты окончания периода In/Out) " +
                "ТО (SSKU = Inactive)"
        );
    }

    @Test
    public void tp3FromActiveAndInOutInsidePeriodNothingChanged() {
        var category50 = category(50);
        var msku = msku(100, IN_OUT, 50)
            .setInoutStartDate(LocalDate.now().minusDays(-2))
            .setInoutFinishDate(LocalDate.now());
        var ssku = ssku(1, "sku", msku, ACTIVE, THIRD_PARTY);

        var reBlock = new REBlock()
            .addMskuStatuses(msku)
            .addCategories(category50)
            .addSskuStatus(ssku)
            .addPurchasePrice(purchasePrice(ssku));
        var result = ruleEngineService.processStatuses(reBlock);

        Assertions.assertThat(result.getChangedShopSkuKeys()).isEmpty();
    }
}
