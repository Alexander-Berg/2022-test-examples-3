package ru.yandex.direct.core.entity.campaign.service.type.update;

import java.util.List;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.naming.TestCaseName;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import ru.yandex.direct.core.entity.campaign.model.DbStrategy;
import ru.yandex.direct.core.entity.campaign.model.MobileContentCampaign;
import ru.yandex.direct.core.entity.campaign.model.MobileContentCampaignWithCustomStrategy;
import ru.yandex.direct.core.entity.campaign.service.type.update.container.RestrictedCampaignsUpdateOperationContainer;
import ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.model.AppliedChanges;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.test.utils.RandomNumberUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.testing.data.TestCampaigns.averageCpiStrategy;
import static ru.yandex.direct.core.testing.data.TestCampaigns.defaultAutobudgetStrategy;

@CoreTest
@RunWith(JUnitParamsRunner.class)
public class MobileContentCampaignWithCustomStrategyUpdateOperationSupportTest {
    private static final long GOAL_ID = 55L;

    private static long campaignId;
    private static MobileContentCampaignWithCustomStrategyUpdateOperationSupport testingSupport;
    private static RestrictedCampaignsUpdateOperationContainer updateContainer;

    @BeforeClass
    public static void beforeClass() {
        updateContainer = RestrictedCampaignsUpdateOperationContainer.create(RandomNumberUtils.nextPositiveInteger(),
                RandomNumberUtils.nextPositiveLong(), ClientId.fromLong(RandomNumberUtils.nextPositiveLong()),
                RandomNumberUtils.nextPositiveLong(), RandomNumberUtils.nextPositiveLong());
        campaignId = RandomNumberUtils.nextPositiveLong();

        testingSupport = new MobileContentCampaignWithCustomStrategyUpdateOperationSupport();
    }

    public static Object[][] parametersOfChangingToCpiStrategy() {
        return new Object[][]{
                {CampaignConstants.DEFAULT_CPI_GOAL_ID, null},
                {GOAL_ID, GOAL_ID},
                {null, null},
        };
    }

    @Test
    @TestCaseName("отправленная цель {0}, ожидаем цель {1}")
    @Parameters(method = "parametersOfChangingToCpiStrategy")
    public void updateToCpiStrategy(Long goalId, Long expectedGoalId) {
        MobileContentCampaignWithCustomStrategy oldCampaign = new MobileContentCampaign()
                .withId(campaignId)
                .withStrategy(defaultAutobudgetStrategy());

        AppliedChanges<MobileContentCampaignWithCustomStrategy> appliedChanges =
                getModelChanges(campaignId, goalId, oldCampaign);
        testingSupport.onChangesApplied(updateContainer, List.of(appliedChanges));

        assertThat(appliedChanges.getModel().getStrategy().getStrategyData().getGoalId())
                .as("цель в кампании")
                .isEqualTo(expectedGoalId);
    }

    private AppliedChanges<MobileContentCampaignWithCustomStrategy> getModelChanges(
            Long campaignId,
            Long newGoalId,
            MobileContentCampaignWithCustomStrategy oldCampaign) {
        ModelChanges<MobileContentCampaignWithCustomStrategy> campaignWithCustomStrategyModelChanges =
                new ModelChanges<>(campaignId, MobileContentCampaignWithCustomStrategy.class);
        DbStrategy newStrategy = averageCpiStrategy(newGoalId);
        campaignWithCustomStrategyModelChanges.process(newStrategy, MobileContentCampaignWithCustomStrategy.STRATEGY);
        return campaignWithCustomStrategyModelChanges.applyTo(oldCampaign);
    }
}
