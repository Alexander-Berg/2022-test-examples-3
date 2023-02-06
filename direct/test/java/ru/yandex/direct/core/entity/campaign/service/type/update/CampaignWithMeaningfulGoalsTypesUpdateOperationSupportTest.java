package ru.yandex.direct.core.entity.campaign.service.type.update;

import java.math.BigDecimal;
import java.util.List;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import ru.yandex.direct.core.entity.campaign.model.CampaignType;
import ru.yandex.direct.core.entity.campaign.model.CampaignWithMeaningfulGoalsWithRequiredFields;
import ru.yandex.direct.core.entity.campaign.model.DbStrategy;
import ru.yandex.direct.core.entity.campaign.model.MeaningfulGoal;
import ru.yandex.direct.core.entity.campaign.model.StrategyName;
import ru.yandex.direct.core.entity.campaign.service.CampaignOptions;
import ru.yandex.direct.core.entity.campaign.service.RequestBasedMetrikaClientAdapter;
import ru.yandex.direct.core.entity.campaign.service.type.update.container.RestrictedCampaignsUpdateOperationContainer;
import ru.yandex.direct.core.entity.campaign.service.type.update.container.RestrictedCampaignsUpdateOperationContainerImpl;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.data.TestCampaigns;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.model.AppliedChanges;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.test.utils.RandomNumberUtils;

import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;

@CoreTest
public class CampaignWithMeaningfulGoalsTypesUpdateOperationSupportTest {
    CampaignWithMeaningfulGoalsTypesUpdateOperationSupport support;

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule().silent();


    private static ClientId clientId;
    private static Long uid;
    private static Long campaignId;
    private static CampaignWithMeaningfulGoalsWithRequiredFields campaign;
    private static RestrictedCampaignsUpdateOperationContainer updateParameters;

    @Mock
    private RequestBasedMetrikaClientAdapter metrikaClientAdapter;

    @Before
    public void before() {
        support = new CampaignWithMeaningfulGoalsTypesUpdateOperationSupport();


        clientId = ClientId.fromLong(RandomNumberUtils.nextPositiveLong());
        uid = RandomNumberUtils.nextPositiveLong();
        int shard = 1;
        campaignId = RandomNumberUtils.nextPositiveLong();
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
    }

    @Test
    public void removeMetrikaValueSource_NotCrrStrategy() {
        campaign = createCampaign(StrategyName.AUTOBUDGET_AVG_CPA);

        var appliedChanges = getAppliedChanges();
        support.onAppliedChangesValidated(updateParameters, List.of(appliedChanges));
        assertThat(appliedChanges.getModel().getMeaningfulGoals().get(0).getIsMetrikaSourceOfValue()).isNull();
    }

    @Test
    public void doNotRemoveMetrikaValueSource_CrrStrategy() {
        campaign = createCampaign(StrategyName.AUTOBUDGET_CRR);

        var appliedChanges = getAppliedChanges();
        support.onAppliedChangesValidated(updateParameters, List.of(appliedChanges));
        assertThat(appliedChanges.getModel().getMeaningfulGoals().get(0).getIsMetrikaSourceOfValue()).isTrue();

    }

    private AppliedChanges<CampaignWithMeaningfulGoalsWithRequiredFields> getAppliedChanges() {
        List<MeaningfulGoal> goals = getMeaningfulGoals();

        ModelChanges<CampaignWithMeaningfulGoalsWithRequiredFields> modelChanges =
                ModelChanges.build(campaign, CampaignWithMeaningfulGoalsWithRequiredFields.MEANINGFUL_GOALS, goals);

        return modelChanges.applyTo(campaign);
    }

    private List<MeaningfulGoal> getMeaningfulGoals() {
        List<MeaningfulGoal> goals = List.of(
                new MeaningfulGoal()
                        .withGoalId(123L)
                        .withConversionValue(BigDecimal.TEN)
                        .withIsMetrikaSourceOfValue(true));
        return goals;
    }


    private CampaignWithMeaningfulGoalsWithRequiredFields createCampaign(StrategyName strategyName) {
        return ((CampaignWithMeaningfulGoalsWithRequiredFields) TestCampaigns.newCampaignByCampaignType(CampaignType.TEXT))
                .withClientId(clientId.asLong())
                .withName("campaign")
                .withType(CampaignType.valueOf(CampaignType.TEXT.name()))
                .withId(campaignId)
                .withMeaningfulGoals(getMeaningfulGoals())
                .withStrategy((DbStrategy) new DbStrategy().withStrategyName(strategyName))
                .withUid(uid);
    }
}
