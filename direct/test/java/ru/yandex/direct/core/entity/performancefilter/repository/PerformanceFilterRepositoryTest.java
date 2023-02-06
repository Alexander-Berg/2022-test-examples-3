package ru.yandex.direct.core.entity.performancefilter.repository;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import one.util.streamex.StreamEx;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.performancefilter.model.PerformanceFilter;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.data.TestPerformanceFilters;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;

import static org.assertj.core.api.Assertions.assertThat;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class PerformanceFilterRepositoryTest {

    @Autowired
    private Steps steps;
    @Autowired
    private PerformanceFilterRepository performanceFilterRepository;

    private Long adGroupId;
    private Long performanceFilterId;
    private Long retConditionId;
    private int shard;

    @Before
    public void setUp() {
        ClientInfo clientInfo = steps.clientSteps().createDefaultClient();
        var retCond = steps.retConditionSteps().createDefaultRetCondition(clientInfo);
        retConditionId = retCond.getRetConditionId();
        shard = clientInfo.getShard();

        var adGroupInfo = steps.adGroupSteps().createDefaultPerformanceAdGroup();
        adGroupId = adGroupInfo.getAdGroupId();

        Long feedId = steps.feedSteps().createDefaultFeed().getFeedId();
        PerformanceFilter filter = TestPerformanceFilters.defaultPerformanceFilter(adGroupInfo.getAdGroupId(), feedId);
        filter.setRetCondId(retCond.getRetConditionId());
        performanceFilterId = steps.performanceFilterSteps().addPerformanceFilter(shard, filter);
    }

    @Test
    public void getFilterIdsByAdGroupIds() {
        Map<Long, List<Long>> filtersByAdGroupIds =
                performanceFilterRepository.getFilterIdsByAdGroupIds(shard, List.of(adGroupId));
        assertThat(filtersByAdGroupIds.values().stream()
                .flatMap(StreamEx::of)
                .collect(Collectors.toList())).isEqualTo(List.of(performanceFilterId));
    }

    @Test
    public void getFilterIdsByRetargetingConditionIds() {
        Map<Long, List<Long>> filtersByAdGroupIds = performanceFilterRepository
                .getFilterIdsByRetargetingConditionIds(shard, List.of(retConditionId));
        assertThat(filtersByAdGroupIds.values().stream()
                .flatMap(StreamEx::of)
                .collect(Collectors.toList())).isEqualTo(List.of(performanceFilterId));
    }
}
