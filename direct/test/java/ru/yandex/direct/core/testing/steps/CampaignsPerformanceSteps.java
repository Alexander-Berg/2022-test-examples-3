package ru.yandex.direct.core.testing.steps;

import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.direct.dbschema.ppc.enums.CampaignsPerformanceNowOptimizingBy;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;

import static ru.yandex.direct.dbschema.ppc.Tables.CAMPAIGNS_PERFORMANCE;

public class CampaignsPerformanceSteps {

    @Autowired
    private DslContextProvider dslContextProvider;

    public CampaignsPerformanceNowOptimizingBy getNowOptimizingByCid(int shard, Long campaignId) {
        return dslContextProvider.ppc(shard)
                .select(CAMPAIGNS_PERFORMANCE.NOW_OPTIMIZING_BY)
                .from(CAMPAIGNS_PERFORMANCE)
                .where(CAMPAIGNS_PERFORMANCE.CID.eq(campaignId))
                .fetchOne(CAMPAIGNS_PERFORMANCE.NOW_OPTIMIZING_BY);
    }
}
