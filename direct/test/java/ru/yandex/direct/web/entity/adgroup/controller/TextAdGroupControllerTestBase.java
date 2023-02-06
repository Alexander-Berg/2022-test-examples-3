package ru.yandex.direct.web.entity.adgroup.controller;

import java.util.List;

import org.junit.Before;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.banner.model.old.OldBanner;
import ru.yandex.direct.core.entity.bidmodifier.BidModifier;
import ru.yandex.direct.core.entity.keyword.model.Keyword;
import ru.yandex.direct.core.entity.vcard.model.Vcard;
import ru.yandex.direct.core.entity.vcard.repository.VcardRepository;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.web.core.model.WebResponse;
import ru.yandex.direct.web.entity.adgroup.model.WebTextAdGroup;

import static java.util.Collections.singletonList;

public class TextAdGroupControllerTestBase extends AdGroupControllerTestBase {

    @Autowired
    protected AdGroupController controller;

    @Autowired
    protected VcardRepository vcardRepository;

    protected CampaignInfo campaignInfo;

    @Before
    public void before() {
        super.before();
        campaignInfo = steps.campaignSteps().createActiveTextCampaign(clientInfo);
    }

    protected void addAndExpectError(WebTextAdGroup requestAdGroup, String path, String code) {
        WebResponse response = controller.saveTextAdGroup(singletonList(requestAdGroup), campaignInfo.getCampaignId(),
                true, false, null, null, null);
        checkErrorResponse(response, path, code);
    }

    protected void updateAndExpectError(WebTextAdGroup requestAdGroup, String path, String code) {
        WebResponse response = controller.saveTextAdGroup(singletonList(requestAdGroup), campaignInfo.getCampaignId(),
                false, false, null, null, null);
        checkErrorResponse(response, path, code);
    }

    protected void updateAndCheckResult(WebTextAdGroup requestAdGroup) {
        updateAndCheckResult(singletonList(requestAdGroup));
    }

    protected void updateAndCheckResult(List<WebTextAdGroup> requestAdGroups) {
        WebResponse response = controller.saveTextAdGroup(requestAdGroups, campaignInfo.getCampaignId(),
                false, false, null, null, null);
        checkResponse(response);
    }

    protected List<AdGroup> findAdGroups() {
        return findAdGroups(campaignInfo.getCampaignId());
    }

    protected List<Keyword> findKeywords() {
        return findKeywords(campaignInfo.getCampaignId());
    }

    protected List<BidModifier> findBidModifiers() {
        return findBidModifiers(campaignInfo.getCampaignId());
    }

    protected List<OldBanner> findBanners() {
        return findBannersByCampaignId(campaignInfo.getCampaignId());
    }

    protected List<Vcard> findVcards() {
        return vcardRepository.getVcards(shard, campaignInfo.getUid());
    }


}
