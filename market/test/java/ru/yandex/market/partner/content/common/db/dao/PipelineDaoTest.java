package ru.yandex.market.partner.content.common.db.dao;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.partner.content.common.BaseDbCommonTest;
import ru.yandex.market.partner.content.common.db.jooq.enums.LockStatus;
import ru.yandex.market.partner.content.common.db.jooq.enums.PipelineType;
import ru.yandex.market.partner.content.common.db.jooq.tables.daos.LockInfoDao;
import ru.yandex.market.partner.content.common.db.jooq.tables.pojos.LockInfo;
import ru.yandex.market.partner.content.common.db.jooq.tables.pojos.Pipeline;
import ru.yandex.market.partner.content.common.engine.parameter.EmptyData;
import ru.yandex.market.partner.content.common.engine.parameter.Param;
import ru.yandex.market.partner.content.common.entity.ExtMonitoringColor;
import ru.yandex.utils.Pair;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.partner.content.common.db.jooq.enums.PipelineType.DATA_CAMP;
import static ru.yandex.market.partner.content.common.db.jooq.enums.PipelineType.FAST_CARD;
import static ru.yandex.market.partner.content.common.entity.ExtMonitoringColor.ORANGE;
import static ru.yandex.market.partner.content.common.entity.ExtMonitoringColor.RED;
import static ru.yandex.market.partner.content.common.entity.ExtMonitoringColor.YELLOW;
import static ru.yandex.utils.Pair.makePair;

public class PipelineDaoTest extends BaseDbCommonTest {
    private PipelineDao pipelineDao;
    private LockInfoDao lockInfoDao;

    @Before
    public void init() {
        lockInfoDao = new LockInfoDao(configuration);
        pipelineDao = new PipelineDao(configuration);
    }

    @Test
    public void longPipelinesMonitoring() {
        final Pipeline redPipeline = createPipeline(Instant.now().minus(Duration.ofDays(5)));
        final Pipeline redPipeline2 = createPipeline(Instant.now().minus(Duration.ofDays(3)));
        final Pipeline orangePipeline = createPipeline(Instant.now().minus(Duration.ofHours(36)));
        final Pipeline yellowPipeline = createPipeline(Instant.now().minus(Duration.ofHours(12)));

        pipelineDao.insert(redPipeline, redPipeline2, orangePipeline, yellowPipeline);

        EnumMap<ExtMonitoringColor, Integer> longPipelinesMonitoring = pipelineDao.longPipelinesMonitoring();
        assertThat(longPipelinesMonitoring.values()).allMatch(i -> i > 0);
        assertThat(longPipelinesMonitoring.get(RED)).isEqualTo(2);
        assertThat(longPipelinesMonitoring.get(ExtMonitoringColor.ORANGE)).isEqualTo(1);
        assertThat(longPipelinesMonitoring.get(ExtMonitoringColor.YELLOW)).isEqualTo(1);

        Map<ExtMonitoringColor, Integer> ticketCountByColor = pipelineDao.pipelinesTicketsCountByColor();
        assertThat(ticketCountByColor.size()).isEqualTo(3);
        assertThat(ticketCountByColor.containsKey(RED)).isTrue();
        assertThat(ticketCountByColor.containsKey(ORANGE)).isTrue();
        assertThat(ticketCountByColor.containsKey(YELLOW)).isTrue();
        assertThat(ticketCountByColor.get(RED)).isEqualTo(2);
        assertThat(ticketCountByColor.get(ORANGE)).isEqualTo(1);
        assertThat(ticketCountByColor.get(YELLOW)).isEqualTo(1);

        final Pipeline redFastPipeline = createPipeline(Instant.now().minus(Duration.ofDays(5)), FAST_CARD);
        final Pipeline redFastPipeline2 = createPipeline(Instant.now().minus(Duration.ofDays(3)), FAST_CARD);
        final Pipeline orangeFastPipeline = createPipeline(Instant.now().minus(Duration.ofHours(36)), FAST_CARD);
        final Pipeline yellowFastPipeline = createPipeline(Instant.now().minus(Duration.ofHours(12)), FAST_CARD);

        pipelineDao.insert(redFastPipeline, redFastPipeline2, orangeFastPipeline, yellowFastPipeline);

        longPipelinesMonitoring = pipelineDao.longPipelinesMonitoring();
        assertThat(longPipelinesMonitoring.get(RED)).isEqualTo(4);
        assertThat(longPipelinesMonitoring.get(ExtMonitoringColor.ORANGE)).isEqualTo(2);
        assertThat(longPipelinesMonitoring.get(ExtMonitoringColor.YELLOW)).isEqualTo(2);

        HashMap<Pair<PipelineType, ExtMonitoringColor>, Integer> longPipelinesMonitoringByType =
                pipelineDao.longPipelinesMonitoringByType();

        assertThat(longPipelinesMonitoringByType.get(makePair(FAST_CARD, RED))).isEqualTo(2);
        assertThat(longPipelinesMonitoringByType.get(makePair(FAST_CARD, ORANGE))).isEqualTo(1);
        assertThat(longPipelinesMonitoringByType.get(makePair(FAST_CARD, YELLOW))).isEqualTo(1);
        assertThat(longPipelinesMonitoringByType.get(makePair(DATA_CAMP, RED))).isEqualTo(2);
        assertThat(longPipelinesMonitoringByType.get(makePair(DATA_CAMP, ORANGE))).isEqualTo(1);
        assertThat(longPipelinesMonitoringByType.get(makePair(DATA_CAMP, YELLOW))).isEqualTo(1);

        ticketCountByColor = pipelineDao.pipelinesTicketsCountByColor();
        assertThat(ticketCountByColor.size()).isEqualTo(3);
        assertThat(ticketCountByColor.containsKey(RED)).isTrue();
        assertThat(ticketCountByColor.containsKey(ORANGE)).isTrue();
        assertThat(ticketCountByColor.containsKey(YELLOW)).isTrue();
        assertThat(ticketCountByColor.get(RED)).isEqualTo(4);
        assertThat(ticketCountByColor.get(ORANGE)).isEqualTo(2);
        assertThat(ticketCountByColor.get(YELLOW)).isEqualTo(2);
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
        pipeline.setTicketsCount(1);
        return pipeline;
    }
}