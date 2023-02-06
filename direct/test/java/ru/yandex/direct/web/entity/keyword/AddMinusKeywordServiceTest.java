package ru.yandex.direct.web.entity.keyword;

import java.util.List;
import java.util.function.Function;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.adgroup.service.MinusKeywordPreparingTool;
import ru.yandex.direct.core.entity.keyword.processing.KeywordNormalizer;
import ru.yandex.direct.core.entity.keyword.processing.KeywordProcessingUtils;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.repository.TestUserRepository;
import ru.yandex.direct.core.testing.steps.AdGroupSteps;
import ru.yandex.direct.web.configuration.DirectWebTest;
import ru.yandex.direct.web.configuration.mock.auth.TestAuthHelper;
import ru.yandex.direct.web.core.model.WebResponse;
import ru.yandex.direct.web.entity.keyword.model.AddAdGroupMinusKeywordsRequestItem;
import ru.yandex.direct.web.entity.keyword.model.AddAdGroupMinusKeywordsResultItem;
import ru.yandex.direct.web.entity.keyword.model.AddCampaignMinusKeywordsRequestItem;
import ru.yandex.direct.web.entity.keyword.model.AddCampaignMinusKeywordsResultItem;
import ru.yandex.direct.web.entity.keyword.model.AddMinusKeywordsRequest;
import ru.yandex.direct.web.entity.keyword.model.AddMinusKeywordsResponse;
import ru.yandex.direct.web.entity.keyword.model.AddMinusKeywordsResult;
import ru.yandex.direct.web.entity.keyword.service.AddMinusKeywordService;
import ru.yandex.direct.web.validation.model.ValidationResponse;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.core.testing.data.TestGroups.activeTextAdGroup;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;

@DirectWebTest
@RunWith(SpringJUnit4ClassRunner.class)
public class AddMinusKeywordServiceTest {

    private static final String MINUS_KEYWORD_11 = "здесь могла бы быть ваша реклама " + randomAlphanumeric(5);
    private static final String MINUS_KEYWORD_12 = "минус-фраза " + randomAlphanumeric(5);
    private static final String MINUS_KEYWORD_21 = "кто здесь " + randomAlphanumeric(5);
    private static final String MINUS_KEYWORD_22 = "фраза не воробей " + randomAlphanumeric(5);

    @Autowired
    private TestUserRepository userRepository;

    @Autowired
    private AdGroupSteps adGroupSteps;

    @Autowired
    private AddMinusKeywordService addMinusKeywordService;

    @Autowired
    private TestAuthHelper testAuthHelper;

    @Autowired
    private MinusKeywordPreparingTool minusKeywordPreparingTool;

    @Autowired
    private KeywordNormalizer keywordNormalizer;

    private AdGroupInfo adGroupInfo1;
    private AdGroupInfo adGroupInfo2;

    @Before
    public void before() {
        adGroupInfo1 = adGroupSteps.createActiveTextAdGroup();
        adGroupInfo2 = adGroupSteps.createAdGroup(activeTextAdGroup(null), adGroupInfo1.getClientInfo());

        testAuthHelper.setOperatorAndSubjectUser(adGroupInfo1.getUid());
    }

    @Test
    public void addMinusKeywords_ValidKeywordsForCampaign_ResultIsValid() {
        AddCampaignMinusKeywordsRequestItem minusKeywordsItem1 = campaignMinusKeywords1(adGroupInfo1.getCampaignId());
        AddCampaignMinusKeywordsRequestItem minusKeywordsItem2 = campaignMinusKeywords2(adGroupInfo2.getCampaignId());
        checkValidRequest(asList(minusKeywordsItem1, minusKeywordsItem2), null);
    }

    @Test
    public void addMinusKeywords_ValidKeywordsForAdGroup_ResultIsValid() {
        AddAdGroupMinusKeywordsRequestItem minusKeywordsItem1 = adGroupMinusKeywords1(adGroupInfo1.getAdGroupId());
        AddAdGroupMinusKeywordsRequestItem minusKeywordsItem2 = adGroupMinusKeywords2(adGroupInfo2.getAdGroupId());
        checkValidRequest(null, asList(minusKeywordsItem1, minusKeywordsItem2));
    }

    @Test
    public void addMinusKeywords_ValidKeywordsForAdGroupAndCampaign_ResultIsValid() {
        AddCampaignMinusKeywordsRequestItem minusKeywordsItem1 = campaignMinusKeywords1(adGroupInfo1.getCampaignId());
        AddAdGroupMinusKeywordsRequestItem minusKeywordsItem2 = adGroupMinusKeywords2(adGroupInfo2.getAdGroupId());
        checkValidRequest(singletonList(minusKeywordsItem1), singletonList(minusKeywordsItem2));
    }

    @Test
    public void addMinusKeywords_NoKeywordsForAdGroupAndCampaign_ResultIsFailed() {
        AddMinusKeywordsRequest request = new AddMinusKeywordsRequest(null, null);

        WebResponse uncheckedResponse = addMinusKeywordService.addMinusKeywords(request);

        assertThat(uncheckedResponse, instanceOf(ValidationResponse.class));
    }

    @Test
    public void addMinusKeywords_ValidKeywordsForCampaignAndEmptyForAdGroup_ResultIsFailed() {
        AddCampaignMinusKeywordsRequestItem minusKeywordsItem1 = campaignMinusKeywords1(adGroupInfo1.getCampaignId());
        AddMinusKeywordsRequest request = new AddMinusKeywordsRequest(singletonList(minusKeywordsItem1), emptyList());

        WebResponse uncheckedResponse = addMinusKeywordService.addMinusKeywords(request);

        assertThat(uncheckedResponse, instanceOf(ValidationResponse.class));
    }

    @Test
    public void addMinusKeywords_ValidKeywordsForAdGroupAndEmptyForCampaign_ResultIsFailed() {
        AddAdGroupMinusKeywordsRequestItem minusKeywordsItem1 = adGroupMinusKeywords1(adGroupInfo1.getCampaignId());
        AddMinusKeywordsRequest request = new AddMinusKeywordsRequest(emptyList(), singletonList(minusKeywordsItem1));

        WebResponse uncheckedResponse = addMinusKeywordService.addMinusKeywords(request);

        assertThat(uncheckedResponse, instanceOf(ValidationResponse.class));
    }

    private void checkValidRequest(List<AddCampaignMinusKeywordsRequestItem> campaignMinusKeywordItems,
                                   List<AddAdGroupMinusKeywordsRequestItem> adGroupMinusKeywordItems) {
        AddMinusKeywordsRequest request =
                new AddMinusKeywordsRequest(campaignMinusKeywordItems, adGroupMinusKeywordItems);

        WebResponse uncheckedResponse = addMinusKeywordService.addMinusKeywords(request);

        assertThat(uncheckedResponse, instanceOf(AddMinusKeywordsResponse.class));

        AddMinusKeywordsResponse actualResponse = (AddMinusKeywordsResponse) uncheckedResponse;
        assertThat(actualResponse, beanDiffer(requestToExpectedResponse(request)));
    }

    private AddCampaignMinusKeywordsRequestItem campaignMinusKeywords1(long id) {
        return new AddCampaignMinusKeywordsRequestItem(id, asList(MINUS_KEYWORD_11, MINUS_KEYWORD_12));
    }

    private AddCampaignMinusKeywordsRequestItem campaignMinusKeywords2(long id) {
        return new AddCampaignMinusKeywordsRequestItem(id, asList(MINUS_KEYWORD_21, MINUS_KEYWORD_22));
    }

    private AddAdGroupMinusKeywordsRequestItem adGroupMinusKeywords1(long id) {
        return new AddAdGroupMinusKeywordsRequestItem(id, asList(MINUS_KEYWORD_11, MINUS_KEYWORD_12));
    }

    private AddAdGroupMinusKeywordsRequestItem adGroupMinusKeywords2(long id) {
        return new AddAdGroupMinusKeywordsRequestItem(id, asList(MINUS_KEYWORD_21, MINUS_KEYWORD_22));
    }

    private AddMinusKeywordsResponse requestToExpectedResponse(AddMinusKeywordsRequest request) {
        List<AddCampaignMinusKeywordsResultItem> campaignResults =
                campaignInputItemsToOutputResults(request.getCampaignMinusKeywords());
        List<AddAdGroupMinusKeywordsResultItem> adGroupResults =
                adGroupInputItemsToOutputResults(request.getAdGroupMinusKeywords());
        return new AddMinusKeywordsResponse(new AddMinusKeywordsResult(campaignResults, adGroupResults));
    }

    private List<AddCampaignMinusKeywordsResultItem> campaignInputItemsToOutputResults(
            List<AddCampaignMinusKeywordsRequestItem> minusKeywordsItems) {
        if (minusKeywordsItems == null) {
            return null;
        }
        Function<AddCampaignMinusKeywordsRequestItem, AddCampaignMinusKeywordsResultItem> createExpectedResultItem =
                requestItem -> {
                    List<String> outputKeywords = inputKeywordsToOutputKeywords(requestItem.getMinusKeywords());
                    int addedCount = requestItem.getMinusKeywords().size();
                    List<String> preparedMinusKeywords =
                            minusKeywordPreparingTool.fullPrepareForSaving(requestItem.getMinusKeywords());
                    int sumLength = KeywordProcessingUtils.getLengthWithoutSpecSymbolsAndSpaces(preparedMinusKeywords);
                    return new AddCampaignMinusKeywordsResultItem(requestItem.getId(),
                            outputKeywords, addedCount, sumLength);
                };
        return mapList(minusKeywordsItems, createExpectedResultItem);
    }

    private List<AddAdGroupMinusKeywordsResultItem> adGroupInputItemsToOutputResults(
            List<AddAdGroupMinusKeywordsRequestItem> minusKeywordsItems) {
        if (minusKeywordsItems == null) {
            return null;
        }
        Function<AddAdGroupMinusKeywordsRequestItem, AddAdGroupMinusKeywordsResultItem> createExpectedResultItem =
                requestItem -> {
                    List<String> outputKeywords = inputKeywordsToOutputKeywords(requestItem.getMinusKeywords());
                    int addedCount = requestItem.getMinusKeywords().size();
                    List<String> preparedMinusKeywords =
                            minusKeywordPreparingTool.fullPrepareForSaving(requestItem.getMinusKeywords());
                    int sumLength = KeywordProcessingUtils.getLengthWithoutSpecSymbolsAndSpaces(preparedMinusKeywords);
                    return new AddAdGroupMinusKeywordsResultItem(requestItem.getId(),
                            outputKeywords, addedCount, sumLength);
                };
        return mapList(minusKeywordsItems, createExpectedResultItem);
    }

    private List<String> inputKeywordsToOutputKeywords(List<String> keywords) {
        List<String> preparedKeywords = minusKeywordPreparingTool.preprocess(keywords);
        return keywordNormalizer.normalizeKeywords(preparedKeywords);
    }
}
