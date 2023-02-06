package ru.yandex.direct.grid.processing.service.showcondition;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.List;

import com.google.common.collect.ImmutableList;
import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.StatusBsSynced;
import ru.yandex.direct.core.entity.campaign.model.CampaignsPlatform;
import ru.yandex.direct.core.entity.keyword.model.Keyword;
import ru.yandex.direct.core.entity.keyword.service.KeywordService;
import ru.yandex.direct.core.entity.relevancematch.model.RelevanceMatch;
import ru.yandex.direct.core.entity.relevancematch.service.RelevanceMatchService;
import ru.yandex.direct.core.testing.data.TestRelevanceMatches;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.model.showcondition.mutation.GdSetBids;
import ru.yandex.direct.grid.processing.model.showcondition.mutation.GdSetBidsPayload;
import ru.yandex.direct.grid.processing.service.dataloader.GridContextProvider;
import ru.yandex.direct.grid.processing.service.showcondition.bids.BidsDataService;
import ru.yandex.direct.grid.processing.util.ContextHelper;

import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath.newPath;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.allFieldsExcept;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
public class SetBidsKeywordAndRelevanceMatchTest {
    private static final BigDecimal PRICE_SEARCH = TestRelevanceMatches.DEFAULT_PRICE_SEARCH.add(BigDecimal.ONE);
    private static final BigDecimal PRICE_CONTEXT = TestRelevanceMatches.DEFAULT_PRICE_CONTEXT.add(BigDecimal.TEN);

    @Autowired
    private Steps steps;

    @Autowired
    private KeywordService keywordService;

    @Autowired
    private RelevanceMatchService relevanceMatchService;

    @Autowired
    private BidsDataService bidsDataService;

    @Autowired
    private GridContextProvider gridContextProvider;

    private UserInfo operatorInfo;
    private AdGroupInfo adGroupInfo;
    private SoftAssertions softAssertions = new SoftAssertions();

    @Before
    public void initTestData() {
        operatorInfo = steps.userSteps().createDefaultUser();

        CampaignInfo campaignInfo =
                steps.campaignSteps().createActiveCampaign(operatorInfo.getClientInfo(), CampaignsPlatform.SEARCH);
        adGroupInfo = steps.adGroupSteps().createActiveTextAdGroup(campaignInfo);
        gridContextProvider.setGridContext(ContextHelper.buildContext(operatorInfo.getUser())
                .withFetchedFieldsReslover(null));
    }

    @Test
    public void setBids_KeywordAndRelevanceMatchSearch() {
        Long keywordId = steps.keywordSteps().createKeyword(adGroupInfo).getId();

        RelevanceMatch unmodifedRelevanceMatch = steps.relevanceMatchSteps().getDefaultRelevanceMatch(adGroupInfo);
        Long relevanceMatchId = steps.relevanceMatchSteps()
                .addRelevanceMatchToAdGroup(Collections.singletonList(unmodifedRelevanceMatch),adGroupInfo).get(0);

        List<Long> showConditionIds = ImmutableList.of(keywordId, relevanceMatchId);
        List<Keyword> expectedKeywords =
                keywordService.getKeywords(operatorInfo.getClientInfo().getClientId(), Collections.singletonList(keywordId));
        List<RelevanceMatch> expectedRelevanceMatches =
                relevanceMatchService.getRelevanceMatchByIds(operatorInfo.getClientInfo().getClientId(), Collections.singletonList(relevanceMatchId));

        GdSetBids gdSetBids = new GdSetBids().withShowConditionIds(showConditionIds)
                .withExactPriceSearch(PRICE_SEARCH)
                .withExactPriceContext(PRICE_CONTEXT);

        GdSetBidsPayload result = bidsDataService.setBids(gdSetBids);

        softAssertions.assertThat(result.getShowConditionIds()).containsOnly(keywordId, relevanceMatchId);

        List<Keyword> actualKeywords =
                keywordService.getKeywords(operatorInfo.getClientInfo().getClientId(), Collections.singletonList(keywordId));
        List<RelevanceMatch> actualRelevanceMatches =
                relevanceMatchService.getRelevanceMatchByIds(operatorInfo.getClientInfo().getClientId(), Collections.singletonList(relevanceMatchId));

        expectedKeywords.forEach(keyword -> keyword
                .withPrice(PRICE_SEARCH.setScale(2, RoundingMode.HALF_UP))
                .withPriceContext(BigDecimal.ONE.setScale(2, RoundingMode.HALF_UP))
                .withStatusBsSynced(StatusBsSynced.NO));
        expectedRelevanceMatches.forEach(relevanceMatch -> relevanceMatch
                .withPrice(PRICE_SEARCH.setScale(2, RoundingMode.HALF_UP))
                .withPriceContext(unmodifedRelevanceMatch.getPriceContext().setScale(2, RoundingMode.HALF_UP))
                .withStatusBsSynced(StatusBsSynced.NO));

        softAssertions.assertThat(actualKeywords).is(matchedBy(beanDiffer(expectedKeywords)
                .useCompareStrategy(allFieldsExcept(newPath(".*", "modificationTime")))));
        softAssertions.assertThat(actualRelevanceMatches).is(matchedBy(beanDiffer(expectedRelevanceMatches)
                .useCompareStrategy(allFieldsExcept(newPath(".*", "lastChangeTime")))));
        softAssertions.assertAll();
    }
}
