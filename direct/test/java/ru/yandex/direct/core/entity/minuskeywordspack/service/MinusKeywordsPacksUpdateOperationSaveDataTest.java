package ru.yandex.direct.core.entity.minuskeywordspack.service;

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.CompareStrategy;
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies;
import ru.yandex.direct.core.entity.minuskeywordspack.container.UpdatedMinusKeywordsPackInfo;
import ru.yandex.direct.core.entity.minuskeywordspack.model.MinusKeywordsPack;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.result.MassResult;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath.newPath;
import static ru.yandex.direct.core.entity.keyword.service.validation.phrase.minusphrase.MinusPhraseDefects.minusWordsPackNotFound;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isSuccessfulWithMatchers;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringRunner.class)
public class MinusKeywordsPacksUpdateOperationSaveDataTest extends MinusKeywordsPacksUpdateOperationBaseTest {

    private static final CompareStrategy COMPARE_STRATEGY = DefaultCompareStrategies.allFields()
            .forFields(newPath(MinusKeywordsPack.HASH.name())).useMatcher(notNullValue());

    @Test
    public void execute_NoChange_SavedCorrectly() {
        Long packToUpdate = createLibraryMinusKeywordsPack(MINUS_WORD).getMinusKeywordPackId();

        List<ModelChanges<MinusKeywordsPack>> changesPacks =
                singletonList(minusKeywordsModelChanges(packToUpdate, MINUS_WORD));
        MassResult<UpdatedMinusKeywordsPackInfo> result = executePartial(changesPacks);
        UpdatedMinusKeywordsPackInfo expectedResult = expectedResult(packToUpdate, singletonList(MINUS_WORD));
        assumeThat(result, isSuccessfulWithMatchers(beanDiffer(expectedResult)));

        MinusKeywordsPack expectedPack = expectedPack(packToUpdate, singletonList(MINUS_WORD));
        assertUpdatedPack(packToUpdate, expectedPack);
    }

    @Test
    public void execute_ChangeMinusKeywords_SavedCorrectly() {
        Long packToUpdate = createLibraryMinusKeywordsPack(MINUS_WORD).getMinusKeywordPackId();

        List<ModelChanges<MinusKeywordsPack>> changesPacks =
                singletonList(minusKeywordsModelChanges(packToUpdate, MINUS_WORD, MINUS_WORD_2));
        MassResult<UpdatedMinusKeywordsPackInfo> result = executePartial(changesPacks);
        UpdatedMinusKeywordsPackInfo expectedResult = expectedResult(packToUpdate, asList(MINUS_WORD, MINUS_WORD_2));
        assumeThat(result, isSuccessfulWithMatchers(beanDiffer(expectedResult)));

        MinusKeywordsPack expectedPack = expectedPack(packToUpdate, asList(MINUS_WORD, MINUS_WORD_2));
        assertUpdatedPack(packToUpdate, expectedPack);
    }

    @Test
    public void execute_ChangeName_SavedCorrectly() {
        Long packToUpdate = createLibraryMinusKeywordsPack(MINUS_WORD).getMinusKeywordPackId();

        List<ModelChanges<MinusKeywordsPack>> changesPacks = singletonList(nameModelChanges(packToUpdate, SECOND_NAME));
        MassResult<UpdatedMinusKeywordsPackInfo> result = executePartial(changesPacks);
        UpdatedMinusKeywordsPackInfo expectedResult = expectedResult(packToUpdate, singletonList(MINUS_WORD))
                .withName(SECOND_NAME);
        assumeThat(result, isSuccessfulWithMatchers(beanDiffer(expectedResult)));

        MinusKeywordsPack expectedPack = expectedPack(packToUpdate, singletonList(MINUS_WORD))
                .withName(SECOND_NAME);
        assertUpdatedPack(packToUpdate, expectedPack);
    }

    // сохранение несколько минус фраз

    @Test
    public void execute_ChangeNameAndChangeMinusKeywords_DataSavedCorrectly() {
        Long packToUpdate1 = createLibraryMinusKeywordsPack(MINUS_WORD).getMinusKeywordPackId();
        Long packToUpdate2 = createLibraryMinusKeywordsPack(MINUS_WORD).getMinusKeywordPackId();


        List<ModelChanges<MinusKeywordsPack>> changesPacks = asList(nameModelChanges(packToUpdate1, SECOND_NAME),
                minusKeywordsModelChanges(packToUpdate2, MINUS_WORD, MINUS_WORD_2));
        MassResult<UpdatedMinusKeywordsPackInfo> result = executePartial(changesPacks);
        UpdatedMinusKeywordsPackInfo expectedResult1 = expectedResult(packToUpdate1, singletonList(MINUS_WORD))
                .withName(SECOND_NAME);
        UpdatedMinusKeywordsPackInfo expectedResult2 = expectedResult(packToUpdate2, asList(MINUS_WORD, MINUS_WORD_2));
        assumeThat(result, isSuccessfulWithMatchers(beanDiffer(expectedResult1), beanDiffer(expectedResult2)));

        MinusKeywordsPack expectedPack1 = expectedPack(packToUpdate1, singletonList(MINUS_WORD))
                .withName(SECOND_NAME);
        assertUpdatedPack(packToUpdate1, expectedPack1);
        MinusKeywordsPack expectedPack2 = expectedPack(packToUpdate2, asList(MINUS_WORD, MINUS_WORD_2));
        assertUpdatedPack(packToUpdate2, expectedPack2);
    }

    @Test
    public void prepareAndApply_MakeLibraryPackPrivate_PackRemainsLibrary() {
        MinusKeywordsPack minusKeywordsPack = createLibraryMinusKeywordsPack(MINUS_WORD).getMinusKeywordsPack();
        ModelChanges<MinusKeywordsPack> modelChanges =
                new ModelChanges<>(minusKeywordsPack.getId(), MinusKeywordsPack.class)
                        .process(false, MinusKeywordsPack.IS_LIBRARY);
        executePartial(singletonList(modelChanges));
        MinusKeywordsPack actualPack = minusKeywordsPackRepository
                .get(clientInfo.getShard(), clientInfo.getClientId(), singletonList(minusKeywordsPack.getId())).get(0);
        assertThat(actualPack.getIsLibrary(), is(true));
    }

    @Test
    public void prepareAndApply_UpdatePrivatePack_Error() {
        MinusKeywordsPack minusKeywordsPack =
                steps.minusKeywordsPackSteps().createPrivateMinusKeywordsPack(clientInfo).getMinusKeywordsPack();
        ModelChanges<MinusKeywordsPack> modelChanges =
                new ModelChanges<>(minusKeywordsPack.getId(), MinusKeywordsPack.class)
                        .process(asList("раз", "два", "три"), MinusKeywordsPack.MINUS_KEYWORDS);
        MassResult<UpdatedMinusKeywordsPackInfo> result = executePartial(singletonList(modelChanges));
        assertThat(result.getValidationResult(), hasDefectDefinitionWith(
                validationError(path(index(0), field(MinusKeywordsPack.ID)), minusWordsPackNotFound())));
    }

    private MinusKeywordsPack expectedPack(Long id, List<String> minusKeywords) {
        return new MinusKeywordsPack()
                .withId(id)
                .withName(DEFAULT_NAME)
                .withMinusKeywords(minusKeywords)
                .withIsLibrary(true);
    }

    private void assertUpdatedPack(Long id, MinusKeywordsPack expected) {
        MinusKeywordsPack actual = getMinusKeywordsPack(id);
        assertThat("сохраненный список минус слов отличается от ожидаемой",
                actual, beanDiffer(expected).useCompareStrategy(COMPARE_STRATEGY));
    }
}
