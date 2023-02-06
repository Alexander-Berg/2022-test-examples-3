package ru.yandex.direct.core.entity.campaign.service.validation.type.add;

import java.math.BigDecimal;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
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
import ru.yandex.direct.core.entity.campaign.model.CampaignWithMeaningfulGoalsWithRequiredFields;
import ru.yandex.direct.core.entity.campaign.model.MeaningfulGoal;
import ru.yandex.direct.core.entity.campaign.service.CampaignOptions;
import ru.yandex.direct.core.entity.campaign.service.RequestBasedMetrikaClientAdapter;
import ru.yandex.direct.core.entity.campaign.service.type.add.container.RestrictedCampaignsAddOperationContainerImpl;
import ru.yandex.direct.core.entity.feature.service.FeatureService;
import ru.yandex.direct.core.entity.metrika.container.CampaignTypeWithCounterIds;
import ru.yandex.direct.core.entity.metrika.service.campaigngoals.CampaignGoalsService;
import ru.yandex.direct.core.entity.retargeting.model.Goal;
import ru.yandex.direct.currency.CurrencyCode;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.metrika.client.MetrikaClientException;
import ru.yandex.direct.validation.result.ValidationResult;

import static java.math.RoundingMode.HALF_UP;
import static java.util.Collections.emptyMap;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static ru.yandex.direct.core.testing.data.TestCampaigns.newCampaignByCampaignType;
import static ru.yandex.direct.core.validation.defects.Defects.metrikaReturnsResultWithErrors;
import static ru.yandex.direct.test.utils.RandomNumberUtils.nextPositiveInteger;
import static ru.yandex.direct.test.utils.RandomNumberUtils.nextPositiveLong;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.defect.ids.CollectionDefectIds.Gen.MUST_BE_IN_COLLECTION;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@RunWith(Parameterized.class)
public class CampaignWithMeaningfulGoalsAddValidationTypeSupportTest {
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
    private CampaignWithMeaningfulGoalsAddValidationTypeSupport typeSupport;
    private ClientId clientId;
    private Long uid;
    private Integer shard;
    private Goal goal;
    private CampaignWithMeaningfulGoalsWithRequiredFields campaignWithOneGoal;
    private CampaignTypeWithCounterIds campaignTypeWithCounterIds;

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
        Long id = nextPositiveLong();
        shard = nextPositiveInteger();
        Long goalId = nextPositiveLong();
        goal = (Goal) new Goal().withId(goalId);

        campaignWithOneGoal = ((CampaignWithMeaningfulGoalsWithRequiredFields) newCampaignByCampaignType(campaignType))
                .withId(id)
                .withCurrency(CurrencyCode.RUB)
                .withMeaningfulGoals(
                        List.of(new MeaningfulGoal()
                                .withGoalId(goalId)
                                .withConversionValue(BigDecimal.TEN.setScale(20, HALF_UP))));
        campaignTypeWithCounterIds = new CampaignTypeWithCounterIds()
                .withCampaignType(campaignType)
                .withCounterIds(new HashSet<>());

        doReturn(false).when(netAcl).isInternalIp(any(InetAddress.class));
    }

    @Test
    public void validate_Successfully() {
        doReturn(Map.of(campaignTypeWithCounterIds, Set.of(goal))).when(campaignGoalsService)
                .getAvailableGoalsForCampaignType(eq(uid), eq(clientId), anySet(), eq(metrikaClientAdapter));
        var container = new RestrictedCampaignsAddOperationContainerImpl(shard, uid, clientId, uid, uid,
                null, new CampaignOptions(), metrikaClientAdapter, emptyMap());
        var vr = typeSupport.validate(container, new ValidationResult<>(List.of(campaignWithOneGoal)));
        assertThat(vr, hasNoDefectsDefinitions());
    }

    @Test
    public void validate_WithMetrikaClientException() {
        doThrow(new MetrikaClientException()).when(campaignGoalsService)
                .getAvailableGoalsForCampaignType(eq(uid), eq(clientId), anySet(), eq(metrikaClientAdapter));
        var container = new RestrictedCampaignsAddOperationContainerImpl(shard, uid, clientId, uid, uid,
                null, new CampaignOptions(), metrikaClientAdapter, emptyMap());
        var vr = typeSupport.validate(container, new ValidationResult<>(List.of(campaignWithOneGoal)));
        assertThat(vr, hasDefectDefinitionWith(validationError(path(index(0)), metrikaReturnsResultWithErrors())));
    }

    @Test
    public void validate_MetrikaCounterIsUnavailableErrors() {
        doReturn(Map.of(campaignTypeWithCounterIds, Set.of())).when(campaignGoalsService)
                .getAvailableGoalsForCampaignType(eq(uid), eq(clientId), anySet(), eq(metrikaClientAdapter));
        var container = new RestrictedCampaignsAddOperationContainerImpl(shard, uid, clientId, uid, uid,
                null, new CampaignOptions(), metrikaClientAdapter, emptyMap());
        var vr = typeSupport.validate(container, new ValidationResult<>(List.of(campaignWithOneGoal)));
        var path = path(index(0), field(CampaignWithMeaningfulGoalsWithRequiredFields.MEANINGFUL_GOALS),
                index(0), field(MeaningfulGoal.GOAL_ID));
        assertThat(vr, hasDefectDefinitionWith(validationError(path, MUST_BE_IN_COLLECTION)));
    }
}
