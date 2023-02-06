package ru.yandex.direct.web.entity.adgroup.controller;

import java.util.List;

import com.google.common.collect.ListMultimap;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.banner.model.old.OldBanner;
import ru.yandex.direct.core.entity.banner.model.old.OldBannerPrice;
import ru.yandex.direct.core.entity.banner.model.old.OldBannerWithTurboLanding;
import ru.yandex.direct.core.entity.banner.model.old.OldTextBanner;
import ru.yandex.direct.core.entity.banner.turbolanding.model.OldBannerTurboLandingStatusModerate;
import ru.yandex.direct.core.entity.sitelink.model.Sitelink;
import ru.yandex.direct.core.entity.vcard.model.Vcard;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.TextBannerInfo;
import ru.yandex.direct.feature.FeatureName;
import ru.yandex.direct.web.configuration.DirectWebTest;
import ru.yandex.direct.web.entity.adgroup.model.WebTextAdGroup;
import ru.yandex.direct.web.entity.banner.model.WebBanner;
import ru.yandex.direct.web.entity.banner.model.WebBannerPrice;
import ru.yandex.direct.web.entity.banner.model.WebBannerSitelink;
import ru.yandex.direct.web.entity.banner.model.WebBannerTurbolanding;
import ru.yandex.direct.web.entity.banner.model.WebBannerVcard;

import static java.util.Arrays.asList;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.core.testing.steps.BannerPriceSteps.PRICE_COMPARE_STRATEGY;
import static ru.yandex.direct.core.testing.steps.BannerPriceSteps.defaultBannerPrice;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;
import static ru.yandex.direct.web.testing.data.TestAdGroups.randomNameWebAdGroup;
import static ru.yandex.direct.web.testing.data.TestBannerSitelinks.randomTitleWebSitelink;
import static ru.yandex.direct.web.testing.data.TestBannerVcards.randomHouseWebVcard;
import static ru.yandex.direct.web.testing.data.TestBanners.randomTitleWebTextBanner;

@DirectWebTest
@RunWith(SpringRunner.class)
public class AdGroupControllerUpdateAdGroupAddTextBannerTest extends TextAdGroupControllerTestBase {

    @Test
    public void update_AdGroupWithAddedBanner_AdGroupIsUpdated() {
        TextBannerInfo bannerInfo = steps.bannerSteps().createActiveTextBanner(campaignInfo);
        WebTextAdGroup requestAdGroup = updateAdGroupAndAddBanner(bannerInfo.getAdGroupInfo());

        List<AdGroup> actualAdGroups = findAdGroups();
        AdGroup actualUpdatedAdGroup = actualAdGroups.get(0);
        assertThat("группа не обновлена", actualUpdatedAdGroup.getName(), equalTo(requestAdGroup.getName()));
    }

    @Test
    public void update_AdGroupWithAddedBanner_BannerIsAdded() {
        AdGroupInfo adGroupInfo = steps.adGroupSteps().createActiveTextAdGroup(campaignInfo);

        WebTextAdGroup requestAdGroup = updateAdGroupAndAddBanner(adGroupInfo);

        List<OldBanner> actualBanners = findBanners();
        assertThat("должен быть один добавленный баннер", actualBanners, hasSize(1));

        OldBanner addedBanner = actualBanners.get(0);
        assertThat("данные баннера отличаются от ожидаемых",
                ((OldTextBanner) addedBanner).getTitle(),
                equalTo(requestAdGroup.getBanners().get(0).getTitle()));
        assertThat("баннер должен быть добавлен в соответствующую группу",
                addedBanner.getAdGroupId(), equalTo(adGroupInfo.getAdGroupId()));
    }

    private WebTextAdGroup updateAdGroupAndAddBanner(AdGroupInfo adGroupInfo) {
        WebTextAdGroup adGroupWithBanners = randomNameWebAdGroup(adGroupInfo.getAdGroupId(), null);
        WebBanner addedBanner = randomTitleWebTextBanner(null);

        adGroupWithBanners.withBanners(singletonList(addedBanner));

        updateAndCheckResult(singletonList(adGroupWithBanners));

        return adGroupWithBanners;
    }


    @Test
    public void update_AdGroupWithAddedBannerWithVcardAndSitelink_BannerIsAdded() {
        AdGroupInfo adGroupInfo = steps.adGroupSteps().createActiveTextAdGroup(campaignInfo);

        WebTextAdGroup requestAdGroup = updateAdGroupAndAddBannerWithVcardAndSitelink(adGroupInfo);

        List<OldBanner> actualBanners = findBanners();
        assertThat("должен быть один добавленный баннер", actualBanners, hasSize(1));

        OldBanner addedBanner = actualBanners.get(0);
        assertThat("данные добавленного баннера отличаются от ожидаемых",
                ((OldTextBanner) addedBanner).getTitle(),
                equalTo(requestAdGroup.getBanners().get(0).getTitle()));
        assertThat("баннер должен быть добавлен в соответствующую группу",
                addedBanner.getAdGroupId(), equalTo(adGroupInfo.getAdGroupId()));
    }

    @Test
    public void update_AdGroupWithAddedBannerWithVcardAndSitelink_VcardIsAdded() {
        AdGroupInfo adGroupInfo = steps.adGroupSteps().createActiveTextAdGroup(campaignInfo);

        WebTextAdGroup requestAdGroup = updateAdGroupAndAddBannerWithVcardAndSitelink(adGroupInfo);

        List<OldBanner> actualBanners = findBanners();
        assumeThat("в группе должен быть один добавленный баннер", actualBanners, hasSize(1));
        OldBanner addedBanner = actualBanners.get(0);

        List<Vcard> vcards = findVcards();
        assertThat("должна быть добавлена одна визитка", vcards, hasSize(1));

        assertThat("id добавленной визитки должен совпадать с id визитки в баннере",
                vcards.get(0).getId(), equalTo(((OldTextBanner) addedBanner).getVcardId()));
        assertThat("данные визитки отличаются от ожидаемых",
                vcards.get(0).getHouse(),
                equalTo(requestAdGroup.getBanners().get(0).getVcard().getHouse()));
    }

    @Test
    public void update_AdGroupWithAddedBannerWithVcardAndSitelink_SitelinkIsAdded() {
        AdGroupInfo adGroupInfo = steps.adGroupSteps().createActiveTextAdGroup(campaignInfo);

        WebTextAdGroup requestAdGroup = updateAdGroupAndAddBannerWithVcardAndSitelink(adGroupInfo);

        List<OldBanner> actualBanners = findBanners();
        assumeThat("в группе должен быть один добавленный баннер", actualBanners, hasSize(1));
        OldBanner addedBanner = actualBanners.get(0);

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
    public void update_AdGroupWithAddedBannerWithRarefiedSitelink_SitelinkIsAdded() {
        AdGroupInfo adGroupInfo = steps.adGroupSteps().createActiveTextAdGroup(campaignInfo);

        long adGroupId = adGroupInfo.getAdGroupId();

        WebTextAdGroup requestAdGroup = randomNameWebAdGroup(adGroupId, null);
        WebBannerSitelink requestSitelink = randomTitleWebSitelink();
        WebBanner requestBanner = randomTitleWebTextBanner(null)
                .withSitelinks(asList(null, requestSitelink, null, null));
        requestAdGroup.withBanners(singletonList(requestBanner));

        updateAndCheckResult(singletonList(requestAdGroup));

        List<OldBanner> actualBanners = findBanners();
        assumeThat("в группе должен быть один добавленный баннер", actualBanners, hasSize(1));
        OldBanner addedBanner = actualBanners.get(0);

        ListMultimap<Long, Sitelink> sitelinks = findSitelinks();
        assertThat("должен быть добавлен 1 набор сайтлинков", sitelinks.keys(), hasSize(1));
        assertThat("должен быть добавлен 1 сайтлинк", sitelinks.values(), hasSize(1));

        assertThat("id добавленного набора сайтлинков должен совпадать с id в баннере",
                sitelinks.keySet().iterator().next(),
                equalTo(((OldTextBanner) addedBanner).getSitelinksSetId()));
        assertThat("данные сайтлинка отличаются от ожидаемых",
                sitelinks.values().iterator().next().getTitle(),
                equalTo(requestSitelink.getTitle()));
    }

    private WebTextAdGroup updateAdGroupAndAddBannerWithVcardAndSitelink(AdGroupInfo adGroupInfo) {
        WebTextAdGroup adGroupWithBanners = randomNameWebAdGroup(adGroupInfo.getAdGroupId(), null);
        WebBanner addedBanner = randomTitleWebTextBanner(null);
        WebBannerVcard vcard = randomHouseWebVcard();
        WebBannerSitelink sitelink = randomTitleWebSitelink();

        addedBanner.withVcard(vcard)
                .withSitelinks(singletonList(sitelink));

        adGroupWithBanners.withBanners(singletonList(addedBanner));

        updateAndCheckResult(singletonList(adGroupWithBanners));

        return adGroupWithBanners;
    }


    @Test
    public void update_AdGroupWithBannersAndAdGroupWithUntouchedBanners_AdGroupIsUpdatedAndBannersAreNotTouched() {
        TextBannerInfo bannerInfo1 = steps.bannerSteps().createActiveTextBanner(campaignInfo);
        TextBannerInfo bannerInfo2 = steps.bannerSteps().createActiveTextBanner(campaignInfo);

        long adGroupUpdatedWithBannersId = bannerInfo1.getAdGroupId();
        long adGroupUpdatedWithoutBannersId = bannerInfo2.getAdGroupId();

        WebTextAdGroup requestAdGroupWithBanners = randomNameWebAdGroup(adGroupUpdatedWithBannersId, null);
        WebBanner requestUpdatedBanner = randomTitleWebTextBanner(bannerInfo1.getBannerId());
        WebBanner requestAddedBanner = randomTitleWebTextBanner(null);
        requestAdGroupWithBanners.withBanners(asList(requestUpdatedBanner, requestAddedBanner));

        WebTextAdGroup requestAdGroupWithoutBanners = randomNameWebAdGroup(adGroupUpdatedWithoutBannersId, null);

        updateAndCheckResult(asList(requestAdGroupWithBanners, requestAdGroupWithoutBanners));

        AdGroup actualAdGroupUpdatedWithoutBanners = adGroupRepository
                .getAdGroups(shard, singleton(adGroupUpdatedWithoutBannersId)).get(0);
        assertThat("группа, отправленная в запросе без баннеров, не обновлена",
                actualAdGroupUpdatedWithoutBanners.getName(),
                equalTo(requestAdGroupWithoutBanners.getName()));

        List<OldBanner> actualUntouchedBanners = bannerRepository
                .getBannersByGroupIds(shard, singletonList(adGroupUpdatedWithoutBannersId));
        assertThat("в группе должен быть один баннер", actualUntouchedBanners, hasSize(1));

        assertThat("данные незатронутого запросом баннера не должны измениться",
                ((OldTextBanner) actualUntouchedBanners.get(0)).getTitle(),
                equalTo(bannerInfo2.getBanner().getTitle()));
    }

    @Test
    public void update_AdGroupWithAddedBannerWithBannerPrice_BannerPriceIsAdded() {
        steps.featureSteps().addClientFeature(clientId, FeatureName.BANNER_PRICES, true);
        OldBannerPrice expectedPrice = defaultBannerPrice();

        WebBanner addedBanner = randomTitleWebTextBanner(null)
                .withBannerPrice(new WebBannerPrice()
                        .withPrice(expectedPrice.getPrice().toString())
                        .withCurrency(expectedPrice.getCurrency().name())
                        .withPriceOld(expectedPrice.getPriceOld().toString())
                        .withPrefix(expectedPrice.getPrefix().name()));

        AdGroupInfo adGroupInfo = steps.adGroupSteps().createActiveTextAdGroup(campaignInfo);
        WebTextAdGroup adGroupWithBanners = randomNameWebAdGroup(adGroupInfo.getAdGroupId(), null)
                .withBanners(singletonList(addedBanner));

        updateAndCheckResult(singletonList(adGroupWithBanners));

        List<OldBanner> actualBanners = findBanners();
        assumeThat("должен быть один добавленный баннер", actualBanners, hasSize(1));

        OldBannerPrice actualPrice = steps.bannerPriceSteps().getBannerPrice(adGroupInfo.getShard(),
                actualBanners.get(0).getId());
        assertThat("цена отличается от ожидаемой", actualPrice, beanDiffer(expectedPrice)
                .useCompareStrategy(PRICE_COMPARE_STRATEGY));
    }

    @Test
    public void update_AdGroupWithAddedBannerWithTurbolandingId_TurbolandingIdIsUpdated() {
        WebBannerTurbolanding webBannerTurbolanding =
                new WebBannerTurbolanding().withId(bannerTurboLandings.get(0).getId());
        WebBanner addedBanner = randomTitleWebTextBanner(null)
                .withTurbolanding(webBannerTurbolanding);

        AdGroupInfo adGroupInfo = steps.adGroupSteps().createActiveTextAdGroup(campaignInfo);
        WebTextAdGroup adGroupWithBanners = randomNameWebAdGroup(adGroupInfo.getAdGroupId(), null)
                .withBanners(singletonList(addedBanner));

        updateAndCheckResult(singletonList(adGroupWithBanners));

        List<OldBanner> actualBanners = findBanners();
        assumeThat("должен быть один добавленный баннер", actualBanners, hasSize(1));

        WebBannerTurbolanding updatedWebBannerTurbolanding =
                new WebBannerTurbolanding().withId(bannerTurboLandings.get(1).getId());
        addedBanner.withId(actualBanners.get(0).getId());
        addedBanner.withTurbolanding(updatedWebBannerTurbolanding);
        updateAndCheckResult(singletonList(adGroupWithBanners));

        List<OldBanner> updatedBanners = findBanners();
        assumeThat("должен быть один добавленный баннер", updatedBanners, hasSize(1));
        OldBannerWithTurboLanding updatedBanner = (OldBannerWithTurboLanding) updatedBanners.get(0);
        assumeThat("id турболендинга должен измениться", updatedBanner.getTurboLandingId(),
                is(updatedWebBannerTurbolanding.getId()));
        assumeThat("статус турболендинга беннера должен быть READY", updatedBanner.getTurboLandingStatusModerate(),
                is(OldBannerTurboLandingStatusModerate.READY));
    }

    @Test
    public void update_AdGroupWithAddedBannerWithTurbolandingId_TurbolandingIdNotUpdate() {
        WebBannerTurbolanding webBannerTurbolanding =
                new WebBannerTurbolanding().withId(bannerTurboLandings.get(0).getId());
        WebBanner addedBanner = randomTitleWebTextBanner(null)
                .withTurbolanding(webBannerTurbolanding);

        AdGroupInfo adGroupInfo = steps.adGroupSteps().createActiveTextAdGroup(campaignInfo);
        WebTextAdGroup adGroupWithBanners = randomNameWebAdGroup(adGroupInfo.getAdGroupId(), null)
                .withBanners(singletonList(addedBanner));

        updateAndCheckResult(singletonList(adGroupWithBanners));

        List<OldBanner> actualBanners = findBanners();
        assumeThat("должен быть один добавленный баннер", actualBanners, hasSize(1));

        // При попытке обновления тех же параметров ничего не должно измениться
        addedBanner.withId(actualBanners.get(0).getId());
        addedBanner.withTurbolanding(webBannerTurbolanding);
        updateAndCheckResult(singletonList(adGroupWithBanners));

        List<OldBanner> updatedBanners = findBanners();
        assumeThat("должен быть один добавленный баннер", updatedBanners, hasSize(1));
        OldBannerWithTurboLanding updatedBanner = (OldBannerWithTurboLanding) updatedBanners.get(0);
        assumeThat("id турболендинга не должен измениться", updatedBanner.getTurboLandingId(),
                is(webBannerTurbolanding.getId()));
        assumeThat("статус турболендинга баннера должен быть READY", updatedBanner.getTurboLandingStatusModerate(),
                is(OldBannerTurboLandingStatusModerate.READY));
    }
}
