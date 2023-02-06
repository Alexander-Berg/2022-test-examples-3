package ru.yandex.market.ir.tms.barcode.row;

import ru.yandex.misc.bender.annotation.Bendable;
import ru.yandex.misc.bender.annotation.BenderPart;

@Bendable
public class ValidationResultRow {

    @BenderPart(strictName = true, name = "barcode")
    private String barcode;

    @BenderPart(strictName = true, name = "barcode_source")
    private String barcodeSource;

    @BenderPart(strictName = true, name = "domain")
    private String domain;

    @BenderPart(strictName = true, name = "model_id")
    private long modelId;

    @BenderPart(strictName = true, name = "modification_id")
    private long modificationId;

    @BenderPart(strictName = true, name = "sku_id")
    private long skuId;

    @BenderPart(strictName = true, name = "category_id")
    private long categoryId;

    @BenderPart(strictName = true, name = "valid_sku_id")
    private boolean validSkuId;

    @BenderPart(strictName = true, name = "valid_domain")
    private boolean validDomain;

    @BenderPart(strictName = true, name = "valid_modification_id")
    private boolean validModificationId;

    @BenderPart(strictName = true, name = "valid_barcode_source")
    private boolean validBarcodeSource;

    @BenderPart(strictName = true, name = "valid_category_id")
    private boolean validCategoryId;

    @BenderPart(strictName = true, name = "valid_barcodes")
    private boolean validBarcodes;

    @BenderPart(strictName = true, name = "valid_model_id")
    private boolean validModelId;

    @BenderPart(strictName = true, name = "overall_valid")
    private boolean overallValid;

    public String getBarcode() {
        return barcode;
    }
}
