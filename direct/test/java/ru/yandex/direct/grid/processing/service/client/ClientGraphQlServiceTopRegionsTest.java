package ru.yandex.direct.grid.processing.service.client;

import java.util.List;
import java.util.Map;

import graphql.ExecutionResult;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.adgroup.repository.AdGroupRepository;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.entity.user.repository.UserRepository;
import ru.yandex.direct.core.testing.data.TestCampaigns;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.core.testing.steps.campaign.model0.Campaign;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.model.GdLimitOffset;
import ru.yandex.direct.grid.processing.model.client.GdClientSearchRequest;
import ru.yandex.direct.grid.processing.model.client.GdClientTopRegionInfo;
import ru.yandex.direct.grid.processing.model.client.GdClientTopRegions;
import ru.yandex.direct.grid.processing.model.client.GdClientTopRegionsPayload;
import ru.yandex.direct.grid.processing.processor.GridGraphQLProcessor;
import ru.yandex.direct.grid.processing.util.GraphQlJsonUtils;
import ru.yandex.direct.grid.processing.util.TestAuthHelper;
import ru.yandex.direct.regions.Region;
import ru.yandex.direct.utils.ListUtils;

import static java.util.List.of;
import static org.hamcrest.Matchers.empty;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.core.testing.data.TestClients.defaultClient;
import static ru.yandex.direct.core.testing.data.TestGroups.defaultTextAdGroup;
import static ru.yandex.direct.grid.processing.configuration.GridProcessingConfiguration.GRAPH_QL_PROCESSOR;
import static ru.yandex.direct.grid.processing.util.ContextHelper.buildContext;
import static ru.yandex.direct.grid.processing.util.GraphQlJsonUtils.graphQlSerialize;
import static ru.yandex.direct.regions.Region.MOSCOW_REGION_ID;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
public class ClientGraphQlServiceTopRegionsTest {

    @Autowired
    @Qualifier(GRAPH_QL_PROCESSOR)
    protected GridGraphQLProcessor processor;

    @Autowired
    Steps steps;

    @Autowired
    AdGroupRepository adGroupRepository;

    @Autowired
    UserRepository userRepository;

    private static final String QUERY_NAME = "adGroupTopRegions";
    private static final String QUERY_TEMPLATE = ""
            + "query {\n" +
            "  getReqId\n" +
            "  %s (input : %s) {\n" +
            "      regions {\n" +
            "        count,\n" +
            "        regionIds\n" +
            "      }\n" +
            "  }\n" +
            "}";

    private Integer shard;
    private ClientInfo clientInfo;
    private User operator;
    private Long campaignIdWithGeo;

    private static final List<Long> AD_GROUP_GEO = of(MOSCOW_REGION_ID, Region.UDMURT_REPUBLIC_REGION_ID);

    @Before
    public void before() {
        clientInfo =
                steps.clientSteps().createClient(defaultClient().withCountryRegionId(Region.KAZAKHSTAN_REGION_ID));

        shard = clientInfo.getShard();
        operator = clientInfo.getChiefUserInfo().getUser();
        TestAuthHelper.setDirectAuthentication(operator);

        Campaign campaignWithGeo = TestCampaigns.activeTextCampaign(clientInfo.getClientId(), clientInfo.getUid());

        CampaignInfo campaignInfo = steps.campaignSteps().createCampaign(campaignWithGeo, clientInfo);
        campaignIdWithGeo = campaignInfo.getCampaignId();

        List<Long> adGroupGeo = List.of(Region.MOSCOW_REGION_ID, Region.UDMURT_REPUBLIC_REGION_ID);

        AdGroup firstAdGroup = defaultTextAdGroup(campaignIdWithGeo).withGeo(adGroupGeo);
        steps.adGroupSteps().createAdGroup(firstAdGroup, campaignInfo);
    }

    @Test
    public void getTopRegions_success() {

        GdClientTopRegions request = new GdClientTopRegions()
                .withLimitOffset(new GdLimitOffset()
                    .withOffset(0)
                    .withLimit(3))
                .withSearchRequest(new GdClientSearchRequest()
                    .withId(clientInfo.getClientId().asLong()));

        String query = String.format(QUERY_TEMPLATE, QUERY_NAME, graphQlSerialize(request));

        ExecutionResult executionResult = processor.processQuery(null, query, null, buildContext(operator));
        assumeThat(executionResult.getErrors(), empty());

        Map<String, Object> data = executionResult.getData();
        GdClientTopRegionsPayload payload = GraphQlJsonUtils.convertValue(data.get(QUERY_NAME), GdClientTopRegionsPayload.class);

        List<GdClientTopRegionInfo> expected = of(new GdClientTopRegionInfo()
                .withRegionIds(ListUtils.longToIntegerList(AD_GROUP_GEO))
                .withCount(1));
        Assert.assertThat(payload.getRegions(), beanDiffer(expected));
    }
}
