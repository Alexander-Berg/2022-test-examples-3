package ru.yandex.market.mbo.mdm.common.masterdata.validator.block;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.blocks.ItemBlock;
import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.blocks.TestBlockCreationUtil;
import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.blocks.sskumd.CustomsCommCodeBlock;
import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.blocks.sskumd.ValueCommentBlock;
import ru.yandex.market.mbo.mdm.common.masterdata.services.cccode.ExistingCCCodeCacheMock;
import ru.yandex.market.mbo.mdm.common.masterdata.services.iris.BeruId;
import ru.yandex.market.mbo.mdm.common.masterdata.services.iris.BeruIdMock;
import ru.yandex.market.mbo.mdm.common.masterdata.validator.ItemBlockValidationResult;
import ru.yandex.market.mbo.mdm.common.utils.MdmBaseDbTestClass;
import ru.yandex.market.mbo.storage.StorageKeyValueService;
import ru.yandex.market.mboc.common.MbocErrors;
import ru.yandex.market.mboc.common.masterdata.model.TimeInUnits;
import ru.yandex.market.mboc.common.masterdata.parsing.SskuMasterDataFields;
import ru.yandex.market.mboc.common.offers.model.ShopSkuKey;
import ru.yandex.market.mboc.common.utils.ErrorInfo;
import ru.yandex.market.mboc.common.utils.MdmProperties;

import static org.assertj.core.api.SoftAssertions.assertSoftly;

public class CustomsCommodityCodeBlockValidatorTest extends MdmBaseDbTestClass {

    private static final String VALID_AND_EXISTING_CCCODE = "1604320010";
    private static final String VALID_AND_NOT_EXISTING_CCCODE = "2106909300";
    private static final ShopSkuKey SHOP_SKU_KEY = new ShopSkuKey(999, "sku");
    private static final ShopSkuKey SHOP_SKU_KEY_1P = new ShopSkuKey(10001, "sku");
    private static final String MBOC_UNKNOWN_CCCODE_ERROR_CODE = MbocErrors.get()
        .mdUnknownCustomsCommodityCode("", "")
        .getErrorCode();

    @Autowired
    private StorageKeyValueService skv;

    private final BeruId beruId = new BeruIdMock(10000, 10001);
    private final ExistingCCCodeCacheMock existingCCCodeCacheMock = new ExistingCCCodeCacheMock();

    private CustomsCommodityCodeBlockValidator validator;

    @Before
    public void setUp() throws Exception {
        validator = new CustomsCommodityCodeBlockValidator(
            beruId,
            skv,
            existingCCCodeCacheMock
        );

        setValidationMode(CustomsCommodityCodeBlockValidator.ValidationMode.GENERATE_ERROR);
        existingCCCodeCacheMock.deleteAll();
        existingCCCodeCacheMock.add(VALID_AND_EXISTING_CCCODE);
    }

    private static ErrorInfo createError(String value) {
        return MbocErrors.get().mdCustomsCommodityCode(SskuMasterDataFields.CUSTOMS_COMMODITY_CODE, value);
    }

    private void assertNotValid(SoftAssertions softly, String value) {
        CustomsCommCodeBlock block = TestBlockCreationUtil.createCustomsCommodityBlock(value,
            Instant.EPOCH,
            SHOP_SKU_KEY.getSupplierId());
        ItemBlockValidationData validationData = new ItemBlockValidationData(List.of(block));

        ErrorInfo expectedError = createError(value);
        ItemBlockValidationResult expectedResult = new ItemBlockValidationResult(block, List.of(expectedError));

        softly.assertThat(CustomsCommodityCodeBlockValidator.validateCustomsCommodityCode(value))
            .contains(expectedError);
        softly.assertThat(validator.validateBlock(block, validationData)).isEqualTo(expectedResult);
        softly.assertThat(validator.findAndValidateBlock(validationData)).isEqualTo(expectedResult);
    }

    private void assertValid(SoftAssertions softly, String value) {
        CustomsCommCodeBlock block = TestBlockCreationUtil.createCustomsCommodityBlock(value,
            Instant.EPOCH,
            SHOP_SKU_KEY.getSupplierId());
        ItemBlockValidationData validationData = new ItemBlockValidationData(List.of(block));

        ItemBlockValidationResult expectedResult = new ItemBlockValidationResult(block, List.of());

        softly.assertThat(CustomsCommodityCodeBlockValidator.validateCustomsCommodityCode(value)).isEmpty();
        softly.assertThat(validator.validateBlock(block, validationData)).isEqualTo(expectedResult);
        softly.assertThat(validator.findAndValidateBlock(validationData)).isEqualTo(expectedResult);
    }

    @Test
    public void whenValidatingCustomsCommodityCodeNonNumericShouldFail() {
        assertSoftly(softly -> {
            assertNotValid(softly, "123456789C");
            assertNotValid(softly, "1234 56789");
            assertNotValid(softly, "1234_56789");
            assertNotValid(softly, "1234,56789");
            assertNotValid(softly, "1234.56789");
            assertNotValid(softly, "1234-56789");
        });
    }

    @Test
    public void whenCustomsCommodityCodeLengthDoesNotMatchShouldFail() {
        assertSoftly(softly -> {
            assertValid(softly, "1234567890");
            assertValid(softly, "12345678901234");
            assertNotValid(softly, "123456789");
            assertNotValid(softly, "12345678900");
            assertNotValid(softly, "123456789000");
            assertNotValid(softly, "123456789000000");
        });
    }

    @Test
    public void whenCustomsCommodityCodeContainsOneCharacterShouldFail() {
        assertSoftly(softly -> {
            assertNotValid(softly, "1111");
            assertNotValid(softly, "0000000000");
            assertNotValid(softly, "11111111111");
            assertNotValid(softly, "99999999999999");
            assertValid(softly, "99999999999991");
        });
    }

    @Test
    public void whenCustomsCommodityCodeContainsMoreThanOneZeroAtTheStart() {
        assertSoftly(softly -> {
            assertNotValid(softly, "0099999999");
            assertValid(softly, "0111111111");
            assertNotValid(softly, "00000000000009");
        });
    }

    @Test
    public void whenNoBLockReturnNoError() {
        ItemBlockValidationData validationData = new ItemBlockValidationData(List.of());
        ItemBlockValidationResult expectedResult = new ItemBlockValidationResult(null, List.of());
        Assertions.assertThat(validator.validateBlock(null, validationData)).isEqualTo(expectedResult);
        Assertions.assertThat(validator.findAndValidateBlock(validationData)).isEqualTo(expectedResult);
    }

    @Test
    public void whenNotSupportedBlockReturnNoError() {
        ValueCommentBlock shelfLifeBlock =
            TestBlockCreationUtil.createShelfLifeBlock(TimeInUnits.UNLIMITED, "", Instant.EPOCH);
        ItemBlockValidationData validationData = new ItemBlockValidationData(List.of(shelfLifeBlock));

        Assertions.assertThat(validator.validateBlock(shelfLifeBlock, validationData))
            .isEqualTo(new ItemBlockValidationResult(shelfLifeBlock, List.of()));
        Assertions.assertThat(validator.findAndValidateBlock(validationData))
            .isEqualTo(new ItemBlockValidationResult(null, List.of()));
    }

    @Test
    public void testAllValidationModes() {
        var testBlock = prepareCCCodeItemBlock(VALID_AND_NOT_EXISTING_CCCODE, SHOP_SKU_KEY_1P.getSupplierId());
        ItemBlockValidationData validationData = new ItemBlockValidationData(List.of(testBlock));

        //NOOP mode
        setValidationMode(CustomsCommodityCodeBlockValidator.ValidationMode.NO_OP);
        Collection<ErrorInfo> errors = validator.validateBlock(testBlock, validationData).getErrorInfos();
        Assertions.assertThat(errors).isEmpty();

        // WARNING mode
        setValidationMode(CustomsCommodityCodeBlockValidator.ValidationMode.GENERATE_WARNING);
        errors = validator.validateBlock(testBlock, validationData).getErrorInfos();
        var error = errors.stream().findFirst().orElseThrow();
        Assertions.assertThat(errors).hasSize(1);
        Assertions.assertThat(error.getErrorCode()).isEqualTo(MBOC_UNKNOWN_CCCODE_ERROR_CODE);
        Assertions.assertThat(error.getLevel()).isEqualTo(ErrorInfo.Level.WARNING);

        // ERROR mode
        setValidationMode(CustomsCommodityCodeBlockValidator.ValidationMode.GENERATE_ERROR);
        errors = validator.validateBlock(testBlock, validationData).getErrorInfos();
        error = errors.stream().findFirst().orElseThrow();
        Assertions.assertThat(errors).hasSize(1);
        Assertions.assertThat(error.getErrorCode()).isEqualTo(MBOC_UNKNOWN_CCCODE_ERROR_CODE);
        Assertions.assertThat(error.getLevel()).isEqualTo(ErrorInfo.Level.ERROR);
    }

    @Test
    public void whenCCCodeIsNotExistAndSupplierIs1pShouldFailValidation() {
        var testBlock = prepareCCCodeItemBlock(VALID_AND_NOT_EXISTING_CCCODE, SHOP_SKU_KEY_1P.getSupplierId());
        ItemBlockValidationData validationData = new ItemBlockValidationData(List.of(testBlock));
        Collection<ErrorInfo> errors = validator.validateBlock(testBlock, validationData).getErrorInfos();

        var error = errors.stream().findFirst().orElseThrow();
        Assertions.assertThat(errors).hasSize(1);
        Assertions.assertThat(error.getErrorCode()).isEqualTo(MBOC_UNKNOWN_CCCODE_ERROR_CODE);
        Assertions.assertThat(error.getParams().get("value")).isEqualTo(VALID_AND_NOT_EXISTING_CCCODE);
    }

    @Test
    public void whenCCCodeIsNotExistAndSupplierIsNot1pShouldPassValidation() {
        var testBlock = prepareCCCodeItemBlock(VALID_AND_NOT_EXISTING_CCCODE, SHOP_SKU_KEY.getSupplierId());
        ItemBlockValidationData validationData = new ItemBlockValidationData(List.of(testBlock));

        Collection<ErrorInfo> errors = validator.validateBlock(testBlock, validationData).getErrorInfos();

        Assertions.assertThat(errors).isEmpty();
    }

    @Test
    public void whenCCCodeIsExistAndSupplierIs1pShouldPassValidation() {
        var testBlock = prepareCCCodeItemBlock(VALID_AND_EXISTING_CCCODE, SHOP_SKU_KEY_1P.getSupplierId());
        ItemBlockValidationData validationData = new ItemBlockValidationData(List.of(testBlock));
        Collection<ErrorInfo> errors = validator.validateBlock(testBlock, validationData).getErrorInfos();

        Assertions.assertThat(errors).isEmpty();
    }

    private void setValidationMode(CustomsCommodityCodeBlockValidator.ValidationMode validationMode) {
        skv.putValue(MdmProperties.CCC_VALIDATION_MODE, validationMode);
        skv.invalidateCache();
    }

    private ItemBlock prepareCCCodeItemBlock(String code, Integer supplierId) {
        return TestBlockCreationUtil.createCustomsCommodityBlock(code,
            Instant.EPOCH,
            supplierId
        );
    }
}
