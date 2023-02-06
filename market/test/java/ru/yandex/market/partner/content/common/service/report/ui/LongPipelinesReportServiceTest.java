package ru.yandex.market.partner.content.common.service.report.ui;

import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.ir.http.PartnerContentUi;
import ru.yandex.market.partner.content.common.DBStateGenerator;

public class LongPipelinesReportServiceTest extends DBStateGenerator {

    private LongPipelinesReportService longPipelinesReportService;

    @Before
    public void setUp() {
        super.setUp();
        longPipelinesReportService = new LongPipelinesReportService(configuration);
    }

    @Test
    public void sanityTest() {
        longPipelinesReportService.listData(PartnerContentUi.ListLongPipelinesRequest.newBuilder().build());
    }

}