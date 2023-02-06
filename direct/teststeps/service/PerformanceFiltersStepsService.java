package ru.yandex.direct.teststeps.service;

import javax.annotation.ParametersAreNonnullByDefault;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ru.yandex.direct.core.entity.performancefilter.model.PerformanceFilter;
import ru.yandex.direct.core.testing.steps.PerformanceFiltersSteps;
import ru.yandex.direct.dbutil.sharding.ShardHelper;

import static ru.yandex.direct.core.testing.data.TestPerformanceFilters.defaultPerformanceFilter;

@Service
@ParametersAreNonnullByDefault
public class PerformanceFiltersStepsService {

    private final PerformanceFiltersSteps performanceFiltersSteps;
    private final ShardHelper shardHelper;

    @Autowired
    public PerformanceFiltersStepsService(PerformanceFiltersSteps performanceFiltersSteps,
                                          ShardHelper shardHelper) {
        this.performanceFiltersSteps = performanceFiltersSteps;
        this.shardHelper = shardHelper;
    }


    public Long createPerformanceFilter(String login, Long adGroupId, String name) {
        int shard = shardHelper.getShardByLoginStrictly(login);

        PerformanceFilter filter = defaultPerformanceFilter(adGroupId, null)
                .withName(name);

        return performanceFiltersSteps.addPerformanceFilter(shard, filter);
    }
}
