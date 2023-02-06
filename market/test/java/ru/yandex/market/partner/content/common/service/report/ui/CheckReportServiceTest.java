package ru.yandex.market.partner.content.common.service.report.ui;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.ir.http.PartnerContentUi;
import ru.yandex.market.partner.content.common.BaseDbCommonTest;
import ru.yandex.market.partner.content.common.db.dao.PipelineDao;
import ru.yandex.market.partner.content.common.db.jooq.enums.LockStatus;
import ru.yandex.market.partner.content.common.db.jooq.enums.PipelineType;
import ru.yandex.market.partner.content.common.db.jooq.tables.daos.LockInfoDao;
import ru.yandex.market.partner.content.common.db.jooq.tables.pojos.LockInfo;
import ru.yandex.market.partner.content.common.db.jooq.tables.pojos.Pipeline;
import ru.yandex.market.partner.content.common.engine.parameter.EmptyData;
import ru.yandex.market.partner.content.common.engine.parameter.Param;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.partner.content.common.db.jooq.enums.PipelineType.DATA_CAMP;
import static ru.yandex.market.partner.content.common.db.jooq.enums.PipelineType.FAST_CARD;

public class CheckReportServiceTest extends BaseDbCommonTest {

    private PipelineDao pipelineDao;
    private LockInfoDao lockInfoDao;
    private LongPipelinesReportService checkReportService;

    @Before
    public void init() {
        lockInfoDao = new LockInfoDao(configuration);
        pipelineDao = new PipelineDao(configuration);
        checkReportService = new LongPipelinesReportService(configuration);
    }

    @Test
    public void testListData() {
        final Pipeline redPipeline = createPipeline(Instant.now().minus(Duration.ofDays(5)));
        final Pipeline redPipeline2 = createPipeline(Instant.now().minus(Duration.ofDays(3)));
        final Pipeline orangePipeline = createPipeline(Instant.now().minus(Duration.ofHours(36)));
        final Pipeline yellowPipeline = createPipeline(Instant.now().minus(Duration.ofHours(12)));

        final Pipeline redFastPipeline = createPipeline(Instant.now().minus(Duration.ofDays(5)), FAST_CARD);
        final Pipeline redFastPipeline2 = createPipeline(Instant.now().minus(Duration.ofDays(3)), FAST_CARD);
        final Pipeline orangeFastPipeline = createPipeline(Instant.now().minus(Duration.ofHours(36)), FAST_CARD);
        final Pipeline yellowFastPipeline = createPipeline(Instant.now().minus(Duration.ofHours(12)), FAST_CARD);

        pipelineDao.insert(redPipeline, redPipeline2, orangePipeline, yellowPipeline);
        pipelineDao.insert(redFastPipeline, redFastPipeline2, orangeFastPipeline, yellowFastPipeline);

        PartnerContentUi.ListLongPipelinesResponse listCheckResponse = checkReportService.listData(
                PartnerContentUi.ListLongPipelinesRequest.newBuilder()
                        .setRequestType(PartnerContentUi.RequestType.ALL)
                        .setOrder(PartnerContentUi.ListLongPipelinesRequest.Order.getDefaultInstance())
                        .setPaging(PartnerContentUi.Paging.newBuilder().setPageSize(10).setStartRow(0).build())
                        .build()
        );

        assertThat(listCheckResponse.getCount()).isEqualTo(8);
        assertThat(listCheckResponse.getDataList().size()).isEqualTo(8);
    }

    private Pipeline createPipeline(Instant instant) {
        return createPipeline(instant, DATA_CAMP);
    }

    private Pipeline createPipeline(Instant instant, PipelineType pipelineType) {
        final Timestamp timestamp = Timestamp.from(instant);
        final LockInfo lockInfo = new LockInfo();
        lockInfo.setStatus(LockStatus.FREE);
        lockInfo.setCreateTime(timestamp);
        lockInfoDao.insert(lockInfo);
        Param emptyData = new EmptyData();
        final Pipeline pipeline = new Pipeline();
        pipeline.setInputData(emptyData);
        pipeline.setType(pipelineType);
        pipeline.setStartDate(timestamp);
        pipeline.setUpdateDate(timestamp);
        pipeline.setLockId(lockInfo.getId());
        return pipeline;
    }

}