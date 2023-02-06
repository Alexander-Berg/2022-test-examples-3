package ru.yandex.direct.grid.processing.service.campaign;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableMap;
import graphql.ExecutionResult;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.naming.TestCaseName;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.feature.FeatureName;
import ru.yandex.direct.grid.model.campaign.GdCampaignPrimaryStatus;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.context.container.GridGraphQLContext;
import ru.yandex.direct.grid.processing.model.campaign.GdCampaignFilterStatus;
import ru.yandex.direct.grid.processing.model.campaign.GdCampaignsContainer;
import ru.yandex.direct.grid.processing.processor.GridGraphQLProcessor;
import ru.yandex.direct.grid.processing.service.dataloader.GridContextProvider;
import ru.yandex.direct.grid.processing.util.CampaignTestDataUtils;
import ru.yandex.direct.grid.processing.util.ContextHelper;
import ru.yandex.direct.grid.processing.util.GraphQLUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.core.testing.data.TestCampaigns.newTextCampaign;
import static ru.yandex.direct.core.testing.data.TestUsers.generateNewUser;
import static ru.yandex.direct.dbschema.ppc.tables.AggrStatusesCampaigns.AGGR_STATUSES_CAMPAIGNS;
import static ru.yandex.direct.feature.FeatureName.HIDE_OLD_SHOW_CAMPS_FOR_DNA;
import static ru.yandex.direct.feature.FeatureName.SHOW_AGGREGATED_STATUS_OPEN_BETA;
import static ru.yandex.direct.feature.FeatureName.SHOW_DNA_BY_DEFAULT;
import static ru.yandex.direct.grid.model.campaign.GdCampaignPrimaryStatus.ACTIVE;
import static ru.yandex.direct.grid.model.campaign.GdCampaignPrimaryStatus.ARCHIVED;
import static ru.yandex.direct.grid.model.campaign.GdCampaignPrimaryStatus.DRAFT;
import static ru.yandex.direct.grid.model.campaign.GdCampaignPrimaryStatus.MODERATION;
import static ru.yandex.direct.grid.model.campaign.GdCampaignPrimaryStatus.MODERATION_DENIED;
import static ru.yandex.direct.grid.model.campaign.GdCampaignPrimaryStatus.STOPPED;
import static ru.yandex.direct.grid.processing.configuration.GridProcessingConfiguration.GRAPH_QL_PROCESSOR;
import static ru.yandex.direct.grid.processing.util.GraphQlJsonUtils.graphQlSerialize;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;

/**
 * Тест на сервис, проверяем что фильтры по аггр. статусам работают.
 */
@GridProcessingTest
@RunWith(JUnitParamsRunner.class)
public class CampaignsGraphQlServiceByAggrStatusTest {

    private static final String QUERY_TEMPLATE = ""
            + "{\n"
            + "  client(searchBy: {login: \"%s\"}) {\n"
            + "    campaigns(input: %s) {\n"
            + "      rowset {\n"
            + "        id\n"
            + "      }\n"
            + "    }\n"
            + "  }\n"
            + "}\n";
    private static final String AGGR_DATA_ACTIVE = ""
            + "{\"r\": [\"CAMPAIGN_ACTIVE\"], "
            + "\"s\": \"RUN_OK\", "
            + "\"sts\": [\"PAYED\"], "
            + "\"cnts\": {\"s\": {\"RUN_OK\": 1}, "
            + "\"grps\": 1}}";
    private static final String AGGR_DATA_RUN_WARN = ""
            + "{\"r\": [\"CAMPAIGN_ACTIVE\"], "
            + "\"s\": \"RUN_WARN\", "
            + "\"sts\": [\"PAYED\"], "
            + "\"cnts\": {\"s\": {\"RUN_WARN\": 1}, "
            + "\"grps\": 1}}";
    private static final String AGGR_DATA_DRAFT = ""
            + "{\"r\": [\"DRAFT\"], "
            + "\"s\": \"DRAFT\", "
            + "\"sts\": [\"DRAFT\"], "
            + "\"cnts\": {\"grps\": 0}}";
    private static final String AGGR_DATA_ARCHIVED = ""
            + "{\"r\": [\"ARCHIVED\"], "
            + "\"s\": \"ARCHIVED\", "
            + "\"sts\": [\"ARCHIVED\"], "
            + "\"cnts\": {\"s\": {\"STOP_CRIT\": 1}, "
            + "\"grps\": 1}}";
    private static final String AGGR_DATA_MODERATION = ""
            + "{\"r\": [\"CAMPAIGN_ON_MODERATION\"], "
            + "\"s\": \"DRAFT\", "
            + "\"sts\": [\"NO_MONEY\"], "
            + "\"cnts\": {\"s\": {\"DRAFT\": 1}, \"sts\": {\"MODERATION\": 1, \"HAS_DRAFT_ON_MODERATION_ADS\": 1}, "
            + "\"grps\": 1}}";
    private static final String AGGR_DATA_MODERATION_DENIED = ""
            + "{\"r\": [\"CAMPAIGN_HAS_NO_ADS_ELIGIBLE_FOR_SERVING\"], "
            + "\"s\": \"STOP_CRIT\", "
            + "\"sts\": [\"PAYED\"], "
            + "\"cnts\": {\"s\": {\"ARCHIVED\": 2}, \"sts\": {\"REJECTED\": 1}, \"grps\": 2}}";
    private static final String AGGR_DATA_STOPPED = ""
            + "{\"r\": [\"SUSPENDED_BY_USER\"], "
            + "\"s\": \"STOP_OK\", "
            + "\"sts\": [\"SUSPENDED\"], "
            + "\"cnts\": {\"s\": {\"RUN_WARN\": 2}, "
            + "\"sts\": {\"BS_RARELY_SERVED\": 2}, "
            + "\"grps\": 2}}";
    private static final String AGGR_DATA_TEMPORARY_PAUSED = ""
            + "{\"r\": [\"CAMPAIGN_IS_PAUSED_BY_TIMETARGETING\"], "
            + "\"s\": \"PAUSE_OK\", "
            + "\"sts\": [\"PAYED\"], "
            + "\"cnts\": {\"grps\": 2}}";

    @ClassRule
    public static final SpringClassRule springClassRule = new SpringClassRule();
    @Rule
    public final SpringMethodRule springMethodRule = new SpringMethodRule();

    @Autowired
    @Qualifier(GRAPH_QL_PROCESSOR)
    private GridGraphQLProcessor processor;
    @Autowired
    private GridContextProvider gridContextProvider;
    @Autowired
    private Steps steps;
    @Autowired
    private DslContextProvider dslContextProvider;

    private GdCampaignsContainer campaignsContainer;
    private GridGraphQLContext context;
    private UserInfo userInfo;
    private ClientId clientId;
    private ClientInfo clientInfo;
    private Long uid;

    @Before
    public void initTestData() {
        userInfo = steps.userSteps().createUser(generateNewUser());
        clientInfo = userInfo.getClientInfo();
        clientId = userInfo.getClientId();
        uid = userInfo.getUid();

        context = ContextHelper.buildContext(userInfo.getUser())
                .withFetchedFieldsReslover(null);
        gridContextProvider.setGridContext(context);
    }

    public static Object[] parametersForGetByStatus() {
        return new Object[][]{
                {GdCampaignPrimaryStatus.ACTIVE, AGGR_DATA_ACTIVE, null},
                {GdCampaignPrimaryStatus.DRAFT, AGGR_DATA_DRAFT, null},
                {GdCampaignPrimaryStatus.ARCHIVED, AGGR_DATA_ARCHIVED, true},
                {GdCampaignPrimaryStatus.MODERATION, AGGR_DATA_MODERATION, null},
                {GdCampaignPrimaryStatus.MODERATION_DENIED, AGGR_DATA_MODERATION_DENIED, null},
                {GdCampaignPrimaryStatus.STOPPED, AGGR_DATA_STOPPED, null},
        };
    }

    @Test
    @Parameters(method = "parametersForGetByStatus")
    @TestCaseName("get by status: {0}, aggr_data: {1}, with archived: {2}")
    public void testPrimaryStatusFilter(GdCampaignPrimaryStatus status,
                                 String aggrData,
                                 Boolean withArhived) {
        steps.featureSteps().addClientFeature(userInfo.getClientId(), SHOW_DNA_BY_DEFAULT, true);

        var campaignInfo = createCampaign(aggrData, status);
        createCampaignsWithDifferentStatusesExcept(status);

        campaignsContainer = CampaignTestDataUtils.getDefaultCampaignsContainerInput();
        campaignsContainer.getFilter()
                .withArchived(withArhived);
        if (status != ARCHIVED) {
            campaignsContainer.getFilter()
                    .withStatusIn(Set.of(status));
        }

        String query = String.format(QUERY_TEMPLATE, context.getOperator().getLogin(),
                graphQlSerialize(campaignsContainer));
        ExecutionResult result = processor.processQuery(null, query, null, context);

        GraphQLUtils.logErrors(result.getErrors());

        assertThat(result.getErrors())
                .isEmpty();
        Map<String, Object> data = result.getData();
        Map<String, Object> expected = expectResult(campaignInfo.getCampaignId());
        assertThat(data)
                .is(matchedBy(beanDiffer(expected)));
    }

    public static Object[] parametersForGetByFilterStatus() {
        return new Object[][]{
                {GdCampaignFilterStatus.ACTIVE, AGGR_DATA_ACTIVE},
                {GdCampaignFilterStatus.RUN_WARN, AGGR_DATA_RUN_WARN},
                {GdCampaignFilterStatus.DRAFT, AGGR_DATA_DRAFT},
                {GdCampaignFilterStatus.ARCHIVED, AGGR_DATA_ARCHIVED},
                {GdCampaignFilterStatus.MODERATION, AGGR_DATA_MODERATION},
                {GdCampaignFilterStatus.MODERATION_DENIED, AGGR_DATA_MODERATION_DENIED},
                {GdCampaignFilterStatus.STOPPED, AGGR_DATA_STOPPED},
                {GdCampaignFilterStatus.TEMPORARILY_PAUSED, AGGR_DATA_TEMPORARY_PAUSED},
        };
    }

    @Test
    @Parameters(method = "parametersForGetByFilterStatus")
    @TestCaseName("get by filter status: {0}, aggr_data: {1}")
    public void testStatusFilter(GdCampaignFilterStatus status,
            String aggrData) {
        GdCampaignPrimaryStatus primaryStatus;
        if (status == GdCampaignFilterStatus.RUN_WARN) {
            primaryStatus = GdCampaignPrimaryStatus.ACTIVE;
        } else {
            primaryStatus = GdCampaignPrimaryStatus.valueOf(status.name());
        }
        var campaignInfo = createCampaign(aggrData, primaryStatus);
        createCampaignsWithDifferentStatusesExcept(primaryStatus);

        campaignsContainer = CampaignTestDataUtils.getDefaultCampaignsContainerInput();
        campaignsContainer.getFilter()
            .withFilterStatusIn(Set.of(status));

        String query = String.format(QUERY_TEMPLATE, context.getOperator().getLogin(),
                graphQlSerialize(campaignsContainer));
        ExecutionResult result = processor.processQuery(null, query, null, context);

        GraphQLUtils.logErrors(result.getErrors());

        assertThat(result.getErrors())
                .isEmpty();
        Map<String, Object> data = result.getData();
        Map<String, Object> expected = expectResult(campaignInfo.getCampaignId());
        assertThat(data)
                .is(matchedBy(beanDiffer(expected)));
    }

    public static Object[] parametersForFeature() {
        return new Object[][]{
                {SHOW_DNA_BY_DEFAULT},
                {HIDE_OLD_SHOW_CAMPS_FOR_DNA},
                {SHOW_AGGREGATED_STATUS_OPEN_BETA},
        };
    }

    @Test
    @Parameters(method = "parametersForFeature")
    @TestCaseName("{0}")
    @SuppressWarnings("unchecked")
    public void testStatusFilter_GetWithoutArchive(FeatureName featureName) {
        steps.featureSteps().addClientFeature(userInfo.getClientId(), featureName, true);

        var campaignIds = createCampaignsWithDifferentStatusesExcept(ARCHIVED);
        var archivedCampaignInfo = createCampaign(AGGR_DATA_ARCHIVED, GdCampaignPrimaryStatus.ARCHIVED);

        campaignsContainer = CampaignTestDataUtils.getDefaultCampaignsContainerInput();
        campaignsContainer.getFilter()
                .withArchived(false);

        String query = String.format(QUERY_TEMPLATE, context.getOperator().getLogin(),
                graphQlSerialize(campaignsContainer));
        ExecutionResult result = processor.processQuery(null, query, null, context);

        GraphQLUtils.logErrors(result.getErrors());

        assertThat(result.getErrors())
                .isEmpty();

        Map<String, Object> data = result.getData();
        Map<String, Object> data2 = (Map<String, Object>) data.get("client");
        Map<String, Object> data3 = (Map<String, Object>) data2.get("campaigns");
        List<Object> rowsets = (List<Object>) data3.get("rowset");

        List<Long> resultCampaignIds = new ArrayList<>();
        for (Object rowset : rowsets) {
            Map<String, Object> row = (Map<String, Object>) rowset;
            resultCampaignIds.add(Long.parseLong(row.get("id").toString()));
        }
        assertThat(resultCampaignIds)
                .containsExactlyInAnyOrder(campaignIds.toArray(Long[]::new))
                .doesNotContain(archivedCampaignInfo.getCampaignId());
    }


    private CampaignInfo createCampaign(String aggrData, GdCampaignPrimaryStatus status) {
        var campaignInfo = steps.campaignSteps().createCampaign(newTextCampaign(clientId, uid)
                .withName(status.name()), clientInfo);
        addAggrStatusesCampaign(campaignInfo.getCampaignId(), aggrData);
        return campaignInfo;
    }

    private List<Long> createCampaignsWithDifferentStatusesExcept(GdCampaignPrimaryStatus status) {
        List<Long> camaignIds = new ArrayList<>();
        if (status != ACTIVE) {
            camaignIds.add(createCampaign(AGGR_DATA_ACTIVE, ACTIVE).getCampaignId());
        }
        if (status != DRAFT) {
            camaignIds.add(createCampaign(AGGR_DATA_DRAFT, DRAFT).getCampaignId());
        }
        if (status != ARCHIVED) {
            camaignIds.add(createCampaign(AGGR_DATA_ARCHIVED, ARCHIVED).getCampaignId());
        }
        if (status != MODERATION) {
            camaignIds.add(createCampaign(AGGR_DATA_MODERATION, MODERATION).getCampaignId());
        }
        if (status != MODERATION_DENIED) {
            camaignIds.add(createCampaign(AGGR_DATA_MODERATION_DENIED, MODERATION_DENIED).getCampaignId());
        }
        if (status != STOPPED) {
            camaignIds.add(createCampaign(AGGR_DATA_STOPPED, STOPPED).getCampaignId());
        }
        return camaignIds;
    }

    private Map<String, Object> expectResult(Long campaignId) {
        return ImmutableMap.of(
                "client",
                ImmutableMap.of(
                        "campaigns", ImmutableMap.of(
                                "rowset", Collections.singletonList(ImmutableMap.of(
                                        "id", campaignId
                                ))
                        )
                ));
    }

    private void addAggrStatusesCampaign(Long cid, String aggrData) {
        dslContextProvider.ppc(userInfo.getShard())
                .insertInto(AGGR_STATUSES_CAMPAIGNS, AGGR_STATUSES_CAMPAIGNS.CID, AGGR_STATUSES_CAMPAIGNS.AGGR_DATA,
                        AGGR_STATUSES_CAMPAIGNS.UPDATED, AGGR_STATUSES_CAMPAIGNS.IS_OBSOLETE)
                .values(cid, aggrData, LocalDateTime.of(LocalDate.now(), LocalTime.MIN), 0L)
                .execute();
    }
}
