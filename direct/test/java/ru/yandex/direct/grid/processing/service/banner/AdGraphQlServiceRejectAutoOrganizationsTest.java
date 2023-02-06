package ru.yandex.direct.grid.processing.service.banner;

import java.util.List;
import java.util.Map;

import graphql.ExceptionWhileDataFetching;
import graphql.ExecutionResult;
import one.util.streamex.StreamEx;
import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.organization.model.BannerPermalink;
import ru.yandex.direct.core.entity.organizations.repository.OrganizationRepository;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.TextBannerInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.exception.GridValidationException;
import ru.yandex.direct.grid.processing.model.api.GdDefect;
import ru.yandex.direct.grid.processing.model.api.GdValidationResult;
import ru.yandex.direct.grid.processing.model.banner.mutation.GdUpdateAdPayloadItem;
import ru.yandex.direct.grid.processing.model.banner.mutation.GdUpdateAdsPayload;
import ru.yandex.direct.grid.processing.model.banner.mutation.GdUpdateOrganization;
import ru.yandex.direct.grid.processing.processor.GridGraphQLProcessor;
import ru.yandex.direct.grid.processing.util.TestAuthHelper;
import ru.yandex.direct.grid.processing.util.UserHelper;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.entity.organization.model.PermalinkAssignType.AUTO;
import static ru.yandex.direct.core.entity.organization.model.PermalinkAssignType.MANUAL;
import static ru.yandex.direct.core.testing.data.TestOrganizations.createBannerPermalink;
import static ru.yandex.direct.grid.processing.configuration.GridProcessingConfiguration.GRAPH_QL_PROCESSOR;
import static ru.yandex.direct.grid.processing.util.ContextHelper.buildContext;
import static ru.yandex.direct.grid.processing.util.GraphQlJsonUtils.convertValue;
import static ru.yandex.direct.grid.processing.util.GraphQlJsonUtils.graphQlSerialize;
import static ru.yandex.direct.validation.defect.CommonDefects.objectNotFound;
import static ru.yandex.direct.validation.defect.CommonDefects.validId;

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
public class AdGraphQlServiceRejectAutoOrganizationsTest {
    private static final long PERMALINK_ID = 123L;

    private static final String PREVIEW_MUTATION = "rejectAutoOrganizations";
    private static final String QUERY_TEMPLATE = ""
            + "mutation {\n"
            + "  %s (input: %s) {\n"
            + "  \tvalidationResult {\n"
            + "      errors {\n"
            + "        code\n"
            + "        path\n"
            + "        params\n"
            + "      },\n"
            + "      warnings {\n"
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

    private static final String MUTATION_NAME = "rejectAutoOrganizations";

    @Autowired
    public OrganizationRepository organizationRepository;
    @Autowired
    @Qualifier(GRAPH_QL_PROCESSOR)
    private GridGraphQLProcessor processor;
    @Autowired
    private Steps steps;

    private Integer shard;
    private User operator;
    private AdGroupInfo adGroupInfo;
    private Long bannerId;

    @Before
    public void before() {
        ClientInfo clientInfo = steps.clientSteps().createDefaultClient();
        shard = clientInfo.getShard();
        operator = UserHelper.getUser(clientInfo.getClient());
        adGroupInfo = steps.adGroupSteps().createDefaultAdGroup(clientInfo);
        TextBannerInfo defaultBannerInfo = steps.bannerSteps().createDefaultBanner(adGroupInfo);
        bannerId = defaultBannerInfo.getBannerId();
        TestAuthHelper.setDirectAuthentication(operator);
    }

    @Test
    public void rejectAutoOrganization_Success_Rejected() {
        organizationRepository.linkOrganizationsToBanners(shard, Map.of(bannerId, PERMALINK_ID), AUTO);

        String query = createQuery(
                List.of(new GdUpdateOrganization().withBannerId(bannerId).withPermalinkId(PERMALINK_ID))
        );
        Map<String, Object> data = processQueryAndGetResult(query);
        validateUpdateSuccessful(data, bannerId);

        List<BannerPermalink> result = organizationRepository
                .getBannerPermalinkByBannerIds(shard, List.of(bannerId))
                .get(bannerId);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(result).isNotNull();
            softly.assertThat(result).isEqualTo(List.of(createBannerPermalink(PERMALINK_ID, AUTO, true)));
        });
    }

    @Test
    public void rejectManualOrganization_Success_NotRejected() {
        organizationRepository.linkOrganizationsToBanners(shard, Map.of(bannerId, PERMALINK_ID), MANUAL);

        String query = createQuery(
                List.of(new GdUpdateOrganization().withBannerId(bannerId).withPermalinkId(PERMALINK_ID))
        );
        Map<String, Object> data = processQueryAndGetResult(query);
        validateUpdateSuccessful(data, bannerId);

        List<BannerPermalink> result = organizationRepository
                .getBannerPermalinkByBannerIds(shard, List.of(bannerId))
                .get(bannerId);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(result).isNotNull();
            softly.assertThat(result).isEqualTo(List.of(createBannerPermalink(PERMALINK_ID, MANUAL, false)));
        });
    }

    @Test
    public void rejectNotExistingOrganization_Success_NotRejected() {
        String query = createQuery(
                List.of(new GdUpdateOrganization().withBannerId(bannerId).withPermalinkId(PERMALINK_ID + 10))
        );
        Map<String, Object> data = processQueryAndGetResult(query);
        validateUpdateSuccessful(data, bannerId);
    }

    @Test
    public void rejectTwoAutoOrganizations_Success_Rejected() {
        TextBannerInfo secondBanner = steps.bannerSteps().createDefaultBanner(adGroupInfo);
        Long secondBannerId = secondBanner.getBannerId();

        organizationRepository.linkOrganizationsToBanners(shard, Map.of(bannerId, PERMALINK_ID), AUTO);
        organizationRepository.linkOrganizationsToBanners(shard, Map.of(secondBannerId, PERMALINK_ID), AUTO);

        String query = createQuery(List.of(
                new GdUpdateOrganization().withBannerId(bannerId).withPermalinkId(PERMALINK_ID),
                new GdUpdateOrganization().withBannerId(secondBannerId).withPermalinkId(PERMALINK_ID))
        );
        Map<String, Object> data = processQueryAndGetResult(query);
        validateUpdateSuccessful(data, bannerId, secondBannerId);

        Map<Long, List<BannerPermalink>> result = organizationRepository
                .getBannerPermalinkByBannerIds(shard, List.of(bannerId, secondBannerId));
        List<BannerPermalink> firstResult = result.get(bannerId);
        List<BannerPermalink> secondResult = result.get(secondBannerId);

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(firstResult).isNotNull();
            softly.assertThat(firstResult).isEqualTo(List.of(createBannerPermalink(PERMALINK_ID, AUTO, true)));
            softly.assertThat(secondResult).isNotNull();
            softly.assertThat(secondResult).isEqualTo(List.of(createBannerPermalink(PERMALINK_ID, AUTO, true)));
        });
    }

    @Test
    public void rejectFromNotOwnedBanner_ErrorNotFound() {
        ClientInfo secondClientInfo = steps.clientSteps().createDefaultClient();
        AdGroupInfo secondAdGroupInfo = steps.adGroupSteps().createDefaultAdGroup(secondClientInfo);
        TextBannerInfo secondBannerInfo = steps.bannerSteps().createDefaultBanner(secondAdGroupInfo);
        Long secondBannerId = secondBannerInfo.getBannerId();

        organizationRepository.linkOrganizationsToBanners(shard, Map.of(secondBannerId, PERMALINK_ID), AUTO);

        String query = createQuery(
                List.of(new GdUpdateOrganization().withBannerId(secondBannerId).withPermalinkId(PERMALINK_ID))
        );
        GdValidationResult validationResult = processQueryAndGetValidationResult(query);
        List<BannerPermalink> result = organizationRepository
                .getBannerPermalinkByBannerIds(secondClientInfo.getShard(), List.of(secondBannerId))
                .get(secondBannerId);

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(validationResult.getErrors()).containsExactly(new GdDefect()
                    .withCode(objectNotFound().defectId().getCode())
                    .withPath("[0].bannerId"));
            softly.assertThat(result).isNotNull();
            softly.assertThat(result).isEqualTo(List.of(createBannerPermalink(PERMALINK_ID, AUTO, false)));
        });
    }

    @Test
    public void reject_InvalidBannerId_ErrorMustBeValidId() {
        String query = createQuery(
                List.of(new GdUpdateOrganization().withBannerId(-1L).withPermalinkId(PERMALINK_ID))
        );
        GdValidationResult validationResult = processQueryAndGetValidationResult(query);

        assertThat(validationResult.getErrors()).containsExactly(new GdDefect()
                .withCode(validId().defectId().getCode())
                .withPath("[0].bannerId"));
    }

    @Test
    public void reject_InvalidPermalinkId_ErrorMustBeValidId() {
        String query = createQuery(
                List.of(new GdUpdateOrganization().withBannerId(bannerId).withPermalinkId(-1L))
        );
        GdValidationResult validationResult = processQueryAndGetValidationResult(query);

        assertThat(validationResult.getErrors()).containsExactly(new GdDefect()
                .withCode(validId().defectId().getCode())
                .withPath("[0].permalinkId"));
    }

    private String createQuery(List<GdUpdateOrganization> organizations) {
        return String.format(QUERY_TEMPLATE, PREVIEW_MUTATION, graphQlSerialize(organizations));
    }

    private Map<String, Object> processQueryAndGetResult(String query) {
        ExecutionResult result = processor.processQuery(null, query, null, buildContext(operator));
        assertThat(result.getErrors()).isEmpty();
        Map<String, Object> data = result.getData();
        assertThat(data.keySet()).contains(MUTATION_NAME);
        return data;
    }

    private GdValidationResult processQueryAndGetValidationResult(String query) {
        ExecutionResult result = processor.processQuery(null, query, null, buildContext(operator));
        assertThat(result.getErrors()).isNotEmpty();
        ExceptionWhileDataFetching fetchingException = (ExceptionWhileDataFetching) result.getErrors().get(0);
        GridValidationException gridValidationException = (GridValidationException) fetchingException.getException();
        return gridValidationException.getValidationResult();
    }

    private void validateUpdateSuccessful(Map<String, Object> data, Long... bannerId) {
        GdUpdateAdsPayload expectedGdUpdateAdsPayload = new GdUpdateAdsPayload()
                .withUpdatedAds(StreamEx.of(bannerId).map(id -> new GdUpdateAdPayloadItem().withId(id)).toList());

        GdUpdateAdsPayload gdUpdateAdsPayload =
                convertValue(data.get(MUTATION_NAME), GdUpdateAdsPayload.class);

        assertThat(gdUpdateAdsPayload).isEqualTo(expectedGdUpdateAdsPayload);
    }
}
