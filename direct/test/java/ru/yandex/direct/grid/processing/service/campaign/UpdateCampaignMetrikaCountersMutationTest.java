package ru.yandex.direct.grid.processing.service.campaign;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.ParametersAreNonnullByDefault;

import com.google.common.collect.Iterables;
import graphql.ExecutionResult;
import graphql.GraphQLError;
import one.util.streamex.IntStreamEx;
import org.apache.commons.collections4.ListUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.campaign.model.MetrikaCounter;
import ru.yandex.direct.core.entity.campaign.repository.CampMetrikaCountersRepository;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.feature.FeatureName;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.model.api.GdDefect;
import ru.yandex.direct.grid.processing.model.api.GdValidationResult;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdUpdateCampaignMetrikaCounters;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdUpdateCampaignMetrikaCountersPayload;
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdUpdateCampaignPayloadItem;
import ru.yandex.direct.grid.processing.model.common.GdMassUpdateAction;
import ru.yandex.direct.grid.processing.util.GraphQLUtils;
import ru.yandex.direct.grid.processing.util.GraphQlTestExecutor;
import ru.yandex.direct.grid.processing.util.TestAuthHelper;
import ru.yandex.direct.grid.processing.util.UserHelper;
import ru.yandex.direct.metrika.client.MetrikaClient;
import ru.yandex.direct.metrika.client.MetrikaClientException;
import ru.yandex.direct.test.utils.RandomNumberUtils;
import ru.yandex.direct.validation.defect.CollectionDefects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.core.entity.campaign.service.CampMetrikaCountersService.MAX_METRIKA_COUNTERS_COUNT;
import static ru.yandex.direct.core.entity.campaign.service.validation.CampaignDefects.cantAddOrDeleteMetrikaCountersToPerformanceCampaign;
import static ru.yandex.direct.core.entity.campaign.service.validation.CampaignDefects.metrikaCounterIsUnavailable;
import static ru.yandex.direct.core.validation.defects.Defects.metrikaReturnsResultWithErrors;
import static ru.yandex.direct.grid.processing.service.campaign.CampaignMutationGraphQlService.UPDATE_CAMPAIGN_METRIKA_COUNTERS;
import static ru.yandex.direct.grid.processing.util.validation.GridValidationHelper.toGdDefect;
import static ru.yandex.direct.grid.processing.util.validation.GridValidationMatchers.gridDefect;
import static ru.yandex.direct.grid.processing.util.validation.GridValidationMatchers.hasErrorsWith;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;
import static ru.yandex.direct.utils.ListUtils.integerToLongList;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
@ParametersAreNonnullByDefault
public class UpdateCampaignMetrikaCountersMutationTest {

    private static final String MUTATION_TEMPLATE = ""
            + "mutation {\n"
            + "  %s (input: %s) {\n"
            + "    validationResult {\n"
            + "      errors {\n"
            + "        code\n"
            + "        path\n"
            + "        params\n"
            + "      }\n"
            + "    }\n"
            + "    updatedCampaigns {\n"
            + "      id\n"
            + "    }\n"
            + "    skippedCampaignIds\n"
            + "  }\n"
            + "}";
    private static final GraphQlTestExecutor.
            TemplateMutation<GdUpdateCampaignMetrikaCounters, GdUpdateCampaignMetrikaCountersPayload>
            UPDATE_CAMPAIGN_METRIKA_COUNTERS_MUTATION = new GraphQlTestExecutor.TemplateMutation<>(
            UPDATE_CAMPAIGN_METRIKA_COUNTERS, MUTATION_TEMPLATE,
            GdUpdateCampaignMetrikaCounters.class, GdUpdateCampaignMetrikaCountersPayload.class);

    @Autowired
    private GraphQlTestExecutor processor;
    @Autowired
    private Steps steps;

    @Autowired
    private MetrikaClient metrikaClient;
    @Autowired
    private CampMetrikaCountersRepository campMetrikaCountersRepository;

    private User operator;
    private ClientInfo clientInfo;
    private int shard;
    private GdUpdateCampaignMetrikaCounters input;

    @Before
    public void before() {
        clientInfo = steps.clientSteps().createDefaultClient();
        shard = clientInfo.getShard();
        steps.featureSteps().addClientFeature(clientInfo.getClientId(),
                FeatureName.MASS_EDIT_METRIKA_COUNTERS_IN_JAVA_FOR_DNA, true);

        operator = UserHelper.getUser(clientInfo.getClient());
        TestAuthHelper.setDirectAuthentication(operator);

        input = new GdUpdateCampaignMetrikaCounters()
                .withAction(GdMassUpdateAction.ADD)
                .withCampaignIds(List.of(RandomNumberUtils.nextPositiveLong()))
                .withMetrikaCounters(List.of(RandomNumberUtils.nextPositiveInteger()));
    }


    @Test
    public void updateCampaignMetrikaCounters_WhenClientWithoutFeature() {
        steps.featureSteps().addClientFeature(clientInfo.getClientId(),
                FeatureName.MASS_EDIT_METRIKA_COUNTERS_IN_JAVA_FOR_DNA, false);

        List<GraphQLError> errors = processor.doMutation(UPDATE_CAMPAIGN_METRIKA_COUNTERS_MUTATION, input, operator)
                .getErrors();

        assertThat(errors)
                .hasSize(1)
                .extracting(GraphQLError::getMessage)
                .allMatch(errorMessage ->
                        errorMessage.endsWith("client does not have a feature to update campaign metrika counters"));
    }

    @Test
    public void updateCampaignMetrikaCounters_CheckRequestValidation_WhenMetrikaCountersIsEmpty() {
        input.setMetrikaCounters(Collections.emptyList());

        ExecutionResult result = processor.doMutation(UPDATE_CAMPAIGN_METRIKA_COUNTERS_MUTATION, input, operator);

        List<GdValidationResult> gdValidationResults = GraphQLUtils.getGdValidationResults(result.getErrors());
        assertThat(gdValidationResults).hasSize(1);

        assertThat(gdValidationResults.get(0)).is(matchedBy(hasErrorsWith(gridDefect(
                path(field(GdUpdateCampaignMetrikaCounters.METRIKA_COUNTERS)),
                CollectionDefects.notEmptyCollection()))));
    }

    @Test
    public void updateCampaignMetrikaCounters_CheckValidateMetrikaCountersAccess() {
        steps.featureSteps().addClientFeature(clientInfo.getClientId(),
                FeatureName.METRIKA_COUNTERS_ACCESS_VALIDATION_ON_SAVE_CAMPAIGN_ENABLED, true);

        GdUpdateCampaignMetrikaCountersPayload payload =
                processor.doMutationAndGetPayload(UPDATE_CAMPAIGN_METRIKA_COUNTERS_MUTATION, input, operator);

        assertThat(payload.getValidationResult())
                .isNotNull()
                .is(matchedBy(hasErrorsWith(gridDefect(
                        path(field(GdUpdateCampaignMetrikaCounters.METRIKA_COUNTERS), index(0)),
                        metrikaCounterIsUnavailable()))));
        assertThat(payload.getUpdatedCampaigns()).isEmpty();
    }

    @Test
    public void updateCampaignMetrikaCounters_SkipCheckValidateMetrikaCountersAccess_WhenActionIsRemove() {
        steps.featureSteps().addClientFeature(clientInfo.getClientId(),
                FeatureName.METRIKA_COUNTERS_ACCESS_VALIDATION_ON_SAVE_CAMPAIGN_ENABLED, true);

        CampaignInfo textCampaign = steps.campaignSteps().createActiveTextCampaign(clientInfo);
        List<Long> oldCounters = List.of(1L, 2L);
        var metrikaCounters = mapList(oldCounters, counter -> new MetrikaCounter().withId(counter));
        campMetrikaCountersRepository.updateMetrikaCounters(shard,
                Map.of(textCampaign.getCampaignId(), metrikaCounters));

        input.withAction(GdMassUpdateAction.REMOVE)
                .withCampaignIds(List.of(textCampaign.getCampaignId()))
                .withMetrikaCounters(List.of(oldCounters.get(0).intValue()));

        List<Long> expectedCounters = ListUtils.subtract(oldCounters, integerToLongList(input.getMetrikaCounters()));
        checkPayloadAndSavedMetrikaCountersInDb(expectedCounters, textCampaign.getCampaignId());
    }

    @Test
    public void updateCampaignMetrikaCounters_WhenMetrikaReturnsResultWithErrors() {
        steps.featureSteps().addClientFeature(clientInfo.getClientId(),
                FeatureName.METRIKA_COUNTERS_ACCESS_VALIDATION_ON_SAVE_CAMPAIGN_ENABLED, true);
        doThrow(new MetrikaClientException())
                .when(metrikaClient).getUsersCountersNum2(eq(List.of(clientInfo.getUid())), any());

        GdUpdateCampaignMetrikaCountersPayload payload =
                processor.doMutationAndGetPayload(UPDATE_CAMPAIGN_METRIKA_COUNTERS_MUTATION, input, operator);

        assertThat(payload.getValidationResult())
                .isNotNull()
                .is(matchedBy(hasErrorsWith(gridDefect(
                        path(field(GdUpdateCampaignMetrikaCounters.METRIKA_COUNTERS)),
                        metrikaReturnsResultWithErrors()))));
        assertThat(payload.getUpdatedCampaigns()).isEmpty();
    }

    @Test
    public void updateCampaignMetrikaCounters_TwoCampaigns_OneWithValidationError() {
        CampaignInfo textCampaign = steps.campaignSteps().createActiveTextCampaign(clientInfo);
        CampaignInfo performanceCampaign = steps.campaignSteps().createActivePerformanceCampaign(clientInfo);

        // для текстовой кампании можно добавлять два счетчика, а для смарт кампании нет
        input.withMetrikaCounters(List.of(1, 2))
                .withCampaignIds(List.of(textCampaign.getCampaignId(), performanceCampaign.getCampaignId()));

        GdUpdateCampaignMetrikaCountersPayload payload =
                processor.doMutationAndGetPayload(UPDATE_CAMPAIGN_METRIKA_COUNTERS_MUTATION, input, operator);

        assertThat(payload.getValidationResult())
                .isNotNull()
                .is(matchedBy(hasErrorsWith(gridDefect(
                        path(field(GdUpdateCampaignMetrikaCounters.CAMPAIGN_IDS), index(1)),
                        cantAddOrDeleteMetrikaCountersToPerformanceCampaign()))));
        assertThat(payload.getUpdatedCampaigns())
                .extracting(GdUpdateCampaignPayloadItem::getId)
                .containsExactly(textCampaign.getCampaignId());
        assertThat(payload.getSkippedCampaignIds())
                .containsExactly(performanceCampaign.getCampaignId());
    }

    @Test
    public void updateCampaignMetrikaCounters_WhenBrokenResult() {
        // присылаем кол-во счетчиков больше лимита, ожидаем получать ошибку валидации верхнего уровня
        List<Integer> counters = IntStreamEx.rangeClosed(1, MAX_METRIKA_COUNTERS_COUNT + 1)
                .boxed()
                .toList();
        input.withMetrikaCounters(counters);

        GdUpdateCampaignMetrikaCountersPayload payload =
                processor.doMutationAndGetPayload(UPDATE_CAMPAIGN_METRIKA_COUNTERS_MUTATION, input, operator);

        GdDefect expectedGdDefect = toGdDefect(
                path(field(GdUpdateCampaignMetrikaCounters.METRIKA_COUNTERS)),
                CollectionDefects.maxCollectionSize(MAX_METRIKA_COUNTERS_COUNT), true);
        assertThat(payload.getValidationResult())
                .isNotNull();
        assertThat(payload.getValidationResult().getErrors())
                .containsExactly(expectedGdDefect);

        assertThat(payload.getUpdatedCampaigns())
                .isEmpty();
        assertThat(payload.getSkippedCampaignIds())
                .isEqualTo(Set.copyOf(input.getCampaignIds()));
    }

    @Test
    public void updateCampaignMetrikaCounters_AddAction() {
        CampaignInfo textCampaign = steps.campaignSteps().createActiveTextCampaign(clientInfo);

        input.withAction(GdMassUpdateAction.ADD)
                .withCampaignIds(List.of(textCampaign.getCampaignId()));

        List<Long> expectedCounters = integerToLongList(input.getMetrikaCounters());
        checkPayloadAndSavedMetrikaCountersInDb(expectedCounters, textCampaign.getCampaignId());
    }

    @Test
    public void updateCampaignMetrikaCounters_RemoveAction() {
        CampaignInfo textCampaign = steps.campaignSteps().createActiveTextCampaign(clientInfo);
        List<Long> oldCounters = List.of(1L, 2L);
        var metrikaCounters = mapList(oldCounters, counter -> new MetrikaCounter().withId(counter));
        campMetrikaCountersRepository.updateMetrikaCounters(shard,
                Map.of(textCampaign.getCampaignId(), metrikaCounters));

        input.withAction(GdMassUpdateAction.REMOVE)
                .withCampaignIds(List.of(textCampaign.getCampaignId()))
                .withMetrikaCounters(List.of(Iterables.getLast(oldCounters).intValue()));

        List<Long> expectedCounters = ListUtils.subtract(oldCounters, integerToLongList(input.getMetrikaCounters()));
        checkPayloadAndSavedMetrikaCountersInDb(expectedCounters, textCampaign.getCampaignId());
    }

    @Test
    public void updateCampaignMetrikaCounters_ReplaceAction() {
        CampaignInfo textCampaign = steps.campaignSteps().createActiveTextCampaign(clientInfo);
        List<Long> oldCounters = List.of(11L, 22L);
        var metrikaCounters = mapList(oldCounters, counter -> new MetrikaCounter().withId(counter));
        campMetrikaCountersRepository.updateMetrikaCounters(shard,
                Map.of(textCampaign.getCampaignId(), metrikaCounters));

        input.withAction(GdMassUpdateAction.REPLACE)
                .withCampaignIds(List.of(textCampaign.getCampaignId()));

        List<Long> expectedCounters = integerToLongList(input.getMetrikaCounters());
        checkPayloadAndSavedMetrikaCountersInDb(expectedCounters, textCampaign.getCampaignId());
    }

    @Test
    public void addCampaignMetrikaCounters_ForCpmCampaign_WithFeature() {
        steps.featureSteps().addClientFeature(clientInfo.getClientId(),
                FeatureName.IS_CPM_BANNER_CAMPAIGN_DISABLED, true);
        CampaignInfo campaign = steps.campaignSteps().createActiveCpmBannerCampaign(clientInfo);
        input.withAction(GdMassUpdateAction.ADD)
                .withCampaignIds(List.of(campaign.getCampaignId()));

        var result = processor.doMutationAndGetPayload(UPDATE_CAMPAIGN_METRIKA_COUNTERS_MUTATION, input, operator);

        assertThat(result.getValidationResult().getErrors()).hasSize(1);
        GdDefect defect = new GdDefect()
                .withPath("campaignIds[0]")
                .withCode("CampaignDefectIds.Gen.METRIKA_COUNTERS_UNSUPPORTED_CAMP_TYPE");
        assertThat(result.getValidationResult().getErrors().get(0)).isEqualTo(defect);
    }

    private void checkPayloadAndSavedMetrikaCountersInDb(List<Long> expectedCounters, Long campaignId) {
        GdUpdateCampaignMetrikaCountersPayload payload =
                processor.doMutationAndGetPayload(UPDATE_CAMPAIGN_METRIKA_COUNTERS_MUTATION, input, operator);

        var expectedPayload = getSuccessPayload(input.getCampaignIds());
        assertThat(payload)
                .is(matchedBy(beanDiffer(expectedPayload)));

        Map<Long, List<Long>> actualCounters = campMetrikaCountersRepository
                .getMetrikaCountersByCids(shard, input.getCampaignIds());
        assertThat(actualCounters)
                .is(matchedBy(beanDiffer(Map.of(campaignId, expectedCounters))));
    }

    private static GdUpdateCampaignMetrikaCountersPayload getSuccessPayload(List<Long> campaignIds) {
        return new GdUpdateCampaignMetrikaCountersPayload()
                .withUpdatedCampaigns(mapList(campaignIds, id -> new GdUpdateCampaignPayloadItem().withId(id)))
                .withSkippedCampaignIds(Collections.emptySet());
    }

}
