package ru.yandex.direct.core.entity.campaign.service.type.update;

import java.util.List;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.direct.audience.client.YaAudienceClient;
import ru.yandex.direct.audience.client.model.CreateExperimentRequest;
import ru.yandex.direct.audience.client.model.CreateExperimentResponse;
import ru.yandex.direct.audience.client.model.CreateExperimentResponseEnvelope;
import ru.yandex.direct.audience.client.model.ExperimentSegmentRequest;
import ru.yandex.direct.audience.client.model.ExperimentSegmentResponse;
import ru.yandex.direct.audience.client.model.SetExperimentGrantRequest;
import ru.yandex.direct.audience.client.model.SetExperimentGrantResponse;
import ru.yandex.direct.audience.client.model.SetExperimentGrantResponseEnvelope;
import ru.yandex.direct.core.entity.brandlift.repository.BrandSurveyRepository;
import ru.yandex.direct.core.entity.campaign.model.CampaignType;
import ru.yandex.direct.core.entity.campaign.model.CampaignWithBrandLift;
import ru.yandex.direct.core.entity.campaign.model.CpmBannerCampaign;
import ru.yandex.direct.core.entity.campaign.service.CampaignOptions;
import ru.yandex.direct.core.entity.campaign.service.CampaignWithBrandLiftExperimentsService;
import ru.yandex.direct.core.entity.campaign.service.RequestBasedMetrikaClientAdapter;
import ru.yandex.direct.core.entity.campaign.service.type.update.container.RestrictedCampaignsUpdateOperationContainer;
import ru.yandex.direct.core.entity.campaign.service.type.update.container.RestrictedCampaignsUpdateOperationContainerImpl;
import ru.yandex.direct.core.entity.client.service.ClientService;
import ru.yandex.direct.core.entity.feature.service.FeatureService;
import ru.yandex.direct.core.entity.retargeting.model.ExperimentRetargetingConditions;
import ru.yandex.direct.core.entity.retargeting.service.RetargetingConditionService;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.feature.FeatureName;
import ru.yandex.direct.model.AppliedChanges;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.test.utils.RandomNumberUtils;

import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;
import static ru.yandex.direct.core.entity.campaign.service.CampaignWithBrandLiftExperimentsService.BRANDLIFT_BK_SETTINGS;
import static ru.yandex.direct.core.entity.campaign.service.CampaignWithBrandLiftExperimentsService.DEFAULT_METRIKA_COUNTER_FOR_AB;
import static ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants.BRAND_LIFT_SURVEYS_LOGIN;

@RunWith(MockitoJUnitRunner.class)
public class CampaignWithBrandLiftUpdateOperationSupportTest {

    @Mock
    private YaAudienceClient yaAudienceClient;

    @Mock
    private FeatureService featureService;

    @Mock
    private ShardHelper shardHelper;

    @Mock
    private ClientService clientService;

    @Mock
    private RetargetingConditionService retargetingConditionService;

    @Mock
    private BrandSurveyRepository brandSurveyRepository;

    @InjectMocks
    private CampaignWithBrandLiftExperimentsService campaignWithBrandLiftExperimentsService;

    @Mock
    private RequestBasedMetrikaClientAdapter metrikaClientAdapter;

    private CampaignWithBrandLiftUpdateOperationSupport operationSupport;

    private static ClientId clientId;
    private static Long uid;
    private static Long campaignId;
    private static CampaignWithBrandLift campaign;
    private static RestrictedCampaignsUpdateOperationContainer updateParameters;

    private static final long METRIKA_COUNTER_ID = RandomNumberUtils.nextPositiveLong();
    private static final long EXPERIMENT_ID = RandomNumberUtils.nextPositiveLong();
    private static final long RET_COND_ID = RandomNumberUtils.nextPositiveLong();
    private static final String CLIENT_LOGIN = "yndx-client";

    @Before
    public void before() {
        var shard = 1;
        operationSupport = new CampaignWithBrandLiftUpdateOperationSupport(retargetingConditionService,
                shardHelper, featureService, campaignWithBrandLiftExperimentsService);

        clientId = ClientId.fromLong(RandomNumberUtils.nextPositiveLong());
        uid = RandomNumberUtils.nextPositiveLong();
        campaignId = RandomNumberUtils.nextPositiveLong();
        campaign = createCampaign();

        updateParameters = new RestrictedCampaignsUpdateOperationContainerImpl(
                shard,
                uid,
                clientId,
                uid,
                uid,
                metrikaClientAdapter,
                new CampaignOptions(),
                null,
                emptyMap()
        );

        doReturn(List.of(new ExperimentRetargetingConditions())).when(retargetingConditionService)
                .findOrCreateExperimentsRetargetingConditions(any(), any(), any(), any());

        doReturn(CLIENT_LOGIN).when(shardHelper).getLoginByUid(anyLong());
    }

    @Test
    public void beforeExecution_callAudience_whenBrandLiftChanged() {
        doReturn(createExperimentResponse(EXPERIMENT_ID, 2L, 3L))
                .when(yaAudienceClient).createExperiment(any(), any());

        doReturn(setExperimentGrantResponse())
                .when(yaAudienceClient).setExperimentGrant(any(), any());

        doReturn(Optional.empty())
                .when(clientService).getExperiment(anyInt(), any());

        campaign.withMetrikaCounters(List.of(METRIKA_COUNTER_ID));

        ModelChanges<CampaignWithBrandLift> modelChanges =
                ModelChanges.build(campaign, CpmBannerCampaign.BRAND_SURVEY_ID, "brand_survey_id");

        AppliedChanges<CampaignWithBrandLift> appliedChanges = modelChanges.applyTo(campaign);
        operationSupport.beforeExecution(updateParameters, List.of(appliedChanges));

        verify(yaAudienceClient, times(1)).createExperiment(eq(CLIENT_LOGIN),
                eq(createExperimentRequest()));

        verify(yaAudienceClient, times(1)).setExperimentGrant(eq(EXPERIMENT_ID),
                eq(setExperimentGrantRequest()));
    }

    @Test
    public void beforeExecution_dontCreateBrandLiftWhenExist() {
        doReturn(createExperimentResponse(EXPERIMENT_ID, 2L, 3L))
                .when(yaAudienceClient).createExperiment(any(), any());

        doReturn(setExperimentGrantResponse())
                .when(yaAudienceClient).setExperimentGrant(any(), any());

        doReturn(Optional.empty())
                .when(clientService).getExperiment(anyInt(), any());

        doReturn(List.of(new ExperimentRetargetingConditions()
                .withRetargetingConditionId(1234L)
                .withStatisticRetargetingConditionId(9876L)))
                .when(retargetingConditionService)
                .findOrCreateExperimentsRetargetingConditions(any(), any(), any(), any());
        campaign.withMetrikaCounters(List.of(METRIKA_COUNTER_ID));

        var brandSurveyId = "brand_survey_id";

        ModelChanges<CampaignWithBrandLift> modelChanges =
                ModelChanges.build(campaign, CpmBannerCampaign.BRAND_SURVEY_ID, brandSurveyId);

        AppliedChanges<CampaignWithBrandLift> appliedChanges = modelChanges.applyTo(campaign);

        operationSupport.beforeExecution(updateParameters, List.of(appliedChanges));

        var retargetingConditionsId = appliedChanges.getModel().getAbSegmentRetargetingConditionId();

        campaign = createCampaign();

        modelChanges = ModelChanges.build(campaign, CpmBannerCampaign.BRAND_SURVEY_ID, brandSurveyId);

        appliedChanges = modelChanges.applyTo(campaign);

        operationSupport.beforeExecution(updateParameters, List.of(appliedChanges));

        assertThat(appliedChanges.getModel().getAbSegmentRetargetingConditionId()).isEqualTo(retargetingConditionsId);
        assertThat(appliedChanges.getModel().getBrandSurveyId()).isEqualTo(brandSurveyId);
    }

    @Test
    public void beforeExecution_callAudience_whenBrandLiftDontChanged() {
        campaign
                .withMetrikaCounters(List.of(METRIKA_COUNTER_ID))
                .withBrandSurveyId("brand_survey_id");

        ModelChanges<CampaignWithBrandLift> modelChanges =
                ModelChanges.build(campaign, CpmBannerCampaign.BRAND_SURVEY_ID, "brand_survey_id");

        AppliedChanges<CampaignWithBrandLift> appliedChanges = modelChanges.applyTo(campaign);
        operationSupport.beforeExecution(updateParameters, List.of(appliedChanges));

        verifyZeroInteractions(yaAudienceClient);
    }

    @Test
    public void onChangesAppliedTest_notChanged() {
        campaign
                .withMetrikaCounters(List.of(METRIKA_COUNTER_ID))
                .withBrandSurveyId("brand_survey_id")
                .withIsBrandLiftHidden(false);

        ModelChanges<CampaignWithBrandLift> modelChanges =
                new ModelChanges<>(campaign.getId(), CampaignWithBrandLift.class);

        AppliedChanges<CampaignWithBrandLift> appliedChanges = modelChanges.applyTo(campaign);

        operationSupport.onChangesApplied(updateParameters, List.of(appliedChanges));

        assertThat(appliedChanges.getModel().getIsBrandLiftHidden()).isFalse();
        assertThat(appliedChanges.getModel().getBrandSurveyId()).isEqualTo("brand_survey_id");
    }

    @Test
    public void onChangesAppliedTest_notChanged_isBrandLiftHidden() {
        when(featureService.isEnabled(anyLong(), eq(FeatureName.BRAND_LIFT_HIDDEN))).thenReturn(true);

        campaign
                .withMetrikaCounters(List.of(METRIKA_COUNTER_ID))
                .withBrandSurveyId("brand_survey_id")
                .withIsBrandLiftHidden(false);

        ModelChanges<CampaignWithBrandLift> modelChanges =
                new ModelChanges<>(campaign.getId(), CampaignWithBrandLift.class);

        AppliedChanges<CampaignWithBrandLift> appliedChanges = modelChanges.applyTo(campaign);
        operationSupport.onChangesApplied(updateParameters, List.of(appliedChanges));

        assertThat(appliedChanges.getModel().getIsBrandLiftHidden()).isFalse();
        assertThat(appliedChanges.getModel().getBrandSurveyId()).isEqualTo("brand_survey_id");
    }

    @Test
    public void onChangesAppliedTest_createBl_isBrandLiftHidden() {
        when(featureService.isEnabled(anyLong(), eq(FeatureName.BRAND_LIFT_HIDDEN))).thenReturn(true);

        campaign
                .withMetrikaCounters(List.of(METRIKA_COUNTER_ID))
                .withBrandSurveyId(null);

        ModelChanges<CampaignWithBrandLift> modelChanges =
                new ModelChanges<>(campaign.getId(), CampaignWithBrandLift.class)
                        .process("brand_survey_id", CampaignWithBrandLift.BRAND_SURVEY_ID);

        AppliedChanges<CampaignWithBrandLift> appliedChanges = modelChanges.applyTo(campaign);
        operationSupport.onChangesApplied(updateParameters, List.of(appliedChanges));

        assertThat(appliedChanges.getModel().getIsBrandLiftHidden()).isTrue();
        assertThat(appliedChanges.getModel().getBrandSurveyId()).isEqualTo("brand_survey_id");
    }

    @Test
    public void onChangesAppliedTest_createBlOverHidden() {
        when(featureService.isEnabled(anyLong(), eq(FeatureName.BRAND_LIFT_HIDDEN))).thenReturn(false);

        campaign
                .withMetrikaCounters(List.of(METRIKA_COUNTER_ID))
                .withBrandSurveyId("brand_survey_id_hidden")
                .withAbSegmentGoalIds(List.of(1L))
                .withSectionIds(List.of(2L))
                .withIsBrandLiftHidden(true);

        ModelChanges<CampaignWithBrandLift> modelChanges =
                new ModelChanges<>(campaign.getId(), CampaignWithBrandLift.class)
                        .process("brand_survey_id", CampaignWithBrandLift.BRAND_SURVEY_ID)
                        .process(List.of(3L), CampaignWithBrandLift.AB_SEGMENT_GOAL_IDS)
                        .process(List.of(4L), CampaignWithBrandLift.SECTION_IDS);

        AppliedChanges<CampaignWithBrandLift> appliedChanges = modelChanges.applyTo(campaign);
        operationSupport.onChangesApplied(updateParameters, List.of(appliedChanges));

        assertThat(appliedChanges.getModel().getIsBrandLiftHidden()).isFalse();
        assertThat(appliedChanges.getModel().getBrandSurveyId()).isEqualTo("brand_survey_id");
        assertThat(appliedChanges.getModel().getAbSegmentGoalIds()).isEqualTo(List.of(3L));
        assertThat(appliedChanges.getModel().getSectionIds()).isEqualTo(List.of(4L));
    }

    @Test
    public void onChangesAppliedTest_createExperimentOverHidden() {
        when(featureService.isEnabled(anyLong(), eq(FeatureName.BRAND_LIFT_HIDDEN))).thenReturn(false);

        campaign
                .withMetrikaCounters(List.of(METRIKA_COUNTER_ID))
                .withBrandSurveyId("brand_survey_id_hidden")
                .withAbSegmentGoalIds(List.of(1L))
                .withSectionIds(List.of(2L))
                .withIsBrandLiftHidden(true);

        ModelChanges<CampaignWithBrandLift> modelChanges =
                new ModelChanges<>(campaign.getId(), CampaignWithBrandLift.class)
                        .process(null, CampaignWithBrandLift.BRAND_SURVEY_ID)
                        .process(List.of(3L), CampaignWithBrandLift.AB_SEGMENT_GOAL_IDS)
                        .process(List.of(4L), CampaignWithBrandLift.SECTION_IDS);

        AppliedChanges<CampaignWithBrandLift> appliedChanges = modelChanges.applyTo(campaign);
        operationSupport.onChangesApplied(updateParameters, List.of(appliedChanges));

        assertThat(appliedChanges.getModel().getIsBrandLiftHidden()).isFalse();
        assertThat(appliedChanges.getModel().getBrandSurveyId()).isNull();
        assertThat(appliedChanges.getModel().getAbSegmentGoalIds()).isEqualTo(List.of(3L));
        assertThat(appliedChanges.getModel().getSectionIds()).isEqualTo(List.of(4L));
    }

    @Test
    public void onChangesAppliedTest_dontChangeHidden() {
        when(featureService.isEnabled(anyLong(), eq(FeatureName.BRAND_LIFT_HIDDEN))).thenReturn(false);

        campaign
                .withMetrikaCounters(List.of(METRIKA_COUNTER_ID))
                .withBrandSurveyId("brand_survey_id_hidden")
                .withAbSegmentGoalIds(List.of(1L))
                .withSectionIds(List.of(2L))
                .withIsBrandLiftHidden(true);

        ModelChanges<CampaignWithBrandLift> modelChanges =
                new ModelChanges<>(campaign.getId(), CampaignWithBrandLift.class)
                        .process(null, CampaignWithBrandLift.BRAND_SURVEY_ID)
                        .process(null, CampaignWithBrandLift.AB_SEGMENT_GOAL_IDS)
                        .process(null, CampaignWithBrandLift.SECTION_IDS);

        AppliedChanges<CampaignWithBrandLift> appliedChanges = modelChanges.applyTo(campaign);
        operationSupport.onChangesApplied(updateParameters, List.of(appliedChanges));

        assertThat(appliedChanges.getModel().getIsBrandLiftHidden()).isTrue();
        assertThat(appliedChanges.getModel().getBrandSurveyId()).isEqualTo("brand_survey_id_hidden");
        assertThat(appliedChanges.getModel().getAbSegmentGoalIds()).isEqualTo(List.of(1L));
        assertThat(appliedChanges.getModel().getSectionIds()).isEqualTo(List.of(2L));
    }

    @Test
    public void onChangesAppliedTest_dontChangeHidden_isBrandLiftHidden() {
        when(featureService.isEnabled(anyLong(), eq(FeatureName.BRAND_LIFT_HIDDEN))).thenReturn(true);

        campaign
                .withMetrikaCounters(List.of(METRIKA_COUNTER_ID))
                .withBrandSurveyId("brand_survey_id_hidden")
                .withAbSegmentGoalIds(List.of(1L))
                .withSectionIds(List.of(2L))
                .withIsBrandLiftHidden(true);

        ModelChanges<CampaignWithBrandLift> modelChanges =
                new ModelChanges<>(campaign.getId(), CampaignWithBrandLift.class)
                        .process("brand_survey_id_hidden", CampaignWithBrandLift.BRAND_SURVEY_ID)
                        .process(null, CampaignWithBrandLift.AB_SEGMENT_GOAL_IDS)
                        .process(null, CampaignWithBrandLift.SECTION_IDS);

        AppliedChanges<CampaignWithBrandLift> appliedChanges = modelChanges.applyTo(campaign);
        operationSupport.onChangesApplied(updateParameters, List.of(appliedChanges));

        assertThat(appliedChanges.getModel().getIsBrandLiftHidden()).isTrue();
        assertThat(appliedChanges.getModel().getBrandSurveyId()).isEqualTo("brand_survey_id_hidden");
        assertThat(appliedChanges.getModel().getAbSegmentGoalIds()).isEqualTo(List.of(1L));
        assertThat(appliedChanges.getModel().getSectionIds()).isEqualTo(List.of(2L));
    }

    private static CreateExperimentResponseEnvelope createExperimentResponse(
            Long experimentId, Long segmentAId, Long segmentBId) {
        return new CreateExperimentResponseEnvelope()
                .withCreateExperimentResponse(new CreateExperimentResponse()
                        .withExperimentId(experimentId)
                        .withExperimentSegments(List.of(
                                new ExperimentSegmentResponse().withSegmentId(segmentAId),
                                new ExperimentSegmentResponse().withSegmentId(segmentBId))
                        )
                );
    }

    private static CreateExperimentRequest createExperimentRequest() {
        ExperimentSegmentRequest segmentA = new ExperimentSegmentRequest()
                .withName("A")
                .withStart(0)
                .withEnd(90);

        ExperimentSegmentRequest segmentB = new ExperimentSegmentRequest()
                .withName("B")
                .withStart(90)
                .withEnd(100);

        return new CreateExperimentRequest()
                .withExperimentName(String.format("Client %d global segment", clientId.asLong()))
                .withBkSettings(BRANDLIFT_BK_SETTINGS)
                .withExperimentSegmentRequests(List.of(segmentA, segmentB))
                .withCounterIds(List.of(DEFAULT_METRIKA_COUNTER_FOR_AB));
    }

    private static SetExperimentGrantRequest setExperimentGrantRequest() {
        return new SetExperimentGrantRequest()
                .withUserLogin(BRAND_LIFT_SURVEYS_LOGIN)
                .withPermission("view");
    }

    private static SetExperimentGrantResponseEnvelope setExperimentGrantResponse() {
        return new SetExperimentGrantResponseEnvelope()
                .withSetExperimentGrantResponse(new SetExperimentGrantResponse()
                        .withPermission("view")
                );
    }

    private static List<ExperimentRetargetingConditions> experimentRetargetingConditions() {
        return List.of(new ExperimentRetargetingConditions()
                .withStatisticRetargetingConditionId(RET_COND_ID)
        );
    }

    private static CampaignWithBrandLift createCampaign() {
        return new CpmBannerCampaign()
                .withClientId(clientId.asLong())
                .withName("campaign")
                .withType(CampaignType.CPM_BANNER)
                .withId(campaignId)
                .withUid(uid);
    }
}
