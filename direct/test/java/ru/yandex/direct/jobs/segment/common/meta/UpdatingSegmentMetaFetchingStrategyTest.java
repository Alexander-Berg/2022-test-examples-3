package ru.yandex.direct.jobs.segment.common.meta;

import java.time.LocalDate;

import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.direct.core.entity.adgroup.model.AdGroupType;
import ru.yandex.direct.core.entity.adgroup.model.UsersSegment;
import ru.yandex.direct.core.testing.data.TestUserSegments;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.jobs.base.logdatatransfer.MetaFetchingStrategy;
import ru.yandex.direct.jobs.configuration.JobsTest;

@JobsTest
@ExtendWith(SpringExtension.class)
public class UpdatingSegmentMetaFetchingStrategyTest extends SegmentMetaFetchingStrategyTestBase {

    @Override
    MetaFetchingStrategy<UsersSegment> createStrategyUnderTest() {
        return new UpdatingSegmentMetaFetchingStrategy(usersSegmentRepository, AdGroupType.CPM_VIDEO);
    }

    @Override
    UsersSegment createReadyToBeFetchedGoal(AdGroupInfo adGroupInfo, LocalDate lastReadLogDate) {
        return TestUserSegments.readyForUpdateSegment(adGroupInfo.getAdGroupId(), 123L)
                .withLastSuccessUpdateTime(lastReadLogDate.atStartOfDay());
    }
}
