package ru.yandex.market.deepmind.common.services.rule_engine;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.deepmind.common.db.jooq.generated.msku.enums.MskuStatusValue;
import ru.yandex.market.deepmind.common.services.rule_engine.pojo.REBlock;
import ru.yandex.market.deepmind.common.services.rule_engine.pojo.RECategory;
import ru.yandex.market.deepmind.common.services.rule_engine.pojo.REInvalidContract;
import ru.yandex.market.deepmind.common.services.rule_engine.pojo.REMskuStatus;

import static ru.yandex.market.deepmind.common.db.jooq.generated.mbo_category.enums.OfferAvailability.ACTIVE;
import static ru.yandex.market.deepmind.common.db.jooq.generated.mbo_category.enums.OfferAvailability.DELISTED;
import static ru.yandex.market.deepmind.common.db.jooq.generated.mbo_category.enums.OfferAvailability.INACTIVE;
import static ru.yandex.market.deepmind.common.db.jooq.generated.mbo_category.enums.OfferAvailability.INACTIVE_TMP;
import static ru.yandex.market.deepmind.common.db.jooq.generated.mbo_category.enums.OfferAvailability.PENDING;
import static ru.yandex.market.deepmind.common.db.jooq.generated.mbo_category.enums.SupplierType.REAL_SUPPLIER;
import static ru.yandex.market.deepmind.common.db.jooq.generated.msku.enums.MskuStatusValue.ARCHIVE;
import static ru.yandex.market.deepmind.common.db.jooq.generated.msku.enums.MskuStatusValue.END_OF_LIFE;
import static ru.yandex.market.deepmind.common.db.jooq.generated.msku.enums.MskuStatusValue.IN_OUT;
import static ru.yandex.market.deepmind.common.db.jooq.generated.msku.enums.MskuStatusValue.NPD;
import static ru.yandex.market.deepmind.common.db.jooq.generated.msku.enums.MskuStatusValue.REGULAR;
import static ru.yandex.market.deepmind.common.db.jooq.generated.msku.enums.MskuStatusValue.SEASONAL;
import static ru.yandex.market.deepmind.common.repository.logicstics.warehouse.DeepmindWarehouseRepository.CROSSDOCK_SOFINO_ID;
import static ru.yandex.market.deepmind.common.repository.logicstics.warehouse.DeepmindWarehouseRepository.EKATERINBURG_ID;
import static ru.yandex.market.deepmind.common.repository.logicstics.warehouse.DeepmindWarehouseRepository.MARSHRUT_ID;
import static ru.yandex.market.deepmind.common.repository.logicstics.warehouse.DeepmindWarehouseRepository.ROSTOV_ID;
import static ru.yandex.market.deepmind.common.repository.logicstics.warehouse.DeepmindWarehouseRepository.SAINT_PETERSBURG_ID;
import static ru.yandex.market.deepmind.common.repository.logicstics.warehouse.DeepmindWarehouseRepository.SAMARA_ID;
import static ru.yandex.market.deepmind.common.repository.logicstics.warehouse.DeepmindWarehouseRepository.TOMILINO_ID;

/**
 * Test of {@link RuleEngineService} of files: {@link rule_engine_v6}. More complex transitions.
 */
public class RuleEngineServiceTest extends BaseRuleEngineServiceTest {

    private RuleEngineService ruleEngineService;

    @Before
    public void setUp() throws Exception {
        ruleEngineService = RuleEngineService.createV6();
    }

    @Test(timeout = 10_000)
    public void testLoop() {
        var engineService = RuleEngineService.create("rule_engine/CatchLoopTest.drl");

        var reCategory = new RECategory(50);
        var status1 = new REMskuStatus()
            .setId(1).setCategoryId(50L).setStatus(MskuStatusValue.END_OF_LIFE);

        Assertions.assertThatThrownBy(() -> {
            var block = new REBlock()
                .addCategories(reCategory)
                .addMskuStatuses(status1);
            engineService.processStatuses(block);
        }).hasMessageContaining("Loop detected in rule");
    }

    /**
     * Если сейчас сезон, но товар не продается, то правила могут зациклится.
     * Т.е. одно правило будет переводить в INACTIVE, другое в ACTIVE (на момент написания это были A2 & I4).
     *
     * Проверяем, что в таком случае статус перейдет в INACTIVE.
     * https://st.yandex-team.ru/DEEPMIND-262#606b0d55d5b43268682a33eb
     */
    @Test
    public void testBigSeasonButWithoutSales() {
        var category50 = category(50).setSeasonId(100L);
        var msku = msku(100, SEASONAL, 50).setSeasonId(100L);
        var season = season(100L, LocalDate.now().minusDays(300), LocalDate.now().plusDays(1));
        var ssku = ssku(1, "sku", msku, ACTIVE, REAL_SUPPLIER)
            .setStatusStartTime(Instant.now().minus(300, ChronoUnit.DAYS));

        var reBlock = new REBlock()
            .addMskuStatuses(msku)
            .addCategories(category50)
            .addSskuStatus(ssku)
            .addSeasons(season)
            .addPurchasePrice(purchasePrice(ssku));
        ruleEngineService.processStatuses(reBlock);

        Assertions.assertThat(ssku.getAvailability()).isEqualTo(ACTIVE);
        Assertions.assertThat(ssku.getRulesThatChangedObject()).isEmpty();

        // Раньше тут было вот так
//        Assertions.assertThat(ssku.getAvailability()).isEqualTo(INACTIVE);
//        Assertions.assertThat(ssku.getRulesThatChangedObject()).containsExactly(
//            "A2. ЕСЛИ (SSKU = Active)" +
//                " И (продажи = 0 больше 60 дней от даты перехода в статус Active) " +
//                "ТО (SSKU = Inactive)"
//        );
//
//        Assertions.assertThat(msku.getStatus()).isEqualTo(SEASONAL);
//        Assertions.assertThat(msku.getRulesThatChangedObject()).isEmpty();
    }

    /**
     * Если год назад статус INACTIVE было переведено во время сезона -
     * значит тогда (год назад) робот его перевел по правилу из-за отсутствия продаж.
     *
     * Т.е. сезонный товар не продавался в прошлый сезон и значит в этот сезон тоже не будет продаваться.
     * Значит не надо его переводить в Active.
     * https://st.yandex-team.ru/DEEPMIND-262#606b0d55d5b43268682a33eb
     */
    @Test
    public void fromInactiveSeasonInPeriodButYearAgoDoNothing() {
        var category50 = category(50).setSeasonId(100L);
        var msku = msku(100, SEASONAL, 50).setSeasonId(100L);
        var season = season(100L, LocalDate.now().minusDays(10), LocalDate.now().plusDays(10));
        var ssku = ssku(1, "sku", msku, INACTIVE, REAL_SUPPLIER)
            .setStatusStartTime(Instant.now().minus(365, ChronoUnit.DAYS));

        var reBlock = new REBlock()
            .addMskuStatuses(msku)
            .addCategories(category50)
            .addSskuStatus(ssku)
            .addSeasons(season)
            .addPurchasePrice(purchasePrice(ssku));
        var result = ruleEngineService.processStatuses(reBlock);

        Assertions.assertThat(result.getChangedSize()).isZero();
    }

    /**
     * Тоже самое только несколько лет назад.
     * https://st.yandex-team.ru/DEEPMIND-262#606b0d55d5b43268682a33eb
     */
    @Test
    public void fromInactiveSeasonInPeriodButYearsAgoDoNothing() {
        var category50 = category(50).setSeasonId(100L);
        var msku = msku(100, SEASONAL, 50).setSeasonId(100L);
        var season = season(100L, LocalDate.now().minusDays(10), LocalDate.now().plusDays(10));
        var ssku = ssku(1, "sku", msku, INACTIVE, REAL_SUPPLIER)
            .setStatusStartTime(Instant.now().minus(3 * 365, ChronoUnit.DAYS));

        var reBlock = new REBlock()
            .addMskuStatuses(msku)
            .addCategories(category50)
            .addSskuStatus(ssku)
            .addSeasons(season)
            .addPurchasePrice(purchasePrice(ssku));
        var result = ruleEngineService.processStatuses(reBlock);

        Assertions.assertThat(result.getChangedSize()).isZero();
    }

    /**
     * Тоже самое только в прошлый период, если их 2.
     * https://st.yandex-team.ru/DEEPMIND-262#606b0d55d5b43268682a33eb
     */
    @Test
    public void fromInactiveSeasonInPrevPeriodDoNothing() {
        var category50 = category(50).setSeasonId(100L);
        var msku = msku(100, SEASONAL, 50).setSeasonId(100L);
        var season = season(100L,
            LocalDate.now().minusDays(100), LocalDate.now().minusDays(90),
            LocalDate.now(), LocalDate.now().plusDays(10));
        var ssku = ssku(1, "sku", msku, INACTIVE, REAL_SUPPLIER)
            .setStatusStartTime(Instant.now().minus(92, ChronoUnit.DAYS));

        var reBlock = new REBlock()
            .addMskuStatuses(msku)
            .addCategories(category50)
            .addSskuStatus(ssku)
            .addSeasons(season)
            .addPurchasePrice(purchasePrice(ssku));
        var result = ruleEngineService.processStatuses(reBlock);

        Assertions.assertThat(result.getChangedSize()).isZero();
    }

    @Test
    public void fromActiveOutSeasonToInactive() {
        var category50 = category(50).setSeasonId(100L);
        var msku = msku(100, SEASONAL, 50).setSeasonId(100L);
        var season = season(100L, LocalDate.now().plusDays(1), LocalDate.now().plusDays(1));
        var ssku = ssku(1, "sku", msku, ACTIVE, REAL_SUPPLIER);
        var sales = sales(ssku, 300, Instant.now());

        var reBlock = new REBlock()
            .addMskuStatuses(msku)
            .addCategories(category50)
            .addSskuStatus(ssku)
            .addSeasons(season)
            .addSales(sales)
            .addPurchasePrice(purchasePrice(ssku));
        ruleEngineService.processStatuses(reBlock);

        Assertions.assertThat(ssku.getAvailability()).isEqualTo(INACTIVE);
        Assertions.assertThat(ssku.getRulesThatChangedObject()).containsExactly(
            "AP3. ЕСЛИ (SSKU = Active или Pending) И (MSKU = Seasonal) " +
                "И (текущая дата > даты окончания периода Seasonal) " +
                "ТО (SSKU = Inactive)"
        );

        Assertions.assertThat(msku.getStatus()).isEqualTo(SEASONAL);
        Assertions.assertThat(msku.getRulesThatChangedObject()).isEmpty();
    }

    @Test
    @SuppressWarnings("checkstyle:linelength")
    public void fromActiveOutSeasonToActiveWithoutSeason() {
        var category50 = category(50); // no season
        var msku = msku(100, SEASONAL, 50, false).setSeasonId(100L);
        var season = season(100L, LocalDate.now().plusDays(1), LocalDate.now().plusDays(1));
        var ssku = ssku(1, "sku", msku, INACTIVE, REAL_SUPPLIER);
        var sales = sales(ssku, 300, Instant.now());

        var reBlock = new REBlock()
            .addMskuStatuses(msku)
            .addCategories(category50)
            .addSskuStatus(ssku)
            .addSeasons(season)
            .addSales(sales)
            .addPurchasePrice(purchasePrice(ssku));
        ruleEngineService.processStatuses(reBlock);

        Assertions.assertThat(ssku.getAvailability()).isEqualTo(INACTIVE);
        Assertions.assertThat(ssku.getRulesThatChangedObject()).isEmpty();

        Assertions.assertThat(msku.getStatus()).isEqualTo(END_OF_LIFE);
        Assertions.assertThat(msku.getRulesThatChangedObject()).contains(
            "M7. ЕСЛИ (MSKU = Seasonal) И (для категории отсутствует период Seasonal) И (Статус MSKU задан не вручную пользователем) ТО (MSKU = Regular)",
            "M5. ЕСЛИ (MSKU = Regular или In/Out) И (все SSKU <> Active или Pending) ТО (MSKU = End_of_life)"
        );
    }

    /**
     * https://st.yandex-team.ru/DEEPMIND-2387
     */
    @Test
    @SuppressWarnings("checkstyle:linelength")
    public void fromActiveOutSeasonToActiveWithoutSeasonModifiedByUser() {
        var category50 = category(50); // no season
        var msku = msku(100, SEASONAL, 50, true).setSeasonId(100L);
        var season = season(100L, LocalDate.now().plusDays(1), LocalDate.now().plusDays(1));
        var ssku = ssku(1, "sku", msku, INACTIVE, REAL_SUPPLIER);
        var sales = sales(ssku, 300, Instant.now());

        var reBlock = new REBlock()
                .addMskuStatuses(msku)
                .addCategories(category50)
                .addSskuStatus(ssku)
                .addSeasons(season)
                .addSales(sales)
                .addPurchasePrice(purchasePrice(ssku));
        ruleEngineService.processStatuses(reBlock);

        Assertions.assertThat(ssku.getAvailability()).isEqualTo(INACTIVE);
        Assertions.assertThat(ssku.getRulesThatChangedObject()).isEmpty();

        Assertions.assertThat(msku.getStatus()).isEqualTo(SEASONAL);
        Assertions.assertThat(msku.getRulesThatChangedObject()).doesNotContain(
                "M7. ЕСЛИ (MSKU = Seasonal) И (для категории отсутствует период Seasonal) И (Статус MSKU задан не вручную пользователем) ТО (MSKU = Regular)"
        );
    }

    /**
     * https://st.yandex-team.ru/DEEPMIND-262#6067263da7460163c0ae9661
     */
    @Test
    public void fromActiveAndArchiveToInactiveAndEndOfLife() {
        var category50 = category(50);
        var msku = msku(100, ARCHIVE, 50);
        var ssku = ssku(1, "sku", msku, ACTIVE, REAL_SUPPLIER)
            .setStatusStartTime(Instant.now().minus(666, ChronoUnit.DAYS));
        var stockStorage = stockStorage(ssku, EKATERINBURG_ID, 93);

        var reBlock = new REBlock()
            .addMskuStatuses(msku)
            .addCategories(category50)
            .addSskuStatus(ssku)
            .addStockStorages(stockStorage)
            .addPurchasePrice(purchasePrice(ssku));
        ruleEngineService.processStatuses(reBlock);

        Assertions.assertThat(ssku.getAvailability()).isEqualTo(INACTIVE);
        Assertions.assertThat(ssku.getRulesThatChangedObject()).containsExactly(
            "A2. ЕСЛИ (SSKU = Active) И (MSKU <> Seasonal) И (Сток > 0) " +
                "И (Продажи = 0 больше 60 дней от даты перехода в статус Active) ТО (SSKU = Inactive)"
        );

        Assertions.assertThat(msku.getStatus()).isEqualTo(END_OF_LIFE);
        // В теории тут также могут быть следующие переходы:
        // M7 Archive -> EOL
        // M10. EOL -> Regular
        // A2. Active -> Inactive
        // M5. Regular -> EOL (то есть Msku 2 раза побывал в статусе EOL).
        // Чтобы не было лишних исполнения правил, приоритет у msku правил понижен см. rule_engine_v4/MskuRulesV4.drl
        // Если тест начал флапать, значит последовательность нарушилась
        Assertions.assertThat(msku.getRulesThatChangedObject()).containsExactly(
            "M8. ЕСЛИ (MSKU = Archive) И (SSKU <> Delisted) ТО (MSKU = End_of_life)"
        );
    }

    @Test
    public void inactiveWithoutStocksToDelistedAndArchive() {
        var category50 = category(50);
        var msku = msku(100, REGULAR, 50);
        var ssku = ssku(1, "sku", msku, INACTIVE, REAL_SUPPLIER)
            .setStatusStartTime(Instant.now().minus(666, ChronoUnit.DAYS));

        var reBlock = new REBlock()
            .addMskuStatuses(msku)
            .addCategories(category50)
            .addSskuStatus(ssku)
            .addPurchasePrice(purchasePrice(ssku));
        ruleEngineService.processStatuses(reBlock);

        Assertions.assertThat(ssku.getAvailability()).isEqualTo(DELISTED);
        Assertions.assertThat(ssku.getRulesThatChangedObject()).containsExactly(
            "I2. ЕСЛИ (SSKU = Inactive)" +
                " И (MSKU <> Seasonal)" +
                " И (Сток = 0 больше 60 дней от даты перехода в статус Inactive) " +
                "ТО (SSKU = Delisted)"
        );

        Assertions.assertThat(msku.getStatus()).isEqualTo(ARCHIVE);
        Assertions.assertThat(msku.getRulesThatChangedObject()).containsExactly(
            "M5. ЕСЛИ (MSKU = Regular или In/Out) И (все SSKU <> Active или Pending) ТО (MSKU = End_of_life)",
            "M6. ЕСЛИ (MSKU = End_of_life) И (все SSKU = Delisted) ТО (MSKU = Archive)"
        );
    }

    /**
     * Если товар сезонный и очень давно не было стоков, то так и будет INACTIVE.
     */
    @Test
    public void seasonalInactiveDoNothing() {
        var category50 = category(50).setSeasonId(100L);
        var msku = msku(100, SEASONAL, 50).setSeasonId(100L);
        var season = season(100L, LocalDate.now().minusDays(2), LocalDate.now().minusDays(1));
        var ssku = ssku(1, "sku", msku, INACTIVE, REAL_SUPPLIER)
            .setStatusStartTime(Instant.now().minus(70, ChronoUnit.DAYS));

        var reBlock = new REBlock()
            .addMskuStatuses(msku)
            .addCategories(category50)
            .addSskuStatus(ssku)
            .addSeasons(season)
            .addPurchasePrice(purchasePrice(ssku));
        ruleEngineService.processStatuses(reBlock);

        Assertions.assertThat(ssku.getAvailability()).isEqualTo(INACTIVE);
        Assertions.assertThat(ssku.getRulesThatChangedObject()).isEmpty();

        Assertions.assertThat(msku.getStatus()).isEqualTo(SEASONAL);
        Assertions.assertThat(msku.getRulesThatChangedObject()).isEmpty();
    }

    /**
     * Если товар сезонный и пользователь руками выставил INACTIVE, то статус станет DELISTED.
     */
    @Test
    public void seasonalInactiveByUserToDelisted() {
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

        Assertions.assertThat(msku.getStatus()).isEqualTo(SEASONAL);
        Assertions.assertThat(msku.getRulesThatChangedObject()).isEmpty();
    }

    /**
     * Если товар сезонный и пользователь руками выставил INACTIVE, то статус станет DELISTED.
     */
    @Test
    public void seasonalInPeriodInactiveByUserToDelisted() {
        var category50 = category(50).setSeasonId(100L);
        var msku = msku(100, SEASONAL, 50).setSeasonId(100L);
        var season = season(100L, LocalDate.now(), LocalDate.now());
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

        Assertions.assertThat(msku.getStatus()).isEqualTo(SEASONAL);
        Assertions.assertThat(msku.getRulesThatChangedObject()).isEmpty();
    }

    /**
     * Если товар сезонный (вне сезона) и пользователь руками выставил INACTIVE, то статус станет DELISTED.
     */
    @Test
    public void seasonalOutPeriodInactiveByUserToDelisted() {
        var category50 = category(50).setSeasonId(100L);
        var msku = msku(100, SEASONAL, 50).setSeasonId(100L);
        var season = season(100L, LocalDate.now().plusDays(1), LocalDate.now().plusDays(1));
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

        Assertions.assertThat(msku.getStatus()).isEqualTo(SEASONAL);
        Assertions.assertThat(msku.getRulesThatChangedObject()).isEmpty();
    }

    /**
     * Если товар сезонный и пользователь руками выставил Active, то останется статус останется Active.
     */
    @Test
    public void seasonalBackToActiveInPeriod() {
        var category50 = category(50).setSeasonId(100L);
        var msku = msku(100, SEASONAL, 50).setSeasonId(100L);
        var season = season(100L, LocalDate.now(), LocalDate.now());
        var ssku = ssku(1, "sku", msku, ACTIVE, REAL_SUPPLIER)
            .setModifiedByUser(true);

        var reBlock = new REBlock()
            .addMskuStatuses(msku)
            .addCategories(category50)
            .addSskuStatus(ssku)
            .addSeasons(season)
            .addPurchasePrice(purchasePrice(ssku));
        ruleEngineService.processStatuses(reBlock);

        Assertions.assertThat(ssku.getAvailability()).isEqualTo(ACTIVE);
        Assertions.assertThat(ssku.getRulesThatChangedObject()).isEmpty();

        Assertions.assertThat(msku.getStatus()).isEqualTo(SEASONAL);
        Assertions.assertThat(msku.getRulesThatChangedObject()).isEmpty();

        // если будет вне периода
        reBlock = new REBlock()
            .addMskuStatuses(msku)
            .addCategories(category50)
            .addSskuStatus(ssku)
            .addSeasons(season(100L, LocalDate.now().plusDays(1), LocalDate.now().plusDays(1)))
            .addPurchasePrice(purchasePrice(ssku));
        ruleEngineService.processStatuses(reBlock);

        Assertions.assertThat(ssku.getAvailability()).isEqualTo(INACTIVE);
        Assertions.assertThat(ssku.getRulesThatChangedObject()).containsExactly(
            "AP3. ЕСЛИ (SSKU = Active или Pending)" +
                " И (MSKU = Seasonal) И (текущая дата > даты окончания периода Seasonal) " +
                "ТО (SSKU = Inactive)"
        );

        Assertions.assertThat(msku.getStatus()).isEqualTo(SEASONAL);
        Assertions.assertThat(msku.getRulesThatChangedObject()).isEmpty();
    }

    /**
     * Если давно не было продаж и стоков, то ssku & msku завершат свой жизненный цикл.
     */
    @Test
    public void fromRegularAndInactiveToArchive() {
        var category50 = category(50);
        var msku = msku(100, REGULAR, 50);
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
            "I2. ЕСЛИ (SSKU = Inactive) И (MSKU <> Seasonal) И (Сток = 0 больше 60 дней от даты перехода " +
                "в статус Inactive) ТО (SSKU = Delisted)"
        );

        Assertions.assertThat(msku.getStatus()).isEqualTo(ARCHIVE);
        Assertions.assertThat(msku.getRulesThatChangedObject()).containsExactly(
            "M5. ЕСЛИ (MSKU = Regular или In/Out) И (все SSKU <> Active или Pending) ТО (MSKU = End_of_life)",
            "M6. ЕСЛИ (MSKU = End_of_life) И (все SSKU = Delisted) ТО (MSKU = Archive)"
        );
    }

    /**
     * Если давно не было продаж и стоков, то ssku & msku завершат свой жизненный цикл.
     */
    @Test
    public void fromNpdAndInactiveToArchive() {
        var category50 = category(50);
        var msku = msku(100, NPD, 50)
            .setNpdFinishDate(LocalDate.now().minusDays(1));
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
            "I2. ЕСЛИ (SSKU = Inactive) И (MSKU <> Seasonal) И (Сток = 0 больше 60 дней от даты перехода " +
                "в статус Inactive) ТО (SSKU = Delisted)"
        );

        Assertions.assertThat(msku.getStatus()).isEqualTo(ARCHIVE);
        Assertions.assertThat(msku.getRulesThatChangedObject()).containsExactly(
            "M4. ЕСЛИ (MSKU = NPD) И (текущая дата > дата окончания NPD) ТО (MSKU = Regular)",
            "M5. ЕСЛИ (MSKU = Regular или In/Out) И (все SSKU <> Active или Pending) ТО (MSKU = End_of_life)",
            "M6. ЕСЛИ (MSKU = End_of_life) И (все SSKU = Delisted) ТО (MSKU = Archive)"
        );
    }

    @Test
    public void fromActiveAndInOutToEOL() {
        var category50 = category(50);
        var msku = msku(100, IN_OUT, 50)
            .setInoutStartDate(LocalDate.now().minusDays(10))
            .setInoutFinishDate(LocalDate.now().minusDays(1));
        var ssku = ssku(1, "sku", msku, ACTIVE, REAL_SUPPLIER)
            .setStatusStartTime(Instant.now().minus(70, ChronoUnit.DAYS));
        var sales = sales(ssku, 300, Instant.now().minus(10, ChronoUnit.DAYS));
        var stocks = stockStorage(ssku, CROSSDOCK_SOFINO_ID, 1);

        var reBlock = new REBlock()
            .addMskuStatuses(msku)
            .addCategories(category50)
            .addSskuStatus(ssku)
            .addSales(sales)
            .addPurchasePrice(purchasePrice(ssku))
            .addStockStorages(stocks);
        ruleEngineService.processStatuses(reBlock);

        Assertions.assertThat(ssku.getAvailability()).isEqualTo(INACTIVE);
        Assertions.assertThat(ssku.getRulesThatChangedObject()).containsExactly(
            "AP4. ЕСЛИ (SSKU = Active или Pending) И (MSKU = In/Out)" +
                " И (текущая дата > даты окончания периода In/Out) ТО (SSKU = Inactive)"
        );

        Assertions.assertThat(msku.getStatus()).isEqualTo(END_OF_LIFE);
        Assertions.assertThat(msku.getRulesThatChangedObject()).containsExactly(
            "M5. ЕСЛИ (MSKU = Regular или In/Out) И (все SSKU <> Active или Pending) ТО (MSKU = End_of_life)"
        );
    }

    /**
     * Если IN/OUT, но пользователь с форсом переводит в INACTIVE, то система завершает свой ЖЦ.
     */
    @Test
    public void fromActiveInOutToForceInactiveByUser() {
        var category50 = category(50);
        var msku = msku(100, IN_OUT, 50)
            .setInoutStartDate(LocalDate.now().minusDays(10))
            .setInoutFinishDate(LocalDate.now().plusDays(1));
        var ssku = ssku(1, "sku", msku, INACTIVE, REAL_SUPPLIER)
            .setStatusStartTime(Instant.now().minus(1, ChronoUnit.DAYS))
            .setModifiedByUser(false);
        var sales = sales(ssku, 300, Instant.now().minus(10, ChronoUnit.DAYS));

        var reBlock = new REBlock()
            .addMskuStatuses(msku)
            .addCategories(category50)
            .addSskuStatus(ssku)
            .addSales(sales)
            .addPurchasePrice(purchasePrice(ssku));
        ruleEngineService.processStatuses(reBlock);

        Assertions.assertThat(ssku.getAvailability()).isEqualTo(INACTIVE);
        Assertions.assertThat(ssku.getRulesThatChangedObject()).isEmpty();

        Assertions.assertThat(msku.getStatus()).isEqualTo(END_OF_LIFE);
        Assertions.assertThat(msku.getRulesThatChangedObject()).containsExactly(
            "M5. ЕСЛИ (MSKU = Regular или In/Out) И (все SSKU <> Active или Pending) ТО (MSKU = End_of_life)"
        );
    }

    @Test
    public void a2i2FromActiveWithoutStocksAndSalesToDelisted() {
        var category50 = category(50);
        var msku = msku(100, ARCHIVE, 50);
        var ssku = ssku(1, "sku", msku, ACTIVE, REAL_SUPPLIER)
            .setStatusStartTime(Instant.now().minus(103, ChronoUnit.DAYS));

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

        Assertions.assertThat(msku.getStatus()).isEqualTo(ARCHIVE);
    }

    @Test
    public void a2i2FromActiveWithZeroStocksAndSalesToDelisted() {
        var category50 = category(50);
        var msku = msku(100, ARCHIVE, 50);
        var ssku = ssku(1, "sku", msku, ACTIVE, REAL_SUPPLIER)
            .setStatusStartTime(Instant.now().minus(666, ChronoUnit.DAYS));
        var stockStorage = stockStorage(ssku, EKATERINBURG_ID, 0)
            .setFitDisappearTime(Instant.now().minus(876, ChronoUnit.DAYS));

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

        Assertions.assertThat(msku.getStatus()).isEqualTo(ARCHIVE);
    }

    /**
     * DEEPMIND-451 No changes.
     */
    @Test
    public void a2i2Test() {
        var category = category(90713);
        var msku = msku(552470001, REGULAR, 90713)
            .setModificationTs(Instant.parse("2019-11-13T21:30:00.558057Z"))
            .setModifiedByUser(false)
            .setNpdFinishDate(LocalDate.parse("2019-11-13"));
        var ssku1 = ssku(465852, "000182.4690612021331", msku, ACTIVE, REAL_SUPPLIER)
            .setModificationTs(Instant.now().minus(7, ChronoUnit.DAYS)) // новый ssku
            .setStatusStartTime(Instant.now().minus(7, ChronoUnit.DAYS))
            .setModifiedByUser(true);
        var ssku2  = ssku(465852, "000182.4690612020570", msku, DELISTED, REAL_SUPPLIER)
            .setModificationTs(Instant.now().minus(3, ChronoUnit.DAYS)) // новый ssku
            .setStatusStartTime(Instant.now().minus(3, ChronoUnit.DAYS))
            .setModifiedByUser(false);

        var reBlock = new REBlock()
            .addMskuStatuses(msku)
            .addCategories(category)
            .addSskuStatus(ssku1, ssku2)
            .addStockStorages(
                stockStorage(ssku2, 172, 0)
                    .setFitAppearTime(Instant.parse("2020-06-01T00:00:00.00Z"))
                    .setFitDisappearTime(Instant.parse("2020-06-06T00:00:00.00Z")),
                stockStorage(ssku2, 147, 0)
                    .setFitAppearTime(Instant.parse("2019-09-20T00:00:00.00Z"))
                    .setFitDisappearTime(Instant.parse("2021-02-06T00:00:00.00Z")),
                stockStorage(ssku2, 145, 0)
                    .setFitAppearTime(Instant.parse("2019-09-14T00:00:00.00Z"))
                    .setFitDisappearTime(Instant.parse("2020-12-06T00:00:00.00Z")),
                stockStorage(ssku2, 145, 0)
                    .setFitAppearTime(Instant.parse("2019-10-15T00:00:00.00Z"))
                    .setFitDisappearTime(Instant.parse("2019-11-08T00:00:00.00Z"))
            )
            .addSales(sales(ssku2, 300, Instant.now().minus(45, ChronoUnit.DAYS)))
            .addPurchasePrice(purchasePrice(ssku1, ssku2));
        var noChanges = ruleEngineService.processStatuses(reBlock);

        Assertions.assertThat(ssku1.getAvailability()).isEqualTo(ACTIVE);
        Assertions.assertThat(ssku2.getAvailability()).isEqualTo(DELISTED);

        Assertions.assertThat(noChanges.getChangedShopSkuKeys()).isEmpty();
    }

    /**
     * Если ни стоков, ни продаж уже давно нет, но msku сезонный, то сработает только правило A2.
     */
    @Test
    public void fromActiveAndSeasonalDoNothing() {
        var category50 = category(50).setSeasonId(777L);
        var season = season(777L, LocalDate.now(), LocalDate.now().plusDays(1));
        var msku = msku(100, SEASONAL, 50).setSeasonId(777L);
        var ssku = ssku(1, "sku", msku, ACTIVE, REAL_SUPPLIER)
            .setStatusStartTime(Instant.now().minus(666, ChronoUnit.DAYS));

        var reBlock = new REBlock()
            .addMskuStatuses(msku)
            .addCategories(category50)
            .addSskuStatus(ssku)
            .addSeasons(season)
            .addPurchasePrice(purchasePrice(ssku));
        ruleEngineService.processStatuses(reBlock);

        Assertions.assertThat(ssku.getAvailability()).isEqualTo(ACTIVE);
        Assertions.assertThat(ssku.getRulesThatChangedObject()).isEmpty();
    }

    @Test
    public void testModifiedByUser() {
        var category50 = category(50);
        var msku = msku(100, ARCHIVE, 50);
        var ssku = ssku(1, "sku", msku, DELISTED, REAL_SUPPLIER).setModifiedByUser(true);
        var stockStorage = stockStorage(ssku, TOMILINO_ID, 1);

        var reBlock = new REBlock()
            .addMskuStatuses(msku)
            .addCategories(category50)
            .addSskuStatus(ssku)
            .addStockStorages(stockStorage)
            .addPurchasePrice(purchasePrice(ssku));
        ruleEngineService.processStatuses(reBlock);

        Assertions.assertThat(ssku.getAvailability()).isEqualTo(INACTIVE);
        Assertions.assertThat(ssku.isRobotChanges()).isTrue();
        Assertions.assertThat(ssku.isModifiedByUser()).isFalse();
    }

    /**
     * Проверяем цепочку событий для сезонной ssku.
     * 1. сезон не активен ssku = INACTIVE
     * 2. сезон начался ssku = PENDING
     * 3. товар есть на стоках ssku = ACTIVE
     * 4. У ssku нет закупочной цены, ssku = INACTIVE_TMP
     * 5. Закупочную цену добавили, ssku = PENDING
     * 6. товар есть на стоках ssku = ACTIVE
     * 7. Сезон закончился, ssku = INACTIVE
     */
    @Test
    public void testSeasonalAndNoPurchasePriceChain() {
        var category50 = category(50).setSeasonId(100L);
        var msku = msku(100, SEASONAL, 50).setSeasonId(100L);
        var season = season(100L, LocalDate.parse("2021-06-03"), LocalDate.parse("2021-06-10"));
        var ssku = ssku(1, "sku", msku, INACTIVE, REAL_SUPPLIER)
            .setStatusStartTime(Instant.parse("2021-06-01T10:00:00Z"));
        var stock = stockStorage(ssku, SAINT_PETERSBURG_ID, 1);

        var reBlock1 = new REBlock()
            .addMskuStatuses(msku)
            .addCategories(category50)
            .addSskuStatus(ssku)
            .addSeasons(season)
            .addStockStorages(stock);
        ssku.clearRules();
        ruleEngineService.processStatuses(reBlock1, clock("2021-06-04T10:00:00Z"));

        // assert
        Assertions.assertThat(ssku.getRulesThatChangedObject()).contains(
            "I4. ЕСЛИ (SSKU = Inactive)" +
                " И (MSKU = Seasonal)" +
                " И (текущая дата >= даты начала периода Seasonal)" +
                " И (переход в статус Inactive сделан автоматически системой)" +
                " И (переход в статус был осуществлен вне периода) " +
                "ТО (SSKU = Pending)",
            // Это правило может сработать, а может и нет, но для нас это не очень важно
            // главное, чтобы статус стал INACTIVE_TMP
            // "P1. ЕСЛИ (SSKU = PENDING) и (Сток > 0) ТО (SSKU = Active)",
            "AP5. ЕСЛИ (SSKU = Active или Pending) И (нет закупочной цены) " +
                "ТО (SSKU = Inactive_tmp) И (Причина = Нет закупочной цены) И (Дата окончания = null)"
        );
        Assertions.assertThat(ssku.getAvailability()).isEqualTo(INACTIVE_TMP);

        // заливаем закупочные цены
        var reBlock2 = new REBlock()
            .addMskuStatuses(msku)
            .addCategories(category50)
            .addSskuStatus(ssku)
            .addSeasons(season)
            .addStockStorages(stock)
            .addPurchasePrice(purchasePrice(ssku));
        ssku.clearRules();
        ruleEngineService.processStatuses(reBlock2, clock("2021-06-05T10:00:00Z"));

        // assert
        Assertions.assertThat(ssku.getRulesThatChangedObject()).containsExactly(
            "IT4. ЕСЛИ (SSKU = Inactive_tmp) И (Причина = Нет закупочной цены) И (есть закупочная цена) " +
                "ТО (SSKU = Pending)",
            "P1. ЕСЛИ (SSKU = PENDING) и (Сток > 0) ТО (SSKU = Active)"
        );
        Assertions.assertThat(ssku.getAvailability()).isEqualTo(ACTIVE);

        // сезон заканчивается
        ssku.clearRules();
        ruleEngineService.processStatuses(reBlock2, clock("2021-06-11T10:00:00Z"));

        // assert
        Assertions.assertThat(ssku.getRulesThatChangedObject()).containsExactly(
            "AP3. ЕСЛИ (SSKU = Active или Pending) И (MSKU = Seasonal)" +
                " И (текущая дата > даты окончания периода Seasonal) " +
                "ТО (SSKU = Inactive)"
        );
        Assertions.assertThat(ssku.getAvailability()).isEqualTo(INACTIVE);
    }

    /**
     * Проверяем цепочку событий для сезонной ssku.
     * 1. сезон не активен ssku = INACTIVE
     * 2. сезон начался ssku = PENDING
     * 3. товар есть на стоках ssku = ACTIVE
     * 4. У ssku нет закупочной цены, ssku = INACTIVE_TMP
     * 5. Сезон закончился, ssku = INACTIVE_TMP (без изменений)
     * 6. Закупочную цену добавили, ssku = PENDING
     * 5. Сезон закончился, ssku = INACTIVE
     */
    @Test
    public void testSeasonalAndNoPurchasePriceChain2() {
        var category50 = category(50).setSeasonId(100L);
        var msku = msku(100, SEASONAL, 50).setSeasonId(100L);
        var season = season(100L, LocalDate.parse("2021-06-03"), LocalDate.parse("2021-06-10"));
        var ssku = ssku(1, "sku", msku, INACTIVE, REAL_SUPPLIER)
            .setStatusStartTime(Instant.parse("2021-06-01T10:00:00Z"));
        var stock = stockStorage(ssku, ROSTOV_ID, 1);

        var reBlock1 = new REBlock()
            .addMskuStatuses(msku)
            .addCategories(category50)
            .addSskuStatus(ssku)
            .addSeasons(season)
            .addStockStorages(stock);
        ssku.clearRules();
        ruleEngineService.processStatuses(reBlock1, clock("2021-06-04T10:00:00Z"));

        // assert
        Assertions.assertThat(ssku.getRulesThatChangedObject()).contains(
            "I4. ЕСЛИ (SSKU = Inactive)" +
                " И (MSKU = Seasonal)" +
                " И (текущая дата >= даты начала периода Seasonal)" +
                " И (переход в статус Inactive сделан автоматически системой)" +
                " И (переход в статус был осуществлен вне периода) " +
                "ТО (SSKU = Pending)",
            // Это правило может сработать, а может и нет, но для нас это не очень важно
            // главное, чтобы статус стал INACTIVE_TMP
            // "P1. ЕСЛИ (SSKU = PENDING) и (Сток > 0) ТО (SSKU = Active)",
            "AP5. ЕСЛИ (SSKU = Active или Pending) И (нет закупочной цены) " +
                "ТО (SSKU = Inactive_tmp) И (Причина = Нет закупочной цены) И (Дата окончания = null)"
        );
        Assertions.assertThat(ssku.getAvailability()).isEqualTo(INACTIVE_TMP);

        // сезон заканчивается
        ssku.clearRules();
        ruleEngineService.processStatuses(reBlock1, clock("2021-06-11T10:00:00Z"));

        // assert
        Assertions.assertThat(ssku.getRulesThatChangedObject()).isEmpty();
        Assertions.assertThat(ssku.getAvailability()).isEqualTo(INACTIVE_TMP);

        // заливаем закупочные цены
        var reBlock2 = new REBlock()
            .addMskuStatuses(msku)
            .addCategories(category50)
            .addSskuStatus(ssku)
            .addSeasons(season)
            .addPurchasePrice(purchasePrice(ssku));
        ssku.clearRules();
        ruleEngineService.processStatuses(reBlock2, clock("2021-06-12T10:00:00Z"));

        // assert
        Assertions.assertThat(ssku.getRulesThatChangedObject()).containsExactly(
            "IT4. ЕСЛИ (SSKU = Inactive_tmp) И (Причина = Нет закупочной цены) И (есть закупочная цена) " +
                "ТО (SSKU = Pending)",
            "AP3. ЕСЛИ (SSKU = Active или Pending) И (MSKU = Seasonal) " +
                "И (текущая дата > даты окончания периода Seasonal) " +
                "ТО (SSKU = Inactive)"
        );
        Assertions.assertThat(ssku.getAvailability()).isEqualTo(INACTIVE);
    }

    @Test
    public void testFromInactiveAndSeasonalToActive() {
        var category50 = category(50).setSeasonId(100L);
        var msku = msku(100, SEASONAL, 50).setSeasonId(100L);
        var season = season(100L, LocalDate.now().minusDays(2), LocalDate.now().plusDays(1));
        var ssku = ssku(1, "sku", msku, INACTIVE, REAL_SUPPLIER)
            .setStatusStartTime(Instant.now().minus(3, ChronoUnit.DAYS));
        var stock = stockStorage(ssku, EKATERINBURG_ID, 1);

        var reBlock = new REBlock()
            .addMskuStatuses(msku)
            .addCategories(category50)
            .addSskuStatus(ssku)
            .addSeasons(season)
            .addPurchasePrice(purchasePrice(ssku))
            .addStockStorages(stock);
        ruleEngineService.processStatuses(reBlock);

        Assertions.assertThat(ssku.getAvailability()).isEqualTo(ACTIVE);
        Assertions.assertThat(ssku.getRulesThatChangedObject()).containsExactly(
            "I4. ЕСЛИ (SSKU = Inactive)" +
                " И (MSKU = Seasonal)" +
                " И (текущая дата >= даты начала периода Seasonal)" +
                " И (переход в статус Inactive сделан автоматически системой)" +
                " И (переход в статус был осуществлен вне периода) " +
                "ТО (SSKU = Pending)",
            "P1. ЕСЛИ (SSKU = PENDING) и (Сток > 0) ТО (SSKU = Active)"
        );
    }

    @Test
    public void testSeasonalToPendingToInactive() {
        var category50 = category(50).setSeasonId(100L);
        var msku = msku(100, SEASONAL, 50).setSeasonId(100L);
        var season = season(100L, LocalDate.parse("2007-05-01"), LocalDate.parse("2007-07-01"));
        var ssku = ssku(1, "sku", msku, INACTIVE, REAL_SUPPLIER)
            .setStatusStartTime(Instant.parse("2007-01-01T10:00:30.00Z"));
        var stock = stockStorage(ssku, MARSHRUT_ID, 0);

        var reBlock1 = new REBlock()
            .addMskuStatuses(msku)
            .addCategories(category50)
            .addSskuStatus(ssku)
            .addSeasons(season)
            .addPurchasePrice(purchasePrice(ssku))
            .addStockStorages(stock);
        ruleEngineService.processStatuses(reBlock1, clock("2021-06-01T10:00:00Z"));

        Assertions.assertThat(ssku.getAvailability()).isEqualTo(PENDING);
        Assertions.assertThat(ssku.getRulesThatChangedObject()).containsExactly(
            "I4. ЕСЛИ (SSKU = Inactive)" +
                " И (MSKU = Seasonal)" +
                " И (текущая дата >= даты начала периода Seasonal)" +
                " И (переход в статус Inactive сделан автоматически системой)" +
                " И (переход в статус был осуществлен вне периода) " +
                "ТО (SSKU = Pending)"
        );

        ssku.clearRules();
        ruleEngineService.processStatuses(reBlock1, clock("2021-08-01T10:00:00Z"));

        Assertions.assertThat(ssku.getAvailability()).isEqualTo(INACTIVE);
        Assertions.assertThat(ssku.getRulesThatChangedObject()).containsExactly(
            "AP3. ЕСЛИ (SSKU = Active или Pending)" +
                " И (MSKU = Seasonal)" +
                " И (текущая дата > даты окончания периода Seasonal) ТО (SSKU = Inactive)"
        );
    }

    @Test
    public void ap5ActiveWithoutPurchasePriceToInactiveTmpAndBackToPending() {
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

        // add purchase price
        ssku.clearRules();
        reBlock.addPurchasePrice(purchasePrice(ssku));
        ruleEngineService.processStatuses(reBlock);

        Assertions.assertThat(ssku.getAvailability()).isEqualTo(PENDING);
        Assertions.assertThat(ssku.getRulesThatChangedObject()).containsExactly(
            "IT4. ЕСЛИ (SSKU = Inactive_tmp) И (Причина = Нет закупочной цены) И (есть закупочная цена) " +
                "ТО (SSKU = Pending)"
        );
        Assertions.assertThat(ssku.isHasNoPurchasePrice()).isFalse();
        Assertions.assertThat(ssku.getStatusFinishTime()).isNull();
    }

    @Test
    public void ap7ActiveWithoutValidContractToInactiveTmpAndBackToPending() {
        var category50 = category(50);
        var msku = msku(100, REGULAR, 50);
        var ssku = ssku(1, "sku", msku, ACTIVE, REAL_SUPPLIER);

        var invalidContract = new REInvalidContract(1);
        var reBlock = new REBlock()
            .addMskuStatuses(msku)
            .addCategories(category50)
            .addSskuStatus(ssku)
            .addPurchasePrice(purchasePrice(ssku))
            .addInvalidContract(invalidContract);
        ruleEngineService.processStatuses(reBlock);

        Assertions.assertThat(ssku.getAvailability()).isEqualTo(INACTIVE_TMP);
        Assertions.assertThat(ssku.getRulesThatChangedObject()).containsExactly(
            "AP7. ЕСЛИ (SSKU = Active или Pending) И (нет действующего договора) " +
                "ТО (SSKU = Inactive_tmp) И (Причина = Нет действующего договора) И (Дата окончания = null)"
        );
        Assertions.assertThat(ssku.isHasNoValidContract()).isTrue();
        Assertions.assertThat(ssku.getStatusFinishTime()).isNull();

        // add valid contract
        ssku.clearRules();
        reBlock.removeInvalidContract(invalidContract);
        ruleEngineService.processStatuses(reBlock);

        Assertions.assertThat(ssku.getAvailability()).isEqualTo(PENDING);
        Assertions.assertThat(ssku.getRulesThatChangedObject()).containsExactly(
            "IT5. ЕСЛИ (SSKU = Inactive_tmp) И (Причина = Нет действующего договора) И (есть действующий договор)" +
                " ТО (SSKU = Pending)"
        );
        Assertions.assertThat(ssku.isHasNoValidContract()).isFalse();
        Assertions.assertThat(ssku.getStatusFinishTime()).isNull();
    }

    /**
     * 1. У ssku нет закупочной цены -> INACTIVE_TMP без срока действия
     * 2. Пользователь установил дату окончания для INACTIVE_TMP
     * 3. Появилась закупочная цена -> ДОЛЖНО ОСТАТЬСЯ INACTIVE_TMP!!!
     * 4. Прошла дата окончания. Возвращаем в PENDING.
     * 5. Есть стоки, возвращаем в ACTIVE
     */
    @Test
    public void inactiveTmpWithManualAndAutoSet() {
        var category50 = category(50);
        var msku = msku(100, REGULAR, 50);
        var ssku = ssku(1, "sku", msku, ACTIVE, REAL_SUPPLIER);
        var stock = stockStorage(ssku, SAMARA_ID, 1);

        var reBlock1 = new REBlock()
            .addMskuStatuses(msku)
            .addCategories(category50)
            .addSskuStatus(ssku)
            .addStockStorages(stock);
        ssku.clearRules();
        ruleEngineService.processStatuses(reBlock1, clock("2021-06-04T10:00:00Z"));

        // assert
        Assertions.assertThat(ssku.getAvailability()).isEqualTo(INACTIVE_TMP);
        Assertions.assertThat(ssku.isHasNoPurchasePrice()).isTrue();
        Assertions.assertThat(ssku.getRulesThatChangedObject()).contains(
            "AP5. ЕСЛИ (SSKU = Active или Pending) И (нет закупочной цены) " +
                "ТО (SSKU = Inactive_tmp) И (Причина = Нет закупочной цены) И (Дата окончания = null)"
        );

        // выставляем дату окончания
        ssku.setAvailability(INACTIVE_TMP)
            .setStatusFinishTime(Instant.parse("2021-06-05T10:00:00Z"));

        // заливаем закупочные цены
        var reBlock2 = new REBlock()
            .addMskuStatuses(msku)
            .addCategories(category50)
            .addSskuStatus(ssku)
            .addStockStorages(stock)
            .addPurchasePrice(purchasePrice(ssku));
        ssku.clearRules();
        ruleEngineService.processStatuses(reBlock2, clock("2021-06-04T11:00:00Z"));

        // assert
        Assertions.assertThat(ssku.getAvailability()).isEqualTo(INACTIVE_TMP);
        Assertions.assertThat(ssku.getRulesThatChangedObject()).isEmpty();
        Assertions.assertThat(ssku.isHasNoPurchasePrice()).isTrue();

        // заканчивается период действия inactive
        ssku.clearRules();
        ruleEngineService.processStatuses(reBlock2, clock("2021-06-05T11:00:00Z"));

        // assert
        Assertions.assertThat(ssku.getAvailability()).isEqualTo(ACTIVE);
        Assertions.assertThat(ssku.getRulesThatChangedObject()).containsExactly(
            "IT2. ЕСЛИ (SSKU = Inactive_tmp)" +
                " И (Текущая дата > даты окончания периода Inactive_tmp)" +
                " И (прошлый статус <> Delisted или Inactive)" +
                " ТО (SSKU = Pending)",
            "P1. ЕСЛИ (SSKU = PENDING) и (Сток > 0) ТО (SSKU = Active)"
        );
        Assertions.assertThat(ssku.isHasNoPurchasePrice()).isFalse();
    }

    /**
     * 1. У ssku нет закупочной цены -> INACTIVE_TMP без срока действия
     * 2. Пользователь установил дату окончания для INACTIVE_TMP
     * 3. После срока окончания периода будет небольшой флап.
     * Так как статус станется PENDING, но тут же снова станет INACTIVE_TMP без срока окончания
     */
    @Test
    public void inactiveTmpWithManualAndAutoSet2() {
        var category50 = category(50);
        var msku = msku(100, REGULAR, 50);
        var ssku = ssku(1, "sku", msku, ACTIVE, REAL_SUPPLIER);
        var stock = stockStorage(ssku, SAMARA_ID, 1);

        var reBlock1 = new REBlock()
            .addMskuStatuses(msku)
            .addCategories(category50)
            .addSskuStatus(ssku)
            .addStockStorages(stock);
        ssku.clearRules();
        ruleEngineService.processStatuses(reBlock1, clock("2021-06-04T10:00:00Z"));

        // assert
        Assertions.assertThat(ssku.getAvailability()).isEqualTo(INACTIVE_TMP);
        Assertions.assertThat(ssku.getRulesThatChangedObject()).contains(
            "AP5. ЕСЛИ (SSKU = Active или Pending) И (нет закупочной цены) " +
                "ТО (SSKU = Inactive_tmp) И (Причина = Нет закупочной цены) И (Дата окончания = null)"
        );

        // выставляем дату окончания
        ssku.setAvailability(INACTIVE_TMP)
            .setStatusFinishTime(Instant.parse("2021-06-05T10:00:00Z"));

        // заливаем закупочные цены
        var reBlock2 = new REBlock()
            .addMskuStatuses(msku)
            .addCategories(category50)
            .addSskuStatus(ssku)
            .addStockStorages(stock);
        ssku.clearRules();
        ruleEngineService.processStatuses(reBlock2, clock("2021-06-05T11:00:00Z"));

        // assert
        Assertions.assertThat(ssku.getAvailability()).isEqualTo(INACTIVE_TMP);
        Assertions.assertThat(ssku.getStatusFinishTime()).isNull();
        Assertions.assertThat(ssku.getRulesThatChangedObject()).contains(
            "IT2. ЕСЛИ (SSKU = Inactive_tmp)" +
                " И (Текущая дата > даты окончания периода Inactive_tmp)" +
                " И (прошлый статус <> Delisted или Inactive)" +
                " ТО (SSKU = Pending)",
            "AP5. ЕСЛИ (SSKU = Active или Pending) И (нет закупочной цены) " +
                "ТО (SSKU = Inactive_tmp) И (Причина = Нет закупочной цены) И (Дата окончания = null)"
        );
    }

    /**
     * Если оффер с окончанием действия INACTIVE_TMP и сразу с planned INACTIVE_TMP
     * finish_time < planned_start_time
     */
    @Test
    public void fromInactiveTmpToPendingAndAgainToInactiveTmp() {
        var category50 = category(50);
        var msku = msku(100, REGULAR, 50);
        var ssku = ssku(1, "sku", msku, INACTIVE_TMP, REAL_SUPPLIER)
            .setStatusFinishTime(Instant.parse("2020-12-01T10:15:30.00Z"))
            .setPlannedStartAt(Instant.parse("2020-12-10T10:15:30.00Z"))
            .setPlannedFinishAt(Instant.parse("2020-12-20T10:15:30.00Z"));

        var reBlock1 = new REBlock()
            .addMskuStatuses(msku)
            .addCategories(category50)
            .addSskuStatus(ssku)
            .addPurchasePrice(purchasePrice(ssku));
        ssku.clearRules();
        ruleEngineService.processStatuses(reBlock1, clock("2020-12-05T00:00:00.00Z"));

        // assert
        Assertions.assertThat(ssku.getAvailability()).isEqualTo(PENDING);
        Assertions.assertThat(ssku.getRulesThatChangedObject()).contains(
            "IT2. ЕСЛИ (SSKU = Inactive_tmp)" +
                " И (Текущая дата > даты окончания периода Inactive_tmp)" +
                " И (прошлый статус <> Delisted или Inactive)" +
                " ТО (SSKU = Pending)"
        );
        Assertions.assertThat(ssku.getPlannedStartAt()).isEqualTo(Instant.parse("2020-12-10T10:15:30.00Z"));
        Assertions.assertThat(ssku.getPlannedFinishAt()).isEqualTo(Instant.parse("2020-12-20T10:15:30.00Z"));

        // запускаем через какое-то время еще раз
        ssku.clearRules();
        ruleEngineService.processStatuses(reBlock1, clock("2020-12-11T00:00:00.00Z"));

        // assert
        Assertions.assertThat(ssku.getAvailability()).isEqualTo(INACTIVE_TMP);
        Assertions.assertThat(ssku.getRulesThatChangedObject()).contains(
            "IT1. ЕСЛИ (Указан период для Inactive_tmp)" +
                " И (Текущая дата >= даты начала периода Inactive_tmp) " +
                "ТО (SSKU = Inactive_tmp)"
        );
        Assertions.assertThat(ssku.getStatusFinishTime()).isEqualTo(Instant.parse("2020-12-20T10:15:30.00Z"));
        Assertions.assertThat(ssku.getPlannedStartAt()).isNull();
        Assertions.assertThat(ssku.getPlannedFinishAt()).isNull();
    }

    /**
     * Если оффер с окончанием действия INACTIVE_TMP и сразу с planned INACTIVE_TMP
     * finish_time >= planned_start_time
     */
    @Test
    public void fromInactiveTmpToInactiveTmp() {
        var category50 = category(50);
        var msku = msku(100, REGULAR, 50);
        var ssku = ssku(1, "sku", msku, INACTIVE_TMP, REAL_SUPPLIER)
            .setStatusFinishTime(Instant.parse("2020-12-15T15:15:30.00Z"))
            .setPlannedStartAt(Instant.parse("2020-12-10T10:15:30.00Z"))
            .setPlannedFinishAt(Instant.parse("2020-12-20T10:15:30.00Z"));

        var reBlock1 = new REBlock()
            .addMskuStatuses(msku)
            .addCategories(category50)
            .addSskuStatus(ssku)
            .addPurchasePrice(purchasePrice(ssku));
        ssku.clearRules();
        ruleEngineService.processStatuses(reBlock1, clock("2020-12-05T00:00:00.00Z"));

        // assert
        Assertions.assertThat(ssku.getAvailability()).isEqualTo(INACTIVE_TMP);
        Assertions.assertThat(ssku.getRulesThatChangedObject()).isEmpty();
        Assertions.assertThat(ssku.getStatusFinishTime()).isEqualTo(Instant.parse("2020-12-15T15:15:30.00Z"));
        Assertions.assertThat(ssku.getPlannedStartAt()).isEqualTo(Instant.parse("2020-12-10T10:15:30.00Z"));
        Assertions.assertThat(ssku.getPlannedFinishAt()).isEqualTo(Instant.parse("2020-12-20T10:15:30.00Z"));

        // запускаем через какое-то время еще раз
        ssku.clearRules();
        ruleEngineService.processStatuses(reBlock1, clock("2020-12-11T00:00:00.00Z"));

        // assert
        Assertions.assertThat(ssku.getAvailability()).isEqualTo(INACTIVE_TMP);
        Assertions.assertThat(ssku.getRulesThatChangedObject()).contains(
            "IT1. ЕСЛИ (Указан период для Inactive_tmp)" +
                " И (Текущая дата >= даты начала периода Inactive_tmp) " +
                "ТО (SSKU = Inactive_tmp)"
        );
        Assertions.assertThat(ssku.getStatusFinishTime()).isEqualTo(Instant.parse("2020-12-20T10:15:30.00Z"));
        Assertions.assertThat(ssku.getPlannedStartAt()).isNull();
        Assertions.assertThat(ssku.getPlannedFinishAt()).isNull();
    }

    @Test
    public void fromPendingAndInoutToInactiveAndEndOfLife() {
        var category50 = category(50);
        var msku = msku(100, IN_OUT, 50)
            .setInoutStartDate(LocalDate.now().minusDays(10))
            .setInoutFinishDate(LocalDate.now().minusDays(1));
        var ssku = ssku(1, "sku", msku, PENDING, REAL_SUPPLIER)
            .setStatusStartTime(Instant.now().minus(70, ChronoUnit.DAYS));
        var sales = sales(ssku, 172, Instant.now().minus(10, ChronoUnit.DAYS));
        var stocks = stockStorage(ssku, CROSSDOCK_SOFINO_ID, 1);

        var reBlock = new REBlock()
            .addMskuStatuses(msku)
            .addCategories(category50)
            .addSskuStatus(ssku)
            .addSales(sales)
            .addPurchasePrice(purchasePrice(ssku))
            .addStockStorages(stocks);
        ruleEngineService.processStatuses(reBlock);

        Assertions.assertThat(ssku.getAvailability()).isEqualTo(INACTIVE);
        Assertions.assertThat(ssku.getRulesThatChangedObject()).contains(
            "AP4. ЕСЛИ (SSKU = Active или Pending) И (MSKU = In/Out)" +
                " И (текущая дата > даты окончания периода In/Out) ТО (SSKU = Inactive)"
        );

        Assertions.assertThat(msku.getStatus()).isEqualTo(END_OF_LIFE);
        Assertions.assertThat(msku.getRulesThatChangedObject()).containsExactly(
            "M5. ЕСЛИ (MSKU = Regular или In/Out) И (все SSKU <> Active или Pending) ТО (MSKU = End_of_life)"
        );
    }
}
