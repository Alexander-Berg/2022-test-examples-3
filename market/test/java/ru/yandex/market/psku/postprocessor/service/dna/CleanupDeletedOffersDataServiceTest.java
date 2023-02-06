package ru.yandex.market.psku.postprocessor.service.dna;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.ir.autogeneration.common.helpers.ModelStorageHelper;
import ru.yandex.market.mbo.http.ModelStorage;
import ru.yandex.market.psku.postprocessor.bazinga.dna.ActivateModelProcessingTask;
import ru.yandex.market.psku.postprocessor.common.BaseDBTest;
import ru.yandex.market.psku.postprocessor.common.db.dao.DeletedMappingModelsDao;
import ru.yandex.market.psku.postprocessor.common.db.dao.DeletedMappingModelsQueueDao;
import ru.yandex.market.psku.postprocessor.common.db.jooq.enums.CleanupStatus;
import ru.yandex.market.psku.postprocessor.common.db.jooq.tables.pojos.DeletedMappingModels;
import ru.yandex.market.psku.postprocessor.common.db.jooq.tables.pojos.DeletedMappingModelsQueue;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;


public class CleanupDeletedOffersDataServiceTest extends BaseDBTest {

    @Autowired
    DeletedMappingModelsQueueDao deletedMappingModelsQueueDao;

    @Autowired
    DeletedMappingModelsDao deletedMappingModelsDao;

    private CleanupDeletedOffersDataService addDataService;
    private ActivateModelProcessingTask activateTask;
    private ModelStorageHelper modelStorageHelper;

    @Before
    public void setup() {
        modelStorageHelper = Mockito.mock(ModelStorageHelper.class);
        addDataService = new CleanupDeletedOffersDataService(deletedMappingModelsQueueDao, modelStorageHelper);
        activateTask = new ActivateModelProcessingTask(deletedMappingModelsDao, deletedMappingModelsQueueDao);
    }

    @Test
    public void addToProcessTableTest() {
        Timestamp ts = Timestamp.from(Instant.now());
        // проверям что корректно если что-то уже есть в обработке
        deletedMappingModelsDao.insert(
                new DeletedMappingModels(11L, CleanupStatus.READY_FOR_PROCESSING, ts, ts, 111L)
        );
        when(modelStorageHelper.findModels(any())).thenReturn(List.of(
                ModelStorage.Model.newBuilder().setId(1L)
                        .addRelations(ModelStorage.Relation.newBuilder()
                                .setType(ModelStorage.RelationType.SKU_PARENT_MODEL)
                                .setId(11L)
                                .build())
                        .build(),
                ModelStorage.Model.newBuilder().setId(2L)
                        .addRelations(ModelStorage.Relation.newBuilder()
                                .setType(ModelStorage.RelationType.SKU_PARENT_MODEL)
                                .setId(22L)
                                .build())
                        .build()
        ));
        addDataService.addToProcessQueue(List.of(1L, 2L));
        List<DeletedMappingModelsQueue> queueList =
                deletedMappingModelsQueueDao.findAll();
        assertEquals(2, queueList.size());
        activateTask.execute(null);
        List<DeletedMappingModels> queue =
                deletedMappingModelsDao.fetchReadyForProcessingModels(100);
        assertEquals(2, queue.size());
        assertEquals(2, deletedMappingModelsDao.fetchByStatus(CleanupStatus.READY_FOR_PROCESSING).size());
    }

}
