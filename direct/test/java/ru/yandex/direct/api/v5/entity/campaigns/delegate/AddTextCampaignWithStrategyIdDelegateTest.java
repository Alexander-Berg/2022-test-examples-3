package ru.yandex.direct.api.v5.entity.campaigns.delegate;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import com.yandex.direct.api.v5.campaigns.AddRequest;
import com.yandex.direct.api.v5.campaigns.AddResponse;
import com.yandex.direct.api.v5.campaigns.CampaignAddItem;
import com.yandex.direct.api.v5.campaigns.PriorityGoalsArray;
import com.yandex.direct.api.v5.campaigns.PriorityGoalsItem;
import com.yandex.direct.api.v5.campaigns.StrategyAverageCpaAdd;
import com.yandex.direct.api.v5.campaigns.TextCampaignAddItem;
import com.yandex.direct.api.v5.campaigns.TextCampaignNetworkStrategyAdd;
import com.yandex.direct.api.v5.campaigns.TextCampaignNetworkStrategyTypeEnum;
import com.yandex.direct.api.v5.campaigns.TextCampaignSearchStrategyAdd;
import com.yandex.direct.api.v5.campaigns.TextCampaignSearchStrategyTypeEnum;
import com.yandex.direct.api.v5.campaigns.TextCampaignStrategyAdd;
import com.yandex.direct.api.v5.general.ArrayOfInteger;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.api.v5.testing.configuration.Api5Test;
import ru.yandex.direct.core.entity.strategy.model.BaseStrategy;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.testing.data.strategy.TestAutobudgetAvgClick.autobudgetAvgClick;
import static ru.yandex.direct.core.testing.data.strategy.TestAutobudgetAvgCpaStrategy.autobudgetAvgCpa;
import static ru.yandex.direct.core.testing.data.strategy.TestAutobudgetCrrStrategy.autobudgetCrr;

@Api5Test
@RunWith(Parameterized.class)
public class AddTextCampaignWithStrategyIdDelegateTest extends AddCampaignWithStrategyIdDelegateBaseTest {

    @Parameterized.Parameter
    public TextCampaignSearchStrategyTypeEnum strategyType;

    @Parameterized.Parameter(1)
    public BaseStrategy strategy;

    @Parameterized.Parameters(name = "RequestSource = {0}")
    public static Collection<Object[]> params() {
        return Arrays.asList(new Object[][]{
                {
                        TextCampaignSearchStrategyTypeEnum.AVERAGE_CPC,
                        autobudgetAvgClick()
                },
                {
                        TextCampaignSearchStrategyTypeEnum.AVERAGE_CPA,
                        autobudgetAvgCpa().withMetrikaCounters(List.of(COUNTER_ID))
                                .withGoalId(VALID_GOAL_ID)
                },
                {
                        TextCampaignSearchStrategyTypeEnum.AVERAGE_CRR,
                        autobudgetCrr().withMetrikaCounters(List.of(COUNTER_ID))
                                .withGoalId(VALID_GOAL_ID)
                }
        });

    }


    @Test
    public void addTextCampaignWithStrategyId() {
        Long strategyId = createStrategyAndGetId(strategy);
        TextCampaignAddItem textCampaignAddItem = new TextCampaignAddItem();
        textCampaignAddItem.withStrategyId(strategyId)
                .withCounterIds(new ArrayOfInteger().withItems(COUNTER_ID.intValue()))
                .withPriorityGoals(new PriorityGoalsArray().withItems(new PriorityGoalsItem()
                        .withGoalId(VALID_GOAL_ID)
                        .withValue(100000000)))
                .withBiddingStrategy(new TextCampaignStrategyAdd()
                        .withNetwork(new TextCampaignNetworkStrategyAdd()
                                .withBiddingStrategyType(TextCampaignNetworkStrategyTypeEnum.SERVING_OFF))
                        .withSearch(new TextCampaignSearchStrategyAdd()
                                .withBiddingStrategyType(TextCampaignSearchStrategyTypeEnum.AVERAGE_CPA)
                                .withAverageCpa(new StrategyAverageCpaAdd().withAverageCpa(1000000L).withGoalId(0)))
                );
        AddRequest request = getAddRequest(NAME, textCampaignAddItem);
        AddResponse response = genericApiService.doAction(delegate, request);

        assertThat(response.getAddResults().get(0).getErrors()).isEmpty();
    }

    public static AddRequest getAddRequest(String name, TextCampaignAddItem textCampaignAddItem) {
        var campaignAddItem = new CampaignAddItem()
                .withName(name)
                .withStartDate(DATETIME_FORMATTER.format(LocalDateTime.now()))
                .withTextCampaign(textCampaignAddItem);

        return new AddRequest().withCampaigns(campaignAddItem);
    }

}
