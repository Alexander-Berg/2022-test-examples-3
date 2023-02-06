package ru.yandex.market.ir.tms.barcode.row;

import ru.yandex.misc.bender.annotation.Bendable;
import ru.yandex.misc.bender.annotation.BenderPart;

@Bendable
public class WhiteListCategoriesRow {
    @BenderPart(strictName = true, name = "name")
    private final String name;

    @BenderPart(strictName = true, name = "hid")
    private final int hid;

    @BenderPart(strictName = true, name = "department")
    private final String department;

    public WhiteListCategoriesRow(String name, int hid, String department) {
        this.name = name;
        this.hid = hid;
        this.department = department;
    }
}
