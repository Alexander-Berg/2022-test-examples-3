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
import ru.yandex.direct.core.entity.contentpromotion.repository.ContentPromotionRepository;
import ru.yandex.direct.core.entity.keyword.model.Keyword;
import ru.yandex.direct.core.entity.relevancematch.model.RelevanceMatch;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.ContentPromotionBannerInfo;
import ru.yandex.direct.core.testing.info.KeywordInfo;
import ru.yandex.direct.web.configuration.DirectWebTest;
import ru.yandex.direct.web.core.model.WebResponse;
import ru.yandex.direct.web.entity.adgroup.model.WebAdGroupBidModifiers;
import ru.yandex.direct.web.entity.adgroup.model.WebContentPromotionAdGroup;
import ru.yandex.direct.web.entity.adgroup.model.WebContentPromotionAdGroupType;
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
import static ru.yandex.direct.core.testing.data.TestBanners.activeContentPromotionBannerCollectionType;
import static ru.yandex.direct.core.testing.data.TestGroups.activeContentPromotionAdGroup;
import static ru.yandex.direct.core.testing.data.TestKeywords.keywordForContentPromotionVideo;
import static ru.yandex.direct.web.testing.data.TestAdGroups.randomNameWebContentPromotionCollectionAdGroup;
import static ru.yandex.direct.web.testing.data.TestAdGroups.randomNameWebContentPromotionVideoAdGroup;
import static ru.yandex.direct.web.testing.data.TestAdGroups.webAdGroupRelevanceMatch;
import static ru.yandex.direct.web.testing.data.TestBanners.webContentPromotionCollectionBanner;
import static ru.yandex.direct.web.testing.data.TestBidModifiers.fullContentPromotionAdGroupBidModifiers;
import static ru.yandex.direct.web.testing.data.TestKeywords.randomPhraseKeyword;

@DirectWebTest
@RunWith(SpringRunner.class)
public class ContentPromotionCollectionsAdGroupUpdateTest extends ContentPromotionVideoAdGroupControllerTestBase {

    @Autowired
    private ContentPromotionRepository contentPromotionRepository;

    private AdGroupInfo contentPromotionAdGroup;
    private Long adGroupId;
    private Long contentId;

    @Before
    public void before() {
        super.before();
        contentPromotionAdGroup = steps.adGroupSteps().createDefaultContentPromotionAdGroup(campaignInfo,
                ContentPromotionAdgroupType.COLLECTION);
        adGroupId = contentPromotionAdGroup.getAdGroupId();
        contentId = contentPromotionRepository.insertContentPromotion(contentPromotionAdGroup.getClientId(),
                new ContentPromotionContent()
                        .withExternalId("EXTERNAL")
                        .withUrl(VIDEO_HREF)
                        .withIsInaccessible(false)
                        .withType(ContentPromotionContentType.COLLECTION));
    }

    @Test
    public void saveContentPromotionCollectionAdGroup_ChangePrimitiveFields_FieldsUpdatedCorrectly() {
        WebContentPromotionAdGroup webContentPromotionVideoAdGroup =
                randomNameWebContentPromotionVideoAdGroup(adGroupId, campaignInfo.getCampaignId())
                        .withAdGroupContentType(WebContentPromotionAdGroupType.COLLECTION);

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
    public void saveContentPromotionCollectionAdGroup_AddBanner_BannerAdded() {
        WebContentPromotionBanner webBanner = webContentPromotionCollectionBanner(null, contentId);
        WebContentPromotionAdGroup webContentPromotionVideoAdGroup =
                randomNameWebContentPromotionCollectionAdGroup(adGroupId, campaignInfo.getCampaignId())
                        .withAdGroupContentType(WebContentPromotionAdGroupType.COLLECTION)
                        .withBanners(singletonList(webBanner));

        updateAdGroups(singletonList(webContentPromotionVideoAdGroup));

        checkBannerContentPromotion(adGroupId, webBanner);
    }

    @Test
    public void saveContentPromotionCollectionAdGroup_DeleteBanner_BannerDeleted() {
        steps.bannerSteps().createActiveContentPromotionBanner(
                activeContentPromotionBannerCollectionType(campaignInfo.getCampaignId(), adGroupId),
                contentPromotionAdGroup);

        WebContentPromotionAdGroup webContentPromotionVideoAdGroup =
                randomNameWebContentPromotionVideoAdGroup(adGroupId, campaignInfo.getCampaignId())
                        .withAdGroupContentType(WebContentPromotionAdGroupType.COLLECTION);

        updateAdGroups(singletonList(webContentPromotionVideoAdGroup));

        var banners = findBanners(adGroupId);
        assertThat("в группе не должно быть баннеров", banners, hasSize(1));
    }

    @Test
    public void saveContentPromotionCollectionAdGroup_AddBannerPackshotHref_PackshotHrefIsAdded() {
        ContentPromotionBannerInfo contentPromotionBanner =
                steps.bannerSteps().createActiveContentPromotionBanner(
                        activeContentPromotionBannerCollectionType(campaignInfo.getCampaignId(), adGroupId)
                                .withHref(VIDEO_HREF)
                                .withVisitUrl(null),
                        contentPromotionAdGroup);

        WebContentPromotionBanner webBanner =
                webContentPromotionCollectionBanner(contentPromotionBanner.getBannerId(), contentId)
                        .withVisitUrl("https://www.yandex.ru/");
        WebContentPromotionAdGroup webAdGroup =
                randomNameWebContentPromotionVideoAdGroup(adGroupId, campaignInfo.getCampaignId())
                        .withAdGroupContentType(WebContentPromotionAdGroupType.COLLECTION)
                        .withBanners(singletonList(webBanner));

        updateAdGroups(singletonList(webAdGroup));

        var banners = findBanners(adGroupId);
        assertThat("в группе должен быть один баннер", banners, hasSize(1));

        checkBannerContentPromotion(adGroupId, webBanner);
    }


    @Test
    public void saveContentPromotionCollectionAdGroup_UpdateBannerPackshotHref_PackshotHrefIsChanged() {
        ContentPromotionBannerInfo contentPromotionBanner =
                steps.bannerSteps().createActiveContentPromotionBanner(
                        activeContentPromotionBannerCollectionType(campaignInfo.getCampaignId(), adGroupId)
                                .withHref(VIDEO_HREF),
                        contentPromotionAdGroup);

        WebContentPromotionBanner webBanner =
                webContentPromotionCollectionBanner(contentPromotionBanner.getBannerId(), contentId)
                        .withVisitUrl("https://www.google.ru/");
        WebContentPromotionAdGroup webAdGroup =
                randomNameWebContentPromotionVideoAdGroup(adGroupId, campaignInfo.getCampaignId())
                        .withAdGroupContentType(WebContentPromotionAdGroupType.COLLECTION)
                        .withBanners(singletonList(webBanner));

        updateAdGroups(singletonList(webAdGroup));

        var banners = findBanners(adGroupId);
        assertThat("в группе должен быть один баннер", banners, hasSize(1));

        checkBannerContentPromotion(adGroupId, webBanner);
    }

    @Test
    public void saveContentPromotionCollectionAdGroup_AddKeywords_KeywordsAdded() {
        WebContentPromotionAdGroup webContentPromotionVideoAdGroup =
                randomNameWebContentPromotionVideoAdGroup(adGroupId, campaignInfo.getCampaignId())
                        .withAdGroupContentType(WebContentPromotionAdGroupType.COLLECTION)
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
    public void saveContentPromotionCollectionAdGroup_DeleteKeywords_KeywordsDeleted() {
        steps.keywordSteps().createKeyword(contentPromotionAdGroup, keywordForContentPromotionVideo());

        WebContentPromotionAdGroup webContentPromotionVideoAdGroup =
                randomNameWebContentPromotionVideoAdGroup(adGroupId, campaignInfo.getCampaignId())
                        .withAdGroupContentType(WebContentPromotionAdGroupType.COLLECTION);

        updateAdGroups(singletonList(webContentPromotionVideoAdGroup));

        List<Keyword> keywords = findKeywords();
        assertThat("в группе не должно быть фраз", keywords, hasSize(0));
    }

    @Test
    public void saveContentPromotionCollectionAdGroup_UpdateKeywords_KeywordsUpdated() {
        KeywordInfo keyword =
                steps.keywordSteps().createKeyword(contentPromotionAdGroup, keywordForContentPromotionVideo());

        WebContentPromotionAdGroup webContentPromotionVideoAdGroup =
                randomNameWebContentPromotionVideoAdGroup(adGroupId, campaignInfo.getCampaignId())
                        .withAdGroupContentType(WebContentPromotionAdGroupType.COLLECTION)
                        .withGeneralPrice(130.0)
                        .withKeywords(singletonList(new WebKeyword()
                                .withId(keyword.getId())
                                .withPhrase("new phrase")));

        updateAdGroups(singletonList(webContentPromotionVideoAdGroup));

        List<Keyword> keywords = findKeywords();
        assertThat("в группе должна быть одна фраза", keywords, hasSize(1));

        Double generalPrice = webContentPromotionVideoAdGroup.getGeneralPrice();
        Keyword expectedKeyword = new Keyword()
                .withId(keyword.getId())
                .withPriceContext(BigDecimal.valueOf(generalPrice).setScale(2, RoundingMode.DOWN))
                .withPhrase(webContentPromotionVideoAdGroup.getKeywords().get(0).getPhrase());
        assertThat("фраза обновилась корректно", keywords.get(0),
                beanDiffer(expectedKeyword).useCompareStrategy(onlyExpectedFields()));
    }

    @Test
    public void saveContentPromotionCollectionAdGroup_AddBidModifiers_BidModifiersAdded() {
        WebAdGroupBidModifiers webBidModifiers =
                fullContentPromotionAdGroupBidModifiers(retCondId);
        WebContentPromotionAdGroup webContentPromotionVideoAdGroup =
                randomNameWebContentPromotionVideoAdGroup(adGroupId, campaignInfo.getCampaignId())
                        .withAdGroupContentType(WebContentPromotionAdGroupType.COLLECTION)
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
    public void saveContentPromotionCollectionAdGroup_DeleteBidModifiers_BidModifiersDeleted() {
        steps.bidModifierSteps().createDefaultAdGroupBidModifierMobile(contentPromotionAdGroup);

        WebContentPromotionAdGroup webContentPromotionVideoAdGroup =
                randomNameWebContentPromotionVideoAdGroup(adGroupId, campaignInfo.getCampaignId())
                        .withAdGroupContentType(WebContentPromotionAdGroupType.COLLECTION)
                        .withKeywords(singletonList(randomPhraseKeyword(null)));

        updateAdGroups(singletonList(webContentPromotionVideoAdGroup));

        List<BidModifier> bidModifiers = findBidModifiers();
        assertThat("корректировки должны были удалиться", bidModifiers, hasSize(0));
    }

    @Test
    public void saveContentPromotionCollectionAdGroup_UpdateTwoAdGroups_AdGroupsUpdated() {
        AdGroupInfo anotherContentVideoPromotionAdGroup =
                steps.adGroupSteps().createDefaultContentPromotionAdGroup(campaignInfo,
                        ContentPromotionAdgroupType.COLLECTION);

        WebAdGroupBidModifiers webBidModifiers =
                fullContentPromotionAdGroupBidModifiers(retCondId);
        WebContentPromotionAdGroup adGroupWithBidModifiers =
                randomNameWebContentPromotionVideoAdGroup(anotherContentVideoPromotionAdGroup.getAdGroupId(),
                        campaignInfo.getCampaignId())
                        .withAdGroupContentType(WebContentPromotionAdGroupType.COLLECTION)
                        .withBidModifiers(webBidModifiers);

        KeywordInfo keyword = steps.keywordSteps().createKeyword(contentPromotionAdGroup,
                keywordForContentPromotionVideo());
        WebContentPromotionAdGroup adGroupWithKeywords =
                randomNameWebContentPromotionVideoAdGroup(adGroupId, campaignInfo.getCampaignId())
                        .withAdGroupContentType(WebContentPromotionAdGroupType.COLLECTION)
                        .withGeneralPrice(130.0)
                        .withKeywords(singletonList(new WebKeyword()
                                .withId(keyword.getId())
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
    public void saveContentPromotionCollectionAdGroup_UpdateTags_TagsUpdated() {
        List<Long> campaignTags = steps.tagCampaignSteps()
                .createDefaultTags(campaignInfo.getShard(), campaignInfo.getClientId(), campaignInfo.getCampaignId(),
                        3);
        AdGroupInfo adGroup =
                steps.adGroupSteps().createAdGroup(activeContentPromotionAdGroup(campaignInfo.getCampaignId(),
                        ContentPromotionAdgroupType.COLLECTION).withTags(campaignTags), campaignInfo);

        WebContentPromotionAdGroup webContentPromotionVideoAdGroup =
                randomNameWebContentPromotionVideoAdGroup(adGroup.getAdGroupId(), campaignInfo.getCampaignId())
                        .withAdGroupContentType(WebContentPromotionAdGroupType.COLLECTION)
                        .withTags(singletonMap(campaignTags.get(0).toString(), 1));

        updateAdGroups(singletonList(webContentPromotionVideoAdGroup));

        List<Long> tags = findTags(adGroup.getAdGroupId());
        assertThat("теги добавились", tags, containsInAnyOrder(campaignTags.get(0)));
    }

    @Test
    public void saveContentPromotionCollectionAdGroup_UpdatePageGroupTags_PageGroupTagsUpdated() {
        List<String> pageGroupTags = asList("page_group_tag1", "page_group_tag2");
        AdGroupInfo adGroupInfo =
                steps.adGroupSteps().createAdGroup(activeContentPromotionAdGroup(campaignInfo.getCampaignId(),
                        ContentPromotionAdgroupType.COLLECTION), campaignInfo);

        WebContentPromotionAdGroup webContentPromotionVideoAdGroup =
                randomNameWebContentPromotionVideoAdGroup(adGroupInfo.getAdGroupId(), campaignInfo.getCampaignId())
                        .withAdGroupContentType(WebContentPromotionAdGroupType.COLLECTION)
                        .withPageGroupTags(pageGroupTags);

        updateAdGroups(singletonList(webContentPromotionVideoAdGroup));

        AdGroup actualAdGroup = findAdGroup(adGroupInfo.getAdGroupId()).get(0);
        assertThat("statusBsSynced сбросился", actualAdGroup.getStatusBsSynced(), equalTo(StatusBsSynced.NO));
        assertThat("теги для таргетинга обновились", actualAdGroup.getPageGroupTags(), containsInAnyOrder("page_group_tag1", "page_group_tag2",
                getValueOfPageGroupOrTargetTagEnum(PageGroupTagEnum.CONTENT_PROMOTION_COLLECTION_TAG)));
    }

    @Test
    public void saveContentPromotionCollectionAdGroup_UpdateTargetTags_TargetTagsUpdated() {
        List<String> targetTags = asList("target_tag1", "target_tag2");
        AdGroupInfo adGroupInfo =
                steps.adGroupSteps().createAdGroup(activeContentPromotionAdGroup(campaignInfo.getCampaignId(),
                        ContentPromotionAdgroupType.COLLECTION), campaignInfo);

        WebContentPromotionAdGroup webContentPromotionVideoAdGroup =
                randomNameWebContentPromotionVideoAdGroup(adGroupInfo.getAdGroupId(), campaignInfo.getCampaignId())
                        .withAdGroupContentType(WebContentPromotionAdGroupType.COLLECTION)
                        .withTargetTags(targetTags);

        updateAdGroups(singletonList(webContentPromotionVideoAdGroup));

        AdGroup actualAdGroup = findAdGroup(adGroupInfo.getAdGroupId()).get(0);
        assertThat("statusBsSynced сбросился", actualAdGroup.getStatusBsSynced(), equalTo(StatusBsSynced.NO));
        assertThat("теги для таргетинга обновились", actualAdGroup.getTargetTags(), containsInAnyOrder("target_tag1", "target_tag2",
                getValueOfPageGroupOrTargetTagEnum(TargetTagEnum.CONTENT_PROMOTION_COLLECTION_TAG)));
    }

    @Test
    public void saveContentPromotionCollectionAdGroup_AddRelevanceMatch_RelevanceMatchAdded() {
        WebContentPromotionAdGroup webContentPromotionVideoAdGroup =
                randomNameWebContentPromotionVideoAdGroup(adGroupId, campaignInfo.getCampaignId())
                        .withAdGroupContentType(WebContentPromotionAdGroupType.COLLECTION)
                        .withRelevanceMatches(singletonList(webAdGroupRelevanceMatch(null)));

        updateAdGroups(singletonList(webContentPromotionVideoAdGroup));

        List<RelevanceMatch> relevanceMatches = findRelevanceMatches(adGroupId);
        assertThat("в группу должен добавиться бесфразный таргетинг", relevanceMatches, hasSize(1));
    }

    private void updateAdGroups(List<WebContentPromotionAdGroup> webContentPromotionVideoAdGroups) {
        WebResponse webResponse = controller
                .saveContentPromotionVideoAdGroup(webContentPromotionVideoAdGroups, campaignInfo.getCampaignId(), false,
                        true, false, null);
        checkResponse(webResponse);
    }
}
