package ru.yandex.market.ff.model.bo;

import java.util.Collections;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.Assert.assertTrue;

class SupplierContentMappingTest {
    private static final int MARKET_BARCODES_MAX_LENGTH = 50;
    private static final String NORMAL_LENGTH_BARCODES = "Normal length barcodes";
    private static final String EXACT_50_CHARS_LENGTH_BARCODES = "50ch 50ch 50ch 50ch 50ch 50ch long length barcodes";
    private static final String LONG_LENGTH_BARCODES = "Very long long long long long long long length barcodes";
    private static final String ZERO_LENGTH_BARCODES = "";

    @Test
    void shouldSkipLongAndZeroLengthMarketBarcodes() {
        List<String> inputBarcodes = Collections.unmodifiableList(List.of(NORMAL_LENGTH_BARCODES,
                EXACT_50_CHARS_LENGTH_BARCODES, LONG_LENGTH_BARCODES, ZERO_LENGTH_BARCODES));

        List<String> resultBarcodes = SupplierContentMapping
                .builder("mockArticleName", 222L, "mockName")
                .setMarketBarcodes(inputBarcodes)
                .build().getMarketBarcodes();
        Assertions.assertThat(resultBarcodes).containsExactlyInAnyOrder(
                NORMAL_LENGTH_BARCODES, EXACT_50_CHARS_LENGTH_BARCODES);
        resultBarcodes.forEach(
                it -> assertTrue(it != null && !it.isBlank() && it.length() <= MARKET_BARCODES_MAX_LENGTH));
    }
}
