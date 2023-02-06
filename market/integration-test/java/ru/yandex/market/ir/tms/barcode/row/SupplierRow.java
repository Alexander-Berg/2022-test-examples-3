package ru.yandex.market.ir.tms.barcode.row;

import ru.yandex.misc.bender.annotation.Bendable;
import ru.yandex.misc.bender.annotation.BenderPart;

@Bendable
public class SupplierRow {

    @BenderPart(strictName = true, name = "shop_id")
    private final long shopId;

    @BenderPart(strictName = true, name = "domain")
    private final String domain;

    public SupplierRow(long shopId, String domain) {
        this.shopId = shopId;
        this.domain = domain;
    }
}
