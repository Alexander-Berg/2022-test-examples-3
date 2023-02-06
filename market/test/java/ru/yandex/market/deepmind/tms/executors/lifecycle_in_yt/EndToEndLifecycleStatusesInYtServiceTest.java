package ru.yandex.market.deepmind.tms.executors.lifecycle_in_yt;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.deepmind.common.db.jooq.generated.mbo_category.enums.OfferAcceptanceStatus;
import ru.yandex.market.deepmind.common.db.jooq.generated.mbo_category.enums.OfferAvailability;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.enums.MskuStatusValue;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.MskuStatus;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.Season;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.SskuStatus;
import ru.yandex.market.deepmind.common.repository.service_offer.ServiceOfferReplica;
import ru.yandex.market.deepmind.common.repository.service_offer.ServiceOfferReplicaFilter;
import ru.yandex.market.deepmind.common.services.rule_engine.RuleEngineService;
import ru.yandex.market.mboc.common.utils.SecurityUtil;

import static ru.yandex.market.deepmind.common.db.jooq.generated.mbo_category.enums.OfferAvailability.ACTIVE;
import static ru.yandex.market.deepmind.common.db.jooq.generated.mbo_category.enums.OfferAvailability.DELISTED;
import static ru.yandex.market.deepmind.common.db.jooq.generated.mbo_category.enums.OfferAvailability.INACTIVE;
import static ru.yandex.market.deepmind.common.db.jooq.generated.mbo_category.enums.OfferAvailability.INACTIVE_TMP;
import static ru.yandex.market.deepmind.common.db.jooq.generated.mbo_category.enums.OfferAvailability.PENDING;
import static ru.yandex.market.deepmind.common.db.jooq.generated.msku.enums.MskuStatusValue.ARCHIVE;
import static ru.yandex.market.deepmind.common.db.jooq.generated.msku.enums.MskuStatusValue.EMPTY;
import static ru.yandex.market.deepmind.common.db.jooq.generated.msku.enums.MskuStatusValue.END_OF_LIFE;
import static ru.yandex.market.deepmind.common.db.jooq.generated.msku.enums.MskuStatusValue.IN_OUT;
import static ru.yandex.market.deepmind.common.db.jooq.generated.msku.enums.MskuStatusValue.PRE_NPD;
import static ru.yandex.market.deepmind.common.db.jooq.generated.msku.enums.MskuStatusValue.REGULAR;
import static ru.yandex.market.deepmind.common.db.jooq.generated.msku.enums.MskuStatusValue.SEASONAL;
import static ru.yandex.market.deepmind.common.repository.logicstics.warehouse.DeepmindWarehouseRepository.CROSSDOCK_SOFINO_ID;
import static ru.yandex.market.deepmind.common.repository.logicstics.warehouse.DeepmindWarehouseRepository
    .EKATERINBURG_ID;
import static ru.yandex.market.deepmind.common.repository.logicstics.warehouse.DeepmindWarehouseRepository.MARSHRUT_ID;
import static ru.yandex.market.deepmind.common.repository.logicstics.warehouse.DeepmindWarehouseRepository.ROSTOV_ID;
import static ru.yandex.market.deepmind.common.repository.logicstics.warehouse.DeepmindWarehouseRepository.SOFINO_ID;
import static ru.yandex.market.deepmind.common.repository.logicstics.warehouse.DeepmindWarehouseRepository.TOMILINO_ID;
import static ru.yandex.market.deepmind.common.services.DeepmindConstants.AUTO_STATUS_ROBOT;
import static ru.yandex.market.deepmind.common.services.DeepmindConstants.MR_PIPELINE_VERSION;

/**
 * End-to-end tests of {@link ru.yandex.market.deepmind.common.services.lifecycle.LifecycleStatusesInYtService}.
 */
public class EndToEndLifecycleStatusesInYtServiceTest extends BaseLifecycleStatusesInYtServiceTest {

    @Before
    public void setUp() throws Exception {
        super.setUp();

        // set properties
        deepmindStorageKeyValueService.putValue(MR_PIPELINE_VERSION, RuleEngineService.V6);
    }

    @Test
    public void runWithOneMskuStatus() {
        mskuStatusRepository.save(mskuStatusRepository.findById(404040L).get()
            .setMskuStatus(MskuStatusValue.NPD)
            .setNpdStartDate(LocalDate.now().minusDays(1))
            .setNpdFinishDate(LocalDate.now().minusDays(1)));

        doubleRun();

        var mskuStatus = mskuStatusRepository.findById(404040L);
        Assertions.assertThat(mskuStatus).get()
            .extracting(MskuStatus::getMskuStatus)
            .isEqualTo(MskuStatusValue.REGULAR);
    }

    @Test
    public void runFor1P() {
        mskuStatusRepository.save(mskuStatusRepository.findById(505050L).get()
            .setMskuStatus(MskuStatusValue.NPD)
            .setNpdStartDate(LocalDate.now().minusDays(1))
            .setNpdFinishDate(LocalDate.now().minusDays(1)));

        addPurchasePrice(77, "sku5", 100);

        doubleRun();

        var mskuStatus = mskuStatusRepository.findById(505050L);
        Assertions.assertThat(mskuStatus).get()
            .extracting(MskuStatus::getMskuStatus)
            .isEqualTo(MskuStatusValue.REGULAR);
    }

    @Test
    public void testSeasonChange() {
        seasonRepository.save(new Season().setId(1L).setName("test season"));
        mskuStatusRepository.save(mskuStatusRepository.findById(404040L).get()
            .setMskuStatus(MskuStatusValue.SEASONAL)
            .setSeasonId(1L));

        doubleRun();

        var mskuStatus = mskuStatusRepository.findById(404040L);
        Assertions.assertThat(mskuStatus).get()
            .extracting(MskuStatus::getMskuStatus)
            .isEqualTo(MskuStatusValue.REGULAR);
    }

    /**
     * Тестируем корректный маппинг 1P продаж и правило A2.
     */
    @Test
    public void testSales() {
        sskuStatusRepository.save(
            sskuStatusRepository.findByKey(77, "sku5").get()
                .setAvailability(ACTIVE)
                .setStatusStartAt(Instant.now().minus(70, ChronoUnit.DAYS)),
            sskuStatusRepository.findByKey(60, "sku4").get()
                .setAvailability(ACTIVE)
                .setStatusStartAt(Instant.now().minus(100, ChronoUnit.DAYS))
        );

        mskuStatusRepository.save(mskuStatusRepository.findById(505050L).get().setMskuStatus(MskuStatusValue.REGULAR));
        mskuStatusRepository.save(mskuStatusRepository.findById(404040L).get().setMskuStatus(MskuStatusValue.REGULAR));

        addSalesWithWarehouse(77, "sku5", TOMILINO_ID, LocalDateTime.now().minusDays(70));
        addSalesWithWarehouse(77, "sku5", EKATERINBURG_ID, LocalDateTime.now().minusDays(80));
        addSalesWithWarehouse(77, "sku5", ROSTOV_ID, LocalDateTime.now().minusDays(80));
        addSalesWithWarehouse(60, "sku4", EKATERINBURG_ID, LocalDateTime.now().minusDays(100));
        addPurchasePrice(77, "sku5", 100);

        mskuStockRepository.insertBatch(
            stock(77, "sku5", EKATERINBURG_ID, 1),
            stock(77, "sku5", MARSHRUT_ID, 0),
            stock(60, "sku4", MARSHRUT_ID, 6)
        );

        doubleRun();

        var sku5 = sskuStatusRepository.findByKey(77, "sku5").get();
        Assertions.assertThat(sku5)
            .extracting(SskuStatus::getAvailability, SskuStatus::getComment)
            .containsExactly(
                INACTIVE,
                "A2. ЕСЛИ (SSKU = Active) И (MSKU <> Seasonal) И (Сток > 0) " +
                    "И (Продажи = 0 больше 60 дней от даты перехода в статус Active) ТО (SSKU = Inactive)"
            );

        // для 3p нет такого правила
        var sku4 = sskuStatusRepository.findByKey(60, "sku4").get();
        Assertions.assertThat(sku4)
            .extracting(SskuStatus::getAvailability, SskuStatus::getComment)
            .containsExactly(
                OfferAvailability.ACTIVE,
                null
            );
    }

    /**
     * Если сейчас идет сезон, то ssku будет seasonal.
     * Если пользователь не хочет, чтобы ssku закупали и сделает ее inactive,
     * то мы не должны перевести обратно в ACTIVE.
     */
    @Test
    public void seasonalSskuMakeInactive() {
        // Создаем сезон с периодом от вчера до завтра включительно
        var seasonId = seasonRepository.saveWithPeriodsAndReturn(
            season(LocalDate.now().minusDays(1), LocalDate.now().plusDays(1))
        ).getId();

        // помечаем категорию как сезонную
        categorySettingsRepository.save(categorySettings(22L, seasonId));

        // помечаем msku как сезонный
        var mskuStatus = mskuStatusRepository.findById(505050L).get();
        mskuStatusRepository.save(mskuStatus.setMskuStatus(MskuStatusValue.SEASONAL).setSeasonId(seasonId));

        // помечаем ssku как INACTIVE от имени пользователя
        var sskuStatus = sskuStatusRepository.findByKey(77, "sku5").get();
        sskuStatusRepository.save(sskuStatus
            .setAvailability(INACTIVE)
            .setModifiedByUser(true)
            .setComment("Перевод пользователем в INACTIVE"));

        addPurchasePrice(77, "sku5", 100);

        doubleRun();

        // дополнительно проверяем комментарий, как знак того, что статус не поменялся руками
        var sku5 = sskuStatusRepository.findByKey(77, "sku5").get();
        Assertions.assertThat(sku5)
            .extracting(SskuStatus::getAvailability, SskuStatus::getComment)
            .containsExactly(INACTIVE, "Перевод пользователем в INACTIVE");
    }

    /**
     * Если сейчас идет сезон, то ssku будет seasonal. И статус перейдет из INACTIVE -> ACTIVE
     */
    @Test
    public void seasonalSskuWillBeActive() {
        // Создаем сезон с периодом от вчера до завтра включительно
        var seasonId = seasonRepository.saveWithPeriodsAndReturn(
            season(LocalDate.now().minusDays(1), LocalDate.now().plusDays(1))
        ).getId();

        // помечаем категорию как сезонную
        categorySettingsRepository.save(categorySettings(11L, seasonId));

        // помечаем msku как сезонный
        var mskuStatus = mskuStatusRepository.findById(505050L).get();
        mskuStatusRepository.save(mskuStatus.setMskuStatus(MskuStatusValue.SEASONAL).setSeasonId(seasonId));
        // помечаем ssku как INACTIVE от робота
        sskuStatusRepository.save(sskuStatusRepository.findByKey(77, "sku5").get()
            .setAvailability(INACTIVE)
            .setStatusStartAt(Instant.now().minus(70, ChronoUnit.DAYS))
            .setModifiedByUser(false)
            .setComment("Перевод роботом в INACTIVE")
        );

        addPurchasePrice(77, "sku5", 100);

        doubleRun();

        var sku5 = sskuStatusRepository.findByKey(77, "sku5").get();
        Assertions.assertThat(sku5)
            .extracting(SskuStatus::getAvailability, SskuStatus::getComment)
            .containsExactly(PENDING,
                "I4. ЕСЛИ (SSKU = Inactive)" +
                    " И (MSKU = Seasonal)" +
                    " И (текущая дата >= даты начала периода Seasonal)" +
                    " И (переход в статус Inactive сделан автоматически системой)" +
                    " И (переход в статус был осуществлен вне периода) " +
                    "ТО (SSKU = Pending)");
    }

    /**
     * Если ssku & msku уже архивные, а пользователю надо закупить товар, то он может это сделать
     * переводом ssku в статус Inactive. Система не откатит все назад.
     * https://st.yandex-team.ru/DEEPMIND-262#6067263da7460163c0ae9661
     */
    @Test
    public void returnFromArchive() {
        // от имени робота
        SecurityUtil.wrapWithLogin(AUTO_STATUS_ROBOT, () -> {
            var sku = sskuStatusRepository.findByKey(77, "sku5").get();
            sskuStatusRepository.save(sku.setAvailability(OfferAvailability.DELISTED));

            var msku = mskuStatusRepository.findById(505050L).get();
            mskuStatusRepository.save(msku.setMskuStatus(MskuStatusValue.ARCHIVE));
        });

        addPurchasePrice(77, "sku5", 100);

        // прогоняем в первый раз, ничего не должно поменяться
        doubleRun();

        // меняем статус от имени пользователя
        SecurityUtil.wrapWithLogin("unit-test-user", () -> {
            var sku = sskuStatusRepository.findByKey(77, "sku5").get();
            sskuStatusRepository.save(sku.setAvailability(INACTIVE));
        });

        // прогоняем во второй
        doubleRun();

        Assertions.assertThat(sskuStatusRepository.findByKey(77, "sku5"))
            .get().extracting(SskuStatus::getAvailability, SskuStatus::getComment)
            .containsExactly(INACTIVE, null);
        Assertions.assertThat(mskuStatusRepository.findById(505050L))
            .get().extracting(MskuStatus::getMskuStatus, MskuStatus::getComment)
            .containsExactly(MskuStatusValue.END_OF_LIFE,
                "M8. ЕСЛИ (MSKU = Archive) И (SSKU <> Delisted) ТО (MSKU = End_of_life)");
    }

    @Test
    public void fromActiveWithoutStocksAndSalesToDelisted() {
        sskuStatusRepository.save(sskuStatusRepository.findByKey(77, "sku5").get()
            .setAvailability(ACTIVE)
            .setStatusStartAt(Instant.now().minus(70, ChronoUnit.DAYS))
        );
        sskuStatusRepository.save(sskuStatusRepository.findByKey(60, "sku4").get()
            .setAvailability(ACTIVE)
            .setStatusStartAt(Instant.now().minus(100, ChronoUnit.DAYS))
        );

        mskuStatusRepository.save(mskuStatusRepository.findById(505050L).get().setMskuStatus(MskuStatusValue.REGULAR));
        mskuStatusRepository.save(mskuStatusRepository.findById(404040L).get().setMskuStatus(MskuStatusValue.REGULAR));

        addPurchasePrice(77, "sku5", 100);

        doubleRun();

        var sku5 = sskuStatusRepository.findByKey(77, "sku5").get();
        Assertions.assertThat(sku5)
            .extracting(SskuStatus::getAvailability, SskuStatus::getComment)
            .containsExactly(
                DELISTED,
                "A1. ЕСЛИ (SSKU = Active) И (MSKU <> Seasonal) " +
                    "И (Сток = 0 больше 60 дней от даты перехода в статус Active) ТО (SSKU = Delisted)"
            );

        var mskuStatus505050 = mskuStatusRepository.findById(505050L).get();
        Assertions.assertThat(mskuStatus505050.getMskuStatus()).isEqualTo(ARCHIVE);
        Assertions.assertThat(mskuStatus505050.getComment()).isEqualTo(
            "M5. ЕСЛИ (MSKU = Regular или In/Out) И (все SSKU <> Active или Pending) ТО (MSKU = End_of_life)\n" +
                "M6. ЕСЛИ (MSKU = End_of_life) И (все SSKU = Delisted) ТО (MSKU = Archive)"
        );

        // для 3p нет такого правила
        var sku4 = sskuStatusRepository.findByKey(60, "sku4").get();
        Assertions.assertThat(sku4)
            .extracting(SskuStatus::getAvailability, SskuStatus::getComment)
            .containsExactly(
                OfferAvailability.ACTIVE,
                null
            );

        var mskuStatus404040 = mskuStatusRepository.findById(404040L).get();
        Assertions.assertThat(mskuStatus404040.getMskuStatus()).isEqualTo(REGULAR);
        Assertions.assertThat(mskuStatus404040.getComment()).isNullOrEmpty();
    }

    /**
     * Если год назад статус INACTIVE было переведено во время сезона -
     * значит тогда (год назад) робот его перевел по правилу из-за отсутствия продаж.
     * <p>
     * Т.е. сезонный товар не продавался в прошлый сезон и значит в этот сезон тоже не будет продаваться.
     * Значит не надо его переводить в Active.
     * https://st.yandex-team.ru/DEEPMIND-262#606b0d55d5b43268682a33eb
     */
    @Test
    public void fromInactiveSeasonInPeriodButYearAgoDoNothing() {
        var seasonId = seasonRepository.saveWithPeriodsAndReturn(
            season(LocalDate.now().minusDays(10), LocalDate.now().plusDays(10))
        ).getId();

        // помечаем категорию как сезонную
        categorySettingsRepository.save(categorySettings(22L, seasonId));

        // помечаем msku как сезонный
        var mskuStatus = mskuStatusRepository.findById(505050L).get();
        mskuStatusRepository.save(mskuStatus.setMskuStatus(MskuStatusValue.SEASONAL).setSeasonId(seasonId));

        sskuStatusRepository.save(sskuStatusRepository.findByKey(77, "sku5").get()
            .setAvailability(INACTIVE)
            .setModifiedByUser(false)
            .setStatusStartAt(Instant.now().minus(365, ChronoUnit.DAYS))
        );

        addPurchasePrice(77, "sku5", 100);

        doubleRun();

        var sku5 = sskuStatusRepository.findByKey(77, "sku5").get();
        Assertions.assertThat(sku5.getAvailability()).isEqualTo(INACTIVE);
        Assertions.assertThat(sku5.getComment()).isNullOrEmpty();

        var msku505050 = mskuStatusRepository.findById(505050L).get();
        Assertions.assertThat(msku505050.getMskuStatus()).isEqualTo(SEASONAL);
        Assertions.assertThat(msku505050.getComment()).isNullOrEmpty();
    }

    /**
     * Тоже самое только несколько лет назад.
     * https://st.yandex-team.ru/DEEPMIND-262#606b0d55d5b43268682a33eb
     */
    @Test
    public void fromInactiveSeasonInPeriodButYearsAgoDoNothing() {
        var seasonId = seasonRepository.saveWithPeriodsAndReturn(
            season(LocalDate.now().minusDays(10), LocalDate.now().plusDays(10))
        ).getId();

        // помечаем категорию как сезонную
        categorySettingsRepository.save(categorySettings(22L, seasonId));

        // помечаем msku как сезонный
        var mskuStatus = mskuStatusRepository.findById(505050L).get();
        mskuStatusRepository.save(mskuStatus.setMskuStatus(MskuStatusValue.SEASONAL).setSeasonId(seasonId));

        sskuStatusRepository.save(sskuStatusRepository.findByKey(77, "sku5").get()
            .setAvailability(INACTIVE)
            .setModifiedByUser(false)
            .setStatusStartAt(Instant.now().minus(3 * 365, ChronoUnit.DAYS))
        );

        addPurchasePrice(77, "sku5", 100);

        doubleRun();

        var sku5 = sskuStatusRepository.findByKey(77, "sku5").get();
        Assertions.assertThat(sku5.getAvailability()).isEqualTo(INACTIVE);
        Assertions.assertThat(sku5.getComment()).isNullOrEmpty();

        var msku505050 = mskuStatusRepository.findById(505050L).get();
        Assertions.assertThat(msku505050.getMskuStatus()).isEqualTo(SEASONAL);
        Assertions.assertThat(msku505050.getComment()).isNullOrEmpty();
    }

    /**
     * Тоже самое только в прошлый период, если их 2.
     * https://st.yandex-team.ru/DEEPMIND-262#606b0d55d5b43268682a33eb
     */
    @Test
    public void fromInactiveSeasonInPrevPeriodDoNothing() {
        var seasonId = seasonRepository.saveWithPeriodsAndReturn(season(
            LocalDate.now().minusDays(100), LocalDate.now().minusDays(90),
            LocalDate.now().minusDays(10), LocalDate.now().plusDays(10)
        )).getId();

        // помечаем категорию как сезонную
        categorySettingsRepository.save(categorySettings(22L, seasonId));

        // помечаем msku как сезонный
        var mskuStatus = mskuStatusRepository.findById(505050L).get();
        mskuStatusRepository.save(mskuStatus.setMskuStatus(MskuStatusValue.SEASONAL).setSeasonId(seasonId));

        sskuStatusRepository.save(sskuStatusRepository.findByKey(77, "sku5").get()
            .setAvailability(INACTIVE)
            .setModifiedByUser(false)
            .setStatusStartAt(Instant.now().minus(92, ChronoUnit.DAYS))
        );

        addPurchasePrice(77, "sku5", 100);

        doubleRun();

        var sku5 = sskuStatusRepository.findByKey(77, "sku5").get();
        Assertions.assertThat(sku5.getAvailability()).isEqualTo(INACTIVE);
        Assertions.assertThat(sku5.getComment()).isNullOrEmpty();

        var msku505050 = mskuStatusRepository.findById(505050L).get();
        Assertions.assertThat(msku505050.getMskuStatus()).isEqualTo(SEASONAL);
        Assertions.assertThat(msku505050.getComment()).isNullOrEmpty();
    }

    @Test
    public void removeAllSskuShouldReturnToEmpty() {
        mskuStatusRepository.save(mskuStatusRepository.findById(404040L).get()
            .setMskuStatus(REGULAR)
            .setComment("Regular status"));

        var offersToDelete = serviceOfferReplicaRepository.findOffers(new ServiceOfferReplicaFilter()
            .setApprovedSkuIds(List.of(404040L)));
        serviceOfferReplicaRepository.deleteByEntities(offersToDelete);

        doubleRun();

        var mskuStatus = mskuStatusRepository.findById(404040L).get();
        Assertions.assertThat(mskuStatus.getMskuStatus()).isEqualTo(EMPTY);
        Assertions.assertThat(mskuStatus.getComment()).contains(
            "M9. ЕСЛИ (MSKU <> EMPTY) И (к MSKU не привязан ни один SSKU) ТО (MSKU = EMPTY)"
        );
    }

    @Test
    public void fromActiveWithPlannedTimeToInactiveTmp() {
        // помечаем ssku как INACTIVE_TMP
        var sskuStatus = sskuStatusRepository.findByKey(77, "sku5").get();
        var plannedStartAt = Instant.now().minus(1, ChronoUnit.SECONDS);
        var plannedFinishAt = Instant.now().plus(1, ChronoUnit.DAYS);
        sskuStatusRepository.save(sskuStatus
            .setPlannedStartAt(plannedStartAt)
            .setPlannedFinishAt(plannedFinishAt)
            .setPlannedComment("Хочу перевести в INACTIVE_TMP"));

        addPurchasePrice(77, "sku5", 100);

        doubleRun();

        // дополнительно проверяем комментарий, на валидность правила
        var sku5 = sskuStatusRepository.findByKey(77, "sku5").get();
        Assertions.assertThat(sku5)
            .extracting(SskuStatus::getAvailability, SskuStatus::getComment)
            .containsExactly(
                INACTIVE_TMP,
                "Хочу перевести в INACTIVE_TMP\n\n" +
                    "IT1. ЕСЛИ (Указан период для Inactive_tmp) И (Текущая дата >= даты начала периода Inactive_tmp) " +
                    "ТО (SSKU = Inactive_tmp)"
            );
        // проверяем, что время примерно равно now()
        Assertions.assertThat(sku5.getStatusStartAt()).isAfter(plannedStartAt);
        Assertions.assertThat(sku5.getStatusFinishAt()).isEqualTo(plannedFinishAt);
        Assertions.assertThat(sku5.getPlannedStartAt()).isNull();
        Assertions.assertThat(sku5.getPlannedFinishAt()).isNull();
        Assertions.assertThat(sku5.getPlannedComment()).isNull();
        Assertions.assertThat(sku5.getPreviousAvailability()).isEqualTo(ACTIVE);
    }

    @Test
    public void fromInactiveTmpToPending() {
        // помечаем ssku как INACTIVE_TMP
        var sskuStatus = sskuStatusRepository.findByKey(77, "sku5").get();
        sskuStatusRepository.save(sskuStatus
            .setAvailability(INACTIVE_TMP)
            .setStatusFinishAt(Instant.now())
            .setPreviousAvailability(ACTIVE)
            .setComment("Перевод пользователем в INACTIVE_TMP"));

        addPurchasePrice(77, "sku5", 100);

        doubleRun();

        // дополнительно проверяем комментарий, на валидность правила
        var sku5 = sskuStatusRepository.findByKey(77, "sku5").get();
        Assertions.assertThat(sku5)
            .extracting(SskuStatus::getAvailability, SskuStatus::getComment)
            .containsExactly(
                PENDING,
                "IT2. ЕСЛИ (SSKU = Inactive_tmp)" +
                    " И (Текущая дата > даты окончания периода Inactive_tmp)" +
                    " И (прошлый статус <> Delisted или Inactive)" +
                    " ТО (SSKU = Pending)"
            );
        // проверяем, что время примерно равно now()
        Assertions.assertThat(sku5.getStatusStartAt()).isAfter(Instant.now().minusSeconds(1000));
        Assertions.assertThat(sku5.getStatusFinishAt()).isNull();
        Assertions.assertThat(sku5.getPreviousAvailability()).isEqualTo(INACTIVE_TMP);
    }

    @Test
    public void inactiveTmpStayTheSame() {
        // помечаем ssku как INACTIVE_TMP
        var sskuStatus = sskuStatusRepository.findByKey(77, "sku5").get();
        sskuStatusRepository.save(sskuStatus
            .setAvailability(INACTIVE_TMP)
            .setHasNoPurchasePrice(false)
            .setStatusFinishAt(null)
            .setPreviousAvailability(DELISTED)
            .setComment("some comment"));

        doubleRun();

        var sku5 = sskuStatusRepository.findByKey(77, "sku5").get();
        // проверяем, что все осталось прежним
        Assertions.assertThat(sku5.getStatusFinishAt()).isNull();
        Assertions.assertThat(sku5.getAvailability()).isEqualTo(INACTIVE_TMP);
        Assertions.assertThat(sku5.getPreviousAvailability()).isEqualTo(DELISTED);
        Assertions.assertThat(sku5.getComment()).isEqualTo("some comment");

        // добавим закупочную цену и проверим что IT4 не отработает из-за HasNoPurchasePrice = false
        addPurchasePrice(77, "sku5", 100);
        doubleRun();

        sku5 = sskuStatusRepository.findByKey(77, "sku5").get();
        // проверяем, что все осталось прежним
        Assertions.assertThat(sku5.getStatusFinishAt()).isNull();
        Assertions.assertThat(sku5.getAvailability()).isEqualTo(INACTIVE_TMP);
        Assertions.assertThat(sku5.getPreviousAvailability()).isEqualTo(DELISTED);
        Assertions.assertThat(sku5.getComment()).isEqualTo("some comment");

        // поменяем предыдущий статус, так как он тоже фигурирует в правилах (IT2, IT3)
        sskuStatusRepository.save(sku5.setPreviousAvailability(ACTIVE));
        doubleRun();

        sku5 = sskuStatusRepository.findByKey(77, "sku5").get();
        // проверяем, что все осталось прежним (кроме previous availability)
        Assertions.assertThat(sku5.getStatusFinishAt()).isNull();
        Assertions.assertThat(sku5.getAvailability()).isEqualTo(INACTIVE_TMP);
        Assertions.assertThat(sku5.getPreviousAvailability()).isEqualTo(ACTIVE);
        Assertions.assertThat(sku5.getComment()).isEqualTo("some comment");
    }

    @Test
    public void fromInactiveTmpToDelistedIfPreviousInactive() {
        // помечаем ssku как INACTIVE_TMP
        sskuStatusRepository.save(sskuStatusRepository.findByKey(77, "sku5").get()
            .setAvailability(INACTIVE_TMP)
            .setStatusFinishAt(Instant.now())
            .setPreviousAvailability(DELISTED)
            .setComment("Перевод пользователем в INACTIVE_TMP")
        );

        addPurchasePrice(77, "sku5", 100);

        doubleRun();

        // дополнительно проверяем комментарий, на валидность правила
        var sku5 = sskuStatusRepository.findByKey(77, "sku5").get();
        Assertions.assertThat(sku5)
            .extracting(SskuStatus::getAvailability, SskuStatus::getComment)
            .containsExactly(
                INACTIVE,
                "IT3. ЕСЛИ (SSKU = Inactive_tmp)" +
                    " И (Текущая дата > даты окончания периода Inactive_tmp)" +
                    " И (прошлый статус = Delisted или Inactive)" +
                    " ТО (SSKU = Inactive)"
            );
        // проверяем, что время примерно равно now()
        Assertions.assertThat(sku5.getStatusStartAt()).isAfter(Instant.now().minusSeconds(1000));
        Assertions.assertThat(sku5.getStatusFinishAt()).isNull();
        Assertions.assertThat(sku5.getPreviousAvailability()).isEqualTo(INACTIVE_TMP);
    }

    @Test
    public void fromInactiveTmpToPendingIfPreviousIsNull() {
        // помечаем ssku как INACTIVE_TMP
        var sskuStatus = sskuStatusRepository.findByKey(77, "sku5").get();
        sskuStatusRepository.save(sskuStatus
            .setAvailability(INACTIVE_TMP)
            .setStatusFinishAt(Instant.now())
            .setComment("Перевод пользователем в INACTIVE_TMP"));

        addPurchasePrice(77, "sku5", 100);

        doubleRun();

        // дополнительно проверяем комментарий, на валидность правила
        var sku5 = sskuStatusRepository.findByKey(77, "sku5").get();
        Assertions.assertThat(sku5)
            .extracting(SskuStatus::getAvailability, SskuStatus::getComment)
            .containsExactly(
                PENDING,
                "IT2. ЕСЛИ (SSKU = Inactive_tmp)" +
                    " И (Текущая дата > даты окончания периода Inactive_tmp)" +
                    " И (прошлый статус <> Delisted или Inactive)" +
                    " ТО (SSKU = Pending)"
            );
        // проверяем, что время примерно равно now()
        Assertions.assertThat(sku5.getStatusStartAt()).isAfter(Instant.now().minusSeconds(1000));
        Assertions.assertThat(sku5.getStatusFinishAt()).isNull();
        Assertions.assertThat(sku5.getPreviousAvailability()).isEqualTo(INACTIVE_TMP);
    }

    @Test
    public void fromInactiveTmpDoNothing() {
        // помечаем ssku как INACTIVE_TMP
        sskuStatusRepository.save(sskuStatusRepository.findByKey(77, "sku5").get()
            .setAvailability(INACTIVE_TMP)
            .setStatusStartAt(Instant.now().minus(92, ChronoUnit.DAYS))
            .setStatusFinishAt(Instant.now().plus(1, ChronoUnit.DAYS))
            .setPreviousAvailability(PENDING)
            .setComment("Перевод пользователем в INACTIVE_TMP")
        );

        addPurchasePrice(77, "sku5", 100);

        doubleRun();

        // дополнительно проверяем комментарий, на валидность правила
        var sku5 = sskuStatusRepository.findByKey(77, "sku5").get();
        Assertions.assertThat(sku5)
            .extracting(SskuStatus::getAvailability, SskuStatus::getComment)
            .containsExactly(
                INACTIVE_TMP,
                "Перевод пользователем в INACTIVE_TMP"
            );
    }

    @Test
    public void activeWithoutPurchasePriceWillBeInactiveTmp() {
        mskuStockRepository.insertBatch(
            stock(77, "sku5", EKATERINBURG_ID, 1)
        );

        doubleRun();

        // дополнительно проверяем комментарий, на валидность правила
        var sku5 = sskuStatusRepository.findByKey(77, "sku5").get();
        Assertions.assertThat(sku5.getAvailability()).isEqualTo(INACTIVE_TMP);
        Assertions.assertThat(sku5.getComment()).contains(
            "AP5. ЕСЛИ (SSKU = Active или Pending) И (нет закупочной цены)" +
                " ТО (SSKU = Inactive_tmp) И (Причина = Нет закупочной цены) И (Дата окончания = null)"
        );
        Assertions.assertThat(sku5.getHasNoPurchasePrice()).isEqualTo(true);

        // add price
        addPurchasePrice(77, "sku5", 666);

        doubleRun();

        sku5 = sskuStatusRepository.findByKey(77, "sku5").get();
        Assertions.assertThat(sku5)
            .extracting(SskuStatus::getAvailability, SskuStatus::getComment, SskuStatus::getHasNoPurchasePrice)
            .containsExactly(
                ACTIVE,
                "IT4. ЕСЛИ (SSKU = Inactive_tmp) И (Причина = Нет закупочной цены) И (есть закупочная цена) " +
                    "ТО (SSKU = Pending)\n" +
                    "P1. ЕСЛИ (SSKU = PENDING) и (Сток > 0) ТО (SSKU = Active)",
                false
            );
    }

    @Test
    public void activeWithoutValidContractBeInactiveTmp() {
        mskuStockRepository.insertBatch(
            stock(77, "sku5", EKATERINBURG_ID, 1)
        );
        addPurchasePrice(77, "sku5", 100);
        addInvalidContractSupplier(465852);
        doubleRun();

        // дополнительно проверяем комментарий, на валидность правила
        var sku5 = sskuStatusRepository.findByKey(77, "sku5").get();
        Assertions.assertThat(sku5.getAvailability()).isEqualTo(INACTIVE_TMP);
        Assertions.assertThat(sku5.getComment()).contains(
            "AP7. ЕСЛИ (SSKU = Active или Pending) И (нет действующего договора) ТО" +
                " (SSKU = Inactive_tmp) И (Причина = Нет действующего договора) И (Дата окончания = null)"
        );
        Assertions.assertThat(sku5.getHasNoValidContract()).isEqualTo(true);

        // remove invalid contract
        removeInvalidContractSupplier(465852);
        // обход правила P1
        mskuStockRepository.deleteAll();
        mskuStockRepository.insertBatch(
            stock(77, "sku5", EKATERINBURG_ID, 0)
        );

        doubleRun();

        sku5 = sskuStatusRepository.findByKey(77, "sku5").get();
        Assertions.assertThat(sku5)
            .extracting(SskuStatus::getAvailability, SskuStatus::getComment, SskuStatus::getHasNoValidContract)
            .containsExactly(
                PENDING,
                "IT5. ЕСЛИ (SSKU = Inactive_tmp) И (Причина = Нет действующего договора) И (есть действующий договор)" +
                    " ТО (SSKU = Pending)",
                false
            );
    }

    @Test
    public void noContractAndNoPruchasePriceRulesOrder() {
        mskuStockRepository.insertBatch(
            stock(77, "sku5", EKATERINBURG_ID, 1)
        );
        addInvalidContractSupplier(465852);
        doubleRun();

        // дополнительно проверяем комментарий, на валидность правила
        var sku5 = sskuStatusRepository.findByKey(77, "sku5").get();
        Assertions.assertThat(sku5.getAvailability()).isEqualTo(INACTIVE_TMP);
        Assertions.assertThat(sku5.getComment()).contains(
            "AP7. ЕСЛИ (SSKU = Active или Pending) И (нет действующего договора) ТО" +
                " (SSKU = Inactive_tmp) И (Причина = Нет действующего договора) И (Дата окончания = null)"
        );
        Assertions.assertThat(sku5.getHasNoValidContract()).isEqualTo(true);

        // remove invalid contract
        removeInvalidContractSupplier(465852);
        // обход правила P1
        mskuStockRepository.deleteAll();
        mskuStockRepository.insertBatch(
            stock(77, "sku5", EKATERINBURG_ID, 0)
        );

        doubleRun();

        sku5 = sskuStatusRepository.findByKey(77, "sku5").get();
        Assertions.assertThat(sku5)
            .extracting(SskuStatus::getAvailability, SskuStatus::getComment, SskuStatus::getHasNoValidContract,
                SskuStatus::getHasNoPurchasePrice)
            .containsExactly(
                INACTIVE_TMP,
                "Нет закупочной цены\n\nIT5. ЕСЛИ (SSKU = Inactive_tmp) И" +
                    " (Причина = Нет действующего договора) И (есть действующий договор)" +
                    " ТО (SSKU = Pending)\n" +
                    "AP5. ЕСЛИ (SSKU = Active или Pending) И (нет закупочной цены) ТО (SSKU = Inactive_tmp)" +
                    " И (Причина = Нет закупочной цены) И (Дата окончания = null)",
                false,
                true
            );

        addPurchasePrice(77, "sku5", 100);

        doubleRun();

        sku5 = sskuStatusRepository.findByKey(77, "sku5").get();
        Assertions.assertThat(sku5)
            .extracting(SskuStatus::getAvailability, SskuStatus::getComment, SskuStatus::getHasNoValidContract,
                SskuStatus::getHasNoPurchasePrice)
            .containsExactly(
                PENDING,
                "IT4. ЕСЛИ (SSKU = Inactive_tmp) И (Причина = Нет закупочной цены) И" +
                    " (есть закупочная цена) ТО (SSKU = Pending)",
                false,
                false
            );
    }

    @Test
    public void ebtCategoriesIgnoredByAP5() {
        mskuStockRepository.insertBatch(
            stock(102, "ebtSku", EKATERINBURG_ID, 1)
        );

        doubleRun();

        var ebtSku = sskuStatusRepository.findByKey(102, "ebtSku").get();
        Assertions.assertThat(ebtSku.getAvailability()).isEqualTo(ACTIVE);
    }

    /**
     * Если сезонный ssku находится в периоде, то ssku должен быть ACTIVE.
     * Но так как нет закупочной цены, то должен быть INACTIVE_TMP
     * Как только закупочная цена появляется, то ssku становится ACTIVE.
     */
    @Test
    public void seasonalOfferWithoutPurchasePriceWillBeInactiveTmpInsidePeriod() {
        // Создаем сезон с периодом от вчера до завтра включительно
        var seasonId = seasonRepository.saveWithPeriodsAndReturn(
            season(LocalDate.now().minusDays(1), LocalDate.now().plusDays(1))
        ).getId();

        // помечаем категорию как сезонную
        categorySettingsRepository.save(categorySettings(22L, seasonId));

        // помечаем msku как сезонный
        var mskuStatus = mskuStatusRepository.findById(505050L).get();
        mskuStatusRepository.save(mskuStatus.setMskuStatus(MskuStatusValue.SEASONAL).setSeasonId(seasonId));

        sskuStatusRepository.save(sskuStatusRepository.findByKey(77, "sku5").get()
            .setAvailability(INACTIVE)
            .setStatusStartAt(Instant.now().minus(4, ChronoUnit.DAYS))
        );

        doubleRun();

        var sku5 = sskuStatusRepository.findByKey(77, "sku5").get();
        Assertions.assertThat(sku5)
            .extracting(SskuStatus::getAvailability, SskuStatus::getComment)
            .containsExactly(
                INACTIVE_TMP,
                "Нет закупочной цены\n\n" +
                    "I4. ЕСЛИ (SSKU = Inactive)" +
                    " И (MSKU = Seasonal)" +
                    " И (текущая дата >= даты начала периода Seasonal)" +
                    " И (переход в статус Inactive сделан автоматически системой)" +
                    " И (переход в статус был осуществлен вне периода) " +
                    "ТО (SSKU = Pending)\n" +
                    "AP5. ЕСЛИ (SSKU = Active или Pending) И (нет закупочной цены) " +
                    "ТО (SSKU = Inactive_tmp) И (Причина = Нет закупочной цены) И (Дата окончания = null)");

        // добавляем закупочную цену
        addPurchasePrice(77, "sku5", 1);

        doubleRun();

        sku5 = sskuStatusRepository.findByKey(77, "sku5").get();
        Assertions.assertThat(sku5)
            .extracting(SskuStatus::getAvailability, SskuStatus::getComment)
            .containsExactly(
                PENDING,
                "IT4. ЕСЛИ (SSKU = Inactive_tmp) И (Причина = Нет закупочной цены) И (есть закупочная цена) " +
                    "ТО (SSKU = Pending)");

    }

    /**
     * Если сезонный ssku находится вне периода, то ssku должен быть INACTIVE.
     * И так как нет цены, то должен быть INACTIVE.
     * Как только закупочная цена появляется, то ssku остается INACTIVE.
     */
    @Test
    public void seasonalOfferWithoutPurchasePriceWillBeInactiveOutsidePeriod() {
        var seasonId = seasonRepository.saveWithPeriodsAndReturn(
            season(LocalDate.now().minusDays(5), LocalDate.now().minusDays(2))
        ).getId();

        // помечаем категорию как сезонную
        categorySettingsRepository.save(categorySettings(22L, seasonId));

        // помечаем msku как сезонный
        var mskuStatus = mskuStatusRepository.findById(505050L).get();
        mskuStatusRepository.save(mskuStatus.setMskuStatus(MskuStatusValue.SEASONAL).setSeasonId(seasonId));

        sskuStatusRepository.save(
            sskuStatusRepository.findByKey(77, "sku5").get()
                .setAvailability(INACTIVE)
                .setHasNoPurchasePrice(true) // помечаем флаг, так как нет закупочной цены
                .setComment("Custom comment")
        );

        doubleRun();

        var sku5 = sskuStatusRepository.findByKey(77, "sku5").get();
        Assertions.assertThat(sku5)
            .extracting(SskuStatus::getAvailability, SskuStatus::getComment)
            .containsExactly(INACTIVE, "Custom comment");

        // добавляем закупочную цену
        addPurchasePrice(77, "sku5", 1);

        doubleRun();

        sku5 = sskuStatusRepository.findByKey(77, "sku5").get();
        Assertions.assertThat(sku5)
            .extracting(SskuStatus::getAvailability, SskuStatus::getComment)
            .containsExactly(
                INACTIVE,
                "Custom comment");
    }

    @Test
    public void p1PendingToActiveIfOnStocks() {
        var sku5 = sskuStatusRepository.findByKey(77, "sku5").get();
        sskuStatusRepository.save(sku5.setAvailability(PENDING));
        mskuStockRepository.insertBatch(stock(77, "sku5", EKATERINBURG_ID, 1));
        addPurchasePrice(77, "sku5", 1);

        doubleRun();

        sku5 = sskuStatusRepository.findByKey(77, "sku5").get();
        Assertions.assertThat(sku5)
            .extracting(SskuStatus::getAvailability, SskuStatus::getComment)
            .containsExactly(
                ACTIVE,
                "P1. ЕСЛИ (SSKU = PENDING) и (Сток > 0) ТО (SSKU = Active)"
            );
    }

    /**
     * Быстрые карточки не будут в выгрузках в YT, и, возможно, не будут в БД.
     * Но статусы нам надо создавать.
     */
    @Test
    public void noMskuInDataBaseAndYtTable() {
        var fastMskuId = 100500L;
        serviceOfferReplicaRepository.save(offer(60, "offer-1", fastMskuId));
        sskuStatusRepository.save(new SskuStatus()
            .setSupplierId(60).setShopSku("offer-1").setAvailability(PENDING)
        );

        doubleRun();

        var mskuStatus = mskuStatusRepository.findById(fastMskuId);
        Assertions.assertThat(mskuStatus).get()
            .extracting(MskuStatus::getMskuStatus)
            .isEqualTo(PRE_NPD);
    }

    /**
     * Если по статусу, у которого нет msku исчезли маппинги на ssku, то статус должен стать empty.
     */
    @Test
    public void noMskuToEmptyStatus() {
        var fastMskuId = 100500L;
        mskuStatusRepository.save(new MskuStatus()
            .setMarketSkuId(fastMskuId)
            .setMskuStatus(REGULAR)
        );

        doubleRun();

        var mskuStatus = mskuStatusRepository.findById(fastMskuId);
        Assertions.assertThat(mskuStatus).get()
            .extracting(MskuStatus::getMskuStatus)
            .isEqualTo(EMPTY);
    }

    /**
     * Тест проверяет, что когда все данные пропали, остались только стоки
     * и какая-то другая информацию, то алгоритм не сломается.
     */
    @Test
    public void testSskuAndMskuIsMissing() {
        addSalesWithWarehouse(84, "offer-84-1", EKATERINBURG_ID,
            LocalDateTime.now().minusDays(80));
        addSalesWithWarehouse(84, "offer-84-2", SOFINO_ID,
            LocalDateTime.now().minusDays(100));
        addSalesWithWarehouse(84, "offer-84-2", TOMILINO_ID,
            LocalDateTime.now().minusDays(1));

        mskuStockRepository.insertBatch(
            stock(84, "offer-84-1", EKATERINBURG_ID, 1),
            stock(84, "offer-84-2", MARSHRUT_ID, 0),
            stock(84, "offer-84-3", MARSHRUT_ID, 6)
        );

        doubleRun();

        Assertions.assertThat(sskuStatusRepository.findByKey(84, "offer-84-1")).isEmpty();
        Assertions.assertThat(sskuStatusRepository.findByKey(84, "offer-84-2")).isEmpty();
        Assertions.assertThat(sskuStatusRepository.findByKey(84, "offer-84-3")).isEmpty();
    }

    /**
     * Тест проверяет правила AP4, TP3 и M5
     * 1P и 3P SSKU переходят в INACTIVE при MSKU в IN_OUT после окончания периода
     * и MSKU переходит из IN_OUT в END_OF_LIFE
     */
    @Test
    public void mskuFromInoutToEndOfLifeWith1pAnd3pSsku() {
        sskuStatusRepository.save(
            sskuStatusRepository.findByKey(101, "sku100").get()
                .setAvailability(ACTIVE),
            sskuStatusRepository.findByKey(102, "sku100").get()
                .setAvailability(ACTIVE)
        );

        mskuStatusRepository.save(mskuStatusRepository.findById(10L).get()
            .setMskuStatus(IN_OUT)
            .setInoutStartDate(LocalDate.now().minusDays(2))
            .setInoutFinishDate(LocalDate.now().minusDays(1))
        );

        doubleRun();

        var sku101 = sskuStatusRepository.findByKey(101, "sku100").get();
        Assertions.assertThat(sku101)
            .extracting(SskuStatus::getAvailability, SskuStatus::getComment)
            .containsExactly(
                INACTIVE,
                "TP3. ЕСЛИ (SSKU = Active или Pending) И (MSKU = In/Out)" +
                    " И (текущая дата > даты окончания периода In/Out) " +
                    "ТО (SSKU = Inactive)"
            );

        var sku102 = sskuStatusRepository.findByKey(102, "sku100").get();
        Assertions.assertThat(sku102)
            .extracting(SskuStatus::getAvailability, SskuStatus::getComment)
            .containsExactly(
                INACTIVE,
                "AP4. ЕСЛИ (SSKU = Active или Pending) И (MSKU = In/Out)" +
                    " И (текущая дата > даты окончания периода In/Out) " +
                    "ТО (SSKU = Inactive)"
            );

        var mskuStatus = mskuStatusRepository.findById(10L);
        Assertions.assertThat(mskuStatus).get()
            .extracting(MskuStatus::getMskuStatus, MskuStatus::getComment)
            .containsExactly(
                END_OF_LIFE,
                "M5. ЕСЛИ (MSKU = Regular или In/Out) И (все SSKU <> Active или Pending) ТО (MSKU = End_of_life)"
            );
    }

    /**
     * Тест проверяет правила AP6
     * SSKU переходят из PENDING/ACTIVE в INACTIVE если тип MSKU = Corefix
     * и deadstock на любом складе МСК > 30 дней
     * и нет продаж на этом складе > 30 дней
     * и последние 30 дней статус ЖЦ не менялся
     */
    @Test
    public void ap6FromActiveToInactive() {
        sskuStatusRepository.save(
            sskuStatusRepository.findByKey(77, "sku5").get()
                .setAvailability(ACTIVE)
                .setStatusStartAt(Instant.now().minus(40, ChronoUnit.DAYS))
        );
        addSalesWithWarehouse(77, "sku5", 171L, LocalDateTime.now().minusDays(40));
        addCorefix(505050L);
        addDeadstock(77, "sku5", 171L, LocalDate.now().minusDays(40));
        addPurchasePrice(77, "sku5", 100); // чтобы избежать конфликт с AP5
        mskuStockRepository.insertBatch(
            stock(77, "sku5", TOMILINO_ID, 1) // чтобы избежать конфликт с P3
        );
        doubleRun();
        var sku5 = sskuStatusRepository.findByKey(77, "sku5").get();
        Assertions.assertThat(sku5)
            .extracting(SskuStatus::getAvailability, SskuStatus::getComment)
            .containsExactly(
                INACTIVE,
                "AP6. (SSKU = Active или Pending)" +
                    " И (тип MSKU = Corefix)" +
                    " И (deadstock на любом складе в МСК > 30 дней)" +
                    " И (нет продаж на этом складе > 30 дней)" +
                    " И (последние 30 дней статус ЖЦ не менялся)" +
                    " ТО (SSKU = Inactive)"
            );
    }

    /**
     * Тест проверяет правила AP6
     * SSKU переходят из PENDING/ACTIVE в INACTIVE если тип MSKU = Corefix
     * и deadstock на любом складе МСК > 30 дней
     * и вообще нет продаж товара
     * и последние 30 дней статус ЖЦ не менялся
     */
    @Test
    public void ap6FromActiveToInactiveWithoutSales() {
        sskuStatusRepository.save(sskuStatusRepository.findByKey(77, "sku5").get()
            .setAvailability(ACTIVE)
            .setStatusStartAt(Instant.now().minus(40, ChronoUnit.DAYS))
        );
        addCorefix(505050L);
        addDeadstock(77, "sku5", 171L, LocalDate.now().minusDays(40));
        addPurchasePrice(77, "sku5", 100); // чтобы избежать конфликт с AP5
        mskuStockRepository.insertBatch(
            stock(77, "sku5", TOMILINO_ID, 1) // чтобы избежать конфликт с P3
        );
        doubleRun();
        var sku5 = sskuStatusRepository.findByKey(77, "sku5").get();
        Assertions.assertThat(sku5)
            .extracting(SskuStatus::getAvailability, SskuStatus::getComment)
            .containsExactly(
                INACTIVE,
                "AP6. (SSKU = Active или Pending)" +
                    " И (тип MSKU = Corefix)" +
                    " И (deadstock на любом складе в МСК > 30 дней)" +
                    " И (нет продаж на этом складе > 30 дней)" +
                    " И (последние 30 дней статус ЖЦ не менялся)" +
                    " ТО (SSKU = Inactive)"
            );
    }

    /**
     * Тестируем то, что правила не работают со складами не из интерфейса Разума.
     * На примере правила P1.
     */
    @Test
    public void testWarehouseNotFromDeepmindUI() {
        var sku5 = sskuStatusRepository.findByKey(77, "sku5").get();
        sskuStatusRepository.save(sku5.setAvailability(PENDING));
        //CROSSDOCK_SOFINO_ID - fake warehouse
        mskuStockRepository.insertBatch(stock(77, "sku5", CROSSDOCK_SOFINO_ID, 1));
        addPurchasePrice(77, "sku5", 1);

        doubleRun();

        sku5 = sskuStatusRepository.findByKey(77, "sku5").get();
        Assertions.assertThat(sku5).extracting(SskuStatus::getAvailability).isEqualTo(PENDING);

        mskuStockRepository.deleteAll();
        mskuStockRepository.insertBatch(stock(77, "sku5", EKATERINBURG_ID, 1));
        addPurchasePrice(77, "sku5", 1);

        doubleRun();

        sku5 = sskuStatusRepository.findByKey(77, "sku5").get();
        Assertions.assertThat(sku5)
            .extracting(SskuStatus::getAvailability, SskuStatus::getComment)
            .containsExactly(
                ACTIVE,
                "P1. ЕСЛИ (SSKU = PENDING) и (Сток > 0) ТО (SSKU = Active)"
            );
    }

    private ServiceOfferReplica offer(
        int supplierId, String ssku, long mskuId) {
        var supplier = deepmindSupplierRepository.findById(supplierId).get();
        return new ServiceOfferReplica()
            .setBusinessId(supplierId)
            .setSupplierId(supplierId)
            .setShopSku(ssku)
            .setTitle("title " + ssku)
            .setCategoryId(33L)
            .setSeqId(0L)
            .setMskuId(mskuId)
            .setSupplierType(supplier.getSupplierType())
            .setModifiedTs(Instant.now())
            .setAcceptanceStatus(OfferAcceptanceStatus.OK);
    }
}
