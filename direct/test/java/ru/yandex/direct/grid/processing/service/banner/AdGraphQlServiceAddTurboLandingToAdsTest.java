package ru.yandex.direct.grid.processing.service.banner;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.banner.model.BannerWithTurboLanding;
import ru.yandex.direct.core.entity.banner.model.old.OldCpmBanner;
import ru.yandex.direct.core.entity.banner.repository.BannerTypedRepository;
import ru.yandex.direct.core.entity.turbolanding.model.TurboLanding;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.testing.data.TestBanners;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.BannerInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.CreativeInfo;
import ru.yandex.direct.core.testing.info.ImageHashBannerInfo;
import ru.yandex.direct.core.testing.info.TextBannerInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.model.api.GdDefect;
import ru.yandex.direct.grid.processing.model.api.GdValidationResult;
import ru.yandex.direct.grid.processing.model.banner.mutation.GdAddTurboLandingToAds;
import ru.yandex.direct.grid.processing.model.banner.mutation.GdUpdateAdPayloadItem;
import ru.yandex.direct.grid.processing.model.banner.mutation.GdUpdateAdsPayload;
import ru.yandex.direct.grid.processing.util.GraphQlTestExecutor;
import ru.yandex.direct.grid.processing.util.TestAuthHelper;
import ru.yandex.direct.grid.processing.util.UserHelper;

import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
public class AdGraphQlServiceAddTurboLandingToAdsTest {
    private static final String MUTATION_TEMPLATE = ""
            + "mutation {\n"
            + "  %s (input: %s) {\n"
            + "   validationResult {\n"
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

    private static final String UPDATE_ADS_MUTATION_NAME = "addTurboLandingToAds";

    static final GraphQlTestExecutor.TemplateMutation<GdAddTurboLandingToAds, GdUpdateAdsPayload> UPDATE_ADS_MUTATION =
            new GraphQlTestExecutor.TemplateMutation<>(UPDATE_ADS_MUTATION_NAME, MUTATION_TEMPLATE,
                    GdAddTurboLandingToAds.class, GdUpdateAdsPayload.class);

    @Autowired
    private GraphQlTestExecutor processor;

    @Autowired
    private Steps steps;

    @Autowired
    public BannerTypedRepository bannerRepository;

    private ClientInfo clientInfo;
    private int shard;
    private AdGroupInfo defaultAdGroup;
    private TextBannerInfo firstAd;
    private TextBannerInfo secondAd;
    private Long firstAdId;
    private Long secondAdId;
    private User operator;
    private TurboLanding defaultTurboLanding;

    @Before
    public void before() {
        clientInfo = steps.clientSteps().createDefaultClient();
        shard = clientInfo.getShard();
        ClientId clientId = clientInfo.getClientId();
        defaultAdGroup = steps.adGroupSteps().createDefaultAdGroup(clientInfo);
        firstAd = steps.bannerSteps().createDefaultBanner(defaultAdGroup);
        secondAd = steps.bannerSteps().createDefaultBanner(defaultAdGroup);
        firstAdId = firstAd.getBannerId();
        secondAdId = secondAd.getBannerId();
        operator = UserHelper.getUser(clientInfo.getClient());
        defaultTurboLanding = steps.turboLandingSteps().createDefaultTurboLanding(clientId);
        TestAuthHelper.setDirectAuthentication(operator);
    }

    @Test
    public void addTurboLandingToOneAd_success() {
        GdAddTurboLandingToAds input = new GdAddTurboLandingToAds()
                .withAdIds(singletonList(firstAdId))
                .withTurboLandingId(defaultTurboLanding.getId());
        GdUpdateAdsPayload gdUpdateAdsPayload = processor.doMutationAndGetPayload(UPDATE_ADS_MUTATION, input, operator);

        validateUpdateSuccessful(gdUpdateAdsPayload, firstAdId);

        BannerWithTurboLanding actualBanner = bannerRepository
                .getStrictly(firstAd.getShard(), singletonList(firstAdId), BannerWithTurboLanding.class)
                .get(0);

        assertThat(actualBanner.getTurboLandingId()).isEqualTo(defaultTurboLanding.getId());
    }

    @Test
    public void addTurboLandingToTwoAds_success() {
        GdAddTurboLandingToAds input = new GdAddTurboLandingToAds()
                .withAdIds(Arrays.asList(firstAdId, secondAdId))
                .withTurboLandingId(defaultTurboLanding.getId());
        GdUpdateAdsPayload gdUpdateAdsPayload = processor.doMutationAndGetPayload(UPDATE_ADS_MUTATION, input, operator);

        validateUpdateSuccessful(gdUpdateAdsPayload, firstAdId, secondAdId);

        BannerWithTurboLanding firstActualBanner = bannerRepository
                .getStrictly(firstAd.getShard(), singletonList(firstAdId), BannerWithTurboLanding.class)
                .get(0);

        BannerWithTurboLanding secondActualBanner = bannerRepository
                .getStrictly(secondAd.getShard(), singletonList(secondAdId), BannerWithTurboLanding.class)
                .get(0);

        assertThat(firstActualBanner.getTurboLandingId()).isEqualTo(defaultTurboLanding.getId());
        assertThat(secondActualBanner.getTurboLandingId()).isEqualTo(defaultTurboLanding.getId());
    }

    @Test
    public void addTurboLandingToImageBanner_success() {
        ImageHashBannerInfo imageAd = steps.bannerSteps().createActiveImageHashBanner(defaultAdGroup);
        steps.bannerSteps().createImage(imageAd); // без картинки imageBanner не валиден
        Long imageAdId = imageAd.getBannerId();
        GdAddTurboLandingToAds input = new GdAddTurboLandingToAds()
                .withAdIds(singletonList(imageAdId))
                .withTurboLandingId(defaultTurboLanding.getId());
        GdUpdateAdsPayload gdUpdateAdsPayload = processor.doMutationAndGetPayload(UPDATE_ADS_MUTATION, input, operator);

        validateUpdateSuccessful(gdUpdateAdsPayload, imageAdId);

        BannerWithTurboLanding actualBanner =
                bannerRepository.getStrictly(shard, singleton(imageAdId), BannerWithTurboLanding.class).get(0);

        assertThat(actualBanner.getTurboLandingId()).isEqualTo(defaultTurboLanding.getId());
    }

    @Test
    public void addTurboLandingToInvalidAd_failure() {
        GdAddTurboLandingToAds input = new GdAddTurboLandingToAds()
                .withAdIds(Arrays.asList(firstAdId, Long.MAX_VALUE))
                .withTurboLandingId(defaultTurboLanding.getId());

        GdUpdateAdsPayload gdUpdateAdsPayload = processor.doMutationAndGetPayload(UPDATE_ADS_MUTATION, input, operator);

        GdValidationResult expectedValidationResult = new GdValidationResult()
                .withErrors(
                        singletonList(new GdDefect()
                                .withCode("BannerDefectIds.Gen.AD_NOT_FOUND")
                                .withPath("adUpdateItems[1].id")));

        GdUpdateAdsPayload expectedPayload = new GdUpdateAdsPayload()
                .withUpdatedAds(singletonList(new GdUpdateAdPayloadItem().withId(firstAdId)))
                .withValidationResult(expectedValidationResult);

        assertThat(gdUpdateAdsPayload).is(matchedBy(beanDiffer(expectedPayload)));
    }

    @Test
    public void addNullTurboLandingToAd_success() {
        GdAddTurboLandingToAds input = new GdAddTurboLandingToAds()
                .withAdIds(singletonList(firstAdId))
                .withTurboLandingId(null);

        GdUpdateAdsPayload gdUpdateAdsPayload = processor.doMutationAndGetPayload(UPDATE_ADS_MUTATION, input, operator);

        validateUpdateSuccessful(gdUpdateAdsPayload, firstAdId);

        BannerWithTurboLanding firstActualBanner = bannerRepository.
                getStrictly(firstAd.getShard(), singletonList(firstAdId), BannerWithTurboLanding.class)
                .get(0);

        assertThat(firstActualBanner.getTurboLandingId()).isNull();
    }

    @Test
    public void addNullTurboLandingToAdWithoutHref_failure() {
        CreativeInfo creative = steps.creativeSteps().addDefaultHtml5CreativeForGeoproduct(clientInfo);
        OldCpmBanner cpmBanner = TestBanners.activeCpmBannerWithTurbolanding(
                defaultAdGroup.getCampaignId(),
                defaultAdGroup.getAdGroupId(),
                creative.getCreativeId(),
                defaultTurboLanding.getId());
        BannerInfo activeCpmBanner = steps.bannerSteps().createBanner(cpmBanner, defaultAdGroup);
        GdAddTurboLandingToAds input = new GdAddTurboLandingToAds()
                .withAdIds(singletonList(activeCpmBanner.getBannerId()))
                .withTurboLandingId(null);

        GdUpdateAdsPayload gdUpdateAdsPayload = processor.doMutationAndGetPayload(UPDATE_ADS_MUTATION, input, operator);

        GdValidationResult expectedValidationResult = new GdValidationResult()
                .withErrors(
                        singletonList(new GdDefect()
                                .withCode("BannerDefectIds.Gen.REQUIRED_HREF_OR_TURBOLANDING_ID")
                                .withPath("adUpdateItems[0]")));

        GdUpdateAdsPayload expectedPayload = new GdUpdateAdsPayload()
                .withUpdatedAds(Collections.emptyList())
                .withValidationResult(expectedValidationResult);

        assertThat(gdUpdateAdsPayload).is(matchedBy(beanDiffer(expectedPayload)));
    }

    private void validateUpdateSuccessful(GdUpdateAdsPayload gdUpdateAdsPayload, Long... bannerIds) {
        List<Long> updatedIds = mapList(gdUpdateAdsPayload.getUpdatedAds(), GdUpdateAdPayloadItem::getId);
        assertThat(updatedIds).containsExactlyInAnyOrder(bannerIds);
        assertThat(gdUpdateAdsPayload.getValidationResult()).isNull();
    }
}
