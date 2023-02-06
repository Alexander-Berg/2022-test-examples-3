package ru.yandex.direct.grid.core.entity.banner.repository;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.adgroup.model.AdGroupType;
import ru.yandex.direct.core.entity.banner.model.old.OldBannerType;
import ru.yandex.direct.core.entity.banner.model.old.OldCpmBanner;
import ru.yandex.direct.core.entity.banner.model.old.OldTextBanner;
import ru.yandex.direct.core.entity.banner.type.href.BannersUrlHelper;
import ru.yandex.direct.core.entity.sitelink.model.SitelinkSet;
import ru.yandex.direct.core.testing.data.TestSitelinks;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.grid.core.configuration.GridCoreTest;
import ru.yandex.direct.grid.core.entity.banner.model.GdiBannerWithSitelinksForReplace;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.core.testing.data.TestBanners.activeCpmBanner;
import static ru.yandex.direct.core.testing.data.TestBanners.activeTextBanner;

@GridCoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class GridFindAndReplaceBannerRepositoryTest {

    @Autowired
    private Steps steps;

    @Autowired
    private GridFindAndReplaceBannerRepository repoUnderTest;

    @Autowired
    private BannersUrlHelper bannersUrlHelper;

    private ClientInfo clientInfo;
    private CampaignInfo campaignInfo;

    private int shard;

    private OldTextBanner banner;
    private SitelinkSet sitelinkSet;
    private List<String> defaultSlDomains;
    private static final String DEFAULT_BANNER_DOMAIN = "ya.ru";

    @Before
    public void before() {
        defaultSlDomains = Arrays.asList(
                bannersUrlHelper.extractHostFromHrefWithWwwOrNull(TestSitelinks.defaultSitelink().getHref()),
                bannersUrlHelper.extractHostFromHrefWithWwwOrNull(TestSitelinks.defaultSitelink2().getHref()));
        campaignInfo = steps.campaignSteps().createDefaultCampaign();
        clientInfo = campaignInfo.getClientInfo();
        shard = clientInfo.getShard();
        sitelinkSet = steps.sitelinkSetSteps().createDefaultSitelinkSet(clientInfo).getSitelinkSet();
        banner = steps.bannerSteps()
                .createBanner(activeTextBanner(null, null)
                                .withSitelinksSetId(sitelinkSet.getId())
                                .withHref("http://ya.ru/?serch=123")
                                .withDomain(DEFAULT_BANNER_DOMAIN),
                        campaignInfo).getBanner();
    }


    @Test
    public void getBannerAndSitelinksDomains_complexTest() {
        String bannerDomain = "m.ya.ru";
        String bannerDomain2 = "m.test.test.ru";
        OldTextBanner banner2 = createBanner(bannerDomain, true);
        OldTextBanner banner3 = createBanner(bannerDomain, true);
        OldTextBanner banner4 = createBanner(bannerDomain2, false);

        Set<String> actualDomains = repoUnderTest
                .getBannerAndSitelinksDomains(shard, clientInfo.getClientId(),
                        Arrays.asList(banner.getId(), banner2.getId(), banner3.getId(), banner4.getId()));

        Set<String> expectedDomains = new HashSet<>();
        expectedDomains.add(DEFAULT_BANNER_DOMAIN);
        expectedDomains.add(bannerDomain);
        expectedDomains.add(bannerDomain2);
        expectedDomains.addAll(defaultSlDomains);

        assertEquals("Sets should be equals", actualDomains, expectedDomains);
    }

    @Test
    public void getBannerAndSitelinksDomains_existOnlySitelinkHrefTest() {
        OldTextBanner secondBanner = steps.bannerSteps()
                .createBanner(
                        activeTextBanner(null, null)
                                .withSitelinksSetId(sitelinkSet.getId()),
                        campaignInfo)
                .getBanner();

        Set<String> actualDomains = repoUnderTest
                .getBannerAndSitelinksDomains(shard, clientInfo.getClientId(), singletonList(secondBanner.getId()));

        Set<String> expectedDomains = new HashSet<>(defaultSlDomains);
        assertEquals("Sets should be equals", actualDomains, expectedDomains);
    }

    @Test
    public void getBannerAndSitelinksDomains_checkDistinctSelectTest() {
        String bannerDomain = "m.ya.ru";
        OldTextBanner banner2 = createBanner(bannerDomain, true);
        OldTextBanner banner3 = createBanner(bannerDomain, true);


        Set<String> actualDomains = repoUnderTest
                .getBannerAndSitelinksDomains(shard, clientInfo.getClientId(),
                        Arrays.asList(banner.getId(), banner2.getId(), banner3.getId()));

        Set<String> expectedDomains = new HashSet<>();
        expectedDomains.add(DEFAULT_BANNER_DOMAIN);
        expectedDomains.add(bannerDomain);
        expectedDomains.addAll(defaultSlDomains);

        assertEquals("Sets should be equals", actualDomains, expectedDomains);
    }

    @Test
    public void getBannerAndSitelinksDomains_sitelinkNotExistTest() {
        String bannerDomain = "m.ya.ru";
        OldTextBanner secondBanner = createBanner(bannerDomain, false);

        Set<String> actualDomains = repoUnderTest
                .getBannerAndSitelinksDomains(shard, clientInfo.getClientId(),
                        singletonList(secondBanner.getId()));

        Set<String> expectedDomains = new HashSet<>();
        expectedDomains.add(bannerDomain);

        assertEquals("Sets should be equals", actualDomains, expectedDomains);
    }

    private OldTextBanner createBanner(String bannerDomain, boolean withSitelink) {
        Long sitelinkSetId = withSitelink ? sitelinkSet.getId() : null;

        return steps.bannerSteps()
                .createBanner(
                        activeTextBanner(null, null)
                                .withDomain(bannerDomain)
                                .withSitelinksSetId(sitelinkSetId),
                        campaignInfo)
                .getBanner();
    }

    @Test
    public void getBannerWithSitelinks_OneBanner_OneSitelinkSet() {
        List<GdiBannerWithSitelinksForReplace> result = repoUnderTest
                .getBannerWithSitelinks(shard, clientInfo.getClientId(),
                        singletonList(banner.getId()), true, false);

        assertThat("полученные баннеры с сайтлинками не соответствуют ожиданию", result,
                beanDiffer(singletonList(getBannerWithSitelinks(banner, sitelinkSet))));
    }

    @Test
    public void getBannerWithSitelinks_OneBanner_NoNeedSitelinks() {
        List<GdiBannerWithSitelinksForReplace> result = repoUnderTest
                .getBannerWithSitelinks(shard, clientInfo.getClientId(),
                        singletonList(banner.getId()), false, false);
        assertThat("полученные баннеры с сайтлинками не соответствуют ожиданию", result,
                beanDiffer(singletonList(
                        getBannerWithSitelinks(banner,
                                new SitelinkSet().withId(banner.getSitelinksSetId()).withSitelinks(emptyList())))));
    }

    @Test
    public void getBannerWithSitelinks_OneBannerWithoutSitelinkSetNoNeedSitelinks() {
        OldTextBanner banner = steps.bannerSteps().createBanner(activeTextBanner(null, null), campaignInfo).getBanner();
        List<GdiBannerWithSitelinksForReplace> result = repoUnderTest
                .getBannerWithSitelinks(shard, clientInfo.getClientId(),
                        singletonList(banner.getId()), false, false);
        assertThat("полученные баннеры с сайтлинками не соответствуют ожиданию", result,
                beanDiffer(
                        singletonList(getBannerWithSitelinks(banner, new SitelinkSet().withSitelinks(emptyList())))));
    }

    @Test
    public void getBannerWithSitelinks_OneBannerWithoutSitelinkSetNeedSitelinks() {
        OldTextBanner banner = steps.bannerSteps().createBanner(activeTextBanner(null, null), campaignInfo).getBanner();
        List<GdiBannerWithSitelinksForReplace> result = repoUnderTest
                .getBannerWithSitelinks(shard, clientInfo.getClientId(),
                        singletonList(banner.getId()), true, false);
        assertThat("полученные баннеры с сайтлинками не соответствуют ожиданию", result,
                beanDiffer(
                        singletonList(getBannerWithSitelinks(banner, new SitelinkSet().withSitelinks(emptyList())))));
    }

    @Test
    public void getBannerWithSitelinks_TwoBanner_OneSitelinkSet() {
        OldTextBanner secondBanner = steps.bannerSteps()
                .createBanner(activeTextBanner(null, null).withSitelinksSetId(sitelinkSet.getId()),
                        campaignInfo).getBanner();
        List<GdiBannerWithSitelinksForReplace> result = repoUnderTest
                .getBannerWithSitelinks(shard, clientInfo.getClientId(),
                        asList(banner.getId(), secondBanner.getId()),
                        true, false);
        assertThat("полученные баннеры с сайтлинками не соответствуют ожиданию", result,
                beanDiffer(asList(getBannerWithSitelinks(banner, sitelinkSet),
                        getBannerWithSitelinks(secondBanner, sitelinkSet))));
    }

    @Test
    public void getBannerWithSitelinks_TwoBanner_TwoSitelinkSet() {
        SitelinkSet secondSitelinkSet = steps.sitelinkSetSteps().createDefaultSitelinkSet(clientInfo).getSitelinkSet();
        OldTextBanner secondBanner = steps.bannerSteps()
                .createBanner(activeTextBanner(null, null).withSitelinksSetId(secondSitelinkSet.getId()),
                        campaignInfo).getBanner();
        List<GdiBannerWithSitelinksForReplace> result = repoUnderTest
                .getBannerWithSitelinks(shard, clientInfo.getClientId(),
                        asList(banner.getId(), secondBanner.getId()),
                        true, false);
        assertThat("полученные баннеры с сайтлинками не соответствуют ожиданию", result,
                beanDiffer(asList(getBannerWithSitelinks(banner, sitelinkSet),
                        getBannerWithSitelinks(secondBanner, secondSitelinkSet))));
    }

    @Test
    public void getBannerWithSitelinks_TwoBanner_OneWithSitelinkSet_OneWithout() {
        OldTextBanner secondBanner =
                steps.bannerSteps().createBanner(activeTextBanner(null, null), campaignInfo).getBanner();
        List<GdiBannerWithSitelinksForReplace> result = repoUnderTest
                .getBannerWithSitelinks(shard, clientInfo.getClientId(),
                        asList(banner.getId(), secondBanner.getId()),
                        true, false);
        assertThat("полученные баннеры с сайтлинками не соответствуют ожиданию", result,
                beanDiffer(asList(getBannerWithSitelinks(banner, sitelinkSet),
                        getBannerWithSitelinks(secondBanner, new SitelinkSet().withSitelinks(emptyList())))));
    }

    @Test
    public void getBannerWithDisplayHref_wrongBannerType() {
        // CpmBanner doesn't support display href
        var wrongTypeBanner = (OldCpmBanner) steps.bannerSteps()
                .createBanner(activeCpmBanner(null, null, null), campaignInfo)
                .getBanner();
        var expected = new GdiBannerWithSitelinksForReplace()
                .withId(wrongTypeBanner.getId())
                .withTitle(wrongTypeBanner.getTitle())
                .withTitleExtension(wrongTypeBanner.getTitleExtension())
                .withBody(wrongTypeBanner.getBody())
                .withBannerType(OldBannerType.toSource(wrongTypeBanner.getBannerType()))
                .withAdGroupType(AdGroupType.BASE)
                .withHref(wrongTypeBanner.getHref())
                .withSitelinks(List.of());
        var result = repoUnderTest.getBannerWithSitelinks(shard, clientInfo.getClientId(),
                List.of(wrongTypeBanner.getId()), true, true);
        assertThat("полученные баннеры не соответствуют ожиданию", result,
                beanDiffer(List.of(expected)));
    }

    private GdiBannerWithSitelinksForReplace getBannerWithSitelinks(OldTextBanner banner, SitelinkSet sitelinkSet) {
        return new GdiBannerWithSitelinksForReplace()
                .withId(banner.getId())
                .withTitle(banner.getTitle())
                .withTitleExtension(banner.getTitleExtension())
                .withBody(banner.getBody())
                .withBannerType(OldBannerType.toSource(banner.getBannerType()))
                .withAdGroupType(AdGroupType.BASE)
                .withHref(banner.getHref())
                .withSitelinkSetId(sitelinkSet.getId())
                .withSitelinks(sitelinkSet.getSitelinks());
    }
}
