package ru.yandex.direct.web.entity.adgroup.controller.contentpromotionvideo;

import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.adgroup.model.PageGroupTagEnum;
import ru.yandex.direct.core.entity.adgroup.model.TargetTagEnum;
import ru.yandex.direct.core.entity.banner.model.old.OldBanner;
import ru.yandex.direct.core.entity.bidmodifier.BidModifier;
import ru.yandex.direct.core.entity.contentpromotion.model.ContentPromotionContent;
import ru.yandex.direct.core.entity.contentpromotion.model.ContentPromotionContentType;
import ru.yandex.direct.core.entity.contentpromotion.repository.ContentPromotionRepository;
import ru.yandex.direct.web.configuration.DirectWebTest;
import ru.yandex.direct.web.core.model.WebResponse;
import ru.yandex.direct.web.entity.adgroup.model.WebContentPromotionAdGroup;
import ru.yandex.direct.web.entity.banner.model.WebContentPromotionBanner;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;
import static ru.yandex.direct.web.testing.data.TestAdGroups.randomNameWebContentPromotionCollectionAdGroup;
import static ru.yandex.direct.web.testing.data.TestAdGroups.webAdGroupRelevanceMatch;
import static ru.yandex.direct.web.testing.data.TestBanners.webContentPromotionCollectionBanner;
import static ru.yandex.direct.web.testing.data.TestBidModifiers.fullContentPromotionAdGroupBidModifiers;
import static ru.yandex.direct.web.testing.data.TestKeywords.randomPhraseKeyword;

@DirectWebTest
@RunWith(SpringRunner.class)
public class ContentPromotionCollectionsAdGroupAddTest extends ContentPromotionVideoAdGroupControllerTestBase {

    @Autowired
    private ContentPromotionRepository contentPromotionRepository;

    private Long contentId;

    @Before
    public void before() {
        super.before();
        contentId = contentPromotionRepository.insertContentPromotion(clientId,
                new ContentPromotionContent()
                        .withExternalId("EXTERNAL")
                        .withUrl(VIDEO_HREF)
                        .withIsInaccessible(false)
                        .withType(ContentPromotionContentType.COLLECTION));
    }

    @Test
    public void saveContentPromotionVideoAdGroup_EmptyAdGroup() {
        WebContentPromotionAdGroup requestAdGroup = randomNameWebContentPromotionCollectionAdGroup(null,
                campaignInfo.getCampaignId());

        addAdGroups(singletonList(requestAdGroup));

        List<AdGroup> adGroups = findAdGroups();
        assertThat("должна быть добавлена одна группа", adGroups, hasSize(1));
        assertThat("имя добавленной группы не совпадает с ожидаемым",
                adGroups.get(0).getName(), is(requestAdGroup.getName()));
    }

    @Test
    public void saveContentPromotionCollectionAdGroup_AdGroupWithBanner() {
        WebContentPromotionBanner webBanner = webContentPromotionCollectionBanner(null, contentId);
        WebContentPromotionAdGroup requestAdGroup =
                randomNameWebContentPromotionCollectionAdGroup(null, campaignInfo.getCampaignId())
                        .withBanners(singletonList(webBanner));

        addAdGroups(singletonList(requestAdGroup));

        List<AdGroup> adGroups = findAdGroups();
        assertThat("должна быть добавлена одна группа", adGroups, hasSize(1));

        checkBannerContentPromotion(adGroups.get(0).getId(), webBanner);
    }

    @Test
    public void saveContentPromotionCollectionAdGroup_AdGroupWithKeywords() {
        WebContentPromotionAdGroup requestAdGroup =
                randomNameWebContentPromotionCollectionAdGroup(null, campaignInfo.getCampaignId())
                        .withKeywords(singletonList(randomPhraseKeyword(null)));

        addAdGroups(singletonList(requestAdGroup));

        List<AdGroup> adGroups = findAdGroups();
        assertThat("должна быть добавлена одна группа", adGroups, hasSize(1));
        checkKeywords(requestAdGroup.getKeywords());
    }

    @Test
    public void saveContentPromotionCollectionAdGroup_WithKeywordsAndBidModifiers_BidModifiersAreSavedCorrectly() {
        WebContentPromotionAdGroup requestAdGroup =
                randomNameWebContentPromotionCollectionAdGroup(null, campaignInfo.getCampaignId())
                        .withKeywords(singletonList(randomPhraseKeyword(null)))
                        .withBidModifiers(fullContentPromotionAdGroupBidModifiers(retCondId));

        addAdGroups(singletonList(requestAdGroup));

        List<AdGroup> adGroups = findAdGroups();
        assertThat("должна быть добавлена одна группа", adGroups, hasSize(1));
        List<BidModifier> bidModifiers = findBidModifiers();
        assertThat("должно быть добавлено 3 корректировки", bidModifiers, hasSize(3));

        checkRetargetingBidModifier(bidModifiers, adGroups.get(0).getId(),
                requestAdGroup.getBidModifiers().getRetargetingBidModifier());

        checkDemographyBidModifier(bidModifiers, adGroups.get(0).getId(),
                requestAdGroup.getBidModifiers().getDemographicsBidModifier());

        checkMobileBidModifier(bidModifiers, adGroups.get(0).getId(),
                requestAdGroup.getBidModifiers().getMobileBidModifier());
    }

    @Test
    public void saveContentPromotionCollectionAdGroup_AdGroupWithTags_TagsAreSavedCorrectly() {
        List<Long> campaignTags = steps.tagCampaignSteps()
                .createDefaultTags(campaignInfo.getShard(), campaignInfo.getClientId(), campaignInfo.getCampaignId(),
                        1);

        WebContentPromotionAdGroup requestAdGroup =
                randomNameWebContentPromotionCollectionAdGroup(null, campaignInfo.getCampaignId())
                        .withTags(Collections.singletonMap(campaignTags.get(0).toString(), 1));

        addAdGroups(singletonList(requestAdGroup));

        List<AdGroup> adGroups = findAdGroups();
        assertThat("должна быть добавлена одна группа", adGroups, hasSize(1));
        List<Long> tags = findTags(adGroups.get(0).getId());
        assertThat(tags, containsInAnyOrder(campaignTags.toArray()));
    }

    @Test
    public void saveContentPromotionCollectionAdGroup_AdGroupWithPageGroupTags_PageGroupTagsAreSavedCorrectly() {
        WebContentPromotionAdGroup requestAdGroup =
                randomNameWebContentPromotionCollectionAdGroup(null, campaignInfo.getCampaignId())
                        .withPageGroupTags(asList("page_group_tag1", "page_group_tag2"));

        addAdGroups(singletonList(requestAdGroup));

        List<AdGroup> adGroups = findAdGroups();
        assertThat("должна быть добавлена одна группа", adGroups, hasSize(1));
        List<String> pageGroupTags = adGroups.get(0).getPageGroupTags();
        assertThat(pageGroupTags, containsInAnyOrder("page_group_tag1", "page_group_tag2",
                getValueOfPageGroupOrTargetTagEnum(PageGroupTagEnum.CONTENT_PROMOTION_COLLECTION_TAG)));
    }

    @Test
    public void saveContentPromotionCollectionAdGroup_AdGroupWithTargetTags_TargetTagsAreSavedCorrectly() {
        WebContentPromotionAdGroup requestAdGroup =
                randomNameWebContentPromotionCollectionAdGroup(null, campaignInfo.getCampaignId())
                        .withTargetTags(asList("target_tag1", "target_tag2"));

        addAdGroups(singletonList(requestAdGroup));

        List<AdGroup> adGroups = findAdGroups();
        assertThat("должна быть добавлена одна группа", adGroups, hasSize(1));
        List<String> targetTags = adGroups.get(0).getTargetTags();
        assertThat(targetTags, containsInAnyOrder("target_tag1", "target_tag2",
                getValueOfPageGroupOrTargetTagEnum(TargetTagEnum.CONTENT_PROMOTION_COLLECTION_TAG)));
    }

    @Test
    public void saveContentPromotionCollectionAdGroup_AdGroupWithMinusWords_MinusWordsAreSavedCorrectly() {
        WebContentPromotionAdGroup requestAdGroup =
                randomNameWebContentPromotionCollectionAdGroup(null, campaignInfo.getCampaignId())
                        .withMinusKeywords(asList("два", "три"));

        addAdGroups(singletonList(requestAdGroup));

        List<AdGroup> adGroups = findAdGroups();
        assertThat("должна быть добавлена одна группа", adGroups, hasSize(1));
        List<String> minusWords = adGroups.get(0).getMinusKeywords();
        assertThat(minusWords, is(asList("два", "три")));
    }

    @Test
    public void saveContentPromotionCollectionAdGroup_FullAdGroup_Success() {
        List<Long> campaignTags = steps.tagCampaignSteps()
                .createDefaultTags(campaignInfo.getShard(), campaignInfo.getClientId(), campaignInfo.getCampaignId(),
                        1);

        WebContentPromotionAdGroup requestAdGroup =
                randomNameWebContentPromotionCollectionAdGroup(null, campaignInfo.getCampaignId())
                        .withKeywords(singletonList(randomPhraseKeyword(null)))
                        .withBidModifiers(fullContentPromotionAdGroupBidModifiers(retCondId))
                        .withRelevanceMatches(singletonList(webAdGroupRelevanceMatch(null)))
                        .withGeneralPrice(50.0)
                        .withMinusKeywords(asList("два", "три"))
                        .withTags(Collections.singletonMap(campaignTags.get(0).toString(), 1))
                        .withPageGroupTags(asList("page_group_tag1", "page_group_tag2"))
                        .withTargetTags(asList("target_tag1", "target_tag2"))
                        .withGeo("225")
                        .withBanners(singletonList(webContentPromotionCollectionBanner(null, contentId)));

        addAdGroups(singletonList(requestAdGroup));
    }

    @Test
    public void saveContentPromotionCollectionAdGroup_BannerWithoutPackshotHref_NoPackshotHrefIsSaved() {
        WebContentPromotionBanner webBanner = webContentPromotionCollectionBanner(null, contentId)
                .withVisitUrl(null);
        WebContentPromotionAdGroup requestAdGroup =
                randomNameWebContentPromotionCollectionAdGroup(null, campaignInfo.getCampaignId())
                        .withBanners(singletonList(webBanner));

        addAdGroups(singletonList(requestAdGroup));

        List<AdGroup> adGroups = findAdGroups();
        assertThat("должна быть добавлена одна группа", adGroups, hasSize(1));

        checkBannerContentPromotion(adGroups.get(0).getId(), webBanner);
    }

    @Test
    public void saveContentPromotionCollectionAdGroup_WithContentPromotionVideoId_DomainAndReverseDomainAreCorrect() {
        WebContentPromotionBanner webBanner = webContentPromotionCollectionBanner(null, contentId);
        WebContentPromotionAdGroup requestAdGroup =
                randomNameWebContentPromotionCollectionAdGroup(null, campaignInfo.getCampaignId())
                        .withBanners(singletonList(webBanner));

        addAdGroups(singletonList(requestAdGroup));

        assumeThat("должна быть добавлена одна группа", findAdGroups(), hasSize(1));
        List<OldBanner> banners = findBanners();
        assumeThat("должен быть добавлен один баннер", banners, hasSize(1));

        assertThat("у баннера установлен правильный домен",
                banners.get(0).getDomain(), equalTo("www.youtube.com"));
        assertThat("у баннера установлен правильный обратный домен",
                banners.get(0).getReverseDomain(), equalTo("moc.ebutuoy.www"));
    }

    private void addAdGroups(List<WebContentPromotionAdGroup> adGroups) {
        WebResponse webResponse =
                controller.saveContentPromotionVideoAdGroup(adGroups, campaignInfo.getCampaignId(), true, true, false,
                        null);
        checkResponse(webResponse);
    }
}
