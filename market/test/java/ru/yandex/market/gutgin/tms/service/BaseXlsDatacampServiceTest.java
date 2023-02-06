package ru.yandex.market.gutgin.tms.service;

import ru.yandex.market.gutgin.tms.base.BaseDbGutGinTest;
import ru.yandex.market.partner.content.common.db.jooq.enums.FileProcessState;
import ru.yandex.market.partner.content.common.db.jooq.enums.FileType;
import ru.yandex.market.partner.content.common.db.jooq.enums.ProcessType;
import ru.yandex.market.partner.content.common.entity.goodcontent.RawSku;

import java.sql.Timestamp;

import static ru.yandex.market.partner.content.common.db.jooq.Tables.ABSTRACT_PROCESS;
import static ru.yandex.market.partner.content.common.db.jooq.Tables.FILE_DATA_PROCESS_REQUEST;
import static ru.yandex.market.partner.content.common.db.jooq.Tables.FILE_PROCESS;
import static ru.yandex.market.partner.content.common.db.jooq.Tables.GC_RAW_SKU;

@SuppressWarnings("checkstyle:magicnumber")
public abstract class BaseXlsDatacampServiceTest extends BaseDbGutGinTest {
    protected void prepareDb() {
        dsl().insertInto(ABSTRACT_PROCESS,
            ABSTRACT_PROCESS.ID,
            ABSTRACT_PROCESS.PROCESS_TYPE,
            ABSTRACT_PROCESS.REQUEST_ID
        )
            .values(1L, ProcessType.GOOD_FILE_PROCESS, 1L)
            .values(2L, ProcessType.GOOD_FILE_PROCESS, 2L)
            .values(3L, ProcessType.GOOD_FILE_PROCESS, 3L)
            .execute();

        dsl().insertInto(FILE_DATA_PROCESS_REQUEST,
            FILE_DATA_PROCESS_REQUEST.ID,
            FILE_DATA_PROCESS_REQUEST.SOURCE_ID,
            FILE_DATA_PROCESS_REQUEST.FILE_TYPE,
            FILE_DATA_PROCESS_REQUEST.URL,
            FILE_DATA_PROCESS_REQUEST.CREATE_TIME
        )
            .values(1L, 1, FileType.DCP_SINGLE_EXCEL, "url1", new Timestamp(System.currentTimeMillis()))
            .values(2L, 2, FileType.DCP_SINGLE_EXCEL, "url2", new Timestamp(System.currentTimeMillis()))
            .values(3L, 3, FileType.DCP_SINGLE_EXCEL, "url3", new Timestamp(System.currentTimeMillis()))
            .execute();

        dsl().insertInto(FILE_PROCESS,
            FILE_PROCESS.ID,
            FILE_PROCESS.FILE_DATA_PROCESS_REQUEST_ID,
            FILE_PROCESS.CREATE_TIME,
            FILE_PROCESS.UPDATE_TIME,
            FILE_PROCESS.PROCESS_STATE
        )
            .values(1L, 1L, new Timestamp(System.currentTimeMillis()),
                new Timestamp(System.currentTimeMillis()), FileProcessState.BUCKETS_PROCESSING)
            .values(2L, 2L, new Timestamp(System.currentTimeMillis()),
                new Timestamp(System.currentTimeMillis()), FileProcessState.BUCKETS_PROCESSING)
            .values(3L, 3L, new Timestamp(System.currentTimeMillis()),
                new Timestamp(System.currentTimeMillis()), FileProcessState.BUCKETS_PROCESSING)
            .execute();

        dsl().insertInto(GC_RAW_SKU,
            GC_RAW_SKU.ID,
            GC_RAW_SKU.FILE_PROCESS_ID,
            GC_RAW_SKU.DATA,
            GC_RAW_SKU.CREATE_DATE,
            GC_RAW_SKU.VALID_FOR_DATACAMP
        )
            .values(1L, 1L, RawSku.newBuilder().setCategoryId(1L).setShopSku("1").build(), new Timestamp(System.currentTimeMillis()), true)
            .values(2L, 2L, RawSku.newBuilder().setCategoryId(2L).setShopSku("2").build(), new Timestamp(System.currentTimeMillis()), true)
            .values(3L, 3L, RawSku.newBuilder().setCategoryId(3L).setShopSku("3").build(), new Timestamp(System.currentTimeMillis()), true)
            .execute();
    }
}
