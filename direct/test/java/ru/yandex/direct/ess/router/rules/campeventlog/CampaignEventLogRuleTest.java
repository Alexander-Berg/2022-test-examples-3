package ru.yandex.direct.ess.router.rules.campeventlog;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Stream;

import org.jooq.Named;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.direct.binlog.model.BinlogEvent;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.ess.logicobjects.campeventlog.CampaignEventLogLogicObject;
import ru.yandex.direct.ess.router.configuration.TestConfiguration;
import ru.yandex.direct.ess.router.testutils.CampaignsTableChange;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static ru.yandex.direct.binlog.model.Operation.UPDATE;
import static ru.yandex.direct.dbschema.ppc.Tables.CAMPAIGNS;
import static ru.yandex.direct.ess.logicobjects.campeventlog.CampaignEventLogLogicObject.CampaignEventType.AVG_CPA_CHANGED;
import static ru.yandex.direct.ess.logicobjects.campeventlog.CampaignEventLogLogicObject.CampaignEventType.DAILY_BUDGET_CHANGED;
import static ru.yandex.direct.ess.logicobjects.campeventlog.CampaignEventLogLogicObject.CampaignEventType.ROI_COEF_CHANGED;
import static ru.yandex.direct.ess.logicobjects.campeventlog.CampaignEventLogLogicObject.CampaignEventType.WEEKLY_BUDGET_CHANGED;
import static ru.yandex.direct.ess.router.testutils.CampaignsTableChange.createCampaignEvent;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = TestConfiguration.class)
class CampaignEventLogRuleTest {
    private static final Long CAMPAIGN_ID = 123L;
    private static final Long CLIENT_ID = 321L;

    @Autowired
    private CampaignEventLogRule rule;

    static Stream<Arguments> params() {
        return Stream.of(
                // Weekly budget
                arguments(CAMPAIGNS.STRATEGY_DATA, format("{\"sum\": %s}", 20), format("{\"sum\": %s}", 10),
                        singletonList(getWeeklyBudgetEventLog(BigDecimal.valueOf(20), BigDecimal.valueOf(10)))),

                arguments(CAMPAIGNS.STRATEGY_DATA, format("{\"sum\": %s}", 15.23), format("{\"sum\": %s}", 16.65),
                        singletonList(getWeeklyBudgetEventLog(BigDecimal.valueOf(15.23), BigDecimal.valueOf(16.65)))),

                arguments(CAMPAIGNS.STRATEGY_DATA, format("{\"sum\": %s}", 15.23), format("{\"sum\": %s}", 15.24),
                        singletonList(getWeeklyBudgetEventLog(BigDecimal.valueOf(15.23), BigDecimal.valueOf(15.24)))),

                arguments(CAMPAIGNS.STRATEGY_DATA, format("{\"sum\": %s}", 15), format("{\"sum\": %s}", 15.52),
                        singletonList(getWeeklyBudgetEventLog(BigDecimal.valueOf(15), BigDecimal.valueOf(15.52)))),

                arguments(CAMPAIGNS.STRATEGY_DATA, format("{\"sum\": %s}", 15), format("{\"sum\": %s}", null),
                        singletonList(getWeeklyBudgetEventLog(BigDecimal.valueOf(15), null))),

                arguments(CAMPAIGNS.STRATEGY_DATA, format("{\"sum\": %s}", null), format("{\"sum\": %s}", 23.65),
                        singletonList(getWeeklyBudgetEventLog(null, BigDecimal.valueOf(23.65)))),

                arguments(CAMPAIGNS.STRATEGY_DATA, format("{\"sum\": %s}", 234), format("{\"sum\": %s}", 234),
                        emptyList()),

                arguments(CAMPAIGNS.STRATEGY_DATA, format("{\"sum\": %s}", 23.45), format("{\"sum\": %s}", 23.45),
                        emptyList()),

                arguments(CAMPAIGNS.STRATEGY_DATA, format("{\"sum\": %s}", null), format("{\"sum\": %s}", null),
                        emptyList()),

                arguments(CAMPAIGNS.STRATEGY_DATA, format("{\"sum\": %s}", 234), null,
                        emptyList()),

                arguments(CAMPAIGNS.STRATEGY_DATA, null, format("{\"sum\": %s}", 234),
                        emptyList()),

                arguments(CAMPAIGNS.STRATEGY_DATA, null, null,
                        emptyList()),

                // Average CPA
                arguments(CAMPAIGNS.STRATEGY_DATA, format("{\"avg_cpa\": %s}", 20), format("{\"avg_cpa\": %s}", 10),
                        singletonList(getAvgCpaEventLog(BigDecimal.valueOf(20), BigDecimal.valueOf(10)))),

                arguments(CAMPAIGNS.STRATEGY_DATA, format("{\"avg_cpa\": %s}", 15.23), format("{\"avg_cpa\": %s}",
                        16.65),
                        singletonList(getAvgCpaEventLog(BigDecimal.valueOf(15.23), BigDecimal.valueOf(16.65)))),

                arguments(CAMPAIGNS.STRATEGY_DATA, format("{\"avg_cpa\": %s}", 15.23), format("{\"avg_cpa\": %s}",
                        15.24),
                        singletonList(getAvgCpaEventLog(BigDecimal.valueOf(15.23), BigDecimal.valueOf(15.24)))),

                arguments(CAMPAIGNS.STRATEGY_DATA, format("{\"avg_cpa\": %s}", 234), format("{\"avg_cpa\": %s}", 234),
                        emptyList()),

                arguments(CAMPAIGNS.STRATEGY_DATA, format("{\"avg_cpa\": %s}", 23.45), format("{\"avg_cpa\": %s}",
                        23.45),
                        emptyList()),

                // ROI
                arguments(CAMPAIGNS.STRATEGY_DATA, format("{\"roi_coef\": %s}", 20), format("{\"roi_coef\": %s}", 10),
                        singletonList(getRoiCoefEventLog(BigDecimal.valueOf(20), BigDecimal.valueOf(10)))),

                arguments(CAMPAIGNS.STRATEGY_DATA, format("{\"roi_coef\": %s}", 15.23), format("{\"roi_coef\": %s}",
                        16.65),
                        singletonList(getRoiCoefEventLog(BigDecimal.valueOf(15.23), BigDecimal.valueOf(16.65)))),

                arguments(CAMPAIGNS.STRATEGY_DATA, format("{\"roi_coef\": %s}", 15.23), format("{\"roi_coef\": %s}",
                        15.24),
                        singletonList(getRoiCoefEventLog(BigDecimal.valueOf(15.23), BigDecimal.valueOf(15.24)))),

                arguments(CAMPAIGNS.STRATEGY_DATA, format("{\"roi_coef\": %s}", 234), format("{\"roi_coef\": %s}", 234),
                        emptyList()),

                arguments(CAMPAIGNS.STRATEGY_DATA, format("{\"roi_coef\": %s}", 23.45), format("{\"roi_coef\": %s}",
                        23.45),
                        emptyList()),

                // Weekly budget + CPA
                arguments(CAMPAIGNS.STRATEGY_DATA, format("{\"sum\": %s, \"avg_cpa\": %s}", 20, 30),
                        format("{\"sum\": %s, \"avg_cpa\": %s}", 40, 50),
                        asList(getWeeklyBudgetEventLog(BigDecimal.valueOf(20), BigDecimal.valueOf(40)),
                                getAvgCpaEventLog(BigDecimal.valueOf(30), BigDecimal.valueOf(50)))),

                arguments(CAMPAIGNS.STRATEGY_DATA, format("{\"sum\": %s, \"avg_cpa\": %s}", 20, 30),
                        format("{\"sum\": %s, \"avg_cpa\": %s}", 20, 50),
                        singletonList(getAvgCpaEventLog(BigDecimal.valueOf(30), BigDecimal.valueOf(50)))),

                arguments(CAMPAIGNS.STRATEGY_DATA, format("{\"sum\": %s, \"avg_cpa\": %s}", 20, 30),
                        format("{\"sum\": %s, \"avg_cpa\": %s}", 40, 30),
                        singletonList(getWeeklyBudgetEventLog(BigDecimal.valueOf(20), BigDecimal.valueOf(40)))),

                // Weekly budget + ROI
                arguments(CAMPAIGNS.STRATEGY_DATA, format("{\"sum\": %s, \"roi_coef\": %s}", 20, 30),
                        format("{\"sum\": %s, \"roi_coef\": %s}", 40, 50),
                        asList(getWeeklyBudgetEventLog(BigDecimal.valueOf(20), BigDecimal.valueOf(40)),
                                getRoiCoefEventLog(BigDecimal.valueOf(30), BigDecimal.valueOf(50)))),

                arguments(CAMPAIGNS.STRATEGY_DATA, format("{\"sum\": %s, \"roi_coef\": %s}", 20, 30),
                        format("{\"sum\": %s, \"roi_coef\": %s}", 20, 50),
                        singletonList(getRoiCoefEventLog(BigDecimal.valueOf(30), BigDecimal.valueOf(50)))),

                arguments(CAMPAIGNS.STRATEGY_DATA, format("{\"sum\": %s, \"roi_coef\": %s}", 20, 30),
                        format("{\"sum\": %s, \"roi_coef\": %s}", 40, 30),
                        singletonList(getWeeklyBudgetEventLog(BigDecimal.valueOf(20), BigDecimal.valueOf(40)))),

                // Daily budget
                arguments(CAMPAIGNS.DAY_BUDGET, BigDecimal.valueOf(20), BigDecimal.valueOf(10),
                        singletonList(getDailyBudgetEventLog(BigDecimal.valueOf(20), BigDecimal.valueOf(10)))),

                arguments(CAMPAIGNS.DAY_BUDGET, BigDecimal.valueOf(15.23), BigDecimal.valueOf(16.65),
                        singletonList(getDailyBudgetEventLog(BigDecimal.valueOf(15.23), BigDecimal.valueOf(16.65)))),

                arguments(CAMPAIGNS.DAY_BUDGET, BigDecimal.valueOf(15.23), BigDecimal.valueOf(15.24),
                        singletonList(getDailyBudgetEventLog(BigDecimal.valueOf(15.23), BigDecimal.valueOf(15.24)))),

                arguments(CAMPAIGNS.DAY_BUDGET, BigDecimal.valueOf(15), BigDecimal.valueOf(15.52),
                        singletonList(getDailyBudgetEventLog(BigDecimal.valueOf(15), BigDecimal.valueOf(15.52)))),

                arguments(CAMPAIGNS.DAY_BUDGET, BigDecimal.valueOf(234), BigDecimal.valueOf(234),
                        emptyList()),

                arguments(CAMPAIGNS.DAY_BUDGET, BigDecimal.valueOf(23.45), BigDecimal.valueOf(23.45),
                        emptyList())
        );
    }

    private static CampaignEventLogLogicObject getWeeklyBudgetEventLog(BigDecimal beforeValue, BigDecimal afterValue) {
        return getEventTemplate()
                .withCampaignEventType(WEEKLY_BUDGET_CHANGED)
                .withBudgetBefore(beforeValue)
                .withBudgetAfter(afterValue);
    }

    private static CampaignEventLogLogicObject getAvgCpaEventLog(BigDecimal beforeValue, BigDecimal afterValue) {
        return getEventTemplate()
                .withCampaignEventType(AVG_CPA_CHANGED)
                .withAvgCpaBefore(beforeValue)
                .withAvgCpaAfter(afterValue);
    }

    private static CampaignEventLogLogicObject getRoiCoefEventLog(BigDecimal beforeValue, BigDecimal afterValue) {
        return getEventTemplate()
                .withCampaignEventType(ROI_COEF_CHANGED)
                .withRoiCoefBefore(beforeValue)
                .withRoiCoefAfter(afterValue);
    }

    private static CampaignEventLogLogicObject getDailyBudgetEventLog(BigDecimal beforeValue, BigDecimal afterValue) {
        return getEventTemplate()
                .withCampaignEventType(DAILY_BUDGET_CHANGED)
                .withBudgetBefore(beforeValue)
                .withBudgetAfter(afterValue);
    }

    private static CampaignEventLogLogicObject getEventTemplate() {
        return new CampaignEventLogLogicObject()
                .withCampaignId(CAMPAIGN_ID)
                .withClientId(ClientId.fromLong(CLIENT_ID));
    }

    @ParameterizedTest(name = "{0} {1} {2}")
    @MethodSource("params")
    void mapBinlogEvent(Named columnName, Object beforeValue, Object afterValue,
                        List<CampaignEventLogLogicObject> expectedResultObjects) {
        CampaignsTableChange campaignsTableChange =
                new CampaignsTableChange().withCid(CAMPAIGN_ID).withClientId(CLIENT_ID);
        campaignsTableChange.addChangedColumn(columnName, beforeValue, afterValue);
        BinlogEvent binlogEvent = createCampaignEvent(singletonList(campaignsTableChange), UPDATE);

        List<CampaignEventLogLogicObject> resultObjects = rule.mapBinlogEvent(binlogEvent);

        assertThat(resultObjects).containsExactly(expectedResultObjects.toArray(CampaignEventLogLogicObject[]::new));
    }
}
