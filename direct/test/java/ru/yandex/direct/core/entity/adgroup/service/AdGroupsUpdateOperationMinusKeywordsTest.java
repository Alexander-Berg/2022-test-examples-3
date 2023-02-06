package ru.yandex.direct.core.entity.adgroup.service;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.adgroup.model.TextAdGroup;
import ru.yandex.direct.core.entity.minuskeywordspack.model.MinusKeywordsPack;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.MinusKeywordsPackInfo;
import ru.yandex.direct.core.testing.repository.TestMinusKeywordsPackRepository;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.operation.Applicability;
import ru.yandex.direct.result.MassResult;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath.newPath;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyFields;
import static ru.yandex.direct.core.entity.keyword.service.validation.phrase.minusphrase.MinusPhraseConstraints.GROUP_MINUS_KEYWORDS_MAX_LENGTH;
import static ru.yandex.direct.core.entity.keyword.service.validation.phrase.minusphrase.MinusPhraseConstraints.WORDS_MAX_COUNT;
import static ru.yandex.direct.core.entity.keyword.service.validation.phrase.minusphrase.MinusPhraseDefects.maxCountWordsInKeyword;
import static ru.yandex.direct.core.entity.keyword.service.validation.phrase.minusphrase.MinusPhraseDefects.maxLengthMinusKeywords;
import static ru.yandex.direct.core.testing.data.TestGroups.activeTextAdGroup;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isFullySuccessful;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isSuccessful;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectWithDefinition;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringRunner.class)
public class AdGroupsUpdateOperationMinusKeywordsTest extends AdGroupsUpdateOperationTestBase {

    @Autowired
    private TestMinusKeywordsPackRepository testMinusKeywordsPackRepository;

    @Test(expected = IllegalArgumentException.class)
    public void prepareAndApply_MinusKeywordsIdChanged() {
        AdGroupInfo adGroup = adGroupSteps.createAdGroup(activeTextAdGroup(), clientInfo);
        ModelChanges<AdGroup> modelChanges = new ModelChanges<>(adGroup.getAdGroupId(), AdGroup.class)
                .process(1L, AdGroup.MINUS_KEYWORDS_ID);

        createUpdateOperation(Applicability.FULL, singletonList(modelChanges));
    }

    @Test
    public void prepareAndApply_MinusKeywordsAdded() {
        AdGroupInfo adGroupWithoutMinusKeywords = adGroupSteps
                .createAdGroup(activeTextAdGroup().withMinusKeywords(emptyList()), clientInfo);
        ModelChanges<AdGroup> modelChanges =
                modelChangesWithValidMinusKeywords(adGroupWithoutMinusKeywords.getAdGroupId());
        AdGroupsUpdateOperation updateOperation =
                createUpdateOperation(Applicability.FULL, singletonList(modelChanges));
        MassResult<Long> result = updateOperation.prepareAndApply();
        assertThat(result, isFullySuccessful());

        AdGroup actualAdGroup =
                adGroupRepository.getAdGroups(shard, singletonList(adGroupWithoutMinusKeywords.getAdGroupId())).get(0);
        AdGroup expected = new TextAdGroup()
                .withMinusKeywords(modelChanges.getChangedProp(AdGroup.MINUS_KEYWORDS));

        assertThat("минус фразы добавлены успешно", actualAdGroup,
                beanDiffer(expected).useCompareStrategy(
                        onlyFields(newPath(AdGroup.MINUS_KEYWORDS_ID.name()), newPath(AdGroup.MINUS_KEYWORDS.name()))
                                .forFields(newPath(AdGroup.MINUS_KEYWORDS_ID.name())).useMatcher(notNullValue())));
    }

    @Test
    public void prepareAndApply_MinusKeywordsChanged_MinusKeywordsIdChanged() {
        AdGroupInfo adGroupWithMinusKeywords = adGroupSteps
                .createAdGroup(activeTextAdGroup().withMinusKeywords(singletonList("раз два три")), clientInfo);
        ModelChanges<AdGroup> modelChanges =
                modelChangesWithValidMinusKeywords(adGroupWithMinusKeywords.getAdGroupId());
        AdGroupsUpdateOperation updateOperation =
                createUpdateOperation(Applicability.FULL, singletonList(modelChanges));
        MassResult<Long> result = updateOperation.prepareAndApply();
        assertThat(result, isFullySuccessful());

        AdGroup actualAdGroup =
                adGroupRepository.getAdGroups(shard, singletonList(adGroupWithMinusKeywords.getAdGroupId())).get(0);
        AdGroup expected = new TextAdGroup()
                .withMinusKeywords(modelChanges.getChangedProp(AdGroup.MINUS_KEYWORDS));

        assertThat("минус фразы изменены успешно", actualAdGroup,
                beanDiffer(expected).useCompareStrategy(
                        onlyFields(newPath(AdGroup.MINUS_KEYWORDS_ID.name()), newPath(AdGroup.MINUS_KEYWORDS.name()))
                                .forFields(newPath(AdGroup.MINUS_KEYWORDS_ID.name())).useMatcher(allOf(notNullValue(),
                                not(adGroupWithMinusKeywords.getAdGroup().getMinusKeywordsId())))));
    }

    @Test
    public void prepareAndApply_MinusKeywordsSetToNull_MinusKeywordsDeleted() {
        AdGroupInfo adGroupWithMinusKeywords = adGroupSteps
                .createAdGroup(activeTextAdGroup().withMinusKeywords(singletonList("раз два три")), clientInfo);
        ModelChanges<AdGroup> modelChanges = new ModelChanges<>(adGroupWithMinusKeywords.getAdGroupId(), AdGroup.class)
                .process(null, AdGroup.MINUS_KEYWORDS);
        AdGroupsUpdateOperation updateOperation =
                createUpdateOperation(Applicability.FULL, singletonList(modelChanges));
        MassResult<Long> result = updateOperation.prepareAndApply();
        assertThat(result, isFullySuccessful());

        AdGroup actualAdGroup =
                adGroupRepository.getAdGroups(shard, singletonList(adGroupWithMinusKeywords.getAdGroupId())).get(0);
        AdGroup expected = new TextAdGroup()
                .withMinusKeywordsId(null)
                .withMinusKeywords(emptyList());

        assertThat("минус фразы удалены успешно", actualAdGroup,
                beanDiffer(expected).useCompareStrategy(
                        onlyFields(newPath(AdGroup.MINUS_KEYWORDS_ID.name()), newPath(AdGroup.MINUS_KEYWORDS.name()))));
    }

    @Test
    public void prepareAndApply_MinusKeywordsSetToEmptyList_MinusKeywordsDeleted() {
        AdGroupInfo adGroupWithMinusKeywords = adGroupSteps
                .createAdGroup(activeTextAdGroup().withMinusKeywords(singletonList("раз два три")), clientInfo);
        ModelChanges<AdGroup> modelChanges = new ModelChanges<>(adGroupWithMinusKeywords.getAdGroupId(), AdGroup.class)
                .process(emptyList(), AdGroup.MINUS_KEYWORDS);
        AdGroupsUpdateOperation updateOperation =
                createUpdateOperation(Applicability.FULL, singletonList(modelChanges));
        MassResult<Long> result = updateOperation.prepareAndApply();
        assertThat(result, isFullySuccessful());

        AdGroup actualAdGroup =
                adGroupRepository.getAdGroups(shard, singletonList(adGroupWithMinusKeywords.getAdGroupId())).get(0);
        AdGroup expected = new TextAdGroup()
                .withMinusKeywordsId(null)
                .withMinusKeywords(emptyList());

        assertThat("минус фразы удалены успешно", actualAdGroup,
                beanDiffer(expected).useCompareStrategy(
                        onlyFields(newPath(AdGroup.MINUS_KEYWORDS_ID.name()), newPath(AdGroup.MINUS_KEYWORDS.name()))));
    }

    @Test
    public void prepareAndApply_SeveralAdGroupsWithMinusKeywordsAddedChangedDeleted() {
        AdGroupInfo adGroupWithoutMinusKeywords = adGroupSteps
                .createAdGroup(activeTextAdGroup().withMinusKeywords(emptyList()), clientInfo);
        AdGroupInfo adGroupWithMinusKeywordsForChange = adGroupSteps
                .createAdGroup(activeTextAdGroup().withMinusKeywords(singletonList("раз два три")), clientInfo);
        AdGroupInfo adGroupWithMinusKeywordsForDelete = adGroupSteps
                .createAdGroup(activeTextAdGroup().withMinusKeywords(singletonList("минус фраза")), clientInfo);

        List<ModelChanges<AdGroup>> modelChangesList = asList(
                modelChangesWithValidMinusKeywords(adGroupWithoutMinusKeywords.getAdGroupId()),
                modelChangesWithValidMinusKeywords(adGroupWithMinusKeywordsForChange.getAdGroupId()),
                new ModelChanges<>(adGroupWithMinusKeywordsForDelete.getAdGroupId(), AdGroup.class)
                        .process(null, AdGroup.MINUS_KEYWORDS)
        );
        AdGroupsUpdateOperation updateOperation =
                createUpdateOperation(Applicability.FULL, modelChangesList);
        MassResult<Long> result = updateOperation.prepareAndApply();
        assertThat(result, isFullySuccessful());
    }

    @Test
    public void prepareAndApply_MinusKeywordsNotAdded_MinusKeywordsIdRemainsNull() {
        AdGroupInfo adGroupWithoutMinusKeywords = adGroupSteps
                .createAdGroup(activeTextAdGroup().withMinusKeywords(emptyList()), clientInfo);

        ModelChanges<AdGroup> modelChanges = modelChangesWithValidName(adGroupWithoutMinusKeywords.getAdGroup());
        AdGroupsUpdateOperation updateOperation =
                createUpdateOperation(Applicability.FULL, singletonList(modelChanges));
        MassResult<Long> result = updateOperation.prepareAndApply();
        assertThat(result, isFullySuccessful());

        AdGroup actualAdGroup =
                adGroupRepository.getAdGroups(shard, singletonList(adGroupWithoutMinusKeywords.getAdGroupId())).get(0);
        assertThat(actualAdGroup.getMinusKeywordsId(), nullValue());
    }

    @Test
    public void prepareAndApply_OneAdGroupWithValidAndOneWithInvalidMinusKeywords_ValidAddedSuccessfully() {
        ModelChanges<AdGroup> adGroupModelChanges = modelChangesWithValidMinusKeywords(adGroup2.getId());
        List<ModelChanges<AdGroup>> modelChangesList = asList(
                modelChangesWithInvalidMinusKeywords(adGroup1),
                adGroupModelChanges
        );
        AdGroupsUpdateOperation updateOperation =
                createUpdateOperation(Applicability.PARTIAL, modelChangesList);
        MassResult<Long> result = updateOperation.prepareAndApply();
        assertThat(result, isSuccessful(false, true));

        List<AdGroup> adGroups = adGroupRepository.getAdGroups(shard, asList(adGroup1.getId(), adGroup2.getId()));
        assertThat("невалидные фразы не добавились", adGroups.get(0).getMinusKeywordsId(), nullValue());
        assertThat("валидные фразы добавились", adGroups.get(1).getMinusKeywords(),
                contains(adGroupModelChanges.getChangedProp(AdGroup.MINUS_KEYWORDS).toArray()));
    }

    @Test
    public void prepareAndApply_DuplicatedMinusKeywordsAdded_DeduplicationWorks() {
        MinusKeywordsPackInfo minusKeywordsPack =
                steps.minusKeywordsPackSteps().createPrivateMinusKeywordsPack(clientInfo);
        ModelChanges<AdGroup> modelChanges = new ModelChanges<>(adGroup1.getId(), AdGroup.class)
                .process(minusKeywordsPack.getMinusKeywordsPack().getMinusKeywords(), AdGroup.MINUS_KEYWORDS);
        AdGroupsUpdateOperation updateOperation =
                createUpdateOperation(Applicability.FULL, singletonList(modelChanges));
        MassResult<Long> result = updateOperation.prepareAndApply();
        assertThat(result, isFullySuccessful());

        AdGroup adGroup = adGroupRepository.getAdGroups(shard, singletonList(adGroup1.getId())).get(0);
        AdGroup expected = new TextAdGroup()
                .withMinusKeywordsId(minusKeywordsPack.getMinusKeywordPackId())
                .withMinusKeywords(minusKeywordsPack.getMinusKeywordsPack().getMinusKeywords());
        assertThat("привязались существующие минус фразы", adGroup,
                beanDiffer(expected).useCompareStrategy(
                        onlyFields(newPath(AdGroup.MINUS_KEYWORDS_ID.name()), newPath(AdGroup.MINUS_KEYWORDS.name()))));
    }

    @Test
    public void prepareAndApply_ValidMinusKeywordsButInvalidAdGroup_PackNotAdded() {
        List<MinusKeywordsPack> clientPacksBefore = testMinusKeywordsPackRepository.getClientPacks(shard, clientId);
        assumeThat("у клиента не должно быть наборов, чтобы потом проверить, что их не появилось", clientPacksBefore,
                hasSize(0));

        ModelChanges<AdGroup> modelChanges = new ModelChanges<>(adGroup1.getId(), AdGroup.class)
                .process(null, AdGroup.NAME)
                .process(singletonList("хорошая минус фраза"), AdGroup.MINUS_KEYWORDS);

        AdGroupsUpdateOperation updateOperation =
                createUpdateOperation(Applicability.FULL, singletonList(modelChanges));
        MassResult<Long> result = updateOperation.prepareAndApply();
        assertThat(result, isSuccessful(false));

        List<MinusKeywordsPack> clientPacksAfter = testMinusKeywordsPackRepository.getClientPacks(shard, clientId);
        assertThat("наборы не добавились", clientPacksAfter, hasSize(0));
    }

    /**
     * Проверяем, что валидация, удаленная из {@link ru.yandex.direct.core.entity.adgroup.service.validation.AdGroupValidationService},
     * работает через операцию добавления наборов
     */
    @Test
    public void prepareAndApply_TooLongMinusKeywordsAdded_ValidationError() {
        ModelChanges<AdGroup> modelChanges = new ModelChanges<>(adGroup1.getId(), AdGroup.class)
                .process(createTooLongKeywords(), AdGroup.MINUS_KEYWORDS);
        AdGroupsUpdateOperation updateOperation =
                createUpdateOperation(Applicability.FULL, singletonList(modelChanges));
        MassResult<Long> result = updateOperation.prepareAndApply();
        assertThat(result.getValidationResult(), hasDefectWithDefinition(
                validationError(path(index(0), field(AdGroup.MINUS_KEYWORDS.name())),
                        maxLengthMinusKeywords(GROUP_MINUS_KEYWORDS_MAX_LENGTH))));
    }

    @Test
    public void prepareAndApply_MaxWordsInMinusKeywordsAdded_SingleValidationError() {
        List<String> invalidMinusKeywords = singletonList("раз два три чет пять ше семь овер");
        ModelChanges<AdGroup> modelChanges = new ModelChanges<>(adGroup1.getId(), AdGroup.class)
                .process(invalidMinusKeywords, AdGroup.MINUS_KEYWORDS);
        AdGroupsUpdateOperation updateOperation =
                createUpdateOperation(Applicability.FULL, singletonList(modelChanges));
        MassResult<Long> result = updateOperation.prepareAndApply();
        assertThat("должна быть одна ошибка валидации", result.getValidationResult().flattenErrors(), hasSize(1));
        assertThat(result.getValidationResult(), hasDefectWithDefinition(
                validationError(path(index(0), field(AdGroup.MINUS_KEYWORDS.name())),
                        maxCountWordsInKeyword(WORDS_MAX_COUNT, invalidMinusKeywords))));
    }

    private List<String> createTooLongKeywords() {
        // суммарная длина всех минус-слов = 10 * 5 * 82 = 4100
        List<String> minusKeywords = new ArrayList<>();
        for (int i = 0; i < 82; i++) {
            minusKeywords.add(StringUtils.repeat(randomAlphanumeric(10), " ", 5));
        }
        return minusKeywords;
    }

}
