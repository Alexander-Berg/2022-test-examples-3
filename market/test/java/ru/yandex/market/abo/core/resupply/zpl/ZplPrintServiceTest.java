package ru.yandex.market.abo.core.resupply.zpl;

import org.junit.jupiter.api.Test;

import ru.yandex.market.abo.core.resupply.stock.ResupplyStock;

public class ZplPrintServiceTest {

    @Test
    public void testZpl() {
        AboBarcodeData bd = new AboBarcodeData(1L, "shopsku", 2L, ResupplyStock.GOOD);
        System.out.println(new ZplPrintService().getZpl(bd));
    }
}
