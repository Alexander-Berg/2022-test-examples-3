package ru.yandex.market.psku.postprocessor.service.dna;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.List;

import org.assertj.core.groups.Tuple;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.psku.postprocessor.common.BaseDBTest;
import ru.yandex.market.psku.postprocessor.common.db.dao.DeletedMappingModelsDao;
import ru.yandex.market.psku.postprocessor.common.db.dao.DeletedMappingModelsResultDao;
import ru.yandex.market.psku.postprocessor.common.db.jooq.enums.CleanupStatus;
import ru.yandex.market.psku.postprocessor.common.db.jooq.tables.pojos.DeletedMappingModelsResult;
import ru.yandex.market.psku.postprocessor.common.db.jooq.tables.pojos.DeletedMappingModels;
import ru.yandex.market.psku.postprocessor.common.db.jooq.tables.pojos.DeletedMappingModelsQueue;

import static org.assertj.core.api.Assertions.assertThat;

public class CleanupDeletedOffersStatusServiceTest extends BaseDBTest {

    private static final long MODEL_ID1 = 11L;
    private static final long MODEL_ID2 = 12L;
    private static final long MODEL_ID3 = 13L;
    private static final long MODEL_ID4 = 14L;
    private static final long MODEL_ID5 = 15L;

    private static final long QUEUE_ID = 111L;

    @Autowired
    private DeletedMappingModelsDao deletedMappingModelsDao;

    @Autowired
    private DeletedMappingModelsResultDao deletedMappingModelsResultDao;

    private CleanupDeletedOffersStatusService cleanupDeletedOffersStatusService;

    private final Timestamp create_ts = Timestamp.from(Instant.now().minus(1, ChronoUnit.MINUTES));
    private final Timestamp update_ts = Timestamp.from(Instant.now());

    @Before
    public void setUp() {
        cleanupDeletedOffersStatusService = new CleanupDeletedOffersStatusService(deletedMappingModelsDao,
                deletedMappingModelsResultDao);
    }

    @Test
    public void whenRequestStatusOfModelInDifferentStageThenReturnForAllRequested() {
        prepareModelInProgress();
        prepareFinishedModels();
        List<Long> requestModelIds = List.of(MODEL_ID1, MODEL_ID3, MODEL_ID5);

        Collection<CleanupDeletedOffersStatusService.ModelStatus> modelStatuses =
                cleanupDeletedOffersStatusService.getModelStatuses(requestModelIds);

        assertThat(modelStatuses).hasSize(3);
        assertThat(modelStatuses)
                .extracting("modelId", "status", "timestamp")
                .containsExactlyInAnyOrder(
                    new Tuple(MODEL_ID1, CleanupStatus.READY_FOR_PROCESSING, create_ts.getTime()),
                    new Tuple(MODEL_ID3, CleanupStatus.NO_MAPPINGS, update_ts.getTime()),
                    // для моделей которые нигде не нашли надо вернуть статус null
                    new Tuple(MODEL_ID5, null, null)
                );
    }

    private void prepareModelInProgress() {
        List<DeletedMappingModels> models = List.of(
            new DeletedMappingModels(MODEL_ID1, CleanupStatus.READY_FOR_PROCESSING, update_ts, create_ts, QUEUE_ID)
        );

        deletedMappingModelsDao.insert(models);
    }

    private void prepareFinishedModels() {
        List<DeletedMappingModelsResult> models = List.of(
                new DeletedMappingModelsResult(1L, QUEUE_ID, MODEL_ID3, CleanupStatus.NO_MAPPINGS,
                        create_ts, update_ts),
                new DeletedMappingModelsResult(2L, QUEUE_ID, MODEL_ID4, CleanupStatus.SUCCESS,
                        create_ts, update_ts)
        );
        deletedMappingModelsResultDao.insert(models);
    }
}
