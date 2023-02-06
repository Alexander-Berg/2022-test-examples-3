package ru.yandex.direct.core.entity.keyword.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.CompareStrategy;
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies;
import ru.yandex.direct.core.entity.adgroup.model.AdGroupType;
import ru.yandex.direct.core.entity.client.service.ClientService;
import ru.yandex.direct.core.entity.keyword.container.AdGroupInfoForKeywordAdd;
import ru.yandex.direct.core.entity.keyword.container.AddedKeywordInfo;
import ru.yandex.direct.core.entity.keyword.container.KeywordsAddOperationParams;
import ru.yandex.direct.core.entity.keyword.model.Keyword;
import ru.yandex.direct.core.entity.keyword.service.validation.KeywordDefects;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.currency.Currency;
import ru.yandex.direct.currency.Money;
import ru.yandex.direct.operation.Applicability;
import ru.yandex.direct.result.MassResult;
import ru.yandex.direct.test.utils.differ.BigDecimalDiffer;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsIterableContainingInOrder.contains;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath.newPath;
import static ru.yandex.direct.core.entity.keyword.model.Keyword.PHRASE;
import static ru.yandex.direct.core.entity.keyword.service.validation.KeywordDefects.maxKeywordsPerAdGroupExceeded;
import static ru.yandex.direct.core.entity.keyword.service.validation.phrase.keyphrase.PhraseDefects.onlyStopWords;
import static ru.yandex.direct.core.testing.data.TestKeywords.defaultClientKeyword;
import static ru.yandex.direct.core.validation.defects.MoneyDefects.invalidValueNotLessThan;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;
import static ru.yandex.direct.test.utils.matcher.LocalDateTimeMatcher.approximatelyNow;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isFullySuccessful;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.utils.FunctionalUtils.intRange;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringRunner.class)
public class KeywordsAddOperationWithNonexistentAdGroupsOnPrepareTest extends KeywordsAddOperationBaseTest {

    private static final CompareStrategy COMPARE_STRATEGY = DefaultCompareStrategies.allFields()
            .forFields(newPath("modificationTime")).useMatcher(approximatelyNow())
            .forFields(newPath("price"), newPath("priceContext")).useDiffer(new BigDecimalDiffer());

    @Autowired
    private ClientService clientService;

    @Test(expected = IllegalStateException.class)
    public void exceptionThrownWhenAdGroupsInfoAreNotSetBeforePrepare() {
        List<Keyword> keywords = singletonList(defaultClientKeyword());
        KeywordsAddOperation addOperation = createOperationWithNonexistantAdGroups(keywords);
        addOperation.prepareAndApply();
    }

    @Test(expected = IllegalStateException.class)
    public void exceptionThrownWhenNotAllKeywordsHaveAdGroupInfo() {
        List<Keyword> keywords = asList(defaultClientKeyword(), defaultClientKeyword());
        KeywordsAddOperation addOperation = createOperationWithNonexistantAdGroups(keywords);
        addOperation.setKeywordsAdGroupInfo(singletonMap(1, getFakeAdGroupInfo()));
    }

    @Test(expected = IllegalArgumentException.class)
    public void exceptionThrownWhenExistingKeywordsGiven() {
        List<Keyword> keywords = singletonList(defaultClientKeyword());
        KeywordsAddOperation addOperation = createOperationWithNonexistantAdGroupsAndExistingKeywords(keywords);
        addOperation.prepareAndApply();
    }

    @Test(expected = IllegalStateException.class)
    public void exceptionThrownWhenAdGroupsIdsAreNotSetBeforeApply() {
        List<Keyword> keywords = singletonList(defaultClientKeyword());
        KeywordsAddOperation addOperation = createOperationWithNonexistantAdGroups(keywords);
        addOperation.setKeywordsAdGroupInfo(singletonMap(0, getFakeAdGroupInfo()));
        addOperation.prepareAndApply();
    }

    @Test(expected = IllegalStateException.class)
    public void exceptionThrownWhenNotAllAdGroupIdsGiven() {
        List<Keyword> keywords = asList(defaultClientKeyword(), defaultClientKeyword());
        KeywordsAddOperation addOperation = createOperationWithNonexistantAdGroups(keywords);

        Map<Integer, AdGroupInfoForKeywordAdd> keywordsAdGroupInfo = new HashMap<>();
        keywordsAdGroupInfo.put(0, getFakeAdGroupInfo());
        keywordsAdGroupInfo.put(1, getFakeAdGroupInfo());
        addOperation.setKeywordsAdGroupInfo(keywordsAdGroupInfo);

        addOperation.prepare();
        addOperation.setAdGroupsIds(singletonMap(0, 1L));
        addOperation.apply();
    }

    @Test
    public void successfulAddOneKeyword() {
        List<Keyword> keywords = singletonList(defaultClientKeyword());
        KeywordsAddOperation addOperation = createOperationWithNonexistantAdGroups(keywords);
        AdGroupInfo adGroup = adGroupSteps.createActiveTextAdGroup(clientInfo);
        addOperation.setKeywordsAdGroupInfo(
                singletonMap(0, new AdGroupInfoForKeywordAdd(1, adGroup.getCampaignId(), AdGroupType.BASE)));
        Optional<MassResult<AddedKeywordInfo>> prepareResult = addOperation.prepare();
        assumeThat("prepare should be successful", prepareResult.isPresent(), is(false));

        addOperation.setAdGroupsIds(singletonMap(0, adGroup.getAdGroupId()));
        MassResult<AddedKeywordInfo> result = addOperation.apply();
        assertThat(result, isFullySuccessful());
        List<Keyword> actualKeywords =
                keywordRepository.getKeywordsByAdGroupId(adGroup.getShard(), adGroup.getAdGroupId());
        assertThat(keywords, contains(beanDiffer(actualKeywords.get(0)).useCompareStrategy(COMPARE_STRATEGY)));
    }

    @Test
    public void successfulAddSeveralKeywords() {
        List<Keyword> keywords = asList(defaultClientKeyword(), defaultClientKeyword());
        KeywordsAddOperation addOperation = createOperationWithNonexistantAdGroups(keywords);
        AdGroupInfo adGroup1 = adGroupSteps.createActiveTextAdGroup(clientInfo);
        AdGroupInfo adGroup2 = adGroupSteps.createActiveTextAdGroup(clientInfo);
        Map<Integer, AdGroupInfoForKeywordAdd> keywordsAdGroupInfo = new HashMap<>();
        keywordsAdGroupInfo.put(0, new AdGroupInfoForKeywordAdd(1, adGroup1.getCampaignId(), AdGroupType.BASE));
        keywordsAdGroupInfo.put(1, new AdGroupInfoForKeywordAdd(2, adGroup2.getCampaignId(), AdGroupType.BASE));
        addOperation.setKeywordsAdGroupInfo(keywordsAdGroupInfo);

        Optional<MassResult<AddedKeywordInfo>> prepareResult = addOperation.prepare();
        assumeThat("prepare should be successful", prepareResult.isPresent(), is(false));

        Map<Integer, Long> adGroupsIds = new HashMap<>();
        adGroupsIds.put(0, adGroup1.getAdGroupId());
        adGroupsIds.put(1, adGroup2.getAdGroupId());
        addOperation.setAdGroupsIds(adGroupsIds);

        MassResult<AddedKeywordInfo> result = addOperation.apply();
        assertThat(result, isFullySuccessful());
        List<Keyword> actualKeywords =
                keywordRepository.getKeywordsByAdGroupId(adGroup1.getShard(), adGroup1.getAdGroupId());
        actualKeywords.addAll(keywordRepository.getKeywordsByAdGroupId(adGroup2.getShard(), adGroup2.getAdGroupId()));
        assertThat(keywords, contains(mapList(actualKeywords,
                actualKeyword -> beanDiffer(actualKeyword).useCompareStrategy(COMPARE_STRATEGY))));
    }

    @Test
    public void noValidationErrorWhenMaxKeywordsInAdGroup() {
        Long keywordsLimit = clientLimitsService.massGetClientLimits(singletonList(clientInfo.getClientId()))
                .iterator().next()
                .getKeywordsCountLimitOrDefault();
        List<Keyword> keywords = Collections.nCopies(keywordsLimit.intValue(), defaultClientKeyword());
        KeywordsAddOperation addOperation = createOperationWithNonexistantAdGroups(keywords);
        AdGroupInfo adGroup = adGroupSteps.createActiveTextAdGroup(clientInfo);
        Map<Integer, AdGroupInfoForKeywordAdd> keywordsAdGroupInfo = new HashMap<>();
        intRange(0, keywords.size()).forEach(index -> keywordsAdGroupInfo
                .put(index, new AdGroupInfoForKeywordAdd(1, adGroup.getCampaignId(), AdGroupType.BASE)));
        addOperation.setKeywordsAdGroupInfo(keywordsAdGroupInfo);
        Optional<MassResult<AddedKeywordInfo>> prepareResult = addOperation.prepare();
        assertThat(prepareResult.isPresent(), is(false));
        addOperation.setAdGroupsIds(singletonMap(0, adGroup.getAdGroupId()));
        MassResult<AddedKeywordInfo> result = addOperation.apply();
        assertThat(result, isFullySuccessful());
    }

    // ошибки валидации

    @Test
    public void validationErrorWhenTooManyKeywordsInAdGroup() {
        Long keywordsLimit = clientLimitsService.massGetClientLimits(singletonList(clientInfo.getClientId()))
                .iterator().next()
                .getKeywordsCountLimitOrDefault();
        List<Keyword> keywords = Collections.nCopies(keywordsLimit.intValue() + 1, defaultClientKeyword());

        MassResult<AddedKeywordInfo> prepareResult = prepareOperationExpectingErrors(keywords);
        assertThat(prepareResult.getValidationResult(), hasDefectDefinitionWith(
                validationError(path(index(0)), maxKeywordsPerAdGroupExceeded(keywordsLimit.intValue(), 1))));
    }

    @Test
    public void validationErrorWhenNotAcceptableAdGroupType() {
        CampaignInfo activeTextCampaign = steps.campaignSteps().createActiveTextCampaign(clientInfo);

        List<Keyword> keywords = singletonList(defaultClientKeyword());

        KeywordsAddOperation addOperation = createOperationWithNonexistantAdGroups(keywords);

        addOperation.setKeywordsAdGroupInfo(singletonMap(0,
                new AdGroupInfoForKeywordAdd(1, activeTextCampaign.getCampaignId(), AdGroupType.DYNAMIC)));

        Optional<MassResult<AddedKeywordInfo>> prepareResult = addOperation.prepare();
        assertThat(prepareResult.isPresent(), is(true));
        assertThat(prepareResult.get().getValidationResult(), hasDefectDefinitionWith(
                validationError(path(index(0), field("adGroupId")),
                        KeywordDefects.notAcceptableAdGroupType())));
    }

    @Test
    public void validationErrorWhenInvalidPrice() {
        Currency currency = clientService.getWorkCurrency(clientInfo.getClientId());
        List<Keyword> keywords =
                singletonList(defaultClientKeyword().withPrice(currency.getMinPrice().add(BigDecimal.ONE.negate())));
        MassResult<AddedKeywordInfo> prepareResult = prepareOperationExpectingErrors(keywords);
        assertThat(prepareResult.getValidationResult(),
                hasDefectDefinitionWith(validationError(path(index(0), field("price")),
                        invalidValueNotLessThan(Money.valueOf(currency.getMinPrice(), currency.getCode())))));
    }

    @Test
    public void validationErrorWhenKeywordIsValidOnFirstValidationStepAndInvalidOnSecond() {
        List<Keyword> keywords =
                singletonList(defaultClientKeyword().withPhrase("на по"));
        MassResult<AddedKeywordInfo> prepareResult = prepareOperationExpectingErrors(keywords);
        assertThat(prepareResult.getValidationResult(),
                hasDefectDefinitionWith(validationError(path(index(0), field(PHRASE)), onlyStopWords())));
    }

    private MassResult<AddedKeywordInfo> prepareOperationExpectingErrors(List<Keyword> keywords) {
        Optional<MassResult<AddedKeywordInfo>> prepareResult = prepareOperation(keywords);
        assertThat(prepareResult.isPresent(), is(true));
        return prepareResult.get();
    }

    private Optional<MassResult<AddedKeywordInfo>> prepareOperation(List<Keyword> keywords) {
        CampaignInfo activeTextCampaign = steps.campaignSteps().createActiveTextCampaign(clientInfo);
        KeywordsAddOperation addOperation = createOperationWithNonexistantAdGroups(keywords);

        Map<Integer, AdGroupInfoForKeywordAdd> keywordsAdGroupInfo = new HashMap<>();
        intRange(0, keywords.size())
                .forEach(index -> keywordsAdGroupInfo.put(index,
                        new AdGroupInfoForKeywordAdd(1, activeTextCampaign.getCampaignId(), AdGroupType.BASE)));

        addOperation.setKeywordsAdGroupInfo(keywordsAdGroupInfo);

        return addOperation.prepare();
    }

    private KeywordsAddOperation createOperationWithNonexistantAdGroups(List<Keyword> keywords) {
        KeywordsAddOperationParams operationParams = KeywordsAddOperationParams.builder()
                .withAdGroupsNonexistentOnPrepare(true)
                .withUnglueEnabled(true)
                .withIgnoreOversize(false)
                .withAutoPrices(false)
                .withWeakenValidation(false)
                .build();
        return createOperation(Applicability.FULL, operationParams, keywords, null, clientInfo.getUid());
    }

    private KeywordsAddOperation createOperationWithNonexistantAdGroupsAndExistingKeywords(List<Keyword> keywords) {
        KeywordsAddOperationParams operationParams = KeywordsAddOperationParams.builder()
                .withAdGroupsNonexistentOnPrepare(true)
                .withUnglueEnabled(true)
                .withIgnoreOversize(false)
                .withAutoPrices(false)
                .withWeakenValidation(false)
                .build();
        return createOperation(Applicability.FULL, operationParams, keywords, new ArrayList<>(), clientInfo.getUid());
    }

    private AdGroupInfoForKeywordAdd getFakeAdGroupInfo() {
        return new AdGroupInfoForKeywordAdd(2, 1L, AdGroupType.BASE);
    }
}
