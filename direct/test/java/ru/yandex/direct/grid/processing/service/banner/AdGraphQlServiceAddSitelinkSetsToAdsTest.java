package ru.yandex.direct.grid.processing.service.banner;

import java.util.List;

import com.google.common.primitives.Longs;
import one.util.streamex.StreamEx;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.banner.model.BannerWithSitelinks;
import ru.yandex.direct.core.entity.banner.repository.BannerTypedRepository;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.CpmBannerInfo;
import ru.yandex.direct.core.testing.info.NewPerformanceBannerInfo;
import ru.yandex.direct.core.testing.info.SitelinkSetInfo;
import ru.yandex.direct.core.testing.info.TextBannerInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.model.api.GdDefect;
import ru.yandex.direct.grid.processing.model.api.GdValidationResult;
import ru.yandex.direct.grid.processing.model.banner.mutation.GdAddSitelinkSetToAds;
import ru.yandex.direct.grid.processing.model.banner.mutation.GdUpdateAdPayloadItem;
import ru.yandex.direct.grid.processing.model.banner.mutation.GdUpdateAdsPayload;
import ru.yandex.direct.grid.processing.util.GraphQlTestExecutor;
import ru.yandex.direct.grid.processing.util.TestAuthHelper;
import ru.yandex.direct.grid.processing.util.UserHelper;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.core.testing.data.TestBanners.activeTextBanner;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
public class AdGraphQlServiceAddSitelinkSetsToAdsTest {
    private static final String MUTATION_TEMPLATE = ""
            + "mutation {\n"
            + "  %s (input: %s) {\n"
            + "  \tvalidationResult {\n"
            + "      errors {\n"
            + "        code\n"
            + "        path\n"
            + "        params\n"
            + "      }\n"
            + "    }\n"
            + "    updatedAds {"
            + "      id"
            + "    }\n"
            + "  }\n"
            + "}";

    private static final String UPDATE_ADS_MUTATION_NAME = "addSitelinkSetToAds";
    private static final GraphQlTestExecutor.TemplateMutation<GdAddSitelinkSetToAds, GdUpdateAdsPayload> UPDATE_ADS_MUTATION =
            new GraphQlTestExecutor.TemplateMutation<>(UPDATE_ADS_MUTATION_NAME, MUTATION_TEMPLATE,
                    GdAddSitelinkSetToAds.class, GdUpdateAdsPayload.class);

    @Autowired
    private GraphQlTestExecutor processor;

    @Autowired
    private Steps steps;

    @Autowired
    public BannerTypedRepository bannerRepository;

    private TextBannerInfo textAd;
    private Long textAdId;
    private User operator;
    private SitelinkSetInfo defaultSitelinkSet;
    private ClientInfo clientInfo;

    @Before
    public void before() {
        clientInfo = steps.clientSteps().createDefaultClient();
        AdGroupInfo defaultAdGroup = steps.adGroupSteps().createDefaultAdGroup(clientInfo);
        textAd = steps.bannerSteps().createDefaultBanner(defaultAdGroup);
        defaultSitelinkSet = steps.sitelinkSetSteps().createDefaultSitelinkSet(clientInfo);
        textAdId = textAd.getBannerId();
        operator = UserHelper.getUser(clientInfo.getClient());
        TestAuthHelper.setDirectAuthentication(operator);
    }

    @Test
    public void addSitelinkSetToOneTextAd_Success() {
        GdAddSitelinkSetToAds input = new GdAddSitelinkSetToAds()
                .withSitelinkSetId(defaultSitelinkSet.getSitelinkSetId())
                .withAdIds(singletonList(textAdId));
        GdUpdateAdsPayload gdUpdateAdsPayload =
                processor.doMutationAndGetPayload(UPDATE_ADS_MUTATION, input, operator);

        validateUpdateSuccessful(gdUpdateAdsPayload, textAdId);

        BannerWithSitelinks actualBanner = bannerRepository.getStrictly(textAd.getShard(),
                singletonList(textAdId), BannerWithSitelinks.class).get(0);

        assertThat(actualBanner.getSitelinksSetId()).isEqualTo(defaultSitelinkSet.getSitelinkSetId());
    }

    @Test
    public void addSitelinkSetToTextAdAndCpmAd_TextAdSaved() {
        CpmBannerInfo cpmAd =
                steps.bannerSteps().createActiveCpmVideoBanner(clientInfo);

        GdAddSitelinkSetToAds input = new GdAddSitelinkSetToAds()
                .withSitelinkSetId(defaultSitelinkSet.getSitelinkSetId())
                .withAdIds(asList(textAdId, cpmAd.getBannerId()));
        GdUpdateAdsPayload gdUpdateAdsPayload =
                processor.doMutationAndGetPayload(UPDATE_ADS_MUTATION, input, operator);

        GdValidationResult expectedValidationResult = new GdValidationResult().withErrors(
                singletonList(new GdDefect().withCode("BannerDefectIds.Gen.INCONSISTENT_BANNER_TYPE")
                        .withPath("adUpdateItems[1]")));
        GdUpdateAdsPayload expectedPayload = new GdUpdateAdsPayload()
                .withUpdatedAds(singletonList(new GdUpdateAdPayloadItem().withId(textAdId)))
                .withValidationResult(expectedValidationResult);
        assertThat(gdUpdateAdsPayload).is(matchedBy(beanDiffer(expectedPayload)));

        BannerWithSitelinks actualBanner = bannerRepository.getStrictly(textAd.getShard(),
                singletonList(textAdId), BannerWithSitelinks.class).get(0);

        assertThat(actualBanner.getSitelinksSetId()).isEqualTo(defaultSitelinkSet.getSitelinkSetId());
    }

    @Test
    public void addSitelinkSetToTextAdAndPerformanceAd_TextAdSaved() {
        Long feedId = steps.feedSteps().createDefaultFeed(clientInfo).getFeedId();
        AdGroupInfo performanceAdGroupInfo = steps.adGroupSteps().createActivePerformanceAdGroup(clientInfo, feedId);
        NewPerformanceBannerInfo performanceBannerInfo = steps.performanceBannerSteps()
                .createPerformanceBanner(performanceAdGroupInfo);

        GdAddSitelinkSetToAds input = new GdAddSitelinkSetToAds()
                .withSitelinkSetId(defaultSitelinkSet.getSitelinkSetId())
                .withAdIds(asList(textAdId, performanceBannerInfo.getBannerId()));
        GdUpdateAdsPayload gdUpdateAdsPayload =
                processor.doMutationAndGetPayload(UPDATE_ADS_MUTATION, input, operator);

        GdValidationResult expectedValidationResult = new GdValidationResult().withErrors(
                singletonList(new GdDefect().withCode("BannerDefectIds.Gen.INCONSISTENT_BANNER_TYPE")
                        .withPath("adUpdateItems[1]")));
        GdUpdateAdsPayload expectedPayload = new GdUpdateAdsPayload()
                .withUpdatedAds(singletonList(new GdUpdateAdPayloadItem().withId(textAdId)))
                .withValidationResult(expectedValidationResult);
        assertThat(gdUpdateAdsPayload).is(matchedBy(beanDiffer(expectedPayload)));

        BannerWithSitelinks actualBanner = bannerRepository.getStrictly(textAd.getShard(),
                singletonList(textAdId), BannerWithSitelinks.class).get(0);

        assertThat(actualBanner.getSitelinksSetId()).isEqualTo(defaultSitelinkSet.getSitelinkSetId());
    }

    @Test
    public void addSitelinkSetIdToOneInvalidTextAdAndOneValidTextAd_Success() {
        GdAddSitelinkSetToAds input = new GdAddSitelinkSetToAds()
                .withAdIds(Longs.asList(textAdId, Long.MAX_VALUE))
                .withSitelinkSetId(defaultSitelinkSet.getSitelinkSetId());

        GdUpdateAdsPayload gdUpdateAdsPayload =
                processor.doMutationAndGetPayload(UPDATE_ADS_MUTATION, input, operator);

        GdValidationResult expectedValidationResult = new GdValidationResult().withErrors(
                singletonList(new GdDefect().withCode("BannerDefectIds.Gen.AD_NOT_FOUND")
                        .withPath("adUpdateItems[1].id")));

        GdUpdateAdsPayload expectedPayload = new GdUpdateAdsPayload()
                .withUpdatedAds(singletonList(new GdUpdateAdPayloadItem().withId(textAdId)))
                .withValidationResult(expectedValidationResult);

        assertThat(gdUpdateAdsPayload).is(matchedBy(beanDiffer(expectedPayload)));
    }

    @Test
    public void removeSitelinksSet_success() {
        TextBannerInfo textBanner = steps.bannerSteps().createBanner(activeTextBanner()
                .withSitelinksSetId(defaultSitelinkSet.getSitelinkSetId()), clientInfo);

        GdAddSitelinkSetToAds input = new GdAddSitelinkSetToAds()
                .withAdIds(List.of(textBanner.getBannerId()))
                .withSitelinkSetId(null);

        GdUpdateAdsPayload gdUpdateAdsPayload =
                processor.doMutationAndGetPayload(UPDATE_ADS_MUTATION, input, operator);

        validateUpdateSuccessful(gdUpdateAdsPayload, textBanner.getBannerId());

        BannerWithSitelinks actualBanner = bannerRepository.getStrictly(textAd.getShard(),
                singletonList(textAdId), BannerWithSitelinks.class).get(0);

        assertThat(actualBanner.getSitelinksSetId()).isNull();
    }

    private void validateUpdateSuccessful(GdUpdateAdsPayload gdUpdateAdsPayload, Long... bannerId) {
        List<GdUpdateAdPayloadItem> updatedAds =
                StreamEx.of(bannerId).map(id -> new GdUpdateAdPayloadItem().withId(id)).toList();
        GdUpdateAdsPayload expectedGdUpdateAdsPayload = new GdUpdateAdsPayload()
                .withUpdatedAds(updatedAds);

        assertThat(gdUpdateAdsPayload)
                .is(matchedBy(beanDiffer(expectedGdUpdateAdsPayload)));
    }

}
