package ru.yandex.market.partner.content.common.service.report.ui;

import org.jooq.Record;
import org.jooq.Result;
import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.ir.http.PartnerContentUi;
import ru.yandex.market.partner.content.common.DBStateGenerator;
import ru.yandex.market.partner.content.common.db.jooq.enums.DataBucketMessageType;
import ru.yandex.market.partner.content.common.db.jooq.enums.FileType;
import ru.yandex.market.partner.content.common.db.jooq.enums.ProcessType;
import ru.yandex.market.partner.content.common.db.jooq.tables.Source;
import ru.yandex.market.partner.content.common.db.jooq.tables.records.GcMessageReportFactsRecord;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static ru.yandex.market.partner.content.common.db.jooq.Tables.ABSTRACT_PROCESS;
import static ru.yandex.market.partner.content.common.db.jooq.Tables.DATA_BUCKET;
import static ru.yandex.market.partner.content.common.db.jooq.Tables.DATA_BUCKET_FILE_PROCESS;
import static ru.yandex.market.partner.content.common.db.jooq.Tables.DATA_BUCKET_MESSAGE;
import static ru.yandex.market.partner.content.common.db.jooq.Tables.GC_MESSAGE_REPORT_FACTS;
import static ru.yandex.market.partner.content.common.db.jooq.Tables.PROTOCOL_MESSAGE;
import static ru.yandex.market.partner.content.common.db.jooq.Tables.SOURCE;
import static ru.yandex.market.partner.content.common.db.jooq.Tables.TICKET_PROCESS_REQUEST;

/**
 * @author dergachevfv
 * @since 6/24/20
 */
public class GcMessageReportServiceTest extends DBStateGenerator {

    private GcMessageReportService gcMessageReportService;

    @Before
    public void setUp() {
        super.setUp();
        gcMessageReportService = new GcMessageReportService(configuration, slaveConfiguration);
    }

    @Test
    public void listDataWithFiltersSmokeTest() {
        PartnerContentUi.ListGcMessageRequest.Builder builder = PartnerContentUi.ListGcMessageRequest.newBuilder();
        Stream.of(PartnerContentUi.ListGcMessageRequest.Filter.Column.values())
                .forEach(column -> builder.addFilter(
                        PartnerContentUi.ListGcMessageRequest.Filter.newBuilder()
                                .setColumn(column)
                                .setValue("123")
                ));

        gcMessageReportService.listData(
                builder.build()
        );
    }

    @Test
    public void listDataWithOrderSmokeTest() {
        PartnerContentUi.ListGcMessageRequest.Builder builder = PartnerContentUi.ListGcMessageRequest.newBuilder();

        // default order
        gcMessageReportService.listData(
                builder.build()
        );

        Stream.of(PartnerContentUi.ListGcMessageRequest.Order.Column.values())
                .forEach(column -> gcMessageReportService.listData(
                        builder.setOrder(PartnerContentUi.ListGcMessageRequest.Order.newBuilder()
                                .setColumn(column))
                                .build()
                ));
    }

    @Test
    public void actualizeReportSmokeTest() {
        gcMessageReportService.actualizeReport();
    }

    @Test
    public void actualizeReportTestOk() {
        long protocolIdToSave = 2L;
        String protocolCodeToSave = "Test_code_2";

        Record beforeActualizeRecord = dsl().select()
                .from(GC_MESSAGE_REPORT_FACTS)
                .where(GC_MESSAGE_REPORT_FACTS.PROTOCOL_MESSAGE_ID.eq(protocolIdToSave))
                .fetchOne();
        assertNull(beforeActualizeRecord);

        dsl().insertInto(PROTOCOL_MESSAGE)
                .columns(
                        PROTOCOL_MESSAGE.ID,
                        PROTOCOL_MESSAGE.CODE,
                        PROTOCOL_MESSAGE.PARAMS
                )
                .values(1L, "Test_code_1", new HashMap<>())
                .values(protocolIdToSave, protocolCodeToSave, new HashMap<>())
                .values(3L, "Test_code_3", new HashMap<>())
                .values(4L, "Test_code_4", new HashMap<>())
                .execute();

        dsl().insertInto(GC_MESSAGE_REPORT_FACTS)
                .columns(
                        GC_MESSAGE_REPORT_FACTS.ID,
                        GC_MESSAGE_REPORT_FACTS.PROTOCOL_MESSAGE_ID,
                        GC_MESSAGE_REPORT_FACTS.CODE
                )
                .values(1L, 1L, "Test_code_1")
                .execute();
        prepareActualizeReportFor(2);

        gcMessageReportService.actualizeReport();

        Record resultRecord = dsl().select()
                .from(GC_MESSAGE_REPORT_FACTS)
                .where(GC_MESSAGE_REPORT_FACTS.PROTOCOL_MESSAGE_ID.eq(protocolIdToSave))
                .fetchOne();

        assertNotNull(resultRecord);
        assertEquals(protocolCodeToSave, resultRecord.get(PROTOCOL_MESSAGE.CODE));
    }

    @Test
    public void actualizeReportWillProcessRecordsWithIdSequenceGap() {
        dsl().insertInto(PROTOCOL_MESSAGE)
                .columns(
                        PROTOCOL_MESSAGE.ID,
                        PROTOCOL_MESSAGE.CODE,
                        PROTOCOL_MESSAGE.PARAMS
                )
                .values(1L, "Test_code_1", new HashMap<>())
                .values((long) Integer.MAX_VALUE, "Huge_gap_code", new HashMap<>())
                .execute();

        prepareActualizeReportFor(1);
        prepareActualizeReportFor(Integer.MAX_VALUE);

        gcMessageReportService.actualizeReport();

        Result<GcMessageReportFactsRecord> records = dsl().fetch(GC_MESSAGE_REPORT_FACTS);
        List<Long> recordsValues = records.getValues(GC_MESSAGE_REPORT_FACTS.PROTOCOL_MESSAGE_ID, Long.class);

        assertEquals(2, recordsValues.size());
        assertTrue(recordsValues.contains(1L));
        assertTrue(recordsValues.contains((long) Integer.MAX_VALUE));
    }

    private void prepareActualizeReportFor(Integer protocolId) {
        long testDefaultId = protocolId.longValue();

        Source BUCKET_SOURCE = SOURCE.as("BUCKET_SOURCE");
        dsl().insertInto(BUCKET_SOURCE)
                .columns(
                        BUCKET_SOURCE.SOURCE_NAME,
                        BUCKET_SOURCE.SOURCE_ID
                )
                .values("Test bucket source", protocolId)
                .execute();

        dsl().insertInto(DATA_BUCKET)
                .columns(
                        DATA_BUCKET.ID,
                        DATA_BUCKET.SOURCE_ID,
                        DATA_BUCKET.CATEGORY_ID
                )
                .values(testDefaultId, protocolId, testDefaultId)
                .execute();

        dsl().insertInto(DATA_BUCKET_MESSAGE)
                .columns(
                        DATA_BUCKET_MESSAGE.TYPE,
                        DATA_BUCKET_MESSAGE.PROTOCOL_MESSAGE_ID,
                        DATA_BUCKET_MESSAGE.DATA_BUCKET_ID
                )
                .values(DataBucketMessageType.IMAGE_VALIDATION, testDefaultId, testDefaultId)
                .execute();

        dsl().insertInto(ABSTRACT_PROCESS)
                .columns(
                        ABSTRACT_PROCESS.ID,
                        ABSTRACT_PROCESS.REQUEST_ID,
                        ABSTRACT_PROCESS.PROCESS_TYPE
                )
                .values(testDefaultId, testDefaultId, ProcessType.BETTER_FILE_PROCESS)
                .execute();

        dsl().insertInto(DATA_BUCKET_FILE_PROCESS)
                .columns(
                        DATA_BUCKET_FILE_PROCESS.DATA_BUCKET_ID,
                        DATA_BUCKET_FILE_PROCESS.FILE_PROCESS_ID,
                        DATA_BUCKET_FILE_PROCESS.CATEGORY_ID
                )
                .values(testDefaultId, testDefaultId, testDefaultId)
                .execute();

        dsl().insertInto(TICKET_PROCESS_REQUEST)
                .columns(
                        TICKET_PROCESS_REQUEST.ID,
                        TICKET_PROCESS_REQUEST.SOURCE_ID,
                        TICKET_PROCESS_REQUEST.FILE_TYPE,
                        TICKET_PROCESS_REQUEST.CREATE_TIME
                )
                .values(testDefaultId, protocolId, FileType.DCP_SINGLE_EXCEL, Timestamp.from(Instant.now()))
                .execute();
    }
}
