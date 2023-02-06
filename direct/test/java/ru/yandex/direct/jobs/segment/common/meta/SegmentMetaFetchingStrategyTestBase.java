package ru.yandex.direct.jobs.segment.common.meta;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.direct.core.entity.adgroup.model.UsersSegment;
import ru.yandex.direct.core.entity.userssegments.repository.UsersSegmentRepository;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.QueryWithoutIndex;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.jobs.base.logdatatransfer.MetaFetchingStrategy;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.testing.steps.ClientSteps.DEFAULT_SHARD;
import static ru.yandex.direct.dbschema.ppc.Tables.VIDEO_SEGMENT_GOALS;

public abstract class SegmentMetaFetchingStrategyTestBase {

    static final LocalDate TODAY = LocalDate.now();

    @Autowired
    Steps steps;

    @Autowired
    UsersSegmentRepository usersSegmentRepository;

    @Autowired
    DslContextProvider dslContextProvider;

    MetaFetchingStrategy<UsersSegment> segmentMetaFetchingStrategy;

    @BeforeEach
    public void before() {
        segmentMetaFetchingStrategy = createStrategyUnderTest();
        clearVideoGoals();
    }

    abstract MetaFetchingStrategy<UsersSegment> createStrategyUnderTest();

    @Test
    public void returnsEmptyListWhenNoGoalsExist() {
        List<UsersSegment> videoGoals = segmentMetaFetchingStrategy.fetch(DEFAULT_SHARD, 10);
        assertThat(videoGoals).isEmpty();
    }

    @Test
    public void dontFetchAnotherAdGroupTypes() {
        AdGroupInfo adGroupInfo = steps.adGroupSteps().createActiveCpmAudioAdGroup();
        UsersSegment videoGoal = createReadyToBeFetchedGoal(adGroupInfo, TODAY);
        usersSegmentRepository.addSegments(adGroupInfo.getShard(), singletonList(videoGoal));

        List<UsersSegment> videoGoals = segmentMetaFetchingStrategy.fetch(DEFAULT_SHARD, 10);
        assertThat(videoGoals).isEmpty();
    }

    @Test
    public void returnsOneGoalWhenOneGoalExists() {
        addGoal();
        List<UsersSegment> videoGoals = segmentMetaFetchingStrategy.fetch(DEFAULT_SHARD, 10);
        assertThat(videoGoals).hasSize(1);
    }

    @Test
    public void returnsLimitedListOfOldestGoalsWhenGoalsAmountGreaterThanLimit() {
        int limit = 2;
        AdGroupInfo adGroupInfo1 = addGoal(TODAY.minusDays(7));
        addGoal(TODAY.minusDays(4));
        AdGroupInfo adGroupInfo2 = addGoal(TODAY.minusDays(11));
        addGoal(TODAY.minusDays(3));
        addGoal(TODAY.minusDays(5));
        List<UsersSegment> videoGoals = segmentMetaFetchingStrategy.fetch(DEFAULT_SHARD, limit);
        assertThat(videoGoals).hasSize(limit);

        videoGoals.sort(Comparator.comparing(UsersSegment::getAdGroupId));
        assertThat(videoGoals.get(0).getAdGroupId()).isEqualTo(adGroupInfo1.getAdGroupId());
        assertThat(videoGoals.get(1).getAdGroupId()).isEqualTo(adGroupInfo2.getAdGroupId());
    }

    void addGoal() {
        addGoal(TODAY.minusDays(5));
    }

    AdGroupInfo addGoal(LocalDate lastReadLogDate) {
        AdGroupInfo adGroupInfo = steps.adGroupSteps().createActiveCpmVideoAdGroup();
        UsersSegment videoGoal = createReadyToBeFetchedGoal(adGroupInfo, lastReadLogDate);
        usersSegmentRepository.addSegments(adGroupInfo.getShard(), singletonList(videoGoal));
        return adGroupInfo;
    }

    abstract UsersSegment createReadyToBeFetchedGoal(AdGroupInfo adGroupInfo, LocalDate lastReadLogDate);

    @QueryWithoutIndex("потому что тестируемый код смотрит во всю базу, и ее нужно чистить от ненужных объектов")
    void clearVideoGoals() {
        dslContextProvider.ppc(DEFAULT_SHARD)
                .deleteFrom(VIDEO_SEGMENT_GOALS)
                .execute();
    }
}
