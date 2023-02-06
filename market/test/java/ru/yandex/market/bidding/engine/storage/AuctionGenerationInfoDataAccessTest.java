package ru.yandex.market.bidding.engine.storage;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import ru.yandex.market.bidding.FunctionalTest;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.util.DateTimes;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.market.bidding.AuctionTestCommons.DELTA;
import static ru.yandex.market.bidding.AuctionTestCommons.SNAPSHOT;
import static ru.yandex.market.bidding.AuctionTestCommons.STAGE_PROCESSING_FAILED;
import static ru.yandex.market.bidding.AuctionTestCommons.STAGE_PROCESSING_OK;

/**
 * Тесты для {@link AuctionGenerationInfoDataAccess}.
 */
class AuctionGenerationInfoDataAccessTest extends FunctionalTest {
    private static final long GID_222 = 222;
    private static final long GID_333 = 333;
    private static final long GID_444 = 444;
    private static final long GID_555 = 555;

    @Autowired
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    @Autowired
    private AuctionGenerationInfoDataAccess auctionGenerationInfoDataAccess;

    @DisplayName("id послденего поколения в базе")
    @DbUnitDataSet(before = "db/AuctionGenerationInfoDataAccessTest.before.csv")
    @Test
    void test_lastGid() {
        assertThat(auctionGenerationInfoDataAccess.lastGid(), is(GID_333));
    }

    @DisplayName("Вставка результатов применения снепшота/дельты")
    @DbUnitDataSet(
            before = "db/AuctionGenerationInfoDataAccessTest.before.csv",
            after = "db/AuctionGenerationInfoDataAccessTest.insertGenerationInfo.after.csv"
    )
    @Test
    void test_insertGenerationInfo() {
        int today = (int) DateTimes.toInstantAtDefaultTz(LocalDate.now().atStartOfDay()).getEpochSecond();

        auctionGenerationInfoDataAccess.insertGenerationInfo(
                GID_444,
                today,
                DELTA,
                "some_generation_name",
                11111,
                "/first/path/to/directory/",
                "hahn.yt.yandex.net"
        );

        auctionGenerationInfoDataAccess.insertGenerationInfo(
                GID_555,
                today,
                SNAPSHOT,
                null,
                22222,
                "/second/path/to/directory/",
                "arnold.yt.yandex.net"
        );
    }

    /**
     * Проверка времени немного топорная - следствие того что проставляется средствами sysdate в sql.
     */
    @DisplayName("Обновление статуса обработки для снепшота/дельты")
    @DbUnitDataSet(
            before = "db/AuctionGenerationInfoDataAccessTest.before.csv",
            after = "db/AuctionGenerationInfoDataAccessTest.merge_n_insert_time.after.csv"
    )
    @Test
    void test_updateGenerationMergeNInsertStatus() {
        Instant curTime = Instant.now();

        auctionGenerationInfoDataAccess.updateGenerationInsertStatus(GID_222, STAGE_PROCESSING_FAILED);
        auctionGenerationInfoDataAccess.updateGenerationMergeStatus(GID_222, STAGE_PROCESSING_FAILED);
        GenInfoTimes genInfoTimes = getStageTimes(GID_222);

        assertTrue(Duration.between(curTime, genInfoTimes.insertTime).toMinutes() < 5L);
        assertTrue(Duration.between(curTime, genInfoTimes.mergeTime).toMinutes() < 5L);

        auctionGenerationInfoDataAccess.updateGenerationInsertStatus(GID_333, STAGE_PROCESSING_OK);
        auctionGenerationInfoDataAccess.updateGenerationMergeStatus(GID_333, STAGE_PROCESSING_OK);

        genInfoTimes = getStageTimes(GID_333);
        assertTrue(Duration.between(curTime, genInfoTimes.insertTime).toMinutes() < 5L);
        assertTrue(Duration.between(curTime, genInfoTimes.mergeTime).toMinutes() < 5L);
    }

    @DisplayName("Проверка того, что меняется raw_size для снапшота/дельты")
    @DbUnitDataSet(
            before = "db/AuctionGenerationInfoDataAccessTest.before.csv",
            after = "db/AuctionGenerationInfoDataAccessTest.updateIdxSize.after.csv"
    )
    @Test
    void test_updateIdxSize() {
        long gid = 9L;
        long idxSize = 123456789L;
        auctionGenerationInfoDataAccess.updateIdxSize(gid, idxSize);
    }

    private GenInfoTimes getStageTimes(long gid) {
        return namedParameterJdbcTemplate.query(
                "" +
                        "select" +
                        "  insert_time," +
                        "  merge_time " +
                        "from shops_web.auction_generation_info " +
                        "where id=:id",
                new MapSqlParameterSource("id", gid),
                (xs, i) -> new GenInfoTimes(
                        xs.getTimestamp("insert_time").toInstant(),
                        xs.getTimestamp("merge_time").toInstant()
                )
        ).get(0);
    }

    private static class GenInfoTimes {
        final Instant mergeTime;
        final Instant insertTime;

        GenInfoTimes(Instant mergeTime, Instant insertTime) {
            this.mergeTime = mergeTime;
            this.insertTime = insertTime;
        }
    }
}
