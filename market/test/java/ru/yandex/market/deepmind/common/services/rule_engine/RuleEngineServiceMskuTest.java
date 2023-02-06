package ru.yandex.market.deepmind.common.services.rule_engine;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.deepmind.common.services.rule_engine.pojo.REBlock;
import ru.yandex.market.deepmind.common.services.rule_engine.pojo.RECategory;
import ru.yandex.market.deepmind.common.services.rule_engine.pojo.REMskuStatus;
import ru.yandex.market.deepmind.common.services.rule_engine.pojo.RESeason;
import ru.yandex.market.deepmind.common.services.rule_engine.pojo.REStockStorage;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.deepmind.common.db.jooq.generated.mbo_category.enums.OfferAvailability.ACTIVE;
import static ru.yandex.market.deepmind.common.db.jooq.generated.mbo_category.enums.OfferAvailability.DELISTED;
import static ru.yandex.market.deepmind.common.db.jooq.generated.mbo_category.enums.OfferAvailability.INACTIVE;
import static ru.yandex.market.deepmind.common.db.jooq.generated.mbo_category.enums.OfferAvailability.PENDING;
import static ru.yandex.market.deepmind.common.db.jooq.generated.mbo_category.enums.SupplierType.FIRST_PARTY;
import static ru.yandex.market.deepmind.common.db.jooq.generated.mbo_category.enums.SupplierType.REAL_SUPPLIER;
import static ru.yandex.market.deepmind.common.db.jooq.generated.mbo_category.enums.SupplierType.THIRD_PARTY;
import static ru.yandex.market.deepmind.common.db.jooq.generated.msku.enums.MskuStatusValue.ARCHIVE;
import static ru.yandex.market.deepmind.common.db.jooq.generated.msku.enums.MskuStatusValue.EMPTY;
import static ru.yandex.market.deepmind.common.db.jooq.generated.msku.enums.MskuStatusValue.END_OF_LIFE;
import static ru.yandex.market.deepmind.common.db.jooq.generated.msku.enums.MskuStatusValue.IN_OUT;
import static ru.yandex.market.deepmind.common.db.jooq.generated.msku.enums.MskuStatusValue.NPD;
import static ru.yandex.market.deepmind.common.db.jooq.generated.msku.enums.MskuStatusValue.PRE_NPD;
import static ru.yandex.market.deepmind.common.db.jooq.generated.msku.enums.MskuStatusValue.REGULAR;
import static ru.yandex.market.deepmind.common.db.jooq.generated.msku.enums.MskuStatusValue.SEASONAL;

/**
 * Test of {@link RuleEngineService} of file: {@link rule_engine_v6/MskuRulesV6.drl}.
 */
public class RuleEngineServiceMskuTest extends BaseRuleEngineServiceTest {

    private RuleEngineService ruleEngineService;

    @Before
    public void setUp() throws Exception {
        ruleEngineService = RuleEngineService.createV6();
    }

    @Test
    public void noStatusIfNoOffer() {
        var category50 = new RECategory(50);
        var msku = msku(100, EMPTY, 50);

        ruleEngineService.processStatuses(new REBlock().addCategories(category50).addMskuStatuses(msku));

        Assertions.assertThat(msku.getStatus()).isEqualTo(EMPTY);
    }

    @Test
    public void m1m2ChangeFromEmptyToNpdSeasonal() {
        // arrange
        var seasonalMsku = new REMskuStatus(100).setCategoryId(50L).setStatus(EMPTY);
        var category50 = new RECategory(50).setSeasonId(111L);
        var ssku100 = ssku(1, "sku", 100, ACTIVE);

        var npdMsku = new REMskuStatus(200).setCategoryId(60L).setStatus(EMPTY);
        var category60 = new RECategory(60);
        var ssku200 = ssku(2, "sku", 200, INACTIVE);

        var stock100 = new REStockStorage(ssku100, 1).setFit(1);
        var stock200 = new REStockStorage(ssku200, 1).setFit(1);

        // act
        var reBlock = new REBlock()
            .addMskuStatuses(seasonalMsku, npdMsku)
            .addCategories(category50, category60)
            .addSskuStatus(ssku100, ssku200)
            .addStockStorages(stock100, stock200)
            .addSeasons(new RESeason(111L))
            .addPurchasePrice(purchasePrice(ssku100, ssku200));
        ruleEngineService.processStatuses(reBlock);

        // assert
        Assertions.assertThat(seasonalMsku.getStatus()).isEqualTo(SEASONAL);
        Assertions.assertThat(seasonalMsku.getSeasonId()).isEqualTo(111L);
        Assertions.assertThat(npdMsku.getStatus()).isEqualTo(NPD);
    }

    @Test
    public void testChangeFromEmptyToPreNpd() {
        // arrange
        var msku = new REMskuStatus(200).setCategoryId(60L).setStatus(EMPTY);
        var category60 = new RECategory(60);
        var ssku200 = ssku(2, "sku", 200, INACTIVE);

        // act
        var reBlock = new REBlock()
            .addMskuStatuses(msku)
            .addCategories(category60)
            .addSskuStatus(ssku200);
        ruleEngineService.processStatuses(reBlock);

        // assert
        Assertions.assertThat(msku.getStatus()).isEqualTo(PRE_NPD);
        Assertions.assertThat(msku.getRulesThatChangedObject()).containsExactly(
            "M1. ЕСЛИ (MSKU = EMPTY) И (к MSKU привязали SSKU из категории без периода Seasonal) ТО (MSKU = PRE_NPD)"
        );

        msku.clearRules();

        // add stocks
        reBlock = new REBlock()
            .addMskuStatuses(msku)
            .addCategories(category60)
            .addSskuStatus(ssku200)
            .addStockStorages(stockStorage(ssku200, 1, 1));
        ruleEngineService.processStatuses(reBlock);

        // assert
        Assertions.assertThat(msku.getStatus()).isEqualTo(NPD);
        Assertions.assertThat(msku.getRulesThatChangedObject()).containsExactly(
            "M3. ЕСЛИ (MSKU = PRE_NPD) И (есть стоки) ТО (MSKU = NPD)",
            "M4. ЕСЛИ (MSKU = NPD) И (дата окончания NPD = null) TO (дата окончания NPD = дата начала NPD + 15 дней)"
        );
    }

    @Test
    public void m3setNpdFinishDate() {
        // arrange
        var category50 = new RECategory(50);
        var msku = msku(200, NPD, 50)
            .setNpdStartDate(LocalDate.parse("2007-12-03"));
        var ssku = ssku(1, "sku", msku, ACTIVE);

        // act
        var reBlock = new REBlock()
            .addCategories(category50)
            .addMskuStatuses(msku)
            .addSskuStatus(ssku)
            .addPurchasePrice(purchasePrice(ssku));
        ruleEngineService.processStatuses(reBlock);

        // assert
        Assertions.assertThat(msku.getNpdFinishDate()).isEqualTo(LocalDate.parse("2007-12-03").plusDays(15));
    }

    @Test
    public void m3DontChangeNpdFinishDateIfItAlreadySet() {
        // arrange
        var category50 = new RECategory(50);
        var msku = msku(200, NPD, 50)
            .setNpdStartDate(LocalDate.parse("2021-01-01")).setNpdFinishDate(LocalDate.parse("2021-04-01"));
        var ssku1 = ssku(1, "sku", 200, ACTIVE);
        var ssku2 = ssku(2, "sku", 200, ACTIVE);

        var stock1 = new REStockStorage(ssku1, 123).setFit(1)
            .setFitAppearTime(Instant.parse("2007-12-03T00:00:00.00Z"));
        var stock2 = new REStockStorage(ssku1, 124).setFit(0)
            .setFitAppearTime(Instant.parse("2007-12-10T00:00:00.00Z"));
        var stock3 = new REStockStorage(ssku1, 124).setFit(0)
            .setFitDisappearTime(Instant.parse("2020-01-01T00:00:00.00Z"));
        var stock4 = new REStockStorage(ssku2, 123).setFit(1)
            .setFitAppearTime(Instant.parse("2007-12-15T00:00:00.00Z"));
        var stock5 = new REStockStorage(ssku2, 124).setFit(5)
            .setFitAppearTime(Instant.parse("2007-12-20T00:00:00.00Z"));

        // act
        var reBlock = new REBlock()
            .addCategories(category50)
            .addMskuStatuses(msku)
            .addSskuStatus(ssku1, ssku2)
            .addStockStorages(stock1, stock2, stock3, stock4, stock5)
            .addPurchasePrice(purchasePrice(ssku1, ssku2));
        ruleEngineService.processStatuses(reBlock);

        // assert
        Assertions.assertThat(msku.getNpdFinishDate()).isEqualTo(LocalDate.parse("2021-04-01"));
    }

    @Test
    public void m3setNpdFinishDateWithSeveralMsku() {
        // arrange
        var category50 = new RECategory(50);
        var msku1 = msku(100, NPD, 50)
            .setNpdStartDate(LocalDate.parse("2007-12-03"));
        var msku2 = msku(200, NPD, 50)
            .setNpdStartDate(LocalDate.parse("2020-01-01"))
            .setNpdFinishDate(LocalDate.parse("2029-01-01"));

        var ssku1 = ssku(1, "sku", msku1, ACTIVE);
        var ssku2 = ssku(2, "sku", msku2, ACTIVE);

        // act
        var reBlock = new REBlock()
            .addCategories(category50)
            .addMskuStatuses(msku1, msku2)
            .addSskuStatus(ssku1, ssku2)
            .addPurchasePrice(purchasePrice(ssku1, ssku2));
        ruleEngineService.processStatuses(reBlock);

        // assert
        Assertions.assertThat(msku1.getNpdFinishDate()).isEqualTo(LocalDate.parse("2007-12-03").plusDays(15));
        Assertions.assertThat(msku2.getNpdFinishDate()).isEqualTo(LocalDate.parse("2029-01-01"));
    }

    @Test
    public void m3changeFromNpdToRegular() {
        // Для одного msku настраиваем у категории сезон, для второго нет
        // Даже при наличии сезона статус должен стать регулярным
        var categoryWithSeason = new RECategory(50).setSeasonId(111L);
        var categoryWithoutSeason = new RECategory(60);

        var msku1 = new REMskuStatus(100).setCategoryId(categoryWithSeason.getCategoryId())
            .setStatus(NPD)
            .setNpdStartDate(LocalDate.now().minusDays(2))
            .setNpdFinishDate(LocalDate.now().minusDays(1));
        var msku2 = new REMskuStatus(200).setCategoryId(categoryWithoutSeason.getCategoryId())
            .setStatus(NPD)
            .setNpdStartDate(LocalDate.now().minusDays(21))
            .setNpdFinishDate(LocalDate.now().minusDays(20));
        var npd1 = new REMskuStatus(300).setCategoryId(categoryWithSeason.getCategoryId())
            .setStatus(NPD)
            .setNpdStartDate(LocalDate.now().minusDays(1))
            .setNpdFinishDate(LocalDate.now().plusDays(10));
        var npd2 = new REMskuStatus(400).setCategoryId(categoryWithoutSeason.getCategoryId())
            .setStatus(NPD)
            .setNpdStartDate(LocalDate.now().minusDays(1))
            .setNpdFinishDate(LocalDate.now().plusDays(10));

        var ssku1 = ssku(1, "shop-sku-1", msku1, ACTIVE);
        var ssku2 = ssku(1, "shop-sku-2", msku2, ACTIVE);
        var ssku3 = ssku(1, "shop-sku-3", npd1, ACTIVE);
        var ssku4 = ssku(1, "shop-sku-4", npd2, ACTIVE);

        // act
        var reBlock = new REBlock()
            .addMskuStatuses(msku1, msku2, npd1, npd2)
            .addSskuStatus(ssku1, ssku2, ssku3, ssku4)
            .addCategories(categoryWithSeason, categoryWithoutSeason)
            .addSeasons(new RESeason(111L))
            .addPurchasePrice(purchasePrice(ssku1, ssku2, ssku3, ssku4));
        ruleEngineService.processStatuses(reBlock);

        // assert
        Assertions.assertThat(msku1.getStatus()).isEqualTo(REGULAR);
        Assertions.assertThat(msku1.getSeasonId()).isNull();
        Assertions.assertThat(msku2.getStatus()).isEqualTo(REGULAR);
        Assertions.assertThat(msku2.getSeasonId()).isNull();
        Assertions.assertThat(npd1.getStatus()).isEqualTo(NPD);
        Assertions.assertThat(npd2.getStatus()).isEqualTo(NPD);
    }

    @Test
    public void m4changeFromRegularOrInOutToEndOfLife() {
        // arrange
        var category50 = new RECategory(50);
        // msku1 и msku2 должны поменять свои статусы, так как нет ssku в статусе ACTIVE
        // msku3 и msku4 оставить прежние, так как есть ssku в статусе ACTIVE
        var msku1 = msku(100, REGULAR, 50);
        var msku2 = msku(200, IN_OUT, 50);
        var msku3 = msku(300, REGULAR, 50);
        var msku4 = msku(400, IN_OUT, 50);
        var ssku11 = ssku(1, "sku1", msku1.getId(), DELISTED, THIRD_PARTY);
        var ssku12 = ssku(1, "sku2", msku1.getId(), INACTIVE, THIRD_PARTY);
        var ssku21 = ssku(465852, "000042.sku1", msku2.getId(), INACTIVE, REAL_SUPPLIER);
        var ssku31 = ssku(465852, "sku1", msku3.getId(), ACTIVE, FIRST_PARTY);
        var ssku32 = ssku(465852, "000555.sku2", msku3.getId(), INACTIVE, REAL_SUPPLIER);
        var ssku41 = ssku(4, "sku1", msku4.getId(), ACTIVE, THIRD_PARTY);

        var stock12 = new REStockStorage(ssku12, 1).setFit(1);
        var stock21 = new REStockStorage(ssku21, 1).setFit(1);
        var stock31 = new REStockStorage(ssku31, 1).setFit(1);
        var stock32 = new REStockStorage(ssku32, 1).setFit(1);
        var stock41 = new REStockStorage(ssku41, 1).setFit(1);

        // act
        var reBlock = new REBlock()
            .addCategories(category50)
            .addMskuStatuses(msku1, msku2)
            .addSskuStatus(ssku11, ssku12, ssku21, ssku31, ssku32, ssku41)
            .addStockStorages(stock12, stock21, stock31, stock32, stock41)
            .addPurchasePrice(purchasePrice(ssku11, ssku12, ssku21, ssku31, ssku32, ssku41));
        ruleEngineService.processStatuses(reBlock);

        // assert
        Assertions.assertThat(msku1.getStatus()).isEqualTo(END_OF_LIFE);
        Assertions.assertThat(msku2.getStatus()).isEqualTo(END_OF_LIFE);
        Assertions.assertThat(msku3.getStatus()).isEqualTo(REGULAR);
        Assertions.assertThat(msku4.getStatus()).isEqualTo(IN_OUT);
    }

    @Test
    public void m5changeFromEndOfLifeToArchive() {
        var status1 = msku(101, REGULAR, 1);
        var status2 = msku(102, END_OF_LIFE, 1);
        var status3 = msku(103, END_OF_LIFE, 1);
        var status4 = msku(104, END_OF_LIFE, 1);
        var status5 = msku(105, END_OF_LIFE, 1);
        var status6 = msku(106, END_OF_LIFE, 1);

        var ssku1 = ssku(1, "shop-sku-1", 101, ACTIVE);
        var ssku2 = ssku(1, "shop-sku-2", 102, DELISTED);
        var ssku3 = ssku(1, "shop-sku-3", 103, INACTIVE);
        var ssku41 = ssku(1, "shop-sku-41", 104, DELISTED);
        var ssku42 = ssku(1, "shop-sku-42", 104, DELISTED);
        var ssku51 = ssku(1, "shop-sku-51", 105, ACTIVE);
        var ssku52 = ssku(1, "shop-sku-52", 105, DELISTED);

        var stock1 = new REStockStorage(ssku1, 1).setFit(1);
        var stock3 = new REStockStorage(ssku3, 1).setFit(1);
        var stock51 = new REStockStorage(ssku51, 1).setFit(1);

        var block = new REBlock()
            .addCategories(category(1))
            .addMskuStatuses(status1, status2, status3, status4, status5, status6)
            .addSskuStatus(ssku1, ssku2, ssku3, ssku41, ssku42, ssku51, ssku52)
            .addStockStorages(stock1, stock3, stock51)
            .addPurchasePrice(purchasePrice(ssku1, ssku2, ssku3, ssku41, ssku42, ssku51, ssku52));
        ruleEngineService.processStatuses(block);

        assertThat(status1.getStatus()).isEqualTo(REGULAR);
        assertThat(status2.getStatus()).isEqualTo(ARCHIVE);
        assertThat(status3.getStatus()).isEqualTo(END_OF_LIFE);
        assertThat(status4.getStatus()).isEqualTo(ARCHIVE);
        assertThat(status5.getStatus()).isEqualTo(REGULAR);
        assertThat(status6.getStatus()).isEqualTo(EMPTY);
    }

    @Test
    public void m5dontArchiveIfStockAtLeastOne() {
        var category50 = new RECategory(50);
        var msku = msku(101, END_OF_LIFE, 50);
        var ssku11 = ssku(1, "shop-sku-11", 101, DELISTED);
        var ssku12 = ssku(1, "shop-sku-12", 101, DELISTED);

        var stock11 = new REStockStorage(ssku11, 1)
            .setFitDisappearTime(Instant.now().minus(70, ChronoUnit.DAYS));
        var stock12 = new REStockStorage(ssku12, 2)
            .setFit(1);

        var block = new REBlock()
            .addCategories(category50)
            .addMskuStatuses(msku)
            .addSskuStatus(ssku11, ssku12)
            .addStockStorages(stock11, stock12);
        ruleEngineService.processStatuses(block);

        assertThat(msku.getStatus()).isEqualTo(END_OF_LIFE);
    }

    @Test
    public void m6changeFromSeasonalToRegular() {
        // arrange
        var seasonalMsku = msku(100, SEASONAL, 50L)
            .setSeasonId(111L);
        var category50 = new RECategory(50).setSeasonId(111L);

        var regularMsku = msku(200, SEASONAL, 60L)
            .setSeasonId(100L);
        var category60 = new RECategory(60);

        var ssku1 = ssku(1, "shop-sku-1", 100, ACTIVE);
        var ssku2 = ssku(1, "shop-sku-2", 200, ACTIVE);

        // act
        var reBlock = new REBlock()
            .addMskuStatuses(seasonalMsku, regularMsku)
            .addSskuStatus(ssku1, ssku2)
            .addCategories(category50, category60)
            .addSeasons(new RESeason(111L), new RESeason(100L))
            .addPurchasePrice(purchasePrice(ssku1, ssku2));
        ruleEngineService.processStatuses(reBlock);

        // assert
        Assertions.assertThat(seasonalMsku.getStatus()).isEqualTo(SEASONAL);
        Assertions.assertThat(seasonalMsku.getSeasonId()).isEqualTo(111L);
        Assertions.assertThat(regularMsku.getStatus()).isEqualTo(REGULAR);
        Assertions.assertThat(regularMsku.getSeasonId()).isNull();
    }

    @Test
    public void m7changeFromArchiveToEndOfLife() {
        var status1 = msku(101, ARCHIVE, 1);
        var status2 = msku(102, ARCHIVE, 1);
        var status3 = msku(103, ARCHIVE, 1);
        var status4 = msku(104, ARCHIVE, 1);
        var status5 = msku(105, ARCHIVE, 1);
        var status6 = msku(106, ARCHIVE, 1);
        var status7 = msku(107, ARCHIVE, 1);

        var ssku1 = ssku(1, "shop-sku-1", 101, ACTIVE);
        var ssku2 = ssku(1, "shop-sku-2", 102, DELISTED);
        var ssku3 = ssku(1, "shop-sku-3", 103, INACTIVE);
        var ssku41 = ssku(1, "shop-sku-41", 104, DELISTED);
        var ssku42 = ssku(1, "shop-sku-42", 104, DELISTED);
        var ssku51 = ssku(1, "shop-sku-51", 105, ACTIVE);
        var ssku52 = ssku(1, "shop-sku-52", 105, DELISTED);
        var ssku61 = ssku(1, "shop-sku-61", 106, ACTIVE);
        var ssku62 = ssku(1, "shop-sku-62", 106, INACTIVE);

        var stock1 = new REStockStorage(ssku1, 1).setFit(1);
        var stock3 = new REStockStorage(ssku3, 1).setFit(1);
        var stock51 = new REStockStorage(ssku51, 1).setFit(1);
        var stock61 = new REStockStorage(ssku61, 1).setFit(1);

        var block = new REBlock()
            .addCategories(category(1))
            .addMskuStatuses(status1, status2, status3, status4, status5, status6, status7)
            .addStockStorages(stock1, stock3, stock51, stock61)
            .addSskuStatus(ssku1, ssku2, ssku3, ssku41, ssku42, ssku51, ssku52, ssku61, ssku62)
            .addPurchasePrice(purchasePrice(ssku1, ssku2, ssku3, ssku41, ssku42, ssku51, ssku52, ssku61, ssku62));
        ruleEngineService.processStatuses(block);

        assertThat(status1.getStatus()).isEqualTo(REGULAR);
        assertThat(status2.getStatus()).isEqualTo(ARCHIVE);
        assertThat(status3.getStatus()).isEqualTo(END_OF_LIFE);
        assertThat(status4.getStatus()).isEqualTo(ARCHIVE);
        assertThat(status5.getStatus()).isEqualTo(REGULAR);
        assertThat(status6.getStatus()).isEqualTo(REGULAR);
        assertThat(status7.getStatus()).isEqualTo(EMPTY);
    }

    /**
     * Проверяем, что если msku будет в статусе NPD, но без ssku,
     * то статус поменяется на null.
     * <p>
     * Это гипотетическая ситуация, такого не должно на практике случиться.
     * Но тест все равно существует, чтобы проверить отсутствие циклов в правилах.
     */
    @Test
    public void m8ResetMskuStatusIfNoSskuFound() {
        var category50 = new RECategory(50);
        var msku = msku(200, NPD, 50)
            .setModificationTs(Instant.parse("2007-12-03T10:15:30.00Z"))
            .setNpdStartDate(LocalDate.parse("2007-12-03"))
            .setNpdFinishDate(LocalDate.parse("2008-02-02"));

        var reBlock = new REBlock()
            .addCategories(category50)
            .addMskuStatuses(msku);
        ruleEngineService.processStatuses(reBlock);

        Assertions.assertThat(msku.getStatus()).isEqualTo(EMPTY);
        // Проверяем, что все аттрибьюты будут в null
        Assertions.assertThat(msku)
            .usingRecursiveComparison()
            // не важно, как мы пришли к этому статусу, главное, что все поля null
            .ignoringFields("comment", "rulesThatChangedObject")
            .isEqualTo(msku(200, EMPTY, 50).setModificationTs(Instant.parse("2007-12-03T10:15:30.00Z")));
    }

    @Test
    public void m8DontChangeNewAndEmptyStatus() {
        var category50 = new RECategory(50);
        var msku = msku(100, EMPTY, 50)
            .setModificationTs(Instant.MIN);

        var reBlock = new REBlock()
            .addCategories(category50)
            .addMskuStatuses(msku);
        var result = ruleEngineService.processStatuses(reBlock);

        Assertions.assertThat(result.getChangedMskuIds()).isEmpty();
        Assertions.assertThat(msku.getStatus()).isEqualTo(EMPTY);
    }

    @Test
    public void m8ResetMskuStatusIfNoSskuFoundSeveralMsku() {
        var category50 = new RECategory(50);
        var msku1 = msku(200, NPD, 50)
            .setModificationTs(Instant.parse("2007-12-03T10:15:30.00Z"))
            .setNpdStartDate(LocalDate.parse("2007-12-03"))
            .setNpdFinishDate(LocalDate.parse("2008-02-02"));
        var msku2 = msku(300, REGULAR, 50)
            .setModificationTs(Instant.parse("2007-12-03T10:15:30.00Z"));
        var ssku = ssku(1, "a", msku2, ACTIVE);

        var reBlock = new REBlock()
            .addCategories(category50)
            .addMskuStatuses(msku1, msku2)
            .addSskuStatus(ssku)
            .addPurchasePrice(purchasePrice(ssku));
        var result = ruleEngineService.processStatuses(reBlock);

        Assertions.assertThat(msku1.getStatus()).isEqualTo(EMPTY);
        Assertions.assertThat(msku2.getStatus()).isEqualTo(REGULAR);
        Assertions.assertThat(result.getChangedMskuIds()).containsExactly(200L);
    }

    @Test
    public void m9FromEndOfLifeToRegular() {
        var category50 = category(50);
        var msku = msku(100, END_OF_LIFE, 50);
        var ssku = ssku(1, "sku", msku, ACTIVE, REAL_SUPPLIER);
        var sales = sales(ssku, 300, Instant.now());

        var reBlock = new REBlock()
            .addMskuStatuses(msku)
            .addCategories(category50)
            .addSskuStatus(ssku)
            .addSales(sales)
            .addPurchasePrice(purchasePrice(ssku));
        ruleEngineService.processStatuses(reBlock);

        Assertions.assertThat(msku.getStatus()).isEqualTo(REGULAR);
        Assertions.assertThat(msku.getRulesThatChangedObject()).containsExactly(
            "M10. ЕСЛИ (MSKU = End_of_life) И (SSKU = Active или Pending) " +
                "И (MSKU из категории без периода Seasonal) TO (MSKU = Regular)"
        );
    }

    @Test
    public void m10FromEndOfLifeToSeasonal() {
        var category50 = category(50).setSeasonId(100L);
        var season = season(100, LocalDate.now(), LocalDate.now());
        var msku = msku(100, END_OF_LIFE, 50);
        var ssku = ssku(1, "sku", msku, PENDING, REAL_SUPPLIER);
        var sales = sales(ssku, 300, Instant.now());

        var reBlock = new REBlock()
            .addMskuStatuses(msku)
            .addCategories(category50)
            .addSskuStatus(ssku)
            .addSales(sales)
            .addSeasons(season)
            .addPurchasePrice(purchasePrice(ssku));
        ruleEngineService.processStatuses(reBlock);

        Assertions.assertThat(msku.getStatus()).isEqualTo(SEASONAL);
        Assertions.assertThat(msku.getSeasonId()).isEqualTo(100L);
        Assertions.assertThat(msku.getRulesThatChangedObject()).containsExactly(
            "M11. ЕСЛИ (MSKU = End_of_life) И (SSKU = Active или Pending) " +
                "И (MSKU из категории с периодом Seasonal) TO (MSKU = Seasonal)"
        );
    }

    @Test
    public void m9FromArchiveToReguar() {
        var category50 = category(50);
        var msku = msku(100, ARCHIVE, 50);
        var ssku = ssku(1, "sku", msku, ACTIVE, REAL_SUPPLIER);
        var sales = sales(ssku, 300, Instant.now());

        var reBlock = new REBlock()
            .addMskuStatuses(msku)
            .addCategories(category50)
            .addSskuStatus(ssku)
            .addSales(sales)
            .addPurchasePrice(purchasePrice(ssku));
        ruleEngineService.processStatuses(reBlock);

        Assertions.assertThat(msku.getStatus()).isEqualTo(REGULAR);
        Assertions.assertThat(msku.getRulesThatChangedObject()).containsExactly(
            "M8. ЕСЛИ (MSKU = Archive) И (SSKU <> Delisted) ТО (MSKU = End_of_life)",
            "M10. ЕСЛИ (MSKU = End_of_life) И (SSKU = Active или Pending) " +
                "И (MSKU из категории без периода Seasonal) TO (MSKU = Regular)"
        );
    }

    @Test
    public void mskuFromInoutToEndOfLifeWith1pAnd3pSsku() {
        var category50 = category(50);
        var msku = msku(100, IN_OUT, 50)
            .setInoutStartDate(LocalDate.now().minusDays(2))
            .setInoutFinishDate(LocalDate.now().minusDays(1));
        var ssku1 = ssku(1, "sku", msku, ACTIVE, THIRD_PARTY);
        var ssku2 = ssku(2, "sku", msku, ACTIVE, REAL_SUPPLIER);

        var reBlock = new REBlock()
            .addMskuStatuses(msku)
            .addCategories(category50)
            .addSskuStatus(ssku1, ssku2)
            .addPurchasePrice(purchasePrice(ssku1), purchasePrice(ssku2));
        ruleEngineService.processStatuses(reBlock);

        Assertions.assertThat(ssku1.getAvailability()).isEqualTo(INACTIVE);
        Assertions.assertThat(ssku2.getAvailability()).isEqualTo(INACTIVE);
        Assertions.assertThat(msku.getStatus()).isEqualTo(END_OF_LIFE);

        Assertions.assertThat(ssku1.getRulesThatChangedObject()).containsExactly(
            "TP3. ЕСЛИ (SSKU = Active или Pending) И (MSKU = In/Out)" +
                " И (текущая дата > даты окончания периода In/Out) " +
                "ТО (SSKU = Inactive)"
        );
        Assertions.assertThat(ssku2.getRulesThatChangedObject()).containsExactly(
            "AP4. ЕСЛИ (SSKU = Active или Pending) И (MSKU = In/Out)" +
                " И (текущая дата > даты окончания периода In/Out) " +
                "ТО (SSKU = Inactive)"
        );
        Assertions.assertThat(msku.getRulesThatChangedObject()).containsExactly(
            "M5. ЕСЛИ (MSKU = Regular или In/Out) И (все SSKU <> Active или Pending) ТО (MSKU = End_of_life)"
        );
    }
}
