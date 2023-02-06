package ru.yandex.market.gutgin.tms.pipeline.util;

import java.sql.Timestamp;
import java.time.Instant;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.market.gutgin.tms.config.TrackerServicesConfig;
import ru.yandex.market.gutgin.tms.service.StartrekService;
import ru.yandex.market.mboc.common.utils.PGaaSZonkyInitializer;
import ru.yandex.market.partner.content.common.DBDcpStateGenerator;
import ru.yandex.market.partner.content.common.config.CommonTestConfig;
import ru.yandex.market.partner.content.common.db.dao.PipelineDao;
import ru.yandex.market.partner.content.common.db.jooq.enums.MrgrienPipelineStatus;
import ru.yandex.market.partner.content.common.db.jooq.enums.PipelineType;
import ru.yandex.market.partner.content.common.db.jooq.tables.pojos.Pipeline;
import ru.yandex.market.partner.content.common.engine.parameter.EmptyData;
import ru.yandex.market.partner.content.common.engine.parameter.Param;


@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(initializers = PGaaSZonkyInitializer.class,
        classes = {
                CommonTestConfig.class,
                TrackerServicesConfig.class
        })
@Ignore
public class UpdateOfferMetricsOnStartrekActionTest extends DBDcpStateGenerator {

    private UpdateOfferMetricsOnStartrekAction action;
    @Autowired
    private StartrekService startrekService;
    @Autowired
    PipelineDao pipelineDao;

    @Before
    public void setUp() {
        super.setUp();
        action = new UpdateOfferMetricsOnStartrekAction(pipelineDao, startrekService,
                null, 10, true, 111);
    }

    @Test
    public void whenRunningPipeWithTicketExistsThenUpdateStTicket() {
        generateDBDcpInitialStateNew(1, states -> {});
        pipelineDao.insert(createPipeline(Instant.now(), PipelineType.CSKU, "MCR-4115"));
        action.runAction();
    }

    private Pipeline createPipeline(Instant instant, PipelineType pipelineType, String stTicketNumber) {
        final Timestamp timestamp = Timestamp.from(instant);
        Param emptyData = new EmptyData();
        final Pipeline pipeline = new Pipeline();
        pipeline.setInputData(emptyData);
        pipeline.setType(pipelineType);
        pipeline.setStartDate(timestamp);
        pipeline.setUpdateDate(timestamp);
        pipeline.setStatus(MrgrienPipelineStatus.RUNNING);
        pipeline.setStTicketNumber(stTicketNumber);
        pipeline.setDataBucketId(dataBucketId);
        return pipeline;
    }
}
