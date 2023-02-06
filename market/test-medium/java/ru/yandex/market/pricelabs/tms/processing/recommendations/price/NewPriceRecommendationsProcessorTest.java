package ru.yandex.market.pricelabs.tms.processing.recommendations.price;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.pricelabs.misc.TimingUtils;
import ru.yandex.market.pricelabs.model.recommendation.PriceRecommendation;
import ru.yandex.market.pricelabs.model.recommendation.PriceRecommendationsSource;
import ru.yandex.market.pricelabs.processing.recommendations.repository.SyncInfoRepository;
import ru.yandex.market.pricelabs.tms.processing.recommendations.AbstractRecommendationsProcessorConfiguration;

import static ru.yandex.market.pricelabs.processing.recommendations.constant.RecommendationsConstant.PRICE_RECOMMENDATIONS;
import static ru.yandex.market.pricelabs.processing.recommendations.constant.RecommendationsConstant.PRICE_RECOMMENDATIONS_DIFF;

@DisplayName("Тесты на процессор для загрузки ценовых рекомендаций NewPriceRecommendationsProcessor")
public class NewPriceRecommendationsProcessorTest extends AbstractRecommendationsProcessorConfiguration {

    @Autowired
    private NewPriceRecommendationsProcessor processor;

    @Autowired
    private SyncInfoRepository syncInfoRepository;

    @BeforeEach
    @AfterEach
    protected void init() {

        executors.priceRecommendationsExecutor1().clearSourceTable();
        executors.priceRecommendationsExecutor2().clearSourceTable();
        executors.priceRecommendationsExecutor3().clearSourceTable();

        executors.priceRecommendationsExecutor1().clearSourceDiffTable();
        executors.priceRecommendationsExecutor2().clearSourceDiffTable();
        executors.priceRecommendationsExecutor3().clearSourceDiffTable();

        executors.priceRecommendationsExecutor1().clearTargetTable();
        syncInfoRepository.clear();
    }

    @DisplayName("Первичный импорт в пустую таблицу прошел успешно")
    @Test
    protected void testImport_firstImport_fullSync() {

        insertPriceRecommendationsFullSource();
        insertPriceRecommendationsDiffSource(0d, 1023d, 4532d);

        TimingUtils.addTime(16 * 60 * 1000);
        processor.sync();

        Instant now = getInstant();
        executors.priceRecommendationsExecutor1().verify(List.of(
                getPriceRecommendation(949799, "84520", 1860, 1000, 0.44999999999999996,
                        now, now),
                getPriceRecommendation(1367820, "1187405", 5180, 5000, 0.1250965250965251,
                        now, now),
                getPriceRecommendation(2666761, "7411831", 7075, 7000, 0.05,
                        now, now)
        ), priceRecommendation -> priceRecommendation.setComputation_datetime(now));

        assertFullAndDiffSyncInfo(now, now, "2022-07-12T14:00:00.123", "2022-07-12T14:00:00.123");

        TimingUtils.resetTime();
    }

    @DisplayName("Импорт диффа при нормальных условиях прошел успешно")
    @Test
    protected void testImport_importDiff_diffSync() {

        insertPriceRecommendationsFullSource();
        insertPriceRecommendationsDiffSource(6000d, 1700d, 4000d);
        insertPriceRecommendationsTarget();

        Instant syncInfoInstant = getInstant();
        insertFullAndDiffSyncInfo(syncInfoInstant, "2022-07-12T12:00:00.123", "2022-07-12T13:00:00.123");

        TimingUtils.addTime(16 * 60 * 1000);
        processor.sync();

        Instant now = getInstant();
        executors.priceRecommendationsExecutor1().verify(List.of(
                getPriceRecommendation(949799, "84520", 1860, 1700, 0.44999999999999996,
                        now, now),
                getPriceRecommendation(2666761, "7411831", 7075, 6000, 0.05,
                        now, now),
                getPriceRecommendation(9999999, "11", 5180, 5180, 0.1250965250965251,
                        Instant.parse("2022-07-04T00:00:00.00Z"), now)
        ), priceRecommendation -> priceRecommendation.setComputation_datetime(now));

        assertFullAndDiffSyncInfo(syncInfoInstant, now, "2022-07-12T12:00:00.123", "2022-07-12T14:00:00.123");

        TimingUtils.resetTime();
    }

    @DisplayName("Полный импорт за новый день прошел успешно")
    @Test
    protected void testImport_newDay_fullSync() {

        insertPriceRecommendationsFullSource();
        insertPriceRecommendationsDiffSource(0d, 1023d, 4532d);
        insertPriceRecommendationsTarget();

        Instant syncInfoInstant = getInstant();
        insertFullAndDiffSyncInfo(syncInfoInstant, "2022-07-11T12:00:00.123", "2022-07-12T13:00:00.123");

        TimingUtils.addTime(16 * 60 * 1000);
        processor.sync();

        Instant now = getInstant();
        executors.priceRecommendationsExecutor1().verify(List.of(
                getPriceRecommendation(949799, "84520", 1860, 1000, 0.44999999999999996,
                        now, now),
                getPriceRecommendation(1367820, "1187405", 5180, 5000, 0.1250965250965251,
                        now, now),
                getPriceRecommendation(2666761, "7411831", 7075, 7000, 0.05,
                        now, now)
        ), priceRecommendation -> priceRecommendation.setComputation_datetime(now));

        assertFullAndDiffSyncInfo(now, now, "2022-07-12T14:00:00.123", "2022-07-12T14:00:00.123");

        TimingUtils.resetTime();
    }

    @DisplayName("Полный импорт при пропущенном диффе прошел успешно")
    @Test
    protected void testImport_missedDiff_fullSync() {

        insertPriceRecommendationsFullSource();
        insertPriceRecommendationsDiffSource(0d, 1023d, 4532d);
        insertPriceRecommendationsTarget();

        Instant syncInfoInstant = getInstant();
        insertFullAndDiffSyncInfo(syncInfoInstant, "2022-07-12T12:00:00.123", "2022-07-12T12:00:00.123");

        TimingUtils.addTime(16 * 60 * 1000);
        processor.sync();

        Instant now = getInstant();
        executors.priceRecommendationsExecutor1().verify(List.of(
                getPriceRecommendation(949799, "84520", 1860, 1000, 0.44999999999999996,
                        now, now),
                getPriceRecommendation(1367820, "1187405", 5180, 5000, 0.1250965250965251,
                        now, now),
                getPriceRecommendation(2666761, "7411831", 7075, 7000, 0.05,
                        now, now)
        ), priceRecommendation -> priceRecommendation.setComputation_datetime(now));

        assertFullAndDiffSyncInfo(now, now, "2022-07-12T14:00:00.123", "2022-07-12T14:00:00.123");

        TimingUtils.resetTime();
    }

    @DisplayName("При отсутствии новых таблиц импорт не производился")
    @Test
    protected void testImport_noNewTables_noSync() {

        insertPriceRecommendationsFullSource();
        insertPriceRecommendationsDiffSource(0d, 1023d, 4532d);
        insertPriceRecommendationsTarget();

        Instant syncInfoInstant = getInstant();
        insertFullAndDiffSyncInfo(syncInfoInstant, "2022-07-12T12:00:00.123", "2022-07-12T14:00:00.123");

        TimingUtils.addTime(16 * 60 * 1000);
        processor.sync();

        executors.priceRecommendationsExecutor1().verify(List.of(
                getPriceRecommendation(949799, "84520", 1860, 1860, 0.44999999999999996,
                        Instant.parse("2022-07-04T00:00:00.00Z"), Instant.parse("2022-07-04T00:00:00.00Z")),
                getPriceRecommendation(1367820, "1187405", 5180, 5180, 0.1250965250965251,
                        Instant.parse("2022-07-04T00:00:00.00Z"), Instant.parse("2022-07-04T00:00:00.00Z")),
                getPriceRecommendation(9999999, "11", 5180, 5180, 0.1250965250965251,
                        Instant.parse("2022-07-04T00:00:00.00Z"), Instant.parse("2022-07-04T00:00:00.00Z"))
        ));

        assertFullAndDiffSyncInfo(syncInfoInstant, syncInfoInstant,
                "2022-07-12T12:00:00.123", "2022-07-12T14:00:00.123");

        TimingUtils.resetTime();
    }

    @DisplayName("Таблица создана менее 15 минут назад, импорт не производился")
    @Test
    protected void testImport_tableCreationTimeIsWrong_noSync() {

        insertPriceRecommendationsFullSource();
        insertPriceRecommendationsDiffSource(0d, 1023d, 4532d);
        insertPriceRecommendationsTarget();

        Instant syncInfoInstant = getInstant();
        insertFullAndDiffSyncInfo(syncInfoInstant, "2022-07-11T12:00:00.123", "2022-07-12T13:00:00.123");

        processor.sync();

        executors.priceRecommendationsExecutor1().verify(List.of(
                getPriceRecommendation(949799, "84520", 1860, 1860, 0.44999999999999996,
                        Instant.parse("2022-07-04T00:00:00.00Z"), Instant.parse("2022-07-04T00:00:00.00Z")),
                getPriceRecommendation(1367820, "1187405", 5180, 5180, 0.1250965250965251,
                        Instant.parse("2022-07-04T00:00:00.00Z"), Instant.parse("2022-07-04T00:00:00.00Z")),
                getPriceRecommendation(9999999, "11", 5180, 5180, 0.1250965250965251,
                        Instant.parse("2022-07-04T00:00:00.00Z"), Instant.parse("2022-07-04T00:00:00.00Z"))
        ));

        assertFullAndDiffSyncInfo(syncInfoInstant, syncInfoInstant,
                "2022-07-11T12:00:00.123", "2022-07-12T13:00:00.123");
    }

    @DisplayName("Дифф-таблица создана менее 15 минут назад, импорт не производился")
    @Test
    protected void testImport_diffTableCreationTimeIsWrong_noSync() {

        insertPriceRecommendationsFullSource();
        insertPriceRecommendationsDiffSource(6000d, 1700d, 4000d);
        insertPriceRecommendationsTarget();

        Instant syncInfoInstant = getInstant();
        insertFullAndDiffSyncInfo(syncInfoInstant, "2022-07-12T12:00:00.123", "2022-07-12T13:00:00.123");

        processor.sync();

        executors.priceRecommendationsExecutor1().verify(List.of(
                getPriceRecommendation(949799, "84520", 1860, 1860, 0.44999999999999996,
                        Instant.parse("2022-07-04T00:00:00.00Z"), Instant.parse("2022-07-04T00:00:00.00Z")),
                getPriceRecommendation(1367820, "1187405", 5180, 5180, 0.1250965250965251,
                        Instant.parse("2022-07-04T00:00:00.00Z"), Instant.parse("2022-07-04T00:00:00.00Z")),
                getPriceRecommendation(9999999, "11", 5180, 5180, 0.1250965250965251,
                        Instant.parse("2022-07-04T00:00:00.00Z"), Instant.parse("2022-07-04T00:00:00.00Z"))
        ));

        assertFullAndDiffSyncInfo(syncInfoInstant, syncInfoInstant,
                "2022-07-12T12:00:00.123", "2022-07-12T13:00:00.123");
    }

    private void insertPriceRecommendationsFullSource() {
        executors.priceRecommendationsExecutor1().insertSource(List.of(
                getPriceRecommendationsSource(2666761, "7411831", 7075, 0, 0.05),
                getPriceRecommendationsSource(949799, "84520", 1860, 1023, 0.44999999999999996),
                getPriceRecommendationsSource(1367820, "1187405", 5180, 4532, 0.1250965250965251)
        ));

        executors.priceRecommendationsExecutor2().insertSource(List.of(
                getPriceRecommendationsSource(2666761, "7411831", 7075, 0, 0.05),
                getPriceRecommendationsSource(949799, "84520", 1860, 1023, 0.44999999999999996),
                getPriceRecommendationsSource(1367820, "1187405", 5180, 4532, 0.1250965250965251)
        ));

        executors.priceRecommendationsExecutor3().insertSource(List.of(
                getPriceRecommendationsSource(2666761, "7411831", 7075, 7000, 0.05),
                getPriceRecommendationsSource(949799, "84520", 1860, 1000, 0.44999999999999996),
                getPriceRecommendationsSource(1367820, "1187405", 5180, 5000, 0.1250965250965251)
        ));
    }

    private void insertPriceRecommendationsDiffSource(double recPrice1, double recPrice2, double recPrice3) {
        executors.priceRecommendationsExecutor1().insertSourceDiff(List.of(
                getPriceRecommendationsDiffSource(2666761, "7411831", 7075, 0, 0.05, 1),
                getPriceRecommendationsDiffSource(949799, "84520", 1860, 1023, 0.44999999999999996, 2),
                getPriceRecommendationsDiffSource(1367820, "1187405", 5180, 4532, 0.1250965250965251, 3)
        ));

        executors.priceRecommendationsExecutor2().insertSourceDiff(List.of(
                getPriceRecommendationsDiffSource(2666761, "7411831", 7075, 0, 0.05, 1),
                getPriceRecommendationsDiffSource(949799, "84520", 1860, 1023, 0.44999999999999996, 2),
                getPriceRecommendationsDiffSource(1367820, "1187405", 5180, 4532, 0.1250965250965251, 3)
        ));

        executors.priceRecommendationsExecutor3().insertSourceDiff(List.of(
                getPriceRecommendationsDiffSource(2666761, "7411831", 7075, recPrice1, 0.05, 1),
                getPriceRecommendationsDiffSource(949799, "84520", 1860, recPrice2, 0.44999999999999996, 2),
                getPriceRecommendationsDiffSource(1367820, "1187405", 5180, recPrice3, 0.1250965250965251, 3)
        ));
    }

    private void insertPriceRecommendationsTarget() {
        executors.priceRecommendationsExecutor1().insert(List.of(
                getPriceRecommendation(949799, "84520", 1860, 1860, 0.44999999999999996,
                        Instant.parse("2022-07-04T00:00:00.00Z"), Instant.parse("2022-07-04T00:00:00.00Z")),
                getPriceRecommendation(1367820, "1187405", 5180, 5180, 0.1250965250965251,
                        Instant.parse("2022-07-04T00:00:00.00Z"), Instant.parse("2022-07-04T00:00:00.00Z")),
                getPriceRecommendation(9999999, "11", 5180, 5180, 0.1250965250965251,
                        Instant.parse("2022-07-04T00:00:00.00Z"), Instant.parse("2022-07-04T00:00:00.00Z"))
        ));
    }

    private void insertFullAndDiffSyncInfo(Instant syncInfoInstant, String tableNameFull, String tableNameDiff) {
        insertSyncInfo(PRICE_RECOMMENDATIONS, syncInfoInstant, tableNameFull);
        insertSyncInfo(PRICE_RECOMMENDATIONS_DIFF, syncInfoInstant, tableNameDiff);
    }

    private void assertFullAndDiffSyncInfo(Instant instantFull, Instant instantDiff,
                                           String tableNameFull, String tableNameDiff) {
        assertSyncInfo(PRICE_RECOMMENDATIONS, instantFull, tableNameFull);
        assertSyncInfo(PRICE_RECOMMENDATIONS_DIFF, instantDiff, tableNameDiff);
    }

    private PriceRecommendationsSource getPriceRecommendationsSource(
            long supplierId, String offerId, double price, double recommendedPrice,
            double recommendedPromocode) {
        PriceRecommendationsSource priceRecommendationsSource = new PriceRecommendationsSource();
        priceRecommendationsSource.setSupplier_id(supplierId);
        priceRecommendationsSource.setOffer_id(offerId);
        priceRecommendationsSource.setPrice(price);
        priceRecommendationsSource.setRecommended_price(recommendedPrice);
        priceRecommendationsSource.setRecommended_promocode(recommendedPromocode);
        priceRecommendationsSource.setStatus(1L);
        return priceRecommendationsSource;
    }

    private PriceRecommendationsSource getPriceRecommendationsDiffSource(
            long supplierId, String offerId, double price, double recommendedPrice,
            double recommendedPromocode, int updateStatus) {
        PriceRecommendationsSource priceRecommendationsSource = new PriceRecommendationsSource();
        priceRecommendationsSource.setSupplier_id(supplierId);
        priceRecommendationsSource.setOffer_id(offerId);
        priceRecommendationsSource.setPrice(price);
        priceRecommendationsSource.setRecommended_price(recommendedPrice);
        priceRecommendationsSource.setRecommended_promocode(recommendedPromocode);
        priceRecommendationsSource.setStatus(1L);
        priceRecommendationsSource.setUpdate_status(updateStatus);
        return priceRecommendationsSource;
    }

    private PriceRecommendation getPriceRecommendation(
            long partnerId, String offerId, double price, double recommendedPrice,
            double recommendedPromocode, Instant now, Instant computationTime) {
        PriceRecommendation priceRecommendation = new PriceRecommendation();
        priceRecommendation.setPartner_id(partnerId);
        priceRecommendation.setOffer_id(offerId);
        priceRecommendation.setPrice(price);
        priceRecommendation.setRecommended_price(recommendedPrice);
        priceRecommendation.setRecommended_promocode(recommendedPromocode);
        priceRecommendation.setStatus(1);
        priceRecommendation.setComputation_datetime(computationTime);
        priceRecommendation.setUpdated_at(now);
        return priceRecommendation;
    }
}
