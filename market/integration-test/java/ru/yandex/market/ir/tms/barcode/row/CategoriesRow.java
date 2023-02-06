package ru.yandex.market.ir.tms.barcode.row;

import ru.yandex.market.mbo.export.MboParameters;
import ru.yandex.misc.bender.annotation.Bendable;
import ru.yandex.misc.bender.annotation.BenderPart;

@Bendable
public class CategoriesRow {
    @BenderPart(strictName = true, name = "hid")
    private final long hid;

    @BenderPart(strictName = true, name = "data")
    private final String data;

    @BenderPart(strictName = true, name = "leaf")
    private final boolean leaf;

    public CategoriesRow(long hid, boolean leaf, MboParameters.Category data) {
        this.hid = hid;
        this.leaf = leaf;
        this.data = new String(data.toByteString().toByteArray());
    }
}
