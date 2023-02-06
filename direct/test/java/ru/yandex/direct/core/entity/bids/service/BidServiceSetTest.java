package ru.yandex.direct.core.entity.bids.service;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.StatusBsSynced;
import ru.yandex.direct.core.entity.bids.container.SetBidItem;
import ru.yandex.direct.core.entity.bids.container.ShowConditionType;
import ru.yandex.direct.core.entity.bids.model.Bid;
import ru.yandex.direct.core.entity.bids.repository.BidRepository;
import ru.yandex.direct.core.entity.campaign.container.CampaignStrategyChangingSettings;
import ru.yandex.direct.core.entity.campaign.model.Campaign;
import ru.yandex.direct.core.entity.campaign.model.TextCampaign;
import ru.yandex.direct.core.entity.keyword.model.Keyword;
import ru.yandex.direct.core.entity.keyword.service.KeywordService;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.KeywordInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.currency.CurrencyCode;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.result.MassResult;
import ru.yandex.direct.result.Result;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static ru.yandex.direct.core.entity.bids.validation.BidsDefects.Bids.FIELD_DOES_NOT_MATCH_STRATEGY;
import static ru.yandex.direct.core.testing.data.TestBanners.activeTextBanner;
import static ru.yandex.direct.core.testing.data.TestKeywords.defaultKeyword;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isFullySuccessful;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class BidServiceSetTest {

    private static final BigDecimal PRICE_CONTEXT = BigDecimal.valueOf(19L);
    private static final BigDecimal PRICE_SEARCH = BigDecimal.valueOf(10L);

    @Autowired
    private Steps steps;

    @Autowired
    private KeywordService keywordService;

    @Autowired
    private BidService testingBidService;

    @Autowired
    private BidRepository bidRepository;

    private KeywordInfo keywordInfo;
    private KeywordInfo anotherKeywordInfo;
    private ClientId clientId;
    private Long clientUid;
    private CampaignInfo autoBudgetCampaignInfo;
    private KeywordInfo autobudgetCampaignKeyword;
    private KeywordInfo anotherAutobudgetCampaignKeyword;
    private AdGroupInfo autobudgetAdGroupInfo;
    private AdGroupInfo adGroupInfo;
    private AdGroupInfo adGroupInfo2;


    @Before
    public void setUp() throws Exception {
        keywordInfo = steps.keywordSteps().createDefaultKeyword();
        steps.bannerSteps().createBanner(activeTextBanner(keywordInfo.getCampaignId(), keywordInfo.getAdGroupId()),
                keywordInfo.getAdGroupInfo());

        ClientInfo clientInfo = keywordInfo.getAdGroupInfo().getClientInfo();
        clientId = clientInfo.getClientId();
        clientUid = clientInfo.getUid();
        anotherKeywordInfo = steps.keywordSteps().createKeyword(keywordInfo.getAdGroupInfo());

        autoBudgetCampaignInfo = steps.campaignSteps().createActiveTextCampaignAutoStrategy(clientInfo);
        autobudgetAdGroupInfo = steps.adGroupSteps().createActiveTextAdGroup(autoBudgetCampaignInfo);
        autobudgetCampaignKeyword = steps.keywordSteps().createKeyword(autobudgetAdGroupInfo);
        anotherAutobudgetCampaignKeyword = steps.keywordSteps().createKeyword(autobudgetAdGroupInfo);
        adGroupInfo = steps.adGroupSteps().createDefaultAdGroup();
        adGroupInfo2 = steps.adGroupSteps().createDefaultAdGroup();
    }

    @Test
    public void setBid_PriceSearchPriceContextSet_NoError() {
        Long id = keywordInfo.getId();
        List<SetBidItem> setBidItems = Collections.singletonList(new SetBidItem()
                .withId(id)
                .withPriceContext(PRICE_CONTEXT)
                .withPriceSearch(PRICE_SEARCH)
                .withShowConditionType(ShowConditionType.KEYWORD));

        MassResult<SetBidItem> result = testingBidService.setBids(clientId, clientUid, setBidItems);

        assertCorrectResultForSingleKeywordIsWrittenToDb(id, result,
                t -> t.equals(setBidItems.get(0).getPriceSearch()),
                t -> t.equals(setBidItems.get(0).getPriceContext()));
    }

    @Test
    public void setBid_AutobudgetCampaignNotAutobudgetKeyword_Error() {
        List<SetBidItem> setBidItems = Collections.singletonList(new SetBidItem()
                .withId(autobudgetCampaignKeyword.getId())
                .withPriceContext(PRICE_CONTEXT)
                .withPriceSearch(PRICE_SEARCH)
                .withShowConditionType(ShowConditionType.KEYWORD));

        MassResult<SetBidItem> result = testingBidService.setBids(clientId, clientUid, setBidItems);
        assertThat(result.getValidationResult()).is(matchedBy(hasDefectDefinitionWith(
                validationError(path(index(0), field(Bid.AUTOBUDGET_PRIORITY)), FIELD_DOES_NOT_MATCH_STRATEGY))));
    }

    @Test
    public void setBid_AutobudgetCampaign_NoError() {
        List<SetBidItem> setBidItems = Collections.singletonList(new SetBidItem()
                .withId(autobudgetCampaignKeyword.getId())
                .withPriceContext(PRICE_CONTEXT)
                .withPriceSearch(PRICE_SEARCH)
                .withShowConditionType(ShowConditionType.KEYWORD)
                .withAutobudgetPriority(1));

        MassResult<SetBidItem> result = testingBidService.setBids(clientId, clientUid, setBidItems);

        assertCorrectResultForSingleKeywordIsWrittenToDb(autobudgetCampaignKeyword.getId(), result,
                t -> t != null, t -> t != null);
    }


    @Test
    public void setBid_AutobudgetCampaignNoSearchPriceNoPriceContext_NoError() {
        List<SetBidItem> setBidItems = Collections.singletonList(new SetBidItem()
                .withId(anotherAutobudgetCampaignKeyword.getId())
                .withPriceContext(null)
                .withPriceSearch(null)
                .withShowConditionType(ShowConditionType.KEYWORD)
                .withAutobudgetPriority(1));

        MassResult<SetBidItem> result = testingBidService.setBids(clientId, clientUid, setBidItems);
        assertCorrectResultForSingleKeywordIsWrittenToDb(anotherAutobudgetCampaignKeyword.getId(), result,
                null, null);
    }

    @Test
    public void setBid_NoContextPrice_NoError() {
        Long id = anotherKeywordInfo.getId();
        List<SetBidItem> setBidItems = Collections.singletonList(new SetBidItem()
                .withId(id)
                .withPriceSearch(PRICE_SEARCH)
                .withShowConditionType(ShowConditionType.KEYWORD));

        MassResult<SetBidItem> result = testingBidService.setBids(clientId, clientUid, setBidItems);
        assertCorrectResultForSingleKeywordIsWrittenToDb(id, result,
                t -> t.equals(setBidItems.get(0).getPriceSearch()), null);
    }

    @Test
    public void saveManualBidsTest() {
        BigDecimal priceBidFirst = BigDecimal.valueOf(5);
        BigDecimal priceBidSecond = BigDecimal.valueOf(10);

        KeywordInfo keywordInfo1 = steps.keywordSteps()
                .createKeyword(adGroupInfo, defaultKeyword()
                        .withPrice(priceBidFirst)
                        .withPriceContext(priceBidFirst)
                );
        KeywordInfo keywordInfo2 = steps.keywordSteps()
                .createKeyword(adGroupInfo2, defaultKeyword()
                        .withPrice(priceBidSecond)
                        .withPriceContext(priceBidSecond)
                );

        int shard = adGroupInfo.getShard();
        int shard2 = adGroupInfo2.getShard();

        Campaign campaign1 = new Campaign()
                .withId(adGroupInfo.getCampaignId())
                .withClientId(adGroupInfo.getClientId().asLong());

        Campaign campaign2 = new Campaign()
                .withId(adGroupInfo2.getCampaignId())
                .withClientId(adGroupInfo2.getClientId().asLong());

        List<Bid> bidsForCampaign1 = bidRepository.getBidsByCampaignIds(shard,
                singletonList(adGroupInfo.getCampaignId()));
        MatcherAssert.assertThat(bidsForCampaign1, hasSize(1));
        MatcherAssert.assertThat(bidsForCampaign1.get(0), Matchers.allOf(
                Matchers.hasProperty("price", comparesEqualTo(priceBidFirst)),
                Matchers.hasProperty("priceContext", comparesEqualTo(priceBidFirst)),
                Matchers.hasProperty("id", equalTo(keywordInfo1.getId())),
                Matchers.hasProperty("campaignId", equalTo(keywordInfo1.getCampaignId())),
                Matchers.hasProperty("adGroupId", equalTo(keywordInfo1.getAdGroupId()))
        ));
        List<Bid> bidsForCampaign2 = bidRepository.getBidsByCampaignIds(shard2,
                singletonList(adGroupInfo2.getCampaignId()));
        MatcherAssert.assertThat(bidsForCampaign2, hasSize(1));
        MatcherAssert.assertThat(bidsForCampaign2.get(0), Matchers.allOf(
                Matchers.hasProperty("price", comparesEqualTo(priceBidSecond)),
                Matchers.hasProperty("priceContext", comparesEqualTo(priceBidSecond)),
                Matchers.hasProperty("id", equalTo(keywordInfo2.getId())),
                Matchers.hasProperty("campaignId", equalTo(keywordInfo2.getCampaignId())),
                Matchers.hasProperty("adGroupId", equalTo(keywordInfo2.getAdGroupId()))
        ));
        testingBidService.saveManualBids(asList(campaign1, campaign2));

        List<Bid> bidsAfterCopyForCampaign1 = bidRepository.getBidsManualPricesForCampaignIds(shard,
                singletonList(adGroupInfo.getCampaignId()));
        MatcherAssert.assertThat(bidsAfterCopyForCampaign1, hasSize(1));
        MatcherAssert.assertThat(bidsAfterCopyForCampaign1.get(0), Matchers.allOf(
                Matchers.hasProperty("price", comparesEqualTo(priceBidFirst)),
                Matchers.hasProperty("priceContext", comparesEqualTo(priceBidFirst)),
                Matchers.hasProperty("id", equalTo(keywordInfo1.getId())),
                Matchers.hasProperty("campaignId", equalTo(keywordInfo1.getCampaignId()))
        ));
        List<Bid> bidsAfterCopyForCampaign2 = bidRepository.getBidsByCampaignIds(shard2,
                singletonList(adGroupInfo2.getCampaignId()));
        MatcherAssert.assertThat(bidsAfterCopyForCampaign2, hasSize(1));
        MatcherAssert.assertThat(bidsAfterCopyForCampaign2.get(0), Matchers.allOf(
                Matchers.hasProperty("price", comparesEqualTo(priceBidSecond)),
                Matchers.hasProperty("priceContext", comparesEqualTo(priceBidSecond)),
                Matchers.hasProperty("id", equalTo(keywordInfo2.getId())),
                Matchers.hasProperty("campaignId", equalTo(keywordInfo2.getCampaignId()))
        ));
    }

    @Test
    public void restoreManualBidsTest() {
        CurrencyCode currencyCode = CurrencyCode.EUR;
        BigDecimal maxPrice = currencyCode.getCurrency().getMaxPrice();
        BigDecimal priceBidFirst = BigDecimal.valueOf(5);
        BigDecimal priceBidSecond = BigDecimal.valueOf(10);
        BigDecimal priceBidThird = BigDecimal.valueOf(15);
        BigDecimal priceBidManualFirst = BigDecimal.valueOf(100);
        BigDecimal priceBidManualSecond = maxPrice.multiply(BigDecimal.TEN);

        KeywordInfo keywordInfo1 = steps.keywordSteps()
                .createKeyword(adGroupInfo, defaultKeyword()
                        .withPrice(priceBidFirst)
                        .withPriceContext(priceBidFirst)
                );
        KeywordInfo keywordInfo2 = steps.keywordSteps()
                .createKeyword(adGroupInfo, defaultKeyword()
                        .withPrice(priceBidSecond)
                        .withPriceContext(priceBidSecond)
                );
        KeywordInfo keywordInfo3 = steps.keywordSteps()
                .createKeyword(adGroupInfo, defaultKeyword()
                        .withPrice(priceBidThird)
                        .withPriceContext(priceBidThird)
                );
        int shard = adGroupInfo.getShard();
        Bid bid1 = new Bid()
                .withId(keywordInfo1.getId())
                .withCampaignId(keywordInfo1.getCampaignId())
                .withPrice(priceBidManualFirst)
                .withPriceContext(priceBidManualFirst);

        Bid bid2 = new Bid()
                .withId(keywordInfo2.getId())
                .withCampaignId(keywordInfo2.getCampaignId())
                .withPrice(priceBidManualSecond)
                .withPriceContext(priceBidManualSecond);

        Bid bid3 = new Bid()
                .withId(0L)
                .withCampaignId(adGroupInfo.getCampaignId())
                .withPrice(BigDecimal.valueOf(1L))
                .withPriceContext(BigDecimal.valueOf(2L));

        TextCampaign campaign = new TextCampaign()
                .withId(adGroupInfo.getCampaignId())
                .withClientId(adGroupInfo.getClientId().asLong())
                .withCurrency(currencyCode);

        bidRepository.insertBidsToBidsManualPrices(shard, asList(bid1, bid2, bid3));
        testingBidService.restoreManualBids(adGroupInfo.getClientInfo().getClientId(),
                CampaignStrategyChangingSettings.create(
                        currencyCode.getCurrency().getMinPrice(),
                        currencyCode.getCurrency().getMaxPrice(),
                        true),
                List.of(campaign));

        List<Bid> bids = bidRepository.getBidsByCampaignIds(shard, singletonList(adGroupInfo.getCampaignId()));

        MatcherAssert.assertThat(bids, hasSize(3));
        MatcherAssert.assertThat(bids.get(0), allOf(
                hasProperty("price", comparesEqualTo(priceBidManualFirst)),
                hasProperty("priceContext", comparesEqualTo(priceBidManualFirst)),
                hasProperty("id", equalTo(keywordInfo1.getId())),
                hasProperty("campaignId", equalTo(keywordInfo1.getCampaignId())),
                hasProperty("adGroupId", equalTo(keywordInfo1.getAdGroupId()))
        ));
        MatcherAssert.assertThat(bids.get(1), allOf(
                hasProperty("price", comparesEqualTo(maxPrice)),
                hasProperty("priceContext", comparesEqualTo(maxPrice)),
                hasProperty("id", equalTo(keywordInfo2.getId())),
                hasProperty("campaignId", equalTo(keywordInfo2.getCampaignId())),
                hasProperty("adGroupId", equalTo(keywordInfo2.getAdGroupId()))
        ));
        MatcherAssert.assertThat(bids.get(2), allOf(
                hasProperty("price", comparesEqualTo(priceBidThird)),
                hasProperty("priceContext", comparesEqualTo(priceBidThird)),
                hasProperty("id", equalTo(keywordInfo3.getId())),
                hasProperty("campaignId", equalTo(keywordInfo3.getCampaignId())),
                hasProperty("adGroupId", equalTo(keywordInfo3.getAdGroupId()))
        ));

        List<Bid> remainedInBidManual = bidRepository.getBidsFromBidsManualPricesByCampaignIds(shard,
                singletonList(adGroupInfo.getCampaignId()));

        MatcherAssert.assertThat(remainedInBidManual, hasSize(0));
    }

    @Test
    public void resetPriceContextTest() {
        CurrencyCode currencyCode = CurrencyCode.EUR;
        BigDecimal priceBidFirst = BigDecimal.valueOf(5);
        BigDecimal priceBidSecond = BigDecimal.valueOf(10);
        KeywordInfo keywordInfo1 = steps.keywordSteps()
                .createKeyword(adGroupInfo, defaultKeyword()
                        .withPrice(priceBidFirst)
                        .withPriceContext(priceBidFirst)
                        .withStatusBsSynced(StatusBsSynced.YES)
                );
        KeywordInfo keywordInfo2 = steps.keywordSteps()
                .createKeyword(adGroupInfo2, defaultKeyword()
                        .withPrice(priceBidSecond)
                        .withPriceContext(priceBidSecond)
                        .withStatusBsSynced(StatusBsSynced.YES)
                );
        int shard = adGroupInfo.getShard();
        int shard2 = adGroupInfo2.getShard();
        Campaign campaign1 = new Campaign()
                .withId(adGroupInfo.getCampaignId())
                .withClientId(adGroupInfo.getClientId().asLong())
                .withCurrency(currencyCode);
        Campaign campaign2 = new Campaign()
                .withId(adGroupInfo2.getCampaignId())
                .withClientId(adGroupInfo2.getClientId().asLong())
                .withCurrency(currencyCode);

        testingBidService.resetPriceContext(0L, asList(
                new TextCampaign().withId(adGroupInfo.getCampaignId()).withCurrency(currencyCode),
                new TextCampaign().withId(adGroupInfo2.getCampaignId()).withCurrency(currencyCode)
        ));

        List<Bid> bidsForCampaign1 = bidRepository.getBidsByCampaignIds(shard, singletonList(campaign1.getId()));
        MatcherAssert.assertThat(bidsForCampaign1, hasSize(1));
        MatcherAssert.assertThat(bidsForCampaign1.get(0), Matchers.allOf(
                Matchers.hasProperty("price", comparesEqualTo(priceBidFirst)),
                Matchers.hasProperty("priceContext", comparesEqualTo(BigDecimal.ZERO)),
                Matchers.hasProperty("id", equalTo(keywordInfo1.getId())),
                Matchers.hasProperty("campaignId", equalTo(keywordInfo1.getCampaignId())),
                Matchers.hasProperty("adGroupId", equalTo(keywordInfo1.getAdGroupId())),
                Matchers.hasProperty("statusBsSynced", equalTo(StatusBsSynced.NO))
        ));
        List<Bid> bidsBaseForCampaign1 = bidRepository.getBidsWithRelevanceMatchByCampaignIds(shard,
                singletonList(campaign1.getId()));
        MatcherAssert.assertThat(bidsForCampaign1, hasSize(1));
        MatcherAssert.assertThat(bidsBaseForCampaign1.get(0), Matchers.allOf(
                Matchers.hasProperty("price", comparesEqualTo(priceBidFirst)),
                Matchers.hasProperty("priceContext", comparesEqualTo(BigDecimal.ZERO)),
                Matchers.hasProperty("id", equalTo(keywordInfo1.getId())),
                Matchers.hasProperty("campaignId", equalTo(keywordInfo1.getCampaignId())),
                Matchers.hasProperty("adGroupId", equalTo(keywordInfo1.getAdGroupId())),
                Matchers.hasProperty("statusBsSynced", equalTo(StatusBsSynced.NO))
        ));

        List<Bid> bidsForCampaign2 = bidRepository.getBidsByCampaignIds(shard2, singletonList(campaign2.getId()));
        MatcherAssert.assertThat(bidsForCampaign2, hasSize(1));
        MatcherAssert.assertThat(bidsForCampaign2.get(0), Matchers.allOf(
                Matchers.hasProperty("price", comparesEqualTo(priceBidSecond)),
                Matchers.hasProperty("priceContext", comparesEqualTo(BigDecimal.ZERO)),
                Matchers.hasProperty("id", equalTo(keywordInfo2.getId())),
                Matchers.hasProperty("campaignId", equalTo(keywordInfo2.getCampaignId())),
                Matchers.hasProperty("adGroupId", equalTo(keywordInfo2.getAdGroupId())),
                Matchers.hasProperty("statusBsSynced", equalTo(StatusBsSynced.NO))
        ));
        List<Bid> bidsBaseForCampaign2 = bidRepository.getBidsWithRelevanceMatchByCampaignIds(shard,
                singletonList(campaign2.getId()));
        MatcherAssert.assertThat(bidsBaseForCampaign2, hasSize(1));
        MatcherAssert.assertThat(bidsBaseForCampaign2.get(0), Matchers.allOf(
                Matchers.hasProperty("price", comparesEqualTo(priceBidSecond)),
                Matchers.hasProperty("priceContext", comparesEqualTo(BigDecimal.ZERO)),
                Matchers.hasProperty("id", equalTo(keywordInfo2.getId())),
                Matchers.hasProperty("campaignId", equalTo(keywordInfo2.getCampaignId())),
                Matchers.hasProperty("adGroupId", equalTo(keywordInfo2.getAdGroupId())),
                Matchers.hasProperty("statusBsSynced", equalTo(StatusBsSynced.NO))
        ));

    }

    private void assertCorrectResultForSingleKeywordIsWrittenToDb(Long keywordId, MassResult<SetBidItem> result,
                                                                  Predicate<BigDecimal> expectedPriceSearchPredicate,
                                                                  Predicate<BigDecimal> expectedPriceContextPredicate) {

        assertThat(result).is(matchedBy(isFullySuccessful()));

        List<Result<SetBidItem>> resultList = result.getResult();
        //проверяем, что правильный результат записался в BIDS
        List<Keyword> keywords =
                keywordService.getKeywords(clientId, singletonList(keywordId));
        assertThat(keywords.size() == resultList.size());

        //проверяем, что из BIDS_BASE не получаем keywords
        List<Bid> resultFromBidsBase = bidRepository
                .getRelevanceMatchByIds(keywordInfo.getShard(), mapList(resultList, t -> t.getResult().getId()));
        assertThat(resultFromBidsBase).isEmpty();

        if (expectedPriceSearchPredicate != null) {
            assertThat(expectedPriceSearchPredicate.test(keywords.get(0).getPrice()));
        }
        if (expectedPriceContextPredicate != null) {
            assertThat(expectedPriceContextPredicate.test(keywords.get(0).getPriceContext()));
        }
    }
}
