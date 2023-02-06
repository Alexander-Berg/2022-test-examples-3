package ru.yandex.market.ir.tms.barcode.row;

import ru.yandex.misc.bender.annotation.Bendable;
import ru.yandex.misc.bender.annotation.BenderPart;

@Bendable
public class BarcodeStorageRow {
    @BenderPart(strictName = true, name = "sku_id")
    private final long skuId;

    @BenderPart(strictName = true, name = "barcode")
    private final String barcode;

    @BenderPart(strictName = true, name = "barcode_source")
    private final String barcodeSource;

    @BenderPart(strictName = true, name = "category_id")
    private final long categoryId;

    @BenderPart(strictName = true, name = "model_id")
    private final long modelId;

    @BenderPart(strictName = true, name = "last_seen_ts")
    private final long lastSeenTs;

    @BenderPart(strictName = true, name = "max_barcodes_len")
    private final long maxBarcodesLen;

    @BenderPart(strictName = true, name = "domain")
    private final String domain;

    @BenderPart(strictName = true, name = "modification_id")
    private final long modificationId;

    @SuppressWarnings("checkstyle:ParameterNumber")
    public BarcodeStorageRow(
        long modelId,
        long modificationId,
        long skuId,
        String barcode,
        String barcodeSource,
        long categoryId,
        long lastSeenTs,
        long maxBarcodesLen,
        String domain
    ) {
        this.modelId = modelId;
        this.modificationId = modificationId;
        this.skuId = skuId;
        this.barcode = barcode;
        this.barcodeSource = barcodeSource;
        this.categoryId = categoryId;
        this.lastSeenTs = lastSeenTs;
        this.maxBarcodesLen = maxBarcodesLen;
        this.domain = domain;
    }

    public String getBarcode() {
        return barcode;
    }

    public String getDomain() {
        return domain;
    }
}
