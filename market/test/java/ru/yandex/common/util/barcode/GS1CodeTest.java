package ru.yandex.common.util.barcode;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.SplittableRandom;

import com.google.common.collect.ImmutableSet;
import org.assertj.core.api.Assertions;
import org.junit.Test;

public class GS1CodeTest {

    private static final SplittableRandom RANDOM = new SplittableRandom(1005378L);

    @Test
    public void testValidCodeCreation() {
        String validCode = "3809300214219";

        Optional<GS1Code> createdForEAN13 = GS1Code.create(validCode, GS1CodeFormat.EAN_13);
        Optional<GS1Code> createdForGTIN13 = GS1Code.create(validCode, GS1CodeFormat.GTIN_13);
        Optional<GS1Code> createdWithoutFormat = GS1Code.create(validCode);
        Optional<GS1Code> createdWithAllowedFormat = GS1Code.create(validCode, ImmutableSet.of(
            GS1CodeFormat.GTIN_8, GS1CodeFormat.GTIN_13, GS1CodeFormat.SSCC
        ));

        Assertions.assertThat(createdForEAN13).isPresent();
        Assertions.assertThat(createdForGTIN13).isPresent();
        Assertions.assertThat(createdWithoutFormat).isPresent();
        Assertions.assertThat(createdWithAllowedFormat).isPresent();

        Assertions.assertThat(createdForEAN13.get().getCode()).isEqualTo(validCode);
        Assertions.assertThat(createdForGTIN13.get().getCode()).isEqualTo(validCode);
        Assertions.assertThat(createdWithoutFormat.get().getCode()).isEqualTo(validCode);
        Assertions.assertThat(createdWithAllowedFormat.get().getCode()).isEqualTo(validCode);

        Assertions.assertThat(createdForEAN13.get().getFormat()).isEqualTo(GS1CodeFormat.EAN_13);
        Assertions.assertThat(createdForGTIN13.get().getFormat()).isEqualTo(GS1CodeFormat.GTIN_13);
        Assertions.assertThat(createdWithoutFormat.get().getFormat().getLength()).isEqualTo(validCode.length());
        Assertions.assertThat(createdWithAllowedFormat.get().getFormat()).isEqualTo(GS1CodeFormat.GTIN_13);
    }

    @Test
    public void testInvalidCodeCreation() {
        Assertions.assertThat(GS1Code.create(null)).isEmpty();
        Assertions.assertThat(GS1Code.create("")).isEmpty();
        Assertions.assertThat(GS1Code.create(" \t \n")).isEmpty();
        Assertions.assertThat(GS1Code.create("1205x321")).isEmpty();
        Assertions.assertThat(GS1Code.create(" 3809300214219 ")).isEmpty();
        Assertions.assertThat(GS1Code.create("-3809300214219")).isEmpty();
        Assertions.assertThat(GS1Code.create("+3809300214219")).isEmpty();
        Assertions.assertThat(GS1Code.create("1234")).isEmpty();
        Assertions.assertThat(GS1Code.create("3809300214210")).isEmpty(); // bad check digit

        // Valid code, but provided formats do not contain appropriate format.
        Assertions.assertThat(GS1Code.create("3809300214219", ImmutableSet.of(
                GS1CodeFormat.GTIN_8, GS1CodeFormat.GTIN_12, GS1CodeFormat.UPC_A, GS1CodeFormat.SSCC
        ))).isEmpty();

        // Valid code but expected format doesn't match.
        Assertions.assertThat(GS1Code.create("3809300214219", GS1CodeFormat.UPC_A)).isEmpty();
    }

    @Test
    public void testInvalidCodeCreationThrows() {
        Assertions.assertThatThrownBy(() -> GS1Code.createOrThrow(null))
                .isExactlyInstanceOf(IllegalArgumentException.class);
        Assertions.assertThatThrownBy(() -> GS1Code.createOrThrow(""))
                .isExactlyInstanceOf(IllegalArgumentException.class);
        Assertions.assertThatThrownBy(() -> GS1Code.createOrThrow(" \t \n"))
                .isExactlyInstanceOf(IllegalArgumentException.class);
        Assertions.assertThatThrownBy(() -> GS1Code.createOrThrow("1205x321"))
                .isExactlyInstanceOf(IllegalArgumentException.class);
        Assertions.assertThatThrownBy(() -> GS1Code.createOrThrow(" 3809300214219 "))
                .isExactlyInstanceOf(IllegalArgumentException.class);
        Assertions.assertThatThrownBy(() -> GS1Code.createOrThrow("-3809300214219"))
                .isExactlyInstanceOf(IllegalArgumentException.class);
        Assertions.assertThatThrownBy(() -> GS1Code.createOrThrow("+3809300214219"))
                .isExactlyInstanceOf(IllegalArgumentException.class);
        Assertions.assertThatThrownBy(() -> GS1Code.createOrThrow("1234"))
                .isExactlyInstanceOf(IllegalArgumentException.class);
        Assertions.assertThatThrownBy(() -> GS1Code.createOrThrow("3809300214210"))
                .isExactlyInstanceOf(IllegalArgumentException.class);

        // Valid code, but provided formats do not contain appropriate format.
        Assertions.assertThatThrownBy(() -> GS1Code.createOrThrow("3809300214219", ImmutableSet.of(
                GS1CodeFormat.GTIN_8, GS1CodeFormat.GTIN_12, GS1CodeFormat.UPC_A, GS1CodeFormat.SSCC
        ))).isExactlyInstanceOf(IllegalArgumentException.class);

        // Valid code but expected format doesn't match.
        Assertions.assertThatThrownBy(() -> GS1Code.createOrThrow("3809300214219", GS1CodeFormat.UPC_A))
                .isExactlyInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void testValidGTIN8() throws IOException, URISyntaxException {
        List<String> dataset = Files.readAllLines(Paths.get(getClass().getResource("/barcode/gtin8.txt").toURI()));
        dataset.forEach(gtin8 -> {
            Assertions.assertThat(GS1CodeUtils.isCodeValid(gtin8, ImmutableSet.of(GS1CodeFormat.GTIN_8))).isTrue();
        });
    }

    @Test
    public void testValidUPCs() throws IOException, URISyntaxException {
        List<String> dataset = Files.readAllLines(Paths.get(getClass().getResource("/barcode/upca.txt").toURI()));
        dataset.forEach(upc -> {
            Assertions.assertThat(GS1CodeUtils.isCodeValid(upc, ImmutableSet.of(GS1CodeFormat.UPC_A))).isTrue();
        });
    }

    @Test
    public void testValidEAN13() throws IOException, URISyntaxException {
        List<String> dataset = Files.readAllLines(Paths.get(getClass().getResource("/barcode/ean13.txt").toURI()));
        dataset.forEach(ean13 -> {
            Assertions.assertThat(GS1CodeUtils.isCodeValid(ean13, ImmutableSet.of(GS1CodeFormat.EAN_13))).isTrue();
        });
    }

    @Test
    public void testValidGTIN14() throws IOException, URISyntaxException {
        List<String> dataset = Files.readAllLines(Paths.get(getClass().getResource("/barcode/gtin14.txt").toURI()));
        dataset.forEach(gtin14 -> {
            Assertions.assertThat(GS1CodeUtils.isCodeValid(gtin14, ImmutableSet.of(GS1CodeFormat.GTIN_14))).isTrue();
        });
    }

    @Test
    public void testValidSSCC() throws IOException, URISyntaxException {
        List<String> dataset = Files.readAllLines(Paths.get(getClass().getResource("/barcode/sscc.txt").toURI()));
        dataset.forEach(sscc -> {
            Assertions.assertThat(GS1CodeUtils.isCodeValid(sscc, ImmutableSet.of(GS1CodeFormat.SSCC))).isTrue();
        });
    }

    @Test
    public void testInvalidGTIN8() throws IOException, URISyntaxException {
        List<String> dataset = Files.readAllLines(Paths.get(getClass().getResource("/barcode/gtin8.txt").toURI()));
        dataset.forEach(gtin8 -> {
            gtin8 = spoilCheckDigit(gtin8);
            Assertions.assertThat(GS1CodeUtils.isCodeValid(gtin8, ImmutableSet.of(GS1CodeFormat.GTIN_8))).isFalse();
        });
    }

    @Test
    public void testInvalidUPCs() throws IOException, URISyntaxException {
        List<String> dataset = Files.readAllLines(Paths.get(getClass().getResource("/barcode/upca.txt").toURI()));
        dataset.forEach(upc -> {
            upc = spoilCheckDigit(upc);
            Assertions.assertThat(GS1CodeUtils.isCodeValid(upc, ImmutableSet.of(GS1CodeFormat.UPC_A))).isFalse();
        });
    }

    @Test
    public void testInvalidEAN13() throws IOException, URISyntaxException {
        List<String> dataset = Files.readAllLines(Paths.get(getClass().getResource("/barcode/ean13.txt").toURI()));
        dataset.forEach(ean13 -> {
            ean13 = spoilCheckDigit(ean13);
            Assertions.assertThat(GS1CodeUtils.isCodeValid(ean13, ImmutableSet.of(GS1CodeFormat.EAN_13))).isFalse();
        });
    }

    @Test
    public void testInvalidGTIN14() throws IOException, URISyntaxException {
        List<String> dataset = Files.readAllLines(Paths.get(getClass().getResource("/barcode/gtin14.txt").toURI()));
        dataset.forEach(gtin14 -> {
            gtin14 = spoilCheckDigit(gtin14);
            Assertions.assertThat(GS1CodeUtils.isCodeValid(gtin14, ImmutableSet.of(GS1CodeFormat.GTIN_14))).isFalse();
        });
    }

    @Test
    public void testInvalidSSCC() throws IOException, URISyntaxException {
        List<String> dataset = Files.readAllLines(Paths.get(getClass().getResource("/barcode/sscc.txt").toURI()));
        dataset.forEach(sscc -> {
            sscc = spoilCheckDigit(sscc);
            Assertions.assertThat(GS1CodeUtils.isCodeValid(sscc, ImmutableSet.of(GS1CodeFormat.SSCC))).isFalse();
        });
    }

    @Test
    public void testGetCheckDigit() {
        GS1Code code = GS1Code.createOrThrow("3809300214219");
        Assertions.assertThat(code.getCheckDigit()).isEqualTo(9);
    }

    /**
     * See {@link GS1CodeUtils#isForInternalUsage(String, GS1CodeFormat)}.
     */
    @Test
    public void testInternalUsageCheck() {
        GS1Code code = GS1Code.createOrThrow("2809300214210");
        Assertions.assertThat(code.isForInternalUsage()).isTrue();
    }

    @Test
    public void testExpandedBarcodes() {
        String shortCode = "609387639157";
        GS1Code code = GS1Code.createOrThrow(shortCode);

        GS1Code expandedToGTIN8 = code.expandedTo(GS1CodeFormat.GTIN_8); // should not expand
        GS1Code expandedToUPCA = code.expandedTo(GS1CodeFormat.UPC_A); // should not expand
        GS1Code expandedToGTIN12 = code.expandedTo(GS1CodeFormat.GTIN_12); // should not expand
        GS1Code expandedToGTIN13 = code.expandedTo(GS1CodeFormat.GTIN_13);
        GS1Code expandedToEAN13 = code.expandedTo(GS1CodeFormat.EAN_13);
        GS1Code expandedToGTIN14 = code.expandedTo(GS1CodeFormat.GTIN_14);
        GS1Code expandedToGSIN = code.expandedTo(GS1CodeFormat.GSIN);
        GS1Code expandedToSSCC = code.expandedTo(GS1CodeFormat.SSCC);

        Assertions.assertThat(expandedToGTIN8).isEqualTo(code);
        Assertions.assertThat(expandedToUPCA).isEqualTo(code);
        Assertions.assertThat(expandedToGTIN12).isEqualTo(code);

        Assertions.assertThat(expandedToGTIN13.getCode()).isEqualTo("0" + shortCode);
        Assertions.assertThat(expandedToEAN13.getCode()).isEqualTo("0" + shortCode);
        Assertions.assertThat(expandedToGTIN14.getCode()).isEqualTo("00" + shortCode);
        Assertions.assertThat(expandedToGSIN.getCode()).isEqualTo("00000" + shortCode);
        Assertions.assertThat(expandedToSSCC.getCode()).isEqualTo("000000" + shortCode);
    }

    @Test
    public void testReducedBarcodes() {
        // 1. Cannot shrink to less than 8 symbols
        GS1Code code = GS1Code.createOrThrow("00048767");
        Assertions.assertThat(code.reduced()).isEqualTo(code);

        // 2. Cannot shrink to non existing format
        code = GS1Code.createOrThrow("000111111114");
        Assertions.assertThat(code.reduced()).isEqualTo(code);

        // 3. Can shrink to shorter format
        code = GS1Code.createOrThrow("000011111115");
        Assertions.assertThat(code.reduced()).isEqualTo(GS1Code.createOrThrow("11111115"));

        // 4. Can shrink to shorter format through several "steps"
        code = GS1Code.createOrThrow("00000000000048767");
        Assertions.assertThat(code.reduced()).isEqualTo(GS1Code.createOrThrow("00048767"));

        // 5. No leading zeroes at all
        code = GS1Code.createOrThrow("60804002973162");
        Assertions.assertThat(code.reduced()).isEqualTo(code);
    }

    @Test
    public void testIsValidIsbn10() {
        Assertions.assertThat(GS1CodeUtils.isValidIsbn10("0201530821")).isTrue();
        Assertions.assertThat(GS1CodeUtils.isValidIsbn10("0201530822")).isFalse();
        Assertions.assertThat(GS1CodeUtils.isValidIsbn10("020153082X")).isFalse();
        Assertions.assertThat(GS1CodeUtils.isValidIsbn10("140723997X")).isTrue();
        Assertions.assertThat(GS1CodeUtils.isValidIsbn10("240723997X")).isFalse();
    }

    private static String spoilCheckDigit(String validCode) {
        int checkDigit = Character.digit(validCode.charAt(validCode.length() - 1), GS1CodeUtils.DIGIT_RADIX);
        int offset = RANDOM.nextInt(1, 10);
        int newCheckDigit = (checkDigit + offset) % 10;
        return validCode.substring(0, validCode.length() - 1) + newCheckDigit;
    }
}
