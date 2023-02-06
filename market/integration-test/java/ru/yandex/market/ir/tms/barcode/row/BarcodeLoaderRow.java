package ru.yandex.market.ir.tms.barcode.row;

import ru.yandex.bolts.collection.ListF;
import ru.yandex.misc.bender.annotation.Bendable;
import ru.yandex.misc.bender.annotation.BenderPart;

@Bendable
public class BarcodeLoaderRow {
    @BenderPart(strictName = true, name = "category_id")
    private final long categoryId;

    @BenderPart(strictName = true, name = "quorum")
    private final long quorum;

    @BenderPart(strictName = true, name = "model_modif_sku_id")
    private final long modelModifSkuId;

    @BenderPart(strictName = true, name = "barcode")
    private final String barcode;

    @BenderPart(strictName = true, name = "domains")
    private final ListF<String> domains;

    public BarcodeLoaderRow(long categoryId, long quorum, long modelModifSkuId, String barcode, ListF<String> domains) {
        this.categoryId = categoryId;
        this.quorum = quorum;
        this.modelModifSkuId = modelModifSkuId;
        this.barcode = barcode;
        this.domains = domains;
    }

    public String getBarcode() {
        return barcode;
    }
}
