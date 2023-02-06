package ru.yandex.direct.grid.processing.service.showcondition;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import graphql.ExecutionResult;
import org.jooq.Select;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.campaign.model.CampaignsPlatform;
import ru.yandex.direct.core.entity.keyword.model.Keyword;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.entity.user.repository.UserRepository;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.KeywordInfo;
import ru.yandex.direct.core.testing.steps.KeywordSteps;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.grid.core.util.yt.YtDynamicSupport;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.context.container.GridGraphQLContext;
import ru.yandex.direct.grid.processing.model.showcondition.GdShowConditionFilter;
import ru.yandex.direct.grid.processing.model.showcondition.GdShowConditionsContainer;
import ru.yandex.direct.grid.processing.processor.GridGraphQLProcessor;
import ru.yandex.direct.grid.processing.service.dataloader.GridContextProvider;
import ru.yandex.direct.grid.processing.service.showcondition.tools.ShowConditionCommonUtils;
import ru.yandex.direct.grid.processing.util.ContextHelper;
import ru.yandex.yt.ytclient.wire.UnversionedRowset;

import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static ru.yandex.direct.core.testing.data.TestKeywords.defaultKeyword;
import static ru.yandex.direct.grid.processing.configuration.GridProcessingConfiguration.GRAPH_QL_PROCESSOR;
import static ru.yandex.direct.grid.processing.service.showcondition.tools.ShowConditionCommonUtils.getAnswer;
import static ru.yandex.direct.grid.processing.util.GraphQLUtils.checkErrors;
import static ru.yandex.direct.grid.processing.util.GraphQLUtils.getDataValue;
import static ru.yandex.direct.grid.processing.util.GraphQLUtils.list;
import static ru.yandex.direct.grid.processing.util.GraphQlJsonUtils.graphQlSerialize;
import static ru.yandex.direct.grid.processing.util.ShowConditionTestDataUtils.getDefaultGdShowConditionsContainer;

/**
 * Проверяем правильность возврата допустимых операций над условиями показа
 */
@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
public class ShowConditionGraphQlServiceAccessesActionsTest {

    private static final String QUERY_TEMPLATE = ""
            + "{\n"
            + "  client(searchBy: {login: \"%s\"}) {\n"
            + "      showConditions(input: %s) {\n"
            + "          rowset {\n"
            + "            id\n"
            + "            access {\n"
            + "              actions\n"
            + "            }\n"
            + "         }\n"
            + "    }\n"
            + "  }\n"
            + "}\n";

    @Autowired
    @Qualifier(GRAPH_QL_PROCESSOR)
    private GridGraphQLProcessor processor;
    @Autowired
    private Steps steps;
    @Autowired
    UserRepository userRepository;
    @Autowired
    private KeywordSteps keywordSteps;
    @Autowired
    private YtDynamicSupport gridYtSupport;
    @Autowired
    private GridContextProvider gridContextProvider;

    private ClientInfo clientInfo;
    private CampaignInfo campaignInfo;
    private AdGroupInfo adGroupInfo;
    private GridGraphQLContext context;

    @Before
    public void setUp() {
        clientInfo = steps.clientSteps().createDefaultClient();
        campaignInfo = steps.campaignSteps().createActiveCampaign(clientInfo, CampaignsPlatform.SEARCH);
        adGroupInfo = steps.adGroupSteps().createDefaultAdGroup(campaignInfo);

        User user = userRepository.fetchByUids(clientInfo.getShard(), singletonList(clientInfo.getUid())).get(0);
        context = ContextHelper.buildContext(user);
        gridContextProvider.setGridContext(context);
    }

    @Test
    public void getAccessesActions_success() {
        KeywordInfo keywordInfo = keywordSteps.createKeyword(adGroupInfo, defaultKeyword());
        Answer<UnversionedRowset> answer = getAnswer(list(adGroupInfo), list(keywordInfo));
        doAnswer(answer).when(gridYtSupport).selectRows(eq(clientInfo.getShard()), any(Select.class), anyBoolean());

        ExecutionResult result = processQuery(campaignInfo.getCampaignId(), keywordInfo.getId());
        checkErrors(result.getErrors());

        Map<String, Object> data = result.getData();

        List<String> actions = getDataValue(data, "client/showConditions/rowset/0/access/actions");
        assertThat(actions).containsExactlyInAnyOrder("SUSPEND", "DELETE");
    }

    @Test
    public void getAccessesActions_whenKeywordSuspended_success() {
        Keyword keyword = defaultKeyword().withIsSuspended(true);
        KeywordInfo keywordInfo = keywordSteps.createKeyword(adGroupInfo, keyword);
        Answer<UnversionedRowset> answer = getAnswer(list(adGroupInfo), list(keywordInfo));
        doAnswer(answer).when(gridYtSupport).selectRows(eq(clientInfo.getShard()), any(Select.class), anyBoolean());

        ExecutionResult result = processQuery(campaignInfo.getCampaignId(), keywordInfo.getId());
        checkErrors(result.getErrors());

        Map<String, Object> data = result.getData();

        List<String> actions = getDataValue(data, "client/showConditions/rowset/0/access/actions");
        assertThat(actions).containsExactlyInAnyOrder("UNSUSPEND", "DELETE");
    }

    @Test
    public void getAccessesActions_whenCampaignArchived_success() {
        Keyword keyword = defaultKeyword();
        KeywordInfo keywordInfo = keywordSteps.createKeyword(adGroupInfo, keyword);
        steps.campaignSteps().archiveCampaign(campaignInfo.getShard(), campaignInfo.getCampaignId());
        Answer<UnversionedRowset> answer = getAnswer(list(adGroupInfo), list(keywordInfo));
        doAnswer(answer).when(gridYtSupport).selectRows(eq(clientInfo.getShard()), any(Select.class), anyBoolean());

        ExecutionResult result = processQuery(campaignInfo.getCampaignId(), keywordInfo.getId());
        checkErrors(result.getErrors());

        Map<String, Object> data = result.getData();

        List<String> actions = getDataValue(data, "client/showConditions/rowset/0/access/actions");
        assertThat(actions).isEmpty();
    }

    private ExecutionResult processQuery(Long campaignId, Long keywordId) {
        GdShowConditionsContainer showConditionsContainer = getDefaultGdShowConditionsContainer()
                .withFilter(new GdShowConditionFilter()
                        .withShowConditionIdIn(singleton(keywordId))
                        .withCampaignIdIn(singleton(campaignId)))
                .withOrderBy(Collections.singletonList(ShowConditionCommonUtils.ORDER_BY_ID));

        String graphQlSerialize = graphQlSerialize(showConditionsContainer);
        String query = String.format(QUERY_TEMPLATE, context.getOperator().getLogin(), graphQlSerialize);
        return processor.processQuery(null, query, null, context);
    }

}
