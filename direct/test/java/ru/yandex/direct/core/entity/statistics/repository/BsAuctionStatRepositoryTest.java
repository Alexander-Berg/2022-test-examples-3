package ru.yandex.direct.core.entity.statistics.repository;

import java.math.BigInteger;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.assertj.core.api.SoftAssertions;
import org.jooq.DSLContext;
import org.jooq.types.ULong;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.statistics.container.AdGroupIdAndPhraseIdPair;
import ru.yandex.direct.core.entity.statistics.container.ChangedPhraseIdInfo;
import ru.yandex.direct.core.entity.statistics.container.ProcessedAuctionStat;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbschema.ppc.tables.records.BsAuctionStatRecord;
import ru.yandex.direct.dbutil.QueryWithoutIndex;
import ru.yandex.direct.dbutil.exception.RollbackException;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.test.utils.RandomNumberUtils;

import static java.time.Month.AUGUST;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.dbschema.ppc.Tables.BS_AUCTION_STAT;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class BsAuctionStatRepositoryTest {

    private static final LocalDateTime FIXED_DATE = LocalDateTime.of(2021, 1, 11, 4, 20);

    @Autowired
    private Steps steps;

    @Autowired
    private BsAuctionStatRepository bsAuctionStatRepository;

    @Autowired
    private DslContextProvider dslContextProvider;

    /**
     * Тестируем случай, когда у нас есть устаревшая запись с ключами их списка. Метод должен удалить ее.
     */
    @Test
    public void deleteUnusedIds_unusedRecord_recordDeleted() {
        runWithEmptyBsAuctionStatTable(dsl -> {
            BsAuctionStatRecord record = makeRecord();
            dsl.insertInto(BS_AUCTION_STAT)
                    .set(record)
                    .execute();

            int deleted = bsAuctionStatRepository.deleteUnusedByIds(dsl,
                    List.of(new AdGroupIdAndPhraseIdPair(1, ULong.valueOf(1))), LocalDateTime.now());

            assertThat(deleted).isEqualTo(1);
        });
    }

    /**
     * Тестируем случай, когда запись с ключами из списка еще не устарела. Метод не должен ее удалять.
     */
    @Test
    public void deleteUnusedIds_reusedRecord_recordNotDeleted() {
        runWithEmptyBsAuctionStatTable(dsl -> {
            BsAuctionStatRecord record = makeRecord();
            dsl.insertInto(BS_AUCTION_STAT)
                    .set(record)
                    .execute();

            int deleted = bsAuctionStatRepository.deleteUnusedByIds(dsl,
                    List.of(new AdGroupIdAndPhraseIdPair(1, ULong.valueOf(1))),
                    LocalDateTime.now().minusDays(2));

            assertThat(deleted).isEqualTo(0);
        });
    }

    /**
     * Тестируем случай, когда в базе есть устаревшая запись, но нет ее ключей в списке на удаление.
     * Метод не должен ее удалять.
     */
    @Test
    public void deleteUnusedIds_expiredRecordWithDifferentIds_recordNotDeleted() {
        runWithEmptyBsAuctionStatTable(dsl -> {
            BsAuctionStatRecord record = makeRecord();
            dsl.insertInto(BS_AUCTION_STAT)
                    .set(record)
                    .execute();

            int deleted = bsAuctionStatRepository.deleteUnusedByIds(dsl,
                    List.of(new AdGroupIdAndPhraseIdPair(1, ULong.valueOf(2))), LocalDateTime.now());

            assertThat(deleted).isEqualTo(0);
        });
    }

    /**
     * Тестируем случай, когда нет ключей в списке на удаление. Метод не должен ничего удалять.
     */
    @Test
    public void deleteUnusedIds_noIdsToDelete_noDeletedRecords() {
        runWithEmptyBsAuctionStatTable(dsl -> {
            BsAuctionStatRecord record = makeRecord();
            dsl.insertInto(BS_AUCTION_STAT)
                    .set(record)
                    .execute();

            int deleted = bsAuctionStatRepository.deleteUnusedByIds(dsl, List.of(), LocalDateTime.now());

            assertThat(deleted).isEqualTo(0);
        });
    }

    @QueryWithoutIndex("Удаление всех данных для тестов")
    private void runWithEmptyBsAuctionStatTable(Consumer<DSLContext> test) {
        try {
            dslContextProvider.ppcTransaction(1, configuration -> {
                DSLContext dsl = configuration.dsl();
                dsl.deleteFrom(BS_AUCTION_STAT).execute();

                test.accept(dsl);

                throw new RollbackException();
            });
        } catch (RollbackException ignored) {
        }
    }

    private BsAuctionStatRecord makeRecord() {
        BsAuctionStatRecord record = new BsAuctionStatRecord();
        record.setPid(1L);
        record.setPhraseid(ULong.valueOf(1));
        record.setShows((long) RandomNumberUtils.nextPositiveInteger());
        record.setClicks((long) RandomNumberUtils.nextPositiveInteger());
        record.setRank(1L);
        record.setStattime(LocalDateTime.now().minusDays(1).truncatedTo(ChronoUnit.SECONDS));
        record.setPshows((long) RandomNumberUtils.nextPositiveInteger());
        record.setPclicks((long) RandomNumberUtils.nextPositiveInteger());
        return record;
    }

    /**
     * Тест проверяет, что для уже существующей пары pid, PhraseID все значения обновятся, а для не существующей
     * добаятся
     */
    @Test
    public void updateBsAuctionStat_UpdateAndInsert_Test() {
        var adGroupInfo = steps.adGroupSteps().createDefaultAdGroup();
        int shard = adGroupInfo.getShard();
        var pid = adGroupInfo.getAdGroupId();
        var phraseIdToUpdate = new BigInteger("15859473995177289860");
        var phraseIdToInsert = new BigInteger("15859473995177289863");

        var statTime1 = LocalDateTime.of(2019, AUGUST, 1, 1, 2, 3);

        // Первая вставка
        var firstInsertedProcessedAuctionStat = new ProcessedAuctionStat.Builder()
                .withPid(pid)
                .withPhraseId(phraseIdToUpdate)
                .withClicks(3L)
                .withPclicks(4L)
                .withShows(30L)
                .withPshows(40L)
                .withRank(4L)
                .withStatTime(statTime1)
                .build();

        bsAuctionStatRepository.updateBsAuctionStat(shard, List.of(firstInsertedProcessedAuctionStat));
        var firstInsertedProcessedAuctionStatGot = selectProcessedAuctionStat(shard, pid, phraseIdToUpdate);
        assertThat(firstInsertedProcessedAuctionStatGot).isEqualTo(firstInsertedProcessedAuctionStat);

        var statTime2 = statTime1.plusMinutes(3);
        // Уже существующая пара pid, PhraseID все значения должна обновиться
        var updatedProcessedAuctionStat = new ProcessedAuctionStat.Builder()
                .withPid(pid)
                .withPhraseId(phraseIdToUpdate)
                .withClicks(4L)
                .withPclicks(5L)
                .withShows(44L)
                .withPshows(55L)
                .withRank(6L)
                .withStatTime(statTime2)
                .build();

        // Для pid, PhraseID еще нет значений - вставятся новые
        var insertedProcessedAuctionStat = new ProcessedAuctionStat.Builder()
                .withPid(pid)
                .withPhraseId(phraseIdToInsert)
                .withClicks(1L)
                .withPclicks(2L)
                .withShows(10L)
                .withPshows(20L)
                .withRank(1L)
                .withStatTime(statTime2)
                .build();
        bsAuctionStatRepository.updateBsAuctionStat(shard, List.of(updatedProcessedAuctionStat,
                insertedProcessedAuctionStat));

        var updatedProcessedAuctionStatGot = selectProcessedAuctionStat(shard, pid, phraseIdToUpdate);
        var insertedProcessedAuctionStatGot = selectProcessedAuctionStat(shard, pid, phraseIdToInsert);
        assertThat(updatedProcessedAuctionStatGot).isEqualTo(updatedProcessedAuctionStat);
        assertThat(insertedProcessedAuctionStatGot).isEqualTo(insertedProcessedAuctionStat);

    }

    @Test
    public void updatePhraseId_updateOneTest() {
        BigInteger oldPhraseId = BigInteger.valueOf(666666);
        Long pid = 9999998L;

        BsAuctionStatRecord record = makeRecord();
        record.setPid(pid);
        record.setPhraseid(ULong.valueOf(oldPhraseId));
        record.setStattime(FIXED_DATE);
        dslContextProvider.ppc(1).insertInto(BS_AUCTION_STAT).set(record).execute();

        BigInteger newPhaseId = BigInteger.valueOf(1572402405);
        var change = new ChangedPhraseIdInfo(null, pid, oldPhraseId, newPhaseId);
        bsAuctionStatRepository.updatePhraseId(1, List.of(change));

        var result = fetchFromDb(pid);
        assertThat(result)
                .as("старый PhraseID не выбирается")
                .doesNotContainKey(oldPhraseId)
                .as("из базы выбрался новый PhraseID")
                .extractingByKey(newPhaseId, InstanceOfAssertFactories.type(BsAuctionStatRecord.class))
                .extracting(BsAuctionStatRecord::intoMap)
                .usingRecursiveComparison()
                .ignoringFields("PhraseID")
                .as("обновился только PhraseID")
                .isEqualTo(record.intoMap());

        cleanupDb(List.of(pid));
    }

    @Test
    public void updatePhraseId_updateTwoAdgroupsTest() {
        Long pid1 = 9999997L;
        Long pid2 = 9999996L;
        BigInteger oldPhraseId1 = BigInteger.valueOf(666555);
        BigInteger oldPhraseId2 = BigInteger.valueOf(666444);
        BigInteger oldPhraseId3 = oldPhraseId2;

        // обновится
        BsAuctionStatRecord record1 = makeRecord();
        record1.setPid(pid1);
        record1.setPhraseid(ULong.valueOf(oldPhraseId1));
        // не должно измениться
        BsAuctionStatRecord record2 = makeRecord();
        record2.setPid(pid1);
        record2.setPhraseid(ULong.valueOf(oldPhraseId2));
        record2.setStattime(FIXED_DATE);
        // обновится
        BsAuctionStatRecord record3 = makeRecord();
        record3.setPid(pid2);
        record3.setPhraseid(ULong.valueOf(oldPhraseId3));

        dslContextProvider.ppc(1).insertInto(BS_AUCTION_STAT)
                .set(record1)
                .newRecord().set(record2)
                .newRecord().set(record3)
                .execute();

        BigInteger newPhaseId1 = BigInteger.valueOf(1676824461);
        BigInteger newPhaseId3 = BigInteger.valueOf(1601912285);

        var changes = List.of(
                new ChangedPhraseIdInfo(null, pid1, oldPhraseId1, newPhaseId1),
                new ChangedPhraseIdInfo(null, pid2, oldPhraseId3, newPhaseId3));
        bsAuctionStatRepository.updatePhraseId(1, changes);

        var sa = new SoftAssertions();
        var result1 = fetchFromDb(pid1);
        sa.assertThat(result1)
                .as("старое значение PhraseID у первой фразы в одной группе не выбирается")
                .doesNotContainKey(oldPhraseId1);
        sa.assertThat(result1)
                .as("из базы выбралось новое значение PhraseID")
                .extractingByKey(newPhaseId1, InstanceOfAssertFactories.type(BsAuctionStatRecord.class))
                .extracting(BsAuctionStatRecord::intoMap)
                .usingRecursiveComparison()
                .ignoringFields("PhraseID")
                .as("обновился только PhraseID")
                .isEqualTo(record1.intoMap());
        sa.assertThat(result1)
                .as("данные второй фразы первой группы не изменились")
                .extractingByKey(oldPhraseId2, InstanceOfAssertFactories.type(BsAuctionStatRecord.class))
                .extracting(BsAuctionStatRecord::intoMap)
                .usingRecursiveComparison()
                .isEqualTo(record2.intoMap());

        var result2 = fetchFromDb(pid2);
        sa.assertThat(result2)
                .as("старое значение PhraseID у фразы в другой группе не выбирается")
                .doesNotContainKey(oldPhraseId3);
        sa.assertThat(result2)
                .as("из базы выбралось новое значение PhraseID")
                .extractingByKey(newPhaseId3, InstanceOfAssertFactories.type(BsAuctionStatRecord.class))
                .extracting(BsAuctionStatRecord::intoMap)
                .usingRecursiveComparison()
                .ignoringFields("PhraseID")
                .as("обновился только PhraseID")
                .isEqualTo(record3.intoMap());
        sa.assertAll();

        cleanupDb(List.of(pid1, pid2));
    }

    @Test
    public void updatePhraseId_duplicateTest() {
        Long pid = 9999995L;
        BigInteger oldPhraseId1 = BigInteger.valueOf(333333);
        BigInteger oldPhraseId2 = new BigInteger("2326546851808312331");

        BsAuctionStatRecord record1 = makeRecord();
        record1.setPid(pid);
        record1.setPhraseid(ULong.valueOf(oldPhraseId1));
        record1.setStattime(FIXED_DATE);
        BsAuctionStatRecord record2 = makeRecord();
        record2.setPid(pid);
        record2.setPhraseid(ULong.valueOf(oldPhraseId2));
        record2.setStattime(FIXED_DATE);

        dslContextProvider.ppc(1).insertInto(BS_AUCTION_STAT)
                .set(record1)
                .newRecord().set(record2)
                .execute();

        BigInteger newPhaseId1 = oldPhraseId2;

        var changes = List.of(new ChangedPhraseIdInfo(null, pid, oldPhraseId1, newPhaseId1));
        var sa = new SoftAssertions();
        sa.assertThatCode(() ->
                        bsAuctionStatRepository.updatePhraseId(1, changes))
                .as("запрос в базу прошел без ошибок")
                .doesNotThrowAnyException();

        var result = fetchFromDb(pid);
        sa.assertThat(result)
                .as("в базе осталось 2 записи")
                .hasSize(2);
        sa.assertThat(result)
                .extractingByKey(oldPhraseId1, InstanceOfAssertFactories.type(BsAuctionStatRecord.class))
                .extracting(BsAuctionStatRecord::intoMap)
                .usingRecursiveComparison()
                .as("обновляемая фраза не изменилась")
                .isEqualTo(record1.intoMap());
        sa.assertThat(result)
                .extractingByKey(oldPhraseId2, InstanceOfAssertFactories.type(BsAuctionStatRecord.class))
                .extracting(BsAuctionStatRecord::intoMap)
                .usingRecursiveComparison()
                .as("конфликтуемая фраза не изменились")
                .isEqualTo(record2.intoMap());
        sa.assertAll();

        cleanupDb(List.of(pid));
    }

    private int cleanupDb(Collection<Long> pid) {
        return dslContextProvider.ppc(1)
                .deleteFrom(BS_AUCTION_STAT)
                .where(BS_AUCTION_STAT.PID.in(pid))
                .execute();
    }

    private Map<BigInteger, BsAuctionStatRecord> fetchFromDb(Long pid) {
        return dslContextProvider.ppc(1)
                .select(BS_AUCTION_STAT.PID,
                        BS_AUCTION_STAT.PHRASE_ID,
                        BS_AUCTION_STAT.STATTIME,
                        BS_AUCTION_STAT.RANK,
                        BS_AUCTION_STAT.PSHOWS,
                        BS_AUCTION_STAT.PCLICKS,
                        BS_AUCTION_STAT.SHOWS,
                        BS_AUCTION_STAT.CLICKS)
                .from(BS_AUCTION_STAT)
                .where(BS_AUCTION_STAT.PID.eq(pid))
                .fetchMap(r -> r.get(BS_AUCTION_STAT.PHRASE_ID).toBigInteger(), BsAuctionStatRecord.class);
    }

    private ProcessedAuctionStat selectProcessedAuctionStat(int shard, long pid, BigInteger phraseId) {
        return dslContextProvider.ppc(shard)
                .select(BS_AUCTION_STAT.PID, BS_AUCTION_STAT.PHRASE_ID, BS_AUCTION_STAT.CLICKS,
                        BS_AUCTION_STAT.PCLICKS, BS_AUCTION_STAT.SHOWS, BS_AUCTION_STAT.PSHOWS, BS_AUCTION_STAT.RANK,
                        BS_AUCTION_STAT.STATTIME)
                .from(BS_AUCTION_STAT)
                .where(BS_AUCTION_STAT.PID.eq(pid).and(BS_AUCTION_STAT.PHRASE_ID.eq(ULong.valueOf(phraseId))))
                .fetchOne(r -> new ProcessedAuctionStat.Builder()
                        .withPid(r.get(BS_AUCTION_STAT.PID))
                        .withPhraseId(r.get(BS_AUCTION_STAT.PHRASE_ID).toBigInteger())
                        .withClicks(r.get(BS_AUCTION_STAT.CLICKS))
                        .withPclicks(r.get(BS_AUCTION_STAT.PCLICKS))
                        .withShows(r.get(BS_AUCTION_STAT.SHOWS))
                        .withPshows(r.get(BS_AUCTION_STAT.PSHOWS))
                        .withRank(r.get(BS_AUCTION_STAT.RANK))
                        .withStatTime(r.get(BS_AUCTION_STAT.STATTIME))
                        .build());
    }
}
