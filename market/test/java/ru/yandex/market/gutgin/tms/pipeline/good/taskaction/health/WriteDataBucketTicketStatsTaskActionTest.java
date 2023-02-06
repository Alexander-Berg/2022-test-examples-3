package ru.yandex.market.gutgin.tms.pipeline.good.taskaction.health;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.gutgin.tms.service.health.TskvWriterMockImpl;
import ru.yandex.market.partner.content.common.DBDcpStateGenerator;
import ru.yandex.market.partner.content.common.db.dao.PipelineDao;
import ru.yandex.market.partner.content.common.db.jooq.tables.pojos.GcSkuTicket;
import ru.yandex.market.partner.content.common.service.health.TskvFields;
import ru.yandex.market.request.trace.TskvRecordBuilder;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author dergachevfv
 * @since 5/21/20
 */
public class WriteDataBucketTicketStatsTaskActionTest extends DBDcpStateGenerator {

    public static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
            .withZone(ZoneId.systemDefault());

    @Autowired
    private PipelineDao pipelineDao;

    private TskvWriterMockImpl tskvWriter;

    private WriteDataBucketTicketStatsTaskAction action;

    @Before
    public void setUp() {
        super.setUp();

        tskvWriter = new TskvWriterMockImpl();

        action = new WriteDataBucketTicketStatsTaskAction(
                gcSkuTicketDao,
                pipelineDao,
                tskvWriter,
                DATE_TIME_FORMATTER
        );
    }

    @Test
    public void testWriteTskvRecordsOk() {
        List<GcSkuTicket> gcSkuTickets = generateDBDcpInitialStateNew(3);

        action.doRun(processDataBucketData);

        List<String> expected = gcSkuTickets.stream()
                .map(this::toTskvRecord)
                .map(TskvRecordBuilder::build)
                .collect(Collectors.toList());

        List<String> actual = tskvWriter.getTskvRecords().stream()
                .map(TskvRecordBuilder::build)
                .collect(Collectors.toList());

        assertThat(actual)
                .containsExactlyInAnyOrderElementsOf(expected);
    }

    private TskvRecordBuilder toTskvRecord(GcSkuTicket ticket) {
        TskvRecordBuilder builder = new TskvRecordBuilder();

        builder.add(TskvFields.DATE.getName(), Instant.now(), DATE_TIME_FORMATTER)
                .add(TskvFields.TICKET_TYPE.getName(), ticket.getType())
                .add(TskvFields.SHOP_SKU.getName(), ticket.getShopSku())
                .add(TskvFields.PARTNER_SKU_ID.getName(), ticket.getPartnerSkuId())
                .add(TskvFields.SOURCE_ID.getName(), ticket.getSourceId())
                .add(TskvFields.CATEGORY_ID.getName(), ticket.getCategoryId())
                .add(TskvFields.CREATE_DATE.getName(), ticket.getCreateDate().toInstant(), DATE_TIME_FORMATTER)
                .add(TskvFields.UPDATE_DATE.getName(), ticket.getUpdateDate().toInstant(), DATE_TIME_FORMATTER)
                .add(TskvFields.RESULT_MBO_PMODEL_ID.getName(), ticket.getResultMboPmodelId())
                .add(TskvFields.RESULT_MBO_PSKU_ID.getName(), ticket.getResultMboPskuId())
                .add(TskvFields.DATA_BUCKET_ID.getName(), ticket.getDataBucketId())
                .add(TskvFields.VALID.getName(), ticket.getValid())
                .add(TskvFields.CREATE_TIME.getName(), ticket.getCreateTime().toInstant(), DATE_TIME_FORMATTER)
                .add(TskvFields.EXISTING_MBO_PMODEL_ID.getName(), ticket.getExistingMboPmodelId())
                .add(TskvFields.EXISTING_MBO_PSKU_ID.getName(), ticket.getExistingMboPskuId())
                .add(TskvFields.PARTNER_SHOP_ID.getName(), ticket.getPartnerShopId())
                .add(TskvFields.TICKET_SOURCE.getName(), ticket.getTicketSource());
        return builder;
    }
}
