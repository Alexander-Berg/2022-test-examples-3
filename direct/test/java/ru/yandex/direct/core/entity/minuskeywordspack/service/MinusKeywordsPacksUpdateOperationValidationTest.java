package ru.yandex.direct.core.entity.minuskeywordspack.service;

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.minuskeywordspack.container.UpdatedMinusKeywordsPackInfo;
import ru.yandex.direct.core.entity.minuskeywordspack.model.MinusKeywordsPack;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.result.MassResult;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.core.entity.keyword.service.validation.phrase.minusphrase.MinusPhraseDefectIds.MinusPhrase.ILLEGAL_MINUS_KEYWORD_CHARS;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isSuccessful;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isSuccessfulWithMatchers;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.defect.ids.StringDefectIds.LENGTH_CANNOT_BE_MORE_THAN_MAX;
import static ru.yandex.direct.validation.result.DefectIds.MUST_BE_VALID_ID;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringRunner.class)
public class MinusKeywordsPacksUpdateOperationValidationTest extends MinusKeywordsPacksUpdateOperationBaseTest {

    @Test
    public void execute_Partial_OneValidItem_ResultIsOk() {
        Long packIdToUpdate = createLibraryMinusKeywordsPack(MINUS_WORD).getMinusKeywordPackId();
        List<ModelChanges<MinusKeywordsPack>> modelChanges =
                singletonList(minusKeywordsModelChanges(packIdToUpdate, MINUS_WORD_2));

        MassResult<UpdatedMinusKeywordsPackInfo> result = executePartial(modelChanges);

        UpdatedMinusKeywordsPackInfo expectedResult = expectedResult(packIdToUpdate, singletonList(MINUS_WORD_2));
        assumeThat(result, isSuccessfulWithMatchers(beanDiffer(expectedResult)));
    }

    @Test
    public void execute_Full_OneValidItem_ResultIsOk() {
        Long packIdToUpdate = createLibraryMinusKeywordsPack(MINUS_WORD).getMinusKeywordPackId();
        List<ModelChanges<MinusKeywordsPack>> modelChanges =
                singletonList(minusKeywordsModelChanges(packIdToUpdate, MINUS_WORD_2));

        MassResult<UpdatedMinusKeywordsPackInfo> result = executeFull(modelChanges);

        UpdatedMinusKeywordsPackInfo expectedResult = expectedResult(packIdToUpdate, singletonList(MINUS_WORD_2));
        assumeThat(result, isSuccessfulWithMatchers(beanDiffer(expectedResult)));
    }

    @Test
    public void execute_Partial_TwoValidItems_ResultIsOk() {
        Long packIdToUpdate1 = createLibraryMinusKeywordsPack(MINUS_WORD).getMinusKeywordPackId();
        Long packIdToUpdate2 = createLibraryMinusKeywordsPack(MINUS_WORD).getMinusKeywordPackId();
        List<ModelChanges<MinusKeywordsPack>> modelChanges =
                asList(minusKeywordsModelChanges(packIdToUpdate1, MINUS_WORD_2),
                        minusKeywordsModelChanges(packIdToUpdate2, MINUS_WORD_3));

        MassResult<UpdatedMinusKeywordsPackInfo> result = executePartial(modelChanges);

        UpdatedMinusKeywordsPackInfo expectedResult1 = expectedResult(packIdToUpdate1, singletonList(MINUS_WORD_2));
        UpdatedMinusKeywordsPackInfo expectedResult2 = expectedResult(packIdToUpdate2, singletonList(MINUS_WORD_3));
        assumeThat(result, isSuccessfulWithMatchers(beanDiffer(expectedResult1), beanDiffer(expectedResult2)));
    }

    @Test
    public void execute_Full_TwoValidItems_ResultIsOk() {
        Long packIdToUpdate1 = createLibraryMinusKeywordsPack(MINUS_WORD).getMinusKeywordPackId();
        Long packIdToUpdate2 = createLibraryMinusKeywordsPack(MINUS_WORD).getMinusKeywordPackId();
        List<ModelChanges<MinusKeywordsPack>> modelChanges =
                asList(minusKeywordsModelChanges(packIdToUpdate1, MINUS_WORD_2),
                        minusKeywordsModelChanges(packIdToUpdate2, MINUS_WORD_3));

        MassResult<UpdatedMinusKeywordsPackInfo> result = executeFull(modelChanges);

        UpdatedMinusKeywordsPackInfo expectedResult1 = expectedResult(packIdToUpdate1, singletonList(MINUS_WORD_2));
        UpdatedMinusKeywordsPackInfo expectedResult2 = expectedResult(packIdToUpdate2, singletonList(MINUS_WORD_3));
        assumeThat(result, isSuccessfulWithMatchers(beanDiffer(expectedResult1), beanDiffer(expectedResult2)));
    }

    // pre validation

    @Test
    public void execute_Partial_OneItemWithPreInvalidId_ResultHasInvalidItem() {
        List<ModelChanges<MinusKeywordsPack>> modelChanges = singletonList(minusKeywordsModelChanges(-1L, MINUS_WORD));
        MassResult<UpdatedMinusKeywordsPackInfo> result = executePartial(modelChanges);

        assertThat(result, isSuccessful(false));
        assertThat(result.getValidationResult(),
                hasDefectDefinitionWith(validationError(path(index(0), field("id")), MUST_BE_VALID_ID)));
    }

    @Test
    public void execute_Partial_OneItemWithPreInvalidMinusKeyword_ResultHasInvalidItem() {
        Long packIdToUpdate = createLibraryMinusKeywordsPack(MINUS_WORD).getMinusKeywordPackId();
        List<ModelChanges<MinusKeywordsPack>> modelChanges =
                singletonList(minusKeywordsModelChanges(packIdToUpdate, PREINVALID_MINUS_KEYWORD));
        MassResult<UpdatedMinusKeywordsPackInfo> result = executePartial(modelChanges);

        assertThat(result, isSuccessful(false));
        assertThat(result.getValidationResult(),
                hasDefectDefinitionWith(
                        validationError(path(index(0), field(MinusKeywordsPack.MINUS_KEYWORDS.name()), index(0)),
                                ILLEGAL_MINUS_KEYWORD_CHARS)));
    }

    @Test
    public void execute_Partial_OneValidItemAndOneItemWithPreInvalidMinusKeywords_ResultHasInvalidItem() {
        Long packIdToUpdate1 = createLibraryMinusKeywordsPack(MINUS_WORD).getMinusKeywordPackId();
        Long packIdToUpdate2 = createLibraryMinusKeywordsPack(MINUS_WORD).getMinusKeywordPackId();
        List<ModelChanges<MinusKeywordsPack>> modelChanges =
                asList(minusKeywordsModelChanges(packIdToUpdate1, MINUS_WORD),
                        minusKeywordsModelChanges(packIdToUpdate2, PREINVALID_MINUS_KEYWORD));

        MassResult<UpdatedMinusKeywordsPackInfo> result = executePartial(modelChanges);

        UpdatedMinusKeywordsPackInfo expectedResult1 = expectedResult(packIdToUpdate1, singletonList(MINUS_WORD));
        assertThat(result, isSuccessfulWithMatchers(beanDiffer(expectedResult1), null));
        assertThat(result.getValidationResult(),
                hasDefectDefinitionWith(
                        validationError(path(index(1), field(MinusKeywordsPack.MINUS_KEYWORDS.name()), index(0)),
                                ILLEGAL_MINUS_KEYWORD_CHARS)));
    }

    @Test
    public void execute_Full_OneValidItemAndOneItemWithPreInvalidMinusKeywords_ResultHasInvalidItem() {
        Long packIdToUpdate1 = createLibraryMinusKeywordsPack(MINUS_WORD).getMinusKeywordPackId();
        Long packIdToUpdate2 = createLibraryMinusKeywordsPack(MINUS_WORD).getMinusKeywordPackId();
        List<ModelChanges<MinusKeywordsPack>> modelChanges =
                asList(minusKeywordsModelChanges(packIdToUpdate1, MINUS_WORD),
                        minusKeywordsModelChanges(packIdToUpdate2, PREINVALID_MINUS_KEYWORD));

        MassResult<UpdatedMinusKeywordsPackInfo> result = executeFull(modelChanges);

        assertThat(result, isSuccessfulWithMatchers(equalTo(null), null));
        assertThat(result.getValidationResult(),
                hasDefectDefinitionWith(
                        validationError(path(index(1), field(MinusKeywordsPack.MINUS_KEYWORDS.name()), index(0)),
                                ILLEGAL_MINUS_KEYWORD_CHARS)));
    }

    // validation

    @Test
    public void execute_Partial_OneItemWithInvalidName_ResultHasInvalidItem() {
        Long packIdToUpdate = createLibraryMinusKeywordsPack(MINUS_WORD).getMinusKeywordPackId();
        List<ModelChanges<MinusKeywordsPack>> modelChanges =
                singletonList(nameModelChanges(packIdToUpdate, INVALID_NAME));
        MassResult<UpdatedMinusKeywordsPackInfo> result = executePartial(modelChanges);
        assertThat(result, isSuccessful(false));
        assertThat(result.getValidationResult(),
                hasDefectDefinitionWith(validationError(path(index(0), field("name")),
                        LENGTH_CANNOT_BE_MORE_THAN_MAX)));
    }

    @Test
    public void execute_Partial_OneValidItemAndOneItemWithInvalidName_ResultHasInvalidItem() {
        Long packIdToUpdate1 = createLibraryMinusKeywordsPack(MINUS_WORD).getMinusKeywordPackId();
        Long packIdToUpdate2 = createLibraryMinusKeywordsPack(MINUS_WORD).getMinusKeywordPackId();

        List<ModelChanges<MinusKeywordsPack>> modelChanges =
                asList(minusKeywordsModelChanges(packIdToUpdate1, MINUS_WORD_2),
                        nameModelChanges(packIdToUpdate2, INVALID_NAME));

        MassResult<UpdatedMinusKeywordsPackInfo> result = executePartial(modelChanges);

        UpdatedMinusKeywordsPackInfo expectedResult1 = expectedResult(packIdToUpdate1, singletonList(MINUS_WORD_2));
        assertThat(result, isSuccessfulWithMatchers(beanDiffer(expectedResult1), null));
        assertThat(result.getValidationResult(),
                hasDefectDefinitionWith(validationError(path(index(1), field("name")),
                        LENGTH_CANNOT_BE_MORE_THAN_MAX)));
    }

    @Test
    public void execute_Full_OneValidItemAndOneItemWithInvalidName_OperationFailed() {
        Long packIdToUpdate1 = createLibraryMinusKeywordsPack(MINUS_WORD).getMinusKeywordPackId();
        Long packIdToUpdate2 = createLibraryMinusKeywordsPack(MINUS_WORD).getMinusKeywordPackId();

        List<ModelChanges<MinusKeywordsPack>> modelChanges =
                asList(minusKeywordsModelChanges(packIdToUpdate1, MINUS_WORD_2),
                        nameModelChanges(packIdToUpdate2, INVALID_NAME));

        MassResult<UpdatedMinusKeywordsPackInfo> result = executeFull(modelChanges);

        assertThat(result, isSuccessfulWithMatchers(equalTo(null), null));
        assertThat(result.getValidationResult(),
                hasDefectDefinitionWith(validationError(path(index(1), field("name")),
                        LENGTH_CANNOT_BE_MORE_THAN_MAX)));
    }
}
