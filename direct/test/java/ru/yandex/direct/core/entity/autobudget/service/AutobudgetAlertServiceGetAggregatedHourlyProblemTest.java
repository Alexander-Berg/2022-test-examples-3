package ru.yandex.direct.core.entity.autobudget.service;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.core.entity.autobudget.model.AutobudgetAggregatedHourlyProblem;
import ru.yandex.direct.core.entity.autobudget.model.AutobudgetHourlyProblem;
import ru.yandex.direct.core.entity.autobudget.model.HourlyAutobudgetAlert;
import ru.yandex.direct.core.entity.autobudget.repository.AutobudgetCpaAlertRepository;
import ru.yandex.direct.core.entity.autobudget.repository.AutobudgetHourlyAlertRepository;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.sharding.ShardHelper;

import static java.util.Collections.singleton;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.core.testing.data.TestAutobudgetAlerts.defaultActiveHourlyAlert;

@RunWith(Parameterized.class)
public class AutobudgetAlertServiceGetAggregatedHourlyProblemTest {
    private static final ClientId CLIENT_ID = ClientId.fromLong(1234L);
    private static final Long CAMPAIGN_ID = 123L;
    private static final int SHARD = 1;

    @Parameterized.Parameter
    public EnumSet<AutobudgetHourlyProblem> problems;

    @Parameterized.Parameter(1)
    public AutobudgetAggregatedHourlyProblem expectedAggregatedProblem;

    private AutobudgetAlertService autobudgetAlertService;
    private AutobudgetHourlyAlertRepository autobudgetHourlyAlertRepository;

    @Parameterized.Parameters(name = "{0}")
    public static Iterable<Object[]> data() {
        return Arrays.asList(
                // null в prepare() транслируется в отсутствие алертов у кампании
                new Object[]{null,
                        AutobudgetAggregatedHourlyProblem.NO_PROBLEM},

                new Object[]{EnumSet.noneOf(AutobudgetHourlyProblem.class),
                        AutobudgetAggregatedHourlyProblem.NO_PROBLEM},
                // всевозможные варианты автобюджетных проблем
                new Object[]{EnumSet.of(AutobudgetHourlyProblem.IN_ROTATION),
                        AutobudgetAggregatedHourlyProblem.NO_PROBLEM},
                new Object[]{EnumSet.of(AutobudgetHourlyProblem.MAX_BID_REACHED),
                        AutobudgetAggregatedHourlyProblem.MAX_BID_REACHED},
                new Object[]{EnumSet.of(AutobudgetHourlyProblem.MARGINAL_PRICE_REACHED),
                        AutobudgetAggregatedHourlyProblem.MAX_BID_REACHED},
                new Object[]{EnumSet.of(AutobudgetHourlyProblem.UPPER_POSITIONS_REACHED),
                        AutobudgetAggregatedHourlyProblem.UPPER_POSITIONS_REACHED},
                new Object[]{EnumSet.of(AutobudgetHourlyProblem.ENGINE_MIN_COST_LIMITED),
                        AutobudgetAggregatedHourlyProblem.ENGINE_MIN_COST_LIMITED},
                new Object[]{EnumSet.of(AutobudgetHourlyProblem.LIMITED_BY_BALANCE),
                        AutobudgetAggregatedHourlyProblem.NO_PROBLEM},
                new Object[]{EnumSet.of(AutobudgetHourlyProblem.NO_BANNERS),
                        AutobudgetAggregatedHourlyProblem.NO_PROBLEM},
                new Object[]{EnumSet.of(AutobudgetHourlyProblem.LIMIT_BY_AVG_COST),
                        AutobudgetAggregatedHourlyProblem.NO_PROBLEM},
                new Object[]{EnumSet.of(AutobudgetHourlyProblem.WALLET_DAILY_BUDGET_REACHED),
                        AutobudgetAggregatedHourlyProblem.WALLET_DAY_BUDGET_REACHED},

                // сочетания проблем, где одна перекрывает другие
                new Object[]{EnumSet.of(
                        AutobudgetHourlyProblem.WALLET_DAILY_BUDGET_REACHED,
                        AutobudgetHourlyProblem.MAX_BID_REACHED),
                        AutobudgetAggregatedHourlyProblem.WALLET_DAY_BUDGET_REACHED},
                new Object[]{EnumSet.of(
                        AutobudgetHourlyProblem.ENGINE_MIN_COST_LIMITED,
                        AutobudgetHourlyProblem.MAX_BID_REACHED),
                        AutobudgetAggregatedHourlyProblem.ENGINE_MIN_COST_LIMITED},
                new Object[]{EnumSet.of(
                        AutobudgetHourlyProblem.UPPER_POSITIONS_REACHED,
                        AutobudgetHourlyProblem.MAX_BID_REACHED),
                        AutobudgetAggregatedHourlyProblem.UPPER_POSITIONS_REACHED}
        );
    }

    @Before
    public void prepare() {
        AutobudgetCpaAlertRepository autobudgetCpaAlertRepository = mock(AutobudgetCpaAlertRepository.class);
        ShardHelper shardHelper = mock(ShardHelper.class);
        autobudgetHourlyAlertRepository = mock(AutobudgetHourlyAlertRepository.class);
        autobudgetAlertService = new AutobudgetAlertService(autobudgetHourlyAlertRepository, shardHelper,
                autobudgetCpaAlertRepository);

        when(shardHelper.getShardByClientId(CLIENT_ID)).thenReturn(SHARD);

        if (problems != null) {
            HourlyAutobudgetAlert alert = defaultActiveHourlyAlert(CAMPAIGN_ID).withProblems(problems);
            when(autobudgetHourlyAlertRepository.getAlerts(anyInt(), any()))
                    .thenReturn(ImmutableMap.of(CAMPAIGN_ID, alert));
        } else {
            when(autobudgetHourlyAlertRepository.getAlerts(anyInt(), any()))
                    .thenReturn(new HashMap<>());
        }

        autobudgetAlertService = new AutobudgetAlertService(autobudgetHourlyAlertRepository, shardHelper, autobudgetCpaAlertRepository);
    }

    @Test
    public void getAggregatedHourlyProblem_CorrectResult() {
        Map<Long, AutobudgetAggregatedHourlyProblem> result =
                autobudgetAlertService.getAggregatedHourlyProblems(CLIENT_ID, singleton(CAMPAIGN_ID));
        assertThat("Агрегированная автобюджетная проблема не совпадает с ожидаемой", result.get(CAMPAIGN_ID),
                is(expectedAggregatedProblem));
    }
}
