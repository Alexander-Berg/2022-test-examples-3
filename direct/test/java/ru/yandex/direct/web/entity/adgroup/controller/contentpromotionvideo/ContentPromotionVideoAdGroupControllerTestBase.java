package ru.yandex.direct.web.entity.adgroup.controller.contentpromotionvideo;

import java.util.List;

import org.junit.Before;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies;
import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.banner.model.Banner;
import ru.yandex.direct.core.entity.banner.model.ContentPromotionBanner;
import ru.yandex.direct.core.entity.banner.model.old.OldBanner;
import ru.yandex.direct.core.entity.banner.type.href.BannersUrlHelper;
import ru.yandex.direct.core.entity.bidmodifier.BidModifier;
import ru.yandex.direct.core.entity.contentpromotion.repository.ContentPromotionRepository;
import ru.yandex.direct.core.entity.keyword.model.Keyword;
import ru.yandex.direct.core.entity.tag.model.Tag;
import ru.yandex.direct.core.entity.tag.repository.TagRepository;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.web.core.model.WebResponse;
import ru.yandex.direct.web.entity.adgroup.controller.AdGroupControllerTestBase;
import ru.yandex.direct.web.entity.adgroup.controller.ContentPromotionVideoAdGroupController;
import ru.yandex.direct.web.entity.adgroup.model.WebContentPromotionAdGroup;
import ru.yandex.direct.web.entity.banner.model.WebContentPromotionBanner;
import ru.yandex.direct.web.entity.keyword.model.WebKeyword;

import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.core.testing.data.campaign.TestCampaignsStrategy.defaultStrategy;
import static ru.yandex.direct.core.testing.data.campaign.TestContentPromotionCampaigns.fullContentPromotionCampaign;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;

public class ContentPromotionVideoAdGroupControllerTestBase extends AdGroupControllerTestBase {

    protected static final String VIDEO_HREF = "https://www.youtube.com/232323";

    @Autowired
    private TagRepository tagRepository;

    @Autowired
    protected ContentPromotionVideoAdGroupController controller;

    @Autowired
    protected ContentPromotionRepository contentPromotionRepository;

    @Autowired
    private BannersUrlHelper bannersUrlHelper;

    protected CampaignInfo campaignInfo;

    @Before
    public void before() {
        super.before();
        campaignInfo = steps.contentPromotionCampaignSteps()
                .createCampaign(clientInfo, fullContentPromotionCampaign()
                        .withStrategy(defaultStrategy()));
    }

    protected void addAndCheckResponse(WebContentPromotionAdGroup requestAdGroup) {
        WebResponse response = controller.saveContentPromotionVideoAdGroup(singletonList(requestAdGroup),
                campaignInfo.getCampaignId(), true, false, false, null);
        checkResponse(response);
    }

    protected void addAndExpectError(WebContentPromotionAdGroup requestAdGroup, String path, String code) {
        WebResponse response = controller.saveContentPromotionVideoAdGroup(singletonList(requestAdGroup),
                campaignInfo.getCampaignId(), true, false, false, null);
        checkErrorResponse(response, path, code);
    }

    protected void updateAndExpectError(WebContentPromotionAdGroup requestAdGroup, String path, String code) {
        WebResponse response = controller.saveContentPromotionVideoAdGroup(singletonList(requestAdGroup),
                campaignInfo.getCampaignId(), false, false, null, null);
        checkErrorResponse(response, path, code);
    }

    protected void updateAndCheckResult(WebContentPromotionAdGroup requestAdGroup) {
        updateAndCheckResult(singletonList(requestAdGroup));
    }

    protected void updateAndCheckResult(List<WebContentPromotionAdGroup> requestAdGroups) {
        WebResponse response = controller.saveContentPromotionVideoAdGroup(requestAdGroups,
                campaignInfo.getCampaignId(), false, false, null, null);
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

    protected List<Long> findTags(long adGroupId) {
        List<Tag> tags =
                tagRepository.getAdGroupsTags(campaignInfo.getShard(), singletonList(adGroupId)).get(adGroupId);
        return mapList(tags, Tag::getId);
    }

    protected void checkBannerContentPromotion(long adGroupId, WebContentPromotionBanner webBanner) {
        var banners = findBanners(adGroupId);
        assertThat("в группе должен быть один баннер", banners, hasSize(1));
        var content = contentPromotionRepository.getContentPromotion(clientId,
                singletonList(webBanner.getContentResource().getContentId()));
        assertThat("Должен быть контент в базе", content, hasSize(1));

        var expectedBanner = new ContentPromotionBanner()
                .withTitle(webBanner.getTitle())
                .withBody(webBanner.getDescription())
                .withContentPromotionId(content.get(0).getId())
                .withHref(content.get(0).getUrl())
                .withDomain(bannersUrlHelper.extractHostFromHrefWithWwwOrNull(content.get(0).getUrl()))
                .withVisitUrl(webBanner.getVisitUrl());

        assertThat("баннер обновился корректно", banners.get(0),
                beanDiffer((Banner) expectedBanner)
                        .useCompareStrategy(DefaultCompareStrategies.onlyExpectedFields()));
    }

    protected void checkKeywords(List<WebKeyword> requestKeywords) {
        List<Keyword> keywords = findKeywords();
        assertThat("неверное количество фраз", keywords, hasSize(requestKeywords.size()));

        assertThat("фраза не совпадает с ожидаемой",
                keywords.get(0).getPhrase(), is(requestKeywords.get(0).getPhrase()));
    }
}
