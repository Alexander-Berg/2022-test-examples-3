package ru.yandex.market.fps.module.supplier1p.offers.test;

import java.util.Map;

import org.jetbrains.annotations.NotNull;

import ru.yandex.market.crm.util.Randoms;
import ru.yandex.market.fps.module.supplier1p.Supplier1p;
import ru.yandex.market.fps.module.supplier1p.offers.Feed;
import ru.yandex.market.fps.module.supplier1p.offers.XlsxByMarketTemplateWritingService;
import ru.yandex.market.fps.module.supplier1p.test.impl.SupplierTestUtils;
import ru.yandex.market.jmf.bcp.BcpService;
import ru.yandex.market.jmf.logic.def.AttachmentsService;
import ru.yandex.market.mboc.http.MboMappingsService;

public abstract class AbstractFeedTest {
    protected static final String FEED_URL = "https://feed.ru/data";
    protected final BcpService bcpService;
    protected final AttachmentsService attachmentsService;
    protected final MboMappingsService mboMappingsService;
    protected final XlsxByMarketTemplateWritingService xlsxByMarketTemplateWritingService;
    private final SupplierTestUtils supplierTestUtils;

    protected AbstractFeedTest(
            SupplierTestUtils supplierTestUtils, BcpService bcpService,
            AttachmentsService attachmentsService,
            MboMappingsService mboMappingsService,
            XlsxByMarketTemplateWritingService xlsxByMarketTemplateWritingService) {
        this.supplierTestUtils = supplierTestUtils;
        this.bcpService = bcpService;
        this.attachmentsService = attachmentsService;
        this.mboMappingsService = mboMappingsService;
        this.xlsxByMarketTemplateWritingService = xlsxByMarketTemplateWritingService;
    }

    @NotNull
    protected Feed createFeed() {
        return bcpService.create(Feed.class, Map.of(
                Feed.TITLE, "test_feed.xlsx",
                Feed.FEED_FILE, attachmentsService.createDetached("test_feed.xlsx", FEED_URL, "xlsx"),
                Feed.SUPPLIER, supplierTestUtils.createSupplier(Map.of(
                        Supplier1p.MBI_SUPPLIER_ID, Randoms.longValue()
                ))
        ));
    }

}
