package ru.yandex.direct.core.entity.adgroup.repository.typesupport;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.adgroup.model.PerformanceAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.StatusBLGenerated;
import ru.yandex.direct.core.entity.adgroup.repository.AdGroupRepository;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.FeedInfo;
import ru.yandex.direct.core.testing.info.PerformanceAdGroupInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.model.AppliedChanges;
import ru.yandex.direct.model.ModelChanges;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;
import static ru.yandex.direct.dbschema.ppc.Tables.ADGROUPS_PERFORMANCE;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class PerformanceAdGroupSupportTest {

    @Autowired
    private AdGroupRepository adGroupRepository;
    @Autowired
    private Steps steps;
    @Autowired
    private DslContextProvider dslContextProvider;
    @Autowired
    PerformanceAdGroupSupport performanceAdGroupSupport;

    @Test
    public void updateAdGroups_success() {
        PerformanceAdGroupInfo groupInfo = steps.adGroupSteps().createDefaultPerformanceAdGroup();
        PerformanceAdGroup performanceAdGroup = groupInfo.getPerformanceAdGroup();
        Integer shard = groupInfo.getShard();
        Long adGroupId = groupInfo.getAdGroupId();
        ClientId clientId = groupInfo.getClientId();
        FeedInfo newFeedInfo = steps.feedSteps().createDefaultFeed();
        Long newFeedId = newFeedInfo.getFeedId();

        PerformanceAdGroup groupWithChangedFields = new PerformanceAdGroup()
                .withFeedId(newFeedId)
                .withStatusBLGenerated(StatusBLGenerated.PROCESSING)
                .withFieldToUseAsName("The changed name")
                .withFieldToUseAsBody("The changed body");

        AppliedChanges<PerformanceAdGroup> changes =
                new ModelChanges<>(adGroupId, PerformanceAdGroup.class)
                        .process(groupWithChangedFields.getFeedId(), PerformanceAdGroup.FEED_ID)
                        .process(groupWithChangedFields.getStatusBLGenerated(), PerformanceAdGroup.STATUS_B_L_GENERATED)
                        .process(groupWithChangedFields.getFieldToUseAsName(), PerformanceAdGroup.FIELD_TO_USE_AS_NAME)
                        .process(groupWithChangedFields.getFieldToUseAsBody(), PerformanceAdGroup.FIELD_TO_USE_AS_BODY)
                        .applyTo(performanceAdGroup);

        performanceAdGroupSupport.updateAdGroups(singletonList(changes), clientId, dslContextProvider.ppc(shard).dsl());

        AdGroup actual = adGroupRepository.getAdGroups(shard, singletonList(adGroupId)).get(0);

        assertThat(actual)
                .is(matchedBy(beanDiffer(groupWithChangedFields)
                        .useCompareStrategy(onlyExpectedFields())));
    }

    @Test
    public void getIncompleteAdGroupIdOk_success() {
        PerformanceAdGroupInfo groupInfo = steps.adGroupSteps().createDefaultPerformanceAdGroup();
        Integer shard = groupInfo.getShard();
        Long adGroupId = groupInfo.getAdGroupId();

        dslContextProvider.ppc(shard).deleteFrom(ADGROUPS_PERFORMANCE)
                .where(ADGROUPS_PERFORMANCE.PID.eq(adGroupId))
                .execute();

        AdGroup gotAdGroup = adGroupRepository.getAdGroups(shard, singletonList(adGroupId)).get(0);

        assertThat(gotAdGroup.getId()).isEqualTo(groupInfo.getAdGroupId());
    }

}
