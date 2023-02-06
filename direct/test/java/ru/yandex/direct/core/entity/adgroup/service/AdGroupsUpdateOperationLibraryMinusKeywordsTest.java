package ru.yandex.direct.core.entity.adgroup.service;

import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.CompareStrategy;
import ru.yandex.direct.core.entity.StatusBsSynced;
import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.minuskeywordspack.repository.MinusKeywordsPackRepository;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.MinusKeywordsPackSteps;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.result.MassResult;
import ru.yandex.direct.validation.defect.CommonDefects;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath.newPath;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.allFieldsExcept;
import static ru.yandex.direct.core.entity.adgroup.model.AdGroup.LIBRARY_MINUS_KEYWORDS_IDS;
import static ru.yandex.direct.core.entity.keyword.service.validation.phrase.minusphrase.MinusPhraseConstraints.MAX_LINKED_PACKS_TO_ONE_AD_GROUP;
import static ru.yandex.direct.core.entity.keyword.service.validation.phrase.minusphrase.MinusPhraseDefects.minusWordsPackNotFound;
import static ru.yandex.direct.core.testing.data.TestGroups.activeTextAdGroup;
import static ru.yandex.direct.operation.Applicability.PARTIAL;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isFullySuccessful;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isSuccessful;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectWithDefinition;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.defect.CollectionDefects.duplicatedElement;
import static ru.yandex.direct.validation.defect.CollectionDefects.maxCollectionSize;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringRunner.class)
public class AdGroupsUpdateOperationLibraryMinusKeywordsTest extends AdGroupsUpdateOperationTestBase {

    private static final CompareStrategy WITHOUT_FIELDS = allFieldsExcept(newPath(AdGroup.LAST_CHANGE.name()));

    @Autowired
    private MinusKeywordsPackSteps packSteps;
    @Autowired
    private MinusKeywordsPackRepository packRepository;

    private List<Long> oldPackIds;
    private List<Long> newPackIds;
    private AdGroup defaultAdGroup;

    @Before
    public void before() {
        super.before();
        oldPackIds = packSteps.createLibraryMinusKeywordsPacks(clientInfo, 1);
        newPackIds = packSteps.createLibraryMinusKeywordsPacks(clientInfo, 1);
        defaultAdGroup = createAdGroupWithLinkedPacks(oldPackIds);
    }

    @Test
    public void prepareAndApply_AdGroupWithAlreadyLinkedPacks_LinkedPacksIsUpdated() {
        Long adGroupId = defaultAdGroup.getId();

        ModelChanges<AdGroup> modelChanges = getModelChanges(adGroupId, newPackIds);

        MassResult<Long> result = createUpdateOperation(PARTIAL, singletonList(modelChanges)).prepareAndApply();
        assertThat(result, isFullySuccessful());

        AdGroup actualAdGroup = adGroupRepository.getAdGroups(shard, singletonList(adGroupId)).get(0);
        AdGroup expectedAdGroup = toExpectedAdGroup(defaultAdGroup, newPackIds);

        assertThat(actualAdGroup,
                beanDiffer(expectedAdGroup)
                        .useCompareStrategy(WITHOUT_FIELDS));
    }

    @Test
    public void prepareAndApply_AdGroupWithMaxLinkedPacks_LinkedPacksIsUpdated() {
        List<Long> packIds = packSteps.createLibraryMinusKeywordsPacks(clientInfo, MAX_LINKED_PACKS_TO_ONE_AD_GROUP);

        Long adGroupId = defaultAdGroup.getId();

        ModelChanges<AdGroup> modelChanges = getModelChanges(adGroupId, packIds);

        MassResult<Long> result = createUpdateOperation(PARTIAL, singletonList(modelChanges)).prepareAndApply();
        assertThat(result, isFullySuccessful());

        AdGroup actualAdGroup = adGroupRepository.getAdGroups(shard, singletonList(adGroupId)).get(0);
        AdGroup expectedAdGroup = toExpectedAdGroup(defaultAdGroup, packIds);

        assertThat(actualAdGroup,
                beanDiffer(expectedAdGroup)
                        .useCompareStrategy(WITHOUT_FIELDS));
    }

    @Test
    public void prepareAndApply_TwoAdGroupsWithoutLinkedPacks_PacksLinked() {
        List<Long> adGroupIds = asList(adGroup1.getId(), adGroup2.getId());

        ModelChanges<AdGroup> mc1 = getModelChanges(adGroup1.getId(), newPackIds);
        ModelChanges<AdGroup> mc2 = getModelChanges(adGroup2.getId(), newPackIds);

        MassResult<Long> result = createUpdateOperation(PARTIAL, asList(mc1, mc2)).prepareAndApply();
        assertThat(result, isFullySuccessful());

        List<AdGroup> actualAdGroups = adGroupRepository.getAdGroups(shard, adGroupIds);
        AdGroup expectedAdGroup1 = toExpectedAdGroup(adGroup1, newPackIds);
        AdGroup expectedAdGroup2 = toExpectedAdGroup(adGroup2, newPackIds);

        assertThat(actualAdGroups.get(0), beanDiffer(expectedAdGroup1).useCompareStrategy(WITHOUT_FIELDS));
        assertThat(actualAdGroups.get(1), beanDiffer(expectedAdGroup2).useCompareStrategy(WITHOUT_FIELDS));
    }

    @Test
    public void prepareAndApply_TwoAdGroupsSetPackIdsIsNullAndIsEmpty_BothLibPackLinksIsDeleted() {
        AdGroup adGroup1 = createAdGroupWithLinkedPacks(oldPackIds);
        AdGroup adGroup2 = createAdGroupWithLinkedPacks(oldPackIds);

        List<Long> adGroupIds = asList(adGroup1.getId(), adGroup2.getId());

        ModelChanges<AdGroup> mc1 = getModelChanges(adGroup1.getId(), null);
        ModelChanges<AdGroup> mc2 = getModelChanges(adGroup2.getId(), emptyList());

        MassResult<Long> result = createUpdateOperation(PARTIAL, asList(mc1, mc2)).prepareAndApply();
        assertThat(result, isFullySuccessful());

        List<AdGroup> actualAdGroups = adGroupRepository.getAdGroups(shard, adGroupIds);
        AdGroup expectedAdGroup1 = toExpectedAdGroup(adGroup1, emptyList());
        AdGroup expectedAdGroup2 = toExpectedAdGroup(adGroup2, emptyList());

        assertThat(actualAdGroups.get(0), beanDiffer(expectedAdGroup1).useCompareStrategy(WITHOUT_FIELDS));
        assertThat(actualAdGroups.get(1), beanDiffer(expectedAdGroup2).useCompareStrategy(WITHOUT_FIELDS));
    }

    @Test
    public void prepareAndApply_PackIdIsNull_NotNullError() {
        Long adGroupId = defaultAdGroup.getId();

        ModelChanges<AdGroup> modelChanges = getModelChanges(adGroupId, singletonList(null));

        MassResult<Long> result = createUpdateOperation(PARTIAL, singletonList(modelChanges)).prepareAndApply();
        assertThat(result, isSuccessful(false));

        ValidationResult<?, Defect> actual = result.getValidationResult();

        Assertions.assertThat(actual).is(matchedBy(hasDefectWithDefinition(
                validationError(path(index(0), field(AdGroup.LIBRARY_MINUS_KEYWORDS_IDS), index(0)),
                        CommonDefects.notNull()))));
    }

    @Test
    public void prepareAndApply_PackIdsNotUnique_DuplicatedElementError() {
        Long adGroupId = defaultAdGroup.getId();
        Long newPackId = packSteps.createLibraryMinusKeywordsPack(clientInfo).getMinusKeywordPackId();

        ModelChanges<AdGroup> modelChanges = getModelChanges(adGroupId, asList(newPackId, newPackId));

        MassResult<Long> result = createUpdateOperation(PARTIAL, singletonList(modelChanges)).prepareAndApply();
        assertThat(result, isSuccessful(false));

        ValidationResult<?, Defect> actual = result.getValidationResult();

        Assertions.assertThat(actual).is(matchedBy(hasDefectWithDefinition(
                validationError(path(index(0), field(AdGroup.LIBRARY_MINUS_KEYWORDS_IDS), index(0)),
                        duplicatedElement()))));
    }

    @Test
    public void prepareAndApply_PackNotExist_MinusWordsPackNotFoundError() {
        Long adGroupId = defaultAdGroup.getId();

        Long packId = packSteps.createLibraryMinusKeywordsPack(clientInfo).getMinusKeywordPackId();
        Long packIdForDelete = packSteps.createLibraryMinusKeywordsPack(clientInfo).getMinusKeywordPackId();
        packRepository.delete(shard, clientId, singletonList(packIdForDelete));

        ModelChanges<AdGroup> modelChanges = getModelChanges(adGroupId, asList(packId, packIdForDelete));

        MassResult<Long> result = createUpdateOperation(PARTIAL, singletonList(modelChanges)).prepareAndApply();
        assertThat(result, isSuccessful(false));

        ValidationResult<?, Defect> actual = result.getValidationResult();

        Assertions.assertThat(actual).is(matchedBy(hasDefectWithDefinition(
                validationError(path(index(0), field(AdGroup.LIBRARY_MINUS_KEYWORDS_IDS), index(1)),
                        minusWordsPackNotFound()))));
    }

    @Test
    public void prepareAndApply_LinkPrivatePack_MinusWordsPackNotFoundError() {
        Long adGroupId = defaultAdGroup.getId();
        Long privatePackId = packSteps.createPrivateMinusKeywordsPack(clientInfo).getMinusKeywordPackId();

        ModelChanges<AdGroup> modelChanges = getModelChanges(adGroupId, singletonList(privatePackId));

        MassResult<Long> result = createUpdateOperation(PARTIAL, singletonList(modelChanges)).prepareAndApply();
        assertThat(result, isSuccessful(false));

        ValidationResult<?, Defect> actual = result.getValidationResult();

        Assertions.assertThat(actual).is(matchedBy(hasDefectWithDefinition(
                validationError(path(index(0), field(AdGroup.LIBRARY_MINUS_KEYWORDS_IDS), index(0)),
                        minusWordsPackNotFound()))));
    }

    @Test
    public void prepareAndApply_SetAnotherClientPack_MinusWordsPackNotFoundError() {
        Long adGroupId = defaultAdGroup.getId();

        ClientInfo anotherClientInfo = steps.clientSteps().createDefaultClient();
        Long anotherClientPack = packSteps.createLibraryMinusKeywordsPacks(anotherClientInfo, 1).get(0);

        ModelChanges<AdGroup> modelChanges = getModelChanges(adGroupId, singletonList(anotherClientPack));

        MassResult<Long> result = createUpdateOperation(PARTIAL, singletonList(modelChanges)).prepareAndApply();
        assertThat(result, isSuccessful(false));

        ValidationResult<?, Defect> actual = result.getValidationResult();

        Assertions.assertThat(actual).is(matchedBy(hasDefectWithDefinition(
                validationError(path(index(0), field(AdGroup.LIBRARY_MINUS_KEYWORDS_IDS), index(0)),
                        minusWordsPackNotFound()))));
    }

    @Test
    public void prepareAndApply_SetMoreThanMaxPacksCount_MaxCollectionSizeError() {
        Long adGroupId = defaultAdGroup.getId();

        List<Long> packs = packSteps.createLibraryMinusKeywordsPacks(clientInfo, MAX_LINKED_PACKS_TO_ONE_AD_GROUP + 1);
        ModelChanges<AdGroup> modelChanges = getModelChanges(adGroupId, packs);

        MassResult<Long> result = createUpdateOperation(PARTIAL, singletonList(modelChanges)).prepareAndApply();
        assertThat(result, isSuccessful(false));

        ValidationResult<?, Defect> actual = result.getValidationResult();

        Assertions.assertThat(actual).is(matchedBy(hasDefectWithDefinition(
                validationError(path(index(0), field(AdGroup.LIBRARY_MINUS_KEYWORDS_IDS)),
                        maxCollectionSize(MAX_LINKED_PACKS_TO_ONE_AD_GROUP)))));
    }

    private AdGroup toExpectedAdGroup(AdGroup adGroup, List<Long> packIds) {
        return adGroup
                .withStatusBsSynced(StatusBsSynced.NO)
                .withLibraryMinusKeywordsIds(packIds);
    }

    private AdGroup createAdGroupWithLinkedPacks(List<Long> packIds) {
        return adGroupSteps.createAdGroup(activeTextAdGroup().withLibraryMinusKeywordsIds(packIds), clientInfo)
                .getAdGroup();
    }

    private ModelChanges<AdGroup> getModelChanges(Long id, List<Long> packIds) {
        return new ModelChanges<>(id, AdGroup.class)
                .process(packIds, LIBRARY_MINUS_KEYWORDS_IDS);
    }

}
