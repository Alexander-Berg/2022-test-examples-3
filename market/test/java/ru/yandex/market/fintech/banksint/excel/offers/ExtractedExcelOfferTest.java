package ru.yandex.market.fintech.banksint.excel.offers;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import ru.yandex.market.fintech.banksint.excel.validation.ExcelOfferValidationError;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExtractedExcelOfferTest {

    @Test
    void testCopy() {
        ExtractedExcelOffer offer = new ExtractedExcelOffer(
                ExcelOffer.newBuilder()
                        .setMsku(1234L)
                        .setSku("sku")
                        .setName("name")
                        .setCategory("category")
                        .setCurrentPrice(BigDecimal.TEN)
                        .setAvailable(1L)
                        .setInstallment6(true)
                        .build()
        );
        offer.addError(ExcelOfferValidationError.DUPLICATE_SKU);
        offer.addError(ExcelOfferValidationError.NOT_ENOUGH_SOURCE_INFO);

        ExtractedExcelOffer copy = offer.copy();
        assertEquals(2, copy.getErrors().size());
        assertTrue(copy.getErrors().contains(ExcelOfferValidationError.DUPLICATE_SKU));
        assertTrue(copy.getErrors().contains(ExcelOfferValidationError.NOT_ENOUGH_SOURCE_INFO));

        assertNotSame(copy.getFileOffer(), offer.getFileOffer());
        // fields are shallow copies
        assertSame(copy.getSku(), offer.getSku());
        assertSame(copy.getMsku(), offer.getMsku());
    }
}
