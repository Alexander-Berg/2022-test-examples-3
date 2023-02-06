package ru.yandex.direct.grid.processing.service.campaign;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableMap;
import graphql.ExecutionResult;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.context.container.GridGraphQLContext;
import ru.yandex.direct.grid.processing.model.campaign.GdCampaignsContainer;
import ru.yandex.direct.grid.processing.processor.GridGraphQLProcessor;
import ru.yandex.direct.grid.processing.service.dataloader.GridContextProvider;
import ru.yandex.direct.grid.processing.util.CampaignTestDataUtils;
import ru.yandex.direct.grid.processing.util.ContextHelper;
import ru.yandex.direct.grid.processing.util.GraphQLUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.core.testing.data.TestUsers.generateNewUser;
import static ru.yandex.direct.grid.processing.configuration.GridProcessingConfiguration.GRAPH_QL_PROCESSOR;
import static ru.yandex.direct.grid.processing.service.campaign.CampaignsGraphQlService.HAS_RUNNING_UNMODERATED_ADS_RESOLVER_NAME;
import static ru.yandex.direct.grid.processing.util.GraphQlJsonUtils.graphQlSerialize;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;

/**
 * Тест на сервис, проверяем в основном то, что фильтры и сортировки работают.
 */
@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
public class CampaignsHasRunningUnmoderatedBannersGraphQlServiceTest {

    private static final String QUERY_TEMPLATE = ""
            + "{\n"
            + "  client(searchBy: {login: \"%s\"}) {\n"
            + "    campaigns(input: %s) {\n"
            + "      rowset {\n"
            + "        status {\n"
            + "          %s\n"
            + "        }\n"
            + "      }\n"
            + "    }\n"
            + "  }\n"
            + "}\n";

    private GdCampaignsContainer campaignsContainer;
    private GridGraphQLContext context;

    @Autowired
    @Qualifier(GRAPH_QL_PROCESSOR)
    private GridGraphQLProcessor processor;

    @Autowired
    private Steps steps;

    @Autowired
    private GridContextProvider gridContextProvider;


    @Before
    public void initTestData() {
        UserInfo userInfo = steps.userSteps().createUser(generateNewUser());
        CampaignInfo defaultCampaign = steps.campaignSteps().createActiveTextCampaign(userInfo.getClientInfo());

        campaignsContainer = CampaignTestDataUtils.getDefaultCampaignsContainerInput();

        context = ContextHelper.buildContext(userInfo.getUser())
                .withFetchedFieldsReslover(null);
        gridContextProvider.setGridContext(context);
        campaignsContainer.getFilter().setCampaignIdIn(Set.of(defaultCampaign.getCampaignId()));
    }


    @Test
    public void testHasRunningUnmoderatedBanners() {
        String query = String.format(QUERY_TEMPLATE, context.getOperator().getLogin(),
                graphQlSerialize(campaignsContainer), HAS_RUNNING_UNMODERATED_ADS_RESOLVER_NAME);
        ExecutionResult result = processor.processQuery(null, query, null, context);

        GraphQLUtils.logErrors(result.getErrors());

        assertThat(result.getErrors()).isEmpty();

        Map<String, Object> data = result.getData();
        Map<String, Object> expected = Collections.singletonMap(
                "client", ImmutableMap.of(
                        "campaigns", Map.of(
                                "rowset", Collections.singletonList(Map.of(
                                                "status", Map.of(
                                                        HAS_RUNNING_UNMODERATED_ADS_RESOLVER_NAME, false)
                                        )
                                )
                        )
                )
        );

        assertThat(data)
                .is(matchedBy(beanDiffer(expected)));
    }
}
