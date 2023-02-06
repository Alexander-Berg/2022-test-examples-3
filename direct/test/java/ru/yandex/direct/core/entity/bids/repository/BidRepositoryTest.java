package ru.yandex.direct.core.entity.bids.repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.hamcrest.Matchers;
import org.jooq.Configuration;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.StatusBsSynced;
import ru.yandex.direct.core.entity.bids.container.ShowConditionType;
import ru.yandex.direct.core.entity.bids.model.Bid;
import ru.yandex.direct.core.entity.keyword.repository.KeywordRepository;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.KeywordInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static ru.yandex.direct.core.entity.bids.repository.BidRepository.HEAVY_CAMP_BIDS_BORDER;
import static ru.yandex.direct.core.testing.data.TestKeywords.defaultKeyword;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class BidRepositoryTest {

    @Autowired
    private DslContextProvider dslContextProvider;

    @Autowired
    private Steps steps;

    @Autowired
    private BidRepository bidRepository;

    @Autowired
    private KeywordRepository keywordRepository;

    @Autowired
    private ShardHelper shardHelper;

    private AdGroupInfo adGroupInfo;
    private KeywordInfo keywordInfo1;
    private KeywordInfo keywordInfo2;
    private BigDecimal priceFirst;
    private BigDecimal priceSecond;
    private BigDecimal finalPriceFirst;
    private BigDecimal finalPriceSecond;
    private Bid bid1;
    private Bid bid2;

    @Before
    public void before() {
        adGroupInfo = steps.adGroupSteps().createDefaultAdGroup();

        priceFirst = BigDecimal.valueOf(5);
        priceSecond = BigDecimal.valueOf(10);
        finalPriceFirst = BigDecimal.valueOf(20);
        finalPriceSecond = BigDecimal.valueOf(30);

        keywordInfo1 = steps.keywordSteps()
                .createKeyword(adGroupInfo, defaultKeyword()
                        .withPrice(priceFirst)
                        .withPriceContext(priceFirst)
                        .withStatusBsSynced(StatusBsSynced.YES)
                );
        keywordInfo2 = steps.keywordSteps()
                .createKeyword(adGroupInfo, defaultKeyword()
                        .withPrice(priceSecond)
                        .withPriceContext(priceSecond)
                        .withStatusBsSynced(StatusBsSynced.YES)
                );

        bid1 = new Bid()
                .withId(keywordInfo1.getId())
                .withCampaignId(keywordInfo1.getCampaignId())
                .withPrice(finalPriceFirst)
                .withPriceContext(finalPriceFirst);

        bid2 = new Bid()
                .withId(keywordInfo2.getId())
                .withCampaignId(keywordInfo2.getCampaignId())
                .withPrice(finalPriceSecond)
                .withPriceContext(finalPriceSecond);
    }

    @Test
    public void addBids_AddKeywordToBidsBase() {
        Bid bid = getBidWithNoDefaults();
        Configuration conf = dslContextProvider.ppc(adGroupInfo.getShard()).configuration();
        bidRepository.addBids(conf.dsl(), singletonList(bid));
        List<Bid> bidsFromDB = bidRepository.getRelevanceMatchByIds(adGroupInfo.getShard(), singletonList(bid.getId()));
        assertThat("по добавленному keyword id ничего не получаем из bids_base", bidsFromDB, empty());
    }

    @Test
    public void copyFromBidsToBidsManualPricesTest() {
        int shard = adGroupInfo.getShard();
        List<Bid> bids = bidRepository.getBidsByCampaignIds(shard, singletonList(adGroupInfo.getCampaignId()));
        assertThat("Должны были добавиться 2 ключевые фразы", bids, hasSize(2));
        assertThat(bids.get(0), Matchers.allOf(
                Matchers.hasProperty("price", comparesEqualTo(priceFirst)),
                Matchers.hasProperty("priceContext", comparesEqualTo(priceFirst)),
                Matchers.hasProperty("id", equalTo(keywordInfo1.getId())),
                Matchers.hasProperty("campaignId", equalTo(keywordInfo1.getCampaignId())),
                Matchers.hasProperty("adGroupId", equalTo(keywordInfo1.getAdGroupId()))
        ));
        assertThat(bids.get(1), Matchers.allOf(
                Matchers.hasProperty("price", comparesEqualTo(priceSecond)),
                Matchers.hasProperty("priceContext", comparesEqualTo(priceSecond)),
                Matchers.hasProperty("id", equalTo(keywordInfo2.getId())),
                Matchers.hasProperty("campaignId", equalTo(keywordInfo2.getCampaignId())),
                Matchers.hasProperty("adGroupId", equalTo(keywordInfo2.getAdGroupId()))
        ));
        bidRepository.insertBidsToBidsManualPrices(shard, singletonList(bid1));

        List<Bid> bidsFromBidsManualPricesBeforeCopy = bidRepository.getBidsFromBidsManualPricesByCampaignIds(shard,
                singletonList(adGroupInfo.getCampaignId()));

        assertThat(bidsFromBidsManualPricesBeforeCopy, hasSize(1));

        bidRepository.copyFromBidsToBidsManualPricesForCampaignIds(shard, singletonList(adGroupInfo.getCampaignId()));

        List<Bid> bidsFromBidsManualPrices = bidRepository.getBidsFromBidsManualPricesByCampaignIds(shard,
                singletonList(adGroupInfo.getCampaignId()));

        assertThat(bidsFromBidsManualPrices, hasSize(2));
        assertThat(bidsFromBidsManualPrices.get(0), Matchers.allOf(
                Matchers.hasProperty("price", comparesEqualTo(priceFirst)),
                Matchers.hasProperty("priceContext", comparesEqualTo(priceFirst)),
                Matchers.hasProperty("id", equalTo(keywordInfo1.getId())),
                Matchers.hasProperty("campaignId", equalTo(keywordInfo1.getCampaignId()))
        ));
        assertThat(bidsFromBidsManualPrices.get(1), Matchers.allOf(
                Matchers.hasProperty("price", comparesEqualTo(priceSecond)),
                Matchers.hasProperty("priceContext", comparesEqualTo(priceSecond)),
                Matchers.hasProperty("id", equalTo(keywordInfo2.getId())),
                Matchers.hasProperty("campaignId", equalTo(keywordInfo2.getCampaignId()))
        ));
    }

    @Test
    public void getBidsManualPricesForCampaignTest() {
        int shard = adGroupInfo.getShard();

        bidRepository.insertBidsToBidsManualPrices(shard, asList(bid1, bid2));

        List<Bid> bids = bidRepository.getBidsManualPricesForCampaignIds(shard,
                singletonList(adGroupInfo.getCampaignId()));

        assertThat(bids, hasSize(2));
        assertThat(bids.get(0), Matchers.allOf(
                Matchers.hasProperty("price", comparesEqualTo(finalPriceFirst)),
                Matchers.hasProperty("priceContext", comparesEqualTo(finalPriceFirst)),
                Matchers.hasProperty("id", equalTo(keywordInfo1.getId())),
                Matchers.hasProperty("campaignId", equalTo(keywordInfo1.getCampaignId()))
        ));
        assertThat(bids.get(1), Matchers.allOf(
                Matchers.hasProperty("price", comparesEqualTo(finalPriceSecond)),
                Matchers.hasProperty("priceContext", comparesEqualTo(finalPriceSecond)),
                Matchers.hasProperty("id", equalTo(keywordInfo2.getId())),
                Matchers.hasProperty("campaignId", equalTo(keywordInfo2.getCampaignId()))
        ));
    }

    @Test
    public void deleteBidManualPricesForCampaignTest() {
        int shard = adGroupInfo.getShard();
        List<Bid> bidsManualPrices = bidRepository.getBidsManualPricesForCampaignIds(shard,
                singletonList(adGroupInfo.getCampaignId()));

        List<Bid> bids = bidRepository.getBidsByCampaignIds(shard,
                singletonList(adGroupInfo.getCampaignId()));

        assertThat(bidsManualPrices, hasSize(0));
        assertThat(bids, hasSize(2));

        bidRepository.insertBidsToBidsManualPrices(shard, asList(bid1, bid2));

        List<Bid> bidsManualPricesAfterInsert = bidRepository.getBidsManualPricesForCampaignIds(shard,
                singletonList(adGroupInfo.getCampaignId()));

        assertThat(bidsManualPricesAfterInsert, hasSize(2));

        bidRepository.deleteBidManualPricesForCampaignIds(shard, singletonList(adGroupInfo.getCampaignId()));

        List<Bid> bidsManualPricesAfterDelete = bidRepository.getBidsManualPricesForCampaignIds(shard,
                singletonList(adGroupInfo.getCampaignId()));

        assertThat(bidsManualPricesAfterDelete, hasSize(0));

    }

    @Test
    public void resetPriceContextToZeroForBidsByCampaignIdsTest() {
        int shard = adGroupInfo.getShard();
        List<Bid> bids = bidRepository.getBidsByCampaignIds(shard, singletonList(adGroupInfo.getCampaignId()));

        assertThat(bids, hasSize(2));

        assertThat(bids.get(0), Matchers.allOf(
                Matchers.hasProperty("price", comparesEqualTo(priceFirst)),
                Matchers.hasProperty("priceContext", comparesEqualTo(priceFirst)),
                Matchers.hasProperty("id", equalTo(keywordInfo1.getId())),
                Matchers.hasProperty("campaignId", equalTo(keywordInfo1.getCampaignId())),
                Matchers.hasProperty("adGroupId", equalTo(keywordInfo1.getAdGroupId())),
                Matchers.hasProperty("statusBsSynced", equalTo(StatusBsSynced.YES))
        ));
        assertThat(bids.get(1), Matchers.allOf(
                Matchers.hasProperty("price", comparesEqualTo(priceSecond)),
                Matchers.hasProperty("priceContext", comparesEqualTo(priceSecond)),
                Matchers.hasProperty("id", equalTo(keywordInfo2.getId())),
                Matchers.hasProperty("campaignId", equalTo(keywordInfo2.getCampaignId())),
                Matchers.hasProperty("adGroupId", equalTo(keywordInfo2.getAdGroupId())),
                Matchers.hasProperty("statusBsSynced", equalTo(StatusBsSynced.YES))
        ));

        bidRepository.resetPriceContextToZeroForBidsByCampaignIds(shard, singletonList(adGroupInfo.getCampaignId()));

        List<Bid> bidsAfterReset = bidRepository.getBidsByCampaignIds(shard,
                singletonList(adGroupInfo.getCampaignId()));

        assertThat(bidsAfterReset.get(0), Matchers.allOf(
                Matchers.hasProperty("price", comparesEqualTo(priceFirst)),
                Matchers.hasProperty("priceContext", comparesEqualTo(BigDecimal.ZERO)),
                Matchers.hasProperty("id", equalTo(keywordInfo1.getId())),
                Matchers.hasProperty("campaignId", equalTo(keywordInfo1.getCampaignId())),
                Matchers.hasProperty("adGroupId", equalTo(keywordInfo1.getAdGroupId())),
                Matchers.hasProperty("statusBsSynced", equalTo(StatusBsSynced.NO))
        ));
        assertThat(bidsAfterReset.get(1), Matchers.allOf(
                Matchers.hasProperty("price", comparesEqualTo(priceSecond)),
                Matchers.hasProperty("priceContext", comparesEqualTo(BigDecimal.ZERO)),
                Matchers.hasProperty("id", equalTo(keywordInfo2.getId())),
                Matchers.hasProperty("campaignId", equalTo(keywordInfo2.getCampaignId())),
                Matchers.hasProperty("adGroupId", equalTo(keywordInfo2.getAdGroupId())),
                Matchers.hasProperty("statusBsSynced", equalTo(StatusBsSynced.NO))
        ));
    }

    private Bid getBidWithNoDefaults() {
        return new Bid()
                .withId(shardHelper.generatePhraseIds(1).get(0))
                .withType(ShowConditionType.KEYWORD)
                .withCampaignId(adGroupInfo.getCampaignId())
                .withAdGroupId(adGroupInfo.getAdGroupId())
                .withAutobudgetPriority(3)
                .withPrice(new BigDecimal("100.00"))
                .withPriceContext(new BigDecimal("120.00"))
                .withStatusBsSynced(StatusBsSynced.YES)
                .withIsSuspended(true)
                .withIsDeleted(true)
                .withLastChange(LocalDateTime.now().withNano(0));
    }

    @Test
    public void getHeavyCampaignIdsTest() {
        int halfLimit = HEAVY_CAMP_BIDS_BORDER / 2 + 1;
        var dummyKeywords1 = Stream.generate(() -> new KeywordInfo().withAdGroupInfo(adGroupInfo))
                .limit(halfLimit).collect(Collectors.toList());
        var dummyKeywords2 = Stream.generate(() -> new KeywordInfo().withAdGroupInfo(adGroupInfo))
                .limit(halfLimit).collect(Collectors.toList());
        int shard = adGroupInfo.getShard();
        List<Long> listWithCid = singletonList(adGroupInfo.getCampaignId());

        // При заполненной половине heavy-лимита bids кампания не heavy
        steps.keywordSteps().createKeywords(dummyKeywords1);
        List<Long> heavyCampaignIds = bidRepository.getHeavyCampaignIds(shard, listWithCid);
        assertThat(heavyCampaignIds, empty());

        // После архивации половины лимита кампания тоже не heavy
        List<Long> bidIds = bidRepository.getBidIdsByCampaignIds(shard, listWithCid);
        keywordRepository.archiveKeywords(dslContextProvider.ppc(shard).configuration(), bidIds);
        heavyCampaignIds = bidRepository.getHeavyCampaignIds(shard, listWithCid);
        assertThat(heavyCampaignIds, empty());

        // Заполнив остальную половину лимита, кампания стала heavy
        steps.keywordSteps().createKeywords(dummyKeywords2);
        heavyCampaignIds = bidRepository.getHeavyCampaignIds(shard, listWithCid);
        assertThat(heavyCampaignIds, equalTo(listWithCid));

        // После архивации второй половины лимита кампания тоже heavy
        bidIds = bidRepository.getBidIdsByCampaignIds(shard, listWithCid);
        keywordRepository.archiveKeywords(dslContextProvider.ppc(shard).configuration(), bidIds);
        heavyCampaignIds = bidRepository.getHeavyCampaignIds(shard, listWithCid);
        assertThat(heavyCampaignIds, equalTo(listWithCid));
    }
}
