package ru.yandex.market.partner.content.common.service.report.ui;

import org.assertj.core.api.Assertions;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.ir.http.PartnerContentUi;
import ru.yandex.market.partner.content.common.DBDcpStateGenerator;
import ru.yandex.market.partner.content.common.DBStateGenerator;
import ru.yandex.market.partner.content.common.db.jooq.enums.GcSkuValidationType;
import ru.yandex.market.partner.content.common.db.jooq.tables.pojos.GcSkuTicket;

import java.sql.Timestamp;
import java.util.List;

import static ru.yandex.market.partner.content.common.db.jooq.Tables.GC_SKU_VALIDATION;

public class GcValidationReportServiceTest extends DBDcpStateGenerator {

    private GcValidationReportService gcValidationReportService;

    @Override
    @Before
    public void setUp() {
        super.setUp();
        gcValidationReportService = new GcValidationReportService(configuration);
    }

    @Test
    public void testLooseFiltersAreProhibited() {
        generateDBDcpInitialStateNew(25);

        gcValidationReportService.actualizeReport();

        PartnerContentUi.ListGcValidationRequest requestTemplate = PartnerContentUi.ListGcValidationRequest.newBuilder()
                .setPaging(
                        PartnerContentUi.Paging.newBuilder()
                                .setPageSize(100)
                                .setStartRow(0)
                                .build()
                )
                .setOrder(
                        PartnerContentUi.ListGcValidationRequest.Order.newBuilder()
                                .setColumn(PartnerContentUi.ListGcValidationRequest.Order.Column.PROCESS_ID)
                                .build()
                )
                .setRequestType(PartnerContentUi.RequestType.ALL)
                .build();


        Assertions.assertThatThrownBy(() ->
                gcValidationReportService.listData(requestTemplate.toBuilder().build())
        ).isInstanceOf(IllegalArgumentException.class);

        Assertions.assertThatThrownBy(() ->
                gcValidationReportService.listData(
                        requestTemplate.toBuilder()
                                .addFilter(
                                        PartnerContentUi.ListGcValidationRequest.Filter.newBuilder()
                                                .setColumn(PartnerContentUi.ListGcValidationRequest.Filter.Column.VALIDATION_TYPE)
                                                .setValue("UNIQUE_SKU_NAME")
                                                .build()
                                )
                                .build())
        ).isInstanceOf(IllegalArgumentException.class);
    }
}
