package ru.yandex.direct.grid.processing.service.banner;

import java.util.List;

import one.util.streamex.StreamEx;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.banner.model.BannerWithCreative;
import ru.yandex.direct.core.entity.banner.model.old.OldCpmBanner;
import ru.yandex.direct.core.entity.banner.model.old.OldCpmOutdoorBanner;
import ru.yandex.direct.core.entity.banner.model.old.OldTextBanner;
import ru.yandex.direct.core.entity.banner.repository.BannerTypedRepository;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.BannerCreativeInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.model.api.GdDefect;
import ru.yandex.direct.grid.processing.model.api.GdValidationResult;
import ru.yandex.direct.grid.processing.model.banner.GdAdType;
import ru.yandex.direct.grid.processing.model.banner.mutation.GdAddAdCreative;
import ru.yandex.direct.grid.processing.model.banner.mutation.GdUpdateAdPayloadItem;
import ru.yandex.direct.grid.processing.model.banner.mutation.GdUpdateAdsPayload;
import ru.yandex.direct.grid.processing.util.GraphQlTestExecutor;
import ru.yandex.direct.grid.processing.util.TestAuthHelper;
import ru.yandex.direct.grid.processing.util.UserHelper;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.core.testing.data.TestBanners.activeCpmBanner;
import static ru.yandex.direct.core.testing.data.TestBanners.activeCpmOutdoorBanner;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
public class AdGraphQlServiceMassCreativeTest {
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

    private static final String MUTATION_NAME = "addAdCreative";
    private static final GraphQlTestExecutor.TemplateMutation<GdAddAdCreative, GdUpdateAdsPayload>
            UPDATE_ADS_MUTATION =
            new GraphQlTestExecutor.TemplateMutation<>(MUTATION_NAME, MUTATION_TEMPLATE,
                    GdAddAdCreative.class, GdUpdateAdsPayload.class);

    @Autowired
    private GraphQlTestExecutor processor;

    @Autowired
    private Steps steps;

    @Autowired
    public BannerTypedRepository bannerRepository;

    private Long adId;
    private GdAdType adType;
    private User operator;
    private ClientInfo clientInfo;
    private Long creativeId;

    @Before
    public void before() {
        clientInfo = steps.clientSteps().createDefaultClient();
        operator = UserHelper.getUser(clientInfo.getClient());

        TestAuthHelper.setDirectAuthentication(operator);
    }

    @Test
    public void removeCreative_Success() {
        BannerCreativeInfo<OldTextBanner> bannerCreativeInfo =
                steps.bannerCreativeSteps().createTextBannerCreative(clientInfo);
        adId = bannerCreativeInfo.getBanner().getId();
        GdAddAdCreative input = new GdAddAdCreative()
                .withAdType(GdAdType.TEXT)
                .withAdIds(List.of(adId))
                .withCreativeId(null);

        GdUpdateAdsPayload gdUpdateAdsPayload = processor.doMutationAndGetPayload(UPDATE_ADS_MUTATION, input, operator);

        validateUpdateSuccessful(gdUpdateAdsPayload, adId);

        var actualBanner =
                bannerRepository.getSafely(clientInfo.getShard(), List.of(adId), BannerWithCreative.class).get(0);

        assertThat(actualBanner.getCreativeId()).isNull();
    }

    @Test
    public void addCreativeToText_Success() {
        creativeId = steps.creativeSteps().getNextCreativeId();
        AdGroupInfo adGroup = steps.adGroupSteps().createDefaultAdGroup(clientInfo);
        adType = GdAdType.TEXT;
        adId = steps.bannerSteps().createDefaultBanner(adGroup).getBannerId();
        steps.creativeSteps().addDefaultVideoAdditionCreative(clientInfo, creativeId);
        test();
    }

    @Test
    public void addCreativeToCpm_Success() {
        creativeId = steps.creativeSteps().getNextCreativeId();
        AdGroupInfo adGroup = steps.adGroupSteps().createActiveCpmBannerAdGroup(clientInfo);
        Long oldCreativeId = steps.creativeSteps().getNextCreativeId();
        steps.creativeSteps().addDefaultHtml5Creative(clientInfo, oldCreativeId);
        OldCpmBanner banner = activeCpmBanner(adGroup.getCampaignId(), adGroup.getAdGroupId(), oldCreativeId);
        adType = GdAdType.CPM_BANNER;
        adId = steps.bannerSteps().createActiveCpmBanner(banner, adGroup).getBannerId();
        steps.creativeSteps().addDefaultHtml5Creative(clientInfo, creativeId);
        test();
    }

    @Test
    public void addIncompatibleCreative_Error() {
        creativeId = steps.creativeSteps().getNextCreativeId();
        Long oldCreativeId = steps.creativeSteps().getNextCreativeId();
        steps.creativeSteps().addDefaultCpmOutdoorVideoCreative(clientInfo, oldCreativeId);
        AdGroupInfo adGroup = steps.adGroupSteps().createActiveCpmOutdoorAdGroup(clientInfo);
        OldCpmOutdoorBanner banner =
                activeCpmOutdoorBanner(adGroup.getCampaignId(), adGroup.getAdGroupId(), oldCreativeId);
        adType = GdAdType.CPM_OUTDOOR;
        adId = steps.bannerSteps().createActiveCpmOutdoorBanner(banner, adGroup).getBannerId();
        steps.creativeSteps().addDefaultCpmOutdoorVideoCreative(clientInfo, creativeId);

        GdAddAdCreative input = new GdAddAdCreative()
                .withAdType(GdAdType.CPM_OUTDOOR)
                .withAdIds(List.of(adId))
                .withCreativeId(creativeId);
        GdUpdateAdsPayload gdUpdateAdsPayload = processor.doMutationAndGetPayload(UPDATE_ADS_MUTATION, input, operator);

        GdValidationResult expectedValidationResult = new GdValidationResult().withErrors(
                List.of(new GdDefect().withCode("BannerDefectIds.Gen.FORBIDDEN_TO_CHANGE")
                        .withPath("adUpdateItems[0]")));
        GdUpdateAdsPayload expectedPayload = new GdUpdateAdsPayload()
                .withUpdatedAds(emptyList())
                .withValidationResult(expectedValidationResult);

        assertThat(gdUpdateAdsPayload).is(matchedBy(beanDiffer(expectedPayload)));
    }

    @Test
    public void addCompatibleCreativeToTwoAds_Success() {
        creativeId = steps.creativeSteps().getNextCreativeId();
        AdGroupInfo adGroup = steps.adGroupSteps().createDefaultAdGroup(clientInfo);
        adId = steps.bannerSteps().createDefaultBanner(adGroup).getBannerId();
        Long anotherAdId = steps.bannerSteps().createDefaultBanner(adGroup).getBannerId();
        steps.creativeSteps().addDefaultVideoAdditionCreative(clientInfo, creativeId);

        GdAddAdCreative input = new GdAddAdCreative()
                .withAdType(GdAdType.TEXT)
                .withAdIds(List.of(adId, anotherAdId))
                .withCreativeId(creativeId);
        GdUpdateAdsPayload gdUpdateAdsPayload = processor.doMutationAndGetPayload(UPDATE_ADS_MUTATION, input, operator);

        assertThat(gdUpdateAdsPayload.getValidationResult()).isNull();

        List<BannerWithCreative> banners =
                bannerRepository.getSafely(clientInfo.getShard(), List.of(adId, anotherAdId),
                        BannerWithCreative.class);

        List<Long> creativeIds = mapList(banners, BannerWithCreative::getCreativeId);

        assertThat(creativeIds).size().isEqualTo(2);
        assertThat(creativeIds).allMatch(id -> id.equals(creativeId));
    }

    @Test
    public void addCreativeToTwoAds_PartialSuccess() {
        creativeId = steps.creativeSteps().getNextCreativeId();
        AdGroupInfo textAdGroup = steps.adGroupSteps().createDefaultAdGroup(clientInfo);
        AdGroupInfo cpmAdGroup = steps.adGroupSteps().createActiveCpmBannerAdGroup(clientInfo);
        Long textAdId = steps.bannerSteps().createDefaultBanner(textAdGroup).getBannerId();
        Long cpmAdId = steps.bannerSteps().createDefaultBanner(cpmAdGroup).getBannerId();
        steps.creativeSteps().addDefaultVideoAdditionCreative(clientInfo, creativeId);

        GdAddAdCreative input = new GdAddAdCreative()
                .withAdType(GdAdType.TEXT)
                .withAdIds(List.of(textAdId, cpmAdId))
                .withCreativeId(creativeId);
        GdUpdateAdsPayload gdUpdateAdsPayload = processor.doMutationAndGetPayload(UPDATE_ADS_MUTATION, input, operator);

        GdValidationResult expectedValidationResult = new GdValidationResult().withErrors(
                List.of(new GdDefect().withCode("BannerDefectIds.Gen.INCONSISTENT_BANNER_TYPE")
                        .withPath("adUpdateItems[1]")));
        GdUpdateAdsPayload expectedPayload = new GdUpdateAdsPayload()
                .withUpdatedAds(List.of(new GdUpdateAdPayloadItem().withId(textAdId)))
                .withValidationResult(expectedValidationResult);

        assertThat(gdUpdateAdsPayload).is(matchedBy(beanDiffer(expectedPayload)));
    }

    private void test() {
        GdAddAdCreative input = new GdAddAdCreative()
                .withAdIds(List.of(adId))
                .withAdType(adType)
                .withCreativeId(creativeId);
        GdUpdateAdsPayload gdUpdateAdsPayload = processor.doMutationAndGetPayload(UPDATE_ADS_MUTATION, input, operator);

        validateUpdateSuccessful(gdUpdateAdsPayload, adId);

        var actualBanner =
                bannerRepository.getSafely(clientInfo.getShard(), List.of(adId), BannerWithCreative.class).get(0);

        assertThat(actualBanner.getCreativeId()).isNotNull();
        assertThat(actualBanner.getCreativeId()).isEqualTo(creativeId);
    }

    private void validateUpdateSuccessful(GdUpdateAdsPayload gdUpdateAdsPayload, Long... bannerId) {
        List<GdUpdateAdPayloadItem> updatedAds =
                StreamEx.of(bannerId).map(id -> new GdUpdateAdPayloadItem().withId(id)).toList();
        GdUpdateAdsPayload expectedGdUpdateAdsPayload = new GdUpdateAdsPayload()
                .withUpdatedAds(updatedAds);

        assertThat(gdUpdateAdsPayload).is(matchedBy(beanDiffer(expectedGdUpdateAdsPayload)));
    }

}
