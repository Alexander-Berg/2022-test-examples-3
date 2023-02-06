package ru.yandex.market.psku.postprocessor.bazinga.dna;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import com.google.protobuf.Message;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mbo.http.ModelCardApi;
import ru.yandex.market.psku.postprocessor.common.BaseDBTest;
import ru.yandex.market.psku.postprocessor.common.db.dao.DeletedMappingModelsDao;
import ru.yandex.market.psku.postprocessor.common.db.dao.ExternalRequestResponseDao;
import ru.yandex.market.psku.postprocessor.common.db.jooq.enums.CleanupStatus;
import ru.yandex.market.psku.postprocessor.common.db.jooq.enums.RequestStatus;
import ru.yandex.market.psku.postprocessor.common.db.jooq.tables.pojos.DeletedMappingModels;
import ru.yandex.market.psku.postprocessor.common.db.jooq.tables.pojos.ExternalRequestResponse;

import static org.assertj.core.api.Assertions.assertThat;

public class ClearOldDataTaskTest extends BaseDBTest {

    private static final long MODEL_ID1 = 111L;
    private static final long MODEL_ID2 = 112L;
    private static final long MODEL_ID3 = 113L;
    private static final long MODEL_ID4 = 114L;

    private static final long QUEUE_ID = 11L;

    private final Message request = ModelCardApi.SaveModelsGroupRequest.newBuilder().build();

    @Autowired
    private DeletedMappingModelsDao deletedMappingModelsDao;
    @Autowired
    private ExternalRequestResponseDao externalRequestResponseDao;

    private ClearOldDataTask clearOldDataTask;

    @Before
    public void setUp() throws Exception {
        clearOldDataTask = new ClearOldDataTask(deletedMappingModelsDao, externalRequestResponseDao);
    }

    @Test
    public void testSimpleRemoveByDate() {
        externalRequestResponseDao.insert(
            new ExternalRequestResponse(null, MODEL_ID1, request,
                    Timestamp.from(Instant.now().minus(20, ChronoUnit.HOURS)),
                    null, RequestStatus.FINISHED, QUEUE_ID),
            new ExternalRequestResponse(null, MODEL_ID2, request,
                    Timestamp.from(Instant.now().minus(20, ChronoUnit.HOURS)),
                    null, RequestStatus.CREATED, QUEUE_ID),
            new ExternalRequestResponse(null, MODEL_ID3, request,
                    Timestamp.from(Instant.now().minus(25, ChronoUnit.HOURS)),
                    null, RequestStatus.FINISHED, QUEUE_ID),
            new ExternalRequestResponse(null, MODEL_ID4, request,
                    Timestamp.from(Instant.now().minus(25, ChronoUnit.HOURS)),
                    null, RequestStatus.CREATED, QUEUE_ID)
        );

        clearOldDataTask.execute(null);

        List<ExternalRequestResponse> allRequests = externalRequestResponseDao.findAll();

        assertThat(allRequests)
                .extracting("modelId")
                .containsExactlyInAnyOrderElementsOf(List.of(MODEL_ID1, MODEL_ID2, MODEL_ID4));
    }


    @Test
    public void testRemoveNotInProcessing() {
        Timestamp ts = Timestamp.from(Instant.now());
        deletedMappingModelsDao.insert(
                new DeletedMappingModels(MODEL_ID1, CleanupStatus.READY_FOR_PROCESSING,  ts, ts, QUEUE_ID),
                new DeletedMappingModels(MODEL_ID3, CleanupStatus.READY_FOR_PROCESSING,  ts, ts, QUEUE_ID)
        );
        externalRequestResponseDao.insert(
                new ExternalRequestResponse(null, MODEL_ID1, request,
                        Timestamp.from(Instant.now().minus(20, ChronoUnit.HOURS)),
                        null, RequestStatus.FINISHED, QUEUE_ID),
                new ExternalRequestResponse(null, MODEL_ID2, request,
                        Timestamp.from(Instant.now().minus(20, ChronoUnit.HOURS)),
                        null, RequestStatus.FINISHED, QUEUE_ID),
                new ExternalRequestResponse(null, MODEL_ID3, request,
                        Timestamp.from(Instant.now().minus(25, ChronoUnit.HOURS)),
                        null, RequestStatus.FINISHED, QUEUE_ID),
                new ExternalRequestResponse(null, MODEL_ID4, request,
                        Timestamp.from(Instant.now().minus(25, ChronoUnit.HOURS)),
                        null, RequestStatus.FINISHED, QUEUE_ID)
        );

        clearOldDataTask.execute(null);

        List<ExternalRequestResponse> allRequests = externalRequestResponseDao.findAll();

        assertThat(allRequests)
                .extracting("modelId")
                .containsExactlyInAnyOrderElementsOf(List.of(MODEL_ID1, MODEL_ID2, MODEL_ID3));
    }
}
