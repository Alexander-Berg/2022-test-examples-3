package ru.yandex.direct.web.entity.adgroup.controller;

import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.bsauction.BsRequest;
import ru.yandex.direct.bsauction.BsRequestPhrase;
import ru.yandex.direct.bsauction.BsTrafaretClient;
import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.auction.container.bs.KeywordBidBsAuctionData;
import ru.yandex.direct.core.entity.banner.model.old.OldBanner;
import ru.yandex.direct.core.entity.banner.model.old.OldBannerWithBody;
import ru.yandex.direct.core.entity.banner.model.old.OldBannerWithTitle;
import ru.yandex.direct.core.entity.campaign.model.Campaign;
import ru.yandex.direct.core.entity.campaign.model.CampaignsAutobudget;
import ru.yandex.direct.core.entity.campaign.model.CampaignsPlatform;
import ru.yandex.direct.core.entity.campaign.model.DbStrategy;
import ru.yandex.direct.core.entity.campaign.model.DbStrategyBase;
import ru.yandex.direct.core.entity.campaign.model.StrategyName;
import ru.yandex.direct.core.entity.keyword.model.Keyword;
import ru.yandex.direct.core.entity.keyword.service.KeywordBsAuctionService;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.currency.Currency;
import ru.yandex.direct.currency.Money;
import ru.yandex.direct.dbschema.ppc.enums.CampOptionsStrategy;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.web.configuration.DirectWebTest;
import ru.yandex.direct.web.core.model.WebResponse;
import ru.yandex.direct.web.data.TestWebAdGroupBuilder;
import ru.yandex.direct.web.entity.adgroup.model.WebTextAdGroup;
import ru.yandex.direct.web.entity.banner.model.WebBanner;
import ru.yandex.direct.web.entity.keyword.model.WebKeyword;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.core.entity.keyword.service.KeywordAutoPricesCalculator.AUTOBROKER_MULTIPLIER;
import static ru.yandex.direct.core.entity.keyword.service.KeywordTestUtils.getCampaignMap;
import static ru.yandex.direct.dbschema.ppc.Tables.CAMP_OPTIONS;
import static ru.yandex.direct.integrations.configuration.IntegrationsConfiguration.BS_TRAFARET_AUCTION_CLIENT_WEB;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;
import static ru.yandex.direct.web.testing.data.TestKeywords.randomPhraseKeyword;

@DirectWebTest
@RunWith(SpringRunner.class)
public class AdGroupControllerUpdateKeywordsPricesTest extends TextAdGroupControllerTestBase {

    @Autowired
    @Qualifier(BS_TRAFARET_AUCTION_CLIENT_WEB)
    private BsTrafaretClient bsTrafaretClient;

    @Autowired
    private KeywordBsAuctionService keywordBsAuctionService;

    @Autowired
    private DslContextProvider dslContextProvider;

    @Before
    public void before() {
        super.before();
        when(bsTrafaretClient.getAuctionResultsWithPositionCtrCorrection(anyList())).thenAnswer(
                invocation -> {
                    List<BsRequest<BsRequestPhrase>> requests = invocation.getArgument(0);
                    return generateDefaultBsAuctionResponse(requests);
                }
        );
    }

    @After
    public void after() {
    }

    @Test
    public void update_AdGroupWithCommonPriceAddNewKeyword_CommonPriceIsSet() {
        AdGroupInfo adGroupInfo = steps.adGroupSteps().createActiveTextAdGroup(campaignInfo);

        double commonPrice = 12.0;
        WebTextAdGroup requestAdGroup = webAdGroupFromExisting(adGroupInfo.getAdGroupId());
        requestAdGroup.withGeneralPrice(commonPrice);
        WebKeyword requestKeyword = randomPhraseKeyword(null)
                .withPrice(null)
                .withPriceContext(null);
        requestAdGroup.withKeywords(singletonList(requestKeyword));

        updateAndCheckResult(singletonList(requestAdGroup));

        List<Keyword> keywords = findKeywords();
        assertThat("в группе должна быть одна фраза", keywords, hasSize(1));
        assertThat("цена фразы на поиске не совпадает с ожидаемой",
                moneyOf(keywords.get(0).getPrice()), equalTo(moneyOf(commonPrice)));
        assertThat("цена фразы в сети не совпадает с ожидаемой",
                moneyOf(keywords.get(0).getPriceContext()), equalTo(moneyOf(commonPrice)));
    }

    @Test
    public void update_AdGroupWithCommonPriceUpdateOldKeyword_CommonPriceIsSet() {
        double commonPrice = 12.0;
        String oldPhrase = "куплю бетон";
        String updatedPhrase = "продам кирпич";

        WebTextAdGroup webTextAdGroup = TestWebAdGroupBuilder.someWebAdGroup(campaignInfo.getCampaignId())
                .withSomeBanner()
                .withSomeKeyword(kw -> kw.withPhrase(oldPhrase).withPrice(123.0).withPriceContext(456.0))
                .withSomeKeyword(kw -> kw.withPrice(124.0).withPriceContext(457.0))
                .build();
        addAndCheckResult(singletonList(webTextAdGroup));
        List<AdGroup> addedAdGroups = findAdGroups();
        WebTextAdGroup requestAdGroup = webAdGroupFromExisting(addedAdGroups.get(0).getId());
        requestAdGroup.withGeneralPrice(commonPrice);
        requestAdGroup.getKeywords().stream().filter(kw -> kw.getPhrase().equals(oldPhrase))
                .collect(Collectors.toList()).get(0).withPhrase(updatedPhrase).withPrice(null).withPriceContext(null);

        updateAndCheckResult(singletonList(requestAdGroup));

        List<Keyword> keywords = findKeywords();
        assertThat("в группе должно быть две фразы", keywords, hasSize(2));
        Keyword updatedKeyword = keywords.stream().filter(kw -> kw.getPhrase().equals(updatedPhrase))
                .collect(Collectors.toList()).get(0);
        assertThat("цена фразы на поиске не совпадает с ожидаемой",
                moneyOf(updatedKeyword.getPrice()), equalTo(moneyOf(commonPrice)));
        assertThat("цена фразы в сети не совпадает с ожидаемой",
                moneyOf(updatedKeyword.getPriceContext()), equalTo(moneyOf(commonPrice)));
    }

    @Test
    public void update_AdGroupWithDifferentPrices_AutoPriceIsSet() {
        WebTextAdGroup webTextAdGroup = TestWebAdGroupBuilder.someWebAdGroup(campaignInfo.getCampaignId())
                .withSomeBanner()
                .withSomeKeyword(kw -> kw.withPrice(123.0).withPriceContext(456.0))
                .withSomeKeyword(kw -> kw.withPrice(124.0).withPriceContext(457.0))
                .build();
        addAndCheckResult(singletonList(webTextAdGroup));
        List<AdGroup> addedAdGroups = findAdGroups();
        WebTextAdGroup requestAdGroup = webAdGroupFromExisting(addedAdGroups.get(0).getId());
        long maxOldKeywordId = requestAdGroup.getKeywords().stream()
                .map(WebKeyword::getId)
                .max(Comparator.naturalOrder()).orElseThrow(NullPointerException::new);
        WebKeyword newWebKeyword = randomPhraseKeyword(null)
                .withPrice(null)
                .withPriceContext(null);
        requestAdGroup.getKeywords().add(newWebKeyword);

        updateAndCheckResult(singletonList(requestAdGroup));

        List<Keyword> keywords = findKeywords();
        assertThat("в группе должно быть три фразы", keywords, hasSize(3));
        List<Keyword> newKeywords =
                keywords.stream().filter(kw -> kw.getId() > maxOldKeywordId).collect(Collectors.toList());
        assertThat("должна была добавиться одна фраза", newKeywords, hasSize(1));
        Keyword newKeyword = newKeywords.get(0);
        Money expectedPrice = getFirstGuaranteePrice(newKeyword)
                .multiply(AUTOBROKER_MULTIPLIER).roundToAuctionStepUp();
        assertThat("цена фразы на поиске не совпадает с ожидаемой",
                moneyOf(newKeyword.getPrice()), equalTo(expectedPrice));
        assertThat("цена фразы в сети не совпадает с ожидаемой",
                moneyOf(newKeyword.getPriceContext()), equalTo(moneyOf(clientCurrency.getDefaultPrice())));
    }

    @Test
    public void update_AdGroupWithSamePricesInSearchStrategy_AutoPriceIsSet() {
        double samePrice = 123.0;
        Campaign campaign =
                campaignRepository.getCampaignsWithStrategy(shard, singletonList(campaignInfo.getCampaignId())).get(0);
        DbStrategyBase manualSearchStrategy = new DbStrategy().withPlatform(CampaignsPlatform.SEARCH)
                .withAutobudget(CampaignsAutobudget.NO).withStrategyName(StrategyName.DEFAULT_);
        ModelChanges<Campaign> mc = new ModelChanges<>(campaign.getId(), Campaign.class);
        mc.process((DbStrategy) manualSearchStrategy, Campaign.STRATEGY);
        campaignRepository.updateCampaigns(shard, singletonList(mc.applyTo(campaign)));
        dslContextProvider.ppc(shard)
                .update(CAMP_OPTIONS)
                .set(CAMP_OPTIONS.STRATEGY, (CampOptionsStrategy) null)
                .where(CAMP_OPTIONS.CID.eq(campaign.getId()))
                .execute();
        WebTextAdGroup webTextAdGroup = TestWebAdGroupBuilder.someWebAdGroup(campaignInfo.getCampaignId())
                .withSomeBanner()
                .withSomeKeyword(kw -> kw.withPrice(samePrice).withPriceContext(samePrice))
                .withSomeKeyword(kw -> kw.withPrice(samePrice).withPriceContext(samePrice))
                .build();
        addAndCheckResult(singletonList(webTextAdGroup));
        List<AdGroup> addedAdGroups = findAdGroups();
        WebTextAdGroup requestAdGroup = webAdGroupFromExisting(addedAdGroups.get(0).getId());
        long maxOldKeywordId = requestAdGroup.getKeywords().stream()
                .map(WebKeyword::getId)
                .max(Comparator.naturalOrder()).orElseThrow(NullPointerException::new);
        WebKeyword newWebKeyword = randomPhraseKeyword(null)
                .withPrice(null)
                .withPriceContext(null);
        requestAdGroup.getKeywords().add(newWebKeyword);

        updateAndCheckResult(singletonList(requestAdGroup));

        List<Keyword> keywords = findKeywords();
        assertThat("в группе должно быть три фразы", keywords, hasSize(3));
        List<Keyword> newKeywords =
                keywords.stream().filter(kw -> kw.getId() > maxOldKeywordId).collect(Collectors.toList());
        assertThat("должна была добавиться одна фраза", newKeywords, hasSize(1));
        Keyword newKeyword = newKeywords.get(0);
        assertThat("цена фразы на поиске не совпадает с ожидаемой",
                moneyOf(newKeyword.getPrice()), equalTo(moneyOf(samePrice)));
    }

    @Test
    public void update_AdGroupAddOldPhrase_OldPricesAreSet() {
        String oldPhrase1 = "купю бетон";
        double oldPrice1 = 123.0;
        double oldPriceContext1 = 456.0;
        String oldPhrase2 = "продам кирпичи";
        double oldPrice2 = 124.0;
        double oldPriceContext2 = 457.0;
        String replaceOldPhrase1 = "куплю картон";
        WebTextAdGroup webTextAdGroup = TestWebAdGroupBuilder.someWebAdGroup(campaignInfo.getCampaignId())
                .withSomeBanner()
                .withSomeKeyword(
                        kw -> kw.withPhrase(oldPhrase1).withPrice(oldPrice1).withPriceContext(oldPriceContext1))
                .withSomeKeyword(
                        kw -> kw.withPhrase(oldPhrase2).withPrice(oldPrice2).withPriceContext(oldPriceContext2))
                .build();
        addAndCheckResult(singletonList(webTextAdGroup));
        List<AdGroup> addedAdGroups = findAdGroups();
        WebTextAdGroup requestAdGroup = webAdGroupFromExisting(addedAdGroups.get(0).getId());
        long maxOldKeywordId = requestAdGroup.getKeywords().stream()
                .map(WebKeyword::getId)
                .max(Comparator.naturalOrder()).orElseThrow(NullPointerException::new);
        requestAdGroup.getKeywords().stream().filter(kw -> kw.getPhrase().equals(oldPhrase1))
                .collect(Collectors.toList()).get(0).withPhrase(replaceOldPhrase1);
        WebKeyword newWebKeyword = randomPhraseKeyword(null)
                .withPhrase(oldPhrase1)
                .withPrice(null)
                .withPriceContext(null);
        requestAdGroup.getKeywords().add(newWebKeyword);

        updateAndCheckResult(singletonList(requestAdGroup));

        List<Keyword> keywords = findKeywords();
        assertThat("в группе должно быть три фразы", keywords, hasSize(3));
        List<Keyword> newKeywords =
                keywords.stream().filter(kw -> kw.getId() > maxOldKeywordId).collect(Collectors.toList());
        assertThat("должна была добавиться одна фраза", newKeywords, hasSize(1));
        Keyword newKeyword = newKeywords.get(0);
        assertThat("цена фразы на поиске не совпадает с ожидаемой",
                moneyOf(newKeyword.getPrice()), equalTo(moneyOf(oldPrice1)));
        assertThat("цена фразы в сети не совпадает с ожидаемой",
                moneyOf(newKeyword.getPriceContext()), equalTo(moneyOf(oldPriceContext1)));
    }

    private void addAndCheckResult(List<WebTextAdGroup> requestAdGroups) {
        WebResponse response = controller.saveTextAdGroup(requestAdGroups, campaignInfo.getCampaignId(),
                true, false, false, null, null);
        checkResponse(response);
    }

    private WebTextAdGroup webAdGroupFromExisting(long adGroupId) {
        List<AdGroup> coreAdGroups = adGroupRepository.getAdGroups(shard, Collections.singletonList(adGroupId));
        AdGroup coreAdGroup = coreAdGroups.get(0);

        List<OldBanner> coreAdGroupBanners =
                bannerRepository.getBannersByAdGroups(shard, singletonList(adGroupId)).get(adGroupId);
        List<WebBanner> webBanners =
                mapList(coreAdGroupBanners, AdGroupControllerUpdateKeywordsPricesTest::webBannerFromCoreBanner);

        List<Keyword> coreAdGroupKeywords = keywordRepository.getKeywordsByAdGroupId(shard, adGroupId);
        List<WebKeyword> webKeywords =
                mapList(coreAdGroupKeywords, AdGroupControllerUpdateKeywordsPricesTest::webKeywordFromCoreKeyword);

        return new WebTextAdGroup()
                .withId(adGroupId)
                .withCampaignId(coreAdGroup.getCampaignId())
                .withName(coreAdGroup.getName())
                .withMinusKeywords(coreAdGroup.getMinusKeywords())
                .withGeo(coreAdGroup.getGeo().stream().map(Object::toString).collect(Collectors.joining(",")))
                .withBanners(webBanners)
                .withKeywords(webKeywords)
                //.withTags(adGroup.getTags())
                ;
    }

    private static WebBanner webBannerFromCoreBanner(OldBanner coreBanner) {
        String href = coreBanner.getHref();
        Pattern p = Pattern.compile("^(https?://)(.*)$");
        Matcher m = p.matcher(href);
        checkState(m.find());
        return new WebBanner()
                .withId(coreBanner.getId())
                .withTitle(((OldBannerWithTitle) coreBanner).getTitle())
                .withBody(((OldBannerWithBody) coreBanner).getBody())
                .withUrlProtocol(m.group(1))
                .withHref(m.group(2))
                .withAdType("text");
    }

    private static WebKeyword webKeywordFromCoreKeyword(Keyword coreKeyword) {
        return new WebKeyword()
                .withId(coreKeyword.getId())
                .withPhrase(coreKeyword.getPhrase())
//                .withPrice(coreKeyword.getPrice().doubleValue())
//                .withPriceContext(coreKeyword.getPriceContext().doubleValue())
                .withAutobudgetPriority(coreKeyword.getAutobudgetPriority());
    }

    private Money getFirstGuaranteePrice(Keyword kw) {
        Currency currency = clientService.getWorkCurrency(clientId);
        IdentityHashMap<Keyword, KeywordBidBsAuctionData> auctionResult =
                keywordBsAuctionService.getBsAuctionData(clientId, singletonList(kw), currency,
                        getCampaignMap(campaignRepository, kw));
        return auctionResult.get(kw).getGuarantee().first().getBidPrice();
    }

}
