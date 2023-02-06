package ru.yandex.direct.core.entity.campaign.service.validation.type.update;

import java.math.BigDecimal;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import ru.yandex.direct.common.net.NetAcl;
import ru.yandex.direct.core.entity.campaign.model.CampaignType;
import ru.yandex.direct.core.entity.campaign.model.CampaignWithMeaningfulGoals;
import ru.yandex.direct.core.entity.campaign.model.CampaignWithMeaningfulGoalsWithRequiredFields;
import ru.yandex.direct.core.entity.campaign.model.MeaningfulGoal;
import ru.yandex.direct.core.entity.campaign.model.TextCampaign;
import ru.yandex.direct.core.entity.campaign.service.CampaignOptions;
import ru.yandex.direct.core.entity.campaign.service.RequestBasedMetrikaClientAdapter;
import ru.yandex.direct.core.entity.campaign.service.type.update.container.RestrictedCampaignsUpdateOperationContainerImpl;
import ru.yandex.direct.core.entity.feature.service.FeatureService;
import ru.yandex.direct.core.entity.metrika.service.campaigngoals.CampaignGoalsService;
import ru.yandex.direct.core.entity.retargeting.model.Goal;
import ru.yandex.direct.currency.CurrencyCode;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.metrika.client.MetrikaClientException;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.utils.InterruptedRuntimeException;
import ru.yandex.direct.validation.result.ValidationResult;

import static java.math.RoundingMode.HALF_UP;
import static java.util.Collections.emptyMap;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants.ENGAGED_SESSION_GOAL_ID;
import static ru.yandex.direct.core.testing.data.TestCampaigns.newCampaignByCampaignType;
import static ru.yandex.direct.core.validation.defects.Defects.metrikaReturnsResultWithErrors;
import static ru.yandex.direct.test.utils.RandomNumberUtils.nextPositiveInteger;
import static ru.yandex.direct.test.utils.RandomNumberUtils.nextPositiveLong;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectWithDefinition;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.defect.ids.CollectionDefectIds.Gen.MUST_BE_IN_COLLECTION;
import static ru.yandex.direct.validation.defect.ids.NumberDefectIds.MUST_BE_IN_THE_INTERVAL_INCLUSIVE;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@RunWith(Parameterized.class)
public class CampaignWithMeaningfulGoalsUpdateValidationTypeSupportTest {
    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule().silent();
    @Mock
    private CampaignGoalsService campaignGoalsService;
    @Mock
    private RequestBasedMetrikaClientAdapter metrikaClientAdapter;
    @Mock
    private NetAcl netAcl;
    @Mock
    private FeatureService featureService;

    @InjectMocks
    private CampaignWithMeaningfulGoalsUpdateValidationTypeSupport typeSupport;
    private ClientId clientId;
    private Long uid;
    private Long id;
    private Long goalId;
    private long goalIdOther;
    private Integer shard;
    private Goal goal;
    private ModelChanges<CampaignWithMeaningfulGoalsWithRequiredFields> modelChanges;
    private Map<Long, CampaignWithMeaningfulGoalsWithRequiredFields> unmodifiedModels;
    private Map<Long, CampaignWithMeaningfulGoalsWithRequiredFields> unmodifiedModelsWithGoals;
    private ModelChanges<CampaignWithMeaningfulGoalsWithRequiredFields> modelChangesWithTwoGoals;

    @Parameterized.Parameter
    public CampaignType campaignType;

    @Parameterized.Parameters(name = "{0}")
    public static Collection typeOfCampaignParameter() {
        return Arrays.asList(new Object[][]{
                {CampaignType.TEXT},
                {CampaignType.MCBANNER},
        });
    }

    @Before
    public void before() {
        clientId = ClientId.fromLong(nextPositiveLong());
        uid = nextPositiveLong();
        id = nextPositiveLong();
        shard = nextPositiveInteger();
        goalId = nextPositiveLong();
        goal = (Goal) new Goal().withId(goalId);
        MeaningfulGoal meaningfulGoal = new MeaningfulGoal()
                .withGoalId(goalId)
                .withConversionValue(BigDecimal.TEN.setScale(5, HALF_UP));

        goalIdOther = nextPositiveLong();
        MeaningfulGoal meaningfulGoalOther = new MeaningfulGoal()
                .withGoalId(goalIdOther)
                .withConversionValue(BigDecimal.TEN.setScale(5, HALF_UP));
        var defaultCampaign = (CampaignWithMeaningfulGoalsWithRequiredFields) newCampaignByCampaignType(campaignType)
                .withId(id);
        modelChanges = ModelChanges
                .build(defaultCampaign, CampaignWithMeaningfulGoals.MEANINGFUL_GOALS, List.of(meaningfulGoal));
        modelChangesWithTwoGoals = ModelChanges.build(defaultCampaign, CampaignWithMeaningfulGoals.MEANINGFUL_GOALS,
                List.of(meaningfulGoal, meaningfulGoalOther));
        unmodifiedModels = Map.of(id,
                ((CampaignWithMeaningfulGoalsWithRequiredFields) newCampaignByCampaignType(campaignType))
                        .withId(id)
                        .withCurrency(CurrencyCode.RUB)
                        .withMeaningfulGoals(Collections.emptyList()));
        unmodifiedModelsWithGoals = Map.of(id,
                ((CampaignWithMeaningfulGoalsWithRequiredFields) newCampaignByCampaignType(campaignType))
                        .withId(id)
                        .withCurrency(CurrencyCode.RUB)
                        .withMeaningfulGoals(
                                List.of(new MeaningfulGoal()
                                        .withGoalId(goalId)
                                        .withConversionValue(BigDecimal.TEN.setScale(20, HALF_UP)))));

        doReturn(false).when(netAcl).isInternalIp(any(InetAddress.class));
    }

    @Test
    public void validateBeforeApply_Successfully() {
        doReturn(Map.of(id, Set.of(goal))).when(campaignGoalsService)
                .getAvailableGoalsForCampaignId(eq(uid), eq(clientId), anyMap(), eq(metrikaClientAdapter));
        var container = new RestrictedCampaignsUpdateOperationContainerImpl(shard, uid, clientId, uid, uid,
                metrikaClientAdapter, new CampaignOptions(), null, emptyMap());
        var vr = typeSupport.validateBeforeApply(container,
                new ValidationResult<>(List.of(modelChanges)), unmodifiedModels);
        assertThat(vr, hasNoDefectsDefinitions());
    }

    @Test
    public void validateBeforeApply_WithMetrikaClientException() {
        doThrow(new MetrikaClientException()).when(campaignGoalsService)
                .getAvailableGoalsForCampaignId(eq(uid), eq(clientId), anyMap(), eq(metrikaClientAdapter));
        var container = new RestrictedCampaignsUpdateOperationContainerImpl(shard, uid, clientId, uid, uid,
                metrikaClientAdapter, new CampaignOptions(), null, emptyMap());
        var vr = typeSupport.validateBeforeApply(container,
                new ValidationResult<>(List.of(modelChanges)), unmodifiedModels);
        assertThat(vr, hasDefectDefinitionWith(validationError(path(index(0)), metrikaReturnsResultWithErrors())));
    }

    @Test
    public void validateBeforeApply_WithMetrikaClientExceptionGoalNotChanged_HasNoErrors() {
        doThrow(new MetrikaClientException()).when(campaignGoalsService)
                .getAvailableGoalsForCampaignId(eq(uid), eq(clientId), anyMap(), eq(metrikaClientAdapter));
        var container = new RestrictedCampaignsUpdateOperationContainerImpl(shard, uid, clientId, uid, uid,
                metrikaClientAdapter, new CampaignOptions(), null, emptyMap());
        var vr = typeSupport.validateBeforeApply(container,
                new ValidationResult<>(List.of(modelChanges)), unmodifiedModelsWithGoals);
        assertThat(vr, hasNoDefectsDefinitions());
    }

    @Test
    public void validateBeforeApply_WithMetrikaClientException_hasConversionValueError() {
        doThrow(new MetrikaClientException()).when(campaignGoalsService)
                .getAvailableGoalsForCampaignId(eq(uid), eq(clientId), anyMap(), eq(metrikaClientAdapter));
        modelChanges.process(List.of(new MeaningfulGoal()
                        .withGoalId(ENGAGED_SESSION_GOAL_ID)
                        .withConversionValue(CurrencyCode.RUB.getCurrency().getMaxAutobudget().add(BigDecimal.valueOf(1)))),
                TextCampaign.MEANINGFUL_GOALS);
        var container = new RestrictedCampaignsUpdateOperationContainerImpl(shard, uid, clientId, uid, uid,
                metrikaClientAdapter, new CampaignOptions(), null, emptyMap());
        var vr = typeSupport.validateBeforeApply(container,
                new ValidationResult<>(List.of(modelChanges)), unmodifiedModels);
        assertThat(vr, hasDefectWithDefinition(validationError(path(index(0), field(TextCampaign.MEANINGFUL_GOALS),
                        index(0), field(MeaningfulGoal.CONVERSION_VALUE)), MUST_BE_IN_THE_INTERVAL_INCLUSIVE)));
    }

    @Test
    public void validateBeforeApply_WithInterruptedRuntimeException() {
        doThrow(new InterruptedRuntimeException()).when(campaignGoalsService)
                .getAvailableGoalsForCampaignId(eq(uid), eq(clientId), anyMap(), eq(metrikaClientAdapter));
        var container = new RestrictedCampaignsUpdateOperationContainerImpl(shard, uid, clientId, uid, uid,
                metrikaClientAdapter, new CampaignOptions(), null, emptyMap());
        var vr = typeSupport.validateBeforeApply(container,
                new ValidationResult<>(List.of(modelChanges)), unmodifiedModels);
        assertThat(vr, hasDefectDefinitionWith(validationError(path(index(0)), metrikaReturnsResultWithErrors())));
    }

    @Test
    public void validateBeforeApply_MetrikaCounterIsUnavailableErrors() {
        doReturn(Map.of(id, Set.of())).when(campaignGoalsService)
                .getAvailableGoalsForCampaignId(eq(uid), eq(clientId), anyMap(), eq(metrikaClientAdapter));
        var container = new RestrictedCampaignsUpdateOperationContainerImpl(shard, uid, clientId, uid, uid,
                metrikaClientAdapter, new CampaignOptions(), null, emptyMap());
        var vr = typeSupport.validateBeforeApply(container,
                new ValidationResult<>(List.of(modelChanges)), unmodifiedModels);
        var path = path(index(0), field(CampaignWithMeaningfulGoals.MEANINGFUL_GOALS),
                index(0), field(MeaningfulGoal.GOAL_ID));
        assertThat(vr, hasDefectDefinitionWith(validationError(path, MUST_BE_IN_COLLECTION)));
    }

    @Test
    public void validateBeforeApply_AddingTwoGoals_OneGoalIsUnavailableErrors() {
        doReturn(Map.of(id, Set.of(new Goal().withId(goalIdOther)))).when(campaignGoalsService)
                .getAvailableGoalsForCampaignId(eq(uid), eq(clientId), anyMap(), eq(metrikaClientAdapter));
        var container = new RestrictedCampaignsUpdateOperationContainerImpl(shard, uid, clientId, uid, uid,
                metrikaClientAdapter, new CampaignOptions(), null, emptyMap());
        var vr = typeSupport.validateBeforeApply(container,
                new ValidationResult<>(List.of(modelChangesWithTwoGoals)), unmodifiedModelsWithGoals);

        assertThat(vr, hasNoDefectsDefinitions());
    }

    @Test
    public void validateBeforeApply_MetrikaCountersNotChangedButNotValid_HasNoErrors() {
        doReturn(Map.of(id, Set.of())).when(campaignGoalsService)
                .getAvailableGoalsForCampaignId(eq(uid), eq(clientId), anyMap(), eq(metrikaClientAdapter));
        var container = new RestrictedCampaignsUpdateOperationContainerImpl(shard, uid, clientId, uid, uid,
                metrikaClientAdapter, new CampaignOptions(), null, emptyMap());
        var vr = typeSupport.validateBeforeApply(container,
                new ValidationResult<>(List.of(modelChanges)), unmodifiedModelsWithGoals);
        assertThat(vr, hasNoDefectsDefinitions());
    }

    @Test
    public void validateBeforeApply_ValidButMissingFromUnmodifiedModelsCampaign_Successfully() {
        doReturn(Map.of(id, Set.of())).when(campaignGoalsService)
                .getAvailableGoalsForCampaignId(eq(uid), eq(clientId), anyMap(), eq(metrikaClientAdapter));
        var container = new RestrictedCampaignsUpdateOperationContainerImpl(shard, uid, clientId, uid, uid,
                metrikaClientAdapter, new CampaignOptions(), null, emptyMap());
        var vr = typeSupport.validateBeforeApply(container,
                new ValidationResult<>(List.of(modelChanges)), Map.of());
        assertThat(vr, hasNoDefectsDefinitions());
    }
}
