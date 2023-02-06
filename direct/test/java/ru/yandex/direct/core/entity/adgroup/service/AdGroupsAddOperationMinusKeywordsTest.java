package ru.yandex.direct.core.entity.adgroup.service;

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.repository.TestMinusKeywordsPackRepository;
import ru.yandex.direct.operation.Applicability;
import ru.yandex.direct.result.MassResult;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.keyword.service.validation.phrase.minusphrase.MinusPhraseDefects.nestedOrEmptySquareBrackets;
import static ru.yandex.direct.core.testing.data.TestGroups.clientTextAdGroup;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isFullySuccessful;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isSuccessful;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class AdGroupsAddOperationMinusKeywordsTest extends AdGroupsAddOperationTestBase {

    @Autowired
    private TestMinusKeywordsPackRepository testMinusKeywordsPackRepository;

    @Test(expected = IllegalArgumentException.class)
    public void prepareAndApply_MinusKeywordsIdSpecified() {
        AdGroup adGroup = clientTextAdGroup(campaignInfo.getCampaignId())
                .withMinusKeywordsId(1L);

        createFullAddOperation(singletonList(adGroup)).prepareAndApply();
    }

    @Test
    public void prepareAndApply_ValidWithoutMinusKeywords() {
        AdGroup adGroup = clientTextAdGroup(campaignInfo.getCampaignId())
                .withMinusKeywords(null);

        MassResult<Long> result = createFullAddOperation(singletonList(adGroup)).prepareAndApply();
        assertThat(result, isFullySuccessful());
        AdGroup actualAdGroup = actualAdGroup(result.get(0).getResult());

        assertThat(actualAdGroup.getMinusKeywords(), empty());
        assertThat(actualAdGroup.getMinusKeywordsId(), nullValue());
    }

    @Test
    public void prepareAndApply_ValidWithMinusKeywords() {
        List<String> minusKeywords = asList("word1", "word2");
        AdGroup adGroup = clientTextAdGroup(campaignInfo.getCampaignId())
                .withMinusKeywords(minusKeywords);

        MassResult<Long> result = createFullAddOperation(singletonList(adGroup)).prepareAndApply();
        assertThat(result, isFullySuccessful());
        AdGroup actualAdGroup = actualAdGroup(result.get(0).getResult());

        assertThat(actualAdGroup.getMinusKeywords(), is(minusKeywords));
        assertThat(actualAdGroup.getMinusKeywordsId(), notNullValue());
    }

    @Test
    public void prepareAndApply_InvalidMinusKeywords_ValidationError() {
        AdGroup adGroup = adGroupWithInvalidMinusKeywords();

        MassResult<Long> result = createFullAddOperation(singletonList(adGroup)).prepareAndApply();
        assertThat(result, isSuccessful(false));

        assertThat(result.getValidationResult(), hasDefectDefinitionWith(
                validationError(path(index(0), field(AdGroup.MINUS_KEYWORDS)),
                        nestedOrEmptySquareBrackets(singletonList("[]")))));
    }

    @Test
    public void prepareAndApply_TwoValidGroups_AddedMinusKeywordPacksMatchToGroup() {
        List<String> mk1 = singletonList(randomAlphanumeric(10));
        List<String> mk2 = singletonList(randomAlphanumeric(10));
        AdGroup adGroup1 = clientTextAdGroup(campaignInfo.getCampaignId())
                .withMinusKeywords(mk1);
        AdGroup adGroup2 = clientTextAdGroup(campaignInfo.getCampaignId())
                .withMinusKeywords(mk2);

        MassResult<Long> result = createFullAddOperation(asList(adGroup1, adGroup2)).prepareAndApply();

        assertThat(result, isSuccessful(true, true));

        AdGroup actualAdGroup1 = actualAdGroup(result.get(0).getResult());
        assertThat(actualAdGroup1.getMinusKeywords(), is(mk1));
        assertThat(actualAdGroup1.getMinusKeywordsId(), notNullValue());

        AdGroup actualAdGroup2 = actualAdGroup(result.get(1).getResult());
        assertThat(actualAdGroup2.getMinusKeywords(), is(mk2));
        assertThat(actualAdGroup2.getMinusKeywordsId(), notNullValue());
    }

    @Test
    public void prepareAndApply_ValidaAndInvalidGroups_MinusKeywordPackNotCreatedForInvalidGroup() {
        AdGroup adGroupInvalid = adGroupWithInvalidName()
                .withMinusKeywords(singletonList(randomAlphanumeric(10)));
        AdGroup adGroupValid = clientTextAdGroup(campaignInfo.getCampaignId())
                .withMinusKeywords(singletonList(randomAlphanumeric(10)));

        int countMinusKeywordPackBefore = testMinusKeywordsPackRepository.getClientPacks(shard, clientId).size();
        MassResult<Long> result = createAddOperation(Applicability.PARTIAL, asList(adGroupInvalid, adGroupValid))
                .prepareAndApply();
        int countMinusKeywordPackAfter = testMinusKeywordsPackRepository.getClientPacks(shard, clientId).size();

        assertThat(result, isSuccessful(false, true));
        assertThat("должен добавиться только один новый набор минус-фраз (для валидной группы)",
                countMinusKeywordPackAfter, is(countMinusKeywordPackBefore + 1));
    }

    @Test
    public void prepareAndApply_ValidWithoutMinusKeywordsAndInvalidWithMinusKeywordsGroups_MinusKeywordPackNotCreated() {
        List<AdGroup> adGroups = asList(
                adGroupWithInvalidName()
                        .withMinusKeywords(singletonList(randomAlphanumeric(10))),
                clientTextAdGroup(campaignInfo.getCampaignId())
                        .withMinusKeywords(null),
                clientTextAdGroup(campaignInfo.getCampaignId())
                        .withMinusKeywords(emptyList())
        );

        int countMinusKeywordPackBefore = testMinusKeywordsPackRepository.getClientPacks(shard, clientId).size();
        MassResult<Long> result = createAddOperation(Applicability.PARTIAL, adGroups).prepareAndApply();
        int countMinusKeywordPackAfter = testMinusKeywordsPackRepository.getClientPacks(shard, clientId).size();

        assertThat(result, isSuccessful(false, true, true));
        assertThat("ни один набор минус-фраз не должен добавиться",
                countMinusKeywordPackAfter, is(countMinusKeywordPackBefore));
    }

    @Test
    public void prepareAndApply_ValidWithoutMinusKeywordsAndValidWithInvalidMinusKeywordsGroups_MinusKeywordPackNotCreated() {
        List<AdGroup> adGroups = asList(
                adGroupWithInvalidMinusKeywords(),
                clientTextAdGroup(campaignInfo.getCampaignId())
                        .withMinusKeywords(null),
                clientTextAdGroup(campaignInfo.getCampaignId())
                        .withMinusKeywords(emptyList())
        );

        int countMinusKeywordPackBefore = testMinusKeywordsPackRepository.getClientPacks(shard, clientId).size();
        MassResult<Long> result = createAddOperation(Applicability.PARTIAL, adGroups).prepareAndApply();
        int countMinusKeywordPackAfter = testMinusKeywordsPackRepository.getClientPacks(shard, clientId).size();

        assertThat(result, isSuccessful(false, true, true));
        assertThat("ни один набор минус-фраз не должен добавиться",
                countMinusKeywordPackAfter, is(countMinusKeywordPackBefore));
    }

    @Test
    public void prepareAndApply_MixedItems() {
        List<AdGroup> adGroups = asList(
                adGroupWithInvalidMinusKeywords(),
                adGroupWithInvalidName()
                        .withMinusKeywords(null),
                clientTextAdGroup(campaignInfo.getCampaignId())
                        .withMinusKeywords(null),
                clientTextAdGroup(campaignInfo.getCampaignId())
                        .withMinusKeywords(asList("word1", "word2"))
        );

        MassResult<Long> result = createAddOperation(Applicability.PARTIAL, adGroups).prepareAndApply();
        assertThat(result, isSuccessful(false, false, true, true));
    }

    private AdGroup actualAdGroup(Long adGroupId) {
        return adGroupRepository.getAdGroups(clientInfo.getShard(), singletonList(adGroupId)).get(0);
    }
}
