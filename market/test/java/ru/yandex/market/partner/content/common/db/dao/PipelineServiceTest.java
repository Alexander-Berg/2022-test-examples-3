package ru.yandex.market.partner.content.common.db.dao;

import org.jooq.Configuration;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import ru.yandex.market.partner.content.common.BaseDbCommonTest;
import ru.yandex.market.partner.content.common.db.jooq.enums.LockStatus;
import ru.yandex.market.partner.content.common.db.jooq.enums.MrgrienPipelineStatus;
import ru.yandex.market.partner.content.common.db.jooq.enums.PipelineType;
import ru.yandex.market.partner.content.common.db.jooq.tables.pojos.Pipeline;
import ru.yandex.market.partner.content.common.engine.parameter.ProcessDataBucketData;
import ru.yandex.market.partner.content.common.engine.parameter.RequestProcessSimpleExcelData;
import ru.yandex.market.partner.content.common.entity.PriorityIdentityWrapper;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.partner.content.common.db.jooq.Tables.LOCK_INFO;
import static ru.yandex.market.partner.content.common.db.jooq.Tables.PIPELINE;
import static ru.yandex.market.partner.content.common.db.jooq.Tables.SERVICE_INSTANCE;

public class PipelineServiceTest extends BaseDbCommonTest {
    @Autowired
    @Qualifier("jooq.config.configuration")
    Configuration configuration;

    @Autowired
    PipelineService pipelineService;

    @Test
    public void testCreatePipeline() {
        RequestProcessSimpleExcelData data = new RequestProcessSimpleExcelData();
        data.setRequestId(1);
        final int version = 2;
        final long pipelineId = pipelineService.createPipeline(data, PipelineType.SINGLE_XLS, version);
        final Pipeline pipeline = pipelineService.getPipeline(pipelineId);
        assertNewPipeline(pipeline, pipelineId, data, version);
    }

    @Test()
    public void testGetAvailableForProcess() {
        RequestProcessSimpleExcelData data = new RequestProcessSimpleExcelData();
        data.setRequestId(1);
        final int version = 3;
        final long pipelineId = pipelineService.createPipeline(data, PipelineType.SINGLE_XLS, version);
        List<PriorityIdentityWrapper> pipelineIds = pipelineService.getAvailableForProcessPipelineIds(null, null, null);
        final Optional<Long> pipelineIdOptional = pipelineIds.stream()
            .filter(v -> v.getId() == pipelineId)
            .findAny().map(PriorityIdentityWrapper::getId);
        Assert.assertEquals(true, pipelineIdOptional.isPresent());
        final Pipeline pipeline = pipelineService.getPipeline(pipelineIdOptional.get());
        assertNewPipeline(pipeline, pipelineId, data, version);
    }

    @Test
    public void testShardGetAvailableForProcess() {
        // для теста нужны инстансы
        dsl().insertInto(SERVICE_INSTANCE)
            .values(1L, "host1", 80, true, Instant.now())
            .values(2L, "host2", 80, true, Instant.now())
            .execute();

        List<Long> oddPipes = List.of(1001L, 1003L);
        List<Long> evenPipes = List.of(1000L, 1002L);

        Stream.concat(oddPipes.stream(), evenPipes.stream()).forEach(this::createPipelineWithLock);

        List<Long> pipelineIds =
            pipelineService.getAvailableForProcessPipelineIds(1L, 1L, 0L)
                .stream().map(PriorityIdentityWrapper::getId).collect(Collectors.toList());

        assertThat(pipelineIds).containsAll(evenPipes)
                .doesNotContainAnyElementsOf(oddPipes);

    }


    private void createPipelineWithLock(Long pipelineId) {
        Timestamp now = Timestamp.from(Instant.now());
        dsl().insertInto(LOCK_INFO, LOCK_INFO.ID, LOCK_INFO.STATUS, LOCK_INFO.CREATE_TIME, LOCK_INFO.UPDATE_TIME)
            .values(pipelineId, LockStatus.FREE, now, now)
            .execute();
        dsl().insertInto(PIPELINE, PIPELINE.ID, PIPELINE.STATUS, PIPELINE.LOCK_ID, PIPELINE.TYPE, PIPELINE.INPUT_DATA
            , PIPELINE.START_DATE, PIPELINE.UPDATE_DATE, PIPELINE.VERSION, PIPELINE.PRIORITY, PIPELINE.DATA_BUCKET_ID)
            .values(pipelineId, MrgrienPipelineStatus.NEW, pipelineId, PipelineType.DATA_CAMP,
                new ProcessDataBucketData(), now, now, 3, 0, null)
            .execute();
    }

    private void assertNewPipeline(Pipeline pipeline, long expectedPipelineId,
                                   RequestProcessSimpleExcelData expectedData,
                                   Integer version) {
        Assert.assertEquals(expectedPipelineId, pipeline.getId().longValue());
        Assert.assertEquals(MrgrienPipelineStatus.NEW, pipeline.getStatus());
        Assert.assertEquals(PipelineType.SINGLE_XLS, pipeline.getType());
        Assert.assertEquals(expectedData, pipeline.getInputData());
        Assert.assertEquals(version, pipeline.getVersion());
        Assert.assertNotNull(pipeline.getLockId());
    }
}