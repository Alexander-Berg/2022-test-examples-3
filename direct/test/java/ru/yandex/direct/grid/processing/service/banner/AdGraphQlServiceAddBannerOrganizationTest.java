package ru.yandex.direct.grid.processing.service.banner;

import java.util.List;
import java.util.Map;

import graphql.ExecutionResult;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.banner.model.old.OldBanner;
import ru.yandex.direct.core.entity.banner.model.old.OldBannerStatusModerate;
import ru.yandex.direct.core.entity.banner.model.old.OldBannerType;
import ru.yandex.direct.core.entity.banner.model.old.OldTextBanner;
import ru.yandex.direct.core.entity.banner.repository.old.OldBannerRepository;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.entity.user.repository.UserRepository;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.core.testing.stub.OrganizationsClientStub;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.model.banner.GdAdType;
import ru.yandex.direct.grid.processing.model.banner.mutation.GdAddAd;
import ru.yandex.direct.grid.processing.model.banner.mutation.GdAddAds;
import ru.yandex.direct.grid.processing.model.banner.mutation.GdAddAdsPayload;
import ru.yandex.direct.grid.processing.processor.GridGraphQLProcessor;
import ru.yandex.direct.grid.processing.util.TestAuthHelper;
import ru.yandex.direct.rbac.RbacService;

import static java.util.Collections.singletonList;
import static org.apache.commons.lang.math.RandomUtils.nextLong;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;
import static ru.yandex.direct.grid.processing.configuration.GridProcessingConfiguration.GRAPH_QL_PROCESSOR;
import static ru.yandex.direct.grid.processing.model.banner.GdAdType.TEXT;
import static ru.yandex.direct.grid.processing.util.ContextHelper.buildContext;
import static ru.yandex.direct.grid.processing.util.GraphQlJsonUtils.convertValue;
import static ru.yandex.direct.grid.processing.util.GraphQlJsonUtils.graphQlSerialize;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
public class AdGraphQlServiceAddBannerOrganizationTest {

    private static final String MUTATION_NAME = "addAds";
    private static final String QUERY_TEMPLATE = ""
            + "mutation {\n"
            + "  %s (input: %s) {\n"
            + "    validationResult {\n"
            + "      errors {\n"
            + "        code\n"
            + "        path\n"
            + "        params\n"
            + "      }\n"
            + "    }\n"
            + "    addedAds {"
            + "      id"
            + "    }\n"
            + "  }\n"
            + "}";

    @Autowired
    @Qualifier(GRAPH_QL_PROCESSOR)
    private GridGraphQLProcessor processor;

    @Autowired
    private Steps steps;

    @Autowired
    private OldBannerRepository bannerRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrganizationsClientStub organizationsClient;

    @Autowired
    private RbacService rbacService;

    private User operator;
    private int shard;
    private ClientInfo clientInfo;
    private ClientId clientId;
    private Long chiefUid;

    private final Long permalinkId = nextLong();

    @Before
    public void before() {
        clientInfo = steps.clientSteps().createDefaultClient();
        clientId = clientInfo.getClientId();
        shard = clientInfo.getShard();
        operator = userRepository.fetchByUids(shard, singletonList(clientInfo.getUid())).get(0);
        TestAuthHelper.setDirectAuthentication(operator);
        chiefUid = rbacService.getChiefByClientId(clientId);
    }

    @Test
    public void addAds_TextBannerWithOrganization_OrganizationAdded() {
        AdGroupInfo adGroupInfo = steps.adGroupSteps().createActiveTextAdGroup(clientInfo);
        GdAddAdsPayload gdAddAdsPayload = addBanner(adGroupInfo.getAdGroupId(), TEXT, true, permalinkId);
        validateAddSuccessful(gdAddAdsPayload);

        Long bannerId = gdAddAdsPayload.getAddedAds().get(0).getId();

        OldTextBanner expectedBanner = new OldTextBanner()
                .withBannerType(OldBannerType.TEXT)
                .withStatusModerate(OldBannerStatusModerate.NEW)
                .withPermalinkId(permalinkId);

        checkBanner(bannerId, expectedBanner);
    }

    private GdAddAdsPayload addBanner(Long adGroupId, GdAdType type, boolean saveDraft, Long permalinkId) {
        GdAddAd gdAddAd = new GdAddAd()
                .withAdType(type)
                .withTitle("title")
                .withBody("body")
                .withHref(null)
                .withIsMobile(false)
                .withAdGroupId(adGroupId)
                .withPermalinkId(permalinkId);

        GdAddAds gdAddCpmAds = new GdAddAds()
                .withSaveDraft(saveDraft)
                .withAdAddItems(singletonList(gdAddAd));

        String query = createQuery(gdAddCpmAds);
        organizationsClient.addUidsByPermalinkId(permalinkId, List.of(chiefUid));
        return processQueryAndGetResult(query);
    }

    private String createQuery(GdAddAds gdAddAds) {
        return String.format(QUERY_TEMPLATE, MUTATION_NAME, graphQlSerialize(gdAddAds));
    }

    private GdAddAdsPayload processQueryAndGetResult(String query) {
        ExecutionResult result = processor.processQuery(null, query, null, buildContext(operator));
        assertThat(result.getErrors()).isEmpty();

        Map<String, Object> data = result.getData();
        assertThat(data).containsOnlyKeys(MUTATION_NAME);

        return convertValue(data.get(MUTATION_NAME), GdAddAdsPayload.class);
    }

    private void validateAddSuccessful(GdAddAdsPayload actualGdAddAdsPayload) {
        assertThat(actualGdAddAdsPayload.getValidationResult()).isNull();
    }

    private void checkBanner(Long bannerId, OldBanner expectedBanner) {
        OldBanner actualBanner = getBanner(bannerId);
        assertThat(actualBanner).is(matchedBy(beanDiffer(expectedBanner).useCompareStrategy(onlyExpectedFields())));
    }

    private OldBanner getBanner(Long bannerId) {
        return bannerRepository.getBanners(shard, singletonList(bannerId)).get(0);
    }
}
