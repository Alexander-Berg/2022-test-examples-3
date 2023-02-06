package ru.yandex.direct.core.entity.adgroup.service;

import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.MinusKeywordsPackSteps;
import ru.yandex.direct.result.MassResult;
import ru.yandex.direct.validation.defect.CommonDefects;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.keyword.service.validation.phrase.minusphrase.MinusPhraseConstraints.MAX_LINKED_PACKS_TO_ONE_AD_GROUP;
import static ru.yandex.direct.core.entity.keyword.service.validation.phrase.minusphrase.MinusPhraseDefects.minusWordsPackNotFound;
import static ru.yandex.direct.core.testing.data.TestGroups.clientTextAdGroup;
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
@RunWith(SpringJUnit4ClassRunner.class)
public class AdGroupsAddOperationLibraryMinusKeywordsTest extends AdGroupsAddOperationTestBase {

    @Autowired
    private MinusKeywordsPackSteps packSteps;

    private AdGroup defaultAdGroup;

    @Before
    public void before() {
        super.before();
        defaultAdGroup = clientTextAdGroup(campaignInfo.getCampaignId());
    }

    @Test
    public void prepareAndApply_MinusKeywordsIdsSpecified() {
        List<Long> packs = packSteps.createLibraryMinusKeywordsPacks(clientInfo, 1);
        defaultAdGroup.withLibraryMinusKeywordsIds(packs);

        MassResult<Long> result = createFullAddOperation(singletonList(defaultAdGroup)).prepareAndApply();
        assertThat(result, isFullySuccessful());
        AdGroup actualAdGroup = actualAdGroup(result.get(0).getResult());

        assertThat(actualAdGroup.getLibraryMinusKeywordsIds(), containsInAnyOrder(packs.toArray()));
    }

    @Test
    public void prepareAndApply_MaxMinusKeywordsPacksCount_Successful() {
        List<Long> maxPacks = packSteps.createLibraryMinusKeywordsPacks(clientInfo, MAX_LINKED_PACKS_TO_ONE_AD_GROUP);
        defaultAdGroup.withLibraryMinusKeywordsIds(maxPacks);

        MassResult<Long> result = createFullAddOperation(singletonList(defaultAdGroup)).prepareAndApply();
        assertThat(result, isFullySuccessful());
        AdGroup actualAdGroup = actualAdGroup(result.get(0).getResult());

        assertThat(actualAdGroup.getLibraryMinusKeywordsIds(), containsInAnyOrder(maxPacks.toArray()));
    }

    @Test
    public void prepareAndApply_MinusKeywordsIdsNull() {
        defaultAdGroup.withLibraryMinusKeywordsIds(null);

        MassResult<Long> result = createFullAddOperation(singletonList(defaultAdGroup)).prepareAndApply();
        assertThat(result, isFullySuccessful());
        AdGroup actualAdGroup = actualAdGroup(result.get(0).getResult());

        assertThat(actualAdGroup.getLibraryMinusKeywordsIds(), empty());
    }

    @Test
    public void prepareAndApply_MinusKeywordsIdsEmpty() {
        defaultAdGroup.withLibraryMinusKeywordsIds(emptyList());

        MassResult<Long> result = createFullAddOperation(singletonList(defaultAdGroup)).prepareAndApply();
        assertThat(result, isFullySuccessful());
        AdGroup actualAdGroup = actualAdGroup(result.get(0).getResult());

        assertThat(actualAdGroup.getLibraryMinusKeywordsIds(), empty());
    }

    @Test
    public void prepareAndApply_PackIdIsNull_NotNullError() {
        defaultAdGroup.withLibraryMinusKeywordsIds(singletonList(null));

        MassResult<Long> result = createFullAddOperation(singletonList(defaultAdGroup)).prepareAndApply();
        assertThat(result, isSuccessful(false));
        ValidationResult<?, Defect> actual = result.getValidationResult();

        Assertions.assertThat(actual).is(matchedBy(hasDefectWithDefinition(
                validationError(path(index(0), field(AdGroup.LIBRARY_MINUS_KEYWORDS_IDS), index(0)),
                        CommonDefects.notNull()))));
    }

    @Test
    public void prepareAndApply_PackIdsNotUnique_DuplicatedElementError() {
        Long newPackId = packSteps.createLibraryMinusKeywordsPack(clientInfo).getMinusKeywordPackId();
        defaultAdGroup.withLibraryMinusKeywordsIds(asList(newPackId, newPackId));

        MassResult<Long> result = createFullAddOperation(singletonList(defaultAdGroup)).prepareAndApply();
        assertThat(result, isSuccessful(false));
        ValidationResult<?, Defect> actual = result.getValidationResult();

        Assertions.assertThat(actual).is(matchedBy(hasDefectWithDefinition(
                validationError(path(index(0), field(AdGroup.LIBRARY_MINUS_KEYWORDS_IDS), index(0)),
                        duplicatedElement()))));
    }

    @Test
    public void prepareAndApply_PackNotExist_MinusWordsPackNotFoundError() {
        Long packId = packSteps.createLibraryMinusKeywordsPack(clientInfo).getMinusKeywordPackId();
        Long packIdForDelete = packSteps.createLibraryMinusKeywordsPack(clientInfo).getMinusKeywordPackId();
        minusKeywordsPackRepository.delete(shard, clientId, singletonList(packIdForDelete));

        defaultAdGroup.withLibraryMinusKeywordsIds(asList(packId, packIdForDelete));

        MassResult<Long> result = createFullAddOperation(singletonList(defaultAdGroup)).prepareAndApply();
        assertThat(result, isSuccessful(false));
        ValidationResult<?, Defect> actual = result.getValidationResult();

        Assertions.assertThat(actual).is(matchedBy(hasDefectWithDefinition(
                validationError(path(index(0), field(AdGroup.LIBRARY_MINUS_KEYWORDS_IDS), index(1)),
                        minusWordsPackNotFound()))));
    }

    @Test
    public void prepareAndApply_LinkPrivatePack_MinusWordsPackNotFounddError() {
        Long privatePackId = packSteps.createPrivateMinusKeywordsPack(clientInfo).getMinusKeywordPackId();

        defaultAdGroup.withLibraryMinusKeywordsIds(singletonList(privatePackId));

        MassResult<Long> result = createFullAddOperation(singletonList(defaultAdGroup)).prepareAndApply();
        assertThat(result, isSuccessful(false));
        ValidationResult<?, Defect> actual = result.getValidationResult();

        Assertions.assertThat(actual).is(matchedBy(hasDefectWithDefinition(
                validationError(path(index(0), field(AdGroup.LIBRARY_MINUS_KEYWORDS_IDS), index(0)),
                        minusWordsPackNotFound()))));
    }

    @Test
    public void prepareAndApply_SetAnotherClientPack_MinusWordsPackNotFoundError() {
        ClientInfo anotherClientInfo = campaignSteps.createDefaultCampaign().getClientInfo();
        Long anotherClientPack = packSteps.createLibraryMinusKeywordsPacks(anotherClientInfo, 1).get(0);

        defaultAdGroup.withLibraryMinusKeywordsIds(singletonList(anotherClientPack));

        MassResult<Long> result = createFullAddOperation(singletonList(defaultAdGroup)).prepareAndApply();
        assertThat(result, isSuccessful(false));
        ValidationResult<?, Defect> actual = result.getValidationResult();

        Assertions.assertThat(actual).is(matchedBy(hasDefectWithDefinition(
                validationError(path(index(0), field(AdGroup.LIBRARY_MINUS_KEYWORDS_IDS), index(0)),
                        minusWordsPackNotFound()))));
    }

    @Test
    public void prepareAndApply_SetMoreThanMaxPacksCount_MaxCollectionSizeError() {
        List<Long> packs = packSteps.createLibraryMinusKeywordsPacks(clientInfo, MAX_LINKED_PACKS_TO_ONE_AD_GROUP + 1);
        defaultAdGroup.withLibraryMinusKeywordsIds(packs);

        MassResult<Long> result = createFullAddOperation(singletonList(defaultAdGroup)).prepareAndApply();
        assertThat(result, isSuccessful(false));
        ValidationResult<?, Defect> actual = result.getValidationResult();

        Assertions.assertThat(actual).is(matchedBy(hasDefectWithDefinition(
                validationError(path(index(0), field(AdGroup.LIBRARY_MINUS_KEYWORDS_IDS)),
                        maxCollectionSize(MAX_LINKED_PACKS_TO_ONE_AD_GROUP)))));
    }

    private AdGroup actualAdGroup(Long adGroupId) {
        return adGroupRepository.getAdGroups(clientInfo.getShard(), singletonList(adGroupId)).get(0);
    }
}
