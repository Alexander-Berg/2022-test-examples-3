package ru.yandex.market.fintech.banksint.excel.csv;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;
import ru.yandex.market.fintech.banksint.excel.offers.ExtractedExcelOffer;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExtractedExcelOfferCsvBatchConsumerTest {

    private static final List<String> TEST_ROW_1 = List.of(
            "123456",
            "sku_1",
            "name1",
            "category_1",
            "123445",
            "1",
            "Да",
            "ДА",
            "Нет"
    );

    private static final List<String> TEST_ROW_2 = List.of(
            "123456.555",
            "sku_1",
            "name1",
            "category_1",
            "1dsasa",
            "100thousand",
            "Да?",
            "No",
            "Yeaaaaah"
    );

    private static final List<String> TEST_ROW_3 = List.of(
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            ""
    );

    private TestBatchConsumer testConsumer = new TestBatchConsumer(null, 0);

    @Test
    void testCorrectRowProcessing() {
        assertDoesNotThrow(() -> testConsumer.parse(TEST_ROW_1));
        ExtractedExcelOffer correctOffer = testConsumer.parse(TEST_ROW_1);
        assertEquals(123456L, correctOffer.getMsku());
        assertEquals("sku_1", correctOffer.getSku());
        assertEquals("name1", correctOffer.getName());
        assertEquals("category_1", correctOffer.getCategory());
        assertEquals(new BigDecimal(123445), correctOffer.getCurrentPrice());
        assertEquals(1, correctOffer.getAvailable());
        assertTrue(correctOffer.isInstallment6());
        assertTrue(correctOffer.isInstallment12());
        assertFalse(correctOffer.isInstallment24());

    }

    @Test
    void testPartiallyIncorrectRowProcessing() {
        assertDoesNotThrow(() -> testConsumer.parse(TEST_ROW_2));
        ExtractedExcelOffer partiallyCorrect =  testConsumer.parse(TEST_ROW_2);
        assertEquals(123457, partiallyCorrect.getMsku());
        assertEquals(0, partiallyCorrect.getAvailable());
        assertNull(partiallyCorrect.getCurrentPrice());
        assertFalse(partiallyCorrect.isInstallment6());
        assertFalse(partiallyCorrect.isInstallment12());
        assertFalse(partiallyCorrect.isInstallment24());
    }

    @Test
    void testBlankRowProcessing() {
        assertDoesNotThrow(() -> testConsumer.parse(TEST_ROW_3));
        ExtractedExcelOffer blank =  testConsumer.parse(TEST_ROW_3);
        String empty = "";
        assertNull(blank.getMsku());
        assertNull(blank.getCurrentPrice());
        assertNull(blank.getAvailable());
        assertEquals(empty, blank.getName());
        assertEquals(empty, blank.getCategory());
        assertEquals(empty, blank.getSku());
        assertFalse(blank.isInstallment6());
        assertFalse(blank.isInstallment12());
        assertFalse(blank.isInstallment24());
    }

    private static class TestBatchConsumer extends ExtractedExcelOfferCsvBatchConsumer {
        TestBatchConsumer(Consumer<List<ExtractedExcelOffer>> consumer, int batchSize) {
            super(consumer, batchSize);
            headerIndex = Map.of(
                    "market-sku", 0,
                    "shop-sku", 1,
                    "name", 2,
                    "category", 3,
                    "price", 4,
                    "count", 5,
                    "installment_6", 6,
                    "installment_12", 7,
                    "installment_24", 8
            );
        }

        @Override
        public ExtractedExcelOffer parse(List<String> csvRow) {
            return super.parse(csvRow);
        }
    }


}
