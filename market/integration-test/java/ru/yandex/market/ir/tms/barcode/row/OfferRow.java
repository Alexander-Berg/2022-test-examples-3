package ru.yandex.market.ir.tms.barcode.row;

import ru.yandex.misc.bender.annotation.Bendable;
import ru.yandex.misc.bender.annotation.BenderPart;

@Bendable
public class OfferRow {

    @BenderPart(strictName = true, name = "category_id")
    private final long categoryId;

    @BenderPart(strictName = true, name = "supplier_id")
    private final long supplierId;

    @BenderPart(strictName = true, name = "approved_sku_mapping_id")
    private final long approvedSkuMappingId;

    @BenderPart(strictName = true, name = "bar_code")
    private final String barcodes;

    public OfferRow(long categoryId, long supplierId, long approvedSkuMappingId, String barcodes) {
        this.categoryId = categoryId;
        this.supplierId = supplierId;
        this.approvedSkuMappingId = approvedSkuMappingId;
        this.barcodes = barcodes;
    }
}
