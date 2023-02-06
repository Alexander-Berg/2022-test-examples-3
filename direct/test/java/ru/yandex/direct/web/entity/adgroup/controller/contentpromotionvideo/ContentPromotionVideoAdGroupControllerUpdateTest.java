package ru.yandex.direct.web.entity.adgroup.controller.contentpromotionvideo;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.StatusBsSynced;
import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.adgroup.model.AdGroupType;
import ru.yandex.direct.core.entity.adgroup.model.ContentPromotionAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.ContentPromotionAdgroupType;
import ru.yandex.direct.core.entity.adgroup.model.PageGroupTagEnum;
import ru.yandex.direct.core.entity.adgroup.model.TargetTagEnum;
import ru.yandex.direct.core.entity.bidmodifier.BidModifier;
import ru.yandex.direct.core.entity.contentpromotion.model.ContentPromotionContent;
import ru.yandex.direct.core.entity.contentpromotion.model.ContentPromotionContentType;
import ru.yandex.direct.core.entity.keyword.model.Keyword;
import ru.yandex.direct.core.entity.relevancematch.model.RelevanceMatch;
import ru.yandex.direct.core.testing.data.banner.TestContentPromotionBanners;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.adgroup.ContentPromotionAdGroupInfo;
import ru.yandex.direct.core.testing.info.campaign.ContentPromotionCampaignInfo;
import ru.yandex.direct.web.configuration.DirectWebTest;
import ru.yandex.direct.web.core.model.WebResponse;
import ru.yandex.direct.web.entity.adgroup.model.WebAdGroupBidModifiers;
import ru.yandex.direct.web.entity.adgroup.model.WebContentPromotionAdGroup;
import ru.yandex.direct.web.entity.banner.model.WebContentPromotionBanner;
import ru.yandex.direct.web.entity.keyword.model.WebKeyword;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;
import static ru.yandex.direct.core.testing.data.TestGroups.activeContentPromotionAdGroup;
import static ru.yandex.direct.core.testing.data.TestKeywords.keywordForContentPromotionVideo;
import static ru.yandex.direct.web.testing.data.TestAdGroups.randomNameWebContentPromotionVideoAdGroup;
import static ru.yandex.direct.web.testing.data.TestAdGroups.webAdGroupRelevanceMatch;
import static ru.yandex.direct.web.testing.data.TestBanners.webContentPromotionVideoBanner;
import static ru.yandex.direct.web.testing.data.TestBidModifiers.fullContentPromotionAdGroupBidModifiers;
import static ru.yandex.direct.web.testing.data.TestKeywords.randomPhraseKeyword;

@DirectWebTest
@RunWith(SpringRunner.class)
public class ContentPromotionVideoAdGroupControllerUpdateTest extends ContentPromotionVideoAdGroupControllerTestBase {
    private static final String ANOTHER_VIDEO_HREF = VIDEO_HREF + "42";

    private ContentPromotionAdGroupInfo contentVideoPromotionAdGroup;
    private Long adGroupId;
    private ContentPromotionContent content;
    private Long anotherContentId;

    @Autowired
    private TestContentPromotionBanners testContentPromotionBanners;

    @Before
    public void before() {
        super.before();
        contentVideoPromotionAdGroup = steps.contentPromotionAdGroupSteps()
                .createDefaultAdGroup((ContentPromotionCampaignInfo) campaignInfo, ContentPromotionAdgroupType.VIDEO);
        adGroupId = contentVideoPromotionAdGroup.getAdGroupId();

        long contentId = contentPromotionRepository.insertContentPromotion(campaignInfo.getClientId(),
                new ContentPromotionContent()
                        .withUrl(VIDEO_HREF)
                        .withType(ContentPromotionContentType.VIDEO)
                        .withIsInaccessible(false)
                        .withExternalId("external-id"));
        content = contentPromotionRepository.getContentPromotion(campaignInfo.getClientId(),
                singletonList(contentId)).get(0);

        anotherContentId = contentPromotionRepository.insertContentPromotion(campaignInfo.getClientId(),
                new ContentPromotionContent()
                        .withUrl(ANOTHER_VIDEO_HREF)
                        .withType(ContentPromotionContentType.VIDEO)
                        .withIsInaccessible(false)
                        .withExternalId("external-id2"));
    }

    @Test
    public void saveContentPromotionVideoAdGroup_ChangePrimitiveFields_FieldsUpdatedCorrectly() {
        WebContentPromotionAdGroup webContentPromotionVideoAdGroup =
                randomNameWebContentPromotionVideoAdGroup(adGroupId, campaignInfo.getCampaignId());

        updateAdGroups(singletonList(webContentPromotionVideoAdGroup));

        AdGroup expected = new ContentPromotionAdGroup()
                .withType(AdGroupType.CONTENT_PROMOTION)
                .withId(adGroupId)
                .withName(webContentPromotionVideoAdGroup.getName());

        List<AdGroup> adGroups = findAdGroup(adGroupId);
        assertThat("группа обновилась корректно", adGroups.get(0),
                beanDiffer(expected).useCompareStrategy(onlyExpectedFields()));
    }

    @Test
    public void saveContentPromotionVideoAdGroup_AddBanner_BannerAdded() {
        WebContentPromotionBanner webBanner = webContentPromotionVideoBanner(null, content.getId());
        WebContentPromotionAdGroup webContentPromotionVideoAdGroup =
                randomNameWebContentPromotionVideoAdGroup(adGroupId, campaignInfo.getCampaignId())
                        .withBanners(singletonList(webBanner));

        updateAdGroups(singletonList(webContentPromotionVideoAdGroup));

        checkBannerContentPromotion(adGroupId, webBanner);
    }

    @Test
    public void saveContentPromotionVideoAdGroup_DeleteBanner_BannerDeleted() {
        steps.contentPromotionBannerSteps().createBanner(
                contentVideoPromotionAdGroup,
                content,
                testContentPromotionBanners.fullContentPromoBanner());

        WebContentPromotionAdGroup webContentPromotionVideoAdGroup =
                randomNameWebContentPromotionVideoAdGroup(adGroupId, campaignInfo.getCampaignId());

        updateAdGroups(singletonList(webContentPromotionVideoAdGroup));

        var banners = findBanners(adGroupId);
        assertThat("в группе не должно быть баннеров", banners, hasSize(1));
    }

    @Test
    public void saveContentPromotionVideoAdGroup_UpdateBanner_BannerUpdated() {
        var contentPromotionBannerInfo = steps.contentPromotionBannerSteps().createBanner(
                contentVideoPromotionAdGroup,
                content,
                testContentPromotionBanners.fullContentPromoBanner());

        WebContentPromotionBanner webBanner = webContentPromotionVideoBanner(contentPromotionBannerInfo.getBannerId(),
                anotherContentId);
        WebContentPromotionAdGroup webAdGroup =
                randomNameWebContentPromotionVideoAdGroup(adGroupId, campaignInfo.getCampaignId())
                        .withBanners(singletonList(webBanner));

        updateAdGroups(singletonList(webAdGroup));

        var banners = findBanners(adGroupId);
        assertThat("в группе должен быть один баннер", banners, hasSize(1));

        checkBannerContentPromotion(adGroupId, webBanner);
    }

    @Test
    public void saveContentPromotionVideoAdGroup_UpdateBannerTitle_BannerHrefNotChanged() {
        var contentPromotionBannerInfo = steps.contentPromotionBannerSteps().createBanner(
                contentVideoPromotionAdGroup,
                content,
                testContentPromotionBanners.fullContentPromoBanner(null, VIDEO_HREF));

        WebContentPromotionBanner webBanner = webContentPromotionVideoBanner(
                contentPromotionBannerInfo.getBannerId(), contentPromotionBannerInfo.getContentId())
                .withTitle("New title 123");
        WebContentPromotionAdGroup webAdGroup =
                randomNameWebContentPromotionVideoAdGroup(adGroupId, campaignInfo.getCampaignId())
                        .withBanners(singletonList(webBanner));

        updateAdGroups(singletonList(webAdGroup));

        var banners = findBanners(adGroupId);
        assertThat("в группе должен быть один баннер", banners, hasSize(1));

        checkBannerContentPromotion(adGroupId, webBanner);
    }

    @Test
    public void saveContentPromotionVideoAdGroup_AddBannerPackshotHref_PackshotHrefIsAdded() {
        var contentPromotionBannerInfo = steps.contentPromotionBannerSteps().createBanner(
                contentVideoPromotionAdGroup,
                content,
                testContentPromotionBanners.fullContentPromoBanner(null, VIDEO_HREF)
                        .withVisitUrl(null));

        WebContentPromotionBanner webBanner = webContentPromotionVideoBanner(contentPromotionBannerInfo.getBannerId(),
                contentPromotionBannerInfo.getContentId()).withVisitUrl("https://www.yandex.ru/");
        WebContentPromotionAdGroup webAdGroup =
                randomNameWebContentPromotionVideoAdGroup(adGroupId, campaignInfo.getCampaignId())
                        .withBanners(singletonList(webBanner));

        updateAdGroups(singletonList(webAdGroup));

        var banners = findBanners(adGroupId);
        assertThat("в группе должен быть один баннер", banners, hasSize(1));

        checkBannerContentPromotion(adGroupId, webBanner);
    }


    @Test
    public void saveContentPromotionVideoAdGroup_UpdateBannerPackshotHref_PackshotHrefIsChanged() {
        var contentPromotionBannerInfo = steps.contentPromotionBannerSteps().createBanner(
                contentVideoPromotionAdGroup,
                content,
                testContentPromotionBanners.fullContentPromoBanner(null, VIDEO_HREF));

        WebContentPromotionBanner webBanner = webContentPromotionVideoBanner(
                contentPromotionBannerInfo.getBannerId(), contentPromotionBannerInfo.getContentId())
                .withVisitUrl("https://www.google.ru/");
        WebContentPromotionAdGroup webAdGroup =
                randomNameWebContentPromotionVideoAdGroup(adGroupId, campaignInfo.getCampaignId())
                        .withBanners(singletonList(webBanner));

        updateAdGroups(singletonList(webAdGroup));

        var banners = findBanners(adGroupId);
        assertThat("в группе должен быть один баннер", banners, hasSize(1));

        checkBannerContentPromotion(adGroupId, webBanner);
    }

    @Test
    public void saveContentPromotionVideoAdGroup_AddKeywords_KeywordsAdded() {
        WebContentPromotionAdGroup webContentPromotionVideoAdGroup =
                randomNameWebContentPromotionVideoAdGroup(adGroupId, campaignInfo.getCampaignId())
                        .withGeneralPrice(120.0)
                        .withKeywords(singletonList(randomPhraseKeyword(null)));

        updateAdGroups(singletonList(webContentPromotionVideoAdGroup));

        List<Keyword> keywords = findKeywords();
        assertThat("в группе должна быть одна фраза", keywords, hasSize(1));

        Double generalPrice = webContentPromotionVideoAdGroup.getGeneralPrice();
        Keyword expectedKeyword = new Keyword()
                .withPrice(null)
                .withPriceContext(BigDecimal.valueOf(generalPrice).setScale(2, RoundingMode.DOWN))
                .withPhrase(webContentPromotionVideoAdGroup.getKeywords().get(0).getPhrase());
        assertThat("добавилась корректная фраза", keywords.get(0),
                beanDiffer(expectedKeyword).useCompareStrategy(onlyExpectedFields()));
    }

    @Test
    public void saveContentPromotionVideoAdGroup_DeleteKeywords_KeywordsDeleted() {
        steps.newKeywordSteps().createKeyword(contentVideoPromotionAdGroup, keywordForContentPromotionVideo());

        WebContentPromotionAdGroup webContentPromotionVideoAdGroup =
                randomNameWebContentPromotionVideoAdGroup(adGroupId, campaignInfo.getCampaignId());

        updateAdGroups(singletonList(webContentPromotionVideoAdGroup));

        List<Keyword> keywords = findKeywords();
        assertThat("в группе не должно быть фраз", keywords, hasSize(0));
    }

    @Test
    public void saveContentPromotionVideoAdGroup_UpdateKeywords_KeywordsUpdated() {
        var keyword = steps.newKeywordSteps()
                .createKeyword(contentVideoPromotionAdGroup, keywordForContentPromotionVideo());

        WebContentPromotionAdGroup webContentPromotionVideoAdGroup =
                randomNameWebContentPromotionVideoAdGroup(adGroupId, campaignInfo.getCampaignId())
                        .withGeneralPrice(130.0)
                        .withKeywords(singletonList(new WebKeyword()
                                .withId(keyword.getKeywordId())
                                .withPhrase("new phrase")));

        updateAdGroups(singletonList(webContentPromotionVideoAdGroup));

        List<Keyword> keywords = findKeywords();
        assertThat("в группе должна быть одна фраза", keywords, hasSize(1));

        Double generalPrice = webContentPromotionVideoAdGroup.getGeneralPrice();
        Keyword expectedKeyword = new Keyword()
                .withId(keyword.getKeywordId())
                .withPriceContext(BigDecimal.valueOf(generalPrice).setScale(2, RoundingMode.DOWN))
                .withPhrase(webContentPromotionVideoAdGroup.getKeywords().get(0).getPhrase());
        assertThat("фраза обновилась корректно", keywords.get(0),
                beanDiffer(expectedKeyword).useCompareStrategy(onlyExpectedFields()));
    }

    @Test
    public void saveContentPromotionVideoAdGroup_AddBidModifiers_BidModifiersAdded() {
        WebAdGroupBidModifiers webBidModifiers =
                fullContentPromotionAdGroupBidModifiers(retCondId);
        WebContentPromotionAdGroup webContentPromotionVideoAdGroup =
                randomNameWebContentPromotionVideoAdGroup(adGroupId, campaignInfo.getCampaignId())
                        .withKeywords(singletonList(randomPhraseKeyword(null)))
                        .withBidModifiers(webBidModifiers);

        updateAdGroups(singletonList(webContentPromotionVideoAdGroup));

        List<BidModifier> bidModifiers = findBidModifiers();
        assertThat("должно было добавиться 3 корректировки", bidModifiers, hasSize(3));
        checkDemographyBidModifier(bidModifiers, adGroupId, webBidModifiers.getDemographicsBidModifier());
        checkRetargetingBidModifier(bidModifiers, adGroupId, webBidModifiers.getRetargetingBidModifier());
        checkMobileBidModifier(bidModifiers, adGroupId, webBidModifiers.getMobileBidModifier());
    }

    @Test
    public void saveContentPromotionVideoAdGroup_DeleteBidModifiers_BidModifiersDeleted() {
        steps.newBidModifierSteps().createDefaultAdGroupBidModifierDemographics(contentVideoPromotionAdGroup);

        WebContentPromotionAdGroup webContentPromotionVideoAdGroup =
                randomNameWebContentPromotionVideoAdGroup(adGroupId, campaignInfo.getCampaignId())
                        .withKeywords(singletonList(randomPhraseKeyword(null)));

        updateAdGroups(singletonList(webContentPromotionVideoAdGroup));

        List<BidModifier> bidModifiers = findBidModifiers();
        assertThat("корректировки должны были удалиться", bidModifiers, hasSize(0));
    }

    @Test
    public void saveContentPromotionVideoAdGroup_UpdateTwoAdGroups_AdGroupsUpdated() {
        AdGroupInfo anotherContentPromotionAdGroup = steps.adGroupSteps().createDefaultContentPromotionAdGroup(
                campaignInfo, ContentPromotionAdgroupType.VIDEO);

        WebAdGroupBidModifiers webBidModifiers = fullContentPromotionAdGroupBidModifiers(retCondId);
        WebContentPromotionAdGroup adGroupWithBidModifiers = randomNameWebContentPromotionVideoAdGroup(
                anotherContentPromotionAdGroup.getAdGroupId(), campaignInfo.getCampaignId())
                .withBidModifiers(webBidModifiers);

        var keyword = steps.newKeywordSteps().createKeyword(contentVideoPromotionAdGroup,
                keywordForContentPromotionVideo());
        WebContentPromotionAdGroup adGroupWithKeywords =
                randomNameWebContentPromotionVideoAdGroup(adGroupId, campaignInfo.getCampaignId())
                        .withGeneralPrice(130.0)
                        .withKeywords(singletonList(new WebKeyword()
                                .withId(keyword.getKeywordId())
                                .withPhrase("new phrase")));

        updateAdGroups(asList(adGroupWithBidModifiers, adGroupWithKeywords));

        checkKeywords(adGroupWithKeywords.getKeywords());

        List<BidModifier> bidModifiers = findBidModifiers();
        assertThat("должно было добавиться 3 корректировки", bidModifiers, hasSize(3));

        Long adGroupWithBidModifiersId = adGroupWithBidModifiers.getId();

        checkRetargetingBidModifier(bidModifiers, adGroupWithBidModifiersId,
                webBidModifiers.getRetargetingBidModifier());
        checkDemographyBidModifier(bidModifiers, adGroupWithBidModifiersId,
                webBidModifiers.getDemographicsBidModifier());
        checkMobileBidModifier(bidModifiers, adGroupWithBidModifiersId, webBidModifiers.getMobileBidModifier());
    }

    @Test
    public void saveContentPromotionVideoAdGroup_UpdateTags_TagsUpdated() {
        List<Long> campaignTags = steps.tagCampaignSteps().createDefaultTags(
                campaignInfo.getShard(), campaignInfo.getClientId(), campaignInfo.getCampaignId(), 3);
        AdGroupInfo adGroup = steps.adGroupSteps().createAdGroup(
                activeContentPromotionAdGroup(campaignInfo.getCampaignId(), ContentPromotionAdgroupType.VIDEO)
                        .withTags(campaignTags), campaignInfo);

        WebContentPromotionAdGroup webContentPromotionVideoAdGroup =
                randomNameWebContentPromotionVideoAdGroup(adGroup.getAdGroupId(), campaignInfo.getCampaignId())
                        .withTags(singletonMap(campaignTags.get(0).toString(), 1));

        updateAdGroups(singletonList(webContentPromotionVideoAdGroup));

        List<Long> tags = findTags(adGroup.getAdGroupId());
        assertThat("теги добавились", tags, containsInAnyOrder(campaignTags.get(0)));
    }

    @Test
    public void saveContentPromotionVideoAdGroup_UpdatePageGroupTags_PageGroupTagsUpdated() {
        List<String> pageGroupTags = asList("page_group_tag1", "page_group_tag2");
        AdGroupInfo adGroupInfo = steps.adGroupSteps().createAdGroup(
                activeContentPromotionAdGroup(campaignInfo.getCampaignId(), ContentPromotionAdgroupType.VIDEO),
                campaignInfo);

        WebContentPromotionAdGroup webContentPromotionVideoAdGroup =
                randomNameWebContentPromotionVideoAdGroup(adGroupInfo.getAdGroupId(), campaignInfo.getCampaignId())
                        .withPageGroupTags(pageGroupTags);

        updateAdGroups(singletonList(webContentPromotionVideoAdGroup));

        AdGroup actualAdGroup = findAdGroup(adGroupInfo.getAdGroupId()).get(0);
        assertThat("statusBsSynced сбросился", actualAdGroup.getStatusBsSynced(), equalTo(StatusBsSynced.NO));
        assertThat("теги для таргетинга обновились", actualAdGroup.getPageGroupTags(),
                containsInAnyOrder("page_group_tag1", "page_group_tag2",
                        getValueOfPageGroupOrTargetTagEnum(PageGroupTagEnum.CONTENT_PROMOTION_VIDEO_TAG)));
    }

    @Test
    public void saveContentPromotionVideoAdGroup_UpdateTargetTags_TargetTagsUpdated() {
        List<String> targetTags = asList("target_tag1", "target_tag2");
        AdGroupInfo adGroupInfo = steps.adGroupSteps().createAdGroup(
                activeContentPromotionAdGroup(campaignInfo.getCampaignId(), ContentPromotionAdgroupType.VIDEO),
                campaignInfo);

        WebContentPromotionAdGroup webContentPromotionVideoAdGroup =
                randomNameWebContentPromotionVideoAdGroup(adGroupInfo.getAdGroupId(), campaignInfo.getCampaignId())
                        .withTargetTags(targetTags);

        updateAdGroups(singletonList(webContentPromotionVideoAdGroup));

        AdGroup actualAdGroup = findAdGroup(adGroupInfo.getAdGroupId()).get(0);
        assertThat("statusBsSynced сбросился", actualAdGroup.getStatusBsSynced(), equalTo(StatusBsSynced.NO));
        assertThat("теги для таргетинга обновились", actualAdGroup.getTargetTags(), containsInAnyOrder("target_tag1",
                "target_tag2",
                getValueOfPageGroupOrTargetTagEnum(TargetTagEnum.CONTENT_PROMOTION_VIDEO_TAG)));
    }

    @Test
    public void saveContentPromotionVideoAdGroup_AddRelevanceMatch_RelevanceMatchAdded() {
        WebContentPromotionAdGroup webContentPromotionVideoAdGroup =
                randomNameWebContentPromotionVideoAdGroup(adGroupId, campaignInfo.getCampaignId())
                        .withRelevanceMatches(singletonList(webAdGroupRelevanceMatch(null)));

        updateAdGroups(singletonList(webContentPromotionVideoAdGroup));

        List<RelevanceMatch> relevanceMatches = findRelevanceMatches(adGroupId);
        assertThat("в группу должен добавиться бесфразный таргетинг", relevanceMatches, hasSize(1));
    }

    private void updateAdGroups(List<WebContentPromotionAdGroup> webContentPromotionVideoAdGroups) {
        WebResponse webResponse = controller.saveContentPromotionVideoAdGroup(webContentPromotionVideoAdGroups,
                campaignInfo.getCampaignId(), false, true, false, null);
        checkResponse(webResponse);
    }
}
