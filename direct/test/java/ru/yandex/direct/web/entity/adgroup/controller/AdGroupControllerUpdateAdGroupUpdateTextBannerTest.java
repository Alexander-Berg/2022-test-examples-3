package ru.yandex.direct.web.entity.adgroup.controller;

import java.util.List;

import com.google.common.collect.ListMultimap;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.banner.model.old.OldBanner;
import ru.yandex.direct.core.entity.banner.model.old.OldBannerPrice;
import ru.yandex.direct.core.entity.banner.model.old.OldTextBanner;
import ru.yandex.direct.core.entity.sitelink.model.Sitelink;
import ru.yandex.direct.core.entity.vcard.model.Vcard;
import ru.yandex.direct.core.testing.info.TextBannerInfo;
import ru.yandex.direct.feature.FeatureName;
import ru.yandex.direct.web.configuration.DirectWebTest;
import ru.yandex.direct.web.entity.adgroup.model.WebTextAdGroup;
import ru.yandex.direct.web.entity.banner.model.WebBanner;
import ru.yandex.direct.web.entity.banner.model.WebBannerPrice;
import ru.yandex.direct.web.entity.banner.model.WebBannerSitelink;
import ru.yandex.direct.web.entity.banner.model.WebBannerVcard;

import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.core.testing.data.TestBanners.activeTextBanner;
import static ru.yandex.direct.core.testing.steps.BannerPriceSteps.PRICE_COMPARE_STRATEGY;
import static ru.yandex.direct.core.testing.steps.BannerPriceSteps.defaultBannerPrice;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;
import static ru.yandex.direct.web.testing.data.TestAdGroups.randomNameWebAdGroup;
import static ru.yandex.direct.web.testing.data.TestBannerSitelinks.randomTitleWebSitelink;
import static ru.yandex.direct.web.testing.data.TestBannerVcards.randomHouseWebVcard;
import static ru.yandex.direct.web.testing.data.TestBanners.randomTitleWebTextBanner;

@DirectWebTest
@RunWith(SpringRunner.class)
public class AdGroupControllerUpdateAdGroupUpdateTextBannerTest extends TextAdGroupControllerTestBase {

    @Test
    public void update_AdGroupWithUpdatedBanner_BannerIsUpdated() {
        TextBannerInfo bannerInfo = steps.bannerSteps().createActiveTextBanner(campaignInfo);
        long adGroupId = bannerInfo.getAdGroupId();
        long bannerId = bannerInfo.getBannerId();

        WebTextAdGroup requestAdGroup = updateAdGroupAndUpdateBanner(adGroupId, bannerId);

        List<OldBanner> actualBanners = findBanners();
        OldBanner addedBanner = actualBanners.get(0);

        assertThat("данные баннера отличаются от ожидаемых",
                ((OldTextBanner) addedBanner).getTitle(),
                equalTo(requestAdGroup.getBanners().get(0).getTitle()));
    }

    private WebTextAdGroup updateAdGroupAndUpdateBanner(long adGroupId, long bannerId) {
        WebTextAdGroup adGroupWithBanners = randomNameWebAdGroup(adGroupId, null);
        WebBanner addedBanner = randomTitleWebTextBanner(bannerId);

        adGroupWithBanners.withBanners(singletonList(addedBanner));

        updateAndCheckResult(singletonList(adGroupWithBanners));

        return adGroupWithBanners;
    }


    @Test
    public void update_AdGroupWithUpdatedBannerWithAddedVcardAndSitelinks_VcardIsAdded() {
        TextBannerInfo bannerInfo = steps.bannerSteps().createActiveTextBanner(campaignInfo);

        WebTextAdGroup requestAdGroup = updateAdGroupAndUpdateBannerWithVcardAndSitelink(bannerInfo);

        List<OldBanner> actualBanners = findBanners();
        OldBanner updatedBanner = actualBanners.get(0);

        List<Vcard> vcards = findVcards();
        assertThat("должна быть добавлена одна визитка", vcards, hasSize(1));

        assertThat("id добавленной визитки должен совпадать с id визитки в баннере",
                vcards.get(0).getId(), equalTo(((OldTextBanner) updatedBanner).getVcardId()));
        assertThat("данные визитки отличаются от ожидаемых",
                vcards.get(0).getHouse(),
                equalTo(requestAdGroup.getBanners().get(0).getVcard().getHouse()));
    }

    @Test
    public void update_AdGroupWithUpdatedBannerWithAddedVcardAndSitelinks_SitelinkSetIsAdded() {
        TextBannerInfo bannerInfo = steps.bannerSteps().createActiveTextBanner(campaignInfo);

        WebTextAdGroup requestAdGroup = updateAdGroupAndUpdateBannerWithVcardAndSitelink(bannerInfo);

        List<OldBanner> actualBanners = findBanners();
        OldBanner updatedBanner = actualBanners.get(0);

        ListMultimap<Long, Sitelink> sitelinks = findSitelinks();
        assertThat("должен быть добавлен 1 набор сайтлинков", sitelinks.keys(), hasSize(1));
        assertThat("должен быть добавлен 1 сайтлинк", sitelinks.values(), hasSize(1));

        assertThat("id добавленного набора сайтлинков должен совпадать с id в баннере",
                sitelinks.keySet().iterator().next(),
                equalTo(((OldTextBanner) updatedBanner).getSitelinksSetId()));
        assertThat("данные сайтлинка отличаются от ожидаемых",
                sitelinks.values().iterator().next().getTitle(),
                equalTo(requestAdGroup.getBanners().get(0).getSitelinks().get(0).getTitle()));
    }

    private WebTextAdGroup updateAdGroupAndUpdateBannerWithVcardAndSitelink(TextBannerInfo bannerInfo) {
        WebTextAdGroup adGroupWithBanners = randomNameWebAdGroup(bannerInfo.getAdGroupId(), null);
        WebBanner addedBanner = randomTitleWebTextBanner(bannerInfo.getBannerId());
        WebBannerVcard vcard = randomHouseWebVcard();
        WebBannerSitelink sitelink = randomTitleWebSitelink();

        addedBanner.withVcard(vcard)
                .withSitelinks(singletonList(sitelink));

        adGroupWithBanners.withBanners(singletonList(addedBanner));

        updateAndCheckResult(singletonList(adGroupWithBanners));

        return adGroupWithBanners;
    }

    @Test
    public void update_AdGroupWithUpdatedBannerWithBannerPrice_BannerPriceIsAdded() {
        steps.featureSteps().addClientFeature(clientId, FeatureName.BANNER_PRICES, true);
        OldBannerPrice expectedPrice = defaultBannerPrice();

        TextBannerInfo bannerInfo = steps.bannerSteps().createActiveTextBanner(campaignInfo);
        assumeThat(steps.bannerPriceSteps().getBannerPrice(bannerInfo), nullValue());

        long adGroupId = bannerInfo.getAdGroupId();
        long bannerId = bannerInfo.getBannerId();
        WebBanner addedBanner = randomTitleWebTextBanner(bannerId)
                .withBannerPrice(new WebBannerPrice()
                        .withPrice(expectedPrice.getPrice().toString())
                        .withCurrency(expectedPrice.getCurrency().name())
                        .withPriceOld(expectedPrice.getPriceOld().toString())
                        .withPrefix(expectedPrice.getPrefix().name()));

        WebTextAdGroup adGroupWithBanners = randomNameWebAdGroup(adGroupId, null)
                .withBanners(singletonList(addedBanner));

        updateAndCheckResult(singletonList(adGroupWithBanners));

        List<OldBanner> actualBanners = findBanners();
        assumeThat("должен быть один добавленный баннер", actualBanners, hasSize(1));

        OldBannerPrice actualPrice = steps.bannerPriceSteps().getBannerPrice(bannerInfo);
        assertThat("цена отличается от ожидаемой", actualPrice, beanDiffer(expectedPrice)
                .useCompareStrategy(PRICE_COMPARE_STRATEGY));
    }

    @Test
    public void update_AdGroupWithUpdatedBannerWithBannerPrice_BannerPriceIsDeleted() {
        OldBannerPrice bannerPrice = defaultBannerPrice();
        OldTextBanner banner = activeTextBanner(campaignInfo.getCampaignId(), null).withBannerPrice(bannerPrice);
        TextBannerInfo bannerInfo = steps.bannerSteps().createBanner(banner, campaignInfo);
        assumeThat(steps.bannerPriceSteps().getBannerPrice(bannerInfo), notNullValue());

        long adGroupId = bannerInfo.getAdGroupId();
        long bannerId = bannerInfo.getBannerId();
        WebBanner addedBanner = randomTitleWebTextBanner(bannerId).withBannerPrice(null);

        WebTextAdGroup adGroupWithBanners = randomNameWebAdGroup(adGroupId, null)
                .withBanners(singletonList(addedBanner));

        updateAndCheckResult(singletonList(adGroupWithBanners));

        List<OldBanner> actualBanners = findBanners();
        assumeThat("должен быть один добавленный баннер", actualBanners, hasSize(1));

        OldBannerPrice actualPrice = steps.bannerPriceSteps().getBannerPrice(bannerInfo);
        assertThat("цена должна быть удалена", actualPrice, nullValue());
    }
}
