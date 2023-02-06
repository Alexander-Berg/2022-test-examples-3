package ru.yandex.direct.grid.processing.service.dynamiccondition;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.dynamictextadtarget.model.DynamicFeedAdTarget;
import ru.yandex.direct.core.entity.dynamictextadtarget.model.DynamicTextAdTarget;
import ru.yandex.direct.core.entity.dynamictextadtarget.repository.DynamicTextAdTargetRepository;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.model.api.GdDefect;
import ru.yandex.direct.grid.processing.model.api.GdValidationResult;
import ru.yandex.direct.grid.processing.model.dynamiccondition.mutation.GdDeleteDynamicAdTargets;
import ru.yandex.direct.grid.processing.model.dynamiccondition.mutation.GdDeleteDynamicAdTargetsPayload;
import ru.yandex.direct.grid.processing.util.GraphQlTestExecutor;
import ru.yandex.direct.grid.processing.util.TestAuthHelper;
import ru.yandex.direct.grid.processing.util.UserHelper;
import ru.yandex.direct.multitype.entity.LimitOffset;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;


@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
public class DynamicConditionsGraphQlServiceDeleteTest {

    private static final String MUTATION_NAME = "deleteDynamicAdTargets";
    private static final String QUERY_TEMPLATE = ""
            + "mutation {\n"
            + "  %s (input: %s) {\n"
            + "    deletedDynamicConditionIds\n"
            + "    validationResult{\n"
            + "      errors{\n"
            + "        code,\n"
            + "        params,\n"
            + "        path\n"
            + "      }\n"
            + "      warnings{\n"
            + "        code,\n"
            + "        params,\n"
            + "        path\n"
            + "      }\n"
            + "    }"
            + "  }\n"
            + "}";

    private static final GraphQlTestExecutor.TemplateMutation<GdDeleteDynamicAdTargets,
            GdDeleteDynamicAdTargetsPayload> MUTATION =
            new GraphQlTestExecutor.TemplateMutation<>(MUTATION_NAME, QUERY_TEMPLATE,
                    GdDeleteDynamicAdTargets.class, GdDeleteDynamicAdTargetsPayload.class);

    @Autowired
    private GraphQlTestExecutor graphQlTestExecutor;
    @Autowired
    private Steps steps;
    @Autowired
    private DynamicTextAdTargetRepository dynamicTextAdTargetRepository;

    private User operator;
    private int shard;
    private ClientId clientId;
    private Long dynamicTextConditionId;
    private Long dynamicFeedConditionId;
    private Long anotherClientDynamicConditionId;

    @Before
    public void before() {
        ClientInfo clientInfo = steps.clientSteps().createDefaultClient();
        shard = clientInfo.getShard();
        clientId = clientInfo.getClientId();

        operator = UserHelper.getUser(clientInfo.getClient());
        TestAuthHelper.setDirectAuthentication(operator);

        AdGroupInfo dynamicTextAdGroup = steps.adGroupSteps().createActiveDynamicTextAdGroup(clientInfo);
        AdGroupInfo dynamicFeedAdGroup = steps.adGroupSteps().createActiveDynamicFeedAdGroup(clientInfo);
        AdGroupInfo anotherClientAdGroup = steps.adGroupSteps().createActiveDynamicTextAdGroup();

        dynamicTextConditionId = steps.dynamicTextAdTargetsSteps()
                .createDefaultDynamicTextAdTarget(dynamicTextAdGroup)
                .getDynamicConditionId();

        dynamicFeedConditionId = steps.dynamicTextAdTargetsSteps()
                .createDefaultDynamicFeedAdTarget(dynamicFeedAdGroup)
                .getDynamicConditionId();

        anotherClientDynamicConditionId = steps.dynamicTextAdTargetsSteps()
                .createDefaultDynamicTextAdTarget(anotherClientAdGroup)
                .getDynamicConditionId();
    }

    @Test
    public void deleteDynamicTextAdTargets_success() {
        GdDeleteDynamicAdTargetsPayload payload = deleteDynamicAdTarget(dynamicTextConditionId);

        GdDeleteDynamicAdTargetsPayload expectedPayload = new GdDeleteDynamicAdTargetsPayload()
                .withDeletedDynamicConditionIds(singletonList(dynamicTextConditionId))
                .withValidationResult(null);

        assertThat(payload).is(matchedBy(beanDiffer(expectedPayload)));

        List<DynamicTextAdTarget> dynamicTextAdTargets = dynamicTextAdTargetRepository
                .getDynamicTextAdTargetsWithDomainType(shard, clientId,
                        singletonList(dynamicTextConditionId), false, LimitOffset.maxLimited());

        assertThat(dynamicTextAdTargets).isEmpty();
    }

    @Test
    public void deleteDynamicFeedAdTargets_success() {
        GdDeleteDynamicAdTargetsPayload payload = deleteDynamicAdTarget(dynamicFeedConditionId);

        GdDeleteDynamicAdTargetsPayload expectedPayload = new GdDeleteDynamicAdTargetsPayload()
                .withDeletedDynamicConditionIds(singletonList(dynamicFeedConditionId))
                .withValidationResult(null);

        assertThat(payload).is(matchedBy(beanDiffer(expectedPayload)));

        List<DynamicFeedAdTarget> dynamicFeedAdTargets = dynamicTextAdTargetRepository
                .getDynamicFeedAdTargets(shard, clientId, singletonList(dynamicFeedConditionId));

        assertThat(dynamicFeedAdTargets).hasSize(1);
        assertThat(dynamicFeedAdTargets.get(0).getId()).isNull();
    }

    @Test
    public void deleteDynamicAdTargets_validationError_whenAnotherClientDynamicConditionId() {
        GdDeleteDynamicAdTargetsPayload payload = deleteDynamicAdTarget(anotherClientDynamicConditionId);

        GdDeleteDynamicAdTargetsPayload expectedPayload = new GdDeleteDynamicAdTargetsPayload()
                .withDeletedDynamicConditionIds(emptyList())
                .withValidationResult(new GdValidationResult()
                        .withErrors(singletonList(new GdDefect()
                                .withCode("DefectIds.OBJECT_NOT_FOUND")
                                .withPath("dynamicConditionIds[0]")))
                        .withWarnings(emptyList()));

        assertThat(payload).is(matchedBy(beanDiffer(expectedPayload)));
    }

    private GdDeleteDynamicAdTargetsPayload deleteDynamicAdTarget(Long dynamicConditionId) {
        GdDeleteDynamicAdTargets input = new GdDeleteDynamicAdTargets()
                .withDynamicConditionIds(singletonList(dynamicConditionId));
        return graphQlTestExecutor.doMutationAndGetPayload(MUTATION, input, operator);
    }
}
