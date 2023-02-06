package ru.yandex.direct.core.entity.keyword.service;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import org.assertj.core.api.JUnitSoftAssertions;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.bsauction.BsRequest;
import ru.yandex.direct.bsauction.BsTrafaretClient;
import ru.yandex.direct.core.entity.adgroup.service.MinusKeywordPreparingTool;
import ru.yandex.direct.core.entity.auction.container.BsRequestPhraseWrapper;
import ru.yandex.direct.core.entity.auction.container.bs.Block;
import ru.yandex.direct.core.entity.auction.container.bs.KeywordTrafaretData;
import ru.yandex.direct.core.entity.auction.container.bs.Position;
import ru.yandex.direct.core.entity.campaign.repository.CampaignRepository;
import ru.yandex.direct.core.entity.keyword.model.Keyword;
import ru.yandex.direct.core.entity.keyword.model.Place;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.data.TestGroups;
import ru.yandex.direct.core.testing.data.TestMinusKeywordsPacks;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.KeywordInfo;
import ru.yandex.direct.core.testing.info.MinusKeywordsPackInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.currency.CurrencyCode;
import ru.yandex.direct.currency.Money;
import ru.yandex.direct.currency.currencies.CurrencyRub;
import ru.yandex.direct.dbutil.model.ClientId;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.core.entity.keyword.service.KeywordBsAuctionService.getKeywordPlaces;
import static ru.yandex.direct.core.entity.keyword.service.KeywordTestUtils.getCampaignMap;
import static ru.yandex.direct.core.testing.data.TestBanners.activeTextBanner;
import static ru.yandex.direct.core.testing.mock.BsTrafaretClientMockUtils.setCustomMockOnBsTrafaretClient;
import static ru.yandex.direct.core.testing.mock.BsTrafaretClientMockUtils.setDefaultMockOnBsTrafaretClient;
import static ru.yandex.direct.integrations.configuration.IntegrationsConfiguration.BS_TRAFARET_AUCTION_CLIENT;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class KeywordBsAuctionServiceTest {

    @Rule
    public JUnitSoftAssertions softly = new JUnitSoftAssertions();

    @Autowired
    private Steps steps;

    @Autowired
    @Qualifier(BS_TRAFARET_AUCTION_CLIENT)
    private BsTrafaretClient bsTrafaretClient;

    @Autowired
    private KeywordBsAuctionService keywordBsAuctionService;
    @Autowired
    private MinusKeywordPreparingTool minusKeywordPreparingTool;
    @Autowired
    private CampaignRepository campaignRepository;

    private KeywordInfo keywordInfo;
    private KeywordInfo keywordWithoutBannerInfo;

    @Before
    public void before() {
        keywordInfo = steps.keywordSteps().createDefaultKeyword();
        keywordInfo.getKeyword().withPrice(BigDecimal.ONE).withPriceContext(BigDecimal.ONE);
        steps.bannerSteps().createBanner(activeTextBanner(keywordInfo.getCampaignId(), keywordInfo.getAdGroupId()),
                keywordInfo.getAdGroupInfo());

        keywordWithoutBannerInfo = steps.keywordSteps().createDefaultKeyword();
        setDefaultMockOnBsTrafaretClient(bsTrafaretClient);
    }

    @Test
    public void getBsResults_existingKeyword_success() throws Exception {
        List<KeywordTrafaretData> keywordTrafaretDataList = keywordBsAuctionService
                .getTrafaretAuction(keywordInfo.getAdGroupInfo().getClientId(),
                        singletonList(keywordInfo.getKeyword()),
                        getCampaignMap(campaignRepository, keywordInfo.getKeyword()));
        softly.assertThat(keywordTrafaretDataList).hasSize(1);
        softly.assertThat(keywordTrafaretDataList).doesNotContainNull();
    }

    @Test
    public void getBsResults_existingKeywordWithOnlyAutoBudget_success() throws Exception {
        Keyword keyword = keywordInfo.getKeyword().withPrice(null).withPriceContext(null).withAutobudgetPriority(3);
        List<KeywordTrafaretData> keywordTrafaretDataList = keywordBsAuctionService
                .getTrafaretAuction(keywordInfo.getAdGroupInfo().getClientId(), singletonList(keyword),
                        getCampaignMap(campaignRepository, keyword));
        softly.assertThat(keywordTrafaretDataList).hasSize(1);
        softly.assertThat(keywordTrafaretDataList).doesNotContainNull();
    }

    @Test
    public void getBsResults_newKeywordWithoutPhraseId_success() throws Exception {
        KeywordInfo keywordInfo2 = steps.keywordSteps().createKeyword(keywordInfo.getAdGroupInfo());
        Keyword keyword1 = keywordInfo.getKeyword().withId(null).withPhraseBsId(BigInteger.ZERO);
        Keyword keyword2 = keywordInfo2.getKeyword()
                .withId(null).withPhraseBsId(BigInteger.ZERO)
                .withPrice(BigDecimal.ONE).withPriceContext(BigDecimal.ONE);
        List<KeywordTrafaretData> keywordTrafaretDataList = keywordBsAuctionService
                .getTrafaretAuction(keywordInfo.getAdGroupInfo().getClientId(), asList(keyword1, keyword2),
                        getCampaignMap(campaignRepository, keyword1, keyword2));
        softly.assertThat(keywordTrafaretDataList).hasSize(2);
        softly.assertThat(keywordTrafaretDataList).doesNotContainNull();
    }

    @Test
    public void getBsResults_newKeywordWithPhraseId_success() throws Exception {
        Keyword keyword = keywordInfo.getKeyword().withId(null).withPhraseBsId(BigInteger.ONE);
        List<KeywordTrafaretData> keywordTrafaretDataList = keywordBsAuctionService
                .getTrafaretAuction(keywordInfo.getAdGroupInfo().getClientId(), singletonList(keyword),
                        getCampaignMap(campaignRepository, keyword));
        softly.assertThat(keywordTrafaretDataList).hasSize(1);
        softly.assertThat(keywordTrafaretDataList).doesNotContainNull();
    }

    @Test
    public void getBsResults_notCallBsAuction_whenKeywordHasNoBanner() throws Exception {
        List<KeywordTrafaretData> keywordTrafaretDataList = keywordBsAuctionService
                .getTrafaretAuction(keywordWithoutBannerInfo.getAdGroupInfo().getClientId(),
                        singletonList(keywordWithoutBannerInfo.getKeyword()),
                        getCampaignMap(campaignRepository, keywordWithoutBannerInfo.getKeyword()));

        assertThat("ответ должен быть пустым", keywordTrafaretDataList, empty());
    }

    @Test
    public void convertAuctionDataToPlaceMap_EmptyList_ResultIsEmpty() {
        IdentityHashMap<Keyword, Place> map = getKeywordPlaces(emptyList(), CurrencyRub.getInstance());
        assertThat(map, beanDiffer(emptyMap()));
    }

    @Test
    public void convertAuctionDataToPlaceMap_PriceIsNull_ResultIsEmpty() {
        ClientId clientId = keywordInfo.getAdGroupInfo().getClientId();
        Keyword keyword = keywordInfo.getKeyword();
        keyword.withPrice(null);
        Map<Keyword, KeywordTrafaretData> trafaretDataMap = keywordBsAuctionService
                .getTrafaretAuctionMapSafe(clientId, singletonList(keyword),
                        getCampaignMap(campaignRepository, keyword));

        IdentityHashMap<Keyword, Place> map = getKeywordPlaces(trafaretDataMap.values(), CurrencyRub.getInstance());
        assertThat(map, beanDiffer(emptyMap()));
    }

    @Test
    public void convertAuctionDataToPlaceMap_PriceNotNull_ResultContainsPlace() {
        ClientId clientId = keywordInfo.getAdGroupInfo().getClientId();
        Keyword keyword = keywordInfo.getKeyword();
        setCustomMockOnBsTrafaretClient(bsTrafaretClient, BigDecimal.valueOf(5), BigDecimal.valueOf(10));

        Map<Keyword, KeywordTrafaretData> trafaretDataMap = keywordBsAuctionService
                .getTrafaretAuctionMapSafe(clientId, singletonList(keyword),
                        getCampaignMap(campaignRepository, keyword));
        // 10 рублей - наощупь подобранная ставка, которая попадает на позицию PREMIUM2
        keyword.withPrice(BigDecimal.valueOf(10));

        IdentityHashMap<Keyword, Place> map = getKeywordPlaces(trafaretDataMap.values(), CurrencyRub.getInstance());
        assertThat(map, beanDiffer(singletonMap(keyword, Place.PREMIUM2)));
    }

    /**
     * {@link KeywordBsAuctionService#getTrafaretAuctionMapSafe(ClientId, List)}
     * возвращает то же самое, что
     * {@link KeywordBsAuctionService#getTrafaretAuction(ClientId, List)},
     * только в виде мапы.
     */
    @Test
    public void getTrafaretAuctionMap_GetCorrectMap() {
        ClientId clientId = keywordInfo.getAdGroupInfo().getClientId();
        Keyword keyword = keywordInfo.getKeyword();

        List<KeywordTrafaretData> keywordTrafaretDataList = keywordBsAuctionService
                .getTrafaretAuction(clientId, singletonList(keyword), getCampaignMap(campaignRepository, keyword));
        Map<Keyword, KeywordTrafaretData> map = keywordBsAuctionService
                .getTrafaretAuctionMapSafe(clientId, singletonList(keyword),
                        getCampaignMap(campaignRepository, keyword));

        assertThat(map, beanDiffer(singletonMap(keyword, keywordTrafaretDataList.get(0))));
    }

    @Test
    public void getTrafaretAuction_AdGroupWithLibraryMinusKeywords_MinusKeywordsMerged() {
        ClientInfo clientInfo = steps.clientSteps().createDefaultClient();
        MinusKeywordsPackInfo libraryMinusKeywordsPack =
                steps.minusKeywordsPackSteps().createMinusKeywordsPack(TestMinusKeywordsPacks.libraryMinusKeywordsPack()
                        .withMinusKeywords(singletonList("word 2")), clientInfo);
        MinusKeywordsPackInfo privateMinusKeywordsPack =
                steps.minusKeywordsPackSteps().createMinusKeywordsPack(TestMinusKeywordsPacks.libraryMinusKeywordsPack()
                        .withMinusKeywords(singletonList("word 1")), clientInfo);

        AdGroupInfo adGroupWithLibraryMinusKeywords = steps.adGroupSteps().createAdGroup(TestGroups.activeTextAdGroup()
                        .withMinusKeywordsId(privateMinusKeywordsPack.getMinusKeywordPackId())
                        .withMinusKeywords(privateMinusKeywordsPack.getMinusKeywordsPack().getMinusKeywords())
                        .withLibraryMinusKeywordsIds(singletonList(libraryMinusKeywordsPack.getMinusKeywordPackId())),
                clientInfo);
        steps.bannerSteps().createActiveTextBanner(adGroupWithLibraryMinusKeywords);
        KeywordInfo adGroupKeyword = steps.keywordSteps().createKeyword(adGroupWithLibraryMinusKeywords);

        List<String> allMinusKeywords = minusKeywordPreparingTool
                .mergePrivateAndLibrary(privateMinusKeywordsPack.getMinusKeywordsPack().getMinusKeywords(),
                        singletonList(libraryMinusKeywordsPack.getMinusKeywordsPack().getMinusKeywords()));

        keywordBsAuctionService
                .getTrafaretAuction(clientInfo.getClientId(), singletonList(adGroupKeyword.getKeyword()),
                        getCampaignMap(campaignRepository, adGroupKeyword.getKeyword()));
        ArgumentCaptor<List<BsRequest<BsRequestPhraseWrapper>>> bsRequestCaptor =
                ArgumentCaptor.forClass(List.class);
        verify(bsTrafaretClient, atLeastOnce()).getAuctionResultsWithPositionCtrCorrection(bsRequestCaptor.capture());

        List<BsRequest<BsRequestPhraseWrapper>> bsClientArgument = bsRequestCaptor.getValue();
        assertThat(bsClientArgument, hasSize(1));
        assertThat(bsClientArgument.get(0).getPhrases(), hasSize(1));
        assertThat(bsClientArgument.get(0).getPhrases().get(0).getAdGroupForAuction().getAdGroup().getMinusKeywords(),
                contains(allMinusKeywords.toArray()));
    }

    private Block buildBlock() {
        return new Block(asList(
                buildPosition(440, 440),
                buildPosition(330, 330),
                buildPosition(220, 220),
                buildPosition(110, 110)));
    }

    private Position buildPosition(int price, int amnesty) {
        return new Position(Money.valueOf(amnesty, CurrencyCode.YND_FIXED),
                Money.valueOf(price, CurrencyCode.YND_FIXED));
    }

}
