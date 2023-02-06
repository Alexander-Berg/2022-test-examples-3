package ru.yandex.direct.grid.processing.service.group;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import graphql.ExecutionResult;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.StatusBsSynced;
import ru.yandex.direct.core.entity.relevancematch.model.RelevanceMatch;
import ru.yandex.direct.core.entity.relevancematch.repository.RelevanceMatchRepository;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.model.api.GdDefect;
import ru.yandex.direct.grid.processing.model.api.GdValidationResult;
import ru.yandex.direct.grid.processing.model.group.mutation.GdChangeAdGroupsRelevanceMatch;
import ru.yandex.direct.grid.processing.model.group.mutation.GdChangeAdGroupsRelevanceMatchPayload;
import ru.yandex.direct.grid.processing.processor.GridGraphQLProcessor;
import ru.yandex.direct.grid.processing.util.GraphQlJsonUtils;
import ru.yandex.direct.grid.processing.util.TestAuthHelper;
import ru.yandex.direct.grid.processing.util.UserHelper;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.util.BigDecimalComparator.BIG_DECIMAL_COMPARATOR;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.grid.processing.configuration.GridProcessingConfiguration.GRAPH_QL_PROCESSOR;
import static ru.yandex.direct.grid.processing.util.ContextHelper.buildContext;
import static ru.yandex.direct.grid.processing.util.GraphQlJsonUtils.graphQlSerialize;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
public class AdGroupGraphQlServiceChangeRelevanceMatchTest {
    private static final String MUTATION_NAME = "changeAdGroupsRelevanceMatch";
    private static final String MUTATION_TEMPLATE = ""
            + "mutation {\n"
            + "  %s(input: %s) {\n"
            + "    totalCount \n"
            + "    successCount \n"
            + "    updatedAdGroupIds \n"
            + "    validationResult {\n"
            + "      errors {\n"
            + "        code\n"
            + "        path\n"
            + "        params\n"
            + "      }\n"
            + "      warnings {\n"
            + "        code\n"
            + "        path\n"
            + "        params\n"
            + "      }\n"
            + "    }\n"
            + "  }\n"
            + "}";

    private Long adGroupId;
    private User operator;
    private GdChangeAdGroupsRelevanceMatch request;

    @Autowired
    @Qualifier(GRAPH_QL_PROCESSOR)
    private GridGraphQLProcessor processor;

    @Autowired
    protected RelevanceMatchRepository relevanceMatchRepository;

    @Autowired
    private Steps steps;

    private Long campaignId;
    private ClientInfo clientInfo;
    private AdGroupInfo adGroupInfo;
    private AdGroupInfo activeDynamicTextAdGroup;

    @Before
    public void initTestData() {
        clientInfo = steps.clientSteps().createDefaultClient();
        adGroupInfo = steps.adGroupSteps().createDefaultAdGroup(clientInfo);
        adGroupId = adGroupInfo.getAdGroupId();
        campaignId = adGroupInfo.getCampaignId();

        activeDynamicTextAdGroup = steps.adGroupSteps().createActiveDynamicTextAdGroup(clientInfo);

        operator = UserHelper.getUser(adGroupInfo);
        TestAuthHelper.setDirectAuthentication(operator);

        request = new GdChangeAdGroupsRelevanceMatch()
                .withAdGroupIds(List.of(adGroupId))
                .withEnableRelevanceMatch(true);
    }

    @After
    public void afterTest() {
        SecurityContextHolder.clearContext();
    }

    @Test
    public void enableRelevanceMatch() {
        List<Long> adGroupIds = List.of(adGroupId, activeDynamicTextAdGroup.getAdGroupId());
        request.setAdGroupIds(adGroupIds);

        String query = String.format(MUTATION_TEMPLATE, MUTATION_NAME, graphQlSerialize(request));
        ExecutionResult result = processor.processQuery(null, query, null, buildContext(operator));
        assertThat(result.getErrors())
                .isEmpty();

        GdChangeAdGroupsRelevanceMatchPayload expectedPayload = new GdChangeAdGroupsRelevanceMatchPayload()
                .withSuccessCount(1)
                .withTotalCount(2)
                .withUpdatedAdGroupIds(List.of(adGroupId))
                .withValidationResult(new GdValidationResult()
                        .withErrors(List.of(new GdDefect()
                                .withCode("CampaignDefectIds.Gen.CAMPAIGN_TYPE_NOT_SUPPORTED")
                                .withPath("[1]")))
                        .withWarnings(emptyList()));

        Map<String, Object> data = result.getData();
        assertThat(data).containsOnlyKeys(MUTATION_NAME);

        GdChangeAdGroupsRelevanceMatchPayload payload =
                GraphQlJsonUtils.convertValue(data.get(MUTATION_NAME), GdChangeAdGroupsRelevanceMatchPayload.class);
        assertThat(payload).is(matchedBy(beanDiffer(expectedPayload)));

        RelevanceMatch expectedNewRelevanceMatch = new RelevanceMatch()
                .withAdGroupId(adGroupId)
                .withCampaignId(campaignId)
                .withIsDeleted(false)
                .withIsSuspended(false)
                .withStatusBsSynced(StatusBsSynced.NO);

        checkNewRelevanceMatch(adGroupId, expectedNewRelevanceMatch);
    }

    @Test
    public void disableRelevanceMatch() {
        RelevanceMatch relevanceMatch = steps.relevanceMatchSteps().getDefaultRelevanceMatch(adGroupInfo);
        steps.relevanceMatchSteps()
                .addRelevanceMatchToAdGroup(List.of(relevanceMatch), adGroupInfo);

        Map<Long, RelevanceMatch> relevanceMatchMap = relevanceMatchRepository
                .getRelevanceMatchesByAdGroupIds(clientInfo.getShard(), clientInfo.getClientId(),
                        singletonList(adGroupId));
        assertThat(relevanceMatchMap).containsOnlyKeys(adGroupId);

        List<Long> adGroupIds = List.of(adGroupId, activeDynamicTextAdGroup.getAdGroupId());

        request.setAdGroupIds(adGroupIds);
        request.setEnableRelevanceMatch(false);

        String query = String.format(MUTATION_TEMPLATE, MUTATION_NAME, graphQlSerialize(request));
        ExecutionResult result = processor.processQuery(null, query, null, buildContext(operator));
        assertThat(result.getErrors()).isEmpty();

        GdChangeAdGroupsRelevanceMatchPayload expectedPayload = new GdChangeAdGroupsRelevanceMatchPayload()
                .withSuccessCount(2)
                .withTotalCount(2)
                .withUpdatedAdGroupIds(adGroupIds)
                .withValidationResult(new GdValidationResult()
                        .withWarnings(emptyList())
                        .withErrors(emptyList()));

        Map<String, Object> data = result.getData();
        assertThat(data).containsOnlyKeys(MUTATION_NAME);

        GdChangeAdGroupsRelevanceMatchPayload payload =
                GraphQlJsonUtils.convertValue(data.get(MUTATION_NAME), GdChangeAdGroupsRelevanceMatchPayload.class);
        assertThat(payload).is(matchedBy(beanDiffer(expectedPayload)));

        checkRelevanceMatchIsDeleted(adGroupIds);
    }

    private void checkNewRelevanceMatch(Long adGroupId, RelevanceMatch expectedRelevanceMatch) {
        Map<Long, RelevanceMatch> relevanceMatchMap = relevanceMatchRepository
                .getRelevanceMatchesByAdGroupIds(clientInfo.getShard(), clientInfo.getClientId(),
                        singletonList(adGroupId));

        assertThat(relevanceMatchMap)
                .containsOnlyKeys(adGroupId);

        assertThat(relevanceMatchMap.get(adGroupId))
                .usingComparatorForType(BIG_DECIMAL_COMPARATOR, BigDecimal.class)
                .isEqualToIgnoringNullFields(expectedRelevanceMatch);
    }

    private void checkRelevanceMatchIsDeleted(Collection<Long> adGroupId) {
        Map<Long, RelevanceMatch> relevanceMatchMap = relevanceMatchRepository
                .getRelevanceMatchesByAdGroupIds(clientInfo.getShard(), clientInfo.getClientId(), adGroupId);

        assertThat(relevanceMatchMap)
                .isEmpty();
    }
}
