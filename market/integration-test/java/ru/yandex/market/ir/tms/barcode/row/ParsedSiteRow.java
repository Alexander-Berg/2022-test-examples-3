package ru.yandex.market.ir.tms.barcode.row;

import ru.yandex.misc.bender.annotation.Bendable;
import ru.yandex.misc.bender.annotation.BenderPart;

@Bendable
public class ParsedSiteRow {
    @BenderPart(strictName = true, name = "barcode")
    private final String barcode;

    @BenderPart(strictName = true, name = "barcode_source")
    private final String barcodeSource;

    @BenderPart(strictName = true, name = "domain")
    private final String domain;

    @BenderPart(strictName = true, name = "model_id")
    private final long modelId;

    @BenderPart(strictName = true, name = "modification_id")
    private final long modificationId;

    @BenderPart(strictName = true, name = "sku_id")
    private final long skuId;

    @BenderPart(strictName = true, name = "category_id")
    private final long categoryId;

    public ParsedSiteRow(
        String barcode,
        String barcodeSource,
        String domain,
        long modelId,
        long modificationId,
        long skuId,
        long categoryId
    ) {
        this.barcode = barcode;
        this.barcodeSource = barcodeSource;
        this.domain = domain;
        this.modelId = modelId;
        this.modificationId = modificationId;
        this.skuId = skuId;
        this.categoryId = categoryId;
    }
}
