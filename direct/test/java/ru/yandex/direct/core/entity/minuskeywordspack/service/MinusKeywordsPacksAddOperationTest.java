package ru.yandex.direct.core.entity.minuskeywordspack.service;

import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.CompareStrategy;
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies;
import ru.yandex.direct.core.entity.keyword.service.validation.phrase.minusphrase.MinusPhraseValidator.ValidationMode;
import ru.yandex.direct.core.entity.minuskeywordspack.container.AddedMinusKeywordsPackInfo;
import ru.yandex.direct.core.entity.minuskeywordspack.model.MinusKeywordsPack;
import ru.yandex.direct.core.entity.minuskeywordspack.repository.MinusKeywordsPackRepository;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.operation.Applicability;
import ru.yandex.direct.result.MassResult;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.apache.commons.lang3.RandomStringUtils.randomNumeric;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.notNullValue;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath.newPath;
import static ru.yandex.direct.core.entity.keyword.service.validation.phrase.minusphrase.MinusPhraseDefects.illegalMinusKeywordChars;
import static ru.yandex.direct.core.testing.data.TestMinusKeywordsPacks.libraryMinusKeywordsPack;
import static ru.yandex.direct.core.testing.data.TestMinusKeywordsPacks.privateMinusKeywordsPack;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isFullySuccessful;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isSuccessful;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectWithDefinition;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class MinusKeywordsPacksAddOperationTest {

    private static final String INVALID_MINUS_KEYWORD = "синтаксически невалидная %$&";
    private static final CompareStrategy COMPARE_STRATEGY = DefaultCompareStrategies.allFields()
            .forFields(newPath(MinusKeywordsPack.HASH.name())).useMatcher(notNullValue());

    @Autowired
    private MinusKeywordsPacksAddOperationFactory minusKeywordsPacksAddOperationFactory;
    @Autowired
    private MinusKeywordsPackRepository packRepository;
    @Autowired
    private Steps steps;

    private ClientId clientId;
    private int shard;

    @Before
    public void before() {
        ClientInfo clientInfo = steps.clientSteps().createDefaultClient();
        clientId = clientInfo.getClientId();
        shard = clientInfo.getShard();
    }

    @Test
    public void prepareAndApply_DuplicateMinusKeywordsLibraryPacksMode_Create() {
        MinusKeywordsPack defaultPack = libraryMinusKeywordsPack();
        List<MinusKeywordsPack> models = singletonList(defaultPack);

        MassResult<AddedMinusKeywordsPackInfo> firstResult = createAddOperation(models).prepareAndApply();
        assertThat(firstResult).is(matchedBy(isFullySuccessful()));
        Long firstGeneratedId = defaultPack.getId();

        MassResult<AddedMinusKeywordsPackInfo> secondResult = createAddOperation(models).prepareAndApply();
        assertThat(secondResult).is(matchedBy(isFullySuccessful()));
        Long secondGeneratedId = defaultPack.getId();

        assertThat(secondGeneratedId).isNotEqualTo(firstGeneratedId);

    }

    @Test
    public void prepareAndApply_DuplicateMinusKeywordsPrivatePacksMode_Deduplicate() {
        MinusKeywordsPack defaultPack = privateMinusKeywordsPack();

        String minusWord = "минус-фраза " + randomNumeric(5);
        defaultPack.setMinusKeywords(singletonList(minusWord));
        List<MinusKeywordsPack> models = singletonList(defaultPack);

        MassResult<AddedMinusKeywordsPackInfo> firstResult =
                createAddOperationWithPrivatePacks(models).prepareAndApply();
        assertThat(firstResult).is(matchedBy(isFullySuccessful()));
        Long firstGeneratedId = defaultPack.getId();

        MassResult<AddedMinusKeywordsPackInfo> secondResult =
                createAddOperationWithPrivatePacks(models).prepareAndApply();
        assertThat(secondResult).is(matchedBy(isFullySuccessful()));
        Long secondGeneratedId = defaultPack.getId();

        assertThat(secondGeneratedId).isEqualTo(firstGeneratedId);
    }

    @Test
    public void execute_MultiplePacks_SavedCorrectly() {
        MinusKeywordsPack pack1 = privateMinusKeywordsPack().withMinusKeywords(singletonList("pack1"));
        MinusKeywordsPack pack2 = privateMinusKeywordsPack().withMinusKeywords(singletonList("pack2"));

        MassResult<AddedMinusKeywordsPackInfo> result =
                createAddOperationWithPrivatePacks(asList(pack1, pack2)).prepareAndApply();
        assumeThat(result, isSuccessful());

        assertPackCorrectlyCreated(pack1);
        assertPackCorrectlyCreated(pack2);
    }

    @Test
    public void prepareAndApply_InvalidMinusWords_NotCreated() {
        MinusKeywordsPack pack = privateMinusKeywordsPack().withMinusKeywords(singletonList(INVALID_MINUS_KEYWORD));
        List<MinusKeywordsPack> models = singletonList(pack);

        MassResult<AddedMinusKeywordsPackInfo> result = createAddOperation(models).prepareAndApply();

        ValidationResult<?, Defect> actualValidationResult = result.getValidationResult();
        assertThat(actualValidationResult).is(matchedBy(hasDefectWithDefinition(
                validationError(path(index(0), field("minusKeywords")),
                        illegalMinusKeywordChars(singletonList(INVALID_MINUS_KEYWORD))))));

    }

    @Test
    public void prepareAndApply_PrivatePacksTrue_IsLibraryFlagSet() {
        MinusKeywordsPack defaultPack = privateMinusKeywordsPack()
                .withIsLibrary(null);
        MassResult<AddedMinusKeywordsPackInfo> result =
                createAddOperationWithPrivatePacks(singletonList(defaultPack)).prepareAndApply();
        assertThat(result).is(matchedBy(isFullySuccessful()));
        assertThat(defaultPack.getIsLibrary()).isFalse();
    }

    @Test
    public void prepareAndApply_PrivatePacksFalse_IsLibraryFlagSet() {
        MinusKeywordsPack defaultPack = libraryMinusKeywordsPack()
                .withIsLibrary(null);
        MassResult<AddedMinusKeywordsPackInfo> result =
                createAddOperation(singletonList(defaultPack)).prepareAndApply();
        assertThat(result).is(matchedBy(isFullySuccessful()));
        assertThat(defaultPack.getIsLibrary()).isTrue();
    }

    private MinusKeywordsPacksAddOperation createAddOperation(List<MinusKeywordsPack> models) {

        return minusKeywordsPacksAddOperationFactory
                .newInstance(Applicability.FULL, models, ValidationMode.ONE_ERROR_PER_TYPE, clientId, shard, false);
    }

    private MinusKeywordsPacksAddOperation createAddOperationWithPrivatePacks(List<MinusKeywordsPack> models) {

        return minusKeywordsPacksAddOperationFactory
                .newInstance(Applicability.FULL, models, ValidationMode.ONE_ERROR_PER_TYPE, clientId, shard, true);

    }

    private void assertPackCorrectlyCreated(MinusKeywordsPack expected) {
        Long id = expected.getId();

        MinusKeywordsPack actual = packRepository.getMinusKeywordsPacks(shard, clientId, singletonList(id)).get(id);
        Assert.assertThat("сохраненный список минус слов отличается от ожидаемого",
                actual, beanDiffer(expected).useCompareStrategy(COMPARE_STRATEGY));
    }

}
