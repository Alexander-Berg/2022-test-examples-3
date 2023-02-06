package ru.yandex.direct.core.entity.changes.service;

import java.time.LocalDateTime;
import java.util.Set;

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.changes.model.CheckIntReq;
import ru.yandex.direct.core.entity.changes.model.CheckIntResp;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.TextBannerInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.utils.CollectionUtils;

import static org.junit.Assert.assertThat;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class CheckServiceTest {
    @Autowired
    private DslContextProvider dslContextProvider;

    @Autowired
    private CheckService checkService;

    @Autowired
    private Steps steps;

    private ClientInfo clientInfo;

    @Before
    public void before() {
        clientInfo = steps.clientSteps().createDefaultClient();
    }

    @Test
    public void testTwoBannersSameGroupAdsRequest() {
        CampaignInfo campaignInfo = steps.campaignSteps().createActiveTextCampaign(clientInfo);
        AdGroupInfo adGroupInfo = steps.adGroupSteps().createActiveTextAdGroup(campaignInfo);
        TextBannerInfo bannerInfoFirst = steps.bannerSteps().createActiveTextBanner(adGroupInfo);
        TextBannerInfo bannerInfoSecond = steps.bannerSteps().createActiveTextBanner(adGroupInfo);
        CheckIntReq checkIntReq = defaultReqWithNoStats(null, null,
                Set.of(bannerInfoFirst.getBannerId(), bannerInfoSecond.getBannerId()));
        CheckIntResp resp = processRequestForDefaultClientReturnResult(checkIntReq);
        assertThat(resp.getIds(CheckIntResp.ParamBlock.ModifiedAdIds), Matchers.containsInAnyOrder(
                Matchers.is(bannerInfoFirst.getBannerId()), Matchers.is(bannerInfoSecond.getBannerId())));
    }

    @Test
    public void testCampaignWithZeroDateCampaignIdsRequestNoChanges() {
        CampaignInfo campaignInfo = steps.campaignSteps().createActiveTextCampaign(clientInfo);
        steps.campaignSteps().setLastChange(clientInfo.getShard(), campaignInfo.getCampaignId(), null);
        CheckIntReq checkIntReq = defaultReqWithNoStats(Set.of(campaignInfo.getCampaignId()), null, null);
        CheckIntResp resp = processRequestForDefaultClientReturnResult(checkIntReq);
        assertThat(CollectionUtils.isEmpty(resp.getIds(CheckIntResp.ParamBlock.ModifiedCampaignIds)),
                Matchers.is(true));
    }

    @Test
    public void testCampaignWithZeroDatePidsRequestNoChanges() {
        CampaignInfo campaignInfo = steps.campaignSteps().createActiveTextCampaign(clientInfo);
        AdGroupInfo adGroupInfo = steps.adGroupSteps().createActiveTextAdGroup(campaignInfo);
        steps.campaignSteps().setLastChange(clientInfo.getShard(), campaignInfo.getCampaignId(), null);
        CheckIntReq checkIntReq = defaultReqWithNoStats(null, Set.of(adGroupInfo.getAdGroupId()), null);
        CheckIntResp resp = processRequestForDefaultClientReturnResult(checkIntReq);
        assertThat(CollectionUtils.isEmpty(resp.getIds(CheckIntResp.ParamBlock.ModifiedCampaignIds)),
                Matchers.is(true));
    }

    @Test
    public void testGroupWithZeroDatePidsRequestNoChanges() {
        CampaignInfo campaignInfo = steps.campaignSteps().createActiveTextCampaign(clientInfo);
        AdGroupInfo adGroupInfo = steps.adGroupSteps().createActiveTextAdGroup(campaignInfo);
        steps.adGroupSteps().setLastChange(clientInfo.getShard(), adGroupInfo.getAdGroupId(), null);
        CheckIntReq checkIntReq = defaultReqWithNoStats(null, Set.of(adGroupInfo.getAdGroupId()), null);
        CheckIntResp resp = processRequestForDefaultClientReturnResult(checkIntReq);
        assertThat(CollectionUtils.isEmpty(resp.getIds(CheckIntResp.ParamBlock.ModifiedAdGroupIds)),
                Matchers.is(true));
    }

    @Test
    public void testCampaignWithZeroDateBidsRequestNoChanges() {
        CampaignInfo campaignInfo = steps.campaignSteps().createActiveTextCampaign(clientInfo);
        AdGroupInfo adGroupInfo = steps.adGroupSteps().createActiveTextAdGroup(campaignInfo);
        TextBannerInfo bannerInfo = steps.bannerSteps().createActiveTextBanner(adGroupInfo);
        steps.campaignSteps().setLastChange(clientInfo.getShard(), campaignInfo.getCampaignId(), null);
        CheckIntReq checkIntReq = defaultReqWithNoStats(null, null, Set.of(bannerInfo.getBannerId()));
        CheckIntResp resp = processRequestForDefaultClientReturnResult(checkIntReq);
        assertThat(CollectionUtils.isEmpty(resp.getIds(CheckIntResp.ParamBlock.ModifiedCampaignIds)),
                Matchers.is(true));
    }

    private CheckIntResp processRequestForDefaultClientReturnResult(CheckIntReq checkIntReq) {
        return checkService.processInternalRequest(
                clientInfo.getUid(), clientInfo.getClientId(), checkIntReq);
    }

    private static CheckIntReq defaultReqWithNoStats(Set<Long> cids, Set<Long> pids, Set<Long> bids) {
        return new CheckIntReq(cids, pids, bids, LocalDateTime.now().minusDays(2),
                true, true, true, false);
    }
}
