package ru.yandex.direct.grid.processing.service.banner;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import graphql.ExecutionResult;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.banner.model.old.OldCpcVideoBanner;
import ru.yandex.direct.core.entity.banner.repository.old.OldBannerTurboGalleriesRepository;
import ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.BannerCreativeInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.TextBannerInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.model.api.GdValidationResult;
import ru.yandex.direct.grid.processing.model.banner.mutation.GdUpdateAdPayloadItem;
import ru.yandex.direct.grid.processing.model.banner.mutation.GdUpdateAdTurboGalleryHref;
import ru.yandex.direct.grid.processing.model.banner.mutation.GdUpdateAdTurboGalleryHrefsInput;
import ru.yandex.direct.grid.processing.model.banner.mutation.GdUpdateAdsPayload;
import ru.yandex.direct.grid.processing.processor.GridGraphQLProcessor;
import ru.yandex.direct.grid.processing.util.TestAuthHelper;
import ru.yandex.direct.grid.processing.util.UserHelper;

import static java.util.Collections.singletonList;
import static java.util.Map.entry;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.core.testing.data.TestBanners.activeTextBanner;
import static ru.yandex.direct.grid.processing.configuration.GridProcessingConfiguration.GRAPH_QL_PROCESSOR;
import static ru.yandex.direct.grid.processing.util.ContextHelper.buildContext;
import static ru.yandex.direct.grid.processing.util.GraphQlJsonUtils.convertValue;
import static ru.yandex.direct.grid.processing.util.GraphQlJsonUtils.graphQlSerialize;
import static ru.yandex.direct.grid.processing.util.validation.GridValidationHelper.toGdValidationResult;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
public class AdGraphQlServiceUpdateTurboGalleryHrefsTest {
    private static final String QUERY_TEMPLATE = ""
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

    private static final String UPDATE_ADS_MUTATION = "updateTurboGalleryHrefs";
    private static final String TURBO_GALLERY_HREF = "https://yandex.ru/turbo?text";

    @Autowired
    @Qualifier(GRAPH_QL_PROCESSOR)
    private GridGraphQLProcessor processor;

    @Autowired
    private Steps steps;

    @Autowired
    private OldBannerTurboGalleriesRepository turboGalleriesRepository;

    private int shard;
    private long bannerId;
    private AdGroupInfo defaultAdGroup;
    private User operator;
    private ClientInfo clientInfo;

    @Before
    public void before() {
        clientInfo = steps.clientSteps().createDefaultClient();
        shard = clientInfo.getShard();
        defaultAdGroup = steps.adGroupSteps().createDefaultAdGroup(clientInfo);
        TextBannerInfo defaultBanner = steps.bannerSteps().createDefaultBanner(defaultAdGroup);
        bannerId = defaultBanner.getBannerId();
        operator = UserHelper.getUser(clientInfo.getClient());
        TestAuthHelper.setDirectAuthentication(operator);
    }

    @Test
    public void updateTurboGalleryHrefs_addNew() {
        GdUpdateAdTurboGalleryHref gdUpdateAdTurboGalleryHref = new GdUpdateAdTurboGalleryHref()
                .withBannerId(bannerId)
                .withHref(TURBO_GALLERY_HREF);

        String query = createQuery(singletonList(gdUpdateAdTurboGalleryHref));
        Map<String, Object> data = processQueryAndGetResult(query);
        validateUpdateSuccessful(singletonList(bannerId), data);

        String addedTurboGalleryHref = turboGalleriesRepository.getTurboGalleriesByBannerIds(shard,
                singletonList(bannerId)).get(bannerId);
        assertThat(addedTurboGalleryHref).isEqualTo(TURBO_GALLERY_HREF);
    }

    @Test
    public void updateTurboGalleryHrefs_nonexistentBannerId() {
        Long invalidId = 120000L;
        GdUpdateAdTurboGalleryHref gdUpdateAdTurboGalleryHref = new GdUpdateAdTurboGalleryHref()
                .withBannerId(invalidId)
                .withHref(TURBO_GALLERY_HREF);

        String query = createQuery(singletonList(gdUpdateAdTurboGalleryHref));
        Map<String, Object> data = processQueryAndGetResult(query);

        GdValidationResult expectedGdValidationResult = toGdValidationResult(
                path(field(GdUpdateAdsPayload.UPDATED_ADS.name()), index(0), field(GdUpdateAdPayloadItem.ID.name())),
                BannerDefects.adNotFound())
                .withWarnings(null);

        validateUpdateException(expectedGdValidationResult, data);
    }

    @Test
    public void updateTurboGalleryHrefs_invalidBannerType() {
        AdGroupInfo activeCpmVideoAdGroup = steps.adGroupSteps().createActiveCpmVideoAdGroup(clientInfo);
        BannerCreativeInfo<OldCpcVideoBanner> cpcVideoBannerCreative =
                steps.bannerCreativeSteps().createCpcVideoBannerCreative(clientInfo);

        Long bannerId = steps.bannerSteps()
                .createDefaultCpcVideoBanner(activeCpmVideoAdGroup, cpcVideoBannerCreative.getCreativeId())
                .getBannerId();
        GdUpdateAdTurboGalleryHref gdUpdateAdTurboGalleryHref = new GdUpdateAdTurboGalleryHref()
                .withBannerId(bannerId)
                .withHref(TURBO_GALLERY_HREF);

        String query = createQuery(singletonList(gdUpdateAdTurboGalleryHref));
        Map<String, Object> data = processQueryAndGetResult(query);

        GdValidationResult expectedGdValidationResult = toGdValidationResult(
                path(field(GdUpdateAdsPayload.UPDATED_ADS.name()), index(0)),
                BannerDefects.inconsistentBannerType())
                .withWarnings(null);

        validateUpdateException(expectedGdValidationResult, data);
    }

    @Test
    public void updateTurboGalleryHrefs_update_success() {
        List<Long> bannerIds = new ArrayList<>();
        bannerIds.add(createBannerWithDefaultTurboGalleryAndGetId());
        bannerIds.add(createBannerWithDefaultTurboGalleryAndGetId());
        Map<Long, String> existedTurboGalleriesByBannerIds =
                turboGalleriesRepository.getTurboGalleriesByBannerIds(shard,
                        bannerIds);
        assertThat(existedTurboGalleriesByBannerIds)
                .contains(entry(bannerIds.get(0), TURBO_GALLERY_HREF), entry(bannerIds.get(1), TURBO_GALLERY_HREF));

        String newTurboGalleryHref = TURBO_GALLERY_HREF + "new";

        String query = createQuery(bannerIds, newTurboGalleryHref);
        Map<String, Object> data = processQueryAndGetResult(query);
        validateUpdateSuccessful(bannerIds, data);

        Map<Long, String> addedTurboGalleryHrefByBannerIds =
                turboGalleriesRepository.getTurboGalleriesByBannerIds(shard,
                        bannerIds);
        assertThat(addedTurboGalleryHrefByBannerIds)
                .contains(entry(bannerIds.get(0), newTurboGalleryHref), entry(bannerIds.get(1), newTurboGalleryHref));
    }

    @Test
    public void updateTurboGalleryHrefs_delete_success() {
        Long bannerId = createBannerWithDefaultTurboGalleryAndGetId();

        String newTurboGalleryHref = null;
        GdUpdateAdTurboGalleryHref gdUpdateAdTurboGalleryHref = new GdUpdateAdTurboGalleryHref()
                .withBannerId(bannerId)
                .withHref(newTurboGalleryHref);

        String query = createQuery(singletonList(gdUpdateAdTurboGalleryHref));
        Map<String, Object> data = processQueryAndGetResult(query);
        validateUpdateSuccessful(singletonList(bannerId), data);

        Map<Long, String> result = turboGalleriesRepository.getTurboGalleriesByBannerIds(shard,
                singletonList(bannerId));
        assertThat(result).isEmpty();
    }

    private Long createBannerWithDefaultTurboGalleryAndGetId() {
        return steps.bannerSteps()
                .createBanner(activeTextBanner().withTurboGalleryHref(TURBO_GALLERY_HREF), defaultAdGroup)
                .getBanner()
                .getId();
    }

    private Map<String, Object> processQueryAndGetResult(String query) {
        ExecutionResult result = processor.processQuery(null, query, null, buildContext(operator));
        assertThat(result.getErrors()).isEmpty();
        Map<String, Object> data = result.getData();
        assertThat(data).containsOnlyKeys(UPDATE_ADS_MUTATION);
        return data;
    }

    private void validateUpdateException(GdValidationResult expectedGdValidationResult, Map<String, Object> data) {
        GdUpdateAdsPayload gdUpdateAdsPayload =
                convertValue(data.get(UPDATE_ADS_MUTATION), GdUpdateAdsPayload.class);

        assertThat(gdUpdateAdsPayload.getValidationResult().getErrors()).isNotNull();
        assertThat(gdUpdateAdsPayload.getValidationResult())
                .is(matchedBy(beanDiffer(expectedGdValidationResult)));

    }

    private void validateUpdateSuccessful(List<Long> bannerIds, Map<String, Object> data) {
        List<GdUpdateAdPayloadItem> payload = new ArrayList<>();
        for (Long bannerId : bannerIds) {
            payload.add(new GdUpdateAdPayloadItem().withId(bannerId));
        }
        GdUpdateAdsPayload expectedGdUpdateAdsPayload = new GdUpdateAdsPayload()
                .withUpdatedAds(payload);

        GdUpdateAdsPayload gdUpdateAdsPayload =
                convertValue(data.get(UPDATE_ADS_MUTATION), GdUpdateAdsPayload.class);

        assertThat(gdUpdateAdsPayload)
                .is(matchedBy(beanDiffer(expectedGdUpdateAdsPayload)));
    }

    private String createQuery(List<GdUpdateAdTurboGalleryHref> gdUpdateAds) {
        GdUpdateAdTurboGalleryHrefsInput input =
                new GdUpdateAdTurboGalleryHrefsInput().withHrefUpdateItems(gdUpdateAds);
        return String.format(QUERY_TEMPLATE, UPDATE_ADS_MUTATION, graphQlSerialize(input));
    }

    private String createQuery(List<Long> bannerIds, String newTurboGalleryHref) {
        List<GdUpdateAdTurboGalleryHref> requestList = new ArrayList<>();
        for (Long bannerId : bannerIds) {
            GdUpdateAdTurboGalleryHref gdUpdateAdTurboGalleryHref = new GdUpdateAdTurboGalleryHref()
                    .withBannerId(bannerId)
                    .withHref(newTurboGalleryHref);
            requestList.add(gdUpdateAdTurboGalleryHref);
        }
        GdUpdateAdTurboGalleryHrefsInput input =
                new GdUpdateAdTurboGalleryHrefsInput().withHrefUpdateItems(requestList);
        return String.format(QUERY_TEMPLATE, UPDATE_ADS_MUTATION, graphQlSerialize(input));
    }
}
