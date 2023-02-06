package ru.yandex.direct.oneshot.oneshots.delete_keywords_from_bids_base;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.common.db.PpcPropertiesSupport;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbschema.ppc.enums.BidsBaseBidType;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.oneshot.configuration.OneshotTest;
import ru.yandex.direct.oneshot.oneshots.delete_keywords_from_bids_base.repository.OneshotBidsBaseRepository;

import static ru.yandex.direct.dbschema.ppc.Tables.BIDS_BASE;

@OneshotTest
@RunWith(SpringRunner.class)
public class DeleteKeywordsFromBidsBaseByClientIdOneshotTest {

    @Autowired
    private Steps steps;
    @Autowired
    private DslContextProvider dslContextProvider;
    @Autowired
    private OneshotBidsBaseRepository oneshotBidsBaseRepository;
    @Autowired
    private PpcPropertiesSupport ppcPropertiesSupport;
    @Autowired
    private ShardHelper shardHelper;

    private AdGroupInfo adGroupInfo1;
    private AdGroupInfo adGroupInfo2;
    private DeleteKeywordsFromBidsBaseOneshot oneshot;
    private int shard;

    @Before
    public void before() {
        oneshot = new DeleteKeywordsFromBidsBaseOneshot(
                oneshotBidsBaseRepository, ppcPropertiesSupport, shardHelper);

        ClientInfo clientInfo1 = steps.clientSteps().createDefaultClient();
        ClientInfo clientInfo2 = steps.clientSteps().createDefaultClient();

        adGroupInfo1 = steps.adGroupSteps().createActiveTextAdGroup(clientInfo1);
        adGroupInfo2 = steps.adGroupSteps().createActiveTextAdGroup(clientInfo2);
        shard = adGroupInfo1.getShard();
    }

    @Test
    public void test() {
        addKeywordsToBidsBase(Set.of(1L, 3L), adGroupInfo1);
        addKeywordsToBidsBase(Set.of(2L), adGroupInfo2);

        oneshot.execute(new InputData(adGroupInfo1.getClientId().asLong()), null, shard);

        List<Long> bidBaseIds1 = getBidsBaseIds(adGroupInfo1.getAdGroupId());
        List<Long> bidBaseIds2 = getBidsBaseIds(adGroupInfo2.getAdGroupId());

        SoftAssertions softAssertions = new SoftAssertions();
        softAssertions.assertThat(bidBaseIds1)
                .as("данные в bids_base от отправленного клиента")
                .isEmpty();
        softAssertions.assertThat(bidBaseIds2)
                .as("данные в bids_base от другого клиента")
                .hasSize(1)
                .contains(2L);
        softAssertions.assertAll();
    }

    public void addKeywordsToBidsBase(Set<Long> bidIds, AdGroupInfo adGroupInfo) {
        bidIds.forEach(bidId -> addToBidsBase(bidId, adGroupInfo));
    }

    private void addToBidsBase(long bidId, AdGroupInfo adGroupInfo) {
        dslContextProvider.ppc(shard)
                .insertInto(BIDS_BASE)
                .set(BIDS_BASE.BID_ID, bidId)
                .set(BIDS_BASE.BID_TYPE, BidsBaseBidType.keyword)
                .set(BIDS_BASE.CID, adGroupInfo.getCampaignId())
                .set(BIDS_BASE.PID, adGroupInfo.getAdGroupId())
                .set(BIDS_BASE.PRICE, BigDecimal.TEN)
                .set(BIDS_BASE.PRICE_CONTEXT, BigDecimal.TEN)
                .set(BIDS_BASE.OPTS, "")
                .execute();
    }

    public List<Long> getBidsBaseIds(long pid) {
        return dslContextProvider.ppc(shard)
                .select(BIDS_BASE.BID_ID)
                .from(BIDS_BASE)
                .where(BIDS_BASE.PID.eq(pid))
                .fetch(BIDS_BASE.BID_ID);
    }
}
