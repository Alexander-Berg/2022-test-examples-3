package ru.yandex.direct.core.entity.campaign.service.type.add;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.direct.core.entity.campaign.model.CampaignWithBrandLift;
import ru.yandex.direct.core.entity.campaign.model.CpmBannerCampaign;
import ru.yandex.direct.core.entity.campaign.service.CampaignWithBrandLiftExperimentsService;
import ru.yandex.direct.core.entity.campaign.service.RequestBasedMetrikaClientAdapter;
import ru.yandex.direct.core.entity.campaign.service.type.add.container.RestrictedCampaignsAddOperationContainer;
import ru.yandex.direct.core.entity.feature.service.FeatureService;
import ru.yandex.direct.core.entity.retargeting.model.ExperimentRetargetingConditions;
import ru.yandex.direct.core.entity.retargeting.service.RetargetingConditionService;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.feature.FeatureName;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.AdditionalMatchers.or;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class CampaignWithBrandLiftAddOperationSupportTest {

    @InjectMocks
    private CampaignWithBrandLiftAddOperationSupport operationSupport;

    @Mock
    private RetargetingConditionService retargetingConditionService;

    @Mock
    private ShardHelper shardHelper;

    @Mock
    private FeatureService featureService;

    @Mock
    private CampaignWithBrandLiftExperimentsService campaignWithBrandLiftExperimentsService;

    @Mock
    private RestrictedCampaignsAddOperationContainer container;

    @Before
    public void setUp() {
        when(container.getMetrikaClient())
                .thenReturn(mock(RequestBasedMetrikaClientAdapter.class));
        when(container.getClientId())
                .thenReturn(ClientId.fromLong(1));
    }

    @Test
    public void shouldPrepareExperimentsBeforeExecution_ifExperimentsHaveDifferentSectionId() {
        //given
        var campaigns = List.of(createCampaign("test"), createCampaign("test"));
        var retargetingCondition = new ExperimentRetargetingConditions()
                .withRetargetingConditionId(1L)
                .withStatisticRetargetingConditionId(2L);

        when(shardHelper.getLoginByUid(anyLong()))
                .thenReturn("login");
        when(campaignWithBrandLiftExperimentsService.prepareExperiment(any(), any()))
                .thenReturn(Map.of(1L, 1L), Map.of(2L, 2L));
        when(retargetingConditionService.findOrCreateExperimentsRetargetingConditions(any(), any(), any(), any()))
                .thenReturn(List.of(retargetingCondition, retargetingCondition));

        //when
        operationSupport.beforeExecution(container, campaigns);

        //then
        verify(campaignWithBrandLiftExperimentsService, times(2))
                .prepareExperiment(any(), any());
        var preparedExperimentsCapture = ArgumentCaptor.forClass(Map.class);
        verify(retargetingConditionService, times(1))
                .findOrCreateExperimentsRetargetingConditions(any(), any(), any(), preparedExperimentsCapture.capture());
        assertThat(preparedExperimentsCapture.getAllValues().get(0))
                .hasSize(2)
                .containsEntry(1L, List.of(1L))
                .containsEntry(2L, List.of(2L));
        campaigns.forEach(campaign -> assertThat(campaign)
                .hasFieldOrPropertyWithValue("abSegmentRetargetingConditionId", 1L)
                .hasFieldOrPropertyWithValue("abSegmentStatisticRetargetingConditionId", 2L)
        );
    }

    @Test
    public void shouldPrepareExperimentsBeforeExecution_ifExperimentsHaveSameSectionId() {
        //given
        var campaigns = List.of(createCampaign("test"), createCampaign("test"));
        var retargetingCondition = new ExperimentRetargetingConditions()
                .withRetargetingConditionId(1L)
                .withStatisticRetargetingConditionId(2L);

        when(shardHelper.getLoginByUid(anyLong()))
                .thenReturn("login");
        when(campaignWithBrandLiftExperimentsService.prepareExperiment(any(), any()))
                .thenReturn(Map.of(1L, 1L), Map.of(1L, 2L));
        when(retargetingConditionService.findOrCreateExperimentsRetargetingConditions(any(), any(), any(), any()))
                .thenReturn(List.of(retargetingCondition, retargetingCondition));

        //when
        operationSupport.beforeExecution(container, campaigns);

        //then
        verify(campaignWithBrandLiftExperimentsService, times(2))
                .prepareExperiment(any(), any());
        var preparedExperimentsCapture = ArgumentCaptor.forClass(Map.class);
        verify(retargetingConditionService, times(1))
                .findOrCreateExperimentsRetargetingConditions(any(), any(), any(), preparedExperimentsCapture.capture());
        assertThat(preparedExperimentsCapture.getAllValues().get(0))
                .hasSize(1)
                .containsEntry(1L, List.of(1L, 2L));
        campaigns.forEach(campaign -> assertThat(campaign)
                .hasFieldOrPropertyWithValue("abSegmentRetargetingConditionId", 1L)
                .hasFieldOrPropertyWithValue("abSegmentStatisticRetargetingConditionId", 2L)
        );
    }

    @Test
    public void shouldUpdateCampaignsBeforeException_withoutPreparingExperiments_ifThereAreNoCampaignsWithBrandSurveyId() {
        //given
        var campaigns = List.of(createCampaign(null));
        var retargetingCondition = new ExperimentRetargetingConditions()
                .withRetargetingConditionId(1L)
                .withStatisticRetargetingConditionId(2L);
        when(retargetingConditionService.findOrCreateExperimentsRetargetingConditions(any(), any(), any(), any()))
                .thenReturn(List.of(retargetingCondition, retargetingCondition));

        //when
        operationSupport.beforeExecution(container, campaigns);

        //then
        verify(campaignWithBrandLiftExperimentsService, never()).prepareExperiment(any(), any());
        verify(retargetingConditionService, times(1))
                .findOrCreateExperimentsRetargetingConditions(any(), any(), any(), or(isNull(), eq(Collections.emptyMap())));
        campaigns.forEach(campaign -> assertThat(campaign)
                .hasFieldOrPropertyWithValue("abSegmentRetargetingConditionId", 1L)
                .hasFieldOrPropertyWithValue("abSegmentStatisticRetargetingConditionId", 2L)
        );
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowExceptionBeforeExecution_ifUserNotFound() {
        var campaigns = List.of(createCampaign("test"));

        when(shardHelper.getLoginsByUids(any()))
                .thenReturn(List.of(Collections.emptyList()));
        when(shardHelper.getLoginByUid(anyLong())).thenCallRealMethod();

        operationSupport.beforeExecution(container, campaigns);
    }

    @Test(expected = NullPointerException.class)
    public void shouldThrowExceptionBeforeExecution_ifLoginIsNull() {
        var campaigns = List.of(createCampaign("test"));

        when(shardHelper.getLoginByUid(anyLong()))
                .thenReturn(null);
        when(campaignWithBrandLiftExperimentsService.prepareExperiment(any(), any())).thenCallRealMethod();
        operationSupport.beforeExecution(container, campaigns);
    }

    @Test
    public void shouldSetIsBrandLiftHiddenField() {
        var campaigns = List.of(createCampaign("test"), createCampaign(null));
        when(featureService.isEnabled(anyLong(), eq(FeatureName.BRAND_LIFT_HIDDEN))).thenReturn(true);

        operationSupport.onPreValidated(container, campaigns);

        assertThat(campaigns.get(0).getIsBrandLiftHidden()).isTrue();
        assertThat(campaigns.get(1).getIsBrandLiftHidden()).isFalse();
    }

    private CampaignWithBrandLift createCampaign(String brandSurveyId) {
        return new CpmBannerCampaign()
                .withBrandSurveyId(brandSurveyId);
    }
}
