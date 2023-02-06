package ru.yandex.market.abo.core.resupply.zpl;

import java.util.Base64;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static ru.yandex.market.abo.core.resupply.stock.ResupplyStock.BAD_3P;

public class AboBarcodeDataTest {

    @Test
    public void ascii() {
        int supplier = 564367;
        String sku = "Q07-3голубой,глянцевый";
        assertEquals("19156-BAD_3P-NTY0MzY3LlEwNy0z0LPQvtC70YPQsdC-0Lks0LPQu9GP0L3RhtC10LLRi9C5",
                new AboBarcodeData(supplier, sku, 19156, BAD_3P).toAscii());

        assertEquals(supplier + "." + sku,
                new String(Base64.getUrlDecoder()
                        .decode("NTY0MzY3LlEwNy0z0LPQvtC70YPQsdC-0Lks0LPQu9GP0L3RhtC10LLRi9C5")));
    }

}
