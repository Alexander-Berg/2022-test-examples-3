package ru.yandex.market.gutgin.tms.pipeline.dcp.xls.validation;

import ru.yandex.market.partner.content.common.DBStateGenerator;
import ru.yandex.market.partner.content.common.db.jooq.enums.FileType;
import ru.yandex.market.partner.content.common.db.jooq.enums.ProcessType;
import ru.yandex.market.partner.content.common.db.jooq.tables.pojos.GcRawSku;
import ru.yandex.market.partner.content.common.entity.goodcontent.RawSku;
import ru.yandex.market.partner.content.common.message.MessageInfo;
import ru.yandex.market.partner.content.common.service.mock.DataCampServiceMock;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

public abstract class BaseDcpExcelValidationTest extends DBStateGenerator {

    protected static final List<GcRawSku> EMPTY = Collections.emptyList();
    protected static final Consumer<List<? extends MessageInfo>> NO_MESSAGES = x -> {};
    protected static final int BUSINESS_ID = PARTNER_SHOP_ID;
    protected static final long CATEGORY_ID = 123L;
    protected static final int ROW_INDEX = 999;
    protected Validation<GcRawSku> validation;
    protected final DataCampServiceMock dataCampServiceMock = new DataCampServiceMock();

    @Override
    protected long createFileProcessId(long fileDataProcessRequestId) {
        return this.fileProcessDao.insertNewFileProcess(fileDataProcessRequestId, ProcessType.DCP_FILE_PROCESS);
    }

    @Override
    protected long createFileDataProcessRequest(int sourceId, Boolean ignoreWhiteBackgroundCheck) {
        return this.partnerContentDao.createFileDataProcessRequest(
            sourceId, BUSINESS_ID, "http://some.url", false,
            FileType.DCP_SINGLE_EXCEL, (Integer)null, ignoreWhiteBackgroundCheck, false, false);
    }

    protected GcRawSku createGcRawSku(String shopSku) {
        RawSku data = RawSku.newBuilder()
            .setCategoryId(CATEGORY_ID)
            .setShopSku(shopSku)
            .setRowIndex(ROW_INDEX)
            .build();
        GcRawSku gcRawSku = new GcRawSku();
        gcRawSku.setFileProcessId(processId);
        gcRawSku.setCreateDate(Timestamp.from(Instant.now()));
        gcRawSku.setData(data);
        return gcRawSku;
    }

    protected GcRawSku createGcRawSkuWithInvalidGroupId(String shopSku) {
        RawSku data = RawSku.newBuilder()
            .setCategoryId(CATEGORY_ID)
            .setShopSku(shopSku)
            .setRowIndex(ROW_INDEX)
            .setGroupId("Clearly invalid")
            .build();
        GcRawSku gcRawSku = new GcRawSku();
        gcRawSku.setFileProcessId(processId);
        gcRawSku.setCreateDate(Timestamp.from(Instant.now()));
        gcRawSku.setData(data);
        return gcRawSku;
    }

    protected void check(List<GcRawSku> expectedValid,
                         List<GcRawSku> expectedInvalid,
                         Consumer<List<? extends MessageInfo>> messageRequirements) {
        List<GcRawSku> all = new ArrayList<>();
        all.addAll(expectedValid);
        all.addAll(expectedInvalid);

        Validation.Result<GcRawSku> result = validation.validate(all);

        assertThat(result.getInvalidValues()).containsExactlyInAnyOrderElementsOf(expectedInvalid);
        assertThat(result.getMessages()).satisfies(messageRequirements);
    }
}
