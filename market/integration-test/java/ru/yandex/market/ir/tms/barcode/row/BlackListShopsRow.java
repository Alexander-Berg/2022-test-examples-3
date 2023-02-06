package ru.yandex.market.ir.tms.barcode.row;

import ru.yandex.misc.bender.annotation.Bendable;
import ru.yandex.misc.bender.annotation.BenderPart;

@Bendable
public class BlackListShopsRow {
    @BenderPart(strictName = true, name = "domain")
    private final String domain;

    @BenderPart(strictName = true, name = "commentary")
    private final String commentary;

    public BlackListShopsRow(String domain, String commentary) {
        this.domain = domain;
        this.commentary = commentary;
    }
}
