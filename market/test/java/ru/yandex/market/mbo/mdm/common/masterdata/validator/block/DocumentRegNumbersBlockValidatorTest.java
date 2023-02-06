package ru.yandex.market.mbo.mdm.common.masterdata.validator.block;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.blocks.ItemBlock;
import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.blocks.TestBlockCreationUtil;
import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.blocks.sskumd.ValueCommentBlock;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownMdmParams;
import ru.yandex.market.mbo.mdm.common.masterdata.validator.ItemBlockValidationResult;
import ru.yandex.market.mboc.common.MbocErrors;
import ru.yandex.market.mboc.common.masterdata.model.QualityDocument;
import ru.yandex.market.mboc.common.masterdata.model.TimeInUnits;
import ru.yandex.market.mboc.common.utils.ErrorInfo;

public class DocumentRegNumbersBlockValidatorTest {
    private static final DocumentRegNumbersBlockValidator VALIDATOR = new DocumentRegNumbersBlockValidator();

    private static ItemBlock createDocumentRegNumberBlock(List<String> regNumbers) {
        return TestBlockCreationUtil.createStringsMdmParamValueBlock(
            KnownMdmParams.DOCUMENT_REG_NUMBER,
            regNumbers,
            Instant.EPOCH
        );
    }

    private static ErrorInfo createError(String regNumber) {
        return MbocErrors.get().documentWithRegistrationNumbersDoesNotExist(regNumber);
    }

    private static Set<QualityDocument> createDocuments(List<String> regNumbers) {
        return regNumbers.stream()
            .map(regNumber -> new QualityDocument().setRegistrationNumber(regNumber))
            .collect(Collectors.toSet());
    }

    private static ItemBlockValidationData createValidationData(ItemBlock block,
                                                                List<String> existingDocumentsRegNumbers) {
        return new ItemBlockValidationData(List.of(block), Map.of(), createDocuments(existingDocumentsRegNumbers), 0L);
    }

    @Test
    public void whenBlockContainsOnlyExistingDocumentsReturnNoErrors() {
        ItemBlock block = createDocumentRegNumberBlock(List.of("123123", "856", "14447"));
        ItemBlockValidationData validationData = createValidationData(block, List.of("123123", "14447", "9119", "856"));

        ItemBlockValidationResult expectedResult = new ItemBlockValidationResult(block, List.of());

        Assertions.assertThat(VALIDATOR.validateBlock(block, validationData)).isEqualTo(expectedResult);
        Assertions.assertThat(VALIDATOR.findAndValidateBlock(validationData)).isEqualTo(expectedResult);
    }

    @Test
    public void whenBlockContainsMissingDocumentReturnError() {
        ItemBlock block = createDocumentRegNumberBlock(List.of("123124", "431", "845"));
        ItemBlockValidationData validationData = createValidationData(block, List.of("431", "123123", "65789"));

        ItemBlockValidationResult expectedResult = new ItemBlockValidationResult(block, List.of(
            createError("123124"), createError("845")
        ));

        Assertions.assertThat(VALIDATOR.validateBlock(block, validationData)).isEqualTo(expectedResult);
        Assertions.assertThat(VALIDATOR.findAndValidateBlock(validationData)).isEqualTo(expectedResult);
    }

    @Test
    public void whenBlockContainsNoRegNumbersReturnNoError() {
        ItemBlock block = createDocumentRegNumberBlock(List.of());
        ItemBlockValidationData validationData = createValidationData(block, List.of("123123"));

        ItemBlockValidationResult expectedResult = new ItemBlockValidationResult(block, List.of());

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
        ItemBlockValidationData validationData = createValidationData(shelfLifeBlock, List.of("123123"));

        Assertions.assertThat(VALIDATOR.validateBlock(shelfLifeBlock, validationData))
            .isEqualTo(new ItemBlockValidationResult(shelfLifeBlock, List.of()));
        Assertions.assertThat(VALIDATOR.findAndValidateBlock(validationData))
            .isEqualTo(new ItemBlockValidationResult(null, List.of()));
    }
}
