package ru.yandex.market.mbo.mdm.common.masterdata.validator.block;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.SplittableRandom;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.commons.validator.routines.checkdigit.CheckDigitException;
import org.apache.commons.validator.routines.checkdigit.EAN13CheckDigit;
import org.assertj.core.api.Assertions;
import org.junit.Test;

import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.blocks.ItemBlock;
import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.blocks.TestBlockCreationUtil;
import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.blocks.sskumd.MdmParamValueBlock;
import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.blocks.sskumd.ValueCommentBlock;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownMdmParams;
import ru.yandex.market.mbo.mdm.common.masterdata.validator.ItemBlockValidationResult;
import ru.yandex.market.mbo.mdm.common.util.GtinUtils;
import ru.yandex.market.mboc.common.MbocErrors;
import ru.yandex.market.mboc.common.masterdata.TestDataUtils;
import ru.yandex.market.mboc.common.masterdata.model.TimeInUnits;
import ru.yandex.market.mboc.common.masterdata.parsing.SskuMasterDataFields;
import ru.yandex.market.mboc.common.utils.ErrorInfo;

@SuppressWarnings("checkstyle:MagicNumber")
public class GtinValidatorTest {
    private static final GTINValidator VALIDATOR = new GTINValidator();
    private static final SplittableRandom RANDOM = new SplittableRandom(155493L);

    private static String gtin() {
        return TestDataUtils.generateValidGTIN(RANDOM);
    }

    private static List<String> gtins(int amount) {
        return IntStream.range(0, amount).mapToObj(i -> gtin())
            .map(String::valueOf)
            .collect(Collectors.toList());
    }

    private static MdmParamValueBlock<String> createBlock(List<String> gtins) {
        return TestBlockCreationUtil.createStringsMdmParamValueBlock(
            KnownMdmParams.GTIN,
            gtins,
            Instant.EPOCH
        );
    }

    private static ErrorInfo createError(String gtin) {
        return MbocErrors.get().excelInvalidGtin(SskuMasterDataFields.GTINS, gtin);
    }

    private static void assertValid(String gtin) {
        ItemBlock block = createBlock(Collections.singletonList(gtin));
        var validationData = new ItemBlockValidationData(List.of(block));

        var expectedResult = new ItemBlockValidationResult(block, List.of());

        Assertions.assertThat(GTINValidator.validateGTIN(gtin)).isEmpty();
        Assertions.assertThat(VALIDATOR.validateBlock(block, validationData)).isEqualTo(expectedResult);
        Assertions.assertThat(VALIDATOR.findAndValidateBlock(validationData)).isEqualTo(expectedResult);
    }

    private static void assertNotValid(String gtin) {
        ItemBlock block = createBlock(Collections.singletonList(gtin));
        var validationData = new ItemBlockValidationData(List.of(block));

        ErrorInfo expectedError = createError(gtin);
        var expectedResult = new ItemBlockValidationResult(block, List.of(expectedError));

        Assertions.assertThat(GTINValidator.validateGTIN(gtin)).isPresent();
        Assertions.assertThat(VALIDATOR.validateBlock(block, validationData)).isEqualTo(expectedResult);
        Assertions.assertThat(VALIDATOR.findAndValidateBlock(validationData)).isEqualTo(expectedResult);
    }

    @Test
    public void whenValidGtinsShouldNotReturnErrors() {
        gtins(100).forEach(GtinValidatorTest::assertValid);
    }

    @Test
    public void whenValidManualExamplesShouldNotReturnErrors() {
        List<String> examples = List.of(
            "12345670",
            "10000007",
            "96827567",

            "123456789012",
            "100000000007",
            "582040022537",

            "1234567890128",
            "1000000000009",
            "2194188471030",

            "12345678901231",
            "10000000000007",
            "74073220588004"
        );
        examples.forEach(GtinValidatorTest::assertValid);
    }

    @Test
    public void whenEan13ValidShouldPassValidation() throws CheckDigitException {
        long min = 100000000000L;
        long max = min * 10;
        for (int i = 0; i < 100500; ++i) {
            String ean13WithoutCheckDigit = String.valueOf(RANDOM.nextLong(min, max));
            int checkDigit = Integer.parseInt(EAN13CheckDigit.EAN13_CHECK_DIGIT.calculate(ean13WithoutCheckDigit));
            String ean13 = ean13WithoutCheckDigit + checkDigit;
            Assertions.assertThat(GtinUtils.computeChecksum(ean13)).isEqualTo(checkDigit);
            Assertions.assertThat(GtinUtils.isValid(ean13WithoutCheckDigit + GtinUtils.computeChecksum(ean13)))
                .isTrue();
        }
    }

    @Test
    public void whenNonDigitsShouldReturnError() {
        List<String> invalidGtins = List.of(
            "",
            "null",
            "12O43",    // O is letter
            "-12",      // leading sign
            "24.6",     // decimal point
            "24,6",     // decimal comma
            "23463321 " // spaces
        );
        invalidGtins.forEach(GtinValidatorTest::assertNotValid);
        assertNotValid(null);
    }

    @Test
    public void whenInvalidLengthShouldReturnError() {
        List<String> invalidLengthGtins = List.of(
            "1234567",
            "123456789",
            "123456789012345"
        );
        invalidLengthGtins.forEach(GtinValidatorTest::assertNotValid);
    }

    @Test
    public void testReturnErrorForEachInvalid() {
        List<String> gtins = List.of(
            "123456789",      //invalid
            "10000007",       //valid
            "123456789012345" //invalid
        );
        ItemBlock block = createBlock(gtins);
        var validationData = new ItemBlockValidationData(List.of(block));

        List<ErrorInfo> expectedErrors = List.of(createError("123456789"), createError("123456789012345"));
        var expectedResult = new ItemBlockValidationResult(block, expectedErrors);

        Assertions.assertThat(VALIDATOR.validateBlock(block, validationData)).isEqualTo(expectedResult);
        Assertions.assertThat(VALIDATOR.findAndValidateBlock(validationData)).isEqualTo(expectedResult);
    }

    @Test
    public void whenNoBLockReturnNoError() {
        var validationData = new ItemBlockValidationData(List.of());
        var expectedResult = new ItemBlockValidationResult(null, List.of());

        Assertions.assertThat(VALIDATOR.validateBlock(null, validationData)).isEqualTo(expectedResult);
        Assertions.assertThat(VALIDATOR.findAndValidateBlock(validationData)).isEqualTo(expectedResult);
    }

    @Test
    public void whenNotSupportedBlockReturnNoError() {
        ValueCommentBlock shelfLifeBlock =
            TestBlockCreationUtil.createShelfLifeBlock(TimeInUnits.UNLIMITED, "", Instant.EPOCH);
        var validationData = new ItemBlockValidationData(List.of(shelfLifeBlock));

        Assertions.assertThat(VALIDATOR.validateBlock(shelfLifeBlock, validationData))
            .isEqualTo(new ItemBlockValidationResult(shelfLifeBlock, List.of()));
        Assertions.assertThat(VALIDATOR.findAndValidateBlock(validationData))
            .isEqualTo(new ItemBlockValidationResult(null, List.of()));
    }
}
