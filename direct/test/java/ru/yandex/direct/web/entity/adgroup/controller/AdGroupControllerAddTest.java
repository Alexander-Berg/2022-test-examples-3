package ru.yandex.direct.web.entity.adgroup.controller;

import java.util.List;

import com.google.common.collect.ListMultimap;
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
import ru.yandex.direct.core.entity.banner.model.old.OldBanner;
import ru.yandex.direct.core.entity.banner.model.old.OldTextBanner;
import ru.yandex.direct.core.entity.banner.turbolanding.model.OldBannerTurboLandingStatusModerate;
import ru.yandex.direct.core.entity.bidmodifier.BidModifier;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierVideo;
import ru.yandex.direct.core.entity.sitelink.model.Sitelink;
import ru.yandex.direct.core.entity.vcard.model.Vcard;
import ru.yandex.direct.web.configuration.DirectWebTest;
import ru.yandex.direct.web.core.model.WebResponse;
import ru.yandex.direct.web.entity.adgroup.model.WebAdGroupBidModifiers;
import ru.yandex.direct.web.entity.adgroup.model.WebAdGroupRelevanceMatch;
import ru.yandex.direct.web.entity.adgroup.model.WebAdGroupRetargeting;
import ru.yandex.direct.web.entity.adgroup.model.WebTextAdGroup;
import ru.yandex.direct.web.entity.banner.model.WebBanner;
import ru.yandex.direct.web.entity.banner.model.WebBannerSitelink;
import ru.yandex.direct.web.entity.banner.model.WebBannerTurbolanding;
import ru.yandex.direct.web.entity.banner.model.WebBannerVcard;
import ru.yandex.direct.web.entity.keyword.model.WebKeyword;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.integrations.configuration.IntegrationsConfiguration.BS_TRAFARET_AUCTION_CLIENT;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;
import static ru.yandex.direct.web.testing.data.TestAdGroups.adGroupRetargeting;
import static ru.yandex.direct.web.testing.data.TestAdGroups.randomNameWebAdGroup;
import static ru.yandex.direct.web.testing.data.TestAdGroups.webAdGroupRelevanceMatch;
import static ru.yandex.direct.web.testing.data.TestBannerSitelinks.randomTitleWebSitelink;
import static ru.yandex.direct.web.testing.data.TestBannerVcards.randomHouseWebVcard;
import static ru.yandex.direct.web.testing.data.TestBanners.randomTitleWebTextBanner;
import static ru.yandex.direct.web.testing.data.TestBidModifiers.fullWebAdGroupBidModifiers;
import static ru.yandex.direct.web.testing.data.TestBidModifiers.fullWebAdGroupBidModifiersWithDevice;
import static ru.yandex.direct.web.testing.data.TestKeywords.randomPhraseKeyword;

@DirectWebTest
@RunWith(SpringRunner.class)
public class AdGroupControllerAddTest extends TextAdGroupControllerTestBase {

    @Autowired
    @Qualifier(BS_TRAFARET_AUCTION_CLIENT)
    private BsTrafaretClient bsTrafaretClient;

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

    @Test
    public void emptyAdGroupIsAdded() {
        WebTextAdGroup requestAdGroup = randomNameWebAdGroup(null, campaignInfo.getCampaignId());

        addAndCheckResult(singletonList(requestAdGroup));

        List<AdGroup> addedAdGroups = findAdGroups();
        assertThat("должна быть добавлена одна группа", addedAdGroups, hasSize(1));
        assertThat("имя добавленной группы не совпадает с ожидаемым",
                addedAdGroups.get(0).getName(), equalTo(requestAdGroup.getName()));
    }

    @Test
    public void adGroupWithPageGroupTagsAdded() {
        List<String> pageGroupTags = asList("page_group_tag1", "page_group_tag2");

        WebTextAdGroup requestAdGroup = randomNameWebAdGroup(null, campaignInfo.getCampaignId())
                .withPageGroupTags(pageGroupTags);

        addAndCheckResult(singletonList(requestAdGroup));

        List<AdGroup> adGroups = findAdGroups();
        assertThat("должна быть добавлена одна группа", adGroups, hasSize(1));
        List<String> actualPageGroupTags = adGroups.get(0).getPageGroupTags();
        assertThat(actualPageGroupTags, containsInAnyOrder(pageGroupTags.toArray()));
    }

    @Test
    public void adGroupWithTargetTagsAdded() {
        List<String> targetTags = asList("target_tag1", "target_tag2");

        WebTextAdGroup requestAdGroup = randomNameWebAdGroup(null, campaignInfo.getCampaignId())
                .withTargetTags(targetTags);

        addAndCheckResult(singletonList(requestAdGroup));

        List<AdGroup> adGroups = findAdGroups();
        assertThat("должна быть добавлена одна группа", adGroups, hasSize(1));
        List<String> actualTargetTags = adGroups.get(0).getTargetTags();
        assertThat(actualTargetTags, containsInAnyOrder(targetTags.toArray()));
    }

    @Test
    public void adGroupIsAddedWhenFullAdGroupIsSent() {
        WebTextAdGroup requestAdGroup = addFullAdGroupAndCheckResult();

        List<AdGroup> addedAdGroups = findAdGroups();
        assertThat("должна быть добавлена одна группа", addedAdGroups, hasSize(1));
        assertThat("данные добавленной группы не совпадает с ожидаемыми",
                addedAdGroups.get(0).getName(), equalTo(requestAdGroup.getName()));
    }

    @Test
    public void bannerIsAddedWhenFullAdGroupIsSent() {
        WebTextAdGroup requestAdGroup = addFullAdGroupAndCheckResult();

        List<AdGroup> addedAdGroups = findAdGroups();
        assumeThat("должна быть добавлена одна группа", addedAdGroups, hasSize(1));

        List<OldBanner> actualBanners = findBanners();
        assertThat("должен быть один добавленный баннер", actualBanners, hasSize(1));

        OldBanner addedBanner = actualBanners.get(0);
        assertThat("данные добавленного баннера отличаются от ожидаемых",
                ((OldTextBanner) addedBanner).getTitle(),
                equalTo(requestAdGroup.getBanners().get(0).getTitle()));
        assertThat("баннер должен быть добавлен в соответствующую группу",
                addedBanner.getAdGroupId(), equalTo(addedAdGroups.get(0).getId()));
        assertThat("к баннеру должен быть добавлен турболендинг",
                ((OldTextBanner) addedBanner).getTurboLandingId(),
                equalTo(bannerTurboLandings.get(0).getId()));
        assertThat("к баннеру должен быть добавлен статус модерации турболендинга",
                ((OldTextBanner) addedBanner).getTurboLandingStatusModerate(),
                equalTo(OldBannerTurboLandingStatusModerate.READY));
    }

    @Test
    public void vcardIsAddedWhenFullAdGroupIsSent() {
        WebTextAdGroup requestAdGroup = addFullAdGroupAndCheckResult();

        List<OldBanner> actualBanners = findBanners();
        assumeThat("должен быть один добавленный баннер", actualBanners, hasSize(1));

        OldBanner addedBanner = actualBanners.get(0);
        assertThat("в баннере должен быть установлен id визитки",
                ((OldTextBanner) addedBanner).getVcardId(), notNullValue());

        List<Vcard> vcards = findVcards();
        assertThat("должна быть добавлена одна визитка", vcards, hasSize(1));

        assertThat("id добавленной визитки должен совпадать с id визитки в баннере",
                vcards.get(0).getId(), equalTo(((OldTextBanner) addedBanner).getVcardId()));
        assertThat("данные визитки отличаются от ожидаемых",
                vcards.get(0).getHouse(),
                equalTo(requestAdGroup.getBanners().get(0).getVcard().getHouse()));
    }

    @Test
    public void sitelinkSetIsAddedWhenFullAdGroupIsSent() {
        WebTextAdGroup requestAdGroup = addFullAdGroupAndCheckResult();

        List<OldBanner> actualBanners = findBanners();
        assumeThat("должен быть один добавленный баннер", actualBanners, hasSize(1));

        OldBanner addedBanner = actualBanners.get(0);
        assertThat("в баннере должен быть установлен id набора сайтлинков",
                ((OldTextBanner) addedBanner).getSitelinksSetId(), notNullValue());

        ListMultimap<Long, Sitelink> sitelinks = findSitelinks();
        assertThat("должен быть добавлен 1 набор сайтлинков", sitelinks.keys(), hasSize(1));
        assertThat("должен быть добавлен 1 сайтлинк", sitelinks.values(), hasSize(1));

        assertThat("id добавленного набора сайтлинков должен совпадать с id в баннере",
                sitelinks.keySet().iterator().next(),
                equalTo(((OldTextBanner) addedBanner).getSitelinksSetId()));
        assertThat("данные сайтлинка отличаются от ожидаемых",
                sitelinks.values().iterator().next().getTitle(),
                equalTo(requestAdGroup.getBanners().get(0).getSitelinks().get(0).getTitle()));
    }

    @Test
    public void retargetingBidModifierIsAddedWhenFullAdGroupIsSent() {
        WebTextAdGroup requestAdGroup = addFullAdGroupAndCheckResult();
        long addedAdGroupId = getAddedAdGroupId();

        List<BidModifier> actualBidModifiers = findBidModifiers();
        assertThat("должно быть добавлено 4 корректировки", actualBidModifiers, hasSize(4));

        checkRetargetingBidModifier(actualBidModifiers, addedAdGroupId,
                requestAdGroup.getBidModifiers().getRetargetingBidModifier());
    }

    @Test
    public void demographyBidModifierIsAddedWhenFullAdGroupIsSent() {
        WebTextAdGroup requestAdGroup = addFullAdGroupAndCheckResult();
        long addedAdGroupId = getAddedAdGroupId();

        List<BidModifier> actualBidModifiers = findBidModifiers();
        assertThat("должно быть добавлено 4 корректировки", actualBidModifiers, hasSize(4));

        checkDemographyBidModifier(actualBidModifiers, addedAdGroupId,
                requestAdGroup.getBidModifiers().getDemographicsBidModifier());
    }

    @Test
    public void mobileBidModifierIsAddedWhenFullAdGroupIsSent() {
        WebTextAdGroup requestAdGroup = addFullAdGroupAndCheckResult();
        long addedAdGroupId = getAddedAdGroupId();

        List<BidModifier> actualBidModifiers = findBidModifiers();
        assertThat("должно быть добавлено 4 корректировки", actualBidModifiers, hasSize(4));

        checkMobileBidModifier(actualBidModifiers, addedAdGroupId,
                requestAdGroup.getBidModifiers().getMobileBidModifier());
    }

    @Test
    public void mobileIosBidModifierIsAddedWhenFullAdGroupIsSent() {
        WebTextAdGroup requestAdGroup = addFullAdGroupWithDeviceModifiersAndCheckResult();
        long addedAdGroupId = getAddedAdGroupId();

        List<BidModifier> actualBidModifiers = findBidModifiers();
        assertThat("должно быть добавлено 5 корректировки", actualBidModifiers, hasSize(5));

        checkMobileBidModifier(actualBidModifiers, addedAdGroupId,
                requestAdGroup.getBidModifiers().getMobileBidModifier());
    }

    @Test
    public void desktopBidModifierIsAddedWhenFullAdGroupIsSent() {
        WebTextAdGroup requestAdGroup = addFullAdGroupWithDeviceModifiersAndCheckResult();
        long addedAdGroupId = getAddedAdGroupId();

        List<BidModifier> actualBidModifiers = findBidModifiers();
        assertThat("должно быть добавлено 5 корректировки", actualBidModifiers, hasSize(5));

        checkDesktopBidModifier(actualBidModifiers, addedAdGroupId,
                requestAdGroup.getBidModifiers().getDesktopBidModifier());
    }

    @Test
    public void videoBidModifierIsAddedWhenFullAdGroupIsSent() {
        WebTextAdGroup requestAdGroup = addFullAdGroupAndCheckResult();
        long addedAdGroupId = getAddedAdGroupId();

        List<BidModifier> actualBidModifiers = findBidModifiers();
        assertThat("должно быть добавлено 4 корректировки", actualBidModifiers, hasSize(4));

        BidModifierVideo bidModifierVideo =
                extractOnlyOneBidModifierOfType(actualBidModifiers, BidModifierVideo.class);

        assertThat("видео-корректировка должна быть привязана к добавляемой группе",
                bidModifierVideo.getAdGroupId(),
                equalTo(addedAdGroupId));
        Integer expectedPercent = requestAdGroup.getBidModifiers()
                .getVideoBidModifier().getPercent();
        assertThat("данные видео-корректировки отличаются от ожидаемых",
                bidModifierVideo.getVideoAdjustment().getPercent(),
                equalTo(expectedPercent));
    }

    private WebTextAdGroup addFullAdGroupWithDeviceModifiersAndCheckResult() {
        WebAdGroupBidModifiers requestBidModifiers = fullWebAdGroupBidModifiersWithDevice(retCondId);
        return addFullAdGroupAndCheckResult(requestBidModifiers);
    }

    private WebTextAdGroup addFullAdGroupAndCheckResult() {
        WebAdGroupBidModifiers requestBidModifiers = fullWebAdGroupBidModifiers(retCondId);
        return addFullAdGroupAndCheckResult(requestBidModifiers);
    }

    private WebTextAdGroup addFullAdGroupAndCheckResult(WebAdGroupBidModifiers requestBidModifiers) {
        WebBannerTurbolanding webBannerTurbolanding =
                new WebBannerTurbolanding().withId(bannerTurboLandings.get(0).getId());
        steps.retConditionSteps().createDefaultRetCondition();
        WebTextAdGroup requestAdGroup = randomNameWebAdGroup(null, campaignInfo.getCampaignId());
        WebBanner requestBanner = randomTitleWebTextBanner(null).withTurbolanding(webBannerTurbolanding);
        WebBannerVcard requestVcard = randomHouseWebVcard();
        WebBannerSitelink requestSitelink = randomTitleWebSitelink();
        WebKeyword requestKeyword = randomPhraseKeyword(null);
        WebAdGroupRelevanceMatch requestRelMatch = webAdGroupRelevanceMatch(null);

        WebAdGroupRetargeting requestRetargeting = adGroupRetargeting(retCondId);

        requestBanner.withVcard(requestVcard)
                .withSitelinks(singletonList(requestSitelink));

        requestAdGroup.withKeywords(singletonList(requestKeyword))
                .withRelevanceMatches(singletonList(requestRelMatch))
                .withRetargetings(singletonList(requestRetargeting))
                .withBidModifiers(requestBidModifiers)
                .withBanners(singletonList(requestBanner));

        addAndCheckResult(singletonList(requestAdGroup));

        return requestAdGroup;
    }

    private void addAndCheckResult(List<WebTextAdGroup> requestAdGroups) {
        WebResponse response = controller.saveTextAdGroup(requestAdGroups, campaignInfo.getCampaignId(),
                true, false, false, null, null);
        checkResponse(response);
    }

    private long getAddedAdGroupId() {
        List<AdGroup> addedAdGroups = findAdGroups();
        assumeThat("должна быть добавлена 1 группа", addedAdGroups, hasSize(1));
        return addedAdGroups.get(0).getId();
    }
}
