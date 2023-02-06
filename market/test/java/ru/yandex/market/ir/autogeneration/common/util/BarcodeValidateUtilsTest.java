package ru.yandex.market.ir.autogeneration.common.util;

import com.google.common.collect.ImmutableList;
import org.assertj.core.api.Assertions;
import org.junit.Test;

public class BarcodeValidateUtilsTest {

    private static final ImmutableList<String> GTIN_VALID_BARCODES =
        ImmutableList.of("797266714467", "87280500", "93737412187479", "8147933775581");

    private static final ImmutableList<String> GTIN_INVALID_BARCODES =
        ImmutableList.of("123", "2147933775581", "8147933775585", "899121530013513485");

    private static final ImmutableList<String> VALID_ISBNS_13 =
        ImmutableList.of("978-3-16-148410-0", "978 3 16 148410 0", "978-1-60309-427-6");

    private static final ImmutableList<String> VALID_ISBNS_10 =
        ImmutableList.of("0201530821", "140723997X", "0-19-853453-1", "3-16-148410-X");

    private static final ImmutableList<String> INVALID_ISBNS =
        ImmutableList.of("978-3-16-14", "9783161484102", "971-60309-427-6", "3-16-148450-X");

    private static final ImmutableList<String> VALID_ISSN =
            ImmutableList.of("977-0-86963600-9" , "977-2-58585000-4");

    private static final ImmutableList<String> INVALID_ISSN =
            ImmutableList.of("976-3-16-148410-0");

    @Test
    public void invalidBarcodeTest() {
        GTIN_INVALID_BARCODES.forEach(this::checkThatInvalid);
    }

    @Test
    public void validBarcodeTest() {
        GTIN_VALID_BARCODES.forEach(this::checkThatValid);
    }

    @Test
    public void nullAndEmptyBarcodeTest() {
        checkThatInvalid(null);
        checkThatInvalid("");
    }

    @Test
    public void validIsbns() {
        VALID_ISBNS_13.forEach(isbn13 -> checkFormattedIsbn(isbn13, true));
        VALID_ISBNS_10.forEach(isbn13 -> checkFormattedIsbn(isbn13, true));
    }

    @Test
    public void invalidIsbns() {
        INVALID_ISBNS.forEach(isbn13 -> checkFormattedIsbn(isbn13, false));
    }

    @Test
    public void validIsbnThroughConverting() {
        // все isbn-13 должны быть валидны как баркоды
        VALID_ISBNS_13.stream()
            .map(BarcodeValidateUtils::convertISBNToBarcode)
            .forEach(this::checkThatValid);
    }

    @Test
    public void validIsbnButInvalidBarcode() {
        // isbn-10 валидны как isbn
        VALID_ISBNS_10.forEach(isbn13 -> checkFormattedIsbn(isbn13, true));
        // но невалидны как баркоды
        VALID_ISBNS_10.stream()
            .map(BarcodeValidateUtils::convertISBNToBarcode)
            .forEach(this::checkThatInvalid);
    }

    @Test
    public void issnTest() {
        VALID_ISSN.forEach(issn -> checkFormattedIsbn(issn, true));
        INVALID_ISSN.forEach(issn -> checkFormattedIsbn(issn, false));
    }

    private void checkThatValid(String barcode) {
        check(barcode, true);
    }

    private void checkThatInvalid(String barcode) {
        check(barcode, false);
    }

    private void check(String barcode, boolean expectedToBeValid) {
        Assertions.assertThat(BarcodeValidateUtils.isBarcodeValid(barcode)).as("Check barcode '%s'",
                barcode).isEqualTo(expectedToBeValid);
    }

    private void checkFormattedIsbn(String barcode, boolean expectedToBeValid) {
        Assertions.assertThat(BarcodeValidateUtils.isFormattedISBNorISSNCodeValid(barcode)).as("Check isbn '%s'",
            barcode).isEqualTo(expectedToBeValid);
    }
}
