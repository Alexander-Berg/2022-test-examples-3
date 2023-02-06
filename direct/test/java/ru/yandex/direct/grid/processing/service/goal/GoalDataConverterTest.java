package ru.yandex.direct.grid.processing.service.goal;

import java.math.BigDecimal;
import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.Test;

import ru.yandex.direct.grid.processing.model.goal.GdCpaSource;
import ru.yandex.direct.grid.processing.model.goal.GdGoalsRecommendedCostPerActionByCampaignId;
import ru.yandex.direct.grid.processing.model.goal.GdRecommendedGoalCostPerAction;
import ru.yandex.direct.test.utils.RandomNumberUtils;

import static org.assertj.core.api.Assertions.assertThat;

@ParametersAreNonnullByDefault
public class GoalDataConverterTest {

    @Test
    public void costPerActionByCampaignIdToString() {
        Long campaignIdFirst = RandomNumberUtils.nextPositiveLong();
        Long campaignIdSecond = RandomNumberUtils.nextPositiveLong();
        Long goalIdFirst = RandomNumberUtils.nextPositiveLong();
        Long goalIdSecond = RandomNumberUtils.nextPositiveLong();
        BigDecimal cpaFirst = RandomNumberUtils.nextPositiveBigDecimal();
        BigDecimal cpaSecond = RandomNumberUtils.nextPositiveBigDecimal();
        List<GdGoalsRecommendedCostPerActionByCampaignId> costPerActionByCampaignIdList = List.of(
                new GdGoalsRecommendedCostPerActionByCampaignId()
                        .withCampaignId(campaignIdFirst)
                        .withRecommendedGoalsCostPerAction(List.of(
                                new GdRecommendedGoalCostPerAction()
                                        .withId(goalIdFirst)
                                        .withCostPerActionSource(GdCpaSource.CAMPAIGN)
                                        .withCostPerAction(cpaFirst),
                                new GdRecommendedGoalCostPerAction()
                                        .withId(goalIdSecond)
                                        .withCostPerActionSource(GdCpaSource.CAMPAIGN)
                                        .withCostPerAction(cpaSecond)
                        )),
                new GdGoalsRecommendedCostPerActionByCampaignId()
                        .withCampaignId(campaignIdSecond)
                        .withRecommendedGoalsCostPerAction(List.of(
                                new GdRecommendedGoalCostPerAction()
                                        .withId(goalIdFirst)
                                        .withCostPerActionSource(GdCpaSource.CAMPAIGN)
                                        .withCostPerAction(cpaFirst)
                        ))
        );

        String result = GoalDataConverter.costPerActionByCampaignIdToString(costPerActionByCampaignIdList);
        assertThat(result).isEqualTo("[{\"campaign_id\":" + campaignIdFirst + "," +
                "\"recommended_cpa\":[{\"goal_id\":" + goalIdFirst + ",\"source\":\"CAMPAIGN\",\"cpa\":" + cpaFirst +
                "},{\"goal_id\":" + goalIdSecond + ",\"source\":\"CAMPAIGN\",\"cpa\":" + cpaSecond + "}]}," +
                "{\"campaign_id\":" + campaignIdSecond + ",\"recommended_cpa\":[{\"goal_id\":" + goalIdFirst + "," +
                "\"source\":\"CAMPAIGN\",\"cpa\":" + cpaFirst + "}]}]");
    }
}
