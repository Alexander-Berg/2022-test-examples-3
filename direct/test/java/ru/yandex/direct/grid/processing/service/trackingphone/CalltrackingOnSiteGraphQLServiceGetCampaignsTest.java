package ru.yandex.direct.grid.processing.service.trackingphone;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import graphql.ExecutionResult;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.entity.user.service.UserService;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.DomainInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.context.container.GridGraphQLContext;
import ru.yandex.direct.grid.processing.model.trackingphone.GdCalltrackingOnSiteCampaigns;
import ru.yandex.direct.grid.processing.processor.GridGraphQLProcessor;
import ru.yandex.direct.grid.processing.service.dataloader.GridContextProvider;
import ru.yandex.direct.grid.processing.util.ContextHelper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.grid.processing.configuration.GridProcessingConfiguration.GRAPH_QL_PROCESSOR;
import static ru.yandex.direct.grid.processing.util.GraphQlJsonUtils.graphQlSerialize;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
public class CalltrackingOnSiteGraphQLServiceGetCampaignsTest {
    private static final String DOMAIN_POSTFIX = ".com";
    private static final String PHONE = "+74950350365";

    private static final String QUERY_TEMPLATE = "{\n" +
            "  client(searchBy: {login: \"%s\"}) {\n" +
            "    calltrackingOnSiteCampaigns(input: %s) {\n" +
            "      campaigns {\n" +
            "        id\n" +
            "      }\n" +
            "    }\n" +
            "  }\n" +
            "}";

    @Autowired
    private GridContextProvider gridContextProvider;

    @Autowired
    @Qualifier(GRAPH_QL_PROCESSOR)
    private GridGraphQLProcessor processor;
    @Autowired
    private Steps steps;
    @Autowired
    private UserService userService;

    private GridGraphQLContext context;
    private ClientInfo clientInfo;
    private DomainInfo domainInfo;
    private int shard;
    private long calltrackingSettingsId;

    @Before
    public void setUp() {
        clientInfo = steps.clientSteps().createDefaultClient();
        shard = clientInfo.getShard();
        User user = userService.getUser(clientInfo.getUid());
        domainInfo = steps.domainSteps().createDomain(clientInfo.getShard(), DOMAIN_POSTFIX);
        calltrackingSettingsId = steps.calltrackingSettingsSteps().add(clientInfo.getClientId(),
                domainInfo.getDomainId(), 1L, List.of(PHONE));

        context = ContextHelper.buildContext(user);
        gridContextProvider.setGridContext(context);
    }

    @After
    public void tearDown() {
        steps.calltrackingSettingsSteps().deleteAll(shard);
    }

    @Test
    public void happyPath() {
        CampaignInfo campaignInfo = steps.campaignSteps().createActiveTextCampaign(clientInfo);
        steps.campCalltrackingSettingsSteps().link(shard, campaignInfo.getCampaignId(), calltrackingSettingsId);

        GdCalltrackingOnSiteCampaigns input = new GdCalltrackingOnSiteCampaigns()
                .withUrl("https://" + domainInfo.getDomain().getDomain() + "/123");

        var query = String.format(QUERY_TEMPLATE, context.getOperator().getLogin(), graphQlSerialize(input));
        ExecutionResult result = processor.processQuery(null, query, null, context);
        assumeThat(result.getErrors(), hasSize(0));

        Map<String, Object> data = result.getData();

        Map<String, Object> expected = Collections.singletonMap(
                "client",
                ImmutableMap.of(
                        "calltrackingOnSiteCampaigns", ImmutableMap.of(
                                "campaigns", ImmutableList.of(
                                        ImmutableMap.of("id", campaignInfo.getCampaignId())
                                )
                        )
                )
        );
        assertThat(data).is(matchedBy(beanDiffer(expected)));
    }

    @Test
    public void getEmptyCalltrackingOnSiteCampaigns() {
        steps.campaignSteps().createActiveTextCampaign(clientInfo);

        GdCalltrackingOnSiteCampaigns input = new GdCalltrackingOnSiteCampaigns()
                .withUrl("https://" + domainInfo.getDomain().getDomain() + "/123");
        var query = String.format(QUERY_TEMPLATE, context.getOperator().getLogin(), graphQlSerialize(input));
        ExecutionResult result = processor.processQuery(null, query, null, context);
        assumeThat(result.getErrors(), hasSize(0));

        Map<String, Object> data = result.getData();

        Map<String, Object> expected = Collections.singletonMap(
                "client",
                ImmutableMap.of(
                        "calltrackingOnSiteCampaigns", ImmutableMap.of(
                                "campaigns", ImmutableList.of()
                        )
                )
        );

        assertThat(data).is(matchedBy(beanDiffer(expected)));
    }

}
