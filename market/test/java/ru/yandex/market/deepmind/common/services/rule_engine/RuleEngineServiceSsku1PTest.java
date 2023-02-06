package ru.yandex.market.deepmind.common.services.rule_engine;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.deepmind.common.services.rule_engine.pojo.REBlock;
import ru.yandex.market.deepmind.common.services.rule_engine.pojo.REInvalidContract;
import ru.yandex.market.deepmind.common.services.rule_engine.pojo.REMskuCorefix;
import ru.yandex.market.deepmind.common.services.rule_engine.pojo.RESskuDeadstock;

import static ru.yandex.market.deepmind.common.db.jooq.generated.mbo_category.enums.OfferAvailability.ACTIVE;
import static ru.yandex.market.deepmind.common.db.jooq.generated.mbo_category.enums.OfferAvailability.DELISTED;
import static ru.yandex.market.deepmind.common.db.jooq.generated.mbo_category.enums.OfferAvailability.INACTIVE;
import static ru.yandex.market.deepmind.common.db.jooq.generated.mbo_category.enums.OfferAvailability.INACTIVE_TMP;
import static ru.yandex.market.deepmind.common.db.jooq.generated.mbo_category.enums.OfferAvailability.PENDING;
import static ru.yandex.market.deepmind.common.db.jooq.generated.mbo_category.enums.SupplierType.FIRST_PARTY;
import static ru.yandex.market.deepmind.common.db.jooq.generated.mbo_category.enums.SupplierType.REAL_SUPPLIER;
import static ru.yandex.market.deepmind.common.db.jooq.generated.msku.enums.MskuStatusValue.ARCHIVE;
import static ru.yandex.market.deepmind.common.db.jooq.generated.msku.enums.MskuStatusValue.EMPTY;
import static ru.yandex.market.deepmind.common.db.jooq.generated.msku.enums.MskuStatusValue.IN_OUT;
import static ru.yandex.market.deepmind.common.db.jooq.generated.msku.enums.MskuStatusValue.NPD;
import static ru.yandex.market.deepmind.common.db.jooq.generated.msku.enums.MskuStatusValue.REGULAR;
import static ru.yandex.market.deepmind.common.db.jooq.generated.msku.enums.MskuStatusValue.SEASONAL;
import static ru.yandex.market.deepmind.common.repository.logicstics.warehouse.DeepmindWarehouseRepository
    .CROSSDOCK_SOFINO_ID;
import static ru.yandex.market.deepmind.common.repository.logicstics.warehouse.DeepmindWarehouseRepository
    .EKATERINBURG_ID;
import static ru.yandex.market.deepmind.common.repository.logicstics.warehouse.DeepmindWarehouseRepository.ROSTOV_ID;
import static ru.yandex.market.deepmind.common.repository.logicstics.warehouse.DeepmindWarehouseRepository.SOFINO_ID;
import static ru.yandex.market.deepmind.common.repository.logicstics.warehouse.DeepmindWarehouseRepository.TOMILINO_ID;

/**
 * Test of {@link RuleEngineService} of file:
 * {@link rule_engine_v6/SskuRulesV6_1P_Active.drl},
 * {@link rule_engine_v6/SskuRulesV6_1P_Inactive.drl},
 * {@link rule_engine_v6/SskuRulesV6_1P_Delisted.drl}
 */
public class RuleEngineServiceSsku1PTest extends BaseRuleEngineServiceTest {

    private RuleEngineService ruleEngineService;

    @Before
    public void setUp() throws Exception {
        ruleEngineService = RuleEngineService.createV6();
    }

    //---------------------
    // from Active
    //---------------------

    @Test
    public void a1FromActiveIfNoStocksToDelisted() {
        var category50 = category(50);
        var msku = msku(100, EMPTY, 50);
        var ssku = ssku(1, "sku", msku, ACTIVE, REAL_SUPPLIER)
            .setStatusStartTime(Instant.now().minus(70, ChronoUnit.DAYS));

        var reBlock = new REBlock()
            .addMskuStatuses(msku)
            .addCategories(category50)
            .addSskuStatus(ssku)
            .addPurchasePrice(purchasePrice(ssku));
        ruleEngineService.processStatuses(reBlock);

        Assertions.assertThat(ssku.getAvailability()).isEqualTo(DELISTED);
        Assertions.assertThat(ssku.getRulesThatChangedObject()).containsExactly(
            "A1. ЕСЛИ (SSKU = Active) И (MSKU <> Seasonal) " +
                "И (Сток = 0 больше 60 дней от даты перехода в статус Active) ТО (SSKU = Delisted)"
        );
    }

    @Test
    public void a1FromActiveIfZeroStocksTooFarToDelisted() {
        var category50 = category(50);
        var msku = msku(100, EMPTY, 50);
        var ssku = ssku(1, "sku", msku, ACTIVE, REAL_SUPPLIER)
            .setStatusStartTime(Instant.now().minus(70, ChronoUnit.DAYS));
        var stockStorage = stockStorage(ssku, CROSSDOCK_SOFINO_ID, 0)
            .setFitDisappearTime(Instant.now().minus(80, ChronoUnit.DAYS));

        var reBlock = new REBlock()
            .addMskuStatuses(msku)
            .addCategories(category50)
            .addSskuStatus(ssku)
            .addStockStorages(stockStorage)
            .addPurchasePrice(purchasePrice(ssku));
        ruleEngineService.processStatuses(reBlock);

        Assertions.assertThat(ssku.getAvailability()).isEqualTo(DELISTED);
        Assertions.assertThat(ssku.getRulesThatChangedObject()).containsExactly(
            "A1. ЕСЛИ (SSKU = Active) И (MSKU <> Seasonal) " +
                "И (Сток = 0 больше 60 дней от даты перехода в статус Active) ТО (SSKU = Delisted)"
        );
    }

    @Test
    public void a1FromActiveIfZeroStocksRecentDoNothing() {
        var category50 = category(50);
        var msku = msku(100, EMPTY, 50);
        var ssku = ssku(1, "sku", msku, ACTIVE, REAL_SUPPLIER)
            .setStatusStartTime(Instant.now().minus(70, ChronoUnit.DAYS));
        var stockStorage = stockStorage(ssku, CROSSDOCK_SOFINO_ID, 0)
            .setFitDisappearTime(Instant.now().minus(40, ChronoUnit.DAYS));

        var reBlock = new REBlock()
            .addMskuStatuses(msku)
            .addCategories(category50)
            .addSskuStatus(ssku)
            .addStockStorages(stockStorage)
            .addSales(sales(ssku, 302, Instant.now().minus(40, ChronoUnit.DAYS)))
            .addPurchasePrice(purchasePrice(ssku));
        ruleEngineService.processStatuses(reBlock);

        Assertions.assertThat(ssku.getAvailability()).isEqualTo(ACTIVE);
        Assertions.assertThat(ssku.getRulesThatChangedObject()).isEmpty();
    }

    @Test
    public void a2FromActiveIfNoSalesToInactive() {
        var category50 = category(50);
        var msku = msku(100, EMPTY, 50);
        var ssku = ssku(1, "sku", msku, ACTIVE, REAL_SUPPLIER)
            .setStatusStartTime(Instant.now().minus(70, ChronoUnit.DAYS));
        var stockStorage = stockStorage(ssku, SOFINO_ID, 1);

        var reBlock = new REBlock()
            .addMskuStatuses(msku)
            .addCategories(category50)
            .addSskuStatus(ssku)
            .addStockStorages(stockStorage)
            .addPurchasePrice(purchasePrice(ssku));
        ruleEngineService.processStatuses(reBlock);

        Assertions.assertThat(ssku.getAvailability()).isEqualTo(INACTIVE);
        Assertions.assertThat(ssku.getRulesThatChangedObject()).containsExactly(
            "A2. ЕСЛИ (SSKU = Active) И (MSKU <> Seasonal) И (Сток > 0)" +
                " И (Продажи = 0 больше 60 дней от даты перехода в статус Active) ТО (SSKU = Inactive)"
        );
        // проверяем что время поменялось. Не сравниваем время на equals так как это сравнение будет флапать
        Assertions.assertThat(ssku.getStatusStartTime()).isAfter(Instant.now().minusSeconds(100));
    }

    @Test
    public void a2FromActiveIfSalesWereTooFarToInactive() {
        var category50 = category(50);
        var msku = msku(100, REGULAR, 50);
        var ssku = ssku(1, "sku", msku, ACTIVE, FIRST_PARTY)
            .setStatusStartTime(Instant.now().minus(70, ChronoUnit.DAYS));
        var sales = sales(ssku, 171, Instant.now().minus(200, ChronoUnit.DAYS));
        var stockStorage = stockStorage(ssku, ROSTOV_ID, 1);

        var reBlock = new REBlock()
            .addMskuStatuses(msku)
            .addCategories(category50)
            .addSskuStatus(ssku)
            .addSales(sales)
            .addStockStorages(stockStorage)
            .addPurchasePrice(purchasePrice(ssku));
        ruleEngineService.processStatuses(reBlock);

        Assertions.assertThat(ssku.getAvailability()).isEqualTo(INACTIVE);
        Assertions.assertThat(ssku.getRulesThatChangedObject()).containsExactly(
            "A2. ЕСЛИ (SSKU = Active) И (MSKU <> Seasonal) И (Сток > 0) " +
                "И (Продажи = 0 больше 60 дней от даты перехода в статус Active) ТО (SSKU = Inactive)"
        );
        // проверяем что время поменялось. Не сравниваем время на equals так как это сравнение будет флапать
        Assertions.assertThat(ssku.getStatusStartTime()).isAfter(Instant.now().minusSeconds(100));
    }

    @Test
    public void a2FromActiveIfSeveralSalesWereTooFarToInactive() {
        var category50 = category(50);
        var msku = msku(100, REGULAR, 50);
        var ssku = ssku(1, "sku", msku, ACTIVE, FIRST_PARTY)
            .setStatusStartTime(Instant.now().minus(70, ChronoUnit.DAYS));
        var sales = sales(ssku, 171, Instant.now().minus(200, ChronoUnit.DAYS));
        var sales2 = sales(ssku, 171, Instant.now().minus(201, ChronoUnit.DAYS));
        var sales3 = sales(ssku, 300, Instant.now().minus(70, ChronoUnit.DAYS));
        var stockStorage = stockStorage(ssku, ROSTOV_ID, 1);

        var reBlock = new REBlock()
            .addMskuStatuses(msku)
            .addCategories(category50)
            .addSskuStatus(ssku)
            .addSales(sales, sales2, sales3)
            .addStockStorages(stockStorage)
            .addPurchasePrice(purchasePrice(ssku));
        ruleEngineService.processStatuses(reBlock);

        Assertions.assertThat(ssku.getAvailability()).isEqualTo(INACTIVE);
        Assertions.assertThat(ssku.getRulesThatChangedObject()).containsExactly(
            "A2. ЕСЛИ (SSKU = Active) И (MSKU <> Seasonal) И (Сток > 0) " +
                "И (Продажи = 0 больше 60 дней от даты перехода в статус Active) ТО (SSKU = Inactive)"
        );
        // проверяем что время поменялось. Не сравниваем время на equals так как это сравнение будет флапать
        Assertions.assertThat(ssku.getStatusStartTime()).isAfter(Instant.now().minusSeconds(100));
    }

    @Test
    public void a2FromActiveIfSeveralSalesWithRecentWereTooFarToInactive() {
        var category50 = category(50);
        var msku = msku(100, REGULAR, 50);
        var ssku = ssku(1, "sku", msku, ACTIVE, FIRST_PARTY)
            .setStatusStartTime(Instant.now().minus(70, ChronoUnit.DAYS));
        var sales = sales(ssku, 171, Instant.now().minus(200, ChronoUnit.DAYS));
        var sales2 = sales(ssku, 171, Instant.now().minus(201, ChronoUnit.DAYS));
        var sales3 = sales(ssku, 300, Instant.now().minus(70, ChronoUnit.DAYS));
        var sales4 = sales(ssku, 302, Instant.now().minus(10, ChronoUnit.DAYS));
        var stockStorage = stockStorage(ssku, ROSTOV_ID, 1);

        var reBlock = new REBlock()
            .addMskuStatuses(msku)
            .addCategories(category50)
            .addSskuStatus(ssku)
            .addSales(sales, sales2, sales3, sales4)
            .addStockStorages(stockStorage)
            .addPurchasePrice(purchasePrice(ssku));
        ruleEngineService.processStatuses(reBlock);

        Assertions.assertThat(ssku.getAvailability()).isEqualTo(ACTIVE);
        Assertions.assertThat(ssku.getRulesThatChangedObject()).isEmpty();
    }

    @Test
    public void a2FromActiveIfSalesWereRecentlyNothingChanged() {
        var category50 = category(50);
        var msku = msku(100, NPD, 50);
        var ssku = ssku(1, "sku", msku, ACTIVE, FIRST_PARTY)
            .setStatusStartTime(Instant.now().minus(70, ChronoUnit.DAYS));
        var sales = sales(ssku, 300, Instant.now().minus(1, ChronoUnit.DAYS));

        var reBlock = new REBlock()
            .addMskuStatuses(msku)
            .addCategories(category50)
            .addSskuStatus(ssku)
            .addSales(sales)
            .addStockStorages(stockStorage(ssku, ROSTOV_ID, 8))
            .addPurchasePrice(purchasePrice(ssku));
        var result = ruleEngineService.processStatuses(reBlock);

        Assertions.assertThat(result.getChangedShopSkuKeys()).isEmpty();
    }

    @Test
    public void a2FromRecentActiveIsSalesWereTooFarNothingChanged() {
        var category50 = category(50);
        var msku = msku(100, NPD, 50);
        var ssku = ssku(1, "sku", msku, ACTIVE, FIRST_PARTY)
            .setStatusStartTime(Instant.now().minus(1, ChronoUnit.DAYS));
        var sales = sales(ssku, 300, Instant.now().minus(1, ChronoUnit.DAYS));

        var reBlock = new REBlock()
            .addMskuStatuses(msku)
            .addCategories(category50)
            .addSskuStatus(ssku)
            .addSales(sales)
            .addPurchasePrice(purchasePrice(ssku));
        var result = ruleEngineService.processStatuses(reBlock);

        Assertions.assertThat(result.getChangedShopSkuKeys()).isEmpty();
    }

    @Test
    public void ap3FromActiveAndSeasonalOutsidePeriodToInactive() {
        var category50 = category(50, 100L);
        var msku = msku(100, SEASONAL, 50).setSeasonId(100L);
        var season = season(100L,
            LocalDate.now().plusDays(1), LocalDate.now().plusDays(2));
        var ssku = ssku(1, "sku", msku, ACTIVE, REAL_SUPPLIER);

        var reBlock = new REBlock()
            .addMskuStatuses(msku)
            .addCategories(category50)
            .addSskuStatus(ssku)
            .addSeasons(season)
            .addPurchasePrice(purchasePrice(ssku));
        ruleEngineService.processStatuses(reBlock);

        Assertions.assertThat(ssku.getAvailability()).isEqualTo(INACTIVE);
        Assertions.assertThat(ssku.getRulesThatChangedObject()).containsExactly(
            "AP3. ЕСЛИ (SSKU = Active или Pending)" +
                " И (MSKU = Seasonal) И (текущая дата > даты окончания периода Seasonal)" +
                " ТО (SSKU = Inactive)"
        );
        // проверяем что время поменялось. Не сравниваем время на equals так как это сравнение будет флапать
        Assertions.assertThat(ssku.getStatusStartTime()).isAfter(Instant.now().minusSeconds(100));
    }

    @Test
    public void ap3FromActiveAndSeasonalOutsidePeriodsToInactive() {
        var category50 = category(50, 100L);
        var msku = msku(100, SEASONAL, 50).setSeasonId(100L);
        var season = season(100L,
            LocalDate.now().plusDays(-2), LocalDate.now().plusDays(-1),
            LocalDate.now().plusDays(1), LocalDate.now().plusDays(1)
        );
        var ssku = ssku(1, "sku", msku, ACTIVE, REAL_SUPPLIER);

        var reBlock = new REBlock()
            .addMskuStatuses(msku)
            .addCategories(category50)
            .addSskuStatus(ssku)
            .addSeasons(season)
            .addPurchasePrice(purchasePrice(ssku));
        ruleEngineService.processStatuses(reBlock);

        Assertions.assertThat(ssku.getAvailability()).isEqualTo(INACTIVE);
        Assertions.assertThat(ssku.getRulesThatChangedObject()).containsExactly(
            "AP3. ЕСЛИ (SSKU = Active или Pending)" +
                " И (MSKU = Seasonal) И (текущая дата > даты окончания периода Seasonal)" +
                " ТО (SSKU = Inactive)"
        );
        // проверяем что время поменялось. Не сравниваем время на equals так как это сравнение будет флапать
        Assertions.assertThat(ssku.getStatusStartTime()).isAfter(Instant.now().minusSeconds(100));
    }

    @Test
    public void ap3FromActiveAndSeasonalInPeriodNothingChanged() {
        var category50 = category(50, 100L);
        var msku = msku(100, SEASONAL, 50).setSeasonId(100L);
        var season = season(100L,
            LocalDate.now().plusDays(-2), LocalDate.now().plusDays(1)
        );
        var ssku = ssku(1, "sku", msku, ACTIVE, REAL_SUPPLIER);

        var reBlock = new REBlock()
            .addMskuStatuses(msku)
            .addCategories(category50)
            .addSskuStatus(ssku)
            .addSeasons(season)
            .addPurchasePrice(purchasePrice(ssku));
        var result = ruleEngineService.processStatuses(reBlock);

        Assertions.assertThat(result.getChangedShopSkuKeys()).isEmpty();
    }

    @Test
    public void ap3FromActiveAndSeasonalInOnePeriodNothingChanged() {
        var category50 = category(50, 100L);
        var msku = msku(100, SEASONAL, 50).setSeasonId(100L);
        var season = season(100L,
            LocalDate.now().plusDays(-2), LocalDate.now().plusDays(1),
            LocalDate.now().plusDays(1), LocalDate.now().plusDays(2)
        );
        var ssku = ssku(1, "sku", msku, ACTIVE, REAL_SUPPLIER);

        var reBlock = new REBlock()
            .addMskuStatuses(msku)
            .addCategories(category50)
            .addSskuStatus(ssku)
            .addSeasons(season)
            .addPurchasePrice(purchasePrice(ssku));
        var result = ruleEngineService.processStatuses(reBlock);

        Assertions.assertThat(result.getChangedShopSkuKeys()).isEmpty();
    }

    @Test
    public void a4FromActiveAndInOutOutsidePeriodToInactive() {
        var category50 = category(50);
        var msku = msku(100, IN_OUT, 50)
            .setInoutStartDate(LocalDate.now().minusDays(2))
            .setInoutFinishDate(LocalDate.now().minusDays(1));
        var ssku = ssku(1, "sku", msku, ACTIVE, REAL_SUPPLIER);

        var reBlock = new REBlock()
            .addMskuStatuses(msku)
            .addCategories(category50)
            .addSskuStatus(ssku)
            .addPurchasePrice(purchasePrice(ssku));
        ruleEngineService.processStatuses(reBlock);

        Assertions.assertThat(ssku.getAvailability()).isEqualTo(INACTIVE);
        Assertions.assertThat(ssku.getRulesThatChangedObject()).containsExactly(
            "AP4. ЕСЛИ (SSKU = Active или Pending) И (MSKU = In/Out)" +
                " И (текущая дата > даты окончания периода In/Out) " +
                "ТО (SSKU = Inactive)"
        );
    }

    @Test
    public void a4FromActiveAndInOutInsidePeriodNothingChanged() {
        var category50 = category(50);
        var msku = msku(100, IN_OUT, 50)
            .setInoutStartDate(LocalDate.now().minusDays(-2))
            .setInoutFinishDate(LocalDate.now());
        var ssku = ssku(1, "sku", msku, ACTIVE, REAL_SUPPLIER);

        var reBlock = new REBlock()
            .addMskuStatuses(msku)
            .addCategories(category50)
            .addSskuStatus(ssku)
            .addPurchasePrice(purchasePrice(ssku));
        var result = ruleEngineService.processStatuses(reBlock);

        Assertions.assertThat(result.getChangedShopSkuKeys()).isEmpty();
    }

    @Test
    public void ap5ActiveWithoutPurchasePriceToInactiveTmp() {
        var category50 = category(50);
        var msku = msku(100, REGULAR, 50);
        var ssku = ssku(1, "sku", msku, ACTIVE, REAL_SUPPLIER);

        var reBlock = new REBlock()
            .addMskuStatuses(msku)
            .addCategories(category50)
            .addSskuStatus(ssku);
        ruleEngineService.processStatuses(reBlock);

        Assertions.assertThat(ssku.getAvailability()).isEqualTo(INACTIVE_TMP);
        Assertions.assertThat(ssku.getRulesThatChangedObject()).containsExactly(
            "AP5. ЕСЛИ (SSKU = Active или Pending) И (нет закупочной цены) " +
                "ТО (SSKU = Inactive_tmp) И (Причина = Нет закупочной цены) И (Дата окончания = null)"
        );
        Assertions.assertThat(ssku.isHasNoPurchasePrice()).isTrue();
        Assertions.assertThat(ssku.getStatusFinishTime()).isNull();
    }

    @Test
    public void a6FromRecentActiveAndArchiveWithoutStocksDoNothing() {
        var category50 = category(50);
        var msku = msku(100, ARCHIVE, 50);
        var ssku = ssku(1, "sku", msku, ACTIVE, REAL_SUPPLIER);

        var reBlock = new REBlock()
            .addMskuStatuses(msku)
            .addCategories(category50)
            .addSskuStatus(ssku)
            .addPurchasePrice(purchasePrice(ssku));
        var result = ruleEngineService.processStatuses(reBlock);

        Assertions.assertThat(result.getChangedShopSkuKeys()).isEmpty();
    }

    //---------------------
    // from Inactive_tmp
    //---------------------

    @Test
    public void it1PlannedStartDate() {
        var startTime = Instant.now();
        var finishTime = Instant.now().plus(10, ChronoUnit.DAYS);

        var category50 = category(50);
        var msku = msku(100, REGULAR, 50);
        var ssku = ssku(1, "sku", msku, ACTIVE, REAL_SUPPLIER)
            .setStatusStartTime(Instant.parse("2020-12-03T10:15:30.00Z"))
            .setPlannedStartAt(startTime)
            .setPlannedFinishAt(finishTime);
        var stockStorage = stockStorage(ssku, TOMILINO_ID, 1);
        var sales = sales(ssku, 300, Instant.now().minus(10, ChronoUnit.DAYS));

        var reBlock = new REBlock()
            .addMskuStatuses(msku)
            .addCategories(category50)
            .addSskuStatus(ssku)
            .addStockStorages(stockStorage)
            .addPurchasePrice(purchasePrice(ssku))
            .addSales(sales);
        ruleEngineService.processStatuses(reBlock);

        Assertions.assertThat(ssku.getAvailability()).isEqualTo(INACTIVE_TMP);
        Assertions.assertThat(ssku.getStatusFinishTime()).isEqualTo(finishTime);
        Assertions.assertThat(ssku.getRulesThatChangedObject()).containsExactly(
            "IT1. ЕСЛИ (Указан период для Inactive_tmp)" +
                " И (Текущая дата >= даты начала периода Inactive_tmp) " +
                "ТО (SSKU = Inactive_tmp)"
        );
    }

    @Test
    public void it1FromInactiveTmpDoNothing() {
        var category50 = category(50);
        var msku = msku(100, EMPTY, 50);
        var ssku = ssku(1, "sku", msku, INACTIVE_TMP, REAL_SUPPLIER)
            .setStatusStartTime(Instant.now().minus(70, ChronoUnit.DAYS))
            .setStatusFinishTime(Instant.now().plus(1, ChronoUnit.DAYS));

        var reBlock = new REBlock()
            .addMskuStatuses(msku)
            .addCategories(category50)
            .addSskuStatus(ssku);
        var result = ruleEngineService.processStatuses(reBlock);

        Assertions.assertThat(result.getChangedShopSkuKeys()).isEmpty();
    }

    @Test
    public void it2FromInactiveTmpToPending() {
        var category50 = category(50);
        var msku = msku(100, EMPTY, 50);
        var ssku = ssku(1, "sku", msku, INACTIVE_TMP, REAL_SUPPLIER)
            .setStatusStartTime(Instant.now().minus(70, ChronoUnit.DAYS))
            .setStatusFinishTime(Instant.now());

        var reBlock = new REBlock()
            .addMskuStatuses(msku)
            .addCategories(category50)
            .addSskuStatus(ssku)
            .addPurchasePrice(purchasePrice(ssku));
        ruleEngineService.processStatuses(reBlock);

        Assertions.assertThat(ssku.getAvailability()).isEqualTo(PENDING);
        Assertions.assertThat(ssku.getRulesThatChangedObject()).containsExactly(
            "IT2. ЕСЛИ (SSKU = Inactive_tmp)" +
                " И (Текущая дата > даты окончания периода Inactive_tmp)" +
                " И (прошлый статус <> Delisted или Inactive)" +
                " ТО (SSKU = Pending)"
        );
        // проверяем что время поменялось. Не сравниваем время на equals так как это сравнение будет флапать
        Assertions.assertThat(ssku.getStatusStartTime()).isAfter(Instant.now().minusSeconds(100));
        Assertions.assertThat(ssku.getStatusFinishTime()).isNull();
    }

    @Test
    public void it2FromInactiveTmpWithPreviousStatusToPending() {
        var category50 = category(50);
        var msku = msku(100, EMPTY, 50);
        var ssku = ssku(1, "sku", msku, INACTIVE_TMP, REAL_SUPPLIER)
            .setPreviousAvailability(ACTIVE)
            .setStatusStartTime(Instant.now().minus(70, ChronoUnit.DAYS))
            .setStatusFinishTime(Instant.now());

        var reBlock = new REBlock()
            .addMskuStatuses(msku)
            .addCategories(category50)
            .addSskuStatus(ssku)
            .addPurchasePrice(purchasePrice(ssku));
        ruleEngineService.processStatuses(reBlock);

        Assertions.assertThat(ssku.getAvailability()).isEqualTo(PENDING);
        Assertions.assertThat(ssku.getRulesThatChangedObject()).containsExactly(
            "IT2. ЕСЛИ (SSKU = Inactive_tmp)" +
                " И (Текущая дата > даты окончания периода Inactive_tmp)" +
                " И (прошлый статус <> Delisted или Inactive)" +
                " ТО (SSKU = Pending)"
        );
        // проверяем что время поменялось. Не сравниваем время на equals так как это сравнение будет флапать
        Assertions.assertThat(ssku.getStatusStartTime()).isAfter(Instant.now().minusSeconds(100));
        Assertions.assertThat(ssku.getStatusFinishTime()).isNull();
    }

    @Test
    public void it3FromInactiveTmpWithPreviousStatusDelistedToInactive() {
        var category50 = category(50);
        var msku = msku(100, EMPTY, 50);
        var ssku = ssku(1, "sku", msku, INACTIVE_TMP, REAL_SUPPLIER)
            .setPreviousAvailability(DELISTED)
            .setStatusStartTime(Instant.now().minus(70, ChronoUnit.DAYS))
            .setStatusFinishTime(Instant.now());

        var reBlock = new REBlock()
            .addMskuStatuses(msku)
            .addCategories(category50)
            .addSskuStatus(ssku)
            .addPurchasePrice(purchasePrice(ssku));
        ruleEngineService.processStatuses(reBlock);

        Assertions.assertThat(ssku.getAvailability()).isEqualTo(INACTIVE);
        Assertions.assertThat(ssku.getRulesThatChangedObject()).containsExactly(
            "IT3. ЕСЛИ (SSKU = Inactive_tmp)" +
                " И (Текущая дата > даты окончания периода Inactive_tmp)" +
                " И (прошлый статус = Delisted или Inactive)" +
                " ТО (SSKU = Inactive)"
        );
        // проверяем что время поменялось. Не сравниваем время на equals так как это сравнение будет флапать
        Assertions.assertThat(ssku.getStatusStartTime()).isAfter(Instant.now().minusSeconds(100));
        Assertions.assertThat(ssku.getStatusFinishTime()).isNull();
    }

    @Test
    public void it5FromInactiveTmpWithValidContractToPending() {
        var category50 = category(50);
        var msku = msku(100, EMPTY, 50);
        var ssku = ssku(1, "sku", msku, INACTIVE_TMP, REAL_SUPPLIER);
        ssku.setHasNoValidContract(true);
        var reBlock = new REBlock()
            .addMskuStatuses(msku)
            .addCategories(category50)
            .addSskuStatus(ssku)
            .addPurchasePrice(purchasePrice(ssku));
        ruleEngineService.processStatuses(reBlock);

        Assertions.assertThat(ssku.getAvailability()).isEqualTo(PENDING);
        Assertions.assertThat(ssku.getRulesThatChangedObject()).containsExactly(
            "IT5. ЕСЛИ (SSKU = Inactive_tmp) И (Причина = Нет действующего договора) И (есть действующий договор)" +
                " ТО (SSKU = Pending)"
        );
        Assertions.assertThat(ssku.isHasNoPurchasePrice()).isFalse();
        Assertions.assertThat(ssku.getStatusFinishTime()).isNull();
    }

    @Test
    public void it3FromInactiveTmpWithPreviousStatusInactiveToInactive() {
        var category50 = category(50);
        var msku = msku(100, EMPTY, 50);
        var ssku = ssku(1, "sku", msku, INACTIVE_TMP, REAL_SUPPLIER)
            .setPreviousAvailability(INACTIVE)
            .setStatusStartTime(Instant.now().minus(70, ChronoUnit.DAYS))
            .setStatusFinishTime(Instant.now());

        var reBlock = new REBlock()
            .addMskuStatuses(msku)
            .addCategories(category50)
            .addSskuStatus(ssku)
            .addPurchasePrice(purchasePrice(ssku));
        ruleEngineService.processStatuses(reBlock);

        Assertions.assertThat(ssku.getAvailability()).isEqualTo(INACTIVE);
        Assertions.assertThat(ssku.getRulesThatChangedObject()).containsExactly(
            "IT3. ЕСЛИ (SSKU = Inactive_tmp)" +
                " И (Текущая дата > даты окончания периода Inactive_tmp)" +
                " И (прошлый статус = Delisted или Inactive)" +
                " ТО (SSKU = Inactive)"
        );
        // проверяем что время поменялось. Не сравниваем время на equals так как это сравнение будет флапать
        Assertions.assertThat(ssku.getStatusStartTime()).isAfter(Instant.now().minusSeconds(100));
        Assertions.assertThat(ssku.getStatusFinishTime()).isNull();
    }

    @Test
    public void inactiveTmpWithFalseHasNoPurchasePriceAndNullFinishTimeStayTheSame() {
        var category50 = category(50);
        var msku = msku(100, EMPTY, 50);
        var ssku = ssku(1, "sku", msku, INACTIVE_TMP, REAL_SUPPLIER)
            .setStatusStartTime(Instant.now().minus(70, ChronoUnit.DAYS))
            .setStatusFinishTime(null)
            .setHasNoPurchasePrice(false)
            .setComment("some comment");

        var reBlock = new REBlock()
            .addMskuStatuses(msku)
            .addCategories(category50)
            .addSskuStatus(ssku)
            .addPurchasePrice(purchasePrice(ssku));
        ruleEngineService.processStatuses(reBlock);

        Assertions.assertThat(ssku.getAvailability()).isEqualTo(INACTIVE_TMP);
        Assertions.assertThat(ssku.getComment()).isEqualTo("some comment");
        Assertions.assertThat(ssku.getStatusStartTime()).isEqualTo(ssku.getStatusStartTime());
        Assertions.assertThat(ssku.getStatusFinishTime()).isNull();
    }

    //---------------------
    // from Inactive
    //---------------------

    @Test
    public void i2FromInactiveWithoutStocksToDelisted() {
        var category50 = category(50);
        var msku = msku(100, EMPTY, 50);
        var ssku = ssku(1, "sku", msku, INACTIVE, REAL_SUPPLIER)
            .setStatusStartTime(Instant.now().minus(70, ChronoUnit.DAYS));

        var reBlock = new REBlock()
            .addMskuStatuses(msku)
            .addCategories(category50)
            .addSskuStatus(ssku)
            .addPurchasePrice(purchasePrice(ssku));
        ruleEngineService.processStatuses(reBlock);

        Assertions.assertThat(ssku.getAvailability()).isEqualTo(DELISTED);
        Assertions.assertThat(ssku.getRulesThatChangedObject()).containsExactly(
            "I2. ЕСЛИ (SSKU = Inactive) И (MSKU <> Seasonal)" +
                " И (Сток = 0 больше 60 дней от даты перехода в статус Inactive)" +
                " ТО (SSKU = Delisted)"
        );
        // проверяем что время поменялось. Не сравниваем время на equals так как это сравнение будет флапать
        Assertions.assertThat(ssku.getStatusStartTime()).isAfter(Instant.now().minusSeconds(100));
    }

    @Test
    public void i2FromInactiveWithZeroStocksToDelisted() {
        var category50 = category(50);
        var msku = msku(100, EMPTY, 50);
        var ssku = ssku(1, "sku", msku, INACTIVE, REAL_SUPPLIER)
            .setStatusStartTime(Instant.now().minus(70, ChronoUnit.DAYS));
        var stocks = stockStorage(ssku, TOMILINO_ID, 0)
            .setFitDisappearTime(Instant.now().minus(90, ChronoUnit.DAYS));

        var reBlock = new REBlock()
            .addMskuStatuses(msku)
            .addCategories(category50)
            .addSskuStatus(ssku)
            .addStockStorages(stocks)
            .addPurchasePrice(purchasePrice(ssku));
        ruleEngineService.processStatuses(reBlock);

        Assertions.assertThat(ssku.getAvailability()).isEqualTo(DELISTED);
        Assertions.assertThat(ssku.getRulesThatChangedObject()).containsExactly(
            "I2. ЕСЛИ (SSKU = Inactive) И (MSKU <> Seasonal)" +
                " И (Сток = 0 больше 60 дней от даты перехода в статус Inactive)" +
                " ТО (SSKU = Delisted)"
        );
    }

    @Test
    public void i2FromInactiveWithZeroStocksWithoutDisappearToDelisted() {
        var category50 = category(50);
        var msku = msku(100, EMPTY, 50);
        var ssku = ssku(1, "sku", msku, INACTIVE, REAL_SUPPLIER)
            .setStatusStartTime(Instant.now().minus(70, ChronoUnit.DAYS));
        var stocks = stockStorage(ssku, TOMILINO_ID, 0)
            .setFitDisappearTime(null);

        var reBlock = new REBlock()
            .addMskuStatuses(msku)
            .addCategories(category50)
            .addSskuStatus(ssku)
            .addStockStorages(stocks)
            .addPurchasePrice(purchasePrice(ssku));
        ruleEngineService.processStatuses(reBlock);

        Assertions.assertThat(ssku.getAvailability()).isEqualTo(DELISTED);
        Assertions.assertThat(ssku.getRulesThatChangedObject()).containsExactly(
            "I2. ЕСЛИ (SSKU = Inactive) И (MSKU <> Seasonal)" +
                " И (Сток = 0 больше 60 дней от даты перехода в статус Inactive)" +
                " ТО (SSKU = Delisted)"
        );
    }

    @Test
    public void i2FromInactiveWithNegativeStocksToDelisted() {
        var category50 = category(50);
        var msku = msku(100, EMPTY, 50);
        var ssku = ssku(1, "sku", msku, INACTIVE, REAL_SUPPLIER)
            .setStatusStartTime(Instant.now().minus(70, ChronoUnit.DAYS));
        var stocks = stockStorage(ssku, TOMILINO_ID, -1)
            .setFitDisappearTime(Instant.now().minus(90, ChronoUnit.DAYS));

        var reBlock = new REBlock()
            .addMskuStatuses(msku)
            .addCategories(category50)
            .addSskuStatus(ssku)
            .addStockStorages(stocks)
            .addPurchasePrice(purchasePrice(ssku));
        ruleEngineService.processStatuses(reBlock);

        Assertions.assertThat(ssku.getAvailability()).isEqualTo(DELISTED);
        Assertions.assertThat(ssku.getRulesThatChangedObject()).containsExactly(
            "I2. ЕСЛИ (SSKU = Inactive) И (MSKU <> Seasonal)" +
                " И (Сток = 0 больше 60 дней от даты перехода в статус Inactive)" +
                " ТО (SSKU = Delisted)"
        );
    }

    @Test
    public void i2FromInactiveWithNegativeStocksWithoutDissapearToDelisted() {
        var category50 = category(50);
        var msku = msku(100, EMPTY, 50);
        var ssku = ssku(1, "sku", msku, INACTIVE, REAL_SUPPLIER)
            .setStatusStartTime(Instant.now().minus(70, ChronoUnit.DAYS));
        var stocks = stockStorage(ssku, TOMILINO_ID, -1)
            .setFitDisappearTime(null);

        var reBlock = new REBlock()
            .addMskuStatuses(msku)
            .addCategories(category50)
            .addSskuStatus(ssku)
            .addStockStorages(stocks)
            .addPurchasePrice(purchasePrice(ssku));
        ruleEngineService.processStatuses(reBlock);

        Assertions.assertThat(ssku.getAvailability()).isEqualTo(DELISTED);
        Assertions.assertThat(ssku.getRulesThatChangedObject()).containsExactly(
            "I2. ЕСЛИ (SSKU = Inactive) И (MSKU <> Seasonal)" +
                " И (Сток = 0 больше 60 дней от даты перехода в статус Inactive)" +
                " ТО (SSKU = Delisted)"
        );
    }

    @Test
    public void i2FromInactiveWithZeroRecentStocksTDoNothing() {
        var category50 = category(50);
        var msku = msku(100, EMPTY, 50);
        var ssku = ssku(1, "sku", msku, INACTIVE, REAL_SUPPLIER)
            .setStatusStartTime(Instant.now().minus(70, ChronoUnit.DAYS));
        var stocks = stockStorage(ssku, TOMILINO_ID, 0)
            .setFitDisappearTime(Instant.now().minus(60, ChronoUnit.DAYS));

        var reBlock = new REBlock()
            .addMskuStatuses(msku)
            .addCategories(category50)
            .addSskuStatus(ssku)
            .addStockStorages(stocks)
            .addPurchasePrice(purchasePrice(ssku));
        var result = ruleEngineService.processStatuses(reBlock);

        Assertions.assertThat(result.getChangedShopSkuKeys()).isEmpty();
    }

    @Test
    public void i2FromInactiveWithStocksTDoNothing() {
        var category50 = category(50);
        var msku = msku(100, EMPTY, 50);
        var ssku = ssku(1, "sku", msku, INACTIVE, REAL_SUPPLIER)
            .setStatusStartTime(Instant.now().minus(70, ChronoUnit.DAYS));
        var stocks = stockStorage(ssku, TOMILINO_ID, 10)
            .setFitDisappearTime(Instant.now().minus(90, ChronoUnit.DAYS))
            .setFitAppearTime(Instant.now().minus(10, ChronoUnit.DAYS));

        var reBlock = new REBlock()
            .addMskuStatuses(msku)
            .addCategories(category50)
            .addSskuStatus(ssku)
            .addStockStorages(stocks)
            .addPurchasePrice(purchasePrice(ssku));
        var result = ruleEngineService.processStatuses(reBlock);

        Assertions.assertThat(result.getChangedShopSkuKeys()).isEmpty();
    }

    @Test
    public void i3FromInactiveAndSeasonalWithoutStocksToDelisted() {
        var category50 = category(50).setSeasonId(100L);
        var msku = msku(100, SEASONAL, 50).setSeasonId(100L);
        var season = season(100L, LocalDate.now().minusDays(2), LocalDate.now().minusDays(1));
        var ssku = ssku(1, "sku", msku, INACTIVE, REAL_SUPPLIER)
            .setStatusStartTime(Instant.now().minus(70, ChronoUnit.DAYS))
            .setModifiedByUser(true);

        var reBlock = new REBlock()
            .addMskuStatuses(msku)
            .addCategories(category50)
            .addSskuStatus(ssku)
            .addSeasons(season)
            .addPurchasePrice(purchasePrice(ssku));
        ruleEngineService.processStatuses(reBlock);

        Assertions.assertThat(ssku.getAvailability()).isEqualTo(DELISTED);
        Assertions.assertThat(ssku.getRulesThatChangedObject()).containsExactly(
            "I3. ЕСЛИ (SSKU = Inactive) " +
                "И (MSKU = Seasonal) " +
                "И (Сток = 0 больше 60 дней от даты перехода в статус Inactive) " +
                "И (переход в статус Inactive сделан вручную пользователем) " +
                "ТО (SSKU = Delisted)"
        );
    }

    @Test
    public void i3FromInactiveAndSeasonalWithZeroStocksToDelisted() {
        var category50 = category(50).setSeasonId(100L);
        var msku = msku(100, SEASONAL, 50).setSeasonId(100L);
        var season = season(100L, LocalDate.now().minusDays(2), LocalDate.now().minusDays(1));
        var ssku = ssku(1, "sku", msku, INACTIVE, REAL_SUPPLIER)
            .setStatusStartTime(Instant.now().minus(70, ChronoUnit.DAYS))
            .setModifiedByUser(true);
        var storage = stockStorage(ssku, SOFINO_ID, 0)
            .setFitDisappearTime(Instant.now().minus(90, ChronoUnit.DAYS));

        var reBlock = new REBlock()
            .addMskuStatuses(msku)
            .addCategories(category50)
            .addSskuStatus(ssku)
            .addSeasons(season)
            .addStockStorages(storage)
            .addPurchasePrice(purchasePrice(ssku));
        ruleEngineService.processStatuses(reBlock);

        Assertions.assertThat(ssku.getAvailability()).isEqualTo(DELISTED);
        Assertions.assertThat(ssku.getRulesThatChangedObject()).containsExactly(
            "I3. ЕСЛИ (SSKU = Inactive) " +
                "И (MSKU = Seasonal) " +
                "И (Сток = 0 больше 60 дней от даты перехода в статус Inactive) " +
                "И (переход в статус Inactive сделан вручную пользователем) " +
                "ТО (SSKU = Delisted)"
        );
    }

    @Test
    public void i3FromRecentInactiveAndSeasonalWithZeroStocksDoNothing() {
        var category50 = category(50).setSeasonId(100L);
        var msku = msku(100, SEASONAL, 50).setSeasonId(100L);
        var season = season(100L, LocalDate.now().minusDays(2), LocalDate.now().minusDays(1));
        var ssku = ssku(1, "sku", msku, INACTIVE, REAL_SUPPLIER)
            .setModifiedByUser(true);
        var storage = stockStorage(ssku, SOFINO_ID, 0)
            .setFitDisappearTime(Instant.now().minus(90, ChronoUnit.DAYS));

        var reBlock = new REBlock()
            .addMskuStatuses(msku)
            .addCategories(category50)
            .addSskuStatus(ssku)
            .addSeasons(season)
            .addStockStorages(storage)
            .addPurchasePrice(purchasePrice(ssku));
        var result = ruleEngineService.processStatuses(reBlock);

        Assertions.assertThat(result.getChangedShopSkuKeys()).isEmpty();
    }

    @Test
    public void i3FromInactiveAndSeasonalWithRecentZeroStocksDoNothing() {
        var category50 = category(50).setSeasonId(100L);
        var msku = msku(100, SEASONAL, 50).setSeasonId(100L);
        var season = season(100L, LocalDate.now().minusDays(2), LocalDate.now().minusDays(1));
        var ssku = ssku(1, "sku", msku, INACTIVE, REAL_SUPPLIER)
            .setStatusStartTime(Instant.now().minus(100, ChronoUnit.DAYS))
            .setModifiedByUser(true);
        var storage = stockStorage(ssku, SOFINO_ID, 0)
            .setFitDisappearTime(Instant.now().minus(59, ChronoUnit.DAYS));

        var reBlock = new REBlock()
            .addMskuStatuses(msku)
            .addCategories(category50)
            .addSskuStatus(ssku)
            .addSeasons(season)
            .addStockStorages(storage)
            .addPurchasePrice(purchasePrice(ssku));
        var result = ruleEngineService.processStatuses(reBlock);

        Assertions.assertThat(result.getChangedShopSkuKeys()).isEmpty();
    }

    @Test
    public void i3FromInactiveAndSeasonalWithZerStocksoDidByRobotDoNothing() {
        var category50 = category(50).setSeasonId(100L);
        var msku = msku(100, SEASONAL, 50).setSeasonId(100L);
        var season = season(100L, LocalDate.now().minusDays(2), LocalDate.now().minusDays(1));
        var ssku = ssku(1, "sku", msku, INACTIVE, REAL_SUPPLIER)
            .setStatusStartTime(Instant.now().minus(100, ChronoUnit.DAYS));
        var storage = stockStorage(ssku, SOFINO_ID, 0)
            .setFitDisappearTime(Instant.now().minus(100, ChronoUnit.DAYS));

        var reBlock = new REBlock()
            .addMskuStatuses(msku)
            .addCategories(category50)
            .addSskuStatus(ssku)
            .addSeasons(season)
            .addStockStorages(storage)
            .addPurchasePrice(purchasePrice(ssku));
        var result = ruleEngineService.processStatuses(reBlock);

        Assertions.assertThat(result.getChangedShopSkuKeys()).isEmpty();
    }

    @Test
    public void i4FromInactiveAndSeasonalToPending() {
        var category50 = category(50).setSeasonId(100L);
        var msku = msku(100, SEASONAL, 50).setSeasonId(100L);
        var season = season(100L, LocalDate.now().minusDays(2), LocalDate.now().plusDays(1));
        var ssku = ssku(1, "sku", msku, INACTIVE, REAL_SUPPLIER)
            .setStatusStartTime(Instant.now().minus(3, ChronoUnit.DAYS));

        var reBlock = new REBlock()
            .addMskuStatuses(msku)
            .addCategories(category50)
            .addSskuStatus(ssku)
            .addSeasons(season)
            .addPurchasePrice(purchasePrice(ssku));
        ruleEngineService.processStatuses(reBlock);

        Assertions.assertThat(ssku.getAvailability()).isEqualTo(PENDING);
        Assertions.assertThat(ssku.getRulesThatChangedObject()).containsExactly(
            "I4. ЕСЛИ (SSKU = Inactive)" +
                " И (MSKU = Seasonal)" +
                " И (текущая дата >= даты начала периода Seasonal)" +
                " И (переход в статус Inactive сделан автоматически системой)" +
                " И (переход в статус был осуществлен вне периода) " +
                "ТО (SSKU = Pending)"
        );
    }

    @Test
    public void i4FromInactiveAndSeasonalWithUserDoNothing() {
        var category50 = category(50).setSeasonId(100L);
        var msku = msku(100, SEASONAL, 50).setSeasonId(100L);
        var season = season(100L, LocalDate.now().minusDays(2), LocalDate.now().plusDays(1));
        var ssku = ssku(1, "sku", msku, INACTIVE, REAL_SUPPLIER)
            .setModifiedByUser(true);

        var reBlock = new REBlock()
            .addMskuStatuses(msku)
            .addCategories(category50)
            .addSskuStatus(ssku)
            .addSeasons(season)
            .addPurchasePrice(purchasePrice(ssku));
        var result = ruleEngineService.processStatuses(reBlock);

        Assertions.assertThat(result.getChangedShopSkuKeys()).isEmpty();
    }

    //---------------------
    // from Delisted
    //---------------------

    @Test
    public void d1FromDelistedWithStocksToInactive() {
        var category50 = category(50);
        var msku = msku(100, ARCHIVE, 50);
        var ssku = ssku(1, "sku", msku, DELISTED, REAL_SUPPLIER);
        var stockStorage = stockStorage(ssku, TOMILINO_ID, 1);

        var reBlock = new REBlock()
            .addMskuStatuses(msku)
            .addCategories(category50)
            .addSskuStatus(ssku)
            .addStockStorages(stockStorage)
            .addPurchasePrice(purchasePrice(ssku));
        ruleEngineService.processStatuses(reBlock);

        Assertions.assertThat(ssku.getAvailability()).isEqualTo(INACTIVE);
        Assertions.assertThat(ssku.getRulesThatChangedObject()).containsExactly(
            "D1. ЕСЛИ (SSKU = Delisted) И (Сток > 0) ТО (SSKU = Inactive)"
        );
    }

    @Test
    public void d1FromDelistedWithZeroStocksDoNothing() {
        var category50 = category(50);
        var msku = msku(100, ARCHIVE, 50);
        var ssku = ssku(1, "sku", msku, DELISTED, REAL_SUPPLIER);
        var stockStorage = stockStorage(ssku, TOMILINO_ID, 0);

        var reBlock = new REBlock()
            .addMskuStatuses(msku)
            .addCategories(category50)
            .addSskuStatus(ssku)
            .addStockStorages(stockStorage)
            .addPurchasePrice(purchasePrice(ssku));
        var result = ruleEngineService.processStatuses(reBlock);

        Assertions.assertThat(result.getChangedShopSkuKeys()).isEmpty();
    }

    //---------------------
    // from Pending
    //---------------------

    @Test
    public void p1FromPendingToActive() {
        var category50 = category(50);
        var msku = msku(100, REGULAR, 50);
        var ssku = ssku(1, "sku", msku, PENDING, REAL_SUPPLIER);
        var stock = stockStorage(ssku, EKATERINBURG_ID, 1);

        var reBlock = new REBlock()
            .addMskuStatuses(msku)
            .addCategories(category50)
            .addSskuStatus(ssku)
            .addStockStorages(stock)
            .addPurchasePrice(purchasePrice(ssku));
        ruleEngineService.processStatuses(reBlock);

        Assertions.assertThat(ssku.getAvailability()).isEqualTo(ACTIVE);
        Assertions.assertThat(ssku.getRulesThatChangedObject()).containsExactly(
            "P1. ЕСЛИ (SSKU = PENDING) и (Сток > 0) ТО (SSKU = Active)"
        );
    }

    @Test
    public void p3FromPendingToInactive() {
        var category50 = category(50);
        var msku = msku(100, REGULAR, 50);
        var ssku = ssku(1, "sku", msku, PENDING, REAL_SUPPLIER)
            .setStatusStartTime(Instant.now().minus(46, ChronoUnit.DAYS));
        var stock = stockStorage(ssku, TOMILINO_ID, 0)
            .setFitDisappearTime(Instant.now().minus(46, ChronoUnit.DAYS));

        var reBlock = new REBlock()
            .addMskuStatuses(msku)
            .addCategories(category50)
            .addSskuStatus(ssku)
            .addStockStorages(stock)
            .addPurchasePrice(purchasePrice(ssku));
        ruleEngineService.processStatuses(reBlock);

        Assertions.assertThat(ssku.getAvailability()).isEqualTo(DELISTED);
        Assertions.assertThat(ssku.getRulesThatChangedObject()).containsExactly(
            "P3. ЕСЛИ (SSKU = Pending) И (MSKU <> Seasonal)" +
                " И (Сток = 0 больше 45 дней от даты перехода в статус Pending) " +
                "ТО (SSKU = Delisted)"
        );
    }

    @Test
    public void p3FromPendingDoNothingIfSomeStocksIsRecentToInactive() {
        var category50 = category(50);
        var msku = msku(100, REGULAR, 50);
        var ssku = ssku(1, "sku", msku, PENDING, REAL_SUPPLIER)
            .setStatusStartTime(Instant.now().minus(46, ChronoUnit.DAYS));
        var stock1 = stockStorage(ssku, TOMILINO_ID, 0)
            .setFitDisappearTime(Instant.now().minus(46, ChronoUnit.DAYS));
        var stock2 = stockStorage(ssku, SOFINO_ID, 0)
            .setFitDisappearTime(Instant.now().minus(15, ChronoUnit.DAYS));

        var reBlock = new REBlock()
            .addMskuStatuses(msku)
            .addCategories(category50)
            .addSskuStatus(ssku)
            .addStockStorages(stock1, stock2)
            .addPurchasePrice(purchasePrice(ssku));
        ruleEngineService.processStatuses(reBlock);

        Assertions.assertThat(ssku.getAvailability()).isEqualTo(PENDING);
        Assertions.assertThat(ssku.getRulesThatChangedObject()).isEmpty();
    }

    @Test
    public void ap3FromPendingAndSeasonalOutsidePeriodToInactive() {
        var category50 = category(50, 100L);
        var msku = msku(100, SEASONAL, 50).setSeasonId(100L);
        var season = season(100L,
            LocalDate.now().plusDays(1), LocalDate.now().plusDays(2));
        var ssku = ssku(1, "sku", msku, PENDING, REAL_SUPPLIER);

        var reBlock = new REBlock()
            .addMskuStatuses(msku)
            .addCategories(category50)
            .addSskuStatus(ssku)
            .addSeasons(season)
            .addPurchasePrice(purchasePrice(ssku));
        ruleEngineService.processStatuses(reBlock);

        Assertions.assertThat(ssku.getAvailability()).isEqualTo(INACTIVE);
        Assertions.assertThat(ssku.getRulesThatChangedObject()).containsExactly(
            "AP3. ЕСЛИ (SSKU = Active или Pending)" +
                " И (MSKU = Seasonal) И (текущая дата > даты окончания периода Seasonal)" +
                " ТО (SSKU = Inactive)"
        );
        // проверяем что время поменялось. Не сравниваем время на equals так как это сравнение будет флапать
        Assertions.assertThat(ssku.getStatusStartTime()).isAfter(Instant.now().minusSeconds(100));
    }

    @Test
    public void ap3FromPendingAndSeasonalOutsidePeriodsToInactive() {
        var category50 = category(50, 100L);
        var msku = msku(100, SEASONAL, 50).setSeasonId(100L);
        var season = season(100L,
            LocalDate.now().plusDays(-2), LocalDate.now().plusDays(-1),
            LocalDate.now().plusDays(1), LocalDate.now().plusDays(1)
        );
        var ssku = ssku(1, "sku", msku, PENDING, REAL_SUPPLIER);

        var reBlock = new REBlock()
            .addMskuStatuses(msku)
            .addCategories(category50)
            .addSskuStatus(ssku)
            .addSeasons(season)
            .addPurchasePrice(purchasePrice(ssku));
        ruleEngineService.processStatuses(reBlock);

        Assertions.assertThat(ssku.getAvailability()).isEqualTo(INACTIVE);
        Assertions.assertThat(ssku.getRulesThatChangedObject()).containsExactly(
            "AP3. ЕСЛИ (SSKU = Active или Pending)" +
                " И (MSKU = Seasonal) И (текущая дата > даты окончания периода Seasonal)" +
                " ТО (SSKU = Inactive)"
        );
        // проверяем что время поменялось. Не сравниваем время на equals так как это сравнение будет флапать
        Assertions.assertThat(ssku.getStatusStartTime()).isAfter(Instant.now().minusSeconds(100));
    }

    @Test
    public void ap3FromPendingAndSeasonalInPeriodNothingChanged() {
        var category50 = category(50, 100L);
        var msku = msku(100, SEASONAL, 50).setSeasonId(100L);
        var season = season(100L,
            LocalDate.now().plusDays(-2), LocalDate.now().plusDays(1)
        );
        var ssku = ssku(1, "sku", msku, PENDING, REAL_SUPPLIER);

        var reBlock = new REBlock()
            .addMskuStatuses(msku)
            .addCategories(category50)
            .addSskuStatus(ssku)
            .addSeasons(season)
            .addPurchasePrice(purchasePrice(ssku));
        var result = ruleEngineService.processStatuses(reBlock);

        Assertions.assertThat(result.getChangedShopSkuKeys()).isEmpty();
    }

    @Test
    public void ap3FromPendingAndSeasonalInOnePeriodNothingChanged() {
        var category50 = category(50, 100L);
        var msku = msku(100, SEASONAL, 50).setSeasonId(100L);
        var season = season(100L,
            LocalDate.now().plusDays(-2), LocalDate.now().plusDays(1),
            LocalDate.now().plusDays(1), LocalDate.now().plusDays(2)
        );
        var ssku = ssku(1, "sku", msku, PENDING, REAL_SUPPLIER);

        var reBlock = new REBlock()
            .addMskuStatuses(msku)
            .addCategories(category50)
            .addSskuStatus(ssku)
            .addSeasons(season)
            .addPurchasePrice(purchasePrice(ssku));
        var result = ruleEngineService.processStatuses(reBlock);

        Assertions.assertThat(result.getChangedShopSkuKeys()).isEmpty();
    }

    @Test
    public void ap5PendingWithoutPurchasePriceToInactiveTmp() {
        var category50 = category(50);
        var msku = msku(100, REGULAR, 50);
        var ssku = ssku(1, "sku", msku, PENDING, REAL_SUPPLIER);

        var reBlock = new REBlock()
            .addMskuStatuses(msku)
            .addCategories(category50)
            .addSskuStatus(ssku);
        ruleEngineService.processStatuses(reBlock);

        Assertions.assertThat(ssku.getAvailability()).isEqualTo(INACTIVE_TMP);
        Assertions.assertThat(ssku.getRulesThatChangedObject()).containsExactly(
            "AP5. ЕСЛИ (SSKU = Active или Pending) И (нет закупочной цены) " +
                "ТО (SSKU = Inactive_tmp) И (Причина = Нет закупочной цены) И (Дата окончания = null)"
        );
        Assertions.assertThat(ssku.isHasNoPurchasePrice()).isTrue();
        Assertions.assertThat(ssku.getStatusFinishTime()).isNull();
    }

    @Test
    public void ap7PendingWithoutContractToInactiveTmp() {
        var category50 = category(50);
        var msku = msku(100, REGULAR, 50);
        var ssku = ssku(1, "sku", msku, PENDING, REAL_SUPPLIER);

        var reBlock = new REBlock()
            .addMskuStatuses(msku)
            .addCategories(category50)
            .addSskuStatus(ssku)
            .addPurchasePrice(purchasePrice(ssku))
            .addInvalidContract(new REInvalidContract(1));
        ruleEngineService.processStatuses(reBlock);

        Assertions.assertThat(ssku.getAvailability()).isEqualTo(INACTIVE_TMP);
        Assertions.assertThat(ssku.getRulesThatChangedObject()).containsExactly(
            "AP7. ЕСЛИ (SSKU = Active или Pending) И (нет действующего договора) " +
                "ТО (SSKU = Inactive_tmp) И (Причина = Нет действующего договора) И (Дата окончания = null)"
        );
        Assertions.assertThat(ssku.isHasNoValidContract()).isTrue();
        Assertions.assertThat(ssku.getStatusFinishTime()).isNull();
    }

    @Test
    public void ap5IgnoreSumSupplier() {
        var category50 = category(50);
        var msku = msku(100, REGULAR, 50);
        var ssku = ssku(465852, "000042.sku", msku, PENDING, REAL_SUPPLIER);
        ssku.setRawSupplierId(1868733);
        ssku.setRawShopSku("sku");

        var reBlock = new REBlock()
            .addMskuStatuses(msku)
            .addCategories(category50)
            .addSskuStatus(ssku);
        ruleEngineService.processStatuses(reBlock);

        Assertions.assertThat(ssku.getAvailability()).isEqualTo(PENDING);
        Assertions.assertThat(ssku.getRulesThatChangedObject()).isEmpty();
    }

    @Test
    public void ap5IgnoreSomeCategories() {
        var category50 = category(50);
        var msku = msku(100, REGULAR, 50);
        var ssku = ssku(465852, "000042.sku", msku, PENDING, REAL_SUPPLIER);
        ssku.setEbtCategories(List.of(198118L));
        ssku.setCategoryId(198118L);

        var reBlock = new REBlock()
            .addMskuStatuses(msku)
            .addCategories(category50)
            .addSskuStatus(ssku);
        ruleEngineService.processStatuses(reBlock);

        Assertions.assertThat(ssku.getAvailability()).isEqualTo(PENDING);
        Assertions.assertThat(ssku.getRulesThatChangedObject()).isEmpty();
    }

    @Test
    public void ap6PendingOrActiveCorefixDeadstockToInactive() {
        var category50 = category(50);
        var msku = msku(100, REGULAR, 50);
        var ssku = ssku(1, "sku", msku, ACTIVE, REAL_SUPPLIER)
            .setStatusStartTime(LocalDate.now().minusDays(31).atStartOfDay(ZoneId.systemDefault()).toInstant());
        var corefix = new REMskuCorefix()
            .setMskuId(100);
        var deadstock = new RESskuDeadstock()
            .setSupplierId(1)
            .setShopSku("sku")
            .setWarehouseId(171)
            .setMoscowWarehouseIds(List.of(145L, 171L, 172L, 304L))
            .setDeadstockSince(LocalDate.now().minusDays(31));
        var saleWithWarehouse = sales(ssku, 171, Instant.now().minus(31, ChronoUnit.DAYS));

        var reBlock = new REBlock()
            .addMskuStatuses(msku)
            .addCategories(category50)
            .addSskuStatus(ssku)
            .addCorefix(corefix)
            .addDeadstock(deadstock)
            .addSales(saleWithWarehouse)
            .addPurchasePrice(purchasePrice(ssku));
        ruleEngineService.processStatuses(reBlock);

        Assertions.assertThat(ssku.getAvailability()).isEqualTo(INACTIVE);
        Assertions.assertThat(ssku.getRulesThatChangedObject()).containsExactly(
            "AP6. (SSKU = Active или Pending) И" +
                " (тип MSKU = Corefix) И" +
                " (deadstock на любом складе в МСК > 30 дней) И" +
                " (нет продаж на этом складе > 30 дней) И" +
                " (последние 30 дней статус ЖЦ не менялся) ТО" +
                " (SSKU = Inactive)"
        );
    }

    @Test
    public void ap6PendingOrActiveCorefixDeadstockWithoutSalesToInactive() {
        var category50 = category(50);
        var msku = msku(100, REGULAR, 50);
        var ssku = ssku(1, "sku", msku, ACTIVE, REAL_SUPPLIER)
            .setStatusStartTime(LocalDate.now().minusDays(31).atStartOfDay(ZoneId.systemDefault()).toInstant());
        var corefix = new REMskuCorefix()
            .setMskuId(100);
        var deadstock = new RESskuDeadstock()
            .setSupplierId(1)
            .setShopSku("sku")
            .setWarehouseId(171)
            .setMoscowWarehouseIds(List.of(145L, 171L, 172L, 304L))
            .setDeadstockSince(LocalDate.now().minusDays(31));

        var reBlock = new REBlock()
            .addMskuStatuses(msku)
            .addCategories(category50)
            .addSskuStatus(ssku)
            .addCorefix(corefix)
            .addDeadstock(deadstock)
            .addPurchasePrice(purchasePrice(ssku));
        ruleEngineService.processStatuses(reBlock);

        Assertions.assertThat(ssku.getAvailability()).isEqualTo(INACTIVE);
        Assertions.assertThat(ssku.getRulesThatChangedObject()).containsExactly(
            "AP6. (SSKU = Active или Pending) И" +
                " (тип MSKU = Corefix) И" +
                " (deadstock на любом складе в МСК > 30 дней) И" +
                " (нет продаж на этом складе > 30 дней) И" +
                " (последние 30 дней статус ЖЦ не менялся) ТО" +
                " (SSKU = Inactive)"
        );
    }

    @Test
    public void ap6PendingOrActiveCorefixDeadstockWithSalesOneAnotherWarehouseToInactive() {
        var category50 = category(50);
        var msku = msku(100, REGULAR, 50);
        var ssku = ssku(1, "sku", msku, ACTIVE, REAL_SUPPLIER)
            .setStatusStartTime(LocalDate.now().minusDays(31).atStartOfDay(ZoneId.systemDefault()).toInstant());
        var corefix = new REMskuCorefix()
            .setMskuId(100);
        var deadstock = new RESskuDeadstock()
            .setSupplierId(1)
            .setShopSku("sku")
            .setWarehouseId(171)
            .setMoscowWarehouseIds(List.of(145L, 171L, 172L, 304L))
            .setDeadstockSince(LocalDate.now().minusDays(31));
        var saleWithWarehouse = sales(ssku, 172, Instant.now().minus(1, ChronoUnit.DAYS));

        var reBlock = new REBlock()
            .addMskuStatuses(msku)
            .addCategories(category50)
            .addSskuStatus(ssku)
            .addCorefix(corefix)
            .addDeadstock(deadstock)
            .addSales(saleWithWarehouse)
            .addPurchasePrice(purchasePrice(ssku));
        ruleEngineService.processStatuses(reBlock);

        Assertions.assertThat(ssku.getAvailability()).isEqualTo(INACTIVE);
        Assertions.assertThat(ssku.getRulesThatChangedObject()).containsExactly(
            "AP6. (SSKU = Active или Pending) И" +
                " (тип MSKU = Corefix) И" +
                " (deadstock на любом складе в МСК > 30 дней) И" +
                " (нет продаж на этом складе > 30 дней) И" +
                " (последние 30 дней статус ЖЦ не менялся) ТО" +
                " (SSKU = Inactive)"
        );
    }

    @Test
    public void ap6PendingOrActiveCorefixDeadstockWithSeveralSalesWontChange() {
        var category50 = category(50);
        var msku = msku(100, REGULAR, 50);
        var ssku = ssku(1, "sku", msku, ACTIVE, REAL_SUPPLIER)
            .setStatusStartTime(LocalDate.now().minusDays(31).atStartOfDay(ZoneId.systemDefault()).toInstant());
        var corefix = new REMskuCorefix()
            .setMskuId(100);
        var deadstock = new RESskuDeadstock()
            .setSupplierId(1)
            .setShopSku("sku")
            .setWarehouseId(171)
            .setMoscowWarehouseIds(List.of(145L, 171L, 172L, 304L))
            .setDeadstockSince(LocalDate.now().minusDays(31));
        // сначала создаем продажу на складке, там где товар был дедсток
        var saleWithWarehouse1 = sales(ssku, 171, Instant.now().minus(2, ChronoUnit.DAYS));
        // потом на другом складе, где нет продаж
        var saleWithWarehouse2 = sales(ssku, 171, Instant.now().minus(1, ChronoUnit.DAYS));

        var reBlock = new REBlock()
            .addMskuStatuses(msku)
            .addCategories(category50)
            .addSskuStatus(ssku)
            .addCorefix(corefix)
            .addDeadstock(deadstock)
            .addSales(saleWithWarehouse1, saleWithWarehouse2)
            .addPurchasePrice(purchasePrice(ssku));
        var ruleEngineResult = ruleEngineService.processStatuses(reBlock);

        Assertions.assertThat(ssku.getAvailability()).isEqualTo(ACTIVE);
        // не будет изменений, так как есть продажи
        Assertions.assertThat(ruleEngineResult.getChangedShopSkuKeys()).isEmpty();
    }

    @Test
    public void ap6PendingOrActiveWithSeveralDeadstockToInactive() {
        var category50 = category(50);
        var msku = msku(100, REGULAR, 50);
        var ssku = ssku(1, "sku", msku, ACTIVE, REAL_SUPPLIER)
            .setStatusStartTime(LocalDate.now().minusDays(31).atStartOfDay(ZoneId.systemDefault()).toInstant());
        var corefix = new REMskuCorefix()
            .setMskuId(100);
        var deadstock1 = new RESskuDeadstock()
            .setSupplierId(1)
            .setShopSku("sku")
            .setWarehouseId(171)
            .setMoscowWarehouseIds(List.of(145L, 171L, 172L, 304L))
            .setDeadstockSince(LocalDate.now().minusDays(31));
        var deadstock2 = new RESskuDeadstock()
            .setSupplierId(1)
            .setShopSku("sku")
            .setWarehouseId(100500)
            .setMoscowWarehouseIds(List.of(145L, 171L, 172L, 304L))
            .setDeadstockSince(LocalDate.now().minusDays(31));
        var saleWithWarehouse = sales(ssku, 171, Instant.now().minus(31, ChronoUnit.DAYS));

        var reBlock = new REBlock()
            .addMskuStatuses(msku)
            .addCategories(category50)
            .addSskuStatus(ssku)
            .addCorefix(corefix)
            .addDeadstock(deadstock1, deadstock2)
            .addSales(saleWithWarehouse)
            .addPurchasePrice(purchasePrice(ssku));
        ruleEngineService.processStatuses(reBlock);

        Assertions.assertThat(ssku.getAvailability()).isEqualTo(INACTIVE);
        Assertions.assertThat(ssku.getRulesThatChangedObject()).containsExactly(
            "AP6. (SSKU = Active или Pending) И" +
                " (тип MSKU = Corefix) И" +
                " (deadstock на любом складе в МСК > 30 дней) И" +
                " (нет продаж на этом складе > 30 дней) И" +
                " (последние 30 дней статус ЖЦ не менялся) ТО" +
                " (SSKU = Inactive)"
        );
    }
}
