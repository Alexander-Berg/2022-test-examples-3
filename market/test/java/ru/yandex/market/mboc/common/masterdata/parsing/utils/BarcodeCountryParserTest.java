package ru.yandex.market.mboc.common.masterdata.parsing.utils;

import java.util.Collection;

import org.assertj.core.api.SoftAssertions;
import org.junit.Test;

/**
 * @author amaslak
 */
public class BarcodeCountryParserTest {

    public static final int BARCODE_LENGTH = 13;

    private static String makeBarcode(String prefix) {
        return (prefix + "01234567890123456789").substring(0, BARCODE_LENGTH);
    }

    @Test
    public void whenCreatePrefixExpandWorks() {
        BarcodeCountryParser.PrefixRange p000 = BarcodeCountryParser.PrefixRange.of("000");
        BarcodeCountryParser.PrefixRange p001 = BarcodeCountryParser.PrefixRange.of("001-001");
        BarcodeCountryParser.PrefixRange p00002 = BarcodeCountryParser.PrefixRange.of("00002-00005");

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(p000.expand()).containsExactly("000");
            s.assertThat(p001.expand()).containsExactly("001");
            s.assertThat(p00002.expand()).containsExactly("00002", "00003", "00004", "00005");
        });
    }

    @Test
    public void whenBarcodeGuessCountry() {
        Collection<String> p800 = BarcodeCountryParser.guess(makeBarcode("800"));
        Collection<String> p549 = BarcodeCountryParser.guess(makeBarcode("549"));
        Collection<String> p982 = BarcodeCountryParser.guess(makeBarcode("982"));

        Collection<String> p001 = BarcodeCountryParser.guess(makeBarcode("001"));
        Collection<String> p0002 = BarcodeCountryParser.guess(makeBarcode("0002"));
        Collection<String> p00003 = BarcodeCountryParser.guess(makeBarcode("00003"));


        SoftAssertions.assertSoftly(s -> {
            s.assertThat(p800).containsExactly("Италия");
            s.assertThat(p549).containsExactlyInAnyOrder("Бельгия", "Люксембург");
            s.assertThat(p982).isNull();

            s.assertThat(p001).containsExactly("США");
            s.assertThat(p0002).containsExactly("США");
            s.assertThat(p00003).containsExactly("США");
        });
    }
}
