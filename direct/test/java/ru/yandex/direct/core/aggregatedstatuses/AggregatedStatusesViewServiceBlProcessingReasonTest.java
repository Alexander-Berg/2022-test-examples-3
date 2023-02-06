package ru.yandex.direct.core.aggregatedstatuses;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import ru.yandex.direct.core.aggregatedstatuses.repository.AggregatedStatusesRepository;
import ru.yandex.direct.core.entity.aggregatedstatuses.GdSelfStatusEnum;
import ru.yandex.direct.core.entity.aggregatedstatuses.GdSelfStatusReason;
import ru.yandex.direct.core.entity.aggregatedstatuses.ad.AdStatesEnum;
import ru.yandex.direct.core.entity.aggregatedstatuses.adgroup.AdGroupCounters;
import ru.yandex.direct.core.entity.aggregatedstatuses.adgroup.AggregatedStatusAdGroupData;
import ru.yandex.direct.core.entity.aggregatedstatuses.keyword.KeywordStatesEnum;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.PerformanceAdGroupInfo;
import ru.yandex.direct.core.testing.steps.Steps;

import static ru.yandex.direct.core.entity.aggregatedstatuses.GdSelfStatusEnum.RUN_WARN;
import static ru.yandex.direct.core.entity.aggregatedstatuses.GdSelfStatusEnum.STOP_WARN;
import static ru.yandex.direct.core.entity.aggregatedstatuses.GdSelfStatusReason.ADGROUP_BL_PROCESSING;
import static ru.yandex.direct.core.entity.aggregatedstatuses.GdSelfStatusReason.ADGROUP_BL_PROCESSING_WITH_OLD_VERSION_SHOWN;
import static ru.yandex.direct.core.entity.aggregatedstatuses.GdSelfStatusReason.ADGROUP_RARELY_SERVED;

@CoreTest
@RunWith(Parameterized.class)
public class AggregatedStatusesViewServiceBlProcessingReasonTest {

    private static final AdGroupCounters ADGROUP_COUNTERS = new AdGroupCounters(2, 2, 1,
            Map.of(GdSelfStatusEnum.RUN_OK, 1, STOP_WARN, 1),
            Map.of(AdStatesEnum.MODERATION, 1),
            Map.of(STOP_WARN, 1, GdSelfStatusEnum.STOP_OK, 1),
            Map.of(KeywordStatesEnum.REJECTED, 1, KeywordStatesEnum.SUSPENDED, 1),
            Map.of(), Map.of());

    @ClassRule
    public static final SpringClassRule SPRING_CLASS_RULE = new SpringClassRule();
    @Rule
    public SpringMethodRule springMethodRule = new SpringMethodRule();

    @Autowired
    private AggregatedStatusesViewService aggregatedStatusesViewService;
    @Autowired
    private AggregatedStatusesRepository aggregatedStatusesRepository;
    @Autowired
    private Steps steps;

    private PerformanceAdGroupInfo adGroupInfo;
    private int shard;
    private long groupId;

    @Parameter
    public String testDescription;
    @Parameter(1)
    public AdGroupCounters counters;
    @Parameter(2)
    public GdSelfStatusReason reason;
    @Parameter(3)
    public boolean expectCounters;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {"Для статуса с ADGROUP_BL_PROCESSING_WITH_OLD_VERSION_SHOWN reason -> counters не возвращаем",
                        ADGROUP_COUNTERS, ADGROUP_BL_PROCESSING_WITH_OLD_VERSION_SHOWN, false},
                {"Для статуса с ADGROUP_BL_PROCESSING reason -> counters не возвращаем",
                        ADGROUP_COUNTERS, ADGROUP_BL_PROCESSING, false},
                {"Для статуса без counters и с ADGROUP_BL_PROCESSING reason -> counters не возвращаем",
                        null, ADGROUP_BL_PROCESSING, false},
                {"Для статуса не с ADGROUP_BL_PROCESSING(_WITH_OLD_VERSION_SHOWN) reason -> возвращаем counters",
                        ADGROUP_COUNTERS, ADGROUP_RARELY_SERVED, true},
        });
    }

    @Before
    public void before() {
        ClientInfo clientInfo = steps.clientSteps().createDefaultClient();
        shard = clientInfo.getShard();
        adGroupInfo = steps.adGroupSteps().createDefaultPerformanceAdGroup(clientInfo);
        groupId = adGroupInfo.getAdGroupId();
    }

    @Test
    public void getAdGroupStatusesByIds() {
        AggregatedStatusAdGroupData adGroupStatus = new AggregatedStatusAdGroupData(
                null, counters, RUN_WARN, reason);
        aggregatedStatusesRepository.updateAdGroups(shard, null, Map.of(adGroupInfo.getAdGroupId(), adGroupStatus));

        Map<Long, AggregatedStatusAdGroupData> groupIdToAggrData =
                aggregatedStatusesViewService.getAdGroupStatusesByIds(shard, Set.of(groupId));

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(groupIdToAggrData).as("group id").containsKey(groupId);
            if (expectCounters) {
                softly.assertThat(groupIdToAggrData.get(groupId).getCounters()).as("counters").isNotNull();
            } else {
                softly.assertThat(groupIdToAggrData.get(groupId).getCounters()).as("counters").isNull();
            }
        });
    }
}
