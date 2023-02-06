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

import ru.yandex.direct.core.entity.domain.repository.AggregatorDomainsRepository;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.model.banner.GdAdType;
import ru.yandex.direct.grid.processing.model.banner.mutation.GdAddAdsPayload;
import ru.yandex.direct.grid.processing.model.banner.mutation.GdAddMcBannerAd;
import ru.yandex.direct.grid.processing.model.banner.mutation.GdAddMcBannerAds;
import ru.yandex.direct.grid.processing.processor.GridGraphQLProcessor;
import ru.yandex.direct.grid.processing.util.TestAuthHelper;
import ru.yandex.direct.grid.processing.util.UserHelper;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.testing.data.TestBanners.VK_TEST_PUBLIC_AGGREGATOR_DOMAIN;
import static ru.yandex.direct.core.testing.data.TestBanners.VK_TEST_PUBLIC_HREF;
import static ru.yandex.direct.core.testing.data.TestBanners.defaultMcBannerImageFormat;
import static ru.yandex.direct.grid.processing.configuration.GridProcessingConfiguration.GRAPH_QL_PROCESSOR;
import static ru.yandex.direct.grid.processing.util.ContextHelper.buildContext;
import static ru.yandex.direct.grid.processing.util.GraphQlJsonUtils.convertValue;
import static ru.yandex.direct.grid.processing.util.GraphQlJsonUtils.graphQlSerialize;

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
public class AdGraphQlServiceAddMcBannerAdsTest {
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
            + "    addedAds {"
            + "      id"
            + "    }\n"
            + "  }\n"
            + "}";

    private static final String ADD_ADS_MUTATION = "addMcBannerAds";
    private static final String HREF = "https://yandex.ru";

    @Autowired
    @Qualifier(GRAPH_QL_PROCESSOR)
    private GridGraphQLProcessor processor;

    @Autowired
    private Steps steps;

    @Autowired
    public AggregatorDomainsRepository aggregatorDomainsRepository;

    private int shard;
    private User operator;
    private ClientInfo clientInfo;

    @Before
    public void before() {
        clientInfo = steps.clientSteps().createDefaultClient();
        shard = clientInfo.getShard();
        operator = UserHelper.getUser(clientInfo.getClient());
        TestAuthHelper.setDirectAuthentication(operator);
    }

    @Test
    public void addBanners_aggregatorDomain() {
        AdGroupInfo adGroupInfo = steps.adGroupSteps().createActiveMcBannerAdGroup(clientInfo);
        GdAddMcBannerAd gdAddAd = new GdAddMcBannerAd()
                .withAdType(GdAdType.MCBANNER)
                .withAdImageHash(createBannerImageHash())
                .withAdGroupId(adGroupInfo.getAdGroupId())
                .withHref(VK_TEST_PUBLIC_HREF);

        GdAddMcBannerAds gdAddAds = createAddRequest(gdAddAd);

        String query = createQuery(gdAddAds);
        GdAddAdsPayload addAdsPayload = processQueryAndGetResult(query);
        assertThat(addAdsPayload.getValidationResult()).isNull();
        Long bannerId = addAdsPayload.getAddedAds().get(0).getId();
        assertThat(getAggregatorDomain(bannerId)).isEqualTo(VK_TEST_PUBLIC_AGGREGATOR_DOMAIN);
    }

    @Test
    public void addBanners_notAggregatorDomain() {
        AdGroupInfo adGroupInfo = steps.adGroupSteps().createActiveMcBannerAdGroup(clientInfo);
        GdAddMcBannerAd gdAddAd = new GdAddMcBannerAd()
                .withAdType(GdAdType.MCBANNER)
                .withAdImageHash(createBannerImageHash())
                .withAdGroupId(adGroupInfo.getAdGroupId())
                .withHref(HREF);

        GdAddMcBannerAds gdAddAds = createAddRequest(gdAddAd);

        String query = createQuery(gdAddAds);
        GdAddAdsPayload addAdsPayload = processQueryAndGetResult(query);
        assertThat(addAdsPayload.getValidationResult()).isNull();
        Long bannerId = addAdsPayload.getAddedAds().get(0).getId();
        assertThat(getAggregatorDomain(bannerId)).isNull();
    }

    private String getAggregatorDomain(Long bannerId) {
        return aggregatorDomainsRepository.getAggregatorDomains(shard, List.of(bannerId)).get(bannerId);
    }

    private GdAddAdsPayload processQueryAndGetResult(String query) {
        ExecutionResult result = processor.processQuery(null, query, null, buildContext(operator));
        assertThat(result.getErrors()).isEmpty();
        Map<String, Object> data = result.getData();
        assertThat(data).containsOnlyKeys(ADD_ADS_MUTATION);
        return convertValue(data.get(ADD_ADS_MUTATION), GdAddAdsPayload.class);
    }

    private GdAddMcBannerAds createAddRequest(GdAddMcBannerAd... gdAddAd) {
        return new GdAddMcBannerAds()
                .withSaveDraft(true)
                .withAdAddItems(List.of(gdAddAd));
    }

    private String createQuery(GdAddMcBannerAds gdAddAds) {
        return String.format(QUERY_TEMPLATE, ADD_ADS_MUTATION, graphQlSerialize(gdAddAds));
    }

    private String createBannerImageHash() {
        return steps.bannerSteps()
                .createBannerImageFormat(clientInfo, defaultMcBannerImageFormat(null))
                .getImageHash();
    }
}
