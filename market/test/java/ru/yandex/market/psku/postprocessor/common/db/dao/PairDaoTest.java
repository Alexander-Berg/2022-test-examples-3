package ru.yandex.market.psku.postprocessor.common.db.dao;

import org.assertj.core.api.Assertions;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.yandex.market.psku.postprocessor.common.BaseDBTest;
import ru.yandex.market.psku.postprocessor.common.db.jooq.enums.PairState;
import ru.yandex.market.psku.postprocessor.common.db.jooq.enums.PairType;
import ru.yandex.market.psku.postprocessor.common.db.jooq.tables.pojos.Pair;
import ru.yandex.market.psku.postprocessor.common.util.PairBuilder;
import ru.yandex.market.psku.postprocessor.common.util.PairStorageBuilder;
import ru.yandex.utils.CloseableIterator;

import java.io.IOException;
import java.util.List;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static ru.yandex.market.psku.postprocessor.common.db.jooq.Tables.PAIR;
import static ru.yandex.market.psku.postprocessor.common.db.jooq.Tables.PAIR_STORAGE;

/**
 * @author Fedor Dergachev <a href="mailto:dergachevfv@yandex-team.ru"></a>
 */
public class PairDaoTest extends BaseDBTest {

    @Autowired
    private PairDao pairDao;

    private static final String SESSION_NAME = "testsession";

    @Test
    public void whenCreatePairNewTableIfNotExistOk() {
        pairDao.createNewTable();

        dsl().selectCount()
                .from(PAIR);
    }

    @Test
    public void whenCreateDropCreatePairNewTableOk() {
        pairDao.createNewTable();
        pairDao.dropNewTable();
        pairDao.createNewTable();
    }

    @Test
    public void whenSaveNewPairsOk() {
        Long sessionId = createNewSession(SESSION_NAME);
        List<Pair> pairs = Stream.of(
                new PairBuilder().pskuId(10).mskuId(11).type(PairType.UC).countPskuOnMsku(1)
                    .sessionId(sessionId),
                new PairBuilder().pskuId(20).mskuId(21).type(PairType.REPORT).countPskuOnMsku(1)
                    .reportPosition(0).reportMatchRate(0.5).sessionId(sessionId),
                new PairBuilder().pskuId(30).mskuId(31).type(PairType.REPORT).countPskuOnMsku(1)
                    .reportPosition(1).reportMatchRate(0.1).sessionId(sessionId))
                .map(PairBuilder::build)
                .collect(Collectors.toList());

        pairDao.createNewTable();
        pairDao.saveToPairNew(pairs);

        List<Pair> pairsSaved = dsl()
                .fetch(PairDao.PAIR_NEW)
                .into(Pair.class);

        Assertions.assertThat(pairsSaved)
                .usingFieldByFieldElementComparator()
                .containsExactlyInAnyOrderElementsOf(pairs);
    }

    @Test
    public void whenSaveNewPairsDuplicatedOd() {
        Long sessionId = createNewSession(SESSION_NAME);
        List<Pair> pairs = Stream.of(
                new PairBuilder().pskuId(11).mskuId(10).type(PairType.REPORT).countPskuOnMsku(1)
                    .reportPosition(0).reportMatchRate(0.5).sessionId(sessionId),
                new PairBuilder().pskuId(21).mskuId(20).type(PairType.REPORT).countPskuOnMsku(1)
                    .reportPosition(0).reportMatchRate(0.5).sessionId(sessionId))
                .map(PairBuilder::build)
                .collect(Collectors.toList());

        pairDao.createNewTable();
        pairDao.saveToPairNew(pairs);
        pairDao.saveToPairNew(pairs);

        List<Pair> pairsSaved = dsl()
                .fetch(PairDao.PAIR_NEW)
                .into(Pair.class);

        Assertions.assertThat(pairsSaved)
                .usingFieldByFieldElementComparator()
                .containsExactlyInAnyOrderElementsOf(pairs);
    }

    @Test
    public void whenRotatePairTablesOk() {
        Long sessionId = createNewSession(SESSION_NAME);
        List<Pair> pairs = Stream.of(
                new PairBuilder().pskuId(1).mskuId(10).type(PairType.UC).countPskuOnMsku(1)
                    .sessionId(sessionId),
                new PairBuilder().pskuId(2).mskuId(20).type(PairType.REPORT).countPskuOnMsku(1)
                    .reportPosition(0).reportMatchRate(0.5).sessionId(sessionId),
                new PairBuilder().pskuId(3).mskuId(30).type(PairType.REPORT).countPskuOnMsku(1)
                    .reportPosition(1).reportMatchRate(0.1).sessionId(sessionId))
                .map(PairBuilder::build)
                .collect(Collectors.toList());

        pairDao.insert(pairs);

        List<Pair> pairsNew = Stream.of(
                new PairBuilder().pskuId(4).mskuId(40).type(PairType.UC).countPskuOnMsku(1)
                    .sessionId(sessionId),
                new PairBuilder().pskuId(5).mskuId(50).type(PairType.REPORT).countPskuOnMsku(1)
                    .reportPosition(0).reportMatchRate(0.5).sessionId(sessionId),
                new PairBuilder().pskuId(6).mskuId(60).type(PairType.REPORT).countPskuOnMsku(1)
                    .reportPosition(1).reportMatchRate(0.1).sessionId(sessionId))
                .map(PairBuilder::build)
                .collect(Collectors.toList());

        pairDao.createNewTable();
        pairDao.saveToPairNew(pairsNew);
        pairDao.rotateTables();

        List<Pair> pairsOldAfter = dsl()
                .fetch(PairDao.PAIR_OLD)
                .into(Pair.class);

        List<Pair> pairsAfter = pairDao.findAll();

        Assertions.assertThat(pairsOldAfter)
                .usingFieldByFieldElementComparator()
                .containsExactlyInAnyOrderElementsOf(pairs);
        Assertions.assertThat(pairsAfter)
                .usingFieldByFieldElementComparator()
                .containsExactlyInAnyOrderElementsOf(pairsNew);

        Assert.assertFalse(pairDao.checkIfNewTableExists());
    }

    @Test
    public void whenFindPairsToSendSentPairsExcluded() throws IOException {
        Long sessionId = createNewSession(SESSION_NAME);
        List<Pair> pairs = Stream.of(
                new PairBuilder().pskuId(1).mskuId(10).type(PairType.UC).countPskuOnMsku(1)
                    .sessionId(sessionId),
                new PairBuilder().pskuId(2).mskuId(20).type(PairType.REPORT).countPskuOnMsku(1)
                    .reportPosition(0).reportMatchRate(0.5).sessionId(sessionId),
                new PairBuilder().pskuId(3).mskuId(30).type(PairType.REPORT).countPskuOnMsku(1)
                    .reportPosition(1).reportMatchRate(0.1).sessionId(sessionId),
                new PairBuilder().pskuId(4).mskuId(40).type(PairType.UC).countPskuOnMsku(1)
                    .sessionId(sessionId))
                .map(PairBuilder::build)
                .collect(Collectors.toList());
        pairDao.insert(pairs);

        Stream.of(new PairStorageBuilder().pskuId(1).mskuId(10).type(PairType.UC).countPskuOnMsku(1)
                .sessionId(sessionId),
                new PairStorageBuilder().pskuId(2).mskuId(222).type(PairType.UC).countPskuOnMsku(1)
                    .sessionId(sessionId),
                new PairStorageBuilder().pskuId(3).mskuId(333).type(PairType.UC).countPskuOnMsku(1)
                    .state(PairState.FINISHED)
                        .sessionId(sessionId))
                .map(PairStorageBuilder::build)
                .forEach(storedPair ->
                        dsl().insertInto(PAIR_STORAGE)
                                .set(dsl().newRecord(PAIR_STORAGE, storedPair))
                                .execute());

        List<Pair> foundPairs;
        try (CloseableIterator<Pair> iterator = pairDao.findPairsToSend()) {
            foundPairs = StreamSupport
                    .stream(Spliterators.spliteratorUnknownSize(iterator, Spliterator.ORDERED), false)
                    .collect(Collectors.toList());
        }

        List<Pair> expectedPairs = Stream.of(
//                new PairBuilder().pskuId(3).mskuId(30).type(PairType.REPORT).countPskuOnMsku(1)
//                    .reportPosition(1).reportMatchRate(0.1)
//                        .sessionId(sessionId),
                new PairBuilder().pskuId(4).mskuId(40).type(PairType.UC).countPskuOnMsku(1)
                    .sessionId(sessionId))
                .map(PairBuilder::build)
                .collect(Collectors.toList());
        Assertions.assertThat(foundPairs)
                .usingFieldByFieldElementComparator()
                .containsExactlyInAnyOrderElementsOf(expectedPairs);
    }
}
