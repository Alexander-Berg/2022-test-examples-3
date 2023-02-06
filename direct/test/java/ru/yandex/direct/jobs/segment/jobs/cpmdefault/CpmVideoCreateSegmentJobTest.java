package ru.yandex.direct.jobs.segment.jobs.cpmdefault;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.direct.common.db.PpcPropertyNames;
import ru.yandex.direct.core.entity.adgroup.model.UsersSegment;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.jobs.configuration.JobsTest;
import ru.yandex.direct.jobs.segment.MockedSegmentLogFetchingStrategyFactory;
import ru.yandex.direct.jobs.segment.MockedTargetUpdatingStrategyFactory;
import ru.yandex.direct.jobs.segment.SegmentJobsTestBase;
import ru.yandex.direct.jobs.segment.jobs.cmpdefault.CpmVideoCreateSegmentJob;
import ru.yandex.direct.scheduler.hourglass.TaskParametersMap;
import ru.yandex.direct.scheduler.support.DirectShardedJob;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.testing.data.TestUserSegments.readyForCreateSegment;

@JobsTest
@ExtendWith(SpringExtension.class)
public class CpmVideoCreateSegmentJobTest extends SegmentJobsTestBase {

    CpmVideoCreateSegmentJob job;

    AdGroupInfo adGroupInfo;
    UsersSegment segment;

    @BeforeEach
    public void before() {
        job = new CpmVideoCreateSegmentJob(
                metaFetchingStrategyFactory,
                new MockedSegmentLogFetchingStrategyFactory(FINISH_LOG_DATE, ppcPropertiesSupport),
                preprocessorFactory,
                new MockedTargetUpdatingStrategyFactory(AUDIENCE_ID),
                metaUpdatingStrategyFactory,
                ppcPropertiesSupport,
                solomonPushClient);

        adGroupInfo = steps.adGroupSteps().createActiveCpmVideoAdGroup();
        segment = readyForCreateSegment(adGroupInfo.getAdGroupId())
                .withTimeCreated(TODAY.minusDays(10).atStartOfDay())
                .withLastSuccessUpdateTime(TODAY.minusDays(5).atStartOfDay());
        usersSegmentRepository.addSegments(adGroupInfo.getShard(), singletonList(segment));
    }

    @Test
    public void createSegmentWhenPropertyIsTrue() {
        ppcPropertiesSupport.get(PpcPropertyNames.SEGMENT_JOBS_RUN_GREEDY_JOBS).set(true);

        runJob();

        UsersSegment actualVideoGoal = usersSegmentRepository
                .getSegmentByPrimaryKey(adGroupInfo.getShard(), segment.getAdGroupId(), segment.getType());
        assertThat(actualVideoGoal.getLastSuccessUpdateTime())
                .isEqualTo(FINISH_LOG_DATE.minusDays(1).atStartOfDay());
        assertThat(actualVideoGoal.getExternalAudienceId())
                .isEqualTo(AUDIENCE_ID);
    }

    @Test
    public void dontCreateSegmentWhenPropertyIsFalse() {
        ppcPropertiesSupport.get(PpcPropertyNames.SEGMENT_JOBS_RUN_GREEDY_JOBS).set(false);

        runJob();

        UsersSegment actualVideoGoal = fetchSegment();
        assertThat(actualVideoGoal.getExternalAudienceId()).isEqualTo(0L);
    }

    private void runJob() {
        job.initialize(TaskParametersMap.of(DirectShardedJob.SHARD_PARAM, String.valueOf(adGroupInfo.getShard())));
        job.execute();
    }

    private UsersSegment fetchSegment() {
        return usersSegmentRepository.getSegmentByPrimaryKey(adGroupInfo.getShard(),
                segment.getAdGroupId(), segment.getType());
    }
}
