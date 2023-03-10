package ru.yandex.direct.core.entity.minuskeywordspack.service;

import java.util.Collection;
import java.util.List;

import com.google.common.collect.ImmutableSet;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.CompareStrategy;
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies;
import ru.yandex.direct.core.entity.keyword.service.validation.phrase.minusphrase.MinusPhraseValidator;
import ru.yandex.direct.core.entity.minuskeywordspack.model.MinusKeywordsPack;
import ru.yandex.direct.core.entity.minuskeywordspack.repository.MinusKeywordsPackRepository;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.operation.Applicability;
import ru.yandex.direct.validation.defect.CommonDefects;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.thymeleaf.util.SetUtils.singletonSet;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath.newPath;
import static ru.yandex.direct.core.entity.keyword.service.validation.phrase.minusphrase.MinusPhraseDefects.illegalMinusKeywordChars;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringRunner.class)
public class AddMinusKeywordsPackSubOperationTest {

    @Autowired
    private AddMinusKeywordsPackSubOperationFactory factory;
    @Autowired
    private MinusKeywordsPackRepository minusKeywordsPackRepository;

    @Autowired
    private Steps steps;

    private ClientInfo clientInfo;

    @Before
    public void before() {
        clientInfo = steps.clientSteps().createDefaultClient();
    }

    @Test
    public void emptyMinusKeywords_NothingHappen() {
        AddMinusKeywordsPackSubOperation subOperation = createSubOperation(emptyList());
        subOperation.prepare();
        subOperation.setIndexesToApply(emptySet());
        subOperation.apply();
        List<Long> minusKeywordPackIds = subOperation.getMinusKeywordPackIds();
        assertThat(minusKeywordPackIds, hasSize(0));
    }

    @Test
    public void minusKeywordsPackAddedSuccessfully() {
        List<String> minusKeywords = asList("??????", "??????");
        AddMinusKeywordsPackSubOperation subOperation =
                createSubOperation(singletonList(minusKeywords));
        subOperation.prepare();
        subOperation.setIndexesToApply(singletonSet(0));
        subOperation.apply();
        List<Long> minusKeywordPackIds = subOperation.getMinusKeywordPackIds();
        assertThat("???? ???????????????? ?????????????????? ???? null id", minusKeywordPackIds, contains(notNullValue()));

        MinusKeywordsPack minusKeywordsPacks =
                minusKeywordsPackRepository.getMinusKeywordsPacks(clientInfo.getShard(), clientInfo.getClientId(),
                        singletonList(minusKeywordPackIds.get(0))).get(minusKeywordPackIds.get(0));

        MinusKeywordsPack expected = new MinusKeywordsPack()
                .withMinusKeywords(minusKeywords)
                .withIsLibrary(false);
        assertThat("?????????? ?????????????????? ??????????????????", minusKeywordsPacks,
                beanDiffer(expected).useCompareStrategy(getPackCompareStrategy(expected)));
    }

    @Test
    public void minusKeywordsPacksAddedPartially() {
        List<String> minusKeywords1 = asList("????????????", "??????????");
        List<String> minusKeywords2 = asList("??????????", "????????????");
        AddMinusKeywordsPackSubOperation subOperation =
                createSubOperation(asList(minusKeywords1, minusKeywords2));
        subOperation.prepare();
        subOperation.setIndexesToApply(singletonSet(1));
        subOperation.apply();

        List<Long> minusKeywordPackIds = subOperation.getMinusKeywordPackIds();
        assertThat("?????? ?????????????????????????? ?????????????? id ???????????? ???????? null ???? ???????????????????????????????? ??????????????", minusKeywordPackIds,
                contains(nullValue(), notNullValue()));

        Long packId = minusKeywordPackIds.get(1);
        MinusKeywordsPack minusKeywordsPacks =
                minusKeywordsPackRepository.getMinusKeywordsPacks(clientInfo.getShard(), clientInfo.getClientId(),
                        singletonList(packId)).get(packId);

        MinusKeywordsPack expected = new MinusKeywordsPack()
                .withMinusKeywords(minusKeywords2)
                .withIsLibrary(false);
        assertThat("?????????????????? ???????????????????? ??????????", minusKeywordsPacks,
                beanDiffer(expected).useCompareStrategy(getPackCompareStrategy(expected)));
    }

    @Test
    public void severalPacks_Successful() {
        List<String> minusKeywords1 = asList("????????????", "??????????");
        List<String> minusKeywords2 = asList("??????????", "????????????");
        AddMinusKeywordsPackSubOperation subOperation =
                createSubOperation(asList(minusKeywords1, minusKeywords2));
        subOperation.prepare();
        subOperation.setIndexesToApply(ImmutableSet.of(0, 1));
        subOperation.apply();

        List<Long> minusKeywordPackIds = subOperation.getMinusKeywordPackIds();
        assertThat("???????????? ???????? 2 ???? null id", minusKeywordPackIds, contains(notNullValue(), notNullValue()));


        Collection<MinusKeywordsPack> actualPacks =
                minusKeywordsPackRepository.getMinusKeywordsPacks(clientInfo.getShard(), clientInfo.getClientId(),
                        minusKeywordPackIds).values();

        List<MinusKeywordsPack> expected = asList(new MinusKeywordsPack()
                        .withMinusKeywords(minusKeywords1)
                        .withIsLibrary(false),
                new MinusKeywordsPack()
                        .withMinusKeywords(minusKeywords2)
                        .withIsLibrary(false));
        assertThat("???????????????????? ???????????????????? ????????????", actualPacks, containsInAnyOrder(
                mapList(expected, pack -> beanDiffer(pack).useCompareStrategy(getPackCompareStrategy(pack)))));
    }

    @Test
    public void minusKeywordsWithValidationError_CorrectPaths() {
        AddMinusKeywordsPackSubOperation subOperation = createSubOperation(singletonList(null));
        ValidationResult<List<List<String>>, Defect> vr = subOperation.prepare();
        assertThat("?????????????????? ?????????????????????? ???????????? ??????????????????", vr,
                hasDefectDefinitionWith(validationError(path(index(0)), CommonDefects.notNull())));
    }

    @Test
    public void validationErrorOnSecondPack_CorrectPaths() {
        List<String> minusKeywords1 = asList("??????????", "????????????");
        List<String> minusKeywords2 = asList("????????????????????", "%$&", "??????????");
        AddMinusKeywordsPackSubOperation subOperation =
                createSubOperation(asList(minusKeywords1, minusKeywords2));
        ValidationResult<List<List<String>>, Defect> vr = subOperation.prepare();

        assertThat("?????????????????? ?????????????????????? ???????????? ??????????????????", vr,
                hasDefectDefinitionWith(
                        validationError(path(index(1)), illegalMinusKeywordChars(singletonList("%$&")))));
    }

    @Test
    public void prepareReturnsValidationErrorsWhenPartialAndHasErrorsOnSomeElement() {
        List<String> minusKeywords1 = asList("??????????", "????????????");
        List<String> minusKeywords2 = asList("????????????????????", "%$&", "??????????");
        AddMinusKeywordsPackSubOperation subOperation =
                factory.newInstance(Applicability.PARTIAL, asList(minusKeywords1, minusKeywords2),
                        MinusPhraseValidator.ValidationMode.ONE_ERROR_PER_TYPE,
                        clientInfo.getClientId(), clientInfo.getShard());
        ValidationResult<List<List<String>>, Defect> vr = subOperation.prepare();

        assertThat("?????????????????? ???????????? ??????????????????", vr,
                hasDefectDefinitionWith(
                        validationError(path(index(1)), illegalMinusKeywordChars(singletonList("%$&")))));
    }

    /**
     * ????????????????, ?????????????? ?????????? ???????????????????????? ?????? ?????????????????????? ?????????? ???????????????? apply ?? ???????????? {@link Applicability#PARTIAL}
     * ???????? ???????? ?????? ?????????? ?????????? ?????????????????? ??????????????????. ?? ???????? ???????????? ?? ???????????????????? ???????????????? ???? ???????????? ???????????????????? apply
     * (?????????? ?????????? ????????????????????).
     */
    @Test
    public void exceptionIsNotThrownOnApplyWhenIndexesToApplyIsEmpty() {
        List<String> minusKeywords = asList("????????????????????", "%$&", "??????????");
        AddMinusKeywordsPackSubOperation subOperation =
                factory.newInstance(Applicability.PARTIAL, singletonList(minusKeywords),
                        MinusPhraseValidator.ValidationMode.ONE_ERROR_PER_TYPE,
                        clientInfo.getClientId(), clientInfo.getShard());
        subOperation.prepare();
        subOperation.setIndexesToApply(emptySet());
        subOperation.apply();
    }

    @Test(expected = NullPointerException.class)
    public void exceptionThrownWhenIndexesToApplyWasNotSet() {
        List<String> minusKeywords = asList("??????", "??????");
        AddMinusKeywordsPackSubOperation subOperation = createSubOperation(singletonList(minusKeywords));
        subOperation.prepare();
        subOperation.apply();
    }

    private AddMinusKeywordsPackSubOperation createSubOperation(List<List<String>> minusKeywords) {
        return factory.newInstance(Applicability.FULL, minusKeywords,
                MinusPhraseValidator.ValidationMode.ONE_ERROR_PER_TYPE,
                clientInfo.getClientId(), clientInfo.getShard());
    }

    private CompareStrategy getPackCompareStrategy(MinusKeywordsPack minusKeywordsPack) {
        return DefaultCompareStrategies.allFields()
                .forFields(newPath(MinusKeywordsPack.ID.name())).useMatcher(notNullValue())
                .forFields(newPath(MinusKeywordsPack.HASH.name())).useMatcher(notNullValue())
                .forFields(newPath(MinusKeywordsPack.NAME.name())).useMatcher(nullValue())
                .forFields(newPath(MinusKeywordsPack.MINUS_KEYWORDS.name()))
                .useMatcher(containsInAnyOrder(minusKeywordsPack.getMinusKeywords().toArray()));
    }
}
