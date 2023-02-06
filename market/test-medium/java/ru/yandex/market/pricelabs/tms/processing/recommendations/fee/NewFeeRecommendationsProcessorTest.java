package ru.yandex.market.pricelabs.tms.processing.recommendations.fee;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.pricelabs.misc.TimingUtils;
import ru.yandex.market.pricelabs.model.recommendation.FeeRecommendation;
import ru.yandex.market.pricelabs.model.recommendation.NewFeeRecommendationSource;
import ru.yandex.market.pricelabs.tms.processing.recommendations.AbstractRecommendationsProcessorConfiguration;

import static ru.yandex.market.pricelabs.processing.recommendations.constant.RecommendationsConstant.FEE_RECOMMENDATIONS;
import static ru.yandex.market.pricelabs.processing.recommendations.constant.RecommendationsConstant.FEE_RECOMMENDATIONS_DIFF;

@DisplayName("Тесты на процессор для загрузки рекомендаций ставок NewFeeRecommendationsProcessor")
public class NewFeeRecommendationsProcessorTest extends AbstractRecommendationsProcessorConfiguration {

    private static final int SIXTEEN_MINUTES = 16 * 60 * 1000;

    @Autowired
    private NewFeeRecommendationsProcessor processor;


    @BeforeEach
    protected void beforeEach() {
        clear();
        insertFeeRecommendationsFullSource();
        insertFeeRecommendationsDiffSource();
    }

    @AfterEach
    protected void afterEach() {
        clear();
    }

    protected void clear() {
        TimingUtils.resetTime();
        executors.feeRecommendationsExecutor1().clearSourceTable();
        executors.feeRecommendationsExecutor2().clearSourceTable();
        executors.feeRecommendationsExecutor3().clearSourceTable();

        executors.feeRecommendationsExecutor1().clearSourceDiffTable();
        executors.feeRecommendationsExecutor2().clearSourceDiffTable();
        executors.feeRecommendationsExecutor3().clearSourceDiffTable();

        executors.feeRecommendationsExecutor1().clearTargetTable();
        syncInfoRepository.clear();
    }

    @DisplayName("Первичный импорт в пустую таблицу прошел успешно")
    @Test
    protected void testImport_firstImport_fullSync() {
        TimingUtils.addTime(SIXTEEN_MINUTES);
        processor.sync();

        Instant now = getInstant();
        List<FeeRecommendation> actualRecommendationList = List.of(
                createFeeRecommendation(949799, "84520", false, 500, 100,
                        now, now),
                createFeeRecommendation(1367820, "1187405", false, 700, 200,
                        now, now),
                createFeeRecommendation(2666761, "7411831", true, 1000, 500,
                        now, now)
        );
        executors.feeRecommendationsExecutor1().verify(actualRecommendationList,
                recommendation -> recommendation.setComputation_datetime(now));

        String fullTableTimeStr = "2022-07-12T14:00:00.123";
        String diffTableTimeStr = "2022-07-12T14:00:00.123";
        assertFullAndDiffSyncInfo(now, now, fullTableTimeStr, diffTableTimeStr);
    }

    @DisplayName("Импорт диффа при нормальных условиях прошел успешно")
    @Test
    protected void testImport_importDiff_diffSync() {
        insertFeeRecommendationsTarget();

        Instant syncInfoInstant = getInstant();
        insertFullAndDiffSyncInfo(syncInfoInstant, "2022-07-12T12:00:00.123", "2022-07-12T13:00:00.123");

        TimingUtils.addTime(SIXTEEN_MINUTES);
        processor.sync();

        Instant now = getInstant();
        List<FeeRecommendation> actualRecommendationList = List.of(
                createFeeRecommendation(949799, "84520", false, 1023, 100,
                        now, now),
                createFeeRecommendation(2666761, "7411831", true, 0, 500,
                        now, now),
                createFeeRecommendation(9999999, "11", false, 351, 39,
                        Instant.parse("2022-07-04T00:00:00.00Z"), now)
        );
        executors.feeRecommendationsExecutor1().verify(actualRecommendationList,
                recommendation -> recommendation.setComputation_datetime(now));

        String fullTableTimeStr = "2022-07-12T12:00:00.123";
        String diffTableTimeStr = "2022-07-12T14:00:00.123";
        assertFullAndDiffSyncInfo(syncInfoInstant, now, fullTableTimeStr, diffTableTimeStr);
    }

    @DisplayName("Полный импорт за новый день прошел успешно")
    @Test
    protected void testImport_newDay_fullSync() {
        insertFeeRecommendationsTarget();

        Instant syncInfoInstant = getInstant();
        insertFullAndDiffSyncInfo(syncInfoInstant, "2022-07-11T12:00:00.123", "2022-07-12T13:00:00.123");

        TimingUtils.addTime(SIXTEEN_MINUTES);
        processor.sync();

        Instant now = getInstant();
        List<FeeRecommendation> actualRecommendationList = List.of(
                createFeeRecommendation(949799, "84520", false, 500, 100,
                        now, now),
                createFeeRecommendation(1367820, "1187405", false, 700, 200,
                        now, now),
                createFeeRecommendation(2666761, "7411831", true, 1000, 500,
                        now, now)
        );
        executors.feeRecommendationsExecutor1().verify(actualRecommendationList,
                recommendation -> recommendation.setComputation_datetime(now));

        String fullTableTimeStr = "2022-07-12T14:00:00.123";
        String diffTableTimeStr = "2022-07-12T14:00:00.123";
        assertFullAndDiffSyncInfo(now, now, fullTableTimeStr, diffTableTimeStr);
    }

    @DisplayName("Полный импорт при пропущенном диффе прошел успешно")
    @Test
    protected void testImport_missedDiff_fullSync() {
        insertFeeRecommendationsTarget();

        Instant syncInfoInstant = getInstant();
        insertFullAndDiffSyncInfo(syncInfoInstant, "2022-07-12T12:00:00.123", "2022-07-12T12:00:00.123");

        TimingUtils.addTime(SIXTEEN_MINUTES);
        processor.sync();

        Instant now = getInstant();
        List<FeeRecommendation> actualRecommendationList = List.of(
                createFeeRecommendation(949799, "84520", false, 500, 100,
                        now, now),
                createFeeRecommendation(1367820, "1187405", false, 700, 200,
                        now, now),
                createFeeRecommendation(2666761, "7411831", true, 1000, 500,
                        now, now)
        );
        executors.feeRecommendationsExecutor1().verify(actualRecommendationList,
                recommendation -> recommendation.setComputation_datetime(now));

        String fullTableTimeStr = "2022-07-12T14:00:00.123";
        String diffTableTimeStr = "2022-07-12T14:00:00.123";
        assertFullAndDiffSyncInfo(now, now, fullTableTimeStr, diffTableTimeStr);
    }

    @DisplayName("При отсутствии новых таблиц импорт не производился")
    @Test
    protected void testImport_noNewTables_noSync() {
        insertFeeRecommendationsTarget();

        Instant syncInfoInstant = getInstant();
        insertFullAndDiffSyncInfo(syncInfoInstant, "2022-07-12T12:00:00.123", "2022-07-12T14:00:00.123");

        TimingUtils.addTime(SIXTEEN_MINUTES);
        processor.sync();

        Instant instant = Instant.parse("2022-07-04T00:00:00.00Z");
        List<FeeRecommendation> actualRecommendationList = List.of(
                createFeeRecommendation(949799, "84520", false, 500, 100,
                        instant, instant),
                createFeeRecommendation(1367820, "1187405", false, 900, 500,
                        instant, instant),
                createFeeRecommendation(9999999, "11", false, 351, 39,
                        instant, instant)
        );
        executors.feeRecommendationsExecutor1().verify(actualRecommendationList);

        String fullTableTimeStr = "2022-07-12T12:00:00.123";
        String diffTableTimeStr = "2022-07-12T14:00:00.123";
        assertFullAndDiffSyncInfo(syncInfoInstant, syncInfoInstant, fullTableTimeStr, diffTableTimeStr);
    }

    @DisplayName("Таблица создана менее 15 минут назад, импорт не производился")
    @Test
    protected void testImport_tableCreationTimeIsWrong_noSync() {
        insertFeeRecommendationsTarget();

        Instant syncInfoInstant = getInstant();
        insertFullAndDiffSyncInfo(syncInfoInstant, "2022-07-11T12:00:00.123", "2022-07-12T13:00:00.123");

        processor.sync();

        Instant instant = Instant.parse("2022-07-04T00:00:00.00Z");
        List<FeeRecommendation> actualRecommendationList = List.of(
                createFeeRecommendation(949799, "84520", false, 500, 100,
                        instant, instant),
                createFeeRecommendation(1367820, "1187405", false, 900, 500,
                        instant, instant),
                createFeeRecommendation(9999999, "11", false, 351, 39,
                        instant, instant)
        );
        executors.feeRecommendationsExecutor1().verify(actualRecommendationList);

        String fullTableTimeStr = "2022-07-11T12:00:00.123";
        String diffTableTimeStr = "2022-07-12T13:00:00.123";
        assertFullAndDiffSyncInfo(syncInfoInstant, syncInfoInstant, fullTableTimeStr, diffTableTimeStr);
    }

    @DisplayName("Дифф-таблица создана менее 15 минут назад, импорт не производился")
    @Test
    protected void testImport_diffTableCreationTimeIsWrong_noSync() {
        insertFeeRecommendationsTarget();

        Instant syncInfoInstant = getInstant();
        insertFullAndDiffSyncInfo(syncInfoInstant, "2022-07-12T12:00:00.123", "2022-07-12T13:00:00.123");

        processor.sync();

        Instant instant = Instant.parse("2022-07-04T00:00:00.00Z");
        List<FeeRecommendation> actualRecommendationList = List.of(
                createFeeRecommendation(949799, "84520", false, 500, 100,
                        instant, instant),
                createFeeRecommendation(1367820, "1187405", false, 900, 500,
                        instant, instant),
                createFeeRecommendation(9999999, "11", false, 351, 39,
                        instant, instant)
        );
        executors.feeRecommendationsExecutor1().verify(actualRecommendationList);

        String fullTableTimeStr = "2022-07-12T12:00:00.123";
        String diffTableTimeStr = "2022-07-12T13:00:00.123";
        assertFullAndDiffSyncInfo(syncInfoInstant, syncInfoInstant, fullTableTimeStr, diffTableTimeStr);
    }

    private void insertFeeRecommendationsFullSource() {
        List<NewFeeRecommendationSource> sourceList = List.of(
                createFeeRecommendationsSource(2666761, "7411831", true, 1000, 500),
                createFeeRecommendationsSource(949799, "84520", false, 500, 100),
                createFeeRecommendationsSource(1367820, "1187405", false, 700, 200)
        );
        executors.feeRecommendationsExecutor1().insertSource(sourceList);
        executors.feeRecommendationsExecutor2().insertSource(sourceList);
        executors.feeRecommendationsExecutor3().insertSource(sourceList);
    }

    private void insertFeeRecommendationsDiffSource() {
        List<NewFeeRecommendationSource> sourceList = List.of(
                createFeeRecommendationsDiffSource(2666761, "7411831", true, 1000, 500, 1),
                createFeeRecommendationsDiffSource(949799, "84520", false, 500, 100, 2),
                createFeeRecommendationsDiffSource(1367820, "1187405", false, 700, 200, 3)
        );
        List<NewFeeRecommendationSource> diffSourceList = List.of(
                createFeeRecommendationsDiffSource(2666761, "7411831", true, 0, 500, 1),
                createFeeRecommendationsDiffSource(949799, "84520", false, 1023, 100, 2),
                createFeeRecommendationsDiffSource(1367820, "1187405", false, 4532, 200, 3)
        );
        executors.feeRecommendationsExecutor1().insertSourceDiff(sourceList);
        executors.feeRecommendationsExecutor2().insertSourceDiff(sourceList);
        executors.feeRecommendationsExecutor3().insertSourceDiff(diffSourceList);
    }

    private void insertFeeRecommendationsTarget() {
        Instant instant = Instant.parse("2022-07-04T00:00:00.00Z");
        List<FeeRecommendation> recommendationList = List.of(
                createFeeRecommendation(949799, "84520", false, 500, 100,
                        instant, instant),
                createFeeRecommendation(1367820, "1187405", false, 900, 500,
                        instant, instant),
                createFeeRecommendation(9999999, "11", false, 351, 39,
                        instant, instant)
        );
        executors.feeRecommendationsExecutor1().insert(recommendationList);
    }

    private void insertFullAndDiffSyncInfo(Instant syncedAt, String fullTableTimeStr, String diffTableTimeStr) {
        insertSyncInfo(FEE_RECOMMENDATIONS, syncedAt, fullTableTimeStr);
        insertSyncInfo(FEE_RECOMMENDATIONS_DIFF, syncedAt, diffTableTimeStr);
    }

    private NewFeeRecommendationSource createFeeRecommendationsDiffSource(
            long supplierId, String offerId, boolean tooHighFee, long recommendedFee,
            long potentialShowsPerDay, int updateStatus) {
        NewFeeRecommendationSource source = createFeeRecommendationsSource(
                supplierId, offerId, tooHighFee, recommendedFee, potentialShowsPerDay);
        source.setUpdate_status(updateStatus);
        return source;
    }

    private NewFeeRecommendationSource createFeeRecommendationsSource(
            long supplierId, String offerId, boolean tooHighFee, long recommendedFee,
            long potentialShowsPerDay) {
        NewFeeRecommendationSource source = new NewFeeRecommendationSource();
        source.setSupplier_id(supplierId);
        source.setOffer_id(offerId);
        source.set_too_high_fee(tooHighFee);
        source.setRecommended_shop_fee(recommendedFee);
        source.setMsku_potential_rkm_shows_per_day(potentialShowsPerDay);
        return source;
    }

    private FeeRecommendation createFeeRecommendation(
            long partnerId, String offerId, boolean tooHighFee, long recommendedFee,
            long potentialShowsPerDay, Instant updatedAt, Instant computationTime) {
        FeeRecommendation recommendation = new FeeRecommendation();
        recommendation.setPartner_id(partnerId);
        recommendation.setOffer_id(offerId);
        recommendation.set_too_high_fee(tooHighFee);
        recommendation.setRecommended_fee(recommendedFee);
        recommendation.setPotential_shows_per_day(potentialShowsPerDay);
        recommendation.setComputation_datetime(computationTime);
        recommendation.setUpdated_at(updatedAt);
        return recommendation;
    }

    private void assertFullAndDiffSyncInfo(Instant fullSyncedAt, Instant diffSyncedAt,
                                           String fullTableTimeStr, String diffTableTimeStr) {
        assertSyncInfo(FEE_RECOMMENDATIONS, fullSyncedAt, fullTableTimeStr);
        assertSyncInfo(FEE_RECOMMENDATIONS_DIFF, diffSyncedAt, diffTableTimeStr);
    }
}
