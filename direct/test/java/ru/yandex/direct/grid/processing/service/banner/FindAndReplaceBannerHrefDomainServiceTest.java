package ru.yandex.direct.grid.processing.service.banner;

import java.util.Set;

import com.google.common.collect.ImmutableSet;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.banner.model.old.OldBanner;
import ru.yandex.direct.core.entity.banner.model.old.OldTextBanner;
import ru.yandex.direct.core.entity.banner.repository.old.OldBannerRepository;
import ru.yandex.direct.core.entity.sitelink.model.Sitelink;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.SitelinkSetInfo;
import ru.yandex.direct.core.testing.info.TextBannerInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.model.banner.mutation.GdFindAndReplaceAdsHrefDomain;
import ru.yandex.direct.grid.processing.model.banner.mutation.GdFindAndReplaceAdsHrefDomainInstruction;
import ru.yandex.direct.grid.processing.model.banner.mutation.GdFindAndReplaceAdsHrefPayloadItem;
import ru.yandex.direct.grid.processing.model.banner.mutation.GdFindAndReplaceAdsHrefPreviewPayloadItemSitelink;
import ru.yandex.direct.grid.processing.model.banner.mutation.GdFindAndReplaceAdsHrefTargetType;
import ru.yandex.direct.grid.processing.model.common.GdCachedResult;
import ru.yandex.direct.grid.processing.model.common.GdResult;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath.newPath;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.allFieldsExcept;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;
import static ru.yandex.direct.core.testing.data.TestBanners.activeTextBanner;
import static ru.yandex.direct.core.testing.data.TestSitelinkSets.sitelinkSet;
import static ru.yandex.direct.core.testing.data.TestSitelinks.defaultSitelink;
import static ru.yandex.direct.core.testing.data.TestSitelinks.defaultSitelink2;

@GridProcessingTest
@RunWith(SpringRunner.class)
public class FindAndReplaceBannerHrefDomainServiceTest {
    private static final String REPLACE_HREF_DOMAIN_ARG_NAME = "input";
    private static final Set<GdFindAndReplaceAdsHrefTargetType> ALL_TARGETS =
            ImmutableSet.of(GdFindAndReplaceAdsHrefTargetType.AD_HREF, GdFindAndReplaceAdsHrefTargetType.SITELINK_HREF);

    @Autowired
    private FindAndReplaceBannerHrefDomainService service;
    @Autowired
    private Steps steps;
    @Autowired
    private OldBannerRepository bannerRepository;

    private ClientInfo clientInfo;
    private SitelinkSetInfo sitelinkSet;
    private TextBannerInfo banner;

    private GdFindAndReplaceAdsHrefDomain input;

    @Before
    public void before() {
        clientInfo = steps.clientSteps().createDefaultClient();

        sitelinkSet = steps.sitelinkSetSteps().createSitelinkSet(sitelinkSet(clientInfo.getClientId(), asList(
                defaultSitelink().withHref("http://replace.net/1"),
                defaultSitelink2().withHref("http://yandex.ru/2"))), clientInfo);

        banner = steps.bannerSteps().createBanner(activeTextBanner()
                .withHref("http://yandex.ru")
                .withSitelinksSetId(sitelinkSet.getSitelinkSetId()), clientInfo);

        input = new GdFindAndReplaceAdsHrefDomain()
                .withAdIds(singletonList(banner.getBannerId()))
                .withTargetTypes(ALL_TARGETS)
                .withSitelinkIdsHrefExceptions(emptyMap())
                .withAdIdsHrefExceptions(emptySet())
                .withReplaceInstruction(new GdFindAndReplaceAdsHrefDomainInstruction()
                        .withSearch(singletonList("yandex.ru"))
                        .withReplace("replace.da"));
    }

    @Test
    public void preview_OneBanner_Successful() {
        GdCachedResult<GdFindAndReplaceAdsHrefPayloadItem> result =
                service.preview(input, clientInfo.getUid(), clientInfo.getClientId(), REPLACE_HREF_DOMAIN_ARG_NAME);

        Sitelink sitelink1 = sitelinkSet.getSitelinkSet().getSitelinks().get(0);
        Sitelink sitelink2 = sitelinkSet.getSitelinkSet().getSitelinks().get(1);
        GdCachedResult<GdFindAndReplaceAdsHrefPayloadItem> expected =
                new GdCachedResult<GdFindAndReplaceAdsHrefPayloadItem>()
                        .withRowset(singletonList(new GdFindAndReplaceAdsHrefPayloadItem()
                                .withAdId(banner.getBannerId())
                                .withOldHref(banner.getBanner().getHref())
                                .withNewHref(banner.getBanner().getHref().replace("yandex.ru", "replace.da"))
                                .withSitelinks(asList(
                                        new GdFindAndReplaceAdsHrefPreviewPayloadItemSitelink()
                                                .withSitelinkId(sitelink1.getId())
                                                .withTitle(sitelink1.getTitle())
                                                .withOldHref(sitelink1.getHref())
                                                .withNewHref(null),
                                        new GdFindAndReplaceAdsHrefPreviewPayloadItemSitelink()
                                                .withSitelinkId(sitelink2.getId())
                                                .withTitle(sitelink2.getTitle())
                                                .withOldHref(sitelink2.getHref())
                                                .withNewHref(sitelink2.getHref().replace("yandex.ru", "replace.da"))))))
                        .withTotalCount(1)
                        .withSuccessCount(1);

        assertThat(expected, beanDiffer(result).useCompareStrategy(allFieldsExcept(newPath("cacheKey"))));
    }

    @Test
    public void replace_OneBanner_Successful() {
        GdResult<GdFindAndReplaceAdsHrefPayloadItem> result =
                service.replace(input, clientInfo.getUid(), clientInfo.getClientId(), REPLACE_HREF_DOMAIN_ARG_NAME);

        Sitelink sitelink1 = sitelinkSet.getSitelinkSet().getSitelinks().get(0);
        Sitelink sitelink2 = sitelinkSet.getSitelinkSet().getSitelinks().get(1);
        GdResult<GdFindAndReplaceAdsHrefPayloadItem> expected =
                new GdCachedResult<GdFindAndReplaceAdsHrefPayloadItem>()
                        .withRowset(singletonList(new GdFindAndReplaceAdsHrefPayloadItem()
                                .withAdId(banner.getBannerId())
                                .withOldHref(banner.getBanner().getHref())
                                .withNewHref(banner.getBanner().getHref().replace("yandex.ru", "replace.da"))
                                .withSitelinks(asList(
                                        new GdFindAndReplaceAdsHrefPreviewPayloadItemSitelink()
                                                .withSitelinkId(sitelink1.getId())
                                                .withTitle(sitelink1.getTitle())
                                                .withOldHref(sitelink1.getHref())
                                                .withNewHref(null),
                                        new GdFindAndReplaceAdsHrefPreviewPayloadItemSitelink()
                                                .withSitelinkId(sitelink2.getId())
                                                .withTitle(sitelink2.getTitle())
                                                .withOldHref(sitelink2.getHref())
                                                .withNewHref(sitelink2.getHref().replace("yandex.ru", "replace.da"))))))
                        .withTotalCount(1)
                        .withSuccessCount(1);

        assertThat(expected, beanDiffer(result));

        OldBanner actualBanner =
                bannerRepository.getBanners(clientInfo.getShard(), singletonList(this.banner.getBannerId())).get(0);
        assertThat((OldTextBanner) actualBanner, beanDiffer(new OldTextBanner()
                .withId(banner.getBannerId())
                .withHref(banner.getBanner().getHref().replace("yandex.ru", "replace.da")))
                .useCompareStrategy(onlyExpectedFields()));
    }

    @Test
    public void replace_OnlySitelinkUpdated_BannerNewHrefIsNull() {
        TextBannerInfo banner = steps.bannerSteps().createBanner(activeTextBanner().withHref("http://norepalce.com")
                .withSitelinksSetId(sitelinkSet.getSitelinkSetId()), clientInfo);

        input.withAdIds(singletonList(banner.getBannerId()));

        GdResult<GdFindAndReplaceAdsHrefPayloadItem> result =
                service.replace(input, clientInfo.getUid(), clientInfo.getClientId(), REPLACE_HREF_DOMAIN_ARG_NAME);

        assertThat(result.getRowset().get(0).getNewHref(), nullValue());
    }
}
