package ru.yandex.direct.core.entity.campaign.service.validation.type.update;

import java.util.Arrays;
import java.util.Collection;
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

import ru.yandex.direct.core.entity.campaign.model.CampaignType;
import ru.yandex.direct.core.entity.campaign.model.CampaignWithExperiments;
import ru.yandex.direct.core.entity.campaign.service.CampaignOptions;
import ru.yandex.direct.core.entity.campaign.service.RequestBasedMetrikaClientAdapter;
import ru.yandex.direct.core.entity.campaign.service.type.update.container.RestrictedCampaignsUpdateOperationContainer;
import ru.yandex.direct.core.entity.campaign.service.type.update.container.RestrictedCampaignsUpdateOperationContainerImpl;
import ru.yandex.direct.core.entity.feature.service.FeatureService;
import ru.yandex.direct.core.entity.retargeting.service.RetargetingConditionService;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.metrika.client.MetrikaClientException;
import ru.yandex.direct.metrika.client.model.response.RetargetingCondition;
import ru.yandex.direct.test.utils.RandomNumberUtils;
import ru.yandex.direct.utils.InterruptedRuntimeException;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static java.util.Collections.emptyMap;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verifyZeroInteractions;
import static ru.yandex.direct.core.testing.data.TestCampaigns.newCampaignByCampaignType;
import static ru.yandex.direct.core.validation.defects.Defects.metrikaReturnsResultWithErrors;
import static ru.yandex.direct.feature.FeatureName.AB_SEGMENTS;
import static ru.yandex.direct.feature.FeatureName.EXPERIMENT_RET_CONDITIONS_CREATING_ON_TEXT_CAMPAIGNS_MODIFY_IN_JAVA_FOR_DNA;
import static ru.yandex.direct.metrika.client.model.response.RetargetingCondition.Type.AB_SEGMENT;
import static ru.yandex.direct.test.utils.RandomNumberUtils.nextPositiveLong;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@RunWith(Parameterized.class)
public class CampaignWithExperimentsUpdateValidationTypeSupportMetrikaTest {
    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule().silent();

    @Mock
    private FeatureService featureService;

    @Mock
    private RetargetingConditionService retargetingConditionService;

    @InjectMocks
    private CampaignWithExperimentsUpdateValidationTypeSupport typeSupport;

    @Mock
    private RequestBasedMetrikaClientAdapter metrikaClientAdapter;

    private ClientId clientId;
    private Long operatorUid;
    private int shard;
    private long sectionId;
    private RestrictedCampaignsUpdateOperationContainer container;

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
        clientId = ClientId.fromLong(RandomNumberUtils.nextPositiveLong());
        operatorUid = RandomNumberUtils.nextPositiveLong();
        shard = RandomNumberUtils.nextPositiveInteger();
        sectionId = nextPositiveLong();

        container = new RestrictedCampaignsUpdateOperationContainerImpl(
                shard, operatorUid, clientId, clientId.asLong(), null, metrikaClientAdapter, new CampaignOptions(),
                null, emptyMap()
        );

        doReturn(Set.of(EXPERIMENT_RET_CONDITIONS_CREATING_ON_TEXT_CAMPAIGNS_MODIFY_IN_JAVA_FOR_DNA.getName(),
                AB_SEGMENTS.getName()))
                .when(featureService)
                .getEnabledForClientId(clientId);
    }

    @Test
    public void test_featureOff() {
        doReturn(Set.of(AB_SEGMENTS.getName())).when(featureService).getEnabledForClientId(clientId);
        CampaignWithExperiments campaign = (CampaignWithExperiments) newCampaignByCampaignType(campaignType);

        ValidationResult<List<CampaignWithExperiments>, Defect> vr =
                typeSupport.validate(container, new ValidationResult<>(List.of(campaign)), Map.of());

        assertThat(vr, hasNoDefectsDefinitions());
        verifyZeroInteractions(metrikaClientAdapter);
    }

    @Test
    public void test_featureOn_metrikaWorks() {
        CampaignWithExperiments campaign = ((CampaignWithExperiments) newCampaignByCampaignType(campaignType))
                .withSectionIds(List.of(sectionId));

        doReturn(Map.of(clientId.asLong(), List.of(new RetargetingCondition().withSectionId(sectionId)
                .withType(AB_SEGMENT))))
                .when(metrikaClientAdapter)
                .getAbSegmentGoals();

        ValidationResult<List<CampaignWithExperiments>, Defect> vr =
                typeSupport.validate(container, new ValidationResult<>(List.of(campaign)), Map.of());

        assertThat(vr, hasNoDefectsDefinitions());
    }

    @Test
    public void test_featureOn_metrikaErrorMetrikaClientException() {
        doThrow(new MetrikaClientException()).when(metrikaClientAdapter).getAbSegmentGoals();

        CampaignWithExperiments campaign = ((CampaignWithExperiments) newCampaignByCampaignType(campaignType))
                .withSectionIds(List.of(sectionId));

        ValidationResult<List<CampaignWithExperiments>, Defect> vr =
                typeSupport.validate(container, new ValidationResult<>(List.of(campaign)), Map.of());

        assertThat(vr, hasDefectDefinitionWith(validationError(path(index(0)), metrikaReturnsResultWithErrors())));
    }

    @Test
    public void test_featureOn_metrikaErrorInterruptedRuntimeException() {
        doThrow(new InterruptedRuntimeException()).when(metrikaClientAdapter).getAbSegmentGoals();

        CampaignWithExperiments campaign = ((CampaignWithExperiments) newCampaignByCampaignType(campaignType))
                .withSectionIds(List.of(sectionId));

        ValidationResult<List<CampaignWithExperiments>, Defect> vr =
                typeSupport.validate(container, new ValidationResult<>(List.of(campaign)), Map.of());

        assertThat(vr, hasDefectDefinitionWith(validationError(path(index(0)), metrikaReturnsResultWithErrors())));
    }
}
