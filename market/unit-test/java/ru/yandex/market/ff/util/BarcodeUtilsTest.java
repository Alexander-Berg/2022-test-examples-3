package ru.yandex.market.ff.util;

import java.util.List;

import org.junit.jupiter.api.Test;

import ru.yandex.market.ff.base.SoftAssertionSupport;


class BarcodeUtilsTest extends SoftAssertionSupport {

    @Test
    void splitOnSingleBarcodeWithoutWhiteSpaces() {
        var actual = BarcodeUtils.split("123");
        var expected = List.of("123");

        assertions.assertThat(actual).isEqualTo(expected);
    }

    @Test
    void splitOnSingleBarcodeWithWhiteSpaces() {
        var actual = BarcodeUtils.split(" 123  \t");
        var expected = List.of("123");

        assertions.assertThat(actual).isEqualTo(expected);
    }

    @Test
    void splitOnBarcodesWithWhiteSpaces() {
        var actual = BarcodeUtils.split(" 123  \t, 456,789\t");
        var expected = List.of("123", "456", "789");

        assertions.assertThat(actual).isEqualTo(expected);
    }

    @Test
    void splitOnBarcodesWitout() {
        var actual = BarcodeUtils.split("123,456,789");
        var expected = List.of("123", "456", "789");

        assertions.assertThat(actual).isEqualTo(expected);
    }

    @Test
    void splitOnNullString() {
        var actual = BarcodeUtils.split(null);
        var expected = List.of();

        assertions.assertThat(actual).isEqualTo(expected);
    }

    @Test
    void splitOnEmptyBarcodes() {
        var actual = BarcodeUtils.split(",\t  ,, ,");
        var expected = List.of("", "", "", "", "");

        assertions.assertThat(actual).isEqualTo(expected);
    }

    @Test
    void splitOnBarcodesWithSpecialChars() {
        var actual = BarcodeUtils.split("''123ABCDE , ''123'\\ABC\\\\DE");
        var expected = List.of("123ABCDE", "123ABCDE");

        assertions.assertThat(actual).isEqualTo(expected);
    }

    @Test
    void joinOnNullList() {
        var actual = BarcodeUtils.join(null);

        assertions.assertThat(actual).isEqualTo(null);
    }

    @Test
    void joinOnEmptyList() {
        var actual = BarcodeUtils.join(List.of());

        assertions.assertThat(actual).isEqualTo(null);
    }

    @Test
    void joinOnBarcodesWithWhiteSpaces() {
        var actual = BarcodeUtils.join(List.of(" 123", "  456\t\t", "789"));
        var expected = "123,456,789";

        assertions.assertThat(actual).isEqualTo(expected);
    }

    @Test
    void joinOnBarcodesWithoutWhiteSpaces() {
        var actual = BarcodeUtils.join(List.of("123", "456", "789"));
        var expected = "123,456,789";

        assertions.assertThat(actual).isEqualTo(expected);
    }

    @Test
    void joinOnSingleBarcodeWithoutWhiteSpaces() {
        var actual = BarcodeUtils.join(List.of("123"));
        var expected = "123";

        assertions.assertThat(actual).isEqualTo(expected);
    }

    @Test
    void joinOnSingleBarcodeWithtWhiteSpaces() {
        var actual = BarcodeUtils.join(List.of(" 123\t\t"));
        var expected = "123";

        assertions.assertThat(actual).isEqualTo(expected);
    }
}
