package ru.yandex.market.psku.postprocessor.common.db.dao;

import org.assertj.core.api.Assertions;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.yandex.market.psku.postprocessor.common.BaseDBTest;
import ru.yandex.market.psku.postprocessor.common.db.jooq.enums.MatchTarget;
import ru.yandex.market.psku.postprocessor.common.db.jooq.enums.ProcessingResult;
import ru.yandex.market.psku.postprocessor.common.db.jooq.enums.PskuStorageState;
import ru.yandex.market.psku.postprocessor.common.db.jooq.tables.pojos.PskuResultStorage;
import ru.yandex.market.psku.postprocessor.common.db.jooq.tables.pojos.PskuToModel;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import static ru.yandex.market.psku.postprocessor.common.db.jooq.Tables.PAIR;

public class PskuToModelDaoTest extends BaseDBTest {
    @Autowired
    private PskuToModelDao pskuToModelDao;
    @Autowired
    PskuResultStorageDao pskuResultStorageDao;

    private static final String SESSION_NAME = "testsession";

    @Test
    public void whenCreatePairNewTableIfNotExistOk() {
        pskuToModelDao.createNewTable();

        dsl().selectCount()
            .from(PAIR);
    }

    @Test
    public void whenCreateDropCreatePairNewTableOk() {
        pskuToModelDao.createNewTable();
        pskuToModelDao.dropNewTable();
        pskuToModelDao.createNewTable();
    }

    @Test
    public void whenSaveNewPskuToModelsOk() {
        Long sessionId = createNewSession(SESSION_NAME);
        List<PskuToModel> pskuToModels = Arrays.asList(
            new PskuToModel(10L, 11L, 100L, 100L, sessionId, MatchTarget.MODEL),
            new PskuToModel(11L, 12L, 100L, 100L, sessionId, MatchTarget.MODEL)
            );

        pskuToModelDao.createNewTable();
        pskuToModelDao.saveToNew(pskuToModels);

        List<PskuToModel> pskuToModelsSaved = dsl()
            .fetch(PskuToModelDao.PSKU_TO_MODEL_NEW)
            .into(PskuToModel.class);

        Assertions.assertThat(pskuToModelsSaved)
            .usingFieldByFieldElementComparator()
            .containsExactlyInAnyOrderElementsOf(pskuToModels);
    }

    @Test
    public void whenSaveNewPairsDuplicatedOd() {
        Long sessionId = createNewSession(SESSION_NAME);
        List<PskuToModel> pskuToModels = Arrays.asList(
            new PskuToModel(10L, 11L, 100L, 100L, sessionId, MatchTarget.MODEL),
            new PskuToModel(11L, 12L, 100L, 100L, sessionId, MatchTarget.MODEL)
        );

        pskuToModelDao.createNewTable();
        pskuToModelDao.saveToNew(pskuToModels);
        pskuToModelDao.saveToNew(pskuToModels);

        List<PskuToModel> pairsSaved = dsl()
            .fetch(PskuToModelDao.PSKU_TO_MODEL_NEW)
            .into(PskuToModel.class);

        Assertions.assertThat(pairsSaved)
            .usingFieldByFieldElementComparator()
            .containsExactlyInAnyOrderElementsOf(pskuToModels);
    }

    @Test
    public void whenRotateTablesFiltersAlreadyProcessedPskus() {
        Long sessionId = createNewSession(SESSION_NAME);
        List<PskuToModel> pskuToModels = Arrays.asList(
            new PskuToModel(10L, 11L, 100L, 100L, sessionId, MatchTarget.MODEL),
            new PskuToModel(11L, 12L, 100L, 100L, sessionId, MatchTarget.MODEL)
        );

        pskuToModelDao.insert(pskuToModels);

        PskuToModel alreadyProcessedModel = new PskuToModel(12L, 11L, 100L, 100L, sessionId, MatchTarget.MODEL);
        PskuToModel notAlreadyProcessedModel = new PskuToModel(13L, 12L, 100L, 100L, sessionId, MatchTarget.MODEL);
        List<PskuToModel> pskuToModelsNew = Arrays.asList(
            alreadyProcessedModel,
            notAlreadyProcessedModel
        );

        pskuResultStorageDao.insert(new PskuResultStorage(
            null, 12L, 100L, ProcessingResult.NEED_INFO, null,
            Timestamp.from(Instant.now()), null, PskuStorageState.NEED_INFO, null, null, null, null));

        pskuToModelDao.createNewTable();
        pskuToModelDao.saveToNew(pskuToModelsNew);
        pskuToModelDao.rotateTables();

        List<PskuToModel> pairsOldAfter = dsl()
            .fetch(PskuToModelDao.PSKU_TO_MODEL_OLD)
            .into(PskuToModel.class);

        List<PskuToModel> pairsAfter = pskuToModelDao.findAll();

        Assertions.assertThat(pairsOldAfter)
            .usingFieldByFieldElementComparator()
            .containsExactlyInAnyOrderElementsOf(pskuToModels);
        Assertions.assertThat(pairsAfter)
            .usingFieldByFieldElementComparator()
            .containsExactlyInAnyOrderElementsOf(Arrays.asList(notAlreadyProcessedModel));

        Assert.assertFalse(pskuToModelDao.checkIfNewTableExists());
    }

    @Test
    public void whenRotatePairTablesOk() {
        Long sessionId = createNewSession(SESSION_NAME);
        List<PskuToModel> pskuToModels = Arrays.asList(
            new PskuToModel(10L, 11L, 100L, 100L, sessionId, MatchTarget.MODEL),
            new PskuToModel(11L, 12L, 100L, 100L, sessionId, MatchTarget.MODEL)
        );

        pskuToModelDao.insert(pskuToModels);

        List<PskuToModel> pskuToModelsNew = Arrays.asList(
            new PskuToModel(12L, 11L, 100L, 100L, sessionId, MatchTarget.MODEL),
            new PskuToModel(13L, 12L, 100L, 100L, sessionId, MatchTarget.MODEL)
        );

        pskuToModelDao.createNewTable();
        pskuToModelDao.saveToNew(pskuToModelsNew);
        pskuToModelDao.rotateTables();

        List<PskuToModel> pairsOldAfter = dsl()
            .fetch(PskuToModelDao.PSKU_TO_MODEL_OLD)
            .into(PskuToModel.class);

        List<PskuToModel> pairsAfter = pskuToModelDao.findAll();

        Assertions.assertThat(pairsOldAfter)
            .usingFieldByFieldElementComparator()
            .containsExactlyInAnyOrderElementsOf(pskuToModels);
        Assertions.assertThat(pairsAfter)
            .usingFieldByFieldElementComparator()
            .containsExactlyInAnyOrderElementsOf(pskuToModelsNew);

        Assert.assertFalse(pskuToModelDao.checkIfNewTableExists());
    }


}