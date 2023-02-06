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

import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.DynamicBannerInfo;
import ru.yandex.direct.core.testing.info.TextBannerInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.processor.GridGraphQLProcessor;
import ru.yandex.direct.grid.processing.util.GraphQlJsonUtils;
import ru.yandex.direct.grid.processing.util.UserHelper;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static ru.yandex.direct.core.testing.data.TestBanners.activeDynamicBanner;
import static ru.yandex.direct.grid.processing.configuration.GridProcessingConfiguration.GRAPH_QL_PROCESSOR;
import static ru.yandex.direct.grid.processing.util.ContextHelper.buildContext;
import static ru.yandex.direct.grid.processing.util.GraphQlJsonUtils.graphQlSerialize;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
public class AdGraphQlServiceGetAdsDisplayHrefsTest {
    private static final String GET_DISPLAY_HREFS_QUERY =
            "getAdsDisplayHrefs";
    private static final String QUERY_TEMPLATE = ""
            + "query {\n"
            + "  %s (adIds: %s)"
            + "}";

    @Autowired
    @Qualifier(GRAPH_QL_PROCESSOR)
    private GridGraphQLProcessor processor;
    @Autowired
    private Steps steps;

    private ClientInfo clientInfo;
    private User operator;

    @Before
    public void before() {
        clientInfo = steps.clientSteps().createDefaultClient();
        operator = UserHelper.getUser(clientInfo.getClient());
    }

    @Test
    public void getAdsDisplayHrefs() {
        TextBannerInfo textBanner =
                steps.bannerSteps().createActiveTextBanner(steps.campaignSteps().createActiveTextCampaign(clientInfo));
        DynamicBannerInfo dynamicBanner = steps.bannerSteps().createBanner(
                activeDynamicBanner(textBanner.getCampaignId(), textBanner.getAdGroupId())
                        .withDisplayHref("displayHref2"), textBanner.getAdGroupInfo());

        List<Long> request = asList(textBanner.getBannerId(), dynamicBanner.getBannerId());
        String query = String.format(QUERY_TEMPLATE, GET_DISPLAY_HREFS_QUERY, graphQlSerialize(request));
        ExecutionResult result = processor.processQuery(null, query, null, buildContext(operator));
        assertThat(result.getErrors()).isEmpty();

        List<String> expected =
                asList(textBanner.getBanner().getDisplayHref(), dynamicBanner.getBanner().getDisplayHref());
        Map<String, Object> data = result.getData();
        assertThat(data).containsOnlyKeys(GET_DISPLAY_HREFS_QUERY);

        List<String> payload = GraphQlJsonUtils.convertValue(data.get(GET_DISPLAY_HREFS_QUERY), ArrayList.class);
        assertThat(payload).is(matchedBy(containsInAnyOrder(expected.toArray())));
    }
}
