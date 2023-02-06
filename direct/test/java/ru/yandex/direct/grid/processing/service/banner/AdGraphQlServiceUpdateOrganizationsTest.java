package ru.yandex.direct.grid.processing.service.banner;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import graphql.ExecutionResult;
import one.util.streamex.StreamEx;
import org.apache.commons.lang.math.RandomUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.CompareStrategy;
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies;
import ru.yandex.direct.core.entity.banner.model.old.DisplayHrefStatusModerate;
import ru.yandex.direct.core.entity.banner.model.old.OldBannerStatusPostModerate;
import ru.yandex.direct.core.entity.banner.model.old.OldTextBanner;
import ru.yandex.direct.core.entity.banner.repository.old.OldBannerRepository;
import ru.yandex.direct.core.entity.organization.model.Organization;
import ru.yandex.direct.core.entity.organizations.repository.OrganizationRepository;
import ru.yandex.direct.core.entity.trackingphone.model.ClientPhone;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.TextBannerInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.core.testing.stub.OrganizationsClientStub;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.model.api.GdDefect;
import ru.yandex.direct.grid.processing.model.banner.mutation.GdUpdateAdPayloadItem;
import ru.yandex.direct.grid.processing.model.banner.mutation.GdUpdateAdsPayload;
import ru.yandex.direct.grid.processing.model.banner.mutation.GdUpdateOrganization;
import ru.yandex.direct.grid.processing.processor.GridGraphQLProcessor;
import ru.yandex.direct.grid.processing.util.TestAuthHelper;
import ru.yandex.direct.grid.processing.util.UserHelper;

import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath.newPath;
import static ru.yandex.direct.core.entity.StatusBsSynced.NO;
import static ru.yandex.direct.core.entity.banner.model.old.OldBannerStatusModerate.READY;
import static ru.yandex.direct.core.entity.organizations.validation.OrganizationDefects.organizationNotFound;
import static ru.yandex.direct.grid.processing.configuration.GridProcessingConfiguration.GRAPH_QL_PROCESSOR;
import static ru.yandex.direct.grid.processing.util.ContextHelper.buildContext;
import static ru.yandex.direct.grid.processing.util.GraphQlJsonUtils.convertValue;
import static ru.yandex.direct.grid.processing.util.GraphQlJsonUtils.graphQlSerialize;

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
public class AdGraphQlServiceUpdateOrganizationsTest {
    private static final long PERMALINK = 123L;
    private static final CompareStrategy COMPARE_STRATEGY = DefaultCompareStrategies
            .allFieldsExcept(newPath("lastChange"), newPath("statusBsSynced"), newPath("permalinkId"), newPath("preferVCardOverPermalink"));

    private static final String PREVIEW_MUTATION = "updateOrganizations";
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

    private static final String MUTATION_NAME = "updateOrganizations";

    @Autowired
    public OldBannerRepository bannerRepository;
    @Autowired
    public OrganizationRepository organizationRepository;
    @Autowired
    @Qualifier(GRAPH_QL_PROCESSOR)
    private GridGraphQLProcessor processor;
    @Autowired
    private Steps steps;
    @Autowired
    private OrganizationsClientStub organizationsClient;

    private long bannerId;
    private User operator;
    private ClientId clientId;
    private TextBannerInfo defaultBannerInfo;
    private AdGroupInfo defaultAdGroup;

    @Before
    public void before() {
        ClientInfo clientInfo = steps.clientSteps().createDefaultClient();
        defaultAdGroup = steps.adGroupSteps().createDefaultAdGroup(clientInfo);
        defaultBannerInfo = steps.bannerSteps().createDefaultBanner(defaultAdGroup);
        bannerId = defaultBannerInfo.getBannerId();
        operator = UserHelper.getUser(clientInfo.getClient());
        clientId = clientInfo.getClientId();
        TestAuthHelper.setDirectAuthentication(operator);
        organizationsClient.addUidsByPermalinkId(PERMALINK, List.of(operator.getUid()));
    }

    @Test
    public void updateOneOrganization() {
        String query = createQuery(
                singletonList(new GdUpdateOrganization().withBannerId(bannerId).withPermalinkId(PERMALINK))
        );
        Map<String, Object> data = processQueryAndGetResult(query);
        validateUpdateSuccessful(data, bannerId);

        Organization result = organizationRepository
                .getOrganizationsByBannerIds(defaultBannerInfo.getShard(), clientId, singletonList(bannerId))
                .get(bannerId);
        assertThat(result, notNullValue());
        assertThat(result.getPermalinkId(), is(PERMALINK));

        OldTextBanner actualBanner =
                (OldTextBanner) bannerRepository.getBanners(defaultBannerInfo.getShard(), singletonList(bannerId)).get(0);
        assertThat(actualBanner, beanDiffer(resetModerationAndBsSynced(defaultBannerInfo.getBanner()))
                .useCompareStrategy(COMPARE_STRATEGY));
    }

    @Test
    public void updateTwoOrganizations() {
        TextBannerInfo secondBanner = steps.bannerSteps().createDefaultBanner(defaultAdGroup);
        Long secondBannerId = secondBanner.getBannerId();

        String query = createQuery(
                Arrays.asList(
                        new GdUpdateOrganization().withBannerId(bannerId).withPermalinkId(PERMALINK),
                        new GdUpdateOrganization().withBannerId(secondBannerId).withPermalinkId(PERMALINK))
        );
        Map<String, Object> data = processQueryAndGetResult(query);
        validateUpdateSuccessful(data, bannerId, secondBannerId);

        for (TextBannerInfo b : Arrays.asList(defaultBannerInfo, secondBanner)) {
            Long bid = b.getBannerId();
            Organization result = organizationRepository
                    .getOrganizationsByBannerIds(defaultBannerInfo.getShard(), clientId, singletonList(bid))
                    .get(bid);
            assertThat(result, notNullValue());
            assertThat(result.getPermalinkId(), is(PERMALINK));

            OldTextBanner actualBanner = (OldTextBanner) bannerRepository
                    .getBanners(b.getShard(), singletonList(bid)).get(0);
            assertThat(actualBanner, beanDiffer(resetModerationAndBsSynced(b.getBanner()))
                    .useCompareStrategy(COMPARE_STRATEGY));
        }
    }

    @Test
    public void deleteOrganization() {
        updateOneOrganization();

        String query = createQuery(singletonList(new GdUpdateOrganization().withBannerId(bannerId)));
        Map<String, Object> data = processQueryAndGetResult(query);
        validateUpdateSuccessful(data, bannerId);

        Organization result = organizationRepository
                .getOrganizationsByBannerIds(defaultBannerInfo.getShard(), clientId, singletonList(bannerId))
                .get(bannerId);
        assertThat(result, nullValue());

        OldTextBanner actualBanner =
                (OldTextBanner) bannerRepository.getBanners(defaultBannerInfo.getShard(), singletonList(bannerId)).get(0);
        assertThat(actualBanner, beanDiffer(resetModerationAndBsSynced(defaultBannerInfo.getBanner()))
                .useCompareStrategy(COMPARE_STRATEGY));
    }

    @Test
    public void deleteOrganization_bannerWithPhone() {
        long permalinkId = RandomUtils.nextLong();
        steps.organizationSteps().createClientOrganization(clientId, permalinkId);
        ClientPhone phone = steps.clientPhoneSteps().addDefaultClientManualPhone(clientId);
        steps.organizationSteps().linkOrganizationToBanner(clientId, permalinkId, bannerId);
        steps.clientPhoneSteps().linkPhoneIdToBanner(defaultBannerInfo.getShard(), bannerId, phone.getId());

        String query = createQuery(singletonList(new GdUpdateOrganization().withBannerId(bannerId)));
        Map<String, Object> data = processQueryAndGetResult(query);
        validateUpdateSuccessful(data, bannerId);

        Organization result = organizationRepository
                .getOrganizationsByBannerIds(defaultBannerInfo.getShard(), clientId, singletonList(bannerId))
                .get(bannerId);
        assertThat(result, nullValue());

        OldTextBanner actualBanner =
                (OldTextBanner) bannerRepository.getBanners(defaultBannerInfo.getShard(), singletonList(bannerId)).get(0);
        assertThat(actualBanner, beanDiffer(resetModerationAndBsSynced(defaultBannerInfo.getBanner()))
                .useCompareStrategy(COMPARE_STRATEGY));
    }

    @Test
    public void updateNotOwnedOrganization_errorNotFound() {
        String query = createQuery(
                singletonList(new GdUpdateOrganization().withBannerId(bannerId).withPermalinkId(PERMALINK + 1))
        );
        ExecutionResult result = processor.processQuery(null, query, null, buildContext(operator));
        Map<String, Object> data = result.getData();
        GdUpdateAdsPayload payload = convertValue(data.get(MUTATION_NAME), GdUpdateAdsPayload.class);

        assertThat(payload.getUpdatedAds(), empty());
        assertThat(payload.getValidationResult().getErrors(),
                contains(new GdDefect()
                        .withCode(organizationNotFound().defectId().getCode())
                        .withPath("adUpdateItems[0].permalinkId")));
    }

    @Test
    public void updateOneOrganization_invalidRequest() {
        String query = createQuery(
                singletonList(new GdUpdateOrganization().withBannerId(bannerId).withPermalinkId(-1L))
        );
        ExecutionResult result = processor.processQuery(null, query, null, buildContext(operator));
        assertThat(result.getErrors(), hasSize(1));
    }

    private Map<String, Object> processQueryAndGetResult(String query) {
        ExecutionResult result = processor.processQuery(null, query, null, buildContext(operator));
        assertThat(result.getErrors(), empty());
        Map<String, Object> data = result.getData();
        assertThat(data.keySet(), contains(MUTATION_NAME));
        return data;
    }

    private void validateUpdateSuccessful(Map<String, Object> data, Long... bannerId) {
        GdUpdateAdsPayload expectedGdUpdateAdsPayload = new GdUpdateAdsPayload()
                .withUpdatedAds(StreamEx.of(bannerId).map(id -> new GdUpdateAdPayloadItem().withId(id)).toList());

        GdUpdateAdsPayload gdUpdateAdsPayload =
                convertValue(data.get(MUTATION_NAME), GdUpdateAdsPayload.class);

        assertThat(gdUpdateAdsPayload, beanDiffer(expectedGdUpdateAdsPayload));
    }

    private static String createQuery(List<GdUpdateOrganization> organizations) {
        return String.format(QUERY_TEMPLATE, PREVIEW_MUTATION, graphQlSerialize(organizations));
    }

    private static OldTextBanner resetModerationAndBsSynced(OldTextBanner banner) {
        return banner
                .withStatusModerate(READY)
                .withStatusPostModerate(OldBannerStatusPostModerate.NO)
                .withStatusBsSynced(NO)
                .withDisplayHrefStatusModerate(DisplayHrefStatusModerate.READY);
    }
}
