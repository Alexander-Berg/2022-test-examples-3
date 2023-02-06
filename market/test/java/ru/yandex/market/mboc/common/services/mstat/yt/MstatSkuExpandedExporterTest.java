package ru.yandex.market.mboc.common.services.mstat.yt;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

import ru.yandex.market.mboc.common.config.mstat.MstatSkuExpandedTableConfig;
import ru.yandex.market.mboc.common.dict.MbocSupplierType;
import ru.yandex.market.mboc.common.offers.model.Offer;
import ru.yandex.market.mboc.common.services.mstat.MstatOfferState;
import ru.yandex.market.mboc.common.services.mstat.SnapShotContext;
import ru.yandex.market.mboc.common.services.storage.StorageKeyValueServiceMock;

/**
 * @author apluhin
 * @created 2/19/21
 */
public class MstatSkuExpandedExporterTest {

    private final static int BERU_ID = 465852;

    MstatSkuExpandedExporter expandedExporter;

    @Before
    public void setUp() throws Exception {
        MstatSkuExpandedTableConfig skuExpandedTableConfig = new MstatSkuExpandedTableConfig();
        ReflectionTestUtils.setField(skuExpandedTableConfig, "expandedSkuPath", "//test/path");
        expandedExporter = new MstatSkuExpandedExporter(
            null,
            null,
            BERU_ID, //beru_id
            skuExpandedTableConfig,
            new StorageKeyValueServiceMock());
    }

    @Test
    public void testExtractKeys() {
        MstatOfferState offerState = MstatOfferState.builder()
            .shopSku("shop-sku-name")
            .serviceOffers(List.of(
                new Offer.ServiceOffer(1),
                new Offer.ServiceOffer(2)
            )).serviceOfferRealSupplierIds(
                Map.of(
                    1, "1",
                    2, "2"
                )
            )
            .build();
        Assertions.assertThat(expandedExporter.extractKeys(offerState)).isEqualTo(
            Arrays.asList(
                Map.of(
                    "shop_sku", "shop-sku-name",
                    "supplier_id", 1
                ),
                Map.of(
                    "shop_sku", "shop-sku-name",
                    "supplier_id", 2
                )
            )
        );
    }

    @Test
    public void testExtractKeysWithRealSupplier() {
        MstatOfferState offerState = MstatOfferState.builder()
            .shopSku("shop-sku-name")
            .serviceOffers(List.of(
                new Offer.ServiceOffer(1, MbocSupplierType.REAL_SUPPLIER,
                    Offer.AcceptanceStatus.NEW),
                new Offer.ServiceOffer(2, MbocSupplierType.REAL_SUPPLIER,
                    Offer.AcceptanceStatus.NEW)
            )).serviceOfferRealSupplierIds(
                Map.of(
                    1, "1"
                )
            )
            .build();
        Assertions.assertThat(expandedExporter.extractKeys(offerState)).isEqualTo(
            Arrays.asList(
                Map.of(
                    "shop_sku", "1.shop-sku-name",
                    "supplier_id", BERU_ID
                ),
                Map.of(
                    "shop_sku", ".shop-sku-name",
                    "supplier_id", BERU_ID
                )
            )
        );
    }

    @Test
    public void testExportContextCreation() {
        SnapShotContext context = expandedExporter.prepareContext();
        LocalDate prevDay = LocalDate.now().minusDays(1);
        Assertions.assertThat(context.destinationTable).isEqualTo("//test/" + prevDay);
        Assertions.assertThat(context.sql).isEqualTo(
            "INSERT INTO `//test/tmp_table` ( " +
                "supplier_id,  " +
                "shop_sku,  " +
                "raw_supplier_id, " +
                "approved_market_sku_id,  " +
                "supplier_type,  " +
                "vendor_id,  " +
                "category_id,  " +
                "availability,  " +
                "processing_satus,  " +
                "`date`     ) SELECT supplier_id, shop_sku, raw_supplier_id, approved_market_sku_id, supplier_type, " +
                "vendor_id, category_id, availability," +
                " processing_satus, '" + prevDay.toString() + "' as `date`from `//test/path`;"
        );
    }
}
