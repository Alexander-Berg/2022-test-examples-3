package ru.yandex.market.core.tax.model;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ParametersAreNonnullByDefault
class TaxSystemTest {

    @Test
    void testValid() {
        // NO_VAT доступен для всех СНО.
        assertTrue(TaxSystem.OSN.supports(VatRate.NO_VAT));
        assertTrue(TaxSystem.USN.supports(VatRate.NO_VAT));
        assertTrue(TaxSystem.USN_MINUS_COST.supports(VatRate.NO_VAT));
        assertTrue(TaxSystem.ENVD.supports(VatRate.NO_VAT));
        assertTrue(TaxSystem.ESHN.supports(VatRate.NO_VAT));
        assertTrue(TaxSystem.PSN.supports(VatRate.NO_VAT));

        // Остальные ставки доступны только для ОСН
        assertTrue(TaxSystem.OSN.supports(VatRate.VAT_0));
        assertTrue(TaxSystem.OSN.supports(VatRate.VAT_10));
        assertTrue(TaxSystem.OSN.supports(VatRate.VAT_10_110));
        assertTrue(TaxSystem.OSN.supports(VatRate.VAT_18));
        assertTrue(TaxSystem.OSN.supports(VatRate.VAT_18_118));
        assertTrue(TaxSystem.OSN.supports(VatRate.VAT_20));
        assertTrue(TaxSystem.OSN.supports(VatRate.VAT_20_120));
    }

    @Test
    void testInvalid() {
        // Для не ОСН недоступны ставки кроме NO_VAT.
        assertFalse(TaxSystem.USN.supports(VatRate.VAT_0));
        assertFalse(TaxSystem.USN.supports(VatRate.VAT_10));
        assertFalse(TaxSystem.USN.supports(VatRate.VAT_10_110));
        assertFalse(TaxSystem.USN.supports(VatRate.VAT_18));
        assertFalse(TaxSystem.USN.supports(VatRate.VAT_18_118));
        assertFalse(TaxSystem.USN.supports(VatRate.VAT_20));
        assertFalse(TaxSystem.USN.supports(VatRate.VAT_20_120));

        assertFalse(TaxSystem.USN_MINUS_COST.supports(VatRate.VAT_0));
        assertFalse(TaxSystem.USN_MINUS_COST.supports(VatRate.VAT_10));
        assertFalse(TaxSystem.USN_MINUS_COST.supports(VatRate.VAT_10_110));
        assertFalse(TaxSystem.USN_MINUS_COST.supports(VatRate.VAT_18));
        assertFalse(TaxSystem.USN_MINUS_COST.supports(VatRate.VAT_18_118));
        assertFalse(TaxSystem.USN_MINUS_COST.supports(VatRate.VAT_20));
        assertFalse(TaxSystem.USN_MINUS_COST.supports(VatRate.VAT_20_120));

        assertFalse(TaxSystem.ENVD.supports(VatRate.VAT_0));
        assertFalse(TaxSystem.ENVD.supports(VatRate.VAT_10));
        assertFalse(TaxSystem.ENVD.supports(VatRate.VAT_10_110));
        assertFalse(TaxSystem.ENVD.supports(VatRate.VAT_18));
        assertFalse(TaxSystem.ENVD.supports(VatRate.VAT_18_118));
        assertFalse(TaxSystem.ENVD.supports(VatRate.VAT_20));
        assertFalse(TaxSystem.ENVD.supports(VatRate.VAT_20_120));

        assertFalse(TaxSystem.ESHN.supports(VatRate.VAT_0));
        assertFalse(TaxSystem.ESHN.supports(VatRate.VAT_10));
        assertFalse(TaxSystem.ESHN.supports(VatRate.VAT_10_110));
        assertFalse(TaxSystem.ESHN.supports(VatRate.VAT_18));
        assertFalse(TaxSystem.ESHN.supports(VatRate.VAT_18_118));
        assertFalse(TaxSystem.ESHN.supports(VatRate.VAT_20));
        assertFalse(TaxSystem.ESHN.supports(VatRate.VAT_20_120));

        assertFalse(TaxSystem.PSN.supports(VatRate.VAT_0));
        assertFalse(TaxSystem.PSN.supports(VatRate.VAT_10));
        assertFalse(TaxSystem.PSN.supports(VatRate.VAT_10_110));
        assertFalse(TaxSystem.PSN.supports(VatRate.VAT_18));
        assertFalse(TaxSystem.PSN.supports(VatRate.VAT_18_118));
        assertFalse(TaxSystem.PSN.supports(VatRate.VAT_20));
        assertFalse(TaxSystem.PSN.supports(VatRate.VAT_20_120));
    }

}
