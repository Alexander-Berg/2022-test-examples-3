package ru.yandex.market.partner.content.common.service.report.ui;

import com.google.common.collect.Maps;
import org.jooq.DSLContext;
import org.jooq.InsertSetStep;
import org.jooq.Result;
import org.jooq.impl.DSL;
import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.ir.http.PartnerContentUi;
import ru.yandex.market.partner.content.common.DBStateGenerator;
import ru.yandex.market.partner.content.common.db.jooq.Tables;
import ru.yandex.market.partner.content.common.db.jooq.enums.FileProcessMessageType;
import ru.yandex.market.partner.content.common.db.jooq.enums.FileProcessState;
import ru.yandex.market.partner.content.common.db.jooq.enums.FileType;
import ru.yandex.market.partner.content.common.db.jooq.enums.ProcessType;
import ru.yandex.market.partner.content.common.db.jooq.tables.records.DataBucketRecord;
import ru.yandex.market.partner.content.common.db.jooq.tables.records.ProtocolMessageRecord;

import java.sql.Timestamp;
import java.util.*;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.partner.content.common.db.jooq.Tables.*;

public class MessageReportServiceTest extends DBStateGenerator {

    private MessageReportService messageReportService;

    @Before
    public void setUp() {
        super.setUp();
        setupModel();
        messageReportService = new MessageReportService(configuration);
    }

    private void setupModel() {
        DSLContext dslContext = DSL.using(configuration);

        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("shopSKUs", Collections.singletonList(222L));
        paramMap.put("modelName", "red brick");
        paramMap.put("categoryId", 35L);
        paramMap.put("categoryName", "brick");
        paramMap.put("marketModelId", 161L);
        dslContext.insertInto(Tables.PROTOCOL_MESSAGE)
                .set(Tables.PROTOCOL_MESSAGE.ID, 17L)
                .set(PROTOCOL_MESSAGE.PARAMS, paramMap)
                .set(Tables.PROTOCOL_MESSAGE.CODE, "ir.partner_content.warn.model_exist_in_another_category")
                .execute();

        dslContext.insertInto(Tables.SOURCE)
                .set(SOURCE.SOURCE_ID, 93268)
                .set(SOURCE.SOURCE_NAME, "source 25")
                .execute();

        dslContext.insertInto(Tables.FILE_DATA_PROCESS_REQUEST)
                .set(Tables.FILE_DATA_PROCESS_REQUEST.ID, 77L)
                .set(FILE_DATA_PROCESS_REQUEST.SOURCE_ID, 93268)
                .set(FILE_DATA_PROCESS_REQUEST.FILE_TYPE, FileType.ONE_CATEGORY_SIMPLE_EXCEL)
                .set(FILE_DATA_PROCESS_REQUEST.URL, "http://url")
                .set(FILE_DATA_PROCESS_REQUEST.CREATE_TIME, new Timestamp(1000000))
                .execute();

        dslContext.insertInto(Tables.ABSTRACT_PROCESS)
                .set(Tables.ABSTRACT_PROCESS.ID, 189526L)
                .set(Tables.ABSTRACT_PROCESS.PROCESS_TYPE, ProcessType.GOOD_FILE_PROCESS)
                .set(Tables.ABSTRACT_PROCESS.REQUEST_ID, 77L)
                .execute();

        dslContext.insertInto(Tables.FILE_PROCESS)
                .set(Tables.FILE_PROCESS.ID, 189526L)
                .set(Tables.FILE_PROCESS.FILE_DATA_PROCESS_REQUEST_ID, 77L)
                .set(Tables.FILE_PROCESS.CREATE_TIME, new Timestamp(1000000))
                .set(Tables.FILE_PROCESS.UPDATE_TIME, new Timestamp(1000000))
                .set(Tables.FILE_PROCESS.PROCESS_STATE, FileProcessState.NOT_STARTED)
                .execute();

        dslContext.insertInto(Tables.FILE_PROCESS_MESSAGE)
                .set(Tables.FILE_PROCESS_MESSAGE.FILE_PROCESS_ID, 189526L)
                .set(Tables.FILE_PROCESS_MESSAGE.PROTOCOL_MESSAGE_ID, 17L)
                .set(Tables.FILE_PROCESS_MESSAGE.TYPE, FileProcessMessageType.FILE_VALIDATION)
                .execute();
    }

    @Test
    public void testOrderingSmoke() {
        for (PartnerContentUi.ListMessageRequest.Order.Column orderColumn: Arrays.asList(PartnerContentUi.ListMessageRequest.Order.Column.REQUEST_ID,
                PartnerContentUi.ListMessageRequest.Order.Column.PROCESS_ID,
                PartnerContentUi.ListMessageRequest.Order.Column.DATA_BUCKET_ID,
                PartnerContentUi.ListMessageRequest.Order.Column.CODE
        )) {
            PartnerContentUi.ListMessageRequest.Builder builder = PartnerContentUi.ListMessageRequest.newBuilder();
            builder.addFilterBuilder()
                    .setColumn(PartnerContentUi.ListMessageRequest.Filter.Column.PROCESS_ID)
                    .setValue("189526");
            builder.addFilterBuilder()
                    .setColumn(PartnerContentUi.ListMessageRequest.Filter.Column.CODE)
                    .setValue("ir.partner_content.warn.model_exist_in_another_category");
            builder.addFilterBuilder()
                    .setColumn(PartnerContentUi.ListMessageRequest.Filter.Column.SOURCE_ID)
                    .setValue("93268");
            builder.getOrderBuilder().setColumn(orderColumn);

            builder.getPagingBuilder().setPageSize(100).setStartRow(0);

            PartnerContentUi.ListMessageResponse result = messageReportService.listData(
                    builder.build()
            );
            List<PartnerContentUi.ListMessageResponse.Row> dataList = result.getDataList();
            assertThat(dataList.size()).isEqualTo(1);
        }
    }
}
