package ru.yandex.market.fintech.banksint.excel.offers;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ExcelOfferTest {

    @Test
    void testDefaultValues() {
        ExcelOffer offer = ExcelOffer.newBuilder().build();
        assertFalse(offer.isInstallment6());
        assertFalse(offer.isInstallment12());
        assertFalse(offer.isInstallment24());
        assertEquals("", offer.getComment());
        assertFalse(offer.hasActiveInstallments());
    }

}
