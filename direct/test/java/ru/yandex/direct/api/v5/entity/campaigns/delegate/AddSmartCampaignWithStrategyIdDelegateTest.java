package ru.yandex.direct.api.v5.entity.campaigns.delegate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import com.yandex.direct.api.v5.campaigns.AddRequest;
import com.yandex.direct.api.v5.campaigns.AddResponse;
import com.yandex.direct.api.v5.campaigns.CampaignAddItem;
import com.yandex.direct.api.v5.campaigns.PriorityGoalsArray;
import com.yandex.direct.api.v5.campaigns.PriorityGoalsItem;
import com.yandex.direct.api.v5.campaigns.SmartCampaignAddItem;
import com.yandex.direct.api.v5.campaigns.SmartCampaignNetworkStrategyAdd;
import com.yandex.direct.api.v5.campaigns.SmartCampaignNetworkStrategyTypeEnum;
import com.yandex.direct.api.v5.campaigns.SmartCampaignSearchStrategyAdd;
import com.yandex.direct.api.v5.campaigns.SmartCampaignSearchStrategyTypeEnum;
import com.yandex.direct.api.v5.campaigns.SmartCampaignStrategyAdd;
import com.yandex.direct.api.v5.campaigns.StrategyAverageCpaPerCampaignAdd;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.api.v5.testing.configuration.Api5Test;
import ru.yandex.direct.core.entity.strategy.model.BaseStrategy;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.testing.data.strategy.TestAutobudgetAvgCpaPerCampStrategy.autobudgetAvgCpaPerCamp;
import static ru.yandex.direct.core.testing.data.strategy.TestAutobudgetAvgCpaPerFilterStrategy.autobudgetAvgCpaPerFilter;
import static ru.yandex.direct.core.testing.data.strategy.TestAutobudgetAvgCpcPerCampStrategy.autobudgetAvgCpcPerCamp;
import static ru.yandex.direct.core.testing.data.strategy.TestAutobudgetAvgCpcPerFilterStrategy.autobudgetAvgCpcPerFilter;

@Api5Test
@RunWith(Parameterized.class)
public class AddSmartCampaignWithStrategyIdDelegateTest extends AddCampaignWithStrategyIdDelegateBaseTest {
    @Parameterized.Parameter
    public SmartCampaignSearchStrategyTypeEnum strategyType;

    @Parameterized.Parameter(1)
    public BaseStrategy strategy;

    @Parameterized.Parameters(name = "RequestSource = {0}")
    public static Collection<Object[]> params() {
        return Arrays.asList(new Object[][]{
                {
                        SmartCampaignSearchStrategyTypeEnum.AVERAGE_CPC_PER_CAMPAIGN,
                        autobudgetAvgCpcPerCamp()
                                .withAvgBid(BigDecimal.ONE)
                                .withMetrikaCounters(List.of(COUNTER_ID))
                },
                {
                        SmartCampaignSearchStrategyTypeEnum.AVERAGE_CPC_PER_CAMPAIGN,
                        autobudgetAvgCpcPerFilter().withMetrikaCounters(List.of(COUNTER_ID))
                },
                {
                        SmartCampaignSearchStrategyTypeEnum.PAY_FOR_CONVERSION_PER_CAMPAIGN,
                        autobudgetAvgCpaPerCamp().withMetrikaCounters(List.of(COUNTER_ID))
                                .withGoalId(VALID_GOAL_ID)
                },
                {
                        SmartCampaignSearchStrategyTypeEnum.PAY_FOR_CONVERSION_PER_FILTER,
                        autobudgetAvgCpaPerFilter().withMetrikaCounters(List.of(COUNTER_ID))
                                .withGoalId(VALID_GOAL_ID)
                }
        });

    }

    @Test
    public void addSmartCampaignWithStrategyId() {
        Long strategyId = createStrategyAndGetId(strategy);
        SmartCampaignAddItem smartCampaignAddItem = new SmartCampaignAddItem();
        smartCampaignAddItem.withStrategyId(strategyId)
                .withCounterId(COUNTER_ID)
                .withPriorityGoals(new PriorityGoalsArray().withItems(new PriorityGoalsItem()
                        .withGoalId(VALID_GOAL_ID)
                        .withValue(100000000)))
                .withBiddingStrategy(new SmartCampaignStrategyAdd()
                        .withNetwork(new SmartCampaignNetworkStrategyAdd()
                                .withBiddingStrategyType(SmartCampaignNetworkStrategyTypeEnum.SERVING_OFF))
                        .withSearch(new SmartCampaignSearchStrategyAdd()
                                .withBiddingStrategyType(SmartCampaignSearchStrategyTypeEnum.AVERAGE_CPA_PER_CAMPAIGN)
                                .withAverageCpaPerCampaign(new StrategyAverageCpaPerCampaignAdd().withAverageCpa(1000000L).withGoalId(0)))
                );
        AddRequest request = getAddRequest(NAME, smartCampaignAddItem);
        AddResponse response = genericApiService.doAction(delegate, request);

        assertThat(response.getAddResults().get(0).getErrors()).isEmpty();
    }

    public static AddRequest getAddRequest(String name, SmartCampaignAddItem smartCampaignAddItem) {
        var campaignAddItem = new CampaignAddItem()
                .withName(name)
                .withStartDate(DATETIME_FORMATTER.format(LocalDateTime.now()))
                .withSmartCampaign(smartCampaignAddItem);

        return new AddRequest().withCampaigns(campaignAddItem);
    }
}
