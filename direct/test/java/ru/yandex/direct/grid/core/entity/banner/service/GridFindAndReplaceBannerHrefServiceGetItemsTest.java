package ru.yandex.direct.grid.core.entity.banner.service;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher;
import ru.yandex.direct.core.entity.banner.model.old.OldBanner;
import ru.yandex.direct.core.entity.sitelink.model.Sitelink;
import ru.yandex.direct.core.entity.sitelink.model.SitelinkSet;
import ru.yandex.direct.core.testing.data.TestSitelinkSets;
import ru.yandex.direct.grid.core.configuration.GridCoreTest;
import ru.yandex.direct.grid.core.entity.banner.model.GdiFindAndReplaceBannerHrefItem;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.hamcrest.Matchers.contains;
import static org.junit.Assert.assertThat;
import static org.thymeleaf.util.SetUtils.singletonSet;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.core.testing.data.TestSitelinks.defaultSitelink;
import static ru.yandex.direct.core.testing.data.TestSitelinks.defaultSitelink2;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;

@GridCoreTest
@RunWith(SpringRunner.class)
public class GridFindAndReplaceBannerHrefServiceGetItemsTest extends GridFindAndReplaceHrefTestBase {

    @Autowired
    private GridFindAndReplaceBannerHrefService serviceUnderTest;

    @Test
    public void secondBannerNeedUpdateHref() {
        OldBanner bannerWithoutReplace = createBannerWithoutReplace();
        OldBanner bannerWithReplace = createBannerWithReplace();
        List<GdiFindAndReplaceBannerHrefItem> bannersInfo =
                serviceUnderTest.getFindAndReplaceBannersHrefItems(clientId,
                        asList(bannerWithoutReplace.getId(), bannerWithReplace.getId()), emptySet(), emptyMap(),
                        true, true, true, replaceRule, replaceRule);

        List<GdiFindAndReplaceBannerHrefItem> expected =
                singletonList(getBannerPreviewItem(bannerWithReplace, null, emptyMap()));
        assertThat(bannersInfo, beanDiffer(expected));
    }

    @Test
    public void firstBannerWithoutSitelinksSecondNeedUpdateSitelinks() {
        OldBanner bannerWithoutReplace = createBannerWithoutReplace();
        OldBanner bannerWithReplaceSitelinks = createBannerWithoutReplace();
        SitelinkSet sitelinkSetWithReplaceSecondSitelink = createSitelinkSetWithReplaceSecondSitelink();
        linkSitelinkSetToBanner(bannerWithReplaceSitelinks, sitelinkSetWithReplaceSecondSitelink);

        List<GdiFindAndReplaceBannerHrefItem> bannersInfo =
                serviceUnderTest.getFindAndReplaceBannersHrefItems(clientId,
                        asList(bannerWithoutReplace.getId(), bannerWithReplaceSitelinks.getId()), emptySet(),
                        emptyMap(),
                        true, true, true, replaceRule, replaceRule);

        List<GdiFindAndReplaceBannerHrefItem> expected =
                singletonList(getBannerPreviewItem(bannerWithReplaceSitelinks, sitelinkSetWithReplaceSecondSitelink,
                        emptyMap()));
        assertThat(bannersInfo, beanDiffer(expected));
    }

    @Test
    public void firstBannerWithSitelinksSecondNeedUpdateSitelinks() {
        OldBanner bannerWithoutReplace = createBannerWithoutReplace();
        linkSitelinkSetToBanner(bannerWithoutReplace, createSitelinkSetWithoutReplace());

        OldBanner bannerWithReplaceSitelinks = createBannerWithoutReplace();
        SitelinkSet sitelinkSetWithReplaceSecondSitelink = createSitelinkSetWithReplaceSecondSitelink();
        linkSitelinkSetToBanner(bannerWithReplaceSitelinks, sitelinkSetWithReplaceSecondSitelink);

        List<GdiFindAndReplaceBannerHrefItem> bannersInfo =
                serviceUnderTest.getFindAndReplaceBannersHrefItems(clientId,
                        asList(bannerWithoutReplace.getId(), bannerWithReplaceSitelinks.getId()), emptySet(),
                        emptyMap(),
                        true, true, true, replaceRule, replaceRule);

        List<GdiFindAndReplaceBannerHrefItem> expected =
                singletonList(getBannerPreviewItem(bannerWithReplaceSitelinks, sitelinkSetWithReplaceSecondSitelink,
                        emptyMap()));
        assertThat(bannersInfo, beanDiffer(expected));
    }

    @Test
    public void fisrtBannerNeedReplaceAndSecondBannerNeedReplaceSitelink() {
        OldBanner bannerWithReplace = createBannerWithReplace();
        OldBanner bannerWithReplaceSitelinks = createBannerWithoutReplace();
        SitelinkSet sitelinkSetWithReplaceSecondSitelink = createSitelinkSetWithReplaceSecondSitelink();
        linkSitelinkSetToBanner(bannerWithReplaceSitelinks, sitelinkSetWithReplaceSecondSitelink);

        List<GdiFindAndReplaceBannerHrefItem> bannersInfo =
                serviceUnderTest.getFindAndReplaceBannersHrefItems(clientId,
                        asList(bannerWithReplace.getId(), bannerWithReplaceSitelinks.getId()), emptySet(),
                        emptyMap(),
                        true, true, true, replaceRule, replaceRule);

        List<GdiFindAndReplaceBannerHrefItem> expected =
                asList(getBannerPreviewItem(bannerWithReplace, null, emptyMap()),
                        getBannerPreviewItem(bannerWithReplaceSitelinks, sitelinkSetWithReplaceSecondSitelink,
                                emptyMap()));
        assertThat(bannersInfo, beanDiffer(expected));
    }

    @Test
    public void bothBannersDoNotNeedReplace() {
        OldBanner bannerWithoutReplace = createBannerWithoutReplace();
        OldBanner bannerWithoutReplace2 = createBannerWithoutReplace();

        List<GdiFindAndReplaceBannerHrefItem> bannersInfo =
                serviceUnderTest.getFindAndReplaceBannersHrefItems(clientId,
                        asList(bannerWithoutReplace.getId(), bannerWithoutReplace2.getId()), emptySet(),
                        emptyMap(),
                        true, true, true, replaceRule, replaceRule);

        assertThat(bannersInfo, beanDiffer(emptyList()));
    }

    @Test
    public void bannerNeedReplaceButItsInExceptions() {
        OldBanner bannerWithReplace1 = createBannerWithReplace();
        OldBanner bannerWithReplace2 = createBannerWithReplace();

        List<GdiFindAndReplaceBannerHrefItem> bannersInfo =
                serviceUnderTest.getFindAndReplaceBannersHrefItems(clientId,
                        asList(bannerWithReplace1.getId(), bannerWithReplace2.getId()),
                        singletonSet(bannerWithReplace2.getId()), emptyMap(),
                        true, true, true, replaceRule, replaceRule);

        List<GdiFindAndReplaceBannerHrefItem> expected =
                singletonList(getBannerPreviewItem(bannerWithReplace1, null, emptyMap()));
        assertThat(bannersInfo, beanDiffer(expected));
    }

    @Test
    public void oneSitelinkThatNeedReplaceIsInExceptions() {
        OldBanner bannerWithoutReplace = createBannerWithoutReplace();
        List<Sitelink> sitelinks = asList(defaultSitelink().withHref(HREF_NEED_REPLACE),
                defaultSitelink2().withHref(HREF_NEED_REPLACE));
        SitelinkSet sitelinkSet = steps.sitelinkSetSteps()
                .createSitelinkSet(TestSitelinkSets.sitelinkSet(clientInfo.getClientId(), sitelinks), clientInfo)
                .getSitelinkSet();
        linkSitelinkSetToBanner(bannerWithoutReplace, sitelinkSet);

        Map<Long, Set<Long>> sitelinksExceptions =
                singletonMap(bannerWithoutReplace.getId(), singletonSet(sitelinks.get(1).getId()));
        List<GdiFindAndReplaceBannerHrefItem> bannersInfo =
                serviceUnderTest.getFindAndReplaceBannersHrefItems(clientId,
                        singletonList(bannerWithoutReplace.getId()), emptySet(),
                        sitelinksExceptions,
                        true, true, true, replaceRule, replaceRule);

        List<GdiFindAndReplaceBannerHrefItem> expected =
                singletonList(getBannerPreviewItem(bannerWithoutReplace, sitelinkSet, sitelinksExceptions));
        assertThat(bannersInfo, beanDiffer(expected));
    }

    @Test
    public void bannerInExceptionsButItsSitelinksShouldBeUpdated() {
        OldBanner bannerWithReplace = createBannerWithReplace();
        SitelinkSet sitelinkSetWithReplaceSecondSitelink = createSitelinkSetWithReplaceSecondSitelink();
        linkSitelinkSetToBanner(bannerWithReplace, sitelinkSetWithReplaceSecondSitelink);

        List<GdiFindAndReplaceBannerHrefItem> bannersInfo =
                serviceUnderTest.getFindAndReplaceBannersHrefItems(clientId,
                        singletonList(bannerWithReplace.getId()), singletonSet(bannerWithReplace.getId()),
                        emptyMap(),
                        true, true, true, replaceRule, replaceRule);

        List<GdiFindAndReplaceBannerHrefItem> expected =
                singletonList(getBannerPreviewItem(bannerWithReplace, sitelinkSetWithReplaceSecondSitelink, emptyMap())
                        .withNewHref(null));
        assertThat(bannersInfo, beanDiffer(expected));
    }

    @Test
    public void getFindAndReplaceBannersHrefItems_TwoBannersWithSameSitelinkAndSitelinkException() {
        OldBanner banner1 = createBannerWithReplace();
        SitelinkSet sitelinkSetWithReplaceSecondSitelink = createSitelinkSetWithReplaceSecondSitelink();
        linkSitelinkSetToBanner(banner1, sitelinkSetWithReplaceSecondSitelink);

        OldBanner banner2 = createBannerWithReplace();
        linkSitelinkSetToBanner(banner2, sitelinkSetWithReplaceSecondSitelink);

        Sitelink sitelink2 = sitelinkSetWithReplaceSecondSitelink.getSitelinks().get(1);
        List<GdiFindAndReplaceBannerHrefItem> bannersInfo =
                serviceUnderTest.getFindAndReplaceBannersHrefItems(clientInfo.getClientId(),
                        asList(banner1.getId(), banner2.getId()),
                        emptySet(),
                        singletonMap(banner1.getId(), singletonSet(sitelink2.getId())),
                        true, true, true, replaceRule, replaceRule);

        List<GdiFindAndReplaceBannerHrefItem> expectedBannersForUpdate =
                asList(getBannerPreviewItem(banner1, sitelinkSetWithReplaceSecondSitelink,
                        singletonMap(banner1.getId(), singletonSet(sitelink2.getId()))),
                        getBannerPreviewItem(banner2, sitelinkSetWithReplaceSecondSitelink,
                                emptyMap()));

        assertThat(bannersInfo, contains(mapList(expectedBannersForUpdate, BeanDifferMatcher::beanDiffer)));
    }
}
